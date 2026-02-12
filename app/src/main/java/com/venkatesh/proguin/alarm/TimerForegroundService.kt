package com.venkatesh.proguin.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import kotlin.math.max

class TimerForegroundService : Service() {

    private var timer: CountDownTimer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra("taskId").orEmpty()
        val taskName = intent?.getStringExtra("taskName").orEmpty()
        val endAt = intent?.getLongExtra("endAtMillis", 0L) ?: 0L

        NotificationHelper.ensureChannels(this)

        val remaining = max(0L, endAt - System.currentTimeMillis())
        startForeground(
            NotificationHelper.ID_TIMER,
            NotificationHelper.buildTimerNotification(
                this,
                "Timer Running ⏳",
                "$taskName • ${formatMs(remaining)} left"
            )
        )

        timer?.cancel()
        timer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(ms: Long) {
                NotificationHelper.updateTimer(
                    this@TimerForegroundService,
                    "Timer Running ⏳",
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
            context.startForegroundService(i)
        }

        fun stopTimer(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
            NotificationHelper.cancelTimer(context)
        }
    }
}
