package com.mordred.aero.components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * NAV-04: Single navigation breadcrumb item — `label` plus an optional `payload`
 * the caller can attach to identify the navigation target.
 */
public data class AeroBreadcrumbItem(
    val label: String,
    val payload: Any? = null
)

/**
 * NAV-04: Clickable navigation chain rendered as `Home › Folder › Page`.
 *
 * The LAST item represents the current page and is rendered non-clickable in
 * [com.mordred.aero.theme.AeroColorScheme.onSurface]. All earlier items are rendered
 * clickable in [com.mordred.aero.theme.AeroColorScheme.primary] and emit
 * [onItemClick] when clicked.
 *
 * **Overflow:** v1 ships horizontal layout only — no truncate-middle / scroll. Long
 * lists wrap thanks to caller-supplied layout (e.g. FlowRow); v2 may add ellipsis modes.
 *
 * @param items breadcrumb segments, root → current.
 * @param onItemClick invoked when a non-last segment is clicked.
 * @param modifier outer modifier.
 * @param separator separator glyph between segments (default `›`, alternatives `>` `/`).
 */
@Composable
public fun AeroBreadcrumb(
    items: List<AeroBreadcrumbItem>,
    onItemClick: (Int, AeroBreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    separator: String = "›"
) {
    val colors = AeroTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val segmentModifier = if (!isLast) {
                Modifier.clickable { onItemClick(index, item) }
            } else {
                Modifier
            }
            Text(
                text = item.label,
                color = if (isLast) colors.onSurface else colors.primary,
                style = AeroTheme.typography.bodyLarge,
                modifier = segmentModifier
            )
            if (!isLast) {
                Text(
                    text = separator,
                    color = colors.labelText,
                    style = AeroTheme.typography.bodyLarge
                )
            }
        }
    }
}
