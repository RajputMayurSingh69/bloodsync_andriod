package com.example.bloodsync_android.data.model

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val bloodGroup: String = "O+",
    val city: String = "",
    val address: String = "",
    val totalDonations: Int = 0,
    val livesSaved: Int = 0,
    val isAvailableDonor: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val isEmergencyVolunteer: Boolean = true
)
