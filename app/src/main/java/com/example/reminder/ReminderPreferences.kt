package com.example.reminder

import android.content.Context
import android.content.SharedPreferences

class ReminderPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_accountant_reminder_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val KEY_DAILY_REMINDER_HOUR = "daily_reminder_hour"
        private const val KEY_DAILY_REMINDER_MINUTE = "daily_reminder_minute"
        private const val KEY_DEBT_REMINDER_ENABLED = "debt_reminder_enabled"
        private const val KEY_DEBT_REMINDER_DAYS = "debt_reminder_days"
        private const val KEY_CUSTOM_MESSAGE = "custom_reminder_message"
    }

    var isDailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, value).apply()

    var dailyReminderHour: Int
        get() = prefs.getInt(KEY_DAILY_REMINDER_HOUR, 20) // 8:00 PM
        set(value) = prefs.edit().putInt(KEY_DAILY_REMINDER_HOUR, value).apply()

    var dailyReminderMinute: Int
        get() = prefs.getInt(KEY_DAILY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_REMINDER_MINUTE, value).apply()

    var isDebtReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEBT_REMINDER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DEBT_REMINDER_ENABLED, value).apply()

    var debtReminderDays: Int
        get() = prefs.getInt(KEY_DEBT_REMINDER_DAYS, 3) // Every 3 days
        set(value) = prefs.edit().putInt(KEY_DEBT_REMINDER_DAYS, value).apply()

    var customMessage: String
        get() = prefs.getString(KEY_CUSTOM_MESSAGE, "لا تنسَ تسجيل كافة المصروفات والإيرادات اليومية في الدفتر!") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_MESSAGE, value).apply()
}
