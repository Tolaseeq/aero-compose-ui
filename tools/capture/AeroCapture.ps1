<#
.SYNOPSIS
    Non-interfering PrintWindow capture library for the aero-compose-ui showcase (BASE-02).
.DESCRIPTION
    Dot-source this file to load window discovery, PrintWindow-based capture, LockBits pixel
    comparison primitives, and a cursor/foreground-window probe. Every function here avoids
    delivering synthetic input to a live window and avoids reading screen contents outside the
    target window's own content.
.NOTES
    PowerShell 5.1 only. The embedded C# is compiled by the .NET Framework CodeDom compiler and
    must stay C# 5 compatible.
#>

#Requires -Version 5.1
Set-StrictMode -Version 2

if (-not ('AeroCaptureNative' -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing, System.Windows.Forms -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
using System.Text;
using System.Windows.Forms;

public struct RECT
{
    public int Left;
    public int Top;
    public int Right;
    public int Bottom;
}

public struct POINT
{
    public int X;
    public int Y;
}

public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

public class AeroCaptureNative
{
    public const int SW_SHOWNOACTIVATE = 4;
    public const int SW_SHOWMINNOACTIVE = 7;
    public static readonly IntPtr HWND_BOTTOM = (IntPtr)1;
    public const uint SWP_NOSIZE_NOMOVE_NOACTIVATE = 0x13;
    public const uint PW_RENDERFULLCONTENT = 2;
    public const uint GA_ROOT = 2;

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll")]
    public static extern int GetWindowTextLength(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll")]
    public static extern bool IsIconic(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

    [DllImport("user32.dll")]
    public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [DllImport("user32.dll")]
    public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdcBlt, uint nFlags);

    [DllImport("user32.dll")]
    public static extern bool GetCursorPos(out POINT lpPoint);

    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    public static extern uint GetDpiForWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern IntPtr WindowFromPoint(POINT Point);

    [DllImport("user32.dll")]
    public static extern IntPtr GetAncestor(IntPtr hWnd, uint gaFlags);

    [DllImport("user32.dll")]
    public static extern IntPtr SetThreadDpiAwarenessContext(IntPtr dpiContext);

    public static List<IntPtr> FindTopLevelWindows(string exactTitle)
    {
        List<IntPtr> found = new List<IntPtr>();
        EnumWindowsProc callback = delegate(IntPtr hWnd, IntPtr lParam)
        {
            if (!IsWindowVisible(hWnd)) return true;
            int length = GetWindowTextLength(hWnd);
            if (length == 0) return true;
            StringBuilder sb = new StringBuilder(length + 1);
            GetWindowText(hWnd, sb, sb.Capacity);
            if (string.Equals(sb.ToString(), exactTitle, StringComparison.Ordinal))
            {
                found.Add(hWnd);
            }
            return true;
        };
        EnumWindows(callback, IntPtr.Zero);
        return found;
    }

    public static Bitmap Capture(IntPtr hWnd)
    {
        RECT rect;
        if (!GetWindowRect(hWnd, out rect))
        {
            throw new InvalidOperationException("GetWindowRect failed");
        }
        int width = rect.Right - rect.Left;
        int height = rect.Bottom - rect.Top;
        if (width <= 0 || height <= 0)
        {
            throw new InvalidOperationException("Window has non-positive size");
        }
        Bitmap bitmap = new Bitmap(width, height, PixelFormat.Format32bppArgb);
        using (Graphics graphics = Graphics.FromImage(bitmap))
        {
            IntPtr hdc = graphics.GetHdc();
            bool ok;
            try
            {
                ok = PrintWindow(hWnd, hdc, PW_RENDERFULLCONTENT);
            }
            finally
            {
                graphics.ReleaseHdc(hdc);
            }
            if (!ok)
            {
                bitmap.Dispose();
                throw new InvalidOperationException("PrintWindow failed");
            }
        }
        return bitmap;
    }
}

public class AeroPixels
{
    public static int CountDiff(Bitmap a, Bitmap b)
    {
        if (a.Width != b.Width || a.Height != b.Height) return -1;
        BitmapData da = a.LockBits(new Rectangle(0, 0, a.Width, a.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        BitmapData db = b.LockBits(new Rectangle(0, 0, b.Width, b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        int count = 0;
        try
        {
            int stride = da.Stride;
            byte[] bufA = new byte[stride * a.Height];
            byte[] bufB = new byte[stride * b.Height];
            Marshal.Copy(da.Scan0, bufA, 0, bufA.Length);
            Marshal.Copy(db.Scan0, bufB, 0, bufB.Length);
            for (int y = 0; y < a.Height; y++)
            {
                int rowStart = y * stride;
                for (int x = 0; x < a.Width; x++)
                {
                    int idx = rowStart + x * 4;
                    if (bufA[idx] != bufB[idx] || bufA[idx + 1] != bufB[idx + 1] ||
                        bufA[idx + 2] != bufB[idx + 2] || bufA[idx + 3] != bufB[idx + 3])
                    {
                        count++;
                    }
                }
            }
        }
        finally
        {
            a.UnlockBits(da);
            b.UnlockBits(db);
        }
        return count;
    }

    public static List<int[]> DiffCells(Bitmap a, Bitmap b, int cell)
    {
        List<int[]> cells = new List<int[]>();
        if (a.Width != b.Width || a.Height != b.Height) return cells;
        BitmapData da = a.LockBits(new Rectangle(0, 0, a.Width, a.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        BitmapData db = b.LockBits(new Rectangle(0, 0, b.Width, b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        try
        {
            int stride = da.Stride;
            byte[] bufA = new byte[stride * a.Height];
            byte[] bufB = new byte[stride * b.Height];
            Marshal.Copy(da.Scan0, bufA, 0, bufA.Length);
            Marshal.Copy(db.Scan0, bufB, 0, bufB.Length);
            for (int cy = 0; cy * cell < a.Height; cy++)
            {
                for (int cx = 0; cx * cell < a.Width; cx++)
                {
                    int diffPx = 0;
                    int yStart = cy * cell;
                    int yEnd = Math.Min(yStart + cell, a.Height);
                    int xStart = cx * cell;
                    int xEnd = Math.Min(xStart + cell, a.Width);
                    for (int y = yStart; y < yEnd; y++)
                    {
                        int rowStart = y * stride;
                        for (int x = xStart; x < xEnd; x++)
                        {
                            int idx = rowStart + x * 4;
                            if (bufA[idx] != bufB[idx] || bufA[idx + 1] != bufB[idx + 1] ||
                                bufA[idx + 2] != bufB[idx + 2] || bufA[idx + 3] != bufB[idx + 3])
                            {
                                diffPx++;
                            }
                        }
                    }
                    if (diffPx > 0)
                    {
                        cells.Add(new int[] { cx, cy, diffPx });
                    }
                }
            }
        }
        finally
        {
            a.UnlockBits(da);
            b.UnlockBits(db);
        }
        return cells;
    }

    public static int GetArgb(Bitmap b, int x, int y)
    {
        return b.GetPixel(x, y).ToArgb();
    }

    public static bool HasColorBlock(Bitmap b, int argb, int size)
    {
        BitmapData d = b.LockBits(new Rectangle(0, 0, b.Width, b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        try
        {
            int stride = d.Stride;
            byte[] buf = new byte[stride * b.Height];
            Marshal.Copy(d.Scan0, buf, 0, buf.Length);
            Color target = Color.FromArgb(argb);
            for (int y = 0; y + size <= b.Height; y++)
            {
                for (int x = 0; x + size <= b.Width; x++)
                {
                    bool allMatch = true;
                    for (int dy = 0; dy < size && allMatch; dy++)
                    {
                        int rowStart = (y + dy) * stride;
                        for (int dx = 0; dx < size; dx++)
                        {
                            int idx = rowStart + (x + dx) * 4;
                            if (buf[idx] != target.B || buf[idx + 1] != target.G ||
                                buf[idx + 2] != target.R || buf[idx + 3] != target.A)
                            {
                                allMatch = false;
                                break;
                            }
                        }
                    }
                    if (allMatch)
                    {
                        return true;
                    }
                }
            }
        }
        finally
        {
            b.UnlockBits(d);
        }
        return false;
    }

    public static int MaxChannelDelta(Bitmap a, Bitmap b)
    {
        if (a.Width != b.Width || a.Height != b.Height) return -1;
        BitmapData da = a.LockBits(new Rectangle(0, 0, a.Width, a.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        BitmapData db = b.LockBits(new Rectangle(0, 0, b.Width, b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        int maxDelta = 0;
        try
        {
            int stride = da.Stride;
            byte[] bufA = new byte[stride * a.Height];
            byte[] bufB = new byte[stride * b.Height];
            Marshal.Copy(da.Scan0, bufA, 0, bufA.Length);
            Marshal.Copy(db.Scan0, bufB, 0, bufB.Length);
            for (int i = 0; i < bufA.Length; i++)
            {
                int delta = Math.Abs(bufA[i] - bufB[i]);
                if (delta > maxDelta) maxDelta = delta;
            }
        }
        finally
        {
            a.UnlockBits(da);
            b.UnlockBits(db);
        }
        return maxDelta;
    }
}

public class NoActivateForm : Form
{
    protected override bool ShowWithoutActivation
    {
        get { return true; }
    }

    protected override CreateParams CreateParams
    {
        get
        {
            CreateParams cp = base.CreateParams;
            cp.ExStyle |= 0x08000000; // WS_EX_NOACTIVATE
            cp.ExStyle |= 0x00000080; // WS_EX_TOOLWINDOW
            return cp;
        }
    }
}
"@
}

function Initialize-AeroDpiAwareness {
    <#
    .SYNOPSIS
        Sets per-monitor-v2 DPI awareness on this thread so window rectangles come back in
        physical pixels.
    #>
    [CmdletBinding()]
    param()
    [AeroCaptureNative]::SetThreadDpiAwarenessContext([IntPtr]::new(-4)) | Out-Null
}

function Get-AeroInputState {
    <#
    .SYNOPSIS
        Reports the current cursor position and the foreground window handle.
    .DESCRIPTION
        Used to prove a capture pass never moves the cursor or changes the foreground window
        (T-21-02: this function never reads or returns any window title).
    #>
    [CmdletBinding()]
    param()
    $point = New-Object POINT
    [AeroCaptureNative]::GetCursorPos([ref]$point) | Out-Null
    $foreground = [AeroCaptureNative]::GetForegroundWindow()
    [pscustomobject]@{
        CursorX        = $point.X
        CursorY        = $point.Y
        ForegroundHwnd = [int64]$foreground
    }
}

function Find-AeroShowcaseWindow {
    <#
    .SYNOPSIS
        Polls for a single top-level window with the given exact title.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$Title,
        [int]$TimeoutSec = 120
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $candidates = [AeroCaptureNative]::FindTopLevelWindows($Title)
        if ($candidates.Count -gt 1) {
            throw "Find-AeroShowcaseWindow: more than one window titled '$Title'"
        }
        if ($candidates.Count -eq 1) {
            return $candidates[0]
        }
        Start-Sleep -Milliseconds 250
    }
    throw "Find-AeroShowcaseWindow: no window titled '$Title' appeared within $TimeoutSec s"
}

function Invoke-AeroWindowCapture {
    <#
    .SYNOPSIS
        Captures a window's own content via PrintWindow, verifies it against the app's reported
        background, and proves the capture never moved the cursor or changed the foreground window.
    .DESCRIPTION
        Restores a minimized window with SW_SHOWNOACTIVATE (never activating it), keeps it at the
        bottom of the z-order, captures with PrintWindow(hwnd, hdc, 2), and samples two right-margin
        pixels against the caller-supplied expected background (forcing alpha to FF before compare).
        If the input state (cursor position + foreground window) differs before/after, the whole
        capture is retried up to twice more before this throws NON-INTERFERENCE VIOLATION.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][IntPtr]$Hwnd,
        [Parameter(Mandatory = $true)][string]$OutFile,
        [Parameter(Mandatory = $true)][string]$ExpectedBackgroundArgb,
        [switch]$SkipSanity
    )

    $wasIconic = [bool][AeroCaptureNative]::IsIconic($Hwnd)
    if ($wasIconic) {
        [AeroCaptureNative]::ShowWindow($Hwnd, [AeroCaptureNative]::SW_SHOWNOACTIVATE) | Out-Null
        Start-Sleep -Milliseconds 400
    }

    $hex = $ExpectedBackgroundArgb.Trim()
    if ($hex.Length -ne 8) {
        throw "Invoke-AeroWindowCapture: ExpectedBackgroundArgb must be 8 hex digits (AARRGGBB), got '$ExpectedBackgroundArgb'"
    }
    $expR = [Convert]::ToInt32($hex.Substring(2, 2), 16)
    $expG = [Convert]::ToInt32($hex.Substring(4, 2), 16)
    $expB = [Convert]::ToInt32($hex.Substring(6, 2), 16)
    $expectedOpaque = [System.Drawing.Color]::FromArgb(255, $expR, $expG, $expB).ToArgb()

    $dpi = [AeroCaptureNative]::GetDpiForWindow($Hwnd)
    $scale = $dpi / 96.0

    $bitmap = $null
    $externalActivity = $false
    $captured = $false

    for ($interferenceAttempt = 1; $interferenceAttempt -le 3; $interferenceAttempt++) {
        $before = Get-AeroInputState

        [AeroCaptureNative]::SetWindowPos($Hwnd, [AeroCaptureNative]::HWND_BOTTOM, 0, 0, 0, 0, [AeroCaptureNative]::SWP_NOSIZE_NOMOVE_NOACTIVATE) | Out-Null

        $sanityAttempt = 0
        $sanityOk = $SkipSanity.IsPresent
        do {
            $sanityAttempt++
            if ($bitmap) { $bitmap.Dispose() }
            $bitmap = [AeroCaptureNative]::Capture($Hwnd)

            if (-not $SkipSanity) {
                $rect = New-Object RECT
                [AeroCaptureNative]::GetWindowRect($Hwnd, [ref]$rect) | Out-Null
                $w = $rect.Right - $rect.Left
                $h = $rect.Bottom - $rect.Top
                $sampleX = [Math]::Max(0, [int]($w - (16 * $scale)))
                $sampleY1 = [int]($h / 2)
                $sampleY2 = [Math]::Max(0, [int]($h - (16 * $scale)))

                $argb1 = [AeroPixels]::GetArgb($bitmap, $sampleX, $sampleY1)
                $argb2 = [AeroPixels]::GetArgb($bitmap, $sampleX, $sampleY2)
                $c1 = [System.Drawing.Color]::FromArgb($argb1)
                $c2 = [System.Drawing.Color]::FromArgb($argb2)
                $opaque1 = [System.Drawing.Color]::FromArgb(255, $c1.R, $c1.G, $c1.B).ToArgb()
                $opaque2 = [System.Drawing.Color]::FromArgb(255, $c2.R, $c2.G, $c2.B).ToArgb()

                $sanityOk = ($opaque1 -eq $expectedOpaque) -and ($opaque2 -eq $expectedOpaque)
                if (-not $sanityOk -and $sanityAttempt -lt 2) {
                    Start-Sleep -Milliseconds 500
                }
            }
        } while (-not $sanityOk -and $sanityAttempt -lt 2)

        if (-not $sanityOk) {
            $bitmap.Dispose()
            throw "SANITY FAIL: $OutFile background did not match expected $ExpectedBackgroundArgb"
        }

        $after = Get-AeroInputState
        $unchanged = ($before.CursorX -eq $after.CursorX) -and ($before.CursorY -eq $after.CursorY) -and ($before.ForegroundHwnd -eq $after.ForegroundHwnd)

        if ($unchanged) {
            $captured = $true
            $externalActivity = ($interferenceAttempt -gt 1)
            break
        }

        $externalActivity = $true
    }

    if (-not $captured) {
        if ($bitmap) { $bitmap.Dispose() }
        throw "NON-INTERFERENCE VIOLATION: cursor/foreground state changed on all 3 capture attempts"
    }

    $directory = Split-Path -Path $OutFile -Parent
    if ($directory -and -not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    $bitmap.Save($OutFile, [System.Drawing.Imaging.ImageFormat]::Png)

    $width = $bitmap.Width
    $height = $bitmap.Height
    $bitmap.Dispose()

    [pscustomobject]@{
        File             = $OutFile
        Width            = $width
        Height           = $height
        Dpi              = $dpi
        WasIconic        = $wasIconic
        ExternalActivity = $externalActivity
    }
}

function Show-AeroOccluder {
    <#
    .SYNOPSIS
        Covers the target window with an opaque magenta, never-activating form in a hidden child
        process, so a capture can prove PrintWindow reads window content rather than screen pixels.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][IntPtr]$TargetHwnd,
        [Parameter(Mandatory = $true)][int]$Seconds
    )

    $scriptPath = $PSCommandPath
    $hwndValue = $TargetHwnd.ToInt64()
    $intervalMs = $Seconds * 1000
    $childScript = @"
. '$scriptPath'
Initialize-AeroDpiAwareness
`$target = [IntPtr]$hwndValue
`$rect = New-Object RECT
[AeroCaptureNative]::GetWindowRect(`$target, [ref]`$rect) | Out-Null
`$form = New-Object NoActivateForm
`$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::None
`$form.ShowInTaskbar = `$false
`$form.BackColor = [System.Drawing.Color]::FromArgb(255, 255, 0, 255)
`$form.StartPosition = [System.Windows.Forms.FormStartPosition]::Manual
`$form.Bounds = New-Object System.Drawing.Rectangle(`$rect.Left, `$rect.Top, (`$rect.Right - `$rect.Left), (`$rect.Bottom - `$rect.Top))
`$form.Show()
[AeroCaptureNative]::SetWindowPos(`$form.Handle, [AeroCaptureNative]::HWND_BOTTOM, 0, 0, 0, 0, [AeroCaptureNative]::SWP_NOSIZE_NOMOVE_NOACTIVATE) | Out-Null
[AeroCaptureNative]::SetWindowPos(`$target, [AeroCaptureNative]::HWND_BOTTOM, 0, 0, 0, 0, [AeroCaptureNative]::SWP_NOSIZE_NOMOVE_NOACTIVATE) | Out-Null
`$timer = New-Object System.Windows.Forms.Timer
`$timer.Interval = $intervalMs
`$timer.Add_Tick({
    `$timer.Stop()
    `$form.Close()
    [System.Windows.Forms.Application]::ExitThread()
})
`$timer.Start()
[System.Windows.Forms.Application]::Run()
"@

    $encoded = [Convert]::ToBase64String([System.Text.Encoding]::Unicode.GetBytes($childScript))
    $psExe = (Get-Process -Id $PID).Path
    $process = Start-Process -FilePath $psExe -ArgumentList @('-NoProfile', '-WindowStyle', 'Hidden', '-EncodedCommand', $encoded) -PassThru
    return $process
}

function Test-AeroOccluded {
    <#
    .SYNOPSIS
        Returns true when the top-level window at the target's own screen centre is not the
        target itself (i.e. something else now covers that point).
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)

    $rect = New-Object RECT
    [AeroCaptureNative]::GetWindowRect($Hwnd, [ref]$rect) | Out-Null
    $center = New-Object POINT
    $center.X = [int](($rect.Left + $rect.Right) / 2)
    $center.Y = [int](($rect.Top + $rect.Bottom) / 2)
    $atPoint = [AeroCaptureNative]::WindowFromPoint($center)
    $root = [AeroCaptureNative]::GetAncestor($atPoint, [AeroCaptureNative]::GA_ROOT)
    return ($root -ne $Hwnd)
}

function Stop-AeroProcessOfWindow {
    <#
    .SYNOPSIS
        Terminates the process that owns the given window handle, by PID — never by window message.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)

    [uint32]$processId = 0
    [AeroCaptureNative]::GetWindowThreadProcessId($Hwnd, [ref]$processId) | Out-Null
    if ($processId -gt 0) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

function Test-AeroPixelsSelf {
    <#
    .SYNOPSIS
        Falsifiability fixture proof for AeroPixels: proves CountDiff/DiffCells/HasColorBlock can
        both detect a real difference and correctly report "no difference".
    #>
    [CmdletBinding()]
    param()

    $white = New-Object System.Drawing.Bitmap(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphicsWhite = [System.Drawing.Graphics]::FromImage($white)
    $graphicsWhite.Clear([System.Drawing.Color]::White)
    $graphicsWhite.Dispose()

    $whiteCopy = [System.Drawing.Bitmap]($white.Clone())

    $diffZero = [AeroPixels]::CountDiff($white, $whiteCopy)
    if ($diffZero -ne 0) {
        throw "Test-AeroPixelsSelf: expected CountDiff 0 for identical bitmaps, got $diffZero"
    }

    $redBlock = [System.Drawing.Bitmap]($white.Clone())
    $graphicsRed = [System.Drawing.Graphics]::FromImage($redBlock)
    $redBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 0, 0))
    $graphicsRed.FillRectangle($redBrush, 20, 20, 10, 10)
    $graphicsRed.Dispose()
    $redBrush.Dispose()

    $diffBlock = [AeroPixels]::CountDiff($white, $redBlock)
    if ($diffBlock -ne 100) {
        throw "Test-AeroPixelsSelf: expected CountDiff 100 for a 10x10 block, got $diffBlock"
    }

    $cells = [AeroPixels]::DiffCells($white, $redBlock, 10)
    if ($cells.Count -eq 0) {
        throw "Test-AeroPixelsSelf: expected non-empty DiffCells for a 10x10 block"
    }

    $hasRedBlock = [AeroPixels]::HasColorBlock($redBlock, [System.Drawing.Color]::FromArgb(255, 255, 0, 0).ToArgb(), 10)
    if (-not $hasRedBlock) {
        throw "Test-AeroPixelsSelf: expected HasColorBlock to find the 10x10 red block"
    }

    $white.Dispose()
    $whiteCopy.Dispose()
    $redBlock.Dispose()

    Write-Output "AERO_PIXELS_SELFTEST PASS"
}
