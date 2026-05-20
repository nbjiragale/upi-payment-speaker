package com.nbjiragale.upispeaker.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.parser.Transaction
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Serial queue of pending announcements.
 *
 * Drives the TtsEngine one utterance at a time and manages transient audio
 * focus so the announcement plays even when the user is on a call or in a
 * music app — without permanently stealing the audio output.
 *
 * Supports:
 *   * Single-locale or dual-locale (speak in primary then secondary)
 *   * Alarm stream at maximum volume for reliable merchant use
 *   * Kannada number-to-words via NumberToWordsKannada
 */
class AnnouncementQueue(
    private val context: Context,
    private val tts: TtsEngine
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val seq = AtomicLong(0L)
    private var focusRequest: AudioFocusRequest? = null

    fun enqueue(tx: Transaction) {
        val settings = Settings(context)
        val primaryTag = settings.preferredLocaleTag
        val primaryUtterance = utteranceFor(tx, primaryTag)
        val id = "ann-${seq.incrementAndGet()}"
        Log.d(TAG, "Enqueuing announcement '$primaryUtterance' as $id")

        tts.maximiseVolume()
        requestFocus()
        tts.ensureReady {
            tts.setLocale(localeFromTag(primaryTag))
            tts.speak(primaryUtterance, id) { _ ->
                if (settings.ttsMode == Settings.TtsMode.DUAL) {
                    val secondTag = settings.secondLocaleTag
                    if (secondTag.isNotEmpty() && secondTag != primaryTag) {
                        val secondUtterance = utteranceFor(tx, secondTag)
                        val id2 = "ann2-${seq.incrementAndGet()}"
                        tts.setLocale(localeFromTag(secondTag))
                        tts.speak(secondUtterance, id2) { _ ->
                            tts.setLocale(localeFromTag(primaryTag))
                            abandonFocus()
                        }
                    } else {
                        abandonFocus()
                    }
                } else {
                    abandonFocus()
                }
            }
        }
    }

    fun enqueueText(text: String) {
        val id = "raw-${seq.incrementAndGet()}"
        tts.maximiseVolume()
        requestFocus()
        tts.ensureReady {
            tts.speak(text, id) { _ -> abandonFocus() }
        }
    }

    private fun utteranceFor(tx: Transaction, localeTag: String): String {
        val rupees = tx.amountPaise / 100L

        val words: String
        val currencyWord: String
        val verb: String

        when (localeTag) {
            "kn-IN" -> {
                words = NumberToWordsKannada.kannadaIndian(rupees)
                currencyWord = "ರೂಪಾಯಿ"
                verb = when (tx.direction) {
                    Transaction.Direction.CREDIT -> "ಬಂದಿದೆ"
                    Transaction.Direction.DEBIT -> "ಕಳುಹಿಸಲಾಗಿದೆ"
                }
            }
            else -> {
                words = NumberToWords.englishIndian(rupees)
                currencyWord = "rupees"
                verb = when (tx.direction) {
                    Transaction.Direction.CREDIT -> "received"
                    Transaction.Direction.DEBIT -> "paid"
                }
            }
        }

        return "$words $currencyWord $verb"
    }

    private fun localeFromTag(tag: String): Locale {
        val parts = tag.split("-")
        return if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(parts[0])
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
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
