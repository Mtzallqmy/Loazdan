package com.lughzdan.games

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lughzdan.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.UUID

val visualGameTypes=setOf("four_pictures","guess_picture","shadow","symbols","visual_association","visual_logic")
/** Pinned-session reader supplied by the app; no network image fallback. */
val LocalGameImageLoader=staticCompositionLocalOf<suspend (String)->ByteArray?> { { null } }
private object BoardImages {
 val cache=object:LruCache<String,Bitmap>(8*1024*1024) { override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount }
 suspend fun decode(image:VisualImage,loader:suspend (String)->ByteArray?):Bitmap=withContext(Dispatchers.IO) {
  cache.get(image.sha256)?.let {return@withContext it}
  val bytes=loader(image.id) ?: error("Missing asset")
  require(bytes.size<=8*1024*1024)
  val hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
  require(hash==image.sha256)
  val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
  BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
  require(bounds.outWidth==image.width && bounds.outHeight==image.height && bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096)
  require(bounds.outMimeType in setOf("image/png","image/jpeg","image/webp"))
  val options=BitmapFactory.Options().apply {while(image.width/inSampleSize>768 || image.height/inSampleSize>768)inSampleSize*=2}
  val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Invalid asset")
  cache.put(image.sha256,bitmap);bitmap
 }
}
@Composable private fun Asset(image:VisualImage,modifier:Modifier=Modifier) {
 val loader=LocalGameImageLoader.current
 var retry by remember(image.sha256){mutableIntStateOf(0)}
 var bitmap by remember(image.sha256){mutableStateOf<Bitmap?>(null)}
 var failed by remember(image.sha256){mutableStateOf(false)}
 LaunchedEffect(image.sha256,loader,retry) {
  failed=false
  try { bitmap=BoardImages.decode(image,loader) } catch(e:CancellationException){throw e} catch(_:Exception){failed=true}
 }
 Box(modifier.aspectRatio(1f)) {
  bitmap?.let {Image(it.asImageBitmap(),image.alt,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}
   ?: if(failed) Column {Text("الصورة المحلية غير متاحة أو تالفة");TextButton(onClick={retry++}){Text("إعادة قراءة الصورة")}}
   else CircularProgressIndicator(Modifier.size(32.dp))
 }
}
private fun vid()=UUID.randomUUID().toString()
@Composable fun VisualBoard(p:PuzzleDefinition,s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 when(p.type) {
  "four_pictures" -> {
   p.payload.images.chunked(2).forEach {row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach {Asset(it,Modifier.weight(1f))}}}
   Builder(p,s,busy,send)
  }
  "guess_picture" -> {
   Text("اكشف مربعات الصورة. تكلفة كل مربع: ${p.payload.revealPenalty} نقطة")
   Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
    Asset(p.payload.images.single(),Modifier.fillMaxSize())
    Column(Modifier.fillMaxSize()) {repeat(p.payload.revealRows){r->Row(Modifier.weight(1f)){repeat(p.payload.revealCols){c->
     val index=r*p.payload.revealCols+c
     if(index in s.revealedRegions) Spacer(Modifier.weight(1f).fillMaxHeight())
     else Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp).background(MaterialTheme.colorScheme.secondaryContainer).clickable(enabled=!busy){send(PlayerAction.RevealRegion(vid(),r,c))}.semantics {contentDescription="كشف المربع ${index+1}"}) {Text("${index+1}",Modifier.padding(8.dp))}
    }}}}
   }
   Text("مربعات مكشوفة: ${s.revealedRegions.size} / ${p.payload.revealRows*p.payload.revealCols}")
   TextAnswer(s,busy,send)
  }
  "shadow","visual_logic" -> {
   p.payload.images.filter {it.role in setOf("clue","term")}.chunked(3).forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach {image->Column(Modifier.weight(1f)){Asset(image);if(image.role=="term")Text("القيمة: ${p.payload.logicRule?.values?.getOrNull(p.payload.images.filter {it.role=="term"}.indexOf(image))}")}}}}
   Text("اختر الصورة المناسبة")
   p.payload.images.filter {it.role=="option"}.chunked(2).forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach {image->
    Column(Modifier.weight(1f)) {Asset(image);if(p.type=="visual_logic")Text("القيمة: ${p.payload.logicRule?.optionValues?.get(image.id)}");OutlinedButton(onClick={send(PlayerAction.SelectImage(vid(),image.id))},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics {contentDescription="اختيار الصورة ${image.id}"}){Text((if(s.selectedImageId==image.id)"✓ " else "")+"اختيار ${image.id}")}}
   }}}
  }
  "symbols" -> {
   val images=if(s.imageOrder.isEmpty()) p.payload.images else s.imageOrder.map {id->p.payload.images.first {it.id==id}}
   images.forEachIndexed {index,image->
    Asset(image,Modifier.widthIn(max=220.dp))
    if(p.payload.correctImageOrder.isNotEmpty()) Row {listOf(-1 to "السابق",1 to "التالي").forEach {(delta,label)->TextButton(onClick={val ids=images.map {it.id}.toMutableList();ids.add(index+delta,ids.removeAt(index));send(PlayerAction.ReorderImages(vid(),ids))},enabled=!busy&&index+delta in images.indices){Text("نقل $label")}}}
   }
   TextAnswer(s,busy,send)
  }
  "visual_association" -> {
   var selected by rememberSaveable(p.puzzleId){mutableStateOf<String?>(null)}
   Text("اختر صورة من المجموعة الأولى ثم الصورة المرتبطة بها من المجموعة الثانية")
   listOf("left","right").forEach {role->
    Text(if(role=="left")"المجموعة الأولى" else "المجموعة الثانية")
    p.payload.images.filter {it.role==role}.chunked(2).forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach {image->Column(Modifier.weight(1f)) {
     Asset(image)
     OutlinedButton(onClick={if(role=="left")selected=image.id else selected?.let {send(PlayerAction.MatchImages(vid(),it,image.id));selected=null}},enabled=!busy&&(role=="left"||selected!=null),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text((if(selected==image.id)"✓ " else "")+"اختيار ${image.id}")}
    }}}}
   }
   Text("روابط حالية: ${s.imageMatches.size} / ${p.payload.visualPairs.size}")
   s.imageMatches.forEach {Text("${it.leftId} ↔ ${it.rightId}")}
  }
 }
}
@Composable private fun TextAnswer(s:GameSession,busy:Boolean,send:(PlayerAction)->Unit) {
 var draft by rememberSaveable(s.id,s.answerInput){mutableStateOf(s.answerInput)}
 OutlinedTextField(value=draft,onValueChange={if(it.length<=2000)draft=it},label={Text("الإجابة البصرية")},enabled=!busy,modifier=Modifier.fillMaxWidth())
 TextButton(onClick={send(PlayerAction.InputAnswer(vid(),draft))},enabled=!busy&&draft!=s.answerInput){Text("حفظ الإجابة")}
 Text("الإجابة المحفوظة: ${s.answerInput.ifBlank {"لم تُدخل بعد"}}")
}
@Composable fun VisualSolution(p:PuzzleDefinition) {
 p.payload.images.firstOrNull {it.id==p.payload.correctImageId}?.let {Asset(it,Modifier.widthIn(max=240.dp))}
 if(p.payload.acceptedAnswers.isNotEmpty())Text("الإجابة: ${p.payload.acceptedAnswers.joinToString(" / ")}")
}
