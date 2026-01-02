package com.kevann.nosteqTech.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevann.nosteqTech.ApiConfig
import com.kevann.nosteqTech.data.api.OnuDetail
import com.kevann.nosteqTech.data.api.OnuFullStatus
import com.kevann.nosteqTech.data.api.OnuSignalInfo // Import new class
import com.kevann.nosteqTech.data.api.OnuSpeedProfile // Import new class
import com.kevann.nosteqTech.data.api.SmartOltApiService
import com.kevann.nosteqTech.data.api.SpeedTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray // Import JSONArray
import org.json.JSONObject // Import JSONObject
import java.io.File



sealed class NetworkState {
    object Loading : NetworkState()
    data class Success(val onus: List<OnuDetail>) : NetworkState()
    data class Error(val message: String) : NetworkState()
}

class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Loading)
    val networkState: StateFlow<NetworkState> = _networkState

    private val _selectedOnuStatus = MutableStateFlow<OnuFullStatus?>(null)
    val selectedOnuStatus: StateFlow<OnuFullStatus?> = _selectedOnuStatus

    private val _selectedOnuSignal = MutableStateFlow<OnuSignalInfo?>(null)
    val selectedOnuSignal: StateFlow<OnuSignalInfo?> = _selectedOnuSignal

    private val _selectedOnuSpeedProfile = MutableStateFlow<OnuSpeedProfile?>(null)
    val selectedOnuSpeedProfile: StateFlow<OnuSpeedProfile?> = _selectedOnuSpeedProfile

    private val _gpsCoordinates = MutableStateFlow<Map<String, Pair<Double, Double>>>(emptyMap())
    val gpsCoordinates: StateFlow<Map<String, Pair<Double, Double>>> = _gpsCoordinates

    private val _speedTestResult = MutableStateFlow<SpeedTestResult?>(null)
    val speedTestResult: StateFlow<SpeedTestResult?> = _speedTestResult

    private val apiService = SmartOltApiService(ApiConfig.SUBDOMAIN, ApiConfig.API_KEY)
    private val cacheFile = File(application.cacheDir, "onus_cache.json")
    private val CACHE_EXPIRATION_MS = 60 * 60 * 1000 // 1 hour

    private val gpsCacheFile = File(application.cacheDir, "gps_cache.json")
    private val GPS_CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000 // 24 hours

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

    fun fetchGpsCoordinates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && gpsCacheFile.exists() && (System.currentTimeMillis() - gpsCacheFile.lastModified() < GPS_CACHE_EXPIRATION_MS)) {
                try {
                    val cachedJson = gpsCacheFile.readText()
                    val jsonResponse = JSONObject(cachedJson)
                    if (jsonResponse.optBoolean("status")) {
                        val onusArray = jsonResponse.optJSONArray("onus") ?: JSONArray()
                        val gpsMap = mutableMapOf<String, Pair<Double, Double>>()
                        for (i in 0 until onusArray.length()) {
                            val item = onusArray.getJSONObject(i)
                            val lat = item.optDouble("latitude")
                            val long = item.optDouble("longitude")
                            val id = item.optString("unique_external_id")
                            if (!lat.isNaN() && !long.isNaN() && id.isNotEmpty()) {
                                gpsMap[id] = Pair(lat, long)
                            }
                        }
                        _gpsCoordinates.value = gpsMap
                        return@launch
                    }
                } catch (e: Exception) {
                    // Ignore cache error
                }
            }

            val response = apiService.getAllOnusGpsCoordinates()
            if (response.status && response.onus != null) {
                // Update state
                _gpsCoordinates.value = response.onus.associate { it.uniqueExternalId to Pair(it.latitude, it.longitude) }

                // Save to cache (reconstruct simple JSON)
                try {
                    val jsonRoot = JSONObject()
                    jsonRoot.put("status", true)
                    val jsonArray = JSONArray()
                    response.onus.forEach { gps ->
                        val item = JSONObject()
                        item.put("unique_external_id", gps.uniqueExternalId)
                        item.put("latitude", gps.latitude)
                        item.put("longitude", gps.longitude)
                        jsonArray.put(item)
                    }
                    jsonRoot.put("onus", jsonArray)
                    gpsCacheFile.writeText(jsonRoot.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun fetchOnuFullStatus(uniqueExternalId: String) {
        viewModelScope.launch {
            _selectedOnuStatus.value = null // Reset previous status
            val status = apiService.getOnuFullStatusInfo(uniqueExternalId)
            _selectedOnuStatus.value = status
        }
    }

    fun fetchOnuSignal(uniqueExternalId: String) {
        viewModelScope.launch {
            _selectedOnuSignal.value = null // Reset previous signal
            val signal = apiService.getOnuSignal(uniqueExternalId)
            _selectedOnuSignal.value = signal
        }
    }

    fun fetchOnuSpeedProfile(uniqueExternalId: String) {
        viewModelScope.launch {
            _selectedOnuSpeedProfile.value = null // Reset previous profile
            val speedProfile = apiService.getOnuSpeedProfiles(uniqueExternalId)
            _selectedOnuSpeedProfile.value = speedProfile
            if (speedProfile != null) {
                Log.d("[v0]", "Speed Profile for $uniqueExternalId - Download: ${speedProfile.downloadProfileName}, Upload: ${speedProfile.uploadProfileName}")
            } else {
                Log.d("[v0]", "Speed Profile not available for $uniqueExternalId")
            }
        }
    }

    fun getOnuById(sn: String): OnuDetail? {
        val currentState = _networkState.value
        println("[v0] getOnuById - Looking for SN: $sn")
        println("[v0] getOnuById - Current state: $currentState")

        return if (currentState is NetworkState.Success) {
            println("[v0] getOnuById - Available ONUs count: ${currentState.onus.size}")
            currentState.onus.forEachIndexed { index, onu ->
                println("[v0] getOnuById - ONU[$index]: sn=${onu.sn}, name=${onu.name}")
            }
            val found = currentState.onus.find { it.sn == sn }
            println("[v0] getOnuById - Found: ${found != null}")
            found
        } else {
            println("[v0] getOnuById - State is not Success, returning null")
            null
        }
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            _speedTestResult.value = SpeedTestResult(
                downloadSpeedMbps = null,
                uploadSpeedMbps = null,
                isLoading = true
            )

            val result = apiService.runSpeedTest()
            _speedTestResult.value = result

            Log.d("[v0] Speed Test", "Download Speed: ${result.downloadSpeedMbps?.let { "%.2f Mbps".format(it) } ?: "N/A"}")
            if (result.error != null) {
                Log.e("[v0] Speed Test", "Error: ${result.error}")
            }
        }
    }

    fun clearSpeedTestResult() {
        _speedTestResult.value = null
        Log.d("[v0] Speed Test", "Speed test result cleared")
    }
}
