package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.DecimalFormat
import java.util.Calendar

@Composable
fun BudgetsDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()

    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"
    val numberFormat = remember { DecimalFormat("#,##0.##") }

    // Start of current calendar month timestamp
    val startOfCurrentMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Calculate current month's expenses per category name
    val monthlyExpensesByCategory = remember(transactions, startOfCurrentMonth) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= startOfCurrentMonth }
            .groupBy { it.category.trim().lowercase() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    // Total budgeted vs spent
    val categoriesWithBudget = categories.filter { it.monthlyBudget > 0 }
    val totalBudget = categoriesWithBudget.sumOf { it.monthlyBudget }
    val totalSpentOnBudgeted = categoriesWithBudget.sumOf { cat ->
        monthlyExpensesByCategory[cat.name.trim().lowercase()] ?: 0.0
    }

    val totalAllSpentThisMonth = categories.sumOf { cat ->
        monthlyExpensesByCategory[cat.name.trim().lowercase()] ?: 0.0
    }

    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "الميزانيات والحدود الشهرية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "متابعة الإنفاق والسيطرة على المصروفات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // 1. Overview Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ملخص الميزانية الشهرية العامة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PrimaryGreen
                            )

                            Surface(
                                color = PrimaryGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${categoriesWithBudget.size} بند محدد",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("إجمالي الميزانية المحددة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = if (totalBudget > 0) "${numberFormat.format(totalBudget)} $currencySymbol" else "غير محددة",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("المصروف هذا الشهر", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = "${numberFormat.format(totalAllSpentThisMonth)} $currencySymbol",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyExpenseRed
                                )
                            }
                        }

                        if (totalBudget > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val overallRatio = (totalSpentOnBudgeted / totalBudget).toFloat().coerceIn(0f, 1f)
                            val overallPercent = (totalSpentOnBudgeted / totalBudget * 100).toInt()

                            val progressColor = when {
                                overallPercent > 100 -> MoneyExpenseRed
                                overallPercent >= 80 -> Color(0xFFE65100) // Orange
                                else -> PrimaryGreen
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نسبة الاستهلاك: $overallPercent%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = progressColor
                                )

                                val remaining = totalBudget - totalSpentOnBudgeted
                                Text(
                                    text = if (remaining >= 0) "المتبقي: ${numberFormat.format(remaining)} $currencySymbol"
                                    else "🚨 تجاوزت بـ ${numberFormat.format(-remaining)} $currencySymbol",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining >= 0) MoneyIncomeGreen else MoneyExpenseRed
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { overallRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = progressColor,
                                trackColor = progressColor.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "بنود المصروفات والميزانيات المحددة:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of Expense Categories with budget status
                if (categories.isEmpty()) {
                    Text(
                        text = "لا توجد بنود مصروفات مضافة بعد.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    categories.forEach { category ->
                        val spent = monthlyExpensesByCategory[category.name.trim().lowercase()] ?: 0.0
                        val hasBudget = category.monthlyBudget > 0
                        val budget = category.monthlyBudget

                        val spentPercent = if (hasBudget) ((spent / budget) * 100).toInt() else 0
                        val alertPercent = category.alertThresholdPercent

                        val statusColor = when {
                            !hasBudget -> MaterialTheme.colorScheme.outline
                            spentPercent > 100 -> MoneyExpenseRed
                            spentPercent >= alertPercent -> Color(0xFFE65100) // Orange
                            else -> PrimaryGreen
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(statusColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    !hasBudget -> Icons.Default.AccountBalance
                                                    spentPercent > 100 -> Icons.Default.Warning
                                                    spentPercent >= alertPercent -> Icons.Default.NotificationsActive
                                                    else -> Icons.Default.Check
                                                },
                                                contentDescription = null,
                                                tint = statusColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = category.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (hasBudget) "الميزانية: ${numberFormat.format(budget)} $currencySymbol | التنبيه عند $alertPercent%"
                                                else "بدون ميزانية محددة",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { editingCategory = category },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الميزانية",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المصروف هذا الشهر: ${numberFormat.format(spent)} $currencySymbol",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (hasBudget) {
                                        Surface(
                                            color = statusColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    spentPercent > 100 -> "🚨 تجاوزت ($spentPercent%)"
                                                    spentPercent >= alertPercent -> "⚠️ تنبيه قارب ($spentPercent%)"
                                                    else -> "🟢 ضمن الحد ($spentPercent%)"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (hasBudget) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val ratio = (spent / budget).toFloat().coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = statusColor,
                                        trackColor = statusColor.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("إغلاق", color = Color.White)
            }
        }
    )

    // Edit Category Budget Modal Dialog
    editingCategory?.let { cat ->
        SetCategoryBudgetDialog(
            category = cat,
            currencySymbol = currencySymbol,
            onDismiss = { editingCategory = null },
            onSave = { budget, threshold ->
                viewModel.updateCategoryBudget(cat, budget, threshold)
                editingCategory = null
            }
        )
    }
}

@Composable
private fun SetCategoryBudgetDialog(
    category: ExpenseCategory,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit
) {
    var budgetText by remember { mutableStateOf(if (category.monthlyBudget > 0) category.monthlyBudget.toString() else "") }
    var selectedThreshold by remember { mutableStateOf(category.alertThresholdPercent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعيين ميزانية شهرية: ${category.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "حدد الحد الأقصى للمصروف المتوقع شهرياً لهذا البند ليقوم النظام بتنبيهك تلقائياً.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("الميزانية الشهرية المستهدفة ($currencySymbol)", fontSize = 12.sp) },
                    placeholder = { Text("مثال: 2500") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "🔔 حد إرسال التنبيه المبكر عند استهلاك:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val thresholdOptions = listOf(75, 80, 85, 90)
                    thresholdOptions.forEach { pct ->
                        FilterChip(
                            selected = selectedThreshold == pct,
                            onClick = { selectedThreshold = pct },
                            label = { Text("$pct%", fontSize = 11.sp) },
                            leadingIcon = if (selectedThreshold == pct) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetText.toDoubleOrNull() ?: 0.0
                    onSave(amount, selectedThreshold)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("حفظ الميزانية", color = Color.White)
            }
        },
        dismissButton = {
            Row {
                if (category.monthlyBudget > 0) {
                    TextButton(onClick = { onSave(0.0, 80) }) {
                        Text("إزالة الحد", color = MoneyExpenseRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}
