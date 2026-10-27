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
 private fun open() { db=Room.databaseBuilder(context,LughzdanDatabase::class.java,filename).allowMainThreadQueries().build() }
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
  val future=SessionCodec.encode(SessionCodec.decode(row.state).copy(stateVersion=2))
  db.games().save(row.copy(state=future))
  repository.initialize()
  assertTrue(repository.sessions.first().isEmpty())
  assertEquals(id,repository.unavailableSessions.first().single().id)
  assertEquals(future,db.games().session(id)!!.state)
 }
 @Test fun replacedPackCannotReinterpretPinnedSession()=runBlocking {
  val repository=LocalGameRepository(context,db,true);val id=repository.start("p")
  db.games().install(InstalledContentPackEntity("fixture",8,"changed",1,true))
  assertTrue(repository.sessions.first().isEmpty())
  assertEquals(id,repository.unavailableSessions.first().single().id)
  try { repository.act(id,PlayerAction.Select("select","a"));fail("Accepted replaced pack") } catch(_:IllegalArgumentException) { }
  assertEquals(7,SessionCodec.decode(db.games().session(id)!!.state).packVersion)
 }
}
