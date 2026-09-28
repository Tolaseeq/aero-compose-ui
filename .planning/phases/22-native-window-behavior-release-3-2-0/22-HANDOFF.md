# Phase 22: Hand-off (VER-13)

Agent-reviewed evidence for the v3.2 Native Window Behavior milestone, assembled by Plan 22-16
(2026-09-28) after the machine was restored. Companion documents: `22-SESSION.md` (the full
real-input session record), `22-NOTES.md` (every empirical finding, conflict and RED baseline),
`22-UNCONFIRMED.md` (everything NOT proven — read it before treating any row below as full
coverage). Frames: `.captures/22-final/` (final), `.captures/22-session/{jdk,jbr}/` (session),
`.captures/22-baseline/` (pre-phase). The agent's 22-16 review was pixel-measurement-based
(LockBits full-frame + band comparisons, `22-NOTES.md` § "Final hand-off (22-16)") — every
"identical" below is a 0-pixel count, and every gap is named, not smoothed over.

## Release blockers — FAIL items needing a gap-closure plan

The phase is functionally far along (the windows behave natively on the primary JDK path and
the pixels never changed), but these FAIL rows block `v3.2.0` until a gap-closure plan settles
them:

| # | Requirement | What fails | Where proven failing | Known direction |
|---|-------------|-----------|----------------------|-----------------|
| 1 | SNAP-04 | Caption double-click never maximizes | `S04-DBLCLICK-MAX` FAIL on JDK 21 AND JBR 21 (`22-SESSION.md` both pass tables) | same non-client handling family as SNAP-05 |
| 2 | SNAP-05 | Alt+Space system menu never opens on the native window (menu commands Move/Size/Minimize/Maximize/Close all dead; Alt+F4 half works) | `S05-*` FAIL both JVMs; opt-out window DOES open it (`C02-OPTOUT-ALTSPACE` PASS) | C2 SETTLED: the subclass must hand-declare `GetSystemMenu`/`TrackPopupMenu` (absent from jna-platform 5.19.1) — the known fix path |
| 3 | SNAP-06 | Shared-border resize: dragging the common border of two half-snapped windows resizes nothing | `S06-SHARED-BORDER` FAIL both JVMs | unexplained; both windows snap correctly first |
| 4 | BTN-01 (press clause) | Maximize-button press fill differs from minimize's (hover parity IS proven at 0 px) | `B01-PRESS-FRAME` FAIL both JVMs (strip diff 108 px, maxDelta 176) | C5's named fallback condition (FlatLaf-style re-injection) |
| 5 | JBR 21 pass drift | Caption drags (S01 ×4) and Win+arrow hotkeys (S03 ×3) inert; floating glyph crop unstable (W05) — all PASS/green on standard JDK 21 | `22-SESSION.md` § "Differences between passes" | single run per JVM; cause unexplained by the recorded evidence |

Everything else that reads FAIL in the session tables is either a check-formula artifact
(observed values prove the behavior: corners ×4, floors ×2, S07 heuristic, W06 trace timing,
S03-WIN-DOWN ordering — `22-UNCONFIRMED.md` § 15) or environment-blocked (the W04 trio,
S02-LAYOUT-PICK, S06-SNAP-GROUP — `22-UNCONFIRMED.md` § 12).

## Per-requirement table (28 IDs)

| Requirement | Status | Evidence |
|-------------|--------|----------|
| SNAP-01 | PASS (JDK 21) / FAIL (JBR 21) | `S01-LEFT/RIGHT/QUARTER/TOP/DRAG-AWAY-RESTORE` all PASS on JDK (rects = exact halves/quarter of rcWork, restore 1200x800); JBR pass inert (blocker #5). `22-SESSION.md`; frames `.captures/22-session/{jdk,jbr}/frames/` |
| SNAP-02 | PASS (flyout) / UNCONFIRMED (layout pick) | `S02-FLYOUT` PASS both JVMs (UIA/event signature on explorer's `XamlExplorerHostIslandWindow` bridge, ~610-720 ms, no screenshot) + 22-04 early gate `GATE PASS`; `S02-LAYOUT-PICK` UNCONFIRMED (UIA blind to flyout zones) |
| SNAP-03 | PASS (JDK 21) / FAIL (JBR 21) | `S03-WIN-LEFT/RIGHT/UP` PASS on JDK (half-screen rects, `placement=Maximized`); `S03-WIN-DOWN` FAIL is an ordering artifact (its own Win+Up landed after restore — behavior proven by the standalone rows); JBR pass inert (blocker #5) |
| SNAP-04 | FAIL | blocker #1 above |
| SNAP-05 | FAIL | blocker #2 above; `C02-OPTOUT-ALTSPACE` PASS proves the comparison path; Alt+F4 half of `S05-CLOSE-VS-ALTF4` PROVEN both JVMs |
| SNAP-06 | FAIL (border) / UNCONFIRMED (group) | blocker #3; `S06-SNAP-GROUP` UNCONFIRMED (no thumbnail window in 2 s hover) |
| SNAP-07 | PASS | `S07-FANCYZONES` on JBR: Shift-drag landed at `16,16,1432,1016` — exactly a real priority-grid zone (work area inset by the layout's 16 px spacing); the FAIL verdict is the check's 2x2-grid heuristic. JDK attempt started without foreground (recorded). Zones read from PowerToys `applied-layouts.json` |
| WIN-01 | PASS | `W01-MAX-VISIBLE-TASKBAR` (maximized client == rcWork, taskbar not overlaid) + `W01-AUTOHIDE-REVEAL` (taskbar revealed after ~232 ms hover over the 2 px inset) — both JVMs; headless `V11-MAX-WORKAREA`/`V11-AUTOHIDE-EDGE` PASS since 22-05, re-proven 22-16 |
| WIN-02 | PASS | All four edges: correct system cursors + 60 px single-axis growth, both JVMs; corners: correct shapes + 60x60 (formula-artifact FAILs); floors: width clamped exactly 320 (library) / 260 (app, D-01); `V11-HT-EDGE/CORNER-*` + `V11-MINSIZE` PASS since 22-11 |
| WIN-03 | PASS | `W03-FRAMES` PASS both JVMs (floating title band 0 px vs pre-phase baseline); `W03-CORNERS` PASS (152/150 DWMWCP_DONOTROUND read-backs = 1); 22-16 final: FULL-FRAME 0 px vs baseline, all three schemes; no white strip, no ghost caption (`22-NOTES.md` 22-05/22-06/22-09) |
| WIN-04 | UNCONFIRMED | `W04-*` trio on both passes: the 150 % virtual display never attached on this machine (contingency) — `22-UNCONFIRMED.md` § 2-3 |
| WIN-05 | PASS (JDK 21) / FAIL (JBR 21 crop) | `W05-PLACEMENT`: 17/17 placement-vs-IsZoomed samples both JVMs; glyph switch proven (588 px crop diff); floating crop stable 0 px on JDK, unstable 588 px on JBR (blocker #5) |
| WIN-06 | PASS | `W06-INDEPENDENT` + `W06-OPEN-WHILE-DRAGGING` PASS both; `W06-CLOSE-DURING-DRAG` behavior proven (closeEvent=True, main alive, v11 fail=0) — FAIL is the 5 s ncdestroy-trace window (formula artifact); headless ncdestroy trace since 22-12 |
| BTN-01 | FAIL (press clause) / PASS (hover, click) | blocker #4; hover parity 0 px both JVMs (`B01-HOVER-FRAME`), real click toggles Maximized/Floating both (`B01-CLICK`) |
| BTN-02 | PASS | `B02-MIN/LEADING/MARKED/CLOSE-CLICK` all PASS both JVMs (real clicks reach Compose, no window drag); boundary classification headless since 22-07/22-11 |
| API-01 | PASS | Source compatibility: showcase compiles with call sites byte-unchanged (`d85bc86`, 22-12); legacy branch structurally identical (22-07 code read); `PublicApiNoJnaTest` bytecode scan (22-13) |
| API-02 | PASS | `V11-N-HT-MARKED-BOUNDARY` FAIL→PASS with `markAeroTitleBarInteractive()` (22-11→22-12); real marked-click PASS both JVMs (22-15) |
| API-03 | PASS | `-Paero.nativeChrome=false` permanent RED control: exact legacy branch, zero install traces — re-proven final 22-16 (`RED OK`, main 0/18, narrow 0/8) |
| API-04 | PASS | `rememberAeroWindowChrome` public since 22-19; AeroTitleBar is built ON it, so the whole phase's live verification ran through the API (VER-11 18/18+8/8, RED control, click toggle, title band 0 px); generality beyond AeroTitleBar's layout = Plan 13 headless tests (see `22-UNCONFIRMED.md` § 13) |
| DEP-01 | PASS | JNA + jna-platform 5.19.1, `implementation` scope, checksums re-verified against the resolved jars (22-02); zero leakage onto `:showcase` compile classpath; matches Pinya's own version |
| SHW-17 | PASS | Narrow 300x480 window with `AeroTitleBar`, leading slot + marked "Вернуть" overlay (22-08); exercised by every session pass and the 22-16 final frames |
| VER-11 | PASS | Final GREEN 22-16: main 18/18, narrow 8/8, `-AssertV11` exit 0; every GREEN has a RED counterpart (Plan 01 pre-phase baseline 0/18 + the opt-out control 0/18+0/8) — tables below |
| VER-12 | PASS | Early gate `GATE PASS` (22-04, third run); full session on JDK 21 and JBR 21 with the maintainer's warn-and-"ок" (22-15); teardown verified 22-16: driver removed (device + store package gone, screens=1), auto-hide ON (byte 8 = 0x03), PowerToys kept by the maintainer's «оставь» — per-pass totals below |
| VER-13 | PASS | This hand-off + `22-UNCONFIRMED.md` + the agent-first frame review (`22-NOTES.md` § "Final hand-off (22-16)", measurement-based; visual reviews earlier in phase) — nothing unconfirmed presented as passed |
| VER-14 | PASS | 541 existing tests green at phase start; locked count raised 541→592 in named commits, each raise preceded by a recorded failing guard run (22-13); `AERO_TEST_COUNT total=592` re-verified in the 22-15 readiness build |
| REL-06 | PENDING | README window-behavior section — Plans 17-18, not executed yet |
| REL-07 | PENDING | KDoc `AeroTitleBar`/`AeroResizeHandles` + removing the "Aero Snap limitation" caveat — Plans 17-18 |
| REL-08 | PENDING | Version `3.2.0`, tag, JitPack build — Plans 17-18 |

## VER-11: GREEN and RED

### GREEN (final run, 22-16, post-teardown, standard JDK 21, native path through the public API)

`V11 SUMMARY main pass=18 fail=0 skip=0` / `V11 SUMMARY narrow pass=8 fail=0 skip=0`,
`-AssertV11` exit 0. Full per-check output: `.captures/22-final/v11-green-console.log`, JSON
`v11-green.json`.

| Check | Result | Observed (both windows unless noted) |
|-------|--------|--------------------------------------|
| V11-STYLE | PASS | all five style bits (`0x96CF0000`) |
| V11-HT-CAPTION / V11-N-HT-CAPTION | PASS | `2 (HTCAPTION)` via `SunAwtCanvas:-1 -> SunAwtFrame:2` |
| V11-HT-MAX / V11-N-HT-MAX | PASS | `9 (HTMAXBUTTON)` via the child→frame chain |
| V11-HT-MIN-BOUNDARY / V11-HT-CLOSE-BOUNDARY | PASS | `min=1,captionLeftOfMin=2` / `close=1,max=9` |
| V11-N-HT-LEADING-BOUNDARY | PASS | `leading=1,captionRightOfLeading=2` |
| V11-N-HT-MARKED-BOUNDARY | PASS | `marked=1,captionLeftOfMarked=2` |
| V11-HT-EDGE-L/R/T/B (x4) | PASS | codes 10/11/12/15 via the chain |
| V11-HT-CORNER-TL/TR/BL/BR (x4) | PASS | codes 13/14/16/17 via the chain |
| V11-HT-CLIENT-BOUNDARY | PASS | `client=1,edgeLeft=10` |
| V11-HT-MAXIMIZED-TOP | PASS | `2` |
| V11-MAX-WORKAREA | PASS | client `0,0,1920,1078`; Bottom=2 (auto-hide, need 1-4) ok |
| V11-AUTOHIDE-EDGE | PASS | Bottom uncovered by 2 px |
| V11-MINSIZE | PASS | main `320x240`; narrow `260x200` (V11-N-MINSIZE, app floor honored) |

### RED (every GREEN above has a RED counterpart)

| Control | Result | Evidence |
|---------|--------|----------|
| Plan 01 pre-phase baseline (unmodified window, old `WindowDraggableArea`) | `V11 SUMMARY pass=0 fail=18` — RED OK | `.captures/22-baseline/red-main.json`; `22-NOTES.md` § "RED baseline (22-01)" (all 18 rows FAIL, none SKIP; `V11-MAX-WORKAREA` initially false-positived and was hardened in `0ac7b50` before counting) |
| Opt-out RED control, final 22-16 (`-Paero.nativeChrome=false` + chrome trace) | main `pass=0 fail=18`, narrow `pass=0 fail=8`, `RED OK`; `GWL_STYLE=0x960B0000` (= pre-phase C1); zero `AERO_CHROME` lines | `.captures/22-final/v11-red-console.log`, `v11-red-control.json` |
| Opt-out RED control, in-session (22-15, both passes) | `A04-OPTOUT` PASS: v11 `pass=0 fail=17` per pass on the opt-out launch | `.captures/22-session/{jdk,jbr}/results.json` |
| Narrow-window checks (fixture new this phase) | covered by the opt-out control (0/8) — no pre-phase fixture existed | 22-12's permanent RED control record |

Which GREEN checks have no RED of their own: the real-input checks outside the VER-11 family
(`22-UNCONFIRMED.md` § 16) — S01 has an old-path observation (22-04 early-gate record: "no snap
of any kind happens" on the old path); the rest rest on VER-11's RED discipline.

## VER-12: per-pass totals (22-15, real input, both after the maintainer's warn-and-"ок")

| Pass | Totals (54 checks) | Notes |
|------|--------------------|-------|
| JDK 21 (`:showcase:run`, `C:\Users\1\.jdks\ms-21.0.9`) | pass=31 fail=18 unconfirmed=5 | FAIL texture: 6 formula artifacts + S05 family (6) + S04 + S06-SHARED-BORDER + S07-attempt + B01-PRESS + W06-trace; see `22-SESSION.md` § "Honest reading" |
| JBR 21 (`:showcase:hotRun`, `C:\Users\1\.jdks\jbr-21.0.9`) | pass=24 fail=25 unconfirmed=5 | adds the JBR-only S01x4/S03x3/W05 drift (blocker #5) |

Early gate (22-04): `GATE PASS` on the third real run (flyout observed on a standard DefWindowProc
control AND on the Compose window; half-snap + restore native). Teardown (22-16): verified —
see the VER-12 row above. Environment notes: the 150 % virtual display never attached (W04
UNCONFIRMED by contingency); PowerToys installed for FancyZones and KEPT by the maintainer's
word; auto-hide OFF only during the visible-panel checks, restored and verified.

## The seven research conflicts and their settled answers

| # | Question | Settled answer | Where |
|---|----------|----------------|-------|
| C1 | Style bits of `undecorated=true` on JDK 21 | `0x960B0000` = `WS_POPUP\|WS_SYSMENU\|WS_MINIMIZEBOX\|WS_MAXIMIZEBOX\|WS_CLIPCHILDREN` (no CAPTION/THICKFRAME; capture mode adds `WS_EX_NOACTIVATE`); the native path brings all five needed bits (`0x96CF0000`) | 22-01 T3, 22-02 T3 |
| C2 | Is Alt+Space automatic once the style bits exist? | NO — the subclass forwards unowned messages into AWT's WndProc which swallows the system-menu trigger; hand-declared `GetSystemMenu`/`TrackPopupMenu` (absent from jna-platform 5.19.1) is the known fix | 22-15 (real input, both JVMs; opt-out comparison) |
| C3 | Auto-hide taskbar inset size + edge detection | Detection: `ABM_GETSTATE` + `ABM_GETAUTOHIDEBAREX` per edge against the window's own monitor; inset 2 px (`AUTO_HIDE_INSET_PX`); reveal-on-hover proven with real input (~232 ms) | 22-01, 22-05, 22-15 |
| C4 | Does `WindowState.placement` sync automatically? | YES — zero push code, before and with the subclass installed; re-proven 6/6 on the hardened subclass; the WM_SIZE branch documents why no push exists | 22-02 T3, 22-10 T2 |
| C5 | Max-button approach: message forwarding vs state bridge | Interaction-source bridge via EDT hop (real `Hover/PressInteraction` into the shared `MutableInteractionSource`); click path + swallow check proven live; FlatLaf-style re-injection recorded as the fallback — the session's press-fill mismatch (blocker #4) is exactly that condition | 22-09 T3, 22-15 |
| C6 | Are `DwmExtendFrameIntoClientArea` / corner preference needed? | Windows 11 did NOT round the corners by default once the bits returned (maintainer-observed); chosen look: variant A (square, no system shadow) = `DWMWCP_DONOTROUND` + 0 margins, applied and read back | 22-06 |
| C7 | Does JBR's decoration machinery interfere under `hotRun`? | No engagement anywhere in the codebase (grep zero); cold JDK vs hot JBR byte-identical probe answers; three reload cycles = one install. (The 22-15 JBR session drift, blocker #5, is a NEW finding about that run, not a C7 reversal) | 22-01, 22-02, 22-10; 22-15 for the new finding |

## Where to look

- Frames: `.captures/22-final/` (9 final frames + console logs + JSONs), `.captures/22-session/`
  (per-pass `results.json`, `frames/`, `console-3.log`), `.captures/22-baseline/` (pre-phase).
- Session narrative and honest FAIL reading: `22-SESSION.md`.
- Every probe finding since 22-01: `22-NOTES.md`.
- Everything NOT proven: `22-UNCONFIRMED.md`.
