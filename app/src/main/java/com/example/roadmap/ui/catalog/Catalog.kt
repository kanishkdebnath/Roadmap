package com.example.roadmap.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.components.*
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    var seg by remember { mutableIntStateOf(0) }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rings", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RingProgress(0.25f, size = RingSize.Small)
                RingProgress(0.62f, size = RingSize.Medium)
                RingProgress(1f, size = RingSize.Large)
            }
            Text("Segmented", style = MaterialTheme.typography.titleMedium)
            SegmentedControl(listOf("Active", "Archive"), seg, onSelect = { seg = it })
            Text("Tiles", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoadmapHue.entries.forEach { GradientTile(it.name.first(), it) }
            }
            Text("Chips", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip("6 milestones")
                DeadlineChip("Sep 30", DeadlineState.Normal)
                DeadlineChip("May 15", DeadlineState.Overdue)
                DeadlineChip("Done", DeadlineState.Done)
            }
            LinkChip("developer.android.com")
            Text("Buttons", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Create", {})
                SecondaryButton("Import", {})
                DangerButton("Delete", {})
            }
            RoadmapFab("New", {})
            Text("Steps", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DragGrip(); StepCheckbox(true, {}); StepCheckbox(false, {})
            }
            AddInline("Add step", {})
            EmptyState(
                Icons.Outlined.Map,
                "No roadmaps yet",
                "Create your first roadmap, or import one from JSON.",
                RoadmapHue.Emerald,
            )
        }
    }
}

@Preview(name = "Catalog Light") @Composable private fun CatalogLight() =
    RoadmapTheme(darkTheme = false) { DesignSystemCatalog() }

@Preview(name = "Catalog Dark") @Composable private fun CatalogDark() =
    RoadmapTheme(darkTheme = true) { DesignSystemCatalog() }
