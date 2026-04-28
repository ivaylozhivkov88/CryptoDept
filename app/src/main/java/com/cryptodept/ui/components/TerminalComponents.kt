// STEP 17: Common Terminal UI Components
// Created: 2024-05-22
// Style: Wall Street 90s (Terminal)

package com.cryptodept.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.*

@Composable
fun TerminalErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .border(2.dp, WallStreetRed, RectangleShape)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "!!! SYSTEM FAILURE !!!",
                        color = WallStreetRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "ERROR_LOG: $message",
                        color = WallStreetRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "[ PRESS TO RETRY ]",
                        color = WallStreetGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onRetry() }
                            .border(1.dp, WallStreetGreen, RectangleShape)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalLoadingScreen(message: String = "LOADING...") {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Text(
            "> $message█",
            color = WallStreetGreen.copy(alpha = alpha),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

@Composable
fun TerminalLoadingSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(GridGray.copy(alpha = alpha), RectangleShape)
            .border(1.dp, GridGray, RectangleShape)
    )
}

@Composable
fun TerminalErrorOverlay(message: String, onRetry: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CRTBlack)
            .padding(16.dp)
            .border(2.dp, WallStreetRed, RectangleShape),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "!!! SYSTEM FAILURE !!!",
            color = WallStreetRed,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ERROR_LOG: $message",
            color = WallStreetRed,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "PRESS ANYWHERE TO RETRY",
            color = WallStreetAmber,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.background(CRTBlack)
        )
    }
}

@Composable
fun NoDataState(message: String = "NO_RECORDS_FOUND") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ">>> $message <<<",
            color = GridGray,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
