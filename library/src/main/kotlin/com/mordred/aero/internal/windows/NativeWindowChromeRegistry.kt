package com.mordred.aero.internal.windows

import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import java.awt.Window
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
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

    private class InstalledChrome(
        val frameProc: AeroFrameWndProc,
        val frameProcPtr: Pointer,
        val previousFrameProc: Pointer,
        val children: MutableList<InstalledChild>,
        var ownerCount: Int,
    )

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
     * (T-22-SC-adjacent: no native call happens at all). If the AWT peer does not exist yet
     * ([Window.isDisplayable] false), install is deferred to a one-shot [HierarchyListener]
     * (`Native.getWindowPointer` needs a displayable AWT peer to return a valid pointer).
     */
    internal fun acquire(window: Window): ChromeHandle? {
        if (!isWindowsOs) return null
        if (!window.isDisplayable) return acquireDeferred(window)
        return installOrReuse(window)
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

        val existing = installed[hwndLong]
        if (existing != null && currentWndProcPointer(hwnd) == existing.frameProcPtr) {
            existing.ownerCount += 1
            chromeTrace("reuse", hwndLong, "ownerCount=${existing.ownerCount}")
            return ChromeHandle { release(hwndLong) }
        }

        Win32Chrome.ensureNativeFrameStyles(hwnd)
        val previousFrameProc = currentWndProcPointer(hwnd)
        val frameProc = AeroFrameWndProc(hwndLong, previousFrameProc)
        val frameProcPtr = CallbackReference.getFunctionPointer(frameProc)
        val chrome = InstalledChrome(
            frameProc = frameProc,
            frameProcPtr = frameProcPtr,
            previousFrameProc = previousFrameProc,
            children = mutableListOf(),
            ownerCount = 1,
        )
        // Strong refs recorded BEFORE the frame's WNDPROC is swapped, so a message dispatched
        // the instant after SetWindowLongPtr always finds a live registry entry (Pitfall 1).
        installed[hwndLong] = chrome
        User32.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, frameProcPtr)
        chromeTrace("install", hwndLong, "frameProc=$frameProcPtr")

        // WIN-03: apply the maintainer's chosen corner/shadow look explicitly (22-06).
        applyCornerPolicy(hwnd, maximized = aeroUser32.IsZoomed(hwnd))

        val enumProc = WinUser.WNDENUMPROC { childHwnd, _ ->
            subclassChild(hwndLong, childHwnd, chrome)
            true
        }
        User32.INSTANCE.EnumChildWindows(hwnd, enumProc, Pointer.NULL)

        return ChromeHandle { release(hwndLong) }
    }

    private fun subclassChild(frameHwndLong: Long, childHwnd: HWND, chrome: InstalledChrome) {
        val childHwndLong = Pointer.nativeValue(childHwnd.pointer)
        val previous = currentWndProcPointer(childHwnd)
        val childProc = AeroChildWndProc(frameHwndLong, childHwndLong, previous)
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

        // Only drop the entry after restoration is attempted and read back (or deliberately
        // skipped because another subclass is now on top) — never before.
        installed.remove(hwndLong)
    }

    /** WM_NCDESTROY has already torn the HWND down; just drop the map entry. */
    internal fun onDestroyed(hwndLong: Long) {
        installed.remove(hwndLong)
    }

    private fun currentWndProcPointer(hwnd: HWND): Pointer =
        Pointer(User32.INSTANCE.GetWindowLongPtr(hwnd, GWLP_WNDPROC).toLong())
}
