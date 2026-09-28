package com.mordred.aero.internal.windows

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import javax.swing.SwingUtilities

/**
 * WIN-05 / PITFALLS 3: the single, authoritative declaration of the messages
 * [AeroFrameWndProc] owns. Every message NOT in this set is forwarded verbatim through
 * `CallWindowProc(previous, ...)` — AWT's own WndProc still runs for focus, IME, painting,
 * drag-and-drop and accessibility, and its extendedState → `WindowState` sync pipeline stays
 * undisturbed (conflict C4: "syncs automatically", settled 22-02).
 *
 * Ownership mode per message — the ONLY branches allowed to return something other than
 * `CallWindowProc`'s result are WM_NCCALCSIZE (wParam 1), WM_NCHITTEST and the NC button
 * messages at HTMAXBUTTON:
 *  - WM_NCCALCSIZE   — wParam 0: forwarded untouched; wParam 1: modified-then-swallowed
 *                      (`rgrc[0]` rewritten in place, return 0; WIN-01).
 *  - WM_NCHITTEST    — answered from the live region snapshot; HTCLIENT forwards so AWT's
 *                      own answer stands (SNAP-01).
 *  - WM_NCMOUSEMOVE / WM_NCMOUSELEAVE — observed-then-forwarded (max-button hover bridge,
 *                      BTN-01; forwarding keeps `DefWindowProc`'s Snap Layouts flyout path).
 *  - WM_NCLBUTTONDOWN / WM_NCLBUTTONUP — swallowed ONLY at HTMAXBUTTON (return 0, no
 *                      classic-button paint, no double-fire); every other hit code forwards
 *                      unchanged (BTN-01).
 *  - WM_NCLBUTTONDBLCLK — three-way disposition (SNAP-04): HTCAPTION answers from
 *                      `DefWindowProc` (the OS default performs SC_MAXIMIZE/SC_RESTORE from
 *                      the style bits), HTMAXBUTTON keeps the button-down swallow, every
 *                      other hit code forwards unchanged.
 *  - WM_SYSCOMMAND   — SC_KEYMENU only is owned (the system menu is displayed through
 *                      hand-declared GetSystemMenu/TrackPopupMenu, then the chosen command
 *                      is re-dispatched as a forwarded WM_SYSCOMMAND, SNAP-05); every other
 *                      command forwards verbatim (the proven Alt+F4 / snap paths, C2).
 *  - WM_STYLECHANGING — modified-then-forwarded (required styles written into `styleNew`
 *                      in place before forwarding, WIN-03 / PITFALLS 4).
 *  - WM_SIZE         — observed-then-forwarded (corner policy re-applied AFTER
 *                      `CallWindowProc`; WIN-03 / PITFALLS 24).
 *  - WM_GETMINMAXINFO — forwarded first, then ptMinTrackSize raised to the D-01 floor when
 *                      the app set no AWT minimum (the forwarded result is returned verbatim;
 *                      WIN-02 / D-01 / T-22-24: the library never writes window.minimumSize).
 *  - WM_PARENTNOTIFY — observed-then-forwarded (a child-create notification hops to the EDT
 *                      so the registry subclasses the new child; PITFALLS 4/16).
 *  - WM_NCDESTROY    — observed-then-forwarded (registry entry dropped after forwarding,
 *                      PITFALLS 5).
 */
internal val OWNED_FRAME_MESSAGES: Set<Int> = setOf(
    WM_NCCALCSIZE,
    WM_NCHITTEST,
    WM_NCMOUSEMOVE,
    WM_NCMOUSELEAVE,
    WM_NCLBUTTONDOWN,
    WM_NCLBUTTONUP,
    WM_NCLBUTTONDBLCLK,
    WM_SYSCOMMAND,
    WM_STYLECHANGING,
    WM_SIZE,
    WM_GETMINMAXINFO,
    WM_PARENTNOTIFY,
    WM_NCDESTROY,
)

/**
 * PITFALLS 3: the child proc owns WM_NCHITTEST only — every other message the child receives
 * is forwarded verbatim through `CallWindowProc(previous, ...)`.
 */
internal val OWNED_CHILD_MESSAGES: Set<Int> = setOf(WM_NCHITTEST)

/**
 * WIN-02 / PITFALLS 13: the edge/corner resize band for [hwnd] — recomputed on EVERY
 * `WM_NCHITTEST` because a window may have just crossed monitors: the system sizing frame
 * via `GetSystemMetricsForDpi` at the window's current `GetDpiForWindow` DPI, never thinner
 * than CMP's own 8 dp undecorated resizer at the same scale (so no Compose-side resize zone
 * can receive a press the native band is supposed to own).
 */
private fun resizeBandForHwnd(hwnd: HWND): Int {
    val dpi = aeroUser32.GetDpiForWindow(hwnd)
    return resizeBandPx(
        aeroUser32.GetSystemMetricsForDpi(SM_CXSIZEFRAME, dpi),
        aeroUser32.GetSystemMetricsForDpi(SM_CXPADDEDBORDER, dpi),
        dpi / 96f,
    )
}

/**
 * SNAP-01 / SNAP-02 / BTN-01: the frame's `WindowProc`. Owns exactly
 * [OWNED_FRAME_MESSAGES]; every other message is `CallWindowProc(previous, ...)` verbatim —
 * AWT's own WndProc still needs to run for focus, IME, painting and `WindowState` sync.
 */
internal class AeroFrameWndProc(
    val hwnd: Long,
    val previous: Pointer,
    val regions: HitTestRegionRegistry,
    val maxButton: AeroMaxButtonInteraction,
) : WinUser.WindowProc {

    // BTN-01 / T-22-19: the native-side hover/pressed mirror, touched only on the toolkit
    // thread. Compose-visible state changes cross threads exclusively through
    // SwingUtilities.invokeLater (PITFALLS 6) — never from inside the callback.
    private var ncHovered = false
    private var ncPressed = false

    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        LRESULT(
            dispatchSafely(
                handle = { handle(hwnd, uMsg, wParam, lParam) },
                fallback = { callPrevious(hwnd, uMsg, wParam, lParam).toLong() },
            ),
        )

    private fun handle(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long =
        // PITFALLS 3: nothing outside OWNED_FRAME_MESSAGES is ever touched — the set is the
        // authoritative audit of ownership, so a message cannot gain handling here without
        // also being declared in it (and every owned branch below returns `CallWindowProc`'s
        // result unless it is one of the three sanctioned swallow/answer cases).
        if (uMsg !in OWNED_FRAME_MESSAGES) {
            callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        } else {
            when (uMsg) {
                WM_NCCALCSIZE -> handleNcCalcSize(hwnd, wParam, lParam)

                WM_NCHITTEST -> handleNcHitTest(hwnd, wParam, lParam)

                WM_NCMOUSEMOVE -> handleNcMouseMove(hwnd, uMsg, wParam, lParam)

                WM_NCMOUSELEAVE -> handleNcMouseLeave(hwnd, uMsg, wParam, lParam)

                WM_NCLBUTTONDOWN -> handleNcButtonDown(hwnd, uMsg, wParam, lParam)

                WM_NCLBUTTONDBLCLK -> handleNcDoubleClick(hwnd, uMsg, wParam, lParam)

                WM_NCLBUTTONUP -> handleNcButtonUp(hwnd, uMsg, wParam, lParam)

                WM_SYSCOMMAND -> handleSysCommand(hwnd, uMsg, wParam, lParam)

                WM_STYLECHANGING -> handleStyleChanging(hwnd, uMsg, wParam, lParam)

                WM_SIZE -> handleSize(hwnd, uMsg, wParam, lParam)

                WM_GETMINMAXINFO -> handleGetMinMaxInfo(hwnd, uMsg, wParam, lParam)

                WM_PARENTNOTIFY -> handleParentNotify(hwnd, uMsg, wParam, lParam)

                WM_NCDESTROY -> {
                    val result = callPrevious(hwnd, uMsg, wParam, lParam).toLong()
                    NativeWindowChromeRegistry.onDestroyed(this.hwnd)
                    result
                }

                else -> callPrevious(hwnd, uMsg, wParam, lParam).toLong()
            }
        }

    /**
     * WIN-05 / PITFALLS 4+16: AWT/Skiko may create a NEW child HWND at any time (canvas
     * re-creation); until subclassed it answers `WM_NCHITTEST` with HTCLIENT everywhere and
     * the child→frame bounce is broken. The message itself is forwarded untouched; a
     * child-create notification hops to the EDT (PITFALLS 6 — never install from inside the
     * callback) where the registry re-enumerates and subclasses what is missing.
     */
    private fun handleParentNotify(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        val result = callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        if ((wParam.toLong() and 0xFFFF).toInt() == WM_CREATE) {
            SwingUtilities.invokeLater { NativeWindowChromeRegistry.verifyByHwnd(this.hwnd) }
        }
        return result
    }

    /**
     * BTN-01 / PITFALLS 12 / T-22-21: over HTMAXBUTTON the hover is bridged to Compose, and
     * non-client leave tracking (one-shot by design) is re-armed on EVERY such message or the
     * hover highlight sticks after the cursor leaves. The message itself is always forwarded
     * afterwards so `DefWindowProc` keeps driving the Snap Layouts flyout (22-04 early gate).
     */
    private fun handleNcMouseMove(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        if (wParam.toInt() == HTMAXBUTTON) {
            aeroUser32.TrackMouseEvent(TRACKMOUSEEVENT(hwnd, TME_LEAVE or TME_NONCLIENT))
            if (!ncHovered) {
                ncHovered = true
                hop { maxButton.hoverEnter() }
            }
        } else if (ncHovered || ncPressed) {
            ncHovered = false
            ncPressed = false
            hop {
                maxButton.hoverExit()
                maxButton.cancelPress()
            }
        }
        return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }

    /** BTN-01 / PITFALLS 12: leaving the non-client area clears hover and cancels any press. */
    private fun handleNcMouseLeave(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        if (ncHovered || ncPressed) {
            ncHovered = false
            ncPressed = false
            hop {
                maxButton.hoverExit()
                maxButton.cancelPress()
            }
        }
        return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }

    /**
     * BTN-01 / WIN-05 / ARCHITECTURE Anti-Pattern 4: button-down at HTMAXBUTTON is swallowed
     * (return 0, never forwarded) so `DefWindowProc` neither paints a classic caption button
     * over the Compose one nor double-fires the toggle. Every other hit code forwards
     * unchanged — caption drag keeps working natively.
     */
    private fun handleNcButtonDown(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        if (wParam.toInt() != HTMAXBUTTON) return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        return swallowMaxButtonPress()
    }

    /**
     * SNAP-04: the caption double-click is routed by [ncDoubleClickDisposition]. At HTCAPTION the
     * message is handed to `User32.INSTANCE.DefWindowProc` and its result returned verbatim —
     * the OS default performs SC_MAXIMIZE when the window is floating and SC_RESTORE when zoomed,
     * driven by the WS_CAPTION/WS_MAXIMIZEBOX style bits the frame keeps set. Forwarding into
     * AWT's proc instead is where the double-click died (22-SESSION S04-DBLCLICK-MAX FAIL both
     * JVMs). At HTMAXBUTTON the swallow matches the single button-down (no classic-button paint,
     * no double-fire); every other hit code keeps forwarding unchanged.
     *
     * Fallback decision (recorded, NOT implemented): if the re-verification session's real
     * double-click still fails AND the chrome trace shows zero WM_NCLBUTTONDBLCLK arrivals at
     * this proc (a CS_DBLCLKS class-style question), the named fix is synthesizing the
     * double-click from two downs within `GetDoubleClickTime` on the toolkit thread — decided
     * by trace evidence in that session, not here.
     */
    private fun handleNcDoubleClick(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long =
        when (ncDoubleClickDisposition(wParam.toInt())) {
            NcDoubleClickDisposition.DEF_WINDOW_PROC ->
                User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam).toLong()
            NcDoubleClickDisposition.SWALLOW -> swallowMaxButtonPress()
            NcDoubleClickDisposition.FORWARD_AWT ->
                callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        }

    /** BTN-01: the HTMAXBUTTON swallow shared by button-down and double-click. */
    private fun swallowMaxButtonPress(): Long {
        ncPressed = true
        chromeTrace("max-button", this.hwnd, "down")
        hop { maxButton.press() }
        return 0L
    }

    /**
     * BTN-01 / WIN-05: button-up at HTMAXBUTTON is swallowed the same way; the click toggles
     * `windowState.placement` on the EDT through [AeroMaxButtonInteraction.release] — the
     * same code path as today's Compose `onClick`, never a `WM_SYSCOMMAND`.
     */
    private fun handleNcButtonUp(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        if (wParam.toInt() != HTMAXBUTTON) return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        val wasPressed = ncPressed
        ncPressed = false
        chromeTrace("max-button", this.hwnd, "up click=$wasPressed")
        hop { maxButton.release(click = wasPressed) }
        return 0L
    }

    /**
     * SNAP-05 / C2: WM_SYSCOMMAND(SC_KEYMENU) is the Alt+Space system-menu trigger. DefWindowProc
     * answers it by showing the window's system menu; forwarding it into AWT's proc instead
     * swallows the trigger (C2's settled finding), so this branch owns it: fetch the menu with
     * the hand-declared `GetSystemMenu(hwnd, false)` and display it at the client origin (under
     * the title bar's left edge, DefWindowProc's own placement) via `TrackPopupMenu` with
     * TPM_RETURNCMD. The nested menu loop is DefWindowProc's own behavior for this syscommand;
     * the whole branch runs inside [dispatchSafely], whose fallback is the passthrough call.
     *
     * The chosen command (non-zero return) is re-dispatched with `SendMessage(hwnd,
     * WM_SYSCOMMAND, ...)` — it re-enters this proc, discriminates to FORWARD, and flows
     * through AWT to `DefWindowProc` exactly like the proven Alt+F4 / SC_CLOSE path. Every
     * non-SC_KEYMENU syscommand forwards verbatim, so Alt+F4, the flyout's commands and all
     * snap syscommands keep byte-identical behavior. Fallback decision (recorded, NOT
     * implemented): if the real Alt+Space chord in the re-verification session shows SC_KEYMENU
     * never arrives at this proc, owning WM_SYSKEYDOWN is the named next fix — decided by
     * chrome-trace evidence there, not here.
     */
    private fun handleSysCommand(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        if (systemMenuDisposition(wParam.toInt()) != SysCommandDisposition.MENU) {
            return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        }
        chromeTrace("sysmenu", this.hwnd, "open")
        val menu = aeroUser32.GetSystemMenu(hwnd, false)
        if (menu != null) {
            val origin = POINT(0, 0)
            aeroUser32.ClientToScreen(hwnd, origin)
            val selected = aeroUser32.TrackPopupMenu(
                menu,
                TPM_RETURNCMD or TPM_RIGHTBUTTON or TPM_LEFTALIGN or TPM_TOPALIGN,
                origin.x,
                origin.y,
                0,
                hwnd,
                null,
            )
            if (selected != 0) {
                User32.INSTANCE.SendMessage(hwnd, WM_SYSCOMMAND, WPARAM(selected.toLong()), LPARAM(0))
            }
        }
        return 0L
    }

    /** BTN-01 / PITFALLS 6 / T-22-19: the ONLY crossing into Compose-land — always the EDT. */
    private fun hop(action: () -> Unit) {
        SwingUtilities.invokeLater { action() }
    }

    /**
     * WIN-03 / PITFALLS 24: corner rounding follows maximize/restore. The message is
     * forwarded to `CallWindowProc(previous)` FIRST — AWT owns the `WindowState` sync
     * pipeline (C4) and nothing here may replace it — then the corner policy is re-applied
     * for the new state (square while maximized, the maintainer's chosen look when floating).
     *
     * C4 (settled 22-02, subclass live): `WindowState.placement` / `isMinimized` follow
     * OS-originated SC_MAXIMIZE / SC_RESTORE / SC_MINIMIZE and non-Compose moves/resizes
     * through AWT's own extendedState pipeline with zero native code, so this branch adds NO
     * explicit placement push — a push could only risk a feedback loop (T-22-23).
     */
    private fun handleSize(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        val result = callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        when (wParam.toInt()) {
            SIZE_MAXIMIZED -> applyCornerPolicy(hwnd, maximized = true)
            SIZE_RESTORED -> applyCornerPolicy(hwnd, maximized = false)
        }
        return result
    }

    /**
     * D-01 / WIN-02 / T-22-03: the resize floor. The message is forwarded FIRST — AWT owns
     * the structure's content, and when the app set its own AWT minimum that value is
     * already in ptMinTrackSize and must stand. Only when the app set no minimum
     * (`isMinimumSizeSet` false, tracked by the registry) is ptMinTrackSize raised to the
     * 320×240 dp default in physical px at the window's current DPI. Every other field
     * (ptReserved, ptMaxSize, ptMaxPosition, ptMaxTrackSize) keeps exactly what the
     * previous proc filled, and the forwarded result is returned verbatim. The library
     * itself never writes a window minimum size (T-22-24).
     *
     * MINMAXINFO native layout — five POINTs; ptMinTrackSize.x at byte offset 24, .y at 28
     * (not shipped by jna-platform 5.19.1, so the offsets are used directly).
     */
    private fun handleGetMinMaxInfo(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        val result = callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        val info = Pointer(lParam.toLong())
        val awtWidth = info.getInt(24)
        val awtHeight = info.getInt(28)
        val appMinimumSet = NativeWindowChromeRegistry.appMinimumSetFor(this.hwnd)
        val dpi = aeroUser32.GetDpiForWindow(hwnd)
        val (width, height) = resolveMinimumTrackSizePx(appMinimumSet, awtWidth, awtHeight, dpi / 96f)
        if (width != awtWidth || height != awtHeight) {
            info.setInt(24, width)
            info.setInt(28, height)
            chromeTrace("mintrack", this.hwnd, "appMinimumSet=$appMinimumSet dpi=$dpi floor=${width}x$height")
        }
        return result
    }

    /**
     * WIN-03: keeps [REQUIRED_FRAME_STYLES] set even if AWT rewrites `GWL_STYLE` later
     * (activation changes, DnD registration, always-on-top toggles — Pitfall 4). `STYLESTRUCT`
     * is `{ DWORD styleOld; DWORD styleNew }`; writing `styleNew` in place before forwarding
     * changes the style Windows actually applies. Only `GWL_STYLE` is touched —
     * `GWL_EXSTYLE` changes pass through unread.
     */
    private fun handleStyleChanging(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        if (wParam.toInt() == GWL_STYLE) {
            val styleStruct = Pointer(lParam.toLong())
            val styleNew = styleStruct.getInt(4)
            val kept = withRequiredFrameStyles(styleNew)
            if (kept != styleNew) {
                styleStruct.setInt(4, kept)
                chromeTrace(
                    "style-kept",
                    this.hwnd,
                    "styleNew=0x${styleNew.toString(16)} kept=0x${kept.toString(16)}",
                )
            }
        }
        return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }

    /**
     * SNAP-01 / BTN-02 / PITFALLS 6: converts the screen point to client coordinates INSIDE
     * the handler (window position never enters the math), then classifies it against the
     * live region snapshot `AeroTitleBar` publishes — a plain volatile read of an immutable
     * [HitTestSnapshot], never Compose state from this native thread.
     */
    private fun handleNcHitTest(hwnd: HWND, wParam: WPARAM, lParam: LPARAM): Long {
        val (screenX, screenY) = decodeScreenPoint(lParam.toLong())
        val point = POINT(screenX, screenY)
        aeroUser32.ScreenToClient(hwnd, point)
        val clientRect = RECT()
        User32.INSTANCE.GetClientRect(hwnd, clientRect)
        val code = classifyHitTest(
            snapshot = regions.snapshot(),
            x = point.x,
            y = point.y,
            clientWidth = clientRect.right - clientRect.left,
            clientHeight = clientRect.bottom - clientRect.top,
            maximized = aeroUser32.IsZoomed(hwnd),
            resizeBandPx = resizeBandForHwnd(hwnd),
        )
        // HTCLIENT: let AWT's own answer stand (keeps its default outside published regions).
        return if (code == HTCLIENT) callPrevious(hwnd, WM_NCHITTEST, wParam, lParam).toLong() else code.toLong()
    }

    /**
     * WIN-01 / PITFALLS 8+9: `wParam == 0` is forwarded untouched (AWT owns that shape).
     * `wParam != 0` (proposed-rect form): floating state returns 0 with `rgrc[0]` unchanged
     * (client = the full proposed window rect, hiding the native caption paint); maximized
     * state rewrites `rgrc[0]` in place to [maximizedClientRect] — the intersection with the
     * window's own monitor work area, minus a taskbar-aware auto-hide inset — before also
     * returning 0. Never touches `WM_GETMINMAXINFO` or `WM_DPICHANGED` (out of this handler's
     * scope, forward-only per PITFALLS 14).
     */
    private fun handleNcCalcSize(hwnd: HWND, wParam: WPARAM, lParam: LPARAM): Long {
        if (wParam.toInt() == 0) return callPrevious(hwnd, WM_NCCALCSIZE, wParam, lParam).toLong()

        if (aeroUser32.IsZoomed(hwnd)) {
            val params = Pointer(lParam.toLong())
            val proposed = PxRect(params.getInt(0), params.getInt(4), params.getInt(8), params.getInt(12))

            val monitor = User32.INSTANCE.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST)
            val monitorInfo = WinUser.MONITORINFO()
            User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo)
            val workArea = monitorInfo.rcWork.toPxRect()
            val edges = autoHideEdges(monitorInfo.rcMonitor.toPxRect())

            val client = maximizedClientRect(proposed, workArea, edges)
            params.setInt(0, client.left)
            params.setInt(4, client.top)
            params.setInt(8, client.right)
            params.setInt(12, client.bottom)
            chromeTrace("nccalc-max", this.hwnd, "rect=$client edges=$edges")
        }
        return 0L
    }

    private fun callPrevious(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        User32.INSTANCE.CallWindowProc(previous, hwnd, uMsg, wParam, lParam)
}

/**
 * SNAP-01: the child `WindowProc` — Skiko's `HardwareLayer` (`SunAwtCanvas`) covers the whole
 * client area and answers WM_NCHITTEST before the frame ever sees it, so the frame's
 * classification must be reached through this proc, not by subclassing the frame alone. Owns
 * exactly WM_NCHITTEST: bounces HTTRANSPARENT back up to the frame wherever the frame would
 * answer non-client, `CallWindowProc(previous)` for its own HTCLIENT answer and for every
 * other message.
 */
internal class AeroChildWndProc(
    val frameHwnd: Long,
    val childHwnd: Long,
    val previous: Pointer,
    val regions: HitTestRegionRegistry,
) : WinUser.WindowProc {

    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        LRESULT(
            dispatchSafely(
                handle = { handle(hwnd, uMsg, wParam, lParam) },
                fallback = { callPrevious(hwnd, uMsg, wParam, lParam).toLong() },
            ),
        )

    private fun handle(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long =
        // PITFALLS 3: the child owns WM_NCHITTEST only (OWNED_CHILD_MESSAGES); everything
        // else forwards verbatim before any owned logic can touch it.
        if (uMsg !in OWNED_CHILD_MESSAGES) {
            callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        } else {
            when (uMsg) {
                WM_NCHITTEST -> handleNcHitTest(hwnd, wParam, lParam)
                else -> callPrevious(hwnd, uMsg, wParam, lParam).toLong()
            }
        }

    private fun handleNcHitTest(hwnd: HWND, wParam: WPARAM, lParam: LPARAM): Long {
        val (screenX, screenY) = decodeScreenPoint(lParam.toLong())
        val frame = HWND(Pointer(frameHwnd))
        val point = POINT(screenX, screenY)
        aeroUser32.ScreenToClient(frame, point)
        val clientRect = RECT()
        User32.INSTANCE.GetClientRect(frame, clientRect)
        val code = classifyHitTest(
            snapshot = regions.snapshot(),
            x = point.x,
            y = point.y,
            clientWidth = clientRect.right - clientRect.left,
            clientHeight = clientRect.bottom - clientRect.top,
            maximized = aeroUser32.IsZoomed(frame),
            resizeBandPx = resizeBandForHwnd(frame),
        )
        return if (code == HTCLIENT) {
            callPrevious(hwnd, WM_NCHITTEST, wParam, lParam).toLong()
        } else {
            HTTRANSPARENT.toLong()
        }
    }

    private fun callPrevious(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        User32.INSTANCE.CallWindowProc(previous, hwnd, uMsg, wParam, lParam)
}
