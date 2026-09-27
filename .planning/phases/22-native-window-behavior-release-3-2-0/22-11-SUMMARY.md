---
phase: 22-native-window-behavior-release-3-2-0
plan: 11
subsystem: windows-native
tags: [win-02, win-06, ver-11, d-01, resize, wm_getminmaxinfo, nchittest]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-07 live regions + classifyHitTest with the resizeBandPx seam; 22-08 narrow fixture + V11-N checks; 22-10 churn-hardened install and owned-message sets"
provides:
  - "Edge/corner resize bands in classifyHitTest (corner = band of two adjacent edges, bands first, none while maximized) with resizeBandPx = max(DPI-scaled system sizing frame, CMP's 8 dp resizer), recomputed per WM_NCHITTEST at GetDpiForWindow DPI"
  - "WM_GETMINMAXINFO min-track floor: forwarded first, ptMinTrackSize raised to the D-01 default (320x240 dp at the window's DPI) only when the app set no AWT minimum; the library never writes window.minimumSize"
  - "MinimumSize.kt: pure D-01 resolution for the native px path (resolveMinimumTrackSizePx) and the Compose dp path (resolveComposeMinimumDp)"
  - "NativeChromeStatus: per-window Compose State<Boolean> native-chrome-active (pure, no JNA), written by the registry at install/reuse/uninstall/install-failure"
  - "AeroResizeHandles: shouldComposeResizeHandles gate (Floating && !nativeChromeActive) + per-drag-lambda resolveComposeMinimumDp — one resize path per platform"
  - "Live GREEN: main V11 18/18 PASS (edges/corners/client-boundary/minsize), narrow 7/8 (only the Plan 12 marked RED remains)"
affects: [22-12, 22-13, 22-15, 22-16, 22-17]

tech-stack:
  added: []
  patterns:
    - "D-01 as one pure function pair shared by both paths: the app's AWT minimum when isMinimumSizeSet, else 320x240 dp — px resolution for WM_GETMINMAXINFO, dp resolution for the Compose drag clamps"
    - "The resize band is recomputed on every WM_NCHITTEST from the window's CURRENT DPI (a window may have just crossed monitors; PITFALLS 13) and never drops below CMP's own 8 dp resizer, so no Compose-side resize zone can receive a press (T-22-25)"
    - "Cross-thread app-minimum tracking: a @Volatile flag holder in the registry entry, written by a minimumSize PropertyChangeListener on the EDT, read by the frame proc's WM_GETMINMAXINFO handler through the registry (the same singleton access WM_NCDESTROY already uses)"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/MinimumSize.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeChromeStatus.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-11-SUMMARY.md
  modified:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestClassification.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Geometry.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "The WM_GETMINMAXINFO handler reads appMinimumSet through NativeWindowChromeRegistry.appMinimumSetFor(hwnd) — the same singleton access WM_NCDESTROY already uses — instead of a constructor-injected flag, so reinstalls need no re-wiring; the entry keeps the flag in an @Volatile holder updated by the minimumSize PropertyChangeListener"
  - "The handler only writes ptMinTrackSize (and traces event=mintrack) when it actually raises a value: an app-set minimum passes through untouched, which is also why the narrow window shows no mintrack trace"
  - "WIN-02 / WIN-06 / VER-11 stay Pending in REQUIREMENTS.md: the real-drag clauses (system cursor, live resize, shared-border SNAP-06) are Plan 15's real-input session, the marked-element clause is Plan 12's (22-05/22-07/22-08/22-10 precedent)"

requirements-completed: []

duration: ~15min
completed: 2026-09-27
---

# Phase 22 Plan 11: Native edge/corner resize + minimum-size floor — Summary

**Windows now resizes the window from every edge and corner through the real child→frame HTTRANSPARENT chain with a DPI-correct 8 px band at 100% (never thinner than CMP's own resizer), AeroResizeHandles stands down wherever native chrome is active, and the D-01 floor lands exactly: main clamps a 50x50 SetWindowPos to 320x240, the narrow window keeps its own 260x200 — main V11 18/18 PASS.**

## Performance

- **Duration:** ~15min
- **Tasks:** 3/3 (bands + WM_GETMINMAXINFO floor; AeroResizeHandles gate + Compose-side D-01; live check main + narrow)
- **Files:** 9 (2 created library files, 6 modified library/NOTES files, this SUMMARY) + 4 git-ignored capture artifacts under `.captures/22-resize/`
- `./gradlew :library:test --rerun` — AERO_TEST_COUNT total=541 green after Task 1 and after Task 2; `:library:compileKotlin :showcase:compileKotlin` green
- Evidence: `.captures/22-resize/live.json` + `console.log` + `Measure-Band.ps1` output, launch log `.captures/22-baseline/logs/winprobe-run-191550.489.out.log`, 22-NOTES.md "Native resize (22-11)"

## Accomplishments

- `classifyHitTest` answers HTLEFT/HTRIGHT/HTTOP/HTBOTTOM and the four corner codes from the band (corner = within the band of two adjacent edges; corner band → edge band → maximize → min/close/interactive → caption → client); no bands while maximized; `resizeBandPx` = max(system frame, CMP 8 dp resizer) — measured 4+4=8 px = 8 dp at this machine's 100% DPI.
- Both WndProcs recompute the band per `WM_NCHITTEST` from `GetDpiForWindow` + `GetSystemMetricsForDpi` (PITFALLS 13: a window may have just crossed monitors).
- `WM_GETMINMAXINFO` joined `OWNED_FRAME_MESSAGES`: forwarded first (AWT owns the struct — an app-set minimum is already in ptMinTrackSize), then ptMinTrackSize raised to 320×240 dp × scale only when `appMinimumSet` is false; fixed offsets 24/28; traced `event=mintrack`.
- Registry: entry tracks `appMinimumSet` (seeded from `isMinimumSizeSet`, kept live by a `minimumSize` PropertyChangeListener, removed at uninstall); `NativeChromeStatus` set true at install/reuse, false at uninstall/install-failure.
- `AeroResizeHandles`: `shouldComposeResizeHandles(placement, nativeChromeActive)` gate; the hardcoded 320f/240f are gone — each drag lambda resolves the floor via `resolveComposeMinimumDp` at drag time, so a later app-set minimum is honored; off-Windows identical for apps that set nothing (API-01, D-01).
- Live check: main `V11 SUMMARY pass=18 fail=0 skip=0` — the nine Plan 11 targets GREEN in one step (edges 10/11/12/15, corners 13/14/16/17, client boundary, MINSIZE exactly 320x240), the eight prior passes unregressed, `foregroundTaken=False`. Narrow `pass=7 fail=1`: edges L/R/B native, MINSIZE exactly 260x200 (floor did not clobber the app value), MARKED-BOUNDARY the only FAIL — the standing RED for API-02, quoted not weakened.

## Task Commits

1. **Task 1: Native edge/corner bands and the WM_GETMINMAXINFO floor (D-01 native path)** - `00e4f78` (feat)
2. **Task 2: AeroResizeHandles — Windows no-op when native chrome active; D-01 on the Compose path** - `f1a87a0` (feat)
3. **Task 3: Live check on the main and narrow window + NOTES section** - `8371569` (docs)

## Decisions Made

- `appMinimumSet` reaches the frame proc through `NativeWindowChromeRegistry.appMinimumSetFor(hwnd)` (the WM_NCDESTROY precedent) rather than a constructor flag, so `reinstallFrameProc`'s new procs need no extra wiring; the plan's "@Volatile var on the entry" is realized as an `AppMinimumFlag` holder inside the entry (same semantics, avoids entry↔listener constructor circularity).
- The WM_GETMINMAXINFO handler writes (and traces) only when it raises a value — an honored app minimum leaves AWT's struct byte-identical, which is the T-22-24 guarantee made observable.
- No requirement marked complete (rule: full-clause proof only) — WIN-02's live-resize/system-cursor clause and SNAP-06 shared-border resize need Plan 15's real drag; WIN-06's marked-element clause is Plan 12's; VER-11's marked-elements clause ditto.

## Deviations from Plan

None — the plan executed as written. The Task 3 contingency ("if a 50x50 SetWindowPos request is not clamped at all, defer the min-size proof to Plan 15") did not trigger: `SetWindowPos` IS clamped through WM_GETMINMAXINFO on this WS_THICKFRAME window (main obtained exactly 320x240), so the min-size checks are GREEN, not DEFERRED.

## Assumption Drift (advisory)

- The plan's Task 3 listed `V11-N-HT-LEADING-BOUNDARY` among the expected PASSes while 22-08 had recorded it FAIL — no drift in the end: that FAIL predated 22-07's leading publishing and this run confirms PASS (leading=1, captionRightOfLeading=2).

## Issues Encountered

None — compile, both grep gates, 541-count suite, and the live probe all passed first try; no leftover showcase JVMs after the run.

## User Setup Required

None — no external service configuration required.

## Threat Model Coverage

- **T-22-03 (MINMAXINFO pointer write):** mitigated — written only after `CallWindowProc`, inside `dispatchSafely`, fixed offsets 24/28, values only ever raised.
- **T-22-24 (consumer minimumSize tampering):** mitigated — the library never calls `setMinimumSize` (grep gate green across `library/src/main/kotlin`); it only answers WM_GETMINMAXINFO when the app set nothing and clamps its own Compose drags; the narrow window's 260x200 survived the floor live.
- **T-22-25 (double resize path):** mitigated — band = max(system frame, 8 dp) fully shadows CMP's resizer and `AeroResizeHandles` composes nothing while native chrome is active; every edge/corner answer arrives through the child→frame chain (`SunAwtCanvas:-1 -> SunAwtFrame:<code>`).

## Known Stubs

None — bands, floor, gate and status flag are all wired end to end and proven live.

## Next Phase Readiness

- Plan 12 (marked interactive elements): `V11-N-HT-MARKED-BOUNDARY` FAIL is its RED; the classifier's interactive-rect branch and the leading-slot precedent (LEADING_INTERACTIVE_ID) are the seam.
- Plan 13 (pixel parity) / Plan 15 (real input): the real-drag clauses of WIN-02/SNAP-03/SNAP-04 and SNAP-06 shared-border resize; the band and the no-op gate are the baseline they exercise.
- Plan 16 (unconfirmed list): band behavior at DPI ≠ 100% remains untested on this single-monitor machine (VER-F02 gap, as before).

---

*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-27*

## Self-Check: PASSED

- Created files exist (MinimumSize.kt, NativeChromeStatus.kt, SUMMARY, live.json); task commits 00e4f78 / f1a87a0 / 8371569 present in git log; no showcase JVM left running.
