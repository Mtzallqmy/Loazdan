package com.lughzdan.data

import android.content.Context
import com.lughzdan.content.ChunkDownloader
import com.lughzdan.db.LughzdanDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Uses the normal signature, chunk, revision and transactional installation path offline. */
object BundledLiteraryPack {
 const val ID="4b260af1-d41e-5759-bf8b-cc4867948a36"
 suspend fun install(context:Context,db:LughzdanDatabase,installer:PackInstaller)=withContext(Dispatchers.IO) {
  if(!context.assets.list("literary").orEmpty().contains("manifest.json"))return@withContext
  val head=db.content().head(ID)
  if((head?.activeVersion ?: 0)>=2 || (head?.highWater ?: 0)>2 || db.content().journal(ID,2)?.phase=="DELETED")return@withContext
  val raw=context.assets.open("literary/manifest.json").use {it.readBytes()}
  val manifest=installer.verify(raw)
  require(manifest.packId==ID && manifest.version==2)
  installer.mutex.lock()
  try {
   installer.chunks.mkdirs()
   for(file in manifest.files) {
    require(file.chunks.size==1 && file.chunks.single().sha256==file.sha256)
    val chunk=file.chunks.single();val target=File(installer.chunks,chunk.sha256)
    if(ChunkDownloader.verified(target,chunk))continue
    val staging=File(installer.chunks,chunk.sha256+".bundle")
    try {
     context.assets.open("literary/"+file.path).use {source->FileOutputStream(staging).use {sink->source.copyTo(sink);sink.fd.sync()}}
     require(ChunkDownloader.verified(staging,chunk)) {"Bundled literary chunk mismatch"}
     check(staging.renameTo(target))
    } finally {staging.delete()}
   }
  } finally {installer.mutex.unlock()}
  installer.install(raw,"ديوان الألغاز — ٤٨ تحديًا")
 }
}
