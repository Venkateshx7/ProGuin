package com.venkatesh.proguin.ui.player

data class PlayerStatus(
    val level: Int,
    val rank: String
)

fun rankFromLevel(level: Int): String {
    val lv = level.coerceIn(1, 100)
    return when (lv) {
        in 1..10 -> "E-Rank"
        in 11..20 -> "D-Rank"
        in 21..35 -> "C-Rank"
        in 36..55 -> "B-Rank"
        in 56..80 -> "A-Rank"
        else -> "S-Rank"
    }
}