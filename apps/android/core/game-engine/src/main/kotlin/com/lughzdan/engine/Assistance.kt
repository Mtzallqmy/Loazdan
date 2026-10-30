package com.lughzdan.engine

/** Hints are suggestions until UseHint is dispatched; requesting one never charges points. */
data class HintSuggestion(val hint:Hint,val cell:Cell?=null,val token:String?=null,val relatedIds:List<String> = emptyList())
object HintProvider {
 fun reveal(p:PuzzleDefinition,s:GameSession,h:Hint):String {
  val suggestion=next(p.copy(hints=listOf(h)),s) ?: return h.text
  val detail=when(p.type){
   "crossword"->suggestion.cell?.let {"الصف ${it.row+1}، العمود ${it.col+1}: ${suggestion.token}"}
   "wordsearch"->suggestion.cell?.let {"بداية الكلمة: الصف ${it.row+1}، العمود ${it.col+1}"}
   "matching"->if(p.payload.options.isNotEmpty()) p.payload.options.firstOrNull {it.id!=p.payload.correctId}?.text?.let {"استبعد: $it"} else if(suggestion.relatedIds.size==2) {
    val left=p.payload.left.firstOrNull {it.id==suggestion.relatedIds[0]}?.text
    val right=p.payload.right.firstOrNull {it.id==suggestion.relatedIds[1]}?.text
    if(left!=null&&right!=null) "$left ← $right" else null
   } else null
   "order"->p.payload.items.firstOrNull {it.id==suggestion.relatedIds.firstOrNull()}?.text?.let {"البداية: $it"}
   "anagram", "four_pictures"->suggestion.token?.let {"الحرف الأول: $it"}
   else->null
  }
  return (if(detail==null) h.text else "${h.text} — $detail").take(4096)
 }

 fun next(p:PuzzleDefinition,s:GameSession):HintSuggestion? {
  if(s.status in setOf(SessionStatus.WON,SessionStatus.LOST)) return null
  val h=p.hints.firstOrNull {it.id !in s.usedHintIds} ?: return null
  return when(p.type){
   "crossword"->{val pair=p.payload.entries.flatMap {entryCells(it).zip(it.tokens)}.firstOrNull {(c,t)->normalized(p,s.cells[key(c)] ?: "")!=normalized(p,t)};HintSuggestion(h,pair?.first,pair?.second)}
   "wordsearch"->{val w=p.payload.words.firstOrNull {it.id !in s.foundWordIds};HintSuggestion(h,w?.path?.firstOrNull(),relatedIds=listOfNotNull(w?.id))}
   "matching"->{val pair=p.payload.pairs.firstOrNull {it !in s.matchedPairs};HintSuggestion(h,relatedIds=if(pair==null) emptyList() else listOf(pair.leftId,pair.rightId))}
   "order"->HintSuggestion(h,relatedIds=p.payload.correctOrder.take(1))
   "anagram", "four_pictures"->HintSuggestion(h,token=p.payload.acceptedAnswers.firstOrNull {answer->s.answerHistory.none {normalized(p,it)==normalized(p,answer)}}?.let {tokens(it).firstOrNull()})
   else->HintSuggestion(h)
  }
 }
}
data class PuzzleMetrics(val size:Int,val intersections:Int=0,val fixedPositions:Int=0,val directions:Int=0)
object DifficultyController {
 fun metrics(p:PuzzleDefinition):PuzzleMetrics=when(p.type){
  "crossword"->{val all=p.payload.entries.flatMap {entryCells(it)};PuzzleMetrics(all.distinct().size,all.groupingBy {it}.eachCount().count {it.value>1})}
  "wordsearch"->PuzzleMetrics(p.payload.words.sumOf {it.path.size},directions=p.payload.words.map {Cell((it.path.last().row-it.path.first().row).compareTo(0),(it.path.last().col-it.path.first().col).compareTo(0))}.distinct().size)
  "anagram", "four_pictures"->PuzzleMetrics(p.payload.tiles.size,fixedPositions=p.payload.fixedSlots.size)
  "matching"->PuzzleMetrics(p.payload.pairs.size)
  "order"->PuzzleMetrics(p.payload.items.size)
  in visualTypes->PuzzleMetrics(p.payload.images.size)
  else->PuzzleMetrics(p.payload.options.size)
 }
 /** Recent terminal records only. One mistake-free unassisted win is insufficient to raise level. */
 fun nextDifficulty(current:Int,recent:List<GameSession>):Int {
  require(current in 1..5)
  val finished=recent.filter {it.status in setOf(SessionStatus.WON,SessionStatus.LOST)}.takeLast(5)
  if(finished.size<3) return current
  val strong=finished.count {it.status==SessionStatus.WON&&it.wrongSubmissions==0&&it.usedHintIds.isEmpty()&&it.revealedRegions.isEmpty()}
  val struggling=finished.count {it.status==SessionStatus.LOST||it.wrongSubmissions>=2}
  return when {strong==finished.size->(current+1).coerceAtMost(5);struggling*2>=finished.size->(current-1).coerceAtLeast(1);else->current}
 }
}
