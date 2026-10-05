package com.bloodsync.config

import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * BloodSync Backend - Environment Configuration Loader
 * Converted from: backend/src/config/environment.js
 */

private val logger = KotlinLogging.logger {}

private val env = try {
    dotenv {
        directory = System.getProperty("user.dir")
        ignoreIfMissing = true
    }
} catch (e: Exception) {
    logger.warn { "[Environment] .env file not found, using system environment variables only." }
    null
}

private fun envOrNull(key: String): String? = env?.getOrNull(key) ?: System.getenv(key)
private fun envOrDefault(key: String, default: String): String = envOrNull(key)?.takeIf { it.isNotBlank() } ?: default

val NODE_ENV: String = envOrDefault("NODE_ENV", "development")
val PORT: Int = envOrDefault("PORT", "5000").toIntOrNull() ?: 5000

// -----------------------------------------------------------------
// SMTP Configuration
// -----------------------------------------------------------------
data class SmtpConfig(
    val host: String,
    val port: Int,
    val secure: Boolean,
    val user: String,
    val password: String,
    val fromName: String,
    val fromAddress: String,
)

val SMTP_CONFIG = SmtpConfig(
    host        = envOrDefault("SMTP_HOST", "smtp.gmail.com"),
    port        = envOrDefault("SMTP_PORT", "465").toIntOrNull() ?: 465,
    secure      = envOrDefault("SMTP_SECURE", "true") == "true"
                    || (envOrDefault("SMTP_PORT", "465").toIntOrNull() ?: 465) == 465,
    user        = envOrDefault("SMTP_USER", ""),
    password    = envOrDefault("SMTP_PASSWORD", ""),
    fromName    = envOrDefault("EMAIL_FROM_NAME", "BloodSync Security"),
    fromAddress = envOrNull("EMAIL_FROM_ADDRESS") ?: envOrNull("SMTP_USER") ?: "no-reply@bloodsync.org",
)

// -----------------------------------------------------------------
// Firebase Configuration
// -----------------------------------------------------------------
data class FirebaseConfig(
    val serviceAccountPath: String,
    val serviceAccountJson: String,
    val projectId: String,
    val clientEmail: String,
    val privateKey: String,
)

val FIREBASE_CONFIG = FirebaseConfig(
    serviceAccountPath  = envOrDefault("FIREBASE_SERVICE_ACCOUNT_PATH", ""),
    serviceAccountJson  = envOrNull("FIREBASE_SERVICE_ACCOUNT_JSON")
                            ?: envOrDefault("FIREBASE_SERVICE_ACCOUNT_BASE64", ""),
    projectId           = envOrDefault("FIREBASE_PROJECT_ID", "bloodsync-3b5cf"),
    clientEmail         = envOrDefault("FIREBASE_CLIENT_EMAIL", ""),
    privateKey          = (envOrNull("FIREBASE_PRIVATE_KEY") ?: "").replace("\\n", "\n"),
)

// -----------------------------------------------------------------
// Security & OTP Configuration
// -----------------------------------------------------------------
data class SecurityConfig(
    val otpExpiryMinutes: Int,
    val otpMaxAttempts: Int,
    val otpSaltSecret: String,
    val jwtSecret: String,
    val jwtExpiration: String,
)

val SECURITY_CONFIG = SecurityConfig(
    otpExpiryMinutes = envOrDefault("OTP_EXPIRY_MINUTES", "5").toIntOrNull() ?: 5,
    otpMaxAttempts   = envOrDefault("OTP_MAX_ATTEMPTS", "5").toIntOrNull() ?: 5,
    otpSaltSecret    = envOrDefault("OTP_SALT_SECRET", "bloodsync_default_secure_salt_change_me_in_prod"),
    jwtSecret        = envOrDefault("JWT_SECRET", "bloodsync_super_secret_jwt_key_change_me_in_prod"),
    jwtExpiration    = envOrDefault("JWT_EXPIRATION", "24h"),
)

// -----------------------------------------------------------------
// Rate Limiting Configuration
// -----------------------------------------------------------------
data class RateLimitConfig(
    val windowMinutes: Int,
    val maxRequestsPerWindow: Int,
    val cooldownSeconds: Int,
)

val RATE_LIMIT_CONFIG = RateLimitConfig(
    windowMinutes        = envOrDefault("RATE_LIMIT_WINDOW_MINUTES", "10").toIntOrNull() ?: 10,
    maxRequestsPerWindow = envOrDefault("RATE_LIMIT_MAX_REQUESTS_PER_WINDOW", "3").toIntOrNull() ?: 3,
    cooldownSeconds      = envOrDefault("RATE_LIMIT_COOLDOWN_SECONDS", "60").toIntOrNull() ?: 60,
)

// -----------------------------------------------------------------
// Monitoring & Webhook Configuration
// -----------------------------------------------------------------
data class MonitoringConfig(
    val alertWebhookUrl: String,
    val alertNotificationEmail: String,
    val enableRealtimeAlerts: Boolean,
)

val MONITORING_CONFIG = MonitoringConfig(
    alertWebhookUrl         = envOrDefault("ALERT_WEBHOOK_URL", ""),
    alertNotificationEmail  = envOrDefault("ALERT_NOTIFICATION_EMAIL", ""),
    enableRealtimeAlerts    = envOrDefault("ENABLE_REALTIME_ALERTS", "false") == "true",
)

// -----------------------------------------------------------------
// Configuration Validation
// -----------------------------------------------------------------
data class ConfigValidationResult(val isValid: Boolean, val warnings: List<String>)

fun validateConfig(): ConfigValidationResult {
    val warnings = mutableListOf<String>()

    if (SMTP_CONFIG.user.isBlank() || SMTP_CONFIG.password.isBlank()) {
        warnings.add("SMTP_USER or SMTP_PASSWORD is not set. Mailer will fail to send real emails.")
    }
    if (SECURITY_CONFIG.otpSaltSecret.contains("change_me")) {
        warnings.add("OTP_SALT_SECRET is using default placeholder. Set a strong secret in production.")
    }
    if (SECURITY_CONFIG.jwtSecret.contains("change_me")) {
        warnings.add("JWT_SECRET is using default placeholder. Set a strong secret in production.")
    }
    return ConfigValidationResult(isValid = true, warnings = warnings)
}
