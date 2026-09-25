package com.mordred.showcase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowStateListener
import kotlin.math.round
import kotlinx.coroutines.flow.collect

/**
 * Whether machine-readable window-state reporting is enabled for this launch, from
 * `-Daero.windowState=true`. Read once; a normal launch pays nothing for this file.
 */
private val windowStateReportingEnabled: Boolean by lazy {
    System.getProperty("aero.windowState") == "true"
}

private fun round1(value: Float): String {
    val rounded = round(value * 10) / 10
    return if (rounded == rounded.toLong().toFloat()) "${rounded.toLong()}.0" else rounded.toString()
}

private fun round1(value: Double): String = round1(value.toFloat())

/** Combines the four [WindowState] properties this reporter watches into one snapshot key. */
private data class WindowSnapshotKey(
    val placement: WindowPlacement,
    val minimized: Boolean,
    val size: DpSize,
    val position: WindowPosition
)

/**
 * Prints one `AERO_WINDOW_STATE` line describing [windowState] and [window]'s current AWT state,
 * in a fixed key order, then flushes stdout so a launcher tailing the process log sees it right
 * away. Called from both the Compose-side [snapshotFlow] observer and the AWT-side listeners in
 * [WindowStateReporter], so it always reflects whichever side changed.
 */
private fun emitWindowState(windowState: WindowState, window: Window, label: String) {
    val placement = windowState.placement.toString()
    val minimized = windowState.isMinimized
    val extendedState = if (window is java.awt.Frame) window.extendedState else -1
    val sizeDp = "${round1(windowState.size.width.value)}x${round1(windowState.size.height.value)}"
    val posDp = when (val position = windowState.position) {
        is WindowPosition.Absolute -> "${round1(position.x.value)},${round1(position.y.value)}"
        else -> "platform"
    }
    val bounds = window.bounds
    val awtBounds = "${bounds.x},${bounds.y},${bounds.width},${bounds.height}"
    val insets = window.insets
    val insetsStr = "${insets.top},${insets.left},${insets.bottom},${insets.right}"
    val scale = window.graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0
    val minSizeSet = window.isMinimumSizeSet
    val minSize = window.minimumSize
    val minSizeStr = "${minSize.width}x${minSize.height}"
    val resizable = if (window is java.awt.Frame) window.isResizable else false
    val jvmVendor = System.getProperty("java.vendor")?.replace(' ', '_')
    val jvm = "${System.getProperty("java.version")}/$jvmVendor"

    println(
        "AERO_WINDOW_STATE label=$label placement=$placement minimized=$minimized " +
            "awtExtendedState=$extendedState sizeDp=$sizeDp posDp=$posDp awtBounds=$awtBounds " +
            "insets=$insetsStr scale=${round1(scale)} minSizeSet=$minSizeSet minSize=$minSizeStr " +
            "resizable=$resizable jvm=$jvm"
    )
    System.out.flush()
}

/**
 * Prints one `AERO_EVENT name=<name> <detail>` line when window-state reporting is enabled
 * (`-Daero.windowState=true`); a no-op otherwise. Verification plans use this for close-request /
 * click evidence that has no corresponding [WindowState] change to observe.
 */
public fun reportShowcaseEvent(name: String, detail: String = "") {
    if (!windowStateReportingEnabled) return
    println("AERO_EVENT name=$name $detail")
    System.out.flush()
}

/**
 * Prints a machine-readable `AERO_WINDOW_STATE` line every time [windowState] or the underlying
 * AWT window changes, gated by `-Daero.windowState=true`. Composes nothing when reporting is
 * disabled, so a normal launch is unchanged.
 *
 * Reports from two independent sources: Compose's own [snapshotFlow] over [windowState] (the
 * Compose-side view), and AWT `ComponentListener`/`WindowStateListener` callbacks on the frame
 * window (the OS-side view). The two can desync when the OS changes the window without
 * [WindowState] following — comparing both sides' lines is how later verification plans detect
 * that desync.
 */
@Composable
public fun FrameWindowScope.WindowStateReporter(windowState: WindowState, label: String) {
    if (!windowStateReportingEnabled) return

    LaunchedEffect(Unit) {
        snapshotFlow {
            WindowSnapshotKey(
                placement = windowState.placement,
                minimized = windowState.isMinimized,
                size = windowState.size,
                position = windowState.position
            )
        }.collect {
            emitWindowState(windowState, window, label)
        }
    }

    DisposableEffect(window) {
        val componentListener = object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent) = emitWindowState(windowState, window, label)
            override fun componentResized(e: ComponentEvent) = emitWindowState(windowState, window, label)
            override fun componentShown(e: ComponentEvent) = emitWindowState(windowState, window, label)
        }
        val windowStateListener = WindowStateListener { emitWindowState(windowState, window, label) }
        window.addComponentListener(componentListener)
        window.addWindowStateListener(windowStateListener)
        onDispose {
            window.removeComponentListener(componentListener)
            window.removeWindowStateListener(windowStateListener)
        }
    }
}
