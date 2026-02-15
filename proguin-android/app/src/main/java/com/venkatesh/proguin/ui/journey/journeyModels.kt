package com.venkatesh.proguin.journey

data class JourneyTaskTemplate(
    val name: String,
    val minutes: Int,
    val reward: String? = null
)

data class JourneyDayPlan(
    val day: Int,
    val arc: Int,
    val arcTitle: String,
    val story: String,
    val quote: String,
    val tasks: List<JourneyTaskTemplate>
)
