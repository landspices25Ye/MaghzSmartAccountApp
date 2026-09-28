package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ReminderNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "accounting_reminders_channel"
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_MESSAGE = "extra_reminder_message"
        const val EXTRA_TYPE = "extra_reminder_type"
        const val ACTION_SHOW_REMINDER = "com.example.ACTION_SHOW_REMINDER_NOTIFICATION"

        const val TYPE_DAILY_TRANSACTIONS = "daily_transactions"
        const val TYPE_DEBT_FOLLOWUP = "debt_followup"
        const val TYPE_TEST = "test_notification"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule alarms after device reboot
            val scheduler = ReminderScheduler(context)
            scheduler.rescheduleAllReminders()
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "⏰ تذكير محاسبي مهم"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "يرجى مراجعة دفترك وتسجيل عملياتك اليومية."
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_DAILY_TRANSACTIONS

        showNotification(context, title, message, type)

        // Reschedule next alarm for daily or periodic triggers
        val scheduler = ReminderScheduler(context)
        if (type == TYPE_DAILY_TRANSACTIONS) {
            scheduler.scheduleDailyReminder()
        } else if (type == TYPE_DEBT_FOLLOWUP) {
            scheduler.scheduleDebtReminder()
        }
    }

    private fun showNotification(context: Context, title: String, message: String, type: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات المحاسب الذكي",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات التذكير بتسجيل المعاملات اليومية ومتابعة الديون"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open MainActivity on click
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            type.hashCode(),
            openAppIntent,
            pendingIntentFlags
        )

        val notificationId = when (type) {
            TYPE_DAILY_TRANSACTIONS -> 1001
            TYPE_DEBT_FOLLOWUP -> 1002
            else -> 1003
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }
}
