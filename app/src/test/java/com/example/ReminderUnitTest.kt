package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.reminder.ReminderPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderUnitTest {

    @Test
    fun testReminderPreferencesSaveAndRetrieve() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ReminderPreferences(context)

        prefs.isDailyReminderEnabled = true
        prefs.dailyReminderHour = 21
        prefs.dailyReminderMinute = 30
        prefs.customMessage = "تذكير يومي مخصص"

        assertEquals(true, prefs.isDailyReminderEnabled)
        assertEquals(21, prefs.dailyReminderHour)
        assertEquals(30, prefs.dailyReminderMinute)
        assertEquals("تذكير يومي مخصص", prefs.customMessage)

        prefs.isDebtReminderEnabled = true
        prefs.debtReminderDays = 5
        assertEquals(true, prefs.isDebtReminderEnabled)
        assertEquals(5, prefs.debtReminderDays)
    }
}
