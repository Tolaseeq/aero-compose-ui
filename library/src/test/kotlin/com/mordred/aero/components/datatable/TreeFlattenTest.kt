package com.mordred.aero.components.datatable

import com.mordred.aero.components.datatable.internal.flattenTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TreeFlattenTest {

    private data class N(val id: Int, val kids: List<N> = emptyList())

    private val childrenFn: (N) -> List<N> = { it.kids }
    private val isExpandableFn: (N) -> Boolean = { it.kids.isNotEmpty() }
    private val keyFn: (N) -> Any = { it.id }

    @Test
    fun noExpansionGivesRootsOnly() {
        val root1 = N(1, listOf(N(11), N(12)))
        val root2 = N(2, listOf(N(21)))
        val result = flattenTree(listOf(root1, root2), emptySet(), childrenFn, isExpandableFn, keyFn)
        assertEquals(2, result.size)
        assertEquals(0, result[0].depth)
        assertEquals(0, result[1].depth)
        assertEquals(1, result[0].key)
        assertEquals(2, result[1].key)
    }

    @Test
    fun expandOneRootIncludesItsChildren() {
        val root1 = N(1, listOf(N(11), N(12)))
        val root2 = N(2, listOf(N(21)))
        val result = flattenTree(listOf(root1, root2), setOf(1), childrenFn, isExpandableFn, keyFn)
        // root1(d0), kid11(d1), kid12(d1), root2(d0)
        assertEquals(4, result.size)
        assertEquals(1, result[0].key)
        assertEquals(0, result[0].depth)
        assertEquals(11, result[1].key)
        assertEquals(1, result[1].depth)
        assertEquals(12, result[2].key)
        assertEquals(1, result[2].depth)
        assertEquals(2, result[3].key)
        assertEquals(0, result[3].depth)
    }

    @Test
    fun depthTwoNesting() {
        // root -> child -> grandchild; expand both
        val grandchild = N(111)
        val child = N(11, listOf(grandchild))
        val root = N(1, listOf(child))
        val result = flattenTree(listOf(root), setOf(1, 11), childrenFn, isExpandableFn, keyFn)
        // root(d0), child(d1), grandchild(d2)
        assertEquals(3, result.size)
        assertEquals(0, result[0].depth)
        assertEquals(1, result[1].depth)
        assertEquals(2, result[2].depth)
        assertEquals(111, result[2].key)
    }

    @Test
    fun collapsedNodeHidesChildren() {
        // root -> child -> grandchild; expand root but NOT child
        val grandchild = N(111)
        val child = N(11, listOf(grandchild))
        val root = N(1, listOf(child))
        val result = flattenTree(listOf(root), setOf(1), childrenFn, isExpandableFn, keyFn)
        // root(d0), child(d1) — grandchild absent
        assertEquals(2, result.size)
        assertEquals(1, result[0].key)
        assertEquals(11, result[1].key)
        assertEquals(1, result[1].depth)
        // grandchild must not be present
        assertFalse(result.any { it.key == 111 })
    }

    @Test
    fun emptyChildrenNodeStillAppears() {
        val leaf = N(5)  // no kids, isExpandable = false
        val result = flattenTree(listOf(leaf), emptySet(), childrenFn, isExpandableFn, keyFn)
        assertEquals(1, result.size)
        assertEquals(5, result[0].key)
        assertFalse(result[0].isExpandable)
    }

    @Test
    fun isExpandedFlagSetCorrectly() {
        val root1 = N(1, listOf(N(11)))
        val root2 = N(2, listOf(N(21)))
        val result = flattenTree(listOf(root1, root2), setOf(1), childrenFn, isExpandableFn, keyFn)
        // root1 is expanded, root2 is not
        assertTrue(result.first { it.key == 1 }.isExpanded)
        assertFalse(result.first { it.key == 2 }.isExpanded)
    }
}
