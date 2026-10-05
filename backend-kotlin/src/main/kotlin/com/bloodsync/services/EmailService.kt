package com.bloodsync.services

import com.bloodsync.config.*
import com.bloodsync.templates.generateOtpEmailTemplate
import com.bloodsync.utils.maskEmail
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart

/**
 * BloodSync Backend - Email Dispatch Service
 * Converted from: backend/src/services/emailService.js
 *
 * Handles Jakarta Mail Gmail SMTP transmission with error mapping
 * and zero credential exposure.
 */

private val logger = KotlinLogging.logger {}

data class EmailResult(
    val success: Boolean,
    val status: String,
    val messageId: String? = null,
    val errorCategory: String? = null,
    val errorMessage: String? = null,
)

object EmailService {

    /**
     * Dispatches the OTP email to the recipient using Jakarta Mail Gmail SMTP.
     */
    fun sendOtpEmail(
        recipientEmail: String,
        otp: String,
        requestId: String,
    ): EmailResult {
        val maskedEmail = maskEmail(recipientEmail)

        // 1. Verify email configuration
        if (SMTP_CONFIG.user.isBlank() || SMTP_CONFIG.password.isBlank()) {
            logger.error { "[EmailService][$requestId] Missing SMTP credentials for dispatch." }
            return EmailResult(
                success       = false,
                status        = EmailSendStatus.FAILED,
                errorCategory = ErrorCategories.EMAIL_CONFIGURATION_ERROR,
                errorMessage  = "Gmail SMTP credentials (SMTP_USER / SMTP_PASSWORD) not configured.",
            )
        }

        // 2. Generate template
        val template = generateOtpEmailTemplate(
            otp            = otp,
            expiryMinutes  = SECURITY_CONFIG.otpExpiryMinutes,
            recipientEmail = recipientEmail,
        )

        return try {
            val session = Mailer.getSession()
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress("${SMTP_CONFIG.fromName} <${SMTP_CONFIG.fromAddress}>"))
                setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = "🩸 BloodSync Verification Code"
                addHeader("X-BloodSync-Request-ID", requestId)
                addHeader("X-Priority", "1")

                val htmlPart = MimeBodyPart().also { it.setContent(template.html, "text/html; charset=UTF-8") }
                val textPart = MimeBodyPart().also { it.setText(template.text, "UTF-8") }
                val multipart = MimeMultipart("alternative").apply {
                    addBodyPart(textPart)
                    addBodyPart(htmlPart)
                }
                setContent(multipart)
            }

            logger.info { "[EmailService][$requestId] Dispatching OTP email to $maskedEmail..." }

            val transport = session.getTransport("smtp")
            transport.connect(SMTP_CONFIG.host, SMTP_CONFIG.user, SMTP_CONFIG.password)
            transport.sendMessage(msg, msg.allRecipients)
            transport.close()

            logger.info { "[EmailService][$requestId] Email dispatched successfully to $maskedEmail." }

            EmailResult(
                success   = true,
                status    = EmailSendStatus.SENT,
                messageId = msg.messageID,
            )
        } catch (error: Exception) {
            val classified = classifySmtpError(error)
            logger.error {
                "[EmailService][$requestId] Failed sending to $maskedEmail. " +
                    "Category: ${classified.first}. Error: ${classified.second}"
            }
            EmailResult(
                success       = false,
                status        = EmailSendStatus.FAILED,
                errorCategory = classified.first,
                errorMessage  = classified.second,
            )
        }
    }

    /**
     * Classifies low-level SMTP errors into strict system error categories.
     * Returns Pair(errorCategory, humanMessage).
     */
    private fun classifySmtpError(error: Exception): Pair<String, String> {
        val msg = error.message?.lowercase() ?: ""

        return when {
            msg.contains("authentication failed")
                || msg.contains("badcredentials")
                || msg.contains("username and password not accepted")
                || msg.contains("invalid credentials")
                || msg.contains("535") ->
                ErrorCategories.SMTP_AUTH_ERROR to
                    "Gmail SMTP Authentication Failed. Check SMTP_USER and Gmail 16-character App Password."

            msg.contains("connection refused")
                || msg.contains("unknownhostexception")
                || msg.contains("timed out") ->
                ErrorCategories.EMAIL_CONFIGURATION_ERROR to
                    "Network or host configuration error: ${error.javaClass.simpleName}"

            else ->
                ErrorCategories.EMAIL_SEND_ERROR to "Failed to deliver email: ${error.message}"
        }
    }
}
