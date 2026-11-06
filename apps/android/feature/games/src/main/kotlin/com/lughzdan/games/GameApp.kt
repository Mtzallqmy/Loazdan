package com.lughzdan.games
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.saveable.rememberSaveable
import com.lughzdan.designsystem.DiscoveryIcon
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
 val progress by vm.progress.collectAsStateWithLifecycle()
 val packs by vm.packs.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle()
 val submitting by vm.submitting.collectAsStateWithLifecycle()
 val tone=remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC,35) }.getOrNull() }
 DisposableEffect(Unit) { onDispose { tone?.release() } }
 fun play(id:String) { vm.start(id) { nav.navigate("session/$it") } }
 LughzdanTheme(settings.dark) {
  val route=nav.currentBackStackEntryAsState().value?.destination?.route
  val playing=route=="session/{id}"
  Scaffold(bottomBar={ if(ready && !playing) NavigationBar {
   listOf("home" to "الرئيسية","worlds" to "العوالم","catalog" to "مكتبتي","profile" to "ملفي").forEach { (destination,label)->
    NavigationBarItem(modifier=Modifier.testTag("nav-$destination"),selected=route==destination,
     onClick={nav.navigate(destination){launchSingleTop=true;restoreState=true;popUpTo("home"){saveState=true}}},
     icon={DiscoveryIcon(destination)},label={Text(label,maxLines=1)})
   }
  } }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal=16.dp)) {
    if(!playing) Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text("لُغْزَدان",style=MaterialTheme.typography.titleLarge);DiscoveryIcon("brand") }
    else TextButton(onClick={nav.popBackStack()}){Text("العودة · تُحفظ حركاتك تلقائيًا")}
    error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::initialize){Text("إعادة المحاولة")} }
    if(!ready) { CircularProgressIndicator();Text("تهيئة المكتبة المحلية…") }
    else NavHost(nav,startDestination="home",modifier=Modifier.weight(1f)) {
     composable("home") {
      val sessions by vm.sessions.collectAsStateWithLifecycle()
      Page {
      Text("فضولك بداية الرحلة",style=MaterialTheme.typography.headlineLarge)
      Text("دقائق من اللعب، ومعنى جديد تكتشفه كل يوم.",color=MaterialTheme.colorScheme.onSurfaceVariant)
      Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
       Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("إلى أين يأخذك فضولك؟",style=MaterialTheme.typography.titleLarge)
        Text("ستة عوالم من الكلمات والصور والمنطق. العب دون إنترنت.")
        Button(onClick={nav.navigate("worlds")},modifier=Modifier.fillMaxWidth()){Text("ابدأ رحلة الاكتشاف")}
       }
      }
      sessions.firstOrNull()?.let {record->
       OutlinedCard(onClick={nav.navigate("session/${record.session.id}")},modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)){Text("متابعة آخر نشاط",style=MaterialTheme.typography.titleMedium);Text(gameNames[record.puzzle.type] ?: "جولتك المحفوظة")}
       }
      }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
       Text("المستوى ${ProgressionRules.playerLevel(progress.totalXp)}")
       Text("${progress.completed} ألغاز مكتملة · ${progress.totalXp} خبرة")
      }
      LinearProgressIndicator(progress={ (progress.totalXp%250)/250f },modifier=Modifier.fillMaxWidth())
      OutlinedCard(onClick={nav.navigate("daily")},modifier=Modifier.fillMaxWidth()) {
       Column(Modifier.padding(16.dp)){Text("موعدك مع تحدي اليوم",style=MaterialTheme.typography.titleMedium);Text("لغز ثابت طوال اليوم · تقدم محفوظ محليًا")}
      }
      TextButton(onClick={nav.navigate("catalog")}){Text("ابدأ تحدي المفردات")}
     } }
     composable("worlds") { WorldsScreen(vm) { worldId -> nav.navigate("world/$worldId") } }
     composable("world/{id}") { entry ->
      val worldId = entry.arguments?.getString("id") ?: "words"
      WorldDetailScreen(worldId, vm, onPlay = { play(it) }, onBack = { nav.popBackStack() })
     }
     composable("achievements") { AchievementsScreen(vm) }
     composable("daily") { DailyChallengeScreen(vm) { vm.startDaily {nav.navigate("session/$it")} } }
     composable("profile") { Column(Modifier.fillMaxSize()) {
      Row {TextButton(onClick={nav.navigate("settings")},modifier=Modifier.testTag("nav-settings")){Text("الإعدادات")};TextButton(onClick={nav.navigate("achievements")}){Text("الإنجازات")};TextButton(onClick={nav.navigate("history")}){Text("السجل")}}
      Box(Modifier.weight(1f)){ProfileScreen(vm) {nav.navigate("account")}}
     } }
     composable("account") { AccountSyncScreen(vm) }
     composable("catalog") {
      var filter by rememberSaveable {mutableStateOf<String?>(null)}
      var query by rememberSaveable {mutableStateOf("")}
      val registry=remember {GameRegistry()}
      val visible=remember(puzzles,filter,query){puzzles.withIndex().filter {it.value.type in registry.types && (filter==null || it.value.type==filter) && (query.isBlank() || (it.value.title+" "+it.value.payload.prompt+" "+gameNames[it.value.type]).contains(query.trim(),ignoreCase=true))}}
      LazyColumn(modifier=Modifier.testTag("catalog-list"),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
       item {Text("مكتبة الاكتشاف",style=MaterialTheme.typography.headlineMedium)}
       item {Row {TextButton(onClick={nav.navigate("library")}){Text("الحزم والتنزيلات")};if(vm.vocabularyRepository!=null)TextButton(onClick={nav.navigate("vocabulary")}){Text("مفرداتي")}}}
       item {OutlinedTextField(value=query,onValueChange={query=it},label={Text("ابحث عن موضوع أو نوع لعبة")},singleLine=true,modifier=Modifier.fillMaxWidth())}
       if(visible.isEmpty()) item {Text("لا توجد نتائج مطابقة ضمن الألغاز المحمّلة. جرّب موضوعًا آخر أو اعرض المزيد.")}
       item {ScrollableTabRow(selectedTabIndex=(listOf<String?>(null)+gameNames.keys).indexOf(filter).coerceAtLeast(0),edgePadding=0.dp) {
        (listOf<String?>(null)+gameNames.keys).forEach {type->Tab(selected=filter==type,onClick={filter=type},text={Text(if(type==null)"الكل" else gameNames[type] ?: type,maxLines=1)})}
       }}
       itemsIndexed(visible,key={_,item->item.value.puzzleId}) {_,item->
        val p=item.value;val unlocked=p.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp)
        OutlinedCard(onClick={play(p.puzzleId)},enabled=!busy&&unlocked,modifier=Modifier.fillMaxWidth()) {
         Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
          Text(gameNames[p.type] ?: p.type,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
          Text("لغز ${item.index+1} · الصعوبة ${p.difficulty}",style=MaterialTheme.typography.titleMedium)
          Text(if(unlocked)"ابدأ الجولة" else "يفتح عند ${(p.difficulty-1)*300} خبرة",style=MaterialTheme.typography.bodySmall)
         }
        }
       }
       item {TextButton(onClick=vm::morePuzzles){Text("عرض المزيد من الألغاز المثبتة")}}
      }
     }
     composable("session/{id}") { entry->
      val id=entry.arguments?.getString("id").orEmpty()
      val sessionFlow=remember(id){vm.observeSession(id)}
      val currentRecord by sessionFlow.collectAsStateWithLifecycle(initialValue=null)
      val record=currentRecord
      if(record==null)Text("جارٍ استعادة الجولة. إن تعذر فتحها، تبقى بياناتها محفوظة في السجل.") else Page {
       val s=record.session;val p=record.puzzle;val terminal=s.status in setOf(SessionStatus.WON,SessionStatus.LOST)
       var priorStatus by remember(s.id){mutableStateOf(s.status)}
       LaunchedEffect(s.status) {if(priorStatus!=s.status && s.status==SessionStatus.WON && settings.sound)tone?.startTone(ToneGenerator.TONE_PROP_ACK,100);priorStatus=s.status}
       Text("${if(p.fixtureOnly) "مسودة اختبار" else "محتوى الحزمة"} · الصعوبة ${p.difficulty} · ${listOf("مبتدئ","سهل","متوسط","متقدم","خبير")[p.difficulty-1]}",color=MaterialTheme.colorScheme.secondary)

       Text(gameNames[p.type] ?: p.title,style=MaterialTheme.typography.titleMedium)
       Text(p.payload.prompt.ifBlank {when(p.type){"anagram"->"كوّن كلمة من الحروف المتاحة";"order"->"رتّب البطاقات لتكوين النص";"crossword"->"حل الكلمات باستخدام القرائن";else->p.title}},style=MaterialTheme.typography.headlineSmall)
       if(s.status==SessionStatus.PAUSED) {
        Text("حُفظت جولتك. استأنفها عندما تكون مستعدًا.")
        Button(onClick={vm.resume(s.id)},enabled=!busy){Text("استئناف الجولة")}
       } else if(!terminal) {
        Text("المحاولات المتبقية: ${3-s.wrongSubmissions}")
        val loader:suspend (String)->ByteArray?=remember(s.id) { {imageId->vm.readImage(s.id,imageId)} }
        CompositionLocalProvider(LocalGameImageLoader provides loader) { GameBoard(p,s,busy){vm.action(s.id,it)} }
        Button(onClick={vm.submit(s.id)},enabled=(p.type!="choice"||s.selectedId!=null)&&!busy&&!submitting,modifier=Modifier.fillMaxWidth()){Text(if(submitting)"جارٍ الحفظ…" else "تحقق من الإجابة")}

        if(s.wrongSubmissions>0)Text("الإجابة السابقة غير صحيحة؛ حاول مرة أخرى.")
        HintProvider.next(p,s)?.hint?.let { hint->TextButton(onClick={vm.hint(s.id,hint.id)},enabled=!busy){Text("تلميح (−${hint.penalty} نقطة)")} };p.hints.filter {it.id in s.usedHintIds}.forEach {hint->Text(s.revealedHints[hint.id] ?: hint.text)}
        TextButton(onClick={vm.action(s.id,PlayerAction.Pause(java.util.UUID.randomUUID().toString()))},enabled=!busy){Text("إيقاف مؤقت وحفظ")}
       } else {
        Text(if(s.status==SessionStatus.WON)"إجابة صحيحة! +${s.score} نقطة" else "انتهت المحاولات",style=MaterialTheme.typography.titleLarge)
        p.payload.options.firstOrNull {it.id==p.payload.correctId}?.let {Text("الإجابة: ${it.text}")}
        if(p.type in visualGameTypes) {
         val loader:suspend (String)->ByteArray?=remember(s.id) { {imageId->vm.readImage(s.id,imageId)} }
         CompositionLocalProvider(LocalGameImageLoader provides loader) {VisualSolution(p)}
        }
        Text(p.explanation)
        Text(if(p.fixtureOnly) "المصدر: صياغة داخلية للاختبار؛ الاعتماد اللغوي معلق." else "المصدر: الحزمة العربية الأصلية · تفاصيل المراجع في المكتبة",style=MaterialTheme.typography.bodySmall)
        if(p.type!="choice")Text("إجاباتك: "+s.answerHistory.joinToString("، "))
        Text("إجاباتك: "+s.submittedIds.joinToString("، "){ key->p.payload.options.firstOrNull { it.id==key }?.text ?: key })
        Button(onClick={val unlocked=puzzles.filter {it.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp)};val next=unlocked[(unlocked.indexOfFirst { it.puzzleId==p.puzzleId }+1)%unlocked.size];play(next.puzzleId)},enabled=!busy&&puzzles.any {it.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp)}){Text("اللغز التالي")}
        OutlinedButton(onClick={play(p.puzzleId)},enabled=!busy){Text("إعادة المحاولة")}
        TextButton(onClick={nav.navigate("history")}){Text("سجل الإجابات")}
       }
      }
     }
     composable("vocabulary") { vm.vocabularyRepository?.let { vocabulary->VocabularyScreen(vocabulary) { nav.navigate("catalog") } } }
     composable("history") {
      val sessions by vm.sessions.collectAsStateWithLifecycle()
      val unavailableSessions by vm.unavailableSessions.collectAsStateWithLifecycle()
      Page {
      Text("سجل الجولات",style=MaterialTheme.typography.headlineMedium)
      if(sessions.isEmpty())Text("ابدأ لغزًا لتظهر جولتك هنا.")
      unavailableSessions.forEach { Text(it.reason,color=MaterialTheme.colorScheme.error) }
      TextButton(onClick=vm::moreSessions){Text("تحميل جولات أقدم")}
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
