package com.venkatesh.proguin.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.venkatesh.proguin.MainActivity
import com.venkatesh.proguin.R
import com.venkatesh.proguin.data.StatsStore

class ProguinWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val stats = StatsStore(context)
        val streak = stats.streak()
        val todayDone = stats.todayDone()
        val todayFocus = stats.todayFocusMinutes()

        for (appWidgetId in appWidgetIds) {
            val rv = RemoteViews(context.packageName, R.layout.widget_proguin)

            rv.setTextViewText(R.id.w_title, "ProGuin • Focus Mode")
            rv.setTextViewText(R.id.w_sub, "Streak: $streak  •  Done: $todayDone  •  Focus: ${todayFocus}m")

            val openIntent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                context,
                1001,
                openIntent,
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.w_root, pi)
            rv.setOnClickPendingIntent(R.id.w_btn, pi)

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }
}
