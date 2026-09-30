package com.example.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import com.example.ui.theme.MoneyDebtBlue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.ReportTemplate
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionCardItem
import com.example.ui.components.TransactionDetailsDialog
import com.example.export.ExcelExportHelper
import com.example.export.PdfExportHelper
import com.example.ui.components.SaveTemplateDialog
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AccountingViewModel,
    preselectedParty: Party? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val reportTemplates by viewModel.reportTemplates.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"

    var selectedTabIndex by remember { mutableIntStateOf(if (preselectedParty != null) 1 else 0) } // 0: Custom Reports, 1: Statements & Daily

    val currencyFormat = remember { DecimalFormat("#,##0.##") }
    val ymdFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    val todayDateString = remember { SimpleDateFormat("EEEE، dd MMMM yyyy", Locale("ar")).format(Date()) }

    // --- Custom Report Filter States ---
    var reportName by remember { mutableStateOf("تقرير مالي مخصص") }
    var selectedDatePreset by remember { mutableStateOf("ALL") } // ALL, TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_30_DAYS, CUSTOM
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // Accounts Scope: ALL, CASH_ALL, CASH_SPECIFIC, CUSTOMERS_ALL, CUSTOMER_SPECIFIC, SUPPLIERS_ALL, SUPPLIER_SPECIFIC
    var selectedAccountScope by remember { mutableStateOf("ALL") }
    var specificCashBoxId by remember { mutableStateOf<Long?>(null) }
    var specificPartyId by remember { mutableStateOf<Long?>(null) }

    // Transaction Types Selection
    var selectedTypes by remember { mutableStateOf(TransactionType.values().toSet()) }

    // Amount Range
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    var isFiltersExpanded by remember { mutableStateOf(true) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    // --- Party Statement States for Tab 1 ---
    var selectedStatementPartyId by remember(preselectedParty) { mutableStateOf(preselectedParty?.id) }
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    // --- Trial Balance / All Parties States for Tab 2 ---
    var trialSearchQuery by remember { mutableStateOf("") }
    var trialFilterType by remember { mutableStateOf("ALL") } // ALL, CUSTOMERS, SUPPLIERS, NON_ZERO
    var voucherTxForDetails by remember { mutableStateOf<TransactionRecord?>(null) }

    val partyMap = remember(parties) { parties.associateBy { it.id } }
    val boxMap = remember(cashBoxes) { cashBoxes.associateBy { it.id } }

    // Calculate Date Range Bounds
    val dateRangeBounds = remember(selectedDatePreset, customStartDate, customEndDate) {
        val nowCal = Calendar.getInstance()
        when (selectedDatePreset) {
            "TODAY" -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                Pair(start, nowCal.timeInMillis)
            }
            "YESTERDAY" -> {
                val yCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val start = Calendar.getInstance().apply {
                    timeInMillis = yCal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    timeInMillis = yCal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                Pair(start, end)
            }
            "THIS_WEEK" -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                Pair(start, nowCal.timeInMillis)
            }
            "THIS_MONTH" -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                Pair(start, nowCal.timeInMillis)
            }
            "LAST_30_DAYS" -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }.timeInMillis
                Pair(start, nowCal.timeInMillis)
            }
            "CUSTOM" -> {
                Pair(customStartDate, customEndDate)
            }
            else -> Pair(null, null)
        }
    }

    // Filtered Transactions for Custom Report
    val filteredCustomReport = remember(
        transactions,
        dateRangeBounds,
        selectedAccountScope,
        specificCashBoxId,
        specificPartyId,
        selectedTypes,
        minAmountText,
        maxAmountText
    ) {
        val minAmt = minAmountText.toDoubleOrNull()
        val maxAmt = maxAmountText.toDoubleOrNull()
        val (startTs, endTs) = dateRangeBounds

        transactions.filter { tx ->
            // Date Filter
            if (startTs != null && tx.timestamp < startTs) return@filter false
            if (endTs != null && tx.timestamp > endTs) return@filter false

            // Type Filter
            if (!selectedTypes.contains(tx.type)) return@filter false

            // Account Scope Filter
            when (selectedAccountScope) {
                "CASH_ALL" -> {
                    if (tx.cashBoxId == null && tx.targetCashBoxId == null) return@filter false
                }
                "CASH_SPECIFIC" -> {
                    if (specificCashBoxId != null && tx.cashBoxId != specificCashBoxId && tx.targetCashBoxId != specificCashBoxId) {
                        return@filter false
                    }
                }
                "CUSTOMERS_ALL" -> {
                    val p = tx.partyId?.let { partyMap[it] }
                    if (p == null || p.type != PartyType.CUSTOMER) return@filter false
                }
                "CUSTOMER_SPECIFIC" -> {
                    if (specificPartyId != null && tx.partyId != specificPartyId) return@filter false
                }
                "SUPPLIERS_ALL" -> {
                    val p = tx.partyId?.let { partyMap[it] }
                    if (p == null || p.type != PartyType.SUPPLIER) return@filter false
                }
                "SUPPLIER_SPECIFIC" -> {
                    if (specificPartyId != null && tx.partyId != specificPartyId) return@filter false
                }
            }

            // Amount Filter
            if (minAmt != null && tx.amount < minAmt) return@filter false
            if (maxAmt != null && tx.amount > maxAmt) return@filter false

            true
        }
    }

    // Summaries for Custom Report
    val customReportSummary = remember(filteredCustomReport) {
        var totalIn = 0.0
        var totalOut = 0.0
        filteredCustomReport.forEach { tx ->
            when (tx.type) {
                TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> totalIn += tx.amount
                TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> totalOut += tx.amount
                else -> {}
            }
        }
        Triple(totalIn, totalOut, totalIn - totalOut)
    }

    // Date Description text
    val dateRangeDescription = remember(selectedDatePreset, customStartDate, customEndDate) {
        when (selectedDatePreset) {
            "TODAY" -> "اليوم"
            "YESTERDAY" -> "أمس"
            "THIS_WEEK" -> "هذا الأسبوع"
            "THIS_MONTH" -> "هذا الشهر"
            "LAST_30_DAYS" -> "آخر 30 يوم"
            "CUSTOM" -> {
                val s = customStartDate?.let { ymdFormat.format(Date(it)) } ?: "البداية"
                val e = customEndDate?.let { ymdFormat.format(Date(it)) } ?: "النهاية"
                "من $s إلى $e"
            }
            else -> "جميع التواريخ"
        }
    }

    // Accounts Description text
    val accountsDescription = remember(selectedAccountScope, specificCashBoxId, specificPartyId, cashBoxes, parties) {
        when (selectedAccountScope) {
            "CASH_ALL" -> "كافة الصناديق والخزائن"
            "CASH_SPECIFIC" -> "صندوق: " + (cashBoxes.firstOrNull { it.id == specificCashBoxId }?.name ?: "محدد")
            "CUSTOMERS_ALL" -> "كافة العملاء (المدينون)"
            "CUSTOMER_SPECIFIC" -> "عميل: " + (parties.firstOrNull { it.id == specificPartyId }?.name ?: "محدد")
            "SUPPLIERS_ALL" -> "كافة الموردين (الدائنون)"
            "SUPPLIER_SPECIFIC" -> "مورد: " + (parties.firstOrNull { it.id == specificPartyId }?.name ?: "محدد")
            else -> "كافة الحسابات"
        }
    }

    val typesDescription = remember(selectedTypes) {
        if (selectedTypes.size == TransactionType.values().size) "كافة أنواع العمليات"
        else selectedTypes.joinToString("، ") { it.titleAr }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Navigation
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("التقارير المخصصة", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("كشف حساب ويومية", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("ميزان الأرصدة (مدين/دائن)", fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
        }

        if (selectedTabIndex == 0) {
            // === TAB 0: Customizable Reports & Saved Templates ===
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Saved Report Templates Carousel
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قوالب التقارير المحفوظة 📑",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showSaveTemplateDialog = true },
                                modifier = Modifier.testTag("save_as_template_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ كقالب جديد", fontSize = 12.sp)
                            }
                        }

                        if (reportTemplates.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(reportTemplates, key = { it.id }) { template ->
                                    TemplateChipItem(
                                        template = template,
                                        onSelect = {
                                            reportName = template.name
                                            selectedDatePreset = template.datePreset
                                            customStartDate = template.customStartDate
                                            customEndDate = template.customEndDate
                                            selectedAccountScope = template.accountScope
                                            specificCashBoxId = template.targetCashBoxId
                                            specificPartyId = template.targetPartyId
                                            if (template.transactionTypes == "ALL" || template.transactionTypes.isBlank()) {
                                                selectedTypes = TransactionType.values().toSet()
                                            } else {
                                                val typeNames = template.transactionTypes.split(",")
                                                selectedTypes = typeNames.mapNotNull { name ->
                                                    try { TransactionType.valueOf(name) } catch (e: Exception) { null }
                                                }.toSet()
                                            }
                                            Toast.makeText(context, "تم تطبيق قالب: ${template.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        onDelete = {
                                            viewModel.deleteReportTemplate(template)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Customization Panel (Accordion Card)
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFiltersExpanded = !isFiltersExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تخصيص معايير التقرير",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Icon(
                                    imageVector = if (isFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(visible = isFiltersExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 1. Date Range
                                    Text("1. الفترة الزمنية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val presets = listOf(
                                            "ALL" to "الكل",
                                            "TODAY" to "اليوم",
                                            "YESTERDAY" to "أمس",
                                            "THIS_WEEK" to "هذا الأسبوع",
                                            "THIS_MONTH" to "هذا الشهر",
                                            "LAST_30_DAYS" to "آخر 30 يوم",
                                            "CUSTOM" to "تخصيص تاريخ..."
                                        )
                                        items(presets) { (preset, label) ->
                                            FilterChip(
                                                selected = selectedDatePreset == preset,
                                                onClick = { selectedDatePreset = preset },
                                                label = { Text(label, fontSize = 12.sp) }
                                            )
                                        }
                                    }

                                    // Custom Date Range Picker Fields
                                    if (selectedDatePreset == "CUSTOM") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = customStartDate?.let { ymdFormat.format(Date(it)) } ?: "",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("من تاريخ") },
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        showDatePicker(context) { time -> customStartDate = time }
                                                    }) {
                                                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = customEndDate?.let { ymdFormat.format(Date(it)) } ?: "",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("إلى تاريخ") },
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        showDatePicker(context) { time -> customEndDate = time }
                                                    }) {
                                                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // 2. Accounts Scope
                                    Text("2. الحسابات والجهات:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val scopes = listOf(
                                            "ALL" to "كافة الحسابات",
                                            "CASH_ALL" to "الصناديق فقط",
                                            "CUSTOMERS_ALL" to "العملاء فقط (مدينون)",
                                            "SUPPLIERS_ALL" to "الموردين فقط (دائنون)"
                                        )
                                        items(scopes) { (scope, label) ->
                                            FilterChip(
                                                selected = selectedAccountScope == scope,
                                                onClick = {
                                                    selectedAccountScope = scope
                                                    specificCashBoxId = null
                                                    specificPartyId = null
                                                },
                                                label = { Text(label, fontSize = 12.sp) }
                                            )
                                        }
                                    }

                                    // 3. Transaction Types Multi-Select
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("3. أنواع العمليات:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Row {
                                            TextButton(onClick = { selectedTypes = TransactionType.values().toSet() }) {
                                                Text("الكل", fontSize = 11.sp)
                                            }
                                            TextButton(onClick = { selectedTypes = emptySet() }) {
                                                Text("مسح", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(TransactionType.values()) { type ->
                                            val isSelected = selectedTypes.contains(type)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedTypes = if (isSelected) selectedTypes - type else selectedTypes + type
                                                },
                                                label = { Text(type.titleAr, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    // 4. Amount Range (Optional)
                                    Text("4. نطاق المبلغ (اختياري):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = minAmountText,
                                            onValueChange = { minAmountText = it },
                                            label = { Text("الحد الأدنى ($currencySymbol)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = maxAmountText,
                                            onValueChange = { maxAmountText = it },
                                            label = { Text("الحد الأقصى ($currencySymbol)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary KPIs Card & Export Buttons
                item {
                    val (totalIn, totalOut, netBal) = customReportSummary

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ملخص التقرير المالي المخصص 📊",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$dateRangeDescription • $accountsDescription",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("المقبوضات (+)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(totalIn)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyIncomeGreen,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("المدفوعات (-)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(totalOut)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyExpenseRed,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("صافي الحركة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(netBal)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        color = if (netBal >= 0) MoneyIncomeGreen else MoneyExpenseRed,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("عدد الحركات", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${filteredCustomReport.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons (Export to Excel & PDF)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val file = ExcelExportHelper.exportCustomReportToExcel(
                                            context = context,
                                            reportTitle = reportName,
                                            dateRangeText = dateRangeDescription,
                                            accountsText = accountsDescription,
                                            typesText = typesDescription,
                                            transactions = filteredCustomReport,
                                            parties = partyMap,
                                            cashBoxes = boxMap,
                                            totalInflow = totalIn,
                                            totalOutflow = totalOut,
                                            netBalance = netBal
                                        )
                                        if (file != null) {
                                            ExcelExportHelper.shareFile(context, file)
                                        } else {
                                            Toast.makeText(context, "فشل إنشاء ملف إكسل", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_custom_excel_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41))
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تصدير Excel")
                                }

                                Button(
                                    onClick = {
                                        if (activity != null) {
                                            PdfExportHelper.printCustomReport(
                                                activity = activity,
                                                reportTitle = reportName,
                                                dateRangeText = dateRangeDescription,
                                                accountsText = accountsDescription,
                                                typesText = typesDescription,
                                                transactions = filteredCustomReport,
                                                parties = partyMap,
                                                cashBoxes = boxMap,
                                                totalInflow = totalIn,
                                                totalOutflow = totalOut,
                                                netBalance = netBal
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_custom_pdf_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("طباعة PDF")
                                }
                            }
                        }
                    }
                }

                // Table of Results Header
                item {
                    Text(
                        text = "الحركات المشمولة بالتقرير (${filteredCustomReport.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Results list
                if (filteredCustomReport.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("لا توجد حركات مطابقة لمعايير هذا التقرير", fontWeight = FontWeight.Bold)
                                Text("قم بتعديل الفترة أو الحسابات أو أنواع العمليات أعلاه", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    items(filteredCustomReport, key = { it.id }) { tx ->
                        val partyName = tx.partyId?.let { id -> parties.firstOrNull { it.id == id }?.name }
                        val boxName = tx.cashBoxId?.let { id -> cashBoxes.firstOrNull { it.id == id }?.name }

                        TransactionCardItem(
                            tx = tx,
                            partyName = partyName,
                            boxName = boxName,
                            timeString = SimpleDateFormat("dd MMM yyyy", Locale("ar")).format(Date(tx.timestamp)),
                            currencyFormat = currencyFormat,
                            defaultCurrencySymbol = currencySymbol,
                            onClick = { voucherTxForDetails = tx },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        } else if (selectedTabIndex == 1) {
            // === TAB 1: Statements & Daily Report ===
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Daily Report Section
                item {
                    val todayStart = remember {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    val todayTransactions = transactions.filter { it.timestamp >= todayStart }
                    val todayReceipts = todayTransactions.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val todayPayments = todayTransactions.filter { it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تقرير اليوم المالي 📅",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = todayDateString,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("مقبوضات اليوم (+)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(todayReceipts)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyIncomeGreen,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("مدفوعات اليوم (-)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(todayPayments)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyExpenseRed,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("صافي حركة اليوم", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    val netToday = todayReceipts - todayPayments
                                    Text(
                                        text = "${currencyFormat.format(netToday)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        color = if (netToday >= 0) MoneyIncomeGreen else MoneyExpenseRed,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "عدد العمليات المسجلة اليوم: ${todayTransactions.size} حركة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Party Statement Selector Card
                item {
                    val selectedParty = parties.firstOrNull { it.id == selectedStatementPartyId }
                    val partyTransactions = if (selectedStatementPartyId != null) {
                        transactions.filter { it.partyId == selectedStatementPartyId }
                    } else emptyList()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "كشف حساب تفصيلي لطرف (عميل / مورد)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = partyDropdownExpanded,
                                onExpandedChange = { partyDropdownExpanded = !partyDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedParty?.name ?: "اختر عميل أو مورد لعرض كشف حسابه...",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الطرف المعني") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = partyDropdownExpanded,
                                    onDismissRequest = { partyDropdownExpanded = false }
                                ) {
                                    parties.forEach { party ->
                                        val label = if (party.type == PartyType.CUSTOMER) "عميل" else "مورد"
                                        DropdownMenuItem(
                                            text = { Text("${party.name} ($label - رصيد: ${currencyFormat.format(party.balance)} $currencySymbol)") },
                                            onClick = {
                                                selectedStatementPartyId = party.id
                                                partyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedParty != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                val isCustomer = selectedParty.type == PartyType.CUSTOMER

                                // Calculate Debit, Credit, and progressive running balance
                                val sortedPartyTx = partyTransactions.sortedBy { it.timestamp }
                                var totalDebit = 0.0
                                var totalCredit = 0.0

                                val ledgerItems = sortedPartyTx.map { tx ->
                                    var debit = 0.0
                                    var credit = 0.0

                                    if (isCustomer) {
                                        when (tx.type) {
                                            TransactionType.CUSTOMER_NEW_DEBIT, TransactionType.INCOME -> debit = tx.amount
                                            TransactionType.CUSTOMER_RECEIPT -> credit = tx.amount
                                            TransactionType.SETTLEMENT -> {
                                                if (tx.amount >= 0) debit = tx.amount else credit = -tx.amount
                                            }
                                            else -> debit = tx.amount
                                        }
                                    } else {
                                        when (tx.type) {
                                            TransactionType.SUPPLIER_NEW_CREDIT, TransactionType.EXPENSE -> credit = tx.amount
                                            TransactionType.SUPPLIER_PAYMENT -> debit = tx.amount
                                            TransactionType.SETTLEMENT -> {
                                                if (tx.amount >= 0) credit = tx.amount else debit = -tx.amount
                                            }
                                            else -> credit = tx.amount
                                        }
                                    }

                                    totalDebit += debit
                                    totalCredit += credit

                                    Triple(tx, debit, credit)
                                }

                                val finalBalanceSide = if (isCustomer) {
                                    if (selectedParty.balance >= 0) "دين عليه (لنا عنده)" else "دين له (علينا له)"
                                } else {
                                    if (selectedParty.balance >= 0) "دين له (علينا له)" else "دين عليه (لنا عنده)"
                                }

                                // 3 Metric Summary Cards: Debit, Credit, Balance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MoneyIncomeGreen.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("إجمالي مدين (عليه)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${currencyFormat.format(totalDebit)} $currencySymbol",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MoneyIncomeGreen
                                            )
                                        }
                                    }

                                    Surface(
                                        color = MoneyExpenseRed.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("إجمالي دائن (له)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${currencyFormat.format(totalCredit)} $currencySymbol",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MoneyExpenseRed
                                            )
                                        }
                                    }

                                    Surface(
                                        color = PrimaryGreen.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("الرصيد الصافي", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${currencyFormat.format(Math.abs(selectedParty.balance))} $currencySymbol",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "حالة الرصيد النهائي: $finalBalanceSide • (${partyTransactions.size} حركة مسجلة)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val file = ExcelExportHelper.exportAccountStatementToExcel(
                                                context = context,
                                                party = selectedParty,
                                                transactions = partyTransactions,
                                                cashBoxes = boxMap,
                                                currencySymbol = currencySymbol
                                            )
                                            if (file != null) {
                                                ExcelExportHelper.shareFile(context, file)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إكسل (مدين/دائن/رصيد)", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (activity != null) {
                                                PdfExportHelper.printPartyStatement(
                                                    activity = activity,
                                                    party = selectedParty,
                                                    transactions = partyTransactions,
                                                    cashBoxes = boxMap,
                                                    currencySymbol = currencySymbol
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("طباعة PDF", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Party Ledger Transactions List (مدين / دائن / الرصيد)
                if (selectedStatementPartyId != null) {
                    val selectedParty = parties.firstOrNull { it.id == selectedStatementPartyId }
                    if (selectedParty != null) {
                        val isCustomer = selectedParty.type == PartyType.CUSTOMER
                        val sortedPartyTx = transactions.filter { it.partyId == selectedParty.id }.sortedBy { it.timestamp }

                        if (sortedPartyTx.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("لا توجد حركات مسجلة لهذا الطرف حتى الآن", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "جدول حركات الحساب التفصيلي (${sortedPartyTx.size} حركة):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            var runningProgBalance = 0.0
                            items(sortedPartyTx.size) { index ->
                                val tx = sortedPartyTx[index]
                                val boxName = tx.cashBoxId?.let { boxMap[it]?.name }
                                var debit = 0.0
                                var credit = 0.0

                                if (isCustomer) {
                                    when (tx.type) {
                                        TransactionType.CUSTOMER_NEW_DEBIT, TransactionType.INCOME -> {
                                            debit = tx.amount
                                            runningProgBalance += tx.amount
                                        }
                                        TransactionType.CUSTOMER_RECEIPT -> {
                                            credit = tx.amount
                                            runningProgBalance -= tx.amount
                                        }
                                        TransactionType.SETTLEMENT -> {
                                            if (tx.amount >= 0) {
                                                debit = tx.amount
                                                runningProgBalance += tx.amount
                                            } else {
                                                credit = -tx.amount
                                                runningProgBalance -= -tx.amount
                                            }
                                        }
                                        else -> {
                                            debit = tx.amount
                                            runningProgBalance += tx.amount
                                        }
                                    }
                                } else {
                                    when (tx.type) {
                                        TransactionType.SUPPLIER_NEW_CREDIT, TransactionType.EXPENSE -> {
                                            credit = tx.amount
                                            runningProgBalance += tx.amount
                                        }
                                        TransactionType.SUPPLIER_PAYMENT -> {
                                            debit = tx.amount
                                            runningProgBalance -= tx.amount
                                        }
                                        TransactionType.SETTLEMENT -> {
                                            if (tx.amount >= 0) {
                                                credit = tx.amount
                                                runningProgBalance += tx.amount
                                            } else {
                                                debit = -tx.amount
                                                runningProgBalance -= -tx.amount
                                            }
                                        }
                                        else -> {
                                            credit = tx.amount
                                            runningProgBalance += tx.amount
                                        }
                                    }
                                }

                                val balanceSide = if (isCustomer) {
                                    if (runningProgBalance >= 0) "عليه" else "له"
                                } else {
                                    if (runningProgBalance >= 0) "له" else "عليه"
                                }

                                PartyStatementLedgerItem(
                                    tx = tx,
                                    index = index + 1,
                                    debit = debit,
                                    credit = credit,
                                    runningBalance = runningProgBalance,
                                    balanceSide = balanceSide,
                                    boxName = boxName,
                                    currencySymbol = currencySymbol,
                                    currencyFormat = currencyFormat,
                                    onClick = { voucherTxForDetails = tx }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // === TAB 2: Trial Balance / All Parties Balances (ميزان الأرصدة الشامل - مدين / دائن / رصيد) ===
            val txByParty = remember(transactions) { transactions.groupBy { it.partyId } }

            val totalCustomersDebit = remember(parties) {
                parties.filter { it.type == PartyType.CUSTOMER && it.balance > 0 }.sumOf { it.balance }
            }
            val totalSuppliersCredit = remember(parties) {
                parties.filter { it.type == PartyType.SUPPLIER && it.balance > 0 }.sumOf { it.balance }
            }
            val netMarketDebtPosition = remember(totalCustomersDebit, totalSuppliersCredit) {
                totalCustomersDebit - totalSuppliersCredit
            }

            val filteredTrialParties = remember(parties, trialSearchQuery, trialFilterType) {
                parties.filter { party ->
                    when (trialFilterType) {
                        "CUSTOMERS" -> party.type == PartyType.CUSTOMER
                        "SUPPLIERS" -> party.type == PartyType.SUPPLIER
                        "NON_ZERO" -> party.balance != 0.0
                        else -> true
                    }
                }.filter { party ->
                    trialSearchQuery.isBlank() ||
                        party.name.contains(trialSearchQuery, ignoreCase = true) ||
                        party.phone.contains(trialSearchQuery)
                }.sortedWith(compareBy({ it.type }, { -Math.abs(it.balance) }))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header KPIs Summary Card
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ميزان أرصدة الأطراف والديون ⚖️",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${parties.size} طرف مسجل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MoneyIncomeGreen.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("ديون العملاء (لنا عندهم)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${currencyFormat.format(totalCustomersDebit)} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MoneyIncomeGreen
                                        )
                                    }
                                }

                                Surface(
                                    color = MoneyExpenseRed.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("التزامات الموردين (علينا)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${currencyFormat.format(totalSuppliersCredit)} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MoneyExpenseRed
                                        )
                                    }
                                }

                                Surface(
                                    color = PrimaryGreen.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("صافي مركز الديون", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${currencyFormat.format(Math.abs(netMarketDebtPosition))} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (netMarketDebtPosition >= 0) PrimaryGreen else MoneyExpenseRed
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Export buttons for the entire Trial Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val file = ExcelExportHelper.exportTrialBalanceToExcel(
                                            context = context,
                                            parties = parties,
                                            transactions = transactions,
                                            currencySymbol = currencySymbol
                                        )
                                        if (file != null) {
                                            ExcelExportHelper.shareFile(context, file)
                                        } else {
                                            Toast.makeText(context, "فشل إنشاء ملف الإكسل", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصدير إكسل (CSV)", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (activity != null) {
                                            PdfExportHelper.printTrialBalanceReport(
                                                activity = activity,
                                                parties = parties,
                                                transactions = transactions,
                                                currencySymbol = currencySymbol
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("طباعة PDF رسمي", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Search & Filter Toolbar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = trialSearchQuery,
                            onValueChange = { trialSearchQuery = it },
                            placeholder = { Text("بحث باسم الطرف أو الهاتف...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen) },
                            trailingIcon = {
                                if (trialSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { trialSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val filters = listOf(
                                "ALL" to "الكل (${parties.size})",
                                "CUSTOMERS" to "العملاء (مدينون)",
                                "SUPPLIERS" to "الموردين (دائنون)",
                                "NON_ZERO" to "الأرصدة النشطة"
                            )
                            items(filters) { (type, label) ->
                                FilterChip(
                                    selected = trialFilterType == type,
                                    onClick = { trialFilterType = type },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // List of parties in Trial Balance with Debit, Credit, and Balance
                if (filteredTrialParties.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (trialSearchQuery.isNotBlank()) "لم يتم العثور على أطراف مطابقة للبحث" else "لا توجد أطراف مسجلة بعد",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTrialParties, key = { it.id }) { party ->
                        val isCustomer = party.type == PartyType.CUSTOMER
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
                            party.balance > 0 -> if (isCustomer) "دين عليه (لنا عنده)" else "دين له (علينا له)"
                            party.balance < 0 -> if (isCustomer) "دين له (علينا له)" else "دين عليه (لنا عنده)"
                            else -> "خالص (0.00)"
                        }

                        val statusColor = when {
                            party.balance > 0 -> if (isCustomer) MoneyIncomeGreen else MoneyExpenseRed
                            party.balance < 0 -> if (isCustomer) MoneyExpenseRed else MoneyIncomeGreen
                            else -> Color.Gray
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (isCustomer) PrimaryGreen.copy(alpha = 0.12f) else MoneyDebtBlue.copy(alpha = 0.12f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isCustomer) Icons.Default.Person else Icons.Default.Store,
                                                    contentDescription = null,
                                                    tint = if (isCustomer) PrimaryGreen else MoneyDebtBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(party.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = if (isCustomer) "عميل ${if (party.phone.isNotBlank()) "• ${party.phone}" else ""}" else "مورد ${if (party.phone.isNotBlank()) "• ${party.phone}" else ""}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Surface(
                                        color = statusColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontSize = 10.sp,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Debit, Credit, Balance Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("إجمالي مدين (عليه)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = "${currencyFormat.format(partyDebit)} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MoneyIncomeGreen
                                        )
                                    }
                                    Column {
                                        Text("إجمالي دائن (له)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = "${currencyFormat.format(partyCredit)} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MoneyExpenseRed
                                        )
                                    }
                                    Column {
                                        Text("الرصيد الصافي", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = "${currencyFormat.format(Math.abs(party.balance))} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = statusColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Actions: View Statement in Tab 1, WhatsApp, Excel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedStatementPartyId = party.id
                                            selectedTabIndex = 1
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("كشف الحساب التفصيلي", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (party.phone.isNotBlank()) {
                                            IconButton(
                                                onClick = { openPartyWhatsApp(context, party, currencySymbol) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                val file = ExcelExportHelper.exportAccountStatementToExcel(
                                                    context = context,
                                                    party = party,
                                                    transactions = partyTx,
                                                    cashBoxes = boxMap,
                                                    currencySymbol = currencySymbol
                                                )
                                                if (file != null) {
                                                    ExcelExportHelper.shareFile(context, file)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.TableChart, contentDescription = "إكسل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Template Dialog
    if (showSaveTemplateDialog) {
        val summaryText = "$dateRangeDescription • $accountsDescription • $typesDescription"
        SaveTemplateDialog(
            currentFilterSummary = summaryText,
            onDismiss = { showSaveTemplateDialog = false },
            onConfirm = { name ->
                val template = ReportTemplate(
                    name = name,
                    datePreset = selectedDatePreset,
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    accountScope = selectedAccountScope,
                    targetCashBoxId = specificCashBoxId,
                    targetPartyId = specificPartyId,
                    transactionTypes = if (selectedTypes.size == TransactionType.values().size) "ALL" else selectedTypes.joinToString(",") { it.name },
                    minAmount = minAmountText.toDoubleOrNull(),
                    maxAmount = maxAmountText.toDoubleOrNull()
                )
                viewModel.saveReportTemplate(template)
                showSaveTemplateDialog = false
            }
        )
    }

    voucherTxForDetails?.let { tx ->
        val party = parties.firstOrNull { it.id == tx.partyId }
        val box = cashBoxes.firstOrNull { it.id == tx.cashBoxId }
        TransactionDetailsDialog(
            transaction = tx,
            party = party,
            cashBox = box,
            currencySymbol = currencySymbol,
            onDelete = { viewModel.deleteTransaction(tx) },
            onDismiss = { voucherTxForDetails = null }
        )
    }
}

@Composable
fun TemplateChipItem(
    template: ReportTemplate,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        onClick = onSelect,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(template.name, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "حذف القالب", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun showDatePicker(context: android.content.Context, onDateSelected: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
            onDateSelected(selected)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
fun PartyStatementLedgerItem(
    tx: TransactionRecord,
    index: Int,
    debit: Double,
    credit: Double,
    runningBalance: Double,
    balanceSide: String,
    boxName: String?,
    currencySymbol: String,
    currencyFormat: DecimalFormat,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "$index", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("ar")).format(Date(tx.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tx.type.titleAr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tx.description.ifBlank { tx.type.titleAr },
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!boxName.isNullOrBlank() && boxName != "-") {
                Text(
                    text = "الصندوق: $boxName",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Debit, Credit, Running Balance Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Debit (عليه)
                Column(horizontalAlignment = Alignment.Start) {
                    Text("مدين (عليه)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = if (debit > 0) "+${currencyFormat.format(debit)} $currencySymbol" else "-",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (debit > 0) MoneyIncomeGreen else Color.Gray
                    )
                }

                // Credit (له)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("دائن (له)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = if (credit > 0) "-${currencyFormat.format(credit)} $currencySymbol" else "-",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (credit > 0) MoneyExpenseRed else Color.Gray
                    )
                }

                // Balance (الرصيد بعد الحركة)
                Column(horizontalAlignment = Alignment.End) {
                    Text("الرصيد بعد الحركة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${currencyFormat.format(Math.abs(runningBalance))} $currencySymbol ($balanceSide)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun openPartyWhatsApp(context: android.content.Context, party: Party, currencySymbol: String) {
    val amountFormatted = DecimalFormat("#,##0.##").format(Math.abs(party.balance))
    val isCustomer = party.type == PartyType.CUSTOMER
    val message = if (isCustomer) {
        "السلام عليكم ورحمة الله أخي الكريم ${party.name}،\nنود إحاطتكم بأن رصيد الحساب المسجل لدينا هو $amountFormatted $currencySymbol (${if (party.balance >= 0) "مستحق عليكم" else "لكم علينا"}).\nشاكرين ومقدرين حسن تعاونكم الدائم معنا."
    } else {
        "السلام عليكم ورحمة الله أخي الكريم ${party.name}،\nبخصوص حسابنا لديكم، مسجل لدينا رصيد قدره $amountFormatted $currencySymbol (${if (party.balance >= 0) "مستحق لكم علينا" else "لنا لديكم"}).\nشاكرين ومقدرين حسن تعاونكم الدائم."
    }

    try {
        val cleanPhone = party.phone.replace(Regex("[^0-9+]"), "")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}
