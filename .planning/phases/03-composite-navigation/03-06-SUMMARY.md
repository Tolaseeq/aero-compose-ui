---
phase: 03-composite-navigation
plan: "06"
subsystem: ui
tags: [compose-multiplatform, kotlin, coroutines, suspend, snapshot-state, material-icons-extended, notifications]

requires:
  - phase: 01-foundation
    provides: AeroTheme tokens (colors.primary, colors.error, colors.onSurface), AeroTypography (bodyLarge), glassEffect/glassPanel modifiers
  - phase: 02-atomic-components
    provides: AeroIconButton (24.dp size), AeroOutlinedButton (toast action button)
  - phase: 03-composite-navigation/01
    provides: Wave-0 stub tests AeroToastHostStateTest + AeroBannerKindTest (upgraded by this plan)

provides:
  - "OVL-05 AeroToast: stacked auto-dismissing toast notifications"
  - "AeroToastHostState with mutableStateListOf-backed stack (max 5) + suspendCancellableCoroutine showToast"
  - "AeroToastHost composable with per-toast LaunchedEffect dismiss timer keyed by data.id"
  - "AeroToastDuration enum (Short=4s, Long=10s, Indefinite=Long.MAX_VALUE) + AeroToastResult enum + AeroToastAction"
  - "OVL-06 AeroNotificationBanner: in-flow inline banner (no host)"
  - "AeroBannerKind enum (Info, Warning, Error, Success) with paired Material outlined icons"

affects: [showcase-integration, future-snackbar-replacements, dialog-confirmation-flows]

tech-stack:
  added:
    - "kotlinx-coroutines-test (testImplementation; was already referenced in build.gradle.kts but missing from libs.versions.toml — fixed)"
  patterns:
    - "suspendCancellableCoroutine + onDismissed callback for showXxx() suspend semantics"
    - "key(data.id) + LaunchedEffect(data.id) to keep per-item state stable across stack reorders"
    - "Bottom-end Box.fillMaxSize().padding(16.dp) overlay positioning"
    - "Icons.Outlined (NOT Filled) consistent with AeroAlertKind precedent"

key-files:
  created:
    - "library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastDuration.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHostState.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/overlay/AeroNotificationBannerTest.kt"
  modified:
    - "library/src/test/kotlin/com/mordred/aero/components/overlay/AeroToastHostStateTest.kt (Wave-0 stub upgraded to 4 lifecycle tests)"
    - "library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt (Wave-0 stub upgraded to 5 enum tests)"
    - "gradle/libs.versions.toml (added kotlinx-coroutines-test alias)"

key-decisions:
  - "[Phase 03-06] AeroToastHostState uses suspendCancellableCoroutine to mirror Material3 SnackbarHostState.showSnackbar suspend semantics — caller awaits result"
  - "[Phase 03-06] Stack overflow policy = evict-OLDEST (FIFO) with AeroToastResult.Evicted; chosen over evict-newest so high-priority recent toasts stay visible"
  - "[Phase 03-06] Per-toast LaunchedEffect(data.id) inside key(data.id) — each toast carries its own dismiss timer that survives stack reorders without restarting"
  - "[Phase 03-06] AeroToastData public payload with internal constructor + internal onDismissed callback — public surface readable from AeroToastHost; mutation only via AeroToastHostState.showToast/dismiss"
  - "[Phase 03-06] AeroBannerKind icon mapping uses Icons.Outlined (CheckCircle for Success) — consistent with AeroAlertKind precedent"
  - "[Phase 03-06] Banner accent at border 0.6 alpha + glassPanel background — matches Aero glass aesthetic without overwhelming the in-flow body text"
  - "[Phase 03-06] kotlinx-coroutines-test alias added to libs.versions.toml — build.gradle.kts already referenced libs.kotlinx.coroutines.test (latent dangling alias from a prior plan)"

patterns-established:
  - "suspending API with stack: showXxx() returns a Result enum; data carries internal onDismissed callback; remove() triggers callback"
  - "MAX_STACK_SIZE companion constant + while-loop eviction in show...() before append"
  - "key(stableId) wrapper around Per-item LaunchedEffect to scope side-effects per logical item"

requirements-completed: [OVL-05, OVL-06]

duration: 25min
completed: 2026-04-28
---

# Phase 03 Plan 06: Notifications (Toast + Banner) Summary

**Stacked auto-dismissing toasts with suspendCancellableCoroutine + per-toast LaunchedEffect timer (OVL-05) plus in-flow notification banner with 4 kind variants (OVL-06).**

## Performance

- **Duration:** 25 min
- **Started:** 2026-04-28T13:05:30Z
- **Completed:** 2026-04-28T13:30:43Z
- **Tasks:** 2 (both TDD: RED -> GREEN)
- **Files created:** 6 (5 main + 1 test)
- **Files modified:** 3 (2 test stubs upgraded + libs.versions.toml)

## Accomplishments

- **OVL-05 AeroToast** ships with full state model: `AeroToastHostState.showToast(message, duration, action)` suspends the caller until the toast is dismissed, returning `AeroToastResult.{Dismissed, ActionPerformed, Evicted}`. Stack capped at 5 (`MAX_STACK_SIZE`); a 6th toast evicts the oldest.
- **AeroToastHost** composable renders all stacked toasts at Bottom-end with 16dp window inset; `key(data.id)` + `LaunchedEffect(data.id)` give every toast its own dismiss timer that survives stack reorders without restarting.
- **OVL-06 AeroNotificationBanner** ships as an in-flow Row composable (no host): caller decides where it appears and toggles visibility. 4 kind variants (Info/Warning/Error/Success) drive icon + 1dp accent border at 0.6 alpha over `glassPanel`.
- **Two Wave-0 stubs upgraded:** `AeroToastHostStateTest` (4 lifecycle tests) and `AeroBannerKindTest` (5 enum/icon mapping tests).
- **Latent build issue fixed:** `library/build.gradle.kts` referenced `libs.kotlinx.coroutines.test` but the alias was missing from `libs.versions.toml` — a dangling reference from a prior plan that would have broken any future test invocation needing coroutines-test. Added the alias.

## Task Commits

Each task was committed atomically (TDD: RED+GREEN combined per the plan's authoring style):

1. **Task 1: AeroToastDuration + AeroToastHostState + AeroToastHost (OVL-05) + filled-in AeroToastHostStateTest** — `70da289` (feat)
2. **Task 2: AeroBannerKind enum + AeroNotificationBanner (OVL-06) + filled-in AeroBannerKindTest** — `51d2117` (feat)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastDuration.kt` — Short/Long/Indefinite duration enum + AeroToastResult + AeroToastAction
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHostState.kt` — class with mutableStateListOf-backed `toasts`, `showToast` suspend fun, `MAX_STACK_SIZE = 5`, internal `dismiss(id, result)`
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt` — `AeroToastHost(state, modifier)` composable rendering Box(Bottom-end).Column with `key(data.id)` + `LaunchedEffect(data.id) { delay; dismiss }`
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt` — enum with 4 entries; `.icon` property mapping each to an `Icons.Outlined` ImageVector
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt` — `AeroNotificationBanner(kind, text, onDismiss?, actions?, modifier)` composable
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroToastHostStateTest.kt` — 4 tests using runTest + async + yield: append, evict-oldest at 6th, dismiss-resumes-with-Dismissed, MAX_STACK_SIZE constant
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt` — 5 tests: 4 entries set + 4 icon mapping checks
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroNotificationBannerTest.kt` — Class.forName reachability smoke test (composable can't be referenced via `::`)
- `gradle/libs.versions.toml` — added `kotlinx-coroutines-test` alias (already referenced by build.gradle.kts)

## Decisions Made

- **Stack overflow = evict-oldest**: chosen over evict-newest so the most recent (likely most relevant) toast stays visible. Evicted callers resume with `AeroToastResult.Evicted` so they can detect rejection.
- **Public AeroToastData with internal constructor + internal onDismissed**: public read surface for `AeroToastHost`, mutation gated behind `AeroToastHostState`.
- **`key(data.id)` + `LaunchedEffect(data.id)` over Box-level timer**: ensures per-toast timers survive when the stack reorders (e.g., on eviction); a single host-level timer would restart for every change.
- **Banner is Row (not Column)** with optional `actions: @Composable RowScope.() -> Unit`: matches the typical inline banner pattern in Material/Fluent design and lets callers add custom buttons next to the close button without nested layouts.
- **`Class.forName(...Kt)` for AeroNotificationBanner reachability test**: per the established Phase-3 convention (Plan 03-01 decision: function references to `@Composable` functions are disallowed by the Compose compiler).
- **Icons.Outlined (CheckCircle for Success)**: matches AeroAlertKind precedent and avoids importing `Icons.Filled` for a single component.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added `kotlinx-coroutines-test` alias to libs.versions.toml**
- **Found during:** Task 1 (test compile)
- **Issue:** `library/build.gradle.kts` already had `testImplementation(libs.kotlinx.coroutines.test)` from a prior commit (eae46d2 / earlier), but `libs.versions.toml` did NOT declare the alias — a dangling reference that would fail Gradle resolution as soon as any test file imported `kotlinx.coroutines.test.runTest`. The plan anticipated this case and called it out in the action notes.
- **Fix:** Added `kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }` (1.10.2, matching the existing `kotlinx-coroutines-core` version).
- **Files modified:** `gradle/libs.versions.toml`
- **Verification:** `./gradlew :library:test --tests "com.mordred.aero.components.overlay.AeroToastHostStateTest"` exits 0; all 4 toast tests pass.
- **Committed in:** `70da289` (Task 1 commit)

**2. [Rule 3 - Blocking] Repeated daemon stalls in CI required `./gradlew --stop` between iterations**
- **Found during:** Task 1 (initial compile attempts)
- **Issue:** Kotlin daemon left stale Persistent caches after each compile error, manifesting as `LazyStorage` "registration stack trace" exceptions in subsequent runs.
- **Fix:** Stopped daemon (`./gradlew --stop`) and removed `library/build/kotlin` once at the start of Task 1; subsequent runs were stable.
- **Files modified:** none (transient)
- **Verification:** Subsequent gradle invocations completed without daemon errors.
- **Committed in:** N/A (no source changes)

---

**Total deviations:** 2 auto-fixed (1 dangling-alias blocking, 1 transient daemon issue).
**Impact on plan:** Both auto-fixes were necessary to unblock the test build. No scope creep — the plan explicitly anticipated the coroutines-test dependency need.

## Issues Encountered

- **Parallel-execution interference (no remediation needed):** During this plan's execution, other plans (03-02, 03-03, 03-04 with AeroAlertKind/AeroDialog/AeroDrawer/AeroResizeHandles) were committed to `master` in parallel by sibling executor agents. STATE.md as of plan start said "03-01 complete" but the repo was actually at HEAD `21af5e0/89e1d12/580a6fd/eae46d2/...`. This produced phantom "untracked files" (`AeroAlertKind.kt`, `AeroAlertDialog.kt`, `AeroDrawer.kt`, `AeroMenuBar.kt`, `ResizeHandles.kt`, etc.) that appeared and disappeared between `git status` invocations. Resolved by restraining staging to only this plan's files (`git add` with explicit paths). My commits (`70da289`, `51d2117`) include only OVL-05 + OVL-06 sources/tests + the dangling-alias fix.
- **Initial RED step proved by compile failure (not test failure):** Tests couldn't even compile until the GREEN source files existed. Documented in TDD reference as the canonical "compile-error == RED" pattern.

## Self-Check: PASSED

All claimed artifacts verified to exist on disk and reference real commits:
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastDuration.kt` — FOUND
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHostState.kt` — FOUND
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt` — FOUND
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt` — FOUND
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt` — FOUND
- Commit `70da289` — FOUND on master (`feat(03-06): add AeroToast, AeroToastHostState, AeroToastHost (OVL-05)`)
- Commit `51d2117` — FOUND on master (`feat(03-06): add AeroBannerKind + AeroNotificationBanner (OVL-06)`)
- All 10 tests pass (4 ToastHostState + 5 BannerKind + 1 NotificationBanner reachability).

## Next Phase Readiness

- OVL-05 + OVL-06 ready for showcase wiring (Plan 03-08).
- `AeroToastHostState` is the public surface; future use in showcase: `val state = remember { AeroToastHostState() }; AeroToastHost(state); scope.launch { state.showToast("Saved") }`.
- No pending blockers introduced by this plan.

---
*Phase: 03-composite-navigation*
*Plan: 06*
*Completed: 2026-04-28*
