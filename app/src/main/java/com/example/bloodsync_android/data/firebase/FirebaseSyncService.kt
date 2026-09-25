package com.example.bloodsync_android.data.firebase

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.bloodsync_android.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class FirebaseSyncService(private val context: Context) {

    private val tag = "BloodSyncFirebase"

    private val _isFirebaseConnected = mutableStateOf(false)
    val isFirebaseConnected: State<Boolean> = _isFirebaseConnected

    private val _connectionStatus = mutableStateOf("Initializing Firebase...")
    val connectionStatus: State<String> = _connectionStatus

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var emergencyListener: ListenerRegistration? = null
    private var bloodBankListener: ListenerRegistration? = null
    private var donorListener: ListenerRegistration? = null

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }

            if (app != null) {
                firestore = FirebaseFirestore.getInstance()
                val authInstance = FirebaseAuth.getInstance()
                auth = authInstance

                // Authenticate to satisfy Firestore security rules
                if (authInstance.currentUser == null) {
                    authInstance.signInAnonymously()
                        .addOnSuccessListener {
                            Log.d(tag, "Signed in anonymously for secure Firestore access: ${it.user?.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.w(tag, "Anonymous auth notice: ${e.message}")
                        }
                }

                _isFirebaseConnected.value = true
                _connectionStatus.value = "Connected to Firebase Cloud"
                Log.d(tag, "Firebase initialized successfully.")
            } else {
                _isFirebaseConnected.value = false
                _connectionStatus.value = "Firebase initialized with local fallback"
            }
        } catch (e: Exception) {
            _isFirebaseConnected.value = false
            _connectionStatus.value = "Offline / Local Mode (${e.localizedMessage ?: "Awaiting google-services.json"})"
            Log.w(tag, "Firebase init notice: ${e.message}")
        }
    }

    /**
     * Publish Emergency Blood Request to Firebase Firestore in real-time.
     */
    fun publishEmergencyRequest(
        request: EmergencyRequest,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val db = firestore
        if (db == null) {
            onSuccess() // Fallback to local
            return
        }

        val currentUserId = auth?.currentUser?.uid ?: ""

        val data = hashMapOf(
            "id" to request.id,
            "userId" to currentUserId,
            "patientName" to request.patientName,
            "bloodGroupNeeded" to request.bloodGroupNeeded,
            "unitsRequired" to request.unitsRequired,
            "hospitalName" to request.hospitalName,
            "hospitalAddress" to request.hospitalAddress,
            "contactPhone" to request.contactPhone,
            "urgencyLevel" to request.urgencyLevel.name,
            "status" to request.status.name,
            "additionalNotes" to request.additionalNotes,
            "requestedAt" to request.requestedAt,
            "donorsNotifiedCount" to request.donorsNotifiedCount,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("emergency_requests")
            .document(request.id)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(tag, "Emergency SOS published to Firestore: ${request.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to publish emergency SOS to Firestore", e)
                onFailure(e)
            }
    }

    /**
     * Listen for real-time Emergency Requests from Firebase.
     */
    fun listenToEmergencyRequests(onUpdate: (List<EmergencyRequest>) -> Unit) {
        val db = firestore ?: return
        try {
            emergencyListener?.remove()
            emergencyListener = db.collection("emergency_requests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Emergency listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                EmergencyRequest(
                                    id = doc.getString("id") ?: doc.id,
                                    patientName = doc.getString("patientName") ?: "Unknown Patient",
                                    bloodGroupNeeded = doc.getString("bloodGroupNeeded") ?: "O+",
                                    unitsRequired = (doc.getLong("unitsRequired") ?: 1L).toInt(),
                                    hospitalName = doc.getString("hospitalName") ?: "Medical Center",
                                    hospitalAddress = doc.getString("hospitalAddress") ?: "Hospital Ward",
                                    contactPhone = doc.getString("contactPhone") ?: "",
                                    urgencyLevel = try {
                                        UrgencyLevel.valueOf(doc.getString("urgencyLevel") ?: "IMMEDIATE")
                                    } catch (e: Exception) {
                                        UrgencyLevel.IMMEDIATE
                                    },
                                    status = try {
                                        EmergencyStatus.valueOf(doc.getString("status") ?: "BROADCASTING")
                                    } catch (e: Exception) {
                                        EmergencyStatus.BROADCASTING
                                    },
                                    additionalNotes = doc.getString("additionalNotes") ?: "",
                                    requestedAt = doc.getString("requestedAt") ?: "Just now",
                                    donorsNotifiedCount = (doc.getLong("donorsNotifiedCount") ?: 10L).toInt(),
                                    responders = emptyList()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            onUpdate(list)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set up emergency listener", e)
        }
    }

    /**
     * Listen for real-time Blood Banks from Firebase server.
     */
    fun listenToBloodBanks(onUpdate: (List<BloodBank>) -> Unit) {
        val db = firestore ?: return
        try {
            bloodBankListener?.remove()
            bloodBankListener = db.collection("blood_banks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Blood banks listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                BloodBank(
                                    id = doc.getString("id") ?: doc.id,
                                    name = doc.getString("name") ?: "Blood Bank",
                                    address = doc.getString("address") ?: "",
                                    distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                                    openHours = doc.getString("openHours") ?: "24/7",
                                    phone = doc.getString("phone") ?: "",
                                    bloodStockStatus = doc.getString("bloodStockStatus") ?: "Available"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            onUpdate(list)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set up blood bank listener", e)
        }
    }

    /**
     * Listen for real-time registered donors from Firebase server.
     * Sanitized projection: private PII (email, phone, street address) is never exposed.
     */
    fun listenToDonors(onUpdate: (List<UserProfile>) -> Unit) {
        val db = firestore ?: return
        try {
            donorListener?.remove()
            donorListener = db.collection("donors")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Donors listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                UserProfile(
                                    id = doc.getString("id") ?: doc.id,
                                    name = doc.getString("displayName") ?: doc.getString("name") ?: "Volunteer Donor",
                                    email = "", // Sanitized - private to owner
                                    phone = "", // Sanitized - private to owner
                                    bloodGroup = doc.getString("bloodGroup") ?: "O+",
                                    city = doc.getString("city") ?: "",
                                    address = "", // Sanitized - private to owner
                                    totalDonations = (doc.getLong("totalDonations") ?: 0L).toInt(),
                                    livesSaved = (doc.getLong("livesSaved") ?: 0L).toInt(),
                                    isAvailableDonor = doc.getBoolean("isAvailableDonor") ?: true
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            onUpdate(list)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set up donors listener", e)
        }
    }

    /**
     * Sync user profile to Firestore.
     * Step 2: Full details (email, phone, address) are written exclusively to private /users/{userId}.
     * Only a sanitized projection is written to public /donors/{userId}.
     */
    fun saveUserProfile(profile: UserProfile) {
        val db = firestore ?: return
        val currentUserId = auth?.currentUser?.uid ?: profile.id.ifBlank { "usr_${System.currentTimeMillis()}" }

        // 1. Private User Profile (Full data under /users/{userId})
        val privateData = hashMapOf(
            "id" to currentUserId,
            "name" to profile.name,
            "email" to profile.email,
            "phone" to profile.phone,
            "bloodGroup" to profile.bloodGroup,
            "city" to profile.city,
            "address" to profile.address,
            "totalDonations" to profile.totalDonations,
            "livesSaved" to profile.livesSaved,
            "isAvailableDonor" to profile.isAvailableDonor,
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(currentUserId).set(privateData, SetOptions.merge())

        // 2. Public Directory Projection (Sanitized data under /donors/{userId})
        if (profile.isAvailableDonor) {
            val sanitizedPublicData = hashMapOf(
                "id" to currentUserId,
                "displayName" to if (profile.name.isNotBlank()) profile.name.take(1).uppercase() + "***" else "Volunteer Donor",
                "bloodGroup" to profile.bloodGroup,
                "city" to profile.city,
                "totalDonations" to profile.totalDonations,
                "livesSaved" to profile.livesSaved,
                "isAvailableDonor" to true,
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            db.collection("donors").document(currentUserId).set(sanitizedPublicData, SetOptions.merge())
        } else {
            // Remove from public registry if marked unavailable
            db.collection("donors").document(currentUserId).delete()
        }
    }

    /**
     * Save booked appointment to Firestore.
     * Includes userId to enforce Firestore ownership rules.
     */
    fun saveAppointment(appointment: Appointment) {
        val db = firestore ?: return
        val currentUserId = auth?.currentUser?.uid ?: ""
        val data = hashMapOf(
            "id" to appointment.id,
            "userId" to currentUserId,
            "bloodBankId" to appointment.bloodBankId,
            "bloodBankName" to appointment.bloodBankName,
            "bloodBankAddress" to appointment.bloodBankAddress,
            "date" to appointment.date,
            "timeSlot" to appointment.timeSlot,
            "donationType" to appointment.donationType,
            "status" to appointment.status.name,
            "referenceCode" to appointment.referenceCode,
            "bookedAt" to appointment.bookedAt
        )
        db.collection("appointments").document(appointment.id).set(data, SetOptions.merge())
    }

    fun cleanup() {
        emergencyListener?.remove()
        bloodBankListener?.remove()
        donorListener?.remove()
    }
}
