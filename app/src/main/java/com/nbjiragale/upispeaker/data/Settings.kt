package com.nbjiragale.upispeaker.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple SharedPreferences-backed user-settings store.
 *
 * Kept intentionally tiny for the scaffold. DataStore migration is on the
 * roadmap (Milestone 3) once we add proper Compose-friendly observables.
 */
class Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("upi_speaker", Context.MODE_PRIVATE)

    var listeningEnabled: Boolean
        get() = prefs.getBoolean(KEY_LISTENING, false)
        set(value) = prefs.edit().putBoolean(KEY_LISTENING, value).apply()

    var mutedUntilMs: Long
        get() = prefs.getLong(KEY_MUTED_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_MUTED_UNTIL, value).apply()

    var announceDebits: Boolean
        get() = prefs.getBoolean(KEY_ANNOUNCE_DEBITS, false)
        set(value) = prefs.edit().putBoolean(KEY_ANNOUNCE_DEBITS, value).apply()

    var preferredLocaleTag: String
        get() = prefs.getString(KEY_LOCALE, "en-IN") ?: "en-IN"
        set(value) = prefs.edit().putString(KEY_LOCALE, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var listenerLastBoundAtMs: Long
        get() = prefs.getLong(KEY_LISTENER_BOUND_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LISTENER_BOUND_AT, value).apply()

    fun isMuted(): Boolean = mutedUntilMs > System.currentTimeMillis()

    private companion object {
        const val KEY_LISTENING = "listening_enabled"
        const val KEY_MUTED_UNTIL = "muted_until_ms"
        const val KEY_ANNOUNCE_DEBITS = "announce_debits"
        const val KEY_LOCALE = "preferred_locale"
        const val KEY_ONBOARDING_DONE = "onboarding_completed"
        const val KEY_LISTENER_BOUND_AT = "listener_last_bound_at"
    }
}
