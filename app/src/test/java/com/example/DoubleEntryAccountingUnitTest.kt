package com.example

import com.example.data.model.AccountCategory
import com.example.data.model.CashBox
import com.example.data.model.JournalEntry
import com.example.data.model.JournalLineItem
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.domain.AccountingAuditEngine
import com.example.domain.DoubleEntryEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleEntryAccountingUnitTest {

    @Test
    fun testCustomerReceiptGeneratesBalancedEntry() {
        val tx = TransactionRecord(
            id = 1,
            type = TransactionType.CUSTOMER_RECEIPT,
            amount = 500.0,
            cashBoxId = 1,
            partyId = 2,
            currency = "ر.س",
            description = "سداد دفعة من محمد"
        )

        val (entry, lines) = DoubleEntryEngine.createJournalEntryForTransaction(
            transaction = tx,
            cashBoxName = "الصندوق الرئيسي",
            partyName = "محمد",
            currencySymbol = "ر.س"
        )

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        assertEquals(500.0, totalDebit, 0.001)
        assertEquals(500.0, totalCredit, 0.001)
        assertEquals(2, lines.size)
        // Dr: Cash, Cr: Accounts Receivable
        assertEquals(AccountCategory.ASSET, lines[0].accountCategory)
        assertEquals(500.0, lines[0].debit, 0.001)
        assertEquals(AccountCategory.ASSET, lines[1].accountCategory)
        assertEquals(500.0, lines[1].credit, 0.001)
    }

    @Test
    fun testSupplierPaymentGeneratesBalancedEntry() {
        val tx = TransactionRecord(
            id = 2,
            type = TransactionType.SUPPLIER_PAYMENT,
            amount = 1200.0,
            cashBoxId = 1,
            partyId = 3,
            currency = "ر.س",
            description = "دفعة لمؤسسة الأمل"
        )

        val (entry, lines) = DoubleEntryEngine.createJournalEntryForTransaction(
            transaction = tx,
            cashBoxName = "الصندوق الرئيسي",
            partyName = "مؤسسة الأمل",
            currencySymbol = "ر.س"
        )

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        assertEquals(1200.0, totalDebit, 0.001)
        assertEquals(1200.0, totalCredit, 0.001)
        // Dr: Liability (Supplier), Cr: Cash
        assertEquals(AccountCategory.LIABILITY, lines[0].accountCategory)
        assertEquals(1200.0, lines[0].debit, 0.001)
        assertEquals(AccountCategory.ASSET, lines[1].accountCategory)
        assertEquals(1200.0, lines[1].credit, 0.001)
    }

    @Test
    fun testExpenseGeneratesBalancedEntry() {
        val tx = TransactionRecord(
            id = 3,
            type = TransactionType.EXPENSE,
            amount = 75.0,
            cashBoxId = 1,
            category = "وقود وبنزين",
            currency = "ر.س"
        )

        val (entry, lines) = DoubleEntryEngine.createJournalEntryForTransaction(
            transaction = tx,
            cashBoxName = "الصندوق الرئيسي",
            currencySymbol = "ر.س"
        )

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        assertEquals(75.0, totalDebit, 0.001)
        assertEquals(75.0, totalCredit, 0.001)
        assertEquals(AccountCategory.EXPENSE, lines[0].accountCategory)
        assertEquals(AccountCategory.ASSET, lines[1].accountCategory)
    }

    @Test
    fun testTransferBetweenBoxesGeneratesBalancedEntry() {
        val tx = TransactionRecord(
            id = 4,
            type = TransactionType.TRANSFER,
            amount = 2000.0,
            cashBoxId = 1,
            targetCashBoxId = 2,
            currency = "ر.س"
        )

        val (entry, lines) = DoubleEntryEngine.createJournalEntryForTransaction(
            transaction = tx,
            cashBoxName = "الصندوق الرئيسي",
            targetCashBoxName = "بنك الراجحي",
            currencySymbol = "ر.س"
        )

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        assertEquals(2000.0, totalDebit, 0.001)
        assertEquals(2000.0, totalCredit, 0.001)
        assertEquals("صندوق: بنك الراجحي", lines[0].accountName)
        assertEquals("صندوق: الصندوق الرئيسي", lines[1].accountName)
    }

    @Test
    fun testTrialBalanceAndFinancialSummary() {
        val allLines = listOf(
            JournalLineItem(journalEntryId = 1, accountCategory = AccountCategory.ASSET, accountName = "صندوق: رئيسي", debit = 1000.0, credit = 0.0),
            JournalLineItem(journalEntryId = 1, accountCategory = AccountCategory.REVENUE, accountName = "إيرادات نقدية", debit = 0.0, credit = 1000.0),
            JournalLineItem(journalEntryId = 2, accountCategory = AccountCategory.EXPENSE, accountName = "مصروف: كهرباء", debit = 200.0, credit = 0.0),
            JournalLineItem(journalEntryId = 2, accountCategory = AccountCategory.ASSET, accountName = "صندوق: رئيسي", debit = 0.0, credit = 200.0)
        )

        val trialBalance = DoubleEntryEngine.computeTrialBalance(allLines)
        val sumDr = trialBalance.sumOf { it.totalDebit }
        val sumCr = trialBalance.sumOf { it.totalCredit }

        assertEquals(1200.0, sumDr, 0.001)
        assertEquals(1200.0, sumCr, 0.001)

        val summary = DoubleEntryEngine.computeFinancialSummary(allLines)
        assertEquals(800.0, summary.totalAssets, 0.001)
        assertEquals(1000.0, summary.totalRevenue, 0.001)
        assertEquals(200.0, summary.totalExpenses, 0.001)
        assertEquals(800.0, summary.netProfit, 0.001)
        assertEquals(800.0, summary.workingCapital, 0.001)
    }

    @Test
    fun testAuditEngineDetectsNegativeCashBoxBalance() {
        val cashBoxes = listOf(
            CashBox(id = 1, name = "الصندوق الرئيسي", balance = -250.0)
        )
        val parties = emptyList<Party>()
        val transactions = emptyList<TransactionRecord>()
        val entries = emptyList<JournalEntry>()
        val lines = emptyList<JournalLineItem>()

        val auditResult = AccountingAuditEngine.runComprehensiveAudit(
            cashBoxes = cashBoxes,
            parties = parties,
            transactions = transactions,
            journalEntries = entries,
            allLineItems = lines
        )

        assertTrue(auditResult.anomalies.isNotEmpty())
        assertEquals("CASH_BOX", auditResult.anomalies[0].targetType)
        assertTrue(auditResult.anomalies[0].title.contains("رصيد مكشوف"))
    }
}
