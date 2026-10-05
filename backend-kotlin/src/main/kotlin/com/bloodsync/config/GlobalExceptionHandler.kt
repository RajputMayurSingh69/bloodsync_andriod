package com.bloodsync.config

import com.bloodsync.config.Constants.ERROR_CATEGORIES
import com.bloodsync.config.Constants.OTP_GEN_STATUS
import com.bloodsync.config.Constants.REQUEST_STATUS
import com.bloodsync.services.LoggerBotService
import com.bloodsync.utils.Sanitizer.sanitizeIp
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.UUID

@ControllerAdvice
class GlobalExceptionHandler(
    private val loggerBotService: LoggerBotService
) {

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, req: HttpServletRequest): ResponseEntity<Any> {
        val requestId = req.getHeader("x-request-id") ?: UUID.randomUUID().toString()
        val clientIp = sanitizeIp(req.remoteAddr)
        val userAgent = req.getHeader("user-agent") ?: "unknown"
        val rawEmail = "unknown" // Might be difficult to extract easily here without caching request body

        loggerBotService.logOtpEvent(
            requestId = requestId,
            rawEmail = rawEmail,
            requestStatus = REQUEST_STATUS.FAILURE,
            otpGenStatus = OTP_GEN_STATUS.SKIPPED,
            emailSendingStatus = Constants.EMAIL_SEND_STATUS.FAILED,
            errorCategory = ERROR_CATEGORIES.UNKNOWN_ERROR,
            errorMessage = ex.message ?: "Internal server error occurred.",
            metadata = mapOf(
                "clientIp" to clientIp,
                "userAgent" to userAgent,
                "path" to req.requestURI,
                "method" to req.method
            )
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
            "success" to false,
            "message" to "An unexpected error occurred. Please try again later.",
            "requestId" to requestId
        ))
    }
}
