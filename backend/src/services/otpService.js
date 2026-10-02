/**
 * BloodSync Backend - OTP Service
 * Handles CSPRNG OTP generation, secure HMAC storage in Firestore, and timing-safe verification.
 */

const crypto = require('crypto');
const { admin, db } = require('../config/firebase');
const { SECURITY_CONFIG } = require('../config/environment');
const {
  ERROR_CATEGORIES,
  FIRESTORE_COLLECTIONS,
} = require('../config/constants');
const { generateSecureOtp, hashOtp, timingSafeVerify } = require('../utils/cryptoUtils');

// Fallback in-memory map if Firestore is unavailable in local testing
const inMemoryOtpStore = new Map();

class OtpService {
  /**
   * Generates a unique, deterministic document key for an email.
   * @param {string} email
   * @returns {string}
   */
  getDocumentKey(email) {
    const normalized = String(email).trim().toLowerCase();
    return crypto.createHash('sha256').update(normalized).digest('hex');
  }

  /**
   * Generates a 6-digit OTP, stores its secure HMAC hash in Firestore,
   * and returns the plaintext OTP strictly for transmission to the email service.
   * @param {string} email
   * @param {string} requestId
   * @returns {Promise<{ otp: string, expiresAt: Date, hashedOtp: string }>}
   */
  async createAndStoreOtp(email, requestId) {
    const normalizedEmail = email.trim().toLowerCase();
    const docKey = this.getDocumentKey(normalizedEmail);

    // 1. Generate cryptographically secure 6-digit OTP
    const otp = generateSecureOtp(6);

    // 2. Hash OTP with HMAC-SHA256 and server-side secret
    const hashedOtp = hashOtp(normalizedEmail, otp);

    const nowMs = Date.now();
    const expiryMs = nowMs + SECURITY_CONFIG.otpExpiryMinutes * 60 * 1000;
    const expiresAt = new Date(expiryMs);

    const record = {
      emailHash: docKey,
      hashedOtp,
      attempts: 0,
      maxAttempts: SECURITY_CONFIG.otpMaxAttempts,
      verified: false,
      requestId,
      createdAtMs: nowMs,
      expiresAtMs: expiryMs,
    };

    // Store in-memory cache
    inMemoryOtpStore.set(docKey, record);

    // 3. Store securely in Firestore
    if (db) {
      try {
        const docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS).doc(docKey);
        await docRef.set({
          emailHash: docKey,
          hashedOtp,
          attempts: 0,
          maxAttempts: SECURITY_CONFIG.otpMaxAttempts,
          verified: false,
          requestId,
          createdAt: admin.firestore.Timestamp.fromMillis(nowMs),
          expiresAt: admin.firestore.Timestamp.fromMillis(expiryMs),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      } catch (dbError) {
        console.error(`[OtpService] Firestore error while saving OTP record: ${dbError.message}`);
        const error = new Error('Database failed to store verification record');
        error.category = ERROR_CATEGORIES.OTP_DATABASE_ERROR;
        throw error;
      }
    }

    return {
      otp, // strictly for in-memory email dispatch; never persisted or logged
      expiresAt,
      hashedOtp,
    };
  }

  /**
   * Verifies a user-supplied 6-digit OTP against the stored HMAC hash.
   * Enforces expiry checking, maximum attempts, and timing-safe comparison.
   * @param {string} email
   * @param {string} candidateOtp
   * @param {string} requestId
   * @returns {Promise<{ success: boolean, reason?: string, errorCategory?: string }>}
   */
  async verifyOtp(email, candidateOtp, requestId) {
    const normalizedEmail = email.trim().toLowerCase();
    const docKey = this.getDocumentKey(normalizedEmail);
    const now = Date.now();

    let record = null;
    let docRef = null;

    if (db) {
      try {
        docRef = db.collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS).doc(docKey);
        const snapshot = await docRef.get();
        if (snapshot.exists) {
          const data = snapshot.data();
          record = {
            ...data,
            expiresAtMs: data.expiresAt ? data.expiresAt.toMillis() : 0,
            attempts: data.attempts || 0,
            maxAttempts: data.maxAttempts || SECURITY_CONFIG.otpMaxAttempts,
          };
        }
      } catch (dbError) {
        console.error(`[OtpService] Failed to read OTP record from Firestore: ${dbError.message}`);
      }
    }

    // Fallback to in-memory store if Firestore document was not found or unavailable
    if (!record) {
      record = inMemoryOtpStore.get(docKey);
    }

    if (!record) {
      return {
        success: false,
        reason: 'No pending verification request found for this email. Please request a new code.',
        errorCategory: ERROR_CATEGORIES.UNKNOWN_ERROR,
      };
    }

    // Check if already used
    if (record.verified) {
      return {
        success: false,
        reason: 'This verification code has already been used. Please request a new code.',
        errorCategory: ERROR_CATEGORIES.UNKNOWN_ERROR,
      };
    }

    // Check brute-force attempts
    if (record.attempts >= record.maxAttempts) {
      return {
        success: false,
        reason: 'Maximum verification attempts exceeded. Please request a new code.',
        errorCategory: ERROR_CATEGORIES.RATE_LIMIT_ERROR,
      };
    }

    // Check expiration
    if (now > record.expiresAtMs) {
      return {
        success: false,
        reason: 'Verification code has expired. Please request a new code.',
        errorCategory: ERROR_CATEGORIES.UNKNOWN_ERROR,
      };
    }

    // Compute HMAC of user's candidate OTP
    const candidateHash = hashOtp(normalizedEmail, String(candidateOtp).trim());
    const isMatch = timingSafeVerify(candidateHash, record.hashedOtp);

    if (!isMatch) {
      const updatedAttempts = record.attempts + 1;
      const remainingAttempts = Math.max(0, record.maxAttempts - updatedAttempts);

      // Increment attempt counter in DB
      if (docRef) {
        await docRef.update({
          attempts: admin.firestore.FieldValue.increment(1),
          lastFailedAttemptAt: admin.firestore.FieldValue.serverTimestamp(),
        }).catch((err) => console.warn(`Failed to update attempts: ${err.message}`));
      }
      if (inMemoryOtpStore.has(docKey)) {
        inMemoryOtpStore.get(docKey).attempts = updatedAttempts;
      }

      return {
        success: false,
        reason: remainingAttempts > 0
          ? `Invalid verification code. ${remainingAttempts} attempts remaining.`
          : 'Invalid verification code. Maximum attempts exceeded. Please request a new code.',
        errorCategory: ERROR_CATEGORIES.UNKNOWN_ERROR,
      };
    }

    // Success: Mark as verified and invalidate
    if (docRef) {
      await docRef.update({
        verified: true,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        verifiedRequestId: requestId,
      }).catch((err) => console.warn(`Failed to mark verified: ${err.message}`));
    }
    if (inMemoryOtpStore.has(docKey)) {
      inMemoryOtpStore.get(docKey).verified = true;
    }

    return {
      success: true,
    };
  }
}

module.exports = new OtpService();
