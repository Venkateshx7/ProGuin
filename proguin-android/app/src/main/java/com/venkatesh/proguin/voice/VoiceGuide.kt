package com.venkatesh.proguin.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * VoiceGuide = "speak once" helper for tutorial / guide voice lines.
 *
 * - speakOnce(key, text): speaks only once per key (stored in SharedPreferences)
 * - resetKey(key): allow that line again
 * - resetAll(): allow all guide lines again
 * - release(): shutdown TTS cleanly
 */
class VoiceGuide(context: Context) {

    private val appContext = context.applicationContext
    private val sp = appContext.getSharedPreferences("proguin_voice_guide", Context.MODE_PRIVATE)

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    init {
        try {
            tts = TextToSpeech(appContext) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    try {
                        // Prefer device language; fallback EN.
                        val res = tts?.setLanguage(Locale.getDefault()) ?: TextToSpeech.LANG_MISSING_DATA
                        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts?.setLanguage(Locale.ENGLISH)
                        }
                    } catch (_: Exception) {
                    }

                    // Keep guide voice not annoying
                    try { tts?.setSpeechRate(0.95f) } catch (_: Exception) {}
                    try { tts?.setPitch(1.02f) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {
            ttsReady = false
        }
    }

    /**
     * Speaks only once for the given key.
     * If you want it again, call resetKey(key) or resetAll().
     */
    fun speakOnce(key: String, text: String) {
        if (key.isBlank()) {
            speak(text)
            return
        }

        val already = try { sp.getBoolean(key, false) } catch (_: Exception) { false }
        if (already) return

        try { sp.edit().putBoolean(key, true).apply() } catch (_: Exception) { }
        speak(text)
    }

    /**
     * Speak always (no once rule)
     */
    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ttsReady) return

        try {
            val id = "guide_" + System.currentTimeMillis()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun resetKey(key: String) {
        if (key.isBlank()) return
        try { sp.edit().remove(key).apply() } catch (_: Exception) {}
    }

    fun resetAll() {
        try { sp.edit().clear().apply() } catch (_: Exception) {}
    }

    fun release() {
        try { tts?.stop() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        ttsReady = false
    }
}