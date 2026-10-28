package com.lughzdan.db
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName="installed_packs", primaryKeys=["packId","version"]) data class InstalledContentPackEntity(val packId:String,val version:Int,val sha256:String,val bytes:Long,val fixtureOnly:Boolean)
@Entity(tableName="puzzles", primaryKeys=["puzzleId","revision"], indices=[Index("packId")]) data class LocalPuzzleEntity(val puzzleId:String,val revision:Int,val packId:String,val definition:String,val ordinal:Int)
@Entity(tableName="sessions",indices=[Index("updatedAt")]) data class GameSessionEntity(@PrimaryKey val id:String,val puzzleId:String,val revision:Int,val state:String,val updatedAt:Long)
@Entity(tableName="progress") data class PlayerProgressEntity(@PrimaryKey val puzzleId:String,val bestScore:Int,val completed:Boolean,val attempts:Int,@ColumnInfo(defaultValue="'choice'") val gameType:String="choice",@ColumnInfo(defaultValue="0") val xp:Int=0)
@Entity(tableName="achievements") data class AchievementEntity(@PrimaryKey val id:String,val awardedAt:Long)
@Entity(tableName="sync_operations",indices=[Index(value=["sessionId"],unique=true)]) data class SyncOperationEntity(@PrimaryKey val operationId:String,val sessionId:String,val kind:String,val payload:String)
@Dao interface GameDao {
 @Query("SELECT puzzles.* FROM puzzles INNER JOIN (SELECT puzzleId, MAX(revision) AS revision FROM puzzles GROUP BY puzzleId) latest USING(puzzleId,revision) ORDER BY ordinal") fun observePuzzles():Flow<List<LocalPuzzleEntity>>
 @Query("SELECT puzzles.* FROM puzzles INNER JOIN (SELECT puzzleId, MAX(revision) AS revision FROM puzzles GROUP BY puzzleId) latest USING(puzzleId,revision) ORDER BY ordinal") suspend fun puzzles():List<LocalPuzzleEntity>
 @Query("SELECT * FROM puzzles WHERE puzzleId=:id AND revision=:revision") suspend fun puzzle(id:String,revision:Int):LocalPuzzleEntity?
 @Query("SELECT COUNT(*) FROM puzzles") fun observeContentCount():Flow<Long>
 @Query("SELECT * FROM puzzles WHERE puzzleId=:id ORDER BY revision DESC LIMIT 1") suspend fun latestPuzzle(id:String):LocalPuzzleEntity?
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertPuzzles(items:List<LocalPuzzleEntity>)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun install(pack:InstalledContentPackEntity)
 @Query("SELECT * FROM installed_packs") fun observePacks():Flow<List<InstalledContentPackEntity>>
 @Query("SELECT * FROM installed_packs WHERE packId=:id ORDER BY version DESC LIMIT 1") suspend fun pack(id:String):InstalledContentPackEntity?
 @Query("SELECT * FROM installed_packs WHERE packId=:id AND version=:version") suspend fun pinnedPack(id:String,version:Int):InstalledContentPackEntity?
 @Query("SELECT * FROM achievements ORDER BY awardedAt,id") fun observeAchievements():Flow<List<AchievementEntity>>
 @Query("SELECT * FROM sessions ORDER BY updatedAt DESC,id DESC") fun observeSessions():Flow<List<GameSessionEntity>>
 @Query("SELECT * FROM sessions") suspend fun storedSessions():List<GameSessionEntity>
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
@Database(entities=[InstalledContentPackEntity::class,LocalPuzzleEntity::class,GameSessionEntity::class,PlayerProgressEntity::class,AchievementEntity::class,SyncOperationEntity::class],version=2,exportSchema=true)
abstract class LughzdanDatabase:RoomDatabase(){
 abstract fun games():GameDao
 companion object {
  val MIGRATION_1_2:Migration=object:Migration(1,2) {
   override fun migrate(db:SupportSQLiteDatabase) {
    db.execSQL("CREATE TABLE installed_packs_v2 (packId TEXT NOT NULL, version INTEGER NOT NULL, sha256 TEXT NOT NULL, bytes INTEGER NOT NULL, fixtureOnly INTEGER NOT NULL, PRIMARY KEY(packId, version))")
    db.execSQL("INSERT INTO installed_packs_v2 SELECT packId,version,sha256,bytes,fixtureOnly FROM installed_packs")
    db.execSQL("DROP TABLE installed_packs")
    db.execSQL("ALTER TABLE installed_packs_v2 RENAME TO installed_packs")
    db.execSQL("ALTER TABLE progress ADD COLUMN gameType TEXT NOT NULL DEFAULT 'choice'")
    db.execSQL("ALTER TABLE progress ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
    db.execSQL("UPDATE progress SET xp=bestScore")
   }
  }
 }
}
