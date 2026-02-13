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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.venkatesh.proguin.alarm.AlarmScheduler
import com.venkatesh.proguin.alarm.NotificationHelper
import com.venkatesh.proguin.alarm.TimerForegroundService
import com.venkatesh.proguin.ui.ModeSelectScreen
import com.venkatesh.proguin.ui.WelcomeScreen
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
    val rewardText: String,
    val scheduledStartText: String,
    val startedAtText: String,
    val completed: Boolean
)

private enum class TaskTab(val title: String) {
    ALL("All"),
    RUNNING("Running"),
    SCHEDULED("Scheduled"),
    COMPLETED("Completed")
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val refreshTick = mutableIntStateOf(0)

    private val pagesUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshTick.intValue++
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
                        onInfinite = { page = "tasks" }
                    )

                    "tasks" -> TasksPremiumScreen(refreshTickValue = refreshTick.intValue)

                    "journey" -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("74 Days Journey (Coming Soon)")
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { page = "mode" }) { Text("Back") }
                        }
                    }
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
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(pagesUpdatedReceiver)
        } catch (_: Exception) { }
    }

    @Composable
    private fun TasksPremiumScreen(refreshTickValue: Int) {
        val context = LocalContext.current

        // Back polish
        var lastBack by remember { mutableStateOf(0L) }
        BackHandler {
            val now = System.currentTimeMillis()
            if (now - lastBack < 2000) {
                (context as? ComponentActivity)?.finish()
            } else {
                lastBack = now
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }

        // Notification permission (Android 13+)
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

            if (granted) {
                action()
            } else {
                if (pendingNotifAction != null) return
                pendingNotifAction = action
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val py = remember { Python.getInstance() }
        val core = remember { py.getModule("proguin.core") }
        val pagesPath = remember { File(context.filesDir, "pages.json").absolutePath }

        // Data state
        var pageTitle by remember { mutableStateOf("My Tasks") }
        var currentPageId by remember { mutableStateOf("default") }
        var tasksUi by remember { mutableStateOf(listOf<TaskUi>()) }
        var pageIds by remember { mutableStateOf(listOf<String>()) }

        // UI state
        var selectedTab by remember { mutableStateOf(TaskTab.ALL) }

        // Add sheet state
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
            pageIds = pagesContainer.keys.map { it.toString().replace("'", "") }

            val currentPageObj = pagesContainer[PyObject.fromJava(cpId)]
            val currentPageMap = currentPageObj?.asMap() ?: emptyMap()

            pageTitle = currentPageMap[PyObject.fromJava("title")]?.toString() ?: "My Tasks"

            val tasksObj = currentPageMap[PyObject.fromJava("tasks")]
            val taskList = tasksObj?.asList() ?: emptyList()

            tasksUi = taskList.map { task ->
                val m = task.asMap()

                val timerText = m[PyObject.fromJava("timer_minutes")]?.toString().orEmpty()
                    .let { if (it == "None") "" else it }
                val rewardText = m[PyObject.fromJava("reward")]?.toString().orEmpty()
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
                    rewardText = rewardText,
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

        LaunchedEffect(Unit) {
            NotificationHelper.ensureChannels(context)
            refreshFromPages()
        }

        LaunchedEffect(refreshTickValue) {
            refreshFromPages()
        }

        // Running state (from service prefs)
        val activeTaskId = remember(refreshTickValue) { TimerForegroundService.getActiveTaskId(context) }

        fun isRunning(t: TaskUi): Boolean {
            return activeTaskId.isNotBlank() && activeTaskId == t.id && !t.completed
        }

        fun isScheduled(t: TaskUi): Boolean {
            return t.scheduledStartText.isNotBlank() && !t.completed && !isRunning(t)
        }

        val visibleTasks = remember(tasksUi, selectedTab, activeTaskId) {
            tasksUi.filter { t ->
                when (selectedTab) {
                    TaskTab.ALL -> true
                    TaskTab.RUNNING -> isRunning(t)
                    TaskTab.SCHEDULED -> isScheduled(t)
                    TaskTab.COMPLETED -> t.completed
                }
            }
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    pageTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Page: $currentPageId",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        actions = {
                            // Page dropdown
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
                                        .width(160.dp),
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
                                            text = { Text(id) },
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
                            IconButton(onClick = { menuOpen = true }) {
                                Text("⋮")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("+ New Page") },
                                    onClick = { menuOpen = false; showNewPageDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Page") },
                                    onClick = { menuOpen = false; renamePageName = currentPageId; showRenamePageDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Page") },
                                    onClick = { menuOpen = false; showDeletePageDialog = true }
                                )
                            }
                        }
                    )

                    // Tabs
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        TaskTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTab.ordinal == index,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) }
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

            // ===== Content =====
            if (visibleTasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "No tasks here",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Tap “Add task” to create your first task. Use tabs to filter Running / Scheduled / Completed.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { showAddSheet = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add task")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(visibleTasks) { indexInVisible, t ->
                        // Need original index for python operations
                        val indexInAll = tasksUi.indexOfFirst { it.id == t.id }.coerceAtLeast(indexInVisible)

                        TaskCardPremium(
                            task = t,
                            running = isRunning(t),
                            scheduled = isScheduled(t),
                            onStart = {
                                ensureNotificationPermissionThen {
                                    val pages = core.callAttr("load_pages", pagesPath)
                                    core.callAttr("start_task_current_page", pages, indexInAll)
                                    core.callAttr("save_pages", pagesPath, pages)

                                    NotificationHelper.showReminder(context, "ProGuin", "Started: ${t.name}")

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

                                sendPagesUpdated()
                            },
                            onDelete = {
                                val reqCode = taskIdToRequestCode(t.id)
                                AlarmScheduler.cancel(context, reqCode)
                                TimerForegroundService.stopTimer(context)

                                val pages = core.callAttr("load_pages", pagesPath)
                                core.callAttr("delete_task_current_page", pages, indexInAll)
                                core.callAttr("save_pages", pagesPath, pages)

                                sendPagesUpdated()
                            }
                        )
                    }
                }
            }

            // ===== Add Task bottom sheet =====
            if (showAddSheet) {
                ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Create Task", style = MaterialTheme.typography.headlineSmall)

                        TextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Task name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Quick timer", style = MaterialTheme.typography.labelLarge)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AssistChip(onClick = { timerInput = "1" }, label = { Text("1m") })
                            AssistChip(onClick = { timerInput = "5" }, label = { Text("5m") })
                            AssistChip(onClick = { timerInput = "10" }, label = { Text("10m") })
                            AssistChip(onClick = { timerInput = "25" }, label = { Text("25m") })
                            AssistChip(onClick = { timerInput = "" }, label = { Text("No") })
                        }

                        TextField(
                            value = timerInput,
                            onValueChange = { timerInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Timer minutes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        TextField(
                            value = rewardInput,
                            onValueChange = { rewardInput = it },
                            label = { Text("Reward (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Schedule (optional)", style = MaterialTheme.typography.labelLarge)

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
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (scheduledLabel.isBlank()) "Pick date & time" else scheduledLabel)
                            }

                            OutlinedButton(
                                onClick = { scheduledMillis = null; scheduledLabel = "" },
                                modifier = Modifier.width(110.dp)
                            ) {
                                Text("Clear")
                            }
                        }

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

                                // Ask notification permission when user schedules a task (your requirement)
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

                        Spacer(Modifier.height(10.dp))
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(task.name, style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(meta, style = MaterialTheme.typography.bodyMedium)
                }

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
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Done")
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}
