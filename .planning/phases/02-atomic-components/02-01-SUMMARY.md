---
phase: 02-atomic-components
plan: "01"
subsystem: buttons
tags: [buttons, hover, pressed, focus, disabled, interaction-states, animation, BTN-01, BTN-02, BTN-03, BTN-04]
dependency_graph:
  requires:
    - 01-foundation (AeroTheme.colors, AeroTypography, GlassModifiers)
  provides:
    - AeroButton (BTN-01)
    - AeroOutlinedButton (BTN-02)
    - AeroIconButton (BTN-03)
    - AeroToolbar + AeroToolbarDefaults.Divider (BTN-04)
    - InteractionStates internal helpers (canonical hover/pressed/focus pattern for Phase 2)
    - ButtonsSection showcase
  affects:
    - 02-02 through 02-05 (all copy the InteractionStates pattern)
    - 02-06 (ShowcaseApp wires ButtonsSection)
tech_stack:
  added:
    - compose.animation dependency in library/build.gradle.kts (already present from 02-02 agent, no-op re-add)
  patterns:
    - animateFloatAsState with tween(150ms, LinearEasing) for pressed scale (0.97f)
    - drawWithContent overlay for hover (buttonHover token)
    - Modifier.border for focus ring (borderSelected, 2.dp)
    - Modifier.alpha + CompositionLocalProvider for disabled state (0.4f)
    - indication=null on clickable to suppress M3 ripple when drawing custom states
key_files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroToolbar.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt
  modified: []
decisions:
  - "Hover overlay via drawWithContent inside Button content lambda (Box wrapping Text) rather than outer wrapper — avoids clipping issues with M3 Button shape"
  - "AeroIconButton uses indication=null on clickable to suppress M3 ripple; hover/pressed drawn manually via Box overlay + graphicsLayer scale"
  - "AeroToolbarDefaults is a top-level object (not companion) — idiomatic Kotlin for composable-containing helpers"
  - "showDividers parameter on AeroToolbar is reserved/no-op — consumers compose AeroToolbarDefaults.Divider() manually (documented in KDoc)"
  - "LocalContentColor import updated to androidx.compose.material3.LocalContentColor by linter — accepted, M3 is the correct source"
metrics:
  duration: "~8 minutes"
  completed_date: "2026-04-28"
  tasks_completed: 3
  files_created: 6
  files_modified: 0
requirements_satisfied: [BTN-01, BTN-02, BTN-03, BTN-04]
---

# Phase 2 Plan 01: Button Components Summary

**One-liner:** Four Aero button variants (filled, outlined, icon, toolbar) with 150ms LinearEasing hover/pressed/focus/disabled animation pattern using AeroTheme.colors tokens.

## What Was Built

### Library — 5 new files under `library/.../components/buttons/`

**InteractionStates.kt** — Internal helpers establishing the canonical animation pattern:
- `rememberHoverState`, `rememberPressedState`, `rememberFocusState` wrap `collectIs*AsState` from a shared `InteractionSource`
- `animatedAlpha` — convenience `animateFloatAsState` at 150ms LinearEasing
- `ANIMATION_DURATION_MS = 150` — single source of truth for all Phase 2 animation durations

**AeroButton.kt** (BTN-01) — Filled button:
- M3 `Button` primitive, `containerColor = primary.copy(0.8f)`, disabled at `0.4f` alpha
- Hover: `buttonHover` overlay via `drawWithContent` inside content Box
- Pressed: `animateFloatAsState` 0.97f scale via `graphicsLayer`
- Focus: `Modifier.border(2.dp, borderSelected, RoundedCornerShape(4.dp))`
- Shape: `RoundedCornerShape(4.dp)`, `height = 30.dp`, padding `h=12dp, v=2dp`

**AeroOutlinedButton.kt** (BTN-02) — Outlined transparent button:
- M3 `OutlinedButton` primitive, `contentColor = onSurface`
- Border: 1.dp `glassBorder` at rest → 2.dp `borderSelected` on focus → `borderDefault.copy(0.4f)` disabled
- Same hover/pressed/focus/disabled matrix as AeroButton

**AeroIconButton.kt** (BTN-03) — Square icon button:
- Box-based (not M3 IconButton), `size = 32.dp`, `RoundedCornerShape(4.dp)`
- Hover: `buttonHover` background in `Box(matchParentSize())`
- `indication = null` on `Modifier.clickable` — suppresses M3 ripple (custom states drawn manually)
- Disabled: `Modifier.alpha(0.4f)` + `CompositionLocalProvider(LocalContentColor)` for icon tint

**AeroToolbar.kt** (BTN-04) — Horizontal toolbar:
- `Row` with `glassPanel(cornerRadius = 4.dp)` background, `height = 40.dp`
- `Arrangement.spacedBy(2.dp)` between icon buttons
- `AeroToolbarDefaults.Divider()` — `Box` with `1.dp` width, `borderDefault.copy(0.5f)` fill

### Showcase — 1 new file

**ButtonsSection.kt** — Table-layout section with private `SectionRow` helper (140.dp label column + variant row). Demonstrates all four variants: enabled + disabled per button type, plus a full AeroToolbar with B/I/U/S icon buttons and a divider.

## Animation Pattern (Canonical for Phase 2)

All interactive states use `animateFloatAsState` with `tween(150, LinearEasing)`:

| State    | Implementation                                | Visual                            |
|----------|-----------------------------------------------|-----------------------------------|
| Hover    | `buttonHover` overlay via `drawWithContent`   | White glow at 25% alpha           |
| Pressed  | `graphicsLayer { scaleX/Y = 0.97f }`          | Slight shrink                     |
| Focus    | `Modifier.border(2.dp, borderSelected, ...)`  | Accent-colored outer ring         |
| Disabled | `Modifier.alpha(0.4f)` + color copies at 0.4  | Faded, non-interactive            |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-existing `animateColorAsState` import from wrong package**
- **Found during:** Task 1 (first compile attempt)
- **Issue:** Multiple stub files created by earlier Phase 2 agents imported `animateColorAsState` from `androidx.compose.animation.core` — the function actually lives in `androidx.compose.animation`. However, the HEAD commit `1aa1427` (02-02 agent) had already fixed the imports and added `compose.animation`. The working tree edits by this agent were no-ops that matched HEAD.
- **Fix:** Verified HEAD already correct — `compose.animation` in build.gradle.kts, corrected imports in all stubs. No net change.
- **Files modified:** None (already correct in HEAD)
- **Commit:** n/a

**2. [Rule 3 - Blocking] `LayoutDirection` wrong import in AeroDropdownMenu.kt**
- **Found during:** Task 1 compile
- **Issue:** `import androidx.compose.ui.layout.LayoutDirection` — correct is `androidx.compose.ui.unit.LayoutDirection`. Again, HEAD already had the fix from a prior agent; this agent's edit was a no-op.
- **Fix:** Already correct in HEAD.
- **Files modified:** None (already correct in HEAD)
- **Commit:** n/a

## Compile Gates

- `./gradlew :library:compileKotlin` — BUILD SUCCESSFUL (verified with `--rerun-tasks`)
- `./gradlew :library:test` — BUILD SUCCESSFUL (19 existing tests pass, no regression)
- `./gradlew :showcase:compileKotlin` — BUILD SUCCESSFUL

## Self-Check: PASSED

All 6 files verified to exist. All acceptance criteria substrings confirmed present. Zero `Color(0x)` literals in buttons package. All three compile gates green.
