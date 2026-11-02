package com.lughzdan.engine

internal val visualTypes = setOf("four_pictures", "guess_picture", "shadow", "symbols", "visual_association", "visual_logic")
internal fun answerTextValid(value: String) = value.length <= 2000 && value.none { Character.isISOControl(it) } && runCatching { ArabicText.comparison(value) }.isSuccess
internal fun visualOptions(p: PuzzleDefinition) = p.payload.images.filter { it.role == "option" }.map { it.id }
internal fun imagePermutation(p: PuzzleDefinition, ids: List<String>) = ids.size == p.payload.images.size && ids.toSet() == p.payload.images.map { it.id }.toSet()
internal fun imagePairValid(p: PuzzleDefinition, pair: MatchPair) = p.payload.images.any { it.id == pair.leftId && it.role == "left" } && p.payload.images.any { it.id == pair.rightId && it.role == "right" }
internal fun visualSolved(p: PuzzleDefinition, s: GameSession): Boolean = when(p.type) {
 "four_pictures" -> s.selectedTileIndices.all { it in p.payload.tiles.indices } && p.payload.acceptedAnswers.any { normalized(p,it) == normalized(p,builtAnswer(p,s)) } && s.answerHistory.lastOrNull() == builtAnswer(p,s)
 "guess_picture", "symbols" -> p.payload.acceptedAnswers.any { normalized(p,it) == normalized(p,s.answerInput) } && (p.payload.correctImageOrder.isEmpty() || s.imageOrder == p.payload.correctImageOrder)
 "shadow", "visual_logic" -> s.selectedImageId == p.payload.correctImageId
 "visual_association" -> s.imageMatches.toSet() == p.payload.visualPairs.toSet()
 else -> false
}
/** Typed proof: a constant additive or multiplicative step determines exactly one next value. */
internal fun logicErrors(p: PuzzleDefinition): List<String> = buildList {
 val rule = p.payload.logicRule
 if(rule == null) { add("missing_logic_rule"); return@buildList }
 val ids = visualOptions(p).toSet()
 if(rule.kind !in setOf("add", "multiply") || rule.values.size !in 3..12 || rule.values.any { it !in -1000000..1000000 } || rule.step !in -1000..1000 || (rule.kind == "add" && rule.step == 0) || (rule.kind == "multiply" && rule.step in -1..1)) add("invalid_logic_rule")
 fun next(v:Int):Long = if(rule.kind == "add") v.toLong() + rule.step else v.toLong() * rule.step
 if(rule.values.zipWithNext().any { (a,b) -> next(a) != b.toLong() }) add("unproved_logic_sequence")
 val expected = rule.values.lastOrNull()?.let { next(it) }
 if(rule.optionValues.keys != ids || rule.optionValues.values.any { it !in -1000000..1000000 } || rule.optionValues.values.toSet().size != ids.size || expected == null || rule.optionValues[p.payload.correctImageId]?.toLong() != expected) add("unproved_logic_answer")
 if(p.payload.images.count { it.role == "term" } != rule.values.size) add("logic_term_count")
}
abstract class VisualPlugin(type:String): BoardPlugin(type) {
 override fun errors(p:PuzzleDefinition):List<String> = buildList {
  val b=p.payload; val ids=b.images.map {it.id}
  if(b.prompt.isBlank() || b.images.size !in 1..24 || ids.toSet().size != ids.size) add("invalid_visual_images")
  if(b.images.any { it.id.isBlank() || it.id.length>2000 || it.alt.isBlank() || it.alt.length>2000 || it.license.isBlank() || it.license.length>2000 || it.sourceRef !in p.sourceRefs || !Regex("media/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.(?:webp|png|jpg)").matches(it.assetPath) || it.width !in 1..8192 || it.height !in 1..8192 || !Regex("[a-f0-9]{64}").matches(it.sha256) }) add("invalid_visual_asset")
  when(type) {
   "four_pictures" -> {
    if(b.images.size!=4 || b.images.any {it.role!="clue"}) add("four_clues_required")
    addAll(AnagramPlugin().validatePuzzle(p.copy(type="anagram",payload=b.copy(variant=""))).errors)
   }
   "guess_picture" -> {
    if(b.images.size!=1 || b.images.any {it.role!="clue"} || b.revealRows !in 1..8 || b.revealCols !in 1..8 || b.revealPenalty !in 0..100) add("invalid_reveal_grid")
   }
   "symbols" -> {
    if(b.images.size !in 2..12 || b.images.any {it.role!="symbol"} || (b.correctImageOrder.isNotEmpty() && !imagePermutation(p,b.correctImageOrder))) add("invalid_symbols")
   }
   "shadow", "visual_logic" -> {
    if(visualOptions(p).size !in 2..6 || b.correctImageId !in visualOptions(p)) add("invalid_image_options")
    if(type=="shadow" && (b.images.count {it.role=="clue"}!=1 || b.images.any {it.role !in setOf("clue","option")})) add("invalid_shadow")
    if(type=="visual_logic") {
     if(b.images.any {it.role !in setOf("term","option")}) add("invalid_logic_image_role")
     addAll(logicErrors(p))
    }
   }
   "visual_association" -> {
    val left=b.images.filter {it.role=="left"}.map {it.id};val right=b.images.filter {it.role=="right"}.map {it.id}
    if(left.size !in 2..12 || right.size!=left.size || b.images.any {it.role !in setOf("left","right")} || b.visualPairs.size!=left.size || b.visualPairs.map {it.leftId}.toSet()!=left.toSet() || b.visualPairs.map {it.rightId}.toSet()!=right.toSet()) add("invalid_visual_pairs")
   }
  }
  if(type in setOf("guess_picture","symbols") && (b.acceptedAnswers.size !in 1..10 || b.acceptedAnswers.any {it.isBlank() || !answerTextValid(it)} || b.acceptedAnswers.map {normalized(p,it)}.toSet().size!=b.acceptedAnswers.size)) add("invalid_visual_answers")
  if(type!="guess_picture" && (b.revealRows!=0 || b.revealCols!=0 || b.revealPenalty!=0)) add("foreign_reveal_config")
  if(type!="symbols" && b.correctImageOrder.isNotEmpty() || type !in setOf("shadow","visual_logic") && b.correctImageId.isNotEmpty() || type!="visual_association" && b.visualPairs.isNotEmpty() || type!="visual_logic" && b.logicRule!=null) add("foreign_visual_config")
 }
 override fun createSession(puzzle:PuzzleDefinition,sessionId:String):GameSession = super.createSession(puzzle,sessionId).copy(imageOrder=if(type=="symbols") puzzle.payload.images.map {it.id} else emptyList())
 override fun evaluate(puzzle:PuzzleDefinition,session:GameSession,action:PlayerAction):GameSession {
  require(validateSession(puzzle,session).valid) { "Invalid visual session" }
  val next=super.evaluate(puzzle,session,action)
  if(next!=session || session.status!=SessionStatus.ACTIVE || action.id.isBlank() || action.id in session.processedActions) return next
  val acknowledged=when(action) {
   is PlayerAction.InputAnswer -> type in setOf("guess_picture","symbols") && answerTextValid(action.answer)
   is PlayerAction.SelectImage -> type in setOf("shadow","visual_logic") && action.imageId in visualOptions(puzzle)
   is PlayerAction.RevealRegion -> type=="guess_picture" && action.row in 0 until puzzle.payload.revealRows && action.col in 0 until puzzle.payload.revealCols
   is PlayerAction.ReorderImages -> type=="symbols" && puzzle.payload.correctImageOrder.isNotEmpty() && imagePermutation(puzzle,action.imageIds)
   is PlayerAction.MatchImages -> type=="visual_association" && imagePairValid(puzzle,MatchPair(action.leftId,action.rightId))
   is PlayerAction.Shuffle -> type=="four_pictures"
   else -> false
  }
  return if(acknowledged) next.copy(processedActions=next.processedActions+action.id) else next
 }
 private fun visualVerdict(p:PuzzleDefinition,s:GameSession) = verdict(p,s,visualSolved(p,s)).let { if(it.status==SessionStatus.WON) it.copy(score=ScoringEngine.score(p,it.wrongSubmissions,it.usedHintIds,it.revealedRegions.size)) else it }
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession {
  if(type=="four_pictures") return AnagramPlugin().reduce(p,s,a)
  return when(a) {
   is PlayerAction.InputAnswer -> if(type in setOf("guess_picture","symbols") && answerTextValid(a.answer)) s.copy(answerInput=a.answer) else s
   is PlayerAction.RevealRegion -> if(type=="guess_picture" && a.row in 0 until p.payload.revealRows && a.col in 0 until p.payload.revealCols) s.copy(revealedRegions=s.revealedRegions+(a.row*p.payload.revealCols+a.col)) else s
   is PlayerAction.SelectImage -> if(type in setOf("shadow","visual_logic") && a.imageId in visualOptions(p)) s.copy(selectedImageId=a.imageId) else s
   is PlayerAction.ReorderImages -> if(type=="symbols" && p.payload.correctImageOrder.isNotEmpty() && imagePermutation(p,a.imageIds)) s.copy(imageOrder=a.imageIds) else s
   is PlayerAction.MatchImages -> {val pair=MatchPair(a.leftId,a.rightId);if(type=="visual_association" && imagePairValid(p,pair)) s.copy(imageMatches=s.imageMatches.filter {it.leftId!=a.leftId && it.rightId!=a.rightId}+pair) else s}
   is PlayerAction.Submit -> if(when(type) { "guess_picture","symbols" -> s.answerInput.isNotBlank(); "shadow","visual_logic" -> s.selectedImageId!=null; "visual_association" -> s.imageMatches.size==p.payload.visualPairs.size; else -> false }) visualVerdict(p,s) else s
   else -> s
  }
 }
}
class FourPicturesPlugin:VisualPlugin("four_pictures")
class GuessPicturePlugin:VisualPlugin("guess_picture")
class ShadowPlugin:VisualPlugin("shadow")
class SymbolsPlugin:VisualPlugin("symbols")
class VisualAssociationPlugin:VisualPlugin("visual_association")
class VisualLogicPlugin:VisualPlugin("visual_logic")
