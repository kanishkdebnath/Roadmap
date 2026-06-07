# Roadmap Detail Screen Implementation Plan (Phase 5)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the **Roadmap Detail screen** (spec F2–F7): header (title/description/ring/deadline + Edit/Archive/Delete), milestone cards with derived progress + overdue, step rows (checkbox toggle, link chips that open the browser), inline add for milestones/steps, the create/edit/delete dialogs, and **navigation** from the list into detail and back.

**Architecture:** A `RoadmapDetailViewModel` observes `repository.observeRoadmap(id)` (the sorted `RoadmapWithChildren` tree) and exposes every mutation (delegating to the Phase-2 repository, which already does the transactional work + completion recompute). The Compose detail screen composes the Phase-1 design system. **Navigation is a lightweight manual state machine** — a top-level `RoadmapApp` holds the current route (List or Detail(id)) with a `BackHandler` — chosen over navigation-compose, which only ships an alpha at this version. Drag-reorder is **Phase 6**; the import flow is **Phase 7**.

**Tech Stack:** Kotlin · Compose + Material 3 · ViewModel + StateFlow (Phase 4 deps) · Room repository (Phase 2) · domain helpers (Phase 3) · `Intent.ACTION_VIEW` for links. ViewModel is Robolectric-tested; UI is verified by `@Preview`, compile, **and on the running emulator** (`adb`).

**Conventions:**
- Branch `feat/detail-screen` (created by the controller; plan committed there). Stay on it; never `main`. Finishes as a **GitHub PR**.
- Package `ui/detail/` for the screen + VM; `ui/RoadmapApp.kt` for navigation.
- Reuse existing components/signatures (verified):
  - `RingProgress(progress: Float, modifier, size: RingSize)`; `MetaChip(text, icon?, modifier)`; `DeadlineChip(text, state: DeadlineState, modifier)`; `LinkChip(label, modifier, onClick)`; `StepCheckbox(checked, onToggle, modifier)`; `DragGrip(modifier)`; `AddInline(text, onClick, modifier)`; `RoadmapFab/PrimaryButton/SecondaryButton/DangerButton(text, onClick, modifier)`.
  - domain: `RoadmapWithChildren.progress()/totalSteps()/completedSteps()/isOverdue(today)`, `MilestoneWithSteps.progress()/totalSteps()/completedSteps()/isOverdue(today)`; `ui/format/formatDeadline(iso)`.
  - repo: `observeRoadmap(id)`, `updateRoadmap`, `setArchived`, `deleteRoadmap`, `addMilestone`, `updateMilestone`, `deleteMilestone`, `addStep`, `updateStepTitle`, `setStepCompleted`, `deleteStep`, `setStepLinks(stepId, List<LinkDraft>)`.
- Run tests: `./gradlew :app:testDebugUnitTest`. Build/install: `./gradlew :app:installDebug` then drive the emulator with `adb` (stylus handwriting is disabled; capture screenshots via `adb shell screencap -p /sdcard/s.png && adb pull`).

---

## File structure (this phase)

```
app/src/main/java/com/example/roadmap/
  ui/detail/RoadmapDetailViewModel.kt   (create: UiState + VM + Factory)
  ui/detail/RoadmapDetailScreen.kt      (create: stateless screen + milestone/step composables + previews)
  ui/detail/DetailDialogs.kt            (create: New/Edit Milestone, Edit Step, Edit Roadmap, Delete confirms)
  ui/detail/RoadmapDetailRoute.kt       (create: stateful host owning dialogs + link opening)
  ui/RoadmapApp.kt                      (create: manual List<->Detail navigation)
  MainActivity.kt                       (modify: host RoadmapApp)
app/src/test/java/com/example/roadmap/
  ui/detail/RoadmapDetailViewModelTest.kt   (Robolectric)
```

---

## Task 1: RoadmapDetailViewModel

**Files:** Create `ui/detail/RoadmapDetailViewModel.kt`; Test `ui/detail/RoadmapDetailViewModelTest.kt`.

- [ ] **Step 1: Create RoadmapDetailViewModel.kt**
```kotlin
package com.example.roadmap.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.RoadmapRepository
import com.example.roadmap.data.relation.RoadmapWithChildren
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoadmapDetailUiState(val roadmap: RoadmapWithChildren? = null)

class RoadmapDetailViewModel(
    private val repository: RoadmapRepository,
    private val roadmapId: Long,
) : ViewModel() {

    val uiState: StateFlow<RoadmapDetailUiState> =
        kotlinx.coroutines.flow.MutableStateFlow(RoadmapDetailUiState()).let { _ ->
            repository.observeRoadmap(roadmapId)
                .let { flow -> flow }
                .let { flow ->
                    kotlinx.coroutines.flow.map(flow) { RoadmapDetailUiState(it) }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoadmapDetailUiState())

    // roadmap
    fun updateRoadmap(title: String, description: String?, deadline: String?) =
        launch { repository.updateRoadmap(roadmapId, title, description, deadline) }
    fun setArchived(archived: Boolean) = launch { repository.setArchived(roadmapId, archived) }
    fun deleteRoadmap() = launch { repository.deleteRoadmap(roadmapId) }

    // milestones
    fun addMilestone(title: String) = launch { repository.addMilestone(roadmapId, title) }
    fun updateMilestone(id: Long, title: String, description: String?, deadline: String?) =
        launch { repository.updateMilestone(id, title, description, deadline) }
    fun deleteMilestone(id: Long) = launch { repository.deleteMilestone(id) }

    // steps
    fun addStep(milestoneId: Long, title: String) = launch { repository.addStep(milestoneId, title) }
    fun updateStepTitle(id: Long, title: String) = launch { repository.updateStepTitle(id, title) }
    fun setStepCompleted(id: Long, completed: Boolean) = launch { repository.setStepCompleted(id, completed) }
    fun deleteStep(id: Long) = launch { repository.deleteStep(id) }
    fun setStepLinks(stepId: Long, links: List<LinkDraft>) = launch { repository.setStepLinks(stepId, links) }

    private inline fun launch(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

class RoadmapDetailViewModelFactory(
    private val repository: RoadmapRepository,
    private val roadmapId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoadmapDetailViewModel(repository, roadmapId) as T
}
```
> Implementer note: the `uiState` initializer above is intentionally written plainly — simplify it to the idiomatic form:
> ```kotlin
> val uiState: StateFlow<RoadmapDetailUiState> =
>     repository.observeRoadmap(roadmapId)
>         .map { RoadmapDetailUiState(it) }
>         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoadmapDetailUiState())
> ```
> with `import kotlinx.coroutines.flow.map`. Use that clean form; drop the nested `.let` scaffolding.

- [ ] **Step 2: Write the Robolectric test** — `app/src/test/java/com/example/roadmap/ui/detail/RoadmapDetailViewModelTest.kt`:
```kotlin
package com.example.roadmap.ui.detail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.RoomRoadmapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapDetailViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomRoadmapRepository
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        repo = RoomRoadmapRepository(db) { t++ }
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun observes_tree_and_toggles_step_completion() = runTest(dispatcher) {
        val rid = repo.createRoadmap("Goal")
        val mid = repo.addMilestone(rid, "M")
        val sid = repo.addStep(mid, "S")
        val vm = RoadmapDetailViewModel(repo, rid)

        advanceUntilIdle()
        val tree = vm.uiState.first { it.roadmap != null }.roadmap!!
        assertEquals("Goal", tree.roadmap.title)
        assertEquals(1, tree.milestones.size)
        assertEquals("S", tree.milestones[0].steps[0].step.title)

        vm.setStepCompleted(sid, true)
        advanceUntilIdle()
        val after = vm.uiState.first { it.roadmap?.milestones?.firstOrNull()?.steps?.firstOrNull()?.step?.completed == true }
        assertNotNull(after.roadmap!!.milestones[0].milestone.completedAt)  // milestone auto-completes (F6)
    }
}
```

- [ ] **Step 3: Run + verify** — `./gradlew :app:testDebugUnitTest --tests "*RoadmapDetailViewModelTest"` → PASS.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/roadmap/ui/detail/RoadmapDetailViewModel.kt app/src/test/java/com/example/roadmap/ui/detail/RoadmapDetailViewModelTest.kt
git commit -m "ui: RoadmapDetailViewModel (observe tree + mutations over repository)"
```

---

## Task 2: Detail screen (read + step toggle)

**Files:** Create `ui/detail/RoadmapDetailScreen.kt`.

> Stateless screen taking the tree + callbacks. Milestone/step derived values use the Phase-3 domain extensions. `LocalDate.now()` for overdue. Per-milestone overflow menu (Edit/Delete) and per-step row click are wired to callbacks (dialogs come in Task 4).

- [ ] **Step 1: Create RoadmapDetailScreen.kt**
```kotlin
package com.example.roadmap.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren
import com.example.roadmap.data.relation.StepWithLinks
import com.example.roadmap.domain.*
import com.example.roadmap.ui.components.*
import com.example.roadmap.ui.format.formatDeadline
import com.example.roadmap.ui.theme.RoadmapTheme
import java.time.LocalDate

class DetailCallbacks(
    val onBack: () -> Unit,
    val onEditRoadmap: () -> Unit,
    val onArchive: () -> Unit,
    val onDeleteRoadmap: () -> Unit,
    val onAddMilestone: () -> Unit,
    val onEditMilestone: (MilestoneEntity) -> Unit,
    val onDeleteMilestone: (Long) -> Unit,
    val onAddStep: (milestoneId: Long) -> Unit,
    val onEditStep: (StepWithLinks) -> Unit,
    val onToggleStep: (id: Long, completed: Boolean) -> Unit,
    val onOpenLink: (url: String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapDetailScreen(tree: RoadmapWithChildren, cb: DetailCallbacks, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val r = tree.roadmap
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(cb.onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { inner ->
        LazyColumn(
            Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.headlineMedium)
                                if (!r.description.isNullOrBlank()) {
                                    Text(r.description, style = MaterialTheme.typography.bodyMedium,
                                        color = RoadmapTheme.colors.muted, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            RingProgress(tree.progress(), size = RingSize.Large)
                        }
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetaChip("${tree.completedSteps()} / ${tree.totalSteps()} steps")
                            val dl = formatDeadline(r.deadline)
                            if (dl != null) {
                                val overdue = tree.isOverdue(today)
                                DeadlineChip(if (overdue) "Overdue · $dl" else "Due $dl",
                                    if (overdue) DeadlineState.Overdue else DeadlineState.Normal)
                            }
                        }
                        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton("Edit", cb.onEditRoadmap, Modifier.weight(1f))
                            SecondaryButton(if (r.archived) "Unarchive" else "Archive", cb.onArchive, Modifier.weight(1f))
                            DangerButton("Delete", cb.onDeleteRoadmap)
                        }
                    }
                }
            }
            item {
                Text("Milestones · ${tree.milestones.size}", style = MaterialTheme.typography.labelMedium,
                    color = RoadmapTheme.colors.faint,
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 4.dp))
            }
            items(tree.milestones, key = { it.milestone.id }) { m ->
                MilestoneCard(m, today, cb, Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            }
            item {
                AddInline("Add milestone", cb.onAddMilestone,
                    Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MilestoneCard(m: MilestoneWithSteps, today: LocalDate, cb: DetailCallbacks, modifier: Modifier) {
    val done = m.milestone.completedAt != null
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.milestone.title, style = MaterialTheme.typography.titleSmall,
                        color = if (done) RoadmapTheme.colors.muted else MaterialTheme.colorScheme.onSurface)
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${m.completedSteps()} / ${m.totalSteps()} steps",
                            style = MaterialTheme.typography.bodySmall, color = RoadmapTheme.colors.muted)
                        val dl = formatDeadline(m.milestone.deadline)
                        if (dl != null && m.isOverdue(today)) DeadlineChip("Overdue · $dl", DeadlineState.Overdue)
                    }
                }
                RingProgress(m.progress(), size = RingSize.Small)
                MilestoneMenu(onEdit = { cb.onEditMilestone(m.milestone) }, onDelete = { cb.onDeleteMilestone(m.milestone.id) })
            }
            Column(Modifier.padding(top = 4.dp)) {
                m.steps.forEach { s -> StepRow(s, cb) }
                AddInline("Add step", { cb.onAddStep(m.milestone.id) }, Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Rounded.MoreVert, "Milestone actions") }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { open = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun StepRow(s: StepWithLinks, cb: DetailCallbacks) {
    Row(
        Modifier.fillMaxWidth().clickable { cb.onEditStep(s) }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        StepCheckbox(s.step.completed, { cb.onToggleStep(s.step.id, !s.step.completed) })
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.step.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (s.step.completed) RoadmapTheme.colors.faint else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (s.step.completed) TextDecoration.LineThrough else null,
                overflow = TextOverflow.Ellipsis,
            )
            if (s.links.isNotEmpty()) {
                Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.links.forEach { l -> LinkChip(l.label ?: l.url, onClick = { cb.onOpenLink(l.url) }) }
                }
            }
        }
    }
}

// ---- preview ----
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun DetailPreview() {
    val tree = com.example.roadmap.ui.detail.sampleTree()
    RoadmapTheme { RoadmapDetailScreen(tree, com.example.roadmap.ui.detail.noopCallbacks()) }
}
```

- [ ] **Step 2: Add preview helpers** at the bottom of `RoadmapDetailScreen.kt` (sample tree + no-op callbacks so the preview compiles):
```kotlin
internal fun sampleTree(): RoadmapWithChildren {
    val r = com.example.roadmap.data.entity.RoadmapEntity(id = 1, title = "Learn Kotlin & Compose",
        description = "From basics to a Material 3 app.", deadline = "2026-09-30")
    fun step(id: Long, t: String, done: Boolean) =
        StepWithLinks(com.example.roadmap.data.entity.StepEntity(id = id, milestoneId = 1, title = t, completed = done, position = id.toInt()),
            if (id == 1L) listOf(com.example.roadmap.data.entity.LinkEntity(stepId = 1, url = "https://developer.android.com", position = 0)) else emptyList())
    val m1 = MilestoneWithSteps(com.example.roadmap.data.entity.MilestoneEntity(id = 1, roadmapId = 1, title = "Compose Basics", position = 0,
        completedAt = null), listOf(step(1, "Composables", true), step(2, "State", false)))
    return RoadmapWithChildren(r, listOf(m1))
}
internal fun noopCallbacks() = DetailCallbacks({}, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})
```

- [ ] **Step 3: Compile + preview** — `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`. Render the preview in Android Studio.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/roadmap/ui/detail/RoadmapDetailScreen.kt
git commit -m "ui: roadmap detail screen (header, milestone cards, step rows, toggle)"
```

---

## Task 3: Navigation (RoadmapApp) + wire MainActivity

**Files:** Create `ui/RoadmapApp.kt`; Create `ui/detail/RoadmapDetailRoute.kt` (minimal, no dialogs yet); Modify `MainActivity.kt`.

- [ ] **Step 1: Create a minimal `RoadmapDetailRoute.kt`** (Task 4 adds the dialogs)
```kotlin
package com.example.roadmap.ui.detail

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.RoadmapRepository
import android.content.Intent
import androidx.core.net.toUri

@Composable
fun RoadmapDetailRoute(repository: RoadmapRepository, roadmapId: Long, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
```

- [ ] **Step 2: Create RoadmapApp.kt** (manual nav)
```kotlin
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
import com.example.roadmap.ui.detail.RoadmapDetailRoute
import com.example.roadmap.ui.list.RoadmapListRoute
import com.example.roadmap.ui.list.RoadmapListViewModel
import com.example.roadmap.ui.list.RoadmapListViewModelFactory

@Composable
fun RoadmapApp(repository: RoadmapRepository, modifier: Modifier = Modifier) {
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = detailId != null) { detailId = null }

    val current = detailId
    if (current == null) {
        val listVm: RoadmapListViewModel = viewModel(factory = RoadmapListViewModelFactory(repository))
        RoadmapListRoute(listVm, onOpenRoadmap = { detailId = it }, modifier = modifier)
    } else {
        RoadmapDetailRoute(repository, current, onBack = { detailId = null }, modifier = modifier)
    }
}
```

- [ ] **Step 3: Replace MainActivity.kt to host RoadmapApp**
```kotlin
package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.roadmap.data.RoadmapGraph
import com.example.roadmap.ui.RoadmapApp
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = RoadmapGraph.repository(applicationContext)
        setContent {
            RoadmapTheme { RoadmapApp(repository, Modifier.fillMaxSize()) }
        }
    }
}
```

- [ ] **Step 4: Build + emulator smoke** — `./gradlew :app:installDebug`, launch, create a roadmap, tap it → detail opens; toggle a step → ring updates; system back → returns to list. Capture a screenshot.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt app/src/main/java/com/example/roadmap/ui/detail/RoadmapDetailRoute.kt app/src/main/java/com/example/roadmap/MainActivity.kt
git commit -m "ui: manual list<->detail navigation + wire detail route (toggle, links, delete)"
```

---

## Task 4: Dialogs (create/edit/delete) + wire the route

**Files:** Create `ui/detail/DetailDialogs.kt`; Modify `ui/detail/RoadmapDetailRoute.kt`.

- [ ] **Step 1: Create DetailDialogs.kt**
```kotlin
package com.example.roadmap.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.relation.StepWithLinks

/** New roadmap / milestone, and Edit roadmap / milestone all share this title+description+deadline form. */
@Composable
fun EditEntityDialog(
    dialogTitle: String,
    initialTitle: String = "",
    initialDescription: String? = null,
    initialDeadline: String? = null,
    showDescription: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, deadline: String?) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    var deadline by remember { mutableStateOf(initialDeadline ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(),
                onClick = { onConfirm(title.trim(), description.trim().ifBlank { null }, deadline.trim().ifBlank { null }) }) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (showDescription) OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(deadline, { deadline = it }, label = { Text("Deadline YYYY-MM-DD (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

/** Edit a step's title + its links; "Delete step" lives here too. */
@Composable
fun EditStepDialog(
    step: StepWithLinks,
    onDismiss: () -> Unit,
    onSave: (title: String, links: List<LinkDraft>) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(step.step.title) }
    val links = remember { mutableStateListOf<Pair<String, String>>().apply { addAll(step.links.map { it.url to (it.label ?: "") }) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit step") },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && links.all { it.first.isBlank() || it.first.startsWith("http://") || it.first.startsWith("https://") },
                onClick = {
                    val drafts = links.filter { it.first.isNotBlank() }.map { LinkDraft(it.first.trim(), it.second.trim().ifBlank { null }) }
                    onSave(title.trim(), drafts)
                }) { Text("Save") }
        },
        dismissButton = { TextButton(onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Links", style = MaterialTheme.typography.labelMedium)
                links.forEachIndexed { i, (url, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(url, { links[i] = it to label }, label = { Text("https://…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(label, { links[i] = url to it }, label = { Text("Label (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        IconButton({ links.removeAt(i) }) { Icon(Icons.Rounded.Close, "Remove link") }
                    }
                }
                TextButton({ links.add("" to "") }) { Icon(Icons.Rounded.Add, null); Text("Add link") }
                Text("Links must start with http:// or https://", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton({ onConfirm() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
```

- [ ] **Step 2: Wire the dialogs into `RoadmapDetailRoute.kt`** — replace the file body with a version that owns dialog state. Add a sealed dialog state and render the right dialog. Full replacement:
```kotlin
package com.example.roadmap.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
fun RoadmapDetailRoute(repository: RoadmapRepository, roadmapId: Long, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: RoadmapDetailViewModel = viewModel(factory = RoadmapDetailViewModelFactory(repository, roadmapId), key = "detail-$roadmapId")
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
            onAddStep = { dialog = DetailDialog.NewStep(it) },
            onEditStep = { dialog = DetailDialog.EditStep(it) },
            onToggleStep = { id, completed -> vm.setStepCompleted(id, completed) },
            onOpenLink = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } },
        ),
        modifier,
    )

    when (val d = dialog) {
        null -> Unit
        DetailDialog.EditRoadmap -> EditEntityDialog("Edit roadmap", tree.roadmap.title, tree.roadmap.description, tree.roadmap.deadline,
            onDismiss = { dialog = null }, onConfirm = { t, de, dl -> vm.updateRoadmap(t, de, dl); dialog = null })
        DetailDialog.NewMilestone -> EditEntityDialog("New milestone", showDescription = false,
            onDismiss = { dialog = null }, onConfirm = { t, _, _ -> vm.addMilestone(t); dialog = null })
        is DetailDialog.EditMilestone -> EditEntityDialog("Edit milestone", d.milestone.title, d.milestone.description, d.milestone.deadline,
            onDismiss = { dialog = null }, onConfirm = { t, de, dl -> vm.updateMilestone(d.milestone.id, t, de, dl); dialog = null })
        is DetailDialog.NewStep -> EditEntityDialog("New step", showDescription = false,
            onDismiss = { dialog = null }, onConfirm = { t, _, _ -> vm.addStep(d.milestoneId, t); dialog = null })
        is DetailDialog.EditStep -> EditStepDialog(d.step,
            onDismiss = { dialog = null },
            onSave = { t, links -> vm.updateStepTitle(d.step.step.id, t); vm.setStepLinks(d.step.step.id, links); dialog = null },
            onDelete = { vm.deleteStep(d.step.step.id); dialog = null })
        DetailDialog.DeleteRoadmap -> ConfirmDeleteDialog("Delete roadmap?",
            "“${tree.roadmap.title}” and all its milestones, steps, and links will be permanently removed.",
            onDismiss = { dialog = null }, onConfirm = { dialog = null; vm.deleteRoadmap(); onBack() })
        is DetailDialog.DeleteMilestone -> ConfirmDeleteDialog("Delete milestone?",
            "This milestone and its steps will be permanently removed.",
            onDismiss = { dialog = null }, onConfirm = { vm.deleteMilestone(d.id); dialog = null })
    }
}
```

- [ ] **Step 3: Build + emulator** — `./gradlew :app:installDebug`; verify: add a milestone, add a step, edit step links, delete a milestone, edit the roadmap. Screenshot the populated detail.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/roadmap/ui/detail/DetailDialogs.kt app/src/main/java/com/example/roadmap/ui/detail/RoadmapDetailRoute.kt
git commit -m "ui: detail dialogs (new/edit milestone+step+links, edit roadmap, delete confirms)"
```

---

## Task 5: Wrap-up + on-device verification

- [ ] **Step 1: Full sweep** — `./gradlew :app:testDebugUnitTest :app:assembleDebug` → all pass, `BUILD SUCCESSFUL`.
- [ ] **Step 2: Confirm `core-ktx` provides `toUri`** — `androidx.core.net.toUri` is in `androidx.core:core-ktx` (already a dependency). If unresolved, use `android.net.Uri.parse(url)` instead.
- [ ] **Step 3: On-device** — `./gradlew :app:installDebug`; drive the full flow (create roadmap → open → add milestone → add steps → toggle → edit links → open a link → back). Capture light + dark (`adb shell "cmd uimode night yes|no"`) screenshots.

---

## Self-Review (completed during planning)

- **F2 (roadmap CRUD):** Edit (EditEntityDialog), Archive/Unarchive (`setArchived`), Delete with confirm + cascade (`deleteRoadmap`, repo cascades) — Tasks 2,4,5 ✓.
- **F3 (milestones):** add (inline → NewMilestone), edit, delete (cascade) via the milestone overflow menu + dialogs — Tasks 2,4 ✓. Reorder is **Phase 6** (drag) — out of scope.
- **F4 (steps):** add (inline), edit title (EditStep), toggle complete (`setStepCompleted` → milestone auto-completes via repo F6), delete (in EditStep) — Tasks 2,4 ✓. Reorder → Phase 6.
- **F5 (links):** 0–N links per step edited in EditStepDialog (url+label), http/https validation on save, render as chips, tap opens browser via `ACTION_VIEW` — Tasks 2,4 ✓.
- **F6 (derived completion):** handled by the repository (Phase 2); the VM test asserts a milestone auto-completes when its last step is checked.
- **F7 (progress & overdue):** roadmap + milestone rings via `progress()`, overdue badges via `isOverdue(today)` — Task 2 ✓.
- **Navigation:** manual List↔Detail with `BackHandler`; delete-roadmap navigates back — Tasks 3,4 ✓.
- **Type consistency:** `RoadmapDetailUiState(roadmap)`, VM methods match repo signatures, `DetailCallbacks` fields used consistently across screen/route, dialog composables (`EditEntityDialog`/`EditStepDialog`/`ConfirmDeleteDialog`) — all defined before use. Reuses verified component + domain signatures.
- **Deferred (tracked):** drag-reorder (Phase 6); import/export (Phase 7); a date-picker (free-text date field for now, degrades gracefully via `formatDeadline`/`isOverdue`). UI verified by preview + compile + emulator; VM by Robolectric.
```
