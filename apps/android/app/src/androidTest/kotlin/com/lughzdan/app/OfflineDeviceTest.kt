package com.lughzdan.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
/** Run phases in separate instrumentation processes using scripts/device_acceptance.sh. */
@RunWith(AndroidJUnit4::class)
class OfflineDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun waitFor(text:String) { compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
 @Test fun phaseOneSolve() {
  waitFor("ابدأ تحدي المفردات")
  compose.onNodeWithText("ابدأ تحدي المفردات").performScrollTo().performClick()
  compose.onNodeWithText("لغز 1 · الصعوبة 1").performScrollTo().performClick()
  waitFor("بعيد");compose.onNodeWithText("بعيد").performScrollTo().performClick()
  compose.waitUntil(10_000) { compose.onAllNodesWithText("تحقق من الإجابة").filter(isEnabled()).fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("تحقق من الإجابة").performScrollTo().performClick()
  waitFor("إجابة صحيحة! +100 نقطة")
 }
 @Test fun phaseTwoRestoreOffline() {
  waitFor("متابعة آخر نشاط")
  compose.onNodeWithText("متابعة آخر نشاط").performScrollTo().performClick()
  waitFor("إجابة صحيحة! +100 نقطة")
  compose.onNodeWithText("اللغز التالي").performScrollTo().performClick()
  waitFor("مكتبة");compose.onNodeWithText("مكتبة").performScrollTo().performClick()
  compose.waitUntil(10_000) { compose.onAllNodesWithText("تحقق من الإجابة").filter(isEnabled()).fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("تحقق من الإجابة").performScrollTo().performClick()
  waitFor("إجابة صحيحة! +100 نقطة")
 }
}
