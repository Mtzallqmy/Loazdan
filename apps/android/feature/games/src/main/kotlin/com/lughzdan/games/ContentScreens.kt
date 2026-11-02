package com.lughzdan.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lughzdan.domain.*

@Composable internal fun ContentLibrary(vm:GameViewModel,onDownloads:()->Unit,onStorage:()->Unit) {
 val catalog by vm.catalog.collectAsStateWithLifecycle()
 val local by vm.packs.collectAsStateWithLifecycle()
 val disabled by vm.cloudDisabledReason.collectAsStateWithLifecycle()
 var search by remember { mutableStateOf("") }
 var installedOnly by remember { mutableStateOf(false) }
 var selected by remember { mutableStateOf<ContentPack?>(null) }
 val published=catalog.filter { it.published }
 val visible=published.filter { (!installedOnly||it.installedVersion!=null)&&(search.isBlank()||it.title.contains(search,true)||it.id.contains(search,true)) }
 LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  item { Text("مكتبة المحتوى",style=MaterialTheme.typography.headlineMedium) }
  item { OutlinedTextField(value=search,onValueChange={search=it},label={Text("البحث في الحزم")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
  item { Row { FilterChip(selected=!installedOnly,onClick={installedOnly=false},label={Text("كل المنشور")});Spacer(Modifier.width(8.dp));FilterChip(selected=installedOnly,onClick={installedOnly=true},label={Text("المثبتة")}) } }
  item { Row { TextButton(onClick=onDownloads){Text("التنزيلات")};TextButton(onClick=onStorage){Text("التخزين")};TextButton(onClick={vm.contentAction { refresh() }},enabled=disabled==null){Text("تحديث المكتبة")} } }
  disabled?.let { reason->item { Text(reason) } }
  if(visible.isEmpty()) item { Text(if(installedOnly)"لا توجد حزم منشورة مثبتة تطابق البحث." else "لا توجد حزم منشورة متاحة تطابق البحث.") }
  items(visible,key={it.id}) { pack->OutlinedCard(onClick={selected=pack},modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(pack.title,style=MaterialTheme.typography.titleMedium);Text("النسخة ${pack.version} · التنزيل ${bytes(pack.downloadBytes)}");Text(if(pack.installedVersion!=null)"مثبتة · النسخة ${pack.installedVersion}" else "متاحة للتنزيل") } } }
  if(published.isEmpty()&&search.isBlank()) items(local,key={"local-"+it.id}) { pack->Text("${pack.id} · النسخة ${pack.version} · ${bytes(pack.bytes)}\n"+if(pack.fixtureOnly)"حزمة اختبار محلية غير منشورة" else "مثبتة محليًا") }
 }
 selected?.let { pack->AlertDialog(onDismissRequest={selected=null},title={Text(pack.title)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(pack.description);Text("النسخة ${pack.version}\nحجم التنزيل: ${bytes(pack.downloadBytes)}\nالحجم المثبت: ${bytes(pack.installedBytes)}");Text("الترخيص: ${pack.license.ifBlank { "غير متاح" }}");Text("المصادر: "+pack.sources.ifEmpty { listOf("غير متاحة") }.joinToString("\n"))}},confirmButton={TextButton(onClick={vm.contentAction { download(pack.id) };selected=null},enabled=disabled==null){Text(if(pack.installedVersion==null)"تنزيل" else "تنزيل النسخة المنشورة")}},dismissButton={TextButton(onClick={selected=null}){Text("إغلاق")}}) }
}

@Composable internal fun DownloadsScreen(vm:GameViewModel) {
 val downloads by vm.downloads.collectAsStateWithLifecycle()
 val preferences by vm.contentPreferences.collectAsStateWithLifecycle()
 LazyColumn(verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  item { Text("التنزيلات",style=MaterialTheme.typography.headlineMedium);Text(if(preferences.wifiOnly)"التنزيل عبر Wi-Fi فقط" else "يسمح باستخدام بيانات الهاتف");Text("تبدأ التنزيلات يدويًا من المكتبة.") }
  if(downloads.isEmpty())item { Text("لا توجد تنزيلات.") }
  items(downloads,key={it.packId}) { task->Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { Text(task.packId,style=MaterialTheme.typography.titleMedium);Text(downloadLabel(task.status));Text("${bytes(task.downloadedBytes)} / ${bytes(task.totalBytes)}")
   if(task.totalBytes>0)LinearProgressIndicator(progress={ (task.downloadedBytes.toDouble()/task.totalBytes).coerceIn(0.0,1.0).toFloat() },modifier=Modifier.fillMaxWidth())
   task.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
   Row { when(task.status) {
    DownloadStatus.QUEUED,DownloadStatus.DOWNLOADING,DownloadStatus.WAITING_FOR_WIFI->TextButton(onClick={vm.contentAction { pause(task.packId) }}){Text("إيقاف مؤقت")}
    DownloadStatus.PAUSED->TextButton(onClick={vm.contentAction { resume(task.packId) }}){Text("استئناف")}
    DownloadStatus.FAILED,DownloadStatus.CANCELLED->TextButton(onClick={vm.contentAction { retry(task.packId) }}){Text("إعادة المحاولة")}
    else->Unit
   };if(task.status !in setOf(DownloadStatus.COMPLETED,DownloadStatus.CANCELLED))TextButton(onClick={vm.contentAction { cancel(task.packId) }}){Text("إلغاء التنزيل")} }
  } }
 }
}

@Composable internal fun StorageScreen(vm:GameViewModel) {
 val usage by vm.storage.collectAsStateWithLifecycle()
 val packs by vm.packs.collectAsStateWithLifecycle()
 var deleting by remember { mutableStateOf<InstalledPack?>(null) }
 LazyColumn(verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  item { Text("التخزين",style=MaterialTheme.typography.headlineMedium);Text("الحزم المثبتة: ${bytes(usage.installedBytes)}\nالملفات المؤقتة: ${bytes(usage.temporaryBytes)}");Text("حذف الحزمة يحفظ تقدمك وسجل جولاتك. أعد تنزيل الحزمة لمتابعة ألغازها.");TextButton(onClick={vm.contentAction { cleanupTemporaryFiles() }}){Text("تنظيف الملفات المؤقتة")} }
  items(packs,key={it.id}) { pack->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("${pack.id}\n${bytes(pack.bytes)}",modifier=Modifier.weight(1f));TextButton(onClick={deleting=pack}){Text("حذف الحزمة")} } }
 }
 deleting?.let { pack->AlertDialog(onDismissRequest={deleting=null},title={Text("حذف الحزمة؟")},text={Text("ستُحذف ${pack.id} من الجهاز مع الاحتفاظ بتقدمك.")},confirmButton={TextButton(onClick={vm.contentAction { delete(pack.id) };deleting=null}){Text("حذف")}},dismissButton={TextButton(onClick={deleting=null}){Text("إلغاء")}}) }
}
private fun bytes(value:Long)=when { value>=1024*1024->"%.1f ميغابايت".format(value/1048576.0);value>=1024->"%.1f كيلوبايت".format(value/1024.0);else->"$value بايت" }
private fun downloadLabel(status:DownloadStatus)=when(status) { DownloadStatus.QUEUED->"في الانتظار";DownloadStatus.WAITING_FOR_WIFI->"بانتظار Wi-Fi";DownloadStatus.DOWNLOADING->"جارٍ التنزيل";DownloadStatus.VERIFYING->"جارٍ التحقق";DownloadStatus.INSTALLING->"جارٍ التثبيت";DownloadStatus.PAUSED->"متوقف مؤقتًا";DownloadStatus.FAILED->"فشل التنزيل";DownloadStatus.COMPLETED->"اكتمل التثبيت";DownloadStatus.CANCELLED->"ملغى" }
