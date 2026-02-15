package com.venkatesh.proguin.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat

class TimerForegroundService : Service() {

    private var taskId: String = ""
    private var taskName: String = ""
    private var totalSeconds: Int = 0
    private var secondsLeft: Int = 0
    private var running: Boolean = false

    private var timer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)

        when (intent?.action) {
            ACT_START -> {
                taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
                taskName = intent.getStringExtra(EXTRA_TASK_NAME).orEmpty()
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
                if (minutes <= 0 || taskId.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                totalSeconds = minutes * 60
                secondsLeft = totalSeconds
                running = true
                persistState(active = true)
                startAsForeground()
                startCountdown()
            }

            ACT_PAUSE -> pauseInternal()
            ACT_RESUME -> resumeInternal()
            ACT_STOP -> stopInternal()
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        val notifText = formatTime(secondsLeft)
        val notification = NotificationHelper.buildTimerNotification(
            context = this,
            notifId = NOTIF_ID,
            taskId = taskId,
            taskName = taskName.ifBlank { "Timer" },
            contentText = "Remaining: $notifText",
            isRunning = running
        )
        startForeground(NOTIF_ID, notification)
    }

    private fun updateNotification() {
        val notifText = formatTime(secondsLeft)
        val notification = NotificationHelper.buildTimerNotification(
            context = this,
            notifId = NOTIF_ID,
            taskId = taskId,
            taskName = taskName.ifBlank { "Timer" },
            contentText = if (running) "Remaining: $notifText" else "Paused: $notifText",
            isRunning = running
        )
        NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
    }

    private fun startCountdown() {
        timer?.cancel()
        timer = object : CountDownTimer(secondsLeft * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                secondsLeft = (ms / 1000L).toInt().coerceAtLeast(0)
                persistState(active = true)
                updateNotification()
            }

            override fun onFinish() {
                secondsLeft = 0
                running = false
                updateNotification()
                persistState(active = false)

                NotificationHelper.showReminder(
                    context = this@TimerForegroundService,
                    title = "Timer done ✅",
                    message = taskName.ifBlank { "Task" }
                )

                sendBroadcast(Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                    setPackage(packageName)
                })

                stopInternal()
            }
        }.start()
    }

    private fun pauseInternal() {
        if (!running) return
        running = false
        timer?.cancel()
        persistState(active = true)
        updateNotification()
    }

    private fun resumeInternal() {
        if (running) return
        if (secondsLeft <= 0) return
        running = true
        persistState(active = true)
        updateNotification()
        startCountdown()
    }

    private fun stopInternal() {
        timer?.cancel()
        running = false
        secondsLeft = 0
        persistState(active = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun persistState(active: Boolean) {
        val p = getSharedPreferences(PREF, Context.MODE_PRIVATE)
        p.edit()
            .putBoolean(KEY_ACTIVE, active)
            .putString(KEY_TASK_ID, if (active) taskId else "")
            .putString(KEY_TASK_NAME, if (active) taskName else "")
            .putInt(KEY_TOTAL_SEC, totalSeconds)
            .putInt(KEY_LEFT_SEC, secondsLeft)
            .putBoolean(KEY_RUNNING, running)
            .apply()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    private fun formatTime(sec: Int): String {
        val m = (sec / 60).coerceAtLeast(0)
        val s = (sec % 60).coerceAtLeast(0)
        return "%02d:%02d".format(m, s)
    }

    companion object {
        private const val PREF = "proguin_timer"
        private const val KEY_ACTIVE = "active"
        private const val KEY_TASK_ID = "task_id"
        private const val KEY_TASK_NAME = "task_name"
        private const val KEY_TOTAL_SEC = "total_sec"
        private const val KEY_LEFT_SEC = "left_sec"
        private const val KEY_RUNNING = "running"

        private const val NOTIF_ID = 4117

        private const val ACT_START = "ACT_START"
        private const val ACT_PAUSE = "ACT_PAUSE"
        private const val ACT_RESUME = "ACT_RESUME"
        private const val ACT_STOP = "ACT_STOP"

        private const val EXTRA_TASK_ID = "taskId"
        private const val EXTRA_TASK_NAME = "taskName"
        private const val EXTRA_MINUTES = "minutes"

        fun startTimer(context: Context, taskId: String, taskName: String, minutes: Int) {
            val i = Intent(context, TimerForegroundService::class.java).apply {
                action = ACT_START
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_MINUTES, minutes)
            }
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun pauseTimer(context: Context) {
            val i = Intent(context, TimerForegroundService::class.java).apply { action = ACT_PAUSE }
            context.startService(i)
        }

        fun resumeTimer(context: Context) {
            val i = Intent(context, TimerForegroundService::class.java).apply { action = ACT_RESUME }
            context.startService(i)
        }

        fun stopTimer(context: Context) {
            val i = Intent(context, TimerForegroundService::class.java).apply { action = ACT_STOP }
            context.startService(i)
        }

        fun getActiveTaskId(context: Context): String {
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val active = p.getBoolean(KEY_ACTIVE, false)
            return if (active) p.getString(KEY_TASK_ID, "") ?: "" else ""
        }

        fun getTimerLeftSeconds(context: Context): Int {
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            return p.getInt(KEY_LEFT_SEC, 0)
        }

        fun isRunning(context: Context): Boolean {
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            return p.getBoolean(KEY_RUNNING, false)
        }
    }
}


