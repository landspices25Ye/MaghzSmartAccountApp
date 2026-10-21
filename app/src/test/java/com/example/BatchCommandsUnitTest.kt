package com.example

import com.example.data.model.TransactionType
import com.example.domain.BatchCommandItem
import com.example.domain.BatchCommandStatus
import com.example.domain.BatchCommandsEngine
import com.example.domain.BatchExecutionSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchCommandsUnitTest {

    @Test
    fun testSplitBatchText_multilineWithBulletsAndNumbers() {
        val input = """
            1. قبضت 500 ريال من العميل محمد
            • صرفت 80 ريال بنزين من الصندوق الرئيسي
            - سددت للمورد خالد 1500 ريال
            # هذا تعليق يجب تجاهله
            4) حول 300 ريال من الصندوق الرئيسي إلى العهدة
        """.trimIndent()

        val commands = BatchCommandsEngine.splitBatchText(input)
        assertEquals(4, commands.size)
        assertTrue(commands[0].contains("قبضت 500 ريال من العميل محمد"))
        assertTrue(commands[1].contains("صرفت 80 ريال بنزين"))
        assertTrue(commands[2].contains("سددت للمورد خالد 1500 ريال"))
        assertTrue(commands[3].contains("حول 300 ريال"))
    }

    @Test
    fun testSplitBatchText_semicolonSeparated() {
        val input = "قبضت 400 من سالم; صرف 50 شاي وقهوة؛ قيد دين 200 على فهد"
        val commands = BatchCommandsEngine.splitBatchText(input)
        assertEquals(3, commands.size)
        assertTrue(commands[0].contains("400"))
        assertTrue(commands[1].contains("50"))
        assertTrue(commands[2].contains("200"))
    }

    @Test
    fun testParseBatchItems_recognizesIntents() = runBlocking {
        val commands = listOf(
            "قبضت 750 ريال من العميل سمير",
            "صرفت 120 ريال وقود بنزين",
            "سددت للمورد شركة النور 2000 ريال",
            "حول 500 ريال من الصندوق إلى العهدة"
        )

        val parsedItems = BatchCommandsEngine.parseBatchItems(
            commands = commands,
            defaultCurrencySymbol = "ر.س",
            defaultCashBoxName = "الصندوق الرئيسي",
            availableCategories = listOf("وقود", "إيجار", "صيانة")
        )

        assertEquals(4, parsedItems.size)

        // Command 1: Customer receipt
        assertEquals(TransactionType.CUSTOMER_RECEIPT, parsedItems[0].parsedIntent?.transactionType)
        assertEquals(750.0, parsedItems[0].parsedIntent?.amount ?: 0.0, 0.01)

        // Command 2: Expense
        assertEquals(TransactionType.EXPENSE, parsedItems[1].parsedIntent?.transactionType)
        assertEquals(120.0, parsedItems[1].parsedIntent?.amount ?: 0.0, 0.01)

        // Command 3: Supplier payment
        assertEquals(TransactionType.SUPPLIER_PAYMENT, parsedItems[2].parsedIntent?.transactionType)
        assertEquals(2000.0, parsedItems[2].parsedIntent?.amount ?: 0.0, 0.01)

        // Command 4: Transfer
        assertEquals(TransactionType.TRANSFER, parsedItems[3].parsedIntent?.transactionType)
        assertEquals(500.0, parsedItems[3].parsedIntent?.amount ?: 0.0, 0.01)
    }

    @Test
    fun testBatchExecutionSummaryCalculation() {
        val summary = BatchExecutionSummary(
            totalCommands = 4,
            successfulCount = 3,
            failedCount = 1,
            totalInflow = 1250.0,
            totalOutflow = 800.0,
            executionReport = "تقرير ناجح"
        )

        assertEquals(4, summary.totalCommands)
        assertEquals(3, summary.successfulCount)
        assertEquals(1, summary.failedCount)
        assertEquals(1250.0, summary.totalInflow, 0.001)
        assertEquals(800.0, summary.totalOutflow, 0.001)
    }
}
