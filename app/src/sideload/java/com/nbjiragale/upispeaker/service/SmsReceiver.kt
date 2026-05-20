package com.nbjiragale.upispeaker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.data.TransactionLog
import com.nbjiragale.upispeaker.parser.BankSmsRegex

/**
 * Path B — sideload-only secondary trigger.  Listens for the system
 * SMS_RECEIVED broadcast and forwards parsed bank-credit messages to
 * SpeakerForegroundService.
 *
 * This class is only compiled into the `sideload` build flavour.
 * The play flavour has no source set for it and the manifest stub in
 * app/src/sideload/AndroidManifest.xml is what registers it with the
 * system.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val settings = Settings(context)
        if (!settings.listeningEnabled || settings.isMuted()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // SMS can be split across multiple PDUs; concatenate body per-sender.
        val grouped = messages.groupBy { it.displayOriginatingAddress ?: "" }
        val log = TransactionLog(context)

        grouped.forEach { (sender, parts) ->
            val body = parts.joinToString("") { it.messageBody.orEmpty() }
            val tx = BankSmsRegex.parse(sender, body, System.currentTimeMillis())
            if (tx == null) {
                Log.v(TAG, "No transaction matched in SMS from $sender")
                return@forEach
            }
            if (!log.insertIfNew(tx)) return@forEach

            val rupees = tx.amountPaise / 100L
            val words = com.nbjiragale.upispeaker.tts.NumberToWords.englishIndian(rupees)
            SpeakerForegroundService.announce(context, "$words rupees received")
        }
    }

    private companion object { const val TAG = "SmsReceiver" }
}
