package com.example.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyDebtBlue
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen

@Composable
fun QuickActionsGrid(
    onActionClick: (TransactionType) -> Unit,
    onTransferClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "إجراءات سريعة فورية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                title = "قبض عميل",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = MoneyIncomeGreen,
                onClick = { onActionClick(TransactionType.CUSTOMER_RECEIPT) },
                tag = "quick_action_customer_receipt",
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                title = "دفع مورد",
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                color = MoneyExpenseRed,
                onClick = { onActionClick(TransactionType.SUPPLIER_PAYMENT) },
                tag = "quick_action_supplier_payment",
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                title = "تسجيل مصروف",
                icon = Icons.Default.Payments,
                color = Color(0xFFE11D48),
                onClick = { onActionClick(TransactionType.EXPENSE) },
                tag = "quick_action_expense",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                title = "دين عميل",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = MoneyDebtBlue,
                onClick = { onActionClick(TransactionType.CUSTOMER_NEW_DEBIT) },
                tag = "quick_action_new_debit",
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                title = "تحويل صناديق",
                icon = Icons.Default.SwapHoriz,
                color = MoneyGold,
                onClick = onTransferClick,
                tag = "quick_action_transfer",
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                title = "تسجيل صوتي",
                icon = Icons.Default.Mic,
                color = PrimaryGreen,
                onClick = onVoiceClick,
                tag = "quick_action_voice",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
