package com.mordred.aero.components.datatable

import com.mordred.aero.components.datatable.internal.applySingleClick
import com.mordred.aero.components.datatable.internal.applyCtrlClick
import com.mordred.aero.components.datatable.internal.computeShiftRange
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectionLogicTest {

    @Test
    fun singleClickReplacesSelection() {
        val result = applySingleClick("c")
        assertEquals(setOf("c"), result)
    }

    @Test
    fun ctrlClickAddsThenRemoves() {
        val added = applyCtrlClick(setOf("a"), "b")
        assertEquals(setOf("a", "b"), added)

        val removed = applyCtrlClick(setOf("a", "b"), "b")
        assertEquals(setOf("a"), removed)
    }

    @Test
    fun shiftRangeForwardInclusive() {
        val displayed = listOf("a", "b", "c", "d")
        val result = computeShiftRange(displayed, anchorKey = "a", clickedKey = "c")
        assertEquals(setOf("a", "b", "c"), result)
    }

    @Test
    fun shiftRangeBackwardSameResult() {
        val displayed = listOf("a", "b", "c", "d")
        val result = computeShiftRange(displayed, anchorKey = "c", clickedKey = "a")
        assertEquals(setOf("a", "b", "c"), result)
    }

    @Test
    fun shiftRangeNullAnchorIsSingle() {
        val displayed = listOf("a", "b", "c", "d")
        val result = computeShiftRange(displayed, anchorKey = null, clickedKey = "b")
        assertEquals(setOf("b"), result)
    }

    @Test
    fun shiftRangeStableAcrossSort() {
        // After sort, displayed order is reversed — range follows displayed order, not insertion order
        val displayed = listOf("d", "c", "b", "a")
        val result = computeShiftRange(displayed, anchorKey = "d", clickedKey = "b")
        assertEquals(setOf("d", "c", "b"), result)
    }
}
