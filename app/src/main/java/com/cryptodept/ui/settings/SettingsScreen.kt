@file:Suppress("DEPRECATION")

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptodept.BuildConfig
import com.cryptodept.data.auth.AuthService
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.components.FeatureHelpIcon
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.navigation.Screen
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
    navController: androidx.navigation.NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
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
    val tier by viewModel.tierAccessManager.currentTier.collectAsState()
    val isPro = tier.isPaid
    val isAdmin = tier.isAdmin
    
    val soundEnabled by viewModel.soundsEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val powerUserMode by viewModel.powerUserMode.collectAsState()
    val screensaverTimeout by viewModel.screensaverTimeout.collectAsState()
    val phosphorMode by viewModel.phosphorMode.collectAsState()
    val securityWarning by viewModel.securityWarning.collectAsState()
    val forceShowAllFeatures by viewModel.forceShowAllFeatures.collectAsState()
    val tiltProtectionEnabled by viewModel.tiltProtectionEnabled.collectAsState()

    var showPaywall by remember { mutableStateOf(false) }

    if (showPaywall) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPaywall = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.cryptodept.ui.paywall.PaywallScreen(
                onDismiss = { showPaywall = false }
            )
        }
    }

    // Scaffold with Snackbar
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
                            colors.error,
                            androidx.compose.foundation.shape
                                .RoundedCornerShape(2.dp),
                        ).background(colors.error.copy(alpha = 0.1f))
                        .padding(12.dp),
            ) {
                Text(warning, color = colors.error, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    .tutorialTarget(TutorialTargetId.SETTINGS_TIER)
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
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPro) "Unlimited Terminal Access (Max 15 Coins)" else "Limited to 3 Tracked Coins",
                            color = colors.dimText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                        FeatureHelpIcon(
                            feature = if (isPro) FeatureKey.WATCHLISTS_UNLIMITED else FeatureKey.WATCHLIST_SINGLE,
                            iconSize = 10.dp
                        )
                    }
                }

                if (!isPro) {
                    Text(
                        text = "[GO PRO]",
                        color = colors.amber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showPaywall = true },
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

        // TILT PROTECTION (Change 9)
        SettingRow("TILT_PROTECTION", "Auto-locks terminal during emotional volatility", tiltProtectionEnabled) {
            viewModel.setTiltProtectionEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PROGRESSIVE DISCLOSURE OVERRIDE
        SettingRow("SHOW_ALL_FEATURES", "Override progressive disclosure", forceShowAllFeatures) {
            viewModel.setForceShowAllFeatures(it)
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

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                    .tutorialTarget(TutorialTargetId.SETTINGS_GLOSSARY)
                    .clickable { navController.navigate(Screen.Glossary.route) }
                    .padding(12.dp),
        ) {
            Column {
                Text("CRYPTO_GLOSSARY", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text("Learn key crypto and trading terms", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
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
                Text("Reset first-run onboarding sequence", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // LEGAL & COMPLIANCE
        Text(
            text = ">>> LEGAL_&_COMPLIANCE",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                .clickable { uriHandler.openUri("https://gist.githubusercontent.com/ivaylozhivkov88/147ca22ec93a2af3dd9224c69466af82/raw/") }
                .padding(12.dp),
        ) {
            Column {
                Text("PRIVACY_POLICY", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text("Review how we handle your data", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var isDeleting by remember { mutableStateOf(false) }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteConfirmation = false },
                title = { Text(">>> DELETE_DATA_PROTOCOL", color = colors.danger, fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
                text = {
                    Text(
                        "WARNING: This action will permanently erase your account, access tier, and all quantitative history from our cloud nodes. This process cannot be undone.",
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                containerColor = Color.Black,
                modifier = Modifier.border(1.dp, colors.danger, RectangleShape),
                confirmButton = {
                    TextButton(
                        enabled = !isDeleting,
                        onClick = {
                            coroutineScope.launch {
                                isDeleting = true
                                val result = viewModel.deleteAccount(authService)
                                isDeleting = false
                                if (result.isSuccess) {
                                    showDeleteConfirmation = false
                                    snackbarHostState.showSnackbar("DATA_ERASED: SESSION_TERMINATED")
                                } else {
                                    snackbarHostState.showSnackbar("ERROR: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.danger, strokeWidth = 2.dp)
                        } else {
                            Text("CONFIRM_ERASE", color = colors.danger, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                },
                dismissButton = {
                    TextButton(enabled = !isDeleting, onClick = { showDeleteConfirmation = false }) {
                        Text("CANCEL", color = colors.dimText, fontFamily = FontFamily.Monospace)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.danger.copy(alpha = 0.2f), RectangleShape)
                .clickable(enabled = currentUser != null) { showDeleteConfirmation = true }
                .padding(12.dp),
        ) {
            Column {
                val textColor = if (currentUser != null) colors.danger else colors.dimText
                Text("DELETE_ACCOUNT", color = textColor, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text(
                    if (currentUser != null) "Permanently remove all data from terminal" else "Log in to manage account data",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ADMIN CONSOLE (Visible only to authorized admins)
        if (isAdmin) {
            Text(
                text = ">>> ADMIN_COMMAND_CENTER",
                color = colors.danger,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.danger, RectangleShape)
                    .clickable { navController.navigate(com.cryptodept.ui.navigation.Screen.Prediction.route) }
                    .padding(12.dp),
            ) {
                Column {
                    Text("PREDICTION_TRACK_RECORD", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text("Historical AI accuracy metrics & stats", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

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
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "RESOLVED_TIER: ${tier.name}",
                        color = colors.amber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.forceSyncIdentity(authService) }) {
                        Text("[ FORCE_IDENTITY_SYNC ]", color = colors.primary, fontSize = 10.sp)
                    }
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

        Spacer(modifier = Modifier.height(24.dp))

        // --- TEST/REVIEWER TOOLS ---
        // Task 1.3: Button should be visible for reviewers in Play Store (Closed Test)
        // but hidden in the final Production release.
        if (com.cryptodept.util.TestModeFlag.IS_TEST_PERIOD) {
            Text(
                text = ">>> INTERNAL_TESTING_TOOLS",
                color = colors.danger,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle Admin Status
                OutlinedButton(
                    onClick = { viewModel.setAdminStatus(!isAdmin) },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.danger)
                ) {
                    Text(if (isAdmin) "REVOKE_ADMIN" else "GRANT_ADMIN", color = colors.danger, fontSize = 9.sp)
                }

                // Activate Pro (Simulation)
                OutlinedButton(
                    onClick = { viewModel.setProStatus(!isPro) },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.amber)
                ) {
                    Text(if (isPro) "REVOKE_PRO" else "ACTIVATE_PRO", color = colors.amber, fontSize = 9.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // VERSION INFO
        androidx.compose.material3.Text(
            text = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            Text(desc, color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
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
