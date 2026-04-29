package com.mordred.aero.components.overlay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Info
import com.mordred.aero.icons.`internal`.Warning
import com.mordred.aero.icons.`internal`.XCircle
import com.mordred.aero.icons.`internal`.Question

/**
 * OVL-02: Variant of [AeroAlertDialog]. Drives icon, accent color, and default buttons.
 */
public enum class AeroAlertKind {
    Info,
    Warning,
    Error,
    Question;

    /** Returns the AeroIcons Phosphor icon paired with this kind. */
    public val icon: ImageVector
        get() = when (this) {
            Info     -> AeroIcons.Info
            Warning  -> AeroIcons.Warning
            Error    -> AeroIcons.XCircle
            Question -> AeroIcons.Question
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
