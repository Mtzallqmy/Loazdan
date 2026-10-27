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
 @Before fun setup() { context=ApplicationProvider.getApplicationContext();context.deleteDatabase(filename);open() }
 private fun open() { db=Room.databaseBuilder(context,LughzdanDatabase::class.java,filename).allowMainThreadQueries().build() }
 @After fun close() { db.close();context.deleteDatabase(filename) }
 @Test fun solveCloseReopenOfflineAndDeduplicate()=runBlocking {
  db.games().insertPuzzles(listOf(LocalPuzzleEntity("p",1,"fixture",SessionCodec.json.encodeToString(p),0)))
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
  db.games().insertPuzzles(listOf(LocalPuzzleEntity("p",1,"fixture",SessionCodec.json.encodeToString(p),0)))
  val repository=LocalGameRepository(context,db,true);val id=repository.start("p")
  repository.act(id,PlayerAction.Select("select","b"));db.close();open()
  assertEquals("b",LocalGameRepository(context,db,true).sessions.first().single().session.selectedId)
 }
}
