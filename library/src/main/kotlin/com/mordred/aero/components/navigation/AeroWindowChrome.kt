package com.mordred.aero.components.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.mordred.aero.internal.windows.AeroMaxButtonInteraction
import com.mordred.aero.internal.windows.HitTestRegionRegistry
import com.mordred.aero.internal.windows.MaxButtonDirectory
import com.mordred.aero.internal.windows.NativeWindowChromeRegistry
import com.mordred.aero.internal.windows.PxRect
import com.mordred.aero.internal.windows.TitleBarRole
import com.mordred.aero.internal.windows.WindowRegionsDirectory
import com.mordred.aero.internal.windows.isWindowsOs
import kotlin.math.roundToInt

/**
 * API-02 / BTN-02: marks this element as interactive content sitting over the window's
 * title bar, so on the native-chrome path Windows classifies its pixels as client area
 * (`HTCLIENT`) instead of caption (`HTCAPTION`): the element keeps its ordinary Compose
 * pointer input (e.g. `clickable`) instead of starting a window drag.
 *
 * Typical use: an element the app overlays on the `AeroTitleBar` row — e.g. Pinya's
 * "return the queue" button on its detached queue window. The `AeroTitleBar`'s own
 * `leading` slot is already interactive without this modifier.
 *
 * No effect off Windows, and inert on any window where no native subclass is installed
 * (`AeroTitleBar(..., nativeWindowManagement = false)`, or a failed install): nothing
 * reads the published region there. Outside a window composition (`LocalAwtWindow` null,
 * e.g. tests) the receiver is returned unchanged.
 *
 * The element's live bounds are published to the window's hit-test region registry on
 * every layout pass and removed when the element leaves composition, so a hidden or
 * moved element stops claiming pixels immediately.
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun Modifier.markAeroTitleBarInteractive(): Modifier {
    if (!isWindowsOs) return this
    return composed {
        val window = LocalAwtWindow.current ?: return@composed this
        val regions = remember(window) { WindowRegionsDirectory.forWindow(window) }
        val id = remember(regions) { regions.newInteractiveId() }
        DisposableEffect(regions, id) {
            onDispose { regions.publishInteractive(id, null) }
        }
        this.onGloballyPositioned { coordinates ->
            regions.publishInteractive(id, coordinates.boundsInWindow(clipBounds = true).toPxRect())
        }
    }
}

/**
 * API-04 / D-05: the native window wiring of a custom title bar, for an app that draws its
 * own header instead of using [AeroTitleBar]. Call [rememberAeroWindowChrome] inside the
 * window's composition, then mark the layout with the modifier members:
 *
 *  - the draggable caption band(s) get [captionArea] — Windows then owns dragging, Aero Snap
 *    and double-click-maximize over those pixels (`HTCAPTION`);
 *  - the maximize button gets [maximizeButtonArea] — it answers `HTMAXBUTTON`, so hovering
 *    it shows the Windows 11 Snap Layouts flyout, and a native click toggles
 *    [WindowState.placement] `Maximized <-> Floating` (the same toggle `AeroTitleBar` has
 *    always used);
 *  - every clickable element inside a caption band (search fields, buttons — anything that
 *    must receive ordinary Compose pointer input instead of dragging the window) gets
 *    [captionExclude] (`HTCLIENT`). Interactive content left unmarked inside a caption area
 *    drags the window instead of receiving clicks.
 *
 * Several caption areas are supported (e.g. left and right of a centered search field); the
 * maximize area is one rect per window — the last one laid out wins.
 *
 * The button's hover / pressed state is readable from [maximizeHovered] / [maximizePressed]
 * (or by attaching [maximizeInteractionSource] to the app's own `hoverable`/`clickable`
 * chain, which is what `AeroTitleBar` does — native and Compose-driven interactions then
 * feed one source).
 *
 * When [isNative] is `false` — `nativeWindowManagement = false`, any non-Windows OS, or a
 * failed native install — every modifier returns the receiver unchanged and nothing is
 * published: the app keeps its own drag handling (e.g. `WindowDraggableArea`) and resize
 * zones exactly as before the call. Off Windows the function has no effect at all.
 */
@Composable
public fun FrameWindowScope.rememberAeroWindowChrome(
    windowState: WindowState,
    nativeWindowManagement: Boolean = true,
): AeroWindowChromeState {
    val nativeRequested = isWindowsOs && nativeWindowManagement
    var installFailed by remember(window) { mutableStateOf(false) }
    if (!nativeRequested) {
        // The app's own hoverable/clickable drives the source on the inert path.
        val source = remember { MutableInteractionSource() }
        val hovered = source.collectIsHoveredAsState()
        val pressed = source.collectIsPressedAsState()
        return remember(source, hovered, pressed) {
            AeroWindowChromeStateImpl(null, null, isNative = false, source, hovered, pressed)
        }
    }
    val regions = remember(window) { WindowRegionsDirectory.forWindow(window) }
    val maxInteraction = remember(window) { MaxButtonDirectory.forWindow(window) }
    DisposableEffect(window, nativeRequested) {
        val handle = NativeWindowChromeRegistry.acquire(window)
        // A failed install flips the state inert below — the app keeps its own drag handling.
        if (handle == null) installFailed = true
        onDispose { handle?.release() }
    }
    DisposableEffect(maxInteraction, windowState) {
        maxInteraction.onClick = {
            windowState.placement =
                if (windowState.placement == WindowPlacement.Maximized)
                    WindowPlacement.Floating
                else
                    WindowPlacement.Maximized
        }
        onDispose { maxInteraction.onClick = null }
    }
    val source = maxInteraction.interactionSource
    val hovered = source.collectIsHoveredAsState()
    val pressed = source.collectIsPressedAsState()
    return remember(regions, maxInteraction, source, hovered, pressed, installFailed) {
        AeroWindowChromeStateImpl(regions, maxInteraction, isNative = !installFailed, source, hovered, pressed)
    }
}

/**
 * API-04: the state [rememberAeroWindowChrome] returns. All modifier members publish the
 * element's live window-relative bounds on every layout pass and remove them when the
 * element leaves composition; each is inert (returns the receiver unchanged) while
 * [isNative] is false.
 */
@Stable
public interface AeroWindowChromeState {

    /** Whether the native window wiring is live for this window. See [rememberAeroWindowChrome]. */
    public val isNative: Boolean

    /**
     * Marks the element as a draggable caption area: Windows answers `HTCAPTION` over its
     * bounds and owns dragging, Aero Snap and double-click maximize natively. Interactive
     * content inside the area must be marked with [captionExclude]. Several caption areas
     * may be published for one window.
     */
    public fun Modifier.captionArea(): Modifier

    /**
     * Marks the element as clickable content inside a caption area: its pixels stay
     * `HTCLIENT`, so it receives ordinary Compose pointer input instead of starting a
     * window drag.
     */
    public fun Modifier.captionExclude(): Modifier

    /**
     * Marks the element as the window's maximize button: it answers `HTMAXBUTTON` (hovering
     * shows the Windows 11 Snap Layouts flyout), and a native click toggles
     * `windowState.placement` `Maximized <-> Floating`. One rect per window — the last
     * area laid out wins.
     */
    public fun Modifier.maximizeButtonArea(): Modifier

    /**
     * The interaction source the native maximize path feeds its hover / press interactions
     * into. Attach it to the app's own `hoverable` + `clickable` chain (as `AeroTitleBar`
     * does) so native and Compose interactions render through one source; on the inert path
     * it is a plain source the app's own gestures drive.
     */
    public val maximizeInteractionSource: MutableInteractionSource

    /** Whether the cursor is over the maximize area (`true` while hovered). */
    public val maximizeHovered: State<Boolean>

    /** Whether the maximize area is currently pressed. */
    public val maximizePressed: State<Boolean>
}

/** The one region kind the shared publishing modifier below carries. */
private enum class ChromeRegionKind { Caption, Maximize, Exclude }

/**
 * Takes its collaborators as constructor parameters (registry and interaction bridge both
 * nullable, and `isNative` a plain value) so tests can compose it headlessly against a
 * free-standing registry without a window or a native install — the constructor is the seam
 * the headless API-04 tests build on.
 */
internal class AeroWindowChromeStateImpl(
    private val regions: HitTestRegionRegistry?,
    private val maxInteraction: AeroMaxButtonInteraction?,
    override val isNative: Boolean,
    override val maximizeInteractionSource: MutableInteractionSource,
    override val maximizeHovered: State<Boolean>,
    override val maximizePressed: State<Boolean>,
) : AeroWindowChromeState {

    override fun Modifier.captionArea(): Modifier =
        if (!isNative) this else publishChromeRegion(ChromeRegionKind.Caption)

    override fun Modifier.captionExclude(): Modifier =
        if (!isNative) this else publishChromeRegion(ChromeRegionKind.Exclude)

    override fun Modifier.maximizeButtonArea(): Modifier =
        if (!isNative) this else publishChromeRegion(ChromeRegionKind.Maximize)

    /**
     * The one publishing modifier all three members share: allocates the region id once,
     * publishes the live `boundsInWindow()` on placement, removes the region on dispose.
     * Exclude reuses the interactive-id path of [markAeroTitleBarInteractive].
     */
    @OptIn(ExperimentalComposeUiApi::class)
    private fun Modifier.publishChromeRegion(kind: ChromeRegionKind): Modifier = composed {
        val registry = this@AeroWindowChromeStateImpl.regions ?: return@composed this
        when (kind) {
            ChromeRegionKind.Caption -> {
                val id = remember(registry) { registry.newCaptionId() }
                DisposableEffect(registry, id) {
                    onDispose { registry.publishCaption(id, null) }
                }
                this.onGloballyPositioned { coordinates ->
                    registry.publishCaption(id, coordinates.boundsInWindow(clipBounds = true).toPxRect())
                }
            }
            ChromeRegionKind.Maximize -> {
                DisposableEffect(registry) {
                    onDispose { registry.publishRole(TitleBarRole.Maximize, null) }
                }
                this.onGloballyPositioned { coordinates ->
                    registry.publishRole(TitleBarRole.Maximize, coordinates.boundsInWindow(clipBounds = true).toPxRect())
                }
            }
            ChromeRegionKind.Exclude -> {
                val id = remember(registry) { registry.newInteractiveId() }
                DisposableEffect(registry, id) {
                    onDispose { registry.publishInteractive(id, null) }
                }
                this.onGloballyPositioned { coordinates ->
                    registry.publishInteractive(id, coordinates.boundsInWindow(clipBounds = true).toPxRect())
                }
            }
        }
    }
}

private fun Rect.toPxRect(): PxRect =
    PxRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
