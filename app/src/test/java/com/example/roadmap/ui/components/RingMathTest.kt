package com.example.roadmap.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RingMathTest {
    @Test fun sweep_is_proportional() {
        assertEquals(0f, progressSweep(0f), 0.001f)
        assertEquals(180f, progressSweep(0.5f), 0.001f)
        assertEquals(360f, progressSweep(1f), 0.001f)
    }
    @Test fun sweep_clamps_out_of_range() {
        assertEquals(0f, progressSweep(-0.2f), 0.001f)
        assertEquals(360f, progressSweep(1.5f), 0.001f)
    }
    @Test fun percent_label_rounds() {
        assertEquals("62", progressPercentLabel(0.615f))
        assertEquals("100", progressPercentLabel(1f))
        assertEquals("0", progressPercentLabel(0f))
    }
    @Test fun is_complete_only_at_full() {
        assertEquals(true, isRingComplete(1f))
        assertEquals(true, isRingComplete(1.5f))
        assertEquals(false, isRingComplete(0.999f))
    }
}
