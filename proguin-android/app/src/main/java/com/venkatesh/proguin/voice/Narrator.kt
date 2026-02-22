package com.venkatesh.proguin.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Simple TTS narrator for short voice-over lines.
 * Uses the device's installed TTS engines.
 */
class Narrator(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)

    var enabled: Boolean = true
    var language: Locale = Locale.ENGLISH

    override fun onInit(status: Int) {
        try {
            tts?.language = language
            tts?.setSpeechRate(1.02f)
            tts?.setPitch(0.92f) // slightly deeper “system” vibe
        } catch (_: Exception) { }
    }

    fun speak(text: String) {
        if (!enabled) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PROGUIN_TTS")
        } catch (_: Exception) { }
    }

    fun release() {
        try { tts?.stop() } catch (_: Exception) { }
        try { tts?.shutdown() } catch (_: Exception) { }
        tts = null
    }
}
