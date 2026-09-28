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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
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
import com.example.ui.components.TransactionCardItem
import com.example.data.model.TransactionType
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
                text = { Text("التقارير المخصصة والقوالب", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("كشوفات الحسابات واليومية", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
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
                                            label = { Text("الحد الأدنى (ر.س)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = maxAmountText,
                                            onValueChange = { maxAmountText = it },
                                            label = { Text("الحد الأقصى (ر.س)") },
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
                                        text = "${currencyFormat.format(totalIn)} ر.س",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyIncomeGreen,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("المدفوعات (-)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(totalOut)} ر.س",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyExpenseRed,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("صافي الحركة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(netBal)} ر.س",
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
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        } else {
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
                                        text = "${currencyFormat.format(todayReceipts)} ر.س",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyIncomeGreen,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("مدفوعات اليوم (-)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${currencyFormat.format(todayPayments)} ر.س",
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyExpenseRed,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text("صافي حركة اليوم", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    val netToday = todayReceipts - todayPayments
                                    Text(
                                        text = "${currencyFormat.format(netToday)} ر.س",
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
                                            text = { Text("${party.name} ($label - رصيد: ${party.balance} ر.س)") },
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
                                val balanceDesc = if (isCustomer) {
                                    if (selectedParty.balance >= 0) "ما لنا عنده (دين عليه): ${currencyFormat.format(selectedParty.balance)} ر.س"
                                    else "ما له علينا: ${currencyFormat.format(-selectedParty.balance)} ر.س"
                                } else {
                                    if (selectedParty.balance >= 0) "ما له علينا (دين له): ${currencyFormat.format(selectedParty.balance)} ر.س"
                                    else "ما لنا عنده: ${currencyFormat.format(-selectedParty.balance)} ر.س"
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = balanceDesc,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (selectedParty.balance > 0) (if (isCustomer) MoneyIncomeGreen else MoneyExpenseRed) else Color.Gray
                                        )
                                        Text(
                                            text = "عدد الحركات المسجلة: ${partyTransactions.size}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

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
                                                cashBoxes = boxMap
                                            )
                                            if (file != null) {
                                                ExcelExportHelper.shareFile(context, file)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إكسل الكشف", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (activity != null) {
                                                PdfExportHelper.printPartyStatement(
                                                    activity = activity,
                                                    party = selectedParty,
                                                    transactions = partyTransactions,
                                                    cashBoxes = boxMap
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("طباعة PDF", fontSize = 12.sp)
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
