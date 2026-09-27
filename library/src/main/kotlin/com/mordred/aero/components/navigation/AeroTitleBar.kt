package com.mordred.aero.components.navigation

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.mordred.aero.internal.windows.HitTestRegionRegistry
import com.mordred.aero.internal.windows.LEADING_INTERACTIVE_ID
import com.mordred.aero.internal.windows.NativeWindowChromeRegistry
import com.mordred.aero.internal.windows.PxRect
import com.mordred.aero.internal.windows.TitleBarRole
import com.mordred.aero.internal.windows.WindowRegionsDirectory
import com.mordred.aero.internal.windows.isWindowsOs
import com.mordred.aero.theme.AeroTheme
import kotlin.math.roundToInt

/**
 * NAV-01: Aero-styled custom window title bar.
 *
 * Renders a 32.dp-tall vertical-gradient row containing (left → right):
 *  - optional [leading] slot (e.g., app icon)
 *  - [title] text
 *  - Minimize / Maximize-or-Restore / Close buttons (46.dp × 32.dp each)
 *
 * On Windows the row's live layout regions are published to the native hit-test
 * subclass (SNAP-01): the caption area answers HTCAPTION to the OS, which owns
 * window dragging natively, and the maximize button answers HTMAXBUTTON (Snap
 * Layouts). Minimize, close and [leading] content stay HTCLIENT and keep their
 * ordinary Compose clicks (BTN-02). If the native install fails, the bar falls
 * back to the legacy path below so the window stays draggable.
 *
 * Off Windows (and on the fallback path) the whole row is wrapped in
 * `WindowDraggableArea`, so users can drag the window from any non-button area.
 * Each control button has its own `clickable` so click wins over drag.
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
 * **Aero Snap limitation (legacy path):** `WindowDraggableArea` does NOT pass
 * HTCAPTION to the OS, so dragging to a screen edge does NOT trigger Windows
 * native Aero Snap (snap-to-half, snap-to-quadrant). Native hit-testing on
 * Windows removes this limitation; the limitation text applies to the
 * non-Windows / fallback composition only.
 *
 * @param title window title shown in the bar.
 * @param windowState the parent window's [WindowState]; used to toggle Minimized/Maximized.
 * @param onCloseRequest invoked when the user clicks the close button.
 * @param leading optional composable rendered before the title (e.g., an app icon).
 * @param modifier optional layout modifier.
 */
@Composable
public fun FrameWindowScope.AeroTitleBar(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val nativeRequested = isWindowsOs
    var nativeFailed by remember { mutableStateOf(false) }
    if (nativeRequested && !nativeFailed) {
        // SNAP-01 / PITFALLS 15: native HTCAPTION hit-testing owns the drag — a single drag
        // path, no WindowDraggableArea wrapper.
        val regions = remember(window) { WindowRegionsDirectory.forWindow(window) }
        DisposableEffect(window) {
            val handle = NativeWindowChromeRegistry.acquire(window)
            // T-22-02: a failed install flips to the legacy draggable path below.
            if (handle == null) nativeFailed = true
            onDispose { handle?.release() }
        }
        TitleBarRow(
            title = title,
            windowState = windowState,
            onCloseRequest = onCloseRequest,
            leading = leading,
            rowModifier = modifier,
            regions = regions,
        )
    } else {
        WindowDraggableArea(modifier = modifier) {
            TitleBarRow(
                title = title,
                windowState = windowState,
                onCloseRequest = onCloseRequest,
                leading = leading,
                rowModifier = Modifier,
                regions = null,
            )
        }
    }
}

private fun Rect.toPxRect(): PxRect =
    PxRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

/** Client-area bounds in physical px — the pre-1.12 no-arg `boundsInWindow()` semantics. */
private fun androidx.compose.ui.layout.LayoutCoordinates.clientBounds(): Rect =
    boundsInWindow(clipBounds = true)

/**
 * The modifier a region-publishing call site prepends/appends when [regions] is non-null;
 * [Modifier] (a no-op element) otherwise, so the non-Windows / fallback composition keeps
 * exactly the legacy modifier chain.
 */
private fun regionModifier(regions: HitTestRegionRegistry?, publish: HitTestRegionRegistry.(Rect) -> Unit): Modifier =
    if (regions == null) {
        Modifier
    } else {
        Modifier.onGloballyPositioned { regions.publish(it.clientBounds()) }
    }

/**
 * The shared title-bar row for both drag paths. With [regions] non-null (Windows native
 * path) every region reports its live bounds via `onGloballyPositioned`; with [regions]
 * null the composition is exactly the legacy row (no region-reporting modifiers, no
 * wrapper Box around [leading]).
 */
@Composable
private fun TitleBarRow(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)?,
    rowModifier: Modifier,
    regions: HitTestRegionRegistry?,
) {
    val colors = AeroTheme.colors
    if (regions != null) {
        DisposableEffect(regions) {
            onDispose {
                regions.publishRole(TitleBarRole.Caption, null)
                regions.publishRole(TitleBarRole.Minimize, null)
                regions.publishRole(TitleBarRole.Maximize, null)
                regions.publishRole(TitleBarRole.Close, null)
                regions.publishInteractive(LEADING_INTERACTIVE_ID, null)
            }
        }
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
            .then(regionModifier(regions) { rect -> publishRole(TitleBarRole.Caption, rect.toPxRect()) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            if (regions != null) {
                // BTN-02 / PITFALLS 19: the leading slot is interactive content inside the
                // caption band — published as interactive so it answers HTCLIENT and keeps
                // its ordinary clicks instead of starting a window drag. A wrap-content Box
                // leaves the layout identical.
                Box(
                    Modifier.onGloballyPositioned {
                        regions.publishInteractive(LEADING_INTERACTIVE_ID, it.clientBounds().toPxRect())
                    }
                ) {
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
                modifier = regionModifier(regions) { rect -> publishRole(TitleBarRole.Minimize, rect.toPxRect()) }
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
                onClick = {
                    windowState.placement =
                        if (windowState.placement == WindowPlacement.Maximized)
                            WindowPlacement.Floating
                        else
                            WindowPlacement.Maximized
                },
                modifier = regionModifier(regions) { rect -> publishRole(TitleBarRole.Maximize, rect.toPxRect()) }
            )
            TitleBarButton(
                icon = AeroIcons.X,
                hoverColor = colors.closeButtonHover,
                contentDescription = "Close window",
                onClick = onCloseRequest,
                modifier = regionModifier(regions) { rect -> publishRole(TitleBarRole.Close, rect.toPxRect()) }
            )
        }
    }
}

@Composable
private fun TitleBarButton(
    icon: ImageVector,
    hoverColor: Color,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 32.dp)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
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
