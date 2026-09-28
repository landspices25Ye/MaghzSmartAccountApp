package com.example.ui.components

import android.content.Context
import android.net.Uri
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.backup.BackupFileInfo
import com.example.data.backup.LocalDatabaseBackupManager
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DatabaseBackupDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isAutoBackupEnabled by remember { mutableStateOf(LocalDatabaseBackupManager.isAutoBackupEnabled(context)) }
    var backupsList by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var externalUriToRestore by remember { mutableStateOf<Uri?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupFileInfo?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("ar")) }
    val sizeFormat = remember { DecimalFormat("#,##0.0") }

    fun refreshBackups() {
        scope.launch {
            isLoading = true
            backupsList = LocalDatabaseBackupManager.listAvailableBackups(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshBackups()
    }

    // External file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            externalUriToRestore = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("النسخ الاحتياطي والأمان", fontWeight = FontWeight.Bold)
                    Text("حفظ واستعادة بيانات Room محلياً", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Auto Backup Switch Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isAutoBackupEnabled) MoneyIncomeGreen else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "النسخ التلقائي الدوري",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "حفظ نسخة تلقائية عند التغييرات لتجنب فقدان البيانات",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = {
                                isAutoBackupEnabled = it
                                LocalDatabaseBackupManager.setAutoBackupEnabled(context, it)
                                if (it) {
                                    scope.launch {
                                        LocalDatabaseBackupManager.performBackup(context, viewModel.repository, isAuto = true)
                                        refreshBackups()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("auto_backup_toggle_switch")
                        )
                    }
                }

                // Action Buttons: Create manual backup & Restore from file
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                LocalDatabaseBackupManager.performBackup(context, viewModel.repository, isAuto = false)
                                Toast.makeText(context, "تم إنشاء النسخة الاحتياطية بنجاح ✓", Toast.LENGTH_SHORT).show()
                                refreshBackups()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_manual_backup_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ احتياطي الآن", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("application/json") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_file_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استيراد من ملف", fontSize = 11.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // List of Backups
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "النسخ المتوفرة محلياً (${backupsList.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }

                if (backupsList.isEmpty() && !isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("لا توجد نسخ احتياطية مسجلة بعد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("اضغط 'نسخ احتياطي الآن' لحفظ بياناتك فوراً", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(backupsList, key = { it.filename }) { backup ->
                            BackupCardItem(
                                backup = backup,
                                dateFormat = dateFormat,
                                sizeFormat = sizeFormat,
                                onRestore = { backupToRestore = backup },
                                onShare = { LocalDatabaseBackupManager.shareBackupFile(context, backup.file) },
                                onDelete = { backupToDelete = backup }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("تم")
            }
        }
    )

    // Restore confirmation dialog for local file
    if (backupToRestore != null) {
        val b = backupToRestore!!
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = {
                Text("تأكيد استعادة البيانات", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("هل أنت متأكد من استعادة النسخة الاحتياطية بتاريخ:")
                    Text(dateFormat.format(Date(b.timestamp)), fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    Text("تتضمن: ${b.transactionCount} حركة، ${b.partyCount} طرف، ${b.cashBoxCount} صندوق.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "تنبيه: سيتم استبدال البيانات الحالية بالبيانات الموجودة في النسخة الاحتياطية.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = LocalDatabaseBackupManager.restoreBackup(context, viewModel.repository, b.file)
                            if (result.isSuccess) {
                                Toast.makeText(context, result.getOrNull() ?: "تمت الاستعادة بنجاح ✓", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.localizedMessage ?: "فشل الاستعادة", Toast.LENGTH_LONG).show()
                            }
                            backupToRestore = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("نعم، استعد البيانات")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { backupToRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Restore confirmation dialog for external picked file
    if (externalUriToRestore != null) {
        val uri = externalUriToRestore!!
        AlertDialog(
            onDismissRequest = { externalUriToRestore = null },
            title = {
                Text("تأكيد استيراد واستعادة ملف النسخة", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("هل ترغب في استعادة قاعدة البيانات من الملف المختار؟ سيتم استبدال الحسابات الحالية بما يحتويه الملف.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = LocalDatabaseBackupManager.restoreBackupFromUri(context, viewModel.repository, uri)
                            if (result.isSuccess) {
                                Toast.makeText(context, result.getOrNull() ?: "تمت الاستعادة بنجاح ✓", Toast.LENGTH_LONG).show()
                                refreshBackups()
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.localizedMessage ?: "فشل الاستعادة", Toast.LENGTH_LONG).show()
                            }
                            externalUriToRestore = null
                        }
                    }
                ) {
                    Text("استعادة الآن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { externalUriToRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (backupToDelete != null) {
        val b = backupToDelete!!
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            title = { Text("حذف النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف ملف النسخة الاحتياطية (${b.filename})؟") },
            confirmButton = {
                Button(
                    onClick = {
                        LocalDatabaseBackupManager.deleteBackup(b.file)
                        backupToDelete = null
                        refreshBackups()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { backupToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun BackupCardItem(
    backup: BackupFileInfo,
    dateFormat: SimpleDateFormat,
    sizeFormat: DecimalFormat,
    onRestore: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (backup.isAuto) PrimaryGreen.copy(alpha = 0.12f) else Color(0xFF1976D2).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (backup.isAuto) Icons.Default.CloudDownload else Icons.Default.Save,
                    contentDescription = null,
                    tint = if (backup.isAuto) PrimaryGreen else Color(0xFF1976D2),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(backup.timestamp)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = if (backup.isAuto) PrimaryGreen.copy(alpha = 0.15f) else Color(0xFF1976D2).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (backup.isAuto) "تلقائي" else "يدوي",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (backup.isAuto) PrimaryGreen else Color(0xFF1976D2),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "${backup.transactionCount} حركة • ${backup.partyCount} طرف • ${sizeFormat.format(backup.sizeBytes / 1024.0)} KB",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("استعادة", fontSize = 10.sp)
                }

                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
