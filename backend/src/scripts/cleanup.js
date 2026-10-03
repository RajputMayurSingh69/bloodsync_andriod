#!/usr/bin/env node
/**
 * BloodSync Backend - Standalone Database Purge CLI Script
 * 
 * Usage:
 *   node backend/src/scripts/cleanup.js
 */

const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../../.env') });

const cleanupCronService = require('../services/cleanupCronService');

async function main() {
  console.log('========================================================');
  console.log('🧹 BloodSync Database Purge & Retention Enforcement');
  console.log('========================================================');

  try {
    const result = await cleanupCronService.runFullCleanup();
    console.log('\n✅ Cleanup Results:');
    console.log(`   - Rate Limits Purged: ${result.rateLimitsPurged}`);
    console.log(`   - Aged Logs Purged:    ${result.debuggerLogsPurged}`);
    console.log(`   - Expired OTPs Purged: ${result.expiredOtpsPurged}`);
    console.log(`   - Total Deleted:       ${result.totalDocumentsCleaned}`);
    console.log(`   - Execution Time:      ${result.durationMs}ms\n`);
    process.exit(0);
  } catch (err) {
    console.error(`\n❌ Fatal Cleanup Error: ${err.message}`);
    process.exit(1);
  }
}

main();
