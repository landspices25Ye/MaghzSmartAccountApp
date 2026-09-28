package com.example

import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetUnitTest {

    @Test
    fun testExpenseCategoryBudgetCalculation() {
        val category = ExpenseCategory(
            name = "إيجار",
            monthlyBudget = 5000.0,
            alertThresholdPercent = 80
        )

        assertEquals("إيجار", category.name)
        assertEquals(5000.0, category.monthlyBudget, 0.001)
        assertEquals(80, category.alertThresholdPercent)

        val tx1 = TransactionRecord(type = TransactionType.EXPENSE, amount = 3000.0, category = "إيجار")
        val tx2 = TransactionRecord(type = TransactionType.EXPENSE, amount = 1500.0, category = "إيجار")

        val totalSpent = listOf(tx1, tx2).sumOf { it.amount }
        assertEquals(4500.0, totalSpent, 0.001)

        val percentUsed = (totalSpent / category.monthlyBudget * 100).toInt()
        assertEquals(90, percentUsed)

        assertTrue(percentUsed >= category.alertThresholdPercent)
    }
}
