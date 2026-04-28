---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: Phase 3 Plan 01 complete — popup infrastructure promoted to public components/popup/, CNT-05 + CNT-06 ship, AeroTheme installs LocalScrollbarStyle, AeroTextArea + AeroDropdownPopup retrofitted, 9 Wave-0 stub tests in place
stopped_at: Completed 03-01-PLAN.md (popup foundation + scrollbar primitives + AeroTheme wiring + retrofits)
last_updated: "2026-04-28T13:02:06.327Z"
last_activity: 2026-04-28 — Phase 3 Plan 01 complete — popup package public, CNT-05/CNT-06 ship, scrollbar style theme-wide, retrofits done (CNT-05, CNT-06 satisfied)
progress:
  total_phases: 3
  completed_phases: 2
  total_plans: 18
  completed_plans: 11
  percent: 70
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-27)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, and a showcase — no manual style work required
**Current focus:** Phase 1 — Foundation

## Current Position

Phase: 3 of 3 (Composite + Navigation) — IN PROGRESS
Current Plan: 1 of 8 in phase complete (03-01 popup foundation + scrollbar primitives + retrofits done)
Status: Phase 3 Plan 01 complete — popup infrastructure promoted to public components/popup/, CNT-05 + CNT-06 ship, AeroTheme installs LocalScrollbarStyle, AeroTextArea + AeroDropdownPopup retrofitted, 9 Wave-0 stub tests in place
Last activity: 2026-04-28 — Phase 3 Plan 01 complete — popup package public, CNT-05/CNT-06 ship, scrollbar style theme-wide, retrofits done (CNT-05, CNT-06 satisfied)

Progress: [██████░░░░] 61%

## Performance Metrics

**Velocity:**
- Total plans completed: 4
- Average duration: ~7 min
- Total execution time: ~27 min

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
| Phase 01-foundation P01 | 6 | 3 tasks | 14 files |
| Phase 01-foundation P02 | 11 | 3 tasks | 5 files |
| Phase 01-foundation P03 | 7 | 2 tasks | 3 files |
| Phase 01-foundation P04 | 3 | 2 tasks | 5 files |
| Phase 02-atomic-components P01 | 8 | 3 tasks | 6 files |
| Phase 02-atomic-components P02 | 7 | 3 tasks | 7 files |
| Phase 02-atomic-components P05 | 9 | 3 tasks | 5 files |
| Phase 02-atomic-components P04 | 9 | 3 tasks | 6 files |
| Phase 02-atomic-components P03 | 10 | 3 tasks | 6 files |
| Phase 02-atomic-components P06 | ~45 | 3 tasks | 2 files |
| Phase 03-composite-navigation P01 | 7m | 4 tasks | 16 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Phase 1: Use `undecorated=true` WITHOUT `transparent=true` — prevents Win11 EXCEPTION_ACCESS_VIOLATION (issue #3757); glass effect simulated via gradient
- Phase 1: Glass effect implemented in single `drawBehind` block to avoid overdraw and iGPU performance collapse
- Phase 1: Library module uses `compose.desktop.common` (not `currentOs`) to keep JAR platform-neutral
- Phase 3: AeroTitleBar placed in Phase 3 because it requires `FrameWindowScope` — only available in real window context
- [Phase 01-foundation]: Gradle 8.14.3 used (not 8.10.2) — cached locally, fully CMP 1.7.3 compatible
- [Phase 01-foundation]: Kotlin 2.1.21 / CMP 1.7.3 confirmed as actual published versions (RESEARCH.md cited non-existent 2.3.21/1.10.3)
- [Phase 01-foundation]: :library uses compose.desktop.common (platform-neutral JAR); :showcase uses compose.desktop.currentOs (native binary)
- [Phase 01-foundation]: AeroColorScheme drops 6 mordred domain tokens: connectionActive, connectionInactive, executionHighlight, logBackground, timeStampBackground, cardContent — application-specific, no meaning in a general UI library
- [Phase 01-foundation]: bodySmall retained on AeroTypography despite UI-SPEC removal — Plan 03 Material3 bridge maps MaterialTheme.typography.bodySmall to it; removing breaks the bridge
- [Phase 01-foundation]: @Immutable runtime reflection test replaced with isData check — Compose @Immutable uses BINARY retention (CLASS level), not visible at runtime; isData is correct runtime proxy
- [Phase 01-foundation]: defaultFactory pivot: ProvidableCompositionLocal.defaultFactory not public in CMP 1.7.3/Kotlin 2.1.21 — AeroThemeTest uses smoke tests per plan fallback
- [Phase 01-foundation]: JAVA_HOME must be JDK 17 (Gradle toolchain) for all Gradle invocations — Java 25 version string breaks Kotlin compiler JavaVersion.parse()
- [Phase 01-foundation]: Showcase omits explicitApi() — showcase build.gradle.kts has no constraint, public modifiers omitted
- [Phase 01-foundation]: No transparent/undecorated on Window — Win11 EXCEPTION_ACCESS_VIOLATION blocker remains in effect for Phase 1
- [Phase 02-atomic-components]: compose.animation added to library/build.gradle.kts — animateColorAsState is in androidx.compose.animation not .core
- [Phase 02-atomic-components]: AeroPasswordField uses inline BasicTextField (not AeroTextField wrapper) to support visualTransformation parameter
- [Phase 02-atomic-components]: AeroFilePicker uses inline glassSurface Box for Обзор button — avoids Wave 1 compile-order dependency on AeroOutlinedButton
- [Phase 02-01-buttons]: indication=null on AeroIconButton.clickable — hover/pressed drawn manually, suppresses M3 ripple double-effect
- [Phase 02-01-buttons]: AeroToolbarDefaults is a top-level object (not companion) — idiomatic Kotlin for composable helpers
- [Phase 02-01-buttons]: hover overlay via drawWithContent inside Button content Box (not outer wrapper) — avoids M3 Button shape clipping issues
- [Phase 02-atomic-components]: AeroComboBox popup uses PopupProperties(focusable=false) to preserve text field keyboard focus; keyboard nav inside popup not supported in v1
- [Phase 02-atomic-components]: AeroPopupPositionProvider is internal (same module) — Phase 3 AeroPopover/AeroContextMenu/AeroMenuBar can reuse it directly without duplication
- [Phase 02-04-range/list]: Color.Unspecified sentinel for AeroBadge defaults — AeroTheme.colors not accessible as Kotlin default arg outside Composable context
- [Phase 02-04-range/list]: AeroSlider tooltip centred above slider (not tracking thumb x) — Phase 3 KDoc enhancement documented
- [Phase 02-atomic-components]: animateColorAsState is in androidx.compose.animation (not .core) in CMP 1.7.3
- [Phase 02-atomic-components]: AeroChip uses primary.copy(0.25f) selected bg and cardBackground.copy(0.3f) unselected — direct MordredChip port
- [Phase 02-atomic-components]: Color.Transparent allowed in AeroSegmentedControl — named constant not hex literal
- [Phase 02-06-showcase]: M3 Button LocalMinimumInteractiveComponentSize defaults to 48dp — must set to Dp.Unspecified so hover overlay Box(matchParentSize) aligns with visual button bounds, not inflated touch target
- [Phase 02-06-showcase]: Dropdown popups need two-layer .background() (opaque base + translucent tint) — single 0xCC alpha is insufficient for readability over glassy parent surfaces
- [Phase 02-06-showcase]: All themes' buttonHover must be alpha-tinted whites (<=0x40 alpha) — fully opaque overlay paints over button text; Classic theme was incorrectly opaque
- [Phase 02-06-showcase]: AeroFilePicker keeps path field fully editable; Browse button is convenience-only to populate the field via native dialog
- [Phase 02-06-showcase]: AeroNumberSpinner scroll wheel via pointerInput PointerEventType.Scroll — works whether or not the field has keyboard focus (Mordred-style spinner pattern)
- [Phase 03-composite-navigation]: AeroPopupSide enum + auto-flip provider serves DRP, OVL-03/04/07 simultaneously — single source of truth for anchored popup placement
- [Phase 03-composite-navigation]: AeroDropdownPopup uses AeroScrollArea wrapping inner Column (preserves vertical padding); AeroTextArea uses AeroScrollBar overlay (BasicTextField needs its own verticalScroll modifier order)
- [Phase 03-composite-navigation]: Stub-test compile-reachability uses Class.forName(...Kt) instead of ::Composable — Kotlin Compose plugin disallows function references to @Composable functions

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 1: Validate whether Win11 `undecorated+transparent` crash is fixed in CMP 1.10.3 (may have been patched — test in Phase 1)
- Phase 1: Decide Haze vs. gradient-only glass rendering after seeing demo output
- Phase 3: AeroTitleBar + native Aero Snap (HTCAPTION) requires spike — `WindowDraggableArea` does not pass HTCAPTION to OS; this is a documented limitation

## Session Continuity

Last session: 2026-04-28T13:02:02.167Z
Stopped at: Completed 03-01-PLAN.md (popup foundation + scrollbar primitives + AeroTheme wiring + retrofits)
Resume file: None
