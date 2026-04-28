package com.mordred.aero.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassSurface

/**
 * File path picker with an inline "Обзор" button that opens a native [java.awt.FileDialog].
 *
 * The text field is read-only; the selected path is written back via [onPathChange].
 * The "Обзор" button is rendered using [glassSurface] to avoid a dependency on AeroOutlinedButton
 * (Plan 02-01), which is compiled in the same Wave 1 and may not be available at link time.
 *
 * @param path Current file path string.
 * @param onPathChange Callback invoked with the absolute path selected by the user, or
 *   the current value if the dialog is dismissed.
 * @param modifier Modifier applied to the outer Row.
 * @param enabled Whether the picker is interactive.
 * @param mode [java.awt.FileDialog] mode — [java.awt.FileDialog.LOAD] (default) or [java.awt.FileDialog.SAVE].
 * @param dialogTitle Title shown in the native file dialog.
 * @param placeholder Placeholder shown in the path field when empty.
 */
@Composable
public fun AeroFilePicker(
    path: String,
    onPathChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    mode: Int = java.awt.FileDialog.LOAD,
    dialogTitle: String = "Select file",
    placeholder: String = "Path..."
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.alpha(if (enabled) 1f else 0.4f)
    ) {
        AeroTextField(
            value = path,
            onValueChange = onPathChange,
            modifier = Modifier.weight(1f),
            readOnly = true,
            placeholder = placeholder,
            enabled = enabled
        )
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .glassSurface(cornerRadius = 4.dp)
                .clickable(enabled = enabled) {
                    openFileDialog(dialogTitle, mode)?.let(onPathChange)
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Обзор",
                color = AeroTheme.colors.onSurface,
                style = AeroTheme.typography.bodyLarge
            )
        }
    }
}

private fun openFileDialog(title: String, mode: Int): String? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, mode)
    dialog.isVisible = true
    val name = dialog.file ?: return null
    val dir = dialog.directory ?: ""
    return dir + name
}
