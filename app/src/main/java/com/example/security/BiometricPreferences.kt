package com.example.security

import android.content.Context
import android.content.SharedPreferences

class BiometricPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_accountant_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_LAST_UNLOCKED_TIME = "key_last_unlocked_time"
        private const val KEY_LOCK_ON_BACKGROUND = "key_lock_on_background"
    }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true) // Default enabled to secure financial data
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var isLockOnBackgroundEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ON_BACKGROUND, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ON_BACKGROUND, value).apply()

    var lastUnlockedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_UNLOCKED_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UNLOCKED_TIME, value).apply()
}
