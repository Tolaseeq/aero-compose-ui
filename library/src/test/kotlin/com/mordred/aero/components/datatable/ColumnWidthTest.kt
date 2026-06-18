package com.mordred.aero.components.datatable

import androidx.compose.ui.unit.dp
import com.mordred.aero.components.datatable.internal.resolveColumnWidths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ColumnWidthTest {

    @Test
    fun fixedRespectsMinWidth() {
        val result = resolveColumnWidths(
            widths = listOf(AeroColumnWidth.Fixed(20.dp)),
            minWidthsDp = listOf(40f),
            totalWidthPx = 200f,
            pxPerDp = 1f,
        )
        assertEquals(40f, result[0].dpValue, 0.01f)
    }

    @Test
    fun fixedAboveMinUnchanged() {
        val result = resolveColumnWidths(
            widths = listOf(AeroColumnWidth.Fixed(100.dp)),
            minWidthsDp = listOf(40f),
            totalWidthPx = 200f,
            pxPerDp = 1f,
        )
        assertEquals(100f, result[0].dpValue, 0.01f)
    }

    @Test
    fun weightSplitsRemaining() {
        val result = resolveColumnWidths(
            widths = listOf(AeroColumnWidth.Weight(1f), AeroColumnWidth.Weight(1f)),
            minWidthsDp = listOf(40f, 40f),
            totalWidthPx = 200f,
            pxPerDp = 1f,
        )
        assertEquals(100f, result[0].dpValue, 0.01f)
        assertEquals(100f, result[1].dpValue, 0.01f)
    }

    @Test
    fun weightClampsToMinWhenNoSpace() {
        // Fixed takes all 200px, weight column gets 0 remaining -> clamps to minWidth 40f
        val result = resolveColumnWidths(
            widths = listOf(AeroColumnWidth.Fixed(200.dp), AeroColumnWidth.Weight(1f)),
            minWidthsDp = listOf(40f, 40f),
            totalWidthPx = 200f,
            pxPerDp = 1f,
        )
        assertEquals(200f, result[0].dpValue, 0.01f)
        assertEquals(40f, result[1].dpValue, 0.01f)
    }

    @Test
    fun mixedFixedAndWeight() {
        // Fixed(50dp) + two Weight(1f) columns, total=250px, pxPerDp=1f
        // remaining = 250 - 50 = 200px; each weight gets 100px = 100dp
        val result = resolveColumnWidths(
            widths = listOf(
                AeroColumnWidth.Fixed(50.dp),
                AeroColumnWidth.Weight(1f),
                AeroColumnWidth.Weight(1f),
            ),
            minWidthsDp = listOf(40f, 40f, 40f),
            totalWidthPx = 250f,
            pxPerDp = 1f,
        )
        assertEquals(50f, result[0].dpValue, 0.01f)
        assertEquals(100f, result[1].dpValue, 0.01f)
        assertEquals(100f, result[2].dpValue, 0.01f)
    }

    @Test
    fun tableTypesReachable() {
        val col = AeroTableColumn<String>(
            header = "h",
            width = AeroColumnWidth.Fixed(10.dp),
            cell = {},
        )
        assertNotNull(col)
        val weight = AeroColumnWidth.Weight(1f)
        assertNotNull(weight)
    }
}
