package com.bloodsync.util

/**
 * Clinical Blood Compatibility Helper
 * Implements standard ABO/Rh Whole Blood compatibility rules for patient transfusion & donor matching.
 *
 * Universal Red Cell Donor: O- (Can donate red blood cells to all ABO/Rh groups)
 * Universal Red Cell Recipient: AB+ (Can receive red blood cells from all ABO/Rh groups)
 */
object BloodCompatibilityHelper {

    /**
     * Map of Patient / Recipient Blood Group -> Set of clinically compatible Donor Blood Groups
     */
    private val RECIPIENT_TO_DONOR_COMPATIBILITY: Map<String, Set<String>> = mapOf(
        "O-"  to setOf("O-"),
        "O+"  to setOf("O+", "O-"),
        "A-"  to setOf("A-", "O-"),
        "A+"  to setOf("A+", "A-", "O+", "O-"),
        "B-"  to setOf("B-", "O-"),
        "B+"  to setOf("B+", "B-", "O+", "O-"),
        "AB-" to setOf("AB-", "A-", "B-", "O-"),
        "AB+" to setOf("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-")
    )

    /**
     * Map of Donor Blood Group -> Set of clinically compatible Recipient Blood Groups
     */
    private val DONOR_TO_RECIPIENT_COMPATIBILITY: Map<String, Set<String>> = mapOf(
        "O-"  to setOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"),
        "O+"  to setOf("O+", "A+", "B+", "AB+"),
        "A-"  to setOf("A-", "A+", "AB-", "AB+"),
        "A+"  to setOf("A+", "AB+"),
        "B-"  to setOf("B-", "B+", "AB-", "AB+"),
        "B+"  to setOf("B+", "AB+"),
        "AB-" to setOf("AB-", "AB+"),
        "AB+" to setOf("AB+")
    )

    /**
     * Checks if a donor blood group can donate to a specific patient blood group.
     */
    fun isDonorCompatible(donorGroup: String, patientGroup: String): Boolean {
        val cleanPatient = patientGroup.trim().uppercase()
        val cleanDonor = donorGroup.trim().uppercase()
        val compatibleDonors = RECIPIENT_TO_DONOR_COMPATIBILITY[cleanPatient] ?: return cleanDonor == cleanPatient
        return compatibleDonors.contains(cleanDonor)
    }

    /**
     * Returns the set of all donor blood groups that can donate to the specified patient blood group.
     */
    fun getCompatibleDonorGroups(patientGroup: String): Set<String> {
        val cleanPatient = patientGroup.trim().uppercase()
        return RECIPIENT_TO_DONOR_COMPATIBILITY[cleanPatient] ?: emptySet()
    }

    /**
     * Returns the set of recipient groups that can receive from the specified donor group.
     */
    fun getCompatibleRecipientGroups(donorGroup: String): Set<String> {
        val cleanDonor = donorGroup.trim().uppercase()
        return DONOR_TO_RECIPIENT_COMPATIBILITY[cleanDonor] ?: emptySet()
    }
}
