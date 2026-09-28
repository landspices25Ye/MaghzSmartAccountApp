package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.BudgetsDialog
import com.example.ui.components.DailyOverviewSection
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.components.TransactionCardItem
import com.example.ui.components.TransferCashDialog
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.theme.MoneyDebtBlue
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    onNavigateToAi: () -> Unit,
    onNavigateToParties: () -> Unit,
    onNavigateToCashBoxes: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val aiMemories by viewModel.aiMemories.collectAsStateWithLifecycle()
    val activeMemoriesCount by viewModel.activeMemoriesCount.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showBudgetsDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(TransactionType.CUSTOMER_RECEIPT) }
    var selectedCategoryQuickFilter by remember { mutableStateOf<String?>(null) }

    val currencyFormat = remember { DecimalFormat("#,##0.##") }
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val todayDateFormat = remember { SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")) }

    // Ratio of Assets vs Liabilities
    val totalAssets = summary.totalCash + summary.owedToMe
    val totalLiabilities = summary.owedByMe
    val financialHealthRatio = if (totalAssets + totalLiabilities > 0) {
        (totalAssets / (totalAssets + totalLiabilities)).toFloat().coerceIn(0f, 1f)
    } else 1f

    val displayedRecentTx = remember(recentTransactions, selectedCategoryQuickFilter) {
        if (selectedCategoryQuickFilter == null) recentTransactions
        else recentTransactions.filter { it.category == selectedCategoryQuickFilter || it.type.titleAr == selectedCategoryQuickFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern Visual Hero Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onNavigateToAi() }
                    .testTag("ai_assistant_hero_banner"),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dashboard_hero_1790549054953),
                        contentDescription = "المحاسب الذكي",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Scrim gradient for text legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xEE093A24),
                                        Color(0xAA0D5C3A),
                                        Color(0x33000000)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0x3300C853),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "المحاسب الذكي 🤖",
                                        color = Color(0xFFB9F6CA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini AI",
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "تحدث أو اكتب عملياتك اليومية",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Text(
                                text = "يتنبأ ببند المصروف والصندوق الافتراضي تلقائياً",
                                color = Color(0xEEFFFFFF),
                                fontSize = 11.sp
                            )
                        }

                        // Voice Mic circular action button
                        IconButton(
                            onClick = { showVoiceDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853))
                                .shadow(6.dp, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "تسجيل بالصوت",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // AI Memory Quick Status Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showMemoryDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ذاكرة المحاسب",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = "$activeMemoriesCount قواعد محفوظة",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showReminderDialog = true }
                        .testTag("dashboard_reminders_banner_btn"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "التذكيرات والإشعارات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "جدولة تنبيهات المعاملات والديون",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modern Glassmorphic Financial Overview Card
        item {
            val categoriesWithBudget = expenseCategories.filter { it.monthlyBudget > 0 }
            val totalBudget = categoriesWithBudget.sumOf { it.monthlyBudget }

            val startOfCurrentMonth = remember {
                java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val monthlyExpensesByCategory = remember(allTransactions, startOfCurrentMonth) {
                allTransactions
                    .filter { it.type == TransactionType.EXPENSE && it.timestamp >= startOfCurrentMonth }
                    .groupBy { it.category.trim().lowercase() }
                    .mapValues { entry -> entry.value.sumOf { tx -> tx.amount } }
            }

            val totalSpentOnBudgeted = categoriesWithBudget.sumOf { cat ->
                monthlyExpensesByCategory[cat.name.trim().lowercase()] ?: 0.0
            }

            val overdueCategoriesCount = categoriesWithBudget.count { cat ->
                val spent = monthlyExpensesByCategory[cat.name.trim().lowercase()] ?: 0.0
                spent > cat.monthlyBudget
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showBudgetsDialog = true }
                    .testTag("dashboard_budget_summary_card"),
                shape = RoundedCornerShape(18.dp),
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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "الميزانية الشهرية والإنفاق",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (totalBudget > 0) "${categoriesWithBudget.size} فئات محددة" else "انقر لتعيين ميزانيات الفئات وتلقي تنبيهات التجاوز",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (overdueCategoriesCount > 0) {
                            Surface(
                                color = MoneyExpenseRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MoneyExpenseRed, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🚨 $overdueCategoriesCount فئات تجاوزت", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MoneyExpenseRed)
                                }
                            }
                        } else {
                            TextButton(onClick = { showBudgetsDialog = true }) {
                                Text("إدارة الميزانية", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (totalBudget > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val ratio = (totalSpentOnBudgeted / totalBudget).toFloat().coerceIn(0f, 1f)
                        val percent = (totalSpentOnBudgeted / totalBudget * 100).toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "المستهلك: ${currencyFormat.format(totalSpentOnBudgeted)} / ${currencyFormat.format(totalBudget)} ${summary.currencySymbol}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$percent%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (percent > 100) MoneyExpenseRed else PrimaryGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (percent > 100) MoneyExpenseRed else PrimaryGreen,
                            trackColor = PrimaryGreen.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }

        // Modern Glassmorphic Financial Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryGreen.copy(alpha = 0.09f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "صافي الموقف المالي (صافي الثروة)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = todayDateFormat.format(Date()),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                            )
                        }

                        Surface(
                            color = if (summary.netWorth >= 0) MoneyIncomeGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (summary.netWorth >= 0) "موقف إيجابي ممتاز ✓" else "التزامات مستحقة !",
                                fontSize = 11.sp,
                                color = if (summary.netWorth >= 0) MoneyIncomeGreen else Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${currencyFormat.format(summary.netWorth)} ${summary.currencySymbol}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (summary.netWorth >= 0) PrimaryGreen else Color.Red
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress balance bar (Assets vs Liabilities)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("نسبة الأصول النقدية والديون:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text("${(financialHealthRatio * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { financialHealthRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryGreen,
                            trackColor = MoneyExpenseRed.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Three Pillar Mini Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pillar 1: Cash in Boxes
                        ElevatedCard(
                            onClick = onNavigateToCashBoxes,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("النقدية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${currencyFormat.format(summary.totalCash)} ${summary.currencySymbol}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Text("في ${cashBoxes.size} صناديق", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Pillar 2: Owed to me (ما لي عنده)
                        ElevatedCard(
                            onClick = onNavigateToParties,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MoneyIncomeGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = MoneyIncomeGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ما لي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${currencyFormat.format(summary.owedToMe)} ${summary.currencySymbol}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyIncomeGreen
                                )
                                Text("ديون العملاء", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Pillar 3: Owed by me (ما علي له)
                        ElevatedCard(
                            onClick = onNavigateToParties,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MoneyExpenseRed.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = MoneyExpenseRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ما علي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${currencyFormat.format(summary.owedByMe)} ${summary.currencySymbol}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyExpenseRed
                                )
                                Text("التزامات الموردين", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        // Daily Analytics & Charts Section
        item {
            DailyOverviewSection(
                transactions = allTransactions
            )
        }

        // Quick Entry Grid Actions
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تسجيل العمليات السريعة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showVoiceDialog = true }) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بالصوت", fontSize = 12.sp, color = PrimaryGreen)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionModernItem(
                        title = "قبض من عميل",
                        subtitle = "+ زيادة نقدية",
                        icon = Icons.Default.ArrowDownward,
                        color = MoneyIncomeGreen,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            dialogInitialType = TransactionType.CUSTOMER_RECEIPT
                            showAddDialog = true
                        }
                    )

                    QuickActionModernItem(
                        title = "دفع لمورد",
                        subtitle = "- صرف نقدية",
                        icon = Icons.Default.ArrowUpward,
                        color = MoneyExpenseRed,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            dialogInitialType = TransactionType.SUPPLIER_PAYMENT
                            showAddDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionModernItem(
                        title = "دين على عميل",
                        subtitle = "ما لي عنده",
                        icon = Icons.Default.People,
                        color = MoneyDebtBlue,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            dialogInitialType = TransactionType.CUSTOMER_NEW_DEBIT
                            showAddDialog = true
                        }
                    )

                    QuickActionModernItem(
                        title = "مصروف تشغيلي",
                        subtitle = "بنزين، فواتير...",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        color = MoneyGold,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            dialogInitialType = TransactionType.EXPENSE
                            showAddDialog = true
                        }
                    )

                    QuickActionModernItem(
                        title = "تحويل صناديق",
                        subtitle = "نقل رصيد",
                        icon = Icons.Default.SwapHoriz,
                        color = Color(0xFF00897B),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTransferDialog = true
                        }
                    )
                }
            }
        }

        // Recent Transactions Section Header & Category Filters
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر الحركات المسجلة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("عرض وسجل كامل", fontSize = 12.sp)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Quick Filter Chips
                if (expenseCategories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryQuickFilter == null,
                                onClick = { selectedCategoryQuickFilter = null },
                                label = { Text("الكل", fontSize = 11.sp) }
                            )
                        }
                        items(expenseCategories.take(6)) { cat ->
                            FilterChip(
                                selected = selectedCategoryQuickFilter == cat.name,
                                onClick = {
                                    selectedCategoryQuickFilter = if (selectedCategoryQuickFilter == cat.name) null else cat.name
                                },
                                label = { Text(cat.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions List
        if (displayedRecentTx.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("لا توجد حركات مسجلة حالياً", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "ابدأ بتسجيل مقبوضاتك ومصروفاتك بالصوت أو الأزرار السريعة بسهولة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(displayedRecentTx, key = { it.id }) { tx ->
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

    if (showAddDialog) {
        AddTransactionDialog(
            cashBoxes = cashBoxes,
            parties = parties,
            categories = expenseCategories,
            currencies = currencies,
            defaultCurrencySymbol = summary.currencySymbol,
            initialType = dialogInitialType,
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

    if (showTransferDialog) {
        TransferCashDialog(
            cashBoxes = cashBoxes,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amt, note ->
                viewModel.transferBetweenBoxes(fromId, toId, amt, note)
                showTransferDialog = false
            }
        )
    }

    if (showVoiceDialog) {
        VoiceTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false }
        )
    }

    if (showMemoryDialog) {
        com.example.ui.components.AiMemoryDialog(
            viewModel = viewModel,
            onDismiss = { showMemoryDialog = false }
        )
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            onDismiss = { showReminderDialog = false }
        )
    }

    if (showBudgetsDialog) {
        BudgetsDialog(
            viewModel = viewModel,
            onDismiss = { showBudgetsDialog = false }
        )
    }
}

@Composable
fun QuickActionModernItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
        }
    }
}
