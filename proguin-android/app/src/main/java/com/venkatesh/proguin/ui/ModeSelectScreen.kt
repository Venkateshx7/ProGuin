package com.venkatesh.proguin.ui

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
fun ModeSelectScreen(
    on74Days: () -> Unit,
    onInfinite: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val bgTop = Color(0xFFBFE9FF)      // brighter sky
    val bgMid = Color(0xFFF4F7FF)      // soft white
    val bgBottom = Color(0xFFE9C7FF)   // stronger pink/purple

    val cardColor = Color(0xFFFFFFFF).copy(alpha = 0.92f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        bgTop,
                        bgMid,
                        bgBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    "Choose your path",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )

                Text(
                    "Pick a mode. You can always switch later.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF374151)
                )

                // ✅ Primary (already visible)
                Button(
                    onClick = onInfinite,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D5FF),
                        contentColor = Color(0xFF001018)
                    )
                ) {
                    Text("Infinite Tasks", fontWeight = FontWeight.SemiBold)
                }

                // ✅ Make 74 days visible like primary (filled)
                Button(
                    onClick = on74Days,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),  // strong purple
                        contentColor = Color.White
                    )
                ) {
                    Text("74 Days • 74 KMs", fontWeight = FontWeight.SemiBold)
                }

                // ✅ Back should also be clearly visible (tonal filled)
                FilledTonalButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFE5E7EB),
                        contentColor = Color(0xFF111827)
                    )
                ) {
                    Text("Back", fontWeight = FontWeight.SemiBold)
                }

                Text(
                    "Tip: Keep tasks small. Win daily.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF374151)
                )
            }
        }
    }
}

