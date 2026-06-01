package com.cryptodept.domain.usecase.whale

import com.cryptodept.BuildConfig
import com.cryptodept.data.api.etherscan.EtherscanService
import com.cryptodept.data.api.helius.HeliusService
import com.cryptodept.data.api.mempool.MempoolService
import com.cryptodept.data.remoteconfig.RemoteConfigService
import com.cryptodept.data.whales.KnownWallets
import com.cryptodept.domain.model.WhaleTransactionV2
import com.cryptodept.domain.model.WhaleSignificance
import com.cryptodept.domain.model.TransactionType
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.util.WhaleThresholds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import java.math.BigDecimal
import java.math.BigInteger

class AggregateWhaleActivityUseCase @Inject constructor(
    private val etherscan: EtherscanService,
    private val mempool: MempoolService,
    private val helius: HeliusService,
    private val cryptoRepository: CryptoRepository,
    private val remoteConfig: RemoteConfigService,
) {
    
    suspend fun execute(
        minUsd: Double? = null,
        maxPerChain: Int = 10,
    ): List<WhaleTransactionV2> = coroutineScope {
        val ethPrice = cryptoRepository.getCachedPrice("ethereum")
        val btcPrice = cryptoRepository.getCachedPrice("bitcoin")
        val solPrice = cryptoRepository.getCachedPrice("solana")
        
        val ethWhales = async { fetchEthWhales(ethPrice, maxPerChain) }
        val btcWhales = async { fetchBtcWhales(btcPrice, maxPerChain) }
        val solWhales = async { fetchSolWhales(solPrice, maxPerChain) }
        
        val ethThreshold = minUsd ?: WhaleThresholds.ETH_USD
        val btcThreshold = minUsd ?: WhaleThresholds.BTC_USD
        val solThreshold = minUsd ?: WhaleThresholds.SOL_USD
        
        (ethWhales.await().filter { it.amountUsd >= ethThreshold } + 
         btcWhales.await().filter { it.amountUsd >= btcThreshold } + 
         solWhales.await().filter { it.amountUsd >= solThreshold })
            .sortedByDescending { it.timestamp }
    }
    
    private suspend fun fetchEthWhales(ethPrice: Double, limit: Int): List<WhaleTransactionV2> = coroutineScope {
        val apiKey = BuildConfig.ETHERSCAN_API_KEY
        if (apiKey.isBlank()) return@coroutineScope emptyList()
        
        KnownWallets.ETH_WALLETS.take(5).map { wallet ->
            async {
                try {
                    val response = etherscan.getNormalTransactions(address = wallet.address, apiKey = apiKey, offset = limit)
                    if (response.status == "1") {
                        response.result.map { tx ->
                            val amount = BigInteger(tx.value).toBigDecimal().divide(BigDecimal("1000000000000000000")).toDouble()
                            val usd = amount * ethPrice
                            WhaleTransactionV2(
                                hash = tx.hash,
                                blockchain = "ethereum",
                                symbol = "ETH",
                                amount = amount,
                                amountUsd = usd,
                                fromAddress = tx.from,
                                toAddress = tx.to,
                                fromOwner = KnownWallets.lookupName(tx.from)?.name,
                                toOwner = KnownWallets.lookupName(tx.to)?.name,
                                transactionType = TransactionType.classify(KnownWallets.lookupName(tx.from)?.name, KnownWallets.lookupName(tx.to)?.name),
                                timestamp = tx.timestamp.toLong() * 1000L,
                                explorerUrl = "https://etherscan.io/tx/${tx.hash}",
                                significance = WhaleSignificance.fromAmount(usd)
                            )
                        }
                    } else emptyList()
                } catch (_: Exception) { emptyList() }
            }
        }.awaitAll().flatten()
    }

    private suspend fun fetchBtcWhales(btcPrice: Double, limit: Int): List<WhaleTransactionV2> = coroutineScope {
        KnownWallets.BTC_ADDRESSES.take(3).map { wallet ->
            async {
                try {
                    val txs = mempool.getAddressTransactions(wallet.address)
                    txs.take(limit).map { tx ->
                        val totalSats = tx.vout.sumOf { it.value }
                        val amount = totalSats / 100_000_000.0
                        val usd = amount * btcPrice
                        WhaleTransactionV2(
                            hash = tx.txid,
                            blockchain = "bitcoin",
                            symbol = "BTC",
                            amount = amount,
                            amountUsd = usd,
                            fromAddress = tx.vin.firstOrNull()?.prevout?.scriptpubkey_address ?: "",
                            toAddress = tx.vout.firstOrNull()?.scriptpubkey_address ?: "",
                            fromOwner = tx.vin.firstOrNull()?.prevout?.scriptpubkey_address?.let { KnownWallets.lookupName(it)?.name },
                            toOwner = tx.vout.firstOrNull()?.scriptpubkey_address?.let { KnownWallets.lookupName(it)?.name },
                            transactionType = TransactionType.classify(null, null),
                            timestamp = (tx.status.block_time ?: (System.currentTimeMillis() / 1000)) * 1000L,
                            explorerUrl = "https://mempool.space/tx/${tx.txid}",
                            significance = WhaleSignificance.fromAmount(usd)
                        )
                    }
                } catch (_: Exception) { emptyList() }
            }
        }.awaitAll().flatten()
    }

    private suspend fun fetchSolWhales(solPrice: Double, limit: Int): List<WhaleTransactionV2> = coroutineScope {
        val apiKey = BuildConfig.HELIUS_API_KEY
        if (apiKey.isBlank()) return@coroutineScope emptyList()
        
        KnownWallets.SOL_WALLETS.take(3).map { wallet ->
            async {
                try {
                    val txs = helius.getAddressTransactions(address = wallet.address, apiKey = apiKey, limit = limit)
                    txs.mapNotNull { tx ->
                        val transfer = tx.nativeTransfers?.firstOrNull() ?: return@mapNotNull null
                        val amount = transfer.amount / 1_000_000_000.0
                        val usd = amount * solPrice
                        WhaleTransactionV2(
                            hash = tx.signature,
                            blockchain = "solana",
                            symbol = "SOL",
                            amount = amount,
                            amountUsd = usd,
                            fromAddress = transfer.fromUserAccount ?: "",
                            toAddress = transfer.toUserAccount ?: "",
                            fromOwner = transfer.fromUserAccount?.let { KnownWallets.lookupName(it)?.name },
                            toOwner = transfer.toUserAccount?.let { KnownWallets.lookupName(it)?.name },
                            transactionType = TransactionType.classify(null, null),
                            timestamp = tx.timestamp * 1000L,
                            explorerUrl = "https://solscan.io/tx/${tx.signature}",
                            significance = WhaleSignificance.fromAmount(usd)
                        )
                    }
                } catch (_: Exception) { emptyList() }
            }
        }.awaitAll().flatten()
    }
}
