package com.example.roadmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletionTest {
    @Test fun all_complete_nonempty_returns_now() {
        assertEquals(42L, milestoneCompletedAt(listOf(true, true), now = 42L))
    }

    @Test fun any_incomplete_returns_null() {
        assertNull(milestoneCompletedAt(listOf(true, false), now = 42L))
    }

    @Test fun empty_returns_null() {
        assertNull(milestoneCompletedAt(emptyList(), now = 42L))
    }
}
