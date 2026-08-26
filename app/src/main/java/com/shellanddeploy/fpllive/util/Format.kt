package com.shellanddeploy.fpllive.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {

    /** Prices are stored in tenths of £m (now_cost 150 = £15.0m). */
    fun price(tenthsOfMillion: Int?): String {
        val v = tenthsOfMillion ?: 0
        return String.format(Locale.UK, "£%.1fm", v / 10.0)
    }

    fun priceShort(tenthsOfMillion: Int?): String {
        val v = tenthsOfMillion ?: 0
        return String.format(Locale.UK, "£%.1f", v / 10.0)
    }

    /** Value from entry history is in tenths of £m too. */
    fun teamValue(tenthsOfMillion: Int?): String {
        val v = tenthsOfMillion ?: 0
        return String.format(Locale.UK, "£%.1fm", v / 10.0)
    }

    fun decimal(value: String): String {
        val d = value.toDoubleOrNull() ?: 0.0
        return if (d == d.toInt().toDouble()) d.toInt().toString()
        else String.format(Locale.UK, "%.1f", d)
    }

    fun decimal(value: Double): String {
        val d = value
        return if (d == d.toInt().toDouble()) d.toInt().toString()
        else String.format(Locale.UK, "%.1f", d)
    }

    fun ordinal(rank: Int?): String {
        if (rank == null || rank <= 0) return "—"
        return NumberFormat.getIntegerInstance(Locale.UK).format(rank)
    }

    fun percent(value: String): String {
        val d = value.toDoubleOrNull() ?: 0.0
        return String.format(Locale.UK, "%.1f%%", d)
    }

    fun percent(value: Double): String = String.format(Locale.UK, "%.1f%%", value)

    private val eventDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
    private val kickoffFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun kickoffTime(iso: String): String = runCatching {
        val instant = Instant.parse(iso)
        val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        kickoffFormatter.format(ldt)
    }.getOrElse { iso }

    fun deadline(iso: String): String = runCatching {
        val instant = Instant.parse(iso)
        val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        eventDateFormatter.format(ldt)
    }.getOrElse { iso }

    /** "updated Xs ago" / "updated Xm ago" / "updated Xh ago". */
    fun timeAgo(epochMillis: Long): String {
        val diff = System.currentTimeMillis() - epochMillis
        val seconds = diff / 1000
        return when {
            seconds < 0 -> "just now"
            seconds < 5 -> "just now"
            seconds < 60 -> "updated ${seconds}s ago"
            seconds < 3600 -> "updated ${seconds / 60}m ago"
            seconds < 86400 -> "updated ${seconds / 3600}h ago"
            else -> "updated ${seconds / 86400}d ago"
        }
    }

    fun matchMinute(minutes: Int): String = when {
        minutes <= 0 -> "KO"
        minutes in 1..45 -> "$minutes'"
        minutes in 46..90 -> "$minutes'"
        minutes in 91..120 -> "ET ${minutes - 90}'"
        else -> "$minutes'"
    }

    fun statusLabel(status: String): String = when (status) {
        "a" -> "Available"
        "d" -> "Doubtful"
        "i" -> "Injured"
        "u" -> "Unavailable"
        "n" -> "Not in squad"
        "s" -> "Suspended"
        else -> "Unknown"
    }
}
