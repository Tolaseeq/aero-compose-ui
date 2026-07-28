# Phase 20: Verification - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-28
**Phase:** 20-verification
**Areas discussed:** Folded todos, Sign-off shape + DPI, Grep gates VER-01/02, VER-03 baseline, Scratch consumer VER-05

---

## Folded Todos

Matched by `todo.match-phase 20` at score 0.9: "AeroButton label-to-fill contrast is below the WCAG
4.5:1 floor".

| Option | Description | Selected |
|--------|-------------|----------|
| Fold into Phase 20 | Fix + value-level contrast test enter the phase plan; Phase 20's sign-off re-reviews AeroButton on three themes anyway, so no separate round is needed | ✓ |
| Measure only | Add the contrast test, freeze current values as known debt, leave the visual alone | |
| Keep deferred | Goes to backlog / next milestone; Phase 20 records it as a known open issue | |

**User's choice:** Fold into Phase 20.
**Notes:** The todo was deferred at G5 *because* fixing it needs fresh sign-off — Phase 20 is that
sign-off, making this its cheapest moment.

---

## Sign-off shape + DPI

### Scope of the human sign-off (SHW-16)

| Option | Description | Selected |
|--------|-------------|----------|
| Hybrid | Coherence pass over all eight × three themes, plus a full state matrix only for what changed after its own sign-off (both buttons, incl. the contrast fix). Range not re-reviewed state-by-state | ✓ |
| Full matrix again | All 8 × all states × 3 themes, no trust in prior sign-offs | |
| Coherence only | All eight side by side × 3 themes; no per-state re-review at all | |

**User's choice:** Hybrid.
**Notes:** Grounded on a scan finding presented before the question — `AeroButton`/`AeroButtonSurface`
were edited *after* Phase 17's sign-off (19-09's WR-01 focus gate, the G5 unification), while
`AeroSlider`/`AeroRangeSlider`/`AeroProgressBar` have been untouched since 18-04.

### Non-100% DPI pass

| Option | Description | Selected |
|--------|-------------|----------|
| 125% + 200%, one theme | Fractional scale for 1dp contours/seams/bevels; coarse scale for gloss and gradient proportionality | ✓ |
| 125% only, one theme | Requirement minimum, nastiest single scale, large scale left unproven | |
| 150% on three themes | One scale, whole colour matrix — if the worry is scale × Classic's opaque tokens | |

**User's choice:** 125% + 200% on one theme.

### Gate ordering

| Option | Description | Selected |
|--------|-------------|----------|
| Review before sign-off | `/gsd-code-review` over the Phase 16–19 diff, findings closed before the human looks | ✓ |
| Sign-off before review | Phase 19's order — eye first, review as a second gate | |
| No review in Phase 20 | Trust the per-phase reviews (18 and 19 each had one) | |

**User's choice:** Review before sign-off.
**Notes:** Decided against the precedent that Phase 19's sign-off PASSED at 19-08 and review then
opened CR-01/CR-02 + WR-01/03/04, costing four more plans and a re-sign-off.

### Showcase presentation (SHW-15)

| Option | Description | Selected |
|--------|-------------|----------|
| New section, permanent | All eight in one flow, stays in the repo for future visual milestones; existing sections only audited for state completeness | ✓ |
| Section for the gate only | Same page, deleted after sign-off to avoid duplicating showcase content | |
| Audit existing sections only | Nothing new; coherence judged by switching between four sections | |

**User's choice:** New section, permanent.
**Notes:** Prompted by the observation that the hybrid sign-off needs a screen showing all eight
together, and no such screen exists today.

---

## Grep gates VER-01/02

The first framing of the VER-01 question was not understood ("не понял вопроса"). It was re-asked
after showing the actual historical bug (`endY = 100f` in `glassSurface`) and its fix
(`size.height * 0.32f`) as code, with a table comparing what each detection strategy catches.

### VER-01 formulation

| Option | Description | Selected |
|--------|-------------|----------|
| Require `size.` (strict) | An explicit end stop must derive from `size.`; gradients with no explicit stops are legal. Catches both `endY = 100f` and the named-constant bypass `endY = GLOSS_END_PX`. One carve-out needed | ✓ |
| Search for numeric literals | Fails on an explicit number at the end of a gradient. Simple, no false positives, bypassed by a single named constant | |
| Ban all numeric literals | No exceptions, but 8 harmless `startY = 0f` sites would need rewriting, two of them in components this milestone does not touch | |

**User's choice:** Require `size.` — strict.

### VER-01 scope

| Option | Description | Selected |
|--------|-------------|----------|
| Whole library | All 18 gradient call sites across 9 files | ✓ |
| Eight targets + primitives | Gate strictly at the milestone boundary | |
| Whole library + showcase | Also the demo module | |

**User's choice:** Whole library.
**Notes:** Chosen after being shown that out-of-scope files are already compliant —
`AeroDrawer.kt:150` uses `size.height * 0.4f`, `AeroPopover.kt:75` uses `size.height * 0.55f` — so
library-wide coverage is expected to be green immediately and costs nothing.

### VER-02 formulation

Presented as a table of three distinct ways to bypass `aeroSurface()`'s centralized clip.

| Option | Description | Selected |
|--------|-------------|----------|
| First two | `aeroGlowRing` must precede `aeroSurface`; no component-authored `.clip(` after `aeroSurface(` in the same chain. Both checkable by source position, no exception lists | ✓ |
| All three | Plus banning hand-rolled `drawBehind` + `.clip` glass — needs an exception list for `GlassModifiers.kt`, the color picker and overlays, which draw that way legitimately | |
| Ordering only | Exactly the failure that occurred at 16-05 and cost a separate investigation | |

**User's choice:** First two.

### VER-06 fail-proof

| Option | Description | Selected |
|--------|-------------|----------|
| Fixture in the test | Detector extracted as a pure function over text, with violating and compliant fixture strings asserted alongside; the proof re-runs on every build | ✓ |
| Phase-17 style transcript | Temporarily break the real source, record FAIL→PASS in the SUMMARY, revert. Established precedent, but one-time and non-executable | |
| Both | Fixtures as the permanent guarantee plus one live run against the real file | |

**User's choice:** Fixture in the test.

**Not asked — settled by precedent:** gate mechanism is a Kotlin source-scan test in the normal
suite, following the six existing `*SourceTest.kt` files.

---

## VER-03 baseline

### Source of the expected values

| Option | Description | Selected |
|--------|-------------|----------|
| From tag v2.0.4 | The real pre-migration code, as VER-03 asks. `AeroListItem`'s fixed-36dp → min-36dp entered as an explicit exception citing the approved G1 closure | ✓ |
| Freeze current values | Simplest, no exceptions, but cannot by definition detect drift that already happened in Phases 15–19 | |
| From the tag, no exceptions | Would require reverting `AeroListItem` to a fixed 36dp and closing G1 differently — reopens an accepted decision | |

**User's choice:** From tag v2.0.4.
**Notes:** Presented alongside a manual pre-check showing public defaults held (button 30dp, switch
36×18 with a 14dp thumb, segment 28dp, progress 8dp), with the list row as the single real divergence.

### Measurement mechanism

Asked twice and interrupted both times; the user asked for a plain-language recap of the phase
instead. After the recap, **Claude took this as discretion** rather than pressing a third time — it
is a "how is the test written" question, not a "how should it look" question, and the user's standing
preference is to be asked identity questions, not implementation ones.

**Claude's call:** measure sizes from composed nodes, assert corner radii at value level (a radius
does not participate in layout and cannot be read off a composed node).

---

## Scratch consumer VER-05

### Where it lives

| Option | Description | Selected |
|--------|-------------|----------|
| Separate, via JitPack | Entirely outside the repo, pulls the library by tag — exactly the path a stranger takes. Precedent: 15-06's throwaway `v3.0.0-alpha01` tag | ✓ |
| Separate, via local Maven | Folder outside the repo, library published locally and pulled as a real dependency | |
| Third module in the repo | Beside `library` and `showcase`. Simplest, but builds together with everything and therefore proves nothing about the published artifact | |

**User's choice:** Separate, via JitPack.

### What it must prove

| Option | Description | Selected |
|--------|-------------|----------|
| Launches and renders | A window with all eight components, outside the showcase's internal conventions | ✓ |
| Compiles | Resolved and compiled is enough | |

**User's choice:** Launches and renders.

---

## Claude's Discretion

- How each gate test is written — detector shape, fixture strings, source-file resolution.
- VER-03's measurement mechanism (see above — taken as discretion after two interruptions).
- The direction and magnitude of the `AeroButton` contrast fix, inside the one-source-of-truth
  constraint the user imposed.
- Layout and grouping of the new review section; which states are shown statically vs. exercised live.
- The throwaway tag name used for VER-05's JitPack pull.
- Which missing states in the four existing sections are worth topping up.

**Constraint the user imposed on the contrast fix** (free-text answer, replacing the offered options):

> "Нельзя просто так взять и изменить бекграунд. Он должен быть ЕДИНЫМ для подобных элементов,
> например для сегментед контрола и кнопки, ЕДИНЫЙ СТИЛЬ. Сам решай, как будешь всё править, но
> должно быть не вырвиглазно и красиво и читаемо и единый стиль."

The two offered directions (explicit near-white label token vs. darkening the fill) were both
declined as framed — the answer is that whichever is chosen must move `AeroButton` and
`AeroSegmentedControl` together from one shared source of truth. Recorded as CONTEXT.md D-12.

## Deferred Ideas

- G4 — `AeroOrnamentTokens` brightness on AeroBlue/AeroDark; deferred at 19-08 to a separate Phase 16
  foundation session.
- `AeroRangeSlider` accessibility semantics + keyboard support.
- Arrow-key roving focus for `AeroSegmentedControl`.
- `AeroDropdown` popup-offset regression (DROP-FIX-01).
- VLST-F01, VRNG-F01, VIS-F01 — already-flagged future requirements.
- `AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton` — outside the eight-target set.
