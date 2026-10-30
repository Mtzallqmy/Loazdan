package com.lughzdan.games
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lughzdan.domain.*
import com.lughzdan.engine.*
import com.lughzdan.preferences.SettingsStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
class GameViewModel(val repository:ProgressRepository,val settingsStore:SettingsStore,val contentRepository:ContentRepository?=null):ViewModel() {
 val puzzles=repository.puzzles.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val sessions=repository.sessions.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val unavailableSessions=repository.unavailableSessions.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val progress=repository.progress.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Progress(0,0))
 val packs=repository.packs.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val settings=settingsStore.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),com.lughzdan.preferences.Settings())
 val catalog=(contentRepository?.catalog ?: flowOf(emptyList())).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val downloads=(contentRepository?.downloads ?: flowOf(emptyList())).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val contentPreferences=(contentRepository?.preferences ?: flowOf(ContentPreferences())).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),ContentPreferences())
 val storage=(contentRepository?.storage ?: flowOf(ContentStorage())).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),ContentStorage())
 val cloudDisabledReason=(contentRepository?.disabledReason ?: flowOf("لم يُضبط خادم للمحتوى. يمكنك استخدام الحزم المثبتة دون اتصال.")).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 fun contentAction(action:suspend ContentRepository.()->Unit) { val content=contentRepository ?: return;viewModelScope.launch { try { content.action() } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { _error.value="تعذّر إتمام عملية المحتوى. أعد المحاولة." } } }
 private val _ready=MutableStateFlow(false);val ready=_ready.asStateFlow()
 private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
 private val _busy=MutableStateFlow(false);val busy=_busy.asStateFlow()
 private val mutex=Mutex()
 private val submissionPending=AtomicBoolean(false)
 init { initialize() }
 fun initialize() { viewModelScope.launch { try { repository.initialize();_ready.value=true;_error.value=null } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { _error.value="تعذّر فتح المحتوى المحلي. أعد المحاولة." } } }
 suspend fun readImage(sessionId:String,imageId:String)=repository.readImage(sessionId,imageId)
 fun morePuzzles() {viewModelScope.launch {repository.loadMore()}}
 fun start(id:String,onStarted:(String)->Unit) { viewModelScope.launch { if(!mutex.tryLock())return@launch;_busy.value=true;try { val started=repository.start(id);sessions.first { records->records.any { it.session.id==started } };onStarted(started) } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { _error.value="تعذّر بدء الجولة." } finally { _busy.value=false;mutex.unlock() } } }
 fun action(id:String,action:PlayerAction) { dispatch(id,action) }
 private fun dispatch(id:String,action:PlayerAction)=viewModelScope.launch { mutex.withLock { _busy.value=true;try { val committed=repository.act(id,action);sessions.first { records->records.any { it.session==committed } };_error.value=null } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { _error.value="لم تُحفظ الحركة. حاول مجددًا." } finally { _busy.value=false } } }
 fun resume(id:String)=action(id,PlayerAction.Resume(UUID.randomUUID().toString()))
 fun select(id:String,option:String)=action(id,PlayerAction.Select(UUID.randomUUID().toString(),option))
 fun submit(id:String) {
  if(!submissionPending.compareAndSet(false,true)) return
  dispatch(id,PlayerAction.Submit(UUID.randomUUID().toString())).invokeOnCompletion { submissionPending.set(false) }
 }
 fun hint(id:String,hint:String)=action(id,PlayerAction.UseHint(UUID.randomUUID().toString(),hint))
 fun dark(value:Boolean) { viewModelScope.launch { settingsStore.dark(value) } }
 fun sound(value:Boolean) { viewModelScope.launch { settingsStore.sound(value) } }
}
