package com.mordred.aero.components.layout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.layout.internal.accordion.accordionToggleMulti
import com.mordred.aero.components.layout.internal.accordion.accordionToggleSingle
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretRight
import com.mordred.aero.theme.AeroTheme
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.graphics.graphicsLayer
import com.mordred.aero.theme.glassPanel

/**
 * Display mode for [AeroAccordion]: controls how many sections can be open simultaneously.
 */
public enum class AeroAccordionMode {
    /**
     * Exactly one section open at a time. Opening section B automatically closes section A.
     * Use [LAYO-02] for single-mode coordination via [accordionToggleSingle].
     */
    Single,

    /**
     * Any number of sections can be open simultaneously (default).
     * [LAYO-01, LAYO-02]
     */
    Multi,
}

/**
 * A single collapsible section for [AeroAccordion].
 *
 * @param title header text shown in all states.
 * @param leadingIcon optional icon rendered left of the title. Tint is always explicit
 *   (v1.1 rule: [AeroIcons] tint must never inherit LocalContentColor).
 * @param content composable body shown when the section is expanded. The lambda
 *   is **not** a class — there is no DSL overhead (PITFALL-13: sections hold no state).
 */
public data class AeroAccordionSection(
    public val title: String,
    public val leadingIcon: ImageVector? = null,
    public val content: @Composable () -> Unit,
)

/**
 * A list of collapsible sections with single- or multi-mode expansion.
 *
 * [LAYO-01, LAYO-02]
 *
 * ## Hybrid ownership (controlled vs uncontrolled)
 *
 * When `onExpandedChange == null` (default) the component is **uncontrolled**: it manages
 * expansion state internally, seeded from [initiallyExpanded]. This is the common case.
 *
 * When `onExpandedChange != null` the component is **controlled**: the caller owns all
 * expansion state and [AeroAccordion] becomes a pure renderer. The caller must supply the
 * current state via [expandedIndices] (mode = [AeroAccordionMode.Multi]) or [expandedIndex]
 * (mode = [AeroAccordionMode.Single]) and respond to [onExpandedChange] to update it.
 * **Do not collapse to one branch** — both paths are intentional (matches AeroDataTable
 * hybrid-sort and AeroTableColumn KDoc convention).
 *
 * ## Animation
 *
 * Content area uses [Modifier.animateContentSize] (~160ms, [FastOutSlowInEasing]).
 * The caret rotates 0° → 90° via [animateFloatAsState]. Neither uses a height-measurement
 * approach (PITFALL §Performance traps: extra layout passes → jank).
 *
 * @param sections ordered list of sections to render.
 * @param modifier modifier applied to the outer [Column].
 * @param mode [AeroAccordionMode.Multi] (default) or [AeroAccordionMode.Single].
 * @param initiallyExpanded indices open on first composition (uncontrolled only).
 * @param expandedIndex controlled open index for [AeroAccordionMode.Single]; `null` = none open.
 *   Ignored when [onExpandedChange] is `null`.
 * @param expandedIndices controlled open index set for [AeroAccordionMode.Multi].
 *   Ignored when [onExpandedChange] is `null`.
 * @param onExpandedChange when non-null the accordion is controlled; called with the new
 *   expanded set after every toggle. The caller must respond by updating the state passed
 *   via [expandedIndices] / [expandedIndex]. Never called when `null`.
 */
@Composable
public fun AeroAccordion(
    sections: List<AeroAccordionSection>,
    modifier: Modifier = Modifier,
    mode: AeroAccordionMode = AeroAccordionMode.Multi,
    initiallyExpanded: Set<Int> = emptySet(),
    expandedIndex: Int? = null,
    expandedIndices: Set<Int>? = null,
    onExpandedChange: ((Set<Int>) -> Unit)? = null,
) {
    val controlled = onExpandedChange != null

    // Uncontrolled internal state for multi mode — plain Set in mutableStateOf triggers recomposition on assignment
    var internalExpandedSet by remember { mutableStateOf(initiallyExpanded.toSet()) }
    // For single uncontrolled mode we track a single nullable Int
    var internalExpandedSingle by remember {
        mutableStateOf(if (mode == AeroAccordionMode.Single) initiallyExpanded.firstOrNull() else null)
    }

    Column(modifier = modifier) {
        sections.forEachIndexed { index, section ->
            // Derive expanded state from controlled params or internal state
            val expanded: Boolean = if (controlled) {
                when (mode) {
                    AeroAccordionMode.Single -> expandedIndex == index
                    AeroAccordionMode.Multi  -> expandedIndices?.contains(index) == true
                }
            } else {
                when (mode) {
                    AeroAccordionMode.Single -> internalExpandedSingle == index
                    AeroAccordionMode.Multi  -> index in internalExpandedSet
                }
            }

            val onToggle: () -> Unit = {
                if (controlled) {
                    val next: Set<Int> = when (mode) {
                        AeroAccordionMode.Single -> {
                            val next = accordionToggleSingle(expandedIndex, index)
                            if (next == null) emptySet() else setOf(next)
                        }
                        AeroAccordionMode.Multi  -> {
                            val current = expandedIndices ?: emptySet()
                            accordionToggleMulti(current, index)
                        }
                    }
                    onExpandedChange!!(next)
                } else {
                    when (mode) {
                        AeroAccordionMode.Single -> {
                            internalExpandedSingle = accordionToggleSingle(internalExpandedSingle, index)
                        }
                        AeroAccordionMode.Multi  -> {
                            internalExpandedSet = accordionToggleMulti(internalExpandedSet, index)
                        }
                    }
                }
            }

            AccordionSectionRow(
                section = section,
                expanded = expanded,
                onToggle = onToggle,
            )

            // 1dp borderDefault divider between sections (not after the last).
            // F11: padding(horizontal = 8.dp) insets the divider to sit within the rounded
            // section background's straight edge (matches 8.dp cornerRadius of glassPanel).
            if (index < sections.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(1.dp)
                        .background(AeroTheme.colors.borderDefault)
                )
            }
        }
    }
}

/**
 * Private section row — renders header + animated content area.
 * Holds NO state (PITFALL-13): receives [expanded] + [onToggle] from [AeroAccordion].
 */
@Composable
private fun AccordionSectionRow(
    section: AeroAccordionSection,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val caretRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "caretRotation",
    )

    Column {
        // Header row with glass panel surface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(cornerRadius = 8.dp)
                // Clip hover/press indication to the rounded surface so it follows the 8dp corners.
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Optional leading icon
            section.leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AeroTheme.colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Title
            Text(
                text = section.title,
                color = AeroTheme.colors.onSurface,
                style = AeroTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )

            // Caret — rotates 0->90 on expand via graphicsLayer rotationZ
            Icon(
                imageVector = AeroIcons.CaretRight,
                contentDescription = null,
                tint = AeroTheme.colors.onSurface,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = caretRotation },
            )
        }

        // Content area — animateContentSize handles height transition (no height measurement needed)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)),
        ) {
            if (expanded) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    section.content()
                }
            }
        }
    }
}
