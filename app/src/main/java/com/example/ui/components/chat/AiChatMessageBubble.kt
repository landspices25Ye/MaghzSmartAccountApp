package com.example.ui.components.chat

import android.content.Context
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.export.ExcelExportHelper
import com.example.export.PdfExportHelper
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.ChatReportAction
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiChatMessageBubble(
    message: ChatMessage,
    parties: List<Party>,
    cashBoxes: List<CashBox>,
    transactions: List<TransactionRecord>,
    defaultCurrencySymbol: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val isUser = message.isUser

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                color = PrimaryGreen,
                shape = CircleShape,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) PrimaryGreen else MaterialTheme.colorScheme.surface,
                tonalElevation = if (isUser) 0.dp else 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )

                    // Learned memory indicator card
                    if (!message.learnedMemoryText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (isUser) Color.White.copy(alpha = 0.2f) else PrimaryGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = if (isUser) Color.White else PrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تم الحفظ في الذاكرة: \"${message.learnedMemoryText}\"",
                                    fontSize = 11.sp,
                                    color = if (isUser) Color.White else PrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Interactive Report Export Card
                    message.reportAction?.let { report ->
                        Spacer(modifier = Modifier.height(10.dp))
                        ChatReportExportCard(
                            report = report,
                            context = context,
                            parties = parties,
                            cashBoxes = cashBoxes,
                            transactions = transactions,
                            currencySymbol = defaultCurrencySymbol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatReportExportCard(
    report: ChatReportAction,
    context: Context,
    parties: List<Party>,
    cashBoxes: List<CashBox>,
    transactions: List<TransactionRecord>,
    currencySymbol: String
) {
    val formatter = remember { DecimalFormat("#,##0.##") }

    // Accurately resolve target party or cash box
    val matchedParty = remember(report, parties) {
        if (report.partyId != null) {
            parties.firstOrNull { it.id == report.partyId }
        } else if (!report.partyName.isNullOrBlank()) {
            parties.firstOrNull { it.name.contains(report.partyName, ignoreCase = true) }
        } else null
    }

    val matchedCashBox = remember(report, cashBoxes) {
        if (report.cashBoxId != null) {
            cashBoxes.firstOrNull { it.id == report.cashBoxId }
        } else if (!report.cashBoxName.isNullOrBlank()) {
            cashBoxes.firstOrNull { it.name.contains(report.cashBoxName, ignoreCase = true) }
        } else null
    }

    // Filter transactions specifically for this requested report only
    val filteredTransactions = remember(report, transactions, matchedParty, matchedCashBox) {
        when {
            matchedParty != null -> {
                transactions.filter { it.partyId == matchedParty.id }
            }
            matchedCashBox != null -> {
                transactions.filter { it.cashBoxId == matchedCashBox.id || it.targetCashBoxId == matchedCashBox.id }
            }
            report.reportType.equals("DAILY", ignoreCase = true) -> {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                transactions.filter { it.timestamp >= startOfDay }
            }
            else -> transactions
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "📊 ${report.reportTitle}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (matchedParty != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "مدين (عليه): ${formatter.format(report.totalIn)} $currencySymbol",
                        fontSize = 11.sp,
                        color = MoneyIncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "دائن (له): ${formatter.format(report.totalOut)} $currencySymbol",
                        fontSize = 11.sp,
                        color = MoneyExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                val isCustomer = matchedParty.type == PartyType.CUSTOMER
                val balanceSide = if (isCustomer) {
                    if (report.netBalance >= 0) "عليه (لنا عنده)" else "له (علينا له)"
                } else {
                    if (report.netBalance >= 0) "له (علينا له)" else "عليه (لنا عنده)"
                }
                Text(
                    text = "الرصيد الصافي: ${formatter.format(Math.abs(report.netBalance))} $currencySymbol ($balanceSide)",
                    fontSize = 11.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            } else if (report.totalIn > 0 || report.totalOut > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (report.totalIn > 0) {
                        Text(
                            text = "وارد (+): ${formatter.format(report.totalIn)} $currencySymbol",
                            fontSize = 11.sp,
                            color = MoneyIncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (report.totalOut > 0) {
                        Text(
                            text = "منصرف (-): ${formatter.format(report.totalOut)} $currencySymbol",
                            fontSize = 11.sp,
                            color = MoneyExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Export Buttons (PDF & Excel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val partyMap = parties.associateBy { it.id }
                        val boxMap = cashBoxes.associateBy { it.id }
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            if (matchedParty != null) {
                                PdfExportHelper.printPartyStatement(
                                    activity = activity,
                                    party = matchedParty,
                                    transactions = filteredTransactions,
                                    cashBoxes = boxMap,
                                    currencySymbol = currencySymbol
                                )
                            } else {
                                PdfExportHelper.printTransactionsReport(
                                    activity = activity,
                                    transactions = filteredTransactions,
                                    parties = partyMap,
                                    cashBoxes = boxMap,
                                    title = report.reportTitle,
                                    currencySymbol = currencySymbol
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        val partyMap = parties.associateBy { it.id }
                        val boxMap = cashBoxes.associateBy { it.id }

                        val file = if (matchedParty != null) {
                            ExcelExportHelper.exportAccountStatementToExcel(
                                context = context,
                                party = matchedParty,
                                transactions = filteredTransactions,
                                cashBoxes = boxMap,
                                currencySymbol = currencySymbol
                            )
                        } else {
                            val calcTotalIn = if (report.totalIn > 0) report.totalIn else filteredTransactions.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }.sumOf { it.amount }
                            val calcTotalOut = if (report.totalOut > 0) report.totalOut else filteredTransactions.filter { it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                            val calcNet = if (report.netBalance != 0.0) report.netBalance else (calcTotalIn - calcTotalOut)

                            ExcelExportHelper.exportTransactionsToExcel(
                                context = context,
                                reportTitle = report.reportTitle,
                                transactions = filteredTransactions,
                                parties = partyMap,
                                cashBoxes = boxMap,
                                totalInflow = calcTotalIn,
                                totalOutflow = calcTotalOut,
                                netBalance = calcNet,
                                currencySymbol = currencySymbol
                            )
                        }
                        if (file != null) {
                            ExcelExportHelper.shareFile(context, file)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", fontSize = 11.sp)
                }
            }
        }
    }
}
