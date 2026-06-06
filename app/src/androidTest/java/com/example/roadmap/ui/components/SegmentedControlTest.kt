package com.example.roadmap.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.roadmap.ui.theme.RoadmapTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SegmentedControlTest {
    @get:Rule val rule = createComposeRule()
    @Test fun click_reports_index() {
        var selected = 0
        rule.setContent {
            RoadmapTheme { SegmentedControl(listOf("Active", "Archive"), selected, onSelect = { selected = it }) }
        }
        rule.onNodeWithText("Archive").performClick()
        assertEquals(1, selected)
    }
}
