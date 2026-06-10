package com.example.roadmap.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionTest {
    @Test fun reduce_motion_is_true_only_when_animator_scale_is_zero() {
        assertEquals(true, reduceMotion(0f))
        assertEquals(false, reduceMotion(1f))
        assertEquals(false, reduceMotion(0.5f))
    }
}
