package com.cryptodept.ui.components.crt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.TerminalGreen
import kotlinx.coroutines.delay

@Composable
fun BootSequenceScreen(onBootComplete: () -> Unit) {
    val bootLogs = remember { mutableStateListOf<String>() }
    val fullLogs = listOf(
        "CRYPTODEPT SYSTEM V1.0.24",
        "COPYRIGHT (C) 1994 CRYPTODEPT CORP.",
        "------------------------------------",
        "INITIALIZING MEMORY CHECK... OK",
        "LOADING NETWORK DRIVERS... OK",
        "CONNECTING TO BINANCE CLOUD... OK",
        "ESTABLISHING SECURE TUNNEL... OK",
        "FETCHING GLOBAL MARKET STATE... OK",
        "DECRYPTING CRYPTO STREAMS... OK",
        "------------------------------------",
        "SYSTEM READY.",
        "PRESS START (OR WAIT)..."
    )

    val asciiArt = """
     _  _  _  _  _  _  _  _  _ 
    | || || || || || || || || |
    | || || || || || || || || |
    |_||_||_||_||_||_||_||_||_|
     C R Y P T O D E P T
    """.trimIndent()

    LaunchedEffect(Unit) {
        delay(500)
        bootLogs.add("> SYSTEM BOOT")
        for (log in fullLogs) {
            delay(300)
            bootLogs.add("> $log")
        }
        delay(1000)
        onBootComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = asciiArt,
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(bootLogs) { log ->
                    Text(
                        text = log,
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        
        // Add CRT Overlay on top for effect
        CRTOverlay()
    }
}
