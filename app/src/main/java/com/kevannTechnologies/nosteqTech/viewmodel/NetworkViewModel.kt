package com.kevannTechnologies.nosteqTech.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevannTechnologies.nosteqTech.ApiConfig
import com.kevannTechnologies.nosteqTech.ZoneConfig
import com.kevannTechnologies.nosteqTech.data.api.*
import com.kevannTechnologies.nosteqTech.data.api.cache.FirestoreOnuCache
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// -------------------- NETWORK STATE --------------------

sealed class NetworkState {
    object Loading : NetworkState()
    data class Success(val onus: List<OnuDetail>) : NetworkState()
    data class Error(val message: String) : NetworkState()
}

// -------------------- VIEW MODEL --------------------

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    // -------------------- CORE STATE --------------------

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Loading)
    val networkState: StateFlow<NetworkState> = _networkState

    private val _onuStatuses = MutableStateFlow<Map<String, OnuStatus>>(emptyMap())
    val onuStatuses: StateFlow<Map<String, OnuStatus>> = _onuStatuses

    private val _onusByStatus =
        MutableStateFlow<Map<String, List<OnuDetail>>>(emptyMap())

    private val _displayedOnuCount = MutableStateFlow(100)
    val displayedOnuCount: StateFlow<Int> = _displayedOnuCount

    private val _searchQuery = MutableStateFlow("")
    private val _userRole = MutableStateFlow("")
    private val _userServiceArea = MutableStateFlow("")

    // -------------------- API & CACHE --------------------

    private val apiService =
        SmartOltApiService(ApiConfig.SUBDOMAIN, ApiConfig.API_KEY)

    private val firestoreCache = FirestoreOnuCache()
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("onus_cache", 0)

    // -------------------- INIT (KEY OPTIMIZATION) --------------------

    init {
        /**
         * Build ONU groups ONCE when:
         * - ONU list changes
         * - Status list changes
         */
        viewModelScope.launch {
            combine(_networkState, _onuStatuses) { state, statuses ->
                if (state !is NetworkState.Success) return@combine emptyMap()

                val grouped = state.onus.groupBy { onu ->
                    val status = statuses[onu.sn]?.status ?: "unknown"
                    Log.d("[v0] GroupBy", "ONU ${onu.sn}: status=$status (from map: ${statuses[onu.sn]?.status})")
                    status.lowercase()
                }
                Log.d("[v0] GroupBy", "Final groups: ${grouped.keys}")
                grouped
            }.collect {
                _onusByStatus.value = it
                Log.d("[v0] GroupBy", "Updated _onusByStatus with keys: ${it.keys}, sizes: ${it.mapValues { (_, v) -> v.size }}")
            }
        }
    }

    // -------------------- FILTERED FLOWS --------------------

    private fun filteredOnusFlow(statusKey: String): StateFlow<List<OnuDetail>> =
        combine(
            _onusByStatus,
            _searchQuery,
            _userRole,
            _userServiceArea,
            _displayedOnuCount
        ) { grouped, search, role, area, count ->

            var list = grouped[statusKey] ?: emptyList()
            Log.d("[v0] FilterFlow($statusKey)", "Looking for key '$statusKey' in grouped keys: ${grouped.keys}")
            Log.d("[v0] FilterFlow($statusKey)", "Found ${list.size} ONUs for status '$statusKey'")

            // Technician zone filtering
            if (role.equals("technician", true) && area.isNotEmpty()) {
                list = list.filter {
                    isOnuInZone(it.zoneName ?: "", area)
                }
                Log.d("[v0] FilterFlow($statusKey)", "After zone filter: ${list.size} ONUs")
            }

            // Search filtering
            if (search.isNotBlank()) {
                list = list.filter {
                    it.name.contains(search, true) ||
                            it.sn.contains(search, true) ||
                            it.username?.startsWith(search, true) == true
                }
                Log.d("[v0] FilterFlow($statusKey)", "After search filter: ${list.size} ONUs")
            }

            val result = list.take(count)
            Log.d("[v0] FilterFlow($statusKey)", "Final result: ${result.size} ONUs")
            result
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val onlineOnu = filteredOnusFlow("online")
    val losOnu = filteredOnusFlow("los")
    val offlineOnu = filteredOnusFlow("offline")
    val powerFailOnu = filteredOnusFlow("power fail")

    // -------------------- ONU DETAILS --------------------

    private val _selectedOnuSignal = MutableStateFlow<OnuSignalInfo?>(null)
    val selectedOnuSignal: StateFlow<OnuSignalInfo?> = _selectedOnuSignal

    private val _gpsCoordinates = MutableStateFlow<Map<String, Pair<Double, Double>>>(emptyMap())
    val gpsCoordinates: StateFlow<Map<String, Pair<Double, Double>>> = _gpsCoordinates

    private val _selectedOnuSpeedProfile = MutableStateFlow<String?>(null)
    val selectedOnuSpeedProfile: StateFlow<String?> = _selectedOnuSpeedProfile

    private val _speedTestResult = MutableStateFlow<SpeedTestResult?>(null)
    val speedTestResult: StateFlow<SpeedTestResult?> = _speedTestResult

    // -------------------- DATA FETCHING --------------------

    fun fetchAllOnus(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val cachedJson = sharedPreferences.getString("onus_list", null)

                if (!forceRefresh && cachedJson != null) {
                    val cached = parseOnuListFromJson(cachedJson)
                    if (cached.isNotEmpty()) {
                        _networkState.value = NetworkState.Success(cached)
                        Log.d("[v0]", "Loaded ${cached.size} ONUs from cache")
                    }
                }

                val fresh = firestoreCache.getOnusFromCache()
                if (!fresh.isNullOrEmpty()) {
                    sharedPreferences.edit()
                        .putString("onus_list", convertOnuListToJson(fresh))
                        .apply()

                    _networkState.value = NetworkState.Success(fresh)
                    Log.d("[v0]", "Loaded ${fresh.size} ONUs from Firestore")
                }
            } catch (e: Exception) {
                _networkState.value =
                    NetworkState.Error(e.message ?: "Failed to load ONUs")
            }
        }
    }

    fun fetchOnuStatuses() {
        viewModelScope.launch {
            try {
                val response = apiService.getOnuStatuses(null, null, null, null)
                if (response.status && response.response != null) {
                    _onuStatuses.value =
                        response.response.associate {
                            it.sn to it.copy(status = it.status.lowercase())
                        }
                    Log.d("[v0]", "Fetched ONU statuses")
                }
            } catch (e: Exception) {
                Log.e("[v0]", "Status fetch failed: ${e.message}")
            }
        }
    }

    // -------------------- HELPERS --------------------

    fun loadMoreOnu() {
        _displayedOnuCount.value += 100
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setUserRole(role: String) {
        _userRole.value = role
    }

    fun setUserServiceArea(area: String) {
        _userServiceArea.value = area
    }

    fun getOnuById(sn: String): OnuDetail? {
        val state = _networkState.value
        return if (state is NetworkState.Success) {
            state.onus.find { it.sn == sn }
        } else {
            null
        }
    }

    fun fetchOnuFullStatus(uniqueExternalId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getOnuFullStatusInfo(uniqueExternalId)
                if (response != null) {
                    Log.d("[v0]", "Fetched ONU full status for $uniqueExternalId")
                }
            } catch (e: Exception) {
                Log.e("[v0]", "Failed to fetch ONU full status: ${e.message}")
            }
        }
    }

    fun fetchOnuSignal(uniqueExternalId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getOnuSignal(uniqueExternalId)
                if (response != null) {
                    _selectedOnuSignal.value = response
                    Log.d("[v0]", "Fetched ONU signal for $uniqueExternalId")
                }
            } catch (e: Exception) {
                Log.e("[v0]", "Failed to fetch ONU signal: ${e.message}")
            }
        }
    }

    fun fetchOnuSpeedProfile(uniqueExternalId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getOnuSpeedProfiles(uniqueExternalId)
                if (response != null) {
                    _selectedOnuSpeedProfile.value = response.toString()
                    Log.d("[v0]", "Fetched ONU speed profile for $uniqueExternalId")
                }
            } catch (e: Exception) {
                Log.e("[v0]", "Failed to fetch ONU speed profile: ${e.message}")
            }
        }
    }



    fun fetchLiveOnuStatus(uniqueExternalId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getOnuStatus(uniqueExternalId)
                if (response != null) {
                    Log.d("[v0]", "Fetched live ONU status for $uniqueExternalId")
                }
            } catch (e: Exception) {
                Log.e("[v0]", "Failed to fetch live ONU status: ${e.message}")
            }
        }
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            try {
                _speedTestResult.value = SpeedTestResult(
                    downloadSpeedMbps = null,
                    uploadSpeedMbps = null,
                    isLoading = true
                )
                val response = apiService.runSpeedTest()
                if (response != null) {
                    _speedTestResult.value = response
                    Log.d("[v0]", "Speed test completed")
                }
            } catch (e: Exception) {
                _speedTestResult.value = SpeedTestResult(
                    downloadSpeedMbps = null,
                    uploadSpeedMbps = null,
                    error = e.message ?: "Speed test failed"
                )
                Log.e("[v0]", "Speed test failed: ${e.message}")
            }
        }
    }

    fun clearSpeedTestResult() {
        _speedTestResult.value = null
    }

    private fun isOnuInZone(onuZone: String, userArea: String): Boolean {
        return ZoneConfig.isOnuInZone(onuZone, userArea)
    }

    // -------------------- CACHE SERIALIZATION --------------------

    private fun convertOnuListToJson(onus: List<OnuDetail>): String {
        val array = JSONArray()
        onus.forEach {
            array.put(JSONObject().apply {
                put("sn", it.sn)
                put("name", it.name)
                put("zoneName", it.zoneName)
                put("model", it.onuTypeName)
            })
        }
        return array.toString()
    }

    private fun parseOnuListFromJson(json: String): List<OnuDetail> {
        val list = mutableListOf<OnuDetail>()
        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                OnuDetail(
                    uniqueExternalId = o.optString("uniqueExternalId", null),
                    sn = o.optString("sn"),
                    name = o.optString("name"),
                    oltId = o.optString("oltId", null),
                    oltName = o.optString("oltName", null),
                    board = o.optString("board", null),
                    port = o.optString("port", null),
                    onu = o.optString("onu", null),
                    onuTypeId = o.optString("onuTypeId", null),
                    onuTypeName = o.optString("model", null),
                    zoneId = o.optString("zoneId", null),
                    zoneName = o.optString("zoneName", null),
                    address = o.optString("address", null),
                    odbName = o.optString("odbName", null),
                    mode = o.optString("mode", null),
                    wanMode = o.optString("wanMode", null),
                    ipAddress = o.optString("ipAddress", null),
                    subnetMask = o.optString("subnetMask", null),
                    defaultGateway = o.optString("defaultGateway", null),
                    dns1 = o.optString("dns1", null),
                    dns2 = o.optString("dns2", null),
                    username = o.optString("username", null),
                    password = o.optString("password", null),
                    catv = o.optString("catv", null),
                    administrativeStatus = o.optString("administrativeStatus", null),
                    servicePorts = null,
                    phoneNumber = o.optString("phoneNumber", null),
                    status = o.optString("status", "Unknown")
                )
            )
        }
        return list
    }
}
