/**
 * BloodSync Backend - Nodemailer & Gmail SMTP Transporter Setup
 */

const nodemailer = require('nodemailer');
const { SMTP_CONFIG } = require('./environment');
const { ERROR_CATEGORIES } = require('./constants');

let transporterInstance = null;

function createTransporter() {
  if (transporterInstance) {
    return transporterInstance;
  }

  const isPort465 = Number(SMTP_CONFIG.port) === 465;

  const transportOptions = {
    host: SMTP_CONFIG.host,
    port: Number(SMTP_CONFIG.port),
    secure: SMTP_CONFIG.secure || isPort465, // true for 465, false for 587/STARTTLS
    auth: {
      user: SMTP_CONFIG.user,
      pass: SMTP_CONFIG.password,
    },
    pool: true,
    maxConnections: 5,
    maxMessages: 100,
    rateLimit: 10, // max 10 messages/second
    connectionTimeout: 15000, // 15 seconds
    socketTimeout: 30000,     // 30 seconds
    tls: {
      // Do not fail on invalid certs in test environments, but enforce in production
      rejectUnauthorized: process.env.NODE_ENV === 'production',
      minVersion: 'TLSv1.2',
    },
  };

  transporterInstance = nodemailer.createTransport(transportOptions);
  return transporterInstance;
}

/**
 * Verifies SMTP connection and classifies any configuration or authentication failure
 */
async function verifyTransporter() {
  const transporter = createTransporter();

  if (!SMTP_CONFIG.user || !SMTP_CONFIG.password) {
    return {
      success: false,
      category: ERROR_CATEGORIES.EMAIL_CONFIGURATION_ERROR,
      message: 'SMTP credentials missing. Please set SMTP_USER and SMTP_PASSWORD in .env',
    };
  }

  try {
    await transporter.verify();
    return {
      success: true,
      message: `SMTP Transporter verified successfully for ${SMTP_CONFIG.user}`,
    };
  } catch (error) {
    let category = ERROR_CATEGORIES.EMAIL_SEND_ERROR;

    if (
      error.code === 'EAUTH' ||
      error.responseCode === 535 ||
      (error.message && error.message.toLowerCase().includes('badcredentials')) ||
      (error.message && error.message.toLowerCase().includes('username and password not accepted'))
    ) {
      category = ERROR_CATEGORIES.SMTP_AUTH_ERROR;
    } else if (
      error.code === 'ECONNREFUSED' ||
      error.code === 'ENOTFOUND' ||
      error.code === 'ETIMEDOUT'
    ) {
      category = ERROR_CATEGORIES.EMAIL_CONFIGURATION_ERROR;
    }

    return {
      success: false,
      category,
      message: error.message,
      code: error.code,
      responseCode: error.responseCode,
    };
  }
}

module.exports = {
  createTransporter,
  verifyTransporter,
};
