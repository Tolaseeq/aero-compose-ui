package com.mordred.aero.components.datatable

import com.mordred.aero.components.datatable.internal.NodeState
import com.mordred.aero.components.datatable.internal.toggleNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeStateTest {

    @Test
    fun firstExpandFires() {
        // null current = never seen; first expand fires onExpand
        val result = toggleNode(null)
        assertTrue(result.state.isExpanded)
        assertTrue(result.state.childrenLoaded)
        assertTrue(result.shouldFireExpand)
    }

    @Test
    fun collapseDoesNotFire() {
        // Node was expanded+loaded; collapse should not fire
        val result = toggleNode(NodeState(isExpanded = true, childrenLoaded = true))
        assertFalse(result.state.isExpanded)
        assertFalse(result.shouldFireExpand)
        // childrenLoaded must remain true even after collapse
        assertTrue(result.state.childrenLoaded)
    }

    @Test
    fun reExpandAfterLoadDoesNotFire() {
        // Node was previously expanded (childrenLoaded=true) then collapsed (isExpanded=false)
        // Re-expanding must NOT fire onExpand — PITFALL-05
        val result = toggleNode(NodeState(isExpanded = false, childrenLoaded = true))
        assertTrue(result.state.isExpanded)
        assertFalse(result.shouldFireExpand, "onExpand must not re-fire on re-expand after load")
    }

    @Test
    fun expandCollapseExpandFiresOnce() {
        // Simulate full sequence: expand (fires) -> collapse (no fire) -> re-expand (no fire)
        var fireCount = 0

        // Step 1: first expand from null state
        val r1 = toggleNode(null)
        if (r1.shouldFireExpand) fireCount++
        assertEquals(1, fireCount)
        assertTrue(r1.state.isExpanded)
        assertTrue(r1.state.childrenLoaded)

        // Step 2: collapse
        val r2 = toggleNode(r1.state)
        if (r2.shouldFireExpand) fireCount++
        assertEquals(1, fireCount, "collapse must not increment fire count")
        assertFalse(r2.state.isExpanded)
        assertTrue(r2.state.childrenLoaded)

        // Step 3: re-expand
        val r3 = toggleNode(r2.state)
        if (r3.shouldFireExpand) fireCount++
        assertEquals(1, fireCount, "re-expand must not fire onExpand again")
        assertTrue(r3.state.isExpanded)
    }

    @Test
    fun scrollBackScenario() {
        // Documents the scroll-out/scroll-back case (PITFALL-05):
        // A node that was expanded, had its composable disposed (scroll-out),
        // and then scrolled back is represented as NodeState(isExpanded=false, childrenLoaded=true)
        // when the user collapses and then re-expands, or as restored isExpanded=true state.
        // Either way, re-expanding a childrenLoaded=true node must not re-fire.
        val restoredAfterScrollBack = NodeState(isExpanded = false, childrenLoaded = true)
        val result = toggleNode(restoredAfterScrollBack)
        assertTrue(result.state.isExpanded)
        assertFalse(result.shouldFireExpand, "scroll-back re-expand must not fire onExpand")
    }
}
