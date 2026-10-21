package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.domain.BatchCommandItem
import com.example.domain.BatchCommandStatus
import com.example.domain.BatchCommandsEngine
import com.example.domain.BatchExecutionSummary
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchCommandsDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val batchItems by viewModel.batchItems.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isBatchExecuting.collectAsStateWithLifecycle()
    val progress by viewModel.batchProgress.collectAsStateWithLifecycle()
    val batchSummary by viewModel.batchSummary.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"

    val clipboardManager = LocalClipboardManager.current
    val decimalFormat = remember { DecimalFormat("#,##0.##") }

    var rawInputText by remember {
        mutableStateOf(
            """
            قبضت 500 ريال من العميل محمد علي
            صرفت 85 ريال بنزين للسيارة من الصندوق الرئيسي
            سددت للمورد مؤسسة الأمل 1200 ريال دفعة
            حول 400 ريال من الصندوق الرئيسي إلى العهدة
            """.trimIndent()
        )
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Auto-switch tabs when items are parsed or summary arrives
    LaunchedEffect(batchItems.size) {
        if (batchItems.isNotEmpty() && selectedTabIndex == 0) {
            selectedTabIndex = 1
        }
    }

    LaunchedEffect(batchSummary) {
        if (batchSummary != null) {
            selectedTabIndex = 2
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isExecuting) {
                viewModel.clearBatchState()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("batch_commands_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنفيذ حزمة أوامر دفعة واحدة (Batch Execution)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أضف قائمة بالعمليات وسيقوم الوكيل بتحليلها وتنفيذها آلياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isExecuting) {
                                viewModel.clearBatchState()
                                onDismiss()
                            }
                        },
                        enabled = !isExecuting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("1. كتابة الأوامر", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("2. المعاينة (${batchItems.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        enabled = batchSummary != null,
                        text = { Text("3. التقرير النهائي", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Contents
                when (selectedTabIndex) {
                    0 -> BatchInputTab(
                        rawInputText = rawInputText,
                        onInputChanged = { rawInputText = it },
                        onPresetSelected = { preset ->
                            rawInputText = preset.commands.joinToString("\n")
                        },
                        onAnalyzeClick = {
                            viewModel.parseBatchCommands(rawInputText)
                        }
                    )

                    1 -> BatchPreviewTab(
                        items = batchItems,
                        isExecuting = isExecuting,
                        progress = progress,
                        currencySymbol = currencySymbol,
                        decimalFormat = decimalFormat,
                        onToggleItem = { viewModel.toggleBatchCommandItem(it) },
                        onDeleteItem = { viewModel.removeBatchCommandItem(it) },
                        onExecuteBatch = {
                            viewModel.executeBatchCommands()
                        },
                        onBackToInput = { selectedTabIndex = 0 }
                    )

                    2 -> BatchReportTab(
                        summary = batchSummary,
                        onDone = {
                            viewModel.clearBatchState()
                            onDismiss()
                        },
                        onCopyReport = { report ->
                            clipboardManager.setText(AnnotatedString(report))
                            viewModel.clearBatchState()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchInputTab(
    rawInputText: String,
    onInputChanged: (String) -> Unit,
    onPresetSelected: (com.example.domain.BatchPresetTemplate) -> Unit,
    onAnalyzeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "قوالب أوامر جاهزة وسريعة:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BatchCommandsEngine.PRESET_TEMPLATES.forEach { preset ->
                    AssistChip(
                        onClick = { onPresetSelected(preset) },
                        label = { Text(preset.title, fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "اكتب أو الصق قائمة العمليات (عملية في كل سطر):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = rawInputText,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("batch_commands_input_field"),
                placeholder = {
                    Text(
                        "مثال:\nقبضت 500 من أحمد سالم\nصرفت 60 بنزين\nسددت 1000 للمورد شركة التقنية\nقيد دين 300 على سامي\nحول 500 من الصندوق إلى العهدة",
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onAnalyzeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("analyze_batch_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = rawInputText.isNotBlank()
        ) {
            Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("تحليل ومعاينة الحزمة 🔍", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BatchPreviewTab(
    items: List<BatchCommandItem>,
    isExecuting: Boolean,
    progress: Pair<Int, Int>,
    currencySymbol: String,
    decimalFormat: DecimalFormat,
    onToggleItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onExecuteBatch: () -> Unit,
    onBackToInput: () -> Unit
) {
    val selectedCount = items.count { it.isSelected }
    val totalInflow = items.filter { it.isSelected }.sumOf {
        val txType = it.parsedIntent?.transactionType
        if (txType == TransactionType.CUSTOMER_RECEIPT || txType == TransactionType.INCOME) it.parsedIntent.amount else 0.0
    }
    val totalOutflow = items.filter { it.isSelected }.sumOf {
        val txType = it.parsedIntent?.transactionType
        if (txType == TransactionType.SUPPLIER_PAYMENT || txType == TransactionType.EXPENSE) it.parsedIntent.amount else 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Summary KPI Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المحدد: $selectedCount من أصل ${items.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (totalInflow > 0) {
                            Text(
                                text = "مقبوضات: +${decimalFormat.format(totalInflow)} $currencySymbol",
                                color = MoneyIncomeGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (totalOutflow > 0) {
                            Text(
                                text = "مدفوعات: -${decimalFormat.format(totalOutflow)} $currencySymbol",
                                color = MoneyExpenseRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isExecuting) {
                // Execution Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "جاري تنفيذ الحزمة بالوكيل المحاسبي...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "${progress.first} / ${progress.second}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val progressFraction = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // List of Command Cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    BatchItemCard(
                        item = item,
                        currencySymbol = currencySymbol,
                        decimalFormat = decimalFormat,
                        isExecuting = isExecuting,
                        onToggle = { onToggleItem(item.id) },
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBackToInput,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp),
                enabled = !isExecuting
            ) {
                Text("تعديل النص")
            }

            Button(
                onClick = onExecuteBatch,
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp)
                    .testTag("execute_batch_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isExecuting && selectedCount > 0
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري التنفيذ...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تنفيذ ($selectedCount) عمليات ⚡", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BatchItemCard(
    item: BatchCommandItem,
    currencySymbol: String,
    decimalFormat: DecimalFormat,
    isExecuting: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val intent = item.parsedIntent

    val badgeColor = when (intent?.transactionType) {
        TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> MoneyIncomeGreen
        TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> MoneyExpenseRed
        TransactionType.CUSTOMER_NEW_DEBIT -> Color(0xFF1E40AF)
        TransactionType.SUPPLIER_NEW_CREDIT -> Color(0xFFB45309)
        TransactionType.TRANSFER -> Color(0xFF0D9488)
        else -> MaterialTheme.colorScheme.primary
    }

    val actionLabel = when (intent?.transactionType) {
        TransactionType.CUSTOMER_RECEIPT -> "سند قبض عميل"
        TransactionType.SUPPLIER_PAYMENT -> "سند دفع مورد"
        TransactionType.EXPENSE -> "سند صرف مصروفات"
        TransactionType.INCOME -> "إيراد نقدي"
        TransactionType.CUSTOMER_NEW_DEBIT -> "قيد دين على عميل"
        TransactionType.SUPPLIER_NEW_CREDIT -> "التزام مالي لمورد"
        TransactionType.TRANSFER -> "تحويل بين الصناديق"
        else -> intent?.action ?: "أمر مالي"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (item.status == BatchCommandStatus.PROCESSING) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                enabled = !isExecuting
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Status Indicator
                    when (item.status) {
                        BatchCommandStatus.PENDING -> {
                            Text(
                                text = if (intent != null && intent.amount > 0) "${decimalFormat.format(intent.amount)} $currencySymbol" else "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = badgeColor
                            )
                        }
                        BatchCommandStatus.PROCESSING -> {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        }
                        BatchCommandStatus.SUCCESS -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MoneyIncomeGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("تم بنجاح", fontSize = 10.sp, color = MoneyIncomeGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        BatchCommandStatus.FAILED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("تعثر", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                        BatchCommandStatus.SKIPPED -> {
                            Text("مستبعد", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.rawText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Sub details if parsed
                if (intent != null) {
                    val details = mutableListOf<String>()
                    if (!intent.partyName.isNullOrBlank()) details.add("الطرف: ${intent.partyName}")
                    if (!intent.category.isNullOrBlank()) details.add("التصنيف: ${intent.category}")
                    if (!intent.cashBoxName.isNullOrBlank()) details.add("الصندوق: ${intent.cashBoxName}")
                    if (!intent.targetCashBoxName.isNullOrBlank()) details.add("إلى: ${intent.targetCashBoxName}")

                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" • "),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (!item.executionMessage.isNullOrBlank() && item.status == BatchCommandStatus.FAILED) {
                    Text(
                        text = "⚠️ ${item.executionMessage}",
                        fontSize = 10.sp,
                        color = Color.Red
                    )
                }
            }

            if (!isExecuting && item.status == BatchCommandStatus.PENDING) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchReportTab(
    summary: BatchExecutionSummary?,
    onDone: () -> Unit,
    onCopyReport: (String) -> Unit
) {
    if (summary == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Success Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (summary.failedCount == 0) MoneyIncomeGreen.copy(alpha = 0.12f) else Color(0xFFFEF3C7)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (summary.failedCount == 0) Icons.Default.CheckCircle else Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = if (summary.failedCount == 0) MoneyIncomeGreen else Color(0xFFD97706),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اكتمل تنفيذ الحزمة المحاسبية!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تم تنفيذ (${summary.successfulCount} من ${summary.totalCommands}) عملية وتوليد قيود اليومية المتوازنة لها بنجاح.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Report View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    item {
                        Text(
                            text = summary.executionReport,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onCopyReport(summary.executionReport) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("نسخ التقرير")
            }

            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تم وإغلاق", fontWeight = FontWeight.Bold)
            }
        }
    }
}
