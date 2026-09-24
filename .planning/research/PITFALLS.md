# Pitfalls Research — Native Windows Window Management for Undecorated Compose Desktop Windows

**Domain:** JNA-based `WM_NCHITTEST`/`WM_NCCALCSIZE` window-procedure subclassing to give an undecorated AWT/Compose Desktop window (JDK 21, standard JDK — not JBR) native Aero Snap / Windows 11 Snap Layouts / taskbar-aware maximize behaviour, layered on `aero-compose-ui`'s existing `AeroTitleBar` + `AeroResizeHandles`.
**Researched:** 2026-09-25
**Confidence:** MEDIUM-HIGH — Win32 mechanics are HIGH confidence (Microsoft Learn + Windows Terminal's real production source). JNA-specific wiring is MEDIUM (JNA docs/issues + analogous JVM prior art: FlatLaf, a JavaFX/JNA precedent). Compose/CMP-specific interaction points are MEDIUM (JetBrains issue trackers, no first-party "how to add Aero Snap" doc exists — CMP's own open issues confirm this is unsolved upstream).

This is a **single implementation phase** (per milestone scope). "Phase to address" below therefore names an internal **step** within that one phase, not a separate roadmap phase — for the phase planner to turn into ordered plans/tasks.

---

## Critical Pitfalls

### Pitfall 1: JNA callback object garbage-collected → native crash mid-session

**What goes wrong:**
The `WinUser.WindowProc`-style callback object passed to `SetWindowLongPtr(hwnd, GWLP_WNDPROC, callback)` is a normal Java object. If nothing on the Java side holds a strong reference to it, the GC is free to collect it — usually not immediately, but after enough allocation pressure (e.g. during a showcase demo session, a drag-heavy resize test, or a long-running consumer app). The native side still has a raw pointer into JNA's generated trampoline for that callback; the next Windows message dispatched to that HWND (basically guaranteed, since WndProc gets constant traffic) calls freed/invalid memory → `EXCEPTION_ACCESS_VIOLATION`, killing the whole JVM (not a catchable Java exception — this is a native SIGSEGV-equivalent).

**Why it happens:**
JNA's own docs are explicit: "you must keep a reference to the callback object until you deregister the callback; if the callback object is garbage-collected, the native callback invocation will probably crash." It's easy to build the callback as a local/lambda inside a `remember { }` block or a `LaunchedEffect`, which does NOT pin it against GC the way a class-level field does — Compose's `remember` cache can be dropped on recomposition/disposal in ways that don't map cleanly to "this native subclass is still installed."

**How to avoid:**
Store the `WindowProc` callback instance in a structure whose lifetime is explicitly tied to the HWND's lifetime — e.g. a `WeakHashMap`-free, `HWND`-keyed map of "installed subclass" objects owned by a singleton manager object (not a `remember` block), with the callback as a `val` field on that per-window object. Never let the only reference live in a lambda passed straight to `SetWindowLongPtr`. Keep this object alive for the full life of the AWT peer, and only release the reference after `WM_NCDESTROY` (or after explicitly restoring the original WndProc) has been processed.

**Warning signs:** Crash is intermittent, does not reproduce on demand, worse under GC pressure (stress-test: force `System.gc()` repeatedly while dragging/resizing). No Java stack trace — only a `hs_err_pid*.log` JVM crash dump (the project already has several such logs in its root from unrelated causes — this pitfall would look identical: a native fault, no Kotlin stack).

**Phase step:** Step 1 (JNA subclass scaffolding) — the callback-lifetime container is the first thing built, before any hit-test logic, and gets its own crash-repro test (force GC under active drag) before anything else is layered on.

**Sources:** JNA `CallbacksAndClosures.md` (HIGH, official JNA docs) — https://github.com/java-native-access/jna/blob/master/www/CallbacksAndClosures.md ; JNA narkive thread "Callback crashes on garbage collection" (MEDIUM, community confirmation) — https://users.jna.dev.java.narkive.com/iUN9QeCH/callback-crashes-on-garbage-collection

---

### Pitfall 2: `SetWindowLong` instead of `SetWindowLongPtr` truncates the WndProc pointer on 64-bit

**What goes wrong:**
On 64-bit Windows (the only target here — JDK 21 desktop is x64), `SetWindowLong`/`GetWindowLong` with `GWL_WNDPROC` operate on a 32-bit `LONG`. A 64-bit function pointer (the original AWT WndProc, or JNA's callback trampoline) silently truncates to its low 32 bits when stored/retrieved this way. The result isn't a clean crash — it's a corrupted pointer that may work by coincidence in testing (low address bits happen to be stable) and crash unpredictably in the field, or worse, jump execution into unrelated code.

**Why it happens:** JNA's `User32` interface exposes both `SetWindowLong`/`GetWindowLong` (legacy, ANSI-oriented naming carried over from Win32 headers) and `SetWindowLongPtr`/`GetWindowLongPtr` (the 64-bit-safe pair). It's easy to reach for the shorter/more commonly-documented name from older tutorials (most Win32 "how to subclass a window" blog posts predate widespread x64 desktop and still show `SetWindowLong`).

**How to avoid:** Use `SetWindowLongPtr`/`GetWindowLongPtr` exclusively for `GWLP_WNDPROC` (note the pointer variant also uses the `GWLP_*` constant names, not `GWL_*`). Store the original WndProc pointer as `LONG_PTR`/`Pointer`, never as `int`/`LONG`.

**Warning signs:** Works in local dev, crashes for a consumer on a different Windows build or after ASLR places the original WndProc above the 4 GB boundary — a genuinely hard bug to reproduce on demand, which makes it worth eliminating by construction rather than by testing.

**Phase step:** Step 1 (JNA subclass scaffolding) — code-review gate, not a runtime test: grep for `SetWindowLong(` / `GetWindowLong(` (without `Ptr`) in the new source before merging.

**Sources:** Microsoft Learn, `SetWindowLongPtrA` (HIGH, official) — https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowlongptra ; Microsoft Learn `CallWindowProcA` (HIGH) — https://learn.microsoft.com/en-us/windows/desktop/api/winuser/nf-winuser-callwindowproca

---

### Pitfall 3: Not chaining unhandled messages to AWT's original WndProc via `CallWindowProc`

**What goes wrong:**
`SetWindowLongPtr(hwnd, GWLP_WNDPROC, ourProc)` **replaces** the window procedure; it does not layer alongside it. AWT's own WndProc (`AwtWindow::WindowProc` in the JDK's native `awt_Window.cpp`) still needs to run for every message the new hook doesn't specifically want to override — focus handling, IME composition, painting dispatch back into the Java event queue, drag-and-drop, accessibility (UIA/MSAA), system color/theme change notifications, etc. If the custom procedure only handles `WM_NCHITTEST`/`WM_NCCALCSIZE`/`WM_NCLBUTTONDOWN` and does not call `CallWindowProc(originalWndProc, hwnd, msg, wParam, lParam)` for everything else, the window silently loses all the behavior AWT normally provides through that HWND — mouse/keyboard input into Compose stops being delivered correctly, or IME breaks, or the window never repaints on `WM_PAINT`.

**Why it happens:** It's tempting to write a "smart" WndProc that only forwards messages the code author thought of, rather than defaulting to "forward everything unless I have a specific reason to intercept it."

**How to avoid:** Structure the hook as: intercept only the small, named set of messages this feature needs (`WM_NCHITTEST`, `WM_NCCALCSIZE`, `WM_NCLBUTTONDOWN`/`UP` for `HTMAXBUTTON`, `WM_GETMINMAXINFO`, `WM_NCMOUSEMOVE`/`WM_NCMOUSELEAVE` for hover, `WM_DPICHANGED`) — for every other message, unconditionally `CallWindowProc` to the saved original AWT WndProc pointer and return its result immediately. Store that original pointer at subclass-install time via `GetWindowLongPtr(hwnd, GWLP_WNDPROC)` before overwriting it — never assume/hardcode it.

**Warning signs:** Anything not directly touched by the new title bar starts behaving oddly first — IME input broken, drag-and-drop of files onto the window stops, screen readers stop announcing the window, IME candidate window mispositioned. These are easy to miss because they're not the feature under test.

**Phase step:** Step 1 — the "always chain by default" rule goes in as a structural invariant of the WndProc dispatcher before any hit-test-specific logic is added, verified with an IME/paint/focus smoke check on the showcase.

**Sources:** Microsoft Learn, `CallWindowProc` / "Window Procedure Chaining" (HIGH, official) — https://learn.microsoft.com/en-us/windows/desktop/api/winuser/nf-winuser-callwindowproca ; OpenJDK `awt_Window.cpp` / `awt_Frame.cpp` (HIGH, primary source for what AWT's own proc does) — https://github.com/openjdk/jdk/blob/master/src/java.desktop/windows/native/libawt/windows/awt_Frame.cpp

---

### Pitfall 4: AWT re-subclasses or restores its own WndProc, silently undoing the hook

**What goes wrong:**
AWT is not a passive HWND owner — under certain conditions (window activation changes, drag-and-drop registration/unregistration, changing `setAlwaysOnTop`, certain focus-transfer paths, disposal) the JDK's native Windows AWT layer itself calls `SetWindowLongPtr(hwnd, GWLP_WNDPROC, ...)` or otherwise expects to own that slot. If our subclass installs itself once at window-creation time and AWT later re-asserts its own procedure (or expects `GWLP_WNDPROC` to still point at its own function for some internal check), our hook can be silently evicted — Snap Layouts/hit-testing stop working with no exception, no log, just "it used to work."

**Why it happens:** This class of interaction is undocumented for OpenJDK's Windows AWT peer (there's no public "we promise never to touch `GWLP_WNDPROC` after window-creation" contract) — the safe assumption is that AWT considers the WndProc slot its own property, and any external subclass is inherently living on borrowed time unless verified against the actual JDK build in use.

**How to avoid:** After every documented AWT-triggering event that could plausibly touch the window's native properties (window shown, `setVisible`, DnD `setDropTarget`, `toFront`/`toBack`, always-on-top toggle), re-verify (not blindly re-install) that `GetWindowLongPtr(hwnd, GWLP_WNDPROC)` still equals our installed pointer; if it doesn't, re-subclass and re-chain to the *new* current procedure (not the originally-saved one, which may now be stale). Treat re-subclassing as idempotent and cheap rather than a one-time setup step.

**Warning signs:** Snap Layouts/hit-testing worked right after window creation but stopped after some unrelated interaction (opening a dialog, toggling always-on-top, drag-and-drop). Intermittent, state-dependent — hard to reproduce without a deliberate stress sequence.

**Phase step:** Step 5 (Compose/OS state sync + regression guard) — add a periodic (or event-triggered) "is our WndProc still installed" self-check the verification step can assert on, since this class of bug has no natural exception to catch.

**Sources:** No official JDK doc states this contract either way (flagged as **LOW confidence / unverified negative claim** — training-data inference from general Win32 subclassing folklore, not a specific OpenJDK source read). **This needs first-hand verification during Step 1** against the actual JDK 21 Windows AWT peer behavior rather than being taken on faith — the maintainer's plan already calls this out as something to verify, not assume.

---

### Pitfall 5: Wrong unsubclass order on window dispose / multiple windows sharing global state

**What goes wrong:**
On window close, the subclass must be removed (`SetWindowLongPtr(hwnd, GWLP_WNDPROC, originalProc)`) **before** the HWND is destroyed, and the callback object must not be released before that restoration happens — releasing it first re-creates Pitfall 1's dangling-pointer crash during the brief window between "GC eligible" and "WM_NCDESTROY delivered." With **multiple windows** (the milestone explicitly requires supporting several native-chrome windows per app, including a narrow ~300px one), the fix must be per-HWND, not a single global "install once" — a naive `object` singleton holding one `originalWndProc` pointer will corrupt behavior for every window after the first, since each window has its own original AWT proc pointer.

**Why it happens:** Single-window prototypes (which is how most WM_NCHITTEST tutorials and the JavaFX/JNA prior art, JFxBorderlessNative, are written) don't surface this — the bug only appears once a second window is opened, or once close-while-dragging is tested.

**How to avoid:** Key every piece of subclass state (original proc pointer, callback object reference, hover/pressed hit-test state for the custom max button) by `HWND`, in a map, not in singleton fields. On dispose: (1) restore the original proc, (2) confirm restoration via `GetWindowLongPtr` read-back, (3) only then drop the map entry / let the callback become GC-eligible.

**Warning signs:** First window works; second window opened in the same process shows wrong title-bar behavior, or closing one window breaks native chrome in a still-open sibling. Crash-on-close specifically (not crash-on-drag) is another signature of releasing the callback too early relative to `WM_NCDESTROY`.

**Phase step:** Step 1 (data structure design) must be per-HWND from the start; Step 6 (verification) must include a multi-window test opening/closing 2+ native-chrome windows including the narrow interactive-title-bar one, plus a close-during-active-drag repro.

**Sources:** Derived from JNA callback-lifetime guidance (HIGH, see Pitfall 1 sources) applied to the project's explicit multi-window requirement (MEDIUM — reasoning, not a found incident report for this exact case).

---

### Pitfall 6: WndProc runs on the AWT toolkit thread — reading/writing Compose state from it deadlocks or corrupts

**What goes wrong:**
The native window procedure for an AWT HWND is invoked by whatever thread pumps that thread's message queue — for AWT this is the **AWT-Windows / Toolkit thread**, which is *not* the same thread as Compose's UI/recomposition dispatcher (Compose Desktop runs its own coroutine-driven render loop). If the `WM_NCHITTEST` or `WM_NCLBUTTONDOWN` handler tries to read `windowState.placement`, call into `MutableState` snapshot reads/writes, or otherwise touch Compose runtime state directly and synchronously from inside the JNA callback, two failure modes are possible: (a) a **cross-thread Snapshot violation** (Compose state reads/writes have thread-affinity expectations tied to the Snapshot system) producing corrupted UI state or exceptions on the *next* recomposition, unrelated in time to the click that caused it; (b) a **deadlock**, if the callback blocks waiting on a result that itself requires the EDT/Compose thread to be free, while that thread is itself blocked waiting on the native call to return (this can happen with `SendMessage`-style synchronous native calls issued from Compose code back into the same HWND while inside its own WndProc — classic same-thread re-entrancy deadlock, or cross-thread if the message pump is stalled).

**Why it happens:** It's natural to want `WM_NCHITTEST` to answer based on live Compose layout (e.g. "is this pixel over the interactive `leading` content in the title bar?") — but that requires crossing from native-callback thread into Compose's owned state, which was never designed for synchronous native re-entrancy.

**How to avoid:** Treat the WndProc callback as a producer that only reads **plain, thread-safe, already-computed data** (e.g. a `java.util.concurrent.atomic.AtomicReference<HitTestRegions>` or a `volatile` snapshot struct updated by Compose on its own thread after each layout pass) — never call into `Snapshot`/`MutableState` machinery directly from the callback. Any Windows message answer that depends on "what's currently in the title bar" must be served from a pre-published, lock-free snapshot, not computed on demand inside the callback.

**Warning signs:** Rare, non-deterministic freezes specifically under drag/resize or clicking near interactive title-bar content; exceptions surfacing in unrelated recompositions; works fine when stepping through in a debugger (changes timing, masks the race) but fails intermittently at full speed — the classic signature of a cross-thread race.

**Phase step:** Step 1 (data flow design) — decide the publish/read boundary (a single `AtomicReference` snapshot struct) before writing any hit-test logic; Step 6 (verification) needs a stress test that hammers hover/click near the interactive title-bar `leading` slot while resizing, since this is a timing bug that won't show up in a single manual click.

**Sources:** General JNA-callback-thread-affinity guidance is MEDIUM confidence (inferred from documented Win32 threading rules — a WndProc always runs on the thread that owns the window — combined with Compose's known Snapshot thread-affinity model); **no exact incident report found for this specific JNA+Compose combination** — flagged for empirical verification in Step 1, not taken as fact from training data alone.

---

### Pitfall 7: Uncaught exceptions thrown inside a JNA callback crash the JVM (or corrupt native state) instead of propagating as a normal Java exception

**What goes wrong:**
A Java exception thrown inside a method invoked as a native callback (JNA `Callback.invoke`, called from inside `DispatchMessage`) does not unwind normally back through the native call stack the way it would in pure-Java code — JNA either logs it via its default `UncaughtExceptionHandler` and returns a default/zero value (which the OS then treats as "hit-test says nothing" or "message unhandled," silently breaking behavior) or, depending on configuration and JNA version, the exception can leave the native call in an undefined state. Either way, a `NullPointerException` from an unguarded read of `windowState` mid-drag does **not** show up as a normal stack trace crash the way the rest of the Kotlin/Compose codebase's errors do — it either vanishes or destabilizes the app.

**Why it happens:** Kotlin/Compose code is written assuming the JVM's normal exception model; a JNA callback boundary breaks that assumption without an obvious compile-time signal.

**How to avoid:** Wrap the entire body of the WndProc callback in a top-level `try { ... } catch (t: Throwable) { ...log via a known-safe path (no Compose calls)...; return defWindowProcFallback(...) }` — never let any exception escape the callback un-caught. Fail safe: on any internal error, forward to `CallWindowProc`/`DefWindowProc` rather than returning an arbitrary value, so a bug in the custom logic degrades to "acts like a normal AWT window" rather than corrupting the message loop.

**Warning signs:** Weird one-off "window stopped responding to hover" or "hit-test returned wrong region once" without any visible exception in logs — the tell is a *missing* stack trace where one would normally be expected.

**Phase step:** Step 1 — the try/catch-and-fall-back-to-DefWindowProc wrapper is part of the initial callback scaffold, not an afterthought; Step 6 — a fault-injection test (force an exception path) proving the fallback engages instead of crashing.

**Sources:** JNA `Callback` / `CallbacksAndClosures.md` documents that uncaught exceptions in callbacks are handled specially (MEDIUM confidence — the doc's general callback-safety guidance; exact current-version exception-swallowing behavior not independently re-verified against JNA 5.19.1's changelog in this pass) — https://github.com/java-native-access/jna/blob/master/www/CallbacksAndClosures.md

---

### Pitfall 8: `WM_NCCALCSIZE` naive "return 0" causes maximized-window overhang — content clipped, corners hang off-screen, taskbar covered

**What goes wrong:**
The textbook way to remove the frame is: on `WM_NCCALCSIZE` with `wParam == TRUE`, return 0 so the client area equals the full proposed window rect. This is correct for the **floating** (non-maximized) state, but when the window is **maximized**, Windows still computes the proposed rect using `WS_THICKFRAME` sizing-frame math that extends past every monitor edge by the frame thickness (`SM_CXFRAME`/`SM_CYFRAME` + `SM_CXPADDEDBORDER`, DPI-scaled) — a "return 0" handler leaves that overhang in place, so the top/content gets pushed off-screen and the window visually overlaps/covers the taskbar and screen edges. This is a very well-known, very commonly mis-implemented step — it is *the* signature bug of hand-rolled borderless windows.

**Why it happens:** The naive fix (return 0 unconditionally) looks correct because it matches the floating case, and the bug is invisible until the window is actually maximized on a real monitor.

**How to avoid:** When `wParam == TRUE` and the window `IsZoomed` (maximized): take the frame's proposed rect and **inset it inward** by the DPI-scaled frame thickness (`GetSystemMetricsForDpi(SM_CXFRAME)+SM_CXPADDEDBORDER` horizontally, `SM_CYFRAME+SM_CXPADDEDBORDER` vertically) at the window's *current* DPI (not a hardcoded 96 DPI value) before returning it — this is exactly the fix documented and applied in a real Electron-adjacent project (`kelpie` PR #140) and matches Windows Terminal's own `NonClientIslandWindow::_UpdateFrameMargins`/`OnNcCalcSize` handling, which pushes `newSize.top` down by `_GetResizeHandleHeight()` when maximized.

**Warning signs:** Window looks perfect floating, but maximizing crops the top of the title bar / bottom content, or the window visibly extends past the monitor edge into the taskbar area.

**Phase step:** Step 2 (hit-testing & frame removal) — this is the core deliverable of that step; must be tested at both 100% and (per milestone) 150% DPI, maximized, on the single available monitor.

**Sources:** Microsoft Learn `WM_NCCALCSIZE` (HIGH, official) — https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-nccalcsize ; Windows Terminal `NonClientIslandWindow.cpp` (HIGH, real Microsoft production source, read directly) — https://github.com/microsoft/terminal/blob/main/src/cascadia/WindowsTerminal/NonClientIslandWindow.cpp ; `kelpie` PR #140 fix description (MEDIUM, third-party OSS but technically precise and matches Terminal's approach) — https://github.com/UnlikeOtherAI/kelpie/pull/140

---

### Pitfall 9: Maximized window swallows the auto-hidden taskbar entirely (this project's exact repro condition — auto-hide is ON)

**What goes wrong:**
Fixing Pitfall 8's overhang against the *monitor bounds* is not enough. If the maximized client rect is made to exactly equal the **full monitor rect** (`rcMonitor`, not `rcWork`), Windows treats the window as effectively full-screen over that edge, and an **auto-hidden** taskbar loses its ability to pop back up on mouse-hover at that screen edge — it can become stuck hidden until the window is un-maximized or Explorer is restarted. This is the project's literal dev-machine condition: single monitor, taskbar at the bottom, auto-hide **ON**, so the monitor work area equals the full monitor and there is no natural "make it fit rcWork" fallback the way there would be with auto-hide off.

**Why it happens:** The naive "maximize to monitor bounds so nothing is clipped" fix from Pitfall 8, applied without taskbar-awareness, overshoots straight into this second bug — the two pitfalls are easy to conflate into one fix that solves one and re-breaks the other.

**How to avoid:** Detect auto-hide taskbars per-edge via `SHAppBarMessage(ABM_GETSTATE, ...)` (check the `ABS_AUTOHIDE` flag) combined with `ABM_GETAUTOHIDEBAREX` (or the older `ABM_GETAUTOHIDEBAR`) per edge to find which edges actually have an auto-hide bar docked. On any edge where one is detected, inset the maximized client rect by a small fixed amount (Windows Terminal uses **2 px**) on that edge specifically — enough for the OS's edge-hover detection to still register, without visibly cropping content in any meaningful way. This must be re-evaluated on `WM_GETMINMAXINFO` (which is what actually governs the maximize size Windows computes) as well as consumed in the `WM_NCCALCSIZE` handler, since both are involved.

**Warning signs:** On the actual dev machine (auto-hide ON, single monitor, bottom taskbar): maximize the app, then try to reveal the taskbar by moving the mouse to the bottom edge — if it doesn't reappear, this pitfall has been hit exactly. This is directly testable without any DPI-scale monitor, unlike Pitfall 8 which needs 150% to fully exercise.

**Warning signs (visible taskbar case, `WM_GETMINMAXINFO`/`rcWork`):** When auto-hide is off, the correct behavior instead comes from `WM_GETMINMAXINFO`'s `MINMAXINFO.ptMaxSize`/`ptMaxPosition`, which by default already respects `rcWork` (the monitor rect minus the visible taskbar) — a custom `WM_GETMINMAXINFO` handler that overrides these with the *full monitor rect* (a common mistake when "fixing" Pitfall 8 by hand) will make maximize cover a **non-auto-hide** taskbar too. Test both taskbar modes even though the dev machine defaults to auto-hide.

**Phase step:** Step 4 (taskbar & DPI edge cases) — this is its own dedicated step given the dev machine's specific auto-hide-ON condition; must be verified via `SHAppBarMessage`/`GetWindowRect` state checks (see Verification section below), not just visual inspection, since "does the taskbar visually reappear" requires either real mouse movement (banned per project rules) or a scripted equivalent.

**Sources:** Windows Terminal `NonClientIslandWindow.cpp`, auto-hide handling with `ABM_GETSTATE`/`ABM_GETAUTOHIDEBAREX` and the 2px inset (HIGH, read directly from Microsoft's production source) — https://github.com/microsoft/terminal/blob/main/src/cascadia/WindowsTerminal/NonClientIslandWindow.cpp ; corroborating community writeups (MEDIUM) — https://github.com/microsoft/terminal/issues/13679 , Firefox Bugzilla 642851 (MEDIUM, independent confirmation of the same class of bug) — https://bugzilla.mozilla.org/show_bug.cgi?id=642851

---

### Pitfall 10: `undecorated=true` alone does not create `WS_THICKFRAME`/full caption-button styles — the window is plain `WS_POPUP`, and native Snap simply does nothing on it

**What goes wrong:**
Reading the actual OpenJDK Windows AWT native source confirms: an undecorated `Frame`'s native style is set to `WS_POPUP | WS_SYSMENU | WS_CLIPCHILDREN | WS_MAXIMIZEBOX | WS_MINIMIZEBOX` (extended style 0) — **no `WS_CAPTION`, and critically no `WS_THICKFRAME`**. `WS_THICKFRAME` is what gives a window the native sizing-border behavior that Aero Snap (drag-to-edge, Win+Arrow, Snap Layouts flyout on the maximize button) is built around; without it, even a perfectly correct `WM_NCHITTEST`/`WM_NCCALCSIZE` implementation may not get full native Snap behavior, because the window class fundamentally isn't advertising itself as resizable-with-a-frame to DWM/the shell. This directly matches the CMP GitHub issue reports of Aero Snap simply not working on undecorated CMP windows (JetBrains/compose-multiplatform#1248, #2062) — those reports predate any JNA-based fix and describe exactly this starting condition.

**Why it happens:** `undecorated=true` in Compose/AWT is documented as "no native title bar/border," which developers read as "same window, just chrome hidden" — it is not; it is a structurally different native window style.

**How to avoid:** After window creation (from Step 1's subclass-install point, before the window is first shown, or immediately after via `SetWindowPos(..., SWP_FRAMECHANGED)`), explicitly `SetWindowLongPtr(hwnd, GWL_STYLE, currentStyle | WS_THICKFRAME | WS_MAXIMIZEBOX | WS_MINIMIZEBOX | WS_CAPTION)` — note `WS_CAPTION` may be needed too (some approaches add it and then hide it visually via `WM_NCCALCSIZE`, since a plain `WS_THICKFRAME` without `WS_CAPTION` has historically had inconsistent Snap-Layouts recognition in some Windows versions; **this specific point needs first-hand verification during Step 2**, it's the maintainer's expected approach but not yet confirmed against Windows 11 24H2 specifically). Always follow any `GWL_STYLE` change with `SetWindowPos(hwnd, NULL, 0,0,0,0, SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER)` — Windows caches frame geometry and won't re-evaluate the new styles otherwise.

**Warning signs:** Hit-testing appears to work (returns `HTCAPTION`/`HTTOP`/etc. correctly when queried directly) but dragging to a screen edge still doesn't show the Snap preview, or the maximize-button hover never shows the Snap Layouts flyout — the style bits, not the message handling, are the actual blocker.

**Phase step:** Step 1 (subclass scaffolding) must add the style-bit fixup as an explicit, separate, verified sub-step immediately after subclass install, before any hit-test logic is written — get `WS_THICKFRAME`/`WS_MAXIMIZEBOX` confirmed present (`GetWindowLongPtr(hwnd, GWL_STYLE)` read-back) before debugging hit-testing at all, since hit-testing on the wrong style class will never fully work regardless of how correct the message handlers are.

**Sources:** OpenJDK `awt_Frame.cpp` (HIGH, primary source, confirms exact style flags for undecorated frames) — https://github.com/openjdk/jdk/blob/master/src/java.desktop/windows/native/libawt/windows/awt_Frame.cpp ; JetBrains/compose-multiplatform#1248 and #2062 (MEDIUM, confirms the symptom is reported and unsolved upstream) — https://github.com/JetBrains/compose-multiplatform/issues/1248 , https://github.com/JetBrains/compose-multiplatform/issues/2062

---

### Pitfall 11: `HTMAXBUTTON` breaks click/hover/tooltip behavior unless `DefWindowProc`/`DwmDefWindowProc` is deliberately kept in the loop for that button, and Snap Layouts require `WS_MAXIMIZEBOX` too

**What goes wrong:**
Returning `HTMAXBUTTON` from `WM_NCHITTEST` when the cursor is over the custom-drawn maximize button in `AeroTitleBar`'s `TitleBarButton` is what makes Windows 11 show the Snap Layouts flyout on hover — but it also hands Windows ownership of that hit-test region for **click handling**: `WM_NCLBUTTONDOWN`/`WM_NCLBUTTONUP` with `HTMAXBUTTON` go through non-client message handling, not the client-area `clickable`/`hoverable` modifiers the existing `TitleBarButton` composable uses. If `DefWindowProc` (or `DwmDefWindowProc`, which several sources note should be given first refusal for caption-button hit-testing) is not correctly wired for that region, one of two things breaks: either (a) the OS default painting takes over and paints its own generic caption button on top of/instead of the Aero-styled button (losing the custom visual entirely), or (b) the custom Compose `hoverable`/`clickable` state stops updating because the click never reaches Compose's client-area pointer input pipeline the normal way. Additionally, Snap Layouts on hover **only appears at all if `WS_MAXIMIZEBOX` is present** in the window style (tied to Pitfall 10) — a maximize button drawn in Compose with no corresponding native style bit will hit-test as `HTMAXBUTTON` but never trigger the flyout, a confusing partial-failure state.

**Why it happens:** This is inherently a "the OS now owns this pixel region" feature — full ownership handoff (native click semantics, native hover semantics) is DWM Snap Layouts' actual contract, not opt-in per-message.

**How to avoid:** When `WM_NCHITTEST` reports `HTMAXBUTTON`, suppress Compose's own `hoverable`/`clickable` handling for that button's client-side composable (it must become purely a *visual* surface reflecting hover/pressed state published from the native side, not an interactive `clickable` — the click itself must be actuated by the OS, which will deliver a `WM_SYSCOMMAND SC_MAXIMIZE`/`SC_RESTORE` that the app handles the same way `windowState.placement` toggling already works). Publish native hover/pressed state (from `WM_NCMOUSEMOVE`/`WM_NCLBUTTONDOWN`/`WM_NCLBUTTONUP` on `HTMAXBUTTON`, with `WM_NCMOUSELEAVE` clearing it — see Pitfall 12) into the same thread-safe snapshot structure from Pitfall 6, and have Compose read that to drive the existing hover-tint visual, instead of `collectIsHoveredAsState()`. Pass `WM_NCHITTEST` first to `DwmDefWindowProc` per Microsoft's guidance for caption buttons before falling back to custom logic.

**Warning signs:** Maximize button visually never shows hover/press feedback anymore once HTMAXBUTTON is wired (state publishing not connected); or a generic OS-drawn square briefly flashes over the custom icon on click (DefWindowProc default painting winning); or Snap Layouts flyout simply never appears despite correct hit-test answers (missing `WS_MAXIMIZEBOX`, see Pitfall 10).

**Phase step:** Step 3 (Snap Layouts / max button) — this is the step's central deliverable; must be built and verified as its own unit after Steps 1–2 (subclass + basic caption/resize hit-testing) are solid, since it depends on both.

**Sources:** Microsoft Learn `WM_NCHITTEST` (HIGH, official, explicitly documents `HTMAXBUTTON` and the `DwmDefWindowProc` guidance) — https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest ; Microsoft Learn "Apply snap layout menu" (HIGH, official, Win32-app-specific guidance confirming `WM_NCHITTEST`→`HTMAXBUTTON` requirement) — https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-snap-layout-menu ; winit issue #3884 and Windows Terminal issue #9443 (MEDIUM, independent confirmations from other native-window projects hitting the identical requirement) — https://github.com/rust-windowing/winit/issues/3884 , https://github.com/microsoft/terminal/issues/9443

---

### Pitfall 12: `WM_NCMOUSELEAVE` tracking is not automatic — custom caption-button hover gets stuck "hot" after the cursor leaves

**What goes wrong:** Unlike client-area mouse leave (which AWT/Compose's `hoverable` already handles via normal pointer input), non-client mouse-leave detection for the custom `HTMAXBUTTON` region requires an explicit `TrackMouseEvent` call (with `TME_LEAVE | TME_NONCLIENT`) issued from inside the `WM_NCMOUSEMOVE` handler — and that tracking is **one-shot**: `WM_NCMOUSELEAVE` fires once, then tracking is cancelled and must be re-armed on the next `WM_NCMOUSEMOVE`. Skip the re-arm and the hover-highlight on the maximize button can get stuck lit after a single leave-then-reenter-then-leave sequence.

**Why it happens:** `TrackMouseEvent`'s one-shot nature is a Win32 API design wart that's easy to miss when the analogous client-area Compose API (`hoverable`) hides this complexity entirely.

**How to avoid:** Call `TrackMouseEvent` with `TME_LEAVE | TME_NONCLIENT` on every `WM_NCMOUSEMOVE` over the tracked button (not just the first), and clear the published hover state (Pitfall 6/11's snapshot struct) on `WM_NCMOUSELEAVE`.

**Warning signs:** Hover highlight on the maximize button "sticks" after fast mouse movement across it, especially noticeable when moving the cursor quickly from the button straight onto the Snap Layouts flyout and back.

**Phase step:** Step 3, same unit as Pitfall 11 — write the re-arm-on-every-NCMOUSEMOVE rule into the same hover-state-publishing code path from the start rather than patching it in after a visible bug report.

**Sources:** Microsoft Learn `WM_NCMOUSELEAVE` (HIGH, official, documents the tracking-cancel behavior) — https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-ncmouseleave

---

### Pitfall 13: `WM_NCHITTEST` coordinates are physical screen pixels; Compose dp/window-local coordinates are not — every hit-test comparison is a DPI bug waiting to happen

**What goes wrong:** `lParam` in `WM_NCHITTEST` carries the cursor position in **physical screen pixels**, in screen coordinates (not window-client-relative, and not DPI-independent). Compose's own coordinate space is dp-based and window-relative. Any hit-test logic that compares "is this pixel inside the title bar's interactive `leading` slot" needs three separate correct conversions: (1) screen → window-client physical pixels (subtract the window's screen origin, itself DPI-scaled), (2) physical pixels → dp (divide by the *window's actual current DPI scale factor*, not a hardcoded 1.0 or 1.5), (3) keep this in sync as the window moves between the project's stated 100%/150% monitor scenarios or as `WM_DPICHANGED` fires. A hit-test region computed once at window-creation DPI and never refreshed will be silently wrong on any monitor at a different scale, or after a drag between monitors of different scale.

**Why it happens:** It's easy to build and test the hit-test math on a single 100%-DPI dev monitor (which is this project's actual dev machine) and never notice the physical-vs-dp mismatch until a 150% monitor is involved — exactly the milestone's stated multi-DPI requirement, which the dev machine cannot self-verify end-to-end (single monitor).

**How to avoid:** Always fetch the window's *current* DPI via `GetDpiForWindow(hwnd)` (per-monitor-v2 aware) at the moment of each `WM_NCHITTEST` call — do not cache it across window moves — and convert consistently. Route all interactive-region boundaries (the `leading` slot in `AeroTitleBar`, the button hit zones) through one single conversion helper shared between the native hit-test code and whatever Compose-side layout reports positions, so there's exactly one place DPI math can be wrong instead of many.

**Warning signs:** Hit-test math looks correct at 100% DPI (the only DPI actually available on the dev machine) but is untested and unverifiable in-house at 150% — this pitfall is specifically flagged because the project **cannot fully verify it locally** (single monitor, 100% only) and needs either a documented gap (like v3.0/v3.1's DPI gaps already recorded in STATE.md) or a scripted DPI-change simulation.

**Phase step:** Step 4 (DPI edge cases) — since this can't be fully verified on the dev machine's single 100% monitor, the phase plan should explicitly decide whether 150% verification is in scope (requiring either temporarily changing the dev monitor's scale factor in Windows Settings, or accepting a named gap the way v3.0's SHW-16 125%/200% gap was accepted) rather than silently shipping unverified.

**Sources:** Windows Terminal `NonClientIslandWindow.cpp`, `GetSystemMetricsForDpi`/DPI-aware button-width scaling (HIGH, confirms physical-pixel/DPI handling pattern in a real shipped app) — https://github.com/microsoft/terminal/blob/main/src/cascadia/WindowsTerminal/NonClientIslandWindow.cpp ; JDK-8286581 / JDK-8055453 (MEDIUM, official OpenJDK bug tracker, confirms Java's own DPI-awareness history/gaps on Windows) — https://bugs.openjdk.org/browse/JDK-8286581 , https://bugs.openjdk.org/browse/JDK-8055453

---

### Pitfall 14: `WM_DPICHANGED` during a drag between differently-scaled monitors — who applies the suggested rect, AWT or the custom code, and does `WindowState` drift out of dp-sync

**What goes wrong:** `WM_DPICHANGED` delivers a `RECT*` (via `lParam`) that Windows *suggests* the app use for its new size/position at the new DPI. If AWT's own Windows peer already handles this message (as it must, to keep its own layout/font metrics correct — Java has had per-monitor DPI awareness work since JDK 9+, with JDK 21 shipping a `PerMonitorV2` manifest by default) and the new JNA subclass *also* tries to handle/apply it (or worse, doesn't forward it to AWT via `CallWindowProc` at all — see Pitfall 3), the two can disagree about the final window rect, causing a visible size "jump" during the DPI transition, or `windowState.size`/`windowState.position` (Compose's dp-based state) ending up computed from a stale physical-pixel rect that doesn't match what AWT itself settled on.

**Why it happens:** This is a two-owner problem: AWT already has DPI-transition logic; adding a second WndProc-level handler for the same message without knowing exactly what AWT's handler does creates a race/conflict, not an enhancement. This project's dev machine (single monitor) cannot exercise this at all — it can only be exercised with 2+ differently-scaled monitors or a simulated `WM_DPICHANGED`.

**How to avoid:** Default to **not** intercepting `WM_DPICHANGED` at all — forward it unconditionally to AWT's original WndProc (Pitfall 3's chaining rule) and let AWT's existing, already-tested DPI-transition logic own it entirely. Only add custom `WM_DPICHANGED` handling if a specific defect is observed that proves AWT's own handling is insufficient for this window's undecorated/subclassed configuration — and if so, treat that as a deliberately scoped, separately verified sub-feature, not a default assumption baked into Step 1.

**Warning signs:** Cannot be observed on the dev machine's single 100% monitor at all — this is a **known verification gap**, analogous to v3.0/v3.1's already-accepted 125%/200% DPI gaps (STATE.md), and should be named as such rather than silently assumed-fine.

**Phase step:** Step 4 — explicitly scope this as "forward, don't override, unless empirically proven necessary," and record the DPI-transition-during-drag scenario as an accepted verification gap in the final report if no second monitor becomes available, exactly as v3.0's SHW-16 gap was recorded rather than hidden.

**Sources:** Java per-monitor DPI awareness / manifest (MEDIUM, secondary sources + OpenJDK bug tracker corroboration, no single authoritative page found stating JDK 21's exact default) — https://bugs.openjdk.org/browse/JDK-8286581 ; Microsoft Learn `WM_DPICHANGED_BEFOREPARENT` and related DPI messages (HIGH for the Win32 side) — https://learn.microsoft.com/windows/win32/hidpi/wm-dpichanged-beforeparent

---

### Pitfall 15: Compose `WindowState.placement` desyncs from actual OS window state, and the existing `AeroResizeHandles`/`WindowDraggableArea` actively fight the new native behavior

**What goes wrong:** This project's `AeroTitleBar` reads `windowState.placement == WindowPlacement.Maximized` to decide which icon to show (`FrameCorners` vs `Square`) and to drive the minimize/maximize/restore buttons — this is **Compose's own tracked state**, which is normally kept in sync by CMP's window-event bridging when the user interacts through CMP's own code paths (the title bar's `onClick` handlers, or CMP's built-in resize). Once maximize/restore/snap can also happen **natively** (double-click on a native-recognized caption area, Win+Up/Down, a Snap Layouts flyout choice, dragging to a screen edge — none of which route through `windowState.placement = ...` in Kotlin code), `WindowState.placement` can silently go stale relative to the real OS window state, which then shows the wrong icon (maximize icon shown while the window is actually maximized, or vice versa) and can make the existing `AeroResizeHandles` composable — which **gates its own 8 drag zones on `windowState.placement != WindowPlacement.Floating`, returning early to disable them** — either stay active when it shouldn't (window natively maximized but Compose still thinks it's Floating, so the invisible resize-zone Boxes remain live and interactable, producing bizarre resize behavior on an already-maximized window) or stay disabled when it shouldn't. Separately, `WindowDraggableArea` (still wrapping the whole title bar `Row` in current `AeroTitleBar.kt`) does not pass `HTCAPTION` to the OS at all (confirmed by the file's own KDoc and PROJECT.md's own out-of-scope note) — once native `HTCAPTION` hit-testing is added, **the existing `WindowDraggableArea` wrapper becomes redundant at best and a second, competing drag-handling mechanism at worst**, and must be removed (or at minimum proven inert) rather than left in place "just in case."

**Why it happens:** `WindowState` was designed around CMP owning 100% of window-placement transitions; introducing an OS-driven side channel (native Snap/maximize) breaks that single-source-of-truth assumption, and it's exactly the kind of gap that only surfaces once a real native Snap action is triggered, not from code review of the Compose side alone. This is directly corroborated by an existing JetBrains/compose-multiplatform issue (#3625) about an undecorated CMP window's *existing* resizer misbehaving because it only gated on `isResizable`, not `placement == Floating` — the exact same class of bug this project's `AeroResizeHandles` needs to avoid, one level up (native maximize instead of CMP's built-in one).

**How to avoid:** (1) After any native message that changes maximize/restore/minimize/snap state (`WM_SYSCOMMAND` with `SC_MAXIMIZE`/`SC_RESTORE`/`SC_MINIMIZE`, or a size/move that results in `IsZoomed(hwnd)` becoming true/false), explicitly push the new state into `windowState.placement` from the WndProc's published-state mechanism (Pitfall 6) so Compose's title-bar icon and any other placement-dependent logic stay correct — never assume CMP's own bridging already covers OS-native transitions it didn't originate. (2) Remove `WindowDraggableArea` from `AeroTitleBar` once native `HTCAPTION` hit-testing covers the same drag region — leaving both active is not free redundancy, it's two systems answering "is this a drag" for overlapping pixels. (3) Re-verify `AeroResizeHandles`' existing `if (windowState.placement != WindowPlacement.Floating) return` guard continues to correctly disable the 8 resize-zone Boxes once `windowState.placement` can now also flip due to *native* Snap/maximize, not just CMP-driven ones.

**Warning signs:** Maximize/restore icon in the title bar shows the wrong state after a Snap Layouts choice or Win+Up; invisible resize-handle Boxes still respond to drag on a window that's visibly (natively) maximized; dragging the title bar produces two competing behaviors (a slight double-drag jitter) once both `WindowDraggableArea` and native `HTCAPTION` are active simultaneously.

**Phase step:** Step 5 (Compose/OS state sync) — this step exists specifically to close this gap, after Steps 2–3 (hit-testing, Snap Layouts) are functionally correct at the Win32 level; must include removing `WindowDraggableArea` and re-testing `AeroResizeHandles`' Floating-only guard against native transitions specifically, not just CMP-driven ones.

**Sources:** `AeroTitleBar.kt`/`ResizeHandles.kt` current source (HIGH, read directly, this repo) ; JetBrains/compose-multiplatform#3625 (MEDIUM, independent confirmation of the same *class* of placement-gating bug in CMP's own built-in resizer) — https://github.com/JetBrains/compose-multiplatform/issues/3625 ; PROJECT.md's own recorded "Aero Snap on custom window" out-of-scope note (HIGH, this repo, confirms `WindowDraggableArea` does not send `HTCAPTION`)

---

### Pitfall 16: JBR vs standard JDK — the showcase's Hot Reload run is on JBR, which has its **own** native custom-title-bar machinery, and Hot Reload can double-install a subclass on effect re-run

**What goes wrong:** The published library must run on a **standard JDK 21** (no JBR requirement, per constraints) — but the showcase's `hotRun`/Hot Reload path specifically runs on **JetBrains Runtime 21** (already true today per PROJECT.md's Codebase line), and JBR has had its own custom-window-decoration feature (enabled by default since JBR 2023.2, exposed via `java.awt.Window$CustomWindowDecoration` reflection) that may install its own native hooks/behavior for the same HWND concept this milestone's JNA code targets. Running the new JNA-based subclass under JBR risks **two independent systems** trying to own non-client behavior for the same window — at minimum a wasted/confusing verification signal (does a JBR-run showcase actually exercise *this* code path, or does JBR's own machinery intercept first), at worst actual conflict if JBR's own decoration handling also touches `GWLP_WNDPROC` or DWM attributes. Separately, **Compose Hot Reload re-runs `@Composable` effects** (this project's own MEMORY notes flag "hot reload re-running effects and double-installing a subclass" as a specific named risk) — if the JNA subclass-install call lives inside a `LaunchedEffect`/`remember` block that Hot Reload re-executes on every code edit without a correct disposal/idempotency guard, each hot-reload edit could re-subclass an already-subclassed window, stacking WndProc chains indefinitely (each new install saves "current" as "original" and wraps it — after N reloads, N layers deep, N times the per-message overhead, and any bug in unsubclass-ordering compounds N-fold).

**Why it happens:** JBR's own decoration system and this milestone's from-scratch JNA system are two independently-evolved, un-coordinated pieces of code that happen to both want ownership of the same native surface, and Hot Reload's whole point (re-run composition without restarting the process) is structurally hostile to "install this exactly once per process" native side-effects unless explicitly guarded.

**How to avoid:** Confirm — do not assume — whether the showcase should actually disable JBR's own `CustomWindowDecoration` if it's active (or whether it's simply inert because `undecorated=true`+`transparent=false` already sidesteps it — needs checking, since JBR's mechanism is normally opt-in via that same reflection API, so it likely isn't engaged unless the showcase explicitly calls it, which a `grep` of the codebase can confirm quickly). For Hot Reload idempotency: gate the native subclass-install behind an explicit **already-installed check** (Pitfall 4's "verify, don't blindly reinstall" logic doubles as the fix here) keyed by HWND, so a re-run install call is a safe no-op rather than a stacking re-subclass. Prefer installing the subclass from a stable, non-Composable lifecycle point (e.g. tied to the AWT window's own creation/dispose, similar in spirit to this project's own locked lesson that DSL/side-effecting lambdas must not be `@Composable`) rather than from inside a recomposable effect block that Hot Reload's re-execution model can trigger repeatedly.

**Warning signs:** Behavior differs between a Hot-Reload showcase session and a cold `./gradlew run` of the same showcase code — a strong signal that either JBR's own machinery or repeated-install stacking is involved, not the new feature code itself; hover/hit-test responsiveness measurably degrading the more times hot reload has re-run in a session (a symptom of a growing WndProc chain).

**Phase step:** Step 1 (idempotent-install guard, keyed by HWND, from the start) and Step 6 (verification must explicitly test: cold `./gradlew run` first, note behavior; then Hot Reload session with 3+ consecutive edits touching the title-bar composable, confirm no degradation) — this project already has the Hot Reload MCP tooling from v3.1 available to drive this check itself.

**Sources:** JetBrains/JetBrainsRuntime issue #362, "JBR Custom Window Decoration problem" (MEDIUM, confirms JBR has its own decoration machinery with real conflicts reported) — https://github.com/JetBrains/JetBrainsRuntime/issues/362 ; project's own MEMORY note naming this exact risk (HIGH, first-party, `~/.claude` memory already flags "hot reload re-running effects and double-installing a subclass" verbatim in the milestone context) ; PROJECT.md/STATE.md confirming JBR 21 is the `hotRun`/showcase JVM (HIGH, this repo)

---

### Pitfall 17: `transparent = true` is banned project-wide — do not reach for it "just for this feature" to fix shadow/rounded-corner cosmetics

**What goes wrong:** Several of the cosmetic side-effects of frame removal (restoring the DWM drop shadow via `DwmExtendFrameIntoClientArea` with negative margins, getting clean rounded corners) are commonly "solved" in generic borderless-window tutorials using window-transparency tricks. This project has a **locked, hard constraint**: `undecorated = true` WITHOUT `transparent = true`, because `transparent = true` causes `EXCEPTION_ACCESS_VIOLATION` on Windows 11 (CMP-3757 / GH#3171) — already true today (`AeroTitleBar.kt`'s own KDoc states it) and explicitly repeated as a constraint for this milestone. This rule extends to **all** `Popup`/`Dialog` per STATE.md's locked-decisions list, not just the top-level `Window`.

**Why it happens:** Most public borderless-window-with-shadow guides for AWT/Swing/JavaFX/Compose assume transparency is available and safe to use as a tool — it is specifically unsafe on this exact CMP/Windows 11 combination, a fact this project already learned the hard way before this milestone started.

**How to avoid:** Solve shadow/rounded-corner cosmetics using **DWM-attribute-only** techniques that don't require `transparent = true`: `DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, ...)` for corner rounding (Windows 11-only API, no transparency needed), and — if a native drop shadow is wanted at all — `DwmExtendFrameIntoClientArea` with a minimal (not full-negative) margin on a `WS_POPUP`-based (not `WS_THICKFRAME`-white-border-prone) style, understanding this may conflict with Pitfall 10's `WS_THICKFRAME` addition and needs its own empirical check rather than copying a tutorial verbatim. If shadow/rounded-corner cosmetics prove to require an approach this constraint blocks, that is itself a finding to report, not a reason to bend the constraint.

**Warning signs:** Any code review or research suggestion involving `transparent = true`, `WS_EX_LAYERED`, or `SetLayeredWindowAttributes` for this feature should be treated as a hard stop and escalated, not quietly tried "to see if it's fine this time" — this exact failure mode (EXCEPTION_ACCESS_VIOLATION) is a full-process crash, not a recoverable bug.

**Phase step:** Step 2 (frame removal) — the constraint should be restated as an explicit non-goal/guard-rail in that step's own scope notes, since shadow/corner cosmetics are exactly the kind of "nice to also fix while I'm in here" scope creep this constraint needs to block.

**Sources:** `AeroTitleBar.kt` KDoc + PROJECT.md Constraints + STATE.md locked decisions (HIGH, all first-party, this repo, already-proven incident) ; CMP-3757 / GH#3171 (referenced by name in this repo's own source, not independently re-fetched this pass — carried forward as already-verified project knowledge)

---

### Pitfall 18: A resize-flicker (white/black bars) regression already exists in CMP itself around AWT/Compose layout-phase desync — native resize is likely to expose it more, not less

**What goes wrong:** JetBrains' own YouTrack tracker has open issues describing white background flicker / visual artifacts (white stripes/bars) during window/dialog resize in Compose Desktop, explicitly attributed to Compose and Swing/AWT's layout and rendering phases not being synchronized (CMP-7700 "Synchronize Compose and Swing layout and rendering phases," CMP-7919 "White background flicker during window resize"). This project's **current** `AeroResizeHandles` drives resize entirely from the Compose/Kotlin side (`windowState.size = DpSize(...)` inside `detectDragGestures`), which already interacts with this class of bug. **Native resize** (the OS resizing the HWND directly via `WM_NCLBUTTONDOWN`+drag on a resize border, or Snap actions) changes window geometry via an entirely different path — the native HWND resizes essentially instantly on every mouse-move tick, while Compose's Skia-based render surface must catch up asynchronously — which is likely to expose this same class of desync *more* severely (a fully native drag can outrun Compose's render loop far more than a Compose-driven `pointerInput` drag naturally throttled by recomposition), not less.

**Why it happens:** This is an upstream CMP rendering-pipeline limitation, not something introduced by this milestone's JNA code — but switching part of the resize/drag path from Compose-driven to OS-driven changes the *timing characteristics* in a way that's likely to make an existing latent bug more visible, which is a genuine risk to the milestone's "looks native" goal even though the root cause isn't this milestone's code.

**How to avoid:** Treat this as a known-risk area to explicitly test, not assume-fine because "it's CMP's bug, not ours." Concretely: capture before/after `PrintWindow` frames during an active native-border resize drag (the project's own sanctioned capture method) specifically looking for the white/black-bar artifact; if reproduced, check whether it's within the severity/scope already tolerated for the existing Compose-driven resize (if so, name it as a carried-forward, not milestone-introduced, gap) or whether native resize measurably worsens it (if so, it needs its own mitigation or an explicit scope note, the same way v3.0/v3.1 named DPI gaps rather than hiding them).

**Warning signs:** Visible white/black flash strips along resize edges during a fast native-border drag, more pronounced than during the existing Compose-driven `AeroResizeHandles` drag at comparable speed.

**Phase step:** Step 6 (verification) — include a dedicated native-resize-drag capture pass (fast drag, both a resize edge and a corner) compared against the existing Compose-driven resize's known baseline flicker characteristics, and record findings as a named gap if not fully resolvable within this milestone (do not silently ship a worse resize experience than today's).

**Sources:** JetBrains YouTrack CMP-7700 and CMP-7919 (MEDIUM — found via search-result snippet describing the issue titles and a stated CMP 1.11.0-alpha03 partial-fix note; full issue page content could not be fetched directly this pass, so exact current-1.12.0 status is not independently re-confirmed) — https://youtrack.jetbrains.com/issue/CMP-7700 , https://youtrack.jetbrains.com/issue/CMP-7919

---

## Moderate Pitfalls

### Pitfall 19: Focus/activation on `HTCAPTION` clicks, and the title bar's own interactive `leading` content

**What goes wrong:** Returning `HTCAPTION` for most of the title-bar row (required for native drag) means the OS treats a click there as "activate + potential drag," which is normally fine — but this milestone explicitly calls out "a narrow ~300 px window with interactive content in its title bar" and `AeroTitleBar`'s existing `leading` slot is already documented as holding arbitrary composables (e.g. an app icon, but the API allows anything). Any pixel inside that `leading` slot must hit-test as `HTCLIENT` (so Compose's own click handling works), not `HTCAPTION` — but the boundary between "draggable chrome" and "interactive `leading` content" is exactly the DPI-sensitive, Compose-layout-sourced region from Pitfall 13, so this is really Pitfall 13's consequence playing out at the API-surface level: get the boundary wrong and either dragging breaks (whole bar returns HTCLIENT) or the `leading` content becomes unclickable (whole bar returns HTCAPTION).

**How to avoid:** The published hit-test-region snapshot (Pitfall 6) must carry not just "is this the title bar" but the exact interactive sub-rectangles within it (each button, each pixel of `leading` if it's interactive), refreshed on every Compose layout pass, not just once at composition.

**Phase step:** Step 3 (same unit as button hit-testing) — this is functionally identical work to the maximize/minimize/close button hit-zones, just generalized to an arbitrary caller-supplied `leading` slot.

---

### Pitfall 20: Alt+Space system menu — verifying Move/Size actually work once the window is a subclassed `WS_THICKFRAME`/`WS_POPUP` hybrid

**What goes wrong:** `WS_SYSMENU` is already present on AWT's undecorated-frame style (Pitfall 10), so Alt+Space already opens *a* system menu today — but "Move" and "Size" menu items specifically rely on the window correctly answering hit-tests and responding to the resulting `WM_SYSCOMMAND SC_MOVE`/`SC_SIZE` messages, which in turn drive keyboard-based move/resize via synthetic mouse-like tracking. If the custom `WM_NCHITTEST`/`WM_NCCALCSIZE` handling has any of Pitfalls 8–10's bugs, Move/Size via the keyboard-only system menu path can fail in ways that look different from mouse-driven drag/resize failures (no cursor position to reason about, since it's keyboard-initiated).

**How to avoid:** Explicitly test Alt+Space → Move and Alt+Space → Size (both are legitimate, non-cursor-moving keyboard interactions the project's "don't move the real cursor" rule doesn't forbid) as part of verification, not just mouse-driven drag/resize.

**Phase step:** Step 6 (verification) — add as an explicit keyboard-only checklist item, distinct from mouse-drag testing.

---

### Pitfall 21: Keyboard/accessibility (UIA) exposure of a custom-drawn `HTMAXBUTTON`

**What goes wrong:** A native caption button normally exposes itself to UI Automation (screen readers, accessibility tooling) as a standard control with a recognizable role/name. A Compose-drawn button that merely *hit-tests* as `HTMAXBUTTON` for mouse purposes gets none of that automatically — Compose's own semantics tree (which is what Compose's accessibility bridge actually exposes) still describes it as a normal clickable Box in the client area, which may now be misleading (since actual activation goes through the OS non-client path, not a semantics-tree "perform click" action) or, if the `clickable` modifier was removed per Pitfall 11's fix, may stop being exposed as interactive at all.

**How to avoid:** Explicitly set/verify the button's Compose `semantics` (role = Button, appropriate content description reflecting Maximize/Restore state) independent of whatever native hit-testing changes are made, so screen-reader users retain a discoverable, describable control even though the actual OS-level activation path changed.

**Phase step:** Step 3 or Step 6 — at minimum a verification checklist item; this is a "looks done but isn't" risk since visual/mouse testing won't surface it.

---

### Pitfall 22: JNA native library extraction, `--enable-native-access` warnings on JDK 21+, and jna-platform version clash with consumers

**What goes wrong:** Three distinct, compounding packaging risks: (1) JNA extracts a native `jnidispatch` shared library to a temp directory at runtime by default — this can fail or behave unexpectedly in locked-down or unusual consumer environments (permissions, AV scanners, read-only temp), though this is a long-standing JNA characteristic, not new to this milestone. (2) JDK 21+ prints a `WARNING: A restricted method in java.lang.System has been called by com.sun.jna.Native` console warning whenever JNA loads its native code, unless the consuming application is launched with `--enable-native-access=ALL-UNNAMED` (or the specific module) — this warning is harmless but visible, and JDK 24+ is moving toward making unguarded restricted-method calls throw rather than warn (JEP 472 direction), which is a forward-looking packaging risk for a published library, not just a cosmetic one. (3) The constraint notes the consumer **already uses jna-platform 5.19.1** — publishing this library with a *different* JNA/jna-platform version than a consumer app already depends on risks the well-documented JNA failure mode where two different versions of the native `jnidispatch` library get loaded into the same JVM (via different classloaders or JAR versions), producing `UnsatisfiedLinkError`/version-mismatch native-load failures that are notoriously confusing to diagnose from a consumer's perspective.

**How to avoid:** Pin the library's JNA/jna-platform dependency to exactly `5.19.1` (matching the stated consumer version) rather than "whatever's latest at implementation time," and document this as a floor/pin the same way Material3 is already pinned for a similar cross-version-drift reason. Add a README note about `--enable-native-access` being needed on JDK 21+ launches if consumers see the warning (or investigate whether it can be suppressed/declared via the library's own module descriptor — needs verification, not assumed). Do not bundle/shade a different-versioned copy of JNA inside the library artifact.

**Phase step:** Step 1 (dependency setup, first commit of the phase) — pin the version before writing any JNA code, and add the README consumer note as part of the phase's own release-adjacent documentation step (mirroring how v3.1 documented its own consumer-floor bump).

**Sources:** JNA issue #1711 "Restricted method call warning in Java 25" and #1665 (MEDIUM, JNA's own issue tracker, confirms the warning and forward direction) — https://github.com/java-native-access/jna/issues/1711 , https://github.com/java-native-access/jna/issues/1665 ; JEP 472 (HIGH, official OpenJDK JEP) — https://openjdk.org/jeps/472 ; Oracle "Restricted Methods" docs (HIGH, official) — https://docs.oracle.com/en/java/javase/22/core/restricted-methods.html ; general JNA version-mismatch community reports (MEDIUM, multiple independent projects) — e.g. https://github.com/testcontainers/testcontainers-java/issues/3308

---

## Minor Pitfalls

### Pitfall 23: `WS_THICKFRAME` reintroducing a stray 1px white top border/strip on Windows 10-era rendering paths (secondary risk even though target is Windows 11 24H2)

**What goes wrong:** A commonly-reported side-symptom of adding `WS_THICKFRAME` back for Snap support (Pitfall 10) alongside `DwmExtendFrameIntoClientArea` is a stray white/light 1px line at the top edge, especially historically on Windows 10 — one documented cause-and-fix pairing found is that the white line specifically correlates with `WS_THICKFRAME` combined with a bare `DwmExtendFrameIntoClientArea` call, and using `WS_POPUP` instead of `WS_THICKFRAME` for the specific styling that triggers the DWM frame extension avoids it — but that's in direct tension with Pitfall 10's requirement to *add* `WS_THICKFRAME` for Snap. This is a real potential contradiction between "needs `WS_THICKFRAME` for Snap" and "avoid white strip via `WS_POPUP`" that needs to be resolved empirically on this project's actual target (Windows 11 24H2, not Windows 10) rather than assumed solved by copying either fix verbatim.

**How to avoid:** Test specifically for this artifact once `WS_THICKFRAME` + non-client-area removal are both in place; since the target OS is Windows 11 24H2 (not 10), this may simply not reproduce — treat the Windows-10-era reports as background context, not a certain bug, and verify rather than pre-emptively "fixing" a problem that may not exist on the actual target.

**Phase step:** Step 2, as a specific visual-capture check (zoom into the top edge pixel row via `PrintWindow` capture) once frame removal is done.

---

### Pitfall 24: Win11 rounded corners inherited-through-maximize inconsistency

**What goes wrong:** Windows 11 rounds the corners of top-level windows by default, but (per Microsoft's own guidance) automatically squares them back only for windows DWM tracks as maximized *the normal way* — a custom/borderless window may keep rounded corners even while visually "maximized" via this milestone's own maximize logic, since DWM's own maximize-tracking may not consider a `WM_NCCALCSIZE`-manipulated window "really" maximized in the same sense. This is a cosmetic-only issue (unlike Pitfalls 8–9) but affects the "looks native" goal.

**How to avoid:** Explicitly call `DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, DWMWCP_DONOTROUND)` while maximized and `DWMWCP_DEFAULT` (or `DWMWCP_ROUND`) while floating, driven by the same maximize/restore transition handling as Pitfall 15's state-sync work, rather than relying on DWM to infer it.

**Phase step:** Step 3 or 5 (whichever ends up owning maximize/restore transition handling) — bundle this cosmetic fix into that same transition-handling code path rather than treating it as separate work.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|--------------------|-----------------|------------------|
| Skip per-HWND keying, use one global "installed subclass" singleton | Faster to write/demo with one window | Breaks the moment a second native-chrome window opens (explicit multi-window requirement) — Pitfall 5 | Never for this milestone; the requirement is explicit |
| Handle `WM_NCHITTEST`/`WM_NCCALCSIZE` only, skip re-verifying subclass survival after AWT-triggering events | Simpler code, works in short demo sessions | Silent feature loss after real-world interaction sequences (dialogs, DnD, always-on-top) — Pitfall 4 | Only if Step 1 empirically proves AWT never re-touches `GWLP_WNDPROC` in this JDK build, and that finding is documented, not assumed |
| Leave `WindowDraggableArea` in place "just in case native hit-testing has gaps" | Feels like a safety net | Two competing drag-handling systems on overlapping pixels — Pitfall 15 | Never — remove it once native `HTCAPTION` covers the title bar, or explicitly prove it's inert first |
| Skip `--enable-native-access` / JNA version-pin documentation for a first pass | Ships faster | Confusing consumer-side warnings/crashes discovered post-release, harder to retrofit into six existing consumer apps | Never — this is a one-line README addition alongside existing consumer-floor documentation (v3.1 precedent) |
| Assume 150%-DPI hit-test math is "probably fine" without a way to verify it | Avoids blocking on hardware the dev machine doesn't have | Ships an unverified DPI code path, echoing the exact gap pattern (SHW-16, VER-F02) this project has already had to name twice | Only if explicitly recorded as a named gap the way prior DPI gaps were — never silently |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|------------------|-------------------|
| AWT's native Windows peer | Assuming the JNA subclass, once installed, stays installed for the window's whole life | Re-verify `GWLP_WNDPROC` ownership after AWT-triggering events; re-chain to the *current* proc, not a stale saved one (Pitfall 4) |
| Compose `WindowState` | Assuming `windowState.placement` reflects OS reality after adding native Snap/maximize paths | Push OS-observed maximize/restore/minimize transitions into `windowState.placement` explicitly from the WndProc side (Pitfall 15) |
| JBR (showcase/Hot Reload only) | Assuming a Hot-Reload-verified behavior generalizes to the published (standard-JDK) library | Verify cold `./gradlew run` (or an equivalent non-JBR run) exhibits the same behavior as the Hot Reload session before trusting the MCP-driven check (Pitfall 16) |
| Consumer apps' existing JNA/jna-platform 5.19.1 | Publishing with a different pinned JNA version "because it's newer" | Pin to the consumer's already-used 5.19.1 exactly (Pitfall 22) |
| DWM (`DwmDefWindowProc`, `DwmSetWindowAttribute`) | Hand-rolling caption-button hit-testing/painting without giving `DwmDefWindowProc` first refusal | Pass `WM_NCHITTEST` to `DwmDefWindowProc` first for caption-button regions, per Microsoft's own guidance (Pitfall 11) |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|-----------------|
| Computing hit-test regions (layout traversal, DPI conversion) fresh on every `WM_NCHITTEST` call | Sluggish/stuttery hover feedback during fast mouse movement across the title bar, since `WM_NCHITTEST` fires on nearly every mouse-move tick | Publish a pre-computed snapshot from Compose's own layout pass (Pitfall 6's structure) and have the WndProc do only cheap point-in-rect lookups against it | Noticeable once the title bar has several interactive regions (buttons + `leading` slot) and the narrow ~300px window makes regions tightly packed |
| Stacked WndProc chains from repeated Hot Reload re-installs (Pitfall 16) | Progressively slower hit-test/hover response the longer a Hot Reload session runs | Idempotent install guard keyed by HWND | Becomes visible after several hot-reload edits in one session; easy to miss in a short test |

## "Looks Done But Isn't" Checklist

- [ ] **Snap Layouts flyout:** A correct `WM_NCHITTEST` → `HTMAXBUTTON` answer (provable via `SendMessage`) does **not** by itself prove the flyout actually appears — the flyout is a DWM/shell-owned visual that also depends on `WS_MAXIMIZEBOX` being set and Windows 11's Snap Layouts being enabled in Settings; verify the actual flyout, not just the hit-test answer (Pitfall 11, and Verification section below).
- [ ] **Maximize covers the taskbar:** Passing a visual "looks maximized, taskbar visible" check at 100% DPI with auto-hide does not prove the auto-hide *hover-to-reveal* interaction still works — that specifically requires the `SHAppBarMessage`/`ABM_GETAUTOHIDEBAR` state check or an actual (banned) mouse-hover test; don't conflate "not visibly covering" with "hover-reveal still works" (Pitfall 9).
- [ ] **Multi-window support:** Working correctly with one native-chrome window open does not prove multi-window correctness — explicitly test 2+ open simultaneously, including opening a second while the first is mid-drag, and the narrow ~300px window specifically (Pitfall 5).
- [ ] **`WindowState.placement` accuracy:** The title bar's maximize/restore icon looking correct after a **Compose-driven** click does not prove it stays correct after a **native** Snap/Win+Up/double-click-caption transition — these are different code paths that must each be separately verified (Pitfall 15).
- [ ] **Accessibility:** Mouse-driven verification of the maximize button does not prove screen-reader/UIA exposure still works once its click-handling moved to the non-client path (Pitfall 21).
- [ ] **541-test count guard:** Adding new pure-JVM unit tests for hit-test math (which can and should be tested without any native window at all) is necessary but not sufficient — the actual native behavior (Snap, taskbar, DPI) has no unit-testable surface and needs the capture/inspection-based verification described below; don't let a green test suite substitute for that.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|----------------|-----------------|
| JNA callback GC crash (Pitfall 1) | LOW once diagnosed, but diagnosis itself is HIGH (native crash, no stack trace) | Reproduce via forced `System.gc()` during active drag; once confirmed, fix is a straightforward field-lifetime change — the cost is almost entirely in recognizing the symptom, not fixing it |
| `WS_THICKFRAME`-vs-`WM_NCCALCSIZE` overhang (Pitfall 8) | LOW | Well-documented fix (inset by DPI-scaled frame thickness on maximize); Windows Terminal's own source is a direct reference implementation |
| Auto-hide taskbar swallowed (Pitfall 9) | MEDIUM | Requires the `SHAppBarMessage`/`ABM_GETAUTOHIDEBAREX` detection path to be added if skipped initially — not just a one-line tweak, but a bounded, well-scoped addition |
| `WindowState` desync (Pitfall 15) | MEDIUM | Requires threading OS-observed transitions back into Compose state via the same publish mechanism used for hit-testing — architecturally straightforward once Pitfall 6's snapshot structure exists, expensive to retrofit if that structure wasn't built generally enough from the start |
| Hot Reload double-subclass (Pitfall 16) | LOW | Add the idempotent-install guard; low cost, but easy to miss until a multi-edit Hot Reload session is specifically tested |
| DPI hit-test math wrong at 150% (Pitfall 13) | HIGH to fully fix without hardware; MEDIUM to properly document as a gap | If no 150% monitor/simulation is available, the honest recovery is naming the gap explicitly (as v3.0/v3.1 already did for DPI runs), not silently shipping unverified |

## Pitfall-to-Phase Mapping

| Pitfall | Phase Step | Verification |
|---------|------------|---------------|
| 1. Callback GC crash | Step 1 | Forced-GC-during-drag stress test; must not crash |
| 2. `SetWindowLong` vs `Ptr` truncation | Step 1 | Code-review grep gate for bare `SetWindowLong(`/`GetWindowLong(` |
| 3. Missing `CallWindowProc` chaining | Step 1 | IME/paint/DnD/focus smoke check on the showcase after subclass install |
| 4. AWT re-subclassing | Step 1 + Step 5 | Periodic `GetWindowLongPtr` self-check the verification step can assert on |
| 5. Unsubclass order / multi-window | Step 1 + Step 6 | Multi-window open/close test incl. close-during-drag repro |
| 6. Cross-thread state read from WndProc | Step 1 | Stress test hovering/clicking interactive title-bar content while resizing |
| 7. Uncaught exceptions in callback | Step 1 + Step 6 | Fault-injection test proving fallback-to-DefWindowProc engages |
| 8. `WM_NCCALCSIZE` maximize overhang | Step 2 | Visual capture of maximized state at 100% (and 150% if available) DPI |
| 9. Auto-hide taskbar swallowed | Step 4 | `SHAppBarMessage`/`ABM_GETAUTOHIDEBAR` state check pre/post maximize (dev machine's real auto-hide-ON condition) |
| 10. Missing `WS_THICKFRAME`/style bits | Step 1 (style fixup) | `GetWindowLongPtr(GWL_STYLE)` read-back confirms bits before hit-test debugging begins |
| 11. `HTMAXBUTTON` click/paint/flyout | Step 3 | `WM_SYSCOMMAND` verification + actual Snap Layouts flyout appearance, not just hit-test answer |
| 12. `WM_NCMOUSELEAVE` re-arm | Step 3 | Fast mouse-in/out-of-button sequence, hover state must clear |
| 13. Physical-pixel/DPI hit-test math | Step 4 | Named gap if no 150% monitor available; otherwise real 150% capture |
| 14. `WM_DPICHANGED` during drag | Step 4 | Named gap (cannot exercise on single-monitor dev machine) unless simulated |
| 15. `WindowState` desync + `WindowDraggableArea` removal | Step 5 | Native Snap/Win+Up transition reflected in title-bar icon; `WindowDraggableArea` removed and drag re-verified |
| 16. JBR / Hot Reload double-subclass | Step 1 (guard) + Step 6 | Cold run vs. Hot Reload multi-edit session compared |
| 17. `transparent=true` temptation | Step 2 (guard-rail, no code) | Code review: no `transparent=true`/`WS_EX_LAYERED` introduced |
| 18. Resize flicker exposure | Step 6 | `PrintWindow` capture during fast native-border drag, compared to existing Compose-driven-resize baseline |
| 19. `leading`-slot vs caption boundary | Step 3 | Interactive `leading` content clickable; rest of bar still drags |
| 20. Alt+Space Move/Size | Step 6 | Keyboard-only system-menu Move/Size checklist item |
| 21. UIA exposure of custom max button | Step 3 / Step 6 | Compose semantics role/description verified independent of visual test |
| 22. JNA packaging/version pin | Step 1 (first commit) | Dependency pinned to 5.19.1; README note added |
| 23. Stray white top border | Step 2 | Zoomed `PrintWindow` capture of top edge row |
| 24. Rounded-corner maximize inconsistency | Step 3 / 5 | `DWMWA_WINDOW_CORNER_PREFERENCE` toggled with maximize/restore transitions |

## Verification: What Can and Cannot Be Proven Without Moving the Real Cursor/Keyboard

This project's locked rule (STATE.md) bans synthetic mouse/keyboard input into the live app and bans anything but `PrintWindow(hwnd, hdc, 2)` for capture. That constrains what this milestone's own verification step can prove:

**Provable without synthetic input, via direct Win32 queries against the running process's own HWND:**
- `SendMessage(hwnd, WM_NCHITTEST, 0, MAKELPARAM(x,y))` for arbitrary (x,y) — proves the hit-test **answer** is correct for a given point, without moving the cursor there.
- `PostMessage`/`SendMessage(hwnd, WM_SYSCOMMAND, SC_MAXIMIZE/SC_RESTORE/SC_MINIMIZE, 0)` — proves the window responds correctly to the same commands a real Snap Layouts choice or system-menu action would send, without the flyout UI itself.
- `GetWindowRect`/`GetClientRect` vs. `MonitorInfo.rcWork`/`rcMonitor` — proves maximize geometry is correct (Pitfall 8/9) by comparison, without visual inspection.
- `GetWindowLongPtr(hwnd, GWL_STYLE)` — proves `WS_THICKFRAME`/`WS_MAXIMIZEBOX`/`WS_MINIMIZEBOX` bits are actually set (Pitfall 10).
- `SHAppBarMessage(ABM_GETSTATE/ABM_GETAUTOHIDEBAREX, ...)` — proves auto-hide-taskbar detection logic identifies the right edge, independent of whether the taskbar visually reveals itself.
- `GetWindowLongPtr(hwnd, GWLP_WNDPROC)` read-back — proves subclass install/survival/restoration state (Pitfalls 1, 4, 5, 16).
- `PrintWindow` captures compared before/after a `SendMessage`-driven state change (e.g. before/after `SC_MAXIMIZE`) — proves the **visual result** of a state transition without a live mouse drag.

**NOT provable this way — false-positive risk if treated as sufficient:**
- **A correct `HTMAXBUTTON` hit-test answer proves nothing about the Snap Layouts flyout actually appearing.** The flyout is rendered by the shell/DWM in response to a real, sustained mouse-hover — there is no documented message-based way to trigger it programmatically. This must either be verified by the maintainer directly (a real mouse hover, which the project's rules reserve for human sign-off, not agent-driven synthetic input) or explicitly named as an unverified-by-the-agent gap, the same way v3.0 named its DPI gaps.
- **Live drag-to-edge Snap preview** (the translucent preview rectangle shown while dragging toward a screen edge) — same reasoning; no message-based trigger exists for this purely visual, hover/drag-duration-dependent shell feature.
- **Auto-hide taskbar's actual hover-to-reveal behavior** — `SHAppBarMessage` state queries prove the *detection* logic is correct, but not that a real user's mouse hovering at the bottom edge will reveal it; this final link needs either maintainer verification or is a named gap.
- **`WM_DPICHANGED`-during-drag behavior** (Pitfall 14) — cannot be exercised at all without a second, differently-scaled monitor or a physically-triggered DPI change; on this project's single-monitor dev machine, this is provably a gap, not a "should have tested more" oversight.

**Building a guard that provably FAILS on the old `WindowDraggableArea`-only implementation** (per this project's own locked "a regression guard must provably fail on unfixed code" rule): before writing the new hit-testing code, first write the `SendMessage(hwnd, WM_NCHITTEST, ...)` check against the **current, unmodified** `AeroTitleBar`/`AeroResizeHandles` code and confirm it returns `HTCLIENT` (or whatever the unmodified AWT default answers) rather than `HTCAPTION`/`HTTOP`/etc. across the title bar and resize-zone regions — proving the guard actually distinguishes "native hit-testing present" from "native hit-testing absent," the same discipline this project applied to its RCMP/TOOL-16 regression guards in prior milestones.

## Sources

- Microsoft Learn — `WM_NCHITTEST` (HIGH, official): https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest
- Microsoft Learn — `WM_NCCALCSIZE` (HIGH, official): https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-nccalcsize
- Microsoft Learn — `WM_NCMOUSELEAVE` (HIGH, official): https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-ncmouseleave
- Microsoft Learn — `SetWindowLongPtrA` / `CallWindowProcA` (HIGH, official): https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowlongptra , https://learn.microsoft.com/en-us/windows/desktop/api/winuser/nf-winuser-callwindowproca
- Microsoft Learn — "Apply snap layout menu" (HIGH, official): https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-snap-layout-menu
- Microsoft Learn — "Apply rounded corners in desktop apps for Windows 11" (HIGH, official): https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-rounded-corners
- Microsoft Learn — `WM_DPICHANGED_BEFOREPARENT` (HIGH, official): https://learn.microsoft.com/windows/win32/hidpi/wm-dpichanged-beforeparent
- Microsoft Learn — `DwmExtendFrameIntoClientArea` / "Custom Window Frame Using DWM" (HIGH, official): https://learn.microsoft.com/en-us/windows/win32/dwm/customframe
- Windows Terminal — `NonClientIslandWindow.cpp` (HIGH, real Microsoft production source, read directly): https://github.com/microsoft/terminal/blob/main/src/cascadia/WindowsTerminal/NonClientIslandWindow.cpp
- Windows Terminal issues #13679, #9443 (MEDIUM, corroborating real-world reports): https://github.com/microsoft/terminal/issues/13679 , https://github.com/microsoft/terminal/issues/9443
- `kelpie` PR #140 (MEDIUM, third-party OSS, technically precise fix matching Terminal's approach): https://github.com/UnlikeOtherAI/kelpie/pull/140
- JNA — `CallbacksAndClosures.md` (HIGH, official JNA docs): https://github.com/java-native-access/jna/blob/master/www/CallbacksAndClosures.md
- JNA issues #1711, #1665 (MEDIUM, JNA's own tracker, `--enable-native-access` warnings): https://github.com/java-native-access/jna/issues/1711 , https://github.com/java-native-access/jna/issues/1665
- OpenJDK JEP 472 (HIGH, official): https://openjdk.org/jeps/472 ; Oracle "Restricted Methods" (HIGH, official): https://docs.oracle.com/en/java/javase/22/core/restricted-methods.html
- OpenJDK `awt_Frame.cpp` (HIGH, primary source, confirms undecorated-frame native style bits): https://github.com/openjdk/jdk/blob/master/src/java.desktop/windows/native/libawt/windows/awt_Frame.cpp
- OpenJDK bug tracker — JDK-8286581, JDK-8055453 (MEDIUM, DPI-awareness history on Windows): https://bugs.openjdk.org/browse/JDK-8286581 , https://bugs.openjdk.org/browse/JDK-8055453
- JetBrains/compose-multiplatform issues #1248, #2062, #3625 (MEDIUM, confirms Aero Snap / placement-gating gaps are open, unsolved upstream): https://github.com/JetBrains/compose-multiplatform/issues/1248 , https://github.com/JetBrains/compose-multiplatform/issues/2062 , https://github.com/JetBrains/compose-multiplatform/issues/3625
- JetBrains YouTrack CMP-7700, CMP-7919 (MEDIUM, search-snippet-sourced, not independently re-fetched in full this pass): https://youtrack.jetbrains.com/issue/CMP-7700 , https://youtrack.jetbrains.com/issue/CMP-7919
- JetBrains/JetBrainsRuntime issue #362 (MEDIUM, confirms JBR's own custom-decoration machinery and real conflicts): https://github.com/JetBrains/JetBrainsRuntime/issues/362
- winit issue #3884 (MEDIUM, independent confirmation of `HTMAXBUTTON` requirement from another native-window Rust project): https://github.com/rust-windowing/winit/issues/3884
- FlatLaf window decorations / Windows 11 Snap Layouts issue #407 (MEDIUM, JVM-ecosystem precedent for exactly this feature, though implemented via a native DLL rather than JNA): https://www.formdev.com/flatlaf/window-decorations/ , https://github.com/JFormDesigner/FlatLaf/issues/407
- Firefox Bugzilla 642851 (MEDIUM, independent cross-project confirmation of the auto-hide-taskbar-maximize bug class): https://bugzilla.mozilla.org/show_bug.cgi?id=642851
- This repository (HIGH, primary/first-party): `AeroTitleBar.kt`, `ResizeHandles.kt`, `Main.kt`, `PROJECT.md`, `STATE.md`
- `~/.claude` project memory (HIGH, first-party, maintainer-recorded prior lessons directly referenced in the milestone context — JBR/Hot Reload double-subclass risk, `PrintWindow`-only capture rule, regression-guard-must-fail-first discipline)

---
*Pitfalls research for: aero-compose-ui v3.2.0 — native Windows window management (Aero Snap / Snap Layouts / hit testing) for undecorated Compose Desktop windows*
*Researched: 2026-09-25*
