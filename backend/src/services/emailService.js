/**
 * BloodSync Backend - Email Dispatch Service
 * Handles Nodemailer Gmail SMTP transmission with error mapping and zero credential exposure.
 */

const { createTransporter } = require('../config/mailer');
const { SMTP_CONFIG, SECURITY_CONFIG } = require('../config/environment');
const { ERROR_CATEGORIES, EMAIL_SEND_STATUS } = require('../config/constants');
const { generateOtpEmailTemplate } = require('../templates/otpEmailTemplate');
const { maskEmail } = require('../utils/sanitizer');

class EmailService {
  /**
   * Dispatches the OTP email to the recipient using Nodemailer Gmail SMTP.
   * @param {Object} params
   * @param {string} params.recipientEmail
   * @param {string} params.otp - Plaintext OTP (strictly used in email body, never logged)
   * @param {string} params.requestId
   * @returns {Promise<{ success: boolean, messageId?: string, errorCategory?: string, errorMessage?: string }>}
   */
  async sendOtpEmail({ recipientEmail, otp, requestId }) {
    const maskedEmail = maskEmail(recipientEmail);

    // 1. Verify email configuration
    if (!SMTP_CONFIG.user || !SMTP_CONFIG.password) {
      console.error(`[EmailService][${requestId}] Missing SMTP credentials for dispatch.`);
      return {
        success: false,
        status: EMAIL_SEND_STATUS.FAILED,
        errorCategory: ERROR_CATEGORIES.EMAIL_CONFIGURATION_ERROR,
        errorMessage: 'Gmail SMTP credentials (SMTP_USER / SMTP_PASSWORD) not configured.',
      };
    }

    // 2. Generate template
    const { html, text } = generateOtpEmailTemplate({
      otp,
      expiryMinutes: SECURITY_CONFIG.otpExpiryMinutes,
      recipientEmail,
    });

    const transporter = createTransporter();

    const mailOptions = {
      from: `"${SMTP_CONFIG.fromName}" <${SMTP_CONFIG.fromAddress}>`,
      to: recipientEmail,
      subject: '🩸 BloodSync Verification Code',
      text,
      html,
      headers: {
        'X-BloodSync-Request-ID': requestId,
        'X-Priority': '1', // High priority for OTP
      },
    };

    try {
      console.log(`[EmailService][${requestId}] Dispatching OTP email to ${maskedEmail}...`);
      const info = await transporter.sendMail(mailOptions);
      console.log(`[EmailService][${requestId}] Email dispatched successfully. MessageID: ${info.messageId}`);

      return {
        success: true,
        status: EMAIL_SEND_STATUS.SENT,
        messageId: info.messageId,
      };
    } catch (error) {
      const classified = this.classifySmtpError(error);
      console.error(
        `[EmailService][${requestId}] Failed sending to ${maskedEmail}. Category: ${classified.category}. Error: ${classified.message}`
      );

      return {
        success: false,
        status: EMAIL_SEND_STATUS.FAILED,
        errorCategory: classified.category,
        errorMessage: classified.message,
      };
    }
  }

  /**
   * Classifies low-level SMTP errors into strict system error categories.
   * @param {Error} error
   * @returns {{ category: string, message: string }}
   */
  classifySmtpError(error) {
    const errMessage = (error.message || '').toLowerCase();
    const errCode = error.code || '';
    const responseCode = error.responseCode || 0;

    // Authentication failure (Bad credentials / Invalid App Password)
    if (
      errCode === 'EAUTH' ||
      responseCode === 535 ||
      errMessage.includes('badcredentials') ||
      errMessage.includes('username and password not accepted') ||
      errMessage.includes('invalid credentials')
    ) {
      return {
        category: ERROR_CATEGORIES.SMTP_AUTH_ERROR,
        message: 'Gmail SMTP Authentication Failed. Check SMTP_USER and Gmail 16-character App Password.',
      };
    }

    // Connection or DNS configuration issues
    if (
      errCode === 'ECONNREFUSED' ||
      errCode === 'ENOTFOUND' ||
      errCode === 'ETIMEDOUT' ||
      errMessage.includes('getaddrinfo')
    ) {
      return {
        category: ERROR_CATEGORIES.EMAIL_CONFIGURATION_ERROR,
        message: `Network or host configuration error: ${error.code || error.message}`,
      };
    }

    // General send failure
    return {
      category: ERROR_CATEGORIES.EMAIL_SEND_ERROR,
      message: `Failed to deliver email: ${error.message}`,
    };
  }
}

module.exports = new EmailService();
