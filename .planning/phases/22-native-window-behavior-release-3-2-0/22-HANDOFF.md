# Phase 22: Hand-off (VER-13)

Agent-reviewed evidence for the v3.2 Native Window Behavior milestone, assembled by Plan 22-16
(2026-09-28) after the machine was restored. Companion documents: `22-SESSION.md` (the full
real-input session record), `22-NOTES.md` (every empirical finding, conflict and RED baseline),
`22-UNCONFIRMED.md` (everything NOT proven — read it before treating any row below as full
coverage). Frames: `.captures/22-final/` (final), `.captures/22-session/{jdk,jbr}/` (session),
`.captures/22-baseline/` (pre-phase). The agent's 22-16 review was pixel-measurement-based
(LockBits full-frame + band comparisons, `22-NOTES.md` § "Final hand-off (22-16)") — every
"identical" below is a 0-pixel count, and every gap is named, not smoothed over.

## Gap-closure outcomes — the five 22-16 blockers after 22-20..22-25

Resolved by plans 22-20..22-24 (mechanisms/formulas, no unjustified library change),
re-verified live in the 22-25 session (consent recorded; `.captures/22-reverify/`), and
booked by 22-26. No row below is silently passed or silently failed — each carries its
honest outcome:

| # | Requirement | Blocker (22-16) | Outcome (22-25 re-verification) | Status |
|---|-------------|-----------------|--------------------------------|--------|
| 1 | SNAP-04 | Caption double-click never maximizes | Mechanism landed (22-20: `WM_NCLBUTTONDBLCLK` at HTCAPTION → `DefWindowProc`); headless posted-message proof green (`.captures/22-gapmenu/`). Live: PASS on JBR (real MAX+RESTORE pair from a verified floating reset), FAIL on JDK the same run — an unexplained JVM-differential | OPEN as a requirement — stays Pending; differential recorded (`22-UNCONFIRMED.md` § 17) |
| 2 | SNAP-05 | Alt+Space menu never opens | Menu now OPENS on JBR (`S05-ALTSPACE-MENU` PASS: #32768 of our pid, foreground=True — the 22-23 hand-declared `GetSystemMenu`/`TrackPopupMenu` fix works). NEW open item: menu command navigation inert (Move/Size/Minimize/Maximize/Close produce no state change with the menu open and the window foreground; the SIZE attempt left the window 160x28). JDK family rows were foreground-blocked (VS Code pid 12752 race); Alt+F4 half PROVEN on JBR | PARTIAL — stays Pending |
| 3 | SNAP-06 | Shared-border resize moves nothing | Library-side exonerated headlessly (22-24: no NCCALCSIZE clamp, border hit codes byte-match the OS seam convention in every reachable state). Live: UNPROVEN — both 22-25 passes failed at PRE-SNAP (foreground-blocked narrow chord), so the border drag never ran against a landed pair; the Snap Groups thumbnail was never observed (observation declined-by-protocol) | UNPROVEN — stays Pending |
| 4 | BTN-01 (press clause) | Maximize-button press fill differs from minimize's | RESOLVED: parity exact on JDK (`B01-PRESS-FRAME` PASS: press-max-vs-press-min diff=0, press-vs-rest 144 — the 22-15 FAIL was a check-formula artifact, 22-23: the check maximized its own window before the min step). JBR 22-25 row blocked by the end-of-pass zero-diff cluster (visual never captured — not a parity difference) | RESOLVED — BTN-01 flipped Complete |
| 5 | JBR 21 pass drift | Caption drags + Win+arrow chords inert; W05 glyph crop unstable | RESOLVED for chords and W05 stability: chords work foreground-ours on JBR (S03-WIN-RIGHT/UP/DOWN PASS — 22-21's foreground explanation confirmed); the W05 float-instability did NOT reproduce (16/16 samples, float1-vs-float2 = 0). OPEN for the caption drag: S01 ×4 FAIL with stronger discrimination (same-point double-click works, chords work, edge drags work — the drag modal loop specifically does not move the JBR window) | SPLIT — SNAP-03/WIN-05 flipped Complete; SNAP-01 stays Pending |

The 22-15 check-formula artifacts named in the original blocker list were fixed (22-22/22-23)
and re-run green wherever the gesture could be judged (corners TR/BL/BR, both floors,
S03-WIN-DOWN on JBR, W06 ncdestroy on JBR). Everything still without a green row — the W04
trio, S02-LAYOUT-PICK, S06-SNAP-GROUP, and the 22-25 residual items (JDK S04 differential,
S05 command navigation, JBR S01 drag, TL-corner anomaly, JDK ncdestroy lag, harness gaps) —
is carried in `22-UNCONFIRMED.md` § 12 and § 17.

## Per-requirement table (28 IDs)

| Requirement | Status | Evidence |
|-------------|--------|----------|
| SNAP-01 | Pending (JDK evidence stale / JBR drift open) | JDK `S01-*` all PASS 22-15 (rects = exact halves/quarter of rcWork, restore 1200x800) but predates the WndProc changes — stale for flipping (22-25 verdict); JBR S01 ×4 FAIL 22-25 with the drift narrowed to the drag modal loop (same-point double-click, chords and edge drags all work on the same window). `22-SESSION.md`; `.captures/22-reverify/jbr/` |
| SNAP-02 | Pending (flyout PASS / layout pick unproven) | `S02-FLYOUT` PASS both JVMs 22-15 (UIA/event signature on explorer's `XamlExplorerHostIslandWindow` bridge, ~610-720 ms, no screenshot) + 22-04 early gate `GATE PASS`; `S02-LAYOUT-PICK` UNCONFIRMED (UIA blind to flyout zones) — the 22-25 re-run threw a harness error before the verdict (reference control matched, 708/652 ms; `Events.EventName` formatting throw), and the maintainer observation was declined-by-protocol |
| SNAP-03 | PASS — flipped Complete 22-26 | Chords proven on both JVMs: JDK 22-15 `S03-WIN-LEFT/RIGHT/UP` PASS (half-screen rects) + WIN-DOWN behavior proven by the standalone rows; JBR 22-25 `S03-WIN-RIGHT/UP/DOWN` PASS with `foregroundOurs=True` (LEFT FAIL = un-retried foreground fluke, RIGHT passed foreground-ours one check later). The 22-15 JBR inertness was foreground-shaped (22-21 confirmed) |
| SNAP-04 | PARTIAL — stays Pending | JBR 22-25: real MAX+RESTORE pair PASS from a verified floating reset (the 22-20 mechanism fires); JDK 22-25 `S04-DBLCLICK-MAX` FAIL (isZoomed=False, verified floating reset) — an unexplained JVM-differential; outcome #1 above |
| SNAP-05 | PARTIAL — stays Pending | Menu OPENS on JBR (`S05-ALTSPACE-MENU` PASS 22-25, the 22-23 fix); command navigation inert (outcome #2); JDK family foreground-blocked; `C02-OPTOUT-ALTSPACE` PASS proves the comparison path; Alt+F4 half of `S05-CLOSE-VS-ALTF4` PROVEN (both JVMs 22-15, JBR 22-25) |
| SNAP-06 | UNPROVEN — stays Pending | Library-side exonerated (22-24); both 22-25 passes failed at PRE-SNAP (foreground-blocked narrow chord — chord-failure evidence, not border evidence); `S06-SNAP-GROUP` thumbnail never observed (2 s hover 22-15; observation declined-by-protocol 22-25); outcome #3 |
| SNAP-07 | Observed-only — stays Pending | 22-15 JBR Shift-drag landed at `16,16,1432,1016` — a real priority-grid zone (union of zones 0+1, the 22-22 PowerToys model derives it exactly); zones found again 22-25 (priority-grid, 3 zones, spacing 16) but the drag never started foreground-ours on either JVM (environment-blocked, one retry each) — no green S07 verdict row on the current build |
| WIN-01 | PASS | `W01-MAX-VISIBLE-TASKBAR` (maximized client == rcWork, taskbar not overlaid) + `W01-AUTOHIDE-REVEAL` (taskbar revealed after ~232 ms hover over the 2 px inset) — both JVMs; headless `V11-MAX-WORKAREA`/`V11-AUTOHIDE-EDGE` PASS since 22-05, re-proven 22-16 |
| WIN-02 | PASS — flipped Complete 22-26 | Re-verified 22-25 with the corrected formulas: floors PASS both JVMs (320 exact library / 260 exact app, D-01), corners TR/BL/BR PASS both JVMs (correct shapes + 60x60); edges PASS both JVMs 22-15 (system cursors + 60 px single-axis growth); `V11-HT-EDGE/CORNER-*` + `V11-MINSIZE` PASS since 22-11 and re-proven post-gap-closure. Open: the TL corner read inert in 22-25 on both JVMs (zero deltas — `22-UNCONFIRMED.md` § 17) |
| WIN-03 | PASS | `W03-FRAMES` PASS both JVMs (floating title band 0 px vs pre-phase baseline); `W03-CORNERS` PASS (152/150 DWMWCP_DONOTROUND read-backs = 1); 22-16 final: FULL-FRAME 0 px vs baseline, all three schemes; no white strip, no ghost caption (`22-NOTES.md` 22-05/22-06/22-09) |
| WIN-04 | UNCONFIRMED | `W04-*` trio on both passes: the 150 % virtual display never attached on this machine (contingency) — `22-UNCONFIRMED.md` § 2-3 |
| WIN-05 | PASS — flipped Complete 22-26 | Placement correspondence: 17/17 samples JDK (22-15) + 16/16 JBR (22-25, zero mismatches); glyph switch proven 22-15 both JVMs (588/616 px crop diff); the 22-15 JBR float-instability did NOT reproduce in 22-25 (float1-vs-float2 = 0 — blocker #5's W05 half resolved). Open: the 22-25 glyph-switch capture read flat (`22-UNCONFIRMED.md` § 17) |
| WIN-06 | PASS | `W06-INDEPENDENT` + `W06-OPEN-WHILE-DRAGGING` PASS both; `W06-CLOSE-DURING-DRAG` re-verified 22-25: PASS on JBR (closeEvent=True, `ncdestroy=True` within budget — the 22-22 pipe-lag question decided for JBR), JDK close path green but `ncdestroy` absent within the 10 s budget (open lag, § 17); headless ncdestroy trace since 22-12 |
| BTN-01 | PASS — flipped Complete 22-26 | Press parity exact on JDK 22-25 (`B01-PRESS-FRAME` PASS: press-max-vs-min diff=0, press-vs-rest 144 — the 22-15 FAIL was a check-formula artifact, 22-23); hover parity 0 px both JVMs (`B01-HOVER-FRAME` 22-15, re-proven JDK 22-25); real click toggles Maximized/Floating both JVMs (`B01-CLICK` 22-15, JDK 22-25). The JBR 22-25 triple FAIL is the end-of-pass zero-diff cluster (environment-blocked, § 17) |
| BTN-02 | PASS | `B02-MIN/LEADING/MARKED/CLOSE-CLICK` all PASS both JVMs (real clicks reach Compose, no window drag); boundary classification headless since 22-07/22-11 |
| API-01 | PASS | Source compatibility: showcase compiles with call sites byte-unchanged (`d85bc86`, 22-12); legacy branch structurally identical (22-07 code read); `PublicApiNoJnaTest` bytecode scan (22-13) |
| API-02 | PASS | `V11-N-HT-MARKED-BOUNDARY` FAIL→PASS with `markAeroTitleBarInteractive()` (22-11→22-12); real marked-click PASS both JVMs (22-15) |
| API-03 | PASS | `-Paero.nativeChrome=false` permanent RED control: exact legacy branch, zero install traces — re-proven final 22-16 (`RED OK`, main 0/18, narrow 0/8) |
| API-04 | PASS | `rememberAeroWindowChrome` public since 22-19; AeroTitleBar is built ON it, so the whole phase's live verification ran through the API (VER-11 18/18+8/8, RED control, click toggle, title band 0 px); generality beyond AeroTitleBar's layout = Plan 13 headless tests (see `22-UNCONFIRMED.md` § 13) |
| DEP-01 | PASS | JNA + jna-platform 5.19.1, `implementation` scope, checksums re-verified against the resolved jars (22-02); zero leakage onto `:showcase` compile classpath; matches Pinya's own version |
| SHW-17 | PASS | Narrow 300x480 window with `AeroTitleBar`, leading slot + marked "Вернуть" overlay (22-08); exercised by every session pass and the 22-16 final frames |
| VER-11 | PASS | Final GREEN 22-16: main 18/18, narrow 8/8, `-AssertV11` exit 0; every GREEN has a RED counterpart (Plan 01 pre-phase baseline 0/18 + the opt-out control 0/18+0/8) — tables below |
| VER-12 | PASS | Early gate `GATE PASS` (22-04, third run); full session on JDK 21 and JBR 21 with the maintainer's warn-and-"ок" (22-15); teardown verified 22-16: driver removed (device + store package gone, screens=1), auto-hide ON (byte 8 = 0x03), PowerToys kept by the maintainer's «оставь»; gap-closure re-verification under recorded consent 22-25 (23-check JDK + 32-check JBR subsets, teardown verified) — per-pass totals below |
| VER-13 | PASS | This hand-off + `22-UNCONFIRMED.md` + the agent-first frame review (`22-NOTES.md` § "Final hand-off (22-16)", measurement-based; visual reviews earlier in phase) — nothing unconfirmed presented as passed |
| VER-14 | PASS | 541 existing tests green at phase start; locked count raised 541→592 in named commits, each raise preceded by a recorded failing guard run (22-13); `AERO_TEST_COUNT total=592` re-verified in the 22-15 readiness build |
| REL-06 | PASS | README window-behavior section written verified-only by 22-17; refreshed by 22-26 from the 22-25 verdict table (press parity + Win+↓ into the works list, Known-gaps sentence rewritten to what remains unproven) |
| REL-07 | PASS | KDoc `AeroTitleBar`/`AeroResizeHandles` rewritten by 22-17 (the "Aero Snap limitation" caveat removed); 22-26 restored the double-click and Alt+Space statements worded to what 22-25 proved (JBR-qualified) |
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

Gap-closure re-verification (22-25, recorded consent): the affected-check subsets re-ran live
on the gap-closure build — JDK 23 checks (pass=10 fail=12 unconfirmed=1), JBR 32 checks
(pass=13 fail=18 unconfirmed=1). Every non-PASS row carries its foreground line and is
environment-blocked, an honest FAIL, or a harness error — never a product verdict without
that attribution. Full pass tables, the environment anomaly record (VS Code pid 12752
foreground race) and the verified teardown: `22-SESSION.md` § "Gap-closure re-verification
(22-25)"; evidence `.captures/22-reverify/{jdk,jbr}/` (`results.json` one pass subdirectory
deeper: `jdk/jdk/`, `jbr/jbr/`).

## The seven research conflicts and their settled answers

| # | Question | Settled answer | Where |
|---|----------|----------------|-------|
| C1 | Style bits of `undecorated=true` on JDK 21 | `0x960B0000` = `WS_POPUP\|WS_SYSMENU\|WS_MINIMIZEBOX\|WS_MAXIMIZEBOX\|WS_CLIPCHILDREN` (no CAPTION/THICKFRAME; capture mode adds `WS_EX_NOACTIVATE`); the native path brings all five needed bits (`0x96CF0000`) | 22-01 T3, 22-02 T3 |
| C2 | Is Alt+Space automatic once the style bits exist? | NO — the subclass forwards unowned messages into AWT's WndProc which swallows the system-menu trigger; hand-declared `GetSystemMenu`/`TrackPopupMenu` (absent from jna-platform 5.19.1) is the known fix | 22-15 (real input, both JVMs; opt-out comparison) |
| C3 | Auto-hide taskbar inset size + edge detection | Detection: `ABM_GETSTATE` + `ABM_GETAUTOHIDEBAREX` per edge against the window's own monitor; inset 2 px (`AUTO_HIDE_INSET_PX`); reveal-on-hover proven with real input (~232 ms) | 22-01, 22-05, 22-15 |
| C4 | Does `WindowState.placement` sync automatically? | YES — zero push code, before and with the subclass installed; re-proven 6/6 on the hardened subclass; the WM_SIZE branch documents why no push exists | 22-02 T3, 22-10 T2 |
| C5 | Max-button approach: message forwarding vs state bridge | Interaction-source bridge via EDT hop (real `Hover/PressInteraction` into the shared `MutableInteractionSource`); click path + swallow check proven live; FlatLaf-style re-injection recorded as the fallback — the 22-15 press-fill mismatch looked like that condition but was a check-formula artifact (22-23: the check maximized its own window before the min step); re-injection never needed, parity proven exact on JDK 22-25 | 22-09 T3, 22-15; 22-23/22-25 for the resolution |
| C6 | Are `DwmExtendFrameIntoClientArea` / corner preference needed? | Windows 11 did NOT round the corners by default once the bits returned (maintainer-observed); chosen look: variant A (square, no system shadow) = `DWMWCP_DONOTROUND` + 0 margins, applied and read back | 22-06 |
| C7 | Does JBR's decoration machinery interfere under `hotRun`? | No engagement anywhere in the codebase (grep zero); cold JDK vs hot JBR byte-identical probe answers; three reload cycles = one install. (The 22-15 JBR session drift, blocker #5, was a NEW finding about that run, not a C7 reversal — chords/W05 resolved as foreground-shaped by 22-25, the caption drag remains an open drift) | 22-01, 22-02, 22-10; 22-15/22-25 for the drift record |

## Where to look

- Frames: `.captures/22-final/` (9 final frames + console logs + JSONs), `.captures/22-session/`
  (per-pass `results.json`, `frames/`, `console-3.log`), `.captures/22-baseline/` (pre-phase),
  `.captures/22-reverify/` (22-25 gap-closure re-verification, both JVMs + console logs).
- Session narrative and honest FAIL reading: `22-SESSION.md`.
- Every probe finding since 22-01: `22-NOTES.md`.
- Everything NOT proven: `22-UNCONFIRMED.md`.
