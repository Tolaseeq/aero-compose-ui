package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.list.AeroBadge
import com.mordred.aero.components.list.AeroListItem
import com.mordred.aero.theme.AeroTheme

/**
 * 19-04: extended with state-matrix rows for [AeroListItem] so every state named in
 * 19-04-PLAN.md's `must_haves.truths` is reachable by a reviewer with a mouse and a Tab key,
 * without editing code — the four selected x hovered combinations (via the live click-driven
 * "Inbox"/"Sent"/"Drafts" rows), two adjacent pinned-selected rows (the pill adjacency backstop),
 * a disabled row, a display-only (`onClick == null`) row that must gain no focus stop, and a
 * long-label selected row exercising the E3 overflow backstop (VLST-01, VLST-02, VLST-03).
 */
@Composable
fun ListSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var selectedIndex by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Lists & Data Display", color = colors.onBackground, style = typography.title)

        Text(
            text = "AeroListItem — click a row to select it, then hover that SAME row (the VLST-02 " +
                "combined selected+hovered state); hover an unselected row for the neutral hover pill; " +
                "Tab through for the in-bounds focus stroke.",
            color = colors.labelText,
            style = typography.bodySmall,
        )
        ListRow(label = "AeroListItem") {
            Column(Modifier.width(360.dp)) {
                listOf("Inbox", "Sent", "Drafts").forEachIndexed { i, item ->
                    AeroListItem(
                        text = item,
                        onClick = { selectedIndex = i },
                        selected = selectedIndex == i,
                        secondaryText = "secondary line",
                        trailingContent = { AeroBadge(text = "${i + 1}") }
                    )
                }
            }
        }

        Text(
            text = "AeroListItem — two rows pinned selected as immediate siblings, confirming they " +
                "read as two separate pills with a visible gap rather than merging into one bar.",
            color = colors.labelText,
            style = typography.bodySmall,
        )
        ListRow(label = "AeroListItem (adjacent selected)") {
            Column(Modifier.width(360.dp)) {
                AeroListItem(text = "Pinned selected — first", onClick = {}, selected = true)
                AeroListItem(text = "Pinned selected — second", onClick = {}, selected = true)
            }
        }

        Text(
            text = "AeroListItem — a disabled row (dead pill + dimmed content), a display-only row " +
                "with no onClick (must gain no focus stop when tabbing), and a selected row with a " +
                "deliberately long primary label plus a secondary line to exercise the E3 overflow " +
                "backstop against the pill's bounded edge.",
            color = colors.labelText,
            style = typography.bodySmall,
        )
        ListRow(label = "AeroListItem (states)") {
            Column(Modifier.width(360.dp)) {
                AeroListItem(text = "Disabled row", onClick = {}, enabled = false)
                AeroListItem(text = "Display-only row (no onClick)", selected = false)
                AeroListItem(
                    text = LONG_LIST_ITEM_LABEL,
                    selected = true,
                    secondaryText = "Secondary line stays visible beneath the overflowing primary text"
                )
            }
        }

        ListRow(label = "AeroBadge") {
            AeroBadge(text = "12")
            AeroBadge(text = "NEW")
            AeroBadge(text = "!", color = colors.error, contentColor = colors.onError)
        }
    }
}

/** Deliberately long (>=60 char) primary label exercising the E3 overflow/long-text backstop (19-UI-SPEC.md). */
private const val LONG_LIST_ITEM_LABEL =
    "This is a deliberately long primary label used to exercise the AeroListItem pill overflow backstop"

@Composable
private fun ListRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyMedium, modifier = Modifier.width(140.dp).padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
