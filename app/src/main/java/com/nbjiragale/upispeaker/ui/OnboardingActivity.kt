package com.nbjiragale.upispeaker.ui

import android.Manifest
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.databinding.ActivityOnboardingBinding
import com.nbjiragale.upispeaker.oem.DeepLinks
import com.nbjiragale.upispeaker.oem.Oem
import com.nbjiragale.upispeaker.oem.OemDetector
import com.nbjiragale.upispeaker.oem.RestrictionDetector
import com.nbjiragale.upispeaker.service.SpeakerForegroundService

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

        // Update step pill
        binding.tvStepNumber.text = (i + 1).toString()
        binding.tvStepIndicator.text = getString(R.string.step_of, i + 1, steps.size)

        val (title, body, buttonText, showPrivacy, action) = renderStep(step)
        binding.tvTitle.text = title
        binding.tvBody.text = body
        binding.btnPrimary.text = buttonText
        binding.btnPrimary.setOnClickListener { action() }

        // Privacy note
        binding.privacyNote.visibility = if (showPrivacy) View.VISIBLE else View.GONE

        // Build progress dots
        buildProgressDots(i, steps.size)

        // Build illustration
        buildIllustration(step)
    }

    private data class StepUi(
        val title: String,
        val body: String,
        val buttonText: String,
        val showPrivacy: Boolean,
        val onClick: () -> Unit
    )

    private fun renderStep(step: Step): StepUi = when (step) {
        Step.Welcome -> StepUi(
            getString(R.string.onboarding_welcome_title),
            getString(R.string.onboarding_welcome_body),
            getString(R.string.grant_permission),
            false
        ) { advance() }

        Step.PostNotifications -> StepUi(
            getString(R.string.onboarding_notif_post_title),
            getString(R.string.onboarding_notif_post_body),
            getString(R.string.grant_permission),
            false
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else advance()
        }

        Step.NotificationListener -> StepUi(
            getString(R.string.onboarding_notif_listener_title),
            getString(R.string.onboarding_notif_listener_body),
            getString(R.string.grant_permission),
            true
        ) { DeepLinks.openNotificationListenerSettings(this) }

        Step.ExactAlarm -> StepUi(
            getString(R.string.onboarding_exact_alarm_title),
            getString(R.string.onboarding_exact_alarm_body),
            getString(R.string.open_oem_setting),
            false
        ) { DeepLinks.openExactAlarmSettings(this) }

        Step.BatteryOptimisation -> StepUi(
            getString(R.string.onboarding_battery_title),
            getString(R.string.onboarding_battery_body),
            getString(R.string.open_oem_setting),
            false
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
                getString(R.string.open_oem_setting),
                false
            ) { openOemSettings(step.oem) }
        }

        Step.TestAnnouncement -> StepUi(
            getString(R.string.onboarding_test_title),
            getString(R.string.onboarding_test_body),
            getString(R.string.finish),
            false
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

    private fun buildProgressDots(current: Int, total: Int) {
        val container = binding.progressDots
        container.removeAllViews()
        val dp = resources.displayMetrics.density

        for (i in 0 until total) {
            val dot = View(this).apply {
                val w: Int
                val h = (6 * dp).toInt()
                val bgRes: Int
                when {
                    i < current -> {
                        w = (6 * dp).toInt()
                        bgRes = R.drawable.bg_dot_progress_done
                    }
                    i == current -> {
                        w = (24 * dp).toInt()
                        bgRes = R.drawable.bg_dot_progress_active
                    }
                    else -> {
                        w = (6 * dp).toInt()
                        bgRes = R.drawable.bg_dot_progress_pending
                    }
                }
                val params = LinearLayout.LayoutParams(w, h).apply {
                    marginStart = (3 * dp).toInt()
                    marginEnd = (3 * dp).toInt()
                }
                layoutParams = params
                setBackgroundResource(bgRes)
            }
            container.addView(dot)
        }
    }

    private fun buildIllustration(step: Step) {
        val container = binding.illustrationContainer
        container.removeAllViews()
        val dp = resources.displayMetrics.density

        // Simple notification card illustration
        val cardStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Main notification card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val w = (240 * dp).toInt()
            val h = (64 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(w, h).apply {
                gravity = Gravity.CENTER
            }
            val bg = GradientDrawable().apply {
                cornerRadius = 14 * dp
                setColor(ContextCompat.getColor(this@OnboardingActivity, R.color.surface))
                setStroke((1 * dp).toInt(), ContextCompat.getColor(this@OnboardingActivity, R.color.border))
            }
            background = bg
            setPadding((14 * dp).toInt(), (10 * dp).toInt(), (14 * dp).toInt(), (10 * dp).toInt())
        }

        // Provider badge
        val badge = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
            val bg = GradientDrawable().apply {
                cornerRadius = 9 * dp
                setColor(0xFF5F259F.toInt())
            }
            background = bg
        }
        val badgeLetter = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = "P"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
        }
        badge.addView(badgeLetter)
        card.addView(badge)

        // Text column
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding((10 * dp).toInt(), 0, 0, 0)
        }
        textCol.addView(TextView(this).apply {
            text = "PhonePe"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@OnboardingActivity, R.color.fg))
        })
        textCol.addView(TextView(this).apply {
            text = "Received ₹250"
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@OnboardingActivity, R.color.fg_muted))
        })
        card.addView(textCol)

        cardStack.addView(card)
        container.addView(cardStack)
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
