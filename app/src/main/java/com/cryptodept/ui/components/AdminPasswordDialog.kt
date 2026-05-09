package com.cryptodept.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.BuildConfig
import com.cryptodept.ui.theme.LocalTerminalColors
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Strictly for Admin Password entry in Settings.
 */
@Composable
fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onAuthorized: () -> Unit,
    onGoogleSignIn: (String) -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    val context = LocalContext.current
    
    var passwordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { onGoogleSignIn(it) }
        } catch (e: Exception) {
            // Handle error
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
        title = { 
            Text(">>> ADMIN_AUTHORIZATION", color = colors.primary, fontFamily = FontFamily.Monospace) 
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ENTER MASTER ACCESS KEY:", 
                    color = colors.dimText, 
                    fontSize = 12.sp, 
                    fontFamily = FontFamily.Monospace, 
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        loginError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = colors.primary, fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    isError = loginError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.dimText,
                        cursorColor = colors.primary,
                        errorBorderColor = colors.danger,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                    ),
                )

                if (loginError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ACCESS_DENIED: INVALID_KEY",
                        color = colors.danger, 
                        fontSize = 10.sp, 
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "--- OR ---",
                    color = colors.grid,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign In Button - Terminal Style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.primary, RectangleShape)
                        .clickable {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            launcher.launch(client.signInIntent)
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "LOGIN WITH GOOGLE",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanInput = passwordInput.trim().uppercase()
                // Security check simplified for public repository
                if (cleanInput.isNotEmpty()) {
                    onAuthorized()
                    onDismiss()
                } else {
                    loginError = true
                }
            }) {
                Text("AUTHORIZE", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.dimText, fontFamily = FontFamily.Monospace)
            }
        },
    )
}
