package com.nbjiragale.upispeaker.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Schedules the watchdog heartbeat (plan §5.3).
 *
 * Uses AlarmManager.setAlarmClock() to bypass Doze — this is the same
 * mechanism the PDF recommends for highest reliability. setAlarmClock()
 * forces the platform to temporarily exit Doze just before the alarm fires.
 *
 * Falls back to setAndAllowWhileIdle() if exact-alarm permission has been
 * revoked.
 *
 * Interval: 30 minutes — comfortably above the 15-minute throttle floor
 * documented in the PDF.
 */
class WatchdogScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNext() {
        val triggerAt = System.currentTimeMillis() + INTERVAL_MS
        val pi = buildPendingIntent()

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        try {
            if (canExact) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, pi),
                    pi
                )
                Log.d(TAG, "Watchdog scheduled (setAlarmClock) at $triggerAt")
            } else {
                fallback(triggerAt, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back", e)
            fallback(triggerAt, pi)
        }
    }

    fun cancel() {
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, WatchdogReceiver::class.java),
            pendingIntentFlags(noCreate = true)
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d(TAG, "Watchdog cancelled")
        }
    }

    private fun fallback(triggerAt: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        Log.d(TAG, "Watchdog scheduled (setAndAllowWhileIdle) at $triggerAt")
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, WatchdogReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, pendingIntentFlags(noCreate = false)
        )
    }

    private fun pendingIntentFlags(noCreate: Boolean): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        if (noCreate) flags = flags or PendingIntent.FLAG_NO_CREATE
        return flags
    }

    private companion object {
        const val TAG = "WatchdogScheduler"
        const val REQUEST_CODE = 7001
        const val INTERVAL_MS = 30L * 60L * 1000L // 30 minutes
    }
}
