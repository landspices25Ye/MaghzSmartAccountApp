package com.example.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.MoneyIncomeGreen
import java.text.DecimalFormat

@Composable
fun BudgetOverviewSection(
    expenseCategories: List<ExpenseCategory>,
    allTransactions: List<TransactionRecord>,
    currencySymbol: String,
    onOpenBudgetsDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgetedCategories = remember(expenseCategories) {
        expenseCategories.filter { it.monthlyBudget > 0 }
    }

    if (budgetedCategories.isEmpty()) return

    val formatter = remember { DecimalFormat("#,##0.##") }

    // Calculate current month's expenses per category
    val currentMonthStart = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.timeInMillis
    }

    val monthExpensesByCategory = remember(allTransactions, currentMonthStart) {
        allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= currentMonthStart }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الميزانيات التقديرية للشهر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = onOpenBudgetsDialog,
                modifier = Modifier.testTag("dashboard_manage_budgets_button")
            ) {
                Text("إدارة الميزانيات", fontSize = 12.sp)
            }
        }

        budgetedCategories.take(3).forEach { category ->
            val spent = monthExpensesByCategory[category.name] ?: 0.0
            val budget = category.monthlyBudget
            val ratio = (spent / budget).toFloat().coerceIn(0f, 1f)
            val percent = (spent / budget * 100).toInt()
            val isExceeded = spent >= budget
            val isNearLimit = percent >= category.alertThresholdPercent

            val progressColor = when {
                isExceeded -> MoneyExpenseRed
                isNearLimit -> MoneyGold
                else -> MoneyIncomeGreen
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenBudgetsDialog() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isNearLimit || isExceeded) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = progressColor,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "${formatter.format(spent)} / ${formatter.format(budget)} $currencySymbol",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = progressColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isExceeded) "تم تجاوز الميزانية بـ ${percent - 100}%" else "المتبقي: ${formatter.format((budget - spent).coerceAtLeast(0.0))} $currencySymbol",
                            fontSize = 11.sp,
                            color = if (isExceeded) MoneyExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$percent%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }
                }
            }
        }
    }
}
