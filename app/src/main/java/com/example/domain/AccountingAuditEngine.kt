package com.example.domain

import com.example.data.model.CashBox
import com.example.data.model.JournalEntry
import com.example.data.model.JournalLineItem
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import kotlin.math.abs

object AccountingAuditEngine {

    fun runComprehensiveAudit(
        cashBoxes: List<CashBox>,
        parties: List<Party>,
        transactions: List<TransactionRecord>,
        journalEntries: List<JournalEntry>,
        allLineItems: List<JournalLineItem>
    ): AuditReportResult {
        val anomalies = mutableListOf<FinancialAnomaly>()

        // 1. Check Cash Boxes for Negative Balances (عجز نقدي)
        for (box in cashBoxes) {
            if (box.balance < -0.01) {
                anomalies.add(
                    FinancialAnomaly(
                        title = "رصيد مكشوف في ${box.name}",
                        description = "رصيد الصندوق سالب (${String.format("%.2f", box.balance)}). هذا يشير لوجود عمليات صرف دون تسجيل مقبوضات مسبقة.",
                        severity = AnomalySeverity.HIGH,
                        targetType = "CASH_BOX",
                        targetId = box.id,
                        suggestedFix = "قم بإيداع أو تسجيل تحويل وارد إلى هذا الصندوق، أو تسوية الرصيد الفعلي."
                    )
                )
            }
        }

        // 2. Check Journal Balance (توازن دفتر اليومية العام)
        val totalDebit = allLineItems.sumOf { it.debit }
        val totalCredit = allLineItems.sumOf { it.credit }
        val diff = abs(totalDebit - totalCredit)
        val isBalanced = diff < 0.01

        if (!isBalanced) {
            anomalies.add(
                FinancialAnomaly(
                    title = "عدم توازن القيود المحاسبية العامة",
                    description = "إجمالي المدين (${String.format("%.2f", totalDebit)}) لا يساوي إجمالي الدائن (${String.format("%.2f", totalCredit)}). الفارق: ${String.format("%.2f", diff)}.",
                    severity = AnomalySeverity.HIGH,
                    targetType = "SYSTEM",
                    suggestedFix = "إجراء إعادة ترحيل تلقائي أو مراجعة القيود اليدوية غير المتوازنة."
                )
            )
        }

        // 3. Check for Suspicious Duplicate Transactions (عمليات مكررة محتملة خلال 5 دقائق)
        val sortedTx = transactions.sortedBy { it.timestamp }
        for (i in 0 until sortedTx.size - 1) {
            val curr = sortedTx[i]
            val next = sortedTx[i + 1]
            val timeDiffMinutes = (next.timestamp - curr.timestamp) / (1000 * 60)
            if (timeDiffMinutes <= 5 &&
                curr.amount == next.amount &&
                curr.type == next.type &&
                curr.partyId == next.partyId &&
                curr.cashBoxId == next.cashBoxId &&
                curr.amount > 0
            ) {
                anomalies.add(
                    FinancialAnomaly(
                        title = "احتمال تكرار حركة بالخطأ (${curr.type.titleAr})",
                        description = "تم تسجيل عمليتين متطابقتين بقيمة ${curr.amount} بفارق ${timeDiffMinutes} دقيقة فقط.",
                        severity = AnomalySeverity.MEDIUM,
                        targetType = "TRANSACTION",
                        targetId = next.id,
                        suggestedFix = "تأكد من عدم تكرار تسجيل الفاتورة أو السند مرتين بالخطأ، وحذف العملية الزائدة إن لزم."
                    )
                )
            }
        }

        // 4. Check for Parties with Abnormal / Negative Balances
        for (party in parties) {
            if (party.type == PartyType.CUSTOMER && party.balance < -100.0) {
                anomalies.add(
                    FinancialAnomaly(
                        title = "رصيد دائن للعميل ${party.name}",
                        description = "العميل دفع أكثر من المستحق عليه برصيد دائن قدره ${String.format("%.2f", abs(party.balance))}.",
                        severity = AnomalySeverity.LOW,
                        targetType = "PARTY",
                        targetId = party.id,
                        suggestedFix = "تسجيل فاتورة مبيعات جديدة لخصم الدفعة المقدمة، أو إعادة المبلغ الزائد له."
                    )
                )
            } else if (party.type == PartyType.SUPPLIER && party.balance < -100.0) {
                anomalies.add(
                    FinancialAnomaly(
                        title = "دفعة مقدمة غير مسواة للمورد ${party.name}",
                        description = "تم سداد مبالغ للمورد تزيد عن الفواتير المسجلة بمقدار ${String.format("%.2f", abs(party.balance))}.",
                        severity = AnomalySeverity.LOW,
                        targetType = "PARTY",
                        targetId = party.id,
                        suggestedFix = "تسجيل فاتورة المشتريات المستلمة لتسوية الحساب."
                    )
                )
            }
        }

        // 5. Large Expenses without category
        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE && tx.category.isBlank() && tx.amount >= 500.0) {
                anomalies.add(
                    FinancialAnomaly(
                        title = "مصروف كبير غير مبوب (${tx.amount})",
                        description = "تم تسجيل مصروف بمبلغ كبير دون تحديد تصنيف المصروف بدقة.",
                        severity = AnomalySeverity.LOW,
                        targetType = "TRANSACTION",
                        targetId = tx.id,
                        suggestedFix = "تعديل العملية وتحديد بند المصروف (إيجار، وقود، صيانة...) لضمان دقة التقارير."
                    )
                )
            }
        }

        return AuditReportResult(
            totalCashBoxesChecked = cashBoxes.size,
            totalPartiesChecked = parties.size,
            totalTransactionsAudited = transactions.size,
            totalJournalEntriesAudited = journalEntries.size,
            isBalanced = isBalanced,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            debitCreditDifference = diff,
            anomalies = anomalies
        )
    }
}
