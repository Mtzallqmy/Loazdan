package com.lughzdan.games
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lughzdan.domain.*
import com.lughzdan.engine.*
import com.lughzdan.preferences.SettingsStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
class GameViewModel(val repository:ProgressRepository,val settingsStore:SettingsStore):ViewModel() {
 val puzzles=repository.puzzles.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val sessions=repository.sessions.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val progress=repository.progress.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Progress(0,0))
 val packs=repository.packs.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val settings=settingsStore.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),com.lughzdan.preferences.Settings())
 private val _ready=MutableStateFlow(false);val ready=_ready.asStateFlow()
 private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
 private val _busy=MutableStateFlow(false);val busy=_busy.asStateFlow()
 private val mutex=Mutex()
 init { initialize() }
 fun initialize() { viewModelScope.launch { try { repository.initialize();_ready.value=true;_error.value=null } catch(_:Exception) { _error.value="تعذّر فتح المحتوى المحلي. أعد المحاولة." } } }
 fun start(id:String,onStarted:(String)->Unit) { viewModelScope.launch { if(!mutex.tryLock())return@launch;_busy.value=true;try { onStarted(repository.start(id)) } catch(_:Exception) { _error.value="تعذّر بدء الجولة." } finally { _busy.value=false;mutex.unlock() } } }
 fun action(id:String,action:PlayerAction) { viewModelScope.launch { mutex.withLock { _busy.value=true;try { repository.act(id,action);_error.value=null } catch(_:Exception) { _error.value="لم تُحفظ الحركة. حاول مجددًا." } finally { _busy.value=false } } } }
 fun select(id:String,option:String)=action(id,PlayerAction.Select(UUID.randomUUID().toString(),option))
 fun submit(id:String)=action(id,PlayerAction.Submit(UUID.randomUUID().toString()))
 fun hint(id:String,hint:String)=action(id,PlayerAction.UseHint(UUID.randomUUID().toString(),hint))
 fun dark(value:Boolean) { viewModelScope.launch { settingsStore.dark(value) } }
 fun sound(value:Boolean) { viewModelScope.launch { settingsStore.sound(value) } }
}
