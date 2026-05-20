package com.nbjiragale.upispeaker.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.alarm.WatchdogScheduler
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.data.TransactionLog
import com.nbjiragale.upispeaker.parser.Transaction
import com.nbjiragale.upispeaker.tts.AnnouncementQueue
import com.nbjiragale.upispeaker.tts.TtsEngine
import com.nbjiragale.upispeaker.ui.MainActivity
import com.nbjiragale.upispeaker.util.AppNotificationChannels

/**
 * The one long-lived service.  Holds:
 *   * the TTS engine,
 *   * the announcement queue,
 *   * the in-memory dedupe table.
 *
 * Foreground type = mediaPlayback (plan §5.1).  No Android 15 timeout
 * applies because we are genuinely playing audible TTS.
 */
class SpeakerForegroundService : Service() {

    private lateinit var settings: Settings
    private lateinit var log: TransactionLog
    private lateinit var tts: TtsEngine
    private lateinit var queue: AnnouncementQueue

    override fun onCreate() {
        super.onCreate()
        AppNotificationChannels.ensureCreated(this)
        settings = Settings(this)
        log = TransactionLog(this)
        tts = TtsEngine(this)
        queue = AnnouncementQueue(this, tts)

        startForegroundCompat()
        tts.ensureReady()

        WatchdogScheduler(this).scheduleNext()
        Log.i(TAG, "SpeakerForegroundService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANNOUNCE -> {
                val text = intent.getStringExtra(EXTRA_TEXT)
                val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, -1L)
                if (!settings.isMuted()) {
                    if (amountPaise > 0) {
                        val tx = Transaction(
                            direction = Transaction.Direction.CREDIT,
                            amountPaise = amountPaise,
                            payerOrPayee = null,
                            referenceId = null,
                            sourcePackage = intent.getStringExtra(EXTRA_SOURCE_PKG) ?: "",
                            source = Transaction.Source.NOTIFICATION,
                            rawText = text ?: "",
                            timestampMs = System.currentTimeMillis()
                        )
                        queue.enqueue(tx)
                        showPopupIfEnabled(amountPaise)
                    } else if (!text.isNullOrBlank()) {
                        queue.enqueueText(text)
                    }
                }
            }
            ACTION_TEST -> queue.enqueueText(getString(R.string.tts_test_amount))
            ACTION_REFRESH_NOTIFICATION -> refreshNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        Log.i(TAG, "SpeakerForegroundService onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------

    private fun startForegroundCompat() {
        val notification = buildNotification(count = log.countToday(), warning = null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshNotification() {
        val notification = buildNotification(count = log.countToday(), warning = null)
        val nm = androidx.core.app.NotificationManagerCompat.from(this)
        try {
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "POST_NOTIFICATIONS not granted, cannot refresh.", e)
        }
    }

    private fun buildNotification(count: Int, warning: String?): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            pendingIntentFlags()
        )

        val text = warning ?: if (count == 0) {
            getString(R.string.fgs_text_idle)
        } else {
            getString(R.string.fgs_text, count)
        }

        return NotificationCompat.Builder(this, AppNotificationChannels.SPEAKER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.fgs_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showPopupIfEnabled(amountPaise: Long) {
        if (!settings.showPaymentPopup) return
        val popupIntent = Intent(this, com.nbjiragale.upispeaker.ui.PaymentPopupActivity::class.java)
            .putExtra(com.nbjiragale.upispeaker.ui.PaymentPopupActivity.EXTRA_AMOUNT_PAISE, amountPaise)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(popupIntent)
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

    companion object {
        private const val TAG = "SpeakerFGS"
        const val NOTIFICATION_ID = 9101
        const val ACTION_ANNOUNCE = "com.nbjiragale.upispeaker.ANNOUNCE"
        const val ACTION_TEST = "com.nbjiragale.upispeaker.TEST"
        const val ACTION_REFRESH_NOTIFICATION = "com.nbjiragale.upispeaker.REFRESH"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_AMOUNT_PAISE = "extra_amount_paise"
        const val EXTRA_SOURCE_PKG = "extra_source_pkg"

        fun start(context: Context) {
            val intent = Intent(context, SpeakerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpeakerForegroundService::class.java))
        }

        fun announce(context: Context, text: String) {
            val intent = Intent(context, SpeakerForegroundService::class.java)
                .setAction(ACTION_ANNOUNCE)
                .putExtra(EXTRA_TEXT, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun announceTransaction(context: Context, amountPaise: Long, sourcePackage: String, rawText: String) {
            val intent = Intent(context, SpeakerForegroundService::class.java)
                .setAction(ACTION_ANNOUNCE)
                .putExtra(EXTRA_AMOUNT_PAISE, amountPaise)
                .putExtra(EXTRA_SOURCE_PKG, sourcePackage)
                .putExtra(EXTRA_TEXT, rawText)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun test(context: Context) {
            val intent = Intent(context, SpeakerForegroundService::class.java).setAction(ACTION_TEST)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
