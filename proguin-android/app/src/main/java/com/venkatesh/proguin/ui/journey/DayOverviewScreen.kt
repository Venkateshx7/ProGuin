@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui.journey

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.PyObject
import com.chaquo.python.Python

@Composable
fun DayOverviewScreen(
    day: Int,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val py = remember { Python.getInstance() }
    val journey = remember { py.getModule("proguin.journey") }

    var dayTitle by remember { mutableStateOf("Day $day") }
    var arcTitle by remember { mutableStateOf("") }
    var story by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }

    fun pyStr(map: Map<PyObject, PyObject>, key: String, fallback: String = ""): String {
        return try {
            map[PyObject.fromJava(key)]?.toString() ?: fallback
        } catch (_: Exception) { fallback }
    }

    LaunchedEffect(day) {
        try {
            val plan = journey.callAttr("get_day_plan", day)
            val pm = plan.asMap()
            arcTitle = pyStr(pm, "arc_title", "")
            dayTitle = pyStr(pm, "day_title", "Day $day")
            story = pyStr(pm, "story", "")
            quote = pyStr(pm, "quote", "")
        } catch (_: Exception) {
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 18.sp)
                    }
                }
            )
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (arcTitle.isNotBlank()) Text(arcTitle, fontWeight = FontWeight.SemiBold)
                    Text(dayTitle, fontWeight = FontWeight.Bold)

                    if (story.isNotBlank()) {
                        HorizontalDivider()
                        Text("Story", fontWeight = FontWeight.SemiBold)
                        Text(story)
                    }

                    if (quote.isNotBlank()) {
                        HorizontalDivider()
                        Text("Quote", fontWeight = FontWeight.SemiBold)
                        Text("“$quote”")
                    }
                }
            }
        }
    }
}
