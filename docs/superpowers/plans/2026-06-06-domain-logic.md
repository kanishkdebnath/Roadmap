# Domain Logic Implementation Plan (Phase 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide the pure, JVM-tested read-side domain logic the screens will consume — roadmap/milestone **progress**, **overdue** detection (via `java.time`), and **import validation** against the bulk schema (§8).

**Architecture:** All pure Kotlin in `com.example.roadmap.domain`, no Android/Room runtime — so everything is plain JUnit (no Robolectric). Progress/overdue provide a tiny core function plus thin extensions over the Phase-2 read model (`RoadmapWithChildren`/`MilestoneWithSteps`). Import validation operates on the Phase-2 `RoadmapDraft` and returns a sealed result; the import UI (Phase 7) will call it before `RoadmapRepository.importRoadmap`.

**Tech Stack:** Kotlin, `java.time.LocalDate` (native on minSdk 26), JUnit4. Builds on Phase 2 types: `RoadmapDraft/MilestoneDraft/StepDraft/LinkDraft` (in `data/RoadmapRepository.kt`) and `RoadmapWithChildren/MilestoneWithSteps/StepWithLinks` (in `data/relation/`).

**Conventions:**
- Branch `feat/domain-logic` (created in Task 1). Stay on it; never `main`.
- Tests run via `./gradlew :app:testDebugUnitTest` (these are plain JVM tests — fast, no emulator/Robolectric).
- Commit after each task. Existing domain helpers already present: `domain/Completion.kt`, `domain/Reorder.kt`.
- Integration into the repository/UI is **out of scope** here (Phase 4/5/7 wire these); this phase delivers tested functions only.

---

## File structure (this phase)

```
app/src/main/java/com/example/roadmap/domain/
  Progress.kt    (progressFraction + counts/progress extensions over the read model)
  Overdue.kt     (isOverdue core + extensions; java.time)
  ImportValidation.kt  (validateImport(RoadmapDraft): ImportValidation, §8 rules)
app/src/test/java/com/example/roadmap/domain/
  ProgressTest.kt   OverdueTest.kt   ImportValidationTest.kt
```

---

## Task 1: Progress (TDD)

**Files:** Create `domain/Progress.kt`; Test `domain/ProgressTest.kt`. (Also creates the branch.)

- [ ] **Step 1: Create the branch**
```bash
git checkout -b feat/domain-logic
```

- [ ] **Step 2: Write the failing test** — `app/src/test/java/com/example/roadmap/domain/ProgressTest.kt`:
```kotlin
package com.example.roadmap.domain

import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren
import com.example.roadmap.data.relation.StepWithLinks
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {
    private fun step(completed: Boolean) =
        StepWithLinks(StepEntity(milestoneId = 1, title = "s", completed = completed, position = 0), emptyList())

    private fun milestone(vararg completed: Boolean) =
        MilestoneWithSteps(MilestoneEntity(roadmapId = 1, title = "m", position = 0), completed.map { step(it) })

    @Test fun fraction_guards_divide_by_zero() {
        assertEquals(0f, progressFraction(0, 0), 0.0001f)
    }

    @Test fun fraction_is_completed_over_total() {
        assertEquals(0.5f, progressFraction(1, 2), 0.0001f)
        assertEquals(1f, progressFraction(3, 3), 0.0001f)
    }

    @Test fun milestone_progress_counts_its_steps() {
        val m = milestone(true, false, true, false)   // 2 of 4
        assertEquals(2, m.completedSteps())
        assertEquals(4, m.totalSteps())
        assertEquals(0.5f, m.progress(), 0.0001f)
    }

    @Test fun roadmap_progress_aggregates_across_milestones() {
        val roadmap = RoadmapWithChildren(
            RoadmapEntity(title = "r"),
            listOf(milestone(true, true), milestone(false, false, true)),   // 2/2 + 1/3 = 3/5
        )
        assertEquals(3, roadmap.completedSteps())
        assertEquals(5, roadmap.totalSteps())
        assertEquals(0.6f, roadmap.progress(), 0.0001f)
    }

    @Test fun empty_roadmap_is_zero_progress() {
        val roadmap = RoadmapWithChildren(RoadmapEntity(title = "r"), emptyList())
        assertEquals(0f, roadmap.progress(), 0.0001f)
    }
}
```

- [ ] **Step 3: Run → FAIL** — `./gradlew :app:testDebugUnitTest --tests "*ProgressTest"` → unresolved references.

- [ ] **Step 4: Create Progress.kt**
```kotlin
package com.example.roadmap.domain

import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren

/** Fraction in 0f..1f; 0 when there are no steps (spec §7.2 divide-by-zero guard). */
fun progressFraction(completed: Int, total: Int): Float =
    if (total <= 0) 0f else completed.toFloat() / total.toFloat()

fun MilestoneWithSteps.totalSteps(): Int = steps.size
fun MilestoneWithSteps.completedSteps(): Int = steps.count { it.step.completed }
fun MilestoneWithSteps.progress(): Float = progressFraction(completedSteps(), totalSteps())

fun RoadmapWithChildren.totalSteps(): Int = milestones.sumOf { it.totalSteps() }
fun RoadmapWithChildren.completedSteps(): Int = milestones.sumOf { it.completedSteps() }
fun RoadmapWithChildren.progress(): Float = progressFraction(completedSteps(), totalSteps())
```

- [ ] **Step 5: Run → PASS.**

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/example/roadmap/domain/Progress.kt app/src/test/java/com/example/roadmap/domain/ProgressTest.kt
git commit -m "domain: roadmap/milestone progress (pure, tested)"
```

---

## Task 2: Overdue (TDD)

**Files:** Create `domain/Overdue.kt`; Test `domain/OverdueTest.kt`.

- [ ] **Step 1: Write the failing test** — `app/src/test/java/com/example/roadmap/domain/OverdueTest.kt`:
```kotlin
package com.example.roadmap.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OverdueTest {
    private val today = LocalDate.of(2026, 6, 6)

    @Test fun past_deadline_and_incomplete_is_overdue() {
        assertTrue(isOverdue(deadline = "2026-06-05", isComplete = false, today = today))
    }

    @Test fun complete_is_never_overdue() {
        assertFalse(isOverdue(deadline = "2026-06-05", isComplete = true, today = today))
    }

    @Test fun future_or_today_deadline_is_not_overdue() {
        assertFalse(isOverdue("2026-06-07", isComplete = false, today = today))
        assertFalse(isOverdue("2026-06-06", isComplete = false, today = today)) // due today, not yet overdue
    }

    @Test fun null_or_unparseable_deadline_is_not_overdue() {
        assertFalse(isOverdue(null, isComplete = false, today = today))
        assertFalse(isOverdue("not-a-date", isComplete = false, today = today))
    }
}
```

- [ ] **Step 2: Run → FAIL.**

- [ ] **Step 3: Create Overdue.kt**
```kotlin
package com.example.roadmap.domain

import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren
import java.time.LocalDate

/**
 * A deadline is overdue iff it is set, parses to a date strictly before `today` (local date),
 * and the item is not complete (spec §7.3). Unparseable dates are treated as not overdue.
 */
fun isOverdue(deadline: String?, isComplete: Boolean, today: LocalDate): Boolean {
    if (deadline == null || isComplete) return false
    val date = runCatching { LocalDate.parse(deadline) }.getOrNull() ?: return false
    return date.isBefore(today)
}

/** A milestone is complete when its derived completedAt is set. */
fun MilestoneWithSteps.isOverdue(today: LocalDate): Boolean =
    isOverdue(milestone.deadline, isComplete = milestone.completedAt != null, today = today)

/** A roadmap is complete when it has steps and all of them are done. */
fun RoadmapWithChildren.isOverdue(today: LocalDate): Boolean {
    val total = totalSteps()
    val complete = total > 0 && completedSteps() == total
    return isOverdue(roadmap.deadline, isComplete = complete, today = today)
}
```

- [ ] **Step 4: Run → PASS.**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/roadmap/domain/Overdue.kt app/src/test/java/com/example/roadmap/domain/OverdueTest.kt
git commit -m "domain: overdue detection via java.time (pure, tested)"
```

---

## Task 3: Import validation (TDD)

**Files:** Create `domain/ImportValidation.kt`; Test `domain/ImportValidationTest.kt`.

- [ ] **Step 1: Write the failing test** — `app/src/test/java/com/example/roadmap/domain/ImportValidationTest.kt`:
```kotlin
package com.example.roadmap.domain

import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.MilestoneDraft
import com.example.roadmap.data.RoadmapDraft
import com.example.roadmap.data.StepDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportValidationTest {
    @Test fun minimal_valid_roadmap_passes() {
        val result = validateImport(RoadmapDraft(title = "Goal"))
        assertEquals(ImportValidation.Valid, result)
    }

    @Test fun full_valid_tree_passes() {
        val draft = RoadmapDraft(
            title = "Goal",
            milestones = listOf(
                MilestoneDraft("M", steps = listOf(
                    StepDraft("S", links = listOf(LinkDraft("https://a", "A"), LinkDraft("http://b"))),
                )),
            ),
        )
        assertEquals(ImportValidation.Valid, validateImport(draft))
    }

    @Test fun blank_roadmap_title_is_invalid() {
        assertTrue(validateImport(RoadmapDraft(title = "")) is ImportValidation.Invalid)
    }

    @Test fun overlong_title_is_invalid() {
        assertTrue(validateImport(RoadmapDraft(title = "x".repeat(201))) is ImportValidation.Invalid)
    }

    @Test fun non_http_link_is_invalid() {
        val draft = RoadmapDraft(
            title = "Goal",
            milestones = listOf(MilestoneDraft("M", steps = listOf(
                StepDraft("S", links = listOf(LinkDraft("ftp://x"))),
            ))),
        )
        assertTrue(validateImport(draft) is ImportValidation.Invalid)
    }

    @Test fun too_many_milestones_is_invalid() {
        val draft = RoadmapDraft(title = "Goal", milestones = List(101) { MilestoneDraft("M") })
        assertTrue(validateImport(draft) is ImportValidation.Invalid)
    }

    @Test fun too_many_links_is_invalid() {
        val draft = RoadmapDraft(
            title = "Goal",
            milestones = listOf(MilestoneDraft("M", steps = listOf(
                StepDraft("S", links = List(21) { LinkDraft("https://a") }),
            ))),
        )
        assertTrue(validateImport(draft) is ImportValidation.Invalid)
    }
}
```

- [ ] **Step 2: Run → FAIL.**

- [ ] **Step 3: Create ImportValidation.kt** (rules from §8: titles 1–200; descriptions ≤2000; urls http/https; caps milestones ≤100, steps ≤200/milestone, links ≤20/step)
```kotlin
package com.example.roadmap.domain

import com.example.roadmap.data.RoadmapDraft

/** Result of validating a pasted import payload against the bulk schema (spec §8). */
sealed interface ImportValidation {
    data object Valid : ImportValidation
    data class Invalid(val reason: String) : ImportValidation
}

private const val MAX_TITLE = 200
private const val MAX_DESCRIPTION = 2000
private const val MAX_MILESTONES = 100
private const val MAX_STEPS = 200
private const val MAX_LINKS = 20

private fun titleValid(t: String) = t.length in 1..MAX_TITLE
private fun descriptionValid(d: String?) = d == null || d.length <= MAX_DESCRIPTION
private fun urlValid(u: String) = u.startsWith("http://") || u.startsWith("https://")

fun validateImport(draft: RoadmapDraft): ImportValidation {
    if (!titleValid(draft.title)) return ImportValidation.Invalid("Roadmap title must be 1–$MAX_TITLE characters.")
    if (!descriptionValid(draft.description)) return ImportValidation.Invalid("Roadmap description must be ≤$MAX_DESCRIPTION characters.")
    if (draft.milestones.size > MAX_MILESTONES) return ImportValidation.Invalid("A roadmap may have at most $MAX_MILESTONES milestones.")

    draft.milestones.forEachIndexed { mi, m ->
        val mLabel = "Milestone ${mi + 1}"
        if (!titleValid(m.title)) return ImportValidation.Invalid("$mLabel title must be 1–$MAX_TITLE characters.")
        if (!descriptionValid(m.description)) return ImportValidation.Invalid("$mLabel description must be ≤$MAX_DESCRIPTION characters.")
        if (m.steps.size > MAX_STEPS) return ImportValidation.Invalid("$mLabel may have at most $MAX_STEPS steps.")

        m.steps.forEachIndexed { si, s ->
            val sLabel = "Step ${si + 1} in $mLabel"
            if (!titleValid(s.title)) return ImportValidation.Invalid("$sLabel title must be 1–$MAX_TITLE characters.")
            if (s.links.size > MAX_LINKS) return ImportValidation.Invalid("$sLabel may have at most $MAX_LINKS links.")
            s.links.forEach { l ->
                if (!urlValid(l.url)) return ImportValidation.Invalid("$sLabel has a link that is not http:// or https://: '${l.url}'.")
            }
        }
    }
    return ImportValidation.Valid
}
```

- [ ] **Step 4: Run → PASS.**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/roadmap/domain/ImportValidation.kt app/src/test/java/com/example/roadmap/domain/ImportValidationTest.kt
git commit -m "domain: import payload validation against bulk schema §8 (pure, tested)"
```

---

## Task 4: Wrap-up

- [ ] **Step 1: Full sweep** — `./gradlew :app:testDebugUnitTest :app:assembleDebug` → all pass, `BUILD SUCCESSFUL`.
- [ ] **Step 2:** Confirm no Android/Room runtime imports crept into `domain/` (these are pure functions; only `java.time` + Phase-2 data types are allowed): `grep -rn "androidx.room\|android\." app/src/main/java/com/example/roadmap/domain` returns nothing.

---

## Self-Review (completed during planning)

- **Spec §7.2 (progress):** `progressFraction` with divide-by-zero guard + milestone/roadmap aggregation extensions (Task 1) ✓.
- **Spec §7.3 (overdue):** `isOverdue(deadline, isComplete, today)` using `LocalDate`, strictly-before, complete-never-overdue, injectable `today` for tests; milestone (completedAt) and roadmap (all-steps-done) extensions (Task 2) ✓.
- **Spec §8 (import validation):** titles 1–200 at roadmap/milestone/step; descriptions ≤2000 at roadmap/milestone (steps have no description — matches `StepDraft`); url http/https; caps 100/200/20 (Task 3) ✓. Malformed-JSON handling is parse-time → **Phase 7** (not here).
- **Type consistency:** `progressFraction(Int,Int):Float`, `*.progress()/totalSteps()/completedSteps()`, `isOverdue(String?,Boolean,LocalDate):Boolean`, `ImportValidation.Valid/Invalid`, `validateImport(RoadmapDraft)` — consistent; build on real Phase-2 types verified to exist (`RoadmapDraft` etc. in `data/RoadmapRepository.kt`, relation POJOs in `data/relation/`).
- **Out of scope (tracked):** wiring progress/overdue into the detail/list UI (Phase 4/5); calling `validateImport` before `importRoadmap` in the import flow (Phase 7); a search-capable roadmap query + card projection for the list (flagged for Phase 4).
```
