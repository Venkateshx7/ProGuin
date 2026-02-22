package com.venkatesh.proguin.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    /**
     * ✅ IMPORTANT (2026 reality):
     * On modern Android (12+), exact alarms can be delayed without SCHEDULE_EXACT_ALARM permission.
     * On Android 14+ the system is also stricter about starting Foreground Services from background.
     *
     * We do the best reliable thing for reminders:
     * - Use setAlarmClock when possible (more reliable delivery; treated as user-visible alarm)
     * - Fallback to setExactAndAllowWhileIdle
     */

    // ✅ OLD signature (kept) — but we force "default" instead of ""
    fun scheduleAllowWhileIdle(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        taskId: String,
        taskName: String,
        timerMinutes: Int
    ): Boolean {
        return scheduleAllowWhileIdle(
            context = context,
            requestCode = requestCode,
            triggerAtMillis = triggerAtMillis,
            pageId = "default", // ✅ IMPORTANT
            taskId = taskId,
            taskName = taskName,
            timerMinutes = timerMinutes
        )
    }

    // ✅ NEW signature (Journey-safe)
    fun scheduleAllowWhileIdle(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        pageId: String,
        taskId: String,
        taskName: String,
        timerMinutes: Int
    ): Boolean {
        return try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val safePageId = if (pageId.isBlank()) "default" else pageId

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("pageId", safePageId)
                putExtra("taskId", taskId)
                putExtra("taskName", taskName)
                putExtra("timerMinutes", timerMinutes)
                // ✅ For multi-schedule safety: receiver can ignore stale alarms
                putExtra("triggerAtMillis", triggerAtMillis)
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

            val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)

            val canExact =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

            if (canExact) {
                try {
                    // ✅ Prefer AlarmClock for best delivery (system treats it as a real alarm)
                    // It also provides a "showIntent" which the system can use for UI.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val showIntent = Intent(context, com.venkatesh.proguin.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("nav_target", "tasks")
                            putExtra("nav_page_id", safePageId)
                            putExtra("nav_task_id", taskId)
                            putExtra("nav_tab", "scheduled")
                        }

                        val showPi = PendingIntent.getActivity(
                            context,
                            40000 + (requestCode % 20000),
                            showIntent,
                            flags
                        )

                        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showPi)
                        am.setAlarmClock(info, pi)
                        return true
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                    }
                } catch (_: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancel(context: Context, requestCode: Int) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

            val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            am.cancel(pi)
            pi.cancel()
        } catch (_: Exception) { }
    }
}