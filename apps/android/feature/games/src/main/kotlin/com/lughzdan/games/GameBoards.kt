package com.lughzdan.games

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lughzdan.engine.*
import java.util.UUID

private fun aid()=UUID.randomUUID().toString()
val gameNames=mapOf("choice" to "تحدي المفردات العربية — أربعة اختيارات","crossword" to "الكلمات المتقاطعة","wordsearch" to "البحث عن الكلمات","anagram" to "بناء الكلمات","matching" to "العلاقات الدلالية","order" to "ترتيب الكلمات")
@Composable private fun Grid(rows:Int,cols:Int,cell:@Composable (Int,Int)->Unit) {
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
  Column(Modifier.horizontalScroll(rememberScrollState())) { repeat(rows){r->Row {repeat(cols){c->cell(r,c)}}} }
 }
}
/** Boards translate gestures to intents; the engine owns all game rules. */
@Composable fun GameBoard(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 when(p.type) {
 "choice" -> p.payload.options.forEach { o->OutlinedButton(onClick={send(PlayerAction.Select(aid(),o.id))},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text((if(s.selectedId==o.id)"✓ " else "")+o.text)} }
 "crossword" -> Crossword(p,s,busy,send)
 "wordsearch" -> Search(p,s,busy,send)
 "anagram" -> Builder(p,s,busy,send)
 "matching" -> Matching(p,s,busy,send)
 "order" -> Ordering(p,s,busy,send)
 }
}
private fun path(e:CrosswordEntry)=e.tokens.indices.map { Cell(e.row+if(e.direction=="down")it else 0,e.col-if(e.direction=="across")it else 0) }
@Composable private fun Crossword(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var entryId by remember(p.puzzleId){mutableStateOf(p.payload.entries.first().id)}
 val e=p.payload.entries.first {it.id==entryId}
 var active by remember(p.puzzleId){mutableStateOf(Cell(e.row,e.col))}
 Text("الكلمة النشطة: ${e.clue} · ${if(e.direction=="down")"عمودي" else "أفقي"}")
 p.payload.entries.forEach { item->TextButton(onClick={entryId=item.id;active=Cell(item.row,item.col)},modifier=Modifier.heightIn(min=48.dp)){Text((if(item.id==entryId)"✓ " else "")+item.clue)} }
 TextButton(onClick={p.payload.entries.firstOrNull {it.id!=entryId&&active in path(it)}?.let {entryId=it.id}}){Text("تغيير الاتجاه عند التقاطع")}
 val open=p.payload.entries.flatMap(::path).toSet()
 Grid(p.payload.rows,p.payload.cols){r,c->val pos=Cell(r,c)
  if(pos !in open)Surface(Modifier.size(48.dp).padding(2.dp),color=MaterialTheme.colorScheme.surfaceVariant){} else OutlinedButton(onClick={active=pos;if(pos !in path(e))entryId=p.payload.entries.first {pos in path(it)}.id},enabled=!busy,modifier=Modifier.size(48.dp).semantics {contentDescription="خلية $r $c"},contentPadding=PaddingValues(0.dp)){Text((if(pos==active)"•" else "")+(s.cells["$r,$c"]?:""))}
 }
 "ابتثجحخدذرزسشصضطظعغفقكلمنهويأةإآىءؤئ".chunked(6).forEach {line->Row(Modifier.horizontalScroll(rememberScrollState())) {line.forEach {token->OutlinedButton(onClick={send(PlayerAction.InputCell(aid(),active.row,active.col,token.toString()));val cells=path(e);active=cells[(cells.indexOf(active)+1).coerceAtMost(cells.lastIndex)]},enabled=!busy,modifier=Modifier.size(48.dp),contentPadding=PaddingValues(0.dp)){Text(token.toString())}}} }
 TextButton(onClick={send(PlayerAction.InputCell(aid(),active.row,active.col,""))},enabled=!busy){Text("مسح الخلية")}
}
@Composable private fun Search(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var start by remember(p.puzzleId){mutableStateOf<Cell?>(null)}
 Text("اختر بداية الكلمة ثم نهايتها في خط مستقيم")
 Grid(p.payload.grid.size,p.payload.grid.first().size){r,c->val pos=Cell(r,c)
  OutlinedButton(enabled=!busy,onClick={val a=start;if(a==null)start=pos else {val dr=r-a.row;val dc=c-a.col;val n=maxOf(kotlin.math.abs(dr),kotlin.math.abs(dc));val cells=if(dr==0||dc==0||kotlin.math.abs(dr)==kotlin.math.abs(dc))(0..n).map {Cell(a.row+it*dr.compareTo(0),a.col+it*dc.compareTo(0))} else listOf(a,pos);send(PlayerAction.SelectPath(aid(),cells));start=null}},modifier=Modifier.size(48.dp).semantics {contentDescription="بحث $r $c"},contentPadding=PaddingValues(0.dp)){Text((if(pos==start||pos in s.selectedPath)"•" else "")+p.payload.grid[r][c])}
 }
 p.payload.words.forEach {Text((if(it.id in s.foundWordIds)"✓ " else "○ ")+it.label)}
 TextButton(onClick={start=null},enabled=start!=null){Text("إلغاء تحديد البداية")}
}
@Composable private fun Builder(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 Text("اضغط الحروف لبناء الإجابة؛ اضغط حرفًا في الإجابة لإزالته")
 Row(Modifier.horizontalScroll(rememberScrollState())) {s.selectedTileIndices.forEachIndexed {slot,index->OutlinedButton(onClick={send(PlayerAction.RemoveTile(aid(),slot))},enabled=!busy,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)){Text(p.payload.tiles[index])}}}
 p.payload.fixedSlots.forEach {Text("الحرف الثابت ${it.index+1}: ${it.token}")}
 val reserved=mutableSetOf<Int>();p.payload.fixedSlots.forEach {f->p.payload.tiles.indices.firstOrNull {it !in reserved&&p.payload.tiles[it]==f.token}?.let {reserved+=it}}
 s.tileOrder.ifEmpty {p.payload.tiles.indices.toList()}.chunked(5).forEach {row->Row {row.forEach {index->OutlinedButton(onClick={send(PlayerAction.AddTile(aid(),index))},enabled=!busy&&index !in s.selectedTileIndices&&index !in reserved,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).semantics {contentDescription="حرف $index ${p.payload.tiles[index]}"}){Text(p.payload.tiles[index])}}}}
 Row {TextButton(onClick={send(PlayerAction.Undo(aid()))},enabled=!busy){Text("تراجع")};TextButton(onClick={send(PlayerAction.Shuffle(aid(),s.processedActions.size+1))},enabled=!busy){Text("خلط الحروف")}}
}
@Composable private fun Matching(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var selected by remember(p.puzzleId){mutableStateOf<String?>(null)}
 Text("اختر كلمة ثم العلاقة المناسبة")
 Row(Modifier.fillMaxWidth()) {
  Column(Modifier.weight(1f)){p.payload.left.forEach {o->OutlinedButton(onClick={selected=o.id},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text((if(selected==o.id)"✓ " else "")+o.text)}}}
  Column(Modifier.weight(1f)){p.payload.right.forEach {o->OutlinedButton(onClick={selected?.let {send(PlayerAction.Match(aid(),it,o.id))};selected=null},enabled=!busy&&selected!=null,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(o.text)}}}
 }
 s.matchedPairs.forEach {pair->Text("${p.payload.left.first {it.id==pair.leftId}.text} ↔ ${p.payload.right.first {it.id==pair.rightId}.text}")}
}
@Composable private fun Ordering(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 val order=s.orderedIds.ifEmpty {p.payload.items.map {it.id}}
 Text("حرّك الكلمات لأعلى أو لأسفل")
 order.forEachIndexed {index,id->val text=p.payload.items.first {it.id==id}.text;Text("${index+1}. $text");Row {listOf(-1 to "لأعلى",1 to "لأسفل").forEach {(delta,label)->TextButton(onClick={val next=order.toMutableList();next.add(index+delta,next.removeAt(index));send(PlayerAction.Reorder(aid(),next))},enabled=!busy&&index+delta in order.indices,modifier=Modifier.heightIn(min=48.dp).semantics {contentDescription="$label $text"}){Text(label)}}}}
}
