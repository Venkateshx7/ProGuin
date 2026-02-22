package com.venkatesh.proguin.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.ui.theme.NeonCyan

@Composable
fun SoloQuestPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "alpha"
    )

    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .scale(scale)
            .alpha(a)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.85f),
                        Color(0xFF6FE7FF).copy(alpha = 0.35f),
                        NeonCyan.copy(alpha = 0.85f),
                    )
                ),
                shape = shape
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A1326).copy(alpha = 0.92f),
                        Color(0xFF0B1020).copy(alpha = 0.92f),
                        Color(0xFF050811).copy(alpha = 0.92f),
                    )
                ),
                shape
            )
            .padding(14.dp)
    ) {
        // HEADER like "QUEST INFO"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEAF6FF)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "!",
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(14.dp))

        content()
    }
}

