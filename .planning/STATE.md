---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: Glass Refinement
status: executing
stopped_at: Completed 20-01-PLAN.md (VER-01/VER-02 gates)
last_updated: "2026-07-28T16:38:02.214Z"
last_activity: 2026-07-28
progress:
  total_phases: 6
  completed_phases: 5
  total_plans: 39
  completed_plans: 33
  percent: 83
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-07-21 — after scoping v3.0 Glass Refinement)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** Phase 20 — verification

## Current Position

Milestone: v3.0 Glass Refinement
Phase: 20 (verification) — EXECUTING
Plan: 2 of 7
Status: Ready to execute
Last activity: 2026-07-28

Progress: [█████████░] 85%

## v3.0 Roadmap (2026-07-21)

Six phases, continuing numbering from 15 (project shipped through Phase 14 / v2.0.4). Dependency-justified order per `.planning/research/SUMMARY.md`, validated against `.planning/REQUIREMENTS.md` during roadmap creation — no structural changes made to the research proposal; it held up against the actual requirement set.

| Phase | Name | Requirements | Depends on |
|-------|------|---------------|------------|
| 15 | Toolchain Upgrade | TOOL-01..08 (8) | Phase 14 |
| 16 | Foundation — Aero Primitives Layer | PRIM-01..18 (18) | Phase 15 |
| 17 | Buttons | VBTN-01..06 (6) | Phase 16 |
| 18 | Range | VRNG-01..09 (9) | Phase 16 |
| 19 | Selectors + Lists | VSEL-01..04, VLST-01..04 (8) | Phase 17, Phase 18 |
| 20 | Verification | SHW-15..16, VER-01..06 (8) | Phase 17, 18, 19 |

**Coverage: 57/57 v3.0 requirements mapped, 0 orphaned.** Note: `.planning/REQUIREMENTS.md`'s own Traceability section previously stated "53 total" — an internal miscount in that doc; the actual enumerated REQ-IDs sum to 57 (TOOL 8 + PRIM 18 + VBTN 6 + VRNG 9 + VSEL 4 + VLST 4 + SHW 2 + VER 6). Corrected during roadmap creation; see updated Traceability table in REQUIREMENTS.md.

Research flags carried into planning: Phases 15, 16, 18 likely need `/gsd:research-phase` (toolchain-pairing gate outcome unknown, dropShadow/innerShadow + M3 Slider-slot spikes, Range's multiple moving parts). Phases 17, 19, 20 use well-established patterns and can be planned directly.

## Baseline Findings — why these eight look Material (surveyed 2026-07-21)

**None of the eight uses any glass modifier for its own surface.** The only glass in these files is the drag tooltip pill in the two sliders.

| Component | Current surface | Aero gaps |
|-----------|-----------------|-----------|
| `AeroButton` | M3 `Button`, flat `primary@0.8f`, `RoundedCornerShape(4.dp)` | no gradient, no border at rest, no gloss; hover = flat `0x40FFFFFF` `drawRect` over the **whole unclipped rect** (corners included) |
| `AeroOutlinedButton` | M3 `OutlinedButton`, transparent, 1.dp `glassBorder` | no fill, no gradient, no gloss |
| `AeroSwitch` | two plain Boxes (no M3 `Switch`) | fully flat; no border, no thumb shadow, no gloss; **no hover/press/focus at all** |
| `AeroSegmentedControl` | plain `Row` + 1.dp border | selected segment is flat `primary@0.3f`; no raised/pressed bevel; **no hover, no focus** |
| `AeroSlider` | M3 `Slider` + `SliderColors` | entirely M3-drawn track/thumb; no groove, no gloss, no hover/focus |
| `AeroRangeSlider` | custom `Canvas` (M3 banned per PITFALL-03) | flat 4.dp `drawLine` tracks, flat `drawCircle` thumbs + ring; no gradient/shadow/rim; no hover/press |
| `AeroProgressBar` | plain nested Boxes | entirely flat; no gradient fill, no trough inner shadow, no border, no animated sheen |
| `AeroListItem` | plain `Row`, `.background(animatedBg)`, **not clipped** | hard-edged full-bleed flat highlight; no rounded selection pill, no gradient, no focus visual |

**Glass layer defects to fix (part of this milestone):**

- `glassEffect(elevation = …)` — parameter is **dead**; `shadow` is imported but never applied. Every `elevation = 2.dp` call site is a no-op.
- `glassSurface` — gloss gradient is hardcoded `endY = 100f` **pixels**, not proportional, so it never completes on short controls and is a thin band on tall ones.
- `glassSurface` — `drawBehind` runs before `.clip(shape)` and the 1.dp stroke is bounds-centred, so its outer half is clipped away → effectively a 0.5.dp border.

## Toolchain Upgrade — MANDATORY (locked 2026-07-21)

- **Target version LOCKED: Compose Multiplatform `1.11.1` + Kotlin `2.4.10`.** Pairing was never shipped matched by JetBrains (six weeks apart); Phase 15's first task is a bare `./gradlew build` to settle this empirically.
- **Fallbacks, in order, if the gate fails (escalate to user, never pick silently):** (a) Kotlin 2.4.10 + CMP `1.12.0-beta02` (matched, prerelease); (b) CMP 1.11.1 + newest Kotlin it accepts; (c) CMP `1.9.3` + Kotlin unchanged.
- `compose.material3` alias resolves to alpha at 1.11.x — must pin explicit stable coordinate.
- CMP 1.11.0 deprecates `runComposeUiTest` v1 and flips default `TestDispatcher` `Unconfined`→`Standard`; `AeroPanelGroupRecomposeUiTest` (sole v2.0.4 RCMP regression guard) must be ported AND re-proven to fail on unfixed code post-port.
- Consumer compatibility (`aska`, `satellite-control`) is explicitly NOT a constraint — they migrate separately or stay on 2.0.4.

## Architecture positions locked for planning

- `AeroSlider` KEEPS Material3's `Slider` and supplies custom `thumb =`/`track =` slots (verified: both slots are long-stable, not new-in-1.4/1.5) — this downgrades what was flagged as the milestone's one HIGH-complexity outlier to MEDIUM. Spike required in Phase 16 to confirm custom-sized slots don't clip/misalign inside M3's internal layout math; if it fails, fallback is full M3 removal + reuse of `AeroRangeSlider`'s drag pattern (re-promotes Phase 18 to HIGH complexity).
- `AeroButton`/`AeroOutlinedButton` drop M3's `Button`/`Surface` container but KEEP `Modifier.clickable(role = Role.Button, indication = null, ...)` — lower risk than the `AeroRangeSlider` zero-semantics precedent.
- One `internal fun DrawScope.drawAeroSurfaceCore(style, cornerPx)`, exposed as `Modifier.aeroSurface(style, shape)` (Box-owning) and direct calls (Canvas-owning, e.g. `AeroRangeSlider`). One implementation, not three.
- `AeroOrnamentTokens.derive(base)` uses RGB lighten/darken (Classic's tokens are opaque; alpha manipulation doesn't work), plus a trailing `ornamentOverride: AeroOrnamentTokens? = null` escape hatch — source-compatible.
- **Wider blast radius than eight components:** fixing `GlassModifiers.kt` re-renders the ~40 out-of-scope components sharing those modifiers. PRIM-17's full-library smoke pass is a Phase 16 EXIT item, not deferred to Phase 20.
- **Design decisions locked:** `AeroProgressBar` sheen optional, default OFF; indeterminate keeps 1500ms restart timing, no ping-pong; `AeroSegmentedControl` selected segment is recessed/pressed (reuses pressed-button code), never raised; `AeroListItem` bottom mirror reflection deferred out of scope.

## Performance Metrics

**v1.0:** 26 plans, ~3 days, average ~7–25 min per plan.
**v1.1:** 11 plans, single-day push (2026-04-29, ~10 h), 60 commits, 340 files changed, +20,212 / −477 lines.
**v2.0:** 55 plans, ~49 days, 145 commits, 152 files changed, +27,406 / −2,285 lines.
**v2.0.1:** 4 plans, single-day push (2026-06-22, ~2h20m), 25 commits, 9 code files changed, +520 / −14 lines.
**v2.0.2:** 8 plans (Phases 13 + 13.1), ~1-day push (2026-06-22→23), 49 commits, 4 code files, +1,516 lines.
**v2.0.4:** 3 plans, single-day push incl. corrective release (2026-06-25→26), real RCMP root-cause fix.
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 15 P02 | 15min | 2 tasks | 3 files |
| Phase 15 P03 | 5min | 3 tasks | 1 files |
| Phase 15 P04 | 25min | 3 tasks | 7 files |
| Phase 15 P05 | 20min | 1 tasks | 1 files |
| Phase 15 P06 | 8min | 2 tasks | 0 files |
| Phase 16 P01 | 20min | 1 tasks | 11 files |
| Phase 16 P03 | 10min | 1 tasks | 2 files |
| Phase 16 P04 | 8min | 1 tasks | 6 files |
| Phase 16 P02 | 32min | 2 tasks | 3 files |
| Phase 16 P05 | 2h10min | 2 tasks | 6 files |
| Phase 17 P01 | 20min | 2 tasks | 4 files |
| Phase 17 P02 | 13min | 2 tasks | 4 files |
| Phase 17 P03 | 15min | 2 tasks | 5 files |
| Phase 17 P04 | 8min | 1 tasks | 1 files |
| Phase 17 P05 | 39min | 1 tasks | 6 files |
| Phase 18-range P01 | 1h04min | 2 tasks | 4 files |
| Phase 18-range P02 | 19min | 2 tasks | 2 files |
| Phase 18 P03 | 7min | 2 tasks | 2 files |
| Phase 18-range P04 | 19min | 2 tasks | 5 files |
| Phase 19 P01 | 3min | 2 tasks | 3 files |
| Phase 19 P02 | 8min | 2 tasks | 4 files |
| Phase 19 P03 | 21min | 2 tasks | 5 files |
| Phase 19 P05 | 15min | 2 tasks | 5 files |
| Phase 19 P06 | 24min | 2 tasks | 3 files |
| Phase 19 P07 | 22min | 3 tasks | 3 files |
| Phase 19 P08 | 12min | 2 tasks | 3 files |
| Phase 19 P09 | 20min | 2 tasks | 5 files |
| Phase 19 P10 | 18min | 2 tasks | 2 files |
| Phase 19 P11 | 20min | 3 tasks | 7 files |
| Phase 19 P12 | 10min | 2 tasks | 2 files |
| Phase 20 P01 | 11min | 2 tasks | 2 files |

## Accumulated Context

### Roadmap Evolution

- **v3.0 ROADMAP.md created (2026-07-21):** Six phases (15–20), dependency-justified per research SUMMARY.md and re-validated against REQUIREMENTS.md during roadmap creation — the six-phase proposal held up unchanged (standard granularity, 5-8 typical, fits at 6). Toolchain (15) isolated first per hard constraint; Foundation (16) before all components; Buttons (17) before Selectors (19, `AeroSegmentedControl` reuses pressed-button fill); Range (18) before Selectors (19, `AeroSwitch` needs Range's proven thumb/groove primitives); Verification (20) last, consolidated. 57/57 requirements mapped (corrected from REQUIREMENTS.md's stale "53 total" traceability note — actual per-category REQ-ID counts sum to 57).
- Phase 14 added for v2.0.3 (patch milestone, single phase per user scope): horizontal-controlled recompose-during-drag duplication fix + JitPack release. All 8 requirements (RCMP-01..04, REG-01..02, REL-01..02) map to Phase 14 — 100% coverage. No `/gsd:research-phase` — direct edit to existing code, mirrors Phase 13/v2.0.1 patch precedent. (ROADMAP created 2026-06-25)
- Phase 13.1 inserted after Phase 13: AeroPanelGroup horizontal orientation variant (PNL-HORIZ-01) — side-by-side columns, vertical dividers, drag resizes width; orthogonal to the vertical/stacked AeroPanelGroup shipped in Phase 13. (INSERTED 2026-06-23)

### Decisions

Full decision log in PROJECT.md "Key Decisions" table. Active decisions affecting current work — see "Toolchain Upgrade" and "Architecture positions locked" sections above for v3.0-specific ones. Carried forward from prior milestones:

- `undecorated=true` BEZ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION rule (locked since Phase 1); extends to ALL Popup/Dialog
- Glass effect in single `drawBehind` block — performance baseline
- `detectDragGestures` banned for Canvas-based drag on Compose Desktop — use `awaitPointerEventScope` + manual loop (PITFALL-03)
- `AeroScrollArea` banned inside DataTable / TreeView — raw `LazyListState + AeroScrollBar` (PITFALL-01)
- Pattern 3 (`AeroPanelGroup` precedent) is the locked answer for "animation vs. drag write the same value" — reused explicitly by VRNG-09 in Phase 18
- Builder/DSL lambdas that side-effect into a collection must NOT be `@Composable` (v2.0.4 RCMP root cause)
- A regression guard must provably FAIL on unfixed code before it counts as a guard (v2.0.3 false-positive lesson) — directly encoded as TOOL-04 and VER-06 in v3.0
- [Phase 15]: Kotlin 2.4.10 + CMP 1.11.1 build gate passed on first bundled attempt (three known-safe fixes together); no fallback/escalation needed
- [Phase 15]: Kept v1 runComposeUiTest for AeroPanelGroupRecomposeUiTest (deprecated-but-compiling at CMP 1.11.1); live re-proof confirmed the guard is not inert (FAIL with 11 headers on reverted fix, PASS with 1 on restored fix), closing TOOL-04
- [Phase 15]: Full 232-test suite green (incl. 12 PanelGroupLogicTest) + human-confirmed no-drift visual verdict (pixel-diff corroborated) across three themes vs plan-01 baseline — migration proven behaviorally and visually inert (TOOL-05/TOOL-06)
- [Phase 15]: TOOL-07: research signature's package for dropShadow/innerShadow (androidx.compose.ui.graphics.shadow) was wrong; corrected via javap bytecode inspection to androidx.compose.ui.draw, recorded verbatim in ScratchAeroShadowProof.kt KDoc as the Phase 16 source of truth
- [Phase 15]: Used a throwaway pre-release tag (v3.0.0-alpha01) to prove the JitPack build instead of bumping build.gradle.kts, keeping the locked bump-on-milestone rule intact
- [Phase 15]: jitpack.yml left unchanged - the actual JitPack build log confirmed openjdk17 is sufficient for Kotlin 2.4.10 + CMP 1.11.1, closing TOOL-08
- [Phase 16, Plan 01]: AeroOrnamentTokens field set and lighten/darken magnitudes taken verbatim from 16-RESEARCH.md as calibration starting points, re-reviewed at 16-05 sign-off
- [Phase 16, Plan 01]: drawAeroSurfaceCore called from inside Modifier.aeroSurface's drawWithCache/onDrawBehind per 16-RESEARCH.md Pattern 1's literal skeleton, preserving the single-function four-exposure-path architecture over stricter per-call brush caching
- [Phase 16, Plan 03]: glassEffect(elevation) revived via dropShadow (source-compatible) not removed - both elevation=2.dp call sites (AeroSlider, AeroRangeSlider) keep compiling; 16-05 confirms revive or requests removal per D-03
- [Phase 16, Plan 03]: glassSurface gloss fraction set to 0.32 (D-01's ~30-35% band), deliberately distinct from glassPanel's 0.55 - only the size.height * fraction idiom was copied, not the value
- [Phase 16, Plan 03]: glassPanel also migrated to drawWithCache alongside glassSurface (Rule 2 - threat model T-16-04 names both functions for the per-frame-allocation mitigation), zero visual/value change
- [Phase 16, Plan 04]: rememberAeroInteractionState(source) added at components/common — booleans only (hovered/pressed/focused), no color resolution, matching the PRIM-15 drift-risk prohibition
- [Phase 16, Plan 04]: InteractionStates.kt relocated components/buttons -> components/common (single source of truth); AeroButton/AeroOutlinedButton/AeroIconButton imports updated, behavior unchanged
- [Phase 16, Plan 02]: aeroGlowRing/aeroThumbSurface/aeroGroove all reuse drawAeroSurfaceCore via style.copy() field swaps (thumb: cornerPx=radiusPx collapses to circle; groove: fillTop/fillBottom + bevelLight/bevelShadow swapped, gloss off) - zero bespoke gradient implementations
- [Phase 16, Plan 02]: D-02 resolved manual-gradient (not native dropShadow/innerShadow) for aeroGlowRing's double-stroke and the groove's inner-shadow cue, matching drawAeroSurfaceCore's existing all-gradient approach
- [Phase 16, Plan 05]: PRIM-18 spike PASS (bytecode-proven + human-confirmed) - AeroSlider keeps Material3 Slider + custom thumb=/track= slots at MEDIUM complexity for Phase 18, not full M3 removal
- [Phase 16, Plan 05]: D-03 CONFIRMED - glassEffect(elevation)'s revived dropShadow (wired Plan 03) stays final; improves cards/panels across all three themes without noise
- [Phase 16, Plan 05]: aeroGlowRing was invisible in the gallery (radial-gradient bloom clipped by aeroSurface's own .clip + same-hue-family hoverGlow) - fixed via multi-ring concentric bloom, USAGE CONTRACT (apply outside any clip), and two intensity soften passes; final hoverGlow=primary.lighten(0.30f), now a documented binding rule for Phase 17-19 components
- [Phase ?]: [Phase 17, Plan 01]: AeroSurfaceStyle.rest(colors, cornerRadius=4.dp) used verbatim for the tracer rest-state - D-01 accent-identity finding confirmed no bespoke accent-override style needed
- [Phase ?]: [Phase 17, Plan 01]: Glow-before-surface modifier ordering (aeroGlowRing x2 then aeroSurface) proven end-to-end and locked as the pattern all future Aero components composing these two primitives must follow
- [Phase ?]: [Phase 17, Plan 02]: Disabled-fill blend uses androidx.compose.ui.graphics.lerp (not Color.lighten/darken) to match UI-SPEC's literal two-step lerp wording, verified correct on Classic's opaque tokens via a dedicated Classic-safety test
- [Phase ?]: [Phase 17, Plan 02]: pressedRecess/flattenDisabled/hoverLighten placed in theme/AeroSurfaceStyle.kt (not components/buttons/) specifically so Phase 19's AeroSegmentedControl can import pressedRecess cross-package without a reach-around
- [Phase ?]: [Phase 17, Plan 03]: outlinedStyle() implemented verbatim per 17-RESEARCH.md Pattern 5 formula (fill alpha x0.15, gloss proportionally scaled 0.15/0.32, rim literal 0.85) — placed in components/buttons/AeroButtonSurface.kt, not theme/, since it has no Phase 19 cross-package reuse need unlike pressedRecess
- [Phase ?]: [Phase 17, Plan 03]: Pitfall 6 compile-proof spike done via direct javap bytecode inspection of ui-test-desktop-1.11.1.jar/ui-desktop-1.11.1.jar (mirrors TOOL-07 precedent) rather than a throwaway scratch composable - performKeyInput/pressKey/Key.Enter/Key.Spacebar/requestFocus all resolved exactly as assumed
- [Phase ?]: [Phase 17, Plan 03]: VBTN-03/VBTN-06 source-scan guards proven to FAIL against deliberately-reintroduced broken code (temporary local edits, reverted before commit) before being trusted, per the v2.0.3 false-positive-sign-off lesson
- [Phase ?]: [Phase 17, Plan 04]: No forced-state static styling added for hover/press/focus - the live three-theme switcher plus real mouse/keyboard makes these transient states genuinely reviewable without bypassing resolveButtonStyle's real state-resolution path
- [Phase ?]: [Phase 17, Plan 04]: Long-label truncation demo needed an explicit Modifier.width(120.dp) on the demo instance only - AeroButtonSurface's Box has no width constraint of its own, so without a width cap the label would widen the button instead of truncating
- [Phase ?]: [Phase 17, Plan 05]: Human three-theme x five-state sign-off APPROVED for AeroButton/AeroOutlinedButton after two calibration rounds closing rim brightness, filled-fill contrast, and disabled-legibility defects the gate itself caught
- [Phase ?]: [Phase 17, Plan 05]: Rim alpha capped via min(native glassBorder alpha, 0.45) rather than a flat constant, so Classic's naturally dimmer rim is not forced down to match AeroBlue/AeroDark
- [Phase ?]: [Phase 17, Plan 05]: Filled-button fill darkening (primary.darken 0.20f/0.36f) scoped to AeroButtonSurface's own resolution only, not pushed into shared AeroOrnamentTokens/AeroSurfaceStyle.rest() defaults, to avoid darkening every other accent-derived surface
- [Phase ?]: [Phase 17, Plan 05]: flattenDisabled's terminal blend target changed from light borderDefault to theme's own base.surface (blend 0.4->0.5) so disabled buttons recede into background instead of standing out as light-gray
- [Phase ?]: [Phase 18, Plan 01]: AeroSlider thumb/track dp sizing (20.dp/4.dp) matches AeroRangeSlider's locked dimensions rather than M3's raw SliderTokens (4dp x44dp pill, 16dp track) — M3's default shape is incompatible with aeroThumbSurface's circle-only primitive
- [Phase ?]: [Phase 18, Plan 01]: Focus and press/drag glow rings diverge in hue (borderSelected vs onSurface-derived neutral), not just intensity — same-hue-family intensity-only differentiation was the round-1 human-sign-off defect root cause, since hoverGlow and borderSelected are both primary-derived
- [Phase ?]: [Phase 18, Plan 01]: onValueChangeFinished wired as null through SliderState internally, no new AeroSlider public parameter — resolves 18-RESEARCH.md Open Question 1, keeps VRNG-02's 1:1 public-signature constraint
- [Phase ?]: DrawScope.inset(left,top,right,bottom) chosen for AeroRangeSlider's sub-region Canvas draws (groove/active-segment/thumbs) — unlike translate(), inset() also shrinks the receiver's size, which drawAeroSurfaceCore/drawAeroThumb depend on
- [Phase ?]: Per-thumb hover on AeroRangeSlider's Canvas uses MutableInteractionSource.tryEmit (non-suspend), not emit — AwaitPointerEventScope is @RestrictsSuspension and forbids calling the suspend emit() from inside its loop
- [Phase ?]: [Phase 18, Plan 03]: AeroProgressBar has no enabled/disabled state and none was added - plan's must_haves forbid new public API beyond showRunningSheen; UI-SPEC's Disabled row scopes to AeroSlider/AeroRangeSlider only
- [Phase ?]: [Phase 18, Plan 03]: Indeterminate edge-fade implemented as a BlendMode.DstIn overlay chained after aeroSurface(...) rather than modifying drawAeroSurfaceCore - keeps the shared primitive untouched, localizes the horizontal-fade need to the one component requiring it
- [Phase ?]: [Phase 18, Plan 04]: AeroSlider is the cross-component reference for focus/press-drag glow treatment, not AeroRangeSlider - human sign-off corrected an initially-reversed direction mid-checkpoint (b9cbc08 reverted via ecb544b), then AeroRangeSlider was given AeroSlider's per-thumb glow-ring + 1.05x lift via a new DrawScope.drawAeroGlowRing direct-Canvas primitive
- [Phase ?]: [Phase 18, Plan 04]: AeroRangeSlider's keyboard-focus ring is human-accepted as deferred out of scope - its Canvas has no keyboard-focus tracking at all (pre-existing VRNG-04 accessibility deferral); adding one would be new per-thumb keyboard-navigation logic beyond this render-only restyle
- [Phase ?]: [Phase 19, Plan 01]: D-11 base-then-transform resolver implemented exactly as specified (selected resolves base FIRST, hoverLighten composes SECOND) - verified byte-for-byte via AeroListItemStylesTest, structurally eliminating the VLST-02 selection-suppresses-hover bug class
- [Phase ?]: [Phase 19, Plan 01]: D-13 in-bounds inset focus stroke used instead of aeroGlowRing for AeroListItem - list rows live inside scrolling/clipping containers that would slice an outer bloom
- [Phase ?]: [Phase 19, Plan 02]: resolveSwitchGrooveStyle/resolveSwitchThumbStyle implemented mirroring resolveSliderThumbStyle's precedence chain minus isDragging; D-05 pressed brightens gloss instead of recessing, verified via strict glossAlpha inequality plus unchanged fill stops
- [Phase ?]: [Phase 19, Plan 02]: thumb Box kept as a sibling of the groove Box (D-03) so its drop shadow/glow escape the 18dp track clip; both aeroGlowRing calls chained before aeroGroove/aeroThumbSurface per the primitives ordering rule
- [Phase ?]: [Phase 19, Plan 03]: PRESSED_INNER_SHADOW widened private -> internal in AeroButtonSurface.kt, value unchanged, enabling cross-package reuse by AeroSegmentedControl (VSEL-03)
- [Phase ?]: [Phase 19, Plan 03]: resolveSegmentStyle folds pressed into an effectiveProgress (1f when pressed, else selectedProgress) so an unselected-but-pressed segment reaches full recess immediately, matching the styles-test behavior spec exactly
- [Phase ?]: [Phase 19, Plan 03]: selected segment label switches to colors.surface (not colors.primary) for legibility over the recessed accent fill; unselected labels stay colors.onSurface
- [Phase ?]: [Phase 19, Plan 03]: 1.dp inter-segment separator dropped (D-08/D-10) - each segment's own raised/recessed bevel/rim contour now supplies the visual break
- [Phase ?]: 19-05: interaction-derived FocusVisibility reducer (not LocalInputModeManager) gates AeroSwitch's focus glow ring, per this plan's planner_finding — platform focus-visible only gates Indication, which the library disables everywhere
- [Phase ?]: [Phase 19, Plan 06]: AeroListItem's row height changed from fixed .height(36.dp) to .heightIn(min = ROW_MIN_HEIGHT); pill/focus Boxes switched from fill-the-parent to matchParentSize() so they inherit the row's resolved (not fixed) size, closing gap G1
- [Phase ?]: [Phase 19, Plan 06]: AeroListItem's in-bounds focus stroke gated on state.focusVisible (19-05's shared mechanism) instead of the raw focused flag, closing gap G2 for the third of three components sharing that gap
- [Phase ?]: [Phase 19, Plan 07]: RECESSED_FILL_DARKEN = 0.20f applied via copy(...) to resolveSegmentStyle's recessed fillTop/fillBottom AFTER the imported pressedRecess(PRESSED_INNER_SHADOW) transform, matching AeroButtonSurface's own FILLED_FILL_TOP_DARKEN so the recessed segment lands in the same value neighbourhood as the pressed AeroButton; theme/ and buttons/ provably untouched (git status --porcelain gate)
- [Phase ?]: [Phase 19, Plan 07]: Segment label collapsed to one content token (colors.onSurface) in every state, deleting the per-segment animateColorAsState inversion that used colors.surface (a background token carrying 0xCC alpha on AeroBlue/AeroDark) — closing gap G3's label fault; contrast restored by darkening the recessed fill instead of re-inverting the label
- [Phase ?]: [Phase 19, Plan 07]: AeroSegmentedControl's per-segment focus stroke gated on state.focusVisible (19-05's shared reducer), the third and final Phase 19 component to adopt it, closing gap G2 across AeroSwitch/AeroListItem/AeroSegmentedControl
- [Phase ?]: [Phase 19, Plan 08]: Three-theme re-sign-off APPROVED — G1/G2/G3 confirmed closed by eye on AeroBlue, AeroDark and Classic; no defects routed back to 19-05/06/07
- [Phase ?]: [Phase 19, Plan 08]: G4 (AeroOrnamentTokens brightness on AeroBlue/AeroDark) re-confirmed but stays deferred by explicit reviewer decision — routes to a separate Phase 16 foundation session, with a new direction to try darker values specifically for those two themes
- [Phase ?]: [Phase 19, Plan 08]: RECESSED_FILL_DARKEN = 0.20f (19-07's open judgement call) confirmed correct by the reviewer ('в самый раз' / just right) — kept unchanged, no follow-up tuning
- [Phase ?]: [Phase 19, Plan 09]: Reducer fix scoped to FocusVisibility(hovered = hovered) one-argument change - WR-04's counted-hover field-shape alternative deliberately not taken here, deferred to 19-11
- [Phase ?]: [Phase 19, Plan 09]: AeroButtonSurface focus glow moved to state.focusVisible, matching AeroSwitch/AeroSegmentedControl/AeroListItem - closes WR-01, resolveButtonStyle's dead focused parameter left untouched
- [Phase ?]: [Phase 19, Plan 10]: RAISED_FILL_TOP_DARKEN/RAISED_FILL_BOTTOM_DARKEN landed at 0.45f/0.61f (colors.primary darken, applied before pressedRecess) - materially darker than AeroButtonSurface's 0.20f/0.36f precedent because this label is locked to the on-surface content token; new 12-assertion WCAG contrast test (MIN_LABEL_CONTRAST=3f) gates the regression closing CR-01
- [Phase ?]: [Phase 19, Plan 11]: Segment key uses index-and-value, not value alone - duplicate options remain unguarded (WR-07 deferred) so a value-only key could collide
- [Phase ?]: [Phase 19, Plan 11]: Three different hover-emitter removal treatments (switch: delete outright, segmented control: delete per-segment/keep outer, list row: move into else-branch) because each component's OTHER interaction modifier relates to the shared source differently
- [Phase ?]: [Phase 19, Plan 11]: WR-04 premise (toggleable/selectable/clickable emit hover on their own) proven via new HoverEmissionTest suite against the library's own Compose build BEFORE removal, per plan's explicit fallback-clause requirement - all 3 premise tests and 2 post-removal component tests passed on first run
- [Phase ?]: [Phase 19, Plan 12] Gate NOT approved this round — blocks A-F (CR-01/CR-02/WR-01/WR-03/WR-04) PASSED, block G FAILED opening gap G5
- [Phase ?]: [Phase 19, Plan 12] G5: AeroSegmentedControl's raised fill (CR-01/WR-12 darken, chasing contrast vs colors.onSurface) has drifted into a bespoke colour unlike AeroButton's own on-fill token; fix direction is to unify with AeroButton/AeroButtonSurface's code path, not further retune segment-specific constants
- [Phase ?]: [Phase 19, Plan 12] Depth/recess judgement explicitly accepted by the developer and excluded from G5's scope
- [Phase ?]: Tracer checkpoint (Task 1) approved as-is - START coords stay outside D-05, named constant carrying no size. is a violation, three-layer shape (pure detector -> in-file fixtures -> real-source scan) locked as the template all later gates replicate
- [Phase ?]: VER-02 modifierChains() excludes fun-declaration lines from chain-start detection - without this, AeroSurfacePrimitives.kt's own fun Modifier.aeroSurface(...) declaration would self-flag its internal .clip(shape) as a false VER-02 violation

### Pending Todos

- Gap-close: AeroDropdown popup offset regression (v1.0 carry-over) — explicitly OUT of v3.0 scope; candidate for future milestone (DROP-FIX-01)
- Deferred to future milestones: inline pickers, DataTable cell-edit/reorder/filter, TreeView DnD, ColorPicker eyedropper, StepperWizard branching, Sidebar drag-resize, AeroDateTimeRangePicker hover-preview (DTR-HOVER-01), AeroPanelGroup drag-to-reorder (PNL-REORDER-01), nested AeroPanelGroup first-class API (PNL-NEST-01), keyboard resize (PNL-KBD-01)
- v3.0 Future Requirements (deferred, see REQUIREMENTS.md): VLST-F01 (list-item mirror reflection), VRNG-F01 (Win7 ping-pong indeterminate), VIS-F01 (visual sweep of remaining ~40 components)

### Blockers/Concerns

- **Phase 15 gate is genuinely unverified until run:** the Kotlin 2.4.10 + CMP 1.11.1 pairing gate (`./gradlew build`) has no prior evidence either way. If it fails, escalate to the user with the three named fallbacks — do not pick one silently.
- **No real external consumer app tracks v3.0** (`aska`/`satellite-control` stay pinned to the old toolchain) — the project's historically strongest regression-catcher is unavailable this milestone. VER-05's minimal scratch-consumer step is the mitigation, not a full replacement.

## Session Continuity

Last session: 2026-07-28T16:38:02.208Z
Stopped at: Completed 20-01-PLAN.md (VER-01/VER-02 gates)
Resume file: None
Next action: `/gsd:plan-phase 15` (Toolchain Upgrade — consider `/gsd:research-phase 15` first per research flag)
