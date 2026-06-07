# Roadmaps List Screen Implementation Plan (Phase 4)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Roadmaps **list screen** (spec F1) — segmented Active/Archive, search across title/description/milestone-titles, a card grid with progress rings + deadline/overdue badges, empty/no-results states, a New-roadmap dialog (F2 create), and an Import entry point (F8 stub) — backed by a `RoadmapListViewModel` over the Phase-2 repository.

**Architecture:** Add a reactive **card-projection** query to the data layer (`RoadmapCard` = roadmap + milestone/step aggregate counts), with search baked into SQL. `RoadmapListViewModel` exposes a `StateFlow<RoadmapListUiState>` derived from scope + query via `flatMapLatest`. The Compose `RoadmapListScreen` composes the **existing Phase-1 design-system components** (RingProgress, SegmentedControl, GradientTile, chips, FAB, EmptyState) — it adds layout, not new visual primitives. **Navigation is deferred to Phase 5**; the list exposes an `onOpenRoadmap(id)` callback (no-op for now). The detail screen, drag-reorder, and the full import flow are later phases.

**Tech Stack:** Kotlin · Compose + Material 3 · `androidx.lifecycle:lifecycle-viewmodel-compose` (2.10.0) · ViewModel + StateFlow · Room (Phase 2) · domain helpers (Phase 3). ViewModel/query tests run on the JVM under Robolectric; UI is verified by `@Preview` + compile (no emulator).

**Conventions:**
- Branch `feat/list-screen` (already created by the controller; the plan is committed there). Stay on it; never `main`. Finishes as a **GitHub PR**, not a local merge.
- Packages: data additions in `data/`, screen in `ui/list/`, small format util in `ui/format/`.
- Run tests: `./gradlew :app:testDebugUnitTest`. Full: `./gradlew :app:assembleDebug`.
- Reuse existing components — do NOT rebuild rings/chips/etc. Existing signatures:
  - `RingProgress(progress: Float, modifier, size: RingSize)` (`RingSize.Small/Medium/Large`)
  - `SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier)`
  - `GradientTile(initial: Char, hue: RoadmapHue, modifier, size)`; `hueForId(id: Long): RoadmapHue`
  - `MetaChip(text, icon: ImageVector? = null, modifier)`; `DeadlineChip(text, state: DeadlineState, modifier)` (`DeadlineState.Normal/Overdue/Done`)
  - `RoadmapFab(text, onClick, modifier)`; `PrimaryButton/SecondaryButton(text, onClick, modifier)`
  - `EmptyState(icon: ImageVector, title, message, hue: RoadmapHue, modifier, actions: @Composable (() -> Unit)?)`
  - `RoadmapTheme.colors` (e.g. `.muted`, `.faint`); domain: `progressFraction(Int,Int)`, `isOverdue(String?,Boolean,LocalDate)`.

---

## File structure (this phase)

```
app/build.gradle.kts                                  (modify: add lifecycle-viewmodel-compose + -ktx)
app/src/main/java/com/example/roadmap/
  data/relation/RoadmapCard.kt          (create: card projection POJO)
  data/dao/RoadmapDao.kt                (modify: add observeCards query)
  data/RoadmapRepository.kt             (modify: add observeRoadmapCards to interface)
  data/RoomRoadmapRepository.kt         (modify: implement observeRoadmapCards)
  ui/format/DateFormat.kt               (create: formatDeadline)
  ui/list/RoadmapListViewModel.kt       (create: UiState + ViewModel + Factory)
  ui/list/RoadmapListScreen.kt          (create: screen + card item + dialog + empty states)
  MainActivity.kt                       (modify: show the list)
app/src/test/java/com/example/roadmap/
  data/RoadmapCardQueryTest.kt          (Robolectric: counts + search + filter)
  ui/format/DateFormatTest.kt           (JVM)
  ui/list/RoadmapListViewModelTest.kt   (Robolectric: real repo + VM)
```

---

## Task 1: Card-projection + search query (data)

**Files:** Create `data/relation/RoadmapCard.kt`; Modify `data/dao/RoadmapDao.kt`; Modify `data/RoadmapRepository.kt` + `data/RoomRoadmapRepository.kt`; Test `data/RoadmapCardQueryTest.kt`.

- [ ] **Step 1: Create RoadmapCard.kt**
```kotlin
package com.example.roadmap.data.relation

import androidx.room.Embedded
import com.example.roadmap.data.entity.RoadmapEntity

/** List-card projection: a roadmap plus aggregate counts for its ring + milestone badge. */
data class RoadmapCard(
    @Embedded val roadmap: RoadmapEntity,
    val milestoneCount: Int,
    val totalSteps: Int,
    val completedSteps: Int,
)
```

- [ ] **Step 2: Add `observeCards` to RoadmapDao.kt**
Add `import com.example.roadmap.data.relation.RoadmapCard` and this method (LIKE is ASCII case-insensitive in SQLite; empty query matches all):
```kotlin
    @Query("""
        SELECT r.*,
          (SELECT COUNT(*) FROM milestone m WHERE m.roadmapId = r.id) AS milestoneCount,
          (SELECT COUNT(*) FROM step s JOIN milestone m ON s.milestoneId = m.id WHERE m.roadmapId = r.id) AS totalSteps,
          (SELECT COUNT(*) FROM step s JOIN milestone m ON s.milestoneId = m.id WHERE m.roadmapId = r.id AND s.completed = 1) AS completedSteps
        FROM roadmap r
        WHERE r.archived = :archived
          AND (
            :query = ''
            OR r.title LIKE '%' || :query || '%'
            OR r.description LIKE '%' || :query || '%'
            OR EXISTS (SELECT 1 FROM milestone m WHERE m.roadmapId = r.id AND m.title LIKE '%' || :query || '%')
          )
        ORDER BY r.updatedAt DESC
    """)
    fun observeCards(archived: Boolean, query: String): Flow<List<RoadmapCard>>
```

- [ ] **Step 3: Add to repository interface (`RoadmapRepository.kt`)**
Add `import com.example.roadmap.data.relation.RoadmapCard` and, near `observeRoadmaps`:
```kotlin
    fun observeRoadmapCards(archived: Boolean, query: String): Flow<List<RoadmapCard>>
```

- [ ] **Step 4: Implement in `RoomRoadmapRepository.kt`**
Add `import com.example.roadmap.data.relation.RoadmapCard` and:
```kotlin
    override fun observeRoadmapCards(archived: Boolean, query: String): Flow<List<RoadmapCard>> =
        roadmaps.observeCards(archived, query)
```

- [ ] **Step 5: Write the Robolectric query test** — `app/src/test/java/com/example/roadmap/data/RoadmapCardQueryTest.kt`:
```kotlin
package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapCardQueryTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomRoadmapRepository
    private var clock = 1L
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        repo = RoomRoadmapRepository(db) { clock++ }   // increasing clock => deterministic updatedAt order
    }
    @After fun teardown() = db.close()

    @Test fun card_has_aggregate_counts() = runTest {
        val rid = repo.createRoadmap("Learn Kotlin")
        val m1 = repo.addMilestone(rid, "Basics")
        repo.addMilestone(rid, "Compose")
        val s1 = repo.addStep(m1, "s1"); repo.addStep(m1, "s2")
        repo.setStepCompleted(s1, true)
        val card = repo.observeRoadmapCards(archived = false, query = "").first().single()
        assertEquals(2, card.milestoneCount)
        assertEquals(2, card.totalSteps)
        assertEquals(1, card.completedSteps)
    }

    @Test fun search_matches_title_description_and_milestone_title() = runTest {
        val a = repo.createRoadmap("Run a marathon", description = "fitness goal")
        repo.addMilestone(a, "Long runs")
        val b = repo.createRoadmap("Learn Spanish")
        repo.addMilestone(b, "Vocabulary")

        // milestone title match -> surfaces parent
        assertEquals(listOf("Run a marathon"),
            repo.observeRoadmapCards(false, "long").first().map { it.roadmap.title })
        // description match
        assertEquals(listOf("Run a marathon"),
            repo.observeRoadmapCards(false, "fitness").first().map { it.roadmap.title })
        // title match
        assertEquals(listOf("Learn Spanish"),
            repo.observeRoadmapCards(false, "spanish").first().map { it.roadmap.title })
        // no match
        assertEquals(0, repo.observeRoadmapCards(false, "zzz").first().size)
    }

    @Test fun archived_scope_filters() = runTest {
        val a = repo.createRoadmap("Active one")
        repo.createRoadmap("To archive").also { repo.setArchived(it, true) }
        assertEquals(listOf("Active one"), repo.observeRoadmapCards(false, "").first().map { it.roadmap.title })
        assertEquals(listOf("To archive"), repo.observeRoadmapCards(true, "").first().map { it.roadmap.title })
    }
}
```

- [ ] **Step 6: Run + verify** — `./gradlew :app:testDebugUnitTest --tests "*RoadmapCardQueryTest"` → PASS (3 tests).

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/example/roadmap/data app/src/test/java/com/example/roadmap/data/RoadmapCardQueryTest.kt
git commit -m "data: reactive card-projection query with search (title/description/milestone)"
```

---

## Task 2: Deadline formatting (JVM, TDD)

**Files:** Create `ui/format/DateFormat.kt`; Test `ui/format/DateFormatTest.kt`.

- [ ] **Step 1: Write the failing test** — `app/src/test/java/com/example/roadmap/ui/format/DateFormatTest.kt`:
```kotlin
package com.example.roadmap.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DateFormatTest {
    @Before fun setup() { Locale.setDefault(Locale.US) }

    @Test fun formats_iso_date_as_month_day() {
        assertEquals("Sep 30", formatDeadline("2026-09-30"))
        assertEquals("Jan 5", formatDeadline("2026-01-05"))
    }
    @Test fun null_or_invalid_is_null() {
        assertNull(formatDeadline(null))
        assertNull(formatDeadline("nope"))
    }
}
```

- [ ] **Step 2: Run → FAIL.**

- [ ] **Step 3: Create DateFormat.kt**
```kotlin
package com.example.roadmap.ui.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "2026-09-30" -> "Sep 30" (device locale); null/unparseable -> null. */
fun formatDeadline(iso: String?): String? {
    if (iso == null) return null
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return null
    return date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}
```

- [ ] **Step 4: Run → PASS.**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/roadmap/ui/format/DateFormat.kt app/src/test/java/com/example/roadmap/ui/format/DateFormatTest.kt
git commit -m "ui: deadline date formatter (pure, tested)"
```

---

## Task 3: RoadmapListViewModel

**Files:** Modify `app/build.gradle.kts` (deps); Create `ui/list/RoadmapListViewModel.kt`; Test `ui/list/RoadmapListViewModelTest.kt`.

- [ ] **Step 1: Add lifecycle-viewmodel deps**
In `gradle/libs.versions.toml` `[libraries]`:
```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycleRuntimeKtx" }
```
In `app/build.gradle.kts` dependencies:
```kotlin
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
```
Run `./gradlew :app:compileDebugKotlin` to confirm they resolve.

- [ ] **Step 2: Create RoadmapListViewModel.kt**
```kotlin
package com.example.roadmap.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.RoadmapRepository
import com.example.roadmap.data.relation.RoadmapCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoadmapListUiState(
    val archived: Boolean = false,
    val query: String = "",
    val cards: List<RoadmapCard> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoadmapListViewModel(private val repository: RoadmapRepository) : ViewModel() {
    private val archived = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<RoadmapListUiState> =
        combine(archived, query) { a, q -> a to q }
            .flatMapLatest { (a, q) ->
                repository.observeRoadmapCards(a, q).map { RoadmapListUiState(a, q, it) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoadmapListUiState())

    fun setArchived(value: Boolean) { archived.value = value }
    fun setQuery(value: String) { query.value = value }
    fun createRoadmap(title: String, description: String?, deadline: String?) {
        viewModelScope.launch { repository.createRoadmap(title, description, deadline) }
    }
}

class RoadmapListViewModelFactory(private val repository: RoadmapRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoadmapListViewModel(repository) as T
}
```

- [ ] **Step 3: Write the Robolectric ViewModel test** (real repo + VM) — `app/src/test/java/com/example/roadmap/ui/list/RoadmapListViewModelTest.kt`:
```kotlin
package com.example.roadmap.ui.list

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.RoomRoadmapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapListViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var vm: RoadmapListViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        vm = RoadmapListViewModel(RoomRoadmapRepository(db) { t++ })
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun cards_reflect_repository_and_query_filters() = runTest(dispatcher) {
        vm.createRoadmap("Learn Kotlin", null, null)
        vm.createRoadmap("Run marathon", null, null)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.first { it.cards.size == 2 }.cards.size)

        vm.setQuery("kotlin")
        advanceUntilIdle()
        assertEquals(listOf("Learn Kotlin"),
            vm.uiState.first { it.query == "kotlin" }.cards.map { it.roadmap.title })
    }

    @Test fun archived_scope_switch() = runTest(dispatcher) {
        vm.createRoadmap("Active", null, null)
        advanceUntilIdle()
        vm.setArchived(true)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.first { it.archived }.cards.size)
    }
}
```

- [ ] **Step 4: Run + verify** — `./gradlew :app:testDebugUnitTest --tests "*RoadmapListViewModelTest"` → PASS (2 tests).

- [ ] **Step 5: Commit**
```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/example/roadmap/ui/list/RoadmapListViewModel.kt app/src/test/java/com/example/roadmap/ui/list/RoadmapListViewModelTest.kt
git commit -m "ui: RoadmapListViewModel (scope/search state over repository cards)"
```

---

## Task 4: List screen UI

**Files:** Create `ui/list/RoadmapListScreen.kt`.

> Composes existing Phase-1 components. Card progress = `progressFraction(completedSteps, totalSteps)`; overdue = `isOverdue(deadline, complete, today)` where a card is complete when `totalSteps > 0 && completedSteps == totalSteps`. Hue per card = `hueForId(roadmap.id)`; tile initial = first letter of title.

- [ ] **Step 1: Create RoadmapListScreen.kt**
```kotlin
package com.example.roadmap.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.relation.RoadmapCard
import com.example.roadmap.domain.isOverdue
import com.example.roadmap.domain.progressFraction
import com.example.roadmap.ui.components.*
import com.example.roadmap.ui.format.formatDeadline
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.hueForId
import java.time.LocalDate

@Composable
fun RoadmapListScreen(
    state: RoadmapListUiState,
    onScopeChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenRoadmap: (Long) -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { RoadmapFab("New", onCreate) },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Roadmaps", style = MaterialTheme.typography.headlineMedium)
                    Text("${state.cards.size} ${if (state.archived) "archived" else "active"}",
                        style = MaterialTheme.typography.bodySmall, color = RoadmapTheme.colors.muted)
                }
                IconButton(onClick = onImport) {
                    Icon(Icons.Outlined.Map, contentDescription = "Import") // placeholder icon; full import in Phase 7
                }
            }

            SegmentedControl(
                options = listOf("Active", "Archive"),
                selectedIndex = if (state.archived) 1 else 0,
                onSelect = { onScopeChange(it == 1) },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                placeholder = { Text("Search roadmaps & milestones…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            when {
                state.cards.isEmpty() && state.query.isNotBlank() ->
                    EmptyState(Icons.Rounded.Search, "No matches",
                        "No roadmaps match “${state.query}”. Try another term or clear the search.",
                        RoadmapHue.Cyan, Modifier.fillMaxSize())
                state.cards.isEmpty() ->
                    EmptyState(Icons.Outlined.Map,
                        if (state.archived) "Nothing archived" else "No roadmaps yet",
                        if (state.archived) "Roadmaps you archive will appear here."
                        else "Create your first roadmap, or import one from JSON.",
                        RoadmapHue.Emerald, Modifier.fillMaxSize(),
                        actions = if (state.archived) null else ({
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryButton("Create roadmap", onCreate)
                                SecondaryButton("Import", onImport)
                            }
                        }))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    items(state.cards, key = { it.roadmap.id }) { card ->
                        RoadmapCardItem(card, onClick = { onOpenRoadmap(card.roadmap.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapCardItem(card: RoadmapCard, onClick: () -> Unit) {
    val r = card.roadmap
    val complete = card.totalSteps > 0 && card.completedSteps == card.totalSteps
    val overdue = isOverdue(r.deadline, complete, LocalDate.now())
    val progress = progressFraction(card.completedSteps, card.totalSteps)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
            GradientTile(r.title.firstOrNull() ?: '?', hueForId(r.id))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!r.description.isNullOrBlank()) {
                    Text(r.description, style = MaterialTheme.typography.bodyMedium,
                        color = RoadmapTheme.colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp))
                }
                Row(Modifier.padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip("${card.milestoneCount} milestones")
                    val deadlineText = formatDeadline(r.deadline)
                    if (deadlineText != null) {
                        DeadlineChip(
                            if (overdue) "Overdue · $deadlineText" else deadlineText,
                            if (overdue) DeadlineState.Overdue else if (complete) DeadlineState.Done else DeadlineState.Normal,
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            RingProgress(progress, size = RingSize.Medium)
        }
    }
}

// ---- previews ----
private fun sampleCard(id: Long, title: String, desc: String?, ms: Int, total: Int, done: Int, deadline: String?) =
    RoadmapCard(RoadmapEntity(id = id, title = title, description = desc, deadline = deadline), ms, total, done)

@Preview(name = "List Light")
@Composable
private fun ListPreviewLight() = RoadmapTheme(darkTheme = false) {
    RoadmapListScreen(
        RoadmapListUiState(cards = listOf(
            sampleCard(1, "Learn Kotlin & Compose", "From basics to a Material 3 app.", 6, 24, 15, "2026-09-30"),
            sampleCard(2, "Run a Half-Marathon", "12-week build-up.", 4, 12, 3, "2026-07-12"),
        )),
        {}, {}, {}, {}, {})
}

@Preview(name = "List Dark")
@Composable
private fun ListPreviewDark() = RoadmapTheme(darkTheme = true) {
    RoadmapListScreen(RoadmapListUiState(cards = emptyList()), {}, {}, {}, {}, {})
}
```

- [ ] **Step 2: Compile + render previews** — `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`. Open `RoadmapListScreen.kt` previews in Android Studio; compare card layout/empty states to `mockups/roadmap-mockup.html`.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/roadmap/ui/list/RoadmapListScreen.kt
git commit -m "ui: roadmaps list screen (cards, segmented, search, empty states) over design system"
```

---

## Task 5: New-roadmap dialog + wire MainActivity

**Files:** Modify `ui/list/RoadmapListScreen.kt` (add dialog + a stateful host); Modify `MainActivity.kt`.

- [ ] **Step 1: Add the dialog + a stateful screen host to `RoadmapListScreen.kt`**
Append:
```kotlin
@Composable
fun NewRoadmapDialog(onDismiss: () -> Unit, onConfirm: (String, String?, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onConfirm(title.trim(), description.trim().ifBlank { null }, deadline.trim().ifBlank { null })
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New roadmap") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(deadline, { deadline = it }, label = { Text("Deadline YYYY-MM-DD (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

/** Stateful host: owns the VM + dialog visibility; used by MainActivity. */
@Composable
fun RoadmapListRoute(viewModel: RoadmapListViewModel, onOpenRoadmap: (Long) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    RoadmapListScreen(
        state = state,
        onScopeChange = viewModel::setArchived,
        onQueryChange = viewModel::setQuery,
        onOpenRoadmap = onOpenRoadmap,
        onCreate = { showNew = true },
        onImport = { /* TODO Phase 7: paste-JSON import */ },
        modifier = modifier,
    )
    if (showNew) {
        NewRoadmapDialog(
            onDismiss = { showNew = false },
            onConfirm = { t, d, dl -> viewModel.createRoadmap(t, d, dl); showNew = false },
        )
    }
}
```
Add imports as needed: `androidx.compose.runtime.collectAsState`, `getValue`, `mutableStateOf`, `remember`, `setValue`, `androidx.compose.material3.AlertDialog`, `TextButton`.

- [ ] **Step 2: Replace MainActivity.kt to host the list**
```kotlin
package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.RoadmapGraph
import com.example.roadmap.ui.list.RoadmapListRoute
import com.example.roadmap.ui.list.RoadmapListViewModel
import com.example.roadmap.ui.list.RoadmapListViewModelFactory
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = RoadmapGraph.repository(applicationContext)
        setContent {
            RoadmapTheme {
                val vm: RoadmapListViewModel = viewModel(factory = RoadmapListViewModelFactory(repository))
                RoadmapListRoute(
                    viewModel = vm,
                    onOpenRoadmap = { /* TODO Phase 5: navigate to detail */ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
```

- [ ] **Step 3: Full build** — `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL` (the app now launches into the live, DB-backed list).

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/roadmap/ui/list/RoadmapListScreen.kt app/src/main/java/com/example/roadmap/MainActivity.kt
git commit -m "ui: New-roadmap dialog + wire MainActivity to the live list"
```

---

## Task 6: Wrap-up

- [ ] **Step 1: Full sweep** — `./gradlew :app:testDebugUnitTest :app:assembleDebug` → all tests pass, `BUILD SUCCESSFUL`.
- [ ] **Step 2:** Confirm `MainActivity` no longer references the catalog (the catalog file may remain for previews): `grep -n "DesignSystemCatalog" app/src/main/java/com/example/roadmap/MainActivity.kt` returns nothing.

---

## Self-Review (completed during planning)

- **Spec F1.1/F1.2 (scope):** SegmentedControl Active/Archive → `setArchived`; `observeCards(archived,…)` (Tasks 1,3,4) ✓.
- **F1.3 (card):** title, description preview, RingProgress (% steps from counts), milestone count, deadline + overdue styling (Task 4) ✓.
- **F1.4 (search):** SQL matches title/description/milestone-title, case-insensitive (LIKE) (Task 1) ✓.
- **F1.5 (sort):** `ORDER BY updatedAt DESC` (Task 1) ✓.
- **F1.6 (empty states):** distinct "no roadmaps" (with Create/Import CTAs) vs "no results for search" vs empty archive (Task 4) ✓.
- **F1.7 (actions):** New (FAB → dialog, Task 5); Import entry point present as a stubbed icon (full paste-JSON flow is **Phase 7**) ✓.
- **F2 create:** `createRoadmap(title, description, deadline)` via VM → repository (Tasks 3,5) ✓.
- **Type consistency:** `RoadmapCard(roadmap, milestoneCount, totalSteps, completedSteps)`; `observeRoadmapCards(Boolean,String)`; `RoadmapListUiState(archived,query,cards)`; VM methods `setArchived/setQuery/createRoadmap`; reuse of `RingProgress/SegmentedControl/GradientTile/MetaChip/DeadlineChip/EmptyState/RoadmapFab/hueForId/progressFraction/isOverdue/formatDeadline` — all match existing signatures ✓.
- **Deferred (tracked):** navigation + detail (`onOpenRoadmap` is a stub) → Phase 5; full paste-JSON import (`onImport` stub) → Phase 7; edit/archive/delete from the list → Detail screen (Phase 5). UI is preview/compile-verified (no emulator); ViewModel + query are Robolectric-tested.
```
