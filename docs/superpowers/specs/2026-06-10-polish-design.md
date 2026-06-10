# Phase 8 — Polish (animations + accessibility) — Design Spec

**Status:** Approved design, 2026-06-10
**Parent spec:** [PRODUCT_SPEC.md](../../../PRODUCT_SPEC.md) §9 (stagger on list load; check/strike on step toggle) + §10 (accessibility: color-not-alone, touch-friendly checkboxes/drag handles).
**Scope:** A focused polish pass — four small animations + an accessibility pass — all honoring the system "Remove animations" setting. The final phase of the original plan. One PR.

## 1. Goal & non-goals

**Goal:** Add the spec's motion (§9) and close the accessibility gaps (§10) without changing behavior or data. Every animation degrades to instant when the user has reduced motion enabled.

**Non-goals:** No new features, screens, or data changes. Not adopting Material 3 *dynamic color* (the app deliberately fixes the brand `#022E1C` — an already-settled deviation from §10's mention). No new dependencies. No restructuring of working screens beyond what these refinements touch.

## 2. Cross-cutting: motion tokens + reduce-motion

New `ui/theme/Motion.kt`:
- **Pure (JVM-tested):** `fun reduceMotion(animatorScale: Float): Boolean = animatorScale == 0f` — the system "Remove animations" a11y toggle sets `Settings.Global.ANIMATOR_DURATION_SCALE` to `0`.
- **Composable:** `@Composable fun rememberReduceMotion(): Boolean` — reads that setting via `LocalContext`'s `contentResolver` (default `1f` if unset) and returns `reduceMotion(scale)`.
- **Tokens:** shared constants — `MotionDurations` (e.g., `fast = 150`, `medium = 300` ms) and a standard easing — so the four animations are consistent.

Every animation in §3–§6 reads `rememberReduceMotion()` and, when `true`, snaps to the target with no animation. This is what makes adding motion accessibility-correct (not a regression for motion-sensitive users).

## 3. List load stagger (§9)

`ui/list/RoadmapListScreen.kt` — each `RoadmapCardItem` in the `LazyColumn` animates in on first appearance: fade (0→1 alpha) + a small upward slide (e.g., 12dp), staggered by item index (a small per-index start delay, capped so a long list doesn't lag). Implemented with a per-item enter animation (`animateFloatAsState`/`AnimatedVisibility` driven once on first composition). Reduce-motion → cards render at full alpha immediately.

## 4. Step-check animation (§9)

- `ui/components/StepRowParts.kt` `StepCheckbox` — animate the background color (`animateColorAsState` outline→primary) and the check icon (scale/fade in) on toggle.
- `ui/detail/RoadmapDetailScreen.kt` `StepRow` — animate the title color (`animateColorAsState` onSurface↔faint) on toggle; the strikethrough appears with it. (Strikethrough itself is a `TextDecoration` and isn't smoothly animatable; the color fade carries the transition.)
- Reduce-motion → instant swap (current behavior).

## 5. Ring sweep (§9 polish)

`ui/components/RingProgress.kt` — animate the drawn sweep: `val animatedSweep by animateFloatAsState(targetValue = progressSweep(progress), animationSpec = if (reduceMotion) snap() else tween(MotionDurations.medium))` and draw `animatedSweep` instead of the raw value. The pure `progressSweep`/`progressPercentLabel`/`isRingComplete` math and the 100% checkmark are unchanged. The percent text still reflects the target value. (`rememberReduceMotion()` is read inside the composable.)

## 6. Nav transitions (polish)

`ui/RoadmapApp.kt` — wrap the top-level tab content and the per-tab list↔detail switch in a `Crossfade` (or `AnimatedContent` with a fade) so switching tabs and drilling into/out of a detail cross-fades rather than hard-cutting. Keyed on a stable key (`tab` + which sub-screen). Reduce-motion → `Crossfade` with `snap()`/zero-duration (instant). Must preserve the existing back/nav-bar-hidden behavior and `rememberSaveable` state.

## 7. Accessibility pass (§10)

- **Touch targets ≥48dp:**
  - `StepCheckbox` — keep the 22dp visual, expand the interactive area to 48dp (`Modifier.minimumInteractiveComponentSize()` or a 48dp clickable wrapper).
  - `DragGrip` / the drag handle — ensure a ≥48dp touch area (the visual stays 18dp).
  - (Material `IconButton`s are already 48dp — no change.)
- **Semantics / TalkBack:**
  - `StepCheckbox` — switch `.clickable` → `Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })` so it announces "checkbox, checked/not checked" (it already sets `stateDescription`).
  - Roadmap list cards (`RoadmapCardItem`) and milestone cards (`MilestoneCard`) — `Modifier.semantics(mergeDescendants = true)` so TalkBack reads each card as one node with a sensible label, not a pile of fragments.
  - Audit: decorative icons pass `contentDescription = null` (most already do); interactive ones have labels.
- **Color-not-alone (audit-only, already satisfied):** `RingProgress` exposes a percent/Complete label; `DeadlineChip` pairs a calendar icon + text; done steps show a checkmark + strikethrough; overdue shows "Overdue ·" text. Confirm nothing regressed; no code change expected.

## 8. Testing & verification

- **Pure (JUnit):** `MotionTest` — `reduceMotion(0f) == true`, `reduceMotion(1f) == false`, `reduceMotion(0.5f) == false`. Ring math already covered by `RingMathTest`.
- **No Compose unit tests** (project convention). Verify on the emulator:
  - Toggle a step → checkbox + title + ring animate; list load → cards stagger; switch tabs / open a roadmap → crossfade.
  - **TalkBack on:** each card reads as a unit; checkbox announces role + state; ring announces percent; drag handle/checkbox are comfortable to hit.
  - **Settings → Remove animations on:** all four animations snap instantly (no motion).
  - Light + dark both correct.

## 9. Files

- Create: `ui/theme/Motion.kt`; test `ui/theme/MotionTest.kt`.
- Modify: `ui/list/RoadmapListScreen.kt` (stagger + card semantics), `ui/components/StepRowParts.kt` (checkbox animation + touch target + toggleable; drag-grip touch target), `ui/detail/RoadmapDetailScreen.kt` (step title animation + milestone-card semantics), `ui/components/RingProgress.kt` (animated sweep), `ui/RoadmapApp.kt` (crossfade transitions).

## 10. Resolved decisions

| Decision | Choice | Why |
|---|---|---|
| Reduce-motion source | `Settings.Global.ANIMATOR_DURATION_SCALE == 0` | The standard Android signal for the "Remove animations" a11y toggle; no new dependency. |
| Strikethrough animation | Color-fade carries it; decoration toggles | `TextDecoration` isn't smoothly animatable; YAGNI on a custom drawn strike. |
| Dynamic color | Not adopted | App fixes the brand `#022E1C` by design (settled). |
| Scope | All four bundles + cross-cutting reduce-motion, one PR | Cohesive polish pass; each item is small and independent within it. |
| Nav transition style | Gentle `Crossfade` | Subtle, low-risk, preserves the manual state machine + saved state. |
