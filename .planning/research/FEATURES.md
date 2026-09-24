# Feature Research

**Domain:** Native Windows window management for undecorated Compose Desktop windows (Aero Snap, Snap Layouts, Snap Groups, DWM chrome) on top of a JNA `WM_NCHITTEST`/`WM_NCCALCSIZE` subclass, for `AeroTitleBar` + `AeroResizeHandles`.
**Researched:** 2026-09-25
**Confidence:** HIGH for the Win32/DWM mechanisms (Microsoft Learn, current pages, dated 2025‑07‑14 / 2026‑06‑16). MEDIUM for how those mechanisms interact with an AWT/Compose-Desktop `HWND` specifically — there is no official JetBrains documentation for this; the conclusions below are corroborated by converging prior art (Windows Terminal, FlatLaf/Swing, Flutter's Windows embedder, WPF, Electron, Chromium) that all solved the identical problem the identical way, plus two open, unresolved JetBrains issues confirming Compose Desktop has no built-in answer.

## Executive summary

Every behavior in the maintainer's list is a **stock Win32/DWM feature that any normal top-level window gets automatically** — Windows doesn't know or care that the window is "Compose". The entire job is: (1) put back the window styles `undecorated = true` strips out, (2) answer three messages correctly (`WM_NCCALCSIZE`, `WM_NCHITTEST`, `WM_GETMINMAXINFO`), and (3) forward the maximize button's clicks through the non-client path (`WM_NCLBUTTONDOWN`/`UP` with `HTMAXBUTTON`) instead of Compose's `clickable`. Once that's right, Snap, Snap Layouts, Snap Groups, shared-border resize, Aero Shake, Win+arrow, Alt‑Tab thumbnails, shadow and Win11 rounded corners all follow **for free** — none of them are things to build, only things to unlock. The only real engineering is the JNA subclass itself, the maximize-button/interactive-region hit-test plumbing, and the taskbar-aware maximize (`WM_GETMINMAXINFO`), which Java's own undecorated-window maximize does *not* get right by default.

The maintainer's assumed approach (JNA WndProc subclass; `WM_NCHITTEST` → `HTCAPTION`/`HTMAXBUTTON`/resize codes/`HTCLIENT`; keep `WS_THICKFRAME`/`WS_MAXIMIZEBOX`/`WS_MINIMIZEBOX`; remove the frame via `WM_NCCALCSIZE`) is **correct and matches every independent implementation found** (Windows Terminal, FlatLaf, Flutter, WPF, Electron, Chromium). One correction/addition: it must also **keep (or add back) `WS_CAPTION` and `WS_SYSMENU`** — these are required for the Alt+Space system menu, for `WS_SYSMENU` itself (which requires `WS_CAPTION`), and they materially help DWM apply the shadow and Win11 rounded corners. Removing all visible chrome is done entirely inside `WM_NCCALCSIZE` (return 0 for the client rect = window rect), not by stripping these styles at the `WS_*` level. Because Compose Desktop's `Window(undecorated = true)` creates a bare `WS_POPUP` peer lacking `WS_CAPTION`/`WS_SYSMENU`/`WS_MINIMIZEBOX`/`WS_MAXIMIZEBOX` (and possibly `WS_THICKFRAME` — unconfirmed, verify empirically), the JNA layer must **add these styles back via `SetWindowLongPtr(GWL_STYLE, …)`** before the `WM_NCCALCSIZE` trick has anything to act on. This is a load-bearing prerequisite step the maintainer's summary didn't spell out.

## OS Mechanism Reference

For every required and adjacent behavior: the Win32/DWM mechanism, Windows 10 vs 11 differences, whether it's automatic once hit-testing/styles are right or needs extra code, how the app observes the result, and how to verify it. Confidence marked per row; HIGH = Microsoft Learn fetched directly, MEDIUM = cross-framework prior art convergence, LOW = single community source.

| # | Behavior | OS mechanism | Win10 vs Win11 | Automatic vs needs code | How app observes result | Confidence |
|---|----------|---------------|-----------------|--------------------------|--------------------------|------------|
| 1 | Drag to edge/corner snaps (half/quarter), drag away restores | System move loop entered via `WM_NCLBUTTONDOWN`(`HTCAPTION`) or `DefWindowProc`'s handling of `WM_NCHITTEST`→`HTCAPTION`; DWM tracks cursor proximity to screen edges during that loop and calls `SetWindowPlacement`/moves the HWND | Both — quarter-snap (drag to corner) since Windows 10; halves since Windows 7 | **Needs code**: only the correct `WM_NCHITTEST` return (`HTCAPTION` over the draggable strip, not over buttons/interactive regions) | HWND rect changes; AWT `ComponentListener` fires `componentMoved`/`componentResized`; Compose `WindowState.size`/`position` update if wired to those events (see placement discussion below) | HIGH — [WM_NCHITTEST](https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest), [WM_SYSCOMMAND](https://learn.microsoft.com/en-us/windows/win32/menurc/wm-syscommand) |
| 2 | Hovering maximize button shows Snap Layouts flyout | `WM_NCHITTEST` must return `HTMAXBUTTON` over the button's rect; DWM then owns hover/flyout entirely | **Windows 11 only** — feature does not exist on Windows 10 (no flyout; hover does nothing extra) | **Needs code** — explicitly documented gap: apps with custom captions lose this unless they answer `HTMAXBUTTON` | Flyout is drawn entirely by DWM outside the app; app only sees `WM_NCLBUTTONDOWN`(`HTMAXBUTTON`) if the user clicks a cell inside it (functionally: the window ends up snapped) | HIGH — [Support snap layouts for desktop apps on Windows 11](https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-snap-layout-menu) (updated 2026‑06‑16) |
| 3 | Win+Left/Right/Up/Down | Shell hotkey → `WM_SYSCOMMAND`-equivalent internal snap call on the foreground window; requires the window to be snap-eligible (`WS_THICKFRAME`, not `WS_EX_TOOLWINDOW`, min size compatible) | Both, works identically; only the *flyout* (behavior #2) is Win11-only | Automatic once styles/hit-test are correct — **not gated on `HTMAXBUTTON`** at all (confirmed: "Aero Snap by dragging and Win+Arrow keyboard shortcuts are unaffected" even if `HTMAXBUTTON` is never returned) | Same as #1 | MEDIUM — corroborated across multiple GitHub issues discussing custom-titlebar snap regressions, not a single dedicated MS Learn page |
| 4 | Double-click title bar maximizes/restores | `DefWindowProc` default handling of `WM_NCLBUTTONDBLCLK` with `HTCAPTION` → `SC_MAXIMIZE`/`SC_RESTORE` | Both | Automatic — **only if `WM_NCLBUTTONDBLCLK` is forwarded to `DefWindowProc`** (or the subclass calls it) rather than swallowed by Compose's own double-click detection on the draggable Row | `windowState.placement` changes if wired through `WM_SIZE`/`SC_MAXIMIZE`↔Compose bridge | HIGH — standard `DefWindowProc` behavior for `WS_CAPTION` windows, referenced from [WM_NCHITTEST](https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest) remarks (`HTCAPTION`) |
| 5 | Alt+Space system menu | `WM_SYSCOMMAND` with `SC_KEYMENU`, auto-dispatched by the system when the window has `WS_SYSMENU`; `DefWindowProc` draws and runs the menu | Both | Automatic **if `WS_SYSMENU`/`WS_CAPTION` styles are present** and unhandled `WM_SYSCOMMAND` reaches `DefWindowProc` | Native popup menu; on selection, standard `SC_MOVE`/`SC_SIZE`/`SC_MAXIMIZE`/`SC_MINIMIZE`/`SC_CLOSE` arrive as normal `WM_SYSCOMMAND` | HIGH — [WM_SYSCOMMAND](https://learn.microsoft.com/en-us/windows/win32/menurc/wm-syscommand) (`SC_KEYMENU`, requires `WS_SYSMENU`) |
| 6 | Snapped pair shares a border (resize one, other follows) | Windows 11's window manager tracks two windows as a snap pair and drags both borders together during the resize loop | **Windows 11 only** ("Resize a snapped window and any adjacent snapped window simultaneously", System ▸ Multitasking) | Automatic once #1/#3 work correctly on *both* windows — no extra code; purely DWM bookkeeping once both HWNDs are recognized as snapped | Both HWNDs get `WM_WINDOWPOSCHANGING`/`WM_SIZE` in the same user gesture | MEDIUM — community sources (ElevenForum, GadgetHacks); no MS Learn page fetched specifically for this setting — **flag for validation** |
| 7 | Snap Groups in the taskbar | Shell/taskbar groups HWNDs it recognizes as a snapped set; shown on taskbar hover, Task View, Alt‑Tab | **Windows 11 only** | Automatic, no app code — purely shell UI once windows are snapped via #1/#3 | Not observable by the app at all | MEDIUM — general Windows 11 feature documentation (Microsoft Support "Snap your windows"), consistent across multiple sources |
| 8 | Maximize fills the monitor's work area, never covers the taskbar (incl. auto-hide) | `WM_GETMINMAXINFO` → app must set `MINMAXINFO.ptMaxSize`/`ptMaxPosition` to the monitor's `MONITORINFO.rcWork`, not `rcMonitor`; must be computed **per-monitor** (`MonitorFromWindow`) and re-evaluated for auto-hide taskbars, which reduce `rcWork` further | Both — same mechanism; nothing Win11-specific | **Needs code, always** — this is the one behavior that Java's own undecorated-frame maximize is known to get wrong by default (it will cover an auto-hide taskbar, and can cover any taskbar depending on JDK/AWT peer behavior for `WS_POPUP`-style windows) | `GetWindowRect` after `SC_MAXIMIZE` should equal `rcWork`, not `rcMonitor` | HIGH — [WM_GETMINMAXINFO](https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-getminmaxinfo) (`ptMaxSize`/`ptMaxPosition`); `MONITORINFO.rcWork` is documented Win32 multi-monitor API |
| 9 | Multi-monitor different scaling (100%/150%) keeps sane sizes | Per-Monitor-V2 DPI awareness (`WM_DPICHANGED` carries a suggested `RECT*` in `lParam`); JDK 9+ AWT is Per-Monitor-V2 aware by default on Windows | Both | Automatic via JDK's own DPI handling **unless** the new WndProc subclass intercepts `WM_DPICHANGED` and fails to forward the suggested rect — a real regression risk introduced *by this milestone*, not solved by it | Window's logical `DpSize` (Compose `dp`) stays visually constant across the move; physical pixel size changes | MEDIUM — Per-Monitor-V2 default in JDK 9+ is documented Java/AWT behavior, not independently verified against this specific subclass interaction; **only testable with a second monitor at 150%, which this dev machine (single 1920×1080 @100%) lacks** |
| 10 | Several such windows in one app (main + narrow detachable queue window) | Each top-level HWND needs its own subclass instance and its own hit-test state (maximize-button rect, interactive-region list) | Both | **Needs code** — must be designed as a per-window installer/uninstaller from day one, not a process-global hook | N/A — an architecture requirement, not an OS-observable behavior | N/A (design constraint, not an OS fact) |
| 11 | FancyZones (PowerToys) picks the window up | FancyZones hooks the same system move loop (`WM_NCLBUTTONDOWN`/`HTCAPTION`) and low-level mouse/keyboard hooks; it inspects standard window styles to decide zoning eligibility | Both (PowerToys ships for both) | Automatic once #1 works — FancyZones does not require anything beyond standard Aero-Snap-eligible window behavior | Window repositioned into a FancyZones zone on drop | LOW — general PowerToys documentation/community knowledge, not MS Learn; **cannot be verified on this machine — PowerToys is not installed** |
| 12 | Minimize/restore animations | DWM window-minimize transition (genie/thumbnail animation), triggered by `SC_MINIMIZE`/`ShowWindow(SW_MINIMIZE)` | Both | Automatic — a DWM compositor effect independent of window chrome, as long as `SC_MINIMIZE` reaches `DefWindowProc` normally | Purely visual | HIGH — standard DWM behavior for any composited top-level window; no special MS Learn citation needed (default OS behavior, not opt-in) |
| 13 | Taskbar click to minimize/restore | Taskbar sends `WM_SYSCOMMAND`(`SC_MINIMIZE`/`SC_RESTORE`) or calls `ShowWindow` directly | Both | Automatic; unrelated to custom chrome — already works today via the existing minimize button's `windowState.isMinimized` wiring | `windowState.isMinimized` toggles | HIGH — standard shell behavior |
| 14 | Aero Shake (shake title bar to minimize all other windows) | DWM watches the drag gesture during the `HTCAPTION` move loop for a shake pattern | Both (Windows 7+ feature, still present in 10/11, togglable in Settings) | Automatic once `HTCAPTION` hit-testing is correct over the drag strip — **zero extra code** | Other top-level windows minimize; shaking again restores them | MEDIUM — well-documented long-standing DWM feature, no single current MS Learn page fetched for this specific mechanic |
| 15 | Win+Shift+Left/Right/Up/Down (move window to adjacent monitor) | Shell hotkey directly repositions the HWND to the equivalent location on the neighboring monitor | Both | **Fully automatic, built into Windows — "no additional code or custom setup"** for any top-level window | `WM_WINDOWPOSCHANGED`/`WM_MOVE` on the new monitor | MEDIUM — consistent across multiple consumer documentation sources (How-To Geek etc.), standard shell shortcut since Windows 10 |
| 16 | Win+D / Show desktop | Shell sends `WM_SYSCOMMAND`(`SC_MINIMIZE`)-equivalent to all top-level windows | Both | Automatic, unrelated to custom chrome | All windows minimize | HIGH — standard shell behavior, same mechanism as #13 |
| 17 | "Show this window on all desktops" | Shell/taskbar right-click context menu action calling `IVirtualDesktopManager` on the HWND; entirely shell-side | **Windows 10 (1809+)/11 both have Task View + virtual desktops**; the "show on all desktops" / "show window on this desktop only" context-menu toggle is present in both, UI differs slightly | Automatic — **no library code needed**, any normal taskbar-visible top-level window qualifies | Not observable from inside the app | MEDIUM — general Windows Virtual Desktops documentation; not verified against a custom-chrome window specifically |
| 18 | Alt+Tab thumbnails | DWM live-thumbnail (same mechanism behind Task View), works for any visible top-level window that isn't `WS_EX_TOOLWINDOW` and isn't owned | Both | Automatic — **already works today**, independent of this milestone; unaffected by the `WM_NCCALCSIZE` chrome removal | Thumbnail quality reflects last-painted client content | HIGH — standard DWM composition behavior for top-level windows |
| 19 | Window shadow and Win11 rounded corners | Shadow: DWM's standard `WS_CAPTION` drop-shadow (do **not** need `DwmExtendFrameIntoClientArea` unless the shadow disappears because `WM_NCCALCSIZE` zeroed the frame too aggressively — some implementations extend a 1px margin to keep it). Rounded corners: automatic DWM behavior for any normal top-level overlapped window on Windows 11 (`DWMWA_WINDOW_CORNER_PREFERENCE` defaults to `DWMWCP_DEFAULT` = system decides = rounded); only needed if the app wants to *override* the default (e.g., force square) | Rounded corners: **Windows 11 only** (no-op API on Windows 10). Shadow: both | Shadow: automatic once `WS_CAPTION` is kept, may need `DwmExtendFrameIntoClientArea({0,0,1,0})` if it's clipped by the zero-height `WM_NCCALCSIZE` return. Rounded corners: **fully automatic, zero code**, unless opting out | Visual only | HIGH — [DwmExtendFrameIntoClientArea](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmextendframeintoclientarea), [Apply rounded corners in desktop apps for Windows 11](https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-rounded-corners) |
| 20 | Restore-from-maximize by dragging the title | Built into `DefWindowProc`'s handling of an `HTCAPTION` drag start while `WM_SYSCOMMAND` state is `SC_MAXIMIZE`: the system implicitly issues `SC_RESTORE` then continues the move loop | Both | Automatic once #1/#4 hit-testing is correct | `windowState.placement` flips to Floating mid-gesture | HIGH — standard `DefWindowProc`/caption-drag behavior |
| 21 | Snap Assist after snapping (thumbnails of other open windows offered to fill the rest) | DWM shows Snap Assist automatically the instant a window finishes an edge/corner snap or a `HTMAXBUTTON` flyout snap | Both (Snap Assist itself dates to Windows 10; the flyout that leads into it in #2 is Win11-only) | Automatic — a consequence of #1/#2/#3 working, no separate code | Visual only, user picks a neighbor | MEDIUM — standard Windows 10/11 Snap Assist behavior, general documentation, not independently MS-Learn-cited here |
| 22 | Keyboard focus after snap | Standard Win32 focus rules: the snapped window keeps foreground/input focus through the move/size loop and any subsequent `SetWindowPos` | Both | Automatic — **only breaks if the JNA subclass or hit-test handling accidentally eats keyboard/focus messages** | `GetForegroundWindow()`/Compose focus state should be unchanged before/after | MEDIUM — inferred from standard Win32 focus semantics; the risk is regression introduced by the subclass, not the OS |

## Feature Landscape

### Table Stakes (Ship in v3.2.0 — the maintainer's explicit bullet list)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Edge/corner Aero Snap + drag-away restore (#1) | Literally table stakes for *any* Windows app; users drag windows to edges reflexively | MEDIUM | Just correct `HTCAPTION`/resize-code hit-testing; the drag itself is 100% OS |
| Snap Layouts flyout on maximize-button hover (#2) | Windows 11's primary window-management UX since 21H2 | MEDIUM–HIGH | Needs `HTMAXBUTTON` over the exact button rect, tracked live as the title bar re-lays-out (theme change, narrow-window truncation, Pinya's counter shifting the button) |
| Win+Arrow snap (#3) | Power-user default expectation, keyboard-only workflow | LOW | Free once #1's hit-testing is correct — not gated on `HTMAXBUTTON` at all |
| Double-click title maximizes/restores (#4) | Universal Windows convention since Windows 95 | LOW | Just don't let Compose's own double-tap gesture detector on the draggable Row swallow `WM_NCLBUTTONDBLCLK` before it reaches `DefWindowProc` |
| Alt+Space system menu (#5) | Accessibility/keyboard-only users rely on this; also screen-reader convention | LOW | Requires keeping `WS_SYSMENU`+`WS_CAPTION` — a correction to the maintainer's assumed style list |
| Shared-border resize of a snapped pair (#6) | Explicitly called out by the maintainer for Pinya's main+queue window pair | LOW (free, but Win11-only) | Zero code — automatic DWM bookkeeping once #1/#3 work on both windows. On Windows 10 this simply does not happen; that's correct OS behavior, not a bug to work around |
| Snap Groups in taskbar (#7) | Explicitly requested | LOW (free, Win11-only) | Same as #6 — purely a consequence, no code |
| Maximize fills work area, respects auto-hide taskbar (#8) | The one thing Java's own undecorated-maximize is known to get wrong | MEDIUM–HIGH | Requires a `WM_GETMINMAXINFO` handler using `MonitorFromWindow` + `GetMonitorInfo(...).rcWork`, re-evaluated per monitor, and must specifically account for the auto-hide taskbar this dev machine already has enabled |
| Multi-monitor 100%/150% sane sizing (#9) | Explicitly requested; matches this org's real deployment shape | MEDIUM, but **cannot be fully verified on this machine** (single monitor, 100% only) | Primary risk is regression: don't let the new `WM_DPICHANGED` interception break JDK's existing Per-Monitor-V2 handling |
| N windows per app, with interactive title-bar content (#10, Pinya) | Explicitly requested consumer scenario (main window + narrow ~300px detachable queue window with a counter + "return queue" action in its title bar) | HIGH | Drives the public API shape below — needs per-window subclass lifecycle and a way to mark sub-regions of the title bar as `HTCLIENT` |
| FancyZones pickup (#11) | Explicitly requested | LOW (free) | Automatic once #1 works; **verification on this machine is blocked — PowerToys is not installed** |
| Maximize/minimize animations (#12) | Missing = feels broken/janky compared to every other window | LOW (free) | Automatic DWM compositor effect |
| Taskbar click minimize/restore (#13) | Baseline taskbar interaction | LOW (already works) | Unaffected by this milestone |
| Alt+Tab thumbnails (#18) | Users expect a live preview, not a blank/garbled tile | LOW (already works) | Independent of the chrome-removal work; verify it doesn't regress |
| Window shadow + Win11 rounded corners (#19) | Explicitly requested; also load-bearing for the "looks like a native window" goal | MEDIUM | Shadow may need a 1px `DwmExtendFrameIntoClientArea` margin if `WM_NCCALCSIZE` zeroes it out entirely; rounded corners are free on Win11 |
| Restore-by-dragging-maximized-title (#20) | Universal convention | LOW (free) | Comes with #1/#4 |
| Snap Assist after snapping (#21) | Explicitly requested corollary of Snap | LOW (free) | Automatic consequence of #1–#3 |
| Keyboard focus preserved after snap (#22) | Explicitly requested; regression-prone | LOW, but must be verified | Real risk is the new subclass swallowing focus/keyboard messages, not an OS gap |

### Differentiators (Not required by the bullet list, cheap because they're free — worth calling out, not worth spending phase budget on)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Aero Shake (#14) | Thematically on-brand for an "Aero" library; a recognizable Windows 7-era gesture | LOW (free once #1's `HTCAPTION` is right) | No code path to build — falls out of correct hit-testing. Worth one line in the requirements doc so nobody "fixes" it away by swallowing the drag gesture |
| Win+Shift+Arrow move-to-monitor (#15) | Power-user multi-monitor workflow | LOW (free) | Built into Windows for any top-level window; nothing to build or test beyond not breaking it |
| Win+D / Show desktop (#16) | Baseline but not explicitly requested | LOW (free, already works) | Same mechanism as taskbar minimize |
| "Show this window on all desktops" (#17) | Virtual-desktop power users | LOW (free, shell-driven) | No library API involved at all; verify only that the JNA subclass doesn't somehow break virtual-desktop assignment |

### Anti-Features (Do NOT build — commonly the "obvious" next step, but wrong here)

| Feature | Why It Looks Appealing | Why Problematic | Alternative |
|---------|------------------------|------------------|-------------|
| Custom snapping logic in Compose (re-implementing edge-proximity detection, half/quarter math, in Kotlin) | The current code already computes drag deltas in Kotlin for resize handles — tempting to extend the same pattern to snap | Reinvents behavior the OS already provides better (correct multi-monitor math, DPI awareness, Snap Assist integration, Snap Groups); will never match native feel and breaks the instant Microsoft changes zone geometry (3-window layouts, portrait stacking) | Delegate to the OS: correct `HTCAPTION`/`HTMAXBUTTON`/resize-code hit-testing is the entire job |
| Custom "ghost rectangle" snap-preview overlays (drawing our own translucent preview as the user drags near an edge) | Mimics what users see Windows do natively; feels like "supporting" snap | The OS already draws this once real `HTCAPTION` dragging + the system move loop are used — a custom overlay would either double up visually or actively fight the native one, and would need re-tuning on every Windows UI refresh | Nothing to build — native preview comes for free with #1 |
| Reimplementing the Snap Layouts flyout as a custom Compose popup instead of returning `HTMAXBUTTON` | Full visual control; avoids depending on undocumented DWM UI | Guaranteed to look wrong relative to the real Windows 11 flyout, won't integrate with Snap Assist/Snap Groups, and breaks on every Windows UI refresh; Microsoft's own guidance is explicitly "return `HTMAXBUTTON`," not "draw your own" | `WM_NCHITTEST` → `HTMAXBUTTON` over the button rect, full stop |
| AppBar-style edge docking (reserving screen real estate like the old taskbar `SHAppBarMessage` API) | Superficially related — "window management," "screen edges" | Explicitly out of scope; conflicts with normal top-level window semantics, taskbar auto-hide interaction, and multi-monitor behavior; nobody asked for it | N/A — not requested, don't build |
| Building a custom "pin to all virtual desktops" menu/UI inside the app | Feels like completing the feature set | The shell already exposes this for any taskbar-visible window; duplicating it adds a UI surface with no OS-level hook to actually call (`IVirtualDesktopManager` from inside your own process, on your own window, adds nothing the shell menu doesn't already do) | Do nothing — verify only that it isn't accidentally broken |
| Manually re-computing window size/position on `WM_DPICHANGED` instead of honoring the suggested `RECT*` in `lParam` and JDK's own Per-Monitor-V2 handling | Feels more "in control" | Error-prone versus Microsoft's own suggested-rect algorithm; the JDK already does this correctly by default — the only real risk is the *new* subclass breaking that existing correct behavior | Forward `WM_DPICHANGED` to `DefWindowProc`/let the peer handle it; don't intercept its geometry math |

## Feature Dependencies

```
[WS_CAPTION + WS_SYSMENU + WS_MINIMIZEBOX + WS_MAXIMIZEBOX + WS_THICKFRAME restored via SetWindowLongPtr]
    └──requires──> [JNA WndProc subclass installed on the real HWND]
                       ├──requires──> [WM_NCCALCSIZE handler: zero the non-client frame]
                       │                   └──enables──> [Window shadow + Win11 rounded corners (#19)]
                       │                   └──enables──> [Alt+Space system menu (#5), requires WS_SYSMENU specifically]
                       ├──requires──> [WM_NCHITTEST handler: HTCAPTION / HTMAXBUTTON / resize codes / HTCLIENT]
                       │                   └──enables──> [Edge/corner snap (#1), Win+Arrow (#3), double-click maximize (#4),
                       │                                   Aero Shake (#14), restore-by-drag (#20)]
                       │                   └──enables──> [Snap Layouts flyout (#2)]
                       │                                     └──enables──> [Snap Assist (#21)]
                       │                                     └──enables (Win11 only, automatic)──> [Snap Groups (#7), shared-border resize (#6)]
                       ├──requires──> [WM_NCLBUTTONDOWN/UP forwarding for HTMAXBUTTON: click-through to maximize/restore]
                       └──requires──> [WM_GETMINMAXINFO handler: MONITORINFO.rcWork per monitor, incl. auto-hide taskbar]
                                           └──enables──> [Maximize fills work area (#8)]

[Per-window subclass instance + lifecycle-managed uninstall on window dispose]
    └──requires──> [Public API: interactive-region registration for title-bar content]
                       └──enables──> [Pinya main + narrow queue window with counter/"return queue" button (#10)]

[Existing AeroResizeHandles 8-zone Compose drag handlers]
    └──conflicts with──> [Native HTTOP/HTBOTTOM/HTLEFT/HTRIGHT/HT*CORNER* resize codes]
                             (once WM_NCHITTEST answers resize codes natively, the Compose-side pointerInput
                              resize handlers become redundant/competing input paths — must be retired or
                              gated off, not run in parallel)
```

### Dependency Notes

- **Everything requires the restored window styles first.** `WM_NCCALCSIZE`/`WM_NCHITTEST` handling is inert on a bare `WS_POPUP` window — there's no frame to hide and no system menu to invoke. This is the first phase-ordering constraint: styles-and-subclass-plumbing must land before any individual behavior can be verified.
- **`AeroResizeHandles` conflicts with native resize hit-testing.** Once `WM_NCHITTEST` correctly returns `HTTOP`/`HTLEFT`/etc. on the border, the existing Compose `pointerInput`-based resize handles are handling the exact same physical pixels via a completely different, non-OS-native path (`detectDragGestures` mutating `WindowState.size` directly). Running both simultaneously risks double-application of drag deltas or fighting cursors. This is a **decommission-or-gate**, not an additive, dependency — a real design decision for the roadmap, not just a research footnote.
- **Snap Layouts (#2) enables Snap Assist (#21) and, on Windows 11, is upstream of Snap Groups (#7) and shared-border resize (#6)** — but #6/#7 also happen for windows snapped via plain edge-drag (#1) or Win+Arrow (#3), not only via the flyout. So #2 is not strictly required for #6/#7, only for the *flyout-driven* snap path; #1/#3 alone are sufficient upstream dependencies.
- **Multi-window support (#10) is an architectural dependency of the public API**, not a downstream consequence of any single OS behavior — every other behavior in this table is "per-window" already at the OS level (each HWND has its own hit-test/message stream), so #10 is really "don't build the subclass as a singleton."

## Public API Additions Needed

Constraint: `AeroTitleBar`'s existing signature (`title, windowState, onCloseRequest, leading, modifier`) must stay source-compatible. New capability is additive.

1. **Opt-in-by-default native chrome, with an escape hatch.** Add a trailing parameter, e.g. `nativeWindowManagement: Boolean = true`, to `AeroTitleBar` (and a matching one for consumers driving `AeroResizeHandles` directly). Default `true` gives every existing consumer the new behavior without a source change; setting it `false` restores today's Compose-only dragging/resizing — needed as an escape hatch for edge cases (e.g., this project's own `tools/capture/` `PrintWindow` tooling, which launches non-focusable capture windows and must not be destabilized by a native subclass touching focus/activation). Must no-op silently on non-Windows (guard with a runtime `os.name` check before touching any JNA/`User32` class — JNA will throw `UnsatisfiedLinkError` if Win32 natives are referenced off-Windows).

2. **A way to mark extra interactive regions inside the title bar as client area.** Pinya's narrow queue window needs a counter + "return queue" button living inside the title bar row, and those need `HTCLIENT` (real Compose clicks), not `HTCAPTION` (drag). Two additive options, not mutually exclusive:
   - Automatically treat the existing `leading` slot's measured bounds as `HTCLIENT` (track via an internal `Modifier.onGloballyPositioned` on the `leading()` call site) — covers the common case with zero new API surface.
   - Add a new trailing slot, e.g. `actions: (@Composable RowScope.() -> Unit)? = null`, for content after the title but before the min/max/close buttons, with the same automatic bounds-tracking — covers Pinya's counter + "return queue" button without repurposing `leading`.
   Both slots' bounds need to be fed, per-window, into the hit-test callback the JNA layer consults on every `WM_NCHITTEST`.

3. **A lower-level primitive for fully custom title bars that don't use `AeroTitleBar`.** Something like `FrameWindowScope.installAeroWindowChrome(windowState: WindowState, maximizeButtonBounds: () -> Rect?, clientRegions: () -> List<Rect> = { emptyList() })`, callable directly. `AeroTitleBar` becomes a thin, source-compatible wrapper over this primitive rather than owning the JNA logic itself — this is also what makes per-window lifecycle management (#10) testable in isolation from `AeroTitleBar`'s visuals.

4. **Disposal is not optional — it's a correctness requirement.** Whatever installs the subclass (`SetWindowLongPtr`/`SetWindowSubclass`) must uninstall it in a `DisposableEffect`'s `onDispose` tied to the window's own lifecycle, calling back into `DefWindowProc`/`RemoveWindowSubclass` before the HWND is destroyed. A native callback surviving past window disposal is a use-after-free crash class — this project has already shipped a Windows-11-specific native crash from window-chrome misconfiguration once (`transparent = true` → `EXCEPTION_ACCESS_VIOLATION`, documented in the existing `AeroTitleBar` KDoc), so this is a proven risk category here specifically, not a generic warning.

## Verification Methods (per behavior class)

| Verification class | Applies to | How |
|---|---|---|
| **(a) Provable without moving the real cursor/keyboard** | #1 (edge classification), #2 (`HTMAXBUTTON` returned), #4 (`WM_NCLBUTTONDBLCLK` reaches `DefWindowProc`), #5 (`WS_SYSMENU` present), #8, #19 (style bits, corner preference) | `SendMessage(hwnd, WM_NCHITTEST, 0, MAKELPARAM(x,y))` at specific screen points and assert the returned HT* code; `SendMessage(hwnd, WM_SYSCOMMAND, SC_MAXIMIZE, 0)` then `GetWindowRect` vs `GetMonitorInfo(...).rcWork`; `GetWindowLongPtr(hwnd, GWL_STYLE)`/`GWL_EXSTYLE` bitmask checks; `DwmGetWindowAttribute(DWMWA_WINDOW_CORNER_PREFERENCE)` readback. All doable headless, from a JNA test harness, matching this project's existing "no synthetic cursor/keyboard input" discipline (`tools/capture/`, Compose Hot Reload MCP self-review pattern) |
| **(b) Needs real mouse/keyboard input** | #1's actual snap-on-release visual, #2's flyout appearance/hover timing, #3/#15 (Win+Arrow, Win+Shift+Arrow), #6/#7 (requires two real snapped windows), #14 (Aero Shake gesture), #21 (Snap Assist UI) | Requires a human (the maintainer) or a real `SendInput`/`java.awt.Robot`-driven system move loop — this project's established policy is that synthetic system input is off-limits for agent self-checks (per "Never delivery synthetic cursor/keyboard" precedent from `v3.1`); these must go to the maintainer's own hands-on pass, same as prior visual sign-offs |
| **(c) Needs hardware/software this machine lacks** | #9 at real second-monitor-different-DPI (only one monitor, 100%, present), #11 FancyZones (PowerToys not installed), #17 across a genuine second Windows 10 machine (only Windows 11 24H2 present) | Must be explicitly named as an "unconfirmed" gap in the eventual sign-off, following this project's own established pattern (`21-UNCONFIRMED.md` from v3.1) rather than silently skipped or claimed as tested |

## MVP Definition

### Launch With (v3.2.0)

Everything the maintainer's bullet list names explicitly — items #1–#11, #19, #22 from the reference table, plus the styles/subclass/dispose plumbing they all depend on:

- [ ] Restore `WS_CAPTION`/`WS_SYSMENU`/`WS_MINIMIZEBOX`/`WS_MAXIMIZEBOX`/`WS_THICKFRAME` on the undecorated HWND — foundation for everything else
- [ ] JNA subclass with `WM_NCCALCSIZE` (zero the frame), `WM_NCHITTEST` (caption/max-button/resize codes/client), `WM_NCLBUTTONDOWN`/`UP` for `HTMAXBUTTON` click-through, `WM_GETMINMAXINFO` (taskbar-aware maximize), installed/uninstalled per window
- [ ] `AeroTitleBar` interactive-region plumbing (`leading` + new `actions` slot bounds fed to hit-test) — required for Pinya's counter/"return queue" button
- [ ] Decommission or gate off `AeroResizeHandles`' Compose-side drag handlers where native resize codes now own the same pixels
- [ ] `nativeWindowManagement` opt-out param, defaulting to on, source-compatible

### Add After Validation

- [ ] Explicit `DwmExtendFrameIntoClientArea` shadow-margin tuning, only if the default `WS_CAPTION` shadow turns out clipped by the `WM_NCCALCSIZE` zeroing (verify first — may not be needed)
- [ ] Explicit `DWMWA_WINDOW_CORNER_PREFERENCE` override API, only if a consumer needs to force square corners (default is already correct/automatic)

### Future / Not This Milestone

- [ ] Anything from the anti-features table — explicitly do not build
- [ ] Real second-monitor-at-150% and PowerToys-FancyZones verification — blocked on hardware/software this machine lacks; document as unconfirmed, don't fake a pass

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Styles + subclass foundation (`WM_NCCALCSIZE`/`WM_NCHITTEST`) | HIGH | HIGH | P1 |
| Snap Layouts flyout (`HTMAXBUTTON`, live button-rect tracking) | HIGH | MEDIUM | P1 |
| Taskbar-aware maximize (`WM_GETMINMAXINFO`) | HIGH | MEDIUM | P1 |
| Interactive title-bar regions (Pinya) | HIGH | HIGH | P1 |
| Multi-window lifecycle (install/dispose per HWND) | HIGH | MEDIUM | P1 |
| Snap Groups / shared-border resize / Snap Assist / Aero Shake / Win+Shift+Arrow / Win+D / show-on-all-desktops / Alt-Tab thumbnails | MEDIUM–HIGH | LOW (all free consequences) | P1 (ship because they fall out for free) but P3 effort (nothing to build, only verify) |
| Multi-monitor DPI regression guard | MEDIUM | LOW (don't break what JDK already does) | P2 |
| Shadow margin tuning | LOW | LOW | P3 |
| Corner-preference override API | LOW | LOW | P3 |

## Sources

- [Support snap layouts for desktop apps on Windows 11 — Microsoft Learn](https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-snap-layout-menu) (updated 2026-06-16) — HIGH
- [WM_NCHITTEST message — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest) (2025-07-14) — HIGH
- [WM_NCCALCSIZE message — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-nccalcsize) (2025-07-14) — HIGH
- [WM_SYSCOMMAND message — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/menurc/wm-syscommand) (2025-12-02) — HIGH
- [WM_GETMINMAXINFO message — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-getminmaxinfo) (2025-07-14) — HIGH
- [DwmExtendFrameIntoClientArea function — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmextendframeintoclientarea) — HIGH
- [Apply rounded corners in desktop apps for Windows 11 — Microsoft Learn](https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-rounded-corners) — HIGH
- [DWM_WINDOW_CORNER_PREFERENCE enumeration — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwm_window_corner_preference) — HIGH
- [Windows Terminal — NonClientIslandWindow.cpp (GitHub)](https://github.com/microsoft/terminal/blob/main/src/cascadia/WindowsTerminal/NonClientIslandWindow.cpp) — reference implementation, HTMAXBUTTON child-window pattern — MEDIUM (secondary source, cross-checked against MS Learn guidance it implements)
- [FlatLaf — Window Decorations](https://www.formdev.com/flatlaf/window-decorations/) and [FlatLaf issue #407 — Windows 11 Snap Layouts don't work](https://github.com/JFormDesigner/FlatLaf/issues/407) — independent Java/Swing prior art solving the identical problem (JVM app, custom title bar, `HTMAXBUTTON`) — MEDIUM
- [JetBrains YouTrack CMP-6039 — How to trigger Aero Snap with undecorated window on Windows](https://youtrack.jetbrains.com/issue/CMP-6039) and [CMP-6031 — Support for undecorated windows with custom title pane decoration](https://youtrack.jetbrains.com/issue/CMP-6031) — confirms **no built-in Compose Desktop solution exists**; pages did not render full comment threads via fetch — LOW/unconfirmed content, but the absence of an official answer itself is the useful finding
- [JetBrains/compose-multiplatform issue #1248 — How to trigger Aero Snap with undecorated window](https://github.com/JetBrains/compose-jb/issues/1248) and [#2062 — Is it possible to activate Aero Snap when using the undecorated option?](https://github.com/JetBrains/compose-multiplatform/issues/2062) — both open/unanswered as fetched — confirms this is unsolved upstream — MEDIUM
- Community sources (ElevenForum, GadgetHacks, How-To Geek, Microsoft Support "Snap your windows") for Snap Groups, shared-border resize setting, and Win+Shift+Arrow behavior — LOW/MEDIUM, flagged individually above where used
- Existing project source read directly: `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt`, `ResizeHandles.kt`, `showcase/src/main/kotlin/com/mordred/showcase/Main.kt`, `.planning/PROJECT.md`

---
*Feature research for: native Windows window management on undecorated Compose Desktop windows (aero-compose-ui v3.2.0)*
*Researched: 2026-09-25*
