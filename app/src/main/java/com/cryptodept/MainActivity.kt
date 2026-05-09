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
import com.cryptodept.ui.components.crt.CRTOverlay
import com.cryptodept.ui.navigation.NavGraph
import com.cryptodept.ui.onboarding.OnboardingScreen
import com.cryptodept.ui.screensaver.*
import com.cryptodept.ui.theme.*
import com.cryptodept.util.ConnectivityObserver
import com.cryptodept.util.HapticService
import com.cryptodept.util.TerminalAudioManager
import com.cryptodept.util.toCurrency
import com.cryptodept.util.toPercentage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    lateinit var screensaverCycleService: ScreensaverCycleService

    @Inject
    lateinit var heatmapDataRepository: com.cryptodept.domain.repository.HeatmapDataRepository

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        checkNotificationPermission()

        lifecycleScope.launch {
            preferencesService.incrementLaunchCount()
        }

        NewsSyncWorker.schedule(this)
        com.cryptodept.service.CryptoDataSyncWorker.schedule(this)
        com.cryptodept.service.AccuracyVerificationWorker
            .schedule(this)

        setContent {
            val phosphorModeStr by preferencesService.phosphorMode.collectAsState(initial = "GREEN")
            val mode =
                when (phosphorModeStr) {
                    "AMBER" -> PhosphorMode.AMBER
                    "WHITE" -> PhosphorMode.CRT
                    else -> PhosphorMode.GREEN
                }

            var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            var isIdle by remember { mutableStateOf(false) }

            val windowInsetsController =
                remember {
                    WindowCompat.getInsetsController(window, window.decorView)
                }

            val screensaverTimeout by preferencesService.screensaverTimeout.collectAsState(initial = 1)
            val btcPriceFlow = remember { cryptoRepository.getCoinPrice("bitcoin") }
            val btcPriceState by btcPriceFlow.collectAsState(initial = null)

            val allPricesState by remember(cryptoRepository) {
                cryptoRepository.getTrackedCoinPrices().map { list ->
                    list.map { "${it.symbol.uppercase()} ${it.currentPrice.toCurrency(0)}" }
                }
            }.collectAsState(initial = emptyList())

            val riskScoreState by riskEngine.observeRiskScore().collectAsState(initial = 50)
            val currentScreensaver by screensaverCycleService.currentScreensaver.collectAsState()
            val heatmapData by heatmapDataRepository.getHeatmapData().collectAsState(initial = emptyList())
            val isOnboardingComplete by preferencesService.isOnboardingComplete.collectAsState(initial = true)
            val connectivityStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)

            LaunchedEffect(lastInteractionTime, screensaverTimeout) {
                isIdle = false
                if (screensaverTimeout > 0) {
                    delay(screensaverTimeout * 60 * 1000L)
                    isIdle = true
                    screensaverCycleService.startCycling()
                }
            }

            LaunchedEffect(isIdle) {
                if (isIdle) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                    screensaverCycleService.stopCycling()
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            CompositionLocalProvider(
                LocalTerminalAudioManager provides soundService,
                com.cryptodept.ui.components.LocalHapticManager provides hapticService,
                com.cryptodept.ui.components.LocalAnalyticsManager provides analyticsService,
            ) {
                CryptoDeptTheme(mode = mode) {
                    if (!isOnboardingComplete) {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                lifecycleScope.launch {
                                    preferencesService.setOnboardingComplete(true)
                                }
                            },
                        )
                    } else {
                        val navController = rememberNavController()
                        
                        // Handle shortcut route
                        LaunchedEffect(intent) {
                            intent?.getStringExtra("route")?.let { route ->
                                navController.navigate(route)
                            }
                        }

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
                                        NavGraph(
                                            navController = navController,
                                            preferencesService = preferencesService,
                                        )
                                    }
                                    CRTOverlay()
                                }
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

                                androidx.compose.animation.Crossfade(
                                    targetState = currentScreensaver,
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(300),
                                    label = "screensaver_fade",
                                ) { type ->
                                    when (type) {
                                        ScreensaverType.BLOOMBERG_WALL -> {
                                            BloombergWallScreen(
                                                modifier = Modifier.fillMaxSize(),
                                                onDismiss = {
                                                    isIdle = false
                                                    lastInteractionTime = System.currentTimeMillis()
                                                },
                                            )
                                        }
                                        ScreensaverType.MATRIX_RAIN -> {
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
                                        ScreensaverType.HEATMAP -> {
                                            HeatmapScreensaverScreen(items = heatmapData)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        reviewService.requestReviewIfAppropriate(this)
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
}
