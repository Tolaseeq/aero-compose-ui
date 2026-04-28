---
phase: 03-composite-navigation
plan: "02"
subsystem: ui
tags: [compose, jetpack-compose, kotlin, glass-modifier, containers, aero-card, aero-panel, aero-divider, aero-group-box]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: "Modifier.glassEffect / Modifier.glassPanel, AeroTheme.colors (borderDefault, labelText, background), AeroTheme.typography.label"
  - phase: 03-01
    provides: "containers/ package layout, Class.forName(...Kt) compile-smoke pattern"
provides:
  - "AeroCard composable: glass-styled card (CNT-01) with shadow + rounded corners + content slot"
  - "AeroPanel composable: section-level glass panel (CNT-02) with no shadow / no border"
  - "AeroDivider composable: 1.dp horizontal/vertical divider (CNT-03) using borderDefault color"
  - "AeroGroupBox composable: labeled rounded border around content (CNT-04), label inset on top edge"
  - "Four compile-reachability tests for the new composables"
affects:
  - "03-08 showcase wiring (ContainersSection.kt will mount these four)"
  - "03-04 overlays (AeroDialog/AeroPopover may delegate to AeroPanel/glassEffect for chrome)"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Container-as-thin-wrapper: each AeroCard / AeroPanel is just `Box(modifier.glassXxx().padding(...)) { content() }`"
    - "AeroGroupBox label-on-border carve-out: `Modifier.background(colors.background)` over the top border line breaks the stroke visually"
    - "AeroDivider mode switch via `Modifier` builder branch (no nested composables) — compiles to a single Box"

key-files:
  created:
    - "library/src/main/kotlin/com/mordred/aero/components/containers/AeroCard.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/containers/AeroPanel.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/containers/AeroDivider.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/containers/AeroGroupBox.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/containers/AeroCardTest.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/containers/AeroPanelTest.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/containers/AeroDividerTest.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/containers/AeroGroupBoxTest.kt"
  modified: []

key-decisions:
  - "AeroCard public API: (modifier, cornerRadius=8.dp, elevation=4.dp, padding=16.dp, content) — defaults match glassEffect defaults; padding is a single Dp (not PaddingValues) for ergonomic typical use"
  - "AeroPanel cornerRadius default = 0.dp — panels are typically full-width sections; matches glassPanel default"
  - "AeroDivider thickness param exposed (default 1.dp) — vertical=false is horizontal default; uses fillMaxWidth/fillMaxHeight so caller constrains length via the modifier"
  - "AeroGroupBox label carve-out via Modifier.background(colors.background) — opaque-background on the floating Text paints over the top border, producing the Windows-Forms inset-label look without measuring text width"
  - "Compile-smoke tests use Class.forName(\"...Kt\") instead of ::Composable function reference (Compose plugin disallows function references to @Composable functions) — applied per existing AeroScrollAreaTest decision"

patterns-established:
  - "Thin-wrapper-over-glass-modifier: containers expose an opt-in `padding` Dp, default 16.dp, then unwrap the slot inside a single Box — no internal layout assumptions"
  - "Class.forName(...Kt) compile-reachability: applied uniformly across all four new tests; deviates from the plan's `::Component` test code which would fail to compile"

requirements-completed: [CNT-01, CNT-02, CNT-03, CNT-04]

# Metrics
duration: 11min
completed: 2026-04-28
---

# Phase 3 Plan 02: Static Containers Summary

**Four glass-styled container composables (AeroCard, AeroPanel, AeroDivider, AeroGroupBox) implemented as thin wrappers over Phase 1 glass modifiers — completing the CNT-01..06 requirement set.**

## Performance

- **Duration:** ~11 min
- **Started:** 2026-04-28T13:05:05Z
- **Completed:** 2026-04-28T13:16:32Z
- **Tasks:** 2
- **Files created:** 8 (4 main + 4 test)
- **Files modified:** 0

## Accomplishments

- AeroCard (CNT-01): public composable, `Modifier.glassEffect` with shadow + rounded corners + content slot
- AeroPanel (CNT-02): public composable, `Modifier.glassPanel` for section-level backgrounds (no shadow/border)
- AeroDivider (CNT-03): 1.dp Box with `borderDefault` color, `vertical: Boolean` switch + `thickness: Dp` override
- AeroGroupBox (CNT-04): rounded 1.dp border with floating label — label `background(colors.background)` carves out the top border behind the label glyph (Windows-Forms inset-label idiom)
- Compile-reachability tests for all four; full container test suite (`AeroScrollAreaTest`, `AeroScrollBarTest`, `AeroCardTest`, `AeroPanelTest`, `AeroDividerTest`, `AeroGroupBoxTest`) passes 6/6

## Task Commits

Each task was committed atomically:

1. **Task 1: AeroCard + AeroPanel (CNT-01, CNT-02)** — `1429800` (feat)
2. **Task 2: AeroDivider + AeroGroupBox (CNT-03, CNT-04)** — `21af5e0` (feat)

**Plan metadata:** _filled in by final docs commit below_

## Files Created/Modified

Created:
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroCard.kt` — `public fun AeroCard` (glassEffect wrapper, shadow + rounded corners + content slot)
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroPanel.kt` — `public fun AeroPanel` (glassPanel wrapper, panel-level background)
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroDivider.kt` — `public fun AeroDivider` (1.dp horizontal/vertical line in borderDefault)
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroGroupBox.kt` — `public fun AeroGroupBox` (rounded border + label inset on top edge)
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroCardTest.kt` — `aeroCardCompileSmoke`
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroPanelTest.kt` — `aeroPanelCompileSmoke`
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroDividerTest.kt` — `aeroDividerCompileSmoke`
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroGroupBoxTest.kt` — `aeroGroupBoxCompileSmoke`

Modified: none.

## Decisions Made

- **AeroDivider thickness param exposed** (in addition to the plan's `vertical: Boolean`) — caller can produce thicker rules without re-implementing; default remains 1.dp to match Aero/Win-Classic.
- **AeroGroupBox label carve-out via opaque-background Text** — the plan code already used `.background(colors.background)`, but I confirmed during implementation this is the right approach (no `drawWithContent` measure-and-paint, no `Layout` custom measurement; the Text background simply paints over the underlying border line).
- **Compile-smoke tests use `Class.forName("...Kt")` not `::Composable`** — applied per the existing decision logged for Plan 03-01: "Stub-test compile-reachability uses Class.forName(...Kt) instead of ::Composable — Kotlin Compose plugin disallows function references to @Composable functions." The plan's verbatim test code (`val ref = ::AeroCard`) would not compile; using the `Class.forName` form is the previously-validated pattern.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 — Bug] Replaced `::Component` test refs with `Class.forName("...Kt")`**
- **Found during:** Task 1 (AeroCardTest authoring)
- **Issue:** The plan's task action verbatim says `val ref = ::AeroCard` for each compile-smoke test. The Compose compiler plugin rejects function references to `@Composable` functions, so the tests would fail to compile.
- **Fix:** Used `Class.forName("com.mordred.aero.components.containers.AeroCardKt")` — same pattern already established in `AeroScrollAreaTest.kt` (Plan 03-01). Applied uniformly to all four new tests (Card / Panel / Divider / GroupBox).
- **Files modified:** all four `*Test.kt` files in this plan
- **Verification:** `./gradlew :library:test --tests "com.mordred.aero.components.containers.*"` runs all 6 container tests with 0 failures
- **Committed in:** `1429800` (Task 1) and `21af5e0` (Task 2)

**2. [Rule 3 — Blocking] Set JAVA_HOME to JDK 17 for gradle invocations**
- **Found during:** Task 1 verification (first `./gradlew` call)
- **Issue:** `JAVA_HOME` was unset; gradle wrapper picked up the system default (JDK 25) which the toolchain rejects with "25.0.2" error.
- **Fix:** Exported `JAVA_HOME=C:/Users/1/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2` for each gradle invocation (matches the locked decision: "JAVA_HOME must be JDK 17 (Gradle toolchain) for all Gradle invocations").
- **Files modified:** none
- **Verification:** `./gradlew :library:compileKotlin` exits 0 with JAVA_HOME=JDK17
- **Committed in:** N/A (env-only change, not a file)

---

**Total deviations:** 2 auto-fixed (1 bug fix, 1 blocking)
**Impact on plan:** Both auto-fixes are mechanical fidelity to existing locked decisions. No scope creep, no behavioral change to the four new composables.

## Issues Encountered

- **Pre-existing uncommitted parallel work blocked the test compile.** Before Task 1 verification, `git status` showed parallel work for Plans 03-04 / 03-05 / 03-06 (overlay/AeroDialog, overlay/AeroToastHost, navigation/AeroTitleBar, navigation/AeroMenuBar, modified `AeroToastHostStateTest.kt` referring to types not yet shipped, and gradle wiring for `kotlinx-coroutines-test`). These are completely outside Plan 03-02 scope but their test compile errors fail the whole `:library:compileTestKotlin` task.
- **Resolution:** Stashed the parallel work into two `git stash` entries (one for tracked-file modifications, one for untracked files) for the duration of Task 1 / Task 2 verification, then `git stash pop` restored everything after both task commits landed. Plan 03-02 commits never include any of the parallel-work files; the working tree post-plan is identical to the working tree pre-plan plus the four new container files / four new tests / one SUMMARY.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- CNT-01..04 ship; together with CNT-05 (AeroScrollArea) + CNT-06 (AeroScrollBar) from Plan 03-01, the entire containers package is now complete.
- Plan 03-08 (showcase wiring) can mount `ContainersSection.kt` referencing all four new components plus AeroScrollArea/AeroScrollBar.
- Plan 03-04 overlays (AeroDialog, AeroPopover) MAY delegate their chrome rendering to `Modifier.glassEffect` directly — they do not need to wrap AeroCard, but they could; either is a clean composition.
- No new theme tokens required; no new dependencies added; no popup/dialog logic introduced (per plan scope).

---
*Phase: 03-composite-navigation*
*Completed: 2026-04-28*

## Self-Check: PASSED

Verified all 4 main files + 4 test files exist on disk:
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroCard.kt` ✓
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroPanel.kt` ✓
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroDivider.kt` ✓
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroGroupBox.kt` ✓
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroCardTest.kt` ✓
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroPanelTest.kt` ✓
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroDividerTest.kt` ✓
- `library/src/test/kotlin/com/mordred/aero/components/containers/AeroGroupBoxTest.kt` ✓

Verified both task commits exist in git log:
- `1429800` ✓
- `21af5e0` ✓
