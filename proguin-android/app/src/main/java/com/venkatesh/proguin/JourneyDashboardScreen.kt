package com.venkatesh.proguin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.journey.journeyProgressStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyDashboardScreen(
    onBack: () -> Unit
) {

    // ✅ FIX: system back should follow app navigation, not close activity
    BackHandler { onBack() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { journeyProgressStore(context) }

    val completed = store.completedDays()
    val currentDay = store.currentDay()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journey Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { inner ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Card(
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Current Day", fontWeight = FontWeight.Bold)
                        Text("Day $currentDay / 74")
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Days Completed", fontWeight = FontWeight.Bold)
                        Text("${completed.size} / 74")

                        LinearProgressIndicator(
                            progress = completed.size / 74f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}