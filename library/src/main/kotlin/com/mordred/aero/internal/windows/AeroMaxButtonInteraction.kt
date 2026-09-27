package com.mordred.aero.internal.windows

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset

/**
 * BTN-01 / D-02 / PITFALLS 6+11 / T-22-19: per-window bridge that turns the maximize
 * button's non-client mouse messages back into ordinary Compose interactions.
 *
 * Once the maximize rect answers HTMAXBUTTON (SNAP-01), Windows delivers the mouse over it
 * as WM_NC* messages Compose never sees. `AeroFrameWndProc` converts those messages into
 * calls to this class's methods — always through `SwingUtilities.invokeLater`, so every
 * method here runs on the EDT and only ever touches [interactionSource] there (the WndProc
 * itself keeps two plain booleans on the toolkit thread). The unchanged `TitleBarButton`
 * rendering reads hover from `collectIsHoveredAsState()` and press from `LocalIndication`
 * on this same source, so parity with minimize/close is structural, not a matching exercise.
 *
 * Every method is idempotent ([hoverEnter] while already entered is a no-op) and
 * non-suspending (`tryEmit` never blocks, T-22-19).
 */
internal class AeroMaxButtonInteraction(
    private val regions: HitTestRegionRegistry,
) {

    /** Fed exclusively on the EDT; `TitleBarButton`'s `hoverable` + `clickable` read it. */
    val interactionSource = MutableInteractionSource()

    /**
     * The placement toggle `AeroTitleBar` installs on the native path (today's maximize
     * `onClick` verbatim); invoked by [release] with `click = true`.
     */
    @Volatile
    var onClick: (() -> Unit)? = null

    private var currentHover: HoverInteraction.Enter? = null
    private var currentPress: PressInteraction.Press? = null

    /** WM_NCMOUSEMOVE at HTMAXBUTTON. */
    fun hoverEnter() {
        if (currentHover != null) return
        val enter = HoverInteraction.Enter()
        currentHover = enter
        interactionSource.tryEmit(enter)
    }

    /** The cursor left the button (another NC hit code) or the non-client area (WM_NCMOUSELEAVE). */
    fun hoverExit() {
        val enter = currentHover ?: return
        currentHover = null
        interactionSource.tryEmit(HoverInteraction.Exit(enter))
    }

    /** WM_NCLBUTTONDOWN / WM_NCLBUTTONDBLCLK at HTMAXBUTTON. */
    fun press() {
        if (currentPress != null) return
        val press = PressInteraction.Press(buttonCenter())
        currentPress = press
        interactionSource.tryEmit(press)
    }

    /**
     * WM_NCLBUTTONUP at HTMAXBUTTON. [click] mirrors the native pressed flag: a stray up
     * without a preceding down is not a click and never invokes [onClick].
     */
    fun release(click: Boolean) {
        val press = currentPress
        if (press != null) {
            currentPress = null
            interactionSource.tryEmit(PressInteraction.Release(press))
        }
        if (click) onClick?.invoke()
    }

    /** Press aborted (cursor left the non-client area mid-press): a Cancel, never a click. */
    fun cancelPress() {
        val press = currentPress ?: return
        currentPress = null
        interactionSource.tryEmit(PressInteraction.Cancel(press))
    }

    /**
     * The indication's press origin: the centre of the live Maximize rect in the button's own
     * local px — the same origin a centred mouse press gives a ripple. Falls back to the
     * button's nominal 46x32dp half-size at 100% DPI; unreachable in practice, because a
     * press can only follow an HTMAXBUTTON answer, which requires the rect to be published.
     */
    private fun buttonCenter(): Offset {
        val rect = regions.snapshot().roles[TitleBarRole.Maximize] ?: return Offset(23f, 16f)
        return Offset((rect.right - rect.left) / 2f, (rect.bottom - rect.top) / 2f)
    }
}

/**
 * BTN-01: window → [AeroMaxButtonInteraction] lookup shared by `AeroTitleBar` (Compose side)
 * and `NativeWindowChromeRegistry` (install-time capture for the frame proc). A
 * [java.util.WeakHashMap] under `synchronized`, no JNA — same discipline as
 * [WindowRegionsDirectory], safe to call on every OS.
 */
internal object MaxButtonDirectory {

    private val interactions = java.util.WeakHashMap<java.awt.Window, AeroMaxButtonInteraction>()

    fun forWindow(window: java.awt.Window): AeroMaxButtonInteraction =
        synchronized(interactions) {
            interactions.getOrPut(window) {
                AeroMaxButtonInteraction(WindowRegionsDirectory.forWindow(window))
            }
        }
}
