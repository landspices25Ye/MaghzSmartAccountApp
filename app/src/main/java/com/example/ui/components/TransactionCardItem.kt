package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyDebtBlue
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import java.text.DecimalFormat

@Composable
fun TransactionCardItem(
    tx: TransactionRecord,
    partyName: String?,
    boxName: String?,
    timeString: String,
    currencyFormat: DecimalFormat,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = tx.type in listOf(
        TransactionType.CUSTOMER_RECEIPT,
        TransactionType.INCOME
    )
    val color = when (tx.type) {
        TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> MoneyIncomeGreen
        TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> MoneyExpenseRed
        TransactionType.CUSTOMER_NEW_DEBIT -> MoneyDebtBlue
        TransactionType.SUPPLIER_NEW_CREDIT -> Color(0xFFC2185B)
        TransactionType.TRANSFER -> Color(0xFF00897B)
        TransactionType.SETTLEMENT -> Color(0xFF7B1FA2)
    }

    val categoryIcon = getCategoryIcon(tx.category, tx.type)
    val currencyLabel = tx.currency.ifBlank { "ر.س" }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = tx.type.titleAr,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.type.titleAr,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!partyName.isNullOrBlank()) {
                        Text(
                            text = " • $partyName",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (tx.category.isNotBlank()) {
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tx.category,
                                fontSize = 10.sp,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "${tx.description}${if (!boxName.isNullOrBlank()) " ($boxName)" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else if (tx.type == TransactionType.TRANSFER || tx.type == TransactionType.SETTLEMENT) "" else "-"}${currencyFormat.format(tx.amount)} $currencyLabel",
                    fontWeight = FontWeight.Bold,
                    color = color,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف العملية",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(category: String, type: TransactionType): ImageVector {
    val lower = category.lowercase()
    return when {
        lower.contains("بنزين") || lower.contains("نقل") || lower.contains("وقود") || lower.contains("سيارة") -> Icons.Default.DirectionsCar
        lower.contains("فواتير") || lower.contains("كهرباء") || lower.contains("ماء") || lower.contains("انترنت") || lower.contains("هاتف") -> Icons.Default.Bolt
        lower.contains("إيجار") || lower.contains("ايجار") || lower.contains("مكتب") || lower.contains("محل") -> Icons.Default.Home
        lower.contains("رواتب") || lower.contains("أجور") || lower.contains("عمال") || lower.contains("مكافأة") -> Icons.Default.Person
        lower.contains("ضيافة") || lower.contains("قهوة") || lower.contains("طعام") || lower.contains("بوفيه") || lower.contains("شاي") -> Icons.Default.Coffee
        lower.contains("صيانة") || lower.contains("تصليح") || lower.contains("قطع") -> Icons.Default.Build
        lower.contains("بضاعة") || lower.contains("مشتريات") || lower.contains("مخزون") -> Icons.Default.ShoppingBag
        lower.contains("تسويق") || lower.contains("إعلان") || lower.contains("دعاية") -> Icons.Default.Campaign
        type == TransactionType.CUSTOMER_RECEIPT || type == TransactionType.INCOME -> Icons.Default.ArrowDownward
        type == TransactionType.SUPPLIER_PAYMENT || type == TransactionType.EXPENSE -> Icons.Default.ArrowUpward
        type == TransactionType.TRANSFER -> Icons.Default.AccountBalanceWallet
        else -> Icons.AutoMirrored.Filled.ReceiptLong
    }
}
