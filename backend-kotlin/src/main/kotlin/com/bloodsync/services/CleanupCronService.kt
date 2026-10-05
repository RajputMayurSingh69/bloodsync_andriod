package com.bloodsync.services

import com.bloodsync.config.Constants.FIRESTORE_COLLECTIONS
import com.bloodsync.config.FirebaseAdmin.db
import com.google.cloud.Timestamp
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CleanupCronService {
    private val msPerDay = 24 * 60 * 60 * 1000L
    private val rateLimitRetentionMs = 24 * 60 * 60 * 1000L // 24 hours
    private val logRetentionDays = 14L // 14 days
    private val batchSize = 400

    private suspend fun purgeExpiredRateLimits(): Map<String, Any> {
        if (db == null) return mapOf("deleted" to 0, "scanned" to 0)

        val cutoffMillis = System.currentTimeMillis() - rateLimitRetentionMs
        val cutoffTimestamp = Timestamp.of(java.util.Date(cutoffMillis))
        var deletedCount = 0
        var scannedCount = 0

        try {
            val snapshot = db.collection(FIRESTORE_COLLECTIONS.OTP_RATE_LIMITS)
                .whereLessThan("lastRequestAt", cutoffTimestamp)
                .limit(batchSize)
                .get()
                .get()

            scannedCount = snapshot.size()

            if (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                    deletedCount++
                }
                batch.commit().get()
            }

            return mapOf("deleted" to deletedCount, "scanned" to scannedCount)
        } catch (e: Exception) {
            println("[CleanupService] Error purging otp_rate_limits: ${e.message}")
            return mapOf("deleted" to deletedCount, "scanned" to scannedCount, "error" to (e.message ?: ""))
        }
    }

    private suspend fun purgeOldDebuggerLogs(retentionDays: Long = logRetentionDays): Map<String, Any> {
        if (db == null) return mapOf("deleted" to 0, "scanned" to 0)

        val cutoffMillis = System.currentTimeMillis() - retentionDays * msPerDay
        val cutoffTimestamp = Timestamp.of(java.util.Date(cutoffMillis))
        var deletedCount = 0
        var scannedCount = 0

        try {
            val snapshot = db.collection(FIRESTORE_COLLECTIONS.BACKEND_DEBUGGER_LOGS)
                .whereLessThan("timestamp", cutoffTimestamp)
                .limit(batchSize)
                .get()
                .get()

            scannedCount = snapshot.size()

            if (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                    deletedCount++
                }
                batch.commit().get()
            }

            return mapOf("deleted" to deletedCount, "scanned" to scannedCount)
        } catch (e: Exception) {
            println("[CleanupService] Error purging backend_debugger_logs: ${e.message}")
            return mapOf("deleted" to deletedCount, "scanned" to scannedCount, "error" to (e.message ?: ""))
        }
    }

    private suspend fun purgeExpiredOtps(): Map<String, Any> {
        if (db == null) return mapOf("deleted" to 0, "scanned" to 0)

        val nowTimestamp = Timestamp.now()
        var deletedCount = 0
        var scannedCount = 0

        try {
            val snapshot = db.collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS)
                .whereLessThan("expiresAt", nowTimestamp.toString()) // Requires correct type query based on DB mapping
                .limit(batchSize)
                .get()
                .get()

            scannedCount = snapshot.size()

            if (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                    deletedCount++
                }
                batch.commit().get()
            }

            return mapOf("deleted" to deletedCount, "scanned" to scannedCount)
        } catch (e: Exception) {
            println("[CleanupService] Error purging otp_verifications: ${e.message}")
            return mapOf("deleted" to deletedCount, "scanned" to scannedCount, "error" to (e.message ?: ""))
        }
    }

    @Scheduled(initialDelay = 20000, fixedRate = 7 * 24 * 60 * 60 * 1000L) // Initial 20s, then weekly
    suspend fun runFullCleanup(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        println("[CleanupService] Starting scheduled database purge at ${java.time.Instant.ofEpochMilli(startTime)}...")

        val rateLimitsResult = purgeExpiredRateLimits()
        val debuggerLogsResult = purgeOldDebuggerLogs()
        val expiredOtpsResult = purgeExpiredOtps()

        val durationMs = System.currentTimeMillis() - startTime
        val totalDeleted = (rateLimitsResult["deleted"] as Int) +
                (debuggerLogsResult["deleted"] as Int) +
                (expiredOtpsResult["deleted"] as Int)

        val summary = mapOf(
            "success" to true,
            "timestamp" to java.time.Instant.now().toString(),
            "durationMs" to durationMs,
            "totalDocumentsCleaned" to totalDeleted,
            "rateLimitsPurged" to (rateLimitsResult["deleted"] as Int),
            "debuggerLogsPurged" to (debuggerLogsResult["deleted"] as Int),
            "expiredOtpsPurged" to (expiredOtpsResult["deleted"] as Int)
        )

        println("[CleanupService] Purge completed in ${durationMs}ms: ${summary["rateLimitsPurged"]} rate-limits, ${summary["debuggerLogsPurged"]} logs, ${summary["expiredOtpsPurged"]} OTPs purged.")

        try {
            if (db != null) {
                db.collection(FIRESTORE_COLLECTIONS.BACKEND_DEBUGGER_LOGS).add(mapOf(
                    "timestamp" to com.google.cloud.firestore.FieldValue.serverTimestamp(),
                    "requestId" to "cleanup_${System.currentTimeMillis()}",
                    "requestStatus" to "SUCCESS",
                    "serverEnvironment" to (System.getenv("NODE_ENV") ?: "development"),
                    "action" to "FIRESTORE_TTL_AUTO_CLEANUP",
                    "metadata" to summary
                )).get()
            }
        } catch (e: Exception) {
            // Non-blocking
        }

        return summary
    }
}
