package com.lughzdan.db
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="installed_packs") data class InstalledContentPackEntity(@PrimaryKey val packId:String,val version:Int,val sha256:String,val bytes:Long,val fixtureOnly:Boolean)
@Entity(tableName="puzzles", primaryKeys=["puzzleId","revision"], indices=[Index("packId")]) data class LocalPuzzleEntity(val puzzleId:String,val revision:Int,val packId:String,val definition:String,val ordinal:Int)
@Entity(tableName="sessions",indices=[Index("updatedAt")]) data class GameSessionEntity(@PrimaryKey val id:String,val puzzleId:String,val revision:Int,val state:String,val updatedAt:Long)
@Entity(tableName="progress") data class PlayerProgressEntity(@PrimaryKey val puzzleId:String,val bestScore:Int,val completed:Boolean,val attempts:Int)
@Entity(tableName="achievements") data class AchievementEntity(@PrimaryKey val id:String,val awardedAt:Long)
@Entity(tableName="sync_operations",indices=[Index(value=["sessionId"],unique=true)]) data class SyncOperationEntity(@PrimaryKey val operationId:String,val sessionId:String,val kind:String,val payload:String)
@Dao interface GameDao {
 @Query("SELECT * FROM puzzles ORDER BY ordinal") fun observePuzzles():Flow<List<LocalPuzzleEntity>>
 @Query("SELECT * FROM puzzles ORDER BY ordinal") suspend fun puzzles():List<LocalPuzzleEntity>
 @Query("SELECT * FROM puzzles WHERE puzzleId=:id AND revision=:revision") suspend fun puzzle(id:String,revision:Int):LocalPuzzleEntity?
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertPuzzles(items:List<LocalPuzzleEntity>)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun install(pack:InstalledContentPackEntity)
 @Query("SELECT * FROM installed_packs") fun observePacks():Flow<List<InstalledContentPackEntity>>
 @Query("SELECT * FROM installed_packs WHERE packId=:id") suspend fun pack(id:String):InstalledContentPackEntity?
 @Query("SELECT * FROM sessions ORDER BY updatedAt DESC,id DESC") fun observeSessions():Flow<List<GameSessionEntity>>
 @Query("SELECT * FROM sessions WHERE id=:id") suspend fun session(id:String):GameSessionEntity?
 @Upsert suspend fun save(session:GameSessionEntity)
 @Query("SELECT * FROM progress WHERE puzzleId=:id") suspend fun progress(id:String):PlayerProgressEntity?
 @Query("SELECT * FROM progress") fun observeProgress():Flow<List<PlayerProgressEntity>>
 @Upsert suspend fun saveProgress(progress:PlayerProgressEntity)
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun award(achievement:AchievementEntity):Long
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun enqueue(operation:SyncOperationEntity):Long
 @Query("SELECT COUNT(*) FROM sync_operations") suspend fun operationCount():Int
 @Query("DELETE FROM puzzles WHERE packId=:packId") suspend fun removeContent(packId:String)
}
@Database(entities=[InstalledContentPackEntity::class,LocalPuzzleEntity::class,GameSessionEntity::class,PlayerProgressEntity::class,AchievementEntity::class,SyncOperationEntity::class],version=1,exportSchema=true)
abstract class LughzdanDatabase:RoomDatabase(){ abstract fun games():GameDao }
