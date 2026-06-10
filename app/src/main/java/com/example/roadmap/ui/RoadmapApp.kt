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
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.ui.detail.RoadmapDetailRoute
import com.example.roadmap.ui.journal.DayEditorStub
import com.example.roadmap.ui.journal.JournalRoute
import com.example.roadmap.ui.list.RoadmapListRoute
import com.example.roadmap.ui.list.RoadmapListViewModel
import com.example.roadmap.ui.list.RoadmapListViewModelFactory
import java.time.LocalDate

/** Top-level destinations. */
enum class Tab { Roadmaps, Journal }

@Composable
fun RoadmapApp(
    repository: RoadmapRepository,
    journalRepository: JournalRepository,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.Roadmaps) }
    var roadmapDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
    // LocalDate isn't Saveable; store the selected journal day as an epoch-day Long.
    var journalEditorDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val journalEditorDate = journalEditorDay?.let { LocalDate.ofEpochDay(it) }

    val inDetail = (tab == Tab.Roadmaps && roadmapDetailId != null) ||
        (tab == Tab.Journal && journalEditorDate != null)
    BackHandler(enabled = inDetail) {
        if (tab == Tab.Roadmaps) roadmapDetailId = null else journalEditorDay = null
    }

    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 600.dp
        Scaffold(
            bottomBar = { if (!wide && !inDetail) RoadmapNavBar(tab) { tab = it } },
        ) { inner ->
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
                        Tab.Journal -> {
                            val d = journalEditorDate
                            if (d == null) {
                                JournalRoute(journalRepository, onOpenDay = { journalEditorDay = it.toEpochDay() })
                            } else {
                                DayEditorStub(d, onBack = { journalEditorDay = null })
                            }
                        }
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
