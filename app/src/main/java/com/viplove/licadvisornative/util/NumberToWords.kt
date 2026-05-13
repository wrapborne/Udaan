package com.viplove.licadvisornative.util

import java.text.NumberFormat
import java.util.*

object NumberToWords {

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
        "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private fun convertLessThanOneThousand(number: Int): String {
        var n = number
        val current: String

        if (n % 100 < 20) {
            current = units[n % 100]
            n /= 100
        } else {
            val unitPart = units[n % 10]
            n /= 10
            val tenPart = tens[n % 10]
            current = if (unitPart.isNotEmpty()) "$tenPart $unitPart" else tenPart
            n /= 10
        }
        if (n == 0) return current.trim()
        val hundredPart = units[n]
        return if (current.isNotEmpty()) "$hundredPart Hundred $current" else "$hundredPart Hundred"
    }

    fun convert(number: Long): String {
        if (number == 0L) return "Zero"

        var num = number
        var answer = ""

        if (num / 10000000 > 0) {
            answer += convertLessThanOneThousand((num / 10000000).toInt()) + " Crore "
            num %= 10000000
        }

        if (num / 100000 > 0) {
            answer += convertLessThanOneThousand((num / 100000).toInt()) + " Lakh "
            num %= 100000
        }

        if (num / 1000 > 0) {
            answer += convertLessThanOneThousand((num / 1000).toInt()) + " Thousand "
            num %= 1000
        }

        if (num > 0) {
            answer += convertLessThanOneThousand(num.toInt())
        }

        return answer.trim().replace("\\s+".toRegex(), " ") + " Only"
    }
}
