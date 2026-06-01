package com.cryptodept.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.update.AppUpdateState
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.AppUpdateViewModel

/**
 * Update banner that appears at top of Dashboard when update is available.
 * 
 * Non-invasive design:
 * - Dismissible (24h cooldown)
 * - Inline (does not block main content)
 * - CRT-styled (consistent with app aesthetic)
 * 
 * Immediate updates (priority 4-5) bypass this and use full-screen flow.
 */
@Composable
fun AppUpdateBanner(
    viewModel: AppUpdateViewModel = hiltViewModel(),
    activity: android.app.Activity,
) {
    val state by viewModel.updateState.collectAsState()
    
    // Auto-check on first composition
    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
    }
    
    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { _ ->
        // User accepted or declined update flow
    }
    
    AnimatedVisibility(
        visible = state is AppUpdateState.Available ||
                  state is AppUpdateState.Downloading ||
                  state is AppUpdateState.DownloadedReadyToInstall,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
    ) {
        when (val currentState = state) {
            is AppUpdateState.Available -> {
                if (currentState.isImmediate) {
                    // Critical update — auto-launch immediate update flow
                    LaunchedEffect(Unit) {
                        viewModel.startImmediateUpdate(activity, updateLauncher)
                    }
                } else {
                    // Flexible update — show banner
                    FlexibleUpdateBanner(
                        sizeBytes = currentState.sizeBytes,
                        onUpdate = {
                            viewModel.startFlexibleUpdate(activity, updateLauncher)
                        },
                        onDismiss = { viewModel.dismissUpdate() },
                    )
                }
            }
            is AppUpdateState.Downloading -> {
                DownloadProgressBanner(
                    progressPercent = currentState.progressPercent,
                    bytesDownloaded = currentState.bytesDownloaded,
                    totalBytes = currentState.totalBytes,
                )
            }
            AppUpdateState.DownloadedReadyToInstall -> {
                InstallReadyBanner(
                    onInstall = { viewModel.completeUpdate() },
                    onLater = { /* will prompt on next app launch */ },
                )
            }
            else -> { /* no-op */ }
        }
    }
}

@Composable
private fun FlexibleUpdateBanner(
    sizeBytes: Long,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val sizeMb = sizeBytes / (1024 * 1024)
    
    Surface(
        color = colors.background,
        border = BorderStroke(1.dp, colors.amber),
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⬇️", fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ">>> UPDATE_READY",
                        color = colors.amber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "Build: ${sizeMb}MB",
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            
            Spacer(Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "[ DOWNLOAD ]",
                        color = colors.background,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "[ LATER ]",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressBanner(
    progressPercent: Int,
    bytesDownloaded: Long,
    totalBytes: Long,
) {
    val colors = LocalTerminalColors.current
    val downloadedMb = bytesDownloaded / (1024 * 1024)
    val totalMb = totalBytes / (1024 * 1024)
    
    Surface(
        color = colors.background,
        border = BorderStroke(1.dp, colors.primary),
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier.size(24.dp),
                    color = colors.primary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = ">>> DOWNLOADING_UPDATE",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "${downloadedMb}MB / ${totalMb}MB ($progressPercent%)",
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.grid,
            )
        }
    }
}

@Composable
private fun InstallReadyBanner(
    onInstall: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    
    Surface(
        color = colors.background,
        border = BorderStroke(1.dp, colors.primary),
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "✅ UPDATE_READY_TO_INSTALL",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "App will restart to complete the update.",
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onLater) {
                    Text(
                        text = "[LATER]",
                        color = colors.dimText,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RectangleShape,
                ) {
                    Text(
                        text = "[INSTALL_AND_RESTART]",
                        color = colors.background,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
