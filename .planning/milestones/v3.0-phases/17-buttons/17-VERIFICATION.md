---
phase: 17-buttons
verified: 2026-07-23T15:32:01Z
status: passed
score: 6/6 must-haves verified
behavior_unverified: 0
overrides_applied: 0
---

# Phase 17: Buttons Verification Report

**Phase Goal:** `AeroButton` and `AeroOutlinedButton` read as genuine Aero glass controls with correct hover/press/focus/disabled states, while keeping keyboard activation and button semantics. Both variants paint their own Aero glass surface via one shared internal painter (no Material3 container), with all five interaction states, `Role.Button` + Space/Enter keyboard semantics, and the mandatory three-theme human visual sign-off passed.
**Verified:** 2026-07-23T15:32:01Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `AeroButton` renders a two-tone gradient fill with a visible seam, proportional top gloss, inner bevel, and outer contour at rest (Roadmap SC1 / VBTN-01) | VERIFIED | `AeroButtonSurface.kt` resolves `AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)` overridden to `primary.darken(0.20f/0.36f)` two-tone fill, chained through `.aeroSurface(style, RoundedCornerShape(4.dp))` (gloss/bevel/rim fields of `AeroSurfaceStyle`, `AeroSurfaceStyle.kt:23-41`). Confirmed by the mandatory 17-05 human three-theme sign-off (APPROVED, see truth 5) and `AeroButtonTest` render suite (3/3 themes green). |
| 2 | Hover shows glow-ring + fill brighten, press inverts the gradient with inner-shadow recess (no scale-shrink), focus is a persistent glow ring, disabled reads distinctly flattened — all Aero idiom; hover cue is clipped to the 4.dp corner (Roadmap SC2 / VBTN-02 / VBTN-03) | VERIFIED | `resolveButtonStyle()` applies `hoverLighten()`/`pressedRecess()`/`flattenDisabled()` with disabled-wins/press-over-hover precedence (`AeroButtonSurface.kt:244-279`); value-level tests `AeroButtonStylesTest` (7/7 green) assert the exact transforms. Both `aeroGlowRing` calls precede the single `aeroSurface()` call in the modifier chain (glow-before-surface ordering intact, `AeroButtonSurface.kt:87-105`) so the hover/focus bloom is clipped by `aeroSurface`'s own `.clip()`, and the no-unclipped-`drawWithContent`-overlay guard (`AeroButtonSurfaceSourceTest`, VBTN-03) passes on both button files. Confirmed visually at the 17-05 human sign-off. |
| 3 | `Role.Button` semantics and Space/Enter keyboard activation still work after the M3 container is dropped, via `Modifier.clickable(role = Role.Button, ...)` (Roadmap SC3 / VBTN-04) | VERIFIED | `AeroButtonSurface.kt:88-94` — `Box.clickable(role = Role.Button, indication = null, interactionSource = interactionSource, enabled = enabled, onClick = onClick)`. `AeroButtonSemanticsTest` (5/5 green): Role.Button + click-action assertions for both variants, Space and Enter each invoke `onClick` after `requestFocus()`. |
| 4 | `AeroOutlinedButton` shows the equivalent outlined-variant treatment and cannot visually drift from `AeroButton` because both consume one shared internal surface composable (Roadmap SC4 / VBTN-05 / VBTN-06) | VERIFIED | `AeroOutlinedButton.kt` delegates to `AeroButtonSurface(..., outlined = true)` with no painting of its own; `outlinedStyle()` (`AeroButtonSurface.kt:195-199`) is a fixed delta (fill α×0.15, gloss proportionally scaled, theme-aware rim) applied on top of the per-state filled resolution. `AeroOutlinedButtonStylesTest` (7/7 green) proves `outlined == outlinedStyle(filled)` for every enabled state, plus the disabled-ordering and rim-comparison invariants added at 17-05 calibration. `AeroButtonSurfaceSourceTest` proves both `AeroButton.kt`/`AeroOutlinedButton.kt` call only `AeroButtonSurface(` and neither calls `aeroSurface(`/`drawAeroSurfaceCore(` directly. |
| 5 | A human three-theme (AeroBlue/AeroDark/Classic) × five-state sign-off passes for both buttons before the phase is done (17-05 mandatory gate) | VERIFIED | `17-05-SUMMARY.md` records an APPROVED verdict reached after two operator-driven calibration rounds; the four fix commits (`60b0c23`, `eff570f`, `09aa7ad`, `7b2cd58`) are present in `git log` and their content (theme-aware rim alpha cap, `primary.darken` fill, `flattenDisabled` targeting `base.surface`) is live in the current `AeroButtonSurface.kt`/`AeroSurfaceStyle.kt` source, not just narrated. |
| 6 | The two structural source-scan guards (VBTN-03 no-unclipped-overlay, VBTN-06 single-shared-surface) still exist, still pass, and the calibration did not break the shared-surface architecture or the glow-before-surface modifier-chain ordering | VERIFIED | `AeroButtonSurfaceSourceTest.kt` (5 tests) present and green post-calibration. Modifier chain in current `AeroButtonSurface.kt` still reads `.height() → .clickable(role=Role.Button) → .aeroGlowRing(focus) → .aeroGlowRing(hover) → .aeroSurface(style, shape)` — unchanged ordering. Both `AeroButton.kt`/`AeroOutlinedButton.kt` remain thin wrappers with zero M3/`drawWithContent`/`graphicsLayer` remnants (grep returns no matches). |

**Score:** 6/6 truths verified (0 present-but-behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `library/.../buttons/AeroButtonSurface.kt` | Shared internal surface backing both buttons | VERIFIED | Exists, substantive (280 lines), wired from both public wrappers |
| `library/.../buttons/AeroButton.kt` | Thin public wrapper, M3 removed | VERIFIED | 45 lines, delegates 100% to `AeroButtonSurface(..., outlined = false)`, no M3/drawWithContent/graphicsLayer |
| `library/.../buttons/AeroOutlinedButton.kt` | Thin public wrapper, M3 removed | VERIFIED | 47 lines, delegates 100% to `AeroButtonSurface(..., outlined = true)`, no M3/drawWithContent/graphicsLayer |
| `library/.../theme/AeroSurfaceStyle.kt` | `pressedRecess`/`flattenDisabled`/`hoverLighten` pure transforms | VERIFIED | Present at lines 92-146, cross-phase-importable (`internal`, package `theme`) |
| `library/src/test/.../AeroButtonTest.kt` | Render + click smoke test, 3 themes | VERIFIED | 4/4 tests green |
| `library/src/test/.../AeroButtonStylesTest.kt` | Per-state pure value tests | VERIFIED | 7/7 tests green |
| `library/src/test/.../AeroOutlinedButtonStylesTest.kt` | Outlined fixed-delta tests | VERIFIED | 7/7 tests green |
| `library/src/test/.../AeroButtonSemanticsTest.kt` | Role.Button + keyboard test | VERIFIED | 5/5 tests green |
| `library/src/test/.../AeroButtonSurfaceSourceTest.kt` | VBTN-03/VBTN-06 source-scan guards | VERIFIED | 5/5 tests green |
| `showcase/.../ButtonsSection.kt` | Demo rows, both variants, all statically-reviewable states | VERIFIED | Enabled/disabled/long-label rows for both variants; live-state review note present; compiles |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `AeroButton.kt` | `AeroButtonSurface.kt` | `AeroButtonSurface(..., outlined = false)` call | WIRED | Confirmed by direct read + `AeroButtonSurfaceSourceTest` |
| `AeroOutlinedButton.kt` | `AeroButtonSurface.kt` | `AeroButtonSurface(..., outlined = true)` call | WIRED | Confirmed by direct read + `AeroButtonSurfaceSourceTest` |
| `AeroButtonSurface.kt` | `theme/AeroSurfaceStyle.kt` | `resolveButtonStyle` calls `pressedRecess`/`flattenDisabled`/`hoverLighten`/`AeroSurfaceStyle.rest` | WIRED | Confirmed by direct read |
| `AeroButtonSurface.kt` modifier chain | `theme/AeroSurfacePrimitives.kt` | `.aeroGlowRing(focus)` → `.aeroGlowRing(hover)` → `.aeroSurface(style, shape)` | WIRED | Ordering verified unchanged post-calibration |
| `showcase/ButtonsSection.kt` | `AeroButton`/`AeroOutlinedButton` | direct composable calls | WIRED | Confirmed by direct read; `:showcase:compileKotlin` green |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full library test suite green (single full run, not filtered per-truth) | `./gradlew :library:test -q` | Exit 0; test-results XML: 303 tests, 0 failures, 0 errors across the library (5 button test classes: 4+7+7+5+5 = 28 tests) | PASS |
| Showcase compiles against restyled buttons | `./gradlew :showcase:compileKotlin -q` | Exit 0, no errors | PASS |
| No M3 container / unclipped-overlay / scale-shrink remnants | `grep -E "material3\.Button\|material3\.OutlinedButton\|LocalMinimumInteractiveComponentSize\|graphicsLayer\|drawWithContent\|animateFloatAsState"` on `AeroButton.kt`, `AeroOutlinedButton.kt`, `AeroButtonSurface.kt` | No matches | PASS |
| Calibration commits present in history and content live in current source | `git log --oneline -- library/.../buttons/ library/.../theme/AeroSurfaceStyle.kt` | `60b0c23`, `eff570f`, `09aa7ad`, `7b2cd58` present; formulas (`min(glassBorder.alpha, 0.45)`, `primary.darken`, `base.surface` blend target) confirmed live in `AeroButtonSurface.kt`/`AeroSurfaceStyle.kt` | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| VBTN-01 | 17-01, 17-02, 17-05 | Two-tone fill, seam, gloss, bevel, contour | SATISFIED | `AeroButtonSurface.kt` style resolution; human sign-off |
| VBTN-02 | 17-02, 17-04, 17-05 | hover/press/focus/disabled Aero-idiom states | SATISFIED | `resolveButtonStyle` + transforms; `AeroButtonStylesTest`; human sign-off |
| VBTN-03 | 17-03 | Hover overlay clipped to shape, no square-corner bleed | SATISFIED | Glow-before-surface ordering; `AeroButtonSurfaceSourceTest` no-unclipped-overlay guard (proven fail-then-pass per 17-03-SUMMARY) |
| VBTN-04 | 17-01, 17-03 | Role.Button + Space/Enter keyboard | SATISFIED | `Modifier.clickable(role = Role.Button, ...)`; `AeroButtonSemanticsTest` (5/5 green) |
| VBTN-05 | 17-03, 17-04, 17-05 | Outlined variant equivalent treatment | SATISFIED | `outlinedStyle()` fixed delta; `AeroOutlinedButtonStylesTest`; human sign-off |
| VBTN-06 | 17-01, 17-03 | Shared internal surface, no visual drift | SATISFIED | Both wrappers delegate solely to `AeroButtonSurface`; `AeroButtonSurfaceSourceTest` shared-surface guard (proven fail-then-pass per 17-03-SUMMARY) |

No orphaned requirements — REQUIREMENTS.md maps only VBTN-01..06 to Phase 17, and all six appear in at least one plan's `requirements:` frontmatter.

### Anti-Patterns Found

None. Scanned `AeroButtonSurface.kt`, `AeroButton.kt`, `AeroOutlinedButton.kt`, `AeroSurfaceStyle.kt`, `ButtonsSection.kt` for `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER`/"not yet implemented"/"coming soon" — zero matches. No M3-container remnants, no unclipped `drawWithContent`, no `graphicsLayer` scale-shrink, no `Color.Transparent` fades in the new transforms.

### Human Verification Required

None outstanding. The phase's own mandatory human gate (17-05, three-theme × five-state sign-off for both `AeroButton` and `AeroOutlinedButton`) was already executed as part of phase execution and reached an APPROVED verdict after two calibration rounds. This verifier independently confirmed:
- The four calibration commits exist in `git log` with the exact hashes cited in `17-05-SUMMARY.md`.
- Their content (theme-aware rim-alpha cap, darkened filled fill, `base.surface`-targeted `flattenDisabled`) is live in the current source, not merely narrated.
- The full test suite (303 tests) and both structural source-scan guards (VBTN-03, VBTN-06) are green against the post-calibration code — the calibration did not regress the shared-surface architecture or the glow-before-surface modifier-chain ordering.

### Gaps Summary

None. All four ROADMAP Phase 17 Success Criteria and all six VBTN requirement IDs are satisfied by both automated evidence (303/303 tests green, source-scan guards proven fail-then-pass per 17-03-SUMMARY.md, no M3/anti-pattern remnants) and the already-completed mandatory human three-theme × five-state visual sign-off (17-05, APPROVED after two calibration rounds whose commits are verifiably present and live in the current source).

---

_Verified: 2026-07-23T15:32:01Z_
_Verifier: Claude (gsd-verifier)_
