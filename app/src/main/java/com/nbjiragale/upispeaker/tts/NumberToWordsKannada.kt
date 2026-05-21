package com.nbjiragale.upispeaker.tts

/**
 * Indian numbering converter for Kannada:
 * 250 -> "ಇನ್ನೂರ ಐವತ್ತು" (innūra aivattu),
 * 100000 -> "ಒಂದು ಲಕ್ಷ" (ondu laksha),
 * 10000000 -> "ಒಂದು ಕೋಟಿ" (ondu kōṭi).
 *
 * Uses the standard Kannada number word system with Indian grouping
 * (ones, tens, hundreds, thousands, lakhs, crores).
 */
object NumberToWordsKannada {

    private val ones = arrayOf(
        "ಸೊನ್ನೆ",      // 0 - sonne
        "ಒಂದು",        // 1 - ondu
        "ಎರಡು",        // 2 - eradu
        "ಮೂರು",        // 3 - mooru
        "ನಾಲ್ಕು",      // 4 - naalku
        "ಐದು",         // 5 - aidu
        "ಆರು",         // 6 - aaru
        "ಏಳು",         // 7 - elu
        "ಎಂಟು",        // 8 - entu
        "ಒಂಬತ್ತು",     // 9 - ombattu
        "ಹತ್ತು",       // 10 - hattu
        "ಹನ್ನೊಂದು",    // 11 - hannondu
        "ಹನ್ನೆರಡು",    // 12 - hanneradu
        "ಹದಿಮೂರು",     // 13 - hadimooru
        "ಹದಿನಾಲ್ಕು",   // 14 - hadinaalku
        "ಹದಿನೈದು",     // 15 - hadinaidu
        "ಹದಿನಾರು",     // 16 - hadinaaru
        "ಹದಿನೇಳು",     // 17 - hadinelu
        "ಹದಿನೆಂಟು",    // 18 - hadinentu
        "ಹತ್ತೊಂಬತ್ತು"  // 19 - hattombattu
    )

    private val tens = arrayOf(
        "",             // 0
        "",             // 10 (covered by ones)
        "ಇಪ್ಪತ್ತು",    // 20 - ippattu
        "ಮೂವತ್ತು",     // 30 - moovattu
        "ನಲವತ್ತು",     // 40 - nalavattu
        "ಐವತ್ತು",      // 50 - aivattu
        "ಅರವತ್ತು",     // 60 - aravattu
        "ಎಪ್ಪತ್ತು",    // 70 - eppattu
        "ಎಂಬತ್ತು",     // 80 - embattu
        "ತೊಂಬತ್ತು"     // 90 - tombattu
    )

    fun kannadaIndian(amount: Long): String {
        if (amount == 0L) return ones[0]
        if (amount < 0) return "ಮೈನಸ್ " + kannadaIndian(-amount)

        val crore = amount / 10_000_000L
        var rest = amount % 10_000_000L
        val lakh = rest / 100_000L
        rest %= 100_000L
        val thousand = rest / 1_000L
        rest %= 1_000L
        val hundred = rest / 100L
        rest %= 100L

        val sb = StringBuilder()
        if (crore > 0) sb.append(twoDigitGroup(crore)).append(" ಕೋಟಿ ")
        if (lakh > 0) sb.append(twoDigitGroup(lakh)).append(" ಲಕ್ಷ ")
        if (thousand > 0) sb.append(twoDigitGroup(thousand)).append(" ಸಾವಿರ ")
        if (hundred > 0) sb.append(ones[hundred.toInt()]).append("ನೂರ ")
        if (rest > 0) sb.append(twoDigitGroup(rest))
        return sb.toString().trim()
    }

    private fun twoDigitGroup(n: Long): String {
        if (n < 20) return ones[n.toInt()]
        val t = (n / 10).toInt()
        val o = (n % 10).toInt()
        return if (o == 0) tens[t] else "${tens[t]} ${ones[o]}"
    }
}
