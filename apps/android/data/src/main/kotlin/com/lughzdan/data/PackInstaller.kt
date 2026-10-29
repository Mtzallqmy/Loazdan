package com.lughzdan.data

import androidx.room.withTransaction
import com.lughzdan.content.*
import com.lughzdan.db.*
import com.lughzdan.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import android.system.Os
import android.system.OsConstants

/** One lock shared by download/install/delete/recovery. Database pointer is the commit point. */
class PackInstaller(private val db:LughzdanDatabase, val root:File, private val verifier:ManifestVerifier,
 private val appVersion:Int=3, private val checkpoint:(String)->Unit={}) {
 val mutex=Mutex()
 private val json=Json { encodeDefaults=true }
 private val dao=db.content()
 val chunks=File(root,"chunks")
 private fun directory(id:String,version:Int)=File(root,"versions/$id/$version")
 private fun syncDirectory(file:File) { val fd=Os.open(file.absolutePath,OsConstants.O_RDONLY or OsConstants.O_DIRECTORY,0);try {Os.fsync(fd)} finally {Os.close(fd)} }
 suspend fun verify(bytes:ByteArray):SignedManifest {
  val preliminary=verifier.verify(bytes.inputStream(),appVersion,2)
  return verifier.verify(bytes.inputStream(),appVersion,2,dao.head(preliminary.packId)?.highWater?:0)
 }
 private suspend fun dependencies(m:SignedManifest) {
  val installed=mutableMapOf<Dependency,SignedManifest>()
  suspend fun load(d:Dependency,seen:MutableSet<Dependency>) {
   require(seen.add(d)) {"Dependency cycle"}
   if(d !in installed) {
    require(db.games().pinnedPack(d.packId,d.version)!=null) {"Dependency must be installed first"}
    val j=dao.journal(d.packId,d.version) ?: error("Dependency manifest unavailable")
    val manifest=verifier.verify(j.manifest.byteInputStream(),appVersion,2)
    require(filesValid(manifest,directory(d.packId,d.version))) {"Dependency damaged"}
    installed[d]=manifest
    manifest.dependencies.forEach {load(it,seen)}
   }
   seen.remove(d)
  }
  m.dependencies.forEach {load(it,mutableSetOf())}
  ManifestPolicy.validateDependencies(m,installed)
 }
 private fun filesValid(m:SignedManifest,dir:File):Boolean = m.files.all { f->
  val file=File(dir,f.path)
  file.canonicalPath.startsWith(dir.canonicalPath+File.separator) && ChunkDownloader.verified(file,Chunk(f.sha256,f.bytes))
 }
 /** Chunks already verified by downloader. Never modifies a previously installed directory. */
 suspend fun install(bytes:ByteArray,title:String,control:DownloadControl=DownloadControl())=withContext(Dispatchers.IO) {mutex.withLock {
  val m=verify(bytes);dependencies(m)
  val id=m.packId;val digest=java.security.MessageDigest.getInstance("SHA-256").digest(json.encodeToString(m).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
  val previous=dao.head(id)
  require(previous!=null || db.games().pack(id)==null) {"Bundled pack updates require an explicit ownership migration"}
  dao.journal(id,m.version)?.let {require(it.digest==digest) {"Published version is immutable"}}
  if(previous?.activeVersion==m.version) {require(filesValid(m,directory(id,m.version)));return@withLock}
  val staging=File(root,"staging/$id/${m.version}")
  val final=directory(id,m.version)
  root.mkdirs()
  require(root.usableSpace>m.installedBytes+maxOf(100L*1024*1024,m.installedBytes/10)) {"Insufficient installation space"}
  // Empty inactive pointer must precede imports so no staged row enters the playable catalog.
  db.withTransaction {
   if(previous==null)dao.head(PackHeadEntity(id,null,null,0,title,"",0))
   dao.journal(InstallJournalEntity(id,m.version,"PREPARING",bytes.toString(Charsets.UTF_8),digest,System.currentTimeMillis()))
  }
  checkpoint("JOURNALED")
  if(!final.exists()) {
   staging.deleteRecursively();check(staging.mkdirs())
   m.files.forEach {ContentFiles.assemble(it,chunks,staging,control)}
   FileOutputStream(File(staging,"manifest.json")).use {it.write(bytes);it.fd.sync()}
   checkpoint("MATERIALIZED")
   importIndex(m,staging)
   dao.journal(InstallJournalEntity(id,m.version,"PREPARED",bytes.toString(Charsets.UTF_8),digest,System.currentTimeMillis()))
   staging.walkBottomUp().filter {it.isDirectory}.forEach(::syncDirectory)
   final.parentFile!!.mkdirs();check(staging.renameTo(final));syncDirectory(final.parentFile!!)
   checkpoint("RENAMED")
  } else {require(filesValid(m,final));importIndex(m,final)}
  if(control.cancelled || control.paused)throw java.util.concurrent.CancellationException()
  commit(m,title,digest,bytes.toString(Charsets.UTF_8),final)
  checkpoint("COMMITTED")
 } }
 private suspend fun importIndex(m:SignedManifest,dir:File) {
  var ordinal=0
  val seen=mutableSetOf<Pair<String,Int>>()
  for(file in m.files.filter {it.kind=="puzzles"}) {
   // Each shard/line was bounded and UTF-8 validated before reaching this parser.
   File(dir,file.path).bufferedReader().useLines { lines->
    for(line in lines) {
     require(line.isNotBlank() && line.length<=262144)
     val p=json.decodeFromString<PuzzleDefinition>(line)
     require(p.status=="published" && !p.fixtureOnly && p.sourceRefs.all {ref->ref in m.sourceRefs}) {"Only referenced published puzzles can be installed"}
     require(GameRegistry().plugin(p.type).validatePuzzle(p).valid)
     require(seen.add(p.puzzleId to p.revision)) {"Duplicate puzzle"}
     db.withTransaction {
      val prior=db.games().puzzle(p.puzzleId,p.revision)
      require(prior==null || (prior.packId==m.packId && json.decodeFromString<PuzzleDefinition>(prior.definition)==p)) {"Immutable puzzle revision changed"}
      val identity=db.games().anyPuzzle(p.puzzleId)
      require(identity==null || (identity.packId==m.packId && json.decodeFromString<PuzzleDefinition>(identity.definition).type==p.type))
      if(prior==null)db.games().insertPuzzles(listOf(LocalPuzzleEntity(p.puzzleId,p.revision,m.packId,line,ordinal)))
      dao.members(listOf(PackMemberEntity(m.packId,m.version,p.puzzleId,p.revision)))
     }
     ordinal++
    }
   }
  }
  require(ordinal>0 && dao.memberCount(m.packId,m.version)==ordinal)
  checkpoint("INDEXED")
 }
 private suspend fun commit(m:SignedManifest,title:String,digest:String,raw:String,dir:File) {
  require(filesValid(m,dir))
  db.withTransaction {
   val old=dao.head(m.packId)
   require(m.version >= (old?.highWater?:0))
   db.games().install(InstalledContentPackEntity(m.packId,m.version,digest,m.installedBytes,false))
   dao.head(PackHeadEntity(m.packId,m.version,old?.activeVersion,m.version,title,raw,m.installedBytes))
   dao.journal(InstallJournalEntity(m.packId,m.version,"COMMITTED",raw,digest,System.currentTimeMillis()))
  }
 }
 suspend fun recover()=withContext(Dispatchers.IO) {mutex.withLock {
  for(j in dao.journals()) {
   if(j.phase=="DELETED")continue
   val m=runCatching {verifier.verify(j.manifest.byteInputStream(),appVersion,2)}.getOrNull() ?: continue
   val final=directory(j.packId,j.version)
   val head=dao.head(j.packId)
   if(head?.activeVersion==j.version && !filesValid(m,final)) {
    val backup=head.previousVersion?.let {dao.journal(j.packId,it)}
    val prior=backup?.let {runCatching {verifier.verify(it.manifest.byteInputStream(),appVersion,2)}.getOrNull()}
    val valid=prior!=null && filesValid(prior,directory(prior.packId,prior.version))
    db.withTransaction {dao.head(head.copy(activeVersion=if(valid)prior!!.version else null,previousVersion=null,manifest=if(valid)backup!!.manifest else head.manifest,installedBytes=if(valid)prior!!.installedBytes else 0));dao.journal(j.copy(phase="QUARANTINED"));dao.deleteInstalledVersion(j.packId,j.version)}
   } else if(j.phase in setOf("PREPARING","PREPARED") && final.exists() && filesValid(m,final) && m.version >= (head?.highWater?:0)) {
    dependencies(m);importIndex(m,final);commit(m,head?.title?:m.packId,j.digest,j.manifest,final)
   }
  }
 } }
 suspend fun delete(id:String)=withContext(Dispatchers.IO) {mutex.withLock {
  val head=dao.head(id) ?: error("Bundled content cannot be deleted")
  require(dao.journals().none {j->j.phase=="COMMITTED" && j.packId!=id && runCatching {json.decodeFromString<SignedManifest>(j.manifest).dependencies.any {it.packId==id}}.getOrDefault(true)}) {"Another pack depends on this pack"}
  db.withTransaction {
   dao.head(head.copy(activeVersion=null,previousVersion=null,installedBytes=0))
   dao.journals().filter {it.packId==id}.forEach {dao.journal(it.copy(phase="DELETED"))}
   dao.deleteInstalled(id)
  }
  // The pointer is cleared before removal; existing progress, states and receipts survive.
  File(root,"versions/$id").deleteRecursively();File(root,"staging/$id").deleteRecursively()
 } }
}
