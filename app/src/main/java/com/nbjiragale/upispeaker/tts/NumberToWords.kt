package com.nbjiragale.upispeaker.tts

/**
 * Indian numbering converter: 250 -> "two hundred and fifty",
 * 100000 -> "one lakh", 12500 -> "twelve thousand five hundred".
 *
 * Stub for the scaffold; full multi-locale implementation lands in M2.
 */
object NumberToWords {

    private val ones = arrayOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen"
    )

    private val tens = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    )

    fun englishIndian(amount: Long): String {
        if (amount == 0L) return "zero"
        if (amount < 0) return "minus " + englishIndian(-amount)

        val crore = amount / 10_000_000L
        var rest = amount % 10_000_000L
        val lakh = rest / 100_000L
        rest %= 100_000L
        val thousand = rest / 1_000L
        rest %= 1_000L
        val hundred = rest / 100L
        rest %= 100L

        val sb = StringBuilder()
        if (crore > 0) sb.append(twoDigitGroup(crore)).append(" crore ")
        if (lakh > 0) sb.append(twoDigitGroup(lakh)).append(" lakh ")
        if (thousand > 0) sb.append(twoDigitGroup(thousand)).append(" thousand ")
        if (hundred > 0) sb.append(ones[hundred.toInt()]).append(" hundred ")
        if (rest > 0) {
            if (sb.isNotEmpty()) sb.append("and ")
            sb.append(twoDigitGroup(rest))
        }
        return sb.toString().trim()
    }

    private fun twoDigitGroup(n: Long): String {
        if (n < 20) return ones[n.toInt()]
        val t = (n / 10).toInt()
        val o = (n % 10).toInt()
        return if (o == 0) tens[t] else "${tens[t]} ${ones[o]}"
    }
}
