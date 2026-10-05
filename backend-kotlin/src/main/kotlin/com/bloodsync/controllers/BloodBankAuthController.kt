package com.bloodsync.controllers

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
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/bank-auth")
class BloodBankAuthController(
    private val emailService: EmailService,
    private val loggerBotService: LoggerBotService
) {
    private val passwordEncoder = BCryptPasswordEncoder(12)
    private val collectionName = FIRESTORE_COLLECTIONS.BANK_REGISTRATIONS

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

    @PostMapping("/register")
    suspend fun register(@RequestBody request: RegisterRequest, req: HttpServletRequest): ResponseEntity<Any> {
        val startTime = System.currentTimeMillis()
        val requestId = req.getHeader("x-request-id") ?: UUID.randomUUID().toString()
        val clientIp = sanitizeIp(req.remoteAddr)
        val userAgent = req.getHeader("user-agent") ?: "unknown"

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
            return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "Validation failed.", "errors" to validationErrors))
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
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf(
                        "success" to false,
                        "message" to "An account with this email already exists. Please sign in."
                    ))
                }
            } catch (e: Exception) {
                println("[BankAuth] Duplicate check failed: ${e.message}")
            }
        }

        val passwordHash = try {
            passwordEncoder.encode(request.password)
        } catch (e: Exception) {
            println("[BankAuth] bcrypt hash error: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("success" to false, "message" to "Registration failed. Please try again later."))
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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("success" to false, "message" to "Registration failed. Please try again later."))
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

        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf(
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

    @PostMapping("/login")
    suspend fun login(@RequestBody request: LoginRequest, req: HttpServletRequest): ResponseEntity<Any> {
        val startTime = System.currentTimeMillis()
        val requestId = req.getHeader("x-request-id") ?: UUID.randomUUID().toString()
        val clientIp = sanitizeIp(req.remoteAddr)
        val userAgent = req.getHeader("user-agent") ?: "unknown"

        val email = request.email
        val password = request.password

        if (email.isNullOrBlank() || !isValidEmail(email)) {
            return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "A valid email address is required."))
        }
        if (password.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "Password is required."))
        }

        val normalizedEmail = email.trim().lowercase()

        if (db == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("success" to false, "message" to "Service temporarily unavailable."))
        }

        var bankData: Map<String, Any>? = null
        var bankDocId: String? = null
        try {
            val snap = db.collection(collectionName).whereEqualTo("email", normalizedEmail).limit(1).get().get()
            if (snap.isEmpty) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("success" to false, "message" to "Invalid email or password."))
            }
            val doc = snap.documents[0]
            bankData = doc.data
            bankDocId = doc.id
        } catch (e: Exception) {
            println("[BankAuth] Login DB fetch failed: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("success" to false, "message" to "Sign in failed. Please try again."))
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("success" to false, "message" to "Invalid email or password."))
        }

        if (bankData?.get("active") == false) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("success" to false, "message" to "Your account has been deactivated. Contact BloodSync support."))
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

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Signed in successfully.",
            "data" to safeData.plus(mapOf(
                "maskedEmail" to maskEmail(normalizedEmail),
                "token" to jwtToken,
                "firebaseCustomToken" to firebaseCustomToken
            ))
        ))
    }

    @GetMapping("/me")
    fun getMe(req: HttpServletRequest): ResponseEntity<Any> {
        val decoded = verifyBankJwt(req.getHeader("authorization"))
        if (decoded == null || decoded["role"] != "blood_bank") {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("success" to false, "message" to "Unauthorized. Valid bank token required."))
        }

        if (db == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("success" to false, "message" to "Service temporarily unavailable."))
        }

        try {
            val email = decoded["email"] as? String ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("success" to false, "message" to "Unauthorized. Valid bank token required."))
            val snap = db.collection(collectionName).whereEqualTo("email", email).limit(1).get().get()
            if (snap.isEmpty) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("success" to false, "message" to "Bank profile not found."))
            }

            val docData = snap.documents[0].data
            docData.remove("passwordHash")
            docData.remove("registrationIp")

            return ResponseEntity.ok(mapOf("success" to true, "data" to docData))
        } catch (e: Exception) {
            println("[BankAuth] getMe failed: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("success" to false, "message" to "Failed to fetch profile."))
        }
    }
}
