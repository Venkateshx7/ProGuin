@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package com.venkatesh.proguin

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.venkatesh.proguin.alarm.AlarmScheduler
import com.venkatesh.proguin.alarm.NotificationHelper
import com.venkatesh.proguin.alarm.TimerForegroundService
import com.venkatesh.proguin.ui.BackupUi
import com.venkatesh.proguin.ui.ModeSelectScreen
import com.venkatesh.proguin.ui.TaskCardPremium
import com.venkatesh.proguin.ui.WelcomeScreen
import com.venkatesh.proguin.ui.infinite.InfiniteHubScreen
import com.venkatesh.proguin.ui.journey.DayCompleteScreen
import com.venkatesh.proguin.ui.journey.DayOverviewScreen
import com.venkatesh.proguin.ui.journey.Day74FinishScreen
import com.venkatesh.proguin.ui.journey.JourneyHubScreen
import com.venkatesh.proguin.ui.journey.JourneyIntroScreen
import com.venkatesh.proguin.ui.theme.NeonCyan
import com.venkatesh.proguin.ui.theme.NeonPink
import com.venkatesh.proguin.ui.theme.ProGuinTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class TaskUi(
    val id: String,
    val name: String,
    val timerMinutesText: String,
    val timerMinutes: Int,
    val rewardText: String,
    val scheduledStartText: String,
    val startedAtText: String,
    val completed: Boolean
)

// Compatibility alias (fixes old references)
private typealias Task = TaskUi

private enum class TaskTab(val title: String) {
    ALL("All"),
    RUNNING("Running"),
    SCHEDULED("Scheduled"),
    COMPLETED("Completed")
}

class MainActivity : ComponentActivity() {

    private val refreshTick = mutableIntStateOf(0)

    private val pagesUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshTick.intValue++
        }
    }

    // ✅ Auto day generator receiver (Journey -> Tasks)
    private val generateDayPlanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val day = intent.getIntExtra("day", 1).coerceIn(1, 74)
            try {
                generateJourneyDayIntoTasks(context, day)

                // ✅ FIX #1: force correct page immediately (prevents wrong day / race)
                val dayId = "journey_day_" + day.toString().padStart(2, '0')
                setCurrentPageInPagesJson(context, dayId)

            } catch (_: Exception) {
            } finally {
                refreshTick.intValue++
            }
        }
    }

    // ✅ Switch pages.json current_page (used for Infinite / Journey)
    private fun setCurrentPageInPagesJson(context: Context, pageId: String) {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            val py = Python.getInstance()
            val core = py.getModule("proguin.core")
            val pagesPath = File(context.filesDir, "pages.json").absolutePath

            val pages = core.callAttr("load_pages", pagesPath)
            val pagesMap = pages.asMap()

            pagesMap[PyObject.fromJava("current_page")] = PyObject.fromJava(pageId)

            core.callAttr("save_pages", pagesPath, pages)
            sendPagesUpdatedBroadcast(context)
        } catch (_: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        enableEdgeToEdge()

        setContent {
            ProGuinTheme {

                var page by remember { mutableStateOf("welcome") }

                // ✅ remembers where Tasks was opened from
                var tasksEntry by rememberSaveable { mutableStateOf("mode") }

                // ✅ remember which day got completed
                var completedDayArg by remember { mutableIntStateOf(1) }

                // ✅ for DayOverviewScreen
                var overviewDayArg by remember { mutableIntStateOf(1) }

                when (page) {

                    "welcome" -> WelcomeScreen(
                        onStart = { page = "mode" }
                    )

                    "mode" -> ModeSelectScreen(
                        // ✅ open intro first, not directly hub
                        on74Days = { page = "journey_intro" },
                        onInfinite = { page = "infinite" },
                        onBack = { page = "welcome" }
                    )

                    // ✅ Journey Intro
                    "journey_intro" -> JourneyIntroScreen(
                        onBack = { page = "mode" },
                        onStartJourney = { page = "journey" }
                    )

                    // ✅ Infinite separate screen -> then opens Tasks
                    "infinite" -> InfiniteHubScreen(
                        onOpenInfiniteTasks = {
                            tasksEntry = "infinite" // ✅ important
                            setCurrentPageInPagesJson(this@MainActivity, "default")
                            page = "tasks"
                        },
                        onBack = { page = "mode" }
                    )

                    "tasks" -> TasksPremiumScreen(
                        refreshTickValue = refreshTick.intValue,
                        // ✅ Back target depends on where Tasks came from
                        onBackToMode = {
                            when (tasksEntry) {
                                "infinite" -> page = "infinite"   // ✅ Tasks -> InfiniteHub
                                "journey" -> page = "journey"     // ✅ Tasks -> JourneyHub
                                else -> page = "mode"
                            }
                        },

                        // ✅ open overview (story/quote) for current day
                        onOpenDayOverview = { day ->
                            overviewDayArg = day.coerceIn(1, 74)
                            page = "day_overview"
                        },

                        // ✅ pass which day completed
                        onDayCompleted = { doneDay ->
                            completedDayArg = doneDay
                            refreshTick.intValue++

                            // ✅ Always go to DayComplete first (saves progress there)
                            page = "day_complete"
                        }
                    )

                    "journey" -> {
                        JourneyHubScreen(
                            onStartToday = {
                                tasksEntry = "journey" // ✅ important
                                page = "tasks"
                            },
                            onBack = {
                                page = "mode"
                            }
                        )
                    }

                    "day_overview" -> DayOverviewScreen(
                        day = overviewDayArg,
                        onBack = { page = "tasks" }
                    )

                    "day_complete" -> {
                        DayCompleteScreen(
                            completedDay = completedDayArg,
                            onContinue = {
                                refreshTick.intValue++

                                // ✅ If Day 74 completed -> show final finish screen
                                if (completedDayArg >= 74) {
                                    page = "finish74"
                                } else {
                                    page = "journey"
                                }
                            }
                        )
                    }

                    "finish74" -> Day74FinishScreen(
                        onBackToMode = { page = "mode" }
                    )

                    else -> WelcomeScreen(onStart = { page = "mode" })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter("com.venkatesh.proguin.PAGES_UPDATED")
        ContextCompat.registerReceiver(
            this,
            pagesUpdatedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val genFilter = IntentFilter("com.venkatesh.proguin.GENERATE_DAY_PLAN")
        ContextCompat.registerReceiver(
            this,
            generateDayPlanReceiver,
            genFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(pagesUpdatedReceiver)
        } catch (_: Exception) {
        }
        try {
            unregisterReceiver(generateDayPlanReceiver)
        } catch (_: Exception) {
        }
    }

    // ===========================
    // Auto day generator (Journey -> Tasks pages.json)
    // ===========================
    private fun generateJourneyDayIntoTasks(context: Context, day: Int) {

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val core = py.getModule("proguin.core")
        val journey = py.getModule("proguin.journey")
        val pagesPath = File(context.filesDir, "pages.json").absolutePath

        val dayId = "journey_day_" + day.toString().padStart(2, '0')
        val dayTitle = "Journey Day " + day.toString().padStart(2, '0')

        val pages = core.callAttr("load_pages", pagesPath)
        val pagesMap = pages.asMap()

        // Create page if missing
        val pagesContainerObj = pagesMap[PyObject.fromJava("pages")]
        val pagesContainer = pagesContainerObj?.asMap() ?: emptyMap()
        val pageKey = PyObject.fromJava(dayId)

        if (!pagesContainer.containsKey(pageKey)) {
            core.callAttr("add_page", pages, dayId, dayTitle)
        }

        // Switch current page always
        pagesMap[PyObject.fromJava("current_page")] = PyObject.fromJava(dayId)

        // Check if already generated tasks
        val pagesContainer2 = pages.asMap()[PyObject.fromJava("pages")]?.asMap() ?: emptyMap()
        val dayPageObj = pagesContainer2[pageKey]
        val dayPageMap = dayPageObj?.asMap() ?: emptyMap()
        val tasksObj = dayPageMap[PyObject.fromJava("tasks")]
        val existingTasksCount = try {
            tasksObj?.asList()?.size ?: 0
        } catch (_: Exception) {
            0
        }

        if (existingTasksCount > 0) {
            core.callAttr("save_pages", pagesPath, pages)
            sendPagesUpdatedBroadcast(context)
            return
        }

        // Get plan from journey.py
        val plan = journey.callAttr("get_day_plan", day)
        val planMap = plan.asMap()
        val planTasksObj = planMap[PyObject.fromJava("tasks")]
        val planTasks = try {
            planTasksObj?.asList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        for (t in planTasks) {

            val tm = t.asMap()
            val name = tm[PyObject.fromJava("name")]?.toString().orEmpty()
            val minutes = tm[PyObject.fromJava("minutes")]?.toString()?.toIntOrNull()

            if (name.isBlank()) continue

            val task = core.callAttr(
                "build_task",
                name,
                minutes,
                null,
                null
            )

            core.callAttr("add_task_to_current_page", pages, task)
        }

        core.callAttr("save_pages", pagesPath, pages)
        sendPagesUpdatedBroadcast(context)
    }

    private fun sendPagesUpdatedBroadcast(context: Context) {
        val i = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(i)
    }

    // ===========================
    // UI Helpers
    // ===========================

    @Composable
    private fun PremiumBackground(content: @Composable () -> Unit) {

        val cs = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.18f),
                            cs.background
                        )
                    )
                )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonPink.copy(alpha = 0.14f),
                                cs.background
                            ),
                            center = Offset(1000f, 1200f)
                        )
                    )
            )

            content()
        }
    }

    // ✅ day from "journey_day_01"
    private fun dayFromPageId(pageId: String): Int? {
        if (!pageId.startsWith("journey_day_")) return null
        return pageId.removePrefix("journey_day_").toIntOrNull()
    }

    // ✅ Week mapping: 1-7 => week1, 8-14 => week2 ...
    private fun weekFromDay(day: Int): Int {
        return ((day - 1) / 7) + 1
    }

    private data class Quad(
        val a: Color,
        val b: Color,
        val scrim: Float,
        val dir: Int
    )

    @Composable
    private fun TaskScreenBackground(
        currentPageId: String,
        content: @Composable () -> Unit
    ) {
        val day = remember(currentPageId) { dayFromPageId(currentPageId) }
        val week = remember(day) { day?.let { weekFromDay(it) } }

        val bgResId = remember(currentPageId, week) {
            when {
                currentPageId == "default" || currentPageId == "infinite" -> R.drawable.penguin_bg

                week != null -> when (week) {
                    1 -> R.drawable.bg_arc1
                    2 -> R.drawable.bg_arc2
                    3 -> R.drawable.bg_arc3
                    4 -> R.drawable.bg_arc4
                    5 -> R.drawable.bg_arc5
                    6 -> R.drawable.bg_arc6
                    7 -> R.drawable.bg_arc7
                    8 -> R.drawable.bg_arc8
                    9 -> R.drawable.bg_arc9
                    10 -> R.drawable.bg_arc10
                    else -> R.drawable.bg_arc11
                }

                else -> R.drawable.penguin_bg
            }
        }

        val (tintA, tintB, baseScrimAlpha, directionIndex) = remember(week) {
            when (week ?: 0) {
                1 -> Quad(Color(0xFF00E5FF), Color(0xFFFF4FD8), 0.10f, 0)
                2 -> Quad(Color(0xFFFF4FD8), Color(0xFF00E5FF), 0.11f, 1)
                3 -> Quad(Color(0xFF7C4DFF), Color(0xFF00E5FF), 0.11f, 2)
                4 -> Quad(Color(0xFFFFC107), Color(0xFFFF4FD8), 0.11f, 3)
                5 -> Quad(Color(0xFF00E676), Color(0xFF00E5FF), 0.11f, 0)
                6 -> Quad(Color(0xFFFF5252), Color(0xFFFFC107), 0.12f, 1)
                7 -> Quad(Color(0xFF40C4FF), Color(0xFF7C4DFF), 0.11f, 2)
                8 -> Quad(Color(0xFFFF4FD8), Color(0xFF00E676), 0.12f, 3)
                9 -> Quad(Color(0xFF00E5FF), Color(0xFFFFC107), 0.11f, 0)
                10 -> Quad(Color(0xFF7C4DFF), Color(0xFFFF4FD8), 0.12f, 1)
                else -> Quad(Color(0xFF00E5FF), Color(0xFFFF4FD8), 0.12f, 2)
            }
        }

        val (start, end) = remember(directionIndex) {
            when (directionIndex) {
                0 -> Offset(0f, 0f) to Offset(1000f, 1200f)
                1 -> Offset(1000f, 0f) to Offset(0f, 1200f)
                2 -> Offset(0f, 1200f) to Offset(1000f, 0f)
                else -> Offset(1000f, 1200f) to Offset(0f, 0f)
            }
        }

        Box(Modifier.fillMaxSize()) {

            Image(
                painter = painterResource(bgResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = baseScrimAlpha.coerceIn(0.04f, 0.09f)))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tintA.copy(alpha = 0.12f),
                                Color.Transparent,
                                tintB.copy(alpha = 0.10f)
                            ),
                            start = start,
                            end = end
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 520f
                        )
                    )
            )

            content()
        }
    }

    data class PageItem(
        val id: String,
        val title: String,
        val isJourney: Boolean
    )

    @Composable
    private fun TasksPremiumScreen(
        refreshTickValue: Int,
        onBackToMode: () -> Unit,
        onOpenDayOverview: (Int) -> Unit,
        onDayCompleted: (Int) -> Unit
    )
    {

        val context = LocalContext.current
        val cs = MaterialTheme.colorScheme

        BackHandler { onBackToMode() }

        // ✅ prevent day_complete loop for same day
        var alreadyTriggeredDayComplete by rememberSaveable { mutableStateOf(false) }

        // ===== Notification permission (Android 13+) =====
        var pendingNotifAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        val notifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            val action = pendingNotifAction
            pendingNotifAction = null

            if (!granted) {
                Toast.makeText(
                    context,
                    "Notifications denied. App still works, but reminders may be hidden.",
                    Toast.LENGTH_LONG
                ).show()
            }

            action?.invoke()
        }

        fun hasNotifPermission(): Boolean {
            if (Build.VERSION.SDK_INT < 33) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun ensureNotificationPermissionThen(action: () -> Unit) {
            if (Build.VERSION.SDK_INT < 33) {
                action()
                return
            }

            val granted = hasNotifPermission()

            if (granted) {
                action()
            } else {
                pendingNotifAction = action
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // ===== Python + storage =====
        val py = remember { Python.getInstance() }
        val core = remember { py.getModule("proguin.core") }
        val pagesPath = remember { File(context.filesDir, "pages.json").absolutePath }

        var pageTitle by remember { mutableStateOf("My Tasks") }
        var currentPageId by remember { mutableStateOf("default") }
        var tasksUi by remember { mutableStateOf(listOf<TaskUi>()) }

        var pageIds by remember { mutableStateOf(listOf<PageItem>()) }

        var selectedTab by remember { mutableStateOf(TaskTab.ALL) }

        // Add Task
        var showAddSheet by remember { mutableStateOf(false) }
        var nameInput by remember { mutableStateOf("") }
        var timerInput by remember { mutableStateOf("") }
        var rewardInput by remember { mutableStateOf("") }
        var scheduledMillis by remember { mutableStateOf<Long?>(null) }
        var scheduledLabel by remember { mutableStateOf("") }

        // ✅ Journey: schedule a specific task
        var scheduleTargetTask by remember { mutableStateOf<TaskUi?>(null) }

        // Infinite pages dialogs
        var showNewPageDialog by remember { mutableStateOf(false) }
        var showRenamePageDialog by remember { mutableStateOf(false) }
        var showDeletePageDialog by remember { mutableStateOf(false) }
        var newPageName by remember { mutableStateOf("") }
        var renamePageName by remember { mutableStateOf("") }

        // Backup / Restore sheet
        var showBackupSheet by remember { mutableStateOf(false) }

        fun taskIdToRequestCode(taskId: String): Int = abs(taskId.hashCode()).coerceAtLeast(1)

        fun isoFromMillis(ms: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            return sdf.format(ms)
        }

        fun prettyFromMillis(ms: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
            return sdf.format(ms)
        }

        fun sendPagesUpdated() {
            val i = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(i)
        }

        fun norm(s: String): String = s.replace("'", "")

        fun refreshFromPages() {

            val pages = core.callAttr("load_pages", pagesPath)
            val pagesMap = pages.asMap()

            val cpRaw = pagesMap[PyObject.fromJava("current_page")]?.toString() ?: "default"
            val cpId = norm(cpRaw)
            currentPageId = cpId

            val pagesContainer = pagesMap[PyObject.fromJava("pages")]?.asMap() ?: emptyMap()

            pageIds = pagesContainer.map { (k, v) ->
                val id = norm(k.toString())

                val title = try {
                    val vm = v.asMap()
                    norm(vm[PyObject.fromJava("title")]?.toString() ?: id)
                } catch (_: Exception) {
                    id
                }

                PageItem(
                    id = id,
                    title = title,
                    isJourney = id.startsWith("journey_day_")
                )
            }.sortedBy { it.title.lowercase() }

            pageTitle = pageIds.firstOrNull { it.id == cpId }?.title ?: "My Tasks"

            val currentPageObj =
                pagesContainer[PyObject.fromJava(cpId)] ?: pagesContainer[PyObject.fromJava(cpRaw)]
            val currentPageMap = currentPageObj?.asMap() ?: emptyMap()

            val tasksObj = currentPageMap[PyObject.fromJava("tasks")]
            val taskList = tasksObj?.asList() ?: emptyList()

            tasksUi = taskList.map { task ->
                val m = task.asMap()

                val timerText = m[PyObject.fromJava("timer_minutes")]?.toString().orEmpty()
                    .let { if (it == "None") "" else it }
                val timerMinutesInt = timerText.trim().toIntOrNull() ?: 0

                val rewardText2 = m[PyObject.fromJava("reward")]?.toString().orEmpty()
                    .let { if (it == "None") "" else it }

                val startedText = m[PyObject.fromJava("started_at")]?.toString().orEmpty()
                    .let { if (it == "None") "" else it }

                val scheduledText = m[PyObject.fromJava("scheduled_start")]?.toString().orEmpty()
                    .let { if (it == "None") "" else it }

                val completed = m[PyObject.fromJava("completed")]?.toString().orEmpty()
                    .equals("True", ignoreCase = true)

                TaskUi(
                    id = m[PyObject.fromJava("id")]?.toString().orEmpty(),
                    name = m[PyObject.fromJava("name")]?.toString().orEmpty(),
                    timerMinutesText = timerText,
                    timerMinutes = timerMinutesInt,
                    rewardText = rewardText2,
                    scheduledStartText = scheduledText,
                    startedAtText = startedText,
                    completed = completed
                )
            }
        }

        fun setCurrentPage(pageId: String) {
            try {
                val pages = core.callAttr("load_pages", pagesPath)
                core.callAttr("set_current_page", pages, pageId)
                core.callAttr("save_pages", pagesPath, pages)

                sendPagesUpdated()
                refreshFromPages()
            } catch (_: Exception) {
            }
        }

        fun checkIfAllCompletedAndGoNext() {
            val list = tasksUi
            val isJourney = currentPageId.startsWith("journey_day_")
            val allDone = list.isNotEmpty() && list.all { it.completed }

            if (isJourney && allDone) {
                if (!alreadyTriggeredDayComplete) {
                    alreadyTriggeredDayComplete = true
                    val doneDay = dayFromPageId(currentPageId) ?: 1
                    onDayCompleted(doneDay)
                }
            } else {
                alreadyTriggeredDayComplete = false
            }
        }

        LaunchedEffect(Unit) {
            NotificationHelper.ensureChannels(context)
            refreshFromPages()
            checkIfAllCompletedAndGoNext()
        }

        LaunchedEffect(refreshTickValue) {
            refreshFromPages()
            checkIfAllCompletedAndGoNext()
        }

        val activeTaskId: String = remember(refreshTickValue) {
            TimerForegroundService.getActiveTaskId(context)
        }

        fun isRunning(t: TaskUi): Boolean =
            activeTaskId.isNotBlank() && activeTaskId == t.id && !t.completed

        fun isScheduled(t: TaskUi): Boolean =
            t.scheduledStartText.isNotBlank() && !t.completed && !isRunning(t)

        fun tasksForTab(tab: TaskTab): List<TaskUi> {
            return tasksUi.filter { t ->
                when (tab) {
                    TaskTab.ALL -> true
                    TaskTab.RUNNING -> isRunning(t)
                    TaskTab.SCHEDULED -> isScheduled(t)
                    TaskTab.COMPLETED -> t.completed
                }
            }
        }

        val pagerState = rememberPagerState(
            initialPage = selectedTab.ordinal,
            pageCount = { TaskTab.entries.size }
        )

        LaunchedEffect(pagerState.currentPage) {
            selectedTab = TaskTab.entries[pagerState.currentPage]
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        fun openSchedulePickerForTask(task: TaskUi) {
            scheduleTargetTask = task
            scheduledMillis = null
            scheduledLabel = ""

            val cal = Calendar.getInstance()
            val dp = DatePickerDialog(
                context,
                { _, y, m, d ->
                    val picked = Calendar.getInstance()
                    picked.set(Calendar.YEAR, y)
                    picked.set(Calendar.MONTH, m)
                    picked.set(Calendar.DAY_OF_MONTH, d)

                    val tp = TimePickerDialog(
                        context,
                        { _, hh, mm ->
                            picked.set(Calendar.HOUR_OF_DAY, hh)
                            picked.set(Calendar.MINUTE, mm)
                            picked.set(Calendar.SECOND, 0)
                            picked.set(Calendar.MILLISECOND, 0)

                            val ms = picked.timeInMillis
                            scheduledMillis = ms
                            scheduledLabel = prettyFromMillis(ms)

                            // ✅ Save schedule for this task by ID + schedule alarm
                            val target = scheduleTargetTask
                            if (target == null) return@TimePickerDialog

                            val schedIso = isoFromMillis(ms)

                            try {
                                val pages = core.callAttr("load_pages", pagesPath)

                                // ✅ requires core.py helper: set_task_schedule_by_id(pages, taskId, schedIso)
                                val okObj = core.callAttr("set_task_schedule_by_id", pages, target.id, schedIso)
                                core.callAttr("save_pages", pagesPath, pages)

                                ensureNotificationPermissionThen {
                                    try {
                                        val reqCode = taskIdToRequestCode(target.id)

                                        val ok = AlarmScheduler.scheduleAllowWhileIdle(
                                            context = context,
                                            requestCode = reqCode,
                                            triggerAtMillis = ms,
                                            taskId = target.id,
                                            taskName = target.name.ifBlank { "Task" },
                                            timerMinutes = target.timerMinutes
                                        )

                                        Toast.makeText(
                                            context,
                                            if (ok) "Scheduled ✅" else "Schedule failed ❌",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Schedule failed ❌",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                                sendPagesUpdated()
                                refreshFromPages()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Schedule save failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                scheduleTargetTask = null
                            }
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        false
                    )
                    tp.show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dp.show()
        }

        TaskScreenBackground(currentPageId = currentPageId) {

            val isJourney = currentPageId.startsWith("journey_day_")

            Scaffold(
                containerColor = Color.Transparent,

                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        title = {
                            Column {
                                Text(
                                    pageTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (isJourney) "Journey" else "Infinite",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackToMode) {
                                Text("←", fontSize = 18.sp)
                            }
                        },
                        actions = {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Text("⋮", fontSize = 18.sp)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {

                                    // ✅ NEW: Day overview for Journey pages (story/quote)
                                    if (isJourney) {
                                        DropdownMenuItem(
                                            text = { Text("Day Overview") },
                                            onClick = {
                                                expanded = false
                                                val d = dayFromPageId(currentPageId) ?: 1
                                                onOpenDayOverview(d)
                                            }
                                        )
                                    }

                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        onClick = {
                                            expanded = false
                                            refreshFromPages()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Backup / Restore") },
                                        onClick = {
                                            expanded = false
                                            showBackupSheet = true
                                        }
                                    )

                                    if (!isJourney) {

                                        DropdownMenuItem(
                                            text = { Text("New Page") },
                                            onClick = { expanded = false; showNewPageDialog = true }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Rename Page") },
                                            onClick = {
                                                expanded = false
                                                renamePageName = pageTitle
                                                showRenamePageDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Delete Page") },
                                            onClick = {
                                                expanded = false
                                                showDeletePageDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                },

                // ✅ Hide “+” only in Journey mode
                floatingActionButton = {
                    if (!isJourney) {
                        FloatingActionButton(
                            onClick = {
                                nameInput = ""
                                timerInput = ""
                                rewardInput = ""
                                scheduledMillis = null
                                scheduledLabel = ""
                                showAddSheet = true
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add")
                        }
                    }
                }

            ) { innerPadding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {

                    if (!isJourney) {

                        val infinitePages = pageIds
                            .filter { !it.isJourney }
                            .sortedBy { it.title.lowercase() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            infinitePages.forEach { p ->
                                FilterChip(
                                    selected = p.id == currentPageId,
                                    onClick = { setCurrentPage(p.id) },
                                    label = {
                                        Text(
                                            p.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (isJourney) {

                        val journeyPages = pageIds
                            .filter { it.isJourney }
                            .sortedBy { it.id }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            journeyPages.forEach { p ->
                                FilterChip(
                                    selected = p.id == currentPageId,
                                    onClick = { setCurrentPage(p.id) },
                                    label = {
                                        Text(
                                            p.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->

                        val tab = TaskTab.entries[pageIndex]
                        val visibleTasks = tasksForTab(tab)

                        if (visibleTasks.isEmpty()) {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = cs.surface.copy(alpha = 0.90f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("No tasks here yet.", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (isJourney) "Tasks will appear for this day automatically."
                                            else "Tap “+” to create one.",
                                            color = cs.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                        } else {

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(visibleTasks) { _, t ->
                                    val indexInAll = tasksUi.indexOfFirst { it.id == t.id }

                                    TaskCardPremium(
                                        task = t,
                                        running = isRunning(t),
                                        scheduled = isScheduled(t),

                                        onStart = {
                                            ensureNotificationPermissionThen {

                                                try {
                                                    val py2 = Python.getInstance()
                                                    val core2 = py2.getModule("proguin.core")
                                                    val pagesPath2 = File(context.filesDir, "pages.json").absolutePath

                                                    val pages2 = core2.callAttr("load_pages", pagesPath2)
                                                    core2.callAttr("start_task_by_id", pages2, t.id)
                                                    core2.callAttr("save_pages", pagesPath2, pages2)
                                                } catch (_: Exception) {}

                                                try {
                                                    NotificationHelper.showReminder(
                                                        context = context,
                                                        title = "Started ▶️",
                                                        message = t.name.ifBlank { "Task" }
                                                    )
                                                } catch (_: Exception) {}

                                                val minutes = t.timerMinutesText.toIntOrNull() ?: 0
                                                if (minutes > 0) {
                                                    TimerForegroundService.startTimer(
                                                        context = context,
                                                        taskId = t.id,
                                                        taskName = t.name,
                                                        minutes = minutes
                                                    )
                                                }

                                                val updateIntent = Intent("com.venkatesh.proguin.PAGES_UPDATED").apply {
                                                    setPackage(context.packageName)
                                                }
                                                context.sendBroadcast(updateIntent)
                                            }
                                        },

                                        onDone = {
                                            val reqCode = taskIdToRequestCode(t.id)
                                            AlarmScheduler.cancel(context, reqCode)
                                            TimerForegroundService.stopTimer(context, t.id)

                                            val pages = core.callAttr("load_pages", pagesPath)
                                            core.callAttr("mark_task_done_current_page", pages, indexInAll)
                                            core.callAttr("save_pages", pagesPath, pages)

                                            try {
                                                if (hasNotifPermission()) {
                                                    NotificationHelper.showReminder(
                                                        context = context,
                                                        title = "Completed ✅",
                                                        message = t.name.ifBlank { "Task" }
                                                    )
                                                }
                                            } catch (_: Exception) {}

                                            sendPagesUpdated()
                                            refreshFromPages()
                                            checkIfAllCompletedAndGoNext()
                                        },

                                        onDelete = {
                                            val reqCode = taskIdToRequestCode(t.id)
                                            AlarmScheduler.cancel(context, reqCode)
                                            TimerForegroundService.stopTimer(context, t.id)

                                            val pages = core.callAttr("load_pages", pagesPath)
                                            core.callAttr("delete_task_current_page", pages, indexInAll)
                                            core.callAttr("save_pages", pagesPath, pages)

                                            sendPagesUpdated()
                                            refreshFromPages()
                                            checkIfAllCompletedAndGoNext()
                                        },

                                        // ✅ Schedule button ONLY for Journey
                                        showSchedule = isJourney,
                                        onSchedule = {
                                            openSchedulePickerForTask(t)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showBackupSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBackupSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        BackupUi()
                    }
                }

                if (showNewPageDialog) {
                    AlertDialog(
                        onDismissRequest = { showNewPageDialog = false },
                        title = { Text("Create new page") },
                        text = {
                            TextField(
                                value = newPageName,
                                onValueChange = { newPageName = it },
                                label = { Text("Page title") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                val title = newPageName.trim()
                                if (title.isBlank()) return@Button

                                try {
                                    val pages = core.callAttr("load_pages", pagesPath)
                                    val id = "page_" + System.currentTimeMillis().toString()

                                    core.callAttr("add_page", pages, id, title)
                                    core.callAttr("set_current_page", pages, id)
                                    core.callAttr("save_pages", pagesPath, pages)

                                    newPageName = ""
                                    showNewPageDialog = false

                                    sendPagesUpdated()
                                    refreshFromPages()
                                } catch (_: Exception) {
                                }
                            }) { Text("Create") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNewPageDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showRenamePageDialog) {
                    AlertDialog(
                        onDismissRequest = { showRenamePageDialog = false },
                        title = { Text("Rename page") },
                        text = {
                            TextField(
                                value = renamePageName,
                                onValueChange = { renamePageName = it },
                                label = { Text("New title") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                val title = renamePageName.trim()
                                if (title.isBlank()) return@Button

                                try {
                                    val pages = core.callAttr("load_pages", pagesPath)
                                    val pagesMap = pages.asMap()
                                    val pagesContainer =
                                        pagesMap[PyObject.fromJava("pages")]?.asMap()

                                    val pageObj =
                                        pagesContainer?.get(PyObject.fromJava(currentPageId))
                                    val pageMap = pageObj?.asMap()

                                    pageMap?.set(
                                        PyObject.fromJava("title"),
                                        PyObject.fromJava(title)
                                    )

                                    core.callAttr("save_pages", pagesPath, pages)

                                    showRenamePageDialog = false
                                    sendPagesUpdated()
                                    refreshFromPages()
                                } catch (_: Exception) {
                                }
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showRenamePageDialog = false
                            }) { Text("Cancel") }
                        }
                    )
                }

                if (showDeletePageDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeletePageDialog = false },
                        title = { Text("Delete this page?") },
                        text = { Text("This will delete the page and its tasks.") },
                        confirmButton = {
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F)
                                ),
                                onClick = {
                                    try {
                                        val pages = core.callAttr("load_pages", pagesPath)
                                        core.callAttr("delete_page", pages, currentPageId)
                                        core.callAttr("save_pages", pagesPath, pages)

                                        showDeletePageDialog = false
                                        sendPagesUpdated()
                                        refreshFromPages()
                                    } catch (_: Exception) {
                                    }
                                }
                            ) { Text("Delete", color = Color.White) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeletePageDialog = false
                            }) { Text("Cancel") }
                        }
                    )
                }

                // ✅ Add Task BottomSheet (Infinite only, because FAB is hidden in Journey)
                if (showAddSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showAddSheet = false },
                        sheetState = sheetState
                    ) {

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 22.dp)
                        ) {

                            item {
                                Text("Create Task", style = MaterialTheme.typography.headlineSmall)
                            }

                            item {
                                TextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Task name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                Text(
                                    "Quick timer",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    AssistChip(
                                        onClick = { timerInput = "1" },
                                        label = { Text("1m") })
                                    AssistChip(
                                        onClick = { timerInput = "5" },
                                        label = { Text("5m") })
                                    AssistChip(
                                        onClick = { timerInput = "10" },
                                        label = { Text("10m") })
                                    AssistChip(
                                        onClick = { timerInput = "25" },
                                        label = { Text("25m") })
                                    AssistChip(
                                        onClick = { timerInput = "" },
                                        label = { Text("No") })
                                }
                            }

                            item {
                                TextField(
                                    value = timerInput,
                                    onValueChange = {
                                        timerInput = it.filter { ch -> ch.isDigit() }
                                    },
                                    label = { Text("Timer minutes (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                TextField(
                                    value = rewardInput,
                                    onValueChange = { rewardInput = it },
                                    label = { Text("Reward (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                Text(
                                    "Schedule (optional)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    OutlinedButton(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            val dp = DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val picked = Calendar.getInstance()
                                                    picked.set(Calendar.YEAR, y)
                                                    picked.set(Calendar.MONTH, m)
                                                    picked.set(Calendar.DAY_OF_MONTH, d)

                                                    val tp = TimePickerDialog(
                                                        context,
                                                        { _, hh, mm ->
                                                            picked.set(Calendar.HOUR_OF_DAY, hh)
                                                            picked.set(Calendar.MINUTE, mm)
                                                            picked.set(Calendar.SECOND, 0)
                                                            picked.set(Calendar.MILLISECOND, 0)

                                                            val ms = picked.timeInMillis
                                                            scheduledMillis = ms
                                                            scheduledLabel = prettyFromMillis(ms)
                                                        },
                                                        cal.get(Calendar.HOUR_OF_DAY),
                                                        cal.get(Calendar.MINUTE),
                                                        false
                                                    )
                                                    tp.show()
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            )
                                            dp.show()
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.92f),
                                            contentColor = Color(0xFF0B0F1A)
                                        )
                                    ) {
                                        Icon(Icons.Filled.Schedule, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (scheduledLabel.isBlank()) "Set schedule" else "Edit schedule",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scheduledMillis = null
                                            scheduledLabel = ""
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFFFFE8E8),
                                            contentColor = Color(0xFFD32F2F)
                                        )
                                    ) {
                                        Text("Clear", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (scheduledLabel.isNotBlank()) {
                                item {
                                    Text(
                                        "Scheduled: $scheduledLabel",
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        val name = nameInput.trim()
                                        if (name.isBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Enter task name",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }

                                        val minutes = timerInput.trim().toIntOrNull()
                                        val reward = rewardInput.trim().ifBlank { null }
                                        val schedIso: String? =
                                            scheduledMillis?.let { isoFromMillis(it) }

                                        try {
                                            val pages = core.callAttr("load_pages", pagesPath)

                                            val taskObj = core.callAttr(
                                                "build_task",
                                                name,
                                                minutes,
                                                reward,
                                                schedIso
                                            )

                                            core.callAttr(
                                                "add_task_to_current_page",
                                                pages,
                                                taskObj
                                            )
                                            core.callAttr("save_pages", pagesPath, pages)

                                            if (scheduledMillis != null) {
                                                val ms = scheduledMillis!!

                                                ensureNotificationPermissionThen {
                                                    try {
                                                        val taskMap = taskObj.asMap()
                                                        val taskId =
                                                            taskMap[PyObject.fromJava("id")]?.toString()
                                                                .orEmpty()
                                                        val reqCode = taskIdToRequestCode(taskId)

                                                        val ok =
                                                            AlarmScheduler.scheduleAllowWhileIdle(
                                                                context = context,
                                                                requestCode = reqCode,
                                                                triggerAtMillis = ms,
                                                                taskId = taskId,
                                                                taskName = name,
                                                                timerMinutes = minutes ?: 0
                                                            )

                                                        Toast.makeText(
                                                            context,
                                                            if (ok) "Scheduled ✅" else "Schedule failed ❌",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    } catch (_: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            "Schedule failed ❌",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }

                                            sendPagesUpdated()
                                            refreshFromPages()
                                            showAddSheet = false

                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Save failed: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Save Task", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
