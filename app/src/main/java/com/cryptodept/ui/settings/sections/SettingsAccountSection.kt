package com.cryptodept.ui.settings.sections

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.BuildConfig
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.components.FeatureHelpIcon
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.ui.tutorial.tutorialTarget

@Composable
fun SettingsAccountSection(
    isPro: Boolean,
    isAdmin: Boolean,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    tierName: String,
    isAuthenticating: Boolean,
    onShowPaywall: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    onForceSyncIdentity: () -> Unit,
    onNavigateToPredictionRecord: () -> Unit,
    onSetAdminStatus: (Boolean) -> Unit,
    onSetProStatus: (Boolean) -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val colors = LocalTerminalColors.current

    Column {
        // PRO STATUS
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, if (isPro) colors.primary else colors.amber, RectangleShape)
                .background(if (isPro) colors.primary.copy(alpha = 0.05f) else colors.amber.copy(alpha = 0.05f))
                .tutorialTarget(TutorialTargetId.SETTINGS_TIER)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (isPro) "CRYPTODEPT PRO ACTIVE" else "CRYPTODEPT FREE TIER",
                        color = if (isPro) colors.primary else colors.amber,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPro) "Unlimited Terminal Access (Max 30 Coins)" else "Limited to 3 Tracked Coins",
                            color = if (isPro) colors.primary else colors.dimText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                        FeatureHelpIcon(
                            feature = if (isPro) FeatureKey.WATCHLISTS_UNLIMITED else FeatureKey.WATCHLIST_SINGLE,
                            iconSize = 10.dp
                        )
                    }
                }

                if (!isPro) {
                    Text(
                        text = "[GO PRO]",
                        color = colors.amber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onShowPaywall() },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // LEGAL & COMPLIANCE
        Text(
            text = ">>> LEGAL_&_COMPLIANCE",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                .clickable { onOpenPrivacyPolicy() }
                .padding(12.dp),
        ) {
            Column {
                Text("PRIVACY_POLICY", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text("Review how we handle your data", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.danger.copy(alpha = 0.2f), RectangleShape)
                .clickable(enabled = currentUser != null) { onDeleteAccount() }
                .padding(12.dp),
        ) {
            Column {
                val textColor = if (currentUser != null) colors.danger else colors.dimText
                Text("DELETE_ACCOUNT", color = textColor, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text(
                    if (currentUser != null) "Permanently remove all data from terminal" else "Log in to manage account data",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ADMIN CONSOLE
        if (isAdmin) {
            Text(
                text = ">>> ADMIN_COMMAND_CENTER",
                color = colors.danger,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.danger, RectangleShape)
                    .clickable { onNavigateToPredictionRecord() }
                    .padding(12.dp),
            ) {
                Column {
                    Text("PREDICTION_TRACK_RECORD", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text("Historical AI accuracy metrics & stats", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // AUTH SECTION
        Text(
            text = ">>> AUTHENTICATION_SYSTEM",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val context = LocalContext.current
        var showRedeemHint by remember { mutableStateOf(false) }

        if (showRedeemHint) {
            AlertDialog(
                onDismissRequest = { showRedeemHint = false },
                title = { Text("REDEEM PROMO CODE", color = colors.primary, fontFamily = FontFamily.Monospace) },
                text = {
                    Text(
                        "To redeem an app-specific code (like WELCOME26):\n\n" +
                        "1. Tap [GO TO PURCHASE] below.\n" +
                        "2. In the Google Play window, tap your payment method (or the small arrow).\n" +
                        "3. Select 'Redeem code' from the list.\n" +
                        "4. Enter your code to activate your 7-day Intelligence Pass.",
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showRedeemHint = false
                        onShowPaywall() 
                    }) {
                        Text("GO TO PURCHASE", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRedeemHint = false }) {
                        Text("CLOSE", color = colors.dimText)
                    }
                },
                containerColor = Color.Black,
                modifier = Modifier.border(1.dp, colors.primary, RectangleShape)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showRedeemHint = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "REDEEM PROMO CODE",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Text(
                    text = "How to activate your 7-day Intelligence Pass",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = "?",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
            )
        }

        HorizontalDivider(color = colors.grid, thickness = 0.5.dp)

        if (currentUser != null) {
            TerminalCard(title = "OPERATOR: ${currentUser.email}") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "STATUS: AUTHENTICATED",
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "RESOLVED_TIER: $tierName",
                        color = colors.amber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onForceSyncIdentity) {
                        Text("[ FORCE_IDENTITY_SYNC ]", color = colors.primary, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, colors.danger)
                    ) {
                        Text(
                            text = "DE-AUTHORIZE (SIGN OUT)",
                            color = colors.danger,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isAuthenticating) colors.grid else colors.primary, RectangleShape)
                    .clickable(enabled = !isAuthenticating) { onSignIn() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isAuthenticating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "AUTHENTICATING...",
                            color = colors.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text(
                        "LOGIN WITH GOOGLE",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TEST TOOLS
        if (com.cryptodept.util.TestModeFlag.IS_TEST_PERIOD) {
            Text(
                text = ">>> INTERNAL_TESTING_TOOLS",
                color = colors.danger,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSetAdminStatus(!isAdmin) },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.danger)
                ) {
                    Text(if (isAdmin) "REVOKE_ADMIN" else "GRANT_ADMIN", color = colors.danger, fontSize = 9.sp)
                }

                OutlinedButton(
                    onClick = { onSetProStatus(!isPro) },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.amber)
                ) {
                    Text(if (isPro) "REVOKE_PRO" else "ACTIVATE_PRO", color = colors.amber, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // VERSION INFO
        Text(
            text = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
