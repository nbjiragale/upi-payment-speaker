package com.nbjiragale.upispeaker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.data.TransactionLog
import com.nbjiragale.upispeaker.parser.Providers

/**
 * Path A — primary trigger.
 *
 * The system binds this service once the user enables "Notification access".
 * onNotificationPosted fires for every notification posted by every other
 * app; we filter to known UPI / bank packages and hand the parsed
 * Transaction off to the foreground service.
 *
 * onListenerConnected / onListenerDisconnected write a heartbeat to
 * Settings so the WatchdogReceiver can detect a stale binding.
 */
class UpiNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener connected")
        Settings(this).listenerLastBoundAtMs = System.currentTimeMillis()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Listener disconnected — requesting rebind")
        Settings(this).listenerLastBoundAtMs = 0L
        // Best-effort rebind; the watchdog will retry.
        try {
            requestRebind(android.content.ComponentName(this, this::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "requestRebind threw", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (!Providers.isKnownUpiSource(pkg)) return

        val settings = Settings(this)
        if (!settings.listeningEnabled || settings.isMuted()) return

        val extras = sbn.notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        Log.d(TAG, "Notification from $pkg: title='$title' text='$text'")

        val tx = Providers.extractorFor(pkg)
            .extract(pkg, title, text, bigText, sbn.postTime) ?: return

        val log = TransactionLog(this)
        if (!log.insertIfNew(tx)) {
            Log.d(TAG, "Duplicate transaction, ignoring: ${tx.dedupeKey()}")
            return
        }
        SpeakerForegroundService.announce(this, utteranceFor(tx))
    }

    private fun utteranceFor(tx: com.nbjiragale.upispeaker.parser.Transaction): String {
        // The foreground service has the full TTS pipeline; here we just
        // form the raw text and let it speak.  In M2 this moves into a
        // shared helper.
        val rupees = tx.amountPaise / 100L
        val words = com.nbjiragale.upispeaker.tts.NumberToWords.englishIndian(rupees)
        return "$words rupees received"
    }

    private companion object { const val TAG = "UpiNotifListener" }
}
