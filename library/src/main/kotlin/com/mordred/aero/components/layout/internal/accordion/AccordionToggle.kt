package com.mordred.aero.components.layout.internal.accordion

/**
 * Pure single-mode toggle: returns the new expanded index after clicking [clickedIndex].
 *
 * - If [clickedIndex] == [expandedIndex], the section closes -> returns null.
 * - Otherwise the clicked section opens, replacing the previous one.
 *
 * No Compose imports — this function is unit-testable on plain JVM.
 */
internal fun accordionToggleSingle(expandedIndex: Int?, clickedIndex: Int): Int? =
    if (expandedIndex == clickedIndex) null else clickedIndex

/**
 * Pure multi-mode toggle: returns the new set of expanded indices after clicking [clickedIndex].
 *
 * - If [clickedIndex] is already in [expandedIndices], it is removed (collapsed).
 * - Otherwise [clickedIndex] is added (expanded), keeping all existing expanded indices.
 *
 * No Compose imports — this function is unit-testable on plain JVM.
 */
internal fun accordionToggleMulti(expandedIndices: Set<Int>, clickedIndex: Int): Set<Int> =
    if (clickedIndex in expandedIndices) expandedIndices - clickedIndex else expandedIndices + clickedIndex
