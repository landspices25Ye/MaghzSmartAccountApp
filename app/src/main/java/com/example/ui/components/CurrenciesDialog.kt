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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppCurrency
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel

@Composable
fun CurrenciesDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var currencyToEdit by remember { mutableStateOf<AppCurrency?>(null) }

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
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعدادات العملات", fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = {
                        currencyToEdit = null
                        showAddEditDialog = true
                    },
                    modifier = Modifier.testTag("add_currency_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة عملة جديدة", tint = PrimaryGreen)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "حدد العملة الافتراضية التي سيتم استخدامها تلقائياً عند عدم ذكر العملة في العمليات والصوت:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currencies, key = { it.id }) { currency ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currency.isDefault) PrimaryGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (currency.isDefault) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen) else null
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
                                        .background(if (currency.isDefault) PrimaryGreen else Color.Gray.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currency.symbol,
                                        color = if (currency.isDefault) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currency.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${currency.code})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (currency.isDefault) {
                                        Surface(
                                            color = PrimaryGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "العملة الافتراضية الرئيسية ✓",
                                                color = PrimaryGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!currency.isDefault) {
                                        OutlinedButton(
                                            onClick = { viewModel.setDefaultCurrency(currency.id) },
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("تعيين كافتراضية", fontSize = 10.sp)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            currencyToEdit = currency
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                                    }

                                    if (!currency.isDefault && currencies.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteCurrency(currency) },
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
        AddEditCurrencySubDialog(
            currency = currencyToEdit,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { code, name, symbol, isDefault ->
                if (currencyToEdit != null) {
                    viewModel.updateCurrency(currencyToEdit!!, code, name, symbol, isDefault)
                } else {
                    viewModel.addCurrency(code, name, symbol, isDefault)
                }
                showAddEditDialog = false
            }
        )
    }
}

@Composable
private fun AddEditCurrencySubDialog(
    currency: AppCurrency?,
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String, symbol: String, isDefault: Boolean) -> Unit
) {
    var code by remember { mutableStateOf(currency?.code ?: "") }
    var name by remember { mutableStateOf(currency?.name ?: "") }
    var symbol by remember { mutableStateOf(currency?.symbol ?: "") }
    var isDefault by remember { mutableStateOf(currency?.isDefault ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currency == null) "إضافة عملة جديدة" else "تعديل بيانات العملة",
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
                    label = { Text("اسم العملة (مثال: ريال سعودي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("رمز أو اختصار العملة (مثال: ر.س أو $)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود العملة الدولي (مثال: SAR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جعلها العملة الافتراضية للنظام", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && symbol.isNotBlank()) {
                        onConfirm(code.ifBlank { symbol }, name, symbol, isDefault)
                    }
                },
                enabled = name.isNotBlank() && symbol.isNotBlank()
            ) {
                Text("حفظ العملة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
