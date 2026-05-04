package com.mordred.aero.components.layout.internal.stepper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Check
import com.mordred.aero.theme.AeroTheme

/**
 * Internal horizontal step indicator for AeroStepperWizard (Phase 10).
 *
 * **Locked horizontal-only** (per 07-CONTEXT.md). Vertical orientation deferred to v2.x if a
 * real consumer appears.
 *
 * **Visual states** (per ROADMAP success criterion #5):
 * - **Current** (`i == currentStep`): 24dp filled circle in `colors.primary`; step number
 *   `(i+1)` in `colors.onPrimary`.
 * - **Completed** (`i < currentStep`): 24dp filled circle in `colors.primary.copy(alpha = 0.6f)`;
 *   `Icon(AeroIcons.Check)` at 12dp tinted `colors.onPrimary`.
 * - **Upcoming** (`i > currentStep`): 24dp outlined circle, 1dp stroke in `colors.labelText`
 *   (chosen over `colors.borderDefault` for AeroDark contrast — `borderDefault` at 25% alpha on
 *   the AeroDark background reads as nearly invisible; `labelText` is fully opaque). Step
 *   number `(i+1)` in `colors.labelText`.
 *
 * **Connector lines:** 2dp stroke between dots. Color is `colors.primary` for the completed-side
 * (between dot `i` and `i+1` when `i < currentStep`), else `colors.borderDefault` for the
 * upcoming-side.
 *
 * **AeroDark contrast** is verified via Phase7ScratchSection's three-theme switch.
 *
 * **Typography note:** Plan referenced `typography.caption`; the actual `AeroTypography` exposes
 * `label` (11sp Bold) which is the closest visual equivalent and is what Plan-01 also substituted.
 *
 * @param currentStep 0-based; 0 = first step is current.
 * @param totalSteps must be >= 1.
 * @param onStepClick null = non-interactive (default for AeroStepperWizard).
 */
@Composable
internal fun AeroStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    onStepClick: ((Int) -> Unit)? = null,
) {
    require(totalSteps >= 1) { "AeroStepIndicator requires totalSteps >= 1; got $totalSteps" }
    val colors = AeroTheme.colors

    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        for (i in 0 until totalSteps) {
            StepDot(
                index = i,
                currentStep = currentStep,
                onClick = if (onStepClick != null) ({ onStepClick(i) }) else null,
            )
            if (i < totalSteps - 1) {
                // Connector line; color depends on whether dot `i` is on the completed side.
                val connectorColor = if (i < currentStep) colors.primary else colors.borderDefault
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(connectorColor),
                )
            }
        }
    }
}

@Composable
private fun StepDot(
    index: Int,
    currentStep: Int,
    onClick: (() -> Unit)?,
) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    val state = when {
        index < currentStep  -> StepState.Completed
        index == currentStep -> StepState.Current
        else                 -> StepState.Upcoming
    }

    val baseModifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .let { if (onClick != null) it.clickable { onClick() } else it }

    when (state) {
        StepState.Current -> Box(
            modifier = baseModifier.background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (index + 1).toString(),
                color = colors.onPrimary,
                style = typography.label.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        StepState.Completed -> Box(
            modifier = baseModifier.background(colors.primary.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AeroIcons.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
        StepState.Upcoming -> Box(
            modifier = baseModifier
                .border(width = 1.dp, color = colors.labelText, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (index + 1).toString(),
                color = colors.labelText,
                style = typography.label,
            )
        }
    }
}

private enum class StepState { Current, Completed, Upcoming }
