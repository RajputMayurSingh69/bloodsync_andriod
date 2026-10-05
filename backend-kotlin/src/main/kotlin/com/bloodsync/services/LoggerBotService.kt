package com.bloodsync.services

import com.bloodsync.config.ErrorCategories
import com.bloodsync.config.FirebaseAdmin
import com.bloodsync.config.FirestoreCollections
import com.bloodsync.config.NODE_ENV
import com.bloodsync.config.RequestStatus
import com.bloodsync.utils.maskEmail
import com.bloodsync.utils.sanitizeDetails
import com.google.cloud.firestore.FieldValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

/**
 * BloodSync Backend - Firebase Debugger Bot & Real-Time Monitoring Service
 * Converted from: backend/src/services/loggerBotService.js
 *
 * Records system events into `backend_debugger_logs` and triggers alerts
 * for critical failures without exposing PII or credentials.
 */

private val logger = KotlinLogging.logger {}

data class OtpEventLog(
    val requestId: String,
    val rawEmail: String,
    val requestStatus: String,
    val otpGenStatus: String,
    val emailSendingStatus: String,
    val errorCategory: String? = null,
    val errorMessage: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

object LoggerBotService {

    suspend fun logOtpEvent(event: OtpEventLog) {
        val maskedUserEmail = maskEmail(event.rawEmail)
        val sanitizedMetadata = sanitizeDetails(event.metadata) as? Map<*, *> ?: emptyMap<String, Any?>()
        val sanitizedErrorMessage = event.errorMessage?.let { sanitizeDetails(it) as? String }

        // 1. Console output
        val level = if (event.requestStatus == RequestStatus.SUCCESS) "INFO" else "ERROR"
        logger.info {
            "[DebuggerBot][$level] [${event.requestId}] status=${event.requestStatus} " +
                "email=$maskedUserEmail otpGen=${event.otpGenStatus} " +
                "emailSend=${event.emailSendingStatus} errCat=${event.errorCategory ?: "none"}"
        }

        // 2. Persist to Firestore `backend_debugger_logs`
        val db = FirebaseAdmin.db
        if (db != null) {
            try {
                val logEntry = mapOf(
                    "timestamp"          to FieldValue.serverTimestamp(),
                    "requestId"          to event.requestId,
                    "maskedUserEmail"    to maskedUserEmail,
                    "requestStatus"      to event.requestStatus,
                    "otpGenStatus"       to event.otpGenStatus,
                    "emailSendingStatus" to event.emailSendingStatus,
                    "errorCategory"      to event.errorCategory,
                    "errorMessage"       to sanitizedErrorMessage,
                    "serverEnvironment"  to NODE_ENV,
                    "expiresAt"          to com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
                        Instant.now().plusSeconds(14L * 24 * 60 * 60).epochSecond, 0
                    ),
                    "metadata"           to sanitizedMetadata,
                )
                db.collection(FirestoreCollections.BACKEND_DEBUGGER_LOGS).add(logEntry).get()
            } catch (e: Exception) {
                logger.error { "[DebuggerBot] CRITICAL: Failed to write debugger log to Firestore: ${e.message}" }
            }
        } else {
            logger.warn { "[DebuggerBot] Firestore db not available. Event logged to stdout only." }
        }

        // 3. Evaluate Critical Alert Triggers
        if (event.requestStatus == RequestStatus.FAILURE && event.errorCategory != null) {
            evaluateAlertConditions(
                requestId        = event.requestId,
                maskedUserEmail  = maskedUserEmail,
                errorCategory    = event.errorCategory,
                errorMessage     = sanitizedErrorMessage,
                sanitizedMetadata = sanitizedMetadata,
            )
        }
    }

    private suspend fun evaluateAlertConditions(
        requestId: String,
        maskedUserEmail: String,
        errorCategory: String,
        errorMessage: String?,
        sanitizedMetadata: Map<*, *>,
    ) {
        val criticalCategories = setOf(
            ErrorCategories.SMTP_AUTH_ERROR,
            ErrorCategories.EMAIL_SEND_ERROR,
            ErrorCategories.EMAIL_CONFIGURATION_ERROR,
            ErrorCategories.FIREBASE_ERROR,
            ErrorCategories.OTP_DATABASE_ERROR,
        )

        val isCritical   = criticalCategories.contains(errorCategory)
        val isRateLimit  = errorCategory == ErrorCategories.RATE_LIMIT_ERROR
        val isUnknown    = errorCategory == ErrorCategories.UNKNOWN_ERROR

        if (!isCritical && !isRateLimit && !isUnknown) return

        val severity = when {
            isCritical  -> "CRITICAL"
            isRateLimit -> "WARNING"
            else        -> "ERROR"
        }

        val alertId = "ALERT-${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}"
        val summary = "Automated Debugger Alert: [$errorCategory] triggered on $NODE_ENV"

        logger.error { "🚨 [CRITICAL_ALERT_BOT] $severity: $summary" }

        // Persist to critical_alerts collection
        val db = FirebaseAdmin.db
        if (db != null) {
            try {
                val alertDoc = mapOf(
                    "alertId"           to alertId,
                    "timestamp"         to Instant.now().toString(),
                    "serverEnvironment" to NODE_ENV,
                    "severity"          to severity,
                    "errorCategory"     to errorCategory,
                    "requestId"         to requestId,
                    "maskedUserEmail"   to maskedUserEmail,
                    "summary"           to summary,
                    "details"           to (errorMessage ?: "No additional technical message provided"),
                    "metadata"          to sanitizedMetadata,
                    "createdAt"         to FieldValue.serverTimestamp(),
                    "expiresAt"         to com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
                        Instant.now().plusSeconds(30L * 24 * 60 * 60).epochSecond, 0
                    ),
                    "resolved"          to false,
                )
                db.collection(FirestoreCollections.CRITICAL_ALERTS).add(alertDoc).get()
            } catch (e: Exception) {
                logger.error { "[CRITICAL_ALERT_BOT] Failed to save alert to Firestore: ${e.message}" }
            }
        }

        // Dispatch webhook alert
        WebhookAlertService.triggerAlert(
            AlertPayload(
                severity      = severity,
                errorCategory = errorCategory,
                title         = summary,
                message       = errorMessage ?: "No additional technical message provided",
                requestId     = requestId,
                maskedUserEmail = maskedUserEmail,
            )
        )
    }
}
