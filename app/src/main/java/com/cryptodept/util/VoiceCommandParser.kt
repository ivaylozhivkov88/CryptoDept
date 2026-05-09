package com.cryptodept.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandParser
    @Inject
    constructor() {
        fun parse(text: String): VoiceCommand {
            val input = text.lowercase()

            return when {
                input.contains("alert") || input.contains("warn") -> {
                    val symbol = extractSymbol(input)
                    val price = extractPrice(input)
                    VoiceCommand.CreateAlert(symbol, price)
                }
                input.contains("analyze") || input.contains("report") -> {
                    val symbol = extractSymbol(input)
                    VoiceCommand.Analyze(symbol)
                }
                input.contains("price") || input.contains("show") -> {
                    val symbol = extractSymbol(input)
                    VoiceCommand.ShowPrice(symbol)
                }
                input.contains("coach") || input.contains("help") -> {
                    VoiceCommand.OpenCoach
                }
                else -> VoiceCommand.Unknown
            }
        }

        private fun extractSymbol(input: String): String {
            val symbols = listOf("bitcoin", "ethereum", "solana", "xrp", "cardano", "polkadot")
            for (s in symbols) {
                if (input.contains(s)) return s
            }
            val words = input.split(" ")
            val knownSymbols = listOf("btc", "eth", "sol", "ada", "dot", "xrp")
            for (w in words) {
                if (knownSymbols.contains(w)) return w
            }
            return "bitcoin"
        }

        private fun extractPrice(input: String): Double {
            val regex = Regex("""(\d+([.,]\d+)?)""")
            val match = regex.find(input)
            return match?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        }
    }

sealed class VoiceCommand {
    data class ShowPrice(
        val symbol: String,
    ) : VoiceCommand()

    data class CreateAlert(
        val symbol: String,
        val price: Double,
    ) : VoiceCommand()

    data class Analyze(
        val symbol: String,
    ) : VoiceCommand()

    object OpenCoach : VoiceCommand()

    object Unknown : VoiceCommand()
}
