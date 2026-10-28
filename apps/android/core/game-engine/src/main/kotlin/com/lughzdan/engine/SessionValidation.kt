package com.lughzdan.engine
fun validateGameSession(p:PuzzleDefinition,s:GameSession)=ValidationResult(buildList {
 if(s.puzzleId!=p.puzzleId||s.revision!=p.revision||s.stateVersion !in 1..2||(p.type!="choice"&&s.stateVersion!=2)) add("session_pin_mismatch")
 if(s.id.isBlank()||s.packVersion<1||s.wrongSubmissions !in 0..3||(s.status==SessionStatus.LOST)!=(s.wrongSubmissions==3)) add("invalid_session_state")
 if(s.revealedHints.size>10||s.revealedHints.any {(id,text)->id !in s.usedHintIds||p.hints.none {it.id==id}||text.isBlank()||text.length>4096}) add("invalid_revealed_hints")
 if(s.usedHintIds.any {id->p.hints.none {it.id==id}}||s.processedActions.any {it.isBlank()}) add("unknown_hint_or_action")
 if((s.status==SessionStatus.WON&&s.score!=ScoringEngine.score(p,s.wrongSubmissions,s.usedHintIds))||(s.status!=SessionStatus.WON&&s.score!=0)) add("invalid_score")
 if(s.selectedId!=null&&p.payload.options.none {it.id==s.selectedId}||s.submittedIds.any {id->p.payload.options.none {it.id==id}}) add("unknown_option")
 if(s.selectedTileIndices.distinct().size!=s.selectedTileIndices.size||s.selectedTileIndices.any {it !in p.payload.tiles.indices||it in reservedTiles(p.payload)}) add("invalid_selected_tiles")
 if(s.tileOrder.isNotEmpty()&&(s.tileOrder.size!=p.payload.tiles.size||s.tileOrder.toSet()!=p.payload.tiles.indices.toSet())) add("invalid_tile_order")
 if(s.foundWordIds.any {id->p.payload.words.none {it.id==id}}) add("unknown_found_word")
 if(s.selectedPath.isNotEmpty()&&(!straight(s.selectedPath)||s.selectedPath.any {it.row !in p.payload.grid.indices||it.col !in p.payload.grid[it.row].indices})) add("invalid_path")
 if(s.matchedPairs.any {pair->p.payload.left.none {it.id==pair.leftId}||p.payload.right.none {it.id==pair.rightId}}||s.matchedPairs.map {it.leftId}.distinct().size!=s.matchedPairs.size||s.matchedPairs.map {it.rightId}.distinct().size!=s.matchedPairs.size) add("invalid_pairs")
 if(s.orderedIds.isNotEmpty()&&(s.orderedIds.size!=p.payload.items.size||s.orderedIds.toSet()!=p.payload.items.map {it.id}.toSet())) add("invalid_order_progress")
 val allowed=p.payload.entries.flatMap {entryCells(it)}.map {key(it)}.toSet()
 if(s.cells.any {(c,t)->c !in allowed||!validToken(t)}) add("invalid_cell_progress")
 if(p.type!="anagram"&&(s.selectedTileIndices.isNotEmpty()||s.tileOrder.isNotEmpty()||s.answerHistory.isNotEmpty())||p.type!="matching"&&s.matchedPairs.isNotEmpty()||p.type!="order"&&s.orderedIds.isNotEmpty()||p.type!="wordsearch"&&(s.selectedPath.isNotEmpty()||s.foundWordIds.isNotEmpty())||p.type!="crossword"&&s.cells.isNotEmpty()||!(p.type=="choice"||p.type=="matching"&&p.payload.options.isNotEmpty())&&(s.selectedId!=null||s.submittedIds.isNotEmpty())) add("foreign_progress")
 if(s.status==SessionStatus.WON){val solved=when(p.type){
  "choice"->s.selectedId==p.payload.correctId
  "crossword"->p.payload.entries.all {e->entryCells(e).zip(e.tokens).all {(c,t)->normalized(p,s.cells[key(c)] ?: "")==normalized(p,t)}}
  "wordsearch"->s.foundWordIds==p.payload.words.map {it.id}.toSet()
  "matching"->if(p.payload.options.isNotEmpty()) s.selectedId==p.payload.correctId else s.matchedPairs.toSet()==p.payload.pairs.toSet()
  "order"->s.orderedIds==p.payload.correctOrder
  "anagram"->if(multipleAnswers(p)) p.payload.acceptedAnswers.all {a->s.answerHistory.any {normalized(p,a)==normalized(p,it)}} else s.selectedTileIndices.all {it in p.payload.tiles.indices}&&s.answerHistory.lastOrNull()==builtAnswer(p,s)&&p.payload.acceptedAnswers.any {normalized(p,it)==normalized(p,builtAnswer(p,s))}
  else->false};if(!solved) add("unsolved_terminal_record")}
})
