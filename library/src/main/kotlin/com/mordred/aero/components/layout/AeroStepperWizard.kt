package com.mordred.aero.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.components.layout.internal.stepper.AeroStepIndicator
import com.mordred.aero.components.layout.internal.wizard.isLastStep
import com.mordred.aero.components.layout.internal.wizard.nextStepIndex
import com.mordred.aero.components.layout.internal.wizard.prevStepIndex

/**
 * A single step in an [AeroStepperWizard].
 *
 * @param label short title shown in the [AeroStepIndicator] row (e.g. "Account info").
 * @param content composable content for this step.
 * @param onValidate commit gate called ONLY in the Next/Finish [onClick] lambda — never in the
 *   composable body (PITFALL-12). Returns `true` if the wizard should advance; `false` to keep
 *   the user on this step. Defaults to always-valid `{ true }`.
 * @param canProceed live enabled-state of the Next button driven by the caller (e.g. set to
 *   `true` only when required fields are filled). This is the **live** signal; [onValidate] is
 *   the **commit** gate. Defaults to `true`.
 */
public data class AeroWizardStep(
    public val label: String,
    public val content: @Composable () -> Unit,
    public val onValidate: () -> Boolean = { true },
    public val canProceed: Boolean = true,
)

/**
 * LAYO-08, LAYO-09: A linear step wizard rendering the Phase 7 [AeroStepIndicator] on top,
 * the current step's content in the middle, and Back / Next / Finish buttons at the bottom.
 *
 * ## Ownership model — hybrid (uncontrolled default / controlled)
 * **Uncontrolled (default):** the wizard manages the current step internally, seeded by
 * [initialStep]. Use this mode when the caller does not need to drive the step from outside.
 *
 * **Controlled:** when both [onStepChange] AND [currentStep] are supplied the wizard becomes
 * **controlled** — [onStepChange] is called on every navigation event and the wizard renders
 * the step indicated by [currentStep]. Do NOT collapse to one branch; both modes are
 * intentional (same hybrid pattern used by [AeroAccordion]).
 *
 * ## Validate gate (PITFALL-12)
 * [AeroWizardStep.onValidate] is called **exactly once** and **only** inside the Next/Finish
 * `onClick` lambda. It is never called in the composable body, in `enabled`, or in any
 * `derivedStateOf`. The Next button's `enabled` state uses [AeroWizardStep.canProceed] only.
 *
 * ## Back state preservation (PITFALL-12 corollary / Pitfall 5)
 * All step composables remain composed at all times — only the current step is shown by
 * constraining inactive steps to `Modifier.size(0.dp)`. This preserves all `remember` /
 * `rememberSaveable` state so navigating Back and returning to a step restores its content.
 *
 * ## Surface
 * The wizard is **surface-less** — it applies no background or glass effect. The caller is
 * responsible for wrapping it in a glass surface (same convention as [AeroStepIndicator]).
 *
 * ## Deferred
 * Non-linear branching is out of scope for v2.0 (STEP-BR-01).
 *
 * @param steps ordered list of wizard steps (must not be empty).
 * @param onFinish invoked when the user clicks Finish on the last step and [AeroWizardStep.onValidate] passes.
 * @param modifier outer [Modifier].
 * @param initialStep 0-based index of the step to show first (uncontrolled mode only).
 * @param currentStep controlled step index; non-null + [onStepChange] present = controlled mode.
 * @param onStepChange callback for step changes; makes the wizard controlled when provided.
 * @param backLabel text on the Back button.
 * @param nextLabel text on the Next button.
 * @param finishLabel text on the Finish button (shown instead of Next on the last step).
 */
@Composable
public fun AeroStepperWizard(
    steps: List<AeroWizardStep>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 0,
    currentStep: Int? = null,
    onStepChange: ((Int) -> Unit)? = null,
    backLabel: String = "Back",
    nextLabel: String = "Next",
    finishLabel: String = "Finish",
) {
    require(steps.isNotEmpty()) { "AeroStepperWizard requires at least one step" }

    // Uncontrolled internal state — only used when the wizard is NOT controlled.
    var internalStep by remember { mutableStateOf(initialStep) }

    // The effective step index — controlled: caller owns currentStep; uncontrolled: internalStep.
    val controlled = onStepChange != null
    val currentStepInt: Int = if (controlled) (currentStep ?: 0) else internalStep

    // Helper that dispatches a navigation event to the right owner.
    val setStep: (Int) -> Unit = { i ->
        if (controlled) {
            onStepChange!!(i)
        } else {
            internalStep = i
        }
    }

    val lastIndex = steps.lastIndex
    val last = isLastStep(currentStepInt, lastIndex)

    Column(modifier = modifier.padding(16.dp)) {
        // Horizontal step indicator — Phase 7 primitive (0-based, surface-less).
        AeroStepIndicator(
            currentStep = currentStepInt,
            totalSteps = steps.size,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        // Step content area — ALL steps remain composed to preserve remember/rememberSaveable
        // state on Back navigation (PITFALL-12 corollary / Pitfall 5).
        // Active step fills its width; inactive steps collapse to 0dp (not removed from composition).
        steps.forEachIndexed { index, step ->
            Box(
                modifier = if (index == currentStepInt) {
                    Modifier.fillMaxWidth().weight(1f, fill = false)
                } else {
                    Modifier.size(0.dp)
                },
            ) {
                step.content()
            }
        }

        // Navigation button row: Back (outlined) | spacer | Next/Finish (filled)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            AeroOutlinedButton(
                text = backLabel,
                onClick = { setStep(prevStepIndex(currentStepInt)) },
                enabled = currentStepInt > 0,
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Next/Finish button — enabled from canProceed (live caller-driven signal).
            // onValidate is called ONLY here (PITFALL-12): never in the composable body.
            AeroButton(
                text = if (last) finishLabel else nextLabel,
                enabled = steps[currentStepInt].canProceed,
                onClick = {
                    val step = steps[currentStepInt]
                    if (step.onValidate()) {              // PITFALL-12: called ONLY here
                        if (last) onFinish()
                        else setStep(nextStepIndex(currentStepInt, lastIndex, valid = true))
                    }
                },
            )
        }
    }
}
