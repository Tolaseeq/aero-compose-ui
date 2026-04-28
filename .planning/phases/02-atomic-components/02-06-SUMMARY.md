---
phase: 02-atomic-components
plan: "06"
subsystem: showcase
tags: [showcase, wiring, integration, segmented-control, visual-checkpoint, BTN-01, BTN-02, BTN-03, BTN-04, INP-01, INP-02, INP-03, INP-04, INP-05, INP-06, SEL-01, SEL-02, SEL-03, SEL-04, SEL-05, DRP-01, DRP-02, RNG-01, RNG-02, LST-01, LST-02]

dependency_graph:
  requires:
    - phase: 02-atomic-components plans 01-05
      provides: ButtonsSection, InputSection, SelectionSection, DropdownSection, RangeSection, ListSection composables
    - phase: 01-foundation
      provides: AeroTheme, AeroColorScheme, glass modifiers
  provides:
    - ShowcaseApp wired to all 6 Phase 2 sections (0 PlaceholderSection calls remain)
    - ThemeSwitcher rebuilt on AeroSegmentedControl (SEL-05 proven publicly exported)
    - Visual checkpoint approval — all 21 Phase 2 components verified interactive across 3 themes
  affects:
    - Phase 3 (composite components) — showcase pattern for adding new sections is established

tech_stack:
  added: []
  patterns:
    - "Section composables self-title — no Column+Text wrapper needed in ShowcaseApp (FoundationSection exception)"
    - "ThemeOption wrapper data class for AeroSegmentedControl<T> when option type lacks readable toString"
    - "LocalMinimumInteractiveComponentSize = Dp.Unspecified on M3 Button to match hover overlay to visual bounds"
    - "Two-layer .background() for popup opacity: opaque base + translucent tint over glassy parent surface"
    - "buttonHover token must use alpha-tinted white (<=0x40) — fully opaque overlay paints over button text"
    - "Scroll wheel on spinner via pointerInput { awaitPointerEventScope { PointerEventType.Scroll } }"

key_files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/ThemeSwitcher.kt

key_decisions:
  - "M3 Button LocalMinimumInteractiveComponentSize defaults to 48dp, inflating layout bounds beyond visual button — set to Dp.Unspecified so hover overlay matches button shape exactly"
  - "Dropdown popups require two-layer .background() (opaque base + translucent tint) — single 0xCC alpha is insufficient over glassy parent surfaces"
  - "All themes' buttonHover must be alpha-tinted whites (<=0x40 alpha), never fully opaque — full-opacity overlay paints over button text"
  - "AeroFilePicker path field is fully editable; Browse button is a convenience to populate the field (not the only input path)"
  - "AeroNumberSpinner scroll wheel via pointerInput PointerEventType.Scroll — works whether or not the field has keyboard focus"

patterns-established:
  - "ShowcaseApp section wiring: import section composable, call it directly — each section renders its own title Text"
  - "ThemeOption wrapper: when AeroSegmentedControl<T> needs human-readable labels and T lacks readable toString, wrap in a simple data class(name, value)"

requirements-completed: [BTN-01, BTN-02, BTN-03, BTN-04, INP-01, INP-02, INP-03, INP-04, INP-05, INP-06, SEL-01, SEL-02, SEL-03, SEL-04, SEL-05, DRP-01, DRP-02, RNG-01, RNG-02, LST-01, LST-02]

duration: "~45 min (including 4 fix rounds during visual checkpoint)"
completed: "2026-04-28"
---

# Phase 2 Plan 06: ShowcaseApp Wiring Summary

**ShowcaseApp wired to all 6 Phase 2 sections and ThemeSwitcher rebuilt on AeroSegmentedControl, then visually verified across AeroBlue/AeroDark/Classic with 4 rounds of fixes before checkpoint approval.**

## Performance

- **Duration:** ~45 min (including visual checkpoint fix rounds)
- **Started:** 2026-04-28
- **Completed:** 2026-04-28
- **Tasks:** 3 (Task 1: ThemeSwitcher rewrite, Task 2: ShowcaseApp wiring, Task 3: Visual checkpoint — APPROVED)
- **Files modified:** 2 core + several library fixes across fix rounds

## Accomplishments

- ThemeSwitcher.kt replaced all Material3 Button row code with AeroSegmentedControl, proving SEL-05 is publicly exported and functional in a real use-case
- ShowcaseApp.kt now invokes all 6 section composables (ButtonsSection, InputSection, SelectionSection, DropdownSection, RangeSection, ListSection) — zero PlaceholderSection calls remain for Phase 2 categories
- Visual checkpoint approved: all 21 Phase 2 requirement IDs (BTN-01..04, INP-01..06, SEL-01..05, DRP-01..02, RNG-01..02, LST-01..02) confirmed visible and interactive across all three themes

## Task Commits

Each task was committed atomically:

1. **Task 1: Rewrite ThemeSwitcher with AeroSegmentedControl** - `9d575f0` (feat)
2. **Task 2: Wire all 6 sections into ShowcaseApp** - `ba64c20` (feat)
3. **Task 3: Visual checkpoint** — APPROVED after 4 rounds of fixes (see Deviations)

## Fix Commits During Visual Checkpoint Cycle

| Commit | Description |
|--------|-------------|
| `9b5fe83` | AeroFilePicker path field made fully editable (not read-only) |
| `b46422a` | Hover overlay initial attempt to cover full button surface |
| `55c16e7` | Replace emoji icons (search, password) with monochrome Canvas glyphs |
| `ceede83` | Dropdown popup width matches anchor + opaque background (first attempt) |
| `ff62389` | Button hover fix via LocalMinimumInteractiveComponentSize = Dp.Unspecified (correct final fix) |
| `4f9cbce` | AeroNumberSpinner mouse wheel scroll increments/decrements value |
| `124b303` | Dropdown popup two-layer opaque background (correct final fix) |
| `c7d7fed` | Classic theme buttonHover made translucent so button text remains visible |

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — All 6 section imports added; 5 PlaceholderSection calls replaced with ButtonsSection(), InputSection(), SelectionSection(), DropdownSection(), RangeSection(), ListSection(); ListSection added as new 6th category
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ThemeSwitcher.kt` — Replaced Material3 Button row with AeroSegmentedControl; added private ThemeOption data class wrapper

## Decisions Made

- **LocalMinimumInteractiveComponentSize must be Dp.Unspecified on M3 Button:** M3 defaults to 48dp minimum, inflating the layout touch target beyond the visual button border. The hover overlay Box uses matchParentSize() which matches the inflated layout bounds, causing the hover glow to extend well beyond the visible button. Setting the local to Dp.Unspecified restores 1:1 alignment between visual button and hover overlay.

- **Dropdown popup two-layer background:** A single `background(color.copy(alpha = 0.80f))` over a glassy parent surface still shows through enough to make text unreadable. The correct pattern is two `.background()` modifiers: first a fully opaque neutral base, then a tinted translucent layer on top.

- **buttonHover token transparency:** The Classic theme had buttonHover set to an opaque color, painting over button labels entirely. All themes must use alpha-tinted whites (alpha <= 0x40 / 0.25f) for the hover overlay — the effect should be a subtle brightening, not a cover.

- **AeroFilePicker editability:** The path field is kept as a fully editable BasicTextField. The Browse button populates it via the native file dialog, but the user can also type or paste paths directly. Read-only was rejected as it prevents keyboard-only workflows.

- **AeroNumberSpinner scroll wheel:** Implemented via `pointerInput { awaitPointerEventScope { loop { val e = awaitPointerEvent(); if (e.type == PointerEventType.Scroll) ... } } }`. This intercepts scroll events regardless of keyboard focus state, matching the Mordred-style spinner behavior.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] AeroFilePicker path field was read-only**
- **Found during:** Task 3 (visual checkpoint, INP-06 verification)
- **Issue:** The path text field did not accept keyboard input — only the Browse button could populate it
- **Fix:** Changed field to a fully editable BasicTextField so users can type or paste paths directly
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/input/AeroFilePicker.kt`
- **Committed in:** `9b5fe83`

**2. [Rule 1 - Bug] Hover overlay extended beyond visible button bounds**
- **Found during:** Task 3 (visual checkpoint, BTN-01..02 hover verification)
- **Issue:** The hover glow box sized to M3 Button's inflated 48dp minimum touch target, not the 30dp visual button
- **Fix:** Set `CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified)` around the Button call
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt`, `AeroOutlinedButton.kt`
- **Committed in:** `b46422a` (initial attempt), `ff62389` (correct final fix)

**3. [Rule 1 - Bug] Emoji icons in password/search fields caused font rendering issues**
- **Found during:** Task 3 (visual checkpoint, INP-03, INP-05 verification)
- **Issue:** 👁 and 🔍 emoji rendered at inconsistent sizes and with color font glyphs that looked out of place in the Aero monochrome aesthetic
- **Fix:** Replaced with Canvas-drawn monochrome vector glyphs matching the rest of the icon vocabulary
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt`, `AeroSearchField.kt`
- **Committed in:** `55c16e7`

**4. [Rule 1 - Bug] Dropdown popup showed through glassy background (two separate sub-issues)**
- **Found during:** Task 3 (visual checkpoint, DRP-01..02 verification)
- **Issue A:** Popup width was narrower than the trigger anchor — visually misaligned  
- **Issue B:** Single-layer 0xCC alpha background was insufficient over glassy parent — text unreadable
- **Fix:** Width clamped to anchor width via custom popup position provider; background changed to two-layer (opaque base + translucent tint)
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdownMenu.kt` (or equivalent popup layer)
- **Committed in:** `ceede83` (width + first bg attempt), `124b303` (correct two-layer bg)

**5. [Rule 1 - Bug] AeroNumberSpinner did not respond to mouse wheel**
- **Found during:** Task 3 (visual checkpoint, INP-04 verification)
- **Issue:** Scroll wheel over the spinner had no effect — only the arrow buttons worked
- **Fix:** Added `pointerInput` block with `awaitPointerEventScope` listening for `PointerEventType.Scroll` events to increment/decrement the value
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt`
- **Committed in:** `4f9cbce`

**6. [Rule 1 - Bug] Classic theme buttonHover was opaque, covering button text**
- **Found during:** Task 3 (visual checkpoint, BTN-01..04 Classic theme verification)
- **Issue:** Classic theme's buttonHover color token was set to a fully opaque value, painting completely over button labels on hover
- **Fix:** Changed Classic buttonHover to a translucent white (alpha ~0x30) matching the other themes' intent
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` (Classic preset)
- **Committed in:** `c7d7fed`

---

**Total deviations:** 6 auto-fixed (all Rule 1 — bugs discovered during visual verification)
**Impact on plan:** All fixes required for Phase 2 acceptance criteria. No scope creep — every fix addresses a directly observable correctness failure in the running showcase.

## Issues Encountered

The visual checkpoint required 4 fix rounds before approval. Each round revealed issues that were invisible at compile time (layout sizing, color rendering, input behavior). The iterative fix-and-relaunch cycle is expected for showcase integration plans — compile gates cannot catch visual/behavioral regressions.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

**Phase 2 is visually complete.** A developer can run `./gradlew :showcase:run` and see every Phase 2 component (21 total) live and interactive across all three Aero themes.

**Ready for Phase 3:**
- AeroPopupPositionProvider is marked `internal` — Phase 3 AeroPopover, AeroContextMenu, and AeroMenuBar can reuse it directly from the same module
- All interaction state patterns (hover overlay, focus ring, pressed scale, disabled alpha) are established in InteractionStates.kt and copied consistently across all Phase 2 components
- The showcase section pattern is established — Phase 3 plans follow the same composable + ShowcaseApp wiring structure

**Known deferred items for Phase 3:**
- AeroSlider tooltip tracks slider position but not thumb x — tooltip is centered above the slider track; Phase 3 KDoc enhancement documents the limitation
- AeroTitleBar + native Aero Snap (HTCAPTION) requires a spike — WindowDraggableArea does not pass HTCAPTION to OS (documented CMP limitation)

---
*Phase: 02-atomic-components*
*Completed: 2026-04-28*
