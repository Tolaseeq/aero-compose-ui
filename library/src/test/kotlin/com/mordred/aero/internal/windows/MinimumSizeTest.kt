package com.mordred.aero.internal.windows

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * D-01: the pure minimum-size resolution both resize paths share — the app's own AWT minimum
 * when the app set one, else the 320 x 240 dp default floor in physical px at the window's
 * current scale for the WM_GETMINMAXINFO path and in dp for the Compose drag clamps. The
 * library never overwrites the app's `window.minimumSize`; an app-set value passes through
 * untouched, smaller or larger than the default.
 */
class MinimumSizeTest {

    /** D-01: nothing set — the default floor, converted at the window's scale. */
    @Test
    fun unsetAppMinimumGetsTheDefaultFloorInPhysicalPxAtTheWindowScale() {
        assertEquals(320 to 240, resolveMinimumTrackSizePx(appMinimumSet = false, awtFilledWidthPx = 1, awtFilledHeightPx = 1, scale = 1.0f))
        assertEquals(480 to 360, resolveMinimumTrackSizePx(appMinimumSet = false, awtFilledWidthPx = 1, awtFilledHeightPx = 1, scale = 1.5f))
    }

    /** D-01: values AWT already filled above the floor are kept, never lowered. */
    @Test
    fun awtFilledValuesAboveTheFloorPassThrough() {
        assertEquals(500 to 400, resolveMinimumTrackSizePx(appMinimumSet = false, awtFilledWidthPx = 500, awtFilledHeightPx = 400, scale = 1.0f))
    }

    /**
     * D-01 / T-22-24: an app-set minimum (the SHW-17 narrow window's 260 x 200 dp, which AWT
     * fills as 390 x 300 px at 150%) is returned unchanged — the floor never raises it to the
     * 480 x 360 default.
     */
    @Test
    fun appSetMinimumIsReturnedUnchangedNeverRaisedToTheDefault() {
        assertEquals(390 to 300, resolveMinimumTrackSizePx(appMinimumSet = true, awtFilledWidthPx = 390, awtFilledHeightPx = 300, scale = 1.5f))
    }

    /** D-01 Compose path: the app's own dp minimum when set. */
    @Test
    fun composePathUsesTheAppMinimumWhenSet() {
        assertEquals(260f to 200f, resolveComposeMinimumDp(appMinimumSet = true, appMinWidth = 260, appMinHeight = 200))
    }

    /** D-01 Compose path: the 320 x 240 dp floor when the app set nothing. */
    @Test
    fun composePathDefaultsToThe320x240FloorWhenUnset() {
        assertEquals(320f to 240f, resolveComposeMinimumDp(appMinimumSet = false, appMinWidth = 1, appMinHeight = 1))
    }
}
