package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.components.overlay.AeroAlertDialog
import com.mordred.aero.components.overlay.AeroAlertKind
import com.mordred.aero.components.overlay.AeroBannerKind
import com.mordred.aero.components.overlay.AeroContextMenuItem
import com.mordred.aero.components.overlay.AeroDialog
import com.mordred.aero.components.overlay.AeroDrawer
import com.mordred.aero.components.overlay.AeroDrawerSide
import com.mordred.aero.components.overlay.AeroNotificationBanner
import com.mordred.aero.components.overlay.AeroPopover
import com.mordred.aero.components.overlay.AeroToastHostState
import com.mordred.aero.components.overlay.AeroTooltip
import com.mordred.aero.components.overlay.aeroContextMenu
import com.mordred.aero.components.popup.AeroPopupSide
import com.mordred.aero.theme.AeroTheme
import kotlinx.coroutines.launch

/**
 * Phase 3 — Overlays section: OVL-01..OVL-08.
 *
 * Each overlay is triggered by an inline button so the user can demonstrate it
 * during the visual checkpoint. The [toastState] is owned by ShowcaseApp and
 * passed in here so the toast triggers can reach the host mounted at app root.
 *
 * **Right-click context menu wiring note:** the AeroOutlinedButton wraps its
 * own clickable handler which may not surface secondary clicks. If the visual
 * checkpoint shows the right-click does NOT open the menu, switch the wiring
 * to a Box wrapper so the modifier sits OUTSIDE the button's pointer-event
 * consumer. Documented in 03-08-SUMMARY.md.
 */
@Composable
fun OverlaysSection(toastState: AeroToastHostState) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val scope = rememberCoroutineScope()

    var dialogOpen by remember { mutableStateOf(false) }
    var alertKind by remember { mutableStateOf<AeroAlertKind?>(null) }
    var popoverOpen by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    var bannerVisible by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Overlays", color = colors.onBackground, style = typography.title)

        SectionRow(label = "AeroDialog") {
            AeroButton(text = "Open dialog", onClick = { dialogOpen = true })
        }
        if (dialogOpen) {
            AeroDialog(
                onDismissRequest = { dialogOpen = false },
                title = { Text("Dialog title", color = colors.onSurface, style = typography.title) },
                content = { Text("Dialog content body.", color = colors.onSurface, style = typography.bodyLarge) },
                buttons = {
                    AeroOutlinedButton(text = "Cancel", onClick = { dialogOpen = false })
                    Spacer(Modifier.width(8.dp))
                    AeroButton(text = "OK", onClick = { dialogOpen = false })
                }
            )
        }

        SectionRow(label = "AeroAlertDialog") {
            AeroOutlinedButton(text = "Info",     onClick = { alertKind = AeroAlertKind.Info })
            AeroOutlinedButton(text = "Warning",  onClick = { alertKind = AeroAlertKind.Warning })
            AeroOutlinedButton(text = "Error",    onClick = { alertKind = AeroAlertKind.Error })
            AeroOutlinedButton(text = "Question", onClick = { alertKind = AeroAlertKind.Question })
        }
        alertKind?.let { kind ->
            AeroAlertDialog(
                onDismissRequest = { alertKind = null },
                kind = kind,
                title = "Alert: $kind",
                message = "This is an AeroAlertDialog of kind $kind."
            )
        }

        SectionRow(label = "AeroTooltip") {
            AeroTooltip(text = "Tooltip text — appears after 600ms hover") {
                AeroOutlinedButton(text = "Hover me", onClick = {})
            }
        }

        SectionRow(label = "AeroContextMenu") {
            // Box wrapper so Modifier.aeroContextMenu sits OUTSIDE the button's
            // pointer-event consumer. AeroOutlinedButton's own clickable can otherwise
            // swallow the secondary-press event before our pointer-input modifier sees it.
            Box(
                modifier = Modifier.aeroContextMenu(
                    items = listOf(
                        AeroContextMenuItem.Action(label = "Copy", onClick = {}),
                        AeroContextMenuItem.Action(label = "Paste", onClick = {}),
                        AeroContextMenuItem.Divider,
                        AeroContextMenuItem.Action(label = "Delete", onClick = {})
                    )
                )
            ) {
                AeroOutlinedButton(text = "Right-click me", onClick = {})
            }
        }

        SectionRow(label = "AeroToast") {
            AeroOutlinedButton(
                text = "Show toast",
                onClick = { scope.launch { toastState.showToast("Saved successfully") } }
            )
            AeroOutlinedButton(
                text = "Show 5 toasts",
                onClick = {
                    repeat(5) { i ->
                        scope.launch { toastState.showToast("Toast #${i + 1}") }
                    }
                }
            )
        }

        SectionRow(label = "AeroNotificationBanner") {
            Column(
                modifier = Modifier.width(440.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (bannerVisible) {
                    AeroNotificationBanner(
                        kind = AeroBannerKind.Info,
                        text = "Info banner with dismiss button.",
                        onDismiss = { bannerVisible = false }
                    )
                }
                AeroNotificationBanner(kind = AeroBannerKind.Warning, text = "Warning banner.")
                AeroNotificationBanner(kind = AeroBannerKind.Error, text = "Error banner.")
                AeroNotificationBanner(kind = AeroBannerKind.Success, text = "Success banner.")
            }
        }

        SectionRow(label = "AeroPopover") {
            Box {
                AeroOutlinedButton(text = "Toggle popover", onClick = { popoverOpen = !popoverOpen })
                AeroPopover(
                    expanded = popoverOpen,
                    onDismissRequest = { popoverOpen = false },
                    side = AeroPopupSide.Bottom
                ) {
                    Column(modifier = Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Popover title", color = colors.onSurface, style = typography.title)
                        Text("Popover body content.", color = colors.onSurface, style = typography.bodyLarge)
                    }
                }
            }
        }

        SectionRow(label = "AeroDrawer") {
            AeroOutlinedButton(text = "Open Start drawer", onClick = { drawerOpen = true })
        }
        AeroDrawer(
            open = drawerOpen,
            onDismissRequest = { drawerOpen = false },
            side = AeroDrawerSide.Start,
            drawerWidth = 280.dp
        ) {
            Text(
                text = "Drawer content",
                color = colors.onSurface,
                style = typography.title,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "Click the scrim or press Esc to dismiss.",
                color = colors.onSurface,
                style = typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun SectionRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = AeroTheme.colors.labelText,
            style = AeroTheme.typography.bodyMedium,
            modifier = Modifier.width(140.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
