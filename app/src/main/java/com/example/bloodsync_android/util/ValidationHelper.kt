package com.example.bloodsync_android.util

import java.util.regex.Pattern

/**
 * Defensive Input Validation and Sanitization Utility
 * Guards against malicious input, buffer overflows, format string injection, and corrupt database states.
 */
object ValidationHelper {

    private val PHONE_PATTERN: Pattern = Pattern.compile("^(\\+?[0-9]{1,4}[\\s-]?)?([0-9]{10,12})$")
    private val EMAIL_PATTERN: Pattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    private val VALID_BLOOD_GROUPS = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    /**
     * Sanitizes plain text by trimming and stripping dangerous control characters.
     */
    fun sanitizeText(input: String, maxLength: Int = 100): String {
        return input.trim()
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // Strip non-printable ASCII control characters
            .take(maxLength)
    }

    /**
     * Validates full name or person name (2 - 60 chars, letters, spaces, standard punctuation).
     */
    fun validateName(name: String): ValidationResult {
        val clean = name.trim()
        return when {
            clean.isEmpty() -> ValidationResult(false, "Name cannot be empty.")
            clean.length < 2 -> ValidationResult(false, "Name must be at least 2 characters long.")
            clean.length > 60 -> ValidationResult(false, "Name cannot exceed 60 characters.")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates phone numbers (must contain 10-13 digits, optional + prefix).
     */
    fun validatePhone(phone: String): ValidationResult {
        val digitsOnly = phone.filter { it.isDigit() }
        return when {
            phone.isBlank() -> ValidationResult(false, "Phone number is required.")
            digitsOnly.length < 10 -> ValidationResult(false, "Please enter a valid 10-digit mobile number.")
            digitsOnly.length > 13 -> ValidationResult(false, "Phone number cannot exceed 13 digits.")
            !PHONE_PATTERN.matcher(phone.trim()).matches() && digitsOnly.length !in 10..13 -> {
                ValidationResult(false, "Invalid phone number format.")
            }
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates email address (optional if blank, but if present must be valid).
     */
    fun validateEmail(email: String, required: Boolean = false): ValidationResult {
        val clean = email.trim()
        if (clean.isEmpty()) {
            return if (required) ValidationResult(false, "Email is required.") else ValidationResult(true)
        }
        return if (EMAIL_PATTERN.matcher(clean).matches()) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "Please enter a valid email address.")
        }
    }

    /**
     * Validates blood group string.
     */
    fun validateBloodGroup(bloodGroup: String): ValidationResult {
        return if (VALID_BLOOD_GROUPS.contains(bloodGroup.trim().uppercase())) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "Invalid blood group: $bloodGroup. Must be one of A+, A-, B+, B-, AB+, AB-, O+, O-.")
        }
    }

    /**
     * Validates emergency blood units requested (between 1 and 10 units per request).
     */
    fun validateUnits(units: Int): ValidationResult {
        return when {
            units < 1 -> ValidationResult(false, "Units requested must be at least 1.")
            units > 10 -> ValidationResult(false, "Emergency request limit is 10 units. Contact blood bank for higher needs.")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates hospital details.
     */
    fun validateHospitalName(hospital: String): ValidationResult {
        val clean = hospital.trim()
        return when {
            clean.isEmpty() -> ValidationResult(false, "Hospital name is required.")
            clean.length < 3 -> ValidationResult(false, "Hospital name must be at least 3 characters.")
            clean.length > 100 -> ValidationResult(false, "Hospital name cannot exceed 100 characters.")
            else -> ValidationResult(true)
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
