package com.lughzdan.engine
import kotlin.random.Random

internal fun key(c: Cell)="${c.row},${c.col}"
internal fun entryCells(e: CrosswordEntry)=e.tokens.indices.map {Cell(e.row+if(e.direction=="down") it else 0,e.col-if(e.direction=="across") it else 0)}
internal fun normalized(p: PuzzleDefinition,s:String)=ArabicText.comparison(s,NormalizationPolicy(removeMarks=p.normalization=="arabic_basic_v1"))
internal fun tokens(s:String):List<String> {
 val out=mutableListOf<String>()
 s.codePoints().toArray().forEach {cp->val t=String(Character.toChars(cp));if(Character.getType(cp) in listOf(6,7,8) && out.isNotEmpty()) out[out.lastIndex]+=t else out.add(t)}
 return out
}
internal fun validToken(s:String)=s.length in 1..8 && runCatching {
 val cp=s.codePointAt(0); val arabic=cp in 0x0621..0x063A || cp in 0x0641..0x064A || cp in 0x066E..0x066F || cp in 0x0671..0x06D3 || cp in 0x06FA..0x06FC
 arabic && s.isNotBlank() && tokens(ArabicText.comparison(s)).size==1 && Character.getType(s.codePointAt(0)) !in listOf(6,7,8) && s.codePoints().noneMatch {Character.isISOControl(it)||Character.isWhitespace(it)}}.getOrDefault(false)
/** Input adapters share the exact token policy used by puzzle and action validation. */
object ArabicCellInput {
 fun isValid(token:String):Boolean=validToken(token)
}
internal fun reservedTiles(b:ChoicePayload):Set<Int> {
 val used=mutableSetOf<Int>();for(f in b.fixedSlots){val i=b.tiles.indices.firstOrNull {it !in used && b.tiles[it]==f.token} ?: return setOf(-1);used.add(i)};return used
}
internal fun multipleAnswers(p:PuzzleDefinition)=p.payload.variant in setOf("multi_word","words_multiple","multiple","word_from_word")
internal fun builtAnswer(p:PuzzleDefinition,s:GameSession):String {
 val selected=s.selectedTileIndices.map {p.payload.tiles[it]}.iterator();val fixed=p.payload.fixedSlots.associate {it.index to it.token}
 return (0 until s.selectedTileIndices.size+fixed.size).joinToString(""){fixed[it] ?: if(selected.hasNext()) selected.next() else ""}
}
abstract class BoardPlugin(final override val type:String):GamePlugin {
 protected abstract fun errors(p:PuzzleDefinition):List<String>
 protected abstract fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession
 override fun validatePuzzle(puzzle:PuzzleDefinition)=ValidationResult(buildList {
  addAll(ChoicePlugin().validatePuzzle(puzzle.copy(type="choice",payload=ChoicePayload("q",listOf(Option("a","a"),Option("b","b")),"a"))).errors)
  if(puzzle.type!=type) add("wrong_type")
  val allowed=when(type){"anagram"->setOf("","anagram","missing_letter","word_from_letters","word_from_word","multi_word","words_multiple","multiple","arrange","missing","extra");"matching"->setOf("","synonym","antonym","word_meaning","intruder","context","difference");"order"->setOf("","sentence","story","procedure","chronology","proverb","poetry","completion");else->setOf("")}
  if(puzzle.payload.variant !in allowed) add("unsupported_variant")
  addAll(errors(puzzle))
 })
 override fun createSession(puzzle:PuzzleDefinition,sessionId:String):GameSession {
  require(validatePuzzle(puzzle).valid);return GameSession(sessionId,puzzle.puzzleId,puzzle.revision,stateVersion=2,tileOrder=puzzle.payload.tiles.indices.toList(),orderedIds=puzzle.payload.items.map {it.id})
 }
 protected fun verdict(p:PuzzleDefinition,s:GameSession,correct:Boolean)=if(correct) s.copy(status=SessionStatus.WON,score=ScoringEngine.score(p,s.wrongSubmissions,s.usedHintIds)) else s.copy(wrongSubmissions=s.wrongSubmissions+1,status=if(s.wrongSubmissions>=2) SessionStatus.LOST else SessionStatus.ACTIVE)
 override fun evaluate(puzzle:PuzzleDefinition,session:GameSession,action:PlayerAction):GameSession {
  require(session.puzzleId==puzzle.puzzleId && session.revision==puzzle.revision && session.stateVersion==2)
  if(session.status in setOf(SessionStatus.WON,SessionStatus.LOST) || action.id in session.processedActions || action.id.isBlank() || (session.status==SessionStatus.PAUSED && action !is PlayerAction.Resume)) return session
  val n=when(action){is PlayerAction.Resume->session.copy(status=SessionStatus.ACTIVE);is PlayerAction.UseHint->{val h=puzzle.hints.firstOrNull {it.id==action.hintId};if(h==null) session else if(h.id in session.usedHintIds) session else session.copy(usedHintIds=session.usedHintIds+h.id,revealedHints=session.revealedHints+(h.id to HintProvider.reveal(puzzle,session,h)))};else->reduce(puzzle,session,action)}
  val acknowledged=when(action){
   is PlayerAction.Resume->true
   is PlayerAction.UseHint->puzzle.hints.any {it.id==action.hintId}
   is PlayerAction.Reorder->type=="order"&&action.itemIds.size==puzzle.payload.items.size&&action.itemIds.toSet()==puzzle.payload.items.map {it.id}.toSet()
   is PlayerAction.Shuffle->type=="anagram"
   is PlayerAction.InputCell->type=="crossword"&&Cell(action.row,action.col) in puzzle.payload.entries.flatMap {entryCells(it)}&&(action.token.isEmpty()||validToken(action.token))
   is PlayerAction.SelectPath->type=="wordsearch"&&straight(action.path)&&action.path.all {it.row in puzzle.payload.grid.indices&&it.col in puzzle.payload.grid[it.row].indices}
   is PlayerAction.Select->type=="matching"&&puzzle.payload.options.any {it.id==action.optionId}
   is PlayerAction.Match->type=="matching"&&puzzle.payload.left.any {it.id==action.leftId}&&puzzle.payload.right.any {it.id==action.rightId}
   is PlayerAction.Submit->when(type){"anagram"->session.selectedTileIndices.isNotEmpty();"wordsearch"->session.selectedPath.isNotEmpty();"matching"->if(puzzle.payload.options.isNotEmpty()) session.selectedId!=null else session.matchedPairs.size==puzzle.payload.pairs.size;"order"->true;"crossword"->puzzle.payload.entries.flatMap {entryCells(it)}.all {session.cells[key(it)]?.isNotEmpty()==true};else->false}
   else->false
  }
  return if(n==session&&!acknowledged) session else n.copy(processedActions=n.processedActions+action.id)
 }
}
internal fun crosswordErrors(b:ChoicePayload,compare:(String)->String={it}):List<String> = buildList {
 if(b.rows !in 3..15 || b.cols !in 3..15 || b.entries.size !in 2..60) add("invalid_dimensions_or_entries")
 if(b.entries.map {it.id}.distinct().size!=b.entries.size || b.entries.map {compare(it.tokens.joinToString(""))}.distinct().size!=b.entries.size) add("duplicate_entry")
 val board=mutableMapOf<Cell,String>();val paths=b.entries.map {entryCells(it)}
 b.entries.forEachIndexed {i,e->
  if(e.id.isBlank()||e.clue.isBlank()||e.sourceRef.isBlank()||e.direction !in setOf("across","down")||e.tokens.size !in 2..15||e.tokens.any {!validToken(it)}) add("invalid_entry")
  paths[i].zip(e.tokens).forEach {(c,t)->if(c.row !in 0 until b.rows||c.col !in 0 until b.cols) add("entry_out_of_bounds");val prev=board.put(c,compare(t));if(prev!=null&&prev!=compare(t)) add("conflicting_intersection")}
  for(j in 0 until i) if(e.direction==b.entries[j].direction && paths[i].any {it in paths[j]}) add("same_direction_overlap")
 }
 if(paths.isNotEmpty()) {val seen=mutableSetOf(0);var changed=true;while(changed){changed=false;for(i in paths.indices) if(i !in seen && seen.any {j->paths[i].any {it in paths[j]}}){seen.add(i);changed=true}};if(seen.size!=paths.size) add("disconnected_entries")}
 for(c in board.keys) for(d in listOf("across","down")) {
  val dr=if(d=="down") 1 else 0;val dc=if(d=="across") -1 else 0
  if(Cell(c.row-dr,c.col-dc) in board) continue
  val run=mutableListOf<Cell>();var current=c
  while(current in board){run.add(current);current=Cell(current.row+dr,current.col+dc)}
  if(run.size>1 && b.entries.indices.none {b.entries[it].direction==d&&paths[it]==run}) add("undeclared_run")
 }
}
class CrosswordPlugin:BoardPlugin("crossword") {
 override fun errors(p:PuzzleDefinition)=crosswordErrors(p.payload){normalized(p,it)}+if(p.payload.entries.any {it.sourceRef !in p.sourceRefs}) listOf("unknown_entry_source") else emptyList()
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession {
  val solution=p.payload.entries.flatMap {entryCells(it).zip(it.tokens)}.toMap()
  return when(a){is PlayerAction.InputCell->{val c=Cell(a.row,a.col);if(c !in solution||(a.token.isNotEmpty()&&!validToken(a.token))) s else s.copy(cells=if(a.token.isEmpty()) s.cells-key(c) else s.cells+(key(c) to a.token))};is PlayerAction.Submit->if(solution.keys.any {s.cells[key(it)].isNullOrEmpty()}) s else verdict(p,s,solution.all {(c,t)->normalized(p,s.cells[key(c)] ?: "")==normalized(p,t)});else->s}
 }
}
internal fun straight(path:List<Cell>):Boolean {
 if(path.size !in 2..15) return false;val dr=path[1].row-path[0].row;val dc=path[1].col-path[0].col
 return dr in -1..1&&dc in -1..1&&(dr!=0||dc!=0)&&path.zipWithNext().all {(a,b)->b.row-a.row==dr&&b.col-a.col==dc}
}
internal fun occurrences(grid:List<List<String>>,target:List<String>):Set<Set<Cell>> = buildSet {
 for(r in grid.indices) for(c in grid[r].indices) for(dr in -1..1) for(dc in -1..1) {if(dr==0&&dc==0) continue;val path=target.indices.map {Cell(r+it*dr,c+it*dc)};if(path.all {it.row in grid.indices&&it.col in grid[it.row].indices}&&path.map {grid[it.row][it.col]}==target) add(path.toSet())}
}
class WordsearchPlugin:BoardPlugin("wordsearch") {
 override fun errors(p:PuzzleDefinition):List<String> = buildList {
  val b=p.payload;val width=b.grid.firstOrNull()?.size ?: 0
  if(b.grid.size !in 3..15||width !in 3..15||b.grid.any {it.size!=width||it.any {t->!validToken(t)}}) add("invalid_grid")
  if(b.words.size !in 1..30||b.words.map {it.id}.distinct().size!=b.words.size||b.words.map {it.path.toSet()}.distinct().size!=b.words.size) add("invalid_words")
  for(w in b.words){if(w.id.isBlank()||!straight(w.path)||w.path.any {it.row !in b.grid.indices||it.col !in b.grid[it.row].indices}) add("invalid_word_path") else {
   val target=w.path.map {normalized(p,b.grid[it.row][it.col])}
   if(target.joinToString("")!=normalized(p,w.label)) add("word_label_mismatch")
   val declared=b.words.filter {normalized(p,it.label)==normalized(p,w.label)}.map {it.path.toSet()}.toSet()
   if(occurrences(b.grid.map {row->row.map {normalized(p,it)}},target)!=declared) add("undeclared_occurrence")
  }}
 }
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession=when(a){
  is PlayerAction.SelectPath->if(straight(a.path)&&a.path.all {it.row in p.payload.grid.indices&&it.col in p.payload.grid[it.row].indices}) s.copy(selectedPath=a.path) else s
  is PlayerAction.Submit->{if(s.selectedPath.isEmpty()) s else {val w=p.payload.words.firstOrNull {it.path==s.selectedPath||it.path.reversed()==s.selectedPath};if(w==null) verdict(p,s.copy(selectedPath=emptyList()),false) else if(w.id in s.foundWordIds) s else s.copy(selectedPath=emptyList(),foundWordIds=s.foundWordIds+w.id).let {if(it.foundWordIds.size==p.payload.words.size) verdict(p,it,true) else it}}}
  else->s
 }
}
class AnagramPlugin:BoardPlugin("anagram") {
 override fun errors(p:PuzzleDefinition):List<String> = buildList {
  val b=p.payload
  if(b.tiles.size !in 2..30||b.tiles.any {!validToken(it)}||b.acceptedAnswers.size !in 1..10||b.acceptedAnswers.any {it.isBlank()}) add("invalid_tiles_or_answers")
  if(b.fixedSlots.map {it.index}.distinct().size!=b.fixedSlots.size||b.fixedSlots.any {it.index !in b.tiles.indices||!validToken(it.token)}||-1 in reservedTiles(b)) add("invalid_fixed_slots")
  if(b.acceptedAnswers.map {normalized(p,it)}.distinct().size!=b.acceptedAnswers.size) add("duplicate_normalized_answer")
  for(answer in b.acceptedAnswers){val ts=tokens(normalized(p,answer));val inventory=b.tiles.map {normalized(p,it)}.toMutableList();if(b.fixedSlots.size>=ts.size||ts.any {!inventory.remove(it)}||b.fixedSlots.any {it.index !in ts.indices||normalized(p,it.token)!=ts.getOrNull(it.index)}) add("unbuildable_answer")}
 }
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession=when(a){
  is PlayerAction.AddTile->if(a.tileIndex !in p.payload.tiles.indices||a.tileIndex in s.selectedTileIndices||a.tileIndex in reservedTiles(p.payload)) s else s.copy(selectedTileIndices=s.selectedTileIndices+a.tileIndex)
  is PlayerAction.RemoveTile->if(a.slotIndex !in s.selectedTileIndices.indices) s else s.copy(selectedTileIndices=s.selectedTileIndices.filterIndexed {i,_->i!=a.slotIndex})
  is PlayerAction.Undo->if(s.selectedTileIndices.isEmpty()) s else s.copy(selectedTileIndices=s.selectedTileIndices.dropLast(1))
  is PlayerAction.Shuffle->s.copy(tileOrder=p.payload.tiles.indices.shuffled(Random(a.seed)))
  is PlayerAction.Submit->if(s.selectedTileIndices.isEmpty()) s else {
   val value=builtAnswer(p,s);val target=normalized(p,value);val correct=p.payload.acceptedAnswers.any {normalized(p,it)==target}
   if(!multipleAnswers(p)) verdict(p,s.copy(answerHistory=s.answerHistory+value),correct)
   else if(!correct) verdict(p,s.copy(answerHistory=s.answerHistory+value),false)
   else if(s.answerHistory.any {normalized(p,it)==target}) s
   else {val next=s.copy(answerHistory=s.answerHistory+value,selectedTileIndices=emptyList());if(p.payload.acceptedAnswers.all {answer->next.answerHistory.any {normalized(p,it)==normalized(p,answer)}}) verdict(p,next,true) else next}
  }
  else->s
 }
}
class MatchingPlugin:BoardPlugin("matching") {
 override fun errors(p:PuzzleDefinition):List<String> {
  val b=p.payload
  if(b.options.isNotEmpty()) return ChoicePlugin().validatePuzzle(p.copy(type="choice")).errors + buildList {
   if(b.variant !in setOf("intruder","context","difference")||b.options.size!=4) add("invalid_semantic_choice_variant")
   if(b.options.map {normalized(p,it.text)}.distinct().size!=b.options.size) add("duplicate_normalized_option")
  }
  return buildList {
  if(b.left.size !in 2..12||b.right.size!=b.left.size||b.pairs.size!=b.left.size||b.left.map {it.id}.toSet().size!=b.left.size||b.right.map {it.id}.toSet().size!=b.right.size||(b.left+b.right).any {it.id.isBlank()||it.text.isBlank()}) add("invalid_matching_items")
  if(b.left.map {normalized(p,it.text)}.distinct().size!=b.left.size||b.right.map {normalized(p,it.text)}.distinct().size!=b.right.size) add("duplicate_normalized_option")
  if(b.pairs.map {it.leftId}.toSet()!=b.left.map {it.id}.toSet()||b.pairs.map {it.rightId}.toSet()!=b.right.map {it.id}.toSet()) add("invalid_pairs")
  if(b.variant !in setOf("","synonym","antonym","word_meaning")) add("invalid_semantic_pair_variant")
 }
 }
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession {
  if(p.payload.options.isNotEmpty()) return when(a) {
   is PlayerAction.Select->if(p.payload.options.any {it.id==a.optionId}) s.copy(selectedId=a.optionId) else s
   is PlayerAction.Submit->if(s.selectedId==null) s else verdict(p,s.copy(submittedIds=s.submittedIds+s.selectedId),s.selectedId==p.payload.correctId).let {if(it.status==SessionStatus.ACTIVE) it.copy(selectedId=null) else it}
   else->s
  }
  return when(a){
  is PlayerAction.Match->if(p.payload.left.none {it.id==a.leftId}||p.payload.right.none {it.id==a.rightId}) s else s.copy(matchedPairs=s.matchedPairs.filter {it.leftId!=a.leftId&&it.rightId!=a.rightId}+MatchPair(a.leftId,a.rightId))
  is PlayerAction.Submit->if(s.matchedPairs.size!=p.payload.pairs.size) s else verdict(p,s,s.matchedPairs.toSet()==p.payload.pairs.toSet())
  else->s
 }
 }
}
class OrderPlugin:BoardPlugin("order") {
 override fun errors(p:PuzzleDefinition)=if(p.payload.items.size in 2..20&&p.payload.items.all {it.id.isNotBlank()&&it.text.isNotBlank()}&&p.payload.items.map {it.id}.toSet().size==p.payload.items.size&&p.payload.correctOrder.size==p.payload.items.size&&p.payload.correctOrder.toSet()==p.payload.items.map {it.id}.toSet()) emptyList() else listOf("invalid_order")
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession=when(a){is PlayerAction.Reorder->if(a.itemIds.size==p.payload.items.size&&a.itemIds.toSet()==p.payload.items.map {it.id}.toSet()) s.copy(orderedIds=a.itemIds) else s;is PlayerAction.Submit->verdict(p,s,s.orderedIds==p.payload.correctOrder);else->s}
}
