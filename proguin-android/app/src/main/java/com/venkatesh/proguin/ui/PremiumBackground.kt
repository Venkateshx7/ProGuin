package com.venkatesh.proguin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.venkatesh.proguin.R
import androidx.compose.material3.MaterialTheme

@Composable
fun PremiumBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxSize()) {

        // Your premium background image
        Image(
            painter = painterResource(id = R.drawable.penguin_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Strong readability scrim (THIS fixes “wordings not visible”)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.background.copy(alpha = 0.78f),
                            cs.background.copy(alpha = 0.62f),
                            cs.background.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        content()
    }
}
