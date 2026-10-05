package com.bloodsync.config

import com.bloodsync.config.Constants.ERROR_CATEGORIES
import com.bloodsync.config.Constants.OTP_GEN_STATUS
import com.bloodsync.config.Constants.REQUEST_STATUS
import com.bloodsync.services.LoggerBotService
import com.bloodsync.utils.Sanitizer.sanitizeIp
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.UUID

fun Application.configureExceptionHandling() {
    val loggerBotService = LoggerBotService()

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val requestId = call.request.header("x-request-id") ?: UUID.randomUUID().toString()
            val clientIp = sanitizeIp(call.request.origin.remoteHost)
            val userAgent = call.request.headers["user-agent"] ?: "unknown"

            loggerBotService.logOtpEvent(
                requestId = requestId,
                rawEmail = "unknown",
                requestStatus = REQUEST_STATUS.FAILURE,
                otpGenStatus = OTP_GEN_STATUS.SKIPPED,
                emailSendingStatus = Constants.EMAIL_SEND_STATUS.FAILED,
                errorCategory = ERROR_CATEGORIES.UNKNOWN_ERROR,
                errorMessage = cause.message ?: "Internal server error occurred.",
                metadata = mapOf(
                    "clientIp" to clientIp,
                    "userAgent" to userAgent,
                    "path" to call.request.path(),
                    "method" to call.request.httpMethod.value
                )
            )

            call.respond(HttpStatusCode.InternalServerError, mapOf(
                "success" to false,
                "message" to "An unexpected error occurred. Please try again later.",
                "requestId" to requestId
            ))
        }
    }
}
