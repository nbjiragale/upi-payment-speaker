package com.nbjiragale.upispeaker.alarm

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.util.Log
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.oem.RestrictionDetector
import com.nbjiragale.upispeaker.service.SpeakerForegroundService
import com.nbjiragale.upispeaker.service.UpiNotificationListener

/**
 * Watchdog heartbeat fired by AlarmManager every ~30 minutes.
 *
 * Responsibilities (plan §5.3):
 *   1. Verify the SpeakerForegroundService is running; restart if not.
 *   2. Verify the NotificationListenerService is still bound; trigger the
 *      MIUI/Funtouch/ColorOS rebind dance if not.
 *   3. Re-schedule the next heartbeat.
 *
 * Wake-lock duration is bounded (10 s max) — we never hold a lock past the
 * end of onReceive.
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wl.acquire(WAKELOCK_TIMEOUT_MS)

        try {
            val settings = Settings(context)
            if (!settings.listeningEnabled) {
                Log.d(TAG, "Listening disabled, watchdog standing down.")
                return
            }

            if (!isServiceRunning(context)) {
                Log.w(TAG, "Foreground service not running, restarting.")
                SpeakerForegroundService.start(context)
            }

            val detector = RestrictionDetector(context)
            if (!detector.isNotificationListenerEnabled()) {
                Log.w(TAG, "Notification listener not enabled — user intervention required.")
            } else {
                // Rebind dance — proven workaround for MIUI/Funtouch/ColorOS
                // unbinding the listener after force-stop or app update.
                rebindNotificationListener(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Watchdog failed", e)
        } finally {
            WatchdogScheduler(context).scheduleNext()
            if (wl.isHeld) wl.release()
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // getRunningServices is deprecated for foreign apps but still works
        // for callers querying their OWN services, which is what we do here.
        val targetClass = SpeakerForegroundService::class.java.name
        return am.getRunningServices(Int.MAX_VALUE).any { it.service.className == targetClass }
    }

    private fun rebindNotificationListener(context: Context) {
        val component = ComponentName(context, UpiNotificationListener::class.java)
        val pm = context.packageManager

        // Toggle component-enabled state to nudge the system to rebind.
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                NotificationListenerService.requestRebind(component)
                Log.d(TAG, "Requested rebind of UpiNotificationListener")
            } catch (e: Exception) {
                Log.w(TAG, "requestRebind threw", e)
            }
        }
    }

    private companion object {
        const val TAG = "WatchdogReceiver"
        const val WAKELOCK_TAG = "UpiSpeaker:Watchdog"
        const val WAKELOCK_TIMEOUT_MS = 10_000L
    }
}
