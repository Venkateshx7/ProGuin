package com.venkatesh.proguin.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.venkatesh.proguin.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {

        // 1) Background image
        Image(
            painter = painterResource(id = R.drawable.penguin_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2) Dark overlay (makes text readable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
        )

        // 3) Premium "glass" card + content
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(22.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to ProGuin",
                    style = MaterialTheme.typography.headlineMedium,
                    color = androidx.compose.ui.graphics.Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Build discipline. Track focus. Win the day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f)
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "STAY HARD 💪", Toast.LENGTH_SHORT).show()
                        onStart()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = "Start Your Focus Journey",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
