package com.nbjiragale.upispeaker.oem

import android.os.Build

/**
 * Identifies the OEM so the onboarding wizard can show the correct
 * background-restriction-bypass instructions.
 *
 * Detection uses Build.MANUFACTURER first, then Build.BRAND, both
 * lower-cased. Vivo's two skins (Funtouch OS and Origin OS) are reported
 * separately because their settings deep-links differ slightly on newer
 * Origin OS releases.
 */
enum class Oem(val displayName: String) {
    VIVO_FUNTOUCH("Vivo (Funtouch OS)"),
    VIVO_ORIGIN("Vivo (Origin OS)"),
    XIAOMI("Xiaomi"),
    OPPO("Oppo"),
    REALME("Realme"),
    ONEPLUS("OnePlus"),
    SAMSUNG("Samsung"),
    HUAWEI("Huawei"),
    HONOR("Honor"),
    GENERIC("Android");
}

object OemDetector {

    fun detect(): Oem {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        val displayId = Build.DISPLAY.orEmpty().lowercase()

        return when {
            manufacturer.contains("vivo") || brand.contains("vivo") || brand.contains("iqoo") ->
                if (displayId.contains("origin") || Build.VERSION.SDK_INT >= 33) {
                    // Origin OS is the post-Funtouch skin on newer Vivo devices.
                    Oem.VIVO_ORIGIN
                } else {
                    Oem.VIVO_FUNTOUCH
                }
            manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ->
                Oem.XIAOMI
            manufacturer.contains("oppo") -> Oem.OPPO
            manufacturer.contains("realme") -> Oem.REALME
            manufacturer.contains("oneplus") -> Oem.ONEPLUS
            manufacturer.contains("samsung") -> Oem.SAMSUNG
            manufacturer.contains("huawei") -> Oem.HUAWEI
            manufacturer.contains("honor") -> Oem.HONOR
            else -> Oem.GENERIC
        }
    }
}
