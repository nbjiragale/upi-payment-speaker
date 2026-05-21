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

class HistoryFragment : Fragment() {

    private lateinit var txLog: TransactionLog
    private var activeFilter = "today"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txLog = TransactionLog(requireContext())

        setupSubtitle(view)
        setupChips(view)
        setupSummaryCard(view)
        setupSparklineBars(view)
        buildTransactionGroups(view)
    }

    private fun setupSubtitle(view: View) {
        val count = txLog.countToday()
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

                setOnClickListener {
                    activeFilter = id
                    view?.let { setupChips(it) }
                }
            }
            container.addView(chip)
        }
    }

    private fun setupSummaryCard(view: View) {
        view.findViewById<TextView>(R.id.tvHistoryTotal).text = "0"
    }

    private fun setupSparklineBars(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.barsContainer)
        container.removeAllViews()
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val barData = listOf(0.3f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 1.0f, 0.7f, 0.2f, 0.5f, 0.3f, 0.45f)
        val maxH = 36f * dp

        for (value in barData) {
            val bar = View(ctx).apply {
                val h = (value * maxH).toInt().coerceAtLeast((2 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    h,
                    1f
                ).apply {
                    marginStart = (1.5f * dp).toInt()
                    marginEnd = (1.5f * dp).toInt()
                    gravity = Gravity.BOTTOM
                }
                val bg = GradientDrawable().apply {
                    cornerRadius = 2f * dp
                    setColor(ContextCompat.getColor(ctx, R.color.primary))
                    alpha = (value * 255).toInt().coerceIn(80, 255)
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

        val groups = getSampleTransactionGroups()
        if (groups.isEmpty()) return

        for (group in groups) {
            // Date header
            val header = TextView(ctx).apply {
                text = group.dateLabel
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.fg_dim))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                letterSpacing = 0.08f
                setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
            }
            container.addView(header)

            // Card for this group
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

            for ((i, tx) in group.transactions.withIndex()) {
                val itemView = LayoutInflater.from(ctx).inflate(R.layout.item_transaction, card, false)
                bindTransactionItem(itemView, tx)

                // Divider between items
                if (i < group.transactions.size - 1) {
                    val divider = View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (1 * dp).toInt()
                        ).apply {
                            marginStart = (64 * dp).toInt()
                            marginEnd = (16 * dp).toInt()
                        }
                        setBackgroundColor(ContextCompat.getColor(ctx, R.color.border))
                    }
                    card.addView(itemView)
                    card.addView(divider)
                } else {
                    card.addView(itemView)
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

    private data class TransactionGroup(
        val dateLabel: String,
        val transactions: List<HomeFragment.TransactionItem>
    )

    private fun getSampleTransactionGroups(): List<TransactionGroup> {
        // Empty state — real data comes from TransactionLog
        return emptyList()
    }
}
