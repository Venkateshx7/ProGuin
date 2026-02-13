package com.venkatesh.proguin.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val EXTRA_TIMER_MINUTES = "timerMinutes"

    const val CH_REMINDERS = "proguin_reminders"
    const val CH_TIMER = "proguin_timer"

    // Timer actions
    const val ACTION_TIMER_STOP = "com.venkatesh.proguin.ACTION_TIMER_STOP"
    const val ACTION_TIMER_DONE = "com.venkatesh.proguin.ACTION_TIMER_DONE"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_TASK_NAME = "taskName"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Reminders: normal importance (sound ok)
        if (nm.getNotificationChannel(CH_REMINDERS) == null) {
            val ch = NotificationChannel(
                CH_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(ch)
        }

        // Timers: LOW importance + NO sound (prevents tick sound spam)
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

    fun showReminder(context: Context, title: String, message: String) {
        ensureChannels(context)

        val n = NotificationCompat.Builder(context, CH_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        safeNotify(context, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n)
    }

    fun buildTimerNotification(
        context: Context,
        notifId: Int,
        taskId: String,
        taskName: String,
        contentText: String
    ): Notification {
        ensureChannels(context)

        val stopIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = ACTION_TIMER_STOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_NAME, taskName)
        }

        val doneIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = ACTION_TIMER_DONE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_NAME, taskName)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

        val piStop = PendingIntent.getBroadcast(
            context,
            9001 + notifId,
            stopIntent,
            flags
        )

        val piDone = PendingIntent.getBroadcast(
            context,
            9002 + notifId,
            doneIntent,
            flags
        )

        return NotificationCompat.Builder(context, CH_TIMER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(taskName.ifBlank { "Timer" })
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // ✅ critical: update won't ring again
            .setSilent(true)        // ✅ extra safety: no sound on updates
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", piStop)
            .addAction(0, "Done", piDone)
            .build()
    }

    fun safeNotify(context: Context, id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Notifications denied -> ignore safely
        } catch (_: Exception) {
            // OEM weirdness -> ignore safely
        }
    }
}
