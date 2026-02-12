package com.venkatesh.proguin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModeSelectScreen(on74Days: () -> Unit,
                     onInfinite: () -> Unit) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Choose Your Path")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = on74Days) {
            Text("74 Days • 74 KMs")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onInfinite) {
            Text("Infinite Tasks")
        }
    }
}
