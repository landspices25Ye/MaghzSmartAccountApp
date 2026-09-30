package com.example.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionCardItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentTransactionsSection(
    recentTransactions: List<TransactionRecord>,
    parties: List<Party>,
    cashBoxes: List<CashBox>,
    defaultCurrencySymbol: String,
    onNavigateToTransactions: () -> Unit,
    onDeleteTransaction: (TransactionRecord) -> Unit,
    onTransactionClick: ((TransactionRecord) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("الكل") }

    val partyMap = remember(parties) { parties.associateBy { it.id } }
    val boxMap = remember(cashBoxes) { cashBoxes.associateBy { it.id } }

    val filteredList = remember(recentTransactions, selectedFilter) {
        when (selectedFilter) {
            "مقبوضات" -> recentTransactions.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }
            "مدفوعات" -> recentTransactions.filter { it.type == TransactionType.SUPPLIER_PAYMENT }
            "مصروفات" -> recentTransactions.filter { it.type == TransactionType.EXPENSE }
            "ديون" -> recentTransactions.filter { it.type == TransactionType.CUSTOMER_NEW_DEBIT || it.type == TransactionType.SUPPLIER_NEW_CREDIT }
            else -> recentTransactions
        }
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
                text = "آخر الحركات والعمليات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = onNavigateToTransactions,
                modifier = Modifier.testTag("dashboard_view_all_transactions_button")
            ) {
                Text("عرض الكل", fontSize = 12.sp)
            }
        }

        // Quick Category Filter Row
        val filters = listOf("الكل", "مقبوضات", "مدفوعات", "مصروفات", "ديون")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filterName ->
                FilterChip(
                    selected = selectedFilter == filterName,
                    onClick = { selectedFilter = filterName },
                    label = { Text(filterName, fontSize = 12.sp) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد حركات مسجلة ضمن هذا التصنيف",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
            val currencyFormat = remember { DecimalFormat("#,##0.##") }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredList.take(6).forEach { tx ->
                    val partyName = tx.partyId?.let { partyMap[it]?.name }
                    val boxName = tx.cashBoxId?.let { boxMap[it]?.name }
                    val timeString = timeFormat.format(Date(tx.timestamp))

                    TransactionCardItem(
                        tx = tx,
                        partyName = partyName,
                        boxName = boxName,
                        timeString = timeString,
                        currencyFormat = currencyFormat,
                        defaultCurrencySymbol = defaultCurrencySymbol,
                        onClick = { onTransactionClick?.invoke(tx) },
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }
}
