package com.nbjiragale.upispeaker.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.databinding.ActivityOnboardingBinding
import com.nbjiragale.upispeaker.oem.DeepLinks
import com.nbjiragale.upispeaker.oem.Oem
import com.nbjiragale.upispeaker.oem.OemDetector
import com.nbjiragale.upispeaker.oem.RestrictionDetector
import com.nbjiragale.upispeaker.service.SpeakerForegroundService

/**
 * Step-wise wizard following the pattern from the spec page 9–10 and
 * plan §9.  Only requests permissions that are not yet granted.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var settings: Settings
    private lateinit var detector: RestrictionDetector

    private val steps = mutableListOf<Step>()
    private var index = 0

    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { advance() }

    private val openSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { advance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = Settings(this)
        detector = RestrictionDetector(this)
        buildSteps()
        if (steps.isEmpty()) finishOnboarding() else show(0)

        binding.btnSkip.setOnClickListener { advance() }
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate; the user may have completed the step out of band.
        buildSteps()
        if (steps.isEmpty()) finishOnboarding() else show(index.coerceAtMost(steps.lastIndex))
    }

    private fun buildSteps() {
        steps.clear()
        steps += Step.Welcome
        val state = detector.snapshot()
        if (!state.postNotificationsGranted) steps += Step.PostNotifications
        if (!state.notificationListenerEnabled) steps += Step.NotificationListener
        if (!state.exactAlarmGranted) steps += Step.ExactAlarm
        if (!state.ignoringBatteryOptimisations) steps += Step.BatteryOptimisation
        val oem = OemDetector.detect()
        if (oem != Oem.GENERIC) steps += Step.OemSpecific(oem)
        steps += Step.TestAnnouncement
    }

    private fun show(i: Int) {
        index = i
        val step = steps[i]
        binding.tvStepIndicator.text = getString(R.string.step_of, i + 1, steps.size)
        val (title, body, buttonText, action) = renderStep(step)
        binding.tvTitle.text = title
        binding.tvBody.text = body
        binding.btnPrimary.text = buttonText
        binding.btnPrimary.setOnClickListener { action() }
    }

    private data class StepUi(
        val title: String,
        val body: String,
        val buttonText: String,
        val onClick: () -> Unit
    )

    private fun renderStep(step: Step): StepUi = when (step) {
        Step.Welcome -> StepUi(
            getString(R.string.onboarding_welcome_title),
            getString(R.string.onboarding_welcome_body),
            getString(R.string.grant_permission)
        ) { advance() }

        Step.PostNotifications -> StepUi(
            getString(R.string.onboarding_notif_post_title),
            getString(R.string.onboarding_notif_post_body),
            getString(R.string.grant_permission)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else advance()
        }

        Step.NotificationListener -> StepUi(
            getString(R.string.onboarding_notif_listener_title),
            getString(R.string.onboarding_notif_listener_body),
            getString(R.string.open_oem_setting)
        ) { DeepLinks.openNotificationListenerSettings(this) }

        Step.ExactAlarm -> StepUi(
            getString(R.string.onboarding_exact_alarm_title),
            getString(R.string.onboarding_exact_alarm_body),
            getString(R.string.open_oem_setting)
        ) { DeepLinks.openExactAlarmSettings(this) }

        Step.BatteryOptimisation -> StepUi(
            getString(R.string.onboarding_battery_title),
            getString(R.string.onboarding_battery_body),
            getString(R.string.open_oem_setting)
        ) { DeepLinks.openBatteryOptimizationSettings(this) }

        is Step.OemSpecific -> {
            val body = if (step.oem == Oem.VIVO_FUNTOUCH || step.oem == Oem.VIVO_ORIGIN) {
                getString(R.string.onboarding_oem_body, step.oem.displayName) +
                    "\n\n" + getString(R.string.onboarding_vivo_extra_body)
            } else {
                getString(R.string.onboarding_oem_body, step.oem.displayName)
            }
            StepUi(
                getString(R.string.onboarding_oem_title, step.oem.displayName),
                body,
                getString(R.string.open_oem_setting)
            ) { openOemSettings(step.oem) }
        }

        Step.TestAnnouncement -> StepUi(
            getString(R.string.onboarding_test_title),
            getString(R.string.onboarding_test_body),
            getString(R.string.finish)
        ) {
            settings.listeningEnabled = true
            SpeakerForegroundService.start(this)
            SpeakerForegroundService.test(this)
            finishOnboarding()
        }
    }

    private fun openOemSettings(oem: Oem) {
        when (oem) {
            Oem.VIVO_FUNTOUCH, Oem.VIVO_ORIGIN -> {
                DeepLinks.openVivoAutoStart(this)
                DeepLinks.openVivoBackgroundPowerWhitelist(this)
            }
            Oem.XIAOMI -> DeepLinks.openXiaomiAutoStart(this)
            Oem.OPPO, Oem.REALME, Oem.ONEPLUS -> DeepLinks.openOppoAutoLaunch(this)
            Oem.SAMSUNG -> DeepLinks.openSamsungNeverSleepingApps(this)
            else -> DeepLinks.openAppDetails(this)
        }
    }

    private fun advance() {
        if (index >= steps.lastIndex) finishOnboarding() else show(index + 1)
    }

    private fun finishOnboarding() {
        settings.onboardingCompleted = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private sealed class Step {
        object Welcome : Step()
        object PostNotifications : Step()
        object NotificationListener : Step()
        object ExactAlarm : Step()
        object BatteryOptimisation : Step()
        data class OemSpecific(val oem: Oem) : Step()
        object TestAnnouncement : Step()
    }
}
