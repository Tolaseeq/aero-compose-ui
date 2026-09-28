# Phase 22 — Full VER-12 real-input session record

Plan 22-15. Task 1 (readiness) recorded 2026-09-28. The session itself (Task 3) appends
`## Pass 1 — JDK 21` / `## Pass 2 — JBR 21` below after the maintainer's "ок".

## Readiness

Recorded by Task 1, 2026-09-28, before any consent was asked. Nothing was installed, no real
input was sent, no settings were changed while recording this section.

### Builds

- `./gradlew :showcase:classes --console=plain` → BUILD SUCCESSFUL (7 tasks, all UP-TO-DATE).
- Full unfiltered `./gradlew :library:test --rerun --console=plain` → BUILD SUCCESSFUL,
  `AERO_TEST_COUNT total=592 skipped=0 expected=592 expectedSkipped=0 filtered=false` — the
  locked count from Plan 13 holds.

### Installer re-verification (against 22-SESSION-ENV.md, exact match required)

Session scratchpad (outside the repo, nothing committed, nothing executed):
`C:\Users\1\AppData\Local\Temp\aero-22-session\`. Fresh downloads from the official GitHub
Releases APIs (tag `25.7.23` and `v0.101.2362.0`), then SHA-256 + Authenticode re-verified
(`Verify-Installers.ps1` in the scratchpad):

| File | SHA-256 | Authenticode | Signer |
|------|---------|--------------|--------|
| `MttVDD.dll` | MATCH (C9CA…BB05) | Valid | CN=SignPath Foundation (GlobalSign GCC R45 CodeSigning CA 2020) |
| `mttvdd.cat` | MATCH (08A0…68F6) | Valid | CN=SignPath Foundation (GlobalSign GCC R45 CodeSigning CA 2020) |
| `VDD Control.exe` | MATCH (CC6F…2F0F) | Valid | CN=SignPath Foundation (GlobalSign GCC R45 CodeSigning CA 2020) |
| `devcon.exe` | MATCH (77E8…FA05) | Valid | CN=Microsoft Corporation (Microsoft Code Signing PCA 2010) |
| `PowerToysUserSetup-0.101.2362.0-x64.exe` | MATCH (D56F…0DE3) | Valid | CN=Microsoft Corporation (Microsoft Code Signing PCA 2024) |

`MttVDD.inf` present. `driver-flat\` (scratchpad) assembles the five vetted files in the flat
layout `Install-AeroVirtualDisplay -DriverDir` expects. Task 3's real run passes
`-DriverDir C:\Users\1\AppData\Local\Temp\aero-22-session\driver-flat` and
`-PowerToysInstaller C:\Users\1\AppData\Local\Temp\aero-22-session\PowerToysUserSetup-0.101.2362.0-x64.exe`;
`SessionEnv.ps1` re-verifies every SHA-256 again before anything elevated runs.

Driver identity for the consent message: VirtualDrivers "Virtual Display Driver" (MttVDD),
release 25.7.23, signed by SignPath Foundation (GlobalSign) — no test-signing mode needed.
PowerToys: per-user `PowerToysUserSetup` v0.101.2362.0, signed Microsoft Corporation, no UAC.

### Dry run (timed)

Command:
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/winprobe/Invoke-FullSession.ps1 -DryRun -Pass Both -Phase Checks -OutDir .captures/22-session-readiness`

- **Exit code 0**, wall-clock **412 s (6 min 52 s)**.
- jdk pass: `jvm=standard path=C:\Users\1\.jdks\ms-21.0.9\bin\java.exe`, 172.8 s,
  totals pass=2 fail=0 unconfirmed=52 (54 SESSION lines, 55 IDs — C02 emits two).
- jbr pass: `jvm=JBR path=C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe`, 181.7 s, same totals.
- All **55 check IDs listed exactly once per pass** (grep-verified), 0 `SESSION_CHECK_ERROR`.
- A04-OPTOUT RED control self-proved in both passes: on the `-Paero.nativeChrome=false` launch
  every V11 check FAILs (`v11 pass=0 fail=18`) and zero chrome-trace lines appear.
- Cursor `685,926 → 685,926` unchanged; `EnvUnchanged=True` (screens, auto-hide, PowerToys,
  FancyZones, virtual display all identical before/after); zero installs.
- Zero leftover `com.mordred.showcase.MainKt` JVMs after the run (PID-tracked teardown).
- API-04 attribution line in both pass headers:
  `AeroTitleBar -> rememberAeroWindowChrome (code references rememberAeroWindowChrome=1, NativeWindowChromeRegistry=0)`.

### Minutes estimate for the maintainer (N)

| Component | Basis | Time |
|-----------|-------|------|
| Mechanics of both passes (launches incl. hotRun, window waits, V11 probe cycles, W06/C02/F18 relaunches, frame captures) | measured in the dry run above | 412 s ≈ 7 min |
| Planned real input of both passes (real-only sleeps, drag durations, hover/flyout waits summed from the script, ~107 s per pass) | computed from `Invoke-FullSession.ps1` | ≈ 4 min |
| Environment setup (devcon install + UAC + display-appear wait ~1 min; DisplayConfig module install — confirmed absent today — + scale set ~1 min; PowerToys silent install ~2.5 min; FancyZones start ~0.5 min; auto-hide toggle instant) | estimated from 22-SESSION-ENV.md commands | ≈ 5 min |
| **Subtotal** | 7 + 4 + 5 | **16 min** |
| Round up, add 3 (plan's formula) | 16 → 16 + 3 | **N = 19 min** |

### Restore target (recorded read-only; nothing changed)

- `Get-AeroEnvState`: screens=1; primary monitor `0,0,1920,1080`, work `0,0,1920,1080`,
  96 DPI (scale 1.0); `TaskbarAutoHideOn=True`; `PowerToysInstalled=False`;
  `PowerToysRunning=False`; `FancyZonesRunning=False`; `VirtualDisplayPresent=False`.
- `HKCU:\...\Explorer\StuckRects3` value `Settings`, byte offset 8 = **0x03**
  (auto-hide + always-on-top) — the byte the session must leave unchanged.
- Snap settings: `SnapAssistFlyoutOn=True`, `WindowArrangementActive=True`.
- End-of-session obligation (Task 3): auto-hide back to ON (0x03 preserved), no leftover
  showcase JVM; the virtual display driver and PowerToys stay installed on purpose until
  Plan 16, on the maintainer's word.

### JVM paths verified

- Pass 1 (standard JDK 21): `C:\Users\1\.jdks\ms-21.0.9\bin\java.exe` — exists,
  `openjdk version "21.0.9" Microsoft-12574459`; dry-run pass reported `jvm=standard`.
- Pass 2 (JBR 21): `C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe` — exists,
  `JBR-21.0.9+1-1038.76-nomod`; dry-run pass reported `jvm=JBR`.

**Readiness verdict:** the session can start the moment consent arrives — builds green at the
locked count, installers byte-identical to the vetted record, the suite dry-run-proven at
exit 0 with a measured duration, and the restore target recorded.

## Session environment (what actually happened before the checks)

Consent: maintainer's «ок» @ 2026-09-28T00:04:59Z. The maintainer was present and hands-off
from the first UAC prompt until the end-of-run announcement. Three script launches were
needed; the first two changed environment state only (no real input was sent in either):

1. **Run 1 (aborted at env setup, 3 m 31 s, zero real input).** devcon install succeeded at the
   UAC the maintainer confirmed ("Device node created ... Drivers installed successfully"), but
   the suite's presence check looked for `ROOT\MttVDD\*` while the real device enumerates as
   `ROOT\DISPLAY\0000` (class Display, "Virtual Display Driver") — and the virtual display
   never attached (screen count stayed 1 through the 3-minute wait). The run then died at
   `Install-Module DisplayConfig` (a `ShouldContinue` NRE in the non-interactive host). Cursor
   and auto-hide untouched.
2. **Driver repair (elevated, vetted commands only, 2 UAC prompts total).** Diagnosis: with the
   device running (Status OK, monitor devnodes `DISPLAY\MTT1337\*` present), `Enable-Display`
   on the inactive VDD path fails ("A device attached to the system is not functioning");
   `devcon restart` and a clean remove+reinstall (single instance) also produce no monitor
   arrival — on this machine the MttVDD adapter installs but never attaches a display without
   its own Control app, and no mode configuration exists anywhere in the registry. The
   DisplayConfig PowerShell module was pre-installed (NuGet provider + trusted PSGallery +
   DisplayConfig 1.1.1, CurrentUser) to take the failing `Install-Module` path out of the
   suite.
3. **Run 2 (aborted at env setup, 4 m 28 s, zero real input).** Exposed two more live-only
   gaps: `Set-DisplayScale` targeting the INACTIVE VDD display misapplied the scale to the
   PRIMARY panel (96 -> 144 DPI; caught by the suite's own safety check), and PowerToys
   installs its binary at `%LOCALAPPDATA%\PowerToys\PowerToys.exe` (not
   `...\Microsoft\PowerToys\`). The primary was immediately restored to 100 % (verified 96 DPI,
   1920x1080). Auto-hide untouched.
4. **Hardening commits** `d30e605` (real instance-path matching, idempotent installs,
   soft display-scale failure) and `0dd682e` (PowerToys paths, inactive-display scale guard,
   `applied-layouts.json` zone parser, `ToArray()` for the PS 5.1 `@()`-on-List[object]
   pitfall).
5. **Run 3 — the real session** (console `.captures/22-session/console-3.log`, summary
   `summary.json`, per-pass `jdk/` and `jbr/`). Env steps actually taken: virtual display
   install skipped (device already present; attach still absent), display scale skipped by the
   inactive-display guard, PowerToys install skipped (already installed; running with
   FancyZones), taskbar auto-hide turned OFF for the checks and restored ON at the end
   (`SESSION AUTOHIDE-RESTORE before=False after=True verified=True`). **WIN-04 contingency
   active:** the 150 % virtual display never existed on this machine, so all three W04 checks
   are UNCONFIRMED by design; everything else ran.
6. **Post-run state (verified):** StuckRects3 byte 8 = 0x03 (auto-hide ON, the recorded
   original), primary monitor 1920x1080 @ 96 DPI, screens=1, zero leftover
   `com.mordred.showcase.MainKt` JVMs, no notepad, cursor restored
   (`SESSION CURSOR before=1200,466 after=1200,466`). The MttVDD device and PowerToys remain
   installed on purpose for Plan 16.

## Pass 1 — JDK 21 (C:\Users\1\.jdks\ms-21.0.9\bin\java.exe)

Standard JDK (`jvm=standard`, pid 10784, main=0x2E0042 narrow=0x1F04AA), `:showcase:run`.
API-04 attribution: `AeroTitleBar -> rememberAeroWindowChrome (code references
rememberAeroWindowChrome=1, NativeWindowChromeRegistry=0)`. Evidence root
`.captures/22-session/jdk/` (`results.json`, `frames/`); console lines in
`.captures/22-session/console-3.log`.

**Totals: pass=31 fail=18 unconfirmed=5 (54 checks).**

| Id | Requirement | Result | Evidence |
|----|-------------|--------|----------|
| S01-LEFT-HALF | SNAP-01 | PASS | real drag to (1,516); rect=0,0,960,1032 = left half of rcWork |
| S01-RIGHT-HALF | SNAP-01 | PASS | rect=960,0,1920,1032 = right half |
| S01-QUARTER-TL | SNAP-01 | PASS | rect=0,0,960,516 = top-left quarter |
| S01-TOP-MAXIMIZE | SNAP-01 | PASS | drag to top edge; isZoomed=True |
| S01-DRAG-AWAY-RESTORE | SNAP-01 | PASS | drag away; restored=1200x800 |
| S02-FLYOUT | SNAP-02 | PASS | positive control 618 ms; Compose flyout 722 ms, signal=Event, SHOW/UNCLOAKED on explorer's `Windows.UI.Composition.DesktopWindowContentBridge` + `Xaml_WindowedPopupClass` + `XamlExplorerHostIslandWindow`; signature match=True (UIA/event, no screenshot) |
| S02-LAYOUT-PICK | SNAP-02 | UNCONFIRMED | flyout appears but UIA cannot see a zone element in its descendants (no click target) |
| S03-WIN-LEFT | SNAP-03 | PASS | chord LWin+Left; rect=0,0,960,1032 |
| S03-WIN-RIGHT | SNAP-03 | PASS | rect=960,0,1920,1032 |
| S03-WIN-UP | SNAP-03 | PASS | isZoomed=True placement=Maximized |
| S03-WIN-DOWN | SNAP-03 | FAIL | inside the check's own Win+Up: zoomedAfterUp=False (standalone S03-WIN-UP above passed seconds earlier) |
| S04-DBLCLICK-MAX | SNAP-04 | FAIL | real caption double-click; isZoomed=False (never maximized) |
| S04-DBLCLICK-RESTORE | SNAP-04 | PASS | restored 1200x800 (window still floating) |
| S05-ALTSPACE-MENU | SNAP-05 | FAIL | Alt+Space; no #32768 menu window of our process |
| S05-MOVE | SNAP-05 | FAIL | menu navigation never had a menu: moved dx=0 dy=0 |
| S05-SIZE | SNAP-05 | FAIL | resized dWidth=0 dHeight=0 |
| S05-MINIMIZE | SNAP-05 | FAIL | isIconic=False |
| S05-MAXIMIZE | SNAP-05 | FAIL | isZoomed=False placement=Floating |
| S05-RESTORE | SNAP-05 | PASS | restored 1200x800 |
| S05-CLOSE-VS-ALTF4 | SNAP-05 | FAIL | menuCloseEvent=False/narrowGone=False (menu path dead); Alt+F4 half PROVEN: altF4Event=True, window destroyed, fixture reopened |
| C02-OPTOUT-ALTSPACE | SNAP-05 (C2) | PASS | opt-out window (`-Paero.nativeChrome=false`, style 0x960B0000, zero AERO_CHROME lines): menuVisible=True, menu-Move moved dx=220 — the C2 comparison observation |
| A04-OPTOUT | API-04 | PASS | RED control: v11 pass=0 fail=17 on the opt-out launch (every VER-11 check fails without the native path) |
| S06-SHARED-BORDER | SNAP-06 | FAIL | both windows half-snapped first (S03 above), then border drag: mainRightDelta=0 narrowLeftDelta=0 |
| S06-SNAP-GROUP | SNAP-06 | UNCONFIRMED | taskbar button found (961,1056); no TaskListThumbnailWnd appeared within 2 s of the hover |
| S07-FANCYZONES | SNAP-07 | FAIL | zones found (priority-grid, heuristic zone-count=3); drag started with foregroundOurs=False; rect unchanged 360,140,1560,940 — no snap happened this attempt |
| W01-MAX-VISIBLE-TASKBAR | WIN-01 | PASS | maximized client == rcWork; taskbar not overlapped |
| W01-AUTOHIDE-REVEAL | WIN-01 | PASS | auto-hide taskbar revealed after 232 ms of bottom-row hover over the maximized 2 px inset (C3 reveal half) |
| W02-EDGE-L | WIN-02 | PASS | shape=SizeWE; dWidth=-60 dHeight=0 |
| W02-EDGE-R | WIN-02 | PASS | shape=SizeWE; dWidth=60 dHeight=0 |
| W02-EDGE-T | WIN-02 | PASS | shape=SizeNS; dHeight=-60 |
| W02-EDGE-B | WIN-02 | PASS | shape=SizeNS; dHeight=60 |
| W02-CORNER-TL | WIN-02 | FAIL | observed shape=SizeNWSE, dWidth=60 dHeight=60 — the behavior is correct; the check's formula forbids the second axis for corner drags (check bug, see Findings) |
| W02-CORNER-TR | WIN-02 | FAIL | shape=SizeNESW, 60x60 — same formula bug |
| W02-CORNER-BL | WIN-02 | FAIL | shape=SizeNESW, 60x60 — same formula bug |
| W02-CORNER-BR | WIN-02 | FAIL | shape=SizeNWSE, 60x60 — same formula bug |
| W02-MAIN-FLOOR | WIN-02 | FAIL | inward left-edge drag: width clamped at exactly 320 (the floor WORKS); height stayed 800 because a left-edge drag cannot change height (check expectation bug) |
| W02-NARROW-FLOOR | WIN-02 | FAIL | width clamped at exactly 260 (app floor honored, D-01); height expectation bug as above |
| W03-FRAMES | WIN-03 | PASS | 6 frames (floating/snapped/maximized both windows); floating title band vs pre-phase baseline diffPixels=0 |
| W03-CORNERS | WIN-03 | PASS | all 152 dwm trace read-backs = 1 (DWMWCP_DONOTROUND, the C6 choice) |
| W04-DRAG-TO-150 | WIN-04 | UNCONFIRMED | virtual 150 % display not present — the MttVDD device installs (Status OK) but never attaches a monitor on this machine (contingency) |
| W04-WIN-SHIFT-ARROW | WIN-04 | UNCONFIRMED | requires the 150 % display (scale=1) |
| W04-MAXIMIZE-ON-150 | WIN-04 | UNCONFIRMED | requires the 150 % display (scale=1) |
| W05-PLACEMENT | WIN-05 | PASS | 17 placement samples, 0 mismatches (reporter placement == IsZoomed at every step); float1-vs-float2 glyph crop diff 0; float-vs-maximized diff 616 px (glyph switched) |
| W06-INDEPENDENT | WIN-06 | PASS | dragging narrow left main unmoved (tol 2 px) and vice versa; installLines>=2, reuse=0 |
| W06-OPEN-WHILE-DRAGGING | WIN-06 | PASS | narrow opened mid-drag (12 s delay) with its own install line; post-drag v11 fail=0 on both windows |
| W06-CLOSE-DURING-DRAG | WIN-06 | FAIL | closeEvent=True (WM_CLOSE mid-drag reached onCloseRequest), mainAlive, v11 fail=0, new hs_err=0; but no `event=ncdestroy` trace line within 5 s |
| B01-HOVER-FRAME | BTN-01 | PASS | hover-max vs hover-min strips diffPixels=0; hover vs rest diffPixels=144 (hover paints) |
| B01-PRESS-FRAME | BTN-01 | FAIL | press-max vs press-min diffPixels=144 maxDelta=176 — press fill differs between max and min (C5 fallback condition triggered) |
| B01-CLICK | BTN-01 | PASS | real click toggled Maximized then Floating |
| B02-MIN-CLICK | BTN-02 | PASS | min click: reporter minimized=true, isIconic=True (restored after) |
| B02-LEADING-CLICK | BTN-02 | PASS | AERO_EVENT name=click name=leading within 3 s |
| B02-MARKED-CLICK | BTN-02 | PASS | AERO_EVENT name=click name=marked; window not dragged |
| B02-CLOSE-CLICK | BTN-02 | PASS | close click ran onCloseRequest (label=narrow), window destroyed, fixture reopened |
| F18-RESIZE-FRAMES | PITFALLS 18 | PASS | 5 live-resize frames, 5 distinct widths on the native path; opt-out comparison set captured |

## Pass 2 — JBR 21 (C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe)

JBR (`jvm=JBR`, pid 8404, main=0x60534 narrow=0xE0536), `:showcase:hotRun`. Same evidence
layout under `.captures/22-session/jbr/`.

**Totals: pass=24 fail=25 unconfirmed=5 (54 checks).**

| Id | Requirement | Result | Evidence |
|----|-------------|--------|----------|
| S01-LEFT-HALF | SNAP-01 | FAIL | drag caption (891,156) -> (1,516); rect unchanged 360,140,1560,940 — the caption drag never moved the window |
| S01-RIGHT-HALF | SNAP-01 | FAIL | rect unchanged |
| S01-QUARTER-TL | SNAP-01 | FAIL | rect unchanged |
| S01-TOP-MAXIMIZE | SNAP-01 | FAIL | isZoomed=False after drag to top |
| S01-DRAG-AWAY-RESTORE | SNAP-01 | PASS | restored 1200x800 |
| S02-FLYOUT | SNAP-02 | PASS | positive control 610 ms; Compose flyout 611 ms, signal=Event, matched `E\|Windows.UI.Composition.DesktopWindowContentBridge\|explorer` (UIA/event, no screenshot) |
| S02-LAYOUT-PICK | SNAP-02 | UNCONFIRMED | UIA cannot see a zone element in the flyout descendants |
| S03-WIN-LEFT | SNAP-03 | FAIL | chord sent; rect unchanged 360,140,1560,940 |
| S03-WIN-RIGHT | SNAP-03 | FAIL | rect unchanged |
| S03-WIN-UP | SNAP-03 | FAIL | isZoomed=False placement=Floating |
| S03-WIN-DOWN | SNAP-03 | FAIL | zoomedAfterUp=False |
| S04-DBLCLICK-MAX | SNAP-04 | FAIL | isZoomed=False |
| S04-DBLCLICK-RESTORE | SNAP-04 | PASS | restored 1200x800 |
| S05-ALTSPACE-MENU | SNAP-05 | FAIL | no #32768 menu window of our process |
| S05-MOVE | SNAP-05 | FAIL | dx=0 dy=0 |
| S05-SIZE | SNAP-05 | FAIL | dWidth=0 dHeight=0 |
| S05-MINIMIZE | SNAP-05 | FAIL | isIconic=False |
| S05-MAXIMIZE | SNAP-05 | PASS | system menu Maximize maximized the window (the one S05 command that landed — see Differences) |
| S05-RESTORE | SNAP-05 | PASS | restored 1200x800 |
| S05-CLOSE-VS-ALTF4 | SNAP-05 | FAIL | menuClose=False/False; Alt+F4 half PROVEN (event=True, destroyed, reopened) |
| C02-OPTOUT-ALTSPACE | SNAP-05 (C2) | PASS | opt-out: menuVisible=True moveDx=220 sizeDw=0 (own standard-JDK opt-out launch inside the pass) |
| A04-OPTOUT | API-04 | PASS | RED control: v11 pass=0 fail=17 |
| S06-SHARED-BORDER | SNAP-06 | FAIL | mainRightDelta=0 narrowLeftDelta=0 (pre-snap hotkeys had also failed on this pass) |
| S06-SNAP-GROUP | SNAP-06 | UNCONFIRMED | no TaskListThumbnailWnd within 2 s |
| S07-FANCYZONES | SNAP-07 | FAIL | Shift-drag DID snap: rect=16,16,1432,1016 — exactly a real FancyZones priority-grid zone (work area inset by the layout's 16 px spacing); the check's heuristic 2x2-grid expectation (0,0,960,516) does not model priority-grid geometry |
| W01-MAX-VISIBLE-TASKBAR | WIN-01 | PASS | maximized client == rcWork; taskbar not overlapped |
| W01-AUTOHIDE-REVEAL | WIN-01 | PASS | revealed after 231 ms (C3 reveal half) |
| W02-EDGE-L/R/T/B | WIN-02 | PASS | all four: correct SizeWE/SizeNS shapes, 60 px single-axis growth |
| W02-CORNER-TL/TR/BL/BR | WIN-02 | FAIL | same as JDK: correct shapes and 60x60 diagonal growth; corner formula bug in the check |
| W02-MAIN-FLOOR | WIN-02 | FAIL | width clamped exactly 320 (floor works); height expectation bug |
| W02-NARROW-FLOOR | WIN-02 | FAIL | width clamped exactly 260; height expectation bug |
| W03-FRAMES | WIN-03 | PASS | 6 frames; floating title band diff 0 |
| W03-CORNERS | WIN-03 | PASS | all 150 dwm read-backs = 1 |
| W04-DRAG-TO-150 | WIN-04 | UNCONFIRMED | same contingency as JDK |
| W04-WIN-SHIFT-ARROW | WIN-04 | UNCONFIRMED | same |
| W04-MAXIMIZE-ON-150 | WIN-04 | UNCONFIRMED | same |
| W05-PLACEMENT | WIN-05 | FAIL | 17 samples 0 mismatches and glyph switch 616 px, but float1-vs-float2 glyph crop diffPixels=616 maxDelta=54 — the floating crop was NOT stable across the maximize cycle (JDK: 0) |
| W06-INDEPENDENT | WIN-06 | PASS | both directions unmoved; reuse=0 |
| W06-OPEN-WHILE-DRAGGING | WIN-06 | PASS | own install mid-drag; v11 green on both |
| W06-CLOSE-DURING-DRAG | WIN-06 | FAIL | closeEvent=True, main alive, v11 fail=0, hs_err=0; ncdestroy trace not seen in 5 s |
| B01-HOVER-FRAME | BTN-01 | PASS | hover parity diff 0; hover vs rest 144 |
| B01-PRESS-FRAME | BTN-01 | FAIL | press parity diff 144 maxDelta=176 (C5 fallback condition) |
| B01-CLICK | BTN-01 | PASS | toggled Maximized then Floating |
| B02-MIN-CLICK | BTN-02 | PASS | minimized=true reported |
| B02-LEADING-CLICK | BTN-02 | PASS | event reached Compose |
| B02-MARKED-CLICK | BTN-02 | PASS | event reached Compose; not dragged |
| B02-CLOSE-CLICK | BTN-02 | PASS | close request + destroy + reopen |
| F18-RESIZE-FRAMES | PITFALLS 18 | PASS | 5 distinct widths; opt-out set captured |

## Differences between passes

Checks whose result differs between JDK 21 and JBR 21 (every difference is a finding):

| Id | JDK | JBR | Observation |
|----|-----|-----|-------------|
| S01-LEFT/RIGHT/QUARTER/TOP (4) | PASS | FAIL | On JBR no caption drag moved the window (rect unchanged in all four), while edge drags (W02) and the Shift-drag (S07) DID move it; on JDK all four snapped. Unexplained by the recorded evidence — a JBR/hotRun-specific interaction of the native caption drag path, gap-closure material |
| S03-WIN-LEFT/RIGHT/UP (3) | PASS | FAIL | Same shape: Win+arrow hotkeys inert on JBR in this run (window not foreground-able or chords swallowed), while B01/B02 caption clicks and S07 focus-click+drag worked |
| S05-MAXIMIZE | FAIL | PASS | The only S05 command that landed on JBR; with ALTSPACE-MENU failing on both, the JBR PASS is consistent with a fluke menu opening (450 ms wait) rather than a working path |
| W05-PLACEMENT | PASS | FAIL | JBR floating glyph crop unstable across the maximize cycle (616 px vs JDK's 0); placement samples themselves matched 17/17 on both |

Everything else is result-identical between the JVMs, including all four FAIL groups that are
check-formula artifacts (corners, floors — see below) and the C2 system-menu failure.

### Honest reading of the FAIL groups

- **Real product gaps (gap closure):** S05 family (Alt+Space menu never opens on the native
  window although the opt-out shows it — C2 SETTLED, hand-declared GetSystemMenu/
  TrackPopupMenu is the known fix path); S04-DBLCLICK-MAX (double-click caption never
  maximizes, both JVMs); S06-SHARED-BORDER (shared border drag resizes nothing); B01-PRESS
  (press fill parity broken — C5's named fallback condition); JBR-only S01/S03 caption-drag and
  hotkey inertness; JBR W05 glyph-crop instability.
- **Check-formula artifacts (behavior actually proven by the observed values):** W02-CORNER-*
  (correct cursor shapes + 60x60 diagonal growth; the formula forbids the second axis);
  W02-MAIN/NARROW-FLOOR (width clamped exactly at 320/260 — the floor requirement is proven;
  the height expectation is unreachable from a left-edge drag).
- **Environment-blocked:** W04-* (no 150 % virtual display on this machine — contingency);
  S02-LAYOUT-PICK and S06-SNAP-GROUP (UIA cannot observe the needed elements); S07 on JDK
  (drag started without foreground; on JBR the snap itself is proven by the rect landing
  exactly on a real zone).

## Real-input duration

- Pass 1 (JDK 21): 272.8 s inside the pass (launch-to-teardown, all input scripted).
- Pass 2 (JBR 21): 255.2 s.
- Whole run 3 (env steps + both passes + restore): 572.7 s (~9 m 33 s).
- The two aborted env-setup attempts and the driver repair sent NO real mouse/keyboard input
  (2 UAC confirmations by the maintainer, devcon/registry/module work only). Total
  maintainer hands-off window: ~2 h; hands-off time actually spent moving the mouse/keyboard:
  ~8.8 min.
