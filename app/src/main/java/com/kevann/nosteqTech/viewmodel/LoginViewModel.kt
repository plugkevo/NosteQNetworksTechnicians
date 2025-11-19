package com.kevann.nosteqTech.viewmodel



import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(email: String, pass: String) {
        _loginState.value = LoginState.Loading

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _loginState.value = LoginState.Success
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
