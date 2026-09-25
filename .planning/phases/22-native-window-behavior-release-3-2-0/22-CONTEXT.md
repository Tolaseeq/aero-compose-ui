# Phase 22: Native Window Behavior + Release 3.2.0 - Context

**Gathered:** 2026-09-25
**Status:** Ready for planning
**Source:** plan-phase session (no discuss-phase — the maintainer chose to plan directly from ROADMAP + REQUIREMENTS + research; the decisions below were settled in that session)

<domain>
## Phase Boundary

Windows treats every window built on `AeroTitleBar`/`AeroResizeHandles` as a native window —
snapping, Snap Layouts, hotkeys, shared-border resize, taskbar-aware maximize, multi-monitor DPI
moves, FancyZones — on a standard JDK 21 without JBR, and the behavior ships as `v3.2.0` on JitPack.

Scope, the 10-step execution order, the 7 conflicts to settle empirically and the success criteria
are fixed by `.planning/ROADMAP.md` § Phase 22 and the 27 requirements in `.planning/REQUIREMENTS.md`
(SNAP-01..07, WIN-01..06, BTN-01..02, API-01..03, DEP-01, SHW-17, VER-11..14, REL-06..08).

**Not in this phase:** any change to how the title bar or its buttons look; Windows 10 verification
(no machine — goes on the "unconfirmed" list, VER-F04); 125% / 200% DPI visual passes (VER-F02);
upgrading consumer apps (Pinya, aska, oper, …) to `v3.2.0`.

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked before this session — do not re-litigate)
- Execution order is the 10-step order in ROADMAP.md § Phase 22, riskiest first; each step is its own
  commit under the existing test suite.
- The 7 conflicts in `.planning/research/SUMMARY.md` § "Conflicts to Settle Empirically" are settled by
  cheap first-hand checks during execution (style-bit read-back first), not by more desk research.
- Real mouse/keyboard input happens only in the two sessions VER-12 names (early 1–2 min gate right
  after the first draft; full session at the end), each after warning the maintainer and getting their
  "ok", announcing when done. The early gate failing = stop and ask before building anything further.
- Window capture is `PrintWindow(hwnd, hdc, 2)` only; the Hot Reload MCP `take_screenshot` is banned.
  The agent inspects every window itself before the maintainer sees it (VER-13).
- Every automated live-window check (VER-11) is proven RED on the old `WindowDraggableArea` behavior
  before it counts as GREEN.
- The locked test count (541) is raised only by a commit whose message names the reason (VER-14).
- Release follows the v3.1.0 pattern: version bump commit, disposable verify tag green on JitPack, then
  the real `v3.2.0` tag (REL-08).

### Minimum window size
- **D-01:** Default resize floor stays **320×240 (dp)** for every window on `AeroTitleBar` /
  `AeroResizeHandles`, including once resizing is handed to Windows — apps that change nothing (aska,
  oper, …) behave exactly as today. An app may set its own minimum through the standard AWT
  `window.minimumSize` (`Component.isMinimumSizeSet()` is true); the library then honors that value
  instead of 320×240, smaller or larger. On the native Windows path the floor is applied in physical
  pixels at the window's current DPI (320 dp × scale), and the library must not overwrite the app's own
  `window.minimumSize`. The SHW-17 narrow showcase window sets its own minimum below 300 px so a ~300 px
  window opens at ~300 px and can be resized back down to it (the Pinya detached-queue scenario, Pinya
  D-23/D-25). The same resolution rule (app minimum if set, else 320×240) applies to the Compose-side
  handles on Linux/macOS, which is behavior-neutral for apps that set nothing.

### Appearance
- **D-02:** No UI-SPEC. The title bar and its three buttons look exactly as today; only the input path
  changes. The maximize button's hover / pressed / maximized-glyph states must be visually identical to
  today's Compose-driven states (verified by the agent's own `PrintWindow` frames). If any visual
  choice does arise during execution, it is shown to the maintainer as side-by-side options in the
  Visual Companion, never decided silently.

### Dependency
- **D-03:** `net.java.dev.jna:jna` and `jna-platform` **5.19.1** are verified legitimate by the
  orchestrator on 2026-09-25: Maven Central serves both POMs (HTTP 200), 5.19.1 is the `<release>` in
  `maven-metadata.xml`, and the published `.jar.sha1` values match the jars already in the local Gradle
  cache (`jna` ca303052cd617c1af2e2c8d344c98a706fb63143, `jna-platform`
  d1e54d9231da5ca3fa730d52960deaa555475468). Adding them needs **no** human-verify checkpoint. Scope is
  `implementation` in `:library` only; no JNA type appears in any public signature (API-01, DEP-01).

### Proof of the Snap Layouts flyout
- **D-04:** In the VER-12 sessions the agent records the Snap Layouts flyout's appearance itself,
  without a screenshot, via a small UI Automation check kept under `tools/` (as VER-12 states). If UI
  Automation cannot observe the flyout, stop and ask the maintainer — do not silently downgrade that
  check to "maintainer saw it".

### Claude's Discretion
- Shape of the live-window opt-in harness for VER-11 (research recommends a Kotlin `main()` under
  `tools/`, following the `tools/capture/` precedent, over a new Gradle source set) — must not run in
  the default `gradlew test`.
- Internal file layout under `library/src/main/kotlin/com/mordred/aero/internal/windows/` per
  `.planning/research/ARCHITECTURE.md`.
- How the second narrow showcase window is switched on (follow the existing `-Paero.*` launch-property
  precedent in `showcase/build.gradle.kts`).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope
- `.planning/ROADMAP.md` § Phase 22 — goal, 7 empirical conflicts, 10-step order, success criteria
- `.planning/REQUIREMENTS.md` — v3.2 requirement text (SNAP, WIN, BTN, API, DEP, SHW-17, VER-11..14, REL-06..08)
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-RESEARCH.md` — codebase grounding, environment facts, Validation Architecture

### Win32 / JNA design (milestone research)
- `.planning/research/SUMMARY.md` — conflicts to settle empirically, implications for roadmap
- `.planning/research/ARCHITECTURE.md` — component layout, `CallWindowProc` passthrough, `HitTestSnapshot` bridge, max-button EDT hop, public API shape
- `.planning/research/STACK.md` — JNA 5.19.1, hand-declared extension interfaces (`GetDpiForWindow`, `GetSystemMetricsForDpi`, `TrackMouseEvent`, `Dwmapi`)
- `.planning/research/PITFALLS.md` — 24 step-mapped pitfalls; "provably fails on old code" guard recipe
- `.planning/research/FEATURES.md` — OS behaviors unlocked, Pinya multi-window scenario
- `.planning/research/MCP-HOWTO.md` — maintainer-verified Hot Reload MCP recipe (wins over desk research)

### Code touched
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt`
- `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt` (`minW = 320f, minH = 240f`, line-56 gate)
- `library/build.gradle.kts` (`lockedTestTotal = 541` guard), `gradle/libs.versions.toml`
- `showcase/src/main/kotlin/com/mordred/showcase/Main.kt`, `showcase/build.gradle.kts`
- `README.md`, `jitpack.yml`, `build.gradle.kts` (version)

### Consumer scenario
- `C:/1A_WORK/pinya/.planning/phases/02-mailbox-multi-project-attention-takeover/02-CONTEXT.md` — D-23 (detachable narrow queue window), D-25 (depends on this release), D-27 (independent top-level window)

### Precedent
- `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/` — 21-VALIDATION.md, 21-UNCONFIRMED.md, 21-HRM.md, plans 13–14 (verify-tag release)

</canonical_refs>

<specifics>
## Specific Ideas

- Pinya's detached queue window carries a waiting-requests counter and a "вернуть очередь" action in
  its title bar — the SHW-17 narrow window mirrors this with one interactive header element marked via
  API-02.
- This dev machine: one 1920×1080 monitor at 100%, taskbar auto-hide ON, PowerToys not installed,
  Windows 11 24H2 (26100), JBR 21.0.9 at `C:\Users\1\.jdks\jbr-21.0.9` for `hotRun`.

</specifics>

<deferred>
## Deferred Ideas

- Windows 10 behavior verification — VER-F04 (named on the unconfirmed list, not claimed).
- 125% / 200% DPI visual passes — VER-F02.
- A greppable gate forbidding `com.sun.jna` outside `internal/windows/` — worth doing later, not required here.

</deferred>

---

*Phase: 22-native-window-behavior-release-3-2-0*
*Context gathered: 2026-09-25 during /bm:plan-phase*
