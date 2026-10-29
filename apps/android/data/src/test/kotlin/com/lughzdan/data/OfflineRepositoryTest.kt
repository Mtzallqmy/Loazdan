package com.lughzdan.data
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lughzdan.db.*
import com.lughzdan.engine.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class OfflineRepositoryTest {
 private lateinit var context:Context
 private lateinit var db:LughzdanDatabase
 private val filename="offline-test.db"
 private val p=PuzzleDefinition(1,"p",1,"g","choice","words",1,1,"arabic_basic_v1","t","draft",true,listOf("fixture"),"explain",emptyList(),ChoicePayload("q",listOf(Option("a","a"),Option("b","b"),Option("c","c"),Option("d","d")),"a"))
 @Before fun setup() { context=ApplicationProvider.getApplicationContext();context.deleteDatabase(filename);open();runBlocking {
  db.games().install(InstalledContentPackEntity("fixture",7,"fixture-digest",1,true))
  db.games().insertPuzzles(listOf(LocalPuzzleEntity("p",1,"fixture",SessionCodec.json.encodeToString(p),0)))
 } }
 private fun open() { db=Room.databaseBuilder(context,LughzdanDatabase::class.java,filename).addMigrations(LughzdanDatabase.MIGRATION_1_2,LughzdanDatabase.MIGRATION_2_3).allowMainThreadQueries().build() }
 @After fun close() { db.close();context.deleteDatabase(filename) }
 @Test fun solveCloseReopenOfflineAndDeduplicate()=runBlocking {
  var repository=LocalGameRepository(context,db,true)
  val id=repository.start("p")
  repository.act(id,PlayerAction.Select("select","a"));repository.act(id,PlayerAction.Submit("submit"))
  assertEquals(100,repository.progress.first().totalScore)
  db.close();open();repository=LocalGameRepository(context,db,true)
  assertEquals(SessionStatus.WON,repository.sessions.first().single().session.status)
  repository.act(id,PlayerAction.Submit("submit"))
  assertEquals(1,db.games().operationCount());assertEquals(1,db.games().progress("p")!!.attempts)
  val retry=repository.start("p");repository.act(retry,PlayerAction.Select("s2","a"));repository.act(retry,PlayerAction.Submit("a2"))
  assertEquals(100,repository.progress.first().totalScore)
  db.games().removeContent("fixture")
  assertEquals(100,repository.progress.first().totalScore)
 }
 @Test fun lockedDifficultyCannotBeStartedThroughRepository()=runBlocking {
  val locked=p.copy(puzzleId="locked",difficulty=2)
  db.games().insertPuzzles(listOf(LocalPuzzleEntity("locked",1,"fixture",SessionCodec.json.encodeToString(locked),1)))
  val repository=LocalGameRepository(context,db,true)
  try { repository.start("locked");fail("Locked level was started") } catch(_:IllegalArgumentException) { }
  assertTrue(db.games().storedSessions().isEmpty())
  db.games().saveProgress(PlayerProgressEntity("earned",300,true,1,"choice",300))
  assertNotNull(repository.start("locked"))
 }
 @Test fun midRoundSelectionRestores()=runBlocking {
  val repository=LocalGameRepository(context,db,true);val id=repository.start("p")
  repository.act(id,PlayerAction.Select("select","b"));db.close();open()
  val restored=LocalGameRepository(context,db,true);restored.initialize()
  val saved=restored.sessions.first().single().session
  assertEquals("b",saved.selectedId);assertEquals(SessionStatus.PAUSED,saved.status)
  assertEquals("fixture",saved.packId);assertEquals(7,saved.packVersion)
  restored.act(id,PlayerAction.Submit("paused-submit"))
  assertEquals(0,db.games().operationCount());assertEquals(0,restored.sessions.first().single().session.wrongSubmissions)
  restored.act(id,PlayerAction.Resume("resume"));restored.initialize()
  assertEquals(SessionStatus.ACTIVE,restored.sessions.first().single().session.status)
  restored.act(id,PlayerAction.Submit("submit"))
  assertEquals(1,restored.sessions.first().single().session.wrongSubmissions)
 }
 @Test fun futureStateIsPreservedAndReported()=runBlocking {
  val repository=LocalGameRepository(context,db,true);val id=repository.start("p")
  val row=db.games().session(id)!!
  val future=SessionCodec.encode(SessionCodec.decode(row.state).copy(stateVersion=99))
  db.games().save(row.copy(state=future))
  repository.initialize()
  assertTrue(repository.sessions.first().isEmpty())
  assertEquals(id,repository.unavailableSessions.first().single().id)
  assertEquals(future,db.games().session(id)!!.state)
 }
 @Test fun upgradedPackRetainsPinnedSession()=runBlocking {
  val repository=LocalGameRepository(context,db,true);val id=repository.start("p")
  db.games().install(InstalledContentPackEntity("fixture",8,"changed",1,true))
  assertEquals(id,repository.sessions.first().single().session.id)
  repository.act(id,PlayerAction.Select("select","a"))
  assertEquals(7,SessionCodec.decode(db.games().session(id)!!.state).packVersion)
 }
 @Test fun invalidSavedAnswerIsPreservedWithoutRendering()=runBlocking {
  val repository=LocalGameRepository(context,db,true);val id=repository.start("p")
  val row=db.games().session(id)!!
  val invalid=SessionCodec.encode(SessionCodec.decode(row.state).copy(status=SessionStatus.WON,score=100,submittedIds=listOf("unknown")))
  db.games().save(row.copy(state=invalid));repository.initialize()
  assertTrue(repository.sessions.first().isEmpty())
  assertEquals(id,repository.unavailableSessions.first().single().id)
  assertEquals(invalid,db.games().session(id)!!.state)
 }
 @Test fun foreignPackRowCannotRenderSession()=runBlocking {
  val repository=LocalGameRepository(context,db,true);val id=repository.start("p")
  db.games().removeContent("fixture")
  db.games().insertPuzzles(listOf(LocalPuzzleEntity("p",1,"foreign",SessionCodec.json.encodeToString(p),0)))
  assertTrue(repository.sessions.first().isEmpty())
  assertEquals(id,repository.unavailableSessions.first().single().id)
 }

 @Test fun populatedVersionOneMigratesWithoutLosingPinsOrRewards()=runBlocking {
  db.close();context.deleteDatabase(filename)
  val old=context.openOrCreateDatabase(filename,Context.MODE_PRIVATE,null)
  old.execSQL("CREATE TABLE IF NOT EXISTS `installed_packs` (`packId` TEXT NOT NULL, `version` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `bytes` INTEGER NOT NULL, `fixtureOnly` INTEGER NOT NULL, PRIMARY KEY(`packId`))")
  old.execSQL("CREATE TABLE IF NOT EXISTS `puzzles` (`puzzleId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `packId` TEXT NOT NULL, `definition` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`puzzleId`, `revision`))")
  old.execSQL("CREATE INDEX IF NOT EXISTS `index_puzzles_packId` ON `puzzles` (`packId`)")
  old.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` TEXT NOT NULL, `puzzleId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `state` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
  old.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_updatedAt` ON `sessions` (`updatedAt`)")
  old.execSQL("CREATE TABLE IF NOT EXISTS `progress` (`puzzleId` TEXT NOT NULL, `bestScore` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, PRIMARY KEY(`puzzleId`))")
  old.execSQL("CREATE TABLE IF NOT EXISTS `achievements` (`id` TEXT NOT NULL, `awardedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
  old.execSQL("CREATE TABLE IF NOT EXISTS `sync_operations` (`operationId` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `kind` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`operationId`))")
  old.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_operations_sessionId` ON `sync_operations` (`sessionId`)")
  // Literal Stage02 records deliberately omit every additive Stage03 payload/state field.
  val legacy="""{"id":"legacy","puzzleId":"p","revision":1,"packVersion":1,"packId":"fixture","stateVersion":1,"status":"WON","wrongSubmissions":0,"score":100,"selectedId":"a","usedHintIds":[],"processedActions":["select","submit"],"submittedIds":["a"]}"""
  val legacyDefinition="""{"schemaVersion":1,"puzzleId":"p","revision":1,"gameId":"g","type":"choice","world":"words","difficulty":1,"rulesVersion":1,"normalization":"arabic_basic_v1","title":"t","status":"draft","fixtureOnly":true,"sourceRefs":["fixture"],"explanation":"explain","hints":[],"payload":{"prompt":"q","options":[{"id":"a","text":"a"},{"id":"b","text":"b"},{"id":"c","text":"c"},{"id":"d","text":"d"}],"correctId":"a"}}"""
  old.execSQL("INSERT INTO installed_packs VALUES ('fixture',1,'fixture-digest',1,1)")
  old.execSQL("INSERT INTO puzzles VALUES (?,?,?,?,?)",arrayOf("p",1,"fixture",legacyDefinition,0))
  old.execSQL("INSERT INTO sessions VALUES (?,?,?,?,?)",arrayOf("legacy","p",1,legacy,12))
  old.execSQL("INSERT INTO progress VALUES ('p',100,1,2)")
  old.execSQL("INSERT INTO achievements VALUES ('first_win',123)")
  old.execSQL("INSERT INTO sync_operations VALUES ('receipt','completed-old','complete_session','preserved')")
  old.version=1;old.close();open()
  assertEquals(legacy,db.games().session("legacy")!!.state)
  assertEquals(1,db.games().operationCount())
  assertEquals(1,db.games().pinnedPack("fixture",1)!!.version)
  assertEquals(100,db.games().progress("p")!!.xp)
  assertEquals(2,db.games().progress("p")!!.attempts)
  assertEquals("choice",db.games().progress("p")!!.gameType)
  val added=p.copy(puzzleId="new-puzzle")
  val bytes=(SessionCodec.json.encodeToString(p)+"\n"+SessionCodec.json.encodeToString(added)+"\n").toByteArray()
  val digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  val manifest=BundledManifest(1,"fixture",2,true,"data/test.jsonl",bytes.size.toLong(),digest)
  val repository=LocalGameRepository(context,db,true,manifest to bytes)
  repository.initialize()
  assertEquals(legacy,db.games().session("legacy")!!.state)
  assertEquals(legacyDefinition,db.games().puzzle("p",1)!!.definition)
  assertEquals("a",repository.sessions.first().single().session.selectedId)
  assertEquals(1,repository.sessions.first().single().session.packVersion)
  assertEquals(2,db.games().pack("fixture")!!.version)
  assertNotNull(db.games().puzzle("new-puzzle",1))
  assertEquals(100,repository.progress.first().totalXp)
  assertEquals(listOf("first_win"),repository.progress.first().achievements)
  assertEquals(1,db.games().operationCount())

 }
 @Test fun boardSessionsRoundTripAndRewardsRemainPerGame()=runBlocking {
  val order=p.copy(puzzleId="order-p",type="order",payload=ChoicePayload(items=listOf(Option("a","A"),Option("b","B")),correctOrder=listOf("b","a")))
  val anagram=p.copy(puzzleId="anagram-p",type="anagram",payload=ChoicePayload(tiles=listOf("ا","ب"),acceptedAnswers=listOf("اب")))
  db.games().insertPuzzles(listOf(order,anagram).mapIndexed { index,item->LocalPuzzleEntity(item.puzzleId,1,"fixture",SessionCodec.json.encodeToString(item),index+1) })
  var repository=LocalGameRepository(context,db,true)
  val orderId=repository.start(order.puzzleId);val anagramId=repository.start(anagram.puzzleId)
  repository.act(orderId,PlayerAction.Reorder("order",listOf("b","a")))
  repository.act(anagramId,PlayerAction.AddTile("tile",0))
  db.close();open();repository=LocalGameRepository(context,db,true);repository.initialize()
  val restored=repository.sessions.first().associateBy { it.session.id }
  assertEquals(listOf("b","a"),restored.getValue(orderId).session.orderedIds)
  assertEquals(listOf(0),restored.getValue(anagramId).session.selectedTileIndices)
  repository.act(orderId,PlayerAction.Resume("resume"));repository.act(orderId,PlayerAction.Submit("finish"))
  repository.act(orderId,PlayerAction.Submit("duplicate"))
  val progress=repository.progress.first()
  assertEquals(100,progress.totalXp)
  assertEquals(100,progress.perGame.single {it.gameType=="order"}.xp)
  assertTrue("first_win_order" in progress.achievements)
  assertEquals(1,db.games().operationCount())
 }

 @Test fun releasePolicyRejectsFixtureWithoutChangingInstalledContent()=runBlocking {
  val bytes=(SessionCodec.json.encodeToString(p)+"\n").toByteArray()
  val digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  // Even a falsely non-fixture manifest cannot admit fixture puzzle records.
  val manifest=BundledManifest(1,"fixture",8,false,"data/test.jsonl",bytes.size.toLong(),digest)
  try { LocalGameRepository(context,db,false,manifest to bytes).initialize();fail("Fixture imported in release") }
  catch(_:IllegalArgumentException) { }
  assertEquals(7,db.games().pack("fixture")!!.version)
  assertEquals(1,db.games().puzzles().size)
 }
 @Test fun conflictingUpgradeLeavesOldPackAndPuzzleIntact()=runBlocking {
  val original=db.games().puzzle("p",1)!!.definition
  val bytes=(SessionCodec.json.encodeToString(p.copy(title="changed revision"))+"\n").toByteArray()
  val digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  val manifest=BundledManifest(1,"fixture",8,true,"data/test.jsonl",bytes.size.toLong(),digest)
  try { LocalGameRepository(context,db,true,manifest to bytes).initialize();fail("Changed immutable revision accepted") }
  catch(_:IllegalArgumentException) { }
  assertEquals(7,db.games().pack("fixture")!!.version)
  assertEquals(original,db.games().puzzle("p",1)!!.definition)
 }

}
