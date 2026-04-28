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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.mordred.aero.theme.AeroTheme

/**
 * NAV-01: Aero-styled custom window title bar.
 *
 * Renders a 32.dp-tall vertical-gradient row containing (left → right):
 *  - optional [leading] slot (e.g., app icon)
 *  - [title] text
 *  - Minimize / Maximize-or-Restore / Close buttons (46.dp × 32.dp each)
 *
 * The whole row is wrapped in `WindowDraggableArea`, so users can drag the window
 * from any non-button area. Each control button has its own `clickable` so click
 * wins over drag.
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
 * **Aero Snap limitation:** `WindowDraggableArea` does NOT pass HTCAPTION to the OS,
 * so dragging to a screen edge does NOT trigger Windows native Aero Snap (snap-to-half,
 * snap-to-quadrant). This is a known CMP limitation — see PROJECT.md Out-of-Scope.
 * Aero Snap support requires JNI/WinAPI and is explicitly v2+.
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
    val colors = AeroTheme.colors
    WindowDraggableArea(modifier = modifier) {
        Row(
            modifier = Modifier
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
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
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
                    glyph = "─",
                    hoverColor = colors.buttonHover,
                    textColor = colors.titleBarText,
                    onClick = { windowState.isMinimized = true }
                )
                TitleBarButton(
                    glyph = if (windowState.placement == WindowPlacement.Maximized) "❒" else "□",
                    hoverColor = colors.buttonHover,
                    textColor = colors.titleBarText,
                    onClick = {
                        windowState.placement =
                            if (windowState.placement == WindowPlacement.Maximized)
                                WindowPlacement.Floating
                            else
                                WindowPlacement.Maximized
                    }
                )
                TitleBarButton(
                    glyph = "✕",
                    hoverColor = colors.closeButtonHover,
                    textColor = colors.titleBarText,
                    onClick = onCloseRequest
                )
            }
        }
    }
}

@Composable
private fun TitleBarButton(
    glyph: String,
    hoverColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 32.dp)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .background(if (hovered) hoverColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = textColor,
            fontSize = 13.sp
        )
    }
}
