---
phase: 22-native-window-behavior-release-3-2-0
plan: 09
subsystem: windows-native
tags: [btn-01, win-05, d-02, wndproc, interaction-source, compose-desktop]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-07 live hit-test regions (maximize rect answers HTMAXBUTTON); 22-02 WndProc subclass + CallWindowProc passthrough; 22-04 early gate (Snap Layouts flyout path)"
provides:
  - "AeroMaxButtonInteraction: per-window MutableInteractionSource fed with real Hover/Press interactions from the native side via EDT hop; idempotent hover/press/release/cancel; MaxButtonDirectory (WeakHashMap, no JNA)"
  - "AeroFrameWndProc owns WM_NCMOUSEMOVE / WM_NCMOUSELEAVE / WM_NCLBUTTONDOWN / WM_NCLBUTTONUP / WM_NCLBUTTONDBLCLK at HTMAXBUTTON: TrackMouseEvent re-armed on every NC move, down/up/dblclk swallowed, everything else forwarded"
  - "TitleBarButton internal with optional shared interactionSource (LocalIndication explicit); maximize button on the native path renders hover/press from the bridge — D-02 parity by construction"
  - "Live proof: posted NC down/up at HTMAXBUTTON toggles placement Maximized <-> Floating through today's onClick path; floating title band 0 differing pixels vs baseline; C5 SETTLED"
affects: [22-13, 22-15]

tech-stack:
  added: []
  patterns:
    - "Native-to-Compose write direction = real HoverInteraction/PressInteraction objects into the shared MutableInteractionSource via SwingUtilities.invokeLater (ARCHITECTURE Pattern 3 refined); native side keeps two plain booleans on the toolkit thread"
    - "Non-client leave tracking re-armed on every WM_NCMOUSEMOVE over the button (TrackMouseEvent TME_LEAVE|TME_NONCLIENT is one-shot)"
    - "Swallow-not-forward for NC button messages at HTMAXBUTTON so DefWindowProc never paints a classic caption button or double-fires (ARCHITECTURE Anti-Pattern 4)"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroMaxButtonInteraction.kt
    - .captures/22-maxbutton/Invoke-MaxButtonCheck.ps1 (git-ignored capture artifact)
  modified:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "Press origin for the bridged PressInteraction = centre of the live Maximize rect in the button's local px (nominal 46x32dp half-size as the unreachable fallback) — the same origin a centred real press gives the indication"
  - "The placement toggle is one shared lambda used by both the Compose clickable onClick and the bridge's onClick, so the WIN-05 'same code path' requirement is structural, not duplicated logic"
  - "Native-side EDT hops are gated on the two plain booleans (hovered/pressed), so a WM_NCMOUSELEAVE with no prior hover posts nothing to the EDT; the bridge methods are idempotent regardless"
  - "BTN-01 and WIN-05 stay Pending in REQUIREMENTS.md: hover/press pixel parity is Plan 13's, native-transition glyph correctness is Plan 15's (22-05/22-07 precedent)"

requirements-completed: []

duration: ~25min
completed: 2026-09-27
---

# Phase 22 Plan 09: Maximize-button interaction bridge — Summary

**Once the maximize button answers HTMAXBUTTON, Windows delivers its mouse as non-client messages Compose never sees — this plan bridges them back as real `HoverInteraction`/`PressInteraction` objects into the same `MutableInteractionSource` the unchanged `TitleBarButton` rendering already reads, so the button looks and clicks exactly like minimize/close (D-02 parity by construction), and conflict #5 is settled.**

## Performance

- **Duration:** ~25 min
- **Tasks:** 3/3 (bridge + WndProc handling; TitleBarButton wiring; live check + C5)
- **Files modified:** 5 (1 created library file, 4 modified library/notes files) + 1 git-ignored probe script
- `./gradlew :library:test --rerun` — AERO_TEST_COUNT total=541 green after both code tasks; `:showcase:compileKotlin` green with its call site untouched
- Evidence: `.captures/22-maxbutton/live.json`, launch log `.captures/22-maxbutton/logs/`, 22-NOTES.md "Max-button interaction bridge (22-09)"

## Accomplishments

- `AeroMaxButtonInteraction.kt`: per-window bridge holding the shared `MutableInteractionSource`; EDT-only `hoverEnter`/`hoverExit`/`press`/`release(click)`/`cancelPress`, all idempotent and non-suspending (`tryEmit`); press origin = centre of the live Maximize rect; `release(click = true)` invokes the installed `onClick`. `MaxButtonDirectory.forWindow` mirrors `WindowRegionsDirectory` (WeakHashMap under synchronized, no JNA, safe on every OS).
- `Win32Interop.kt`: WM_NCMOUSEMOVE/WM_NCLBUTTONDOWN/UP/DBLCLK/WM_NCMOUSELEAVE, TME_LEAVE/TME_NONCLIENT, and a hand-declared `TRACKMOUSEEVENT` Structure + `TrackMouseEvent` on `AeroUser32` — verified against the jna-platform 5.19.1 jar that no TRACKMOUSEEVENT class exists in it.
- `AeroFrameWndProc`: owns the five NC mouse messages. Over HTMAXBUTTON, `TrackMouseEvent(TME_LEAVE|TME_NONCLIENT)` is re-armed on EVERY `WM_NCMOUSEMOVE` (one-shot API, PITFALLS 12) and hover enter is hopped once per edge; every `WM_NCMOUSEMOVE` is still forwarded so `DefWindowProc` keeps driving the Snap Layouts flyout (22-04 early-gate path not regressed). NC down/up/dblclk at HTMAXBUTTON return 0 without `CallWindowProc` (no classic-button paint, no double-fire); at every other hit code they forward unchanged. Native side keeps two plain booleans on the toolkit thread; all Compose crossings go through `SwingUtilities.invokeLater` (PITFALLS 6, T-22-19).
- `AeroTitleBar.kt`: `TitleBarButton` is `internal` with a trailing `interactionSource: MutableInteractionSource? = null` — null keeps the legacy body verbatim, non-null shares the source between `hoverable` and `clickable(interactionSource, LocalIndication.current, onClick)` so hover background AND press indication come from the unchanged rendering code. Only the maximize call site on the native path passes the bridged source; its `onClick` and the bridge's `onClick` are one shared placement-toggle lambda (WIN-05); icon and content description still derive from `windowState.placement`.
- Live check (22-NOTES.md): V11-HT-MAX PASS; posted NC down/up toggled the reporter placement Maximized then Floating (`awtExtendedState` 6 then 0); trace shows `down`/`up click=true` per pair and nothing else; floating title band and white-strip bands 0 differing pixels vs the pre-phase baseline; foreground never taken; all launched processes stopped. C5 SETTLED with the FlatLaf re-injection fallback named for Plan 15.

## Task Commits

1. **Task 1: AeroMaxButtonInteraction + WndProc non-client mouse handling** - `aa47500` (feat)
2. **Task 2: TitleBarButton reads the bridged interaction source (D-02 parity by construction)** - `23b14b5` (feat)
3. **Task 3: Message-level live check of the click path; conflict #5 recorded** - `b3ca95e` (docs)

## Files Created/Modified

- `library/.../internal/windows/AeroMaxButtonInteraction.kt` — interaction bridge + directory (created)
- `library/.../internal/windows/Win32Interop.kt` — NC mouse constants, TRACKMOUSEEVENT, TrackMouseEvent
- `library/.../internal/windows/AeroWndProc.kt` — frame proc NC mouse handling + EDT hop
- `library/.../internal/windows/NativeWindowChromeRegistry.kt` — bridge captured at install
- `library/.../components/navigation/AeroTitleBar.kt` — TitleBarButton internal + bridged-source wiring
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md` — "Max-button interaction bridge (22-09)"

## Decisions Made

- The bridged `PressInteraction.Press` origin is the centre of the live Maximize rect in the button's local px (with the nominal 46x32dp half-size as the documented, unreachable fallback — a press can only follow an HTMAXBUTTON answer, which requires the rect published), matching what a centred real press gives the indication; the Plan 13 pixel test compares against this.
- One shared `togglePlacement` lambda serves both the Compose `clickable` `onClick` and the bridge's `onClick` — the WIN-05 "same code path as today's onClick" requirement holds by construction.
- EDT hops are gated on the native-side booleans: a `WM_NCMOUSELEAVE` or off-button `WM_NCMOUSEMOVE` with no prior hover/press posts nothing, avoiding pointless EDT churn; the bridge methods are idempotent regardless.

## Deviations from Plan

None - plan executed exactly as written.

## Assumption Drift (advisory)

- The plan's interfaces asserted TRACKMOUSEEVENT/TrackMouseEvent are absent from jna-platform 5.19.1 — verified first-hand against the resolved jar (no TRACKMOUSEEVENT class exists); the hand-declaration was genuinely required, not precautionary.

## Issues Encountered

None. The live check passed on the first run (both toggles, both pixel bands, no foreground interference).

## User Setup Required

None — no external service configuration required.

## Threat Model Coverage

- **T-22-19 (race/deadlock):** mitigated as designed — native side keeps two plain booleans touched only on the toolkit thread; every Compose-visible write crosses through `SwingUtilities.invokeLater`; `tryEmit` never suspends.
- **T-22-21 (stuck hover):** mitigated as designed — `TrackMouseEvent(TME_LEAVE|TME_NONCLIENT)` re-armed on every `WM_NCMOUSEMOVE` over the button; `WM_NCMOUSELEAVE` and off-button moves clear hover and cancel press.
- **T-22-22 (accessibility regression):** mitigated as designed — the maximize button keeps its `clickable` with the shared source (same semantics/focus/keyboard path as today); only the mouse input route changed.

## Known Stubs

None — the bridge is wired end to end: native messages feed the shared source, the button renders from it, and the click toggles real placement (proven live).

## Next Phase Readiness

- Plan 13 (pixel parity test): `TitleBarButton` is `internal` with the explicit-source path it needs; the press origin is the rect-centre convention it compares against.
- Plan 15 (real input): owns the real-hover frames that close the C5 fallback condition and BTN-01's hover clause; the HTMAXBUTTON answer and the `WM_NCMOUSEMOVE` forwarding the flyout needs are unregressed.
- Plan 11 (resize bands): untouched — `classifyHitTest` and the V11 edge/corner FAILs remain its GREEN targets.

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-27*

## Self-Check: PASSED
