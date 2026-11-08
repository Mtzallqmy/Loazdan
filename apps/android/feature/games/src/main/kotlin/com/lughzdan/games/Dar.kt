package com.lughzdan.games

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lughzdan.engine.DarMeasure
import java.util.Locale
import kotlin.math.roundToInt

private data class Exhibit(val id:String,val room:String,val title:String,val work:String,val clues:List<String>,val goal:String,val proof:String,val other:String,val transfer:String,val answer:String,val wrong:String)
private val exhibits=listOf(
    Exhibit("edge","مجلس الشعر","حدّ السيف وحدّ المعنى","p24",listOf("حدّه","الحدّ"),"استعد حاشية المخطوط بتعيين معنى كل كلمة.","بين الجد واللعب","أصدق أنباءً","عبارة نثرية من تأليف الفريق: شحذ الصانع حدّ أداته، ثم وضع حدًّا للنزاع. ما معنى الثانية؟","نهاية تفصل في النزاع","شفرة الأداة"),
    Exhibit("shade","المرصد","ظلّ في فناء تعز","",listOf("افحص الشاخص","افحص طرف الظل"),"أعد رسم العلامة المفقودة في لوح الفناء.","يقصر الظل","يطول الظل","ضاعفنا ارتفاع الشاخص إلى 20 سم وأثبتنا الزاوية عند 45°. ما طول الظل؟","20 سم","10 سم"),
    Exhibit("elegy","مجلس الشعر","صوتان للرثاء","p14",listOf("عين الخنساء","أمنية مالك"),"حدّد الصوت الذي يرجو العودة إلى موطنه.","بوادي الغضى","أم بالعين عوّار","في عبارة مؤلفة: ليتني أبيت بين أهلي. أي شاهد أقرب؟","أمنية مالك","سؤال الخنساء عن دمعها"),
    Exhibit("doves","الورّاق","شبكة الحمامات","n02",listOf("خيط الشبكة","أمر المطوّقة"),"استعد الجملة التي تجعل خلاص الجماعة ممكنًا.","تعاون جميعا","تنجُ كل حمامة وحدها","في حكاية جديدة علق زورق؛ أي قرار يحاكي حجة المطوّقة؟","ينسّق الركاب دفعة واحدة","يدفع كل راكب في اتجاه مختلف"),
    Exhibit("baghdad","الورّاق","ورقة بغداد","n14",listOf("الرغبة","النقود"),"اجمع الدافعين اللذين يُهيّئان للمفارقة.","اشتهيت الأزاذ · وليس معي عقد على نقد","دخلت البصرة في فتاء","في مشهد مؤلف يشتهي الراوي وليمة ويتحسّس كيسه الفارغ؛ أين المفارقة؟","بين الرغبة والقدرة","بين مدينتين"),
    Exhibit("echo","المرصد","مسافة الصدى","",listOf("مسار الذهاب","مسار الرجوع"),"احسب بعد جدار من زمن عودة النبضة. نفترض سرعة ثابتة 340 م/ث وجدارًا ساكنًا.","يقطع الصوت المسافة مرتين","يقطع الصوت المسافة مرة","عاد الصدى بعد ثانيتين. ما بعد الجدار؟","340 مترًا","680 مترًا")
)
private val manuscript=androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.dar_amiri))
private fun fmt(v:Double)=String.format(Locale.ROOT,"%.1f",v)
@Composable private fun darData():Pair<SharedPreferences,Map<String,*>> {
    val ctx=LocalContext.current.applicationContext
    val store=remember(ctx){ctx.getSharedPreferences("dar-alathar-v1",Context.MODE_PRIVATE)}
    var values by remember(store){mutableStateOf(store.all.toMap())}
    DisposableEffect(store){
        val listener=SharedPreferences.OnSharedPreferenceChangeListener { p,_->values=p.all.toMap() }
        store.registerOnSharedPreferenceChangeListener(listener);values=store.all.toMap()
        onDispose { store.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return store to values
}
@Composable internal fun DarDoors(open:(String)->Unit) {
    val (_,data)=darData()
    val recent=exhibits.find{it.id==data["last"]}
    Text("دار الأثر",style=MaterialTheme.typography.headlineLarge)
    if(recent!=null) FilledTonalButton(onClick={open(recent.id)},modifier=Modifier.fillMaxWidth()){
        Text("تابع رحلتك · ${recent.title} · ${data[recent.id+":clue"]?:"صفحة الدليل"}")
    }
    exhibits.groupBy {it.room}.forEach { (room,items)->
        Text(room,style=MaterialTheme.typography.titleMedium)
        items.forEach { e->OutlinedCard(onClick={open(e.id)},modifier=Modifier.fillMaxWidth().testTag("dar-"+e.id)){
            Text(e.title,Modifier.padding(14.dp),style=MaterialTheme.typography.titleLarge)
        }}
    }
}
@Composable internal fun DarPages(open:(String)->Unit) {
    val (_,data)=darData()
    exhibits.filter {data.containsKey(it.id+":page")}.forEach { e->
        TextButton(onClick={open(e.id)}){Text("صفحة الأثر · "+e.title)}
    }
}
@Composable internal fun DarScene(id:String,read:(String)->Unit,back:()->Unit) {
    val e=exhibits.firstOrNull{it.id==id}?:return
    val (prefs,d)=darData()
    val ctx=LocalContext.current
    val uri=LocalUriHandler.current
    val key="$id:"
    val step=d[key+"step"] as? Int?:0
    val angle=d[key+"angle"] as? Int?:30
    val clue=d[key+"clue"] as? String?:""
    val feedback=d[key+"feedback"] as? String?:""
    fun save(block:SharedPreferences.Editor.()->Unit){
        prefs.edit().apply{putString("last",id);block()}.apply()
    }
    fun next(){save{putInt(key+"step",step+1);remove(key+"feedback")}}
    fun miss(message:String){save{putString(key+"feedback",message);putInt(key+"attempts",(d[key+"attempts"] as? Int?:0)+1)}}
    val page=d[key+"page"] as? String
    val sound=remember(ctx,id){DarSound(ctx.applicationContext)}
    var speaking by remember {mutableStateOf(false)}
    DisposableEffect(sound){onDispose{sound.close()}}
    val effects=d["fx"]==true
    val voice=d["voice"]!=false
    val music=d["ambience"]==true
    LaunchedEffect(music,speaking){sound.room(music,speaking)}
    var heard by remember(id){mutableIntStateOf(step)}
    LaunchedEffect(step){if(step!=heard && effects)sound.play(if(step==5)2 else if(step==1)0 else 1);heard=step}
    Page {
        TextButton(onClick=back){Text("العودة إلى الدار")}
        Text(e.room,style=MaterialTheme.typography.labelLarge)
        Text(e.title,style=MaterialTheme.typography.headlineMedium)
        Text(e.goal)
        if(e.id=="shade"){
            Text("مشهد تخييلي · سطح أفقي وأشعة متوازية ثابتة السمت.")
            ShadowBoard(angle)
        }
        if(e.id=="edge"){
            WithReadings { readings,_,_ ->
                readings.find{it.id=="p24"}?.let { w->
                    val parts=w.text.first().split('\n')
                    val big=d[key+"large"]==true
                    BoxWithConstraints {
                        if(maxWidth>=600.dp) Row {parts.forEach{Text(it,Modifier.weight(1f),fontFamily=manuscript,fontSize=if(big)28.sp else 21.sp,lineHeight=42.sp)}}
                        else Column {parts.forEach{Text(it,fontFamily=manuscript,fontSize=if(big)28.sp else 21.sp,lineHeight=42.sp)}}
                    }
                    TextButton(onClick={save{putBoolean(key+"large",!big)}}){Text(if(big)"تصغير البيت" else "تكبير البيت")}
                    if(voice) LocalReadingVoice(w.text.first()){speaking=it}
                    val mark="saved:p24:0"
                    val book=ctx.getSharedPreferences("diwan-arab-v1",Context.MODE_PRIVATE)
                    TextButton(onClick={if(!book.contains(mark))book.edit().putLong(mark,System.currentTimeMillis()).apply();save{putString(key+"notice","حُفظ البيت في ديواني")}}){Text(d[key+"notice"] as? String?:"احفظ البيت في ديواني")}
                }
            }
        } else if(e.work.isNotEmpty()){
            WithReadings { readings,_,_->readings.find{it.id==e.work}?.let{Text(it.text.first(),style=MaterialTheme.typography.titleLarge)} }
        }
        if(id=="echo"){
            val seconds=d[key+"seconds"] as? Int?:1
            Text("زمن رجوع النبضة: $seconds ث · المسافة: ${seconds*170} م")
            LinearProgressIndicator(progress={seconds/4f},modifier=Modifier.fillMaxWidth())
            Row { listOf(-1,1).forEach{delta->TextButton(onClick={save{putInt(key+"seconds",(seconds+delta).coerceIn(1,4))}}){Text(if(delta>0)"زد الزمن" else "أنقص الزمن")} } }
        }
        if(step==0){
            Text("اختر قرينتك الأولى")
            e.clues.forEach { path->OutlinedButton(onClick={save{putString(key+"clue",path);putInt(key+"step",1);remove(key+"feedback")}}){Text(path)} }
        } else if(step==1){
            Text("قرينتك · $clue")
            if(id=="edge"){
                val first=clue=="حدّه"
                val second=d[key+"paired"]==true
                val blade=if(second)!first else first
                Text("ضع القرينة عند «"+(if(blade)"حدّه" else "الحدّ")+"»")
                listOf("فصل","نصل").forEach { option->OutlinedButton(onClick={
                    if((option=="نصل")==blade){if(second)next() else save{putBoolean(key+"paired",true);remove(key+"feedback")}}
                    else miss(if(blade)"الهاء تعود إلى السيف؛ افحص طرفه القاطع." else "انظر إلى «بين الجد واللعب»؛ موضع تمييز.")
                }){Text(option)} }
            }else{
                Text(if(id=="shade") "تنبّأ: ماذا يحدث لطول الظل عند رفع الضوء؟" else "أي شاهد يدعم تفسيرك؟")
                listOf(e.other,e.proof).forEach { s->OutlinedButton(onClick={if(s==e.proof)next() else miss("راجع قرينتك · "+e.goal)}){Text(s)} }
            }
        }else if(step==2){
            if(id=="shade"){
                Text("حرّك الضوء وسجّل طرف الظل عند زاويتين مختلفتين.")
                Slider(angle.toFloat(),{save{putInt(key+"angle",it.roundToInt())}},valueRange=20f..70f,steps=49)
                Row {listOf(-1,1).forEach{v->TextButton(onClick={save{putInt(key+"angle",(angle+v).coerceIn(20,70))}}){Text(if(v<0)"خفض الضوء" else "رفع الضوء")}}}
                val first=d[key+"mark"] as? Int
                TextButton(onClick={if(first==null)save{putInt(key+"mark",angle)} else if(first!=angle)save{putInt(key+"mark2",angle);putInt(key+"step",3);remove(key+"feedback")} else miss("غيّر الزاوية قبل وضع العلامة الثانية.")}){Text(if(first==null)"ضع العلامة الأولى" else "ضع العلامة الثانية")}
                first?.let{Text("العلامة الأولى · ${fmt(DarMeasure.shadow(10.0,it))} سم عند $it°")}
            }else{
                Text(if(id=="edge")"اختر الشاهد الذي يبيّن معنى الفصل." else "ثبّت دليلك في الصفحة.")
                listOf(e.proof,e.other).forEach{s->OutlinedButton(onClick={if(s==e.proof)next() else miss("ابحث عن القرينة المرتبطة بغاية المشهد.")}){Text(s)}}
            }
        }else if(step==3 && id=="shade"){
            val target=d[key+"mark2"] as Int
            Text("أعد رسم علامة اللوح عند $target°، بارتفاع شاخص 10 سم. هامش القبول ±0.5 سم.")
            val value=d[key+"length"] as? String?:""
            OutlinedTextField(value,{save{putString(key+"length",it)}},label={Text("المسافة بالسنتيمتر")})
            Button(onClick={val number=value.map{it.digitToIntOrNull()?.digitToChar()?:it}.joinToString("").replace('٫','.').toDoubleOrNull()
                if(number!=null && DarMeasure.matches(number,10.0,target))next() else miss("قِس من قاعدة الشاخص إلى الطرف.")}){Text("ثبّت القياس")}
        }else if(step<5){
            Text(e.transfer,style=MaterialTheme.typography.titleLarge)
            listOf(e.wrong,e.answer).forEach{s->OutlinedButton(onClick={
                if(s==e.answer)save{putInt(key+"step",5);remove(key+"feedback");if(page==null)putString(key+"page","بدأت بفحص «$clue». الشاهد: ${e.proof}. التطبيق: ${e.answer}.")}
                else miss("أعد قراءة الشاهد ثم طبّقه على المثال الجديد.")
            }){Text(s)}}
        }else{
            Text("صفحة مكتشفة",style=MaterialTheme.typography.titleLarge);Text(page.orEmpty())
            if(e.work.isNotEmpty())TextButton(onClick={read(e.work)}){Text("افتح المختار في ديواني")}
            OutlinedButton(onClick={save{putInt(key+"step",0);remove(key+"mark");remove(key+"mark2");remove(key+"paired");remove(key+"feedback")}}){Text("تحدّي الإتقان · أعد الحل بطريق آخر")}
        }
        if(feedback.isNotEmpty())Text(feedback,color=MaterialTheme.colorScheme.error)
        listOf(Triple("fx","المؤثرات",effects),Triple("voice","الإلقاء الآلي",voice),Triple("ambience","خلفية المجلس",music)).forEach{(k,label,on)->
            FilterChip(on,{save{putBoolean(k,!on)}},{Text(label+if(on)" · مفعّل" else " · متوقف")})
        }
        if(id=="shade")TextButton(onClick={runCatching{uri.openUri("https://openstax.org/books/precalculus-2e/pages/5-4-right-triangle-trigonometry")}}){Text("مرجع النموذج · OpenStax")}
    }
}
@Composable private fun ShadowBoard(angle:Int){
    val length=DarMeasure.shadow(10.0,angle)
    val description="ارتفاع الشاخص 10 سم · زاوية الضوء $angle° · طول الظل ${fmt(length)} سم"
    val ink=MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxWidth().height(148.dp).semantics { contentDescription=description }) {
        val unit=minOf(size.width/34f,size.height/13f)
        val foot=Offset(size.width*.1f,size.height*.88f)
        val top=foot.copy(y=foot.y-10*unit)
        val end=foot.copy(x=foot.x+length.toFloat()*unit)
        drawLine(ink,Offset(0f,foot.y),Offset(size.width,foot.y),2f)
        drawLine(ink,foot,top,5f);drawLine(Color(0xffdd9a29),top,end,3f)
        drawLine(Color(0xff288b80),foot,end,7f);drawCircle(ink,5f,end)
    }
    Text(description)
}
