package com.lughzdan.domain
import com.lughzdan.engine.*
import kotlinx.coroutines.flow.Flow

data class PlayRecord(val session:GameSession,val puzzle:PuzzleDefinition)
data class UnavailableSession(val id:String,val reason:String)
data class Progress(val totalScore:Int,val completed:Int)
data class InstalledPack(val id:String,val version:Int,val bytes:Long,val fixtureOnly:Boolean)
interface ProgressRepository {
 val puzzles:Flow<List<PuzzleDefinition>>
 val sessions:Flow<List<PlayRecord>>
 val unavailableSessions:Flow<List<UnavailableSession>>
 val progress:Flow<Progress>
 val packs:Flow<List<InstalledPack>>
 suspend fun initialize()
 suspend fun start(puzzleId:String):String
 suspend fun act(sessionId:String,action:PlayerAction):GameSession
}
class GameSessionManager(private val repository:ProgressRepository) {
 suspend fun start(puzzleId:String)=repository.start(puzzleId)
 suspend fun dispatch(sessionId:String,action:PlayerAction)=repository.act(sessionId,action)
}
