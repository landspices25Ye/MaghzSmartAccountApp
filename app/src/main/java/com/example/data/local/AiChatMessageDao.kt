package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AiChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatMessageDao {
    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<AiChatMessageEntity>

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<AiChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: AiChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<AiChatMessageEntity>)

    @Delete
    suspend fun delete(message: AiChatMessageEntity)

    @Query("DELETE FROM ai_chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM ai_chat_messages")
    suspend fun getMessageCount(): Int
}
