package com.mordred.aero.components.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * LST-01: Aero-styled list row with hover and selection state.
 *
 * Background animates using [AeroTheme.colors.buttonHover] on hover and a
 * semi-transparent [AeroTheme.colors.primary] overlay when [selected].
 *
 * @param text Primary label text.
 * @param onClick Click handler. If null, the row is non-clickable.
 * @param modifier Layout modifier.
 * @param enabled Whether the item responds to interaction.
 * @param selected Whether the row displays a selection highlight.
 * @param leadingContent Optional composable rendered before the text column.
 * @param trailingContent Optional composable rendered after the text column.
 * @param secondaryText Optional secondary label rendered below [text].
 * @param interactionSource Interaction source — pass to share state with parent.
 */
@Composable
public fun AeroListItem(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    secondaryText: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeroTheme.colors
    val hovered by interactionSource.collectIsHoveredAsState()

    val animatedBg by animateColorAsState(
        targetValue = when {
            selected -> colors.primary.copy(alpha = 0.2f)
            hovered && enabled -> colors.buttonHover
            else -> Color.Transparent
        },
        animationSpec = tween(150, easing = LinearEasing),
        label = "listItemBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(animatedBg)
            .hoverable(interactionSource)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 12.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingContent != null) {
            leadingContent()
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = text,
                color = colors.onSurface,
                style = AeroTheme.typography.bodyLarge
            )
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    color = colors.labelText,
                    style = AeroTheme.typography.bodySmall
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
