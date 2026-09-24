package com.example.bloodsync_android.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class EligibilityStatus {
    ELIGIBLE,
    ELIGIBLE_FUTURE,
    NOT_ELIGIBLE
}

data class HealthRecord(
    val age: Int = 26,
    val gender: String = "Male", // "Male", "Female", "Other"
    val weightKg: Double = 68.0,
    val lastDonationDateString: String = "", // YYYY-MM-DD or MMM dd, yyyy
    val hemoglobinGPerDl: Double = 14.2,
    val systolicBp: Int = 120,
    val diastolicBp: Int = 80,
    val pulseBpm: Int = 72,
    val hasTattooRecent: Boolean = false,
    val hasColdFeverRecent: Boolean = false,
    val hasAntibioticsRecent: Boolean = false,
    val isPregnant: Boolean = false
) {
    // 3 Months = 90 Days strict interval rule
    val gapDaysRequired: Int = 90

    private fun parseLastDonationDate(): Date? {
        val formats = listOf("yyyy-MM-dd", "MMM dd, yyyy", "dd-MM-yyyy")
        for (format in formats) {
            try {
                val parsed = SimpleDateFormat(format, Locale.US).parse(lastDonationDateString)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    fun getNextEligibleDateMillis(): Long {
        val lastDate = parseLastDonationDate() ?: return System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.time = lastDate
        cal.add(Calendar.DAY_OF_YEAR, gapDaysRequired)
        return cal.timeInMillis
    }

    fun getNextEligibleDateFormatted(): String {
        val millis = getNextEligibleDateMillis()
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(millis))
    }

    fun isAppointmentDateAllowed(dateStr: String): Boolean {
        val targetMillis = parseAnyDate(dateStr) ?: return true
        val minEligibleMillis = getNextEligibleDateMillis()
        // Allow if target is on or after minEligibleMillis (comparing day level)
        val calTarget = Calendar.getInstance().apply { timeInMillis = targetMillis }
        val calMin = Calendar.getInstance().apply { timeInMillis = minEligibleMillis }

        // Strip time of day
        calTarget.set(Calendar.HOUR_OF_DAY, 0)
        calTarget.set(Calendar.MINUTE, 0)
        calTarget.set(Calendar.SECOND, 0)
        calTarget.set(Calendar.MILLISECOND, 0)

        calMin.set(Calendar.HOUR_OF_DAY, 0)
        calMin.set(Calendar.MINUTE, 0)
        calMin.set(Calendar.SECOND, 0)
        calMin.set(Calendar.MILLISECOND, 0)

        return !calTarget.before(calMin)
    }

    private fun parseAnyDate(dateStr: String): Long? {
        val formats = listOf("MMM dd, yyyy", "yyyy-MM-dd", "EEE, MMM dd", "MMM dd")
        for (f in formats) {
            try {
                val d = SimpleDateFormat(f, Locale.US).parse(dateStr)
                if (d != null) {
                    val cal = Calendar.getInstance()
                    val currentYear = cal.get(Calendar.YEAR)
                    cal.time = d
                    if (cal.get(Calendar.YEAR) == 1970) {
                        cal.set(Calendar.YEAR, currentYear)
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun calculateEligibility(): EligibilityResult {
        val ineligibilityReasons = mutableListOf<String>()

        // 1. Age check (18 - 65)
        if (age < 18) {
            ineligibilityReasons.add("Must be at least 18 years old (currently $age)")
        } else if (age > 65) {
            ineligibilityReasons.add("Maximum donation age is 65 years (currently $age)")
        }

        // 2. Weight check (>= 50 kg)
        if (weightKg < 50.0) {
            ineligibilityReasons.add("Minimum weight required is 50 kg (currently ${String.format(Locale.US, "%.1f", weightKg)} kg)")
        }

        // 3. Hemoglobin check (>= 12.5 for female, >= 13.0 for male)
        val minHb = if (gender.equals("Female", ignoreCase = true)) 12.5 else 13.0
        if (hemoglobinGPerDl < minHb) {
            ineligibilityReasons.add("Hemoglobin must be at least $minHb g/dL (currently ${String.format(Locale.US, "%.1f", hemoglobinGPerDl)} g/dL)")
        }

        // 4. Blood pressure check (Systolic 90-140, Diastolic 60-90)
        if (systolicBp !in 90..140 || diastolicBp !in 60..90) {
            ineligibilityReasons.add("Blood pressure $systolicBp/$diastolicBp mmHg is outside acceptable range (90-140 / 60-90)")
        }

        // 5. Medical conditions / temporary deferrals
        if (hasTattooRecent) {
            ineligibilityReasons.add("Tattoos or body piercings require a 6-month deferral period")
        }
        if (hasColdFeverRecent) {
            ineligibilityReasons.add("Active cold, flu, or fever in the last 14 days")
        }
        if (hasAntibioticsRecent) {
            ineligibilityReasons.add("Antibiotic medication taken within the last 7 days")
        }
        if (isPregnant && gender.equals("Female", ignoreCase = true)) {
            ineligibilityReasons.add("Donations are not permitted during pregnancy or within 6 months postpartum")
        }

        // 6. 3-Month Donation Gap Rule (90 days interval)
        var daysRemainingForNextDonation = 0
        var nextEligibleDateFormatted = ""

        val lastDate = parseLastDonationDate()
        if (lastDate != null) {
            val cal = Calendar.getInstance()
            cal.time = lastDate
            cal.add(Calendar.DAY_OF_YEAR, gapDaysRequired)
            val eligibleDate = cal.time

            val today = Date()
            val diffMillis = eligibleDate.time - today.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

            if (diffDays > 0) {
                daysRemainingForNextDonation = diffDays
                val displaySdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                nextEligibleDateFormatted = displaySdf.format(eligibleDate)
            }
        }

        if (ineligibilityReasons.isNotEmpty()) {
            return EligibilityResult(
                status = EligibilityStatus.NOT_ELIGIBLE,
                reasons = ineligibilityReasons,
                daysRemaining = daysRemainingForNextDonation,
                nextEligibleDate = nextEligibleDateFormatted
            )
        }

        if (daysRemainingForNextDonation > 0) {
            return EligibilityResult(
                status = EligibilityStatus.ELIGIBLE_FUTURE,
                reasons = listOf("3-Month rule: Donors can only donate once every 3 months (90 days). $daysRemainingForNextDonation days remaining."),
                daysRemaining = daysRemainingForNextDonation,
                nextEligibleDate = nextEligibleDateFormatted
            )
        }

        return EligibilityResult(
            status = EligibilityStatus.ELIGIBLE,
            reasons = emptyList(),
            daysRemaining = 0,
            nextEligibleDate = "Today"
        )
    }
}

data class EligibilityResult(
    val status: EligibilityStatus,
    val reasons: List<String>,
    val daysRemaining: Int,
    val nextEligibleDate: String
)
