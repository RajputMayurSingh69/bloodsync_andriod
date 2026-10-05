package com.bloodsync.services

import com.bloodsync.config.ErrorCategories
import com.bloodsync.config.MONITORING_CONFIG
import com.bloodsync.config.NODE_ENV
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * BloodSync Backend - Critical Error Webhook Alert Service
 * Converted from: backend/src/services/webhookAlertService.js
 *
 * Provides unified multi-platform webhook dispatching to Discord, Slack,
 * Telegram, and custom webhook endpoints.
 */

private val logger = KotlinLogging.logger {}

data class AlertPayload(
    val severity: String      = "CRITICAL",
    val errorCategory: String = "SYSTEM_ERROR",
    val title: String?        = null,
    val message: String       = "An unexpected system error occurred.",
    val requestId: String?    = null,
    val maskedUserEmail: String = "system",
    val metadata: Map<String, Any?> = emptyMap(),
)

data class AlertResult(
    val success: Boolean,
    val skipped: Boolean = false,
    val reason: String? = null,
    val statusCode: Int? = null,
)

object WebhookAlertService {

    private val recentAlerts = mutableMapOf<String, Long>()
    private const val ALERT_THROTTLE_MS = 30_000L

    suspend fun triggerAlert(alert: AlertPayload): AlertResult {
        val webhookUrl = MONITORING_CONFIG.alertWebhookUrl
            .takeIf { it.isNotBlank() }
            ?: return AlertResult(success = false, skipped = true, reason = "ALERT_WEBHOOK_URL is not configured")

        val now = System.currentTimeMillis()
        val throttleKey = "${alert.errorCategory}:${alert.severity}"

        synchronized(recentAlerts) {
            val lastSent = recentAlerts[throttleKey] ?: 0L
            if (now - lastSent < ALERT_THROTTLE_MS) {
                val elapsed = (now - lastSent) / 1000
                logger.warn { "[WebhookAlert] Throttling alert for [$throttleKey]. Last sent ${elapsed}s ago." }
                return AlertResult(success = false, skipped = true, reason = "Throttled to avoid notification flood")
            }
            recentAlerts[throttleKey] = now
        }

        return try {
            val url = URL(webhookUrl.trim())
            val effectiveTitle = alert.title ?: "BloodSync ${alert.severity}: ${alert.errorCategory}"
            val timestamp = java.time.Instant.now().toString()

            val payload = buildPayload(
                host          = url.host.lowercase(),
                severity      = alert.severity,
                errorCategory = alert.errorCategory,
                title         = effectiveTitle,
                message       = alert.message,
                requestId     = alert.requestId ?: "sys_${Integer.toHexString((Math.random() * 0xFFFFFF).toInt())}",
                maskedEmail   = alert.maskedUserEmail,
                timestamp     = timestamp,
            )

            dispatchHttpRequest(webhookUrl, payload)
        } catch (err: Exception) {
            logger.error { "[WebhookAlert] Failed to dispatch webhook alert: ${err.message}" }
            AlertResult(success = false, reason = err.message)
        }
    }

    private fun buildPayload(
        host: String,
        severity: String,
        errorCategory: String,
        title: String,
        message: String,
        requestId: String,
        maskedEmail: String,
        timestamp: String,
    ): String {
        val isDiscord  = host.contains("discord.com")  || host.contains("discordapp.com")
        val isSlack    = host.contains("slack.com")
        val isTelegram = host.contains("telegram.org")

        val colorHex   = if (severity == "CRITICAL") 15158332 else if (severity == "WARNING") 15120418 else 3447003

        return when {
            isDiscord -> """
                {
                    "username": "BloodSync Sentry Bot",
                    "content": "🚨 **[$severity] $errorCategory** in `$NODE_ENV`",
                    "embeds": [{
                        "title": "$title",
                        "description": "${message.replace("\"", "'")}",
                        "color": $colorHex,
                        "fields": [
                            {"name": "Environment", "value": "`$NODE_ENV`", "inline": true},
                            {"name": "Severity",    "value": "**$severity**", "inline": true},
                            {"name": "Category",    "value": "`$errorCategory`", "inline": true},
                            {"name": "Request ID",  "value": "`$requestId`", "inline": true},
                            {"name": "User",        "value": "$maskedEmail", "inline": true}
                        ],
                        "footer": {"text": "BloodSync Monitoring Engine • bloodsync-3b5cf"},
                        "timestamp": "$timestamp"
                    }]
                }
            """.trimIndent()

            isSlack -> """
                {
                    "text": "🚨 *BloodSync Alert [$severity]*: `$errorCategory`\n${message.replace("\"", "'")}",
                    "attachments": [{
                        "title": "$title",
                        "text": "${message.replace("\"", "'")}",
                        "fields": [
                            {"title": "Environment", "value": "$NODE_ENV", "short": true},
                            {"title": "Severity",    "value": "$severity", "short": true},
                            {"title": "Category",    "value": "$errorCategory", "short": true},
                            {"title": "Request ID",  "value": "$requestId", "short": true}
                        ],
                        "footer": "BloodSync Monitoring Engine"
                    }]
                }
            """.trimIndent()

            isTelegram -> """
                {
                    "text": "🚨 <b>BloodSync Alert [$severity]</b>\n<b>Category:</b> <code>$errorCategory</code>\n<b>Environment:</b> <code>$NODE_ENV</code>\n<b>User:</b> <code>$maskedEmail</code>\n<b>Request ID:</b> <code>$requestId</code>\n\n<b>Details:</b> ${message.replace("\"", "'")}",
                    "parse_mode": "HTML"
                }
            """.trimIndent()

            else -> """
                {
                    "severity": "$severity",
                    "errorCategory": "$errorCategory",
                    "title": "$title",
                    "message": "${message.replace("\"", "'")}",
                    "requestId": "$requestId",
                    "maskedUserEmail": "$maskedEmail",
                    "serverEnvironment": "$NODE_ENV",
                    "timestamp": "$timestamp"
                }
            """.trimIndent()
        }
    }

    private suspend fun dispatchHttpRequest(webhookUrl: String, jsonBody: String): AlertResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(webhookUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("User-Agent", "BloodSync-AlertBot/1.0")
                conn.connectTimeout = 6000
                conn.readTimeout    = 6000
                conn.doOutput = true

                conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

                val status = conn.responseCode
                conn.disconnect()

                if (status in 200..299) {
                    logger.info { "[WebhookAlert] Successfully dispatched alert to ${url.host} (Status $status)" }
                    AlertResult(success = true, statusCode = status)
                } else {
                    logger.warn { "[WebhookAlert] Target returned HTTP $status" }
                    AlertResult(success = false, statusCode = status)
                }
            } catch (e: Exception) {
                logger.warn { "[WebhookAlert] Network dispatch error: ${e.message}" }
                AlertResult(success = false, reason = e.message)
            }
        }
    }
}
