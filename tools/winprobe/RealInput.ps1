<#
.SYNOPSIS
    Guarded SendInput driver for the VER-12 real-input sessions (aero-compose-ui v3.2.0).
.DESCRIPTION
    Dot-sourceable. Every function that can deliver real mouse/keyboard input takes a mandatory
    -Session parameter, and a session can only be created by New-AeroInputSession, which requires
    a non-empty -AuthorizedBy string. A -DryRun session logs every planned action and sends
    nothing -- SendInput, SetCursorPos and keyboard events are never reached on that path. Also
    invocable directly with -SelfTest, which builds a DryRun session, plans a drag and a key
    chord, and proves the real cursor never moved.
.NOTES
    PowerShell 5.1 only. The embedded C# is compiled by the .NET Framework CodeDom compiler and
    must stay C# 5 compatible. Absolute mouse coordinates are normalized over the virtual screen
    (SM_XVIRTUALSCREEN/SM_YVIRTUALSCREEN/SM_CXVIRTUALSCREEN/SM_CYVIRTUALSCREEN) under
    per-monitor-v2 DPI awareness, per Initialize-AeroDpiAwareness (tools/capture/AeroCapture.ps1).
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$SelfTest
)

Set-StrictMode -Version 2

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here '..\capture\AeroCapture.ps1')

if (-not ('AeroRealInput.Native' -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

namespace AeroRealInput
{
    [StructLayout(LayoutKind.Sequential)]
    public struct RiPoint
    {
        public int X;
        public int Y;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct CURSORINFO
    {
        public int cbSize;
        public int flags;
        public IntPtr hCursor;
        public RiPoint ptScreenPos;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct MOUSEINPUT
    {
        public int dx;
        public int dy;
        public uint mouseData;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Explicit)]
    public struct InputUnion
    {
        [FieldOffset(0)] public MOUSEINPUT mi;
        [FieldOffset(0)] public KEYBDINPUT ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT
    {
        public uint type;
        public InputUnion u;
    }

    public class Native
    {
        public const uint INPUT_MOUSE = 0;
        public const uint INPUT_KEYBOARD = 1;

        public const uint MOUSEEVENTF_MOVE = 0x0001;
        public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
        public const uint MOUSEEVENTF_LEFTUP = 0x0004;
        public const uint MOUSEEVENTF_ABSOLUTE = 0x8000;
        public const uint MOUSEEVENTF_VIRTUALDESK = 0x4000;

        public const uint KEYEVENTF_KEYUP = 0x0002;

        public const int SM_XVIRTUALSCREEN = 76;
        public const int SM_YVIRTUALSCREEN = 77;
        public const int SM_CXVIRTUALSCREEN = 78;
        public const int SM_CYVIRTUALSCREEN = 79;

        public const int CURSOR_SHOWING = 0x00000001;

        [DllImport("user32.dll", SetLastError = true)]
        public static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

        [DllImport("user32.dll")]
        public static extern int GetSystemMetrics(int nIndex);

        [DllImport("user32.dll")]
        public static extern bool GetCursorInfo(out CURSORINFO pci);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        public static extern IntPtr LoadCursorW(IntPtr hInstance, IntPtr lpCursorName);
    }
}
"@
}

# Known cursor IDs (LoadCursor(NULL, IDC_*)) this file can name via Get-AeroCursorShape.
$AeroCursorIdMap = [ordered]@{
    Arrow    = 32512
    SizeWE   = 32644
    SizeNS   = 32645
    SizeNWSE = 32642
    SizeNESW = 32643
}

# Key-name -> virtual-key-code table for Send-AeroKeyChord. Matches this plan's own
# <interfaces> block (VK_LWIN 0x5B, VK_SHIFT 0x10, VK_MENU 0x12, VK_ESCAPE 0x1B, VK_SPACE 0x20,
# VK_LEFT 0x25, VK_UP 0x26, VK_RIGHT 0x27, VK_DOWN 0x28, VK_RETURN 0x0D, VK_F4 0x73).
$AeroKeyNameToVk = [ordered]@{
    LWin   = 0x5B
    Shift  = 0x10
    Alt    = 0x12
    Escape = 0x1B
    Space  = 0x20
    Left   = 0x25
    Up     = 0x26
    Right  = 0x27
    Down   = 0x28
    Enter  = 0x0D
    F4     = 0x73
}

function New-AeroInputSession {
    <#
    .SYNOPSIS
        Creates an input-session object. Every function in this file that can send real input
        requires one, and requires a non-empty -AuthorizedBy (the calling session script passes
        the maintainer's own "ok" reply text plus a timestamp; this file never invents one).
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$AuthorizedBy,
        [switch]$DryRun
    )
    [pscustomobject]@{
        AuthorizedBy = $AuthorizedBy
        IsDryRun     = [bool]$DryRun
        CreatedAt    = Get-Date
        ActionLog    = New-Object System.Collections.Generic.List[string]
    }
}

function Add-AeroInputLog {
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][string]$Action
    )
    [void]$Session.ActionLog.Add($Action)
}

function Get-AeroVirtualScreenRect {
    [CmdletBinding()]
    param()
    [pscustomobject]@{
        X      = [AeroRealInput.Native]::GetSystemMetrics([AeroRealInput.Native]::SM_XVIRTUALSCREEN)
        Y      = [AeroRealInput.Native]::GetSystemMetrics([AeroRealInput.Native]::SM_YVIRTUALSCREEN)
        Width  = [AeroRealInput.Native]::GetSystemMetrics([AeroRealInput.Native]::SM_CXVIRTUALSCREEN)
        Height = [AeroRealInput.Native]::GetSystemMetrics([AeroRealInput.Native]::SM_CYVIRTUALSCREEN)
    }
}

function ConvertTo-AeroAbsoluteCoord {
    <#
    .SYNOPSIS
        Normalizes a physical-pixel screen point to the 0..65535 range SendInput's
        MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_VIRTUALDESK expects.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][int]$X, [Parameter(Mandatory = $true)][int]$Y)
    $vs = Get-AeroVirtualScreenRect
    $normX = [int][Math]::Round((($X - $vs.X) * 65535.0) / [Math]::Max(1, $vs.Width - 1))
    $normY = [int][Math]::Round((($Y - $vs.Y) * 65535.0) / [Math]::Max(1, $vs.Height - 1))
    [pscustomobject]@{ X = $normX; Y = $normY }
}

function Send-AeroRawMouseInput {
    <#
    .SYNOPSIS
        Sends exactly one SendInput mouse event. Never called outside a non-DryRun code path.
    #>
    [CmdletBinding()]
    param([int]$Dx = 0, [int]$Dy = 0, [Parameter(Mandatory = $true)][uint32]$Flags, [uint32]$MouseData = 0)
    $mi = New-Object 'AeroRealInput.MOUSEINPUT'
    $mi.dx = $Dx
    $mi.dy = $Dy
    $mi.mouseData = $MouseData
    $mi.dwFlags = $Flags
    $mi.time = 0
    $mi.dwExtraInfo = [IntPtr]::Zero

    $union = New-Object 'AeroRealInput.InputUnion'
    $union.mi = $mi

    $input = New-Object 'AeroRealInput.INPUT'
    $input.type = [AeroRealInput.Native]::INPUT_MOUSE
    $input.u = $union

    $arr = [AeroRealInput.INPUT[]]@($input)
    $size = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroRealInput.INPUT]))
    [AeroRealInput.Native]::SendInput(1, $arr, $size) | Out-Null
}

function Send-AeroRawKeyEvent {
    <#
    .SYNOPSIS
        Sends exactly one SendInput keyboard event (down or up). Never called outside a
        non-DryRun code path.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][uint16]$Vk, [switch]$KeyUp)
    $ki = New-Object 'AeroRealInput.KEYBDINPUT'
    $ki.wVk = $Vk
    $ki.wScan = 0
    $ki.dwFlags = if ($KeyUp) { [AeroRealInput.Native]::KEYEVENTF_KEYUP } else { 0 }
    $ki.time = 0
    $ki.dwExtraInfo = [IntPtr]::Zero

    $union = New-Object 'AeroRealInput.InputUnion'
    $union.ki = $ki

    $input = New-Object 'AeroRealInput.INPUT'
    $input.type = [AeroRealInput.Native]::INPUT_KEYBOARD
    $input.u = $union

    $arr = [AeroRealInput.INPUT[]]@($input)
    $size = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroRealInput.INPUT]))
    [AeroRealInput.Native]::SendInput(1, $arr, $size) | Out-Null
}

function Move-AeroCursor {
    <#
    .SYNOPSIS
        Absolute SendInput moves to (X, Y) in physical pixels, via several intermediate points so
        the OS sees real motion rather than a single teleport.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][int]$X,
        [Parameter(Mandatory = $true)][int]$Y,
        [int]$Steps = 12,
        [int]$DurationMs = 250
    )
    Add-AeroInputLog -Session $Session -Action "Move X=$X Y=$Y Steps=$Steps DurationMs=$DurationMs"
    if ($Session.IsDryRun) { return }

    $current = Get-AeroInputState
    $startX = $current.CursorX
    $startY = $current.CursorY
    $flags = [AeroRealInput.Native]::MOUSEEVENTF_MOVE -bor [AeroRealInput.Native]::MOUSEEVENTF_ABSOLUTE -bor [AeroRealInput.Native]::MOUSEEVENTF_VIRTUALDESK
    $stepDelay = [Math]::Max(1, [int]($DurationMs / [Math]::Max(1, $Steps)))
    for ($i = 1; $i -le $Steps; $i++) {
        $fx = [int]($startX + (($X - $startX) * $i / $Steps))
        $fy = [int]($startY + (($Y - $startY) * $i / $Steps))
        $abs = ConvertTo-AeroAbsoluteCoord -X $fx -Y $fy
        Send-AeroRawMouseInput -Dx $abs.X -Dy $abs.Y -Flags $flags
        Start-Sleep -Milliseconds $stepDelay
    }
}

function Invoke-AeroMouseButton {
    <#
    .SYNOPSIS
        Sends a left-button down or up event at the cursor's current position.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [switch]$Down,
        [switch]$Up
    )
    if ($Down.IsPresent -eq $Up.IsPresent) {
        throw 'Invoke-AeroMouseButton: specify exactly one of -Down or -Up'
    }
    $action = if ($Down) { 'Down' } else { 'Up' }
    Add-AeroInputLog -Session $Session -Action "MouseButton $action"
    if ($Session.IsDryRun) { return }
    $flag = if ($Down) { [AeroRealInput.Native]::MOUSEEVENTF_LEFTDOWN } else { [AeroRealInput.Native]::MOUSEEVENTF_LEFTUP }
    Send-AeroRawMouseInput -Flags $flag
}

function Invoke-AeroDrag {
    <#
    .SYNOPSIS
        Moves to the start point, presses the left button, moves to the end point over -Steps /
        -DurationMs, holds for -HoldMs, then releases. -WithKey Shift holds Shift for the whole
        drag (used for a constrained/duplicate-style drag elsewhere in the codebase's own Win32
        vocabulary; optional here).
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][int]$FromX,
        [Parameter(Mandatory = $true)][int]$FromY,
        [Parameter(Mandatory = $true)][int]$ToX,
        [Parameter(Mandatory = $true)][int]$ToY,
        [int]$Steps = 20,
        [int]$DurationMs = 700,
        [int]$HoldMs = 500,
        [ValidateSet('Shift')][string]$WithKey
    )
    Add-AeroInputLog -Session $Session -Action "Drag From=$FromX,$FromY To=$ToX,$ToY Steps=$Steps DurationMs=$DurationMs HoldMs=$HoldMs WithKey=$WithKey"

    Move-AeroCursor -Session $Session -X $FromX -Y $FromY -Steps 6 -DurationMs 150
    if ($WithKey -eq 'Shift') {
        Add-AeroInputLog -Session $Session -Action 'KeyDown Shift'
        if (-not $Session.IsDryRun) { Send-AeroRawKeyEvent -Vk $AeroKeyNameToVk['Shift'] }
    }
    Invoke-AeroMouseButton -Session $Session -Down
    Move-AeroCursor -Session $Session -X $ToX -Y $ToY -Steps $Steps -DurationMs $DurationMs
    if ($HoldMs -gt 0) {
        Add-AeroInputLog -Session $Session -Action "Hold ${HoldMs}ms"
        if (-not $Session.IsDryRun) { Start-Sleep -Milliseconds $HoldMs }
    }
    Invoke-AeroMouseButton -Session $Session -Up
    if ($WithKey -eq 'Shift') {
        Add-AeroInputLog -Session $Session -Action 'KeyUp Shift'
        if (-not $Session.IsDryRun) { Send-AeroRawKeyEvent -Vk $AeroKeyNameToVk['Shift'] -KeyUp }
    }
}

function Invoke-AeroClick {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][int]$X,
        [Parameter(Mandatory = $true)][int]$Y
    )
    Add-AeroInputLog -Session $Session -Action "Click X=$X Y=$Y"
    Move-AeroCursor -Session $Session -X $X -Y $Y -Steps 8 -DurationMs 150
    Invoke-AeroMouseButton -Session $Session -Down
    if (-not $Session.IsDryRun) { Start-Sleep -Milliseconds 60 }
    Invoke-AeroMouseButton -Session $Session -Up
}

function Invoke-AeroDoubleClick {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][int]$X,
        [Parameter(Mandatory = $true)][int]$Y
    )
    Add-AeroInputLog -Session $Session -Action "DoubleClick X=$X Y=$Y"
    Invoke-AeroClick -Session $Session -X $X -Y $Y
    if (-not $Session.IsDryRun) { Start-Sleep -Milliseconds 90 }
    Invoke-AeroMouseButton -Session $Session -Down
    if (-not $Session.IsDryRun) { Start-Sleep -Milliseconds 60 }
    Invoke-AeroMouseButton -Session $Session -Up
}

function Send-AeroKeyChord {
    <#
    .SYNOPSIS
        Presses the named keys in order, then releases them in reverse order.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][string[]]$Keys
    )
    $unknown = @($Keys | Where-Object { -not $AeroKeyNameToVk.Contains($_) })
    if ($unknown.Count -gt 0) {
        throw "Send-AeroKeyChord: unknown key name(s) $($unknown -join ',')"
    }
    Add-AeroInputLog -Session $Session -Action "KeyChord $($Keys -join '+')"
    if ($Session.IsDryRun) { return }

    foreach ($k in $Keys) {
        Send-AeroRawKeyEvent -Vk $AeroKeyNameToVk[$k]
        Start-Sleep -Milliseconds 30
    }
    $reversed = [System.Collections.Generic.List[string]]::new($Keys)
    $reversed.Reverse()
    foreach ($k in $reversed) {
        Send-AeroRawKeyEvent -Vk $AeroKeyNameToVk[$k] -KeyUp
        Start-Sleep -Milliseconds 30
    }
}

function Get-AeroCursorShape {
    <#
    .SYNOPSIS
        Reads the current cursor via GetCursorInfo and names it Arrow/SizeWE/SizeNS/SizeNWSE/
        SizeNESW/Hidden/Other by comparing hCursor against LoadCursor(NULL, IDC_*) handles.
    #>
    [CmdletBinding()]
    param()
    $ci = New-Object 'AeroRealInput.CURSORINFO'
    $ci.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroRealInput.CURSORINFO]))
    [AeroRealInput.Native]::GetCursorInfo([ref]$ci) | Out-Null
    if (($ci.flags -band [AeroRealInput.Native]::CURSOR_SHOWING) -eq 0) {
        return 'Hidden'
    }
    foreach ($name in $AeroCursorIdMap.Keys) {
        $handle = [AeroRealInput.Native]::LoadCursorW([IntPtr]::Zero, [IntPtr]$AeroCursorIdMap[$name])
        if ($handle -eq $ci.hCursor) { return $name }
    }
    return 'Other'
}

function Save-AeroCursor {
    <#
    .SYNOPSIS
        Reads the current cursor position (no input sent -- GetCursorPos only).
    #>
    [CmdletBinding()]
    param()
    $state = Get-AeroInputState
    [pscustomobject]@{ X = $state.CursorX; Y = $state.CursorY }
}

function Restore-AeroCursor {
    <#
    .SYNOPSIS
        Moves the cursor back to a point captured by Save-AeroCursor. This sends real input (a
        SendInput move), so it takes -Session like every other input function here.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)]$Saved
    )
    Add-AeroInputLog -Session $Session -Action "RestoreCursor X=$($Saved.X) Y=$($Saved.Y)"
    Move-AeroCursor -Session $Session -X $Saved.X -Y $Saved.Y -Steps 1 -DurationMs 1
}

function Test-AeroRealInputSelf {
    <#
    .SYNOPSIS
        Builds a DryRun session, plans a drag and a Win+Left chord, prints the plan, and proves
        the real cursor position is unchanged (nothing was sent).
    .NOTES
        Uses Write-Host, not Write-Output, for its diagnostic lines: this function's return value
        is a plain boolean, and anything written to the success/output stream here would flatten
        into the caller's captured `$ok = Test-AeroRealInputSelf` instead of reaching the console
        (the same gotcha WinProbe.ps1's Invoke-WinProbeV11 documents and avoids).
    #>
    [CmdletBinding()]
    param()
    Initialize-AeroDpiAwareness
    $before = Get-AeroInputState
    $session = New-AeroInputSession -AuthorizedBy 'RealInput-SelfTest' -DryRun
    Invoke-AeroDrag -Session $session -FromX 100 -FromY 100 -ToX 300 -ToY 300 -Steps 10 -DurationMs 200 -HoldMs 100
    Send-AeroKeyChord -Session $session -Keys @('LWin', 'Left')
    $after = Get-AeroInputState
    $unchanged = ($before.CursorX -eq $after.CursorX) -and ($before.CursorY -eq $after.CursorY)

    Write-Host 'REALINPUT_SELFTEST plan:'
    foreach ($a in $session.ActionLog) { Write-Host "  $a" }
    Write-Host "REALINPUT_SELFTEST cursor-before=$($before.CursorX),$($before.CursorY) cursor-after=$($after.CursorX),$($after.CursorY) unchanged=$unchanged"

    if (-not $unchanged) {
        Write-Host 'REALINPUT_SELFTEST FAIL: cursor moved during a DryRun session'
        return $false
    }
    Write-Host 'REALINPUT_SELFTEST PASS'
    return $true
}

if ($SelfTest) {
    $ok = Test-AeroRealInputSelf
    if (-not $ok) { exit 1 }
    exit 0
}
