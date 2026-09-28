package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.export.ExcelExportHelper
import com.example.export.PdfExportHelper
import com.example.search.IntelligentSearchEngine
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.TransactionCardItem
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    // Advanced manual filters (can be combined with intelligent search)
    var filterDatePreset by remember { mutableStateOf("ALL") }
    var filterMinAmount by remember { mutableStateOf<Double?>(null) }
    var filterMaxAmount by remember { mutableStateOf<Double?>(null) }

    val currencyFormat = remember { DecimalFormat("#,##0.##") }
    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
            }
        } else {
            showVoiceDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceDialog = true
        } else {
            showVoiceDialog = true
        }
    }

    // Execute Intelligent Search
    val searchSummary = remember(transactions, parties, cashBoxes, searchQuery, selectedCategoryFilter, filterDatePreset, filterMinAmount, filterMaxAmount) {
        // First run intelligent NLP search
        val initialSummary = IntelligentSearchEngine.executeSearch(
            query = searchQuery,
            transactions = transactions,
            parties = parties,
            cashBoxes = cashBoxes
        )

        var list = initialSummary.transactions

        // Apply quick category tab filter if not 'الكل'
        if (selectedCategoryFilter != "الكل") {
            list = list.filter { tx ->
                when (selectedCategoryFilter) {
                    "مقبوضات" -> tx.type == TransactionType.CUSTOMER_RECEIPT || tx.type == TransactionType.INCOME
                    "مدفوعات" -> tx.type == TransactionType.SUPPLIER_PAYMENT || tx.type == TransactionType.EXPENSE
                    "ديون" -> tx.type == TransactionType.CUSTOMER_NEW_DEBIT || tx.type == TransactionType.SUPPLIER_NEW_CREDIT
                    "مصروفات" -> tx.type == TransactionType.EXPENSE
                    "تحويلات" -> tx.type == TransactionType.TRANSFER
                    else -> true
                }
            }
        }

        // Apply extra min/max amount if set from filter sheet
        if (filterMinAmount != null) {
            list = list.filter { it.amount >= filterMinAmount!! }
        }
        if (filterMaxAmount != null) {
            list = list.filter { it.amount <= filterMaxAmount!! }
        }

        var total = 0.0
        var inflow = 0.0
        var outflow = 0.0
        list.forEach { tx ->
            total += tx.amount
            when (tx.type) {
                TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> inflow += tx.amount
                TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> outflow += tx.amount
                else -> {}
            }
        }

        searchSummaryCopy(
            list,
            total,
            inflow,
            outflow,
            initialSummary.matchedCriteriaDescription
        )
    }

    val partyMap = remember(parties) { parties.associateBy { it.id } }
    val boxMap = remember(cashBoxes) { cashBoxes.associateBy { it.id } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "عملية جديدة")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Intelligent Search Input Bar
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("intelligent_search_input"),
                            placeholder = { Text("بحث ذكي (كلمة، تاريخ، مبلغ > 500، شخص)...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "بحث", tint = PrimaryGreen)
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            showVoiceDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "بحث صوتي", tint = PrimaryGreen)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filterMinAmount != null || filterMaxAmount != null || filterDatePreset != "ALL") {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "تصفية متقدمة",
                                tint = if (filterMinAmount != null || filterMaxAmount != null || filterDatePreset != "ALL") PrimaryGreen else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Smart Examples Chips
                    AnimatedVisibility(visible = searchQuery.isEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val smartQueries = listOf(
                                "> 500 $currencySymbol",
                                "اليوم",
                                "أمس",
                                "هذا الأسبوع",
                                "هذا الشهر",
                                "بنزين",
                                "مقبوضات",
                                "مصروف"
                            )
                            items(smartQueries) { queryExample ->
                                AssistChip(
                                    onClick = { searchQuery = queryExample },
                                    label = { Text(queryExample, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Active Search Criteria Badge
                    if (searchQuery.isNotBlank() && searchSummary.matchedCriteriaDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔍 تحليل البحث: ${searchSummary.matchedCriteriaDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Quick Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("الكل", "مقبوضات", "مدفوعات", "ديون", "مصروفات", "تحويلات")
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedCategoryFilter == filter,
                        onClick = { selectedCategoryFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            // Live Search Metrics & Export Actions Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "النتائج: ${searchSummary.count} حركة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (searchSummary.totalInflow > 0) {
                                Text(
                                    text = "+${currencyFormat.format(searchSummary.totalInflow)} $currencySymbol",
                                    color = MoneyIncomeGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (searchSummary.totalOutflow > 0) {
                                Text(
                                    text = "-${currencyFormat.format(searchSummary.totalOutflow)} $currencySymbol",
                                    color = MoneyExpenseRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Direct Export Buttons for current search results
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val file = ExcelExportHelper.exportCustomReportToExcel(
                                    context = context,
                                    reportTitle = if (searchQuery.isNotBlank()) "بحث_$searchQuery" else "العمليات_المالية",
                                    dateRangeText = "حسب البحث والفلترة",
                                    accountsText = "كافة الحسابات المطابقة",
                                    typesText = selectedCategoryFilter,
                                    transactions = searchSummary.transactions,
                                    parties = partyMap,
                                    cashBoxes = boxMap,
                                    totalInflow = searchSummary.totalInflow,
                                    totalOutflow = searchSummary.totalOutflow,
                                    netBalance = searchSummary.netBalance
                                )
                                if (file != null) {
                                    ExcelExportHelper.shareFile(context, file)
                                } else {
                                    Toast.makeText(context, "فشل إنشاء ملف إكسل", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = "تصدير نتائج البحث إكسل", tint = Color(0xFF107C41))
                        }

                        IconButton(
                            onClick = {
                                if (activity != null) {
                                    PdfExportHelper.printCustomReport(
                                        activity = activity,
                                        reportTitle = if (searchQuery.isNotBlank()) "نتائج البحث: $searchQuery" else "كشف العمليات المالية",
                                        dateRangeText = "حسب معايير البحث",
                                        accountsText = "الحسابات المطابقة",
                                        typesText = selectedCategoryFilter,
                                        transactions = searchSummary.transactions,
                                        parties = partyMap,
                                        cashBoxes = boxMap,
                                        totalInflow = searchSummary.totalInflow,
                                        totalOutflow = searchSummary.totalOutflow,
                                        netBalance = searchSummary.netBalance
                                    )
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "طباعة نتائج البحث PDF", tint = Color(0xFFD32F2F))
                        }
                    }
                }
            }

            // Transactions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchSummary.transactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("لا توجد حركات مالية مطابقة للبحث", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "جرّب البحث بكلمة أخرى، أو مسح الفلاتر، أو التحدث بالصوت",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (searchQuery.isNotEmpty() || filterMinAmount != null || filterMaxAmount != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            searchQuery = ""
                                            selectedCategoryFilter = "الكل"
                                            filterMinAmount = null
                                            filterMaxAmount = null
                                        }
                                    ) {
                                        Text("إعادة ضبط معايير البحث")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(searchSummary.transactions, key = { it.id }) { tx ->
                        val partyName = tx.partyId?.let { id -> parties.firstOrNull { it.id == id }?.name }
                        val boxName = tx.cashBoxId?.let { id -> cashBoxes.firstOrNull { it.id == id }?.name }

                        TransactionCardItem(
                            tx = tx,
                            partyName = partyName,
                            boxName = boxName,
                            timeString = timeFormat.format(Date(tx.timestamp)),
                            currencyFormat = currencyFormat,
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }

    // Advanced Filter Modal Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "تصفية متقدمة لنتائج البحث",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Amount Range
                Text("نطاق المبلغ ($currencySymbol):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = filterMinAmount?.toString() ?: "",
                        onValueChange = { filterMinAmount = it.toDoubleOrNull() },
                        label = { Text("الحد الأدنى") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = filterMaxAmount?.toString() ?: "",
                        onValueChange = { filterMaxAmount = it.toDoubleOrNull() },
                        label = { Text("الحد الأقصى") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Quick Amount Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { filterMinAmount = 0.0; filterMaxAmount = 100.0 },
                        label = { Text("< 100 $currencySymbol", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { filterMinAmount = 100.0; filterMaxAmount = 500.0 },
                        label = { Text("100 - 500", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { filterMinAmount = 500.0; filterMaxAmount = 2000.0 },
                        label = { Text("500 - 2000", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { filterMinAmount = 2000.0; filterMaxAmount = null },
                        label = { Text("> 2000 $currencySymbol", fontSize = 11.sp) }
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تطبيق التصفية")
                    }
                    OutlinedButton(
                        onClick = {
                            filterMinAmount = null
                            filterMaxAmount = null
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء التصفية")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            cashBoxes = cashBoxes,
            parties = parties,
            categories = expenseCategories,
            currencies = currencies,
            defaultCurrencySymbol = currencySymbol,
            onDismiss = { showAddDialog = false },
            onConfirm = { type, amount, cashBoxId, partyId, description, targetCashBoxId, cat, curr ->
                viewModel.recordDirectTransaction(
                    type = type,
                    amount = amount,
                    cashBoxId = cashBoxId,
                    partyId = partyId,
                    description = description,
                    targetCashBoxId = targetCashBoxId,
                    category = cat,
                    currency = curr
                )
                showAddDialog = false
            }
        )
    }

    if (showVoiceDialog) {
        com.example.ui.components.VoiceTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false },
            onSuccess = { reply ->
                showVoiceDialog = false
            }
        )
    }
}

private fun searchSummaryCopy(
    list: List<com.example.data.model.TransactionRecord>,
    total: Double,
    inflow: Double,
    outflow: Double,
    desc: String
): com.example.search.SearchSummary {
    return com.example.search.SearchSummary(
        transactions = list,
        count = list.size,
        totalAmount = total,
        totalInflow = inflow,
        totalOutflow = outflow,
        netBalance = inflow - outflow,
        matchedCriteriaDescription = desc
    )
}
