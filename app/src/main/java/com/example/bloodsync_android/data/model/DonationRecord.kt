package com.example.bloodsync_android.data.model

enum class DonationStatus {
    VERIFIED,
    COMPLETED,
    IN_REVIEW
}

data class DonationRecord(
    val id: String,
    val date: String,
    val hospitalName: String,
    val location: String,
    val bloodGroup: String,
    val unitsDonated: Int = 1,
    val donationType: String = "Whole Blood",
    val status: DonationStatus = DonationStatus.VERIFIED,
    val certificateId: String? = null,
    val hemoglobinRecorded: Double = 13.8,
    val bloodPressure: String = "120/80 mmHg",
    val pulseRate: Int = 72,
    val doctorOrPhlebotomist: String = "Dr. Robert Vance, MD",
    val notes: String = "Successful standard whole blood donation. Donor vitals stable throughout."
)
