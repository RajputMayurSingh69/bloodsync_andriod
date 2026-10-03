/**
 * BloodSync Backend - Rate Limit & Log Auto-Cleanup Service
 * 
 * Purges expired rate-limit records (`otp_rate_limits`), expired OTP verifications,
 * and aged debugger logs from Cloud Firestore to keep storage optimized and GDPR/HIPAA compliant.
 * 
 * Operates on a weekly automated schedule and supports on-demand admin execution.
 */

const { admin, db } = require('../config/firebase');
const { FIRESTORE_COLLECTIONS } = require('../config/constants');

const MS_PER_DAY = 24 * 60 * 60 * 1000;
const RATE_LIMIT_RETENTION_MS = 24 * 60 * 60 * 1000; // 24 hours
const LOG_RETENTION_DAYS = 14; // 14 days
const BATCH_SIZE = 400; // Under Firestore limit of 500 operations per commit

class CleanupCronService {
  constructor() {
    this.intervalId = null;
    this.isRunning = false;
  }

  /**
   * Purges expired documents from the `otp_rate_limits` collection.
   * Any rate-limit entry where `lastRequestAt` is older than 24 hours has expired.
   */
  async purgeExpiredRateLimits() {
    if (!db) return { deleted: 0, scanned: 0 };

    const cutoffMillis = Date.now() - RATE_LIMIT_RETENTION_MS;
    const cutoffTimestamp = admin.firestore.Timestamp.fromMillis(cutoffMillis);
    let deletedCount = 0;
    let scannedCount = 0;

    try {
      // Find expired rate limits by lastRequestAt
      const snapshot = await db
        .collection(FIRESTORE_COLLECTIONS.OTP_RATE_LIMITS)
        .where('lastRequestAt', '<', cutoffTimestamp)
        .limit(BATCH_SIZE)
        .get();

      scannedCount = snapshot.size;

      if (!snapshot.empty) {
        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
          batch.delete(doc.ref);
          deletedCount++;
        });
        await batch.commit();
      }

      return { deleted: deletedCount, scanned: scannedCount };
    } catch (err) {
      console.error(`[CleanupService] Error purging otp_rate_limits: ${err.message}`);
      return { deleted: deletedCount, scanned: scannedCount, error: err.message };
    }
  }

  /**
   * Purges aged debugger logs from the `backend_debugger_logs` collection.
   * Default retention is 14 days.
   */
  async purgeOldDebuggerLogs(retentionDays = LOG_RETENTION_DAYS) {
    if (!db) return { deleted: 0, scanned: 0 };

    const cutoffMillis = Date.now() - retentionDays * MS_PER_DAY;
    const cutoffTimestamp = admin.firestore.Timestamp.fromMillis(cutoffMillis);
    let deletedCount = 0;
    let scannedCount = 0;

    try {
      const snapshot = await db
        .collection(FIRESTORE_COLLECTIONS.BACKEND_DEBUGGER_LOGS)
        .where('timestamp', '<', cutoffTimestamp)
        .limit(BATCH_SIZE)
        .get();

      scannedCount = snapshot.size;

      if (!snapshot.empty) {
        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
          batch.delete(doc.ref);
          deletedCount++;
        });
        await batch.commit();
      }

      return { deleted: deletedCount, scanned: scannedCount };
    } catch (err) {
      console.error(`[CleanupService] Error purging backend_debugger_logs: ${err.message}`);
      return { deleted: deletedCount, scanned: scannedCount, error: err.message };
    }
  }

  /**
   * Purges expired OTP records from `otp_verifications`.
   */
  async purgeExpiredOtps() {
    if (!db) return { deleted: 0, scanned: 0 };

    const nowTimestamp = admin.firestore.Timestamp.now();
    let deletedCount = 0;
    let scannedCount = 0;

    try {
      const snapshot = await db
        .collection(FIRESTORE_COLLECTIONS.OTP_VERIFICATIONS)
        .where('expiresAt', '<', nowTimestamp)
        .limit(BATCH_SIZE)
        .get();

      scannedCount = snapshot.size;

      if (!snapshot.empty) {
        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
          batch.delete(doc.ref);
          deletedCount++;
        });
        await batch.commit();
      }

      return { deleted: deletedCount, scanned: scannedCount };
    } catch (err) {
      console.error(`[CleanupService] Error purging otp_verifications: ${err.message}`);
      return { deleted: deletedCount, scanned: scannedCount, error: err.message };
    }
  }

  /**
   * Executes a complete cleanup cycle across all rate limits, expired OTPs, and logs.
   */
  async runFullCleanup() {
    const startTime = Date.now();
    console.log(`[CleanupService] Starting scheduled database purge at ${new Date(startTime).toISOString()}...`);

    const rateLimitsResult = await this.purgeExpiredRateLimits();
    const debuggerLogsResult = await this.purgeOldDebuggerLogs(LOG_RETENTION_DAYS);
    const expiredOtpsResult = await this.purgeExpiredOtps();

    const durationMs = Date.now() - startTime;
    const totalDeleted =
      (rateLimitsResult.deleted || 0) +
      (debuggerLogsResult.deleted || 0) +
      (expiredOtpsResult.deleted || 0);

    const summary = {
      success: true,
      timestamp: new Date().toISOString(),
      durationMs,
      totalDocumentsCleaned: totalDeleted,
      rateLimitsPurged: rateLimitsResult.deleted || 0,
      debuggerLogsPurged: debuggerLogsResult.deleted || 0,
      expiredOtpsPurged: expiredOtpsResult.deleted || 0,
    };

    console.log(
      `[CleanupService] Purge completed in ${durationMs}ms: ` +
      `${summary.rateLimitsPurged} rate-limits, ${summary.debuggerLogsPurged} logs, ${summary.expiredOtpsPurged} OTPs purged.`
    );

    // Record cleanup summary log in Firestore
    try {
      if (db) {
        await db.collection(FIRESTORE_COLLECTIONS.BACKEND_DEBUGGER_LOGS).add({
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          requestId: `cleanup_${Date.now()}`,
          requestStatus: 'SUCCESS',
          serverEnvironment: process.env.NODE_ENV || 'development',
          action: 'FIRESTORE_TTL_AUTO_CLEANUP',
          metadata: summary,
        });
      }
    } catch (_logErr) {
      // Non-blocking
    }

    return summary;
  }

  /**
   * Starts the 7-day recurring cleanup timer.
   */
  startWeeklySchedule() {
    if (this.isRunning) return;
    this.isRunning = true;

    console.log('[CleanupService] Starting weekly Firestore rate-limit & log cleanup scheduler...');

    // Run initial cleanup 20 seconds after server startup
    setTimeout(() => {
      this.runFullCleanup().catch((err) =>
        console.warn(`[CleanupService] Initial cleanup notice: ${err.message}`)
      );
    }, 20000);

    // Recurring every 7 days (7 * 24 * 60 * 60 * 1000 ms)
    this.intervalId = setInterval(() => {
      this.runFullCleanup().catch((err) =>
        console.warn(`[CleanupService] Periodic cleanup notice: ${err.message}`)
      );
    }, 7 * MS_PER_DAY);
  }

  /**
   * Halts the recurring scheduler.
   */
  stopSchedule() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.isRunning = false;
  }
}

module.exports = new CleanupCronService();
