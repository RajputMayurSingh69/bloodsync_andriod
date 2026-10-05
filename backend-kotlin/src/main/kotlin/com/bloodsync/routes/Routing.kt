package com.bloodsync.routes

import com.bloodsync.config.Environment.NODE_ENV
import com.bloodsync.config.Mailer
import com.bloodsync.services.CleanupCronService
import com.bloodsync.services.CooldownCronService
import com.bloodsync.services.EmergencyDispatcherService
import com.bloodsync.services.LoggerBotService
import com.bloodsync.services.WebhookAlertService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant

fun Application.configureRouting() {
    val startupTime = System.currentTimeMillis()
    val mailer = Mailer()
    val loggerBotService = LoggerBotService()
    val cooldownCronService = CooldownCronService(loggerBotService)
    val cleanupCronService = CleanupCronService()
    val webhookAlertService = WebhookAlertService()
    val emergencyDispatcherService = EmergencyDispatcherService(loggerBotService)

    routing {
        get("/") {
            call.respondText("BloodSync Backend is running!")
        }
        
        authRoutes()
        bankAuthRoutes()
        bloodBankRoutes()
        
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "healthy",
                "service" to "bloodsync-backend",
                "environment" to NODE_ENV,
                "timestamp" to Instant.now().toString(),
                "uptimeSeconds" to (System.currentTimeMillis() - startupTime) / 1000
            ))
        }

        get("/debugger/status") {
            val smtpStatus = mailer.verifyTransporter()
            call.respond(HttpStatusCode.OK, mapOf(
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

        post("/admin/cron/cooldown-check") {
            val result = cooldownCronService.runCooldownScan()
            call.respond(HttpStatusCode.OK, mapOf("success" to true) + result.toString().let { mapOf("result" to it) })
        }

        post("/admin/cron/cleanup-expired") {
            val result = cleanupCronService.runFullCleanup()
            call.respond(HttpStatusCode.OK, mapOf("success" to true) + result)
        }

        post("/admin/test-alert") {
            val payload = try {
                call.receive<Map<String, Any>>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Invalid payload"))
                return@post
            }
            
            val result = webhookAlertService.triggerAlert(
                severity = payload["severity"] as? String ?: "WARNING",
                errorCategory = payload["errorCategory"] as? String ?: "WEBHOOK_VERIFICATION_TEST",
                title = payload["title"] as? String ?: "BloodSync Test Alert",
                message = payload["message"] as? String ?: "Manual webhook alert verification from BloodSync backend.",
                requestId = "test_${System.currentTimeMillis()}",
                maskedUserEmail = "admin@bloodsync.org",
                metadata = mapOf("source" to "Admin API /admin/test-alert")
            )
            if (result["success"] == true) {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(HttpStatusCode.BadRequest, result)
            }
        }

        post("/emergency/dispatch") {
            val payload = try {
                call.receive<Map<String, Any>>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Invalid payload"))
                return@post
            }
            
            val result = emergencyDispatcherService.onEmergencyCreated(payload)
            if (result.success) {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(HttpStatusCode.InternalServerError, result)
            }
        }
    }
}
