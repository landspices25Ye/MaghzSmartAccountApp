package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CashBox

@Composable
fun AddCashBoxDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, initialBalance: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var initialBalanceText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة صندوق / خزينة / حساب",
                style = MaterialTheme.typography.titleLarge,
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
                    label = { Text("اسم الصندوق أو الحساب") },
                    placeholder = { Text("مثال: المحفظة، حساب الراجحي، خزنة المحل") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashbox_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("الرصيد الافتتاحي (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val initBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                        onConfirm(name.trim(), initBal, note.trim())
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_cashbox_btn")
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferCashDialog(
    cashBoxes: List<CashBox>,
    onDismiss: () -> Unit,
    onConfirm: (fromBoxId: Long, toBoxId: Long, amount: Double, note: String) -> Unit
) {
    var fromBoxId by remember { mutableStateOf(cashBoxes.firstOrNull()?.id ?: 0L) }
    var toBoxId by remember { mutableStateOf(cashBoxes.getOrNull(1)?.id ?: cashBoxes.firstOrNull()?.id ?: 0L) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    val fromBoxName = cashBoxes.firstOrNull { it.id == fromBoxId }?.name ?: ""
    val toBoxName = cashBoxes.firstOrNull { it.id == toBoxId }?.name ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تحويل نقدية بين الصناديق",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // From box
                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = !fromExpanded }
                ) {
                    OutlinedTextField(
                        value = fromBoxName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("من صندوق (المصدر)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        cashBoxes.forEach { box ->
                            DropdownMenuItem(
                                text = { Text("${box.name} (${box.balance} ر.س)") },
                                onClick = {
                                    fromBoxId = box.id
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                // To box
                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = !toExpanded }
                ) {
                    OutlinedTextField(
                        value = toBoxName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("إلى صندوق (المستلم)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        cashBoxes.filter { it.id != fromBoxId }.forEach { box ->
                            DropdownMenuItem(
                                text = { Text("${box.name} (${box.balance} ر.س)") },
                                onClick = {
                                    toBoxId = box.id
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المحول (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && fromBoxId != toBoxId) {
                        onConfirm(fromBoxId, toBoxId, amount, note.trim())
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && fromBoxId != toBoxId
            ) {
                Text("تحويل الآن")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
