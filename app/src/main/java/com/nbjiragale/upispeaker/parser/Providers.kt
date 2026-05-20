package com.nbjiragale.upispeaker.parser

/**
 * Known UPI-related app packages. The NotificationListener routes incoming
 * notifications to the right extractor based on the source package.
 *
 * NOTE: extractor implementations are stubbed — they return null for now.
 * Milestone 2 of the plan fills these in for PhonePe and GPay first.
 */
object Providers {

    const val PHONEPE = "com.phonepe.app"
    const val GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PAYTM = "net.one97.paytm"
    const val BHIM = "in.org.npci.upiapp"
    const val AMAZON_PAY = "in.amazon.mShop.android.shopping"
    const val WHATSAPP = "com.whatsapp" // WhatsApp Pay
    const val CRED = "com.dreamplug.androidapp"

    /** Bank apps that surface UPI credit notifications. Add as needed. */
    val BANK_APPS = setOf(
        "com.snapwork.hdfc",
        "com.csam.icici.bank.imobile",
        "com.sbi.lotusintouch",
        "com.axis.mobile",
        "com.fss.pnbpsp",
        "com.kotak.android.kotak811"
    )

    fun isKnownUpiSource(pkg: String): Boolean =
        pkg in setOf(PHONEPE, GPAY, PAYTM, BHIM, AMAZON_PAY, WHATSAPP, CRED) ||
            pkg in BANK_APPS

    fun extractorFor(pkg: String): NotificationExtractor = when (pkg) {
        PHONEPE -> PhonePeExtractor
        GPAY -> GPayExtractor
        PAYTM -> PaytmExtractor
        else -> GenericExtractor
    }
}

/**
 * Strategy interface implemented per-provider.  Each extractor pulls
 * direction / amount / counterparty out of the notification's title +
 * text + bigText extras.
 *
 * Stubs return null so the foreground service can no-op gracefully until
 * we ship the real regex bundle in Milestone 2.
 */
interface NotificationExtractor {
    fun extract(
        sourcePackage: String,
        title: String?,
        text: String?,
        bigText: String?,
        timestampMs: Long
    ): Transaction?
}

object PhonePeExtractor : NotificationExtractor {
    override fun extract(sourcePackage: String, title: String?, text: String?, bigText: String?, timestampMs: Long): Transaction? {
        // TODO(M2): real PhonePe extraction.
        // Common patterns to handle:
        //   "Received ₹500 from Ramesh on PhonePe"
        //   "₹250 received from RAJESH via UPI"
        return null
    }
}

object GPayExtractor : NotificationExtractor {
    override fun extract(sourcePackage: String, title: String?, text: String?, bigText: String?, timestampMs: Long): Transaction? {
        // TODO(M2): real GPay extraction.
        return null
    }
}

object PaytmExtractor : NotificationExtractor {
    override fun extract(sourcePackage: String, title: String?, text: String?, bigText: String?, timestampMs: Long): Transaction? {
        // TODO(M4): real Paytm extraction.
        return null
    }
}

object GenericExtractor : NotificationExtractor {
    override fun extract(sourcePackage: String, title: String?, text: String?, bigText: String?, timestampMs: Long): Transaction? {
        // Last-resort fallback: scan combined text for "₹<amount>" + verb.
        // TODO(M4): implement.
        return null
    }
}
