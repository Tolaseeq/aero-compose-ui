package com.mordred.aero.components.layout

import com.mordred.aero.components.layout.internal.accordion.accordionToggleMulti
import com.mordred.aero.components.layout.internal.accordion.accordionToggleSingle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccordionToggleTest {

    // -- accordionToggleSingle --

    @Test
    fun singleOpenNewSectionClosesPrevious() {
        assertEquals(1, accordionToggleSingle(expandedIndex = 0, clickedIndex = 1))
    }

    @Test
    fun singleClickOpenSectionClosesIt() {
        assertNull(accordionToggleSingle(expandedIndex = 0, clickedIndex = 0))
    }

    @Test
    fun singleOpenFromNone() {
        assertEquals(2, accordionToggleSingle(expandedIndex = null, clickedIndex = 2))
    }

    // -- accordionToggleMulti --

    @Test
    fun multiKeepsBothWhenOpeningNew() {
        assertEquals(setOf(0, 1), accordionToggleMulti(expandedIndices = setOf(0), clickedIndex = 1))
    }

    @Test
    fun multiTogglesOffOneWhileOtherStays() {
        assertEquals(setOf(1), accordionToggleMulti(expandedIndices = setOf(0, 1), clickedIndex = 0))
    }

    @Test
    fun multiOpenFromEmpty() {
        assertEquals(setOf(3), accordionToggleMulti(expandedIndices = emptySet(), clickedIndex = 3))
    }
}
