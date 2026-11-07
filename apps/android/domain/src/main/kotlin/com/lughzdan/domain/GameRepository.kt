package com.lughzdan.domain
import com.lughzdan.engine.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PlayRecord(val session:GameSession,val puzzle:PuzzleDefinition)
data class UnavailableSession(val id:String,val reason:String)
data class GameProgress(val gameType:String,val totalScore:Int,val completed:Int,val attempts:Int,val xp:Int)
data class Progress(val totalScore:Int,val completed:Int,val totalXp:Int=totalScore,val perGame:List<GameProgress> = emptyList(),val achievements:List<String> = emptyList())
data class InstalledPack(val id:String,val version:Int,val bytes:Long,val fixtureOnly:Boolean)
interface ProgressRepository {
 val atlasCatalog:Flow<List<PuzzleDefinition>> get()=puzzles
 val atlasRuns:Flow<List<AtlasRun>> get()=kotlinx.coroutines.flow.flowOf(emptyList())
 fun observeRun(id:String):Flow<AtlasRun?> = atlasRuns.map {runs->runs.firstOrNull {it.id==id}}
 suspend fun createRun(run:AtlasRun):String=error("Atlas unavailable")
 suspend fun nextInRun(id:String):String?=error("Atlas unavailable")
 val puzzles:Flow<List<PuzzleDefinition>>
 val sessions:Flow<List<PlayRecord>>
 val unavailableSessions:Flow<List<UnavailableSession>>
 val progress:Flow<Progress>
 val packs:Flow<List<InstalledPack>>
 fun observeSession(id:String):Flow<PlayRecord?> = sessions.map { records->records.find {it.session.id==id} }
 suspend fun loadMoreSessions() {}
 suspend fun startDaily(day:String):String = error("Daily challenge unavailable")
 suspend fun loadMore() {}
 suspend fun initialize()
 suspend fun start(puzzleId:String):String
 suspend fun act(sessionId:String,action:PlayerAction):GameSession
 suspend fun readImage(sessionId:String,imageId:String):ByteArray? = null
}
class GameSessionManager(private val repository:ProgressRepository) {
 suspend fun start(puzzleId:String)=repository.start(puzzleId)
 suspend fun dispatch(sessionId:String,action:PlayerAction)=repository.act(sessionId,action)
}
