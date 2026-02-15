package com.venkatesh.proguin.ui.journey

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chaquo.python.Python
import java.io.File

@Composable
fun DayCompleteScreen(onContinue: () -> Unit) {

    val context = LocalContext.current
    val py = remember { Python.getInstance() }
    val journey = remember { py.getModule("proguin.journey") }

    val path = File(context.filesDir, "journey.json").absolutePath

    LaunchedEffect(Unit) {
        val data = journey.callAttr("load", path)
        val updated = journey.callAttr("complete_day", data)
        journey.callAttr("save", path, updated)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Day Complete!", style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.height(20.dp))

            Button(onClick = onContinue) {
                Text("Continue Journey")
            }
        }
    }
}
