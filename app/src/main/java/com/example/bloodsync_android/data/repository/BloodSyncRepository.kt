package com.example.bloodsync_android.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.bloodsync_android.data.firebase.FirebaseSyncService
import com.example.bloodsync_android.data.model.*
import com.example.bloodsync_android.data.notification.NotificationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ThemeMode {
    SYSTEM, // Fallback
    LIGHT,  // Light Mode
    DARK    // Dark Mode
}

class BloodSyncRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bloodsync_prefs", Context.MODE_PRIVATE)

    // Firebase Cloud Sync Service
    val firebaseService: FirebaseSyncService = FirebaseSyncService(context)

    // Reactive Compose states for real-time UI updates (Default: LIGHT mode)
    private val _themeMode = mutableStateOf(
        try {
            val saved = prefs.getString("theme_mode", ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
            ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }
    )
    val themeMode: State<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    private val _userProfile = mutableStateOf(UserProfile())
    val userProfile: State<UserProfile> = _userProfile

    private val _donationHistory = mutableStateListOf<DonationRecord>()
    val donationHistory: List<DonationRecord> = _donationHistory

    private val _certificates = mutableStateListOf<Certificate>()
    val certificates: List<Certificate> = _certificates

    private val _emergencyRequests = mutableStateListOf<EmergencyRequest>()
    val emergencyRequests: List<EmergencyRequest> = _emergencyRequests

    private val _healthRecord = mutableStateOf(HealthRecord())
    val healthRecord: State<HealthRecord> = _healthRecord

    private val _appointments = mutableStateListOf<Appointment>()
    val appointments: List<Appointment> = _appointments

    private val _bloodBanks = mutableStateListOf<BloodBank>()
    val bloodBanks: List<BloodBank> = _bloodBanks

    private val _donors = mutableStateListOf<UserProfile>()
    val donors: List<UserProfile> = _donors

    private val _notifications = mutableStateListOf<AppNotification>()
    val notifications: List<AppNotification> = _notifications

    private val _unreadNotificationCount = mutableIntStateOf(0)
    val unreadNotificationCount: State<Int> = _unreadNotificationCount

    private val _isUserLoggedIn = mutableStateOf(false)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn

    init {
        // Initialize notification channels
        NotificationHelper.createNotificationChannels(context)

        // Clear any old dummy demo data on upgrade to v2.0
        val hasClearedDummyData = prefs.getBoolean("has_cleared_dummy_data_v2", false)
        if (!hasClearedDummyData) {
            prefs.edit().clear().putBoolean("has_cleared_dummy_data_v2", true).apply()
        }

        // Load persisted local data
        loadFromPrefs()
        updateUnreadCount()

        // Setup real-time Firebase listeners to sync with server
        setupFirebaseSync()
    }

    private fun setupFirebaseSync() {
        // Listen to live Emergency SOS broadcasts from Firebase
        firebaseService.listenToEmergencyRequests { realList ->
            if (realList.isNotEmpty()) {
                _emergencyRequests.clear()
                _emergencyRequests.addAll(realList)
                saveEmergencyToPrefs()
            }
        }

        // Listen to real Blood Banks from server
        firebaseService.listenToBloodBanks { realBanks ->
            if (realBanks.isNotEmpty()) {
                _bloodBanks.clear()
                _bloodBanks.addAll(realBanks)
            }
        }

        // Listen to verified donors directory
        firebaseService.listenToDonors { realDonors ->
            if (realDonors.isNotEmpty()) {
                _donors.clear()
                _donors.addAll(realDonors)
            }
        }
    }

    fun clearLocalCache() {
        prefs.edit().clear().putBoolean("has_cleared_dummy_data_v2", true).apply()
        _donationHistory.clear()
        _certificates.clear()
        _appointments.clear()
        _emergencyRequests.clear()
        _notifications.clear()
        _donors.clear()
        _userProfile.value = UserProfile()
        _healthRecord.value = HealthRecord()
        _isUserLoggedIn.value = false
        _themeMode.value = ThemeMode.LIGHT
        updateUnreadCount()
    }

    // ==========================================
    // NOTIFICATION METHODS
    // ==========================================
    fun postNotification(
        title: String,
        message: String,
        type: NotificationType,
        targetScreen: String? = null,
        targetId: String? = null
    ) {
        val notifId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        val newNotif = AppNotification(
            id = notifId,
            title = title,
            message = message,
            timestamp = timestamp,
            type = type,
            isRead = false,
            targetScreen = targetScreen,
            targetId = targetId
        )
        _notifications.add(0, newNotif)
        updateUnreadCount()
        saveNotificationsToPrefs()

        NotificationHelper.sendSystemNotification(
            context = context,
            notificationId = notifId.hashCode(),
            title = title,
            message = message,
            type = type
        )
    }

    fun markNotificationAsRead(id: String) {
        val index = _notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            _notifications[index] = _notifications[index].copy(isRead = true)
            updateUnreadCount()
            saveNotificationsToPrefs()
        }
    }

    fun markAllNotificationsAsRead() {
        for (i in _notifications.indices) {
            _notifications[i] = _notifications[i].copy(isRead = true)
        }
        updateUnreadCount()
        saveNotificationsToPrefs()
    }

    fun deleteNotification(id: String) {
        _notifications.removeAll { it.id == id }
        updateUnreadCount()
        saveNotificationsToPrefs()
    }

    fun clearAllNotifications() {
        _notifications.clear()
        updateUnreadCount()
        saveNotificationsToPrefs()
    }

    private fun updateUnreadCount() {
        _unreadNotificationCount.intValue = _notifications.count { !it.isRead }
    }

    // ==========================================
    // DONATION LOGGING & CERTIFICATE GENERATION
    // ==========================================
    fun logCompletedDonation(
        hospitalName: String,
        location: String,
        bloodGroup: String,
        units: Int,
        donationType: String,
        hemoglobin: Double,
        doctorName: String,
        notes: String
    ): DonationRecord {
        val recordId = "rec_" + UUID.randomUUID().toString().take(8)
        val certId = "cert_" + UUID.randomUUID().toString().take(8)
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
        val currentCount = _userProfile.value.totalDonations + 1
        val certCode = "BS-CERT-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${bloodGroup.take(2)}-${1000 + currentCount}"

        val newCert = Certificate(
            id = certId,
            certificateCode = certCode,
            donorName = if (_userProfile.value.name.isNotBlank()) _userProfile.value.name else "Verified Donor",
            bloodGroup = bloodGroup,
            donationDate = dateStr,
            donationCount = currentCount,
            donationMilestone = "${getOrdinal(currentCount)} Lifesaver Donation",
            hospitalName = hospitalName,
            units = units,
            verifiedBy = doctorName,
            issueDate = dateStr,
            qrVerificationCode = "VERIFIED-BS-AUTO-$certCode"
        )
        _certificates.add(0, newCert)

        val record = DonationRecord(
            id = recordId,
            date = dateStr,
            hospitalName = hospitalName,
            location = location,
            bloodGroup = bloodGroup,
            unitsDonated = units,
            donationType = donationType,
            status = DonationStatus.VERIFIED,
            certificateId = certId,
            hemoglobinRecorded = hemoglobin,
            bloodPressure = "120/80 mmHg",
            pulseRate = 72,
            doctorOrPhlebotomist = doctorName,
            notes = notes
        )
        _donationHistory.add(0, record)

        _userProfile.value = _userProfile.value.copy(
            totalDonations = currentCount,
            livesSaved = currentCount * 3
        )

        val isoDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        _healthRecord.value = _healthRecord.value.copy(
            lastDonationDateString = isoDateStr
        )

        saveAllToPrefs()

        postNotification(
            title = "🎉 Donation Verified & Logged",
            message = "Thank you! Your donation of $units unit(s) at $hospitalName has been recorded.",
            type = NotificationType.SYSTEM,
            targetScreen = "history"
        )
        postNotification(
            title = "🎖️ New Certificate Issued!",
            message = "Your certificate for ${getOrdinal(currentCount)} blood donation is ready.",
            type = NotificationType.CERTIFICATE,
            targetScreen = "certificate",
            targetId = certId
        )

        return record
    }

    fun addDonationRecord(
        hospitalName: String,
        location: String,
        bloodGroup: String,
        units: Int,
        donationType: String,
        hemoglobin: Double,
        doctorName: String,
        notes: String
    ): DonationRecord = logCompletedDonation(
        hospitalName = hospitalName,
        location = location,
        bloodGroup = bloodGroup,
        units = units,
        donationType = donationType,
        hemoglobin = hemoglobin,
        doctorName = doctorName,
        notes = notes
    )

    // ==========================================
    // EMERGENCY REQUEST METHODS (CONNECTED TO FIREBASE)
    // ==========================================
    fun createEmergencyRequest(
        patientName: String,
        bloodGroup: String,
        units: Int,
        hospitalName: String,
        hospitalAddress: String,
        contactPhone: String,
        urgencyLevel: UrgencyLevel,
        notes: String
    ): EmergencyRequest {
        val emgId = "emg_" + UUID.randomUUID().toString().take(8)
        val timeStr = "Just now"

        val request = EmergencyRequest(
            id = emgId,
            patientName = patientName,
            bloodGroupNeeded = bloodGroup,
            unitsRequired = units,
            hospitalName = hospitalName,
            hospitalAddress = hospitalAddress,
            contactPhone = contactPhone,
            urgencyLevel = urgencyLevel,
            additionalNotes = notes,
            requestedAt = timeStr,
            status = EmergencyStatus.BROADCASTING,
            donorsNotifiedCount = 1,
            responders = emptyList()
        )

        _emergencyRequests.add(0, request)
        saveEmergencyToPrefs()

        // Push directly to Firebase Firestore Cloud Server
        firebaseService.publishEmergencyRequest(request)

        postNotification(
            title = "🚨 EMERGENCY: $bloodGroup Blood Needed!",
            message = "Urgent: $units unit(s) required for $patientName at $hospitalName. Broadcasted to network.",
            type = NotificationType.EMERGENCY,
            targetScreen = "emergency",
            targetId = emgId
        )

        return request
    }

    fun fulfillEmergencyRequest(id: String) {
        val index = _emergencyRequests.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = _emergencyRequests[index].copy(status = EmergencyStatus.FULFILLED)
            _emergencyRequests[index] = updated
            saveEmergencyToPrefs()
            firebaseService.publishEmergencyRequest(updated)

            postNotification(
                title = "✅ Emergency Request Fulfilled",
                message = "The blood requirement for ${updated.patientName} was fulfilled successfully.",
                type = NotificationType.SYSTEM
            )
        }
    }

    fun cancelEmergencyRequest(id: String) {
        val index = _emergencyRequests.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = _emergencyRequests[index].copy(status = EmergencyStatus.CANCELLED)
            _emergencyRequests[index] = updated
            saveEmergencyToPrefs()
            firebaseService.publishEmergencyRequest(updated)
        }
    }

    // ==========================================
    // HEALTH & ELIGIBILITY METHODS
    // ==========================================
    fun updateHealthRecord(record: HealthRecord) {
        _healthRecord.value = record
        saveHealthToPrefs()

        val result = record.calculateEligibility()
        if (result.status == EligibilityStatus.ELIGIBLE) {
            postNotification(
                title = "🩺 You Are Ready to Donate!",
                message = "Your health profile is in optimal condition. Help save lives today.",
                type = NotificationType.ELIGIBILITY,
                targetScreen = "health"
            )
        }
    }

    // ==========================================
    // APPOINTMENT SCHEDULING (CONNECTED TO FIREBASE)
    // ==========================================
    fun bookAppointment(
        bloodBank: BloodBank,
        date: String,
        timeSlot: String,
        donationType: String
    ): Appointment {
        val aptId = "apt_" + UUID.randomUUID().toString().take(8)
        val refCode = "BS-APT-" + (1000..9999).random()

        val appointment = Appointment(
            id = aptId,
            bloodBankId = bloodBank.id,
            bloodBankName = bloodBank.name,
            bloodBankAddress = bloodBank.address,
            date = date,
            timeSlot = timeSlot,
            donationType = donationType,
            status = AppointmentStatus.UPCOMING,
            referenceCode = refCode,
            reminderEnabled = true,
            bookedAt = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
        )

        _appointments.add(0, appointment)
        saveAppointmentsToPrefs()

        // Sync with Firebase Cloud Server
        firebaseService.saveAppointment(appointment)

        postNotification(
            title = "📅 Appointment Scheduled!",
            message = "Booking confirmed at ${bloodBank.name} on $date at $timeSlot (Ref: $refCode).",
            type = NotificationType.APPOINTMENT,
            targetScreen = "appointment",
            targetId = aptId
        )

        return appointment
    }

    fun cancelAppointment(id: String) {
        val index = _appointments.indexOfFirst { it.id == id }
        if (index != -1) {
            val apt = _appointments[index]
            val updated = apt.copy(status = AppointmentStatus.CANCELLED)
            _appointments[index] = updated
            saveAppointmentsToPrefs()
            firebaseService.saveAppointment(updated)

            postNotification(
                title = "Appointment Cancelled",
                message = "Your appointment at ${apt.bloodBankName} on ${apt.date} was cancelled.",
                type = NotificationType.APPOINTMENT
            )
        }
    }

    fun rescheduleAppointment(id: String, newDate: String, newTimeSlot: String) {
        val index = _appointments.indexOfFirst { it.id == id }
        if (index != -1) {
            val apt = _appointments[index]
            val updated = apt.copy(
                date = newDate,
                timeSlot = newTimeSlot,
                status = AppointmentStatus.UPCOMING
            )
            _appointments[index] = updated
            saveAppointmentsToPrefs()
            firebaseService.saveAppointment(updated)

            postNotification(
                title = "📅 Appointment Rescheduled",
                message = "New appointment date: $newDate at $newTimeSlot at ${apt.bloodBankName}.",
                type = NotificationType.APPOINTMENT
            )
        }
    }

    // ==========================================
    // USER PROFILE & DONOR REGISTRATION
    // ==========================================
    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        saveProfileToPrefs()
        // Sync user profile and donor status to Firebase Cloud Server
        firebaseService.saveUserProfile(profile)
    }

    fun registerDonor(profile: UserProfile) {
        if (!_donors.any { it.phone == profile.phone && profile.phone.isNotBlank() }) {
            _donors.add(0, profile)
        }
        updateUserProfile(profile)
    }

    fun addBloodBank(bloodBank: BloodBank) {
        if (!_bloodBanks.any { it.name.equals(bloodBank.name, ignoreCase = true) }) {
            _bloodBanks.add(bloodBank)
        }
    }

    fun loginUser(name: String, email: String, phone: String, bloodGroup: String) {
        val id = "usr_" + UUID.randomUUID().toString().take(8)
        val profile = UserProfile(
            id = id,
            name = name,
            email = email,
            phone = phone,
            bloodGroup = bloodGroup,
            isAvailableDonor = true
        )
        _userProfile.value = profile
        _isUserLoggedIn.value = true
        prefs.edit().putBoolean("is_logged_in", true).apply()
        saveProfileToPrefs()
        firebaseService.saveUserProfile(profile)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        _isUserLoggedIn.value = loggedIn
        prefs.edit().putBoolean("is_logged_in", loggedIn).apply()
    }

    fun logoutUser() {
        _isUserLoggedIn.value = false
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    // ==========================================
    // LOCAL PERSISTENCE (SharedPreferences)
    // ==========================================
    fun saveAllToPrefs() {
        saveProfileToPrefs()
        saveHealthToPrefs()
        saveCertificatesToPrefs()
        saveHistoryToPrefs()
        saveAppointmentsToPrefs()
        saveEmergencyToPrefs()
        saveNotificationsToPrefs()
    }

    private fun saveProfileToPrefs() {
        val p = _userProfile.value
        val obj = JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("email", p.email)
            put("phone", p.phone)
            put("bloodGroup", p.bloodGroup)
            put("city", p.city)
            put("address", p.address)
            put("totalDonations", p.totalDonations)
            put("livesSaved", p.livesSaved)
            put("isAvailableDonor", p.isAvailableDonor)
            put("isNotificationEnabled", p.isNotificationEnabled)
            put("isEmergencyVolunteer", p.isEmergencyVolunteer)
        }
        prefs.edit().putString("user_profile_json", obj.toString()).apply()
    }

    private fun saveHealthToPrefs() {
        val h = _healthRecord.value
        val obj = JSONObject().apply {
            put("age", h.age)
            put("gender", h.gender)
            put("weightKg", h.weightKg)
            put("lastDonationDateString", h.lastDonationDateString)
            put("hemoglobinGPerDl", h.hemoglobinGPerDl)
            put("systolicBp", h.systolicBp)
            put("diastolicBp", h.diastolicBp)
            put("pulseBpm", h.pulseBpm)
            put("hasTattooRecent", h.hasTattooRecent)
            put("hasColdFeverRecent", h.hasColdFeverRecent)
            put("hasAntibioticsRecent", h.hasAntibioticsRecent)
            put("isPregnant", h.isPregnant)
        }
        prefs.edit().putString("health_record_json", obj.toString()).apply()
    }

    private fun saveCertificatesToPrefs() {
        val array = JSONArray()
        for (c in _certificates) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("certificateCode", c.certificateCode)
                put("donorName", c.donorName)
                put("bloodGroup", c.bloodGroup)
                put("donationDate", c.donationDate)
                put("donationCount", c.donationCount)
                put("donationMilestone", c.donationMilestone)
                put("hospitalName", c.hospitalName)
                put("units", c.units)
                put("verifiedBy", c.verifiedBy)
                put("issueDate", c.issueDate)
                put("qrVerificationCode", c.qrVerificationCode)
            }
            array.put(obj)
        }
        prefs.edit().putString("certificates_json", array.toString()).apply()
    }

    private fun saveHistoryToPrefs() {
        val array = JSONArray()
        for (r in _donationHistory) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("date", r.date)
                put("hospitalName", r.hospitalName)
                put("location", r.location)
                put("bloodGroup", r.bloodGroup)
                put("unitsDonated", r.unitsDonated)
                put("donationType", r.donationType)
                put("status", r.status.name)
                put("certificateId", r.certificateId ?: "")
                put("hemoglobinRecorded", r.hemoglobinRecorded)
                put("bloodPressure", r.bloodPressure)
                put("pulseRate", r.pulseRate)
                put("doctorOrPhlebotomist", r.doctorOrPhlebotomist)
                put("notes", r.notes)
            }
            array.put(obj)
        }
        prefs.edit().putString("donation_history_json", array.toString()).apply()
    }

    private fun saveAppointmentsToPrefs() {
        val array = JSONArray()
        for (a in _appointments) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("bloodBankId", a.bloodBankId)
                put("bloodBankName", a.bloodBankName)
                put("bloodBankAddress", a.bloodBankAddress)
                put("date", a.date)
                put("timeSlot", a.timeSlot)
                put("donationType", a.donationType)
                put("status", a.status.name)
                put("referenceCode", a.referenceCode)
                put("reminderEnabled", a.reminderEnabled)
                put("bookedAt", a.bookedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("appointments_json", array.toString()).apply()
    }

    private fun saveEmergencyToPrefs() {
        val array = JSONArray()
        for (e in _emergencyRequests) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("patientName", e.patientName)
                put("bloodGroupNeeded", e.bloodGroupNeeded)
                put("unitsRequired", e.unitsRequired)
                put("hospitalName", e.hospitalName)
                put("hospitalAddress", e.hospitalAddress)
                put("contactPhone", e.contactPhone)
                put("urgencyLevel", e.urgencyLevel.name)
                put("additionalNotes", e.additionalNotes)
                put("requestedAt", e.requestedAt)
                put("status", e.status.name)
                put("donorsNotifiedCount", e.donorsNotifiedCount)
            }
            array.put(obj)
        }
        prefs.edit().putString("emergency_json", array.toString()).apply()
    }

    private fun saveNotificationsToPrefs() {
        val array = JSONArray()
        for (n in _notifications) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("message", n.message)
                put("timestamp", n.timestamp)
                put("type", n.type.name)
                put("isRead", n.isRead)
                put("targetScreen", n.targetScreen ?: "")
                put("targetId", n.targetId ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString("notifications_json", array.toString()).apply()
    }

    private fun loadFromPrefs() {
        try {
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            _isUserLoggedIn.value = isLoggedIn

            // Profile
            prefs.getString("user_profile_json", null)?.let {
                val obj = JSONObject(it)
                _userProfile.value = UserProfile(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    email = obj.optString("email", ""),
                    phone = obj.optString("phone", ""),
                    bloodGroup = obj.optString("bloodGroup", "O+"),
                    city = obj.optString("city", ""),
                    address = obj.optString("address", ""),
                    totalDonations = obj.optInt("totalDonations", 0),
                    livesSaved = obj.optInt("livesSaved", 0),
                    isAvailableDonor = obj.optBoolean("isAvailableDonor", true),
                    isNotificationEnabled = obj.optBoolean("isNotificationEnabled", true),
                    isEmergencyVolunteer = obj.optBoolean("isEmergencyVolunteer", true)
                )
            }

            // Health
            prefs.getString("health_record_json", null)?.let {
                val obj = JSONObject(it)
                _healthRecord.value = HealthRecord(
                    age = obj.optInt("age", 26),
                    gender = obj.optString("gender", "Male"),
                    weightKg = obj.optDouble("weightKg", 68.0),
                    lastDonationDateString = obj.optString("lastDonationDateString", ""),
                    hemoglobinGPerDl = obj.optDouble("hemoglobinGPerDl", 14.2),
                    systolicBp = obj.optInt("systolicBp", 120),
                    diastolicBp = obj.optInt("diastolicBp", 80),
                    pulseBpm = obj.optInt("pulseBpm", 72),
                    hasTattooRecent = obj.optBoolean("hasTattooRecent", false),
                    hasColdFeverRecent = obj.optBoolean("hasColdFeverRecent", false),
                    hasAntibioticsRecent = obj.optBoolean("hasAntibioticsRecent", false),
                    isPregnant = obj.optBoolean("isPregnant", false)
                )
            }

            // Certificates
            prefs.getString("certificates_json", null)?.let {
                val array = JSONArray(it)
                _certificates.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    _certificates.add(
                        Certificate(
                            id = obj.getString("id"),
                            certificateCode = obj.getString("certificateCode"),
                            donorName = obj.getString("donorName"),
                            bloodGroup = obj.getString("bloodGroup"),
                            donationDate = obj.getString("donationDate"),
                            donationCount = obj.getInt("donationCount"),
                            donationMilestone = obj.getString("donationMilestone"),
                            hospitalName = obj.getString("hospitalName"),
                            units = obj.optInt("units", 1),
                            verifiedBy = obj.getString("verifiedBy"),
                            issueDate = obj.getString("issueDate"),
                            qrVerificationCode = obj.getString("qrVerificationCode")
                        )
                    )
                }
            }

            // History
            prefs.getString("donation_history_json", null)?.let {
                val array = JSONArray(it)
                _donationHistory.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    _donationHistory.add(
                        DonationRecord(
                            id = obj.getString("id"),
                            date = obj.getString("date"),
                            hospitalName = obj.getString("hospitalName"),
                            location = obj.getString("location"),
                            bloodGroup = obj.getString("bloodGroup"),
                            unitsDonated = obj.optInt("unitsDonated", 1),
                            donationType = obj.optString("donationType", "Whole Blood"),
                            status = DonationStatus.valueOf(obj.optString("status", "VERIFIED")),
                            certificateId = obj.optString("certificateId").takeIf { s -> s.isNotEmpty() },
                            hemoglobinRecorded = obj.optDouble("hemoglobinRecorded", 14.0),
                            bloodPressure = obj.optString("bloodPressure", "120/80 mmHg"),
                            pulseRate = obj.optInt("pulseRate", 72),
                            doctorOrPhlebotomist = obj.optString("doctorOrPhlebotomist", "Medical Officer"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            // Appointments
            prefs.getString("appointments_json", null)?.let {
                val array = JSONArray(it)
                _appointments.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    _appointments.add(
                        Appointment(
                            id = obj.getString("id"),
                            bloodBankId = obj.getString("bloodBankId"),
                            bloodBankName = obj.getString("bloodBankName"),
                            bloodBankAddress = obj.getString("bloodBankAddress"),
                            date = obj.getString("date"),
                            timeSlot = obj.getString("timeSlot"),
                            donationType = obj.optString("donationType", "Whole Blood"),
                            status = AppointmentStatus.valueOf(obj.optString("status", "UPCOMING")),
                            referenceCode = obj.getString("referenceCode"),
                            reminderEnabled = obj.optBoolean("reminderEnabled", true),
                            bookedAt = obj.optString("bookedAt", "")
                        )
                    )
                }
            }

            // Emergency
            prefs.getString("emergency_json", null)?.let {
                val array = JSONArray(it)
                _emergencyRequests.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    _emergencyRequests.add(
                        EmergencyRequest(
                            id = obj.getString("id"),
                            patientName = obj.getString("patientName"),
                            bloodGroupNeeded = obj.getString("bloodGroupNeeded"),
                            unitsRequired = obj.getInt("unitsRequired"),
                            hospitalName = obj.getString("hospitalName"),
                            hospitalAddress = obj.getString("hospitalAddress"),
                            contactPhone = obj.getString("contactPhone"),
                            urgencyLevel = UrgencyLevel.valueOf(obj.optString("urgencyLevel", "IMMEDIATE")),
                            additionalNotes = obj.optString("additionalNotes", ""),
                            requestedAt = obj.optString("requestedAt", "Recent"),
                            status = EmergencyStatus.valueOf(obj.optString("status", "BROADCASTING")),
                            donorsNotifiedCount = obj.optInt("donorsNotifiedCount", 10),
                            responders = emptyList()
                        )
                    )
                }
            }

            // Notifications
            prefs.getString("notifications_json", null)?.let {
                val array = JSONArray(it)
                _notifications.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    _notifications.add(
                        AppNotification(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            message = obj.getString("message"),
                            timestamp = obj.getString("timestamp"),
                            type = NotificationType.valueOf(obj.optString("type", "SYSTEM")),
                            isRead = obj.optBoolean("isRead", false),
                            targetScreen = obj.optString("targetScreen").takeIf { s -> s.isNotEmpty() },
                            targetId = obj.optString("targetId").takeIf { s -> s.isNotEmpty() }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore parse errors on corrupted prefs
        }
    }

    private fun getOrdinal(n: Int): String {
        return when {
            n % 100 in 11..13 -> "${n}th"
            n % 10 == 1 -> "${n}st"
            n % 10 == 2 -> "${n}nd"
            n % 10 == 3 -> "${n}rd"
            else -> "${n}th"
        }
    }
}
