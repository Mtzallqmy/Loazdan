package com.lughzdan.preferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class SettingsStoreTest {
 @Test fun preferencesSurviveStoreRecreation()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<android.content.Context>()
  val store=SettingsStore(context);store.dark(true);store.sound(true)
  val restored=SettingsStore(context).settings.first()
  assertTrue(restored.dark);assertTrue(restored.sound)
 }
}
