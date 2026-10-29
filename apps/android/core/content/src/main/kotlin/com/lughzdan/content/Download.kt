package com.lughzdan.content
import java.io.*
import java.net.*
import java.security.MessageDigest
import java.util.concurrent.CancellationException

class DownloadControl {
 @Volatile var paused=false
 @Volatile var cancelled=false
 @Volatile private var abort:(()->Unit)?=null
 fun cancel(){cancelled=true;abort?.invoke()}
 fun pause(){paused=true;abort?.invoke()}
 internal fun check(){if(paused || cancelled || Thread.currentThread().isInterrupted)throw CancellationException("Download paused/cancelled")}
 internal fun bind(a:(()->Unit)?){abort=a;if(paused || cancelled)a?.invoke()}
}
data class ChunkRequest(val url:URL,val offset:Long,val ifRange:String?)
interface ChunkResponse:Closeable {val status:Int;fun header(name:String):String?;val body:InputStream}
fun interface ChunkTransport {fun open(request:ChunkRequest,control:DownloadControl):ChunkResponse}
class UrlConnectionTransport:ChunkTransport {
 override fun open(request:ChunkRequest,control:DownloadControl):ChunkResponse {
  require(request.url.protocol=="https");val c=request.url.openConnection() as HttpURLConnection
  c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=15000;c.setRequestProperty("Accept-Encoding","identity")
  if(request.offset>0){c.setRequestProperty("Range","bytes=${request.offset}-");request.ifRange?.let{c.setRequestProperty("If-Range",it)}}
  control.bind{c.disconnect()};control.check()
  try {val code=c.responseCode;return object:ChunkResponse {
   override val status=code
   override fun header(name:String)=c.getHeaderField(name)
   override val body:InputStream get()=if(code in 200..299)c.inputStream else c.errorStream ?: ByteArrayInputStream(byteArrayOf())
   override fun close(){c.disconnect();control.bind(null)}
  }}catch(e:Exception){c.disconnect();control.bind(null);control.check();throw e}
 }
}
/** Run off the main thread and serialize writers for each cache directory. */
class ChunkDownloader(private val root:File,private val transport:ChunkTransport=UrlConnectionTransport()) {
 init{require(root.mkdirs() || root.isDirectory);var ancestor:File?=root.absoluteFile;while(ancestor!=null){require(!java.nio.file.Files.isSymbolicLink(ancestor.toPath()));ancestor=ancestor.parentFile}}
 fun download(chunk:Chunk,url:()->URL,control:DownloadControl=DownloadControl(),progress:(Long)->Unit={}):File {
  require(chunk.sha256.matches(Regex("[a-f0-9]{64}")) && chunk.bytes in 1..8388608)
  val target=File(root,chunk.sha256);val part=File(root,"${chunk.sha256}.part");val tag=File(root,"${chunk.sha256}.etag")
  listOf(target,part,tag).forEach{require(!java.nio.file.Files.isSymbolicLink(it.toPath()))};control.check()
  if(verified(target,chunk)){progress(chunk.bytes);return target};target.delete()
  if(part.length()>chunk.bytes || !strong(tag.takeIf{it.isFile && it.length()<=1024}?.readText())){part.delete();tag.delete()}
  if(verified(part,chunk))return promote(part,target,tag)
  var refreshes=0
  while(true){
   control.check();val offset=part.length();val etag=tag.takeIf{it.exists()}?.readText()
   val r=transport.open(ChunkRequest(url(),offset,etag),control)
   if(r.status==401 || r.status==403){r.close();require(refreshes++<2){"Expired URL retry limit"};continue}
   r.use{
    if(r.status==416){if(verified(part,chunk))return promote(part,target,tag);error("Unsatisfied range")}
    require(r.status==200 || r.status==206){"HTTP ${r.status}"}
    require(r.header("Content-Encoding").let{it==null || it=="identity"})
    val append=r.status==206
    if(append){val match=Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matchEntire(r.header("Content-Range") ?: "") ?: error("Invalid Content-Range")
     val(start,end,total)=match.destructured
     require(start.toLong()==offset && total.toLong()==chunk.bytes && end.toLong()==chunk.bytes-1)
     require(offset>0 && strong(etag) && r.header("ETag")==etag){"Range validator changed"}
    }
    val expected=chunk.bytes-(if(append)offset else 0L);r.header("Content-Length")?.let{require(it.toLong()==expected)}
    val newTag=r.header("ETag");if(strong(newTag))tag.writeText(newTag!!) else tag.delete()
    var count=if(append)offset else 0L;progress(count)
    try{FileOutputStream(part,append).use{out->r.body.use{input->val b=ByteArray(32768);while(true){control.check();val n=input.read(b);if(n<0)break;require(count+n<=chunk.bytes){"Chunk overflow"};out.write(b,0,n);count+=n;progress(count)}};out.fd.sync()}}catch(e:Exception){control.check();throw e}
    control.check();require(count==chunk.bytes){"Truncated chunk"}
    if(!verified(part,chunk)){part.delete();tag.delete();error("Chunk digest mismatch")}
    return promote(part,target,tag)
   }
  }
 }
 private fun promote(part:File,target:File,tag:File):File{require(part.renameTo(target));tag.delete();return target}
 companion object {
  fun strong(s:String?):Boolean=s!=null && s.length in 2..1024 && s.startsWith('"') && s.endsWith('"') && s.substring(1,s.length-1).all{it.code>=0x21 && it!='"' && it.code!=127}
  fun verified(file:File,chunk:Chunk):Boolean=file.isFile && !java.nio.file.Files.isSymbolicLink(file.toPath()) && file.length()==chunk.bytes && digest(file)==chunk.sha256
  fun digest(file:File):String{val md=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val b=ByteArray(32768);while(true){val n=input.read(b);if(n<0)break;md.update(b,0,n)}};return md.digest().joinToString(""){"%02x".format(it)}}
 }
}
object ContentFiles {
 fun assemble(file:ManifestFile,chunkDirectory:File,stagingDirectory:File,control:DownloadControl=DownloadControl()):File {
  require(ManifestPolicy.safePath(file.path) && file.bytes>0 && file.chunks.isNotEmpty())
  require(ManifestPolicy.sum(file.chunks.map { it.bytes })==file.bytes);control.check()
  require(stagingDirectory.mkdirs() || stagingDirectory.isDirectory)
  var ancestor:File?=stagingDirectory.absoluteFile
  while(ancestor!=null) { require(!java.nio.file.Files.isSymbolicLink(ancestor.toPath()));ancestor=ancestor.parentFile }
  val out=File(stagingDirectory,file.path)
  require(out.canonicalPath.startsWith(stagingDirectory.canonicalPath+File.separator))
  var parent=stagingDirectory
  file.path.split('/').dropLast(1).forEach { segment ->
   parent=File(parent,segment);require(!java.nio.file.Files.isSymbolicLink(parent.toPath()))
   require(parent.mkdir() || parent.isDirectory)
  }
  require(!java.nio.file.Files.isSymbolicLink(out.toPath()))
  // Staging is private to one installer; never overwrite an already materialized file.
  require(out.createNewFile()) { "Staging output already exists" }
  try {
   FileOutputStream(out).use { sink -> file.chunks.forEach { c ->
    control.check();val source=File(chunkDirectory,c.sha256);require(ChunkDownloader.verified(source,c))
    source.inputStream().use { input -> val buffer=ByteArray(32768);while(true) {
     control.check();val n=input.read(buffer);if(n<0)break;sink.write(buffer,0,n)
    } }
   };sink.fd.sync() }
   control.check();require(out.length()==file.bytes && ChunkDownloader.digest(out)==file.sha256)
   validateType(out,file.kind);return out
  } catch(e:Exception) { out.delete();throw e }
 }

 fun validateType(file:File,kind:String){
  val head=file.inputStream().use{input->val a=ByteArray(12);var n=0;while(n<12){val r=input.read(a,n,12-n);if(r<0)break;n+=r};a.copyOf(n)}
  when(kind){
   "puzzles"->{require(file.length()<=8388608);InputStreamReader(file.inputStream(),Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)).use { reader->val chars=CharArray(8192);while(reader.read(chars)>=0){} };file.inputStream().buffered().use{input->var line=0;while(true){val b=input.read();if(b<0)break;if(b==10)line=0 else{line++;require(line<=256*1024){"JSONL line limit"}}}}}
   "rights"->file.inputStream().use{val bytes=it.readNBytesCompat(16*1024*1024);Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes));Unit}
   "media"->require(when(file.extension){
    "png"->head.take(8).toByteArray().contentEquals(byteArrayOf(-119,80,78,71,13,10,26,10))
    "jpg"->head.size>=3 && head[0]==(-1).toByte() && head[1]==(-40).toByte() && head[2]==(-1).toByte()
    "ogg"->head.take(4).toByteArray().toString(Charsets.US_ASCII)=="OggS"
    "webp"->head.size==12 && head.take(4).toByteArray().toString(Charsets.US_ASCII)=="RIFF" && head.drop(8).toByteArray().toString(Charsets.US_ASCII)=="WEBP"
    else->false}){"Media magic mismatch"}
   else->error("Unknown file kind")
  }
 }
}
