package com.bloodsync.data.emergency

import android.content.Context
import android.util.Log
import com.bloodsync.data.model.EmergencyRequest
import com.bloodsync.data.model.EmergencyStatus
import com.bloodsync.data.model.UrgencyLevel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Real-time Emergency Listener that monitors Firestore's "emergency_requests"
 * collection and streams new inbound emergency broadcasts as a Kotlin Flow.
 */
class EmergencyListener(private val context: Context) {

    private val tag = "EmergencyListener"
    private var registration: ListenerRegistration? = null

    /**
     * Expose a reactive Kotlin Flow of active emergency requests from Firestore.
     */
    fun getEmergencyRequestsFlow(): Flow<List<EmergencyRequest>> = callbackFlow {
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Failed to get Firestore instance: ${e.message}")
            null
        }

        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("emergency_requests")
            .whereEqualTo("status", "BROADCASTING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Firestore snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val patient = doc.getString("patientName") ?: "Emergency Patient"
                            val hospital = doc.getString("hospitalName") ?: "Medical Center"

                            EmergencyRequest(
                                id = id,
                                patientName = patient,
                                bloodGroupNeeded = doc.getString("bloodGroupNeeded") ?: "O+",
                                unitsRequired = (doc.getLong("unitsRequired") ?: 1L).toInt(),
                                hospitalName = hospital,
                                hospitalAddress = doc.getString("hospitalAddress") ?: "Hospital Ward",
                                contactPhone = doc.getString("contactPhone") ?: "",
                                urgencyLevel = try {
                                    UrgencyLevel.valueOf(doc.getString("urgencyLevel") ?: "IMMEDIATE")
                                } catch (_: Exception) {
                                    UrgencyLevel.IMMEDIATE
                                },
                                status = try {
                                    EmergencyStatus.valueOf(doc.getString("status") ?: "BROADCASTING")
                                } catch (_: Exception) {
                                    EmergencyStatus.BROADCASTING
                                },
                                additionalNotes = doc.getString("additionalNotes") ?: "",
                                requestedAt = doc.getString("requestedAt") ?: "Just now",
                                donorsNotifiedCount = (doc.getLong("donorsNotifiedCount") ?: 1L).toInt(),
                                responders = emptyList(),
                                latitude = doc.getDouble("latitude"),
                                longitude = doc.getDouble("longitude"),
                                requesterId = doc.getString("userId") ?: doc.getString("requesterId") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to parse emergency document ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(tag, "Received ${requests.size} active emergency requests from Firestore.")
                    trySend(requests)
                }
            }

        awaitClose {
            Log.d(tag, "Closing emergency snapshot listener.")
            listener.remove()
        }
    }

    /**
     * Start observing emergencies and automatically dispatch to EmergencyAlertManager.
     */
    fun startListening(isAppInForeground: Boolean = true) {
        stopListening()
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) { null } ?: return

        registration = db.collection("emergency_requests")
            .whereEqualTo("status", "BROADCASTING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val parsed = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val patient = doc.getString("patientName") ?: "Emergency Patient"
                            val hospital = doc.getString("hospitalName") ?: "Medical Center"

                            EmergencyRequest(
                                id = id,
                                patientName = patient,
                                bloodGroupNeeded = doc.getString("bloodGroupNeeded") ?: "O+",
                                unitsRequired = (doc.getLong("unitsRequired") ?: 1L).toInt(),
                                hospitalName = hospital,
                                hospitalAddress = doc.getString("hospitalAddress") ?: "Hospital Ward",
                                contactPhone = doc.getString("contactPhone") ?: "",
                                urgencyLevel = try {
                                    UrgencyLevel.valueOf(doc.getString("urgencyLevel") ?: "IMMEDIATE")
                                } catch (_: Exception) {
                                    UrgencyLevel.IMMEDIATE
                                },
                                status = try {
                                    EmergencyStatus.valueOf(doc.getString("status") ?: "BROADCASTING")
                                } catch (_: Exception) {
                                    EmergencyStatus.BROADCASTING
                                },
                                additionalNotes = doc.getString("additionalNotes") ?: "",
                                requestedAt = doc.getString("requestedAt") ?: "Just now",
                                donorsNotifiedCount = (doc.getLong("donorsNotifiedCount") ?: 1L).toInt(),
                                responders = emptyList(),
                                latitude = doc.getDouble("latitude"),
                                longitude = doc.getDouble("longitude"),
                                requesterId = doc.getString("userId") ?: doc.getString("requesterId") ?: ""
                            )
                        } catch (_: Exception) { null }
                    }

                    // Forward to EmergencyAlertManager
                    EmergencyAlertManager.onEmergencyRequestsBatchArrived(
                        context = context,
                        requests = parsed,
                        isAppInForeground = isAppInForeground
                    )
                }
            }
    }

    fun stopListening() {
        registration?.remove()
        registration = null
    }
}
