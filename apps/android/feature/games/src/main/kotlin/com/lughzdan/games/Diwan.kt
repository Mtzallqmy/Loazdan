package com.lughzdan.games

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal data class ReadingQuestion(val prompt: String, val options: List<String>, val correct: Int, val explanation: String)
internal data class Reading(
    val id: String, val title: String, val author: String, val theme: String, val era: String,
    val text: List<String>, val note: String, val source: String, val questions: List<ReadingQuestion>,
    val companion: String, val companionNote: String, val companionSource: String
)
private fun JSONArray.strings() = List(length()) { getString(it) }
private object DiwanCatalog {
    private var cached: List<Reading>? = null
    @Synchronized fun load(context: Context): List<Reading> = cached ?: run {
        val json = JSONArray(context.assets.open("diwan/readings.json").bufferedReader().use { it.readText() })
        List(json.length()) { index ->
            val r = json.getJSONObject(index)
            val quiz = r.getJSONArray("questions")
            Reading(r.getString("id"), r.getString("title"), r.getString("author"), r.getString("theme"), r.getString("era"),
                r.getJSONArray("text").strings(), r.getString("note"), r.getString("source"), List(quiz.length()) { n ->
                    val q = quiz.getJSONObject(n)
                    ReadingQuestion(q.getString("prompt"), q.getJSONArray("options").strings(), q.getInt("correct"), q.getString("explanation"))
                }, r.optString("companion"), r.optString("companionNote"), r.optString("companionSource"))
        }
    }.also { cached = it }
}
@Composable internal fun WithReadings(content: @Composable (List<Reading>, SharedPreferences, Map<String, *>) -> Unit) {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) { context.getSharedPreferences("diwan-arab-v1", Context.MODE_PRIVATE) }
    var values by remember(prefs) { mutableStateOf(prefs.all.toMap()) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> values = p.all.toMap() }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        values = prefs.all.toMap()
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val data by produceState<Result<List<Reading>>?>(null, context) {
        value = withContext(Dispatchers.IO) { runCatching { DiwanCatalog.load(context) } }
    }
    val result = data
    when {
        result == null -> CircularProgressIndicator()
        result.isFailure -> Text("تعذّرت قراءة ديوان العرب.")
        else -> content(result.getOrThrow(), prefs, values)
    }
}
private val marks = Regex("[\\p{M}ـ]")
private fun String.searchKey() = replace(marks,"").replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي')
private fun mark(id: String, line: Int) = "saved:$id:$line"
private const val DAY = 86_400_000L

@Composable internal fun DiwanLibrary(onRead: (String) -> Unit) = WithReadings { works, _, values ->
    var filter by rememberSaveable { mutableStateOf("الكل") }
    var query by rememberSaveable { mutableStateOf("") }
    val filters = remember(works) { listOf("الكل","ديواني","موعد المراجعة") + works.map { it.theme }.distinct() }
    val now = System.currentTimeMillis()
    val visible = remember(works, filter, query, values) { works.filter { work ->
        val dates = work.text.indices.mapNotNull { values[mark(work.id,it)] as? Long }
        val matches = when (filter) {
            "الكل" -> true
            "ديواني" -> dates.isNotEmpty()
            "موعد المراجعة" -> dates.any { it <= now }
            else -> work.theme == filter
        }
        matches && (work.title + " " + work.author + " " + work.era + " " + work.text.joinToString(" ")).searchKey().contains(query.trim().searchKey())
    } }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=20.dp)) {
        item { DarPages { onRead("page-"+it) } }
        item { Text("ديوان العرب", style=MaterialTheme.typography.headlineMedium) }
        item { OutlinedTextField(query,{query=it},label={Text("ابحث بشاعر أو بكلمات من النص")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            filters.forEach { name -> FilterChip(filter==name,{filter=name},{Text(name)}) }
        } }
        if (visible.isEmpty()) item { Text(when(filter) {
            "ديواني" -> "افتح نصًا واحفظ منه بيتًا أو مقطعًا ليظهر هنا."
            "موعد المراجعة" -> "لا توجد مختارات مستحقة الآن."
            else -> "لم نعثر على نص يطابق البحث."
        }) }
        items(visible,key={it.id}) { work ->
            OutlinedCard(onClick={onRead(work.id)},modifier=Modifier.fillMaxWidth().testTag("reading-${work.id}")) {
                Column(Modifier.padding(14.dp)) {
                    Text(work.title,style=MaterialTheme.typography.titleLarge)
                    Text("${work.author} · ${work.theme}",style=MaterialTheme.typography.bodyMedium)
                    val done=work.questions.indices.count { values["solved:${work.id}:$it"] == true }
                    if(done>0) Text("أجبت عن $done من ${work.questions.size}",style=MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable internal fun DiwanReader(id: String, onBack: () -> Unit, onRead: (String) -> Unit) = WithReadings { works,prefs,values ->
    val work=works.firstOrNull { it.id==id }
    if(work==null) { TextButton(onClick=onBack){Text("العودة إلى الديوان")}; return@WithReadings }
    val uri=LocalUriHandler.current
    var mode by rememberSaveable(id) { mutableStateOf("النص") }
    var n by rememberSaveable(id) { mutableIntStateOf(0) }
    var attempt by rememberSaveable(id,n) { mutableIntStateOf(-1) }
    var revealed by rememberSaveable(id) { mutableIntStateOf(-1) }
    Page {
        TextButton(onClick=onBack) {Text("العودة إلى الديوان")}
        Text(work.title,style=MaterialTheme.typography.headlineMedium)
        Text("${work.author} · ${work.era} · ${work.theme}",color=MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("النص","اختبر نفسك","تسميع").forEach { label -> FilterChip(mode==label,{mode=label},{Text(label)}) }
        }
        when(mode) {
            "النص" -> {
                work.text.forEachIndexed { i,line ->
                    val key=mark(id,i)
                    OutlinedCard(modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                        Text(line,style=MaterialTheme.typography.titleLarge)
                        TextButton(onClick={
                            if(values.containsKey(key)) prefs.edit().remove(key).apply()
                            else prefs.edit().putLong(key,System.currentTimeMillis()).apply()
                        }) { Text(if(values.containsKey(key)) "إزالة من ديواني" else "احفظ في ديواني") }
                    } }
                }
                LocalReadingVoice(work.text.joinToString("، "))
                Text(work.note)
                TextButton(onClick={runCatching {uri.openUri(work.source)}}) {Text("اقرأ المصدر")}
                if(work.companionNote.isNotBlank()) Text(work.companionNote)
                if(work.companionSource.isNotBlank()) TextButton(onClick={runCatching { uri.openUri(work.companionSource) }}) {Text("قراءة المقارنة الأدبية")}
                works.find { it.id == work.companion }?.let { next ->
                    OutlinedButton(onClick={onRead(next.id)}) {Text("اقرأ معه · ${next.title}")}
                }
            }
            "اختبر نفسك" -> {
                val q=work.questions[n]
                val key="solved:$id:$n"
                val solved=values[key]==true
                Text("سؤال ${n+1} من ${work.questions.size}")
                Text(q.prompt,style=MaterialTheme.typography.titleLarge)
                q.options.forEachIndexed { idx,option ->
                    OutlinedButton(enabled=!solved,onClick={
                        attempt=idx
                        if(idx==q.correct) prefs.edit().putBoolean(key,true).apply()
                    },modifier=Modifier.fillMaxWidth()) {Text(option)}
                }
                if(solved) Text("✓ ${q.options[q.correct]}\n${q.explanation}")
                else if(attempt>=0) Text("راجع النص وجرّب إجابة أخرى.")
                Row {
                    if(n>0) TextButton(onClick={n--}) {Text("السابق")}
                    if(n<work.questions.lastIndex) TextButton(onClick={n++}) {Text("التالي")}
                    else TextButton(onClick=onBack) {Text("العودة إلى الديوان")}
                }
            }
            else -> {
                val saved=work.text.indices.filter { values.containsKey(mark(id,it)) }
                if(saved.isEmpty()) Text("احفظ بيتًا أو مقطعًا من تبويب النص لتبدأ التسميع.")
                saved.forEach { i ->
                    val line=work.text[i]
                    Text("المطلع · ${line.substringBefore(' ')} …")
                    if(revealed==i) {
                        Text(line,style=MaterialTheme.typography.titleLarge)
                        Row {
                            listOf("أتقنته" to 3,"أراجعه غدًا" to 1).forEach { (label,days) ->
                                TextButton(onClick={prefs.edit().putLong(mark(id,i),System.currentTimeMillis()+days*DAY).apply();revealed=-1}) {Text(label)}
                            }
                        }
                    } else OutlinedButton(onClick={revealed=i}) {Text("اعرض النص للتحقق")}
                }
            }
        }
    }
}

@Composable internal fun LocalReadingVoice(text: String) {
    val context=LocalContext.current.applicationContext
    var engine by remember {mutableStateOf<TextToSpeech?>(null)}
    var available by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    DisposableEffect(context) {
        var closed=false
        val handler=Handler(Looper.getMainLooper())
        val tts=TextToSpeech(context) { status ->
            handler.post {
                if(!closed && status==TextToSpeech.SUCCESS) {
                    val voice=engine?.voices.orEmpty().filter { it.locale.language=="ar" && !it.isNetworkConnectionRequired }
                        .maxByOrNull {it.quality}
                    if(voice!=null) available=engine?.setVoice(voice)==TextToSpeech.SUCCESS
                }
            }
        }
        engine=tts
        onDispose { closed=true;tts.stop();tts.shutdown();engine=null }
    }
    OutlinedButton(enabled=available,onClick={
        message=if(engine?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"diwan") == TextToSpeech.ERROR) "تعذّرت القراءة الصوتية." else ""
    }) {Text(if(available) "استمع بصوت الجهاز" else "يلزم صوت عربي مثبّت للاستماع دون إنترنت")}
    if(message.isNotEmpty()) Text(message)
}
