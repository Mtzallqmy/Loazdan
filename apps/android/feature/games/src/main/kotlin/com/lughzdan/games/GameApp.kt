package com.lughzdan.games
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.lughzdan.designsystem.LughzdanTheme
import com.lughzdan.engine.*

@Composable fun GameApp(vm:GameViewModel) {
 val nav=rememberNavController()
 val ready by vm.ready.collectAsStateWithLifecycle();val error by vm.error.collectAsStateWithLifecycle()
 val settings by vm.settings.collectAsStateWithLifecycle();val puzzles by vm.puzzles.collectAsStateWithLifecycle()
 val sessions by vm.sessions.collectAsStateWithLifecycle();val progress by vm.progress.collectAsStateWithLifecycle()
 val packs by vm.packs.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle()
 val tone=remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC,35) }.getOrNull() }
 DisposableEffect(Unit) { onDispose { tone?.release() } }
 fun play(id:String) { vm.start(id) { nav.navigate("session/$it") } }
 LughzdanTheme(settings.dark) {
  Scaffold(bottomBar={ if(ready) NavigationBar { listOf("home" to "الرئيسية","catalog" to "الألعاب","library" to "المكتبة","settings" to "الإعدادات").forEach { (route,label)->NavigationBarItem(selected=nav.currentBackStackEntryAsState().value?.destination?.route==route,onClick={ nav.navigate(route){launchSingleTop=true;popUpTo("home"){saveState=true};restoreState=true} },icon={Text(label.take(1))},label={Text(label)}) } } }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp)) {
    Text("لُغْزَدان",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(vertical=16.dp))
    error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::initialize){Text("إعادة المحاولة")} }
    if(!ready) { CircularProgressIndicator();Text("تهيئة المكتبة المحلية…") }
    else NavHost(nav,startDestination="home",modifier=Modifier.weight(1f)) {
     composable("home") { Page {
      Text("مدينة الكلمات",style=MaterialTheme.typography.headlineMedium)
      Text("مستواك ${1+progress.completed/5} · ${progress.totalScore} نقطة · ${progress.completed} ألغاز مكتملة")
      Text("العب بالمحتوى المثبّت دون اتصال")
      sessions.firstOrNull()?.let { record->Button(onClick={nav.navigate("session/${record.session.id}")}){Text("متابعة آخر نشاط")} }
      Button(onClick={nav.navigate("catalog")}){Text("ابدأ تحدي المفردات")}
      TextButton(onClick={nav.navigate("history")}){Text("مراجعة الإجابات السابقة")}
     } }
     composable("catalog") { Page {
      Text("الألعاب المتاحة",style=MaterialTheme.typography.headlineMedium)
      if(puzzles.isEmpty())Text("لا توجد حزمة معتمدة مثبتة. نسخة الإنتاج لا تتضمن مسودات الاختبار.")
      if("choice" in GameRegistry().types && puzzles.isNotEmpty()) {
       Text("تحدي المفردات العربية — أربعة اختيارات")
       Text("محتوى اختبار غير معتمد علميًا",color=MaterialTheme.colorScheme.error)
       puzzles.forEachIndexed { index,p->OutlinedButton(onClick={play(p.puzzleId)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("لغز ${index+1} · الصعوبة ${p.difficulty}")} }
      }
     } }
     composable("session/{id}") { entry->
      val id=entry.arguments?.getString("id");val record=sessions.find { it.session.id==id }
      if(record==null)Text("جاري استعادة الجولة المحلية…") else Page {
       val s=record.session;val p=record.puzzle;val terminal=s.status in setOf(SessionStatus.WON,SessionStatus.LOST)
       Text("مسودة اختبار · الصعوبة ${p.difficulty}",color=MaterialTheme.colorScheme.secondary)
       Text(p.payload.prompt,style=MaterialTheme.typography.headlineSmall)
       if(!terminal) {
        Text("المحاولات المتبقية: ${3-s.wrongSubmissions}")
        p.payload.options.forEach { option->OutlinedButton(onClick={vm.select(s.id,option.id)},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text((if(s.selectedId==option.id) "✓ " else "")+option.text)} }
        Button(onClick={vm.submit(s.id);if(settings.sound)tone?.startTone(ToneGenerator.TONE_PROP_BEEP,80)},enabled=s.selectedId!=null&&!busy,modifier=Modifier.fillMaxWidth()){Text(if(busy)"جارٍ الحفظ…" else "تحقق من الإجابة")}
        if(s.wrongSubmissions>0)Text("الإجابة السابقة غير صحيحة؛ حاول مرة أخرى.")
        p.hints.firstOrNull()?.let { hint->TextButton(onClick={vm.hint(s.id,hint.id)},enabled=!busy&&hint.id !in s.usedHintIds){Text("تلميح (−${hint.penalty} نقطة)")};if(hint.id in s.usedHintIds)Text(hint.text) }
       } else {
        Text(if(s.status==SessionStatus.WON)"إجابة صحيحة! +${s.score} نقطة" else "انتهت المحاولات",style=MaterialTheme.typography.titleLarge)
        Text("الإجابة: ${p.payload.options.first { it.id==p.payload.correctId }.text}")
        Text(p.explanation)
        Text("المصدر: صياغة داخلية للاختبار؛ الاعتماد اللغوي معلق.",style=MaterialTheme.typography.bodySmall)
        Text("إجاباتك: "+s.submittedIds.joinToString("، "){ key->p.payload.options.first { it.id==key }.text })
        Button(onClick={val next=puzzles[(puzzles.indexOfFirst { it.puzzleId==p.puzzleId }+1)%puzzles.size];play(next.puzzleId)},enabled=!busy){Text("اللغز التالي")}
        OutlinedButton(onClick={play(p.puzzleId)},enabled=!busy){Text("إعادة المحاولة")}
        TextButton(onClick={nav.navigate("history")}){Text("سجل الإجابات")}
       }
      }
     }
     composable("history") { Page {
      Text("سجل الجولات",style=MaterialTheme.typography.headlineMedium)
      if(sessions.isEmpty())Text("ابدأ لغزًا لتظهر جولتك هنا.")
      sessions.forEach { record->TextButton(onClick={nav.navigate("session/${record.session.id}")}){Text("${record.puzzle.payload.prompt} · ${record.session.score} نقطة")} }
     } }
     composable("library") { Page {
      Text("مكتبة المحتوى",style=MaterialTheme.typography.headlineMedium)
      if(packs.isEmpty())Text("لا توجد حزم مثبتة.")
      packs.forEach { pack->Text("${pack.id} · النسخة ${pack.version}\nمثبّت محليًا · ${pack.bytes} بايت\n"+if(pack.fixtureOnly)"حزمة اختبار غير معتمدة" else "محتوى منشور") }
      Text("تنزيل الحزم الخارجية غير مفعّل في هذه المرحلة. المحتوى المثبت يعمل دون إنترنت.")
     } }
     composable("settings") { Page {
      Text("الإعدادات",style=MaterialTheme.typography.headlineMedium)
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("الوضع الداكن");Switch(checked=settings.dark,onCheckedChange=vm::dark)}
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("صوت تأكيد الإجابة");Switch(checked=settings.sound,onCheckedChange=vm::sound)}
      Text("التقدم والإعدادات محفوظان على هذا الجهاز. إزالة التطبيق تمسح بيانات الضيف. لا حساب أو رفع بيانات في هذه النسخة.")
     } }
    }
   }
  }
 }
}
@Composable private fun Page(content:@Composable ColumnScope.()->Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content) }
