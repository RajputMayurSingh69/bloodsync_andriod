/**
 * BloodSync Backend - Firebase Debugger Bot & Real-Time Monitoring Service
 * 
 * Records system events into `backend_debugger_logs` and triggers alerts
 * for critical failures without exposing PII or credentials.
 */

const { admin, db } = require('../config/firebase');
const {
  ERROR_CATEGORIES,
  REQUEST_STATUS,
  FIRESTORE_COLLECTIONS,
} = require('../config/constants');
const { NODE_ENV, MONITORING_CONFIG } = require('../config/environment');
const { maskEmail, sanitizeDetails } = require('../utils/sanitizer');
const webhookAlertService = require('./webhookAlertService');

class LoggerBotService {
  /**
   * Records an OTP lifecycle event to the `backend_debugger_logs` collection.
   * @param {Object} params
   * @param {string} params.requestId
   * @param {string} params.rawEmail
   * @param {string} params.requestStatus - SUCCESS | FAILURE
   * @param {string} params.otpGenStatus - GENERATED | FAILED | SKIPPED
   * @param {string} params.emailSendingStatus - SENT | FAILED | PENDING
   * @param {string|null} [params.errorCategory] - One of ERROR_CATEGORIES or null
   * @param {string} [params.errorMessage] - Sanitized technical message
   * @param {Object} [params.metadata] - Additional contextual data (duration, ip, userAgent)
   */
  async logOtpEvent({
    requestId,
    rawEmail,
    requestStatus,
    otpGenStatus,
    emailSendingStatus,
    errorCategory = null,
    errorMessage = null,
    metadata = {},
  }) {
    const maskedUserEmail = maskEmail(rawEmail);
    const sanitizedMetadata = sanitizeDetails(metadata);
    const sanitizedErrorMessage = errorMessage ? sanitizeDetails(errorMessage) : null;

    const logEntry = {
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      requestId,
      maskedUserEmail,
      requestStatus,
      otpGenStatus,
      emailSendingStatus,
      errorCategory,
      errorMessage: sanitizedErrorMessage,
      serverEnvironment: NODE_ENV,
      expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 14 * 24 * 60 * 60 * 1000), // 14-day TTL
      metadata: sanitizedMetadata,
    };

    // 1. Console stream for local/container visibility
    const level = requestStatus === REQUEST_STATUS.SUCCESS ? 'INFO' : 'ERROR';
    console.log(
      `[DebuggerBot][${level}] [${requestId}] status=${requestStatus} email=${maskedUserEmail} otpGen=${otpGenStatus} emailSend=${emailSendingStatus} errCat=${errorCategory || 'none'}`
    );

    // 2. Persist to Firestore collection `backend_debugger_logs`
    try {
      if (db) {
        await db.collection(FIRESTORE_COLLECTIONS.BACKEND_DEBUGGER_LOGS).add(logEntry);
      } else {
        console.warn('[DebuggerBot] Firestore db not available. Event logged to stdout only.');
      }
    } catch (firestoreError) {
      console.error(
        `[DebuggerBot] CRITICAL: Failed to write debugger log to Firestore: ${firestoreError.message}`
      );
    }

    // 3. Evaluate Critical Alert Triggers
    if (requestStatus === REQUEST_STATUS.FAILURE && errorCategory) {
      await this.evaluateAlertConditions({
        requestId,
        maskedUserEmail,
        errorCategory,
        errorMessage: sanitizedErrorMessage,
        sanitizedMetadata,
      });
    }

    return logEntry;
  }

  /**
   * Evaluates if the error constitutes a critical event and triggers immediate alerts.
   * Critical events include:
   * - SMTP failures (SMTP_AUTH_ERROR, EMAIL_SEND_ERROR, EMAIL_CONFIGURATION_ERROR)
   * - Firebase database failures (FIREBASE_ERROR, OTP_DATABASE_ERROR)
   * - High severity rate limit abuse (RATE_LIMIT_ERROR)
   */
  async evaluateAlertConditions({
    requestId,
    maskedUserEmail,
    errorCategory,
    errorMessage,
    sanitizedMetadata,
  }) {
    const criticalCategories = [
      ERROR_CATEGORIES.SMTP_AUTH_ERROR,
      ERROR_CATEGORIES.EMAIL_SEND_ERROR,
      ERROR_CATEGORIES.EMAIL_CONFIGURATION_ERROR,
      ERROR_CATEGORIES.FIREBASE_ERROR,
      ERROR_CATEGORIES.OTP_DATABASE_ERROR,
    ];

    const isCritical = criticalCategories.includes(errorCategory);
    const isRateLimit = errorCategory === ERROR_CATEGORIES.RATE_LIMIT_ERROR;

    if (!isCritical && !isRateLimit && errorCategory !== ERROR_CATEGORIES.UNKNOWN_ERROR) {
      return;
    }

    const severity = isCritical ? 'CRITICAL' : isRateLimit ? 'WARNING' : 'ERROR';

    const alertPayload = {
      alertId: `ALERT-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      timestamp: new Date().toISOString(),
      serverEnvironment: NODE_ENV,
      severity,
      errorCategory,
      requestId,
      maskedUserEmail,
      summary: `Automated Debugger Alert: [${errorCategory}] triggered on ${NODE_ENV}`,
      details: errorMessage || 'No additional technical message provided',
      metadata: sanitizedMetadata,
    };

    console.error(`🚨 [CRITICAL_ALERT_BOT] ${alertPayload.severity}: ${alertPayload.summary}`);

    // Persist to critical_alerts collection in Firestore
    try {
      if (db) {
        await db.collection(FIRESTORE_COLLECTIONS.CRITICAL_ALERTS).add({
          ...alertPayload,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30-day TTL
          resolved: false,
        });
      }
    } catch (dbErr) {
      console.error(`[CRITICAL_ALERT_BOT] Failed to save alert to Firestore: ${dbErr.message}`);
    }

    // Dispatch webhook alert to Discord/Slack/Telegram/Custom via WebhookAlertService
    await webhookAlertService.triggerAlert({
      severity: alertPayload.severity,
      errorCategory: alertPayload.errorCategory,
      title: alertPayload.summary,
      message: alertPayload.details,
      requestId: alertPayload.requestId,
      maskedUserEmail: alertPayload.maskedUserEmail,
      metadata: alertPayload.metadata,
    });
  }
}

module.exports = new LoggerBotService();
