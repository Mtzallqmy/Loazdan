package com.lughzdan.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lughzdan.engine.*
import com.lughzdan.games.LiteraryWordBoard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class LiteraryWordsUiTest {
 @get:Rule val compose=createComposeRule()
 private fun puzzle(variant:String)=PuzzleDefinition(1,"p",1,"g","text_answer","words",1,1,"arabic_basic_v1","test","draft",true,listOf("source"),"Test explanation",listOf(Hint("letter-2","step","الحرف الثالث: م",10)),ChoicePayload(prompt="أداة للكتابة",acceptedAnswers=listOf("قلم"),variant=variant,mode="riddles",fixedSlots=if(variant==LiteraryWords.COMPLETE)listOf(FixedSlot(0,"ق"))else emptyList(),sourceLabel="Synthetic UI test"))
 @Test fun revealedSlotAndRepeatedEditsReachTheEngineWithoutMovingLetters() {
  val p=puzzle(LiteraryWords.COMPLETE);val engine=TextAnswerPlugin();val state=mutableStateOf(engine.createSession(p,"ui"))
  compose.setContent {MaterialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
   LiteraryWordBoard(p,state.value,false){state.value=engine.evaluate(p,state.value,it)}
  }}}
  compose.onNodeWithContentDescription("خانة 1").assertIsNotEnabled()
  compose.onNodeWithContentDescription("حرف الديوان ل").performScrollTo().performClick()
  compose.onNodeWithContentDescription("حرف الديوان م").performScrollTo().performClick()
  compose.runOnIdle {assertEquals("قلم",state.value.answerInput)}
  compose.onNodeWithContentDescription("خانة 2").performScrollTo().performClick()
  compose.onNodeWithText("مسح الخانة المحددة").performScrollTo().performClick()
  compose.runOnIdle {assertEquals("ق م",state.value.answerInput)}
 }
 @Test fun riddleAcceptsTypedArabicAndIgnoresDiacritics() {
  val p=puzzle(LiteraryWords.RIDDLE);val engine=TextAnswerPlugin();val state=mutableStateOf(engine.createSession(p,"riddle"))
  compose.setContent {MaterialTheme {Column {LiteraryWordBoard(p,state.value,false){state.value=engine.evaluate(p,state.value,it)}}}}
  compose.onNodeWithTag("literary-answer").performTextInput("قَلَم")
  compose.runOnIdle {assertEquals(SessionStatus.WON,engine.evaluate(p,state.value,PlayerAction.Submit("submit")).status)}
 }
}
