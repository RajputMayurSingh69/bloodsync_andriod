/**
 * BloodSync Backend - Server Entry Point
 * 
 * Secure Express application providing OTP authentication, Gmail SMTP integration,
 * and real-time Firebase Debugger Bot monitoring.
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { PORT, NODE_ENV, validateConfig } = require('./config/environment');
const { verifyTransporter } = require('./config/mailer');
const authRoutes = require('./routes/authRoutes');
const bloodBankRoutes = require('./routes/bloodBankRoutes');
const bankAuthRoutes = require('./routes/bankAuthRoutes');
const errorHandler = require('./middleware/errorHandlerMiddleware');
const emergencyDispatcherService = require('./services/emergencyDispatcherService');
const bloodBankService = require('./services/bloodBankService');
const cooldownCronService = require('./services/cooldownCronService');
const cleanupCronService = require('./services/cleanupCronService');
const webhookAlertService = require('./services/webhookAlertService');

const app = express();

// Security Middlewares
app.use(helmet());
app.use(cors({ origin: true, credentials: true }));
app.use(express.json({ limit: '100kb' }));
app.use(express.urlencoded({ extended: true, limit: '100kb' }));

// Request tracing middleware
app.use((req, res, next) => {
  if (!req.headers['x-request-id']) {
    const { v4: uuidv4 } = require('uuid');
    req.headers['x-request-id'] = uuidv4();
  }
  res.setHeader('X-Request-ID', req.headers['x-request-id']);
  next();
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'healthy',
    service: 'bloodsync-backend',
    environment: NODE_ENV,
    timestamp: new Date().toISOString(),
    uptimeSeconds: Math.floor(process.uptime()),
  });
});

// System Diagnostics & Debugger Status endpoint
app.get('/debugger/status', async (req, res) => {
  const smtpStatus = await verifyTransporter();
  res.status(200).json({
    service: 'BloodSync Debugger & Monitoring Bot',
    environment: NODE_ENV,
    smtp: {
      connected: smtpStatus.success,
      category: smtpStatus.category || null,
      message: smtpStatus.message,
    },
    timestamp: new Date().toISOString(),
  });
});

// Mount Routes
app.use('/auth', authRoutes);
app.use('/blood-banks', bloodBankRoutes);
app.use('/bank-auth', bankAuthRoutes);

// Manual trigger for WHO 90-Day Cooldown Scan
app.post('/admin/cron/cooldown-check', async (req, res) => {
  const result = await cooldownCronService.runCooldownScan();
  res.status(200).json({ success: true, ...result });
});

// Manual trigger for Firestore Expired Rate-Limits & Logs Cleanup
app.post('/admin/cron/cleanup-expired', async (req, res) => {
  const result = await cleanupCronService.runFullCleanup();
  res.status(200).json({ success: true, ...result });
});

// Test Webhook Alert Dispatch Endpoint (for developer verification)
app.post('/admin/test-alert', async (req, res) => {
  const result = await webhookAlertService.triggerAlert({
    severity: req.body.severity || 'WARNING',
    errorCategory: req.body.errorCategory || 'WEBHOOK_VERIFICATION_TEST',
    title: req.body.title || 'BloodSync Test Alert',
    message: req.body.message || 'Manual webhook alert verification from BloodSync backend.',
    requestId: 'test_' + Date.now(),
    maskedUserEmail: 'admin@bloodsync.org',
    metadata: { source: 'Admin API /admin/test-alert' },
  });
  res.status(result.success ? 200 : 400).json(result);
});

// Emergency SOS Dispatch Endpoint (manual trigger / webhook)
app.post('/emergency/dispatch', async (req, res) => {
  const result = await emergencyDispatcherService.onEmergencyCreated(req.body);
  res.status(result.success ? 200 : 500).json(result);
});

// 404 Handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Resource not found: ${req.method} ${req.originalUrl}`,
  });
});

// Global Error Handler
app.use(errorHandler);

// Start Server
const server = app.listen(PORT, async () => {
  console.log(`=======================================================`);
  console.log(`🩸 BloodSync Backend running on port ${PORT} [${NODE_ENV}]`);
  console.log(`📡 Health Check: http://localhost:${PORT}/health`);
  console.log(`🛠️ Debugger Status: http://localhost:${PORT}/debugger/status`);
  console.log(`👤 User Auth (OTP): http://localhost:${PORT}/auth`);
  console.log(`🏥 Blood Bank Auth: http://localhost:${PORT}/bank-auth`);
  console.log(`🏦 Blood Banks: http://localhost:${PORT}/blood-banks`);
  console.log(`🚨 Emergency Dispatcher: http://localhost:${PORT}/emergency/dispatch`);
  console.log(`🧹 Database Purge: http://localhost:${PORT}/admin/cron/cleanup-expired`);
  console.log(`=======================================================`);

  // Start real-time Firestore Emergency SOS Listener
  emergencyDispatcherService.startListener();

  // Initialize default blood banks & start automated schedulers
  bloodBankService.initializeDefaults();
  cooldownCronService.startDailySchedule();
  cleanupCronService.startWeeklySchedule();

  // Validate configuration warnings
  const { warnings } = validateConfig();
  if (warnings.length > 0) {
    warnings.forEach((warn) => console.warn(`⚠️  [CONFIG WARNING] ${warn}`));
  }

  // Pre-flight SMTP check
  const smtpCheck = await verifyTransporter();
  if (smtpCheck.success) {
    console.log(`✅ [SMTP] ${smtpCheck.message}`);
  } else {
    console.warn(`⚠️ [SMTP] Pre-flight verification failed: [${smtpCheck.category}] ${smtpCheck.message}`);
    // Trigger instant webhook alert if configured
    webhookAlertService.triggerAlert({
      severity: 'CRITICAL',
      errorCategory: smtpCheck.category,
      title: 'Gmail SMTP Pre-flight Verification Failed',
      message: `Failed to connect to Gmail SMTP: ${smtpCheck.message}. OTP delivery will fail until credentials are corrected.`,
      requestId: 'boot_smtp_preflight',
    });
  }
});

// Uncaught exception and unhandled rejection listeners for fail-safe alerting
process.on('uncaughtException', (err) => {
  console.error('[Process] Uncaught Exception:', err);
  webhookAlertService.triggerAlert({
    severity: 'CRITICAL',
    errorCategory: 'UNCAUGHT_EXCEPTION',
    title: 'Node.js Fatal Uncaught Exception',
    message: err.message || 'Fatal uncaught exception occurred in host process.',
    metadata: { stack: err.stack },
  });
});

process.on('unhandledRejection', (reason) => {
  console.error('[Process] Unhandled Rejection:', reason);
  webhookAlertService.triggerAlert({
    severity: 'CRITICAL',
    errorCategory: 'UNHANDLED_REJECTION',
    title: 'Node.js Unhandled Promise Rejection',
    message: String(reason && reason.message ? reason.message : reason),
  });
});

// Graceful shutdown handling
function handleShutdown(signal) {
  console.log(`\n[Server] Received ${signal}. Starting graceful shutdown...`);
  server.close(() => {
    console.log('[Server] HTTP server closed cleanly. Exiting process.');
    process.exit(0);
  });

  // Force exit if shutdown takes too long
  setTimeout(() => {
    console.error('[Server] Forcing shutdown due to timeout.');
    process.exit(1);
  }, 10000);
}

process.on('SIGTERM', () => handleShutdown('SIGTERM'));
process.on('SIGINT', () => handleShutdown('SIGINT'));

module.exports = app;
