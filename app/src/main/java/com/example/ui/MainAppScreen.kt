package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Party
import com.example.ui.components.AiMemoryDialog
import com.example.ui.components.AiSettingsDialog
import com.example.ui.components.BiometricSettingsDialog
import com.example.ui.components.BudgetsDialog
import com.example.ui.components.CurrenciesDialog
import com.example.ui.components.ExpenseCategoriesDialog
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.CashBoxesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.viewmodel.AccountingViewModel

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("الرئيسية", Icons.Default.Dashboard),
    PARTIES("الديون والجهات", Icons.Default.People),
    CASH_BOXES("الصناديق", Icons.Default.AccountBalanceWallet),
    TRANSACTIONS("الحركات", Icons.AutoMirrored.Filled.ReceiptLong),
    AI_CHAT("المحاسب الذكي", Icons.Default.AutoAwesome),
    REPORTS("التقارير", Icons.Default.Assessment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AccountingViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var selectedPartyForStatement by remember { mutableStateOf<Party?>(null) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showAiSettingsDialog by remember { mutableStateOf(false) }
    var showCurrenciesDialog by remember { mutableStateOf(false) }
    var showExpenseCategoriesDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAiMemoryDialog by remember { mutableStateOf(false) }
    var showBiometricSettingsDialog by remember { mutableStateOf(false) }
    var showReminderSettingsDialog by remember { mutableStateOf(false) }
    var showBudgetsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val feedback by viewModel.operationFeedback.collectAsStateWithLifecycle()

    LaunchedEffect(feedback) {
        feedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Hardware back button navigation
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    // Force RTL for Arabic accounting experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                Screen.DASHBOARD -> "محاسبي الذكي"
                                Screen.PARTIES -> "العملاء والموردين"
                                Screen.CASH_BOXES -> "الصناديق والخزائن"
                                Screen.TRANSACTIONS -> "كشف العمليات"
                                Screen.AI_CHAT -> "المساعد المحاسبي الذكي"
                                Screen.REPORTS -> "التقارير والتصدير"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { currentScreen = Screen.REPORTS },
                            modifier = Modifier.testTag("topbar_reports_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "التقارير",
                                tint = if (currentScreen == Screen.REPORTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextMode = when (currentThemeMode) {
                                    com.example.ui.theme.AppThemeMode.LIGHT -> com.example.ui.theme.AppThemeMode.DARK
                                    com.example.ui.theme.AppThemeMode.DARK -> com.example.ui.theme.AppThemeMode.SYSTEM
                                    com.example.ui.theme.AppThemeMode.SYSTEM -> com.example.ui.theme.AppThemeMode.LIGHT
                                }
                                viewModel.setThemeMode(nextMode)
                            },
                            modifier = Modifier.testTag("topbar_theme_quick_toggle")
                        ) {
                            val icon = when (currentThemeMode) {
                                com.example.ui.theme.AppThemeMode.LIGHT -> Icons.Default.LightMode
                                com.example.ui.theme.AppThemeMode.DARK -> Icons.Default.DarkMode
                                com.example.ui.theme.AppThemeMode.SYSTEM -> Icons.Default.Palette
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "تغيير المظهر",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showVoiceDialog = true },
                            modifier = Modifier.testTag("topbar_voice_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "تسجيل بالصوت",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.testTag("topbar_settings_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "قائمة الإعدادات",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🎨 مظهر التطبيق (فاتح / داكن / نظام)") },
                                onClick = {
                                    showTopMenu = false
                                    showThemeDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("💰 إعدادات العملات والعملة الافتراضية") },
                                onClick = {
                                    showTopMenu = false
                                    showCurrenciesDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("📂 إعداد بنود المصروفات (التصنيفات)") },
                                onClick = {
                                    showTopMenu = false
                                    showExpenseCategoriesDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("📊 الميزانيات التقديرية وتنبيهات الإنفاق") },
                                onClick = {
                                    showTopMenu = false
                                    showBudgetsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("💾 النسخ الاحتياطي والتصدير (النسخ التلقائي)") },
                                onClick = {
                                    showTopMenu = false
                                    showBackupDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("⏰ التذكيرات والإشعارات الدورية (المعاملات والديون)") },
                                onClick = {
                                    showTopMenu = false
                                    showReminderSettingsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🔐 الأمان وحماية الدفاتر (قفل البصمة/الوجه)") },
                                onClick = {
                                    showTopMenu = false
                                    showBiometricSettingsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🧠 ذاكرة المحاسب الذكي (الحقائق والقواعد)") },
                                onClick = {
                                    showTopMenu = false
                                    showAiMemoryDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = com.example.ui.theme.PrimaryGreen)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🤖 إعدادات الذكاء الاصطناعي (Gemini)") },
                                onClick = {
                                    showTopMenu = false
                                    showAiSettingsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val screens = listOf(
                        Screen.DASHBOARD,
                        Screen.TRANSACTIONS,
                        Screen.PARTIES,
                        Screen.CASH_BOXES,
                        Screen.AI_CHAT
                    )

                    screens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                selectedPartyForStatement = null
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding()
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAi = { currentScreen = Screen.AI_CHAT },
                        onNavigateToParties = { currentScreen = Screen.PARTIES },
                        onNavigateToCashBoxes = { currentScreen = Screen.CASH_BOXES },
                        onNavigateToTransactions = { currentScreen = Screen.TRANSACTIONS }
                    )
                    Screen.PARTIES -> PartiesScreen(
                        viewModel = viewModel,
                        onOpenStatement = { party ->
                            selectedPartyForStatement = party
                            currentScreen = Screen.REPORTS
                        }
                    )
                    Screen.CASH_BOXES -> CashBoxesScreen(viewModel = viewModel)
                    Screen.TRANSACTIONS -> TransactionsScreen(viewModel = viewModel)
                    Screen.AI_CHAT -> AiChatScreen(viewModel = viewModel)
                    Screen.REPORTS -> ReportsScreen(
                        viewModel = viewModel,
                        preselectedParty = selectedPartyForStatement
                    )
                }

                if (showVoiceDialog) {
                    VoiceTransactionDialog(
                        viewModel = viewModel,
                        onDismiss = { showVoiceDialog = false }
                    )
                }

                if (showCurrenciesDialog) {
                    CurrenciesDialog(
                        viewModel = viewModel,
                        onDismiss = { showCurrenciesDialog = false }
                    )
                }

                if (showExpenseCategoriesDialog) {
                    ExpenseCategoriesDialog(
                        viewModel = viewModel,
                        onDismiss = { showExpenseCategoriesDialog = false }
                    )
                }

                if (showAiSettingsDialog) {
                    AiSettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { showAiSettingsDialog = false }
                    )
                }

                if (showBackupDialog) {
                    com.example.ui.components.DatabaseBackupDialog(
                        viewModel = viewModel,
                        onDismiss = { showBackupDialog = false }
                    )
                }

                if (showAiMemoryDialog) {
                    AiMemoryDialog(
                        viewModel = viewModel,
                        onDismiss = { showAiMemoryDialog = false }
                    )
                }

                if (showBiometricSettingsDialog) {
                    BiometricSettingsDialog(
                        onDismiss = { showBiometricSettingsDialog = false }
                    )
                }

                if (showReminderSettingsDialog) {
                    ReminderSettingsDialog(
                        onDismiss = { showReminderSettingsDialog = false }
                    )
                }

                if (showBudgetsDialog) {
                    BudgetsDialog(
                        viewModel = viewModel,
                        onDismiss = { showBudgetsDialog = false }
                    )
                }

                if (showThemeDialog) {
                    com.example.ui.components.ThemeSettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { showThemeDialog = false }
                    )
                }
            }
        }
    }
}
