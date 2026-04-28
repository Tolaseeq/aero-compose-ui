package com.mordred.aero.components.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * NAV-02: Sealed hierarchy of items shown inside an [AeroMenuBar] dropdown.
 *
 * Mirrors the shape of `AeroContextMenuItem` (Plan 03-05) but is kept deliberately
 * separate so the menu-bar shortcut/icon presentation can evolve independently from
 * context-menu items without breaking either API.
 */
public sealed class AeroMenuItem {

    /** A clickable command. Optional [icon] / [shortcut] are reserved for future visual work. */
    public data class Action(
        val label: String,
        val onClick: () -> Unit,
        val icon: ImageVector? = null,
        val shortcut: String? = null,
        val enabled: Boolean = true
    ) : AeroMenuItem()

    /** Horizontal divider between groups of items. */
    public data object Divider : AeroMenuItem()

    /**
     * A nested submenu. v1 renders the row with a flat "▶" marker; cascading expansion
     * is deferred to v2 (CONTEXT.md "Claude's discretion").
     */
    public data class Submenu(
        val label: String,
        val items: List<AeroMenuItem>,
        val icon: ImageVector? = null
    ) : AeroMenuItem()
}

/**
 * NAV-02: A top-level menu in the bar — a label plus its dropdown items.
 */
public data class AeroTopLevelMenu(
    val label: String,
    val items: List<AeroMenuItem>
)
