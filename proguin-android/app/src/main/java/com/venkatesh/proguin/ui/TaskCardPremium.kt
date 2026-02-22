package com.venkatesh.proguin.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.TaskUi
import com.venkatesh.proguin.ui.theme.SL_Deep
import com.venkatesh.proguin.ui.theme.SL_Surface
import com.venkatesh.proguin.ui.theme.SL_NeonBlue
import com.venkatesh.proguin.ui.theme.SL_NeonCyan
import com.venkatesh.proguin.ui.theme.SL_NeonViolet

@Composable
fun TaskCardPremium(
    task: TaskUi,
    running: Boolean,
    scheduled: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    highlight: Boolean = false,
    showSchedule: Boolean = false,
    onSchedule: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme

    // --- “Crystal pop” entry animation ---
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(task.id) { appeared = true }

    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.965f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "card_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "card_alpha"
    )

    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val outerShape = RoundedCornerShape(20.dp)
    val borderBrush = Brush.linearGradient(
        listOf(
            SL_NeonBlue.copy(alpha = if (highlight) 0.95f else glow),
            SL_NeonViolet.copy(alpha = if (highlight) 0.90f else glow),
            SL_NeonCyan.copy(alpha = if (highlight) 0.85f else glow)
        )
    )

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            SL_Deep.copy(alpha = 0.82f),
            SL_Surface.copy(alpha = 0.92f),
            SL_Deep.copy(alpha = 0.78f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            }
            .shadow(
                elevation = if (highlight) 22.dp else 14.dp,
                shape = outerShape,
                ambientColor = SL_NeonCyan.copy(alpha = 0.22f),
                spotColor = SL_NeonViolet.copy(alpha = 0.22f)
            )
            .clip(outerShape)
            .background(glassBrush)
            .border(1.dp, borderBrush, outerShape)
    ) {

        // Subtle top “crystal highlight” bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            SL_NeonCyan.copy(alpha = 0.75f),
                            SL_NeonViolet.copy(alpha = 0.60f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = task.name.ifBlank { "Task" },
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val statusText = when {
                task.completed -> "Completed"
                running -> "Running"
                scheduled -> "Scheduled"
                else -> "Idle"
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text(statusText) })

                if (task.recurrenceText.isNotBlank() && !task.completed) {
                    AssistChip(onClick = { }, label = { Text("Repeat") })
                }

                if (task.tagsText.isNotBlank()) {
                    AssistChip(onClick = { }, label = { Text("Tags") })
                }
            }

            if (task.rewardText.isNotBlank() || task.timerMinutes > 0 || task.scheduledStartText.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

                    if (task.timerMinutes > 0) {
                        Text(
                            text = "${task.timerMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant
                        )
                    }

                    if (task.rewardText.isNotBlank()) {
                        Text(
                            text = "Reward: ${task.rewardText}",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (task.scheduledStartText.isNotBlank() && !task.completed) {
                        Text(
                            text = "Scheduled: ${task.scheduledStartText}",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (task.tagsText.isNotBlank()) {
                Text(
                    text = "Tags: ${task.tagsText}",
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.subtasksSummary.isNotBlank()) {
                Text(
                    text = task.subtasksSummary,
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (task.notePreview.isNotBlank()) {
                Text(
                    text = "Note: ${task.notePreview}",
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStart,
                    enabled = !task.completed,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Start",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDone,
                    enabled = !task.completed,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Done",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Delete",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ✅ Schedule button only for Journey (passed from MainActivity)
            if (showSchedule && !task.completed) {
                OutlinedButton(
                    onClick = onSchedule,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Schedule", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Bottom inner glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            SL_NeonBlue.copy(alpha = 0.10f),
                            SL_NeonViolet.copy(alpha = 0.14f)
                        )
                    )
                )
        )
    }
}
