package com.example.bloodsync_android.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.bloodsync_android.data.model.*
import com.example.bloodsync_android.data.notification.NotificationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ThemeMode {
    SYSTEM, // Phone jesa (Follow phone system)
    LIGHT,  // Light Mode
    DARK    // Dark Mode
}

class BloodSyncRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bloodsync_prefs", Context.MODE_PRIVATE)

    // Reactive Compose states for real-time UI updates
    private val _themeMode = mutableStateOf(
        try {
            val saved = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(saved)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
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

    private val _notifications = mutableStateListOf<AppNotification>()
    val notifications: List<AppNotification> = _notifications

    private val _unreadNotificationCount = mutableIntStateOf(0)
    val unreadNotificationCount: State<Int> = _unreadNotificationCount

    private val _isUserLoggedIn = mutableStateOf(false)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn

    fun resetDemoData() {
        prefs.edit().clear().apply()
        seedInitialData()
        saveAllToPrefs()
        _isUserLoggedIn.value = true
        _themeMode.value = ThemeMode.SYSTEM
        updateUnreadCount()
    }

    init {
        // Initialize notification channels
        NotificationHelper.createNotificationChannels(context)

        // Load or seed data
        loadOrSeedData()
        updateUnreadCount()
    }

    private fun loadOrSeedData() {
        val hasSeeded = prefs.getBoolean("has_seeded_v1", false)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        _isUserLoggedIn.value = isLoggedIn

        if (!hasSeeded) {
            seedInitialData()
            saveAllToPrefs()
            prefs.edit().putBoolean("has_seeded_v1", true).apply()
        } else {
            loadFromPrefs()
        }
    }

    private fun seedInitialData() {
        // 1. Initial Profile
        _userProfile.value = UserProfile(
            id = "usr_991",
            name = "Alex Rivera",
            email = "alex.rivera@bloodsync.org",
            phone = "+1 (555) 234-8899",
            bloodGroup = "O+",
            city = "Central Metro",
            address = "742 Healthway Boulevard",
            totalDonations = 4,
            livesSaved = 12,
            isAvailableDonor = true,
            isNotificationEnabled = true,
            isEmergencyVolunteer = true
        )

        // 2. Initial Certificates
        val cert1 = Certificate(
            id = "cert_101",
            certificateCode = "BS-CERT-2025-O-1001",
            donorName = "Alex Rivera",
            bloodGroup = "O+",
            donationDate = "Jan 12, 2025",
            donationCount = 1,
            donationMilestone = "1st Lifesaver Donation",
            hospitalName = "Metro General Hospital",
            units = 1,
            verifiedBy = "Dr. Eleanor Vance, MD",
            issueDate = "Jan 12, 2025",
            qrVerificationCode = "VERIFIED-BS-2025-01-ALEXR"
        )
        val cert2 = Certificate(
            id = "cert_102",
            certificateCode = "BS-CERT-2025-O-2412",
            donorName = "Alex Rivera",
            bloodGroup = "O+",
            donationDate = "May 08, 2025",
            donationCount = 2,
            donationMilestone = "2nd Lifesaver Donation",
            hospitalName = "City Red Cross Blood Center",
            units = 1,
            verifiedBy = "Dr. Michael Chen, Chief of Pathology",
            issueDate = "May 08, 2025",
            qrVerificationCode = "VERIFIED-BS-2025-05-ALEXR"
        )
        val cert3 = Certificate(
            id = "cert_103",
            certificateCode = "BS-CERT-2025-O-3891",
            donorName = "Alex Rivera",
            bloodGroup = "O+",
            donationDate = "Sep 20, 2025",
            donationCount = 3,
            donationMilestone = "3rd Lifesaver Donation",
            hospitalName = "St. Jude Memorial Hospital",
            units = 1,
            verifiedBy = "Dr. Eleanor Vance, MD",
            issueDate = "Sep 20, 2025",
            qrVerificationCode = "VERIFIED-BS-2025-09-ALEXR"
        )
        val cert4 = Certificate(
            id = "cert_104",
            certificateCode = "BS-CERT-2026-O-4921",
            donorName = "Alex Rivera",
            bloodGroup = "O+",
            donationDate = "Jan 24, 2026",
            donationCount = 4,
            donationMilestone = "4th Lifesaver Donation",
            hospitalName = "Grace Valley Medical Blood Bank",
            units = 1,
            verifiedBy = "Dr. Sarah Al-Mansoor, MD",
            issueDate = "Jan 24, 2026",
            qrVerificationCode = "VERIFIED-BS-2026-01-ALEXR"
        )
        _certificates.addAll(listOf(cert4, cert3, cert2, cert1))

        // 3. Initial Donation Records
        val rec1 = DonationRecord(
            id = "rec_004",
            date = "Jan 24, 2026",
            hospitalName = "Grace Valley Medical Blood Bank",
            location = "East Wing, Room 204",
            bloodGroup = "O+",
            unitsDonated = 1,
            donationType = "Whole Blood (450ml)",
            status = DonationStatus.VERIFIED,
            certificateId = "cert_104",
            hemoglobinRecorded = 14.5,
            bloodPressure = "118/76 mmHg",
            pulseRate = 70,
            doctorOrPhlebotomist = "Nurse David Miller, RN",
            notes = "Standard donor collection completed without any adverse reaction. Refreshment provided."
        )
        val rec2 = DonationRecord(
            id = "rec_003",
            date = "Sep 20, 2025",
            hospitalName = "St. Jude Memorial Hospital",
            location = "Transfusion Unit, 3rd Floor",
            bloodGroup = "O+",
            unitsDonated = 1,
            donationType = "Whole Blood (450ml)",
            status = DonationStatus.VERIFIED,
            certificateId = "cert_103",
            hemoglobinRecorded = 14.0,
            bloodPressure = "120/80 mmHg",
            pulseRate = 72,
            doctorOrPhlebotomist = "Dr. Eleanor Vance, MD",
            notes = "Excellent donor condition. Screened clean for all viral markers."
        )
        val rec3 = DonationRecord(
            id = "rec_002",
            date = "May 08, 2025",
            hospitalName = "City Red Cross Blood Center",
            location = "Donor Lounge B",
            bloodGroup = "O+",
            unitsDonated = 1,
            donationType = "Whole Blood (450ml)",
            status = DonationStatus.VERIFIED,
            certificateId = "cert_102",
            hemoglobinRecorded = 13.9,
            bloodPressure = "122/82 mmHg",
            pulseRate = 74,
            doctorOrPhlebotomist = "Phlebotomist Angela Rios",
            notes = "Platelet count and Hb checked prior to draw. Smooth procedure."
        )
        val rec4 = DonationRecord(
            id = "rec_001",
            date = "Jan 12, 2025",
            hospitalName = "Metro General Hospital",
            location = "Main Blood Pavilion",
            bloodGroup = "O+",
            unitsDonated = 1,
            donationType = "Whole Blood (450ml)",
            status = DonationStatus.VERIFIED,
            certificateId = "cert_101",
            hemoglobinRecorded = 14.2,
            bloodPressure = "116/74 mmHg",
            pulseRate = 68,
            doctorOrPhlebotomist = "Dr. Michael Chen, MD",
            notes = "First time registered donation in the BloodSync network."
        )
        _donationHistory.addAll(listOf(rec1, rec2, rec3, rec4))

        // 4. Initial Health Record
        _healthRecord.value = HealthRecord(
            age = 27,
            gender = "Male",
            weightKg = 72.0,
            lastDonationDateString = "2026-01-24",
            hemoglobinGPerDl = 14.5,
            systolicBp = 118,
            diastolicBp = 76,
            pulseBpm = 70,
            hasTattooRecent = false,
            hasColdFeverRecent = false,
            hasAntibioticsRecent = false,
            isPregnant = false
        )

        // 5. Initial Blood Banks
        _bloodBanks.addAll(
            listOf(
                BloodBank(
                    id = "bb_01",
                    name = "Metro Central Blood Bank",
                    address = "120 Medical Plaza Way, Suite 100",
                    distanceKm = 1.2,
                    openHours = "08:00 AM - 08:00 PM",
                    phone = "+1 (555) 441-2000",
                    bloodStockStatus = "Critical: O-, A- Low"
                ),
                BloodBank(
                    id = "bb_02",
                    name = "City Red Cross Donor Center",
                    address = "450 Red Cross Drive",
                    distanceKm = 3.5,
                    openHours = "07:30 AM - 07:00 PM",
                    phone = "+1 (555) 441-3500",
                    bloodStockStatus = "Urgent: B+, O+ High Demand"
                ),
                BloodBank(
                    id = "bb_03",
                    name = "Grace Valley Hospital Transfusion",
                    address = "890 Hope Avenue",
                    distanceKm = 5.1,
                    openHours = "24/7 Transfusion Center",
                    phone = "+1 (555) 441-8900",
                    bloodStockStatus = "All Types Welcome"
                ),
                BloodBank(
                    id = "bb_04",
                    name = "St. Jude Children's Blood Pavilion",
                    address = "210 Care Circle",
                    distanceKm = 6.8,
                    openHours = "09:00 AM - 05:00 PM",
                    phone = "+1 (555) 441-7200",
                    bloodStockStatus = "Platelets Needed Urgently"
                )
            )
        )

        // 6. Initial Appointments
        val calSeed = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 2) }
        val seedUpcomingDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(calSeed.time)
        val seedBookedDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())

        val apt1 = Appointment(
            id = "apt_101",
            bloodBankId = "bb_01",
            bloodBankName = "Metro Central Blood Bank",
            bloodBankAddress = "120 Medical Plaza Way, Suite 100",
            date = seedUpcomingDate,
            timeSlot = "10:30 AM",
            donationType = "Whole Blood",
            status = AppointmentStatus.UPCOMING,
            referenceCode = "SYNC-APT-9921",
            reminderEnabled = true,
            bookedAt = seedBookedDate
        )
        val apt2 = Appointment(
            id = "apt_102",
            bloodBankId = "bb_03",
            bloodBankName = "Grace Valley Hospital Transfusion",
            bloodBankAddress = "890 Hope Avenue",
            date = "Jan 24, 2026",
            timeSlot = "02:00 PM",
            donationType = "Whole Blood",
            status = AppointmentStatus.COMPLETED,
            referenceCode = "SYNC-APT-4902",
            reminderEnabled = false,
            bookedAt = "Jan 18, 2026"
        )
        _appointments.addAll(listOf(apt1, apt2))

        // 7. Initial Emergency Requests
        val emg1 = EmergencyRequest(
            id = "emg_001",
            patientName = "Marcus Brody (Trauma ICU)",
            bloodGroupNeeded = "O-",
            unitsRequired = 3,
            hospitalName = "Metro General Hospital",
            hospitalAddress = "Emergency Wing, 100 Emergency Dr",
            contactPhone = "+1 (555) 911-0422",
            urgencyLevel = UrgencyLevel.IMMEDIATE,
            additionalNotes = "Emergency surgery ongoing due to vehicular collision. Rare O- needed urgently.",
            requestedAt = "15 mins ago",
            status = EmergencyStatus.RESPONDERS_ACTIVE,
            donorsNotifiedCount = 38,
            responders = listOf(
                EmergencyResponder("resp_1", "Sarah Jenkins", "O-", 1.8, 12, "En route", "+1 (555) 234-9911"),
                EmergencyResponder("resp_2", "Liam Foster", "O-", 3.2, 22, "Confirmed", "+1 (555) 345-8822")
            )
        )
        val emg2 = EmergencyRequest(
            id = "emg_002",
            patientName = "Elena Rostova",
            bloodGroupNeeded = "B+",
            unitsRequired = 2,
            hospitalName = "St. Jude Memorial Hospital",
            hospitalAddress = "Oncology Ward, 210 Care Circle",
            contactPhone = "+1 (555) 911-5588",
            urgencyLevel = UrgencyLevel.URGENT,
            additionalNotes = "Chemotherapy patient platelet/blood replacement.",
            requestedAt = "1 hour ago",
            status = EmergencyStatus.RESPONDERS_ACTIVE,
            donorsNotifiedCount = 56,
            responders = listOf(
                EmergencyResponder("resp_3", "Kevin Zhao", "B+", 2.5, 18, "At reception", "+1 (555) 456-1133")
            )
        )
        _emergencyRequests.addAll(listOf(emg1, emg2))

        // 8. Initial Notifications
        _notifications.addAll(
            listOf(
                AppNotification(
                    id = "notif_001",
                    title = "🚨 Emergency: O- Blood Needed!",
                    message = "Metro General Hospital urgently needs 3 units of O- blood for trauma surgery.",
                    timestamp = "15 mins ago",
                    type = NotificationType.EMERGENCY,
                    isRead = false,
                    targetScreen = "emergency",
                    targetId = "emg_001"
                ),
                AppNotification(
                    id = "notif_002",
                    title = "📅 Appointment Confirmed",
                    message = "Your donation appointment is scheduled at Metro Central Blood Bank for $seedUpcomingDate at 10:30 AM.",
                    timestamp = "2 hours ago",
                    type = NotificationType.APPOINTMENT,
                    isRead = false,
                    targetScreen = "appointment",
                    targetId = "apt_101"
                ),
                AppNotification(
                    id = "notif_003",
                    title = "🩺 You Are Eligible to Donate!",
                    message = "Your 90-day recovery window has passed! You can save lives again.",
                    timestamp = "1 day ago",
                    type = NotificationType.ELIGIBILITY,
                    isRead = true,
                    targetScreen = "health"
                ),
                AppNotification(
                    id = "notif_004",
                    title = "🎖️ Certificate of Appreciation Issued",
                    message = "Your 4th Lifesaver Certificate for Grace Valley Medical is ready to view and share.",
                    timestamp = "Jan 24, 2026",
                    type = NotificationType.CERTIFICATE,
                    isRead = true,
                    targetScreen = "certificate",
                    targetId = "cert_104"
                )
            )
        )
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

        // Trigger Android System Notification
        val numericId = (System.currentTimeMillis() % 100000).toInt()
        NotificationHelper.sendSystemNotification(context, numericId, title, message, type)
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
        val updated = _notifications.map { it.copy(isRead = true) }
        _notifications.clear()
        _notifications.addAll(updated)
        updateUnreadCount()
        saveNotificationsToPrefs()
    }

    private fun updateUnreadCount() {
        _unreadNotificationCount.intValue = _notifications.count { !it.isRead }
    }

    // ==========================================
    // DONATION HISTORY METHODS
    // ==========================================
    fun addDonationRecord(
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
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())

        val currentCount = _userProfile.value.totalDonations + 1
        val certId = "cert_" + UUID.randomUUID().toString().take(8)
        val certCode = "BS-CERT-2026-" + bloodGroup.replace("+", "P").replace("-", "N") + "-" + (1000 + currentCount * 333)

        val newCert = Certificate(
            id = certId,
            certificateCode = certCode,
            donorName = _userProfile.value.name,
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

        // Update profile donation count & lives saved (1 donation = 3 lives)
        _userProfile.value = _userProfile.value.copy(
            totalDonations = currentCount,
            livesSaved = currentCount * 3
        )

        // Update last donation date in health record
        val isoDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        _healthRecord.value = _healthRecord.value.copy(
            lastDonationDateString = isoDateStr
        )

        saveAllToPrefs()

        // Trigger push notifications
        postNotification(
            title = "🎉 Donation Verified & Logged",
            message = "Thank you! Your donation of $units unit(s) at $hospitalName has been verified.",
            type = NotificationType.SYSTEM,
            targetScreen = "history"
        )
        postNotification(
            title = "🎖️ New Certificate Issued!",
            message = "Your certificate for ${getOrdinal(currentCount)} blood donation is now ready.",
            type = NotificationType.CERTIFICATE,
            targetScreen = "certificate",
            targetId = certId
        )

        return record
    }

    // ==========================================
    // EMERGENCY REQUEST METHODS
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

        // Simulated responders based on blood group
        val initialResponders = listOf(
            EmergencyResponder(
                id = "resp_" + UUID.randomUUID().toString().take(6),
                name = "Dr. Marcus Reed",
                bloodGroup = bloodGroup,
                distanceKm = 1.4,
                etaMinutes = 10,
                status = "Dispatched",
                phone = contactPhone
            ),
            EmergencyResponder(
                id = "resp_" + UUID.randomUUID().toString().take(6),
                name = "Elena Petrova",
                bloodGroup = bloodGroup,
                distanceKm = 2.9,
                etaMinutes = 20,
                status = "On the way",
                phone = "+1 (555) 772-9901"
            )
        )

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
            status = EmergencyStatus.RESPONDERS_ACTIVE,
            donorsNotifiedCount = 47,
            responders = initialResponders
        )

        _emergencyRequests.add(0, request)
        saveEmergencyToPrefs()

        // Push real emergency notification
        postNotification(
            title = "🚨 EMERGENCY: $bloodGroup Blood Needed!",
            message = "Urgent: $units unit(s) required for $patientName at $hospitalName. Matching donors broadcasted.",
            type = NotificationType.EMERGENCY,
            targetScreen = "emergency",
            targetId = emgId
        )

        return request
    }

    fun fulfillEmergencyRequest(id: String) {
        val index = _emergencyRequests.indexOfFirst { it.id == id }
        if (index != -1) {
            _emergencyRequests[index] = _emergencyRequests[index].copy(status = EmergencyStatus.FULFILLED)
            saveEmergencyToPrefs()
            postNotification(
                title = "✅ Emergency Request Fulfilled",
                message = "The blood requirement for ${_emergencyRequests[index].patientName} was fulfilled successfully.",
                type = NotificationType.SYSTEM
            )
        }
    }

    fun cancelEmergencyRequest(id: String) {
        val index = _emergencyRequests.indexOfFirst { it.id == id }
        if (index != -1) {
            _emergencyRequests[index] = _emergencyRequests[index].copy(status = EmergencyStatus.CANCELLED)
            saveEmergencyToPrefs()
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
    // APPOINTMENT SCHEDULING METHODS
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
            _appointments[index] = apt.copy(status = AppointmentStatus.CANCELLED)
            saveAppointmentsToPrefs()
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
            _appointments[index] = apt.copy(
                date = newDate,
                timeSlot = newTimeSlot,
                status = AppointmentStatus.UPCOMING
            )
            saveAppointmentsToPrefs()
            postNotification(
                title = "📅 Appointment Rescheduled",
                message = "New appointment date: $newDate at $newTimeSlot at ${apt.bloodBankName}.",
                type = NotificationType.APPOINTMENT
            )
        }
    }

    // ==========================================
    // USER PROFILE & AUTH METHODS
    // ==========================================
    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        saveProfileToPrefs()
    }

    fun setLoggedIn(loggedIn: Boolean) {
        _isUserLoggedIn.value = loggedIn
        prefs.edit().putBoolean("is_logged_in", loggedIn).apply()
    }

    // ==========================================
    // PERSISTENCE (SharedPreferences + JSON)
    // ==========================================
    private fun saveAllToPrefs() {
        saveProfileToPrefs()
        saveHistoryToPrefs()
        saveCertificatesToPrefs()
        saveHealthToPrefs()
        saveAppointmentsToPrefs()
        saveEmergencyToPrefs()
        saveNotificationsToPrefs()
    }

    private fun saveProfileToPrefs() {
        val p = _userProfile.value
        val json = JSONObject().apply {
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
        prefs.edit().putString("user_profile_json", json.toString()).apply()
    }

    private fun saveHistoryToPrefs() {
        val array = JSONArray()
        _donationHistory.forEach { rec ->
            val obj = JSONObject().apply {
                put("id", rec.id)
                put("date", rec.date)
                put("hospitalName", rec.hospitalName)
                put("location", rec.location)
                put("bloodGroup", rec.bloodGroup)
                put("unitsDonated", rec.unitsDonated)
                put("donationType", rec.donationType)
                put("status", rec.status.name)
                put("certificateId", rec.certificateId ?: "")
                put("hemoglobinRecorded", rec.hemoglobinRecorded)
                put("bloodPressure", rec.bloodPressure)
                put("pulseRate", rec.pulseRate)
                put("doctorOrPhlebotomist", rec.doctorOrPhlebotomist)
                put("notes", rec.notes)
            }
            array.put(obj)
        }
        prefs.edit().putString("donation_history_json", array.toString()).apply()
    }

    private fun saveCertificatesToPrefs() {
        val array = JSONArray()
        _certificates.forEach { cert ->
            val obj = JSONObject().apply {
                put("id", cert.id)
                put("certificateCode", cert.certificateCode)
                put("donorName", cert.donorName)
                put("bloodGroup", cert.bloodGroup)
                put("donationDate", cert.donationDate)
                put("donationCount", cert.donationCount)
                put("donationMilestone", cert.donationMilestone)
                put("hospitalName", cert.hospitalName)
                put("units", cert.units)
                put("verifiedBy", cert.verifiedBy)
                put("issueDate", cert.issueDate)
                put("qrVerificationCode", cert.qrVerificationCode)
            }
            array.put(obj)
        }
        prefs.edit().putString("certificates_json", array.toString()).apply()
    }

    private fun saveHealthToPrefs() {
        val h = _healthRecord.value
        val json = JSONObject().apply {
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
        prefs.edit().putString("health_record_json", json.toString()).apply()
    }

    private fun saveAppointmentsToPrefs() {
        val array = JSONArray()
        _appointments.forEach { apt ->
            val obj = JSONObject().apply {
                put("id", apt.id)
                put("bloodBankId", apt.bloodBankId)
                put("bloodBankName", apt.bloodBankName)
                put("bloodBankAddress", apt.bloodBankAddress)
                put("date", apt.date)
                put("timeSlot", apt.timeSlot)
                put("donationType", apt.donationType)
                put("status", apt.status.name)
                put("referenceCode", apt.referenceCode)
                put("reminderEnabled", apt.reminderEnabled)
                put("bookedAt", apt.bookedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("appointments_json", array.toString()).apply()
    }

    private fun saveEmergencyToPrefs() {
        val array = JSONArray()
        _emergencyRequests.forEach { emg ->
            val obj = JSONObject().apply {
                put("id", emg.id)
                put("patientName", emg.patientName)
                put("bloodGroupNeeded", emg.bloodGroupNeeded)
                put("unitsRequired", emg.unitsRequired)
                put("hospitalName", emg.hospitalName)
                put("hospitalAddress", emg.hospitalAddress)
                put("contactPhone", emg.contactPhone)
                put("urgencyLevel", emg.urgencyLevel.name)
                put("additionalNotes", emg.additionalNotes)
                put("requestedAt", emg.requestedAt)
                put("status", emg.status.name)
                put("donorsNotifiedCount", emg.donorsNotifiedCount)

                val respArray = JSONArray()
                emg.responders.forEach { resp ->
                    val rObj = JSONObject().apply {
                        put("id", resp.id)
                        put("name", resp.name)
                        put("bloodGroup", resp.bloodGroup)
                        put("distanceKm", resp.distanceKm)
                        put("etaMinutes", resp.etaMinutes)
                        put("status", resp.status)
                        put("phone", resp.phone)
                    }
                    respArray.put(rObj)
                }
                put("responders", respArray)
            }
            array.put(obj)
        }
        prefs.edit().putString("emergency_json", array.toString()).apply()
    }

    private fun saveNotificationsToPrefs() {
        val array = JSONArray()
        _notifications.forEach { n ->
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
            // Profile
            prefs.getString("user_profile_json", null)?.let {
                val obj = JSONObject(it)
                _userProfile.value = UserProfile(
                    id = obj.optString("id", "usr_991"),
                    name = obj.optString("name", "Alex Rivera"),
                    email = obj.optString("email", "alex.rivera@bloodsync.org"),
                    phone = obj.optString("phone", "+1 (555) 234-8899"),
                    bloodGroup = obj.optString("bloodGroup", "O+"),
                    city = obj.optString("city", "Central Metro"),
                    address = obj.optString("address", "742 Healthway Boulevard"),
                    totalDonations = obj.optInt("totalDonations", 4),
                    livesSaved = obj.optInt("livesSaved", 12),
                    isAvailableDonor = obj.optBoolean("isAvailableDonor", true),
                    isNotificationEnabled = obj.optBoolean("isNotificationEnabled", true),
                    isEmergencyVolunteer = obj.optBoolean("isEmergencyVolunteer", true)
                )
            }

            // Health
            prefs.getString("health_record_json", null)?.let {
                val obj = JSONObject(it)
                _healthRecord.value = HealthRecord(
                    age = obj.optInt("age", 27),
                    gender = obj.optString("gender", "Male"),
                    weightKg = obj.optDouble("weightKg", 72.0),
                    lastDonationDateString = obj.optString("lastDonationDateString", "2026-01-24"),
                    hemoglobinGPerDl = obj.optDouble("hemoglobinGPerDl", 14.5),
                    systolicBp = obj.optInt("systolicBp", 118),
                    diastolicBp = obj.optInt("diastolicBp", 76),
                    pulseBpm = obj.optInt("pulseBpm", 70),
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
                            doctorOrPhlebotomist = obj.optString("doctorOrPhlebotomist", "Dr. Robert Vance"),
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
                            bookedAt = obj.optString("bookedAt", "2026-09-24")
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
                    val respArray = obj.optJSONArray("responders") ?: JSONArray()
                    val respondersList = mutableListOf<EmergencyResponder>()
                    for (j in 0 until respArray.length()) {
                        val r = respArray.getJSONObject(j)
                        respondersList.add(
                            EmergencyResponder(
                                id = r.getString("id"),
                                name = r.getString("name"),
                                bloodGroup = r.getString("bloodGroup"),
                                distanceKm = r.getDouble("distanceKm"),
                                etaMinutes = r.getInt("etaMinutes"),
                                status = r.optString("status", "On the way"),
                                phone = r.optString("phone", "+1 (555) 000-0000")
                            )
                        )
                    }

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
                            status = EmergencyStatus.valueOf(obj.optString("status", "RESPONDERS_ACTIVE")),
                            donorsNotifiedCount = obj.optInt("donorsNotifiedCount", 35),
                            responders = respondersList
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

            // Blood banks always re-populated if empty
            if (_bloodBanks.isEmpty()) {
                _bloodBanks.addAll(
                    listOf(
                        BloodBank("bb_01", "Metro Central Blood Bank", "120 Medical Plaza Way", 1.2),
                        BloodBank("bb_02", "City Red Cross Donor Center", "450 Red Cross Drive", 3.5),
                        BloodBank("bb_03", "Grace Valley Hospital Transfusion", "890 Hope Avenue", 5.1),
                        BloodBank("bb_04", "St. Jude Children's Blood Pavilion", "210 Care Circle", 6.8)
                    )
                )
            }
        } catch (_: Exception) {
            seedInitialData()
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
