package com.venkatesh.proguin.alarm

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import java.io.File
import java.util.Locale

class TimerForegroundService : Service() {

    private var timer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID).orEmpty()
        val taskName = intent?.getStringExtra(EXTRA_TASK_NAME).orEmpty()
        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 0) ?: 0

        if (taskId.isBlank() || minutes <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        setActiveTaskId(this, taskId)
        sendPagesUpdated()

        val totalMs = minutes * 60_000L
        val firstNotif = buildNotif(taskId, taskName, totalMs)

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIF_ID,
                    firstNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                )
            } else {
                startForeground(NOTIF_ID, firstNotif)
            }
        } catch (_: Exception) {
            clearActiveTaskId(this)
            sendPagesUpdated()
            stopSelf()
            return START_NOT_STICKY
        }

        timer?.cancel()
        timer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(msLeft: Long) {
                val n = buildNotif(taskId, taskName, msLeft)
                NotificationHelper.safeNotify(this@TimerForegroundService, NOTIF_ID, n)
            }

            override fun onFinish() {
                // auto complete in pages.json
                try {
                    val py = com.chaquo.python.Python.getInstance()
                    val core = py.getModule("proguin.core")
                    val pagesPath = File(filesDir, "pages.json").absolutePath
                    val pages = core.callAttr("load_pages", pagesPath)
                    core.callAttr("mark_task_done_by_id", pages, taskId)
                    core.callAttr("save_pages", pagesPath, pages)
                } catch (_: Exception) { }

                NotificationHelper.showReminder(
                    context = this@TimerForegroundService,
                    title = "Completed ✅",
                    message = taskName.ifBlank { "Task" }
                )

                clearActiveTaskId(this@TimerForegroundService)
                sendPagesUpdated()

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        clearActiveTaskId(this)
        sendPagesUpdated()
        super.onDestroy()
    }

    private fun buildNotif(taskId: String, taskName: String, msLeft: Long): Notification {
        val sec = (msLeft / 1000L).toInt()
        val mm = sec / 60
        val ss = sec % 60
        val timeText = String.format(Locale.US, "%02d:%02d remaining", mm, ss)

        return NotificationHelper.buildTimerNotification(
            context = this,
            notifId = NOTIF_ID,
            taskId = taskId,
            taskName = taskName,
            contentText = timeText
        )
    }

    private fun sendPagesUpdated() {
        val i = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
            setPackage(packageName)
        }
        sendBroadcast(i)
    }

    companion object {
        private const val NOTIF_ID = 2001
        private const val EXTRA_TASK_ID = "taskId"
        private const val EXTRA_TASK_NAME = "taskName"
        private const val EXTRA_MINUTES = "minutes"

        private const val PREFS = "proguin_prefs"
        private const val KEY_ACTIVE_TASK_ID = "active_task_id"

        fun startTimer(context: Context, taskId: String, taskName: String, minutes: Int) {
            val i = Intent(context, TimerForegroundService::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_MINUTES, minutes)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            } catch (_: Exception) { }
        }

        fun stopTimer(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
            clearActiveTaskId(context)
            val i = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(i)
        }

        fun getActiveTaskId(context: Context): String {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_TASK_ID, "") ?: ""
        }

        private fun setActiveTaskId(context: Context, taskId: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_TASK_ID, taskId)
                .apply()
        }

        private fun clearActiveTaskId(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_TASK_ID)
                .apply()
        }
    }
}

