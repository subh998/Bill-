package com.example.export

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

object NumberFormatters {

    private val indianSymbols = DecimalFormatSymbols(Locale.ENGLISH).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun formatAmount(value: Double): String {
        val rounded = (value * 100.0).roundToLong() / 100.0
        val whole = rounded.toLong()
        val frac = ((rounded - whole) * 100.0).roundToLong().coerceIn(0L, 99L)
        val wholeStr = formatIndianInteger(whole)
        return "$wholeStr.${frac.toString().padStart(2, '0')}"
    }

    fun formatQuantity(value: Double): String {
        val df = DecimalFormat("#,##0.###", indianSymbols)
        return df.format(value)
    }

    fun formatIndianInteger(number: Long): String {
        val negative = number < 0
        val absStr = kotlin.math.abs(number).toString()
        if (absStr.length <= 3) return if (negative) "-$absStr" else absStr
        val lastThree = absStr.takeLast(3)
        val remaining = absStr.dropLast(3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        val formatted = "${sb.reverse()},$lastThree"
        return if (negative) "-$formatted" else formatted
    }
}

object IndianCurrencyToWords {

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convert(amount: Double): String {
        if (amount <= 0.0) return "Rupees Zero Only"
        val totalPaise = (amount * 100.0).roundToLong()
        val rupees = totalPaise / 100L
        val paise = (totalPaise % 100L).toInt()

        val rupeesPart = if (rupees == 0L) "Zero" else convertInteger(rupees)
        return if (paise > 0) {
            "Rupees $rupeesPart and ${convertTwoDigits(paise)} Paise Only"
        } else {
            "Rupees $rupeesPart Only"
        }
    }

    private fun convertInteger(n: Long): String {
        if (n == 0L) return ""
        val sb = StringBuilder()
        var num = n

        val crore = (num / 10_000_000L).toInt()
        num %= 10_000_000L
        if (crore > 0) {
            sb.append(convertInteger(crore.toLong())).append(" Crore ")
        }

        val lakh = (num / 100_000L).toInt()
        num %= 100_000L
        if (lakh > 0) {
            sb.append(convertTwoDigits(lakh)).append(" Lakh ")
        }

        val thousand = (num / 1_000L).toInt()
        num %= 1_000L
        if (thousand > 0) {
            sb.append(convertTwoDigits(thousand)).append(" Thousand ")
        }

        val hundred = (num / 100L).toInt()
        num %= 100L
        if (hundred > 0) {
            sb.append(units[hundred]).append(" Hundred ")
        }

        if (num > 0L) {
            sb.append(convertTwoDigits(num.toInt()))
        }

        return sb.toString().trim()
    }

    private fun convertTwoDigits(n: Int): String = when {
        n < 20 -> units[n]
        else -> {
            val t = tens[n / 10]
            val u = units[n % 10]
            if (u.isEmpty()) t else "$t $u"
        }
    }
}
