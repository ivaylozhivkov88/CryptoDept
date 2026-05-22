package com.cryptodept.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.cryptodept.data.datastore.SystemSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

typealias HapticManager = HapticService

@Singleton
class HapticService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: SystemSettingsManager,
    ) {
        private val scope = CoroutineScope(Dispatchers.Main)

        private val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

        fun lightTick() = checkAndVibrate { vibrate(8) }

        fun tick() = lightTick()

        fun navTick() = checkAndVibrate { vibrate(15) }

        fun whaleAlert() = checkAndVibrate { vibrate(longArrayOf(0, 60, 80, 60)) }

        fun priceAlert() = checkAndVibrate { vibrate(40) }

        fun tiltLock() = checkAndVibrate { vibrate(400) }

        fun achievement() = checkAndVibrate { vibrate(longArrayOf(0, 50, 60, 50, 60, 50)) }

        fun confirm() =
            checkAndVibrate {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    vibrate(20)
                }
            }

        fun success() =
            checkAndVibrate {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                } else {
                    vibrate(longArrayOf(0, 30, 50, 30))
                }
            }

        fun error() =
            checkAndVibrate {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    vibrate(longArrayOf(0, 100, 50, 100))
                }
            }

        fun alert() =
            checkAndVibrate {
                vibrate(longArrayOf(0, 50, 100, 50, 100, 50))
            }

        private fun checkAndVibrate(action: () -> Unit) {
            scope.launch {
                if (settings.hapticEnabled.first()) {
                    action()
                }
            }
        }

        private fun vibrate(ms: Long) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        }

        private fun vibrate(pattern: LongArray) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
