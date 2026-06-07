package com.example.roadmap.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.RoadmapRepository

@Composable
fun RoadmapDetailRoute(
    repository: RoadmapRepository,
    roadmapId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: RoadmapDetailViewModel = viewModel(
        factory = RoadmapDetailViewModelFactory(repository, roadmapId),
        key = "detail-$roadmapId",
    )
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val tree = state.roadmap

    if (tree == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val cb = DetailCallbacks(
        onBack = onBack,
        onEditRoadmap = { /* Task 4 */ },
        onArchive = { vm.setArchived(!tree.roadmap.archived) },
        onDeleteRoadmap = { vm.deleteRoadmap(); onBack() },
        onAddMilestone = { /* Task 4 */ },
        onEditMilestone = { /* Task 4 */ },
        onDeleteMilestone = { vm.deleteMilestone(it) },
        onAddStep = { /* Task 4 */ },
        onEditStep = { /* Task 4 */ },
        onToggleStep = { id, completed -> vm.setStepCompleted(id, completed) },
        onOpenLink = { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        },
    )
    RoadmapDetailScreen(tree, cb, modifier)
}
