package com.mordred.aero.components.overlay

import androidx.compose.ui.graphics.vector.ImageVector
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Info
import com.mordred.aero.icons.`internal`.Warning
import com.mordred.aero.icons.`internal`.XCircle
import com.mordred.aero.icons.`internal`.CheckCircle

/** OVL-06: Variant of [AeroNotificationBanner]. Drives icon + accent color. */
public enum class AeroBannerKind {
    Info,
    Warning,
    Error,
    Success;

    /** Returns the AeroIcons Phosphor icon paired with this kind. */
    public val icon: ImageVector
        get() = when (this) {
            Info     -> AeroIcons.Info
            Warning  -> AeroIcons.Warning
            Error    -> AeroIcons.XCircle
            Success  -> AeroIcons.CheckCircle
        }
}
