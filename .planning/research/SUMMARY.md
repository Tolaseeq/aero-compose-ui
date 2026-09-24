# Project Research Summary

**Project:** aero-compose-ui
**Milestone:** v3.2 Native Window Behavior (release `v3.2.0`)
**Domain:** Native Win32 window-management integration (Aero Snap, Windows 11 Snap Layouts, hit-testing, taskbar-aware maximize) for an undecorated Compose Desktop window, on a standard JDK 21 (no JBR dependency), via JNA
**Researched:** 2026-09-25
**Confidence:** MEDIUM-HIGH

## Executive Summary

Every behavior on the maintainer's list -- Aero Snap, Snap Layouts flyout, Win+Arrow, shared-border resize, Snap Groups, taskbar-aware maximize, 100%/150% DPI moves, multi-window support, FancyZones -- is a stock Win32/DWM feature any normal top-level window gets automatically. Windows doesn't know the window is "Compose." The job is entirely: (1) put back the native window styles `undecorated = true` strips out, (2) answer `WM_NCHITTEST`/`WM_NCCALCSIZE`/`WM_GETMINMAXINFO` correctly via a JNA-based window-procedure subclass, and (3) forward the maximize button's clicks through the non-client path instead of Compose's `clickable`. Once that is right, Snap, Snap Layouts, Snap Groups, shared-border resize, Aero Shake, Win+arrow, Alt-Tab thumbnails, drop shadow and Win11 rounded corners all follow for free -- nothing to build, only to unlock. The real engineering is the JNA subclass itself (callback lifetime, thread-safety, per-HWND lifecycle), the maximize-button hit-test/interaction plumbing, and taskbar-aware maximize, none of which Compose Desktop's own undecorated-window maximize gets right by default.

The maintainer's assumed approach (JNA WndProc subclass; `WM_NCHITTEST` -> `HTCAPTION`/`HTMAXBUTTON`/resize codes/`HTCLIENT`; frame removal via `WM_NCCALCSIZE`) is correct and matches every independent prior-art implementation found (FlatLaf/Swing on a standard JDK, Windows Terminal, a minimal C reference, Flutter/WPF/Electron by extension). It needs one factual correction before planning proceeds (see below): the brief assumed `WS_THICKFRAME`/`WS_MAXIMIZEBOX`/`WS_MINIMIZEBOX` survive `undecorated = true` and only need to be "kept." Reading OpenJDK's actual `awt_Frame.cpp` shows the undecorated native style is `WS_POPUP | WS_SYSMENU | WS_CLIPCHILDREN | WS_MAXIMIZEBOX | WS_MINIMIZEBOX` -- `WS_THICKFRAME` and `WS_CAPTION` are both **absent**, not merely hidden. Both must be added back explicitly via `SetWindowLongPtr(GWL_STYLE, ...)` before any hit-test/frame-removal logic can do anything -- this is a load-bearing first sub-step, not an afterthought.

The main risk is not "will Snap work" (very likely yes, given four independent prior-art convergences) but **precise Win32-message-recipe correctness on `ComposeWindow`/Skiko specifically**, which no one has documented for Compose Desktop before (two open, unanswered JetBrains issues, CMP-6039/CMP-6031, confirm this). All four research files converge on the same architecture and the same message set, but disagree on several implementation-level specifics that only an empirical spike on this exact stack can resolve -- see "Conflicts to settle empirically" below. The milestone is scoped as a single phase with an existential-risk spike first, exactly matching the maintainer's stated plan.

## Key Findings

### Recommended Stack

Add `net.java.dev.jna:jna` and `net.java.dev.jna:jna-platform`, both pinned to **5.19.1** -- the current Maven Central release and, critically, the exact version the primary consumer (Pinya) already uses, eliminating version-skew/native-dispatch-clash risk. No native toolchain, no `.dll` to compile/sign (unlike FlatLaf's own C++ approach) -- pure JNI dlopen of the OS's existing `user32.dll`/`dwmapi.dll`. Both jars scope as `implementation` (never `api`) -- no `com.sun.jna.*` type may appear in any public `AeroTitleBar`/`AeroWindowChrome` signature, mirroring the existing `kotlinx-coroutines-core` precedent in `library/build.gradle.kts`.

**Core technologies:**
- `net.java.dev.jna:jna:5.19.1` -- JNI runtime (`Pointer`, `Native`, `Callback`) -- required transitively by jna-platform, pin explicitly
- `net.java.dev.jna:jna-platform:5.19.1` -- pre-built `User32`/`WinDef`/`WinUser` Win32 bindings -- covers most needed calls; `GetDpiForWindow`, `GetSystemMetricsForDpi`, `TrackMouseEvent`, `Dwmapi`, and (unverified, low-confidence) `GetSystemMenu`/`TrackPopupMenu` must be hand-declared as extension interfaces since they're absent from the shipped bindings
- No `--enable-native-access` warning expected on JDK 21 (JNI-only through 5.19.x; the warning/restriction regime is JDK 24+/JEP 472) -- re-verify if the toolchain ever moves past JDK 24

### Expected Features

**Must have (table stakes -- the maintainer's explicit bullet list, all free once styles+subclass are right):**
- Edge/corner Aero Snap + drag-away restore, Win+Arrow snap, double-click-title maximize/restore, Alt+Space system menu, taskbar-aware maximize (the one thing Java's own undecorated maximize is known to get wrong), 100%/150% DPI multi-monitor moves, multi-window support with interactive title-bar content (Pinya's narrow queue window), Snap Layouts flyout on maximize-button hover, shared-border resize of a snapped pair, Snap Groups in the taskbar, FancyZones pickup

**Should have (differentiators -- free consequences, worth naming so nobody "fixes" them away):**
- Aero Shake, Win+Shift+Arrow move-to-monitor, Win+D, "show on all desktops" -- all zero-code once `HTCAPTION` hit-testing is correct

**Explicitly do NOT build (anti-features):**
- Custom snapping/ghost-preview logic in Compose, a custom Compose popup reimplementing the Snap Layouts flyout, AppBar-style edge docking, a custom "pin to all desktops" UI, manual `WM_DPICHANGED` geometry math overriding AWT's own -- every one of these reinvents something the OS already does better and will drift from every future Windows UI refresh

### Architecture Approach

Two threads, one lock-free bridge each direction. Compose (EDT) publishes an immutable `HitTestSnapshot` (caption/button/interactive-region rects in client-area px) into an `AtomicReference` on every layout pass; the Toolkit thread's `AeroWndProc` reads it on `WM_NCHITTEST` with a plain volatile read -- no locks, no blocking. The only write in the opposite direction is a `SwingUtilities.invokeLater`-hopped boolean (hover/pressed) for the maximize button, feeding a small `AeroMaxButtonInteraction` state object that `TitleBarButton` reads instead of its own `collectIsHoveredAsState()`. Everything the subclass doesn't explicitly own is forwarded via `CallWindowProc` to AWT's original WndProc -- this is the single most load-bearing invariant in the whole design, since it's what keeps `WindowState`, focus, IME, and accessibility working.

**Major components:**
1. `NativeWindowChromeRegistry` (new, internal) -- per-HWND install/uninstall lifecycle, owns the strong `Callback` reference (GC-safety)
2. `AeroWndProc` (new, internal) -- the actual `WinUser.WindowProc`; handles `WM_NCHITTEST`/`WM_NCCALCSIZE`/non-client mouse/`WM_GETMINMAXINFO`, chains everything else
3. `HitTestRegionRegistry` (new, internal) -- `AtomicReference<HitTestSnapshot>` publisher/reader, the cross-thread bridge
4. `AeroTitleBar` / `AeroResizeHandles` (existing, modified) -- source-compatible additive API (`nativeWindowManagement: Boolean = true`, `markAeroTitleBarInteractive()`, `rememberAeroWindowChrome()`); `AeroResizeHandles` becomes a Windows-native no-op once native resize hit-testing is active

### Critical Pitfalls

1. **JNA callback GC'd mid-session -> native crash (JVM-fatal, no stack trace)** -- pin the `WindowProc` `Callback` as a `val` field on a manager object with HWND-scoped lifetime, never a `remember`/lambda; verify with a forced-`System.gc()`-during-drag stress test.
2. **`undecorated = true` does not yield `WS_THICKFRAME`; native Snap silently does nothing without it** -- confirmed by reading `awt_Frame.cpp` directly (see the correction above); read back `GetWindowLongPtr(GWL_STYLE)` before debugging any hit-test logic.
3. **`WM_NCCALCSIZE` naive "return 0" causes maximized-window taskbar overhang, and the fix for that (using `rcMonitor`) can in turn swallow the auto-hide taskbar's hover-reveal on the dev machine's actual configuration (auto-hide ON)** -- needs DPI-scaled frame-thickness inset when maximized, plus `SHAppBarMessage`/`ABM_GETAUTOHIDEBAREX`-based per-edge auto-hide detection with a small (Windows Terminal uses 2px) inset.
4. **Not chaining unhandled messages to AWT's original WndProc via `CallWindowProc`** -- silently breaks IME, focus, DnD, accessibility; structure the hook as "forward by default, intercept only the named few."
5. **`WindowState.placement` can desync from real OS state once maximize/restore/snap can happen natively (not just through Compose's own click handlers)**, and the existing `WindowDraggableArea` + `AeroResizeHandles` become second, competing input paths once native `HTCAPTION`/resize codes take over the same pixels -- `WindowDraggableArea` must be removed, not left "just in case."

## Conflicts to Settle Empirically

The four research files converge on the overall architecture and message set but disagree on several implementation specifics. Each must be resolved by a cheap, concrete first-hand check -- most are answerable headlessly (no synthetic cursor/keyboard) via `SendMessage`/`GetWindowLongPtr` against the real running HWND, per PITFALLS.md's own "provable without moving the real cursor" verification class.

1. **Which style bits `undecorated = true` actually yields on JDK 21.**
   FEATURES.md assumed `WS_CAPTION`/`WS_SYSMENU`/`WS_MINIMIZEBOX`/`WS_MAXIMIZEBOX` are *all* stripped. PITFALLS.md, having read OpenJDK's `awt_Frame.cpp` directly, found the actual style is `WS_POPUP | WS_SYSMENU | WS_CLIPCHILDREN | WS_MAXIMIZEBOX | WS_MINIMIZEBOX` (ext style 0) -- i.e. `WS_SYSMENU`/`WS_MAXIMIZEBOX`/`WS_MINIMIZEBOX` are already present; only `WS_CAPTION` and `WS_THICKFRAME` are missing.
   **Cheapest check:** `GetWindowLongPtr(hwnd, GWL_STYLE)` read-back on the current, unmodified showcase window before writing any new code -- zero risk, zero cursor/keyboard, first sub-step of Step 2 ("Window styles").

2. **Alt+Space: automatic once `WS_SYSMENU`/`WS_CAPTION` are present, or does it need explicit `WM_SYSCOMMAND`/`GetSystemMenu`/`TrackPopupMenu` handling?**
   STACK.md treats it as needing explicit interception (`GetSystemMenu`+`TrackPopupMenu`, both absent from `jna-platform`, hand-declaration needed). FEATURES.md calls it fully automatic ("if `WS_SYSMENU`/`WS_CAPTION` are present and unhandled `WM_SYSCOMMAND` reaches `DefWindowProc`"). PITFALLS.md notes `WS_SYSMENU` is *already* present on today's unmodified undecorated frame, implying Alt+Space may already open *a* system menu even before this milestone's changes -- the open question is only whether Move/Size items function correctly once hit-testing changes.
   **Cheapest check:** headless -- confirm `WS_SYSMENU` via the style read-back above (#1); the actual flyout/menu behavior (including whether Move/Size work) is a keyboard-only check, explicitly allowed without moving the cursor, and is the natural first item in the end-of-phase real-input session -- test it both on the unmodified baseline and after the subclass is installed, to isolate what the milestone's own code changed.

3. **Auto-hide taskbar inset size (1px vs 2px) and edge-detection method.**
   No source in this research set independently proposes 1px for the *auto-hide inset* specifically -- PITFALLS.md's only concrete number is Windows Terminal's 2px (`NonClientIslandWindow.cpp`, read directly). The "1px" figures that appear elsewhere in STACK.md/ARCHITECTURE.md are for an unrelated concern (`DwmExtendFrameIntoClientArea` shadow margins), not the auto-hide inset -- treat any 1px-for-auto-hide claim as unsupported by this research and default to the 2px, `SHAppBarMessage(ABM_GETSTATE/ABM_GETAUTOHIDEBAREX)`-driven, per-edge approach PITFALLS.md documents from Windows Terminal's real source.
   **Cheapest check:** `SHAppBarMessage(ABM_GETAUTOHIDEBAREX)` state query (headless) confirms which edge has an auto-hide bar; implement the 2px inset; verify hover-reveal in the maintainer's real-input session with auto-hide restored to ON afterward.

4. **Whether `WindowState.placement` syncs automatically (ARCHITECTURE.md) or needs explicit pushing (PITFALLS.md).**
   ARCHITECTURE.md claims that as long as `WM_SIZE` is forwarded via `CallWindowProc`, AWT's own extendedState -> `WindowStateListener` pipeline keeps `windowState.placement` correct for *all* maximize/restore transitions, including OS-native ones -- "no new sync code needed." PITFALLS.md (Pitfall 15) argues native Snap/maximize/Win+Up/double-click-caption transitions can leave `windowState.placement` stale relative to real OS state, and recommends explicitly pushing OS-observed transitions into `windowState.placement` from the WndProc side.
   **Cheapest check:** headless -- after the Step 1 spike installs `HTMAXBUTTON`, drive `SendMessage(hwnd, WM_SYSCOMMAND, SC_MAXIMIZE, 0)` and read `windowState.placement` from the JVM side; if it already flips correctly via the existing `WM_SIZE`-forwarding pipeline, ARCHITECTURE.md's simpler no-new-code claim wins and Pitfall 15's mitigation is unnecessary; if not, build the explicit push.

5. **Max-button interaction approach: FlatLaf-style forwarding of `WM_NCMOUSEMOVE`/`WM_NCLBUTTONDOWN`/`UP` into the client message stream, vs. a direct native->`State<Boolean>` EDT-hop bridge that suppresses Compose's own `clickable`/`hoverable` entirely.**
   STACK.md cites FlatLaf's actual approach as re-injecting non-client mouse messages as ordinary client messages so the underlying Compose button keeps using its normal `hoverable`/`clickable` pipeline unchanged. ARCHITECTURE.md (Pattern 3) and PITFALLS.md (Pitfall 11) instead specify suppressing Compose's own interaction handling for the maximize button and driving a dedicated `AeroMaxButtonInteraction` state purely from the WndProc via an EDT hop -- a different, arguably simpler design that doesn't require synthesizing input messages.
   **Cheapest check:** build the ARCHITECTURE-chosen `AeroMaxButtonInteraction` bridge first (cheaper, no message re-synthesis) and confirm hover/press visuals via the Compose Hot Reload MCP's semantic-tree/visual inspection (no real input needed); fall back to FlatLaf's message-forwarding approach only if the state-bridge approach demonstrably fails to match the other two buttons' feel.

6. **Whether `DwmExtendFrameIntoClientArea` / `DWMWA_WINDOW_CORNER_PREFERENCE` are needed at all.**
   STACK.md and FEATURES.md both treat these as optional/likely-unnecessary (shadow and rounded corners "should" be automatic once `WS_CAPTION` is kept). ARCHITECTURE.md flags LOW-MEDIUM confidence on rounded-corner necessity and says "verify visually." PITFALLS.md (Pitfall 24) goes further and predicts a probable real need: DWM's own maximize-tracking may not recognize a `WM_NCCALCSIZE`-manipulated window as "really" maximized, leaving corners incorrectly rounded while visually maximized, and recommends actively toggling `DWMWA_WINDOW_CORNER_PREFERENCE` on maximize/restore transitions.
   **Cheapest check:** `PrintWindow` capture comparison of corners/shadow before and after a headless `SC_MAXIMIZE`/`SC_RESTORE` toggle (no live drag needed) -- resolves this without any DWM-attribute code first; add the explicit toggle only if the capture shows the predicted rounding bug.

7. **Whether JBR's own custom-window-decoration machinery interferes with the new subclass under `:showcase`'s `hotRun` (JetBrains Runtime).**
   Only PITFALLS.md raises this (Pitfall 16) -- the other three files treat JBR/Jewel purely as an unrelated design reference, not a runtime-interference risk for the showcase itself. Not a contradiction between files, but an open question named by only one of the four and worth carrying forward explicitly.
   **Cheapest check:** grep the codebase for any existing `com.jetbrains.JBR`/`CustomWindowDecoration` usage (near-zero cost, confirms whether JBR's mechanism is even engaged today); then compare a cold `./gradlew run` against a `hotRun` session for behavioral drift once the Step 1 spike exists, and add an idempotent-install guard regardless (also needed for Hot Reload's own effect-rerun risk).

## Implications for Roadmap

Per the maintainer's explicit scope decision, this milestone is **one phase** (Phase 22), not several -- the steps below are ordered, separately-committed steps within that single phase, riskiest first, matching ARCHITECTURE.md's "Suggested Build Order" and PITFALLS.md's phase-step mapping (both research files independently converged on the same ordering).

### Phase 22: Native Window Behavior

**Rationale:** Existential risk (does `HTMAXBUTTON`/Snap Layouts even work on `ComposeWindow`/Skiko at all, on a standard JDK, no JBR?) must be resolved before investing in the rest of the design -- the maintainer's own instruction is "stop and report the conflict" if the spike fails. Everything else is ordered so that later steps depend on earlier ones being empirically proven, not assumed.

**Step 1 -- Spike (existential-risk gate).** On a throwaway `main()` or the real showcase window, on a standard JDK 21 (not JBR), install a minimal JNA `WindowProc` subclass returning `HTCAPTION` everywhere except one hardcoded rect returning `HTMAXBUTTON`. This is where conflicts #1 (style-bit read-back), #4 (`WindowState.placement` sync), and #7 (JBR grep) get their cheap headless checks. Hover the hardcoded rect in the maintainer's real-input session and confirm the Snap Layouts flyout actually opens -- a correct `HTMAXBUTTON` answer alone does **not** prove the flyout appears (it is a DWM/shell-owned visual with no queryable/message-based trigger); the flyout must be *observed*, e.g. via UI Automation/window enumeration during the real-input session, never via the banned full-screen capture method. If the flyout doesn't appear, stop and report before building anything further. Also wires the JNA callback-lifetime container (GC-safety) and the try/catch-and-fall-back-to-`DefWindowProc` wrapper as structural invariants from the start, not an afterthought.

**Step 2 -- Window styles.** Add back `WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX` via `SetWindowLongPtr(GWL_STYLE, ...)` + `SetWindowPos(..., SWP_FRAMECHANGED)` -- the correction to the maintainer's brief: the unmodified undecorated frame already has `WS_SYSMENU`/`WS_MAXIMIZEBOX`/`WS_MINIMIZEBOX` (confirmed by reading `awt_Frame.cpp` directly), but is missing both `WS_CAPTION` and `WS_THICKFRAME` -- without `WS_THICKFRAME` in particular, native Snap simply does nothing regardless of how correct the message handlers are. Verify via `GetWindowLongPtr` read-back before any hit-test debugging.

**Step 3 -- Frame removal.** `WM_NCCALCSIZE`: return 0 for the floating state; for maximized, inset by the DPI-scaled frame thickness (`GetSystemMetricsForDpi(SM_CXFRAME/SM_CYFRAME, SM_CXPADDEDBORDER)` at the window's *current* DPI) to avoid the classic overhang bug, then further inset by a 2px auto-hide-taskbar margin on any edge `SHAppBarMessage` reports as auto-hidden (conflict #3) -- verify "never covers the taskbar," visible or auto-hide, before moving on.

**Step 4 -- Region registry + real hit-testing.** `HitTestRegionRegistry`/`HitTestSnapshot`, `onGloballyPositioned` wiring in `AeroTitleBar` for caption/button/interactive-content (`leading`) rects; replace Step 1's hardcoded rect with the live one via `ScreenToClient`. Window position never enters the calculation (anti-pattern: hardcoded/cached rects from tracked window position).

**Step 5 -- Maximize-button interaction bridge.** `TrackMouseEvent(TME_NONCLIENT | TME_LEAVE)`, re-armed on every `WM_NCMOUSEMOVE` (one-shot tracking gotcha), feeding `AeroMaxButtonInteraction` via an EDT hop -- resolve conflict #5 here (state-bridge approach first, FlatLaf-style message-forwarding only as a fallback). Suppress Compose's own `hoverable`/`clickable` for this one button only; swallow `WM_NCLBUTTONDOWN`/`UP` at `HTMAXBUTTON` rather than letting `DefWindowProc` paint a classic button.

**Step 6 -- "Don't break what works" checkpoint.** Audit that every unhandled message reaches `CallWindowProc`; re-run the existing 541-test suite; manually confirm `WindowState.placement`/`size`/`position`, focus, minimize animation, and Alt+Space are unchanged from before this phase (resolves conflict #2's remaining open half -- Move/Size via the keyboard-only system menu, explicitly distinct from mouse-driven checks).

**Step 7 -- `AeroResizeHandles` Windows no-op + Win32 edge/corner classification.** Once `WM_NCHITTEST` correctly answers resize codes, the existing Compose-side `pointerInput` resize handlers on the same physical pixels become a competing input path, not an additive one -- gate the composable's early-return on "native chrome active" (Windows) and leave it unchanged elsewhere (non-Windows). Move resize-border thickness to `GetSystemMetricsForDpi`; verify against the narrow ~300px second window specifically.

**Step 8 -- Public API surface.** `nativeWindowManagement: Boolean = true` opt-out, `markAeroTitleBarInteractive()`, `rememberAeroWindowChrome()` -- all additive, source-compatible; verify multi-window (main + narrow queue window, both native-chrome-active simultaneously, including open-second-while-first-is-mid-drag and close-during-active-drag).

**Step 9 -- DPI/multi-monitor + taskbar pass.** 100%/150% scaling (using the temporarily installed virtual display driver), drag across monitors of different scale, snapped-pair shared border, Snap Groups smoke check, FancyZones smoke check (installed for this check), visible-taskbar case (auto-hide temporarily disabled). Default policy for `WM_DPICHANGED`: forward-only to AWT's original proc, do not intercept, unless the DPI-move test specifically proves AWT's own handling insufficient for this subclassed window.

**Step 10 -- Tests + proof + release plumbing.** Headless unit tests (region registry, hit-test classification math, DPI-thickness formula -- pure-JVM, part of the locked test-count guard); a live-window opt-in harness (`SendMessage`/`GetWindowRect`/`GetMonitorInfo` against the real HWND, promoted from Step 1's spike script); showcase wiring; bump the test-count guard with a commit explaining why; capture-based before/after visual proof via `tools/capture/`. Then, and only then, the single maintainer-supervised real-input session covering everything that has no message-based trigger: Snap Layouts flyout hover, drag-to-edge preview, Aero Shake, Win+Arrow/Win+Shift+Arrow, Alt+Space Move/Size, double-click maximize, FancyZones drop, the 150% virtual-monitor drag, and the visible-taskbar case -- the agent warns the maintainer not to touch the PC beforehand, waits for "ok," drives the session, and announces when done. Close with README window-behavior section, KDoc "Aero Snap limitation" removal, `v3.2.0` release.

### Phase Ordering Rationale

- Styles-and-subclass plumbing (Steps 1-2) must land before any individual OS behavior can be verified -- `WM_NCCALCSIZE`/`WM_NCHITTEST` are inert on a bare `WS_POPUP` window.
- Frame removal and hit-testing (Steps 3-4) come before the maximize-button interaction bridge (Step 5) because the button's rect only means anything once real region reporting exists.
- The "don't break what works" checkpoint (Step 6) is placed immediately after the core native-chrome mechanics are functionally correct, deliberately before `AeroResizeHandles` is touched -- regressions in `WindowState`/focus/IME must be caught before a second subsystem (resize handles) is modified on top of them.
- DPI/multi-monitor/taskbar work (Step 9) is placed last among the functional steps because it needs the temporary virtual-display-driver setup -- expensive to set up and tear down, so it should run once, after everything it might need to re-verify is already stable.
- The real-input session (end of Step 10) is last by the maintainer's own explicit constraint: it is the only step needing real mouse/keyboard, and everything provable headlessly should be proven headlessly first so the real-input session only needs to confirm the small remaining set of purely-visual, non-message-triggerable behaviors (the Snap Layouts flyout, Aero Shake, Snap Assist, FancyZones drop).

### Research Flags

Since this is a single phase, "needs deeper research" translates to: which steps should get a `/bm:plan-phase --research-phase 22` pass (or an equivalent focused re-check) during planning, versus which steps can proceed directly from this research.

**Needs a focused re-check during planning (the conflicts above are not yet resolved):**
- Steps 1-2 (style bits, spike outcome, Alt+Space baseline) -- conflicts #1, #2, #4, #7
- Step 3 (auto-hide inset size/detection) -- conflict #3
- Step 5 (max-button interaction approach) -- conflict #5
- Step 6/9 (rounded-corner/shadow necessity) -- conflict #6

**Standard, well-documented patterns (proceed directly, low research risk):**
- Step 4 (region registry -- a well-understood cross-thread immutable-snapshot pattern, no disagreement across sources)
- Step 7 (resize-handle gating -- the conflict here is a design decision already made, not an open research question)
- Step 8 (public API shape -- already spec'd in ARCHITECTURE.md with HIGH confidence on source-compatibility, MEDIUM only on binary-compatibility, which is a build/test concern not a research one)
- Step 10's headless-test and capture-based proof mechanics (directly reuses this project's existing, proven tooling -- `tools/capture/`, the locked test-count-guard pattern)

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM-HIGH | Library facts (versions, licenses, jar sizes, JPMS/JDK-21 non-impact) are HIGH, verified live against Maven Central and JNA's own GitHub source. The exact Win32-message recipe is MEDIUM -- well-attested in Swing/AWT prior art (FlatLaf) and a minimal C reference, but not yet proven against `ComposeWindow`/Skiko specifically; that is exactly what Step 1's spike exists to close. |
| Features | HIGH for the Win32/DWM mechanisms themselves (Microsoft Learn, current pages). MEDIUM for how those mechanisms interact with an AWT/Compose-Desktop HWND specifically -- no official JetBrains documentation exists; conclusions are corroborated by convergent prior art across five independent toolkits, plus two open unresolved JetBrains issues confirming Compose Desktop has no built-in answer. |
| Architecture | MEDIUM-HIGH | Win32 message contracts and the prior-art code they're modeled on (FlatLaf, Windows Terminal, `melak47/BorderlessWindow`) are HIGH confidence, directly read. Several Compose/Skiko/AWT interaction points (the `WindowState` sync claim, corner-preference necessity, fullscreen/peer-recreation edge case) are explicitly flagged MEDIUM or LOW and named for the Step 1 spike to confirm rather than assumed. |
| Pitfalls | MEDIUM-HIGH | Win32 mechanics are HIGH (Microsoft Learn + Windows Terminal's real production source, read directly). JNA-specific wiring (callback GC, thread-affinity) is MEDIUM (official JNA docs + analogous JVM prior art). Compose/CMP-specific interaction points are MEDIUM (JetBrains issue trackers; no first-party "how to add Aero Snap to Compose Desktop" doc exists anywhere). |

**Overall confidence:** MEDIUM-HIGH -- the architecture, message set, and pitfall catalogue are unusually well cross-corroborated for a niche integration (four independent research passes converged on the same core design), but a real, specific set of implementation details (the seven conflicts above) remain genuinely unresolved by desk research and require Step 1's spike before the rest of the phase can be built with confidence.

### Gaps to Address

- **All seven items in "Conflicts to settle empirically"** -- each has a named cheapest first-hand check; none should be silently assumed one way or the other during planning.
- **Windows 10 behavior** -- no Windows 10 machine available; per the maintainer's own decision, this goes on an explicit "unconfirmed" list in the final report, following the project's existing `21-UNCONFIRMED.md` precedent, rather than being tested or silently assumed identical to Windows 11.
- **`GetSystemMenu`/`TrackPopupMenu` presence in `jna-platform` 5.19.1** -- STACK.md flags this as LOW-MEDIUM confidence, not exhaustively checked; re-verify during Step 1/2 before assuming hand-declaration is or isn't needed.
- **Binary-compatibility of the new default `nativeWindowManagement` parameter on `AeroTitleBar`** -- source-compatibility is HIGH confidence, but ARCHITECTURE.md flags actual binary-compatibility (relevant if any consumer links against an old compiled JAR rather than recompiling from JitPack source) as unverified; worth a real external-consumer recompilation check before treating the API contract as fully proven.
- **Fullscreen toggle / native-peer recreation** -- explicitly out of this milestone's target feature list, but ARCHITECTURE.md flags it as a "must not crash if it happens" edge case for `NativeWindowChromeRegistry`'s install/uninstall lifecycle design; not a blocking gap, but should be a design consideration, not silently ignored.
- **`WM_DPICHANGED`-during-active-drag between differently-scaled monitors** -- cannot be exercised without the temporary virtual-display setup; the maintainer has authorized that setup for this milestone specifically, so this should move from "gap" to "verified" during Step 9, not remain a named gap by default.

## Sources

### Primary (HIGH confidence)
- Microsoft Learn -- `WM_NCHITTEST`, `WM_NCCALCSIZE`, `WM_SYSCOMMAND`, `WM_GETMINMAXINFO`, `WM_NCMOUSELEAVE`, `SetWindowLongPtrA`, `CallWindowProcA`, `DwmExtendFrameIntoClientArea`, "Apply snap layout menu," "Apply rounded corners in desktop apps for Windows 11," `DWM_WINDOW_CORNER_PREFERENCE`, `WM_DPICHANGED_BEFOREPARENT`
- `https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/maven-metadata.xml` -- live-fetched version confirmation
- `https://github.com/java-native-access/jna` (source + `CallbacksAndClosures.md` + `LICENSE`) -- read directly
- `https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-natives/flatlaf-natives-windows/src/main/cpp/FlatWndProc.cpp` -- read directly, strongest single implementation reference
- `https://github.com/microsoft/terminal/blob/main/src/cascadia/WindowsTerminal/NonClientIslandWindow.cpp` -- read directly, real Microsoft production source
- `https://github.com/openjdk/jdk/blob/master/src/java.desktop/windows/native/libawt/windows/awt_Frame.cpp` -- read directly, source of the style-bit correction
- `https://github.com/melak47/BorderlessWindow` -- read directly, canonical minimal reference implementation
- This repository: `AeroTitleBar.kt`, `ResizeHandles.kt`, `Main.kt`, `PROJECT.md`, `STATE.md` -- read directly

### Secondary (MEDIUM confidence)
- `https://github.com/JetBrains/compose-multiplatform/issues/1248`, `#2062`, `#3625` and YouTrack CMP-6039/CMP-6031 -- confirm Compose Desktop has no built-in Aero Snap solution, open/unanswered
- `https://github.com/rust-windowing/winit/issues/3884`, `https://github.com/Nihmar/Niman/issues/169`, `https://github.com/microsoft/terminal/issues/9443` / `#13679` -- independent cross-ecosystem `HTMAXBUTTON`/auto-hide corroboration
- `https://github.com/JetBrains/JetBrainsRuntime/issues/362` -- JBR's own decoration machinery, real reported conflicts
- `https://github.com/Konyaco/compose-fluent-ui` -- closest known JNA+Compose-Desktop prior art, not independently source-read this pass
- JEP 472 / `openjdk.org`, JNA issues #1665/#1711 -- JDK-24+ native-access warning timeline

### Tertiary (LOW confidence -- flagged, not load-bearing)
- Community sources (ElevenForum, GadgetHacks, How-To Geek, Microsoft Support "Snap your windows") for Snap Groups/shared-border-resize/Win+Shift+Arrow specifics
- `GetSystemMenu`/`TrackPopupMenu` presence in `jna-platform` 5.19.1 -- not exhaustively verified

---
*Research completed: 2026-09-25*
*Ready for roadmap: yes*
