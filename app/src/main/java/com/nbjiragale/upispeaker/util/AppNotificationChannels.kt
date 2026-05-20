package com.nbjiragale.upispeaker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.nbjiragale.upispeaker.R

object AppNotificationChannels {

    const val SPEAKER = "channel_speaker"
    const val ALERTS = "channel_alerts"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                SPEAKER,
                context.getString(R.string.channel_speaker_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_speaker_description)
                setShowBadge(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                ALERTS,
                context.getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alerts_description)
                setShowBadge(true)
            }
        )
    }
}
