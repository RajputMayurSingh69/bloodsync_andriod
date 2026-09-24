package com.example.bloodsync_android.data.model

enum class AppointmentStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED
}

data class BloodBank(
    val id: String,
    val name: String,
    val address: String,
    val distanceKm: Double,
    val openHours: String = "8:00 AM - 6:00 PM",
    val phone: String = "+1 (555) 345-6789",
    val bloodStockStatus: String = "Critical Need: O+, B-",
    val availableDates: List<String> = listOf("Today", "Tomorrow", "In 2 Days", "In 3 Days")
)

data class Appointment(
    val id: String,
    val bloodBankId: String,
    val bloodBankName: String,
    val bloodBankAddress: String,
    val date: String,
    val timeSlot: String,
    val donationType: String = "Whole Blood",
    val status: AppointmentStatus = AppointmentStatus.UPCOMING,
    val referenceCode: String,
    val reminderEnabled: Boolean = true,
    val bookedAt: String = "2026-09-24"
)
