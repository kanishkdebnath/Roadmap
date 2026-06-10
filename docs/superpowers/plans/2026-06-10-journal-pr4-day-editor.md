# Journal — PR 4: Day Editor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `DayEditorStub` with the real day editor (J2–J7): mood (5 emoji + up to 3 tags), summary, events, links, references (+ searchable picker), with Save (dirty-gated) and Delete (confirm) — completing the Journal feature.

**Architecture:** A `JournalDayViewModel` loads the day once (`observeDay` → editable `JournalDraft` + resolved reference titles), holds the draft in a `StateFlow`, derives `canSave` from the domain `canSaveJournal` (plus link-URL validity), and exposes `save`/`delete`. A stateless `DayEditorScreen` composes focused sub-editors (`MoodSelector`, `EventsEditor`, `LinksEditor`, `ReferencesEditor`) and a `ReferencePicker` dialog. A `DayEditorRoute` hosts the VM and wires save/delete → back. `RoadmapApp`'s Journal drill-down swaps the stub for the route.

**Tech Stack:** Kotlin · Jetpack Compose · Material 3 · Coroutines/Flow · JUnit + Robolectric (VM test). Reuses PR-1 repository + domain dirty-check and existing components (`LinkChip`, `PrimaryButton`, `DangerButton`, `ConfirmDeleteDialog`).

**Spec:** `docs/superpowers/specs/2026-06-09-journal-integration-design.md` §7 (Day editor) + §8 (edge cases) + the approved mockup.

**Branch:** `feat/journal-editor`, off the updated `main` (PR 1–3 merged). Final journal PR.

**Build/verify** (JAVA_HOME in `~/.zshenv`; re-export `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home` if needed):
- Single test: `./gradlew :app:testDebugUnitTest --tests "*JournalDayViewModelTest"`
- Full suite: `./gradlew :app:testDebugUnitTest`  · Compile: `./gradlew :app:compileDebugKotlin`  · Install: `./gradlew :app:installDebug`

**Testing reality:** the VM is Robolectric-tested (TDD, Task 1). Compose sub-editors/screen are verified by `@Preview` + compile + emulator (per CLAUDE.md). PR 4 finally lets you create entries, so the emulator check also confirms the heatmap tints from PR 3.

**Reused APIs (already on `main`):**
- `JournalRepository`: `observeDay(LocalDate): Flow<JournalDayWithChildren?>`, `observeResolvedReferences(dayId: Long): Flow<List<ResolvedReference>>`, `searchReferenceTargets(q: String): Flow<List<RefTarget>>`, `suspend saveDay(JournalDraft)`, `suspend deleteDay(LocalDate)`.
- `JournalDraft(date, moodScale=0, moodTags=emptyList(), summary=null, events=emptyList(), links=emptyList(), references=emptyList())`; `EventDraft(text, important=false, time=null)`; `ReferenceDraft(type, roadmapId, milestoneId=null)`; `LinkDraft(url, label=null)` (in `com.example.roadmap.data`).
- `JournalDayWithChildren(day, events, links, references)` (sorted); `ResolvedReference(reference, resolvedTitle)`; `RefTarget(type, roadmapId, milestoneId, title)`.
- `MoodTag { Focused, Tired, Anxious, Grateful, Restless, Excited, Low, Calm }`; `RefType { Roadmap, Milestone }`.
- domain: `JournalDraft.normalized()`, `canSaveJournal(draft, saved)`.
- components: `LinkChip(label, modifier, onClick)`, `PrimaryButton(text, onClick, modifier, enabled)`, `DangerButton(text, onClick, modifier)`, `ConfirmDeleteDialog(title, message, onDismiss, onConfirm)`, `RoadmapTheme.colors` (`muted`, `faint`, `done`, `doneContainer`, `amber`, `overdue`).

---

## File map

**Create (all under `app/src/main/java/com/example/roadmap/ui/journal/`):**
- `JournalDayViewModel.kt` — `DayEditorUiState`, `RefKey`, VM + factory + `toDraft`
- `MoodSelector.kt` — 5 emoji faces + tag chips
- `EventsEditor.kt` — event rows + add
- `LinksEditor.kt` — link rows + add
- `ReferencesEditor.kt` — reference chips + `ReferencePicker` dialog
- `DayEditorScreen.kt` — stateless assembly (app bar, sections, Delete)
- `DayEditorRoute.kt` — stateful host
- Test: `app/src/test/java/com/example/roadmap/ui/journal/JournalDayViewModelTest.kt`

**Modify:** `app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt` — Journal drill-down renders `DayEditorRoute` instead of `DayEditorStub`.
**Delete:** `app/src/main/java/com/example/roadmap/ui/journal/DayEditorStub.kt`.

---

## Task 1: JournalDayViewModel

**Files:**
- Create: `app/src/main/java/com/example/roadmap/ui/journal/JournalDayViewModel.kt`
- Test: `app/src/test/java/com/example/roadmap/ui/journal/JournalDayViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.roadmap.ui.journal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.MoodTag
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalDayViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomJournalRepository
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.of(2026, 6, 9)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        repo = RoomJournalRepository(db) { t++ }
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun loads_existing_day_into_draft() = runTest(dispatcher) {
        repo.saveDay(
            JournalDraft(date = date, moodScale = 4, moodTags = listOf(MoodTag.Focused),
                summary = "good", events = listOf(EventDraft("standup", important = true, time = "10am")))
        )
        advanceUntilIdle()
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        val s = vm.uiState.first { it.loaded }
        assertEquals(4, s.draft.moodScale)
        assertEquals(listOf(MoodTag.Focused), s.draft.moodTags)
        assertEquals("good", s.draft.summary)
        assertEquals(listOf("standup"), s.draft.events.map { it.text })
        assertFalse(s.canSave)   // unchanged after load
    }

    @Test fun editing_enables_save_and_persists() = runTest(dispatcher) {
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        assertFalse(vm.uiState.first { it.loaded }.canSave)
        vm.update { it.copy(moodScale = 4, summary = "new day") }
        assertTrue(vm.uiState.value.canSave)
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertTrue(saved)
        val persisted = repo.observeDay(date).first()!!
        assertEquals(4, persisted.day.moodScale)
        assertEquals("new day", persisted.day.summary)
    }

    @Test fun invalid_link_url_blocks_save() = runTest(dispatcher) {
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        vm.update { it.copy(moodScale = 3, links = listOf(LinkDraft("not-a-url"))) }
        assertFalse(vm.uiState.value.canSave)
        vm.update { it.copy(links = listOf(LinkDraft("https://ok.com"))) }
        assertTrue(vm.uiState.value.canSave)
    }

    @Test fun delete_removes_the_day() = runTest(dispatcher) {
        repo.saveDay(JournalDraft(date = date, moodScale = 3))
        advanceUntilIdle()
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        vm.uiState.first { it.loaded }
        var deleted = false
        vm.delete { deleted = true }
        advanceUntilIdle()
        assertTrue(deleted)
        assertNull(repo.observeDay(date).first())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalDayViewModelTest"`
Expected: FAIL — unresolved `JournalDayViewModel`.

- [ ] **Step 3: Write `JournalDayViewModel.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.data.journal.RefTarget
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import com.example.roadmap.domain.canSaveJournal
import com.example.roadmap.domain.normalized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Identifies a reference target for the resolved-title lookup. */
data class RefKey(val roadmapId: Long, val milestoneId: Long?)

data class DayEditorUiState(
    val date: LocalDate,
    val draft: JournalDraft,
    val refTitles: Map<RefKey, String?> = emptyMap(),  // null title => deleted target
    val loaded: Boolean = false,
    val canSave: Boolean = false,
)

class JournalDayViewModel(
    private val repository: JournalRepository,
    private val date: LocalDate,
) : ViewModel() {

    private var saved: JournalDraft = JournalDraft(date = date)
    private val _state = MutableStateFlow(DayEditorUiState(date, JournalDraft(date = date)))
    val uiState: StateFlow<DayEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val day = repository.observeDay(date).first()
            val draft = day?.toDraft(date) ?: JournalDraft(date = date)
            val titles = if (day != null) {
                repository.observeResolvedReferences(day.day.id).first()
                    .associate { RefKey(it.reference.roadmapId, it.reference.milestoneId) to it.resolvedTitle }
            } else {
                emptyMap()
            }
            saved = draft
            _state.value = DayEditorUiState(date, draft, titles, loaded = true, canSave = false)
        }
    }

    /** Apply an edit to the draft (the screen passes a copy(...) transform). */
    fun update(block: (JournalDraft) -> JournalDraft) {
        val d = block(_state.value.draft)
        _state.value = _state.value.copy(draft = d, canSave = computeCanSave(d))
    }

    /** Add a picked reference (dedup + 10 cap), remembering its title for display. */
    fun addReference(target: RefTarget) {
        val d = _state.value.draft
        val exists = d.references.any {
            it.type == target.type && it.roadmapId == target.roadmapId && it.milestoneId == target.milestoneId
        }
        if (d.references.size >= 10 || exists) return
        val nd = d.copy(references = d.references + ReferenceDraft(target.type, target.roadmapId, target.milestoneId))
        val titles = _state.value.refTitles + (RefKey(target.roadmapId, target.milestoneId) to target.title)
        _state.value = _state.value.copy(draft = nd, refTitles = titles, canSave = computeCanSave(nd))
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch { repository.saveDay(_state.value.draft.normalized()); onSaved() }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch { repository.deleteDay(date); onDeleted() }
    }

    private fun computeCanSave(d: JournalDraft): Boolean =
        canSaveJournal(d, saved) && d.links.all { it.url.isBlank() || it.url.isHttpUrl() }
}

private fun String.isHttpUrl() = startsWith("http://") || startsWith("https://")

private fun JournalDayWithChildren.toDraft(date: LocalDate) = JournalDraft(
    date = date,
    moodScale = day.moodScale,
    moodTags = day.moodTags,
    summary = day.summary,
    events = events.map { EventDraft(it.text, it.important, it.time) },
    links = links.map { LinkDraft(it.url, it.label) },
    references = references.map { ReferenceDraft(it.type, it.roadmapId, it.milestoneId) },
)

class JournalDayViewModelFactory(
    private val repository: JournalRepository,
    private val date: LocalDate,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        JournalDayViewModel(repository, date) as T
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalDayViewModelTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/JournalDayViewModel.kt \
        app/src/test/java/com/example/roadmap/ui/journal/JournalDayViewModelTest.kt
git commit -m "ui: JournalDayViewModel (load draft, dirty-gated canSave, save/delete)"
```

---

## Task 2: MoodSelector

**Files:** Create `app/src/main/java/com/example/roadmap/ui/journal/MoodSelector.kt`

- [ ] **Step 1: Write `MoodSelector.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.ui.theme.RoadmapTheme

private val Faces = listOf(1 to "😞", 2 to "😔", 3 to "😐", 4 to "🙂", 5 to "😄")
private val MoodLabels = mapOf(1 to "Rough", 2 to "Low", 3 to "Okay", 4 to "Good", 5 to "Great")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodSelector(
    scale: Int,
    tags: List<MoodTag>,
    onScale: (Int) -> Unit,
    onToggleTag: (MoodTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Faces.forEach { (s, emoji) ->
                val selected = s == scale
                Column(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) RoadmapTheme.colors.doneContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .then(if (selected) Modifier.border(2.dp, RoadmapTheme.colors.done, RoundedCornerShape(11.dp)) else Modifier)
                        .clickable { onScale(s) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        MoodLabels.getValue(s),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (selected) RoadmapTheme.colors.done else RoadmapTheme.colors.faint,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MoodTag.entries.forEach { tag ->
                val selected = tag in tags
                val atCap = tags.size >= 3 && !selected
                TagChip(tag.name.lowercase(), selected = selected, enabled = !atCap) { onToggleTag(tag) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Up to 3 tags · ${tags.size} selected",
            style = MaterialTheme.typography.labelSmall,
            color = RoadmapTheme.colors.faint,
        )
    }
}

@Composable
private fun TagChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(999.dp),
            )
            .alpha(if (enabled || selected) 1f else 0.4f)
            .clickable(enabled = enabled || selected) { onClick() }
            .padding(horizontal = 11.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else RoadmapTheme.colors.muted,
    )
}
```

- [ ] **Step 2: Verify compile** — `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/MoodSelector.kt
git commit -m "ui: MoodSelector (5 emoji faces + tag chips, 3 cap)"
```

---

## Task 3: EventsEditor + LinksEditor

**Files:**
- Create: `app/src/main/java/com/example/roadmap/ui/journal/EventsEditor.kt`
- Create: `app/src/main/java/com/example/roadmap/ui/journal/LinksEditor.kt`

- [ ] **Step 1: Write `EventsEditor.kt`** (cap 20; text ≤500, time ≤20 enforced by `take`)

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun EventsEditor(
    events: List<EventDraft>,
    onChange: (List<EventDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        events.forEachIndexed { i, e ->
            EventRow(
                event = e,
                onEvent = { updated -> onChange(events.toMutableList().also { it[i] = updated }) },
                onRemove = { onChange(events.toMutableList().also { it.removeAt(i) }) },
            )
        }
        if (events.size < 20) {
            Text(
                "+ Add event",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickableText { onChange(events + EventDraft(text = "")) },
            )
        }
    }
}

@Composable
private fun EventRow(event: EventDraft, onEvent: (EventDraft) -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        IconButton(onClick = { onEvent(event.copy(important = !event.important)) }) {
            if (event.important) {
                Icon(Icons.Rounded.Star, "Unmark important", tint = RoadmapTheme.colors.amber)
            } else {
                Icon(Icons.Rounded.StarBorder, "Mark important", tint = RoadmapTheme.colors.faint)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                event.text, { onEvent(event.copy(text = it.take(500))) },
                placeholder = { Text("What happened?") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                event.time ?: "", { onEvent(event.copy(time = it.take(20).ifBlank { null })) },
                placeholder = { Text("Time (optional), e.g. 10am") },
                singleLine = true,
                modifier = Modifier.width(200.dp),
            )
        }
        IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove event") }
    }
}
```

- [ ] **Step 2: Write `LinksEditor.kt`** (cap 10; url validity is gated by the VM's `canSave`, shown as a hint here)

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun LinksEditor(
    links: List<LinkDraft>,
    onChange: (List<LinkDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anyInvalid = links.any { it.url.isNotBlank() && !it.url.startsWith("http://") && !it.url.startsWith("https://") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        links.forEachIndexed { i, l ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        l.url,
                        { newUrl -> onChange(links.toMutableList().also { it[i] = l.copy(url = newUrl) }) },
                        placeholder = { Text("https://…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        l.label ?: "",
                        { newLabel -> onChange(links.toMutableList().also { it[i] = l.copy(label = newLabel.ifBlank { null }) }) },
                        placeholder = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(onClick = { onChange(links.toMutableList().also { it.removeAt(i) }) }) {
                    Icon(Icons.Rounded.Close, "Remove link")
                }
            }
        }
        if (anyInvalid) {
            Text(
                "Links must start with http:// or https://",
                style = MaterialTheme.typography.bodySmall,
                color = RoadmapTheme.colors.overdue,
            )
        }
        if (links.size < 10) {
            Text(
                "+ Add link",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp).clickableText { onChange(links + LinkDraft("")) },
            )
        }
    }
}
```

- [ ] **Step 3: Add the shared `clickableText` helper.** Both editors above (and the Add affordances) use a tiny modifier extension. Add it to a new file `app/src/main/java/com/example/roadmap/ui/journal/JournalUi.kt`:

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/** A text-only tappable affordance (used for the inline "+ Add …" rows). */
fun Modifier.clickableText(onClick: () -> Unit): Modifier = this.clickable(role = Role.Button) { onClick() }
```

- [ ] **Step 4: Verify compile** — `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/EventsEditor.kt \
        app/src/main/java/com/example/roadmap/ui/journal/LinksEditor.kt \
        app/src/main/java/com/example/roadmap/ui/journal/JournalUi.kt
git commit -m "ui: EventsEditor + LinksEditor (inline rows, caps, validity hint)"
```

---

## Task 4: ReferencesEditor + ReferencePicker

**Files:** Create `app/src/main/java/com/example/roadmap/ui/journal/ReferencesEditor.kt`

> Spec deviation (intentional): the spec/mockup mention "tapping a live chip opens that roadmap." In the editor that would navigate away from an unsaved entry, so chips here are **remove-only** (✕). Tap-to-open is deferred — the spec itself flags it as "easy to cut" (adaptation #5). The resolved title (or struck-through "(deleted)") still renders.

- [ ] **Step 1: Write `ReferencesEditor.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.data.journal.RefTarget
import com.example.roadmap.data.journal.RefType
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.ui.theme.RoadmapTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferencesEditor(
    references: List<ReferenceDraft>,
    titleFor: (ReferenceDraft) -> String?,   // null => deleted target
    onRemove: (ReferenceDraft) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (references.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                references.forEach { ref ->
                    RefChip(
                        title = titleFor(ref),
                        isMilestone = ref.type == RefType.Milestone,
                        onRemove = { onRemove(ref) },
                    )
                }
            }
        }
        if (references.size < 10) {
            Text(
                "+ Add roadmap or milestone",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp).clickableText { onAddClick() },
            )
        }
    }
}

@Composable
private fun RefChip(title: String?, isMilestone: Boolean, onRemove: () -> Unit) {
    val deleted = title == null
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            if (isMilestone) Icons.Outlined.Flag else Icons.Outlined.Map,
            null,
            tint = RoadmapTheme.colors.muted,
            modifier = Modifier.padding(end = 1.dp),
        )
        Text(
            title ?: "(deleted)",
            style = MaterialTheme.typography.labelMedium,
            color = if (deleted) RoadmapTheme.colors.faint else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (deleted) TextDecoration.LineThrough else null,
        )
        Icon(
            Icons.Rounded.Close, "Remove reference",
            tint = RoadmapTheme.colors.faint,
            modifier = Modifier.clickable { onRemove() },
        )
    }
}

/** Searchable picker over the app's roadmaps + milestones. */
@Composable
fun ReferencePicker(
    repository: JournalRepository,
    onDismiss: () -> Unit,
    onPick: (RefTarget) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results by remember(query) { repository.searchReferenceTargets(query) }
        .collectAsState(initial = emptyList())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reference") },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Close") } },
        text = {
            Column {
                OutlinedTextField(
                    query, { query = it },
                    placeholder = { Text("Search roadmaps & milestones…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                    items(results) { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(t); onDismiss() }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (t.type == RefType.Milestone) Icons.Outlined.Flag else Icons.Outlined.Map,
                                null, tint = RoadmapTheme.colors.muted,
                            )
                            Column {
                                Text(t.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (t.type == RefType.Milestone) "Milestone" else "Roadmap",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoadmapTheme.colors.faint,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
```

- [ ] **Step 2: Verify compile** — `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/ReferencesEditor.kt
git commit -m "ui: ReferencesEditor chips + searchable ReferencePicker"
```

---

## Task 5: DayEditorScreen (assembly)

Stateless screen: app bar (back, date, dirty-gated Save), scrolling sections, Delete-entry danger button. Sub-editors are driven by `DayEditorUiState` + callbacks.

**Files:** Create `app/src/main/java/com/example/roadmap/ui/journal/DayEditorScreen.kt`

- [ ] **Step 1: Write `DayEditorScreen.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.ui.components.DangerButton
import com.example.roadmap.ui.theme.RoadmapTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayLabel = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorScreen(
    state: DayEditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onScale: (Int) -> Unit,
    onToggleTag: (MoodTag) -> Unit,
    onSummary: (String) -> Unit,
    onEvents: (List<EventDraft>) -> Unit,
    onLinks: (List<LinkDraft>) -> Unit,
    onRemoveReference: (ReferenceDraft) -> Unit,
    onAddReference: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = state.draft
    Scaffold(
        modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.date.format(DayLabel)) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Mood", required = true) {
                MoodSelector(d.moodScale, d.moodTags, onScale, onToggleTag)
            }
            Section("Summary") {
                OutlinedTextField(
                    d.summary ?: "", { onSummary(it.take(500)) },
                    placeholder = { Text("One line about your day…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${(d.summary ?: "").length} / 500",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoadmapTheme.colors.faint,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Section("Events") { EventsEditor(d.events, onEvents) }
            Section("Links") { LinksEditor(d.links, onLinks) }
            Section("References") {
                ReferencesEditor(
                    references = d.references,
                    titleFor = { ref -> state.refTitles[RefKey(ref.roadmapId, ref.milestoneId)] },
                    onRemove = onRemoveReference,
                    onAddClick = onAddReference,
                )
            }
            DangerButton("Delete entry", onDelete, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Section(title: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            buildString { append(title.uppercase()); if (required) append("  •  REQUIRED") },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (required) RoadmapTheme.colors.overdue else RoadmapTheme.colors.faint,
        )
        content()
    }
}
```

- [ ] **Step 2: Verify compile** — `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 3: Add a `@Preview` to `DayEditorScreen.kt`** (sanity-check the layout):

```kotlin
@androidx.compose.ui.tooling.preview.Preview(name = "Day editor")
@Composable
private fun DayEditorPreview() = com.example.roadmap.ui.theme.RoadmapTheme {
    DayEditorScreen(
        state = DayEditorUiState(
            date = LocalDate.of(2026, 6, 9),
            draft = com.example.roadmap.data.journal.JournalDraft(
                date = LocalDate.of(2026, 6, 9),
                moodScale = 4,
                moodTags = listOf(MoodTag.Focused, MoodTag.Grateful),
                summary = "Shipped the theme toggle.",
                events = listOf(EventDraft("Standup", important = true, time = "10am"), EventDraft("Fixed a bug")),
                links = listOf(LinkDraft("https://developer.android.com", "Docs")),
                references = listOf(ReferenceDraft(com.example.roadmap.data.journal.RefType.Roadmap, 1)),
            ),
            refTitles = mapOf(RefKey(1, null) to "Learn Kotlin"),
            loaded = true,
            canSave = true,
        ),
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    )
}
```

- [ ] **Step 4: Verify compile again** — `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/DayEditorScreen.kt
git commit -m "ui: DayEditorScreen — app bar Save + sections + Delete"
```

---

## Task 6: DayEditorRoute + wire into the app

**Files:**
- Create: `app/src/main/java/com/example/roadmap/ui/journal/DayEditorRoute.kt`
- Modify: `app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt`
- Delete: `app/src/main/java/com/example/roadmap/ui/journal/DayEditorStub.kt`

- [ ] **Step 1: Write `DayEditorRoute.kt`**

```kotlin
package com.example.roadmap.ui.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.ui.components.ConfirmDeleteDialog
import java.time.LocalDate

/** Stateful host: owns the day VM, the reference picker, and the delete-confirm dialog. */
@Composable
fun DayEditorRoute(
    repository: JournalRepository,
    date: LocalDate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: JournalDayViewModel = viewModel(
        factory = JournalDayViewModelFactory(repository, date),
        key = "day-$date",
    )
    val state by vm.uiState.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    DayEditorScreen(
        state = state,
        onBack = onBack,
        onSave = { vm.save(onBack) },
        onDelete = { showDeleteConfirm = true },
        onScale = { s -> vm.update { it.copy(moodScale = s) } },
        onToggleTag = { tag ->
            vm.update {
                if (tag in it.moodTags) it.copy(moodTags = it.moodTags - tag)
                else if (it.moodTags.size < 3) it.copy(moodTags = it.moodTags + tag)
                else it
            }
        },
        onSummary = { s -> vm.update { it.copy(summary = s.ifBlank { null }) } },
        onEvents = { events -> vm.update { it.copy(events = events) } },
        onLinks = { links -> vm.update { it.copy(links = links) } },
        onRemoveReference = { ref -> vm.update { it.copy(references = it.references - ref) } },
        onAddReference = { showPicker = true },
        modifier = modifier,
    )

    if (showPicker) {
        ReferencePicker(
            repository = repository,
            onDismiss = { showPicker = false },
            onPick = { target -> vm.addReference(target) },
        )
    }
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete entry?",
            message = "This journal entry will be permanently removed.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; vm.delete(onBack) },
        )
    }
}
```

- [ ] **Step 2: Wire it into `RoadmapApp.kt`.** Replace the `DayEditorStub` import:
```kotlin
import com.example.roadmap.ui.journal.DayEditorStub
```
with:
```kotlin
import com.example.roadmap.ui.journal.DayEditorRoute
```
And in the Journal tab branch, replace:
```kotlin
                            } else {
                                DayEditorStub(d, onBack = { journalEditorDay = null })
                            }
```
with:
```kotlin
                            } else {
                                DayEditorRoute(journalRepository, d, onBack = { journalEditorDay = null })
                            }
```

- [ ] **Step 3: Delete the stub**

```bash
git rm app/src/main/java/com/example/roadmap/ui/journal/DayEditorStub.kt
```

- [ ] **Step 4: Verify compile** — `./gradlew :app:compileDebugKotlin` → SUCCESS (no remaining references to `DayEditorStub`).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/journal/DayEditorRoute.kt \
        app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt
git commit -m "ui: wire DayEditorRoute into the Journal tab (replaces stub)"
```

---

## Definition of done (PR 4)

- [ ] Full suite green: `./gradlew :app:testDebugUnitTest` (adds `JournalDayViewModelTest`)
- [ ] `./gradlew :app:installDebug`, then on the emulator (pin `screencap` to the app's display id; the soft keyboard shifts dialog buttons — see CLAUDE.md):
  - [ ] Journal tab → tap today → editor opens; **Save is disabled** until a mood is chosen.
  - [ ] Pick a mood + 2 tags (3rd tag past the cap is disabled), type a summary, add an event (star it + a time), add a link, add a reference via the picker (search finds an existing roadmap/milestone). **Save** enables; tap it → returns to the heatmap and **the day's cell is now mood-tinted** (this also confirms PR 3's live tints).
  - [ ] Re-open the same day → all fields reload; Save is disabled again until edited.
  - [ ] Add an invalid link URL → Save disables + the http(s) hint shows; fix it → Save re-enables.
  - [ ] Delete entry → confirm dialog → entry removed (cell un-tints).
  - [ ] Reference chip for a roadmap shows its title; remove with ✕.
  - [ ] Dark mode renders correctly; nav bar stays hidden while the editor is open.
- [ ] Push and open the PR against `main`:

```bash
git push -u origin feat/journal-editor
gh pr create --base main --title "Journal PR 4: day editor" \
  --body "Final journal PR: replaces DayEditorStub with the full day editor (mood + tags, summary, events, links, references + searchable picker), Save (dirty-gated via canSaveJournal + link validity) and Delete (confirm). JournalDayViewModel Robolectric-tested; reuses the PR-1 repo + dirty-check. Completes the Journal feature. Spec §7."
```

**After merge:** the Journal feature is complete (data layer → nav → heatmap → editor). Remaining roadmap-app work is the original Phase 7b (export) + Phase 8 (polish).
