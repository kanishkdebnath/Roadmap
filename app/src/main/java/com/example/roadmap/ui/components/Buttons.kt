package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.primaryBrush

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(RoadmapTheme.colors.primaryBrush)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(role = Role.Button, enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(SolidColor(MaterialTheme.colorScheme.surfaceVariant))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(SolidColor(RoadmapTheme.colors.overdue))
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun RoadmapFab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(RoadmapTheme.colors.primaryBrush)
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("+", color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge)
        Text(text, color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Preview @Composable private fun ButtonsPreview() {
    RoadmapTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
            PrimaryButton("Create", {})
            SecondaryButton("Import", {})
            DangerButton("Delete", {})
        }
    }
}

@Preview @Composable private fun FabPreview() {
    RoadmapTheme {
        RoadmapFab("New Roadmap", {}, Modifier.padding(16.dp))
    }
}
