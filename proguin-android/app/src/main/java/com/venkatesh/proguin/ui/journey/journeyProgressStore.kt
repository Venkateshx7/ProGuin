package com.venkatesh.proguin.journey

import android.content.Context

class journeyProgressStore(context: Context) {

    private val sp = context.getSharedPreferences("proguin_journey", Context.MODE_PRIVATE)

    fun currentDay(): Int = sp.getInt("current_day", 1).coerceIn(1, 74)

    fun setCurrentDay(day: Int) {
        sp.edit().putInt("current_day", day.coerceIn(1, 74)).apply()
    }

    fun markDayCompleted(day: Int) {
        val d = day.coerceIn(1, 74)
        val done = completedDays().toMutableSet()
        done.add(d)
        sp.edit().putString("completed_days", done.joinToString(",")).apply()

        if (d == currentDay() && d < 74) {
            setCurrentDay(d + 1)
        }
    }

    fun completedDays(): Set<Int> {
        val s = sp.getString("completed_days", "").orEmpty().trim()
        if (s.isBlank()) return emptySet()
        return s.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..74 }
            .toSet()
    }

    fun isUnlocked(day: Int): Boolean {
        val d = day.coerceIn(1, 74)
        return d <= currentDay()
    }

    fun resetAll() {
        sp.edit().clear().apply()
    }
}
