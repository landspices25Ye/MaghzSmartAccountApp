package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val actionType: String? = null, // "TRANSACTION", "QUERY", "QUESTION", "CHAT", "REMEMBER"
    val relatedTransactionId: Long? = null,
    val learnedMemoryText: String? = null // إذا تضمنت الرسالة حفظ معلومة جديدة في الذاكرة
)
