package com.example.roadmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReorderTest {
    @Test fun valid_when_same_id_set() {
        assertTrue(isValidReorder(current = listOf(1L, 2L, 3L), requested = listOf(3L, 1L, 2L)))
    }

    @Test fun invalid_when_ids_added_or_dropped() {
        assertFalse(isValidReorder(listOf(1L, 2L), listOf(1L, 2L, 3L)))
        assertFalse(isValidReorder(listOf(1L, 2L, 3L), listOf(1L, 2L)))
    }

    @Test fun positions_are_contiguous_from_zero() {
        assertEquals(mapOf(3L to 0, 1L to 1, 2L to 2), positionsFor(listOf(3L, 1L, 2L)))
    }
}
