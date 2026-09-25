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

**Check performed:** Not checked live here — `WS_SYSMENU` is already confirmed present on the
pre-phase window (see C1), which is the headless half of this conflict. The actual flyout/menu
behavior needs real keyboard input.

**Decision:** Deferred — needs the real-input session.

**Settled by:** 22-15 T3.

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

**Settled by:** 22-05 T3 + 22-15 T3.

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
