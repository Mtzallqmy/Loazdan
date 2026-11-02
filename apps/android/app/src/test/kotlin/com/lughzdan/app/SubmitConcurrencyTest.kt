package com.lughzdan.app

import androidx.test.core.app.ApplicationProvider
import com.lughzdan.domain.*
import com.lughzdan.engine.*
import com.lughzdan.games.GameViewModel
import com.lughzdan.preferences.SettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class SubmitConcurrencyTest {
 @Test fun repeatedSubmitWhileCommitIsPendingDoesNotCountTwoAttempts()=runTest {
  Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  val repository=GatedRepository()
  val vm=GameViewModel(repository,SettingsStore(ApplicationProvider.getApplicationContext()))
  val subscriber=backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.sessions.collect() }
  try {
   vm.submit("s")
   runCurrent()
   Assert.assertEquals(1,repository.calls)
   Assert.assertTrue(vm.busy.value)
   vm.submit("s")
   runCurrent()
   repository.release.complete(Unit)
   runCurrent()
   Assert.assertEquals(1,repository.calls)
   Assert.assertEquals(1,repository.session.wrongSubmissions)
   Assert.assertFalse(vm.busy.value)
   // A later deliberate retry remains possible after the first commit finishes.
   vm.submit("s")
   runCurrent()
   Assert.assertEquals(2,repository.calls)
  } finally {subscriber.cancel();Dispatchers.resetMain()}
 }
 private class GatedRepository:ProgressRepository {
  val release=CompletableDeferred<Unit>()
  var calls=0
  var session=GameSession("s","p",1)
  private val puzzle=PuzzleDefinition(1,"p",1,"order","order","test",1,1,"exact_v1","test","published",true,emptyList(),"",emptyList(),ChoicePayload())
  override val puzzles=flowOf(listOf(puzzle))
  override val sessions=MutableStateFlow(listOf(PlayRecord(session,puzzle)))
  override val unavailableSessions=flowOf(emptyList<UnavailableSession>())
  override val progress=flowOf(Progress(0,0))
  override val packs=flowOf(emptyList<InstalledPack>())
  override suspend fun initialize()=Unit
  override suspend fun start(puzzleId:String)="s"
  override suspend fun act(sessionId:String,action:PlayerAction):GameSession {
   calls++
   release.await()
   session=session.copy(wrongSubmissions=session.wrongSubmissions+1)
   sessions.value=listOf(PlayRecord(session,puzzle))
   return session
  }
 }
}
