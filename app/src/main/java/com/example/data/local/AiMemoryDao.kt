package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AiMemoryFact
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMemoryDao {
    @Query("SELECT * FROM ai_memory_facts ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<AiMemoryFact>>

    @Query("SELECT * FROM ai_memory_facts ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<AiMemoryFact>

    @Query("SELECT * FROM ai_memory_facts WHERE isActive = 1 ORDER BY timestamp DESC")
    suspend fun getActiveMemories(): List<AiMemoryFact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: AiMemoryFact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<AiMemoryFact>)

    @Update
    suspend fun update(memory: AiMemoryFact)

    @Delete
    suspend fun delete(memory: AiMemoryFact)

    @Query("DELETE FROM ai_memory_facts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE ai_memory_facts SET isActive = :isActive WHERE id = :id")
    suspend fun setActiveStatus(id: Long, isActive: Boolean)

    @Query("DELETE FROM ai_memory_facts")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM ai_memory_facts WHERE isActive = 1")
    fun getActiveCountFlow(): Flow<Int>
}
