# Architecture Research: Native Windows Chrome for AeroTitleBar / AeroResizeHandles

**Domain:** Native Win32 window-management integration (Aero Snap, Snap Layouts, hit testing) for an undecorated Compose Desktop window, via JNA — no JBR dependency.
**Researched:** 2026-09-25
**Confidence:** MEDIUM-HIGH overall (Win32 message contracts and prior-art code are HIGH confidence, well-documented; several Compose/Skiko/AWT interaction points are MEDIUM and explicitly flagged for the Plan 1 spike to confirm empirically before the rest of the phase is built)

## Standard Architecture

### System Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Compose UI thread (EDT — Skiko desktop dispatches through Swing's       │
│  Event Dispatch Thread)                                                  │
│                                                                            │
│  AeroTitleBar (existing, modified)        AeroResizeHandles (modified)   │
│  ├─ Row { leading?, title, 3×TitleBarButton }                            │
│  ├─ onGloballyPositioned per region  ──┐   on Windows w/ native chrome   │
│  ├─ markAeroTitleBarInteractive() ────┤   active: early-return no-op    │
│  │   (new modifier, leading/custom     │                                 │
│  │    content inside title bar)        │                                 │
│  └─ TitleBarButton(maximize) reads ◄───┼── AeroMaxButtonInteraction      │
│     hover/pressed State fed from       │   (new: State<Boolean> pair,   │
│     native non-client messages         │    written only from EDT hop)  │
│                                         ▼                                 │
│                              HitTestRegionRegistry (new, internal)       │
│                              AtomicReference<HitTestSnapshot>            │
│                              (immutable: caption/client/button rects,    │
│                               in client-area px, no DPI math needed)     │
└───────────────────────────────────┬──────────────────────────────────────┘
                                     │ lock-free snapshot read
                                     │ (no blocking, no shared mutable state)
┌────────────────────────────────────▼─────────────────────────────────────┐
│  AWT-Windows / Toolkit thread (native Win32 message pump —               │
│  DispatchMessage calls the window's WNDPROC synchronously here)          │
│                                                                            │
│  AeroWndProc (new, internal, one instance per HWND)                      │
│  ├─ WM_NCHITTEST     → ScreenToClient + HitTestRegionRegistry.read()     │
│  ├─ WM_NCCALCSIZE    → 0 (normal) / monitor work-area inset (maximized)  │
│  ├─ WM_NCMOUSEMOVE / WM_NCMOUSEHOVER / WM_NCMOUSELEAVE (HTMAXBUTTON)     │
│  │      → TrackMouseEvent(TME_NONCLIENT) + EDT-post hover state          │
│  ├─ WM_NCLBUTTONDOWN / WM_NCLBUTTONUP (HTMAXBUTTON)                      │
│  │      → EDT-post pressed state + windowState.placement toggle          │
│  │      → return 0 (swallow — no DefWindowProc classic-button paint)     │
│  └─ everything else → CallWindowProc(previousWndProc, ...)  (passthrough)│
│                                                                            │
│  NativeWindowChromeRegistry (new, internal object)                       │
│  keyed by HWND (Long) → { AeroWndProc instance, previousWndProc pointer, │
│                            strong Callback ref, install/uninstall state }│
└───────────────────────────────────┬──────────────────────────────────────┘
                                     │ Win32 API calls (User32, Dwmapi via JNA)
┌────────────────────────────────────▼─────────────────────────────────────┐
│  Windows: DWM / Shell (Snap Layouts flyout, Aero Snap, Snap Groups,      │
│  FancyZones — all driven by the OS once WM_NCHITTEST answers correctly)  │
└────────────────────────────────────────────────────────────────────────────┘
```

The critical structural fact this diagram encodes: **two threads, one lock-free bridge each direction.** Compose writes region rects and reads none of Win32's state; the WndProc reads region rects and writes nothing Compose-visible except through an explicit EDT hop. There is no shared mutable object touched from both sides without going through `AtomicReference` (read direction) or `SwingUtilities.invokeLater`-style EDT posting (write direction).

### Component Responsibilities

| Component | Responsibility | New / Modified |
|-----------|----------------|-----------------|
| `AeroTitleBar` | Renders chrome; on Windows, installs native chrome via a new internal `DisposableEffect`; reports region rects; exposes `nativeWindowManagement` opt-out and consumes `AeroMaxButtonInteraction` for the maximize button's visual state | **Modified** |
| `AeroResizeHandles` | On Windows with native chrome active: no-op (OS owns resize via HT* codes). Elsewhere: unchanged today's behavior | **Modified** |
| `NativeWindowChromeRegistry` | Owns one `AeroWndProc` + strong `Callback` reference per HWND; install/uninstall lifecycle; survives recomposition; supports N windows per app | **New, internal** |
| `AeroWndProc` | The actual `WinUser.WindowProc` implementation: WM_NCHITTEST / WM_NCCALCSIZE / non-client mouse messages for the maximize button; chains everything else to the previous (AWT) WndProc | **New, internal** |
| `HitTestRegionRegistry` | Thread-safe (`AtomicReference`) publisher/reader of an immutable `HitTestSnapshot` (caption / client-exclude / max-button rects in client-area px) | **New, internal** |
| `AeroMaxButtonInteraction` | Small holder of `MutableState<Boolean>` hover/pressed for the maximize button only, written exclusively via EDT hop from `AeroWndProc` | **New, internal (exposed read-only to `AeroTitleBar`)** |
| `markAeroTitleBarInteractive()` | Public `Modifier` extension marking a subtree (e.g. `leading`, or content inside a consumer-built title bar) as client area, excluded from HTCAPTION | **New, public, additive** |
| `rememberAeroWindowChrome()` | Public lower-level API for consumers building their own title bar from scratch, exposing the same caption/button/interactive-region wiring `AeroTitleBar` uses internally | **New, public, additive** |
| `WindowState` (Compose Multiplatform, not ours) | `placement`/`size`/`position` — kept in sync automatically by AWT's existing extendedState → `WindowStateListener` pipeline, **as long as** `AeroWndProc` chains unhandled messages (especially `WM_SIZE`, `WM_SYSCOMMAND`) to the previous WndProc | Unmodified, but load-bearing |

## Recommended Project Structure

```
library/src/main/kotlin/com/mordred/aero/
├── components/navigation/
│   ├── AeroTitleBar.kt              # existing — modified: region reporting, opt-out flag, native max-button state
│   └── ResizeHandles.kt             # existing — modified: Windows no-op guard
├── internal/windows/                # new package — Windows-only, `internal`, JNA-backed
│   ├── NativeWindowChromeRegistry.kt   # HWND → chrome instance map, strong Callback refs
│   ├── AeroWndProc.kt                  # WinUser.WindowProc implementation
│   ├── HitTestRegionRegistry.kt        # HitTestSnapshot + AtomicReference publisher/reader
│   ├── AeroMaxButtonInteraction.kt     # hover/pressed State bridge for the maximize button
│   ├── Win32Chrome.kt                  # window-style setup (WS_CAPTION|WS_THICKFRAME|...), WM_NCCALCSIZE math
│   ├── Win32Dpi.kt                     # GetSystemMetricsForDpi wrappers, monitor work-area lookup
│   └── Win32Interop.kt                 # any JNA interface/constant not already in jna-platform's User32/Dwmapi
└── components/navigation/AeroWindowChrome.kt   # new — public, additive lower-level API surface
```

### Structure Rationale

- **`internal/windows/` as its own package, not inside `components/navigation/`:** everything in it is platform-specific, JNA-backed, and must never leak into a public composable signature. Kotlin `internal` visibility (module-scoped, matches the rest of `:library`) plus a dedicated package makes "nothing here is public API" enforceable by convention and greppable by the same kind of gate the project already uses for Material3 alpha (`tools/verify/check-material3.sh`) — a follow-up gate could grep for `com.sun.jna` outside this package.
- **One file per Win32 responsibility, not one giant `WindowsChrome.kt`:** mirrors the project's existing style (e.g. `GlassModifiers.kt` vs the four call paths documented in v3.0's Aero Primitives Foundation) — each file maps to one of the 9 architecture questions in this milestone, which keeps future maintenance (e.g. a FancyZones-specific fix) scoped to one file.
- **The public additive surface lives beside `AeroTitleBar.kt`,** not inside `internal/windows/`, because it must compile and have sensible (no-op) behavior on non-Windows platforms too — it's a cross-platform-facing API that happens to be backed by a Windows-only implementation underneath, same pattern as `AeroResizeHandles` today (`public` because showcase is a separate Gradle module, per its own KDoc).

## Architectural Patterns

### Pattern 1: Per-HWND Win32 subclassing via `CallWindowProc` passthrough

**What:** `NativeWindowChromeRegistry.install(hwnd)` reads the current `GWLP_WNDPROC` via `GetWindowLongPtr`, stores it as `previousWndProc`, then installs `AeroWndProc` via `SetWindowLongPtr`. `AeroWndProc.callback(hwnd, msg, wParam, lParam)` handles only the messages this feature cares about and returns; for every other message it calls `User32.INSTANCE.CallWindowProc(previousWndProc, hwnd, msg, wParam, lParam)` and returns that result verbatim.
**When to use:** Always — this is the only safe way to add behavior to a window AWT already owns without breaking AWT's own state tracking (extendedState → `WindowState.placement`, focus, IME, drag-and-drop, accessibility). Full WNDPROC *replacement* (ignoring the old proc) is the anti-pattern below.
**Trade-offs:** Correct and low-risk, but every message this code does NOT explicitly recognize must still be forwarded — a bug here (accidentally swallowing e.g. `WM_SIZE` or `WM_ACTIVATE`) silently breaks `WindowState` sync, minimize animation, or focus, with no compile-time signal. This is why Plan 6 in the build order below is a dedicated "don't break what works" checkpoint.
**Example (shape, not literal JNA syntax):**
```kotlin
internal class AeroWndProc(
    private val hwnd: HWND,
    private val previousWndProc: Pointer,
    private val regions: HitTestRegionRegistry,
    private val onMaxButtonToggle: () -> Unit, // posts to windowState on EDT
) : WinUser.WindowProc {
    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
        return when (uMsg) {
            WinUser.WM_NCHITTEST -> handleNcHitTest(hwnd, lParam)
            WM_NCCALCSIZE -> handleNcCalcSize(hwnd, wParam, lParam)
            WM_NCMOUSEMOVE, WM_NCMOUSEHOVER, WM_NCMOUSELEAVE -> handleNcMouse(uMsg, wParam)
            WM_NCLBUTTONDOWN, WM_NCLBUTTONUP -> handleNcButton(uMsg, wParam)
            else -> User32Ext.INSTANCE.CallWindowProc(previousWndProc, hwnd, uMsg, wParam, lParam)
        }
    }
}
```

### Pattern 2: Immutable-snapshot bridge for cross-thread region publishing

**What:** Compose (EDT) never mutates a shared rect object; instead each recomposition that changes layout produces a brand-new immutable `HitTestSnapshot` (data class: `captionRect: Rect`, `excludedRects: List<Rect>` — minimize/close/interactive content — , `maxButtonRect: Rect?`, all in **client-area pixels**, i.e. the raw px `onGloballyPositioned`/`boundsInWindow()` already reports on desktop Compose, no DPI conversion needed on the Compose side) and swaps it into an `AtomicReference` in `HitTestRegionRegistry`. `AeroWndProc.handleNcHitTest` reads that reference once per `WM_NCHITTEST` call — a plain volatile read, no lock, no blocking of the Toolkit thread.
**When to use:** Any time native code (a different OS thread) needs a read-mostly view of Compose layout state. This is the same shape the Flutter/`nativeapi` prior art uses ("a small method channel carrying the button's rect on layout") — Compose's `onGloballyPositioned` is the channel here, `AtomicReference` is the delivery mechanism.
**Trade-offs:** Worst case is "one hit-test call sees last frame's rects" during an active resize/DPI change — cosmetically harmless (a 1-frame-late hit-test boundary), never a torn read, never a crash. Converting screen coordinates to client coordinates via `ScreenToClient(hwnd, pt)` *inside* the handler (rather than tracking window position separately in Kotlin) removes an entire class of staleness bugs — window position becomes irrelevant to the hit-test math.
**Example:**
```kotlin
// Compose side, inside AeroTitleBar's Row, per region:
Modifier.onGloballyPositioned { coords ->
    regionRegistry.publish(role = Caption, rect = coords.boundsInWindow())
}
```

### Pattern 3: Non-client-driven button state fed back through a one-way EDT hop

**What:** `AeroWndProc` never touches a Compose `MutableState` directly from the Toolkit thread. On `WM_NCMOUSEMOVE`/`WM_NCLBUTTONDOWN`/etc. for `HTMAXBUTTON`, it computes the new boolean and does `SwingUtilities.invokeLater { AeroMaxButtonInteraction.hovered.value = true }` (or an equivalent EDT-posting call) — mirroring exactly how a *normal* mouse event for the minimize/close buttons already arrives on the EDT today. `TitleBarButton` for the maximize button reads `AeroMaxButtonInteraction.hovered`/`.pressed` (via `State<Boolean>`, `by` delegate) instead of its own `interactionSource.collectIsHoveredAsState()`, so it renders with the exact same hover/press visuals the other two buttons use — no new visual code path.
**When to use:** Specifically for the maximize button, because it is the only one of the three buttons whose *hit-testing* must be owned by the OS (to get `HTMAXBUTTON` → Snap Layouts). Minimize and close stay `HTCLIENT` and keep their existing `hoverable`/`clickable` Compose-native handling unchanged.
**Trade-offs:** Two sources of truth to keep visually identical (Compose's own interaction-source path for min/close vs. the native-fed path for maximize) — mitigated by making `AeroMaxButtonInteraction` expose the same `State<Boolean>` shape `collectIsHoveredAsState()` already gives `TitleBarButton`, so the rendering code in `TitleBarButton` barely changes, only the *source* of the boolean differs per-button.

## Data Flow

### Region publish → hit test → button state (the two flows this feature adds)

```
Flow A — layout → hit test (Compose EDT → Toolkit thread, read-only)
[Recomposition / resize / DPI change]
    ↓ onGloballyPositioned (per caption/button/interactive-content region)
[AeroTitleBar / markAeroTitleBarInteractive()]
    ↓ HitTestRegionRegistry.publish(role, rect)  — builds new immutable HitTestSnapshot
[AtomicReference<HitTestSnapshot>]
    ↓ volatile read, no lock
[AeroWndProc.handleNcHitTest]  ← WM_NCHITTEST(screenX, screenY) from DWM/Shell
    ↓ ScreenToClient(hwnd, pt) → classify against snapshot → HT* code
[Windows: draws resize cursor / offers drag / opens Snap Layouts flyout on hover]

Flow B — non-client mouse → Compose button visuals (Toolkit thread → Compose EDT, write-only)
[WM_NCMOUSEMOVE / WM_NCMOUSEHOVER / WM_NCMOUSELEAVE / WM_NCLBUTTONDOWN / WM_NCLBUTTONUP at HTMAXBUTTON]
    ↓ AeroWndProc computes hovered/pressed booleans, and on LBUTTONUP the toggle intent
    ↓ SwingUtilities.invokeLater { ... }
[AeroMaxButtonInteraction.hovered / .pressed  (MutableState<Boolean>)]
    ↓ Compose recomposition (EDT, normal Compose scheduling)
[TitleBarButton(maximize) renders hover/press exactly like min/close]
    ↓ on LBUTTONUP only
[windowState.placement = Maximized ↔ Floating]  (existing WindowState field, unmodified)
```

### WindowState sync (Q6, not a new flow — an existing one this feature must not break)

Compose Multiplatform's `WindowState.placement` already mirrors the underlying AWT `Frame`'s extended state through a `WindowStateListener` Skiko installs when the `Window` composable creates its `ComposeWindow` (a `javax.swing.JFrame` subclass). That listener reacts to Java `WindowEvent.WINDOW_STATE_CHANGED`, which AWT's Windows peer fires from its own handling of `WM_SIZE`'s `SIZE_MAXIMIZED`/`SIZE_RESTORED` wParam. **This entire pipeline is untouched by this feature as long as `AeroWndProc` forwards `WM_SIZE` (and everything else it doesn't explicitly own) to the previous WndProc via `CallWindowProc`** (Pattern 1). No new listener, no new sync code is needed for `windowState.placement`/`size`/`position` — this is a "don't touch it" boundary, verified by Plan 6's checkpoint, not a "build it" task.

A Win11 half-snap does **not** set `SIZE_MAXIMIZED` (it's a plain move+resize the Shell performs after `WM_NCHITTEST`/drag-to-edge), so `windowState.placement` correctly stays `Floating` while snapped — `AeroTitleBar`'s existing icon logic (`placement == Maximized ? FrameCorners : Square`) is already correct for this case with **zero changes**; this was verified as a non-finding, not assumed.

## DPI, Multi-Window, and Multi-Monitor Considerations

Traditional "scaling considerations" (100 vs 10K vs 1M users) don't apply to a UI library; the equivalent axes here are DPI scale, monitor count, and window count per process — all explicitly in the milestone's scope.

| Concern | 100% DPI, 1 window | 150% DPI or mixed-DPI multi-monitor | N windows (main + narrow ~300px second) |
|---|---|---|---|
| Hit-test rects | Compose `boundsInWindow()` already in client px, no conversion | Same — Skiko renders the client-area surface at the OS's per-window DPI scale already, so Compose px == physical px for that window; no extra math needed **in the region registry**. Only the Win32-side constants (resize-border thickness) need `GetSystemMetricsForDpi(hwnd-aware)`, not the region rects themselves | Each window has its own `HitTestSnapshot` and its own `AeroWndProc`/HWND — no cross-window state |
| Resize border thickness | `SM_CXSIZEFRAME + SM_CXPADDEDBORDER` ≈ 8–12 px | Query `GetSystemMetricsForDpi` with the window's *current* DPI (re-read on `WM_DPICHANGED`), not a cached global value — a window can move between monitors of different scale | At 150% scale ≈ 18 px per edge; against a 300px-wide (physical ≈450px at 150%) second window, left+right resize bands (~36px) plus a reasonable interactive-content margin still leaves comfortable client width — verify empirically in Plan 9, do not assume |
| Maximize target | `GetMonitorInfo` work area of `MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST)` | Same call, now genuinely necessary — a window maximized on monitor 2 must not use monitor 1's work area | Independent per window, no shared state |
| Subclass lifecycle | Install once in `DisposableEffect(window)` | Re-check DPI-derived constants on `WM_DPICHANGED` inside `AeroWndProc`, no reinstall needed | `NativeWindowChromeRegistry` keyed by HWND (`Long` from `Pointer.nativeValue`) supports arbitrarily many concurrent installs; each holds its own strong `Callback` reference so none is GC'd independently of the others |

**Fullscreen toggle and peer recreation — explicitly flagged as unverified (LOW confidence):** if a consumer flips `WindowPlacement.Fullscreen` (not currently used by the showcase, not in this milestone's target list), Compose Desktop may route through `GraphicsDevice.setFullScreenWindow`, which can alter window styles or, in some AWT code paths, tear down and recreate the native peer. `NativeWindowChromeRegistry` should therefore key its install decision on `window.isDisplayable` **plus** a listener (`ComponentListener.componentShown` or a `HierarchyListener`) rather than a one-shot `DisposableEffect(window)` that assumes the peer is stable for the composable's lifetime — reinstalling on peer recreation, uninstalling cleanly beforehand to avoid a dangling `CallWindowProc` chain pointing at a destroyed HWND. This needs a live-window check; it is not exercised by this milestone's target features, so treat it as "must not crash if it happens," not "must be pixel-perfect."

## Anti-Patterns

### Anti-Pattern 1: Full WNDPROC replacement (ignoring the previous proc)

**What people do:** `SetWindowLongPtr(hwnd, GWLP_WNDPROC, myProc)` and handle only the messages the new feature needs, returning a default value (e.g. 0 or `DefWindowProc`) for everything else instead of chaining to the *actual* previous WndProc.
**Why it's wrong:** AWT's own WndProc (which was installed first) is what feeds `WindowState.placement`, focus, IME composition, and accessibility. Skipping it silently breaks all of that — exactly the class of regression this milestone must avoid ("existing tests stay green").
**Do this instead:** Store the previous proc pointer at install time and `CallWindowProc` it for every message this feature doesn't explicitly own (Pattern 1).

### Anti-Pattern 2: Hardcoded/cached hit-test rects computed from window position

**What people do:** Track `windowState.position` + button offsets in Kotlin, convert to screen coordinates manually, and compare against the incoming `WM_NCHITTEST` screen point.
**Why it's wrong:** Introduces a second, easy-to-desync source of truth for window position (AWT's own position tracking vs. a Kotlin-side copy), and gets stale specifically during the drag/resize/Snap operations this feature is trying to support — the worst possible time for it to be wrong.
**Do this instead:** Convert the incoming screen point to client coordinates with `ScreenToClient(hwnd, pt)` *inside* the handler and compare against client-space rects (Pattern 2). Window position never enters the calculation.

### Anti-Pattern 3: Returning `HTCAPTION` for the whole title bar row including the buttons

**What people do:** Report one big caption rect for the entire title bar and rely on Windows to "figure out" the buttons.
**Why it's wrong:** Windows does not know about the three Compose buttons at all — anywhere `HTCAPTION` is returned, `WM_LBUTTONDOWN` never reaches the client area, so Compose's own `clickable`/`hoverable` on minimize/close would silently stop working the moment native chrome is installed.
**Do this instead:** Explicitly exclude minimize and close button rects (report `HTCLIENT` there, unchanged Compose click handling) and report the maximize button rect as `HTMAXBUTTON` specifically (Pattern 3) — only the maximize button needs OS-owned hit testing, and only because that's the sole hit-test code that unlocks Snap Layouts.

### Anti-Pattern 4: Letting `DefWindowProc` handle `WM_NCLBUTTONDOWN`/`UP` at `HTMAXBUTTON`

**What people do:** Return the caption-button hit-test code but still forward the button-down/up messages to `DefWindowProc`.
**Why it's wrong:** `DefWindowProc` will paint and drive a classic Win32 caption button (wrong visuals, wrong theme, fights the Compose-rendered button underneath) and may double-fire the maximize toggle.
**Do this instead:** Swallow `WM_NCLBUTTONDOWN`/`WM_NCLBUTTONUP` for `HTMAXBUTTON` (return 0, do not forward), drive the toggle and pressed-state from the Kotlin handler exclusively (Pattern 3).

## Integration Points

### External Services (Win32 APIs)

| API surface | Integration Pattern | Notes |
|---|---|---|
| `User32.SetWindowLongPtr` / `GetWindowLongPtr` (`GWLP_WNDPROC`, `GWL_STYLE`) | Install/uninstall subclass; add `WS_CAPTION\|WS_SYSMENU\|WS_THICKFRAME\|WS_MINIMIZEBOX\|WS_MAXIMIZEBOX` at install time | **Correction to the maintainer's brief:** keep `WS_CAPTION` and `WS_SYSMENU` too, not just `WS_THICKFRAME`/`WS_MAXIMIZEBOX`/`WS_MINIMIZEBOX` — confirmed by prior art (melak47/BorderlessWindow): `WS_CAPTION` is what gives minimize animation and (with `WS_SYSMENU`) the Alt+Space system menu the milestone explicitly asks for, and both are part of the standard "borderless-but-behaves-native" recipe. `WM_NCCALCSIZE` returning 0 hides the *painting* of the caption without removing the style bits DWM/Shell key off of. (MEDIUM-HIGH confidence — verify no ghost native caption paint in Plan 2.) |
| `User32.CallWindowProc` | Passthrough for all unhandled messages | Load-bearing for `WindowState` sync (see above) — HIGH confidence, standard Win32 subclassing contract |
| `WM_NCHITTEST` / `WM_NCCALCSIZE` / `WM_NCMOUSEMOVE` / `WM_NCMOUSEHOVER` / `WM_NCMOUSELEAVE` / `WM_NCLBUTTONDOWN` / `WM_NCLBUTTONUP` | Core hit-testing and button-state contract | HIGH confidence — documented Microsoft Learn messages, confirmed against FlatLaf's real native implementation and the winit/Niman/Electron prior art for `HTMAXBUTTON` specifically |
| `TrackMouseEvent` with `TME_NONCLIENT\|TME_LEAVE` (and optionally `TME_HOVER`) | Arm `WM_NCMOUSELEAVE`/`WM_NCMOUSEHOVER` for the maximize button the same way client-area hover tracking normally works | HIGH confidence, standard pattern, must be re-armed after every `WM_NCMOUSELEAVE` (tracking is one-shot) |
| `Dwmapi.DwmExtendFrameIntoClientArea` with `MARGINS{1,1,1,1}` | Preserve the native drop shadow around a borderless window | **Optional** — cosmetic; confirmed pattern from melak47/BorderlessWindow. Skip in the first pass if it complicates the spike; add once the core hit-testing is proven. |
| `Dwmapi.DwmSetWindowAttribute(DWMWA_WINDOW_CORNER_PREFERENCE, DWMWCP_ROUND)` | Win11 rounded corners | **Optional, LOW-MEDIUM confidence on necessity** — sources disagree on whether a borderless-but-`WS_CAPTION`-having window already gets DWM's default rounding on Win11 or needs this explicit call; verify visually in the spike (Plan 1/3) rather than assume either way |
| `Dwmapi.DwmGetWindowAttribute(DWMWA_VISIBLE_FRAME_BORDER_THICKNESS)` | Win11-specific 1px top-border color line some borderless implementations add | Optional refinement (FlatLaf uses it) — not required for the milestone's target features, note only |
| `GetMonitorInfo` / `MonitorFromWindow(MONITOR_DEFAULTTONEAREST)` | Maximize target = monitor work area, per-monitor | Core to `WM_NCCALCSIZE` maximized handling — HIGH confidence, this is literally the "never covers the taskbar" requirement |
| `User32.GetSystemMetricsForDpi(hwndDpi, SM_CXSIZEFRAME/SM_CYSIZEFRAME, SM_CXPADDEDBORDER)` | Resize-border thickness at the window's *current* DPI, re-queried on `WM_DPICHANGED` | HIGH confidence, standard technique; do not hardcode the existing 4.dp Compose value once native hit-testing owns resize |
| `ScreenToClient` | Convert `WM_NCHITTEST`'s screen-space `lParam` into client coordinates comparable to Compose's `boundsInWindow()` rects | Removes the need to track window position at all (Pattern 2) |
| Native Snap Layouts flyout itself | **Not directly integrable/testable** — no public API to query or assert its on-screen appearance; the only integration point is *correctly answering* `WM_NCHITTEST` with `HTMAXBUTTON`, which Windows uses internally to decide whether to offer it | Confirmed pattern across three independent prior-art sources (winit issue, Niman issue, Electron/VS Code/Windows Terminal convention) — the flyout's actual appearance remains a human/maintainer visual-proof step, same honesty standard as `21-UNCONFIRMED.md` |
| JNA `Callback` GC lifetime | `NativeWindowChromeRegistry` must hold a strong reference to every installed `WindowProc` `Callback` instance for the lifetime of the window; letting it become unreachable while still installed causes a crash on the next message (dangling native function pointer) | HIGH confidence — documented, well-known JNA pitfall, directly relevant since this feature installs a long-lived native callback per window |

### Internal Boundaries

| Boundary | Communication | Notes |
|---|---|---|
| `AeroTitleBar` (Compose/EDT) ↔ `HitTestRegionRegistry` (shared) | `onGloballyPositioned` → `publish(role, rect)`, writer-only from Compose | No reads from Compose side; registry is purely a write sink here |
| `AeroWndProc` (Toolkit thread) ↔ `HitTestRegionRegistry` (shared) | `AtomicReference.get()`, reader-only from native side | No writes from native side |
| `AeroWndProc` (Toolkit thread) ↔ `AeroMaxButtonInteraction` (Compose state) | `SwingUtilities.invokeLater { state.value = ... }`, one-way, native → Compose | The only place native code touches Compose-visible mutable state, and only through an explicit thread hop |
| `AeroWndProc` (Toolkit thread) ↔ `WindowState` (Compose Multiplatform) | Two paths: (a) direct write `windowState.placement = ...` on `WM_NCLBUTTONUP` toggle, hopped to EDT same as above; (b) indirect, via `CallWindowProc` → AWT's own extendedState listener → Skiko's existing `WindowStateListener` (Pattern 1) — **do not build a second, redundant sync path** for size/position, only the explicit user-toggle needs an explicit write | The indirect path already exists and must not be duplicated or fought |
| `AeroResizeHandles` ↔ "is native chrome active" | A small internal read (e.g. `NativeWindowChromeRegistry.isInstalled(window)` or a `CompositionLocal`/remembered flag threaded down) gates the existing `if (...) return` early-exit, extending it beyond today's `placement != Floating` check | Keeps `AeroResizeHandles` a safe no-op fallback if native install fails for any reason (older Windows, JNA unavailable at runtime, non-Windows OS) — graceful degradation, not a hard dependency |
| `:library` ↔ consumer app (public API) | `AeroTitleBar(..., nativeWindowManagement: Boolean = true)`, `Modifier.markAeroTitleBarInteractive()`, `rememberAeroWindowChrome()` — all new, all additive, zero JNA/Win32 types in any public signature | Mirrors the project's existing discipline of keeping `kotlinx.coroutines` internal-only (`build.gradle.kts` comment: "Internal only — not exposed in any public signature") — apply the same rule to `jna`/`jna-platform` |

## Public API Shape (Q7)

All additions are backward-compatible default-parameter additions or brand-new top-level declarations — every existing call site (`AeroTitleBar(title = ..., windowState = ..., onCloseRequest = ...)` in the showcase and any consumer) compiles unchanged.

```kotlin
// Modified — one new trailing default parameter, source-compatible.
@Composable
public fun FrameWindowScope.AeroTitleBar(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    nativeWindowManagement: Boolean = true, // new — Windows-only effect; no-op elsewhere
): Unit

// New — public, additive. Mark interactive content inside a title bar (the `leading`
// slot, or custom content in a consumer-built title bar) as client area, so it keeps
// receiving normal pointer input instead of being swallowed by HTCAPTION.
public fun Modifier.markAeroTitleBarInteractive(): Modifier

// New — public, additive. Lower-level entry point for consumers who build their own
// title bar instead of using AeroTitleBar, giving them the same native wiring.
@Composable
public fun FrameWindowScope.rememberAeroWindowChrome(
    windowState: WindowState,
): AeroWindowChromeState

public interface AeroWindowChromeState {
    public fun Modifier.captionArea(): Modifier          // HTCAPTION — draggable
    public fun Modifier.captionExclude(): Modifier        // HTCLIENT — normal Compose input
    public fun Modifier.maximizeButtonArea(): Modifier    // HTMAXBUTTON — Snap Layouts trigger
    public val maximizeHovered: State<Boolean>
    public val maximizePressed: State<Boolean>
}
```

**Binary-compatibility caveat (MEDIUM confidence, flag not block):** adding a default-valued parameter to an existing `@Composable` public function is always source-compatible; whether it stays binary-compatible for a consumer's *already-compiled* class files depends on how the Compose compiler's `$default`/composer-param bridging behaves across this specific addition. JitPack distribution means consumers pull source-derived artifacts per version tag and typically recompile against the new version rather than link old bytecode against a new JAR, which lowers this risk in practice — still worth a real recompilation check against an external consumer (mirrors `VER-F03` in `PROJECT.md`'s open candidates) before calling the API contract fully proven.

`jna` + `jna-platform:5.19.1` should be added as `implementation`, not `api` — no `com.sun.jna.*` type appears in any signature above, matching the project's existing rule for `kotlinx-coroutines`.

## Testing Architecture (Q8)

### Headless, JVM-only — part of `:library:test`, counts toward the locked test-count guard

| What | How |
|---|---|
| Hit-test classification | Pure function `classifyHitTest(snapshot: HitTestSnapshot, clientPoint: IntOffset, edgePx: Int, cornerPx: Int): Int`, no JNA/AWT — table-driven tests across caption/button/client/edge/corner points |
| DPI-derived constants | Pure functions for the resize-border-thickness formula, tested at representative scale factors (100/125/150/200%) as plain arithmetic, no live `GetSystemMetricsForDpi` call needed to test the *formula* |
| Maximized-client-rect math | Pure function `computeMaximizedClientRect(monitorWorkArea: Rect): Rect` (or the frame-thickness-inset variant), tested against synthetic monitor geometries including a taskbar on each of the four edges and a secondary monitor at a different origin |
| Region-registry concurrency | Cheap JVM-thread stress test: one writer thread swaps `HitTestSnapshot`s while several reader threads read concurrently, asserting no reader ever observes a partially-constructed rect (trivially true for an immutable data class behind `AtomicReference`, but worth locking in as a regression guard against a future refactor to mutable fields) |

None of the above touches JNA, a real HWND, or a visible window — same category as today's `PanelGroupLogicTest`-style pure-JVM unit tests, and the natural place to grow the test count from 541.

### Needs a live window — new opt-in harness, NOT part of the locked-541 guard

| What | How | Where it runs |
|---|---|---|
| Subclass install/uninstall correctness | Launch a real (showcase or minimal scratch) window, assert `GetWindowLongPtr(hwnd, GWLP_WNDPROC)` changed after install and is restored to the previous proc after uninstall (no dangling pointer, no double-uninstall crash) | New harness |
| `WM_NCHITTEST` correctness against live Compose layout | `SendMessage(hwnd, WM_NCHITTEST, 0, MAKELPARAM(x, y))` at known screen points, compared against the current `HitTestSnapshot` state (dumped via a `-Paero.captureDir`-style system-property hook, mirroring `BASE-05`'s existing pattern) or against `get_semantic_tree` bounds from the Compose Hot Reload MCP for the same points | New harness |
| Maximize-never-covers-taskbar | `SendMessage(hwnd, WM_SYSCOMMAND, SC_MAXIMIZE, 0)`, then compare `GetWindowRect(hwnd)` against `GetMonitorInfo(...).rcWork` | New harness |
| Snap Layouts flyout appearance | **Not automatable** — the harness can only prove the precondition (`WM_NCHITTEST` at the maximize-button point returns `HTMAXBUTTON`); the flyout itself has no queryable state. One maintainer visual pass is the honest verification step here, not a script | Manual, flagged explicitly, not silently skipped |

**Where this harness lives:** it needs `SendMessage`/`GetWindowRect`/`GetMonitorInfo` calls of its own (via JNA, same as the feature code) against a real, visible HWND — this is architecturally the same category of thing as `tools/capture/`'s PowerShell `PrintWindow` toolkit (a maintainer-provided, non-CI, opt-in tool), not a JUnit test that `./gradlew :library:test` runs by default. Recommend either (a) a small Kotlin/JVM `main()` under `tools/` alongside `tools/capture/`, invoked manually, or (b) a separate Gradle source set/task (e.g. `:library:winIntegrationTest`) explicitly excluded from `check`. This is a decision the phase's first plan should make explicitly, not something to leave implicit — the existing `PROJECT.md` precedent (`tools/verify/check-material3.sh`, `tools/capture/`) favors (a): a small opt-in tool, not a new Gradle task category.

**Existing tooling reused, not reinvented:** `.planning/research/MCP-HOWTO.md`'s `PrintWindow`-based capture pipeline is the right tool for the *visual* proof (title bar renders correctly under native chrome, hover/press states look right, shadow/rounded corners present if implemented) — it works on any toolchain and doesn't move the real cursor. The Compose Hot Reload MCP (`hotMcpServer`) is useful for driving/observing the *Compose-side* hover/press animation once `AeroMaxButtonInteraction` feeds it, but its `click`/`get_semantic_tree` tools operate through Compose semantics only — they cannot themselves send `WM_NCHITTEST`/`WM_SYSCOMMAND`, so they complement but do not replace the live-window harness above.

## Suggested Build Order (Q9)

One phase, ordered plans, each a separate commit, riskiest-first per the maintainer's stated approach.

1. **Spike (existential risk, must pass before anything else is built):** on a throwaway `main()` or the real showcase window, running on a **standard JDK 21** (not JBR), install a minimal JNA `WindowProc` subclass that returns `HTCAPTION` everywhere except one hardcoded rect that returns `HTMAXBUTTON`. Manually hover that rect on Windows 11 and confirm the Snap Layouts flyout actually opens on an `undecorated=true, transparent=false` Compose window. **If it does not appear, stop and report the conflict** — per the milestone's explicit instruction — before investing in anything below.
2. **Window styles:** add `WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MINIMIZEBOX|WS_MAXIMIZEBOX` at install time; confirm via `GetWindowLongPtr` and confirm no ghost native caption paints over the Compose content, and that `Frame.getInsets()`/Compose layout are unaffected.
3. **Frame removal:** `WM_NCCALCSIZE` — return 0 for normal state; monitor-work-area inset for maximized. Verify "never covers the taskbar" across at least two monitors with different DPI scales.
4. **Region registry + real hit-testing:** `HitTestRegionRegistry`, `HitTestSnapshot`, `onGloballyPositioned` wiring in `AeroTitleBar` for caption/button/client rects; replace the Plan 1 hardcoded rect with the live one via `ScreenToClient`.
5. **Maximize-button interaction bridge:** `TrackMouseEvent(TME_NONCLIENT)`, non-client mouse messages → `AeroMaxButtonInteraction`, wired into `TitleBarButton` so it visually matches minimize/close.
6. **"Don't break what works" checkpoint:** audit every unhandled message path reaches `CallWindowProc`; re-run the existing 541-test suite plus a manual pass confirming `WindowState.placement`/`size`/`position`, focus, minimize animation, and Alt+Space system menu are unchanged from before this phase.
7. **`AeroResizeHandles` Windows no-op + Win32 edge/corner classification:** gate the composable's early-return on "native chrome active"; move edge/corner hit-test thickness to `GetSystemMetricsForDpi`; verify against the milestone's narrow ~300px second window.
8. **Public API surface:** `nativeWindowManagement` flag, `markAeroTitleBarInteractive()`, `rememberAeroWindowChrome()`; verify multi-window (main + narrow second window, both native-chrome-active simultaneously).
9. **DPI/multi-monitor pass:** 100%/150% scaling, drag across monitors with different scale factors, snapped-pair shared border, Snap Groups smoke check, FancyZones smoke check (should work for free once drag+edge detection is correct, since it hooks the same OS mechanisms — verify, don't assume).
10. **Tests + proof + release plumbing:** headless unit tests (region registry, hit-test math, DPI math), live-window harness (Plan-1-style script promoted to a committed tool), showcase wiring, bump the locked test-count guard in `library/build.gradle.kts` with a commit explaining why, capture-based before/after visual proof via `tools/capture/`.

## Sources

- [WM_NCCALCSIZE message — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-nccalcsize) — HIGH confidence, official docs
- [NCCALCSIZE_PARAMS structure — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-nccalcsize_params) — HIGH confidence
- [TrackMouseEvent function — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-trackmouseevent) — HIGH confidence, `TME_NONCLIENT` behavior and `WM_NCMOUSEHOVER`/`WM_NCMOUSELEAVE` confirmed
- [SetWindowLongPtr — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowlongptra) — HIGH confidence
- [DWM_WINDOW_CORNER_PREFERENCE — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwm_window_corner_preference) — HIGH confidence on the API shape; MEDIUM on whether it's actually required for this window style combination (flagged for the spike)
- [FlatLaf FlatWndProc.cpp — JFormDesigner/FlatLaf](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-natives/flatlaf-natives-windows/src/main/cpp/FlatWndProc.cpp) — HIGH confidence, real shipped native window-subclassing implementation for a JVM desktop toolkit (Swing, closely analogous to AWT/Skiko); confirmed `WM_NCCALCSIZE` maximized handling, `WM_NCHITTEST` delegation shape, non-client mouse forwarding for custom min/max/close buttons, and the JNI-boundary callback pattern this design's `HitTestRegionRegistry` mirrors
- [melak47/BorderlessWindow BorderlessWindow.cpp](https://github.com/melak47/BorderlessWindow/blob/main/src/BorderlessWindow.cpp) — HIGH confidence, canonical minimal C++ reference implementation; source of the `WS_CAPTION`+`WS_SYSMENU` correction to the maintainer's brief, the `WM_NCCALCSIZE` maximized-inset pattern, and `DwmExtendFrameIntoClientArea` shadow preservation
- [Windows title bar: Snap Layouts never open (WM_NCHITTEST never answers HTMAXBUTTON) — Nihmar/Niman#169](https://github.com/Nihmar/Niman/issues/169) — MEDIUM-HIGH confidence, independent confirmation of the `HTMAXBUTTON` requirement and its root-cause shape from a different toolkit's real bug fix
- [Allow returning HITMAXBUTTON from WM_NCHITTEST — rust-windowing/winit#3884](https://github.com/rust-windowing/winit/issues/3884) — MEDIUM confidence (issue is a feature request, not a shipped fix), corroborates the requirement from a third independent ecosystem
- [Support snap layouts for desktop apps on Windows 11 — Microsoft Learn](https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-snap-layout-menu) — HIGH confidence, official guidance confirming `HTMAXBUTTON` is the documented mechanism
- [JNA CallbacksAndClosures.md](https://github.com/java-native-access/jna/blob/master/www/CallbacksAndClosures.md) — HIGH confidence, official JNA documentation of the strong-reference/GC pitfall this design's `NativeWindowChromeRegistry` is built to avoid
- [Weird behavior of undecorated window in its maximized state — JetBrains/compose-multiplatform#3625](https://github.com/JetBrains/compose-multiplatform/issues/3625) — LOW-MEDIUM confidence (discussion was mostly about a Linux/older-version resize-flicker bug, not directly the Windows taskbar-overhang mechanism); cited only to establish that Compose Multiplatform's undecorated-window maximize behavior is a known rough edge upstream, motivating why this feature must own `WM_NCCALCSIZE` itself rather than rely on Compose/Skiko defaults
- [Top-level windows management — Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html) — MEDIUM confidence (general docs, not Windows-specific), used for the `WindowState` shape and general AWT `Frame`/`ComposeWindow` relationship
- `ComposeWindow.desktop.kt` — JetBrains/compose-multiplatform-core (confirmed via search result summary, not directly fetched) — MEDIUM confidence: `ComposeWindow` extends `javax.swing.JFrame`; window creation happens on the AWT Event Dispatch Thread
- AWT `EventDispatchThread`/native "AWT-Windows" toolkit thread behavior (general AWT architecture, confirmed via Microsoft/Oracle-adjacent and OpenJDK-source-derived search summaries) — MEDIUM confidence, standard and long-stable JDK internals, but not verified against this exact JDK 21 build's source — worth a quick `jstack`/thread-name sanity check during Plan 1's spike rather than taken purely on faith
- This repository: `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt`, `ResizeHandles.kt`, `library/src/test/kotlin/.../AeroTitleBarTest.kt`, `showcase/src/main/kotlin/com/mordred/showcase/Main.kt`, `library/build.gradle.kts`, `.planning/research/MCP-HOWTO.md`, `.planning/PROJECT.md` — HIGH confidence, primary sources, read directly

---
*Architecture research for: aero-compose-ui v3.2.0 — native Windows window management integration*
*Researched: 2026-09-25*
