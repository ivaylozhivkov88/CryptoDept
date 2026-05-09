package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalCommandViewModel
    @Inject
    constructor(
        private val speechManager: SpeechManager,
        private val commandParser: VoiceCommandParser,
        private val ttsService: TerminalTtsService,
    ) : ViewModel() {
        private val _speechState = MutableStateFlow<SpeechResult?>(null)
        val speechState: StateFlow<SpeechResult?> = _speechState.asStateFlow()

        fun startListening() {
            viewModelScope.launch {
                speechManager.startListening().collect { result ->
                    _speechState.value = result
                }
            }
        }

        fun handleCommand(text: String): VoiceCommand {
            val command = commandParser.parse(text)
            when (command) {
                is VoiceCommand.ShowPrice -> ttsService.speak("FETCHING ${command.symbol.uppercase()} PRICE DATA...")
                is VoiceCommand.CreateAlert -> ttsService.speak("SETTING ALERT FOR ${command.symbol.uppercase()} AT ${command.price}...")
                is VoiceCommand.Analyze -> ttsService.speak("INITIALIZING DEEP ANALYSIS FOR ${command.symbol.uppercase()}...")
                VoiceCommand.OpenCoach -> ttsService.speak("AI COACH ONLINE. STANDBY.")
                VoiceCommand.Unknown -> ttsService.speak("SIGNAL UNCLEAR. REPEAT COMMAND.")
            }
            return command
        }

        fun stopTts() {
            ttsService.stop()
        }

        override fun onCleared() {
            super.onCleared()
            ttsService.shutdown()
        }
    }
