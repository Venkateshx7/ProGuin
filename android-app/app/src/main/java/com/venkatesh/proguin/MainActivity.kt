package com.venkatesh.proguin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val timerMinutesText: String,   // "" if none
    val rewardText: String,         // "" if none
    val scheduledStartText: String, // "" if none (ISO string)
    val startedAtText: String,      // "" if none
    val completed: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

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

                    "tasks" -> MainScreen()

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

    @Composable
    private fun MainScreen() {
        val context = LocalContext.current

        val py = remember { Python.getInstance() }
        val core = remember { py.getModule("proguin.core") }
        val pagesPath = remember { File(context.filesDir, "pages.json").absolutePath }

        // UI state
        var pageTitle by remember { mutableStateOf("My Tasks") }
        var currentPageId by remember { mutableStateOf("default") }
        var tasksUi by remember { mutableStateOf(listOf<TaskUi>()) }
        var pageIds by remember { mutableStateOf(listOf<String>()) }

        // dialogs / input
        var showNewPageDialog by remember { mutableStateOf(false) }
        var showRenamePageDialog by remember { mutableStateOf(false) }
        var showDeletePageDialog by remember { mutableStateOf(false) }

        var newPageName by remember { mutableStateOf("") }
        var renamePageName by remember { mutableStateOf("") }

        var nameInput by remember { mutableStateOf("") }
        var timerInput by remember { mutableStateOf("") }
        var rewardInput by remember { mutableStateOf("") }

        // V2: schedule picker state
        var scheduledMillis by remember { mutableStateOf<Long?>(null) }
        var scheduledLabel by remember { mutableStateOf("") } // user friendly display

        fun taskIdToRequestCode(taskId: String): Int {
            // stable-ish positive int
            return abs(taskId.hashCode()).coerceAtLeast(1)
        }

        fun isoFromMillis(ms: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            return sdf.format(ms)
        }

        fun prettyFromMillis(ms: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
            return sdf.format(ms)
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
                val taskMap = task.asMap()

                val id = taskMap[PyObject.fromJava("id")]?.toString().orEmpty()
                val name = taskMap[PyObject.fromJava("name")]?.toString().orEmpty()

                val timerObj = taskMap[PyObject.fromJava("timer_minutes")]
                val timerText = timerObj?.toString().orEmpty().let { if (it == "None") "" else it }

                val rewardObj = taskMap[PyObject.fromJava("reward")]
                val rewardText = rewardObj?.toString().orEmpty().let { if (it == "None") "" else it }

                val startObj = taskMap[PyObject.fromJava("started_at")]
                val startedText = startObj?.toString().orEmpty().let { if (it == "None") "" else it }

                val scheduledObj = taskMap[PyObject.fromJava("scheduled_start")]
                val scheduledText = scheduledObj?.toString().orEmpty().let { if (it == "None") "" else it }

                val completedObj = taskMap[PyObject.fromJava("completed")]
                val completed = completedObj?.toString().orEmpty().equals("True", ignoreCase = true)

                TaskUi(
                    id = id,
                    name = name,
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

        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = pageTitle, style = MaterialTheme.typography.headlineSmall)

                // ===== Page dropdown =====
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = currentPageId,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Page") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        pageIds.forEach { pageId ->
                            DropdownMenuItem(
                                text = { Text(pageId) },
                                onClick = {
                                    expanded = false
                                    setCurrentPage(pageId)
                                }
                            )
                        }
                    }
                }

                // ===== Page actions row =====
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showNewPageDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("+ New") }

                    OutlinedButton(
                        onClick = {
                            renamePageName = currentPageId
                            showRenamePageDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Rename") }

                    OutlinedButton(
                        onClick = { showDeletePageDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Delete") }
                }

                // ===== New Page Dialog =====
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
                                if (id.isBlank()) {
                                    Toast.makeText(context, "Enter page name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
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

                // ===== Rename Page Dialog =====
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
                                if (newId.isBlank()) {
                                    Toast.makeText(context, "Enter new id", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
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

                // ===== Delete Page Dialog =====
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
                                refreshFromPages()
                                // go back to default safely
                                setCurrentPage("default")
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDeletePageDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                // ===== Schedule picker row (V2) =====
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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

                                    // after date -> time picker
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
                    ) { Text(if (scheduledLabel.isBlank()) "Pick schedule" else scheduledLabel) }

                    OutlinedButton(
                        onClick = {
                            scheduledMillis = null
                            scheduledLabel = ""
                        },
                        modifier = Modifier.width(90.dp)
                    ) { Text("Clear") }
                }

                // ===== Inputs =====
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Task name") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = timerInput,
                    onValueChange = { timerInput = it },
                    label = { Text("Timer minutes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = rewardInput,
                    onValueChange = { rewardInput = it },
                    label = { Text("Reward (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // ===== Add Task =====
                Button(
                    onClick = {
                        val name = nameInput.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Enter task name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val timerMinutes: Int? = timerInput.trim().toIntOrNull()
                        val reward: String? = rewardInput.trim().takeIf { it.isNotBlank() }

                        val scheduledIso: String? = scheduledMillis?.let { isoFromMillis(it) }

                        val pages = core.callAttr("load_pages", pagesPath)
                        val task = core.callAttr("build_task", name, timerMinutes, reward, scheduledIso)

                        // add task to current page
                        core.callAttr("add_task_to_current_page", pages, task)
                        core.callAttr("save_pages", pagesPath, pages)

                        // schedule alarm if schedule picked
                        val taskMap = task.asMap()
                        val taskId = taskMap[PyObject.fromJava("id")]?.toString().orEmpty()
                        val reqCode = taskIdToRequestCode(taskId)

                        if (scheduledMillis != null) {
                            AlarmScheduler.scheduleExact(
                                context = context,
                                requestCode = reqCode,
                                triggerAtMillis = scheduledMillis!!,
                                taskId = taskId,
                                taskName = name,
                                timerMinutes = timerMinutes ?: 0
                            )
                        }

                        Toast.makeText(context, "Saved ✅", Toast.LENGTH_SHORT).show()

                        nameInput = ""
                        timerInput = ""
                        rewardInput = ""
                        scheduledMillis = null
                        scheduledLabel = ""

                        refreshFromPages()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add Task") }

                // ===== Task list =====
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(tasksUi) { index, t ->
                        TaskCard(
                            task = t,
                            onStart = {
                                val pages = core.callAttr("load_pages", pagesPath)

                                // mark started in python
                                core.callAttr("start_task_current_page", pages, index)
                                core.callAttr("save_pages", pagesPath, pages)

                                val minutes = t.timerMinutesText.toIntOrNull()

                                // show reminder now
                                val startedText = buildString {
                                    append("Started: ${t.name}")
                                    if (minutes != null) append(" • ${minutes} min")
                                    if (t.rewardText.isNotBlank()) append(" • Reward: ${t.rewardText}")
                                }
                                NotificationHelper.showReminder(context, "ProGuin", startedText)

                                // V2: start foreground timer if minutes exists
                                if (minutes != null && minutes > 0) {
                                    TimerForegroundService.startTimer(
                                        context = context,
                                        taskId = t.id,
                                        taskName = t.name,
                                        minutes = minutes
                                    )
                                }

                                refreshFromPages()
                            },
                            onDelete = {
                                // cancel alarm + stop timer service FIRST
                                val reqCode = taskIdToRequestCode(t.id)
                                AlarmScheduler.cancelExact(context, reqCode)
                                TimerForegroundService.stopTimer(context)

                                // delete in python
                                val pages = core.callAttr("load_pages", pagesPath)
                                core.callAttr("delete_task_current_page", pages, index)
                                core.callAttr("save_pages", pagesPath, pages)

                                refreshFromPages()
                            },
                            onDone = {
                                // cancel alarm + stop timer service FIRST
                                val reqCode = taskIdToRequestCode(t.id)
                                AlarmScheduler.cancelExact(context, reqCode)
                                TimerForegroundService.stopTimer(context)

                                // mark done in python
                                val pages = core.callAttr("load_pages", pagesPath)
                                core.callAttr("mark_task_done_current_page", pages, index)
                                core.callAttr("save_pages", pagesPath, pages)

                                refreshFromPages()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskCard(
        task: TaskUi,
        onStart: () -> Unit,
        onDelete: () -> Unit,
        onDone: () -> Unit,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(task.name, style = MaterialTheme.typography.titleMedium)

                if (!task.completed && task.startedAtText.isNotBlank()) {
                    Text("Running ✅")
                }

                if (task.completed) {
                    Text("Completed ✅")
                }

                if (
                    task.timerMinutesText.isNotBlank() ||
                    task.rewardText.isNotBlank() ||
                    task.scheduledStartText.isNotBlank()
                ) {
                    val meta = buildString {
                        if (task.scheduledStartText.isNotBlank()) append("Scheduled: ${task.scheduledStartText}")
                        if (isNotEmpty()) append("  •  ")
                        if (task.timerMinutesText.isNotBlank()) append("${task.timerMinutesText} min")
                        if (task.rewardText.isNotBlank()) {
                            if (isNotEmpty()) append("  •  ")
                            append("Reward: ${task.rewardText}")
                        }
                    }
                    Text(meta, style = MaterialTheme.typography.bodyMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onStart) { Text("Start") }
                    OutlinedButton(onClick = onDelete) { Text("Delete") }
                    OutlinedButton(onClick = onDone) { Text("Done") }
                }
            }
        }
    }
}
