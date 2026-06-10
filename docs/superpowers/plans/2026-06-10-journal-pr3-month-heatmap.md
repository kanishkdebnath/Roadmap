# Journal — PR 3: Month Heatmap — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the interim Journal scaffold with a live, mood-tinted month heatmap that reads `journalRepository.observeMonth`, supports month navigation, and opens a (stub) day editor when a day is tapped.

**Architecture:** A `JournalMonthViewModel` (MutableStateFlow month → `flatMapLatest` → `observeMonth` → `stateIn`, mirroring `RoadmapListViewModel`) feeds a stateless `MonthHeatmapScreen` (reuses the pure `journalMonthGrid` from PR 2 and `RoadmapTheme.colors.moodTint`). A `JournalRoute` hosts the VM; tapping a day bubbles up to `RoadmapApp`, which gains a Journal drill-down sub-state (a `DayEditorStub` for now — the real editor is PR 4) and hides the nav bar while it's open. `MainActivity` threads `RoadmapGraph.journalRepository` into `RoadmapApp`.

**Tech Stack:** Kotlin · Jetpack Compose · Material 3 · Coroutines/Flow · `java.time` · JUnit + Robolectric (VM test).

**Spec:** `docs/superpowers/specs/2026-06-09-journal-integration-design.md` §7 (Month heatmap, J1) + §6 (nav drill-down).

**Branch:** `feat/journal-heatmap`, **stacked on `feat/journal-nav`** (PR 2 / #9, not yet merged — PR 3 builds on its `JournalScaffoldScreen`/`journalMonthGrid`/nav). **After #9 merges to `main`, rebase this branch onto `main`** (`git rebase --onto main feat/journal-nav feat/journal-heatmap`) before opening PR 3 against `main`.

**Build/verify commands** (JAVA_HOME is in `~/.zshenv`; re-export `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home` if needed):
- Single test: `./gradlew :app:testDebugUnitTest --tests "*JournalMonthViewModelTest"`
- Full suite: `./gradlew :app:testDebugUnitTest`
- Compile: `./gradlew :app:compileDebugKotlin`
- Build + install: `./gradlew :app:installDebug`

**Testing reality:** VM logic is Robolectric-tested (TDD, Task 1). The Compose screens are verified by `@Preview` + compile + emulator. **Note:** PR 3 has no entry-creation UI (the day editor is a stub until PR 4), so on a fresh emulator the month renders with no tints — **mood-tint rendering is verified via the `@Preview`s** (which pass sample data). The emulator check covers month nav, today ring, tap → stub → back, and nav-bar hiding.

---

## File map

**Create:**
- `app/src/main/java/com/example/roadmap/ui/journal/JournalMonthViewModel.kt` — `JournalMonthUiState`, VM (injectable `initialMonth`), factory
- `app/src/main/java/com/example/roadmap/ui/journal/MonthHeatmapScreen.kt` — stateless heatmap + previews
- `app/src/main/java/com/example/roadmap/ui/journal/JournalRoute.kt` — stateful host (VM → screen)
- `app/src/main/java/com/example/roadmap/ui/journal/DayEditorStub.kt` — interim day screen (replaced by PR 4)
- `app/src/test/java/com/example/roadmap/ui/journal/JournalMonthViewModelTest.kt`

**Modify:**
- `app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt` — add `journalRepository` param + Journal drill-down (`journalEditorDay`), hide nav in the Journal editor
- `app/src/main/java/com/example/roadmap/MainActivity.kt` — obtain + pass `journalRepository`

**Removed/retired:** `JournalScaffoldScreen.kt` is no longer used by `RoadmapApp` after this PR. Leave the file in place (harmless, has previews) OR delete it — Task 4 deletes it since it's fully superseded.

---

## Task 1: JournalMonthViewModel

**Files:**
- Create: `app/src/main/java/com/example/roadmap/ui/journal/JournalMonthViewModel.kt`
- Test: `app/src/test/java/com/example/roadmap/ui/journal/JournalMonthViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.roadmap.ui.journal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.RoomJournalRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalMonthViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomJournalRepository
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        repo = RoomJournalRepository(db) { t++ }
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun emits_mood_cells_for_the_initial_month() = runTest(dispatcher) {
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 6, 9), moodScale = 4))
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 5, 2), moodScale = 2))
        advanceUntilIdle()
        val vm = JournalMonthViewModel(repo, initialMonth = YearMonth.of(2026, 6))
        val s = vm.uiState.first { it.moodByDate.isNotEmpty() }
        assertEquals(YearMonth.of(2026, 6), s.month)
        assertEquals(mapOf(LocalDate.of(2026, 6, 9) to 4), s.moodByDate)
    }

    @Test fun shiftMonth_changes_month_and_reloads_cells() = runTest(dispatcher) {
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 5, 2), moodScale = 2))
        advanceUntilIdle()
        val vm = JournalMonthViewModel(repo, initialMonth = YearMonth.of(2026, 6))
        vm.uiState.first { it.month == YearMonth.of(2026, 6) }   // start collecting
        vm.shiftMonth(-1)
        advanceUntilIdle()
        val s = vm.uiState.first { it.month == YearMonth.of(2026, 5) }
        assertEquals(mapOf(LocalDate.of(2026, 5, 2) to 2), s.moodByDate)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalMonthViewModelTest"`
Expected: FAIL — unresolved `JournalMonthViewModel`.

- [ ] **Step 3: Write `JournalMonthViewModel.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.journal.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class JournalMonthUiState(
    val month: YearMonth,
    val moodByDate: Map<LocalDate, Int> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalMonthViewModel(
    private val repository: JournalRepository,
    initialMonth: YearMonth = YearMonth.now(),
) : ViewModel() {
    private val month = MutableStateFlow(initialMonth)

    val uiState: StateFlow<JournalMonthUiState> =
        month.flatMapLatest { m ->
            repository.observeMonth(m).map { cells ->
                JournalMonthUiState(m, cells.associate { LocalDate.parse(it.date) to it.moodScale })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalMonthUiState(initialMonth))

    fun shiftMonth(delta: Long) { month.value = month.value.plusMonths(delta) }
}

class JournalMonthViewModelFactory(private val repository: JournalRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = JournalMonthViewModel(repository) as T
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalMonthViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/JournalMonthViewModel.kt \
        app/src/test/java/com/example/roadmap/ui/journal/JournalMonthViewModelTest.kt
git commit -m "ui: JournalMonthViewModel (observeMonth → mood-by-date, month nav)"
```

---

## Task 2: MonthHeatmapScreen (stateless)

The live heatmap: month-nav header, weekday row, mood-tinted day cells (today ringed), a mood legend, and tap-to-open. Verified via `@Preview` + compile.

**Files:**
- Create: `app/src/main/java/com/example/roadmap/ui/journal/MonthHeatmapScreen.kt`

- [ ] **Step 1: Write `MonthHeatmapScreen.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.domain.journalMonthGrid
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.moodTint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthLabel = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val Weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
private val LegendLabels = listOf(1 to "Rough", 2 to "Low", 3 to "Okay", 4 to "Good", 5 to "Great")

@Composable
fun MonthHeatmapScreen(
    state: JournalMonthUiState,
    today: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = remember(state.month) { journalMonthGrid(state.month) }
    Scaffold(modifier, containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("Journal", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onPrevMonth) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month") }
                Text(
                    state.month.format(MonthLabel),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onNextMonth) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Weekdays.forEach { d ->
                    Text(
                        d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = RoadmapTheme.colors.faint,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            mood = date?.let { state.moodByDate[it] },
                            isToday = date == today,
                            onOpenDay = onOpenDay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Legend()
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    mood: Int?,
    isToday: Boolean,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (date != null) {
            val bg = if (mood != null) RoadmapTheme.colors.moodTint(mood) else MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .then(
                        if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        else Modifier,
                    )
                    .clickable { onOpenDay(date) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mood != null) MaterialTheme.colorScheme.onSurface else RoadmapTheme.colors.faint,
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LegendLabels.forEach { (scale, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(RoadmapTheme.colors.moodTint(scale)),
                )
                Spacer(Modifier.size(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = RoadmapTheme.colors.muted)
            }
        }
    }
}

private fun sampleState() = JournalMonthUiState(
    month = YearMonth.of(2026, 6),
    moodByDate = mapOf(
        LocalDate.of(2026, 6, 2) to 3, LocalDate.of(2026, 6, 3) to 5,
        LocalDate.of(2026, 6, 4) to 4, LocalDate.of(2026, 6, 6) to 1,
        LocalDate.of(2026, 6, 8) to 2, LocalDate.of(2026, 6, 12) to 5,
    ),
)

@Preview(name = "Heatmap (light)")
@Composable
private fun HeatmapPreviewLight() = RoadmapTheme(darkTheme = false) {
    MonthHeatmapScreen(sampleState(), LocalDate.of(2026, 6, 9), {}, {}, {})
}

@Preview(name = "Heatmap (dark)")
@Composable
private fun HeatmapPreviewDark() = RoadmapTheme(darkTheme = true) {
    MonthHeatmapScreen(sampleState(), LocalDate.of(2026, 6, 9), {}, {}, {})
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/MonthHeatmapScreen.kt
git commit -m "ui: stateless MonthHeatmapScreen (mood tints, month nav, legend)"
```

---

## Task 3: JournalRoute host + DayEditorStub

**Files:**
- Create: `app/src/main/java/com/example/roadmap/ui/journal/JournalRoute.kt`
- Create: `app/src/main/java/com/example/roadmap/ui/journal/DayEditorStub.kt`

- [ ] **Step 1: Write `JournalRoute.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.journal.JournalRepository
import java.time.LocalDate

/** Stateful host: owns the month VM, renders the heatmap, bubbles day taps up to the app. */
@Composable
fun JournalRoute(
    repository: JournalRepository,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: JournalMonthViewModel = viewModel(factory = JournalMonthViewModelFactory(repository))
    val state by vm.uiState.collectAsState()
    MonthHeatmapScreen(
        state = state,
        today = LocalDate.now(),
        onPrevMonth = { vm.shiftMonth(-1) },
        onNextMonth = { vm.shiftMonth(1) },
        onOpenDay = onOpenDay,
        modifier = modifier,
    )
}
```

- [ ] **Step 2: Write `DayEditorStub.kt`** (replaced by the real editor in PR 4)

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayLabel = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorStub(date: LocalDate, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(date.format(DayLabel)) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "The day editor (mood, summary, events, links, references) arrives in the next update.",
                style = MaterialTheme.typography.bodyMedium,
                color = RoadmapTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/JournalRoute.kt \
        app/src/main/java/com/example/roadmap/ui/journal/DayEditorStub.kt
git commit -m "ui: JournalRoute host + interim DayEditorStub"
```

---

## Task 4: Wire the heatmap into the app

Add the `journalRepository` dependency and the Journal drill-down (heatmap → day editor) to `RoadmapApp`, hiding the nav bar while the editor is open; thread the repository from `MainActivity`; retire the scaffold screen.

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt`
- Modify: `app/src/main/java/com/example/roadmap/MainActivity.kt`
- Delete: `app/src/main/java/com/example/roadmap/ui/journal/JournalScaffoldScreen.kt`

- [ ] **Step 1: Replace `RoadmapApp.kt`** (adds `journalRepository`, the Journal `journalEditorDay` sub-state, and routes the Journal tab to heatmap-or-stub):

```kotlin
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
```

- [ ] **Step 2: Thread `journalRepository` through `MainActivity.kt`**

Add the import and obtain the repo, then pass it to `RoadmapApp`:
```kotlin
import com.example.roadmap.data.journal.JournalRepository  // (not strictly needed; type is inferred)
```
In `onCreate`, after `val repository = …`:
```kotlin
        val journalRepository = RoadmapGraph.journalRepository(applicationContext)
```
And update the `RoadmapApp(...)` call to include it:
```kotlin
                RoadmapApp(
                    repository = repository,
                    journalRepository = journalRepository,
                    themeMode = mode,
                    onSetThemeMode = themeStore::setMode,
                    modifier = Modifier.fillMaxSize(),
                )
```

- [ ] **Step 3: Delete the superseded scaffold**

```bash
git rm app/src/main/java/com/example/roadmap/ui/journal/JournalScaffoldScreen.kt
```

- [ ] **Step 4: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS (no remaining references to `JournalScaffoldScreen`).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt \
        app/src/main/java/com/example/roadmap/MainActivity.kt
git commit -m "ui: wire live month heatmap into the Journal tab (+ day-editor drill-down)"
```

---

## Definition of done (PR 3)

- [ ] Full suite green: `./gradlew :app:testDebugUnitTest` (adds `JournalMonthViewModelTest`; existing tests unaffected)
- [ ] `./gradlew :app:installDebug`, then on the emulator (pin `screencap` to the app's display id):
  - [ ] Journal tab shows the heatmap for the current month with today ringed; the month label + `‹`/`›` are present.
  - [ ] `‹`/`›` change the month (label updates; grid re-lays-out).
  - [ ] Tap any day → the `DayEditorStub` opens (date in the title bar) and **the bottom nav bar is hidden**; back returns to the heatmap with the bar restored.
  - [ ] Dark mode renders correctly.
  - [ ] (Mood tints can't be created in-app yet — confirm them in the `MonthHeatmapScreen` `@Preview`s instead; live tints arrive once PR 4 adds the editor.)
- [ ] Rebase onto `main` once PR 2 (#9) has merged: `git rebase --onto main feat/journal-nav feat/journal-heatmap`, re-run the suite, then:

```bash
git push -u origin feat/journal-heatmap
gh pr create --base main --title "Journal PR 3: month heatmap" \
  --body "Live month heatmap on the Journal tab: JournalMonthViewModel (observeMonth → mood-by-date, month nav), stateless MonthHeatmapScreen (moodTint cells, today ring, legend), JournalRoute host, and a DayEditorStub drill-down (real editor is PR 4) with the nav bar hidden while open. VM Robolectric-tested; tints verified via @Preview. Spec §7. Stacked on / rebased after PR #9."
```

**Next:** PR 4 — the real day editor (mood + tags, summary, events, links, references, Save/Delete) replacing `DayEditorStub`, reusing the PR-1 repository + dirty-check.
