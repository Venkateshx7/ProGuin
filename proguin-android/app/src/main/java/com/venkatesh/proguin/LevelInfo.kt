package com.venkatesh.proguin

data class LevelInfo(
    val level: Int = 1,
    val title: String = "",
    val progress: Float = 0f,
    val xpIntoLevel: Int = 0,
    val xpNeeded: Int = 100,
    val rank: String = "E",
    val totalXp: Int = 0,
    val progressText: String = ""
)