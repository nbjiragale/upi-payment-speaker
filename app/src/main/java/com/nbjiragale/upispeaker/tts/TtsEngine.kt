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
 *   * announces over STREAM_NOTIFICATION so it doesn't get ducked / muted
 *     with music,
 *   * exposes a coroutine-friendly callback hook so AnnouncementQueue can
 *     speak sequentially.
 */
class TtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var initialised = false
    private val pending = ArrayDeque<Utterance>()

    private data class Utterance(val text: String, val id: String, val onDone: (Boolean) -> Unit)

    /** Initialise.  Safe to call repeatedly. */
    fun ensureReady(onReady: (() -> Unit)? = null) {
        if (initialised) {
            onReady?.invoke()
            return
        }
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN")
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
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
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
        }
        progressByUtterance[utteranceId] = onDone
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        initialised = false
        progressByUtterance.clear()
        pending.clear()
    }

    private fun preWarm() {
        // A near-empty utterance forces the engine to allocate native
        // resources up-front so the first real announcement is snappy.
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
