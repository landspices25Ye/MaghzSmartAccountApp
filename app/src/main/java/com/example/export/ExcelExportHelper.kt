package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val currencyFormat = DecimalFormat("#,##0.##")

    fun exportAccountStatementToExcel(
        context: Context,
        party: Party,
        transactions: List<TransactionRecord>,
        cashBoxes: Map<Long, CashBox>,
        currencySymbol: String = "ر.س"
    ): File? {
        try {
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val safeName = party.name.replace(Regex("[^a-zA-Z0-9\u0600-\u06FF_]"), "_")
            val fileName = "كشف_حساب_${safeName}_${fileDateFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    val isCustomer = party.type == PartyType.CUSTOMER
                    val partyTypeLabel = if (isCustomer) "عميل (مدين)" else "مورد (دائن)"
                    val balanceLabel = if (isCustomer) {
                        if (party.balance >= 0) "ما لنا عنده (دين عليه): ${currencyFormat.format(party.balance)} $currencySymbol" else "له علينا: ${currencyFormat.format(-party.balance)} $currencySymbol"
                    } else {
                        if (party.balance >= 0) "ما له علينا (دين له): ${currencyFormat.format(party.balance)} $currencySymbol" else "لنا عنده: ${currencyFormat.format(-party.balance)} $currencySymbol"
                    }

                    writer.write("\"كشف حساب تفصيلي: \",\"${party.name}\"\n")
                    writer.write("\"نوع الطرف: \",\"$partyTypeLabel\"\n")
                    writer.write("\"رقم الهاتف: \",\"${party.phone}\"\n")
                    writer.write("\"الرصيد الصافي النهائي: \",\"$balanceLabel\"\n")
                    writer.write("\"تاريخ الاستخراج: \",\"${dateFormat.format(Date())}\"\n\n")

                    writer.write("\"م\",\"التاريخ والوقت\",\"نوع العملية\",\"البيان والتفاصيل\",\"الصندوق/الخزينة\",\"مدين (عليه)\",\"دائن (له)\",\"الرصيد بعد الحركة\"\n")

                    val sortedTx = transactions.sortedBy { it.timestamp }
                    var totalDebit = 0.0
                    var totalCredit = 0.0
                    var runningBalance = 0.0

                    sortedTx.forEachIndexed { index, tx ->
                        val dateStr = dateFormat.format(Date(tx.timestamp))
                        val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
                        val desc = tx.description.replace("\"", "\"\"")

                        var debit = 0.0
                        var credit = 0.0

                        if (isCustomer) {
                            when (tx.type) {
                                TransactionType.CUSTOMER_NEW_DEBIT, TransactionType.INCOME -> {
                                    debit = tx.amount
                                    runningBalance += tx.amount
                                }
                                TransactionType.CUSTOMER_RECEIPT -> {
                                    credit = tx.amount
                                    runningBalance -= tx.amount
                                }
                                TransactionType.SETTLEMENT -> {
                                    if (tx.amount >= 0) {
                                        debit = tx.amount
                                        runningBalance += tx.amount
                                    } else {
                                        credit = -tx.amount
                                        runningBalance -= -tx.amount
                                    }
                                }
                                else -> {
                                    debit = tx.amount
                                    runningBalance += tx.amount
                                }
                            }
                        } else {
                            when (tx.type) {
                                TransactionType.SUPPLIER_NEW_CREDIT, TransactionType.EXPENSE -> {
                                    credit = tx.amount
                                    runningBalance += tx.amount
                                }
                                TransactionType.SUPPLIER_PAYMENT -> {
                                    debit = tx.amount
                                    runningBalance -= tx.amount
                                }
                                TransactionType.SETTLEMENT -> {
                                    if (tx.amount >= 0) {
                                        credit = tx.amount
                                        runningBalance += tx.amount
                                    } else {
                                        debit = -tx.amount
                                        runningBalance -= -tx.amount
                                    }
                                }
                                else -> {
                                    credit = tx.amount
                                    runningBalance += tx.amount
                                }
                            }
                        }

                        totalDebit += debit
                        totalCredit += credit

                        val balanceSide = if (isCustomer) {
                            if (runningBalance >= 0) "عليه" else "له"
                        } else {
                            if (runningBalance >= 0) "له" else "عليه"
                        }

                        val debitCell = if (debit > 0) debit.toString() else "0"
                        val creditCell = if (credit > 0) credit.toString() else "0"
                        val balCell = "${Math.abs(runningBalance)} ($balanceSide)"

                        writer.write("\"${index + 1}\",\"$dateStr\",\"${tx.type.titleAr}\",\"$desc\",\"$boxName\",\"$debitCell\",\"$creditCell\",\"$balCell\"\n")
                    }

                    writer.write("\n\"المجموع الكلي\",\"\",\"\",\"\",\"\",\"$totalDebit\",\"$totalCredit\",\"${party.balance}\"\n")
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
        netBalance: Double,
        currencySymbol: String = "ر.س"
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
                    writer.write("\"تطبيق محاسبي الذكي - تقرير مالي: $reportTitle\"\n")
                    writer.write("\"الفترة الزمنية:\",\"$dateRangeText\"\n")
                    writer.write("\"الحسابات المحددة:\",\"$accountsText\"\n")
                    writer.write("\"العمليات المشمولة:\",\"$typesText\"\n")
                    writer.write("\"تاريخ الاستخراج:\",\"${dateFormat.format(Date())}\"\n\n")

                    writer.write("\"ملخص التقرير المالي:\"\n")
                    writer.write("\"إجمالي مدين (وارد / عليه):\",\"$totalInflow $currencySymbol\"\n")
                    writer.write("\"إجمالي دائن (منصرف / له):\",\"$totalOutflow $currencySymbol\"\n")
                    writer.write("\"صافي الرصيد:\",\"$netBalance $currencySymbol\"\n")
                    writer.write("\"عدد العمليات:\",\"${transactions.size}\"\n\n")

                    writer.write("\"م\",\"التاريخ والوقت\",\"نوع العملية\",\"الطرف (عميل/مورد)\",\"الصندوق/الخزينة\",\"البيان والتفاصيل\",\"مدين (عليه)\",\"دائن (له)\",\"الرصيد المتراكم\"\n")

                    val sortedTx = transactions.sortedBy { it.timestamp }
                    var runningCum = 0.0

                    sortedTx.forEachIndexed { index, tx ->
                        val dateStr = dateFormat.format(Date(tx.timestamp))
                        val partyName = tx.partyId?.let { parties[it]?.name } ?: "-"
                        val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
                        val desc = tx.description.replace("\"", "\"\"")

                        val isDebit = tx.type in listOf(TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME, TransactionType.CUSTOMER_NEW_DEBIT)
                        val debit = if (isDebit) tx.amount else 0.0
                        val credit = if (!isDebit) tx.amount else 0.0
                        runningCum += (debit - credit)

                        writer.write("\"${index + 1}\",\"$dateStr\",\"${tx.type.titleAr}\",\"$partyName\",\"$boxName\",\"$desc\",\"$debit\",\"$credit\",\"$runningCum\"\n")
                    }

                    writer.write("\n\"المجموع الكلي\",\"\",\"\",\"\",\"\",\"\",\"$totalInflow\",\"$totalOutflow\",\"$netBalance\"\n")
                    writer.flush()
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportTransactionsToExcel(
        context: Context,
        reportTitle: String,
        transactions: List<TransactionRecord>,
        parties: Map<Long, Party>,
        cashBoxes: Map<Long, CashBox>,
        totalInflow: Double = 0.0,
        totalOutflow: Double = 0.0,
        netBalance: Double = 0.0,
        currencySymbol: String = "ر.س"
    ): File? {
        return exportCustomReportToExcel(
            context = context,
            reportTitle = reportTitle,
            dateRangeText = "تقرير فوري من المحاسب الذكي",
            accountsText = "كافة الحسابات المشمولة",
            typesText = "جميع العمليات",
            transactions = transactions,
            parties = parties,
            cashBoxes = cashBoxes,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netBalance = netBalance,
            currencySymbol = currencySymbol
        )
    }

    fun exportTrialBalanceToExcel(
        context: Context,
        parties: List<Party>,
        transactions: List<TransactionRecord>,
        currencySymbol: String = "ر.س"
    ): File? {
        try {
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "ميزان_أرصدة_الأطراف_${fileDateFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            val txByParty = transactions.groupBy { it.partyId }

            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\"تطبيق محاسبي الذكي - ميزان أرصدة الحسابات والأطراف (مدين / دائن / رصيد)\"\n")
                    writer.write("\"تاريخ الاستخراج:\",\"${dateFormat.format(Date())}\"\n")
                    writer.write("\"عدد الأطراف المسجلة:\",\"${parties.size}\"\n\n")

                    var totalCustomerDebit = 0.0
                    var totalSupplierCredit = 0.0

                    parties.forEach { party ->
                        if (party.type == PartyType.CUSTOMER && party.balance > 0) totalCustomerDebit += party.balance
                        if (party.type == PartyType.SUPPLIER && party.balance > 0) totalSupplierCredit += party.balance
                    }

                    writer.write("\"ملخص أرصدة السوق:\"\n")
                    writer.write("\"إجمالي ديون العملاء (لنا عندهم):\",\"${currencyFormat.format(totalCustomerDebit)} $currencySymbol\"\n")
                    writer.write("\"إجمالي التزامات الموردين (علينا لهم):\",\"${currencyFormat.format(totalSupplierCredit)} $currencySymbol\"\n")
                    writer.write("\"صافي مركز الديون:\",\"${currencyFormat.format(totalCustomerDebit - totalSupplierCredit)} $currencySymbol\"\n\n")

                    writer.write("\"م\",\"اسم الطرف\",\"نوع الحساب\",\"رقم الهاتف\",\"إجمالي مدين (عليه)\",\"إجمالي دائن (له)\",\"الرصيد الصافي\",\"حالة الرصيد\"\n")

                    parties.sortedWith(compareBy({ it.type }, { -Math.abs(it.balance) })).forEachIndexed { index, party ->
                        val isCustomer = party.type == PartyType.CUSTOMER
                        val typeLabel = if (isCustomer) "عميل (مدين)" else "مورد (دائن)"
                        val partyTx = txByParty[party.id] ?: emptyList()

                        var partyDebit = 0.0
                        var partyCredit = 0.0

                        partyTx.forEach { tx ->
                            if (isCustomer) {
                                when (tx.type) {
                                    TransactionType.CUSTOMER_NEW_DEBIT, TransactionType.INCOME -> partyDebit += tx.amount
                                    TransactionType.CUSTOMER_RECEIPT -> partyCredit += tx.amount
                                    TransactionType.SETTLEMENT -> if (tx.amount >= 0) partyDebit += tx.amount else partyCredit += -tx.amount
                                    else -> partyDebit += tx.amount
                                }
                            } else {
                                when (tx.type) {
                                    TransactionType.SUPPLIER_NEW_CREDIT, TransactionType.EXPENSE -> partyCredit += tx.amount
                                    TransactionType.SUPPLIER_PAYMENT -> partyDebit += tx.amount
                                    TransactionType.SETTLEMENT -> if (tx.amount >= 0) partyCredit += tx.amount else partyDebit += -tx.amount
                                    else -> partyCredit += tx.amount
                                }
                            }
                        }

                        val statusLabel = when {
                            party.balance > 0 -> if (isCustomer) "عليه (لنا عنده)" else "له (علينا له)"
                            party.balance < 0 -> if (isCustomer) "له (دائن لنا)" else "عليه (لنا عنده)"
                            else -> "خالص (0.00)"
                        }

                        val cleanName = party.name.replace("\"", "\"\"")
                        writer.write("\"${index + 1}\",\"$cleanName\",\"$typeLabel\",\"${party.phone}\",\"${partyDebit}\",\"${partyCredit}\",\"${Math.abs(party.balance)}\",\"$statusLabel\"\n")
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
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "مشاركة التقرير المالي عبر:"))
    }
}
