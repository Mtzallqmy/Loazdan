package com.lughzdan.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lughzdan.domain.*
import kotlinx.coroutines.launch

@Composable fun VocabularyScreen(repository:VocabularyRepository,onOpenGame:(String)->Unit) {
 val entries by repository.entries.collectAsStateWithLifecycle(emptyList())
 val catalog by repository.catalog.collectAsStateWithLifecycle(emptyList())
 val scope=rememberCoroutineScope()
 var search by remember { mutableStateOf("") }
 var favoritesOnly by remember { mutableStateOf(false) }
 var adding by remember { mutableStateOf(false) }
 var selected by remember { mutableStateOf<VocabularyEntry?>(null) }
 val visible=entries.filter { (!favoritesOnly||it.favorite) && matches(it.snapshot,search) }
 LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  item { Text("مفرداتي",style=MaterialTheme.typography.headlineMedium);Text("محفوظة على هذا الجهاز. عدد المراجعات تدريب يدوي ولا يعني اعتماد المحتوى.") }
  item { OutlinedTextField(search,{search=it},label={Text("ابحث بالكلمة أو المعنى أو الجذر أو التصنيف")},modifier=Modifier.fillMaxWidth().testTag("vocabulary-search")) }
  item { Row { FilterChip(favoritesOnly,{favoritesOnly=!favoritesOnly},label={Text("المفضلة")});TextButton({adding=true}){Text("إضافة مفردة")} } }
  if(visible.isEmpty())item { Text("لا توجد مفردات محفوظة تطابق البحث.") }
  items(visible,key={it.snapshot.id}) { entry->
   OutlinedCard(onClick={selected=entry},modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
    Text(entry.snapshot.word,style=MaterialTheme.typography.titleMedium);Text(entry.snapshot.meaning)
    Text("المراجعات اليدوية: ${entry.reviewCount}")
    Row { TextButton({scope.launch { repository.setFavorite(entry.snapshot.id,!entry.favorite) }}){Text(if(entry.favorite)"إزالة من المفضلة" else "إضافة للمفضلة")}
     TextButton({scope.launch {repository.recordReview(entry.snapshot.id)}}){Text("راجعت هذه المفردة")} }
   } }
  }
  val available=catalog.filter { candidate->entries.none {it.snapshot.id==candidate.id} && matches(candidate,search) }
  if(available.isNotEmpty())item { Text("من المحتوى المتاح",style=MaterialTheme.typography.titleMedium) }
  items(available,key={"catalog:${it.id}"}) { snapshot->Column {
   Text(snapshot.word);Text(snapshot.meaning)
   Text("الحالة: ${snapshot.contentStatus}"+if(snapshot.fixtureOnly)" · محتوى اختبار" else "")
   TextButton({scope.launch {repository.save(snapshot)}}){Text("حفظ في مفرداتي")}
  } }
 }
 selected?.let { original->
  val entry=entries.find {it.snapshot.id==original.snapshot.id} ?: original
  val s=entry.snapshot
  AlertDialog(onDismissRequest={selected=null},title={Text(s.word)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(s.meaning);Text("المصدر: ${s.source}\nالموضع: ${s.sourceLocator}")
   if(s.root.isNotBlank())Text("الجذر: ${s.root}")
   if(s.category.isNotBlank())Text("التصنيف: ${s.category}")
   Text("الحالة: ${s.contentStatus}"+if(s.fixtureOnly)" · محتوى اختبار" else "")
   if(s.source=="user_note")Text("ملاحظة شخصية غير مراجعة؛ ليست محتوى علميًا معتمدًا.")
   Text("المراجعات اليدوية: ${entry.reviewCount}")
   s.linkedGameIds.forEach { id->TextButton({selected=null;onOpenGame(id)}){Text("فتح اللعبة المرتبطة: $id")} }
   TextButton({scope.launch {repository.remove(s.id)};selected=null}){Text("حذف من مفرداتي")}
  }},confirmButton={TextButton({selected=null}){Text("إغلاق")}})
 }
 if(adding)ManualWordDialog(onDismiss={adding=false}) { word,meaning,root,category->
  scope.launch {repository.addManual(word,meaning,root,category)};adding=false
 }
}
private fun matches(s:VocabularySnapshot,query:String)=query.isBlank() || listOf(s.word,s.meaning,s.root,s.category).any { it.contains(query,true) }
@Composable private fun ManualWordDialog(onDismiss:()->Unit,onSave:(String,String,String,String)->Unit) {
 var word by remember {mutableStateOf("")};var meaning by remember {mutableStateOf("")}
 var root by remember {mutableStateOf("")};var category by remember {mutableStateOf("")}
 AlertDialog(onDismissRequest=onDismiss,title={Text("إضافة مفردة شخصية")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("تُحفظ كملاحظة شخصية غير مراجعة (user_note / unreviewed).")
  OutlinedTextField(word,{word=it},label={Text("الكلمة")},modifier=Modifier.testTag("manual-word"))
  OutlinedTextField(meaning,{meaning=it},label={Text("المعنى")},modifier=Modifier.testTag("manual-meaning"))
  OutlinedTextField(root,{root=it},label={Text("الجذر (اختياري)")})
  OutlinedTextField(category,{category=it},label={Text("التصنيف (اختياري)")})
 }},confirmButton={TextButton({onSave(word,meaning,root,category)},enabled=word.isNotBlank()&&meaning.isNotBlank()){Text("حفظ المفردة")}},dismissButton={TextButton(onDismiss){Text("إلغاء")}})
}
