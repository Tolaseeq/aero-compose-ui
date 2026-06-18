package com.mordred.aero.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.overlay.AeroTooltip
import com.mordred.aero.theme.AeroTheme

/** LAYO-05/06/07: Three display modes for [AeroSidebar]. */
public enum class AeroSidebarMode {
    /** Full icon + label display (~240dp wide). */
    Expanded,
    /** Icon-only display with tooltip labels (~48dp wide). */
    Collapsed,
    /** Hidden — takes no space in layout (0dp wide). */
    Hidden,
}

/**
 * Returns the target width in [Dp] for a given [AeroSidebarMode].
 *
 * This is an internal pure function — intentionally testable without a Compose runtime.
 * The three target widths (240dp / 48dp / 0dp) are the v2.0 locked values from CONTEXT.md.
 */
internal fun targetWidthForMode(mode: AeroSidebarMode): Dp = when (mode) {
    AeroSidebarMode.Expanded  -> 240.dp
    AeroSidebarMode.Collapsed -> 48.dp
    AeroSidebarMode.Hidden    -> 0.dp
}

/**
 * LAYO-05/06/07: State holder for [AeroSidebar] following the [DrawerState] / [ScaffoldState]
 * idiom. Callers create this via [rememberAeroSidebarState].
 *
 * **PITFALL-11 contract:** Adjacent layout should read [currentWidthDp] (not hard-code the
 * sidebar's width) so it reflows correctly during the collapse animation. Do NOT place
 * [AeroSidebar] inside an [AeroSplitPane] pane — use a top-level [Row] sibling pattern instead.
 *
 * @param initialMode the mode the sidebar starts in.
 */
@Stable
public class AeroSidebarState(initialMode: AeroSidebarMode) {
    /** Current display mode. Change this to trigger an animated width transition. */
    public var mode: AeroSidebarMode by mutableStateOf(initialMode)

    /**
     * Backing mutable state — written by [AeroSidebar] each frame during animation so that
     * adjacent layout can read the live animated value via [currentWidthDp].
     */
    internal val widthState: MutableState<Dp> = mutableStateOf(targetWidthForMode(initialMode))

    /**
     * Live animated width of the sidebar. Adjacent layout reads this to reflow without
     * needing to know the current mode directly. Updated every animation frame by
     * [AeroSidebar] via a [SideEffect].
     *
     * PITFALL-11: reading this value (instead of a static sidebar width) is what prevents
     * layout thrash when the sidebar collapses.
     */
    public val currentWidthDp: State<Dp> get() = widthState
}

/**
 * Creates and remembers an [AeroSidebarState].
 *
 * @param initialMode initial display mode (default: [AeroSidebarMode.Expanded]).
 */
@Composable
public fun rememberAeroSidebarState(
    initialMode: AeroSidebarMode = AeroSidebarMode.Expanded,
): AeroSidebarState = remember { AeroSidebarState(initialMode) }

/**
 * LAYO-07: Composable slot DSL for [AeroSidebar] items.
 *
 * Obtain an instance via the [AeroSidebar] content lambda:
 * ```kotlin
 * AeroSidebar(state = sidebarState) {
 *     item(AeroIcons.House, "Home", selected = true) { ... }
 *     section("Library")
 *     item(AeroIcons.Gear, "Settings", selected = false) { ... }
 *     divider()
 * }
 * ```
 *
 * @param mode the current [AeroSidebarMode] — controls whether labels and section headers
 *   are visible, and whether collapsed icons show [AeroTooltip] labels.
 */
public class AeroSidebarScope internal constructor(internal val mode: AeroSidebarMode) {

    /**
     * A navigation item with an icon, label, selection state, and click handler.
     *
     * Expanded: icon + label row, left 3dp primary accent-bar when selected.
     * Collapsed: icon only (in [AeroTooltip] wrapping so the label appears on hover).
     *           Left accent-bar remains visible in collapsed mode (LAYO-06).
     * Hidden: not shown (sidebar has zero width, so no content renders).
     *
     * @param icon icon vector displayed in both expanded and collapsed modes.
     * @param label text label shown in expanded mode; used as tooltip text in collapsed mode.
     * @param selected when true, renders the primary accent-bar and glass-gradient fill.
     * @param onClick invoked on row click.
     */
    @Composable
    public fun item(
        icon: ImageVector,
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        val colors = AeroTheme.colors
        val interactionSource = remember { MutableInteractionSource() }
        val hovered by interactionSource.collectIsHoveredAsState()

        val rowBackground = when {
            selected -> colors.primary.copy(alpha = 0.15f)
            hovered  -> colors.buttonHover
            else     -> Color.Transparent
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(rowBackground)
                .hoverable(interactionSource)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left primary accent-bar — 3dp, always visible when selected
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(if (selected) colors.primary else Color.Transparent)
            )

            Spacer(modifier = Modifier.width(if (mode == AeroSidebarMode.Collapsed) 9.dp else 12.dp))

            // Icon — wrapped in AeroTooltip when collapsed for label-on-hover
            if (mode == AeroSidebarMode.Collapsed) {
                AeroTooltip(text = label) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) colors.primary else colors.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) colors.primary else colors.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Label — only shown in Expanded mode
            if (mode == AeroSidebarMode.Expanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = if (selected) colors.primary else colors.onSurface,
                    style = AeroTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
        }
    }

    /**
     * A section group header label.
     *
     * Visible only in [AeroSidebarMode.Expanded]. In collapsed mode it is hidden
     * (section label would overflow the 48dp icon-only width).
     *
     * @param label the group header text.
     */
    @Composable
    public fun section(label: String) {
        if (mode == AeroSidebarMode.Expanded) {
            Text(
                text = label.uppercase(),
                color = AeroTheme.colors.labelText,
                style = AeroTheme.typography.label,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
        }
    }

    /**
     * A 1dp horizontal divider using [AeroColorScheme.borderDefault].
     */
    @Composable
    public fun divider() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AeroTheme.colors.borderDefault)
        )
    }
}
