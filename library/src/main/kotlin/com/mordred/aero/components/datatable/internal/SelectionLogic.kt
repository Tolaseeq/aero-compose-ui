package com.mordred.aero.components.datatable.internal

/** Replace selection with a single key. */
internal fun applySingleClick(key: Any): Set<Any> = setOf(key)

/** Toggle one key in/out of the current selection (Ctrl-click). */
internal fun applyCtrlClick(selected: Set<Any>, key: Any): Set<Any> =
    if (key in selected) selected - key else selected + key

/**
 * Inclusive contiguous range between [anchorKey] and [clickedKey] in [displayedKeys]
 * (the keys in current displayed/sorted order). anchor null -> just {clickedKey}.
 * Returns empty if either key is absent from displayedKeys.
 */
internal fun computeShiftRange(
    displayedKeys: List<Any>,
    anchorKey: Any?,
    clickedKey: Any,
): Set<Any> {
    if (anchorKey == null) return setOf(clickedKey)
    val a = displayedKeys.indexOf(anchorKey)
    val b = displayedKeys.indexOf(clickedKey)
    if (a < 0 || b < 0) return emptySet()
    val lo = minOf(a, b)
    val hi = maxOf(a, b)
    return displayedKeys.subList(lo, hi + 1).toSet()
}
