package com.mordred.aero.components.layout

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarStateTest {

    @Test
    fun expandedModeTargetsCorrectWidth() {
        assertEquals(240.dp, targetWidthForMode(AeroSidebarMode.Expanded))
    }

    @Test
    fun collapsedModeTargetsCorrectWidth() {
        assertEquals(48.dp, targetWidthForMode(AeroSidebarMode.Collapsed))
    }

    @Test
    fun hiddenModeTargetsZeroWidth() {
        assertEquals(0.dp, targetWidthForMode(AeroSidebarMode.Hidden))
    }
}
