---
phase: 03-composite-navigation
plan: "08"
subsystem: ui
tags: [compose-multiplatform, kotlin, showcase, integration, theme-hoisting, manual-checkpoint]

# Dependency graph
requires:
  - phase: 03-composite-navigation
    provides: all 19 Phase 3 components (Plans 03-01 through 03-07)
provides:
  - ContainersSection / OverlaysSection / NavigationSection showcase wiring
  - currentScheme state hoisted to Main.kt so AeroTitleBar shares the active theme
  - AeroToastHost mounted at ShowcaseApp root
  - User-approved visual verification for the entire v1 library
affects: [v1-release]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - State-hoisting pattern for window-chrome theme propagation: caller (Main.kt) owns currentScheme + onSchemeChange and passes both into ShowcaseApp; AeroTitleBar (mounted in Main.kt's FrameWindowScope) reads from the same AeroTheme wrapper as the content.
    - AeroToastHost mounted as a sibling overlay inside ShowcaseApp's root Box (not wrapped in scrolling content) so toasts overlay everything.

key-files:
  created:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/ContainersSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/OverlaysSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/NavigationSection.kt
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt (accept currentScheme, mount toast host, call 3 new sections)
    - showcase/src/main/kotlin/com/mordred/showcase/Main.kt (hoist currentScheme, add 1.dp window border)

key-decisions:
  - "AeroDialog switched from DialogWindow to Window so the same FrameWindowScope-based AeroTitleBar used by the main showcase window can be mounted at the dialog top — drag, minimize, close, and gradient strip all match the main window. Trade-off: Window is non-modal at the OS level; for showcase use that's acceptable."
  - "Popups (AeroDropdown, AeroMenuBar, AeroDropdownPopup) place widthIn AND heightIn on the same Box (mirrors AeroComboBox). Pushing heightIn one level deeper via AeroScrollArea was leaving the outer Box height-unbounded during the first popup-content measurement and causing a one-frame full-window flash."
  - "AeroPopupPositionProvider parks the placeholder pass off-screen (windowSize + popupContentSize) when popupContentSize ≥ windowSize so Compose Desktop's first-measurement quirk no longer renders a window-sized rectangle at (0,0)."
  - "AeroMenuBar measures every menu's labels via rememberTextMeasurer and passes the widest + 16.dp padding as anchorWidth, clamped to 120-320.dp — popup width tracks content without any post-composition layout passes."
  - "AeroPopover and AeroDrawer share the same translucent fill: panelBackground × 0.7 alpha + top-half glassHighlight gradient. Popover keeps a 1.dp border (it floats); Drawer is borderless (touches window edges)."
  - "glassEffect dropped Modifier.shadow — its clip + the trailing clip(shape) were rendering a doubled inner stripe. background(brush, shape) + border(width, color, shape) gives the Aero look without the artifact."
  - "glassPanel paints solid panelBackground first then a top-55% glassHighlight → Transparent gradient, matching the Aero shimmer pattern that glassSurface already uses."
  - "AeroGroupBox label sits at outer Box's TopStart; the bordered child Box is pushed down by half the label glyph height so the label centers on the border line."
  - "AeroContextMenu translates pointer-event local coords to window-local via onGloballyPositioned + localToWindow before passing to AeroCursorPositionProvider — popup now opens at the cursor instead of (0, 0)."
  - "AeroDrawer renders inside a Popup with FullWindowPositionProvider so it covers the host window regardless of mount point; focusable=true wires Esc dismissal."
  - "AeroScrollArea renders the scrollbar only when state.maxValue > 0 — fillMaxHeight on a permanently-visible bar was forcing the surrounding Box to claim heightIn's max even when content fit."

patterns-established:
  - "Section composable pattern: each Phase 3 section follows the existing 140.dp label + variants-Row template, keeping visual rhythm with Phase 2 sections."
  - "State-hoisting for window chrome: when a UI library exposes a custom titlebar that needs to share state with content, lift the state to the Window-host caller and let both AeroTheme and the section accept it as parameters."
  - "Translucent floating-surface fill recipe: panelBackground × 0.7 alpha + top-half glassHighlight gradient — reusable for any new floating panel that needs Aero glass without becoming opaque."

requirements-completed: [CNT-01, CNT-02, CNT-03, CNT-04, CNT-05, CNT-06, OVL-01, OVL-02, OVL-03, OVL-04, OVL-05, OVL-06, OVL-07, OVL-08, NAV-01, NAV-02, NAV-03, NAV-04, NAV-05]

# Metrics
duration: ~5h (4 rounds of UAT + final approval)
completed: 2026-04-28
---

# Phase 3 Plan 08: Showcase Integration + Final Visual Checkpoint Summary

**Three showcase sections (Containers / Overlays / Navigation) wire all 19 Phase 3 components, currentScheme state is hoisted to Main.kt so the AeroTitleBar shares the active theme, AeroToastHost is mounted at root, and four iterations of UAT-driven fixes (popup flicker, glass-effect rendering, dialog chrome, drawer/popover translucency) deliver an "approved" verdict on the full v1 library.**

## Performance

- **Duration:** ~5h end-to-end (initial wiring + 4 UAT rounds + final approval)
- **Started:** 2026-04-28 (showcase wiring kicked off)
- **Completed:** 2026-04-28 (user approval)
- **Tasks:** 5 (Tasks 1-4 wired; Task 5 manual checkpoint approved)

## Accomplishments
- 3 new showcase sections covering all 19 Phase 3 components
- ShowcaseApp refactored to receive currentScheme/onSchemeChange and mount AeroToastHost at root
- Main.kt hoists currentScheme state and wraps everything in AeroTheme so the AeroTitleBar gradient updates with the theme switcher
- 1.dp window border added to main window content for a visible floating-window edge
- AeroDialog rebuilt on Window (FrameWindowScope) so the real AeroTitleBar (drag, minimize, close, gradient strip) replaces a custom 1-button strip
- AeroDropdown rewritten to use Popup directly (mirroring AeroComboBox), eliminating the placeholder-pass flicker
- AeroMenuBar uses rememberTextMeasurer to compute popup width per-menu, hugging the widest item
- glassEffect simplified to background+border (no shadow, no doubled rect); glassPanel gained the Aero shimmer; AeroPopover and AeroDrawer share a unified translucent fill

## Task Commits

1. **Task 1: Hoist currentScheme + mount AeroToastHost** — `c1a0629`
2. **Task 2: ContainersSection** — `e361fca`
3. **Task 3: OverlaysSection** — `a92a463`
4. **Task 4: NavigationSection + 3-section wiring** — `4ca054d`
5. **Task 5: Manual visual checkpoint** — approved after 4 rounds of UAT fixes

UAT-driven fix commits (in order):
- `233d906` AeroScrollArea fillMaxSize → fillMaxWidth
- `a8e09c9` 7 round-1 fixes (Dialog/ContextMenu/Dropdown/GroupBox/ScrollArea/Drawer/MenuBar)
- `38c5aa2` round 2 (conditional scrollbar, sharp scrollbar corners, dropdown padding, SubcomposeLayout, popover opaque, dialog Window-based)
- `c53095f` round 3 (popup flicker mitigation, glass restoration, dialog titlebar, window borders)
- `d5ee616` park unmeasured popup off-screen
- `79b9238` rewrite AeroDropdown to use Popup directly (matches AeroComboBox)
- `c13fedf` glassEffect: background+border instead of drawBehind
- `294cc4d` glassEffect: shadow clip=false
- `312cff5` glassEffect: remove shadow entirely
- `d656c1b` glassPanel: top-half highlight gradient
- `ee754b6` AeroPopover/AeroDrawer: unified translucent fill

## Files Created/Modified
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ContainersSection.kt`
- `showcase/src/main/kotlin/com/mordred/showcase/sections/OverlaysSection.kt`
- `showcase/src/main/kotlin/com/mordred/showcase/sections/NavigationSection.kt`
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt`
- `showcase/src/main/kotlin/com/mordred/showcase/Main.kt`
- (UAT rounds touched 11 library files — see commit log)

## Decisions Made
See `key-decisions` in frontmatter.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking] Library deviations driven by UAT feedback**
- **Found during:** Task 5 manual checkpoint (4 rounds of issues)
- **Issue:** Several Phase 3 components had visual or interaction bugs only visible at integration time (popup flicker, glass-effect inner stripe, opaque popover, drawer not dismissing, etc.).
- **Fix:** 11 follow-up commits across 03-01 (AeroScrollArea), 03-04 (AeroDialog), 03-05 (AeroPopover/AeroContextMenu), 03-07 (AeroMenuBar), Phase 1 (GlassModifiers), and the showcase Main.kt. Library tests stayed green throughout.
- **Verification:** User-approved visual checkpoint covering all 3 themes × all 19 Phase 3 components.

---

**Total deviations:** 11 commits of UAT-driven fixes (all touching library + showcase, none breaking earlier plans' tests).
**Impact on plan:** Plan 03-08's scope expanded to absorb cross-cutting Phase 3 polish that surfaced only when components were composed together. No scope creep beyond Phase 3 surface area.

## Issues Encountered
- **Compose Desktop popup placeholder-pass quirk:** calculatePosition is invoked once with popupContentSize ≈ windowSize before the content actually measures. The naive auto-flip + clamp logic rendered a window-sized rectangle at (0,0) for one frame. Resolved by parking the placeholder pass off-screen.
- **DialogWindow vs Window for modal dialogs:** DialogWindow gives DialogWindowScope which has no FrameWindowScope.WindowDraggableArea extension. Manual drag via detectDragGestures introduced lag. Switched to Window for AeroDialog so the existing AeroTitleBar (with WindowDraggableArea) is reusable; trade-off documented in key-decisions.
- **Modifier.shadow rendering an inner stripe in glassEffect:** Even with clip=false, the shadow modifier's outline interaction with the trailing clip(shape) produced a visible inset rectangle. Removed shadow entirely from glassEffect; if a shadow is needed in a specific surface, callers can add it explicitly.

## Next Phase Readiness
- All 19 Phase 3 requirement IDs satisfied. Ready for goal-backward verification.
- v1 milestone reaches its scope endpoint with this plan; gsd-verifier should confirm.

---
*Phase: 03-composite-navigation*
*Completed: 2026-04-28*
