package com.venkatesh.proguin.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlin.math.max
import android.content.pm.ServiceInfo
import android.os.Build

class TimerForegroundService : Service() {

    private var timer: CountDownTimer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra("taskId").orEmpty()
        val taskName = intent?.getStringExtra("taskName").orEmpty()
        val endAt = intent?.getLongExtra("endAtMillis", 0L) ?: 0L

        NotificationHelper.ensureChannels(this)

        val remaining = max(0L, endAt - System.currentTimeMillis())
        val title = "Timer Running ⏳"
        val text = "$taskName • ${formatMs(remaining)} left"

        // ✅ Android 15/16 may block startForeground in some situations.
        // We must not crash the whole app.
        val notif = NotificationHelper.buildTimerNotification(this, title, text)

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NotificationHelper.ID_TIMER,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                )
            } else {
                startForeground(NotificationHelper.ID_TIMER, notif)
            }
        } catch (e: Exception) {
            NotificationHelper.showReminder(
                this,
                "Timer can't run in background",
                "Open the app to continue the timer."
            )
            stopSelf()
            return START_NOT_STICKY
        }

        timer?.cancel()
        timer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(ms: Long) {
                NotificationHelper.updateTimer(
                    this@TimerForegroundService,
                    title,
                    "$taskName • ${formatMs(ms)} left"
                )
            }

            override fun onFinish() {
                NotificationHelper.cancelTimer(this@TimerForegroundService)
                NotificationHelper.showReminder(
                    this@TimerForegroundService,
                    "Timer Done ✅",
                    taskName.ifBlank { "Task" }
                )
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        NotificationHelper.cancelTimer(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    companion object {
        fun startTimer(context: Context, taskId: String, taskName: String, minutes: Int) {
            val endAt = System.currentTimeMillis() + minutes * 60_000L
            val i = Intent(context, TimerForegroundService::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("taskName", taskName)
                putExtra("endAtMillis", endAt)
            }
            // ✅ safest way
            ContextCompat.startForegroundService(context, i)
        }

        fun stopTimer(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
            NotificationHelper.cancelTimer(context)
        }
    }
}
