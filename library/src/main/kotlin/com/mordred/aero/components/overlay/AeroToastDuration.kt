package com.mordred.aero.components.overlay

/**
 * Toast display duration. Mirrors Material3 `SnackbarDuration`.
 *  - [Short]: 4 seconds — typical "Saved!" feedback
 *  - [Long]: 10 seconds — longer messages
 *  - [Indefinite]: stays until programmatically dismissed
 */
public enum class AeroToastDuration(public val millis: Long) {
    Short(4_000L),
    Long(10_000L),
    Indefinite(kotlin.Long.MAX_VALUE)
}

/** Result returned by [AeroToastHostState.showToast]'s suspended call. */
public enum class AeroToastResult {
    /** The toast finished its duration and was dismissed automatically. */
    Dismissed,

    /** The user clicked the toast's action button. */
    ActionPerformed,

    /** The toast was evicted because the stack was full. */
    Evicted
}

/**
 * Optional action button shown on a toast.
 *
 * @param label button label.
 * @param onClick invoked on click; the toast is dismissed and the showToast call
 *   resumes with [AeroToastResult.ActionPerformed].
 */
public data class AeroToastAction(
    public val label: String,
    public val onClick: () -> Unit
)
