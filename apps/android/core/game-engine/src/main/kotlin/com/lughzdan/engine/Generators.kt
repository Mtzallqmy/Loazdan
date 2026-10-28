package com.lughzdan.engine
import kotlin.random.Random
sealed interface GenerationResult {
 data class Success(val payload:ChoicePayload):GenerationResult
 data class Failure(val reason:String):GenerationResult
}
data class GeneratorWord(val id:String,val tokens:List<String>,val clue:String,val sourceRef:String)
private fun validInput(w:List<GeneratorWord>,rows:Int,cols:Int,budget:Int)=rows in 3..15&&cols in 3..15&&budget in 1..100000&&w.isNotEmpty()&&w.map {it.id}.distinct().size==w.size&&w.all {it.id.isNotBlank()&&it.tokens.size in 2..15&&it.tokens.all {t->validToken(t)}}
object CrosswordGenerator {
 fun generate(words:List<GeneratorWord>,rows:Int,cols:Int,seed:Int,maxAttempts:Int=1000):GenerationResult {
  if(!validInput(words,rows,cols,maxAttempts)||words.size !in 2..60||words.any {it.clue.isBlank()||it.sourceRef.isBlank()}) return GenerationResult.Failure("invalid_input")
  val rng=Random(seed);val board=mutableMapOf<Cell,String>();val placed=mutableListOf<CrosswordEntry>();var attempts=0
  for(w in words.sortedByDescending {it.tokens.size}){
   val candidates=buildList {for(r in 0 until rows) for(c in 0 until cols) for(d in listOf("across","down")) add(CrosswordEntry(w.id,r,c,d,w.tokens,w.clue,w.sourceRef))}.shuffled(rng)
   var found:CrosswordEntry?=null
   for(e in candidates){if(++attempts>maxAttempts) return GenerationResult.Failure("attempt_budget_exhausted");val path=entryCells(e)
    if(path.any {it.row !in 0 until rows||it.col !in 0 until cols}||path.zip(e.tokens).any {(c,t)->board[c]!=null&&board[c]!=t}||placed.isNotEmpty()&&path.none {it in board}||placed.any {it.direction==e.direction&&entryCells(it).any {c->c in path}}||path.all {it in board}) continue
    found=e;break
   }
   if(found==null) return GenerationResult.Failure("no_connected_placement")
   placed.add(found);entryCells(found).zip(found.tokens).forEach {(c,t)->board[c]=t}
  }
  val b=ChoicePayload(rows=rows,cols=cols,entries=placed);val errors=crosswordErrors(b)
  return if(errors.isEmpty()) GenerationResult.Success(b) else GenerationResult.Failure(errors.first())
 }
}
object WordSearchGenerator {
 fun generate(words:List<GeneratorWord>,rows:Int,cols:Int,seed:Int,filler:List<String> = listOf("ا","ب","ت","س","م","ن"),maxAttempts:Int=1000):GenerationResult {
  if(!validInput(words,rows,cols,maxAttempts)||words.size>30||filler.isEmpty()||filler.any {!validToken(it)}) return GenerationResult.Failure("invalid_input")
  val rng=Random(seed);val board=mutableMapOf<Cell,String>();val placed=mutableListOf<SearchWord>();var attempts=0
  for(w in words.sortedByDescending {it.tokens.size}){
   val candidates=buildList {for(r in 0 until rows) for(c in 0 until cols) for(dr in -1..1) for(dc in -1..1) if(dr!=0||dc!=0) add(w.tokens.indices.map {Cell(r+it*dr,c+it*dc)})}.shuffled(rng)
   var found:List<Cell>?=null
   for(path in candidates){if(++attempts>maxAttempts) return GenerationResult.Failure("attempt_budget_exhausted");if(path.any {it.row !in 0 until rows||it.col !in 0 until cols}||path.zip(w.tokens).any {(c,t)->board[c]!=null&&board[c]!=t}) continue;found=path;break}
   if(found==null) return GenerationResult.Failure("no_placement")
   placed.add(SearchWord(w.id,w.tokens.joinToString(""),found));found.zip(w.tokens).forEach {(c,t)->board[c]=t}
  }
  if(placed.map {it.path.toSet()}.distinct().size!=placed.size) return GenerationResult.Failure("duplicate_path")
  val grid=List(rows){r->List(cols){c->board[Cell(r,c)] ?: filler[rng.nextInt(filler.size)]}}
  for(w in words){val declared=placed.filter {it.label==w.tokens.joinToString("")}.map {it.path.toSet()}.toSet();if(occurrences(grid,w.tokens)!=declared) return GenerationResult.Failure("duplicate_occurrence")}
  return GenerationResult.Success(ChoicePayload(grid=grid,words=placed))
 }
}
object WordsearchGenerator {
 fun generate(words:List<GeneratorWord>,rows:Int,cols:Int,seed:Int,filler:List<String> = listOf("ا","ب","ت","س","م","ن"),maxAttempts:Int=1000)=WordSearchGenerator.generate(words,rows,cols,seed,filler,maxAttempts)
}
