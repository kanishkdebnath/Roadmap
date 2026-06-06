package com.example.roadmap.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTokensTest {
    @Test fun brand_is_forest_green() {
        assertEquals(Color(0xFF022E1C), Brand)
    }
    @Test fun dark_accent_is_mint() {
        assertEquals(Color(0xFF34D39A), BrandMint)
    }
    @Test fun light_and_dark_have_distinct_surfaces() {
        assertEquals(Color(0xFFFFFFFF), LightTokens.surface)
        assertEquals(Color(0xFF0C2418), DarkTokens.surface)
    }
}
