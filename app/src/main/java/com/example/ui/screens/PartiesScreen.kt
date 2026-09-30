package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.parties.PartyCardItem
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
    val parties by viewModel.parties.collectAsStateWithLifecycle()
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

    val formatter = remember { DecimalFormat("#,##0.##") }

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
            // Segmented Tabs
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("العملاء (ما لنا عندهم)", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("parties_tab_customers")
                    )

                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("الموردين (ما علينا لهم)", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("parties_tab_suppliers")
                    )
                }
            }

            // Summary Header Banner for Selected Tab
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentType == PartyType.CUSTOMER) PrimaryGreen.copy(alpha = 0.08f) else MoneyDebtBlue.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (currentType == PartyType.CUSTOMER) "إجمالي ديون العملاء المستحقة لنا" else "إجمالي التزامات الموردين المستحقة علينا",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatter.format(totalBalance)} $currencySymbol",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (currentType == PartyType.CUSTOMER) MoneyIncomeGreen else MoneyExpenseRed
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${filteredParties.size} جهة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("parties_search_input"),
                placeholder = { Text("بحث بالاسم أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Parties List
            if (filteredParties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "لم يتم العثور على نتائج للبحث" else if (currentType == PartyType.CUSTOMER) "لا يوجد عملاء مضافين بعد" else "لا يوجد موردين مضافين بعد",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "اضغط على زر (+) في الأسفل لإضافة طرف جديد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredParties, key = { it.id }) { party ->
                        PartyCardItem(
                            party = party,
                            currencySymbol = currencySymbol,
                            onOpenStatement = onOpenStatement,
                            onEdit = { partyToEdit = it },
                            onDelete = { viewModel.deleteParty(it) },
                            onQuickPayment = {
                                partyForTransaction = it
                                showTxDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddPartyDialog) {
        AddPartyDialog(
            initialType = currentType,
            currencySymbol = currencySymbol,
            onDismiss = { showAddPartyDialog = false },
            onConfirm = { name, type, phone, balance, notes ->
                viewModel.addParty(name, type, phone, balance, notes)
                showAddPartyDialog = false
            }
        )
    }

    partyToEdit?.let { party ->
        EditPartyDialog(
            party = party,
            onDismiss = { partyToEdit = null },
            onConfirm = { newName, newPhone, newNotes, adjBalance ->
                viewModel.updateParty(party, newName, newPhone, newNotes, adjBalance)
                partyToEdit = null
            }
        )
    }

    if (showTxDialog && partyForTransaction != null) {
        val targetParty = partyForTransaction!!
        val isCustomer = targetParty.type == PartyType.CUSTOMER
        val txType = if (isCustomer) TransactionType.CUSTOMER_RECEIPT else TransactionType.SUPPLIER_PAYMENT
        AddTransactionDialog(
            viewModel = viewModel,
            initialType = txType,
            initialPartyId = targetParty.id,
            onDismiss = {
                showTxDialog = false
                partyForTransaction = null
            }
        )
    }
}
