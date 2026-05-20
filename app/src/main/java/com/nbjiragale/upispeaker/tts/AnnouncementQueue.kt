package com.nbjiragale.upispeaker.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.nbjiragale.upispeaker.parser.Transaction
import java.util.concurrent.atomic.AtomicLong

/**
 * Serial queue of pending announcements.
 *
 * Drives the TtsEngine one utterance at a time and manages transient audio
 * focus so the announcement plays even when the user is on a call or in a
 * music app — without permanently stealing the audio output.
 */
class AnnouncementQueue(
    private val context: Context,
    private val tts: TtsEngine
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val seq = AtomicLong(0L)

    private var focusRequest: AudioFocusRequest? = null

    fun enqueue(tx: Transaction) {
        val utterance = utteranceFor(tx)
        val id = "ann-${seq.incrementAndGet()}"
        Log.d(TAG, "Enqueuing announcement '$utterance' as $id")
        requestFocus()
        tts.ensureReady {
            tts.speak(utterance, id) { _ -> abandonFocus() }
        }
    }

    fun enqueueText(text: String) {
        val id = "raw-${seq.incrementAndGet()}"
        requestFocus()
        tts.ensureReady {
            tts.speak(text, id) { _ -> abandonFocus() }
        }
    }

    private fun utteranceFor(tx: Transaction): String {
        val rupees = tx.amountPaise / 100L
        val words = NumberToWords.englishIndian(rupees)
        val verb = when (tx.direction) {
            Transaction.Direction.CREDIT -> "received"
            Transaction.Direction.DEBIT -> "paid"
        }
        val who = tx.payerOrPayee?.takeIf { it.isNotBlank() }
        val whoPart = when {
            who == null -> ""
            tx.direction == Transaction.Direction.CREDIT -> " from $who"
            else -> " to $who"
        }
        return "$words rupees $verb$whoPart"
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { /* TTS itself reacts to focus loss */ }
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private companion object {
        const val TAG = "AnnouncementQueue"
    }
}
