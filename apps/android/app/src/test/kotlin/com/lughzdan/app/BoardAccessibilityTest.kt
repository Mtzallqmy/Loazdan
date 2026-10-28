package com.lughzdan.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.lughzdan.engine.*
import com.lughzdan.games.GameBoard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class BoardAccessibilityTest {
 @get:Rule val compose=createComposeRule()
 private fun puzzle(type:String,payload:ChoicePayload)=PuzzleDefinition(1,"board",1,type,type,"test",1,1,"arabic_basic_v1","test","draft",true,listOf("synthetic-test"),"Synthetic test explanation",emptyList(),payload)
 private val crossword=puzzle("crossword",ChoicePayload(rows=3,cols=3,entries=listOf(
  CrosswordEntry("a",0,1,"across",listOf("ب","ا"),"الأول","source"),
  CrosswordEntry("b",0,0,"down",listOf("ا","ب"),"الثاني","source"))))
 @Test fun crossingSelectionAndDirectionSurviveRecreation() {
  val restoration=StateRestorationTester(compose)
  restoration.setContent { MaterialTheme { Column {GameBoard(crossword,GameSession("s","board",1),false){}} } }
  compose.onNodeWithContentDescription("خلية 0 0").performClick()
  compose.onNodeWithText("تغيير الاتجاه عند التقاطع").performClick()
  restoration.emulateSavedInstanceStateRestore()
  compose.onNodeWithContentDescription("خلية 0 0").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"الخلية المحددة"))
  compose.onNodeWithText("الكلمة النشطة: الثاني · عمودي").assertExists()
  compose.onNodeWithText("1. الأول · أفقي",substring=true).assertExists()
 }
 @Test fun darkSmallScreenWithLargeTextKeepsGridReachable() { largeTextBoard(true) }
 @Test fun lightSmallScreenWithLargeTextKeepsGridReachable() { largeTextBoard(false) }
 private fun largeTextBoard(dark:Boolean) {
  compose.setContent {
   val density=LocalDensity.current.density
   CompositionLocalProvider(LocalDensity provides Density(density,2f)) {
    MaterialTheme(colorScheme=if(dark)darkColorScheme() else lightColorScheme()) {
     Column(Modifier.width(280.dp).verticalScroll(rememberScrollState())) {GameBoard(crossword,GameSession("s","board",1),false){}}
    }
   }
  }
  compose.onNodeWithContentDescription("خلية 0 0").performScrollTo().assertIsDisplayed().performClick()
  compose.onNodeWithContentDescription("خلية 0 0").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"الخلية المحددة"))
 }
 @Test fun foundSearchPathRemainsMarkedAlongsideNextSelection() {
  val p=puzzle("wordsearch",ChoicePayload(grid=listOf(listOf("ب","ا"),listOf("ا","ب")),words=listOf(SearchWord("w","با",listOf(Cell(0,0),Cell(0,1))))))
  compose.setContent {MaterialTheme {Column {GameBoard(p,GameSession("s","board",1,foundWordIds=setOf("w")),false){}}}}
  compose.onNodeWithContentDescription("بحث 1 0").performClick()
  compose.onNodeWithContentDescription("بحث 0 0").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"حرف في كلمة مكتشفة"))
  compose.onNodeWithText("الكلمات المكتشفة: 1 / 1").assertExists()
 }
 @Test fun markedExactTokenCanBeEnteredAndSubmittedThroughEngine() {
  val p=crossword.copy(normalization="exact_v1",sourceRefs=listOf("source"),payload=crossword.payload.copy(rows=3,cols=3,entries=listOf(
   CrosswordEntry("a",0,1,"across",listOf("قَ","ا"),"الأول","source"),
   CrosswordEntry("b",0,0,"down",listOf("ا","پ"),"الثاني","source"))))
  val engine=CrosswordPlugin()
  val state=mutableStateOf(engine.createSession(p,"s").copy(cells=mapOf("0,0" to "ا","1,0" to "پ")))
  compose.setContent {MaterialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
   GameBoard(p,state.value,false){state.value=engine.evaluate(p,state.value,it)}
   Button(onClick={state.value=engine.evaluate(p,state.value,PlayerAction.Submit("submit"))}){Text("اختبار الإرسال")}
  }}}
  compose.onNodeWithText("حرف الخلية").performScrollTo().performTextInput("قَ")
  compose.onNodeWithText("إدخال الحرف").performScrollTo().performClick()
  compose.onNodeWithText("اختبار الإرسال").performScrollTo().performClick()
  compose.runOnIdle {org.junit.Assert.assertEquals(SessionStatus.WON,state.value.status)}
 }
 @Test fun builderCountsOnlyDistinctAcceptedAnswersAsDiscoveries() {
  val p=puzzle("anagram",ChoicePayload(tiles=listOf("ب","ا"),acceptedAnswers=listOf("با")))
  compose.setContent {MaterialTheme {Column {
   GameBoard(p,GameSession("s","board",1,answerHistory=listOf("اب","با","بَا")),false){}
  }}}
  compose.onNodeWithText("الإجابات المكتشفة: 1 / 1").assertExists()
  compose.onNodeWithText("المحاولات السابقة غير الصحيحة: 1").assertExists()
  compose.onNodeWithText("× اب").assertExists()
  compose.onNodeWithText("✓ اب").assertDoesNotExist()
 }

}
