package com.example.domain

import com.example.ai.ParsedAccountingIntent
import java.util.UUID

enum class BatchCommandStatus {
    PENDING,       // قيد الانتظار والمعاينة
    PROCESSING,    // جاري التنفيذ الآن
    SUCCESS,       // تم بنجاح
    FAILED,        // فشل التنفيذ
    SKIPPED        // تم استبعاده من قبل المستخدم
}

data class BatchCommandItem(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val parsedIntent: ParsedAccountingIntent? = null,
    val isSelected: Boolean = true,
    val status: BatchCommandStatus = BatchCommandStatus.PENDING,
    val executionMessage: String? = null,
    val createdTransactionId: Long? = null
)

data class BatchPresetTemplate(
    val id: String,
    val title: String,
    val description: String,
    val commands: List<String>
)

data class BatchExecutionSummary(
    val totalCommands: Int,
    val successfulCount: Int,
    val failedCount: Int,
    val totalInflow: Double,
    val totalOutflow: Double,
    val executionReport: String,
    val executedItemResults: List<BatchCommandItem> = emptyList()
)
