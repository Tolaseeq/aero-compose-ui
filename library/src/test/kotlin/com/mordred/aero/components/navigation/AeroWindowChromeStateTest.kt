package com.mordred.aero.components.navigation

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import com.mordred.aero.internal.windows.HTCAPTION
import com.mordred.aero.internal.windows.HTCLIENT
import com.mordred.aero.internal.windows.HTMAXBUTTON
import com.mordred.aero.internal.windows.HitTestRegionRegistry
import com.mordred.aero.internal.windows.HitTestSnapshot
import com.mordred.aero.internal.windows.PxRect
import com.mordred.aero.internal.windows.TitleBarRole
import com.mordred.aero.internal.windows.classifyHitTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * API-04 / D-05: headless locks for the custom-title-bar state. The impl class is composed
 * against a free-standing [HitTestRegionRegistry] (its constructor seam), proving the API is
 * general beyond AeroTitleBar's own layout: a custom 48 dp header — maximize box on the LEFT,
 * two caption areas split by a clickable search field — publishes exactly its element bounds
 * and classifies each area's own hit-test code; the opted-out (inert) state publishes nothing
 * while hover state still flows from the shared source; a removed element stops claiming
 * pixels. The never-invoked [apiShape] function is the API-04 compile gate for both
 * `rememberAeroWindowChrome` call shapes and the three modifiers.
 */
@OptIn(ExperimentalTestApi::class)
class AeroWindowChromeStateTest {

    @Test
    fun customHeaderPublishesAndClassifiesItsOwnRegions() = runComposeUiTest {
        val registry = HitTestRegionRegistry()
        val source = MutableInteractionSource()
        var state: AeroWindowChromeState? = null

        setContent {
            val hovered = source.collectIsHoveredAsState()
            val pressed = source.collectIsPressedAsState()
            val s = remember(registry, source) {
                AeroWindowChromeStateImpl(registry, null, isNative = true, source, hovered, pressed)
            }
            state = s
            Row(Modifier.testTag("header").height(48.dp)) {
                with(s) {
                    Box(Modifier.testTag("max").maximizeButtonArea().size(40.dp, 40.dp))
                    Box(Modifier.testTag("captionL").captionArea().weight(1f).fillMaxHeight())
                    Box(Modifier.testTag("search").captionExclude().width(160.dp).fillMaxHeight())
                    Box(Modifier.testTag("captionR").captionArea().weight(1f).fillMaxHeight())
                }
            }
        }
        waitForIdle()

        assertTrue(state?.isNative == true, "the free-standing state is native")

        val snap = registry.snapshot()
        assertEquals(2, snap.captions.size, "two caption areas published")
        assertEquals(1, snap.interactive.size, "one excluded (interactive) rect published")
        val maxRect = assertNotNull(snap.roles[TitleBarRole.Maximize], "the maximize rect published")

        fun bounds(tag: String): Rect = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        fun assertMatches(rect: PxRect, bounds: Rect, what: String) {
            assertEquals(bounds.left, rect.left.toFloat(), 1f, "$what left")
            assertEquals(bounds.top, rect.top.toFloat(), 1f, "$what top")
            assertEquals(bounds.right, rect.right.toFloat(), 1f, "$what right")
            assertEquals(bounds.bottom, rect.bottom.toFloat(), 1f, "$what bottom")
        }

        assertMatches(maxRect, bounds("max"), "the maximize box's published rect")
        assertMatches(snap.interactive.values.single(), bounds("search"), "the search field's published rect")
        val captionBoundses = listOf(bounds("captionL"), bounds("captionR"))
        snap.captions.values.forEach { rect ->
            assertTrue(
                captionBoundses.any { b -> matchesWithinOnePx(rect, b) },
                "published caption rect $rect matches one of the two caption boxes",
            )
        }
        captionBoundses.forEach { b ->
            assertTrue(
                snap.captions.values.any { matchesWithinOnePx(it, b) },
                "caption box $b has a published rect",
            )
        }

        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val clientW = root.width.toInt()
        val clientH = root.height.toInt()
        fun codeAt(b: Rect): Int = classifyHitTest(
            snap,
            ((b.left + b.right) / 2f).toInt(),
            ((b.top + b.bottom) / 2f).toInt(),
            clientW,
            clientH,
            maximized = false,
            resizeBandPx = 0,
        )
        assertEquals(HTMAXBUTTON, codeAt(bounds("max")), "the left-hand maximize box answers HTMAXBUTTON")
        assertEquals(HTCAPTION, codeAt(bounds("captionL")), "the left caption area answers HTCAPTION")
        assertEquals(HTCLIENT, codeAt(bounds("search")), "the clickable search field answers HTCLIENT")
        assertEquals(HTCAPTION, codeAt(bounds("captionR")), "the right caption area answers HTCAPTION")
    }

    @Test
    fun inertStatePublishesNothingAndStillReportsHoverFromTheSource() = runComposeUiTest {
        val registry = HitTestRegionRegistry()
        val source = MutableInteractionSource()
        var state: AeroWindowChromeState? = null

        setContent {
            val hovered = source.collectIsHoveredAsState()
            val pressed = source.collectIsPressedAsState()
            val s = remember(registry, source) {
                AeroWindowChromeStateImpl(registry, null, isNative = false, source, hovered, pressed)
            }
            state = s
            Row(Modifier.height(48.dp)) {
                with(s) {
                    Box(Modifier.testTag("max").maximizeButtonArea().size(40.dp))
                    Box(Modifier.testTag("captionL").captionArea().weight(1f).fillMaxHeight())
                    Box(Modifier.testTag("search").captionExclude().width(160.dp).fillMaxHeight())
                }
            }
        }
        waitForIdle()

        val s = state
        assertNotNull(s)
        assertFalse(s.isNative, "the opted-out state is not native")
        assertEquals(
            HitTestSnapshot.EMPTY,
            registry.snapshot(),
            "the opted-out state publishes nothing — the app keeps its own drag handling",
        )
        assertFalse(s.maximizeHovered.value)
        runOnIdle { source.tryEmit(HoverInteraction.Enter()) }
        waitForIdle()
        assertTrue(s.maximizeHovered.value, "hover state still flows from the source on the inert path")
    }

    @Test
    fun removingAnElementFromCompositionRemovesItsPublishedRect() = runComposeUiTest {
        val registry = HitTestRegionRegistry()
        val source = MutableInteractionSource()
        val showSecondCaption = mutableStateOf(true)

        setContent {
            val hovered = source.collectIsHoveredAsState()
            val pressed = source.collectIsPressedAsState()
            val s = remember(registry, source) {
                AeroWindowChromeStateImpl(registry, null, isNative = true, source, hovered, pressed)
            }
            Row(Modifier.height(48.dp)) {
                with(s) {
                    Box(Modifier.testTag("captionL").captionArea().weight(1f).fillMaxHeight())
                    if (showSecondCaption.value) {
                        Box(Modifier.testTag("captionR").captionArea().width(160.dp).fillMaxHeight())
                    }
                }
            }
        }
        waitForIdle()

        assertEquals(2, registry.snapshot().captions.size, "both caption areas published initially")
        val rightBounds = onNodeWithTag("captionR").fetchSemanticsNode().boundsInRoot
        assertTrue(
            registry.snapshot().captions.values.any { matchesWithinOnePx(it, rightBounds) },
            "the right caption box's rect is published",
        )

        runOnIdle { showSecondCaption.value = false }
        waitForIdle()

        assertEquals(1, registry.snapshot().captions.size, "the disposed caption area stopped claiming pixels")
        assertTrue(
            registry.snapshot().captions.values.none { matchesWithinOnePx(it, rightBounds) },
            "no published rect still matches the removed element",
        )
    }

    /**
     * API-01/API-04 compile gate: both `rememberAeroWindowChrome` call shapes and the three
     * marking modifiers keep compiling. Compilation of [apiShape] below is the gate — if any
     * shape regressed, this file would stop compiling and the test phase would fail.
     */
    @Test
    fun apiShapesCompile() {
        assertTrue(true)
    }

    @Composable
    @Suppress("unused", "UNUSED_VARIABLE", "UNUSED_EXPRESSION")
    private fun FrameWindowScope.apiShape(ws: WindowState) {
        val a = rememberAeroWindowChrome(ws)
        val b = rememberAeroWindowChrome(ws, nativeWindowManagement = false)
        with(a) {
            Modifier.captionArea().captionExclude().maximizeButtonArea()
        }
    }

    private fun matchesWithinOnePx(rect: PxRect, bounds: Rect): Boolean =
        abs(rect.left - bounds.left) <= 1 &&
            abs(rect.top - bounds.top) <= 1 &&
            abs(rect.right - bounds.right) <= 1 &&
            abs(rect.bottom - bounds.bottom) <= 1
}
