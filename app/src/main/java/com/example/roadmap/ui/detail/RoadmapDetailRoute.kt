package com.example.roadmap.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.RoadmapRepository
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.relation.StepWithLinks

private sealed interface DetailDialog {
    data object EditRoadmap : DetailDialog
    data object NewMilestone : DetailDialog
    data class EditMilestone(val milestone: MilestoneEntity) : DetailDialog
    data class NewStep(val milestoneId: Long) : DetailDialog
    data class EditStep(val step: StepWithLinks) : DetailDialog
    data object DeleteRoadmap : DetailDialog
    data class DeleteMilestone(val id: Long) : DetailDialog
}

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
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }
    val tree = state.roadmap

    if (tree == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    RoadmapDetailScreen(
        tree,
        DetailCallbacks(
            onBack = onBack,
            onEditRoadmap = { dialog = DetailDialog.EditRoadmap },
            onArchive = { vm.setArchived(!tree.roadmap.archived) },
            onDeleteRoadmap = { dialog = DetailDialog.DeleteRoadmap },
            onAddMilestone = { dialog = DetailDialog.NewMilestone },
            onEditMilestone = { dialog = DetailDialog.EditMilestone(it) },
            onDeleteMilestone = { dialog = DetailDialog.DeleteMilestone(it) },
            onReorderMilestones = { ids -> vm.reorderMilestones(ids) },
            onAddStep = { dialog = DetailDialog.NewStep(it) },
            onEditStep = { dialog = DetailDialog.EditStep(it) },
            onToggleStep = { id, completed -> vm.setStepCompleted(id, completed) },
            onOpenLink = { url ->
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            },
        ),
        modifier,
    )

    when (val d = dialog) {
        null -> Unit
        DetailDialog.EditRoadmap -> EditEntityDialog(
            "Edit roadmap",
            tree.roadmap.title,
            tree.roadmap.description,
            tree.roadmap.deadline,
            onDismiss = { dialog = null },
            onConfirm = { t, de, dl -> vm.updateRoadmap(t, de, dl); dialog = null },
        )
        DetailDialog.NewMilestone -> EditEntityDialog(
            "New milestone",
            showDescription = false,
            onDismiss = { dialog = null },
            onConfirm = { t, _, _ -> vm.addMilestone(t); dialog = null },
        )
        is DetailDialog.EditMilestone -> EditEntityDialog(
            "Edit milestone",
            d.milestone.title,
            d.milestone.description,
            d.milestone.deadline,
            onDismiss = { dialog = null },
            onConfirm = { t, de, dl -> vm.updateMilestone(d.milestone.id, t, de, dl); dialog = null },
        )
        is DetailDialog.NewStep -> EditEntityDialog(
            "New step",
            showDescription = false,
            onDismiss = { dialog = null },
            onConfirm = { t, _, _ -> vm.addStep(d.milestoneId, t); dialog = null },
        )
        is DetailDialog.EditStep -> EditStepDialog(
            d.step,
            onDismiss = { dialog = null },
            onSave = { t, links ->
                vm.updateStepTitle(d.step.step.id, t)
                vm.setStepLinks(d.step.step.id, links)
                dialog = null
            },
            onDelete = { vm.deleteStep(d.step.step.id); dialog = null },
        )
        DetailDialog.DeleteRoadmap -> ConfirmDeleteDialog(
            "Delete roadmap?",
            "“${tree.roadmap.title}” and all its milestones, steps, and links will be permanently removed.",
            onDismiss = { dialog = null },
            onConfirm = { dialog = null; vm.deleteRoadmap(); onBack() },
        )
        is DetailDialog.DeleteMilestone -> ConfirmDeleteDialog(
            "Delete milestone?",
            "This milestone and its steps will be permanently removed.",
            onDismiss = { dialog = null },
            onConfirm = { vm.deleteMilestone(d.id); dialog = null },
        )
    }
}
