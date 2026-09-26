---
phase: 22-native-window-behavior-release-3-2-0
plan: 08
subsystem: showcase-probe
tags: [shw-17, win-06, api-02, d-01, multi-window, probe]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-01 probe harness + reporter; 22-02/22-05 native chrome (subclass, styles, frame removal); 22-04 passed early gate"
provides:
  - "SHW-17 narrow second window fixture (NarrowQueueWindow.kt, 300x480dp, AeroTitleBar + AeroResizeHandles, own AWT minimum 260x200 — D-01)"
  - "-Daero.secondWindow=true|=<ms> launch property, forwarded in run + hotRun"
  - "AERO_EVENT observability for leading badge click, marked overlay click, narrow close-request"
  - "Probe: $WinProbeLayout.narrow geometry, V11-N-* check set, Wait-WinProbeEvent, -Windows main,narrow multi-window runner"
  - "Live RED evidence for API-02: V11-N-HT-MARKED-BOUNDARY FAIL on the unmarked overlay"
affects: [22-11, 22-12, 22-15]

tech-stack:
  added: []
  patterns:
    - "Second window in the same application {} block, gated by a remembered state flag fed from a launch property (0 = at start, >0 = delayed open)"
    - "Probe layout per window kind: shared row/button constants + per-layout captionXDp override and extraPointsDp, scaled at point-computation time"
    - "Three-way min-size discrimination: app minimum (260x200) vs library floor (320x240) vs no floor (50x50) in one check"

key-files:
  created:
    - showcase/src/main/kotlin/com/mordred/showcase/NarrowQueueWindow.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-08-SUMMARY.md
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/Main.kt
    - showcase/build.gradle.kts
    - tools/winprobe/WinProbe.ps1
    - tools/winprobe/Invoke-WinProbe.ps1

key-decisions:
  - "Narrow window follows the main window's capture contract (non-focusable, window.toBack(), distinct [capture] title) so both windows are probe-launched side by side (T-22-20)"
  - "The 'Вернуть' overlay is deliberately NOT marked interactive on the native path yet — its V11-N-HT-MARKED-BOUNDARY FAIL is the standing RED for API-02 (Plan 12 adds the marker)"
  - "SHW-17 and WIN-06 stay Pending in REQUIREMENTS.md: SHW-17's marked-element clause is Plan 12, WIN-06's resize/hotkey clauses are later plans; this plan only creates the fixture and proves it measurable"

requirements-completed: []

duration: ~13min
completed: 2026-09-26
---

# Phase 22 Plan 08: Narrow second window fixture + probe narrow checks — Summary

**The showcase opens a Pinya-shaped narrow queue window (300x480dp, own 260x200 minimum) via `-Paero.secondWindow`, and the probe measures both windows in one launch — with the unmarked "Вернуть" overlay already proven RED for API-02.**

## Performance

- Tasks: 2/2 (fixture + launch property; probe narrow layout + multi-window smoke)
- `./gradlew :showcase:compileKotlin` green; probe `-SelfTest` PASS (narrow points at scale 1.0 and 1.5, `marked.x=177` at 1.5)
- `git diff --quiet HEAD -- library/` holds — showcase + probe only
- Evidence: `.captures/22-narrow/smoke.json`, probe log `.captures/22-baseline/logs/winprobe-run-232831.551.out.log`

## Accomplishments

- `NarrowQueueWindow.kt`: independent undecorated window with `AeroTitleBar` (leading 24x20dp badge "3", 60x22dp "Вернуть" overlay at x 88..148dp) + `AeroResizeHandles`; sets its own AWT `minimumSize = Dimension(260, 200)`; reports `name=leading` / `name=marked` clicks and `close-request label=narrow` via `AERO_EVENT`.
- `Main.kt`: `-Daero.secondWindow=true` opens at start, `=<ms>` opens after a delay (for the real-input session's drag-while-opening scenario); absent/other never opens.
- Probe: `$WinProbeLayout.narrow`, eight `V11-N-*` checks (caption, max, leading/marked boundaries, edges L/R/B, own min size), `Wait-WinProbeEvent`, and a `-Windows main,narrow` runner mode that finds both windows by exact title and forwards `-Paero.secondWindow=true`.

## Live smoke results (capture mode, `-SkipMaximize`, one launch, pid 17160)

Two independent subclass installs in the chrome trace, one per window:

```
AERO_CHROME event=restyle hwnd=0x1404b6 ... / event=install hwnd=0x1404b6 / event=child-subclass hwnd=0x1404b6 childHwnd=0x20034e
AERO_CHROME event=restyle hwnd=0x110530 ... / event=install hwnd=0x110530 / event=child-subclass hwnd=0x110530 childHwnd=0x705b4
```

Reporter lines for both labels; narrow shows `minSizeSet=true minSize=260x200`, `sizeDp=300.0x480.0`, opened at the capture position (1620,300 = CenterEnd on the 1920x1080 monitor).

Narrow V11 results — recorded as phase state, not GREEN:

```
V11 V11-N-HT-CAPTION          PASS expected=2 observed=2 (HTCAPTION) chain=SunAwtCanvas:-1 -> SunAwtFrame:2
V11 V11-N-HT-MAX              PASS expected=9 observed=9 (HTMAXBUTTON) chain=SunAwtCanvas:-1 -> SunAwtFrame:9
V11 V11-N-HT-LEADING-BOUNDARY FAIL expected=leading=1,captionRightOfLeading=2 observed=leading=2,captionRightOfLeading=2
V11 V11-N-HT-MARKED-BOUNDARY  FAIL expected=marked=1,captionLeftOfMarked=2 observed=marked=2,captionLeftOfMarked=2
V11 V11-N-HT-EDGE-L           FAIL expected=10 observed=1 (HTCLIENT)
V11 V11-N-HT-EDGE-R           FAIL expected=11 observed=1 (HTCLIENT)
V11 V11-N-HT-EDGE-B           FAIL expected=15 observed=1 (HTCLIENT)
V11 V11-N-MINSIZE             PASS expected=>=260x200 and <320x240 observed=260x200
V11 SUMMARY narrow pass=3 fail=5 skip=0
```

- `V11-N-HT-MARKED-BOUNDARY` **FAIL is the expected RED for API-02** — the overlay is not marked interactive until Plan 12; both probe points classify as HTCAPTION today.
- `V11-N-HT-LEADING-BOUNDARY` FAILs the same way (the badge is also unmarked) — same mechanism, same owner plan.
- `V11-N-MINSIZE` already PASSes: Windows clamps the probe's 50x50 request to exactly the app's own AWT minimum (260x200), three-way-discriminated from the library floor (320x240) and no floor (50x50). This is the D-01 "app sets its own minimum" case proven live on the native path — the library-side default floor (which must not override this) is Plan 11's.
- Narrow edge checks FAIL — native edge/corner resize hit-testing is a later plan's scope, unchanged from the main window's state.
- Main window in the same run: V11 pass=5 fail=10 skip=3 (skip = `-SkipMaximize` per plan; `V11-MINSIZE` FAIL `observed=136x50` is Plan 11's floor, not a regression — pre-phase value was 50x50, the 136 width is the WS_THICKFRAME tracking-size minimum).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Threat mitigation] Capture-mode `window.toBack()` for the narrow window**
- **Found during:** Task 1
- **Issue:** The plan's prose specified `focusable = !capture` for the narrow window but did not mention `toBack()`; T-22-20's mitigation requires the second capture window to stay behind other windows, which the main window achieves via `LaunchedEffect(Unit) { window.toBack() }`.
- **Fix:** The narrow window calls `window.toBack()` on capture launches, mirroring the main window.
- **Files modified:** showcase/src/main/kotlin/com/mordred/showcase/NarrowQueueWindow.kt
- **Commit:** 1382ee1

No other deviations — the plan executed as written otherwise.

## Assumption Drift (advisory)

- `V11-N-MINSIZE` was implicitly expected to stay RED until Plan 11 (the plan's smoke text names only the marked boundary as "a free RED"); it actually PASSes already because Windows honors AWT `minimumSize` natively, with no library code involved. This strengthens, not weakens, D-01: the honored-app-minimum path needs no library floor code at all; Plan 11's work is the default 320x240 floor plus not clobbering the app's value.

## Commits

- `1382ee1` feat(22-08): showcase narrow second window fixture (SHW-17)
- `1cec69d` feat(22-08): probe geometry and checks for the narrow window (SHW-17, WIN-06)
- final docs commit — this SUMMARY

## Self-Check: PASSED
