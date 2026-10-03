/**
 * BloodSync Backend - WHO 90-Day Donor Cooldown Automated Cron Service
 * 
 * Implements the World Health Organization (WHO) medical safety rule:
 * Voluntary blood donors must observe a strict 90-day (3-month) recovery period
 * between whole blood donations.
 * 
 * Automatically enforces "COOLDOWN" status and restores donors to "AVAILABLE"
 * once 90 days have elapsed.
 */

const { admin, db } = require('../config/firebase');
const loggerBotService = require('./loggerBotService');
const { REQUEST_STATUS } = require('../config/constants');

const COOLDOWN_DAYS = 90;
const MS_PER_DAY = 24 * 60 * 60 * 1000;

class CooldownCronService {
  constructor() {
    this.intervalId = null;
    this.isRunning = false;
  }

  /**
   * Executes a full scan of donor profiles in Firestore to enforce the 90-day cooldown.
   * @returns {Promise<{ scanned: number, restoredToAvailable: number, keptInCooldown: number, errors: number }>}
   */
  async runCooldownScan() {
    if (!db) {
      console.warn('[CooldownCron] Firestore DB not available. Skipping cooldown scan.');
      return { scanned: 0, restoredToAvailable: 0, keptInCooldown: 0, errors: 0 };
    }

    const now = Date.now();
    console.log(`[CooldownCron] Starting WHO 90-Day donor cooldown audit at ${new Date(now).toISOString()}...`);

    let scanned = 0;
    let restoredToAvailable = 0;
    let keptInCooldown = 0;
    let errors = 0;

    try {
      // 1. Scan users collection
      const usersSnap = await db.collection('users').get();
      scanned = usersSnap.size;

      for (const doc of usersSnap.docs) {
        try {
          const user = doc.data();
          const userId = doc.id;

          // Resolve last donation time from multiple possible fields
          let donationTime = null;

          if (user.lastDonationTimestamp) {
            donationTime = typeof user.lastDonationTimestamp === 'number'
              ? user.lastDonationTimestamp
              : user.lastDonationTimestamp.toMillis ? user.lastDonationTimestamp.toMillis() : null;
          } else if (user.lastDonationDateString || user.lastDonationDate) {
            const parsed = new Date(user.lastDonationDateString || user.lastDonationDate).getTime();
            if (!isNaN(parsed)) donationTime = parsed;
          }

          // If no recorded donation, skip
          if (!donationTime) continue;

          const daysElapsed = (now - donationTime) / MS_PER_DAY;

          if (daysElapsed >= COOLDOWN_DAYS) {
            // 90 days have passed: donor is safe to donate again!
            if (user.isAvailableDonor === false || user.donorStatus === 'COOLDOWN') {
              console.log(`[CooldownCron] Donor ${userId} (${user.name || 'Donor'}) completed ${Math.floor(daysElapsed)} days cooldown. Restoring to AVAILABLE.`);

              const updatePayload = {
                isAvailableDonor: true,
                donorStatus: 'AVAILABLE',
                cooldownDaysRemaining: 0,
                cooldownLiftedAt: admin.firestore.FieldValue.serverTimestamp(),
              };

              await db.collection('users').doc(userId).set(updatePayload, { merge: true });
              await db.collection('donors').doc(userId).set(updatePayload, { merge: true });
              restoredToAvailable++;
            }
          } else {
            // Still in 90-day cooldown window
            const daysRemaining = Math.ceil(COOLDOWN_DAYS - daysElapsed);
            keptInCooldown++;

            if (user.isAvailableDonor !== false || user.donorStatus !== 'COOLDOWN') {
              console.log(`[CooldownCron] Enforcing WHO cooldown for ${userId} (${user.name || 'Donor'}). ${daysRemaining} days remaining.`);

              const updatePayload = {
                isAvailableDonor: false,
                donorStatus: 'COOLDOWN',
                cooldownDaysRemaining: daysRemaining,
                cooldownEnforcedAt: admin.firestore.FieldValue.serverTimestamp(),
              };

              await db.collection('users').doc(userId).set(updatePayload, { merge: true });
              await db.collection('donors').doc(userId).set(updatePayload, { merge: true });
            }
          }
        } catch (itemErr) {
          console.error(`[CooldownCron] Error processing user ${doc.id}:`, itemErr.message);
          errors++;
        }
      }

      console.log(`[CooldownCron] Audit complete. Scanned: ${scanned}, Restored: ${restoredToAvailable}, Cooldown Active: ${keptInCooldown}, Errors: ${errors}`);

      // Log in LoggerBot
      await loggerBotService.logOtpEvent({
        requestId: `cooldown_cron_${Date.now()}`,
        rawEmail: 'scheduler@bloodsync.org',
        requestStatus: REQUEST_STATUS.SUCCESS,
        otpGenStatus: 'SKIPPED',
        emailSendingStatus: 'SKIPPED',
        errorCategory: null,
        errorMessage: null,
        metadata: {
          action: 'WHO_90_DAY_COOLDOWN_SCAN',
          scanned,
          restoredToAvailable,
          keptInCooldown,
          errors,
        },
      });

      return { scanned, restoredToAvailable, keptInCooldown, errors };
    } catch (err) {
      console.error('[CooldownCron] Fatal error during cooldown scan:', err.message);
      return { scanned, restoredToAvailable, keptInCooldown, errors: errors + 1, error: err.message };
    }
  }

  /**
   * Starts the 24-hour recurring timer and executes an initial scan.
   */
  startDailySchedule() {
    if (this.isRunning) return;
    this.isRunning = true;

    console.log('[CooldownCron] Starting daily WHO 90-Day cooldown scheduler...');

    // Run initial scan 10 seconds after server boot
    setTimeout(() => {
      this.runCooldownScan().catch((e) => console.warn('[CooldownCron] Initial scan notice:', e.message));
    }, 10000);

    // Schedule every 24 hours
    this.intervalId = setInterval(() => {
      this.runCooldownScan().catch((e) => console.warn('[CooldownCron] Periodic scan notice:', e.message));
    }, 24 * 60 * 60 * 1000);
  }

  stopSchedule() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.isRunning = false;
  }
}

module.exports = new CooldownCronService();
