# Phase 20: Verification - Context

**Gathered:** 2026-07-28
**Status:** Ready for planning

<domain>
## Phase Boundary

The v3.0 milestone's visual work is demonstrated, mechanically gated, code-reviewed and
human-approved across all three themes before shipping. **No component is restyled in this phase** —
the eight targets were finished in Phases 17/18/19. This phase closes the loop the v2.0.3
false-positive-sign-off lesson demands.

**In scope:**
- A new permanent showcase section presenting all eight restyled components in one flow, so
  cross-component coherence is reviewable at all (SHW-15); existing sections audited for state
  completeness only.
- `/gsd-code-review` over the Phase 16–19 diff, findings closed, **before** the human sign-off.
- The folded `AeroButton` label-contrast fix (see Folded Todos) plus its value-level regression test.
- Three mechanical gates: VER-01 (gradient stops proportional, not pixel-fixed), VER-02 (nobody
  bypasses `aeroSurface()`'s centralized clip order), VER-03 (default sizes and corner radii have
  not silently crept vs. the pre-migration baseline) — each carrying its own fail-then-pass proof
  (VER-06).
- A minimal scratch consumer outside this repository, proving the published artifact is usable by a
  stranger (VER-05).
- The human three-theme sign-off, including non-100% DPI passes (SHW-16).

**Explicitly NOT in this phase:**
- Any restyle of the eight targets beyond the folded contrast fix, and any change to public API,
  default sizes, or behavior (milestone constraint — 1:1 except where VER-03 records an approved
  exception).
- A new keyboard-activation test — VER-04 is already closed by `AeroButtonSemanticsTest` (D-15).
- G4 (`AeroOrnamentTokens` brightness on AeroBlue/AeroDark) — deferred by explicit reviewer decision
  at 19-08 to a separate Phase 16 foundation session.
- The remaining ~40 components' visual sweep (VIS-F01), and every other item in Deferred Ideas.

</domain>

<decisions>
## Implementation Decisions

### Sign-off shape

- **D-01:** The final human sign-off is **hybrid**: a cross-component coherence pass over all eight
  on AeroBlue / AeroDark / Classic, **plus** a full per-state matrix only for what changed after its
  own sign-off. That set is `AeroButton` + `AeroOutlinedButton` (both via `AeroButtonSurface`):
  19-09 moved their focus glow onto `focusVisible` (WR-01), the G5 closure unified the segmented
  control onto their constants, and this phase's contrast fix lands there too. `AeroSlider` /
  `AeroRangeSlider` / `AeroProgressBar` are **not** re-reviewed state-by-state — nothing has touched
  them since 18-04's approval. — **Reversibility:** reversible — widening the matrix later costs
  only review time.
- **D-02:** The non-100% DPI requirement is met with **two passes on one theme (AeroBlue): 125% and
  200%**. 125% is fractional and is where 1dp contours, seams and bevels break on rounding; 200% is
  where gloss and gradient proportionality show up at size. One theme is enough because the failure
  mode being hunted is geometric, not chromatic. — **Reversibility:** reversible.
- **D-03:** **Code review runs BEFORE the human sign-off**, and its findings are closed first.
  Precedent that decides this: in Phase 19 the sign-off PASSED at 19-08, then code review opened
  CR-01/CR-02 + WR-01/03/04, reopening four requirements and costing four more plans plus a
  re-sign-off. The eye reports "too bright"; it does not report "1.70:1 against the fill".
  — **Reversibility:** reversible, but inverting the order reintroduces the exact loop above.
- **D-04:** The showcase gains a **new permanent review section** holding all eight in one flow —
  the coherence pass of D-01 has no screen to happen on today (demos are spread across
  `ButtonsSection` / `RangeSection` / `SelectionSection` / `ListSection`). It stays in the repo for
  future visual milestones rather than being deleted after the gate. Existing sections are audited
  for state completeness and topped up where a state is missing; they are not restructured.
  — **Reversibility:** reversible.

### Mechanical gates

- **D-05:** **VER-01 is a positive rule, not a numeric-literal search:** if a gradient declares an
  explicit end stop, that stop must derive from `size.`; gradients declaring no explicit stops are
  legal (they already span the full bounds). This catches both the historical `endY = 100f` and the
  bypass a numeric search would miss — `endY = GLOSS_END_PX`, the identical bug wearing a named
  constant. Cost of the choice: one explicit carve-out in the rule. — **Reversibility:** reversible.
- **D-06:** **VER-01 covers the whole library**, not just the eight targets plus primitives. Scan
  evidence: only **18 gradient call sites across 9 files** exist, and the out-of-scope ones are
  already correct (`AeroDrawer.kt:150` uses `size.height * 0.4f`, `AeroPopover.kt:75` uses
  `size.height * 0.55f`) — so library-wide is expected to be green on day one and costs nothing,
  while binding everything written later. — **Reversibility:** reversible.
- **D-07:** **VER-02 catches exactly two bypasses**, both checkable by position in the source with
  no exception lists: (a) where a chain contains both, `aeroGlowRing` must precede `aeroSurface` —
  the surface's own `.clip(shape)` erases any bloom drawn after it, which is precisely how the glow
  silently vanished in 16-05 and had to be documented as a USAGE CONTRACT; (b) no component-authored
  `.clip(` after `aeroSurface(` in the same chain — a second clip over the centralized one.
  Hand-rolled `drawBehind` + `.clip` glass is deliberately **not** gated: `GlassModifiers.kt`, the
  color-picker internals and the overlay components draw that way legitimately, and gating it would
  require an exception list that rots. — **Reversibility:** reversible.
- **D-08:** **VER-06 is satisfied by fixtures living inside each gate test**, not by a one-time
  transcript in a SUMMARY. Each detector is extracted as a pure function over source text, with a
  violating string and a compliant string asserted next to it, so the proof that the gate can fail
  re-runs on every build. This is strictly stronger than the Phase 17 precedent (temporarily break
  the real file, record FAIL→PASS in `17-03-SUMMARY.md`, revert), where the proof stops being
  executable the moment it is written down. — **Reversibility:** costly — the convention applies to
  all three new gates; changing it means re-authoring each.
- **D-09:** Gate mechanism is a **Kotlin source-scan test in the normal suite**, following the six
  existing `*SourceTest.kt` files (cwd-independent source resolution via candidate paths) — not a
  separate Gradle task or CI step. Settled by precedent, not asked.

### VER-03 — baseline and measurement

- **D-10:** The expected values come from **git tag `v2.0.4`** — the real pre-migration code, which
  is what VER-03 asks for. A frozen-current baseline was rejected because it cannot, by definition,
  detect drift that already happened during Phases 15–19, which is the drift VER-03 exists to find.
  Manual pre-check: public defaults **held** — `AeroButton height = 30.dp`, `AeroSwitch 36×18` with a
  `14.dp` thumb, `AeroSegmentedControl 28.dp`, `AeroProgressBar 8.dp`; `AeroButton`'s
  `RoundedCornerShape(4.dp)` merely relocated into `AeroButtonSurface`. **One real divergence:**
  `AeroListItem`'s row height changed from a fixed `.height(36.dp)` to `.heightIn(min = 36.dp)`.
  That is entered as an **explicit documented exception** citing the approved G1 closure (19-06 — the
  row had to grow so its text fit inside the selection pill); the user declined to reopen it.
  — **Reversibility:** reversible.
- **D-11:** Sizes are **measured from composed nodes**; corner radii are asserted **at value level**.
  Not a preference — a radius does not participate in layout and cannot be read off a composed node,
  so radii have to be checked against constants regardless, while measuring sizes catches the case
  where the declared number is right and the component still grew (exactly how the list row grew).
  `runComposeUiTest` is already in use (`AeroPanelGroupRecomposeUiTest`, `AeroButtonSemanticsTest`).
  Claude's call, recorded here so it is not re-litigated.

### AeroButton label contrast (folded todo)

- **D-12:** **The fill and the label-color mechanism for `AeroButton` and `AeroSegmentedControl` have
  ONE source of truth** — the shared `AeroButtonSurface` code. The contrast fix moves both together;
  per-component constant retuning is forbidden. This is the standing rule the user restated when the
  problem resurfaced ("должен быть ЕДИНЫМ для подобных элементов… ЕДИНЫЙ СТИЛЬ"), and it is exactly
  why gap G5 was rejected at 19-12: the segmented control's raised fill had been darkened
  (0.45f/0.61f) chasing contrast until it became a bespoke colour unlike the button's.
  **Direction and magnitude are Claude's discretion** under three constraints: the label clears the
  4.5:1 normal-text floor against **both** gradient stops in **all three** themes; the result is not
  garish; the two components read as one style. — **Reversibility:** costly — both components'
  appearance moves together and the change needs the three-theme review to land.
- **D-13:** A **value-level contrast regression test** ships with the fix, mirroring the 12-assertion
  guard written for `AeroSegmentedControlStylesTest` (and retired when G5 closed). No test in the
  codebase measures this today — that is how it went unnoticed since Phase 17.

### VER-05 — scratch consumer

- **D-14:** The scratch consumer lives **outside this repository and pulls the library by tag through
  JitPack** — the same path a stranger would take. A third Gradle module beside `library`/`showcase`
  was rejected precisely because it builds together with everything and therefore proves nothing
  about the published artifact. Precedent for the mechanism: 15-06 proved the JitPack build with a
  throwaway pre-release tag (`v3.0.0-alpha01`) rather than bumping `build.gradle.kts`, keeping the
  locked bump-on-milestone rule intact — do the same here. — **Reversibility:** reversible.
- **D-15:** It must **launch and render all eight components**, not merely compile. A compile-only
  check misses everything that only surfaces at runtime — missing theme wiring, `LocalAeroColors`
  assumptions the showcase satisfies on the components' behalf, resource loading. Deliberately
  written outside the showcase's own conventions. — **Reversibility:** reversible.

### VER-04 — already closed

- **D-16:** **No new keyboard-activation test is written.** `AeroButtonSemanticsTest` already asserts
  `Role.Button` semantics plus Space/Enter activation for both converted buttons (written in 17-03,
  with its CMP 1.11.1 test-API signatures compile-proven by direct `javap` inspection). Phase 20
  verifies that test is current and green, and records VER-04 as closed by it.

### Claude's Discretion

Settled by Claude, corrected during execution and at the Phase 20 sign-off — do not block on asking
the user:

- How each gate test is written (detector shape, fixture strings, file resolution) — D-09/D-08.
- The direction and magnitude of the contrast fix, inside D-12's one-source-of-truth constraint.
- Layout and grouping of the new review section, and what "coherence" concretely means on it — which
  states are shown statically vs. exercised live. Note the Phase 17 P04 precedent: **no forced-state
  static styling** was added, because the live theme switcher plus a real mouse/keyboard makes
  transient states genuinely reviewable without bypassing the real state-resolution path.
- The throwaway tag name used for VER-05's JitPack pull.
- Which missing states in the existing four sections are worth topping up vs. leaving alone.

### Folded Todos

- **`AeroButton` label-to-fill contrast below the WCAG 4.5:1 floor**
  (`.planning/todos/pending/2026-07-28-track-aerobutton-label-contrast-below-wcag-4-5-1-floor.md`,
  match score 0.9). Filed while closing G5 and deferred *because* fixing it is a visual change to a
  Phase-17-signed-off component requiring fresh sign-off — Phase 20 **is** that fresh sign-off, so it
  folds in here at its cheapest moment. Measured ratios against the button's own rest fill:
  AeroBlue 2.35 / 3.49, AeroDark 1.70 / 2.56, Classic 3.98 / 5.54 — short of 4.5:1 on at least one
  stop in every theme, on both stops in two. Root cause: `AeroButtonSurface`'s `Text` sets no
  `color =`, so the label resolves to ambient `LocalContentColor` (`onBackground`, byte-identical to
  `onSurface` in all three schemes) — **the label has never actually been white**, despite
  `FILLED_FILL_TOP_DARKEN`'s KDoc historically claiming the darken existed for legible white text.
  Governed by D-12 and D-13.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner) MUST read these before planning or implementing.**

### Requirements & success criteria (primary)
- `.planning/REQUIREMENTS.md` §"Showcase + Verification (SHW / VER)" lines 82–91 — **SHW-15, SHW-16,
  VER-01..VER-06**, the eight requirements this phase closes. Written in Russian. Line 130 records
  that default sizes/radii drift is "охраняется VER-03"; line 128 records that screenshot-regression
  tooling (Roborazzi / Paparazzi) is Android-only and was rejected — human sign-off plus mechanical
  gates is the deliberate substitute, so do not propose pixel-diff tooling.
- `.planning/ROADMAP.md` §"Phase 20: Verification" (lines 276–293) — the five explicit Success
  Criteria, including VER-06's "provably fail on unfixed code before being proven to pass" wording.

### The folded todo
- `.planning/todos/pending/2026-07-28-track-aerobutton-label-contrast-below-wcag-4-5-1-floor.md` —
  measured ratios, root cause, and the two candidate fix directions. Governed by D-12/D-13.

### Gate precedent — read before authoring any new gate
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` —
  the reference source-scan guard: cwd-independent `sourceFile()` candidate-path resolution, the
  substring-safety reasoning (`aeroSurface(` never occurs inside `AeroButtonSurface(`), and the
  KDoc convention of recording *why* each assertion is a real gate. Its own fail-then-pass proof
  lives in `20`-adjacent history at `.planning/phases/17-buttons/17-03-SUMMARY.md` — D-08 supersedes
  that style for the new gates.
- The other five existing guards, for shape and naming:
  `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt`,
  `.../components/range/AeroSliderSourceTest.kt`, `.../components/range/AeroProgressBarSourceTest.kt`,
  `.../components/selection/AeroSegmentedControlSourceTest.kt`,
  `.../components/selection/AeroSwitchSourceTest.kt`.
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt` — closes
  VER-04 (D-16). Also the `runComposeUiTest` + `performKeyInput` reference for CMP 1.11.1.
- `library/src/test/kotlin/com/mordred/aero/components/layout/AeroPanelGroupRecomposeUiTest.kt` —
  the project's other `runComposeUiTest` user and the origin of the "a guard must provably fail on
  unfixed code" rule; the measurement precedent D-11 builds on.

### What the gates are guarding (load-bearing — read before writing VER-01/VER-02)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` —
  `drawAeroSurfaceCore`, `Modifier.aeroSurface` (which owns the `.clip(shape)` VER-02 protects),
  `aeroGlowRing`/`drawAeroGlowRing`, `aeroThumbSurface`/`drawAeroThumb`, `aeroGroove`. The in-file
  **ordering rule KDoc** (`aeroGlowRing` BEFORE `aeroSurface`) and the `aeroGlowRing` **USAGE
  CONTRACT** (apply outside any clip) are the literal text VER-02 turns into a test.
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — where the original defects
  lived: the `endY = 100f` pixel-fixed gloss VER-01 exists to prevent, and the draw-before-clip
  half-thickness border. Now proportional (`size.height * 0.32f` in `glassSurface`, `0.55` in
  `glassPanel`) and on `drawWithCache`.
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` — the single
  source of truth D-12 binds the contrast fix to: `FILLED_FILL_TOP_DARKEN` / `FILLED_FILL_BOTTOM_DARKEN`,
  `PRESSED_INNER_SHADOW` (internal, consumed cross-package by `AeroSegmentedControl`),
  `outlinedStyle()`, `resolveButtonStyle`, and the `Text` that sets no `color =`.
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` — the other
  half of D-12's unified pair; its raised fill and single `colors.onSurface` label token.

### VER-03 baseline
- Git tag **`v2.0.4`** — the pre-migration source of truth for default sizes and corner radii
  (`git show v2.0.4:library/src/main/kotlin/com/mordred/aero/...`).
- `.planning/phases/15-toolchain-upgrade/baseline/` — the seven pre-migration screenshots across
  three themes. **Visual** baseline only; it contains no numeric values, which is why D-10 goes to
  the tag instead.
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` — `ROW_MIN_HEIGHT` and
  the KDoc explaining the fixed→min change; the one documented VER-03 exception.

### Sibling phases whose gates and sign-offs this phase consolidates
- `.planning/phases/17-buttons/17-CONTEXT.md`, `.planning/phases/18-range/18-CONTEXT.md`,
  `.planning/phases/19-selectors-lists/19-CONTEXT.md` — the per-phase decision sets; each phase's
  "the showcase's formal three-theme sign-off and grep-gates — that is Phase 20" carve-out is the
  scope this phase inherits.
- `.planning/phases/19-selectors-lists/19-UAT.md` — where G5 was opened and the contrast todo filed;
  the direct precedent behind D-12.
- `.planning/phases/16-foundation-aero-primitives-layer/16-CONTEXT.md` and `16-PATTERNS.md` — the
  fidelity band and the analog map.

### Institutional memory (load-bearing)
- `.planning/STATE.md` §"Architecture positions locked for planning" (esp. line 91: fixing
  `GlassModifiers.kt` re-renders the ~40 out-of-scope components — the reason D-06 goes library-wide)
  and §"Blockers/Concerns" (no real external consumer tracks v3.0; VER-05 is the mitigation, "not a
  full replacement").
- `.planning/PROJECT.md` §"Key Decisions" — the v2.0.3/v2.0.4 false-positive-sign-off lesson that
  VER-06 encodes; `Color.Transparent` as anti-pattern (PRIM-14); `drawWithCache` perf baseline
  (PRIM-13).

### No external specs/ADRs
- No external ADRs or standalone spec docs exist, and no `*-SPEC.md` for this phase. Requirements
  live in REQUIREMENTS.md (SHW/VER), ROADMAP.md Success Criteria, and the decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Six shipped `*SourceTest.kt` guards** — the exact shape D-09 reuses. `AeroButtonSurfaceSourceTest`
  is the most fully documented; copy its `sourceFile()` candidate-path helper verbatim rather than
  re-deriving cwd handling.
- **`AeroButtonSemanticsTest`** — closes VER-04 outright (D-16). No new work.
- **`runComposeUiTest` already proven on this toolchain** — v1 API kept deliberately at CMP 1.11.1
  (deprecated but compiling); `AeroPanelGroupRecomposeUiTest` does programmatic drag,
  `AeroButtonSemanticsTest` does `requestFocus()` + `performKeyInput`. D-11's measurement tests have
  no new infrastructure to build.
- **The retired 12-assertion contrast guard** from `AeroSegmentedControlStylesTest` (removed when G5
  closed) — D-13's model; recover its shape from git history rather than inventing an assertion style.
- **Existing showcase sections** (`ButtonsSection`, `RangeSection`, `SelectionSection`, `ListSection`,
  `PrimitivesSection`) already carry per-component state-matrix demo rows from 17-04 / 18-04 / 19-04.
  The new review section composes against the same components, not against forked demo code.

### Established Patterns
- **A guard must provably FAIL on unfixed code before it counts** — the v2.0.3 lesson, encoded as
  VER-06 and sharpened by D-08 into an executable fixture.
- **Source-scan guards over runtime assertions** where the invariant is structural (modifier order,
  which painter is called) — six precedents; runtime measurement is reserved for what only shows up
  composed (D-11).
- **Throwaway pre-release tags for release proofs** — 15-06 used `v3.0.0-alpha01` rather than bumping
  `build.gradle.kts`, keeping the bump-on-milestone rule intact. D-14 reuses this.
- **One shared painter, param-differentiated** (`AeroButtonSurface(outlined = …)`,
  `AeroPanelGroupImpl(orientation)`) — the general form of D-12's one-source-of-truth constraint.
- **No forced-state static styling for review** (17-04) — transient states are reviewed live through
  the real resolution path, not by bypassing it with hardcoded style instances.

### Integration Points
- The new review section plugs into the showcase's existing section registry and `ThemeSwitcher`;
  the three-theme pass is a runtime toggle, not three builds.
- DPI scale is an OS-level display setting for a Compose Desktop window — the 125%/200% passes are
  performed by changing Windows display scaling and relaunching, not by a code path.
- The scratch consumer (D-14) consumes `com.github.Tolaseeq:aero-compose-ui:<tag>` from JitPack;
  `jitpack.yml` was confirmed sufficient on openjdk17 for this toolchain in 15-06.

</code_context>

<specifics>
## Specific Ideas

- The reviewer's standing rule, stated when the contrast fix came up: *"Нельзя просто так взять и
  изменить бекграунд. Он должен быть ЕДИНЫМ для подобных элементов, например для сегментед контрола
  и кнопки, ЕДИНЫЙ СТИЛЬ. Сам решай, как будешь всё править, но должно быть не вырвиглазно и красиво
  и читаемо и единый стиль."* — recorded as D-12.
- The coherence page exists to answer one question by eye: do all eight read as one material family,
  or as eight separate approximations of it. That is a different question from "is each state
  correct", which is what Phases 17–19 already answered.
- 125% is chosen over 150% deliberately: fractional scaling is where a 1dp rim rounds to nothing on
  one edge and two pixels on the other.
- The gates are written so that breaking them is loud and fixing them is obvious — a failing message
  names the requirement ID and the reason, following the existing guards' assertion-message style.

</specifics>

<deferred>
## Deferred Ideas

- **G4 — `AeroOrnamentTokens` brightness on AeroBlue and AeroDark.** Re-confirmed at 19-08 and
  deferred by explicit reviewer decision to a separate Phase 16 foundation session, with a standing
  direction to try darker values specifically for those two themes. Not folded here: it is a
  foundation-layer change whose blast radius is every component, not a verification item.
- **`AeroRangeSlider` accessibility semantics + keyboard support** — zero semantics today (custom
  Canvas). Carried unresolved from Phase 18 and Phase 19; still needs its own phase.
- **Arrow-key roving focus for `AeroSegmentedControl`** — rejected in 19 D-09 as a behavior change in
  a visual-only milestone. Genuine keyboard-ergonomics work for a future phase.
- **`AeroDropdown` popup-offset regression (DROP-FIX-01)** — v1.0 carry-over, explicitly out of v3.0
  scope.
- **VLST-F01** (list-item mirror reflection), **VRNG-F01** (Win7 ping-pong indeterminate),
  **VIS-F01** (visual sweep of the remaining ~40 components) — already flagged future requirements in
  REQUIREMENTS.md.
- **`AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton`** — outside the eight-target set,
  still untouched. Whether they adopt the shared Aero surface is a future-milestone question. Note
  they *are* affected by D-12 if the contrast fix moves shared constants — that impact must be looked
  at during the coherence pass even though restyling them is out of scope.

### Reviewed Todos (not folded)
None — the single matched todo was folded (see Folded Todos).

</deferred>

---

*Phase: 20-verification*
*Context gathered: 2026-07-28*
