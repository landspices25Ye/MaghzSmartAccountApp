package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReportDialog(
    parties: List<Party>,
    cashBoxes: List<CashBox>,
    onSelectPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val customers = remember(parties) { parties.filter { it.type == PartyType.CUSTOMER } }
    val suppliers = remember(parties) { parties.filter { it.type == PartyType.SUPPLIER } }

    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull { it.isDefault } ?: cashBoxes.firstOrNull()) }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }
    var boxDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "طلب تقرير عبر الوكيل الذكي 📊",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر نوع التقرير المالي الذي تريده وسيقوم المحاسب بإعداده وتحليله وتجهيز ملفات الـ PDF والإكسل فوراً:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                // 1. Daily Summary
                ReportOptionCard(
                    icon = Icons.Default.Today,
                    title = "تقرير ملخص حركة اليوم (Daily Summary)",
                    subtitle = "كشف المقبوضات والمدفوعات والمصروفات وصافي حركة اليوم",
                    onClick = {
                        onSelectPrompt("اعمل لي تقرير حركة اليوم مع تصدير PDF وإكسل")
                        onDismiss()
                    }
                )

                // 2. Customer Statement
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("كشف حساب عميل - ما لي (Customer Statement)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (customers.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = customerDropdownExpanded,
                                onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCustomer?.name ?: "اختر العميل...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = customerDropdownExpanded,
                                    onDismissRequest = { customerDropdownExpanded = false }
                                ) {
                                    customers.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text("${c.name} (${c.balance} ر.س)", fontSize = 12.sp) },
                                            onClick = {
                                                selectedCustomer = c
                                                customerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val name = selectedCustomer?.name ?: "العميل"
                                    onSelectPrompt("كشف حساب العميل $name وتصدير PDF وإكسل")
                                    onDismiss()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("إعداد كشف ${selectedCustomer?.name ?: ""} الآن ←", fontSize = 11.sp, color = PrimaryGreen)
                            }
                        } else {
                            Text("لا يوجد عملاء مسجلون بعد.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // 3. Supplier Statement
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("كشف حساب مورد - ما علي (Supplier Statement)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (suppliers.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = supplierDropdownExpanded,
                                onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedSupplier?.name ?: "اختر المورد...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = supplierDropdownExpanded,
                                    onDismissRequest = { supplierDropdownExpanded = false }
                                ) {
                                    suppliers.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text("${s.name} (${s.balance} ر.س)", fontSize = 12.sp) },
                                            onClick = {
                                                selectedSupplier = s
                                                supplierDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val name = selectedSupplier?.name ?: "المورد"
                                    onSelectPrompt("كشف حساب المورد $name وتصدير PDF وإكسل")
                                    onDismiss()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("إعداد كشف ${selectedSupplier?.name ?: ""} الآن ←", fontSize = 11.sp, color = PrimaryGreen)
                            }
                        } else {
                            Text("لا يوجد موردون مسجلون بعد.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // 4. Cash In Hand
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("كشف الصندوق والنقدية (Cash in Hand)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (cashBoxes.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = boxDropdownExpanded,
                                onExpandedChange = { boxDropdownExpanded = !boxDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedBox?.name ?: "اختر الصندوق...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boxDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = boxDropdownExpanded,
                                    onDismissRequest = { boxDropdownExpanded = false }
                                ) {
                                    cashBoxes.forEach { b ->
                                        DropdownMenuItem(
                                            text = { Text("${b.name} (${b.balance} ر.س)", fontSize = 12.sp) },
                                            onClick = {
                                                selectedBox = b
                                                boxDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val name = selectedBox?.name ?: "الصندوق الرئيسي"
                                    onSelectPrompt("كشف نقدية $name وتصدير PDF وإكسل")
                                    onDismiss()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("إعداد كشف ${selectedBox?.name ?: ""} الآن ←", fontSize = 11.sp, color = PrimaryGreen)
                            }
                        }
                    }
                }

                // 5. General Summary Report
                ReportOptionCard(
                    icon = Icons.Default.Assessment,
                    title = "التقرير المالي العام الشامل",
                    subtitle = "ملخص السيولة، إجمالي ما لك عند العملاء، إجمالي ما عليك للموردين، وصافي الموقف المالي",
                    onClick = {
                        onSelectPrompt("اعمل لي التقرير المالي العام الشامل مع تصدير PDF وإكسل")
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun ReportOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 14.sp
                )
            }
            Text("طلب ←", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        }
    }
}
