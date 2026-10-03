/**
 * BloodSync Backend - Critical Error Webhook Alert Service
 * 
 * Provides unified, multi-platform webhook dispatching to Discord, Slack,
 * Telegram, and custom webhook endpoints when critical system events occur
 * (Gmail SMTP auth failure, Firebase credentials error, rate-limit breaches, etc.).
 */

const https = require('https');
const http = require('http');
const { URL } = require('url');
const { MONITORING_CONFIG, NODE_ENV } = require('../config/environment');

class WebhookAlertService {
  constructor() {
    // Cooldown map to prevent webhook flooding (category -> timestamp)
    this.recentAlerts = new Map();
    this.ALERT_THROTTLE_MS = 30000; // 30 seconds throttle per category
  }

  /**
   * Dispatches an alert to ALERT_WEBHOOK_URL.
   * 
   * @param {Object} alert
   * @param {string} alert.severity - 'CRITICAL' | 'WARNING' | 'INFO'
   * @param {string} alert.errorCategory - One of ERROR_CATEGORIES
   * @param {string} [alert.title] - Short human readable title
   * @param {string} alert.message - Main error explanation
   * @param {string} [alert.requestId] - Contextual request ID
   * @param {string} [alert.maskedUserEmail] - Masked user identifier
   * @param {Object} [alert.metadata] - Extra sanitized metadata
   * @returns {Promise<{ success: boolean, skipped?: boolean, reason?: string }>}
   */
  async triggerAlert(alert) {
    const webhookUrl = MONITORING_CONFIG.alertWebhookUrl || process.env.ALERT_WEBHOOK_URL;

    if (!webhookUrl || webhookUrl.trim() === '') {
      // Webhook not configured; silent bypass
      return { success: false, skipped: true, reason: 'ALERT_WEBHOOK_URL is not configured' };
    }

    const {
      severity = 'CRITICAL',
      errorCategory = 'SYSTEM_ERROR',
      title,
      message = 'An unexpected system error occurred.',
      requestId = 'sys_' + Math.random().toString(36).substring(2, 8),
      maskedUserEmail = 'system',
      metadata = {},
    } = alert;

    // Check throttle to prevent alert storm
    const now = Date.now();
    const throttleKey = `${errorCategory}:${severity}`;
    const lastSent = this.recentAlerts.get(throttleKey) || 0;
    if (now - lastSent < this.ALERT_THROTTLE_MS) {
      console.warn(`[WebhookAlert] Throttling alert for [${throttleKey}]. Last sent ${Math.round((now - lastSent)/1000)}s ago.`);
      return { success: false, skipped: true, reason: 'Throttled to avoid notification flood' };
    }
    this.recentAlerts.set(throttleKey, now);

    try {
      const parsedUrl = new URL(webhookUrl.trim());
      const payload = this.buildPayload(parsedUrl, {
        severity,
        errorCategory,
        title: title || `BloodSync ${severity}: ${errorCategory}`,
        message,
        requestId,
        maskedUserEmail,
        metadata,
        timestamp: new Date().toISOString(),
      });

      return await this.dispatchHttpRequest(parsedUrl, payload);
    } catch (err) {
      console.error(`[WebhookAlert] Failed to dispatch webhook alert: ${err.message}`);
      return { success: false, reason: err.message };
    }
  }

  /**
   * Formats the payload according to the destination platform.
   */
  buildPayload(parsedUrl, data) {
    const host = parsedUrl.hostname.toLowerCase();
    const isDiscord = host.includes('discord.com') || host.includes('discordapp.com');
    const isSlack = host.includes('slack.com');
    const isTelegram = host.includes('telegram.org');

    const colorHex = data.severity === 'CRITICAL' ? 0xE74C3C : data.severity === 'WARNING' ? 0xE67E22 : 0x3498DB;
    const colorSlack = data.severity === 'CRITICAL' ? '#E74C3C' : data.severity === 'WARNING' ? '#E67E22' : '#3498DB';

    // 1. Discord Webhook Payload
    if (isDiscord) {
      return {
        username: 'BloodSync Sentry Bot',
        avatar_url: 'https://raw.githubusercontent.com/RajputMayurSingh69/bloodsync_andriod/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png',
        content: `🚨 **[${data.severity}] ${data.errorCategory}** in \`${NODE_ENV}\``,
        embeds: [
          {
            title: data.title,
            description: data.message,
            color: colorHex,
            fields: [
              { name: 'Environment', value: `\`${NODE_ENV}\``, inline: true },
              { name: 'Severity', value: `**${data.severity}**`, inline: true },
              { name: 'Category', value: `\`${data.errorCategory}\``, inline: true },
              { name: 'Request ID', value: `\`${data.requestId}\``, inline: true },
              { name: 'User', value: data.maskedUserEmail || 'N/A', inline: true },
            ],
            footer: {
              text: 'BloodSync Monitoring Engine • bloodsync-3b5cf',
            },
            timestamp: data.timestamp,
          },
        ],
      };
    }

    // 2. Slack Webhook Payload
    if (isSlack) {
      return {
        text: `🚨 *BloodSync Alert [${data.severity}]*: \`${data.errorCategory}\`\n${data.message}`,
        attachments: [
          {
            color: colorSlack,
            title: data.title,
            text: data.message,
            fields: [
              { title: 'Environment', value: NODE_ENV, short: true },
              { title: 'Severity', value: data.severity, short: true },
              { title: 'Category', value: data.errorCategory, short: true },
              { title: 'Request ID', value: data.requestId, short: true },
              { title: 'User', value: data.maskedUserEmail || 'N/A', short: true },
            ],
            footer: 'BloodSync Monitoring Engine',
            ts: Math.floor(Date.now() / 1000),
          },
        ],
      };
    }

    // 3. Telegram Bot Webhook
    if (isTelegram) {
      const chatId = parsedUrl.searchParams.get('chat_id') || process.env.TELEGRAM_CHAT_ID;
      const text = `🚨 <b>BloodSync Alert [${data.severity}]</b>\n` +
        `<b>Category:</b> <code>${data.errorCategory}</code>\n` +
        `<b>Environment:</b> <code>${NODE_ENV}</code>\n` +
        `<b>User:</b> <code>${data.maskedUserEmail}</code>\n` +
        `<b>Request ID:</b> <code>${data.requestId}</code>\n\n` +
        `<b>Details:</b> ${data.message}`;

      return {
        chat_id: chatId,
        text,
        parse_mode: 'HTML',
      };
    }

    // 4. Universal Generic Webhook (Compatible with Discord, Slack, Zapier, n8n, Webhook.site)
    return {
      content: `🚨 [${data.severity}] BloodSync Alert: ${data.errorCategory} - ${data.message}`,
      text: `🚨 [${data.severity}] BloodSync Alert: ${data.errorCategory} - ${data.message}`,
      severity: data.severity,
      errorCategory: data.errorCategory,
      title: data.title,
      message: data.message,
      requestId: data.requestId,
      maskedUserEmail: data.maskedUserEmail,
      serverEnvironment: NODE_ENV,
      timestamp: data.timestamp,
      metadata: data.metadata,
      embeds: [
        {
          title: data.title,
          description: data.message,
          color: colorHex,
          timestamp: data.timestamp,
        },
      ],
    };
  }

  /**
   * Dispatches the HTTP/HTTPS request safely.
   */
  dispatchHttpRequest(parsedUrl, payload) {
    return new Promise((resolve) => {
      const isHttps = parsedUrl.protocol === 'https:';
      const transport = isHttps ? https : http;
      const jsonBody = JSON.stringify(payload);

      const options = {
        hostname: parsedUrl.hostname,
        port: parsedUrl.port || (isHttps ? 443 : 80),
        path: parsedUrl.pathname + parsedUrl.search,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(jsonBody),
          'User-Agent': 'BloodSync-AlertBot/1.0',
        },
        timeout: 6000,
      };

      const req = transport.request(options, (res) => {
        let respData = '';
        res.on('data', (chunk) => { respData += chunk; });
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            console.log(`[WebhookAlert] Successfully dispatched alert to ${parsedUrl.hostname} (Status ${res.statusCode})`);
            resolve({ success: true, statusCode: res.statusCode });
          } else {
            console.warn(`[WebhookAlert] Target returned HTTP ${res.statusCode}: ${respData.substring(0, 150)}`);
            resolve({ success: false, statusCode: res.statusCode, body: respData });
          }
        });
      });

      req.on('error', (err) => {
        console.warn(`[WebhookAlert] Network dispatch error: ${err.message}`);
        resolve({ success: false, error: err.message });
      });

      req.on('timeout', () => {
        req.destroy();
        console.warn('[WebhookAlert] Dispatch request timed out after 6000ms');
        resolve({ success: false, error: 'Request timeout' });
      });

      req.write(jsonBody);
      req.end();
    });
  }
}

module.exports = new WebhookAlertService();
