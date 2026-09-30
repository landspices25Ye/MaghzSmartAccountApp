package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CashBox
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferCashDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"

    var fromBoxId by remember(cashBoxes) {
        mutableStateOf(cashBoxes.firstOrNull { it.isDefault }?.id ?: cashBoxes.firstOrNull()?.id ?: 0L)
    }
    var toBoxId by remember(cashBoxes) {
        mutableStateOf(cashBoxes.drop(1).firstOrNull()?.id ?: 0L)
    }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    val fromBox = cashBoxes.firstOrNull { it.id == fromBoxId }
    val toBox = cashBoxes.firstOrNull { it.id == toBoxId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تحويل نقدية بين الصناديق والخزائن", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // From Box
                ExposedDropdownMenuBox(
                    expanded = expandedFrom,
                    onExpandedChange = { expandedFrom = it }
                ) {
                    OutlinedTextField(
                        value = fromBox?.name ?: "اختر الصندوق المحول منه",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("من صندوق (المصدر)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFrom,
                        onDismissRequest = { expandedFrom = false }
                    ) {
                        cashBoxes.forEach { box ->
                            DropdownMenuItem(
                                text = { Text("${box.name} (رصيده: ${box.balance} $currencySymbol)") },
                                onClick = {
                                    fromBoxId = box.id
                                    expandedFrom = false
                                }
                            )
                        }
                    }
                }

                // To Box
                ExposedDropdownMenuBox(
                    expanded = expandedTo,
                    onExpandedChange = { expandedTo = it }
                ) {
                    OutlinedTextField(
                        value = toBox?.name ?: "اختر الصندوق المحول إليه",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("إلى صندوق (الوجهة)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTo,
                        onDismissRequest = { expandedTo = false }
                    ) {
                        cashBoxes.filter { it.id != fromBoxId }.forEach { box ->
                            DropdownMenuItem(
                                text = { Text("${box.name} (رصيده: ${box.balance} $currencySymbol)") },
                                onClick = {
                                    toBoxId = box.id
                                    expandedTo = false
                                }
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المحول ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("بيان وملاحظات التحويل (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            val isValid = amount > 0 && fromBoxId > 0 && toBoxId > 0 && fromBoxId != toBoxId
            Button(
                onClick = {
                    if (isValid) {
                        viewModel.transferBetweenBoxes(fromBoxId, toBoxId, amount, notes)
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_transfer_btn")
            ) {
                Text("تأكيد التحويل")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
