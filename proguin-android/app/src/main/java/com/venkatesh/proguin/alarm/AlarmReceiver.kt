package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID).orEmpty()
        val taskName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_NAME).orEmpty()

        // ✅ FIX: use the same key your scheduler sends
        val timerMinutes = intent.getIntExtra("timerMinutes", 0)

        // 1) Update pages.json -> mark task started
        try {
            val py = com.chaquo.python.Python.getInstance()
            val core = py.getModule("proguin.core")
            val pagesPath = File(context.filesDir, "pages.json").absolutePath

            val pages = core.callAttr("load_pages", pagesPath)
            core.callAttr("start_task_by_id", pages, taskId)
            core.callAttr("save_pages", pagesPath, pages)
        } catch (_: Exception) { }

        // 2) Notify
        NotificationHelper.showReminder(
            context = context,
            title = "Scheduled Task 🔔",
            message = taskName.ifBlank { "Task" }
        )

        // 3) Start timer if exists
        if (timerMinutes > 0) {
            TimerForegroundService.startTimer(
                context = context,
                taskId = taskId,
                taskName = taskName,
                minutes = timerMinutes
            )
        }

        // 4) Refresh UI if open
        val updateIntent = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(updateIntent)
    }
}
