package com.example.roadmap.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class HueTest {
    @Test fun maps_id_zero_to_first_hue() {
        assertEquals(RoadmapHue.Emerald, hueForId(0))
    }
    @Test fun wraps_around_the_palette() {
        assertEquals(RoadmapHue.Emerald, hueForId(6))   // 6 hues -> wraps
        assertEquals(RoadmapHue.Amber, hueForId(7))
    }
    @Test fun handles_negative_ids() {
        assertEquals(RoadmapHue.Rose, hueForId(-1))     // last hue
    }
    @Test fun gradient_returns_two_stops_per_theme() {
        assertEquals(2, RoadmapHue.Cyan.colors(dark = true).size)
        assertEquals(2, RoadmapHue.Cyan.colors(dark = false).size)
    }
}
