package com.venkatesh.proguin.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TemplateStore(ctx: Context) {

    private val p = ctx.getSharedPreferences("proguin_templates", Context.MODE_PRIVATE)

    data class Template(
        val id: String,
        val title: String,
        val taskName: String,
        val timerMinutes: Int?,
        val reward: String?,
        val tagsCsv: String,
        val recurrenceType: String,
        val recurrenceInterval: Int,
        val weeklyDaysCsv: String,
        val subtasksText: String
    )

    fun list(): List<Template> {
        val raw = p.getString("templates_json", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<Template>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    Template(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        taskName = o.optString("taskName"),
                        timerMinutes = if (o.has("timerMinutes") && !o.isNull("timerMinutes")) o.optInt("timerMinutes") else null,
                        reward = o.optString("reward").ifBlank { null },
                        tagsCsv = o.optString("tagsCsv"),
                        recurrenceType = o.optString("recurrenceType", "none"),
                        recurrenceInterval = o.optInt("recurrenceInterval", 1).coerceAtLeast(1),
                        weeklyDaysCsv = o.optString("weeklyDaysCsv"),
                        subtasksText = o.optString("subtasksText")
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(template: Template) {
        val list = list().toMutableList()
        val idx = list.indexOfFirst { it.id == template.id }
        if (idx >= 0) list[idx] = template else list.add(0, template)

        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("title", it.title)
            o.put("taskName", it.taskName)
            if (it.timerMinutes == null) o.put("timerMinutes", JSONObject.NULL) else o.put("timerMinutes", it.timerMinutes)
            o.put("reward", it.reward ?: "")
            o.put("tagsCsv", it.tagsCsv)
            o.put("recurrenceType", it.recurrenceType)
            o.put("recurrenceInterval", it.recurrenceInterval)
            o.put("weeklyDaysCsv", it.weeklyDaysCsv)
            o.put("subtasksText", it.subtasksText)
            arr.put(o)
        }
        p.edit().putString("templates_json", arr.toString()).apply()
    }

    fun delete(id: String) {
        val list = list().filterNot { it.id == id }
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("title", it.title)
            o.put("taskName", it.taskName)
            if (it.timerMinutes == null) o.put("timerMinutes", JSONObject.NULL) else o.put("timerMinutes", it.timerMinutes)
            o.put("reward", it.reward ?: "")
            o.put("tagsCsv", it.tagsCsv)
            o.put("recurrenceType", it.recurrenceType)
            o.put("recurrenceInterval", it.recurrenceInterval)
            o.put("weeklyDaysCsv", it.weeklyDaysCsv)
            o.put("subtasksText", it.subtasksText)
            arr.put(o)
        }
        p.edit().putString("templates_json", arr.toString()).apply()
    }
}
