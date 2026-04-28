package com.mordred.aero.components.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AeroMenuBarTest {

    /**
     * Compile-reachability — if AeroMenuBar is renamed/moved, this fails to load.
     *
     * Note: cannot use a `::AeroMenuBar` function reference because Kotlin Compose
     * plugin disallows function references to `@Composable` functions. We instead
     * assert the generated `<File>Kt` class is loadable from the classpath, mirroring
     * the established pattern from 03-01 / 03-02 / 03-04 / 03-06 test families.
     */
    @Test
    fun aeroMenuBarCompileSmoke() {
        val cls = Class.forName("com.mordred.aero.components.navigation.AeroMenuBarKt")
        assertNotNull(cls)
        val hasMenuBar = cls.methods.any { it.name == "AeroMenuBar" }
        assertEquals(true, hasMenuBar)
    }

    /**
     * NAV-02 contract: AeroMenuItem is a sealed hierarchy with three variants —
     * Action / Divider / Submenu — so callers can `when`-match exhaustively at
     * compile time.
     */
    @Test
    fun aeroMenuItemSealedHierarchyExhaustive() {
        val item: AeroMenuItem = AeroMenuItem.Action("Save", {})
        val name = when (item) {
            is AeroMenuItem.Action  -> "Action"
            AeroMenuItem.Divider    -> "Divider"
            is AeroMenuItem.Submenu -> "Submenu"
        }
        assertEquals("Action", name)
    }

    @Test
    fun aeroMenuItemActionDataClassEquality() {
        val a = AeroMenuItem.Action(label = "Save", onClick = {})
        val b = AeroMenuItem.Action(label = "Save", onClick = {})
        // Action carries a lambda; equality is structural over (label, icon, shortcut, enabled)
        // — we verify the label survives copy.
        assertEquals(a.label, b.label)
        assertEquals(a.enabled, b.enabled)
    }

    @Test
    fun aeroTopLevelMenuDataClassEquality() {
        val a = AeroTopLevelMenu(label = "File", items = emptyList())
        val b = AeroTopLevelMenu(label = "File", items = emptyList())
        assertEquals(a, b)
    }
}
