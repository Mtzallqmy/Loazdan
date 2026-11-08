package com.lughzdan.games
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.lughzdan.designsystem.LughzdanTheme
import com.lughzdan.engine.*

@Composable fun GameApp(vm:GameViewModel) {
 val view=LocalView.current
 val nav=rememberNavController()
 val ready by vm.ready.collectAsStateWithLifecycle();val error by vm.error.collectAsStateWithLifecycle()
 val settings by vm.settings.collectAsStateWithLifecycle();val puzzles by vm.puzzles.collectAsStateWithLifecycle()
 val atlasPuzzles by vm.atlasPuzzles.collectAsStateWithLifecycle()
 val atlasRuns by vm.atlasRuns.collectAsStateWithLifecycle()
 val discoveries by vm.discoveries.collectAsStateWithLifecycle()
 val progress by vm.progress.collectAsStateWithLifecycle()
 val packs by vm.packs.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle()
 val submitting by vm.submitting.collectAsStateWithLifecycle()
 val context=LocalContext.current
 val audio=remember(context){GameAudio(context)}
 DisposableEffect(audio) {onDispose {audio.close()}}
 fun play(id:String) { vm.start(id) { nav.navigate("session/$it") } }
 LughzdanTheme(settings.dark) {
  val route=nav.currentBackStackEntryAsState().value?.destination?.route
  val playing=route=="session/{id}"
  Scaffold(bottomBar={ if(ready && !playing) NavigationBar {
   listOf("home" to "الدار","worlds" to "الأطلس","diwan" to "ديواني").forEach { (destination,label)->
    NavigationBarItem(modifier=Modifier.testTag("nav-$destination"),selected=route==destination || (destination=="worlds" && route in setOf("atlas","atlas-run/{id}","duel")),
     onClick={if(destination=="home") nav.popBackStack("home",false) else nav.navigate(destination){launchSingleTop=true;restoreState=true;popUpTo("home"){saveState=true}}},
     icon={DiscoveryIcon(if(destination=="diwan")"catalog" else destination)},label={Text(label,maxLines=1)})
   }
  } }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal=16.dp)) {
    if(!playing) Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text("لُغْزَدان",style=MaterialTheme.typography.titleLarge);TextButton(onClick={nav.navigate("catalog")},modifier=Modifier.testTag("nav-catalog")){Text("مكتبتي")};TextButton(onClick={nav.navigate("profile")},modifier=Modifier.testTag("nav-profile")){Text("ملفي")} }
    else Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
     TextButton(onClick={nav.popBackStack()},modifier=Modifier.weight(1f)){Text("العودة · تُحفظ حركاتك تلقائيًا")}
     Switch(checked=settings.sound,onCheckedChange=vm::sound,modifier=Modifier.semantics {contentDescription="أصوات اللعب"})
    }
    error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::initialize){Text("إعادة المحاولة")} }
    if(!ready) {if(error==null) {CircularProgressIndicator();Text("تهيئة المكتبة المحلية…")}}
    else NavHost(nav,startDestination="home",modifier=Modifier.weight(1f),enterTransition={fadeIn(tween(200))},exitTransition={fadeOut(tween(150))},popEnterTransition={fadeIn(tween(200))},popExitTransition={fadeOut(tween(150))}) {
     composable("home") {
      val sessions by vm.sessions.collectAsStateWithLifecycle()
      Page {
      DarDoors { nav.navigate("dar/$it") }
      Text("فضولك بداية الرحلة",style=MaterialTheme.typography.headlineLarge)
      Text("دقائق من اللعب، ومعنى جديد تكتشفه كل يوم.",color=MaterialTheme.colorScheme.onSurfaceVariant)
      Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
       Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.atlas_cover),contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().height(160.dp))
        Text("إلى أين يأخذك فضولك؟",style=MaterialTheme.typography.titleLarge)
        Text("ستة عوالم من اللغة والأدب والبيان واليمن والعلوم والمنطق.")
        Button(onClick={nav.navigate("atlas")},modifier=Modifier.fillMaxWidth()){Text("ابدأ رحلة الاكتشاف")}
       }
      }
      OutlinedCard(onClick={nav.navigate("diwan")},modifier=Modifier.fillMaxWidth().testTag("open-diwan")) {
       Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("ديوان العرب", style=MaterialTheme.typography.titleLarge)
        Text("عيون الشعر والنثر · اقرأ واختبر ذائقتك واحفظ مختاراتك")
       }
      }
      if(puzzles.any(LiteraryWords::supports)) OutlinedCard(onClick={nav.navigate("literary")},modifier=Modifier.fillMaxWidth()) {
       Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("جديد · ديوان الألغاز",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
        Text("أكمل الحروف أو خمن الكلمة · لغة وأدب وشعر وبلاغة")
        Text("اكتشف ${puzzles.count(LiteraryWords::supports)} تحديًا",style=MaterialTheme.typography.labelLarge)
       }
      }
      sessions.firstOrNull()?.let {record->
       OutlinedCard(onClick={nav.navigate("session/${record.session.id}")},modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)){Text("متابعة آخر نشاط",style=MaterialTheme.typography.titleMedium);Text(gameNames[record.puzzle.type] ?: "جولتك المحفوظة")}
       }
      }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
       Text("المستوى ${ProgressionRules.playerLevel(progress.totalXp)}")
       Text("${progress.completed} ألغاز مكتملة · ${progress.totalXp} خبرة",modifier=Modifier.testTag("home-progress"))
      }
      LinearProgressIndicator(progress={ (progress.totalXp%250)/250f },modifier=Modifier.fillMaxWidth())
      OutlinedCard(onClick={nav.navigate("daily")},modifier=Modifier.fillMaxWidth()) {
       Column(Modifier.padding(16.dp)){Text("موعدك مع تحدي اليوم",style=MaterialTheme.typography.titleMedium);Text("لغز ثابت طوال اليوم · تقدم محفوظ محليًا")}
      }
      Row {TextButton(onClick={nav.navigate("duel")}){Text("تحدّي صديق")};TextButton(onClick={nav.navigate("notebook")}){Text("دفتر اكتشافاتي")}}
      TextButton(onClick={nav.navigate("catalog")}){Text("ابدأ تحدي المفردات")}
     } }
     composable("atlas") {AtlasDestination(vm){nav.navigate(it)}}
     composable("atlas-run/{id}") {entry->
      val id=entry.arguments?.getString("id").orEmpty()
      val runFlow=remember(id){vm.observeRun(id)}
      val run by runFlow.collectAsStateWithLifecycle(initialValue=null)
      AtlasRunScreen(run,busy,onContinue={vm.continueAtlasRun(id){nav.navigate("session/$it")}},onAtlas={nav.navigate("atlas"){launchSingleTop=true}})
     }
     composable("duel") {
      DuelSetupScreen(atlasPuzzles,progress.totalXp,busy){first,second,pool->
       vm.createAtlasRun(AtlasRun(java.util.UUID.randomUUID().toString(),"duel","مواجهة $first و$second",pool,playerNames=listOf(first,second))){nav.navigate("atlas-run/$it")}
      }
     }
     composable("notebook") {NotebookScreen(discoveries,onReview={nav.navigate("review")},onLibrary={nav.navigate("vocabulary")})}
     composable("review") {vm.vocabularyRepository?.let {DiscoveryReviewScreen(discoveries,it){nav.navigate("notebook"){launchSingleTop=true}}}}
     composable("dar/{id}") { entry -> DarScene(entry.arguments?.getString("id").orEmpty(),{nav.navigate("diwan/read/$it")},{nav.popBackStack()}) }
     composable("diwan") { DiwanLibrary { if(it.startsWith("page-")) nav.navigate("dar/"+it.removePrefix("page-")) else nav.navigate("diwan/read/$it") } }
     composable("diwan/read/{work}") { entry ->
      DiwanReader(entry.arguments?.getString("work").orEmpty(), onBack={nav.popBackStack()}, onRead={nav.navigate("diwan/read/$it")})
     }
     composable("literary") {
      val recent by vm.sessions.collectAsStateWithLifecycle()
      LiteraryWordsScreen(atlasPuzzles,recent,progress.totalXp,busy,onPlay={play(it)},onResume={nav.navigate("session/$it")})
     }
     composable("worlds") {AtlasDestination(vm){nav.navigate(it)}}
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
      val visible=remember(puzzles,atlasPuzzles,filter,query){(if(query.isBlank())puzzles else atlasPuzzles).withIndex().filter {it.value.type in registry.types && (filter==null || it.value.type==filter) && (query.isBlank() || (it.value.title+" "+it.value.payload.prompt+" "+gameNames[it.value.type]).contains(query.trim(),ignoreCase=true))}}
      LazyColumn(modifier=Modifier.testTag("catalog-list"),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
       item {Text("مكتبة الاكتشاف",style=MaterialTheme.typography.headlineMedium)}
       item {Row {TextButton(onClick={nav.navigate("library")}){Text("الحزم والتنزيلات")};if(vm.vocabularyRepository!=null)TextButton(onClick={nav.navigate("vocabulary")}){Text("مفرداتي")}}}
       if(puzzles.any(LiteraryWords::supports))item {OutlinedButton(onClick={nav.navigate("literary")},modifier=Modifier.fillMaxWidth()){Text("ديوان الألغاز · الحروف والمعاني")}}
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
      if(record==null)Text("جارٍ استعادة الجولة. إن تعذر فتحها، تبقى بياناتها محفوظة في السجل.") else {
       val s=record.session;val p=record.puzzle;val terminal=s.status in setOf(SessionStatus.WON,SessionStatus.LOST)
       Column(Modifier.fillMaxSize()) {
       Page(Modifier.weight(1f)) {
       atlasPuzzles.firstOrNull {"supersedes:${p.puzzleId}" in it.tags}?.let {replacement->OutlinedButton(onClick={play(replacement.puzzleId)},enabled=!busy){Text("افتح النسخة المصححة من هذا اللغز")}}
       var priorSession by remember(s.id){mutableStateOf(s)}
       LaunchedEffect(s) {
        val cue=AnswerFeedback.cue(priorSession,s)
        if(settings.sound && cue!=null)runCatching {audio.play(cue)}
        if(settings.haptic && cue!=null)view.performHapticFeedback(if(cue==FeedbackCue.RETRY)HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY)
        priorSession=s
       }
       Text("${listOf("مبتدئ","سهل","متوسط","متقدم","خبير")[p.difficulty-1]} · ${if(terminal) "النتيجة" else "المحاولات المتبقية: ${3-s.wrongSubmissions}"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)

       if(s.atlasRunId.isNotBlank())atlasRuns.firstOrNull {it.id==s.atlasRunId}?.let {run->Text(if(run.kind=="duel") "جولة ${run.playerNames[(run.step-(if(terminal)1 else 0)).coerceAtLeast(0)%2]}" else run.title,color=MaterialTheme.colorScheme.primary)}
       Text(if(p.payload.variant in setOf("language_detective","who_am_i",WordBridge.VARIANT))AtlasCatalog.name(p) else if(LiteraryWords.supports(p))"ديوان الألغاز · "+if(p.payload.variant==LiteraryWords.COMPLETE)"أكمل الحروف" else "خمن الكلمة" else AtlasCatalog.name(p),style=MaterialTheme.typography.labelLarge)
       Text(p.payload.prompt.ifBlank {when(p.type){"anagram"->"كوّن كلمة من الحروف المتاحة";"order"->"رتّب البطاقات لتكوين النص";"crossword"->"حل الكلمات باستخدام القرائن";else->p.title}},style=MaterialTheme.typography.titleMedium)
       if(s.status==SessionStatus.PAUSED) {
        Text("حُفظت جولتك. استأنفها عندما تكون مستعدًا.")
        Button(onClick={vm.resume(s.id)},enabled=!busy){Text("استئناف الجولة")}
       } else if(!terminal) {
        val loader:suspend (String)->ByteArray?=remember(s.id) { {imageId->vm.readImage(s.id,imageId)} }
        CompositionLocalProvider(LocalGameImageLoader provides loader) { GameBoard(p,s,busy){vm.action(s.id,it)} }
        if(s.wrongSubmissions>0)FeedbackCard(false,"الإجابة السابقة غير صحيحة؛ راجع العلامات وحاول مرة أخرى.")
        p.hints.filter {it.id in s.usedHintIds}.forEach {hint->Text(s.revealedHints[hint.id] ?: hint.text)}
        TextButton(onClick={vm.action(s.id,PlayerAction.Pause(java.util.UUID.randomUUID().toString()))},enabled=!busy){Text("إيقاف مؤقت وحفظ")}
       } else {
        FeedbackCard(s.status==SessionStatus.WON,if(s.status==SessionStatus.WON)"إجابة صحيحة! +${s.score} نقطة" else "انتهت المحاولات؛ اكتشف الإجابة وتعلّم منها")
        if(p.type=="choice" || (p.type=="matching" && p.payload.options.isNotEmpty())) ChoiceAnswers(p,s,false){}
        if(p.type=="crossword")Crossword(p,s,true){}
        p.payload.options.firstOrNull {it.id==p.payload.correctId}?.let {Text("الإجابة: ${it.text}")}
        if(p.type in visualGameTypes) {
         val loader:suspend (String)->ByteArray?=remember(s.id) { {imageId->vm.readImage(s.id,imageId)} }
         CompositionLocalProvider(LocalGameImageLoader provides loader) {VisualSolution(p)}
        }
        if(LiteraryWords.supports(p)) {
         Text("الكلمة: ${p.payload.acceptedAnswers.first()}",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
         Text("${LiteraryWords.categories[p.payload.mode]} · معنى تستبقيه",style=MaterialTheme.typography.labelLarge)
        }
        Text(p.explanation)
        if(p.payload.sourceLabel.isNotBlank()) {
         Text(p.payload.sourceLabel,style=MaterialTheme.typography.bodySmall)
         val uri=LocalUriHandler.current
         if(p.payload.sourceUrl.isNotBlank())TextButton(onClick={runCatching {uri.openUri(p.payload.sourceUrl)}}){Text("قراءة المصدر") }
        } else Text(if(p.fixtureOnly) "المصدر: صياغة داخلية للاختبار؛ الاعتماد اللغوي معلق." else "المصدر: الحزمة العربية الأصلية · تفاصيل المراجع في المكتبة",style=MaterialTheme.typography.bodySmall)
        if(s.answerHistory.isNotEmpty())Text("إجاباتك: "+s.answerHistory.joinToString("، "))
        if(s.submittedIds.isNotEmpty())Text("إجاباتك: "+s.submittedIds.joinToString("، "){ key->p.payload.options.firstOrNull { it.id==key }?.text ?: key })
        if(s.atlasRunId.isNotBlank())Button(onClick={nav.navigate("atlas-run/${s.atlasRunId}")},modifier=Modifier.fillMaxWidth()){Text("متابعة الرحلة أو المواجهة")}
        if(s.atlasRunId.isBlank()) {
        val nextPool=puzzles.filter {it.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp) && (!LiteraryWords.supports(p) || LiteraryWords.supports(it))}
        if(nextPool.isNotEmpty())Button(onClick={play(nextPool[(nextPool.indexOfFirst {it.puzzleId==p.puzzleId}+1)%nextPool.size].puzzleId)},enabled=!busy){Text("اللغز التالي")}
        val different=remember(puzzles,s.id,progress.totalXp){puzzles.filter {it.type!=p.type && it.difficulty<=ProgressionRules.unlockedDifficulty(progress.totalXp)}.shuffled(kotlin.random.Random(s.id.hashCode())).firstOrNull()}
        different?.let { next->OutlinedButton(onClick={play(next.puzzleId)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("جرّب نمطًا مختلفًا · ${gameNames[next.type] ?: next.type}")} }
        if(LiteraryWords.supports(p))TextButton(onClick={nav.navigate("literary"){launchSingleTop=true}}){Text("اختر فئة أخرى من الديوان")}
        OutlinedButton(onClick={play(p.puzzleId)},enabled=!busy){Text("إعادة المحاولة")}
        }
        if(vm.vocabularyRepository!=null)OutlinedButton(onClick={vm.saveDiscovery(p)},enabled=discoveries.none {it.snapshot.id=="${p.puzzleId}:${p.revision}"},modifier=Modifier.fillMaxWidth()){Text(if(discoveries.any {it.snapshot.id=="${p.puzzleId}:${p.revision}"})"محفوظ في دفتر اكتشافاتي" else "احفظ هذا الاكتشاف")}
        TextButton(onClick={nav.navigate("history")}){Text("سجل الإجابات")}
       }
       }
       if(s.status==SessionStatus.ACTIVE) {
        Surface(tonalElevation=3.dp,modifier=Modifier.fillMaxWidth()) {
         Column(Modifier.padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
          Button(onClick={vm.submit(s.id)},enabled=(p.type!="choice"||s.selectedId!=null)&&(!LiteraryWords.supports(p)||LiteraryWords.complete(p,s))&&!busy&&!submitting,modifier=Modifier.fillMaxWidth()){Text(if(submitting)"جارٍ الحفظ…" else "تحقق من الإجابة")}
          if(LiteraryWords.supports(p)) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
           p.hints.firstOrNull {it.id=="clue" && it.id !in s.usedHintIds}?.let {hint->TextButton(onClick={vm.hint(s.id,hint.id)},enabled=!busy,modifier=Modifier.weight(1f)){Text("قرينة إضافية (−${hint.penalty})",style=MaterialTheme.typography.labelMedium)}}
           p.hints.firstOrNull {it.id.startsWith("letter-") && it.id !in s.usedHintIds}?.let {hint->TextButton(onClick={vm.hint(s.id,hint.id)},enabled=!busy,modifier=Modifier.weight(1f)){Text("اكشف حرفًا (−${hint.penalty})",style=MaterialTheme.typography.labelMedium)}}
          } else HintProvider.next(p,s)?.hint?.let {hint->TextButton(onClick={vm.hint(s.id,hint.id)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("تلميح (−${hint.penalty} نقطة)")}}
         }
        }
       }
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
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("اهتزاز اللعب");Switch(checked=settings.haptic,onCheckedChange=vm::haptic)}
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
@Composable internal fun Page(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) { Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content) }
