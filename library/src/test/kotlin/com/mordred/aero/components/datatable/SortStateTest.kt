package com.mordred.aero.components.datatable

import com.mordred.aero.components.datatable.internal.SortState
import com.mordred.aero.components.datatable.internal.nextSortState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SortStateTest {

    @Test
    fun firstClickGivesAsc() {
        val result = nextSortState(SortState(), 0)
        assertEquals(SortDirection.Asc, result.direction)
        assertEquals(0, result.columnKey)
    }

    @Test
    fun sameColumnCyclesAscDescNone() {
        val after1 = nextSortState(SortState(), 0)
        assertEquals(SortDirection.Asc, after1.direction)

        val after2 = nextSortState(after1, 0)
        assertEquals(SortDirection.Desc, after2.direction)

        val after3 = nextSortState(after2, 0)
        assertEquals(SortDirection.None, after3.direction)
        assertNull(after3.columnKey)
    }

    @Test
    fun noneClickGoesBackToAsc() {
        val base = SortState(columnKey = null, direction = SortDirection.None)
        val result = nextSortState(base, 0)
        assertEquals(SortDirection.Asc, result.direction)
        assertEquals(0, result.columnKey)
    }

    @Test
    fun differentColumnResetsToAsc() {
        val base = SortState(columnKey = 0, direction = SortDirection.Desc)
        val result = nextSortState(base, 1)
        assertEquals(SortState(columnKey = 1, direction = SortDirection.Asc), result)
    }

    @Test
    fun onlyOneColumnActive() {
        val base = SortState(columnKey = 0, direction = SortDirection.Asc)
        val result = nextSortState(base, 1)
        assertEquals(1, result.columnKey)
        assertEquals(SortDirection.Asc, result.direction)
    }
}
