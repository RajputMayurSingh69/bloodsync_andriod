package com.bloodsync.services

import com.bloodsync.config.AppConfig
import com.bloodsync.config.FirebaseConfig
import com.bloodsync.config.FirestoreCollections
import com.bloodsync.config.RequestStatus
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max

/**
 * BloodSync Backend - OTP Service (Kotlin)
 *
 * Handles CSPRNG OTP generation, secure HMAC-SHA256 storage in Firestore,
 * and timing-safe verification. Mirrors otpService.js 1:1.
 */
object OtpService {

    // In-memory fallback if Firestore is unavailable
    private data class OtpRecord(
        val emailHash: String,
        val hashedOtp: String,
        var attempts: Int,
        val maxAttempts: Int,
        val expiresAtMs: Long,
        var verified: Boolean,
        val requestId: String
    )

    private val inMemoryOtpStore = ConcurrentHashMap<String, OtpRecord>()

    /** SHA-256 hash of email for use as Firestore document key */
    fun getDocumentKey(email: String): String {
        val normalized = email.trim().lowercase()
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /** Generates a cryptographically secure N-digit OTP */
    private fun generateSecureOtp(digits: Int): String {
        val max = Math.pow(10.0, digits.toDouble()).toLong()
        val random = java.security.SecureRandom().nextLong().let { Math.abs(it) % max }
        return random.toString().padStart(digits, '0')
    }

    /** HMAC-SHA256 of "email:otp" with server secret */
    private fun hashOtp(email: String, otp: String): String {
        val key = SecretKeySpec(AppConfig.otpHmacSecret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
        return mac.doFinal("$email:$otp".toByteArray()).joinToString("") { "%02x".format(it) }
    }

    /** Timing-safe comparison of two HMAC strings (prevents timing attacks) */
    private fun timingSafeVerify(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }

    data class OtpCreationResult(val otp: String, val expiresAt: Long, val hashedOtp: String)

    /**
     * Generates a 6-digit OTP, stores its HMAC hash in Firestore,
     * and returns the plaintext OTP solely for SMTP transmission.
     */
    suspend fun createAndStoreOtp(email: String, requestId: String): OtpCreationResult {
        val normalizedEmail = email.trim().lowercase()
        val docKey = getDocumentKey(normalizedEmail)
        val otp = generateSecureOtp(6)
        val hashedOtp = hashOtp(normalizedEmail, otp)
        val nowMs = System.currentTimeMillis()
        val expiryMs = nowMs + AppConfig.otpExpiryMinutes * 60 * 1000L

        val record = OtpRecord(
            emailHash = docKey,
            hashedOtp = hashedOtp,
            attempts = 0,
            maxAttempts = AppConfig.otpMaxAttempts,
            expiresAtMs = expiryMs,
            verified = false,
            requestId = requestId
        )
        inMemoryOtpStore[docKey] = record

        val db = FirebaseConfig.firestore
        if (db != null) {
            withContext(Dispatchers.IO) {
                try {
                    db.collection(FirestoreCollections.OTP_VERIFICATIONS).document(docKey).set(
                        mapOf(
                            "emailHash" to docKey,
                            "hashedOtp" to hashedOtp,
                            "attempts" to 0,
                            "maxAttempts" to AppConfig.otpMaxAttempts,
                            "verified" to false,
                            "requestId" to requestId,
                            "createdAt" to Timestamp.ofTimeSecondsAndNanos(nowMs / 1000, 0),
                            "expiresAt" to Timestamp.ofTimeSecondsAndNanos(expiryMs / 1000, 0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).get()
                } catch (e: Exception) {
                    throw Exception("Database failed to store verification record").also {
                        it.initCause(e)
                    }
                }
            }
        }

        return OtpCreationResult(otp = otp, expiresAt = expiryMs, hashedOtp = hashedOtp)
    }

    data class VerifyResult(
        val success: Boolean,
        val reason: String? = null,
        val errorCategory: String? = null
    )

    /**
     * Verifies a 6-digit OTP against the stored HMAC hash.
     * Enforces expiry, max attempts, and timing-safe comparison.
     */
    suspend fun verifyOtp(email: String, candidateOtp: String, requestId: String): VerifyResult {
        val normalizedEmail = email.trim().lowercase()
        val docKey = getDocumentKey(normalizedEmail)
        val now = System.currentTimeMillis()

        var record: OtpRecord? = null

        val db = FirebaseConfig.firestore
        if (db != null) {
            withContext(Dispatchers.IO) {
                try {
                    val snapshot = db.collection(FirestoreCollections.OTP_VERIFICATIONS).document(docKey).get().get()
                    if (snapshot.exists()) {
                        val data = snapshot.data ?: return@withContext
                        val expiresAtTs = data["expiresAt"] as? Timestamp
                        record = OtpRecord(
                            emailHash = docKey,
                            hashedOtp = data["hashedOtp"] as? String ?: "",
                            attempts = (data["attempts"] as? Long)?.toInt() ?: 0,
                            maxAttempts = (data["maxAttempts"] as? Long)?.toInt() ?: AppConfig.otpMaxAttempts,
                            expiresAtMs = expiresAtTs?.toDate()?.time ?: 0L,
                            verified = data["verified"] as? Boolean ?: false,
                            requestId = data["requestId"] as? String ?: ""
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        if (record == null) record = inMemoryOtpStore[docKey]

        if (record == null) return VerifyResult(false, "No pending verification request found for this email. Please request a new code.", "UNKNOWN_ERROR")
        if (record!!.verified) return VerifyResult(false, "This verification code has already been used. Please request a new code.", "UNKNOWN_ERROR")
        if (record!!.attempts >= record!!.maxAttempts) return VerifyResult(false, "Maximum verification attempts exceeded. Please request a new code.", "RATE_LIMIT_ERROR")
        if (now > record!!.expiresAtMs) return VerifyResult(false, "Verification code has expired. Please request a new code.", "UNKNOWN_ERROR")

        val candidateHash = hashOtp(normalizedEmail, candidateOtp.trim())
        val isMatch = timingSafeVerify(candidateHash, record!!.hashedOtp)

        if (!isMatch) {
            val updatedAttempts = record!!.attempts + 1
            val remaining = max(0, record!!.maxAttempts - updatedAttempts)

            inMemoryOtpStore[docKey]?.let { it.attempts = updatedAttempts }
            if (db != null) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        db.collection(FirestoreCollections.OTP_VERIFICATIONS).document(docKey).update(
                            mapOf(
                                "attempts" to FieldValue.increment(1),
                                "lastFailedAttemptAt" to FieldValue.serverTimestamp()
                            )
                        ).get()
                    }
                }
            }

            val reason = if (remaining > 0)
                "Invalid verification code. $remaining attempts remaining."
            else
                "Invalid verification code. Maximum attempts exceeded. Please request a new code."

            return VerifyResult(false, reason, "UNKNOWN_ERROR")
        }

        // Success: mark as verified
        inMemoryOtpStore[docKey]?.let { it.verified = true }
        if (db != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    db.collection(FirestoreCollections.OTP_VERIFICATIONS).document(docKey).update(
                        mapOf(
                            "verified" to true,
                            "verifiedAt" to FieldValue.serverTimestamp(),
                            "verifiedRequestId" to requestId
                        )
                    ).get()
                }
            }
        }

        return VerifyResult(true)
    }
}
