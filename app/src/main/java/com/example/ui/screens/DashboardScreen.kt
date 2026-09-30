package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AiMemoryDialog
import com.example.ui.components.BudgetsDialog
import com.example.ui.components.DailyOverviewSection
import com.example.ui.components.TransactionDetailsDialog
import com.example.ui.components.TransferCashDialog
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.components.dashboard.BudgetOverviewSection
import com.example.ui.components.dashboard.DashboardHeroBanner
import com.example.ui.components.dashboard.DashboardSummaryCards
import com.example.ui.components.dashboard.FinancialHealthCard
import com.example.ui.components.dashboard.FinancialInsightsCard
import com.example.ui.components.dashboard.QuickActionsGrid
import com.example.ui.components.dashboard.RecentTransactionsSection
import com.example.ui.viewmodel.AccountingViewModel

@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    onNavigateToAi: () -> Unit,
    onNavigateToParties: () -> Unit,
    onNavigateToCashBoxes: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val financialHealth by viewModel.financialHealth.collectAsStateWithLifecycle()
    val cashRunway by viewModel.cashRunway.collectAsStateWithLifecycle()
    val smartTip by viewModel.smartFinancialTip.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val activeMemoriesCount by viewModel.activeMemoriesCount.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showBudgetsDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(TransactionType.CUSTOMER_RECEIPT) }
    var selectedTxForDetails by remember { mutableStateOf<TransactionRecord?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner with AI & Mic
        item {
            DashboardHeroBanner(
                activeMemoriesCount = activeMemoriesCount,
                onNavigateToAi = onNavigateToAi,
                onVoiceClick = { showVoiceDialog = true },
                onMemoryClick = { showMemoryDialog = true }
            )
        }

        // 2. Financial Summary Cards (Net Worth, Cash, Receivables, Payables)
        item {
            DashboardSummaryCards(
                summary = summary,
                onNavigateToCashBoxes = onNavigateToCashBoxes,
                onNavigateToParties = onNavigateToParties
            )
        }

        // 3. Financial Health Meter (0-100 Score & Assets/Liabilities)
        item {
            FinancialHealthCard(
                financialHealth = financialHealth
            )
        }

        // 4. Financial Insights (Runway & Smart AI Advisory Tip)
        item {
            FinancialInsightsCard(
                cashRunway = cashRunway,
                smartTip = smartTip,
                currencySymbol = summary.currencySymbol
            )
        }

        // 5. Daily Cash Flow Overview & Analytics
        item {
            DailyOverviewSection(
                transactions = allTransactions,
                currencySymbol = summary.currencySymbol
            )
        }

        // 6. Fast Quick Actions Grid (6 Actions)
        item {
            QuickActionsGrid(
                onActionClick = { type ->
                    dialogInitialType = type
                    showAddDialog = true
                },
                onTransferClick = { showTransferDialog = true },
                onVoiceClick = { showVoiceDialog = true }
            )
        }

        // 7. Monthly Budgets Overview
        item {
            BudgetOverviewSection(
                expenseCategories = expenseCategories,
                allTransactions = allTransactions,
                currencySymbol = summary.currencySymbol,
                onOpenBudgetsDialog = { showBudgetsDialog = true }
            )
        }

        // 8. Recent Transactions with Fast Filters
        item {
            RecentTransactionsSection(
                recentTransactions = recentTransactions,
                parties = parties,
                cashBoxes = cashBoxes,
                defaultCurrencySymbol = summary.currencySymbol,
                onNavigateToTransactions = onNavigateToTransactions,
                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                onTransactionClick = { tx -> selectedTxForDetails = tx }
            )
        }
    }

    // Dialogs
    selectedTxForDetails?.let { tx ->
        val party = parties.firstOrNull { it.id == tx.partyId }
        val box = cashBoxes.firstOrNull { it.id == tx.cashBoxId }
        TransactionDetailsDialog(
            transaction = tx,
            party = party,
            cashBox = box,
            currencySymbol = summary.currencySymbol,
            onDelete = { viewModel.deleteTransaction(tx) },
            onDismiss = { selectedTxForDetails = null }
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            initialType = dialogInitialType,
            onDismiss = { showAddDialog = false }
        )
    }

    if (showVoiceDialog) {
        VoiceTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false }
        )
    }

    if (showTransferDialog) {
        TransferCashDialog(
            viewModel = viewModel,
            onDismiss = { showTransferDialog = false }
        )
    }

    if (showMemoryDialog) {
        AiMemoryDialog(
            viewModel = viewModel,
            onDismiss = { showMemoryDialog = false }
        )
    }

    if (showBudgetsDialog) {
        BudgetsDialog(
            viewModel = viewModel,
            onDismiss = { showBudgetsDialog = false }
        )
    }
}
