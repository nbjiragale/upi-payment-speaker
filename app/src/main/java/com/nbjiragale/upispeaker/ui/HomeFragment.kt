package com.nbjiragale.upispeaker.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.Settings
import com.nbjiragale.upispeaker.data.TransactionLog
import com.nbjiragale.upispeaker.oem.RestrictionDetector
import com.nbjiragale.upispeaker.service.SpeakerForegroundService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var settings: Settings
    private lateinit var detector: RestrictionDetector
    private lateinit var txLog: TransactionLog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        settings = Settings(ctx)
        detector = RestrictionDetector(ctx)
        txLog = TransactionLog(ctx)

        setupHeader(view)
        setupStatusCard(view)
        setupWarningBanner(view)
        setupTestButton(view)
        setupRecentTransactions(view)
        startPulseAnimation(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshUi(it) }
    }

    private fun setupHeader(view: View) {
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        view.findViewById<TextView>(R.id.tvDayOfWeek).text = dayFormat.format(cal.time).uppercase()
        view.findViewById<TextView>(R.id.tvDate).text = dateFormat.format(cal.time)
    }

    private fun setupStatusCard(view: View) {
        val statusLabel = view.findViewById<TextView>(R.id.tvStatusLabel)
        val dotContainer = view.findViewById<FrameLayout>(R.id.pulsingDotContainer)

        if (settings.listeningEnabled && !settings.isMuted()) {
            statusLabel.text = getString(R.string.status_listening)
            statusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            dotContainer.visibility = View.VISIBLE
        } else if (settings.isMuted()) {
            statusLabel.text = getString(R.string.status_paused)
            statusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent))
            dotContainer.visibility = View.GONE
        } else {
            statusLabel.text = getString(R.string.status_stopped)
            statusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.fg_dim))
            dotContainer.visibility = View.GONE
        }

        val todayStart = todayStartMillis()
        val count = txLog.countToday()
        val todayTotal = txLog.totalPaiseSince(todayStart)

        view.findViewById<TextView>(R.id.tvPaymentCount).text = count.toString()
        view.findViewById<TextView>(R.id.tvTodayTotal).text = formatIndianNumber(todayTotal / 100)
        view.findViewById<TextView>(R.id.tvLastPayment).text = getLastPaymentTime() ?: "—"

        val avg = if (count > 0) todayTotal / count / 100 else 0L
        view.findViewById<TextView>(R.id.tvAvgPayment).text = "₹${formatIndianNumber(avg)}"
    }

    private fun setupWarningBanner(view: View) {
        val banner = view.findViewById<LinearLayout>(R.id.warningBanner)
        val state = detector.snapshot()

        val warningTitle: String?
        val warningDesc: String?
        when {
            !state.ignoringBatteryOptimisations -> {
                warningTitle = getString(R.string.warn_battery_title)
                warningDesc = getString(R.string.warn_battery_desc)
            }
            !state.notificationListenerEnabled -> {
                warningTitle = "Notification access disabled"
                warningDesc = "We can't hear payment notifications."
            }
            else -> {
                warningTitle = null
                warningDesc = null
            }
        }

        if (warningTitle != null) {
            banner.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvWarningTitle).text = warningTitle
            view.findViewById<TextView>(R.id.tvWarningDesc).text = warningDesc
        } else {
            banner.visibility = View.GONE
        }
    }

    private fun setupTestButton(view: View) {
        view.findViewById<View>(R.id.btnTest).setOnClickListener {
            if (!settings.listeningEnabled) {
                settings.listeningEnabled = true
                SpeakerForegroundService.start(requireContext())
            }
            SpeakerForegroundService.test(requireContext())
        }
    }

    private fun setupRecentTransactions(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvRecentTransactions)
        val emptyView = view.findViewById<TextView>(R.id.tvEmpty)

        val entries = txLog.getRecent(5)
        val transactions = entries.map { entry ->
            val rupees = entry.amountPaise / 100
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestampMs))
            val statusStr = when (entry.status) {
                "SPOKEN" -> "spoken"
                "FAILED_TTS", "FAILED_AUDIO_FOCUS" -> "failed"
                "DUPLICATE" -> "duplicate"
                else -> "spoken"
            }
            val sourceName = entry.source.lowercase().replaceFirstChar { it.uppercase() }
            TransactionItem(formatIndianNumber(rupees), sourceName, timeStr, statusStr)
        }

        if (transactions.isEmpty()) {
            rv.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = TransactionAdapter(transactions)
        }

        view.findViewById<TextView>(R.id.tvSeeAll).setOnClickListener {
            (activity as? MainActivity)?.navigateToHistory()
        }
    }

    private fun startPulseAnimation(view: View) {
        val ring1 = view.findViewById<View>(R.id.pulseRing1)
        val ring2 = view.findViewById<View>(R.id.pulseRing2)

        fun pulseView(v: View, startDelay: Long): AnimatorSet {
            val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 0.7f, 2.4f).apply { duration = 1800; repeatCount = ObjectAnimator.INFINITE }
            val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 0.7f, 2.4f).apply { duration = 1800; repeatCount = ObjectAnimator.INFINITE }
            val alpha = ObjectAnimator.ofFloat(v, "alpha", 0.55f, 0f).apply { duration = 1800; repeatCount = ObjectAnimator.INFINITE }
            return AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                this.startDelay = startDelay
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }

        if (settings.listeningEnabled && !settings.isMuted()) {
            pulseView(ring1, 0)
            pulseView(ring2, 600)
        }
    }

    private fun refreshUi(view: View) {
        setupStatusCard(view)
        setupWarningBanner(view)
        setupRecentTransactions(view)
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getLastPaymentTime(): String? {
        val entries = txLog.getRecent(1)
        if (entries.isEmpty()) return null
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entries[0].timestampMs))
    }

    private fun formatIndianNumber(n: Long): String {
        if (n < 1000) return n.toString()
        val last3 = (n % 1000).toString().padStart(3, '0')
        var rest = n / 1000
        val parts = mutableListOf(last3)
        while (rest > 0) {
            parts.add(0, (rest % 100).toString().let { if (parts.size > 1) it.padStart(2, '0') else it })
            rest /= 100
        }
        return parts.joinToString(",")
    }

    data class TransactionItem(
        val amount: String,
        val source: String,
        val time: String,
        val status: String
    )

    inner class TransactionAdapter(private val items: List<TransactionItem>) :
        RecyclerView.Adapter<TransactionAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val providerFrame: FrameLayout = view.findViewById(R.id.providerIconFrame)
            val providerLetter: TextView = view.findViewById(R.id.tvProviderLetter)
            val providerName: TextView = view.findViewById(R.id.tvProviderName)
            val time: TextView = view.findViewById(R.id.tvTime)
            val status: TextView = view.findViewById(R.id.tvStatus)
            val amount: TextView = view.findViewById(R.id.tvAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.providerName.text = item.source
            holder.time.text = item.time
            holder.amount.text = item.amount

            val (bgColor, letter, letterColor) = getProviderStyle(item.source)
            val bg = GradientDrawable().apply {
                cornerRadius = 11f * holder.itemView.resources.displayMetrics.density
                setColor(bgColor)
            }
            holder.providerFrame.background = bg
            holder.providerLetter.text = letter
            holder.providerLetter.setTextColor(letterColor)

            when (item.status) {
                "spoken" -> {
                    holder.status.text = getString(R.string.status_spoken)
                    holder.status.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                }
                "failed" -> {
                    holder.status.text = getString(R.string.status_failed)
                    holder.status.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn))
                }
                "duplicate" -> {
                    holder.status.text = getString(R.string.status_duplicate)
                    holder.status.setTextColor(ContextCompat.getColor(requireContext(), R.color.fg_dim))
                }
            }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        fun getProviderStyle(source: String): Triple<Int, String, Int> {
            return when {
                source.contains("PhonePe", true) -> Triple(0xFF5F259F.toInt(), "P", 0xFFFFFFFF.toInt())
                source.contains("Google", true) || source.contains("GPay", true) ->
                    Triple(0xFFFFFFFF.toInt(), "G", 0xFF4285F4.toInt())
                source.contains("Paytm", true) -> Triple(0xFF00BAF2.toInt(), "Pt", 0xFFFFFFFF.toInt())
                source.contains("BHIM", true) -> Triple(0xFFFF7A00.toInt(), "B", 0xFFFFFFFF.toInt())
                else -> Triple(0xFF1E232C.toInt(), "?", 0xFF9AA3B2.toInt())
            }
        }
    }
}
