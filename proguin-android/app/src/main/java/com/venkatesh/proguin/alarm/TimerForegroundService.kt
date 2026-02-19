package com.venkatesh.proguin.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
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

        fun startTimer(context: Context, taskId: String, taskName: String, minutes: Int) {
            setActiveTaskId(context, taskId)

            val i = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_MINUTES, minutes)
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
    private var currentNotifId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val act = intent?.action.orEmpty()

        when (act) {

            ACTION_START -> {

                // ✅ Set ids first (fast)
                currentTaskId = intent?.getStringExtra(EXTRA_TASK_ID).orEmpty()
                currentTaskName = intent?.getStringExtra(EXTRA_TASK_NAME).orEmpty()
                currentNotifId = NotificationHelper.timerNotifId(currentTaskId)

                // ✅ Call startForeground ASAP (prevents ForegroundServiceDidNotStartInTimeException)
                // NOTE: remainingMs may be 0 initially, we will stop immediately after if needed.
                remainingMs = 1_000L
                startForegroundNow(isRunning = true)

                val minutes = intent?.getIntExtra(EXTRA_MINUTES, 0) ?: 0
                remainingMs = max(0, minutes) * 60_000L

                // ✅ If minutes is 0, do not keep foreground service alive
                if (remainingMs <= 0L) {
                    clearActiveTaskId(this)
                    stopEverything(currentNotifId)
                    return START_NOT_STICKY
                }

                // ✅ Ensure notification updates to correct time right away
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
            contentText = if (isRunning) "Running • $text" else "Paused • $text",
            isRunning = isRunning
        )

        try {
            startForeground(currentNotifId, notif)
        } catch (_: Exception) {
            // if foreground fails, stop to avoid crash loop
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
            contentText = if (isRunning) "Running • $text" else "Paused • $text",
            isRunning = isRunning
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

                    // ✅ one-time sound via reminders channel
                    try {
                        NotificationHelper.showReminder(
                            context = this@TimerForegroundService,
                            title = "Time’s up ⏰",
                            message = currentTaskName.ifBlank { "Work ended • Take a break" }
                        )
                    } catch (_: Exception) { }

                    // ✅ Update pages.json: stop running
                    try {
                        val py = com.chaquo.python.Python.getInstance()
                        val core = py.getModule("proguin.core")
                        val pagesPath = java.io.File(filesDir, "pages.json").absolutePath
                        val pages = core.callAttr("load_pages", pagesPath)

                        try {
                            core.callAttr("stop_task_by_id", pages, currentTaskId)
                        } catch (_: Exception) {
                            try { core.callAttr("set_task_running", pages, currentTaskId, false) } catch (_: Exception) { }
                        }

                        core.callAttr("save_pages", pagesPath, pages)
                    } catch (_: Exception) { }

                    // ✅ Refresh UI
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
