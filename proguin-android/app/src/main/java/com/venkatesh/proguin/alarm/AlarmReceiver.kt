package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    private fun norm(s: String): String = s.replace("'", "")

    private fun findPageIdForTask(pages: PyObject, taskId: String): String {
        return try {
            val pagesMap = pages.asMap()
            val container = pagesMap[PyObject.fromJava("pages")]?.asMap() ?: emptyMap()

            for ((k, v) in container) {
                val pageId = norm(k.toString())
                val pageMap = v.asMap()
                val tasksObj = pageMap[PyObject.fromJava("tasks")]
                val tasks = try { tasksObj?.asList() ?: emptyList() } catch (_: Exception) { emptyList() }

                for (t in tasks) {
                    try {
                        val tm = t.asMap()
                        val id = tm[PyObject.fromJava("id")]?.toString().orEmpty()
                        if (id == taskId) return pageId
                    } catch (_: Exception) { }
                }
            }

            ""
        } catch (_: Exception) {
            ""
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProGuin:AlarmWakeLock")
        wl.acquire(10_000L)

        try {
            val incomingPageId = intent.getStringExtra("pageId").orEmpty()
            val taskId = intent.getStringExtra("taskId").orEmpty()
            val taskName = intent.getStringExtra("taskName").orEmpty()
            val timerMinutes = intent.getIntExtra("timerMinutes", 0)
            val expectedTrigger = intent.getLongExtra("triggerAtMillis", 0L)

            NotificationHelper.ensureChannels(context)

            try {
                if (!Python.isStarted()) Python.start(AndroidPlatform(context))
            } catch (_: Exception) { }

            val actualPageId = try {
                val py = Python.getInstance()
                val core = py.getModule("proguin.core")
                val pagesPath = File(context.filesDir, "pages.json").absolutePath
                val pages = core.callAttr("load_pages", pagesPath)

                // ✅ Ignore stale alarms:
                // If task's current scheduled_start doesn't match this alarm time, do nothing.
                // Also ignore if task already completed.
                try {
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    val pagesMap = pages.asMap()
                    val container = pagesMap[PyObject.fromJava("pages")]?.asMap() ?: emptyMap()

                    fun parseMs(s: String): Long {
                        return try { fmt.parse(s)?.time ?: 0L } catch (_: Exception) { 0L }
                    }

                    var foundCompleted = false
                    var foundScheduledMs = 0L

                    for ((_, v) in container) {
                        val pageMap = v.asMap()
                        val tasksObj = pageMap[PyObject.fromJava("tasks")]
                        val tasks = try { tasksObj?.asList() ?: emptyList() } catch (_: Exception) { emptyList() }
                        for (t in tasks) {
                            try {
                                val tm = t.asMap()
                                val id = tm[PyObject.fromJava("id")]?.toString().orEmpty()
                                if (id == taskId) {
                                    foundCompleted = tm[PyObject.fromJava("completed")]?.toString() == "True"
                                    val schedStr = tm[PyObject.fromJava("scheduled_start")]?.toString().orEmpty().replace("'", "")
                                    if (schedStr.isNotBlank() && schedStr != "None") {
                                        foundScheduledMs = parseMs(schedStr)
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }

                    if (foundCompleted) {
                        return
                    }

                    if (expectedTrigger > 0L && foundScheduledMs > 0L) {
                        val diff = kotlin.math.abs(foundScheduledMs - expectedTrigger)
                        // allow small drift (doze) but ignore large mismatch
                        if (diff > 2 * 60_000L) {
                            return
                        }
                    }

                } catch (_: Exception) { }

                var resolved = incomingPageId
                if (resolved.isBlank() && taskId.isNotBlank()) {
                    resolved = findPageIdForTask(pages, taskId)
                }
                if (resolved.isBlank()) resolved = "default"

                // ✅ Make sure current_page is correct BEFORE notification click opens app
                try {
                    val pagesMap = pages.asMap()
                    pagesMap[PyObject.fromJava("current_page")] = PyObject.fromJava(resolved)
                } catch (_: Exception) { }

                // ✅ Mark task as started/running in correct page
                try {
                    core.callAttr("start_task_by_id", pages, taskId)
                } catch (_: Exception) {
                    try { core.callAttr("set_task_running", pages, taskId, true) } catch (_: Exception) { }
                }

                core.callAttr("save_pages", pagesPath, pages)

                resolved
            } catch (_: Exception) {
                if (incomingPageId.isNotBlank()) incomingPageId else "default"
            }

            // ✅ Now notification click will open correct pageId always
            try {
                // ✅ Better UX: show a scheduled notification WITH a Start action
                // User tap => Android allows starting FGS reliably.
                NotificationHelper.showScheduledWithStartAction(
                    context = context,
                    title = "New Quest Available",
                    message = taskName.ifBlank { "Task" },
                    pageId = actualPageId,
                    taskId = taskId,
                    taskName = taskName.ifBlank { "Task" },
                    timerMinutes = timerMinutes
                )
            } catch (_: Exception) { }

            /**
             * NOTE (important truth):
             * On Android 14/15/16 (your logs show targetSdk 36),
             * starting a Foreground Service from a BroadcastReceiver in background can be DENIED.
             * That’s why you see:
             * "Background started FGS: Disallowed"
             *
             * So auto-start timer notification AFTER app is killed may not be allowed.
             * We can still start timer when user taps the notification (user-initiated -> allowed).
             */
            // ✅ Best effort: auto-start timer if OS allows. If denied, we keep a pending start.
            var autoStarted = false
            try {
                if (timerMinutes > 0 && taskId.isNotBlank()) {
                    TimerForegroundService.startTimer(
                        context = context,
                        taskId = taskId,
                        taskName = taskName.ifBlank { "Task" },
                        minutes = timerMinutes,
                        pageId = actualPageId
                    )
                    autoStarted = true
                }
            } catch (_: Exception) {
            }

            if (!autoStarted) {
                try {
                    // store pending autostart for when app opens (user taps notification)
                    val sp = context.getSharedPreferences("proguin_pending_timer", Context.MODE_PRIVATE)
                    sp.edit()
                        .putString("taskId", taskId)
                        .putString("taskName", taskName)
                        .putString("pageId", actualPageId)
                        .putInt("minutes", timerMinutes)
                        .apply()
                } catch (_: Exception) { }
            }

            try {
                context.sendBroadcast(
                    Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                        setPackage(context.packageName)
                    }
                )
            } catch (_: Exception) { }

        } finally {
            try { wl.release() } catch (_: Exception) { }
        }
    }
}