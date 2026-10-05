package com.bloodsync.services

import com.bloodsync.config.Constants.ERROR_CATEGORIES
import com.bloodsync.config.Constants.FIRESTORE_COLLECTIONS
import com.bloodsync.config.Environment.SECURITY_CONFIG
import com.bloodsync.config.FirebaseAdmin.db
import com.bloodsync.utils.CryptoUtils.generateSecureOtp
import com.bloodsync.utils.CryptoUtils.hashOtp
import com.bloodsync.utils.CryptoUtils.timingSafeVerify
import com.google.cloud.firestore.FieldValue
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.time.Instant

@Service
class OtpService {
    data class OtpRecord(
        val emailHash: String,
        val hashedOtp: String,
        var attempts: Int,
        val maxAttempts: Int,
        var verified: Boolean,
        val requestId: String,
        val createdAtMs: Long,
        val expiresAtMs: Long
    )

    private val inMemoryOtpStore = ConcurrentHashMap<String, OtpRecord>()

    private fun getDocumentKey(email: String): String {
        val normalized = email.trim().lowercase()
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(normalized.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    data class CreateOtpResult(val otp: String, val expiresAt: Instant, val hashedOtp: String)

    suspend fun createAndStoreOtp(email: String, requestId: String): CreateOtpResult {
        val normalizedEmail = email.trim().lowercase()
        val docKey = getDocumentKey(normalizedEmail)

        val otp = generateSecureOtp(6)
        val hashedOtp = hashOtp(normalizedEmail, otp)

        val nowMs = System.currentTimeMillis()
        val expiryMs = nowMs + SECURITY_CONFIG.otpExpiryMinutes * 60 * 1000
        val expiresAt = Instant.ofEpochMilli(expiryMs)

        val record = OtpRecord(
            emailHash = docKey,
            hashedOtp = hashedOtp,
            attempts = 0,
            maxAttempts = SECURITY_CONFIG.otpMaxAttempts,
            verified = false,
            requestId = requestId,
            createdAtMs = nowMs,
            expiresAtMs = expiryMs
        )

        inMemoryOtpStore[docKey] = record

        if (db != null) {
            try {
                val docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS).document(docKey)
                docRef.set(mapOf(
                    "emailHash" to docKey,
                    "hashedOtp" to hashedOtp,
                    "attempts" to 0,
                    "maxAttempts" to SECURITY_CONFIG.otpMaxAttempts,
                    "verified" to false,
                    "requestId" to requestId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to expiresAt.toString(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )).get()
            } catch (dbError: Exception) {
                println("[OtpService] Firestore error while saving OTP record: ${dbError.message}")
                throw RuntimeException("Database failed to store verification record").apply {
                    // Custom category mapping would require a custom Exception class, using generic runtime exception for now
                }
            }
        }

        return CreateOtpResult(otp, expiresAt, hashedOtp)
    }

    data class VerifyOtpResult(val success: Boolean, val reason: String? = null, val errorCategory: String? = null)

    suspend fun verifyOtp(email: String, candidateOtp: String, requestId: String): VerifyOtpResult {
        val normalizedEmail = email.trim().lowercase()
        val docKey = getDocumentKey(normalizedEmail)
        val now = System.currentTimeMillis()

        var record: OtpRecord? = null

        if (db != null) {
            try {
                val docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS).document(docKey)
                val snapshot = docRef.get().get()
                if (snapshot.exists) {
                    val data = snapshot.data
                    val expiresAtStr = data?.get("expiresAt") as? String
                    val expiresAtMs = if (expiresAtStr != null) Instant.parse(expiresAtStr).toEpochMilli() else 0L
                    val attempts = (data?.get("attempts") as? Long)?.toInt() ?: 0
                    val maxAttempts = (data?.get("maxAttempts") as? Long)?.toInt() ?: SECURITY_CONFIG.otpMaxAttempts

                    record = OtpRecord(
                        emailHash = data?.get("emailHash") as? String ?: docKey,
                        hashedOtp = data?.get("hashedOtp") as? String ?: "",
                        attempts = attempts,
                        maxAttempts = maxAttempts,
                        verified = data?.get("verified") as? Boolean ?: false,
                        requestId = data?.get("requestId") as? String ?: "",
                        createdAtMs = 0L,
                        expiresAtMs = expiresAtMs
                    )
                }
            } catch (dbError: Exception) {
                println("[OtpService] Failed to read OTP record from Firestore: ${dbError.message}")
            }
        }

        if (record == null) {
            record = inMemoryOtpStore[docKey]
        }

        if (record == null) {
            return VerifyOtpResult(false, "No pending verification request found for this email. Please request a new code.", ERROR_CATEGORIES.UNKNOWN_ERROR)
        }

        if (record.verified) {
            return VerifyOtpResult(false, "This verification code has already been used. Please request a new code.", ERROR_CATEGORIES.UNKNOWN_ERROR)
        }

        if (record.attempts >= record.maxAttempts) {
            return VerifyOtpResult(false, "Maximum verification attempts exceeded. Please request a new code.", ERROR_CATEGORIES.RATE_LIMIT_ERROR)
        }

        if (now > record.expiresAtMs) {
            return VerifyOtpResult(false, "Verification code has expired. Please request a new code.", ERROR_CATEGORIES.UNKNOWN_ERROR)
        }

        val candidateHash = hashOtp(normalizedEmail, candidateOtp.trim())
        val isMatch = timingSafeVerify(candidateHash, record.hashedOtp)

        if (!isMatch) {
            val updatedAttempts = record.attempts + 1
            val remainingAttempts = Math.max(0, record.maxAttempts - updatedAttempts)

            if (db != null) {
                val docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS).document(docKey)
                try {
                    docRef.update(
                        "attempts", FieldValue.increment(1),
                        "lastFailedAttemptAt", FieldValue.serverTimestamp()
                    ).get()
                } catch (e: Exception) {
                    println("Failed to update attempts: ${e.message}")
                }
            }
            
            inMemoryOtpStore[docKey]?.attempts = updatedAttempts

            val reason = if (remainingAttempts > 0) {
                "Invalid verification code. $remainingAttempts attempts remaining."
            } else {
                "Invalid verification code. Maximum attempts exceeded. Please request a new code."
            }
            return VerifyOtpResult(false, reason, ERROR_CATEGORIES.UNKNOWN_ERROR)
        }

        if (db != null) {
            val docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS).document(docKey)
            try {
                docRef.update(
                    "verified", true,
                    "verifiedAt", FieldValue.serverTimestamp(),
                    "verifiedRequestId", requestId
                ).get()
            } catch (e: Exception) {
                println("Failed to mark verified: ${e.message}")
            }
        }
        inMemoryOtpStore[docKey]?.verified = true

        return VerifyOtpResult(true)
    }
}
