package com.example.roadmap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.RoadmapRepository
import com.example.roadmap.data.ThemeMode
import com.example.roadmap.ui.detail.RoadmapDetailRoute
import com.example.roadmap.ui.list.RoadmapListRoute
import com.example.roadmap.ui.list.RoadmapListViewModel
import com.example.roadmap.ui.list.RoadmapListViewModelFactory

@Composable
fun RoadmapApp(
    repository: RoadmapRepository,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = detailId != null) { detailId = null }

    val current = detailId
    if (current == null) {
        val listVm: RoadmapListViewModel = viewModel(factory = RoadmapListViewModelFactory(repository))
        RoadmapListRoute(
            listVm,
            onOpenRoadmap = { detailId = it },
            themeMode = themeMode,
            onSetThemeMode = onSetThemeMode,
            modifier = modifier,
        )
    } else {
        RoadmapDetailRoute(repository, current, onBack = { detailId = null }, modifier = modifier)
    }
}
