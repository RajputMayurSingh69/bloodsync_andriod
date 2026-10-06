package com.bloodsync.config

/**
 * BloodSync Backend - System Constants (Kotlin)
 * Mirrors constants.js 1:1
 */
object ErrorCategories {
    const val OTP_GENERATION_ERROR = "OTP_GENERATION_ERROR"
    const val OTP_DATABASE_ERROR = "OTP_DATABASE_ERROR"
    const val EMAIL_CONFIGURATION_ERROR = "EMAIL_CONFIGURATION_ERROR"
    const val SMTP_AUTH_ERROR = "SMTP_AUTH_ERROR"
    const val EMAIL_SEND_ERROR = "EMAIL_SEND_ERROR"
    const val FIREBASE_ERROR = "FIREBASE_ERROR"
    const val RATE_LIMIT_ERROR = "RATE_LIMIT_ERROR"
    const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val DUPLICATE_EMAIL_ERROR = "DUPLICATE_EMAIL_ERROR"
    const val BANK_REGISTRATION_ERROR = "BANK_REGISTRATION_ERROR"
    const val BANK_NOT_FOUND_ERROR = "BANK_NOT_FOUND_ERROR"
    const val UNAUTHORIZED_ERROR = "UNAUTHORIZED_ERROR"
}

object RequestStatus {
    const val SUCCESS = "SUCCESS"
    const val FAILURE = "FAILURE"
}

object OtpGenStatus {
    const val GENERATED = "GENERATED"
    const val FAILED = "FAILED"
    const val SKIPPED = "SKIPPED"
}

object EmailSendStatus {
    const val SENT = "SENT"
    const val FAILED = "FAILED"
    const val PENDING = "PENDING"
}

object FirestoreCollections {
    const val BACKEND_DEBUGGER_LOGS = "backend_debugger_logs"
    const val OTP_VERIFICATIONS = "otp_verifications"
    const val OTP_RATE_LIMITS = "otp_rate_limits"
    const val CRITICAL_ALERTS = "critical_alerts"
    const val USERS = "users"
    const val DONORS = "donors"
    const val BLOOD_BANKS = "blood_banks"
    const val BANK_REGISTRATIONS = "bank_registrations"
    const val EMERGENCY_REQUESTS = "emergency_requests"
}
