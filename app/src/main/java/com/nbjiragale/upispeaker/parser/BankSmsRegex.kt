package com.nbjiragale.upispeaker.parser

/**
 * Sender-ID-keyed extractors for Indian bank SMS.  Used by the sideload-only
 * SmsReceiver (Path B in the plan).
 *
 * SMS sender IDs in India look like `VK-HDFCBK`, `VM-SBIINB`, `JD-AXISBK`,
 * etc. The two-letter prefix is the operator; the suffix identifies the
 * bank. We match against the suffix.
 *
 * TODO(M5): port the actual regexes from a known-good library
 *           (e.g. github.com/jordan-wright/smsparser or build our own).
 *           Stubbed for now so the scaffold compiles.
 */
object BankSmsRegex {

    data class BankPattern(val bankSuffix: String, val creditRegex: Regex, val debitRegex: Regex)

    private val patterns: List<BankPattern> = emptyList()

    @Suppress("UNUSED_PARAMETER")
    fun parse(sender: String, body: String, timestampMs: Long): Transaction? {
        // TODO(M5): real implementation.
        return null
    }
}
