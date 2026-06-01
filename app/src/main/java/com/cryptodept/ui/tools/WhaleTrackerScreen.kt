package com.cryptodept.ui.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.WhaleTransactionV2
import com.cryptodept.domain.model.WhaleSignificance
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.components.skeletons.WhaleTxSkeleton
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
        modifier = Modifier
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
                text = ">>> WHALE TRACKER V2",
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
                    strokeWidth = 2.dp,
                )
            } else {
                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    border = BorderStroke(1.dp, colors.primary),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("SCAN", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        if (isRefreshing && transactions.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("SCANNING_ON_CHAIN_NODES...", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("FETCHING_BTC_ETH_SOL_FLOWS", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        } else if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "NO LARGE MOVES DETECTED ON-CHAIN",
                        color = colors.dimText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = TerminalConfig.UI.FONT_SIZE_NORMAL,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "(Scanning for $1M+ BTC, $2M+ ETH, $500k+ SOL moves)",
                        color = colors.grid,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                    Text(
                        "LAST_SCAN: ${sdf.format(Date())}",
                        color = colors.grid,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { viewModel.refresh() },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
                    ) {
                        Text("FORCE_SCAN_NODES", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TerminalConfig.UI.SPACER_MEDIUM),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = transactions, 
                    key = { it.hash },
                    contentType = { "whale_tx" }
                ) { tx ->
                    WhaleTxCardV2(tx)
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
fun WhaleTxCardV2(tx: WhaleTransactionV2) {
    val colors = LocalTerminalColors.current
    val uriHandler = LocalUriHandler.current
    val sdf = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.US) }

    val sigColor = when (tx.significance) {
        WhaleSignificance.MEGA -> colors.danger
        WhaleSignificance.LARGE -> colors.amber
        else -> colors.primary
    }

    TerminalCard(
        title = "${tx.significance.emoji} ${tx.significance.label}",
        titleColor = sigColor
    ) {
        Column(
            modifier = Modifier.clickable { tx.explorerUrl?.let { uriHandler.openUri(it) } },
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                val from = tx.fromOwner ?: TerminalFormatter.formatShortAddress(tx.fromAddress)
                val to = tx.toOwner ?: TerminalFormatter.formatShortAddress(tx.toAddress)
                
                Text(from, color = if (tx.fromOwner != null) colors.amber else colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(" → ", color = colors.grid, fontSize = 11.sp)
                Text(to, color = if (tx.toOwner != null) colors.amber else colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_MEDIUM))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "BLOCKCHAIN: ${tx.blockchain.uppercase()}",
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
