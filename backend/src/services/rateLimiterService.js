/**
 * BloodSync Backend - Rate Limiter Service
 * Enforces strict per-email rate limiting and resend cooldowns using Firestore.
 */

const { admin, db } = require('../config/firebase');
const { RATE_LIMIT_CONFIG } = require('../config/environment');
const { FIRESTORE_COLLECTIONS } = require('../config/constants');
const crypto = require('crypto');

// In-memory fallback if Firestore is temporarily offline
const inMemoryCache = new Map();

class RateLimiterService {
  /**
   * Hashes the email for use as a Firestore document ID to avoid forbidden characters.
   * @param {string} email
   * @returns {string}
   */
  getEmailKey(email) {
    const normalized = String(email).trim().toLowerCase();
    return crypto.createHash('sha256').update(normalized).digest('hex');
  }

  /**
   * Checks whether the email has exceeded rate limits or is in a cooldown period.
   * @param {string} email
   * @returns {Promise<{ allowed: boolean, reason?: string, retryAfterSeconds?: number }>}
   */
  async checkRateLimit(email) {
    const key = this.getEmailKey(email);
    const now = Date.now();
    const windowMs = RATE_LIMIT_CONFIG.windowMinutes * 60 * 1000;
    const cooldownMs = RATE_LIMIT_CONFIG.cooldownSeconds * 1000;
    const maxRequests = RATE_LIMIT_CONFIG.maxRequestsPerWindow;

    try {
      if (!db) {
        return this.checkMemoryRateLimit(key, now, windowMs, cooldownMs, maxRequests);
      }

      const docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_RATE_LIMITS).doc(key);
      const doc = await docRef.get();

      if (!doc.exists) {
        return { allowed: true };
      }

      const data = doc.data();
      const lastRequestAt = data.lastRequestAt ? data.lastRequestAt.toMillis() : 0;
      const requestTimestamps = Array.isArray(data.requests) ? data.requests : [];

      // 1. Check Cooldown period (e.g., minimum 60s between OTP requests)
      const timeSinceLast = now - lastRequestAt;
      if (timeSinceLast < cooldownMs) {
        const remainingCooldown = Math.ceil((cooldownMs - timeSinceLast) / 1000);
        return {
          allowed: false,
          reason: `Please wait ${remainingCooldown}s before requesting a new verification code.`,
          retryAfterSeconds: remainingCooldown,
        };
      }

      // 2. Check Sliding Window (filter timestamps within window)
      const recentRequests = requestTimestamps.filter((ts) => now - ts < windowMs);

      if (recentRequests.length >= maxRequests) {
        const oldestInWindow = recentRequests[0];
        const resetInSeconds = Math.ceil((windowMs - (now - oldestInWindow)) / 1000);
        return {
          allowed: false,
          reason: `Too many verification requests. Please wait ${resetInSeconds}s before trying again.`,
          retryAfterSeconds: resetInSeconds,
        };
      }

      return { allowed: true };
    } catch (error) {
      console.warn(`[RateLimiter] Firestore error, falling back to memory: ${error.message}`);
      return this.checkMemoryRateLimit(key, now, windowMs, cooldownMs, maxRequests);
    }
  }

  /**
   * Records a new OTP request timestamp for the given email.
   * @param {string} email
   */
  async recordRequest(email) {
    const key = this.getEmailKey(email);
    const now = Date.now();
    const windowMs = RATE_LIMIT_CONFIG.windowMinutes * 60 * 1000;

    // Record in memory
    const memEntry = inMemoryCache.get(key) || { requests: [], lastRequestAt: 0 };
    memEntry.requests = memEntry.requests.filter((ts) => now - ts < windowMs);
    memEntry.requests.push(now);
    memEntry.lastRequestAt = now;
    inMemoryCache.set(key, memEntry);

    // Record in Firestore
    try {
      if (db) {
        const docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_RATE_LIMITS).doc(key);
        const doc = await docRef.get();

        let updatedRequests = [now];
        if (doc.exists) {
          const current = doc.data().requests || [];
          updatedRequests = current.filter((ts) => now - ts < windowMs);
          updatedRequests.push(now);
        }

        await docRef.set({
          emailHash: key,
          lastRequestAt: admin.firestore.Timestamp.fromMillis(now),
          requests: updatedRequests,
          expiresAt: admin.firestore.Timestamp.fromMillis(now + 24 * 60 * 60 * 1000), // 24h retention TTL
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
      }
    } catch (error) {
      console.error(`[RateLimiter] Failed to record request in Firestore: ${error.message}`);
    }
  }

  /**
   * Memory-based fallback check
   */
  checkMemoryRateLimit(key, now, windowMs, cooldownMs, maxRequests) {
    const entry = inMemoryCache.get(key);
    if (!entry) return { allowed: true };

    const timeSinceLast = now - entry.lastRequestAt;
    if (timeSinceLast < cooldownMs) {
      const remainingCooldown = Math.ceil((cooldownMs - timeSinceLast) / 1000);
      return {
        allowed: false,
        reason: `Please wait ${remainingCooldown}s before requesting another code.`,
        retryAfterSeconds: remainingCooldown,
      };
    }

    const recent = entry.requests.filter((ts) => now - ts < windowMs);
    if (recent.length >= maxRequests) {
      const resetInSeconds = Math.ceil((windowMs - (now - recent[0])) / 1000);
      return {
        allowed: false,
        reason: `Rate limit reached. Try again in ${resetInSeconds}s.`,
        retryAfterSeconds: resetInSeconds,
      };
    }

    return { allowed: true };
  }
}

module.exports = new RateLimiterService();
