package com.example.ui.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterSheet(
    currencySymbol: String,
    minAmount: Double?,
    maxAmount: Double?,
    onMinAmountChanged: (Double?) -> Unit,
    onMaxAmountChanged: (Double?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "تصفية متقدمة لنتائج البحث",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Amount Range
            Text("نطاق المبلغ ($currencySymbol):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minAmount?.toString() ?: "",
                    onValueChange = { onMinAmountChanged(it.toDoubleOrNull()) },
                    label = { Text("الحد الأدنى") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxAmount?.toString() ?: "",
                    onValueChange = { onMaxAmountChanged(it.toDoubleOrNull()) },
                    label = { Text("الحد الأقصى") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Quick Amount Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {
                        onMinAmountChanged(0.0)
                        onMaxAmountChanged(100.0)
                    },
                    label = { Text("< 100 $currencySymbol", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = {
                        onMinAmountChanged(100.0)
                        onMaxAmountChanged(500.0)
                    },
                    label = { Text("100 - 500", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = {
                        onMinAmountChanged(500.0)
                        onMaxAmountChanged(2000.0)
                    },
                    label = { Text("500 - 2000", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = {
                        onMinAmountChanged(2000.0)
                        onMaxAmountChanged(null)
                    },
                    label = { Text("> 2000 $currencySymbol", fontSize = 11.sp) }
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تطبيق التصفية")
                }
                OutlinedButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء التصفية")
                }
            }
        }
    }
}
