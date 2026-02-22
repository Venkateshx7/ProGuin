@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.data.StatsStore
import kotlin.math.max

@Composable
fun DashboardScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val stats = remember { StatsStore(ctx) }

    // refresh values each recomposition (fast; uses SharedPrefs)
    val todayDone = stats.todayDone()
    val todayFocus = stats.todayFocusMinutes()
    val streak = stats.streak()
    val best = stats.bestStreak()
    val last7 = stats.last7Days()
    val level = stats.levelInfo()
    val heat = stats.heatmap(84)

    val cs = MaterialTheme.colorScheme

    val quote = remember(todayDone, todayFocus, streak) {
        when {
            streak >= 14 -> "You’ve built real momentum. Keep the standard high."
            streak >= 7 -> "A full week of consistency. That’s how winners are made."
            todayDone >= 5 -> "Strong day. Small wins compound fast."
            todayFocus >= 60 -> "Deep work achieved. Protect this rhythm."
            else -> "Start small. Finish strong. Repeat tomorrow."
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface.copy(alpha = 0.92f),
                    titleContentColor = cs.onSurface,
                    navigationIconContentColor = cs.onSurface
                )
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.94f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill(title = "Done", value = "$todayDone", modifier = Modifier.weight(1f))
                            StatPill(title = "Focus", value = "${todayFocus}m", modifier = Modifier.weight(1f))
                            StatPill(title = "Streak", value = "$streak", modifier = Modifier.weight(1f))
                        }
                        Text(
                            quote,
                            color = cs.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.94f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Level ${level.level} • ${level.title}", color = cs.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { level.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                        )
                        Text(
                            "${level.xpIntoLevel} / ${max(1, level.xpNeeded)} XP to next",
                            color = cs.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.94f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Consistency heatmap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        HeatmapGrid(days = heat, weeks = 12)
                        Text(
                            "Best streak: $best",
                            color = cs.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.94f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Weekly report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        last7.forEach { (label, pair) ->
                            Text(
                                "$label  •  Done: ${pair.first}  •  Focus: ${pair.second} min",
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(title: String, value: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.primaryContainer.copy(alpha = 0.55f))
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HeatmapGrid(days: List<StatsStore.HeatDay>, weeks: Int) {
    val cs = MaterialTheme.colorScheme
    val perWeek = 7
    val total = weeks * perWeek
    val slice = if (days.size >= total) days.takeLast(total) else days

    // arrange as columns (weeks) each 7 rows
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (w in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in 0 until perWeek) {
                    val idx = w * perWeek + d
                    val intensity = slice.getOrNull(idx)?.intensity ?: 0
                    val alpha = when (intensity) {
                        0 -> 0.10f
                        1 -> 0.22f
                        2 -> 0.36f
                        3 -> 0.55f
                        else -> 0.75f
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(cs.primary.copy(alpha = alpha), RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
