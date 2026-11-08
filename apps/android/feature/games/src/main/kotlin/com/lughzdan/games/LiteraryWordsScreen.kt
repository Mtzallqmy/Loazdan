package com.lughzdan.games

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lughzdan.engine.*
import com.lughzdan.domain.PlayRecord
import java.util.UUID

@Composable fun LiteraryWordsScreen(puzzles:List<PuzzleDefinition>,recent:List<PlayRecord>,xp:Int,busy:Boolean,onPlay:(String)->Unit,onResume:(String)->Unit) {
 var mode by rememberSaveable {mutableStateOf(LiteraryWords.COMPLETE)}
 var category by rememberSaveable {mutableStateOf<String?>(null)}
 val all=remember(puzzles){puzzles.filter(LiteraryWords::supports)}
 val visible=remember(all,mode,category){all.filter {it.payload.variant==mode && (category==null || it.payload.mode==category)}.sortedWith(compareBy({it.difficulty},{it.payload.mode},{it.puzzleId}))}
 val recentRound=recent.firstOrNull {LiteraryWords.supports(it.puzzle) && it.session.status in setOf(SessionStatus.ACTIVE,SessionStatus.PAUSED)}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp),modifier=Modifier.testTag("literary-list")) {
  item {
   Surface(shape=MaterialTheme.shapes.extraLarge) {
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer,MaterialTheme.colorScheme.surface))).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text("ديوان الألغاز",style=MaterialTheme.typography.headlineLarge)
     Text("كلمة تكتمل… ومعنى تكتشفه",style=MaterialTheme.typography.titleMedium)
     Text("${all.size} تحديًا · نمطان · ست فئات\nحروف ومعانٍ وشعر وبلاغة، تعمل دون إنترنت.")
    }
   }
  }
  recentRound?.let {record->item {OutlinedButton(onClick={onResume(record.session.id)},modifier=Modifier.fillMaxWidth()){Text("تابع جولتك في الديوان")}}}
  item {
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    listOf(LiteraryWords.COMPLETE to "أكمل الحروف",LiteraryWords.RIDDLE to "خمن الكلمة").forEach {(id,label)->
     FilterChip(selected=mode==id,onClick={mode=id},label={Text(label)},modifier=Modifier.weight(1f).heightIn(min=48.dp))
    }
   }
   Text(if(mode==LiteraryWords.COMPLETE)"حرف ظاهر يرشدك. اختر الخانة ثم أضف الحرف المناسب." else "اقرأ القرينة واكتب الكلمة. يمكنك طلب قرينة إضافية أو كشف حرف.",style=MaterialTheme.typography.bodyMedium)
  }
  item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   FilterChip(selected=category==null,onClick={category=null},label={Text("كل الفئات")})
   LiteraryWords.categories.forEach {(id,label)->FilterChip(selected=category==id,onClick={category=id},label={Text(label)})}
  }}
  if(visible.isEmpty())item {Text("لا توجد تحديات مثبتة لهذا الاختيار بعد. جرّب فئة أخرى أو افتح مكتبة الحزم.")}
  itemsIndexed(visible,key={_,p->p.puzzleId}) {index,p->
   val unlocked=p.difficulty<=ProgressionRules.unlockedDifficulty(xp)
   OutlinedCard(onClick={onPlay(p.puzzleId)},enabled=!busy&&unlocked,modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text("${LiteraryWords.categories[p.payload.mode]} · تحدٍّ ${index+1}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
     Text(p.payload.prompt,style=MaterialTheme.typography.titleMedium,maxLines=3)
     Text("${LiteraryWords.answer(p).length} حروف · ${if(p.difficulty==1)"تمهيدي" else "متوسط"}",style=MaterialTheme.typography.bodySmall)
     Text(if(unlocked)"ابدأ التحدي" else "يفتح عند ${(p.difficulty-1)*300} خبرة",color=MaterialTheme.colorScheme.primary)
    }
   }
  }
 }
}

@Composable fun LiteraryWordBoard(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 val completion=p.payload.variant==LiteraryWords.COMPLETE
 val count=LiteraryWords.answer(p).length
 val revealed=LiteraryWords.revealed(p,s)
 var selected by rememberSaveable(s.id){mutableStateOf((0 until count).firstOrNull {it !in revealed} ?: 0)}
 LaunchedEffect(revealed.keys){if(selected in revealed)selected=(0 until count).firstOrNull {it !in revealed} ?: 0}
 var draft by rememberSaveable(s.id){mutableStateOf(s.answerInput)}
 // Repository emissions can include queued earlier edits. Keep immediate typing local;
 // merge only newly revealed positions, which are immutable hints.
 LaunchedEffect(revealed){if(completion)draft=LiteraryWords.input(p,s,draft)}
 fun edit(value:String){draft=value;send(PlayerAction.InputAnswer(UUID.randomUUID().toString(),value))}
 Text("${LiteraryWords.categories[p.payload.mode]} · $count حروف",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
 Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth()) {
  Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(if(completion)"اختر خانة وأكمل الكلمة" else "الحروف المكشوفة",style=MaterialTheme.typography.titleMedium)
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
    repeat(count){index->
     val fixed=revealed[index]
     val token=fixed ?: if(completion)draft.getOrNull(index)?.takeIf {!it.isWhitespace()}?.toString().orEmpty() else ""
     OutlinedButton(onClick={selected=index},enabled=completion&&!busy&&fixed==null,
      colors=ButtonDefaults.outlinedButtonColors(containerColor=if(completion&&selected==index)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,disabledContainerColor=MaterialTheme.colorScheme.secondaryContainer,disabledContentColor=MaterialTheme.colorScheme.onSecondaryContainer),
      modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=56.dp).semantics {contentDescription="خانة ${index+1}";stateDescription=if(fixed!=null)"حرف مكشوف $fixed" else if(token.isEmpty())"فارغة" else token},contentPadding=PaddingValues(4.dp)) {
      Text(token.ifEmpty {"·"},style=MaterialTheme.typography.titleLarge)
     }
    }
   }
  }
 }
 if(completion) {
  Text("الخانة النشطة: ${selected+1} · الحروف المكشوفة ثابتة",style=MaterialTheme.typography.bodySmall)
  val letters=remember(s.id){SessionPresentation.shuffled((LiteraryWords.answer(p).toList()+"ابتثجحخدذرزسشصضطظعغفقكلمنهويأةإآىءؤئ".toList().shuffled(kotlin.random.Random(s.id.hashCode())).take(6)).distinct().map {it.toString()},s,"literary-keyboard")}
  letters.chunked(6).forEach {row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
   row.forEach {token->OutlinedButton(onClick={
    val chars=draft.padEnd(count,' ').take(count).toCharArray();chars[selected]=token.single()
    edit(chars.concatToString());selected=((selected+1 until count)+(0 until selected)).firstOrNull {it !in revealed && chars[it].isWhitespace()} ?: selected
   },enabled=!busy&&selected !in revealed,modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {contentDescription="حرف الديوان $token"},contentPadding=PaddingValues(0.dp)){Text(token,style=MaterialTheme.typography.titleMedium)}}
  }}
  TextButton(onClick={val chars=draft.padEnd(count,' ').take(count).toCharArray();chars[selected]=' ';edit(chars.concatToString())},enabled=!busy&&selected !in revealed){Text("مسح الخانة المحددة")}
 } else {
  OutlinedTextField(value=draft,onValueChange={if(it.length<=40)edit(it)},label={Text("ما الكلمة التي تدل عليها القرينة؟")},supportingText={Text("تُقبل الحركات أو الكتابة دون تشكيل؛ تبقى الهمزات مميزة.")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("literary-answer"))
 }
}
