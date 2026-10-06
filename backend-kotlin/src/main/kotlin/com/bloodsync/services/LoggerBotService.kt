package com.bloodsync.services

import com.bloodsync.config.AppConfig
import com.bloodsync.config.ErrorCategories
import com.bloodsync.config.FirebaseConfig
import com.bloodsync.config.FirestoreCollections
import com.bloodsync.config.RequestStatus
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BloodSync Backend - Firebase Debugger Bot & Real-Time Monitoring Service (Kotlin)
 *
 * Records system events into `backend_debugger_logs` and triggers alerts
 * for critical failures. Mirrors loggerBotService.js 1:1.
 */
object LoggerBotService {

    data class LogParams(
        val requestId: String,
        val rawEmail: String,
        val requestStatus: String,
        val otpGenStatus: String,
        val emailSendingStatus: String,
        val errorCategory: String? = null,
        val errorMessage: String? = null,
        val metadata: Map<String, Any?> = emptyMap()
    )

    suspend fun logOtpEvent(params: LogParams) {
        val maskedEmail = maskEmail(params.rawEmail)
        val sanitizedError = params.errorMessage?.take(500)

        val level = if (params.requestStatus == RequestStatus.SUCCESS) "INFO" else "ERROR"
        println("[DebuggerBot][$level] [${params.requestId}] status=${params.requestStatus} email=$maskedEmail otpGen=${params.otpGenStatus} emailSend=${params.emailSendingStatus} errCat=${params.errorCategory ?: "none"}")

        val db = FirebaseConfig.firestore
        if (db != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val now = System.currentTimeMillis()
                    db.collection(FirestoreCollections.BACKEND_DEBUGGER_LOGS).add(
                        mapOf(
                            "timestamp" to FieldValue.serverTimestamp(),
                            "requestId" to params.requestId,
                            "maskedUserEmail" to maskedEmail,
                            "requestStatus" to params.requestStatus,
                            "otpGenStatus" to params.otpGenStatus,
                            "emailSendingStatus" to params.emailSendingStatus,
                            "errorCategory" to params.errorCategory,
                            "errorMessage" to sanitizedError,
                            "serverEnvironment" to AppConfig.environment,
                            "expiresAt" to Timestamp.ofTimeSecondsAndNanos((now + 14L * 24 * 60 * 60 * 1000) / 1000, 0),
                            "metadata" to params.metadata
                        )
                    ).get()
                }.onFailure { e ->
                    println("[DebuggerBot] CRITICAL: Failed to write to Firestore: ${e.message}")
                }
            }
        }

        if (params.requestStatus == RequestStatus.FAILURE && params.errorCategory != null) {
            evaluateAlertConditions(params.requestId, maskedEmail, params.errorCategory, sanitizedError, params.metadata)
        }
    }

    private suspend fun evaluateAlertConditions(
        requestId: String,
        maskedEmail: String,
        errorCategory: String,
        errorMessage: String?,
        metadata: Map<String, Any?>
    ) {
        val criticalCategories = setOf(
            ErrorCategories.SMTP_AUTH_ERROR,
            ErrorCategories.EMAIL_SEND_ERROR,
            ErrorCategories.EMAIL_CONFIGURATION_ERROR,
            ErrorCategories.FIREBASE_ERROR,
            ErrorCategories.OTP_DATABASE_ERROR
        )

        val isCritical = errorCategory in criticalCategories
        val isRateLimit = errorCategory == ErrorCategories.RATE_LIMIT_ERROR

        if (!isCritical && !isRateLimit && errorCategory != ErrorCategories.UNKNOWN_ERROR) return

        val severity = when {
            isCritical -> "CRITICAL"
            isRateLimit -> "WARNING"
            else -> "ERROR"
        }

        val alertId = "ALERT-${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}"
        val summary = "Automated Debugger Alert: [$errorCategory] triggered on ${AppConfig.environment}"

        println("🚨 [CRITICAL_ALERT_BOT] $severity: $summary")

        val db = FirebaseConfig.firestore
        if (db != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val now = System.currentTimeMillis()
                    db.collection(FirestoreCollections.CRITICAL_ALERTS).add(
                        mapOf(
                            "alertId" to alertId,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "serverEnvironment" to AppConfig.environment,
                            "severity" to severity,
                            "errorCategory" to errorCategory,
                            "requestId" to requestId,
                            "maskedUserEmail" to maskedEmail,
                            "summary" to summary,
                            "details" to (errorMessage ?: "No additional technical message provided"),
                            "metadata" to metadata,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "expiresAt" to Timestamp.ofTimeSecondsAndNanos((now + 30L * 24 * 60 * 60 * 1000) / 1000, 0),
                            "resolved" to false
                        )
                    ).get()
                }
            }
        }

        WebhookAlertService.triggerAlert(
            severity = severity,
            errorCategory = errorCategory,
            title = summary,
            message = errorMessage ?: "No additional technical message provided",
            requestId = requestId,
            maskedUserEmail = maskedEmail,
            metadata = metadata
        )
    }

    private fun maskEmail(email: String): String {
        val atIdx = email.indexOf('@')
        if (atIdx <= 1) return "***@***"
        val local = email.substring(0, atIdx)
        val domain = email.substring(atIdx)
        return "${local.take(2)}***$domain"
    }
}
