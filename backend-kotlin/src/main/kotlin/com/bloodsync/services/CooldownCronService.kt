package com.bloodsync.services

import com.bloodsync.config.Constants.REQUEST_STATUS
import com.bloodsync.config.FirebaseAdmin.db
import com.google.cloud.firestore.FieldValue
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CooldownCronService(
    private val loggerBotService: LoggerBotService
) {
    private val cooldownDays = 90L
    private val msPerDay = 24 * 60 * 60 * 1000L

    data class ScanResult(val scanned: Int, val restoredToAvailable: Int, val keptInCooldown: Int, val errors: Int, val error: String? = null)

    @Scheduled(initialDelay = 10000, fixedRate = 24 * 60 * 60 * 1000L) // Initial 10s, then daily
    suspend fun runCooldownScan(): ScanResult {
        if (db == null) {
            println("[CooldownCron] Firestore DB not available. Skipping cooldown scan.")
            return ScanResult(0, 0, 0, 0)
        }

        val now = System.currentTimeMillis()
        println("[CooldownCron] Starting WHO 90-Day donor cooldown audit at ${Instant.ofEpochMilli(now)}...")

        var scanned = 0
        var restoredToAvailable = 0
        var keptInCooldown = 0
        var errors = 0

        try {
            val usersSnap = db.collection("users").get().get()
            scanned = usersSnap.size()

            for (doc in usersSnap.documents) {
                try {
                    val user = doc.data
                    val userId = doc.id

                    var donationTime: Long? = null

                    val lastDonationTimestamp = user["lastDonationTimestamp"]
                    if (lastDonationTimestamp != null) {
                        donationTime = when (lastDonationTimestamp) {
                            is Number -> lastDonationTimestamp.toLong()
                            is com.google.cloud.Timestamp -> lastDonationTimestamp.toDate().time
                            else -> null
                        }
                    } else {
                        val dateString = (user["lastDonationDateString"] ?: user["lastDonationDate"]) as? String
                        if (dateString != null) {
                            try {
                                val parsed = Instant.parse(dateString).toEpochMilli()
                                donationTime = parsed
                            } catch (e: Exception) {
                                // ignore parse error
                            }
                        }
                    }

                    if (donationTime == null) continue

                    val daysElapsed = (now - donationTime) / msPerDay.toDouble()

                    if (daysElapsed >= cooldownDays) {
                        if (user["isAvailableDonor"] == false || user["donorStatus"] == "COOLDOWN") {
                            println("[CooldownCron] Donor $userId (${user["name"] ?: "Donor"}) completed ${daysElapsed.toInt()} days cooldown. Restoring to AVAILABLE.")

                            val updatePayload = mapOf(
                                "isAvailableDonor" to true,
                                "donorStatus" to "AVAILABLE",
                                "cooldownDaysRemaining" to 0,
                                "cooldownLiftedAt" to FieldValue.serverTimestamp()
                            )

                            db.collection("users").document(userId).set(updatePayload, com.google.cloud.firestore.SetOptions.merge())
                            db.collection("donors").document(userId).set(updatePayload, com.google.cloud.firestore.SetOptions.merge())
                            restoredToAvailable++
                        }
                    } else {
                        val daysRemaining = Math.ceil(cooldownDays - daysElapsed).toInt()
                        keptInCooldown++

                        if (user["isAvailableDonor"] != false || user["donorStatus"] != "COOLDOWN") {
                            println("[CooldownCron] Enforcing WHO cooldown for $userId (${user["name"] ?: "Donor"}). $daysRemaining days remaining.")

                            val updatePayload = mapOf(
                                "isAvailableDonor" to false,
                                "donorStatus" to "COOLDOWN",
                                "cooldownDaysRemaining" to daysRemaining,
                                "cooldownEnforcedAt" to FieldValue.serverTimestamp()
                            )

                            db.collection("users").document(userId).set(updatePayload, com.google.cloud.firestore.SetOptions.merge())
                            db.collection("donors").document(userId).set(updatePayload, com.google.cloud.firestore.SetOptions.merge())
                        }
                    }
                } catch (itemErr: Exception) {
                    println("[CooldownCron] Error processing user ${doc.id}: ${itemErr.message}")
                    errors++
                }
            }

            println("[CooldownCron] Audit complete. Scanned: $scanned, Restored: $restoredToAvailable, Cooldown Active: $keptInCooldown, Errors: $errors")

            loggerBotService.logOtpEvent(
                requestId = "cooldown_cron_$now",
                rawEmail = "scheduler@bloodsync.org",
                requestStatus = REQUEST_STATUS.SUCCESS,
                otpGenStatus = "SKIPPED",
                emailSendingStatus = "SKIPPED",
                errorCategory = null,
                errorMessage = null,
                metadata = mapOf(
                    "action" to "WHO_90_DAY_COOLDOWN_SCAN",
                    "scanned" to scanned,
                    "restoredToAvailable" to restoredToAvailable,
                    "keptInCooldown" to keptInCooldown,
                    "errors" to errors
                )
            )

            return ScanResult(scanned, restoredToAvailable, keptInCooldown, errors)
        } catch (err: Exception) {
            println("[CooldownCron] Fatal error during cooldown scan: ${err.message}")
            return ScanResult(scanned, restoredToAvailable, keptInCooldown, errors + 1, err.message)
        }
    }
}
