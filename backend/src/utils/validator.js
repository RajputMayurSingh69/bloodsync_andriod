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

module.exports = {
  isValidEmail,
  isValidOtpFormat,
};
