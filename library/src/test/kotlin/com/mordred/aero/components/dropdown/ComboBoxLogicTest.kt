package com.mordred.aero.components.dropdown

import com.mordred.aero.components.dropdown.internal.shouldAutoOpen
import com.mordred.aero.components.dropdown.internal.textAfterSelect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComboBoxLogicTest {

    // -- textAfterSelect --

    @Test
    fun textAfterSelectKeepsLabelByDefault() {
        assertEquals("Russia", textAfterSelect("Russia", clearOnSelect = false))
    }

    @Test
    fun textAfterSelectClearsWhenClearOnSelect() {
        assertEquals("", textAfterSelect("Russia", clearOnSelect = true))
    }

    // -- shouldAutoOpen --

    @Test
    fun typeToFilterOpens() {
        assertTrue(
            shouldAutoOpen(
                focused = true,
                text = "ru",
                options = listOf("Russia", "Romania"),
                justSelected = false,
            )
        )
    }

    @Test
    fun focusedEmptyOffersAll() {
        assertTrue(
            shouldAutoOpen(
                focused = true,
                text = "",
                options = listOf("Russia"),
                justSelected = false,
            )
        )
    }

    @Test
    fun unfocusedNeverOpens() {
        assertFalse(
            shouldAutoOpen(
                focused = false,
                text = "ru",
                options = listOf("Russia"),
                justSelected = false,
            )
        )
    }

    @Test
    fun noMatchNeverOpens() {
        assertFalse(
            shouldAutoOpen(
                focused = true,
                text = "zzz",
                options = listOf("Russia"),
                justSelected = false,
            )
        )
    }

    @Test
    fun justSelectedSuppressesReopenOnExactMatch() {
        assertFalse(
            shouldAutoOpen(
                focused = true,
                text = "Russia",
                options = listOf("Russia"),
                justSelected = true,
            )
        )
    }

    @Test
    fun justSelectedStaysClosedWhenCleared() {
        assertFalse(
            shouldAutoOpen(
                focused = true,
                text = "",
                options = listOf("Russia"),
                justSelected = true,
            )
        )
    }
}
