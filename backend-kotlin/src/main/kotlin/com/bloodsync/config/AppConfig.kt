package com.bloodsync.config

import io.github.cdimascio.dotenv.dotenv

/**
 * BloodSync Backend - Application Configuration
 *
 * Loads all environment variables from .env file (mirrors Node.js dotenv).
 * Provides typed access with sensible defaults.
 */
object AppConfig {
    private val env = dotenv {
        ignoreIfMissing = true
    }

    val port: Int get() = env["PORT", "3000"].toIntOrNull() ?: 3000
    val environment: String get() = env["NODE_ENV", "development"]

    // SMTP / Gmail Configuration
    val smtpHost: String get() = env["SMTP_HOST", "smtp.gmail.com"]
    val smtpPort: Int get() = env["SMTP_PORT", "587"].toIntOrNull() ?: 587
    val smtpUser: String get() = env["SMTP_USER", ""]
    val smtpPassword: String get() = env["SMTP_PASSWORD", ""]
    val smtpFromName: String get() = env["SMTP_FROM_NAME", "BloodSync"]
    val smtpFromAddress: String get() = env["SMTP_FROM_ADDRESS", smtpUser]

    // JWT Configuration
    val jwtSecret: String get() = env["JWT_SECRET", "bloodsync_dev_secret_change_in_production"]
    val jwtExpiration: String get() = env["JWT_EXPIRATION", "24h"]

    // Security configuration
    val otpExpiryMinutes: Int get() = env["OTP_EXPIRY_MINUTES", "5"].toIntOrNull() ?: 5
    val otpMaxAttempts: Int get() = env["OTP_MAX_ATTEMPTS", "5"].toIntOrNull() ?: 5
    val otpHmacSecret: String get() = env["OTP_HMAC_SECRET", "bloodsync_otp_hmac_secret"]

    // Rate Limiting
    val rateLimitWindowMinutes: Int get() = env["RATE_LIMIT_WINDOW_MINUTES", "10"].toIntOrNull() ?: 10
    val rateLimitMaxRequests: Int get() = env["RATE_LIMIT_MAX_REQUESTS", "3"].toIntOrNull() ?: 3
    val rateLimitCooldownSeconds: Int get() = env["RATE_LIMIT_COOLDOWN_SECONDS", "60"].toIntOrNull() ?: 60

    // Firebase Service Account path
    val firebaseServiceAccountPath: String get() = env["FIREBASE_SERVICE_ACCOUNT_PATH", ""]
    val firebaseDatabaseUrl: String get() = env["FIREBASE_DATABASE_URL", ""]

    // Monitoring/Webhook
    val alertWebhookUrl: String get() = env["ALERT_WEBHOOK_URL", ""]
    val telegramChatId: String get() = env["TELEGRAM_CHAT_ID", ""]
}
