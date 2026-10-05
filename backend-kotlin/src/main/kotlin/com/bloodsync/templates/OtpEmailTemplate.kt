package com.bloodsync.templates

/**
 * BloodSync Backend - Branded OTP Email Template
 * Converted from: backend/src/templates/otpEmailTemplate.js
 * Produces responsive, accessible HTML and plaintext emails.
 */

data class OtpEmailTemplate(val html: String, val text: String)

fun generateOtpEmailTemplate(
    otp: String,
    expiryMinutes: Int = 5,
    recipientEmail: String = "",
): OtpEmailTemplate {
    val primaryColor   = "#C62828" // Crimson Red
    val darkTextColor  = "#1F2937"
    val lightBgColor   = "#F9FAFB"
    val currentYear    = java.time.Year.now().value

    val html = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Your BloodSync Verification Code</title>
  <style>
    body {
      margin: 0; padding: 0;
      background-color: $lightBgColor;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      color: $darkTextColor; line-height: 1.6;
    }
    .wrapper { width: 100%; table-layout: fixed; background-color: $lightBgColor; padding: 40px 0; }
    .container {
      max-width: 560px; margin: 0 auto; background: #FFFFFF;
      border-radius: 12px; overflow: hidden;
      box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #E5E7EB;
    }
    .header {
      background: linear-gradient(135deg, #B71C1C 0%, #D32F2F 100%);
      padding: 32px 24px; text-align: center; color: #FFFFFF;
    }
    .header h1 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 0.5px; }
    .header p  { margin: 6px 0 0; font-size: 14px; opacity: 0.9; }
    .content   { padding: 36px 32px; }
    .greeting  { font-size: 16px; font-weight: 600; margin-bottom: 12px; color: #111827; }
    .otp-card  {
      background-color: #FEF2F2; border: 2px dashed #F87171;
      border-radius: 10px; padding: 20px; margin: 28px 0; text-align: center;
    }
    .otp-code {
      font-family: 'Courier New', Courier, monospace;
      font-size: 38px; font-weight: 800; letter-spacing: 8px;
      color: $primaryColor; display: inline-block; margin: 4px 0;
    }
    .badge-expiry { display: inline-block; margin-top: 8px; font-size: 13px; color: #991B1B; font-weight: 500; }
    .security-notice {
      background: #F3F4F6; border-left: 4px solid #9CA3AF;
      padding: 14px 16px; border-radius: 4px;
      font-size: 13px; color: #4B5563; margin-top: 24px;
    }
    .footer {
      background-color: #F9FAFB; border-top: 1px solid #E5E7EB;
      padding: 20px 32px; text-align: center; font-size: 12px; color: #6B7280;
    }
    .footer a { color: $primaryColor; text-decoration: none; }
  </style>
</head>
<body>
  <div class="wrapper">
    <div class="container">
      <div class="header">
        <h1>🩸 BloodSync</h1>
        <p>Real-Time Lifesaving Blood Donation Network</p>
      </div>
      <div class="content">
        <div class="greeting">Hello,</div>
        <p>You requested a one-time verification code to securely access or verify your BloodSync account.</p>
        <div class="otp-card">
          <div style="font-size:13px;text-transform:uppercase;color:#7F1D1D;letter-spacing:1px;font-weight:600;">Your One-Time Password</div>
          <div class="otp-code">$otp</div>
          <div class="badge-expiry">⏱ Valid for the next $expiryMinutes minutes</div>
        </div>
        <p style="font-size:14px;color:#4B5563;">Enter this 6-digit code on the BloodSync application screen to proceed. Never share this code with anyone, including BloodSync support personnel.</p>
        <div class="security-notice">
          <strong>Security Advisory:</strong> If you did not initiate this request, please disregard this email. Your BloodSync account remains secure as long as this code is not disclosed.
        </div>
      </div>
      <div class="footer">
        <p>&copy; $currentYear BloodSync Network. All rights reserved.</p>
        <p>This is an automated operational notification dispatched to <strong>$recipientEmail</strong>.</p>
      </div>
    </div>
  </div>
</body>
</html>
    """.trimIndent()

    val text = """
🩸 BloodSync Verification Code
======================================
Your one-time verification code is: $otp

This code is valid for $expiryMinutes minutes.

Enter this code in the BloodSync application to complete your request.
For your protection, never share this code with anyone.

If you did not request this verification code, please ignore this email.

© $currentYear BloodSync Network
    """.trimIndent()

    return OtpEmailTemplate(html = html, text = text)
}
