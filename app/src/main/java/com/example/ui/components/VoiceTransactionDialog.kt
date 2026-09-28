package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.AssistantResult
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VoiceTransactionDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var spokenText by remember { mutableStateOf("") }
    var inputQuery by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var parseResult by remember { mutableStateOf<AssistantResult?>(null) }
    
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var isRecordingInApp by remember { mutableStateOf(false) }
    
    var liveSpeechStatus by remember { mutableStateOf("انقر الزر الكبير للتسجيل المباشر من الميكروفون، وسيقوم المحاسب بتفريغه وتحليله") }
    var audioAmplitude by remember { mutableFloatStateOf(0.15f) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun processSpeechInput(input: String) {
        if (input.isBlank()) return
        spokenText = input
        inputQuery = input
        isProcessing = true
        errorMessage = null
        parseResult = null

        scope.launch {
            try {
                val result = viewModel.assistantProcessSpeech(input)
                parseResult = result
                isProcessing = false
                if (result.executedTransaction != null) {
                    onSuccess(result.reply)
                }
            } catch (e: Throwable) {
                isProcessing = false
                errorMessage = e.localizedMessage ?: "حدث خطأ أثناء معالجة القيد الصوتي"
            }
        }
    }

    fun stopRecordingInAppAndTranscribe() {
        if (!isRecordingInApp) return
        isRecordingInApp = false
        liveSpeechStatus = "⏳ جاري تفريغ التسجيل الصوتي وتحويله إلى نص عالي الدقة..."
        isProcessing = true
        errorMessage = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        mediaRecorder = null

        val audioFile = recordingFile
        if (audioFile == null || !audioFile.exists() || audioFile.length() <= 0) {
            isProcessing = false
            liveSpeechStatus = "لم يتم تسجيل صوت أو كان التسجيل قصيراً جداً. حاول التحدث مجدداً."
            return
        }

        scope.launch {
            try {
                val transcribedText = viewModel.transcribeAudioFile(audioFile, "audio/mp4")
                if (!transcribedText.isNullOrBlank()) {
                    inputQuery = transcribedText
                    spokenText = transcribedText
                    liveSpeechStatus = "تم التفريغ بنجاح: '$transcribedText'"
                    processSpeechInput(transcribedText)
                } else {
                    isProcessing = false
                    liveSpeechStatus = "تم تسجيل المقطع. يمكنك كتابة القيد أو النقر على العبارات النموذجية."
                    errorMessage = "تعذر تحويل الصوت تلقائياً، يمكنك المتابعة بكتابة القيد في المربع أدناه."
                }
            } catch (e: Throwable) {
                isProcessing = false
                liveSpeechStatus = "حدث خطأ أثناء تفريغ الصوت."
                errorMessage = e.localizedMessage
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            errorMessage = null
        } else {
            errorMessage = "يرجى منح إذن الوصول للميكروفون لبدء التسجيل الصوتي."
        }
    }

    fun startRecordingInApp() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            try {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            } catch (e: Throwable) {
                errorMessage = "يرجى منح إذن الميكروفون من إعدادات الجهاز."
            }
            return
        }

        try {
            if (isRecordingInApp) {
                stopRecordingInAppAndTranscribe()
                return
            }

            val file = File(context.cacheDir, "recorded_voice_tx.m4a")
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
            isRecordingInApp = true
            recordingSeconds = 0
            liveSpeechStatus = "🔴 جاري تسجيل الصوت المباشر من الميكروفون... تحدث الآن ثم انقر مجدداً لإيقاف التسجيل وتفريغه"
            errorMessage = null
        } catch (e: Throwable) {
            isRecordingInApp = false
            errorMessage = "تعذر تشغيل الميكروفون: ${e.localizedMessage}. يمكنك استخدام الإملاء أو النقر على العبارات الجاهزة."
        }
    }

    // System Recognizer Launcher (Google Voice Intent fallback)
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputQuery = spoken
                liveSpeechStatus = "تم التفريغ عبر نظام أندرويد: '$spoken'"
                processSpeechInput(spoken)
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isRecordingInApp) {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            mediaRecorder = null
        }
    }

    // Timer & Live Amplitude Wave loop
    LaunchedEffect(isRecordingInApp) {
        if (isRecordingInApp) {
            recordingSeconds = 0
            while (isActive && isRecordingInApp) {
                delay(100)
                recordingSeconds++
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val norm = (maxAmp / 8000f).coerceIn(0.15f, 1.0f)
                    audioAmplitude = norm
                } catch (e: Throwable) {
                    audioAmplitude = (0.2f + Math.random().toFloat() * 0.8f)
                }
            }
        } else {
            audioAmplitude = 0.15f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecordingInApp) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = {
            if (isRecordingInApp) {
                stopRecordingInAppAndTranscribe()
            }
            onDismiss()
        },
        confirmButton = {
            if (parseResult?.executedTransaction != null) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("تم الإغلاق", color = Color.White)
                }
            } else if (inputQuery.isNotBlank() && !isProcessing) {
                Button(
                    onClick = { processSpeechInput(inputQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("تنفيذ ومعالجة القيد", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isRecordingInApp) {
                    stopRecordingInAppAndTranscribe()
                }
                onDismiss()
            }) {
                Text("إلغاء")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل وتفريغ الصوت المحاسبي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
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
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Microphone Action Circle & Audio Wave Visualizer
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(if (isRecordingInApp) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isRecordingInApp) MoneyExpenseRed.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (isRecordingInApp) {
                                stopRecordingInAppAndTranscribe()
                            } else {
                                startRecordingInApp()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isRecordingInApp) MoneyExpenseRed else PrimaryGreen)
                            .testTag("voice_dialog_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isRecordingInApp) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "تسجيل صوتي",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Listening Status Indicator & Wave Bars
                if (isRecordingInApp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MoneyExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val secs = recordingSeconds / 10
                        val formattedTime = String.format("%02d:%02d", secs / 60, secs % 60)
                        Text(
                            text = "🔴 جاري التسجيل ($formattedTime) - اضغط لإيقاف التسجيل وتفريغه",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneyExpenseRed,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Animated Audio Frequency Bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(24.dp)
                    ) {
                        for (i in 1..10) {
                            val barHeight = (10 + (audioAmplitude * (i % 6 + 1) * 8)).dp
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MoneyExpenseRed)
                            )
                        }
                    }
                } else {
                    Text(
                        text = liveSpeechStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // System Google Voice Fallback Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Throwable) {
                            errorMessage = "محرك Google Voice غير متاح على هذا الجهاز. استخدم التسجيل المباشر أعلاه."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("أو استخدم التعرف الصوتي المباشر للنظام (Google Voice)", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Voice Text Field (Editable Live Transcribed Box)
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_input_field"),
                    label = { Text("النص المفرّغ من التسجيل الصوتي", fontSize = 11.sp) },
                    placeholder = { Text("سيعرض النص المفرغ هنا فور إيقاف التسجيل...", fontSize = 12.sp) },
                    trailingIcon = {
                        if (inputQuery.isNotBlank()) {
                            IconButton(onClick = { processSpeechInput(inputQuery) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "معالجة والقيد",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    maxLines = 3
                )

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جاري فهم القيد الصوتي وتسجيله في المحاسب...",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }

                // Parsed Result Card
                parseResult?.let { res ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.isSuccess) MoneyIncomeGreen.copy(alpha = 0.1f) else MoneyExpenseRed.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (res.isSuccess) MoneyIncomeGreen else PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (res.executedTransaction != null) "تم القيد المحاسبي بنجاح!" else "نتيجة التحليل:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (res.isSuccess) MoneyIncomeGreen else PrimaryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = res.reply,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ℹ️ $err",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Voice Quick Examples / Presets
                Text(
                    text = "أمثلة نموذجية بنقرة واحدة للتجربة المباشرة:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val examples = listOf(
                        "استلمت 1000 من محمد",
                        "I paid 500 for rent",
                        "صرفت 60 بنزين",
                        "Paid 300 to supplier Mike",
                        "سجل دين على علي 400",
                        "Spent 50 on groceries",
                        "تسديد فاتورة كهرباء 150"
                    )
                    items(examples) { ex ->
                        SuggestionChip(
                            onClick = { processSpeechInput(ex) },
                            label = { Text(ex, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    )
}
