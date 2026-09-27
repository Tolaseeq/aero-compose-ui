package com.mordred.aero.internal.windows

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * SNAP-01 / SNAP-02 / BTN-02 / PITFALLS 6+19: the immutable-snapshot bridge that carries
 * `AeroTitleBar`'s live layout regions from the Compose EDT to the native WndProc thread.
 * Pure JVM (`java.util.concurrent` only — no JNA, no Compose), so it is unit-testable
 * headlessly and safe to touch from any thread, the same discipline as
 * `PanelDistribution.kt` / `WndProcSupport.kt`.
 */

/**
 * BTN-02 / PITFALLS 19: the semantic regions of `AeroTitleBar`'s row that Windows must
 * distinguish at hit-test time — the draggable caption, and the three caption buttons whose
 * pixels must NOT become draggable (minimize/close keep ordinary Compose clicks; maximize is
 * reported as HTMAXBUTTON so Windows offers Snap Layouts).
 */
internal enum class TitleBarRole { Caption, Minimize, Maximize, Close }

/**
 * PITFALLS 19: reserved interactive-region id for `AeroTitleBar`'s `leading` slot, so the
 * slot never has to allocate an id through [HitTestRegionRegistry.newInteractiveId].
 */
internal const val LEADING_INTERACTIVE_ID: Long = 0L

/**
 * SNAP-01 / PITFALLS 6: an immutable, copy-on-write view of every published title-bar region.
 * The WndProc reads one of these per `WM_NCHITTEST` — a plain volatile read, never a lock,
 * never a Compose state object (PITFALLS 6: touching Compose state from the AWT toolkit
 * thread deadlocks or corrupts). Worst case during an active resize is a one-frame-late
 * boundary, never a torn read (T-22-19).
 */
internal data class HitTestSnapshot(
    val roles: Map<TitleBarRole, PxRect>,
    val interactive: Map<Long, PxRect>,
) {
    companion object {
        val EMPTY = HitTestSnapshot(emptyMap(), emptyMap())
    }
}

/**
 * SNAP-01 / BTN-02: per-window region store. Compose publishes through [publishRole] /
 * [publishInteractive]; every publish builds a NEW [HitTestSnapshot] and swaps it into the
 * [AtomicReference] (copy-on-write — a null rect removes the entry, so a disposed title bar
 * stops claiming pixels). The native side only calls [snapshot].
 */
internal class HitTestRegionRegistry {

    private val current = AtomicReference(HitTestSnapshot.EMPTY)
    private val nextInteractiveId = AtomicLong(LEADING_INTERACTIVE_ID + 1)

    /** PITFALLS 6: a plain volatile read, safe on the native WndProc thread. */
    fun snapshot(): HitTestSnapshot = current.get()

    /** Publishes (or, with a null [rect], removes) the rect for a title-bar [role]. */
    fun publishRole(role: TitleBarRole, rect: PxRect?) {
        current.updateAndGet { snap ->
            HitTestSnapshot(
                roles = if (rect == null) snap.roles - role else snap.roles + (role to rect),
                interactive = snap.interactive,
            )
        }
    }

    /** Publishes (or, with a null [rect], removes) an interactive sub-region by [id]. */
    fun publishInteractive(id: Long, rect: PxRect?) {
        current.updateAndGet { snap ->
            HitTestSnapshot(
                roles = snap.roles,
                interactive = if (rect == null) snap.interactive - id else snap.interactive + (id to rect),
            )
        }
    }

    /** Allocates an interactive-region id; [LEADING_INTERACTIVE_ID] is reserved and never returned. */
    fun newInteractiveId(): Long = nextInteractiveId.getAndIncrement()
}

/**
 * SNAP-01: window → [HitTestRegionRegistry] lookup shared by `AeroTitleBar` (publisher) and
 * `NativeWindowChromeRegistry` (install-time capture for the procs). Backed by a
 * [java.util.WeakHashMap] under `synchronized` so a closed window's registry is collectible;
 * touches no JNA class, so it is safe to call on every OS.
 */
internal object WindowRegionsDirectory {

    private val registries = java.util.WeakHashMap<java.awt.Window, HitTestRegionRegistry>()

    fun forWindow(window: java.awt.Window): HitTestRegionRegistry =
        synchronized(registries) {
            registries.getOrPut(window) { HitTestRegionRegistry() }
        }
}
