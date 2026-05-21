package com.nbjiragale.upispeaker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.databinding.ActivityMainBinding
import com.nbjiragale.upispeaker.util.LocaleHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: Settings

    private val homeFragment by lazy { HomeFragment() }
    private val historyFragment by lazy { HistoryFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    override fun attachBaseContext(newBase: Context) {
        val tag = Settings(newBase).appLocaleTag
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = Settings(this)

        if (!settings.onboardingCompleted) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setupBottomNav()

        if (savedInstanceState == null) {
            showFragment(homeFragment)
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { showFragment(homeFragment); true }
                R.id.nav_history -> { showFragment(historyFragment); true }
                R.id.nav_settings -> { showFragment(settingsFragment); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navigateToHistory() {
        binding.bottomNav.selectedItemId = R.id.nav_history
    }
}
