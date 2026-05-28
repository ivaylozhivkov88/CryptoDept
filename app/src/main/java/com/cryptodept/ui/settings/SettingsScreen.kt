@file:Suppress("DEPRECATION")

package com.cryptodept.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cryptodept.ui.settings.sections.SettingsTerminalSection
import com.cryptodept.ui.settings.sections.SettingsNotificationsSection
import com.cryptodept.ui.settings.sections.SettingsPsychologySection
import com.cryptodept.ui.settings.sections.SettingsAccountSection
import com.cryptodept.ui.navigation.Screen
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

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
                                val error = result.exceptionOrNull()
                                val message = if (error?.message?.contains("recent authentication", ignoreCase = true) == true) {
                                    "SECURITY_PROTOCOL: Recent login required. Please sign out and sign back in before erasing data."
                                } else {
                                    "ERROR: ${error?.message}"
                                }
                                snackbarHostState.showSnackbar(message)
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
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black)
                    .padding(16.dp),
        ) {
            item {
                Text(
                    ">>> SYSTEM_SETTINGS_V2",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // SECURITY WARNING
            item {
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
            }

            item {
                SettingsTerminalSection(
                    phosphorMode = phosphorMode,
                    screensaverTimeout = screensaverTimeout,
                    powerUserMode = powerUserMode,
                    forceShowAllFeatures = forceShowAllFeatures,
                    onPhosphorModeChange = viewModel::setPhosphorMode,
                    onScreensaverTimeoutChange = viewModel::setScreensaverTimeout,
                    onPowerUserModeChange = viewModel::setPowerUserMode,
                    onForceShowAllFeaturesChange = viewModel::setForceShowAllFeatures,
                    onNavigateToGlossary = { navController.navigate(Screen.Glossary.route) },
                    onRestartTutorial = viewModel::restartOnboarding
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SettingsNotificationsSection(
                    soundEnabled = soundEnabled,
                    notificationsEnabled = notificationsEnabled,
                    hapticEnabled = hapticEnabled,
                    onSoundEnabledChange = viewModel::setSoundsEnabled,
                    onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                    onHapticEnabledChange = viewModel::setHapticEnabled
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SettingsPsychologySection(
                    tiltProtectionEnabled = tiltProtectionEnabled,
                    onTiltProtectionEnabledChange = viewModel::setTiltProtectionEnabled
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                SettingsAccountSection(
                    isPro = isPro,
                    isAdmin = isAdmin,
                    currentUser = currentUser,
                    tierName = tier.name,
                    isAuthenticating = isAuthenticating,
                    onShowPaywall = { showPaywall = true },
                    onDeleteAccount = { showDeleteConfirmation = true },
                    onSignOut = { authService.signOut() },
                    onSignIn = {
                        try {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("SIGN_IN_ERROR: Check Google Play Services")
                            }
                        }
                    },
                    onForceSyncIdentity = { viewModel.forceSyncIdentity(authService) },
                    onNavigateToPredictionRecord = { navController.navigate(com.cryptodept.ui.navigation.Screen.Prediction.route) },
                    onSetAdminStatus = viewModel::setAdminStatus,
                    onSetProStatus = viewModel::setProStatus,
                    onOpenPrivacyPolicy = { uriHandler.openUri("https://gist.githubusercontent.com/ivaylozhivkov88/147ca22ec93a2af3dd9224c69466af82/raw/") }
                )
            }

            item {
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
}
