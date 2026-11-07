package com.lughzdan.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "atlas_runs")
data class AtlasRunEntity(@PrimaryKey val id: String, val state: String, val updatedAt: Long)

@Dao
interface AtlasDao {
    @Query("SELECT * FROM atlas_runs ORDER BY updatedAt DESC LIMIT 50")
    fun observe(): Flow<List<AtlasRunEntity>>

    @Query("SELECT * FROM atlas_runs WHERE id=:id")
    suspend fun get(id: String): AtlasRunEntity?

    @Upsert
    suspend fun save(run: AtlasRunEntity)
}
