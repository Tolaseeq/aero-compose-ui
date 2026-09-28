package com.mordred.aero.components.navigation

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.FrameCorners
import com.mordred.aero.icons.`internal`.Minus
import com.mordred.aero.icons.`internal`.Square
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.theme.AeroTheme

/**
 * NAV-01: Aero-styled custom window title bar.
 *
 * Renders a 32.dp-tall vertical-gradient row containing (left → right):
 *  - optional [leading] slot (e.g., app icon)
 *  - [title] text
 *  - Minimize / Maximize-or-Restore / Close buttons (46.dp × 32.dp each)
 *
 * On Windows the bar is built on the public [rememberAeroWindowChrome] API (SNAP-01): it
 * tells Windows where the caption, the three buttons and interactive content are, and
 * Windows owns the window behavior natively. The caption area answers HTCAPTION (native
 * dragging and Aero Snap — edge, corner and top snapping, with restore on drag-away), the
 * maximize button answers HTMAXBUTTON (hovering it shows the Windows 11 Snap Layouts
 * flyout; a click toggles `WindowPlacement.Maximized <-> Floating`), and maximize is
 * taskbar-aware: a maximized window fills its monitor's work area without covering a
 * visible taskbar and leaves the edge an auto-hidden taskbar needs. Minimize, close and
 * [leading] content stay HTCLIENT and keep their ordinary Compose clicks (BTN-02). If the
 * native install fails, the bar falls back to the legacy composition so the window stays
 * draggable.
 *
 * The [leading] slot is clickable as-is; any other element drawn over the bar must be
 * marked with [markAeroTitleBarInteractive] to receive clicks instead of dragging the
 * window. See the project README ("Windows window behavior") for the verified behavior
 * matrix and known gaps.
 *
 * Pair with `AeroResizeHandles(windowState)` (in `ResizeHandles.kt`) to provide
 * 8-zone window resize on undecorated windows. AeroTitleBar provides the chrome;
 * AeroResizeHandles provides the resize behavior.
 *
 * **Window mode requirement (Win11):** the parent `Window` MUST be created with
 * `undecorated = true` and explicitly `transparent = false`. Setting
 * `transparent = true` causes EXCEPTION_ACCESS_VIOLATION on Windows 11
 * (CMP-3757 / GH#3171). The Aero glass effect is provided by `Modifier.glassEffect`
 * elsewhere — never by window transparency.
 *
 * On Linux and macOS the behavior is unchanged.
 *
 * @param title window title shown in the bar.
 * @param windowState the parent window's [WindowState]; used to toggle Minimized/Maximized.
 * @param onCloseRequest invoked when the user clicks the close button.
 * @param leading optional composable rendered before the title (e.g., an app icon).
 * @param modifier optional layout modifier.
 * @param nativeWindowManagement `false` restores the legacy behavior for this window —
 * Compose drag via `WindowDraggableArea`, Compose resize zones, no native snapping; no
 * effect on other OSes.
 */
@Composable
public fun FrameWindowScope.AeroTitleBar(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    nativeWindowManagement: Boolean = true
) {
    // API-04 / D-05: the native path IS the public chrome API — AeroTitleBar holds no
    // window-subclass wiring of its own, so every app title bar and every custom one
    // run through the same single install.
    val chrome = rememberAeroWindowChrome(windowState, nativeWindowManagement)
    if (chrome.isNative) {
        TitleBarRow(
            title = title,
            windowState = windowState,
            onCloseRequest = onCloseRequest,
            leading = leading,
            rowModifier = modifier,
            chrome = chrome,
        )
    } else {
        WindowDraggableArea(modifier = modifier) {
            TitleBarRow(
                title = title,
                windowState = windowState,
                onCloseRequest = onCloseRequest,
                leading = leading,
                rowModifier = Modifier,
                chrome = null,
            )
        }
    }
}

/**
 * The shared title-bar row for both drag paths. With [chrome] non-null (Windows native
 * path) every region reports its live bounds through the public chrome API — the row is
 * `captionArea()`, minimize/close and the [leading] wrapper are `captionExclude()`, the
 * maximize button is `maximizeButtonArea()` sharing the native-fed interaction source —
 * and each region removes itself when its element leaves composition. With [chrome] null
 * the composition is exactly the legacy row (no region modifiers, no wrapper Box around
 * [leading]).
 */
@Composable
private fun TitleBarRow(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)?,
    rowModifier: Modifier,
    chrome: AeroWindowChromeState?,
) {
    val colors = AeroTheme.colors
    val togglePlacement = {
        windowState.placement =
            if (windowState.placement == WindowPlacement.Maximized)
                WindowPlacement.Floating
            else
                WindowPlacement.Maximized
    }
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .height(32.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.titleBarGradientStart,
                        colors.titleBarGradientEnd
                    )
                )
            )
            .padding(horizontal = 8.dp)
            .then(
                if (chrome == null) Modifier else with(chrome) { Modifier.captionArea() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            if (chrome != null) {
                // BTN-02 / PITFALLS 19: the leading slot is interactive content inside the
                // caption band — excluded so it answers HTCLIENT and keeps its ordinary
                // clicks instead of starting a window drag. A wrap-content Box leaves the
                // layout identical.
                Box(with(chrome) { Modifier.captionExclude() }) {
                    leading()
                }
            } else {
                leading()
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            color = colors.titleBarText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            TitleBarButton(
                icon = AeroIcons.Minus,
                hoverColor = colors.buttonHover,
                contentDescription = "Minimize window",
                onClick = { windowState.isMinimized = true },
                modifier = if (chrome == null) Modifier else with(chrome) { Modifier.captionExclude() }
            )
            TitleBarButton(
                icon = if (windowState.placement == WindowPlacement.Maximized)
                           AeroIcons.FrameCorners
                       else
                           AeroIcons.Square,
                hoverColor = colors.buttonHover,
                contentDescription = if (windowState.placement == WindowPlacement.Maximized)
                                         "Restore window"
                                     else
                                         "Maximize window",
                onClick = togglePlacement,
                modifier = if (chrome == null) Modifier else with(chrome) { Modifier.maximizeButtonArea() },
                interactionSource = chrome?.maximizeInteractionSource
            )
            TitleBarButton(
                icon = AeroIcons.X,
                hoverColor = colors.closeButtonHover,
                contentDescription = "Close window",
                onClick = onCloseRequest,
                modifier = if (chrome == null) Modifier else with(chrome) { Modifier.captionExclude() }
            )
        }
    }
}

/**
 * One caption button (46.dp × 32.dp). With [interactionSource] null the body is the legacy
 * one verbatim (own remembered source; `clickable(onClick)`). With a source given (the
 * native-fed maximize bridge, BTN-01) `hoverable` and `clickable` share it, so the same
 * background expression and the same `LocalIndication` produce hover/press visuals from
 * whichever side fed the interactions (role/focus/keyboard activation unchanged, T-22-22).
 */
@Composable
internal fun TitleBarButton(
    icon: ImageVector,
    hoverColor: Color,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null
) {
    val ownSource = remember { MutableInteractionSource() }
    val source = interactionSource ?: ownSource
    val hovered by source.collectIsHoveredAsState()

    val interactionModifier = if (interactionSource == null) {
        Modifier
            .hoverable(source)
            .clickable(onClick = onClick)
    } else {
        Modifier
            .hoverable(source)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onClick = onClick,
            )
    }

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 32.dp)
            .then(interactionModifier)
            .background(if (hovered) hoverColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(12.dp),
            tint = AeroTheme.colors.onSurface
        )
    }
}
