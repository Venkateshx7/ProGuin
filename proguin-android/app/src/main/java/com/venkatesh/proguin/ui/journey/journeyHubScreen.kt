@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui.journey

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun JourneyHubScreen(
    onStartToday: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val py = remember { Python.getInstance() }
    val journey = remember { py.getModule("proguin.journey") }
    val path = remember { File(context.filesDir, "journey.json").absolutePath }

    // ✅ real progress day
    var unlockedDay by remember { mutableIntStateOf(1) }

    // ✅ currently viewing day (preview)
    var selectedDay by rememberSaveable { mutableIntStateOf(1) }

    var xp by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }

    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var planLoading by remember { mutableStateOf(false) }

    var arcTitle by rememberSaveable { mutableStateOf("") }
    var dayTitle by rememberSaveable { mutableStateOf("") }
    var story by rememberSaveable { mutableStateOf("") }
    var quote by rememberSaveable { mutableStateOf("") }

    var imageKey by rememberSaveable { mutableStateOf("bg_arc1") }
    var overlayTintHex by rememberSaveable { mutableStateOf("#00E5FF") }
    var overlayAlpha by rememberSaveable { mutableFloatStateOf(0.10f) }
    var tiltDeg by rememberSaveable { mutableFloatStateOf(0f) }
    var zoom by rememberSaveable { mutableFloatStateOf(1.02f) }

    var tasksPreview by remember { mutableStateOf(listOf<Pair<String, Int>>()) }

    // ✅ completed days from json
    var completedDays by remember { mutableStateOf(setOf<Int>()) }

    // ✅ If user manually clicked a previous day, don't auto-jump to unlocked day
    var userPickedDay by rememberSaveable { mutableStateOf(false) }

    fun pyInt(map: Map<PyObject, PyObject>, key: String, fallback: Int): Int {
        return try {
            val v = map[PyObject.fromJava(key)]?.toString().orEmpty()
            v.toIntOrNull() ?: fallback
        } catch (_: Exception) { fallback }
    }

    fun pyStr(map: Map<PyObject, PyObject>, key: String, fallback: String = ""): String {
        return try {
            map[PyObject.fromJava(key)]?.toString() ?: fallback
        } catch (_: Exception) { fallback }
    }

    fun pyFloat(map: Map<PyObject, PyObject>, key: String, fallback: Float): Float {
        return try {
            val v = map[PyObject.fromJava(key)]?.toString().orEmpty()
            v.toFloatOrNull() ?: fallback
        } catch (_: Exception) { fallback }
    }

    fun colorFromHex(hex: String, fallback: Color = Color(0xFF00E5FF)): Color {
        return try {
            val clean = hex.trim().removePrefix("#")
            val value = clean.toLong(16)
            val argb = if (clean.length == 6) (0xFF000000 or value).toInt() else value.toInt()
            Color(argb)
        } catch (_: Exception) { fallback }
    }

    suspend fun loadProgressAndPlan() {
        loading = true
        errorMsg = ""
        planLoading = true
        try {
            val dataObj = withContext(Dispatchers.IO) { journey.callAttr("load", path) }
            val dataMap = dataObj.asMap()

            unlockedDay = pyInt(dataMap, "current_day", 1)
            xp = pyInt(dataMap, "xp", 0)
            streak = pyInt(dataMap, "streak", 0)

            completedDays = try {
                val obj = dataMap[PyObject.fromJava("completed_days")]
                obj?.asList()?.mapNotNull { it.toString().toIntOrNull() }?.toSet() ?: emptySet()
            } catch (_: Exception) {
                emptySet()
            }

            // ✅ auto-follow unlocked day unless user selected a previous day
            if (!userPickedDay) {
                selectedDay = unlockedDay
            } else {
                if (selectedDay > unlockedDay) selectedDay = unlockedDay
                if (selectedDay < 1) selectedDay = 1
            }

            val planObj = withContext(Dispatchers.IO) { journey.callAttr("get_day_plan", selectedDay) }
            val pm = planObj.asMap()

            arcTitle = pyStr(pm, "arc_title", "ARC")
            dayTitle = pyStr(pm, "day_title", "Day $selectedDay")
            story = pyStr(pm, "story", "")
            quote = pyStr(pm, "quote", "")

            imageKey = pyStr(pm, "image_key", "bg_arc1")
            overlayTintHex = pyStr(pm, "overlay_tint", "#00E5FF")
            overlayAlpha = pyFloat(pm, "overlay_alpha", 0.10f)
            tiltDeg = pyFloat(pm, "tilt_deg", 0f)
            zoom = pyFloat(pm, "zoom", 1.02f)

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
                } catch (_: Exception) {}
            }
            tasksPreview = list

        } catch (e: Exception) {
            errorMsg = e.message ?: "Journey load failed"
        } finally {
            loading = false
            planLoading = false
        }
    }

    // ✅ First load
    LaunchedEffect(Unit) { loadProgressAndPlan() }

    // ✅ Reload when coming back from tasks/day_complete
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadProgressAndPlan() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val bgResId = remember(imageKey) {
        context.resources.getIdentifier(imageKey, "drawable", context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("74-Day Penguin Journey", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (loading) "Loading..."
                            else "Unlocked Day: $unlockedDay  •  Viewing: Day $selectedDay",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 18.sp)
                    }
                }
            )
        }
    ) { inner ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {

            if (bgResId != 0) {
                Image(
                    painter = painterResource(bgResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0B0F1A), Color(0xFF101A2B))
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                item {
                    if (errorMsg.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Error", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(errorMsg)
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                        )
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

                            if (story.isNotBlank()) Text(story)

                            if (quote.isNotBlank()) {
                                HorizontalDivider()
                                Text("“$quote”")
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Progress", fontWeight = FontWeight.SemiBold)
                            Text("Unlocked Day: $unlockedDay / 74")
                            Text("XP: $xp")
                            Text("Streak: $streak days")
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            Text("Tasks Preview (Day $selectedDay)", fontWeight = FontWeight.SemiBold)

                            if (tasksPreview.isEmpty()) {
                                Text("No tasks found for this day plan.")
                            } else {
                                tasksPreview.forEachIndexed { index, item ->
                                    Text("• ${index + 1}. ${item.first} (${item.second}m)")
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text("Open Previous Completed Days", fontWeight = FontWeight.SemiBold)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val maxChipDay = unlockedDay.coerceAtLeast(1)
                                for (d in 1..maxChipDay) {
                                    val done = completedDays.contains(d)
                                    AssistChip(
                                        onClick = {
                                            if (!planLoading) {
                                                userPickedDay = true
                                                selectedDay = d
                                                scope.launch { loadProgressAndPlan() }
                                            }
                                        },
                                        label = { Text(if (done) "Day $d ✅" else "Day $d") }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    // ✅ Always start unlocked day
                                    userPickedDay = false
                                    selectedDay = unlockedDay

                                    val i = Intent("com.venkatesh.proguin.GENERATE_DAY_PLAN").apply {
                                        setPackage(context.packageName)
                                        putExtra("day", unlockedDay)
                                    }
                                    context.sendBroadcast(i)

                                    onStartToday()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loading && !planLoading
                            ) { Text("Start Today") }
                        }
                    }
                }

                item { Spacer(Modifier.height(30.dp)) }
            }
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
