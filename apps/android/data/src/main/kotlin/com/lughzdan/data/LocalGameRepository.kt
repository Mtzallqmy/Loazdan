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

class LocalGameRepository(private val context:Context,private val db:LughzdanDatabase,private val allowFixtures:Boolean,private val bundledContent:Pair<BundledManifest,ByteArray>?=null):ProgressRepository {
 private val dao=db.games()
 private val json=Json { encodeDefaults=true }
 private val registry=GameRegistry()
 private val initializationMutex=Mutex()
 private var initialized=false
 override val puzzles=dao.observePuzzles().map { rows->rows.map { json.decodeFromString<PuzzleDefinition>(it.definition) } }
 private val loadedSessions=combine(dao.observeSessions(),dao.observePacks(),dao.observeContentCount()) { rows,installed,_ ->
  val available=mutableListOf<PlayRecord>();val unavailable=mutableListOf<UnavailableSession>()
  rows.forEach { row ->
   val state=runCatching { decodeStored(row) }.getOrNull()
   val puzzleRow=dao.puzzle(row.puzzleId,row.revision)
   val puzzle=if(state!=null && puzzleRow!=null) runCatching { validateRecord(state,puzzleRow) }.getOrNull() else null
   val pack=state?.let { saved->installed.find { it.packId==saved.packId && it.version==saved.packVersion } }
   if(state!=null && puzzle!=null && pack!=null) available.add(PlayRecord(state,puzzle))
   else unavailable.add(UnavailableSession(row.id,if(state==null) "حالة هذه الجولة غير مدعومة؛ بياناتها محفوظة دون تعديل." else "محتوى الجولة المثبّت غير متاح؛ بيانات التقدم محفوظة."))
  }
  available.toList() to unavailable.toList()
 }
 override val sessions=loadedSessions.map { it.first }
 override val unavailableSessions=loadedSessions.map { it.second }
 private fun decodeStored(row:GameSessionEntity)=SessionCodec.decode(row.state).also {
  require(it.id==row.id && it.puzzleId==row.puzzleId && it.revision==row.revision)
 }
 private fun validateRecord(state:GameSession,row:LocalPuzzleEntity):PuzzleDefinition {
  require(row.packId==state.packId && row.puzzleId==state.puzzleId && row.revision==state.revision)
  val p=json.decodeFromString<PuzzleDefinition>(row.definition)
  require(p.puzzleId==row.puzzleId && p.revision==row.revision && registry.plugin(p.type).validatePuzzle(p).valid)
  require(registry.plugin(p.type).validateSession(p,state).valid)
  return p
 }
 override val progress=combine(dao.observeProgress(),dao.observeAchievements()) { rows,achievements ->
  Progress(rows.sumOf { it.bestScore },rows.count { it.completed },rows.sumOf { it.xp },
   rows.groupBy { it.gameType }.map { (type,items)->GameProgress(type,items.sumOf { it.bestScore },items.count { it.completed },items.sumOf { it.attempts },items.sumOf { it.xp }) },achievements.map { it.id })
 }
 override val packs=dao.observePacks().map { rows->rows.map { InstalledPack(it.packId,it.version,it.bytes,it.fixtureOnly) } }
 override suspend fun initialize()=withContext(Dispatchers.IO) { initializationMutex.withLock {
  if(initialized) return@withLock
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
 override suspend fun start(puzzleId:String):String = db.withTransaction {
  val row=dao.latestPuzzle(puzzleId) ?: error("Content missing")
  val p=json.decodeFromString<PuzzleDefinition>(row.definition)
  val pack=dao.pack(row.packId) ?: error("Pack missing")
  val session=registry.plugin(p.type).createSession(p,UUID.randomUUID().toString()).copy(packId=pack.packId,packVersion=pack.version)
  dao.save(GameSessionEntity(session.id,p.puzzleId,p.revision,SessionCodec.encode(session),System.currentTimeMillis()))
  session.id
 }
 override suspend fun act(sessionId:String,action:PlayerAction):GameSession = db.withTransaction {
  val row=dao.session(sessionId) ?: error("Session missing")
  val pRow=dao.puzzle(row.puzzleId,row.revision) ?: error("Pinned content missing")
  val p=json.decodeFromString<PuzzleDefinition>(pRow.definition)
  val old=decodeStored(row)
  validateRecord(old,pRow)
  require(pRow.packId==old.packId && dao.pinnedPack(old.packId,old.packVersion)!=null) { "Pinned pack version missing" }
  val next=registry.plugin(p.type).evaluate(p,old,action)
  require(registry.plugin(p.type).validateSession(p,next).valid)
  if(next==old) return@withTransaction old
  dao.save(row.copy(state=SessionCodec.encode(next),updatedAt=System.currentTimeMillis()))
  if(next.status in setOf(SessionStatus.WON,SessionStatus.LOST)) {
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
