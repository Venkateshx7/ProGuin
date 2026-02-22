package com.venkatesh.proguin.ui.journey

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chaquo.python.PyObject
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

    // ✅ extra info for UI messaging
    var infoMsg by remember { mutableStateOf<String?>(null) }

    fun intFromPyMap(m: Map<PyObject, PyObject>, key: String, fallback: Int): Int {
        return try {
            m[PyObject.fromJava(key)]?.toString()?.toIntOrNull() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun completedSetFromPyMap(m: Map<PyObject, PyObject>): Set<Int> {
        return try {
            val obj = m[PyObject.fromJava("completed_days")]
            obj?.asList()?.mapNotNull { it.toString().toIntOrNull() }?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    LaunchedEffect(completedDay) {
        infoMsg = null
        err = null

        if (completedOnceForThisDay) {
            saving = false
            infoMsg = "Already processed on this screen ✅"
            return@LaunchedEffect
        }

        try {
            val data = journey.callAttr("load", path)
            val dm = data.asMap()

            val unlockedDay = intFromPyMap(dm, "current_day", 1).coerceIn(1, 74)
            val doneDays = completedSetFromPyMap(dm)

            // ✅ HARD BLOCK #1: if day already completed, do nothing
            if (doneDays.contains(completedDay)) {
                saving = false
                completedOnceForThisDay = true
                infoMsg = "This day was already completed ✅"
                return@LaunchedEffect
            }

            // ✅ HARD BLOCK #2: only the CURRENT unlocked day can be completed
            if (completedDay != unlockedDay) {
                saving = false
                completedOnceForThisDay = true
                infoMsg = "You can only complete the current unlocked day (Day $unlockedDay)."
                return@LaunchedEffect
            }

            // ✅ OK: complete only unlocked day
            val updated = journey.callAttr("complete_day", data)
            journey.callAttr("save", path, updated)

            completedOnceForThisDay = true
            saving = false
            infoMsg = "Progress saved ✅"

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
                    Text(infoMsg ?: "Done ✅")
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