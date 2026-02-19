@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui.journey

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import java.io.File
import kotlin.math.roundToInt

@Composable
fun Day74FinishScreen(
    onBackToMode: () -> Unit
) {
    BackHandler { onBackToMode() }

    val context = LocalContext.current
    val py = remember { Python.getInstance() }
    val journey = remember { py.getModule("proguin.journey") }
    val path = remember { File(context.filesDir, "journey.json").absolutePath }

    var xp by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var completedCount by remember { mutableIntStateOf(0) }

    fun pyInt(map: Map<PyObject, PyObject>, key: String, fallback: Int): Int {
        return try {
            map[PyObject.fromJava(key)]?.toString()?.toIntOrNull() ?: fallback
        } catch (_: Exception) { fallback }
    }

    LaunchedEffect(Unit) {
        try {
            val data = journey.callAttr("load", path)
            val dm = data.asMap()

            xp = pyInt(dm, "xp", 0)
            streak = pyInt(dm, "streak", 0)

            completedCount = try {
                val obj = dm[PyObject.fromJava("completed_days")]
                obj?.asList()?.size ?: 0
            } catch (_: Exception) { 0 }

        } catch (_: Exception) {
        }
    }

    val pct = ((completedCount.coerceAtMost(74) / 74.0) * 100).roundToInt()

    fun openReview() {
        val pkg = context.packageName
        val market = Uri.parse("market://details?id=$pkg")
        val web = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, market)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, web)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun share() {
        val msg =
            "I completed ProGuin 74-Day Journey ✅\n" +
                    "Streak: $streak days • XP: $xp • Completed: $pct%\n" +
                    "Consistency really changes everything."

        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, msg)
        }
        context.startActivity(
            Intent.createChooser(i, "Share")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🎉 Journey Complete") })
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🏆 You did it.",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text("74 Days Completed ✅", fontWeight = FontWeight.SemiBold)

                    HorizontalDivider()

                    Text("Your Summary", fontWeight = FontWeight.SemiBold)
                    Text("• Completed: $completedCount / 74 ($pct%)")
                    Text("• Streak: $streak days")
                    Text("• XP: $xp")

                    HorizontalDivider()

                    Text(
                        "This means you built discipline, focus, and self-trust.\n" +
                                "You proved you can show up even when you don’t feel like it.\n" +
                                "That skill will carry into studies, job, business — everything."
                    )
                }
            }

            // ✅ FIX: wrap in lambda OR use ::openReview
            Button(
                onClick = { openReview() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("⭐ Give a Play Store Review", fontWeight = FontWeight.SemiBold)
            }

            // ✅ FIX: wrap in lambda OR use ::share
            OutlinedButton(
                onClick = { share() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("📣 Share your streak")
            }

            OutlinedButton(
                onClick = onBackToMode,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to Mode Selection")
            }
        }
    }
}
