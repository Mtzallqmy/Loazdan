package com.lughzdan.data
import android.content.Context
import androidx.room.withTransaction
import com.lughzdan.db.*
import com.lughzdan.domain.*
import com.lughzdan.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

class LocalGameRepository(private val context:Context,private val db:LughzdanDatabase,private val allowFixtures:Boolean):ProgressRepository {
 private val dao=db.games()
 private val json=Json { encodeDefaults=true }
 private val registry=GameRegistry()
 override val puzzles=dao.observePuzzles().map { rows->rows.map { json.decodeFromString<PuzzleDefinition>(it.definition) } }
 override val sessions=combine(dao.observeSessions(),puzzles) { rows,items->rows.mapNotNull { row -> items.find { it.puzzleId==row.puzzleId && it.revision==row.revision }?.let { PlayRecord(SessionCodec.decode(row.state),it) } } }
 override val progress=dao.observeProgress().map { rows->Progress(rows.sumOf { it.bestScore },rows.count { it.completed }) }
 override val packs=dao.observePacks().map { rows->rows.map { InstalledPack(it.packId,it.version,it.bytes,it.fixtureOnly) } }
 override suspend fun initialize()=withContext(Dispatchers.IO) {
  // Release has no draft asset; empty content is explicit, never silently replaced by fixtures.
  if (!context.assets.list("").orEmpty().contains("bundled-manifest.json")) return@withContext
  val manifest=json.decodeFromString<BundledManifest>(context.assets.open("bundled-manifest.json").bufferedReader().use { it.readText() })
  val bytes=context.assets.open(manifest.path).use { it.readBytes() }
  BundledPackVerifier.verify(manifest,bytes,allowFixtures)
  val items=bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.map { line->
   json.decodeFromString<PuzzleDefinition>(line).also { require(registry.plugin(it.type).validatePuzzle(it).valid); require(!it.fixtureOnly || allowFixtures) }
  }.toList()
  require(items.map { it.puzzleId to it.revision }.distinct().size==items.size)
  db.withTransaction {
   val installed=dao.pack(manifest.packId)
   if(installed==null) {
    dao.insertPuzzles(items.mapIndexed { index,p->LocalPuzzleEntity(p.puzzleId,p.revision,manifest.packId,json.encodeToString(p),index) })
    dao.install(InstalledContentPackEntity(manifest.packId,manifest.version,manifest.sha256,manifest.bytes,manifest.fixtureOnly))
   } else require(installed.sha256==manifest.sha256 && installed.version==manifest.version) { "Bundled pack upgrade requires an explicit migration" }
  }
 }
 override suspend fun start(puzzleId:String):String = db.withTransaction {
  val row=dao.puzzles().filter { it.puzzleId==puzzleId }.maxByOrNull { it.revision } ?: error("Content missing")
  val p=json.decodeFromString<PuzzleDefinition>(row.definition)
  val session=registry.plugin(p.type).createSession(p,UUID.randomUUID().toString())
  dao.save(GameSessionEntity(session.id,p.puzzleId,p.revision,SessionCodec.encode(session),System.currentTimeMillis()))
  session.id
 }
 override suspend fun act(sessionId:String,action:PlayerAction) = db.withTransaction {
  val row=dao.session(sessionId) ?: error("Session missing")
  val pRow=dao.puzzle(row.puzzleId,row.revision) ?: error("Pinned content missing")
  val p=json.decodeFromString<PuzzleDefinition>(pRow.definition)
  val old=SessionCodec.decode(row.state)
  val next=registry.plugin(p.type).evaluate(p,old,action)
  if(next==old) return@withTransaction
  dao.save(row.copy(state=SessionCodec.encode(next),updatedAt=System.currentTimeMillis()))
  if(next.status in setOf(SessionStatus.WON,SessionStatus.LOST)) {
   val receipt=dao.enqueue(SyncOperationEntity(UUID.randomUUID().toString(),sessionId,"complete_session",SessionCodec.encode(next)))
   if(receipt!=-1L) {
    val prior=dao.progress(p.puzzleId)
    dao.saveProgress(PlayerProgressEntity(p.puzzleId,maxOf(prior?.bestScore?:0,next.score),(prior?.completed?:false)||next.status==SessionStatus.WON,(prior?.attempts?:0)+1))
    if(next.status==SessionStatus.WON) dao.award(AchievementEntity("first_win",System.currentTimeMillis()))
   }
  }
 }
}
