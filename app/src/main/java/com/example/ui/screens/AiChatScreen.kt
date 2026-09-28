package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.export.ExcelExportHelper
import com.example.export.PdfExportHelper
import com.example.ui.components.AiMemoryDialog
import com.example.ui.components.AiSettingsDialog
import com.example.ui.components.QuickReportDialog
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.ChatReportAction
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AiChatScreen(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val selectedModel by viewModel.aiPreferences.selectedModel.collectAsStateWithLifecycle()
    val useCustomKey by viewModel.aiPreferences.useCustomKey.collectAsStateWithLifecycle()
    val activeMemoriesCount by viewModel.activeMemoriesCount.collectAsStateWithLifecycle()

    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val partyMap = remember(parties) { parties.associateBy { it.id } }
    val boxMap = remember(cashBoxes) { cashBoxes.associateBy { it.id } }

    var inputText by remember { mutableStateOf("") }
    var showAiSettings by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showQuickReportDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Export Handlers
    fun handleExportPdf(action: ChatReportAction) {
        if (activity == null) {
            Toast.makeText(context, "الطباعة وتصدير PDF غير متاحين في هذا المشغل", Toast.LENGTH_SHORT).show()
            return
        }

        when (action.reportType) {
            "PARTY" -> {
                val party = action.partyId?.let { partyMap[it] }
                    ?: parties.firstOrNull { it.name.contains(action.partyName ?: "") }
                if (party != null) {
                    val partyTx = transactions.filter { it.partyId == party.id }
                    PdfExportHelper.printPartyStatement(activity, party, partyTx, boxMap)
                } else {
                    PdfExportHelper.printTransactionsReport(activity, transactions, partyMap, boxMap, action.reportTitle)
                }
            }
            "CASH" -> {
                val box = action.cashBoxId?.let { boxMap[it] }
                val boxTx = if (box != null) {
                    transactions.filter { it.cashBoxId == box.id || it.targetCashBoxId == box.id }
                } else transactions
                PdfExportHelper.printTransactionsReport(activity, boxTx, partyMap, boxMap, action.reportTitle)
            }
            "DAILY" -> {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                val dailyTx = transactions.filter { it.timestamp >= startOfDay }
                PdfExportHelper.printTransactionsReport(activity, dailyTx, partyMap, boxMap, action.reportTitle)
            }
            else -> {
                PdfExportHelper.printTransactionsReport(activity, transactions, partyMap, boxMap, action.reportTitle)
            }
        }
    }

    fun handleExportExcel(action: ChatReportAction) {
        val file = when (action.reportType) {
            "PARTY" -> {
                val party = action.partyId?.let { partyMap[it] }
                    ?: parties.firstOrNull { it.name.contains(action.partyName ?: "") }
                if (party != null) {
                    val partyTx = transactions.filter { it.partyId == party.id }
                    ExcelExportHelper.exportAccountStatementToExcel(context, party, partyTx, boxMap)
                } else {
                    ExcelExportHelper.exportTransactionsToExcel(
                        context = context,
                        reportTitle = action.reportTitle,
                        transactions = transactions,
                        parties = partyMap,
                        cashBoxes = boxMap,
                        totalInflow = action.totalIn,
                        totalOutflow = action.totalOut,
                        netBalance = action.netBalance
                    )
                }
            }
            "CASH" -> {
                val box = action.cashBoxId?.let { boxMap[it] }
                val boxTx = if (box != null) {
                    transactions.filter { it.cashBoxId == box.id || it.targetCashBoxId == box.id }
                } else transactions
                ExcelExportHelper.exportTransactionsToExcel(
                    context = context,
                    reportTitle = action.reportTitle,
                    transactions = boxTx,
                    parties = partyMap,
                    cashBoxes = boxMap,
                    totalInflow = action.totalIn,
                    totalOutflow = action.totalOut,
                    netBalance = action.netBalance
                )
            }
            "DAILY" -> {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                val dailyTx = transactions.filter { it.timestamp >= startOfDay }
                ExcelExportHelper.exportTransactionsToExcel(
                    context = context,
                    reportTitle = action.reportTitle,
                    transactions = dailyTx,
                    parties = partyMap,
                    cashBoxes = boxMap,
                    totalInflow = action.totalIn,
                    totalOutflow = action.totalOut,
                    netBalance = action.netBalance
                )
            }
            else -> {
                ExcelExportHelper.exportTransactionsToExcel(
                    context = context,
                    reportTitle = action.reportTitle,
                    transactions = transactions,
                    parties = partyMap,
                    cashBoxes = boxMap,
                    totalInflow = action.totalIn,
                    totalOutflow = action.totalOut,
                    netBalance = action.netBalance
                )
            }
        }

        if (file != null) {
            ExcelExportHelper.shareFile(context, file)
        } else {
            Toast.makeText(context, "فشل إنشاء ملف Excel", Toast.LENGTH_SHORT).show()
        }
    }

    // In-Chat Voice Recording State & Methods (Continuous, Non-cutting, No sticking dialog)
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isTranscribingAudio by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var audioAmplitude by remember { mutableFloatStateOf(0.15f) }

    fun cancelChatAudioRecording() {
        try {
            if (isRecordingAudio) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecordingAudio = false
        recordingFile?.delete()
        recordingFile = null
    }

    fun stopChatAudioRecording(sendDirectly: Boolean) {
        if (!isRecordingAudio) return
        isRecordingAudio = false
        isTranscribingAudio = true

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        mediaRecorder = null

        val file = recordingFile
        if (file == null || !file.exists() || file.length() <= 0) {
            isTranscribingAudio = false
            Toast.makeText(context, "لم يتم التقاط صوت واضح، تحدث مجدداً", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                val text = viewModel.transcribeAudioFile(file, "audio/mp4")
                if (!text.isNullOrBlank()) {
                    val cleanText = text.trim()
                    if (sendDirectly) {
                        viewModel.sendAiMessage(cleanText)
                    } else {
                        inputText = cleanText
                    }
                } else {
                    Toast.makeText(context, "تعذر التفريغ الصوتي التلقائي، يمكنك الكتابة في المربع", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Throwable) {
                Toast.makeText(context, "حدث خطأ أثناء التفريغ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isTranscribingAudio = false
                file.delete()
                recordingFile = null
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = File(context.cacheDir, "chat_voice_${System.currentTimeMillis()}.m4a")
                if (file.exists()) file.delete()
                recordingFile = file

                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setOutputFile(file.absolutePath)
                recorder.prepare()
                recorder.start()

                mediaRecorder = recorder
                isRecordingAudio = true
                recordingSeconds = 0
            } catch (e: Throwable) {
                isRecordingAudio = false
                Toast.makeText(context, "تعذر تشغيل الميكروفون: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "يرجى منح إذن الميكروفون للتحدث صوتياً", Toast.LENGTH_SHORT).show()
        }
    }

    fun startChatAudioRecording() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            val file = File(context.cacheDir, "chat_voice_${System.currentTimeMillis()}.m4a")
            if (file.exists()) file.delete()
            recordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            isRecordingAudio = true
            recordingSeconds = 0
        } catch (e: Throwable) {
            isRecordingAudio = false
            Toast.makeText(context, "تعذر تشغيل الميكروفون: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Ensure audio recorder cleanup
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isRecordingAudio) {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            mediaRecorder = null
        }
    }

    // Audio timer and wave amplitude loop while recording (never cuts off!)
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isActive && isRecordingAudio) {
                delay(100)
                recordingSeconds++
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val norm = (maxAmp / 8000f).coerceIn(0.15f, 1.0f)
                    audioAmplitude = norm
                } catch (e: Throwable) {
                    audioAmplitude = 0.2f
                }
            }
        } else {
            audioAmplitude = 0.15f
        }
    }

    // Scroll to bottom when messages update or input state changes
    LaunchedEffect(messages.size, isThinking, inputText.isNotEmpty()) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // AI Header info & Toolbar
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "ذكاء اصطناعي",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "المحاسب الذكي ومستشارك المالي",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = "🤖 $selectedModel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (useCustomKey) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF1B8755).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "مفتاح خاص ✓",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1B8755),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Report Launcher Button
                    IconButton(
                        onClick = { showQuickReportDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_quick_reports_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = "التقارير والكشوفات",
                            tint = PrimaryGreen
                        )
                    }

                    // AI Memory Bank Button with Count Badge
                    IconButton(
                        onClick = { showMemoryDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_memory_bank_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeMemoriesCount > 0) {
                                    Badge(
                                        containerColor = PrimaryGreen,
                                        contentColor = Color.White
                                    ) {
                                        Text("$activeMemoriesCount", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "ذاكرة المحاسب",
                                tint = PrimaryGreen
                            )
                        }
                    }

                    // Settings Button
                    IconButton(
                        onClick = { showAiSettings = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_chat_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "إعدادات الذكاء الاصطناعي",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Clear Chat Button
                    IconButton(
                        onClick = { showClearChatDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "مسح المحادثة",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Suggestion Chips with Memory & Quick Prompts
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickPrompts = listOf(
                "📊 تقرير حركة اليوم (PDF/Excel)",
                "👤 كشف حساب عميل",
                "🏢 كشف حساب مورد",
                "💵 كشف النقدية في الصندوق",
                "📑 التقرير المالي الشامل",
                "🧠 ماذا تتذكر عن حساباتي؟",
                "💡 نصائح لتحسين السيولة والميزانية",
                "📊 ما هو صافي موقفي المالي اليوم؟",
                "👥 كشف ديون العملاء والموردين",
                "استلمت 500 من محمد",
                "صرفت 60 بنزين"
            )
            items(quickPrompts) { prompt ->
                AssistChip(
                    onClick = {
                        if (prompt.contains("كشف حساب عميل") || prompt.contains("كشف حساب مورد") || prompt.contains("كشف النقدية")) {
                            showQuickReportDialog = true
                        } else {
                            viewModel.sendAiMessage(prompt)
                        }
                    },
                    label = { Text(prompt, fontSize = 12.sp) }
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    msg = msg,
                    onExportPdf = { action -> handleExportPdf(action) },
                    onExportExcel = { action -> handleExportExcel(action) }
                )
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "المحاسب يحلل كلامك ويسترجع ذاكرته ويعد التقرير المطلوب...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Live Typing Box / Text Input Display Container
        AnimatedVisibility(
            visible = inputText.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = PrimaryGreen.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مستشارك المالي يستمع ويكتب مباشرة:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        IconButton(onClick = { inputText = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = inputText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Input bar
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRecordingAudio || isTranscribingAudio) {
                // In-Chat Voice Recording State: Continuous, Real-Time Wave, Quick Actions
                val secs = recordingSeconds / 10
                val formattedTime = String.format("%02d:%02d", secs / 60, secs % 60)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    IconButton(
                        onClick = { cancelChatAudioRecording() },
                        enabled = !isTranscribingAudio,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .testTag("ai_voice_cancel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "إلغاء التسجيل",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Timer, Waveform & Status Container
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MoneyExpenseRed.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = MoneyExpenseRed.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MoneyExpenseRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTranscribingAudio) "جاري التفريغ..." else formattedTime,
                                color = MoneyExpenseRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Animated Frequency Equalizer
                        if (!isTranscribingAudio) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(20.dp)
                            ) {
                                for (i in 1..6) {
                                    val barHeight = (6 + (audioAmplitude * (i % 4 + 1) * 5)).coerceIn(6f, 20f).dp
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MoneyExpenseRed)
                                    )
                                }
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryGreen
                            )
                        }

                        Text(
                            text = if (isTranscribingAudio) "لحظات..." else "تحدث براحتك",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Transcribe to Text Box Button (Review / Edit before sending)
                    IconButton(
                        onClick = { stopChatAudioRecording(sendDirectly = false) },
                        enabled = !isTranscribingAudio,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("ai_voice_transcribe_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "إملاء في مربع النص للمراجعة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Directly Button
                    IconButton(
                        onClick = { stopChatAudioRecording(sendDirectly = true) },
                        enabled = !isTranscribingAudio,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen)
                            .testTag("ai_voice_send_direct_btn")
                    ) {
                        if (isTranscribingAudio) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال التسجيل المفرّغ مباشرة",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // Normal Input Bar: Mic button + OutlinedTextField + Send button (Reports button removed as requested!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic Button (Tap to record freely in chat without premature cutoff)
                    IconButton(
                        onClick = { startChatAudioRecording() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("ai_mic_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "تحدث بصوتك بحرية",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_input_text"),
                        placeholder = { Text("اكتب أو تحدث، مثل: استلمت 500 من محمد...", fontSize = 12.sp) },
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { inputText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح الإدخال",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendAiMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("ai_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    // AI Settings Dialog
    if (showAiSettings) {
        AiSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showAiSettings = false }
        )
    }

    // AI Memory Dialog
    if (showMemoryDialog) {
        AiMemoryDialog(
            viewModel = viewModel,
            onDismiss = { showMemoryDialog = false }
        )
    }

    // Quick Financial Report Launcher Dialog
    if (showQuickReportDialog) {
        QuickReportDialog(
            parties = parties,
            cashBoxes = cashBoxes,
            onSelectPrompt = { prompt ->
                viewModel.sendAiMessage(prompt)
            },
            onDismiss = { showQuickReportDialog = false }
        )
    }

    // AI Voice Dialog
    if (showVoiceDialog) {
        com.example.ui.components.VoiceTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false },
            onSuccess = { _ ->
                showVoiceDialog = false
            }
        )
    }

    // Clear Chat Confirmation
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PrimaryGreen) },
            title = { Text("بدء محادثة جديدة؟") },
            text = { Text("سيتم مسح سجل المحادثة الحالية والبدء من جديد. (تظل ذاكرة المحاسب وقواعده المحفوظة قائمة).") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearChatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("مسح وبدء جديد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(
    msg: ChatMessage,
    onExportPdf: (ChatReportAction) -> Unit = {},
    onExportExcel: (ChatReportAction) -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currencyFormat = remember { DecimalFormat("#,##0.##") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                ),
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = if (msg.isUser) PrimaryGreen else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = msg.text,
                        color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )

                    // Interactive Report Export Card inside Message Bubble
                    if (msg.reportAction != null) {
                        val report = msg.reportAction
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = report.reportTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Summary Pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("المقبوضات", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = "${currencyFormat.format(report.totalIn)} ر.س",
                                            fontWeight = FontWeight.Bold,
                                            color = MoneyIncomeGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Column {
                                        Text("المدفوعات", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = "${currencyFormat.format(report.totalOut)} ر.س",
                                            fontWeight = FontWeight.Bold,
                                            color = MoneyExpenseRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Column {
                                        Text("الصافي/الرصيد", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = "${currencyFormat.format(report.netBalance)} ر.س",
                                            fontWeight = FontWeight.Bold,
                                            color = if (report.netBalance >= 0) MoneyIncomeGreen else MoneyExpenseRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Two Export Action Buttons (PDF & Excel)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onExportPdf(report) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تصدير PDF", fontSize = 11.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { onExportExcel(report) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تصدير Excel", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // If memory was learned/saved during this message
                    if (!msg.learnedMemoryText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF6750A4).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color(0xFF6750A4),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "🧠 تم حفظها في الذاكرة: ${msg.learnedMemoryText}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6750A4),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Timestamp
            Text(
                text = timeFormat.format(Date(msg.timestamp)),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (msg.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
