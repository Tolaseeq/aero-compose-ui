---
phase: 06-showcase-iconssection
plan: 02
subsystem: ui
tags: [compose-desktop, aero-icons, phosphor, kotlin, showcase]

# Dependency graph
requires:
  - phase: 04-aeroicons-foundation
    provides: AeroIcons.CaretUp, AeroIcons.CaretDown, AeroIcons.X extension properties in internal/*.kt
  - phase: 05-component-migrations
    provides: internal.* import pattern, tint = colors.onSurface convention, Modifier.size(14.dp) affordance size
provides:
  - ButtonsSection.kt AeroIconButton row (BTN-03) renders real Icon(AeroIcons.{CaretUp,CaretDown,X}) at 14dp
  - SHW-06 grep gate closed: Text("▲"/"▼"/"×") = 0 hits in showcase/src/
affects: [06-03-visual-checkpoint]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "AeroIconButton content slot uses Icon(AeroIcons.*) with explicit tint + 14dp size (showcased at BTN-03)"

key-files:
  created: []
  modified:
    - "showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt"

key-decisions:
  - "tint = colors.onSurface used (matches original Text color, mechanical replacement, no color drift)"
  - "Modifier.size(14.dp) matches Phase 5 affordance-icon convention for in-button icons"
  - "contentDescription = null for decorative icons inside AeroIconButton showcase mockup"
  - "AeroToolbar B/I/U/S letter glyphs (lines 58-62) left untouched — locked v1.1 exclusion"

patterns-established:
  - "Phase 5 extension-property import pattern confirmed: AeroIcons facade import + individual internal.* imports per property"

requirements-completed: [SHW-06]

# Metrics
duration: 6min
completed: 2026-04-29
---

# Phase 6 Plan 02: ButtonsSection Text-Glyph Migration Summary

**Three Phosphor Icon(AeroIcons.{CaretUp,CaretDown,X}) calls replace Text("▲"/"▼"/"×") in ButtonsSection's AeroIconButton row, closing SHW-06 with 0 text-glyph hits in showcase/src/**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-29T11:52:57Z
- **Completed:** 2026-04-29T11:58:57Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments

- Replaced three `Text("▲"/"▼"/"×", color = colors.onSurface)` calls in ButtonsSection.kt AeroIconButton row with `Icon(AeroIcons.CaretUp/CaretDown/X, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))`
- Added four imports: `material3.Icon`, `layout.size`, `AeroIcons`, and `internal.CaretDown/CaretUp/X`
- Preserved AeroToolbar BIUS row (lines 70-75) untouched — locked v1.1 exclusion confirmed
- `:showcase:compileKotlin` passes cleanly with JDK 17

## Before / After Grep Counts

| Metric | Before | After |
|--------|--------|-------|
| `Text("▲"/"▼"/"×"/"✕")` hits in showcase/src/ | 3 | 0 |
| `Icon(AeroIcons.(CaretUp\|CaretDown\|X))` hits in ButtonsSection.kt | 0 | 3 |
| `Text("B"\|"I"\|"U"\|"S")` BIUS letter hits (locked exclusion) | 4 | 4 (unchanged) |
| `Modifier.size(14.dp)` hits in ButtonsSection.kt | 0 | 3 |

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace ButtonsSection.kt lines 50-52 Text glyphs with Icon(AeroIcons.*) calls + add imports** - `b7b7a84` (feat)

Also committed as companion to plan 06-01:
- **ShowcaseApp.kt wiring (IconsSection import + call)** - `243bd04` (feat) — was uncommitted from 06-01 execution

**Plan metadata:** (docs commit — see below)

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` — three AeroIconButton content lambdas migrated from Text glyphs to Icon(AeroIcons.*); four imports added; BIUS row untouched

## Decisions Made

- `tint = colors.onSurface` used for all three Icon calls — matches the original `color = colors.onSurface` on the Text glyphs; pure mechanical replacement, no color drift
- `Modifier.size(14.dp)` — Phase 5 affordance-icon convention for in-button icons (AeroToastHost + AeroNotificationBanner close buttons)
- `contentDescription = null` — decorative role inside a showcase mockup (Phase 5 convention for decorative icons)
- AeroToolbar BIUS row untouched — SHW-06 grep only covers ▲▼×✕; letter glyphs are out of v1.1 scope

## Deviations from Plan

### Note on acceptance criterion

The plan's acceptance criterion stated "`grep -c "tint = colors.onSurface"` returns at least 7 (4 BIUS letters + 3 migrated icons)". The BIUS rows use `color = colors.onSurface` (Text parameter), not `tint` (Icon parameter). The actual counts are: 3 `tint = colors.onSurface` (new Icon calls) + 4 `color = colors.onSurface` (unchanged BIUS Text calls) = 7 total `colors.onSurface` references. The plan criterion was an inaccuracy in the grep specification; the implementation is correct.

None — plan executed exactly as specified. All three glyph replacements are mechanical; no code logic changed.

## Issues Encountered

- **JDK version:** Default JAVA_HOME points to JDK 25 which causes Kotlin compiler `JavaVersion.parse("25.0.2")` failure. Required `JAVA_HOME="C:/Users/1/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2"` for successful compile (consistent with STATE.md recorded constraint: "JAVA_HOME must be JDK 17 for all Gradle invocations").
- **False-positive grep:** `grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/` matched the `×` (U+00D7) multiplication sign in a FoundationSection.kt KDoc comment (`120.dp × 80.dp`). The plan's grep pattern uses shell alternation that matched the character outside the `Text("...")` literal. Verified via per-character greps that no actual `Text()` glyph calls remain.
- **ShowcaseApp.kt uncommitted from 06-01:** ShowcaseApp.kt had the `IconsSection(toastState = toastState)` insertion uncommitted from the prior plan (06-01 only staged `IconsSection.kt`). Committed it as `243bd04` before staging this plan's task file.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- SHW-06 closed: 0 text-glyph `Text("▲"/"▼"/"×")` calls in showcase/src/
- SHW-04 (IconsSection grid) and SHW-05 (search/filter) closed by plan 06-01
- Phase 6 plan 03: three-theme visual checkpoint (formal v1.1 milestone sign-off gate) is ready to run
- `./gradlew :showcase:run` (with JAVA_HOME=JDK 17) launches the showcase with all Phase 6 deliverables in place

---
*Phase: 06-showcase-iconssection*
*Completed: 2026-04-29*
