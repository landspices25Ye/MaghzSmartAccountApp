package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType(val titleAr: String) {
    CUSTOMER_RECEIPT("قبض من عميل"),
    SUPPLIER_PAYMENT("دفع لمورد"),
    CUSTOMER_NEW_DEBIT("دين جديد على عميل"),
    SUPPLIER_NEW_CREDIT("دين جديد من مورد"),
    EXPENSE("مصروف"),
    INCOME("إيراد نقدي"),
    TRANSFER("تحويل بين الصناديق"),
    SETTLEMENT("تسوية رصيد")
}

@Entity(tableName = "transactions")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val cashBoxId: Long? = null,
    val targetCashBoxId: Long? = null, // for TRANSFER
    val partyId: Long? = null,          // for Customer / Supplier
    val category: String = "",          // e.g. Expense category name (إيجار, وقود...)
    val currency: String = "",          // e.g. "ر.س", "SAR"
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = false
)
