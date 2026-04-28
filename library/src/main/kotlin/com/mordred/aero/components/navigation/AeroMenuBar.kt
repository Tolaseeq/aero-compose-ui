package com.mordred.aero.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.popup.AeroDropdownItem
import com.mordred.aero.components.popup.AeroDropdownPopup
import com.mordred.aero.components.popup.AeroPopupSide
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassPanel

/**
 * NAV-02: Top-of-window menu bar with click-to-open + cross-item hover-switch.
 *
 * **State model (the "spike" called out in CONTEXT.md):**
 *  - A single `openIndex: Int` is hoisted at AeroMenuBar level (-1 = no menu open).
 *  - Click on a top-level item toggles its dropdown: if openIndex matches the clicked
 *    index, set to -1; otherwise set to clicked index.
 *  - When `openIndex >= 0`, hovering ANY OTHER top-level item auto-switches openIndex
 *    to the hovered index. This is the "menu cascade" behavior familiar from desktop OSes.
 *  - Outside-click on the dropdown calls onDismissRequest, which sets openIndex back to -1.
 *
 * **Alt+letter mnemonics:** explicitly deferred to v2 per CONTEXT.md.
 *
 * @param menus list of top-level menus (label + dropdown items).
 * @param modifier optional outer modifier.
 */
@Composable
public fun AeroMenuBar(
    menus: List<AeroTopLevelMenu>,
    modifier: Modifier = Modifier
) {
    var openIndex by remember { mutableStateOf(-1) }
    val typography = AeroTheme.typography
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .glassPanel(cornerRadius = 0.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        menus.forEachIndexed { index, menu ->
            // Compute the popup width once per menu by measuring every label and
            // taking the widest, plus the item's horizontal padding. Passing this as
            // anchorWidth makes the popup hug the longest item and avoids the
            // SubcomposeLayout flicker that a runtime two-pass approach caused.
            val popupAnchorWidth: Dp = remember(menu, density, typography) {
                val labels = menu.items.mapNotNull { item ->
                    when (item) {
                        is AeroMenuItem.Action -> item.label
                        AeroMenuItem.Divider -> null
                        is AeroMenuItem.Submenu -> "${item.label}    ▶"
                    }
                }
                val widestPx = labels.maxOfOrNull { label ->
                    textMeasurer.measure(label, typography.bodyLarge).size.width
                } ?: 0
                with(density) { widestPx.toDp() } + 16.dp
            }.coerceIn(120.dp, 320.dp)

            TopLevelLabel(
                label = menu.label,
                isOpen = openIndex == index,
                onClick = {
                    openIndex = if (openIndex == index) -1 else index
                },
                onHover = { hovered ->
                    if (hovered && openIndex >= 0 && openIndex != index) {
                        openIndex = index
                    }
                },
                content = {
                    if (openIndex == index) {
                        AeroDropdownPopup(
                            expanded = true,
                            anchorWidth = popupAnchorWidth,
                            onDismissRequest = { openIndex = -1 },
                            side = AeroPopupSide.Bottom
                        ) {
                            menu.items.forEach { item ->
                                MenuItemRow(item, onCloseAll = { openIndex = -1 })
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun TopLevelLabel(
    label: String,
    isOpen: Boolean,
    onClick: () -> Unit,
    onHover: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(hovered) { onHover(hovered) }

    Box {
        Box(
            modifier = Modifier
                .height(28.dp)
                .background(
                    if (isOpen || hovered) colors.buttonHover else Color.Transparent
                )
                .hoverable(interactionSource)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = colors.onSurface,
                style = AeroTheme.typography.bodyLarge
            )
        }
        content()
    }
}

@Composable
private fun MenuItemRow(item: AeroMenuItem, onCloseAll: () -> Unit) {
    val colors = AeroTheme.colors
    when (item) {
        is AeroMenuItem.Action -> AeroDropdownItem(
            text = item.label,
            selected = false,
            highlighted = false,
            onClick = {
                item.onClick()
                onCloseAll()
            }
        )
        AeroMenuItem.Divider -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 8.dp)
                .background(colors.borderDefault)
        )
        is AeroMenuItem.Submenu -> AeroDropdownItem(
            text = "${item.label}    ▶",   // simple flat indicator; cascade deferred
            selected = false,
            highlighted = false,
            onClick = { /* v1: no-op; cascade is enhancement */ }
        )
    }
}
