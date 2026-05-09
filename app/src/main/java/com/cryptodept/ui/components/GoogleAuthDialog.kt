package com.cryptodept.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.BuildConfig
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun GoogleAuthDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val context = LocalContext.current
    val authViewModel: AuthViewModel = hiltViewModel()
    var error by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account.idToken?.let { idToken ->
                authViewModel.signInWithGoogle(
                    idToken = idToken,
                    onSuccess = { 
                        onSuccess()
                        onDismiss()
                    },
                    onError = { error = it }
                )
            } ?: run { error = "MISSING_ID_TOKEN" }
        } catch (e: Exception) {
            error = "AUTH_FAILED: ${e.message}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
        title = { Text(">>> USER_AUTHORIZATION", color = colors.primary, fontFamily = FontFamily.Monospace) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "PERSISTENT SESSION REQUIRED", 
                    color = colors.dimText, 
                    fontSize = 12.sp, 
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // The "Sign in with Google" branded button
                Surface(
                    onClick = {
                        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(clientId)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Color(0xFF747775), RectangleShape),
                    color = Color.White,
                    shape = RectangleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        // Standard Google "G" icon would go here (using text/placeholder if icon missing)
                        Text(
                            "G ", 
                            color = Color.Blue, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            "Sign in with Google", 
                            color = Color(0xFF1F1F1F),
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = ">>> ERROR: $error",
                        color = colors.danger,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.dimText, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
