package com.venkatesh.proguin.alarm

import android.content.Context
import android.content.Intent
import java.io.File

object TaskRuntime {

    fun start(context: Context, taskId: String, taskName: String, minutes: Int) {
        // pages.json -> running
        try {
            val py = com.chaquo.python.Python.getInstance()
            val core = py.getModule("proguin.core")
            val pagesPath = File(context.filesDir, "pages.json").absolutePath

            val pages = core.callAttr("load_pages", pagesPath)
            core.callAttr("start_task_by_id", pages, taskId)
            core.callAttr("save_pages", pagesPath, pages)
        } catch (_: Exception) {
        }

        if (minutes > 0) {
            TimerForegroundService.startTimer(context, taskId, taskName, minutes)
        }

        refresh(context)
    }

    fun stop(context: Context, taskId: String) {
        TimerForegroundService.stopTimer(context, taskId)
        NotificationHelper.cancelTimerNotification(context, taskId)

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

        refresh(context)
    }

    private fun refresh(context: Context) {
        val updateIntent = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(updateIntent)
    }
}
