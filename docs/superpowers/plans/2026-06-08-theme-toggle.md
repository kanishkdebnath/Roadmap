# In-App Theme Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a persisted **Light / Dark / System** theme toggle in the Roadmaps app bar so the existing dark theme is reachable in-app (today it only follows the OS setting).

**Architecture:** A `ThemeMode` enum + a `ThemeStore` (SharedPreferences-backed `StateFlow`) in `data`, provided process-wide by `RoadmapGraph`. `MainActivity` collects the mode, resolves it to a boolean (`System` → `isSystemInDarkTheme()`), and passes it to the already-theme-aware `RoadmapTheme`. The current mode + a setter thread down to the list app bar, which shows an icon + `DropdownMenu`.

**Tech Stack:** Kotlin · Jetpack Compose · `SharedPreferences` (no new dependency) · the existing `RoadmapTheme(darkTheme: Boolean)`.

**Context:** Dark visuals already exist and match the mockup (Phase 1 dark `RoadmapColors` + `DarkColorScheme`, and `RoadmapTheme` already sets the per-theme status bar). This plan only adds **control + persistence**, plus one design-match fix (the app-bar Import icon is a stale `Icons.Outlined.Map`; the mockup uses a download glyph).

---

## File Structure

| File | Change | Responsibility |
|------|--------|----------------|
| `data/ThemeMode.kt` | create | `enum ThemeMode { System, Light, Dark }` |
| `data/ThemeStore.kt` | create | persist + expose `StateFlow<ThemeMode>` |
| `test/.../data/ThemeStoreTest.kt` | create | Robolectric persistence test |
| `data/RoadmapGraph.kt` | modify | provide a process-wide `ThemeStore` |
| `MainActivity.kt` | modify | collect mode → resolve dark → `RoadmapTheme`; pass mode + setter down |
| `ui/RoadmapApp.kt` | modify | thread `themeMode` + `onSetThemeMode` to the list route |
| `ui/list/RoadmapListScreen.kt` | modify | `RoadmapListRoute` + `RoadmapListScreen`: app-bar theme toggle; fix Import icon |

---

## Task 1: ThemeMode + ThemeStore (TDD)

**Files:**
- Create: `app/src/main/java/com/example/roadmap/data/ThemeMode.kt`
- Create: `app/src/main/java/com/example/roadmap/data/ThemeStore.kt`
- Create: `app/src/test/java/com/example/roadmap/data/ThemeStoreTest.kt`

`ThemeStore` touches Android `SharedPreferences`, so its test uses **Robolectric** (like the other data tests).

- [ ] **Step 1: Create the enum**

Create `app/src/main/java/com/example/roadmap/data/ThemeMode.kt`:
```kotlin
package com.example.roadmap.data

/** User's theme preference. System follows the OS dark-mode setting. */
enum class ThemeMode { System, Light, Dark }
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/example/roadmap/data/ThemeStoreTest.kt`:
```kotlin
package com.example.roadmap.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeStoreTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun defaults_to_system() {
        assertEquals(ThemeMode.System, ThemeStore(ctx).mode.value)
    }

    @Test fun set_mode_updates_flow_and_persists_across_instances() {
        val store = ThemeStore(ctx)
        store.setMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, store.mode.value)
        // a fresh instance reads the persisted value
        assertEquals(ThemeMode.Dark, ThemeStore(ctx).mode.value)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ThemeStoreTest"`
Expected: FAIL — `unresolved reference: ThemeStore`. (If `JAVA_HOME` is unset: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.)

- [ ] **Step 4: Implement `ThemeStore`**

Create `app/src/main/java/com/example/roadmap/data/ThemeStore.kt`:
```kotlin
package com.example.roadmap.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists the user's [ThemeMode] in SharedPreferences and exposes it reactively. */
class ThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
        _mode.value = mode
    }

    private fun read(): ThemeMode =
        prefs.getString(KEY, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System

    private companion object { const val KEY = "theme_mode" }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ThemeStoreTest"`
Expected: PASS — both tests green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/ThemeMode.kt \
        app/src/main/java/com/example/roadmap/data/ThemeStore.kt \
        app/src/test/java/com/example/roadmap/data/ThemeStoreTest.kt
git commit -m "feat: ThemeMode + persisted ThemeStore"
```

---

## Task 2: Wire the store through the app

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/data/RoadmapGraph.kt`
- Modify: `app/src/main/java/com/example/roadmap/MainActivity.kt`
- Modify: `app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt`
- Modify: `app/src/main/java/com/example/roadmap/ui/list/RoadmapListScreen.kt` (`RoadmapListRoute` only)

- [ ] **Step 1: Provide a process-wide `ThemeStore` from `RoadmapGraph`**

In `RoadmapGraph.kt`, add a cached provider (mirroring the `database` pattern). Add inside the `object RoadmapGraph`:
```kotlin
    @Volatile private var themeStore: ThemeStore? = null

    fun themeStore(context: Context): ThemeStore =
        themeStore ?: synchronized(this) {
            themeStore ?: ThemeStore(context.applicationContext).also { themeStore = it }
        }
```

- [ ] **Step 2: Resolve + apply the mode in `MainActivity`**

Replace `MainActivity.kt`'s body with (adds the store, collects it, resolves dark, threads it down):
```kotlin
package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.roadmap.data.RoadmapGraph
import com.example.roadmap.data.ThemeMode
import com.example.roadmap.ui.RoadmapApp
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = RoadmapGraph.repository(applicationContext)
        val themeStore = RoadmapGraph.themeStore(applicationContext)
        setContent {
            val mode by themeStore.mode.collectAsState()
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            RoadmapTheme(darkTheme = dark) {
                RoadmapApp(
                    repository = repository,
                    themeMode = mode,
                    onSetThemeMode = themeStore::setMode,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
```

- [ ] **Step 3: Thread params through `RoadmapApp`**

In `RoadmapApp.kt`, add the import:
```kotlin
import com.example.roadmap.data.ThemeMode
```
Change the signature and the list-route call:
```kotlin
@Composable
fun RoadmapApp(
    repository: RoadmapRepository,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = detailId != null) { detailId = null }

    val current = detailId
    if (current == null) {
        val listVm: RoadmapListViewModel = viewModel(factory = RoadmapListViewModelFactory(repository))
        RoadmapListRoute(
            listVm,
            onOpenRoadmap = { detailId = it },
            themeMode = themeMode,
            onSetThemeMode = onSetThemeMode,
            modifier = modifier,
        )
    } else {
        RoadmapDetailRoute(repository, current, onBack = { detailId = null }, modifier = modifier)
    }
}
```

- [ ] **Step 4: Pass params through `RoadmapListRoute`**

In `RoadmapListScreen.kt`, add the import:
```kotlin
import com.example.roadmap.data.ThemeMode
```
Update `RoadmapListRoute`'s signature + its `RoadmapListScreen(...)` call:
```kotlin
@Composable
fun RoadmapListRoute(
    viewModel: RoadmapListViewModel,
    onOpenRoadmap: (Long) -> Unit,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    RoadmapListScreen(
        state = state,
        onScopeChange = viewModel::setArchived,
        onQueryChange = viewModel::setQuery,
        onOpenRoadmap = onOpenRoadmap,
        onCreate = { showNew = true },
        onImport = { showImport = true },
        themeMode = themeMode,
        onSetThemeMode = onSetThemeMode,
        modifier = modifier,
    )
```
(Leave the rest of `RoadmapListRoute` — the `if (showNew)` / `if (showImport)` blocks — unchanged.)

- [ ] **Step 5: Compile (expect ONE error in `RoadmapListScreen` — fixed in Task 3)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: FAIL — `RoadmapListScreen` has no `themeMode`/`onSetThemeMode` params yet (added in Task 3). This confirms the wiring is in place. Do NOT commit yet — Task 3 makes it compile.

---

## Task 3: App-bar theme toggle (RoadmapListScreen)

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/ui/list/RoadmapListScreen.kt` (`RoadmapListScreen` + previews)

- [ ] **Step 1: Add imports**

In `RoadmapListScreen.kt`, add (with the existing imports):
```kotlin
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
```
(`ThemeMode` was imported in Task 2.)

- [ ] **Step 2: Add the two params to `RoadmapListScreen` (with defaults so previews keep compiling)**

Change the `RoadmapListScreen` signature — add `themeMode` + `onSetThemeMode` after `onImport`, before `modifier`:
```kotlin
@Composable
fun RoadmapListScreen(
    state: RoadmapListUiState,
    onScopeChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenRoadmap: (Long) -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    themeMode: ThemeMode = ThemeMode.System,
    onSetThemeMode: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
```

- [ ] **Step 3: Add the theme toggle + fix the Import icon in the header `Row`**

Replace the `IconButton(onClick = onImport) { Icon(Icons.Outlined.Map, … ) }` block (the stale Map placeholder) with a theme toggle followed by a proper download Import icon:
```kotlin
                ThemeMenu(themeMode, onSetThemeMode)
                IconButton(onClick = onImport) {
                    Icon(Icons.Rounded.Download, contentDescription = "Import")
                }
```
(The `Icons.Outlined.Map` import is now unused — remove the line `import androidx.compose.material.icons.outlined.Map` to keep it clean.)

- [ ] **Step 4: Add the `ThemeMenu` composable**

Add this private composable to `RoadmapListScreen.kt` (e.g. just above `NewRoadmapDialog`):
```kotlin
@Composable
private fun ThemeMenu(themeMode: ThemeMode, onSetThemeMode: (ThemeMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                when (themeMode) {
                    ThemeMode.Light -> Icons.Rounded.LightMode
                    ThemeMode.Dark -> Icons.Rounded.DarkMode
                    ThemeMode.System -> Icons.Rounded.BrightnessAuto
                },
                contentDescription = "Theme",
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ThemeMode.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.name) },
                    onClick = { onSetThemeMode(m); open = false },
                    leadingIcon = {
                        if (m == themeMode) Icon(Icons.Rounded.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 5: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (The previews `ListPreviewLight`/`ListPreviewDark` still pass 6 positional args; the new params use their defaults, so they keep compiling.) If `Icons.Rounded.Download` / `BrightnessAuto` are unresolved in the bundled set, use `Icons.Rounded.SaveAlt` / `Icons.Rounded.Brightness4`.

- [ ] **Step 6: Run the full unit-test suite (no regressions)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — all suites green (incl. the new `ThemeStoreTest`).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/RoadmapGraph.kt \
        app/src/main/java/com/example/roadmap/MainActivity.kt \
        app/src/main/java/com/example/roadmap/ui/RoadmapApp.kt \
        app/src/main/java/com/example/roadmap/ui/list/RoadmapListScreen.kt
git commit -m "feat: in-app Light/Dark/System theme toggle in the app bar"
```

---

## Task 4: On-device verification & finish

**Files:** none (verification only).

- [ ] **Step 1: Build + install**

Run: `./gradlew :app:installDebug`
Expected: BUILD SUCCESSFUL; installed on the running emulator.

- [ ] **Step 2: Verify the toggle**

Launch the app. Tap the **theme icon** (left of the Import/download icon in the app bar) → the menu shows **System / Light / Dark** with a check on the current. Pick **Dark** → the whole UI switches to the dark theme immediately (deep-green canvas, mint→cyan accents); the menu icon becomes the moon. Pick **Light** → switches back. (Drive via the mobile MCP using device-pixel coords from `mobile_list_elements_on_screen`; per the on-device notes, get coords from the element list, not the screenshot.)

- [ ] **Step 3: Verify persistence across restart**

With the mode set to **Dark**, fully restart the app (`adb shell am force-stop com.example.roadmap`, then relaunch). Confirm it reopens in **Dark** (the choice persisted). Set it back to **System** and confirm it follows the emulator's current OS mode.

- [ ] **Step 4: Definition of done**

Confirm: `./gradlew :app:assembleDebug` succeeds, `./gradlew :app:testDebugUnitTest` is green, the in-app toggle switches the theme live and persists across restart, and `System` follows the OS.

- [ ] **Step 5: Finish the branch**

Use **superpowers:finishing-a-development-branch** → push `feat/theme-toggle` and open a PR against `main`.

---

## Definition of done

- Compiles (`./gradlew :app:assembleDebug`), unit tests green (incl. `ThemeStoreTest`).
- App-bar toggle switches Light/Dark/System live; choice persists across restart; `System` follows the OS.
- Dark visuals unchanged (already match the mockup); the app-bar Import icon now uses the download glyph (matches the mockup).
- Lands as its own PR against `main`.

## Self-review notes

- **Coverage:** in-app control → `ThemeMenu` (Task 3); persistence → `ThemeStore` (Task 1, tested); applied app-wide → `MainActivity` resolves + `RoadmapTheme` (Task 2); reachable from the list app bar (Task 3); design-match → Import icon fixed to `Download`.
- **Type consistency:** `ThemeMode { System, Light, Dark }`; `ThemeStore.mode: StateFlow<ThemeMode>` + `setMode(ThemeMode)`; `RoadmapGraph.themeStore(Context)`; `RoadmapApp(repository, themeMode, onSetThemeMode, modifier)`; `RoadmapListRoute(viewModel, onOpenRoadmap, themeMode, onSetThemeMode, modifier)`; `RoadmapListScreen(..., themeMode, onSetThemeMode, modifier)` — consistent across tasks.
- **Preview safety:** the two new `RoadmapListScreen` params have defaults, so the existing positional-arg previews keep compiling.
- **YAGNI:** SharedPreferences (no new dependency) for one value; no settings screen — just the app-bar control the request implies.
