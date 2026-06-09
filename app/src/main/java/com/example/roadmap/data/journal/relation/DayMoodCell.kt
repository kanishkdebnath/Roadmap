package com.example.roadmap.data.journal.relation

/** Lightweight heatmap projection: which days have an entry, and their mood. */
data class DayMoodCell(val date: String, val moodScale: Int)
