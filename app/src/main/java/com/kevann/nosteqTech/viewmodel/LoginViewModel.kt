package com.kevann.nosteqTech.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class LoginViewModel(private val context: Context? = null) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val sessionTimeout = 3 * 60 * 60 * 1000L // 3 hours in milliseconds
    val prefs: SharedPreferences? = context?.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun login(email: String, pass: String) {
        _loginState.value = LoginState.Loading

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                saveSessionTimestamp()
                saveLoginCredentials(email, pass)
                _loginState.value = LoginState.Success
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
    }

    private fun saveSessionTimestamp() {
        prefs?.edit()?.apply {
            putLong("session_timestamp", System.currentTimeMillis())
            apply()
        }
    }

    fun isSessionValid(): Boolean {
        val lastSessionTime = prefs?.getLong("session_timestamp", 0L) ?: 0L
        if (lastSessionTime == 0L) return false

        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastSessionTime
        return elapsed < sessionTimeout
    }

    fun logout() {
        auth.signOut()
        prefs?.edit()?.apply {
            remove("session_timestamp")
            remove("user_email")
            remove("user_password")
            apply()
        }
        _loginState.value = LoginState.Idle
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    fun saveLoginCredentials(email: String, password: String) {
        prefs?.edit()?.apply {
            putString("user_email", email)
            putString("user_password", password)
            apply()
        }
    }

    fun restoreSession() {
        val email = prefs?.getString("user_email", null)
        val password = prefs?.getString("user_password", null)

        if (email != null && password != null && isSessionValid()) {
            _loginState.value = LoginState.Loading
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    _loginState.value = LoginState.Success
                }
                .addOnFailureListener { e ->
                    _loginState.value = LoginState.Idle
                    Log.d("[v0] Session Restore", "Failed to restore session: ${e.message}")
                }
        } else {
            _loginState.value = LoginState.Idle
        }
    }
}
