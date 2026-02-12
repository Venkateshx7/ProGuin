package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId").orEmpty()
        val taskName = intent.getStringExtra("taskName").orEmpty()
        val timerMinutes = intent.getIntExtra("timerMinutes", 0)

        NotificationHelper.ensureChannels(context)
        NotificationHelper.showReminder(
            context = context,
            title = "Scheduled Task 🔔",
            message = taskName.ifBlank { "Task" }
        )

        // Auto-start timer service if timer exists
        if (timerMinutes > 0) {
            TimerForegroundService.startTimer(
                context = context,
                taskId = taskId,
                taskName = taskName,
                minutes = timerMinutes
            )
        }
    }
}

