package com.lughzdan.engine

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class LiteraryWordsTest {
 private fun puzzles():List<PuzzleDefinition> {
  val root=generateSequence(File(".").canonicalFile){it.parentFile}.first {File(it,"packs/literary-words-48/puzzles.jsonl").exists()}
  return File(root,"packs/literary-words-48/puzzles.jsonl").readLines().filter {it.isNotBlank()}.map {Json.decodeFromString<PuzzleDefinition>(it)}
 }
 @Test fun allPublishedWordsValidateSolveAndPreserveHintPenalties() {
  val all=puzzles();val engine=TextAnswerPlugin()
  assertEquals(48,all.size);assertEquals(48,all.map {it.puzzleId}.distinct().size)
  assertEquals(mapOf(LiteraryWords.COMPLETE to 24,LiteraryWords.RIDDLE to 24),all.groupingBy {it.payload.variant}.eachCount())
  assertTrue(all.groupingBy {it.payload.mode}.eachCount().values.all {it==8})
  for(p in all) {
   assertEquals(p.puzzleId,emptyList<String>(),engine.validatePuzzle(p).errors)
   var s=engine.createSession(p,p.puzzleId)
   assertTrue(engine.validateSession(p,s).valid)
   if(p.payload.variant==LiteraryWords.COMPLETE) {
    assertFalse(LiteraryWords.complete(p,s))
    assertEquals(s,engine.evaluate(p,s,PlayerAction.Submit("incomplete")))
   }
   val hint=p.hints.first {it.id.startsWith("letter-")}
   s=engine.evaluate(p,s,PlayerAction.UseHint("hint",hint.id))
   val once=s
   s=engine.evaluate(p,s,PlayerAction.UseHint("duplicate",hint.id))
   assertEquals(once,s)
   val revealed=LiteraryWords.revealed(p,s)
   s=SessionCodec.restore(SessionCodec.encode(s))
   assertEquals(revealed,LiteraryWords.revealed(p,s))
   s=engine.evaluate(p,s,PlayerAction.Resume("resume"))
   s=engine.evaluate(p,s,PlayerAction.InputAnswer("answer",p.payload.acceptedAnswers.first()))
   s=engine.evaluate(p,s,PlayerAction.Submit("submit"))
   assertEquals(p.puzzleId,SessionStatus.WON,s.status)
   assertEquals(ScoringEngine.score(p,0,setOf(hint.id)),s.score)
   assertTrue(engine.validateSession(p,s).valid)
   assertEquals(s,engine.evaluate(p,s,PlayerAction.Submit("again")))
  }
 }
 @Test fun completionKeepsSpacesPositionsAndLocksOnlyRevealedLetters() {
  val p=puzzles().first {it.payload.variant==LiteraryWords.COMPLETE && LiteraryWords.answer(it).length>=5}
  val engine=TextAnswerPlugin();var s=engine.createSession(p,"s")
  val length=LiteraryWords.answer(p).length
  val input=" ".repeat(length-1)+"س"
  s=engine.evaluate(p,s,PlayerAction.InputAnswer("last",input))
  assertEquals('س',s.answerInput.last());assertEquals(length,s.answerInput.length)
  assertEquals(' ',s.answerInput[2]);assertEquals(p.payload.fixedSlots.first().token,s.answerInput.first().toString())
  s=engine.evaluate(p,s,PlayerAction.UseHint("reveal",p.hints.first {it.id.startsWith("letter-")}.id))
  val r=LiteraryWords.revealed(p,s)
  s=engine.evaluate(p,s,PlayerAction.InputAnswer("overwrite","س".repeat(length)))
  r.forEach {(i,t)->assertEquals(t,s.answerInput[i].toString())}
 }
}
