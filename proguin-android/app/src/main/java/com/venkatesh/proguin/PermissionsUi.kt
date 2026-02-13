package com.venkatesh.proguin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.venkatesh.proguin.alarm.AlarmScheduler

@Composable
fun rememberPermissionsUiState(): PermissionsUiState {
    return remember { PermissionsUiState() }
}

class PermissionsUiState {

    private var pendingNotifAction: (() -> Unit)? = null
    private var pendingExactAction: ((Boolean) -> Unit)? = null

    var showNotifRationale by mutableStateOf(false)
        private set

    var showExactAlarmDialog by mutableStateOf(false)
        private set

    // Call this once inside your screen Composable
    @Composable
    fun BindDialogs(context: Context) {

        if (showNotifRationale) {
            AlertDialog(
                onDismissRequest = { showNotifRationale = false },
                title = { Text("Enable notifications?") },
                text = {
                    Text(
                        "ProGuin uses notifications for:\n" +
                                "• Scheduled task reminders\n" +
                                "• Running timer status\n\n" +
                                "You can continue without it, but reminders may be silent."
                    )
                },
                confirmButton = {
                    Button(onClick = { showNotifRationale = false }) { Text("OK") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showNotifRationale = false
                        Toast.makeText(context, "Notifications denied. App still works.", Toast.LENGTH_LONG).show()
                        pendingNotifAction?.invoke()
                        pendingNotifAction = null
                    }) { Text("Not now") }
                }
            )
        }

        if (showExactAlarmDialog) {
            AlertDialog(
                onDismissRequest = { showExactAlarmDialog = false },
                title = { Text("Allow exact alarms?") },
                text = {
                    Text(
                        "To ring exactly at the scheduled time, Android needs “Exact alarm” permission.\n\n" +
                                "If you skip, ProGuin will still schedule it, but it may be delayed."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showExactAlarmDialog = false
                        try {
                            val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(i)
                        } catch (_: Exception) { }

                        // fallback now
                        pendingExactAction?.invoke(false)
                        pendingExactAction = null
                    }) { Text("Open settings") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showExactAlarmDialog = false
                        pendingExactAction?.invoke(false)
                        pendingExactAction = null
                    }) { Text("Use fallback") }
                }
            )
        }
    }

    @Composable
    fun ensureNotificationPermissionThen(context: Context, action: () -> Unit) {

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            val a = pendingNotifAction
            pendingNotifAction = null
            if (!granted) {
                Toast.makeText(context, "Notifications denied. App still works.", Toast.LENGTH_LONG).show()
            }
            a?.invoke()
        }

        fun hasNotifPermission(): Boolean {
            if (Build.VERSION.SDK_INT < 33) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (hasNotifPermission()) {
            action()
            return
        }

        pendingNotifAction = action

        val activity = context as? Activity
        val shouldExplain = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )

        if (shouldExplain) {
            showNotifRationale = true
        }

        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun ensureExactAlarmOrFallback(
        context: Context,
        triggerAtMillis: Long,
        onReady: (Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onReady(true)
            return
        }

        val canExact = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            true
        } else {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.canScheduleExactAlarms()
        }

        if (canExact) {
            onReady(true)
            return
        }

        pendingExactAction = onReady
        showExactAlarmDialog = true
    }
}
