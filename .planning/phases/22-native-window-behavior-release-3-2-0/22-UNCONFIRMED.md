# Phase 22: Unconfirmed List (VER-13)

Everything on this list was checked by neither the live-window probe (`Invoke-WinProbe.ps1`,
VER-11), nor the headless test suite (596 locked tests), nor the real-input sessions (VER-12:
the full JDK 21 + JBR 21 passes in 22-15, and the gap-closure re-verification subsets in
22-25). None of it is presented as passed, tested or confirmed anywhere else in Phase 22 — it
is carried here instead, for the maintainer to see alongside `22-HANDOFF.md`. Precedent:
`21-UNCONFIRMED.md`.

## 1. Windows 10 behavior (VER-F04)

No Windows 10 machine exists for this milestone. EVERYTHING Windows-10-specific is
unconfirmed: the style-bit restyle, the `WM_NCHITTEST` classification, Snap Layouts (the
flyout is a Windows 11 shell feature; Windows 10 answers with Aero Snap alone), Snap Groups,
FancyZones interaction, the auto-hide inset math, `WM_NCCALCSIZE` behavior, and the DWM
corner-policy attribute (`DWMWA_WINDOW_CORNER_PREFERENCE` does not exist on Windows 10 — the
library's call would fail or be a no-op there; which of the two is itself unconfirmed).
Nothing in this phase was tested, or silently assumed identical, on Windows 10.

## 2. Display scaling other than 100 %

Every frame, probe run and real-input check in this phase ran at 100 % scale (96 DPI) on the
one physical monitor. The planned 150 % second-display pass (WIN-04) never executed — the
virtual display driver installs on this machine but never attaches a monitor (22-15
contingency), so even the virtual 150 % display never existed. 125 % and 200 % were never
attempted at all (the carried VER-F02 gap from v3.0/v3.1). Any layout, hit-target, or
title-band issue that only appears at a non-100 % scale is entirely unconfirmed.

## 3. A real second physical monitor

Only a virtual display driver was used — and it never attached. No check in this phase ever
ran with two active monitors: no real per-monitor DPI transition (WIN-04's drag and
Win+Shift+arrow checks), no snap onto a secondary monitor, no taskbar-on-second-monitor
auto-hide edges. All multi-monitor behavior is unconfirmed.

## 4. Linux / macOS at runtime

The non-Windows code path is the unchanged legacy branch (`WindowDraggableArea` + Compose
resize handles), compiled by the locked test suite and structurally verified (API-01 code
read, 22-07), but never executed on a Linux or macOS machine in this phase — no such machine
exists here.

## 5. Minimize / restore animation

A compositor effect: `PrintWindow` renders window content only, and the project's sanctioned
capture methods never touch the real screen, so the minimize/restore animation (and whether
it plays natively for these windows) was never observed. Only the state transitions were
proven (`SC_MINIMIZE` → reporter `minimized=true` → restore, 22-10; real minimize click,
22-15).

## 6. The visual effect of corners and shadow

The agent read back DWM attributes (`DWMWCP_DONOTROUND` = 1 at install, maximized and
restored — 152/150 trace read-backs) and the maintainer chose variant A (square, no system
shadow) from a live preview (C6). But what the window LOOKS like on screen — square corners
and no shadow in practice — is maintainer-observed only; no sanctioned capture can see
compositor-applied effects (22-06's observability note).

## 7. IME composition

No IME is installed on this machine; text input during the sessions was plain Latin/Cyrillic
key events. IME composition windows positioning themselves against the native chrome is
unconfirmed.

## 8. Screen-reader behavior beyond the UIA subtree comparison

The UIA subtree was compared to the pre-phase baseline (unchanged: a single Pane — 22-01 vs
22-10). No actual screen reader (Narrator, NVDA) was run against the native-chrome windows;
announcements of the caption buttons, drag regions, or marked interactive elements are
unconfirmed.

## 9. Binary compatibility for consumers linking a pre-compiled old `AeroTitleBar` signature

API-01 is proven at SOURCE level (the showcase compiles with call sites byte-unchanged,
`d85bc86`). A consumer with a pre-compiled binary against the old `AeroTitleBar` signature
was never linked against the new artifact (VER-F03 carries the external-consumer gap; no
consumer app is on `v3.1.0`/`v3.2.0` yet).

## 10. Fullscreen placement and native peer re-creation

No fullscreen (`Window(... fullscreen = true)`) windows and no native peer re-creation
scenarios (display change, sleep/resume, remote-desktop attach) were exercised with the
subclass installed.

## 11. JDK 24+ JNI native-access warnings

The JNA-based subclass was run on JDK 21 (Microsoft) and JBR 21 only. The JDK's
native-access warning regime (per-method `nativeAccess=` declarations needed from JDK 24
warnings onward) was not observed; behavior on future JDKs is unconfirmed.

## 12. Session checks that ended UNCONFIRMED, or differed between the two JVM passes

From 22-15 (`22-SESSION.md`, `.captures/22-session/`), re-scoped by the 22-25 gap-closure
re-verification (`.captures/22-reverify/`):

- **W04-DRAG-TO-150 / W04-WIN-SHIFT-ARROW / W04-MAXIMIZE-ON-150** — UNCONFIRMED on every
  pass (22-25 never re-ran them either): the 150 % virtual display never attached (§2, §3
  above).
- **S02-LAYOUT-PICK** — UNCONFIRMED: the Snap Layouts flyout itself is PROVEN (UIA/event
  signature match, no screenshot), but UIA cannot see a zone element inside the flyout, so
  "choosing a layout places the window" was never executed. The 22-25 S02-FLYOUT re-run
  threw a harness error before the verdict (§ 17) and the maintainer observation was
  declined-by-protocol — the layout-pick clause stays unproven either way.
- **S06-SNAP-GROUP** — UNCONFIRMED: the taskbar button was found, but no
  `TaskListThumbnailWnd` group thumbnail appeared within the 2 s hover window (22-15); the
  22-25 observation opportunity was declined-by-protocol.
- **S01 caption drag on JBR** — FAIL in both JBR runs (22-15 inert; 22-25 reproduced with
  stronger discrimination: on the same window, the same-point double-click, the Win+arrow
  chords and the edge drags all work — not hit-test, not foreground-per-se, not click
  delivery; the drag modal loop specifically does not move the JBR window). See § 17.
- RESOLVED by 22-25 (kept for the record): the 22-15 JBR **S03 chord inertness** was
  foreground-shaped (S03-WIN-RIGHT/UP/DOWN PASS with `foregroundOurs=True`; SNAP-03 flipped
  Complete); the 22-15 JBR **W05 floating-crop instability** did not reproduce (16/16
  samples, float1-vs-float2 = 0; WIN-05 flipped Complete); the 22-15 **S05-MAXIMIZE fluke**
  is superseded by the menu-open fix — the menu now opens on JBR, and the open item is
  command navigation (§ 17).

## 13. The custom-title-bar API live-proven through ONE layout only

API-04 (`rememberAeroWindowChrome`) is live-proven through `AeroTitleBar`'s own layout —
every VER-11 check, the RED control, the click toggle and the title-band parity run through
it (22-19). A DIFFERENT custom header shape (two caption areas, left-hand maximize button,
arbitrary interactive elements) is locked only by the Plan 13 headless tests
(hit-test-classification and API-04 test classes), never by a live window.

## 14. The first consumer (Pinya) not yet upgraded

Pinya (the milestone's first consumer, D-25) still consumes `v3.0.0` like every other app in
`C:\1A_WORK`. No consumer app has compiled or run against the native-chrome library; the
"every library consumer gets snapping for free" claim is proven in this repo's showcase
only.

## 15. Check-formula artifacts — fixed in 22-22/22-23, re-run in 22-25

The five 22-15 formula-artifact classes were corrected (W02 corner two-axis verdicts, floor
width-only clamps, S03-WIN-DOWN `SC_MAXIMIZE` precondition, W06 ncdestroy trace window, S07
priority-grid zone model derived from the PowerToys source) and re-run live in 22-25:
corners TR/BL/BR and both floors PASS on both JVMs, S03-WIN-DOWN PASS on JBR, W06 PASS on
JBR (ncdestroy emitted). The residuals that still have no green row are carried in § 17:
the S07 re-run (drag never started foreground-ours), the JDK-side W06 ncdestroy lag, and
the new TL-corner anomaly.

## 16. Real-input checks without an old-implementation control

The VER-11 family has its RED lineage (Plan 01 baseline + the opt-out control). The
real-input GREEN checks mostly do not: S03 (hotkeys), S05 commands, S06, S07, W02 drags,
B01/B02 clicks were never run against the pre-phase `WindowDraggableArea` window in a
controlled comparison — only S01 (drag snap: "no snap of any kind happens" on the old path,
22-04 early-gate record) and the C02/A04 opt-out comparisons have direct old-path
counterparts. The library-regression guarantee for the others rests on VER-11's RED
discipline, not on their own RED runs.

## 17. Open items from the gap-closure re-verification (22-25)

New findings and still-unproven clauses from the 22-25 session (`.captures/22-reverify/`,
`22-SESSION.md` § "Gap-closure re-verification (22-25)") — follow-up planning material;
none of these is a confirmed product defect:

- **JDK S04 double-click differential** — the caption double-click (mechanism landed in
  22-20, headless posted-message proof green on JDK, real MAX+RESTORE pair PASS on JBR) did
  not fire on JDK in the 22-25 run from a verified floating reset. Unexplained by the
  record; SNAP-04 stays Pending.
- **System-menu command navigation inertness (JBR)** — the menu opens (the 22-23 fix) but
  Move/Size/Minimize/Maximize/Close produce no state change with the menu open and the
  window foreground; the SIZE attempt left the window at 160x28 (partial-engagement
  evidence). SNAP-05 stays Pending.
- **JBR S01 caption-drag drift** — reproduced ×4 with the drag-modal-loop discrimination
  (§ 12); cause unknown; the JDK S01 evidence predates the WndProc changes and is stale for
  flipping. SNAP-01 stays Pending.
- **TL-corner anomaly** — W02-CORNER-TL read inert (zero deltas, shape Arrow/Other) on both
  JVMs in 22-25; the first corner check runs immediately after S07. New and unexplained;
  the other three corners are green on both JVMs.
- **JDK W06 ncdestroy lag** — the JDK close path is green (closeEvent, main alive, v11=0,
  hs_err=0) but the `ncdestroy` trace did not arrive within the 10 s budget; JBR emits it.
  The 22-22 pipe-lag question is decided for JBR only.
- **S02-FLYOUT harness error** — the 22-25 re-run threw while formatting evidence
  (`Events.EventName` property error) after the reference control matched (signal=Event,
  708/652 ms); the flyout-gate blast-radius guard is unproven on the current build by
  harness error, not by observation.
- **Missing S03 retry rule** — `Invoke-SessionHotkeyCheck` single-focuses and judges; the
  22-21 retry rule (one re-focus + one chord repeat before FAIL) is wired only for S07. The
  JBR S03-WIN-LEFT FAIL was judged without it.
- **W05 glyph-switch capture flat** — the 22-25 float-vs-maximized crop showed no
  difference this run (the switch is proven by the 22-15 frames, 588/616 px);
  capture-timing shaped, not re-run.
- **Foreground-race mitigation for future sessions** — an elevated VS Code window
  (pid 12752) held/reclaimed the foreground across focus clicks on both JVMs and blocked
  every foreground-gated gesture in those windows; a future session should close or
  de-elevate the foreign window first, or wire the retry rule before judging.
