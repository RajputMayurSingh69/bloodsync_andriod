package com.bloodsync.services

import com.bloodsync.config.FirebaseAdmin
import com.bloodsync.config.FirestoreCollections
import com.bloodsync.config.RATE_LIMIT_CONFIG
import com.bloodsync.utils.sha256Hex
import com.google.cloud.Timestamp
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

/**
 * BloodSync Backend - Rate Limiter Service
 * Converted from: backend/src/services/rateLimiterService.js
 *
 * Enforces strict per-email rate limiting and resend cooldowns using Firestore.
 */

private val logger = KotlinLogging.logger {}

data class RateLimitResult(
    val allowed: Boolean,
    val reason: String? = null,
    val retryAfterSeconds: Int? = null,
)

// In-memory fallback if Firestore is temporarily offline
private data class MemEntry(val requests: MutableList<Long>, var lastRequestAt: Long)
private val inMemoryCache = mutableMapOf<String, MemEntry>()

object RateLimiterService {

    /**
     * Hashes the email for use as a Firestore document ID.
     */
    private fun getEmailKey(email: String): String = sha256Hex(email.trim().lowercase())

    /**
     * Checks whether the email has exceeded rate limits or is in a cooldown period.
     */
    suspend fun checkRateLimit(email: String): RateLimitResult {
        val key        = getEmailKey(email)
        val now        = System.currentTimeMillis()
        val windowMs   = RATE_LIMIT_CONFIG.windowMinutes * 60_000L
        val cooldownMs = RATE_LIMIT_CONFIG.cooldownSeconds * 1000L
        val maxReqs    = RATE_LIMIT_CONFIG.maxRequestsPerWindow

        val db = FirebaseAdmin.db ?: return checkMemoryRateLimit(key, now, windowMs, cooldownMs, maxReqs)

        return try {
            val docRef = db.collection(FirestoreCollections.OTP_RATE_LIMITS).document(key)
            val doc    = docRef.get().get()

            if (!doc.exists()) return RateLimitResult(allowed = true)

            val lastRequestAt = (doc["lastRequestAt"] as? Timestamp)?.toDate()?.time ?: 0L

            @Suppress("UNCHECKED_CAST")
            val requestTimestamps = (doc["requests"] as? List<*>)
                ?.mapNotNull {
                    when (it) {
                        is Long      -> it
                        is Number    -> it.toLong()
                        is Timestamp -> it.toDate().time
                        else         -> null
                    }
                } ?: emptyList()

            // 1. Check Cooldown period
            val timeSinceLast = now - lastRequestAt
            if (timeSinceLast < cooldownMs) {
                val remaining = ((cooldownMs - timeSinceLast) / 1000).toInt() + 1
                return RateLimitResult(
                    allowed           = false,
                    reason            = "Please wait ${remaining}s before requesting a new verification code.",
                    retryAfterSeconds = remaining,
                )
            }

            // 2. Check Sliding Window
            val recentRequests = requestTimestamps.filter { now - it < windowMs }
            if (recentRequests.size >= maxReqs) {
                val oldestInWindow = recentRequests.minOrNull() ?: now
                val resetIn = ((windowMs - (now - oldestInWindow)) / 1000).toInt() + 1
                return RateLimitResult(
                    allowed           = false,
                    reason            = "Too many verification requests. Please wait ${resetIn}s before trying again.",
                    retryAfterSeconds = resetIn,
                )
            }

            RateLimitResult(allowed = true)
        } catch (e: Exception) {
            logger.warn { "[RateLimiter] Firestore error, falling back to memory: ${e.message}" }
            checkMemoryRateLimit(key, now, windowMs, cooldownMs, maxReqs)
        }
    }

    /**
     * Records a new OTP request timestamp for the given email.
     */
    suspend fun recordRequest(email: String) {
        val key      = getEmailKey(email)
        val now      = System.currentTimeMillis()
        val windowMs = RATE_LIMIT_CONFIG.windowMinutes * 60_000L

        // Update in-memory cache
        synchronized(inMemoryCache) {
            val entry = inMemoryCache.getOrPut(key) { MemEntry(mutableListOf(), 0L) }
            entry.requests.removeAll { now - it >= windowMs }
            entry.requests.add(now)
            entry.lastRequestAt = now
        }

        // Update Firestore
        val db = FirebaseAdmin.db ?: return
        try {
            val docRef = db.collection(FirestoreCollections.OTP_RATE_LIMITS).document(key)
            val doc    = docRef.get().get()

            val existingRequests: MutableList<Long> = if (doc.exists()) {
                @Suppress("UNCHECKED_CAST")
                (doc["requests"] as? List<*>)
                    ?.mapNotNull {
                        when (it) {
                            is Long -> it; is Number -> it.toLong()
                            is Timestamp -> it.toDate().time; else -> null
                        }
                    }?.toMutableList() ?: mutableListOf()
            } else mutableListOf()

            existingRequests.removeAll { now - it >= windowMs }
            existingRequests.add(now)

            docRef.set(
                mapOf(
                    "emailHash"     to key,
                    "lastRequestAt" to Timestamp.ofTimeSecondsAndNanos(now / 1000, ((now % 1000) * 1_000_000).toInt()),
                    "requests"      to existingRequests,
                    "expiresAt"     to Timestamp.ofTimeSecondsAndNanos(
                        Instant.now().plusSeconds(24 * 60 * 60).epochSecond, 0
                    ),
                )
            ).get()
        } catch (e: Exception) {
            logger.error { "[RateLimiter] Failed to record request in Firestore: ${e.message}" }
        }
    }

    /** Memory-based fallback rate-limit check */
    private fun checkMemoryRateLimit(
        key: String, now: Long, windowMs: Long, cooldownMs: Long, maxReqs: Int,
    ): RateLimitResult {
        val entry = synchronized(inMemoryCache) { inMemoryCache[key] } ?: return RateLimitResult(allowed = true)
        val timeSinceLast = now - entry.lastRequestAt
        if (timeSinceLast < cooldownMs) {
            val remaining = ((cooldownMs - timeSinceLast) / 1000).toInt() + 1
            return RateLimitResult(allowed = false,
                reason = "Please wait ${remaining}s before requesting another code.",
                retryAfterSeconds = remaining)
        }
        val recent = entry.requests.filter { now - it < windowMs }
        if (recent.size >= maxReqs) {
            val resetIn = ((windowMs - (now - (recent.minOrNull() ?: now))) / 1000).toInt() + 1
            return RateLimitResult(allowed = false,
                reason = "Rate limit reached. Try again in ${resetIn}s.",
                retryAfterSeconds = resetIn)
        }
        return RateLimitResult(allowed = true)
    }
}
