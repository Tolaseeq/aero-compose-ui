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
