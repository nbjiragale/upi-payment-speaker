package com.nbjiragale.upispeaker

import com.nbjiragale.upispeaker.parser.PhonePeExtractor
import com.nbjiragale.upispeaker.parser.Providers
import com.nbjiragale.upispeaker.parser.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PhonePeExtractorTest {

    private val pkg = Providers.PHONEPE
    private val ts = System.currentTimeMillis()

    @Test
    fun sentToYou_simple() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Ramesh sent ₹10 to you.", null, ts
        )
        assertNotNull(tx)
        assertEquals(1000L, tx!!.amountPaise)
        assertEquals(Transaction.Direction.CREDIT, tx.direction)
        assertNull(tx.payerOrPayee)
    }

    @Test
    fun sentToYou_comma() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Suresh sent ₹1,000 to you.", null, ts
        )
        assertNotNull(tx)
        assertEquals(100_000L, tx!!.amountPaise)
    }

    @Test
    fun sentToYou_indianComma() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Anita sent ₹1,00,000 to you.", null, ts
        )
        assertNotNull(tx)
        assertEquals(10_000_000L, tx!!.amountPaise)
    }

    @Test
    fun received_fromName() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Received ₹500 from Ramesh on PhonePe", null, ts
        )
        assertNotNull(tx)
        assertEquals(50_000L, tx!!.amountPaise)
        assertEquals(Transaction.Direction.CREDIT, tx.direction)
    }

    @Test
    fun received_amountFirst() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "₹250 received from RAJESH via UPI", null, ts
        )
        assertNotNull(tx)
        assertEquals(25_000L, tx!!.amountPaise)
    }

    @Test
    fun received_youReceived() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "You received ₹1,000", null, ts
        )
        assertNotNull(tx)
        assertEquals(100_000L, tx!!.amountPaise)
    }

    @Test
    fun withPaise() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Ramesh sent ₹10.50 to you.", null, ts
        )
        assertNotNull(tx)
        assertEquals(1050L, tx!!.amountPaise)
    }

    @Test
    fun titleAndText() {
        val tx = PhonePeExtractor.extract(
            pkg, "PhonePe", "Deepak sent ₹2,500 to you.", null, ts
        )
        assertNotNull(tx)
        assertEquals(250_000L, tx!!.amountPaise)
    }

    @Test
    fun bigText_fallback() {
        val tx = PhonePeExtractor.extract(
            pkg, "PhonePe", null, "Ramesh sent ₹999 to you. Tap to view details.", ts
        )
        assertNotNull(tx)
        assertEquals(99_900L, tx!!.amountPaise)
    }

    @Test
    fun noAmount_returnsNull() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Open PhonePe to check your balance", null, ts
        )
        assertNull(tx)
    }

    @Test
    fun noCredit_keyword_returnsNull() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Your UPI PIN has been changed. ₹0 deducted.", null, ts
        )
        assertNull(tx)
    }

    @Test
    fun extractAmountPaise_basic() {
        assertEquals(1000L, PhonePeExtractor.extractAmountPaise("₹10"))
        assertEquals(25_000L, PhonePeExtractor.extractAmountPaise("₹250"))
        assertEquals(100_000L, PhonePeExtractor.extractAmountPaise("₹1,000"))
        assertEquals(10_000_000L, PhonePeExtractor.extractAmountPaise("₹1,00,000"))
        assertEquals(1050L, PhonePeExtractor.extractAmountPaise("₹10.50"))
    }

    @Test
    fun extractAmountPaise_noSymbol() {
        assertNull(PhonePeExtractor.extractAmountPaise("10 rupees"))
    }

    @Test
    fun sourceIsNotification() {
        val tx = PhonePeExtractor.extract(
            pkg, null, "Ramesh sent ₹10 to you.", null, ts
        )
        assertEquals(Transaction.Source.NOTIFICATION, tx!!.source)
    }
}
