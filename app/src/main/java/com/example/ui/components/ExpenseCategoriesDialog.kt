package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseCategory
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.DecimalFormat

@Composable
fun ExpenseCategoriesDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"
    val numberFormat = remember { DecimalFormat("#,##0.##") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showBudgetsDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MoneyExpenseRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعداد بنود المصروفات", fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = {
                        categoryToEdit = null
                        showAddEditDialog = true
                    },
                    modifier = Modifier.testTag("add_expense_category_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة بند جديد", tint = PrimaryGreen)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بنود المصروفات والميزانية المحددة:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedButton(
                        onClick = { showBudgetsDialog = true },
                        modifier = Modifier.testTag("open_budgets_subdialog_btn")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📊 الميزانيات", fontSize = 11.sp, color = PrimaryGreen)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MoneyExpenseRed.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MoneyExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    val budgetLabel = if (cat.monthlyBudget > 0)
                                        "الميزانية: ${numberFormat.format(cat.monthlyBudget)} $currencySymbol (التنبيه عند ${cat.alertThresholdPercent}%)"
                                    else "بدون ميزانية شهرية"

                                    Text(
                                        text = budgetLabel,
                                        fontSize = 11.sp,
                                        color = if (cat.monthlyBudget > 0) PrimaryGreen else MaterialTheme.colorScheme.outline,
                                        fontWeight = if (cat.monthlyBudget > 0) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            categoryToEdit = cat
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                                    }

                                    if (categories.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteExpenseCategory(cat) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )

    if (showAddEditDialog) {
        AddEditExpenseCategorySubDialog(
            category = categoryToEdit,
            currencySymbol = currencySymbol,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { name, note, budget, threshold ->
                if (categoryToEdit != null) {
                    viewModel.updateExpenseCategory(categoryToEdit!!, name, note, "receipt")
                    viewModel.updateCategoryBudget(categoryToEdit!!, budget, threshold)
                } else {
                    viewModel.addExpenseCategory(name, "receipt", note, budget, threshold)
                }
                showAddEditDialog = false
            }
        )
    }

    if (showBudgetsDialog) {
        BudgetsDialog(
            viewModel = viewModel,
            onDismiss = { showBudgetsDialog = false }
        )
    }
}

@Composable
private fun AddEditExpenseCategorySubDialog(
    category: ExpenseCategory?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String, budget: Double, alertThresholdPercent: Int) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var note by remember { mutableStateOf(category?.note ?: "") }
    var budgetText by remember { mutableStateOf(if (category != null && category.monthlyBudget > 0) category.monthlyBudget.toString() else "") }
    var alertThreshold by remember { mutableStateOf(category?.alertThresholdPercent ?: 80) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (category == null) "إضافة بند مصروف جديد" else "تعديل بند المصروف",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم بند المصروف (مثال: إيجار، وقود، فواتير)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("الميزانية الشهرية المستهدفة ($currencySymbol) (اختياري)") },
                    placeholder = { Text("مثال: 3000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("الوصف أو أمثلة (يساعد الذكاء الاصطناعي في الفهم)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: بنزين، مواصلات، تاكسي، شحن") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val b = budgetText.toDoubleOrNull() ?: 0.0
                        onConfirm(name.trim(), note.trim(), b, alertThreshold)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("حفظ البند")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
