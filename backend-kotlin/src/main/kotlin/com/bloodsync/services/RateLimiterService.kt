package com.bloodsync.services

import com.bloodsync.config.AppConfig
import com.bloodsync.config.FirebaseConfig
import com.bloodsync.config.FirestoreCollections
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * BloodSync Backend - Rate Limiter Service (Kotlin)
 *
 * Enforces per-email OTP rate limiting and resend cooldowns using Firestore.
 * Mirrors rateLimiterService.js 1:1.
 */
object RateLimiterService {

    private data class MemEntry(val requests: MutableList<Long>, var lastRequestAt: Long)
    private val inMemoryCache = ConcurrentHashMap<String, MemEntry>()

    fun getEmailKey(email: String): String {
        val normalized = email.trim().lowercase()
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    data class RateLimitResult(
        val allowed: Boolean,
        val reason: String? = null,
        val retryAfterSeconds: Int? = null
    )

    suspend fun checkRateLimit(email: String): RateLimitResult {
        val key = getEmailKey(email)
        val now = System.currentTimeMillis()
        val windowMs = AppConfig.rateLimitWindowMinutes * 60 * 1000L
        val cooldownMs = AppConfig.rateLimitCooldownSeconds * 1000L
        val maxRequests = AppConfig.rateLimitMaxRequests

        val db = FirebaseConfig.firestore ?: return checkMemoryRateLimit(key, now, windowMs, cooldownMs, maxRequests)

        return withContext(Dispatchers.IO) {
            try {
                val doc = db.collection(FirestoreCollections.OTP_RATE_LIMITS).document(key).get().get()
                if (!doc.exists()) return@withContext RateLimitResult(true)

                val data = doc.data ?: return@withContext RateLimitResult(true)
                val lastRequestTs = data["lastRequestAt"] as? Timestamp
                val lastRequestAt = lastRequestTs?.toDate()?.time ?: 0L

                @Suppress("UNCHECKED_CAST")
                val requestTimestamps = (data["requests"] as? List<Long>) ?: emptyList()

                val timeSinceLast = now - lastRequestAt
                if (timeSinceLast < cooldownMs) {
                    val remaining = Math.ceil((cooldownMs - timeSinceLast) / 1000.0).toInt()
                    return@withContext RateLimitResult(false, "Please wait ${remaining}s before requesting a new verification code.", remaining)
                }

                val recentRequests = requestTimestamps.filter { now - it < windowMs }
                if (recentRequests.size >= maxRequests) {
                    val oldest = recentRequests.first()
                    val resetIn = Math.ceil((windowMs - (now - oldest)) / 1000.0).toInt()
                    return@withContext RateLimitResult(false, "Too many verification requests. Please wait ${resetIn}s.", resetIn)
                }

                RateLimitResult(true)
            } catch (e: Exception) {
                checkMemoryRateLimit(key, now, windowMs, cooldownMs, maxRequests)
            }
        }
    }

    suspend fun recordRequest(email: String) {
        val key = getEmailKey(email)
        val now = System.currentTimeMillis()
        val windowMs = AppConfig.rateLimitWindowMinutes * 60 * 1000L

        val mem = inMemoryCache.getOrPut(key) { MemEntry(mutableListOf(), 0) }
        mem.requests.removeAll { now - it >= windowMs }
        mem.requests.add(now)
        mem.lastRequestAt = now

        val db = FirebaseConfig.firestore ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                val docRef = db.collection(FirestoreCollections.OTP_RATE_LIMITS).document(key)
                val doc = docRef.get().get()
                val current = if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    (doc.data?.get("requests") as? List<Long>) ?: emptyList()
                } else emptyList()

                val updated = current.filter { now - it < windowMs }.toMutableList().also { it.add(now) }
                docRef.set(
                    mapOf(
                        "emailHash" to key,
                        "lastRequestAt" to Timestamp.ofTimeSecondsAndNanos(now / 1000, 0),
                        "requests" to updated,
                        "expiresAt" to Timestamp.ofTimeSecondsAndNanos((now + 24 * 60 * 60 * 1000) / 1000, 0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.cloud.firestore.SetOptions.merge()
                ).get()
            }
        }
    }

    private fun checkMemoryRateLimit(key: String, now: Long, windowMs: Long, cooldownMs: Long, maxRequests: Int): RateLimitResult {
        val entry = inMemoryCache[key] ?: return RateLimitResult(true)
        val timeSinceLast = now - entry.lastRequestAt
        if (timeSinceLast < cooldownMs) {
            val remaining = Math.ceil((cooldownMs - timeSinceLast) / 1000.0).toInt()
            return RateLimitResult(false, "Please wait ${remaining}s before requesting another code.", remaining)
        }
        val recent = entry.requests.filter { now - it < windowMs }
        if (recent.size >= maxRequests) {
            val resetIn = Math.ceil((windowMs - (now - recent.first())) / 1000.0).toInt()
            return RateLimitResult(false, "Rate limit reached. Try again in ${resetIn}s.", resetIn)
        }
        return RateLimitResult(true)
    }
}
