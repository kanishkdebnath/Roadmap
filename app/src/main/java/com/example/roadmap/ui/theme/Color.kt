package com.example.roadmap.ui.theme

import androidx.compose.ui.graphics.Color

/** Brand identity. */
val Brand = Color(0xFF022E1C)       // primary accent (light) / deep canvas (dark)
val BrandMint = Color(0xFF34D39A)   // derived dark-mode interactive accent
val BrandCyan = Color(0xFF22D3EE)   // dark-mode gradient companion

internal object LightTokens {
    val background = Color(0xFFF5F8F6)
    val surface = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFF2F6F3)
    val outline = Color(0xFFD3DED7)
    val outlineVariant = Color(0xFFE3EBE5)
    val onBackground = Color(0xFF0A1F16)
    val onSurfaceVariant = Color(0xFF5D6E65)
    val primary = Brand
    val onPrimary = Color(0xFFFFFFFF)
    val ringTrack = Color(0xFFE4ECE7)
    val done = Color(0xFF047857)
    val doneContainer = Color(0xFFE2F3EC)
    val overdue = Color(0xFFDC2626)
    val overdueContainer = Color(0xFFFBE8E8)
    val muted = Color(0xFF5D6E65)
    val faint = Color(0xFF8A9890)
    val amber = Color(0xFFF59E0B)
    val amberContainer = Color(0xFFFCEFD2)
    val neutral = Color(0xFF94A3B8)
    val neutralContainer = Color(0xFFEEF1F0)
    val sky = Color(0xFF0EA5E9)
    val skyContainer = Color(0xFFE0F2FE)
}

internal object DarkTokens {
    val background = Color(0xFF061A10)
    val surface = Color(0xFF0C2418)
    val surfaceVariant = Color(0xFF102A1D)
    val outline = Color(0xFF244A37)
    val outlineVariant = Color(0xFF1B3A2A)
    val onBackground = Color(0xFFE9F4EE)
    val onSurfaceVariant = Color(0xFF93AB9D)
    val primary = BrandMint
    val onPrimary = Color(0xFF04130C)
    val ringTrack = Color(0xFF1C3A2B)
    val done = Color(0xFF34D399)
    val doneContainer = Color(0xFF10301F)
    val overdue = Color(0xFFF87171)
    val overdueContainer = Color(0xFF2A1717)
    val muted = Color(0xFF93AB9D)
    val faint = Color(0xFF6C8678)
    val amber = Color(0xFFFBBF24)
    val amberContainer = Color(0xFF2C2510)
    val neutral = Color(0xFF93AB9D)
    val neutralContainer = Color(0xFF1B2A21)
    val sky = Color(0xFF38BDF8)
    val skyContainer = Color(0xFF0E2A38)
}
