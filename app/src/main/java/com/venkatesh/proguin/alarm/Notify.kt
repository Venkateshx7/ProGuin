package com.venkatesh.proguin.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notify {
    const val CHANNEL_ID = "proguin_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)

            if (existing == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Proguin Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
                ch.setDescription("Task reminders and alarms")
                nm.createNotificationChannel(ch)
            }
        }
    }
}