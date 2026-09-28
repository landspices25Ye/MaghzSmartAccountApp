package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportTransactionsToExcel(
        context: Context,
        transactions: List<TransactionRecord>,
        parties: Map<Long, Party>,
        cashBoxes: Map<Long, CashBox>,
        title: String = "تقرير_العمليات_المالية"
    ): File? {
        try {
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "${title}_${fileDateFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                // Write UTF-8 BOM so Microsoft Excel recognizes Arabic correctly
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Title row
                    writer.write("\"تطبيق محاسبي الذكي - $title\"\n")
                    writer.write("\"تاريخ الاستخراج: \",\"${dateFormat.format(Date())}\"\n\n")

                    // Table headers
                    writer.write("\"م\",\"التاريخ والوقت\",\"نوع العملية\",\"الطرف (عميل/مورد)\",\"الصندوق/الخزينة\",\"المبلغ (ريال)\",\"البيان والتفاصيل\"\n")

                    transactions.forEachIndexed { index, tx ->
                        val dateStr = dateFormat.format(Date(tx.timestamp))
                        val partyName = tx.partyId?.let { parties[it]?.name } ?: "-"
                        val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
                        val amount = tx.amount
                        val desc = tx.description.replace("\"", "\"\"")

                        writer.write("\"${index + 1}\",\"$dateStr\",\"${tx.type.titleAr}\",\"$partyName\",\"$boxName\",\"$amount\",\"$desc\"\n")
                    }

                    writer.flush()
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportCustomReportToExcel(
        context: Context,
        reportTitle: String,
        dateRangeText: String,
        accountsText: String,
        typesText: String,
        transactions: List<TransactionRecord>,
        parties: Map<Long, Party>,
        cashBoxes: Map<Long, CashBox>,
        totalInflow: Double,
        totalOutflow: Double,
        netBalance: Double
    ): File? {
        try {
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val safeTitle = reportTitle.replace(Regex("[^a-zA-Z0-9\u0600-\u06FF_]"), "_")
            val fileName = "${safeTitle}_${fileDateFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\"تطبيق محاسبي الذكي - تقرير مالي مخصص: $reportTitle\"\n")
                    writer.write("\"الفترة الزمنية:\",\"$dateRangeText\"\n")
                    writer.write("\"الحسابات المحددة:\",\"$accountsText\"\n")
                    writer.write("\"العمليات المشمولة:\",\"$typesText\"\n")
                    writer.write("\"تاريخ الاستخراج:\",\"${dateFormat.format(Date())}\"\n\n")

                    writer.write("\"ملخص التقرير المالي:\"\n")
                    writer.write("\"إجمالي المقبوضات/الدخل:\",\"$totalInflow ر.س\"\n")
                    writer.write("\"إجمالي المدفوعات/المصروف:\",\"$totalOutflow ر.س\"\n")
                    writer.write("\"صافي الحركة النقدية:\",\"$netBalance ر.س\"\n")
                    writer.write("\"عدد الحركات:\",\"${transactions.size}\"\n\n")

                    writer.write("\"م\",\"التاريخ والوقت\",\"نوع العملية\",\"الطرف (عميل/مورد)\",\"الصندوق/الخزينة\",\"المبلغ (ريال)\",\"البيان والتفاصيل\"\n")

                    transactions.forEachIndexed { index, tx ->
                        val dateStr = dateFormat.format(Date(tx.timestamp))
                        val partyName = tx.partyId?.let { parties[it]?.name } ?: "-"
                        val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
                        val desc = tx.description.replace("\"", "\"\"")
                        writer.write("\"${index + 1}\",\"$dateStr\",\"${tx.type.titleAr}\",\"$partyName\",\"$boxName\",\"${tx.amount}\",\"$desc\"\n")
                    }

                    writer.flush()
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportAccountStatementToExcel(
        context: Context,
        party: Party,
        transactions: List<TransactionRecord>,
        cashBoxes: Map<Long, CashBox>
    ): File? {
        try {
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val safeName = party.name.replace(" ", "_")
            val fileName = "كشف_حساب_${safeName}_${fileDateFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    val partyTypeLabel = if (party.type == PartyType.CUSTOMER) "عميل (مدين)" else "مورد (دائن)"
                    val balanceLabel = if (party.type == PartyType.CUSTOMER) {
                        if (party.balance >= 0) "ما لنا عنده: ${party.balance} ريال" else "له علينا: ${-party.balance} ريال"
                    } else {
                        if (party.balance >= 0) "ما له علينا: ${party.balance} ريال" else "لنا عنده: ${-party.balance} ريال"
                    }

                    writer.write("\"كشف حساب: \",\"${party.name}\"\n")
                    writer.write("\"النوع: \",\"$partyTypeLabel\"\n")
                    writer.write("\"الهاتف: \",\"${party.phone}\"\n")
                    writer.write("\"الرصيد الحالي: \",\"$balanceLabel\"\n")
                    writer.write("\"تاريخ التقرير: \",\"${dateFormat.format(Date())}\"\n\n")

                    writer.write("\"م\",\"التاريخ والوقت\",\"العملية\",\"الصندوق\",\"المبلغ\",\"البيان\"\n")

                    transactions.forEachIndexed { index, tx ->
                        val dateStr = dateFormat.format(Date(tx.timestamp))
                        val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
                        val desc = tx.description.replace("\"", "\"\"")
                        writer.write("\"${index + 1}\",\"$dateStr\",\"${tx.type.titleAr}\",\"$boxName\",\"${tx.amount}\",\"$desc\"\n")
                    }

                    writer.flush()
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "text/csv") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "مشاركة أو فتح الملف عبر Excel"))
    }
}
