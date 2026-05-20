package com.nbjiragale.upispeaker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.nbjiragale.upispeaker.parser.Transaction

/**
 * Minimal SQLite-backed log of every transaction we attempted to announce.
 * Used for:
 *   * the "Recent transactions" list on the home screen,
 *   * de-duplication across Path A and Path B,
 *   * post-mortem debugging when the user reports a missed announcement.
 *
 * Migrated to Room in Milestone 3.
 */
class TransactionLog(context: Context) {

    private val helper = object : SQLiteOpenHelper(
        context.applicationContext, "upi_log.db", null, 1
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE transactions (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    dedupe_key      TEXT NOT NULL UNIQUE,
                    direction       TEXT NOT NULL,
                    amount_paise    INTEGER NOT NULL,
                    counterparty    TEXT,
                    reference_id    TEXT,
                    source_package  TEXT NOT NULL,
                    source          TEXT NOT NULL,
                    raw_text        TEXT,
                    timestamp_ms    INTEGER NOT NULL,
                    status          TEXT NOT NULL DEFAULT 'PENDING'
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_tx_time ON transactions(timestamp_ms DESC)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS transactions")
            onCreate(db)
        }
    }

    enum class Status { PENDING, SPOKEN, FAILED_TTS, FAILED_AUDIO_FOCUS, DUPLICATE }

    /** Inserts a row.  Returns true if newly inserted, false if a duplicate. */
    fun insertIfNew(tx: Transaction): Boolean {
        val db = helper.writableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM transactions WHERE dedupe_key = ?",
            arrayOf(tx.dedupeKey())
        )
        cursor.use { if (it.moveToFirst()) return false }

        db.execSQL(
            """
            INSERT INTO transactions
              (dedupe_key, direction, amount_paise, counterparty, reference_id,
               source_package, source, raw_text, timestamp_ms, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                tx.dedupeKey(),
                tx.direction.name,
                tx.amountPaise,
                tx.payerOrPayee,
                tx.referenceId,
                tx.sourcePackage,
                tx.source.name,
                tx.rawText,
                tx.timestampMs,
                Status.PENDING.name
            )
        )
        return true
    }

    fun updateStatus(dedupeKey: String, status: Status) {
        helper.writableDatabase.execSQL(
            "UPDATE transactions SET status = ? WHERE dedupe_key = ?",
            arrayOf(status.name, dedupeKey)
        )
    }

    fun countToday(): Int {
        val startOfDay = todayStartMillis()
        helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM transactions WHERE timestamp_ms >= ? AND status = ?",
            arrayOf(startOfDay.toString(), Status.SPOKEN.name)
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    private fun todayStartMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
