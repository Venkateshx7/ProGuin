package com.venkatesh.proguin.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("proguin_stats", Context.MODE_PRIVATE)

    private fun todayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Calendar.getInstance().time)
    }

    fun addCompletion(taskName: String) {
        val k = todayKey()
        val cur = p.getInt("done_$k", 0)
        p.edit().putInt("done_$k", cur + 1).apply()

        // mark "day had activity"
        p.edit().putBoolean("active_$k", true).apply()
    }

    fun addFocusMinutes(minutes: Int) {
        val k = todayKey()
        val cur = p.getInt("focus_$k", 0)
        p.edit().putInt("focus_$k", cur + minutes.coerceAtLeast(0)).apply()
        p.edit().putBoolean("active_$k", true).apply()
    }

    fun todayDone(): Int = p.getInt("done_${todayKey()}", 0)
    fun todayFocusMinutes(): Int = p.getInt("focus_${todayKey()}", 0)

    fun streak(): Int {
        var s = 0
        val cal = Calendar.getInstance()
        for (i in 0..365) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val k = sdf.format(cal.time)
            val active = p.getBoolean("active_$k", false)
            if (!active) break
            s++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val best = p.getInt("best_streak", 0)
        if (s > best) p.edit().putInt("best_streak", s).apply()
        return s
    }

    fun bestStreak(): Int = p.getInt("best_streak", 0)

    fun last7Days(): List<Pair<String, Pair<Int, Int>>> {
        val out = mutableListOf<Pair<String, Pair<Int, Int>>>()
        val cal = Calendar.getInstance()
        val sdfLabel = SimpleDateFormat("EEE", Locale.US)
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (i in 0 until 7) {
            val key = sdfKey.format(cal.time)
            val label = sdfLabel.format(cal.time)
            val done = p.getInt("done_$key", 0)
            val focus = p.getInt("focus_$key", 0)
            out.add(label to (done to focus))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return out.reversed()
    }

    fun resetAll() = p.edit().clear().apply()
}
