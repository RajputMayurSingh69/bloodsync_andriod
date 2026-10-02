/**
 * BloodSync Backend - Cryptographic Utilities
 * Provides cryptographically secure random OTP generation, HMAC hashing, and constant-time verification.
 */

const crypto = require('crypto');
const { SECURITY_CONFIG } = require('../config/environment');

/**
 * Generates a cryptographically secure random numeric OTP.
 * Uses crypto.randomInt which is CSPRNG backed (secure against predictability).
 * @param {number} length - Number of digits (default 6)
 * @returns {string} - Random 6-digit numeric string (e.g., "749201")
 */
function generateSecureOtp(length = 6) {
  if (length <= 0 || length > 10) {
    throw new Error('OTP length must be between 1 and 10 digits.');
  }

  const min = Math.pow(10, length - 1);
  const max = Math.pow(10, length);
  const randomNum = crypto.randomInt(min, max);

  return randomNum.toString();
}

/**
 * Computes an HMAC-SHA256 hash of the normalized email and OTP with the server-side salt secret.
 * Storing only the HMAC prevents plaintext exposure even if the database is read.
 * @param {string} email - User email address
 * @param {string} otp - Plaintext OTP
 * @param {string} [salt] - Optional override for salt secret
 * @returns {string} - Hex-encoded HMAC hash
 */
function hashOtp(email, otp, salt = SECURITY_CONFIG.otpSaltSecret) {
  const normalizedEmail = String(email).trim().toLowerCase();
  const payload = `${normalizedEmail}:${otp}`;

  return crypto
    .createHmac('sha256', salt)
    .update(payload)
    .digest('hex');
}

/**
 * Performs timing-safe comparison of two hash strings to prevent side-channel timing attacks.
 * @param {string} hashA - First hash
 * @param {string} hashB - Second hash
 * @returns {boolean} - True if equal, false otherwise
 */
function timingSafeVerify(hashA, hashB) {
  if (typeof hashA !== 'string' || typeof hashB !== 'string') {
    return false;
  }

  const bufA = Buffer.from(hashA, 'hex');
  const bufB = Buffer.from(hashB, 'hex');

  if (bufA.length !== bufB.length) {
    return false;
  }

  return crypto.timingSafeEqual(bufA, bufB);
}

module.exports = {
  generateSecureOtp,
  hashOtp,
  timingSafeVerify,
};
