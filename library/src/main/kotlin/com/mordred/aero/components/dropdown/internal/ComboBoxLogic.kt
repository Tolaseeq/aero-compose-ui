package com.mordred.aero.components.dropdown.internal

/**
 * Pure post-select text rule for AeroComboBox.
 *
 * - [clearOnSelect] == false -> returns [opt] (field shows the chosen label).
 * - [clearOnSelect] == true  -> returns "" (field clears after selection).
 *
 * No Compose imports — unit-testable on plain JVM.
 */
internal fun textAfterSelect(opt: String, clearOnSelect: Boolean): String =
    if (clearOnSelect) "" else opt

/**
 * Pure auto-open decision for the AeroComboBox suggestions popup.
 *
 * - [justSelected] suppresses exactly one auto-open pass triggered by a selection's
 *   own text write, so the popup stays closed right after a click.
 * - Otherwise the popup opens whenever the field is [focused] and at least one option
 *   matches the current [text].
 *
 * No Compose imports — unit-testable on plain JVM.
 */
internal fun shouldAutoOpen(
    focused: Boolean,
    text: String,
    options: List<String>,
    justSelected: Boolean,
): Boolean {
    if (justSelected) return false
    return focused && options.any { it.contains(text, ignoreCase = true) }
}
