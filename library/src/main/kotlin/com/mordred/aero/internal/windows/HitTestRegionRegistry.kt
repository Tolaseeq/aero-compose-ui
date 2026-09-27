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
 * BTN-02 / SNAP-02: the semantic region role Windows must distinguish at hit-test time —
 * the maximize button, reported as HTMAXBUTTON so Windows offers Snap Layouts. Caption
 * areas and clickable elements are id-keyed maps instead (`captions` / `interactive`),
 * because a window can have several of each.
 */
internal enum class TitleBarRole { Maximize }

/**
 * SNAP-01 / PITFALLS 6: an immutable, copy-on-write view of every published title-bar region.
 * The WndProc reads one of these per `WM_NCHITTEST` — a plain volatile read, never a lock,
 * never a Compose state object (PITFALLS 6: touching Compose state from the AWT toolkit
 * thread deadlocks or corrupts). Worst case during an active resize is a one-frame-late
 * boundary, never a torn read (T-22-19).
 *
 * [captions] (API-04) holds the id-keyed draggable caption areas a custom title bar marks
 * through `AeroWindowChromeState.captionArea()` — an app may have several (e.g. left and
 * right of a search field); every entry classifies exactly like the Caption role.
 */
internal data class HitTestSnapshot(
    val roles: Map<TitleBarRole, PxRect>,
    val interactive: Map<Long, PxRect>,
    val captions: Map<Long, PxRect> = emptyMap(),
) {
    companion object {
        val EMPTY = HitTestSnapshot(emptyMap(), emptyMap(), emptyMap())
    }
}

/**
 * SNAP-01 / BTN-02: per-window region store. Compose publishes through [publishRole] /
 * [publishInteractive] / [publishCaption]; every publish builds a NEW [HitTestSnapshot] and
 * swaps it into the [AtomicReference] (copy-on-write — a null rect removes the entry, so a
 * disposed title bar stops claiming pixels). The native side only calls [snapshot].
 */
internal class HitTestRegionRegistry {

    private val current = AtomicReference(HitTestSnapshot.EMPTY)

    // Starts at 1: id 0 was the historical reserved leading-slot id and stays unused.
    private val nextInteractiveId = AtomicLong(1L)

    /** PITFALLS 6: a plain volatile read, safe on the native WndProc thread. */
    fun snapshot(): HitTestSnapshot = current.get()

    /** Publishes (or, with a null [rect], removes) the rect for a title-bar [role]. */
    fun publishRole(role: TitleBarRole, rect: PxRect?) {
        current.updateAndGet { snap ->
            HitTestSnapshot(
                roles = if (rect == null) snap.roles - role else snap.roles + (role to rect),
                interactive = snap.interactive,
                captions = snap.captions,
            )
        }
    }

    /** Publishes (or, with a null [rect], removes) an interactive sub-region by [id]. */
    fun publishInteractive(id: Long, rect: PxRect?) {
        current.updateAndGet { snap ->
            HitTestSnapshot(
                roles = snap.roles,
                interactive = if (rect == null) snap.interactive - id else snap.interactive + (id to rect),
                captions = snap.captions,
            )
        }
    }

    /**
     * API-04: publishes (or, with a null [rect], removes) a draggable caption area by [id].
     * Copy-on-write like the other publishers, so the WndProc's snapshot read stays lock-free.
     */
    fun publishCaption(id: Long, rect: PxRect?) {
        current.updateAndGet { snap ->
            HitTestSnapshot(
                roles = snap.roles,
                interactive = snap.interactive,
                captions = if (rect == null) snap.captions - id else snap.captions + (id to rect),
            )
        }
    }

    /** Allocates an interactive-region id (0 stays unused — the historical leading id). */
    fun newInteractiveId(): Long = nextInteractiveId.getAndIncrement()

    /**
     * API-04: allocates a caption-area id for `captionArea()` usage. Shares the counter with
     * [newInteractiveId] — the two maps are independent, but a single sequence keeps every
     * allocated id distinct per registry.
     */
    fun newCaptionId(): Long = nextInteractiveId.getAndIncrement()
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
