@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.data.LocalBackupManager
import com.venkatesh.proguin.data.SettingsStore
import com.venkatesh.proguin.data.StatsStore

@Composable
fun SettingsScreen(onBack: () -> Unit) {

    BackHandler { onBack() }

    val ctx = LocalContext.current
    val settings = remember { SettingsStore(ctx) }
    val stats = remember { StatsStore(ctx) }

    var defaultMinutes by remember { mutableIntStateOf(settings.defaultMinutes()) }
    var sound by remember { mutableStateOf(settings.soundEnabled()) }
    var vibration by remember { mutableStateOf(settings.vibrationEnabled()) }

    // ✅ Offline export/import
    val localBackup = remember { LocalBackupManager(ctx) }
    var localMsg by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        localMsg = "Saving backup…"
        val r = localBackup.exportToUri(ctx.contentResolver, uri)
        localMsg = if (r.isSuccess) "Offline backup saved ✅" else "Backup failed: ${r.exceptionOrNull()?.message ?: "unknown"}"
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        localMsg = "Restoring backup…"
        val r = localBackup.importFromUri(ctx.contentResolver, uri)
        localMsg = if (r.isSuccess) "Restore complete ✅ (reopen app)" else "Restore failed: ${r.exceptionOrNull()?.message ?: "unknown"}"

        try {
            ctx.sendBroadcast(
                android.content.Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                    setPackage(ctx.packageName)
                }
            )
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp),
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
                    Button(onClick = { stats.resetAll() }) {
                        Text("Reset stats (streak/dashboard)")
                    }
                }
            }

            // ✅ Offline Backup / Restore (Export/Import file)
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Offline Backup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Export a backup file to your phone/PC (works offline). Import restores everything.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                localMsg = ""
                                exportLauncher.launch("ProGuin_backup.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Export") }

                        OutlinedButton(
                            onClick = {
                                localMsg = ""
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Import") }
                    }

                    if (localMsg.isNotBlank()) {
                        Text(localMsg, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}