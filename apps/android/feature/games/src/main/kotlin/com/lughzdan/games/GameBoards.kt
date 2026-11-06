package com.lughzdan.games

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lughzdan.engine.*
import java.util.UUID

private fun aid()=UUID.randomUUID().toString()
val gameNames=mapOf(
 "choice" to "تحدي المفردات العربية — أربعة اختيارات",
 "crossword" to "الكلمات المتقاطعة",
 "wordsearch" to "البحث عن الكلمات",
 "anagram" to "بناء الكلمات",
 "matching" to "العلاقات الدلالية",
 "order" to "ترتيب الجمل والأبيات",
 "text_answer" to "الكتابة والتعبير العربي",
 "sequence" to "المتتاليات والأنماط المنطقية",
 "image_guess" to "تخمين الصور",
 "four_pictures" to "أربع صور وكلمة",
 "guess_picture" to "خمن الصورة",
 "shadow" to "خمن الظل",
 "symbols" to "لغز الرموز",
 "visual_association" to "اكتشف العلاقة",
 "visual_logic" to "المنطق البصري"
)
@Composable private fun Grid(rows:Int,cols:Int,modifier:Modifier=Modifier,cell:@Composable (Int,Int)->Unit) {
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
  Column(Modifier.horizontalScroll(rememberScrollState()).then(modifier)) { repeat(rows){r->Row {repeat(cols){c->cell(r,c)}}} }
 }
}
/** Boards translate gestures to intents; the engine owns all game rules. */
@Composable fun GameBoard(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 when(p.type) {
 "choice" -> ChoiceAnswers(p,s,busy,send)
 "crossword" -> Crossword(p,s,busy,send)
 "wordsearch" -> Search(p,s,busy,send)
 "anagram" -> Builder(p,s,busy,send)
 "matching" -> if(p.payload.options.isNotEmpty()) ChoiceAnswers(p,s,busy,send) else Matching(p,s,busy,send)
 "order" -> Ordering(p,s,busy,send)
 "text_answer" -> TextAnswerBoard(p,s,busy,send)
 "sequence" -> SequenceBoard(p,s,busy,send)
 "image_guess" -> ImageGuessBoard(p,s,busy,send)
 in visualGameTypes -> VisualBoard(p,s,busy,send)
 }
}
private fun path(e:CrosswordEntry)=e.tokens.indices.map { Cell(e.row+if(e.direction=="down")it else 0,e.col-if(e.direction=="across")it else 0) }
@Composable internal fun Crossword(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 val cellSize=(48f*LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
 var entryId by rememberSaveable(p.puzzleId){mutableStateOf(p.payload.entries.first().id)}
 val e=p.payload.entries.first {it.id==entryId}
 var activeRow by rememberSaveable(p.puzzleId){mutableStateOf(e.row)}
 var activeCol by rememberSaveable(p.puzzleId){mutableStateOf(e.col)}
 val active=Cell(activeRow,activeCol)
 fun selectCell(cell:Cell) { activeRow=cell.row;activeCol=cell.col }
 val starts=p.payload.entries.map {Cell(it.row,it.col)}.distinct().sortedWith(compareBy<Cell> {it.row}.thenByDescending {it.col})
 fun number(item:CrosswordEntry)=starts.indexOf(Cell(item.row,item.col))+1
 val terminal=s.status in setOf(SessionStatus.WON,SessionStatus.LOST)
 OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
 Text("سؤال ${number(e)} · ${if(e.direction=="down")"عمودي" else "أفقي"} · ${e.tokens.size} حروف",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
 Text(e.clue,style=MaterialTheme.typography.titleMedium)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
  TextButton(onClick={val i=p.payload.entries.indexOf(e);val next=p.payload.entries[(i-1+p.payload.entries.size)%p.payload.entries.size];entryId=next.id;selectCell(Cell(next.row,next.col))}){Text("السؤال السابق")}
  TextButton(onClick={val i=p.payload.entries.indexOf(e);val next=p.payload.entries[(i+1)%p.payload.entries.size];entryId=next.id;selectCell(Cell(next.row,next.col))}){Text("السؤال التالي")}
 }
 Text("✓ صحيح   × راجع الحرف · يظهر التصحيح بعد التحقق",style=MaterialTheme.typography.bodySmall)
 var showClues by rememberSaveable(s.id){mutableStateOf(false)}
 TextButton(onClick={showClues=!showClues}){Text(if(showClues)"إخفاء قائمة الكلمات" else "اختيار كلمة أخرى")}
 if(showClues) p.payload.entries.forEach { item->TextButton(onClick={entryId=item.id;selectCell(Cell(item.row,item.col))},modifier=Modifier.heightIn(min=48.dp)){Text((if(item.id==entryId)"✓ " else "")+"${number(item)}. ${item.clue} · ${if(item.direction=="down")"عمودي" else "أفقي"}")} }
 TextButton(onClick={p.payload.entries.firstOrNull {it.id!=entryId&&active in path(it)}?.let {entryId=it.id}},enabled=!busy&&p.payload.entries.any {it.id!=entryId&&active in path(it)}){Text("تغيير الاتجاه عند التقاطع")}
 val open=p.payload.entries.flatMap(::path).toSet()
 Grid(p.payload.rows,p.payload.cols){r,c->val pos=Cell(r,c);val mark=AnswerFeedback.cell(p,s,r,c)
  if(pos !in open)Surface(Modifier.size(cellSize).padding(2.dp),color=MaterialTheme.colorScheme.surfaceVariant){} else OutlinedButton(onClick={selectCell(pos);if(pos !in path(e))entryId=p.payload.entries.first {pos in path(it)}.id},enabled=!busy||terminal,colors=ButtonDefaults.outlinedButtonColors(contentColor=feedbackForeground(mark),containerColor=when {mark!=AnswerMark.NEUTRAL->feedbackBackground(mark);pos==active->MaterialTheme.colorScheme.primaryContainer;pos in path(e)->MaterialTheme.colorScheme.secondaryContainer;else->MaterialTheme.colorScheme.surface}),shape=RoundedCornerShape(8.dp),modifier=Modifier.size(cellSize).semantics {contentDescription="خلية $r $c";stateDescription=when {mark!=AnswerMark.NEUTRAL->mark.label;pos==active->"الخلية المحددة";pos in path(e)->"ضمن الكلمة النشطة";else->"خلية"}},contentPadding=PaddingValues(0.dp)){Column(horizontalAlignment=Alignment.CenterHorizontally) {
    val marker=if(mark==AnswerMark.CORRECT)"✓" else if(mark==AnswerMark.INCORRECT)"×" else if(pos==active)"▣" else if(pos in path(e))"─" else ""
    Text((if(pos in starts)"${starts.indexOf(pos)+1} " else "")+marker,fontSize=10.sp,lineHeight=12.sp,maxLines=1)
    Text(s.cells["$r,$c"]?:"",fontSize=18.sp,lineHeight=20.sp,maxLines=1)
   }}
 }
 } }
 if(terminal)return
 var showKeys by rememberSaveable(s.id){mutableStateOf(true)}
 Row {TextButton(onClick={showKeys=!showKeys}){Text(if(showKeys)"طي لوحة الحروف" else "إظهار الحروف")};TextButton(onClick={send(PlayerAction.InputCell(aid(),active.row,active.col,""))},enabled=!busy){Text("مسح الحرف")}}
 if(showKeys) "ابتثجحخدذرزسشصضطظعغفقكلمنهويأةإآىءؤئ".chunked(6).forEach {line->Row(Modifier.fillMaxWidth()) {line.forEach {token->OutlinedButton(onClick={send(PlayerAction.InputCell(aid(),active.row,active.col,token.toString()));val cells=path(e);selectCell(cells[(cells.indexOf(active)+1).coerceAtMost(cells.lastIndex)])},enabled=!busy,modifier=Modifier.weight(1f).height(48.dp).semantics {contentDescription="مفتاح $token"},contentPadding=PaddingValues(0.dp)){Text(token.toString())}}} }
 var keyboard by rememberSaveable {mutableStateOf(false)}
 TextButton(onClick={keyboard=!keyboard}){Text(if(keyboard)"إخفاء إدخال النظام" else "إدخال حرف مشكّل بلوحة النظام")}
 if(keyboard) {
 var cellDraft by rememberSaveable(p.puzzleId){mutableStateOf("")}
 OutlinedTextField(value=cellDraft,onValueChange={cellDraft=it},label={Text("حرف الخلية")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())
 TextButton(onClick={send(PlayerAction.InputCell(aid(),active.row,active.col,cellDraft));cellDraft=""},enabled=!busy&&ArabicCellInput.isValid(cellDraft)){Text("إدخال الحرف")}
 TextButton(onClick={send(PlayerAction.InputCell(aid(),active.row,active.col,""))},enabled=!busy&&!s.cells["${active.row},${active.col}"].isNullOrEmpty()){Text("مسح الخلية")}
 }
}
@Composable private fun Search(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var startRow by rememberSaveable(p.puzzleId){mutableStateOf<Int?>(null)}
 var startCol by rememberSaveable(p.puzzleId){mutableStateOf<Int?>(null)}
 val start=startRow?.let {r->startCol?.let {c->Cell(r,c)}}
 fun selectStart(cell:Cell?) {startRow=cell?.row;startCol=cell?.col}
 val foundCells=p.payload.words.filter {it.id in s.foundWordIds}.flatMap {it.path}.toSet()
 Text("الكلمات المكتشفة: ${s.foundWordIds.size} / ${p.payload.words.size}")
 Text("اضغط مطولًا ثم اسحب على الكلمة، أو انقر بدايتها ثم نهايتها")
 val cellPixels=with(LocalDensity.current){48.dp.toPx()}
 var preview by remember(s.id){mutableStateOf(emptyList<Cell>())}
 fun cellAt(offset:Offset):Cell? {
  val row=kotlin.math.floor(offset.y/cellPixels).toInt();val col=kotlin.math.floor(offset.x/cellPixels).toInt()
  return if(row in p.payload.grid.indices && col in p.payload.grid.first().indices) Cell(row,col) else null
 }
 fun line(a:Cell,b:Cell):List<Cell> {
  val dr=b.row-a.row;val dc=b.col-a.col
  if(dr!=0 && dc!=0 && kotlin.math.abs(dr)!=kotlin.math.abs(dc))return emptyList()
  return (0..maxOf(kotlin.math.abs(dr),kotlin.math.abs(dc))).map {Cell(a.row+it*dr.compareTo(0),a.col+it*dc.compareTo(0))}
 }
 val currentSend by rememberUpdatedState(send)
 Grid(p.payload.grid.size,p.payload.grid.first().size,Modifier.pointerInput(s.id,busy,cellPixels) {
  if(!busy) {var origin:Cell?=null;var cursor=Offset.Zero
   detectDragGesturesAfterLongPress(
    onDragStart={cursor=it;origin=cellAt(it);preview=origin?.let {listOf(it)} ?: emptyList();selectStart(null)},
    onDrag={change,delta->change.consume();cursor+=delta;preview=origin?.let {a->cellAt(cursor)?.let {line(a,it)}} ?: emptyList()},
    onDragEnd={if(preview.isNotEmpty())currentSend(PlayerAction.SelectPath(aid(),preview));preview=emptyList();origin=null},
    onDragCancel={preview=emptyList();origin=null})
  }
 }){r,c->val pos=Cell(r,c)
  OutlinedButton(enabled=!busy,onClick={val a=start;if(a==null)selectStart(pos) else {val dr=r-a.row;val dc=c-a.col;val n=maxOf(kotlin.math.abs(dr),kotlin.math.abs(dc));val cells=if(dr==0||dc==0||kotlin.math.abs(dr)==kotlin.math.abs(dc))(0..n).map {Cell(a.row+it*dr.compareTo(0),a.col+it*dc.compareTo(0))} else listOf(a,pos);send(PlayerAction.SelectPath(aid(),cells));selectStart(null)}},colors=ButtonDefaults.outlinedButtonColors(containerColor=if(pos in preview)MaterialTheme.colorScheme.primaryContainer else if(pos in foundCells)feedbackBackground(AnswerMark.CORRECT) else MaterialTheme.colorScheme.surface),modifier=Modifier.size(48.dp).semantics {contentDescription="بحث $r $c";stateDescription=if(pos in foundCells)"حرف في كلمة مكتشفة" else if(pos==start||pos in s.selectedPath)"حرف محدد" else "حرف"},contentPadding=PaddingValues(0.dp)){Text((if(pos in foundCells)"✓ " else if(pos==start||pos in s.selectedPath)"•" else "")+p.payload.grid[r][c])}
 }
 p.payload.words.forEach {Text((if(it.id in s.foundWordIds)"✓ " else "○ ")+it.label)}
 TextButton(onClick={selectStart(null)},enabled=start!=null){Text("إلغاء تحديد البداية")}
}
@Composable internal fun Builder(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 Text("اضغط الحروف لبناء الإجابة؛ اضغط حرفًا في الإجابة لإزالته")
 val fixed=p.payload.fixedSlots.associate {it.index to it.token}
 val slotCount=maxOf(p.payload.acceptedAnswers.minOfOrNull {ArabicText.comparison(it,NormalizationPolicy(removeMarks=true)).length} ?: 0,s.selectedTileIndices.size+fixed.size,(fixed.keys.maxOrNull()?:-1)+1)
 var selectedSlot=0
 val answer=(0 until slotCount).map {index->fixed[index] ?: s.selectedTileIndices.getOrNull(selectedSlot++)?.let {p.payload.tiles[it]} ?: "□"}.joinToString("")
 Text("الإجابة الحالية: $answer")
 Row(Modifier.horizontalScroll(rememberScrollState())) {
  var removableSlot=0
  repeat(slotCount) {index->
   val token=fixed[index]
   if(token!=null) Surface(Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp),color=MaterialTheme.colorScheme.secondaryContainer) {Text(token,Modifier.padding(12.dp))}
   else {val slot=removableSlot++;val tile=s.selectedTileIndices.getOrNull(slot)
    OutlinedButton(onClick={send(PlayerAction.RemoveTile(aid(),slot))},enabled=!busy&&tile!=null,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)){Text(tile?.let {p.payload.tiles[it]}?:"□")}
   }
  }
 }
 fun compare(answer:String)=ArabicText.comparison(answer,NormalizationPolicy(removeMarks=p.normalization=="arabic_basic_v1"))
 val accepted=p.payload.acceptedAnswers.map(::compare).toSet()
 val discoveries=s.answerHistory.filter {compare(it) in accepted}.distinctBy(::compare)
 val wrongAttempts=s.answerHistory.filter {compare(it) !in accepted}
 Text("الإجابات المكتشفة: ${discoveries.size} / ${accepted.size}")
 discoveries.forEach {Text("✓ $it")}
 if(wrongAttempts.isNotEmpty()) {
  Text("المحاولات السابقة غير الصحيحة: ${wrongAttempts.size}")
  wrongAttempts.forEach {Text("× $it")}
 }
 val reserved=mutableSetOf<Int>();p.payload.fixedSlots.forEach {f->p.payload.tiles.indices.firstOrNull {it !in reserved&&p.payload.tiles[it]==f.token}?.let {reserved+=it}}
 s.tileOrder.ifEmpty {p.payload.tiles.indices.toList()}.chunked(5).forEach {row->Row(Modifier.horizontalScroll(rememberScrollState())) {row.forEach {index->OutlinedButton(onClick={send(PlayerAction.AddTile(aid(),index))},enabled=!busy&&index !in s.selectedTileIndices&&index !in reserved,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).semantics {contentDescription="حرف $index ${p.payload.tiles[index]}"}){Text(p.payload.tiles[index])}}}}
 Row {TextButton(onClick={send(PlayerAction.Undo(aid()))},enabled=!busy&&s.selectedTileIndices.isNotEmpty()){Text("تراجع")};TextButton(onClick={send(PlayerAction.Shuffle(aid(),s.processedActions.size+1))},enabled=!busy){Text("خلط الحروف")}}
}
@Composable private fun Matching(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var selected by rememberSaveable(p.puzzleId){mutableStateOf<String?>(null)}
 Text("اختر كلمة ثم العلاقة المناسبة")
 Row(Modifier.fillMaxWidth()) {
  Column(Modifier.weight(1f)){SessionPresentation.shuffled(p.payload.left,s,"left").forEach {o->OutlinedButton(onClick={selected=o.id},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text((if(selected==o.id)"✓ " else "")+o.text)}}}
  Column(Modifier.weight(1f)){SessionPresentation.matchingRight(p,s).forEach {o->OutlinedButton(onClick={selected?.let {send(PlayerAction.Match(aid(),it,o.id))};selected=null},enabled=!busy&&selected!=null,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(o.text)}}}
 }
 s.matchedPairs.forEach {pair->Text("${p.payload.left.first {it.id==pair.leftId}.text} ↔ ${p.payload.right.first {it.id==pair.rightId}.text}")}
}
@Composable private fun Ordering(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 val order=s.orderedIds.ifEmpty {p.payload.items.map {it.id}}
 val heights=remember(s.id){mutableStateMapOf<String,Int>()}
 var dragging by remember(s.id){mutableStateOf<String?>(null)}
 var target by remember(s.id){mutableStateOf<Int?>(null)}
 val currentOrder by rememberUpdatedState(order)
 val currentSend by rememberUpdatedState(send)
 Text("اضغط مطولًا على البطاقة واسحب لترتيبها، أو استخدم زري النقل")
 order.forEachIndexed {index,id->
  val label=p.payload.items.first {it.id==id}.text
  OutlinedCard(modifier=Modifier.fillMaxWidth().onSizeChanged {heights[id]=it.height}.pointerInput(s.id,id,busy) {
   if(!busy) {var displacement=0f
    detectDragGesturesAfterLongPress(
     onDragStart={dragging=id;target=currentOrder.indexOf(id);displacement=0f},
     onDrag={change,delta->
      change.consume();displacement+=delta.y
      val from=currentOrder.indexOf(id);var destination=from;var remaining=kotlin.math.abs(displacement)
      val direction=if(displacement>=0)1 else -1
      while(destination+direction in currentOrder.indices) {
       val threshold=(heights[currentOrder[destination+direction]] ?: 120).toFloat()
       if(remaining<threshold/2)break
       destination+=direction;remaining-=threshold
      }
      target=destination
     },
     onDragEnd={
      val from=currentOrder.indexOf(id);val to=target
      if(from>=0 && to!=null && to in currentOrder.indices && to!=from) {
       val next=currentOrder.toMutableList();next.add(to,next.removeAt(from));currentSend(PlayerAction.Reorder(aid(),next))
      }
      dragging=null;target=null
     },onDragCancel={dragging=null;target=null})
   }
  },colors=CardDefaults.outlinedCardColors(containerColor=if(dragging==id)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
   Column(Modifier.padding(12.dp)) {
    Text("${index+1}. $label",style=MaterialTheme.typography.titleMedium)
    if(dragging==id)Text("الإفلات في الموضع ${(target ?: index)+1}",style=MaterialTheme.typography.bodySmall)
    Row {listOf(-1 to "لأعلى",1 to "لأسفل").forEach {(delta,actionLabel)->
     TextButton(onClick={val next=order.toMutableList();next.add(index+delta,next.removeAt(index));send(PlayerAction.Reorder(aid(),next))},enabled=!busy&&index+delta in order.indices,modifier=Modifier.heightIn(min=48.dp).semantics {contentDescription="$actionLabel $label"}){Text(actionLabel)}
    }}
   }
  }
 }
}

@Composable private fun TextAnswerBoard(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var input by rememberSaveable(s.id){mutableStateOf(s.answerInput)}
 Text(p.payload.prompt,style=MaterialTheme.typography.titleMedium)
 OutlinedTextField(
  value=input,
  onValueChange={if(it.length<=2000){input=it;send(PlayerAction.InputAnswer(aid(),it))}},
  label={Text("اكتب إجابتك بالعربية")},
  enabled=!busy,
  modifier=Modifier.fillMaxWidth()
 )
 if(p.normalization=="arabic_basic_v1") {
  Text("ملاحظة: يُقبل الجواب بتجريد الحركات؛ تُحفظ الفروق بين الهمزات.",style=MaterialTheme.typography.bodySmall)
 }
}

@Composable private fun SequenceBoard(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var input by rememberSaveable(s.id){mutableStateOf(s.answerInput)}
 Text("أكمل المتتالية وفق النمط المنطقي:",style=MaterialTheme.typography.titleMedium)
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  p.payload.terms.forEach {term->
   Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.padding(vertical=8.dp)) {
    Text("$term",modifier=Modifier.padding(horizontal=16.dp,vertical=10.dp),style=MaterialTheme.typography.titleLarge)
   }
  }
  Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.secondaryContainer,modifier=Modifier.padding(vertical=8.dp)) {
   Text("؟",modifier=Modifier.padding(horizontal=16.dp,vertical=10.dp),style=MaterialTheme.typography.titleLarge)
  }
 }
 }
 if(s.status in setOf(SessionStatus.WON,SessionStatus.LOST) && p.payload.rule.isNotBlank()) {
  Text(p.payload.rule,style=MaterialTheme.typography.bodyMedium)
 }
 OutlinedTextField(
  value=input,
  onValueChange={if(it.length<=50){input=it;send(PlayerAction.InputAnswer(aid(),it))}},
  label={Text("العدد التالي (أرقام عربية أو لاتينية)")},
  singleLine=true,
  enabled=!busy,
  modifier=Modifier.fillMaxWidth()
 )
}

@Composable private fun ImageGuessBoard(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var input by rememberSaveable(s.id){mutableStateOf(s.answerInput)}
 Text("تخمين الصورة — النمط: ${when(p.payload.mode){"four"->"أربع صور";"reveal"->"كشف تدريجي";"shadow"->"ظل الصورة";"symbols"->"رموز متتالية";else->p.payload.mode}}",style=MaterialTheme.typography.titleSmall)
 val steps=p.payload.revealSteps.coerceAtLeast(1)
 val total=steps*steps
 if(p.payload.mode=="reveal"&&p.payload.assets.isNotEmpty()) {
  Text("اكشف أجزاء الصورة بالضغط على المربعات. الأجزاء المكشوفة: ${s.revealedRegions.size} / $total")
  Grid(steps,steps){r,c->
   val idx=r*steps+c
   if(idx in s.revealedRegions) {
    Surface(Modifier.size(48.dp).padding(2.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
     Text("👁",Modifier.padding(8.dp))
    }
   } else {
    OutlinedButton(
     onClick={send(PlayerAction.RevealRegion(aid(),r,c))},
     enabled=!busy,
     modifier=Modifier.size(48.dp).padding(2.dp)
    ) { Text("${idx+1}") }
   }
  }
 }
 p.payload.assets.forEachIndexed {idx,asset->
  Text("عنصر ${idx+1}: ${asset.alt}",style=MaterialTheme.typography.bodySmall)
 }
 OutlinedTextField(
  value=input,
  onValueChange={if(it.length<=2000){input=it;send(PlayerAction.InputAnswer(aid(),it))}},
  label={Text("اكتب ما تدل عليه الصورة")},
  enabled=!busy,
  modifier=Modifier.fillMaxWidth()
 )
}
