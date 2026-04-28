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
import com.cryptodept.ui.navigation.NavGraph
import com.cryptodept.ui.components.TerminalBottomBar
import com.cryptodept.ui.components.GlobalMarketBar
import com.cryptodept.ui.theme.CryptoDeptTheme
import com.cryptodept.ui.components.crt.CRTOverlay
import com.cryptodept.ui.screensaver.MatrixRainScreen
import com.cryptodept.data.datastore.PreferencesManager
import com.cryptodept.service.SoundManager
import com.cryptodept.ui.theme.LocalSoundManager
import com.cryptodept.ui.theme.PhosphorMode
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var soundManager: SoundManager

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Предотвратява заспиването на екрана по време на работа с терминала
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        checkNotificationPermission()

        // Логване на FCM Токен за диагностика на известията
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("FCM_TOKEN", ">>> CORE_SYSTEM_ID: ${task.result}")
            }
        }

        setContent {
            // Извличане на системните настройки за визуализация
            val phosphorModeStr by preferencesManager.phosphorMode.collectAsState(initial = "GREEN")
            val mode = when (phosphorModeStr) {
                "AMBER" -> PhosphorMode.AMBER
                "WHITE" -> PhosphorMode.CRT // Уверете се, че PhosphorMode има CRT стойност
                else -> PhosphorMode.GREEN
            }

            // Управление на състоянието на неактивност (Screensaver)
            var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            var isIdle by remember { mutableStateOf(false) }

            val windowInsetsController = remember {
                WindowCompat.getInsetsController(window, window.decorView)
            }

            // Таймер за автоматично включване на скрийнсейвъра (60 сек)
            LaunchedEffect(lastInteractionTime) {
                isIdle = false
                delay(60000L)
                isIdle = true
            }

            // Скриване/Показване на системните ленти (Status & Navigation) при неактивност
            LaunchedEffect(isIdle) {
                if (isIdle) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                CryptoDeptTheme(mode = mode) {
                    val navController = rememberNavController()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                // Глобално засичане на докосвания за нулиране на таймера за неактивност
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent()
                                        lastInteractionTime = System.currentTimeMillis()
                                        isIdle = false
                                    }
                                }
                            }
                    ) {
                        // Основна структура на терминала
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = androidx.compose.ui.graphics.Color.Black,
                            bottomBar = {
                                // Скриваме лентата, за да не "свети" под скрийнсейвъра
                                if (!isIdle) TerminalBottomBar(navController)
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    GlobalMarketBar()

                                    // ВАЖНО: NavGraph трябва да приема preferencesManager в дефиницията си!
                                    NavGraph(
                                        navController = navController,
                                        preferencesManager = preferencesManager
                                    )
                                }
                                CRTOverlay() // Ефект на сканиращи линии (CRT)
                            }
                        }

                        // Пълноекранен Matrix Screensaver
                        if (isIdle) {
                            MatrixRainScreen(
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

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release() // Почистване на аудио ресурсите
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}