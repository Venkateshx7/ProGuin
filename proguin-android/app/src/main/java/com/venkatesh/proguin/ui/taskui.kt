package com.venkatesh.proguin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProTaskCard(
    title: String,
    metaLine: String,
    status: String,
    isPrimary: Boolean,
    completed: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val container = cs.surface.copy(alpha = 0.94f)
    val border = cs.outlineVariant.copy(alpha = 0.60f)

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = cs.onSurface
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(status, fontWeight = FontWeight.SemiBold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = cs.surfaceVariant.copy(alpha = 0.85f)
                    ),
                    border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.55f))
                )
            }

            if (metaLine.isNotBlank()) {
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ✅ Premium Bright Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = onStart,
                    enabled = !completed,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D2FF),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFFB0BEC5),
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = onDone,
                    enabled = !completed,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBDBDBD),
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD50000),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
