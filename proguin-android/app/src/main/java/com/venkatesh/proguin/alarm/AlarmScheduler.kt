package com.venkatesh.proguin.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun scheduleAllowWhileIdle(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        taskId: String,
        taskName: String,
        timerMinutes: Int
    ): Boolean {

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
                putExtra(NotificationHelper.EXTRA_TASK_NAME, taskName)
                putExtra(NotificationHelper.EXTRA_TIMER_MINUTES, timerMinutes)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return try {

            val canExact =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        am.canScheduleExactAlarms()

            // ✅ Exact if allowed, otherwise allow-while-idle fallback
            if (Build.VERSION.SDK_INT >= 23 && canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancel(context: Context, requestCode: Int) {
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        ) ?: return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
        pi.cancel()
    }
}
