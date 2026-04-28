package com.mordred.aero.components.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * OVL-04 sealed-class hierarchy contract: structural verification of the three
 * variants (Action / Divider / Submenu). Promoted from the Wave-0 stub by Plan 03-05.
 */
class AeroContextMenuItemTest {

    @Test
    fun actionDataClassEquality() {
        val cb: () -> Unit = {}
        val a1 = AeroContextMenuItem.Action(label = "Copy", onClick = cb)
        val a2 = AeroContextMenuItem.Action(label = "Copy", onClick = cb)
        assertEquals(a1, a2)
    }

    @Test
    fun actionWithDifferentLabelsAreUnequal() {
        val a1 = AeroContextMenuItem.Action(label = "Copy", onClick = {})
        val a2 = AeroContextMenuItem.Action(label = "Paste", onClick = {})
        assertNotEquals(a1, a2)
    }

    @Test
    fun dividerIsSingleton() {
        // data object → all references are the same instance
        assertTrue(AeroContextMenuItem.Divider === AeroContextMenuItem.Divider)
    }

    @Test
    fun submenuAcceptsEmptyAndNonEmptyChildren() {
        val empty = AeroContextMenuItem.Submenu(label = "Empty", items = emptyList())
        val nested = AeroContextMenuItem.Submenu(
            label = "Edit",
            items = listOf(AeroContextMenuItem.Action("Cut", {}), AeroContextMenuItem.Divider)
        )
        assertEquals(0, empty.items.size)
        assertEquals(2, nested.items.size)
    }

    @Test
    fun whenExpressionExhaustsAllSealedCases() {
        // Compile-time check: if a new subtype is added without a branch, this won't compile.
        val item: AeroContextMenuItem = AeroContextMenuItem.Action("Copy", {})
        val name: String = when (item) {
            is AeroContextMenuItem.Action  -> "Action"
            AeroContextMenuItem.Divider    -> "Divider"
            is AeroContextMenuItem.Submenu -> "Submenu"
        }
        assertEquals("Action", name)
    }
}
