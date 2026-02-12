package com.venkatesh.proguin.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.venkatesh.proguin.R

object NotificationHelper {

    private const val CH_REMINDERS = "proguin_reminders"
    private const val CH_TIMER = "proguin_timer"

    private const val ID_REMINDER = 1001
    const val ID_TIMER = 2001

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminders = NotificationChannel(
            CH_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val timer = NotificationChannel(
            CH_TIMER,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        )

        nm.createNotificationChannel(reminders)
        nm.createNotificationChannel(timer)
    }

    fun showReminder(context: Context, title: String, message: String) {
        val n = NotificationCompat.Builder(context, CH_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_REMINDER, n)
    }

    fun buildTimerNotification(context: Context, title: String, text: String) =
        NotificationCompat.Builder(context, CH_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    fun updateTimer(context: Context, title: String, text: String) {
        val n = buildTimerNotification(context, title, text)
        NotificationManagerCompat.from(context).notify(ID_TIMER, n)
    }

    fun cancelTimer(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_TIMER)
    }
}
