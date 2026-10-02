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
const errorHandler = require('./middleware/errorHandlerMiddleware');

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
  console.log(`=======================================================`);

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
  }
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
