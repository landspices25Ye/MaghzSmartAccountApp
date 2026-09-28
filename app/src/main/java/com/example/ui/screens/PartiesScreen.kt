package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionType
import com.example.ui.components.AddPartyDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.EditPartyDialog
import com.example.ui.theme.MoneyDebtBlue
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.DecimalFormat

@Composable
fun PartiesScreen(
    viewModel: AccountingViewModel,
    onOpenStatement: (Party) -> Unit
) {
    val context = LocalContext.current
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Customers, 1: Suppliers
    var searchQuery by remember { mutableStateOf("") }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var partyToEdit by remember { mutableStateOf<Party?>(null) }

    var partyForTransaction by remember { mutableStateOf<Party?>(null) }
    var showTxDialog by remember { mutableStateOf(false) }

    val currentType = if (selectedTabIndex == 0) PartyType.CUSTOMER else PartyType.SUPPLIER
    val filteredParties = remember(parties, selectedTabIndex, searchQuery) {
        parties.filter { it.type == currentType }
            .filter {
                searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
    }

    val totalBalance = remember(filteredParties) {
        filteredParties.sumOf { it.balance }
    }

    val currencyFormat = remember { DecimalFormat("#,##0.##") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPartyDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_party_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (currentType == PartyType.CUSTOMER) "إضافة عميل" else "إضافة مورد"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Modern Segmented Tab Row
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                "العملاء (ما لي عندهم)",
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                "الموردين (ما علي لهم)",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }
            }

            // Total summary bar with modern gradient styling
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (currentType == PartyType.CUSTOMER) MoneyIncomeGreen.copy(alpha = 0.08f) else MoneyExpenseRed.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (currentType == PartyType.CUSTOMER) MoneyIncomeGreen.copy(alpha = 0.15f) else MoneyExpenseRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentType == PartyType.CUSTOMER) Icons.Default.Person else Icons.Default.Store,
                                contentDescription = null,
                                tint = if (currentType == PartyType.CUSTOMER) MoneyIncomeGreen else MoneyExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentType == PartyType.CUSTOMER) "إجمالي مستحقاتك عند العملاء:" else "إجمالي التزاماتك للموردين:",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${filteredParties.size} ${if (currentType == PartyType.CUSTOMER) "عملاء مسجلين" else "موردين مسجلين"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Text(
                        text = "${currencyFormat.format(totalBalance)} $currencySymbol",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (currentType == PartyType.CUSTOMER) MoneyIncomeGreen else MoneyExpenseRed,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("بحث باسم العميل، المورد، أو الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = PrimaryGreen) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // List of Parties
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredParties.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (currentType == PartyType.CUSTOMER) Icons.Default.Person else Icons.Default.Store,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (currentType == PartyType.CUSTOMER) "لا يوجد عملاء مسجلين" else "لا يوجد موردين مسجلين",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "اضغط على زر (+) لإضافة حساب جديد بسهولة وتوثيق الحسابات",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(filteredParties, key = { it.id }) { party ->
                        ModernPartyCardItem(
                            party = party,
                            currencySymbol = currencySymbol,
                            currencyFormat = currencyFormat,
                            onCall = {
                                if (party.phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}"))
                                    context.startActivity(intent)
                                }
                            },
                            onWhatsApp = {
                                if (party.phone.isNotBlank()) {
                                    try {
                                        val cleanPhone = party.phone.replace(Regex("[^0-9+]"), "")
                                        val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            onNewTransaction = {
                                partyForTransaction = party
                                showTxDialog = true
                            },
                            onOpenStatement = { onOpenStatement(party) },
                            onEdit = { partyToEdit = party },
                            onDelete = { viewModel.deleteParty(party) }
                        )
                    }
                }
            }
        }
    }

    if (showAddPartyDialog) {
        AddPartyDialog(
            initialType = currentType,
            onDismiss = { showAddPartyDialog = false },
            onConfirm = { name, type, phone, initialBalance, notes ->
                viewModel.addParty(name, type, phone, initialBalance, notes)
                showAddPartyDialog = false
            }
        )
    }

    if (partyToEdit != null) {
        EditPartyDialog(
            party = partyToEdit!!,
            onDismiss = { partyToEdit = null },
            onConfirm = { newName, newPhone, newNotes, adjBal ->
                viewModel.updateParty(partyToEdit!!, newName, newPhone, newNotes, adjBal)
                partyToEdit = null
            }
        )
    }

    if (showTxDialog && partyForTransaction != null) {
        val p = partyForTransaction!!
        val defaultType = if (p.type == PartyType.CUSTOMER) TransactionType.CUSTOMER_RECEIPT else TransactionType.SUPPLIER_PAYMENT
        AddTransactionDialog(
            cashBoxes = cashBoxes,
            parties = parties,
            categories = expenseCategories,
            currencies = currencies,
            defaultCurrencySymbol = currencySymbol,
            initialType = defaultType,
            initialPartyId = p.id,
            onDismiss = {
                showTxDialog = false
                partyForTransaction = null
            },
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
                showTxDialog = false
                partyForTransaction = null
            }
        )
    }
}

@Composable
fun ModernPartyCardItem(
    party: Party,
    currencySymbol: String,
    currencyFormat: DecimalFormat,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onNewTransaction: () -> Unit,
    onOpenStatement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCustomer = party.type == PartyType.CUSTOMER
    val balanceColor = if (isCustomer) {
        if (party.balance > 0) MoneyIncomeGreen else Color.Gray
    } else {
        if (party.balance > 0) MoneyExpenseRed else Color.Gray
    }

    val initials = party.name.trim().take(1).ifBlank { if (isCustomer) "ع" else "م" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initials Circle Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(balanceColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.ExtraBold,
                        color = balanceColor,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = party.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (party.phone.isNotBlank()) {
                        Text(
                            text = party.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${currencyFormat.format(party.balance)} $currencySymbol",
                        fontWeight = FontWeight.ExtraBold,
                        color = balanceColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Surface(
                        color = balanceColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isCustomer) "ما لك عنده" else "ما عليك له",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            if (party.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = party.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onNewTransaction,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isCustomer) "+ قبض / دين" else "- صرف / دين", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenStatement,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("كشف حساب", fontSize = 11.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                    }
                    if (party.phone.isNotBlank()) {
                        IconButton(onClick = onCall, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = MoneyIncomeGreen, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onWhatsApp, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
