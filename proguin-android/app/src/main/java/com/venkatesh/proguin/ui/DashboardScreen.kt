@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.R
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { pad ->

        // ✅ FULL BACKGROUND IMAGE + SCRIM (readable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {

            Image(
                painter = painterResource(R.drawable.bg_arc1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 1f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Today Done: $todayDone", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Today Focus: $todayFocus min", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Streak: $streak  (Best: $best)", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Last 7 days", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        last7.forEach { (label, pair) ->
                            Text(
                                "$label  •  Done: ${pair.first}  •  Focus: ${pair.second} min",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
