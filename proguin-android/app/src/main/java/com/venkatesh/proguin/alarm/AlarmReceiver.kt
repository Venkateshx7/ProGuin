package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID).orEmpty()
        val taskName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_NAME).orEmpty()
        val timerMinutes = intent.getIntExtra(NotificationHelper.EXTRA_TIMER_MINUTES, 0)

        // ✅ Always ensure channels exist
        NotificationHelper.ensureChannels(context)

        // ✅ 1) Show reminder notification (ONE TIME sound)
        // Works only if notification permission is allowed on Android 13+
        try {
            val canNotify =
                if (Build.VERSION.SDK_INT < 33) true
                else ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (canNotify) {
                NotificationHelper.showReminder(
                    context = context,
                    title = "Scheduled Task 🔔",
                    message = taskName.ifBlank { "Task" }
                )
            }
        } catch (_: Exception) { }

        // ✅ 2) Mark task as running in pages.json
        try {
            val py = com.chaquo.python.Python.getInstance()
            val core = py.getModule("proguin.core")
            val pagesPath = File(context.filesDir, "pages.json").absolutePath
            val pages = core.callAttr("load_pages", pagesPath)

            try {
                core.callAttr("start_task_by_id", pages, taskId)
            } catch (_: Exception) {
                try { core.callAttr("set_task_running", pages, taskId, true) } catch (_: Exception) { }
            }

            core.callAttr("save_pages", pagesPath, pages)
        } catch (_: Exception) { }

        // ✅ 3) Auto start timer service
        if (timerMinutes > 0) {
            try {
                TimerForegroundService.startTimer(
                    context = context,
                    taskId = taskId,
                    taskName = taskName,
                    minutes = timerMinutes
                )
            } catch (_: Exception) { }
        }

        // ✅ 4) Refresh UI
        try {
            context.sendBroadcast(
                Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                    setPackage(context.packageName)
                }
            )
        } catch (_: Exception) { }
    }
}
