/**
 * BloodSync Backend - Environment Configuration Loader
 */

const dotenv = require('dotenv');
const path = require('path');

// Load .env file from backend root
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const NODE_ENV = process.env.NODE_ENV || 'development';
const PORT = parseInt(process.env.PORT, 10) || 5000;

const SMTP_CONFIG = {
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.SMTP_PORT, 10) || 465,
  secure: process.env.SMTP_SECURE === 'true' || parseInt(process.env.SMTP_PORT, 10) === 465,
  user: process.env.SMTP_USER || '',
  password: process.env.SMTP_PASSWORD || '',
  fromName: process.env.EMAIL_FROM_NAME || 'BloodSync Security',
  fromAddress: process.env.EMAIL_FROM_ADDRESS || process.env.SMTP_USER || 'no-reply@bloodsync.org',
};

const FIREBASE_CONFIG = {
  serviceAccountPath: process.env.FIREBASE_SERVICE_ACCOUNT_PATH || '',
  projectId: process.env.FIREBASE_PROJECT_ID || 'bloodsync-3b5cf',
  clientEmail: process.env.FIREBASE_CLIENT_EMAIL || '',
  privateKey: process.env.FIREBASE_PRIVATE_KEY
    ? process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
    : '',
};

const SECURITY_CONFIG = {
  otpExpiryMinutes: parseInt(process.env.OTP_EXPIRY_MINUTES, 10) || 5,
  otpMaxAttempts: parseInt(process.env.OTP_MAX_ATTEMPTS, 10) || 5,
  otpSaltSecret: process.env.OTP_SALT_SECRET || 'bloodsync_default_secure_salt_change_me_in_prod',
  jwtSecret: process.env.JWT_SECRET || 'bloodsync_super_secret_jwt_key_change_me_in_prod',
  jwtExpiration: process.env.JWT_EXPIRATION || '24h',
};

const RATE_LIMIT_CONFIG = {
  windowMinutes: parseInt(process.env.RATE_LIMIT_WINDOW_MINUTES, 10) || 10,
  maxRequestsPerWindow: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS_PER_WINDOW, 10) || 3,
  cooldownSeconds: parseInt(process.env.RATE_LIMIT_COOLDOWN_SECONDS, 10) || 60,
};

const MONITORING_CONFIG = {
  alertWebhookUrl: process.env.ALERT_WEBHOOK_URL || '',
  alertNotificationEmail: process.env.ALERT_NOTIFICATION_EMAIL || '',
  enableRealtimeAlerts: process.env.ENABLE_REALTIME_ALERTS === 'true',
};

function validateConfig() {
  const warnings = [];

  if (!SMTP_CONFIG.user || !SMTP_CONFIG.password) {
    warnings.push('SMTP_USER or SMTP_PASSWORD is not set. Nodemailer will fail to send real emails.');
  }

  if (SECURITY_CONFIG.otpSaltSecret.includes('change_me')) {
    warnings.push('OTP_SALT_SECRET is using default placeholder. Set a strong secret in production.');
  }

  if (SECURITY_CONFIG.jwtSecret.includes('change_me')) {
    warnings.push('JWT_SECRET is using default placeholder. Set a strong secret in production.');
  }

  return {
    isValid: true,
    warnings,
  };
}

module.exports = {
  NODE_ENV,
  PORT,
  SMTP_CONFIG,
  FIREBASE_CONFIG,
  SECURITY_CONFIG,
  RATE_LIMIT_CONFIG,
  MONITORING_CONFIG,
  validateConfig,
};
