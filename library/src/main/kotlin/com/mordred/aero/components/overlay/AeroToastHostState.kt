package com.mordred.aero.components.overlay

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Internal payload representing one live toast. Visible to [AeroToastHost] only. */
public data class AeroToastData internal constructor(
    public val id: Long,
    public val message: String,
    public val duration: AeroToastDuration,
    public val action: AeroToastAction?,
    internal val onDismissed: (AeroToastResult) -> Unit
)

/**
 * State holder for [AeroToastHost]. Mirrors Material3's `SnackbarHostState` but with
 * a STACK queue: multiple toasts are visible simultaneously (max 5).
 *
 * Place a single instance at your app root, then trigger toasts from any coroutine:
 * ```
 * val state = remember { AeroToastHostState() }
 * AeroToastHost(state)
 * scope.launch { state.showToast("Saved") }
 * ```
 *
 * Stack behavior: when full (5 entries), adding a new toast evicts the oldest with
 * [AeroToastResult.Evicted].
 */
public class AeroToastHostState {

    /** Live stack of toasts. Read by [AeroToastHost] composable. */
    internal val toasts: SnapshotStateList<AeroToastData> = mutableStateListOf()

    private var nextId: Long = 0L

    public companion object {
        /** Maximum simultaneous toasts. Older are evicted when this is exceeded. */
        public const val MAX_STACK_SIZE: Int = 5
    }

    /**
     * Shows a toast and suspends until it dismisses (auto, action click, or eviction).
     *
     * @param message body text shown in the toast.
     * @param duration display duration; [AeroToastDuration.Indefinite] stays until manually dismissed.
     * @param action optional [AeroToastAction] button shown on the toast.
     * @return the [AeroToastResult] describing how the toast went away.
     */
    public suspend fun showToast(
        message: String,
        duration: AeroToastDuration = AeroToastDuration.Short,
        action: AeroToastAction? = null
    ): AeroToastResult = suspendCancellableCoroutine { cont ->
        val data = AeroToastData(
            id = nextId++,
            message = message,
            duration = duration,
            action = action,
            onDismissed = { result -> if (cont.isActive) cont.resume(result) }
        )
        // If stack full, evict oldest first
        while (toasts.size >= MAX_STACK_SIZE) {
            val evicted = toasts.removeAt(0)
            evicted.onDismissed(AeroToastResult.Evicted)
        }
        toasts.add(data)
    }

    /** Manually dismiss a specific toast by id (used by AeroToastHost when timer fires or action clicked). */
    internal fun dismiss(id: Long, result: AeroToastResult) {
        val target = toasts.firstOrNull { it.id == id } ?: return
        if (toasts.remove(target)) {
            target.onDismissed(result)
        }
    }
}
