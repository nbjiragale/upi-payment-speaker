package com.nbjiragale.upispeaker.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Per-OEM deep links into the system settings screens that govern
 * background-execution survival.
 *
 * The component-name strings come from the attached
 * "Android Background Execution & OEM Restrictions" spec (page 9). Every
 * launch attempts the explicit component first and falls back to the
 * generic application-details screen if the component is missing
 * (which happens on newer ROM versions, e.g. Origin OS 4).
 */
object DeepLinks {

    private const val TAG = "OemDeepLinks"

    // --- Vivo (Funtouch / Origin OS) ---

    fun openVivoAutoStart(context: Context) =
        tryStart(context,
            Intent().setComponent(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            )
        )

    fun openVivoBackgroundPowerWhitelist(context: Context) =
        tryStart(context,
            Intent().setComponent(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            )
        )

    // --- Xiaomi (HyperOS / MIUI) ---

    fun openXiaomiAutoStart(context: Context) =
        tryStart(context,
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            )
        )

    fun openXiaomiAppPerms(context: Context) =
        tryStart(context,
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", context.packageName)
            }
        )

    // --- Oppo / Realme (ColorOS / RealmeUI) ---

    fun openOppoAutoLaunch(context: Context) =
        tryStart(context,
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            )
        )

    // --- Samsung (OneUI) ---

    fun openSamsungNeverSleepingApps(context: Context) =
        tryStart(context,
            Intent("com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY").apply {
                setPackage("com.samsung.android.lool")
                putExtra("activity_type", 2)
            }
        )

    // --- Generic fallbacks ---

    fun openNotificationListenerSettings(context: Context) =
        tryStart(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openBatteryOptimizationSettings(context: Context) =
        tryStart(context,
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
        )

    fun openExactAlarmSettings(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            tryStart(context,
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${context.packageName}"))
            )
        } else {
            openAppDetails(context)
        }
    }

    fun openAppDetails(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details", e)
        }
    }

    private fun tryStart(context: Context, intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Component missing for ${intent.component ?: intent.action}, falling back to app details.")
            openAppDetails(context)
        }
    }
}
