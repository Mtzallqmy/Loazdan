package com.lughzdan.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lughzdan.db.LughzdanDatabase
import com.lughzdan.data.LocalVocabularyRepository
import com.lughzdan.domain.VocabularySnapshot
import com.lughzdan.games.VocabularyScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],application=android.app.Application::class)
class VocabularyUiTest {
 @get:Rule val compose=createComposeRule()
 private lateinit var db:LughzdanDatabase
 @After fun close(){db.close()}
 @Test fun manualAddSearchFavoriteReviewAndProvenanceUseRealRoom() {
  val context=ApplicationProvider.getApplicationContext<android.content.Context>()
  db=Room.inMemoryDatabaseBuilder(context,LughzdanDatabase::class.java).allowMainThreadQueries().build()
  val repository=LocalVocabularyRepository(db)
  compose.setContent {VocabularyScreen(repository,{})}
  compose.onNodeWithText("إضافة مفردة").performScrollTo().performClick()
  compose.onNodeWithTag("manual-word").performTextInput("كِتابٌ")
  compose.onNodeWithTag("manual-meaning").performTextInput("تعريف شخصي")
  compose.onNodeWithText("حفظ المفردة").performClick()
  compose.waitUntil(10_000) {compose.onAllNodesWithText("كِتابٌ").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("إضافة للمفضلة").performScrollTo().performClick()
  compose.onNodeWithText("راجعت هذه المفردة").performScrollTo().performClick()
  compose.waitUntil(10_000) {compose.onAllNodesWithText("المراجعات اليدوية: 1").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag("vocabulary-search").performTextInput("لا تطابق")
  compose.onNodeWithText("لا توجد مفردات محفوظة تطابق البحث.").assertExists()
  compose.onNodeWithTag("vocabulary-search").performTextClearance()
  compose.onNodeWithText("كِتابٌ").performScrollTo().performClick()
  compose.onNodeWithText("ملاحظة شخصية غير مراجعة؛ ليست محتوى علميًا معتمدًا.").assertExists()
  runBlocking {val saved=repository.entries.first().single();assertTrue(saved.favorite);assertEquals(1,saved.reviewCount);assertEquals("unreviewed",saved.snapshot.contentStatus)}
 }
 @Test fun savedSnapshotShowsExactProvenanceAndOpensLinkedGame() {
  val context=ApplicationProvider.getApplicationContext<android.content.Context>()
  db=Room.inMemoryDatabaseBuilder(context,LughzdanDatabase::class.java).allowMainThreadQueries().build()
  val repository=LocalVocabularyRepository(db)
  runBlocking {repository.save(VocabularySnapshot("s","قريب","معنى أصلي","مرجع","موضع:٣",linkedGameIds=listOf("game-id"),contentStatus="draft",fixtureOnly=true))}
  var opened=""
  compose.setContent {VocabularyScreen(repository,{opened=it})}
  compose.waitUntil(10_000) {compose.onAllNodesWithText("قريب").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("قريب").performScrollTo().performClick()
  compose.onNodeWithText("المصدر: مرجع\nالموضع: موضع:٣").assertExists()
  compose.onNodeWithText("الحالة: draft · محتوى اختبار").assertExists()
  compose.onNodeWithText("فتح اللعبة المرتبطة: game-id").performClick()
  assertEquals("game-id",opened)
 }
}
