@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui

import androidx.compose.material3.ExperimentalMaterial3Api


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.data.StatsStore

@Composable
fun DashboardScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val stats = remember { StatsStore(ctx) }

    val todayDone = stats.todayDone()
    val todayFocus = stats.todayFocusMinutes()
    val streak = stats.streak()
    val best = stats.bestStreak()
    val last7 = stats.last7Days()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card { Column(Modifier.padding(14.dp)) { Text("Today Done: $todayDone") } }
            Card { Column(Modifier.padding(14.dp)) { Text("Today Focus: $todayFocus min") } }
            Card { Column(Modifier.padding(14.dp)) { Text("Streak: $streak  (Best: $best)") } }

            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                    last7.forEach { (label, pair) ->
                        Text("$label  •  Done: ${pair.first}  •  Focus: ${pair.second} min")
                    }
                }
            }
        }
    }
}
