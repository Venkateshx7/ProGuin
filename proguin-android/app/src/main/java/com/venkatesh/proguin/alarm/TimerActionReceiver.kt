package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.venkatesh.proguin.data.StatsStore
import java.io.File

class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action.orEmpty()
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID).orEmpty()
        val taskName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_NAME).orEmpty()
        val pageId = intent.getStringExtra(NotificationHelper.EXTRA_PAGE_ID).orEmpty()
        val minutes = intent.getIntExtra(NotificationHelper.EXTRA_TIMER_MINUTES, 0)

        // Start python if needed
        try {
            if (!Python.isStarted()) Python.start(AndroidPlatform(context))
        } catch (_: Exception) { }

        when (action) {

            // ✅ NEW: user clicked "Start Timer" from scheduled notification
            NotificationHelper.ACTION_TIMER_START -> {

                // 1) switch correct page + mark running
                try {
                    val py = Python.getInstance()
                    val core = py.getModule("proguin.core")
                    val pagesPath = File(context.filesDir, "pages.json").absolutePath
                    val pages = core.callAttr("load_pages", pagesPath)
                    val pagesMap = pages.asMap()

                    if (pageId.isNotBlank()) {
                        pagesMap[PyObject.fromJava("current_page")] = PyObject.fromJava(pageId)
                    }

                    try {
                        core.callAttr("start_task_by_id", pages, taskId)
                    } catch (_: Exception) {
                        try { core.callAttr("set_task_running", pages, taskId, true) } catch (_: Exception) { }
                    }

                    core.callAttr("save_pages", pagesPath, pages)
                } catch (_: Exception) { }

                // 2) start timer FGS (allowed because user clicked notification)
                try {
                    if (minutes > 0 && taskId.isNotBlank()) {
                        TimerForegroundService.startTimer(
                            context = context,
                            taskId = taskId,
                            taskName = taskName.ifBlank { "Task" },
                            minutes = minutes,
                            pageId = pageId
                        )
                    }
                } catch (_: Exception) { }

                // 3) update UI
                try {
                    context.sendBroadcast(
                        Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                            setPackage(context.packageName)
                        }
                    )
                } catch (_: Exception) { }
            }

            NotificationHelper.ACTION_TIMER_STOP -> {
                // ✅ Stop timer + stop running state in pages.json (prevents "still running" glitch)
                try { TimerForegroundService.stopTimer(context, taskId) } catch (_: Exception) { }
                try { NotificationHelper.cancelTimerNotification(context, taskId) } catch (_: Exception) { }
                try { TaskRuntime.stop(context, taskId) } catch (_: Exception) { }
            }

            NotificationHelper.ACTION_TIMER_DONE -> {
                try { TimerForegroundService.stopTimer(context, taskId) } catch (_: Exception) { }
                try { NotificationHelper.cancelTimerNotification(context, taskId) } catch (_: Exception) { }

                // Optional: mark done in python if you want (safe attempt)
                try {
                    val py = Python.getInstance()
                    val core = py.getModule("proguin.core")
                    val pagesPath = File(context.filesDir, "pages.json").absolutePath
                    val pages = core.callAttr("load_pages", pagesPath)

                    try { core.callAttr("mark_task_done_by_id", pages, taskId) } catch (_: Exception) { }
                    core.callAttr("save_pages", pagesPath, pages)
                } catch (_: Exception) { }

                // ✅ Stats (XP / Level up)
                try {
                    val stats = StatsStore(context)
                    stats.addCompletion(taskName)
                    if (minutes > 0) stats.addFocusMinutes(minutes)
                } catch (_: Exception) { }

                try {
                    NotificationHelper.showReminder(
                        context = context,
                        title = "Completed ✅",
                        message = taskName.ifBlank { "Task" },
                        pageId = pageId,
                        taskId = taskId,
                        tab = "completed"
                    )
                } catch (_: Exception) { }

                try {
                    context.sendBroadcast(
                        Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                            setPackage(context.packageName)
                        }
                    )
                } catch (_: Exception) { }
            }

            // Pause/Resume:
            // If your TimerForegroundService supports these actions internally, you can wire it later.
            // For now, no crash.
            NotificationHelper.ACTION_TIMER_PAUSE -> {
                try { TimerForegroundService.pauseTimer(context, taskId, taskName) } catch (_: Exception) { }
            }

            NotificationHelper.ACTION_TIMER_RESUME -> {
                try { TimerForegroundService.resumeTimer(context, taskId, taskName) } catch (_: Exception) { }
            }
        }
    }
}