package com.venkatesh.proguin.data

import android.content.Context

class SettingsStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("proguin_settings", Context.MODE_PRIVATE)

    fun defaultMinutes(): Int = p.getInt("default_minutes", 25)
    fun setDefaultMinutes(v: Int) = p.edit().putInt("default_minutes", v.coerceIn(0, 999)).apply()

    fun soundEnabled(): Boolean = p.getBoolean("sound_enabled", true)
    fun setSoundEnabled(v: Boolean) = p.edit().putBoolean("sound_enabled", v).apply()

    fun vibrationEnabled(): Boolean = p.getBoolean("vibration_enabled", false)
    fun setVibrationEnabled(v: Boolean) = p.edit().putBoolean("vibration_enabled", v).apply()

    fun resetAll() = p.edit().clear().apply()
}
