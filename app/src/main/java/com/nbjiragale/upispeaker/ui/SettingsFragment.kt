package com.nbjiragale.upispeaker.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.oem.RestrictionDetector

class SettingsFragment : Fragment() {

    private lateinit var settings: Settings
    private lateinit var detector: RestrictionDetector

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = Settings(requireContext())
        detector = RestrictionDetector(requireContext())

        buildSettingsGroups(view)
        setupVersion(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            val container = it.findViewById<LinearLayout>(R.id.settingsContainer)
            container.removeAllViews()
            buildSettingsGroups(it)
        }
    }

    private fun buildSettingsGroups(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.settingsContainer)

        // Group: Announcement
        addGroup(container, getString(R.string.settings_group_announcement))
        val announcementCard = createCard(container)
        addChevronRow(announcementCard, getString(R.string.settings_mode),
            if (settings.ttsMode == Settings.TtsMode.DUAL) getString(R.string.settings_mode_desc_dual)
            else getString(R.string.settings_mode_desc_single)) {
            showModePicker()
        }
        addDivider(announcementCard)
        addChevronRow(announcementCard, getString(R.string.settings_primary_lang),
            Settings.TtsLocale.fromTag(settings.preferredLocaleTag).displayName) {
            showTtsLocalePicker(isPrimary = true)
        }
        addDivider(announcementCard)
        val secondLangRow = addChevronRow(announcementCard, getString(R.string.settings_second_lang),
            if (settings.secondLocaleTag.isNotEmpty()) Settings.TtsLocale.fromTag(settings.secondLocaleTag).displayName
            else getString(R.string.settings_second_lang_desc)) {
            if (settings.ttsMode == Settings.TtsMode.DUAL) {
                showTtsLocalePicker(isPrimary = false)
            }
        }
        if (settings.ttsMode == Settings.TtsMode.SINGLE) {
            secondLangRow.alpha = 0.4f
        }

        // Group: Sound
        addGroup(container, getString(R.string.settings_group_sound))
        val soundCard = createCard(container)
        addToggleRow(soundCard, getString(R.string.settings_alarm_volume),
            getString(R.string.settings_alarm_volume_desc), settings.useAlarmVolume) { isChecked ->
            settings.useAlarmVolume = isChecked
        }
        addDivider(soundCard)
        addToggleRow(soundCard, getString(R.string.settings_announce_debits),
            getString(R.string.settings_announce_debits_desc), settings.announceDebits) { isChecked ->
            settings.announceDebits = isChecked
        }

        // Group: Display
        addGroup(container, getString(R.string.settings_group_display))
        val displayCard = createCard(container)
        addToggleRow(displayCard, getString(R.string.settings_payment_popup),
            getString(R.string.settings_payment_popup_desc), settings.showPaymentPopup) { isChecked ->
            settings.showPaymentPopup = isChecked
        }

        // Group: Language
        addGroup(container, getString(R.string.settings_group_language))
        val langCard = createCard(container)
        val currentLang = Settings.AppLanguage.fromTag(settings.appLocaleTag)
        addChevronRow(langCard, getString(R.string.settings_app_language), currentLang.displayName) {
            showLanguagePicker()
        }

        // Group: Providers
        addGroup(container, getString(R.string.settings_group_providers))
        val providerCard = createCard(container)
        addProviderRow(providerCard, "PhonePe", 0xFF5F259F.toInt(), "P",
            settings.phonePeEnabled, enabled = true) { isChecked ->
            settings.phonePeEnabled = isChecked
        }
        addDivider(providerCard)
        addProviderRow(providerCard, "Google Pay", 0xFFFFFFFF.toInt(), "G",
            settings.gPayEnabled, letterColor = 0xFF4285F4.toInt(), enabled = true) { isChecked ->
            settings.gPayEnabled = isChecked
        }
        addDivider(providerCard)
        addProviderRow(providerCard, "Paytm", 0xFF00BAF2.toInt(), "Pt", false, comingSoon = true)
        addDivider(providerCard)
        addProviderRow(providerCard, "BHIM", 0xFFFF7A00.toInt(), "B", false, comingSoon = true)

        // Group: Permissions
        addGroup(container, getString(R.string.settings_group_permissions))
        val permCard = createCard(container)
        val state = detector.snapshot()
        val granted = listOf(
            state.postNotificationsGranted,
            state.notificationListenerEnabled,
            state.ignoringBatteryOptimisations,
            state.exactAlarmGranted
        ).count { it }
        addChevronRow(permCard, getString(R.string.settings_permission_status),
            getString(R.string.settings_permission_status_desc, granted, 4),
            warn = granted < 4) {
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
        }
        addDivider(permCard)
        addChevronRow(permCard, getString(R.string.settings_rerun_wizard), null) {
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
        }
    }

    private fun showModePicker() {
        val modes = Settings.TtsMode.entries.toTypedArray()
        val names = arrayOf(getString(R.string.settings_mode_desc_single), getString(R.string.settings_mode_desc_dual))
        val currentIndex = modes.indexOf(settings.ttsMode).coerceAtLeast(0)

        AlertDialog.Builder(requireContext(), R.style.Theme_UpiSpeaker_Dialog)
            .setTitle(getString(R.string.settings_mode))
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                settings.ttsMode = modes[which]
                dialog.dismiss()
                refreshSettings()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTtsLocalePicker(isPrimary: Boolean) {
        val locales = Settings.TtsLocale.entries.toTypedArray()
        val names = locales.map { it.displayName }.toTypedArray()
        val currentTag = if (isPrimary) settings.preferredLocaleTag else settings.secondLocaleTag
        val currentIndex = locales.indexOfFirst { it.tag == currentTag }.coerceAtLeast(0)

        val title = if (isPrimary) getString(R.string.settings_primary_lang) else getString(R.string.settings_second_lang)
        AlertDialog.Builder(requireContext(), R.style.Theme_UpiSpeaker_Dialog)
            .setTitle(title)
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = locales[which]
                if (isPrimary) {
                    settings.preferredLocaleTag = selected.tag
                } else {
                    settings.secondLocaleTag = selected.tag
                }
                dialog.dismiss()
                refreshSettings()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun refreshSettings() {
        view?.let {
            val container = it.findViewById<LinearLayout>(R.id.settingsContainer)
            container.removeAllViews()
            buildSettingsGroups(it)
        }
    }

    private fun showLanguagePicker() {
        val languages = Settings.AppLanguage.entries.toTypedArray()
        val names = languages.map { it.displayName }.toTypedArray()
        val currentIndex = languages.indexOfFirst { it.tag == settings.appLocaleTag }.coerceAtLeast(0)

        AlertDialog.Builder(requireContext(), R.style.Theme_UpiSpeaker_Dialog)
            .setTitle(getString(R.string.settings_app_language))
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = languages[which]
                settings.appLocaleTag = selected.tag
                dialog.dismiss()
                // Recreate activity to apply new locale
                requireActivity().recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addGroup(container: LinearLayout, title: String) {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density
        val tv = TextView(ctx).apply {
            text = title.uppercase()
            textSize = 11f
            setTextColor(ContextCompat.getColor(ctx, R.color.fg_dim))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding((20 * dp).toInt(), (22 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
        }
        container.addView(tv)
    }

    private fun createCard(parent: LinearLayout): LinearLayout {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_18)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (20 * dp).toInt()
                marginEnd = (20 * dp).toInt()
            }
            layoutParams = p
        }
        parent.addView(card)
        return card
    }

    private fun addDivider(card: LinearLayout) {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density
        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * dp).toInt()
            ).apply {
                marginStart = (16 * dp).toInt()
                marginEnd = (16 * dp).toInt()
            }
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.border))
        }
        card.addView(divider)
    }

    private fun addChevronRow(
        card: LinearLayout,
        title: String,
        subtitle: String?,
        warn: Boolean = false,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }

        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(ctx).apply {
            text = title
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(ctx, R.color.fg))
        }
        textCol.addView(titleTv)

        if (subtitle != null) {
            val subTv = TextView(ctx).apply {
                text = subtitle
                textSize = 12.5f
                setTextColor(
                    if (warn) ContextCompat.getColor(ctx, R.color.warn)
                    else ContextCompat.getColor(ctx, R.color.fg_muted)
                )
            }
            textCol.addView(subTv)
        }

        row.addView(textCol)

        val chevron = TextView(ctx).apply {
            text = "›"
            textSize = 20f
            setTextColor(ContextCompat.getColor(ctx, R.color.fg_dim))
        }
        row.addView(chevron)

        card.addView(row)
        return row
    }

    private fun addToggleRow(
        card: LinearLayout,
        title: String,
        subtitle: String,
        isOn: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
        }

        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        textCol.addView(TextView(ctx).apply {
            text = title
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(ctx, R.color.fg))
        })

        textCol.addView(TextView(ctx).apply {
            text = subtitle
            textSize = 12.5f
            setTextColor(ContextCompat.getColor(ctx, R.color.fg_muted))
        })

        row.addView(textCol)

        @Suppress("UseSwitchCompatOrMaterialCode")
        val toggle = Switch(ctx).apply {
            isChecked = isOn
            setOnCheckedChangeListener { _, checked -> onToggle(checked) }
        }
        row.addView(toggle)

        card.addView(row)
    }

    private fun addProviderRow(
        card: LinearLayout,
        name: String,
        bgColor: Int,
        letter: String,
        isOn: Boolean,
        letterColor: Int = 0xFFFFFFFF.toInt(),
        enabled: Boolean = false,
        comingSoon: Boolean = false,
        onToggle: ((Boolean) -> Unit)? = null
    ) {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
            if (comingSoon) alpha = 0.5f
        }

        // Provider icon
        val iconSize = (32 * dp).toInt()
        val icon = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            val bg = GradientDrawable().apply {
                cornerRadius = 10 * dp
                setColor(bgColor)
            }
            background = bg
        }
        val letterTv = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = letter
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(letterColor)
        }
        icon.addView(letterTv)
        row.addView(icon)

        // Name
        val nameTv = TextView(ctx).apply {
            text = name
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(ctx, R.color.fg))
            setPadding((12 * dp).toInt(), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(nameTv)

        if (comingSoon) {
            val badge = TextView(ctx).apply {
                text = getString(R.string.settings_soon_badge)
                textSize = 10.5f
                setTextColor(ContextCompat.getColor(ctx, R.color.fg_dim))
                setBackgroundResource(R.drawable.bg_soon_badge)
                setPadding((8 * dp).toInt(), (3 * dp).toInt(), (8 * dp).toInt(), (3 * dp).toInt())
            }
            row.addView(badge)
        } else {
            @Suppress("UseSwitchCompatOrMaterialCode")
            val toggle = Switch(ctx).apply {
                isChecked = isOn
                setOnCheckedChangeListener { _, checked -> onToggle?.invoke(checked) }
            }
            row.addView(toggle)
        }

        card.addView(row)
    }

    private fun setupVersion(view: View) {
        try {
            val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            view.findViewById<TextView>(R.id.tvVersion).text =
                getString(R.string.settings_version, info.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            view.findViewById<TextView>(R.id.tvVersion).text =
                getString(R.string.settings_version, "1.0.0")
        }
    }
}
