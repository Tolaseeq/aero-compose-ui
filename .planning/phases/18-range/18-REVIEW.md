---
phase: 18-range
reviewed: 2026-07-24T00:00:00Z
depth: standard
files_reviewed: 11
files_reviewed_list:
  - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
  - library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt
  - library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt
  - library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt
  - library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderDragLogicUntouchedTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt
findings:
  critical: 0
  warning: 2
  info: 4
  total: 6
status: issues_found
---

# Phase 18: Code Review Report

**Reviewed:** 2026-07-24
**Depth:** standard
**Files Reviewed:** 11
**Status:** issues_found

## Summary

Reviewed the Phase 18 "Aero glass" restyle of the range components (`AeroSlider`, `AeroRangeSlider`,
`AeroProgressBar`) plus the two new/extended theme primitives (`AeroSurfaceStyle.neutralRest`,
`drawAeroGlowRing` extraction) that back them.

Positive/verified findings (not defects, stated for completeness since they were explicit review
targets):
- The drag/keyboard/step-snap logic in `AeroRangeSlider.kt` (`snapToStep`, `applyThumbMove`,
  `xToValue`, `valueToX`, and the `awaitPointerEventScope` drag loop) is byte-for-byte unchanged
  from the pre-phase baseline (`git diff` confirms zero touch to that region), matching the
  render-only contract and the `AeroRangeSliderDragLogicUntouchedTest` guard.
- The one place this phase adds a manual `awaitPointerEventScope` pointer loop that emits into a
  `MutableInteractionSource` (the new per-thumb hover-tracking block in `AeroRangeSlider.kt`)
  correctly uses `tryEmit`, not the suspending `emit`, respecting `@RestrictsSuspension`.
- `drawAeroSurfaceCore`'s `inset { }` sub-region math in `AeroRangeSlider`'s thumb draw always
  degenerates to an exact `2 * liftedRadius` square regardless of the thumb's x position (verified
  algebraically), so `drawAeroThumb`'s circle-collapse precondition (`cornerPx == half side length`)
  holds even for thumbs near the track edges.

Two real (if scoped/subtle) defects were found in the new code, plus four lower-severity quality
items. None are crashes/security issues; the two Warnings are both legitimate rendering-correctness
bugs a maintainer should fix before/soon after shipping.

## Warnings

### WR-01: New per-thumb hover pointerInput block leaks `HoverInteraction.Enter` on cancellation, causing a stuck hover glow

**File:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt:295-344`

**Issue:** The new hover-tracking `pointerInput(enabled, valueRange) { ... awaitPointerEventScope { while (true) { ... } } }` block tracks `startEnter`/`endEnter` as **local** `var`s inside the coroutine and calls `tryEmit(HoverInteraction.Exit(...))` only from within the `while (true)` loop body. `pointerInput`'s keys (`enabled`, `valueRange`) restart this block by cancelling the running coroutine and relaunching a fresh one whenever either key's value changes. Cancellation happens at the next suspension point (`awaitPointerEvent()`), which throws — the loop body's own `Exit` bookkeeping never runs, and the fresh coroutine starts with brand-new (empty) local `startEnter`/`endEnter` state.

Meanwhile `startInteractionSource`/`endInteractionSource` are `remember`ed at the composable level and survive the restart untouched. `collectIsHoveredAsState()` (used inside `rememberAeroInteractionState`) tracks hover by accumulating `HoverInteraction.Enter` instances and only clears one when a matching `Exit` for that exact `Enter` arrives. If the pointer is hovering a thumb (an `Enter` was `tryEmit`'d) at the moment the block restarts — e.g. the caller changes `valueRange` while the user is hovering, or toggles `enabled` off then back on — the `Enter` is never paired with an `Exit`. That thumb's `hovered` boolean is now stuck `true` forever, e.g. after an `enabled=false → true` cycle the thumb will render its hover glow ring immediately on re-enable even though the pointer is nowhere near it, with no further pointer movement able to clear it (the block only clears hover by observing state transitions, and the interaction source already believes it's hovered).

**Fix:** Wrap the loop in `try/finally` and emit outstanding `Exit`s for any interaction that was still open when the block is cancelled:
```kotlin
awaitPointerEventScope {
    try {
        while (true) {
            // ... existing body ...
        }
    } finally {
        startEnter?.let { startInteractionSource.tryEmit(HoverInteraction.Exit(it)) }
        endEnter?.let { endInteractionSource.tryEmit(HoverInteraction.Exit(it)) }
    }
}
```
(`tryEmit` is a plain non-suspending call, so it's safe to invoke from a `finally` block during cancellation.)

### WR-02: Indeterminate sweep's `BlendMode.DstIn` edge mask has no isolated compositing layer to blend against

**File:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt:205-222`

**Issue:** The indeterminate sweep segment chains `.aeroSurface(sweepStyle, RoundedCornerShape(cornerPx))` (which itself ends in `.clip(shape).drawWithCache { onDrawBehind { drawAeroSurfaceCore(...) } }`) with a second `.drawWithCache { onDrawBehind { drawRect(brush = edgeMask, blendMode = BlendMode.DstIn) } }`. The comment states the intent is for the `DstIn` mask to affect only "the already-painted fill's alpha," not the groove/track drawn by the parent underneath.

`Modifier.clip(shape)` alone does not guarantee an isolated (offscreen) compositing layer — Compose's default `CompositingStrategy.Auto` typically achieves a rounded-rect clip via a canvas clip-path on the shared drawing surface rather than allocating a separate layer, since a plain `clip=true` graphics layer doesn't by itself require offscreen isolation for the `Auto` heuristic. Without an isolated layer, `BlendMode.DstIn`'s "Dst" read is whatever is already present on the shared canvas at those pixels — which, by the time this segment draws, already includes the parent `aeroGroove` track bed painted immediately before it in z-order. On the two "glass" themes (`AeroBlue`/`AeroDark`), `AeroSurfaceStyle.rest(...)`'s fill tones are translucent by design (that's the whole point of the "glass" identity), so the groove color is already blended into "Dst" underneath the segment's own semi-transparent fill before the mask runs. Multiplying that combined Dst by the mask's alpha at the fading edges will darken/fade the groove's contribution too, rather than cleanly revealing "groove, unmasked" the way the KDoc intends. (On `AeroColorScheme.Classic`, whose tokens are fully opaque, this is not visible since the segment's own fill fully occludes the groove regardless.)

**Fix:** Force an isolated offscreen layer before applying the `DstIn` mask, e.g.:
```kotlin
.aeroSurface(sweepStyle, RoundedCornerShape(cornerPx))
.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
.drawWithCache {
    val edgeMask = Brush.horizontalGradient(/* unchanged */)
    onDrawBehind { drawRect(brush = edgeMask, blendMode = BlendMode.DstIn) }
}
```
or draw the fill + mask together inside a single `Canvas`/`drawWithContent` block using `graphicsLayer`/`saveLayer` so the mask only ever reads back this segment's own just-painted pixels. Worth a visual spot-check on `AeroBlue`/`AeroDark` specifically (the Classic-only sign-off pass would not surface this).

## Info

### IN-01: Unused import left behind after replacing `drawLine`/`drawCircle` with `inset { }` blocks

**File:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt:21`

**Issue:** `import androidx.compose.ui.geometry.Offset` is no longer referenced anywhere in the file — the old `Offset(...)` constructor calls (`drawLine(start = Offset(...), ...)`, `drawCircle(center = Offset(...), ...)`) were removed when the draw block was rewritten to use `drawAeroGroove`/`drawAeroSurfaceCore`/`drawAeroThumb` inside `inset { }` regions.

**Fix:** Remove the unused import.

### IN-02: `cornerPx` variable name is misleading — it holds a `Dp`, not a pixel value

**File:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt:85, 177`

**Issue:** `val cornerPx = height / 2` computes a `Dp` (dividing a `Dp` by an `Int` yields a `Dp`), then is passed directly to `AeroSurfaceStyle.rest/neutralRest(colors, cornerPx)` and `RoundedCornerShape(cornerPx)` — both of which correctly expect a `Dp`, so the current usage is functionally correct. But naming it `cornerPx` (a name used elsewhere in this same file family for actual post-`.toPx()` `Float` pixel values, e.g. `drawAeroSurfaceCore(style, cornerPx: Float)`) invites a future maintainer to call `.toPx()` on it "for consistency," silently double-converting.

**Fix:** Rename to `cornerRadius` (matching the `AeroSurfaceStyle.rest(base, cornerRadius: Dp)` parameter it's actually feeding).

### IN-03: Track thickness/corner-radius are re-hardcoded as bare `Dp` literals instead of named constants

**File:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt:211, 350, 366, 381`

**Issue:** `AeroSlider.kt` defines `TRACK_HEIGHT = 4.dp` / `TRACK_CORNER_RADIUS = 2.dp` as named constants and uses them consistently. `AeroRangeSlider.kt` instead re-derives the same values as independent literals: `AeroSurfaceStyle.neutralRest(colors, cornerRadius = 2.dp)` (line 211) and, inside the `Canvas` draw block, `val trackThickness = 4.dp.toPx()` (line 350) with `cornerPx = trackThickness / 2f` computed separately at each of the two `drawAeroGroove`/`drawAeroSurfaceCore` call sites (lines 366, 381). The values are numerically consistent today, but nothing enforces that — if either sibling slider's track dimensions are tuned later, these two files can silently drift out of parity even though the KDoc explicitly claims they're meant to read as "one visual family."

**Fix:** Hoist `TRACK_HEIGHT`/`TRACK_CORNER_RADIUS` (or equivalent) to a shared location both `AeroSlider.kt` and `AeroRangeSlider.kt` reference, mirroring how `resolveSliderThumbStyle`/`resolveSliderTrackStyle` are already reused verbatim across the two files.

### IN-04: Redundant explicit `onValueChangeFinished = null`

**File:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt:155`

**Issue:** `Slider(..., onValueChangeFinished = null, ...)` explicitly passes the parameter's own default value. It's a no-op relative to omitting it and adds a line without changing behavior.

**Fix:** Remove the explicit `onValueChangeFinished = null` argument (or, if intended as documentation that the callback is deliberately unused, say so in a comment instead of a no-op argument).

---

_Reviewed: 2026-07-24_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
