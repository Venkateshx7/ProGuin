package com.venkatesh.proguin.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.venkatesh.proguin.ui.theme.SL_Black
import com.venkatesh.proguin.ui.theme.SL_NeonBlue
import com.venkatesh.proguin.ui.theme.SL_NeonCyan
import com.venkatesh.proguin.ui.theme.SL_NeonViolet
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Lightweight “crystal/portal” reveal overlay (Solo-level vibe).
 * Triggers when [key] changes.
 */
@Composable
fun CrystalRevealOverlay(
    key: Any?,
    modifier: Modifier = Modifier,
    durationMs: Int = 520
) {
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(key) {
        show = true
        delay((durationMs * 0.85f).toLong())
        show = false
    }

    val progress by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "crystal_reveal"
    )

    if (progress <= 0.001f) return

    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.50f, h * 0.32f)
            val maxR = max(w, h) * 1.10f
            val r = (0.10f + 0.90f * progress) * maxR

            // Dark veil + neon aura
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        SL_Black.copy(alpha = 0.35f * progress),
                        SL_Black.copy(alpha = 0.55f * progress)
                    ),
                    center = center,
                    radius = r
                )
            )

            // Crystal ring
            val ringAlpha = 0.55f * progress
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SL_NeonCyan.copy(alpha = ringAlpha),
                        SL_NeonViolet.copy(alpha = ringAlpha * 0.75f),
                        SL_NeonBlue.copy(alpha = ringAlpha * 0.55f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                center = center,
                radius = r,
                style = Stroke(width = with(density) { 2.0.dp.toPx() })
            )

            // Inner shards
            val shardAlpha = 0.35f * progress
            val strokes = listOf(SL_NeonBlue, SL_NeonViolet, SL_NeonCyan)
            val shardCount = 7
            for (i in 0 until shardCount) {
                val t = (i + 1) / (shardCount + 1f)
                val x0 = w * (0.10f + 0.15f * t)
                val y0 = h * (0.10f + 0.08f * t)
                val x1 = w * (0.90f - 0.18f * t)
                val y1 = h * (0.55f + 0.10f * t)
                drawLine(
                    color = strokes[i % strokes.size].copy(alpha = shardAlpha),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = with(density) { 1.2.dp.toPx() }
                )
            }
        }
    }
}
