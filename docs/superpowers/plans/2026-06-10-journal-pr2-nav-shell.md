# Journal — PR 2: Bottom-Nav Shell — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Journal a second top-level destination — add a Material 3 `NavigationBar` (Roadmaps · Journal) that becomes a `NavigationRail` on wide screens and hides when drilled into a roadmap detail, with the Journal tab showing an interim month-calendar scaffold (the live heatmap is PR 3).

**Architecture:** Refactor `ui/RoadmapApp.kt` from the single `detailId` switch into a two-level machine — a top-level `Tab` (rememberSaveable) + the existing per-tab roadmap detail sub-state — wrapped in one app-level `Scaffold` that owns the bottom bar. The existing List/Detail screens keep their own `Scaffold`s; the app Scaffold uses `consumeWindowInsets` so insets aren't double-applied. A pure `journalMonthGrid` helper (domain, JVM-tested) backs the scaffold screen.

**Tech Stack:** Kotlin · Jetpack Compose · Material 3 (`NavigationBar`/`NavigationRail`) · `BoxWithConstraints` for the responsive switch (no window-size-class dependency added) · `java.time` · JUnit.

**Spec:** `docs/superpowers/specs/2026-06-09-journal-integration-design.md` §6 (navigation).

**Branch:** `feat/journal-nav` (already created off the updated `main`, which contains PR 1 + theme-toggle). First of the three remaining journal PRs.

**Build/verify commands** (JAVA_HOME is exported in `~/.zshenv` → Android Studio JBR; re-export `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home` if you hit "Unable to locate a Java Runtime"):
- Single test: `./gradlew :app:testDebugUnitTest --tests "*JournalCalendarTest"`
- Full suite: `./gradlew :app:testDebugUnitTest`
- Compile: `./gradlew :app:compileDebugKotlin`
- Build + install: `./gradlew :app:installDebug`

**Testing reality (per CLAUDE.md):** there are no Compose UI unit tests in this project (no CI emulator). Pure logic is JUnit-tested; Compose screens are verified by `@Preview` + compile, and by installing on the running emulator. So Task 1 is TDD; Tasks 2–3 are compile + `@Preview` + an explicit on-device check in the Definition of Done.

---

## File map

**Create:**
- `app/src/main/java/com/example/roadmap/domain/JournalCalendar.kt` — pure `journalMonthGrid(YearMonth): List<LocalDate?>` (Sunday-first, leading/trailing nulls to whole weeks)
- `app/src/main/java/com/example/roadmap/ui/journal/JournalScaffoldScreen.kt` — interim Journal landing (current month chrome, today ringed, no data) + previews
- `app/src/test/java/com/example/roadmap/domain/JournalCalendarTest.kt`

**Modify:**
- `app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt` — `Tab` enum + two-level nav (NavigationBar/NavigationRail, hidden in detail, theme params threaded)

**Unchanged on purpose:** `MainActivity.kt` (the `RoadmapApp(repository, themeMode, onSetThemeMode, modifier)` signature is preserved — the scaffold needs no repository yet), and the List/Detail screens (theme menu stays in the Roadmaps list header, so theme control is unaffected).

---

## Task 1: Pure month-grid helper

**Files:**
- Create: `app/src/main/java/com/example/roadmap/domain/JournalCalendar.kt`
- Test: `app/src/test/java/com/example/roadmap/domain/JournalCalendarTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.roadmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class JournalCalendarTest {
    @Test fun june_2026_has_one_leading_blank_and_fills_whole_weeks() {
        val grid = journalMonthGrid(YearMonth.of(2026, 6))   // Jun 1 2026 is a Monday
        assertEquals(0, grid.size % 7)                       // rectangular (whole weeks)
        assertEquals(35, grid.size)                          // 1 lead + 30 days + 4 trail
        assertNull(grid[0])                                  // Sunday slot before Mon Jun 1
        assertEquals(LocalDate.of(2026, 6, 1), grid[1])
        assertEquals(LocalDate.of(2026, 6, 9), grid[9])
        assertEquals(LocalDate.of(2026, 6, 30), grid[30])
        assertNull(grid[34])                                 // trailing pad
        assertEquals(30, grid.count { it != null })
    }

    @Test fun month_starting_on_sunday_has_no_leading_blank() {
        val grid = journalMonthGrid(YearMonth.of(2026, 3))   // Mar 1 2026 is a Sunday
        assertEquals(LocalDate.of(2026, 3, 1), grid[0])
        assertEquals(0, grid.size % 7)
    }

    @Test fun february_2027_has_28_days_and_whole_weeks() {
        val grid = journalMonthGrid(YearMonth.of(2027, 2))   // Feb 1 2027 is a Monday
        assertNull(grid[0])
        assertEquals(LocalDate.of(2027, 2, 1), grid[1])
        assertEquals(28, grid.count { it != null })
        assertEquals(0, grid.size % 7)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalCalendarTest"`
Expected: FAIL — unresolved reference `journalMonthGrid`.

- [ ] **Step 3: Write minimal implementation** — `JournalCalendar.kt`:

```kotlin
package com.example.roadmap.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * A Sunday-first month grid for the journal calendar: leading nulls for the days before
 * the 1st, every day of the month, then trailing nulls so the list is whole weeks (size % 7 == 0).
 * Pure — reused by the PR-2 scaffold and the PR-3 heatmap.
 */
fun journalMonthGrid(month: YearMonth): List<LocalDate?> {
    val lead = month.atDay(1).dayOfWeek.value % 7        // Mon=1..Sat=6, Sun(7)→0
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(lead) { null } + days
    val trail = (7 - cells.size % 7) % 7
    return cells + List(trail) { null }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalCalendarTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/domain/JournalCalendar.kt \
        app/src/test/java/com/example/roadmap/domain/JournalCalendarTest.kt
git commit -m "domain: pure journalMonthGrid month-grid helper"
```

---

## Task 2: Journal scaffold screen

The interim Journal landing — the current month's calendar chrome (today ringed) with no data. PR 3 replaces it with the live mood-tinted, navigable heatmap. No unit test (Compose UI); verify via `@Preview` + compile.

**Files:**
- Create: `app/src/main/java/com/example/roadmap/ui/journal/JournalScaffoldScreen.kt`

- [ ] **Step 1: Write `JournalScaffoldScreen.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.domain.journalMonthGrid
import com.example.roadmap.ui.theme.RoadmapTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthLabel = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val Weekdays = listOf("S", "M", "T", "W", "T", "F", "S")

/**
 * Interim Journal landing: the current month's calendar chrome with no entries yet.
 * PR 3 replaces this with the live, mood-tinted, month-navigable heatmap.
 */
@Composable
fun JournalScaffoldScreen(modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val month = remember { YearMonth.from(today) }
    val cells = remember(month) { journalMonthGrid(month) }

    Scaffold(modifier, containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(18.dp)) {
            Text("Journal", style = MaterialTheme.typography.headlineMedium)
            Text(
                month.format(MonthLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = RoadmapTheme.colors.muted,
            )
            Spacer(Modifier.height(16.dp))
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
                        DayCell(date, isToday = date == today, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate?, isToday: Boolean, modifier: Modifier) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (date != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .then(
                        if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = RoadmapTheme.colors.faint,
                )
            }
        }
    }
}

@Preview(name = "Journal scaffold (light)")
@Composable
private fun JournalScaffoldPreviewLight() = RoadmapTheme(darkTheme = false) { JournalScaffoldScreen() }

@Preview(name = "Journal scaffold (dark)")
@Composable
private fun JournalScaffoldPreviewDark() = RoadmapTheme(darkTheme = true) { JournalScaffoldScreen() }
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 3: (Optional but recommended) eyeball the previews** in Android Studio's preview pane — light + dark, today's cell ringed in brand color, empty cells as plain surface tiles.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/JournalScaffoldScreen.kt
git commit -m "ui: interim Journal month scaffold screen"
```

---

## Task 3: RoadmapApp two-level nav (NavigationBar / NavigationRail)

Refactor the app shell into Tab + per-tab sub-state with the bottom bar (phone) / rail (wide), hidden in detail. Threads the existing theme params straight through to the Roadmaps list.

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt`

- [ ] **Step 1: Replace `RoadmapApp.kt` with:**

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
```

Notes for the implementer:
- `Icons.Outlined.Map` and `Icons.Outlined.CalendarMonth` come from `androidx.compose.material.icons.extended` (already a dependency; `Map` is already used by the list screen).
- The `RoadmapListRoute` / `RoadmapDetailRoute` signatures are unchanged — they're just hosted inside the new `Box(weight)` instead of being the whole screen. Don't pass a `modifier` (their defaults + own `Scaffold` fill the box).
- `MainActivity` is **not** modified: `RoadmapApp(repository, themeMode, onSetThemeMode, modifier)` is unchanged.

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt
git commit -m "ui: bottom-nav shell (Roadmaps | Journal), rail on wide, hidden in detail"
```

---

## Definition of done (PR 2)

- [ ] Full suite green: `./gradlew :app:testDebugUnitTest` (Task 1 adds `JournalCalendarTest`; existing tests unaffected — no data/VM changes)
- [ ] `./gradlew :app:installDebug` and verify on the emulator (pin `screencap` to the app's display id — the Resizable AVD has two displays):
  - [ ] Bottom bar shows **Roadmaps · Journal**; Roadmaps is selected on launch and shows the existing list (with its theme menu intact).
  - [ ] Tap **Journal** → the month scaffold for the current month, today's cell ringed; bar still visible.
  - [ ] Tap **Roadmaps** → open a roadmap → **the bottom bar disappears** in the detail; press back → list returns with the bar.
  - [ ] Insets are correct: list content/FAB sit above the bar (not under it), nothing clipped by the status bar (this verifies the `consumeWindowInsets` wiring).
  - [ ] Toggle dark mode (theme menu on the Roadmaps tab) → both tabs render correctly.
  - [ ] (If convenient) rotate to landscape / widen the resizable AVD past ~600dp → the bar becomes a left **rail**.
- [ ] Push and open a PR against `main`:

```bash
git push -u origin feat/journal-nav
gh pr create --base main --title "Journal PR 2: bottom-nav shell" \
  --body "Adds Journal as a second top-level destination: Material 3 NavigationBar (rail on wide screens), hidden in roadmap detail, with an interim month-calendar scaffold on the Journal tab (live heatmap is PR 3). Pure journalMonthGrid helper is JVM-tested; nav verified on the emulator. Spec §6: docs/superpowers/specs/2026-06-09-journal-integration-design.md"
```

**Next:** PR 3 — month heatmap (wire `journalRepository.observeMonth`, mood tints via `moodTint`, month nav, tap a day → stub editor), reusing `journalMonthGrid`. Then PR 4 — the day editor.
