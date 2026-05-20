package com.nbjiragale.upispeaker.oem

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.nbjiragale.upispeaker.service.UpiNotificationListener

/**
 * Detects the current background-execution restriction state.
 *
 * Per the spec: silently re-enabled restrictions are a real concern, so
 * this is invoked from:
 *   1. MainActivity.onResume()
 *   2. SpeakerForegroundService.onCreate()
 *   3. WatchdogReceiver (every 30 minutes)
 *
 * Any failing check produces a user-visible banner + FGS notification text
 * update telling the user to re-grant the dropped permission.
 */
class RestrictionDetector(private val context: Context) {

    data class State(
        val exactAlarmGranted: Boolean,
        val ignoringBatteryOptimisations: Boolean,
        val postNotificationsGranted: Boolean,
        val notificationListenerEnabled: Boolean,
        val standbyBucket: Int,
        val isRestrictedBucket: Boolean
    ) {
        val isHealthy: Boolean
            get() = exactAlarmGranted &&
                    ignoringBatteryOptimisations &&
                    postNotificationsGranted &&
                    notificationListenerEnabled &&
                    !isRestrictedBucket
    }

    fun snapshot(): State {
        val bucket = appStandbyBucket()
        return State(
            exactAlarmGranted = canScheduleExactAlarms(),
            ignoringBatteryOptimisations = isIgnoringBatteryOptimisations(),
            postNotificationsGranted = isPostNotificationsGranted(),
            notificationListenerEnabled = isNotificationListenerEnabled(),
            standbyBucket = bucket,
            // STANDBY_BUCKET_RESTRICTED (= 45) is API 30+ but the value is
            // stable; we explicitly guard the SDK check before relying on it.
            isRestrictedBucket = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    bucket == UsageStatsManager.STANDBY_BUCKET_RESTRICTED
        )
    }

    fun canScheduleExactAlarms(): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
    }

    fun isIgnoringBatteryOptimisations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    fun isPostNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ).orEmpty()
        val expected = "${context.packageName}/${UpiNotificationListener::class.java.name}"
        // The setting stores ComponentName.flattenToString() entries separated by ':'.
        return flat.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun appStandbyBucket(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usm.appStandbyBucket
    }
}
