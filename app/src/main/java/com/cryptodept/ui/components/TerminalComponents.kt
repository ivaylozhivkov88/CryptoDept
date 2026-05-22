package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.*
import com.cryptodept.util.AnalyticsManager
import com.cryptodept.util.HapticManager
import kotlinx.coroutines.launch

val LocalHapticManager = staticCompositionLocalOf<HapticManager?> { null }
val LocalAnalyticsManager = staticCompositionLocalOf<AnalyticsManager?> { null }

/**
 * High-end terminal glow style.
 */
@Composable
fun terminalTextStyle(
    color: Color = LocalTerminalColors.current.primary,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    fontWeight: FontWeight? = null,
    glow: Boolean = false
): TextStyle {
    return TextStyle(
        color = color,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        shadow = if (glow) Shadow(
            color = color.copy(alpha = 0.6f),
            blurRadius = 8f
        ) else null
    )
}

@Composable
fun TerminalCard(
    title: String,
    titleColor: Color = TextGray,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, GridGray)
                .padding(12.dp),
    ) {
        Text(title, color = titleColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray, fontSize = 10.sp) },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        textStyle = LocalTextStyle.current.copy(color = WallStreetGreen, fontFamily = FontFamily.Monospace),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WallStreetGreen,
                unfocusedBorderColor = GridGray,
                cursorColor = WallStreetGreen,
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
            ),
        shape = RectangleShape,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun TerminalLoadingSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, GridGray, RectangleShape)
                .background(Color(0xFF0A0A0A))
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.45f)
                        .height(8.dp)
                        .background(GridGray.copy(alpha = 0.55f), RoundedCornerShape(2.dp)),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .background(GridGray.copy(alpha = 0.35f), RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
fun TerminalErrorOverlay(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    val scope = rememberCoroutineScope()
    var isRetrying by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, colors.error, RectangleShape)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "ERROR",
            color = colors.error,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = if (isRetrying) ">>> REBOOTING_DATA_LINK..." else message.ifBlank { "Unknown error" },
            color = colors.error.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        if (onRetry != null) {
            TextButton(
                onClick = {
                    if (!isRetrying) {
                        isRetrying = true
                        scope.launch {
                            kotlinx.coroutines.delay(800)
                            onRetry()
                            isRetrying = false
                        }
                    }
                },
                enabled = !isRetrying,
                shape = RectangleShape,
                colors = ButtonDefaults.textButtonColors(contentColor = if (isRetrying) colors.dimText else colors.primary),
                modifier = Modifier.border(1.dp, if (isRetrying) colors.dimText else colors.primary, RectangleShape),
            ) {
                Text(if (isRetrying) "WAIT..." else "RETRY", fontFamily = FontFamily.Monospace)
            }
        }
    }
}
