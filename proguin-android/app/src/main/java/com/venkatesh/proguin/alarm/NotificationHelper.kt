package com.venkatesh.proguin.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.venkatesh.proguin.MainActivity
import com.venkatesh.proguin.R
import com.venkatesh.proguin.data.SettingsStore
import kotlin.math.abs

object NotificationHelper {

    const val EXTRA_TIMER_MINUTES = "timerMinutes"

    const val CH_REMINDERS = "proguin_reminders"
    const val CH_TIMER = "proguin_timer"

    // Timer actions
    const val ACTION_TIMER_STOP = "com.venkatesh.proguin.ACTION_TIMER_STOP"
    const val ACTION_TIMER_DONE = "com.venkatesh.proguin.ACTION_TIMER_DONE"
    const val ACTION_TIMER_PAUSE = "com.venkatesh.proguin.ACTION_TIMER_PAUSE"
    const val ACTION_TIMER_RESUME = "com.venkatesh.proguin.ACTION_TIMER_RESUME"

    // user-click start (works even when app is closed)
    const val ACTION_TIMER_START = "com.venkatesh.proguin.ACTION_TIMER_START"

    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_TASK_NAME = "taskName"
    const val EXTRA_PAGE_ID = "pageId"

    // stable notification id per task
    private const val TIMER_NOTIF_BASE = 20000

    fun timerNotifId(taskId: String): Int {
        val h = abs(taskId.hashCode())
        return TIMER_NOTIF_BASE + (h % 10000)
    }

    fun cancelNotificationById(context: Context, id: Int) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(id)
        } catch (_: Exception) { }
    }

    fun cancelTimerNotification(context: Context, taskId: String) {
        cancelNotificationById(context, timerNotifId(taskId))
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Reminders: sound/vibration allowed
        if (nm.getNotificationChannel(CH_REMINDERS) == null) {
            val ch = NotificationChannel(
                CH_REMINDERS,
                "Solo Leveling • Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(ch)
        }

        // Timers: silent ongoing
        if (nm.getNotificationChannel(CH_TIMER) == null) {
            val ch = NotificationChannel(
                CH_TIMER,
                "Solo Leveling • Timers",
                NotificationManager.IMPORTANCE_LOW
            )
            ch.setSound(null, null)
            ch.enableVibration(false)
            ch.enableLights(false)
            nm.createNotificationChannel(ch)
        }
    }

    private fun openTasksPendingIntent(
        context: Context,
        pageId: String = "",
        taskId: String = "",
        tab: String = ""
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("nav_target", "tasks")
            putExtra("nav_page_id", pageId)
            putExtra("nav_task_id", taskId)
            putExtra("nav_tab", tab)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

        val req = 1000 + abs((pageId + "|" + taskId + "|" + tab).hashCode() % 8000)
        return PendingIntent.getActivity(context, req, intent, flags)
    }

    fun showReminder(
        context: Context,
        title: String,
        message: String,
        pageId: String = "",
        taskId: String = "",
        tab: String = ""
    ) {
        ensureChannels(context)

        val settings = SettingsStore(context)

        val b = NotificationCompat.Builder(context, CH_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚔️ $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openTasksPendingIntent(context, pageId = pageId, taskId = taskId, tab = tab))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (!settings.soundEnabled()) b.setSilent(true)
        if (settings.vibrationEnabled()) b.setVibrate(longArrayOf(0, 120, 80, 120))

        safeNotify(context, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), b.build())
    }

    // Scheduled reminder with Start action
    fun showScheduledWithStartAction(
        context: Context,
        title: String,
        message: String,
        pageId: String,
        taskId: String,
        taskName: String,
        timerMinutes: Int
    ) {
        ensureChannels(context)

        val settings = SettingsStore(context)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

        val actionIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = ACTION_TIMER_START
            putExtra(EXTRA_PAGE_ID, pageId)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_NAME, taskName)
            putExtra(EXTRA_TIMER_MINUTES, timerMinutes)
        }

        val actionReq = 12000 + abs((pageId + "::" + taskId).hashCode() % 5000)
        val piStart = PendingIntent.getBroadcast(context, actionReq, actionIntent, flags)

        val b = NotificationCompat.Builder(context, CH_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🟣 SYSTEM: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openTasksPendingIntent(context, pageId = pageId, taskId = taskId, tab = "scheduled"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "⚔️ Start Quest", piStart)

        if (!settings.soundEnabled()) b.setSilent(true)
        if (settings.vibrationEnabled()) b.setVibrate(longArrayOf(0, 120, 80, 120))

        val notifId = 30000 + abs((pageId + "::" + taskId).hashCode() % 9000)
        safeNotify(context, notifId, b.build())
    }

    /**
     * ✅ Solo Leveling notification style (RemoteViews):
     * - Big "Remaining time" font
     * - Neon bar + SYSTEM label
     * - Buttons inside notification (Pause/Resume, Stop, Clear)
     *
     * NOTE: Android may still restrict auto-start from background for scheduled alarms.
     * This UI makes it always reliable: user taps Start/Pause/Stop.
     */
    fun buildTimerNotification(
        context: Context,
        notifId: Int,
        taskId: String,
        taskName: String,
        pageId: String,
        minutes: Int,
        contentText: String,
        isRunning: Boolean,
        remainingMs: Long,
        totalMs: Long
    ): Notification {
        ensureChannels(context)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

        fun actionPI(action: String, requestCode: Int): PendingIntent {
            val i = Intent(context, TimerActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_PAGE_ID, pageId)
                putExtra(EXTRA_TIMER_MINUTES, minutes)
            }
            return PendingIntent.getBroadcast(context, requestCode, i, flags)
        }

        val piStop = actionPI(ACTION_TIMER_STOP, 9001 + notifId)
        val piDone = actionPI(ACTION_TIMER_DONE, 9002 + notifId)
        val piPause = actionPI(ACTION_TIMER_PAUSE, 9003 + notifId)
        val piResume = actionPI(ACTION_TIMER_RESUME, 9004 + notifId)

        val title = taskName.ifBlank { "Quest" }

        val remainingText = formatRemaining(remainingMs)
        val progressPercent = if (totalMs <= 0L) 0 else {
            val done = (totalMs - remainingMs).coerceIn(0L, totalMs)
            ((done * 100L) / totalMs).toInt().coerceIn(0, 100)
        }

        val rvSmall = RemoteViews(context.packageName, R.layout.notification_solo_timer).apply {
            setTextViewText(R.id.soloTitle, title)
            setTextViewText(R.id.soloRemaining, remainingText)
            setTextViewText(R.id.soloSystemLine, "🟣 SYSTEM: $contentText")
            setProgressBar(R.id.soloProgress, 100, progressPercent, false)

            setOnClickPendingIntent(R.id.soloRoot, openTasksPendingIntent(context, pageId, taskId, "running"))
            setOnClickPendingIntent(R.id.soloBtnStop, piStop)

            // Pause/Resume label + action
            setTextViewText(R.id.soloBtnPause, if (isRunning) "⏸" else "▶")
            setOnClickPendingIntent(R.id.soloBtnPause, if (isRunning) piPause else piResume)
        }

        val rvBig = RemoteViews(context.packageName, R.layout.notification_solo_timer_big).apply {
            setTextViewText(R.id.soloTitle, title)
            setTextViewText(R.id.soloRemaining, remainingText)
            setTextViewText(R.id.soloSystemLine, "🟣 SYSTEM: $contentText")
            setProgressBar(R.id.soloProgress, 100, progressPercent, false)

            setOnClickPendingIntent(R.id.soloRoot, openTasksPendingIntent(context, pageId, taskId, "running"))

            setTextViewText(R.id.soloBtnPause, if (isRunning) "⏸ Pause" else "▶ Resume")
            setOnClickPendingIntent(R.id.soloBtnPause, if (isRunning) piPause else piResume)
            setOnClickPendingIntent(R.id.soloBtnStop, piStop)
            setOnClickPendingIntent(R.id.soloBtnDone, piDone)
        }

        val b = NotificationCompat.Builder(context, CH_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openTasksPendingIntent(context, pageId = pageId, taskId = taskId, tab = "running"))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(rvSmall)
            .setCustomBigContentView(rvBig)

        // Keep standard actions too (some devices hide custom buttons)
        if (isRunning) b.addAction(0, "⏸ Pause", piPause) else b.addAction(0, "▶ Resume", piResume)
        b.addAction(0, "🛑 Stop", piStop)
        b.addAction(0, "✅ Clear", piDone)

        if (Build.VERSION.SDK_INT >= 31) {
            b.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return b.build()
    }

    private fun formatRemaining(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0L) / 1000L).toInt()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    fun safeNotify(context: Context, id: Int, notification: Notification) {
        try {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) { }
        catch (_: Exception) { }
    }
}
