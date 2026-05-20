package com.nbjiragale.upispeaker.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.tts.NumberToWords

/**
 * A translucent popup that briefly shows the payment amount on screen,
 * then auto-dismisses. Designed to overlay even over the lock screen so
 * the shopkeeper gets visual + audible confirmation.
 */
class PaymentPopupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showOverLockScreen()

        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, 0L)
        val rupees = amountPaise / 100L
        val paise = amountPaise % 100L

        val amountText = if (paise > 0) {
            "₹$rupees.${paise.toString().padStart(2, '0')}"
        } else {
            "₹$rupees"
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xDD1A1D24.toInt())
            setPadding(64, 80, 64, 80)
        }

        val labelView = TextView(this).apply {
            text = getString(R.string.popup_payment_received)
            setTextColor(0xFF5BD0AA.toInt())
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val amountView = TextView(this).apply {
            text = amountText
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 48f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 16)
        }

        val wordsView = TextView(this).apply {
            text = NumberToWords.englishIndian(rupees) + " rupees"
            setTextColor(0xFFB0B6C0.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
        }

        container.addView(labelView)
        container.addView(amountView)
        container.addView(wordsView)
        setContentView(container)

        Handler(Looper.getMainLooper()).postDelayed({ finish() }, AUTO_DISMISS_MS)
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
        private const val AUTO_DISMISS_MS = 4000L
    }
}
