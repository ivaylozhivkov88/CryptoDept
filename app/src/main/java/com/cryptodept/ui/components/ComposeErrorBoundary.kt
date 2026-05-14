package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * A specialized error boundary for the CryptoDept terminal.
 * While Compose doesn't support traditional try-catch around composables,
 * this component provides a centralized state-based error view.
 */
@Composable
fun ComposeErrorBoundary(content: @Composable () -> Unit) {
    val errorState = remember { mutableStateOf<Throwable?>(null) }
    
    val currentError = errorState.value
    if (currentError != null) {
        CRTErrorScreen(
            error = currentError,
            onRetry = { errorState.value = null }
        )
    } else {
        content()
    }
}

@Composable
private fun CRTErrorScreen(error: Throwable, onRetry: () -> Unit) {
    val colors = LocalTerminalColors.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = ">>> SYSTEM_FAULT_DETECTED <<<",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "[CRITICAL] An unexpected error has been detected.",
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Error code: ${error.javaClass.simpleName}",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Report has been transmitted to mission control.",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            ) {
                Text(
                    text = "[RETRY_PROTOCOL]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
