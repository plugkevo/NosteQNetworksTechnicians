package com.kevannTechnologies.nosteqTech.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.kevannTechnologies.nosteqTech.ApiConfig
import com.kevannTechnologies.nosteqTech.data.api.OnuDetail
import com.kevannTechnologies.nosteqTech.data.api.OnuFullStatus
import com.kevannTechnologies.nosteqTech.data.api.OnuSignalInfo
import com.kevannTechnologies.nosteqTech.data.api.OnuSpeedProfile
import com.kevannTechnologies.nosteqTech.data.api.OnuStatus
import com.kevannTechnologies.nosteqTech.data.api.SmartOltApiService
import com.kevannTechnologies.nosteqTech.data.api.SpeedTestResult
import com.kevannTechnologies.nosteqTech.data.api.cache.FirestoreOnuCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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

    private val _onuStatuses = MutableStateFlow<Map<String, OnuStatus>>(emptyMap())
    val onuStatuses: StateFlow<Map<String, OnuStatus>> = _onuStatuses

    private val _liveOnuStatus = MutableStateFlow<OnuStatus?>(null)
    val liveOnuStatus: StateFlow<OnuStatus?> = _liveOnuStatus

    private val _onusWithStatus = MutableStateFlow<List<OnuDetail>>(emptyList())
    val onusWithStatus: StateFlow<List<OnuDetail>> = _onusWithStatus

    private val _displayedOnuCount = MutableStateFlow(30)
    val displayedOnuCount: StateFlow<Int> = _displayedOnuCount

    private val apiService = SmartOltApiService(ApiConfig.SUBDOMAIN, ApiConfig.API_KEY)
    private val cacheFile = File(application.cacheDir, "onus_cache.json")
    private val CACHE_EXPIRATION_MS = 60 * 60 * 1000

    private val gpsCacheFile = File(application.cacheDir, "gps_cache.json")
    private val GPS_CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000

    private val firestoreCache = FirestoreOnuCache()
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("onus_cache", 0)

    fun fetchAllOnus(
        oltId: Int? = null,
        board: Int? = null,
        port: Int? = null,
        zone: String? = null,
        odb: String? = null,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                // Load cached ONUs from SharedPreferences immediately
                val cachedJson = sharedPreferences.getString("onus_list", null)
                if (cachedJson != null && !forceRefresh) {
                    try {
                        val cachedOnuList = parseOnuListFromJson(cachedJson)
                        if (cachedOnuList.isNotEmpty()) {
                            _networkState.value = NetworkState.Success(cachedOnuList)
                            Log.d("[v0]", "Displaying ${cachedOnuList.size} ONUs from persistent cache")
                        }
                    } catch (e: Exception) {
                        Log.e("[v0]", "Failed to parse cached ONUs: ${e.message}")
                    }
                }

                // Fetch fresh data from Firestore in background
                val freshOnus = firestoreCache.getOnusFromCache()
                if (freshOnus != null && freshOnus.isNotEmpty()) {
                    Log.d("[v0]", "Fetched ${freshOnus.size} ONUs from Firestore")

                    // Save to persistent cache (SharedPreferences)
                    val jsonString = convertOnuListToJson(freshOnus)
                    sharedPreferences.edit().putString("onus_list", jsonString).apply()
                    Log.d("[v0]", "Updated persistent cache with fresh ONUs")

                    // Update UI with fresh data
                    _networkState.value = NetworkState.Success(freshOnus)
                } else {
                    Log.d("[v0]", "No fresh ONUs fetched from Firestore")
                }
            } catch (e: Exception) {
                Log.e("[v0]", "Error fetching ONUs: ${e.message}")
                _networkState.value = NetworkState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun convertOnuListToJson(onus: List<OnuDetail>): String {
        val jsonArray = JSONArray()
        onus.forEach { onu ->
            val obj = JSONObject()
            obj.put("uniqueExternalId", onu.uniqueExternalId ?: "")
            obj.put("sn", onu.sn)
            obj.put("name", onu.name)
            obj.put("model", onu.onuTypeName ?: "")
            obj.put("zoneName", onu.zoneName ?: "")
            obj.put("rxPower", onu.rxPower ?: 0.0)
            obj.put("txPower", onu.txPower ?: 0.0)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun parseOnuListFromJson(jsonString: String): List<OnuDetail> {
        val onus = mutableListOf<OnuDetail>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val onu = OnuDetail(
                uniqueExternalId = obj.optString("uniqueExternalId", ""),
                sn = obj.optString("sn", ""),
                name = obj.optString("name", ""),
                oltId = null,
                oltName = null,
                board = null,
                port = null,
                onu = null,
                onuTypeId = null,
                onuTypeName = obj.optString("model", ""),
                zoneId = null,
                zoneName = obj.optString("zoneName", ""),
                address = null,
                odbName = null,
                mode = null,
                wanMode = null,
                ipAddress = null,
                subnetMask = null,
                defaultGateway = null,
                dns1 = null,
                dns2 = null,
                username = null,
                password = null,
                catv = null,
                administrativeStatus = null,
                servicePorts = null,
                phoneNumber = null,
                status = "Unknown",
                rxPower = obj.optDouble("rxPower", 0.0),
                txPower = obj.optDouble("txPower", 0.0)
            )
            onus.add(onu)
        }
        return onus
    }

    fun fetchGpsCoordinates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && gpsCacheFile.exists() &&
                (System.currentTimeMillis() - gpsCacheFile.lastModified() < GPS_CACHE_EXPIRATION_MS)) {
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
                    Log.e("[v0]", "GPS cache error: ${e.message}")
                }
            }

            val response = apiService.getAllOnusGpsCoordinates()
            if (response.status && response.onus != null) {
                _gpsCoordinates.value = response.onus.associate {
                    it.uniqueExternalId to Pair(it.latitude, it.longitude)
                }

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
                    Log.e("[v0]", "Failed to cache GPS: ${e.message}")
                }
            }
        }
    }

    fun fetchOnuFullStatus(uniqueExternalId: String) {
        viewModelScope.launch {
            _selectedOnuStatus.value = null
            val status = apiService.getOnuFullStatusInfo(uniqueExternalId)
            _selectedOnuStatus.value = status
        }
    }

    fun fetchOnuSignal(uniqueExternalId: String) {
        viewModelScope.launch {
            _selectedOnuSignal.value = null
            val signal = apiService.getOnuSignal(uniqueExternalId)
            _selectedOnuSignal.value = signal
        }
    }

    fun fetchOnuSpeedProfile(uniqueExternalId: String) {
        viewModelScope.launch {
            _selectedOnuSpeedProfile.value = null
            val speedProfile = apiService.getOnuSpeedProfiles(uniqueExternalId)
            _selectedOnuSpeedProfile.value = speedProfile
            if (speedProfile != null) {
                Log.d("[v0]", "Speed Profile - Download: ${speedProfile.downloadProfileName}, " +
                        "Upload: ${speedProfile.uploadProfileName}")
            }
        }
    }

    fun getOnuById(sn: String): OnuDetail? {
        return if (_networkState.value is NetworkState.Success) {
            (_networkState.value as NetworkState.Success).onus.find { it.sn == sn }
        } else {
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

            Log.d("[v0] Speed Test", "Download: ${result.downloadSpeedMbps?.let {
                "%.2f Mbps".format(it) } ?: "N/A"}")
        }
    }

    fun clearSpeedTestResult() {
        _speedTestResult.value = null
    }

    fun fetchOnuStatuses(
        oltId: Int? = null,
        board: Int? = null,
        port: Int? = null,
        zone: String? = null
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.getOnuStatuses(oltId, board, port, zone)
                if (response.status && response.response != null) {
                    val statusMap = response.response.associate { it.sn to it }
                    _onuStatuses.value = statusMap
                    Log.d("[v0]", "Fetched ${statusMap.size} real-time statuses")
                }
            } catch (e: Exception) {
                Log.e("[v0]", "Failed to fetch statuses: ${e.message}")
            }
        }
    }

    fun getOnuStatus(sn: String): OnuStatus? {
        return _onuStatuses.value[sn]
    }

    fun getOnuStatusString(sn: String): String? {
        return _onuStatuses.value[sn]?.status
    }

    fun fetchLiveOnuStatus(uniqueExternalId: String) {
        viewModelScope.launch {
            val status = apiService.getOnuStatus(uniqueExternalId)
            _liveOnuStatus.value = status
        }
    }

    fun loadMoreOnu() {
        _displayedOnuCount.value += 30
    }
}
