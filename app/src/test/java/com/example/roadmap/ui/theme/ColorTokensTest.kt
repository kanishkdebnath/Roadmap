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

    // M3 NavigationBar/Rail + DropdownMenu read surfaceContainer (container) and
    // secondaryContainer (selected indicator). These must map to the green palette,
    // not M3's baseline neutrals — otherwise the nav bar/menus render brown.
    @Test fun dark_navigation_roles_use_the_green_palette() {
        assertEquals(DarkTokens.surfaceVariant, DarkColorScheme.surfaceContainer)
        assertEquals(Color(0xFF10301F), DarkColorScheme.secondaryContainer)
        assertEquals(BrandMint, DarkColorScheme.onSecondaryContainer)
    }

    @Test fun light_navigation_roles_use_the_green_palette() {
        assertEquals(LightTokens.surfaceVariant, LightColorScheme.surfaceContainer)
        assertEquals(Color(0xFFE8EFEA), LightColorScheme.secondaryContainer)
        assertEquals(Brand, LightColorScheme.onSecondaryContainer)
    }
}
