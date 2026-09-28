package com.example

import com.example.ai.LocalAccountingNLP
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.search.IntelligentSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCustomerReceiptParsing() {
        val intent = LocalAccountingNLP.parse("استلمت 500 من محمد")
        assertEquals("TRANSACTION", intent.action)
        assertEquals(TransactionType.CUSTOMER_RECEIPT, intent.transactionType)
        assertEquals(PartyType.CUSTOMER, intent.partyType)
        assertEquals(500.0, intent.amount, 0.01)
    }

    @Test
    fun testSupplierPaymentParsing() {
        val intent = LocalAccountingNLP.parse("دفعت للمورد أحمد 300 ريال")
        assertEquals("TRANSACTION", intent.action)
        assertEquals(TransactionType.SUPPLIER_PAYMENT, intent.transactionType)
        assertEquals(PartyType.SUPPLIER, intent.partyType)
        assertEquals(300.0, intent.amount, 0.01)
    }

    @Test
    fun testExpenseParsingAndCategoryPrediction() {
        val categories = listOf("إيجار", "فواتير ومرافق", "نقل ومواصلات وبنزين", "ضيافة وبوفيه ومأكولات")
        val intent = LocalAccountingNLP.parse("صرفت 60 بنزين", availableCategories = categories)
        assertEquals("TRANSACTION", intent.action)
        assertEquals(TransactionType.EXPENSE, intent.transactionType)
        assertEquals(60.0, intent.amount, 0.01)
        assertEquals("نقل ومواصلات وبنزين", intent.category)
    }

    @Test
    fun testExpenseCategoryPredictionKeywords() {
        val categories = listOf("إيجار", "فواتير ومرافق", "نقل ومواصلات وبنزين", "رواتب وأجور", "ضيافة وبوفيه ومأكولات")

        val cat1 = LocalAccountingNLP.predictCategory("فاتورة كهرباء 200", categories)
        assertEquals("فواتير ومرافق", cat1)

        val cat2 = LocalAccountingNLP.predictCategory("إيجار المحل 3000", categories)
        assertEquals("إيجار", cat2)

        val cat3 = LocalAccountingNLP.predictCategory("عشاء للعمال 80", categories)
        assertEquals("ضيافة وبوفيه ومأكولات", cat3)
    }

    @Test
    fun testArabicNumeralsNormalization() {
        val amount = LocalAccountingNLP.extractAmount("استلمت ٧٥٠.٥ ريال")
        assertEquals(750.5, amount, 0.01)
    }

    @Test
    fun testIntelligentSearchAmountParsing() {
        val parties = listOf(Party(id = 1, name = "محمد العلي", type = PartyType.CUSTOMER))
        val boxes = listOf(CashBox(id = 1, name = "الصندوق الرئيسي"))

        val criteria = IntelligentSearchEngine.parseSearchQuery("> 500", parties, boxes)
        assertEquals(500.0, criteria.minAmount ?: 0.0, 0.01)

        val betweenCriteria = IntelligentSearchEngine.parseSearchQuery("بين 100 و 400", parties, boxes)
        assertEquals(100.0, betweenCriteria.minAmount ?: 0.0, 0.01)
        assertEquals(400.0, betweenCriteria.maxAmount ?: 0.0, 0.01)
    }

    @Test
    fun testIntelligentSearchExecution() {
        val customer = Party(id = 1, name = "سالم الأحمد", type = PartyType.CUSTOMER)
        val parties = listOf(customer)
        val box = CashBox(id = 1, name = "الصندوق الرئيسي")
        val boxes = listOf(box)

        val tx1 = TransactionRecord(
            id = 1,
            type = TransactionType.CUSTOMER_RECEIPT,
            amount = 1200.0,
            partyId = 1,
            cashBoxId = 1,
            description = "دفعة حساب سالم"
        )
        val tx2 = TransactionRecord(
            id = 2,
            type = TransactionType.EXPENSE,
            amount = 80.0,
            cashBoxId = 1,
            description = "بنزين سيارة"
        )

        val list = listOf(tx1, tx2)

        // Search by keyword "بنزين"
        val fuelSearch = IntelligentSearchEngine.executeSearch("بنزين", list, parties, boxes)
        assertEquals(1, fuelSearch.count)
        assertEquals(80.0, fuelSearch.transactions[0].amount, 0.01)

        // Search by amount > 500
        val amountSearch = IntelligentSearchEngine.executeSearch("> 500", list, parties, boxes)
        assertEquals(1, amountSearch.count)
        assertEquals(1200.0, amountSearch.transactions[0].amount, 0.01)

        // Search by party name "سالم"
        val partySearch = IntelligentSearchEngine.executeSearch("سالم", list, parties, boxes)
        assertEquals(1, partySearch.count)
        assertEquals(1200.0, partySearch.transactions[0].amount, 0.01)
    }

    @Test
    fun testEnglishVoiceRentExpenseParsing() {
        val categories = listOf("Rent", "Gas", "Food")
        val intent = LocalAccountingNLP.parse("I paid 500 for rent", availableCategories = categories)
        assertEquals("TRANSACTION", intent.action)
        assertEquals(TransactionType.EXPENSE, intent.transactionType)
        assertEquals(500.0, intent.amount, 0.01)
        assertTrue(intent.description.lowercase().contains("rent"))
    }

    @Test
    fun testEnglishVoiceSupplierPaymentParsing() {
        val intent = LocalAccountingNLP.parse("Paid 300 to supplier Mike")
        assertEquals("TRANSACTION", intent.action)
        assertEquals(TransactionType.SUPPLIER_PAYMENT, intent.transactionType)
        assertEquals(PartyType.SUPPLIER, intent.partyType)
        assertEquals(300.0, intent.amount, 0.01)
        assertTrue(intent.partyName?.lowercase()?.contains("mike") == true)
    }

    @Test
    fun testEnglishVoiceCustomerReceiptParsing() {
        val intent = LocalAccountingNLP.parse("Received 400 from Alice")
        assertEquals("TRANSACTION", intent.action)
        assertEquals(TransactionType.CUSTOMER_RECEIPT, intent.transactionType)
        assertEquals(PartyType.CUSTOMER, intent.partyType)
        assertEquals(400.0, intent.amount, 0.01)
        assertTrue(intent.partyName?.lowercase()?.contains("alice") == true)
    }

    @Test
    fun testEnglishVoiceGasExpenseParsing() {
        val intent = LocalAccountingNLP.parse("Spent 60 on gas")
        assertEquals("TRANSACTION", intent.action)
        assertEquals(TransactionType.EXPENSE, intent.transactionType)
        assertEquals(60.0, intent.amount, 0.01)
        assertTrue(intent.description.lowercase().contains("gas"))
    }

    @Test
    fun testAiAvailableModelsConfiguration() {
        val models = com.example.ai.AiPreferencesManager.AVAILABLE_MODELS
        assertEquals(3, models.size)
        assertEquals("gemini-3.5-flash-lite", com.example.ai.AiPreferencesManager.DEFAULT_MODEL)
        assertTrue(models.any { it.id == "gemini-3.5-flash-lite" })
        assertTrue(models.any { it.id == "gemini-3.7-flash" })
        assertTrue(models.any { it.id == "gemini-3.8-flash" })
        assertFalse(models.any { it.id == "gemini-3.5-flash" })
        assertFalse(models.any { it.id == "gemini-3.1-pro-preview" })
    }

    @Test
    fun testBackupPayloadIntegrity() {
        val payload = com.example.data.backup.BackupPayload(
            version = 3,
            backupType = "AUTO",
            cashBoxes = listOf(CashBox(id = 1, name = "الصندوق الرئيسي", balance = 500.0, isDefault = true)),
            parties = listOf(Party(id = 1, name = "محمد", type = PartyType.CUSTOMER, balance = 200.0)),
            transactions = listOf(TransactionRecord(id = 1, type = TransactionType.CUSTOMER_RECEIPT, amount = 200.0, cashBoxId = 1, partyId = 1))
        )
        assertEquals(3, payload.version)
        assertEquals("AUTO", payload.backupType)
        assertEquals(1, payload.cashBoxes.size)
        assertEquals(1, payload.parties.size)
        assertEquals(1, payload.transactions.size)
        assertEquals(500.0, payload.cashBoxes[0].balance, 0.01)
    }
}
