@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.R
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
                        Text(
                            "74-Day Penguin Journey",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Unlocked Day: ${currentDay.intValue}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = MaterialTheme.colorScheme.onSurface)
                    }
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
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { inner ->

        // ✅ FULL BACKGROUND IMAGE + SCRIM
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {

            Image(
                painter = painterResource(R.drawable.bg_arc1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "ARC ${plan.arc}: ${plan.arcTitle}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            plan.story,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Divider()
                        Text(
                            "“${plan.quote}”",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (!store.isUnlocked(selectedDay)) {
                                Toast.makeText(
                                    context,
                                    "Locked 🔒 Complete earlier days first.",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Today")
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Tasks Preview",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        plan.tasks.forEach {
                            Text(
                                "• ${it.name}  (${it.minutes}m)",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    "Select a Day",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ),
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
                                    Text(
                                        "Day $d",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
}
