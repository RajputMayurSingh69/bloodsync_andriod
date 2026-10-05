package com.bloodsync.utils

/**
 * BloodSync Backend - Sanitization & Privacy Utilities
 * Converted from: backend/src/utils/sanitizer.js
 */

/**
 * Masks an email address for privacy-compliant logging and UI display.
 *
 * Examples:
 *   "alex@gmail.com"       -> "a***@gmail.com"
 *   "john.doe@domain.com"  -> "j***e@domain.com"
 *   "a@b.com"              -> "a***@b.com"
 */
fun maskEmail(email: String?): String {
    if (email.isNullOrBlank()) return "unknown@masked.local"
    val parts = email.trim().lowercase().split("@")
    if (parts.size != 2) return "invalid_email_format"

    val username = parts[0]
    val domain   = parts[1]

    return when {
        username.length <= 1 -> "$username***@$domain"
        username.length == 2 -> "${username[0]}***@$domain"
        else -> {
            val first = username.first()
            val last  = username.last()
            "$first***$last@$domain"
        }
    }
}

/**
 * Anonymizes client IP addresses for safe logging.
 * Examples:
 *   "192.168.1.45"                -> "192.168.1.xxx"
 *   "2001:db8::1"                 -> "2001:db8:xxxx:xxxx"
 */
fun sanitizeIp(ip: String?): String {
    if (ip.isNullOrBlank()) return "0.0.0.0"

    // IPv4
    if (ip.contains('.')) {
        val segments = ip.split('.')
        if (segments.size == 4) {
            return "${segments[0]}.${segments[1]}.${segments[2]}.xxx"
        }
    }

    // IPv6
    if (ip.contains(':')) {
        val segments = ip.split(':')
        return "${segments.take(3).joinToString(":")}:xxxx:xxxx"
    }

    return "anonymized-ip"
}

private val sensitiveKeys = setOf(
    "password", "pass", "smtp_password", "private_key",
    "privatekey", "otp", "secret", "jwt_secret", "token", "authorization",
)

/**
 * Deeply sanitizes a map or string before recording to Firestore or external logs.
 * Strips any occurrence of passwords, private keys, authorization headers, or plain OTPs.
 */
@Suppress("UNCHECKED_CAST")
fun sanitizeDetails(input: Any?): Any? {
    if (input == null) return null

    return when (input) {
        is String -> input
            .replace(Regex("password\\s*[:=]\\s*[\"']?[^\"',\\s]+[\"']?", RegexOption.IGNORE_CASE), "password:[REDACTED]")
            .replace(Regex("pass\\s*[:=]\\s*[\"']?[^\"',\\s]+[\"']?",     RegexOption.IGNORE_CASE), "pass:[REDACTED]")
            .replace(Regex("otp\\s*[:=]\\s*[\"']?\\d{4,8}[\"']?",        RegexOption.IGNORE_CASE), "otp:[REDACTED]")
            .replace(Regex("Bearer\\s+[A-Za-z0-9-_=.]+",                  RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            .replace(Regex("-----BEGIN PRIVATE KEY-----[\\s\\S]*?-----END PRIVATE KEY-----", RegexOption.IGNORE_CASE), "[REDACTED_PRIVATE_KEY]")

        is Map<*, *> -> {
            val result = mutableMapOf<Any?, Any?>()
            for ((k, v) in input) {
                val lowerKey = k.toString().lowercase()
                val isSensitive = sensitiveKeys.any { lowerKey.contains(it) }
                result[k] = if (isSensitive) "[REDACTED]" else sanitizeDetails(v)
            }
            result
        }

        is List<*> -> input.map { sanitizeDetails(it) }

        else -> input
    }
}
