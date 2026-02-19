package com.venkatesh.proguin.ui.infinite

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InfiniteHubScreen(
    onOpenInfiniteTasks: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        cs.background,
                        cs.background.copy(alpha = 0.92f),
                        cs.background
                    )
                )
            )
            .padding(18.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← Back") }
                Text("Infinite Mode", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(40.dp))
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.92f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Endless Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Add tasks anytime, no day limits. Perfect for your daily routine + quick wins.",
                        color = cs.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = onOpenInfiniteTasks,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open Infinite Tasks", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go Back")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "Tip: Use Infinite for habits + general tasks.\nUse Journey for 74-day structured plan.",
                color = cs.onSurfaceVariant
            )
        }
    }
}
