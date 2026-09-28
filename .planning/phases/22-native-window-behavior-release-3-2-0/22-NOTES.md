# Phase 22 Notes — Empirical findings, conflicts, RED baseline

Recorded by Plan 01 (Task 3), 2026-09-25, against the genuinely unmodified showcase window
(`git diff --quiet HEAD -- library/` holds throughout; `grep -rn "com.sun.jna" library/src`
returns nothing). Measured with `tools/winprobe/Invoke-WinProbe.ps1 -Launch run` on this
machine: Windows 11 24H2 build 26100, one 1920x1080 monitor at 100% DPI, taskbar auto-hide ON,
default `java` = Microsoft OpenJDK 21.0.9 (`C:\Users\1\.jdks\ms-21.0.9`), capture mode
(`-Paero.capture=true`, non-focusable, `window.toBack()` on start).

Each entry below: Question / Check performed / Observed / Decision / Settled by.

## Conflicts

### C1

**Question:** Which style bits does `undecorated = true` actually yield on JDK 21 (standard,
not JBR)?

**Check performed:** `GetWindowLongPtr(hwnd, GWL_STYLE)` and `GWL_EXSTYLE` read-back against the
live, unmodified showcase window via `Get-WinProbeWindowInfo`.

**Observed:** `GWL_STYLE = 0x960B0000` decodes to `WS_POPUP | WS_SYSMENU | WS_MINIMIZEBOX |
WS_MAXIMIZEBOX | WS_CLIPCHILDREN` — `WS_CAPTION` and `WS_THICKFRAME` are absent, everything else
present. `GWL_EXSTYLE = 0x08000000` = `WS_EX_NOACTIVATE` only.

**Decision:** PITFALLS.md's `awt_Frame.cpp`-derived prediction (`WS_POPUP | WS_SYSMENU |
WS_CLIPCHILDREN | WS_MAXIMIZEBOX | WS_MINIMIZEBOX`, ext style 0) is confirmed exactly for the
style bits. The one addition — `WS_EX_NOACTIVATE` on `GWL_EXSTYLE` — comes from this probe run's
own capture mode (`focusable = false` in `Main.kt`'s `Window(...)` call adds that ex-style bit on
Swing/AWT); a normal, non-capture launch has `GWL_EXSTYLE = 0`. Step 2 ("Window styles") must add
`WS_CAPTION | WS_THICKFRAME` and must not assume `WS_EX_NOACTIVATE` is present outside capture
mode.

**Settled by:** 22-01 T3.

### C2

**Question:** Is Alt+Space automatic once `WS_SYSMENU`/`WS_CAPTION` are present, or does it need
explicit `WM_SYSCOMMAND`/`GetSystemMenu`/`TrackPopupMenu` handling?

**Check performed (22-15 real-input session, both JVMs):** Real Alt+Space on the native-chrome
showcase window (`S05-ALTSPACE-MENU`: #32768-menu-of-our-process check) versus the same chord on
the opt-out window (`C02-OPTOUT-ALTSPACE`, `-Paero.nativeChrome=false` legacy path, own launch),
then the menu commands Move/Size/Minimize/Maximize/Close via Home/End + arrows + Enter on both
window kinds.

**Observed (identical on JDK 21 and JBR 21):** On the NATIVE window Alt+Space opens NO system
menu (`#32768-of-our-process=False`), and consequently Move/Size/Minimize move/resize nothing
(`dx=0`, `dWidth=0`, `isIconic=False`); the menu-Close half of `S05-CLOSE-VS-ALTF4` fails the
same way while its Alt+F4 half is PROVEN (`AERO_EVENT name=close-request label=narrow`, window
destroyed, reopened). On the OPT-OUT window the same chord DOES open the menu
(`menuVisible=True`), and menu-Move really moves the window (`moveDx=220`). One fluke: JBR's
`S05-MAXIMIZE` landed (a menu opened that once) — recorded as noise, not a working path.

**Decision:** C2 SETTLED — Alt+Space is NOT automatic on the native path: the style bits alone
are not enough because the subclass forwards what it does not own into AWT's WndProc (which
swallows the system-menu trigger) instead of `DefWindowProc`. This is exactly the
hand-declare-`GetSystemMenu`/`TrackPopupMenu` case named in the plan (they are absent from
jna-platform 5.19.1, 22-10's grep proof): a FAIL finding for gap closure — SNAP-05's menu
commands (and S04's double-click maximize, which failed the same session) need explicit
non-client handling.

**Settled by:** 22-15 T3 (`.captures/22-session/{jdk,jbr}/results.json`, checks `S05-*`,
`C02-OPTOUT-ALTSPACE`; console `.captures/22-session/console-3.log`).

### C3

**Question:** Auto-hide taskbar inset size (1px vs 2px) and edge-detection method.

**Check performed:** `SHAppBarMessage(ABM_GETSTATE)` and `ABM_GETAUTOHIDEBAREX` per edge via
`Get-WinProbeTaskbar` against the window's own monitor.

**Observed:** `AutoHideOn = True`; only the `Bottom` edge reports an active auto-hide bar
(`AutoHideEdges = Bottom`). `rcMonitor` and `rcWork` are both `(0,0)-(1920,1080)` — identical,
because with auto-hide ON the OS does not reserve taskbar space in the work area at all.

**Decision:** Detection half is settled (edge = Bottom, and `rcWork == rcMonitor` on an
auto-hide monitor — a detail worth carrying into Step 3's math: the 2px-inset code cannot rely on
`rcWork` alone to know an edge needs insetting, since `rcWork` here already equals the full
monitor). The inset-size implementation and hover-reveal confirmation remain open.

**Settled by:** 22-05 T3 + 22-15 T3 (reveal half below).

**Reveal half (22-15 real-input session):** with the window maximized and the taskbar
auto-hidden, a real mouse hover on the bottom screen row over the 2 px inset reveals the
taskbar — `W01-AUTOHIDE-REVEAL` PASS on both JVMs (revealed after 232 ms on JDK 21 and
231 ms on JBR 21; probe details recorded in each pass's `results.json`). C3 fully SETTLED:
detection (ABM_GETSTATE + ABM_GETAUTOHIDEBAREX per edge), inset (2 px,
`AUTO_HIDE_INSET_PX`), and reveal-on-hover are all proven with real input.

### C4

**Question:** Does `WindowState.placement` sync automatically on an OS-originated maximize, or
does it need an explicit push from the native subclass?

**Check performed:** Headless `SendMessage(hwnd, WM_SYSCOMMAND, SC_MAXIMIZE, 0)` against the
unmodified window, immediately followed by `SetWindowPos(HWND_BOTTOM, SWP_NOACTIVATE)`, then read
the `AERO_WINDOW_STATE` line the Task 1 reporter printed afterward.

**Observed (baseline, no native code exists yet):**
```
AERO_WINDOW_STATE label=main placement=Maximized minimized=false awtExtendedState=6
sizeDp=1920.0x1080.0 posDp=0.0,0.0 awtBounds=0,0,1920,1080 insets=0,0,0,0 scale=1.0
minSizeSet=false minSize=1x1 resizable=true jvm=21.0.9/Microsoft
```
`WindowRectMaximized = (0,0)-(1920,1080)`, exactly `MonitorRcWork`. A subsequent
`SC_RESTORE` + `HWND_BOTTOM` was also sent (restore behavior not separately captured in this
baseline pass).

**Decision:** On the pre-phase code, a plain `SC_MAXIMIZE` sent to the frame already flips
`WindowState.placement` to `Maximized` and resizes to the full monitor — CMP's own
`WM_SIZE`-forwarding pipeline already handles an externally-triggered maximize with **zero**
native code. This is the baseline ARCHITECTURE.md's simpler claim needs to be compared against
once Step 4's hit-testing exists (does installing the subclass change or break this?). Not yet
proof that a *native-driven* SC_MAXIMIZE forwarded through the new subclass behaves identically —
only that today's code, with no subclass installed, already syncs correctly for a raw
`WM_SYSCOMMAND`.

**Settled by:** 22-02 T3 (compares the post-subclass behavior against this exact baseline).

### C5

**Question:** Max-button interaction approach — FlatLaf-style non-client message forwarding vs. a
direct native-to-`State<Boolean>` EDT-hop bridge.

**Check performed:** Not checked here — needs the `AeroMaxButtonInteraction` bridge to exist
first.

**Decision:** Deferred, per SUMMARY.md's own recommended order (build the state-bridge approach
first, fall back to message-forwarding only if it demonstrably fails).

**Settled by:** 22-09 T3.

### C6

**Question:** Are `DwmExtendFrameIntoClientArea` / `DWMWA_WINDOW_CORNER_PREFERENCE` needed at all?

**Check performed:** Not checked here — needs a `PrintWindow` before/after comparison around a
headless `SC_MAXIMIZE`/`SC_RESTORE` toggle, which this task's baseline captures (`AeroBlue-rest.png`,
`AeroBlue-maximized.png`) provide the "before" half of. The rest frame shows a plain rectangular
undecorated window — no rounded corners or drop shadow are visible today (expected: `WS_CAPTION`
is absent, and Windows 11's automatic corner rounding only applies to windows DWM classifies as
having a caption).

**Decision:** Deferred — the "after" comparison needs the native chrome to exist.

**Settled by:** 22-06.

### C7

**Question:** Does JBR's own custom-window-decoration machinery interfere with the new subclass
under `hotRun`?

**Check performed (grep half, SETTLED here):**
```
grep -rnE "JBR|CustomWindowDecoration|jetbrains\.runtime|com\.jetbrains" --include=*.kt --include=*.kts .
```

**Observed:** Zero matches, repo-wide (re-confirmed live in this task; matches the planner's own
finding in 22-RESEARCH.md).

**Decision:** JBR's own decoration machinery is not engaged anywhere in this codebase today —
there is nothing to disable. The remaining risk is purely the Hot Reload re-run /
double-subclass-install idempotency risk (Pitfall 16), unrelated to JBR specifically, and needs an
idempotent-install guard regardless of this finding.

**Settled by:** 22-01 T3 (grep half). Runtime half (cold `./gradlew run` vs `hotRun` behavioral
drift) → 22-02 T3.

## Findings

### F8

**Question:** Does the Compose content live in a child HWND that receives `WM_NCHITTEST` before
the frame (Skiko's `HardwareLayer extends java.awt.Canvas`)?

**Check performed:** `Get-WinProbeChildren` (full descendant enumeration) plus
`Invoke-WinProbeHitTest`'s real chain (`ChildWindowFromPointEx` descent + `WM_NCHITTEST` sent to
the deepest child, vs. `DirectFrameCode` sent straight to the frame) at all 14 named points.

**Observed:** Exactly one descendant HWND: class `SunAwtCanvas`, rect `(0,0)-(1200,800)` —
`CoversClient = True` (fills the entire client area), visible. Every hit-test point's chain is a
single hop: `SunAwtCanvas:1` (the child answers `HTCLIENT` directly, never `HTTRANSPARENT`, so no
parent bounce occurs at any of the 14 points). `DirectFrameCode` (sent straight to the frame,
bypassing the child) is also `1` (`HTCLIENT`) at every point tested — today, with no native
subclass installed, the frame's own default `WndProc` and the child's default `WndProc` agree
everywhere, so this baseline alone cannot distinguish "child answers first" from "frame answers
first". What it does confirm: the single child (`SunAwtCanvas`, Skiko's `HardwareLayer`) exists,
covers the whole client area, and sits directly under the frame in the window tree — exactly
finding F8 predicted. **Plan 02+ must install the hit-test subclass on this child, not only the
frame** — a subclass on the frame alone would never see the message, because
`ChildWindowFromPointEx` from the real OS input path would hand `WM_NCHITTEST` to `SunAwtCanvas`
first for every point inside the client area.

Both the frame's own `GWLP_WNDPROC` and the child's `GWLP_WNDPROC`, read cross-process via
`GetWindowLongPtr`, come back as `0` — a known Windows behavior (querying another process's
window-procedure pointer is meaningless across the process boundary and reads back zero
regardless of whether a subclass exists). This probe's `WndProc` field is a diagnostic-only value;
it cannot be used to detect subclass installation from outside the process once Plan 02 lands, and
that verification will need an in-process signal instead (e.g. the region-registry state itself).

**Settled by:** 22-01 T3 (this finding). Acted on by 22-02+ (subclass target).

### F9

**Question:** Does a probe-driven `SC_MAXIMIZE` take the foreground from the maintainer?

**Check performed:** `Invoke-WinProbeMaximize` and the baseline-capture script both record the
foreground window's owning process before, during, and after a headless `SC_MAXIMIZE` +
immediate `SetWindowPos(HWND_BOTTOM, SWP_NOACTIVATE)`, three separate times (the standalone
`maximize` report, the `v11` check's own maximize cycle, and the `AeroBlue` baseline-frame
maximize).

**Observed:** `ForegroundTaken = False` and `ForegroundAfterRestore = False` in all three runs —
the showcase process's window was never the foreground window during or after a probe-driven
maximize.

**Decision:** F9 = **safe**. Maximize-dependent `V11-*` checks (and this task's `AeroBlue-maximized.png`
baseline frame) can run in headless probe sessions outside a real-input session; `-SkipMaximize`
is not required by default on this machine, though it remains available for hosts where this
does not hold.

**Settled by:** 22-01 T3.

### F10

**Question:** Is CMP 1.12.0's own `UndecoratedWindowResizer` active on the current showcase
window today?

**Check performed:** The Task 1 reporter's `resizable=` field, read via the `AERO_WINDOW_STATE`
line captured during the C4 maximize check, combined with the planner's own source read of
`ComposeWindowPanel.desktop.kt:214` (`undecoratedWindowResizer.enabled = isUndecorated &&
isResizable`, already recorded in 22-01-PLAN.md's `<interfaces>` block).

**Observed:** `resizable=true` (Main.kt's `Window(...)` call does not set `resizable = false`, so
it keeps CMP's default of `true`); the window is `undecorated = true`. Both conditions
`UndecoratedWindowResizer.enabled` requires are met today.

**Decision:** F10 = CMP's own 8dp resizer is active on the showcase window right now, on top of
the existing `AeroResizeHandles` — confirming there are genuinely two competing Compose-side
resize paths today, as the milestone research flagged. WIN-02's gating change in
`ResizeHandles.kt` needs to account for CMP's own resizer separately (it is not something
`AeroResizeHandles`'s own early-return can silence — the two are unrelated composables).

**Settled by:** 22-01 T3.

### F11

**Question:** What are `minSizeSet`/`minSize` for a CMP window whose app sets no minimum size?

**Check performed:** Same `AERO_WINDOW_STATE` line as C4/F10, plus `Invoke-WinProbeMinSize`'s own
50x50px `SetWindowPos` probe.

**Observed:** `minSizeSet=false`, `minSize=1x1` (AWT's unset-minimum default) from the reporter.
`Invoke-WinProbeMinSize` requested 50x50 physical pixels and the window actually shrank to
exactly 50x50 — nothing today clamps the window below any floor at all.

**Decision:** F11 confirms D-01's precondition: `Component.isMinimumSizeSet()` is `false` when
the app (Main.kt) sets nothing, exactly as D-01 assumed. The 320x240dp floor is entirely new
behavior to add; today there is no floor whatsoever below which the window can be resized.

**Settled by:** 22-01 T3.

## RED baseline (22-01)

Full `V11` table from `Invoke-WinProbeV11` against the unmodified window (main, 1200x800dp,
scale 1.0, capture mode). Every check FAILs; none SKIP (auto-hide edge exists on this machine, and
`-SkipMaximize` was not used since F9 = safe).

```
V11 V11-STYLE FAIL expected=WS_CAPTION,WS_SYSMENU,WS_THICKFRAME,WS_MINIMIZEBOX,WS_MAXIMIZEBOX observed=WS_POPUP,WS_SYSMENU,WS_MINIMIZEBOX,WS_MAXIMIZEBOX,WS_CLIPCHILDREN
V11 V11-HT-CAPTION FAIL expected=2 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-MAX FAIL expected=9 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-MIN-BOUNDARY FAIL expected=min=1,captionLeftOfMin=2 observed=min=1,captionLeftOfMin=1
V11 V11-HT-CLOSE-BOUNDARY FAIL expected=close=1,max=9 observed=close=1,max=1
V11 V11-HT-EDGE-L FAIL expected=10 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-EDGE-R FAIL expected=11 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-EDGE-T FAIL expected=12 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-EDGE-B FAIL expected=15 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-CORNER-TL FAIL expected=13 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-CORNER-TR FAIL expected=14 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-CORNER-BL FAIL expected=16 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-CORNER-BR FAIL expected=17 observed=1 (HTCLIENT) chain=SunAwtCanvas:1
V11 V11-HT-CLIENT-BOUNDARY FAIL expected=client=1,edgeLeft=10 observed=client=1,edgeLeft=1
V11 V11-HT-MAXIMIZED-TOP FAIL expected=2 observed=1
V11 V11-MAX-WORKAREA FAIL expected=rcWork=0,0,1920,1080 autoHideEdges=Bottom observed=window=0,0,1920,1080 Left=0(need <=1)=ok Top=0(need <=1)=ok Right=0(need <=1)=ok Bottom=0(autoHide,need 1-4)=BAD foregroundTaken=False
V11 V11-AUTOHIDE-EDGE FAIL expected=auto-hide edges uncovered by >=1px observed=detected edges=Bottom (no native inset code yet)
V11 V11-MINSIZE FAIL expected=>=320x240 observed=50x50
V11 SUMMARY pass=0 fail=18 skip=0
RED OK
```

(`V11-MAX-WORKAREA` initially PASSED on this unmodified window under a blanket "delta <=4px on
every edge" tolerance — a false-positive guard, since CMP's own maximize already fills the full
monitor when auto-hide is on. Fixed in commit `0ac7b50` to require every auto-hide edge be left
1-4px uncovered rather than flush; re-run gives `RED OK` above with all 18 checks failing. See
`feedback_repro_must_exercise_path`.)

**Maximized client rect vs monitor:** `WindowRectMaximized = (0,0)-(1920,1080)`; `MonitorRcWork =
(0,0)-(1920,1080)`; `MonitorRcMonitor = (0,0)-(1920,1080)` — all three identical, because auto-hide
ON means the OS work area already equals the full monitor.

**Taskbar:** `AutoHideOn=True`, `AutoHideEdges=Bottom`.

**Min-size:** requested 50x50px, obtained 50x50px (no floor exists today).

**UIA summary:** `DescendantCount=1`, histogram `{ControlType.Pane: 1}` — the entire Compose
content is exposed to UI Automation as a single opaque Pane; no per-component UIA nodes exist
(consistent with F8 — a Skiko canvas, not individual AWT/Swing controls).

**C4 baseline (placement/awtExtendedState after SC_MAXIMIZE / SC_RESTORE on the OLD code):** see
C4 above — `placement=Maximized`, `awtExtendedState=6` after `SC_MAXIMIZE`; a subsequent
`SC_RESTORE` was sent but its resulting reporter line was not separately captured in this pass.

**Process:** `jvmKind=standard`, `path=C:\Users\1\.jdks\ms-21.0.9\bin\java.exe` — confirmed NOT
JBR (Microsoft OpenJDK 21.0.9, the default `java` on PATH per 22-RESEARCH.md's own environment
table).

**Artifacts:** `.captures/22-baseline/red-main.json` (full structured probe output),
`.captures/22-baseline/AeroBlue-rest.png`, `AeroDark-rest.png`, `Classic-rest.png` (floating-state
frames, one per theme), `.captures/22-baseline/AeroBlue-maximized.png` (F9 = safe, so the
maximized frame was captured).

## Spike (22-02)

Recorded by Plan 02 (Task 3), 2026-09-25, against the showcase window with the new native
WndProc subclass (`internal/windows/`) installed. Same machine as the RED baseline (Windows 11
24H2 build 26100, one 1920x1080 monitor at 100% DPI, taskbar auto-hide ON, capture mode
non-focusable). Probed with `tools/winprobe/Invoke-WinProbe.ps1 -Report
process,styles,children,hittest,maximize,uia -Json .captures/22-spike/{cold,hot-initial}.json`.

### C1 (after) — style read-back with the subclass installed

**Cold run JVM:** `C:\Users\1\.jdks\ms-21.0.9\bin\java.exe`, `jvmKind=standard` (confirmed not
JBR).

**Observed style after install:** `GWL_STYLE = 0x96CF0000` decodes to `WS_POPUP | WS_CAPTION |
WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_CLIPCHILDREN` — all five bits
`Win32Chrome.ensureNativeFrameStyles` is responsible for (`WS_CAPTION`, `WS_SYSMENU`,
`WS_THICKFRAME`, `WS_MINIMIZEBOX`, `WS_MAXIMIZEBOX`) are present, versus the RED baseline's
`0x960B0000` (`WS_CAPTION`/`WS_THICKFRAME` absent). `GWL_EXSTYLE = 0x08000100` — the same
`WS_EX_NOACTIVATE` capture-mode bit as the RED baseline plus `WS_EX_WINDOWEDGE`, unrelated to
this feature.

**Children subclassed:** the trace log (`aero.chromeTrace=true`) shows exactly one
`event=child-subclass` line per run, matching F8's finding of exactly one descendant
(`SunAwtCanvas`, Skiko's `HardwareLayer`):
```
AERO_CHROME event=restyle hwnd=0x630746 before=0x-79f50000 after=0x-79310000
AERO_CHROME event=install hwnd=0x630746 frameProc=native@0x70190010
AERO_CHROME event=child-subclass hwnd=0x630746 childHwnd=0x3b071a
```
(`before`/`after` print via `Int.toString(16)`, which renders a negative-Int magnitude with a
leading `-` rather than raw two's-complement hex — a cosmetic trace-formatting quirk, not a
correctness issue; the authoritative style value is the cross-process `GetWindowLongPtr` read
above, `0x96CF0000`.)

**Hit-test chain, both JVMs, all 14 named points** (cold standard JDK 21 and hot JBR 21 produced
byte-identical answers):
```
caption            code=2 (HTCAPTION)    chain=SunAwtCanvas:-1 -> SunAwtFrame:2
captionLeftOfMin   code=2 (HTCAPTION)    chain=SunAwtCanvas:-1 -> SunAwtFrame:2
min                code=1 (HTCLIENT)     chain=SunAwtCanvas:1
max                code=9 (HTMAXBUTTON)  chain=SunAwtCanvas:-1 -> SunAwtFrame:9
close              code=1 (HTCLIENT)     chain=SunAwtCanvas:1
client             code=1 (HTCLIENT)     chain=SunAwtCanvas:1
edgeLeft           code=1 (HTCLIENT)     chain=SunAwtCanvas:1
edgeRight          code=1 (HTCLIENT)     chain=SunAwtCanvas:1
edgeTop            code=2 (HTCAPTION)    chain=SunAwtCanvas:-1 -> SunAwtFrame:2
edgeBottom         code=1 (HTCLIENT)     chain=SunAwtCanvas:1
cornerTL           code=2 (HTCAPTION)    chain=SunAwtCanvas:-1 -> SunAwtFrame:2
cornerTR           code=2 (HTCAPTION)    chain=SunAwtCanvas:-1 -> SunAwtFrame:2
cornerBL           code=1 (HTCLIENT)     chain=SunAwtCanvas:1
cornerBR           code=1 (HTCLIENT)     chain=SunAwtCanvas:1
```
`V11-HT-CAPTION` (expected=2) and `V11-HT-MAX` (expected=9) are now PASS against the RED
baseline's `observed=1` for both — the child→frame `HTTRANSPARENT` bounce (F8) works exactly as
predicted: the frame's classification is reached through the child. `edgeTop`/`cornerTL`/`cornerTR`
answer HTCAPTION only because they fall inside the 32dp title row in this spike's hardcoded
classifier — genuine edge/corner resize hit-testing is out of this spike's scope. This is
**spike state, not GREEN**: the full `V11` table (edges, maximize geometry, min-size) is settled
by later steps per the plan's own scope note.

**New finding, not previously predicted in this repo's notes:** `MAXIMIZE` reports
`windowRect=(-8,-8)-(1928,1088)` against `rcWork=(0,0)-(1920,1080)` — an 8px overhang on every
edge past the monitor, exactly the "naive `WM_NCCALCSIZE` return 0 unconditionally" maximized
overhang failure mode (the frame's sizing-border math still applies even though the caption
paint is hidden). Compare to the RED baseline, where the *pre-subclass* window maximized to
exactly `rcWork` with zero overhang (no `WM_NCCALCSIZE` handler existed to interfere). This
spike's `WM_NCCALCSIZE` handler returns 0 unconditionally regardless of maximized state — the
inset fix for the maximized case is explicitly out of this spike's scope (see the frame proc's
own `WM_NCCALCSIZE` branch comment). Tracked as a known, expected gap for a later step, not a
regression to fix here.

**Settled:** C1 (after) — SETTLED. Confirmed against the observed values above.

### C4 — does `WindowState.placement` still sync automatically once the subclass is installed?

**Check performed:** Same headless `SC_MAXIMIZE` → `SC_RESTORE` cycle as the RED baseline's C4
check (`Invoke-WinProbeMaximize`), this time against the window with the native subclass
installed, reading the `AERO_WINDOW_STATE` lines the reporter prints.

**Observed:**
```
... placement=Floating   awtExtendedState=0 ...   (before)
... placement=Maximized  awtExtendedState=6 ...   (after SC_MAXIMIZE, awtBounds=-8,-8,1936,1096 — the overhang above)
... placement=Floating   awtExtendedState=0 ...   (after SC_RESTORE)
```

**Decision:** unchanged from the RED baseline's conclusion — `WindowState.placement` still
flips to `Maximized`/back to `Floating` with **zero explicit sync code**, now proven *with* the
native subclass installed and actively answering `WM_NCHITTEST`/`WM_NCCALCSIZE`. This confirms
the frame proc's `else -> CallWindowProc(previous, ...)` passthrough (every message this feature
doesn't explicitly own, including `WM_SIZE`/`WM_SYSCOMMAND`) does not disturb AWT's own
extendedState → `WindowStateListener` pipeline. No explicit placement-push code is needed for
this spike or for a plain native `SC_MAXIMIZE`.

**Settled:** C4 — SETTLED, "syncs automatically" confirmed with the subclass live.

### C7 (runtime half) — JBR `hotRun` vs cold standard-JDK `run`

**Cold run:** `C:\Users\1\.jdks\ms-21.0.9\bin\java.exe`, `jvmKind=standard`.
**Hot run:** `C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe`, `jvmKind=JBR`.

Both runs produced byte-identical `STYLE`, `HITTEST` (all 14 points) and `MAXIMIZE` answers
(see C1/C4 above) — no behavioral drift between the standard JDK the published library targets
and the JBR the showcase's Hot Reload path runs on.

**Three-reload idempotency check:** with the `hotRun` app kept alive (`-KeepRunning`), a
temporary, never-committed edit inside `AeroTitleBar.kt` (`Spacer(Modifier.width(8.dp))` →
`8.01.dp`) was applied and reverted three times, running the top-level `reload` Gradle task
(`./gradlew tasks --all` confirmed the task name is `reload`, not `:showcase:reload`) after each
edit:

| Reload # | Edit state | New `AERO_CHROME` trace lines | Hit-test answers after reload |
|---|---|---|---|
| 1 | `8.01.dp` | none (only the original `restyle`/`install`/`child-subclass` triple from initial launch) | identical to pre-reload |
| 2 | reverted to `8.dp` | none | identical to pre-reload |
| 3 | `8.01.dp` again | none | identical to pre-reload |

Pass = exactly one live install for the HWND held throughout (no second `install` or `reuse`
line ever appeared — Hot Reload's method-body swap did not re-trigger `AeroTitleBar`'s
`DisposableEffect(window)` for this particular edit shape, so the idempotent-reuse path in
`NativeWindowChromeRegistry` was never even exercised by this specific edit — but the WNDPROC
chain was never re-stacked or broken either, and hit-test answers stayed byte-identical across
all three reload cycles, which is the load-bearing guarantee). The temporary edit was reverted
in the source file after the third reload; `git diff --quiet HEAD -- library/` holds.

**Settled by:** 22-02 T3 — SETTLED. C7 fully settled (grep half was 22-01 T3; runtime half is
this task).

**Artifacts:** `.captures/22-spike/cold.json`, `.captures/22-spike/hot-initial.json`,
`.captures/22-spike/rest.png` (PrintWindow frame of the spike window, floating state — agent
inspection: no ghost native caption, no white strip, no content offset visible; the title bar
renders as a single Compose-drawn gradient band exactly as before this phase, consistent with
`WM_NCCALCSIZE` returning 0 hiding the native caption's paint while the style bits stay set for
DWM/Shell).

## Early gate (22-04) — readiness

Recorded by Plan 04 (Task 1), 2026-09-25, before asking the maintainer for the early VER-12
"ok". `:showcase:classes` compiled clean (`BUILD SUCCESSFUL in 20s`, 7 actionable tasks) so the
post-"ok" launch in Task 3 does not pay a compile cost. Ran
`tools/winprobe/Invoke-EarlyGate.ps1 -DryRun -Json .captures/22-gate/readiness.json` against
`HEAD` (commit `1093568`).

**Dry-run result:** exit code `0`. Cursor unchanged: `CursorBefore=(890,986)`,
`CursorAfter=(890,986)` — proven live, not assumed. Verdict line printed is
`GATE FAIL FLYOUT MISSING,SNAP MISSING,RESTORE MISSING`, which is the dry run's expected,
inapplicable verdict (per 22-03's own decision: a dry run never sends input, so FLYOUT/SNAP/
RESTORE can never legitimately read OK — the exit code, not this verdict string, is what proves
the dry run itself worked).

**Step timings (`.captures/22-gate/readiness.json`):**

| Step | Result | Detail | Elapsed |
|---|---|---|---|
| Readiness | OK | `SnapAssistFlyoutOn=True WindowArrangementActive=True` | 33.6ms |
| Launch | OK | `jvm=standard path=C:\Users\1\.jdks\ms-21.0.9\bin\java.exe` | 5683.9ms |
| PositiveControl | SKIPPED | notepad not started in dry run | 0.03ms |
| ComposeFlyout | FLYOUT MISSING | dry run — no real hover performed | 4004.0ms |
| DragSnap | SNAP MISSING | dry run — no real drag performed | 62.3ms |
| DragAway | RESTORE MISSING | dry run — no real drag performed | 25.1ms |

`RealInputMs=9886.0` (the dry-run's simulated action-log timing, not real `SendInput`),
`OverallMs=28943.3`.

**Showcase launch time:** the `Launch` step (showcase process start + window found + reporter
`main` state ready) took **5683.9ms ≈ 5.68s**. JVM launched: `C:\Users\1\.jdks\ms-21.0.9\bin\
java.exe` (`jvmKind=standard`, confirmed not JBR — matches every prior plan's finding of the
default `java` on this machine).

**Minutes estimate for the maintainer (N):** measured launch time (5.68s) + the planned real-input
cap (≤ 2 min = 120s) = 125.68s ≈ 2.09 min → round up to 3 min → add 1 → **N = 4 minutes**.

**Snap settings (read-only, `Get-AeroSnapSettings`):** `SnapAssistFlyoutOn=True`,
`WindowArrangementActive=True` — both snapping and Snap Layouts are enabled on this machine;
nothing was changed (read-only check, per plan instruction).

**`notepad.exe` resolution:** `where notepad.exe` →
`C:\Windows\System32\notepad.exe` and `C:\Windows\notepad.exe` (first match used by
`Start-Process -FilePath 'notepad.exe'`).

**Cleanup check:** after the dry run, no `java.exe` process with `-Paero.chromeTrace=true` /
`-Paero.capture=true` in its command line remained (checked via `Get-CimInstance Win32_Process`);
the many other `java.exe` processes present on this machine are pre-existing Gradle daemons and
the always-on Hot Reload MCP server (`:showcase:hotMcpServer`, started ~12:25, unrelated to this
15:46 dry run) — none traces back to this gate's own launch.

**Readiness verdict:** the session can start the second the maintainer says "ok" — code compiles,
the dry run proves the script itself runs clean and restores the cursor, snap settings are
enabled, and notepad.exe resolves.

## Early gate (22-04) — result

Run by the orchestrator after the maintainer's explicit reply `ok` (2026-09-25, ~16:09 local);
`-AuthorizedBy "ok @ <UTC timestamp>"`. Command:
`Invoke-EarlyGate.ps1 -AuthorizedBy ... -Json .captures/22-gate/early.json` (console:
`.captures/22-gate/early-console.log`, showcase stdout: `.captures/22-gate/logs/winprobe-run-160944.335.out.log`).
JVM `C:\Users\1\.jdks\ms-21.0.9\bin\java.exe` (standard, not JBR). Real input 29.0 s, overall 53.3 s.
Cursor restored (499,619 → 499,619). No showcase JVM or notepad process remained afterwards.

| Step | Result | Detail |
|---|---|---|
| Readiness | OK | `SnapAssistFlyoutOn=True WindowArrangementActive=True` |
| Launch | OK | standard JDK 21; `restyle` + `install` + `child-subclass` traced |
| PositiveControl | ERROR | `notepad HTMAXBUTTON span not found in its top 40px row` — no reference flyout signature |
| ComposeFlyout | FLYOUT MISSING | hover at the `max` point (1171,64) for the 3 s watch window; watcher saw no new shell element |
| DragSnap | SNAP MISSING (mis-specified) | released at (1,64) → rect `0,0,960,540` |
| DragAway | RESTORE OK | 1200×800 restored |

**Verdict line:** `GATE FAIL FLYOUT MISSING,SNAP MISSING`.

**Reading of the evidence (orchestrator):**

- Snapping itself works natively. The reporter trace shows the drag moving the window left
  (`posDp` 48 → −531) and, on release, Windows placing it at `0,0,960,540` — the top-left
  QUARTER of the 1920×1080 work area. The drag target (x=1, y=64) lies inside Windows' top-left
  corner zone, so a quarter snap is the correct OS answer; the gate's "left half" expectation was
  the tool's error (drag target must sit at the vertical middle of the edge). The drag-away then
  shows Windows' restore-on-drag (width 960 → 1200 at the first move) — also native behavior.
  On the RED baseline (old `WindowDraggableArea`) no snap of any kind happens.
- The Snap Layouts flyout is UNPROVEN, not disproven: the positive control never ran (the probe
  found no `HTMAXBUTTON` in the Windows 11 Notepad's top row — Notepad is a WinUI app with its
  own title bar), so there is no reference signature and no proof that the watcher can observe a
  flyout at all. D-04 applies: stop and ask; no silent downgrade.
- Hypothesis for the missing flyout (to check before any rerun): the frame subclass forwards
  `WM_NCMOUSEMOVE` / `WM_NCMOUSELEAVE` over `HTMAXBUTTON` to AWT's original WndProc, which may
  consume them instead of reaching `DefWindowProc`, where the shell's flyout trigger lives.

**Status:** plan 22-04 NOT complete; phase stopped at the global stop rule pending the
maintainer's decision.

## Early gate (22-04) — tooling fix

Recorded after the real run above, fixing the tooling gaps it exposed so the SAME short
real-input session can be rerun with agent-recorded evidence (D-04). No real input was sent while
building or verifying any of this — every check below is either read-only, a WM_NCHITTEST query
message, or gated behind `-DryRun`/`-SelfTest`, neither of which reaches `SendInput`.

### Q1: Why did the positive control fail entirely (no reference flyout signature)?

**Check performed:** Read `Find-AeroMaxButtonSpan`'s scan geometry against `Get-WinProbeWindowInfo`
for a real `WS_CAPTION` window, live.

**Observed:** The scan asked `Invoke-WinProbeHitTest` for `ClientY=10` — a positive offset from
the client-area origin, i.e. *inside* client content. That is correct for the Compose showcase
window (an undecorated window whose "caption" is client-drawn, at the top of client content) but
wrong for any normal `WS_CAPTION` window (Notepad, or any other standard app): its real caption
and max button live in the *non-client* area, **above** the client origin — a negative `ClientY`.
A positive-`ClientY` scan can never land inside a real caption's HTMAXBUTTON span, on any
standard window, regardless of which app it is. This was a tooling bug, not a Notepad-specific
one (WinUI vs. Win32 is a red herring for this particular symptom, even though Windows 11's
Notepad genuinely no longer has its own `HTMAXBUTTON`-answering non-client caption at all).

**Decision:** Two things, both required: (1) stop depending on Notepad, whose own status as a
real answerer of `HTMAXBUTTON` is no longer guaranteed on Windows 11 in the first place; (2) fix
the scan geometry itself to also try the real caption's row. `PositiveControlHost.ps1` +
`PositiveControlWindow.ps1` launch our own `WS_CAPTION`/`WS_THICKFRAME`/`WS_MAXIMIZEBOX` WinForms
window in its own process; `Find-AeroMaxButtonSpan` now computes the real caption's vertical
midpoint as `-(clientOriginScreenY - windowRectTop) / 2` (negative `ClientY`) and tries it before
the Compose-style `ClientY=10` row, so the same function works for either window kind. Live-tested
against the positive-control window: caption offset 31px, span found at `ClientY=-16`,
`CenterX=732` (see the dry-run proof below).

### Q2: Why did the ComposeFlyout step see `found=False` when the maintainer reports seeing the
flyout?

**Check performed:** Re-read `Wait-AeroSnapFlyout`'s baseline/diff logic against what a Snap
Layouts flyout host is likely to do (desk knowledge, flagged unverified until the real run below
confirms or denies it): Windows 11 shell surfaces are frequently pre-created and cloaked, then
merely uncloaked/shown on demand, not created fresh each time.

**Observed:** The old diff only checked "is this HWND new since baseline" (`EnumWindows`-visible
only). A pre-created, cloaked flyout host that was already present (just invisible) at baseline
time, and only gets shown/uncloaked on hover, keeps the *same* HWND across baseline and observed
snapshots — the key `"$ClassName|$ProcessName|$Hwnd"` is identical in both, so the old diff would
never flag it as new. Since the positive control also never produced a reference signature (Q1),
there was no way to tell "watcher can't see anything" apart from "nothing appeared".

**Decision:** `Get-AeroShellSnapshot` now enumerates ALL top-level shell-process windows
(`EnumTopLevelAll`, not `EnumTopLevelVisible`) carrying `Visible`/`Cloaked` flags, and
`Wait-AeroSnapFlyout`'s diff now also flags a window transitioning from
hidden-or-cloaked→visible-and-uncloaked as an appearance, on the *same* HWND. A second, independent
signal was added on top: `Start-AeroShellEventWatch` hosts a `SetWinEventHook`
(`WINEVENT_OUTOFCONTEXT | WINEVENT_SKIPOWNPROCESS`) on a dedicated background thread with its own
`GetMessage` loop, recording `EVENT_OBJECT_SHOW`/`UNCLOAKED`/`CREATE`/`DESTROY`/`HIDE`/`CLOAKED`
and `EVENT_SYSTEM_FOREGROUND` for shell processes plus this run's own PIDs — a genuine SHOW or
UNCLOAKED event on a shell process now also counts as "found". `-SelfTest` proves the hook fires
at all (see below) without any real input. A third-order bug (Rule 1) was found and fixed while
proving this live: the cloak-aware snapshot's raw top-level window count on this machine is ~226
(mostly invisible), and looking up each one's process name via `Get-Process -Id` cost ~4s per
snapshot — far too slow for a 100ms poll inside a 1.5–3s hover window. `Get-AeroProcessNameMap`
batches this into one `Get-Process` call (~60ms) plus O(1) lookups; live-measured snapshot cost is
now ~260ms.

### Q3 (Task C): Flyout host survey — which shell-owned window is the Snap Layouts flyout likely
to be?

**Check performed:** Read-only, no input, no screenshots: `Get-AeroShellSnapshot` (cloak-aware)
filtered to shell-process top-level windows, sorted by process/class; separately confirmed
`ShellExperienceHost`, `StartMenuExperienceHost`, `SearchHost`, `ShellHost` are all running
processes on this machine (`Get-Process`).

**Observed:** 55 shell-owned top-level windows exist right now, entirely under `explorer.exe`
except two single-window entries under `SearchHost` and `ShellHost` (neither resembling a flyout
host by class name or geometry). `ShellExperienceHost` and `StartMenuExperienceHost` — both
running — currently own **zero** top-level windows of any kind (not even hidden ones), consistent
with those processes creating their surfaces only while shown rather than keeping one pre-created.
One `explorer.exe`-owned window stands out as the strongest candidate:

```
explorer|XamlExplorerHostIslandWindow|visible=False|cloaked=True|rect=0,0,1920,1080|size=1920x1080
```

Hidden, cloaked, and sized to exactly the full monitor — exactly the shape Q2's "pre-created,
merely uncloaked/shown on hover" hypothesis predicts for a XAML-island-hosted shell flyout. The
name itself ("XamlExplorerHostIslandWindow") is consistent with desk knowledge that Windows 11's
newer shell flyouts (Snap Layouts among them) are hosted via `explorer.exe`'s own XAML-island
infrastructure — **this attribution is unverified until the real hover run confirms this specific
window (or a UIA element within it) is what actually shows/uncloaks on hover.** Two other
`explorer.exe` windows were cloaked-but-visible (`DummyDWMListenerWindow` ×5, tiny/zero-rect; and
`EdgeUiInputTopWndClass`, a 1905×4px strip at the very top of the screen) — geometry makes both
poor candidates for the flyout itself (too small, or a thin activation strip rather than content),
but are recorded here in case the real run's evidence points at one of them instead.

**Decision:** No code change from this alone (Task C is observation-only); the watcher already
watches all of `explorer.exe`'s top-level windows regardless of which one turns out to be the
actual host, so no watcher change is gated on this identification. Recorded for the real run to
confirm or refute.

### Q4: Why did DragSnap land on a quarter-snap instead of the expected half-snap?

**Check performed:** Compared the real run's drop point `(1, 64)` against Windows' documented
snap-zone geometry (corner zones extend from the very top-left of the work area down to roughly
half the screen height on each edge; edge zones are the remaining middle band).

**Observed:** `y=64` (the showcase's caption height in physical pixels) sits well inside the
top-left corner zone at this resolution, so Windows correctly answered with a quarter snap
(`0,0,960,540`) — the OS behaved correctly for the point given to it; the gate's own drop target
was wrong, not Windows' snap logic.

**Decision:** `DragSnap` now releases at `(workLeft + 1, workAreaCenterY)` — the vertical middle
of the left work-area edge — so the edge zone applies instead of the corner zone. The existing
"left half of `rcWork`, ≤8px per edge" tolerance check did not need to change, only the drop
point did. The reporter's placement line and the final window rect are now also recorded in the
JSON output (`SnapRectAfter`, `SnapReporterPlacement`).

### Dry-run and self-test proof (Task E)

`./gradlew :showcase:classes --console=plain` → `BUILD SUCCESSFUL`, all tasks `UP-TO-DATE`.

`Watch-SnapFlyout.ps1 -SelfTest`: exit `0`. `windows=63 uiaElements=7` (no non-shell/non-own
element carried a `Name`). The hook recorded a `CREATE` event for the positive-control window's
own HWND at `497ms` after the watch started. Cursor position unchanged throughout (asserted
inside the self-test).

`Invoke-EarlyGate.ps1 -DryRun -Json .captures/22-gate/dryrun2.json`: exit `0`.
`CursorBefore=(1229,412)`, `CursorAfter=(1229,412)` — unchanged. Step results:

| Step | Result | Detail | Elapsed |
|---|---|---|---|
| Readiness | OK | `SnapAssistFlyoutOn=True WindowArrangementActive=True` | 28.5ms |
| Launch | OK | `jvm=standard path=C:\Users\1\.jdks\ms-21.0.9\bin\java.exe` | 5986.7ms |
| PositiveControl | OK | dry run — `maxButtonCenter=732,-16` (window closed, no input sent) | 1059.4ms |
| ComposeFlyout | FLYOUT MISSING | dry run — no real hover performed | 3477.7ms |
| DragSnap | SNAP MISSING | dry run — no real drag performed | 82.9ms |
| DragAway | RESTORE MISSING | dry run — no real drag performed | 26.8ms |

`RealInputMs=10792.3` (dry-run's simulated action-log timing, not real `SendInput`),
`OverallMs=34766.7`. No showcase JVM, positive-control, or gate process remained afterward
(checked via `Get-CimInstance Win32_Process`, excluding the checking command's own process).

**Showcase launch time (this run):** 5986.7ms ≈ 5.99s.

**Minutes estimate for the maintainer (N), same formula as Task 1:** 5.99s (launch) + 120s
(planned real-input cap) = 125.99s ≈ 2.10 min → round up to 3 min → add 1 → **N = 4 minutes**
(unchanged from the original readiness estimate).

**Commit-organization note:** the plan's suggested commit split names four separate commits; the
positive-control-usage change and the event-watch wiring inside `Invoke-EarlyGate.ps1` turned out
to be inseparable at the line level (the `PositiveControl` step's own correctness now depends on
the event watch already running), so those two landed in one wiring commit alongside the
drag-target fix, while the two new positive-control files and the `Watch-SnapFlyout.ps1` rewrite
each kept their own commit. An additional, unplanned commit (Rule 1) fixes the `Get-Process`
performance regression the cloak-aware snapshot introduced, found live while proving this section.

**Readiness verdict:** the SAME short real-input session can be rerun the moment the maintainer
says "ok" again — the positive control finds a real reference signature, the watcher can observe
a pre-created-and-uncloaked flyout as well as a brand-new one, and the drag lands in the edge zone
instead of the corner zone. Command for the orchestrator to run after the maintainer's next "ok":

```
tools/winprobe/Invoke-EarlyGate.ps1 -AuthorizedBy "<maintainer reply verbatim> @ <ISO timestamp>" -Json .captures/22-gate/early2.json
```

## Early gate (22-04) — second real run and watcher fix

Second real run after the maintainer's `ok` (2026-09-25 ~16:43 local), same build, fixed tooling
from `c97570f..13660a4`. JSON `.captures/22-gate/early2.json`, console `early2-console.log`.
Standard JDK 21. Real input 34.7 s. Cursor restored (563,987 → 563,987).

| Step | Result | Detail |
|---|---|---|
| PositiveControl | OK | flyout observed after 646 ms via Event — reference signature `E|Windows.UI.Composition.DesktopWindowContentBridge|explorer` |
| ComposeFlyout | ERROR | comparison threw `Cannot convert ... "E|Windows.UI.Composition.DesktopWindowContentBridge|explorer" ... to type "System.Int32"` |
| DragSnap | SNAP OK | released at (1,540) → `0,0,960,1080` = left half of rcWork; reporter placement `Floating` |
| DragAway | RESTORE OK | 1200×800 restored |

- **Watcher calibration (D-04) is now proven:** the event-based watcher observes the Snap Layouts
  flyout on a standard DefWindowProc window (explorer, `Windows.UI.Composition.DesktopWindowContentBridge`,
  646 ms after the hover began).
- **SNAP-01 half-snap and restore are proven** on the spike window (run 2).
- **Two tooling bugs made the Compose flyout verdict unusable:**
  1. `Get-AeroFlyoutSignatureSet` returned a `HashSet[string]` without the unary comma, so a
     one-element set was unrolled into a bare string and `New-Object HashSet[string] -ArgumentList`
     picked the `HashSet(int capacity)` constructor → the exception above.
  2. The WinEvent log is cumulative since the watch started, and `Wait-AeroSnapFlyout` counted
     every shell SHOW/UNCLOAKED event in it — so the Compose step saw the positive control's own
     flyout events and reported `Found=True` on its first poll. That `Found` is therefore NOT
     evidence of a flyout over the Compose window (the maintainer's recollection of seeing it is
     not evidence either, per D-04).
- **Fix (orchestrator):** snapshots carry `EventMark` (event count at baseline);
  `Select-AeroEventsAfterMark` restricts a hover's judgment to events after its own baseline;
  the signature set is returned with `return , $set` and matched with a plain `Contains` loop;
  `ConvertTo-AeroFlyoutEvidence` writes both raw observations (`PositiveFlyout`, `ComposeFlyout`)
  and `FlyoutMatch` into the gate JSON before any verdict is computed. Offline logic test
  (single/multi-element match, mismatch, mark filter edge cases, typed list input, evidence JSON)
  all PASS; `Watch-SnapFlyout.ps1 -SelfTest` PASS; gate `-DryRun` (`dryrun3.json`) exit 0 with the
  cursor unchanged and the new JSON fields present.

**Status:** the Compose flyout still needs one more real hover; plan 22-04 stays incomplete.

## Early gate (22-04) — third real run: GATE PASS

Third real run after the maintainer's `ОК` (2026-09-25, evening), same library build as runs 1–2,
tooling at `1357137`. JSON `.captures/22-gate/early3.json`, console `early3-console.log`.
JVM `C:\Users\1\.jdks\ms-21.0.9\bin\java.exe` (standard, not JBR). Real input 36.1 s, overall 66.4 s.
Cursor restored (596,648 → 596,648). No showcase JVM or positive-control process remained.

| Step | Result | Detail |
|---|---|---|
| PositiveControl | OK | 666 ms: `SHOW Windows.UI.Composition.DesktopWindowContentBridge` (explorer), rect `751,231,1095,475` — under the WinForms window's max button |
| ComposeFlyout | FLYOUT OK | 804 ms: `SHOW Windows.UI.Composition.DesktopWindowContentBridge` rect `999,79,1343,323`, then `SHOW Xaml_WindowedPopupClass` rect `989,77,1353,341`, then `UNCLOAKED XamlExplorerHostIslandWindow` rect `999,79,1343,323` (all explorer) — a 344×244 flyout centred on x≈1171, directly under the showcase's maximize button (hover point 1171,64); matched signature `E|Windows.UI.Composition.DesktopWindowContentBridge|explorer` |
| DragSnap | SNAP OK | released at (1,540) → `0,0,960,1080` = left half of rcWork; reporter placement `Floating` |
| DragAway | RESTORE OK | 1200×800 restored |

**Verdict line:** `GATE PASS`.

**Settled:** Windows 11 24H2 shows the Snap Layouts flyout for a Compose window on a standard
JDK 21 once the frame answers `HTMAXBUTTON` through the child→frame `HTTRANSPARENT` chain (22-02
spike), and edge snapping / restore-on-drag are native. The flyout host is explorer's pre-created
`XamlExplorerHostIslandWindow` (uncloaked on hover) with a `DesktopWindowContentBridge` child — the
survey candidate from the tooling fix is confirmed. The milestone's existential risk is retired;
the phase continues.

## Frame removal (22-05)

Recorded by Plan 05 (Task 3), 2026-09-25, against the showcase with the production style-keeper
(`WM_STYLECHANGING`) and taskbar-aware maximized `WM_NCCALCSIZE` from Tasks 1–2 installed. Same
machine as every prior plan (Windows 11 24H2 build 26100, one 1920x1080 monitor at 100% DPI,
taskbar auto-hide ON). Probed via `tools/winprobe/Invoke-WinProbe.ps1 -Launch run -Report
styles,hittest,maximize,taskbar,minsize,uia,process,v11` and an ad-hoc capture script built for
this task (dot-sources `tools/winprobe/WinProbe.ps1`, one `:showcase:run` launch,
`-Paero.scheme=AeroBlue -Paero.capture=true`), non-focusable capture mode throughout, `-Paero.
chromeTrace=true` used once to confirm the computed rect. F9 (settled 22-01: probe-driven
`SC_MAXIMIZE` never takes the foreground on this machine) held again — no `-SkipMaximize` used.

### V11 results (this task's scope)

```
V11 V11-STYLE PASS expected=WS_CAPTION,WS_SYSMENU,WS_THICKFRAME,WS_MINIMIZEBOX,WS_MAXIMIZEBOX observed=WS_POPUP,WS_CAPTION,WS_SYSMENU,WS_THICKFRAME,WS_MINIMIZEBOX,WS_MAXIMIZEBOX,WS_CLIPCHILDREN
V11 V11-HT-CAPTION PASS / V11-HT-MAX PASS / V11-HT-MIN-BOUNDARY PASS / V11-HT-CLOSE-BOUNDARY PASS (unchanged from 22-02 spike — title-row hit-testing is out of this plan's scope)
V11 V11-HT-MAXIMIZED-TOP PASS expected=2 observed=2
V11 V11-MAX-WORKAREA PASS expected=rcWork=0,0,1920,1080 autoHideEdges=Bottom observed=client=0,0,1920,1078 window=-8,-8,1928,1088 Left=0(need<=1)=ok Top=0(need<=1)=ok Right=0(need<=1)=ok Bottom=2(autoHide,need1-4)=ok foregroundTaken=False
V11 V11-AUTOHIDE-EDGE PASS expected=auto-hide edges uncovered by >=1px observed=detected edges=Bottom deltas=Bottom=2
V11 SUMMARY pass=8 fail=10 skip=0
```

`V11-HT-EDGE-L/R/T/B`, `V11-HT-CORNER-*`, `V11-HT-CLIENT-BOUNDARY` (genuine edge/corner resize
hit-testing, out of this plan's scope per the 22-02 spike note) and `V11-MINSIZE` (the 320×240dp
floor, D-01, not touched by this plan) remain FAIL — unchanged from the 22-02 spike, not a
regression, tracked by their own later plans.

**Tool fix (Rule 1 — bug):** `Invoke-WinProbeV11`'s `V11-AUTOHIDE-EDGE` check was a hardcoded
`FAIL` placeholder written in 22-01 ("no native inset code yet") and could never pass regardless
of correct implementation. Separately, `V11-MAX-WORKAREA` compared `GetWindowRect` (the OUTER
window bounding box) against `rcWork` — but `WM_NCCALCSIZE` only ever controls the CLIENT
rectangle (confirmed live via `-Paero.chromeTrace=true`: the frame proc computed and wrote exactly
`PxRect(left=0, top=0, right=1920, bottom=1078)`, while `GetWindowRect` still reported
`-8,-8,1928,1088` — the pre-existing sizing-border overhang this plan deliberately leaves alone,
since `WM_GETMINMAXINFO` is explicitly out of scope here, Plan 11 territory). Both checks now
measure the CLIENT rect in screen coordinates (`ClientToScreen` + `GetClientRect`, added to
`Invoke-WinProbeMaximize`'s return as `ClientRectMaximized`) — the rectangle that actually governs
what AWT/Skia renders and what a person looking at the screen actually sees. Fixed in
`tools/winprobe/WinProbe.ps1` / `Invoke-WinProbe.ps1`; live-verified PASS above.

### Reporter consistency (insets, sizeDp/scale/awtBounds)

```
REPORTER label=main placement=Floating minimized=false awtExtendedState=0 sizeDp=1200.0x800.0 posDp=360.0,140.0 awtBounds=360,140,1200,800 insets=0,0,0,0 scale=1.0 minSizeSet=false minSize=1x1 resizable=true jvm=21.0.9/Microsoft
```

`insets=0,0,0,0` — no non-client frame reported to AWT. `sizeDp=1200.0x800.0 × scale=1.0 =
1200x800` px, matching `awtBounds` width/height (`1200,800`) exactly (±1 tolerance satisfied
trivially, delta 0).

### Taskbar re-confirm

`Get-WinProbeTaskbar`: `autoHideOn=True autoHideEdges=Bottom` — unchanged from C3's original
detection (22-01).

### Frame comparisons (D-02, WIN-03)

`.captures/22-frame/AeroBlue-rest.png` (1200×800, floating) vs
`.captures/22-baseline/AeroBlue-rest.png` (pre-phase, unmodified window), both `AeroBlue`:

```
COMPARE title-band baseline-vs-rest top=0 bottom=31 diffPixels=0 maxDelta=0 comparable=True
COMPARE white-strip baseline-vs-rest top=0 bottom=2  diffPixels=0 maxDelta=0 comparable=True
```

Zero differing pixels in the title row band and the top-3-row white-strip check — the floating
title bar is pixel-identical to the pre-phase baseline. Agent inspection of both frames: single
Compose-drawn gradient title band, `AeroBlue`/`AeroDark`/`Classic` switcher directly below it, no
native caption glyphs, no white strip, no content offset — matches the baseline exactly.

**Ghost-caption check (WM_NCACTIVATE):** `Send-WinProbeMessage` `WM_NCACTIVATE` (`0x0086`) with
`wParam=0` then `wParam=1`, re-capturing after each:

```
COMPARE title-band rest-vs-deactivated diffPixels=0 maxDelta=0 comparable=True
COMPARE title-band rest-vs-reactivated diffPixels=0 maxDelta=0 comparable=True
```

Zero differing pixels both times — **no ghost native caption or repaint artifact appears on
activation/deactivation**. The documented mitigation (forwarding `WM_NCACTIVATE` with `lParam=-1`
from `AeroFrameWndProc`) was NOT needed; no extra fix commit was made for this.

**Maximized frame (`.captures/22-frame/AeroBlue-maximized.png`, 1936×1096):** pixel-sampled
(not just visually inspected) to separate a real defect from an expected-but-invisible artifact.
`PrintWindow` renders the FULL **window** rect (`-8,-8`–`1928,1088`, 1936×1096), not the client
rect — so the image itself shows an ~8px near-white/black border around the content (sampled:
`(0,0)`≈white `(244,243,243)`, `(8,8)`≈title-bar navy `(26,57,107)` — the client starts exactly at
the +8,+8 offset the window/client rect math predicts; `(1930,100)` and `(100,1088)+` sample pure
black, the off-window-rect padding PrintWindow fills for area it has no content for). Cross-checked
against monitor bounds: since the window rect's negative/overshoot portions
(`x<0`, `x>=1920`, `y<0`, `y>=1080`) are **off the physical 1920×1080 screen**, Windows never
paints them to the real display — only the overlap between the window rect and the monitor is
visible, and that overlap equals the client rect exactly (screen `(0,0)`–`(1920,1078)`) plus the
deliberate 2px bottom auto-hide strip. **Conclusion: the border visible in the raw `PrintWindow`
capture is a capture-method artifact (it renders off-screen window-rect pixels no user ever sees),
not a real visual defect** — nothing to fix here; this is the direct, expected consequence of
scoping this plan to `WM_NCCALCSIZE` only and leaving `WM_GETMINMAXINFO` for Plan 11.

### Conflict #3 (auto-hide inset) — detection half + inset now settled

**Detection method:** `SHAppBarMessage(ABM_GETSTATE)` for the `ABS_AUTOHIDE` flag, then
`SHAppBarMessage(ABM_GETAUTOHIDEBAREX)` per `ABE_LEFT/TOP/RIGHT/BOTTOM` edge against the
maximizing window's own monitor rect (`MonitorFromWindow` + `GetMonitorInfo`, never a hardcoded
primary monitor) — implemented in `Win32Chrome.autoHideEdges`.

**Edges found:** `Bottom` only (matches C3's original 22-01 finding).

**Inset chosen:** 2px (Windows Terminal's production value, `AUTO_HIDE_INSET_PX` in
`Win32Geometry.kt`) — no source supports 1px for this purpose.

**Measured maximized client rect:** `(0,0)-(1920,1078)` — the intersection of the proposed rect
with the monitor's work area (`(0,0)-(1920,1080)`, identical to `rcMonitor` on this auto-hide
monitor per C3), with the Bottom edge inset by the 2px.

**Reveal-on-hover half:** still explicitly assigned to Plan 15 (real input) — this task only
proves the maximized client rect leaves the auto-hide edge geometrically uncovered by the
production inset; whether hovering there actually reveals the taskbar needs real mouse input.

**Settled:** C3 — detection method, edges, chosen inset and the measured rect are all settled
here; the reveal-on-hover confirmation remains Plan 15's.

## Corners and shadow (22-06)

Recorded by Plan 06 (Task 1), 2026-09-26, agent self-review before the maintainer sees the
window (VER-13 discipline, feedback_gui_self_review_before_user). Same machine as every prior
plan. Captured via `.captures/22-corners/Invoke-CornersSweep.ps1` — one `:showcase:run` launch
per scheme (`-Paero.scheme=<scheme> -Paero.capture=true`, non-focusable capture mode),
PrintWindow rest frames into `.captures/22-corners/<scheme>-rest.png`, LockBits comparison
against the pre-phase baseline in `.captures/22-baseline/`. Every process the sweep started was
stopped by the sweep itself.

### Self-review: title bands identical to the baseline

```
COMPARE AeroBlue title-band baseline-vs-rest top=0 bottom=31 diffPixels=0 maxDelta=0 comparable=True
COMPARE AeroBlue white-strip baseline-vs-rest top=0 bottom=2  diffPixels=0 maxDelta=0 comparable=True
COMPARE AeroDark title-band baseline-vs-rest top=0 bottom=31 diffPixels=0 maxDelta=0 comparable=True
COMPARE AeroDark white-strip baseline-vs-rest top=0 bottom=2  diffPixels=0 maxDelta=0 comparable=True
COMPARE Classic  title-band baseline-vs-rest top=0 bottom=31 diffPixels=0 maxDelta=0 comparable=True
COMPARE Classic  white-strip baseline-vs-rest top=0 bottom=2  diffPixels=0 maxDelta=0 comparable=True
```

Zero differing pixels in the title row band and the top-3-row white-strip check for all three
schemes. Style read-back on each run: `GWL_STYLE = 0x96CF0000` (all five style bits present,
unchanged from 22-05), window rect `(360,140)-(1560,940)` (1200x800 floating, no extra
sizing-border growth in floating state). Agent visual inspection of all three frames: a single
Compose-drawn gradient title band per scheme (AeroBlue navy / AeroDark indigo / Classic grey),
the title text, the three caption glyphs, the theme switcher directly below the band — no
native caption glyphs, no ghost caption, no white strip, no content offset. The windows look
exactly as the pre-phase baseline.

### Conflict #6 observability note (recorded fact, not a settlement)

The three PrintWindow frames show square corners and no drop shadow in every scheme — but this
is NOT evidence about what Windows 11 does on screen. PrintWindow renders only the window's own
content into a bitmap; corner rounding and the drop shadow are applied by the DWM compositor
OUTSIDE the window's content, after the window has drawn itself, so neither can ever appear in
a PrintWindow frame regardless of whether the OS applies them. The roadmap's PrintWindow-
comparison check therefore cannot answer C6 ("does Windows 11 round the corners / draw a shadow
by default once WS_CAPTION|WS_THICKFRAME are present?"). Whether the live window shows rounded
corners and/or a shadow is maintainer-observed only (22-06 Task 2), and whatever look is chosen
is then applied explicitly via DWM attributes with read-back (Task 3) so the result is
deterministic across Windows builds rather than depending on an unobservable default.

### Maintainer's observation and choice (Task 2)

Asked 2026-09-26 with the real showcase window open and the Visual Companion preview
(`22-corners-preview.html`, ВАРИАНТ A/B/C side by side) in the browser next to it.

**Reply (verbatim):** variant choice: «a»; observation (follow-up message): «углы не
скруглены щас».

**Recorded:** chosen variant = **A** (square corners, no system shadow — how the window
looked before this phase). Conflict #6 observation: on this machine Windows 11 did NOT
round the window's corners by default once `WS_CAPTION | WS_THICKFRAME` returned (no
explicit DWM attribute was set at that point). The maintainer did not mention a shadow —
recorded verbatim as shadow-unobserved, not inferred either way.

**Settled by:** 22-06 Task 2 (maintainer's observation + choice) and Task 3 (explicit DWM
application + attribute read-back).

### C6 (SETTLED) — does Windows 11 round the corners / draw a shadow by default?

**Observation (maintainer, 2026-09-26, verbatim):** «углы не скруглены щас» — on this machine
Windows 11 did NOT round the window's corners by default once `WS_CAPTION | WS_THICKFRAME`
returned; no shadow was mentioned (recorded as unobserved, not inferred). The agent's own
capture (PrintWindow) structurally cannot see compositor effects — recorded in the self-review.

**Choice (maintainer):** variant **A** — square corners, no system shadow (the pre-phase look),
picked from the Visual Companion preview (`22-corners-preview.html`) with the real window open.

**Application (Task 3):** `Win32Dwm.kt` hand-declares `dwmapi` (DwmSetWindowAttribute /
DwmGetWindowAttribute / DwmExtendFrameIntoClientArea + MARGINS); `CHOSEN_CORNER_LOOK =
WindowCornerLook.SQUARE_NO_SHADOW` (DWMWCP_DONOTROUND, 0 margins). `applyCornerPolicy` runs at
install (NativeWindowChromeRegistry) and on WM_SIZE SIZE_MAXIMIZED / SIZE_RESTORED AFTER
CallWindowProc(previous) (PITFALLS 24: square while maximized).

**Read-back (live, capture mode, standard JDK 21):** every call returned S_OK and the corner
preference read back as 1 (DONOTROUND) at install (floating), after SC_MAXIMIZE and after
SC_RESTORE — trace lines `AERO_CHROME event=dwm ... setHr=0x0 getHr=0x0 readBack=1` in
`.captures/22-corners/logs/winprobe-run-004636.929.out.log`. Maximize geometry unchanged
(client 0,0,1920,1078 vs rcWork 0,0,1920,1080).

**Visual effect:** the DWM visual itself is maintainer-observed, not agent-captured — goes to
the unconfirmed list in Plan 16. Title band + white-strip comparisons vs the pre-phase
baseline after the change: `COMPARE title-band baseline-vs-dwm diffPixels=0`,
`COMPARE white-strip baseline-vs-dwm diffPixels=0` (`.captures/22-corners/AeroBlue-rest-dwm.png`).

**Settled by:** 22-06 Tasks 2-3 (maintainer's observation + choice, explicit application,
attribute read-back). C6 SETTLED.

## Live regions (22-07)

Recorded by Plan 07 (Task 3), 2026-09-27, against the capture-mode showcase with live
region reporting (`HitTestRegionRegistry` + `classifyHitTest`, AeroTitleBar publishing via
`onGloballyPositioned`) and the single native drag path (no `WindowDraggableArea` on
Windows). Same machine as every prior plan. Probed via
`.captures/22-regions/Invoke-RegionsCheck.ps1` — one `:showcase:run` launch
(`-Paero.capture=true -Paero.chromeTrace=true`, pid 12636, standard JDK 21), full V11 at the
stock 1200x800, a `SetWindowPos` resize to 900x600 (SWP_NOMOVE|SWP_NOZORDER|SWP_NOACTIVATE),
500 ms settle, V11 re-run, size restored. JSON `.captures/22-regions/live.json`, launch log
`.captures/22-regions/logs/`.

Deviation from the plan's parenthetical: `-SkipMaximize` was NOT used on pass A — F9
(probe-driven `SC_MAXIMIZE` never takes the foreground on this machine, measured 3x in
22-01) says it is not required, and running the maximize cycle re-proves 22-05's passing
checks against the new classifier (orchestrator rule: do not regress passing V11 checks).
Pass B (resized) used `-SkipMaximize` only to keep the resized window's geometry undisturbed
for measurement.

### V11 at 1200x800 (pass A)

```
V11 V11-STYLE PASS
V11 V11-HT-CAPTION PASS expected=2 observed=2 (HTCAPTION) chain=SunAwtCanvas:-1 -> SunAwtFrame:2
V11 V11-HT-MAX PASS expected=9 observed=9 (HTMAXBUTTON) chain=SunAwtCanvas:-1 -> SunAwtFrame:9
V11 V11-HT-MIN-BOUNDARY PASS expected=min=1,captionLeftOfMin=2 observed=min=1,captionLeftOfMin=2
V11 V11-HT-CLOSE-BOUNDARY PASS expected=close=1,max=9 observed=close=1,max=9
V11 V11-HT-MAXIMIZED-TOP PASS expected=2 observed=2
V11 V11-MAX-WORKAREA PASS ... client=0,0,1920,1078 ... Bottom=2(autoHide,need1-4)=ok foregroundTaken=False
V11 V11-AUTOHIDE-EDGE PASS expected=auto-hide edges uncovered by >=1px observed=detected edges=Bottom deltas=Bottom=2
V11 SUMMARY pass=8 fail=10 skip=0
```

All four title-bar checks now answer from AeroTitleBar's OWN published layout rects, and
22-05's maximize/inset checks (V11-HT-MAXIMIZED-TOP, V11-MAX-WORKAREA, V11-AUTOHIDE-EDGE)
still PASS unchanged — the new classifier did not regress frame removal. Still FAIL,
expected and unchanged (Plan 11 scope): `V11-HT-EDGE-L/R/T/B`, `V11-HT-CORNER-*`,
`V11-HT-CLIENT-BOUNDARY` (resize bands), `V11-MINSIZE` (320x240 floor, observed 136x50 — the
WS_THICKFRAME tracking-size width clamp plus no library floor yet). Note the live-classifier
change in the FAIL texture vs 22-05: `edgeTop`/`cornerTL`/`cornerTR` previously answered
HTCAPTION via the spike's hardcoded 32dp row; corners TL/TR now answer HTCLIENT because the
published caption rect starts after the row's 8dp horizontal padding (the
`onGloballyPositioned` sits after `padding(horizontal = 8.dp)` per plan), while `edgeTop`
still lands inside the caption rect. All three remain FAIL against their HTTOP/HTTOPLEFT/
HTTOPRIGHT expectations either way — Plan 11's bands own them.

### Layout-follow check at 900x600 (pass B)

After `SetWindowPos` to 900x600 (client measured exactly 900x600), 500 ms settle:

```
V11 V11-HT-CAPTION PASS expected=2 observed=2 (HTCAPTION) chain=SunAwtCanvas:-1 -> SunAwtFrame:2
V11 V11-HT-MAX PASS expected=9 observed=9 (HTMAXBUTTON) chain=SunAwtCanvas:-1 -> SunAwtFrame:9
V11 V11-HT-MIN-BOUNDARY PASS expected=min=1,captionLeftOfMin=2 observed=min=1,captionLeftOfMin=2
V11 V11-HT-CLOSE-BOUNDARY PASS expected=close=1,max=9 observed=close=1,max=9
V11 SUMMARY pass=5 fail=10 skip=3
LAYOUT-FOLLOW PASS (4/4 title checks at 900x600)
```

The probe's named points are recomputed from the NEW client size (min.x 1077 -> 777), and
all four checks PASS at the new geometry — the hit-test answers follow the live Compose
layout through a resize, proving the published regions are real layout reads, not the
spike's hardcoded rectangles. Size was restored to 1200x800 afterwards.

### BTN-02 message-level proxy

Posted `WM_LBUTTONDOWN` (wParam MK_LBUTTON=1) / `WM_LBUTTONUP` with client coordinates of
the `min` point (1077,16) to the client-covering `SunAwtCanvas` child HWND found by
`Get-WinProbeChildren`:

```
BTN02-PROXY UNREACHED: posted WM_LBUTTONDOWN/UP produced no minimized=true reporter line within 5s
```

The posted messages did not reach Compose's `clickable` — no `minimized=true` reporter line
appeared within 5 s (recorded in `live.json` as `btn02Proxy.result = "posted messages did
not reach Compose within 5s"`). Per the plan this is NOT faked and NOT treated as a failure
of the library: a synthesized `PostMessage` mouse click is not the real input path (AWT is
known to drop or misroute posted button messages), and the HTCLIENT classification that
lets real clicks reach the buttons is already proven by `V11-HT-MIN-BOUNDARY` /
`V11-HT-CLOSE-BOUNDARY` PASS above. The actual click proof for BTN-02 stays with Plan 15's
real-input session. Nothing was left minimized; no restore was needed.

### Legacy structure (API-01), confirmed by code read

`AeroTitleBar.kt`: the non-Windows / install-failed branch composes exactly
`WindowDraggableArea(modifier = modifier) { TitleBarRow(..., rowModifier = Modifier, regions = null) }`.
With `regions == null`, `regionModifier` returns the no-op `Modifier` (so the Row's chain
stays `fillMaxWidth().height(32.dp).background(gradient).padding(horizontal = 8.dp)` and each
`TitleBarButton`'s chain stays `size(46,32).hoverable.clickable.background`), and `leading`
is invoked directly with no wrapper Box — the legacy composition is structurally identical
to the pre-22-07 component. Exactly one `WindowDraggableArea(` call site remains in the
file, inside that branch.

### Interference

`INTERFERENCE foregroundOwnedByShowcase=False (before=7060 after=7060 showcase=12636)` — the
showcase process never owned the foreground at any point in the run; the maintainer's
session was untouched. Every process this run started was stopped by the script itself
(post-run sweep: zero `com.mordred.showcase.MainKt` JVMs remain).

## Max-button interaction bridge (22-09)

Recorded by Plan 09 (Task 3), 2026-09-27, against the capture-mode showcase with the
`AeroMaxButtonInteraction` bridge installed (Tasks 1-2: the frame WndProc owns the
non-client mouse messages at HTMAXBUTTON and feeds Hover/Press interactions into the
maximize button's shared MutableInteractionSource through EDT hops). Same machine as every
prior plan. Probed via `.captures/22-maxbutton/Invoke-MaxButtonCheck.ps1` — one
`:showcase:run` launch (`-Paero.scheme=AeroBlue -Paero.capture=true
-Paero.chromeTrace=true`, pid 2560, standard JDK 21). JSON
`.captures/22-maxbutton/live.json`, launch log `.captures/22-maxbutton/logs/`.

### V11-HT-MAX (unchanged)

```
V11-HT-MAX PASS expected=9 observed=9 (HTMAXBUTTON) chain=SunAwtCanvas:-1 -> SunAwtFrame:9
```

### Click path (BTN-01 / WIN-05)

Posted `WM_NCLBUTTONDOWN` then `WM_NCLBUTTONUP` (wParam = 9 = HTMAXBUTTON, lParam = the
screen point of the `max` named point) to the frame, twice:

```
CLICK-TOGGLE-1 PASS placement=Maximized awtExtendedState=6
CLICK-TOGGLE-2 PASS placement=Floating awtExtendedState=0
```

The click toggles `windowState.placement` through exactly today's `onClick` code path (the
bridge's `onClick` is the same placement-toggle lambda the Compose `clickable` carries) —
no `WM_SYSCOMMAND` involved. The window was pushed to `HWND_BOTTOM` with `SWP_NOACTIVATE`
after each toggle. `INTERFERENCE foregroundTaken=False (before=7060 afterMax=7060
afterRestore=7060 showcase=2560)` — the placement toggle, like the probe-driven
`SC_MAXIMIZE` F9 measured, never took the foreground on this machine.

### Swallow check (no classic-button paint, no double-fire)

The chrome trace shows the frame proc handling both pairs and nothing else:

```
AERO_CHROME event=max-button hwnd=0xe0540 down
AERO_CHROME event=max-button hwnd=0xe0540 up click=true
AERO_CHROME event=max-button hwnd=0xe0540 down
AERO_CHROME event=max-button hwnd=0xe0540 up click=true
```

After the restore, the floating-state title band was PrintWindow-captured and compared
against the pre-phase baseline:

```
COMPARE title-band baseline-vs-rest top=0 bottom=31 diffPixels=0 maxDelta=0 comparable=True
COMPARE white-strip baseline-vs-rest top=0 bottom=2 diffPixels=0 maxDelta=0 comparable=True
```

Zero differing pixels — `DefWindowProc` never painted a classic caption button over the
Compose one, and exactly one toggle happened per posted pair (no double-fire).

### Hover/press visuals

Cannot be proven headlessly: `TrackMouseEvent` immediately reports leave while the real
cursor is elsewhere, so a posted `WM_NCMOUSEMOVE` cannot hold a hover. Hover/press parity
proof stays with the Plan 13 pixel test and the Plan 15 real-hover frames.

### C5 — SETTLED

**Approach used:** the interaction-source state bridge via EDT hop (ARCHITECTURE Pattern 3,
refined per this plan): instead of two `State<Boolean>`s, the native side emits real
`HoverInteraction` / `PressInteraction` objects into the `MutableInteractionSource` shared
with the unchanged `hoverable` + `clickable` chain, so background colour AND
`LocalIndication` overlays come from the existing rendering code — D-02 parity is
structural, not a matching exercise. Native → Compose crosses threads only through
`SwingUtilities.invokeLater`; the native side keeps two plain booleans (hovered/pressed) on
the toolkit thread (PITFALLS 6, T-22-19). Non-client leave tracking is re-armed on every
`WM_NCMOUSEMOVE` over the button (PITFALLS 12, T-22-21). The button keeps its `clickable`
(role, click action, focus and Enter activation unchanged — T-22-22).

**Evidence:** the click path above (posted NC down/up at HTMAXBUTTON toggles placement
Maximized ↔ Floating through today's `onClick` code path), the swallow check (trace +
0 differing pixels vs baseline), and the forwarding discipline (`WM_NCMOUSEMOVE` still
reaches `CallWindowProc`, the path the 22-04 early gate's Snap Layouts flyout needs).

**Fallback condition:** the FlatLaf-style non-client message re-injection approach is
recorded as **not needed**, unless Plan 15's real-hover frames show a visual mismatch
against the minimize button's hover frames.

**Settled by:** 22-09 T3 — C5 SETTLED (approach chosen, evidenced live, fallback named).

**Cleanup:** every process this run started was stopped by the script itself (post-run
sweep: zero `com.mordred.showcase.MainKt` JVMs remain).

## Don't break what works (22-10)

Recorded by Plan 10 (Task 2), 2026-09-27, against the capture-mode showcase with the Task 1
hardening installed (owned-message sets enforced at dispatch, `NativeWindowChromeRegistry.verify`
+ churn guard, WM_PARENTNOTIFY hop). Same machine as every prior plan (Windows 11 24H2 build
26100, one 1920x1080 monitor at 100% DPI, taskbar auto-hide ON). Evidence:
`.captures/22-dontbreak/run3-console.log` + `live.json` (state sync / GC / UIA, one
`:showcase:run` launch, pid 3992, standard JDK 21, `-Paero.chromeTrace=true`), and
`hotreload-console.log` (JBR reload check, one `:showcase:hotRun` launch, pid 15980).

**WNDPROC-equality mechanism (F8):** a cross-process `GWLP_WNDPROC` read is meaningless
(reads back 0, finding F8), so "frame and child WNDPROC equal the registry's current pointers"
is proven by the registry's OWN in-process comparison: `verify` reads the live
`GWLP_WNDPROC` of the frame and of every child on the EDT and traces the outcome. After each
state step below a fresh `AERO_CHROME event=verify hwnd=... frame=ok children=1` line appeared
(both procs still ours, exactly one child, no `reinstall`/second `child-subclass` repair line at
any point; `event=install` count stayed 1 for the whole run).

### 1. OS-originated state sync (WIN-05 / C4) — 6/6 PASS

| Step | Reporter after step | Verify intact |
|---|---|---|
| SC_MINIMIZE | `placement=Floating minimized=true` | yes (`frame=ok children=1`) |
| ShowWindow(SW_SHOWNOACTIVATE) | `minimized=false` | yes |
| SC_MAXIMIZE | `placement=Maximized` (`awtBounds=-8,-8,1936,1096`, `posDp=-8,-8` — the known sizing-border overhang, Plan 11's `WM_GETMINMAXINFO` territory) | yes |
| SC_RESTORE | `placement=Floating` | yes |
| SetWindowPos move +100,+50 (SWP_NOACTIVATE) | `posDp=460.0,190.0` = px/scale exactly (delta 0) | yes |
| SetWindowPos resize 1000x700 (SWP_NOACTIVATE) | `sizeDp=1000.0x700.0`, `awtBounds=460,190,1000,700` (delta 0) | yes |

`windowState.placement` / `isMinimized` / `size` / `position` all follow OS-originated changes
through AWT's own pipeline with zero push code — C4's "syncs automatically" re-confirmed on the
hardened subclass. `foregroundTaken=False` at every step.

### 2. GC stress (PITFALLS 1 / T-22-01) — PASS

20 rounds × (jcmd `<pid>` GC.run, then 25 `WM_NCHITTEST` through the real child→frame chain at
alternating caption / max / client points) = 500 hit-tests. jcmd ≈ 150–195 ms per round
(`C:\Users\1\.jdks\ms-21.0.9\bin\jcmd.exe`, the JVM that owns the window). Every round's 25
answers byte-identical to round 1 (`caption=2 [SunAwtCanvas:-1 -> SunAwtFrame:2]`,
`max=9 [SunAwtCanvas:-1 -> SunAwtFrame:9]`, `client=1 [SunAwtCanvas:1]` — the chain itself proves
both procs stayed installed). Process alive, window alive, zero new `hs_err_pid*.log`
(repo root and `showcase/` snapshots compared before/after). 20/20 rounds matching.

### 3. Accessibility (T-22-02) — PASS, equal to the 22-01 baseline

`Get-WinProbeUiaSummary`: `DescendantCount=1`, histogram `{ControlType.Pane: 1}` — byte-equal
to the pre-phase RED baseline. The owned-message discipline (everything not owned forwarded)
leaves the UIA subtree exactly as before the phase.

### 4. Hot Reload churn (PITFALLS 16 / T-22-09) — PASS

`:showcase:hotRun` on JBR 21 (`C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe`). Three
bytecode-changing edits in `AeroTitleBar.kt` (`Spacer(Modifier.width(8.dp))` ↔ `8.01.dp`, the
same edit shape 22-02 used), each followed by the top-level `reload` Gradle task (the task name
is `reload`, not `:showcase:reload` — 22-02's own finding; 31–44 s per reload incl. recompile):

| Reload | Source state | Answers vs baseline | install | reinstall | child-subclass |
|---|---|---|---|---|---|
| 1 | `8.01.dp` | identical | 1 | 0 | 1 (initial only) |
| 2 | `8.dp` | identical | 1 | 0 | 1 |
| 3 | `8.01.dp` | identical | 1 | 0 | 1 |

No stacked installs, no reinstall, hit-test answers unchanged throughout. `git diff --quiet --
library/` holds after the final revert; zero `com.mordred.showcase.MainKt` JVMs remain.

### 5. V11 regression sweep (checkpoint insurance) — unchanged vs 22-07

Full `Invoke-WinProbe -Launch run -Report v11` (`.captures/22-dontbreak/v11.json`,
`v11-console.log`): `pass=8 fail=10 skip=0` — the eight passing checks (V11-STYLE, HT-CAPTION,
HT-MAX, HT-MIN-BOUNDARY, HT-CLOSE-BOUNDARY, HT-MAXIMIZED-TOP, MAX-WORKAREA, AUTOHIDE-EDGE) all
still PASS with identical observed values; the ten FAILs are the same Plan 11/12 targets
(edge/corner bands, client boundary, 320x240 min-size) with unchanged observed values. The
hardening regressed nothing.

### 6. Conflict #2 headless half — recorded

- WS_SYSMENU is present on the live native-chrome window (style read-back
  `WS_POPUP,WS_CAPTION,WS_SYSMENU,WS_THICKFRAME,WS_MINIMIZEBOX,WS_MAXIMIZEBOX,WS_CLIPCHILDREN`,
  C1 after-state re-confirmed) — the style prerequisite for Alt+Space is in place.
- `GetSystemMenu` / `TrackPopupMenu` are absent from jna-platform 5.19.1: `unzip -l` over the
  resolved jar (sha1 dir `d1e54d9231da5ca3fa730d52960deaa555475468`, matching D-03's recorded
  checksum) returns zero matching entries — matching the planner's javap. Hand-declaring them is
  needed ONLY if Plan 15's real Alt+Space check fails (SNAP-05's decision stays Plan 15's).

### Tool notes (probe-side, no product change)

- `WinProbe.ps1` gained a `ShowWindow` DllImport + `SW_SHOWNOACTIVATE` (the plan's
  restore-from-minimize step names it; the probe had no wrapper).
- The first check run exposed that a pure move (no resize) emits NO verify trace — the churn
  guard now also listens to `componentMoved` (the plan's own "after EACH step" check requires a
  post-move signal; the Task 1 commit covers it).
- PS 5.1 quirk, recorded for future probe scripts: `ConvertTo-Json` on an ordered dictionary
  holding nested pscustomobjects (a stored reporter line) spun at 100% CPU for minutes in run 1;
  flattening those entries to strings fixed it. Run 1's own measurements (before the dump) all
  matched run 3's.

## Native resize (22-11)

Recorded by Plan 11 (Task 3), 2026-09-27, against the capture-mode showcase with the Tasks 1-2
changes installed (edge/corner bands in `classifyHitTest` behind a DPI-correct `resizeBandPx`,
the WM_GETMINMAXINFO min-track floor, `AeroResizeHandles` standing down while native chrome is
active). Same machine as every prior plan (Windows 11 24H2 build 26100, one 1920x1080 monitor
at 100% DPI, taskbar auto-hide ON). Probed via `tools/winprobe/Invoke-WinProbe.ps1 -Launch run
-Windows main,narrow -GradleProps "-Paero.chromeTrace=true"` — one launch, pid 3060, standard
JDK 21, both windows in non-focusable capture mode. Evidence: `.captures/22-resize/live.json`,
`.captures/22-resize/console.log`, launch log
`.captures/22-baseline/logs/winprobe-run-191550.489.out.log`. Maximize cycle included (F9:
probe-driven `SC_MAXIMIZE` never takes the foreground on this machine).

### Band metrics at 100% DPI

Measured read-only (`GetSystemMetricsForDpi` at DPI 96, `.captures/22-resize/Measure-Band.ps1`):
`SM_CXSIZEFRAME=4` (SM_CYSIZEFRAME also 4), `SM_CXPADDEDBORDER=4` → system sum 8 px; CMP's own
resizer `8 dp × 1.0 = 8 px` → `resizeBandPx = max(8, 8) = 8 px`. The two lower bounds coincide
on this machine, so the native band exactly covers every pixel CMP's own undecorated resizer
could claim — and inside the band the child answers `HTTRANSPARENT` (chains below), so neither
`AeroResizeHandles` (a no-op on this path since Task 2) nor CMP's resizer can receive the
press (WIN-02, T-22-25).

### Main window — V11 SUMMARY pass=18 fail=0 skip=0 (all 18 PASS)

```
V11 V11-STYLE PASS ... observed=WS_POPUP,WS_CAPTION,WS_SYSMENU,WS_THICKFRAME,WS_MINIMIZEBOX,WS_MAXIMIZEBOX,WS_CLIPCHILDREN
V11 V11-HT-CAPTION PASS expected=2 observed=2 (HTCAPTION) chain=SunAwtCanvas:-1 -> SunAwtFrame:2
V11 V11-HT-MAX PASS expected=9 observed=9 (HTMAXBUTTON) chain=SunAwtCanvas:-1 -> SunAwtFrame:9
V11 V11-HT-MIN-BOUNDARY PASS / V11-HT-CLOSE-BOUNDARY PASS
V11 V11-HT-EDGE-L PASS expected=10 observed=10 (HTLEFT) chain=SunAwtCanvas:-1 -> SunAwtFrame:10
V11 V11-HT-EDGE-R PASS expected=11 observed=11 (HTRIGHT) chain=SunAwtCanvas:-1 -> SunAwtFrame:11
V11 V11-HT-EDGE-T PASS expected=12 observed=12 (HTTOP) chain=SunAwtCanvas:-1 -> SunAwtFrame:12
V11 V11-HT-EDGE-B PASS expected=15 observed=15 (HTBOTTOM) chain=SunAwtCanvas:-1 -> SunAwtFrame:15
V11 V11-HT-CORNER-TL PASS expected=13 observed=13 (HTTOPLEFT) chain=SunAwtCanvas:-1 -> SunAwtFrame:13
V11 V11-HT-CORNER-TR PASS expected=14 observed=14 (HTTOPRIGHT) chain=SunAwtCanvas:-1 -> SunAwtFrame:14
V11 V11-HT-CORNER-BL PASS expected=16 observed=16 (HTBOTTOMLEFT) chain=SunAwtCanvas:-1 -> SunAwtFrame:16
V11 V11-HT-CORNER-BR PASS expected=17 observed=17 (HTBOTTOMRIGHT) chain=SunAwtCanvas:-1 -> SunAwtFrame:17
V11 V11-HT-CLIENT-BOUNDARY PASS expected=client=1,edgeLeft=10 observed=client=1,edgeLeft=10
V11 V11-HT-MAXIMIZED-TOP PASS expected=2 observed=2
V11 V11-MAX-WORKAREA PASS ... client=0,0,1920,1078 window=-8,-8,1928,1088 ... Bottom=2(autoHide,need 1-4)=ok foregroundTaken=False
V11 V11-AUTOHIDE-EDGE PASS ... deltas=Bottom=2
V11 V11-MINSIZE PASS expected=>=320x240 observed=320x240
```

- The nine previously-FAILING Plan 11 targets all turned GREEN in one step: the four edge
  codes, the four corner codes, V11-HT-CLIENT-BOUNDARY, and V11-MINSIZE. Every edge/corner
  answer arrives through the real child→frame chain (`SunAwtCanvas:-1 -> SunAwtFrame:<code>`
  at 2 px inside each edge/corner — the child bounces HTTRANSPARENT, the frame classifies the
  band), i.e. the answer comes from the frame after the child stepped aside, exactly as
  required.
- `V11-MINSIZE observed=320x240`: the probe's 50x50 `SetWindowPos` request clamped to exactly
  the 320×240 dp floor × scale 1.0. The plan's contingency ("SetWindowPos not clamped at all →
  record and move the min-size proof to Plan 15") did NOT trigger on this machine —
  DefWindowProc's `WM_WINDOWPOSCHANGING` applies the `WM_GETMINMAXINFO` min-track size to
  `SetWindowPos` for this `WS_THICKFRAME` window (pre-plan observed was 136x50, the raw
  tracking-size clamp with no library floor). The check itself is unchanged.
- The eight previously-passing checks are unregressed (four title-bar checks, V11-STYLE,
  V11-HT-MAXIMIZED-TOP, V11-MAX-WORKAREA, V11-AUTOHIDE-EDGE — identical observed values to
  22-07/22-10).
- Chrome trace (main hwnd 0xe80556): `AERO_CHROME event=mintrack appMinimumSet=false dpi=96
  floor=320x240` — the WM_GETMINMAXINFO handler raising ptMinTrackSize live, with F11's
  `minSizeSet=false` confirming the default-floor branch is the one taken.

### Narrow window (300x480 dp, own minimum 260x200) — pass=7 fail=1

```
V11 V11-N-HT-CAPTION PASS expected=2 observed=2 (HTCAPTION) chain=SunAwtCanvas:-1 -> SunAwtFrame:2
V11 V11-N-HT-MAX PASS expected=9 observed=9 (HTMAXBUTTON) chain=SunAwtCanvas:-1 -> SunAwtFrame:9
V11 V11-N-HT-LEADING-BOUNDARY PASS expected=leading=1,captionRightOfLeading=2 observed=leading=1,captionRightOfLeading=2
V11 V11-N-HT-MARKED-BOUNDARY FAIL expected=marked=1,captionLeftOfMarked=2 observed=marked=2,captionLeftOfMarked=2
V11 V11-N-HT-EDGE-L PASS expected=10 observed=10 (HTLEFT) chain=SunAwtCanvas:-1 -> SunAwtFrame:10
V11 V11-N-HT-EDGE-R PASS expected=11 observed=11 (HTRIGHT) chain=SunAwtCanvas:-1 -> SunAwtFrame:11
V11 V11-N-HT-EDGE-B PASS expected=15 observed=15 (HTBOTTOM) chain=SunAwtCanvas:-1 -> SunAwtFrame:15
V11 V11-N-MINSIZE PASS expected=>=260x200 and <320x240 observed=260x200
V11 SUMMARY narrow pass=7 fail=1 skip=0
```

- `V11-N-MINSIZE observed=260x200`: clamped to exactly the app's own AWT minimum, NOT
  320x240 — the library floor did not clobber the app value (D-01, T-22-24). No
  `event=mintrack` line exists for the narrow hwnd (0x950302): with `appMinimumSet=true` the
  handler leaves AWT's ptMinTrackSize untouched (it only writes when it raises a value).
- `V11-N-HT-EDGE-L/R/B` PASS — the narrow window resizes natively too; the 8 px band on a
  300 px-wide window still leaves the app's 260 px floor reachable from both sides.
- `V11-N-HT-LEADING-BOUNDARY` is now PASS (leading=1, captionRightOfLeading=2) — green since
  22-07's leading publishing (the 22-08 FAIL predates 22-07).
- `V11-N-HT-MARKED-BOUNDARY` FAIL (`marked=2, captionLeftOfMarked=2`, both HTCAPTION) is the
  standing RED for API-02, quoted verbatim above and not weakened — Plan 12's
  `markAeroTitleBarInteractive()` is what turns it GREEN.

### Interference / cleanup

`foregroundTaken=False` at the maximize step (in V11-MAX-WORKAREA's observed line); the
showcase process never owned the foreground — no interference with the maintainer's session
(the multi-window runner emits no separate INTERFERENCE line; the foreground flag inside the
maximize check is the observable). Post-run sweep: zero `com.mordred.showcase.MainKt` JVMs
remain (`.captures/22-resize/Sweep-Leftovers.ps1` → `NO-LEFTOVERS`).

## Public API and multi-window (22-12)

Recorded by Plan 12 (Task 3), 2026-09-27, against the capture-mode showcase with the Task 1
public surface (`nativeWindowManagement` opt-out, `markAeroTitleBarInteractive()`) and the
Task 2 showcase wiring (marked "Вернуть" overlay, `-Paero.nativeChrome=false` control) in
place. Same machine as every prior plan (Windows 11 24H2 build 26100, one 1920x1080 monitor
at 100% DPI, taskbar auto-hide ON), standard JDK 21 throughout. Evidence:
`.captures/22-publicapi/` — `green.json`, `multiwindow.json` + `multiwindow-console.log`,
`red-control.json` + `red-control-console.log`, scripts `Invoke-MultiWindowCheck.ps1`,
`Invoke-RedControl.ps1`, `Test-OptOut.ps1` (git-ignored); launch logs under
`.captures/22-baseline/logs/` and `.captures/22-publicapi/logs/`.

### GREEN: API-02 RED→GREEN (V11-N-HT-MARKED-BOUNDARY)

`Invoke-WinProbe.ps1 -Launch run -Windows main,narrow -GradleProps "-Paero.chromeTrace=true"
-AssertV11` (one launch, maximize cycle included per F9):

```
V11 SUMMARY main pass=18 fail=0 skip=0
V11 V11-N-HT-MARKED-BOUNDARY PASS expected=marked=1,captionLeftOfMarked=2 observed=marked=1,captionLeftOfMarked=2
V11 SUMMARY narrow pass=8 fail=0 skip=0
```

`V11-N-HT-MARKED-BOUNDARY` — FAIL (`marked=2,captionLeftOfMarked=2`) in 22-08 and 22-11,
quoted verbatim as the standing RED — now PASS: the marked overlay's pixels answer HTCLIENT
through the interactive-region path, the caption point left of it still answers HTCAPTION.
All 18 main checks and all 8 narrow checks PASS; nothing regressed. `-AssertV11` exited 0.

### BTN-02 message-level proxy (narrow window)

Posted `WM_LBUTTONDOWN`/`WM_LBUTTONUP` (MK_LBUTTON, client coordinates of the `marked`
(118,16) and `leading` (20,16) points) to the narrow window's client-covering `SunAwtCanvas`
child HWND (0x50482's canvas in the recorded run):

```
BTN02-PROXY marked UNREACHED (no AERO_EVENT within 3s)
BTN02-PROXY leading UNREACHED (no AERO_EVENT within 3s)
```

Posted client messages did not reach Compose within 3 s at either point — the same outcome
22-07 recorded for the `min` point on the main window. Recorded, not faked, and not a library
failure: the HTCLIENT classification that lets REAL clicks reach the elements is proven by
`V11-N-HT-MARKED-BOUNDARY` / `V11-N-HT-LEADING-BOUNDARY` PASS above; the actual click proof
stays with Plan 15's real-input session.

### Permanent RED control: -Paero.nativeChrome=false

`Invoke-RedControl.ps1` (wrapper around `Invoke-WinProbe.ps1 -Launch run -Windows main,narrow
-GradleProps @('-Paero.nativeChrome=false','-Paero.chromeTrace=true') -ExpectRed`):

```
STYLE main 0x960B0000 exStyle=0x08000000 names=WS_POPUP,WS_SYSMENU,WS_MINIMIZEBOX,WS_MAXIMIZEBOX,WS_CLIPCHILDREN
V11 SUMMARY main pass=0 fail=18 skip=0
STYLE narrow 0x960B0000 (same names)
V11 SUMMARY narrow pass=0 fail=8 skip=0
RED OK
```

- `RED OK` for BOTH windows: all 26 checks fail on the opt-out path, including
  `V11-N-HT-MARKED-BOUNDARY` (`marked=1,captionLeftOfMarked=1` — with no subclass the overlay
  point is plain client area, so even the boundary half fails) and `V11-N-MINSIZE`
  (`observed=50x50`: on the legacy style Windows does NOT clamp `SetWindowPos` to the app's
  AWT minimum — the honored-minimum behavior is itself a native-chrome-path trait).
- GWL_STYLE `0x960B0000` equals the pre-phase C1 value exactly (capture mode, so
  `WS_EX_NOACTIVATE` present per C1); the launch log contains ZERO `AERO_CHROME` lines — no
  restyle, no install, no child-subclass. The opt-out composes exactly the legacy branch.
- This launch property is the permanent RED control for every VER-11 check, including the
  narrow-window checks whose fixture did not exist before the phase.

**Tooling pitfall (recorded for every future probe invocation):** two earlier RED attempts
measured an all-PASS native window with zero `AERO_CHROME` lines because
`powershell.exe -File ... -GradleProps "-Paero.nativeChrome=false","-Paero.chromeTrace=true"`
from bash collapses the comma pair into ONE string (bash strips the inner quotes, so
PowerShell argument-mode binding never sees an array; the runner comma-normalizes `-Report`
and `-Windows` but not `-GradleProps`), so gradlew received
`-Paero.nativeChrome=false,-Paero.chromeTrace=true` — the property value was not `"false"`,
the opt-out never engaged, and the trace stayed off. A wrapper `.ps1` binding the array
literally is the reliable invocation shape from bash.

### WIN-06: multi-window independence

Part A (`Invoke-MultiWindowCheck.ps1`, one launch, both windows subclassed, pid 9596):
posted `WM_CLOSE` to the narrow window (hwnd 0x50482):

```
WIN06-CLOSE event=AERO_EVENT name=close-request label=narrow
AERO_CHROME event=ncdestroy hwnd=0x50482 entry dropped
WIN06-CLOSE narrowUninstall=1 mainUninstall=0 mainInstallSingle=True
WIN06-CLOSE mainTitleChecks=5/5 PASS (V11-STYLE, HT-CAPTION, HT-MAX, HT-MIN-BOUNDARY, HT-CLOSE-BOUNDARY)
V11 SUMMARY main pass=15 fail=0 skip=3 (post-close sweep, -SkipMaximize)
INTERFERENCE foregroundOwnedByShowcase=False (before=14116 after=14116 showcase=9596)
```

The posted WM_CLOSE runs the narrow window's own `onCloseRequest` (the reporter event proves
it), the HWND is destroyed before the composable's `onDispose` can run `release()`, so the
registry's WM_NCDESTROY path drops the entry — now traced (`event=ncdestroy`, added in
`44b91ae` after the first run left the close path silent). The main window shows no
uninstall/ncdestroy line, its install count stayed 1, and its title-bar hit-test answers are
byte-identical after its sibling closed.

Part B (same script, `-Paero.secondWindow=3000`): the main window's reporter was ready at
03.336, the narrow window's at 06.556 (gap 3220 ms ≈ the 3 s delay); the trace shows two
independent installs, each with its own child-subclass (`install hwnd=0x700a6` +
`child-subclass`, then `install hwnd=0x70486` + `child-subclass`) — a window opened while
another is already subclassed gets its own install.

Post-run sweep: zero `com.mordred.showcase.MainKt` JVMs remain.

## Custom title-bar API (22-19)

Recorded by Plan 19 (Task 3), 2026-09-27, against the capture-mode showcase with the public
`rememberAeroWindowChrome` / `AeroWindowChromeState` (Task 1, commit `d34c4a6`) and AeroTitleBar
rebuilt on top of it (Task 2, commit `d151d75`). Same machine as every prior plan (Windows 11
24H2 build 26100, one 1920x1080 monitor at 100% DPI, taskbar auto-hide ON), standard JDK 21
throughout, capture mode (non-focusable, behind). Evidence: `.captures/22-apicustom/` —
`green.json` + `green-console.log`, `red-control.json` + `red-control-console.log`,
`click-toggle.json` + `click-toggle-console.log` + `AeroBlue-rest.png`, scripts
`Invoke-GreenCheck.ps1`, `Invoke-RedControl.ps1`, `Invoke-ClickToggleCheck.ps1` (git-ignored);
launch logs under `.captures/22-baseline/logs/` and `.captures/22-apicustom/logs/`.

**D-05 proof choice (as recorded in the plan):** AeroTitleBar itself is built on
`rememberAeroWindowChrome` — not a separate showcase window with a non-AeroTitleBar header.
The API therefore carries the full verification weight of the phase: every VER-11 check on the
main and narrow windows, the `-Paero.nativeChrome=false` RED control and the click-toggle check
below run through it. Generality beyond AeroTitleBar's layout (a custom header with two caption
areas and a left-hand maximize button) is locked headlessly by Plan 13.

### GREEN: VER-11 through the API (identical codes to the Plan 12 GREEN run)

`Invoke-GreenCheck.ps1` (one launch, both windows, `-AssertV11`, maximize cycle included per F9):

```
V11 SUMMARY main pass=18 fail=0 skip=0
V11 SUMMARY narrow pass=8 fail=0 skip=0
```

Every check PASS with byte-identical observed values to the 22-12 GREEN run — e.g.
`V11-HT-CAPTION observed=2 chain=SunAwtCanvas:-1 -> SunAwtFrame:2`,
`V11-HT-MAX observed=9 chain=SunAwtCanvas:-1 -> SunAwtFrame:9`, edge/corner codes 10-17,
`V11-MINSIZE observed=320x240` (main) / `260x200` (narrow),
`V11-N-HT-MARKED-BOUNDARY observed=marked=1,captionLeftOfMarked=2` — now produced entirely
through the public chrome API's `captionArea()` / `captionExclude()` / `maximizeButtonArea()`
publishing (the id-keyed `captions` map replaced the Caption/Minimize/Close roles; precedence
unchanged: bands → Maximize → interactive → caption → client).

### RED control: the API's own opt-out

`Invoke-RedControl.ps1` (`-Paero.nativeChrome=false` → AeroTitleBar passes
`nativeWindowManagement = false` into `rememberAeroWindowChrome`):

```
STYLE main 0x960B0000 ... names=WS_POPUP,WS_SYSMENU,WS_MINIMIZEBOX,WS_MAXIMIZEBOX,WS_CLIPCHILDREN
V11 SUMMARY main pass=0 fail=18 skip=0
STYLE narrow 0x960B0000 (same names)
V11 SUMMARY narrow pass=0 fail=8 skip=0
RED OK
```

GWL_STYLE equals the pre-phase C1 value exactly; the launch log contains ZERO `AERO_CHROME`
lines — no restyle, no install, no child-subclass. `isNative == false` makes every modifier
inert and composes the legacy branch (API-03 semantics applied to API-04).

### Single install, native click toggle, title band (D-02)

`Invoke-ClickToggleCheck.ps1` (one single-window launch, AeroBlue):

```
INSTALL-COUNT PASS installs=[0x402c6=1] reuse=0
AERO_CHROME event=install hwnd=0x402c6 frameProc=native@0x6dc00010
V11-HT-MAX PASS expected=9 observed=9 (HTMAXBUTTON) chain=SunAwtCanvas:-1 -> SunAwtFrame:9
CLICK-TOGGLE-1 PASS placement=Maximized awtExtendedState=6
CLICK-TOGGLE-2 PASS placement=Floating awtExtendedState=0
INTERFERENCE foregroundTaken=False (before=14116 afterMax=14116 afterRestore=14116 showcase=15388)
COMPARE title-band baseline-vs-rest top=0 bottom=31 diffPixels=0 maxDelta=0 comparable=True
COMPARE white-strip baseline-vs-rest top=0 bottom=2  diffPixels=0 maxDelta=0 comparable=True
```

- The refactor introduced no duplicate acquire: exactly one `event=install` per HWND and zero
  `event=reuse` lines (the green two-window run's trace shows the same — one install + one
  child-subclass per HWND, `0x604de` and `0x704ca`, T-22-34).
- A posted NC down/up pair at HTMAXBUTTON still toggles `windowState.placement`
  Maximized ↔ Floating — through the toggle that now lives only in `rememberAeroWindowChrome`
  (BTN-01 / WIN-05 wiring preserved by the move).
- The floating title band is pixel-identical to the pre-phase baseline (0 differing pixels,
  maxDelta 0) — the rebuild changed the wiring, not the pixels (D-02).

Post-run sweep: zero showcase JVMs remain (none with `com.mordred.showcase.MainKt` /
`aero.capture` / `aero.chromeTrace` in the command line).

## Final hand-off (22-16)

Recorded by Plan 16 (Task 3), 2026-09-28, after the teardown (Task 2, same plan). Scripts:
`.captures/22-final/Invoke-FinalFrames.ps1`, `Invoke-RedControl.ps1`, `Invoke-FrameReview.ps1`
(git-ignored). Same machine as every prior plan (Windows 11 24H2 build 26100, one 1920x1080
monitor at 100% DPI, taskbar auto-hide ON — restored and verified by the teardown minutes
earlier), standard JDK 21, capture mode (non-focusable, behind) throughout; zero
`com.mordred.showcase.MainKt` JVMs before and after every run.

### Inspection method (honest limitation)

This executor environment cannot render images visually — a `Read` of a PNG returns a CDN
upload reference, not pixels the agent can see. The 22-16 frame review is therefore
MEASUREMENT-BASED, using the phase's own LockBits tooling (`Compare-WinProbeRegion`) with
full-frame comparisons and sliding band sweeps — the method 22-05 already sanctioned for
separating real defects from expected-but-invisible artifacts ("pixel-sampled, not just
visually inspected"). The visual dimension of the same windows was agent-inspected with eyes
earlier in the phase where the environment allowed it (22-06 corner sweep self-review, 22-09
max-button frames, 22-19 API rebuild frames — all recorded above, all "no ghost caption, no
white strip, single Compose-drawn gradient title band"). Nothing below is presented as a
visual verdict by this plan; every claim is a pixel count.

### Final frames (.captures/22-final/)

9 frames: main + narrow, AeroBlue/AeroDark/Classic, floating; main maximized per scheme
(probe-driven `SC_MAXIMIZE`, F9 = safe). Console: `finalframes-console.log`.

```
COMPARE <scheme> title-band baseline-vs-rest top=0 bottom=31 diffPixels=0 maxDelta=0 comparable=True   (x3 schemes)
COMPARE <scheme> white-strip baseline-vs-rest top=0 bottom=2 diffPixels=0 maxDelta=0 comparable=True    (x3 schemes)
REVIEW <scheme>-main-rest-vs-baseline FULL-FRAME 1200x800 diffPixels=0 maxDelta=0 comparable=True       (x3 schemes)
```

The main floating window is byte-identical to the pre-phase baseline across the ENTIRE frame
— not just the title band — in all three schemes, after everything the phase changed
(restyle, `WM_NCCALCSIZE`, DWM corner policy, the `rememberAeroWindowChrome` rebuild, the
max-button bridge). Even the two known showcase noise components (progress shimmer, layout
counter) landed identical in these frames. Style read-back per run: `GWL_STYLE=0x96CF0000`
both windows; main rect 360,140-1560,940 (1200x800), narrow 1620,300-1920,780 (300x480).
Maximized frames are 1936x1096 (auto-hide ON work area; the session's maximized frames were
1936x1048 under the visible taskbar — the environment difference is consistent).

Cross-run stability: the session's `W03-FRAMES-*-floating` frames (jdk AND jbr passes) vs
the final AeroBlue frames: **0 px diff** for both the main (1200x800) and the narrow
(300x480) window — the real-input session passes and today's post-teardown run rendered
identically.

### Final VER-11 (GREEN + RED, same day, post-teardown)

- GREEN `v11-green.json` / `v11-green-console.log`: main **18/18 PASS**, narrow **8/8 PASS**,
  `-AssertV11` exit 0 — observed values byte-identical to the 22-12/22-19 GREEN runs (e.g.
  `V11-MINSIZE observed=320x240` main / `260x200` narrow, all edge/corner codes 10-17 through
  the real child→frame chain, `client=0,0,1920,1078 Bottom=2(autoHide,need 1-4)=ok`).
- RED `v11-red-control.json` / `v11-red-console.log` (`-Paero.nativeChrome=false` +
  `chromeTrace`, wrapper-bound array): `STYLE 0x960B0000` (= the pre-phase C1 value), main
  **0/18**, narrow **0/8**, `RED OK`, launch log contains **zero** `AERO_CHROME` lines.
- RED provenance: the Plan 01 pre-phase baseline (`.captures/22-baseline/red-main.json`,
  `V11 SUMMARY pass=0 fail=18` — every main check FAIL on the unmodified window) is the
  original RED; the opt-out control additionally covers the 8 narrow checks whose fixture did
  not exist pre-phase. Every VER-11 GREEN therefore has a RED counterpart.

### Session frame re-review (measurements by Invoke-FrameReview.ps1)

- **Hover (BTN-01)**: `strip-hover-max` vs `strip-hover-min` = **0 px** on both passes —
  the native-driven hover render of the maximize button is pixel-identical to the minimize
  button's Compose-driven hover (D-02 parity where it was proven). Hover vs rest = 108 px
  (hover paints).
- **Press (BTN-01)**: `strip-press-max` vs `strip-press-min` = **108 px, maxDelta 176** on
  both passes — the press fill differs between max and min (the C5 named fallback
  condition; the standing B01-PRESS gap, unchanged).
- **W05 glyph crops (28x22)**: jdk float1-vs-float2 = **0 px** (stable); jbr float1-vs-float2
  = **588 px, maxDelta 54** (unstable — the JBR anomaly; the session record's own counter
  said 616 px with the same maxDelta 54, a counting difference between the two tools, same
  conclusion). float-vs-max = 588 px on both (the maximize/restore glyph switch — expected).
- **Snapped frames**: jbr `W03-FRAMES-main-snapped` is 960x1032 (a genuinely snapped frame —
  landing on the half-work-area rect S01 defines). jdk `W03-FRAMES-main-snapped` is 1200x800
  and byte-identical to the floating frame — the capture moment on the JDK pass caught the
  restored window, so THAT PNG carries no snapped evidence; the JDK snap proof is the S01
  rect records (`0,0,960,1032` etc.), not this frame. Both passes' `narrow-snapped` frames
  are 300x480 identical-to-floating (same situation for the narrow window).
- **F18 resize sets**: native 0-4 = 1200/1225/1250/1275/1300 x800 (five distinct widths);
  the opt-out comparison sets are five frames all at 1200x800 on both passes (the opt-out
  control window did not change size across its captures).
- **Frame-context oddities (recorded, strips unaffected)**: on both passes
  `B01-PRESS-FRAME-press-min.png` is a 1936x1048 (maximized) frame while `press-max.png` is
  1200x800 — the press captures ran in different placement states; the strip comparisons
  are button-local and unaffected, but the full frames are not state-matched pairs.
- Sizes not directly comparable (SKIP by the review script, geometry carried by the session
  records instead): maximized vs floating (1936x1048 vs 1200x800), jbr main snapped vs
  floating (960x1032 vs 1200x800).

### Verdict of the review

Nothing new found: every measured difference maps to an already-recorded session finding
(B01-PRESS gap, JBR W05 crop instability, frame-context oddities) or is the expected glyph
switch. The final windows are pixel-identical to the pre-phase product in all three themes,
and the final VER-11 run is GREEN 26/26 with both RED controls alive.
