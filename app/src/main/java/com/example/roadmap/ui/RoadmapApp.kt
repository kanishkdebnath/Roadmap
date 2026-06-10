package com.example.roadmap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.RoadmapRepository
import com.example.roadmap.data.ThemeMode
import com.example.roadmap.ui.detail.RoadmapDetailRoute
import com.example.roadmap.ui.journal.JournalScaffoldScreen
import com.example.roadmap.ui.list.RoadmapListRoute
import com.example.roadmap.ui.list.RoadmapListViewModel
import com.example.roadmap.ui.list.RoadmapListViewModelFactory

/** Top-level destinations. */
enum class Tab { Roadmaps, Journal }

@Composable
fun RoadmapApp(
    repository: RoadmapRepository,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.Roadmaps) }
    var roadmapDetailId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Back inside a roadmap detail pops to the list. The nav bar/rail is hidden while in detail,
    // so tab switches only happen from a tab root (roadmapDetailId is null at that point).
    BackHandler(enabled = tab == Tab.Roadmaps && roadmapDetailId != null) { roadmapDetailId = null }
    val inDetail = tab == Tab.Roadmaps && roadmapDetailId != null

    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 600.dp
        Scaffold(
            bottomBar = { if (!wide && !inDetail) RoadmapNavBar(tab) { tab = it } },
        ) { inner ->
            // The List/Detail screens have their own Scaffolds; consumeWindowInsets tells them the
            // outer Scaffold already applied these insets, so they don't double-pad.
            Row(
                Modifier
                    .padding(inner)
                    .consumeWindowInsets(inner)
                    .fillMaxSize(),
            ) {
                if (wide && !inDetail) RoadmapNavRail(tab) { tab = it }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (tab) {
                        Tab.Roadmaps -> {
                            val id = roadmapDetailId
                            if (id == null) {
                                val listVm: RoadmapListViewModel =
                                    viewModel(factory = RoadmapListViewModelFactory(repository))
                                RoadmapListRoute(
                                    listVm,
                                    onOpenRoadmap = { roadmapDetailId = it },
                                    themeMode = themeMode,
                                    onSetThemeMode = onSetThemeMode,
                                )
                            } else {
                                RoadmapDetailRoute(repository, id, onBack = { roadmapDetailId = null })
                            }
                        }
                        Tab.Journal -> JournalScaffoldScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == Tab.Roadmaps,
            onClick = { onSelect(Tab.Roadmaps) },
            icon = { Icon(Icons.Outlined.Map, contentDescription = null) },
            label = { Text("Roadmaps") },
        )
        NavigationBarItem(
            selected = selected == Tab.Journal,
            onClick = { onSelect(Tab.Journal) },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            label = { Text("Journal") },
        )
    }
}

@Composable
private fun RoadmapNavRail(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationRail {
        NavigationRailItem(
            selected = selected == Tab.Roadmaps,
            onClick = { onSelect(Tab.Roadmaps) },
            icon = { Icon(Icons.Outlined.Map, contentDescription = null) },
            label = { Text("Roadmaps") },
        )
        NavigationRailItem(
            selected = selected == Tab.Journal,
            onClick = { onSelect(Tab.Journal) },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            label = { Text("Journal") },
        )
    }
}
