package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatTaka(amount: Double, useBanglaDigits: Boolean = false): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        val formattedNumber = formatter.format(kotlin.math.abs(amount))

        val finalNumber = if (useBanglaDigits) toBanglaDigits(formattedNumber) else formattedNumber
        return "৳ $finalNumber"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeDateBangla(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val oneDay = 24 * 60 * 60 * 1000L

        return when {
            diff < 0 -> formatDate(timestamp)
            diff < oneDay -> "আজকে"
            diff < 2 * oneDay -> "গতকাল"
            diff < 7 * oneDay -> "${diff / oneDay} দিন আগে"
            else -> formatDate(timestamp)
        }
    }
}
