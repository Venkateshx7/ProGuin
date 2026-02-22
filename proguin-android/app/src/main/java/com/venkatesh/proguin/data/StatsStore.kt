package com.venkatesh.proguin.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class StatsStore(ctx: Context) {

    /**
     * ✅ IMPORTANT:
     * MainActivity uses: StatsStore.LevelInfo(1, "Getting Started", 0f, 0, 120)
     * So first 5 params MUST be:
     * (level, title, progress, xpIntoLevel, xpNeeded)
     *
     * Extra fields have defaults so MainActivity call still compiles.
     */
    data class LevelInfo(
        val level: Int,
        val title: String,
        val progress: Float,     // ✅ for LinearProgressIndicator (0f..1f)
        val xpIntoLevel: Int,
        val xpNeeded: Int,
        val rank: String = "E",
        val totalXp: Int = 0,
        val progressText: String = ""
    )

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

        // ✅ XP: completion gives 10 XP
        addXpInternal(10)
    }

    fun addFocusMinutes(minutes: Int) {
        val m = minutes.coerceAtLeast(0)
        val k = todayKey()
        val cur = p.getInt("focus_$k", 0)
        p.edit().putInt("focus_$k", cur + m).apply()
        p.edit().putBoolean("active_$k", true).apply()

        // ✅ XP: 1 XP per focus minute
        addXpInternal(m)
    }

    fun todayDone(): Int = p.getInt("done_${todayKey()}", 0)
    fun todayFocusMinutes(): Int = p.getInt("focus_${todayKey()}", 0)

    fun streak(): Int {
        var s = 0
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0..365) {
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

    // -------------------
    // ✅ Tier 1/2: XP + Level
    // -------------------

    private fun addXpInternal(xp: Int) {
        val cur = p.getInt("xp_total", 0)
        p.edit().putInt("xp_total", cur + xp.coerceAtLeast(0)).apply()
    }

    fun xpTotal(): Int = p.getInt("xp_total", 0)

    private fun xpForNextLevel(level: Int): Int {
        // simple curve: level 1→2 needs 120, then grows slowly
        return 120 + max(0, level - 1) * 60
    }

    fun levelInfo(): LevelInfo {
        val totalXp = xpTotal()

        var xpInto = totalXp
        var level = 1
        var needed = xpForNextLevel(level)

        while (xpInto >= needed && level < 99) {
            xpInto -= needed
            level++
            needed = xpForNextLevel(level)
        }

        val progress = if (needed <= 0) 0f else (xpInto.toFloat() / needed.toFloat()).coerceIn(0f, 1f)

        val title = when {
            level >= 30 -> "Elite Performer"
            level >= 20 -> "Discipline Builder"
            level >= 10 -> "Focus Specialist"
            level >= 5 -> "Consistency Runner"
            else -> "Getting Started"
        }

        val progressText = "${xpInto.coerceAtLeast(0)} / ${needed.coerceAtLeast(1)} XP"

        val rank = when {
            level >= 50 -> "S"
            level >= 30 -> "A"
            level >= 20 -> "B"
            level >= 10 -> "C"
            level >= 5  -> "D"
            else -> "E"
        }

        return LevelInfo(
            level = level,
            title = title,
            progress = progress,
            xpIntoLevel = xpInto,
            xpNeeded = needed,
            rank = rank,
            totalXp = totalXp,
            progressText = progressText
        )
    }

    // -------------------
    // ✅ Tier 1: Heatmap (last N days)
    // intensity 0..4 based on done + focus minutes
    // -------------------

    fun heatmap(days: Int = 84): List<HeatDay> {
        val out = mutableListOf<HeatDay>()
        val cal = Calendar.getInstance()
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (i in 0 until days) {
            val key = sdfKey.format(cal.time)
            val done = p.getInt("done_$key", 0)
            val focus = p.getInt("focus_$key", 0)
            val score = done * 10 + focus

            val intensity = when {
                score <= 0 -> 0
                score <= 15 -> 1
                score <= 45 -> 2
                score <= 90 -> 3
                else -> 4
            }
            out.add(HeatDay(key = key, intensity = intensity, done = done, focus = focus))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return out.reversed()
    }

    data class HeatDay(
        val key: String,
        val intensity: Int,
        val done: Int,
        val focus: Int
    )

    fun resetAll() = p.edit().clear().apply()
}