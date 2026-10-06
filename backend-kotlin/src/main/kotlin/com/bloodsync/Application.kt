package com.bloodsync

import com.bloodsync.config.AppConfig
import com.bloodsync.plugins.*
import com.bloodsync.services.EmergencyDispatcherService
import com.bloodsync.services.BloodBankService
import com.bloodsync.services.CooldownCronService
import com.bloodsync.services.CleanupCronService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * BloodSync Backend - Ktor Server Entry Point (Kotlin)
 *
 * Replaces the Node.js/Express backend 1:1 with Ktor + Firebase Admin SDK.
 * All services, routes, and functionality are preserved in Kotlin.
 */
fun main() {
    val port = AppConfig.port
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureFirebase()
        configureSecurity()
        configureSerialization()
        configureCors()
        configureStatusPages()
        configureCallLogging()
        configureRouting()
    }.also { server ->
        // Background services started after server binds
        EmergencyDispatcherService.startListener()
        BloodBankService.initializeDefaults()
        CooldownCronService.startDailySchedule()
        CleanupCronService.startWeeklySchedule()

        println("=======================================================")
        println("🩸 BloodSync Ktor Backend running on port $port [${AppConfig.environment}]")
        println("📡 Health Check: http://localhost:$port/health")
        println("🛠️  Debugger Status: http://localhost:$port/debugger/status")
        println("👤 User Auth (OTP): http://localhost:$port/auth")
        println("🏥 Blood Bank Auth: http://localhost:$port/bank-auth")
        println("🏦 Blood Banks: http://localhost:$port/blood-banks")
        println("🚨 Emergency Dispatcher: http://localhost:$port/emergency/dispatch")
        println("🧹 Database Purge: http://localhost:$port/admin/cron/cleanup-expired")
        println("=======================================================")

        server.start(wait = true)
    }
}
