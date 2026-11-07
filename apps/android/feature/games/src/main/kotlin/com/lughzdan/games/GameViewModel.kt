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
class GameViewModel(val repository:ProgressRepository,val settingsStore:SettingsStore,val contentRepository:ContentRepository?=null,val vocabularyRepository:VocabularyRepository?=null):ViewModel() {
 val localGuestId=settingsStore.localGuestId.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),"")
 val atlasPuzzles=repository.atlasCatalog.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val atlasRuns=repository.atlasRuns.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val discoveries=(vocabularyRepository?.entries ?: flowOf(emptyList())).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
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
 private val _submitting=MutableStateFlow(false);val submitting=_submitting.asStateFlow()
 init { viewModelScope.launch {settingsStore.ensureGuestId()}; initialize() }
 fun initialize() { viewModelScope.launch { try { repository.initialize();_ready.value=true;_error.value=null } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { _error.value="تعذّر فتح المحتوى المحلي. أعد المحاولة." } } }
 suspend fun readImage(sessionId:String,imageId:String)=repository.readImage(sessionId,imageId)
 fun observeRun(id:String)=repository.observeRun(id)
 fun observeSession(id:String)=repository.observeSession(id)
 fun moreSessions() {viewModelScope.launch {repository.loadMoreSessions()}}
 fun startDaily(onStarted:(String)->Unit) { viewModelScope.launch {
  if(!mutex.tryLock())return@launch
  _busy.value=true
  try {val day=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Aden")).toString();onStarted(repository.startDaily(day))}
  catch(cancelled:CancellationException){throw cancelled}
  catch(_:Exception){_error.value="تعذّر فتح تحدي اليوم. تحقق من وجود حزمة مثبتة."}
  finally {_busy.value=false;mutex.unlock()}
 } }
 fun morePuzzles() {viewModelScope.launch {repository.loadMore()}}
 fun start(id:String,onStarted:(String)->Unit) { viewModelScope.launch { if(!mutex.tryLock())return@launch;_busy.value=true;try { val started=repository.start(id);onStarted(started) } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { _error.value="تعذّر بدء الجولة." } finally { _busy.value=false;mutex.unlock() } } }

 fun createAtlasRun(run:AtlasRun,onCreated:(String)->Unit) {
  viewModelScope.launch { mutex.withLock {
   _busy.value=true
   try {onCreated(repository.createRun(run))}
   catch(cancelled:CancellationException){throw cancelled}
   catch(_:Exception){_error.value="تعذّر بدء الرحلة. تحقق من تثبيت الحزمة ومستوى الخبرة."}
   finally {_busy.value=false}
  } }
 }
 fun continueAtlasRun(id:String,onSession:(String)->Unit) {
  viewModelScope.launch { mutex.withLock {
   _busy.value=true
   try {repository.nextInRun(id)?.let(onSession)}
   catch(cancelled:CancellationException){throw cancelled}
   catch(_:Exception){_error.value="تعذّر فتح المحطة. تقدمك محفوظ ويمكنك المحاولة مجددًا."}
   finally {_busy.value=false}
  } }
 }
 fun saveDiscovery(p:PuzzleDefinition) {
  val vocabulary=vocabularyRepository ?: return
  viewModelScope.launch {
   try {
    val answer=p.payload.options.firstOrNull {it.id==p.payload.correctId}?.text
      ?: p.payload.acceptedAnswers.firstOrNull() ?: p.payload.rule.ifBlank {p.payload.prompt}
    vocabulary.save(VocabularySnapshot("${p.puzzleId}:${p.revision}",answer,p.explanation,
      p.payload.sourceLabel.ifBlank {"حزمة لُغْزَدان"},p.payload.sourceUrl.ifBlank {p.puzzleId},
      category=AtlasCatalog.world(p),linkedGameIds=listOf(p.puzzleId),contentStatus=p.status,fixtureOnly=p.fixtureOnly))
   } catch(cancelled:CancellationException){throw cancelled}
   catch(_:Exception){_error.value="تعذّر حفظ الاكتشاف. أعد المحاولة."}
  }
 }
 fun action(id:String,action:PlayerAction) { dispatch(id,action) }
 private fun dispatch(id:String,action:PlayerAction)=viewModelScope.launch { mutex.withLock { try { repository.act(id,action);_error.value=null } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { _error.value="لم تُحفظ الحركة. حاول مجددًا." } finally { } } }
 fun resume(id:String)=action(id,PlayerAction.Resume(UUID.randomUUID().toString()))
 fun select(id:String,option:String)=action(id,PlayerAction.Select(UUID.randomUUID().toString(),option))
 fun submit(id:String) {
  if(!submissionPending.compareAndSet(false,true)) return
  _submitting.value=true
  dispatch(id,PlayerAction.Submit(UUID.randomUUID().toString())).invokeOnCompletion { submissionPending.set(false);_submitting.value=false }
 }
 fun hint(id:String,hint:String)=action(id,PlayerAction.UseHint(UUID.randomUUID().toString(),hint))
 fun dark(value:Boolean) { viewModelScope.launch { settingsStore.dark(value) } }
 fun haptic(value:Boolean) {viewModelScope.launch {settingsStore.haptic(value)}}
 fun sound(value:Boolean) { viewModelScope.launch { settingsStore.sound(value) } }
}
