package com.lughzdan.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
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
        compose.waitForIdle()
        if(compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() && compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text))
        }
        try {compose.waitUntil(30_000) { compose.onAllNodesWithText(text).filter(isEnabled()).fetchSemanticsNodes().isNotEmpty() }} catch(error:Throwable) {shot("atlas-failure");println("Waiting for: $text");println(compose.onRoot().printToString());runBlocking {compose.activity.repository.initialize()};throw error}
        val node=compose.onAllNodesWithText(text).filter(isEnabled()).onFirst()
        if(text !in setOf("الرئيسية","العوالم","مكتبتي","ملفي") && text!="تحقق من الإجابة" && !text.startsWith("تلميح") && !text.startsWith("اكشف حرفًا") && !text.startsWith("قرينة إضافية"))node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }
    private fun shot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun currentRun(): AtlasRun {
        var result:AtlasRun?=null
        compose.waitUntil(30_000) {
            result=runBlocking {compose.activity.repository.atlasRuns.first().firstOrNull {!it.finished && it.activeSessionId!=null}}
            result!=null
        }
        return result!!
    }
    private fun clickTile(description: String) {
        val page = compose.onNode(SemanticsMatcher("vertical game page") {
            it.config.contains(SemanticsProperties.VerticalScrollAxisRange)
        })
        val tile = compose.onNodeWithContentDescription(description)
        repeat(4) {
            val viewport = page.fetchSemanticsNode().boundsInRoot
            val target = tile.fetchSemanticsNode()
            val top = target.positionInRoot.y
            val bottom = top + target.size.height
            val delta = when {
                top < viewport.top -> top - viewport.top
                bottom > viewport.bottom -> bottom - viewport.bottom
                else -> 0f
            }
            if (kotlin.math.abs(delta) > 1f) {
                page.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, delta) }
                compose.waitForIdle()
            }
        }
        tile.performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
    }
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
                LiteraryWords.supports(p) -> compose.onNodeWithTag("literary-answer").performScrollTo().performTextReplacement(p.payload.acceptedAnswers.first())
                else -> compose.onNodeWithText("اكتب إجابتك بالعربية").performScrollTo().performTextReplacement(p.payload.acceptedAnswers.first())
            }
            "anagram","four_pictures" -> {
                val used = mutableSetOf<Int>()
                p.payload.acceptedAnswers.first().forEach { token ->
                    val index = p.payload.tiles.indices.first { it !in used && p.payload.tiles[it] == token.toString() }
                    used.add(index)
                    clickTile("حرف $index $token")
                }
            }
            "sequence" -> compose.onNodeWithText("العدد التالي (أرقام عربية أو لاتينية)").performScrollTo().performTextReplacement(p.payload.answer.toString())
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
        click("ابدأ المحطة 3")
        val run = currentRun()
        val active=runBlocking {compose.activity.repository.observeSession(run.activeSessionId!!).first()!!}
        if(active.puzzle.type=="choice")compose.onNodeWithTag("answer-${active.puzzle.payload.correctId}").performScrollTo().performClick()
        else if(active.puzzle.payload.variant==LiteraryWords.COMPLETE) {
            val slot=active.puzzle.payload.acceptedAnswers.first().indices.first {i->active.puzzle.payload.fixedSlots.none {it.index==i}}
            compose.onNodeWithContentDescription("خانة ${slot+1}").performScrollTo().performClick()
            compose.onNodeWithContentDescription("حرف الديوان ${active.puzzle.payload.acceptedAnswers.first()[slot]}").performScrollTo().performClick()
        } else if(WordBridge.supports(active.puzzle))click(WordBridge.route(active.puzzle,WordBridge.start(active.puzzle))!![1])
        else if(active.puzzle.type=="sequence")compose.onNodeWithText("العدد التالي (أرقام عربية أو لاتينية)").performScrollTo().performTextReplacement("1")
        else if(LiteraryWords.supports(active.puzzle))compose.onNodeWithTag("literary-answer").performScrollTo().performTextReplacement("ق")
        else compose.onNodeWithText("اكتب إجابتك بالعربية").performScrollTo().performTextReplacement("ق")
        compose.waitUntil(30_000){runBlocking {compose.activity.repository.observeSession(run.activeSessionId!!).first()!!.session.processedActions.isNotEmpty()}}
        val partial=runBlocking {compose.activity.repository.observeSession(run.activeSessionId!!).first()!!.session}
        val xp = runBlocking { compose.activity.repository.progress.first().totalXp }
        File(compose.activity.filesDir, "atlas-duel.txt").writeText("${run.id}\n$xp\n${SessionCodec.encode(partial)}")
        shot("atlas-duel-handoff")
    }

    @Test fun signedVisualCluesDecodeAndDrawOffline() {
        click("الرئيسية")
        val puzzles=runBlocking {compose.activity.repository.atlasCatalog.first().filter {it.type=="four_pictures" && it.tags.any {tag->tag.startsWith("supersedes:")}}}
        assertEquals(4,puzzles.size)
        val run=AtlasRun("visual-device-${System.nanoTime()}","journey","رحلة القرائن البصرية",puzzles.map {it.puzzleId})
        runBlocking {compose.activity.repository.createRun(run)}
        click("ابدأ رحلة الاكتشاف");click("متابعة ${run.title} · 0/4")
        val evidence=StringBuilder()
        repeat(4) {stage->
            click("ابدأ المحطة ${stage+1}")
            val current=currentRun()
            val record=runBlocking {compose.activity.repository.observeSession(current.activeSessionId!!).first()!!}
            for(image in record.puzzle.payload.images) {
                val bytes=runBlocking {compose.activity.repository.readImage(record.session.id,image.id)!!}
                val bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)!!
                assertEquals(image.width,bitmap.width);assertEquals(image.height,bitmap.height)
                val canvas=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(canvas).drawBitmap(bitmap,null,android.graphics.Rect(0,0,192,192),null)
                val pixels=IntArray(192*192);canvas.getPixels(pixels,0,192,0,0,192,192)
                assertTrue("Expected real colored scene",pixels.toSet().size>8)
                evidence.append("${image.id} ${bitmap.width}x${bitmap.height} colors=${pixels.toSet().size}\n")
                bitmap.recycle();canvas.recycle()
            }
            compose.waitUntil(30_000){compose.onAllNodesWithContentDescription(record.puzzle.payload.images.first().alt).fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription(record.puzzle.payload.images.first().alt).performScrollTo().assertIsDisplayed()
            if(stage==0)shot("atlas-four-pictures")
            solve();click("متابعة الرحلة أو المواجهة")
        }
        File(compose.activity.getExternalFilesDir(null),"atlas-image-render.txt").writeText(evidence.toString())
    }

    @Test fun scaledAtlasLayoutKeepsSubmissionVisible() {
        val config=compose.activity.resources.configuration
        assertTrue("Expected 320–480dp acceptance viewport, got ${config.screenWidthDp}",config.screenWidthDp in 320..480)
        assertTrue(config.fontScale>=1.29f)
        val suffix="${config.screenWidthDp}dp"
        click("الرئيسية")
        if(config.screenWidthDp>=400) {
            click("ملفي");click("الإعدادات")
            compose.onAllNodes(isToggleable()).onFirst().performClick()
            compose.waitForIdle();click("الرئيسية")
        }
        shot("atlas-home-$suffix")
        click("ابدأ رحلة الاكتشاف");shot("atlas-worlds-$suffix")
        click("مدينة اللغة");click("المفردات")
        val run=runBlocking {compose.activity.repository.atlasRuns.first().first {it.kind=="journey" && it.title=="المفردات" && !it.finished}}
        click("ابدأ المحطة ${run.step+1}")
        compose.onNodeWithText("تحقق من الإجابة").assertIsDisplayed()
        shot("atlas-playing-$suffix")
        solve()
        compose.onNodeWithText("إجابة صحيحة!",substring=true).performScrollTo().assertIsDisplayed()
        shot("atlas-result-$suffix")
        File(compose.activity.getExternalFilesDir(null),"atlas-layout-$suffix.txt").writeText("screenWidthDp=${config.screenWidthDp}\nfontScale=${config.fontScale}\n")
    }

    @Test fun duelRestoresOfflineWithoutChangingPlayerExperience() {
        click("الرئيسية")
        click("ابدأ رحلة الاكتشاف")
        val saved = File(compose.activity.filesDir, "atlas-duel.txt").readLines()
        val run = currentRun()
        assertEquals(saved[0], run.id)
        assertEquals(2, run.step)
        val before=SessionCodec.decode(saved[2])
        val restored=runBlocking {compose.activity.repository.observeSession(run.activeSessionId!!).first()!!.session}
        assertEquals(before.copy(status=SessionStatus.PAUSED),restored)
        click("متابعة ${run.title} · ${run.step}/${run.puzzleIds.size}")
        for (stage in run.step until run.puzzleIds.size) {
            if(stage==run.step) {click("استأنف المحطة");click("استئناف الجولة")} else click("ابدأ المحطة ${stage+1}")
            solve();click("متابعة الرحلة أو المواجهة")
        }
        val result = runBlocking { compose.activity.repository.observeRun(run.id).first()!! }
        assertTrue(result.finished)
        assertEquals(saved[1].toInt(), runBlocking { compose.activity.repository.progress.first().totalXp })
        shot("atlas-duel-result")
    }
}
