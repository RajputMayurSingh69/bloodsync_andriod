package com.bloodsync.data.model

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val bloodGroup: String = "O+",
    val city: String = "",
    val address: String = "",
    val gender: String = "Male",
    val dob: String = "",
    val age: Int = 18,
    val totalDonations: Int = 0,
    val livesSaved: Int = 0,
    val isAvailableDonor: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val isEmergencyVolunteer: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val role: String = "user",
    
    // Blood Bank Specific Fields
    val licenseNumber: String = "",
    val contactPerson: String = "",
    val operatingHours: String = "24/7",
    val website: String = ""
)
