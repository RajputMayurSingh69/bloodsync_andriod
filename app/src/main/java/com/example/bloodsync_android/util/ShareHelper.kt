package com.example.bloodsync_android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.bloodsync_android.data.model.EmergencyRequest

object ShareHelper {

    /**
     * Directly opens WhatsApp chat with a given phone number and prefilled message.
     * If WhatsApp is not installed, falls back to browser wa.me link or Android share sheet.
     */
    fun openWhatsApp(context: Context, phoneNumber: String, prefillMessage: String) {
        // Clean phone number: remove spaces, dashes, brackets
        var cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.startsWith("+")) {
            cleanPhone = cleanPhone.substring(1)
        }
        
        if (cleanPhone.length < 7) {
            Toast.makeText(
                context,
                "Donor phone number is protected for privacy. Broadcast an Emergency SOS to connect with nearby donors!",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        try {
            val encodedMessage = Uri.encode(prefillMessage)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // If com.whatsapp fails, try com.whatsapp.w4b (WhatsApp Business) or general browser
            try {
                val encodedMessage = Uri.encode(prefillMessage)
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                // Open browser wa.me link as universal fallback
                try {
                    val encodedMessage = Uri.encode(prefillMessage)
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/$cleanPhone?text=$encodedMessage")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                } catch (e3: Exception) {
                    Toast.makeText(context, "Could not open WhatsApp: ${e3.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Broadcasts an Emergency Blood Request directly to WhatsApp, Telegram, SMS, or any sharing app.
     */
    fun shareEmergencySos(context: Context, request: EmergencyRequest) {
        val sosText = buildString {
            appendLine("🚨 *URGENT BLOOD REQUIRED - BLOODSYNC SOS* 🚨")
            appendLine("----------------------------------------")
            appendLine("🩸 *Blood Group:* ${request.bloodGroupNeeded} (${request.unitsRequired} Units)")
            appendLine("👤 *Patient:* ${request.patientName}")
            appendLine("🏥 *Hospital:* ${request.hospitalName}")
            appendLine("📍 *Location:* ${request.hospitalAddress}")
            appendLine("📞 *Contact Number:* ${request.contactPhone}")
            appendLine("⚠️ *Urgency Level:* ${request.urgencyLevel.name}")
            if (request.additionalNotes.isNotBlank()) {
                appendLine("📝 *Note:* ${request.additionalNotes}")
            }
            appendLine("----------------------------------------")
            appendLine("🙏 *Please share in your WhatsApp groups! Every second counts to save a life.*")
            appendLine("📲 *BloodSync App:* Connect instantly with nearby blood donors.")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "URGENT BLOOD SOS: ${request.bloodGroupNeeded} Needed")
            putExtra(Intent.EXTRA_TEXT, sosText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, "Share Emergency SOS via WhatsApp / Apps").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Share invitation to join BloodSync donor community on WhatsApp.
     */
    fun shareAppInvite(context: Context) {
        val inviteText = buildString {
            appendLine("❤️ *Join the BloodSync Lifesaver Community!*")
            appendLine("Become a verified voluntary blood donor or find matching blood donors during medical emergencies.")
            appendLine("🩸 WHO 90-day donation safety tracking")
            appendLine("🚨 24/7 Real-time Emergency SOS broadcasting")
            appendLine("📜 Official verified donor appreciation certificates")
            appendLine("Download BloodSync & help save lives today!")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, "Share BloodSync via WhatsApp / Apps").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
