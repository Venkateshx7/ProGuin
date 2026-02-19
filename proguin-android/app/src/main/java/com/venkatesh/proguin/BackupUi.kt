package com.venkatesh.proguin.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun BackupUi() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Backup / Restore", style = MaterialTheme.typography.headlineSmall)

        Text(
            "This will backup your pages.json and journey.json inside app storage.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = { doBackup(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Backup")
        }

        OutlinedButton(
            onClick = { doRestore(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore Backup")
        }
    }
}

private fun doBackup(context: Context) {
    try {
        val dir = File(context.filesDir, "backup")
        dir.mkdirs()

        val pages = File(context.filesDir, "pages.json")
        val journey = File(context.filesDir, "journey.json")

        if (pages.exists()) pages.copyTo(File(dir, "pages.json"), overwrite = true)
        if (journey.exists()) journey.copyTo(File(dir, "journey.json"), overwrite = true)

        Toast.makeText(context, "Backup saved ✅ (filesDir/backup)", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun doRestore(context: Context) {
    try {
        val dir = File(context.filesDir, "backup")

        val pagesB = File(dir, "pages.json")
        val journeyB = File(dir, "journey.json")

        if (pagesB.exists()) pagesB.copyTo(File(context.filesDir, "pages.json"), overwrite = true)
        if (journeyB.exists()) journeyB.copyTo(File(context.filesDir, "journey.json"), overwrite = true)

        Toast.makeText(context, "Restore done ✅", Toast.LENGTH_LONG).show()

        // refresh UI
        context.sendBroadcast(
            android.content.Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                setPackage(context.packageName)
            }
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
