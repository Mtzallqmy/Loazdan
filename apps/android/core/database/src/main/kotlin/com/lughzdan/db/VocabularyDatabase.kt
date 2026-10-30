package com.lughzdan.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// No content foreign key: a personal snapshot must outlive pack deletion.
@Entity(tableName="personal_vocabulary")
data class VocabularyEntity(
 @PrimaryKey val id:String,val word:String,val meaning:String,val source:String,
 val sourceLocator:String,val root:String,val category:String,val linkedGameIds:String,
 val contentStatus:String,val fixtureOnly:Boolean,val favorite:Boolean=false,
 val reviewCount:Int=0,val lastReviewedAt:Long?=null
)
@Dao interface VocabularyDao {
 @Query("SELECT * FROM personal_vocabulary ORDER BY word,id") fun observe():Flow<List<VocabularyEntity>>
 @Query("SELECT * FROM personal_vocabulary WHERE id=:id") suspend fun entry(id:String):VocabularyEntity?
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun save(entry:VocabularyEntity):Long
 @Query("UPDATE personal_vocabulary SET favorite=:value WHERE id=:id") suspend fun favorite(id:String,value:Boolean)
 @Query("UPDATE personal_vocabulary SET reviewCount=reviewCount+1,lastReviewedAt=:at WHERE id=:id") suspend fun review(id:String,at:Long)
 @Query("DELETE FROM personal_vocabulary WHERE id=:id") suspend fun remove(id:String)
}
object VocabularySchema {
 const val CREATE="CREATE TABLE IF NOT EXISTS personal_vocabulary (id TEXT NOT NULL, word TEXT NOT NULL, meaning TEXT NOT NULL, source TEXT NOT NULL, sourceLocator TEXT NOT NULL, root TEXT NOT NULL, category TEXT NOT NULL, linkedGameIds TEXT NOT NULL, contentStatus TEXT NOT NULL, fixtureOnly INTEGER NOT NULL, favorite INTEGER NOT NULL, reviewCount INTEGER NOT NULL, lastReviewedAt INTEGER, PRIMARY KEY(id))"
}
