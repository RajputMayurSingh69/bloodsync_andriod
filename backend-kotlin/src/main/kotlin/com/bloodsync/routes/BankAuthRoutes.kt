package com.bloodsync.routes

import com.bloodsync.config.Constants.ERROR_CATEGORIES
import com.bloodsync.config.Constants.FIRESTORE_COLLECTIONS
import com.bloodsync.config.Constants.REQUEST_STATUS
import com.bloodsync.config.Environment.SECURITY_CONFIG
import com.bloodsync.config.FirebaseAdmin.auth
import com.bloodsync.config.FirebaseAdmin.db
import com.bloodsync.services.EmailService
import com.bloodsync.services.LoggerBotService
import com.bloodsync.utils.Sanitizer.maskEmail
import com.bloodsync.utils.Sanitizer.sanitizeIp
import com.bloodsync.utils.Validator.isNonEmptyString
import com.bloodsync.utils.Validator.isValidEmail
import com.bloodsync.utils.Validator.isValidPhone
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
import java.time.Instant
import java.util.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

data class RegisterRequest(
    val bankName: String?,
    val email: String?,
    val phone: String?,
    val city: String?,
    val address: String?,
    val password: String?
)

data class LoginRequest(
    val email: String?,
    val password: String?
)

private val passwordEncoder = BCryptPasswordEncoder(12)
private val collectionName = FIRESTORE_COLLECTIONS.BANK_REGISTRATIONS

private fun signBankJwt(bankId: String, email: String): String {
    return Jwts.builder()
        .setSubject(bankId)
        .setClaims(mapOf("email" to email, "role" to "blood_bank", "type" to "bank_session"))
        .setExpiration(Date(System.currentTimeMillis() + SECURITY_CONFIG.jwtExpirationMillis))
        .signWith(SignatureAlgorithm.HS256, SECURITY_CONFIG.jwtSecret.toByteArray())
        .compact()
}

private fun verifyBankJwt(authHeader: String?): Map<String, Any>? {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
    return try {
        val token = authHeader.substring(7)
        val claims = Jwts.parser().setSigningKey(SECURITY_CONFIG.jwtSecret.toByteArray()).parseClaimsJws(token).body
        claims
    } catch (e: Exception) {
        null
    }
}

fun Route.bankAuthRoutes() {
    val loggerBotService = LoggerBotService()
    val emailService = EmailService(loggerBotService)

    route("/bank-auth") {
        post("/register") {
            val startTime = System.currentTimeMillis()
            val requestId = call.request.header("x-request-id") ?: UUID.randomUUID().toString()
            val clientIp = sanitizeIp(call.request.origin.remoteHost)
            val userAgent = call.request.headers["user-agent"] ?: "unknown"

            val request = try {
                call.receive<RegisterRequest>()
            } catch (e: Exception) {
                RegisterRequest(null, null, null, null, null, null)
            }

            val validationErrors = mutableListOf<String>()
            if (!isNonEmptyString(request.bankName, 3)) validationErrors.add("Blood bank name must be at least 3 characters.")
            if (request.email.isNullOrBlank() || !isValidEmail(request.email)) validationErrors.add("A valid email address is required.")
            if (!isValidPhone(request.phone)) validationErrors.add("A valid phone number (min 10 digits) is required.")
            if (!isNonEmptyString(request.city, 2)) validationErrors.add("City is required.")
            if (!isNonEmptyString(request.address, 8)) validationErrors.add("Full address (min 8 characters) is required.")
            if (request.password.isNullOrBlank() || request.password.length < 6) validationErrors.add("Password must be at least 6 characters.")

            if (validationErrors.isNotEmpty()) {
                loggerBotService.logOtpEvent(
                    requestId = requestId,
                    rawEmail = request.email ?: "unknown",
                    requestStatus = REQUEST_STATUS.FAILURE,
                    otpGenStatus = "SKIPPED",
                    emailSendingStatus = "SKIPPED",
                    errorCategory = ERROR_CATEGORIES.VALIDATION_ERROR,
                    errorMessage = "Bank registration validation failed: ${validationErrors.joinToString(" | ")}",
                    metadata = mapOf("clientIp" to clientIp, "userAgent" to userAgent, "action" to "BANK_REGISTER", "durationMs" to (System.currentTimeMillis() - startTime))
                )
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Validation failed.", "errors" to validationErrors))
                return@post
            }

            val normalizedEmail = request.email!!.trim().lowercase()
            val normalizedPhone = request.phone!!.trim()
            val normalizedName = request.bankName!!.trim()
            val normalizedCity = request.city!!.trim()
            val normalizedAddr = request.address!!.trim()

            if (db != null) {
                try {
                    val existing = db.collection(collectionName)
                        .whereEqualTo("email", normalizedEmail)
                        .limit(1)
                        .get()
                        .get()
                    if (!existing.isEmpty) {
                        call.respond(HttpStatusCode.Conflict, mapOf(
                            "success" to false,
                            "message" to "An account with this email already exists. Please sign in."
                        ))
                        return@post
                    }
                } catch (e: Exception) {
                    println("[BankAuth] Duplicate check failed: ${e.message}")
                }
            }

            val passwordHash = try {
                passwordEncoder.encode(request.password)
            } catch (e: Exception) {
                println("[BankAuth] bcrypt hash error: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Registration failed. Please try again later."))
                return@post
            }

            val bankId = "bank_${UUID.randomUUID().toString().replace("-", "").substring(0, 16)}"
            val stocks = mapOf("A+" to 0, "A-" to 0, "B+" to 0, "B-" to 0, "AB+" to 0, "AB-" to 0, "O+" to 0, "O-" to 0)
            
            val bankDoc = mapOf(
                "bankId" to bankId,
                "bankName" to normalizedName,
                "email" to normalizedEmail,
                "phone" to normalizedPhone,
                "city" to normalizedCity,
                "address" to normalizedAddr,
                "passwordHash" to passwordHash,
                "role" to "blood_bank",
                "verified" to false,
                "active" to true,
                "stocks" to stocks,
                "totalUnitsAvailable" to 0,
                "registrationIp" to clientIp,
                "createdAt" to Instant.now().toString(),
                "updatedAt" to Instant.now().toString()
            )

            if (db != null) {
                try {
                    db.collection(collectionName).document(bankId).set(bankDoc).get()

                    db.collection(FIRESTORE_COLLECTIONS.BLOOD_BANKS).document(bankId).set(mapOf(
                        "bankId" to bankId,
                        "name" to normalizedName,
                        "email" to normalizedEmail,
                        "phone" to normalizedPhone,
                        "city" to normalizedCity,
                        "address" to normalizedAddr,
                        "verified" to false,
                        "active" to true,
                        "stocks" to stocks,
                        "totalUnitsAvailable" to 0,
                        "bloodStockStatus" to "Pending Setup",
                        "createdAt" to Instant.now().toString()
                    )).get()
                } catch (e: Exception) {
                    println("[BankAuth] Firestore write failed: ${e.message}")
                    loggerBotService.logOtpEvent(
                        requestId = requestId,
                        rawEmail = normalizedEmail,
                        requestStatus = REQUEST_STATUS.FAILURE,
                        otpGenStatus = "SKIPPED",
                        emailSendingStatus = "SKIPPED",
                        errorCategory = ERROR_CATEGORIES.FIREBASE_ERROR,
                        errorMessage = "Firestore bank registration write failed: ${e.message}",
                        metadata = mapOf("clientIp" to clientIp, "userAgent" to userAgent, "action" to "BANK_REGISTER", "durationMs" to (System.currentTimeMillis() - startTime))
                    )
                    call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Registration failed. Please try again later."))
                    return@post
                }
            }

            var firebaseCustomToken: String? = null
            if (auth != null) {
                try {
                    var userRecord: UserRecord? = null
                    try {
                        userRecord = auth.getUserByEmail(normalizedEmail)
                    } catch (e: FirebaseAuthException) {
                        if (e.errorCode == "user-not-found") {
                            val createReq = UserRecord.CreateRequest()
                                .setUid(bankId)
                                .setEmail(normalizedEmail)
                                .setEmailVerified(false)
                                .setDisplayName(normalizedName)
                            userRecord = auth.createUser(createReq)
                        }
                    }
                    if (userRecord != null) {
                        auth.setCustomUserClaims(userRecord.uid, mapOf("role" to "blood_bank", "bankId" to bankId, "active" to true))
                        firebaseCustomToken = auth.createCustomToken(userRecord.uid, mapOf("role" to "blood_bank", "bankId" to bankId))
                    }
                } catch (e: Exception) {
                    println("[BankAuth] Firebase Auth setup warning: ${e.message}")
                }
            }

            val jwtToken = signBankJwt(bankId, normalizedEmail)

            try {
                emailService.sendOtpEmail(normalizedEmail, "WELCOME_${normalizedName}", requestId)
            } catch (e: Exception) {
                println("[BankAuth] Welcome email failed: ${e.message}")
            }

            loggerBotService.logOtpEvent(
                requestId = requestId,
                rawEmail = normalizedEmail,
                requestStatus = REQUEST_STATUS.SUCCESS,
                otpGenStatus = "SKIPPED",
                emailSendingStatus = "SENT",
                errorCategory = null,
                errorMessage = null,
                metadata = mapOf(
                    "action" to "BANK_REGISTERED",
                    "bankId" to bankId,
                    "bankName" to normalizedName,
                    "city" to normalizedCity,
                    "clientIp" to clientIp,
                    "userAgent" to userAgent,
                    "durationMs" to (System.currentTimeMillis() - startTime)
                )
            )

            call.respond(HttpStatusCode.Created, mapOf(
                "success" to true,
                "message" to "Blood bank registered successfully. Your account is under review.",
                "data" to mapOf(
                    "bankId" to bankId,
                    "bankName" to normalizedName,
                    "email" to normalizedEmail,
                    "maskedEmail" to maskEmail(normalizedEmail),
                    "city" to normalizedCity,
                    "phone" to normalizedPhone,
                    "address" to normalizedAddr,
                    "verified" to false,
                    "active" to true,
                    "token" to jwtToken,
                    "firebaseCustomToken" to firebaseCustomToken
                )
            ))
        }

        post("/login") {
            val startTime = System.currentTimeMillis()
            val requestId = call.request.header("x-request-id") ?: UUID.randomUUID().toString()
            val clientIp = sanitizeIp(call.request.origin.remoteHost)
            val userAgent = call.request.headers["user-agent"] ?: "unknown"

            val request = try {
                call.receive<LoginRequest>()
            } catch (e: Exception) {
                LoginRequest(null, null)
            }

            val email = request.email
            val password = request.password

            if (email.isNullOrBlank() || !isValidEmail(email)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "A valid email address is required."))
                return@post
            }
            if (password.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Password is required."))
                return@post
            }

            val normalizedEmail = email.trim().lowercase()

            if (db == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("success" to false, "message" to "Service temporarily unavailable."))
                return@post
            }

            var bankData: Map<String, Any>? = null
            var bankDocId: String? = null
            try {
                val snap = db.collection(collectionName).whereEqualTo("email", normalizedEmail).limit(1).get().get()
                if (snap.isEmpty) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Invalid email or password."))
                    return@post
                }
                val doc = snap.documents[0]
                bankData = doc.data
                bankDocId = doc.id
            } catch (e: Exception) {
                println("[BankAuth] Login DB fetch failed: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Sign in failed. Please try again."))
                return@post
            }

            val storedHash = bankData?.get("passwordHash") as? String ?: ""
            val passwordMatch = try {
                passwordEncoder.matches(password, storedHash)
            } catch (e: Exception) { false }

            if (!passwordMatch) {
                loggerBotService.logOtpEvent(
                    requestId = requestId,
                    rawEmail = normalizedEmail,
                    requestStatus = REQUEST_STATUS.FAILURE,
                    otpGenStatus = "SKIPPED",
                    emailSendingStatus = "SKIPPED",
                    errorCategory = ERROR_CATEGORIES.UNAUTHORIZED_ERROR,
                    errorMessage = "Bank login failed: incorrect password.",
                    metadata = mapOf("clientIp" to clientIp, "userAgent" to userAgent, "action" to "BANK_LOGIN", "durationMs" to (System.currentTimeMillis() - startTime))
                )
                call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Invalid email or password."))
                return@post
            }

            if (bankData?.get("active") == false) {
                call.respond(HttpStatusCode.Forbidden, mapOf("success" to false, "message" to "Your account has been deactivated. Contact BloodSync support."))
                return@post
            }

            val jwtToken = signBankJwt(bankDocId ?: "", normalizedEmail)
            var firebaseCustomToken: String? = null
            if (auth != null) {
                try {
                    val uid = (bankData?.get("bankId") as? String) ?: bankDocId ?: ""
                    firebaseCustomToken = auth.createCustomToken(uid, mapOf("role" to "blood_bank", "bankId" to uid))
                } catch (e: Exception) {
                    println("[BankAuth] Firebase token error on login: ${e.message}")
                }
            }

            loggerBotService.logOtpEvent(
                requestId = requestId,
                rawEmail = normalizedEmail,
                requestStatus = REQUEST_STATUS.SUCCESS,
                otpGenStatus = "SKIPPED",
                emailSendingStatus = "SKIPPED",
                errorCategory = null,
                errorMessage = null,
                metadata = mapOf(
                    "action" to "BANK_LOGIN",
                    "bankId" to bankData?.get("bankId"),
                    "bankName" to bankData?.get("bankName"),
                    "clientIp" to clientIp,
                    "userAgent" to userAgent,
                    "durationMs" to (System.currentTimeMillis() - startTime)
                )
            )

            val safeData = bankData?.toMutableMap() ?: mutableMapOf()
            safeData.remove("passwordHash")
            safeData.remove("registrationIp")

            call.respond(HttpStatusCode.OK, mapOf(
                "success" to true,
                "message" to "Signed in successfully.",
                "data" to safeData.plus(mapOf(
                    "maskedEmail" to maskEmail(normalizedEmail),
                    "token" to jwtToken,
                    "firebaseCustomToken" to firebaseCustomToken
                ))
            ))
        }

        get("/me") {
            val decoded = verifyBankJwt(call.request.header("authorization"))
            if (decoded == null || decoded["role"] != "blood_bank") {
                call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Unauthorized. Valid bank token required."))
                return@get
            }

            if (db == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("success" to false, "message" to "Service temporarily unavailable."))
                return@get
            }

            try {
                val email = decoded["email"] as? String
                if (email == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Unauthorized. Valid bank token required."))
                    return@get
                }
                
                val snap = db.collection(collectionName).whereEqualTo("email", email).limit(1).get().get()
                if (snap.isEmpty) {
                    call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Bank profile not found."))
                    return@get
                }

                val docData = snap.documents[0].data
                docData.remove("passwordHash")
                docData.remove("registrationIp")

                call.respond(HttpStatusCode.OK, mapOf("success" to true, "data" to docData))
            } catch (e: Exception) {
                println("[BankAuth] getMe failed: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Failed to fetch profile."))
            }
        }
    }
}
