# Roadmap

An offline-first, single-user **Android goal tracker**. Plan a goal as a **roadmap** of ordered **milestones**, each holding ordered **steps** with reference links — and keep a daily **journal** alongside it. Everything lives on-device: no account, no network, no analytics.

Built with Kotlin, Jetpack Compose, Material 3, and Room.

---

## Features

**Roadmaps**
- Create / edit / delete roadmaps; **archive**; **search** across roadmaps and milestones.
- Milestones and steps with inline add/edit and **drag-to-reorder** at both levels.
- Step **reference links** (http/https) that open in the browser.
- **Progress rings** (steps → milestone → roadmap); a milestone auto-completes when it has ≥1 step and all are done.
- **Deadlines** with local-timezone overdue detection (state shown with text + icon, never color alone).
- **Import from an LLM** — copy a prompt, paste back the JSON a chat tool returns; lenient parsing (tolerates code fences/prose), validated against a bounded schema, inserted atomically.
- **Export** a roadmap to JSON via the Android share sheet — round-trips cleanly with import.

**Journal** (a second top-level destination)
- Month **heatmap** with cells tinted by daily mood; opens to the current month, today ringed; tap any day to edit.
- **Day editor** — mood (1–5 emoji + up to 3 tags), a one-line summary, timestamped events (with an "important" flag), links, and **references to your own roadmaps/milestones** via a searchable picker. One entry per day, saved in a single atomic write; deleted reference targets degrade to a struck-through "(deleted)" chip.

**App-wide**
- **Light / Dark / System** theme toggle (persisted).
- Bottom-navigation shell that becomes a navigation rail on wide screens, hidden while you're in a detail.
- Subtle **animations** (list-load stagger, step check/strike, progress-ring sweep, screen crossfade) that **honor the system "Remove animations" setting**, plus an **accessibility** pass — 48 dp touch targets, TalkBack roles/labels, merged card semantics.

---

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 — a *bespoke* design system on top (fixed brand color `#022E1C`, bundled Inter variable font, custom color/shape tokens; Material dynamic color intentionally disabled). Full dark mode.
- **Persistence:** Room (KSP) — relational schema with `ON DELETE CASCADE` and explicit `position` ordering; reactive `Flow` reads; additive migrations.
- **Serialization:** kotlinx.serialization (import/export + journal mood tags).
- **Concurrency:** Kotlin coroutines / `Flow`.
- **DI:** a small manual provider (`RoadmapGraph`) — no Hilt.
- **minSdk 26 · targetSdk 36 · AGP 9.**

---

## Architecture

Three layers, with dependencies flowing **`ui → domain → data`**:

- **`data/`** — Room entities, DAOs, and `@Relation` read models (Room doesn't order relation lists, so a `sorted()` extension orders every level by `position`). Repositories (`RoomRoadmapRepository`, `RoomJournalRepository`) are the only surface the UI touches: they wrap multi-write operations in a single `@Transaction`, inject a `now: () -> Long` clock for deterministic tests, and re-derive milestone completion after any step change. `RoadmapGraph` is the process-wide database + repository provider.
- **`domain/`** — pure, JVM-tested logic with no Android dependencies: completion, progress aggregation, reorder validation, overdue (`java.time`), import validation, the journal dirty-check, and the calendar month-grid.
- **`ui/`** — Compose screens following a `*ViewModel` (exposes `StateFlow`) + stateless `*Screen` + stateful `*Route` pattern. Navigation is a small manual state machine (`RoadmapApp`): a `NavigationBar`/`NavigationRail` switching two top-level destinations, each with its own list ↔ detail drill-down.

The data model is fully **relational** (`Roadmap → Milestone → Step → Link`, and `JournalDay → Event / Link / Reference`). Journal references point at roadmaps/milestones by plain id (not foreign keys), so deleting a target leaves the row and the chip resolves to "(deleted)" at render.

---

## Build & test

The Gradle wrapper needs a JDK. There's no system JDK on the dev machine — point `JAVA_HOME` at the Android Studio JBR (already exported in `~/.zshenv`):

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew :app:compileDebugKotlin    # compile
./gradlew :app:testDebugUnitTest     # unit tests (JVM — no emulator)
./gradlew :app:assembleDebug         # build the debug APK
./gradlew :app:installDebug          # install on a running emulator/device
```

**Testing strategy** — everything runs on the JVM:

- Pure `domain/` logic → plain **JUnit**.
- Anything touching Room (DAOs, repositories, ViewModels) → **Robolectric** with an in-memory database (`@Config(sdk = [34])`), so no emulator is needed for `testDebugUnitTest`.
- Compose UI has no unit tests; it's verified with `@Preview` and by installing on a device. (~110 unit tests total.)

> `gradle.properties` sets `android.disallowKotlinSourceSets=false` — required for KSP (Room) to coexist with AGP 9's built-in Kotlin. Don't remove it.

---

## Project layout

```
app/src/main/java/com/example/roadmap/
├─ data/            Room entities, DAOs, repositories, import/export
│  └─ journal/      journal entities, DAOs, repository, drafts
├─ domain/          pure logic (completion, progress, overdue, validation, dirty-check, calendar)
└─ ui/
   ├─ theme/        bespoke Material 3 design system (color tokens, motion, type)
   ├─ components/   reusable rings, chips, buttons, checkboxes, drag grips
   ├─ list/ detail/ roadmap screens (list, detail, dialogs)
   └─ journal/      month heatmap + day editor + reference picker
docs/superpowers/   design specs & implementation plans (one per phase)
PRODUCT_SPEC.md     feature spec (F1–F9, §6 data model, §8 schema, §10 NFRs)
JOURNAL_SPEC.md     journal sub-feature spec
```

---

## How it was built

Developed phase-by-phase, each phase landing as its own GitHub PR against `main` via a consistent loop: **brainstorm → design spec → bite-sized implementation plan → test-driven execution → on-device verification**. The specs and plans for every phase live in `docs/superpowers/`.

Phases: design system · data layer · domain logic · roadmaps list · roadmap detail · drag-to-reorder · app icon · import-from-LLM (F8) · in-app theme toggle · the **Journal** sub-feature (data layer → bottom-nav shell → month heatmap → day editor) · export (F9) · polish (animations + accessibility).

---

## Status

The product spec is fully implemented (F1–F9 + the Journal sub-feature + polish). Intentionally deferred for a future version: journal back-reference views, search, mood charts/streaks, reminders, "export all roadmaps," and journal JSON export.
