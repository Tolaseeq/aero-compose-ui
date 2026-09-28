<#
.SYNOPSIS
    Session environment helpers for the full VER-12 real-input session (aero-compose-ui v3.2.0).
.DESCRIPTION
    Dot-sourceable. Read-only state readers (Get-AeroEnvState, Get-AeroFancyZonesZones,
    Test-AeroTaskbarRevealed) plus the authorized, recorded mutators the full session needs:
    Set-AeroTaskbarAutoHide, Install-AeroVirtualDisplay / Remove-AeroVirtualDisplay,
    Set-AeroDisplayScale, Install-AeroPowerToys / Remove-AeroPowerToys, Start-AeroFancyZones.
    Every mutator requires -Session from New-AeroInputSession (RealInput.ps1) and records
    before/after state; a -DryRun session logs the planned action and changes nothing.
    Installer/driver hashes are re-verified against the values recorded in
    .planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION-ENV.md before anything
    is run, and the driver install elevates via Start-Process -Verb RunAs so Windows shows the
    maintainer the UAC prompt they confirm. Driver and PowerToys removal timing stays with the
    teardown plan, not with these functions' callers' judgment.
.NOTES
    PowerShell 5.1 only. The embedded C# is compiled by the .NET Framework CodeDom compiler and
    must stay C# 5 compatible. Types live in the AeroSessionEnv namespace so they never clash
    with AeroWinProbe (WinProbe.ps1) or AeroRealInput (RealInput.ps1).
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$SelfTest
)

Set-StrictMode -Version 2

# Capture before the dot-source below: RealInput.ps1's own param([switch]$SelfTest) re-binds
# $SelfTest in this shared scope when dot-sourced without arguments, which would silently reset
# the value this script was invoked with.
$SelfTestRequested = [bool]$SelfTest

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'RealInput.ps1')

if (-not ('AeroSessionEnv.Native' -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

namespace AeroSessionEnv
{
    [StructLayout(LayoutKind.Sequential)]
    public struct EnvRect
    {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct MonitorInfo
    {
        public int cbSize;
        public EnvRect rcMonitor;
        public EnvRect rcWork;
        public uint dwFlags;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct AppBarData
    {
        public int cbSize;
        public IntPtr hWnd;
        public uint uCallbackMessage;
        public uint uEdge;
        public EnvRect rc;
        public IntPtr lParam;
    }

    public delegate bool MonitorEnumProc(IntPtr hMonitor, IntPtr hdcMonitor, ref EnvRect lprcMonitor, IntPtr dwData);

    public class Native
    {
        public const uint ABM_GETSTATE = 4;
        public const uint ABM_SETSTATE = 0xA;
        public const uint ABS_AUTOHIDE = 1;
        public const uint MONITORINFOF_PRIMARY = 1;
        public const uint MDT_EFFECTIVE_DPI = 0;

        [DllImport("user32.dll")]
        public static extern bool EnumDisplayMonitors(IntPtr hdc, IntPtr clipRect, MonitorEnumProc lpfnEnum, IntPtr dwData);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        public static extern bool GetMonitorInfoW(IntPtr hMonitor, ref MonitorInfo lpmi);

        [DllImport("shcore.dll")]
        public static extern int GetDpiForMonitor(IntPtr hMonitor, uint dpiType, out uint dpiX, out uint dpiY);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        public static extern IntPtr FindWindowW(string lpClassName, string lpWindowName);

        [DllImport("user32.dll")]
        public static extern bool GetWindowRect(IntPtr hWnd, out EnvRect lpRect);

        [DllImport("user32.dll")]
        public static extern bool IsWindowVisible(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        [DllImport("shell32.dll")]
        public static extern uint SHAppBarMessage(uint dwMessage, ref AppBarData pData);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        public static extern int GetClassNameW(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

        public static List<IntPtr> EnumMonitors()
        {
            List<IntPtr> found = new List<IntPtr>();
            MonitorEnumProc callback = delegate(IntPtr hMonitor, IntPtr hdcMonitor, ref EnvRect rect, IntPtr data)
            {
                found.Add(hMonitor);
                return true;
            };
            EnumDisplayMonitors(IntPtr.Zero, IntPtr.Zero, callback, IntPtr.Zero);
            return found;
        }

        public static string GetClassName(IntPtr hWnd)
        {
            StringBuilder sb = new StringBuilder(256);
            GetClassNameW(hWnd, sb, sb.Capacity);
            return sb.ToString();
        }
    }
}
"@
}

# SHA-256 values recorded by the Plan 03 vetting pass in 22-SESSION-ENV.md. Every install/remove
# re-verifies its files against these before running anything -- a mismatch aborts the function.
$AeroVddSha256 = @{
    'MttVDD.dll' = 'C9CA837F57A98FBD43BC416A7F535A95843626E7759EAF85CF0CD7CE334DBB05'
    'mttvdd.cat' = '08A0093FC9B2E32B287A6F8A77CA4DE0A31830D29FC33D2B13A918DC859468F6'
    'devcon.exe' = '77E87E7C4E23D6B9A4F5D86D15ED9EC61A16A242FB299C390AF6F179225BFA05'
}
$AeroVddInstallerSha256 = 'CC6FF00C23E0E62A45EFB7ED59FC83AB0E7F8B0FEAB3E4C76203F360E5DE2F0F'
$AeroPowerToysInstallerSha256 = 'D56FA7130FA68AFE553068C15A59A6B24C8DBCC9A0989A43EF0FC5A373230DE3'
$AeroPowerToysExe = Join-Path $env:LOCALAPPDATA 'Microsoft\PowerToys\PowerToys.exe'
$AeroFancyZonesSettings = Join-Path $env:LOCALAPPDATA 'Microsoft\PowerToys\FancyZones\zones-settings.json'

function Assert-AeroEnvSession {
    <#
    .SYNOPSIS
        Throws unless -Session is an AeroInputSession created by New-AeroInputSession -- the only
        gate through which any mutator in this file can run.
    #>
    param([Parameter(Mandatory = $true)]$Session)
    $looksRight = $Session -and
        $Session.PSObject.Properties['AuthorizedBy'] -and
        $Session.PSObject.Properties['IsDryRun'] -and
        $Session.PSObject.Properties['ActionLog']
    if (-not $looksRight) {
        throw 'Assert-AeroEnvSession: -Session must come from New-AeroInputSession (RealInput.ps1)'
    }
}

function ConvertTo-AeroEnvRectObject {
    param([Parameter(Mandatory = $true)]$Rect)
    [pscustomobject]@{
        Left   = [int]$Rect.Left
        Top    = [int]$Rect.Top
        Right  = [int]$Rect.Right
        Bottom = [int]$Rect.Bottom
    }
}

function Get-AeroEnvMonitors {
    <#
    .SYNOPSIS
        Every display monitor: device-independent identity (handle), primary flag, monitor rect,
        work rect, and effective DPI (shcore GetDpiForMonitor, MDT_EFFECTIVE_DPI) with its scale.
    #>
    [CmdletBinding()]
    param()

    $monitors = New-Object System.Collections.Generic.List[object]
    foreach ($handle in [AeroSessionEnv.Native]::EnumMonitors()) {
        $info = New-Object AeroSessionEnv.MonitorInfo
        $info.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroSessionEnv.MonitorInfo]))
        [AeroSessionEnv.Native]::GetMonitorInfoW($handle, [ref]$info) | Out-Null
        [uint32]$dpiX = 0
        [uint32]$dpiY = 0
        [void][AeroSessionEnv.Native]::GetDpiForMonitor($handle, [AeroSessionEnv.Native]::MDT_EFFECTIVE_DPI, [ref]$dpiX, [ref]$dpiY)
        [void]$monitors.Add([pscustomobject]@{
            Handle       = $handle
            IsPrimary    = (($info.dwFlags -band [AeroSessionEnv.Native]::MONITORINFOF_PRIMARY) -ne 0)
            MonitorRect  = ConvertTo-AeroEnvRectObject $info.rcMonitor
            WorkRect     = ConvertTo-AeroEnvRectObject $info.rcWork
            EffectiveDpi = [int]$dpiX
            Scale        = [Math]::Round($dpiX / 96.0, 3)
        })
    }
    return $monitors
}

function Get-AeroScreenCount {
    [CmdletBinding()]
    param()
    Add-Type -AssemblyName System.Windows.Forms
    return [System.Windows.Forms.Screen]::AllScreens.Count
}

function Test-AeroVddDevicePresent {
    <#
    .SYNOPSIS
        True when the MttVDD virtual display device exists with Status OK. The root-enumerated
        device lands at ROOT\DISPLAY\nnnn (class Display, the INF's "Virtual Display Driver"
        friendly name) -- the instance path never carries the Root\MttVDD hardware ID, so the
        device is matched by Display class + INF device name. A 'ROOT\MttVDD\*' instance
        pattern can never match (the first live install on 2026-09-28 enumerated as
        ROOT\DISPLAY\0000 and the old check saw nothing while devcon reported success).
    #>
    [CmdletBinding()]
    param()
    $devices = @(Get-PnpDevice -Class Display -ErrorAction SilentlyContinue |
        Where-Object { $_.FriendlyName -eq 'Virtual Display Driver' })
    $ok = @($devices | Where-Object { $_.Status -eq 'OK' })
    return ($ok.Count -gt 0)
}

function Get-AeroEnvState {
    <#
    .SYNOPSIS
        Read-only environment snapshot: monitors (rect/work/effective DPI), taskbar auto-hide
        flag, PowerToys install/run state, FancyZones process, virtual display presence, screen
        count. Changes nothing.
    #>
    [CmdletBinding()]
    param()

    $stateData = New-Object AeroSessionEnv.AppBarData
    $stateData.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroSessionEnv.AppBarData]))
    $state = [AeroSessionEnv.Native]::SHAppBarMessage([AeroSessionEnv.Native]::ABM_GETSTATE, [ref]$stateData)
    $autoHideOn = (([uint32]$state) -band [AeroSessionEnv.Native]::ABS_AUTOHIDE) -ne 0

    $powerToysRunning = $false
    $fancyZonesRunning = $false
    try { if (Get-Process -Name 'PowerToys' -ErrorAction SilentlyContinue) { $powerToysRunning = $true } } catch { }
    try { if (Get-Process -Name 'PowerToys.FancyZones' -ErrorAction SilentlyContinue) { $fancyZonesRunning = $true } } catch { }

    [pscustomobject]@{
        Monitors            = @(Get-AeroEnvMonitors)
        ScreenCount         = Get-AeroScreenCount
        TaskbarAutoHideOn   = $autoHideOn
        PowerToysInstalled  = (Test-Path -LiteralPath $AeroPowerToysExe)
        PowerToysRunning    = $powerToysRunning
        FancyZonesRunning   = $fancyZonesRunning
        VirtualDisplayPresent = (Test-AeroVddDevicePresent)
    }
}

function Get-AeroFancyZonesZones {
    <#
    .SYNOPSIS
        Zone rectangles, in screen pixels, of FancyZones' active layout per monitor, read from
        %LOCALAPPDATA%\Microsoft\PowerToys\FancyZones\zones-settings.json.
    .DESCRIPTION
        Custom layouts carry an explicit zones array (X/Y/Width/Height, work-area-relative px) --
        those are converted to screen px against the matching monitor's work rect. Grid layouts
        are divided out of the work area when rows/columns are present; a zone-count-only grid
        uses a column-major ceil-sqrt derivation and is marked heuristic so SNAP-07's +/-8 px
        comparison fails loudly on a wrong derivation instead of passing silently. Monitors are
        matched to the primary screen when the settings file's device ids cannot be matched
        (its device strings are Win32 display ids, not MONITORINFO device names); the primary is
        where the session's snap drag happens, so the origin is exact for that use.
    #>
    [CmdletBinding()]
    param()

    if (-not (Test-Path -LiteralPath $AeroFancyZonesSettings)) {
        return [pscustomobject]@{
            Found = $false
            Reason = "zones-settings.json not found at $AeroFancyZonesSettings (FancyZones has not run on this profile)"
            Monitors = @()
        }
    }

    $config = $null
    try {
        $config = Get-Content -LiteralPath $AeroFancyZonesSettings -Raw -ErrorAction Stop | ConvertFrom-Json
    }
    catch {
        return [pscustomobject]@{
            Found = $false
            Reason = "zones-settings.json could not be parsed: $($_.Exception.Message)"
            Monitors = @()
        }
    }

    $screens = @(Get-AeroEnvMonitors)
    $primary = $null
    foreach ($s in $screens) { if ($s.IsPrimary) { $primary = $s; break } }
    if (-not $primary -and $screens.Count -gt 0) { $primary = $screens[0] }

    $entries = @()
    if ($config.PSObject.Properties['monitors'] -and $config.monitors) {
        $entries = @($config.monitors)
    }
    elseif ($config.PSObject.Properties['defaults'] -and $config.defaults) {
        $entries = @($config.defaults)
    }

    $resultMonitors = New-Object System.Collections.Generic.List[object]
    foreach ($entry in $entries) {
        $layout = $null
        if ($entry.PSObject.Properties['layout'] -and $entry.layout) { $layout = $entry.layout }
        if (-not $layout) { continue }

        $work = $primary.WorkRect
        $zones = New-Object System.Collections.Generic.List[object]
        $layoutType = [string]$layout.type
        $derivation = 'explicit'

        if ($layoutType -eq 'custom' -and $layout.PSObject.Properties['zones'] -and $layout.zones) {
            foreach ($z in @($layout.zones)) {
                [void]$zones.Add([pscustomobject]@{
                    X = [int]($work.Left + [double]$z.X)
                    Y = [int]($work.Top + [double]$z.Y)
                    Width = [int]$z.Width
                    Height = [int]$z.Height
                })
            }
        }
        else {
            $rows = 0
            $columns = 0
            if ($layout.PSObject.Properties['rows'] -and $layout.rows) { $rows = [int]$layout.rows }
            if ($layout.PSObject.Properties['columns'] -and $layout.columns) { $columns = [int]$layout.columns }
            if ($rows -le 0 -or $columns -le 0) {
                $zoneCount = 4
                if ($layout.PSObject.Properties['zone-count'] -and $layout.'zone-count') { $zoneCount = [int]$layout.'zone-count' }
                $columns = [Math]::Ceiling([Math]::Sqrt([double]$zoneCount))
                $rows = [Math]::Ceiling([double]$zoneCount / [Math]::Max(1, $columns))
                $derivation = "heuristic from zone-count=$zoneCount"
            }
            else {
                $derivation = "rows=$rows columns=$columns"
            }
            $workW = $work.Right - $work.Left
            $workH = $work.Bottom - $work.Top
            for ($r = 0; $r -lt $rows; $r++) {
                for ($c = 0; $c -lt $columns; $c++) {
                    [void]$zones.Add([pscustomobject]@{
                        X = [int]($work.Left + ($workW * $c / $columns))
                        Y = [int]($work.Top + ($workH * $r / $rows))
                        Width = [int]($workW / $columns)
                        Height = [int]($workH / $rows)
                    })
                }
            }
        }

        [void]$resultMonitors.Add([pscustomobject]@{
            LayoutType = $layoutType
            Derivation = $derivation
            WorkRect   = $work
            ZoneCount  = $zones.Count
            Zones      = @($zones)
        })
    }

    return [pscustomobject]@{
        Found = ($resultMonitors.Count -gt 0)
        Reason = $(if ($resultMonitors.Count -gt 0) { '' } else { 'no monitor layout entries in zones-settings.json' })
        Monitors = @($resultMonitors)
    }
}

function Test-AeroTaskbarRevealed {
    <#
    .SYNOPSIS
        True when a visible taskbar window (Shell_TrayWnd primary / Shell_SecondaryTrayWnd
        secondary) has at least half its thickness inside -MonitorRect -- an auto-hidden taskbar
        parks essentially all of itself off the monitor, a revealed one slides in.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$MonitorRect
    )

    $classes = @('Shell_TrayWnd', 'Shell_SecondaryTrayWnd')
    $details = New-Object System.Collections.Generic.List[string]
    $revealed = $false
    foreach ($class in $classes) {
        $hwnd = [AeroSessionEnv.Native]::FindWindowW($class, $null)
        if ($hwnd -eq [IntPtr]::Zero) { continue }
        if (-not [AeroSessionEnv.Native]::IsWindowVisible($hwnd)) { continue }
        $rect = New-Object AeroSessionEnv.EnvRect
        [AeroSessionEnv.Native]::GetWindowRect($hwnd, [ref]$rect) | Out-Null

        $interLeft = [Math]::Max($rect.Left, $MonitorRect.Left)
        $interTop = [Math]::Max($rect.Top, $MonitorRect.Top)
        $interRight = [Math]::Min($rect.Right, $MonitorRect.Right)
        $interBottom = [Math]::Min($rect.Bottom, $MonitorRect.Bottom)
        $overlapW = $interRight - $interLeft
        $overlapH = $interBottom - $interTop
        if ($overlapW -le 0 -or $overlapH -le 0) {
            [void]$details.Add("$class rect=$($rect.Left),$($rect.Top),$($rect.Right),$($rect.Bottom) no overlap with monitor")
            continue
        }
        $taskbarH = $rect.Bottom - $rect.Top
        $taskbarW = $rect.Right - $rect.Left
        # Horizontal taskbars judge on vertical slide-in; vertical ones on horizontal.
        $isHorizontal = ($taskbarW -ge $taskbarH)
        $inside = if ($isHorizontal) { $overlapH } else { $overlapW }
        $full = if ($isHorizontal) { $taskbarH } else { $taskbarW }
        $halfIn = ($inside -ge [int]($full / 2))
        if ($halfIn) { $revealed = $true }
        [void]$details.Add("$class rect=$($rect.Left),$($rect.Top),$($rect.Right),$($rect.Bottom) overlap=${overlapW}x${overlapH} inside=$inside full=$full revealed=$halfIn")
    }
    [pscustomobject]@{
        Revealed = $revealed
        Details  = @($details)
    }
}

function Get-AeroTaskbarAutoHideState {
    [CmdletBinding()]
    param()
    $data = New-Object AeroSessionEnv.AppBarData
    $data.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroSessionEnv.AppBarData]))
    $state = [AeroSessionEnv.Native]::SHAppBarMessage([AeroSessionEnv.Native]::ABM_GETSTATE, [ref]$data)
    return (([uint32]$state) -band [AeroSessionEnv.Native]::ABS_AUTOHIDE) -ne 0
}

function Set-AeroTaskbarAutoHide {
    <#
    .SYNOPSIS
        ABM_SETSTATE with ABS_AUTOHIDE (-Enabled) or 0 (-Enabled $false), immediately re-verified
        through ABM_GETSTATE. Requires -Session; records before/after.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][bool]$Enabled
    )
    Assert-AeroEnvSession -Session $Session

    $before = Get-AeroTaskbarAutoHideState
    Add-AeroInputLog -Session $Session -Action "TaskbarAutoHide Enabled=$Enabled (before=$before)"

    $lParamValue = if ($Enabled) { [AeroSessionEnv.Native]::ABS_AUTOHIDE } else { 0 }
    $data = New-Object AeroSessionEnv.AppBarData
    $data.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroSessionEnv.AppBarData]))
    $data.lParam = [IntPtr][int64]$lParamValue

    if (-not $Session.IsDryRun) {
        [void][AeroSessionEnv.Native]::SHAppBarMessage([AeroSessionEnv.Native]::ABM_SETSTATE, [ref]$data)
        Start-Sleep -Milliseconds 400
    }

    $after = Get-AeroTaskbarAutoHideState
    $verified = ($after -eq $Enabled)
    [pscustomobject]@{
        Before = $before
        After = $after
        Requested = $Enabled
        Verified = $(if ($Session.IsDryRun) { $null } else { $verified })
        DryRun = [bool]$Session.IsDryRun
    }
}

function Assert-AeroVddFiles {
    <#
    .SYNOPSIS
        Verifies the driver package files exist in -DriverDir and match the SHA-256 values from
        22-SESSION-ENV.md. Throws (never installs) on any mismatch.
    #>
    param([Parameter(Mandatory = $true)][string]$DriverDir)
    if (-not (Test-Path -LiteralPath $DriverDir -PathType Container)) {
        throw "Assert-AeroVddFiles: driver directory not found: $DriverDir"
    }
    foreach ($name in @('MttVDD.inf', 'MttVDD.dll', 'mttvdd.cat', 'devcon.exe')) {
        $path = Join-Path $DriverDir $name
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Assert-AeroVddFiles: required file missing: $path"
        }
    }
    foreach ($name in $AeroVddSha256.Keys) {
        $path = Join-Path $DriverDir $name
        $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
        if ($hash -ne $AeroVddSha256[$name]) {
            throw "Assert-AeroVddFiles: SHA-256 mismatch for $name -- expected $($AeroVddSha256[$name]) got $hash; refusing to run the installer"
        }
    }
    # The VDD Control app is the documented installer source for the package; when it is present
    # next to the driver files, verify it too (it is not required for a devcon-only install).
    $control = Join-Path $DriverDir 'VDD Control.exe'
    if (Test-Path -LiteralPath $control -PathType Leaf) {
        $hash = (Get-FileHash -LiteralPath $control -Algorithm SHA256).Hash
        if ($hash -ne $AeroVddInstallerSha256) {
            throw "Assert-AeroVddFiles: SHA-256 mismatch for 'VDD Control.exe' -- expected $($AeroVddInstallerSha256) got $hash; refusing to run"
        }
    }
}

function Invoke-AeroDevcon {
    <#
    .SYNOPSIS
        Runs devcon.exe elevated (Start-Process -Verb RunAs) so Windows shows the maintainer the
        UAC prompt; returns the elevated process's exit code.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$DriverDir,
        [Parameter(Mandatory = $true)][string[]]$DevconArgs
    )
    $devcon = Join-Path $DriverDir 'devcon.exe'
    $proc = Start-Process -FilePath $devcon -ArgumentList $DevconArgs -WorkingDirectory $DriverDir -Verb RunAs -Wait -PassThru
    return $proc.ExitCode
}

function Install-AeroVirtualDisplay {
    <#
    .SYNOPSIS
        Installs the vetted MttVDD virtual display driver: `devcon.exe install .\MttVDD.inf
        "Root\MttVDD"` from -DriverDir, elevated (UAC shown to the maintainer), then waits up to
        3 minutes for the display to appear. SHA-256 re-verified first; requires -Session.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][string]$DriverDir
    )
    Assert-AeroEnvSession -Session $Session
    Assert-AeroVddFiles -DriverDir $DriverDir

    $beforeCount = Get-AeroScreenCount
    $beforePresent = Test-AeroVddDevicePresent
    Add-AeroInputLog -Session $Session -Action "VirtualDisplay install from '$DriverDir' (screens=$beforeCount present=$beforePresent)"

    if ($Session.IsDryRun) {
        return [pscustomobject]@{ Before = "screens=$beforeCount present=$beforePresent"; After = 'dry run -- nothing installed'; ExitCode = $null; Verified = $null; DryRun = $true }
    }

    if ($beforePresent) {
        # Idempotence guard: a second `devcon install` creates a SECOND root-enumerated
        # instance (ROOT\DISPLAY\nnnn increments -- observed live 2026-09-28 when a retry
        # produced ROOT\DISPLAY\0000 and \0001 side by side) instead of reusing the first.
        # When the device is already installed, skip the elevated install and report the
        # attach state as-is.
        $attachDeadline = (Get-Date).AddSeconds(10)
        $attached = ((Get-AeroScreenCount) -gt $beforeCount)
        while (-not $attached -and (Get-Date) -lt $attachDeadline) {
            Start-Sleep -Milliseconds 1000
            $attached = ((Get-AeroScreenCount) -gt $beforeCount)
        }
        return [pscustomobject]@{
            Before = "screens=$beforeCount present=$beforePresent"
            After = "screens=$(Get-AeroScreenCount) present=$(Test-AeroVddDevicePresent)"
            ExitCode = 'skipped (device already present)'
            Verified = $attached
            DryRun = $false
        }
    }

    $exitCode = Invoke-AeroDevcon -DriverDir $DriverDir -DevconArgs @('install', '.\MttVDD.inf', 'Root\MttVDD')

    $deadline = (Get-Date).AddMinutes(3)
    $seen = $false
    while ((Get-Date) -lt $deadline) {
        $count = Get-AeroScreenCount
        $present = Test-AeroVddDevicePresent
        if (($count -gt $beforeCount) -and $present) { $seen = $true; break }
        Start-Sleep -Milliseconds 2000
    }
    $afterCount = Get-AeroScreenCount
    [pscustomobject]@{
        Before = "screens=$beforeCount present=$beforePresent"
        After = "screens=$afterCount present=$(Test-AeroVddDevicePresent)"
        ExitCode = $exitCode
        Verified = $seen
        DryRun = $false
    }
}

function Remove-AeroVirtualDisplay {
    <#
    .SYNOPSIS
        Removes the MttVDD device: `devcon.exe remove "Root\MttVDD"`, elevated, then waits up to
        3 minutes for the display to disappear. SHA-256 re-verified first; requires -Session.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][string]$DriverDir
    )
    Assert-AeroEnvSession -Session $Session
    Assert-AeroVddFiles -DriverDir $DriverDir

    $beforeCount = Get-AeroScreenCount
    Add-AeroInputLog -Session $Session -Action "VirtualDisplay remove from '$DriverDir' (screens=$beforeCount)"

    if ($Session.IsDryRun) {
        return [pscustomobject]@{ Before = "screens=$beforeCount"; After = 'dry run -- nothing removed'; ExitCode = $null; Verified = $null; DryRun = $true }
    }

    $exitCode = Invoke-AeroDevcon -DriverDir $DriverDir -DevconArgs @('remove', 'Root\MttVDD')

    $deadline = (Get-Date).AddMinutes(3)
    $gone = $false
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-AeroVddDevicePresent)) { $gone = $true; break }
        Start-Sleep -Milliseconds 2000
    }
    [pscustomobject]@{
        Before = "screens=$beforeCount"
        After = "screens=$(Get-AeroScreenCount) present=$(Test-AeroVddDevicePresent)"
        ExitCode = $exitCode
        Verified = $gone
        DryRun = $false
    }
}

function Set-AeroDisplayScale {
    <#
    .SYNOPSIS
        Sets -Percent scaling on the virtual display ONLY (its PowerToys name 'VDD by MTT'
        identifies it), via the DisplayConfig module's Set-DisplayScale -- the exact method
        22-SESSION-ENV.md vetted. Verifies the target monitor's effective DPI afterwards and
        that the primary monitor's DPI is untouched. Requires -Session.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][int]$Percent
    )
    Assert-AeroEnvSession -Session $Session
    if ($Percent -lt 100 -or $Percent -gt 200) {
        throw "Set-AeroDisplayScale: -Percent must be 100..200, got $Percent"
    }

    $before = @(Get-AeroEnvMonitors)
    $beforePrimary = $null
    foreach ($m in $before) { if ($m.IsPrimary) { $beforePrimary = $m; break } }
    Add-AeroInputLog -Session $Session -Action "DisplayScale ${Percent}% on virtual display only (monitors=$($before.Count))"

    if ($Session.IsDryRun) {
        return [pscustomobject]@{ Before = "$($before.Count) monitor(s)"; After = 'dry run -- scale unchanged'; Verified = $null; DryRun = $true }
    }

    if (-not (Get-Module -ListAvailable -Name DisplayConfig -ErrorAction SilentlyContinue)) {
        Add-AeroInputLog -Session $Session -Action 'DisplayScale: Install-Module DisplayConfig -RequiredVersion 1.1.1 -Scope CurrentUser'
        [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
        Install-Module -Name DisplayConfig -RequiredVersion 1.1.1 -Scope CurrentUser -Force
    }
    Import-Module DisplayConfig -ErrorAction Stop

    $target = Get-DisplayInfo | Where-Object { $_.DisplayName -eq 'VDD by MTT' } | Select-Object -First 1
    if (-not $target) {
        throw "Set-AeroDisplayScale: no display named 'VDD by MTT' found -- the virtual display must be installed first"
    }
    Set-DisplayScale -DisplayId $target.DisplayId -Scale $Percent

    $expectedDpi = [int](96 * $Percent / 100.0)
    $deadline = (Get-Date).AddSeconds(60)
    $seen = $false
    while ((Get-Date) -lt $deadline) {
        $after = @(Get-AeroEnvMonitors)
        foreach ($m in $after) {
            if (-not $m.IsPrimary -and $m.EffectiveDpi -eq $expectedDpi) { $seen = $true; break }
        }
        if ($seen) { break }
        Start-Sleep -Milliseconds 2000
    }

    $afterAll = @(Get-AeroEnvMonitors)
    $afterPrimary = $null
    foreach ($m in $afterAll) { if ($m.IsPrimary) { $afterPrimary = $m; break } }
    if ($beforePrimary -and $afterPrimary -and ($afterPrimary.EffectiveDpi -ne $beforePrimary.EffectiveDpi)) {
        throw "Set-AeroDisplayScale: primary monitor DPI changed $($beforePrimary.EffectiveDpi) -> $($afterPrimary.EffectiveDpi); the primary must never be touched"
    }

    [pscustomobject]@{
        Before = "$($before.Count) monitor(s), primaryDpi=$($beforePrimary.EffectiveDpi)"
        After = "$($afterAll.Count) monitor(s), targetDpi expected=$expectedDpi"
        Verified = $seen
        DryRun = $false
    }
}

function Install-AeroPowerToys {
    <#
    .SYNOPSIS
        Installs the vetted per-user PowerToys silently (installer SHA-256 re-verified first),
        then waits up to 3 minutes for PowerToys.exe to appear. Requires -Session.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [Parameter(Mandatory = $true)][string]$InstallerPath
    )
    Assert-AeroEnvSession -Session $Session

    if (-not (Test-Path -LiteralPath $InstallerPath -PathType Leaf)) {
        throw "Install-AeroPowerToys: installer not found: $InstallerPath"
    }
    $hash = (Get-FileHash -LiteralPath $InstallerPath -Algorithm SHA256).Hash
    if ($hash -ne $AeroPowerToysInstallerSha256) {
        throw "Install-AeroPowerToys: SHA-256 mismatch for $InstallerPath -- expected $($AeroPowerToysInstallerSha256) got $hash; refusing to run the installer"
    }

    $before = Test-Path -LiteralPath $AeroPowerToysExe
    Add-AeroInputLog -Session $Session -Action "PowerToys install '$InstallerPath' (installed=$before)"

    if ($Session.IsDryRun) {
        return [pscustomobject]@{ Before = "installed=$before"; After = 'dry run -- nothing installed'; Verified = $null; DryRun = $true }
    }

    $proc = Start-Process -FilePath $InstallerPath -ArgumentList @('/install', '/quiet', '/norestart') -Wait -PassThru

    $deadline = (Get-Date).AddMinutes(3)
    $seen = $false
    while ((Get-Date) -lt $deadline) {
        if (Test-Path -LiteralPath $AeroPowerToysExe) { $seen = $true; break }
        Start-Sleep -Milliseconds 2000
    }
    [pscustomobject]@{
        Before = "installed=$before"
        After = "installed=$(Test-Path -LiteralPath $AeroPowerToysExe) exitCode=$($proc.ExitCode)"
        Verified = $seen
        DryRun = $false
    }
}

function Remove-AeroPowerToys {
    <#
    .SYNOPSIS
        Uninstalls PowerToys (installer /uninstall /quiet when -InstallerPath still exists,
        otherwise Get-Package 'PowerToys (Preview)' | Uninstall-Package), then waits up to 3
        minutes for PowerToys.exe to disappear. Requires -Session.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session,
        [string]$InstallerPath = ''
    )
    Assert-AeroEnvSession -Session $Session

    $before = Test-Path -LiteralPath $AeroPowerToysExe
    Add-AeroInputLog -Session $Session -Action "PowerToys remove (installed=$before installer=$([bool]$InstallerPath))"

    if ($Session.IsDryRun) {
        return [pscustomobject]@{ Before = "installed=$before"; After = 'dry run -- nothing removed'; Verified = $null; DryRun = $true }
    }

    if ($InstallerPath -and (Test-Path -LiteralPath $InstallerPath -PathType Leaf)) {
        $hash = (Get-FileHash -LiteralPath $InstallerPath -Algorithm SHA256).Hash
        if ($hash -ne $AeroPowerToysInstallerSha256) {
            throw "Remove-AeroPowerToys: SHA-256 mismatch for $InstallerPath -- expected $($AeroPowerToysInstallerSha256) got $hash; refusing to run"
        }
        $proc = Start-Process -FilePath $InstallerPath -ArgumentList @('/uninstall', '/quiet') -Wait -PassThru
    }
    else {
        Get-Package -Name 'PowerToys (Preview)' -ErrorAction SilentlyContinue | Uninstall-Package
    }

    $deadline = (Get-Date).AddMinutes(3)
    $gone = $false
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-Path -LiteralPath $AeroPowerToysExe)) { $gone = $true; break }
        Start-Sleep -Milliseconds 2000
    }
    [pscustomobject]@{
        Before = "installed=$before"
        After = "installed=$(Test-Path -LiteralPath $AeroPowerToysExe)"
        Verified = $gone
        DryRun = $false
    }
}

function Start-AeroFancyZones {
    <#
    .SYNOPSIS
        Starts PowerToys.exe and waits (up to 60 s) for the FancyZones module process, so
        SNAP-07's Shift-drag has a live zone layout. Requires -Session.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Session
    )
    Assert-AeroEnvSession -Session $Session

    if (-not (Test-Path -LiteralPath $AeroPowerToysExe)) {
        throw "Start-AeroFancyZones: PowerToys.exe not found at $AeroPowerToysExe -- install PowerToys first"
    }
    $before = $false
    try { if (Get-Process -Name 'PowerToys.FancyZones' -ErrorAction SilentlyContinue) { $before = $true } } catch { }
    Add-AeroInputLog -Session $Session -Action "FancyZones start (running=$before)"

    if ($Session.IsDryRun) {
        return [pscustomobject]@{ Before = "running=$before"; After = 'dry run -- nothing started'; Verified = $null; DryRun = $true }
    }

    if (-not $before) {
        Start-Process -FilePath $AeroPowerToysExe | Out-Null
    }
    $deadline = (Get-Date).AddSeconds(60)
    $running = $false
    while ((Get-Date) -lt $deadline) {
        try { if (Get-Process -Name 'PowerToys.FancyZones' -ErrorAction SilentlyContinue) { $running = $true; break } } catch { }
        Start-Sleep -Milliseconds 1000
    }
    [pscustomobject]@{
        Before = "running=$before"
        After = "running=$running"
        Verified = $running
        DryRun = $false
    }
}

function Test-AeroSessionEnvSelf {
    <#
    .SYNOPSIS
        Prints the read-only environment snapshot (Get-AeroEnvState) and nothing else -- no
        session is created, nothing is changed, no input is sent.
    #>
    [CmdletBinding()]
    param()
    Initialize-AeroDpiAwareness
    $state = Get-AeroEnvState
    Write-Host "SESSIONENV_SELFTEST monitors=$($state.ScreenCount) autoHideOn=$($state.TaskbarAutoHideOn) powerToysInstalled=$($state.PowerToysInstalled) powerToysRunning=$($state.PowerToysRunning) fancyZonesRunning=$($state.FancyZonesRunning) virtualDisplayPresent=$($state.VirtualDisplayPresent)"
    foreach ($m in $state.Monitors) {
        Write-Host ("SESSIONENV_SELFTEST monitor primary={0} monitorRect={1},{2},{3},{4} workRect={5},{6},{7},{8} dpi={9} scale={10}" -f `
            $m.IsPrimary, $m.MonitorRect.Left, $m.MonitorRect.Top, $m.MonitorRect.Right, $m.MonitorRect.Bottom, `
            $m.WorkRect.Left, $m.WorkRect.Top, $m.WorkRect.Right, $m.WorkRect.Bottom, $m.EffectiveDpi, $m.Scale)
    }
    $zones = Get-AeroFancyZonesZones
    Write-Host "SESSIONENV_SELFTEST fancyZonesZones found=$($zones.Found) reason=$($zones.Reason)"
    foreach ($entry in $zones.Monitors) {
        Write-Host "SESSIONENV_SELFTEST zones layout=$($entry.LayoutType) derivation=$($entry.Derivation) count=$($entry.ZoneCount)"
    }
    Write-Host 'SESSIONENV_SELFTEST PASS'
}

if ($SelfTestRequested) {
    Test-AeroSessionEnvSelf
    exit 0
}
