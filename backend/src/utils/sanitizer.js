/**
 * BloodSync Backend - Sanitization & Privacy Utilities
 * Ensures zero-leakage of OTPs, credentials, and sensitive PII in logs or alerts.
 */

/**
 * Masks an email address for privacy-compliant logging and UI display.
 * Examples:
 *   "alex@gmail.com"        -> "a***@gmail.com"
 *   "john.doe@domain.com"   -> "j***e@domain.com"
 *   "a@b.com"               -> "a***@b.com"
 * @param {string} email
 * @returns {string} Masked email string
 */
function maskEmail(email) {
  if (!email || typeof email !== 'string') {
    return 'unknown@masked.local';
  }

  const parts = email.trim().toLowerCase().split('@');
  if (parts.length !== 2) {
    return 'invalid_email_format';
  }

  const [username, domain] = parts;

  if (username.length <= 1) {
    return `${username}***@${domain}`;
  }

  if (username.length === 2) {
    return `${username[0]}***@${domain}`;
  }

  // First char, asterisks, last char
  const firstChar = username[0];
  const lastChar = username[username.length - 1];
  return `${firstChar}***${lastChar}@${domain}`;
}

/**
 * Anonymizes client IP addresses (e.g. 192.168.1.45 -> 192.168.1.0/24 or hash)
 * @param {string} ip
 * @returns {string}
 */
function sanitizeIp(ip) {
  if (!ip || typeof ip !== 'string') return '0.0.0.0';

  // IPv4
  if (ip.includes('.')) {
    const segments = ip.split('.');
    if (segments.length === 4) {
      return `${segments[0]}.${segments[1]}.${segments[2]}.xxx`;
    }
  }

  // IPv6
  if (ip.includes(':')) {
    const segments = ip.split(':');
    return `${segments.slice(0, 3).join(':')}:xxxx:xxxx`;
  }

  return 'anonymized-ip';
}

/**
 * Deeply sanitizes an object or error before recording to Firestore or external logs.
 * Strips out any occurrence of passwords, private keys, authorization headers, or plain OTPs.
 * @param {any} input
 * @returns {any}
 */
function sanitizeDetails(input) {
  if (!input) return input;

  if (typeof input === 'string') {
    return input
      .replace(/password\s*[:=]\s*["']?[^"',\s]+["']?/gi, 'password:[REDACTED]')
      .replace(/pass\s*[:=]\s*["']?[^"',\s]+["']?/gi, 'pass:[REDACTED]')
      .replace(/otp\s*[:=]\s*["']?\d{4,8}["']?/gi, 'otp:[REDACTED]')
      .replace(/Bearer\s+[A-Za-z0-9-_=.]+/gi, 'Bearer [REDACTED]')
      .replace(/-----BEGIN PRIVATE KEY-----[\s\S]*?-----END PRIVATE KEY-----/gi, '[REDACTED_PRIVATE_KEY]')
      .replace(/ai-za[0-9a-z_-]{35}/gi, '[REDACTED_API_KEY]');
  }

  if (typeof input !== 'object') {
    return input;
  }

  if (Array.isArray(input)) {
    return input.map((item) => sanitizeDetails(item));
  }

  const sanitized = {};
  const sensitiveKeys = [
    'password',
    'pass',
    'smtp_password',
    'private_key',
    'privatekey',
    'otp',
    'secret',
    'jwt_secret',
    'token',
    'authorization',
  ];

  for (const [key, value] of Object.entries(input)) {
    const lowerKey = key.toLowerCase();
    const isSensitive = sensitiveKeys.some((s) => lowerKey.includes(s));

    if (isSensitive) {
      sanitized[key] = '[REDACTED]';
    } else if (typeof value === 'object' && value !== null) {
      sanitized[key] = sanitizeDetails(value);
    } else if (typeof value === 'string') {
      sanitized[key] = sanitizeDetails(value);
    } else {
      sanitized[key] = value;
    }
  }

  return sanitized;
}

module.exports = {
  maskEmail,
  sanitizeIp,
  sanitizeDetails,
};
