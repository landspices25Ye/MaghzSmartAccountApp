package com.example.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus(val descriptionAr: String) {
    AVAILABLE("البصمة متوفرة ومجهزة"),
    NO_HARDWARE("الجهاز لا يدعم مستشعر البصمة"),
    HARDWARE_UNAVAILABLE("مستشعر البصمة غير متاح حالياً"),
    NONE_ENROLLED("لم يتم تسجيل بصمة إصبع أو وجه في إعدادات الجهاز"),
    UNSUPPORTED("نوع المصادقة غير مدعوم على هذا النظام")
}

object BiometricAuthManager {

    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                             BiometricManager.Authenticators.BIOMETRIC_WEAK

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> {
                // Fallback check with device credential included
                val credentialCheck = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                if (credentialCheck == BiometricManager.BIOMETRIC_SUCCESS) {
                    BiometricStatus.AVAILABLE
                } else {
                    BiometricStatus.UNSUPPORTED
                }
            }
        }
    }

    fun canAuthenticateDeviceCredential(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "حماية الحسابات المالية",
        subtitle: String = "يرجى التحقق من هويتك بواسطة بصمة الإصبع أو الوجه للوصول للبيانات",
        negativeButtonText: String = "إلغاء",
        useDeviceCredentialFallback: Boolean = true,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (useDeviceCredentialFallback) {
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                // On older APIs, DEVICE_CREDENTIAL with BIOMETRIC_WEAK may vary, so try strong or credential
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
            try {
                builder.setAllowedAuthenticators(authenticators)
            } catch (e: Exception) {
                // Fallback to biometric only with negative button
                builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                builder.setNegativeButtonText(negativeButtonText)
            }
        } else {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            builder.setNegativeButtonText(negativeButtonText)
        }

        val promptInfo = builder.build()
        biometricPrompt.authenticate(promptInfo)
    }
}
