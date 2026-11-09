package com.lughzdan.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import com.lughzdan.preferences.SettingsStore
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DarDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val preferences get()=ui.activity.getSharedPreferences("dar-alathar-v1",0)
    private fun enter(id:String){
        ui.waitUntil(60_000){ui.onAllNodesWithTag("dar-$id").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("dar-$id").performScrollTo().performClick()
    }
    private fun tap(text:String){ui.onNodeWithText(text).performScrollTo().performClick();ui.waitForIdle()}
    private fun shot(name:String){
        val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(ui.activity.getExternalFilesDir(null),name+".png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)}
        image.recycle()
    }
    @Test fun inspectPoem(){
        enter("edge");tap("حدّه");tap("فصل");tap("نصل")
        Assert.assertEquals(1,preferences.getInt("edge:attempts",0))
        shot("dar-poem-light")
    }
    @Test fun restorePoem(){
        enter("edge");ui.onNodeWithText("ضع القرينة عند «الحدّ»").assertExists()
        tap("فصل");tap("بين الجد واللعب");tap("نهاية تفصل في النزاع")
        val page=preferences.getString("edge:page",null);Assert.assertNotNull(page)
        tap("تحدّي الإتقان · أعد الحل بطريق آخر");tap("الحدّ");tap("فصل");tap("نصل");tap("بين الجد واللعب");tap("نهاية تفصل في النزاع")
        Assert.assertEquals(page,preferences.getString("edge:page",null))
        tap("احفظ البيت في ديواني")
        Assert.assertTrue(ui.activity.getSharedPreferences("diwan-arab-v1",0).contains("saved:p24:0"))
    }
    @Test fun fourMoreDiscoveryPages(){
        val plans=listOf(
            listOf("elegy","أمنية مالك","ليت شعري","بوادي الغضى","أمنية مالك"),
            listOf("baghdad","النقود","غياب النقد","اشتهيت الأزاذ · وليس معي عقد على نقد","بين الرغبة والقدرة"),
            listOf("doves","خيط الشبكة","شبكة واحدة","تعاون جميعا","ينسّق الركاب دفعة واحدة"),
            listOf("echo","مسار الرجوع","زمن الذهاب","يقطع الصوت المسافة مرتين","340 مترًا")
        )
        for(plan in plans){
            enter(plan.first());plan.drop(1).forEach(::tap)
            Assert.assertNotNull(preferences.getString(plan.first()+":page",null))
            tap("العودة إلى الدار")
        }
    }
    @Test fun shadowBoard(){
        runBlocking {SettingsStore(ui.activity).dark(true)}
        enter("shade");tap("افحص الشاخص");tap("يقصر الظل");tap("ضع العلامة الأولى");tap("رفع الضوء")
        shot("dar-shadow-dark");tap("ضع العلامة الثانية")
        ui.onNode(hasSetTextAction()).performScrollTo().performTextInput("16.6")
        tap("ثبّت القياس");tap("20 سم");Assert.assertNotNull(preferences.getString("shade:page",null))
        runBlocking {SettingsStore(ui.activity).dark(false)}
    }
}
