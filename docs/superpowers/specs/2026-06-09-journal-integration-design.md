# Journal Integration — Design Spec

**Status:** Approved design, 2026-06-09
**Parent specs:** [JOURNAL_SPEC.md](../../../JOURNAL_SPEC.md) (feature requirements J1–J8, §4 data model, §5 logic), [PRODUCT_SPEC.md](../../../PRODUCT_SPEC.md) (inherited stack, design tokens, conventions)
**Scope:** Integrate the Journal sub-feature into the Roadmap Android app as a second top-level destination, with easy navigation between Roadmaps and Journal. Built in four sequential PRs against `main`.

Visual designs (navigation pattern, month heatmap cell style, full day editor, dark mode) were validated interactively via the brainstorming Visual Companion before this spec was written. This document is the source of truth for the implementation plan; it captures the decisions, not a reproduction of the mockups.

---

## 1. Goals & non-goals

**Goal:** A faithful port of the pathforge journal module — one entry per calendar day (mood + tags, optional summary, timestamped events, links, and references to this app's roadmaps/milestones) — living in the same on-device Room DB so a day can point at the roadmap it describes, with no network.

**Non-goals (v1, per JOURNAL_SPEC §2):** no search, no charts/analytics/streaks, no attachments, no custom mood tags, no per-event auto-save (whole-day save only), no `job` references, no sync/accounts. Deferred items (back-reference view, reminders, JSON export/import, quick-capture) are out of scope — see §11.

---

## 2. Architecture & layering

Follows the existing `ui → domain → data` dependency flow. `domain/` stays pure Kotlin (`java.time` + data types only).

```
data/journal/        entities, DAOs, read models, JournalRepository (+ impl)
  ├─ entity/         JournalDayEntity, JournalEventEntity, JournalLinkEntity, JournalReferenceEntity
  ├─ dao/            JournalDayDao, JournalEventDao, JournalLinkDao, JournalReferenceDao
  ├─ relation/       JournalDayWithChildren (+ sorted()), DayMoodCell, RefTarget, ResolvedReference
  ├─ MoodTag.kt      enum (8 values); @Serializable
  ├─ RefType.kt      enum (roadmap, milestone)
  ├─ JournalConverters.kt   List<MoodTag> ⇄ JSON String (kotlinx.serialization)
  ├─ JournalDraft.kt        editable day DTO (repository input; mirrors RoadmapDraft)
  └─ JournalRepository.kt / RoomJournalRepository.kt
ui/journal/          MonthHeatmapScreen, DayEditorScreen + sub-editors, ViewModels, Routes
domain/              JournalDirty.kt — normalized() + isDirty() over JournalDraft (pure)
ui/theme/            two new tokens (amber, sky) + moodTint()/moodAccent() extensions
ui/RoadmapApp.kt     refactored to a top-level NavigationBar (Roadmaps | Journal)
```

Wiring stays manual through `RoadmapGraph` (no Hilt). `MainActivity` already obtains the repository and theme store from the graph; it will also obtain the `JournalRepository`.

---

## 3. Data model (Room v2 — additive migration)

`RoadmapDatabase` goes from **version 1 → 2**. The four journal tables are *added*; the existing roadmap tables are untouched, so a real installed DB keeps all its data. A `Migration(1, 2)` with `CREATE TABLE` + index statements is registered on the builder in `RoadmapGraph`. (`exportSchema = false` is retained; **no** destructive fallback — that would wipe the user's roadmaps.)

Embedded Mongo arrays from pathforge become relational child tables with a 0-based `position` column and `ForeignKey(onDelete = CASCADE)` to `journal_day`.

| Table | Columns | Notes |
|---|---|---|
| `journal_day` | `id` PK autoGen; `date` TEXT **UNIQUE** (ISO `YYYY-MM-DD`); `moodScale` INT (1–5); `moodTags` TEXT (JSON list, ≤3); `summary` TEXT? (≤500); `createdAt` INT; `updatedAt` INT | One row per date. Index on `date` (unique). |
| `journal_event` | `id` PK; `dayId` FK→journal_day CASCADE; `text` TEXT (1–500); `important` INT(bool); `time` TEXT? (≤20); `position` INT | Cap 20. Index `dayId`. |
| `journal_link` | `id` PK; `dayId` FK CASCADE; `url` TEXT (http/https); `label` TEXT?; `position` INT | Cap 10. Index `dayId`. Reuses the roadmap link model. |
| `journal_reference` | `id` PK; `dayId` FK CASCADE; `type` TEXT (roadmap\|milestone); `roadmapId` INT (**plain column, not a FK**); `milestoneId` INT? (when milestone); `position` INT | Cap 10. Indices `dayId`, `roadmapId`, `milestoneId`. |

**Enums:** `MoodTag { focused, tired, anxious, grateful, restless, excited, low, calm }` (`@Serializable`); `RefType { roadmap, milestone }`.

**`moodTags` storage:** a Room `TypeConverter` serializes `List<MoodTag>` to/from a JSON string via kotlinx.serialization (already a project dependency). The ≤3 cap is enforced in the domain/UI layer, not the column.

**Critical: references are NOT foreign keys.** `journal_reference.roadmapId` / `milestoneId` are plain integer columns with no cascade. Deleting a roadmap leaves the reference row intact; the label resolves to "(deleted)" at render. This is faithful to pathforge and the reason journal lives in this app (it can join against local roadmap/milestone tables).

### Read models

- **`JournalDayWithChildren`** — `@Relation` lists for events/links/references; a `sorted()` extension orders every child list by `position` (mirrors `RoadmapWithChildren.sorted()`, since Room does not order `@Relation` lists). Feeds the day editor.
- **`DayMoodCell`** — lightweight `(date, moodScale)` projection for the heatmap (no children loaded).
- **`ResolvedReference`** — a `journal_reference` LEFT-JOINed to roadmap/milestone to carry the resolved title (or null → "(deleted)"). No denormalized label is ever stored.
- **`RefTarget`** — `(type, roadmapId, milestoneId?, title)` rows for the reference picker, queried from the existing roadmap/milestone tables.

---

## 4. Repository

`JournalRepository` (interface) + `RoomJournalRepository` (impl) is the only journal surface the UI touches — mirrors `RoomRoadmapRepository`. It injects `now: () -> Long` for deterministic timestamps in tests and wraps multi-write operations in `db.withTransaction`.

```
fun observeMonth(yearMonth: YearMonth): Flow<List<DayMoodCell>>   // heatmap tints
fun observeDay(date: LocalDate): Flow<JournalDayWithChildren?>     // null ⇒ empty draft
fun searchReferenceTargets(query: String): Flow<List<RefTarget>>  // picker, over roadmap/milestone
suspend fun saveDay(draft: JournalDraft)                          // full-day atomic upsert
suspend fun deleteDay(date: LocalDate)
```

**Full-day atomic upsert** (the central operation, mirrors pathforge's PUT-replace) in one `@Transaction`:
1. Upsert `journal_day` by `date` — **preserve `createdAt`**, bump `updatedAt = now()`.
2. Delete all of the day's events/links/references, then re-insert from the draft, assigning `position` by list index.

No per-child mutations exist — the whole day is the unit of write. ViewModels never compute timestamps or positions; they call `saveDay`/`deleteDay`.

---

## 5. Domain logic (pure, JVM-tested)

- **Dirty-check** drives the Save button. `JournalDraft` is a **data-layer DTO** (mirrors `RoadmapDraft`; it is the repository's `saveDay` input). The dirty-check lives in `domain` as pure functions over that DTO — `domain → data` is the allowed dependency direction here, the same way the existing `Progress` extensions operate over `RoadmapWithChildren`. `normalized()` trims text and drops blank/trailing children; `isDirty = draft.normalized() != saved.normalized()`. Save is enabled only when dirty **and** mood is set.
- **Validation** reuses the parent: link `url` must be http/https (reuse the roadmap link validator); caps (events ≤20, links ≤10, refs ≤10, tags ≤3) enforced before save; mood (1–5) required to save.
- **Date helpers** reuse the existing parent `java.time` `LocalDate` utilities (today, month-of, month grid, weekday labels, shift-month). No new date math.

Mood **color** mapping is a UI concern (maps to Compose `Color` tokens) and lives in `ui/theme` — see §7.

---

## 6. Navigation integration

`ui/RoadmapApp.kt` is refactored from a single `detailId: Long?` switch into a **two-level** machine:

- **Top level:** `enum Tab { Roadmaps, Journal }` held in `rememberSaveable`, rendered as a Material 3 `NavigationBar` with two items (Roadmaps — outlined map icon; Journal — calendar/book icon). One tap switches sections.
- **Sub-level:** each tab keeps its own drill-down state — Roadmaps: `roadmapDetailId: Long?`; Journal: `editorDate: LocalDate?`.
- **Bottom bar visibility:** visible at each tab's root (roadmap list, month heatmap); **hidden when drilled into a detail** (roadmap detail or day editor) for full-height content and a clear "deeper" signal.
- `BackHandler`: if a detail is open, back collapses to that tab's root; otherwise default.
- **Responsive:** `NavigationBar` on compact width (phone portrait); `NavigationRail` on expanded width (tablet/landscape), consistent with the spec's responsive requirement.

Journal **opens to today**: selecting the Journal tab lands on the current month's heatmap with today's cell ringed.

---

## 7. UI — screens & components

### Visual tokens
Three new token pairs are added to `RoadmapColors` (and both `LightTokens`/`DarkTokens`): **`amber`/`amberContainer`**, **`sky`/`skyContainer`**, and **`neutral`/`neutralContainer`** (mood 3). Mood color is exposed as extensions:

```kotlin
fun RoadmapColors.moodAccent(scale: Int): Color  // 1 overdue · 2 amber · 3 neutral · 4 done · 5 sky
fun RoadmapColors.moodTint(scale: Int): Color     // container variants of the above
```

**Resolved decision:** mood **5 = sky**, not brand. (JOURNAL_SPEC J1 says "sky", §5 says "brand"; brand deep-green is visually identical to the emerald used for mood 4, so sky is used to keep the scale legible.)

Mood palette (hex from the validated mockups):

| Scale | Label | Light accent / tint | Dark accent / tint |
|---|---|---|---|
| 1 | Rough | `#DC2626` / `#FBE8E8` | `#F87171` / `#2A1717` |
| 2 | Low | `#F59E0B` / `#FCEFD2` | `#FBBF24` / `#2C2510` |
| 3 | Okay | `#94A3B8` / `#EEF1F0` | `#93AB9D` / `#1B2A21` |
| 4 | Good | `#047857` / `#E2F3EC` | `#34D399` / `#10301F` |
| 5 | Great | `#0EA5E9` / `#E0F2FE` | `#38BDF8` / `#0E2A38` |

(1 and 4 reuse existing `overdue/overdueContainer` and `done/doneContainer` tokens.)

### MonthHeatmapScreen (J1)
Custom Compose calendar grid. **Cell style: soft tint + number** — each day shows its date number on a pale `moodTint` wash; empty days are plain surface with a faint outline; today gets a brand ring. Month nav (`‹ June 2026 ›`), weekday header, a mood legend. Tapping any day opens its editor (empty template if no entry). Reads `observeMonth` reactively.

### DayEditorScreen (J2–J7)
Single vertical scroll, top app bar with back · date title · **Save** (text action, enabled only when dirty) · overflow (Delete). Sections top-to-bottom:

- **MoodSelector (J3, required):** 5 emoji faces (😞😔😐🙂😄 → 1–5); selected face tinted + ring. Below: 8 tag chips, up to 3 selectable, with a "N selected" hint.
- **Summary (J4):** optional single-line field, `N / 500` counter.
- **EventsEditor (J5):** card of rows — optional time pill, text, important ★ toggle; ordered (append on add; `position` assigned by index on save); `+ Add event`; cap 20.
- **LinksEditor (J6):** reuses the existing `LinkChip`; `+ Add link`; cap 10; http/https only.
- **ReferencesEditor (J7):** chips for roadmap (🗺) / milestone (◆) targets; tapping a live chip navigates to that roadmap; ✕ removes; deleted targets render struck-through "(deleted)". `+ Add` opens the **ReferencePicker**.
- **Delete entry:** danger action at the bottom (mirrors roadmap detail), with a confirm dialog.

### ReferencePicker
A searchable list/dialog over `searchReferenceTargets` — the user filters their existing roadmaps/milestones and taps to add (up to 10). This local query is the reason journal belongs in this app.

### Responsiveness
Phone (compact): stacked — heatmap is its own screen; tapping a day pushes the editor. Tablet/expanded: side-by-side master-detail (heatmap left, editor right), like the roadmap detail intent.

### Reuse
`LinkChip`, `MetaChip`, buttons, `EmptyState`, `SegmentedControl`, `RingProgress` patterns, and the `RoadmapTheme.colors` token system are reused — no reinvented primitives.

---

## 8. Edge cases

- **Mood required:** Save disabled until a 1–5 mood is chosen.
- **One entry per date:** enforced by the `date` UNIQUE column + upsert-by-date.
- **Deleted reference target:** row persists (non-cascading id columns); renders struck-through "(deleted)"; still removable; never navigable.
- **Caps:** events 20 / links 10 / refs 10 / tags 3 — enforced before save; the relevant "Add" affordance disables at the cap.
- **Empty day opened from heatmap:** loads an empty draft; nothing is written until Save.
- **Dirty-check fairness:** compare *normalized* drafts (trimmed, blanks dropped) so whitespace-only edits don't enable Save.

---

## 9. Testing strategy

Per the project harness — everything is JVM-testable, no emulator in the loop:

- **Domain** (`JournalDraft.normalized()`, dirty-check, cap/validation helpers) → plain JUnit.
- **DAOs, `RoomJournalRepository`, ViewModels** → **Robolectric** `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])` with `Room.inMemoryDatabaseBuilder(...)`. Repo tests assert: full-day upsert preserves `createdAt` + bumps `updatedAt`; delete+reinsert reassigns `position`; reference resolution returns null title for deleted targets; `observeMonth` emits correct mood cells. ViewModel tests use `Dispatchers.setMain(StandardTestDispatcher())` + `runTest` + `advanceUntilIdle()`.
- **Migration(1, 2)** → a Robolectric test opens a v1 DB with seeded roadmap data, runs the migration, asserts the journal tables exist and the roadmap rows survive.
- **Compose UI** → no unit tests (no CI emulator); verify via `@Preview` (light + dark) + `installDebug` on the emulator.

---

## 10. Phasing — four PRs against `main`

Each PR is modular, conventionally committed, independently reviewable, and lands its own tests.

1. **Data layer** (`data:`/`domain:`/`test:`) — 4 entities, enums, converters, DAOs, read models, `JournalRepository` + impl with full-day upsert, `Migration(1, 2)`, `RoadmapGraph` wiring, mood-token additions + `moodTint/moodAccent`, dirty-check domain. No UI. Full Robolectric + JUnit coverage incl. the migration test.
2. **Navigation shell** (`ui:`) — refactor `RoadmapApp.kt` to the `NavigationBar`/`NavigationRail` two-level machine; Journal tab renders a minimal heatmap scaffold (current month, today ringed, empty cells). Roadmaps tab behavior unchanged. Verify on emulator.
3. **Month heatmap** (`ui:`) — real reactive heatmap (soft-tint cells, month nav, legend, today ring) reading `observeMonth`; tapping a day opens a stub editor. `@Preview` light/dark.
4. **Day editor** (`ui:`) — full J2–J7: MoodSelector, summary, EventsEditor, LinksEditor, ReferencesEditor + ReferencePicker, Save (dirty-gated) / Delete (confirm), reference navigation, responsive master-detail. `@Preview` light/dark + emulator verification.

---

## 11. Deferred (out of scope for v1)

Per JOURNAL_SPEC §9: back-reference view ("N entries reference this milestone" — the `roadmapId`/`milestoneId` indices already support it), search, mood trends/charts/streaks, tag & reference filters, reminders (WorkManager), attachments, JSON export/import (to match the roadmap import/export), custom tags, and a "today" quick-capture widget (no overview/dashboard screen exists yet).

---

## 12. Resolved decisions

| Decision | Choice | Why |
|---|---|---|
| Top-level navigation | Bottom `NavigationBar` (→ `NavigationRail` on wide) | Two destinations; thumb-reachable; cleanest fit with the manual state machine. |
| Bottom bar in detail views | Hidden | More content height; clear "drilled deeper" signal. |
| Heatmap cell style | Soft tint + number | Reads as calendar *and* heatmap; reuses container tokens; on-brand. |
| Mood scale 5 color | Sky (new token) | Brand deep-green collides with mood-4 emerald. |
| "Opens to today" | Current month, today ringed | Calendar-first; editor one tap away. |
| Save placement | App bar, dirty-gated | Matches existing detail screens. |
| DB migration | Additive `Migration(1, 2)` | Preserve existing roadmap data; no destructive fallback. |
| `moodTags` storage | JSON column via kotlinx.serialization `TypeConverter` | Dependency already present; faithful to §4. |
