package com.venkatesh.proguin.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.venkatesh.proguin.R

@Composable
fun TasksScreenBackground(
    currentPageId: String,
    content: @Composable BoxScope.() -> Unit
) {

    val bgRes = when {

        // Infinite page
        currentPageId == "infinite" ->
            R.drawable.penguin_bg

        // Journey Day pages
        currentPageId.startsWith("journey_day_") -> {

            val day = currentPageId
                .removePrefix("journey_day_")
                .toIntOrNull() ?: 1

            when (day) {
                1 -> R.drawable.bg_arc1
                2 -> R.drawable.bg_arc2
                3 -> R.drawable.bg_arc3
                4 -> R.drawable.bg_arc4
                5 -> R.drawable.bg_arc5
                6 -> R.drawable.bg_arc6
                7 -> R.drawable.bg_arc7
                8 -> R.drawable.bg_arc8
                9 -> R.drawable.bg_arc9
                10 -> R.drawable.bg_arc10
                else -> R.drawable.bg_arc11
            }
        }

        else -> R.drawable.bg_arc1
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(bgRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ✅ Solo-leveling vibe overlay (does NOT change the image assets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SL_Black.copy(alpha = 0.72f),
                            SL_Deep.copy(alpha = 0.66f),
                            SL_Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        content()
    }
}
