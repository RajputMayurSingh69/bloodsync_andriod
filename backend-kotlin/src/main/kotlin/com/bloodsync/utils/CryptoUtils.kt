package com.bloodsync.utils

import com.bloodsync.config.SECURITY_CONFIG
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * BloodSync Backend - Cryptographic Utilities
 * Converted from: backend/src/utils/cryptoUtils.js
 */

private val secureRandom = SecureRandom()

/**
 * Generates a cryptographically secure random numeric OTP of the specified [length].
 * Uses Java's SecureRandom (CSPRNG-backed) — equivalent to Node's crypto.randomInt.
 *
 * @param length Number of digits (1–10)
 * @return Random numeric string, e.g. "749201"
 */
fun generateSecureOtp(length: Int = 6): String {
    require(length in 1..10) { "OTP length must be between 1 and 10 digits." }
    val min = Math.pow(10.0, (length - 1).toDouble()).toLong()
    val max = Math.pow(10.0, length.toDouble()).toLong()
    val range = max - min
    val randomNum = (secureRandom.nextLong() % range + range) % range + min
    return randomNum.toString().padStart(length, '0').takeLast(length)
}

/**
 * Computes an HMAC-SHA256 hash of the normalised [email] + [otp] with the server-side [salt].
 * Storing only the HMAC prevents plaintext OTP exposure even if the database is read.
 *
 * @param email User email address
 * @param otp   Plaintext OTP
 * @param salt  Optional override for salt secret
 * @return Hex-encoded HMAC hash
 */
fun hashOtp(
    email: String,
    otp: String,
    salt: String = SECURITY_CONFIG.otpSaltSecret,
): String {
    val normalizedEmail = email.trim().lowercase()
    val payload = "$normalizedEmail:$otp"
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(salt.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).toHexString()
}

/**
 * Performs a timing-safe comparison of two hex-encoded hash strings.
 * Prevents side-channel timing attacks — equivalent to Node's crypto.timingSafeEqual.
 *
 * @param hashA First hash
 * @param hashB Second hash
 * @return true if equal, false otherwise
 */
fun timingSafeVerify(hashA: String, hashB: String): Boolean {
    if (hashA.length != hashB.length) return false
    val bufA = hexToBytes(hashA) ?: return false
    val bufB = hexToBytes(hashB) ?: return false
    return MessageDigest.isEqual(bufA, bufB)
}

/**
 * Computes SHA-256 hash of the given [input] string and returns hex-encoded result.
 * Used for deterministic document key generation (e.g. email -> Firestore doc ID).
 */
fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray(Charsets.UTF_8)).toHexString()
}

// ---- Internal helpers ----

private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

private fun hexToBytes(hex: String): ByteArray? {
    if (hex.length % 2 != 0) return null
    return try {
        ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    } catch (_: NumberFormatException) {
        null
    }
}
