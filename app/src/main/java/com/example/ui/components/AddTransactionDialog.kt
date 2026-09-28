package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCurrency
import com.example.data.model.CashBox
import com.example.data.model.ExpenseCategory
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    cashBoxes: List<CashBox>,
    parties: List<Party>,
    categories: List<ExpenseCategory> = emptyList(),
    currencies: List<AppCurrency> = emptyList(),
    defaultCurrencySymbol: String = "ر.س",
    initialType: TransactionType = TransactionType.CUSTOMER_RECEIPT,
    initialPartyId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        type: TransactionType,
        amount: Double,
        cashBoxId: Long?,
        partyId: Long?,
        description: String,
        targetCashBoxId: Long?,
        category: String,
        currency: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "نثرية ومصروفات عامة") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrencySymbol) }

    var selectedBoxId by remember {
        mutableStateOf(cashBoxes.firstOrNull { it.isDefault }?.id ?: cashBoxes.firstOrNull()?.id)
    }
    var targetBoxId by remember {
        mutableStateOf(cashBoxes.getOrNull(1)?.id ?: cashBoxes.firstOrNull()?.id)
    }
    var selectedPartyId by remember {
        mutableStateOf(initialPartyId ?: parties.firstOrNull {
            if (selectedType == TransactionType.CUSTOMER_RECEIPT || selectedType == TransactionType.CUSTOMER_NEW_DEBIT) {
                it.type == PartyType.CUSTOMER
            } else {
                it.type == PartyType.SUPPLIER
            }
        }?.id)
    }

    var boxExpanded by remember { mutableStateOf(false) }
    var partyExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    val relevantParties = remember(selectedType, parties) {
        when (selectedType) {
            TransactionType.CUSTOMER_RECEIPT, TransactionType.CUSTOMER_NEW_DEBIT ->
                parties.filter { it.type == PartyType.CUSTOMER }
            TransactionType.SUPPLIER_PAYMENT, TransactionType.SUPPLIER_NEW_CREDIT ->
                parties.filter { it.type == PartyType.SUPPLIER }
            else -> parties
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل عملية مالية جديدة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type chips
                Text("نوع العملية:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.CUSTOMER_RECEIPT,
                            onClick = { selectedType = TransactionType.CUSTOMER_RECEIPT },
                            label = { Text("قبض من عميل") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.SUPPLIER_PAYMENT,
                            onClick = { selectedType = TransactionType.SUPPLIER_PAYMENT },
                            label = { Text("دفع لمورد") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.CUSTOMER_NEW_DEBIT,
                            onClick = { selectedType = TransactionType.CUSTOMER_NEW_DEBIT },
                            label = { Text("دين على عميل") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.SUPPLIER_NEW_CREDIT,
                            onClick = { selectedType = TransactionType.SUPPLIER_NEW_CREDIT },
                            label = { Text("دين من مورد") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = { Text("مصروف") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            label = { Text("إيراد نقدي") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Amount & Currency Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transaction_amount_input"),
                        singleLine = true
                    )

                    // Currency Dropdown
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(0.7f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("العملة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            if (currencies.isNotEmpty()) {
                                currencies.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (${c.symbol})") },
                                        onClick = {
                                            selectedCurrency = c.symbol
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            } else {
                                listOf("ر.س", "$", "ر.ي", "ج.م", "د.إ", "د.ك").forEach { sym ->
                                    DropdownMenuItem(
                                        text = { Text(sym) },
                                        onClick = {
                                            selectedCurrency = sym
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Category selection (if EXPENSE)
                if (selectedType == TransactionType.EXPENSE && categories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("بند المصروف (تصنيف المصروف)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategory = cat.name
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Party selection (if customer or supplier involved)
                if (selectedType in listOf(
                        TransactionType.CUSTOMER_RECEIPT,
                        TransactionType.SUPPLIER_PAYMENT,
                        TransactionType.CUSTOMER_NEW_DEBIT,
                        TransactionType.SUPPLIER_NEW_CREDIT
                    )
                ) {
                    val partyLabel = if (selectedType in listOf(TransactionType.CUSTOMER_RECEIPT, TransactionType.CUSTOMER_NEW_DEBIT)) "العميل (المدين)" else "المورد (الدائن)"
                    val currentPartyName = relevantParties.firstOrNull { it.id == selectedPartyId }?.name ?: "اختر الطرف..."

                    ExposedDropdownMenuBox(
                        expanded = partyExpanded,
                        onExpandedChange = { partyExpanded = !partyExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentPartyName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(partyLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = partyExpanded,
                            onDismissRequest = { partyExpanded = false }
                        ) {
                            relevantParties.forEach { party ->
                                DropdownMenuItem(
                                    text = { Text(party.name) },
                                    onClick = {
                                        selectedPartyId = party.id
                                        partyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Cash Box selection (Defaults to default cash box)
                if (selectedType != TransactionType.CUSTOMER_NEW_DEBIT && selectedType != TransactionType.SUPPLIER_NEW_CREDIT) {
                    val currentBox = cashBoxes.firstOrNull { it.id == selectedBoxId } ?: cashBoxes.firstOrNull { it.isDefault } ?: cashBoxes.firstOrNull()
                    val currentBoxName = currentBox?.let { "${it.name}${if (it.isDefault) " (افتراضي)" else ""}" } ?: "الصندوق الرئيسي"
                    ExposedDropdownMenuBox(
                        expanded = boxExpanded,
                        onExpandedChange = { boxExpanded = !boxExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentBoxName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الصندوق / الخزينة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boxExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = boxExpanded,
                            onDismissRequest = { boxExpanded = false }
                        ) {
                            cashBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = { Text("${box.name} (${box.balance} $selectedCurrency)${if (box.isDefault) " ★ افتراضي" else ""}") },
                                    onClick = {
                                        selectedBoxId = box.id
                                        boxExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("البيان / ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: دفعة عن فاتورة رقم 12 أو بنزين سيارة") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        val categoryParam = if (selectedType == TransactionType.EXPENSE) selectedCategory else ""
                        onConfirm(
                            selectedType,
                            amount,
                            selectedBoxId,
                            selectedPartyId,
                            description,
                            targetBoxId,
                            categoryParam,
                            selectedCurrency
                        )
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("confirm_transaction_btn")
            ) {
                Text("حفظ العملية")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
