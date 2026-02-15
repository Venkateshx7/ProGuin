@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui.journey

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun JourneyHubScreen(
    onStartToday: () -> Unit
) {
    val context = LocalContext.current

    val py = remember { Python.getInstance() }
    val journey = remember { py.getModule("proguin.journey") }
    val path = remember { File(context.filesDir, "journey.json").absolutePath }

    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var day by remember { mutableIntStateOf(1) }
    var xp by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }

    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    // Day plan UI state
    var planLoading by remember { mutableStateOf(false) }
    var planError by remember { mutableStateOf("") }

    var arcTitle by rememberSaveable { mutableStateOf("") }
    var dayTitle by rememberSaveable { mutableStateOf("") }
    var story by rememberSaveable { mutableStateOf("") }
    var quote by rememberSaveable { mutableStateOf("") }

    var imageKey by rememberSaveable { mutableStateOf("arc1_base") }
    var overlayTintHex by rememberSaveable { mutableStateOf("#00E5FF") }
    var overlayAlpha by rememberSaveable { mutableFloatStateOf(0.10f) }
    var tiltDeg by rememberSaveable { mutableFloatStateOf(0f) }
    var zoom by rememberSaveable { mutableFloatStateOf(1.02f) }
    var gradientAHex by rememberSaveable { mutableStateOf("#0B0F1A") }
    var gradientBHex by rememberSaveable { mutableStateOf("#101A2B") }

    var tasksPreview by remember { mutableStateOf(listOf<Pair<String, Int>>()) }

    fun pyInt(map: Map<PyObject, PyObject>, key: String, fallback: Int): Int {
        return try {
            val v = map[PyObject.fromJava(key)]?.toString().orEmpty()
            v.toIntOrNull() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun pyStr(map: Map<PyObject, PyObject>, key: String, fallback: String = ""): String {
        return try {
            map[PyObject.fromJava(key)]?.toString() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun pyFloat(map: Map<PyObject, PyObject>, key: String, fallback: Float): Float {
        return try {
            val v = map[PyObject.fromJava(key)]?.toString().orEmpty()
            v.toFloatOrNull() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun colorFromHex(hex: String, fallback: Color = Color(0xFF00E5FF)) : Color {
        return try {
            val clean = hex.trim().removePrefix("#")
            val value = clean.toLong(16)
            val argb = if (clean.length == 6) (0xFF000000 or value).toInt() else value.toInt()
            Color(argb)
        } catch (_: Exception) {
            fallback
        }
    }

    suspend fun loadProgressAndPlan() {
        loading = true
        planLoading = true
        errorMsg = ""
        planError = ""

        try {
            val dataObj = withContext(Dispatchers.IO) {
                journey.callAttr("load", path)
            }
            val dataMap = dataObj.asMap()

            day = pyInt(dataMap, "current_day", 1)
            xp = pyInt(dataMap, "xp", 0)
            streak = pyInt(dataMap, "streak", 0)

            val planObj = withContext(Dispatchers.IO) {
                journey.callAttr("get_day_plan", day)
            }
            val pm = planObj.asMap()

            arcTitle = pyStr(pm, "arc_title", "ARC 1")
            dayTitle = pyStr(pm, "day_title", "Day $day")
            story = pyStr(pm, "story", "")
            quote = pyStr(pm, "quote", "")

            imageKey = pyStr(pm, "image_key", "arc1_base")
            overlayTintHex = pyStr(pm, "overlay_tint", "#00E5FF")
            overlayAlpha = pyFloat(pm, "overlay_alpha", 0.10f)
            tiltDeg = pyFloat(pm, "tilt_deg", 0f)
            zoom = pyFloat(pm, "zoom", 1.02f)
            gradientAHex = pyStr(pm, "gradient_a", "#0B0F1A")
            gradientBHex = pyStr(pm, "gradient_b", "#101A2B")

            // tasks preview
            val tasksObj = pm[PyObject.fromJava("tasks")]
            val list = mutableListOf<Pair<String, Int>>()

            if (tasksObj != null) {
                try {
                    for (t in tasksObj.asList()) {
                        val tm = t.asMap()
                        val name = tm[PyObject.fromJava("name")]?.toString().orEmpty()
                        val mins = tm[PyObject.fromJava("minutes")]?.toString()?.toIntOrNull() ?: 0
                        if (name.isNotBlank()) list.add(name to mins)
                    }
                } catch (_: Exception) {
                    // ignore
                }
            }

            tasksPreview = list

        } catch (e: Exception) {
            errorMsg = e.message ?: "Journey load failed"
        } finally {
            loading = false
            planLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProgressAndPlan()
    }

    val bgA = colorFromHex(gradientAHex, Color(0xFF0B0F1AL))
    val bgB = colorFromHex(gradientBHex, Color(0xFF101A2BL))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("74-Day Penguin Journey", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (loading) "Loading..." else "Unlocked Day: $day",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                actions = {
                    TextButton(
                        enabled = !loading && !planLoading,
                        onClick = {
                            scope.launch { loadProgressAndPlan() }
                        }
                    ) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { inner ->

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bgA, bgB)))
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            if (errorMsg.isNotBlank()) {
                Card {
                    Column(Modifier.padding(14.dp)) {
                        Text("Error", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(errorMsg)
                    }
                }
            }

            // =========================
            // Image with layers (Set 1)
            // =========================
            Card(
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(arcTitle, fontWeight = FontWeight.SemiBold)

                    if (planLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        ArcImageWithLayers(
                            imageResName = imageKey,
                            overlayTint = colorFromHex(overlayTintHex),
                            overlayAlpha = overlayAlpha,
                            tiltDeg = tiltDeg,
                            zoom = zoom
                        )
                    }

                    Text(
                        dayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (story.isNotBlank()) {
                        Text(story, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (quote.isNotBlank()) {
                        Divider()
                        Text("“$quote”", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // =========================
            // Progress
            // =========================
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Progress", fontWeight = FontWeight.SemiBold)
                    Text("Day: $day / 74")
                    Text("XP: $xp")
                    Text("Streak: $streak days")
                }
            }

            // =========================
            // Tasks preview
            // =========================
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Today’s Tasks Preview", fontWeight = FontWeight.SemiBold)

                    if (tasksPreview.isEmpty()) {
                        Text("No tasks found for this day plan.")
                    } else {
                        tasksPreview.forEachIndexed { index, item ->
                            val name = item.first
                            val mins = item.second
                            Text("• ${index + 1}. $name  (${mins}m)")
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap Start Today to auto-generate today’s plan into your Tasks screen.",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // =========================
            // Buttons
            // =========================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val i = Intent("com.venkatesh.proguin.GENERATE_DAY_PLAN")
                        i.setPackage(context.packageName)
                        i.putExtra("day", day)
                        context.sendBroadcast(i)

                        onStartToday()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !loading && !planLoading
                ) {
                    Text("Start Today")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch { loadProgressAndPlan() }
                    },
                    enabled = !loading && !planLoading
                ) {
                    Text("Reload")
                }
            }

            if (planError.isNotBlank()) {
                Text(planError)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ArcImageWithLayers(
    imageResName: String,
    overlayTint: Color,
    overlayAlpha: Float,
    tiltDeg: Float,
    zoom: Float
) {
    val context = LocalContext.current
    val resId = remember(imageResName) {
        context.resources.getIdentifier(imageResName, "drawable", context.packageName)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        rotationZ = tiltDeg,
                        scaleX = zoom,
                        scaleY = zoom
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayTint.copy(alpha = overlayAlpha.coerceIn(0f, 1f)))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
