package com.lughzdan.engine

import org.junit.Assert.*
import org.junit.Test

class VisualGamesTest {
 private fun image(id:String,role:String)=VisualImage(id,"media/$id.webp","وصف $id",role,"source","PROJECT-ORIGINAL-FIXTURE",100,100,"a".repeat(64))
 private fun puzzle(type:String,b:ChoicePayload)=PuzzleDefinition(1,"p",1,"g",type,"imagination",1,1,"arabic_basic_v1","عنوان","draft",true,listOf("source"),"تفسير",listOf(Hint("hint","step","تلميح",10)),b)
 private fun payload(type:String):ChoicePayload = when(type) {
  "four_pictures" -> ChoicePayload(prompt="سؤال",images=(1..4).map {image("i$it","clue")},tiles=listOf("ب","ب"),acceptedAnswers=listOf("بب"))
  "guess_picture" -> ChoicePayload(prompt="سؤال",images=listOf(image("i","clue")),acceptedAnswers=listOf("باب"),revealRows=2,revealCols=2,revealPenalty=5)
  "symbols" -> ChoicePayload(prompt="سؤال",images=listOf(image("a","symbol"),image("b","symbol")),acceptedAnswers=listOf("باب"),correctImageOrder=listOf("b","a"))
  "shadow" -> ChoicePayload(prompt="سؤال",images=listOf(image("i","clue"),image("a","option"),image("b","option")),correctImageId="a")
  "visual_association" -> ChoicePayload(prompt="سؤال",images=listOf(image("a","left"),image("b","left"),image("x","right"),image("y","right")),visualPairs=listOf(MatchPair("a","x"),MatchPair("b","y")))
  else -> ChoicePayload(prompt="سؤال",images=(1..3).map {image("t$it","term")}+listOf(image("a","option"),image("b","option")),correctImageId="a",logicRule=LogicRule("add",listOf(1,2,3),mapOf("a" to 4,"b" to 5),1))
 }
 @Test fun allSixVisualTypesSolveRestoreAndReplay() {
  assertTrue(GameRegistry().types.containsAll(visualTypes + setOf("choice","crossword","wordsearch","anagram","matching","order")))
  for(type in visualTypes) {
   val p=puzzle(type,payload(type));val e=GameRegistry().plugin(type)
   assertTrue("$type ${e.validatePuzzle(p).errors}",e.validatePuzzle(p).valid)
   var s=e.createSession(p,"session");assertEquals(2,s.stateVersion)
   val moves=when(type) {
    "four_pictures" -> listOf(PlayerAction.AddTile("a",0),PlayerAction.AddTile("b",1),PlayerAction.Undo("u"),PlayerAction.AddTile("c",1),PlayerAction.Shuffle("shuffle",1))
    "guess_picture" -> listOf(PlayerAction.RevealRegion("reveal",0,1),PlayerAction.InputAnswer("answer","بَاب"))
    "symbols" -> listOf(PlayerAction.ReorderImages("order",listOf("b","a")),PlayerAction.InputAnswer("answer","باب"))
    "visual_association" -> listOf(PlayerAction.MatchImages("a","a","x"),PlayerAction.MatchImages("b","b","y"))
    else -> listOf(PlayerAction.SelectImage("select","a"))
   }
   moves.forEach {s=e.evaluate(p,s,it);assertEquals(s,e.evaluate(p,s,it))}
   s=SessionCodec.restore(SessionCodec.encode(s));assertEquals(SessionStatus.PAUSED,s.status);assertTrue("$type ${e.validateSession(p,s).errors}",e.validateSession(p,s).valid)
   s=e.evaluate(p,s,PlayerAction.Resume("resume"));s=e.evaluate(p,s,PlayerAction.Submit("win"))
   assertEquals(type,SessionStatus.WON,s.status);assertTrue("$type ${e.validateSession(p,s).errors}",e.validateSession(p,s).valid)
   assertEquals(if(type=="guess_picture")95 else 100,s.score)
   assertEquals(s,e.evaluate(p,s,PlayerAction.Submit("win")))
  }
 }
 @Test fun revealPenaltyIsPermanentUniqueAndBounded() {
  val p=puzzle("guess_picture",payload("guess_picture"));val e=GuessPicturePlugin();var s=e.createSession(p,"s")
  assertEquals(s,e.evaluate(p,s,PlayerAction.RevealRegion("invalid",2,0)))
  s=e.evaluate(p,s,PlayerAction.RevealRegion("first",1,1));s=e.evaluate(p,s,PlayerAction.RevealRegion("second",1,1));assertEquals(setOf(3),s.revealedRegions)
  s=e.evaluate(p,s,PlayerAction.InputAnswer("wrong","خطأ"));s=e.evaluate(p,s,PlayerAction.Submit("attempt"));assertEquals(1,s.wrongSubmissions)
  s=e.evaluate(p,s,PlayerAction.UseHint("hint-action","hint"));s=e.evaluate(p,s,PlayerAction.InputAnswer("right","باب"));s=e.evaluate(p,s,PlayerAction.Submit("win"));assertEquals(75,s.score)
 }
 @Test fun noChangeInputAndReorderAreAcknowledged() {
  val p=puzzle("symbols",payload("symbols"));val e=SymbolsPlugin();var s=e.createSession(p,"s");val a=PlayerAction.ReorderImages("same",listOf("a","b"))
  s=e.evaluate(p,s,a);s=e.evaluate(p,s,PlayerAction.ReorderImages("change",listOf("b","a")));assertEquals(s,e.evaluate(p,s,a))
  val blank=PlayerAction.InputAnswer("blank","");s=e.evaluate(p,s,blank);s=e.evaluate(p,s,PlayerAction.InputAnswer("input","باب"));assertEquals(s,e.evaluate(p,s,blank))
 }
 @Test fun corruptionCannotRestoreValidVisualProgress() {
  for(type in visualTypes) {
   val p=puzzle(type,payload(type));val e=GameRegistry().plugin(type);val s=e.createSession(p,"s")
   for(bad in listOf(s.copy(revealedRegions=setOf(999)),s.copy(selectedImageId="missing"),s.copy(imageOrder=listOf("bad")),s.copy(imageMatches=listOf(MatchPair("bad","worse"))),s.copy(answerInput="\u202E"),s.copy(status=SessionStatus.WON,score=100))) {
    val decoded=runCatching {SessionCodec.restore(SessionCodec.encode(bad))}.getOrNull();assertTrue("$type",decoded==null || !e.validateSession(p,decoded).valid)
   }
  }
 }
 @Test fun provenancePathsDimensionsAndProofRejectTampering() {
  val p=puzzle("visual_logic",payload("visual_logic"));val e=VisualLogicPlugin();val b=p.payload
  for(path in listOf("https://example.com/a.webp","media/../a.webp","/media/a.webp","media/a.svg","media/a.webp?x")) assertFalse(e.validatePuzzle(p.copy(payload=b.copy(images=b.images.map {it.copy(assetPath=path)}))).valid)
  assertFalse(e.validatePuzzle(p.copy(payload=b.copy(images=b.images.map {it.copy(sourceRef="unknown")}))).valid)
  assertFalse(e.validatePuzzle(p.copy(payload=b.copy(images=b.images.map {it.copy(width=0,sha256="bad")}))).valid)
  val proof=b.logicRule!!
  for(rule in listOf(proof.copy(values=listOf(1,2,4)),proof.copy(optionValues=mapOf("a" to 5,"b" to 4)),proof.copy(kind="editorial"),proof.copy(optionValues=mapOf("a" to 4,"b" to 4)))) assertFalse(e.validatePuzzle(p.copy(payload=b.copy(logicRule=rule))).valid)
 }
 @Test fun legacyStateDecodesWithEmptyVisualFields() {
  val s=SessionCodec.decode("""{"id":"old","puzzleId":"p","revision":1}""")
  assertEquals(1,s.stateVersion);assertEquals("",s.answerInput);assertTrue(s.revealedRegions.isEmpty());assertNull(s.selectedImageId)
 }
}
