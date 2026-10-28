package com.lughzdan.engine
import kotlin.random.Random
sealed interface GenerationResult {
 data class Success(val payload:ChoicePayload):GenerationResult
 data class Failure(val reason:String):GenerationResult
}
data class GeneratorWord(val id:String,val tokens:List<String>,val clue:String,val sourceRef:String)
private fun validInput(w:List<GeneratorWord>,rows:Int,cols:Int,budget:Int)=rows in 3..15&&cols in 3..15&&budget in 1..50000&&w.isNotEmpty()&&w.map {it.id}.distinct().size==w.size&&w.all {it.id.isNotBlank()&&it.tokens.size in 2..15&&it.tokens.all {t->validToken(t)}}
object CrosswordGenerator {
 fun generate(words:List<GeneratorWord>,rows:Int,cols:Int,seed:Int,maxAttempts:Int=1000):GenerationResult {
  if(words.size !in 2..60||!validInput(words,rows,cols,maxAttempts)||words.any {it.clue.isBlank()||it.sourceRef.isBlank()}) return GenerationResult.Failure("invalid_input")
  val compare={t:String->ArabicText.comparison(t,NormalizationPolicy(removeMarks=true))}
  if(words.map {compare(it.tokens.joinToString(""))}.distinct().size!=words.size) return GenerationResult.Failure("duplicate_answer")
  val rng=Random(seed);val ordered=words.sortedByDescending {it.tokens.size}
  val candidates=ordered.map {w->buildList {
   for(r in 0 until rows) for(c in 0 until cols) for(d in listOf("across","down")) {
    val e=CrosswordEntry(w.id,r,c,d,w.tokens,w.clue,w.sourceRef)
    if(entryCells(e).all {it.row in 0 until rows&&it.col in 0 until cols}) add(e)
   }
  }.shuffled(rng)}
  var nodes=0;var exhausted=false
  fun search(remaining:List<Int>,placed:List<CrosswordEntry>,board:Map<Cell,String>):ChoicePayload? {
   if(remaining.isEmpty()) {val b=ChoicePayload(rows=rows,cols=cols,entries=placed);return if(crosswordErrors(b,compare).isEmpty()) b else null}
   // Trying a different remaining word allows a bridge word before another disconnected word.
   for(index in remaining) for(e in candidates[index]) {
    if(nodes>=maxAttempts){exhausted=true;return null};nodes++
    val path=entryCells(e)
    if(path.zip(e.tokens).any {(c,t)->board[c]!=null&&board[c]!=compare(t)}||placed.isNotEmpty()&&path.none {it in board}||placed.any {it.direction==e.direction&&entryCells(it).any {c->c in path}}||path.all {it in board}) continue
    val next=board+path.zip(e.tokens.map(compare))
    val result=search(remaining-index,placed+e,next)
    if(result!=null) return result
    if(exhausted) return null
   }
   return null
  }
  val result=search(ordered.indices.toList(),emptyList(),emptyMap())
  return if(result!=null) GenerationResult.Success(result) else GenerationResult.Failure(if(exhausted) "attempt_budget_exhausted" else "no_connected_placement")
 }
}
object WordSearchGenerator {
 fun generate(words:List<GeneratorWord>,rows:Int,cols:Int,seed:Int,filler:List<String> = listOf("ا","ب","ت","س","م","ن"),maxAttempts:Int=1000):GenerationResult {
  if(words.size !in 1..30||filler.size !in 1..100||!validInput(words,rows,cols,maxAttempts)||filler.isEmpty()||filler.any {!validToken(it)}) return GenerationResult.Failure("invalid_input")
  val compare={t:String->ArabicText.comparison(t,NormalizationPolicy(removeMarks=true))}
  val rng=Random(seed);var attempts=0
  val ordered=words.sortedByDescending {it.tokens.size}
  val candidates=ordered.map {w->buildList {
   for(r in 0 until rows) for(c in 0 until cols) for(dr in -1..1) for(dc in -1..1) if(dr!=0||dc!=0) {
    val path=w.tokens.indices.map {Cell(r+it*dr,c+it*dc)}
    if(path.all {it.row in 0 until rows&&it.col in 0 until cols}) add(path)
   }
  }}
  if(candidates.any {it.isEmpty()}) return GenerationResult.Failure("no_placement")
  // A legal placement can still acquire undeclared words from random filler.
  // Retry the whole layout so duplicates forced by placed words can also recover.
  while(attempts<maxAttempts) {
   val board=mutableMapOf<Cell,String>();val placed=mutableListOf<SearchWord>()
   for((index,w) in ordered.withIndex()) {
    var found:List<Cell>?=null
    for(path in candidates[index].shuffled(rng)) {
     if(attempts>=maxAttempts) return GenerationResult.Failure("attempt_budget_exhausted")
     attempts++
     if(path.zip(w.tokens).any {(c,t)->board[c]!=null&&compare(board.getValue(c))!=compare(t)}) continue
     found=path;break
    }
    if(found==null) break
    placed.add(SearchWord(w.id,w.tokens.joinToString(""),found));found.zip(w.tokens).forEach {(c,t)->board[c]=t}
   }
   if(placed.size!=words.size||placed.map {it.path.toSet()}.distinct().size!=placed.size) continue
   if(attempts>=maxAttempts) return GenerationResult.Failure("attempt_budget_exhausted")
   attempts++ // Filling and checking a complete board also consumes the global budget.
   val grid=List(rows){r->List(cols){c->board[Cell(r,c)] ?: filler[rng.nextInt(filler.size)]}}
   val normalizedGrid=grid.map {r->r.map(compare)}
   if(words.any {w->
    val declared=placed.filter {compare(it.label)==compare(w.tokens.joinToString(""))}.map {it.path.toSet()}.toSet()
    occurrences(normalizedGrid,w.tokens.map(compare))!=declared
   }) continue
   return GenerationResult.Success(ChoicePayload(grid=grid,words=placed))
  }
  return GenerationResult.Failure("attempt_budget_exhausted")
 }
}
object WordsearchGenerator {
 fun generate(words:List<GeneratorWord>,rows:Int,cols:Int,seed:Int,filler:List<String> = listOf("ا","ب","ت","س","م","ن"),maxAttempts:Int=1000)=WordSearchGenerator.generate(words,rows,cols,seed,filler,maxAttempts)
}
