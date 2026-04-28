---
phase: 03-composite-navigation
plan: "04"
subsystem: ui
tags: [compose-multiplatform, kotlin, dialog, drawer, modal-overlay, dialogwindow, win11]

# Dependency graph
requires:
  - phase: 02-atomic-components
    provides: AeroButton, AeroOutlinedButton, AeroIconButton (used by AeroAlertDialog buttons)
  - phase: 03-composite-navigation
    provides: glassSurface modifier (used by AeroDialog inner container + AeroDrawer panel)
provides:
  - public AeroDialog (OVL-01): OS-level DialogWindow with 3-slot API (title/content/buttons)
  - public AeroAlertKind enum (Info, Warning, Error, Question) with Material Outlined icons
  - public AeroAlertDialog (OVL-02): kind-keyed icon + accent + default buttons
  - public AeroDrawer + AeroDrawerSide (OVL-08): sliding side panel with 220ms slide + scrim
  - compose.materialIconsExtended dependency wired into :library
  - 4 overlay test classes (AeroDialogTest, AeroAlertKindTest expanded, AeroAlertDialogTest, AeroDrawerTest)
affects: [03-05-popover-tooltip-context, 03-06-toast-banner, 03-07-menubar-breadcrumb-tabs, 03-08-showcase]

# Tech tracking
tech-stack:
  added:
    - androidx.compose.material.icons.outlined.* (Info, Warning, Error, HelpOutline)
    - androidx.compose.ui.window.DialogWindow
    - androidx.compose.ui.window.rememberDialogState
    - compose.materialIconsExtended Gradle dependency
  patterns:
    - DialogWindow with undecorated=true / transparent=false (Win11 EXCEPTION_ACCESS_VIOLATION rule, repeat from Phase 1)
    - Glass containment via Modifier.glassSurface inside the DialogWindow body, NOT via window transparency
    - Esc dismissal via onPreviewKeyEvent on either DialogWindow or modal Box wrapper
    - Closed-but-composed pitfall: drawer composables early-return when fully closed AND animation done so they do not consume pointer input
    - @Composable accentFor helper resolves theme tokens that the enum cannot reach (Color companion sentinels for Warning only)
    - Stub-test compile-reachability via Class.forName("...Kt") (Kotlin Compose plugin disallows function refs to @Composable functions; pattern preserved from Plan 03-01)

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDialog.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertDialog.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDrawer.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroDialogTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertDialogTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroDrawerTest.kt
  modified:
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt (Wave-0 stub -> 5 real assertions)
    - library/build.gradle.kts (add compose.materialIconsExtended)

key-decisions:
  - "Win11 rule grep-test: source files ban transparent=true literal so DialogWindow never crashes Win11; AeroDialog enforces undecorated=true / transparent=false explicitly."
  - "AeroAlertDialog wraps AeroDialog by delegation rather than reimplementing DialogWindow plumbing; gives single source of truth for the Win11 rule."
  - "AeroDrawer uses an in-window full-screen Box + scrim (not DialogWindow). Plan rationale: animated slide animation needs to render inside the parent window, not a separate OS window."
  - "AeroDrawer closed-but-composed early-return ('if (!open && offsetFraction == 1f) return') prevents the drawer from eating pointer events when invisible — critical for v1 ergonomics."
  - "Warning kind uses fixed Color(0xFFFFA726) because the locked 23-token AeroColorScheme has no warning slot; Info/Question delegate to colors.primary, Error to colors.error."
  - "compose.materialIconsExtended is a hard dependency for the four AeroAlertKind icons (Outlined.Error and Outlined.HelpOutline are NOT in icons-core). Plan 03-04 added it to :library implementation deps."
  - "AeroDrawerTest uses Class.forName probe (matching the pattern set in Plan 03-01) instead of the plan's `::AeroDrawer` reference because Compose plugin disallows function refs to @Composable functions."

patterns-established:
  - "Modal overlay scrim: in-window full-screen Box with Color.Black.copy(alpha=animatedAlpha) and clickable(indication=null, onClick=onDismissRequest). Pattern reused by future overlays (popover/context menu may need similar)."
  - "Animated drawer offset: offset { IntOffset((signedOffset * drawerWidth.toPx()).roundToInt(), 0) } — pixel offset driven by an animateFloatAsState fraction, signed by side."
  - "Modal dialog API shape: 3-slot (title, content, buttons RowScope), right-aligned button row via Arrangement.End, glass inner container — base for any future modal in the library."

requirements-completed: [OVL-01, OVL-02, OVL-08]

# Metrics
duration: 25m
completed: 2026-04-28
---

# Phase 3 Plan 04: Modal-Overlay Components (Dialog + AlertDialog + Drawer) Summary

**OVL-01 AeroDialog (3-slot DialogWindow modal with Esc), OVL-02 AeroAlertDialog with 4 kinds (Info/Warning/Error/Question driving icon + accent + default buttons), and OVL-08 AeroDrawer (sliding Start/End side panel with 220ms scrim animation and closed-but-composed pointer-input pitfall handled) — all three honoring the Win11 transparent=false rule.**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-04-28T13:05:19Z
- **Completed:** 2026-04-28T13:30:37Z
- **Tasks:** 3 (all completed)
- **Files created:** 7 (4 main + 3 new tests)
- **Files modified:** 2 (AeroAlertKindTest stub -> real assertions; library/build.gradle.kts adds materialIconsExtended)

## Accomplishments

- OVL-01 AeroDialog ships with the locked Win11 rule (`undecorated = true`, `transparent = false`) enforced both in source and via grep-friendly literals; Esc dismissal wired through `onPreviewKeyEvent`; glass containment via `Modifier.glassSurface(cornerRadius = 8.dp)` on an inner Box (not via window transparency).
- OVL-02 AeroAlertDialog ships with 4 kinds. Info/Question -> `colors.primary` accent + OK only; Error -> `colors.error` accent + OK only; Warning -> fixed orange `Color(0xFFFFA726)` accent + OK only; Question additionally renders a Cancel `AeroOutlinedButton` followed by an OK `AeroButton`.
- OVL-08 AeroDrawer ships with `AeroDrawerSide.{Start,End}`, animated slide via `animateFloatAsState` (220ms LinearEasing), animated scrim alpha, scrim-click + Esc dismissal, and the load-bearing closed-but-composed early-return: `if (!open && offsetFraction == 1f) return`.
- AeroAlertKindTest promoted from Plan 03-01 Wave-0 stub to 5 real assertions: enum has exactly 4 entries, each maps to its expected `Icons.Outlined.*` icon (Info, Warning, Error, HelpOutline).
- `compose.materialIconsExtended` added to `:library` dependencies — `Icons.Outlined.Error` and `Icons.Outlined.HelpOutline` are NOT available in `material-icons-core`; this was a missing dep in the plan and was auto-fixed during Task 2.

## Task Commits

Each task was committed atomically:

1. **Task 1: AeroDialog (OVL-01)** - `89e1d12` (feat) — DialogWindow modal + Esc + glass surface
2. **Task 2: AeroAlertKind + AeroAlertDialog (OVL-02)** - `580a6fd` (feat) — 4 kinds, kind-keyed buttons, materialIconsExtended dep
3. **Task 3: AeroDrawer (OVL-08)** - `eae46d2` (feat) — scrim + slide + Esc + closed-but-composed early-return
4. **Follow-up: AeroAlertKindTest real assertions** - `29cf6ee` (test) — Wave-0 stub -> 5 real assertions (lost from Task 2 commit due to workspace state churn)

**Plan metadata:** _will land with the docs commit_ (this SUMMARY.md + STATE.md + ROADMAP.md update)

## Files Created/Modified

**Main sources (created):**
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDialog.kt` - OVL-01 DialogWindow modal with Esc + glass surface inner container
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt` - public enum + icon mapping + internal staticAccent
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertDialog.kt` - OVL-02 wrapper around AeroDialog + accentFor @Composable helper
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDrawer.kt` - OVL-08 + AeroDrawerSide enum

**Test sources (created):**
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroDialogTest.kt` - compile-only smoke (DialogWindow is OS-level)
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertDialogTest.kt` - compile-only smoke
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroDrawerTest.kt` - 2 assertions (enum + Class.forName probe)

**Modified:**
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt` - Wave-0 stub -> 5 real assertions
- `library/build.gradle.kts` - add `implementation(compose.materialIconsExtended)` dep

## Decisions Made

- **Single Win11 rule, single dialog primitive.** AeroAlertDialog delegates to AeroDialog rather than reimplementing DialogWindow flags. Future modal-flavored composables should follow the same pattern.
- **No Color companion for theme tokens on AeroAlertKind.** The enum can only return `Color(0xFFFFA726)` as a fixed value; theme tokens (primary/error) require @Composable scope, so a sibling `accentFor(kind: AeroAlertKind): Color` @Composable helper resolves them. Same pattern as AeroBadge's Color.Unspecified sentinel from Plan 02-04.
- **AeroDrawer uses in-window scrim, not DialogWindow.** Slide animation must render inside the parent window — a separate OS window would defeat the visual continuity. Trade-off: AeroDrawer cannot be Esc-dismissed if focus is outside its scope; mitigated by `onPreviewKeyEvent` on the outer Box.
- **Closed-but-composed early-return is mandatory.** Without `if (!open && offsetFraction == 1f) return`, the drawer's transparent scrim Box still consumes pointer events over the main UI when "closed". This is the v1 idiomatic mitigation; future v2 may use AnimatedVisibility with `LaunchedEffect`-driven removal.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added `compose.materialIconsExtended` dep**
- **Found during:** Task 2 (Implement AeroAlertKind enum)
- **Issue:** The plan referenced `Icons.Outlined.Error` and `Icons.Outlined.HelpOutline`, but `:library` only pulled `material-icons-core` transitively via `compose.material3`. icons-core only ships Info and Warning in `outlined/`; Error and HelpOutline live in `material-icons-extended`.
- **Fix:** Added `implementation(compose.materialIconsExtended)` to `library/build.gradle.kts`.
- **Files modified:** `library/build.gradle.kts`
- **Verification:** `./gradlew :library:compileKotlin` clean run goes green; AeroAlertKindTest compiles and all 5 assertions pass.
- **Committed in:** `580a6fd` (Task 2 commit)

**2. [Rule 1 - Bug] AeroDrawerTest.aeroDrawerCompileSmoke probe via Class.forName instead of `::AeroDrawer`**
- **Found during:** Task 3 (Implement AeroDrawer)
- **Issue:** The plan's `<action>` block called `val ref = ::AeroDrawer; assertNotNull(ref)`. The Kotlin Compose compiler plugin disallows function references to `@Composable` functions; this is a known constraint already documented as a Plan 03-01 decision in STATE.md.
- **Fix:** Replaced with `Class.forName("com.mordred.aero.components.overlay.AeroDrawerKt")` reflective probe — same pattern Plan 03-01 used for its 5 stub composable tests.
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroDrawerTest.kt`
- **Verification:** Test runs; both AeroDrawerTest assertions pass.
- **Committed in:** `eae46d2` (Task 3 commit)

**3. [Rule 3 - Blocking] AeroAlertKindTest follow-up commit**
- **Found during:** Verification after Task 2 commit
- **Issue:** AeroAlertKindTest was rewritten from Wave-0 stub to 5 real assertions inside Task 2 and `git add`ed alongside the other Task 2 files, but the workspace state churn (linter / external process repeatedly reverting unstaged changes) snapped the test file back to its stub form between the Write tool call and the staging snapshot. The commit landed without the test promotion.
- **Fix:** Re-ran the Write, immediately staged + committed as `29cf6ee` follow-up (`test(03-04): promote AeroAlertKindTest from Wave-0 stub to 5 real assertions`).
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt`
- **Verification:** AeroAlertKindTest now reports 5 tests, all passing.
- **Committed in:** `29cf6ee` (follow-up commit)

---

**Total deviations:** 3 auto-fixed (2 Rule 3 blocking, 1 Rule 1 bug)
**Impact on plan:** All three deviations were necessary for the plan to compile, test, and ship — none of them changed the plan's intended behavior. The dep addition was a missing-dep oversight in the plan; the Class.forName probe matches an established Plan 03-01 pattern; the follow-up commit recovered work the workspace lost.

## Issues Encountered

- **Workspace churn during execution:** an external process (linter or IDE-driven cleanup) repeatedly reverted unstaged file changes during Task 2 — `AeroAlertKind.kt`, `AeroAlertDialog.kt`, and `AeroAlertDialogTest.kt` had to be re-written after disappearing from the filesystem; `AeroAlertKindTest.kt` reverted to a stub between Write and `git add` (handled by deviation #3 above). Workaround: write file -> immediate verify-with-Read or short-cycle `git add`.
- **Pre-existing test file referencing forward symbols:** `AeroToastHostStateTest.kt` (committed as a "Wave-0 stub" by Plan 03-01 but actually contains real assertions referencing `AeroToastHostState`/`AeroToastResult`/`AeroToastDuration`) was untracked-modified in the workspace; not my plan's concern. The companion main files (`AeroToastDuration.kt`, `AeroToastHost.kt`, `AeroToastHostState.kt`) are also untracked in the workspace and resolve those references — they are pre-staged in-flight work for Plan 03-07. They were intentionally NOT touched by my commits.
- **JDK 17 toolchain not on PATH:** `JAVA_HOME` was empty and `java -version` reported JDK 25. Gradle toolchain discovered the cached Adoptium 17 at `C:/Users/1/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2/` automatically once `JAVA_HOME` was exported per shell — same setup documented in STATE.md Decisions ("JAVA_HOME must be JDK 17"). No code change required.

## Next Phase Readiness

- Plans 03-05 (anchored popover/tooltip/context menu), 03-06 (toast/banner), 03-07 (menubar/breadcrumb/tabs), and 03-08 (showcase) all unblocked by Plan 03-04. None of them depend on dialog/drawer plumbing — modal overlays are a parallel branch in Wave 2.
- The Win11 transparent rule is now battle-tested in the `:library` overlay package; future modal composables should grep their source files against the rule the same way (no `transparent\s*=\s*true` literal anywhere).
- The compose.materialIconsExtended dep is now available library-wide; future plans needing additional Material icons (toast, banner, breadcrumb chevrons, tabs close icons) can use them without further build.gradle.kts changes.

## Self-Check: PASSED

**Files verified on disk:**
- library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDialog.kt — FOUND
- library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt — FOUND
- library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertDialog.kt — FOUND
- library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDrawer.kt — FOUND
- library/src/test/kotlin/com/mordred/aero/components/overlay/AeroDialogTest.kt — FOUND
- library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt — FOUND
- library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertDialogTest.kt — FOUND
- library/src/test/kotlin/com/mordred/aero/components/overlay/AeroDrawerTest.kt — FOUND

**Commits verified in git log:**
- 89e1d12 (Task 1) — FOUND
- 580a6fd (Task 2) — FOUND
- eae46d2 (Task 3) — FOUND
- 29cf6ee (AeroAlertKindTest follow-up) — FOUND

**Test pass counts (last verified run):**
- AeroDialogTest: 1/1 pass
- AeroAlertKindTest: 5/5 pass
- AeroAlertDialogTest: 1/1 pass
- AeroDrawerTest: 2/2 pass
- Total: 9/9 pass

---
*Phase: 03-composite-navigation*
*Completed: 2026-04-28*
