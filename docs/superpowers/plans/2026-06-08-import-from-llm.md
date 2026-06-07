# Import from LLM (F8) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the paste-JSON "Import from LLM" flow (spec F8): an Import dialog with a copyable, goal-injected prompt and a Paste panel that validates live and atomically creates the whole roadmap tree.

**Architecture:** kotlinx.serialization parses pasted text (leniently, stripping ```` ```json ```` fences/prose) into the existing `RoadmapDraft` types, which already mirror §8. A pure `previewImport` runs parse → the existing `domain.validateImport` for a live note. On Import, the existing transactional `repository.importRoadmap(draft)` does the atomic insert. The app makes **no network calls**.

**Tech Stack:** Kotlin · Jetpack Compose · **kotlinx.serialization (json)** · Room (existing) · the existing `validateImport` / `importRoadmap` / draft types.

**Design spec:** `docs/superpowers/specs/2026-06-08-import-from-llm-design.md`.

---

## File Structure

| File | Change | Responsibility |
|------|--------|----------------|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | modify | kotlinx.serialization plugin + json runtime |
| `data/RoadmapRepository.kt` | modify | `@Serializable` on the 4 draft types |
| `data/ImportJson.kt` | create | `parseRoadmapJson`, `buildImportPrompt`, `ImportPreview`, `previewImport` |
| `test/.../data/ImportJsonTest.kt` | create | JVM unit tests (plain JUnit) |
| `ui/list/RoadmapListViewModel.kt` | modify | `importRoadmap(draft)` |
| `test/.../ui/list/RoadmapListViewModelTest.kt` | modify | import-persistence Robolectric test |
| `ui/components/Buttons.kt` | modify | add `enabled` param to `PrimaryButton` |
| `ui/imports/ImportDialog.kt` | create | the Import dialog composable (pkg `ui.imports` — `import` is a keyword) |
| `ui/list/RoadmapListScreen.kt` | modify | `RoadmapListRoute`: host `showImport` + render `ImportDialog` |

---

## Task 1: Add kotlinx.serialization

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Version catalog — version + plugin + library**

In `gradle/libs.versions.toml`, add to `[versions]` (after `reorderable = "3.1.0"`):
```toml
kotlinxSerialization = "1.8.1"
```
Add to `[libraries]` (after the `reorderable` line):
```toml
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
```
Add to `[plugins]` (after the `ksp` line):
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Apply plugin + add dependency**

In `app/build.gradle.kts`, add to the `plugins { }` block (after `alias(libs.plugins.ksp)`):
```kotlin
    alias(libs.plugins.kotlin.serialization)
```
Add to `dependencies { }` (after `implementation(libs.reorderable)`):
```kotlin
    implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 3: Verify it resolves and the project still compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (If `JAVA_HOME` is unset: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.) If `kotlinx-serialization-json:1.8.1` fails to resolve or compile against Kotlin 2.2.10, fall back to `1.8.0` then `1.7.3` and note which you used.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add kotlinx.serialization for JSON import"
```

---

## Task 2: JSON codec — parse, prompt, preview (TDD)

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/data/RoadmapRepository.kt` (annotate drafts)
- Create: `app/src/main/java/com/example/roadmap/data/ImportJson.kt`
- Create: `app/src/test/java/com/example/roadmap/data/ImportJsonTest.kt`

`kotlinx.serialization` is a JVM library, so this is plain-JUnit JVM-tested (no Robolectric).

- [ ] **Step 1: Annotate the draft types `@Serializable`**

In `data/RoadmapRepository.kt`, add the import near the top (with the other imports):
```kotlin
import kotlinx.serialization.Serializable
```
Then prefix each of the four draft data classes with `@Serializable` (they are currently consecutive lines ~9–12):
```kotlin
@Serializable
data class LinkDraft(val url: String, val label: String? = null)
@Serializable
data class StepDraft(val title: String, val completed: Boolean = false, val links: List<LinkDraft> = emptyList())
@Serializable
data class MilestoneDraft(val title: String, val description: String? = null, val deadline: String? = null, val steps: List<StepDraft> = emptyList())
@Serializable
data class RoadmapDraft(val title: String, val description: String? = null, val deadline: String? = null, val milestones: List<MilestoneDraft> = emptyList())
```

- [ ] **Step 2: Write the failing tests**

Create `app/src/test/java/com/example/roadmap/data/ImportJsonTest.kt`:
```kotlin
package com.example.roadmap.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportJsonTest {
    private val sample = """
        {"title":"Learn Rust","milestones":[
          {"title":"Ownership","steps":[
            {"title":"Read Ch.4","links":[{"url":"https://doc.rust-lang.org"}]}]}]}
    """.trimIndent()

    @Test fun parses_clean_object() {
        val d = parseRoadmapJson(sample).getOrThrow()
        assertEquals("Learn Rust", d.title)
        assertEquals(1, d.milestones.size)
        assertEquals("Read Ch.4", d.milestones[0].steps[0].title)
        assertEquals("https://doc.rust-lang.org", d.milestones[0].steps[0].links[0].url)
    }

    @Test fun strips_markdown_fence() {
        val fenced = "```json\n$sample\n```"
        assertEquals("Learn Rust", parseRoadmapJson(fenced).getOrThrow().title)
    }

    @Test fun strips_surrounding_prose() {
        val prose = "Sure! Here is your roadmap:\n$sample\nHope it helps."
        assertEquals("Learn Rust", parseRoadmapJson(prose).getOrThrow().title)
    }

    @Test fun ignores_unknown_keys() {
        val extra = """{"title":"X","extra":42,"milestones":[]}"""
        assertEquals("X", parseRoadmapJson(extra).getOrThrow().title)
    }

    @Test fun applies_completed_default() {
        val d = parseRoadmapJson("""{"title":"X","milestones":[{"title":"M","steps":[{"title":"S"}]}]}""").getOrThrow()
        assertFalse(d.milestones[0].steps[0].completed)
    }

    @Test fun malformed_json_fails() {
        assertTrue(parseRoadmapJson("{ not json ").isFailure)
        assertTrue(parseRoadmapJson("no braces here").isFailure)
        assertTrue(parseRoadmapJson("   ").isFailure)
    }

    @Test fun prompt_contains_goal_and_schema() {
        val p = buildImportPrompt("Learn Rust")
        assertTrue(p.contains("Learn Rust"))
        assertTrue(p.contains("\"milestones\""))
        assertTrue(p.contains("http://"))
    }

    @Test fun prompt_blank_goal_uses_placeholder() {
        assertTrue(buildImportPrompt("   ").contains("<describe your goal>"))
    }

    @Test fun preview_empty_when_blank() {
        assertEquals(ImportPreview.Empty, previewImport("   "))
    }

    @Test fun preview_invalid_on_bad_json() {
        assertTrue(previewImport("not json at all") is ImportPreview.Invalid)
    }

    @Test fun preview_invalid_on_non_http_link() {
        val bad = """{"title":"X","milestones":[{"title":"M","steps":[{"title":"S","links":[{"url":"ftp://x"}]}]}]}"""
        assertTrue(previewImport(bad) is ImportPreview.Invalid)
    }

    @Test fun preview_valid_with_counts() {
        val json = """
            {"title":"G","milestones":[
              {"title":"M1","steps":[{"title":"a"},{"title":"b"}]},
              {"title":"M2","steps":[{"title":"c"}]},
              {"title":"M3","steps":[]}]}
        """.trimIndent()
        val p = previewImport(json) as ImportPreview.Valid
        assertEquals(3, p.milestones)
        assertEquals(3, p.steps)
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*ImportJsonTest"`
Expected: FAIL — `unresolved reference: parseRoadmapJson` / `buildImportPrompt` / `previewImport` / `ImportPreview`.

- [ ] **Step 4: Implement `data/ImportJson.kt`**

Create `app/src/main/java/com/example/roadmap/data/ImportJson.kt`:
```kotlin
package com.example.roadmap.data

import com.example.roadmap.domain.ImportValidation
import com.example.roadmap.domain.validateImport
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val importJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** Parse pasted text into a [RoadmapDraft], tolerating ```fences and surrounding prose. */
fun parseRoadmapJson(raw: String): Result<RoadmapDraft> {
    val extracted = extractJsonObject(raw)
        ?: return Result.failure(IllegalArgumentException("No JSON object found"))
    return runCatching { importJson.decodeFromString<RoadmapDraft>(extracted) }
}

/** Strip a fenced code block if present, then take the first '{' .. last '}'. */
private fun extractJsonObject(raw: String): String? {
    var s = raw.trim()
    Regex("```(?:json|JSON)?\\s*([\\s\\S]*?)```").find(s)?.let { s = it.groupValues[1].trim() }
    val start = s.indexOf('{')
    val end = s.lastIndexOf('}')
    if (start == -1 || end == -1 || end < start) return null
    return s.substring(start, end + 1)
}

/** The copyable LLM prompt with the user's goal injected (spec §8). */
fun buildImportPrompt(goal: String): String {
    val g = goal.ifBlank { "<describe your goal>" }
    return """
        You are an expert planner. Create a structured roadmap as JSON for this goal:

        "$g"

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
    """.trimIndent()
}

/** Live preview for the dialog note: parse → validate. */
sealed interface ImportPreview {
    data object Empty : ImportPreview
    data class Invalid(val reason: String) : ImportPreview
    data class Valid(val draft: RoadmapDraft, val milestones: Int, val steps: Int) : ImportPreview
}

fun previewImport(raw: String): ImportPreview {
    if (raw.isBlank()) return ImportPreview.Empty
    val draft = parseRoadmapJson(raw).getOrElse {
        return ImportPreview.Invalid("Couldn't read the JSON — paste the whole object the LLM returned.")
    }
    return when (val v = validateImport(draft)) {
        is ImportValidation.Valid -> ImportPreview.Valid(
            draft, draft.milestones.size, draft.milestones.sumOf { it.steps.size },
        )
        is ImportValidation.Invalid -> ImportPreview.Invalid(v.reason)
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*ImportJsonTest"`
Expected: PASS — all 12 tests green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/RoadmapRepository.kt \
        app/src/main/java/com/example/roadmap/data/ImportJson.kt \
        app/src/test/java/com/example/roadmap/data/ImportJsonTest.kt
git commit -m "feat: JSON import codec (lenient parse, prompt, live preview)"
```

---

## Task 3: ViewModel import action (TDD)

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/ui/list/RoadmapListViewModel.kt`
- Modify: `app/src/test/java/com/example/roadmap/ui/list/RoadmapListViewModelTest.kt`

`repository.importRoadmap(draft)` already exists on the `RoadmapRepository` interface and is transactional.

- [ ] **Step 1: Write the failing test**

In `RoadmapListViewModelTest.kt`, add these imports (with the existing ones):
```kotlin
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.MilestoneDraft
import com.example.roadmap.data.RoadmapDraft
import com.example.roadmap.data.StepDraft
```
Add this test method inside the class:
```kotlin
    @Test fun import_persists_full_tree_into_active() = runTest(dispatcher) {
        val draft = RoadmapDraft(
            title = "Imported Goal",
            milestones = listOf(
                MilestoneDraft("M1", steps = listOf(StepDraft("s1"), StepDraft("s2", completed = true))),
                MilestoneDraft("M2", steps = listOf(StepDraft("s3", links = listOf(LinkDraft("https://x.com", "X"))))),
            ),
        )
        vm.importRoadmap(draft)
        advanceUntilIdle()
        val card = vm.uiState.first { it.cards.size == 1 }.cards.single()
        assertEquals("Imported Goal", card.roadmap.title)
        assertEquals(2, card.milestoneCount)
        assertEquals(3, card.totalSteps)
        assertEquals(1, card.completedSteps)
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*RoadmapListViewModelTest"`
Expected: FAIL — `unresolved reference: importRoadmap`.

- [ ] **Step 3: Add the ViewModel method**

In `RoadmapListViewModel.kt`, add the import (with the others):
```kotlin
import com.example.roadmap.data.RoadmapDraft
```
Add the method inside `RoadmapListViewModel`, after `createRoadmap`:
```kotlin
    fun importRoadmap(draft: RoadmapDraft) {
        viewModelScope.launch { repository.importRoadmap(draft) }
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*RoadmapListViewModelTest"`
Expected: PASS — all tests in the class green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/list/RoadmapListViewModel.kt \
        app/src/test/java/com/example/roadmap/ui/list/RoadmapListViewModelTest.kt
git commit -m "feat: list ViewModel importRoadmap action"
```

---

## Task 4: PrimaryButton `enabled` + ImportDialog UI

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/ui/components/Buttons.kt`
- Create: `app/src/main/java/com/example/roadmap/ui/imports/ImportDialog.kt`

UI — verified by compile + `@Preview`, no unit test (no Compose UI tests in this module).

- [ ] **Step 1: Add an `enabled` param to `PrimaryButton`**

In `Buttons.kt`, add the import (with the others):
```kotlin
import androidx.compose.ui.draw.alpha
```
Replace the `PrimaryButton` function with:
```kotlin
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(RoadmapTheme.colors.primaryBrush)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(role = Role.Button, enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge)
    }
}
```

- [ ] **Step 2: Create the ImportDialog**

Create `app/src/main/java/com/example/roadmap/ui/imports/ImportDialog.kt`:
```kotlin
package com.example.roadmap.ui.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roadmap.data.ImportPreview
import com.example.roadmap.data.RoadmapDraft
import com.example.roadmap.data.buildImportPrompt
import com.example.roadmap.data.previewImport
import com.example.roadmap.ui.components.PrimaryButton
import com.example.roadmap.ui.components.SecondaryButton
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun ImportDialog(onDismiss: () -> Unit, onImport: (RoadmapDraft) -> Unit) {
    var goal by remember { mutableStateOf("") }
    var paste by remember { mutableStateOf("") }
    val prompt = remember(goal) { buildImportPrompt(goal) }
    val preview = remember(paste) { previewImport(paste) }
    val clipboard = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(RoadmapHue.Cyan.colors(RoadmapTheme.colors.isDark))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text("Import roadmap", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Generate JSON with any LLM, then paste it back. The app makes no network calls.",
                            style = MaterialTheme.typography.bodySmall, color = RoadmapTheme.colors.muted,
                        )
                    }
                }

                // Your goal
                OutlinedTextField(
                    goal, { goal = it },
                    label = { Text("Your goal") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Panel 1 — prompt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("1 · Prompt template", style = MaterialTheme.typography.labelMedium,
                        color = RoadmapTheme.colors.muted, modifier = Modifier.weight(1f))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(prompt)) }) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp)); Text("Copy")
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    SelectionContainer {
                        Text(
                            prompt,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = RoadmapTheme.colors.muted,
                            modifier = Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState()).padding(12.dp),
                        )
                    }
                }

                // Panel 2 — paste
                Text("2 · Paste JSON", style = MaterialTheme.typography.labelMedium, color = RoadmapTheme.colors.muted)
                OutlinedTextField(
                    paste, { paste = it },
                    placeholder = { Text("Paste the JSON here") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )

                // Live note
                when (val p = preview) {
                    is ImportPreview.Valid -> NoteRow(
                        Icons.Rounded.CheckCircle, RoadmapTheme.colors.done,
                        "Valid · 1 roadmap, ${p.milestones} milestone(s), ${p.steps} step(s) — imported atomically.",
                    )
                    is ImportPreview.Invalid -> NoteRow(Icons.Rounded.ErrorOutline, RoadmapTheme.colors.overdue, p.reason)
                    ImportPreview.Empty -> Unit
                }

                // Footer
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Cancel", onDismiss, Modifier.weight(1f))
                    PrimaryButton(
                        "Import",
                        onClick = { (preview as? ImportPreview.Valid)?.let { onImport(it.draft) } },
                        modifier = Modifier.weight(1f),
                        enabled = preview is ImportPreview.Valid,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteRow(icon: ImageVector, color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ImportDialogPreview() {
    RoadmapTheme { ImportDialog(onDismiss = {}, onImport = {}) }
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `Icons.Rounded.Download` is unresolved in the bundled icon set, use `Icons.Rounded.SaveAlt`. If `Icons.Rounded.ErrorOutline` is unresolved, use `Icons.Rounded.Error`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/Buttons.kt \
        app/src/main/java/com/example/roadmap/ui/imports/ImportDialog.kt
git commit -m "ui: Import roadmap dialog (prompt + paste panels)"
```

---

## Task 5: Wire the dialog into the list

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/ui/list/RoadmapListScreen.kt` (the `RoadmapListRoute` host)

- [ ] **Step 1: Add imports**

In `RoadmapListScreen.kt`, add (with the existing imports):
```kotlin
import com.example.roadmap.ui.imports.ImportDialog
```

- [ ] **Step 2: Host import state and render the dialog**

In `RoadmapListRoute`, add the state next to `showNew` (after `var showNew by remember { mutableStateOf(false) }`):
```kotlin
    var showImport by remember { mutableStateOf(false) }
```
Replace the `onImport` stub line:
```kotlin
        onImport = { /* TODO Phase 7: paste-JSON import */ },
```
with:
```kotlin
        onImport = { showImport = true },
```
And after the existing `if (showNew) { … }` block, add:
```kotlin
    if (showImport) {
        ImportDialog(
            onDismiss = { showImport = false },
            onImport = { draft -> viewModel.importRoadmap(draft); showImport = false },
        )
    }
```

- [ ] **Step 3: Compile + full unit-test suite (no regressions)**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all suites green (the new `ImportJsonTest` + `RoadmapListViewModelTest` import test + everything from Phases 2–6).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/list/RoadmapListScreen.kt
git commit -m "ui: open Import dialog from the roadmaps list"
```

---

## Task 6: On-device verification & finish

**Files:** none (verification only).

- [ ] **Step 1: Build + install**

Run: `./gradlew :app:installDebug`
Expected: BUILD SUCCESSFUL; installed on the running emulator.

- [ ] **Step 2: Verify a real LLM-style round-trip**

Launch the app, tap **Import** (app-bar icon or empty-state button). Type a goal, tap **Copy** (confirm a copy happened). Then paste this **fenced** LLM-style payload into the Paste field (mobile MCP `type_keys`, or `adb shell input text` with escaping — or push via clipboard `adb shell cmd clipboard set-primary-clip` if available):
```
```json
{"title":"Learn Rust","deadline":"2026-12-31","milestones":[
 {"title":"Ownership & Borrowing","steps":[{"title":"Read Ch.4","links":[{"url":"https://doc.rust-lang.org/book"}]},{"title":"Do exercises"}]},
 {"title":"Error handling","steps":[{"title":"Result & Option"}]},
 {"title":"Build a CLI","steps":[{"title":"clap basics"}]}]}
```
```
Confirm the live note shows **green "Valid · 1 roadmap, 3 milestone(s), 4 step(s)…"**, tap **Import**, and confirm the dialog closes and **"Learn Rust"** appears in the Active list with the right milestone/step counts; open it to verify milestones, steps, and the link chip.

- [ ] **Step 3: Verify error paths**

Re-open Import. Paste malformed JSON (`{ "title": `) → confirm a **red** note and **Import disabled**. Paste a payload whose link is `ftp://x` → confirm the red note names the non-http link and Import stays disabled. Confirm no roadmap was created.

- [ ] **Step 4: Definition of done**

Confirm: `./gradlew :app:assembleDebug` succeeds, `./gradlew :app:testDebugUnitTest` is green, the round-trip import works and persists, and error paths block import. Capture a screenshot of the Valid dialog for the PR.

- [ ] **Step 5: Finish the branch**

Use **superpowers:finishing-a-development-branch** → push `feat/import-llm` and open a PR against `main`.

---

## Definition of done (phase)

- Compiles (`./gradlew :app:assembleDebug`), unit tests green (`./gradlew :app:testDebugUnitTest`).
- Pasting valid JSON (incl. ```` ```json ```` fenced) for a 3-milestone roadmap creates it fully in **Active** (F8 acceptance); malformed JSON / non-http link → clear error, nothing created.
- The Prompt panel is copyable with the user's goal injected; no network calls.
- Lands as its own PR against `main`.

## Self-review notes

- **Spec coverage:** F8.1 two panels → Task 4 (dialog); copyable goal-injected prompt → `buildImportPrompt` (Task 2) + Copy button (Task 4). F8.2 validate + atomic insert → `previewImport`/`validateImport` (Task 2) + `importRoadmap` (Task 3, reuses transactional repo). F8.3 appears in Active → import isn't archived (existing repo) + verified Task 6. §8 lenient parse / caps → `parseRoadmapJson` (Task 2) + reused `validateImport`. Acceptance (3-milestone create; malformed/non-http error) → Tasks 2 tests + Task 6.
- **Type consistency:** `parseRoadmapJson: Result<RoadmapDraft>`, `previewImport: ImportPreview` (`Empty`/`Invalid(reason)`/`Valid(draft, milestones, steps)`), `buildImportPrompt(goal): String`, `RoadmapListViewModel.importRoadmap(draft: RoadmapDraft)`, `ImportDialog(onDismiss, onImport: (RoadmapDraft) -> Unit)`, `PrimaryButton(text, onClick, modifier, enabled)` — consistent across tasks.
- **Package name:** `ui.imports` (not `ui.import`) because `import` is a Kotlin hard keyword.
- **Out of scope:** Export (F9) — separate follow-up PR.
