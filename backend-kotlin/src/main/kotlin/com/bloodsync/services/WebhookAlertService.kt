package com.bloodsync.services

import com.bloodsync.config.AppConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.ConcurrentHashMap

/**
 * BloodSync Backend - Webhook Alert Service (Kotlin)
 *
 * Multi-platform webhook dispatching to Discord, Slack, Telegram, and custom endpoints.
 * Mirrors webhookAlertService.js 1:1.
 */
object WebhookAlertService {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        engine {
            requestTimeout = 6000
        }
    }

    private val recentAlerts = ConcurrentHashMap<String, Long>()
    private const val ALERT_THROTTLE_MS = 30_000L

    data class AlertResult(val success: Boolean, val skipped: Boolean = false, val reason: String? = null)

    suspend fun triggerAlert(
        severity: String = "CRITICAL",
        errorCategory: String = "SYSTEM_ERROR",
        title: String? = null,
        message: String = "An unexpected system error occurred.",
        requestId: String = "sys_${(Math.random() * 1_000_000).toLong()}",
        maskedUserEmail: String = "system",
        metadata: Map<String, Any?> = emptyMap()
    ): AlertResult {
        val webhookUrl = AppConfig.alertWebhookUrl.ifBlank { return AlertResult(false, true, "ALERT_WEBHOOK_URL is not configured") }

        val now = System.currentTimeMillis()
        val throttleKey = "$errorCategory:$severity"
        val lastSent = recentAlerts[throttleKey] ?: 0L

        if (now - lastSent < ALERT_THROTTLE_MS) {
            println("[WebhookAlert] Throttling alert for [$throttleKey]. Last sent ${(now - lastSent) / 1000}s ago.")
            return AlertResult(false, true, "Throttled to avoid notification flood")
        }
        recentAlerts[throttleKey] = now

        val resolvedTitle = title ?: "BloodSync $severity: $errorCategory"
        val timestamp = java.time.Instant.now().toString()
        val env = AppConfig.environment

        return withContext(Dispatchers.IO) {
            try {
                val payload = when {
                    webhookUrl.contains("discord.com") || webhookUrl.contains("discordapp.com") ->
                        buildDiscordPayload(severity, errorCategory, resolvedTitle, message, requestId, maskedUserEmail, timestamp, env)
                    webhookUrl.contains("slack.com") ->
                        buildSlackPayload(severity, errorCategory, resolvedTitle, message, requestId, maskedUserEmail, env)
                    else ->
                        buildGenericPayload(severity, errorCategory, resolvedTitle, message, requestId, maskedUserEmail, timestamp, env)
                }

                val response = httpClient.post(webhookUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

                if (response.status.isSuccess()) {
                    println("[WebhookAlert] Successfully dispatched alert (Status ${response.status.value})")
                    AlertResult(true)
                } else {
                    println("[WebhookAlert] Target returned HTTP ${response.status.value}")
                    AlertResult(false, reason = "HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                println("[WebhookAlert] Network dispatch error: ${e.message}")
                AlertResult(false, reason = e.message)
            }
        }
    }

    private fun buildDiscordPayload(
        severity: String, errorCategory: String, title: String, message: String,
        requestId: String, maskedEmail: String, timestamp: String, env: String
    ) = buildJsonObject {
        put("username", "BloodSync Sentry Bot")
        put("content", "🚨 **[$severity] $errorCategory** in `$env`")
        putJsonArray("embeds") {
            add(buildJsonObject {
                put("title", title)
                put("description", message)
                put("color", if (severity == "CRITICAL") 15158332 else if (severity == "WARNING") 15105570 else 3447003)
                putJsonArray("fields") {
                    add(buildJsonObject { put("name", "Environment"); put("value", "`$env`"); put("inline", true) })
                    add(buildJsonObject { put("name", "Severity"); put("value", "**$severity**"); put("inline", true) })
                    add(buildJsonObject { put("name", "Category"); put("value", "`$errorCategory`"); put("inline", true) })
                    add(buildJsonObject { put("name", "Request ID"); put("value", "`$requestId`"); put("inline", true) })
                    add(buildJsonObject { put("name", "User"); put("value", maskedEmail); put("inline", true) })
                }
                putJsonObject("footer") { put("text", "BloodSync Monitoring Engine • bloodsync-kotlin") }
                put("timestamp", timestamp)
            })
        }
    }

    private fun buildSlackPayload(
        severity: String, errorCategory: String, title: String, message: String,
        requestId: String, maskedEmail: String, env: String
    ) = buildJsonObject {
        put("text", "🚨 *BloodSync Alert [$severity]*: `$errorCategory`\n$message")
        putJsonArray("attachments") {
            add(buildJsonObject {
                put("color", if (severity == "CRITICAL") "#E74C3C" else if (severity == "WARNING") "#E67E22" else "#3498DB")
                put("title", title)
                put("text", message)
                putJsonArray("fields") {
                    add(buildJsonObject { put("title", "Environment"); put("value", env); put("short", true) })
                    add(buildJsonObject { put("title", "Severity"); put("value", severity); put("short", true) })
                    add(buildJsonObject { put("title", "Category"); put("value", errorCategory); put("short", true) })
                    add(buildJsonObject { put("title", "Request ID"); put("value", requestId); put("short", true) })
                }
                put("footer", "BloodSync Monitoring Engine")
            })
        }
    }

    private fun buildGenericPayload(
        severity: String, errorCategory: String, title: String, message: String,
        requestId: String, maskedEmail: String, timestamp: String, env: String
    ) = buildJsonObject {
        put("content", "🚨 [$severity] BloodSync Alert: $errorCategory - $message")
        put("severity", severity)
        put("errorCategory", errorCategory)
        put("title", title)
        put("message", message)
        put("requestId", requestId)
        put("maskedUserEmail", maskedEmail)
        put("serverEnvironment", env)
        put("timestamp", timestamp)
    }
}
