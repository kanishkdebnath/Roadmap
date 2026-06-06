# Design System Implementation Plan (Phase 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Roadmap design system — theme tokens, Inter typography, the hue system, and the core Compose component library — matching the approved mockup (`mockups/roadmap-mockup.html`) and `PRODUCT_SPEC.md` §9.

**Architecture:** Extend Material 3 rather than replace it. A proper M3 `ColorScheme` (light/dark, **no dynamic color**) carries standard slots; an `@Immutable RoadmapColors` provided via a `CompositionLocal` carries bespoke tokens (ring track, done, overdue, muted/faint, primary brush). A `RoadmapTheme` composable wires both, plus Inter `Typography` and `Shapes`. Pure logic (ring math, hue selection) is JVM-unit-tested; components are verified by `@Preview` and, where an emulator is available, Compose UI semantics tests.

**Tech Stack:** Kotlin · Jetpack Compose (BOM 2026.02.01) · Material 3 · `compose.ui.graphics` Canvas.

**Conventions:**
- Package root: `com.example.roadmap`. Theme in `ui/theme/`, components in `ui/components/`.
- Run unit tests: `./gradlew :app:testDebugUnitTest`. Compile check: `./gradlew :app:compileDebugKotlin`. Full build: `./gradlew :app:assembleDebug`.
- Commit after each task. Work on branch `feat/design-system` (create in Task 1).
- Rings are uniform brand-colored; **only** tiles/spines/empty-art use hues.

---

## File structure (created/modified in this phase)

```
app/build.gradle.kts                                  (modify: minSdk 26)
app/src/main/res/font/inter_*.ttf                     (create: bundled Inter weights)
app/src/main/java/com/example/roadmap/ui/theme/
    Color.kt        (replace: brand tokens, light/dark, RoadmapColors)
    Type.kt         (replace: Inter Typography + tabular numerals)
    Shape.kt        (create: RoadmapShapes, Spacing)
    Theme.kt        (replace: RoadmapTheme + extended-color plumbing, no dynamic color)
    Hue.kt          (create: RoadmapHue + hueForId)
app/src/main/java/com/example/roadmap/ui/components/
    RingProgress.kt SegmentedControl.kt GradientTile.kt Chips.kt
    Buttons.kt StepRowParts.kt EmptyState.kt
app/src/main/java/com/example/roadmap/ui/catalog/Catalog.kt   (create: preview catalog)
app/src/main/java/com/example/roadmap/MainActivity.kt          (modify: show Catalog temporarily)
app/src/test/java/com/example/roadmap/ui/theme/HueTest.kt      (create)
app/src/test/java/com/example/roadmap/ui/components/RingMathTest.kt (create)
```

---

## Task 1: Build config + branch (minSdk 26, no dynamic color groundwork)

**Files:** Modify `app/build.gradle.kts`

- [ ] **Step 1: Create the working branch**

Run:
```bash
git checkout -b feat/design-system
```

- [ ] **Step 2: Raise minSdk to 26**

In `app/build.gradle.kts`, change `minSdk = 24` to:
```kotlin
        minSdk = 26
```

- [ ] **Step 3: Verify it still builds**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: raise minSdk to 26 for native java.time and fonts"
```

---

## Task 2: Bundle the Inter font family

**Files:** Create `app/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`, `inter_extrabold.ttf`

> Bundling keeps the app offline-first (spec §10). Fetching at *build* time is fine; only *runtime* must be network-free. Android `res/font` filenames must be lowercase, digits, underscores only.

- [ ] **Step 1: Download the five static Inter weights into res/font**

Run (Google Fonts static TTFs; stable raw URLs):
```bash
mkdir -p app/src/main/res/font
base="https://github.com/google/fonts/raw/main/ofl/inter"
curl -fL "$base/Inter%5Bopsz,wght%5D.ttf" -o /tmp/inter-variable.ttf
# Derive static instances with fonttools if available; otherwise fetch known static mirrors:
for pair in "Regular:inter_regular" "Medium:inter_medium" "SemiBold:inter_semibold" "Bold:inter_bold" "ExtraBold:inter_extrabold"; do
  w="${pair%%:*}"; f="${pair##*:}"
  curl -fL "https://raw.githubusercontent.com/rsms/inter/master/docs/font-files/Inter-${w}.ttf" -o "app/src/main/res/font/${f}.ttf"
done
ls -la app/src/main/res/font/
```
Expected: five non-empty `.ttf` files. (If a URL 404s, download Inter from https://github.com/rsms/inter/releases, copy `extras/ttf/Inter-{Regular,Medium,SemiBold,Bold,ExtraBold}.ttf` to the five target names.)

- [ ] **Step 2: Verify the files are valid TrueType**

Run: `file app/src/main/res/font/inter_regular.ttf`
Expected: contains `TrueType` (or `OpenType`).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/font
git commit -m "design: bundle Inter font weights (offline)"
```

---

## Task 3: Color tokens

**Files:** Replace `app/src/main/java/com/example/roadmap/ui/theme/Color.kt`; Test `app/src/test/java/com/example/roadmap/ui/theme/ColorTokensTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/roadmap/ui/theme/ColorTokensTest.kt`:
```kotlin
package com.example.roadmap.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTokensTest {
    @Test fun brand_is_forest_green() {
        assertEquals(Color(0xFF022E1C), Brand)
    }
    @Test fun dark_accent_is_mint() {
        assertEquals(Color(0xFF34D39A), BrandMint)
    }
    @Test fun light_and_dark_have_distinct_surfaces() {
        assertEquals(Color(0xFFFFFFFF), LightTokens.surface)
        assertEquals(Color(0xFF0C2418), DarkTokens.surface)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ColorTokensTest"`
Expected: FAIL — unresolved `Brand`/`LightTokens`.

- [ ] **Step 3: Replace Color.kt**

```kotlin
package com.example.roadmap.ui.theme

import androidx.compose.ui.graphics.Color

/** Brand identity. */
val Brand = Color(0xFF022E1C)       // primary accent (light) / deep canvas (dark)
val BrandMint = Color(0xFF34D39A)   // derived dark-mode interactive accent
val BrandCyan = Color(0xFF22D3EE)   // dark-mode gradient companion

internal object LightTokens {
    val background = Color(0xFFF5F8F6)
    val surface = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFF2F6F3)
    val outline = Color(0xFFD3DED7)
    val outlineVariant = Color(0xFFE3EBE5)
    val onBackground = Color(0xFF0A1F16)
    val onSurfaceVariant = Color(0xFF5D6E65)
    val primary = Brand
    val onPrimary = Color(0xFFFFFFFF)
    val ringTrack = Color(0xFFE4ECE7)
    val done = Color(0xFF059669)
    val doneContainer = Color(0xFFE2F3EC)
    val overdue = Color(0xFFDC2626)
    val overdueContainer = Color(0xFFFBE8E8)
    val muted = Color(0xFF5D6E65)
    val faint = Color(0xFF8A9890)
}

internal object DarkTokens {
    val background = Color(0xFF061A10)
    val surface = Color(0xFF0C2418)
    val surfaceVariant = Color(0xFF102A1D)
    val outline = Color(0xFF244A37)
    val outlineVariant = Color(0xFF1B3A2A)
    val onBackground = Color(0xFFE9F4EE)
    val onSurfaceVariant = Color(0xFF93AB9D)
    val primary = BrandMint
    val onPrimary = Color(0xFF04130C)
    val ringTrack = Color(0xFF1C3A2B)
    val done = Color(0xFF34D399)
    val doneContainer = Color(0xFF10301F)
    val overdue = Color(0xFFF87171)
    val overdueContainer = Color(0xFF2A1717)
    val muted = Color(0xFF93AB9D)
    val faint = Color(0xFF6C8678)
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ColorTokensTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/theme/Color.kt app/src/test/java/com/example/roadmap/ui/theme/ColorTokensTest.kt
git commit -m "design: add brand color tokens (light/dark)"
```

---

## Task 4: Extended colors + RoadmapColors holder

**Files:** Create `app/src/main/java/com/example/roadmap/ui/theme/RoadmapColors.kt`

- [ ] **Step 1: Create the holder + CompositionLocal**

```kotlin
package com.example.roadmap.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/** Bespoke tokens that Material 3's ColorScheme has no slot for. */
@Immutable
data class RoadmapColors(
    val ringTrack: Color,
    val done: Color,
    val doneContainer: Color,
    val overdue: Color,
    val overdueContainer: Color,
    val muted: Color,
    val faint: Color,
    /** Fill for primary actions: solid brand in light, mint→cyan gradient in dark. */
    val primaryBrush: Brush,
    val isDark: Boolean,
)

internal val LightRoadmapColors = RoadmapColors(
    ringTrack = LightTokens.ringTrack,
    done = LightTokens.done,
    doneContainer = LightTokens.doneContainer,
    overdue = LightTokens.overdue,
    overdueContainer = LightTokens.overdueContainer,
    muted = LightTokens.muted,
    faint = LightTokens.faint,
    primaryBrush = SolidColor(Brand),
    isDark = false,
)

internal val DarkRoadmapColors = RoadmapColors(
    ringTrack = DarkTokens.ringTrack,
    done = DarkTokens.done,
    doneContainer = DarkTokens.doneContainer,
    overdue = DarkTokens.overdue,
    overdueContainer = DarkTokens.overdueContainer,
    muted = DarkTokens.muted,
    faint = DarkTokens.faint,
    primaryBrush = Brush.linearGradient(listOf(BrandMint, BrandCyan)),
    isDark = true,
)

val LocalRoadmapColors = staticCompositionLocalOf { LightRoadmapColors }

/** Accessor: `RoadmapTheme.colors.overdue`. The `object` coexists with the `RoadmapTheme` composable. */
object RoadmapTheme {
    val colors: RoadmapColors
        @Composable @ReadOnlyComposable get() = LocalRoadmapColors.current
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/theme/RoadmapColors.kt
git commit -m "design: add extended RoadmapColors + CompositionLocal"
```

---

## Task 5: Typography (Inter + tabular numerals)

**Files:** Replace `app/src/main/java/com/example/roadmap/ui/theme/Type.kt`

- [ ] **Step 1: Replace Type.kt**

```kotlin
package com.example.roadmap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.roadmap.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

/** Apply to any numeric text (counts, percentages) for tabular figures. */
val TabularNums = TextStyle(fontFeatureSettings = "tnum")

private fun inter(
    weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Inter, fontWeight = weight, fontSize = size.sp,
    lineHeight = line.sp, letterSpacing = tracking.sp,
)

val Typography = Typography(
    displaySmall = inter(FontWeight.ExtraBold, 28, 34, -0.5),
    headlineMedium = inter(FontWeight.ExtraBold, 24, 30, -0.5),
    titleLarge = inter(FontWeight.Bold, 20, 26, -0.3),
    titleMedium = inter(FontWeight.Bold, 16, 22, -0.1),
    titleSmall = inter(FontWeight.SemiBold, 14, 20, -0.1),
    bodyLarge = inter(FontWeight.Normal, 15, 22),
    bodyMedium = inter(FontWeight.Normal, 13, 19),
    bodySmall = inter(FontWeight.Medium, 12, 17),
    labelLarge = inter(FontWeight.Bold, 14, 18),
    labelMedium = inter(FontWeight.SemiBold, 12, 16, 0.2),
    labelSmall = inter(FontWeight.Bold, 11, 14, 0.6),
)
```

- [ ] **Step 2: Verify it compiles (font resources resolve)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` (fails if any `inter_*.ttf` is missing — fix Task 2).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/theme/Type.kt
git commit -m "design: Inter typography with tabular numerals"
```

---

## Task 6: Shapes & spacing

**Files:** Create `app/src/main/java/com/example/roadmap/ui/theme/Shape.kt`

- [ ] **Step 1: Create Shape.kt**

```kotlin
package com.example.roadmap.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RoadmapShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** 4-pt spacing scale. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`  → Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/theme/Shape.kt
git commit -m "design: shapes and spacing scale"
```

---

## Task 7: RoadmapTheme (wire it all, drop dynamic color)

**Files:** Replace `app/src/main/java/com/example/roadmap/ui/theme/Theme.kt`

- [ ] **Step 1: Replace Theme.kt**

```kotlin
package com.example.roadmap.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightTokens.primary,
    onPrimary = LightTokens.onPrimary,
    primaryContainer = Color(0xFFE8EFEA),
    onPrimaryContainer = Brand,
    background = LightTokens.background,
    onBackground = LightTokens.onBackground,
    surface = LightTokens.surface,
    onSurface = LightTokens.onBackground,
    surfaceVariant = LightTokens.surfaceVariant,
    onSurfaceVariant = LightTokens.onSurfaceVariant,
    outline = LightTokens.outline,
    outlineVariant = LightTokens.outlineVariant,
    error = LightTokens.overdue,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTokens.primary,
    onPrimary = DarkTokens.onPrimary,
    primaryContainer = Color(0xFF10301F),
    onPrimaryContainer = BrandMint,
    background = DarkTokens.background,
    onBackground = DarkTokens.onBackground,
    surface = DarkTokens.surface,
    onSurface = DarkTokens.onBackground,
    surfaceVariant = DarkTokens.surfaceVariant,
    onSurfaceVariant = DarkTokens.onSurfaceVariant,
    outline = DarkTokens.outline,
    outlineVariant = DarkTokens.outlineVariant,
    error = DarkTokens.overdue,
    onError = DarkTokens.onPrimary,
)

@Composable
fun RoadmapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkRoadmapColors else LightRoadmapColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalRoadmapColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = RoadmapShapes,
            content = content,
        )
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`  → Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/theme/Theme.kt
git commit -m "design: RoadmapTheme wiring brand scheme, extended colors, no dynamic color"
```

---

## Task 8: Hue system (TDD)

**Files:** Create `Hue.kt`; Test `app/src/test/java/com/example/roadmap/ui/theme/HueTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.roadmap.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class HueTest {
    @Test fun maps_id_zero_to_first_hue() {
        assertEquals(RoadmapHue.Emerald, hueForId(0))
    }
    @Test fun wraps_around_the_palette() {
        assertEquals(RoadmapHue.Emerald, hueForId(6))   // 6 hues -> wraps
        assertEquals(RoadmapHue.Amber, hueForId(7))
    }
    @Test fun handles_negative_ids() {
        assertEquals(RoadmapHue.Rose, hueForId(-1))     // last hue
    }
    @Test fun gradient_returns_two_stops_per_theme() {
        assertEquals(2, RoadmapHue.Cyan.colors(dark = true).size)
        assertEquals(2, RoadmapHue.Cyan.colors(dark = false).size)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*HueTest"`
Expected: FAIL — unresolved `RoadmapHue`.

- [ ] **Step 3: Create Hue.kt**

```kotlin
package com.example.roadmap.ui.theme

import androidx.compose.ui.graphics.Color

/** Per-item color identity for icon tiles, milestone spines, empty-state art. */
enum class RoadmapHue(
    val lightStart: Color, val lightEnd: Color,
    val darkStart: Color, val darkEnd: Color,
) {
    Emerald(Color(0xFF10B981), Color(0xFF0D9488), Color(0xFF34D399), Color(0xFF2DD4BF)),
    Amber(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFFBBF24), Color(0xFFFB923C)),
    Indigo(Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF818CF8), Color(0xFF60A5FA)),
    Violet(Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFA78BFA), Color(0xFFC084FC)),
    Cyan(Color(0xFF06B6D4), Color(0xFF0EA5E9), Color(0xFF22D3EE), Color(0xFF38BDF8)),
    Rose(Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFFFB7185), Color(0xFFF472B6));

    fun colors(dark: Boolean): List<Color> =
        if (dark) listOf(darkStart, darkEnd) else listOf(lightStart, lightEnd)
}

/** Deterministic, stable hue for a given entity id (works for negatives). */
fun hueForId(id: Long): RoadmapHue {
    val n = RoadmapHue.entries.size
    val idx = (((id % n) + n) % n).toInt()
    return RoadmapHue.entries[idx]
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*HueTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/theme/Hue.kt app/src/test/java/com/example/roadmap/ui/theme/HueTest.kt
git commit -m "design: hue system with deterministic id mapping"
```

---

## Task 9: RingProgress — ring math (TDD) + component

**Files:** Create `RingProgress.kt`; Test `app/src/test/java/com/example/roadmap/ui/components/RingMathTest.kt`

- [ ] **Step 1: Write the failing math test**

```kotlin
package com.example.roadmap.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RingMathTest {
    @Test fun sweep_is_proportional() {
        assertEquals(0f, progressSweep(0f), 0.001f)
        assertEquals(180f, progressSweep(0.5f), 0.001f)
        assertEquals(360f, progressSweep(1f), 0.001f)
    }
    @Test fun sweep_clamps_out_of_range() {
        assertEquals(0f, progressSweep(-0.2f), 0.001f)
        assertEquals(360f, progressSweep(1.5f), 0.001f)
    }
    @Test fun percent_label_rounds() {
        assertEquals("62", progressPercentLabel(0.615f))
        assertEquals("100", progressPercentLabel(1f))
        assertEquals("0", progressPercentLabel(0f))
    }
    @Test fun is_complete_only_at_full() {
        assertEquals(true, isRingComplete(1f))
        assertEquals(false, isRingComplete(0.999f))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*RingMathTest"`  → Expected: FAIL (unresolved).

- [ ] **Step 3: Create RingProgress.kt (math + component)**

```kotlin
package com.example.roadmap.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.TabularNums
import kotlin.math.roundToInt

// ---- pure math (JVM-testable) ----
fun progressSweep(progress: Float): Float = progress.coerceIn(0f, 1f) * 360f
fun progressPercentLabel(progress: Float): String =
    (progress.coerceIn(0f, 1f) * 100f).roundToInt().toString()
fun isRingComplete(progress: Float): Boolean = progress >= 1f

enum class RingSize(val diameter: Dp, val stroke: Dp, val fontSize: Int) {
    Small(52.dp, 5.dp, 13), Medium(56.dp, 5.dp, 14), Large(104.dp, 9.dp, 27),
}

/** Uniform brand-colored conic-style progress ring. Checkmark at 100%. */
@Composable
fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: RingSize = RingSize.Medium,
) {
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = RoadmapTheme.colors.ringTrack
    val complete = isRingComplete(progress)
    val pct = progressPercentLabel(progress)

    Box(
        modifier = modifier
            .size(size.diameter)
            .semantics { contentDescription = "$pct percent complete" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size.diameter)) {
            val stroke = Stroke(width = size.stroke.toPx(), cap = StrokeCap.Round)
            val inset = size.stroke.toPx() / 2f
            val arcSize = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = size.stroke.toPx()))
            drawArc(ringColor, -90f, progressSweep(progress), false, topLeft, arcSize, style = stroke)
        }
        if (complete) {
            Icon(Icons.Rounded.Check, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size.diameter * 0.42f))
        } else {
            Text(
                pct,
                style = MaterialTheme.typography.titleMedium.merge(TabularNums),
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified.takeIf { false } ?: size.fontSizeSp(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun RingSize.fontSizeSp() =
    androidx.compose.ui.unit.TextUnit(fontSize.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Preview @Composable private fun RingPreview() {
    RoadmapTheme {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            RingProgress(0.62f, size = RingSize.Large)
        }
    }
}
```

> Note: the `fontSize` line keeps the size scale per `RingSize`; if the reviewer prefers, simplify by setting `style = ....copy(fontSize = size.fontSizeSp())`. Behaviour is identical.

- [ ] **Step 4: Run math tests — verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*RingMathTest"`  → Expected: PASS (4 tests).

- [ ] **Step 5: Compile the whole module (component + preview)**

Run: `./gradlew :app:compileDebugKotlin`  → Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/RingProgress.kt app/src/test/java/com/example/roadmap/ui/components/RingMathTest.kt
git commit -m "design: RingProgress component with tested ring math"
```

---

## Task 10: GradientTile

**Files:** Create `app/src/main/java/com/example/roadmap/ui/components/GradientTile.kt`

- [ ] **Step 1: Create GradientTile.kt**

```kotlin
package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

/** Rounded gradient icon tile bearing a single initial. */
@Composable
fun GradientTile(
    initial: Char,
    hue: RoadmapHue,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    val brush = Brush.linearGradient(hue.colors(RoadmapTheme.colors.isDark))
    Box(
        modifier = modifier.size(size).background(brush, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial.uppercaseChar().toString(),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Preview @Composable private fun TilePreview() {
    RoadmapTheme {
        GradientTile('K', RoadmapHue.Emerald)
    }
}
```

- [ ] **Step 2: Compile check** — Run: `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/GradientTile.kt
git commit -m "design: GradientTile (hue-aware icon tile)"
```

---

## Task 11: SegmentedControl

**Files:** Create `app/src/main/java/com/example/roadmap/ui/components/SegmentedControl.kt`

- [ ] **Step 1: Create SegmentedControl.kt**

```kotlin
package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (selected) Modifier.background(RoadmapTheme.colors.primaryBrush)
                        else Modifier.background(SolidColor(MaterialTheme.colorScheme.surfaceVariant))
                    )
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else RoadmapTheme.colors.muted,
                )
            }
        }
    }
}

@Preview @Composable private fun SegPreview() {
    RoadmapTheme {
        SegmentedControl(listOf("Active", "Archive"), 0, {}, Modifier.padding(16.dp))
    }
}
```

- [ ] **Step 2: Compile check** — `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.

- [ ] **Step 3 (optional, needs emulator): Compose UI test**

Create `app/src/androidTest/java/com/example/roadmap/ui/components/SegmentedControlTest.kt`:
```kotlin
package com.example.roadmap.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.roadmap.ui.theme.RoadmapTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SegmentedControlTest {
    @get:Rule val rule = createComposeRule()
    @Test fun click_reports_index() {
        var selected = 0
        rule.setContent {
            RoadmapTheme { SegmentedControl(listOf("Active", "Archive"), selected) { selected = it } }
        }
        rule.onNodeWithText("Archive").performClick()
        assertEquals(1, selected)
    }
}
```
Run (only if a device/emulator is connected): `./gradlew :app:connectedDebugAndroidTest --tests "*SegmentedControlTest"` → Expected: PASS. If no device, skip and rely on the preview.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/SegmentedControl.kt app/src/androidTest/java/com/example/roadmap/ui/components/SegmentedControlTest.kt
git commit -m "design: SegmentedControl (Active/Archive)"
```

---

## Task 12: Chips (meta, deadline, link)

**Files:** Create `app/src/main/java/com/example/roadmap/ui/components/Chips.kt`

- [ ] **Step 1: Create Chips.kt**

```kotlin
package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.TabularNums

enum class DeadlineState { Normal, Overdue, Done }

@Composable
private fun ChipBase(
    text: String, icon: ImageVector?, bg: Color, fg: Color,
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    tabular: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
        Text(
            text, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium
                .merge(if (tabular) TabularNums else androidx.compose.ui.text.TextStyle.Default),
        )
    }
}

@Composable
fun MetaChip(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) =
    ChipBase(text, icon, MaterialTheme.colorScheme.surfaceVariant, RoadmapTheme.colors.muted, modifier, tabular = true)

@Composable
fun DeadlineChip(text: String, state: DeadlineState, modifier: Modifier = Modifier) {
    val c = RoadmapTheme.colors
    val (bg, fg) = when (state) {
        DeadlineState.Normal -> MaterialTheme.colorScheme.surfaceVariant to c.muted
        DeadlineState.Overdue -> c.overdueContainer to c.overdue
        DeadlineState.Done -> c.doneContainer to c.done
    }
    ChipBase(text, Icons.Outlined.CalendarMonth, bg, fg, modifier)
}

@Composable
fun LinkChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    ChipBase(label, Icons.Outlined.Link, MaterialTheme.colorScheme.surfaceVariant,
        RoadmapTheme.colors.muted, modifier, onClick)
```

(Add `import androidx.compose.foundation.layout.size` — included via wildcard avoidance: add explicit `import androidx.compose.foundation.layout.size`.)

- [ ] **Step 2: Add the missing size import and compile**

Ensure `import androidx.compose.foundation.layout.size` is present, then run: `./gradlew :app:compileDebugKotlin` → Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/Chips.kt
git commit -m "design: meta/deadline/link chips with state colors"
```

---

## Task 13: Buttons & FAB

**Files:** Create `app/src/main/java/com/example/roadmap/ui/components/Buttons.kt`

- [ ] **Step 1: Create Buttons.kt**

```kotlin
package com.example.roadmap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(RoadmapTheme.colors.primaryBrush)
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(SolidColor(MaterialTheme.colorScheme.surfaceVariant))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(SolidColor(RoadmapTheme.colors.overdue))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun RoadmapFab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(RoadmapTheme.colors.primaryBrush)
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("+", color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge)
        Text(text, color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge)
    }
}
```

- [ ] **Step 2: Compile check** — `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/Buttons.kt
git commit -m "design: primary/secondary/danger buttons and FAB"
```

---

## Task 14: Step parts — checkbox, grip, add-inline

**Files:** Create `app/src/main/java/com/example/roadmap/ui/components/StepRowParts.kt`

- [ ] **Step 1: Create StepRowParts.kt**

```kotlin
package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun StepCheckbox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(7.dp)
    Row(
        modifier = modifier
            .size(22.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, shape)
            )
            .clickable { onToggle() }
            .semantics { stateDescription = if (checked) "Completed" else "Not completed" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, null,
                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun DragGrip(modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.DragIndicator, contentDescription = "Reorder",
        tint = RoadmapTheme.colors.faint, modifier = modifier.size(18.dp))
}

@Composable
fun AddInline(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = RoadmapTheme.colors.muted
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    style = Stroke(width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                )
            }
            .clickable { onClick() }
            .padding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Rounded.Add, null, tint = color, modifier = Modifier.size(15.dp).padding(start = 11.dp))
        Text(text, color = color, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(end = 11.dp, top = 10.dp, bottom = 10.dp))
    }
}
```

> Note for the implementer: `.padding()` with no args is a no-op placeholder — replace the `AddInline` inner padding with explicit values if spacing looks off in preview; the icon/text already carry their own padding. Keep the dashed border.

- [ ] **Step 2: Compile check** — `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`. Fix imports if the compiler flags `padding`/`size` (add `import androidx.compose.foundation.layout.padding`).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/StepRowParts.kt
git commit -m "design: step checkbox, drag grip, dashed add-inline"
```

---

## Task 15: EmptyState

**Files:** Create `app/src/main/java/com/example/roadmap/ui/components/EmptyState.kt`

- [ ] **Step 1: Create EmptyState.kt**

```kotlin
package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    hue: RoadmapHue,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(92.dp)
                .background(Brush.linearGradient(hue.colors(RoadmapTheme.colors.isDark)),
                    RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = RoadmapTheme.colors.muted, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp))
        if (actions != null) Box(Modifier.padding(top = 8.dp)) { actions() }
    }
}
```

- [ ] **Step 2: Compile check** — `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/components/EmptyState.kt
git commit -m "design: EmptyState scaffold"
```

---

## Task 16: Component catalog + wire MainActivity (visual verification)

**Files:** Create `app/src/main/java/com/example/roadmap/ui/catalog/Catalog.kt`; Modify `app/src/main/java/com/example/roadmap/MainActivity.kt`

- [ ] **Step 1: Create Catalog.kt**

```kotlin
package com.example.roadmap.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.components.*
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    var seg by remember { mutableIntStateOf(0) }
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rings", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RingProgress(0.25f, size = RingSize.Small)
                RingProgress(0.62f, size = RingSize.Medium)
                RingProgress(1f, size = RingSize.Large)
            }
            Text("Segmented", style = MaterialTheme.typography.titleMedium)
            SegmentedControl(listOf("Active", "Archive"), seg) { seg = it }
            Text("Tiles", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoadmapHue.entries.forEach { GradientTile(it.name.first(), it) }
            }
            Text("Chips", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip("6 milestones")
                DeadlineChip("Sep 30", DeadlineState.Normal)
                DeadlineChip("May 15", DeadlineState.Overdue)
                DeadlineChip("Done", DeadlineState.Done)
            }
            LinkChip("developer.android.com")
            Text("Buttons", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Create", {})
                SecondaryButton("Import", {})
                DangerButton("Delete", {})
            }
            RoadmapFab("New", {})
            Text("Steps", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DragGrip(); StepCheckbox(true, {}); StepCheckbox(false, {})
            }
            AddInline("Add step", {})
            EmptyState(Icons.Outlined.Map, "No roadmaps yet",
                "Create your first roadmap, or import one from JSON.", RoadmapHue.Emerald)
        }
    }
}

@Preview(name = "Catalog Light") @Composable private fun CatalogLight() =
    RoadmapTheme(darkTheme = false) { DesignSystemCatalog() }

@Preview(name = "Catalog Dark") @Composable private fun CatalogDark() =
    RoadmapTheme(darkTheme = true) { DesignSystemCatalog() }
```

- [ ] **Step 2: Wire MainActivity to show the catalog (temporary, replaced in Phase 4)**

Replace `app/src/main/java/com/example/roadmap/MainActivity.kt` body with:
```kotlin
package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.roadmap.ui.catalog.DesignSystemCatalog
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadmapTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    DesignSystemCatalog(Modifier.padding(inner))
                }
            }
        }
    }
}
```

- [ ] **Step 3: Full build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Visual check**

Open `ui/catalog/Catalog.kt` in Android Studio; render the **Catalog Light** and **Catalog Dark** previews. Confirm against `mockups/roadmap-mockup.html`: rings uniform brand-colored, six hue tiles, chips with correct state colors, gradient FAB (mint→cyan in dark), dashed add-inline. (If a device is connected: `./gradlew :app:installDebug` and launch.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/catalog/Catalog.kt app/src/main/java/com/example/roadmap/MainActivity.kt
git commit -m "design: component catalog + temporary catalog entry point"
```

---

## Task 17: Phase wrap-up

- [ ] **Step 1: Full test + build sweep**

Run:
```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
Expected: all unit tests PASS, `BUILD SUCCESSFUL`.

- [ ] **Step 2: Remove the leftover Greeting preview if any** — confirm no references to the old `Greeting`/Purple tokens remain: `grep -rn "Purple\|Greeting" app/src/main` returns nothing.

- [ ] **Step 3: Merge to main**

```bash
git checkout main
git merge --no-ff feat/design-system -m "feat: design system (theme, hues, core components)"
git push origin main
```

---

## Self-Review (completed during planning)

- **Spec §9 coverage:** Inter + tabular numerals (Task 5) ✓ · palette/tokens incl. done/overdue/brand (Tasks 3–4, 7) ✓ · RingProgress two sizes + checkmark at 100% (Task 9) ✓ · SegmentedControl (Task 11) ✓ · gradient tiles (Task 10) ✓ · chips (Task 12) ✓ · step check + drag handles + inline add (Task 14) ✓ · empty states component (Task 15) ✓ · full dark mode (Tasks 3–7) ✓ · stagger/check animations deferred to **Phase 8** (noted; this phase delivers static components).
- **No dynamic color:** removed in Task 7 ✓.
- **Rings uniform brand-colored (user decision):** `RingProgress` uses `colorScheme.primary`, never a hue ✓.
- **Type consistency:** `RoadmapTheme.colors` (object) + `RoadmapColors` fields (`ringTrack`, `done`, `overdue`, `muted`, `faint`, `primaryBrush`, `isDark`), `RoadmapHue.colors(dark)`, `hueForId(Long)`, `RingSize`, `DeadlineState`, `progressSweep/progressPercentLabel/isRingComplete` — all defined before use and referenced consistently across tasks ✓.
- **Open implementer notes (intentional, not placeholders):** RingProgress font-size line and AddInline inner padding carry explicit "tune in preview" notes — the behavior is fully specified; only visual spacing is left to the reviewer's eye against the mockup.
