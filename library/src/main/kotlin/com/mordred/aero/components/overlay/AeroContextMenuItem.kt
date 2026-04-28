package com.mordred.aero.components.overlay

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * OVL-04: Sealed hierarchy of items that can appear in an [com.mordred.aero.components.overlay.AeroContextMenu].
 *
 * Three variants:
 *  - [Action]: clickable item with optional icon + shortcut.
 *  - [Divider]: visual separator (no interaction).
 *  - [Submenu]: nested menu (recursive — same item type for children).
 *
 * Submenus are supported recursively (no hard depth cap; expected usage 1-2 levels).
 * The v1 cascade is rendered as a flat row with a `▶` glyph; full hover-cascade
 * expansion is deferred (CONTEXT.md "Claude's discretion").
 */
public sealed class AeroContextMenuItem {

    /** Clickable command. Optional [icon] / [shortcut] are reserved for visual presentation. */
    public data class Action(
        val label: String,
        val onClick: () -> Unit,
        val icon: ImageVector? = null,
        val shortcut: String? = null,
        val enabled: Boolean = true
    ) : AeroContextMenuItem()

    /** Horizontal separator between groups of items. */
    public data object Divider : AeroContextMenuItem()

    /**
     * Nested submenu. v1 renders flat with a `▶` indicator; cascade expansion
     * is deferred to v2.
     */
    public data class Submenu(
        val label: String,
        val items: List<AeroContextMenuItem>,
        val icon: ImageVector? = null
    ) : AeroContextMenuItem()
}
