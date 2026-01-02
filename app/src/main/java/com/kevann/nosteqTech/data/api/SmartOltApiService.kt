package com.kevann.nosteqTech.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log



data class OnuDetailsResponse(
    val status: Boolean,
    val onus: List<OnuDetail>? = null,
    val error: String? = null
)

data class ServicePort(
    val id: Int,
    val name: String
    // Add other fields if necessary based on API response
)

data class OnuDetail(
    val uniqueExternalId: String?,
    val sn: String,
    val name: String,
    val oltId: String?,
    val oltName: String?,
    val board: String?,
    val port: String?,
    val onu: String?,
    val onuTypeId: String?,
    val onuTypeName: String?,
    val zoneId: String?,
    val zoneName: String?,
    val address: String?,
    val odbName: String?,
    val mode: String?,
    val wanMode: String?,
    val ipAddress: String?,
    val subnetMask: String?,
    val defaultGateway: String?,
    val dns1: String?,
    val dns2: String?,
    val username: String?,
    val password: String?,
    val catv: String?,
    val administrativeStatus: String?,
    val servicePorts: List<ServicePort>?,

    val phoneNumber: String?,

    // UI Helper fields (mapped manually or from optional JSON fields)
    val status: String = "Unknown", // Mapped from status or administrative_status
    val rxPower: Double? = null,
    val txPower: Double? = null,
    val lastSeen: String = "Unknown",
    val distance: Int? = null,
    val model: String? = null
)

data class OnuFullStatus(
    val rawText: String,
    val rxPower: Double? = null,
    val txPower: Double? = null,
    val distance: Int? = null,
    val runState: String? = null,
    val lastDownCause: String? = null,
    val lastUpTime: String? = null,
    val ipAddress: String? = null
)

data class OnuSignalInfo(
    val signalQuality: String, // "Critical", "Warning", "Very good"
    val signalValue: String, // Combined value like "-10.39 dBm / -10.74 dBm"
    val signal1310: String?, // 1310nm wavelength
    val signal1490: String? // 1490nm wavelength
)

data class OnuGpsCoordinates(
    val uniqueExternalId: String,
    val latitude: Double,
    val longitude: Double
)

data class OnuGpsResponse(
    val status: Boolean,
    val onus: List<OnuGpsCoordinates>? = null,
    val error: String? = null
)

data class OnuSpeedProfile(
    val uploadProfileName: String?,
    val downloadProfileName: String?
)

data class OnuSpeedProfileResponse(
    val status: Boolean,
    val uploadSpeedProfileName: String? = null,
    val downloadSpeedProfileName: String? = null,
    val error: String? = null
)

data class SpeedTestResult(
    val downloadSpeedMbps: Double?,
    val uploadSpeedMbps: Double?,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SmartOltApiService(
    private val subdomain: String,
    private val apiKey: String
) {
    private val baseUrl = "https://$subdomain.smartolt.com/api"

    suspend fun getOnuSignal(uniqueExternalId: String): OnuSignalInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/onu/get_onu_signal/$uniqueExternalId")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("X-Token", apiKey)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseString = inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)

            if (jsonResponse.optBoolean("status")) {
                OnuSignalInfo(
                    signalQuality = jsonResponse.optString("onu_signal", "Unknown"),
                    signalValue = jsonResponse.optString("onu_signal_value", "N/A"),
                    signal1310 = jsonResponse.optString("onu_signal_1310").ifEmpty { null },
                    signal1490 = jsonResponse.optString("onu_signal_1490").ifEmpty { null }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAllOnusGpsCoordinates(
        oltId: Int? = null,
        board: Int? = null,
        port: Int? = null,
        zone: String? = null
    ): OnuGpsResponse = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder("$baseUrl/onu/get_all_onus_gps_coordinates")
            val params = mutableListOf<String>()

            oltId?.let { params.add("olt_id=$it") }
            board?.let { params.add("board=$it") }
            port?.let { params.add("port=$it") }
            zone?.let { params.add("zone=${java.net.URLEncoder.encode(it, "UTF-8")}") }

            if (params.isNotEmpty()) {
                urlBuilder.append("?${params.joinToString("&")}")
            }

            val url = URL(urlBuilder.toString())
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("X-Token", apiKey)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseString = inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)

            if (jsonResponse.optBoolean("status")) {
                val onusArray = jsonResponse.optJSONArray("onus") ?: JSONArray()
                val gpsList = mutableListOf<OnuGpsCoordinates>()

                for (i in 0 until onusArray.length()) {
                    val item = onusArray.getJSONObject(i)
                    val lat = item.optDouble("latitude")
                    val long = item.optDouble("longitude")
                    val id = item.optString("unique_external_id")

                    if (!lat.isNaN() && !long.isNaN() && id.isNotEmpty()) {
                        gpsList.add(OnuGpsCoordinates(id, lat, long))
                    }
                }
                OnuGpsResponse(status = true, onus = gpsList)
            } else {
                OnuGpsResponse(status = false, error = "Failed to fetch GPS coordinates")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OnuGpsResponse(status = false, error = e.message ?: "Network error")
        }
    }

    suspend fun getOnuFullStatusInfo(uniqueExternalId: String): OnuFullStatus? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/onu/get_onu_full_status_info/$uniqueExternalId")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST" // Documentation says POST
                setRequestProperty("X-Token", apiKey)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30000
                readTimeout = 30000
                doOutput = true // Triggers POST
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseString = inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)

            if (jsonResponse.optBoolean("status")) {
                val fullInfo = jsonResponse.optString("full_status_info", "")
                parseFullStatusInfo(fullInfo)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseFullStatusInfo(info: String): OnuFullStatus {
        // Helper regex to find values after ":"
        fun getValue(key: String): String? {
            val regex = Regex("$key\\s*:\\s*(.+)", RegexOption.IGNORE_CASE) // Added IGNORE_CASE for better matching
            return regex.find(info)?.groupValues?.get(1)?.trim()
        }

        val rxStr = getValue("Rx optical power")
        val txStr = getValue("Tx optical power")
        val distStr = getValue("ONT distance")

        // Clean numeric strings (remove units if any, though regex handles most)
        val rx = rxStr?.replace(Regex("[^0-9.-]"), "")?.toDoubleOrNull()
        val tx = txStr?.replace(Regex("[^0-9.-]"), "")?.toDoubleOrNull()
        val dist = distStr?.replace(Regex("[^0-9]"), "")?.toIntOrNull()

        val ip = getValue("IP address")
            ?: getValue("WAN IP")
            ?: getValue("IPv4 address")
            ?: getValue("IP")

        return OnuFullStatus(
            rawText = info,
            rxPower = rx,
            txPower = tx,
            distance = dist,
            runState = getValue("Run state"),
            lastDownCause = getValue("Last down cause"),
            lastUpTime = getValue("Last up time"),
            ipAddress = ip // Set the parsed IP
        )
    }

    suspend fun fetchRawOnusJson(
        oltId: Int? = null,
        board: Int? = null,
        port: Int? = null,
        zone: String? = null,
        odb: String? = null
    ): String = withContext(Dispatchers.IO) {
        val urlBuilder = StringBuilder("$baseUrl/onu/get_all_onus_details")
        val params = mutableListOf<String>()

        oltId?.let { params.add("olt_id=$it") }
        board?.let { params.add("board=$it") }
        port?.let { params.add("port=$it") }
        zone?.let { params.add("zone=${java.net.URLEncoder.encode(it, "UTF-8")}") }
        odb?.let { params.add("odb=${java.net.URLEncoder.encode(it, "UTF-8")}") }

        if (params.isNotEmpty()) {
            urlBuilder.append("?${params.joinToString("&")}")
        }

        val url = URL(urlBuilder.toString())
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "GET"
            setRequestProperty("X-Token", apiKey)
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 30000
            readTimeout = 30000
        }

        val responseCode = connection.responseCode
        val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        inputStream.bufferedReader().use { it.readText() }
    }

    fun parseOnusJson(jsonString: String): OnuDetailsResponse {
        try {
            val jsonResponse = JSONObject(jsonString)

            if (!jsonResponse.optBoolean("status")) {
                return OnuDetailsResponse(
                    status = false,
                    error = jsonResponse.optString("error", "Unknown error")
                )
            }

            val onuArray = jsonResponse.optJSONArray("onus") ?: JSONArray()
            val onus = mutableListOf<OnuDetail>()

            for (i in 0 until onuArray.length()) {
                val onuJson = onuArray.getJSONObject(i)

                // Parse Service Ports
                val servicePortsList = mutableListOf<ServicePort>()
                val servicePortsArray = onuJson.optJSONArray("service_ports")
                if (servicePortsArray != null) {
                    for (j in 0 until servicePortsArray.length()) {
                        val spJson = servicePortsArray.getJSONObject(j)
                        servicePortsList.add(ServicePort(
                            id = spJson.optInt("id"),
                            name = spJson.optString("name")
                        ))
                    }
                }

                // Robust key checking for status
                val rawStatus = onuJson.optString("status")
                val adminStatus = onuJson.optString("administrative_status").ifEmpty { onuJson.optString("admin_status") }
                val finalStatus = if (rawStatus.isNotEmpty()) rawStatus else adminStatus.ifEmpty { "Unknown" }

                // Robust key checking for other fields
                val ipAddr = onuJson.optString("ip_address").ifEmpty {
                    onuJson.optString("ip").ifEmpty {
                        onuJson.optString("ipv4").ifEmpty {
                            onuJson.optString("wan_ip").ifEmpty { null }
                        }
                    }
                }

                val phone = onuJson.optString("phone_number").ifEmpty {
                    onuJson.optString("phone").ifEmpty {
                        onuJson.optString("customer_phone").ifEmpty {
                            onuJson.optString("mobile").ifEmpty {
                                onuJson.optString("contact_number").ifEmpty { null }
                            }
                        }
                    }
                }

                onus.add(
                    OnuDetail(
                        uniqueExternalId = onuJson.optString("unique_external_id").ifEmpty { onuJson.optString("external_id") }.ifEmpty { null },
                        sn = onuJson.optString("sn").ifEmpty { onuJson.optString("onu_sn", "Unknown") },
                        name = onuJson.optString("name").ifEmpty { onuJson.optString("onu_name", "Unknown") },
                        oltId = onuJson.optString("olt_id").ifEmpty { null },
                        oltName = onuJson.optString("olt_name").ifEmpty { null },
                        board = onuJson.optString("board").ifEmpty { null },
                        port = onuJson.optString("port").ifEmpty { null },
                        onu = onuJson.optString("onu").ifEmpty { null },
                        onuTypeId = onuJson.optString("onu_type_id").ifEmpty { null },
                        onuTypeName = onuJson.optString("onu_type_name").ifEmpty { null },
                        zoneId = onuJson.optString("zone_id").ifEmpty { null },
                        zoneName = onuJson.optString("zone_name").ifEmpty { onuJson.optString("zone", null) },
                        address = onuJson.optString("address").ifEmpty { null },
                        odbName = onuJson.optString("odb_name").ifEmpty { onuJson.optString("odb", null) },
                        mode = onuJson.optString("mode").ifEmpty { null },
                        wanMode = onuJson.optString("wan_mode").ifEmpty { null },
                        ipAddress = ipAddr,
                        subnetMask = onuJson.optString("subnet_mask").ifEmpty { null },
                        defaultGateway = onuJson.optString("default_gateway").ifEmpty { null },
                        dns1 = onuJson.optString("dns1").ifEmpty { null },
                        dns2 = onuJson.optString("dns2").ifEmpty { null },
                        username = onuJson.optString("username").ifEmpty { null },
                        password = onuJson.optString("password").ifEmpty { null },
                        catv = onuJson.optString("catv").ifEmpty { null },
                        administrativeStatus = adminStatus,
                        servicePorts = servicePortsList,
                        phoneNumber = phone,

                        // UI Fields
                        status = finalStatus,
                        rxPower = onuJson.optDouble("rx_power").takeIf { !it.isNaN() },
                        txPower = onuJson.optDouble("tx_power").takeIf { !it.isNaN() },
                        lastSeen = onuJson.optString("last_seen").ifEmpty {
                            onuJson.optString("last_online").ifEmpty {
                                onuJson.optString("last_connected", "Unknown")
                            }
                        },
                        distance = onuJson.optInt("distance").takeIf { it > 0 },
                        model = onuJson.optString("model").ifEmpty { onuJson.optString("onu_type_name", null) }
                    )
                )
            }

            return OnuDetailsResponse(status = true, onus = onus)

        } catch (e: Exception) {
            return OnuDetailsResponse(status = false, error = e.message ?: "Parsing error")
        }
    }

    suspend fun getAllOnusDetails(
        oltId: Int? = null,
        board: Int? = null,
        port: Int? = null,
        zone: String? = null,
        odb: String? = null
    ): OnuDetailsResponse = withContext(Dispatchers.IO) {
        try {
            val jsonString = fetchRawOnusJson(oltId, board, port, zone, odb)
            parseOnusJson(jsonString)
        } catch (e: Exception) {
            OnuDetailsResponse(status = false, error = e.message ?: "Network error")
        }
    }

    suspend fun getOnuSpeedProfiles(uniqueExternalId: String): OnuSpeedProfile? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/onu/get_onu_speed_profiles/$uniqueExternalId")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("X-Token", apiKey)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseString = inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)

            if (jsonResponse.optBoolean("status")) {
                OnuSpeedProfile(
                    uploadProfileName = jsonResponse.optString("upload_speed_profile_name").ifEmpty { null },
                    downloadProfileName = jsonResponse.optString("download_speed_profile_name").ifEmpty { null }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun runSpeedTest(): SpeedTestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val testUrls = listOf(
                "https://speed.cloudflare.com/__down?bytes=10000000", // Cloudflare - 10MB HTTPS
                "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js", // 10MB alternative
                "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png" // Google logo fallback
            )

            var downloadSpeedMbps: Double? = null

            for (testUrl in testUrls) {
                try {
                    val startTime = System.currentTimeMillis()
                    val url = URL(testUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 60000
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val fileSize = connection.contentLength.toLong() // bytes

                        // Download and measure speed
                        val inputStream = connection.inputStream
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            totalBytes += bytesRead
                        }
                        inputStream.close()

                        val endTime = System.currentTimeMillis()
                        val durationSeconds = (endTime - startTime) / 1000.0

                        if (durationSeconds > 0) {
                            val bytesPerSecond = totalBytes / durationSeconds
                            downloadSpeedMbps = (bytesPerSecond * 8) / 1_000_000 // Convert to Mbps
                            Log.d("[v0] Speed Test", "Download Speed: $downloadSpeedMbps Mbps")
                            break // Success, exit loop
                        }
                    }
                } catch (e: Exception) {
                    Log.e("[v0] Speed Test", "Failed with URL: $testUrl - ${e.message}")
                    // Try next URL
                    continue
                }
            }

            SpeedTestResult(
                downloadSpeedMbps = downloadSpeedMbps,
                uploadSpeedMbps = null, // Upload speed requires server cooperation
                isLoading = false,
                error = if (downloadSpeedMbps == null) "Unable to measure speed - check network connectivity" else null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SpeedTestResult(
                downloadSpeedMbps = null,
                uploadSpeedMbps = null,
                isLoading = false,
                error = "Speed test failed: ${e.message}"
            )
        }
    }
}

