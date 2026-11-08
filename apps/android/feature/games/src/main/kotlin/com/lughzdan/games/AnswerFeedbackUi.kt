package com.lughzdan.games

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.lughzdan.engine.*

internal val AnswerMark.label:String get()=when(this) {
 AnswerMark.CORRECT->"صحيحة";AnswerMark.INCORRECT->"غير صحيحة"
 AnswerMark.SELECTED->"محددة، لم تُصحح بعد";AnswerMark.NEUTRAL->"لم تُحدد"
}
@Composable internal fun feedbackBackground(mark:AnswerMark):Color {
 val dark=MaterialTheme.colorScheme.surface.luminance()<0.5f
 return when(mark) {
  AnswerMark.CORRECT->if(dark)Color(0xFF123F30) else Color(0xFFDDF5E8)
  AnswerMark.INCORRECT->MaterialTheme.colorScheme.errorContainer
  AnswerMark.SELECTED->MaterialTheme.colorScheme.primaryContainer
  AnswerMark.NEUTRAL->MaterialTheme.colorScheme.surface
 }
}
@Composable internal fun feedbackForeground(mark:AnswerMark):Color = when(mark) {
 AnswerMark.CORRECT->if(MaterialTheme.colorScheme.surface.luminance()<0.5f)Color(0xFFA4F4C6) else Color(0xFF135638)
 AnswerMark.INCORRECT->MaterialTheme.colorScheme.onErrorContainer
 AnswerMark.SELECTED->MaterialTheme.colorScheme.onPrimaryContainer
 AnswerMark.NEUTRAL->MaterialTheme.colorScheme.onSurface
}
@Composable internal fun FeedbackCard(correct:Boolean,message:String) {
 val mark=if(correct)AnswerMark.CORRECT else AnswerMark.INCORRECT
 Surface(color=feedbackBackground(mark),contentColor=feedbackForeground(mark),shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth()) {
  Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(if(correct)"✓" else "×",style=MaterialTheme.typography.titleMedium)
   Text(message,style=MaterialTheme.typography.titleMedium)
  }
 }
}
@Composable internal fun ChoiceAnswers(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 val terminal=s.status in setOf(SessionStatus.WON,SessionStatus.LOST)
 SessionPresentation.shuffled(p.payload.options,s,"options").forEach {o->
  val mark=AnswerFeedback.option(p,s,o.id)
  val background by animateColorAsState(feedbackBackground(mark),tween(200),label="answer background")
  val foreground by animateColorAsState(feedbackForeground(mark),tween(200),label="answer text")
  val prefix=when(mark){AnswerMark.CORRECT->"✓ ";AnswerMark.INCORRECT->"× ";AnswerMark.SELECTED->"● ";else->""}
  OutlinedButton(onClick={if(!terminal)send(PlayerAction.Select(java.util.UUID.randomUUID().toString(),o.id))},enabled=!busy&&!terminal,
   colors=ButtonDefaults.outlinedButtonColors(containerColor=background,contentColor=foreground,disabledContainerColor=feedbackBackground(mark),disabledContentColor=feedbackForeground(mark)),
   modifier=Modifier.testTag("answer-${o.id}").fillMaxWidth().heightIn(min=56.dp).semantics {stateDescription=mark.label}) {
   Text(prefix+o.text,style=MaterialTheme.typography.titleMedium)
  }
 }
}
