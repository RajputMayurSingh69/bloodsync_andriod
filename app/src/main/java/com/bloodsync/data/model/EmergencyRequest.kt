package com.bloodsync.data.model

enum class UrgencyLevel(val label: String, val badgeColorHex: Long) {
    IMMEDIATE("Immediate (< 1 Hour)", 0xFFD32F2F),       // Red - Critical
    URGENT("Urgent (< 3 Hours)", 0xFFF57F17),           // Yellow - High Priority
    WITHIN_24_HOURS("Scheduled (< 24 Hours)", 0xFF2E7D32) // Green - Standard
}

enum class EmergencyStatus {
    BROADCASTING,
    RESPONDERS_ACTIVE,
    FULFILLED,
    CANCELLED
}

data class EmergencyResponder(
    val id: String = "",
    val name: String = "",
    val bloodGroup: String = "",
    val distanceKm: Double = 0.0,
    val etaMinutes: Int = 15,
    val status: String = "ACCEPTED",
    val phone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
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
    val donorsNotifiedCount: Int = 1,
    val responders: List<EmergencyResponder> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val requesterId: String = ""
)
