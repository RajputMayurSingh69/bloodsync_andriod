package com.bloodsync.utils

/**
 * BloodSync Backend - Request Validators
 * Converted from: backend/src/utils/validator.js
 */

// Strict RFC 5322 compliant regex for standard email addresses
private val EMAIL_REGEX = Regex(
    """^[a-zA-Z0-9.!#${'$'}%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$"""
)

/**
 * Validates whether the given [email] is a valid email format.
 */
fun isValidEmail(email: String?): Boolean {
    if (email.isNullOrBlank()) return false
    val trimmed = email.trim()
    if (trimmed.length < 5 || trimmed.length > 254) return false
    return EMAIL_REGEX.matches(trimmed)
}

/**
 * Validates if the OTP is a valid 6-digit numeric string.
 */
fun isValidOtpFormat(otp: String?): Boolean {
    if (otp == null) return false
    val otpStr = otp.trim()
    return Regex("""^\d{6}$""").matches(otpStr)
}

/**
 * Validates a phone number (Indian & international).
 * Accepts optional leading +, spaces, dashes, parens.
 * Requires minimum 10 digits.
 */
fun isValidPhone(phone: String?): Boolean {
    if (phone.isNullOrBlank()) return false
    val digits = phone.filter { it.isDigit() }
    return digits.length in 10..15
}

/**
 * Validates that a string is non-empty after trimming.
 *
 * @param value     The string to validate
 * @param minLength Minimum required length after trimming (default 1)
 */
fun isNonEmptyString(value: String?, minLength: Int = 1): Boolean {
    if (value.isNullOrBlank()) return false
    return value.trim().length >= minLength
}
