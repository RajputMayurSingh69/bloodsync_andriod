package com.bloodsync.routes

import com.bloodsync.config.Constants
import com.bloodsync.config.Constants.ERROR_CATEGORIES
import com.bloodsync.config.Constants.OTP_GEN_STATUS
import com.bloodsync.config.Constants.REQUEST_STATUS
import com.bloodsync.config.Environment.SECURITY_CONFIG
import com.bloodsync.config.FirebaseAdmin.auth
import com.bloodsync.services.EmailService
import com.bloodsync.services.LoggerBotService
import com.bloodsync.services.OtpService
import com.bloodsync.services.RateLimiterService
import com.bloodsync.utils.Sanitizer.maskEmail
import com.bloodsync.utils.Sanitizer.sanitizeIp
import com.bloodsync.utils.Validator.isValidEmail
import com.bloodsync.utils.Validator.isValidOtpFormat
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

data class OtpRequest(val email: String?)
data class VerifyOtpRequest(val email: String?, val otp: String?)

fun Route.authRoutes() {
    val otpService = OtpService()
    val emailService = EmailService(LoggerBotService())
    val rateLimiterService = RateLimiterService()
    val loggerBotService = LoggerBotService()

    route("/auth") {
        post("/request-otp") {
            val startTime = System.currentTimeMillis()
            val requestId = call.request.header("x-request-id") ?: UUID.randomUUID().toString()
            
            val request = try {
                call.receive<OtpRequest>()
            } catch (e: Exception) {
                OtpRequest(null)
            }
            
            val email = request.email
            val clientIp = sanitizeIp(call.request.origin.remoteHost)
            val userAgent = call.request.headers["user-agent"] ?: "unknown"

            var currentOtpGenStatus = OTP_GEN_STATUS.SKIPPED
            var currentEmailSendStatus = Constants.EMAIL_SEND_STATUS.PENDING

            // STEP 1: Validate incoming email format
            if (email.isNullOrBlank() || !isValidEmail(email)) {
                loggerBotService.logOtpEvent(
                    requestId = requestId,
                    rawEmail = email ?: "unknown",
                    requestStatus = REQUEST_STATUS.FAILURE,
                    otpGenStatus = OTP_GEN_STATUS.SKIPPED,
                    emailSendingStatus = Constants.EMAIL_SEND_STATUS.PENDING,
                    errorCategory = ERROR_CATEGORIES.UNKNOWN_ERROR,
                    errorMessage = "Invalid or missing email address provided in request payload.",
                    metadata = mapOf("clientIp" to clientIp, "userAgent" to userAgent, "durationMs" to (System.currentTimeMillis() - startTime))
                )
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "A valid email address is required."))
                return@post
            }

            val normalizedEmail = email.trim().lowercase()

            // STEP 2: Check rate limits
            val rateLimitCheck = rateLimiterService.checkRateLimit(normalizedEmail)
            if (!rateLimitCheck.allowed) {
                loggerBotService.logOtpEvent(
                    requestId = requestId,
                    rawEmail = normalizedEmail,
                    requestStatus = REQUEST_STATUS.FAILURE,
                    otpGenStatus = OTP_GEN_STATUS.SKIPPED,
                    emailSendingStatus = Constants.EMAIL_SEND_STATUS.PENDING,
                    errorCategory = ERROR_CATEGORIES.RATE_LIMIT_ERROR,
                    errorMessage = rateLimitCheck.reason,
                    metadata = mapOf(
                        "clientIp" to clientIp,
                        "userAgent" to userAgent,
                        "retryAfterSeconds" to rateLimitCheck.retryAfterSeconds,
                        "durationMs" to (System.currentTimeMillis() - startTime)
                    )
                )
                call.respond(HttpStatusCode.TooManyRequests, mapOf(
                    "success" to false,
                    "message" to rateLimitCheck.reason,
                    "retryAfterSeconds" to rateLimitCheck.retryAfterSeconds
                ))
                return@post
            }

            // STEP 3: Generate and store OTP record
            val otpData = try {
                val data = otpService.createAndStoreOtp(normalizedEmail, requestId)
                currentOtpGenStatus = OTP_GEN_STATUS.GENERATED
                data
            } catch (e: Exception) {
                currentOtpGenStatus = OTP_GEN_STATUS.FAILED
                loggerBotService.logOtpEvent(
                    requestId = requestId,
                    rawEmail = normalizedEmail,
                    requestStatus = REQUEST_STATUS.FAILURE,
                    otpGenStatus = currentOtpGenStatus,
                    emailSendingStatus = Constants.EMAIL_SEND_STATUS.PENDING,
                    errorCategory = ERROR_CATEGORIES.OTP_GENERATION_ERROR,
                    errorMessage = e.message ?: "Unknown error",
                    metadata = mapOf("clientIp" to clientIp, "userAgent" to userAgent, "durationMs" to (System.currentTimeMillis() - startTime))
                )
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Unable to process OTP request at this time. Please try again later."
                ))
                return@post
            }

            rateLimiterService.recordRequest(normalizedEmail)

            // STEP 4: Attempt Gmail SMTP delivery
            val emailResult = emailService.sendOtpEmail(normalizedEmail, otpData.otp, requestId)
            currentEmailSendStatus = emailResult.status

            // STEP 5: On Success
            if (emailResult.success) {
                loggerBotService.logOtpEvent(
                    requestId = requestId,
                    rawEmail = normalizedEmail,
                    requestStatus = REQUEST_STATUS.SUCCESS,
                    otpGenStatus = currentOtpGenStatus,
                    emailSendingStatus = currentEmailSendStatus,
                    errorCategory = null,
                    errorMessage = null,
                    metadata = mapOf(
                        "clientIp" to clientIp,
                        "userAgent" to userAgent,
                        "messageId" to emailResult.messageId,
                        "durationMs" to (System.currentTimeMillis() - startTime)
                    )
                )
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "message" to "Verification code sent to your email address.",
                    "data" to mapOf(
                        "maskedEmail" to maskEmail(normalizedEmail),
                        "expiresInMinutes" to SECURITY_CONFIG.otpExpiryMinutes
                    )
                ))
                return@post
            }

            // STEP 6: On Failure
            loggerBotService.logOtpEvent(
                requestId = requestId,
                rawEmail = normalizedEmail,
                requestStatus = REQUEST_STATUS.FAILURE,
                otpGenStatus = currentOtpGenStatus,
                emailSendingStatus = currentEmailSendStatus,
                errorCategory = emailResult.errorCategory ?: ERROR_CATEGORIES.EMAIL_SEND_ERROR,
                errorMessage = emailResult.errorMessage,
                metadata = mapOf("clientIp" to clientIp, "userAgent" to userAgent, "durationMs" to (System.currentTimeMillis() - startTime))
            )

            call.respond(HttpStatusCode.InternalServerError, mapOf(
                "success" to false,
                "message" to "Failed to deliver verification code. Please check your email and try again later."
            ))
        }

        post("/verify-otp") {
            val startTime = System.currentTimeMillis()
            val requestId = call.request.header("x-request-id") ?: UUID.randomUUID().toString()
            
            val request = try {
                call.receive<VerifyOtpRequest>()
            } catch (e: Exception) {
                VerifyOtpRequest(null, null)
            }
            
            val email = request.email
            val otp = request.otp
            val clientIp = sanitizeIp(call.request.origin.remoteHost)
            val userAgent = call.request.headers["user-agent"] ?: "unknown"

            if (email.isNullOrBlank() || !isValidEmail(email)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "A valid email address is required."))
                return@post
            }

            if (otp.isNullOrBlank() || !isValidOtpFormat(otp)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "A 6-digit numeric verification code is required."))
                return@post
            }

            val normalizedEmail = email.trim().lowercase()

            val verification = otpService.verifyOtp(normalizedEmail, otp, requestId)
            if (!verification.success) {
                loggerBotService.logOtpEvent(
                    requestId = requestId,
                    rawEmail = normalizedEmail,
                    requestStatus = REQUEST_STATUS.FAILURE,
                    otpGenStatus = OTP_GEN_STATUS.SKIPPED,
                    emailSendingStatus = Constants.EMAIL_SEND_STATUS.PENDING,
                    errorCategory = verification.errorCategory ?: ERROR_CATEGORIES.UNKNOWN_ERROR,
                    errorMessage = "OTP verification failed: ${verification.reason}",
                    metadata = mapOf("clientIp" to clientIp, "userAgent" to userAgent, "durationMs" to (System.currentTimeMillis() - startTime))
                )
                val status = if (verification.errorCategory == ERROR_CATEGORIES.RATE_LIMIT_ERROR) HttpStatusCode.TooManyRequests else HttpStatusCode.BadRequest
                call.respond(status, mapOf("success" to false, "message" to verification.reason ?: "Unknown error"))
                return@post
            }

            var firebaseCustomToken: String? = null
            try {
                auth?.let {
                    var userRecord: UserRecord? = null
                    try {
                        userRecord = it.getUserByEmail(normalizedEmail)
                    } catch (e: FirebaseAuthException) {
                        if (e.errorCode == "user-not-found") {
                            val createReq = UserRecord.CreateRequest()
                                .setEmail(normalizedEmail)
                                .setEmailVerified(true)
                            userRecord = it.createUser(createReq)
                        }
                    }
                    userRecord?.let { record ->
                        firebaseCustomToken = it.createCustomToken(record.uid, mapOf("email" to normalizedEmail, "verifiedViaOtp" to true))
                    }
                }
            } catch (e: Exception) {
                println("[AuthController] Could not issue Firebase custom token: ${e.message}")
            }

            val jwtToken = Jwts.builder()
                .setClaims(mapOf("email" to normalizedEmail, "type" to "otp_verified_session"))
                .setExpiration(Date(System.currentTimeMillis() + SECURITY_CONFIG.jwtExpirationMillis))
                .signWith(SignatureAlgorithm.HS256, SECURITY_CONFIG.jwtSecret.toByteArray())
                .compact()

            loggerBotService.logOtpEvent(
                requestId = requestId,
                rawEmail = normalizedEmail,
                requestStatus = REQUEST_STATUS.SUCCESS,
                otpGenStatus = OTP_GEN_STATUS.SKIPPED,
                emailSendingStatus = Constants.EMAIL_SEND_STATUS.PENDING,
                errorCategory = null,
                errorMessage = null,
                metadata = mapOf("action" to "OTP_VERIFIED", "clientIp" to clientIp, "userAgent" to userAgent, "durationMs" to (System.currentTimeMillis() - startTime))
            )

            call.respond(HttpStatusCode.OK, mapOf(
                "success" to true,
                "message" to "Email verified successfully.",
                "data" to mapOf(
                    "token" to jwtToken,
                    "firebaseCustomToken" to firebaseCustomToken,
                    "user" to mapOf(
                        "email" to normalizedEmail,
                        "maskedEmail" to maskEmail(normalizedEmail)
                    )
                )
            ))
        }
    }
}
