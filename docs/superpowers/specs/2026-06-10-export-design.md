# JSON Export (F9) — Design Spec

**Status:** Approved design, 2026-06-10
**Parent spec:** [PRODUCT_SPEC.md](../../../PRODUCT_SPEC.md) §9 (F9), §8 (import/export JSON schema), §11 (stack: `kotlinx.serialization`, `FileProvider`).
**Scope:** Export a **single** roadmap to a JSON snapshot (the §8 schema) via the Android share sheet, round-trip-safe with the existing import (F8). One PR against `main`.

## 1. Goal & non-goals

**Goal (F9.1 / F9.2):** From a roadmap's detail screen, produce a JSON document matching the §8 import schema and hand it to the Android share sheet ("Save to Files," messaging, etc.). The exported JSON re-imports cleanly — a faithful round-trip.

**Non-goals (this PR):** "Export all roadmaps" (the §8 schema is a single roadmap object; a bulk/array format would also require extending the import to round-trip — deferred). No new export *format* — it reuses the import schema verbatim. No repository/data-model changes (export is read-only).

## 2. Architecture

One small, cohesive slice; no new layers.

- **`data/ExportJson.kt`** (pure, JVM-testable) — the inverse of `RoomRoadmapRepository.importRoadmap`:
  - `fun RoadmapWithChildren.toDraft(): RoadmapDraft` — maps the **sorted** tree to the existing `@Serializable` `RoadmapDraft`/`MilestoneDraft`/`StepDraft`/`LinkDraft` (already defined in `data/RoadmapRepository.kt`). Carries `title`, `description`, `deadline` at roadmap/milestone level; `title`, `completed`, `links` at step level; `url`, `label` at link level.
  - `fun exportRoadmapJson(tree: RoadmapWithChildren): String` — `Json { prettyPrint = true }.encodeToString(tree.toDraft())`.
  - The caller passes a `sorted()` tree (the detail VM already exposes one), so child order is deterministic.
- **`FileProvider`** — a new `<provider>` in `AndroidManifest.xml` (`androidx.core.content.FileProvider`, authority `${applicationId}.fileprovider`) + `res/xml/file_paths.xml` exposing a `cache-path` for an `exports/` subdir.
- **Detail UI** — a share `IconButton` in `RoadmapDetailScreen`'s `TopAppBar` `actions`, wired via a new `DetailCallbacks.onExport: () -> Unit`. `RoadmapDetailRoute` implements `onExport`: serialize the loaded tree, write the file, and launch the share chooser via `LocalContext`.

## 3. Data flow

1. User taps the share icon on the roadmap detail screen → `cb.onExport()`.
2. `RoadmapDetailRoute` reads the already-loaded `RoadmapWithChildren` (the VM's `state.roadmap`, already sorted), calls `exportRoadmapJson(tree)`.
3. Writes the string to `cacheDir/exports/<slug>.json` (slug = roadmap title → safe filename; fallback `roadmap.json`).
4. `FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)` → content URI.
5. `Intent(ACTION_SEND) { type="application/json"; putExtra(EXTRA_STREAM, uri); addFlags(FLAG_GRANT_READ_URI_PERMISSION) }`, wrapped in `Intent.createChooser`, `startActivity`.

## 4. Round-trip (F9.2)

Because export serializes the same `RoadmapDraft` the importer parses, `exportRoadmapJson(tree)` → `parseRoadmapJson(...)` yields a `RoadmapDraft` equal to `tree.toDraft()`. Preserved: titles, descriptions, deadlines, step `completed`, links (url + label), and ordering. **Not** serialized: DB ids, timestamps, `archived`, and milestone `completedAt` (derived) — all re-established on import (`importRoadmap` assigns ids/positions/timestamps and `recompute`s `completedAt`), so the round-trip is faithful at the content level.

## 5. Error handling

- The share icon is only shown/enabled once the tree is loaded (the route already gates on `tree != null`).
- File-write or `startActivity` failure is caught and surfaced as a non-fatal toast ("Couldn't export"); nothing is mutated (export is read-only).
- Empty roadmap (no milestones) still exports a valid object (`milestones: []`).

## 6. Testing

- **Pure (plain JUnit), `ExportJsonTest`:**
  - `toDraft` maps a `RoadmapWithChildren` (built with the existing `sampleTree()` helper or a hand-built tree) to the expected `RoadmapDraft` — titles/deadlines/completed/links/order.
  - **Round-trip:** `parseRoadmapJson(exportRoadmapJson(tree)).getOrThrow()` equals `tree.toDraft()` (data classes give structural equality).
  - Pretty-printed output is valid JSON the importer accepts.
- **On-device (emulator):** open a roadmap → tap share → the system share sheet appears with a `.json`; "Save to Files" produces a file whose contents paste back through the import dialog and recreate the roadmap.

## 7. Files

- Create: `app/src/main/java/com/example/roadmap/data/ExportJson.kt`; `app/src/main/res/xml/file_paths.xml`; test `app/src/test/java/com/example/roadmap/data/ExportJsonTest.kt`.
- Modify: `app/src/main/AndroidManifest.xml` (add `<provider>`); `ui/detail/RoadmapDetailScreen.kt` (`DetailCallbacks.onExport` + share `IconButton`); `ui/detail/RoadmapDetailRoute.kt` (implement `onExport` — serialize, write, share); `ui/detail/RoadmapDetailScreen.kt` `noopCallbacks()` (add `onExport = {}`).

## 8. Resolved decisions

| Decision | Choice | Why |
|---|---|---|
| Scope | Single roadmap | Round-trips with the §8 single-object import; "all" deferred (would need array schema + import change). |
| Share mechanism | `FileProvider` + `ACTION_SEND` (`application/json`) | Spec §322 names FileProvider; enables "Save to Files" + widest app reach. (Raw-text `ACTION_SEND` and SAF `CREATE_DOCUMENT` considered and rejected.) |
| Trigger | Share `IconButton` in the detail `TopAppBar` | Spec §365 puts F9 on the detail screen; standard, discoverable. |
| Format | Reuse `RoadmapDraft` + `Json { prettyPrint = true }` | Guarantees round-trip with import; human-readable. |
| File location | `cacheDir/exports/<slug>.json` via `cache-path` | No storage permission; transient; FileProvider-shareable. |
