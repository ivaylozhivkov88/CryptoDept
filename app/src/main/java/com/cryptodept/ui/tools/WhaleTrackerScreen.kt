package com.cryptodept.ui.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import com.cryptodept.util.TerminalFormatter
import com.cryptodept.viewmodel.WhaleViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WhaleTrackerScreen(
    onBack: () -> Unit,
    viewModel: WhaleViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val transactions by viewModel.transactions.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(TerminalConfig.UI.DEFAULT_PADDING),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> WHALE TRACKER",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = TerminalConfig.UI.FONT_SIZE_HEADER,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(TerminalConfig.UI.ICON_SIZE_SMALL),
                    color = colors.primary,
                    strokeWidth = TerminalConfig.UI.BORDER_WIDTH * 2,
                )
            } else {
                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    border = BorderStroke(1.dp, colors.primary),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("REFRESH", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        if (transactions.isEmpty() && !isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "NO LARGE ON-CHAIN MOVES DETECTED RECENTLY",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = TerminalConfig.UI.FONT_SIZE_NORMAL,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TerminalConfig.UI.SPACER_MEDIUM),
            ) {
                items(transactions, key = { it.id }) { tx ->
                    WhaleTxCard(tx)
                }
            }
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(TerminalConfig.UI.BORDER_WIDTH, colors.primary),
            shape = RectangleShape,
        ) {
            Text(TerminalConfig.Strings.BACK_TO_TOOLS, color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun WhaleTxCard(tx: WhaleTransaction) {
    val colors = LocalTerminalColors.current
    val uriHandler = LocalUriHandler.current
    val sdf = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.US) }

    val blockchainLabel =
        when (tx.blockchain) {
            Blockchain.ETHEREUM -> "ETH"
            Blockchain.SOLANA -> "SOL"
            Blockchain.BITCOIN -> "BTC"
        }

    TerminalCard(title = "TX: ${TerminalFormatter.formatShortAddress(tx.transactionHash)}") {
        Column(
            modifier =
                Modifier.clickable {
                    val url =
                        when (tx.blockchain) {
                            Blockchain.ETHEREUM -> "https://etherscan.io/tx/${tx.transactionHash}"
                            Blockchain.SOLANA -> "https://solscan.io/tx/${tx.transactionHash}"
                            Blockchain.BITCOIN -> "https://mempool.space/tx/${tx.transactionHash}"
                        }
                    uriHandler.openUri(url)
                },
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${TerminalFormatter.formatPrice(tx.amount)} ${tx.symbol}",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = TerminalConfig.UI.FONT_SIZE_LARGE,
                )
                Text(
                    text = TerminalFormatter.formatCurrency(tx.amountUsd),
                    color = colors.amber,
                    fontSize = TerminalConfig.UI.FONT_SIZE_MEDIUM,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_MEDIUM))

            Text(
                text = "FROM: ${TerminalFormatter.formatShortAddress(tx.fromAddress)}",
                color = colors.dimText,
                fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "TO:   ${TerminalFormatter.formatShortAddress(tx.toAddress)}",
                color = colors.dimText,
                fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_MEDIUM))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "BLOCKCHAIN: $blockchainLabel",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = TerminalConfig.UI.FONT_SIZE_MICRO,
                )
                Text(
                    text = sdf.format(Date(tx.timestamp)),
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = TerminalConfig.UI.FONT_SIZE_MICRO,
                )
            }
        }
    }
}
