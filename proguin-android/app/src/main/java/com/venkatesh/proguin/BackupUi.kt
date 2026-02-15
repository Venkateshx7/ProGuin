package com.venkatesh.proguin

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun BackupActionsMenu(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pagesFile = remember { File(context.filesDir, "pages.json") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pagesFile.inputStream().use { it.copyTo(out) }
            }
            Toast.makeText(context, "Backup saved ✅", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        onDismiss()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                pagesFile.outputStream().use { input.copyTo(it) }
            }
            // Notify UI refresh
            val i = android.content.Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(i)

            Toast.makeText(context, "Backup restored ✅", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        onDismiss()
    }

    DropdownMenuItem(
        text = { Text("Backup (Export pages.json)") },
        onClick = {
            exportLauncher.launch("proguin-backup.json")
        }
    )

    DropdownMenuItem(
        text = { Text("Restore (Import backup)") },
        onClick = {
            importLauncher.launch(arrayOf("application/json"))
        }
    )
}
