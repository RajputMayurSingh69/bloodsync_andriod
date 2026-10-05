package com.bloodsync.services

import com.bloodsync.config.Constants.REQUEST_STATUS
import com.bloodsync.config.FirebaseAdmin.db
import com.google.cloud.firestore.DocumentChange
import com.google.cloud.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Service
class EmergencyDispatcherService(
    private val loggerBotService: LoggerBotService
) {
    private val donorCompatibility = mapOf(
        "O-" to listOf("O-"),
        "O+" to listOf("O+", "O-"),
        "A-" to listOf("A-", "O-"),
        "A+" to listOf("A+", "A-", "O+", "O-"),
        "B-" to listOf("B-", "O-"),
        "B+" to listOf("B+", "B-", "O+", "O-"),
        "AB-" to listOf("AB-", "A-", "B-", "O-"),
        "AB+" to listOf("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-")
    )

    private val processedEmergencyIds = ConcurrentHashMap.newKeySet<String>()
    private var isDispatcherRunning = false

    private fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's mean radius in kilometers
        val toRad = { angle: Double -> (angle * Math.PI) / 180.0 }

        val dLat = toRad(lat2 - lat1)
        val dLon = toRad(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun getEligibleDonorGroups(neededGroup: String): List<String> {
        val clean = neededGroup.trim().uppercase()
        return donorCompatibility[clean] ?: listOf(clean)
    }

    data class EmergencyDispatchResult(
        val success: Boolean,
        val notifiedCount: Int,
        val error: String? = null
    )

    suspend fun onEmergencyCreated(emergency: Map<String, Any>): EmergencyDispatchResult {
        val requestId = emergency["id"] as? String ?: return EmergencyDispatchResult(false, 0, "Invalid emergency payload")

        val bloodGroupNeeded = emergency["bloodGroupNeeded"] as? String ?: "O+"
        val patientName = emergency["patientName"] as? String ?: "Emergency Patient"
        val hospitalName = emergency["hospitalName"] as? String ?: "Hospital"
        val unitsRequired = (emergency["unitsRequired"] as? Number)?.toInt() ?: 1
        val urgency = emergency["urgencyLevel"] as? String ?: "IMMEDIATE"
        val requesterId = emergency["userId"] as? String ?: ""

        val emgLat = (emergency["latitude"] as? Number)?.toDouble()
        val emgLon = (emergency["longitude"] as? Number)?.toDouble()
        val hasEmergencyCoordinates = emgLat != null && emgLon != null
        val maxRadarRadiusKm = 10.0

        println("[EmergencyDispatcher] Processing SOS $requestId for $unitsRequired units of $bloodGroupNeeded at $hospitalName${if (hasEmergencyCoordinates) " (GPS: ${"%.4f".format(emgLat)}, ${"%.4f".format(emgLon)})" else ""}")

        if (db == null) {
            println("[EmergencyDispatcher] Firestore DB not available. Cannot fetch donor tokens.")
            return EmergencyDispatchResult(false, 0, "Firestore uninitialized")
        }

        try {
            val eligibleGroups = getEligibleDonorGroups(bloodGroupNeeded)
            val tokenMap = mutableMapOf<String, Map<String, Any?>>()

            fun evaluateProximity(data: Map<String, Any>): Pair<Boolean, Double?> {
                if (!hasEmergencyCoordinates) return Pair(true, null)

                val donorLat = (data["latitude"] as? Number)?.toDouble()
                val donorLon = (data["longitude"] as? Number)?.toDouble()

                if (donorLat != null && donorLon != null) {
                    val distKm = calculateHaversineDistanceKm(emgLat!!, emgLon!!, donorLat, donorLon)
                    return Pair(distKm <= maxRadarRadiusKm, distKm)
                }
                return Pair(true, null)
            }

            val usersSnap = db.collection("users").whereEqualTo("isAvailableDonor", true).get().get()
            usersSnap.documents.forEach { doc ->
                val data = doc.data
                val donorId = doc.id
                val donorGroup = data["bloodGroup"] as? String ?: ""
                val token = data["fcmToken"] as? String

                if (token != null && eligibleGroups.contains(donorGroup) && donorId != requesterId) {
                    val proximity = evaluateProximity(data)
                    if (proximity.first) {
                        tokenMap[token] = mapOf("donorId" to donorId, "donorGroup" to donorGroup, "distanceKm" to proximity.second)
                    }
                }
            }

            val donorsSnap = db.collection("donors").whereEqualTo("isAvailableDonor", true).get().get()
            donorsSnap.documents.forEach { doc ->
                val data = doc.data
                val donorId = doc.id
                val donorGroup = data["bloodGroup"] as? String ?: ""
                val token = data["fcmToken"] as? String

                if (token != null && eligibleGroups.contains(donorGroup) && donorId != requesterId) {
                    val proximity = evaluateProximity(data)
                    if (proximity.first) {
                        tokenMap[token] = mapOf("donorId" to donorId, "donorGroup" to donorGroup, "distanceKm" to proximity.second)
                    }
                }
            }

            val tokenList = tokenMap.keys.toList()

            if (tokenList.isEmpty()) {
                println("[EmergencyDispatcher] No matching donors with active FCM tokens found within 10km radar for $bloodGroupNeeded.")
                return EmergencyDispatchResult(true, 0)
            }

            println("[EmergencyDispatcher] Found ${tokenList.size} matching donors within 10km radar. Dispatching push alert...")

            val bodyText = if (hasEmergencyCoordinates) {
                "$patientName needs $unitsRequired unit(s) at $hospitalName (Within 10km Radar). Tap to respond now!"
            } else {
                "$patientName needs $unitsRequired unit(s) at $hospitalName. Tap to respond now!"
            }

            val messageBuilder = MulticastMessage.builder()
                .addAllTokens(tokenList)
                .setNotification(Notification.builder()
                    .setTitle("\uD83D\uDEA8 CRITICAL: $bloodGroupNeeded Blood Urgently Needed!")
                    .setBody(bodyText)
                    .build())
                .putAllData(mapOf(
                    "type" to "EMERGENCY",
                    "requestId" to requestId,
                    "bloodGroup" to bloodGroupNeeded,
                    "hospital" to hospitalName,
                    "patient" to patientName,
                    "units" to unitsRequired.toString(),
                    "urgency" to urgency,
                    "emergencyLat" to if (hasEmergencyCoordinates) emgLat.toString() else "",
                    "emergencyLon" to if (hasEmergencyCoordinates) emgLon.toString() else "",
                    "radarRadiusKm" to "10.0"
                ))
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                        .setChannelId("channel_emergency_alerts")
                        .setSound("default")
                        .setDefaultVibrateTimings(true)
                        .setColor("#D32F2F")
                        .build())
                    .build())

            val response = FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build())
            println("[EmergencyDispatcher] FCM Multicast Result: ${response.successCount} delivered, ${response.failureCount} failed.")

            db.collection("emergency_requests").document(requestId).set(mapOf(
                "donorsNotifiedCount" to response.successCount,
                "radarRadiusKm" to 10.0,
                "lastPushDispatchedAt" to FieldValue.serverTimestamp()
            ), com.google.cloud.firestore.SetOptions.merge()).get()

            loggerBotService.logOtpEvent(
                requestId = requestId,
                rawEmail = "emergency-dispatcher@bloodsync.org",
                requestStatus = REQUEST_STATUS.SUCCESS,
                otpGenStatus = "SKIPPED",
                emailSendingStatus = "SKIPPED",
                errorCategory = null,
                errorMessage = null,
                metadata = mapOf(
                    "action" to "FCM_EMERGENCY_DISPATCH",
                    "bloodGroupNeeded" to bloodGroupNeeded,
                    "hasEmergencyCoordinates" to hasEmergencyCoordinates,
                    "tokensCount" to tokenList.size,
                    "deliveredCount" to response.successCount,
                    "failedCount" to response.failureCount
                )
            )

            return EmergencyDispatchResult(true, response.successCount)
        } catch (error: Exception) {
            println("[EmergencyDispatcher] Error dispatching push notification for $requestId: ${error.message}")
            return EmergencyDispatchResult(false, 0, error.message)
        }
    }

    @PostConstruct
    fun startListener() {
        if (isDispatcherRunning || db == null) return

        try {
            println("[EmergencyDispatcher] Starting real-time Firestore listener for emergency_requests...")
            isDispatcherRunning = true

            db.collection("emergency_requests")
                .whereEqualTo("status", "BROADCASTING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("[EmergencyDispatcher] Firestore listener warning: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED) {
                            val docData = change.document.data
                            val id = change.document.id
                            val mutableData = docData.toMutableMap()
                            mutableData["id"] = id

                            val pName = mutableData["patientName"] as? String ?: ""
                            val hName = mutableData["hospitalName"] as? String ?: ""
                            
                            if (pName.contains("Jane Doe") || hName.contains("Metro General") || id.startsWith("emg_dummy")) {
                                return@forEach
                            }

                            if (!processedEmergencyIds.contains(id)) {
                                processedEmergencyIds.add(id)
                                kotlinx.coroutines.DelicateCoroutinesApi::class
                                kotlinx.coroutines.GlobalScope.launch {
                                    onEmergencyCreated(mutableData)
                                }
                            }
                        }
                    }
                }
        } catch (err: Exception) {
            println("[EmergencyDispatcher] Could not start listener: ${err.message}")
        }
    }
}
