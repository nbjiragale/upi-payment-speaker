package com.nbjiragale.upispeaker.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * A thin wrapper around android.speech.tts.TextToSpeech that:
 *   * lazy-initialises on first use,
 *   * pre-warms with a no-op utterance (engines have a cold-start cost),
 *   * announces over STREAM_ALARM so it plays at maximum volume and is
 *     never ducked or muted,
 *   * supports locale switching for multi-language announcements,
 *   * exposes a coroutine-friendly callback hook so AnnouncementQueue can
 *     speak sequentially.
 */
class TtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var initialised = false
    private val pending = ArrayDeque<Utterance>()
    private var currentLocale: Locale = Locale("en", "IN")

    private data class Utterance(val text: String, val id: String, val onDone: (Boolean) -> Unit)

    /** Initialise.  Safe to call repeatedly. */
    fun ensureReady(onReady: (() -> Unit)? = null) {
        if (initialised) {
            onReady?.invoke()
            return
        }
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = currentLocale
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                tts?.setOnUtteranceProgressListener(progressListener)
                initialised = true
                Log.d(TAG, "TTS ready; flushing ${pending.size} pending utterance(s)")
                drainPending()
                preWarm()
                onReady?.invoke()
            } else {
                Log.e(TAG, "TTS init failed: status=$status")
                onReady?.invoke()
            }
        }
    }

    fun speak(text: String, utteranceId: String, onDone: (Boolean) -> Unit) {
        if (!initialised) {
            pending.addLast(Utterance(text, utteranceId, onDone))
            ensureReady()
            return
        }
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
        }
        progressByUtterance[utteranceId] = onDone
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    /**
     * Change the TTS locale for subsequent utterances.
     * Returns true if the locale was set successfully.
     */
    fun setLocale(locale: Locale): Boolean {
        currentLocale = locale
        if (!initialised) return true
        val result = tts?.setLanguage(locale)
        return result == TextToSpeech.LANG_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    /**
     * Maximise the alarm stream volume before speaking so the announcement
     * is always audible.
     */
    fun maximiseVolume() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        initialised = false
        progressByUtterance.clear()
        pending.clear()
    }

    private fun preWarm() {
        speak(" ", "warmup-${System.currentTimeMillis()}") {}
    }

    private fun drainPending() {
        while (pending.isNotEmpty()) {
            val u = pending.removeFirst()
            speak(u.text, u.id, u.onDone)
        }
    }

    private val progressByUtterance = mutableMapOf<String, (Boolean) -> Unit>()

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}
        override fun onDone(utteranceId: String) {
            progressByUtterance.remove(utteranceId)?.invoke(true)
        }
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override fun onError(utteranceId: String) {
            progressByUtterance.remove(utteranceId)?.invoke(false)
        }
        override fun onError(utteranceId: String, errorCode: Int) {
            progressByUtterance.remove(utteranceId)?.invoke(false)
        }
    }

    private companion object {
        const val TAG = "TtsEngine"
    }
}
