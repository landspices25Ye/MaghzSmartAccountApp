package com.example.data.backup

import com.example.data.model.AppCurrency
import com.example.data.model.CashBox
import com.example.data.model.ExpenseCategory
import com.example.data.model.Party
import com.example.data.model.ReportTemplate
import com.example.data.model.TransactionRecord
import java.io.File

data class BackupPayload(
    val version: Int = 3,
    val appName: String = "SmartAccountant",
    val timestamp: Long = System.currentTimeMillis(),
    val backupType: String = "AUTO", // "AUTO" or "MANUAL"
    val cashBoxes: List<CashBox> = emptyList(),
    val parties: List<Party> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val reportTemplates: List<ReportTemplate> = emptyList(),
    val currencies: List<AppCurrency> = emptyList(),
    val expenseCategories: List<ExpenseCategory> = emptyList()
)

data class BackupFileInfo(
    val file: File,
    val filename: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val isAuto: Boolean,
    val transactionCount: Int,
    val cashBoxCount: Int,
    val partyCount: Int
)
