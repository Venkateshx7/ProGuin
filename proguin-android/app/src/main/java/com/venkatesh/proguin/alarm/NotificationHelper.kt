package com.venkatesh.proguin.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.venkatesh.proguin.MainActivity
import com.venkatesh.proguin.data.SettingsStore
import kotlin.math.abs

object NotificationHelper {

    const val EXTRA_TIMER_MINUTES = "timerMinutes"

    const val CH_REMINDERS = "proguin_reminders"
    const val CH_TIMER = "proguin_timer"

    // Timer actions
    const val ACTION_TIMER_STOP = "com.venkatesh.proguin.ACTION_TIMER_STOP"
    const val ACTION_TIMER_DONE = "com.venkatesh.proguin.ACTION_TIMER_DONE"
    const val ACTION_TIMER_PAUSE = "com.venkatesh.proguin.ACTION_TIMER_PAUSE"
    const val ACTION_TIMER_RESUME = "com.venkatesh.proguin.ACTION_TIMER_RESUME"

    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_TASK_NAME = "taskName"

    // ✅ Stable notification id per task
    private const val TIMER_NOTIF_BASE = 20000

    fun timerNotifId(taskId: String): Int {
        val h = abs(taskId.hashCode())
        return TIMER_NOTIF_BASE + (h % 10000)
    }

    fun cancelNotificationById(context: Context, id: Int) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(id)
        } catch (_: Exception) {
        }
    }

    fun cancelTimerNotification(context: Context, taskId: String) {
        cancelNotificationById(context, timerNotifId(taskId))
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Reminders: should make normal sound (like "now start", "time up", scheduled)
        if (nm.getNotificationChannel(CH_REMINDERS) == null) {
            val ch = NotificationChannel(
                CH_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH // ✅ sound + heads-up (normal)
            )
            nm.createNotificationChannel(ch)
        }

        // ✅ Timers: ongoing should be silent
        if (nm.getNotificationChannel(CH_TIMER) == null) {
            val ch = NotificationChannel(
                CH_TIMER,
                "Timers",
                NotificationManager.IMPORTANCE_LOW
            )
            ch.setSound(null, null)
            ch.enableVibration(false)
            ch.enableLights(false)
            nm.createNotificationChannel(ch)
        }
    }


    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, 1000, intent, flags)
    }

    // ✅ Keep this only if you want scheduled reminders. Do NOT call this for manual Start.
    fun showReminder(context: Context, title: String, message: String) {
        ensureChannels(context)

        val settings = SettingsStore(context)
        val b = NotificationCompat.Builder(context, CH_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppPendingIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (!settings.soundEnabled()) b.setSilent(true)
        if (settings.vibrationEnabled()) b.setVibrate(longArrayOf(0, 120, 80, 120))

        safeNotify(context, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), b.build())
    }

    fun buildTimerNotification(
        context: Context,
        notifId: Int,
        taskId: String,
        taskName: String,
        contentText: String,
        isRunning: Boolean
    ): Notification {
        ensureChannels(context)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

        fun actionPI(action: String, requestCode: Int): PendingIntent {
            val i = Intent(context, TimerActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
            }
            return PendingIntent.getBroadcast(context, requestCode, i, flags)
        }

        val piStop = actionPI(ACTION_TIMER_STOP, 9001 + notifId)
        val piDone = actionPI(ACTION_TIMER_DONE, 9002 + notifId)
        val piPause = actionPI(ACTION_TIMER_PAUSE, 9003 + notifId)
        val piResume = actionPI(ACTION_TIMER_RESUME, 9004 + notifId)

        val b = NotificationCompat.Builder(context, CH_TIMER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(taskName.ifBlank { "Timer" })
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppPendingIntent(context))

        if (isRunning) b.addAction(0, "Pause", piPause) else b.addAction(0, "Resume", piResume)
        b.addAction(0, "Stop", piStop)
        b.addAction(0, "Done", piDone)

        if (Build.VERSION.SDK_INT >= 31) {
            b.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return b.build()
    }

    fun safeNotify(context: Context, id: Int, notification: Notification) {
        try {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }
}
