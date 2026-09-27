package com.mordred.aero.internal.windows

import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import java.awt.Window
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SNAP-01 / SNAP-02 / T-22-01 / T-22-09: HWND-keyed install/acquire/release registry. Holds a
 * strong reference to every installed [AeroFrameWndProc] / [AeroChildWndProc] for as long as
 * it is installed — letting a `Callback` become unreachable while still installed crashes the
 * JVM on the next message dispatched to that HWND — and is idempotent across repeated
 * [acquire] calls for the same live HWND, so a Hot Reload re-run of the installing effect is
 * a safe no-op instead of stacking a second subclass on top.
 */
internal object NativeWindowChromeRegistry {

    private class InstalledChild(
        val childHwnd: Long,
        val proc: AeroChildWndProc,
        val procPtr: Pointer,
    )

    /**
     * D-01: whether the app set its own AWT minimum on the window. Shared between the
     * registry entry (written by the property listener on the EDT) and the frame proc's
     * `WM_GETMINMAXINFO` handler (read on the toolkit thread) — `@Volatile` so the read is
     * always current.
     */
    private class AppMinimumFlag(@Volatile var set: Boolean)

    /** D-01: keeps the entry's flag in sync with the app's own minimum-size writes (EDT). */
    private class MinimumSizeListener(private val flag: AppMinimumFlag) : PropertyChangeListener {
        override fun propertyChange(event: PropertyChangeEvent) {
            flag.set = event.newValue != null
        }
    }

    private class InstalledChrome(
        val window: Window,
        var frameProc: AeroFrameWndProc,
        var frameProcPtr: Pointer,
        var previousFrameProc: Pointer,
        val children: MutableList<InstalledChild>,
        val churnGuard: ChurnGuard,
        val appMinimum: AppMinimumFlag,
        val minimumSizeListener: PropertyChangeListener,
        var ownerCount: Int,
    ) {
        /**
         * Procs (and their native pointers) replaced by a later re-install. They stay strongly
         * referenced until the entry is dropped at WM_NCDESTROY / release: a replaced proc may
         * still sit in whoever evicted us, and a `Callback` becoming unreachable while still in
         * a live chain crashes the JVM (PITFALLS 1+4).
         */
        val retiredProcs = mutableListOf<Any>()
    }

    /**
     * WIN-05 / PITFALLS 4: AWT may re-assert its own WndProc and Skiko may re-create child
     * canvases at any time (activation changes, DnD registration, always-on-top toggles), and
     * a silent eviction throws no exception anywhere. Every shown / moved / resized /
     * window-state event re-verifies the install on the EDT — cheap, idempotent, and the only
     * reliable signal that the subclass is still ours. Registered at install, removed at
     * uninstall.
     */
    private class ChurnGuard(private val window: Window) : ComponentListener, WindowStateListener {

        override fun componentShown(event: ComponentEvent) = verify()
        override fun componentResized(event: ComponentEvent) = verify()
        override fun componentMoved(event: ComponentEvent) = verify()
        override fun componentHidden(event: ComponentEvent) = Unit
        override fun windowStateChanged(event: WindowEvent) = verify()

        private fun verify() = NativeWindowChromeRegistry.verify(window)
    }

    private val installed = ConcurrentHashMap<Long, InstalledChrome>()

    /** A caller-held handle; releasing it more than once is a no-op. */
    internal class ChromeHandle(private val onRelease: () -> Unit) {
        private val released = AtomicBoolean(false)

        internal fun release() {
            if (released.compareAndSet(false, true)) onRelease()
        }
    }

    /**
     * Installs (or reuses) native chrome for [window]'s HWND. Returns `null` off-Windows
     * (T-22-SC-adjacent: no native call happens at all) or when install itself throws
     * (T-22-02: traced as `install-failed`; the caller falls back to its legacy
     * `WindowDraggableArea` path so the window stays draggable). If the AWT peer does not
     * exist yet ([Window.isDisplayable] false), install is deferred to a one-shot
     * [HierarchyListener] (`Native.getWindowPointer` needs a displayable AWT peer to return
     * a valid pointer).
     */
    internal fun acquire(window: Window): ChromeHandle? {
        if (!isWindowsOs) return null
        if (!window.isDisplayable) return acquireDeferred(window)
        return try {
            installOrReuse(window)
        } catch (t: Throwable) {
            // WIN-02: a failed install leaves the Compose resize path active.
            NativeChromeStatus.set(window, false)
            chromeTrace("install-failed", 0L, "${t::class.java.name}: ${t.message}")
            null
        }
    }

    private fun acquireDeferred(window: Window): ChromeHandle {
        var deferredHandle: ChromeHandle? = null
        val listener = object : HierarchyListener {
            override fun hierarchyChanged(event: HierarchyEvent) {
                if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && window.isShowing) {
                    window.removeHierarchyListener(this)
                    deferredHandle = installOrReuse(window)
                }
            }
        }
        window.addHierarchyListener(listener)
        return ChromeHandle {
            window.removeHierarchyListener(listener)
            deferredHandle?.release()
        }
    }

    private fun installOrReuse(window: Window): ChromeHandle {
        val hwndPointer = Native.getWindowPointer(window)
        val hwndLong = Pointer.nativeValue(hwndPointer)
        val hwnd = HWND(hwndPointer)
        val regions = WindowRegionsDirectory.forWindow(window)

        val existing = installed[hwndLong]
        if (existing != null && currentWndProcPointer(hwnd) == existing.frameProcPtr) {
            existing.ownerCount += 1
            chromeTrace("reuse", hwndLong, "ownerCount=${existing.ownerCount}")
            NativeChromeStatus.set(window, true)
            return ChromeHandle { release(hwndLong) }
        }

        val chrome: InstalledChrome
        if (existing != null) {
            // The entry is alive but the frame proc is no longer ours (AWT/Skiko re-subclassed,
            // PITFALLS 4). Re-subclass on top of the CURRENT proc, reusing the entry so its
            // churn guard, children and retired-proc references survive; children re-sync below.
            reinstallFrameProc(hwndLong, hwnd, existing, window)
            chrome = existing
        } else {
            // SNAP-01: the per-window region registry `AeroTitleBar` publishes into; both procs
            // read its snapshot on every WM_NCHITTEST. BTN-01: the per-window maximize-button
            // interaction bridge the frame proc feeds through EDT hops.
            val maxButton = MaxButtonDirectory.forWindow(window)

            Win32Chrome.ensureNativeFrameStyles(hwnd)
            val previousFrameProc = currentWndProcPointer(hwnd)
            val frameProc = AeroFrameWndProc(hwndLong, previousFrameProc, regions, maxButton)
            val frameProcPtr = CallbackReference.getFunctionPointer(frameProc)
            val churnGuard = ChurnGuard(window)
            // D-01 / T-22-24: seed from the app's own current setting and keep following its
            // later writes — the WM_GETMINMAXINFO floor may only raise ptMinTrackSize when
            // the app set no minimum. The library itself never writes a window minimum size.
            val appMinimum = AppMinimumFlag(window.isMinimumSizeSet)
            val minimumSizeListener = MinimumSizeListener(appMinimum)
            chrome = InstalledChrome(
                window = window,
                frameProc = frameProc,
                frameProcPtr = frameProcPtr,
                previousFrameProc = previousFrameProc,
                children = mutableListOf(),
                churnGuard = churnGuard,
                appMinimum = appMinimum,
                minimumSizeListener = minimumSizeListener,
                ownerCount = 0,
            )
            // Strong refs recorded BEFORE the frame's WNDPROC is swapped, so a message dispatched
            // the instant after SetWindowLongPtr always finds a live registry entry (Pitfall 1).
            installed[hwndLong] = chrome
            User32.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, frameProcPtr)
            chromeTrace("install", hwndLong, "frameProc=$frameProcPtr")

            // WIN-03: apply the maintainer's chosen corner/shadow look explicitly (22-06).
            applyCornerPolicy(hwnd, maximized = aeroUser32.IsZoomed(hwnd))

            window.addComponentListener(churnGuard)
            window.addWindowStateListener(churnGuard)
            window.addPropertyChangeListener("minimumSize", minimumSizeListener)
        }
        chrome.ownerCount += 1
        syncChildren(hwndLong, hwnd, chrome, regions)
        // WIN-02: native chrome is live — the Compose resize path stands down for this window.
        NativeChromeStatus.set(window, true)

        return ChromeHandle { release(hwndLong) }
    }

    /**
     * WIN-05 / PITFALLS 4 / T-22-09: re-subclasses the frame ON TOP of the CURRENT proc —
     * never blindly restoring the originally-saved one, which may be stale. Returns true when
     * a reinstall actually happened (current proc already ours → false, no-op).
     */
    private fun reinstallFrameProc(
        hwndLong: Long,
        hwnd: HWND,
        chrome: InstalledChrome,
        window: Window,
    ): Boolean {
        val current = currentWndProcPointer(hwnd)
        if (current == chrome.frameProcPtr) return false
        val frameProc = AeroFrameWndProc(
            hwndLong,
            current,
            WindowRegionsDirectory.forWindow(window),
            MaxButtonDirectory.forWindow(window),
        )
        val frameProcPtr = CallbackReference.getFunctionPointer(frameProc)
        // The replaced proc/pointer retire into the entry — still strongly referenced until
        // WM_NCDESTROY / release, because they may live on in whoever evicted us (PITFALLS 1+4).
        chrome.retiredProcs.add(chrome.frameProc)
        chrome.retiredProcs.add(chrome.frameProcPtr)
        chrome.frameProc = frameProc
        chrome.frameProcPtr = frameProcPtr
        chrome.previousFrameProc = current
        User32.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, frameProcPtr)
        chromeTrace("reinstall", hwndLong, "frameProc=$frameProcPtr")
        return true
    }

    /**
     * PITFALLS 4+16: subclasses every live child that is not currently ours (a newly created
     * canvas, or one whose subclass was evicted / whose HWND value was recycled) and drops
     * entries of children that no longer exist. Returns true when anything changed.
     */
    private fun syncChildren(
        hwndLong: Long,
        hwnd: HWND,
        chrome: InstalledChrome,
        regions: HitTestRegionRegistry,
    ): Boolean {
        var changed = false
        val liveChildren = ArrayList<Long>()
        val enumProc = WinUser.WNDENUMPROC { childHwnd, _ ->
            val childHwndLong = Pointer.nativeValue(childHwnd.pointer)
            liveChildren.add(childHwndLong)
            val ours = chrome.children.any {
                it.childHwnd == childHwndLong && currentWndProcPointer(childHwnd) == it.procPtr
            }
            if (!ours) {
                subclassChild(hwndLong, childHwnd, chrome, regions)
                changed = true
            }
            true
        }
        User32.INSTANCE.EnumChildWindows(hwnd, enumProc, Pointer.NULL)

        val stale = chrome.children.filter { it.childHwnd !in liveChildren }
        if (stale.isNotEmpty()) {
            // Those HWNDs no longer exist, so their procs can never be called again — safe to
            // drop the strong references (PITFALLS 1).
            chrome.children.removeAll(stale)
            changed = true
        }
        return changed
    }

    /**
     * WIN-05 / PITFALLS 4+16 / T-22-09 (EDT only): re-checks a live install against the OS —
     * the frame proc must still be ours and every live child subclassed. Traces
     * `event=reinstall` / `event=child-subclass` when it repairs something, or
     * `event=verify frame=ok` when the install is intact; that trace is the in-process
     * signal a probe reads, because a cross-process `GWLP_WNDPROC` read is meaningless
     * (finding F8).
     */
    internal fun verify(window: Window) {
        if (!isWindowsOs || !window.isDisplayable) return
        try {
            verifyInstalled(window)
        } catch (t: Throwable) {
            chromeTrace("verify-failed", 0L, "${t::class.java.name}: ${t.message}")
        }
    }

    /** The WM_PARENTNOTIFY(WM_CREATE) hop target — verifies through the entry's own window. */
    internal fun verifyByHwnd(hwndLong: Long) {
        val chrome = installed[hwndLong] ?: return
        verify(chrome.window)
    }

    /**
     * D-01 input for the frame proc's WM_GETMINMAXINFO floor: did the app set its own AWT
     * minimum on this window? A missing entry (HWND already gone) reads as "no minimum",
     * which only ever applies the default floor to a dying window — never clobbers an app
     * value.
     */
    internal fun appMinimumSetFor(hwndLong: Long): Boolean =
        installed[hwndLong]?.appMinimum?.set ?: false

    private fun verifyInstalled(window: Window) {
        val hwndPointer = Native.getWindowPointer(window)
        val hwndLong = Pointer.nativeValue(hwndPointer)
        val chrome = installed[hwndLong] ?: return
        val hwnd = HWND(hwndPointer)

        val frameChanged = reinstallFrameProc(hwndLong, hwnd, chrome, window)
        val childrenChanged = syncChildren(hwndLong, hwnd, chrome, WindowRegionsDirectory.forWindow(window))
        if (!frameChanged && !childrenChanged) {
            chromeTrace("verify", hwndLong, "frame=ok children=${chrome.children.size}")
        }
    }

    private fun subclassChild(
        frameHwndLong: Long,
        childHwnd: HWND,
        chrome: InstalledChrome,
        regions: HitTestRegionRegistry,
    ) {
        val childHwndLong = Pointer.nativeValue(childHwnd.pointer)
        val previous = currentWndProcPointer(childHwnd)
        val childProc = AeroChildWndProc(frameHwndLong, childHwndLong, previous, regions)
        val childProcPtr = CallbackReference.getFunctionPointer(childProc)
        // Strong ref recorded before the child's WNDPROC is swapped — same ordering rule as
        // the frame above.
        chrome.children.add(InstalledChild(childHwndLong, childProc, childProcPtr))
        User32.INSTANCE.SetWindowLongPtr(childHwnd, GWLP_WNDPROC, childProcPtr)
        chromeTrace("child-subclass", frameHwndLong, "childHwnd=0x${childHwndLong.toString(16)}")
    }

    private fun release(hwndLong: Long) {
        val chrome = installed[hwndLong] ?: return
        chrome.ownerCount -= 1
        if (chrome.ownerCount > 0) return

        // The churn guard stops watching before the procs come off (PITFALLS 4: listeners
        // registered at install are removed at uninstall).
        chrome.window.removeComponentListener(chrome.churnGuard)
        chrome.window.removeWindowStateListener(chrome.churnGuard)
        chrome.window.removePropertyChangeListener("minimumSize", chrome.minimumSizeListener)

        for (child in chrome.children) {
            val childHwnd = HWND(Pointer(child.childHwnd))
            if (currentWndProcPointer(childHwnd) == child.procPtr) {
                User32.INSTANCE.SetWindowLongPtr(childHwnd, GWLP_WNDPROC, child.proc.previous)
                chromeTrace(
                    "uninstall",
                    hwndLong,
                    "child=0x${child.childHwnd.toString(16)} readBack=${currentWndProcPointer(childHwnd)}",
                )
            } else {
                // Someone subclassed on top of us — leave our proc in place as a pure
                // passthrough rather than clobbering theirs.
                chromeTrace("uninstall-skipped", hwndLong, "child=0x${child.childHwnd.toString(16)}")
            }
        }

        val hwnd = HWND(Pointer(hwndLong))
        if (currentWndProcPointer(hwnd) == chrome.frameProcPtr) {
            User32.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, chrome.previousFrameProc)
            chromeTrace("uninstall", hwndLong, "frame readBack=${currentWndProcPointer(hwnd)}")
        } else {
            chromeTrace("uninstall-skipped", hwndLong, "frame")
        }

        // WIN-02: the native resize path is gone — the Compose resize path may act again.
        NativeChromeStatus.set(chrome.window, false)

        // Only drop the entry after restoration is attempted and read back (or deliberately
        // skipped because another subclass is now on top) — never before.
        installed.remove(hwndLong)
    }

    /** WM_NCDESTROY has already torn the HWND down; just drop the map entry. */
    internal fun onDestroyed(hwndLong: Long) {
        if (installed.remove(hwndLong) != null) {
            // WIN-06: window teardown observability — on a posted WM_CLOSE the HWND is gone
            // before the composable's onDispose can run release(), so without this line the
            // close path leaves no trace at all.
            chromeTrace("ncdestroy", hwndLong, "entry dropped")
        }
    }

    private fun currentWndProcPointer(hwnd: HWND): Pointer =
        Pointer(User32.INSTANCE.GetWindowLongPtr(hwnd, GWLP_WNDPROC).toLong())
}
