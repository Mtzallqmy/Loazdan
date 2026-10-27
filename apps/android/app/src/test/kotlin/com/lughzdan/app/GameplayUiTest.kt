package com.lughzdan.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.lughzdan.db.LughzdanDatabase
import com.lughzdan.data.LocalGameRepository
import com.lughzdan.games.GameApp
import com.lughzdan.games.GameViewModel
import com.lughzdan.preferences.SettingsStore
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class GameplayUiTest {
 @get:Rule val compose=createComposeRule()
 private lateinit var db:LughzdanDatabase
 @After fun close() { db.close() }
 @Test fun bundledQuestionCanBeSolvedWithoutNetwork() {
  val context=ApplicationProvider.getApplicationContext<android.content.Context>()
  db=Room.inMemoryDatabaseBuilder(context,LughzdanDatabase::class.java).allowMainThreadQueries().build()
  val vm=GameViewModel(LocalGameRepository(context,db,true),SettingsStore(context))
  compose.setContent { GameApp(vm) }
  compose.waitUntil(20_000) { compose.onAllNodesWithText("ابدأ تحدي المفردات").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("ابدأ تحدي المفردات").performClick()
  compose.onNodeWithText("لغز 1 · الصعوبة 1").performClick()
  compose.waitUntil(10_000) { compose.onAllNodesWithText("بعيد").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("بعيد").performClick()
  compose.waitUntil(10_000) { compose.onAllNodesWithText("تحقق من الإجابة").filter(isEnabled()).fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("تحقق من الإجابة").performScrollTo().performClick()
  compose.waitUntil(10_000) { compose.onAllNodesWithText("إجابة صحيحة! +100 نقطة").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("إجابة صحيحة! +100 نقطة").assertExists()
 }
}
