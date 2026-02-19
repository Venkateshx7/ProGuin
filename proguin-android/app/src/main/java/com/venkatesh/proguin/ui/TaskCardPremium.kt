package com.venkatesh.proguin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.TaskUi

@Composable
fun TaskCardPremium(
    task: TaskUi,
    running: Boolean,
    scheduled: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    showSchedule: Boolean = false,
    onSchedule: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme

    val cardColor = cs.surface.copy(alpha = 0.90f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        color = cardColor
    ) {
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

            AssistChip(
                onClick = { },
                label = { Text(statusText) }
            )

            if (task.timerMinutes > 0) {
                Text(
                    text = "${task.timerMinutes} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                        contentColor = androidx.compose.ui.graphics.Color(0xFF0B0F1A)
                    ),
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFF4FD8),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFD32F2F),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
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
    }
}
