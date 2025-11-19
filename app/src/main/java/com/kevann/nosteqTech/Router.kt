package com.kevann.nosteqTech


enum class RouterStatus {
    LOS, // Loss of Signal (Critical)
    LATENCY, // High Latency (Warning)
    OFFLINE, // Power loss/Offline
    ONLINE // Normal
}

data class Router(
    val id: String,
    val customerName: String,
    val customerId: String,
    val model: String,
    val status: RouterStatus,
    val lastSeen: String,
    val ipAddress: String,
    val macAddress: String,
    val rxPower: Double, // dBm
    val txPower: Double, // dBm
    val temperature: Int, // Celsius
    val address: String,
    val latencyMs: Int = 0
)

