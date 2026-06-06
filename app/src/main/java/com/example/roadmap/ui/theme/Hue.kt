package com.example.roadmap.ui.theme

import androidx.compose.ui.graphics.Color

/** Per-item color identity for icon tiles, milestone spines, empty-state art. */
enum class RoadmapHue(
    val lightStart: Color, val lightEnd: Color,
    val darkStart: Color, val darkEnd: Color,
) {
    Emerald(Color(0xFF10B981), Color(0xFF0D9488), Color(0xFF34D399), Color(0xFF2DD4BF)),
    Amber(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFFBBF24), Color(0xFFFB923C)),
    Indigo(Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF818CF8), Color(0xFF60A5FA)),
    Violet(Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFA78BFA), Color(0xFFC084FC)),
    Cyan(Color(0xFF06B6D4), Color(0xFF0EA5E9), Color(0xFF22D3EE), Color(0xFF38BDF8)),
    Rose(Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFFFB7185), Color(0xFFF472B6));

    fun colors(dark: Boolean): List<Color> =
        if (dark) listOf(darkStart, darkEnd) else listOf(lightStart, lightEnd)
}

/** Deterministic, stable hue for a given entity id (works for negatives). */
fun hueForId(id: Long): RoadmapHue {
    val n = RoadmapHue.entries.size
    val idx = (((id % n) + n) % n).toInt()
    return RoadmapHue.entries[idx]
}
