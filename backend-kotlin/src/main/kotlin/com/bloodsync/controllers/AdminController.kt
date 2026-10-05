package com.bloodsync.controllers

import com.bloodsync.config.Environment.NODE_ENV
import com.bloodsync.config.Mailer
import com.bloodsync.services.CleanupCronService
import com.bloodsync.services.CooldownCronService
import com.bloodsync.services.EmergencyDispatcherService
import com.bloodsync.services.WebhookAlertService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
class AdminController(
    private val mailer: Mailer,
    private val cooldownCronService: CooldownCronService,
    private val cleanupCronService: CleanupCronService,
    private val webhookAlertService: WebhookAlertService,
    private val emergencyDispatcherService: EmergencyDispatcherService
) {
    private val startupTime = System.currentTimeMillis()

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf(
            "status" to "healthy",
            "service" to "bloodsync-backend",
            "environment" to NODE_ENV,
            "timestamp" to Instant.now().toString(),
            "uptimeSeconds" to (System.currentTimeMillis() - startupTime) / 1000
        ))
    }

    @GetMapping("/debugger/status")
    suspend fun debuggerStatus(): ResponseEntity<Any> {
        val smtpStatus = mailer.verifyTransporter()
        return ResponseEntity.ok(mapOf(
            "service" to "BloodSync Debugger & Monitoring Bot",
            "environment" to NODE_ENV,
            "smtp" to mapOf(
                "connected" to smtpStatus.success,
                "category" to smtpStatus.category,
                "message" to smtpStatus.message
            ),
            "timestamp" to Instant.now().toString()
        ))
    }

    @PostMapping("/admin/cron/cooldown-check")
    suspend fun manualCooldownCheck(): ResponseEntity<Any> {
        val result = cooldownCronService.runCooldownScan()
        return ResponseEntity.ok(mapOf("success" to true) + result.toString().let { mapOf("result" to it) }) // Simplified map representation
    }

    @PostMapping("/admin/cron/cleanup-expired")
    suspend fun manualCleanupExpired(): ResponseEntity<Any> {
        val result = cleanupCronService.runFullCleanup()
        return ResponseEntity.ok(mapOf("success" to true) + result)
    }

    @PostMapping("/admin/test-alert")
    suspend fun testAlert(@RequestBody payload: Map<String, Any>): ResponseEntity<Any> {
        val result = webhookAlertService.triggerAlert(
            severity = payload["severity"] as? String ?: "WARNING",
            errorCategory = payload["errorCategory"] as? String ?: "WEBHOOK_VERIFICATION_TEST",
            title = payload["title"] as? String ?: "BloodSync Test Alert",
            message = payload["message"] as? String ?: "Manual webhook alert verification from BloodSync backend.",
            requestId = "test_${System.currentTimeMillis()}",
            maskedUserEmail = "admin@bloodsync.org",
            metadata = mapOf("source" to "Admin API /admin/test-alert")
        )
        return if (result["success"] == true) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/emergency/dispatch")
    suspend fun emergencyDispatch(@RequestBody payload: Map<String, Any>): ResponseEntity<Any> {
        val result = emergencyDispatcherService.onEmergencyCreated(payload)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.internalServerError().body(result)
        }
    }
}
