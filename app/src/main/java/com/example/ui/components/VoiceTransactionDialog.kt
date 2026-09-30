package com.example.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.MoneyExpenseRed
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

    var inputQuery by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    
    var isRecordingAudio by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    var liveSpeechStatus by remember { mutableStateOf("اضغط زر الميكروفون للتحدث بحرية دون انقطاع، ثم انقر لإيقاف التسجيل وتفريغه") }
    var audioAmplitude by remember { mutableFloatStateOf(0.15f) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun processSpeechInputAndAutoClose(input: String) {
        if (input.isBlank()) return
        inputQuery = input
        isProcessing = false
        errorMessage = null

        // Execute directly in viewModelScope so it never gets cancelled on dismiss
        viewModel.processVoiceCommand(input)

        onSuccess(input)
        onDismiss()
    }

    fun stopAudioRecordingAndTranscribe() {
        if (!isRecordingAudio) return
        isRecordingAudio = false
        liveSpeechStatus = "⏳ جاري تفريغ الصوت وتحويله إلى نص دقيق..."
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

        val file = recordingFile
        if (file == null || !file.exists() || file.length() <= 0) {
            isProcessing = false
            liveSpeechStatus = "لم يتم التقاط صوت واضح، اضغط للتحدث مجدداً."
            return
        }

        scope.launch {
            try {
                val text = viewModel.transcribeAudioFile(file, "audio/mp4")
                if (!text.isNullOrBlank()) {
                    inputQuery = text
                    liveSpeechStatus = "تم التفريغ بنجاح: '$text'"
                    // Process speech and automatically dismiss the dialog!
                    processSpeechInputAndAutoClose(text)
                } else {
                    isProcessing = false
                    liveSpeechStatus = "تعذر تحويل المقطع تلقائياً، يمكنك كتابة القيد في المربع أدناه."
                    errorMessage = "لم يتم التعرف على الصوت بدقة، يمكنك كتابته يدوياً."
                }
            } catch (e: Throwable) {
                isProcessing = false
                liveSpeechStatus = "حدث خطأ أثناء التفريغ."
                errorMessage = e.localizedMessage
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            errorMessage = "يرجى منح إذن الميكروفون لاستخدام التسجيل الصوتي."
        }
    }

    fun startContinuousAudioRecording() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            try {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            } catch (e: Throwable) {
                errorMessage = "يرجى السماح بالوصول للميكروفون من إعدادات جهازك."
            }
            return
        }

        try {
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
            isRecordingAudio = true
            recordingSeconds = 0
            liveSpeechStatus = "🔴 جاري التسجيل... تحدث بكل راحتك دون انقطاع ثم اضغط هنا للإيقاف والاعتماد"
            errorMessage = null
        } catch (e: Throwable) {
            isRecordingAudio = false
            errorMessage = "تعذر تشغيل الميكروفون: ${e.localizedMessage}"
        }
    }

    // Cleanup on dispose
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

    // Timer & Live Amplitude Wave loop (Never stops on silence!)
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
        targetValue = if (isRecordingAudio) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = {
            if (isRecordingAudio) {
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                mediaRecorder = null
                isRecordingAudio = false
            }
            onDismiss()
        },
        confirmButton = {
            if (isRecordingAudio) {
                Button(
                    onClick = { stopAudioRecordingAndTranscribe() },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyExpenseRed)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إيقاف واعتماد التسجيل", color = Color.White)
                }
            } else if (inputQuery.isNotBlank() && !isProcessing) {
                Button(
                    onClick = { processSpeechInputAndAutoClose(inputQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("تنفيذ ومعالجة القيد", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isRecordingAudio) {
                    try {
                        mediaRecorder?.stop()
                        mediaRecorder?.release()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    mediaRecorder = null
                    isRecordingAudio = false
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
                        text = "التسجيل والإملاء الصوتي",
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
                Spacer(modifier = Modifier.height(6.dp))

                // Microphone Action Circle & Wave Visualizer
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(if (isRecordingAudio) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isRecordingAudio) MoneyExpenseRed.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (isRecordingAudio) {
                                stopAudioRecordingAndTranscribe()
                            } else {
                                startContinuousAudioRecording()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isRecordingAudio) MoneyExpenseRed else PrimaryGreen)
                            .testTag("voice_dialog_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "تسجيل صوتي",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Listening Status Indicator & Wave Bars
                if (isRecordingAudio) {
                    val secs = recordingSeconds / 10
                    val formattedTime = String.format("%02d:%02d", secs / 60, secs % 60)
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
                        Text(
                            text = "🔴 جاري التسجيل ($formattedTime) - تحدث ثم اضغط للإيقاف والاعتماد",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Text Field (Editable Box)
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_input_field"),
                    label = { Text("النص المفرّغ من التسجيل", fontSize = 11.sp) },
                    placeholder = { Text("سيعرض النص المفرغ هنا فور إيقاف التسجيل...", fontSize = 12.sp) },
                    trailingIcon = {
                        if (inputQuery.isNotBlank() && !isProcessing) {
                            IconButton(onClick = { processSpeechInputAndAutoClose(inputQuery) }) {
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
                            text = "جاري فهم القيد وتنفيذه وإغلاق النافذة تلقائياً...",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ℹ️ $err",
                        color = MaterialTheme.colorScheme.error,
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
                        "أضف عميل جديد اسمه ماجد ورقمه 0501234567",
                        "أضف مورد جديد اسمه شركة النور",
                        "حول 500 من الصندوق الرئيسي إلى بنك الراجحي",
                        "تقرير حركة اليوم مع تصدير PDF",
                        "صرفت 60 بنزين",
                        "تسديد فاتورة كهرباء 150"
                    )
                    items(examples) { ex ->
                        SuggestionChip(
                            onClick = { processSpeechInputAndAutoClose(ex) },
                            label = { Text(ex, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    )
}
