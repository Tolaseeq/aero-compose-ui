package com.mordred.aero.components.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * OVL-02: Variant of [AeroAlertDialog]. Drives icon, accent color, and default buttons.
 */
public enum class AeroAlertKind {
    Info,
    Warning,
    Error,
    Question;

    /** Returns the Material Icons.Outlined icon paired with this kind. */
    public val icon: ImageVector
        get() = when (this) {
            Info     -> Icons.Outlined.Info
            Warning  -> Icons.Outlined.Warning
            Error    -> Icons.Outlined.Error
            Question -> Icons.Outlined.HelpOutline
        }

    /**
     * Static accent (Warning is fixed orange because no warning token exists).
     * Other kinds resolve via the @Composable [accentFor] helper in AeroAlertDialog.kt.
     */
    internal val staticAccent: Color?
        get() = when (this) {
            Warning -> Color(0xFFFFA726)
            else    -> null
        }
}
