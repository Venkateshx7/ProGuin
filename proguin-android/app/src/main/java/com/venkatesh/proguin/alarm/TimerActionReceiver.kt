package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action.orEmpty()
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID).orEmpty()
        val taskName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_NAME).orEmpty()
        val notifId = NotificationHelper.timerNotifId(taskId)

        when (action) {

            NotificationHelper.ACTION_TIMER_STOP -> {
                stopEverywhere(context, taskId, notifId)
            }

            NotificationHelper.ACTION_TIMER_DONE -> {
                doneEverywhere(context, taskId, notifId)
            }

            NotificationHelper.ACTION_TIMER_PAUSE -> {
                TimerForegroundService.pauseTimer(context, taskId, taskName)
                sendRefresh(context)
            }

            NotificationHelper.ACTION_TIMER_RESUME -> {
                TimerForegroundService.resumeTimer(context, taskId, taskName)
                sendRefresh(context)
            }
        }
    }

    private fun stopEverywhere(context: Context, taskId: String, notifId: Int) {
        TimerForegroundService.stopTimer(context, taskId)
        NotificationHelper.cancelNotificationById(context, notifId)

        try {
            val py = com.chaquo.python.Python.getInstance()
            val core = py.getModule("proguin.core")
            val pagesPath = File(context.filesDir, "pages.json").absolutePath
            val pages = core.callAttr("load_pages", pagesPath)

            try {
                core.callAttr("stop_task_by_id", pages, taskId)
            } catch (_: Exception) {
                try { core.callAttr("set_task_running", pages, taskId, false) } catch (_: Exception) { }
            }

            core.callAttr("save_pages", pagesPath, pages)
        } catch (_: Exception) {
        }

        sendRefresh(context)
    }

    private fun doneEverywhere(context: Context, taskId: String, notifId: Int) {
        TimerForegroundService.stopTimer(context, taskId)
        NotificationHelper.cancelNotificationById(context, notifId)

        try {
            val py = com.chaquo.python.Python.getInstance()
            val core = py.getModule("proguin.core")
            val pagesPath = File(context.filesDir, "pages.json").absolutePath
            val pages = core.callAttr("load_pages", pagesPath)

            try {
                core.callAttr("complete_task_by_id", pages, taskId)
            } catch (_: Exception) {
                try { core.callAttr("set_task_running", pages, taskId, false) } catch (_: Exception) { }
            }

            core.callAttr("save_pages", pagesPath, pages)
        } catch (_: Exception) {
        }

        sendRefresh(context)
    }

    private fun sendRefresh(context: Context) {
        val updateIntent = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(updateIntent)
    }
}
