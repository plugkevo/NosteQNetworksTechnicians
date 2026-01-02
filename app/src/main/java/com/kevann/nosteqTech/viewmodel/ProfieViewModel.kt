package com.kevann.nosteqTech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kevann.nosteqTech.data.api.OnuDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TechnicianProfile(
    val id: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val serviceArea: String = "",
    val name: String = ""
)

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object Success : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _profileData = MutableStateFlow<TechnicianProfile?>(null)
    val profileData: StateFlow<TechnicianProfile?> = _profileData.asStateFlow()

    private val _updatePhoneState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val updatePhoneState: StateFlow<ProfileState> = _updatePhoneState.asStateFlow()

    private val _updatePasswordState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val updatePasswordState: StateFlow<ProfileState> = _updatePasswordState.asStateFlow()

    private val _onusManagedCount = MutableStateFlow<Int>(0)
    val onusManagedCount: StateFlow<Int> = _onusManagedCount.asStateFlow()

    fun fetchUserProfile() {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val doc = db.collection("technicians").document(currentUser.uid).get().await()
                    if (doc.exists()) {
                        val profile = TechnicianProfile(
                            id = doc.getString("id") ?: currentUser.uid,
                            email = doc.getString("email") ?: currentUser.email ?: "",
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            serviceArea = doc.getString("serviceArea") ?: "",
                            name = doc.getString("name") ?: ""
                        )
                        _profileData.value = profile
                        _profileState.value = ProfileState.Success
                        println("[v0] Profile Fetched: $profile")
                    } else {
                        _profileState.value = ProfileState.Error("Profile not found")
                    }
                } else {
                    _profileState.value = ProfileState.Error("No user logged in")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to fetch profile")
                println("[v0] Error fetching profile: ${e.message}")
            }
        }
    }

    fun updatePhoneNumber(newPhoneNumber: String) {
        _updatePhoneState.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    db.collection("technicians").document(currentUser.uid)
                        .update("phoneNumber", newPhoneNumber).await()

                    _profileData.value = _profileData.value?.copy(phoneNumber = newPhoneNumber)
                    _updatePhoneState.value = ProfileState.Success
                    println("[v0] Phone updated successfully")
                } else {
                    _updatePhoneState.value = ProfileState.Error("No user logged in")
                }
            } catch (e: Exception) {
                _updatePhoneState.value = ProfileState.Error(e.message ?: "Failed to update phone")
                println("[v0] Error updating phone: ${e.message}")
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        _updatePasswordState.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                // Validate passwords
                if (newPassword != confirmPassword) {
                    _updatePasswordState.value = ProfileState.Error("Passwords don't match")
                    return@launch
                }
                if (newPassword.length < 6) {
                    _updatePasswordState.value = ProfileState.Error("Password must be at least 6 characters")
                    return@launch
                }

                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.email != null) {
                    // Re-authenticate before changing password
                    auth.signInWithEmailAndPassword(currentUser.email!!, currentPassword)
                        .addOnSuccessListener {
                            currentUser.updatePassword(newPassword)
                                .addOnSuccessListener {
                                    _updatePasswordState.value = ProfileState.Success
                                    println("[v0] Password changed successfully")
                                }
                                .addOnFailureListener { e ->
                                    _updatePasswordState.value = ProfileState.Error(e.message ?: "Failed to change password")
                                    println("[v0] Error changing password: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            _updatePasswordState.value = ProfileState.Error("Current password is incorrect")
                            println("[v0] Re-auth failed: ${e.message}")
                        }
                } else {
                    _updatePasswordState.value = ProfileState.Error("No user logged in")
                }
            } catch (e: Exception) {
                _updatePasswordState.value = ProfileState.Error(e.message ?: "Failed to change password")
                println("[v0] Error changing password: ${e.message}")
            }
        }
    }

    fun resetUpdatePhoneState() {
        _updatePhoneState.value = ProfileState.Idle
    }

    fun resetUpdatePasswordState() {
        _updatePasswordState.value = ProfileState.Idle
    }

    fun calculateManagedOnuCount(allOnus: List<OnuDetail>, technicianServiceArea: String) {
        val count = allOnus.count { onu ->
            onu.zoneName.equals(technicianServiceArea, ignoreCase = true)
        }
        _onusManagedCount.value = count
        println("[v0] Profile - Managed ONUs count: $count for service area: $technicianServiceArea")
    }
}

data class OnuDetail(
    val zone: String = ""
)
