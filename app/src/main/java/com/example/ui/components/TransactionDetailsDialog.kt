package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.export.PdfExportHelper
import com.example.ui.theme.MoneyDebtBlue
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailsDialog(
    transaction: TransactionRecord,
    party: Party?,
    cashBox: CashBox?,
    currencySymbol: String = "ر.س",
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currencyFormat = remember { DecimalFormat("#,##0.##") }
    val dateFormat = remember { SimpleDateFormat("EEEE، dd MMMM yyyy - hh:mm a", Locale("ar")) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val (voucherName, voucherColor) = when (transaction.type) {
        TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> "سند قبض مالي" to MoneyIncomeGreen
        TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> "سند صرف مالي" to MoneyExpenseRed
        TransactionType.TRANSFER -> "إشعار تحويل نقدية" to Color(0xFF00897B)
        TransactionType.CUSTOMER_NEW_DEBIT -> "سند قيد مبيعات (ذمة مدينة)" to MoneyDebtBlue
        TransactionType.SUPPLIER_NEW_CREDIT -> "سند قيد مشتريات (ذمة دائنة)" to Color(0xFFC2185B)
        TransactionType.SETTLEMENT -> "سند تسوية حساب" to Color(0xFF7B1FA2)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("تأكيد حذف العملية", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا السند نهائياً؟ سيتم تحديث أرصدة الحسابات والصناديق تلقائياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = voucherName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = voucherColor
                    )
                    Text(
                        text = "سند رقم #${transaction.id}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Amount Banner
                Surface(
                    color = voucherColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "المبلغ المالي",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currencyFormat.format(transaction.amount)} $currencySymbol",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = voucherColor
                        )
                        Text(
                            text = transaction.type.titleAr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = voucherColor
                        )
                    }
                }

                // Details List Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date & Time
                        VoucherDetailRow(label = "التاريخ والوقت:", value = dateFormat.format(Date(transaction.timestamp)))

                        // Party
                        if (party != null) {
                            val role = if (party.type == PartyType.CUSTOMER) "عميل" else "مورد"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("الطرف المعني:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("${party.name} ($role)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                if (party.phone.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}"))
                                                    context.startActivity(dialIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "تعذر فتح الهاتف", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                shareVoucherViaWhatsApp(context, transaction, party, cashBox, currencySymbol)
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // CashBox
                        VoucherDetailRow(label = "الصندوق / الخزينة:", value = cashBox?.name ?: "الخزينة الرئيسية")

                        // Category
                        if (transaction.category.isNotBlank()) {
                            VoucherDetailRow(label = "التصنيف:", value = transaction.category)
                        }

                        // Description
                        VoucherDetailRow(
                            label = "البيان والشرح:",
                            value = transaction.description.ifBlank { transaction.type.titleAr }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Print PDF Button
                Button(
                    onClick = {
                        if (activity != null) {
                            PdfExportHelper.printTransactionVoucher(
                                activity = activity,
                                transaction = transaction,
                                party = party,
                                cashBox = cashBox,
                                currencySymbol = currencySymbol
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة PDF", fontSize = 12.sp)
                }

                // Share via WhatsApp Button
                OutlinedButton(
                    onClick = {
                        shareVoucherViaWhatsApp(context, transaction, party, cashBox, currencySymbol)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontSize = 12.sp)
                }

                // Delete Button
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف السند",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun VoucherDetailRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun shareVoucherViaWhatsApp(
    context: Context,
    transaction: TransactionRecord,
    party: Party?,
    cashBox: CashBox?,
    currencySymbol: String
) {
    val amountFormatted = DecimalFormat("#,##0.##").format(transaction.amount)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))

    val voucherTitle = when (transaction.type) {
        TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> "سند قبض مالي رسمي"
        TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> "سند صرف مالي رسمي"
        TransactionType.TRANSFER -> "إشعار تحويل نقدية"
        else -> "إشعار قيد مالي"
    }

    val message = """
        🧾 *$voucherTitle - محاسبي الذكي*
        ━━━━━━━━━━━━━━━━━━━━
        📌 *رقم السند:* #${transaction.id}
        📅 *التاريخ:* $dateFormat
        💰 *المبلغ:* $amountFormatted $currencySymbol
        👤 *الجهة / الطرف:* ${party?.name ?: "نقدي عام"}
        🏦 *الصندوق / الحساب:* ${cashBox?.name ?: "الخزينة الرئيسية"}
        📝 *البيان والتفاصيل:* ${transaction.description.ifBlank { transaction.type.titleAr }}
        ━━━━━━━━━━━━━━━━━━━━
        ✅ *تم التوثيق والاعتماد في النظام المحاسبي.*
    """.trimIndent()

    try {
        if (party != null && party.phone.isNotBlank()) {
            val cleanPhone = party.phone.replace(Regex("[^0-9+]"), "")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } else {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة السند المالي عبر:"))
        }
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة السند المالي عبر:"))
    }
}
