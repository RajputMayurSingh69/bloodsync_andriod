package com.bloodsync.services

import com.bloodsync.config.FirebaseConfig
import com.bloodsync.config.FirestoreCollections
import com.google.cloud.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BloodSync Backend - Emergency SOS Dispatcher Service (Kotlin)
 *
 * Listens to BROADCASTING emergency requests in Firestore and sends
 * high-priority FCM multicast push notifications to eligible donors within 10km.
 * Mirrors emergencyDispatcherService.js 1:1 including ABO compatibility and Haversine.
 */
object EmergencyDispatcherService {

    // ABO/Rh Compatibility Chart: Key = needed group, Value = eligible donor groups
    private val DONOR_COMPATIBILITY = mapOf(
        "O-" to listOf("O-"),
        "O+" to listOf("O+", "O-"),
        "A-" to listOf("A-", "O-"),
        "A+" to listOf("A+", "A-", "O+", "O-"),
        "B-" to listOf("B-", "O-"),
        "B+" to listOf("B+", "B-", "O+", "O-"),
        "AB-" to listOf("AB-", "A-", "B-", "O-"),
        "AB+" to listOf("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-")
    )

    private val processedEmergencyIds = mutableSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val MAX_RADAR_RADIUS_KM = 10.0

    /** Haversine great-circle distance in km */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val toRad = { d: Double -> d * Math.PI / 180.0 }
        val dLat = toRad(lat2 - lat1)
        val dLon = toRad(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                Math.sin(dLon / 2).let { it * it }
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun getEligibleDonorGroups(neededGroup: String): List<String> {
        val clean = neededGroup.trim().uppercase()
        return DONOR_COMPATIBILITY[clean] ?: listOf(clean)
    }

    data class DispatchResult(val success: Boolean, val notifiedCount: Int, val error: String? = null)

    suspend fun onEmergencyCreated(emergency: Map<String, Any?>): DispatchResult {
        val requestId = emergency["id"] as? String ?: return DispatchResult(false, 0, "Invalid emergency payload")
        val bloodGroupNeeded = emergency["bloodGroupNeeded"] as? String ?: "O+"
        val patientName = emergency["patientName"] as? String ?: "Emergency Patient"
        val hospitalName = emergency["hospitalName"] as? String ?: "Hospital"
        val unitsRequired = (emergency["unitsRequired"] as? Long)?.toInt() ?: 1
        val urgency = emergency["urgencyLevel"] as? String ?: "IMMEDIATE"
        val requesterId = emergency["userId"] as? String ?: ""

        val emgLat = (emergency["latitude"] as? Double) ?: (emergency["latitude"] as? String)?.toDoubleOrNull()
        val emgLon = (emergency["longitude"] as? Double) ?: (emergency["longitude"] as? String)?.toDoubleOrNull()
        val hasCoords = emgLat != null && emgLon != null

        println("[EmergencyDispatcher] Processing SOS $requestId for $unitsRequired units of $bloodGroupNeeded at $hospitalName${if (hasCoords) " (GPS: ${"%.4f".format(emgLat)}, ${"%.4f".format(emgLon)})" else ""}")

        val db = FirebaseConfig.firestore ?: return DispatchResult(false, 0, "Firestore uninitialized")
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull()
            ?: return DispatchResult(false, 0, "FCM uninitialized")

        return withContext(Dispatchers.IO) {
            try {
                val eligibleGroups = getEligibleDonorGroups(bloodGroupNeeded)

                // Token map: fcmToken -> donor metadata (deduplicates across collections)
                val tokenMap = mutableMapOf<String, Map<String, Any?>>()

                fun evaluateProximity(data: Map<String, Any?>): Boolean {
                    if (!hasCoords) return true
                    val dLat = (data["latitude"] as? Double) ?: (data["latitude"] as? String)?.toDoubleOrNull() ?: return true
                    val dLon = (data["longitude"] as? Double) ?: (data["longitude"] as? String)?.toDoubleOrNull() ?: return true
                    return haversineKm(emgLat!!, emgLon!!, dLat, dLon) <= MAX_RADAR_RADIUS_KM
                }

                // 1. users collection (available donors)
                val usersSnap = db.collection(FirestoreCollections.USERS)
                    .whereEqualTo("isAvailableDonor", true)
                    .get().get()

                usersSnap.forEach { doc ->
                    val data = doc.data
                    val donorId = doc.id
                    val token = data["fcmToken"] as? String ?: return@forEach
                    val donorGroup = data["bloodGroup"] as? String ?: return@forEach
                    if (token.isNotBlank() && eligibleGroups.contains(donorGroup) && donorId != requesterId && evaluateProximity(data)) {
                        tokenMap[token] = mapOf("donorId" to donorId, "donorGroup" to donorGroup)
                    }
                }

                // 2. donors collection (available community donors)
                val donorsSnap = db.collection(FirestoreCollections.DONORS)
                    .whereEqualTo("isAvailableDonor", true)
                    .get().get()

                donorsSnap.forEach { doc ->
                    val data = doc.data
                    val donorId = doc.id
                    val token = data["fcmToken"] as? String ?: return@forEach
                    val donorGroup = data["bloodGroup"] as? String ?: return@forEach
                    if (token.isNotBlank() && eligibleGroups.contains(donorGroup) && donorId != requesterId && evaluateProximity(data)) {
                        tokenMap[token] = mapOf("donorId" to donorId, "donorGroup" to donorGroup)
                    }
                }

                val tokenList = tokenMap.keys.toList()
                if (tokenList.isEmpty()) {
                    println("[EmergencyDispatcher] No matching donors found within 10km for $bloodGroupNeeded.")
                    return@withContext DispatchResult(true, 0)
                }

                println("[EmergencyDispatcher] Found ${tokenList.size} eligible donors. Dispatching FCM multicast...")

                // Build FCM multicast message
                val fcmMessage = MulticastMessage.builder()
                    .addAllTokens(tokenList)
                    .setNotification(
                        Notification.builder()
                            .setTitle("🚨 CRITICAL: $bloodGroupNeeded Blood Urgently Needed!")
                            .setBody(
                                if (hasCoords)
                                    "$patientName needs $unitsRequired unit(s) at $hospitalName (Within 10km Radar). Tap to respond now!"
                                else
                                    "$patientName needs $unitsRequired unit(s) at $hospitalName. Tap to respond now!"
                            )
                            .build()
                    )
                    .putAllData(
                        mapOf(
                            "type" to "EMERGENCY",
                            "requestId" to requestId,
                            "bloodGroup" to bloodGroupNeeded,
                            "hospital" to hospitalName,
                            "patient" to patientName,
                            "units" to unitsRequired.toString(),
                            "urgency" to urgency,
                            "emergencyLat" to (emgLat?.toString() ?: ""),
                            "emergencyLon" to (emgLon?.toString() ?: ""),
                            "radarRadiusKm" to "10.0"
                        )
                    )
                    .setAndroidConfig(
                        AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(
                                AndroidNotification.builder()
                                    .setChannelId("channel_emergency_alerts")
                                    .setSound("default")
                                    .setColor("#D32F2F")
                                    .setDefaultVibrateTimings(true)
                                    .build()
                            )
                            .build()
                    )
                    .build()

                val response = messaging.sendEachForMulticast(fcmMessage)
                println("[EmergencyDispatcher] FCM Multicast: ${response.successCount} delivered, ${response.failureCount} failed.")

                // Write back stats to Firestore
                runCatching {
                    db.collection(FirestoreCollections.EMERGENCY_REQUESTS).document(requestId).set(
                        mapOf(
                            "donorsNotifiedCount" to response.successCount,
                            "radarRadiusKm" to 10.0,
                            "lastPushDispatchedAt" to FieldValue.serverTimestamp()
                        ),
                        com.google.cloud.firestore.SetOptions.merge()
                    ).get()
                }

                DispatchResult(true, response.successCount)
            } catch (e: Exception) {
                println("[EmergencyDispatcher] Error dispatching for $requestId: ${e.message}")
                DispatchResult(false, 0, e.message)
            }
        }
    }

    /**
     * Starts real-time Firestore listener for BROADCASTING emergency requests.
     */
    fun startListener() {
        val db = FirebaseConfig.firestore ?: return
        println("[EmergencyDispatcher] Starting real-time Firestore listener for emergency_requests...")

        db.collection(FirestoreCollections.EMERGENCY_REQUESTS)
            .whereEqualTo("status", "BROADCASTING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("[EmergencyDispatcher] Firestore listener warning: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.cloud.firestore.DocumentChange.Type.ADDED) {
                        val docData = change.document.data.toMutableMap()
                        val id = change.document.id

                        // Skip dummy/seeded documents
                        val patientName = docData["patientName"] as? String ?: ""
                        val hospitalName = docData["hospitalName"] as? String ?: ""
                        if (patientName.contains("Jane Doe") || hospitalName.contains("Metro General") || id.startsWith("emg_dummy")) {
                            return@forEach
                        }

                        if (id !in processedEmergencyIds) {
                            processedEmergencyIds.add(id)
                            docData["id"] = id
                            scope.launch {
                                onEmergencyCreated(docData)
                            }
                        }
                    }
                }
            }
    }
}
