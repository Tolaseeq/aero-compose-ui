package com.mordred.aero.internal.windows

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.awt.Window
import java.util.WeakHashMap

/**
 * WIN-02: the per-window "native chrome active" flag for the Compose side. The registry
 * writes it on the EDT (true after a successful install, false after uninstall or install
 * failure); `AeroResizeHandles` reads it so its Compose drag zones stand down wherever
 * Windows owns resizing — exactly one resize path per platform.
 *
 * Pure Compose runtime + AWT, no JNA: reading the state never triggers a native load, and
 * off Windows (or after a failed install) the registry never writes it, so it stays its
 * default `false` and `AeroResizeHandles` keeps today's behavior there (API-01 / D-01).
 */
internal object NativeChromeStatus {

    private val states = WeakHashMap<Window, MutableState<Boolean>>()

    private fun stateFor(window: Window): MutableState<Boolean> =
        synchronized(states) {
            states.getOrPut(window) { mutableStateOf(false) }
        }

    /** The Compose-readable per-window flag; `false` until the registry says otherwise. */
    fun activeState(window: Window): State<Boolean> = stateFor(window)

    /** Registry-only write (EDT): flips the flag as the native install comes and goes. */
    internal fun set(window: Window, active: Boolean) {
        stateFor(window).value = active
    }
}
