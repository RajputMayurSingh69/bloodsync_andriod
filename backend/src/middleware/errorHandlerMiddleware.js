/**
 * BloodSync Backend - Global Error Handler Middleware
 * Sanitizes errors and logs technical details to LoggerBotService without leaking credentials.
 */

const { v4: uuidv4 } = require('uuid');
const loggerBotService = require('../services/loggerBotService');
const {
  ERROR_CATEGORIES,
  REQUEST_STATUS,
  OTP_GEN_STATUS,
  EMAIL_SEND_STATUS,
} = require('../config/constants');
const { sanitizeIp } = require('../utils/sanitizer');

function errorHandlerMiddleware(err, req, res, next) {
  const requestId = req.headers['x-request-id'] || uuidv4();
  const rawEmail = (req.body && req.body.email) || 'unknown';
  const clientIp = sanitizeIp(req.ip || req.connection.remoteAddress);
  const userAgent = req.headers['user-agent'] || 'unknown';

  const errorCategory = err.category || ERROR_CATEGORIES.UNKNOWN_ERROR;

  // Log error via LoggerBotService
  loggerBotService.logOtpEvent({
    requestId,
    rawEmail,
    requestStatus: REQUEST_STATUS.FAILURE,
    otpGenStatus: OTP_GEN_STATUS.SKIPPED,
    emailSendingStatus: EMAIL_SEND_STATUS.FAILED,
    errorCategory,
    errorMessage: err.message || 'Internal server error occurred.',
    metadata: {
      clientIp,
      userAgent,
      path: req.originalUrl,
      method: req.method,
      stack: process.env.NODE_ENV === 'development' ? err.stack : undefined,
    },
  }).catch((logErr) => {
    console.error(`[ErrorHandler] Error logger failed: ${logErr.message}`);
  });

  const statusCode = err.status || err.statusCode || 500;

  // Never return internal error stacks to client in production
  return res.status(statusCode).json({
    success: false,
    message: err.userMessage || 'An unexpected error occurred. Please try again later.',
    requestId,
  });
}

module.exports = errorHandlerMiddleware;
