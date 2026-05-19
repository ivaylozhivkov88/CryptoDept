package com.cryptodept.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.cryptodept.R
import com.cryptodept.data.datastore.SystemSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalAudioManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: SystemSettingsManager,
    ) {
        private val soundPool: SoundPool
        private val sounds = mutableMapOf<String, Int>()

        private var isEnabled = false
        private var currentVolume = 0.5f
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        companion object {
            const val SOUND_BOOT = "boot"
            const val SOUND_ALERT = "alert"
            const val SOUND_GLITCH = "glitch"
            const val SOUND_CLICK = "click"
            const val SOUND_BEEP = "beep"
            const val SOUND_CHIRP = "chirp"
            const val SOUND_CHIME = "chime"
            const val SOUND_POWERUP = "powerup"
        }

        init {
            val audioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

            soundPool =
                SoundPool
                    .Builder()
                    .setMaxStreams(10)
                    .setAudioAttributes(audioAttributes)
                    .build()

            loadSound(SOUND_BOOT, R.raw.boot_complete)
            loadSound(SOUND_ALERT, R.raw.beep_alert)
            loadSound(SOUND_GLITCH, R.raw.glitch_sound)
            loadSound(SOUND_CLICK, R.raw.keyclick)
            // Stubs for new sounds
            loadSound(SOUND_BEEP, R.raw.keyclick)
            loadSound(SOUND_CHIRP, R.raw.beep_alert)
            loadSound(SOUND_CHIME, R.raw.beep_alert)
            loadSound(SOUND_POWERUP, R.raw.boot_complete)

            scope.launch {
                settings.soundsEnabled.collect { enabled ->
                    isEnabled = enabled
                }
            }
            scope.launch {
                settings.soundsVolume.collect { volume ->
                    currentVolume = volume
                }
            }
        }

        private fun loadSound(
            name: String,
            resId: Int,
        ) {
            sounds[name] = soundPool.load(context, resId, 1)
        }

        fun playSound(name: String) {
            if (!isEnabled) return

            sounds[name]?.let { id ->
                soundPool.play(id, currentVolume, currentVolume, 1, 0, 1f)
            }
        }

        fun playBoot() = playSound(SOUND_BOOT)

        fun playAlert() = playSound(SOUND_ALERT)

        fun playGlitch() = playSound(SOUND_GLITCH)

        fun playClick() = playSound(SOUND_CLICK)

        fun playBeep() = playSound(SOUND_BEEP)

        fun playChirp() = playSound(SOUND_CHIRP)

        fun playChime() = playSound(SOUND_CHIME)

        fun playPowerUp() = playSound(SOUND_POWERUP)

        fun release() {
            soundPool.release()
        }
    }
