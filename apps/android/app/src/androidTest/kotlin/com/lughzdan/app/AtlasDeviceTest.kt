package com.lughzdan.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lughzdan.engine.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AtlasDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) {
        try {compose.waitUntil(30_000) { compose.onAllNodesWithText(text).filter(isEnabled()).fetchSemanticsNodes().isNotEmpty() }} catch(error:Throwable) {shot("atlas-failure");println("Waiting for: $text");println(compose.onRoot().printToString());throw error}
        val node=compose.onAllNodesWithText(text).filter(isEnabled()).onFirst()
        if(text !in setOf("الرئيسية","العوالم","مكتبتي","ملفي"))node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }
    private fun shot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun currentRun(): AtlasRun = runBlocking { compose.activity.repository.atlasRuns.first().first { !it.finished } }
    private fun solve() {
        val run = currentRun()
        val record = runBlocking { compose.activity.repository.observeSession(run.activeSessionId!!).first()!! }
        val p = record.puzzle
        when (p.type) {
            "choice" -> compose.onNodeWithTag("answer-${p.payload.correctId}").performScrollTo().performClick()
            "text_answer" -> when {
                WordBridge.supports(p) -> {
                    val path = WordBridge.route(p, WordBridge.start(p))!!
                    for (word in path.drop(1)) { click(word); click("تحقق من الإجابة") }
                    return
                }
                p.payload.variant==LiteraryWords.COMPLETE -> {
                    val fixed = p.payload.fixedSlots.map { it.index }
                    p.payload.acceptedAnswers.first().forEachIndexed { index, token ->
                        if (index !in fixed) {
                            compose.onNodeWithContentDescription("خانة ${index + 1}").performScrollTo().performClick()
                            compose.onNodeWithContentDescription("حرف الديوان $token").performScrollTo().performClick()
                        }
                    }
                }
                LiteraryWords.supports(p) -> compose.onNodeWithTag("literary-answer").performScrollTo().performTextInput(p.payload.acceptedAnswers.first())
                else -> compose.onNodeWithText("اكتب إجابتك بالعربية").performScrollTo().performTextInput(p.payload.acceptedAnswers.first())
            }
            "anagram" -> {
                val used = mutableSetOf<Int>()
                p.payload.acceptedAnswers.first().forEach { token ->
                    val index = p.payload.tiles.indices.first { it !in used && p.payload.tiles[it] == token.toString() }
                    used.add(index)
                    compose.onNodeWithContentDescription("حرف $index $token").performScrollTo().performClick()
                }
            }
            "sequence" -> compose.onNodeWithText("العدد التالي (أرقام عربية أو لاتينية)").performScrollTo().performTextInput(p.payload.answer.toString())
            else -> error("Unexpected journey board ${p.type}")
        }
        click("تحقق من الإجابة")
    }

    @Test fun journeyNotebookAndDuelPersistBeforeProcessKill() {
        click("الرئيسية")
        click("ابدأ رحلة الاكتشاف")
        click("مدينة اللغة")
        click("المفردات")
        shot("atlas-journey-start")
        repeat(5) { stage ->
            click("ابدأ المحطة ${stage + 1}")
            if (stage == 0) shot("atlas-playing")
            solve()
            if (stage == 4) {
                click("احفظ هذا الاكتشاف")
                compose.waitUntil(30_000) { compose.onAllNodesWithText("محفوظ في دفتر اكتشافاتي").fetchSemanticsNodes().isNotEmpty() }
            }
            click("متابعة الرحلة أو المواجهة")
        }
        shot("atlas-journey-complete")
        click("العودة إلى الأطلس")
        click("دفتر اكتشافاتي")
        click("مراجعة من خمس بطاقات")
        click("أظهر المعنى")
        click("تذكرتها")
        click("العودة إلى الدفتر")
        click("الرئيسية")
        click("تحدّي صديق")
        click("ابدأ المواجهة")
        repeat(2) { stage -> click("ابدأ المحطة ${stage + 1}"); solve(); click("متابعة الرحلة أو المواجهة") }
        val run = currentRun()
        val xp = runBlocking { compose.activity.repository.progress.first().totalXp }
        File(compose.activity.filesDir, "atlas-duel.txt").writeText("${run.id}\n$xp")
        shot("atlas-duel-handoff")
    }

    @Test fun duelRestoresOfflineWithoutChangingPlayerExperience() {
        click("الرئيسية")
        click("ابدأ رحلة الاكتشاف")
        val saved = File(compose.activity.filesDir, "atlas-duel.txt").readLines()
        val run = currentRun()
        assertEquals(saved[0], run.id)
        assertEquals(2, run.step)
        click("متابعة ${run.title} · ${run.step}/${run.puzzleIds.size}")
        for (stage in run.step until run.puzzleIds.size) { click("ابدأ المحطة ${stage + 1}"); solve(); click("متابعة الرحلة أو المواجهة") }
        val result = runBlocking { compose.activity.repository.observeRun(run.id).first()!! }
        assertTrue(result.finished)
        assertEquals(saved[1].toInt(), runBlocking { compose.activity.repository.progress.first().totalXp })
        shot("atlas-duel-result")
    }
}
