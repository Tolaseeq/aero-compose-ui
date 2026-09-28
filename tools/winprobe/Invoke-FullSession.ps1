<#
.SYNOPSIS
    Full VER-12 real-input session suite: every SNAP-01..07, WIN-01..06 and BTN-01..02 check as an
    isolated, evidence-producing entry, runnable as a JDK 21 pass (:showcase:run) and a JBR 21
    pass (:showcase:hotRun) (aero-compose-ui v3.2.0).
.DESCRIPTION
    One AeroInputSession (RealInput.ps1) gates every real mouse/keyboard action for the whole
    run; environment changes (virtual 150% display, PowerToys/FancyZones, taskbar auto-hide) go
    through SessionEnv.ps1 functions that require that session and record before/after state.
    -AuthorizedBy is mandatory unless -DryRun. A -DryRun session launches the showcase in capture
    mode (non-focusable, behind), prints every planned action and check, confirms all helper
    functions resolve, and proves the cursor never moved and nothing was installed -- it never
    reaches SendInput. -Checks takes a comma-separated list of check registration Ids to run a
    subset (empty = all; unknown Ids abort before anything is launched, installed or sent). Each check runs in its own try/catch producing {Id, Result, Evidence,
    Frames} with Result PASS|FAIL|UNCONFIRMED, so a failing check never aborts the rest.
    Driver and PowerToys removal are NOT part of this script's session run (the teardown plan
    owns them); at the very end of a real -Phase All run the taskbar auto-hide state is restored
    to the value recorded before -Phase EnvSetup changed it.
.NOTES
    PowerShell 5.1 only. Evidence lines are flattened to strings before they reach
    ConvertTo-Json (the 22-10 probe-script rule: nested pscustomobjects can spin the PS 5.1
    serializer). System-menu items are reached with Home/End + arrows, never mnemonic letters --
    letters are locale-dependent (a Russian Windows uses different mnemonics).
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [string]$AuthorizedBy,
    [switch]$DryRun,
    [ValidateSet('Jdk', 'Jbr', 'Both')][string]$Pass = 'Both',
    [ValidateSet('EnvSetup', 'Checks', 'All')][string]$Phase = 'All',
    [string[]]$Checks = @(),
    [string]$OutDir = 'C:\1A_WORK\ui_lib\.captures\22-session',
    [string]$RepoRoot = 'C:\1A_WORK\ui_lib',
    [string]$DriverDir = '',
    [string]$PowerToysInstaller = '',
    [int]$ReadyTimeoutSec = 300
)

Set-StrictMode -Version 2

if (-not $DryRun -and [string]::IsNullOrWhiteSpace($AuthorizedBy)) {
    Write-Host 'SESSION ABORT: -AuthorizedBy is required unless -DryRun is set; sending no input and changing nothing.'
    exit 1
}

# -Checks subset: empty = every registered check (today's behavior). Non-empty = run exactly the
# checks whose registration Id is in the list, in the suite's own registration order. Entries may
# be comma-separated (powershell -File delivers "A,B,C" as one string) and are split here. The
# valid Ids are read from this script's own registration calls, so the list can never drift from
# the suite. Any unknown Id aborts here -- before any launch, install or input -- because a typo
# must fail loudly, never silently run nothing.
$script:CheckSubset = $null
if (@($Checks).Count -gt 0) {
    # Assembled by concatenation so this extraction pattern never matches its own source line.
    $idPattern = ('Invoke-SessionCheck' + ' -Id ') + "'([^']+)'"
    $validIds = @([regex]::Matches((Get-Content -LiteralPath $PSCommandPath -Raw), $idPattern) |
        ForEach-Object { $_.Groups[1].Value } |
        Where-Object { $_ -cmatch '^[A-Z0-9-]+$' })
    $wanted = @()
    foreach ($entry in @($Checks)) {
        foreach ($part in ([string]$entry).Split(',')) { $wanted += $part.Trim() }
    }
    $unknown = @($wanted | Where-Object { $validIds -notcontains $_ })
    if ($unknown.Count -gt 0) {
        Write-Host "SESSION ABORT: -Checks contains unknown id(s): $($unknown -join ', ')"
        Write-Host "SESSION ABORT: valid id(s) ($($validIds.Count)): $($validIds -join ', ')"
        exit 2
    }
    $script:CheckSubset = New-Object 'System.Collections.Generic.HashSet[string]'
    foreach ($id in $wanted) { [void]$script:CheckSubset.Add($id) }
}

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'WinProbe.ps1')
. (Join-Path $here 'RealInput.ps1')
. (Join-Path $here 'SessionEnv.ps1')
. (Join-Path $here 'PositiveControlWindow.ps1')
. (Join-Path $here 'Watch-SnapFlyout.ps1')

Initialize-AeroDpiAwareness

$session = New-AeroInputSession -AuthorizedBy $(if ($DryRun -and [string]::IsNullOrWhiteSpace($AuthorizedBy)) { 'DryRun-NoAuthorization' } else { $AuthorizedBy }) -DryRun:$DryRun

if (-not [System.IO.Path]::IsPathRooted($OutDir)) {
    $OutDir = Join-Path $RepoRoot $OutDir
}
if (-not (Test-Path -LiteralPath $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}

$script:AllChecks = New-Object System.Collections.Generic.List[object]
$script:CurrentChecks = New-Object System.Collections.Generic.List[object]
$script:EnvSetupRan = $false
$script:AutoHideOriginal = $null
$script:EnvSteps = New-Object System.Collections.Generic.List[string]

# ---------------------------------------------------------------------------
# Small helpers
# ---------------------------------------------------------------------------

function New-SessionOutcome {
    param([Parameter(Mandatory = $true)][ValidateSet('PASS', 'FAIL', 'UNCONFIRMED')][string]$Result, [string]$Detail = '')
    [pscustomobject]@{ Result = $Result; Detail = $Detail }
}

function Get-ReporterField {
    param($State, [string]$Name)
    if (-not $State) { return $null }
    $prop = $State.PSObject.Properties[$Name]
    if ($prop) { return [string]$prop.Value }
    return $null
}

function Get-SessionRect {
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)
    $r = New-Object AeroWinProbe.WRect
    if (-not [AeroWinProbe.Native]::GetWindowRect($Hwnd, [ref]$r)) {
        throw "Get-SessionRect: GetWindowRect failed for hwnd $Hwnd (window gone?)"
    }
    return $r
}

function ConvertTo-SessionRectString {
    param([Parameter(Mandatory = $true)]$Rect)
    return "$($Rect.Left),$($Rect.Top),$($Rect.Right),$($Rect.Bottom)"
}

function Test-SessionRectMatch {
    param([Parameter(Mandatory = $true)]$Actual, [Parameter(Mandatory = $true)]$Expected, [int]$Tolerance = 8)
    return -not (
        ([Math]::Abs($Actual.Left - $Expected.Left) -gt $Tolerance) -or
        ([Math]::Abs($Actual.Top - $Expected.Top) -gt $Tolerance) -or
        ([Math]::Abs($Actual.Right - $Expected.Right) -gt $Tolerance) -or
        ([Math]::Abs($Actual.Bottom - $Expected.Bottom) -gt $Tolerance)
    )
}

function New-SessionWRect {
    param([int]$Left, [int]$Top, [int]$Right, [int]$Bottom)
    $r = New-Object AeroWinProbe.WRect
    $r.Left = $Left; $r.Top = $Top; $r.Right = $Right; $r.Bottom = $Bottom
    return $r
}

function Get-SessionGeometry {
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd, [string]$Layout = 'main')
    $info = Get-WinProbeWindowInfo -Hwnd $Hwnd
    $clientWidthDp = ($info.ClientRect.Right - $info.ClientRect.Left) / $info.Scale
    $clientHeightDp = ($info.ClientRect.Bottom - $info.ClientRect.Top) / $info.Scale
    $points = Get-WinProbeNamedPoints -Layout $WinProbeLayout[$Layout] -ClientWidthDp $clientWidthDp -ClientHeightDp $clientHeightDp -Scale $info.Scale
    $origin = New-Object AeroWinProbe.WPoint
    [AeroWinProbe.Native]::ClientToScreen($Hwnd, [ref]$origin) | Out-Null
    [pscustomobject]@{ Points = $points; Origin = $origin; Info = $info }
}

function Get-SessionScreenPoint {
    param([Parameter(Mandatory = $true)]$Geometry, [Parameter(Mandatory = $true)][string]$PointName)
    $pt = $Geometry.Points[$PointName]
    [pscustomobject]@{ X = $Geometry.Origin.X + $pt.x; Y = $Geometry.Origin.Y + $pt.y }
}

function Get-SessionMonitorInfo {
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)
    $monitor = [AeroWinProbe.Native]::MonitorFromWindow($Hwnd, [AeroWinProbe.Native]::MONITOR_DEFAULTTONEAREST)
    $monitorInfo = New-Object AeroWinProbe.MonitorInfo
    $monitorInfo.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroWinProbe.MonitorInfo]))
    [AeroWinProbe.Native]::GetMonitorInfoW($monitor, [ref]$monitorInfo) | Out-Null
    return $monitorInfo
}

function Test-SessionWindowAlive {
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)
    if ($Hwnd -eq [IntPtr]::Zero) { return $false }
    $r = New-Object AeroWinProbe.WRect
    return [AeroWinProbe.Native]::GetWindowRect($Hwnd, [ref]$r)
}

function Wait-SessionWindowByTitle {
    param([Parameter(Mandatory = $true)][string]$Title, [int]$TimeoutSec = 15)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            return Find-AeroShowcaseWindow -Title $Title -TimeoutSec 1
        }
        catch {
            Start-Sleep -Milliseconds 250
        }
    }
    throw "Wait-SessionWindowByTitle: no window titled '$Title' appeared within $TimeoutSec s"
}

function Wait-SessionNewShowcaseWindow {
    <#
    .SYNOPSIS
        Polls for a top-level window with the given exact title that is NOT one of -ExcludeHwnd.
        A mid-pass second launch (the C02/F18 nativeChrome=false opt-out) carries the same window
        title as the pass's own main window, so title alone can never tell them apart -- the
        pass's HWNDs are excluded explicitly instead.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Title,
        [Parameter(Mandatory = $true)][IntPtr[]]$ExcludeHwnd,
        [int]$TimeoutSec = 120
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $candidates = [AeroCaptureNative]::FindTopLevelWindows($Title)
        foreach ($h in $candidates) {
            if ($ExcludeHwnd -notcontains $h) { return $h }
        }
        Start-Sleep -Milliseconds 250
    }
    throw "Wait-SessionNewShowcaseWindow: no NEW window titled '$Title' appeared within $TimeoutSec s"
}

function Get-SessionLogLines {
    param([string]$LogPath)
    if (-not $LogPath -or -not (Test-Path -LiteralPath $LogPath)) { return @() }
    return @(Get-Content -LiteralPath $LogPath -ErrorAction SilentlyContinue)
}

function Wait-SessionEventAfter {
    param([string]$LogPath, [Parameter(Mandatory = $true)][string]$Name, [string]$Detail = '', [int]$AfterLineCount = 0, [int]$TimeoutSec = 5)
    $prefix = "AERO_EVENT name=$Name"
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $lines = @(Get-SessionLogLines -LogPath $LogPath)
        for ($i = $AfterLineCount; $i -lt $lines.Count; $i++) {
            $line = [string]$lines[$i]
            if ($line.StartsWith($prefix) -and ($Detail -eq '' -or $line.Contains($Detail))) { return $line }
        }
        Start-Sleep -Milliseconds 150
    }
    return $null
}

function Wait-SessionChromeEventAfter {
    <#
    .SYNOPSIS
        Wait-SessionEventAfter's shape for AERO_CHROME trace lines (NativeWindowChromeRegistry's
        chromeTrace, gated by -Daero.chromeTrace=true): polls for a line starting
        "AERO_CHROME event=<Name>" that appeared after line -AfterLineCount, every 150 ms, up to
        -TimeoutSec.
    #>
    param([string]$LogPath, [Parameter(Mandatory = $true)][string]$Name, [string]$Detail = '', [int]$AfterLineCount = 0, [int]$TimeoutSec = 10)
    $prefix = "AERO_CHROME event=$Name"
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $lines = @(Get-SessionLogLines -LogPath $LogPath)
        for ($i = $AfterLineCount; $i -lt $lines.Count; $i++) {
            $line = [string]$lines[$i]
            if ($line.StartsWith($prefix) -and ($Detail -eq '' -or $line.Contains($Detail))) { return $line }
        }
        Start-Sleep -Milliseconds 150
    }
    return $null
}

function Reset-SessionWindow {
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd, [int]$X, [int]$Y, [int]$W, [int]$H)
    $swpNoSizeNoMove = [AeroWinProbe.Native]::SWP_NOMOVE -bor [AeroWinProbe.Native]::SWP_NOSIZE -bor [AeroWinProbe.Native]::SWP_NOACTIVATE
    if ([AeroWinProbe.Native]::IsZoomed($Hwnd)) {
        [void][AeroWinProbe.Native]::SendMessageTimeoutW($Hwnd, [AeroWinProbe.Native]::WM_SYSCOMMAND, [IntPtr][AeroWinProbe.Native]::SC_RESTORE, [IntPtr]::Zero, [AeroWinProbe.Native]::SMTO_ABORTIFHUNG, 2000, [ref]([IntPtr]::Zero))
        [void][AeroWinProbe.Native]::SetWindowPos($Hwnd, [AeroWinProbe.Native]::HWND_BOTTOM, 0, 0, 0, 0, $swpNoSizeNoMove)
        Start-Sleep -Milliseconds 200
    }
    if ([AeroWinProbe.Native]::IsIconic($Hwnd)) {
        [void][AeroWinProbe.Native]::ShowWindow($Hwnd, [AeroWinProbe.Native]::SW_SHOWNOACTIVATE)
        Start-Sleep -Milliseconds 200
    }
    $flags = [AeroWinProbe.Native]::SWP_NOZORDER -bor [AeroWinProbe.Native]::SWP_NOACTIVATE
    [void][AeroWinProbe.Native]::SetWindowPos($Hwnd, [IntPtr]::Zero, $X, $Y, $W, $H, $flags)
    Start-Sleep -Milliseconds 150
}

function Reset-SessionWindows {
    param([Parameter(Mandatory = $true)]$Context)
    try {
        if ($Context.MainHwnd -ne [IntPtr]::Zero -and (Test-SessionWindowAlive -Hwnd $Context.MainHwnd)) {
            Reset-SessionWindow -Hwnd $Context.MainHwnd -X 360 -Y 140 -W 1200 -H 800
        }
    }
    catch { }
    try {
        if ($Context.NarrowHwnd -ne [IntPtr]::Zero -and (Test-SessionWindowAlive -Hwnd $Context.NarrowHwnd)) {
            Reset-SessionWindow -Hwnd $Context.NarrowHwnd -X 1560 -Y 140 -W 300 -H 480
        }
    }
    catch { }
}

function Invoke-SessionFrame {
    param($Context, [Parameter(Mandatory = $true)][IntPtr]$Hwnd, [Parameter(Mandatory = $true)][string]$Name, [switch]$SkipSanity)
    if ([string]::IsNullOrEmpty($Context.CurrentCheckId)) { throw 'Invoke-SessionFrame: no current check id set' }
    $path = Join-Path $Context.FramesDir "$($Context.CurrentCheckId)-$Name.png"
    if ($SkipSanity) {
        Invoke-AeroWindowCapture -Hwnd $Hwnd -OutFile $path -ExpectedBackgroundArgb 'FF000000' -SkipSanity | Out-Null
    }
    else {
        Invoke-AeroWindowCapture -Hwnd $Hwnd -OutFile $path -ExpectedBackgroundArgb 'FF0D1B2A' | Out-Null
    }
    return $path
}

function Copy-SessionBitmapRegion {
    param([Parameter(Mandatory = $true)][string]$Source, [Parameter(Mandatory = $true)][string]$Dest, [int]$X, [int]$Y, [int]$Width, [int]$Height)
    Add-Type -AssemblyName System.Drawing
    $bmp = [System.Drawing.Bitmap]::FromFile($Source)
    try {
        $x0 = [Math]::Max(0, [Math]::Min($X, $bmp.Width - 1))
        $y0 = [Math]::Max(0, [Math]::Min($Y, $bmp.Height - 1))
        $w = [Math]::Max(1, [Math]::Min($Width, $bmp.Width - $x0))
        $h = [Math]::Max(1, [Math]::Min($Height, $bmp.Height - $y0))
        $rect = New-Object System.Drawing.Rectangle($x0, $y0, $w, $h)
        $crop = $bmp.Clone($rect, $bmp.PixelFormat)
        try { $crop.Save($Dest, [System.Drawing.Imaging.ImageFormat]::Png) } finally { $crop.Dispose() }
    }
    finally { $bmp.Dispose() }
}

function New-SessionButtonStrip {
    param($Context, [Parameter(Mandatory = $true)][string]$FramePath, [Parameter(Mandatory = $true)][string]$Name, [int]$CenterX, [int]$Top = 6, [int]$Height = 4, [int]$HalfWidth = 18)
    $dest = Join-Path $Context.FramesDir "$($Context.CurrentCheckId)-strip-$Name.png"
    Copy-SessionBitmapRegion -Source $FramePath -Dest $dest -X ($CenterX - $HalfWidth) -Y $Top -Width (2 * $HalfWidth) -Height $Height
    return $dest
}

function Compare-SessionImages {
    param([Parameter(Mandatory = $true)][string]$Before, [Parameter(Mandatory = $true)][string]$After)
    return Compare-WinProbeRegion -Before $Before -After $After -Top 0 -Bottom 100000 -Left 0 -Right 0
}

function Test-SessionSystemMenuVisible {
    param([Parameter(Mandatory = $true)][uint32]$AppPid)
    foreach ($hwnd in [AeroSnapWatch.Native]::EnumTopLevelAll()) {
        if ([AeroSnapWatch.Native]::GetClassName($hwnd) -eq '#32768') {
            [uint32]$ownerPid = 0
            [AeroSnapWatch.Native]::GetWindowThreadProcessId($hwnd, [ref]$ownerPid) | Out-Null
            if ($ownerPid -eq $AppPid) { return $true }
        }
    }
    return $false
}

function Invoke-SessionFocus {
    param($Context, [Parameter(Mandatory = $true)][IntPtr]$Hwnd, [Parameter(Mandatory = $true)][int]$ScreenX, [Parameter(Mandatory = $true)][int]$ScreenY, $Evidence)
    Invoke-AeroClick -Session $Context.Session -X $ScreenX -Y $ScreenY
    if (-not $Context.Session.IsDryRun) { Start-Sleep -Milliseconds 350 }
    $fg = [AeroWinProbe.Native]::GetForegroundWindow()
    [uint32]$fgPid = 0
    [AeroWinProbe.Native]::GetWindowThreadProcessId($fg, [ref]$fgPid) | Out-Null
    $ours = ($fgPid -eq [uint32]$Context.AppPid)
    if ($null -ne $Evidence) { [void]$Evidence.Add("foregroundOurs=$ours") }
    return $ours
}

function Add-SessionPlacementSample {
    param($Context, [Parameter(Mandatory = $true)][IntPtr]$Hwnd, [Parameter(Mandatory = $true)][string]$Label)
    try {
        $label = if ($Hwnd -eq $Context.MainHwnd) { 'main' } else { 'narrow' }
        $rep = Get-WinProbeReporterState -LogPath $Context.LogPath -Label $label
        $placement = Get-ReporterField $rep 'placement'
        $zoomed = [bool][AeroWinProbe.Native]::IsZoomed($Hwnd)
        $match = (($placement -eq 'Maximized') -eq $zoomed)
        [void]$Context.PlacementSamples.Add("label=$Label placement=$placement isZoomed=$zoomed match=$match")
    }
    catch {
        [void]$Context.PlacementSamples.Add("label=$Label error=$($_.Exception.Message)")
    }
}

function Get-SessionHsErrFiles {
    param([string]$Root)
    $list = @()
    foreach ($dir in @($Root, (Join-Path $Root 'showcase'))) {
        $list += @(Get-ChildItem -Path $dir -Filter 'hs_err_pid*.log' -File -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
    }
    return $list
}

function Get-SessionMainKtPids {
    <#
    .SYNOPSIS
        READ-ONLY: PIDs of every java.exe whose command line mentions the showcase main class.
        hotRun launches spawn more than one such JVM (the app plus hot-reload sidecars), so a
        launch's JVM set is always computed as (current set) minus (set snapshot before launch).
    #>
    @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and $_.CommandLine -match 'com\.mordred\.showcase\.MainKt' } |
        ForEach-Object { [uint32]$_.ProcessId })
}

function Stop-SessionTrackedJvms {
    <#
    .SYNOPSIS
        Kills exactly the given PIDs. The suite never sweeps by command-line pattern: a global
        sweep could kill a JVM this run did not start (e.g. the maintainer's own showcase) or a
        hotRun sidecar the pass still needs.
    #>
    param([uint32[]]$Pids)
    foreach ($p in @($Pids)) {
        Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
    }
}

function Start-SessionShowcase {
    param($Context, [Parameter(Mandatory = $true)][ValidateSet('run', 'hotRun')][string]$Task, [string[]]$ExtraProps = @())
    $logDir = Join-Path $Context.OutDir 'logs'
    $props = @($ExtraProps)
    if ($Context.Session.IsDryRun) { $props += '-Paero.capture=true' }
    return Start-WinProbeShowcase -Task $Task -GradleProps $props -LogDir $logDir -RepoRoot $Context.RepoRoot
}

function Stop-SessionLaunch {
    param($LaunchInfo, [IntPtr]$Hwnd, [uint32[]]$TrackedPids = @())
    if ($Hwnd -ne [IntPtr]::Zero -and $null -ne $LaunchInfo) {
        try { Stop-AeroProcessOfWindow -Hwnd $Hwnd } catch { }
        if ($LaunchInfo.Process) {
            $exited = $LaunchInfo.Process.WaitForExit(30000)
            if (-not $exited -and -not $LaunchInfo.Process.HasExited) {
                & taskkill /PID $LaunchInfo.Process.Id /T /F 2>$null | Out-Null
            }
        }
    }
    Stop-SessionTrackedJvms -Pids $TrackedPids
}

function Invoke-SessionV11 {
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd, [string]$Layout = 'main', [string]$LogPath, [string]$Label = 'main', [switch]$SkipMaximize)
    $results = Invoke-WinProbeV11 -Hwnd $Hwnd -Layout $Layout -LogPath $LogPath -Label $Label -SkipMaximize:$SkipMaximize
    $flat = @($results | ForEach-Object { "id=$($_.Id) result=$($_.Result) observed=$($_.Observed)" })
    $passCount = @($results | Where-Object { $_.Result -eq 'PASS' }).Count
    $failCount = @($results | Where-Object { $_.Result -eq 'FAIL' }).Count
    [pscustomobject]@{ Flat = $flat; PassCount = $passCount; FailCount = $failCount; Total = @($results).Count }
}

function Invoke-SessionCheck {
    <#
    .SYNOPSIS
        Runs one check body (param($ctx, $evidence, $frames)) in its own try/catch, resets both
        windows to known floating rects first (unless -NoReset), records {Id, Result, Evidence,
        Frames}, and prints `SESSION <Id> <Result> <detail>`. The body may return a single
        outcome (New-SessionOutcome) or an array of @{Id;Result;Detail} for compound checks.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Id,
        [Parameter(Mandatory = $true)][pscustomobject]$Context,
        [Parameter(Mandatory = $true)][scriptblock]$Body,
        [switch]$NoReset
    )
    if ($null -ne $script:CheckSubset -and -not $script:CheckSubset.Contains($Id)) { return }
    $Context.CurrentCheckId = $Id
    if (-not $NoReset) { Reset-SessionWindows -Context $Context }

    $evidence = New-Object System.Collections.Generic.List[string]
    $frames = New-Object System.Collections.Generic.List[string]
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $outcomes = $null
    try {
        $outcomes = & $Body $Context $evidence $frames
    }
    catch {
        $outcomes = $null
        [void]$evidence.Add("ERROR: $($_.Exception.Message)")
        Write-Host "SESSION_CHECK_ERROR $Id $($_.Exception.Message)"
    }
    $sw.Stop()

    $normalized = New-Object System.Collections.Generic.List[object]
    if ($null -eq $outcomes) {
        [void]$normalized.Add([pscustomobject]@{ Id = $Id; Result = 'UNCONFIRMED'; Detail = '' })
    }
    elseif ($outcomes -is [System.Array]) {
        foreach ($o in $outcomes) {
            $oId = if ($o.PSObject.Properties['Id']) { [string]$o.Id } else { $Id }
            [void]$normalized.Add([pscustomobject]@{ Id = $oId; Result = [string]$o.Result; Detail = [string]$o.Detail })
        }
    }
    else {
        [void]$normalized.Add([pscustomobject]@{ Id = $Id; Result = [string]$outcomes.Result; Detail = [string]$outcomes.Detail })
    }

    $produced = New-Object System.Collections.Generic.List[object]
    foreach ($o in $normalized) {
        $check = [pscustomobject]@{
            Id = $o.Id
            Result = $o.Result
            Evidence = @($evidence)
            Frames = @($frames)
            ElapsedMs = [int]$sw.Elapsed.TotalMilliseconds
        }
        [void]$script:CurrentChecks.Add($check)
        [void]$script:AllChecks.Add($check)
        [void]$produced.Add($check)
        $lastEvidence = ''
        if ($o.Detail -ne '') { $lastEvidence = $o.Detail }
        elseif ($evidence.Count -gt 0) { $lastEvidence = $evidence[$evidence.Count - 1] }
        Write-Host ("SESSION {0} {1} {2} ({3}ms)" -f $o.Id, $o.Result, $lastEvidence, $check.ElapsedMs)
    }
    return $produced.ToArray()
}

function Find-AeroMaxButtonSpan {
    <#
    .SYNOPSIS
        Scans -Hwnd's caption row via Invoke-WinProbeHitTest for the contiguous HTMAXBUTTON (9)
        span -- language independent, no window text is ever read. Same geometry as the early
        gate's helper (kept local because Invoke-EarlyGate.ps1 is a runnable script, not a
        dot-sourceable library): a real WS_CAPTION caption sits above the client origin, a
        client-drawn one below it, both rows are tried in order.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)
    $info = Get-WinProbeWindowInfo -Hwnd $Hwnd
    $origin = New-Object AeroWinProbe.WPoint
    [AeroWinProbe.Native]::ClientToScreen($Hwnd, [ref]$origin) | Out-Null
    $clientWidth = $info.ClientRect.Right - $info.ClientRect.Left
    $captionTopOffsetPx = $origin.Y - $info.WindowRect.Top

    $candidateRows = New-Object System.Collections.Generic.List[int]
    if ($captionTopOffsetPx -gt 0) {
        [void]$candidateRows.Add([int](-$captionTopOffsetPx / 2))
    }
    [void]$candidateRows.Add(10)

    foreach ($y in $candidateRows) {
        $start = $null
        $end = $null
        for ($x = 0; $x -lt $clientWidth; $x += 2) {
            $hit = Invoke-WinProbeHitTest -Hwnd $Hwnd -ClientX $x -ClientY $y
            if ($hit.Code -eq 9) {
                if ($null -eq $start) { $start = $x }
                $end = $x
            }
            elseif ($null -ne $start) {
                break
            }
        }
        if ($null -ne $start) {
            return [pscustomobject]@{ CenterX = [int](($start + $end) / 2); CenterY = $y }
        }
    }
    return $null
}

# ---------------------------------------------------------------------------
# Real-input composite helpers (every SendInput call stays behind $ctx.Session)
# ---------------------------------------------------------------------------

function Open-SessionSystemMenu {
    param($Context)
    Send-AeroKeyChord -Session $Context.Session -Keys @('Alt', 'Space')
    if (-not $Context.Session.IsDryRun) { Start-Sleep -Milliseconds 450 }
}

function Close-SessionSystemMenu {
    param($Context)
    Send-AeroKeyChord -Session $Context.Session -Keys @('Escape')
    if (-not $Context.Session.IsDryRun) { Start-Sleep -Milliseconds 250 }
}

function Invoke-SessionSystemMenuCommand {
    <#
    .SYNOPSIS
        Alt+Space, then navigates to a menu item and activates it with Enter. -ItemIndex is the
        0-based position from the first item (system menu order: Restore, Move, Size, Minimize,
        Maximize, Close); -1 selects the LAST item (Close) with End. Home/End/arrows are
        locale-independent, mnemonic letters are not.
    #>
    param($Context, [int]$ItemIndex)
    Send-AeroKeyChord -Session $Context.Session -Keys @('Alt', 'Space')
    if ($Context.Session.IsDryRun) { return }
    Start-Sleep -Milliseconds 450
    if ($ItemIndex -lt 0) {
        Send-AeroKeyChord -Session $Context.Session -Keys @('End')
    }
    else {
        Send-AeroKeyChord -Session $Context.Session -Keys @('Home')
        for ($i = 0; $i -lt $ItemIndex; $i++) {
            Send-AeroKeyChord -Session $Context.Session -Keys @('Down')
        }
    }
    Start-Sleep -Milliseconds 120
    Send-AeroKeyChord -Session $Context.Session -Keys @('Enter')
}

function Invoke-SessionSnapDragCheck {
    param($Context, $Evidence, [Parameter(Mandatory = $true)][string]$Label, [Parameter(Mandatory = $true)][int]$ToX, [Parameter(Mandatory = $true)][int]$ToY, $ExpectedRect)
    $g = Get-SessionGeometry -Hwnd $Context.MainHwnd
    $from = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
    [void]$Evidence.Add("drag caption ($($from.X),$($from.Y)) -> ($ToX,$ToY)")
    Invoke-AeroDrag -Session $Context.Session -FromX $from.X -FromY $from.Y -ToX $ToX -ToY $ToY -Steps 20 -DurationMs 700 -HoldMs 500
    if ($Context.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed' }
    Start-Sleep -Milliseconds 800
    Send-AeroKeyChord -Session $Context.Session -Keys @('Escape')
    $after = Get-SessionRect -Hwnd $Context.MainHwnd
    [void]$Evidence.Add("rect=$(ConvertTo-SessionRectString -Rect $after) expected=$(ConvertTo-SessionRectString -Rect $ExpectedRect)")
    Add-SessionPlacementSample -Context $Context -Hwnd $Context.MainHwnd -Label $Label
    if (Test-SessionRectMatch -Actual $after -Expected $ExpectedRect -Tolerance 8) {
        return New-SessionOutcome 'PASS' "rect=$(ConvertTo-SessionRectString -Rect $after)"
    }
    return New-SessionOutcome 'FAIL' "rect=$(ConvertTo-SessionRectString -Rect $after) expected=$(ConvertTo-SessionRectString -Rect $ExpectedRect)"
}

function Invoke-SessionHotkeyCheck {
    param($Context, $Evidence, [Parameter(Mandatory = $true)][string]$Label, [Parameter(Mandatory = $true)][string[]]$Keys, $ExpectedRect, [switch]$JudgeZoomed)
    $g = Get-SessionGeometry -Hwnd $Context.MainHwnd
    $from = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
    [void](Invoke-SessionFocus -Context $Context -Hwnd $Context.MainHwnd -ScreenX $from.X -ScreenY $from.Y -Evidence $Evidence)
    [void]$Evidence.Add("chord $($Keys -join '+')")
    Send-AeroKeyChord -Session $Context.Session -Keys $Keys
    if ($Context.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent' }
    Start-Sleep -Milliseconds 700
    Send-AeroKeyChord -Session $Context.Session -Keys @('Escape')
    Add-SessionPlacementSample -Context $Context -Hwnd $Context.MainHwnd -Label $Label
    if ($JudgeZoomed) {
        $zoomed = [bool][AeroWinProbe.Native]::IsZoomed($Context.MainHwnd)
        $rep = Get-WinProbeReporterState -LogPath $Context.LogPath -Label 'main'
        [void]$Evidence.Add("isZoomed=$zoomed placement=$(Get-ReporterField $rep 'placement')")
        if ($zoomed) { return New-SessionOutcome 'PASS' 'maximized' }
        return New-SessionOutcome 'FAIL' 'expected maximized, window not zoomed'
    }
    $after = Get-SessionRect -Hwnd $Context.MainHwnd
    [void]$Evidence.Add("rect=$(ConvertTo-SessionRectString -Rect $after) expected=$(ConvertTo-SessionRectString -Rect $ExpectedRect)")
    if (Test-SessionRectMatch -Actual $after -Expected $ExpectedRect -Tolerance 8) {
        return New-SessionOutcome 'PASS' "rect=$(ConvertTo-SessionRectString -Rect $after)"
    }
    return New-SessionOutcome 'FAIL' "rect=$(ConvertTo-SessionRectString -Rect $after) expected=$(ConvertTo-SessionRectString -Rect $ExpectedRect)"
}

function Invoke-SessionEdgeResizeCheck {
    param($Context, $Evidence, [Parameter(Mandatory = $true)][string]$PointName, [Parameter(Mandatory = $true)][string]$ExpectedShape, [Parameter(Mandatory = $true)][int]$Dx, [Parameter(Mandatory = $true)][int]$Dy)
    $g = Get-SessionGeometry -Hwnd $Context.MainHwnd
    $pt = Get-SessionScreenPoint -Geometry $g -PointName $PointName
    [void]$Evidence.Add("edge=$PointName point=($($pt.X),$($pt.Y)) expectShape=$ExpectedShape dragBy=($Dx,$Dy) expectGrowth=60±2px")
    Move-AeroCursor -Session $Context.Session -X $pt.X -Y $pt.Y
    if ($Context.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real hover/drag performed' }
    Start-Sleep -Milliseconds 300
    $shape = Get-AeroCursorShape
    $before = Get-SessionRect -Hwnd $Context.MainHwnd
    Invoke-AeroDrag -Session $Context.Session -FromX $pt.X -FromY $pt.Y -ToX ($pt.X + $Dx) -ToY ($pt.Y + $Dy) -Steps 12 -DurationMs 500 -HoldMs 200
    Start-Sleep -Milliseconds 300
    $after = Get-SessionRect -Hwnd $Context.MainHwnd
    $dw = ($after.Right - $after.Left) - ($before.Right - $before.Left)
    $dh = ($after.Bottom - $after.Top) - ($before.Bottom - $before.Top)
    [void]$Evidence.Add("shape=$shape dWidth=$dw dHeight=$dh")
    $shapeOk = ($shape -eq $ExpectedShape)
    # A corner drag (Dx and Dy both non-zero) grows both axes at once: each axis is judged
    # against its own dragged delta. A single-axis drag keeps the edge rule: the dragged axis
    # grows 60 +/- 2 while the untouched axis stays within 2 px.
    if (($Dx -ne 0) -and ($Dy -ne 0)) {
        $widthOk = [Math]::Abs([Math]::Abs($dw) - [Math]::Abs($Dx)) -le 2
        $heightOk = [Math]::Abs([Math]::Abs($dh) - [Math]::Abs($Dy)) -le 2
    }
    else {
        $widthOk = if ($Dx -ne 0) { ([Math]::Abs([Math]::Abs($dw) - 60) -le 2) -and ([Math]::Abs($dh) -le 2) } else { [Math]::Abs($dw) -le 2 }
        $heightOk = if ($Dy -ne 0) { ([Math]::Abs([Math]::Abs($dh) - 60) -le 2) -and ([Math]::Abs($dw) -le 2) } else { [Math]::Abs($dh) -le 2 }
    }
    if ($shapeOk -and $widthOk -and $heightOk) {
        return New-SessionOutcome 'PASS' "shape=$shape dWidth=$dw dHeight=$dh"
    }
    return New-SessionOutcome 'FAIL' "shape=$shape (expected $ExpectedShape) dWidth=$dw dHeight=$dh"
}

function Get-SessionReporterScale {
    param([string]$LogPath, [string]$Label = 'main')
    $rep = Get-WinProbeReporterState -LogPath $LogPath -Label $Label
    $raw = Get-ReporterField $rep 'scale'
    if ([string]::IsNullOrEmpty($raw)) { return 0.0 }
    return [double]::Parse($raw, [System.Globalization.CultureInfo]::InvariantCulture)
}

function Get-SessionReporterSizeDp {
    param([string]$LogPath, [string]$Label = 'main')
    $rep = Get-WinProbeReporterState -LogPath $LogPath -Label $Label
    $raw = Get-ReporterField $rep 'sizeDp'
    if ([string]::IsNullOrEmpty($raw)) { return $null }
    $parts = $raw -split 'x'
    if ($parts.Count -ne 2) { return $null }
    $culture = [System.Globalization.CultureInfo]::InvariantCulture
    return @{
        W = [double]::Parse($parts[0], $culture)
        H = [double]::Parse($parts[1], $culture)
    }
}

function Find-SessionTaskbarButtonRect {
    param([Parameter(Mandatory = $true)][string]$NameContains)
    $root = [System.Windows.Automation.AutomationElement]::RootElement
    $cond = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ClassNameProperty, 'Shell_TrayWnd')
    $tray = $root.FindFirst([System.Windows.Automation.TreeScope]::Children, $cond)
    if (-not $tray) { return $null }
    $items = $tray.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
    foreach ($item in $items) {
        $name = $item.Current.Name
        if ($name -and $name.Contains($NameContains)) {
            return $item.Current.BoundingRectangle
        }
    }
    return $null
}

function ConvertTo-SessionEnvFlat {
    param($State)
    $lines = New-Object System.Collections.Generic.List[string]
    [void]$lines.Add("screens=$($State.ScreenCount) autoHideOn=$($State.TaskbarAutoHideOn) powerToysInstalled=$($State.PowerToysInstalled) powerToysRunning=$($State.PowerToysRunning) fancyZonesRunning=$($State.FancyZonesRunning) virtualDisplayPresent=$($State.VirtualDisplayPresent)")
    foreach ($m in $State.Monitors) {
        [void]$lines.Add("monitor primary=$($m.IsPrimary) rect=$($m.MonitorRect.Left),$($m.MonitorRect.Top),$($m.MonitorRect.Right),$($m.MonitorRect.Bottom) work=$($m.WorkRect.Left),$($m.WorkRect.Top),$($m.WorkRect.Right),$($m.WorkRect.Bottom) dpi=$($m.EffectiveDpi)")
    }
    return @($lines)
}

# ---------------------------------------------------------------------------
# Environment phase
# ---------------------------------------------------------------------------

function Invoke-SessionEnvSetup {
    param($Session, [string]$DriverDir, [string]$Installer)
    Write-Host 'SESSION ENV-SETUP begin'
    $script:EnvSetupRan = $true
    $script:AutoHideOriginal = (Get-AeroEnvState).TaskbarAutoHideOn

    if ([string]::IsNullOrWhiteSpace($DriverDir)) {
        if ($Session.IsDryRun) {
            Add-AeroInputLog -Session $Session -Action 'VirtualDisplay install planned (devcon.exe install .\MttVDD.inf "Root\MttVDD", elevated) -- driver dir not supplied, dry run'
        }
        else {
            throw 'Invoke-SessionEnvSetup: -DriverDir is required for a real EnvSetup (the vetted MttVDD files, 22-SESSION-ENV.md)'
        }
    }
    else {
        $result = Install-AeroVirtualDisplay -Session $Session -DriverDir $DriverDir
        [void]$script:EnvSteps.Add("VirtualDisplay install: before=$($result.Before) after=$($result.After) verified=$($result.Verified)")
    }

    try {
        $scaleResult = Set-AeroDisplayScale -Session $Session -Percent 150
        [void]$script:EnvSteps.Add("DisplayScale 150 on virtual display: before=$($scaleResult.Before) after=$($scaleResult.After) verified=$($scaleResult.Verified)")
    }
    catch {
        # The plan's Task 3 contingency: when the virtual display is installed but never
        # attaches (no active non-primary monitor), the WIN-04 checks self-record UNCONFIRMED
        # and every other check continues; a scale failure must not abort the whole session.
        [void]$script:EnvSteps.Add("DisplayScale 150 FAILED: $($_.Exception.Message) -- virtual display unavailable, WIN-04 checks will be UNCONFIRMED")
        Write-Host "SESSION ENV-SETUP DISPLAY-SCALE ERROR (continuing): $($_.Exception.Message)"
    }

    if ([string]::IsNullOrWhiteSpace($Installer)) {
        if ($Session.IsDryRun) {
            Add-AeroInputLog -Session $Session -Action 'PowerToys install planned (PowerToysUserSetup /install /quiet /norestart) -- installer path not supplied, dry run'
        }
        else {
            throw 'Invoke-SessionEnvSetup: -PowerToysInstaller is required for a real EnvSetup (the vetted PowerToysUserSetup, 22-SESSION-ENV.md)'
        }
    }
    else {
        $ptResult = Install-AeroPowerToys -Session $Session -InstallerPath $Installer
        [void]$script:EnvSteps.Add("PowerToys install: before=$($ptResult.Before) after=$($ptResult.After) verified=$($ptResult.Verified)")
    }

    $fzResult = Start-AeroFancyZones -Session $Session
    [void]$script:EnvSteps.Add("FancyZones start: before=$($fzResult.Before) after=$($fzResult.After) verified=$($fzResult.Verified)")

    $autoHideResult = Set-AeroTaskbarAutoHide -Session $Session -Enabled $false
    [void]$script:EnvSteps.Add("TaskbarAutoHide off: before=$($autoHideResult.Before) after=$($autoHideResult.After) verified=$($autoHideResult.Verified)")

    Write-Host 'SESSION ENV-SETUP done'
}

# ---------------------------------------------------------------------------
# One pass (Jdk = :showcase:run, Jbr = :showcase:hotRun) with the full check suite
# ---------------------------------------------------------------------------

function Invoke-FullPassChecks {
    param(
        [Parameter(Mandatory = $true)][string]$PassName,
        [Parameter(Mandatory = $true)]$Session,
        [string]$RepoRoot,
        [string]$OutRoot,
        [int]$ReadyTimeoutSec = 300
    )
    $IsDryRun = [bool]$Session.IsDryRun
    $task = if ($PassName -eq 'jdk') { 'run' } else { 'hotRun' }
    $passDir = Join-Path $OutRoot $PassName
    $framesDir = Join-Path $passDir 'frames'
    New-Item -ItemType Directory -Path $framesDir -Force | Out-Null
    $script:CurrentChecks = New-Object System.Collections.Generic.List[object]

    $mainTitle = if ($IsDryRun) { 'aero-compose-ui Showcase [capture]' } else { 'aero-compose-ui Showcase' }
    $narrowTitle = if ($IsDryRun) { 'aero-compose-ui Queue [capture]' } else { 'aero-compose-ui Queue' }

    # The D-05 attribution line: AeroTitleBar reaches native code only through the public
    # rememberAeroWindowChrome API (Plan 19's grep gate), so every check below exercises API-04.
    $titleBarPath = Join-Path $RepoRoot 'library\src\main\kotlin\com\mordred\aero\components\navigation\AeroTitleBar.kt'
    $apiHits = @()
    $registryHits = @()
    if (Test-Path -LiteralPath $titleBarPath) {
        $apiHits = @(Select-String -LiteralPath $titleBarPath -Pattern 'rememberAeroWindowChrome' -SimpleMatch | Where-Object { $_.Line -notmatch '^\s*\*' })
        $registryHits = @(Select-String -LiteralPath $titleBarPath -Pattern 'NativeWindowChromeRegistry' -SimpleMatch | Where-Object { $_.Line -notmatch '^\s*\*' })
    }
    $apiPath = "AeroTitleBar -> rememberAeroWindowChrome (AeroTitleBar.kt code references rememberAeroWindowChrome=$($apiHits.Count), NativeWindowChromeRegistry=$($registryHits.Count))"
    Write-Host "SESSION PASS $PassName BEGIN ($task) api04path: $apiPath"

    $ctx = [pscustomobject]@{
        Session          = $Session
        RepoRoot         = $RepoRoot
        PassName         = $PassName
        OutDir           = $passDir
        FramesDir        = $framesDir
        CurrentCheckId   = ''
        AppPid           = 0
        MainHwnd         = [IntPtr]::Zero
        NarrowHwnd       = [IntPtr]::Zero
        LaunchInfo       = $null
        LogPath          = ''
        MainTitle        = $mainTitle
        NarrowTitle      = $narrowTitle
        ReferenceFlyout  = $null
        PlacementSamples = New-Object System.Collections.Generic.List[string]
        HsErrBefore      = @()
        LaunchJvmPids    = @()
    }

    $passSw = [System.Diagnostics.Stopwatch]::StartNew()
    $realSw = [System.Diagnostics.Stopwatch]::StartNew()
    $watchStarted = $false
    $launchJvmKind = ''
    $launchJvmPath = ''

    try {
        $mainKtBefore = Get-SessionMainKtPids
        $ctx.LaunchInfo = Start-SessionShowcase -Context $ctx -Task $task -ExtraProps @('-Paero.chromeTrace=true', '-Paero.secondWindow=true', '-Paero.secondWindowReopenMs=1500')
        $ctx.LogPath = $ctx.LaunchInfo.StdOut
        $ctx.MainHwnd = Find-AeroShowcaseWindow -Title $mainTitle -TimeoutSec $ReadyTimeoutSec
        $null = Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main' } -TimeoutSec $ReadyTimeoutSec
        $ctx.NarrowHwnd = Find-AeroShowcaseWindow -Title $narrowTitle -TimeoutSec $ReadyTimeoutSec
        $null = Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'narrow' } -TimeoutSec $ReadyTimeoutSec
        Start-Sleep -Milliseconds 500
        $ctx.LaunchJvmPids = @(Get-SessionMainKtPids | Where-Object { $mainKtBefore -notcontains $_ })

        $windowInfo = Get-WinProbeWindowInfo -Hwnd $ctx.MainHwnd
        $ctx.AppPid = [uint32]$windowInfo.ProcessId
        $launchJvmKind = $windowInfo.JvmKind
        $launchJvmPath = [string]$windowInfo.ProcessPath
        Write-Host "SESSION PASS $PassName jvm=$launchJvmKind path=$launchJvmPath pid=$($ctx.AppPid) main=0x$('{0:X}' -f $ctx.MainHwnd.ToInt64()) narrow=0x$('{0:X}' -f $ctx.NarrowHwnd.ToInt64())"
        if (($PassName -eq 'jdk' -and $launchJvmKind -ne 'standard') -or ($PassName -eq 'jbr' -and $launchJvmKind -ne 'JBR')) {
            throw "JVM kind mismatch for pass $PassName : expected $(if ($PassName -eq 'jdk') { 'standard' } else { 'JBR' }), running $launchJvmKind ($launchJvmPath)"
        }

        Start-AeroShellEventWatch -WatchedProcessIds @([uint32]$ctx.AppPid) | Out-Null
        $watchStarted = $true
        $ctx.HsErrBefore = Get-SessionHsErrFiles -Root $RepoRoot

        # ---------------- SNAP-01: drag-to-edge snapping ----------------
        Invoke-SessionCheck -Id 'S01-LEFT-HALF' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $midX = [int](($work.Left + $work.Right) / 2)
            $midY = [int](($work.Top + $work.Bottom) / 2)
            $expected = New-SessionWRect -Left $work.Left -Top $work.Top -Right $midX -Bottom $work.Bottom
            return Invoke-SessionSnapDragCheck -Context $ctx -Evidence $evidence -Label 'S01-LEFT-HALF' -ToX ($work.Left + 1) -ToY $midY -ExpectedRect $expected
        }

        Invoke-SessionCheck -Id 'S01-RIGHT-HALF' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $midX = [int](($work.Left + $work.Right) / 2)
            $midY = [int](($work.Top + $work.Bottom) / 2)
            $expected = New-SessionWRect -Left $midX -Top $work.Top -Right $work.Right -Bottom $work.Bottom
            return Invoke-SessionSnapDragCheck -Context $ctx -Evidence $evidence -Label 'S01-RIGHT-HALF' -ToX ($work.Right - 1) -ToY $midY -ExpectedRect $expected
        }

        Invoke-SessionCheck -Id 'S01-QUARTER-TL' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $midX = [int](($work.Left + $work.Right) / 2)
            $midY = [int](($work.Top + $work.Bottom) / 2)
            $expected = New-SessionWRect -Left $work.Left -Top $work.Top -Right $midX -Bottom $midY
            return Invoke-SessionSnapDragCheck -Context $ctx -Evidence $evidence -Label 'S01-QUARTER-TL' -ToX ($work.Left + 1) -ToY ($work.Top + 1) -ExpectedRect $expected
        }

        Invoke-SessionCheck -Id 'S01-TOP-MAXIMIZE' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $from = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $toX = [int](($work.Left + $work.Right) / 2)
            [void]$evidence.Add("drag caption -> ($toX,$($work.Top + 1)) expecting maximize")
            Invoke-AeroDrag -Session $ctx.Session -FromX $from.X -FromY $from.Y -ToX $toX -ToY ($work.Top + 1) -Steps 20 -DurationMs 700 -HoldMs 500
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed' }
            Start-Sleep -Milliseconds 800
            Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
            $zoomed = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            $rep = Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main'
            [void]$evidence.Add("isZoomed=$zoomed placement=$(Get-ReporterField $rep 'placement')")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S01-TOP-MAXIMIZE'
            if ($zoomed) { return New-SessionOutcome 'PASS' 'dragged to top edge, maximized' }
            return New-SessionOutcome 'FAIL' 'expected maximized after drag to top edge'
        }

        Invoke-SessionCheck -Id 'S01-DRAG-AWAY-RESTORE' -Context $ctx -NoReset -Body {
            param($ctx, $evidence, $frames)
            $before = Get-SessionRect -Hwnd $ctx.MainHwnd
            $beforeW = $before.Right - $before.Left
            $beforeH = $before.Bottom - $before.Top
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $from = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $centerX = [int](($mon.rcMonitor.Left + $mon.rcMonitor.Right) / 2)
            $centerY = [int](($mon.rcMonitor.Top + $mon.rcMonitor.Bottom) / 2)
            [void]$evidence.Add("state-before=($beforeW x $beforeH); dragging away to ($centerX,$centerY) expecting restore to 1200x800±2")
            Invoke-AeroDrag -Session $ctx.Session -FromX $from.X -FromY $from.Y -ToX $centerX -ToY $centerY -Steps 20 -DurationMs 700 -HoldMs 200
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed' }
            Start-Sleep -Milliseconds 600
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            $afterW = $after.Right - $after.Left
            $afterH = $after.Bottom - $after.Top
            [void]$evidence.Add("restored=${afterW}x${afterH}")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S01-DRAG-AWAY-RESTORE'
            if (([Math]::Abs($afterW - 1200) -le 2) -and ([Math]::Abs($afterH - 800) -le 2)) {
                return New-SessionOutcome 'PASS' "restored=${afterW}x${afterH}"
            }
            return New-SessionOutcome 'FAIL' "restored=${afterW}x${afterH}, expected 1200x800±2"
        }

        # ---------------- SNAP-02: Snap Layouts flyout ----------------
        Invoke-SessionCheck -Id 'S02-FLYOUT' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $pcInfo = $null
            $pcHwnd = [IntPtr]::Zero
            $reference = $null
            try {
                $pcInfo = Start-AeroPositiveControlWindow -NoActivate:$ctx.Session.IsDryRun
                Add-AeroShellEventWatchProcessId -ProcessId ([uint32]$pcInfo.Process.Id)
                $pcHwnd = Find-AeroShowcaseWindow -Title $pcInfo.Title -TimeoutSec 10
                $maxSpan = Find-AeroMaxButtonSpan -Hwnd $pcHwnd
                if (-not $maxSpan) { throw 'positive-control HTMAXBUTTON span not found on its caption row' }
                $pcOrigin = New-Object AeroWinProbe.WPoint
                [AeroWinProbe.Native]::ClientToScreen($pcHwnd, [ref]$pcOrigin) | Out-Null
                [void]$evidence.Add("positiveControl maxSpan=($($maxSpan.CenterX),$($maxSpan.CenterY))")

                if ($ctx.Session.IsDryRun) {
                    Invoke-AeroClick -Session $ctx.Session -X ($pcOrigin.X + 20) -Y ($pcOrigin.Y + 10)
                    Move-AeroCursor -Session $ctx.Session -X ($pcOrigin.X + $maxSpan.CenterX) -Y ($pcOrigin.Y + $maxSpan.CenterY)
                    Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
                }
                else {
                    Invoke-AeroClick -Session $ctx.Session -X ($pcOrigin.X + 20) -Y ($pcOrigin.Y + 10)
                    $baseline = Get-AeroShellSnapshot
                    Move-AeroCursor -Session $ctx.Session -X ($pcOrigin.X + $maxSpan.CenterX) -Y ($pcOrigin.Y + $maxSpan.CenterY)
                    $reference = Wait-AeroSnapFlyout -Baseline $baseline -TimeoutMs 3000 -MinHoverMs 1500 -PollMs 100
                    $refEvidence = ConvertTo-AeroFlyoutEvidence $reference
                    [void]$evidence.Add("referenceFound=$($refEvidence.Found) signal=$($refEvidence.Signal) delayMs=$($refEvidence.DelayMs)")
                    Move-AeroCursor -Session $ctx.Session -X ($pcOrigin.X - 100) -Y ($pcOrigin.Y - 100)
                    Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
                }
            }
            finally {
                if ($pcHwnd -ne [IntPtr]::Zero) { try { Stop-AeroProcessOfWindow -Hwnd $pcHwnd } catch { } }
                elseif ($pcInfo) { Stop-AeroPositiveControlWindow -Info $pcInfo }
            }

            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
            Invoke-AeroClick -Session $ctx.Session -X $caption.X -Y $caption.Y
            if ($ctx.Session.IsDryRun) {
                $baseline = Get-AeroShellSnapshot
                Move-AeroCursor -Session $ctx.Session -X $max.X -Y $max.Y
                $observed = Wait-AeroSnapFlyout -Baseline $baseline -TimeoutMs 3000 -MinHoverMs 1500 -PollMs 100
                $obsEvidence = ConvertTo-AeroFlyoutEvidence $observed
                [void]$evidence.Add("composeFound=$($obsEvidence.Found) signal=$($obsEvidence.Signal)")
                Move-AeroCursor -Session $ctx.Session -X ($max.X - 150) -Y ($max.Y + 150)
                Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real hover performed'
            }
            $baseline2 = Get-AeroShellSnapshot
            Move-AeroCursor -Session $ctx.Session -X $max.X -Y $max.Y
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $observed2 = Wait-AeroSnapFlyout -Baseline $baseline2 -TimeoutMs 3000 -MinHoverMs 1500 -PollMs 100 -MonitorRect $mon.rcMonitor
            $obs2Evidence = ConvertTo-AeroFlyoutEvidence $observed2
            [void]$evidence.Add("composeFound=$($obs2Evidence.Found) signal=$($obs2Evidence.Signal) delayMs=$($obs2Evidence.DelayMs) newWindows=$($obs2Evidence.NewWindows -join ';') events=$($obs2Evidence.Events.EventName -join ';')")
            Move-AeroCursor -Session $ctx.Session -X ($max.X - 150) -Y ($max.Y + 150)
            Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')

            if (-not $reference -or -not $reference.Found) {
                return New-SessionOutcome 'UNCONFIRMED' 'positive control produced no reference flyout (D-04: do not downgrade silently)'
            }
            $match = Test-AeroFlyoutSignatureMatch -Observed $observed2 -Reference $reference
            [void]$evidence.Add("flyoutMatch=$($match.IsMatch) matched=$($match.MatchedSignatures -join ';')")
            if ($match.IsMatch) {
                $ctx.ReferenceFlyout = $reference
                return New-SessionOutcome 'PASS' "matched reference via $($obs2Evidence.Signal) after $($obs2Evidence.DelayMs)ms"
            }
            return New-SessionOutcome 'FAIL' "flyout signature did not match the positive control (observed=$($match.ObservedSignatures -join ';'))"
        }

        Invoke-SessionCheck -Id 'S02-LAYOUT-PICK' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
            Invoke-AeroClick -Session $ctx.Session -X $caption.X -Y $caption.Y
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $midX = [int](($work.Left + $work.Right) / 2)
            $midY = [int](($work.Top + $work.Bottom) / 2)
            if ($ctx.Session.IsDryRun) {
                Move-AeroCursor -Session $ctx.Session -X $max.X -Y $max.Y
                [void]$evidence.Add('planned: hover max button, wait for flyout, click first zone element, expect window rect on a work-area half/quarter boundary')
                Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real hover/click performed'
            }
            $before = Get-SessionRect -Hwnd $ctx.MainHwnd
            $baseline = Get-AeroShellSnapshot
            Move-AeroCursor -Session $ctx.Session -X $max.X -Y $max.Y
            $flyout = Wait-AeroSnapFlyout -Baseline $baseline -TimeoutMs 3000 -MinHoverMs 1500 -PollMs 100 -MonitorRect $mon.rcMonitor
            if (-not $flyout.Found) {
                Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
                return New-SessionOutcome 'FAIL' 'no flyout appeared on hover, no zone to pick'
            }
            $zoneCandidate = $null
            foreach ($elem in @($flyout.NewUiaElements)) {
                foreach ($d in @($elem.Descendants)) {
                    if (($d.ControlType -eq 'ControlType.Button' -or $d.ControlType -eq 'ControlType.ListItem') -and $d.BoundingRectangle.Width -gt 0) {
                        $zoneCandidate = $d
                        break
                    }
                }
                if ($zoneCandidate) { break }
            }
            if (-not $zoneCandidate) {
                Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
                return New-SessionOutcome 'UNCONFIRMED' 'UIA cannot see a zone element in the flyout descendants (no click target)'
            }
            $zoneRect = $zoneCandidate.BoundingRectangle
            $zx = [int]($zoneRect.X + $zoneRect.Width / 2)
            $zy = [int]($zoneRect.Y + $zoneRect.Height / 2)
            [void]$evidence.Add("zoneElement type=$($zoneCandidate.ControlType) automationId=$($zoneCandidate.AutomationId) center=($zx,$zy)")
            Invoke-AeroClick -Session $ctx.Session -X $zx -Y $zy
            Start-Sleep -Milliseconds 1500
            Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            [void]$evidence.Add("rect=$(ConvertTo-SessionRectString -Rect $after)")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S02-LAYOUT-PICK'
            $onGrid = (
                (([Math]::Abs($after.Left - $work.Left) -le 8) -or ([Math]::Abs($after.Left - $midX) -le 8)) -and
                (([Math]::Abs($after.Right - $work.Right) -le 8) -or ([Math]::Abs($after.Right - $midX) -le 8)) -and
                (([Math]::Abs($after.Top - $work.Top) -le 8) -or ([Math]::Abs($after.Top - $midY) -le 8)) -and
                (([Math]::Abs($after.Bottom - $work.Bottom) -le 8) -or ([Math]::Abs($after.Bottom - $midY) -le 8))
            )
            $moved = -not (Test-SessionRectMatch -Actual $after -Expected $before -Tolerance 8)
            if ($onGrid -and $moved) { return New-SessionOutcome 'PASS' 'zone pick placed the window on a work-area half/quarter' }
            return New-SessionOutcome 'FAIL' "rect=$(ConvertTo-SessionRectString -Rect $after) not on a work-area half/quarter boundary (or unmoved)"
        }

        # ---------------- SNAP-03: Win+arrow hotkeys ----------------
        Invoke-SessionCheck -Id 'S03-WIN-LEFT' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $expected = New-SessionWRect -Left $work.Left -Top $work.Top -Right ([int](($work.Left + $work.Right) / 2)) -Bottom $work.Bottom
            return Invoke-SessionHotkeyCheck -Context $ctx -Evidence $evidence -Label 'S03-WIN-LEFT' -Keys @('LWin', 'Left') -ExpectedRect $expected
        }

        Invoke-SessionCheck -Id 'S03-WIN-RIGHT' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $expected = New-SessionWRect -Left ([int](($work.Left + $work.Right) / 2)) -Top $work.Top -Right $work.Right -Bottom $work.Bottom
            return Invoke-SessionHotkeyCheck -Context $ctx -Evidence $evidence -Label 'S03-WIN-RIGHT' -Keys @('LWin', 'Right') -ExpectedRect $expected
        }

        Invoke-SessionCheck -Id 'S03-WIN-UP' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            return Invoke-SessionHotkeyCheck -Context $ctx -Evidence $evidence -Label 'S03-WIN-UP' -Keys @('LWin', 'Up') -JudgeZoomed
        }

        Invoke-SessionCheck -Id 'S03-WIN-DOWN' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            # The zoomed precondition is established with a probe SC_MAXIMIZE (the W01/F9
            # precedent: SendMessageTimeoutW posts no input and never steals foreground) instead
            # of this check sending its own Win+Up, whose effect raced an earlier restore when
            # the standalone S03-WIN-UP had passed seconds before.
            [void][AeroWinProbe.Native]::SendMessageTimeoutW($ctx.MainHwnd, [AeroWinProbe.Native]::WM_SYSCOMMAND, [IntPtr][AeroWinProbe.Native]::SC_MAXIMIZE, [IntPtr]::Zero, [AeroWinProbe.Native]::SMTO_ABORTIFHUNG, 2000, [ref]([IntPtr]::Zero))
            if ($ctx.Session.IsDryRun) {
                Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Down')
                Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Down')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent'
            }
            Start-Sleep -Milliseconds 700
            $zoomedPrecondition = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            if (-not $zoomedPrecondition) {
                return New-SessionOutcome 'UNCONFIRMED' 'precondition failed: the SC_MAXIMIZE probe did not zoom the window'
            }
            Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Down')
            Start-Sleep -Milliseconds 700
            $restored = -not [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Down')
            Start-Sleep -Milliseconds 700
            $minimized = [bool][AeroWinProbe.Native]::IsIconic($ctx.MainHwnd)
            [void]$evidence.Add("zoomedPrecondition=$zoomedPrecondition restoredAfterFirstDown=$restored minimizedAfterSecondDown=$minimized")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S03-WIN-DOWN'
            if ($minimized) {
                [void][AeroWinProbe.Native]::ShowWindow($ctx.MainHwnd, [AeroWinProbe.Native]::SW_SHOWNOACTIVATE)
                Start-Sleep -Milliseconds 300
            }
            if ($restored -and $minimized) { return New-SessionOutcome 'PASS' 'Win+Down restored then minimized (precondition set by the SC_MAXIMIZE probe)' }
            return New-SessionOutcome 'FAIL' "restored=$restored minimized=$minimized"
        }

        # ---------------- SNAP-04: double-click caption ----------------
        Invoke-SessionCheck -Id 'S04-DBLCLICK-MAX' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            Invoke-AeroDoubleClick -Session $ctx.Session -X $caption.X -Y $caption.Y
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real double-click performed' }
            Start-Sleep -Milliseconds 600
            $zoomed = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            [void]$evidence.Add("isZoomed=$zoomed")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S04-DBLCLICK-MAX'
            if ($zoomed) { return New-SessionOutcome 'PASS' 'double-click maximized' }
            return New-SessionOutcome 'FAIL' 'expected maximized after caption double-click'
        }

        Invoke-SessionCheck -Id 'S04-DBLCLICK-RESTORE' -Context $ctx -NoReset -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            Invoke-AeroDoubleClick -Session $ctx.Session -X $caption.X -Y $caption.Y
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real double-click performed' }
            Start-Sleep -Milliseconds 600
            $zoomed = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            $w = $after.Right - $after.Left
            $h = $after.Bottom - $after.Top
            [void]$evidence.Add("isZoomed=$zoomed size=${w}x${h}")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S04-DBLCLICK-RESTORE'
            if ((-not $zoomed) -and ([Math]::Abs($w - 1200) -le 2) -and ([Math]::Abs($h - 800) -le 2)) {
                return New-SessionOutcome 'PASS' "restored ${w}x${h}"
            }
            return New-SessionOutcome 'FAIL' "isZoomed=$zoomed size=${w}x${h}, expected floating 1200x800±2"
        }

        # ---------------- SNAP-05: system menu ----------------
        Invoke-SessionCheck -Id 'S05-ALTSPACE-MENU' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            Open-SessionSystemMenu -Context $ctx
            if ($ctx.Session.IsDryRun) {
                Close-SessionSystemMenu -Context $ctx
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent'
            }
            $menuVisible = Test-SessionSystemMenuVisible -AppPid ([uint32]$ctx.AppPid)
            [void]$evidence.Add("#32768-of-our-process=$menuVisible")
            Close-SessionSystemMenu -Context $ctx
            if ($menuVisible) { return New-SessionOutcome 'PASS' 'system menu window (#32768) of our process appeared' }
            return New-SessionOutcome 'FAIL' 'no #32768 window of our process after Alt+Space'
        }

        Invoke-SessionCheck -Id 'S05-MOVE' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            $before = Get-SessionRect -Hwnd $ctx.MainHwnd
            Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 1
            if ($ctx.Session.IsDryRun) {
                for ($i = 0; $i -lt 20; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
                Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent'
            }
            Start-Sleep -Milliseconds 300
            for ($i = 0; $i -lt 20; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
            Start-Sleep -Milliseconds 200
            Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
            Start-Sleep -Milliseconds 400
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            $dx = $after.Left - $before.Left
            $dy = $after.Top - $before.Top
            [void]$evidence.Add("moved dx=$dx dy=$dy (expected dx=20±4 dy=0±2)")
            if (([Math]::Abs($dx - 20) -le 4) -and ([Math]::Abs($dy) -le 2)) { return New-SessionOutcome 'PASS' "moved dx=$dx dy=$dy" }
            return New-SessionOutcome 'FAIL' "moved dx=$dx dy=$dy, expected dx=20±4 dy=0±2"
        }

        Invoke-SessionCheck -Id 'S05-SIZE' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            $before = Get-SessionRect -Hwnd $ctx.MainHwnd
            Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 2
            if ($ctx.Session.IsDryRun) {
                for ($i = 0; $i -lt 25; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
                Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent'
            }
            Start-Sleep -Milliseconds 300
            for ($i = 0; $i -lt 25; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
            Start-Sleep -Milliseconds 200
            Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
            Start-Sleep -Milliseconds 400
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            $dw = ($after.Right - $after.Left) - ($before.Right - $before.Left)
            $dh = ($after.Bottom - $after.Top) - ($before.Bottom - $before.Top)
            [void]$evidence.Add("resized dWidth=$dw dHeight=$dh (first Right selects the edge, the rest grow it)")
            if ($dw -ge 12 -and $dw -le 28 -and [Math]::Abs($dh) -le 2) { return New-SessionOutcome 'PASS' "resized dWidth=$dw" }
            return New-SessionOutcome 'FAIL' "resized dWidth=$dw dHeight=$dh"
        }

        Invoke-SessionCheck -Id 'S05-MINIMIZE' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 3
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent' }
            Start-Sleep -Milliseconds 600
            $iconic = [bool][AeroWinProbe.Native]::IsIconic($ctx.MainHwnd)
            $rep = Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main'
            [void]$evidence.Add("isIconic=$iconic minimized=$(Get-ReporterField $rep 'minimized')")
            if ($iconic) {
                [void][AeroWinProbe.Native]::ShowWindow($ctx.MainHwnd, [AeroWinProbe.Native]::SW_SHOWNOACTIVATE)
                Start-Sleep -Milliseconds 300
            }
            if ($iconic) { return New-SessionOutcome 'PASS' 'system menu Minimize minimized the window' }
            return New-SessionOutcome 'FAIL' 'window not minimized after system menu Minimize'
        }

        Invoke-SessionCheck -Id 'S05-MAXIMIZE' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 4
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent' }
            Start-Sleep -Milliseconds 600
            $zoomed = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            $rep = Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main'
            [void]$evidence.Add("isZoomed=$zoomed placement=$(Get-ReporterField $rep 'placement')")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S05-MAXIMIZE'
            if ($zoomed) { return New-SessionOutcome 'PASS' 'system menu Maximize maximized the window' }
            return New-SessionOutcome 'FAIL' 'window not maximized after system menu Maximize'
        }

        Invoke-SessionCheck -Id 'S05-RESTORE' -Context $ctx -NoReset -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 0
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent' }
            Start-Sleep -Milliseconds 600
            $zoomed = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            $w = $after.Right - $after.Left
            $h = $after.Bottom - $after.Top
            [void]$evidence.Add("isZoomed=$zoomed size=${w}x${h}")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S05-RESTORE'
            if ((-not $zoomed) -and ([Math]::Abs($w - 1200) -le 2) -and ([Math]::Abs($h - 800) -le 2)) {
                return New-SessionOutcome 'PASS' "restored ${w}x${h}"
            }
            return New-SessionOutcome 'FAIL' "isZoomed=$zoomed size=${w}x${h}, expected floating 1200x800±2"
        }

        Invoke-SessionCheck -Id 'S05-CLOSE-VS-ALTF4' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.NarrowHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            $mark1 = @(Get-SessionLogLines -LogPath $ctx.LogPath).Count
            Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex -1
            if ($ctx.Session.IsDryRun) {
                Send-AeroKeyChord -Session $ctx.Session -Keys @('Alt', 'F4')
                [void]$evidence.Add('planned: menu Close -> expect AERO_EVENT name=close-request label=narrow + window destroyed; fixture reopens; then Alt+F4 -> same event again')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent'
            }
            Start-Sleep -Milliseconds 800
            $event1 = Wait-SessionEventAfter -LogPath $ctx.LogPath -Name 'close-request' -Detail 'label=narrow' -AfterLineCount $mark1 -TimeoutSec 5
            [void]$evidence.Add("menuCloseEvent=$([bool]$event1)")
            $gone1 = -not (Test-SessionWindowAlive -Hwnd $ctx.NarrowHwnd)
            [void]$evidence.Add("narrowGoneAfterMenuClose=$gone1")
            $ctx.NarrowHwnd = Wait-SessionWindowByTitle -Title $ctx.NarrowTitle -TimeoutSec 15
            Start-Sleep -Milliseconds 500
            $g2 = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $caption2 = Get-SessionScreenPoint -Geometry $g2 -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.NarrowHwnd -ScreenX $caption2.X -ScreenY $caption2.Y -Evidence $evidence)
            $mark2 = @(Get-SessionLogLines -LogPath $ctx.LogPath).Count
            Send-AeroKeyChord -Session $ctx.Session -Keys @('Alt', 'F4')
            Start-Sleep -Milliseconds 800
            $event2 = Wait-SessionEventAfter -LogPath $ctx.LogPath -Name 'close-request' -Detail 'label=narrow' -AfterLineCount $mark2 -TimeoutSec 5
            [void]$evidence.Add("altF4Event=$([bool]$event2)")
            $gone2 = -not (Test-SessionWindowAlive -Hwnd $ctx.NarrowHwnd)
            [void]$evidence.Add("narrowGoneAfterAltF4=$gone2")
            $ctx.NarrowHwnd = Wait-SessionWindowByTitle -Title $ctx.NarrowTitle -TimeoutSec 15
            Start-Sleep -Milliseconds 500
            if ($event1 -and $gone1 -and $event2 -and $gone2) {
                return New-SessionOutcome 'PASS' 'both close paths produced AERO_EVENT name=close-request label=narrow on the same window'
            }
            return New-SessionOutcome 'FAIL' "menuClose=$([bool]$event1)/$gone1 altF4=$([bool]$event2)/$gone2"
        }

        # ---------------- C2 comparison + API-04 opt-out RED control (own short launch) ----------------
        Invoke-SessionCheck -Id 'C02-OPTOUT-ALTSPACE' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $optTitle = if ($ctx.Session.IsDryRun) { 'aero-compose-ui Showcase [capture]' } else { 'aero-compose-ui Showcase' }
            $opt = $null
            $optHwnd = [IntPtr]::Zero
            $optPids = @()
            $c02Outcome = $null
            $a04Outcome = $null
            try {
                $mainKtBeforeOpt = Get-SessionMainKtPids
                $opt = Start-SessionShowcase -Context $ctx -Task 'run' -ExtraProps @('-Paero.chromeTrace=true', '-Paero.nativeChrome=false')
                $optLog = $opt.StdOut
                $optHwnd = Wait-SessionNewShowcaseWindow -Title $optTitle -ExcludeHwnd @($ctx.MainHwnd, $ctx.NarrowHwnd) -TimeoutSec $ReadyTimeoutSec
                $null = Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $optLog -Label 'main' } -TimeoutSec $ReadyTimeoutSec
                $optPids = @(Get-SessionMainKtPids | Where-Object { $mainKtBeforeOpt -notcontains $_ })
                Start-Sleep -Milliseconds 500
                $optInfo = Get-WinProbeWindowInfo -Hwnd $optHwnd
                [void]$evidence.Add("optout jvm=$($optInfo.JvmKind) style=$($optInfo.StyleHex) names=$($optInfo.StyleNames -join ',')")
                $noChromeLines = @(Get-SessionLogLines -LogPath $optLog | Where-Object { $_ -like 'AERO_CHROME*' })
                [void]$evidence.Add("optout chrome trace lines=$($noChromeLines.Count) (0 = no restyle/install/child-subclass, the API-04 opt-out semantics)")

                # A04-OPTOUT: the permanent RED control -- every V11 check must FAIL on the opt-out.
                $v11 = Invoke-SessionV11 -Hwnd $optHwnd -Layout 'main' -LogPath $optLog -Label 'main'
                foreach ($line in $v11.Flat) { [void]$evidence.Add("v11 $line") }
                $a04Ok = ($v11.PassCount -eq 0)
                $a04Outcome = [pscustomobject]@{ Id = 'A04-OPTOUT'; Result = $(if ($a04Ok) { 'PASS' } else { 'FAIL' }); Detail = "RED control: v11 pass=$($v11.PassCount) fail=$($v11.FailCount) (expect pass=0)" }

                # C02's own before/after observations: menu, Move, Size on the opt-out window.
                $g = Get-SessionGeometry -Hwnd $optHwnd
                $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
                [void](Invoke-SessionFocus -Context $ctx -Hwnd $optHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
                if ($ctx.Session.IsDryRun) {
                    Open-SessionSystemMenu -Context $ctx
                    Close-SessionSystemMenu -Context $ctx
                    Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 1
                    for ($i = 0; $i -lt 20; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
                    Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
                    Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 2
                    for ($i = 0; $i -lt 25; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
                    Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
                    $c02Outcome = New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent'
                }
                else {
                    [uint32]$optPid = 0
                    [AeroWinProbe.Native]::GetWindowThreadProcessId($optHwnd, [ref]$optPid) | Out-Null
                    $before = Get-SessionRect -Hwnd $optHwnd
                    Open-SessionSystemMenu -Context $ctx
                    $menuVisible = Test-SessionSystemMenuVisible -AppPid $optPid
                    Close-SessionSystemMenu -Context $ctx
                    Start-Sleep -Milliseconds 300
                    Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 1
                    Start-Sleep -Milliseconds 300
                    for ($i = 0; $i -lt 20; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
                    Start-Sleep -Milliseconds 200
                    Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
                    Start-Sleep -Milliseconds 400
                    $afterMove = Get-SessionRect -Hwnd $optHwnd
                    $moveDx = $afterMove.Left - $before.Left
                    $before2 = Get-SessionRect -Hwnd $optHwnd
                    Invoke-SessionSystemMenuCommand -Context $ctx -ItemIndex 2
                    Start-Sleep -Milliseconds 300
                    for ($i = 0; $i -lt 25; $i++) { Send-AeroKeyChord -Session $ctx.Session -Keys @('Right') }
                    Start-Sleep -Milliseconds 200
                    Send-AeroKeyChord -Session $ctx.Session -Keys @('Enter')
                    Start-Sleep -Milliseconds 400
                    $afterSize = Get-SessionRect -Hwnd $optHwnd
                    $sizeDw = ($afterSize.Right - $afterSize.Left) - ($before2.Right - $before2.Left)
                    [void]$evidence.Add("optout menuVisible=$menuVisible moveDx=$moveDx sizeDw=$sizeDw (C2 comparison observations)")
                    $c02Outcome = New-SessionOutcome 'PASS' "optout menu=$menuVisible moveDx=$moveDx sizeDw=$sizeDw recorded"
                }
            }
            finally {
                if ($optHwnd -ne [IntPtr]::Zero -and $opt) {
                    try { Stop-AeroProcessOfWindow -Hwnd $optHwnd } catch { }
                    if ($opt.Process) {
                        $exited = $opt.Process.WaitForExit(30000)
                        if (-not $exited -and -not $opt.Process.HasExited) {
                            & taskkill /PID $opt.Process.Id /T /F 2>$null | Out-Null
                        }
                    }
                }
                Stop-SessionTrackedJvms -Pids $optPids
            }
            return @($c02Outcome, $a04Outcome)
        }

        # ---------------- SNAP-06: shared border + snap group ----------------
        Invoke-SessionCheck -Id 'S06-SHARED-BORDER' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
            $work = $mon.rcWork
            $midX = [int](($work.Left + $work.Right) / 2)
            $midY = [int](($work.Top + $work.Bottom) / 2)
            $gMain = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $capMain = Get-SessionScreenPoint -Geometry $gMain -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $capMain.X -ScreenY $capMain.Y -Evidence $evidence)
            Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Left')
            $gNarrow = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $capNarrow = Get-SessionScreenPoint -Geometry $gNarrow -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.NarrowHwnd -ScreenX $capNarrow.X -ScreenY $capNarrow.Y -Evidence $evidence)
            Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Right')
            if ($ctx.Session.IsDryRun) {
                Invoke-AeroDrag -Session $ctx.Session -FromX $midX -FromY $midY -ToX ($midX + 100) -ToY $midY -Steps 10 -DurationMs 400 -HoldMs 200
                [void]$evidence.Add('planned: drag shared border at (' + $midX + ',' + $midY + ') by +100px; expect main.Right and narrow.Left both +100±8')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys/drag performed'
            }
            Start-Sleep -Milliseconds 800
            $mainBefore = Get-SessionRect -Hwnd $ctx.MainHwnd
            $narrowBefore = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            [void]$evidence.Add("mainBefore=$(ConvertTo-SessionRectString -Rect $mainBefore) narrowBefore=$(ConvertTo-SessionRectString -Rect $narrowBefore)")
            Invoke-AeroDrag -Session $ctx.Session -FromX $midX -FromY $midY -ToX ($midX + 100) -ToY $midY -Steps 10 -DurationMs 400 -HoldMs 200
            Start-Sleep -Milliseconds 600
            $mainAfter = Get-SessionRect -Hwnd $ctx.MainHwnd
            $narrowAfter = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            $mainRightDelta = $mainAfter.Right - $mainBefore.Right
            $narrowLeftDelta = $narrowAfter.Left - $narrowBefore.Left
            [void]$evidence.Add("mainRightDelta=$mainRightDelta narrowLeftDelta=$narrowLeftDelta (both expected +100±8)")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S06-SHARED-BORDER'
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.NarrowHwnd -Label 'S06-SHARED-BORDER'
            if (([Math]::Abs($mainRightDelta - 100) -le 8) -and ([Math]::Abs($narrowLeftDelta - 100) -le 8)) {
                return New-SessionOutcome 'PASS' "shared border moved together: mainRight +$mainRightDelta, narrowLeft +$narrowLeftDelta"
            }
            return New-SessionOutcome 'FAIL' "mainRightDelta=$mainRightDelta narrowLeftDelta=$narrowLeftDelta"
        }

        Invoke-SessionCheck -Id 'S06-SNAP-GROUP' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $tb = Get-WinProbeTaskbar -Hwnd $ctx.MainHwnd
            $autoHideChanged = $false
            if ($tb.AutoHideOn) {
                $null = Set-AeroTaskbarAutoHide -Session $ctx.Session -Enabled $false
                $autoHideChanged = $true
                [void]$evidence.Add('taskbar auto-hide turned off for this check (restored below)')
            }
            try {
                $buttonRect = $null
                try { $buttonRect = Find-SessionTaskbarButtonRect -NameContains 'aero-compose-ui Showcase' } catch { [void]$evidence.Add("taskbar UIA walk failed: $($_.Exception.Message)") }
                if (-not $buttonRect) {
                    return New-SessionOutcome 'UNCONFIRMED' 'taskbar button not found via UIA (cannot hover what cannot be located)'
                }
                $bx = [int]($buttonRect.X + $buttonRect.Width / 2)
                $by = [int]($buttonRect.Y + $buttonRect.Height / 2)
                [void]$evidence.Add("taskbarButton=($bx,$by)")
                Move-AeroCursor -Session $ctx.Session -X $bx -Y $by
                if ($ctx.Session.IsDryRun) {
                    [void]$evidence.Add('planned: hover taskbar button, watch for TaskListThumbnailWnd, UIA-count grouped thumbnails')
                    return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real hover performed'
                }
                $thumb = $null
                $deadline = (Get-Date).AddSeconds(2)
                while ((Get-Date) -lt $deadline) {
                    foreach ($hwnd in [AeroSnapWatch.Native]::EnumTopLevelAll()) {
                        if ([AeroSnapWatch.Native]::GetClassName($hwnd) -eq 'TaskListThumbnailWnd' -and [AeroSnapWatch.Native]::IsWindowVisible($hwnd)) { $thumb = $hwnd; break }
                    }
                    if ($thumb) { break }
                    Start-Sleep -Milliseconds 100
                }
                Move-AeroCursor -Session $ctx.Session -X 960 -Y 500
                if (-not $thumb) {
                    return New-SessionOutcome 'UNCONFIRMED' 'no TaskListThumbnailWnd appeared within 2s of the hover'
                }
                $items = 0
                try {
                    $element = [System.Windows.Automation.AutomationElement]::FromHandle($thumb)
                    $found = $element.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
                    $items = $found.Count
                }
                catch {
                    [void]$evidence.Add("thumbnail UIA walk failed: $($_.Exception.Message)")
                }
                [void]$evidence.Add("TaskListThumbnailWnd=0x$('{0:X}' -f $thumb.ToInt64()) descendants=$items")
                if ($items -ge 2) { return New-SessionOutcome 'PASS' "grouped thumbnail with $items items" }
                return New-SessionOutcome 'UNCONFIRMED' "thumbnail window appeared but UIA sees $items item(s); cannot tell grouped from single"
            }
            finally {
                if ($autoHideChanged) {
                    $null = Set-AeroTaskbarAutoHide -Session $ctx.Session -Enabled $true
                    [void]$evidence.Add('taskbar auto-hide restored to on')
                }
            }
        }

        # ---------------- SNAP-07: FancyZones ----------------
        Invoke-SessionCheck -Id 'S07-FANCYZONES' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $zones = Get-AeroFancyZonesZones
            [void]$evidence.Add("zonesFound=$($zones.Found) reason=$($zones.Reason)")
            $entry = $null
            foreach ($m in @($zones.Monitors)) { if ($m.ZoneCount -gt 0) { $entry = $m; break } }
            if (-not $entry) {
                return New-SessionOutcome 'UNCONFIRMED' 'no FancyZones zones available (FancyZones not installed/running in this environment)'
            }
            [void]$evidence.Add("layout=$($entry.LayoutType) derivation=$($entry.Derivation)")
            $zone = $entry.Zones[0]
            $zx = [int]($zone.X + $zone.Width / 2)
            $zy = [int]($zone.Y + $zone.Height / 2)
            $expected = New-SessionWRect -Left $zone.X -Top $zone.Y -Right ($zone.X + $zone.Width) -Bottom ($zone.Y + $zone.Height)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            # Re-verify foreground right before the drag (the 22-15 JDK attempt lost it between
            # the focus click and the Shift-drag) and retry the click exactly once when it moved.
            $fgHwnd = [AeroWinProbe.Native]::GetForegroundWindow()
            [uint32]$fgPidNow = 0
            [AeroWinProbe.Native]::GetWindowThreadProcessId($fgHwnd, [ref]$fgPidNow) | Out-Null
            $foregroundOursNow = ($fgPidNow -eq [uint32]$ctx.AppPid)
            if (-not $foregroundOursNow) {
                $foregroundOursNow = Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence
                [void]$evidence.Add("foregroundReverify ours=$foregroundOursNow after one retry")
            }
            else {
                [void]$evidence.Add("foregroundReverify ours=$foregroundOursNow before drag")
            }
            [void]$evidence.Add("shift-drag caption -> zone center ($zx,$zy), zone=$(ConvertTo-SessionRectString -Rect $expected)")
            Invoke-AeroDrag -Session $ctx.Session -FromX $caption.X -FromY $caption.Y -ToX $zx -ToY $zy -Steps 25 -DurationMs 900 -HoldMs 600 -WithKey Shift
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed' }
            Start-Sleep -Milliseconds 800
            Send-AeroKeyChord -Session $ctx.Session -Keys @('Escape')
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            [void]$evidence.Add("rect=$(ConvertTo-SessionRectString -Rect $after) expected=$(ConvertTo-SessionRectString -Rect $expected)")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'S07-FANCYZONES'
            if (Test-SessionRectMatch -Actual $after -Expected $expected -Tolerance 8) {
                return New-SessionOutcome 'PASS' 'window snapped into the FancyZones zone'
            }
            return New-SessionOutcome 'FAIL' "rect=$(ConvertTo-SessionRectString -Rect $after) expected=$(ConvertTo-SessionRectString -Rect $expected)"
        }

        # ---------------- WIN-01: work area / taskbar ----------------
        Invoke-SessionCheck -Id 'W01-MAX-VISIBLE-TASKBAR' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $tb = Get-WinProbeTaskbar -Hwnd $ctx.MainHwnd
            $autoHideChanged = $false
            if ($tb.AutoHideOn) {
                $null = Set-AeroTaskbarAutoHide -Session $ctx.Session -Enabled $false
                $autoHideChanged = $true
                [void]$evidence.Add('taskbar auto-hide turned off for this check (restored below)')
            }
            try {
                $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
                $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
                Invoke-AeroClick -Session $ctx.Session -X $max.X -Y $max.Y
                if ($ctx.Session.IsDryRun) {
                    $v11 = Invoke-SessionV11 -Hwnd $ctx.MainHwnd -LogPath $ctx.LogPath -Label 'main'
                    [void]$evidence.Add("v11 pass=$($v11.PassCount) fail=$($v11.FailCount) (maximize checks included, F9-safe probe cycle)")
                    return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real click performed'
                }
                Start-Sleep -Milliseconds 700
                $origin = New-Object AeroWinProbe.WPoint
                [AeroWinProbe.Native]::ClientToScreen($ctx.MainHwnd, [ref]$origin) | Out-Null
                $clientLocal = New-Object AeroWinProbe.WRect
                [AeroWinProbe.Native]::GetClientRect($ctx.MainHwnd, [ref]$clientLocal) | Out-Null
                $client = New-SessionWRect -Left $origin.X -Top $origin.Y -Right ($origin.X + $clientLocal.Right - $clientLocal.Left) -Bottom ($origin.Y + $clientLocal.Bottom - $clientLocal.Top)
                $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
                $work = $mon.rcWork
                $tray = [AeroSessionEnv.Native]::FindWindowW('Shell_TrayWnd', $null)
                $trayRect = New-Object AeroSessionEnv.EnvRect
                [void][AeroSessionEnv.Native]::GetWindowRect($tray, [ref]$trayRect)
                [void]$evidence.Add("client=$(ConvertTo-SessionRectString -Rect $client) rcWork=$(ConvertTo-SessionRectString -Rect $work) taskbar=$(ConvertTo-SessionRectString -Rect $trayRect)")
                Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'W01-MAX-VISIBLE-TASKBAR'
                $edgesOk = (
                    ([Math]::Abs($client.Left - $work.Left) -le 1) -and
                    ([Math]::Abs($client.Top - $work.Top) -le 1) -and
                    ([Math]::Abs($client.Right - $work.Right) -le 1) -and
                    ([Math]::Abs($client.Bottom - $work.Bottom) -le 1)
                )
                $notOverlapping = ($client.Bottom -le ($trayRect.Top + 1))
                if ($edgesOk -and $notOverlapping) {
                    return New-SessionOutcome 'PASS' 'maximized client == rcWork, taskbar not overlapped'
                }
                return New-SessionOutcome 'FAIL' "edgesOk=$edgesOk taskbarNotOverlapped=$notOverlapping"
            }
            finally {
                if ($autoHideChanged) {
                    $null = Set-AeroTaskbarAutoHide -Session $ctx.Session -Enabled $true
                    [void]$evidence.Add('taskbar auto-hide restored to on')
                }
            }
        }

        Invoke-SessionCheck -Id 'W01-AUTOHIDE-REVEAL' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $tb = Get-WinProbeTaskbar -Hwnd $ctx.MainHwnd
            $wasAutoHide = $tb.AutoHideOn
            if (-not $wasAutoHide) {
                $null = Set-AeroTaskbarAutoHide -Session $ctx.Session -Enabled $true
                [void]$evidence.Add('taskbar auto-hide turned on for this check (restored below)')
            }
            try {
                $mon = Get-SessionMonitorInfo -Hwnd $ctx.MainHwnd
                $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
                $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
                $bottomX = [int](($mon.rcMonitor.Left + $mon.rcMonitor.Right) / 2)
                $bottomY = $mon.rcMonitor.Bottom - 2
                Move-AeroCursor -Session $ctx.Session -X $bottomX -Y $bottomY
                if ($ctx.Session.IsDryRun) {
                    Invoke-AeroClick -Session $ctx.Session -X $max.X -Y $max.Y
                    Move-AeroCursor -Session $ctx.Session -X 960 -Y 500
                    [void]$evidence.Add("planned: maximize, cursor to ($bottomX,$bottomY), expect Test-AeroTaskbarRevealed within 2s")
                    return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real input performed'
                }
                Invoke-AeroClick -Session $ctx.Session -X $max.X -Y $max.Y
                Start-Sleep -Milliseconds 700
                Move-AeroCursor -Session $ctx.Session -X $bottomX -Y $bottomY
                $revealedAt = -1
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                $revealed = $false
                while ($sw.ElapsedMilliseconds -lt 2000) {
                    $probe = Test-AeroTaskbarRevealed -MonitorRect $mon.rcMonitor
                    if ($probe.Revealed) { $revealed = $true; $revealedAt = [int]$sw.ElapsedMilliseconds; break }
                    Start-Sleep -Milliseconds 100
                }
                foreach ($d in @(Test-AeroTaskbarRevealed -MonitorRect $mon.rcMonitor).Details) { [void]$evidence.Add($d) }
                Move-AeroCursor -Session $ctx.Session -X 960 -Y 500
                [void][AeroWinProbe.Native]::SendMessageTimeoutW($ctx.MainHwnd, [AeroWinProbe.Native]::WM_SYSCOMMAND, [IntPtr][AeroWinProbe.Native]::SC_RESTORE, [IntPtr]::Zero, [AeroWinProbe.Native]::SMTO_ABORTIFHUNG, 2000, [ref]([IntPtr]::Zero))
                [void]$evidence.Add("revealed=$revealed at=${revealedAt}ms")
                if ($revealed) { return New-SessionOutcome 'PASS' "auto-hide taskbar revealed after ${revealedAt}ms" }
                return New-SessionOutcome 'FAIL' 'taskbar did not reveal within 2s of the bottom-row hover'
            }
            finally {
                if (-not $wasAutoHide) {
                    $null = Set-AeroTaskbarAutoHide -Session $ctx.Session -Enabled $false
                    [void]$evidence.Add('taskbar auto-hide restored to off')
                }
            }
        }

        # ---------------- WIN-02: edge/corner resize ----------------
        Invoke-SessionCheck -Id 'W02-EDGE-L' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'edgeLeft' -ExpectedShape 'SizeWE' -Dx -60 -Dy 0 }
        Invoke-SessionCheck -Id 'W02-EDGE-R' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'edgeRight' -ExpectedShape 'SizeWE' -Dx 60 -Dy 0 }
        Invoke-SessionCheck -Id 'W02-EDGE-T' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'edgeTop' -ExpectedShape 'SizeNS' -Dx 0 -Dy -60 }
        Invoke-SessionCheck -Id 'W02-EDGE-B' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'edgeBottom' -ExpectedShape 'SizeNS' -Dx 0 -Dy 60 }
        Invoke-SessionCheck -Id 'W02-CORNER-TL' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'cornerTL' -ExpectedShape 'SizeNWSE' -Dx -60 -Dy -60 }
        Invoke-SessionCheck -Id 'W02-CORNER-TR' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'cornerTR' -ExpectedShape 'SizeNESW' -Dx 60 -Dy -60 }
        Invoke-SessionCheck -Id 'W02-CORNER-BL' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'cornerBL' -ExpectedShape 'SizeNESW' -Dx -60 -Dy 60 }
        Invoke-SessionCheck -Id 'W02-CORNER-BR' -Context $ctx -Body { param($ctx, $evidence, $frames)
            return Invoke-SessionEdgeResizeCheck -Context $ctx -Evidence $evidence -PointName 'cornerBR' -ExpectedShape 'SizeNWSE' -Dx 60 -Dy 60 }

        Invoke-SessionCheck -Id 'W02-MAIN-FLOOR' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $pt = Get-SessionScreenPoint -Geometry $g -PointName 'edgeLeft'
            $info = Get-WinProbeWindowInfo -Hwnd $ctx.MainHwnd
            $expectedW = [int][Math]::Round(320 * $info.Scale)
            $before = Get-SessionRect -Hwnd $ctx.MainHwnd
            $beforeH = $before.Bottom - $before.Top
            $targetX = ($g.Origin.X + $g.Info.ClientRect.Right - $g.Info.ClientRect.Left) - 100
            [void]$evidence.Add("inward drag left edge to width~100; expect width floor $expectedW with height $beforeH unchanged")
            Invoke-AeroDrag -Session $ctx.Session -FromX $pt.X -FromY $pt.Y -ToX $targetX -ToY $pt.Y -Steps 15 -DurationMs 600 -HoldMs 300
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed' }
            Start-Sleep -Milliseconds 400
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            $w = $after.Right - $after.Left
            $h = $after.Bottom - $after.Top
            [void]$evidence.Add("obtainedWidth=$w expectedWidth=$expectedW heightBefore=$beforeH heightAfter=$h")
            if (([Math]::Abs($w - $expectedW) -le 2) -and ([Math]::Abs($h - $beforeH) -le 2)) { return New-SessionOutcome 'PASS' "floored at width $w, height ${h} unchanged" }
            return New-SessionOutcome 'FAIL' "obtainedWidth=$w expectedWidth=$expectedW heightAfter=$h heightBefore=$beforeH"
        }

        Invoke-SessionCheck -Id 'W02-NARROW-FLOOR' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $pt = Get-SessionScreenPoint -Geometry $g -PointName 'edgeLeft'
            $expectedW = [int][Math]::Round(260 * $g.Info.Scale)
            $before = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            $beforeH = $before.Bottom - $before.Top
            $targetX = ($g.Origin.X + $g.Info.ClientRect.Right - $g.Info.ClientRect.Left) - 80
            [void]$evidence.Add("inward drag narrow left edge; expect app width floor $expectedW with height $beforeH unchanged (D-01)")
            Invoke-AeroDrag -Session $ctx.Session -FromX $pt.X -FromY $pt.Y -ToX $targetX -ToY $pt.Y -Steps 15 -DurationMs 600 -HoldMs 300
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed' }
            Start-Sleep -Milliseconds 400
            $after = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            $w = $after.Right - $after.Left
            $h = $after.Bottom - $after.Top
            [void]$evidence.Add("obtainedWidth=$w expectedWidth=$expectedW heightBefore=$beforeH heightAfter=$h")
            if (([Math]::Abs($w - $expectedW) -le 2) -and ([Math]::Abs($h - $beforeH) -le 2)) { return New-SessionOutcome 'PASS' "floored at width $w, height ${h} unchanged" }
            return New-SessionOutcome 'FAIL' "obtainedWidth=$w expectedWidth=$expectedW heightAfter=$h heightBefore=$beforeH"
        }

        # ---------------- WIN-03: frames + corners ----------------
        Invoke-SessionCheck -Id 'W03-FRAMES' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $baseline = Join-Path $ctx.RepoRoot '.captures\22-baseline\AeroBlue-rest.png'
            if (-not (Test-Path -LiteralPath $baseline)) {
                return New-SessionOutcome 'UNCONFIRMED' 'pre-phase baseline AeroBlue-rest.png not found'
            }
            $mainFloat = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'main-floating'
            [void]$frames.Add($mainFloat)
            $narrowFloat = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.NarrowHwnd -Name 'narrow-floating' -SkipSanity
            [void]$frames.Add($narrowFloat)
            $band = Compare-WinProbeRegion -Before $baseline -After $mainFloat -Top 0 -Bottom 31
            [void]$evidence.Add("title-band baseline-vs-floating diffPixels=$($band.DiffPixels) maxDelta=$($band.MaxChannelDelta) comparable=$($band.Comparable)")
            if ($ctx.Session.IsDryRun) {
                [void]$evidence.Add('planned (real run): Win+Left / Win+Right snapped frames for both windows + max-click maximized frames for both')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- snapped/maximized frames need real input; floating band compare above'
            }
            $ok = ($band.Comparable -and $band.DiffPixels -eq 0)

            $gMain = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $capMain = Get-SessionScreenPoint -Geometry $gMain -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $capMain.X -ScreenY $capMain.Y -Evidence $evidence)
            Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Left')
            Start-Sleep -Milliseconds 700
            $mainSnapped = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'main-snapped'
            [void]$frames.Add($mainSnapped)
            $gNarrow = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $capNarrow = Get-SessionScreenPoint -Geometry $gNarrow -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.NarrowHwnd -ScreenX $capNarrow.X -ScreenY $capNarrow.Y -Evidence $evidence)
            Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Right')
            Start-Sleep -Milliseconds 700
            $narrowSnapped = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.NarrowHwnd -Name 'narrow-snapped' -SkipSanity
            [void]$frames.Add($narrowSnapped)

            Reset-SessionWindows -Context $ctx
            $maxMain = Get-SessionScreenPoint -Geometry (Get-SessionGeometry -Hwnd $ctx.MainHwnd) -PointName 'max'
            Invoke-AeroClick -Session $ctx.Session -X $maxMain.X -Y $maxMain.Y
            Start-Sleep -Milliseconds 700
            $mainMax = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'main-maximized'
            [void]$frames.Add($mainMax)
            Reset-SessionWindow -Hwnd $ctx.MainHwnd -X 360 -Y 140 -W 1200 -H 800
            $maxNarrow = Get-SessionScreenPoint -Geometry (Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow') -PointName 'max'
            Invoke-AeroClick -Session $ctx.Session -X $maxNarrow.X -Y $maxNarrow.Y
            Start-Sleep -Milliseconds 700
            $narrowMax = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.NarrowHwnd -Name 'narrow-maximized' -SkipSanity
            [void]$frames.Add($narrowMax)
            [void]$evidence.Add("frames=$(@($frames).Count)/6 (floating/snapped/maximized for both windows)")
            if ($ok -and @($frames).Count -ge 6) { return New-SessionOutcome 'PASS' "6 frames captured, floating title band diff 0" }
            return New-SessionOutcome 'FAIL' "bandDiff=$($band.DiffPixels) frames=$(@($frames).Count)"
        }

        Invoke-SessionCheck -Id 'W03-CORNERS' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $dwmLines = @(Get-SessionLogLines -LogPath $ctx.LogPath | Where-Object { $_ -like '*event=dwm*' })
            foreach ($l in $dwmLines) { [void]$evidence.Add($l) }
            if ($dwmLines.Count -eq 0) {
                return New-SessionOutcome 'FAIL' 'no event=dwm trace lines in the launch log (corner policy read-back missing)'
            }
            $badReadBack = @($dwmLines | Where-Object { $_ -notmatch 'readBack=1' })
            [void]$evidence.Add("dwmLines=$($dwmLines.Count) nonDontRound=$($badReadBack.Count) (C6 choice: SQUARE_NO_SHADOW = DWMWCP_DONOTROUND = 1)")
            if ($badReadBack.Count -eq 0) { return New-SessionOutcome 'PASS' "all $($dwmLines.Count) dwm read-backs equal the C6 choice (1)" }
            return New-SessionOutcome 'FAIL' "$($badReadBack.Count) dwm line(s) read back a value other than 1"
        }

        # ---------------- WIN-04: 150% virtual display ----------------
        Invoke-SessionCheck -Id 'W04-DRAG-TO-150' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $env = Get-AeroEnvState
            $virtual = $null
            foreach ($m in @($env.Monitors)) { if (-not $m.IsPrimary) { $virtual = $m; break } }
            if (-not $virtual) {
                return New-SessionOutcome 'UNCONFIRMED' 'virtual 150% display not present (EnvSetup not run in this environment)'
            }
            $toX = [int](($virtual.MonitorRect.Left + $virtual.MonitorRect.Right) / 2)
            $toY = [int](($virtual.MonitorRect.Top + $virtual.MonitorRect.Bottom) / 2)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            [void]$evidence.Add("drag caption -> virtual display center ($toX,$toY)")
            Invoke-AeroDrag -Session $ctx.Session -FromX $caption.X -FromY $caption.Y -ToX $toX -ToY $toY -Steps 25 -DurationMs 1200 -HoldMs 500
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed' }
            Start-Sleep -Milliseconds 800
            $scale = Get-SessionReporterScale -LogPath $ctx.LogPath -Label 'main'
            $sizeDp = Get-SessionReporterSizeDp -LogPath $ctx.LogPath -Label 'main'
            [void]$evidence.Add("reporter scale=$scale sizeDp=$($sizeDp.W)x$($sizeDp.H)")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'W04-DRAG-TO-150'
            if (([Math]::Abs($scale - 1.5) -le 0.05) -and ([Math]::Abs($sizeDp.W - 1200) -le 2) -and ([Math]::Abs($sizeDp.H - 800) -le 2)) {
                return New-SessionOutcome 'PASS' "scale=$scale sizeDp=$($sizeDp.W)x$($sizeDp.H)"
            }
            return New-SessionOutcome 'FAIL' "scale=$scale sizeDp=$($sizeDp.W)x$($sizeDp.H), expected 1.5 and 1200x800±2dp"
        }

        Invoke-SessionCheck -Id 'W04-WIN-SHIFT-ARROW' -Context $ctx -NoReset -Body {
            param($ctx, $evidence, $frames)
            $scaleNow = Get-SessionReporterScale -LogPath $ctx.LogPath -Label 'main'
            if ([Math]::Abs($scaleNow - 1.5) -gt 0.05) {
                return New-SessionOutcome 'UNCONFIRMED' "main window not on the 150% display (scale=$scaleNow); run W04-DRAG-TO-150 first"
            }
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            Send-AeroKeyChord -Session $ctx.Session -Keys @('LWin', 'Shift', 'Left')
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real keys sent' }
            Start-Sleep -Milliseconds 900
            $scale = Get-SessionReporterScale -LogPath $ctx.LogPath -Label 'main'
            $after = Get-SessionRect -Hwnd $ctx.MainHwnd
            $env = Get-AeroEnvState
            $primary = $null
            foreach ($m in @($env.Monitors)) { if ($m.IsPrimary) { $primary = $m; break } }
            $cx = [int](($after.Left + $after.Right) / 2)
            $cy = [int](($after.Top + $after.Bottom) / 2)
            $onPrimary = ($cx -ge $primary.MonitorRect.Left) -and ($cx -lt $primary.MonitorRect.Right) -and ($cy -ge $primary.MonitorRect.Top) -and ($cy -lt $primary.MonitorRect.Bottom)
            [void]$evidence.Add("scale=$scale center=($cx,$cy) onPrimary=$onPrimary")
            if (([Math]::Abs($scale - 1.0) -le 0.05) -and $onPrimary) { return New-SessionOutcome 'PASS' 'Win+Shift+Left returned the window to the primary at scale 1.0' }
            return New-SessionOutcome 'FAIL' "scale=$scale onPrimary=$onPrimary"
        }

        Invoke-SessionCheck -Id 'W04-MAXIMIZE-ON-150' -Context $ctx -NoReset -Body {
            param($ctx, $evidence, $frames)
            $scaleNow = Get-SessionReporterScale -LogPath $ctx.LogPath -Label 'main'
            if ([Math]::Abs($scaleNow - 1.5) -gt 0.05) {
                return New-SessionOutcome 'UNCONFIRMED' "main window not on the 150% display (scale=$scaleNow); run the earlier W04 checks first"
            }
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
            Invoke-AeroClick -Session $ctx.Session -X $max.X -Y $max.Y
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real click performed' }
            Start-Sleep -Milliseconds 700
            $scale = Get-SessionReporterScale -LogPath $ctx.LogPath -Label 'main'
            $rep = Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main'
            $placement = Get-ReporterField $rep 'placement'
            [void]$evidence.Add("scale=$scale placement=$placement")
            $v11 = Invoke-SessionV11 -Hwnd $ctx.MainHwnd -LogPath $ctx.LogPath -Label 'main'
            foreach ($line in @($v11.Flat | Where-Object { $_ -match 'V11-(HT-CAPTION|HT-MAX|HT-MIN-BOUNDARY|HT-CLOSE-BOUNDARY|HT-EDGE|HT-CORNER|MAX-WORKAREA|STYLE)' })) { [void]$evidence.Add($line) }
            $dpiNow = (Get-WinProbeWindowInfo -Hwnd $ctx.MainHwnd).Dpi
            [void]$evidence.Add("v11 pass=$($v11.PassCount) fail=$($v11.FailCount) at dpi=$dpiNow")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'W04-MAXIMIZE-ON-150'
            $ok = ([Math]::Abs($scale - 1.5) -le 0.05) -and ($v11.FailCount -eq 0)
            if ($ok) { return New-SessionOutcome 'PASS' "maximized on the 150% display, v11 fail=0" }
            return New-SessionOutcome 'FAIL' "scale=$scale v11Fail=$($v11.FailCount)"
        }

        # ---------------- WIN-05: placement + glyph ----------------
        Invoke-SessionCheck -Id 'W05-PLACEMENT' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $samples = @($ctx.PlacementSamples)
            foreach ($s in $samples) { [void]$evidence.Add($s) }
            $mismatches = @($samples | Where-Object { $_ -match 'match=False' })
            [void]$evidence.Add("placementSamples=$($samples.Count) mismatches=$($mismatches.Count)")

            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $maxX = [int]$g.Points.max.x
            $f1 = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'glyph-floating-1'
            [void]$frames.Add($f1)
            $c1 = Join-Path $ctx.FramesDir "$($ctx.CurrentCheckId)-crop-float1.png"
            Copy-SessionBitmapRegion -Source $f1 -Dest $c1 -X ($maxX - 14) -Y 5 -Width 28 -Height 22
            [void]$frames.Add($c1)
            if ($ctx.Session.IsDryRun) {
                [void]$evidence.Add('planned (real run): maximize, capture glyph crop (restore glyph), restore, re-capture floating crop; expect max-crop to differ, float1==float2')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- glyph toggle needs real input; placement samples above'
            }
            $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
            Invoke-AeroClick -Session $ctx.Session -X $max.X -Y $max.Y
            Start-Sleep -Milliseconds 700
            $f2 = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'glyph-maximized'
            [void]$frames.Add($f2)
            $c2 = Join-Path $ctx.FramesDir "$($ctx.CurrentCheckId)-crop-max.png"
            Copy-SessionBitmapRegion -Source $f2 -Dest $c2 -X ($maxX - 14) -Y 5 -Width 28 -Height 22
            [void]$frames.Add($c2)
            Reset-SessionWindow -Hwnd $ctx.MainHwnd -X 360 -Y 140 -W 1200 -H 800
            $f3 = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'glyph-floating-2'
            [void]$frames.Add($f3)
            $c3 = Join-Path $ctx.FramesDir "$($ctx.CurrentCheckId)-crop-float2.png"
            Copy-SessionBitmapRegion -Source $f3 -Dest $c3 -X ($maxX - 14) -Y 5 -Width 28 -Height 22
            [void]$frames.Add($c3)
            $floatStable = Compare-SessionImages -Before $c1 -After $c3
            $glyphSwitched = Compare-SessionImages -Before $c1 -After $c2
            [void]$evidence.Add("float1-vs-float2 diffPixels=$($floatStable.DiffPixels) maxDelta=$($floatStable.MaxChannelDelta)")
            [void]$evidence.Add("float-vs-maximized diffPixels=$($glyphSwitched.DiffPixels) maxDelta=$($glyphSwitched.MaxChannelDelta)")
            $ok = ($mismatches.Count -eq 0) -and ($floatStable.DiffPixels -eq 0) -and ($glyphSwitched.DiffPixels -gt 0)
            $placementWord = if ($samples.Count -eq 0) { 'no samples recorded' } else { 'all matched' }
            if ($ok) { return New-SessionOutcome 'PASS' "placement $placementWord; glyph stable floating / switched maximized" }
            return New-SessionOutcome 'FAIL' "mismatches=$($mismatches.Count) floatStableDiff=$($floatStable.DiffPixels) glyphSwitchDiff=$($glyphSwitched.DiffPixels)"
        }

        # ---------------- WIN-06: independence, open-while-dragging, close-during-drag ----------------
        Invoke-SessionCheck -Id 'W06-INDEPENDENT' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $installLines = @(Get-SessionLogLines -LogPath $ctx.LogPath | Where-Object { $_ -like '*event=install*' })
            $reuseLines = @(Get-SessionLogLines -LogPath $ctx.LogPath | Where-Object { $_ -like '*event=reuse*' })
            [void]$evidence.Add("installLines=$($installLines.Count) reuseLines=$($reuseLines.Count)")
            foreach ($l in $installLines) { [void]$evidence.Add($l) }
            if ($ctx.Session.IsDryRun) {
                $gNarrow = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
                $capNarrow = Get-SessionScreenPoint -Geometry $gNarrow -PointName 'caption'
                Invoke-AeroDrag -Session $ctx.Session -FromX $capNarrow.X -FromY $capNarrow.Y -ToX ($capNarrow.X + 120) -ToY ($capNarrow.Y + 60) -Steps 10 -DurationMs 500 -HoldMs 200
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed'
            }
            $mainBefore = Get-SessionRect -Hwnd $ctx.MainHwnd
            $gNarrow = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $capNarrow = Get-SessionScreenPoint -Geometry $gNarrow -PointName 'caption'
            Invoke-AeroDrag -Session $ctx.Session -FromX $capNarrow.X -FromY $capNarrow.Y -ToX ($capNarrow.X + 120) -ToY ($capNarrow.Y + 60) -Steps 10 -DurationMs 500 -HoldMs 200
            Start-Sleep -Milliseconds 400
            $mainAfterNarrowDrag = Get-SessionRect -Hwnd $ctx.MainHwnd
            $mainUnmoved = Test-SessionRectMatch -Actual $mainAfterNarrowDrag -Expected $mainBefore -Tolerance 2
            $narrowBefore = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            $gMain = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $capMain = Get-SessionScreenPoint -Geometry $gMain -PointName 'caption'
            Invoke-AeroDrag -Session $ctx.Session -FromX $capMain.X -FromY $capMain.Y -ToX ($capMain.X - 100) -ToY ($capMain.Y + 40) -Steps 10 -DurationMs 500 -HoldMs 200
            Start-Sleep -Milliseconds 400
            $narrowAfterMainDrag = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            $narrowUnmoved = Test-SessionRectMatch -Actual $narrowAfterMainDrag -Expected $narrowBefore -Tolerance 2
            [void]$evidence.Add("mainUnmovedWhenNarrowDragged=$mainUnmoved narrowUnmovedWhenMainDragged=$narrowUnmoved")
            if ($mainUnmoved -and $narrowUnmoved -and $reuseLines.Count -eq 0 -and $installLines.Count -ge 2) {
                return New-SessionOutcome 'PASS' 'windows independent (dragging one never moved the other)'
            }
            return New-SessionOutcome 'FAIL' "mainUnmoved=$mainUnmoved narrowUnmoved=$narrowUnmoved installs=$($installLines.Count) reuse=$($reuseLines.Count)"
        }

        Invoke-SessionCheck -Id 'W06-OPEN-WHILE-DRAGGING' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            # Relaunch with a delayed narrow window so it appears mid-drag. The delay is measured
            # from process start (~6 s to reporter-ready on this machine), so 12 s opens it while
            # a 6 s drag that starts ~2 s after the window is ready is still in progress.
            Stop-SessionLaunch -LaunchInfo $ctx.LaunchInfo -Hwnd $ctx.MainHwnd -TrackedPids $ctx.LaunchJvmPids
            $mainKtBeforeRelaunch = Get-SessionMainKtPids
            $ctx.LaunchInfo = Start-SessionShowcase -Context $ctx -Task $(if ($ctx.PassName -eq 'jdk') { 'run' } else { 'hotRun' }) -ExtraProps @('-Paero.chromeTrace=true', '-Paero.secondWindow=12000', '-Paero.secondWindowReopenMs=1500')
            $ctx.LogPath = $ctx.LaunchInfo.StdOut
            $ctx.MainHwnd = Find-AeroShowcaseWindow -Title $ctx.MainTitle -TimeoutSec $ReadyTimeoutSec
            $null = Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main' } -TimeoutSec $ReadyTimeoutSec
            Start-Sleep -Milliseconds 500
            $ctx.LaunchJvmPids = @(Get-SessionMainKtPids | Where-Object { $mainKtBeforeRelaunch -notcontains $_ })
            $windowInfo = Get-WinProbeWindowInfo -Hwnd $ctx.MainHwnd
            $ctx.AppPid = [uint32]$windowInfo.ProcessId
            [void]$evidence.Add("relaunched jvm=$($windowInfo.JvmKind) pid=$($ctx.AppPid) narrow opens at 12s")

            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            if ($ctx.Session.IsDryRun) {
                Invoke-AeroDrag -Session $ctx.Session -FromX $caption.X -FromY $caption.Y -ToX ($caption.X + 200) -ToY ($caption.Y + 100) -Steps 30 -DurationMs 6000 -HoldMs 300
                $ctx.NarrowHwnd = Wait-SessionWindowByTitle -Title $ctx.NarrowTitle -TimeoutSec 30
                $null = Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'narrow' } -TimeoutSec 30
                $v11Main = Invoke-SessionV11 -Hwnd $ctx.MainHwnd -LogPath $ctx.LogPath -Label 'main' -SkipMaximize
                $v11Narrow = Invoke-SessionV11 -Hwnd $ctx.NarrowHwnd -Layout 'narrow' -LogPath $ctx.LogPath -Label 'narrow'
                [void]$evidence.Add("v11 main pass=$($v11Main.PassCount) fail=$($v11Main.FailCount); narrow pass=$($v11Narrow.PassCount) fail=$($v11Narrow.FailCount)")
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed (relaunch + narrow-open proven)'
            }
            Invoke-AeroDrag -Session $ctx.Session -FromX $caption.X -FromY $caption.Y -ToX ($caption.X + 200) -ToY ($caption.Y + 100) -Steps 30 -DurationMs 6000 -HoldMs 300
            $ctx.NarrowHwnd = Wait-SessionWindowByTitle -Title $ctx.NarrowTitle -TimeoutSec 30
            $null = Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'narrow' } -TimeoutSec 30
            Start-Sleep -Milliseconds 500
            $mark = @(Get-SessionLogLines -LogPath $ctx.LogPath).Count
            $narrowInfo = Get-WinProbeWindowInfo -Hwnd $ctx.NarrowHwnd
            $installForNarrow = @(Get-SessionLogLines -LogPath $ctx.LogPath | Where-Object { $_ -like '*event=install*' -and $_.Contains('0x' + ('{0:x}' -f $narrowInfo.Hwnd.ToInt64())) })
            [void]$evidence.Add("narrow=0x$('{0:X}' -f $narrowInfo.Hwnd.ToInt64()) ownInstallLines=$($installForNarrow.Count)")
            $v11Main2 = Invoke-SessionV11 -Hwnd $ctx.MainHwnd -LogPath $ctx.LogPath -Label 'main' -SkipMaximize
            $v11Narrow2 = Invoke-SessionV11 -Hwnd $ctx.NarrowHwnd -Layout 'narrow' -LogPath $ctx.LogPath -Label 'narrow'
            [void]$evidence.Add("v11 main pass=$($v11Main2.PassCount) fail=$($v11Main2.FailCount); narrow pass=$($v11Narrow2.PassCount) fail=$($v11Narrow2.FailCount)")
            if ($v11Main2.FailCount -eq 0 -and $v11Narrow2.FailCount -eq 0 -and $installForNarrow.Count -ge 1) {
                return New-SessionOutcome 'PASS' 'narrow opened mid-drag with its own install; v11 green on both'
            }
            return New-SessionOutcome 'FAIL' "v11Fail main=$($v11Main2.FailCount) narrow=$($v11Narrow2.FailCount) installs=$($installForNarrow.Count)"
        }

        Invoke-SessionCheck -Id 'W06-CLOSE-DURING-DRAG' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            if (-not (Test-SessionWindowAlive -Hwnd $ctx.NarrowHwnd)) {
                return New-SessionOutcome 'UNCONFIRMED' 'narrow window not open'
            }
            $hsErrBefore = Get-SessionHsErrFiles -Root $ctx.RepoRoot
            $g = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $mark = @(Get-SessionLogLines -LogPath $ctx.LogPath).Count
            if ($ctx.Session.IsDryRun) {
                [void]$evidence.Add('planned: slow drag of narrow, WM_CLOSE posted mid-drag, expect close-request event + ncdestroy trace, main unaffected, no new hs_err')
                Move-AeroCursor -Session $ctx.Session -X $caption.X -Y $caption.Y
                Invoke-AeroMouseButton -Session $ctx.Session -Down
                [void][AeroWinProbe.Native]::PostMessageW($ctx.NarrowHwnd, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)
                Invoke-AeroMouseButton -Session $ctx.Session -Up
                Start-Sleep -Milliseconds 500
                $event = Wait-SessionEventAfter -LogPath $ctx.LogPath -Name 'close-request' -Detail 'label=narrow' -AfterLineCount $mark -TimeoutSec 5
                $ncdestroyDry = Wait-SessionChromeEventAfter -LogPath $ctx.LogPath -Name 'ncdestroy' -AfterLineCount $mark -TimeoutSec 10
                [void]$evidence.Add("dryRun closeEvent=$([bool]$event) ncdestroy=$([bool]$ncdestroyDry) (WM_CLOSE is a probe message, not real input)")
                if (-not (Test-SessionWindowAlive -Hwnd $ctx.NarrowHwnd)) {
                    $ctx.NarrowHwnd = Wait-SessionWindowByTitle -Title $ctx.NarrowTitle -TimeoutSec 15
                }
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed (close/reopen mechanics proven)'
            }
            Move-AeroCursor -Session $ctx.Session -X $caption.X -Y $caption.Y
            Invoke-AeroMouseButton -Session $ctx.Session -Down
            for ($i = 1; $i -le 3; $i++) {
                Move-AeroCursor -Session $ctx.Session -X ($caption.X + 15 * $i) -Y ($caption.Y + 8 * $i) -Steps 3 -DurationMs 400
                Start-Sleep -Milliseconds 200
            }
            [void][AeroWinProbe.Native]::PostMessageW($ctx.NarrowHwnd, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)
            # The ncdestroy trace can land mid-drag, while the window tears down inside the drag's
            # modal loop: poll seeded at the pre-drag $mark starting the moment WM_CLOSE is posted
            # (the old after-mouse-up 5 s all-lines window missed it), budget 10 s.
            $ncdestroy = Wait-SessionChromeEventAfter -LogPath $ctx.LogPath -Name 'ncdestroy' -AfterLineCount $mark -TimeoutSec 10
            for ($i = 4; $i -le 6; $i++) {
                Move-AeroCursor -Session $ctx.Session -X ($caption.X + 15 * $i) -Y ($caption.Y + 8 * $i) -Steps 3 -DurationMs 400
                Start-Sleep -Milliseconds 200
            }
            Invoke-AeroMouseButton -Session $ctx.Session -Up
            $event = Wait-SessionEventAfter -LogPath $ctx.LogPath -Name 'close-request' -Detail 'label=narrow' -AfterLineCount $mark -TimeoutSec 5
            [void]$evidence.Add("closeEvent=$([bool]$event) ncdestroy=$([bool]$ncdestroy)")
            $mainAlive = Test-SessionWindowAlive -Hwnd $ctx.MainHwnd
            $v11Main = Invoke-SessionV11 -Hwnd $ctx.MainHwnd -LogPath $ctx.LogPath -Label 'main' -SkipMaximize
            [void]$evidence.Add("mainAlive=$mainAlive v11MainFail=$($v11Main.FailCount)")
            $hsErrAfter = Get-SessionHsErrFiles -Root $ctx.RepoRoot
            $newHsErr = @($hsErrAfter | Where-Object { $hsErrBefore -notcontains $_ })
            [void]$evidence.Add("newHsErrFiles=$($newHsErr.Count)")
            $gone = -not (Test-SessionWindowAlive -Hwnd $ctx.NarrowHwnd)
            if ($gone) {
                $ctx.NarrowHwnd = Wait-SessionWindowByTitle -Title $ctx.NarrowTitle -TimeoutSec 15
                Start-Sleep -Milliseconds 500
            }
            if ($event -and $ncdestroy -and $mainAlive -and $v11Main.FailCount -eq 0 -and $newHsErr.Count -eq 0) {
                return New-SessionOutcome 'PASS' 'clean close mid-drag; main unaffected; no hs_err'
            }
            return New-SessionOutcome 'FAIL' "event=$([bool]$event) ncdestroy=$([bool]$ncdestroy) mainAlive=$mainAlive v11Fail=$($v11Main.FailCount) newHsErr=$($newHsErr.Count)"
        }

        # ---------------- BTN-01: maximize button parity ----------------
        Invoke-SessionCheck -Id 'B01-HOVER-FRAME' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
            $min = Get-SessionScreenPoint -Geometry $g -PointName 'min'
            $rest = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'rest'
            [void]$frames.Add($rest)
            $maxX = [int]$g.Points.max.x
            $minX = [int]$g.Points.min.x
            $restMaxStrip = New-SessionButtonStrip -Context $ctx -FramePath $rest -Name 'rest-max' -CenterX $maxX
            $restMinStrip = New-SessionButtonStrip -Context $ctx -FramePath $rest -Name 'rest-min' -CenterX $minX
            [void]$frames.Add($restMaxStrip)
            [void]$frames.Add($restMinStrip)
            Move-AeroCursor -Session $ctx.Session -X $max.X -Y $max.Y
            if ($ctx.Session.IsDryRun) {
                Move-AeroCursor -Session $ctx.Session -X $min.X -Y $min.Y
                Move-AeroCursor -Session $ctx.Session -X 960 -Y 500
                [void]$evidence.Add('planned: hover max -> frame, hover min -> frame, compare glyph-free button strips for identical hover fill')
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real hover performed'
            }
            Start-Sleep -Milliseconds 700
            $hMax = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'hover-max'
            [void]$frames.Add($hMax)
            Move-AeroCursor -Session $ctx.Session -X $min.X -Y $min.Y
            Start-Sleep -Milliseconds 700
            $hMin = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'hover-min'
            [void]$frames.Add($hMin)
            Move-AeroCursor -Session $ctx.Session -X 960 -Y 500
            $hMaxStrip = New-SessionButtonStrip -Context $ctx -FramePath $hMax -Name 'hover-max' -CenterX $maxX
            $hMinStrip = New-SessionButtonStrip -Context $ctx -FramePath $hMin -Name 'hover-min' -CenterX $minX
            [void]$frames.Add($hMaxStrip)
            [void]$frames.Add($hMinStrip)
            $hoverVsHover = Compare-SessionImages -Before $hMaxStrip -After $hMinStrip
            $hoverVsRest = Compare-SessionImages -Before $hMaxStrip -After $restMaxStrip
            [void]$evidence.Add("hover-max-vs-hover-min diffPixels=$($hoverVsHover.DiffPixels) maxDelta=$($hoverVsHover.MaxChannelDelta)")
            [void]$evidence.Add("hover-vs-rest diffPixels=$($hoverVsRest.DiffPixels) maxDelta=$($hoverVsRest.MaxChannelDelta)")
            if ($hoverVsHover.DiffPixels -eq 0 -and $hoverVsRest.DiffPixels -gt 0) {
                return New-SessionOutcome 'PASS' 'identical hover fill on max and min; hover visibly paints'
            }
            return New-SessionOutcome 'FAIL' "hoverParityDiff=$($hoverVsHover.DiffPixels) hoverVisibleDiff=$($hoverVsRest.DiffPixels)"
        }

        Invoke-SessionCheck -Id 'B01-PRESS-FRAME' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            # The press on MAX is a full click sequence: its UP toggles placement and MAXIMIZES
            # the window (B01-CLICK proves the toggle). The min step therefore runs against a
            # window whose buttons moved — the floating-geometry min point would land on the
            # maximized CONTENT, and the parity strip would compare a button against content
            # (the 22-15 FAIL, diffPixels=144 maxDelta=176 both JVMs, frames 1936x1048).
            # Each toggle-causing up is followed by a floating reset, the min point is computed
            # from FRESH geometry, and both press frames are size-guarded against the rest
            # frame so a state drift can never masquerade as a parity number again.
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
            $maxX = [int]$g.Points.max.x
            $rest = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'rest'
            [void]$frames.Add($rest)
            $restMaxStrip = New-SessionButtonStrip -Context $ctx -FramePath $rest -Name 'rest-max' -CenterX $maxX
            [void]$frames.Add($restMaxStrip)
            Add-Type -AssemblyName System.Drawing
            $restImg = [System.Drawing.Bitmap]::FromFile($rest)
            $floatingW = $restImg.Width
            $floatingH = $restImg.Height
            $restImg.Dispose()
            Move-AeroCursor -Session $ctx.Session -X $max.X -Y $max.Y
            if ($ctx.Session.IsDryRun) {
                Invoke-AeroMouseButton -Session $ctx.Session -Down
                Invoke-AeroMouseButton -Session $ctx.Session -Up
                $gDry = Get-SessionGeometry -Hwnd $ctx.MainHwnd
                $minDry = Get-SessionScreenPoint -Geometry $gDry -PointName 'min'
                Move-AeroCursor -Session $ctx.Session -X $minDry.X -Y $minDry.Y
                Invoke-AeroMouseButton -Session $ctx.Session -Down
                Invoke-AeroMouseButton -Session $ctx.Session -Up
                Move-AeroCursor -Session $ctx.Session -X 960 -Y 500
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real press performed'
            }
            Start-Sleep -Milliseconds 400
            Invoke-AeroMouseButton -Session $ctx.Session -Down
            Start-Sleep -Milliseconds 400
            $pMax = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'press-max'
            [void]$frames.Add($pMax)
            Invoke-AeroMouseButton -Session $ctx.Session -Up
            Start-Sleep -Milliseconds 300
            # The up-click toggled placement: undo it before the min step.
            Reset-SessionWindow -Hwnd $ctx.MainHwnd -X 360 -Y 140 -W 1200 -H 800
            $g2 = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $min = Get-SessionScreenPoint -Geometry $g2 -PointName 'min'
            $minX = [int]$g2.Points.min.x
            Move-AeroCursor -Session $ctx.Session -X $min.X -Y $min.Y
            Start-Sleep -Milliseconds 400
            Invoke-AeroMouseButton -Session $ctx.Session -Down
            Start-Sleep -Milliseconds 400
            $pMin = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name 'press-min'
            [void]$frames.Add($pMin)
            Invoke-AeroMouseButton -Session $ctx.Session -Up
            Start-Sleep -Milliseconds 300
            # The min up-click minimizes: restore the suite's floating state.
            Reset-SessionWindow -Hwnd $ctx.MainHwnd -X 360 -Y 140 -W 1200 -H 800
            Move-AeroCursor -Session $ctx.Session -X 960 -Y 500
            foreach ($pair in @(@('press-max', $pMax), @('press-min', $pMin))) {
                $img = [System.Drawing.Bitmap]::FromFile($pair[1])
                $w = $img.Width
                $h = $img.Height
                $img.Dispose()
                if ($w -ne $floatingW -or $h -ne $floatingH) {
                    [void]$evidence.Add("$($pair[0]) frame ${w}x${h} but rest frame ${floatingW}x${floatingH} -- window state drifted during the check")
                    return New-SessionOutcome 'FAIL' "$($pair[0]) frame size ${w}x${h} != floating ${floatingW}x${floatingH} (state drift, not a parity verdict)"
                }
            }
            $pMaxStrip = New-SessionButtonStrip -Context $ctx -FramePath $pMax -Name 'press-max' -CenterX $maxX
            $pMinStrip = New-SessionButtonStrip -Context $ctx -FramePath $pMin -Name 'press-min' -CenterX $minX
            [void]$frames.Add($pMaxStrip)
            [void]$frames.Add($pMinStrip)
            $pressVsPress = Compare-SessionImages -Before $pMaxStrip -After $pMinStrip
            $pressVsRest = Compare-SessionImages -Before $pMaxStrip -After $restMaxStrip
            [void]$evidence.Add("press-max-vs-press-min diffPixels=$($pressVsPress.DiffPixels) maxDelta=$($pressVsPress.MaxChannelDelta)")
            [void]$evidence.Add("press-vs-rest diffPixels=$($pressVsRest.DiffPixels) maxDelta=$($pressVsRest.MaxChannelDelta)")
            if ($pressVsPress.DiffPixels -eq 0 -and $pressVsRest.DiffPixels -gt 0) {
                return New-SessionOutcome 'PASS' 'identical press fill on max and min; press visibly paints'
            }
            return New-SessionOutcome 'FAIL' "pressParityDiff=$($pressVsPress.DiffPixels) pressVisibleDiff=$($pressVsRest.DiffPixels)"
        }

        Invoke-SessionCheck -Id 'B01-CLICK' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $max = Get-SessionScreenPoint -Geometry $g -PointName 'max'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            Invoke-AeroClick -Session $ctx.Session -X $max.X -Y $max.Y
            if ($ctx.Session.IsDryRun) {
                Invoke-AeroClick -Session $ctx.Session -X $max.X -Y $max.Y
                return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real click performed'
            }
            Start-Sleep -Milliseconds 700
            $zoomed1 = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            $rep1 = Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main'
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'B01-CLICK-1'
            $g2 = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $max2 = Get-SessionScreenPoint -Geometry $g2 -PointName 'max'
            Invoke-AeroClick -Session $ctx.Session -X $max2.X -Y $max2.Y
            Start-Sleep -Milliseconds 700
            $zoomed2 = [bool][AeroWinProbe.Native]::IsZoomed($ctx.MainHwnd)
            $rep2 = Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main'
            [void]$evidence.Add("click1 zoomed=$zoomed1 placement=$(Get-ReporterField $rep1 'placement'); click2 zoomed=$zoomed2 placement=$(Get-ReporterField $rep2 'placement')")
            Add-SessionPlacementSample -Context $ctx -Hwnd $ctx.MainHwnd -Label 'B01-CLICK-2'
            if ($zoomed1 -and (-not $zoomed2)) { return New-SessionOutcome 'PASS' 'real click toggled Maximized then Floating' }
            return New-SessionOutcome 'FAIL' "toggle zoomed1=$zoomed1 zoomed2=$zoomed2"
        }

        # ---------------- BTN-02: ordinary clicks ----------------
        Invoke-SessionCheck -Id 'B02-MIN-CLICK' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $min = Get-SessionScreenPoint -Geometry $g -PointName 'min'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.MainHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            Invoke-AeroClick -Session $ctx.Session -X $min.X -Y $min.Y
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real click performed' }
            $minimized = $false
            $deadline = (Get-Date).AddSeconds(5)
            while ((Get-Date) -lt $deadline) {
                $rep = Get-WinProbeReporterState -LogPath $ctx.LogPath -Label 'main'
                if ((Get-ReporterField $rep 'minimized') -eq 'true') { $minimized = $true; break }
                Start-Sleep -Milliseconds 150
            }
            [void]$evidence.Add("reporterMinimized=$minimized isIconic=$([bool][AeroWinProbe.Native]::IsIconic($ctx.MainHwnd))")
            if ([bool][AeroWinProbe.Native]::IsIconic($ctx.MainHwnd)) {
                [void][AeroWinProbe.Native]::ShowWindow($ctx.MainHwnd, [AeroWinProbe.Native]::SW_SHOWNOACTIVATE)
                Start-Sleep -Milliseconds 300
            }
            if ($minimized) { return New-SessionOutcome 'PASS' 'min button click minimized the window' }
            return New-SessionOutcome 'FAIL' 'reporter never reported minimized=true after the click'
        }

        Invoke-SessionCheck -Id 'B02-LEADING-CLICK' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $leading = Get-SessionScreenPoint -Geometry $g -PointName 'leading'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.NarrowHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            $mark = @(Get-SessionLogLines -LogPath $ctx.LogPath).Count
            Invoke-AeroClick -Session $ctx.Session -X $leading.X -Y $leading.Y
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real click performed' }
            $event = Wait-SessionEventAfter -LogPath $ctx.LogPath -Name 'click' -Detail 'name=leading' -AfterLineCount $mark -TimeoutSec 3
            [void]$evidence.Add("event=$event")
            if ($event) { return New-SessionOutcome 'PASS' 'leading badge click reached Compose' }
            return New-SessionOutcome 'FAIL' 'no AERO_EVENT name=click name=leading within 3s'
        }

        Invoke-SessionCheck -Id 'B02-MARKED-CLICK' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $marked = Get-SessionScreenPoint -Geometry $g -PointName 'marked'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.NarrowHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            $before = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            $mark = @(Get-SessionLogLines -LogPath $ctx.LogPath).Count
            Invoke-AeroClick -Session $ctx.Session -X $marked.X -Y $marked.Y
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real click performed' }
            $event = Wait-SessionEventAfter -LogPath $ctx.LogPath -Name 'click' -Detail 'name=marked' -AfterLineCount $mark -TimeoutSec 3
            $after = Get-SessionRect -Hwnd $ctx.NarrowHwnd
            $rectUnchanged = Test-SessionRectMatch -Actual $after -Expected $before -Tolerance 2
            [void]$evidence.Add("event=$event rectUnchanged=$rectUnchanged")
            if ($event -and $rectUnchanged) { return New-SessionOutcome 'PASS' 'marked element click reached Compose; window not dragged' }
            return New-SessionOutcome 'FAIL' "event=$([bool]$event) rectUnchanged=$rectUnchanged"
        }

        Invoke-SessionCheck -Id 'B02-CLOSE-CLICK' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $g = Get-SessionGeometry -Hwnd $ctx.NarrowHwnd -Layout 'narrow'
            $caption = Get-SessionScreenPoint -Geometry $g -PointName 'caption'
            $close = Get-SessionScreenPoint -Geometry $g -PointName 'close'
            [void](Invoke-SessionFocus -Context $ctx -Hwnd $ctx.NarrowHwnd -ScreenX $caption.X -ScreenY $caption.Y -Evidence $evidence)
            $mark = @(Get-SessionLogLines -LogPath $ctx.LogPath).Count
            Invoke-AeroClick -Session $ctx.Session -X $close.X -Y $close.Y
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real click performed' }
            $event = Wait-SessionEventAfter -LogPath $ctx.LogPath -Name 'close-request' -Detail 'label=narrow' -AfterLineCount $mark -TimeoutSec 5
            $gone = -not (Test-SessionWindowAlive -Hwnd $ctx.NarrowHwnd)
            [void]$evidence.Add("event=$event gone=$gone")
            if ($gone) {
                $ctx.NarrowHwnd = Wait-SessionWindowByTitle -Title $ctx.NarrowTitle -TimeoutSec 15
                Start-Sleep -Milliseconds 500
            }
            if ($event -and $gone) { return New-SessionOutcome 'PASS' 'close button click ran onCloseRequest and destroyed the window' }
            return New-SessionOutcome 'FAIL' "event=$([bool]$event) gone=$gone"
        }

        # ---------------- PITFALLS 18: live-resize frames ----------------
        Invoke-SessionCheck -Id 'F18-RESIZE-FRAMES' -Context $ctx -Body {
            param($ctx, $evidence, $frames)
            $optTitle = if ($ctx.Session.IsDryRun) { 'aero-compose-ui Showcase [capture]' } else { 'aero-compose-ui Showcase' }
            $g = Get-SessionGeometry -Hwnd $ctx.MainHwnd
            $right = Get-SessionScreenPoint -Geometry $g -PointName 'edgeRight'
            Move-AeroCursor -Session $ctx.Session -X $right.X -Y $right.Y
            Invoke-AeroMouseButton -Session $ctx.Session -Down
            $widths = New-Object System.Collections.Generic.List[int]
            for ($i = 0; $i -lt 5; $i++) {
                $f = Invoke-SessionFrame -Context $ctx -Hwnd $ctx.MainHwnd -Name "native-$i"
                [void]$frames.Add($f)
                $r = Get-SessionRect -Hwnd $ctx.MainHwnd
                [void]$widths.Add($r.Right - $r.Left)
                Move-AeroCursor -Session $ctx.Session -X ($right.X + 25 * ($i + 1)) -Y $right.Y
                Start-Sleep -Milliseconds 350
            }
            Invoke-AeroMouseButton -Session $ctx.Session -Up
            [void]$evidence.Add("nativePathWidths=$($widths -join ',')")

            $opt = $null
            $optHwnd = [IntPtr]::Zero
            $optPids = @()
            try {
                $mainKtBeforeOpt = Get-SessionMainKtPids
                $opt = Start-SessionShowcase -Context $ctx -Task 'run' -ExtraProps @('-Paero.chromeTrace=true', '-Paero.nativeChrome=false')
                $optHwnd = Wait-SessionNewShowcaseWindow -Title $optTitle -ExcludeHwnd @($ctx.MainHwnd, $ctx.NarrowHwnd) -TimeoutSec $ReadyTimeoutSec
                $null = Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $opt.StdOut -Label 'main' } -TimeoutSec $ReadyTimeoutSec
                $optPids = @(Get-SessionMainKtPids | Where-Object { $mainKtBeforeOpt -notcontains $_ })
                Start-Sleep -Milliseconds 500
                $gOpt = Get-SessionGeometry -Hwnd $optHwnd
                $rightOpt = Get-SessionScreenPoint -Geometry $gOpt -PointName 'edgeRight'
                Move-AeroCursor -Session $ctx.Session -X $rightOpt.X -Y $rightOpt.Y
                Invoke-AeroMouseButton -Session $ctx.Session -Down
                $optWidths = New-Object System.Collections.Generic.List[int]
                for ($i = 0; $i -lt 5; $i++) {
                    $f = Invoke-SessionFrame -Context $ctx -Hwnd $optHwnd -Name "optout-$i" -SkipSanity
                    [void]$frames.Add($f)
                    $r = Get-SessionRect -Hwnd $optHwnd
                    [void]$optWidths.Add($r.Right - $r.Left)
                    Move-AeroCursor -Session $ctx.Session -X ($rightOpt.X + 25 * ($i + 1)) -Y $rightOpt.Y
                    Start-Sleep -Milliseconds 350
                }
                Invoke-AeroMouseButton -Session $ctx.Session -Up
                [void]$evidence.Add("optoutPathWidths=$($optWidths -join ',')")
            }
            finally {
                if ($optHwnd -ne [IntPtr]::Zero -and $opt) {
                    try { Stop-AeroProcessOfWindow -Hwnd $optHwnd } catch { }
                    if ($opt.Process) {
                        $exited = $opt.Process.WaitForExit(30000)
                        if (-not $exited -and -not $opt.Process.HasExited) {
                            & taskkill /PID $opt.Process.Id /T /F 2>$null | Out-Null
                        }
                    }
                }
                Stop-SessionTrackedJvms -Pids $optPids
            }
            if ($ctx.Session.IsDryRun) { return New-SessionOutcome 'UNCONFIRMED' 'dry run -- no real drag performed (both frame sets captured at rest)' }
            $distinct = @($widths | Select-Object -Unique).Count
            [void]$evidence.Add("distinctNativeWidths=$distinct")
            if ($distinct -ge 3) { return New-SessionOutcome 'PASS' "live resize visible across 5 frames ($distinct distinct widths); opt-out comparison captured" }
            return New-SessionOutcome 'FAIL' "only $distinct distinct widths during the native-path drag"
        }
    }
    finally {
        $realSw.Stop()
        $passSw.Stop()
        if ($watchStarted) {
            try { Stop-AeroShellEventWatch | Out-Null } catch { }
        }
        Stop-SessionLaunch -LaunchInfo $ctx.LaunchInfo -Hwnd $ctx.MainHwnd -TrackedPids $ctx.LaunchJvmPids

        $passCount = @($script:CurrentChecks | Where-Object { $_.Result -eq 'PASS' }).Count
        $failCount = @($script:CurrentChecks | Where-Object { $_.Result -eq 'FAIL' }).Count
        $unconfirmedCount = @($script:CurrentChecks | Where-Object { $_.Result -eq 'UNCONFIRMED' }).Count
        $total = $script:CurrentChecks.Count
        Write-Host ("SESSION TOTALS {0} pass={1} fail={2} unconfirmed={3} total={4}" -f $PassName, $passCount, $failCount, $unconfirmedCount, $total)
        Write-Host ("SESSION DURATION {0} realInputMs={1} overallMs={2}" -f $PassName, [int]$realSw.Elapsed.TotalMilliseconds, [int]$passSw.Elapsed.TotalMilliseconds)

        $hsErrFinal = Get-SessionHsErrFiles -Root $RepoRoot
        $passHsErr = @($hsErrFinal | Where-Object { $ctx.HsErrBefore -notcontains $_ })
        $passResult = [ordered]@{
            Pass          = $PassName
            Task          = $task
            DryRun        = $IsDryRun
            Api04Path     = $apiPath
            JvmKind       = $launchJvmKind
            JvmPath       = $launchJvmPath
            AppPid        = $ctx.AppPid
            Totals        = [ordered]@{ Pass = $passCount; Fail = $failCount; Unconfirmed = $unconfirmedCount; Total = $total }
            Checks        = $script:CurrentChecks.ToArray()
            PlacementSamples = @($ctx.PlacementSamples)
            NewHsErrFiles = @($passHsErr)
            RealInputMs   = [int]$realSw.Elapsed.TotalMilliseconds
            OverallMs     = [int]$passSw.Elapsed.TotalMilliseconds
        }
        $resultsPath = Join-Path $passDir 'results.json'
        $passResult | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $resultsPath -Encoding UTF8
        Write-Host "SESSION RESULTS $PassName -> $resultsPath"
    }
}

# ---------------------------------------------------------------------------
# Main flow
# ---------------------------------------------------------------------------

$overallSw = [System.Diagnostics.Stopwatch]::StartNew()
$envBefore = Get-AeroEnvState
$savedCursor = Save-AeroCursor
$envFlatBefore = ConvertTo-SessionEnvFlat -State $envBefore
foreach ($l in $envFlatBefore) { Write-Host "SESSION ENV-BEFORE $l" }

$aborted = $false

try {
    if ($Phase -eq 'EnvSetup' -or $Phase -eq 'All') {
        try {
            Invoke-SessionEnvSetup -Session $session -DriverDir $DriverDir -Installer $PowerToysInstaller
        }
        catch {
            Write-Host "SESSION ENV-SETUP ERROR: $($_.Exception.Message)"
            $aborted = $true
        }
    }
    if (-not $aborted -and ($Phase -eq 'Checks' -or $Phase -eq 'All')) {
        $passNames = @()
        if ($Pass -eq 'Jdk' -or $Pass -eq 'Both') { $passNames += 'jdk' }
        if ($Pass -eq 'Jbr' -or $Pass -eq 'Both') { $passNames += 'jbr' }
        foreach ($p in $passNames) {
            try {
                Invoke-FullPassChecks -PassName $p -Session $session -RepoRoot $RepoRoot -OutRoot $OutDir -ReadyTimeoutSec $ReadyTimeoutSec
            }
            catch {
                Write-Host "SESSION PASS $p ERROR: $($_.Exception.Message)"
                if ($_.InvocationInfo) { Write-Host "SESSION PASS $p ERROR-AT: $($_.InvocationInfo.PositionMessage)" }
                if ($_.ScriptStackTrace) { Write-Host "SESSION PASS $p ERROR-STACK: $($_.ScriptStackTrace)" }
                if ($_.Exception.StackTrace) { Write-Host "SESSION PASS $p ERROR-NETSTACK: $($_.Exception.StackTrace)" }
            }
        }
    }
}
finally {
    if ($script:EnvSetupRan) {
        if (-not $session.IsDryRun) {
            if ($null -ne $script:AutoHideOriginal) {
                try {
                    $restore = Set-AeroTaskbarAutoHide -Session $session -Enabled ([bool]$script:AutoHideOriginal)
                    Write-Host "SESSION AUTOHIDE-RESTORE before=$($restore.Before) after=$($restore.After) verified=$($restore.Verified)"
                }
                catch {
                    Write-Host "SESSION AUTOHIDE-RESTORE ERROR: $($_.Exception.Message)"
                }
            }
        }
        else {
            Add-AeroInputLog -Session $session -Action "planned: restore taskbar auto-hide to original=$($script:AutoHideOriginal) at end of -Phase All"
        }
    }

    try { Restore-AeroCursor -Session $session -Saved $savedCursor } catch { }
    $envAfter = Get-AeroEnvState
    $envFlatAfter = ConvertTo-SessionEnvFlat -State $envAfter
    foreach ($l in $envFlatAfter) { Write-Host "SESSION ENV-AFTER $l" }
    $cursorAfter = Save-AeroCursor

    $totalFail = @($script:AllChecks | Where-Object { $_.Result -eq 'FAIL' }).Count
    $totalPass = @($script:AllChecks | Where-Object { $_.Result -eq 'PASS' }).Count
    $totalUnconfirmed = @($script:AllChecks | Where-Object { $_.Result -eq 'UNCONFIRMED' }).Count
    $totalCount = $script:AllChecks.Count

    Write-Host ("SESSION TOTALS ALL pass={0} fail={1} unconfirmed={2} total={3}" -f $totalPass, $totalFail, $totalUnconfirmed, $totalCount)
    Write-Host ("SESSION DURATION ALL realInputMs=<per-pass, see above> overallMs={0}" -f [int]$overallSw.Elapsed.TotalMilliseconds)
    Write-Host "SESSION CURSOR before=$($savedCursor.X),$($savedCursor.Y) after=$($cursorAfter.X),$($cursorAfter.Y)"
    Write-Host 'SESSION PLAN:'
    foreach ($a in $session.ActionLog) { Write-Host "  $a" }

    $cursorUnchanged = ($savedCursor.X -eq $cursorAfter.X) -and ($savedCursor.Y -eq $cursorAfter.Y)
    $envUnchanged = (
        ($envBefore.ScreenCount -eq $envAfter.ScreenCount) -and
        ($envBefore.TaskbarAutoHideOn -eq $envAfter.TaskbarAutoHideOn) -and
        ($envBefore.PowerToysInstalled -eq $envAfter.PowerToysInstalled) -and
        ($envBefore.PowerToysRunning -eq $envAfter.PowerToysRunning) -and
        ($envBefore.FancyZonesRunning -eq $envAfter.FancyZonesRunning) -and
        ($envBefore.VirtualDisplayPresent -eq $envAfter.VirtualDisplayPresent)
    )

    $summary = [ordered]@{
        DryRun         = [bool]$DryRun
        AuthorizedBy   = $(if ($DryRun) { $session.AuthorizedBy } else { $session.AuthorizedBy })
        Phase          = $Phase
        Pass           = $Pass
        EnvBefore      = $envFlatBefore
        EnvAfter       = $envFlatAfter
        EnvSteps       = @($script:EnvSteps)
        EnvUnchanged   = $envUnchanged
        CursorBefore   = "($($savedCursor.X),$($savedCursor.Y))"
        CursorAfter    = "($($cursorAfter.X),$($cursorAfter.Y))"
        CursorUnchanged = $cursorUnchanged
        Totals         = [ordered]@{ Pass = $totalPass; Fail = $totalFail; Unconfirmed = $totalUnconfirmed; Total = $totalCount }
        Checks         = @($script:AllChecks | ForEach-Object { [pscustomobject]@{ Id = $_.Id; Result = $_.Result; Evidence = @($_.Evidence); Frames = @($_.Frames) } })
        OverallMs      = [int]$overallSw.Elapsed.TotalMilliseconds
        ActionLog      = @($session.ActionLog)
    }
    $summaryPath = Join-Path $OutDir 'summary.json'
    $summary | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $summaryPath -Encoding UTF8
    Write-Host "SESSION SUMMARY -> $summaryPath"

    $script:ExitCode = 0
    if ($DryRun) {
        if (-not $cursorUnchanged) {
            Write-Host 'SESSION DRYRUN FAIL: cursor moved during the dry run'
            $script:ExitCode = 1
        }
        if (-not $envUnchanged) {
            Write-Host 'SESSION DRYRUN FAIL: environment changed during the dry run (install/state drift)'
            $script:ExitCode = 1
        }
    }
    else {
        if ($totalFail -gt 0 -or $aborted) { $script:ExitCode = 1 }
    }
}

exit $script:ExitCode
