# Phase 7a — Import from LLM (F8) — Design

**Status:** approved (brainstorming) · **Date:** 2026-06-08
**Spec source of truth:** `PRODUCT_SPEC.md` F8 + §8 (import schema). Approved visuals: `mockups/roadmap-mockup.html` (the "F8 · Import (Prompt + Paste)" device).

## Goal

Let the user generate a roadmap with any LLM and paste it back in: an **Import roadmap** dialog with a copyable **Prompt** panel (the user's goal baked in) and a **Paste** panel that validates live and atomically creates the whole roadmap tree. **The app makes no network calls** — the LLM round-trip is manual.

## Scope

- **In:** F8 paste-JSON import end to end (parse → validate → atomic insert) + the dialog UI + the prompt builder.
- **Out (separate follow-up PR):** F9 JSON export / share sheet.

## Decisions (locked in brainstorming)

1. **Package:** import-only, its own PR. Export follows next.
2. **Prompt panel:** an in-app **"Your goal"** field; the copied prompt already contains the goal (smoothest round-trip).
3. **Paste parsing:** **lenient** — strip ```` ```json ```` fences / surrounding prose before parsing.

## Existing building blocks (reused as-is)

- `domain/ImportValidation.kt` → `validateImport(draft: RoadmapDraft): ImportValidation` (`Valid` / `Invalid(reason)`), enforcing §8 caps (title 1–200, desc ≤2000, milestones ≤100, steps ≤200/milestone, links ≤20/step, url http/https).
- `data/RoadmapRepository.kt` draft types — **already mirror §8 exactly**:
  - `LinkDraft(url: String, label: String? = null)`
  - `StepDraft(title: String, completed: Boolean = false, links: List<LinkDraft> = emptyList())`
  - `MilestoneDraft(title: String, description: String? = null, deadline: String? = null, steps: List<StepDraft> = emptyList())`
  - `RoadmapDraft(title: String, description: String? = null, deadline: String? = null, milestones: List<MilestoneDraft> = emptyList())`
- `RoomRoadmapRepository.importRoadmap(draft): Long` — atomic (`db.withTransaction`) tree insert, `position` by array index, returns new roadmap id. Imported roadmaps are not archived → appear in **Active**.
- `ui/list/RoadmapListScreen.kt` already exposes an `onImport: () -> Unit` entry point (app-bar icon + empty-state "Import" button), currently a `TODO Phase 7` stub.

## New components

### 1. Gradle — kotlinx.serialization
Add the Kotlin serialization **plugin** + **kotlinx-serialization-json** runtime (version catalog + `app/build.gradle.kts`), mirroring how `reorderable` was added. Plugin id `org.jetbrains.kotlin.plugin.serialization` at the project Kotlin version (`2.2.10`); runtime `org.jetbrains.kotlinx:kotlinx-serialization-json`.

### 2. `@Serializable` draft types
Annotate the four draft data classes in `data/RoadmapRepository.kt` with `@Serializable` (add the import). Their field names/optionality already equal the §8 keys, so no separate DTOs. Defaults (`completed=false`, optional desc/deadline/links) come from the data-class defaults + serialization default handling.

### 3. `data/ImportJson.kt` (new) — pure, JVM-testable
kotlinx.serialization is a JVM library, so this file needs **no Android/Room** and is tested with plain JUnit.

```kotlin
private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

/** Lenient: pull JSON out of ```fences/prose, then decode. */
fun parseRoadmapJson(raw: String): Result<RoadmapDraft>

/** Compose the copyable LLM prompt with the goal injected. */
fun buildImportPrompt(goal: String): String

sealed interface ImportPreview {
    data object Empty : ImportPreview
    data class Invalid(val reason: String) : ImportPreview
    data class Valid(val draft: RoadmapDraft, val milestones: Int, val steps: Int) : ImportPreview
}

/** parse → validate, for the live note. */
fun previewImport(raw: String): ImportPreview
```

**`parseRoadmapJson` lenient algorithm (exact):**
1. `trimmed = raw.trim()`. If blank → `Result.failure`.
2. If a fenced block ```` ``` ```` is present, take the content of the **first** fenced block (strip the opening fence + optional language tag like `json`, and the closing fence).
3. From the result, take the substring from the **first `{`** to the **last `}`** (inclusive). If either is absent → `Result.failure`.
4. `runCatching { json.decodeFromString<RoadmapDraft>(extracted) }` — any `SerializationException` / `IllegalArgumentException` → `Result.failure`.

**`previewImport`:**
- `raw.isBlank()` → `Empty`.
- `parseRoadmapJson` fails → `Invalid("Couldn't read the JSON — paste the whole object the LLM returned.")`.
- else `validateImport(draft)`: `Invalid(reason)` passes the reason through; `Valid` → `Valid(draft, draft.milestones.size, draft.milestones.sumOf { it.steps.size })`.

**`buildImportPrompt(goal)`** — `goal.ifBlank { "<describe your goal>" }` injected into:
```
You are an expert planner. Create a structured roadmap as JSON for this goal:

"<goal>"

Return ONLY one JSON object — no markdown, no code fences, no commentary before or after.

Shape:
{
  "title": "string, 1-200 chars",
  "description": "string, optional",
  "deadline": "YYYY-MM-DD, optional",
  "milestones": [
    {
      "title": "string, 1-200 chars",
      "description": "string, optional",
      "deadline": "YYYY-MM-DD, optional",
      "steps": [
        { "title": "string, 1-200 chars", "completed": false,
          "links": [ { "url": "https://...", "label": "string, optional" } ] }
      ]
    }
  ]
}

Rules:
- Break the goal into 4-8 milestones, each with 3-8 concrete steps.
- Every "title" is 1-200 characters.
- "links" is optional; if present, each "url" MUST start with http:// or https://.
- Set "completed" to false for every step.
- Output the JSON object only.
```

### 4. `ui/import/ImportDialog.kt` (new) — Compose
A scrollable `Dialog` (`usePlatformDefaultWidth = false`) with a `Surface` (shape `large`, near-full-width, max-height ~85% screen, `imePadding`, vertically scrollable Column) matching the mockup:
- **Header:** a cyan rounded icon tile (`RoadmapHue.Cyan` gradient) with a download glyph (`Icons.Rounded.Download`/`SaveAlt`), title **"Import roadmap"**, subtitle *"Generate JSON with any LLM, then paste it back. The app makes no network calls."*
- **"Your goal"** single-line field.
- **Panel "1 · Prompt template"** + **Copy** button → copy `buildImportPrompt(goal)` via `LocalClipboardManager`. A read-only, monospace, bordered box (`surfaceVariant`) showing the composed prompt (height-bounded + scroll).
- **Panel "2 · Paste JSON"** — multiline monospace field.
- **Live note:** from `previewImport(paste)` — hidden when `Empty`; green check + *"Valid · 1 roadmap, N milestone(s), K step(s) — imported atomically."* when `Valid`; error-color + reason when `Invalid`.
- **Footer:** `Cancel` (text) + `Import` (primary) — **Import enabled only when `Valid`**.

State is local (`remember`: goal, paste); the dialog calls back `onImport(draft: RoadmapDraft)` and `onDismiss()`.

### 5. ViewModel + wiring
- `RoadmapListViewModel.importRoadmap(draft: RoadmapDraft)` = `launch { repository.importRoadmap(draft) }` (fire-and-forget; the reactive Active list updates).
- The stateful list host `RoadmapListRoute` (in `RoadmapListScreen.kt`, alongside the existing `showNew`/`NewRoadmapDialog` state) holds `var showImport by remember`; `onImport = { showImport = true }`; renders `ImportDialog(onDismiss = { showImport = false }, onImport = { draft -> vm.importRoadmap(draft); showImport = false })`.

## Data flow

Tap Import → type goal → **Copy** prompt → (paste into any LLM, get JSON) → paste JSON → `previewImport` runs on each change → note shows Valid/Invalid → **Import** (enabled iff Valid) → `vm.importRoadmap(draft)` → `repository.importRoadmap` (atomic) → dialog closes → roadmap appears in **Active**.

## Error handling

| Case | Result |
|------|--------|
| Empty paste | No note; Import disabled |
| Malformed / non-JSON / no `{…}` | `Invalid("Couldn't read the JSON — paste the whole object the LLM returned.")`; Import disabled |
| ```` ```json ```` fence or prose around JSON | Stripped by lenient extraction, then parsed normally |
| Title length / non-http link / array caps | `Invalid(<exact reason from validateImport>)`; Import disabled |
| Valid | Green note with counts; Import enabled |

Import only ever runs on a `Valid` preview, so `repository.importRoadmap`'s own validation never rejects — it stays atomic and cannot partially write.

## Testing

**JVM unit (plain JUnit) — `data/ImportJsonTest.kt`:**
- `parseRoadmapJson`: clean object; ```` ```json ```` fenced; prose-wrapped ("Here's your JSON: { … } Hope this helps!"); unknown keys ignored; `completed` default applied when omitted; malformed → failure; no-braces → failure.
- `buildImportPrompt`: contains the goal text; contains the schema keys + http(s) rule; blank goal → `<describe your goal>`.
- `previewImport`: blank → `Empty`; bad JSON → `Invalid`; non-http link → `Invalid` (reason from validate); 3-milestone valid → `Valid` with correct milestone/step counts.

**Robolectric — `RoadmapListViewModelTest.kt`:**
- `importRoadmap(draft)` persists the full tree (roadmap + milestones + steps + links, ordered by index) and it surfaces in the Active list flow.

**Emulator (manual):**
- Real round-trip: paste LLM-style JSON **with a ```` ```json ```` fence** → live note shows Valid + counts → Import → roadmap opens/appears in Active with the right milestones/steps/links. Also paste malformed JSON and a non-http link → red note, Import disabled, nothing created.

## File structure

| File | Change | Responsibility |
|------|--------|----------------|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | modify | serialization plugin + json runtime |
| `data/RoadmapRepository.kt` | modify | `@Serializable` on the 4 draft types |
| `data/ImportJson.kt` | create | `parseRoadmapJson`, `buildImportPrompt`, `ImportPreview`, `previewImport` |
| `test/.../data/ImportJsonTest.kt` | create | JVM unit tests for the above |
| `ui/import/ImportDialog.kt` | create | the Import dialog composable |
| `ui/list/RoadmapListViewModel.kt` | modify | `importRoadmap(draft)` |
| `test/.../ui/list/RoadmapListViewModelTest.kt` | modify | import persistence test |
| `ui/list/RoadmapListScreen.kt` | modify | `RoadmapListRoute` host: dialog state, wire `onImport` |

## Acceptance criteria (F8)

- Pasting valid JSON for a 3-milestone roadmap creates it **fully** (roadmap + milestones + steps + links, positions by index), appearing in **Active**.
- Pasting **malformed JSON** or a **non-http link** surfaces a clear error and creates **nothing**.
- The Prompt panel is copyable and includes the user's goal.
- No network calls anywhere.
