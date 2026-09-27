---
phase: 22-native-window-behavior-release-3-2-0
plan: 07
subsystem: windows-native
tags: [snap-01, btn-02, ver-11, hit-test, wndproc, jna, compose-desktop]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-02 WndProc subclass + hardcoded spike classifier; 22-05 frame removal + taskbar-aware maximize; 22-01 probe harness (V11 set)"
provides:
  - "HitTestRegionRegistry: per-window copy-on-write HitTestSnapshot behind AtomicReference + WeakHashMap WindowRegionsDirectory (pure, lock-free)"
  - "classifyHitTest: pure precedence classifier (maximize > min/close/interactive > caption), resizeBandPx 0-safe until Plan 11"
  - "AeroTitleBar publishes live regions via onGloballyPositioned; WindowDraggableArea only on the non-Windows / install-failed path (single native drag path)"
  - "Live proof: 4/4 title-bar V11 checks PASS at 1200x800 AND after SetWindowPos resize to 900x600; 22-05 maximize checks unregressed"
affects: [22-09, 22-11, 22-12, 22-15]

tech-stack:
  added: []
  patterns:
    - "Immutable-snapshot bridge: Compose publishes copy-on-write snapshot through AtomicReference; WndProc does a plain volatile read, never locks, never touches Compose state (ARCHITECTURE Pattern 2)"
    - "ScreenToClient inside the WM_NCHITTEST handler — window position never enters hit-test math (Anti-Pattern 2 avoided)"
    - "Region publishing as a conditional modifier element (no-op Modifier when regions == null) so the legacy composition stays structurally identical"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistry.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestClassification.kt
    - .captures/22-regions/Invoke-RegionsCheck.ps1 (git-ignored capture artifact)
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/WndProcSupport.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "Existing HTCLIENT/HTCAPTION/HTMAXBUTTON constants reused; resize-band codes (HTLEFT..HTBOTTOMRIGHT) added to Win32Interop.kt for Plan 11 instead of declaring duplicate HT_CLIENT-style constants in the new pure file"
  - "BTN-02 click proof stays with Plan 15 real input: posted WM_LBUTTONDOWN/UP did not reach Compose within 5s (message-level proxy unreached, recorded not faked); the HTCLIENT classification that admits real clicks is proven by V11-HT-MIN-BOUNDARY / V11-HT-CLOSE-BOUNDARY"
  - "SNAP-01/SNAP-03/SNAP-04/BTN-02/VER-11 all stay Pending in REQUIREMENTS.md: no clause set is fully proven by this plan (22-05 precedent)"

requirements-completed: []

duration: ~45min
completed: 2026-09-27
---

# Phase 22 Plan 07: Live hit-test regions + single native drag path — Summary

**Windows now hit-tests the title bar from AeroTitleBar's own live layout (published copy-on-write through an AtomicReference, classified by a pure function) — the spike's hardcoded rectangles are gone, answers follow a 900x600 resize, and `WindowDraggableArea` survives only on the non-Windows / install-failed path.**

## Performance

- **Duration:** ~45 min
- **Tasks:** 3/3 (pure region core; AeroTitleBar wiring; live check)
- **Files modified:** 7 (2 created library files, 4 modified library files, 1 NOTES) + 1 git-ignored probe script
- `./gradlew :library:test` — AERO_TEST_COUNT total=541 green after both code tasks; `:showcase:compileKotlin` green with its call site untouched
- Evidence: `.captures/22-regions/live.json`, launch log `.captures/22-regions/logs/`

## Accomplishments

- `HitTestRegionRegistry.kt`: `TitleBarRole`, immutable `HitTestSnapshot`, per-window copy-on-write registry (`publishRole`/`publishInteractive` with null-removal, `newInteractiveId`, reserved `LEADING_INTERACTIVE_ID`), and `WindowRegionsDirectory` (WeakHashMap under synchronized — no JNA, safe on every OS).
- `HitTestClassification.kt`: pure `classifyHitTest(snapshot, x, y, clientWidth, clientHeight, maximized, resizeBandPx)` with the planned precedence; `resizeBandPx` accepted and 0-safe (bands are Plan 11). Both WndProcs now classify from `regions.snapshot()` with `ScreenToClient` inside the handler; `classifySpikeTitleRow` deleted.
- `AeroTitleBar`: extracted `TitleBarRow`; on the Windows native path the caption row, each `TitleBarButton` and the `leading` slot publish live bounds via `onGloballyPositioned` (leading as interactive, PITFALLS 19); a `DisposableEffect` clears all regions on dispose. `WindowDraggableArea` remains in exactly one place — the non-Windows / install-failed branch (`acquire` now catches install exceptions, traces `event=install-failed`, returns null → legacy path, T-22-02).
- Live check (22-NOTES.md "Live regions (22-07)"): V11-HT-CAPTION / V11-HT-MAX / V11-HT-MIN-BOUNDARY / V11-HT-CLOSE-BOUNDARY PASS at 1200x800 and again after a SetWindowPos resize to 900x600 (LAYOUT-FOLLOW PASS 4/4) — live layout, not hardcoded rects. 22-05's V11-HT-MAXIMIZED-TOP / V11-MAX-WORKAREA / V11-AUTOHIDE-EDGE still PASS. Edges/corners/min-size FAIL unchanged (Plan 11 scope). Foreground never taken; all launched processes stopped.

## Task Commits

1. **Task 1: Pure region core — snapshot, registry, directory, classifier** - `b2a8bb3` (feat)
2. **Task 2: AeroTitleBar publishes live regions; WindowDraggableArea only on the legacy path** - `646eb38` (feat)
3. **Task 3: Live check — regions follow layout; BTN-02 message-level proxy** - `dc15933` (docs)

## Files Created/Modified

- `library/.../internal/windows/HitTestRegionRegistry.kt` — snapshot/registry/directory bridge (pure)
- `library/.../internal/windows/HitTestClassification.kt` — pure hit-test classifier
- `library/.../internal/windows/WndProcSupport.kt` — spike classifier deleted
- `library/.../internal/windows/AeroWndProc.kt` — both procs classify from the live snapshot
- `library/.../internal/windows/NativeWindowChromeRegistry.kt` — registry captured at install; install-failure fallback
- `library/.../internal/windows/Win32Interop.kt` — resize-band HT codes declared for Plan 11
- `library/.../components/navigation/AeroTitleBar.kt` — region publishing + single native drag path
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md` — "Live regions (22-07)"

## Decisions Made

- Reused the existing `HTCLIENT`/`HTCAPTION`/`HTMAXBUTTON` constants and placed the new resize-band codes in `Win32Interop.kt` (where all HT codes live) rather than declaring duplicate `HT_CLIENT`-style constants as the plan's prose suggested — one naming style, no duplication; `const val` inlines, so the pure files stay JNA-free.
- BTN-02's ordinary-click proof is deferred to Plan 15's real-input session (see Deviations/Issues) — the plan explicitly sanctions this when posted messages do not reach Compose.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] CMP 1.12.0 `boundsInWindow()` signature change**
- **Found during:** Task 2 (AeroTitleBar compile)
- **Issue:** The plan's interfaces assume the no-arg `LayoutCoordinates.boundsInWindow()`; in CMP 1.12.0 it is a deprecated extension forwarding to `boundsInWindow(clipBounds: Boolean = true)`, and the no-arg reference does not resolve without opting into the deprecation.
- **Fix:** Added a private `clientBounds()` helper calling `boundsInWindow(clipBounds = true)` (byte-identical semantics to the pre-1.12 no-arg form) and used it at all three publish sites.
- **Files modified:** library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
- **Verification:** `:library:compileKotlin :showcase:compileKotlin` green; live check PASS at both window sizes
- **Committed in:** `646eb38` (part of task commit)

**2. [Rule 2 - Correctness] AeroTitleBar KDoc updated despite the plan's "KDoc unchanged" note**
- **Found during:** Task 2
- **Issue:** The plan asked to keep the KDoc unchanged, but two of its sentences became factually false with this change ("The whole row is wrapped in WindowDraggableArea"; "Aero Snap support requires JNI/WinAPI and is explicitly v2+"). Shipping KDoc that contradicts the code is a correctness defect; REL-07's broader public-API doc pass remains Plan 17's.
- **Fix:** Minimal KDoc correction: the Windows native path and the fallback are described, and the Snap limitation text is scoped to the legacy path.
- **Files modified:** library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
- **Committed in:** `646eb38`

**3. [Mechanics] Task 3 pass A ran WITHOUT `-SkipMaximize`**
- **Found during:** Task 3
- **Issue:** The plan's parenthetical says to add `-SkipMaximize` "per F9", but F9 actually settles the opposite — probe-driven `SC_MAXIMIZE` never takes the foreground on this machine (measured 3x in 22-01), so skipping is not required.
- **Fix:** Pass A ran the full V11 set including the maximize cycle, which re-proved 22-05's passing checks (V11-HT-MAXIMIZED-TOP, V11-MAX-WORKAREA, V11-AUTOHIDE-EDGE all PASS) against the new classifier — the orchestrator's do-not-regress rule. Pass B (resized window) used `-SkipMaximize` to keep measurement geometry undisturbed. `foregroundTaken=False` confirmed.
- **Committed in:** `dc15933` (recorded in 22-NOTES.md)

---

**Total deviations:** 3 (1 blocking, 1 correctness, 1 mechanics)
**Impact on plan:** All necessary for correctness or mandated by standing rules; no scope creep — edges/corners/bands remain Plan 11's, button-interaction bridge remains Plan 09's.

## Assumption Drift (advisory)

- Plan interfaces assumed `boundsInWindow()` + `roundToInt()` as-is from the Compose API; in CMP 1.12.0 the no-arg form is deprecated and the `clipBounds` parameter is required (handled by deviation 1 — semantics unchanged, `clipBounds = true` IS the old behavior).
- The published caption rect starts after the row's 8dp horizontal padding (the `onGloballyPositioned` is appended after `padding(horizontal = 8.dp)` exactly as the plan specifies), so the outermost 8px strips of the title row answer HTCLIENT rather than HTCAPTION — the only observable difference vs the spike's full-row HTCAPTION, visible only in the already-FAILING corner/edge probe points that Plan 11 owns. No V11 title-bar check regressed.

## Issues Encountered

- **BTN-02 message-level proxy unreached:** posted `WM_LBUTTONDOWN`/`WM_LBUTTONUP` at the `min` point to the `SunAwtCanvas` child produced no `minimized=true` reporter line within 5s. This is the plan's explicitly anticipated outcome ("do not fake it") — a synthesized PostMessage click is not the real input path; recorded in 22-NOTES.md and `live.json`, click proof deferred to Plan 15. Nothing was left minimized; no restore was needed.

## User Setup Required

None — no external service configuration required.

## Threat Model Coverage

- **T-22-19 (torn read / deadlock):** mitigated as designed — immutable data-class snapshots swapped through `AtomicReference.updateAndGet`; the WndProc performs one volatile read per `WM_NCHITTEST`, never locks, never touches Compose state.
- **T-22-02 (install failure DoS):** mitigated as designed — `acquire` catches install exceptions, traces `event=install-failed`, returns null; `AeroTitleBar` flips `nativeFailed` and composes today's `WindowDraggableArea` path.

## Known Stubs

None — every published region is wired to live layout data.

## Next Phase Readiness

- Plan 09 (max-button interaction bridge, C5): the maximize rect already answers HTMAXBUTTON; its hover/press/toggle state bridge is the next native-path piece.
- Plan 11 (resize bands + min-size floor): `classifyHitTest` already accepts `resizeBandPx`/`maximized`; HTLEFT..HTBOTTOMRIGHT codes are declared; the V11 edge/corner/min-size FAILs are its GREEN targets.
- Plan 15 (real input): owns BTN-02's actual click proof and the reveal-on-hover confirmation.

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-27*

## Self-Check: PASSED
