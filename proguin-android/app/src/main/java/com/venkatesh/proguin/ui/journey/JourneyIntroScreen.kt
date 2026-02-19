@file:OptIn(ExperimentalMaterial3Api::class)

package com.venkatesh.proguin.ui.journey

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JourneyIntroScreen(
    onBack: () -> Unit,
    onStartJourney: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("74-Day Journey") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("What this Journey does", fontWeight = FontWeight.Bold)
                    Text("• Builds discipline with small daily wins")
                    Text("• Makes focus automatic over time")
                    Text("• Creates a streak mindset (consistency > motivation)")

                    HorizontalDivider()

                    Text("Share your progress (optional)", fontWeight = FontWeight.Bold)
                    Text("Post daily: “Day X ✅ — I kept my streak!”\nIt motivates others and keeps you accountable.")
                }
            }

            Button(
                onClick = onStartJourney,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Start Journey", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back")
            }
        }
    }
}
