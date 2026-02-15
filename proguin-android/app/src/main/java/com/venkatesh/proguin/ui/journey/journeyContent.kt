package com.venkatesh.proguin.journey

object journeyContent {

    private fun baseArc1(day: Int): List<JourneyTaskTemplate> {
        return listOf(
            JourneyTaskTemplate("Physical Win: Walk / Stretch", minutes = if (day <= 3) 5 else 8),
            JourneyTaskTemplate("Mental Win: 1 Focus Task", minutes = if (day <= 3) 10 else 15),
            JourneyTaskTemplate("Spiritual Win: Gratitude / Prayer / Silence", minutes = if (day <= 3) 3 else 5)
        )
    }

    private fun addArc2(day: Int): List<JourneyTaskTemplate> =
        listOf(
            JourneyTaskTemplate("Add 1 Good Habit (today)", minutes = 3),
            JourneyTaskTemplate("Remove 1 Bad Habit (today)", minutes = 2)
        )

    private fun addArc3(day: Int): List<JourneyTaskTemplate> =
        listOf(JourneyTaskTemplate("1% Improvement: make one thing better", minutes = 5))

    private fun addArc4(day: Int): List<JourneyTaskTemplate> {
        val focus = if (day % 2 == 0) 25 else 15
        return listOf(
            JourneyTaskTemplate("Work Sprint (Timer)", minutes = focus),
            JourneyTaskTemplate("Reward after sprint (healthy)", minutes = 3)
        )
    }

    private fun addArc5(day: Int): List<JourneyTaskTemplate> =
        listOf(
            JourneyTaskTemplate("80/20: Identify top 1 priority", minutes = 5),
            JourneyTaskTemplate("Do the top priority first", minutes = 15)
        )

    private fun addArc6(day: Int): List<JourneyTaskTemplate> =
        listOf(JourneyTaskTemplate("Morning Power (Move + Reflect + Learn)", minutes = 15))

    private fun addArc7(day: Int): List<JourneyTaskTemplate> =
        listOf(
            JourneyTaskTemplate("Learn a Skill (small daily)", minutes = 15),
            JourneyTaskTemplate("Practice that skill (tiny)", minutes = 10)
        )

    private fun addArc8(day: Int): List<JourneyTaskTemplate> =
        listOf(
            JourneyTaskTemplate("Save 1% money today (even ₹1)", minutes = 2),
            JourneyTaskTemplate("Track spending (quick)", minutes = 3)
        )

    private fun addArc9(day: Int): List<JourneyTaskTemplate> =
        listOf(
            JourneyTaskTemplate("Purpose Journal: Why do I climb?", minutes = 10),
            JourneyTaskTemplate("Define 1 goal for top 1%", minutes = 8)
        )

    private fun addArc10(day: Int): List<JourneyTaskTemplate> =
        listOf(
            JourneyTaskTemplate("Track changes: What improved since Day 1?", minutes = 10),
            JourneyTaskTemplate("Tomorrow plan: 1 upgrade", minutes = 5)
        )

    private fun arcMeta(arc: Int): Pair<String, String> {
        return when (arc) {
            1 -> "Awakening: Three Wins" to "The penguin wakes up. It learns to win daily."
            2 -> "Breaking Ice: Add/Remove Habit" to "Old ice cracks. New habits begin."
            3 -> "Compounding: 1% Rule" to "Tiny upgrades become unstoppable growth."
            4 -> "Discipline Engine: Timer + Reward" to "The penguin trains with structure."
            5 -> "Focus Blade: 80/20 Rule" to "The penguin learns what matters most."
            6 -> "Energy Ritual: Morning Power" to "Energy becomes the fuel for wins."
            7 -> "Skill Forge: Learn + Practice" to "Useful skills turn effort into power."
            8 -> "Resource Mastery: Save 1%" to "A strong mind respects money."
            9 -> "Purpose: Top 1% Direction" to "The penguin finds its true mountain."
            else -> "Ascension: God Mode Tracking" to "You track your rise and lock identity."
        }
    }

    private fun quoteFor(arc: Int): String {
        val q = listOf(
            "Small wins wake sleeping giants.",
            "Consistency beats motivation.",
            "Tiny gains beat heroic bursts.",
            "Discipline is freedom.",
            "Not everything deserves your energy.",
            "Energy creates excellence.",
            "Skills compound into freedom.",
            "Small savings build large power.",
            "Direction matters more than speed.",
            "You didn’t climb the mountain. You became someone who can."
        )
        return q[(arc - 1).coerceIn(0, 9)]
    }

    private fun storyFor(day: Int, arc: Int): String {
        return when (arc) {
            1 -> "Day $day: The penguin takes its first step in the snow. A win today is a victory for the future."
            2 -> "Day $day: The penguin drops a bad habit like a heavy stone and picks a lighter path."
            3 -> "Day $day: One tiny upgrade. One tiny step. The mountain doesn’t move—YOU do."
            4 -> "Day $day: A timer starts. The world disappears. Focus becomes your weapon."
            5 -> "Day $day: The penguin stops chasing everything and chooses the one thing that matters."
            6 -> "Day $day: Morning light hits the ice. Energy rises. Your routine becomes your aura."
            7 -> "Day $day: Skills sharpen like blades. Every practice is power stored."
            8 -> "Day $day: The penguin learns resources. Small savings, big strength."
            9 -> "Day $day: The penguin looks at the summit and finally understands WHY."
            else -> "Day $day: You review your journey. This is the final form: GOD MODE."
        }
    }

    private fun arcForDay(day: Int): Int {
        return when {
            day in 1..7 -> 1
            day in 8..14 -> 2
            day in 15..21 -> 3
            day in 22..28 -> 4
            day in 29..35 -> 5
            day in 36..42 -> 6
            day in 43..49 -> 7
            day in 50..56 -> 8
            day in 57..63 -> 9
            else -> 10
        }
    }

    fun getDayPlan(day: Int): JourneyDayPlan {
        val safeDay = day.coerceIn(1, 74)
        val arc = arcForDay(safeDay)
        val (arcTitle, arcDesc) = arcMeta(arc)

        val tasks = mutableListOf<JourneyTaskTemplate>()
        tasks.addAll(baseArc1(safeDay))
        if (arc >= 2) tasks.addAll(addArc2(safeDay))
        if (arc >= 3) tasks.addAll(addArc3(safeDay))
        if (arc >= 4) tasks.addAll(addArc4(safeDay))
        if (arc >= 5) tasks.addAll(addArc5(safeDay))
        if (arc >= 6) tasks.addAll(addArc6(safeDay))
        if (arc >= 7) tasks.addAll(addArc7(safeDay))
        if (arc >= 8) tasks.addAll(addArc8(safeDay))
        if (arc >= 9) tasks.addAll(addArc9(safeDay))
        if (arc >= 10) tasks.addAll(addArc10(safeDay))

        val trimmed = tasks.take(10)

        return JourneyDayPlan(
            day = safeDay,
            arc = arc,
            arcTitle = arcTitle,
            story = arcDesc + "\n\n" + storyFor(safeDay, arc),
            quote = quoteFor(arc),
            tasks = trimmed
        )
    }
}
