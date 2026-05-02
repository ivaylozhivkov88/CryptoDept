package com.cryptodept.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.WallStreetAmber
import com.cryptodept.ui.theme.WallStreetGreen
import com.cryptodept.ui.theme.WallStreetWhite
import kotlinx.coroutines.delay

@Composable
fun PsychologyLockOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, WallStreetAmber)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = ">>> SYSTEM LOCK: EMOTIONAL OVERLOAD",
                    color = WallStreetAmber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Discipline is the bridge between goals and accomplishment. A clear mind sees opportunity where a tilted mind sees only revenge.",
                    color = WallStreetWhite,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                var countdown by remember { mutableIntStateOf(10) }
                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        countdown = 10
                        while (countdown > 0) {
                            delay(1000)
                            countdown--
                        }
                    }
                }
                
                Text(
                    text = "COOLDOWN ACTIVE: 00:00:${String.format("%02d", countdown)}",
                    color = WallStreetAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    enabled = countdown == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (countdown == 0) WallStreetGreen else Color.DarkGray,
                        contentColor = Color.Black
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (countdown > 0) "WAIT FOR CLARITY..." else "RESUME TERMINAL",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
