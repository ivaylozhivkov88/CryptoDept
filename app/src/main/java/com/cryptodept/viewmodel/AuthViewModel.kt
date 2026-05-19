package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.auth.AuthService
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val subscription: SubscriptionAccessManager
) : ViewModel() {
    
    val currentUser: StateFlow<FirebaseUser?> = authService.currentUser

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            authService.signInWithGoogle(idToken)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "LOGIN_FAILED") }
        }
    }

    fun signOut() {
        authService.signOut()
    }

    fun setAdminStatus(isAdmin: Boolean) {
        subscription.setAdminStatus(isAdmin)
    }
}
