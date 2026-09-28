package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import java.text.DecimalFormat

data class DailyCashFlowPoint(
    val dateLabel: String,
    val fullDate: String,
    val income: Double,
    val expense: Double,
    val net: Double = income - expense
)

@Composable
fun DailyCashFlowChart(
    data: List<DailyCashFlowPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد بيانات تدفق نقدي كافية للعرض",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    val currencyFormat = remember { DecimalFormat("#,##0.##") }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Animation progress
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val maxVal = remember(data) {
        val highest = data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0
        if (highest <= 0) 100.0 else highest * 1.15
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_cash_flow_chart")
    ) {
        // Chart Legend & Selected Tooltip Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Legends (Income & Expense)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MoneyIncomeGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("الدخل والمقبوضات", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MoneyExpenseRed)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("المصروفات والمدفوعات", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 3.dp)
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("صافي التدفق", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Active Selection Popover
        val activePoint = selectedIndex?.let { data.getOrNull(it) } ?: data.lastOrNull()
        if (activePoint != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${activePoint.fullDate}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "دخل: +${currencyFormat.format(activePoint.income)}",
                            color = MoneyIncomeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "صرف: -${currencyFormat.format(activePoint.expense)}",
                            color = MoneyExpenseRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "صافي: ${currencyFormat.format(activePoint.net)}",
                            color = if (activePoint.net >= 0) MoneyIncomeGreen else MoneyExpenseRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Canvas Recharts-style grouped bars and net flow line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val sectionWidth = size.width / data.size
                            val tappedIndex = (offset.x / sectionWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = tappedIndex
                        }
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height - 30.dp.toPx() // leave room for labels
                val count = data.size
                val groupWidth = chartWidth / count
                val barWidth = (groupWidth * 0.32f).coerceAtMost(22.dp.toPx())
                val spacing = 3.dp.toPx()

                // Draw horizontal guide lines
                val steps = 3
                for (i in 0..steps) {
                    val y = chartHeight - (chartHeight / steps * i)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val netPoints = mutableListOf<Offset>()

                data.forEachIndexed { index, point ->
                    val centerX = groupWidth * index + groupWidth / 2f
                    val incomeBarX = centerX - barWidth - spacing / 2
                    val expenseBarX = centerX + spacing / 2

                    val incomeHeight = ((point.income / maxVal) * chartHeight * animProgress.value).toFloat()
                    val expenseHeight = ((point.expense / maxVal) * chartHeight * animProgress.value).toFloat()

                    val isSelected = selectedIndex == index

                    // Highlight column if selected
                    if (isSelected) {
                        drawRoundRect(
                            color = Color.Gray.copy(alpha = 0.1f),
                            topLeft = Offset(groupWidth * index, 0f),
                            size = Size(groupWidth, chartHeight),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }

                    // Draw Income Bar (Emerald gradient)
                    if (incomeHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2E7D32), Color(0xFF81C784)),
                                startY = chartHeight - incomeHeight,
                                endY = chartHeight
                            ),
                            topLeft = Offset(incomeBarX, chartHeight - incomeHeight),
                            size = Size(barWidth, incomeHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Draw Expense Bar (Coral Red gradient)
                    if (expenseHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFC62828), Color(0xFFEF9A9A)),
                                startY = chartHeight - expenseHeight,
                                endY = chartHeight
                            ),
                            topLeft = Offset(expenseBarX, chartHeight - expenseHeight),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Net point for spline
                    val normalizedNet = (point.net / maxVal).coerceIn(-1.0, 1.0)
                    val netY = chartHeight - (((point.income - point.expense) / maxVal).coerceAtLeast(0.0) * chartHeight * animProgress.value).toFloat()
                    netPoints.add(Offset(centerX, netY.coerceIn(4.dp.toPx(), chartHeight)))
                }

                // Draw Net flow trend line
                if (netPoints.size > 1) {
                    val linePath = Path().apply {
                        moveTo(netPoints.first().x, netPoints.first().y)
                        for (i in 1 until netPoints.size) {
                            val prev = netPoints[i - 1]
                            val curr = netPoints[i]
                            val midX = (prev.x + curr.x) / 2
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = Color(0xFF1976D2),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots on net flow
                    netPoints.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color(0xFF1976D2),
                            radius = 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Date Labels Row underneath chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEachIndexed { i, point ->
                    Text(
                        text = point.dateLabel,
                        fontSize = 10.sp,
                        color = if (selectedIndex == i) PrimaryGreen else MaterialTheme.colorScheme.outline,
                        fontWeight = if (selectedIndex == i) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
