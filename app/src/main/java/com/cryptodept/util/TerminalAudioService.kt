package com.cryptodept.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.cryptodept.R
import com.cryptodept.data.datastore.PreferencesService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalAudioService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferencesService: PreferencesService,
    ) {
        private val soundPool: SoundPool
        private val sounds = mutableMapOf<String, Int>()

        private var isEnabled = true
        private var currentVolume = 0.5f
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        companion object {
            const val SOUND_BOOT = "boot"
            const val SOUND_ALERT = "alert"
            const val SOUND_GLITCH = "glitch"
            const val SOUND_CLICK = "click"
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
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build()

            loadSound(SOUND_BOOT, R.raw.boot_complete)
            loadSound(SOUND_ALERT, R.raw.beep_alert)
            loadSound(SOUND_GLITCH, R.raw.glitch_sound)
            loadSound(SOUND_CLICK, R.raw.keyclick)

            // Subscribing to settings changes
            scope.launch {
                preferencesService.soundsEnabled.collect { enabled ->
                    isEnabled = enabled
                }
            }
            scope.launch {
                preferencesService.soundsVolume.collect { volume ->
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

        fun release() {
            soundPool.release()
        }
    }
