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

    /** Second locale for dual-TTS mode. Empty string means disabled. */
    var secondLocaleTag: String
        get() = prefs.getString(KEY_SECOND_LOCALE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECOND_LOCALE, value).apply()

    /**
     * TTS mode:
     *   SINGLE  – speak in the primary locale only.
     *   DUAL    – speak in the primary locale, then repeat in the second locale.
     */
    var ttsMode: TtsMode
        get() = TtsMode.fromKey(prefs.getString(KEY_TTS_MODE, TtsMode.SINGLE.key) ?: TtsMode.SINGLE.key)
        set(value) = prefs.edit().putString(KEY_TTS_MODE, value.key).apply()

    /** Whether to show a popup confirmation when a payment is announced. */
    var showPaymentPopup: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POPUP, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_POPUP, value).apply()

    /** App UI language tag. Empty or "system" means follow system locale. */
    var appLocaleTag: String
        get() = prefs.getString(KEY_APP_LOCALE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_LOCALE, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var listenerLastBoundAtMs: Long
        get() = prefs.getLong(KEY_LISTENER_BOUND_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LISTENER_BOUND_AT, value).apply()

    fun isMuted(): Boolean = mutedUntilMs > System.currentTimeMillis()

    enum class TtsMode(val key: String) {
        SINGLE("single"),
        DUAL("dual");

        companion object {
            fun fromKey(key: String): TtsMode = entries.firstOrNull { it.key == key } ?: SINGLE
        }
    }

    /** Available locale choices for the user. */
    enum class TtsLocale(val tag: String, val displayName: String) {
        ENGLISH_IN("en-IN", "English (India)"),
        KANNADA("kn-IN", "ಕನ್ನಡ (Kannada)");

        companion object {
            fun fromTag(tag: String): TtsLocale = entries.firstOrNull { it.tag == tag } ?: ENGLISH_IN
        }
    }

    /** Available app UI language choices. */
    enum class AppLanguage(val tag: String, val displayName: String) {
        SYSTEM("", "System default"),
        ENGLISH("en", "English"),
        KANNADA("kn", "ಕನ್ನಡ");

        companion object {
            fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
        }
    }

    private companion object {
        const val KEY_LISTENING = "listening_enabled"
        const val KEY_MUTED_UNTIL = "muted_until_ms"
        const val KEY_ANNOUNCE_DEBITS = "announce_debits"
        const val KEY_LOCALE = "preferred_locale"
        const val KEY_SECOND_LOCALE = "second_locale"
        const val KEY_TTS_MODE = "tts_mode"
        const val KEY_SHOW_POPUP = "show_payment_popup"
        const val KEY_APP_LOCALE = "app_locale"
        const val KEY_ONBOARDING_DONE = "onboarding_completed"
        const val KEY_LISTENER_BOUND_AT = "listener_last_bound_at"
    }
}
