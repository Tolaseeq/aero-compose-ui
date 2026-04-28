package com.mordred.aero.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * SEL-02: Single radio button with animated dot scale and border.
 */
@Composable
public fun AeroRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(50)

    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.borderDefault,
        animationSpec = tween(150, easing = LinearEasing),
        label = "radioBorder"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "radioDot"
    )

    val radioContent = @Composable {
        Box(
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.4f)
                .size(16.dp)
                .border(1.dp, borderColor, shape)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { onClick?.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                    .background(colors.primary, RoundedCornerShape(50))
            )
        }
    }

    if (label != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            radioContent()
            Text(label, color = colors.onSurface, style = AeroTheme.typography.bodyLarge)
        }
    } else {
        Box(modifier = modifier) {
            radioContent()
        }
    }
}

/**
 * SEL-02: Radio group helper that enforces single-selection across a list of options.
 */
@Composable
public fun <T> AeroRadioGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { it.toString() }
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        for (opt in options) {
            AeroRadioButton(
                selected = (opt == selected),
                onClick = { onSelect(opt) },
                enabled = enabled,
                label = optionLabel(opt)
            )
        }
    }
}
