package com.lughzdan.games
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
 val unavailableSessions by vm.unavailableSessions.collectAsStateWithLifecycle()
 val packs by vm.packs.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle()
 val tone=remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC,35) }.getOrNull() }
 DisposableEffect(Unit) { onDispose { tone?.release() } }
 fun play(id:String) { vm.start(id) { nav.navigate("session/$it") } }
 LughzdanTheme(settings.dark) {
  Scaffold(bottomBar={ if(ready) NavigationBar { listOf("home" to "الرئيسية","catalog" to "الألعاب","library" to "المكتبة","settings" to "الإعدادات").forEach { (route,label)->NavigationBarItem(modifier=Modifier.testTag("nav-$route"),selected=nav.currentBackStackEntryAsState().value?.destination?.route==route,onClick={ nav.navigate(route){launchSingleTop=true;popUpTo("home"){inclusive=false}} },icon={Text(label.take(1))},label={Text(label)}) } } }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp)) {
    Text("لُغْزَدان",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(vertical=16.dp))
    error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::initialize){Text("إعادة المحاولة")} }
    if(!ready) { CircularProgressIndicator();Text("تهيئة المكتبة المحلية…") }
    else NavHost(nav,startDestination="home",modifier=Modifier.weight(1f)) {
     composable("home") { Page {
      Text("مدينة الكلمات",style=MaterialTheme.typography.headlineMedium)
      Text("مستواك ${ProgressionRules.playerLevel(progress.totalXp)} · ${progress.totalScore} نقطة · ${progress.completed} ألغاز مكتملة")
      Text("العب بالمحتوى المثبّت دون اتصال")
      Text("الخبرة: ${progress.totalXp} XP · الصعوبة المفتوحة: ${ProgressionRules.unlockedDifficulty(progress.totalXp)}")
      progress.perGame.forEach { item->Text("${gameNames[item.gameType] ?: item.gameType}: ${item.completed} مكتمل · ${item.totalScore} نقطة · ${item.attempts} جولة") }
      if(progress.achievements.isNotEmpty())Text("الإنجازات: "+progress.achievements.joinToString("، "){if(it=="first_win")"أول فوز" else "أول فوز: "+(gameNames[it.removePrefix("first_win_")] ?: it)})
      sessions.firstOrNull()?.let { record->Button(onClick={nav.navigate("session/${record.session.id}")}){Text("متابعة آخر نشاط")} }
      Button(onClick={nav.navigate("catalog")}){Text("ابدأ تحدي المفردات")}
      TextButton(onClick={nav.navigate("history")}){Text("مراجعة الإجابات السابقة")}
     } }
     composable("catalog") { Page {
      Text("الألعاب المتاحة",style=MaterialTheme.typography.headlineMedium)
      if(puzzles.isEmpty())Text("لا توجد حزمة معتمدة مثبتة. نسخة الإنتاج لا تتضمن مسودات الاختبار.")
      var filter by remember { mutableStateOf<String?>(null) }
      TextButton(onClick={filter=null}){Text("كل الألعاب")}
      gameNames.filterKeys { type->puzzles.any {it.type==type} }.forEach { (type,label)->TextButton(onClick={filter=type}){Text(label)} }
      TextButton(onClick={vm.morePuzzles()}){Text("عرض المزيد من الألغاز المثبتة")}
      puzzles.forEachIndexed {index,p->if(p.type in GameRegistry().types&&(filter==null||filter==p.type)) {
       Text(gameNames[p.type] ?: p.title)
       if(p.difficulty>ProgressionRules.unlockedDifficulty(progress.totalXp))Text("يفتح عند ${(p.difficulty-1)*300} XP")
       if(p.fixtureOnly)Text("محتوى اختبار غير معتمد علميًا",color=MaterialTheme.colorScheme.error)
       OutlinedButton(onClick={play(p.puzzleId)},enabled=!busy&&p.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("لغز ${index+1} · الصعوبة ${p.difficulty}")}
      }}

     } }
     composable("session/{id}") { entry->
      val id=entry.arguments?.getString("id");val record=sessions.find { it.session.id==id }
      if(record==null)Text(unavailableSessions.find { it.id==id }?.reason ?: "جاري استعادة الجولة المحلية…") else Page {
       val s=record.session;val p=record.puzzle;val terminal=s.status in setOf(SessionStatus.WON,SessionStatus.LOST)
       Text("مسودة اختبار · الصعوبة ${p.difficulty} · ${listOf("مبتدئ","سهل","متوسط","متقدم","خبير")[p.difficulty-1]}",color=MaterialTheme.colorScheme.secondary)
       val metrics=DifficultyController.metrics(p)
       Text("حجم التحدي: ${metrics.size} · تقاطعات: ${metrics.intersections}",style=MaterialTheme.typography.bodySmall)
       Text(gameNames[p.type] ?: p.title,style=MaterialTheme.typography.titleMedium)
       Text(p.payload.prompt.ifBlank {p.title},style=MaterialTheme.typography.headlineSmall)
       if(s.status==SessionStatus.PAUSED) {
        Text("حُفظت جولتك. استأنفها عندما تكون مستعدًا.")
        Button(onClick={vm.resume(s.id)},enabled=!busy){Text("استئناف الجولة")}
       } else if(!terminal) {
        Text("المحاولات المتبقية: ${3-s.wrongSubmissions}")
        GameBoard(p,s,busy){vm.action(s.id,it)}
        Button(onClick={vm.submit(s.id);if(settings.sound)tone?.startTone(ToneGenerator.TONE_PROP_BEEP,80)},enabled=(p.type!="choice"||s.selectedId!=null)&&!busy,modifier=Modifier.fillMaxWidth()){Text(if(busy)"جارٍ الحفظ…" else "تحقق من الإجابة")}

        if(s.wrongSubmissions>0)Text("الإجابة السابقة غير صحيحة؛ حاول مرة أخرى.")
        HintProvider.next(p,s)?.hint?.let { hint->TextButton(onClick={vm.hint(s.id,hint.id)},enabled=!busy){Text("تلميح (−${hint.penalty} نقطة)")} };p.hints.filter {it.id in s.usedHintIds}.forEach {hint->Text(s.revealedHints[hint.id] ?: hint.text)}
        TextButton(onClick={vm.action(s.id,PlayerAction.Pause(java.util.UUID.randomUUID().toString()))},enabled=!busy){Text("إيقاف مؤقت وحفظ")}
       } else {
        Text(if(s.status==SessionStatus.WON)"إجابة صحيحة! +${s.score} نقطة" else "انتهت المحاولات",style=MaterialTheme.typography.titleLarge)
        p.payload.options.firstOrNull {it.id==p.payload.correctId}?.let {Text("الإجابة: ${it.text}")}
        Text(p.explanation)
        Text("المصدر: صياغة داخلية للاختبار؛ الاعتماد اللغوي معلق.",style=MaterialTheme.typography.bodySmall)
        if(p.type!="choice")Text("إجاباتك: "+s.answerHistory.joinToString("، "))
        Text("إجاباتك: "+s.submittedIds.joinToString("، "){ key->p.payload.options.firstOrNull { it.id==key }?.text ?: key })
        Button(onClick={val unlocked=puzzles.filter {it.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp)};val next=unlocked[(unlocked.indexOfFirst { it.puzzleId==p.puzzleId }+1)%unlocked.size];play(next.puzzleId)},enabled=!busy&&puzzles.any {it.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp)}){Text("اللغز التالي")}
        OutlinedButton(onClick={play(p.puzzleId)},enabled=!busy){Text("إعادة المحاولة")}
        TextButton(onClick={nav.navigate("history")}){Text("سجل الإجابات")}
       }
      }
     }
     composable("history") { Page {
      Text("سجل الجولات",style=MaterialTheme.typography.headlineMedium)
      if(sessions.isEmpty())Text("ابدأ لغزًا لتظهر جولتك هنا.")
      unavailableSessions.forEach { Text(it.reason,color=MaterialTheme.colorScheme.error) }
      sessions.forEach { record->TextButton(onClick={nav.navigate("session/${record.session.id}")}){Text("${record.puzzle.payload.prompt} · ${record.session.score} نقطة")} }
     } }
     composable("library") { ContentLibrary(vm,{nav.navigate("downloads")},{nav.navigate("storage")}) }
     composable("downloads") { DownloadsScreen(vm) }
     composable("storage") { StorageScreen(vm) }
     composable("settings") { Page {
      Text("الإعدادات",style=MaterialTheme.typography.headlineMedium)
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("الوضع الداكن");Switch(checked=settings.dark,onCheckedChange=vm::dark)}
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("صوت تأكيد الإجابة");Switch(checked=settings.sound,onCheckedChange=vm::sound)}
      val contentPreferences by vm.contentPreferences.collectAsStateWithLifecycle()
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("التنزيل عبر Wi-Fi فقط");Switch(checked=contentPreferences.wifiOnly,onCheckedChange={value->vm.contentAction { setWifiOnly(value) }})}
      Text("تنزيل الحزم يدوي. لا تبدأ تنزيلات تلقائية.")
      TextButton(onClick={nav.navigate("storage")}){Text("إدارة التخزين")}
      Text("التقدم والإعدادات محفوظان على هذا الجهاز. إزالة التطبيق تمسح بيانات الضيف. لا حساب أو رفع بيانات في هذه النسخة.")
     } }
    }
   }
  }
 }
}
@Composable private fun Page(content:@Composable ColumnScope.()->Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content) }
