package com.lughzdan.data
import android.content.Context
import androidx.room.withTransaction
import com.lughzdan.db.*
import com.lughzdan.domain.*
import com.lughzdan.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.UUID

class LocalGameRepository(private val context:Context,private val db:LughzdanDatabase,private val allowFixtures:Boolean,private val bundledContent:Pair<BundledManifest,ByteArray>?=null,private val recoverContent:suspend ()->Unit={}):ProgressRepository {
 private val dao=db.games()
 private val json=Json { encodeDefaults=true }
 private val registry=GameRegistry()
 private val definitions=object:LinkedHashMap<String,PuzzleDefinition>(128,0.75f,true) {
  override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,PuzzleDefinition>?)=size>256
 }
 private fun definition(raw:String):PuzzleDefinition=synchronized(definitions) {
  definitions[raw] ?: json.decodeFromString<PuzzleDefinition>(raw).also { definitions[raw]=it }
 }
 private val initializationMutex=Mutex()
 private var initialized=false
 private val catalogLimit=MutableStateFlow(200)
 override suspend fun loadMore() {catalogLimit.value=Math.addExact(catalogLimit.value,200)}
 @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
 override val puzzles=catalogLimit.flatMapLatest {dao.observePuzzles(it)}.map { rows->rows.map { definition(it.definition) } }.flowOn(Dispatchers.Default)
 private val sessionLimit=MutableStateFlow(50)
 override suspend fun loadMoreSessions() {sessionLimit.value=Math.addExact(sessionLimit.value,50)}
 @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
 private val historyRows=sessionLimit.flatMapLatest {dao.observeSessions(it)}
 private data class CachedRecord(val row:GameSessionEntity,val record:PlayRecord?)
 private val sessionCache=object:LinkedHashMap<String,CachedRecord>(64,0.75f,true) {
  override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,CachedRecord>?)=size>256
 }
 private val sessionCacheMutex=Mutex()
 private val contentChanges=combine(dao.observeContentCount(),db.content().observeHeads()){count,heads->count to heads}
 private var contentStamp:Pair<List<InstalledContentPackEntity>,Pair<Long,List<PackHeadEntity>>>?=null
 private val loadedSessions=combine(historyRows,dao.observePacks(),contentChanges) { rows,installed,count ->
  sessionCacheMutex.withLock {
  val stamp=installed to count
  if(contentStamp!=stamp) {sessionCache.clear();contentStamp=stamp}
  val available=mutableListOf<PlayRecord>();val unavailable=mutableListOf<UnavailableSession>()
  rows.forEach { row ->
   val cached=sessionCache[row.id]
   val record=if(cached!=null && cached.row==row) cached.record else readRecord(row).also {sessionCache[row.id]=CachedRecord(row,it)}
   if(record!=null) available.add(record)
   else unavailable.add(UnavailableSession(row.id,"تعذّر استعادة الجولة؛ بياناتها محفوظة دون تعديل."))
  }
  available.toList() to unavailable.toList()
  }
 }.flowOn(Dispatchers.IO)
 override val sessions=loadedSessions.map { it.first }
 override val unavailableSessions=loadedSessions.map { it.second }
 override fun observeSession(id:String):Flow<PlayRecord?> =
  combine(dao.observeSession(id).distinctUntilChanged(),dao.observePacks(),contentChanges) {row,_,_->row?.let {readRecord(it)}}.flowOn(Dispatchers.IO)
 private suspend fun readRecord(row:GameSessionEntity):PlayRecord? = try {
  val state=decodeStored(row)
  val puzzle=dao.puzzle(row.puzzleId,row.revision) ?: error("Missing pinned puzzle")
  require(dao.pinnedPack(state.packId,state.packVersion)!=null)
  PlayRecord(state,validateRecord(state,puzzle))
 } catch(cancelled:kotlinx.coroutines.CancellationException) {throw cancelled} catch(_:Exception) {null}
 override suspend fun startDaily(day:String):String = db.withTransaction {
  require(java.time.LocalDate.parse(day).toString()==day)
  val id="daily-$day"
  val existing=dao.session(id)
  if(existing!=null) {require(readRecord(existing)!=null);return@withTransaction id}
  val candidates=dao.puzzles().map {definition(it.definition)}.filter {it.difficulty==1 && it.type in registry.types}.sortedBy {it.puzzleId}
  require(candidates.isNotEmpty()) {"No introductory puzzles installed"}
  val index=Math.floorMod(java.time.LocalDate.parse(day).toEpochDay(),candidates.size.toLong()).toInt()
  val p=candidates[index]
  val row=dao.latestPuzzle(p.puzzleId) ?: error("Missing puzzle")
  val pack=dao.pack(row.packId) ?: error("Missing pack")
  val session=registry.plugin(p.type).createSession(p,id).copy(packId=pack.packId,packVersion=pack.version)
  dao.save(GameSessionEntity(id,p.puzzleId,p.revision,SessionCodec.encode(session),System.currentTimeMillis()))
  id
 }
 private fun decodeStored(row:GameSessionEntity)=SessionCodec.decode(row.state).also {
  require(it.id==row.id && it.puzzleId==row.puzzleId && it.revision==row.revision)
 }
 private suspend fun validateRecord(state:GameSession,row:LocalPuzzleEntity):PuzzleDefinition {
  require(db.content().head(state.packId)==null || db.content().membership(state.packId,state.packVersion,state.puzzleId,state.revision)==1) {"Puzzle does not belong to pinned pack version"}
  require(row.packId==state.packId && row.puzzleId==state.puzzleId && row.revision==state.revision)
  val p=definition(row.definition)
  require(p.puzzleId==row.puzzleId && p.revision==row.revision && registry.plugin(p.type).validatePuzzle(p).valid)
  require(registry.plugin(p.type).validateSession(p,state).valid)
  return p
 }
 override val progress=combine(dao.observeProgress(),dao.observeAchievements()) { rows,achievements ->
  Progress(rows.sumOf { it.bestScore },rows.count { it.completed },rows.sumOf { it.xp },
   rows.groupBy { it.gameType }.map { (type,items)->GameProgress(type,items.sumOf { it.bestScore },items.count { it.completed },items.sumOf { it.attempts },items.sumOf { it.xp }) },achievements.map { it.id })
 }
 override val packs=dao.observePacks().map { rows->rows.map { InstalledPack(it.packId,it.version,it.bytes,it.fixtureOnly) } }
 override suspend fun readImage(sessionId:String,imageId:String):ByteArray?=withContext(Dispatchers.IO) {
  val row=dao.session(sessionId) ?: return@withContext null
  val state=decodeStored(row)
  val puzzleRow=dao.puzzle(row.puzzleId,row.revision) ?: return@withContext null
  val p=validateRecord(state,puzzleRow)
  require(dao.pinnedPack(state.packId,state.packVersion)!=null)
  val image=p.payload.images.singleOrNull {it.id==imageId} ?: return@withContext null
  require(image.assetPath.matches(Regex("media/[A-Za-z0-9_/-]+\\.(png|webp|jpg)")) && image.assetPath.split('/').none {it==".."})
  val input=if(db.content().head(state.packId)==null) {
   if (!(allowFixtures && p.fixtureOnly)) {
    val manifest=json.decodeFromString<BundledManifest>(context.assets.open("bundled-manifest.json").bufferedReader().use {it.readText()})
    val pinned=dao.pinnedPack(state.packId,state.packVersion)
    require(!manifest.fixtureOnly && !p.fixtureOnly && manifest.packId==state.packId && manifest.version==state.packVersion && pinned?.sha256==manifest.sha256) {"Media must belong to the verified application bundle"}
   }
   context.assets.open(image.assetPath)
  } else {
   val root=java.io.File(context.filesDir,"packs/versions/${state.packId}/${state.packVersion}").canonicalFile
   val file=java.io.File(root,image.assetPath).canonicalFile
   require(file.path.startsWith(root.path+java.io.File.separator))
   file.inputStream()
  }
  val bytes=input.use { source->
   val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
   while(true) {val n=source.read(buffer);if(n<0)break;require(output.size()+n<=8*1024*1024) {"Image exceeds decode budget"};output.write(buffer,0,n)}
   output.toByteArray()
  }
  val hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
  require(hash==image.sha256) {"Media integrity mismatch"}
  bytes
 }
 override suspend fun initialize()=withContext(Dispatchers.IO) { initializationMutex.withLock {
  if(initialized) return@withLock
  recoverContent()
  // Release has no draft asset; empty content is explicit, never silently replaced by fixtures.
  if (bundledContent!=null || context.assets.list("").orEmpty().contains("bundled-manifest.json")) {
  val manifest=bundledContent?.first ?: json.decodeFromString<BundledManifest>(context.assets.open("bundled-manifest.json").bufferedReader().use { it.readText() })
  val bytes=bundledContent?.second ?: context.assets.open(manifest.path).use { it.readBytes() }
  BundledPackVerifier.verify(manifest,bytes,allowFixtures)
  val items=bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.map { line->
   json.decodeFromString<PuzzleDefinition>(line).also { require(registry.plugin(it.type).validatePuzzle(it).valid); require(!it.fixtureOnly || allowFixtures) }
  }.toList()
  require(items.map { it.puzzleId to it.revision }.distinct().size==items.size)
  db.withTransaction {
   val installed=dao.pack(manifest.packId)
   if(installed==null || manifest.version>installed.version) {
    val additions=items.mapIndexedNotNull { index,p->
     val encoded=json.encodeToString(p)
     val latest=dao.latestPuzzle(p.puzzleId)
     require(latest==null || (latest.packId==manifest.packId && json.decodeFromString<PuzzleDefinition>(latest.definition).type==p.type)) { "Puzzle identity cannot change pack or game type" }
     val prior=dao.puzzle(p.puzzleId,p.revision)
     require(prior==null || (prior.packId==manifest.packId && json.decodeFromString<PuzzleDefinition>(prior.definition)==p)) { "Revision is immutable" }
     if(prior==null) LocalPuzzleEntity(p.puzzleId,p.revision,manifest.packId,encoded,index) else null
    }
    dao.insertPuzzles(additions)
    dao.install(InstalledContentPackEntity(manifest.packId,manifest.version,manifest.sha256,manifest.bytes,manifest.fixtureOnly))
   } else if(installed.version==manifest.version) require(installed.sha256==manifest.sha256) { "Pack version is immutable" }
  }
  }
  // Recover once per repository lifetime; collecting a Flow must never pause live play.
  db.withTransaction {
   dao.storedSessions().forEach { row ->
    val state=runCatching { decodeStored(row).also { saved->
     require(dao.pinnedPack(saved.packId,saved.packVersion)!=null) { "Missing pinned pack" }
     validateRecord(saved,dao.puzzle(row.puzzleId,row.revision) ?: error("Missing pinned puzzle")) } }.getOrNull()
    if(state?.status==SessionStatus.ACTIVE) dao.save(row.copy(state=SessionCodec.encode(state.copy(status=SessionStatus.PAUSED))))
   }
  }
  initialized=true
 } }

 override val atlasRuns=db.atlas().observe().map {rows->rows.map {json.decodeFromString<AtlasRun>(it.state)}}.flowOn(Dispatchers.Default)

 override suspend fun createRun(run:AtlasRun):String = db.withTransaction {
  require(run.kind in setOf("journey","duel"))
  require(run.puzzleIds.size in 1..20 && run.puzzleIds.distinct().size==run.puzzleIds.size)
  require(run.step==0 && run.activeSessionId==null && run.scores==listOf(0,0))
  require(run.playerNames.size==if(run.kind=="duel")2 else 0)
  require(run.playerNames.all {it.isNotBlank() && it.length<=30})
  require(db.atlas().get(run.id)==null)
  for(id in run.puzzleIds) {
   val p=definition((dao.latestPuzzle(id) ?: error("Missing stage")).definition)
   require(p.difficulty<=ProgressionRules.unlockedDifficulty(dao.totalXp()))
  }
  val saved=run.copy(updatedAt=System.currentTimeMillis())
  db.atlas().save(AtlasRunEntity(saved.id,json.encodeToString(saved),saved.updatedAt))
  saved.id
 }

 override suspend fun nextInRun(id:String):String? = db.withTransaction {
  val row=db.atlas().get(id) ?: error("Missing run")
  val run=json.decodeFromString<AtlasRun>(row.state)
  if(run.finished)return@withTransaction null
  run.activeSessionId?.let {require(readRecord(dao.session(it) ?: error("Missing stage session"))!=null);return@withTransaction it}
  val puzzleRow=dao.latestPuzzle(run.puzzleIds[run.step]) ?: error("Missing stage")
  val p=definition(puzzleRow.definition)
  val pack=dao.pack(puzzleRow.packId) ?: error("Missing stage pack")
  val session=registry.plugin(p.type).createSession(p,UUID.randomUUID().toString()).copy(packId=pack.packId,packVersion=pack.version,atlasRunId=run.id,practice=run.kind=="duel")
  val now=System.currentTimeMillis()
  dao.save(GameSessionEntity(session.id,p.puzzleId,p.revision,SessionCodec.encode(session),now))
  val next=run.copy(activeSessionId=session.id,updatedAt=now)
  db.atlas().save(AtlasRunEntity(next.id,json.encodeToString(next),now))
  session.id
 }
 override suspend fun start(puzzleId:String):String = db.withTransaction {
  val row=dao.latestPuzzle(puzzleId) ?: error("Content missing")
  val p=definition(row.definition)
  require(p.difficulty<=ProgressionRules.unlockedDifficulty(dao.totalXp())) { "Difficulty is locked" }
  val pack=dao.pack(row.packId) ?: error("Pack missing")
  val session=registry.plugin(p.type).createSession(p,UUID.randomUUID().toString()).copy(packId=pack.packId,packVersion=pack.version)
  dao.save(GameSessionEntity(session.id,p.puzzleId,p.revision,SessionCodec.encode(session),System.currentTimeMillis()))
  session.id
 }
 override suspend fun act(sessionId:String,action:PlayerAction):GameSession = db.withTransaction {
  val row=dao.session(sessionId) ?: error("Session missing")
  val pRow=dao.puzzle(row.puzzleId,row.revision) ?: error("Pinned content missing")
  val p=definition(pRow.definition)
  val old=decodeStored(row)
  validateRecord(old,pRow)
  require(pRow.packId==old.packId && dao.pinnedPack(old.packId,old.packVersion)!=null) { "Pinned pack version missing" }
  val next=registry.plugin(p.type).evaluate(p,old,action)
  require(registry.plugin(p.type).validateSession(p,next).valid)
  if(next==old) return@withTransaction old
  dao.save(row.copy(state=SessionCodec.encode(next),updatedAt=System.currentTimeMillis()))
  if(next.status in setOf(SessionStatus.WON,SessionStatus.LOST) && old.status !in setOf(SessionStatus.WON,SessionStatus.LOST) && next.atlasRunId.isNotBlank()) {
   val runRow=db.atlas().get(next.atlasRunId) ?: error("Missing run")
   val run=json.decodeFromString<AtlasRun>(runRow.state)
   require(run.activeSessionId==next.id && !run.finished)
   val scores=run.scores.toMutableList()
   scores[if(run.kind=="duel")run.playerIndex else 0]+=next.score
   val updated=run.copy(step=run.step+1,scores=scores,activeSessionId=null,updatedAt=System.currentTimeMillis())
   db.atlas().save(AtlasRunEntity(updated.id,json.encodeToString(updated),updated.updatedAt))
  }
  if(!next.practice && next.status in setOf(SessionStatus.WON,SessionStatus.LOST)) {
   val receipt=dao.enqueue(SyncOperationEntity(UUID.randomUUID().toString(),sessionId,"complete_session",SessionCodec.encode(next)))
   if(receipt!=-1L) {
    val prior=dao.progress(p.puzzleId)
    dao.saveProgress(PlayerProgressEntity(p.puzzleId,maxOf(prior?.bestScore?:0,next.score),(prior?.completed?:false)||next.status==SessionStatus.WON,(prior?.attempts?:0)+1,p.type,maxOf(prior?.xp?:0,next.score)))
    if(next.status==SessionStatus.WON) {
     dao.award(AchievementEntity("first_win",System.currentTimeMillis()))
     dao.award(AchievementEntity("first_win_${p.type}",System.currentTimeMillis()))
    }
   }
  }
  next
 }
}
