package com.lughzdan.engine

import org.junit.Assert.*
import org.junit.Test

class AnswerFeedbackTest {
 private fun puzzle(type:String,payload:ChoicePayload)=PuzzleDefinition(1,"p",1,"g",type,"words",1,1,"arabic_basic_v1","t","draft",true,listOf("s"),"explanation",emptyList(),payload)
 @Test fun selectionNeverClaimsCorrectnessBeforeSubmission() {
  val p=puzzle("choice",ChoicePayload("q",listOf(Option("a","A"),Option("b","B")),"a"))
  val engine=ChoicePlugin();val initial=engine.createSession(p,"s")
  val selected=engine.evaluate(p,initial,PlayerAction.Select("pick","a"))
  assertEquals(AnswerMark.SELECTED,AnswerFeedback.option(p,selected,"a"))
  assertNull(AnswerFeedback.cue(initial,selected))
  val won=engine.evaluate(p,selected,PlayerAction.Submit("submit"))
  assertEquals(AnswerMark.CORRECT,AnswerFeedback.option(p,won,"a"))
  assertEquals(FeedbackCue.SUCCESS,AnswerFeedback.cue(selected,won))
  assertNull(AnswerFeedback.cue(won,SessionCodec.decode(SessionCodec.encode(won))))
  assertNull(AnswerFeedback.cue(won,engine.evaluate(p,won,PlayerAction.Submit("submit"))))
  val wrong=engine.evaluate(p,engine.evaluate(p,initial,PlayerAction.Select("b","b")),PlayerAction.Submit("bad"))
  assertEquals(AnswerMark.INCORRECT,AnswerFeedback.option(p,wrong,"b"))
  assertEquals(AnswerMark.NEUTRAL,AnswerFeedback.option(p,wrong,"a"))
  assertEquals(FeedbackCue.RETRY,AnswerFeedback.cue(initial,wrong))
 }
 @Test fun crosswordFeedbackIsSubmittedDurableAndClearedByEditing() {
  val p=puzzle("crossword",ChoicePayload(rows=3,cols=3,entries=listOf(
   CrosswordEntry("a",0,1,"across",listOf("ب","ت"),"q","s"),
   CrosswordEntry("b",0,1,"down",listOf("ب","ا"),"q","s"))))
  val engine=CrosswordPlugin();var s=engine.createSession(p,"cross")
  for((key,value) in mapOf("0,1" to "بَ","0,0" to "س","1,1" to "ا")) {
   val parts=key.split(',');s=engine.evaluate(p,s,PlayerAction.InputCell(key,parts[0].toInt(),parts[1].toInt(),value))
  }
  assertEquals(AnswerMark.NEUTRAL,AnswerFeedback.cell(p,s,0,1))
  s=engine.evaluate(p,s,PlayerAction.Submit("check"))
  s=SessionCodec.restore(SessionCodec.encode(s))
  assertTrue(engine.validateSession(p,s).valid)
  assertEquals(AnswerMark.CORRECT,AnswerFeedback.cell(p,s,0,1))
  assertEquals(AnswerMark.INCORRECT,AnswerFeedback.cell(p,s,0,0))
  s=engine.evaluate(p,s,PlayerAction.Resume("resume"))
  s=engine.evaluate(p,s,PlayerAction.InputCell("edit",0,0,"ت"))
  assertEquals(AnswerMark.NEUTRAL,AnswerFeedback.cell(p,s,0,0))
  s=engine.evaluate(p,s,PlayerAction.Submit("win"))
  assertEquals(SessionStatus.WON,s.status)
  assertEquals(AnswerMark.CORRECT,AnswerFeedback.cell(p,s,0,0))
 }
 @Test fun discoveriesSoundOnlyOnNewProgress() {
  val before=GameSession("s","p",1)
  val after=before.copy(foundWordIds=setOf("word"))
  assertEquals(FeedbackCue.DISCOVERY,AnswerFeedback.cue(before,after))
  assertNull(AnswerFeedback.cue(after,after.copy(selectedPath=listOf(Cell(0,0)))))
  assertNull(AnswerFeedback.cue(after,after.copy(id="other")))
 }
}
