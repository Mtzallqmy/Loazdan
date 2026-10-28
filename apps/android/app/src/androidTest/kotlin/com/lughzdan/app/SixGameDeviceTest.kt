package com.lughzdan.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SixGameDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun click(text:String) {compose.waitUntil(30_000){compose.onAllNodesWithText(text).filter(isEnabled()).fetchSemanticsNodes().isNotEmpty()};compose.onAllNodesWithText(text).filter(isEnabled()).onFirst().performScrollTo().performClick();compose.waitForIdle()}
 private fun cell(text:String) {compose.waitUntil(30_000){compose.onAllNodesWithContentDescription(text).filter(isEnabled()).fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithContentDescription(text).performScrollTo().performClick();compose.waitForIdle()}
 private fun fixtures(): List<JSONObject> {
  val assets = compose.activity.assets
  fun read(path: String): List<JSONObject> {
   return assets.list(path).orEmpty().flatMap { name ->
    val full = if (path.isEmpty()) name else "$path/$name"
    when {
     name.endsWith(".jsonl") -> assets.open(full).bufferedReader().useLines { lines ->
      lines.filter { it.isNotBlank() }.map { JSONObject(it) }.toList()
     }
     !name.contains('.') -> read(full)
     else -> emptyList()
    }
   }
  }
  return read("")
 }
 private fun fixture(type:String)=fixtures().first {it.getString("type")==type}
 private fun open(type:String) {val all=fixtures();val p=fixture(type);click("ابدأ تحدي المفردات");click("لغز ${all.indexOfFirst {it.getString("puzzleId")==p.getString("puzzleId")}+1} · الصعوبة ${p.getInt("difficulty")}")}
 private fun screenshot(name:String){val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(compose.activity.getExternalFilesDir(null),"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 private fun won(){compose.waitUntil(30_000){compose.onAllNodesWithText("إجابة صحيحة!",substring=true).fetchSemanticsNodes().isNotEmpty()}}
 private fun assertPartial(){val e=fixture("crossword").getJSONObject("payload").getJSONArray("entries").getJSONObject(0);val token=e.getJSONArray("tokens").getString(0);val desc="خلية ${e.getInt("row")} ${e.getInt("col")}";compose.waitUntil(30_000){compose.onAllNodesWithContentDescription(desc).filter(hasText(token,substring=true)).fetchSemanticsNodes().isNotEmpty()}}
 private fun crossword(partial:Boolean) {val entries=fixture("crossword").getJSONObject("payload").getJSONArray("entries");for(i in 0 until entries.length()){val e=entries.getJSONObject(i);val tokens=e.getJSONArray("tokens");for(j in 0 until tokens.length()){if(partial&&(i!=0||j!=0))return;val r=e.getInt("row")+if(e.getString("direction")=="down")j else 0;val c=e.getInt("col")-if(e.getString("direction")=="across")j else 0;cell("خلية $r $c");cell("مفتاح ${tokens.getString(j)}")}}}
 @Test fun phaseThreeCrosswordPartial(){open("crossword");crossword(true);assertPartial();screenshot("stage03-crossword-light")}
 @Test fun phaseFourRestoreCrosswordAndBuilder(){
  click("متابعة آخر نشاط");click("استئناف الجولة");assertPartial();crossword(false);click("تحقق من الإجابة");won()
  click("الرئيسية");open("anagram");val p=fixture("anagram");val hint=p.getJSONArray("hints").getJSONObject(0);click("تلميح (−${hint.getInt("penalty")} نقطة)")
  val payload=p.getJSONObject("payload");val tiles=payload.getJSONArray("tiles");val remaining=payload.getJSONArray("acceptedAnswers").getString(0).toMutableList();val fixed=payload.optJSONArray("fixedSlots");val used=mutableSetOf<Int>()
  if(fixed!=null)for(i in fixed.length()-1 downTo 0){val f=fixed.getJSONObject(i);remaining.removeAt(f.getInt("index"));used+=(0 until tiles.length()).first {it !in used&&tiles.getString(it)==f.getString("token")}}
  remaining.forEach {token->val index=(0 until tiles.length()).first {it !in used&&tiles.getString(it)==token.toString()};used+=index;cell("حرف $index ${tiles.getString(index)}")};click("تحقق من الإجابة");won();screenshot("stage03-builder-light");click("الرئيسية");compose.waitUntil(30_000){compose.onAllNodesWithText("3 ألغاز مكتملة",substring=true).fetchSemanticsNodes().isNotEmpty()};val progress=compose.onNodeWithText("ألغاز مكتملة",substring=true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString {it.text};File(compose.activity.getExternalFilesDir(null),"stage03-progress.txt").writeText(progress)
 }
 @Test fun phaseFiveVerifyBuilderResult(){val previous=File(compose.activity.getExternalFilesDir(null),"stage03-progress.txt").readText();compose.waitUntil(30_000){compose.onAllNodesWithText(previous).fetchSemanticsNodes().isNotEmpty()};click("متابعة آخر نشاط");won();val p=fixture("anagram");val score=50+50*p.getInt("difficulty")-p.getJSONArray("hints").getJSONObject(0).getInt("penalty");compose.onNodeWithText("إجابة صحيحة! +$score نقطة").assertExists();click("الرئيسية");compose.onNodeWithText(previous).assertExists()}
 @Test fun phaseSixOtherBoards(){
  open("wordsearch");val words=fixture("wordsearch").getJSONObject("payload").getJSONArray("words");for(i in 0 until words.length()){val path=words.getJSONObject(i).getJSONArray("path");val first=path.getJSONObject(0);val last=path.getJSONObject(path.length()-1);cell("بحث ${first.getInt("row")} ${first.getInt("col")}");cell("بحث ${last.getInt("row")} ${last.getInt("col")}");click("تحقق من الإجابة")};won()
  click("الرئيسية");open("matching");val m=fixture("matching").getJSONObject("payload");val pairs=m.getJSONArray("pairs");fun text(side:String,id:String):String {val list=m.getJSONArray(side);return (0 until list.length()).map {list.getJSONObject(it)}.first {it.getString("id")==id}.getString("text")};for(i in 0 until pairs.length()){val pair=pairs.getJSONObject(i);click(text("left",pair.getString("leftId")));click(text("right",pair.getString("rightId")))};click("تحقق من الإجابة");won()
  click("الرئيسية");open("order");val o=fixture("order").getJSONObject("payload");val items=o.getJSONArray("items");val current=(0 until items.length()).map {items.getJSONObject(it).getString("id")}.toMutableList();val correct=o.getJSONArray("correctOrder");for(i in 0 until correct.length()){val id=correct.getString(i);while(current.indexOf(id)>i){val index=current.indexOf(id);val label=(0 until items.length()).map {items.getJSONObject(it)}.first {it.getString("id")==id}.getString("text");cell("لأعلى $label");current.removeAt(index);current.add(index-1,id)}};click("تحقق من الإجابة");won();screenshot("stage03-order-light");click("الإعدادات");compose.onAllNodes(isToggleable()).onFirst().performClick();screenshot("stage03-settings-dark")
 }
}
