@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.journey.journeyContent
import com.venkatesh.proguin.journey.journeyProgressStore

@Composable
fun JourneyScreen(
    onBack: () -> Unit,
    onGenerateDayPlan: (Int) -> Unit
) {
    val context = LocalContext.current
    val store = remember { journeyProgressStore(context) }

    val currentDay = remember { mutableIntStateOf(store.currentDay()) }
    val completed = remember { mutableStateOf(store.completedDays()) }

    var selectedDay by remember { mutableIntStateOf(currentDay.intValue) }

    val plan = remember(selectedDay) { journeyContent.getDayPlan(selectedDay) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("74-Day Penguin Journey", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Unlocked Day: ${currentDay.intValue}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            store.resetAll()
                            currentDay.intValue = store.currentDay()
                            completed.value = store.completedDays()
                            selectedDay = currentDay.intValue
                            Toast.makeText(context, "Journey reset ✅", Toast.LENGTH_SHORT).show()
                        }
                    ) { Text("Reset") }
                }
            )
        }
    ) { inner ->

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ARC ${plan.arc}: ${plan.arcTitle}", fontWeight = FontWeight.SemiBold)
                    Text(plan.story, style = MaterialTheme.typography.bodyMedium)
                    Divider()
                    Text("“${plan.quote}”", fontWeight = FontWeight.Medium)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (!store.isUnlocked(selectedDay)) {
                            Toast.makeText(context, "Locked 🔒 Complete earlier days first.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        onGenerateDayPlan(selectedDay)

                        store.markDayCompleted(selectedDay)
                        currentDay.intValue = store.currentDay()
                        completed.value = store.completedDays()

                        Toast.makeText(context, "Day $selectedDay plan added ✅", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add Day Plan to Tasks")
                }

                OutlinedButton(
                    onClick = { selectedDay = currentDay.intValue },
                    modifier = Modifier.widthIn(min = 90.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Today")
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tasks Preview", fontWeight = FontWeight.SemiBold)
                    plan.tasks.forEach {
                        Text("• ${it.name}  (${it.minutes}m)")
                    }
                }
            }

            Text("Select a Day", fontWeight = FontWeight.SemiBold)

            val days = (1..74).toList()

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(days) { d ->
                    val unlocked = store.isUnlocked(d)
                    val done = completed.value.contains(d)

                    val label = when {
                        done -> "✅ Completed"
                        unlocked -> "Unlocked"
                        else -> "Locked"
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = unlocked) { selectedDay = d }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Day $d", fontWeight = FontWeight.SemiBold)
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(
                                when {
                                    !unlocked -> "🔒"
                                    selectedDay == d -> "➡️"
                                    else -> ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
