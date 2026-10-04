/**
 * BloodSync Backend - Request Validators
 */

// Strict RFC 5322 compliant regex for standard email addresses
const EMAIL_REGEX = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/;

/**
 * Validates whether the given string is a valid email format.
 * @param {string} email
 * @returns {boolean}
 */
function isValidEmail(email) {
  if (!email || typeof email !== 'string') {
    return false;
  }
  const trimmed = email.trim();
  if (trimmed.length < 5 || trimmed.length > 254) {
    return false;
  }
  return EMAIL_REGEX.test(trimmed);
}

/**
 * Validates if the OTP is a valid 6-digit numeric string.
 * @param {string} otp
 * @returns {boolean}
 */
function isValidOtpFormat(otp) {
  if (!otp || (typeof otp !== 'string' && typeof otp !== 'number')) {
    return false;
  }
  const otpStr = String(otp).trim();
  return /^\d{6}$/.test(otpStr);
}

/**
 * Validates a phone number (Indian & international).
 * Accepts optional leading +, spaces, dashes, parens.
 * Requires minimum 10 digits.
 * @param {string} phone
 * @returns {boolean}
 */
function isValidPhone(phone) {
  if (!phone || typeof phone !== 'string') return false;
  const digits = phone.replace(/\D/g, '');
  return digits.length >= 10 && digits.length <= 15;
}

/**
 * Validates that a string is non-empty after trimming.
 * @param {string} value
 * @param {number} [minLength=1]
 * @returns {boolean}
 */
function isNonEmptyString(value, minLength = 1) {
  if (!value || typeof value !== 'string') return false;
  return value.trim().length >= minLength;
}

module.exports = {
  isValidEmail,
  isValidOtpFormat,
  isValidPhone,
  isNonEmptyString,
};
