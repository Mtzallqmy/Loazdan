package com.lughzdan.engine

enum class AnswerMark { NEUTRAL, SELECTED, CORRECT, INCORRECT }
enum class FeedbackCue { SUCCESS, RETRY, DISCOVERY }

/** Feedback follows accepted engine state, never a tap or a guessed answer. */
object AnswerFeedback {
 fun option(p:PuzzleDefinition,s:GameSession,id:String):AnswerMark = when {
  s.status in setOf(SessionStatus.WON,SessionStatus.LOST) && id==p.payload.correctId -> AnswerMark.CORRECT
  id in s.submittedIds && id!=p.payload.correctId -> AnswerMark.INCORRECT
  id==s.selectedId -> AnswerMark.SELECTED
  else -> AnswerMark.NEUTRAL
 }
 fun cell(p:PuzzleDefinition,s:GameSession,row:Int,col:Int):AnswerMark {
  val key="$row,$col"
  val checked=s.checkedCells[key] ?: return AnswerMark.NEUTRAL
  if(s.cells[key]!=checked)return AnswerMark.NEUTRAL
  val expected=p.payload.entries.firstNotNullOfOrNull { e->
   val index=entryCells(e).indexOf(Cell(row,col));e.tokens.getOrNull(index)
  } ?: return AnswerMark.NEUTRAL
  return if(normalized(p,checked)==normalized(p,expected)) AnswerMark.CORRECT else AnswerMark.INCORRECT
 }
 fun cue(before:GameSession,after:GameSession):FeedbackCue? = when {
  before.id!=after.id -> null
  before.status!=SessionStatus.WON && after.status==SessionStatus.WON -> FeedbackCue.SUCCESS
  after.wrongSubmissions>before.wrongSubmissions -> FeedbackCue.RETRY
  after.foundWordIds.size>before.foundWordIds.size || after.answerHistory.size>before.answerHistory.size -> FeedbackCue.DISCOVERY
  else -> null
 }
}
