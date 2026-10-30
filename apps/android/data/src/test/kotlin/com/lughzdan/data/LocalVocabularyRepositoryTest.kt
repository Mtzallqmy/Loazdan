package com.lughzdan.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lughzdan.db.*
import com.lughzdan.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class LocalVocabularyRepositoryTest {
 private lateinit var context:Context
 private lateinit var db:LughzdanDatabase
 private val filename="personal-words-test.db"
 @Before fun setup() {context=ApplicationProvider.getApplicationContext();context.deleteDatabase(filename);open()}
 private fun open() {db=Room.databaseBuilder(context,LughzdanDatabase::class.java,filename).addMigrations(LughzdanDatabase.MIGRATION_1_2,LughzdanDatabase.MIGRATION_2_3,LughzdanDatabase.MIGRATION_3_4).allowMainThreadQueries().build()}
 @After fun close() {db.close();context.deleteDatabase(filename)}
 @Test fun immutableOriginalSnapshotAndPersonalStateSurvivePackRemovalAndReopen()=runBlocking {
  var repository=LocalVocabularyRepository(db)
  val s=VocabularySnapshot("sense-1","كِتابٌ","أصل المعنى","source-id","page:12","كتب","قراءة",listOf("game"),"draft",true)
  repository.save(s);repository.setFavorite(s.id,true);repository.recordReview(s.id);repository.recordReview(s.id)
  // Same source identity cannot rewrite a player's original saved content or reset counters.
  repository.save(s.copy(word="changed",meaning="replacement",fixtureOnly=false,contentStatus="approved"))
  db.games().removeContent("pack");db.content().deleteMembers("pack");db.content().deleteInstalled("pack")
  db.close();open();repository=LocalVocabularyRepository(db)
  val saved=repository.entries.first().single()
  assertEquals(s,saved.snapshot);assertTrue(saved.favorite);assertEquals(2,saved.reviewCount);assertNotNull(saved.lastReviewedAt)
  repository.setFavorite(s.id,false);assertFalse(repository.entries.first().single().favorite)
  repository.remove(s.id);assertTrue(repository.entries.first().isEmpty())
 }
 @Test fun manualWordsRetainExactInputAndAreAlwaysUnreviewedUserNotes()=runBlocking {
  val repository=LocalVocabularyRepository(db)
  repository.addManual(" كِتابٌ "," تعريفي الشخصي ","كتب","قراءة")
  val saved=repository.entries.first().single()
  assertEquals(" كِتابٌ ",saved.snapshot.word);assertEquals(" تعريفي الشخصي ",saved.snapshot.meaning)
  assertEquals("user_note",saved.snapshot.source);assertEquals("unreviewed",saved.snapshot.contentStatus)
  assertFalse(saved.snapshot.fixtureOnly);assertEquals(0,saved.reviewCount);assertTrue(saved.snapshot.linkedGameIds.isEmpty())
  repository.recordReview(saved.snapshot.id)
  assertEquals("unreviewed",repository.entries.first().single().snapshot.contentStatus)
  assertTrue(repository.catalog.first().isEmpty())
 }
 @Test fun concurrentReviewUpdatesAreNotLost()=runBlocking {
  val repository=LocalVocabularyRepository(db);repository.addManual("كلمة","معنى")
  val id=repository.entries.first().single().snapshot.id
  kotlinx.coroutines.coroutineScope {repeat(20){launch {repository.recordReview(id)}}}
  assertEquals(20,repository.entries.first().single().reviewCount)
 }
}
