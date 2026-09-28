package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox

@Composable
fun EditCashBoxDialog(
    cashBox: CashBox,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newNote: String, isDefault: Boolean, adjustedBalance: Double?) -> Unit
) {
    var name by remember { mutableStateOf(cashBox.name) }
    var note by remember { mutableStateOf(cashBox.note) }
    var isDefault by remember { mutableStateOf(cashBox.isDefault) }
    var balanceText by remember { mutableStateOf(cashBox.balance.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل بيانات الصندوق / الخزينة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصندوق / الخزينة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_cashbox_name_input")
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("الرصيد الحالي (تعديل / تسوية)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات أو وصف") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: الصندوق النقدي الأساسي للمحل") }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تعيين كصندوق افتراضي (يُستخدم تلقائياً عند عدم التحديد)",
                        fontSize = 12.sp,
                        fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedBal = balanceText.toDoubleOrNull()
                        onConfirm(name.trim(), note.trim(), isDefault, parsedBal)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("edit_cashbox_confirm_btn")
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
