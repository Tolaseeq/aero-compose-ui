package com.mordred.aero.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * Search input field with a leading search icon and a conditional clear button.
 *
 * The search icon (🔍) is rendered in a separate Box attached to the left side of AeroTextField.
 * The trailing clear button (×) appears only when [value] is not empty.
 *
 * Note (Phase 2): The left icon box uses a half-rounded shape (round on left, square on right)
 * while AeroTextField uses a fully-rounded shape. The visual seam is acceptable for Phase 2 —
 * INP-05 only requires "search icon and clear button visible only when text present". A unified
 * composite field is a Phase 3 enhancement.
 *
 * @param value Current search text.
 * @param onValueChange Callback invoked when text changes (also called with "" on clear).
 * @param modifier Modifier applied to the outer Row.
 * @param enabled Whether the field is interactive.
 * @param placeholder Placeholder text shown when [value] is empty.
 * @param onSearch Optional callback invoked with the current value (e.g. on Enter).
 */
@Composable
public fun AeroSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Search...",
    onSearch: (String) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier
    ) {
        val iconShape = RoundedCornerShape(
            topStart = 4.dp, bottomStart = 4.dp,
            topEnd = 0.dp, bottomEnd = 0.dp
        )
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 28.dp)
                .background(AeroTheme.colors.cardBackground, iconShape)
                .border(1.dp, AeroTheme.colors.borderDefault, iconShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🔍", style = AeroTheme.typography.bodyMedium)
        }
        AeroTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = placeholder,
            trailingIcon = if (value.isNotEmpty()) {
                { ClearButton(enabled = enabled, onClick = { onValueChange("") }) }
            } else null
        )
    }
}

@Composable
private fun ClearButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("×", color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyLarge)
    }
}
