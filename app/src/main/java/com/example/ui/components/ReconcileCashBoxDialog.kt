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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox
import java.text.DecimalFormat

@Composable
fun ReconcileCashBoxDialog(
    cashBox: CashBox,
    currencySymbol: String = "ر.س",
    onDismiss: () -> Unit,
    onConfirm: (physicalBalance: Double, note: String) -> Unit
) {
    val decimalFormat = remember { DecimalFormat("#,##0.##") }
    var physicalBalanceText by remember { mutableStateOf(cashBox.balance.toString()) }
    var note by remember { mutableStateOf("تسوية جرد فعلي للنقدية") }

    val physicalBalance by remember {
        derivedStateOf { physicalBalanceText.toDoubleOrNull() ?: cashBox.balance }
    }

    val difference by remember {
        derivedStateOf { physicalBalance - cashBox.balance }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جرد وتسوية رصيد الصندوق",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "الصندوق: ${cashBox.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Book Balance display
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "الرصيد الدفتري المسجل:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${decimalFormat.format(cashBox.balance)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Physical Count Input
                OutlinedTextField(
                    value = physicalBalanceText,
                    onValueChange = { physicalBalanceText = it },
                    label = { Text("الرصيد الفعلي بعد العد والجرد") },
                    placeholder = { Text("أدخل المبلغ الفعلي الموجود...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reconcile_physical_balance_input")
                )

                // Real-time difference analysis
                val diffColor = when {
                    Math.abs(difference) < 0.001 -> Color(0xFF10B981)
                    difference > 0 -> Color(0xFF059669)
                    else -> Color(0xFFDC2626)
                }

                val diffMessage = when {
                    Math.abs(difference) < 0.001 -> "الرصيد مطابق تماماً للجرد الدفتري (لا يوجد عجز أو فائض) ✓"
                    difference > 0 -> "يوجد فائض نقدي قدره +${decimalFormat.format(difference)} $currencySymbol (سيُقيد كإيراد تسوية جرد)"
                    else -> "يوجد عجز نقدي قدره ${decimalFormat.format(difference)} $currencySymbol (سيُقيد كمصروف تسوية عجز)"
                }

                Surface(
                    color = diffColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (Math.abs(difference) < 0.001) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = diffColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = diffMessage,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = diffColor
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة / سبب التسوية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(physicalBalance, note.trim())
                },
                modifier = Modifier.testTag("confirm_reconcile_button")
            ) {
                Text("اعتماد التسوية وترحيل القيد", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ReconcileCashBoxDialog(
    cashBoxes: List<CashBox>,
    defaultCurrencySymbol: String = "ر.س",
    onDismiss: () -> Unit,
    onConfirm: (boxId: Long, physicalBalance: Double, note: String) -> Unit
) {
    var selectedBoxId by remember { mutableStateOf(cashBoxes.firstOrNull { it.isDefault }?.id ?: cashBoxes.firstOrNull()?.id ?: 0L) }
    val selectedBox = cashBoxes.firstOrNull { it.id == selectedBoxId } ?: cashBoxes.firstOrNull()

    if (selectedBox == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("تسوية الصناديق") },
            text = { Text("لا توجد أي صناديق أو خزائن مسجلة بعد.") },
            confirmButton = { Button(onClick = onDismiss) { Text("حسناً") } }
        )
        return
    }

    ReconcileCashBoxDialog(
        cashBox = selectedBox,
        currencySymbol = defaultCurrencySymbol,
        onDismiss = onDismiss,
        onConfirm = { physicalBal, note ->
            onConfirm(selectedBox.id, physicalBal, note)
        }
    )
}
