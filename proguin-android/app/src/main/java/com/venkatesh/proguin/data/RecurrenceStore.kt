package com.venkatesh.proguin.data

import android.content.Context

enum class RepeatRule { NONE, DAILY, WEEKLY }

class RecurrenceStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("proguin_recur", Context.MODE_PRIVATE)

    fun getRule(taskId: String): RepeatRule {
        val v = p.getString("rule_$taskId", RepeatRule.NONE.name) ?: RepeatRule.NONE.name
        return runCatching { RepeatRule.valueOf(v) }.getOrDefault(RepeatRule.NONE)
    }

    fun setRule(taskId: String, rule: RepeatRule) {
        p.edit().putString("rule_$taskId", rule.name).apply()
    }

    fun clearRule(taskId: String) {
        p.edit().remove("rule_$taskId").apply()
    }
}
