package com.example.bloodsync_android.data.model

data class Certificate(
    val id: String,
    val certificateCode: String,
    val donorName: String,
    val bloodGroup: String,
    val donationDate: String,
    val donationCount: Int,
    val donationMilestone: String, // e.g. "4th Blood Donation"
    val hospitalName: String,
    val units: Int = 1,
    val verifiedBy: String = "Dr. Eleanor Martinez, Chief of Transfusion Medicine",
    val issueDate: String,
    val qrVerificationCode: String
)
