package com.lughzdan.content

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.CancellationException

class DownloadTest {
 private val bytes="streamed chunk fixture".toByteArray()
 private fun chunk():Chunk { val f=Files.createTempFile("hash","test").toFile();return try {f.writeBytes(bytes);Chunk(ChunkDownloader.digest(f),bytes.size.toLong())}finally{f.delete()} }
 private fun response(status:Int,body:ByteArray=bytes,headers:Map<String,String> = emptyMap())=object:ChunkResponse {
  override val status=status
  override val body=ByteArrayInputStream(body)
  override fun header(name:String)=headers[name]
  override fun close(){}
 }
 private fun fails(action:()->Unit) {try{action();fail("Expected rejection")}catch(e:IllegalArgumentException){}catch(e:IllegalStateException){}}
 @Test fun verifiedCacheSkipsTransportAndReportsFullProgress(){
  val root=Files.createTempDirectory("chunks").toFile();try{
   val c=chunk();File(root,c.sha256).writeBytes(bytes);var progress=0L
   val result=ChunkDownloader(root,ChunkTransport{_,_->error("network used")}).download(c,{URL("https://example.test/chunk")},progress={progress=it})
   assertArrayEquals(bytes,result.readBytes());assertEquals(c.bytes,progress)
  }finally{root.deleteRecursively()}
 }
 @Test fun rangeUsesStrongValidatorAndChecksContentRange(){
  val root=Files.createTempDirectory("chunks").toFile();try{
   val c=chunk();File(root,"${c.sha256}.part").writeBytes(bytes.copyOf(5));File(root,"${c.sha256}.etag").writeText("\"stable\"")
   val d=ChunkDownloader(root,ChunkTransport{r,_->assertEquals(5L,r.offset);assertEquals("\"stable\"",r.ifRange);response(206,bytes.copyOfRange(5,bytes.size),mapOf("ETag" to "\"stable\"","Content-Range" to "bytes 5-${bytes.size-1}/${bytes.size}"))})
   assertArrayEquals(bytes,d.download(c,{URL("https://example.test/chunk")}).readBytes())
  }finally{root.deleteRecursively()}
 }
 @Test fun fullResponseRestartsPartialAndRefreshIsBounded(){
  val root=Files.createTempDirectory("chunks").toFile();try{
   val c=chunk();File(root,"${c.sha256}.part").writeBytes(bytes.copyOf(5));File(root,"${c.sha256}.etag").writeText("\"old\"")
   assertArrayEquals(bytes,ChunkDownloader(root,ChunkTransport{_,_->response(200)}).download(c,{URL("https://example.test/chunk")}).readBytes())
   File(root,c.sha256).delete();var calls=0
   fails{ChunkDownloader(root,ChunkTransport{_,_->calls++;response(403)}).download(c,{URL("https://example.test/chunk")})};assertEquals(3,calls)
  }finally{root.deleteRecursively()}
 }
 @Test fun rejectsChangedRangeDigestAndOverflow(){
  for(mode in 0..2){val root=Files.createTempDirectory("chunks").toFile();try{
   val c=chunk();if(mode==0){File(root,"${c.sha256}.part").writeBytes(bytes.copyOf(5));File(root,"${c.sha256}.etag").writeText("\"old\"")}
   fails{ChunkDownloader(root,ChunkTransport{_,_->when(mode){0->response(206,bytes.copyOfRange(5,bytes.size),mapOf("ETag" to "\"changed\"","Content-Range" to "bytes 5-${bytes.size-1}/${bytes.size}"));1->response(200,ByteArray(bytes.size));else->response(200,bytes+byteArrayOf(1))}}).download(c,{URL("https://example.test/chunk")})}
   assertFalse(File(root,c.sha256).exists())
  }finally{root.deleteRecursively()}}
 }
 @Test fun unsatisfiedRangeIsRejectedUnlessBytesAlreadyVerify(){
  val root=Files.createTempDirectory("chunks").toFile();try{
   val c=chunk();File(root,"${c.sha256}.part").writeBytes(bytes.copyOf(5));File(root,"${c.sha256}.etag").writeText("\"stable\"")
   fails{ChunkDownloader(root,ChunkTransport{_,_->response(416)}).download(c,{URL("https://example.test/chunk")})}
   File(root,"${c.sha256}.part").writeBytes(bytes)
   assertArrayEquals(bytes,ChunkDownloader(root,ChunkTransport{_,_->error("network used")}).download(c,{URL("https://example.test/chunk")}).readBytes())
  }finally{root.deleteRecursively()}
 }
 @Test fun pauseRetainsValidatedPartialForResume(){
  val root=Files.createTempDirectory("chunks").toFile();try{
   val c=chunk();val control=DownloadControl()
   try {ChunkDownloader(root,ChunkTransport{_,_->response(200,headers=mapOf("ETag" to "\"stable\""))}).download(c,{URL("https://example.test/chunk")},control){if(it>0)control.pause()};fail()}catch(expected:CancellationException){}
   assertFalse(File(root,c.sha256).exists());assertTrue(File(root,"${c.sha256}.part").exists())
   assertArrayEquals(bytes,ChunkDownloader(root,ChunkTransport{_,_->error("network used")}).download(c,{URL("https://example.test/chunk")}).readBytes())
  }finally{root.deleteRecursively()}
 }
 @Test fun cancellationPreventsNetwork(){val root=Files.createTempDirectory("chunks").toFile();try{
  val control=DownloadControl();control.cancel()
  try{ChunkDownloader(root,ChunkTransport{_,_->error("network used")}).download(chunk(),{URL("https://example.test")},control);fail()}catch(expected:CancellationException){}
 }finally{root.deleteRecursively()}}
 @Test fun materializationRejectsSymlinkAndRemovesInvalidMagic(){
  val root=Files.createTempDirectory("materialize").toFile();try{
   val cache=File(root,"cache").apply{mkdir()};val c=chunk();File(cache,c.sha256).writeBytes(bytes)
   val staging=File(root,"stage").apply{mkdir()};val file=ManifestFile("media/image.png","media",c.sha256,c.bytes,listOf(c))
   fails{ContentFiles.assemble(file,cache,staging)};assertFalse(File(staging,file.path).exists())
   File(staging,"media").delete();val outside=File(root,"outside").apply{mkdir()};Files.createSymbolicLink(File(staging,"media").toPath(),outside.toPath())
   fails{ContentFiles.assemble(file,cache,staging)};assertFalse(File(outside,"image.png").exists())
  }finally{root.deleteRecursively()}
 }
}
