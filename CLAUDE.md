# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Roadmap is an offline-first, single-user Android goal-tracker (Kotlin · Jetpack Compose · Material 3 · Room). A roadmap holds ordered milestones, each holds ordered steps, each step carries reference links. `PRODUCT_SPEC.md` is the source of truth — features are referenced by id (F1–F9) and section (§6 data model, §7 business logic, §8 import schema, §9 visual design).

## Build & test

`./gradlew` needs a JDK. There is no system JDK on this machine; `JAVA_HOME` must point at the Android Studio JBR (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) — it is exported in `~/.zshenv`, so a fresh shell already has it. If you hit "Unable to locate a Java Runtime", re-export it. (AGP 9, `minSdk 26`, `targetSdk 36`; `compileSdk` uses AGP 9's `compileSdk { version = release(36) }` DSL.)

- Compile: `./gradlew :app:compileDebugKotlin`
- Unit tests (JVM, no emulator): `./gradlew :app:testDebugUnitTest`
- Single test class: `./gradlew :app:testDebugUnitTest --tests "*RoomRoadmapRepositoryTest"`
- Build APK: `./gradlew :app:assembleDebug`
- Install on a running emulator/device: `./gradlew :app:installDebug`

`gradle.properties` sets `android.disallowKotlinSourceSets=false` — **required** for KSP (Room's annotation processor) to coexist with AGP 9's built-in Kotlin. Do not remove it.

### Test strategy (important)

Pure logic and anything touching Room are both tested **on the JVM** — there is no emulator dependency for `testDebugUnitTest`:

- `domain/` functions and other pure code → plain JUnit.
- Anything touching Room (DAOs, the repository, ViewModels) → **Robolectric**: annotate the class `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])` and build the DB with `Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java)`. The Room JVM "bundled SQLite driver" does **not** work in this Android-only module — use Robolectric. ViewModel tests use `Dispatchers.setMain(StandardTestDispatcher())` + `runTest(dispatcher)` + `advanceUntilIdle()`.

### On-device / UI verification

Compose UI has no unit tests here (no CI emulator) — verify it via `@Preview` + compile, and by installing on the running emulator (`./gradlew :app:installDebug`, then drive with the mobile MCP if loaded, else `adb` directly). The Resizable AVD has **two displays**, so pin `screencap` to the app's display: `adb shell screencap -p -d <displayId> /sdcard/s.png && adb pull /sdcard/s.png`. The soft keyboard shifts AlertDialog buttons upward (tap coords differ with the keyboard open), and stylus handwriting can hijack `input text` (disable with `settings put secure stylus_handwriting_enabled 0`).

## Architecture

Three layers under `com.example.roadmap`, with dependencies flowing **ui → domain → data**. `domain/` is pure Kotlin (only `java.time` + data types) — keep Android/Room imports out of it.

### data/ — Room persistence
- The pathforge web app stored a roadmap as one embedded document; here the tree is **relational** (`RoadmapEntity → MilestoneEntity → StepEntity → LinkEntity`) with `ForeignKey(onDelete = CASCADE)` and an explicit 0-based `position` column at each level (SQLite has no arrays). Room does not order `@Relation` lists, so the read model `data/relation/RoadmapWithChildren.kt` exposes a `sorted()` extension that orders every level by `position`; the repository maps reads through it.
- **`RoadmapRepository` (interface) + `RoomRoadmapRepository` (impl) is the only thing the UI/ViewModels touch.** It injects `now: () -> Long` (so tests get deterministic timestamps), wraps every multi-write operation in `db.withTransaction`, and — crucially — re-derives `milestone.completedAt` after any step add/toggle/delete/import via `domain.milestoneCompletedAt`. ViewModels never compute completion; they just call repository methods.
- `RoadmapWithChildren` (full sorted tree, reactive `Flow`) feeds the detail screen; `RoadmapCard` is a lightweight projection (milestone/step counts computed with SQL subqueries, plus search baked into the query) for list cards.
- `RoadmapGraph` is the manual DI seam — a process-wide DB + repository factory (no Hilt). `MainActivity` gets the repository from it.

### domain/ — pure, JVM-tested logic
`Completion` (milestone auto-completes when ≥1 step and all complete), `Reorder` (validate a reorder is a permutation + assign contiguous positions), `Progress` (fraction + tree-aggregation extensions), `Overdue` (`java.time` local-date comparison), `ImportValidation` (validate a pasted-JSON payload against the §8 bulk schema). The repository and screens compose these.

### ui/ — Compose
- `ui/theme/` is a **bespoke design system extending Material 3** (not a re-skin). `RoadmapTheme` disables dynamic color (brand is fixed `#022e1c`) and provides both a standard M3 `ColorScheme` and an `@Immutable RoadmapColors` (bespoke tokens like `ringTrack`/`done`/`overdue`/`muted`) via `LocalRoadmapColors`, accessed as `RoadmapTheme.colors`; `primaryBrush` is an **extension property** (import it). Typography is the bundled Inter variable font. The 6-hue system (`RoadmapHue` + `hueForId`) colors icon tiles / milestone spines / empty-state art only — **progress rings stay uniform brand-colored** by deliberate design decision.
- `ui/components/` are reused across screens — build a screen by composing them; don't reinvent rings/chips/buttons.
- Screens follow `*ViewModel` (exposes `StateFlow<*UiState>` derived from the repository via `flatMapLatest`/`stateIn`) + a stateless `*Screen` + a stateful `*Route` host. Navigation is a **manual List↔Detail state machine** in `ui/RoadmapApp.kt` (`BackHandler` + `rememberSaveable`), chosen because navigation-compose only ships an alpha at the current versions.

## Workflow

Built phase-by-phase against `PRODUCT_SPEC.md`; each phase has a detailed TDD plan in `docs/superpowers/plans/` (a master plan lists the phases). Each phase lands as its own **GitHub PR against `main`** (not a local merge) — push the branch and `gh pr create`, leaving it for review. Commits are modular and conventional (`feat:`/`data:`/`ui:`/`domain:`/`test:`/`docs:`).
