package com.mordred.aero.components.layout

import com.mordred.aero.components.layout.internal.wizard.isLastStep
import com.mordred.aero.components.layout.internal.wizard.nextStepIndex
import com.mordred.aero.components.layout.internal.wizard.prevStepIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WizardStepTest {

    // nextStepIndex — advance when valid
    @Test
    fun nextAdvancesWhenValid() {
        assertEquals(1, nextStepIndex(current = 0, lastIndex = 3, valid = true))
    }

    // nextStepIndex — stay on current when invalid (Next blocked)
    @Test
    fun nextBlockedWhenInvalid() {
        assertEquals(0, nextStepIndex(current = 0, lastIndex = 3, valid = false))
    }

    // nextStepIndex — already on last step, valid=true does not overflow
    @Test
    fun nextDoesNotOverflowPastLastIndex() {
        assertEquals(3, nextStepIndex(current = 3, lastIndex = 3, valid = true))
    }

    // nextStepIndex — mid-step advance
    @Test
    fun nextAdvancesMidStep() {
        assertEquals(2, nextStepIndex(current = 1, lastIndex = 3, valid = true))
    }

    // nextStepIndex — last step, invalid — stays at last
    @Test
    fun nextBlockedAtLastStepWhenInvalid() {
        assertEquals(3, nextStepIndex(current = 3, lastIndex = 3, valid = false))
    }

    // prevStepIndex — goes back one step
    @Test
    fun prevGoesBack() {
        assertEquals(1, prevStepIndex(current = 2))
    }

    // prevStepIndex — at first step, stays at 0
    @Test
    fun prevAtFirstStayAtZero() {
        assertEquals(0, prevStepIndex(current = 0))
    }

    // isLastStep — current == lastIndex returns true
    @Test
    fun isLastStepTrueWhenOnLast() {
        assertTrue(isLastStep(current = 3, lastIndex = 3))
    }

    // isLastStep — current < lastIndex returns false
    @Test
    fun isLastStepFalseWhenNotLast() {
        assertFalse(isLastStep(current = 2, lastIndex = 3))
    }

    // isLastStep — current == 0 with lastIndex == 0 (single-step wizard)
    @Test
    fun isLastStepTrueForSingleStepWizard() {
        assertTrue(isLastStep(current = 0, lastIndex = 0))
    }
}
