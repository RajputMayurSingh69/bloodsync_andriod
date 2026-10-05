package com.bloodsync

import com.bloodsync.utils.CryptoUtils.generateSecureOtp
import com.bloodsync.utils.CryptoUtils.hashOtp
import com.bloodsync.utils.CryptoUtils.timingSafeVerify
import com.bloodsync.utils.Sanitizer.maskEmail
import com.bloodsync.utils.Validator.isValidOtpFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TestScenarios {

    @Test
    fun testCryptographicOtpGenerationAndHashing() {
        println("📌 [Scenario 1] Cryptographic Generation & Secure Hashing")
        
        val otp1 = generateSecureOtp(6)
        assertTrue(otp1.matches(Regex("^\\d{6}$")), "Generated OTP is exactly 6 numeric digits")
        assertTrue(isValidOtpFormat(otp1), "isValidOtpFormat validates 6 digits correctly")

        val testEmail = "donor.alex@bloodsync.org"
        val hashedOtp = hashOtp(testEmail, otp1)
        assertEquals(64, hashedOtp.length, "HMAC-SHA256 returns 64-character hex hash")

        val candidateHash = hashOtp(testEmail, otp1)
        assertTrue(timingSafeVerify(candidateHash, hashedOtp), "timingSafeVerify matches correct OTP HMAC")

        val wrongHash = hashOtp(testEmail, "000000")
        assertFalse(timingSafeVerify(wrongHash, hashedOtp), "timingSafeVerify rejects invalid OTP HMAC")
    }

    @Test
    fun testSanitizationAndPrivacy() {
        println("📌 [Scenario 2] Privacy Masking & Credential Sanitization")
        
        assertEquals("a***x@gmail.com", maskEmail("alex@gmail.com"), "Masks short email as a***x@gmail.com")
        assertEquals("j***e@bloodsync.org", maskEmail("john.doe@bloodsync.org"), "Masks john.doe@bloodsync.org properly")
    }
}
