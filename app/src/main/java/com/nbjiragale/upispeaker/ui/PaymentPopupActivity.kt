package com.nbjiragale.upispeaker.ui

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan

import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.tts.NumberToWords
import com.nbjiragale.upispeaker.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dark frosted-glass payment popup matching the design handoff.
 * Shows amount, words, provider badge, and a timed progress bar.
 */
class PaymentPopupActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun attachBaseContext(newBase: Context) {
        val tag = Settings(newBase).appLocaleTag
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, 0L)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: "PhonePe"
        val rupees = amountPaise / 100L
        val paise = amountPaise % 100L
        val dp = resources.displayMetrics.density

        // Root: overlay background
        val root = FrameLayout(this).apply {
            setBackgroundColor(0x8C000000.toInt())
            setOnClickListener { finish() }
        }

        // Card container (dark glass)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val cardBg = GradientDrawable().apply {
                cornerRadius = 28 * dp
                setColor(0xF5161A21.toInt())
                setStroke((1 * dp).toInt(), 0x1AFFFFFF)
            }
            background = cardBg
            setPadding((36 * dp).toInt(), (32 * dp).toInt(), (36 * dp).toInt(), (24 * dp).toInt())
            elevation = 24 * dp
        }
        val cardParams = FrameLayout.LayoutParams(
            (300 * dp).toInt(),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }
        root.addView(card, cardParams)

        // Green glow at top of card
        val glow = View(this).apply {
            val glowBg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x305BD0AA, 0x00000000)
            )
            glowBg.cornerRadius = 28 * dp
            background = glowBg
        }
        val glowParams = FrameLayout.LayoutParams(
            (300 * dp).toInt(),
            (120 * dp).toInt()
        ).apply { gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP }
        // Position glow at same position as card
        root.addView(glow, 0, glowParams)

        // "PAYMENT RECEIVED" badge
        val badge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pillBg = GradientDrawable().apply {
                cornerRadius = 100 * dp
                setColor(0x245BD0AA)
            }
            background = pillBg
            setPadding((14 * dp).toInt(), (7 * dp).toInt(), (16 * dp).toInt(), (7 * dp).toInt())
        }
        val checkCircle = TextView(this).apply {
            text = "✓"
            textSize = 14f
            setTextColor(0xFF5BD0AA.toInt())
            setTypeface(typeface, Typeface.BOLD)
        }
        val badgeText = TextView(this).apply {
            text = getString(R.string.popup_payment_received)
            textSize = 11f
            setTextColor(0xFF5BD0AA.toInt())
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding((8 * dp).toInt(), 0, 0, 0)
        }
        badge.addView(checkCircle)
        badge.addView(badgeText)
        card.addView(badge)

        // Amount
        val amountStr = if (paise > 0) {
            "${rupees}.${paise.toString().padStart(2, '0')}"
        } else {
            "$rupees"
        }
        val amountView = TextView(this).apply {
            val ssb = SpannableStringBuilder()
            ssb.append("₹")
            ssb.setSpan(ForegroundColorSpan(0xFFFFC857.toInt()), 0, 1, 0)
            ssb.append(amountStr)
            text = ssb
            textSize = 56f
            setTextColor(0xFFF5F7FA.toInt())
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, (22 * dp).toInt(), 0, (4 * dp).toInt())
        }
        card.addView(amountView)

        // Amount in words
        val wordsView = TextView(this).apply {
            text = NumberToWords.englishIndian(rupees) + " rupees"
            textSize = 14f
            setTextColor(0xFF9AA3B2.toInt())
            gravity = Gravity.CENTER
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC))
            setPadding(0, 0, 0, (22 * dp).toInt())
        }
        card.addView(wordsView)

        // Divider
        val divider = View(this).apply {
            setBackgroundColor(0x0FFFFFFF)
        }
        card.addView(divider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
        ))

        // Footer: provider badge + "via PhonePe" + time
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (16 * dp).toInt(), 0, (12 * dp).toInt())
        }

        // Provider icon
        val (provBg, provLetter, provLetterColor) = HomeFragment.getProviderStyle(source)
        val provIcon = FrameLayout(this).apply {
            val iconSize = (26 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            val bg = GradientDrawable().apply {
                cornerRadius = 7 * dp
                setColor(provBg)
            }
            background = bg
        }
        val provLetterTv = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = provLetter
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(provLetterColor)
        }
        provIcon.addView(provLetterTv)
        footer.addView(provIcon)

        // "via PhonePe"
        val viaTv = TextView(this).apply {
            text = "${getString(R.string.popup_via)} $source"
            textSize = 12.5f
            setTextColor(0xFF9AA3B2.toInt())
            setPadding((10 * dp).toInt(), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        footer.addView(viaTv)

        // Time
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val timeTv = TextView(this).apply {
            text = timeStr
            textSize = 12.5f
            setTextColor(0xFF5C6470.toInt())
            setTypeface(Typeface.MONOSPACE)
        }
        footer.addView(timeTv)
        card.addView(footer)

        // Progress bar
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 0
            val pbBg = GradientDrawable().apply {
                setColor(0x0FFFFFFF)
                cornerRadius = 2 * dp
            }
            progressDrawable = resources.getDrawable(R.drawable.bg_progress_bar, null)
            setPadding(0, 0, 0, 0)
        }
        val pbParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (3 * dp).toInt()
        )
        card.addView(progressBar, pbParams)

        // Animate progress bar
        ObjectAnimator.ofInt(progressBar, "progress", 0, 1000).apply {
            duration = AUTO_DISMISS_MS
            interpolator = LinearInterpolator()
            start()
        }

        setContentView(root)
        handler.postDelayed({ finish() }, AUTO_DISMISS_MS)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_AMOUNT_PAISE = "extra_amount_paise"
        const val EXTRA_SOURCE = "extra_source"
        private const val AUTO_DISMISS_MS = 4000L
    }
}
