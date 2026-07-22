package com.mordred.showcase.scratch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroGroove
import com.mordred.aero.theme.aeroThumbSurface

/**
 * PRIM-18 SPIKE (16-05-PLAN.md Task 1) — M3 `Slider` custom-sized `thumb=`/`track=` slot proof.
 *
 * Answers 16-RESEARCH.md's Open Question 1 / Assumption A7: does M3 `Slider`'s internal layout
 * math accept a custom-sized thumb/track slot without clipping or misaligning it? This spike
 * feeds a raised, circular ~18.dp [aeroThumbSurface] thumb and a recessed 10.dp-tall
 * [aeroGroove] track into a real `androidx.compose.material3.Slider` (Material3 1.9.0, pinned
 * `library/build.gradle.kts:22`) via its `thumb =`/`track =` slot parameters — sized to differ
 * substantially, in both directions, from M3's own defaults.
 *
 * VERDICT: **PASS** — keep M3 `Slider` + custom-sized `thumb=`/`track=` slots at MEDIUM
 * complexity for Phase 18's `AeroSlider` restyle (per STATE.md's locked architecture position
 * and 16-RESEARCH.md's Don't-Hand-Roll table). Do NOT fall back to full M3 removal /
 * `AeroRangeSlider`-style Canvas+`awaitPointerEventScope` reimplementation (which would
 * re-promote Phase 18 to HIGH complexity and require re-deriving keyboard nudge, `steps` snap,
 * `onValueChangeFinished`, and semantics from scratch per 16-RESEARCH.md Pitfall 6/8).
 *
 * EVIDENCE (bytecode inspection of the real `material3-desktop-1.9.0.jar` compiled classes via
 * `javap -c`, mirroring `ScratchAeroShadowProof.kt`'s TOOL-07 verification methodology — a
 * direct read of the shipped algorithm, not an assumption from source-level docs):
 * - `SliderKt$SliderImpl$2$1.measure-3p2s80s(...)` (the `MeasurePolicy` backing the internal
 *   `Layout(...)` call in `SliderImpl`) measures the `THUMB`-tagged `Measurable` FIRST via
 *   `Measurable.measure-BRTryo0(constraints)`, obtaining its real `Placeable`, then derives the
 *   `TRACK` measurable's own measurement constraints from that placeable's actual
 *   `getWidth()`/`getHeight()` via `Constraints.offset(-thumbPlaceable.width, ...)` — never from
 *   a hardcoded `SliderTokens` literal. The overall layout width/height, the track's vertical
 *   centering offset, and the thumb's drag-fraction placement offset are ALL computed from the
 *   two placeables' real measured `getWidth()`/`getHeight()`, confirmed at every arithmetic step
 *   in the decompiled bytecode (`Math.max(thumbW, trackW)`, `(thumbH - trackH) / 2`, etc.) — the
 *   algorithm is written to be size-agnostic by construction, not tuned to
 *   `SliderDefaults.Thumb`/`Track`'s own dimensions.
 * - `SliderImpl`'s own modifier chain applies `Modifier.requiredSizeIn(minWidth = ..., minHeight
 *   = ...)` using `SliderTokens` values (`TrackHeight`/`ThumbWidth`) as a MINIMUM floor only
 *   (maxWidth/maxHeight both left at their `Dp.Unspecified` default — confirmed by the
 *   `$default` bridge's `12` parameter-defaulting bitmask) plus
 *   `InteractiveComponentSizeKt.minimumInteractiveComponentSize()` (the standard ≥48.dp
 *   accessibility touch-target floor). Neither imposes a maximum bound that could clip an
 *   oversized custom thumb/track; an undersized custom slot would only raise the Slider's own
 *   bounding box to the floor, never truncate a larger one.
 * - `SliderTokens` (`javap`-read `<clinit>`) confirms M3's OWN default thumb is a 4.dp-wide ×
 *   44.dp-tall vertical pill (`HandleWidth = 4.dp`, `HandleHeight = 44.dp`, the current M3
 *   "expressive" thumb shape) against a 16.dp `InactiveTrackHeight` — i.e. this spike's ~18.dp
 *   circular thumb and 10.dp track are a substantial two-axis deviation from the default in both
 *   directions (much wider, much shorter thumb; shallower track), a meaningfully different case,
 *   not a near-identical one.
 * - `./gradlew :showcase:compileKotlin` succeeds against the real pinned Material3 1.9.0
 *   artifact using this exact `thumb =`/`track =` call shape.
 *
 * No live/rendered screenshot capture was performed for this verdict (no interactive display
 * automation was available in the executing environment) — the verdict rests on direct
 * inspection of the compiled Slider measure/placement algorithm shipped in the pinned artifact,
 * which is the authoritative source for whether clipping/misalignment occurs (the algorithm IS
 * the layout math the Open Question asks about). If a human visual pass at the Task 2 checkpoint
 * observes any clipping/misalignment that contradicts this bytecode-derived verdict, treat the
 * live observation as authoritative and flip this verdict to FAIL before Phase 18 planning.
 *
 * DISCREPANCY NOTE (correcting 16-RESEARCH.md's Architecture Q5 confidence framing): the
 * `thumb =`/`track =` `Slider` overload compiled here is annotated `@ExperimentalMaterial3Api`
 * at the pinned 1.9.0 coordinate (`e: This material API is experimental...` without an opt-in) —
 * confirmed by this spike's own compile error before `@OptIn(ExperimentalMaterial3Api::class)`
 * was added below. The slot API *shape* is still long-stable per the research (unchanged since
 * pre-1.4), but Phase 18's real `AeroSlider` restyle will need the same `@OptIn` annotation this
 * file uses — a small compile-time detail, not a change to the PASS verdict above.
 *
 * SCOPE GUARD: this is a throwaway-but-evidenced compile+static-analysis proof, not a production
 * primitive. Do NOT wire this into any real showcase screen or component (see 16-05-PLAN.md
 * prohibitions) — `AeroSlider`'s actual restyle happens in Phase 18, consuming this verdict, not
 * this file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScratchSliderSlotSpike() {
    var value by remember { mutableStateOf(0.5f) }

    val thumbStyle = AeroSurfaceStyle.rest(AeroTheme.colors, cornerRadius = 9.dp)
    val trackStyle = AeroSurfaceStyle.rest(AeroTheme.colors, cornerRadius = 5.dp)

    Slider(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        thumb = {
            // ~18.dp raised circular thumb (target FEATURES A12) — vs M3's own 4.dp x 44.dp
            // default vertical-pill handle (SliderTokens.HandleWidth/HandleHeight).
            Box(Modifier.size(18.dp).aeroThumbSurface(thumbStyle))
        },
        track = {
            // 10.dp recessed groove track — vs M3's own 16.dp InactiveTrackHeight default.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .aeroGroove(trackStyle),
            )
        },
    )
}
