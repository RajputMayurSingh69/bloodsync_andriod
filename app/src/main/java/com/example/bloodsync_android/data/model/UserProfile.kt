package com.example.bloodsync_android.data.model

data class UserProfile(
    val id: String = "usr_001",
    val name: String = "Alex Morgan",
    val email: String = "alex.morgan@bloodsync.org",
    val phone: String = "+1 (555) 234-5678",
    val bloodGroup: String = "O+",
    val city: String = "Central Metro",
    val address: String = "742 Evergreen Terrace, Suite 4B",
    val totalDonations: Int = 4,
    val livesSaved: Int = 12,
    val isAvailableDonor: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val isEmergencyVolunteer: Boolean = true
)
