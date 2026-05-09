package com.cryptodept.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun startListening(): Flow<SpeechResult> =
            callbackFlow {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    trySend(SpeechResult.Error("Speech recognition not available"))
                    close()
                    return@callbackFlow
                }

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent =
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    }

                val listener =
                    object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            trySend(SpeechResult.Ready)
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            val message =
                                when (error) {
                                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing"
                                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy"
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
                                    else -> "Unknown error"
                                }
                            trySend(SpeechResult.Error(message))
                            close()
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            trySend(SpeechResult.Success(matches?.firstOrNull() ?: ""))
                            close()
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            trySend(SpeechResult.Partial(matches?.firstOrNull() ?: ""))
                        }

                        override fun onEvent(
                            eventType: Int,
                            params: Bundle?,
                        ) {}
                    }

                recognizer.setRecognitionListener(listener)
                recognizer.startListening(intent)

                awaitClose {
                    recognizer.stopListening()
                    recognizer.destroy()
                }
            }
    }

sealed class SpeechResult {
    object Ready : SpeechResult()

    data class Partial(
        val text: String,
    ) : SpeechResult()

    data class Success(
        val text: String,
    ) : SpeechResult()

    data class Error(
        val message: String,
    ) : SpeechResult()
}
