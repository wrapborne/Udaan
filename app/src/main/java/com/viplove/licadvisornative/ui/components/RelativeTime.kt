package com.viplove.licadvisornative.ui.components

import com.viplove.licadvisornative.ui.screens.formatDateTime

fun relativeTime(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "Never"
    val elapsed = System.currentTimeMillis() - timestamp
    return when {
        elapsed < 60_000 -> "Just now"
        elapsed < 3_600_000 -> "${elapsed / 60_000} min ago"
        elapsed < 86_400_000 -> "${elapsed / 3_600_000} hours ago"
        else -> formatDateTime(timestamp)
    }
}
