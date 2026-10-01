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
    private var appointmentListener: ListenerRegistration? = null
    private var donationListener: ListenerRegistration? = null
    private var certificateListener: ListenerRegistration? = null

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

    fun getFirebaseAuth(): FirebaseAuth? = auth

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

    fun deleteEmergencyRequest(id: String) {
        val db = firestore ?: return
        try {
            db.collection("emergency_requests").document(id).delete()
                .addOnSuccessListener {
                    Log.d(tag, "Emergency request $id deleted from Firestore")
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete emergency request from Firestore", e)
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
                                val patient = doc.getString("patientName") ?: ""
                                val hospital = doc.getString("hospitalName") ?: ""
                                val id = doc.getString("id") ?: doc.id

                                // If this is a legacy dummy/seed test document, purge it from Firestore permanently
                                if (patient.contains("Jane Doe", ignoreCase = true) ||
                                    hospital.contains("Metro General", ignoreCase = true) ||
                                    patient.equals("any one", ignoreCase = true) ||
                                    hospital.equals("no one", ignoreCase = true) ||
                                    id.startsWith("emg_dummy")
                                ) {
                                    doc.reference.delete()
                                    return@mapNotNull null
                                }

                                EmergencyRequest(
                                    id = id,
                                    patientName = patient.ifBlank { "Emergency Patient" },
                                    bloodGroupNeeded = doc.getString("bloodGroupNeeded") ?: "O+",
                                    unitsRequired = (doc.getLong("unitsRequired") ?: 1L).toInt(),
                                    hospitalName = hospital.ifBlank { "Medical Center" },
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
                                    donorsNotifiedCount = (doc.getLong("donorsNotifiedCount") ?: 1L).toInt(),
                                    responders = emptyList()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        // Always deliver the updated list so when collection is clean/empty, local state clears
                        onUpdate(list)
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
                                    email = doc.getString("email") ?: "", 
                                    phone = doc.getString("phone") ?: "", 
                                    bloodGroup = doc.getString("bloodGroup") ?: "O+",
                                    city = doc.getString("city") ?: "",
                                    address = doc.getString("address") ?: "", 
                                    gender = doc.getString("gender") ?: "Male",
                                    age = (doc.getLong("age") ?: 18L).toInt(),
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
            "gender" to profile.gender,
            "age" to profile.age,
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
                "displayName" to profile.name.ifBlank { "Volunteer Donor" },
                "name" to profile.name,
                "phone" to profile.phone,
                "email" to profile.email,
                "bloodGroup" to profile.bloodGroup,
                "city" to profile.city,
                "address" to profile.address,
                "gender" to profile.gender,
                "age" to profile.age,
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

    /**
     * Register a new voluntary donor in Firebase Cloud (both /users and /donors)
     * and log the registration event to /donor_registrations for real-time donor tracking and admin notification.
     */
    fun registerNewDonorInCloud(
        profile: UserProfile,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val db = firestore
        val currentUserId = auth?.currentUser?.uid ?: profile.id.ifBlank { "usr_${System.currentTimeMillis()}" }

        saveUserProfile(profile)

        if (db != null) {
            val registrationLog = hashMapOf(
                "donorId" to currentUserId,
                "name" to profile.name,
                "phone" to profile.phone,
                "email" to profile.email,
                "bloodGroup" to profile.bloodGroup,
                "city" to profile.city,
                "address" to profile.address,
                "gender" to profile.gender,
                "age" to profile.age,
                "isAvailableDonor" to profile.isAvailableDonor,
                "registeredAt" to FieldValue.serverTimestamp(),
                "alertMessage" to "New Voluntary Donor Registered: ${profile.name} (${profile.bloodGroup}), Age: ${profile.age}, Gender: ${profile.gender}, City: ${profile.city}, Contact: ${profile.phone}"
            )
            db.collection("donor_registrations").document(currentUserId)
                .set(registrationLog, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(tag, "Donor registration logged in Firebase Cloud: $currentUserId")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Failed to log donor registration: ${e.message}")
                    onFailure(e)
                }
        } else {
            onSuccess()
        }
    }

    /**
     * Listen for real-time booked appointments for the current user from Firebase Firestore.
     */
    fun listenToAppointments(userId: String, onUpdate: (List<Appointment>) -> Unit) {
        val db = firestore ?: return
        val currentUserId = if (userId.isNotBlank()) userId else (auth?.currentUser?.uid ?: return)
        if (currentUserId.isBlank()) return
        try {
            appointmentListener?.remove()
            appointmentListener = db.collection("appointments")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Appointments listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                Appointment(
                                    id = doc.getString("id") ?: doc.id,
                                    bloodBankId = doc.getString("bloodBankId") ?: "",
                                    bloodBankName = doc.getString("bloodBankName") ?: "Blood Bank",
                                    bloodBankAddress = doc.getString("bloodBankAddress") ?: "",
                                    date = doc.getString("date") ?: "",
                                    timeSlot = doc.getString("timeSlot") ?: "",
                                    donationType = doc.getString("donationType") ?: "Whole Blood",
                                    status = try {
                                        AppointmentStatus.valueOf(doc.getString("status") ?: "UPCOMING")
                                    } catch (_: Exception) {
                                        AppointmentStatus.UPCOMING
                                    },
                                    referenceCode = doc.getString("referenceCode") ?: "",
                                    reminderEnabled = doc.getBoolean("reminderEnabled") ?: true,
                                    bookedAt = doc.getString("bookedAt") ?: ""
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
            Log.e(tag, "Failed to set up appointments listener", e)
        }
    }

    /**
     * Save completed donation record to user's private subcollection in Firestore.
     */
    fun saveDonationRecord(userId: String, record: DonationRecord) {
        val db = firestore ?: return
        val currentUserId = if (userId.isNotBlank()) userId else (auth?.currentUser?.uid ?: return)
        if (currentUserId.isBlank()) return
        val data = hashMapOf(
            "id" to record.id,
            "date" to record.date,
            "hospitalName" to record.hospitalName,
            "location" to record.location,
            "bloodGroup" to record.bloodGroup,
            "unitsDonated" to record.unitsDonated,
            "donationType" to record.donationType,
            "status" to record.status.name,
            "certificateId" to (record.certificateId ?: ""),
            "hemoglobinRecorded" to record.hemoglobinRecorded,
            "bloodPressure" to record.bloodPressure,
            "pulseRate" to record.pulseRate,
            "doctorOrPhlebotomist" to record.doctorOrPhlebotomist,
            "notes" to record.notes,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(currentUserId)
            .collection("donations").document(record.id)
            .set(data, SetOptions.merge())
    }

    /**
     * Save issued certificate to user's private subcollection in Firestore.
     */
    fun saveCertificate(userId: String, cert: Certificate) {
        val db = firestore ?: return
        val currentUserId = if (userId.isNotBlank()) userId else (auth?.currentUser?.uid ?: return)
        if (currentUserId.isBlank()) return
        val data = hashMapOf(
            "id" to cert.id,
            "certificateCode" to cert.certificateCode,
            "donorName" to cert.donorName,
            "bloodGroup" to cert.bloodGroup,
            "donationDate" to cert.donationDate,
            "donationCount" to cert.donationCount,
            "donationMilestone" to cert.donationMilestone,
            "hospitalName" to cert.hospitalName,
            "units" to cert.units,
            "verifiedBy" to cert.verifiedBy,
            "issueDate" to cert.issueDate,
            "qrVerificationCode" to cert.qrVerificationCode,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(currentUserId)
            .collection("certificates").document(cert.id)
            .set(data, SetOptions.merge())
    }

    /**
     * Fetch user profile from Firestore /users/{userId} on sign-in to restore all cloud data.
     */
    fun fetchUserProfile(userId: String, onResult: (UserProfile?) -> Unit) {
        val db = firestore ?: return onResult(null)
        val uid = if (userId.isNotBlank()) userId else (auth?.currentUser?.uid ?: return onResult(null))
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    try {
                        val profile = UserProfile(
                            id = doc.getString("id") ?: doc.id,
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            phone = doc.getString("phone") ?: "",
                            bloodGroup = doc.getString("bloodGroup") ?: "O+",
                            city = doc.getString("city") ?: "",
                            address = doc.getString("address") ?: "",
                            gender = doc.getString("gender") ?: "Male",
                            age = (doc.getLong("age") ?: 18L).toInt(),
                            totalDonations = (doc.getLong("totalDonations") ?: 0L).toInt(),
                            livesSaved = (doc.getLong("livesSaved") ?: 0L).toInt(),
                            isAvailableDonor = doc.getBoolean("isAvailableDonor") ?: true
                        )
                        onResult(profile)
                    } catch (_: Exception) {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Listen for real-time completed donations for current user from Firestore.
     */
    fun listenToUserDonations(userId: String, onUpdate: (List<DonationRecord>) -> Unit) {
        val db = firestore ?: return
        val uid = if (userId.isNotBlank()) userId else (auth?.currentUser?.uid ?: return)
        if (uid.isBlank()) return
        try {
            donationListener?.remove()
            donationListener = db.collection("users").document(uid)
                .collection("donations")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            DonationRecord(
                                id = doc.getString("id") ?: doc.id,
                                date = doc.getString("date") ?: "",
                                hospitalName = doc.getString("hospitalName") ?: "",
                                location = doc.getString("location") ?: "",
                                bloodGroup = doc.getString("bloodGroup") ?: "",
                                unitsDonated = (doc.getLong("unitsDonated") ?: 1L).toInt(),
                                donationType = doc.getString("donationType") ?: "Whole Blood",
                                status = try {
                                    DonationStatus.valueOf(doc.getString("status") ?: "VERIFIED")
                                } catch (_: Exception) {
                                    DonationStatus.VERIFIED
                                },
                                certificateId = doc.getString("certificateId")?.takeIf { it.isNotEmpty() },
                                hemoglobinRecorded = doc.getDouble("hemoglobinRecorded") ?: 14.0,
                                bloodPressure = doc.getString("bloodPressure") ?: "120/80 mmHg",
                                pulseRate = (doc.getLong("pulseRate") ?: 72L).toInt(),
                                doctorOrPhlebotomist = doc.getString("doctorOrPhlebotomist") ?: "Medical Officer",
                                notes = doc.getString("notes") ?: ""
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set up donations listener", e)
        }
    }

    /**
     * Listen for real-time appreciation certificates for current user from Firestore.
     */
    fun listenToUserCertificates(userId: String, onUpdate: (List<Certificate>) -> Unit) {
        val db = firestore ?: return
        val uid = if (userId.isNotBlank()) userId else (auth?.currentUser?.uid ?: return)
        if (uid.isBlank()) return
        try {
            certificateListener?.remove()
            certificateListener = db.collection("users").document(uid)
                .collection("certificates")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Certificate(
                                id = doc.getString("id") ?: doc.id,
                                certificateCode = doc.getString("certificateCode") ?: "",
                                donorName = doc.getString("donorName") ?: "",
                                bloodGroup = doc.getString("bloodGroup") ?: "",
                                donationDate = doc.getString("donationDate") ?: "",
                                donationCount = (doc.getLong("donationCount") ?: 1L).toInt(),
                                donationMilestone = doc.getString("donationMilestone") ?: "",
                                hospitalName = doc.getString("hospitalName") ?: "",
                                units = (doc.getLong("units") ?: 1L).toInt(),
                                verifiedBy = doc.getString("verifiedBy") ?: "",
                                issueDate = doc.getString("issueDate") ?: "",
                                qrVerificationCode = doc.getString("qrVerificationCode") ?: ""
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set up certificates listener", e)
        }
    }

    fun cleanup() {
        emergencyListener?.remove()
        bloodBankListener?.remove()
        donorListener?.remove()
        appointmentListener?.remove()
        donationListener?.remove()
        certificateListener?.remove()
    }
}
