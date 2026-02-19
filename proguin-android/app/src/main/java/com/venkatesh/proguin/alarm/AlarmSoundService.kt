package com.venkatesh.proguin.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AlarmSoundService : Service() {

    companion object {

        private const val NOTIF_ID = 9901

        fun start(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ringtone: Ringtone? = null
    private var stopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        NotificationHelper.ensureChannels(this)

        val taskName = intent?.getStringExtra(NotificationHelper.EXTRA_TASK_NAME).orEmpty()

        val notif = NotificationCompat.Builder(this, NotificationHelper.CH_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm 🔔")
            .setContentText(taskName.ifBlank { "Scheduled task" })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(NOTIF_ID, notif)

        playAlarmTone()

        // ✅ stop after 20 sec automatically (so it won’t keep ringing forever)
        stopJob?.cancel()
        stopJob = scope.launch {
            delay(2_000)
            stopSelfSafely()
        }

        return START_NOT_STICKY
    }

    private fun playAlarmTone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val r = RingtoneManager.getRingtone(this, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                r.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone = r
            r.play()
        } catch (_: Exception) { }
    }

    private fun stopSelfSafely() {
        try {
            ringtone?.stop()
        } catch (_: Exception) { }
        ringtone = null

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) { }

        stopSelf()
    }

    override fun onDestroy() {
        stopJob?.cancel()
        scope.cancel()
        stopSelfSafely()
        super.onDestroy()
    }
}
