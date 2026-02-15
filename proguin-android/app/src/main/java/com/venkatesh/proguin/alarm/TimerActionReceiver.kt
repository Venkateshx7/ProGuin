package com.venkatesh.proguin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_TIMER_STOP -> {
                TimerForegroundService.stopTimer(context)
            }
            NotificationHelper.ACTION_TIMER_PAUSE -> {
                TimerForegroundService.pauseTimer(context)
            }
            NotificationHelper.ACTION_TIMER_RESUME -> {
                TimerForegroundService.resumeTimer(context)
            }
            NotificationHelper.ACTION_TIMER_DONE -> {
                // “Done” ends timer + lets UI mark completion via existing button,
                // but we also send a broadcast so UI can auto-refresh.
                TimerForegroundService.stopTimer(context)
                context.sendBroadcast(
                    Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                        setPackage(context.packageName)
                    }
                )
            }
        }
    }
}
