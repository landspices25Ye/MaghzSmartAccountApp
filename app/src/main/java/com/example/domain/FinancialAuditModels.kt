package com.example.domain

enum class AnomalySeverity(val titleAr: String) {
    HIGH("حرج / عالي الخطورة"),
    MEDIUM("متوسط"),
    LOW("تنبيه خفيف")
}

data class FinancialAnomaly(
    val title: String,
    val description: String,
    val severity: AnomalySeverity,
    val targetType: String, // "CASH_BOX", "PARTY", "TRANSACTION", "SYSTEM"
    val targetId: Long? = null,
    val suggestedFix: String = ""
)

data class AuditReportResult(
    val timestamp: Long = System.currentTimeMillis(),
    val totalCashBoxesChecked: Int,
    val totalPartiesChecked: Int,
    val totalTransactionsAudited: Int,
    val totalJournalEntriesAudited: Int,
    val isBalanced: Boolean,
    val totalDebit: Double,
    val totalCredit: Double,
    val debitCreditDifference: Double,
    val anomalies: List<FinancialAnomaly>
)
