package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = ReminderPreferences(context)

    companion object {
        const val REQUEST_CODE_DAILY = 2001
        const val REQUEST_CODE_DEBT = 2002
    }

    fun rescheduleAllReminders() {
        if (preferences.isDailyReminderEnabled) {
            scheduleDailyReminder()
        } else {
            cancelDailyReminder()
        }

        if (preferences.isDebtReminderEnabled) {
            scheduleDebtReminder()
        } else {
            cancelDebtReminder()
        }
    }

    fun scheduleDailyReminder() {
        if (!preferences.isDailyReminderEnabled) return

        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_TITLE, "📝 تذكير بتسجيل المعاملات اليومية")
            putExtra(
                ReminderNotificationReceiver.EXTRA_MESSAGE,
                preferences.customMessage.ifBlank { "لا تنسَ تسجيل مصروفاتك وإيراداتك اليومية في دفتر الحسابات!" }
            )
            putExtra(ReminderNotificationReceiver.EXTRA_TYPE, ReminderNotificationReceiver.TYPE_DAILY_TRANSACTIONS)
        }

        val pendingIntent = getPendingIntent(REQUEST_CODE_DAILY, intent)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, preferences.dailyReminderHour)
            set(Calendar.MINUTE, preferences.dailyReminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        scheduleAlarm(calendar.timeInMillis, pendingIntent)
    }

    fun scheduleDebtReminder() {
        if (!preferences.isDebtReminderEnabled) return

        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_TITLE, "👥 تذكير بمتابعة ديون العملاء والموردين")
            putExtra(
                ReminderNotificationReceiver.EXTRA_MESSAGE,
                "تحقق من كشوفات الحسابات والديون المستحقة للتحصيل أو السداد اليوم!"
            )
            putExtra(ReminderNotificationReceiver.EXTRA_TYPE, ReminderNotificationReceiver.TYPE_DEBT_FOLLOWUP)
        }

        val pendingIntent = getPendingIntent(REQUEST_CODE_DEBT, intent)

        val days = preferences.debtReminderDays.coerceAtLeast(1)
        val triggerTime = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)

        scheduleAlarm(triggerTime, pendingIntent)
    }

    fun cancelDailyReminder() {
        val intent = Intent(context, ReminderNotificationReceiver::class.java)
        val pendingIntent = getPendingIntent(REQUEST_CODE_DAILY, intent)
        alarmManager.cancel(pendingIntent)
    }

    fun cancelDebtReminder() {
        val intent = Intent(context, ReminderNotificationReceiver::class.java)
        val pendingIntent = getPendingIntent(REQUEST_CODE_DEBT, intent)
        alarmManager.cancel(pendingIntent)
    }

    fun sendTestNotificationNow() {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_TITLE, "🔔 إشعار تجريبي من المحاسب الذكي")
            putExtra(
                ReminderNotificationReceiver.EXTRA_MESSAGE,
                "تم تفعيل التذكيرات بنجاح! سيصلك تذكيرك القادم في الموعد المحدد."
            )
            putExtra(ReminderNotificationReceiver.EXTRA_TYPE, ReminderNotificationReceiver.TYPE_TEST)
        }
        context.sendBroadcast(intent)
    }

    private fun scheduleAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPendingIntent(requestCode: Int, intent: Intent): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
