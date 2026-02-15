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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.venkatesh.proguin.alarm.AlarmScheduler
import com.venkatesh.proguin.alarm.NotificationHelper
import com.venkatesh.proguin.alarm.TimerForegroundService
import com.venkatesh.proguin.ui.ModeSelectScreen
import com.venkatesh.proguin.ui.WelcomeScreen
import com.venkatesh.proguin.ui.journey.DayCompleteScreen
import com.venkatesh.proguin.ui.journey.JourneyHubScreen
import com.venkatesh.proguin.ui.theme.NeonCyan
import com.venkatesh.proguin.ui.theme.NeonPink
import com.venkatesh.proguin.ui.theme.ProGuinTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class TaskUi(
    val id: String,
    val name: String,
    val timerMinutesText: String,
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
            } catch (_: Exception) {
                // Never crash the app because of generator
            } finally {
                refreshTick.intValue++
            }
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

                when (page) {

                    "welcome" -> WelcomeScreen(onStart = { page = "mode" })

                    "mode" -> ModeSelectScreen(
                        on74Days = { page = "journey" },
                        onInfinite = { page = "tasks" },
                        onBack = { page = "welcome" }
                    )

                    "tasks" -> TasksPremiumScreen(
                        refreshTickValue = refreshTick.intValue,
                        onBackToMode = { page = "mode" },
                        onDayCompleted = { page = "day_complete" }
                    )

                    "journey" -> {
                        JourneyHubScreen(
                            onStartToday = {
                                // JourneyHubScreen already broadcasts GENERATE_DAY_PLAN
                                // and then navigates here (tasks).
                                page = "tasks"
                            }
                        )
                    }

                    "day_complete" -> {
                        DayCompleteScreen(
                            onContinue = {
                                // ✅ Mark day complete in journey.json, advance to next day
                                completeCurrentJourneyDayAndSave(this@MainActivity)
                                // ✅ Refresh tasks UI
                                refreshTick.intValue++
                                // ✅ Back to journey hub
                                page = "journey"
                            }
                        )
                    }

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
        try { unregisterReceiver(pagesUpdatedReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(generateDayPlanReceiver) } catch (_: Exception) {}
    }

    // ===========================
    // Journey completion (DayComplete -> advance)
    // ===========================
    private fun completeCurrentJourneyDayAndSave(context: Context) {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            val py = Python.getInstance()
            val journey = py.getModule("proguin.journey")
            val path = File(context.filesDir, "journey.json").absolutePath

            val data = journey.callAttr("load", path)
            val updated = journey.callAttr("complete_day", data)
            journey.callAttr("save", path, updated)

        } catch (_: Exception) {
            // ignore - never crash app here
        }
    }

    // ===========================
    // Auto day generator (Set-1 -> Tasks pages.json)
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
        val existingTasksCount = try { tasksObj?.asList()?.size ?: 0 } catch (_: Exception) { 0 }

        if (existingTasksCount > 0) {
            core.callAttr("save_pages", pagesPath, pages)
            sendPagesUpdatedBroadcast(context)
            return
        }

        // Get plan from journey.py
        val plan = journey.callAttr("get_day_plan", day)
        val planMap = plan.asMap()
        val planTasksObj = planMap[PyObject.fromJava("tasks")]
        val planTasks = try { planTasksObj?.asList() ?: emptyList() } catch (_: Exception) { emptyList() }

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

    @Composable
    private fun TasksPremiumScreen(
        refreshTickValue: Int,
        onBackToMode: () -> Unit,
        onDayCompleted: () -> Unit
    ) {
        val context = LocalContext.current
        val cs = MaterialTheme.colorScheme

        BackHandler { onBackToMode() }

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

        fun ensureNotificationPermissionThen(action: () -> Unit) {
            if (Build.VERSION.SDK_INT < 33) {
                action(); return
            }
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) action() else {
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
        var pageIds by remember { mutableStateOf(listOf<String>()) }

        var selectedTab by remember { mutableStateOf(TaskTab.ALL) }

        // Add Task
        var showAddSheet by remember { mutableStateOf(false) }
        var nameInput by remember { mutableStateOf("") }
        var timerInput by remember { mutableStateOf("") }
        var rewardInput by remember { mutableStateOf("") }
        var scheduledMillis by remember { mutableStateOf<Long?>(null) }
        var scheduledLabel by remember { mutableStateOf("") }

        // Page dialogs
        var showNewPageDialog by remember { mutableStateOf(false) }
        var showRenamePageDialog by remember { mutableStateOf(false) }
        var showDeletePageDialog by remember { mutableStateOf(false) }
        var newPageName by remember { mutableStateOf("") }
        var renamePageName by remember { mutableStateOf("") }

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

        fun refreshFromPages() {
            val pages = core.callAttr("load_pages", pagesPath)
            val pagesMap = pages.asMap()

            val cpId = pagesMap[PyObject.fromJava("current_page")]?.toString() ?: "default"
            currentPageId = cpId

            val pagesContainer = pagesMap[PyObject.fromJava("pages")]?.asMap() ?: emptyMap()
            pageIds = pagesContainer.keys.map { it.toString().replace("'", "") }.sorted()

            val currentPageObj = pagesContainer[PyObject.fromJava(cpId)]
            val currentPageMap = currentPageObj?.asMap() ?: emptyMap()

            pageTitle = currentPageMap[PyObject.fromJava("title")]?.toString() ?: "My Tasks"

            val tasksObj = currentPageMap[PyObject.fromJava("tasks")]
            val taskList = tasksObj?.asList() ?: emptyList()

            tasksUi = taskList.map { task ->
                val m = task.asMap()

                val timerText = m[PyObject.fromJava("timer_minutes")]?.toString().orEmpty()
                    .let { if (it == "None") "" else it }
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
                    rewardText = rewardText2,
                    scheduledStartText = scheduledText,
                    startedAtText = startedText,
                    completed = completed
                )
            }
        }

        fun setCurrentPage(pageId: String) {
            val pages = core.callAttr("load_pages", pagesPath)
            val pagesMap = pages.asMap()
            pagesMap[PyObject.fromJava("current_page")] = PyObject.fromJava(pageId)
            core.callAttr("save_pages", pagesPath, pages)
            refreshFromPages()
        }

        // ✅ detect "all completed" in current page -> trigger Day Complete screen
        fun checkIfAllCompletedAndGoNext() {
            val list = tasksUi
            if (list.isNotEmpty() && list.all { it.completed }) {
                onDayCompleted()
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

        val activeTaskId = remember(refreshTickValue) { TimerForegroundService.getActiveTaskId(context) }

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

        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = selectedTab.ordinal,
            pageCount = { TaskTab.entries.size }
        )

        LaunchedEffect(pagerState.currentPage) {
            selectedTab = TaskTab.entries[pagerState.currentPage]
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        PremiumBackground {
            Scaffold(
                containerColor = cs.background,
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        pageTitle.ifBlank { "My Tasks" },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Page: $currentPageId",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = cs.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    TextField(
                                        value = currentPageId,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .widthIn(min = 120.dp, max = 170.dp),
                                        singleLine = true,
                                        label = { Text("Page") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        pageIds.forEach { id ->
                                            DropdownMenuItem(
                                                text = { Text(id, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                onClick = {
                                                    expanded = false
                                                    setCurrentPage(id)
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                var menuOpen by remember { mutableStateOf(false) }
                                IconButton(onClick = { menuOpen = true }) { Text("⋮", fontSize = 18.sp) }

                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("+ New Page") },
                                        onClick = { menuOpen = false; showNewPageDialog = true }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Rename Page") },
                                        onClick = {
                                            menuOpen = false
                                            renamePageName = currentPageId
                                            showRenamePageDialog = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Delete Page") },
                                        onClick = { menuOpen = false; showDeletePageDialog = true }
                                    )

                                    // Keep your BackupActionsMenu if you already have it
                                    BackupActionsMenu(onDismiss = { menuOpen = false })
                                }
                            }
                        )

                        TabRow(selectedTabIndex = pagerState.currentPage) {
                            TaskTab.entries.forEachIndexed { index, tab ->
                                val selected = pagerState.currentPage == index
                                Tab(
                                    selected = selected,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    text = {
                                        Text(
                                            tab.title,
                                            maxLines = 1,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (selected) cs.primary else cs.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showAddSheet = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Add task") }
                    )
                }
            ) { innerPadding ->

                // ===== Page dialogs =====
                if (showNewPageDialog) {
                    AlertDialog(
                        onDismissRequest = { showNewPageDialog = false },
                        title = { Text("Create new page") },
                        text = {
                            TextField(
                                value = newPageName,
                                onValueChange = { newPageName = it },
                                label = { Text("Page id (e.g. work, study)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                val id = newPageName.trim()
                                if (id.isBlank()) return@Button
                                val pages = core.callAttr("load_pages", pagesPath)
                                core.callAttr("add_page", pages, id, id)
                                core.callAttr("save_pages", pagesPath, pages)
                                showNewPageDialog = false
                                newPageName = ""
                                setCurrentPage(id)
                            }) { Text("Create") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showNewPageDialog = false }) { Text("Cancel") }
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
                                label = { Text("New page id") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                val newId = renamePageName.trim()
                                if (newId.isBlank()) return@Button
                                val pages = core.callAttr("load_pages", pagesPath)
                                core.callAttr("rename_page", pages, currentPageId, newId)
                                core.callAttr("save_pages", pagesPath, pages)
                                showRenamePageDialog = false
                                setCurrentPage(newId)
                            }) { Text("Rename") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showRenamePageDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showDeletePageDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeletePageDialog = false },
                        title = { Text("Delete page?") },
                        text = { Text("Delete '$currentPageId'? Tasks inside will be removed.") },
                        confirmButton = {
                            Button(onClick = {
                                val pages = core.callAttr("load_pages", pagesPath)
                                core.callAttr("delete_page", pages, currentPageId)
                                core.callAttr("save_pages", pagesPath, pages)
                                showDeletePageDialog = false
                                setCurrentPage("default")
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDeletePageDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                // ===== Swipeable content =====
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
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
                                colors = CardDefaults.cardColors(containerColor = cs.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("No tasks here yet.", fontWeight = FontWeight.SemiBold)
                                    Text("Tap “Add task” to create one.", color = cs.onSurfaceVariant)
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

                                            val pages = core.callAttr("load_pages", pagesPath)
                                            core.callAttr("start_task_current_page", pages, indexInAll)
                                            core.callAttr("save_pages", pagesPath, pages)

                                            NotificationHelper.showReminder(
                                                context = context,
                                                title = "Started ▶",
                                                message = t.name.ifBlank { "Task" }
                                            )

                                            val minutes = t.timerMinutesText.toIntOrNull()
                                            if (minutes != null && minutes > 0) {
                                                TimerForegroundService.startTimer(context, t.id, t.name, minutes)
                                            }

                                            sendPagesUpdated()
                                        }
                                    },

                                    onDone = {
                                        val reqCode = taskIdToRequestCode(t.id)
                                        AlarmScheduler.cancel(context, reqCode)
                                        TimerForegroundService.stopTimer(context)

                                        val pages = core.callAttr("load_pages", pagesPath)
                                        core.callAttr("mark_task_done_current_page", pages, indexInAll)
                                        core.callAttr("save_pages", pagesPath, pages)

                                        ensureNotificationPermissionThen {
                                            NotificationHelper.showReminder(
                                                context = context,
                                                title = "Completed ✅",
                                                message = t.name.ifBlank { "Task" }
                                            )
                                        }

                                        sendPagesUpdated()
                                        // re-check after marking done
                                        refreshFromPages()
                                        checkIfAllCompletedAndGoNext()
                                    },

                                    onDelete = {
                                        val reqCode = taskIdToRequestCode(t.id)
                                        AlarmScheduler.cancel(context, reqCode)
                                        TimerForegroundService.stopTimer(context)

                                        val pages = core.callAttr("load_pages", pagesPath)
                                        core.callAttr("delete_task_current_page", pages, indexInAll)
                                        core.callAttr("save_pages", pagesPath, pages)

                                        sendPagesUpdated()
                                        refreshFromPages()
                                        checkIfAllCompletedAndGoNext()
                                    }
                                )
                            }
                        }
                    }
                }

                // ===== Add Task bottom sheet =====
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
                            item { Text("Create Task", style = MaterialTheme.typography.headlineSmall) }

                            item {
                                TextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Task name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item { Text("Quick timer", style = MaterialTheme.typography.labelLarge) }

                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    AssistChip(onClick = { timerInput = "1" }, label = { Text("1m") })
                                    AssistChip(onClick = { timerInput = "5" }, label = { Text("5m") })
                                    AssistChip(onClick = { timerInput = "10" }, label = { Text("10m") })
                                    AssistChip(onClick = { timerInput = "25" }, label = { Text("25m") })
                                    AssistChip(onClick = { timerInput = "" }, label = { Text("No") })
                                }
                            }

                            item {
                                TextField(
                                    value = timerInput,
                                    onValueChange = { timerInput = it.filter { ch -> ch.isDigit() } },
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

                            item { Text("Schedule (optional)", style = MaterialTheme.typography.labelLarge) }

                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val chosen = Calendar.getInstance()
                                                    chosen.set(Calendar.YEAR, y)
                                                    chosen.set(Calendar.MONTH, m)
                                                    chosen.set(Calendar.DAY_OF_MONTH, d)

                                                    TimePickerDialog(
                                                        context,
                                                        { _, hh, mm ->
                                                            chosen.set(Calendar.HOUR_OF_DAY, hh)
                                                            chosen.set(Calendar.MINUTE, mm)
                                                            chosen.set(Calendar.SECOND, 0)
                                                            chosen.set(Calendar.MILLISECOND, 0)

                                                            scheduledMillis = chosen.timeInMillis
                                                            scheduledLabel = prettyFromMillis(chosen.timeInMillis)
                                                        },
                                                        cal.get(Calendar.HOUR_OF_DAY),
                                                        cal.get(Calendar.MINUTE),
                                                        false
                                                    ).show()
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = Brush.linearGradient(listOf(cs.primary, cs.secondary))
                                        )
                                    ) {
                                        Icon(Icons.Filled.Schedule, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (scheduledLabel.isBlank()) "Pick date & time" else scheduledLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = cs.onSurface
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { scheduledMillis = null; scheduledLabel = "" },
                                        modifier = Modifier.widthIn(min = 88.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = Brush.linearGradient(listOf(cs.primary, cs.secondary))
                                        )
                                    ) {
                                        Text("Clear", maxLines = 1, color = cs.onSurface)
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        val name = nameInput.trim()
                                        if (name.isBlank()) {
                                            Toast.makeText(context, "Enter task name", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val timerMinutes = timerInput.trim().toIntOrNull()
                                        val reward = rewardInput.trim().takeIf { it.isNotBlank() }
                                        val scheduledIso = scheduledMillis?.let { isoFromMillis(it) }

                                        val pages = core.callAttr("load_pages", pagesPath)
                                        val task = core.callAttr("build_task", name, timerMinutes, reward, scheduledIso)
                                        core.callAttr("add_task_to_current_page", pages, task)
                                        core.callAttr("save_pages", pagesPath, pages)

                                        val taskId = task.asMap()[PyObject.fromJava("id")]?.toString().orEmpty()
                                        val reqCode = taskIdToRequestCode(taskId)
                                        val triggerAt = scheduledMillis

                                        if (triggerAt != null) {
                                            ensureNotificationPermissionThen {
                                                val ok = AlarmScheduler.scheduleAllowWhileIdle(
                                                    context = context,
                                                    requestCode = reqCode,
                                                    triggerAtMillis = triggerAt,
                                                    taskId = taskId,
                                                    taskName = name,
                                                    timerMinutes = timerMinutes ?: 0
                                                )
                                                if (!ok) {
                                                    Toast.makeText(
                                                        context,
                                                        "Could not schedule alarm. Please try again.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    NotificationHelper.showReminder(
                                                        context = context,
                                                        title = "Scheduled ⏰",
                                                        message = name
                                                    )
                                                }
                                            }
                                        }

                                        nameInput = ""
                                        timerInput = ""
                                        rewardInput = ""
                                        scheduledMillis = null
                                        scheduledLabel = ""
                                        showAddSheet = false

                                        sendPagesUpdated()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Create")
                                }
                            }

                            item { Spacer(Modifier.height(6.dp)) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskCardPremium(
        task: TaskUi,
        running: Boolean,
        scheduled: Boolean,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onDelete: () -> Unit
    ) {
        val cs = MaterialTheme.colorScheme

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    if (running) AssistChip(onClick = {}, label = { Text("Running") })
                    if (task.completed) AssistChip(onClick = {}, label = { Text("Completed") })
                    if (scheduled) AssistChip(onClick = {}, label = { Text("Scheduled") })
                }

                val meta = buildString {
                    if (task.scheduledStartText.isNotBlank()) append("Scheduled: ${task.scheduledStartText}")
                    if (task.timerMinutesText.isNotBlank()) {
                        if (isNotEmpty()) append("  •  ")
                        append("${task.timerMinutesText} min")
                    }
                    if (task.rewardText.isNotBlank()) {
                        if (isNotEmpty()) append("  •  ")
                        append("Reward: ${task.rewardText}")
                    }
                }
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 360.dp

                    val outlineBorder = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(cs.primary, cs.secondary))
                    )

                    if (compact) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onStart,
                                enabled = !task.completed,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Start", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = onDone,
                                enabled = !task.completed,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
                                border = outlineBorder
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = cs.onSurface)
                                Spacer(Modifier.width(6.dp))
                                Text("Done", maxLines = 1, color = cs.onSurface)
                            }

                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                                border = outlineBorder
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = cs.error)
                                Spacer(Modifier.width(6.dp))
                                Text("Delete", maxLines = 1, color = cs.error)
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onStart,
                                enabled = !task.completed,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Start")
                            }

                            OutlinedButton(
                                onClick = onDone,
                                enabled = !task.completed,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
                                border = outlineBorder
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = cs.onSurface)
                                Spacer(Modifier.width(6.dp))
                                Text("Done", color = cs.onSurface)
                            }

                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                                border = outlineBorder
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = cs.error)
                                Spacer(Modifier.width(6.dp))
                                Text("Delete", color = cs.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
