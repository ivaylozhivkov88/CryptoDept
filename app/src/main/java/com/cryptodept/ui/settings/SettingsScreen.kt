package com.cryptodept.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptodept.BuildConfig
import com.cryptodept.data.auth.AuthService
import com.cryptodept.ui.components.AdminPasswordDialog
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.SettingsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsScreenEntryPoint {
    fun authService(): AuthService
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, SettingsScreenEntryPoint::class.java)
    }
    val authService = entryPoint.authService()
    val currentUser by authService.currentUser.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isAuthenticating by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                coroutineScope.launch {
                    isAuthenticating = true
                    val signInResult = authService.signInWithGoogle(idToken)
                    isAuthenticating = false
                    if (signInResult.isSuccess) {
                        viewModel.setAdminStatus(true)
                        snackbarHostState.showSnackbar("AUTHENTICATION_SUCCESSFUL: OPERATOR_LOGGED_IN")
                    } else {
                        snackbarHostState.showSnackbar("AUTHENTICATION_FAILED: ${signInResult.exceptionOrNull()?.message}")
                    }
                }
            } ?: run { isAuthenticating = false }
        } catch (e: Exception) {
            isAuthenticating = false
            coroutineScope.launch {
                snackbarHostState.showSnackbar("AUTH_ERROR: ${e.message}")
            }
        }
    }

    val colors = LocalTerminalColors.current
    val soundEnabled by viewModel.soundsEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val powerUserMode by viewModel.powerUserMode.collectAsState()
    val screensaverTimeout by viewModel.screensaverTimeout.collectAsState()
    val phosphorMode by viewModel.phosphorMode.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val securityWarning by viewModel.securityWarning.collectAsState()

    val billingViewModel: com.cryptodept.viewmodel.BillingViewModel = hiltViewModel()
    val isPro by billingViewModel.billingManager.isPro.collectAsState()

    var showAdminDialog by remember { mutableStateOf(false) }

    // SECRET ADMIN TRIGGER: Tap the "SYSTEM_SETTINGS_V2" text 5 times
    var adminTapCount by remember { mutableIntStateOf(0) }

    if (showAdminDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminDialog = false },
            onAuthorized = { viewModel.setAdminStatus(true) },
            onGoogleSignIn = { idToken ->
                coroutineScope.launch {
                    val result = authService.signInWithGoogle(idToken)
                    if (result.isSuccess) {
                        viewModel.setAdminStatus(true)
                        showAdminDialog = false
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color.Black,
                    contentColor = colors.primary,
                    modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
                    content = {
                        Text(data.visuals.message, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
        Text(
            ">>> SYSTEM_SETTINGS_V2",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            modifier = Modifier.clickable {
                adminTapCount++
                if (adminTapCount >= 5) {
                    showAdminDialog = true
                    adminTapCount = 0
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // SECURITY WARNING (NEW)
        securityWarning?.let { warning ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Color.Red,
                            androidx.compose.foundation.shape
                                .RoundedCornerShape(2.dp),
                        ).background(Color.Red.copy(alpha = 0.1f))
                        .padding(12.dp),
            ) {
                Text(warning, color = Color.Red, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PHOSPHOR MODE TOGGLE
        SettingRow(
            label = "PHOSPHOR_TYPE", 
            desc = "Current: $phosphorMode", 
            checked = true,
            modifier = Modifier.tutorialTarget(TutorialTargetId.SETTINGS_THEME)
        ) {
            val nextMode = if (phosphorMode == "GREEN") "AMBER" else "GREEN"
            viewModel.setPhosphorMode(nextMode)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PRO STATUS
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isPro) colors.primary else colors.amber, RectangleShape)
                    .background(if (isPro) colors.primary.copy(alpha = 0.05f) else colors.amber.copy(alpha = 0.05f))
                    .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (isPro) "CRYPTODEPT PRO ACTIVE" else "CRYPTODEPT FREE TIER",
                        color = if (isPro) colors.primary else colors.amber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = if (isPro) "Unlimited Terminal Access" else "Limited to 3 Tracked Coins",
                        color = colors.dimText,
                        fontSize = 10.sp,
                    )
                }

                if (!isPro) {
                    Text(
                        text = "[GO PRO]",
                        color = colors.amber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Trigger Paywall */ },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AUDIO
        SettingRow("AUDIO_FEEDBACK", "Synthesized sound effects", soundEnabled) {
            viewModel.setSoundsEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NOTIFICATIONS
        SettingRow("PUSH_ALERTS", "High-confidence signal alerts", notificationsEnabled) {
            viewModel.setNotificationsEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HAPTIC
        SettingRow("HAPTIC_FEEDBACK", "Tactile response on interaction", hapticEnabled) {
            viewModel.setHapticEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // POWER USER
        SettingRow("POWER_USER_MODE", "Enable advanced tools & FFT scans", powerUserMode) {
            viewModel.setPowerUserMode(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SCREENSAVER TIMEOUT
        val timeoutLabel = if (screensaverTimeout == 0) "OFF" else "${screensaverTimeout}min"
        SettingRow("SCREENSAVER", "Current timeout: $timeoutLabel", true) {
            val timeouts = listOf(0, 2, 5, 10, 30)
            val nextIdx = (timeouts.indexOf(screensaverTimeout) + 1) % timeouts.size
            viewModel.setScreensaverTimeout(timeouts[nextIdx])
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RESTART TUTORIAL
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                    .tutorialTarget(TutorialTargetId.SETTINGS_REPLAY_TUTORIAL)
                    .clickable { viewModel.restartOnboarding() }
                    .padding(12.dp),
        ) {
            Column {
                Text("RESTART_TUTORIAL", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text("Reset first-run onboarding sequence", color = colors.dimText, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- GOOGLE AUTH SECTION ---
        Text(
            text = ">>> AUTHENTICATION_SYSTEM",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (currentUser != null) {
            TerminalCard(title = "OPERATOR: ${currentUser?.email}") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "STATUS: AUTHENTICATED",
                        color = colors.primary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { authService.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, colors.danger)
                    ) {
                        Text("DE-AUTHORIZE (SIGN OUT)", color = colors.danger, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isAuthenticating) colors.grid else colors.primary, RectangleShape)
                    .clickable(enabled = !isAuthenticating) {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isAuthenticating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "AUTHENTICATING...",
                            color = colors.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text(
                        "LOGIN WITH GOOGLE",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = BorderStroke(1.dp, colors.primary),
            shape = RectangleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        ) {
            Text("< RETURN_TO_CORE", fontFamily = FontFamily.Monospace)
        }
    }
    }
}

@Composable
fun SettingRow(
    label: String,
    desc: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(desc, color = colors.dimText, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.primary,
                    checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                    uncheckedBorderColor = colors.dimText,
                ),
        )
    }
}
