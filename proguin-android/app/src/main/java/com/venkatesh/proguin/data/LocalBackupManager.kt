package com.venkatesh.proguin.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File

/**
 * Offline (user-controlled) backup/export & restore/import.
 *
 * Export writes a single JSON file the user can save/share anywhere (works offline).
 * Import restores the app state from that JSON file.
 */
class LocalBackupManager(private val ctx: Context) {

    private fun pagesFile(): File = File(ctx.filesDir, "pages.json")

    private fun readPagesJson(): String {
        val f = pagesFile()
        return if (f.exists()) f.readText() else ""
    }

    private fun writePagesJson(text: String) {
        pagesFile().writeText(text)
    }

    private fun exportPrefs(name: String): JSONObject {
        val sp = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
        val obj = JSONObject()
        for ((k, v) in sp.all) {
            when (v) {
                is String -> obj.put(k, v)
                is Boolean -> obj.put(k, v)
                is Int -> obj.put(k, v)
                is Long -> obj.put(k, v)
                is Float -> obj.put(k, v.toDouble())
                else -> {}
            }
        }
        return obj
    }

    private fun importPrefs(name: String, obj: JSONObject) {
        val sp = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
        val e = sp.edit().clear()
        val it = obj.keys()
        while (it.hasNext()) {
            val k = it.next()
            val v = obj.get(k)
            when (v) {
                is String -> e.putString(k, v)
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Double -> e.putFloat(k, v.toFloat())
                else -> {}
            }
        }
        e.apply()
    }

    fun buildBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("created_at", System.currentTimeMillis())

        root.put("pages_json", readPagesJson())

        // Full SharedPrefs backups
        root.put("prefs_proguin_settings", exportPrefs("proguin_settings"))
        root.put("prefs_proguin_stats", exportPrefs("proguin_stats"))
        root.put("prefs_proguin_journey", exportPrefs("proguin_journey"))
        root.put("prefs_proguin_recur", exportPrefs("proguin_recur"))
        root.put("prefs_proguin_timer_state", exportPrefs("proguin_timer_state"))
        root.put("prefs_proguin_drive", exportPrefs("proguin_drive"))

        return root.toString(2)
    }

    fun restoreFromBackupJson(json: String) {
        val root = JSONObject(json)

        val pages = root.optString("pages_json", "")
        if (pages.isNotBlank()) writePagesJson(pages)

        root.optJSONObject("prefs_proguin_settings")?.let { importPrefs("proguin_settings", it) }
        root.optJSONObject("prefs_proguin_stats")?.let { importPrefs("proguin_stats", it) }
        root.optJSONObject("prefs_proguin_journey")?.let { importPrefs("proguin_journey", it) }
        root.optJSONObject("prefs_proguin_recur")?.let { importPrefs("proguin_recur", it) }
        root.optJSONObject("prefs_proguin_timer_state")?.let { importPrefs("proguin_timer_state", it) }
        root.optJSONObject("prefs_proguin_drive")?.let { importPrefs("proguin_drive", it) }
    }

    fun exportToUri(resolver: ContentResolver, uri: Uri): Result<Unit> {
        return runCatching {
            resolver.openOutputStream(uri)?.use { os ->
                val bytes = buildBackupJson().toByteArray(Charsets.UTF_8)
                os.write(bytes)
                os.flush()
            } ?: error("Unable to open output stream")
        }
    }

    fun importFromUri(resolver: ContentResolver, uri: Uri): Result<Unit> {
        return runCatching {
            val text = resolver.openInputStream(uri)?.use { ins ->
                ins.readBytes().toString(Charsets.UTF_8)
            } ?: error("Unable to open input stream")
            restoreFromBackupJson(text)
        }
    }
}
