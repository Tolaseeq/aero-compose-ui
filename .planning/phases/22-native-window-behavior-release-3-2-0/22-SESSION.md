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

## Teardown

Plan 22-16 Task 2, 2026-09-28. Maintainer's replies (checkpoint Task 1, recorded verbatim by
the orchestrator): «1 оставь» — PowerToys KEPT; «2 ок» — the UAC prompt for virtual-display
driver removal approved. Script: `.captures/22-final/Invoke-Teardown.ps1` (git-ignored).
No real mouse/keyboard input was sent — the session object authorized environment changes
only.

### Virtual display driver (removed, verified from machine state)

- **Before:** screens=1 (the virtual display never attached on this machine); the device
  existed as `ROOT\DISPLAY\0000` (class Display, friendly name "Virtual Display Driver",
  Status OK) — `vddDeviceCount=1 instances=[ROOT\DISPLAY\0000|OK]`; the `ROOT\MttVDD\*`
  instance pattern was empty (the instance path never carries the hardware id, 22-15's own
  presence-check finding); the driver store still held the package as `oem73.inf`
  (`mttvdd.inf`, Provider MikeTheTech, class Display).
- **Action:** `Remove-AeroVirtualDisplay` → `devcon.exe remove "Root\MttVDD"` elevated (UAC
  confirmed by the maintainer, exit code 0), then `pnputil /delete-driver oem73.inf`
  elevated (exit 0) to remove the store package a device removal leaves behind.
- **After (verification output):**
  ```
  TEARDOWN DRIVER before=screens=1 after=screens=1 present=False exitCode=0 verified=True
  TEARDOWN AFTER screens=1 vddAnyStatusCount=0 rootMttVddCount=0 autoHideOn=True stuckByte8=0x03 virtualDisplayPresent=False
  ```
  Store re-check after pnputil: `mttvdd` occurrences in `pnputil /enum-drivers` = 0;
  `Get-PnpDevice -InstanceId 'ROOT\MttVDD\*'` returns nothing (the verification command
  from 22-SESSION-ENV.md); class-Display "Virtual Display Driver" devices of ANY status = 0;
  `[System.Windows.Forms.Screen]::AllScreens.Count` = 1 (the pre-session value).

### Taskbar auto-hide (verified, no restore needed)

- **Before:** `TaskbarAutoHideOn=True`, `StuckRects3` `Settings` byte 8 = `0x03` — the
  recorded original from `## Readiness`.
- **Action:** none needed — 22-15's end-of-run restore held.
- **After:** `TEARDOWN AUTOHIDE final autoHideOn=True stuckByte8=0x03
  equalsRecordedOriginal=True restoreWasNeeded=False`.

### PowerToys (KEPT per the maintainer's word)

- **Before:** installed (`%LOCALAPPDATA%\PowerToys\PowerToys.exe` present), running,
  FancyZones running.
- **Action:** none — the maintainer answered «1 оставь» (keep); removal was never attempted.
- **After:** `TEARDOWN POWERTOYS decision=KEPT per maintainer «1 оставь» installed=True
  running=True exe=True`.

### Session residue beyond the plan's checklist (named, not silently left)

- **DisplayConfig PowerShell module 1.1.1 (CurrentUser)** — installed during the 22-15
  driver repair; the pre-session state is recorded as absent in `## Minutes estimate`
  ("confirmed absent today"). **Removed** this teardown (`Uninstall-Module DisplayConfig`,
  re-checked absent). The pre-state is known, so restoring it is not a guess.
- **PSGallery `InstallationPolicy=Trusted`** — also set during the 22-15 repair. The
  pre-session value was never recorded, so reverting it to Untrusted would be a guess;
  recorded here instead of silently changed. Effect: future `Install-Module` from PSGallery
  does not prompt. Harmless per-user profile setting; the maintainer can revert with
  `Set-PSRepository PSGallery -InstallationPolicy Untrusted` if they want the prompt back.
- **Scratchpad** `C:\Users\1\AppData\Local\Temp\aero-22-session\` (installers, zips,
  `driver-flat`, diag logs) — deleted after the removal succeeded, existence re-checked
  (gone). `git status --porcelain` carries no scratch or capture path.

Machine state = pre-session state except PowerToys (kept on the maintainer's word) and the
PSGallery trust flag (named above). VER-12's end-of-session clauses are satisfied: driver
removed (verified), auto-hide on (verified), PowerToys per the maintainer's word (kept).

## Gap-closure re-verification readiness (22-25 Task 1)

Recorded 2026-09-28, before any consent was asked. Everything below ran headless: no real
mouse/keyboard input, no installs, no settings changes. Evidence: `.captures/22-reverify/`
(readiness.log + per-run transcripts; mirrored to the main checkout at
`C:\1A_WORK\ui_lib\.captures\22-reverify\`). Worktree base 0a9f74c — all five gap-closure
plans (22-20..22-24) included.

### Builds

- `./gradlew :showcase:classes --console=plain` → BUILD SUCCESSFUL.
- Full unfiltered `./gradlew :library:test --rerun --console=plain` → BUILD SUCCESSFUL,
  `AERO_TEST_COUNT total=596 skipped=0 expected=596 expectedSkipped=0 filtered=false` — the
  locked count raised by 22-20 holds on the gap-closure build.

### V11 regression sweep (capture-mode launch, both windows, no -SkipMaximize)

- GREEN (native path): `V11 SUMMARY main pass=18 fail=0` including V11-MAX-WORKAREA PASS
  (client bottom inset 2 px for the auto-hide taskbar) and V11-AUTOHIDE-EDGE PASS (Bottom=2);
  `V11 SUMMARY narrow pass=8 fail=0` including V11-N-MINSIZE exactly 260x200 (D-01);
  chromeTraceLines=46 on the launch; teardown remainingMainKt=0.
- RED control (`-Paero.nativeChrome=false`, style 0x960B0000): main pass=0 fail=18, narrow
  pass=0 fail=8 (nothing passes — the suite's own A04 rule; 22-15 had recorded fail=17 with
  one SKIP, today all 18 fail), chromeTraceLines=0, teardown remainingMainKt=0.

### Subset dry runs (`-DryRun -Phase Checks`, no input, no installs)

JDK list (23 Ids): S03-WIN-DOWN, S04-DBLCLICK-MAX, S04-DBLCLICK-RESTORE, S05-ALTSPACE-MENU,
S05-MOVE, S05-SIZE, S05-MINIMIZE, S05-MAXIMIZE, S05-RESTORE, S05-CLOSE-VS-ALTF4,
S06-SHARED-BORDER, S07-FANCYZONES, W02-CORNER-TL, W02-CORNER-TR, W02-CORNER-BL,
W02-CORNER-BR, W02-MAIN-FLOOR, W02-NARROW-FLOOR, W06-CLOSE-DURING-DRAG, B01-PRESS-FRAME,
B01-HOVER-FRAME, B01-CLICK, S02-FLYOUT.
JBR list (32 Ids) = the JDK list PLUS S01-LEFT-HALF, S01-RIGHT-HALF, S01-QUARTER-TL,
S01-TOP-MAXIMIZE, S01-DRAG-AWAY-RESTORE, S03-WIN-LEFT, S03-WIN-RIGHT, S03-WIN-UP,
W05-PLACEMENT (the nine 22-21 confirmation-spec drift Ids, byte-identical to 22-21 T2).
B01-HOVER-FRAME, B01-CLICK and S02-FLYOUT are blast-radius guards for the 22-23/22-20
AeroWndProc changes, not FAIL re-runs. The -NoReset dependent pairs
(S04-DBLCLICK-RESTORE after S04-DBLCLICK-MAX, S05-RESTORE after S05-MAXIMIZE,
S01-DRAG-AWAY-RESTORE after S01-TOP-MAXIMIZE) are complete in both lists.

- jdk pass: **exit=0**, idsSeen=23/23 (each exactly once, no others), 0 check errors,
  totals unconfirmed=23 (dry), cursor 1341,926 → 1341,926, env identical, remainingMainKt=0,
  wall-clock **36 s**; jvm=standard (ms-21.0.9).
- jbr pass: **exit=0**, idsSeen=32/32, 0 check errors, cursor unchanged, remainingMainKt=0,
  wall-clock **41 s**; jvm=JBR (jbr-21.0.9).

### Environment pre-check (read-only; recorded, never toggled)

- screens=1; monitor 0,0,1920,1080 work 0,0,1920,1080, 96 DPI (scale 1.0).
- TaskbarAutoHideOn=True, StuckRects3 byte 8 = **0x03** — recorded; the session NEVER toggles
  it (none of the 23/32 subset checks requires a visible taskbar).
- PowerToysInstalled=True, PowerToysRunning=True, FancyZonesRunning=True (no per-user start
  needed — the conditional start path stayed untaken). VirtualDisplayPresent=False and NO
  driver step of any kind exists in this session (W04 stays UNCONFIRMED by design).
- Both JVM paths exist: `C:\Users\1\.jdks\ms-21.0.9\bin\java.exe`,
  `C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe`.

### Minutes estimate for the maintainer (measured terms only)

| Component | Basis | Time |
|-----------|-------|------|
| Mechanics of both passes (launches incl. hotRun, waits, resets, probe cycles, frame captures) | measured dry-run wall-clocks above | 36 s + 41 s = 77 s ≈ 1.3 min |
| Real-only input time (per-check Start-Sleep + drag durations + hover minimums + reopen waits summed from the selected bodies) | computed from Invoke-FullSession.ps1 (per-term table in readiness.log) | jdk 63.1 s + jbr 78.9 s = 142 s ≈ 2.4 min |
| Environment steps | measured (PowerToys already running; auto-hide contributes zero — never toggled) | 0 min |
| **Subtotal** | 77 + 142 | **≈ 3.6 min** |
| Round up, add 3 (plan's formula) | ceil + 3 | **N = 7 min** |

**Readiness verdict:** everything provable without real input is proven — builds green at the
locked 596, V11 18/18 + 8/8 with a clean RED control on the gap-closure build, both subset
dry-run invocations of the 22-22 contract at exit 0 with exactly the selected Ids, the
environment recorded read-only, and a measured 7-minute estimate. The session can start the
moment consent arrives.

## Gap-closure re-verification (22-25)

Consent: maintainer's «ок» @ 2026-09-28 (chat, in response to the presented warning);
`-AuthorizedBy "maintainer ok via chat 2026-09-28"` on both passes. JDK pass started
17:33:21Z, JBR pass 17:48:31Z–17:50:37Z. Evidence: `.captures/22-reverify/jdk/` (23 Ids,
session mechanics 124.1 s) and `.captures/22-reverify/jbr/` (32 Ids, 124.9 s) with console
logs `jdk-console.log` / `jbr-console.log` alongside; per-pass `results.json`,
`summary.json`, `frames/`, `logs/`. The JDK-side wall-clock was dominated by a cold Gradle
launch (~9 min before the pass window) — the readiness estimate's mechanics term was measured
on a warm daemon; the real-input core itself matched the estimate.

**Totals: JDK pass=10 fail=12 unconfirmed=1 (23). JBR pass=13 fail=18 unconfirmed=1 (32).**

### Pass tables (observed rows)

| Id | JDK 21 | JBR 21 |
|----|--------|--------|
| S01-LEFT-HALF | not in JDK subset | FAIL — drag caption (891,156)->(1,540); rect unchanged 360,140,1560,940 |
| S01-RIGHT-HALF | — | FAIL — rect unchanged |
| S01-QUARTER-TL | — | FAIL — rect unchanged |
| S01-TOP-MAXIMIZE | — | FAIL — isZoomed=False placement=Floating |
| S01-DRAG-AWAY-RESTORE | — | PASS — restored=1200x800 (vacuous: TOP-MAXIMIZE had failed, window never left floating) |
| S02-FLYOUT | UNCONFIRMED — reference control FOUND (signal=Event 708 ms) but check threw: `The property 'EventName' cannot be found` | UNCONFIRMED — same error (reference found, 652 ms) |
| S03-WIN-LEFT | not in JDK subset | FAIL — foregroundOurs=False (no retry wired); chord inert |
| S03-WIN-RIGHT | — | PASS — foregroundOurs=True; rect=960,0,1920,1080 |
| S03-WIN-UP | — | PASS — foregroundOurs=True; isZoomed=True placement=Maximized |
| S03-WIN-DOWN | FAIL — foregroundOurs=False; zoomedPrecondition=True, no restore, no minimize | PASS — foregroundOurs=True; restore + minimize both landed |
| S04-DBLCLICK-MAX | FAIL — isZoomed=False (floating start, reset verified) | PASS — isZoomed=True |
| S04-DBLCLICK-RESTORE | PASS — 1200x800 (vacuous: MAX had failed) | PASS — restored 1200x800 (real: MAX had passed) |
| S05-ALTSPACE-MENU | FAIL — foregroundOurs=False; no #32768 of our process | PASS — foregroundOurs=True; #32768-of-our-process=True |
| S05-MOVE | FAIL — foregroundOurs=False; dx=0 | FAIL — foregroundOurs=True, menu open; moved dx=0 dy=0 |
| S05-SIZE | FAIL — foregroundOurs=False; 0x0 | FAIL — menu open; resized 0x0 (window left 160x28 at the S05-RESTORE sample — state drift during the attempt) |
| S05-MINIMIZE | FAIL — foregroundOurs=False | FAIL — menu open; isIconic=False |
| S05-MAXIMIZE | FAIL — foregroundOurs=False | FAIL — menu open; isZoomed=False |
| S05-RESTORE | PASS — vacuous (window already floating) | FAIL — precondition broken (MAX failed); sample size=160x28 |
| S05-CLOSE-VS-ALTF4 | FAIL — both halves blocked (menuClose=False, altF4Event=False) | FAIL — menu-close inert; Alt+F4 half PROVEN: altF4Event=True, narrow destroyed |
| S06-SHARED-BORDER | FAIL — preSnap mainSnapped=True narrowSnapped=False (narrow focus foregroundOurs=False) | FAIL — same: narrow pre-snap chord foreground-blocked |
| S07-FANCYZONES | FAIL — foreground False, reverify ours=False after one retry; drag did not snap; zonesFound=True (priority-grid, 3 zones, spacing 16) | FAIL — same shape after the one retry |
| W02-CORNER-TL | FAIL — shape=Other, deltas 0x0 | FAIL — shape=Arrow, deltas 0x0 |
| W02-CORNER-TR | PASS — SizeNESW, 60x60 | PASS — SizeNESW, 60x60 |
| W02-CORNER-BL | PASS — SizeNESW, 60x60 | PASS — SizeNESW, 60x60 |
| W02-CORNER-BR | PASS — SizeNWSE, 60x60 | PASS — SizeNWSE, 60x60 |
| W02-MAIN-FLOOR | PASS — width=320 exact, height 800 unchanged | PASS — same |
| W02-NARROW-FLOOR | PASS — width=260 exact (D-01), height 480 unchanged | PASS — same |
| W05-PLACEMENT | not in JDK subset | FAIL — placementSamples=16 mismatches=0; float1-vs-float2 diff=0 (22-15 instability NOT reproduced); float-vs-maximized diff=0 (glyph-switch capture flat this run) |
| W06-CLOSE-DURING-DRAG | FAIL — closeEvent=True, mainAlive, v11=0, hs_err=0; ncdestroy=False within the 10 s budget | PASS — closeEvent=True ncdestroy=True; mainAlive; v11=0; hs_err=0 |
| B01-HOVER-FRAME | PASS — hover parity 0; hover-vs-rest 144 | FAIL — all diffs 0 (hover visual not observed in frames) |
| B01-PRESS-FRAME | PASS — press parity 0; press-vs-rest 144 | FAIL — all diffs 0 (press visual not observed) |
| B01-CLICK | PASS — foregroundOurs=True; toggled Maximized/Floating | FAIL — foregroundOurs=False; both clicks zoomed=False |

### The environment anomaly (dominant, both JVMs, intermittent)

A foreign window — VS Code, pid 12752, an elevated `[Administrator]` window, the same pid
the 22-21 battery recorded as the external foreground holder — held or reclaimed the
foreground while `Invoke-SessionFocus`'s real caption click + 350 ms check ran
(`foregroundOurs=False`). The JDK pass was affected through its whole middle (S03-DOWN
through S07); JBR intermittently (S03-LEFT, S06 narrow focus, S07, B01-CLICK). Every
foreground-gated gesture inside those windows failed as a direct consequence: the chords,
Alt+Space, menu navigation and Alt+F4 were delivered to the foreground holder, not to the
showcase window. Two consequences recorded honestly:

- The blocked rows are NOT product verdicts for the fixed behaviors; they are
  environment-blocked outcomes with the foreground line attached.
- Stray input reached the foreground holder (Alt+Space/arrows/Alt+F4, caption clicks and
  drags at y=156 — editor-area drags, not VS Code's title bar). VS Code remained running and
  foreground after both passes (probed); the maintainer should check their editor/chat for
  stray characters all the same.

### Differences against the 22-15 rows

- **S03-WIN-DOWN JBR: FAIL -> PASS** (with foreground=True; restore + minimize both
  landing) — the 22-15 JBR chord inertness was foreground-shaped, as 22-21's leading
  explanation predicted. S03-RIGHT/UP JBR PASS with foreground=True; LEFT failed on a
  foreground fluke the script did not retry (see harness gaps).
- **S04-DBLCLICK-MAX: FAIL/FAIL -> FAIL/PASS** — the 22-20 fix fires on JBR (real
  MAX+RESTORE pair) but did NOT fire on JDK this run from a verified floating reset. A
  JVM-differential the record cannot explain.
- **S05-ALTSPACE-MENU JBR: FAIL -> PASS** — the 22-23 hand-declared system menu opens
  (#32768 of our pid, foreground=True). The S05 failure MODE changed on JBR: was
  "menu never opens", now "menu opens, command navigation inert" (Move/Size/Minimize/
  Maximize/Close all produce no state change with the menu open and the window foreground).
  The S05-SIZE attempt left the window at 160x28 (partial engagement evidence).
- **W02 corners/floors: FAIL (formula artifacts) -> PASS** on TR/BL/BR and both floors on
  BOTH JVMs — the 22-22 formula fixes work. W02-CORNER-TL is a NEW anomaly (both JVMs,
  zero deltas, shape Arrow/Other — the first corner runs immediately after S07).
- **W06: FAIL/FAIL -> FAIL/PASS** — ncdestroy IS emitted on JBR (the 22-22 open question
  decided for JBR: not pipe-lag); JDK did not emit within the 10 s budget despite the close
  path itself being green (closeEvent, alive, v11=0, hs_err=0).
- **B01-PRESS-FRAME: FAIL/FAIL -> PASS/FAIL** — the press-parity fix (C5) is PROVEN on JDK
  (parity 0, press paints 144 px). The JBR FAIL is the zero-diff cluster (see below), not a
  parity difference.
- **S07 JBR: FAIL(proven snap, formula) -> FAIL(no snap, foreground)** — 22-15's JBR row
  had proven the snap itself; today's drag never started foreground-ours even after the one
  retry, so no snap occurred. Weaker evidence day for S07, same verdict letter.
- **W05 JBR: FAIL(crop instability 616 px) -> FAIL(glyph-switch flat)** — the 22-15
  floating-crop instability did NOT reproduce (float1-vs-float2 = 0; placement 16/16);
  instead the float-vs-maximized glyph-switch capture showed no difference this run.
- **S01 JBR: FAIL -> FAIL** — the caption-drag inertness reproduced, now with stronger
  discrimination: on the same run, a double-click at the same caption point (891,156)
  maximized the window (S04 PASS), chords worked with foreground (S03 RIGHT/UP/DOWN PASS),
  and edge drags worked (W02) — so the drift is not hit-test, not foreground-per-se, and
  not click delivery; the drag modal loop specifically does not move the JBR window.

### The JBR tail cluster (B01 triple, zero diffs)

B01-HOVER, B01-PRESS and B01-CLICK ran consecutively at the very end of the JBR pass and
all three show the same shape: nothing changed in the captured frames (hover/press diffs 0)
and the maximize-button clicks did not toggle (with the focus click reporting
foregroundOurs=False). On JDK the same three rows PASS/PASS/PASS with
foregroundOurs=True. The cluster is consistent with the maximize-button region not being
interactive/visible at that moment on JBR (foreign-window occlusion or activation state);
it is NOT a press-parity difference (parity is proven on JDK, and a parity FAIL would show
nonzero press frames differing between max and min, not zero-everything).

### Harness gaps found (recorded, not fixed in-session)

1. `S02-FLYOUT` throws while formatting evidence — `$obs2Evidence.Events.EventName` fails
   when the event collection has an unexpected shape ("The property 'EventName' cannot be
   found"), so the check lands UNCONFIRMED despite the reference control matching
   (signal=Event, 708/652 ms). The flyout-gate blast-radius guard is therefore unproven
   today, on both JVMs, by harness error rather than by observation.
2. The 22-21 retry rule (one re-focus + one chord repeat before any FAIL on the S01/S03
   rows) is NOT wired into `Invoke-SessionHotkeyCheck` — it single-focuses and judges. Only
   S07 carries a retry ("foregroundReverify ours=False after one retry"). The JBR
   S03-WIN-LEFT FAIL was judged without the mandated retry; S03-WIN-RIGHT passing with
   foreground=True a second later shows the fluke shape.
3. The JDK pass's exit code was not recorded (the wrapper's follow-up echo was lost when
   the 600 s foreground timeout moved the command to background); completion itself is
   fully evidenced (totals, env-after, summary.json).

### Maintainer observations

None beyond the consent: the maintainer's only input in the window was «ок» (the plan's
optional observations — hover, flyout, zone click, taskbar group — were not offered as
separate prompts inside the hands-off window; recorded as declined-by-protocol, not as
negative observations).

### Teardown (verified)

- `StuckRects3` byte 8 = **0x03** after both passes — auto-hide unchanged ON, never
  toggled (read-only checks before/after; `EnvUnchanged=True` in both summaries).
- Cursor restored (JDK 373,777 -> 373,777; JBR 269,728 -> 269,728; independent probe after
  the session: 269,728).
- Zero leftover `com.mordred.showcase.MainKt` JVMs (independent probe after the session:
  0; both passes PID-tracked).
- screens=1, primary 1920x1080 @ 96 DPI; PowerToys installed+running, FancyZones running
  (`Get-AeroEnvState`); VirtualDisplayPresent=False; no driver step of any kind ran; no
  UAC occurred.
