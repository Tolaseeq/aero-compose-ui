<#
.SYNOPSIS
    Non-interfering live-window probe library for VER-11 (aero-compose-ui v3.2.0).
.DESCRIPTION
    Dot-sourceable. Answers "what does Windows get at this point" by reproducing the real
    child-to-parent WM_NCHITTEST chain against a live showcase window, reads window styles,
    maximize geometry, taskbar auto-hide state, min-size clamping and a UI Automation summary,
    and evaluates the VER-11 check set. Every type this file defines lives in the AeroWinProbe
    namespace so it never clashes with tools/capture/AeroCapture.ps1's global RECT/POINT types,
    which this file dot-sources and reuses (Initialize-AeroDpiAwareness, Get-AeroInputState,
    Find-AeroShowcaseWindow, Invoke-AeroWindowCapture, Stop-AeroProcessOfWindow).
.NOTES
    PowerShell 5.1 only. The embedded C# is compiled by the .NET Framework CodeDom compiler and
    must stay C# 5 compatible. Non-interference is enforced by construction: this file never
    delivers synthetic mouse or keyboard input and never grabs screen-region pixels (see the
    project's capture-and-input allow-list for the specific banned Win32/GDI calls); the only
    reads are Win32 queries against our own showcase window's own HWND, and this file never
    reads or records the title of any window other than an exact-title match of our own window.
#>

#Requires -Version 5.1
Set-StrictMode -Version 2

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here '..\capture\AeroCapture.ps1')

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

if (-not ('AeroWinProbe.Native' -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
using System.Text;

namespace AeroWinProbe
{
    [StructLayout(LayoutKind.Sequential)]
    public struct WRect
    {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct WPoint
    {
        public int X;
        public int Y;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct MonitorInfo
    {
        public int cbSize;
        public WRect rcMonitor;
        public WRect rcWork;
        public uint dwFlags;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct AppBarData
    {
        public int cbSize;
        public IntPtr hWnd;
        public uint uCallbackMessage;
        public uint uEdge;
        public WRect rc;
        public IntPtr lParam;
    }

    public delegate bool EnumChildProc(IntPtr hWnd, IntPtr lParam);

    public class Native
    {
        public const uint WM_NCHITTEST = 0x0084;
        public const uint WM_SYSCOMMAND = 0x0112;
        public const int SC_MAXIMIZE = 0xF030;
        public const int SC_RESTORE = 0xF120;
        public const int HTTRANSPARENT = -1;
        public const int HTNOWHERE = 0;
        public const int HTCLIENT = 1;
        public const int HTCAPTION = 2;
        public const int HTMINBUTTON = 8;
        public const int HTMAXBUTTON = 9;
        public const int HTLEFT = 10;
        public const int HTRIGHT = 11;
        public const int HTTOP = 12;
        public const int HTTOPLEFT = 13;
        public const int HTTOPRIGHT = 14;
        public const int HTBOTTOM = 15;
        public const int HTBOTTOMLEFT = 16;
        public const int HTBOTTOMRIGHT = 17;
        public const int HTCLOSE = 20;

        public const int GWL_STYLE = -16;
        public const int GWL_EXSTYLE = -20;
        public const int GWLP_WNDPROC = -4;

        public const uint WS_POPUP = 0x80000000;
        public const uint WS_CAPTION = 0x00C00000;
        public const uint WS_SYSMENU = 0x00080000;
        public const uint WS_THICKFRAME = 0x00040000;
        public const uint WS_MINIMIZEBOX = 0x00020000;
        public const uint WS_MAXIMIZEBOX = 0x00010000;
        public const uint WS_CLIPCHILDREN = 0x02000000;

        public const uint CWP_SKIPINVISIBLE = 0x1;
        public const uint CWP_SKIPDISABLED = 0x2;
        public const uint CWP_SKIPTRANSPARENT = 0x4;

        public const uint SMTO_ABORTIFHUNG = 0x2;

        public const uint SWP_NOSIZE = 0x1;
        public const uint SWP_NOMOVE = 0x2;
        public const uint SWP_NOZORDER = 0x4;
        public const uint SWP_NOACTIVATE = 0x10;
        public static readonly IntPtr HWND_BOTTOM = (IntPtr)1;

        public const uint MONITOR_DEFAULTTONEAREST = 2;

        public const uint ABM_GETSTATE = 4;
        public const uint ABM_GETAUTOHIDEBAREX = 0xB;
        public const uint ABS_AUTOHIDE = 1;
        public const uint ABE_LEFT = 0;
        public const uint ABE_TOP = 1;
        public const uint ABE_RIGHT = 2;
        public const uint ABE_BOTTOM = 3;

        public const uint GW_HWNDPREV = 3;

        [DllImport("user32.dll")]
        public static extern IntPtr SendMessageTimeoutW(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam, uint fuFlags, uint uTimeout, out IntPtr lpdwResult);

        [DllImport("user32.dll")]
        public static extern bool PostMessageW(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll")]
        public static extern IntPtr GetWindowLongPtrW(IntPtr hWnd, int nIndex);

        [DllImport("user32.dll")]
        public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);

        [DllImport("user32.dll")]
        public static extern bool GetClientRect(IntPtr hWnd, out WRect lpRect);

        [DllImport("user32.dll")]
        public static extern bool GetWindowRect(IntPtr hWnd, out WRect lpRect);

        [DllImport("user32.dll")]
        public static extern bool ClientToScreen(IntPtr hWnd, ref WPoint lpPoint);

        [DllImport("user32.dll")]
        public static extern bool ScreenToClient(IntPtr hWnd, ref WPoint lpPoint);

        [DllImport("user32.dll")]
        public static extern IntPtr ChildWindowFromPointEx(IntPtr hWndParent, WPoint pt, uint uFlags);

        [DllImport("user32.dll")]
        public static extern IntPtr GetParent(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool EnumChildWindows(IntPtr hWndParent, EnumChildProc lpEnumFunc, IntPtr lParam);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        public static extern int GetClassNameW(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

        [DllImport("user32.dll")]
        public static extern uint GetDpiForWindow(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool IsZoomed(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool IsIconic(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool IsWindowVisible(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern IntPtr MonitorFromWindow(IntPtr hwnd, uint dwFlags);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        public static extern bool GetMonitorInfoW(IntPtr hMonitor, ref MonitorInfo lpmi);

        [DllImport("shell32.dll")]
        public static extern uint SHAppBarMessage(uint dwMessage, ref AppBarData pData);

        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        [DllImport("user32.dll")]
        public static extern IntPtr GetForegroundWindow();

        [DllImport("user32.dll")]
        public static extern IntPtr GetWindow(IntPtr hWnd, uint uCmd);

        [DllImport("user32.dll")]
        public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        public const int SW_SHOWNOACTIVATE = 4;

        public static IntPtr MakeLParam(int x, int y)
        {
            return (IntPtr)(((y & 0xFFFF) << 16) | (x & 0xFFFF));
        }

        public static int SendNcHitTest(IntPtr hWnd, int screenX, int screenY)
        {
            IntPtr result;
            IntPtr lParam = MakeLParam(screenX, screenY);
            SendMessageTimeoutW(hWnd, WM_NCHITTEST, IntPtr.Zero, lParam, SMTO_ABORTIFHUNG, 2000, out result);
            return unchecked((int)result.ToInt64());
        }

        public static uint GetStyle(IntPtr hWnd)
        {
            return unchecked((uint)GetWindowLongPtrW(hWnd, GWL_STYLE).ToInt64());
        }

        public static uint GetExStyle(IntPtr hWnd)
        {
            return unchecked((uint)GetWindowLongPtrW(hWnd, GWL_EXSTYLE).ToInt64());
        }

        public static List<IntPtr> EnumAllChildren(IntPtr hWndParent)
        {
            List<IntPtr> found = new List<IntPtr>();
            EnumChildProc callback = delegate(IntPtr hWnd, IntPtr lParam)
            {
                found.Add(hWnd);
                return true;
            };
            EnumChildWindows(hWndParent, callback, IntPtr.Zero);
            return found;
        }

        public static string GetClassName(IntPtr hWnd)
        {
            StringBuilder sb = new StringBuilder(256);
            GetClassNameW(hWnd, sb, sb.Capacity);
            return sb.ToString();
        }
    }

    public class RegionCompare
    {
        public static Dictionary<string, object> Compare(string beforePath, string afterPath, int top, int bottom, int left, int right, int channelTolerance)
        {
            Bitmap a = new Bitmap(beforePath);
            Bitmap b = new Bitmap(afterPath);
            Dictionary<string, object> result = new Dictionary<string, object>();
            try
            {
                if (a.Width != b.Width || a.Height != b.Height)
                {
                    result["Comparable"] = false;
                    result["DiffPixels"] = -1;
                    result["MaxChannelDelta"] = -1;
                    return result;
                }
                int effectiveLeft = left;
                int effectiveRight = right > 0 ? right : a.Width;
                int effectiveBottom = bottom > 0 ? bottom : a.Height;

                BitmapData da = a.LockBits(new Rectangle(0, 0, a.Width, a.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
                BitmapData db = b.LockBits(new Rectangle(0, 0, b.Width, b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
                int diffCount = 0;
                int maxDelta = 0;
                try
                {
                    int stride = da.Stride;
                    byte[] bufA = new byte[stride * a.Height];
                    byte[] bufB = new byte[stride * b.Height];
                    Marshal.Copy(da.Scan0, bufA, 0, bufA.Length);
                    Marshal.Copy(db.Scan0, bufB, 0, bufB.Length);
                    for (int y = top; y < effectiveBottom && y < a.Height; y++)
                    {
                        int rowStart = y * stride;
                        for (int x = effectiveLeft; x < effectiveRight && x < a.Width; x++)
                        {
                            int idx = rowStart + x * 4;
                            int dB = Math.Abs(bufA[idx] - bufB[idx]);
                            int dG = Math.Abs(bufA[idx + 1] - bufB[idx + 1]);
                            int dR = Math.Abs(bufA[idx + 2] - bufB[idx + 2]);
                            int dA = Math.Abs(bufA[idx + 3] - bufB[idx + 3]);
                            int localMax = Math.Max(Math.Max(dB, dG), Math.Max(dR, dA));
                            if (localMax > maxDelta) maxDelta = localMax;
                            if (localMax > channelTolerance) diffCount++;
                        }
                    }
                }
                finally
                {
                    a.UnlockBits(da);
                    b.UnlockBits(db);
                }
                result["Comparable"] = true;
                result["DiffPixels"] = diffCount;
                result["MaxChannelDelta"] = maxDelta;
                return result;
            }
            finally
            {
                a.Dispose();
                b.Dispose();
            }
        }
    }
}
"@
}

# Single source of probe geometry, in dp, scaled by GetDpiForWindow/96 at point-computation time.
# 'main' matches AeroTitleBar.kt's current layout: 32dp row, 8dp horizontal padding, three 46dp
# buttons in order Minimize/Maximize/Close from the right. 'narrow' is the SHW-17 fixture
# (NarrowQueueWindow.kt, 300x480dp): same row/button constants plus the fixture's own elements —
# a 24x20dp badge starting at x 8dp and the 60x22dp "Вернуть" overlay at x 88..148dp — and its
# own AWT minimum (260x200, D-01), which V11-N-MINSIZE checks against instead of 320x240.
$WinProbeLayout = @{
    main = @{
        titleHeightDp = 32
        rowPaddingDp  = 8
        buttonWidthDp = 46
        buttonOrder   = @('Minimize', 'Maximize', 'Close')
    }
    narrow = @{
        titleHeightDp = 32
        rowPaddingDp  = 8
        buttonWidthDp = 46
        buttonOrder   = @('Minimize', 'Maximize', 'Close')
        # The caption probe point sits left of the "Вернуть" overlay (x 88dp), not at the
        # midpoint of the free span (which the overlay cuts into).
        captionXDp    = 70
        extraPointsDp = @{
            leading               = @{ x = 20;  y = 16 }
            captionRightOfLeading = @{ x = 36;  y = 16 }
            marked                = @{ x = 118; y = 16 }
            captionLeftOfMarked   = @{ x = 82;  y = 16 }
        }
        minWidthDp    = 260
        minHeightDp   = 200
    }
}

function Get-WinProbeNamedPoints {
    <#
    .SYNOPSIS
        Computes the named client points (physical pixels, relative to the frame's client
        origin) for a layout entry given a client size in dp and a DPI scale.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][hashtable]$Layout,
        [Parameter(Mandatory = $true)][double]$ClientWidthDp,
        [Parameter(Mandatory = $true)][double]$ClientHeightDp,
        [Parameter(Mandatory = $true)][double]$Scale
    )

    $padding = $Layout.rowPaddingDp
    $btnW = $Layout.buttonWidthDp
    $titleMidYDp = $Layout.titleHeightDp / 2.0

    $closeCenterDp = $ClientWidthDp - $padding - (0.5 * $btnW)
    $maxCenterDp = $ClientWidthDp - $padding - (1.5 * $btnW)
    $minCenterDp = $ClientWidthDp - $padding - (2.5 * $btnW)
    $minLeftEdgeDp = $ClientWidthDp - $padding - (3.0 * $btnW)
    $captionLeftOfMinDp = $minLeftEdgeDp - 6
    $captionXDp = if ($Layout.ContainsKey('captionXDp')) { [double]$Layout.captionXDp } else { ($padding + $minLeftEdgeDp) / 2.0 }

    function Px([double]$dp) { return [int][Math]::Round($dp * $Scale) }

    $widthPx = Px $ClientWidthDp
    $heightPx = Px $ClientHeightDp

    $points = [ordered]@{
        caption           = @{ x = (Px $captionXDp); y = (Px $titleMidYDp) }
        captionLeftOfMin   = @{ x = (Px $captionLeftOfMinDp); y = (Px $titleMidYDp) }
        min                = @{ x = (Px $minCenterDp); y = (Px $titleMidYDp) }
        max                = @{ x = (Px $maxCenterDp); y = (Px $titleMidYDp) }
        close              = @{ x = (Px $closeCenterDp); y = (Px $titleMidYDp) }
        client             = @{ x = [int]($widthPx / 2); y = [int]($heightPx / 2) }
        edgeLeft           = @{ x = 2; y = [int]($heightPx / 2) }
        edgeRight          = @{ x = ($widthPx - 2); y = [int]($heightPx / 2) }
        edgeTop            = @{ x = (Px $captionXDp); y = 2 }
        edgeBottom         = @{ x = (Px $captionXDp); y = ($heightPx - 2) }
        cornerTL           = @{ x = 2; y = 2 }
        cornerTR           = @{ x = ($widthPx - 2); y = 2 }
        cornerBL           = @{ x = 2; y = ($heightPx - 2) }
        cornerBR           = @{ x = ($widthPx - 2); y = ($heightPx - 2) }
    }
    if ($Layout.ContainsKey('extraPointsDp')) {
        foreach ($name in $Layout.extraPointsDp.Keys) {
            $ep = $Layout.extraPointsDp[$name]
            $points[$name] = @{ x = (Px ([double]$ep.x)); y = (Px ([double]$ep.y)) }
        }
    }
    return $points
}

function Start-WinProbeShowcase {
    <#
    .SYNOPSIS
        Launches `gradlew.bat` hidden for the given task, always forwarding -Paero.windowState=true
        plus any caller-supplied Gradle properties, redirecting stdout/stderr to per-launch logs.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][ValidateSet('run', 'hotRun')][string]$Task,
        [string[]]$GradleProps = @(),
        [Parameter(Mandatory = $true)][string]$LogDir,
        [string]$RepoRoot = 'C:\1A_WORK\ui_lib'
    )
    if (-not (Test-Path -LiteralPath $LogDir)) {
        New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
    }
    $stamp = Get-Date -Format 'HHmmss.fff'
    $stdout = Join-Path $LogDir "winprobe-$Task-$stamp.out.log"
    $stderr = Join-Path $LogDir "winprobe-$Task-$stamp.err.log"
    $gradlewPath = Join-Path $RepoRoot 'gradlew.bat'

    $gradleTask = if ($Task -eq 'hotRun') { ':showcase:hotRun', '--mainClass=com.mordred.showcase.MainKt' } else { ':showcase:run' }
    $argList = @('--console=plain') + $gradleTask + @('-Paero.windowState=true') + $GradleProps

    $process = Start-Process -FilePath $gradlewPath -ArgumentList $argList -WorkingDirectory $RepoRoot `
        -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru

    [pscustomobject]@{
        Process = $process
        StdOut  = $stdout
        StdErr  = $stderr
    }
}

function Remove-AeroOrphanShowcaseProcesses {
    <#
    .SYNOPSIS
        Kills any leftover showcase JVM by command line, in case a launch's own termination
        missed a child process — mirrors tools/capture/Invoke-ShowcaseSweep.ps1's function of the
        same name (not dot-sourced here to keep this file's own dependency surface to
        AeroCapture.ps1 only).
    #>
    $orphans = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and $_.CommandLine -match 'com\.mordred\.showcase\.MainKt' }
    $killed = @()
    foreach ($orphan in $orphans) {
        Stop-Process -Id $orphan.ProcessId -Force -ErrorAction SilentlyContinue
        $killed += $orphan.ProcessId
    }
    return $killed
}

function Get-WinProbeReporterState {
    <#
    .SYNOPSIS
        Parses the latest AERO_WINDOW_STATE line for a given label from a launch's stdout log.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$LogPath,
        [Parameter(Mandatory = $true)][string]$Label,
        [int]$AfterLineIndex = 0
    )
    if (-not (Test-Path -LiteralPath $LogPath)) { return $null }
    $lines = @(Get-Content -LiteralPath $LogPath -ErrorAction SilentlyContinue)
    if (-not $lines) { return $null }
    $prefix = "AERO_WINDOW_STATE label=$Label "
    $match = $null
    for ($i = $AfterLineIndex; $i -lt $lines.Count; $i++) {
        if ($lines[$i].StartsWith($prefix) -or $lines[$i] -eq "AERO_WINDOW_STATE label=$Label") {
            $match = $lines[$i]
        }
    }
    if (-not $match) { return $null }
    $fields = [ordered]@{}
    foreach ($part in ($match -split ' ')) {
        if ($part -match '^([A-Za-z]+)=(.*)$') {
            $fields[$Matches[1]] = $Matches[2]
        }
    }
    return [pscustomobject]$fields
}

function Wait-WinProbeReporter {
    <#
    .SYNOPSIS
        Polls -Predicate (a scriptblock returning the reporter state or $null) until it returns a
        non-null, predicate-satisfying value or -TimeoutSec elapses.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Predicate,
        [int]$TimeoutSec = 10
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $value = & $Predicate
        if ($value) { return $value }
        Start-Sleep -Milliseconds 150
    }
    return $null
}

function Wait-WinProbeEvent {
    <#
    .SYNOPSIS
        Polls a launch's stdout log for an `AERO_EVENT name=<Name>` line (optionally requiring
        -Detail to appear in it) until one appears or -TimeoutSec elapses. Lets a verification
        session read click/close-request evidence without sending any real input.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$LogPath,
        [Parameter(Mandatory = $true)][string]$Name,
        [string]$Detail = '',
        [int]$TimeoutSec = 10
    )
    $prefix = "AERO_EVENT name=$Name"
    return Wait-WinProbeReporter -Predicate {
        if (-not (Test-Path -LiteralPath $LogPath)) { return $null }
        $lines = Get-Content -LiteralPath $LogPath -ErrorAction SilentlyContinue
        if (-not $lines) { return $null }
        foreach ($line in $lines) {
            if ($line.StartsWith($prefix) -and ($Detail -eq '' -or $line.Contains($Detail))) { return $line }
        }
        return $null
    } -TimeoutSec $TimeoutSec
}

function Get-WinProbeWindowInfo {
    <#
    .SYNOPSIS
        Reads window/client rects, DPI+scale, decoded GWL_STYLE/GWL_EXSTYLE, IsZoomed/IsIconic,
        GWLP_WNDPROC, owning process id/path, and jvmKind (JBR vs standard).
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)

    $windowRect = New-Object AeroWinProbe.WRect
    [AeroWinProbe.Native]::GetWindowRect($Hwnd, [ref]$windowRect) | Out-Null
    $clientRect = New-Object AeroWinProbe.WRect
    [AeroWinProbe.Native]::GetClientRect($Hwnd, [ref]$clientRect) | Out-Null
    $dpi = [AeroWinProbe.Native]::GetDpiForWindow($Hwnd)
    $scale = $dpi / 96.0

    $style = [AeroWinProbe.Native]::GetStyle($Hwnd)
    $exStyle = [AeroWinProbe.Native]::GetExStyle($Hwnd)

    $styleNames = New-Object System.Collections.Generic.List[string]
    if (($style -band [AeroWinProbe.Native]::WS_POPUP) -ne 0) { $styleNames.Add('WS_POPUP') }
    if (($style -band [AeroWinProbe.Native]::WS_CAPTION) -eq [AeroWinProbe.Native]::WS_CAPTION) { $styleNames.Add('WS_CAPTION') }
    if (($style -band [AeroWinProbe.Native]::WS_SYSMENU) -ne 0) { $styleNames.Add('WS_SYSMENU') }
    if (($style -band [AeroWinProbe.Native]::WS_THICKFRAME) -ne 0) { $styleNames.Add('WS_THICKFRAME') }
    if (($style -band [AeroWinProbe.Native]::WS_MINIMIZEBOX) -ne 0) { $styleNames.Add('WS_MINIMIZEBOX') }
    if (($style -band [AeroWinProbe.Native]::WS_MAXIMIZEBOX) -ne 0) { $styleNames.Add('WS_MAXIMIZEBOX') }
    if (($style -band [AeroWinProbe.Native]::WS_CLIPCHILDREN) -ne 0) { $styleNames.Add('WS_CLIPCHILDREN') }

    [uint32]$ownerPid = 0
    [AeroWinProbe.Native]::GetWindowThreadProcessId($Hwnd, [ref]$ownerPid) | Out-Null
    $exePath = $null
    try { $exePath = (Get-Process -Id $ownerPid -ErrorAction Stop).Path } catch { $exePath = $null }
    $jvmKind = if ($exePath -and $exePath -match '(?i)jbr') { 'JBR' } else { 'standard' }

    [pscustomobject]@{
        Hwnd           = $Hwnd
        WindowRect     = $windowRect
        ClientRect     = $clientRect
        Dpi            = $dpi
        Scale          = $scale
        StyleHex       = ('0x{0:X8}' -f $style)
        ExStyleHex     = ('0x{0:X8}' -f $exStyle)
        StyleNames     = $styleNames
        IsZoomed       = [bool][AeroWinProbe.Native]::IsZoomed($Hwnd)
        IsIconic       = [bool][AeroWinProbe.Native]::IsIconic($Hwnd)
        WndProc        = [AeroWinProbe.Native]::GetWindowLongPtrW($Hwnd, [AeroWinProbe.Native]::GWLP_WNDPROC)
        ProcessId      = $ownerPid
        ProcessPath    = $exePath
        JvmKind        = $jvmKind
    }
}

function Get-WinProbeChildren {
    <#
    .SYNOPSIS
        Lists every descendant HWND: class name, rect relative to the frame's client origin,
        visible flag, GWLP_WNDPROC value, and whether it covers the whole client area.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)

    $origin = New-Object AeroWinProbe.WPoint
    $origin.X = 0
    $origin.Y = 0
    [AeroWinProbe.Native]::ClientToScreen($Hwnd, [ref]$origin) | Out-Null

    $clientRect = New-Object AeroWinProbe.WRect
    [AeroWinProbe.Native]::GetClientRect($Hwnd, [ref]$clientRect) | Out-Null
    $clientW = $clientRect.Right - $clientRect.Left
    $clientH = $clientRect.Bottom - $clientRect.Top

    $children = [AeroWinProbe.Native]::EnumAllChildren($Hwnd)
    $result = New-Object System.Collections.Generic.List[object]
    foreach ($child in $children) {
        $rect = New-Object AeroWinProbe.WRect
        [AeroWinProbe.Native]::GetWindowRect($child, [ref]$rect) | Out-Null
        $relLeft = $rect.Left - $origin.X
        $relTop = $rect.Top - $origin.Y
        $relRight = $rect.Right - $origin.X
        $relBottom = $rect.Bottom - $origin.Y
        $coversClient = ($relLeft -le 0) -and ($relTop -le 0) -and ($relRight -ge $clientW) -and ($relBottom -ge $clientH)
        [void]$result.Add([pscustomobject]@{
            Hwnd         = $child
            ClassName    = [AeroWinProbe.Native]::GetClassName($child)
            RelLeft      = $relLeft
            RelTop       = $relTop
            RelRight     = $relRight
            RelBottom    = $relBottom
            IsVisible    = [bool][AeroWinProbe.Native]::IsWindowVisible($child)
            WndProc      = [AeroWinProbe.Native]::GetWindowLongPtrW($child, [AeroWinProbe.Native]::GWLP_WNDPROC)
            CoversClient = $coversClient
        })
    }
    return $result
}

function Get-WinProbeHitTestCodeName {
    param([int]$Code)
    switch ($Code) {
        -1 { return 'HTTRANSPARENT' }
        0 { return 'HTNOWHERE' }
        1 { return 'HTCLIENT' }
        2 { return 'HTCAPTION' }
        8 { return 'HTMINBUTTON' }
        9 { return 'HTMAXBUTTON' }
        10 { return 'HTLEFT' }
        11 { return 'HTRIGHT' }
        12 { return 'HTTOP' }
        13 { return 'HTTOPLEFT' }
        14 { return 'HTTOPRIGHT' }
        15 { return 'HTBOTTOM' }
        16 { return 'HTBOTTOMLEFT' }
        17 { return 'HTBOTTOMRIGHT' }
        20 { return 'HTCLOSE' }
        default { return "HT_$Code" }
    }
}

function Invoke-WinProbeHitTest {
    <#
    .SYNOPSIS
        Reproduces Windows' real hit-test chain: descends from the frame to the deepest child at
        the point via ChildWindowFromPointEx, sends WM_NCHITTEST to that child, and — while the
        answer is HTTRANSPARENT — resends to GetParent, per this project's own VER-11 discipline.
    .PARAMETER ClientX
        X offset in physical pixels from the frame's client-area origin.
    .PARAMETER ClientY
        Y offset in physical pixels from the frame's client-area origin.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][IntPtr]$Hwnd,
        [Parameter(Mandatory = $true)][int]$ClientX,
        [Parameter(Mandatory = $true)][int]$ClientY
    )

    $origin = New-Object AeroWinProbe.WPoint
    $origin.X = 0
    $origin.Y = 0
    [AeroWinProbe.Native]::ClientToScreen($Hwnd, [ref]$origin) | Out-Null
    $screenX = $origin.X + $ClientX
    $screenY = $origin.Y + $ClientY

    # Descend to the deepest child at this screen point, converting to each window's own client
    # space before each ChildWindowFromPointEx call (that API expects parent-client coordinates).
    $current = $Hwnd
    $flags = [AeroWinProbe.Native]::CWP_SKIPINVISIBLE -bor [AeroWinProbe.Native]::CWP_SKIPDISABLED -bor [AeroWinProbe.Native]::CWP_SKIPTRANSPARENT
    for ($depth = 0; $depth -lt 25; $depth++) {
        $local = New-Object AeroWinProbe.WPoint
        $local.X = $screenX
        $local.Y = $screenY
        [AeroWinProbe.Native]::ScreenToClient($current, [ref]$local) | Out-Null
        $childAt = [AeroWinProbe.Native]::ChildWindowFromPointEx($current, $local, $flags)
        if ($childAt -eq [IntPtr]::Zero -or $childAt -eq $current) { break }
        $current = $childAt
    }
    $deepest = $current

    $chain = New-Object System.Collections.Generic.List[string]
    $walker = $deepest
    $code = [AeroWinProbe.Native]::SendNcHitTest($walker, $screenX, $screenY)
    $chain.Add("$([AeroWinProbe.Native]::GetClassName($walker)):$code")
    $hops = 0
    while ($code -eq [AeroWinProbe.Native]::HTTRANSPARENT -and $hops -lt 10) {
        $parent = [AeroWinProbe.Native]::GetParent($walker)
        if ($parent -eq [IntPtr]::Zero) { break }
        $walker = $parent
        $code = [AeroWinProbe.Native]::SendNcHitTest($walker, $screenX, $screenY)
        $chain.Add("$([AeroWinProbe.Native]::GetClassName($walker)):$code")
        $hops++
    }

    $directFrameCode = [AeroWinProbe.Native]::SendNcHitTest($Hwnd, $screenX, $screenY)

    [pscustomobject]@{
        Code             = $code
        CodeName         = Get-WinProbeHitTestCodeName -Code $code
        AnsweredBy       = [AeroWinProbe.Native]::GetClassName($walker)
        Chain            = ($chain -join ' -> ')
        DirectFrameCode  = $directFrameCode
        ScreenX          = $screenX
        ScreenY          = $screenY
    }
}

function Invoke-WinProbeMaximize {
    <#
    .SYNOPSIS
        Sends SC_MAXIMIZE, immediately pins the window at the bottom of the z-order so it never
        steals the foreground, measures client rect vs monitor geometry, re-runs the hit-test
        points, then SC_RESTORE + bottom again. Records whether the showcase process ever became
        the foreground process (T-22-08 measurement, feeds finding F9).
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][IntPtr]$Hwnd,
        [string]$LogPath,
        [string]$Label = 'main'
    )

    [uint32]$appPid = 0
    [AeroWinProbe.Native]::GetWindowThreadProcessId($Hwnd, [ref]$appPid) | Out-Null

    function Get-ForegroundOwnedByApp {
        $fg = [AeroWinProbe.Native]::GetForegroundWindow()
        [uint32]$fgPid = 0
        [AeroWinProbe.Native]::GetWindowThreadProcessId($fg, [ref]$fgPid) | Out-Null
        return ($fgPid -eq $appPid)
    }

    $foregroundBefore = Get-ForegroundOwnedByApp
    $sizeSwp = [AeroWinProbe.Native]::SWP_NOMOVE -bor [AeroWinProbe.Native]::SWP_NOSIZE -bor [AeroWinProbe.Native]::SWP_NOACTIVATE

    $result = [AeroWinProbe.Native]::SendMessageTimeoutW($Hwnd, [AeroWinProbe.Native]::WM_SYSCOMMAND, [IntPtr][AeroWinProbe.Native]::SC_MAXIMIZE, [IntPtr]::Zero, [AeroWinProbe.Native]::SMTO_ABORTIFHUNG, 2000, [ref]([IntPtr]::Zero))
    [AeroWinProbe.Native]::SetWindowPos($Hwnd, [AeroWinProbe.Native]::HWND_BOTTOM, 0, 0, 0, 0, $sizeSwp) | Out-Null

    Start-Sleep -Milliseconds 300
    $foregroundDuringMaximize = Get-ForegroundOwnedByApp

    $reporterAfterMax = if ($LogPath) { Get-WinProbeReporterState -LogPath $LogPath -Label $Label } else { $null }

    $windowRectMax = New-Object AeroWinProbe.WRect
    [AeroWinProbe.Native]::GetWindowRect($Hwnd, [ref]$windowRectMax) | Out-Null

    # WIN-01 (22-05): WM_NCCALCSIZE only ever controls the CLIENT rectangle, never the outer
    # window rectangle GetWindowRect reports (WM_GETMINMAXINFO owns that, and this milestone's
    # NCCALCSIZE-only fix deliberately leaves it untouched — Plan 11 territory). The window rect
    # keeps its pre-existing sizing-border overhang past the monitor by design; what must match
    # the work area (minus the auto-hide inset) is the CLIENT rect converted to screen
    # coordinates via ClientToScreen, which is what AWT/Skia actually renders into and what a
    # human looking at the maximized window actually sees.
    $clientOrigin = New-Object AeroWinProbe.WPoint
    $clientOrigin.X = 0
    $clientOrigin.Y = 0
    [AeroWinProbe.Native]::ClientToScreen($Hwnd, [ref]$clientOrigin) | Out-Null
    $clientRectLocal = New-Object AeroWinProbe.WRect
    [AeroWinProbe.Native]::GetClientRect($Hwnd, [ref]$clientRectLocal) | Out-Null
    $clientRectMax = New-Object AeroWinProbe.WRect
    $clientRectMax.Left = $clientOrigin.X
    $clientRectMax.Top = $clientOrigin.Y
    $clientRectMax.Right = $clientOrigin.X + ($clientRectLocal.Right - $clientRectLocal.Left)
    $clientRectMax.Bottom = $clientOrigin.Y + ($clientRectLocal.Bottom - $clientRectLocal.Top)

    $monitor = [AeroWinProbe.Native]::MonitorFromWindow($Hwnd, [AeroWinProbe.Native]::MONITOR_DEFAULTTONEAREST)
    $monitorInfo = New-Object AeroWinProbe.MonitorInfo
    $monitorInfo.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroWinProbe.MonitorInfo]))
    [AeroWinProbe.Native]::GetMonitorInfoW($monitor, [ref]$monitorInfo) | Out-Null

    $edgeTopHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX 200 -ClientY 2

    $result2 = [AeroWinProbe.Native]::SendMessageTimeoutW($Hwnd, [AeroWinProbe.Native]::WM_SYSCOMMAND, [IntPtr][AeroWinProbe.Native]::SC_RESTORE, [IntPtr]::Zero, [AeroWinProbe.Native]::SMTO_ABORTIFHUNG, 2000, [ref]([IntPtr]::Zero))
    [AeroWinProbe.Native]::SetWindowPos($Hwnd, [AeroWinProbe.Native]::HWND_BOTTOM, 0, 0, 0, 0, $sizeSwp) | Out-Null
    Start-Sleep -Milliseconds 300
    $foregroundAfterRestore = Get-ForegroundOwnedByApp

    [pscustomobject]@{
        ForegroundBefore         = $foregroundBefore
        ForegroundTaken          = $foregroundDuringMaximize
        ForegroundAfterRestore   = $foregroundAfterRestore
        WindowRectMaximized      = $windowRectMax
        ClientRectMaximized      = $clientRectMax
        MonitorRcWork            = $monitorInfo.rcWork
        MonitorRcMonitor         = $monitorInfo.rcMonitor
        ReporterAfterMaximize    = $reporterAfterMax
        EdgeTopHitWhileMaximized = $edgeTopHit
    }
}

function Get-WinProbeTaskbar {
    <#
    .SYNOPSIS
        Reads ABM_GETSTATE flags and, per edge, ABM_GETAUTOHIDEBAREX against the window's own
        monitor rect, returning the list of edges with an active auto-hide bar.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)

    $stateData = New-Object AeroWinProbe.AppBarData
    $stateData.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroWinProbe.AppBarData]))
    $state = [AeroWinProbe.Native]::SHAppBarMessage([AeroWinProbe.Native]::ABM_GETSTATE, [ref]$stateData)
    $autoHideOn = (([uint32]$state) -band [AeroWinProbe.Native]::ABS_AUTOHIDE) -ne 0

    $monitor = [AeroWinProbe.Native]::MonitorFromWindow($Hwnd, [AeroWinProbe.Native]::MONITOR_DEFAULTTONEAREST)
    $monitorInfo = New-Object AeroWinProbe.MonitorInfo
    $monitorInfo.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroWinProbe.MonitorInfo]))
    [AeroWinProbe.Native]::GetMonitorInfoW($monitor, [ref]$monitorInfo) | Out-Null

    $edges = @{
        Left   = [AeroWinProbe.Native]::ABE_LEFT
        Top    = [AeroWinProbe.Native]::ABE_TOP
        Right  = [AeroWinProbe.Native]::ABE_RIGHT
        Bottom = [AeroWinProbe.Native]::ABE_BOTTOM
    }
    $autoHideEdges = New-Object System.Collections.Generic.List[string]
    foreach ($name in $edges.Keys) {
        $data = New-Object AeroWinProbe.AppBarData
        $data.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroWinProbe.AppBarData]))
        $data.uEdge = $edges[$name]
        $data.rc = $monitorInfo.rcMonitor
        $ret = [AeroWinProbe.Native]::SHAppBarMessage([AeroWinProbe.Native]::ABM_GETAUTOHIDEBAREX, [ref]$data)
        $hasBar = ($ret -ne 0) -and ((($data.rc.Right - $data.rc.Left) -gt 0) -or (($data.rc.Bottom - $data.rc.Top) -gt 0))
        if ($hasBar) { [void]$autoHideEdges.Add($name) }
    }

    [pscustomobject]@{
        AutoHideOn    = $autoHideOn
        AutoHideEdges = $autoHideEdges
        RcMonitor     = $monitorInfo.rcMonitor
        RcWork        = $monitorInfo.rcWork
    }
}

function Invoke-WinProbeMinSize {
    <#
    .SYNOPSIS
        Saves the window rect, requests a 50x50px resize, reads back the resulting size, then
        restores the saved rect. Returns requested vs obtained px.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)

    $saved = New-Object AeroWinProbe.WRect
    [AeroWinProbe.Native]::GetWindowRect($Hwnd, [ref]$saved) | Out-Null

    $flags = [AeroWinProbe.Native]::SWP_NOMOVE -bor [AeroWinProbe.Native]::SWP_NOZORDER -bor [AeroWinProbe.Native]::SWP_NOACTIVATE
    [AeroWinProbe.Native]::SetWindowPos($Hwnd, [IntPtr]::Zero, 0, 0, 50, 50, $flags) | Out-Null
    Start-Sleep -Milliseconds 150

    $result = New-Object AeroWinProbe.WRect
    [AeroWinProbe.Native]::GetWindowRect($Hwnd, [ref]$result) | Out-Null
    $obtainedW = $result.Right - $result.Left
    $obtainedH = $result.Bottom - $result.Top

    $restoreFlags = [AeroWinProbe.Native]::SWP_NOZORDER -bor [AeroWinProbe.Native]::SWP_NOACTIVATE
    [AeroWinProbe.Native]::SetWindowPos($Hwnd, [IntPtr]::Zero, $saved.Left, $saved.Top, ($saved.Right - $saved.Left), ($saved.Bottom - $saved.Top), $restoreFlags) | Out-Null

    [pscustomobject]@{
        RequestedWidth  = 50
        RequestedHeight = 50
        ObtainedWidth   = $obtainedW
        ObtainedHeight  = $obtainedH
    }
}

function Get-WinProbeUiaSummary {
    <#
    .SYNOPSIS
        Descendant count and ControlType histogram of our own window only, via
        AutomationElement.FromHandle.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)

    $element = [System.Windows.Automation.AutomationElement]::FromHandle($Hwnd)
    if (-not $element) {
        return [pscustomobject]@{ DescendantCount = 0; Histogram = @{} }
    }
    $descendants = $element.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
    $histogram = @{}
    foreach ($node in $descendants) {
        $ctName = $node.Current.ControlType.ProgrammaticName
        if ($histogram.ContainsKey($ctName)) { $histogram[$ctName]++ } else { $histogram[$ctName] = 1 }
    }
    [pscustomobject]@{
        DescendantCount = $descendants.Count
        Histogram       = $histogram
    }
}

function Send-WinProbeMessage {
    <#
    .SYNOPSIS
        Sends (or, with -Post, posts) a generic Win32 message into our own window, returning the
        LRESULT for Send.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][IntPtr]$Hwnd,
        [Parameter(Mandatory = $true)][uint32]$Msg,
        [long]$WParam = 0,
        [long]$LParam = 0,
        [switch]$Post
    )
    if ($Post) {
        return [AeroWinProbe.Native]::PostMessageW($Hwnd, $Msg, [IntPtr]$WParam, [IntPtr]$LParam)
    }
    $result = [IntPtr]::Zero
    [AeroWinProbe.Native]::SendMessageTimeoutW($Hwnd, $Msg, [IntPtr]$WParam, [IntPtr]$LParam, [AeroWinProbe.Native]::SMTO_ABORTIFHUNG, 2000, [ref]$result) | Out-Null
    return $result
}

function Compare-WinProbeRegion {
    <#
    .SYNOPSIS
        LockBits-based pixel-band comparison of two PrintWindow frames (C# in the AeroWinProbe
        namespace, no per-pixel PowerShell loop). Used by later plans for "title bar looks
        exactly as before" (D-02) and "no white strip in the top rows" checks.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$Before,
        [Parameter(Mandatory = $true)][string]$After,
        [Parameter(Mandatory = $true)][int]$Top,
        [Parameter(Mandatory = $true)][int]$Bottom,
        [int]$Left = 0,
        [int]$Right = 0,
        [int]$ChannelTolerance = 0
    )
    $raw = [AeroWinProbe.RegionCompare]::Compare($Before, $After, $Top, $Bottom, $Left, $Right, $ChannelTolerance)
    [pscustomobject]@{
        Comparable      = [bool]$raw['Comparable']
        DiffPixels      = [int]$raw['DiffPixels']
        MaxChannelDelta = [int]$raw['MaxChannelDelta']
    }
}

function Invoke-WinProbeV11 {
    <#
    .SYNOPSIS
        Evaluates the VER-11 check set against a live window and returns Id/Result/Expected/Observed
        objects, plus prints one `V11 <Id> <PASS|FAIL|SKIP> expected=... observed=...` line per
        check and a `V11 SUMMARY pass=<n> fail=<n> skip=<n>` line.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][IntPtr]$Hwnd,
        [string]$Layout = 'main',
        [string]$LogPath,
        [string]$Label = 'main',
        [switch]$SkipMaximize
    )

    $layoutDef = $WinProbeLayout[$Layout]
    $info = Get-WinProbeWindowInfo -Hwnd $Hwnd
    $clientWidthDp = ($info.ClientRect.Right - $info.ClientRect.Left) / $info.Scale
    $clientHeightDp = ($info.ClientRect.Bottom - $info.ClientRect.Top) / $info.Scale
    $points = Get-WinProbeNamedPoints -Layout $layoutDef -ClientWidthDp $clientWidthDp -ClientHeightDp $clientHeightDp -Scale $info.Scale

    function New-V11Result([string]$Id, [string]$Result, [string]$Expected, [string]$Observed) {
        [pscustomobject]@{ Id = $Id; Result = $Result; Expected = $Expected; Observed = $Observed }
    }

    $results = New-Object System.Collections.Generic.List[object]

    function New-V11HitResult([string]$Id, [string]$PointName, [int]$Expected) {
        $pt = $points[$PointName]
        $hit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $pt.x -ClientY $pt.y
        $r = if ($hit.Code -eq $Expected) { 'PASS' } else { 'FAIL' }
        $results.Add((New-V11Result $Id $r "$Expected" "$($hit.Code) ($($hit.CodeName)) chain=$($hit.Chain)"))
        return $hit
    }

    if ($Layout -eq 'narrow') {
        # Narrow-window VER-11 set (SHW-17 fixture, 300x480dp): caption/button classification,
        # the leading badge and the "Вернуть" overlay boundaries, resize edges, and the app's
        # own minimum size (D-01: 260x200, NOT the library's 320x240 default floor). Maximize
        # checks are not part of this set. The marked-element boundary is expected to FAIL until
        # the element is marked interactive on the native path (API-02) — that FAIL is the RED.
        New-V11HitResult 'V11-N-HT-CAPTION' 'caption' 2 | Out-Null
        New-V11HitResult 'V11-N-HT-MAX' 'max' 9 | Out-Null

        $leadingHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['leading'].x -ClientY $points['leading'].y
        $captionRightHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['captionRightOfLeading'].x -ClientY $points['captionRightOfLeading'].y
        $leadingBoundaryPass = ($leadingHit.Code -eq 1) -and ($captionRightHit.Code -eq 2)
        $results.Add((New-V11Result 'V11-N-HT-LEADING-BOUNDARY' $(if ($leadingBoundaryPass) { 'PASS' } else { 'FAIL' }) 'leading=1,captionRightOfLeading=2' "leading=$($leadingHit.Code),captionRightOfLeading=$($captionRightHit.Code)"))

        $markedHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['marked'].x -ClientY $points['marked'].y
        $captionLeftHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['captionLeftOfMarked'].x -ClientY $points['captionLeftOfMarked'].y
        $markedBoundaryPass = ($markedHit.Code -eq 1) -and ($captionLeftHit.Code -eq 2)
        $results.Add((New-V11Result 'V11-N-HT-MARKED-BOUNDARY' $(if ($markedBoundaryPass) { 'PASS' } else { 'FAIL' }) 'marked=1,captionLeftOfMarked=2' "marked=$($markedHit.Code),captionLeftOfMarked=$($captionLeftHit.Code)"))

        New-V11HitResult 'V11-N-HT-EDGE-L' 'edgeLeft' 10 | Out-Null
        New-V11HitResult 'V11-N-HT-EDGE-R' 'edgeRight' 11 | Out-Null
        New-V11HitResult 'V11-N-HT-EDGE-B' 'edgeBottom' 15 | Out-Null

        $narrowMinSize = Invoke-WinProbeMinSize -Hwnd $Hwnd
        $expectedMinW = [Math]::Round($layoutDef.minWidthDp * $info.Scale)
        $expectedMinH = [Math]::Round($layoutDef.minHeightDp * $info.Scale)
        $defaultMinW = [Math]::Round(320 * $info.Scale)
        $defaultMinH = [Math]::Round(240 * $info.Scale)
        # Three-way discrimination: app minimum honored lands at 260x200 (PASS); the library's
        # default floor lands at 320x240 (FAIL, D-01 violated); no floor at all stays 50x50 (FAIL).
        $narrowMinSizePass = ($narrowMinSize.ObtainedWidth -ge $expectedMinW) -and ($narrowMinSize.ObtainedHeight -ge $expectedMinH) `
            -and ($narrowMinSize.ObtainedWidth -lt $defaultMinW) -and ($narrowMinSize.ObtainedHeight -lt $defaultMinH)
        $results.Add((New-V11Result 'V11-N-MINSIZE' $(if ($narrowMinSizePass) { 'PASS' } else { 'FAIL' }) ">=${expectedMinW}x${expectedMinH} and <${defaultMinW}x${defaultMinH}" "$($narrowMinSize.ObtainedWidth)x$($narrowMinSize.ObtainedHeight)"))

        # Write-Host, not Write-Output: see the main path below for why the success stream must
        # carry only the result objects themselves.
        foreach ($r in $results) {
            Write-Host "V11 $($r.Id) $($r.Result) expected=$($r.Expected) observed=$($r.Observed)"
        }
        $passCount = @($results | Where-Object { $_.Result -eq 'PASS' }).Count
        $failCount = @($results | Where-Object { $_.Result -eq 'FAIL' }).Count
        $skipCount = @($results | Where-Object { $_.Result -eq 'SKIP' }).Count
        Write-Host "V11 SUMMARY narrow pass=$passCount fail=$failCount skip=$skipCount"

        return $results
    }

    # V11-STYLE
    $requiredStyles = @('WS_CAPTION', 'WS_SYSMENU', 'WS_THICKFRAME', 'WS_MINIMIZEBOX', 'WS_MAXIMIZEBOX')
    $missing = @($requiredStyles | Where-Object { $info.StyleNames -notcontains $_ })
    $styleResult = if ($missing.Count -eq 0) { 'PASS' } else { 'FAIL' }
    $results.Add((New-V11Result 'V11-STYLE' $styleResult ($requiredStyles -join ',') ($info.StyleNames -join ',')))

    function Add-HitCheck([string]$Id, [string]$PointName, [int]$Expected) {
        $pt = $points[$PointName]
        $hit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $pt.x -ClientY $pt.y
        $r = if ($hit.Code -eq $Expected) { 'PASS' } else { 'FAIL' }
        $results.Add((New-V11Result $Id $r "$Expected" "$($hit.Code) ($($hit.CodeName)) chain=$($hit.Chain)"))
        return $hit
    }

    Add-HitCheck 'V11-HT-CAPTION' 'caption' 2 | Out-Null
    Add-HitCheck 'V11-HT-MAX' 'max' 9 | Out-Null

    $minHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['min'].x -ClientY $points['min'].y
    $captionLeftHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['captionLeftOfMin'].x -ClientY $points['captionLeftOfMin'].y
    $minBoundaryPass = ($minHit.Code -eq 1) -and ($captionLeftHit.Code -eq 2)
    $results.Add((New-V11Result 'V11-HT-MIN-BOUNDARY' $(if ($minBoundaryPass) { 'PASS' } else { 'FAIL' }) 'min=1,captionLeftOfMin=2' "min=$($minHit.Code),captionLeftOfMin=$($captionLeftHit.Code)"))

    $closeHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['close'].x -ClientY $points['close'].y
    $maxHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['max'].x -ClientY $points['max'].y
    $closeBoundaryPass = ($closeHit.Code -eq 1) -and ($maxHit.Code -eq 9)
    $results.Add((New-V11Result 'V11-HT-CLOSE-BOUNDARY' $(if ($closeBoundaryPass) { 'PASS' } else { 'FAIL' }) 'close=1,max=9' "close=$($closeHit.Code),max=$($maxHit.Code)"))

    Add-HitCheck 'V11-HT-EDGE-L' 'edgeLeft' 10 | Out-Null
    Add-HitCheck 'V11-HT-EDGE-R' 'edgeRight' 11 | Out-Null
    Add-HitCheck 'V11-HT-EDGE-T' 'edgeTop' 12 | Out-Null
    Add-HitCheck 'V11-HT-EDGE-B' 'edgeBottom' 15 | Out-Null
    Add-HitCheck 'V11-HT-CORNER-TL' 'cornerTL' 13 | Out-Null
    Add-HitCheck 'V11-HT-CORNER-TR' 'cornerTR' 14 | Out-Null
    Add-HitCheck 'V11-HT-CORNER-BL' 'cornerBL' 16 | Out-Null
    Add-HitCheck 'V11-HT-CORNER-BR' 'cornerBR' 17 | Out-Null

    $clientHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['client'].x -ClientY $points['client'].y
    $edgeLeftHit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $points['edgeLeft'].x -ClientY $points['edgeLeft'].y
    $clientBoundaryPass = ($clientHit.Code -eq 1) -and ($edgeLeftHit.Code -eq 10)
    $results.Add((New-V11Result 'V11-HT-CLIENT-BOUNDARY' $(if ($clientBoundaryPass) { 'PASS' } else { 'FAIL' }) 'client=1,edgeLeft=10' "client=$($clientHit.Code),edgeLeft=$($edgeLeftHit.Code)"))

    # Read taskbar auto-hide state before maximizing: V11-MAX-WORKAREA's tolerance is
    # edge-dependent (an auto-hide edge must be LEFT UNCOVERED by 1-4px so the reveal-on-hover
    # strip still works; a non-auto-hide edge must be flush, ~0px). A single blanket "delta <=
    # 4px on every edge" tolerance is not a guard here — on this machine's auto-hide-bottom
    # monitor, the pre-phase window already matches rcWork exactly on all four edges (CMP's own
    # WM_SIZE-driven placement sync fills the full monitor, deltas=0,0,0,0), which would make
    # a blanket check PASS on unmodified code. Splitting the tolerance by edge is what actually
    # distinguishes "native-computed inset" from "old code happens to fill the monitor".
    $taskbar = Get-WinProbeTaskbar -Hwnd $Hwnd

    if ($SkipMaximize) {
        $results.Add((New-V11Result 'V11-HT-MAXIMIZED-TOP' 'SKIP' 'edgeTop=2 while maximized' '-SkipMaximize'))
        $results.Add((New-V11Result 'V11-MAX-WORKAREA' 'SKIP' 'maximized client == rcWork (inset 1-4px on auto-hide edges, ~0px elsewhere)' '-SkipMaximize'))
        $results.Add((New-V11Result 'V11-AUTOHIDE-EDGE' 'SKIP' 'auto-hide edges uncovered by >=1px' '-SkipMaximize'))
    }
    else {
        $maxResult = Invoke-WinProbeMaximize -Hwnd $Hwnd -LogPath $LogPath -Label $Label
        $maxTopCode = $maxResult.EdgeTopHitWhileMaximized.Code
        $results.Add((New-V11Result 'V11-HT-MAXIMIZED-TOP' $(if ($maxTopCode -eq 2) { 'PASS' } else { 'FAIL' }) '2' "$maxTopCode"))

        # WIN-01: the CLIENT rect (screen coords, ClientToScreen) is what must match the work
        # area — WM_NCCALCSIZE's whole job. GetWindowRect (Invoke-WinProbeMaximize's separately
        # recorded WindowRectMaximized) keeps its pre-existing sizing-border overhang by design
        # (WM_GETMINMAXINFO is untouched in this plan, Plan 11 territory) and is not the right
        # measurement for "does the visible content cover the work area".
        $wr = $maxResult.ClientRectMaximized
        $work = $maxResult.MonitorRcWork
        $deltas = [ordered]@{
            Left   = [Math]::Abs($wr.Left - $work.Left)
            Top    = [Math]::Abs($wr.Top - $work.Top)
            Right  = [Math]::Abs($wr.Right - $work.Right)
            Bottom = [Math]::Abs($wr.Bottom - $work.Bottom)
        }
        $edgeChecks = New-Object System.Collections.Generic.List[string]
        $withinTolerance = $true
        foreach ($edgeName in $deltas.Keys) {
            $delta = $deltas[$edgeName]
            $isAutoHide = $taskbar.AutoHideEdges -contains $edgeName
            $edgeOk = if ($isAutoHide) { ($delta -ge 1) -and ($delta -le 4) } else { $delta -le 1 }
            if (-not $edgeOk) { $withinTolerance = $false }
            $edgeChecks.Add("$edgeName=$delta$(if ($isAutoHide) { '(autoHide,need 1-4)' } else { '(need <=1)' })=$(if ($edgeOk) { 'ok' } else { 'BAD' })")
        }
        $results.Add((New-V11Result 'V11-MAX-WORKAREA' $(if ($withinTolerance) { 'PASS' } else { 'FAIL' }) "rcWork=$($work.Left),$($work.Top),$($work.Right),$($work.Bottom) autoHideEdges=$($taskbar.AutoHideEdges -join ',')" "client=$($wr.Left),$($wr.Top),$($wr.Right),$($wr.Bottom) window=$($maxResult.WindowRectMaximized.Left),$($maxResult.WindowRectMaximized.Top),$($maxResult.WindowRectMaximized.Right),$($maxResult.WindowRectMaximized.Bottom) $($edgeChecks -join ' ') foregroundTaken=$($maxResult.ForegroundTaken)"))

        # WIN-01 (22-05): the native inset code now exists, so this reuses the same per-edge
        # deltas V11-MAX-WORKAREA already measured — every detected auto-hide edge must be left
        # uncovered by 1-4px (Windows Terminal's 2px, plus slack), not the placeholder "detection
        # only, always FAIL" this checked against pre-05 code.
        if ($taskbar.AutoHideEdges.Count -eq 0) {
            $results.Add((New-V11Result 'V11-AUTOHIDE-EDGE' 'SKIP' 'at least one auto-hide edge' 'no auto-hide edge on this monitor'))
        }
        else {
            $autoHideDetail = New-Object System.Collections.Generic.List[string]
            $autoHideOk = $true
            foreach ($edgeName in $taskbar.AutoHideEdges) {
                $delta = $deltas[$edgeName]
                $edgeOk = ($delta -ge 1) -and ($delta -le 4)
                if (-not $edgeOk) { $autoHideOk = $false }
                $autoHideDetail.Add("$edgeName=$delta")
            }
            $results.Add((New-V11Result 'V11-AUTOHIDE-EDGE' $(if ($autoHideOk) { 'PASS' } else { 'FAIL' }) 'auto-hide edges uncovered by >=1px' "detected edges=$($taskbar.AutoHideEdges -join ',') deltas=$($autoHideDetail -join ',')"))
        }
    }

    $minSize = Invoke-WinProbeMinSize -Hwnd $Hwnd
    $expectedMinW = [Math]::Round(320 * $info.Scale)
    $expectedMinH = [Math]::Round(240 * $info.Scale)
    $minSizePass = ($minSize.ObtainedWidth -ge $expectedMinW) -and ($minSize.ObtainedHeight -ge $expectedMinH)
    $results.Add((New-V11Result 'V11-MINSIZE' $(if ($minSizePass) { 'PASS' } else { 'FAIL' }) ">=${expectedMinW}x${expectedMinH}" "$($minSize.ObtainedWidth)x$($minSize.ObtainedHeight)"))

    # Write-Host, not Write-Output: this function's return value is $results itself, and
    # anything written to the success/output stream here would flatten into the caller's
    # captured $v11Results alongside the real result objects (a well-known PowerShell function
    # gotcha), corrupting Invoke-WinProbe.ps1's own pass/fail counting downstream.
    foreach ($r in $results) {
        Write-Host "V11 $($r.Id) $($r.Result) expected=$($r.Expected) observed=$($r.Observed)"
    }
    $passCount = @($results | Where-Object { $_.Result -eq 'PASS' }).Count
    $failCount = @($results | Where-Object { $_.Result -eq 'FAIL' }).Count
    $skipCount = @($results | Where-Object { $_.Result -eq 'SKIP' }).Count
    Write-Host "V11 SUMMARY pass=$passCount fail=$failCount skip=$skipCount"

    return $results
}
