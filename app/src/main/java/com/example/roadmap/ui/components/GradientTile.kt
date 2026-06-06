package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

/** Rounded gradient icon tile bearing a single initial. */
@Composable
fun GradientTile(
    initial: Char,
    hue: RoadmapHue,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    val brush = Brush.linearGradient(hue.colors(RoadmapTheme.colors.isDark))
    Box(
        modifier = modifier.size(size).background(brush, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial.uppercaseChar().toString(),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Preview @Composable private fun TilePreview() {
    RoadmapTheme {
        GradientTile('K', RoadmapHue.Emerald)
    }
}
