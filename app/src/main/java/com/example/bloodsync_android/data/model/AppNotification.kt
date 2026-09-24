package com.example.bloodsync_android.data.model

enum class NotificationType(val titlePrefix: String) {
    EMERGENCY("🚨 Emergency Request"),
    APPOINTMENT("📅 Appointment"),
    ELIGIBILITY("🩺 Health & Eligibility"),
    CERTIFICATE("🎖️ Appreciation Certificate"),
    SYSTEM("ℹ️ System Update")
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val targetScreen: String? = null, // "emergency", "appointment", "health", "certificate"
    val targetId: String? = null
)
