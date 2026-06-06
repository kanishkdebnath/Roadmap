package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    hue: RoadmapHue,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(92.dp)
                .background(Brush.linearGradient(hue.colors(RoadmapTheme.colors.isDark)),
                    RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = RoadmapTheme.colors.muted, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp))
        if (actions != null) Box(Modifier.padding(top = 8.dp)) { actions() }
    }
}

@Preview @Composable private fun EmptyStatePreview() {
    RoadmapTheme {
        EmptyState(
            icon = Icons.Outlined.Map,
            title = "No roadmaps yet",
            message = "Create your first roadmap, or import one from JSON.",
            hue = RoadmapHue.Emerald,
        )
    }
}

@Preview(name = "EmptyState Dark") @Composable private fun EmptyStateDarkPreview() {
    RoadmapTheme(darkTheme = true) {
        EmptyState(
            icon = Icons.Outlined.Map,
            title = "No roadmaps yet",
            message = "Create your first roadmap, or import one from JSON.",
            hue = RoadmapHue.Cyan,
        )
    }
}
