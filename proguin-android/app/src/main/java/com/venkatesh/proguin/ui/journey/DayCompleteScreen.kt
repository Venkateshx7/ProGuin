package com.venkatesh.proguin.ui.journey

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chaquo.python.Python
import java.io.File

@Composable
fun DayCompleteScreen(
    completedDay: Int,
    onContinue: () -> Unit
) {

    val context = LocalContext.current
    val py = remember { Python.getInstance() }
    val journey = remember { py.getModule("proguin.journey") }
    val path = remember { File(context.filesDir, "journey.json").absolutePath }

    // ✅ KEY FIX: reset per day (Day2 screen != Day3 screen)
    var completedOnceForThisDay by rememberSaveable(completedDay) { mutableStateOf(false) }

    var saving by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(completedDay) {
        if (completedOnceForThisDay) {
            saving = false
            return@LaunchedEffect
        }

        try {
            val data = journey.callAttr("load", path)
            val updated = journey.callAttr("complete_day", data)
            journey.callAttr("save", path, updated)

            completedOnceForThisDay = true
            saving = false
        } catch (e: Exception) {
            err = e.message ?: "Failed to complete day"
            saving = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text("Day $completedDay Complete!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))

                if (saving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text("Saving progress…")
                } else if (err != null) {
                    Text("Error: $err")
                } else {
                    Text("Progress saved ✅")
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onContinue,
                    enabled = !saving
                ) {
                    Text("Continue Journey")
                }
            }
        }
    }
}
