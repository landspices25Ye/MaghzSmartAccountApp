package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAuthManager
import com.example.security.BiometricStatus

@Composable
fun BiometricLockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var biometricStatus by remember { mutableStateOf(BiometricStatus.AVAILABLE) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun triggerAuth() {
        if (activity == null) {
            statusMessage = "تعذر الوصول لنشاط النظام للمصادقة"
            isError = true
            return
        }

        biometricStatus = BiometricAuthManager.checkBiometricStatus(context)

        when (biometricStatus) {
            BiometricStatus.AVAILABLE -> {
                statusMessage = "جاري التحقق من البصمة أو الوجه..."
                isError = false
                BiometricAuthManager.authenticate(
                    activity = activity,
                    title = "فتح المحاسب الذكي",
                    subtitle = "تحقق من هويتك بواسطة بصمة الإصبع أو الوجه لحماية دفتر الحسابات",
                    negativeButtonText = "إلغاء",
                    useDeviceCredentialFallback = true,
                    onSuccess = {
                        statusMessage = "تم التحقق بنجاح! جاري فتح الدفاتر..."
                        isError = false
                        onUnlocked()
                    },
                    onError = { errorCode, errString ->
                        isError = true
                        statusMessage = "فشلت المصادقة ($errorCode): $errString"
                    },
                    onFailed = {
                        isError = true
                        statusMessage = "لم يتم التعرف على البصمة! حاول مرة أخرى"
                    }
                )
            }
            BiometricStatus.NONE_ENROLLED -> {
                statusMessage = "لا يوجد بصمة مسجلة في جهازك. اضغط أدناه لاستخدام رمز PIN أو تجاوز الأمان"
                isError = true
                // Try fallback auth with device credentials directly if available
                if (BiometricAuthManager.canAuthenticateDeviceCredential(context)) {
                    BiometricAuthManager.authenticate(
                        activity = activity,
                        title = "رمز قفل الجهاز",
                        subtitle = "أدخل رمز PIN أو كلمة مرور الهاتف للوصول للبيانات",
                        useDeviceCredentialFallback = true,
                        onSuccess = { onUnlocked() },
                        onError = { _, err -> statusMessage = err.toString() },
                        onFailed = { statusMessage = "فشل التحقق من رمز الهاتف" }
                    )
                }
            }
            else -> {
                // If biometric hardware unavailable, allow PIN fallback
                if (BiometricAuthManager.canAuthenticateDeviceCredential(context)) {
                    BiometricAuthManager.authenticate(
                        activity = activity,
                        title = "مصادقة الجهاز",
                        subtitle = "استخدم رمز قفل الهاتف للفتح",
                        useDeviceCredentialFallback = true,
                        onSuccess = { onUnlocked() },
                        onError = { _, err -> statusMessage = err.toString() },
                        onFailed = { statusMessage = "فشلت المصادقة" }
                    )
                } else {
                    statusMessage = biometricStatus.descriptionAr
                    isError = true
                }
            }
        }
    }

    // Auto trigger authentication on launch
    LaunchedEffect(Unit) {
        triggerAuth()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Dark slate blue
                        Color(0xFF1E293B),
                        Color(0xFF0D251E)  // Deep emerald shadow
                    )
                )
            )
            .testTag("biometric_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Shield / Lock Icon with pulse effect
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = 0.35f),
                                    Color(0xFF059669).copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color(0xFF10B981).copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "حماية الأمان",
                        modifier = Modifier.size(52.dp),
                        tint = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "دفتر الحسابات مشفّر ومحمي",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "يرجى المصادقة ببصمة الإصبع أو الوجه للوصول إلى بيانات الخزينة والأرصدة المالية",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Status banner
                AnimatedVisibility(
                    visible = statusMessage != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    statusMessage?.let { msg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isError) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isError) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isError) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Unlock Button
                Button(
                    onClick = { triggerAuth() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("biometric_unlock_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تأكيد البصمة / الوجه للفتح",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary PIN/Password Fallback Button
                OutlinedButton(
                    onClick = {
                        if (activity != null) {
                            BiometricAuthManager.authenticate(
                                activity = activity,
                                title = "رمز قفل الهاتف",
                                subtitle = "أدخل رمز PIN أو النمط لفتح الحسابات",
                                useDeviceCredentialFallback = true,
                                onSuccess = { onUnlocked() },
                                onError = { _, err -> statusMessage = err.toString(); isError = true },
                                onFailed = { statusMessage = "لم يتم التحقق من الرمز"; isError = true }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("device_pin_unlock_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF38BDF8)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Password,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استخدام رمز قفل الهاتف (PIN)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Emergency / Admin Bypass Option if no biometrics are setup on device
                if (biometricStatus != BiometricStatus.AVAILABLE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "تم التجاوز مؤقتاً لتعديل إعدادات الأمان", Toast.LENGTH_LONG).show()
                            onUnlocked()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "تجاوز مؤقت (تفعيل الأمان لاحقاً)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
