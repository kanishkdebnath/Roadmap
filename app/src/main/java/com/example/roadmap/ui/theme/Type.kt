package com.example.roadmap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.roadmap.R

// minSdk 26 supports variable fonts: one file, weights selected via the 'wght' axis.
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: FontWeight) = Font(
    R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    interWeight(FontWeight.Normal),
    interWeight(FontWeight.Medium),
    interWeight(FontWeight.SemiBold),
    interWeight(FontWeight.Bold),
    interWeight(FontWeight.ExtraBold),
)

/** Apply to any numeric text (counts, percentages) for tabular figures. */
val TabularNums = TextStyle(fontFeatureSettings = "tnum")

private fun inter(
    weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Inter, fontWeight = weight, fontSize = size.sp,
    lineHeight = line.sp, letterSpacing = tracking.sp,
)

val Typography = Typography(
    displaySmall = inter(FontWeight.ExtraBold, 28, 34, -0.5),
    headlineMedium = inter(FontWeight.ExtraBold, 24, 30, -0.5),
    titleLarge = inter(FontWeight.Bold, 20, 26, -0.3),
    titleMedium = inter(FontWeight.Bold, 16, 22, -0.1),
    titleSmall = inter(FontWeight.SemiBold, 14, 20, -0.1),
    bodyLarge = inter(FontWeight.Normal, 15, 22),
    bodyMedium = inter(FontWeight.Normal, 13, 19),
    bodySmall = inter(FontWeight.Medium, 12, 17),
    labelLarge = inter(FontWeight.Bold, 14, 18),
    labelMedium = inter(FontWeight.SemiBold, 12, 16, 0.2),
    labelSmall = inter(FontWeight.Bold, 11, 14, 0.6),
)
