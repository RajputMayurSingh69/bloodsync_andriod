package com.bloodsync.services

import com.bloodsync.config.FirebaseConfig
import com.bloodsync.config.FirestoreCollections
import com.google.cloud.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BloodSync Backend - WHO 90-Day Donor Cooldown Cron Service (Kotlin)
 * Mirrors cooldownCronService.js 1:1
 */
object CooldownCronService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val COOLDOWN_DAYS = 90
    private const val MS_PER_DAY = 24 * 60 * 60 * 1000L

    data class CooldownResult(val scanned: Int, val restoredToAvailable: Int, val keptInCooldown: Int, val errors: Int)

    suspend fun runCooldownScan(): CooldownResult {
        val db = FirebaseConfig.firestore ?: return CooldownResult(0, 0, 0, 0).also {
            println("[CooldownCron] Firestore DB not available. Skipping cooldown scan.")
        }

        val now = System.currentTimeMillis()
        println("[CooldownCron] Starting WHO 90-Day donor cooldown audit at ${java.time.Instant.now()}...")

        var scanned = 0
        var restoredToAvailable = 0
        var keptInCooldown = 0
        var errors = 0

        return withContext(Dispatchers.IO) {
            try {
                val usersSnap = db.collection(FirestoreCollections.USERS).get().get()
                scanned = usersSnap.size()

                for (doc in usersSnap.documents) {
                    try {
                        val user = doc.data ?: continue
                        val userId = doc.id

                        var donationTime: Long? = null
                        val tsField = user["lastDonationTimestamp"]
                        when (tsField) {
                            is Long -> donationTime = tsField
                            is com.google.cloud.firestore.Timestamp -> donationTime = tsField.toDate().time
                            is String -> donationTime = runCatching { java.util.Date(tsField).time }.getOrNull()
                        }

                        if (donationTime == null) {
                            val dateStr = user["lastDonationDateString"] as? String ?: user["lastDonationDate"] as? String
                            if (dateStr != null) donationTime = runCatching { java.text.SimpleDateFormat("yyyy-MM-dd").parse(dateStr)?.time }.getOrNull()
                        }

                        if (donationTime == null) continue

                        val daysElapsed = (now - donationTime) / MS_PER_DAY.toDouble()

                        if (daysElapsed >= COOLDOWN_DAYS) {
                            if (user["isAvailableDonor"] == false || user["donorStatus"] == "COOLDOWN") {
                                println("[CooldownCron] Donor $userId completed ${daysElapsed.toInt()} days cooldown. Restoring to AVAILABLE.")
                                val update = mapOf(
                                    "isAvailableDonor" to true,
                                    "donorStatus" to "AVAILABLE",
                                    "cooldownDaysRemaining" to 0,
                                    "cooldownLiftedAt" to FieldValue.serverTimestamp()
                                )
                                db.collection(FirestoreCollections.USERS).document(userId).set(update, com.google.cloud.firestore.SetOptions.merge()).get()
                                db.collection(FirestoreCollections.DONORS).document(userId).set(update, com.google.cloud.firestore.SetOptions.merge()).get()
                                restoredToAvailable++
                            }
                        } else {
                            val daysRemaining = (COOLDOWN_DAYS - daysElapsed).toInt() + 1
                            keptInCooldown++
                            if (user["isAvailableDonor"] != false || user["donorStatus"] != "COOLDOWN") {
                                println("[CooldownCron] Enforcing WHO cooldown for $userId. $daysRemaining days remaining.")
                                val update = mapOf(
                                    "isAvailableDonor" to false,
                                    "donorStatus" to "COOLDOWN",
                                    "cooldownDaysRemaining" to daysRemaining,
                                    "cooldownEnforcedAt" to FieldValue.serverTimestamp()
                                )
                                db.collection(FirestoreCollections.USERS).document(userId).set(update, com.google.cloud.firestore.SetOptions.merge()).get()
                                db.collection(FirestoreCollections.DONORS).document(userId).set(update, com.google.cloud.firestore.SetOptions.merge()).get()
                            }
                        }
                    } catch (e: Exception) {
                        println("[CooldownCron] Error processing user ${doc.id}: ${e.message}")
                        errors++
                    }
                }

                println("[CooldownCron] Audit complete. Scanned: $scanned, Restored: $restoredToAvailable, Cooldown Active: $keptInCooldown, Errors: $errors")
                CooldownResult(scanned, restoredToAvailable, keptInCooldown, errors)
            } catch (e: Exception) {
                println("[CooldownCron] Fatal error during cooldown scan: ${e.message}")
                CooldownResult(scanned, restoredToAvailable, keptInCooldown, errors + 1)
            }
        }
    }

    fun startDailySchedule() {
        scope.launch {
            delay(10_000) // Initial run 10s after boot
            runCatching { runCooldownScan() }.onFailure { println("[CooldownCron] Initial scan error: ${it.message}") }

            while (true) {
                delay(24 * 60 * 60 * 1000L) // Every 24 hours
                runCatching { runCooldownScan() }.onFailure { println("[CooldownCron] Periodic scan error: ${it.message}") }
            }
        }
        println("[CooldownCron] Daily WHO 90-Day cooldown scheduler started.")
    }
}

/**
 * BloodSync Backend - Firestore Cleanup Cron Service (Kotlin)
 * Mirrors cleanupCronService.js 1:1
 */
object CleanupCronService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val BATCH_SIZE = 400
    private const val RATE_LIMIT_RETENTION_MS = 24 * 60 * 60 * 1000L
    private const val LOG_RETENTION_DAYS = 14

    suspend fun runFullCleanup(): Map<String, Any> {
        val start = System.currentTimeMillis()
        println("[CleanupService] Starting scheduled database purge at ${java.time.Instant.now()}...")

        val rateLimitsDeleted = purgeCollection(
            FirestoreCollections.OTP_RATE_LIMITS, "lastRequestAt",
            System.currentTimeMillis() - RATE_LIMIT_RETENTION_MS
        )
        val logsDeleted = purgeCollection(
            FirestoreCollections.BACKEND_DEBUGGER_LOGS, "timestamp",
            System.currentTimeMillis() - LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L
        )
        val otpsDeleted = purgeCollection(
            FirestoreCollections.OTP_VERIFICATIONS, "expiresAt",
            System.currentTimeMillis()
        )

        val duration = System.currentTimeMillis() - start
        val total = rateLimitsDeleted + logsDeleted + otpsDeleted
        println("[CleanupService] Purge completed in ${duration}ms: $rateLimitsDeleted rate-limits, $logsDeleted logs, $otpsDeleted OTPs purged.")

        return mapOf(
            "success" to true,
            "timestamp" to java.time.Instant.now().toString(),
            "durationMs" to duration,
            "totalDocumentsCleaned" to total,
            "rateLimitsPurged" to rateLimitsDeleted,
            "debuggerLogsPurged" to logsDeleted,
            "expiredOtpsPurged" to otpsDeleted
        )
    }

    private suspend fun purgeCollection(collectionName: String, timestampField: String, cutoffMs: Long): Int {
        val db = FirebaseConfig.firestore ?: return 0
        return withContext(Dispatchers.IO) {
            runCatching {
                val cutoff = com.google.cloud.firestore.Timestamp.ofTimeSecondsAndNanos(cutoffMs / 1000, 0)
                val snap = db.collection(collectionName).whereLessThan(timestampField, cutoff).limit(BATCH_SIZE).get().get()
                if (snap.isEmpty) return@withContext 0
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().get()
                snap.size()
            }.getOrElse { e ->
                println("[CleanupService] Error purging $collectionName: ${e.message}")
                0
            }
        }
    }

    fun startWeeklySchedule() {
        scope.launch {
            delay(20_000) // Initial run 20s after boot
            runCatching { runFullCleanup() }.onFailure { println("[CleanupService] Initial cleanup error: ${it.message}") }

            while (true) {
                delay(7 * 24 * 60 * 60 * 1000L) // Every 7 days
                runCatching { runFullCleanup() }.onFailure { println("[CleanupService] Periodic cleanup error: ${it.message}") }
            }
        }
        println("[CleanupService] Weekly Firestore cleanup scheduler started.")
    }
}
