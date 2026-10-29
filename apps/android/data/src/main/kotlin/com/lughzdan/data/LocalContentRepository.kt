package com.lughzdan.data

import android.content.Context
import android.net.ConnectivityManager
import androidx.work.*
import com.lughzdan.content.*
import com.lughzdan.db.*
import com.lughzdan.domain.*
import com.lughzdan.network.ContentApi
import com.lughzdan.preferences.SettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LocalContentRepository(private val context:Context,private val db:LughzdanDatabase,private val settings:SettingsStore,
 private val installer:PackInstaller,private val remote:ContentApi?):ContentRepository {
 private val dao=db.content()
 private val remotePacks=MutableStateFlow<List<ContentPack>>(emptyList())
 private val usage=MutableStateFlow(ContentStorage())
 private val controls=ConcurrentHashMap<String,DownloadControl>()
 private val transferMutex=Mutex() // bounded concurrency=1, also excludes shared chunk writers
 private val work get()=WorkManager.getInstance(context)
 override val disabledReason=flowOf(if(remote==null)"لم تُحدّد خدمة نشر الحزم ومفاتيحها العامة بعد؛ الألعاب المثبتة تعمل دون اتصال." else null)
 override val preferences=settings.settings.map {ContentPreferences(it.wifiOnly,it.automaticDownloads)}
 override val downloads=dao.observeDownloads().map {rows->rows.map {ContentDownload(it.packId,DownloadStatus.valueOf(it.state),it.transferred,it.total,it.error)}}
 override val storage=usage.asStateFlow()
 override val catalog=combine(remotePacks,dao.observeHeads(),db.games().observePacks()) {available,heads,installed->
  val list=available.associateBy {it.id}.toMutableMap()
  installed.groupBy {it.packId}.forEach {(id,versions)->
   val h=heads.find {it.packId==id};val v=if(h==null)versions.maxBy {it.version} else versions.find {it.version==h.activeVersion}
   if(v!=null) {
    val m=h?.manifest?.takeIf {it.isNotEmpty()}?.let {runCatching{Json.decodeFromString<SignedManifest>(it)}.getOrNull()}
    val old=list[id]
    list[id]=(old ?: ContentPack(id,h?.title?:"المحتوى الأساسي",v.version,v.bytes,published=!v.fixtureOnly)).copy(installedVersion=v.version,installedBytes=v.bytes,license=m?.references?.map {it.licenseCode}?.distinct()?.joinToString("، ")?:"",sources=m?.references?.map {it.attribution}?:emptyList())
   }
  }
  list.values.sortedBy {it.title}
 }
 override suspend fun refresh()=withContext(Dispatchers.IO) {
  installer.recover();measure()
  remote?.let {api->
   val found=mutableListOf<ContentPack>();var cursor:String?=null;val seen=mutableSetOf<String?>()
   do {check(seen.add(cursor));val page=api.packs(cursor);found+=page.items.map {ContentPack(it.id,it.title,it.version,it.downloadBytes,it.installedBytes)};cursor=page.nextCursor;require(found.size<=10000)}while(cursor!=null)
   remotePacks.value=found
   if(settings.settings.first().automaticDownloads)for(h in dao.heads().filter {it.activeVersion!=null}) {
    if(found.any {it.id==h.packId && it.version>h.highWater} && dao.download(h.packId)?.state !in activeStates)download(h.packId)
   }
  }
 }
 override suspend fun details(packId:String)=catalog.first().find {it.id==packId}
 override suspend fun download(packId:String,allowMetered:Boolean) {
  check(remote!=null){"Cloud content unavailable"}
  val p=details(packId) ?: error("Unknown published pack")
  require(p.published)
  val old=dao.download(packId)
  if(old?.state in activeStates)return
  dao.download(DownloadEntity(packId,p.version,"QUEUED",0,p.downloadBytes,allowMetered,null,0,System.currentTimeMillis()))
  schedule(packId,allowMetered)
 }
 private suspend fun schedule(id:String,allowMetered:Boolean) {
  val wifi=settings.settings.first().wifiOnly && !allowMetered
  val request=OneTimeWorkRequestBuilder<PackDownloadWorker>().setInputData(workDataOf("packId" to id))
   .setConstraints(Constraints.Builder().setRequiredNetworkType(if(wifi)NetworkType.UNMETERED else NetworkType.CONNECTED).build())
   .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
  work.enqueueUniqueWork("pack:$id",ExistingWorkPolicy.KEEP,request)
 }
 override suspend fun pause(packId:String) {controls[packId]?.pause();withContext(Dispatchers.IO){work.cancelUniqueWork("pack:$packId").result.get()};dao.download(packId)?.let {dao.download(it.copy(state="PAUSED"))}}
 override suspend fun cancel(packId:String) {controls[packId]?.cancel();withContext(Dispatchers.IO){work.cancelUniqueWork("pack:$packId").result.get()};dao.download(packId)?.let {dao.download(it.copy(state="CANCELLED"))}}
 override suspend fun resume(packId:String) {val d=dao.download(packId) ?: error("Unknown download");require(d.state in setOf("PAUSED","FAILED","CANCELLED","WAITING_FOR_WIFI"));dao.download(d.copy(state="QUEUED",error=null));schedule(packId,d.allowMetered)}
 override suspend fun retry(packId:String)=resume(packId)
 override suspend fun delete(packId:String) {cancel(packId);installer.delete(packId);measure()}
 override suspend fun cleanupTemporaryFiles()=withContext(Dispatchers.IO) {transferMutex.withLock {installer.mutex.withLock {
  require(dao.downloads().none {it.state in activeStates}) {"Downloads are active"}
  File(installer.root,"staging").deleteRecursively()
  installer.chunks.deleteRecursively() // reconstructed from immutable active files only by a later redownload
  measure()
 }}}
 override suspend fun setWifiOnly(value:Boolean){settingsWifiOnly=value;settings.wifiOnly(value)}
 override suspend fun setAutomaticDownloads(value:Boolean){settings.automaticDownloads(value)}
 private suspend fun measure()=withContext(Dispatchers.IO) {
  fun size(dir:File)=dir.walkTopDown().filter {it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath())}.sumOf {it.length()}
  usage.value=ContentStorage(size(File(installer.root,"versions")),size(installer.chunks)+size(File(installer.root,"staging")))
 }
 private fun metered()=(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
 /** Short bounded worker; verified chunks and offsets survive quota stops and reboot. */
 suspend fun runDownload(id:String,stopped:()->Boolean):Boolean=withContext(Dispatchers.IO) {transferMutex.withLock {
  val initial=dao.download(id) ?: return@withLock true
  if(initial.state in setOf("PAUSED","CANCELLED","COMPLETED"))return@withLock true
  val api=remote ?: return@withLock true
  val control=DownloadControl();controls[id]=control
  var transferred=0L
  suspend fun state(value:String,error:String?=null) {val current=dao.download(id)?:return; if(current.state !in setOf("PAUSED","CANCELLED"))dao.download(current.copy(state=value,transferred=transferred,error=error,updatedAt=System.currentTimeMillis()))}
  try {
   val bytes=api.manifest(id,initial.version).use {response->response.byteStream().use {input->val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(output.size()+n<=16*1024*1024);output.write(buffer,0,n)};output.toByteArray()}}
   val m=installer.verify(bytes);require(m.packId==id && m.version==initial.version)
   dao.download(initial.copy(total=m.downloadBytes))
   val chunks=m.files.flatMap {it.chunks}.distinctBy {it.sha256}
   val cached=chunks.filter {ChunkDownloader.verified(File(installer.chunks,it.sha256),it)}
   require(installer.root.usableSpace>=ManifestPolicy.requiredStorage(m,cached.map {it.sha256}.toSet(),maxOf(100L*1024*1024,m.installedBytes/10))) {"Insufficient free space"}
   val started=System.nanoTime();transferred=cached.sumOf {it.bytes}
   state("DOWNLOADING")
   val downloader=ChunkDownloader(installer.chunks)
   for(chunk in chunks.filter {it !in cached}) {
    if(stopped() || System.nanoTime()-started>240_000_000_000L){state("QUEUED");return@withLock false}
    if(settings.settings.first().wifiOnly && !initial.allowMetered && metered()){state("WAITING_FOR_WIFI");return@withLock false}
    val before=transferred;var lastReport=0L
    downloader.download(chunk,url={runBlocking {val links=api.downloads(id,m.version);require(links.packId==id && links.version==m.version);URL(links.objects.single {it.sha256==chunk.sha256}.url)}},control=control) {count->
     if(stopped() || (settingsWifiOnly && !initial.allowMetered && metered()))control.pause()
     transferred=before+count
     val now=System.nanoTime();if(now-lastReport>250_000_000L){runBlocking {state("DOWNLOADING")};lastReport=now}
    }
    state("DOWNLOADING")
   }
   state("VERIFYING");state("INSTALLING")
   installer.install(bytes,details(id)?.title?:id,control)
   dao.download(initial.copy(state="COMPLETED",transferred=m.downloadBytes,total=m.downloadBytes,updatedAt=System.currentTimeMillis()))
   measure();true
  } catch(e:java.util.concurrent.CancellationException) {state("QUEUED");false}
  catch(e:java.io.IOException){state("QUEUED","انقطع الاتصال؛ ستُستأنف الأجزاء المحفوظة.");false}
  catch(_:Exception){state("FAILED","تعذر التحقق أو التثبيت. النسخة السابقة والتقدم محفوظان.");true}
  finally {controls.remove(id)}
 } }
 @Volatile private var settingsWifiOnly=true
 suspend fun initialize() {settingsWifiOnly=settings.settings.first().wifiOnly;installer.root.mkdirs();installer.recover();measure()}
 companion object {private val activeStates=setOf("QUEUED","DOWNLOADING","VERIFYING","INSTALLING","WAITING_FOR_WIFI")}
}
interface ContentRuntimeOwner {val contentRuntime:LocalContentRepository}
class PackDownloadWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
 override suspend fun doWork():Result {
  val runtime=(applicationContext as? ContentRuntimeOwner)?.contentRuntime ?: return Result.failure()
  val id=inputData.getString("packId") ?: return Result.failure()
  return if(runtime.runDownload(id){isStopped})Result.success() else Result.retry()
 }
}
