package com.example.export

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val currencyFormat = DecimalFormat("#,##0.##")

    fun printTransactionsReport(
        activity: Activity,
        transactions: List<TransactionRecord>,
        parties: Map<Long, Party>,
        cashBoxes: Map<Long, CashBox>,
        title: String = "كشف العمليات المالية",
        currencySymbol: String = "ر.س"
    ) {
        val html = buildTransactionsHtml(transactions, parties, cashBoxes, title, currencySymbol)
        printHtml(activity, html, title)
    }

    fun printCustomReport(
        activity: Activity,
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
    ) {
        val html = buildCustomReportHtml(
            reportTitle,
            dateRangeText,
            accountsText,
            typesText,
            transactions,
            parties,
            cashBoxes,
            totalInflow,
            totalOutflow,
            netBalance,
            currencySymbol
        )
        printHtml(activity, html, reportTitle)
    }

    fun printPartyStatement(
        activity: Activity,
        party: Party,
        transactions: List<TransactionRecord>,
        cashBoxes: Map<Long, CashBox>,
        currencySymbol: String = "ر.س"
    ) {
        val html = buildPartyHtml(party, transactions, cashBoxes, currencySymbol)
        printHtml(activity, html, "كشف_حساب_${party.name}")
    }

    fun printTrialBalanceReport(
        activity: Activity,
        parties: List<Party>,
        transactions: List<TransactionRecord>,
        currencySymbol: String = "ر.س"
    ) {
        val html = buildTrialBalanceHtml(parties, transactions, currencySymbol)
        printHtml(activity, html, "ميزان_أرصدة_الأطراف")
    }

    fun printTransactionVoucher(
        activity: Activity,
        transaction: TransactionRecord,
        party: Party?,
        cashBox: CashBox?,
        currencySymbol: String = "ر.س"
    ) {
        val html = buildTransactionVoucherHtml(transaction, party, cashBox, currencySymbol)
        val title = when (transaction.type) {
            TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> "سند_قبض_${transaction.id}"
            TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> "سند_صرف_${transaction.id}"
            TransactionType.TRANSFER -> "إشعار_تحويل_${transaction.id}"
            else -> "سند_قيد_${transaction.id}"
        }
        printHtml(activity, html, title)
    }

    private fun printHtml(activity: Activity, html: String, jobName: String) {
        val webView = WebView(activity)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager?.print(jobName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "UTF-8", null)
    }

    private fun buildCustomReportHtml(
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
    ): String {
        val rows = StringBuilder()
        var runningCumulative = 0.0
        val sortedTx = transactions.sortedBy { it.timestamp }

        var totalDebit = 0.0
        var totalCredit = 0.0

        sortedTx.forEachIndexed { i, tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val partyName = tx.partyId?.let { parties[it]?.name } ?: "-"
            val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"

            val isDebit = tx.type in listOf(TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME, TransactionType.CUSTOMER_NEW_DEBIT)
            val debit = if (isDebit) tx.amount else 0.0
            val credit = if (!isDebit) tx.amount else 0.0

            totalDebit += debit
            totalCredit += credit
            runningCumulative += (debit - credit)

            val debitStr = if (debit > 0) "${currencyFormat.format(debit)} $currencySymbol" else "-"
            val creditStr = if (credit > 0) "${currencyFormat.format(credit)} $currencySymbol" else "-"
            val cumStr = "${currencyFormat.format(runningCumulative)} $currencySymbol"

            rows.append("""
                <tr>
                    <td>${i + 1}</td>
                    <td>$dateStr</td>
                    <td><span class="badge">${tx.type.titleAr}</span></td>
                    <td>$partyName</td>
                    <td>$boxName</td>
                    <td>${tx.description}</td>
                    <td class="debit">$debitStr</td>
                    <td class="credit">$creditStr</td>
                    <td class="balance">$cumStr</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; direction: rtl; color: #222; }
                    .header { border-bottom: 2px solid #0D6B42; padding-bottom: 12px; margin-bottom: 16px; }
                    .title { font-size: 22px; font-weight: bold; color: #0D6B42; margin-bottom: 4px; }
                    .subtitle { font-size: 12px; color: #555; }
                    .filter-pill { display: inline-block; background: #e8f5e9; color: #1b5e20; border-radius: 4px; padding: 4px 8px; font-size: 11px; margin-left: 6px; }
                    .metrics-container { display: flex; gap: 10px; margin: 16px 0; }
                    .metric-box { flex: 1; padding: 10px; border-radius: 8px; background: #f7faf7; border: 1px solid #dcdfdc; text-align: center; }
                    .metric-title { font-size: 11px; color: #666; margin-bottom: 4px; }
                    .metric-value { font-size: 16px; font-weight: bold; }
                    .debit { color: #1B8755; font-weight: bold; }
                    .credit { color: #D32F2F; font-weight: bold; }
                    .balance { color: #0D6B42; font-weight: bold; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 8px 6px; text-align: right; font-size: 11px; }
                    th { background-color: #0D6B42; color: white; }
                    tr:nth-child(even) { background-color: #f9fbf9; }
                    tfoot tr { background-color: #e8f5e9; font-weight: bold; }
                    .badge { background: #e8f5e9; color: #1b5e20; padding: 2px 6px; border-radius: 4px; font-size: 10px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">محاسبي الذكي - $reportTitle</div>
                    <div class="subtitle">تاريخ الاستخراج: ${dateFormat.format(Date())}</div>
                    <div style="margin-top: 8px;">
                        <span class="filter-pill">الفترة: $dateRangeText</span>
                        <span class="filter-pill">الحسابات: $accountsText</span>
                        <span class="filter-pill">العمليات: $typesText</span>
                    </div>
                </div>

                <div class="metrics-container">
                    <div class="metric-box">
                        <div class="metric-title">إجمالي مدين (وارد / عليه)</div>
                        <div class="metric-value debit">+${currencyFormat.format(totalDebit)} $currencySymbol</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">إجمالي دائن (منصرف / له)</div>
                        <div class="metric-value credit">-${currencyFormat.format(totalCredit)} $currencySymbol</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">صافي الرصيد</div>
                        <div class="metric-value balance">${currencyFormat.format(netBalance)} $currencySymbol</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">عدد العمليات</div>
                        <div class="metric-value">${transactions.size}</div>
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>التاريخ والوقت</th>
                            <th>نوع الحركة</th>
                            <th>الطرف</th>
                            <th>الصندوق</th>
                            <th>البيان والتفاصيل</th>
                            <th>مدين (عليه)</th>
                            <th>دائن (له)</th>
                            <th>الرصيد المتراكم</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                    <tfoot>
                        <tr>
                            <td colspan="6" style="text-align: center;">المجموع الكلي</td>
                            <td class="debit">${currencyFormat.format(totalDebit)} $currencySymbol</td>
                            <td class="credit">${currencyFormat.format(totalCredit)} $currencySymbol</td>
                            <td class="balance">${currencyFormat.format(netBalance)} $currencySymbol</td>
                        </tr>
                    </tfoot>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildTransactionsHtml(
        transactions: List<TransactionRecord>,
        parties: Map<Long, Party>,
        cashBoxes: Map<Long, CashBox>,
        title: String,
        currencySymbol: String = "ر.س"
    ): String {
        return buildCustomReportHtml(
            reportTitle = title,
            dateRangeText = "كافة الفترات",
            accountsText = "كافة الحسابات",
            typesText = "كافة العمليات",
            transactions = transactions,
            parties = parties,
            cashBoxes = cashBoxes,
            totalInflow = transactions.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }.sumOf { it.amount },
            totalOutflow = transactions.filter { it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            netBalance = transactions.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }.sumOf { it.amount } - transactions.filter { it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            currencySymbol = currencySymbol
        )
    }

    private fun buildPartyHtml(
        party: Party,
        transactions: List<TransactionRecord>,
        cashBoxes: Map<Long, CashBox>,
        currencySymbol: String = "ر.س"
    ): String {
        val rows = StringBuilder()
        val isCustomer = party.type == PartyType.CUSTOMER
        val sortedTx = transactions.sortedBy { it.timestamp }

        var totalDebit = 0.0
        var totalCredit = 0.0
        var runningBalance = 0.0

        sortedTx.forEachIndexed { i, tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"

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

            val debitStr = if (debit > 0) "${currencyFormat.format(debit)} $currencySymbol" else "-"
            val creditStr = if (credit > 0) "${currencyFormat.format(credit)} $currencySymbol" else "-"
            val balStr = "${currencyFormat.format(Math.abs(runningBalance))} $currencySymbol ($balanceSide)"

            rows.append("""
                <tr>
                    <td>${i + 1}</td>
                    <td>$dateStr</td>
                    <td>${tx.description.ifBlank { tx.type.titleAr }}</td>
                    <td>$boxName</td>
                    <td class="debit">$debitStr</td>
                    <td class="credit">$creditStr</td>
                    <td class="balance">$balStr</td>
                </tr>
            """.trimIndent())
        }

        val partyTypeLabel = if (isCustomer) "عميل (مدين)" else "مورد (دائن)"
        val balanceDesc = if (isCustomer) {
            if (party.balance >= 0) "ما لنا عنده (دين عليه): ${currencyFormat.format(party.balance)} $currencySymbol"
            else "ما له علينا: ${currencyFormat.format(-party.balance)} $currencySymbol"
        } else {
            if (party.balance >= 0) "ما له علينا (دين له): ${currencyFormat.format(party.balance)} $currencySymbol"
            else "ما لنا عنده: ${currencyFormat.format(-party.balance)} $currencySymbol"
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; direction: rtl; color: #222; }
                    .header { border-bottom: 2px solid #0D6B42; padding-bottom: 12px; margin-bottom: 16px; }
                    .title { font-size: 24px; font-weight: bold; color: #0D6B42; }
                    .info-box { background: #f0f7f3; border: 1px solid #cce5d6; padding: 12px; border-radius: 8px; margin: 15px 0; display: flex; justify-content: space-between; align-items: center; }
                    .metrics-container { display: flex; gap: 10px; margin: 14px 0; }
                    .metric-box { flex: 1; padding: 10px; border-radius: 8px; background: #f7faf7; border: 1px solid #dcdfdc; text-align: center; }
                    .metric-title { font-size: 11px; color: #666; margin-bottom: 4px; }
                    .metric-value { font-size: 16px; font-weight: bold; }
                    .debit { color: #1B8755; font-weight: bold; }
                    .credit { color: #D32F2F; font-weight: bold; }
                    .balance { color: #0D6B42; font-weight: bold; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 8px 6px; text-align: right; font-size: 12px; }
                    th { background-color: #0D6B42; color: white; }
                    tr:nth-child(even) { background-color: #f9fbf9; }
                    tfoot tr { background-color: #e8f5e9; font-weight: bold; }
                    .balance-tag { font-size: 16px; font-weight: bold; color: #0D6B42; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">كشف حساب: ${party.name}</div>
                    <div style="font-size: 13px; color: #555; margin-top: 4px;">نوع الطرف: $partyTypeLabel | الهاتف: ${party.phone.ifBlank { "غير مسجل" }}</div>
                </div>

                <div class="info-box">
                    <div>
                        <div class="balance-tag">الرصيد النهائي: $balanceDesc</div>
                        <div style="font-size: 11px; color: #666; margin-top: 2px;">تاريخ الاستخراج: ${dateFormat.format(Date())}</div>
                    </div>
                    <div>
                        <span style="background: #0D6B42; color: white; padding: 4px 10px; border-radius: 6px; font-size: 12px;">عدد العمليات: ${transactions.size}</span>
                    </div>
                </div>

                <div class="metrics-container">
                    <div class="metric-box">
                        <div class="metric-title">إجمالي مدين (عليه)</div>
                        <div class="metric-value debit">${currencyFormat.format(totalDebit)} $currencySymbol</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">إجمالي دائن (له)</div>
                        <div class="metric-value credit">${currencyFormat.format(totalCredit)} $currencySymbol</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">الرصيد الصافي</div>
                        <div class="metric-value balance">${currencyFormat.format(Math.abs(party.balance))} $currencySymbol</div>
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>التاريخ والوقت</th>
                            <th>البيان والتفاصيل</th>
                            <th>الصندوق</th>
                            <th>مدين (عليه)</th>
                            <th>دائن (له)</th>
                            <th>الرصيد</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                    <tfoot>
                        <tr>
                            <td colspan="4" style="text-align: center;">المجموع الكلي</td>
                            <td class="debit">${currencyFormat.format(totalDebit)} $currencySymbol</td>
                            <td class="credit">${currencyFormat.format(totalCredit)} $currencySymbol</td>
                            <td class="balance">${currencyFormat.format(Math.abs(party.balance))} $currencySymbol</td>
                        </tr>
                    </tfoot>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildTrialBalanceHtml(
        parties: List<Party>,
        transactions: List<TransactionRecord>,
        currencySymbol: String
    ): String {
        val txByParty = transactions.groupBy { it.partyId }
        val rows = StringBuilder()

        var totalCustDebit = 0.0
        var totalSuppCredit = 0.0
        var grandTotalDebit = 0.0
        var grandTotalCredit = 0.0

        parties.sortedWith(compareBy({ it.type }, { -Math.abs(it.balance) })).forEachIndexed { index, party ->
            val isCustomer = party.type == PartyType.CUSTOMER
            val partyTypeLabel = if (isCustomer) "عميل (مدين)" else "مورد (دائن)"
            val partyTx = txByParty[party.id] ?: emptyList()

            var debit = 0.0
            var credit = 0.0

            partyTx.forEach { tx ->
                if (isCustomer) {
                    when (tx.type) {
                        TransactionType.CUSTOMER_NEW_DEBIT, TransactionType.INCOME -> debit += tx.amount
                        TransactionType.CUSTOMER_RECEIPT -> credit += tx.amount
                        TransactionType.SETTLEMENT -> if (tx.amount >= 0) debit += tx.amount else credit += -tx.amount
                        else -> debit += tx.amount
                    }
                } else {
                    when (tx.type) {
                        TransactionType.SUPPLIER_NEW_CREDIT, TransactionType.EXPENSE -> credit += tx.amount
                        TransactionType.SUPPLIER_PAYMENT -> debit += tx.amount
                        TransactionType.SETTLEMENT -> if (tx.amount >= 0) credit += tx.amount else debit += -tx.amount
                        else -> credit += tx.amount
                    }
                }
            }

            grandTotalDebit += debit
            grandTotalCredit += credit

            if (isCustomer && party.balance > 0) totalCustDebit += party.balance
            if (!isCustomer && party.balance > 0) totalSuppCredit += party.balance

            val statusText = when {
                party.balance > 0 -> if (isCustomer) "عليه (لنا عنده)" else "له (علينا له)"
                party.balance < 0 -> if (isCustomer) "له (دائن لنا)" else "عليه (لنا عنده)"
                else -> "خالص (0.00)"
            }

            val statusClass = if (party.balance > 0) {
                if (isCustomer) "debit" else "credit"
            } else "balance"

            rows.append("""
                <tr>
                    <td>${index + 1}</td>
                    <td><strong>${party.name}</strong></td>
                    <td>$partyTypeLabel</td>
                    <td>${party.phone.ifBlank { "-" }}</td>
                    <td class="debit">${currencyFormat.format(debit)} $currencySymbol</td>
                    <td class="credit">${currencyFormat.format(credit)} $currencySymbol</td>
                    <td class="balance">${currencyFormat.format(Math.abs(party.balance))} $currencySymbol</td>
                    <td class="$statusClass">$statusText</td>
                </tr>
            """.trimIndent())
        }

        val netMarketPosition = totalCustDebit - totalSuppCredit
        val netPositionText = if (netMarketPosition >= 0) "فائض مستحقات لنا بالسوق" else "عجز التزامات علينا للموردين"

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; direction: rtl; color: #222; }
                    .header { border-bottom: 2px solid #0D6B42; padding-bottom: 12px; margin-bottom: 16px; }
                    .title { font-size: 22px; font-weight: bold; color: #0D6B42; }
                    .metrics-container { display: flex; gap: 10px; margin: 14px 0; }
                    .metric-box { flex: 1; padding: 10px; border-radius: 8px; background: #f7faf7; border: 1px solid #dcdfdc; text-align: center; }
                    .metric-title { font-size: 11px; color: #666; margin-bottom: 4px; }
                    .metric-value { font-size: 16px; font-weight: bold; }
                    .debit { color: #1B8755; font-weight: bold; }
                    .credit { color: #D32F2F; font-weight: bold; }
                    .balance { color: #0D6B42; font-weight: bold; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 7px 6px; text-align: right; font-size: 11px; }
                    th { background-color: #0D6B42; color: white; }
                    tr:nth-child(even) { background-color: #f9fbf9; }
                    tfoot tr { background-color: #e8f5e9; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">ميزان أرصدة الحسابات والأطراف (مدين / دائن / رصيد)</div>
                    <div style="font-size: 12px; color: #555; margin-top: 4px;">تاريخ الاستخراج: ${dateFormat.format(Date())} | عدد الأطراف: ${parties.size} جهة</div>
                </div>

                <div class="metrics-container">
                    <div class="metric-box">
                        <div class="metric-title">إجمالي ديون العملاء (لنا عندهم)</div>
                        <div class="metric-value debit">${currencyFormat.format(totalCustDebit)} $currencySymbol</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">إجمالي التزامات الموردين (علينا لهم)</div>
                        <div class="metric-value credit">${currencyFormat.format(totalSuppCredit)} $currencySymbol</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">صافي مركز ديون السوق ($netPositionText)</div>
                        <div class="metric-value balance">${currencyFormat.format(Math.abs(netMarketPosition))} $currencySymbol</div>
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>اسم الطرف</th>
                            <th>النوع</th>
                            <th>الهاتف</th>
                            <th>إجمالي مدين (عليه)</th>
                            <th>إجمالي دائن (له)</th>
                            <th>الرصيد الصافي</th>
                            <th>حالة الرصيد</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                    <tfoot>
                        <tr>
                            <td colspan="4" style="text-align: center;">إجمالي حركة العمليات</td>
                            <td class="debit">${currencyFormat.format(grandTotalDebit)} $currencySymbol</td>
                            <td class="credit">${currencyFormat.format(grandTotalCredit)} $currencySymbol</td>
                            <td class="balance" colspan="2">${currencyFormat.format(Math.abs(netMarketPosition))} $currencySymbol</td>
                        </tr>
                    </tfoot>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildTransactionVoucherHtml(
        tx: TransactionRecord,
        party: Party?,
        cashBox: CashBox?,
        currencySymbol: String
    ): String {
        val (voucherTitle, titleColor, personLabel) = when (tx.type) {
            TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> Triple("سند قبض مالي", "#1B8755", "استلمنا من السيد / الجهة")
            TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> Triple("سند صرف مالي", "#D32F2F", "يصرف إلى السيد / الجهة")
            TransactionType.TRANSFER -> Triple("إشعار تحويل نقدية", "#00897B", "الطرف المستفيد / المحول إليه")
            TransactionType.CUSTOMER_NEW_DEBIT -> Triple("سند قيد مبيعات / ذمة مدينة", "#1976D2", "قيد على حساب العميل")
            TransactionType.SUPPLIER_NEW_CREDIT -> Triple("سند قيد مشتريات / ذمة دائنة", "#C2185B", "قيد لصالح المورد")
            TransactionType.SETTLEMENT -> Triple("سند تسوية حساب", "#7B1FA2", "تسوية حساب السيد")
        }

        val formattedDate = dateFormat.format(Date(tx.timestamp))
        val partyName = party?.name ?: "نقدي - بدون جهة محددة"
        val partyPhone = party?.phone?.ifBlank { "-" } ?: "-"
        val partyTypeStr = party?.type?.let { if (it == PartyType.CUSTOMER) "عميل" else "مورد" } ?: "-"
        val boxName = cashBox?.name ?: "الخزينة الرئيسية"
        val desc = tx.description.ifBlank { tx.type.titleAr }
        val categoryStr = tx.category.ifBlank { "عام" }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        margin: 25px;
                        direction: rtl;
                        color: #222;
                    }
                    .voucher-frame {
                        border: 3px double $titleColor;
                        border-radius: 14px;
                        padding: 24px;
                        background: #fdfdfd;
                    }
                    .header-table {
                        width: 100%;
                        border-bottom: 2px solid $titleColor;
                        padding-bottom: 12px;
                        margin-bottom: 20px;
                    }
                    .system-name {
                        font-size: 16px;
                        font-weight: bold;
                        color: #0D6B42;
                    }
                    .voucher-title {
                        font-size: 26px;
                        font-weight: 900;
                        color: $titleColor;
                        text-align: center;
                    }
                    .voucher-number {
                        font-size: 14px;
                        font-weight: bold;
                        color: #555;
                        text-align: left;
                    }
                    .amount-box {
                        background: #f5f8f5;
                        border: 2px dashed $titleColor;
                        border-radius: 10px;
                        padding: 14px 20px;
                        margin: 16px 0 24px 0;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .amount-label {
                        font-size: 15px;
                        font-weight: bold;
                        color: #444;
                    }
                    .amount-value {
                        font-size: 24px;
                        font-weight: 900;
                        color: $titleColor;
                    }
                    .details-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 30px;
                    }
                    .details-table td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #eee;
                        font-size: 14px;
                    }
                    .details-table .label-col {
                        width: 25%;
                        font-weight: bold;
                        color: #555;
                        background-color: #fafafa;
                    }
                    .signatures {
                        display: flex;
                        justify-content: space-between;
                        margin-top: 40px;
                        padding-top: 15px;
                    }
                    .sig-box {
                        text-align: center;
                        width: 28%;
                        border-top: 1px dashed #888;
                        padding-top: 8px;
                        font-size: 13px;
                        font-weight: bold;
                        color: #444;
                    }
                    .footer-note {
                        text-align: center;
                        font-size: 11px;
                        color: #888;
                        margin-top: 25px;
                    }
                </style>
            </head>
            <body>
                <div class="voucher-frame">
                    <table class="header-table">
                        <tr>
                            <td style="width: 30%;">
                                <div class="system-name">محاسبي الذكي</div>
                                <div style="font-size: 11px; color: #777;">النظام المحاسبي المتكامل</div>
                            </td>
                            <td style="width: 40%; text-align: center;">
                                <div class="voucher-title">$voucherTitle</div>
                            </td>
                            <td style="width: 30%; text-align: left;">
                                <div class="voucher-number">رقم السند: #${tx.id}</div>
                                <div style="font-size: 12px; color: #666; margin-top: 4px;">التاريخ: $formattedDate</div>
                            </td>
                        </tr>
                    </table>

                    <div class="amount-box">
                        <div class="amount-label">المبلغ المطلوب / المقبوض:</div>
                        <div class="amount-value">${currencyFormat.format(tx.amount)} $currencySymbol</div>
                    </div>

                    <table class="details-table">
                        <tr>
                            <td class="label-col">$personLabel:</td>
                            <td><strong>$partyName</strong> (صفة الحساب: $partyTypeStr | الهاتف: $partyPhone)</td>
                        </tr>
                        <tr>
                            <td class="label-col">الصندوق / الحساب المالي:</td>
                            <td>$boxName</td>
                        </tr>
                        <tr>
                            <td class="label-col">التصنيف المحاسبي:</td>
                            <td>$categoryStr</td>
                        </tr>
                        <tr>
                            <td class="label-col">البيان والشرح:</td>
                            <td><strong>$desc</strong></td>
                        </tr>
                    </table>

                    <div class="signatures">
                        <div class="sig-box">
                            توقيع المستلم
                        </div>
                        <div class="sig-box">
                            توقيع المحاسب / أمين الصندوق
                        </div>
                        <div class="sig-box">
                            الختم والاعتماد
                        </div>
                    </div>

                    <div class="footer-note">
                        تم إصدار هذا السند إلكترونياً عبر تطبيق محاسبي الذكي • صالح ومعتمد لإثبات الحركة المالية
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
