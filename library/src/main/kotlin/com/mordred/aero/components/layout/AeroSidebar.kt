package com.mordred.aero.components.layout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mordred.aero.theme.glassPanel

/**
 * LAYO-05, LAYO-06, LAYO-07: Persistent in-layout vertical navigation sidebar with three
 * animated display modes.
 *
 * ## Modes
 * - **Expanded** (~240dp): icon + label rows, section headers, full DSL visible.
 * - **Collapsed** (~48dp): icon-only rows; item labels shown in [AeroTooltip] on hover.
 * - **Hidden** (0dp): sidebar takes no space — adjacent content fills the full width.
 *
 * ## Animation
 * Width transitions between modes are driven by [animateDpAsState] with a 200ms
 * [FastOutSlowInEasing] tween. The live animated width is written back to
 * [AeroSidebarState.widthState] each frame via [SideEffect] so that adjacent layout can
 * read [AeroSidebarState.currentWidthDp] and reflow without hard-coding the width.
 *
 * ## PITFALL-11 — Do NOT nest inside AeroSplitPane
 * Place [AeroSidebar] as a **top-level sibling** in a [Row], never inside a pane of
 * [AeroSplitPane]. Two independent width systems will fight: SplitPane's divider offset
 * and Sidebar's animated width, causing layout thrash during collapse.
 *
 * Correct caller pattern:
 * ```kotlin
 * val sidebarState = rememberAeroSidebarState()
 * Row {
 *     AeroSidebar(state = sidebarState, header = { /* logo / user avatar */ }) {
 *         item(AeroIcons.House, "Home", selected = currentRoute == "home") {
 *             currentRoute = "home"
 *         }
 *         section("Library")
 *         item(AeroIcons.Gear, "Settings", selected = currentRoute == "settings") {
 *             currentRoute = "settings"
 *         }
 *         divider()
 *     }
 *     Box(Modifier.weight(1f)) { mainContent() }
 * }
 * // Do NOT place AeroSidebar inside an AeroSplitPane pane (PITFALL-11).
 * ```
 *
 * @param state the [AeroSidebarState] driving the current mode and exposing the live
 *   animated width via [AeroSidebarState.currentWidthDp].
 * @param modifier applied to the outer [Box] container.
 * @param header optional composable rendered at the top of the sidebar (e.g. logo, user avatar).
 * @param footer optional composable rendered at the bottom of the sidebar (e.g. account menu).
 * @param content the [AeroSidebarScope] slot — declare items, sections, and dividers here.
 */
@Composable
public fun AeroSidebar(
    state: AeroSidebarState,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable AeroSidebarScope.() -> Unit,
) {
    val animatedWidth by animateDpAsState(
        targetValue = targetWidthForMode(state.mode),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "sidebarWidth",
    )

    // PITFALL-11: write the live animated value back into the state so adjacent layout
    // can read state.currentWidthDp and reflow in sync with the animation.
    SideEffect { state.widthState.value = animatedWidth }

    Box(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .glassPanel(),
    ) {
        Column {
            // Optional header slot (logo, user avatar, branding)
            header?.invoke()

            // Item/section/divider content via scope DSL
            AeroSidebarScope(mode = state.mode).content()

            // Push footer to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Optional footer slot (account, sign-out, etc.)
            footer?.invoke()
        }
    }
}
