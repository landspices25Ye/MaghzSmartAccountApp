package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyOverviewSection(
    transactions: List<TransactionRecord>,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { DecimalFormat("#,##0.##") }
    var selectedDaysCount by remember { mutableIntStateOf(7) } // 7, 14, 30 days
    var visualizerMode by remember { mutableStateOf(0) } // 0: Native Compose Recharts style, 1: Recharts Web
    var isExpanded by remember { mutableStateOf(true) }

    val dayLabelFormat = remember { SimpleDateFormat("EE", Locale("ar")) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("ar")) }
    val keyDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    // Generate daily points for the last N days
    val dailyPoints = remember(transactions, selectedDaysCount) {
        val points = mutableListOf<DailyCashFlowPoint>()
        val cal = Calendar.getInstance()

        // Build list of days from (N-1) days ago to today
        val dayTimestamps = mutableListOf<Long>()
        for (i in (selectedDaysCount - 1) downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            dayTimestamps.add(c.timeInMillis)
        }

        dayTimestamps.forEach { dayStart ->
            val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
            val dayTxs = transactions.filter { it.timestamp in dayStart..dayEnd }

            var income = 0.0
            var expense = 0.0

            dayTxs.forEach { tx ->
                when (tx.type) {
                    TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> income += tx.amount
                    TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> expense += tx.amount
                    else -> {}
                }
            }

            val d = Date(dayStart)
            points.add(
                DailyCashFlowPoint(
                    dateLabel = dayLabelFormat.format(d),
                    fullDate = fullDateFormat.format(d),
                    income = income,
                    expense = expense
                )
            )
        }
        points
    }

    val totalIncome = remember(dailyPoints) { dailyPoints.sumOf { it.income } }
    val totalExpense = remember(dailyPoints) { dailyPoints.sumOf { it.expense } }
    val netCashFlow = totalIncome - totalExpense

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_overview_dashboard_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "النظرة اليومية للتدفق النقدي (Daily Overview)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مقارنة الدخل والمصروفات اليومية بالرسم البياني",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    // Controls Bar: Period Selection & Visualizer Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Days count chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(7 to "7 أيام", 14 to "14 يوم", 30 to "30 يوم").forEach { (days, label) ->
                                FilterChip(
                                    selected = selectedDaysCount == days,
                                    onClick = { selectedDaysCount = days },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Mode switcher (Native Compose vs Recharts Web)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = visualizerMode == 0,
                                onClick = { visualizerMode = 0 },
                                label = { Text("المخطط", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                            FilterChip(
                                selected = visualizerMode == 1,
                                onClick = { visualizerMode = 1 },
                                label = { Text("Recharts", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Period KPI Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Income KPI
                        Surface(
                            color = MoneyIncomeGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MoneyIncomeGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إجمالي الدخل", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${currencyFormat.format(totalIncome)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyIncomeGreen,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Expense KPI
                        Surface(
                            color = MoneyExpenseRed.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = MoneyExpenseRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إجمالي المصروف", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "-${currencyFormat.format(totalExpense)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyExpenseRed,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Net Flow KPI
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("صافي التدفق", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${currencyFormat.format(netCashFlow)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (netCashFlow >= 0) MoneyIncomeGreen else MoneyExpenseRed,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // The Visualizer Chart (Native Compose Recharts-style or Interactive Recharts WebView)
                    if (visualizerMode == 0) {
                        DailyCashFlowChart(data = dailyPoints)
                    } else {
                        RechartsCashFlowWebView(data = dailyPoints)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Ledger Breakdown Rows
                    Text(
                        text = "تفصيل الأيام المنصرمة (${dailyPoints.size} أيام)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    dailyPoints.takeLast(5).reversed().forEach { point ->
                        val dayTotal = point.income + point.expense
                        val incomeRatio = if (dayTotal > 0) (point.income / dayTotal).toFloat() else 0.5f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(0.35f)) {
                                Text(point.fullDate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(point.dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }

                            // Visual Income vs Expense Ratio Bar
                            Column(modifier = Modifier.weight(0.35f).padding(horizontal = 6.dp)) {
                                LinearProgressIndicator(
                                    progress = { incomeRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MoneyIncomeGreen,
                                    trackColor = MoneyExpenseRed
                                )
                            }

                            Column(modifier = Modifier.weight(0.3f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (point.net >= 0) "+" else ""}${currencyFormat.format(point.net)} ر.س",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (point.net >= 0) MoneyIncomeGreen else MoneyExpenseRed
                                )
                                Text(
                                    text = "+${currencyFormat.format(point.income)} | -${currencyFormat.format(point.expense)}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
