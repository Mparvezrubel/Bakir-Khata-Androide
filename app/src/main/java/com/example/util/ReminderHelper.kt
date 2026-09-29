package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ReminderHelper {

    fun composeReminderMessage(
        template: String,
        customerName: String,
        amount: Double,
        shopName: String
    ): String {
        val amountStr = Formatters.formatTaka(amount)
        return template
            .replace("{NAME}", customerName.ifBlank { "সম্মানিত গ্রাহক" })
            .replace("{AMOUNT}", amountStr)
            .replace("{SHOP}", shopName.ifBlank { "আমাদের দোকান" })
    }

    private fun cleanPhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }

    fun openSmsApp(context: Context, phoneNumber: String, message: String) {
        val clean = cleanPhoneNumber(phoneNumber)
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$clean")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "এসএমএস অ্যাপ চালু করা যায়নি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String, message: String) {
        var clean = cleanPhoneNumber(phoneNumber)
        if (!clean.startsWith("+")) {
            // Bangladesh country code default if starts with 01
            if (clean.startsWith("01")) {
                clean = "+88$clean"
            }
        }
        val encodedMsg = Uri.encode(message)
        val url = "https://api.whatsapp.com/send?phone=$clean&text=$encodedMsg"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "হোয়াটসঅ্যাপ চালু করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        val clean = cleanPhoneNumber(phoneNumber)
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ডায়াল প্যাড চালু করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(context: Context, subject: String, content: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
        } catch (e: Exception) {
            Toast.makeText(context, "শেয়ার করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }
}
