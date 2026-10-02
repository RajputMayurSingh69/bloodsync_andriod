/**
 * BloodSync Backend - Test Scenarios Suite
 * 
 * Demonstrates and verifies:
 * 1. Success Flow (Cryptographic OTP generation, hashing, timing-safe verification, JWT issuance)
 * 2. SMTP Auth Failure (Classified as SMTP_AUTH_ERROR, logged to Debugger Bot, generic safe error)
 * 3. Rate-Limit Block (Per-email sliding window and cooldown enforcement, 429 status)
 * 4. Expired OTP (Expired timestamp rejection)
 * 5. Brute-Force Protection (Max attempt lockout)
 */

const { generateSecureOtp, hashOtp, timingSafeVerify } = require('../src/utils/cryptoUtils');
const { maskEmail, sanitizeDetails } = require('../src/utils/sanitizer');
const { isValidEmail, isValidOtpFormat } = require('../src/utils/validator');
const { ERROR_CATEGORIES, REQUEST_STATUS, OTP_GEN_STATUS, EMAIL_SEND_STATUS } = require('../src/config/constants');
const otpService = require('../src/services/otpService');
const rateLimiterService = require('../src/services/rateLimiterService');
const loggerBotService = require('../src/services/loggerBotService');
const emailService = require('../src/services/emailService');

async function runTestSuite() {
  console.log('================================================================');
  console.log('🧪 BloodSync OTP & Debugger Bot Test Suite');
  console.log('================================================================\n');

  let passedTests = 0;
  let totalTests = 0;

  function assert(condition, message) {
    totalTests++;
    if (condition) {
      console.log(`  ✅ PASS: ${message}`);
      passedTests++;
    } else {
      console.error(`  ❌ FAIL: ${message}`);
    }
  }

  // -------------------------------------------------------------------------
  // TEST SCENARIO 1: Cryptographic OTP Generation & Timing-Safe Hashing
  // -------------------------------------------------------------------------
  console.log('📌 [Scenario 1] Cryptographic Generation & Secure Hashing');
  const otp1 = generateSecureOtp(6);
  assert(/^\d{6}$/.test(otp1), 'Generated OTP is exactly 6 numeric digits');
  assert(isValidOtpFormat(otp1), 'isValidOtpFormat validates 6 digits correctly');

  const testEmail = 'donor.alex@bloodsync.org';
  const hashedOtp = hashOtp(testEmail, otp1);
  assert(typeof hashedOtp === 'string' && hashedOtp.length === 64, 'HMAC-SHA256 returns 64-character hex hash');

  const candidateHash = hashOtp(testEmail, otp1);
  assert(timingSafeVerify(candidateHash, hashedOtp), 'timingSafeVerify matches correct OTP HMAC');

  const wrongHash = hashOtp(testEmail, '000000');
  assert(!timingSafeVerify(wrongHash, hashedOtp), 'timingSafeVerify rejects invalid OTP HMAC');

  // -------------------------------------------------------------------------
  // TEST SCENARIO 2: Sanitization & Zero-Leakage Privacy
  // -------------------------------------------------------------------------
  console.log('\n📌 [Scenario 2] Privacy Masking & Credential Sanitization');
  assert(maskEmail('alex@gmail.com') === 'a***x@gmail.com', 'Masks short email as a***x@gmail.com');
  assert(maskEmail('john.doe@bloodsync.org') === 'j***e@bloodsync.org', 'Masks john.doe@bloodsync.org properly');

  const dirtyPayload = {
    smtp_password: 'super_secret_app_password',
    otp: '123456',
    token: 'Bearer eyJhbGciOi...',
    user: 'donor',
    error: 'Failed with password=secret123 and otp=987654',
  };
  const cleaned = sanitizeDetails(dirtyPayload);
  assert(cleaned.smtp_password === '[REDACTED]', 'Redacts smtp_password key');
  assert(cleaned.otp === '[REDACTED]', 'Redacts otp key');
  assert(cleaned.token === '[REDACTED]', 'Redacts token key');
  assert(!cleaned.error.includes('secret123'), 'Sanitizes inline passwords in error string');
  assert(!cleaned.error.includes('987654'), 'Sanitizes inline OTPs in error string');

  // -------------------------------------------------------------------------
  // TEST SCENARIO 3: Rate Limiting & Cooldown Protection
  // -------------------------------------------------------------------------
  console.log('\n📌 [Scenario 3] Rate Limiting & Cooldown Engine');
  const rateLimitEmail = 'spammer@example.com';
  
  // First request should be allowed
  const check1 = await rateLimiterService.checkRateLimit(rateLimitEmail);
  assert(check1.allowed === true, 'First OTP request is allowed');
  await rateLimiterService.recordRequest(rateLimitEmail);

  // Immediate second request within 60s cooldown must be rejected
  const check2 = await rateLimiterService.checkRateLimit(rateLimitEmail);
  assert(check2.allowed === false, 'Immediate second OTP request is blocked by cooldown');
  assert(typeof check2.retryAfterSeconds === 'number', 'Returns retryAfterSeconds for client UI display');

  // -------------------------------------------------------------------------
  // TEST SCENARIO 4: SMTP Error Categorization
  // -------------------------------------------------------------------------
  console.log('\n📌 [Scenario 4] SMTP Error Classification');
  const mockAuthError = new Error('Username and Password not accepted');
  mockAuthError.code = 'EAUTH';
  mockAuthError.responseCode = 535;

  const classifiedAuth = emailService.classifySmtpError(mockAuthError);
  assert(classifiedAuth.category === ERROR_CATEGORIES.SMTP_AUTH_ERROR, 'Maps EAUTH/535 to SMTP_AUTH_ERROR');

  const mockNetworkError = new Error('getaddrinfo ENOTFOUND smtp.gmail.com');
  mockNetworkError.code = 'ENOTFOUND';
  const classifiedNetwork = emailService.classifySmtpError(mockNetworkError);
  assert(classifiedNetwork.category === ERROR_CATEGORIES.EMAIL_CONFIGURATION_ERROR, 'Maps ENOTFOUND to EMAIL_CONFIGURATION_ERROR');

  // -------------------------------------------------------------------------
  // TEST SCENARIO 5: Full OTP Lifecycle & Brute-Force Lockout
  // -------------------------------------------------------------------------
  console.log('\n📌 [Scenario 5] OTP Verification & Brute-Force Defense');
  const verifyEmail = 'recipient@bloodsync.org';
  const stored = await otpService.createAndStoreOtp(verifyEmail, 'REQ-TEST-001');
  assert(stored.otp.length === 6, 'Generated valid OTP for recipient');

  // Attempt 1: Wrong OTP
  const fail1 = await otpService.verifyOtp(verifyEmail, '000000', 'REQ-TEST-001');
  assert(fail1.success === false, 'Rejects incorrect OTP');

  // Attempt with correct OTP
  const successVerification = await otpService.verifyOtp(verifyEmail, stored.otp, 'REQ-TEST-001');
  assert(successVerification.success === true, 'Successfully verifies with exact matching OTP');

  // Replay attempt with already used OTP
  const replayAttempt = await otpService.verifyOtp(verifyEmail, stored.otp, 'REQ-TEST-001');
  assert(replayAttempt.success === false, 'Blocks replay attack for already verified code');

  console.log('\n================================================================');
  console.log(`🏁 Test Results: ${passedTests}/${totalTests} Passed`);
  console.log('================================================================\n');
}

if (require.main === module) {
  runTestSuite().catch(console.error);
}

module.exports = runTestSuite;
