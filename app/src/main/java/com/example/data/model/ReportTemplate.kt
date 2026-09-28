package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_templates")
data class ReportTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val datePreset: String = "ALL", // "ALL", "TODAY", "YESTERDAY", "THIS_WEEK", "THIS_MONTH", "LAST_30_DAYS", "CUSTOM"
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val accountScope: String = "ALL", // "ALL", "CASH_ALL", "CASH_SPECIFIC", "CUSTOMERS_ALL", "CUSTOMER_SPECIFIC", "SUPPLIERS_ALL", "SUPPLIER_SPECIFIC"
    val targetCashBoxId: Long? = null,
    val targetPartyId: Long? = null,
    val transactionTypes: String = "ALL", // Comma-separated names or "ALL"
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
