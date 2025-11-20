package com.kevann.nosteqTech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevann.nosteqTech.ApiConfig
import com.kevann.nosteqTech.data.api.OnuDetail
import com.kevann.nosteqTech.data.api.SmartOltApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class NetworkState {
    object Loading : NetworkState()
    data class Success(val onus: List<OnuDetail>) : NetworkState()
    data class Error(val message: String) : NetworkState()
}

class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Loading)
    val networkState: StateFlow<NetworkState> = _networkState

    private val apiService = SmartOltApiService(ApiConfig.SUBDOMAIN, ApiConfig.API_KEY)
    private val cacheFile = File(application.cacheDir, "onus_cache.json")
    private val CACHE_EXPIRATION_MS = 60 * 60 * 1000 // 1 hour

    fun fetchAllOnus(
        oltId: Int? = null,
        board: Int? = null,
        port: Int? = null,
        zone: String? = null,
        odb: String? = null,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _networkState.value = NetworkState.Loading

            if (!forceRefresh && cacheFile.exists() && (System.currentTimeMillis() - cacheFile.lastModified() < CACHE_EXPIRATION_MS)) {
                try {
                    val cachedJson = cacheFile.readText()
                    val response = apiService.parseOnusJson(cachedJson)
                    if (response.status && response.onus != null) {
                        _networkState.value = NetworkState.Success(response.onus)
                        return@launch
                    }
                } catch (e: Exception) {
                    // If cache read fails, proceed to fetch
                }
            }

            try {
                val jsonString = apiService.fetchRawOnusJson(
                    oltId = oltId,
                    board = board,
                    port = port,
                    zone = zone,
                    odb = odb
                )

                // Save to cache
                cacheFile.writeText(jsonString)

                val response = apiService.parseOnusJson(jsonString)

                _networkState.value = if (response.status && response.onus != null) {
                    NetworkState.Success(response.onus)
                } else {
                    NetworkState.Error(response.error ?: "Failed to fetch ONUs")
                }
            } catch (e: Exception) {
                _networkState.value = NetworkState.Error(e.message ?: "Network error")
            }
        }
    }

    fun getOnuById(sn: String): OnuDetail? {
        val currentState = _networkState.value
        return if (currentState is NetworkState.Success) {
            currentState.onus.find { it.sn == sn }
        } else null
    }
}
