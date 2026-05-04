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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.cryptodept.data.datastore.PreferencesManager
import com.cryptodept.service.NewsSyncWorker
import com.cryptodept.service.SoundManager
import com.cryptodept.ui.components.GlobalMarketBar
import com.cryptodept.ui.components.TerminalBottomBar
import com.cryptodept.ui.components.crt.CRTOverlay
import com.cryptodept.ui.navigation.NavGraph
import com.cryptodept.ui.screensaver.MatrixRainScreen
import com.cryptodept.ui.theme.*
import com.cryptodept.util.HapticManager
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.RiskScoreEngine
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var soundManager: SoundManager

    @Inject
    lateinit var hapticManager: HapticManager

    @Inject
    lateinit var analyticsManager: com.cryptodept.util.AnalyticsManager

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var cryptoRepository: CryptoRepository

    @Inject
    lateinit var riskEngine: RiskScoreEngine

    @Inject
    lateinit var reviewManager: com.cryptodept.util.ReviewManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        checkNotificationPermission()

        lifecycleScope.launch {
            preferencesManager.incrementLaunchCount()
        }

        NewsSyncWorker.schedule(this)
        com.cryptodept.service.AlertWorker.schedule(this)

        setContent {
            val phosphorModeStr by preferencesManager.phosphorMode.collectAsState(initial = "GREEN")
            val mode = when (phosphorModeStr) {
                "AMBER" -> PhosphorMode.AMBER
                "WHITE" -> PhosphorMode.CRT
                else -> PhosphorMode.GREEN
            }

            var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            var isIdle by remember { mutableStateOf(false) }

            val windowInsetsController = remember {
                WindowCompat.getInsetsController(window, window.decorView)
            }

            val screensaverTimeout by preferencesManager.screensaverTimeout.collectAsState(initial = 5)
            val btcPriceFlow = remember { cryptoRepository.getCoinPrice("bitcoin") }
            val btcPriceState by btcPriceFlow.collectAsState(initial = null)
            val riskScoreState by riskEngine.observeRiskScore().collectAsState(initial = 50)

            LaunchedEffect(lastInteractionTime, screensaverTimeout) {
                isIdle = false
                if (screensaverTimeout > 0) {
                    delay(screensaverTimeout * 60 * 1000L)
                    isIdle = true
                }
            }

            LaunchedEffect(isIdle) {
                if (isIdle) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            CompositionLocalProvider(
                LocalSoundManager provides soundManager,
                com.cryptodept.ui.components.LocalHapticManager provides hapticManager,
                com.cryptodept.ui.components.LocalAnalyticsManager provides analyticsManager
            ) {
                CryptoDeptTheme(mode = mode) {
                    val navController = rememberNavController()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent()
                                        lastInteractionTime = System.currentTimeMillis()
                                        isIdle = false
                                    }
                                }
                            }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = androidx.compose.ui.graphics.Color.Black,
                            bottomBar = {
                                if (!isIdle) TerminalBottomBar(navController)
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    GlobalMarketBar()
                                    NavGraph(
                                        navController = navController,
                                        preferencesManager = preferencesManager
                                    )
                                }
                                CRTOverlay()
                            }
                        }

                        if (isIdle) {
                            val btcDisplay = btcPriceState?.let { 
                                "$${String.format(java.util.Locale.US, "%,.0f", it.currentPrice)} ${if(it.priceChangePercentage24h >= 0) "▲" else "▼"}${String.format("%.1f", Math.abs(it.priceChangePercentage24h))}%"
                            } ?: "FETCHING..."

                            var showMatrix by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(60_000L)
                                showMatrix = true
                            }

                            if (showMatrix) {
                                MatrixRainScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    btcPrice = btcDisplay,
                                    riskScore = riskScoreState,
                                    onDismiss = {
                                        isIdle = false
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                )
                            } else {
                                com.cryptodept.ui.screensaver.BloombergWallScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    onDismiss = {
                                        isIdle = false
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                )
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
        reviewManager.requestReviewIfAppropriate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}