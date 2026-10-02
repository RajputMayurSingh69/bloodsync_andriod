/**
 * BloodSync Backend - Authentication & OTP Controller
 * 
 * Implements:
 * - POST /auth/request-otp
 * - POST /auth/verify-otp
 * 
 * Strictly follows the 6-step architecture, safe error propagation, and zero credential leakage.
 */

const { v4: uuidv4 } = require('uuid');
const jwt = require('jsonwebtoken');
const { auth } = require('../config/firebase');
const { SECURITY_CONFIG } = require('../config/environment');
const {
  ERROR_CATEGORIES,
  REQUEST_STATUS,
  OTP_GEN_STATUS,
  EMAIL_SEND_STATUS,
} = require('../config/constants');
const { isValidEmail, isValidOtpFormat } = require('../utils/validator');
const { maskEmail, sanitizeIp } = require('../utils/sanitizer');
const otpService = require('../services/otpService');
const emailService = require('../services/emailService');
const rateLimiterService = require('../services/rateLimiterService');
const loggerBotService = require('../services/loggerBotService');

class AuthController {
  /**
   * Handles POST /auth/request-otp
   * 
   * Step 1: Validate incoming email format.
   * Step 2: Check rate limits.
   * Step 3: Generate and store OTP record.
   * Step 4: Attempt Gmail SMTP delivery.
   * Step 5: On Success -> Return generic success response; log success event to Firebase monitoring.
   * Step 6: On Failure -> Return safe generic error to user; log exact technical error to Firebase debugger.
   */
  async requestOtp(req, res) {
    const startTime = Date.now();
    const requestId = req.headers['x-request-id'] || uuidv4();
    const { email } = req.body || {};
    const clientIp = sanitizeIp(req.ip || req.connection.remoteAddress);
    const userAgent = req.headers['user-agent'] || 'unknown';

    let currentOtpGenStatus = OTP_GEN_STATUS.SKIPPED;
    let currentEmailSendStatus = EMAIL_SEND_STATUS.PENDING;

    // STEP 1: Validate incoming email format
    if (!email || !isValidEmail(email)) {
      await loggerBotService.logOtpEvent({
        requestId,
        rawEmail: email || 'unknown',
        requestStatus: REQUEST_STATUS.FAILURE,
        otpGenStatus: OTP_GEN_STATUS.SKIPPED,
        emailSendingStatus: EMAIL_SEND_STATUS.PENDING,
        errorCategory: ERROR_CATEGORIES.UNKNOWN_ERROR,
        errorMessage: 'Invalid or missing email address provided in request payload.',
        metadata: { clientIp, userAgent, durationMs: Date.now() - startTime },
      });

      return res.status(400).json({
        success: false,
        message: 'A valid email address is required.',
      });
    }

    const normalizedEmail = email.trim().toLowerCase();

    // STEP 2: Check rate limits
    const rateLimitCheck = await rateLimiterService.checkRateLimit(normalizedEmail);
    if (!rateLimitCheck.allowed) {
      await loggerBotService.logOtpEvent({
        requestId,
        rawEmail: normalizedEmail,
        requestStatus: REQUEST_STATUS.FAILURE,
        otpGenStatus: OTP_GEN_STATUS.SKIPPED,
        emailSendingStatus: EMAIL_SEND_STATUS.PENDING,
        errorCategory: ERROR_CATEGORIES.RATE_LIMIT_ERROR,
        errorMessage: rateLimitCheck.reason,
        metadata: {
          clientIp,
          userAgent,
          retryAfterSeconds: rateLimitCheck.retryAfterSeconds,
          durationMs: Date.now() - startTime,
        },
      });

      return res.status(429).json({
        success: false,
        message: rateLimitCheck.reason,
        retryAfterSeconds: rateLimitCheck.retryAfterSeconds,
      });
    }

    // STEP 3: Generate and store OTP record
    let otpData;
    try {
      otpData = await otpService.createAndStoreOtp(normalizedEmail, requestId);
      currentOtpGenStatus = OTP_GEN_STATUS.GENERATED;
    } catch (genError) {
      currentOtpGenStatus = OTP_GEN_STATUS.FAILED;

      await loggerBotService.logOtpEvent({
        requestId,
        rawEmail: normalizedEmail,
        requestStatus: REQUEST_STATUS.FAILURE,
        otpGenStatus: currentOtpGenStatus,
        emailSendingStatus: EMAIL_SEND_STATUS.PENDING,
        errorCategory: genError.category || ERROR_CATEGORIES.OTP_GENERATION_ERROR,
        errorMessage: genError.message,
        metadata: { clientIp, userAgent, durationMs: Date.now() - startTime },
      });

      // Step 6: Safe generic error to user
      return res.status(500).json({
        success: false,
        message: 'Unable to process OTP request at this time. Please try again later.',
      });
    }

    // Record request into rate limiter after successful generation
    await rateLimiterService.recordRequest(normalizedEmail);

    // STEP 4: Attempt Gmail SMTP delivery
    const emailResult = await emailService.sendOtpEmail({
      recipientEmail: normalizedEmail,
      otp: otpData.otp, // Plaintext OTP is sent solely to recipient via SMTP
      requestId,
    });

    currentEmailSendStatus = emailResult.status;

    // STEP 5: On Success -> Return generic success response; log success event to Firebase monitoring
    if (emailResult.success) {
      await loggerBotService.logOtpEvent({
        requestId,
        rawEmail: normalizedEmail,
        requestStatus: REQUEST_STATUS.SUCCESS,
        otpGenStatus: currentOtpGenStatus,
        emailSendingStatus: currentEmailSendStatus,
        errorCategory: null,
        errorMessage: null,
        metadata: {
          clientIp,
          userAgent,
          messageId: emailResult.messageId,
          durationMs: Date.now() - startTime,
        },
      });

      return res.status(200).json({
        success: true,
        message: 'Verification code sent to your email address.',
        data: {
          maskedEmail: maskEmail(normalizedEmail),
          expiresInMinutes: SECURITY_CONFIG.otpExpiryMinutes,
        },
      });
    }

    // STEP 6: On Failure -> Safe generic error to user; log exact technical error to Firebase debugger bot
    await loggerBotService.logOtpEvent({
      requestId,
      rawEmail: normalizedEmail,
      requestStatus: REQUEST_STATUS.FAILURE,
      otpGenStatus: currentOtpGenStatus,
      emailSendingStatus: currentEmailSendStatus,
      errorCategory: emailResult.errorCategory || ERROR_CATEGORIES.EMAIL_SEND_ERROR,
      errorMessage: emailResult.errorMessage,
      metadata: { clientIp, userAgent, durationMs: Date.now() - startTime },
    });

    return res.status(500).json({
      success: false,
      message: 'Failed to deliver verification code. Please check your email and try again later.',
    });
  }

  /**
   * Handles POST /auth/verify-otp
   * Validates user OTP against stored HMAC hash, enforces brute-force limits and expiry,
   * and issues JWT & Firebase Custom Token for seamless authentication.
   */
  async verifyOtp(req, res) {
    const startTime = Date.now();
    const requestId = req.headers['x-request-id'] || uuidv4();
    const { email, otp } = req.body || {};
    const clientIp = sanitizeIp(req.ip || req.connection.remoteAddress);
    const userAgent = req.headers['user-agent'] || 'unknown';

    if (!email || !isValidEmail(email)) {
      return res.status(400).json({
        success: false,
        message: 'A valid email address is required.',
      });
    }

    if (!otp || !isValidOtpFormat(otp)) {
      return res.status(400).json({
        success: false,
        message: 'A 6-digit numeric verification code is required.',
      });
    }

    const normalizedEmail = email.trim().toLowerCase();

    // Verify OTP against Firestore HMAC
    const verification = await otpService.verifyOtp(normalizedEmail, otp, requestId);

    if (!verification.success) {
      await loggerBotService.logOtpEvent({
        requestId,
        rawEmail: normalizedEmail,
        requestStatus: REQUEST_STATUS.FAILURE,
        otpGenStatus: OTP_GEN_STATUS.SKIPPED,
        emailSendingStatus: EMAIL_SEND_STATUS.PENDING,
        errorCategory: verification.errorCategory || ERROR_CATEGORIES.UNKNOWN_ERROR,
        errorMessage: `OTP verification failed: ${verification.reason}`,
        metadata: { clientIp, userAgent, durationMs: Date.now() - startTime },
      });

      const statusCode = verification.errorCategory === ERROR_CATEGORIES.RATE_LIMIT_ERROR ? 429 : 400;
      return res.status(statusCode).json({
        success: false,
        message: verification.reason,
      });
    }

    // OTP is valid! Generate session tokens
    let firebaseCustomToken = null;
    try {
      if (auth) {
        // Create or locate Firebase Auth User Record
        let userRecord;
        try {
          userRecord = await auth.getUserByEmail(normalizedEmail);
        } catch (findErr) {
          if (findErr.code === 'auth/user-not-found') {
            userRecord = await auth.createUser({
              email: normalizedEmail,
              emailVerified: true,
            });
          }
        }

        if (userRecord) {
          firebaseCustomToken = await auth.createCustomToken(userRecord.uid, {
            email: normalizedEmail,
            verifiedViaOtp: true,
          });
        }
      }
    } catch (authError) {
      console.warn(`[AuthController] Could not issue Firebase custom token: ${authError.message}`);
    }

    // Generate JWT token for backend API session handling
    const jwtToken = jwt.sign(
      {
        email: normalizedEmail,
        type: 'otp_verified_session',
      },
      SECURITY_CONFIG.jwtSecret,
      { expiresIn: SECURITY_CONFIG.jwtExpiration }
    );

    await loggerBotService.logOtpEvent({
      requestId,
      rawEmail: normalizedEmail,
      requestStatus: REQUEST_STATUS.SUCCESS,
      otpGenStatus: OTP_GEN_STATUS.SKIPPED,
      emailSendingStatus: EMAIL_SEND_STATUS.PENDING,
      errorCategory: null,
      errorMessage: null,
      metadata: {
        action: 'OTP_VERIFIED',
        clientIp,
        userAgent,
        durationMs: Date.now() - startTime,
      },
    });

    return res.status(200).json({
      success: true,
      message: 'Email verified successfully.',
      data: {
        token: jwtToken,
        firebaseCustomToken, // Can be used directly with FirebaseAuth.getInstance().signInWithCustomToken()
        user: {
          email: normalizedEmail,
          maskedEmail: maskEmail(normalizedEmail),
        },
      },
    });
  }
}

module.exports = new AuthController();
