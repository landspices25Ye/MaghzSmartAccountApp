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
        title: String = "كشف العمليات المالية"
    ) {
        val html = buildTransactionsHtml(transactions, parties, cashBoxes, title)
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
        netBalance: Double
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
            netBalance
        )
        printHtml(activity, html, reportTitle)
    }

    fun printPartyStatement(
        activity: Activity,
        party: Party,
        transactions: List<TransactionRecord>,
        cashBoxes: Map<Long, CashBox>
    ) {
        val html = buildPartyHtml(party, transactions, cashBoxes)
        printHtml(activity, html, "كشف_حساب_${party.name}")
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
        netBalance: Double
    ): String {
        val rows = StringBuilder()
        transactions.forEachIndexed { i, tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val partyName = tx.partyId?.let { parties[it]?.name } ?: "-"
            val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
            rows.append("""
                <tr>
                    <td>${i + 1}</td>
                    <td>$dateStr</td>
                    <td><span class="badge">${tx.type.titleAr}</span></td>
                    <td>$partyName</td>
                    <td>$boxName</td>
                    <td class="amount">${currencyFormat.format(tx.amount)} ر.س</td>
                    <td>${tx.description}</td>
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
                    .title { font-size: 24px; font-weight: bold; color: #0D6B42; margin-bottom: 4px; }
                    .subtitle { font-size: 13px; color: #555; }
                    .filter-pill { display: inline-block; background: #e8f5e9; color: #1b5e20; border-radius: 4px; padding: 4px 8px; font-size: 12px; margin-left: 6px; }
                    .metrics-container { display: flex; gap: 12px; margin: 16px 0; }
                    .metric-box { flex: 1; padding: 12px; border-radius: 8px; background: #f7faf7; border: 1px solid #dcdfdc; text-align: center; }
                    .metric-title { font-size: 11px; color: #666; margin-bottom: 4px; }
                    .metric-value { font-size: 18px; font-weight: bold; }
                    .inflow { color: #1B8755; }
                    .outflow { color: #D32F2F; }
                    .net { color: #0D6B42; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 10px 8px; text-align: right; font-size: 12px; }
                    th { background-color: #0D6B42; color: white; }
                    tr:nth-child(even) { background-color: #f9fbf9; }
                    .amount { font-weight: bold; color: #0D6B42; text-align: left; }
                    .badge { background: #e8f5e9; color: #1b5e20; padding: 3px 8px; border-radius: 4px; font-size: 11px; }
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
                        <div class="metric-title">إجمالي المقبوضات/الدخل</div>
                        <div class="metric-value inflow">+${currencyFormat.format(totalInflow)} ر.س</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">إجمالي المدفوعات/المصروف</div>
                        <div class="metric-value outflow">-${currencyFormat.format(totalOutflow)} ر.س</div>
                    </div>
                    <div class="metric-box">
                        <div class="metric-title">صافي الحركة</div>
                        <div class="metric-value net">${currencyFormat.format(netBalance)} ر.س</div>
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
                            <th>المبلغ</th>
                            <th>البيان والتفاصيل</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildTransactionsHtml(
        transactions: List<TransactionRecord>,
        parties: Map<Long, Party>,
        cashBoxes: Map<Long, CashBox>,
        title: String
    ): String {
        val rows = StringBuilder()
        var totalAmount = 0.0

        transactions.forEachIndexed { i, tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val partyName = tx.partyId?.let { parties[it]?.name } ?: "-"
            val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
            totalAmount += tx.amount

            rows.append("""
                <tr>
                    <td>${i + 1}</td>
                    <td>$dateStr</td>
                    <td><span class="badge">${tx.type.titleAr}</span></td>
                    <td>$partyName</td>
                    <td>$boxName</td>
                    <td class="amount">${currencyFormat.format(tx.amount)} ر.س</td>
                    <td>${tx.description}</td>
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
                    .header { text-align: center; border-bottom: 2px solid #0D6B42; padding-bottom: 12px; margin-bottom: 20px; }
                    .title { font-size: 24px; font-weight: bold; color: #0D6B42; margin-bottom: 5px; }
                    .meta { font-size: 13px; color: #666; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 10px 8px; text-align: right; font-size: 13px; }
                    th { background-color: #0D6B42; color: white; }
                    tr:nth-child(even) { background-color: #f9fbf9; }
                    .amount { font-weight: bold; color: #0D6B42; text-align: left; }
                    .badge { background: #e8f5e9; color: #1b5e20; padding: 3px 8px; border-radius: 4px; font-size: 11px; }
                    .summary { margin-top: 20px; padding: 15px; background: #e8f5e9; border-radius: 8px; font-weight: bold; font-size: 15px; display: flex; justify-content: space-between; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">محاسبي الذكي - $title</div>
                    <div class="meta">تاريخ التقرير: ${dateFormat.format(Date())} | إجمالي العمليات: ${transactions.size}</div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>التاريخ</th>
                            <th>النوع</th>
                            <th>الطرف</th>
                            <th>الصندوق</th>
                            <th>المبلغ</th>
                            <th>البيان</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>
                <div class="summary">
                    <span>إجمالي المبالغ المسجلة:</span>
                    <span>${currencyFormat.format(totalAmount)} ريال سعودي</span>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildPartyHtml(
        party: Party,
        transactions: List<TransactionRecord>,
        cashBoxes: Map<Long, CashBox>
    ): String {
        val rows = StringBuilder()
        transactions.forEachIndexed { i, tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val boxName = tx.cashBoxId?.let { cashBoxes[it]?.name } ?: "-"
            rows.append("""
                <tr>
                    <td>${i + 1}</td>
                    <td>$dateStr</td>
                    <td><span class="badge">${tx.type.titleAr}</span></td>
                    <td>$boxName</td>
                    <td class="amount">${currencyFormat.format(tx.amount)} ر.س</td>
                    <td>${tx.description}</td>
                </tr>
            """.trimIndent())
        }

        val partyTypeLabel = if (party.type == PartyType.CUSTOMER) "عميل (مدين)" else "مورد (دائن)"
        val balanceDesc = if (party.type == PartyType.CUSTOMER) {
            if (party.balance >= 0) "ما لنا عنده (دين عليه): ${currencyFormat.format(party.balance)} ر.س"
            else "ما له علينا: ${currencyFormat.format(-party.balance)} ر.س"
        } else {
            if (party.balance >= 0) "ما له علينا (دين له): ${currencyFormat.format(party.balance)} ر.س"
            else "ما لنا عنده: ${currencyFormat.format(-party.balance)} ر.س"
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; direction: rtl; color: #222; }
                    .header { border-bottom: 2px solid #0D6B42; padding-bottom: 12px; margin-bottom: 20px; }
                    .title { font-size: 24px; font-weight: bold; color: #0D6B42; }
                    .info-box { background: #f0f7f3; border: 1px solid #cce5d6; padding: 12px; border-radius: 8px; margin: 15px 0; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 10px 8px; text-align: right; font-size: 13px; }
                    th { background-color: #0D6B42; color: white; }
                    tr:nth-child(even) { background-color: #f9fbf9; }
                    .amount { font-weight: bold; color: #0D6B42; text-align: left; }
                    .badge { background: #e8f5e9; color: #1b5e20; padding: 3px 8px; border-radius: 4px; font-size: 11px; }
                    .balance-tag { font-size: 16px; font-weight: bold; color: #b71c1c; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">كشف حساب: ${party.name}</div>
                    <div>نوع الطرف: $partyTypeLabel | الهاتف: ${party.phone.ifBlank { "غير مسجل" }}</div>
                </div>
                <div class="info-box">
                    <div class="balance-tag">الرصيد الصافي: $balanceDesc</div>
                    <div style="font-size: 12px; color: #666; margin-top: 4px;">تاريخ الاستخراج: ${dateFormat.format(Date())}</div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>التاريخ</th>
                            <th>نوع الحركة</th>
                            <th>الصندوق</th>
                            <th>المبلغ</th>
                            <th>البيان</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()
    }
}
