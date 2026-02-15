package com.venkatesh.proguin.ui.journey

import com.venkatesh.proguin.R

fun arcImageForDay(day: Int): Int {
    return when (day) {
        in 1..7 -> R.drawable.bg_arc1
        in 8..14 -> R.drawable.bg_arc2
        in 15..21 -> R.drawable.bg_arc3
        in 22..28 -> R.drawable.bg_arc4
        in 29..35 -> R.drawable.bg_arc5
        in 36..42 -> R.drawable.bg_arc6
        in 43..49 -> R.drawable.bg_arc7
        in 50..56 -> R.drawable.bg_arc8
        in 57..63 -> R.drawable.bg_arc9
        in 64..70 -> R.drawable.bg_arc10
        else -> R.drawable.bg_arc11
    }
}
