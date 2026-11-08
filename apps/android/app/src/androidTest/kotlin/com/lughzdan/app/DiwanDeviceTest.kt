package com.lughzdan.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiwanDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun openLibrary() {
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("open-diwan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("open-diwan").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("reading-p24").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun click(label: String) = compose.onNodeWithText(label).performScrollTo().performClick()
    @Test fun readPlayAndBookmark() {
        openLibrary()
        compose.onNodeWithTag("reading-p24").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("احفظ في ديواني").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("احفظ في ديواني")[0].performScrollTo().performClick()
        click("اختبر نفسك")
        click("الحدود بين بلدين")
        compose.onNodeWithText("راجع النص وجرّب إجابة أخرى.").assertExists()
        click("حافة السيف ثم الفصل بين أمرين")
        compose.onNodeWithText("✓ حافة السيف ثم الفصل بين أمرين",substring=true).assertExists()
    }
    @Test fun recallSavedLineAfterRestart() {
        openLibrary()
        click("ديواني")
        compose.onNodeWithTag("reading-p24").performClick()
        click("تسميع")
        click("اعرض النص للتحقق")
        click("أتقنته")
        click("العودة إلى الديوان")
        click("موعد المراجعة")
        compose.onNodeWithText("لا توجد مختارات مستحقة الآن.").assertExists()
    }
}
