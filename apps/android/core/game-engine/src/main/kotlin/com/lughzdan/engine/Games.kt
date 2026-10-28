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
internal fun validToken(s:String)=runCatching {s.isNotBlank() && tokens(ArabicText.comparison(s)).size==1 && s.codePoints().noneMatch {Character.isISOControl(it)}}.getOrDefault(false)
internal fun reservedTiles(b:ChoicePayload):Set<Int> {
 val used=mutableSetOf<Int>();for(f in b.fixedSlots){val i=b.tiles.indices.firstOrNull {it !in used && b.tiles[it]==f.token} ?: return setOf(-1);used.add(i)};return used
}
internal fun builtAnswer(p:PuzzleDefinition,s:GameSession):String {
 val selected=s.selectedTileIndices.map {p.payload.tiles[it]}.iterator();val fixed=p.payload.fixedSlots.associate {it.index to it.token}
 return (0 until s.selectedTileIndices.size+fixed.size).joinToString(""){fixed[it] ?: if(selected.hasNext()) selected.next() else ""}
}
abstract class BoardPlugin(final override val type:String):GamePlugin {
 protected abstract fun errors(p:PuzzleDefinition):List<String>
 protected abstract fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession
 override fun validatePuzzle(puzzle:PuzzleDefinition)=ValidationResult(buildList {
  addAll(ChoicePlugin().validatePuzzle(puzzle.copy(type="choice",payload=ChoicePayload("q",listOf(Option("a","a"),Option("b","b")),"a"))).errors)
  if(puzzle.type!=type) add("wrong_type");addAll(errors(puzzle))
 })
 override fun createSession(puzzle:PuzzleDefinition,sessionId:String):GameSession {
  require(validatePuzzle(puzzle).valid);return GameSession(sessionId,puzzle.puzzleId,puzzle.revision,stateVersion=2,tileOrder=puzzle.payload.tiles.indices.toList(),orderedIds=puzzle.payload.items.map {it.id})
 }
 protected fun verdict(p:PuzzleDefinition,s:GameSession,correct:Boolean)=if(correct) s.copy(status=SessionStatus.WON,score=ScoringEngine.score(p,s.wrongSubmissions,s.usedHintIds)) else s.copy(wrongSubmissions=s.wrongSubmissions+1,status=if(s.wrongSubmissions>=2) SessionStatus.LOST else SessionStatus.ACTIVE)
 override fun evaluate(puzzle:PuzzleDefinition,session:GameSession,action:PlayerAction):GameSession {
  require(session.puzzleId==puzzle.puzzleId && session.revision==puzzle.revision && session.stateVersion==2)
  if(session.status in setOf(SessionStatus.WON,SessionStatus.LOST) || action.id in session.processedActions || action.id.isBlank() || (session.status==SessionStatus.PAUSED && action !is PlayerAction.Resume)) return session
  val n=when(action){is PlayerAction.Resume->session.copy(status=SessionStatus.ACTIVE);is PlayerAction.UseHint->if(puzzle.hints.any {it.id==action.hintId}) session.copy(usedHintIds=session.usedHintIds+action.hintId) else session;else->reduce(puzzle,session,action)}
  return if(n==session) session else n.copy(processedActions=n.processedActions+action.id)
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
  if(b.words.size !in 1..30||b.words.map {it.id}.distinct().size!=b.words.size) add("invalid_words")
  for(w in b.words){if(w.id.isBlank()||!straight(w.path)||w.path.any {it.row !in b.grid.indices||it.col !in 0 until width}) add("invalid_word_path") else {
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
  for(answer in b.acceptedAnswers){val ts=tokens(normalized(p,answer));val inventory=b.tiles.map {normalized(p,it)}.toMutableList();if(ts.any {!inventory.remove(it)}||b.fixedSlots.any {it.index !in ts.indices||normalized(p,it.token)!=ts.getOrNull(it.index)}) add("unbuildable_answer")}
 }
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession=when(a){
  is PlayerAction.AddTile->if(a.tileIndex !in p.payload.tiles.indices||a.tileIndex in s.selectedTileIndices||a.tileIndex in reservedTiles(p.payload)) s else s.copy(selectedTileIndices=s.selectedTileIndices+a.tileIndex)
  is PlayerAction.RemoveTile->if(a.slotIndex !in s.selectedTileIndices.indices) s else s.copy(selectedTileIndices=s.selectedTileIndices.filterIndexed {i,_->i!=a.slotIndex})
  is PlayerAction.Undo->if(s.selectedTileIndices.isEmpty()) s else s.copy(selectedTileIndices=s.selectedTileIndices.dropLast(1))
  is PlayerAction.Shuffle->s.copy(tileOrder=p.payload.tiles.indices.shuffled(Random(a.seed)))
  is PlayerAction.Submit->if(s.selectedTileIndices.isEmpty()) s else {val value=builtAnswer(p,s);verdict(p,s.copy(answerHistory=s.answerHistory+value),p.payload.acceptedAnswers.any {normalized(p,it)==normalized(p,value)})}
  else->s
 }
}
class MatchingPlugin:BoardPlugin("matching") {
 override fun errors(p:PuzzleDefinition):List<String> = buildList {val b=p.payload
  if(b.left.size !in 2..12||b.right.size!=b.left.size||b.pairs.size!=b.left.size||b.left.map {it.id}.toSet().size!=b.left.size||b.right.map {it.id}.toSet().size!=b.right.size||(b.left+b.right).any {it.id.isBlank()||it.text.isBlank()}) add("invalid_matching_items")
  if(b.pairs.map {it.leftId}.toSet()!=b.left.map {it.id}.toSet()||b.pairs.map {it.rightId}.toSet()!=b.right.map {it.id}.toSet()) add("invalid_pairs")
 }
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession=when(a){
  is PlayerAction.Match->if(p.payload.left.none {it.id==a.leftId}||p.payload.right.none {it.id==a.rightId}) s else s.copy(matchedPairs=s.matchedPairs.filter {it.leftId!=a.leftId&&it.rightId!=a.rightId}+MatchPair(a.leftId,a.rightId))
  is PlayerAction.Submit->if(s.matchedPairs.size!=p.payload.pairs.size) s else verdict(p,s,s.matchedPairs.toSet()==p.payload.pairs.toSet())
  else->s
 }
}
class OrderPlugin:BoardPlugin("order") {
 override fun errors(p:PuzzleDefinition)=if(p.payload.items.size in 2..20&&p.payload.items.all {it.id.isNotBlank()&&it.text.isNotBlank()}&&p.payload.items.map {it.id}.toSet().size==p.payload.items.size&&p.payload.correctOrder.size==p.payload.items.size&&p.payload.correctOrder.toSet()==p.payload.items.map {it.id}.toSet()) emptyList() else listOf("invalid_order")
 override fun reduce(p:PuzzleDefinition,s:GameSession,a:PlayerAction):GameSession=when(a){is PlayerAction.Reorder->if(a.itemIds.size==p.payload.items.size&&a.itemIds.toSet()==p.payload.items.map {it.id}.toSet()) s.copy(orderedIds=a.itemIds) else s;is PlayerAction.Submit->verdict(p,s,s.orderedIds==p.payload.correctOrder);else->s}
}
