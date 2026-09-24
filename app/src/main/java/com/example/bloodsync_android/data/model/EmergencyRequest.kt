package com.example.bloodsync_android.data.model

enum class UrgencyLevel(val label: String, val badgeColorHex: Long) {
    IMMEDIATE("Immediate (< 1 Hour)", 0xFFDC2626),
    URGENT("Urgent (< 3 Hours)", 0xFFEA580C),
    WITHIN_24_HOURS("Within 24 Hours", 0xFF2563EB)
}

enum class EmergencyStatus {
    BROADCASTING,
    RESPONDERS_ACTIVE,
    FULFILLED,
    CANCELLED
}

data class EmergencyResponder(
    val id: String,
    val name: String,
    val bloodGroup: String,
    val distanceKm: Double,
    val etaMinutes: Int,
    val status: String = "On the way",
    val phone: String = "+1 (555) 019-2834"
)

data class EmergencyRequest(
    val id: String,
    val patientName: String,
    val bloodGroupNeeded: String,
    val unitsRequired: Int,
    val hospitalName: String,
    val hospitalAddress: String,
    val contactPhone: String,
    val urgencyLevel: UrgencyLevel,
    val additionalNotes: String,
    val requestedAt: String,
    val status: EmergencyStatus = EmergencyStatus.BROADCASTING,
    val donorsNotifiedCount: Int = 42,
    val responders: List<EmergencyResponder> = emptyList()
)
