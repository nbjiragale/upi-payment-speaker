package com.nbjiragale.upispeaker.parser

/**
 * A parsed UPI transaction extracted from either a notification or an SMS.
 *
 * Amount is stored as paise (1 INR = 100 paise) to avoid floating-point drift.
 */
data class Transaction(
    val direction: Direction,
    val amountPaise: Long,
    val payerOrPayee: String?,
    val referenceId: String?,
    val sourcePackage: String,
    val source: Source,
    val rawText: String,
    val timestampMs: Long
) {
    enum class Direction { CREDIT, DEBIT }
    enum class Source { NOTIFICATION, SMS, ACCESSIBILITY }

    /** A stable hash used to de-dupe the same transaction arriving via two paths. */
    fun dedupeKey(): String {
        val ref = referenceId.orEmpty().ifEmpty { rawText.hashCode().toString() }
        return "$direction|$amountPaise|$ref"
    }
}
