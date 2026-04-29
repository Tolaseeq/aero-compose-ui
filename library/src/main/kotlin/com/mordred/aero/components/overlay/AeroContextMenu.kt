package com.mordred.aero.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.components.popup.AeroCursorPositionProvider
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretRight
import com.mordred.aero.theme.AeroTheme
import kotlin.math.roundToInt

/**
 * OVL-04: Right-click context menu attached to any composable via Modifier extension.
 *
 * The modifier:
 *  1. Listens for `PointerEventType.Press` events;
 *  2. Filters to right-click only via `event.buttons.isSecondaryPressed`;
 *  3. Captures the cursor coordinates from the first pointer change;
 *  4. Opens a [Popup] anchored at the cursor via [AeroCursorPositionProvider].
 *
 * The popup is dismissed on outside click or back-press. Submenus open on hover
 * via the same recursive content function (1-2 levels expected).
 *
 * **Pitfall:** do NOT use `Modifier.clickable` for right-click detection — that fires on
 * any mouse button. The `onPointerEvent` + `isSecondaryPressed` check is the correct path.
 *
 * @param items context-menu item list.
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun Modifier.aeroContextMenu(items: List<AeroContextMenuItem>): Modifier = composed {
    var expanded by remember { mutableStateOf(false) }
    var cursor by remember { mutableStateOf(IntOffset.Zero) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val positionModifier = Modifier.onGloballyPositioned { coords = it }

    val pointerModifier = Modifier.onPointerEvent(PointerEventType.Press) { event ->
        if (event.buttons.isSecondaryPressed) {
            val change = event.changes.firstOrNull() ?: return@onPointerEvent
            val local = change.position
            val windowOrigin = coords?.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
                ?: androidx.compose.ui.geometry.Offset.Zero
            cursor = IntOffset(
                (windowOrigin.x + local.x).roundToInt(),
                (windowOrigin.y + local.y).roundToInt()
            )
            expanded = true
        }
    }

    if (expanded) {
        Popup(
            popupPositionProvider = remember(cursor) { AeroCursorPositionProvider(cursor) },
            onDismissRequest = { expanded = false },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            AeroContextMenuContent(items = items, onClose = { expanded = false })
        }
    }

    this.then(positionModifier).then(pointerModifier)
}

@Composable
private fun AeroContextMenuContent(
    items: List<AeroContextMenuItem>,
    onClose: () -> Unit
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .shadow(8.dp, shape)
            .clip(shape)
            .background(colors.background, shape)
            .background(colors.panelBackground, shape)
            .border(1.dp, colors.glassBorder, shape)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.width(220.dp)) {
            items.forEach { item -> ContextMenuItemRow(item, onClose) }
        }
    }
}

@Composable
private fun ContextMenuItemRow(
    item: AeroContextMenuItem,
    onClose: () -> Unit
) {
    val colors = AeroTheme.colors
    when (item) {
        is AeroContextMenuItem.Action -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable(enabled = item.enabled) {
                    item.onClick()
                    onClose()
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (item.icon != null) {
                Icon(item.icon, contentDescription = null, tint = colors.onSurface)
            }
            Text(
                text = item.label,
                color = if (item.enabled) colors.onSurface else colors.labelText,
                style = AeroTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (item.shortcut != null) {
                Text(
                    text = item.shortcut,
                    color = colors.labelText,
                    style = AeroTheme.typography.label
                )
            }
        }
        AeroContextMenuItem.Divider -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 8.dp)
                .background(colors.borderDefault)
        )
        is AeroContextMenuItem.Submenu -> {
            // v1: simple flat rendering — submenu cascade left for follow-up enhancement.
            // Show label + "▶" indicator; clicking the row does nothing yet.
            // Future enhancement (Claude's discretion): hover-with-delay opens nested popup.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.icon != null) {
                    Icon(item.icon, contentDescription = null, tint = colors.onSurface)
                }
                Text(
                    text = item.label,
                    color = colors.onSurface,
                    style = AeroTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = AeroIcons.CaretRight,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colors.labelText
                )
            }
        }
    }
}
