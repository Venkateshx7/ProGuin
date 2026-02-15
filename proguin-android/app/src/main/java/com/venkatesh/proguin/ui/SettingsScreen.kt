@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui

import androidx.compose.material3.ExperimentalMaterial3Api


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.data.SettingsStore
import com.venkatesh.proguin.data.StatsStore

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { SettingsStore(ctx) }
    val stats = remember { StatsStore(ctx) }

    var defaultMinutes by remember { mutableIntStateOf(settings.defaultMinutes()) }
    var sound by remember { mutableStateOf(settings.soundEnabled()) }
    var vibration by remember { mutableStateOf(settings.vibrationEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Default timer minutes", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = {
                            defaultMinutes = (defaultMinutes - 1).coerceAtLeast(0)
                            settings.setDefaultMinutes(defaultMinutes)
                        }) { Text("-") }
                        Text("$defaultMinutes", style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = {
                            defaultMinutes = (defaultMinutes + 1).coerceAtMost(999)
                            settings.setDefaultMinutes(defaultMinutes)
                        }) { Text("+") }
                    }
                }
            }

            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Sound")
                        Switch(checked = sound, onCheckedChange = {
                            sound = it
                            settings.setSoundEnabled(it)
                        })
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Vibration")
                        Switch(checked = vibration, onCheckedChange = {
                            vibration = it
                            settings.setVibrationEnabled(it)
                        })
                    }
                }
            }

            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reset")
                    Button(onClick = {
                        stats.resetAll()
                    }) { Text("Reset stats (streak/dashboard)") }
                }
            }
        }
    }
}
