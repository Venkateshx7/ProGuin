package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID).orEmpty()
        val taskName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_NAME).orEmpty()

        when (intent.action) {

            NotificationHelper.ACTION_TIMER_STOP -> {
                TimerForegroundService.stopTimer(context)
                NotificationHelper.showReminder(context, "Stopped ⏹️", taskName.ifBlank { "Timer" })
                sendPagesUpdated(context)
            }

            NotificationHelper.ACTION_TIMER_DONE -> {
                // mark done in pages.json
                try {
                    val py = com.chaquo.python.Python.getInstance()
                    val core = py.getModule("proguin.core")
                    val pagesPath = File(context.filesDir, "pages.json").absolutePath
                    val pages = core.callAttr("load_pages", pagesPath)
                    core.callAttr("mark_task_done_by_id", pages, taskId)
                    core.callAttr("save_pages", pagesPath, pages)
                } catch (_: Exception) { }

                TimerForegroundService.stopTimer(context)
                NotificationHelper.showReminder(context, "Completed ✅", taskName.ifBlank { "Task" })
                sendPagesUpdated(context)
            }
        }
    }

    private fun sendPagesUpdated(context: Context) {
        // Make it explicit (reduces lint warnings, more secure)
        val i = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(i)
    }
}

