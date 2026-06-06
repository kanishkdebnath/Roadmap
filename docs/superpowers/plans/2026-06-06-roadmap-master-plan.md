# Roadmap — Master Implementation Plan (Complete App)

**Goal:** Build the full offline-first Roadmap Android app described in `PRODUCT_SPEC.md`, validated against the approved mockups in `mockups/roadmap-mockup.html`.

**How to use this document:** This is the *roadmap of plans*. The complete app is decomposed into independent, sequenced phases. **Each phase gets its own detailed, bite-sized, TDD plan** (in this same folder) and produces working, testable software on its own. Build them in order; later phases depend on earlier ones as noted.

**Tech stack (from spec §11):** Kotlin · Jetpack Compose + Material 3 · Navigation-Compose · Room · kotlinx.serialization · ViewModel + StateFlow · `java.time` · `Intent.ACTION_VIEW` / share sheet.

---

## Current baseline (committed)

- `main` contains: Android scaffold (Compose BOM `2026.02.01`, Kotlin `2.2.10`, AGP `9.2.1`, `minSdk 24`), `PRODUCT_SPEC.md`, and the HTML mockups. Pushed to `origin/main`.
- The scaffold theme is the default Android Studio **Purple/Pink** template with `dynamicColor = true` — it will be fully replaced in Phase 1.

## Cross-cutting decisions (locked during design)

- **Brand:** `#022e1c`. **Light** = brand as the interactive accent. **Dark** = brand as the deep canvas + a derived **mint `#34d39a`** accent.
- **Dynamic color is OFF** — the brand is fixed across devices (do not use `dynamic*ColorScheme`).
- **Progress rings stay uniform brand-colored** (not per-hue). The per-item **hue system** (6 hues) colors *icon tiles, milestone spines, and empty-state art only*.
- **Typography:** Inter (bundled for offline), tabular numerals for counts/percentages.
- **`minSdk` will be raised 24 → 26** in Phase 1. Rationale: native `java.time` (no desugaring) and native variable/static-font + `FontVariation` support; matches spec §11's recommended API 26. If the user insists on 24, add core-library desugaring in Phase 2 and bundle static Inter weights only.

---

## Phases

### Phase 1 — Design System  ← **build this first**
**Plan:** `2026-06-06-design-system.md`
Build config (minSdk 26, drop dynamic color), bundled Inter typography, full color tokens (light/dark + extended `RoadmapColors` + M3 `ColorScheme`), shapes/spacing, `RoadmapTheme` with extended-color plumbing, the hue system, and the core component library: `RingProgress`, `SegmentedControl`, `GradientTile`, chips (meta/deadline/link), buttons + FAB, `StepCheckbox`, drag `Grip`, `AddInline`, `EmptyState`, and a previewable component **catalog**.
**Delivers:** a self-contained, previewable component kit matching the mockup. No data, no navigation yet.
**Depends on:** baseline only.

### Phase 2 — Data Layer
Add Room + kotlinx.serialization. Entities `Roadmap`/`Milestone`/`Step`/`Link` with FK `ON DELETE CASCADE` and `position` indices (spec §6); DAOs; `RoadmapWithChildren` `@Relation` read model exposed as `Flow`; `RoadmapDatabase`; `RoadmapRepository` (interface + Room impl); `@Transaction` for delete-cascade/reorder/import.
**Delivers:** persistence with instrumented Room tests (in-memory DB).
**Depends on:** baseline (independent of Phase 1; can run in parallel).

### Phase 3 — Domain Logic
Pure-Kotlin, fully JVM-unit-tested: `recomputeMilestoneCompletedAt` (§7.1), roadmap/milestone progress (§7.2), `isOverdue` via `java.time` (§7.3), `validateReorderIds` (§7.4), and import validation against the bulk schema (§8).
**Delivers:** tested domain functions used by the repository and view models.
**Depends on:** Phase 2 (entity types).

### Phase 4 — Roadmaps List Screen (F1, F2-create, F8-entry)
`RoadmapListViewModel` (StateFlow over repository); Navigation-Compose host (List → Detail by id); list UI using Phase 1 components — segmented Active/Archive, search, card grid, empty/no-results states, New-roadmap dialog, Import entry point.
**Depends on:** Phases 1, 2, 3.

### Phase 5 — Roadmap Detail Screen (F2–F7)
`RoadmapDetailViewModel`; responsive header (title/description/ring/Edit-Archive-Delete), milestone cards, step rows (checkbox + inline edit), link chips, inline add for milestones/steps; New/Edit-Milestone, Edit-Step-Links, and Delete-confirm dialogs.
**Depends on:** Phases 1, 2, 3.

### Phase 6 — Drag-to-Reorder (F3.4, F4.5)
Reorderable milestone & step lists; persist contiguous `position` via `validateReorderIds` in a transaction.
**Depends on:** Phase 5.

### Phase 7 — Import / Export (F8, F9)
Import dialog (Prompt + Paste panels) → validate (Phase 3) → atomic tree insert (Phase 2). Export single/all to JSON via `FileProvider` + share sheet; round-trip safe.
**Depends on:** Phases 2, 3, 4.

### Phase 8 — Polish & Non-Functionals (§10)
Stagger + check/strike animations, accessibility (state conveyed by text/icon not color alone, semantics, touch targets), edge-to-edge + per-theme status bar, final QA pass against the mockup.
**Depends on:** Phases 4–7.

---

## Dependency graph

```
baseline ─┬─ Phase 1 (Design System) ───────────────┐
          └─ Phase 2 (Data) ─ Phase 3 (Domain) ─┬──── Phase 4 (List) ─┬─ Phase 7 (Import/Export)
                                                 └──── Phase 5 (Detail) ─ Phase 6 (Reorder)
                                                                          └─ Phase 8 (Polish)
```

Phase 1 and Phases 2→3 are independent and may proceed in parallel. Phases 4 & 5 join both tracks.

## Per-phase definition of done

Compiles (`./gradlew :app:assembleDebug`), unit tests green (`./gradlew testDebugUnitTest`), instrumented tests green where present, previews render, and a self-contained slice of behavior works. Commit frequently; one feature-branch per phase, merged to `main` when the phase's DoD is met.
