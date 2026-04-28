package com.mordred.aero.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * NAV-05 visual partner — a single tab descriptor. Optional [icon] is rendered
 * before the label.
 */
public data class AeroTab(
    val label: String,
    val icon: (@Composable () -> Unit)? = null
)

/**
 * NAV-05: Browser-style horizontal tab bar with active-underline indicator.
 *
 * **Overflow handling:** `Modifier.horizontalScroll(rememberScrollState())` on the
 * outer Row, so 7+ tabs simply scroll horizontally. A sophisticated overflow menu
 * is v2.
 *
 * **Active indicator:** the selected tab gets a glassSurface background + a 2.dp
 * borderSelected underline (so the indicator is visible even when the bg blends
 * into the surrounding chrome).
 *
 * @param tabs list of tab descriptors.
 * @param selectedIndex the currently active tab index.
 * @param onSelect callback invoked when the user clicks a tab.
 * @param modifier outer modifier.
 */
@Composable
public fun AeroTabBar(
    tabs: List<AeroTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            TabCell(
                tab = tab,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun TabCell(
    tab: AeroTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> colors.glassSurface
            hovered  -> colors.buttonHover
            else     -> Color.Transparent
        },
        animationSpec = tween(150, easing = LinearEasing),
        label = "tabBg"
    )
    val underline = if (selected) colors.borderSelected else Color.Transparent

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .background(bgColor)
                .hoverable(interactionSource)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (tab.icon != null) tab.icon.invoke()
            Text(
                text = tab.label,
                color = colors.onSurface,
                style = AeroTheme.typography.bodyLarge
            )
        }
        // Active underline (2.dp tall borderSelected); inactive tabs draw a transparent line
        Box(
            modifier = Modifier
                .width(80.dp)        // visual underline width baseline; long labels still highlight via bg
                .height(2.dp)
                .background(underline)
        )
    }
}
