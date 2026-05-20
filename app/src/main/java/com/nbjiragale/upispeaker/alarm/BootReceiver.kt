package com.nbjiragale.upispeaker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.service.SpeakerForegroundService

/**
 * Self-heals after system events that strip our state:
 *   * BOOT_COMPLETED        — device reboot
 *   * MY_PACKAGE_REPLACED   — app updated
 *   * TIME_SET / TIMEZONE_CHANGED — clock change (prevents watchdog drift)
 *
 * On Vivo / Xiaomi / Oppo this only fires if the user has granted
 * Autostart privileges — the onboarding wizard is the only way to make
 * sure that happens. See plan §5.7.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        Log.d(TAG, "Received system broadcast: ${intent.action}")

        val settings = Settings(context)
        if (!settings.listeningEnabled) {
            Log.d(TAG, "Listening disabled — skipping self-heal.")
            return
        }

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                try {
                    SpeakerForegroundService.start(context)
                    val scheduler = WatchdogScheduler(context)
                    scheduler.cancel()
                    scheduler.scheduleNext()
                    Log.i(TAG, "Self-heal complete for action='${intent.action}'.")
                } catch (e: Exception) {
                    Log.e(TAG, "Self-heal failed", e)
                }
            }
        }
    }

    private companion object { const val TAG = "BootReceiver" }
}
