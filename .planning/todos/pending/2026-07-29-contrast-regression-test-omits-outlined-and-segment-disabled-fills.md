---
created: 2026-07-29T00:00:00Z
type: finding
title: "AeroButtonContrastRegressionTest's \"every fill is covered\" claim omits outlined-button-disabled and segment-disabled fills"
area: testing
severity: minor
files:
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt:28-33
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt:353-392
  - library/src/test/kotlin/com/mordred/aero/verification/VER10OneLabelColorPerThemeTest.kt:211-238
---

## Problem

Filed from **WR-05** in `20-REVIEW.md`'s addendum 2 (commit range `f07cc48..HEAD`, closed
2026-07-29).

`AeroButtonContrastRegressionTest`'s class KDoc states: "Every fill an on-fill label can land on is
covered: filled button rest/hover/press (both stops, all three themes), outlined equivalents,
raised segment rest/hover, and recessed segment rest/hover ... Disabled states are WCAG-1.4.3-exempt
(below) rather than asserted against the floor." In practice, only ONE disabled fill is measured
anywhere in this file: `disabledSurfacesAreExemptFromTheContrastFloorPerWcag143` calls
`resolveButtonStyle(..., outlined = false, ..., enabled = false)` for all three schemes — **filled
button disabled only.** Neither **outlined button disabled** nor **any segment disabled state**
(raised-disabled or recessed-disabled) is measured, regression-bounded, or even exempted-with-a-
named-ratio anywhere in this file. `AeroSegmentedControl` does support `enabled = false` (its
`enabled` parameter flows into `resolveSegmentStyle`'s
`if (!enabled) return depthResolved.flattenDisabled(colors)` branch,
`AeroSegmentedControl.kt:304`), so this is a real, reachable, currently-unmeasured fill.

`VER10OneLabelColorPerThemeTest`'s `fillCases()` — the file explicitly built to enumerate "EVERY
fill an on-fill label actually lands on today" for its own per-scheme worst-case measurement — DOES
include `outlined`-`disabled` (its `listOf(false, true).forEach { outlined -> ... }` loop calls
`resolveButtonStyle(..., enabled = false)` for both `outlined` values), but has **no segment-disabled
case at all**: its five segment cases are `raised`, `raisedHover`, `recessed`, `recessedHover`,
`pressedSegment` — never `resolveSegmentStyle(..., enabled = false)`. So segment-disabled contrast is
entirely unmeasured by either mechanism in this codebase, and outlined-button-disabled is folded only
into VER10's aggregate per-scheme *worst-case-across-all-fills* number — which would only change (and
therefore only catch a regression) if outlined-disabled happens to become the single worst fill in
that scheme; a regression that makes outlined-disabled contrast worse while still not being the
scheme's global worst case would silently pass both files unnoticed.

WCAG 1.4.3 genuinely exempts disabled controls from the contrast floor, so this is **not a compliance
gap** — it is a gap in the "regression-bounded" contrast-guard mechanism's own stated completeness
claim: an omission (rather than an explicit, named carve-out) in a mechanism this milestone built
specifically so that "any observed change is a real regression, not a legitimate ... crossing." A
future fill-formula change to the segment's or outlined button's disabled state could silently make
disabled text meaningfully worse (well past what a reasonable person would call "still exempt but
fine") without either file's assertions moving.

## Solution

Either of two dispositions closes this — the reviewer's own framing, both reasonable:

1. **Widen coverage** — add an `outlinedButtonDisabledClearsOrIsExemptInAllThreeThemes`-style
   assertion to `AeroButtonContrastRegressionTest.kt` mirroring
   `disabledSurfacesAreExemptFromTheContrastFloorPerWcag143`'s existing shape (measure and pin the
   current ratio as a regression bound, same as the filled-disabled case already does), and add a
   `resolveSegmentStyle(..., enabled = false)` case to both this file's fixture set and VER10's
   `fillCases()`.
2. **Narrow the claim** — reword `AeroButtonContrastRegressionTest`'s class KDoc "Every fill ... is
   covered" to explicitly name which disabled fills are and are not measured, so the coverage claim
   matches the actual coverage. The cheapest correct fix may well be this direction rather than
   widening the test, since WCAG 1.4.3 does not require these fills to be measured at all — the gap
   is in the claim's precision, not in product compliance.

Not fixed as part of the WR-04 fix this todo was filed alongside — WR-05 is a test-coverage/
documentation-accuracy gap, not a shipped-contrast defect, and is independent of WR-04's fallback
fix.
