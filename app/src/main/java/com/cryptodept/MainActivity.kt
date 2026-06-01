package com.cryptodept

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.service.NewsSyncWorker
import com.cryptodept.ui.components.GlobalMarketBar
import com.cryptodept.ui.components.TerminalBottomBar
import com.cryptodept.ui.components.LocalHapticManager
import com.cryptodept.ui.components.LocalAnalyticsManager
import com.cryptodept.ui.components.crt.CRTOverlay
import com.cryptodept.ui.navigation.NavGraph
import com.cryptodept.ui.tutorial.*
import com.cryptodept.domain.tutorial.TutorialController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.platform.LocalContext
import com.cryptodept.ui.components.ComposeErrorBoundary
import com.cryptodept.ui.onboarding.OnboardingScreen
import com.cryptodept.ui.screensaver.*
import com.cryptodept.ui.theme.*
import com.cryptodept.util.ConnectivityObserver
import com.cryptodept.util.HapticService
import com.cryptodept.util.TerminalAudioManager
import com.cryptodept.util.toCurrency
import com.cryptodept.util.toPercentage
import com.cryptodept.data.remoteconfig.RemoteConfigService
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri
import android.content.Intent
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MainActivityEntryPoint {
    fun tutorialController(): TutorialController
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var soundService: TerminalAudioManager

    @Inject
    lateinit var hapticService: HapticService

    @Inject
    lateinit var analyticsService: com.cryptodept.util.AnalyticsService

    @Inject
    lateinit var preferencesService: PreferencesService

    @Inject
    lateinit var cryptoRepository: CryptoRepository

    @Inject
    lateinit var riskEngine: RiskScoreEngine

    @Inject
    lateinit var reviewService: com.cryptodept.util.ReviewService

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    @Inject
    lateinit var remoteConfig: RemoteConfigService

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Refresh subscription/pass status on start
        preferencesService.checkProStatus()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        checkNotificationPermission()

        lifecycleScope.launch {
            preferencesService.incrementLaunchCount()
        }

        NewsSyncWorker.schedule(this)
        com.cryptodept.service.CryptoDataSyncWorker.schedule(this)
        com.cryptodept.service.AccuracyVerificationWorker
            .schedule(this)

        // --- FETCH AND LOG FCM TOKEN ---
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("FCM_TOKEN", ">>> CURRENT_TOKEN: ${task.result}")
            }
        }

        // --- SUBSCRIBE TO GLOBAL ALERTS ---
        val fcm = com.google.firebase.messaging.FirebaseMessaging.getInstance()
        fcm.subscribeToTopic("market_alerts")
        fcm.subscribeToTopic("session_transitions")
        fcm.subscribeToTopic("daily_picks")
        
        fcm.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM", ">>> Subscribed to market_alerts")
                }
            }

        @OptIn(kotlinx.coroutines.FlowPreview::class)
        setContent {
            val context = LocalContext.current
            val phosphorModeStr by preferencesService.phosphorMode.collectAsState(initial = "GREEN")
            
            // --- FORCE UPDATE LOGIC ---
            var isUpdateRequired by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                } else {
                    packageManager.getPackageInfo(packageName, 0).versionCode
                }
                
                remoteConfig.fetchAndActivate {
                    val minVersion = remoteConfig.getMinVersionCode()
                    if (currentVersion < minVersion) {
                        isUpdateRequired = true
                    }
                }
            }

            val mode =
                when (phosphorModeStr) {
                    "AMBER" -> PhosphorMode.AMBER
                    "WHITE" -> PhosphorMode.CRT
                    else -> PhosphorMode.GREEN
                }

            var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            var isIdle by remember { mutableStateOf(false) }
            var showBootSequence by remember { mutableStateOf(true) }

            val windowInsetsController =
                remember {
                    WindowCompat.getInsetsController(window, window.decorView)
                }
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

            val screensaverTimeout by preferencesService.screensaverTimeout.collectAsState(initial = 1)
            val btcPriceFlow = remember { cryptoRepository.getCoinPrice("bitcoin") }
            val btcPriceState by btcPriceFlow.collectAsState(initial = null)

            val allPricesState by remember(cryptoRepository) {
                cryptoRepository.getTrackedCoinPrices()
                    .sample(60_000L) // Optimization: update once per minute
                    .map { list ->
                        list.map { coin ->
                            val symbol = coin.symbol.uppercase()
                            val price = if (coin.currentPrice < 1.0) {
                                String.format(Locale.US, "$%.4f", coin.currentPrice)
                            } else {
                                coin.currentPrice.toCurrency(2)
                            }
                            val change = coin.priceChangePercentage24h
                            val sign = if (change >= 0) "▲" else "▼"
                            "$symbol PRICE: $price $sign${String.format(Locale.US, "%.1f", Math.abs(change))}%"
                        }
                    }
            }.collectAsState(initial = emptyList())

            val riskScoreState by riskEngine.observeRiskScore().collectAsState(initial = 50)
            val isOnboardingComplete by preferencesService.isOnboardingComplete.collectAsState(initial = null)
            val connectivityStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)
            
            val mainEntryPoint = remember(context) {
                EntryPointAccessors.fromApplication(context.applicationContext, MainActivityEntryPoint::class.java)
            }
            val tutorialController = mainEntryPoint.tutorialController()
            val tutorialState by tutorialController.state.collectAsState()
            val targetRegistry = remember { TutorialTargetRegistry() }

            LaunchedEffect(lastInteractionTime, screensaverTimeout) {
                isIdle = false
                if (screensaverTimeout > 0) {
                    delay(screensaverTimeout * 60 * 1000L)
                    isIdle = true
                }
            }

            LaunchedEffect(isIdle) {
                if (isIdle) {
                    keyboardController?.hide()
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            CompositionLocalProvider(
                LocalTerminalAudioManager provides soundService,
                LocalHapticManager provides hapticService,
                LocalAnalyticsManager provides analyticsService,
            ) {
                CryptoDeptTheme(mode = mode) {
                    if (isUpdateRequired) {
                        ForceUpdateScreen()
                    } else if (isOnboardingComplete == null) {
                        // SPLASH BUFFER: Wait for state to be read from DataStore to prevent flickering
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                    } else if (isOnboardingComplete == false) {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                lifecycleScope.launch {
                                    preferencesService.setOnboardingComplete(true)
                                }
                            },
                        )
                    } else if (showBootSequence) {
                        com.cryptodept.ui.components.TerminalBootScreen(
                            onComplete = { showBootSequence = false }
                        )
                    } else {
                        val navController = rememberNavController()
                        
                        // Auto-navigate when current step's screenRoute differs from current
                        LaunchedEffect(tutorialState.currentStep?.id) {
                            val step = tutorialState.currentStep ?: return@LaunchedEffect
                            val currentRoute = navController.currentBackStackEntry?.destination?.route ?: return@LaunchedEffect

                            if (currentRoute != step.screenRoute) {
                                kotlinx.coroutines.delay(200)
                                navController.navigate(step.screenRoute) {
                                    launchSingleTop = true
                                }
                                kotlinx.coroutines.delay(600)
                            }
                        }

                        // Handle shortcut route
                        LaunchedEffect(intent) {
                            intent?.getStringExtra("route")?.let { route ->
                                navController.navigate(route)
                            }
                        }

                        CompositionLocalProvider(
                            LocalTutorialTargetRegistry provides targetRegistry
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    awaitPointerEvent()
                                                    lastInteractionTime = System.currentTimeMillis()
                                                    isIdle = false
                                                }
                                            }
                                        },
                            ) {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    containerColor = androidx.compose.ui.graphics.Color.Black,
                                    bottomBar = {
                                        val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
                                        if (!isIdle && !isKeyboardVisible) TerminalBottomBar(navController)
                                    },
                                ) { innerPadding ->
                                    Box(modifier = Modifier.padding(innerPadding)) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            if (connectivityStatus != ConnectivityObserver.Status.Available) {
                                                com.cryptodept.ui.components.OfflineBanner()
                                            }
                                            GlobalMarketBar()
                                            ComposeErrorBoundary {
                                                NavGraph(
                                                    navController = navController,
                                                    preferencesService = preferencesService,
                                                )
                                            }
                                        }
                                        CRTOverlay()
                                    }
                                }

                                // OVERLAY ABOVE EVERYTHING
                                TutorialOverlay(
                                    controller = tutorialController,
                                    registry = targetRegistry
                                )

                                // START DIALOG
                                if (tutorialState.showStartDialog) {
                                    TutorialStartDialog(
                                        onStart = { tutorialController.startTutorial() },
                                        onSkip = {
                                            tutorialController.dismissStartDialog()
                                            tutorialController.completeTutorial()
                                        }
                                    )
                                }

                                // SKIP CONFIRMATION
                                if (tutorialState.showSkipConfirmation) {
                                    TutorialSkipConfirmDialog(
                                        onConfirm = { tutorialController.confirmSkip() },
                                        onCancel = { tutorialController.cancelSkip() }
                                    )
                                }

                                // COMPLETION DIALOG
                                if (tutorialState.showCompletionDialog) {
                                    TutorialCompletionDialog(
                                        onDismiss = { tutorialController.dismissCompletionDialog() }
                                    )
                                }

                                if (isIdle) {
                                    val btcDisplay =
                                        btcPriceState?.let {
                                            "${it.currentPrice.toCurrency(
                                                0,
                                            )} ${if (it.priceChangePercentage24h >= 0) "▲" else "▼"}${it.priceChangePercentage24h.toPercentage(
                                                decimals = 1,
                                            )}"
                                        } ?: "FETCHING..."

                                    MatrixRainScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        btcPrice = btcDisplay,
                                        allPrices = allPricesState,
                                        riskScore = riskScoreState,
                                        onDismiss = {
                                            isIdle = false
                                            lastInteractionTime = System.currentTimeMillis()
                                        },
                                    )
                                }
                            }
                        }

                        // Trigger start dialog after onboarding
                        LaunchedEffect(isOnboardingComplete) {
                            if (tutorialController.shouldShowStartDialog()) {
                                kotlinx.coroutines.delay(800)
                                tutorialController.promptToStartTutorial()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            volumeControlStream = android.media.AudioManager.STREAM_MUSIC
            reviewService.requestReviewIfAppropriate(this)
            
            // Check if update was downloaded while app was in background
            val appUpdateManager = AppUpdateManagerFactory.create(this)
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    android.util.Log.d("Update", "Update downloaded, ready to install")
                }
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    android.util.Log.d("Update", "Update in progress, resuming UI")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Google Play Services interaction failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Composable
    private fun ForceUpdateScreen() {
        val colors = LocalTerminalColors.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .border(1.dp, colors.danger, RectangleShape)
                    .padding(24.dp)
            ) {
                Text(
                    text = "[!!] UPDATE_REQUIRED [!!]",
                    color = colors.danger,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "A critical system update is required to maintain terminal integrity. Your current version is no longer supported by the Global Market Feed.",
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        text = "UPDATE VIA GOOGLE PLAY",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
