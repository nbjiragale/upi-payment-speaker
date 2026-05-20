package com.nbjiragale.upispeaker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.databinding.ActivityMainBinding
import com.nbjiragale.upispeaker.oem.RestrictionDetector
import com.nbjiragale.upispeaker.service.SpeakerForegroundService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: Settings
    private lateinit var detector: RestrictionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = Settings(this)
        detector = RestrictionDetector(this)

        if (!settings.onboardingCompleted) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding.btnToggle.setOnClickListener { toggleListening() }
        binding.btnTest.setOnClickListener {
            if (!settings.listeningEnabled) toggleListening()
            SpeakerForegroundService.test(this)
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun toggleListening() {
        val newState = !settings.listeningEnabled
        settings.listeningEnabled = newState
        if (newState) {
            SpeakerForegroundService.start(this)
        } else {
            SpeakerForegroundService.stop(this)
        }
        refreshUi()
    }

    private fun refreshUi() {
        val state = detector.snapshot()

        val statusText = when {
            !settings.listeningEnabled -> getString(R.string.status_setup_needed)
            settings.isMuted() -> getString(R.string.status_paused)
            else -> getString(R.string.status_listening)
        }
        binding.tvStatus.text = statusText

        val warning: String? = when {
            !state.notificationListenerEnabled -> getString(R.string.warn_listener_unbound)
            !state.exactAlarmGranted -> getString(R.string.warn_exact_alarm_revoked)
            !state.ignoringBatteryOptimisations -> getString(R.string.warn_battery_opt_re_enabled)
            state.isRestrictedBucket -> getString(R.string.warn_restricted_bucket)
            else -> null
        }
        if (warning == null) {
            binding.tvWarning.visibility = android.view.View.GONE
        } else {
            binding.tvWarning.visibility = android.view.View.VISIBLE
            binding.tvWarning.text = warning
        }

        binding.btnToggle.text = if (settings.listeningEnabled) {
            getString(R.string.stop_listening)
        } else {
            getString(R.string.start_listening)
        }
    }
}
