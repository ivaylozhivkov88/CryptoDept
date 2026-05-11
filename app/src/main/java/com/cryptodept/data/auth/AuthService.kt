package com.cryptodept.data.auth

import android.content.Context
import com.cryptodept.data.datastore.PreferencesService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesService: PreferencesService
) {
    private val auth = FirebaseAuth.getInstance()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            updateAdminStatus(firebaseAuth.currentUser)
        }
    }

    private fun updateAdminStatus(user: FirebaseUser?) {
        if (user != null) {
            // Authorized admin emails for CryptoDept Terminal
            val adminEmails = listOf(
                "kaiko.dept@gmail.com",
                "ivaylozhivkov14@gmail.com"
            )
            val isAdminEmail = adminEmails.contains(user.email)
            if (isAdminEmail) {
                // This will also trigger setProStatus(true) internally
                preferencesService.setAdminStatus(true)
            }
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            updateAdminStatus(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        preferencesService.setAdminStatus(false)
    }
}
