package com.kevann.nosteqTech.data.api.cache

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kevann.nosteqTech.data.api.OnuDetail
import kotlinx.coroutines.tasks.await

class FirestoreOnuCache(private val context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val onusCollection = "onus_cache_v2"
    private val metadataDoc = "cache_metadata"
    private val analytics = context?.let { CacheAnalytics(it) }

    // Check if cache is valid (less than 30 minutes old)
    suspend fun isCacheValid(): Boolean {
        return try {
            val metadataRef = firestore.collection(onusCollection).document(metadataDoc)
            val snapshot = metadataRef.get().await()
            val lastUpdated = snapshot.getLong("lastUpdated") ?: 0L
            val currentTime = System.currentTimeMillis()
            val timeDiffMinutes = (currentTime - lastUpdated) / (1000 * 60)

            Log.d("[v0] Firestore Cache", "Cache age: $timeDiffMinutes minutes, Valid: ${timeDiffMinutes < 30}")
            timeDiffMinutes < 30
        } catch (e: Exception) {
            Log.e("[v0] Firestore Cache", "Error checking cache validity: ${e.message}")
            false
        }
    }

    suspend fun getOnusFromCache(): List<OnuDetail>? {
        return try {
            Log.d("[v0] Firestore Cache", "Fetching ONUs from collection...")
            val snapshot = firestore.collection(onusCollection)
                .whereNotEqualTo("sn", metadataDoc) // Exclude metadata document
                .get()
                .await()

            Log.d("[v0] Firestore Cache", "Fetched ${snapshot.documents.size} ONU documents")
            analytics?.recordCacheHit(snapshot.documents.size)

            snapshot.documents.mapNotNull { document ->
                try {
                    OnuDetail(
                        uniqueExternalId = document.getString("id"),
                        sn = document.getString("sn") ?: "",
                        name = document.getString("name") ?: "",
                        oltId = document.getString("oltId"),
                        oltName = document.getString("oltName"),
                        board = document.getString("board"),
                        port = document.getString("port"),
                        onu = document.getString("onu"),
                        onuTypeId = document.getString("onuTypeId"),
                        onuTypeName = document.getString("onuTypeName"),
                        zoneId = document.getString("zoneId"),
                        zoneName = document.getString("zoneName"),
                        address = document.getString("address"),
                        odbName = document.getString("odbName"),
                        mode = document.getString("mode"),
                        wanMode = document.getString("wanMode"),
                        ipAddress = document.getString("ipAddress"),
                        subnetMask = document.getString("subnetMask"),
                        defaultGateway = document.getString("defaultGateway"),
                        dns1 = document.getString("dns1"),
                        dns2 = document.getString("dns2"),
                        username = document.getString("username"),
                        password = document.getString("password"),
                        catv = document.getString("catv"),
                        administrativeStatus = document.getString("administrativeStatus"),
                        servicePorts = emptyList(),
                        phoneNumber = document.getString("phoneNumber"),
                        status = document.getString("status") ?: "Unknown",
                        rxPower = document.getDouble("rxPower"),
                        txPower = document.getDouble("txPower"),
                        lastSeen = document.getString("lastSeen") ?: "Unknown",
                        distance = document.getLong("distance")?.toInt(),
                        model = document.getString("model")
                    )
                } catch (e: Exception) {
                    Log.e("[v0] Firestore Cache", "Error parsing ONU document ${document.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("[v0] Firestore Cache", "Error fetching ONUs from collection: ${e.message}")
            analytics?.recordCacheMiss()
            null
        }
    }

    suspend fun saveOnusToCache(onus: List<OnuDetail>): Int {
        return try {
            Log.d("[v0] Firestore Cache", "Starting batch save of ${onus.size} ONUs...")

            analytics?.recordApiCall(onus.size)

            var savedCount = 0

            // Save in batches to avoid hitting too many concurrent writes
            onus.chunked(100).forEach { batch ->
                val writeBatch = firestore.batch()

                batch.forEach { onuDetail ->
                    val docId = onuDetail.sn // Use serial number as document ID for easy lookup
                    val onuRef = firestore.collection(onusCollection).document(docId)

                    val onuData = mapOf(
                        "id" to onuDetail.uniqueExternalId,
                        "sn" to onuDetail.sn,
                        "name" to onuDetail.name,
                        "oltId" to onuDetail.oltId,
                        "oltName" to onuDetail.oltName,
                        "board" to onuDetail.board,
                        "port" to onuDetail.port,
                        "onu" to onuDetail.onu,
                        "onuTypeId" to onuDetail.onuTypeId,
                        "onuTypeName" to onuDetail.onuTypeName,
                        "zoneId" to onuDetail.zoneId,
                        "zoneName" to onuDetail.zoneName,
                        "address" to onuDetail.address,
                        "odbName" to onuDetail.odbName,
                        "mode" to onuDetail.mode,
                        "wanMode" to onuDetail.wanMode,
                        "ipAddress" to onuDetail.ipAddress,
                        "subnetMask" to onuDetail.subnetMask,
                        "defaultGateway" to onuDetail.defaultGateway,
                        "dns1" to onuDetail.dns1,
                        "dns2" to onuDetail.dns2,
                        "username" to onuDetail.username,
                        "password" to onuDetail.password,
                        "catv" to onuDetail.catv,
                        "administrativeStatus" to onuDetail.administrativeStatus,
                        "phoneNumber" to onuDetail.phoneNumber,
                        "status" to onuDetail.status,
                        "rxPower" to onuDetail.rxPower,
                        "txPower" to onuDetail.txPower,
                        "lastSeen" to onuDetail.lastSeen,
                        "distance" to onuDetail.distance,
                        "model" to onuDetail.model
                    )

                    writeBatch.set(onuRef, onuData)
                    savedCount++
                }

                writeBatch.commit().await()
                Log.d("[v0] Firestore Cache", "Batch saved ${batch.size} ONUs")
            }

            // Update metadata with current timestamp
            val metadataRef = firestore.collection(onusCollection).document(metadataDoc)
            metadataRef.set(
                mapOf(
                    "lastUpdated" to System.currentTimeMillis(),
                    "totalOnus" to onus.size
                )
            ).await()

            Log.d("[v0] Firestore Cache", "Successfully saved all ${onus.size} ONUs to collection")
            savedCount
        } catch (e: Exception) {
            Log.e("[v0] Firestore Cache", "Error saving ONUs to cache: ${e.message}", e)
            0
        }
    }
}
