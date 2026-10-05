package com.bloodsync.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import java.util.Properties

/**
 * BloodSync Backend - Jakarta Mail / Gmail SMTP Transporter Setup
 * Converted from: backend/src/config/mailer.js
 */

private val logger = KotlinLogging.logger {}

data class SmtpVerifyResult(
    val success: Boolean,
    val category: String? = null,
    val message: String,
    val code: String? = null,
)

object Mailer {

    @Volatile
    private var sessionInstance: Session? = null

    fun getSession(): Session {
        return sessionInstance ?: createSession().also { sessionInstance = it }
    }

    private fun createSession(): Session {
        val cfg = SMTP_CONFIG
        val props = Properties().apply {
            put("mail.smtp.host", cfg.host)
            put("mail.smtp.port", cfg.port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "30000")
            if (cfg.secure) {
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
                if (NODE_ENV == "production") {
                    put("mail.smtp.ssl.checkserveridentity", "true")
                }
            } else {
                put("mail.smtp.starttls.enable", "true")
            }
        }

        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(cfg.user, cfg.password)
            }
        })
    }

    /**
     * Verifies SMTP connection by attempting to open a transport connection
     * and classifies any configuration or authentication failure.
     */
    fun verifyTransporter(): SmtpVerifyResult {
        if (SMTP_CONFIG.user.isBlank() || SMTP_CONFIG.password.isBlank()) {
            return SmtpVerifyResult(
                success  = false,
                category = ErrorCategories.EMAIL_CONFIGURATION_ERROR,
                message  = "SMTP credentials missing. Please set SMTP_USER and SMTP_PASSWORD in .env",
            )
        }

        return try {
            val transport = getSession().getTransport("smtp")
            transport.connect(SMTP_CONFIG.host, SMTP_CONFIG.user, SMTP_CONFIG.password)
            transport.close()
            SmtpVerifyResult(
                success = true,
                message = "SMTP Transporter verified successfully for ${SMTP_CONFIG.user}",
            )
        } catch (error: Exception) {
            val msg = error.message?.lowercase() ?: ""
            val category = when {
                msg.contains("authentication failed")
                    || msg.contains("badcredentials")
                    || msg.contains("username and password not accepted")
                    || msg.contains("535") ->
                    ErrorCategories.SMTP_AUTH_ERROR

                msg.contains("connection refused")
                    || msg.contains("unknownhost")
                    || msg.contains("timed out") ->
                    ErrorCategories.EMAIL_CONFIGURATION_ERROR

                else -> ErrorCategories.EMAIL_SEND_ERROR
            }
            SmtpVerifyResult(
                success  = false,
                category = category,
                message  = error.message ?: "Unknown SMTP error",
                code     = error.javaClass.simpleName,
            )
        }
    }
}
