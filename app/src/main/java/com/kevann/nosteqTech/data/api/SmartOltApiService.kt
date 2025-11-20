package com.kevann.nosteqTech.data.api


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


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

    // UI Helper fields (mapped manually or from optional JSON fields)
    val status: String = "Unknown", // Mapped from status or administrative_status
    val rxPower: Double? = null,
    val txPower: Double? = null,
    val lastSeen: String = "Unknown",
    val distance: Int? = null,
    val model: String? = null
)

class SmartOltApiService(
    private val subdomain: String,
    private val apiKey: String
) {
    private val baseUrl = "https://$subdomain.smartolt.com/api"

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
                        ipAddress = onuJson.optString("ip_address").ifEmpty { null },
                        subnetMask = onuJson.optString("subnet_mask").ifEmpty { null },
                        defaultGateway = onuJson.optString("default_gateway").ifEmpty { null },
                        dns1 = onuJson.optString("dns1").ifEmpty { null },
                        dns2 = onuJson.optString("dns2").ifEmpty { null },
                        username = onuJson.optString("username").ifEmpty { null },
                        password = onuJson.optString("password").ifEmpty { null },
                        catv = onuJson.optString("catv").ifEmpty { null },
                        administrativeStatus = adminStatus,
                        servicePorts = servicePortsList,

                        // UI Fields
                        status = finalStatus,
                        rxPower = onuJson.optDouble("rx_power").takeIf { !it.isNaN() },
                        txPower = onuJson.optDouble("tx_power").takeIf { !it.isNaN() },
                        lastSeen = onuJson.optString("last_seen", "Unknown"),
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
}
