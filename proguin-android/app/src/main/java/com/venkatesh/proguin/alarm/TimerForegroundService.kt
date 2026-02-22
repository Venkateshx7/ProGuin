package com.venkatesh.proguin.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.venkatesh.proguin.data.StatsStore
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

class TimerForegroundService : Service() {

    companion object {
        private const val ACTION_START = "com.venkatesh.proguin.TIMER_START"
        private const val ACTION_STOP = "com.venkatesh.proguin.TIMER_STOP"
        private const val ACTION_PAUSE = "com.venkatesh.proguin.TIMER_PAUSE"
        private const val ACTION_RESUME = "com.venkatesh.proguin.TIMER_RESUME"

        private const val EXTRA_TASK_ID = "taskId"
        private const val EXTRA_TASK_NAME = "taskName"
        private const val EXTRA_MINUTES = "minutes"
        private const val EXTRA_PAGE_ID = "pageId"

        private const val SP_NAME = "proguin_timer_state"
        private const val KEY_ACTIVE_TASK_ID = "active_task_id"

        private fun setActiveTaskId(context: Context, taskId: String) {
            context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_TASK_ID, taskId)
                .apply()
        }

        private fun clearActiveTaskId(context: Context) {
            context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_TASK_ID)
                .apply()
        }

        fun getActiveTaskId(context: Context): String {
            return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_TASK_ID, "")
                .orEmpty()
        }

        fun startTimer(context: Context, taskId: String, taskName: String, minutes: Int, pageId: String = "") {
            setActiveTaskId(context, taskId)

            val i = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_MINUTES, minutes)
                putExtra(EXTRA_PAGE_ID, pageId)
            }
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stopTimer(context: Context, taskId: String) {
            clearActiveTaskId(context)

            val i = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(i)
        }

        fun pauseTimer(context: Context, taskId: String, taskName: String) {
            val i = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
            }
            context.startService(i)
        }

        fun resumeTimer(context: Context, taskId: String, taskName: String) {
            setActiveTaskId(context, taskId)

            val i = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
            }
            context.startService(i)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null

    private var running = false
    private var remainingMs = 0L
    private var lastStartElapsed = 0L

    private var currentTaskId = ""
    private var currentTaskName = ""
    private var currentPageId = ""
    private var currentNotifId = 0
    private var currentMinutes = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val act = intent?.action.orEmpty()

        when (act) {

            ACTION_START -> {

                currentTaskId = intent?.getStringExtra(EXTRA_TASK_ID).orEmpty()
                currentTaskName = intent?.getStringExtra(EXTRA_TASK_NAME).orEmpty()
                currentPageId = intent?.getStringExtra(EXTRA_PAGE_ID).orEmpty()
                currentNotifId = NotificationHelper.timerNotifId(currentTaskId)

                // ✅ Read minutes first (so notification can show correct details)
                val minutes = intent?.getIntExtra(EXTRA_MINUTES, 0) ?: 0
                currentMinutes = minutes
                remainingMs = max(0, minutes) * 60_000L

                if (remainingMs <= 0L) {
                    clearActiveTaskId(this)
                    stopEverything(currentNotifId)
                    return START_NOT_STICKY
                }

                // Start foreground ASAP
                startForegroundNow(isRunning = true)

                updateNotification(isRunning = true)
                startTicking()
            }

            ACTION_STOP -> {
                val taskId = intent?.getStringExtra(EXTRA_TASK_ID).orEmpty()
                val notifId = NotificationHelper.timerNotifId(taskId.ifBlank { currentTaskId })

                clearActiveTaskId(this)
                stopEverything(notifId)
            }

            ACTION_PAUSE -> pauseInternal()

            ACTION_RESUME -> {
                if (currentTaskId.isNotBlank()) setActiveTaskId(this, currentTaskId)
                resumeInternal()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundNow(isRunning: Boolean) {
        NotificationHelper.ensureChannels(this)
        val text = formatRemaining(remainingMs)

        val notif = NotificationHelper.buildTimerNotification(
            context = this,
            notifId = currentNotifId,
            taskId = currentTaskId,
            taskName = currentTaskName,
            pageId = currentPageId,
            minutes = currentMinutes,
            contentText = if (isRunning) "Running" else "Paused",
            isRunning = isRunning,
            remainingMs = remainingMs,
            totalMs = (currentMinutes.coerceAtLeast(0) * 60_000L)
        )

        try {
            startForeground(currentNotifId, notif)
        } catch (_: Exception) {
            stopEverything(currentNotifId)
        }
    }

    private fun updateNotification(isRunning: Boolean) {
        val text = formatRemaining(remainingMs)

        val notif = NotificationHelper.buildTimerNotification(
            context = this,
            notifId = currentNotifId,
            taskId = currentTaskId,
            taskName = currentTaskName,
            pageId = currentPageId,
            minutes = currentMinutes,
            contentText = if (isRunning) "Running" else "Paused",
            isRunning = isRunning,
            remainingMs = remainingMs,
            totalMs = (currentMinutes.coerceAtLeast(0) * 60_000L)
        )

        NotificationHelper.safeNotify(this, currentNotifId, notif)
    }

    private fun startTicking() {
        tickJob?.cancel()
        running = true
        lastStartElapsed = SystemClock.elapsedRealtime()

        tickJob = scope.launch {
            while (isActive && running) {
                delay(1000)

                val now = SystemClock.elapsedRealtime()
                val passed = now - lastStartElapsed
                lastStartElapsed = now

                remainingMs = (remainingMs - passed).coerceAtLeast(0L)

                updateNotification(isRunning = true)

                if (remainingMs <= 0L) {

                    clearActiveTaskId(this@TimerForegroundService)

                    try {
                        NotificationHelper.showReminder(
                            context = this@TimerForegroundService,
                            title = "Time’s up ⏰",
                            message = currentTaskName.ifBlank { "Session ended • Take a break" }
                        )
                    } catch (_: Exception) { }

                    // ✅ Mark task DONE automatically + generate next recurring if configured
                    val nextInfo = tryCompleteAndGenerateNext()

                    // ✅ Stats (Dashboard + XP)
                    try {
                        val stats = StatsStore(this@TimerForegroundService)
                        stats.addCompletion(currentTaskName)
                        if (currentMinutes > 0) stats.addFocusMinutes(currentMinutes)
                    } catch (_: Exception) { }

                    // ✅ Schedule next recurring alarm (if any)
                    if (nextInfo != null) {
                        tryScheduleNext(nextInfo)
                    }

                    // Refresh UI
                    try {
                        sendBroadcast(
                            Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                                setPackage(packageName)
                            }
                        )
                    } catch (_: Exception) { }

                    stopEverything(currentNotifId)
                    break
                }
            }
        }
    }

    private data class NextTaskInfo(
        val id: String,
        val name: String,
        val minutes: Int,
        val scheduledIso: String
    )

    private fun tryCompleteAndGenerateNext(): NextTaskInfo? {
        return try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this@TimerForegroundService))
            }
            val py = Python.getInstance()
            val core = py.getModule("proguin.core")
            val pagesPath = File(filesDir, "pages.json").absolutePath
            val pages = core.callAttr("load_pages", pagesPath)

            val nextTask = core.callAttr("complete_task_and_generate_next", pages, currentTaskId)
            core.callAttr("save_pages", pagesPath, pages)

            if (nextTask == null || nextTask.toString() == "None") return null

            val m = nextTask.asMap()
            val id = m[com.chaquo.python.PyObject.fromJava("id")]?.toString().orEmpty()
            val name = m[com.chaquo.python.PyObject.fromJava("name")]?.toString().orEmpty()
            val minutesAny = m[com.chaquo.python.PyObject.fromJava("timer_minutes")]?.toString().orEmpty()
            val minutes = minutesAny.toIntOrNull() ?: 0
            val sched = m[com.chaquo.python.PyObject.fromJava("scheduled_start")]?.toString().orEmpty()
            if (id.isBlank() || sched.isBlank()) null else NextTaskInfo(id, name, minutes, sched)
        } catch (_: Exception) {
            // fallback: just complete
            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(this@TimerForegroundService))
                }
                val py = Python.getInstance()
                val core = py.getModule("proguin.core")
                val pagesPath = File(filesDir, "pages.json").absolutePath
                val pages = core.callAttr("load_pages", pagesPath)
                core.callAttr("complete_task_by_id", pages, currentTaskId)
                core.callAttr("save_pages", pagesPath, pages)
            } catch (_: Exception) { }
            null
        }
    }

    private fun tryScheduleNext(next: NextTaskInfo) {
        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val dt = fmt.parse(next.scheduledIso) ?: return
            val ms = dt.time
            if (ms <= System.currentTimeMillis()) return

            // ✅ Multiple schedules safe request code
            val req = taskScheduleToRequestCode(currentPageId.ifBlank { "default" }, next.id, ms)
            AlarmScheduler.scheduleAllowWhileIdle(
                context = this,
                requestCode = req,
                triggerAtMillis = ms,
                pageId = currentPageId.ifBlank { "default" },
                taskId = next.id,
                taskName = next.name,
                timerMinutes = next.minutes
            )
        } catch (_: Exception) { }
    }

    private fun taskScheduleToRequestCode(pageId: String, taskId: String, triggerAtMillis: Long): Int {
        val raw = "$pageId::$taskId::$triggerAtMillis"
        var h = 7
        for (c in raw) {
            h = 31 * h + c.code
        }
        return h
    }

    private fun pauseInternal() {
        if (!running) return
        running = false
        tickJob?.cancel()
        updateNotification(isRunning = false)
    }

    private fun resumeInternal() {
        if (running) return
        startForegroundNow(isRunning = true)
        startTicking()
    }

    private fun stopEverything(notifId: Int) {
        running = false
        tickJob?.cancel()
        tickJob = null

        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) { }
        try { NotificationManagerCompat.from(this).cancel(notifId) } catch (_: Exception) { }

        stopSelf()
    }

    private fun formatRemaining(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
