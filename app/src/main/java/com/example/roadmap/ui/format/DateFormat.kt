package com.example.roadmap.ui.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "2026-09-30" -> "Sep 30" (device locale); null/unparseable -> null. */
fun formatDeadline(iso: String?): String? {
    if (iso == null) return null
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return null
    return date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}
