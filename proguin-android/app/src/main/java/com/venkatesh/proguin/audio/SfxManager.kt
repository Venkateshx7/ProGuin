package com.venkatesh.proguin.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.venkatesh.proguin.R

/**
 * Lightweight UI SFX player.
 * Uses simple bundled short WAV assets under res/raw.
 */
class SfxManager(context: Context) {

    private val soundPool: SoundPool
    private val sOpen: Int
    private val sClick: Int
    private val sStart: Int
    private val sDone: Int

    var enabled: Boolean = true
    var volume: Float = 0.90f

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        // These resources are bundled with the project.
        sOpen = soundPool.load(context, R.raw.ui_open, 1)
        sClick = soundPool.load(context, R.raw.ui_click, 1)
        sStart = soundPool.load(context, R.raw.task_start, 1)
        sDone = soundPool.load(context, R.raw.task_done, 1)
    }

    fun open() = play(sOpen)
    fun click() = play(sClick)
    fun start() = play(sStart)
    fun done() = play(sDone)

    private fun play(id: Int) {
        if (!enabled) return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() {
        try { soundPool.release() } catch (_: Exception) { }
    }
}
