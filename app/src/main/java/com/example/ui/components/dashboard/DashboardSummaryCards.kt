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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MoneyDebtBlue
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.FinancialSummary
import java.text.DecimalFormat

@Composable
fun DashboardSummaryCards(
    summary: FinancialSummary,
    onNavigateToCashBoxes: () -> Unit,
    onNavigateToParties: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DecimalFormat("#,##0.##") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Primary Highlight: Net Worth Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("dashboard_net_worth_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "صافي الثروة ورأس المال",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${formatter.format(summary.netWorth)} ${summary.currencySymbol}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (summary.netWorth >= 0) PrimaryGreen else MoneyExpenseRed
                    )

                    Text(
                        text = "= النقدية المتوفرة + ديون العملاء - ديون الموردين",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (summary.netWorth >= 0) PrimaryGreen.copy(alpha = 0.15f) else MoneyExpenseRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (summary.netWorth >= 0) "إيجابي" else "عجز",
                        color = if (summary.netWorth >= 0) PrimaryGreen else MoneyExpenseRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Row of Cash in hand & Receivables
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryItemCard(
                title = "النقدية بالصناديق",
                amount = summary.totalCash,
                currencySymbol = summary.currencySymbol,
                icon = Icons.Default.AccountBalanceWallet,
                color = MoneyIncomeGreen,
                onClick = onNavigateToCashBoxes,
                tag = "dashboard_total_cash_card",
                modifier = Modifier.weight(1f)
            )

            SummaryItemCard(
                title = "ما لي عند العملاء",
                amount = summary.owedToMe,
                currencySymbol = summary.currencySymbol,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = MoneyDebtBlue,
                onClick = onNavigateToParties,
                tag = "dashboard_owed_to_me_card",
                modifier = Modifier.weight(1f)
            )
        }

        // Row of Payables
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryItemCard(
                title = "ما علي للموردين",
                amount = summary.owedByMe,
                currencySymbol = summary.currencySymbol,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                color = MoneyExpenseRed,
                onClick = onNavigateToParties,
                tag = "dashboard_owed_by_me_card",
                modifier = Modifier.weight(1f)
            )

            // Liquidity Coverage Ratio or Total Assets
            val totalAssets = summary.totalCash + summary.owedToMe
            SummaryItemCard(
                title = "إجمالي الأصول",
                amount = totalAssets,
                currencySymbol = summary.currencySymbol,
                icon = Icons.Default.Payments,
                color = MoneyGold,
                onClick = onNavigateToCashBoxes,
                tag = "dashboard_total_assets_card",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryItemCard(
    title: String,
    amount: Double,
    currencySymbol: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DecimalFormat("#,##0.##") }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "عرض",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatter.format(amount)} $currencySymbol",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}
