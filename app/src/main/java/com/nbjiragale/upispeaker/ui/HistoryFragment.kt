package com.nbjiragale.upispeaker.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nbjiragale.upispeaker.R
import com.nbjiragale.upispeaker.data.TransactionLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var txLog: TransactionLog
    private var activeFilter = "today"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txLog = TransactionLog(requireContext())

        setupChips(view)
        refreshData(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshData(it) }
    }

    private fun refreshData(view: View) {
        setupSubtitle(view)
        setupSummaryCard(view)
        setupSparklineBars(view)
        buildTransactionGroups(view)
    }

    private fun setupSubtitle(view: View) {
        val sinceMs = getFilterStartMs()
        val count = txLog.countSince(sinceMs)
        view.findViewById<TextView>(R.id.tvHistorySubtitle).text =
            getString(R.string.history_subtitle, count)
    }

    private fun setupChips(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.chipContainer)
        container.removeAllViews()
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val chips = listOf(
            "all" to getString(R.string.filter_all),
            "today" to getString(R.string.filter_today),
            "week" to getString(R.string.filter_this_week),
            "month" to getString(R.string.filter_this_month),
        )

        for ((id, label) in chips) {
            val chip = TextView(ctx).apply {
                text = label
                textSize = 13f
                setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * dp).toInt() }
                layoutParams = params

                if (id == activeFilter) {
                    setBackgroundResource(R.drawable.bg_chip_active)
                    setTextColor(ContextCompat.getColor(ctx, R.color.on_primary))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                } else {
                    setBackgroundResource(R.drawable.bg_chip_inactive)
                    setTextColor(ContextCompat.getColor(ctx, R.color.fg_muted))
                }

                setOnClickListener { _ ->
                    activeFilter = id
                    getView()?.let { v ->
                        setupChips(v)
                        refreshData(v)
                    }
                }
            }
            container.addView(chip)
        }
    }

    private fun setupSummaryCard(view: View) {
        val todayTotal = txLog.totalPaiseSince(todayStartMillis())
        view.findViewById<TextView>(R.id.tvHistoryTotal).text = formatIndianNumber(todayTotal / 100)
    }

    private fun setupSparklineBars(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.barsContainer)
        container.removeAllViews()
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        // Build 7-day sparkline from real data
        val dailyTotals = mutableListOf<Long>()
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dayStart = dayCal.timeInMillis
            val total = txLog.totalPaiseSince(dayStart)
            dailyTotals.add(total)
        }
        val maxVal = dailyTotals.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val maxH = 36f * dp

        for (value in dailyTotals) {
            val fraction = value.toFloat() / maxVal
            val bar = View(ctx).apply {
                val h = (fraction * maxH).toInt().coerceAtLeast((2 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0, h, 1f
                ).apply {
                    marginStart = (2 * dp).toInt()
                    marginEnd = (2 * dp).toInt()
                    gravity = Gravity.BOTTOM
                }
                val bg = GradientDrawable().apply {
                    cornerRadius = 2f * dp
                    setColor(ContextCompat.getColor(ctx, R.color.primary))
                    alpha = if (fraction > 0.01f) (fraction * 255).toInt().coerceIn(80, 255) else 40
                }
                background = bg
            }
            container.addView(bar)
        }
    }

    private fun buildTransactionGroups(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.transactionGroups)
        container.removeAllViews()
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val sinceMs = getFilterStartMs()
        val entries = txLog.getAllSince(sinceMs)

        val emptyView = view.findViewById<TextView>(R.id.tvHistoryEmpty)
        if (entries.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
            return
        }
        emptyView?.visibility = View.GONE

        // Group by date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val grouped = entries.groupBy { dateFormat.format(Date(it.timestampMs)) }

        for ((dateLabel, txns) in grouped) {
            val today = dateFormat.format(Date())
            val displayLabel = if (dateLabel == today) "Today" else dateLabel

            val header = TextView(ctx).apply {
                text = displayLabel.uppercase()
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.fg_dim))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                letterSpacing = 0.08f
                setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
            }
            container.addView(header)

            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                val cardParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = (20 * dp).toInt()
                    marginEnd = (20 * dp).toInt()
                }
                layoutParams = cardParams
                setBackgroundResource(R.drawable.bg_card_18)
            }

            for ((i, entry) in txns.withIndex()) {
                val rupees = entry.amountPaise / 100
                val timeStr = timeFormat.format(Date(entry.timestampMs))
                val statusStr = when (entry.status) {
                    "SPOKEN" -> "spoken"
                    "FAILED_TTS", "FAILED_AUDIO_FOCUS" -> "failed"
                    "DUPLICATE" -> "duplicate"
                    else -> "spoken"
                }
                val sourceName = entry.source.lowercase().replaceFirstChar { it.uppercase() }
                val item = HomeFragment.TransactionItem(
                    formatIndianNumber(rupees), sourceName, timeStr, statusStr
                )

                val itemView = LayoutInflater.from(ctx).inflate(R.layout.item_transaction, card, false)
                bindTransactionItem(itemView, item)

                card.addView(itemView)
                if (i < txns.size - 1) {
                    val divider = View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (1 * dp).toInt()
                        ).apply {
                            marginStart = (60 * dp).toInt()
                            marginEnd = (16 * dp).toInt()
                        }
                        setBackgroundColor(ContextCompat.getColor(ctx, R.color.border))
                    }
                    card.addView(divider)
                }
            }
            container.addView(card)
        }
    }

    private fun bindTransactionItem(view: View, tx: HomeFragment.TransactionItem) {
        val providerFrame = view.findViewById<FrameLayout>(R.id.providerIconFrame)
        val providerLetter = view.findViewById<TextView>(R.id.tvProviderLetter)
        val providerName = view.findViewById<TextView>(R.id.tvProviderName)
        val time = view.findViewById<TextView>(R.id.tvTime)
        val status = view.findViewById<TextView>(R.id.tvStatus)
        val amount = view.findViewById<TextView>(R.id.tvAmount)

        providerName.text = tx.source
        time.text = tx.time
        amount.text = tx.amount

        val (bgColor, letter, letterColor) = HomeFragment.getProviderStyle(tx.source)
        val bg = GradientDrawable().apply {
            cornerRadius = 11f * view.resources.displayMetrics.density
            setColor(bgColor)
        }
        providerFrame.background = bg
        providerLetter.text = letter
        providerLetter.setTextColor(letterColor)

        val ctx = view.context
        when (tx.status) {
            "spoken" -> {
                status.text = getString(R.string.status_spoken)
                status.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            }
            "failed" -> {
                status.text = getString(R.string.status_failed)
                status.setTextColor(ContextCompat.getColor(ctx, R.color.warn))
            }
            "duplicate" -> {
                status.text = getString(R.string.status_duplicate)
                status.setTextColor(ContextCompat.getColor(ctx, R.color.fg_dim))
            }
        }
    }

    private fun getFilterStartMs(): Long {
        val cal = Calendar.getInstance()
        return when (activeFilter) {
            "today" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L // "all"
        }
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
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
}
