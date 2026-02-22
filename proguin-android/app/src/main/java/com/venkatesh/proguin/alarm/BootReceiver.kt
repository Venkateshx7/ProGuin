package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val act = intent?.action.orEmpty()
        if (act != Intent.ACTION_BOOT_COMPLETED && act != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        try {
            val pagesFile = File(context.filesDir, "pages.json")
            if (!pagesFile.exists()) return
            val raw = pagesFile.readText(Charsets.UTF_8).trim()
            if (raw.isBlank()) return

            val obj = JSONObject(raw)
            val pages = obj.optJSONObject("pages") ?: return
            val now = System.currentTimeMillis()

            val inFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

            val it = pages.keys()
            while (it.hasNext()) {
                val pageId = it.next()
                val pageObj = pages.optJSONObject(pageId) ?: continue
                val tasks = pageObj.optJSONArray("tasks") ?: continue

                for (i in 0 until tasks.length()) {
                    val t = tasks.optJSONObject(i) ?: continue
                    val completed = t.optBoolean("completed", false)
                    val startedAt = t.optString("started_at", "")
                    val sched = t.optString("scheduled_start", "")

                    if (completed) continue
                    if (startedAt.isNotBlank()) continue
                    if (sched.isBlank() || sched == "None") continue

                    val dt = try { inFmt.parse(sched) } catch (_: Exception) { null } ?: continue
                    val ms = dt.time
                    if (ms <= now) continue

                    val taskId = t.optString("id", "")
                    val taskName = t.optString("name", "Task")
                    val minutes = if (t.has("timer_minutes") && !t.isNull("timer_minutes")) t.optInt("timer_minutes", 0) else 0

                    // ✅ Multiple schedules support: include time + pageId in requestCode
                    val req = taskScheduleToRequestCode(pageId, taskId, ms)
                    AlarmScheduler.scheduleAllowWhileIdle(
                        context = context,
                        requestCode = req,
                        triggerAtMillis = ms,
                        pageId = pageId,
                        taskId = taskId,
                        taskName = taskName,
                        timerMinutes = minutes
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun taskScheduleToRequestCode(pageId: String, taskId: String, triggerAtMillis: Long): Int {
        // stable & collision-resistant for multiple alarms
        val raw = "$pageId::$taskId::$triggerAtMillis"
        var h = 7
        for (c in raw) {
            h = 31 * h + c.code
        }
        return h
    }
}
