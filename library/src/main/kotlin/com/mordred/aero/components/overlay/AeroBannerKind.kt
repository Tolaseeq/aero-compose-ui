package com.mordred.aero.components.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/** OVL-06: Variant of [AeroNotificationBanner]. Drives icon + accent color. */
public enum class AeroBannerKind {
    Info,
    Warning,
    Error,
    Success;

    /** Returns the Material Icons.Outlined icon paired with this kind. */
    public val icon: ImageVector
        get() = when (this) {
            Info     -> Icons.Outlined.Info
            Warning  -> Icons.Outlined.Warning
            Error    -> Icons.Outlined.Error
            Success  -> Icons.Outlined.CheckCircle
        }
}
