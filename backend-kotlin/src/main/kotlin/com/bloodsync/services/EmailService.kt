package com.bloodsync.services

import com.bloodsync.config.AppConfig
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * BloodSync Backend - Email Dispatch Service (Kotlin)
 *
 * Handles Jakarta Mail (JavaMail) Gmail SMTP transmission.
 * Mirrors emailService.js 1:1 — same error classification, same flow.
 */
object EmailService {

    data class EmailResult(
        val success: Boolean,
        val messageId: String? = null,
        val errorCategory: String? = null,
        val errorMessage: String? = null
    )

    suspend fun sendOtpEmail(recipientEmail: String, otp: String, requestId: String): EmailResult {
        val maskedEmail = maskEmail(recipientEmail)

        if (AppConfig.smtpUser.isBlank() || AppConfig.smtpPassword.isBlank()) {
            println("[EmailService][$requestId] Missing SMTP credentials for dispatch.")
            return EmailResult(
                success = false,
                errorCategory = "EMAIL_CONFIGURATION_ERROR",
                errorMessage = "Gmail SMTP credentials (SMTP_USER / SMTP_PASSWORD) not configured."
            )
        }

        val htmlBody = generateOtpEmailHtml(otp, AppConfig.otpExpiryMinutes, recipientEmail)
        val textBody = "Your BloodSync verification code is: $otp\nThis code expires in ${AppConfig.otpExpiryMinutes} minutes."

        return withContext(Dispatchers.IO) {
            try {
                println("[EmailService][$requestId] Dispatching OTP email to $maskedEmail...")

                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", AppConfig.smtpHost)
                    put("mail.smtp.port", AppConfig.smtpPort.toString())
                    put("mail.smtp.ssl.trust", AppConfig.smtpHost)
                    put("mail.smtp.connectiontimeout", "10000")
                    put("mail.smtp.timeout", "10000")
                }

                val session = Session.getInstance(props) { smtp ->
                    smtp.connect(AppConfig.smtpUser, AppConfig.smtpPassword)
                }

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(AppConfig.smtpFromAddress, AppConfig.smtpFromName))
                    setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                    subject = "🩸 BloodSync Verification Code"
                    addHeader("X-BloodSync-Request-ID", requestId)
                    addHeader("X-Priority", "1")

                    val multipart = MimeMultipart("alternative")

                    val textPart = MimeBodyPart().apply { setText(textBody, "utf-8") }
                    val htmlPart = MimeBodyPart().apply { setContent(htmlBody, "text/html; charset=utf-8") }

                    multipart.addBodyPart(textPart)
                    multipart.addBodyPart(htmlPart)
                    setContent(multipart)
                }

                Transport.send(message, AppConfig.smtpUser, AppConfig.smtpPassword)

                val messageId = message.messageID ?: "<sent>"
                println("[EmailService][$requestId] Email dispatched successfully. MessageID: $messageId")
                EmailResult(success = true, messageId = messageId)
            } catch (e: Exception) {
                val classified = classifySmtpError(e)
                println("[EmailService][$requestId] Failed sending to $maskedEmail. Category: ${classified.first}. Error: ${classified.second}")
                EmailResult(success = false, errorCategory = classified.first, errorMessage = classified.second)
            }
        }
    }

    private fun classifySmtpError(e: Exception): Pair<String, String> {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("badcredentials") || msg.contains("username and password not accepted") ||
            msg.contains("invalid credentials") || msg.contains("authentication") ->
                Pair("SMTP_AUTH_ERROR", "Gmail SMTP Authentication Failed. Check SMTP_USER and Gmail 16-character App Password.")
            msg.contains("connection refused") || msg.contains("unknown host") || msg.contains("timeout") ->
                Pair("EMAIL_CONFIGURATION_ERROR", "Network or host configuration error: ${e.message}")
            else ->
                Pair("EMAIL_SEND_ERROR", "Failed to deliver email: ${e.message}")
        }
    }

    private fun maskEmail(email: String): String {
        val atIdx = email.indexOf('@')
        if (atIdx <= 1) return "***@***"
        val local = email.substring(0, atIdx)
        val domain = email.substring(atIdx)
        val visible = local.take(2)
        return "$visible***$domain"
    }

    /** Minimal HTML OTP email template — mirrors otpEmailTemplate.js */
    private fun generateOtpEmailHtml(otp: String, expiryMinutes: Int, recipientEmail: String): String = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>BloodSync Verification</title></head>
        <body style="font-family:Arial,sans-serif;background:#f9f9f9;padding:20px;">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:12px;padding:32px;box-shadow:0 4px 20px rgba(0,0,0,.08);">
            <div style="text-align:center;margin-bottom:24px;">
              <h2 style="color:#D32F2F;margin:0;">🩸 BloodSync</h2>
              <p style="color:#666;margin:4px 0 0;">Your Email Verification Code</p>
            </div>
            <div style="background:#FFF3F3;border:2px dashed #D32F2F;border-radius:10px;padding:24px;text-align:center;margin:24px 0;">
              <p style="margin:0 0 8px;color:#555;font-size:14px;">Your verification code is:</p>
              <h1 style="font-size:48px;letter-spacing:12px;color:#D32F2F;margin:0;font-weight:700;">$otp</h1>
              <p style="margin:8px 0 0;color:#888;font-size:12px;">Expires in $expiryMinutes minutes</p>
            </div>
            <p style="color:#555;font-size:14px;text-align:center;">
              Enter this code in the BloodSync app to verify your email address.
            </p>
            <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
            <p style="color:#aaa;font-size:11px;text-align:center;">
              This code was requested for $recipientEmail. If you did not request this, please ignore this email.
            </p>
          </div>
        </body>
        </html>
    """.trimIndent()
}
