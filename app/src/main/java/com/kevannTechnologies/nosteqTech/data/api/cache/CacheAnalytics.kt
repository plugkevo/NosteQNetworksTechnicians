package com.kevannTechnologies.nosteqTech.data.api.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

data class CacheMetrics(
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
    val apiCalls: Int = 0,
    val totalOnusServed: Int = 0,
    val cacheSize: Long = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

class CacheAnalytics(private val context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("cache_analytics", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val analyticsCollection = "cache_analytics"

    fun recordCacheHit(onuCount: Int) {
        val hits = sharedPrefs.getInt("cache_hits", 0) + 1
        sharedPrefs.edit().putInt("cache_hits", hits).apply()
        Log.d("[v0] Analytics", "Cache HIT - Total hits: $hits, ONUs served: $onuCount")
        uploadMetricsToFirestore("cache_hit", onuCount)
    }

    fun recordCacheMiss() {
        val misses = sharedPrefs.getInt("cache_misses", 0) + 1
        sharedPrefs.edit().putInt("cache_misses", misses).apply()
        Log.d("[v0] Analytics", "Cache MISS - Total misses: $misses")
        uploadMetricsToFirestore("cache_miss", 0)
    }

    fun recordApiCall(onuCount: Int) {
        val calls = sharedPrefs.getInt("api_calls", 0) + 1
        sharedPrefs.edit().putInt("api_calls", calls).apply()
        Log.d("[v0] Analytics", "API CALL - Total calls: $calls, ONUs fetched: $onuCount")
        uploadMetricsToFirestore("api_call", onuCount)
    }

    fun getMetrics(): CacheMetrics {
        val hits = sharedPrefs.getInt("cache_hits", 0)
        val misses = sharedPrefs.getInt("cache_misses", 0)
        val calls = sharedPrefs.getInt("api_calls", 0)
        val totalOnus = sharedPrefs.getInt("total_onus", 0)

        return CacheMetrics(
            cacheHits = hits,
            cacheMisses = misses,
            apiCalls = calls,
            totalOnusServed = totalOnus,
            lastUpdated = sharedPrefs.getLong("last_updated", System.currentTimeMillis())
        )
    }

    fun resetMetrics() {
        sharedPrefs.edit().clear().apply()
        Log.d("[v0] Analytics", "Metrics reset")
    }

    fun getCacheHitRate(): Double {
        val metrics = getMetrics()
        val totalRequests = metrics.cacheHits + metrics.cacheMisses
        return if (totalRequests > 0) (metrics.cacheHits.toDouble() / totalRequests) * 100 else 0.0
    }

    private fun uploadMetricsToFirestore(eventType: String, onuCount: Int) {
        try {
            val eventData = mapOf(
                "type" to eventType,
                "timestamp" to System.currentTimeMillis(),
                "onuCount" to onuCount,
                "userId" to (sharedPrefs.getString("user_id", "unknown") ?: "unknown")
            )
            firestore.collection(analyticsCollection).add(eventData)
        } catch (e: Exception) {
            Log.e("[v0] Analytics", "Error uploading metrics: ${e.message}")
        }
    }

    fun getTotalApiCallsThisHour(): Int {
        val lastHour = System.currentTimeMillis() - (60 * 60 * 1000)
        // This would require querying Firestore, for now return the total count
        return getMetrics().apiCalls
    }
}
