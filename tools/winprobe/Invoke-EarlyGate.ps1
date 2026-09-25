<#
.SYNOPSIS
    Scripted 1-2 minute early VER-12 real-input gate: positive control, Compose max-button hover,
    drag-to-edge snap, drag-away restore (aero-compose-ui v3.2.0).
.DESCRIPTION
    Dot-sources WinProbe.ps1, RealInput.ps1 and Watch-SnapFlyout.ps1. Every real-input action goes
    through one AeroInputSession for the whole run. Without -DryRun, -AuthorizedBy must be the
    maintainer's own "ok" reply text plus a timestamp -- this script never starts real input on
    its own initiative, and -AuthorizedBy is mandatory unless -DryRun is set (checked before
    anything else runs). -DryRun launches the showcase in capture mode (non-focusable, behind the
    maintainer's other windows) and proves the planned action list without moving the real cursor
    or starting notepad.
.NOTES
    PowerShell 5.1 only. Real input, once authorized, is expected to complete in <= 2 minutes; the
    measured duration is always printed. Each step below is wrapped in its own try/catch so one
    step failing never skips the rest, and cleanup (cursor restore, process teardown) always runs
    via the outer finally block.
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [string]$AuthorizedBy,
    [switch]$DryRun,
    [string]$Json,
    [ValidateSet('run')][string]$ShowcaseTask = 'run',
    [string]$RepoRoot = 'C:\1A_WORK\ui_lib',
    [int]$ReadyTimeoutSec = 300
)

Set-StrictMode -Version 2

if (-not $DryRun -and [string]::IsNullOrWhiteSpace($AuthorizedBy)) {
    Write-Host 'GATE ABORT: -AuthorizedBy is required unless -DryRun is set; sending no input.'
    exit 1
}

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'WinProbe.ps1')
. (Join-Path $here 'RealInput.ps1')
. (Join-Path $here 'Watch-SnapFlyout.ps1')

function Find-AeroWindowByProcessId {
    <#
    .SYNOPSIS
        Finds a visible top-level window owned by -ProcessId. Language-independent (no title
        matching) -- used instead of Find-AeroShowcaseWindow's exact-title match for notepad.exe,
        whose title text varies by locale.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][int]$ProcessId, [int]$TimeoutSec = 10)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        foreach ($hwnd in [AeroSnapWatch.Native]::EnumTopLevelVisible()) {
            [uint32]$ownerPid = 0
            [AeroSnapWatch.Native]::GetWindowThreadProcessId($hwnd, [ref]$ownerPid) | Out-Null
            if ($ownerPid -eq $ProcessId) { return $hwnd }
        }
        Start-Sleep -Milliseconds 200
    }
    return [IntPtr]::Zero
}

function Find-AeroMaxButtonSpan {
    <#
    .SYNOPSIS
        Scans -Hwnd's top 40px client row via Invoke-WinProbeHitTest (a WM_NCHITTEST query
        message, not synthetic input) for the contiguous HTMAXBUTTON (9) span -- language
        independent, no window text is ever read.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)
    $info = Get-WinProbeWindowInfo -Hwnd $Hwnd
    $clientWidth = $info.ClientRect.Right - $info.ClientRect.Left
    $y = 10
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
    if ($null -eq $start) { return $null }
    [pscustomobject]@{ CenterX = [int](($start + $end) / 2); CenterY = $y }
}

function Get-AeroMainWindowGeometry {
    <#
    .SYNOPSIS
        The 'main' layout's named points (physical pixels relative to -Hwnd's client origin) plus
        the client origin in screen coordinates, so callers can build real screen-space targets.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)
    $info = Get-WinProbeWindowInfo -Hwnd $Hwnd
    $clientWidthDp = ($info.ClientRect.Right - $info.ClientRect.Left) / $info.Scale
    $clientHeightDp = ($info.ClientRect.Bottom - $info.ClientRect.Top) / $info.Scale
    $points = Get-WinProbeNamedPoints -Layout $WinProbeLayout.main -ClientWidthDp $clientWidthDp -ClientHeightDp $clientHeightDp -Scale $info.Scale
    $origin = New-Object AeroWinProbe.WPoint
    [AeroWinProbe.Native]::ClientToScreen($Hwnd, [ref]$origin) | Out-Null
    [pscustomobject]@{ Points = $points; Origin = $origin; Info = $info }
}

function Get-AeroMonitorInfoForWindow {
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][IntPtr]$Hwnd)
    $monitor = [AeroWinProbe.Native]::MonitorFromWindow($Hwnd, [AeroWinProbe.Native]::MONITOR_DEFAULTTONEAREST)
    $monitorInfo = New-Object AeroWinProbe.MonitorInfo
    $monitorInfo.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroWinProbe.MonitorInfo]))
    [AeroWinProbe.Native]::GetMonitorInfoW($monitor, [ref]$monitorInfo) | Out-Null
    return $monitorInfo
}

Initialize-AeroDpiAwareness

$authorizedBySession = if ($DryRun -and [string]::IsNullOrWhiteSpace($AuthorizedBy)) { 'DryRun-NoAuthorization' } else { $AuthorizedBy }
$session = New-AeroInputSession -AuthorizedBy $authorizedBySession -DryRun:$DryRun

$steps = New-Object System.Collections.Generic.List[object]
function Add-GateStep {
    param([string]$Name, [string]$Result, [string]$Detail, [double]$ElapsedMs)
    [void]$steps.Add([pscustomobject]@{ Name = $Name; Result = $Result; Detail = $Detail; ElapsedMs = $ElapsedMs })
    Write-Host "GATE_STEP $Name $Result $Detail (${ElapsedMs}ms)"
}

$overallStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$realInputStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$hwnd = [IntPtr]::Zero
$launchInfo = $null
$notepadProcess = $null
$blocked = $false
$verdict = 'GATE FAIL unstarted'
$savedCursor = $null
$flyoutUnobservable = $false
$referenceSignature = $null
$flyoutVerdict = 'FLYOUT MISSING'
$snapVerdict = 'SNAP MISSING'
$restoreVerdict = 'RESTORE MISSING'
$preSnapRect = $null

try {
    # Step 0: Readiness -- never changes the maintainer's settings, only reads them.
    $stepSw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $snapSettings = Get-AeroSnapSettings
        if (-not $snapSettings.SnapAssistFlyoutOn -or -not $snapSettings.WindowArrangementActive) {
            $verdict = 'GATE BLOCKED settings'
            $blocked = $true
            Add-GateStep 'Readiness' $verdict "SnapAssistFlyoutOn=$($snapSettings.SnapAssistFlyoutOn) WindowArrangementActive=$($snapSettings.WindowArrangementActive)" $stepSw.Elapsed.TotalMilliseconds
        }
        else {
            Add-GateStep 'Readiness' 'OK' 'SnapAssistFlyoutOn=True WindowArrangementActive=True' $stepSw.Elapsed.TotalMilliseconds
        }
    }
    catch {
        $verdict = 'GATE BLOCKED settings'
        $blocked = $true
        Add-GateStep 'Readiness' $verdict "error reading snap settings: $($_.Exception.Message)" $stepSw.Elapsed.TotalMilliseconds
    }

    if (-not $blocked) {
        $stepSw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $logDir = Join-Path $RepoRoot '.captures\22-gate\logs'
            $launchProps = @('-Paero.chromeTrace=true')
            if ($DryRun) { $launchProps += '-Paero.capture=true' }
            $title = if ($DryRun) { 'aero-compose-ui Showcase [capture]' } else { 'aero-compose-ui Showcase' }

            $launchInfo = Start-WinProbeShowcase -Task $ShowcaseTask -GradleProps $launchProps -LogDir $logDir -RepoRoot $RepoRoot
            $hwnd = Find-AeroShowcaseWindow -Title $title -TimeoutSec $ReadyTimeoutSec
            Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $launchInfo.StdOut -Label 'main' } -TimeoutSec $ReadyTimeoutSec | Out-Null
            Start-Sleep -Milliseconds 500

            $windowInfo = Get-WinProbeWindowInfo -Hwnd $hwnd
            if ($windowInfo.JvmKind -eq 'JBR') {
                $verdict = 'GATE BLOCKED jvm'
                $blocked = $true
                Add-GateStep 'Launch' $verdict "jvmKind=JBR path=$($windowInfo.ProcessPath)" $stepSw.Elapsed.TotalMilliseconds
            }
            else {
                Add-GateStep 'Launch' 'OK' "jvm=standard path=$($windowInfo.ProcessPath)" $stepSw.Elapsed.TotalMilliseconds
            }
        }
        catch {
            $verdict = 'GATE FAIL launch'
            $blocked = $true
            Add-GateStep 'Launch' 'ERROR' $_.Exception.Message $stepSw.Elapsed.TotalMilliseconds
        }
    }

    $savedCursor = Save-AeroCursor

    # Step 1: PositiveControl -- a native Windows window (notepad) whose max-button is known to
    # answer HTMAXBUTTON, so "flyout not seen on the Compose window" can be told apart from
    # "UI Automation cannot see flyouts at all" (D-04).
    $stepSw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        if ($blocked) {
            Add-GateStep 'PositiveControl' 'SKIPPED' 'gate already blocked' $stepSw.Elapsed.TotalMilliseconds
        }
        elseif ($DryRun) {
            Add-GateStep 'PositiveControl' 'SKIPPED' 'notepad not started in dry run' $stepSw.Elapsed.TotalMilliseconds
        }
        else {
            $notepadProcess = Start-Process -FilePath 'notepad.exe' -PassThru
            $notepadHwnd = Find-AeroWindowByProcessId -ProcessId $notepadProcess.Id -TimeoutSec 10
            if ($notepadHwnd -eq [IntPtr]::Zero) { throw 'notepad window not found within timeout' }

            $maxSpan = Find-AeroMaxButtonSpan -Hwnd $notepadHwnd
            if (-not $maxSpan) { throw 'notepad HTMAXBUTTON span not found in its top 40px row' }

            $origin = New-Object AeroWinProbe.WPoint
            [AeroWinProbe.Native]::ClientToScreen($notepadHwnd, [ref]$origin) | Out-Null

            Invoke-AeroClick -Session $session -X ($origin.X + 20) -Y ($origin.Y + 10)

            $baseline = Get-AeroShellSnapshot
            Move-AeroCursor -Session $session -X ($origin.X + $maxSpan.CenterX) -Y ($origin.Y + $maxSpan.CenterY)
            $flyout = Wait-AeroSnapFlyout -Baseline $baseline -TimeoutMs 3000 -PollMs 100

            if ($flyout.Found) {
                $referenceSignature = $flyout
                Add-GateStep 'PositiveControl' 'OK' "flyout observed after $($flyout.DelayMs)ms" $stepSw.Elapsed.TotalMilliseconds
            }
            else {
                $flyoutUnobservable = $true
                Add-GateStep 'PositiveControl' 'D04 UNOBSERVABLE' 'UI Automation saw no new shell element on the positive control window' $stepSw.Elapsed.TotalMilliseconds
            }

            Move-AeroCursor -Session $session -X ($origin.X - 100) -Y ($origin.Y - 100)
            Send-AeroKeyChord -Session $session -Keys @('Escape')
        }
    }
    catch {
        Add-GateStep 'PositiveControl' 'ERROR' $_.Exception.Message $stepSw.Elapsed.TotalMilliseconds
    }
    finally {
        if ($notepadProcess) {
            try { Stop-Process -Id $notepadProcess.Id -Force -ErrorAction SilentlyContinue } catch { }
        }
    }

    # Step 2: ComposeFlyout -- hover the showcase window's own max button, same watcher, compared
    # against the positive control's reference signature.
    $stepSw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        if ($blocked) {
            Add-GateStep 'ComposeFlyout' 'SKIPPED' 'gate already blocked' $stepSw.Elapsed.TotalMilliseconds
        }
        else {
            $geom = Get-AeroMainWindowGeometry -Hwnd $hwnd
            $captionX = $geom.Origin.X + $geom.Points.caption.x
            $captionY = $geom.Origin.Y + $geom.Points.caption.y
            $maxX = $geom.Origin.X + $geom.Points.max.x
            $maxY = $geom.Origin.Y + $geom.Points.max.y

            Invoke-AeroClick -Session $session -X $captionX -Y $captionY
            $baseline = Get-AeroShellSnapshot
            Move-AeroCursor -Session $session -X $maxX -Y $maxY
            $observed = Wait-AeroSnapFlyout -Baseline $baseline -TimeoutMs 3000 -PollMs 100

            if ($DryRun) {
                $flyoutVerdict = 'FLYOUT MISSING'
                Add-GateStep 'ComposeFlyout' $flyoutVerdict 'dry run -- no real hover performed' $stepSw.Elapsed.TotalMilliseconds
            }
            elseif ($flyoutUnobservable) {
                $flyoutVerdict = 'FLYOUT MISSING'
                Add-GateStep 'ComposeFlyout' $flyoutVerdict 'positive control was D04 UNOBSERVABLE; Compose result is not meaningful' $stepSw.Elapsed.TotalMilliseconds
            }
            elseif ($observed.Found -and $referenceSignature -and (Test-AeroFlyoutSignatureMatch -Observed $observed -Reference $referenceSignature).IsMatch) {
                $flyoutVerdict = 'FLYOUT OK'
                Add-GateStep 'ComposeFlyout' $flyoutVerdict "matched reference after $($observed.DelayMs)ms" $stepSw.Elapsed.TotalMilliseconds
            }
            else {
                $flyoutVerdict = 'FLYOUT MISSING'
                Add-GateStep 'ComposeFlyout' $flyoutVerdict "found=$($observed.Found)" $stepSw.Elapsed.TotalMilliseconds
            }

            Move-AeroCursor -Session $session -X ($maxX - 150) -Y ($maxY + 150)
            Send-AeroKeyChord -Session $session -Keys @('Escape')
        }
    }
    catch {
        Add-GateStep 'ComposeFlyout' 'ERROR' $_.Exception.Message $stepSw.Elapsed.TotalMilliseconds
    }

    # Step 3: DragSnap -- drag the showcase's caption to the monitor's left work-area edge.
    $stepSw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        if ($blocked) {
            Add-GateStep 'DragSnap' 'SKIPPED' 'gate already blocked' $stepSw.Elapsed.TotalMilliseconds
        }
        else {
            $windowRectBefore = New-Object AeroWinProbe.WRect
            [AeroWinProbe.Native]::GetWindowRect($hwnd, [ref]$windowRectBefore) | Out-Null
            $preSnapRect = $windowRectBefore

            $geom = Get-AeroMainWindowGeometry -Hwnd $hwnd
            $captionX = $geom.Origin.X + $geom.Points.caption.x
            $captionY = $geom.Origin.Y + $geom.Points.caption.y

            $monitorInfo = Get-AeroMonitorInfoForWindow -Hwnd $hwnd
            $workLeft = $monitorInfo.rcWork.Left

            Invoke-AeroDrag -Session $session -FromX $captionX -FromY $captionY -ToX ($workLeft + 1) -ToY $captionY -Steps 20 -DurationMs 700 -HoldMs 500

            if (-not $DryRun) { Start-Sleep -Milliseconds 800 }
            Send-AeroKeyChord -Session $session -Keys @('Escape')

            $windowRectAfter = New-Object AeroWinProbe.WRect
            [AeroWinProbe.Native]::GetWindowRect($hwnd, [ref]$windowRectAfter) | Out-Null
            $reporterAfterSnap = Get-WinProbeReporterState -LogPath $launchInfo.StdOut -Label 'main'

            $expectedRight = $workLeft + [int](($monitorInfo.rcWork.Right - $monitorInfo.rcWork.Left) / 2)
            $withinTolerance = (
                [Math]::Abs($windowRectAfter.Left - $workLeft) -le 8 -and
                [Math]::Abs($windowRectAfter.Top - $monitorInfo.rcWork.Top) -le 8 -and
                [Math]::Abs($windowRectAfter.Right - $expectedRight) -le 8 -and
                [Math]::Abs($windowRectAfter.Bottom - $monitorInfo.rcWork.Bottom) -le 8
            )

            if ($DryRun) {
                $snapVerdict = 'SNAP MISSING'
                Add-GateStep 'DragSnap' $snapVerdict 'dry run -- no real drag performed' $stepSw.Elapsed.TotalMilliseconds
            }
            elseif ($withinTolerance) {
                $snapVerdict = 'SNAP OK'
                Add-GateStep 'DragSnap' $snapVerdict "rect=$($windowRectAfter.Left),$($windowRectAfter.Top),$($windowRectAfter.Right),$($windowRectAfter.Bottom) placement=$($reporterAfterSnap.placement)" $stepSw.Elapsed.TotalMilliseconds
            }
            else {
                $snapVerdict = 'SNAP MISSING'
                Add-GateStep 'DragSnap' $snapVerdict "rect=$($windowRectAfter.Left),$($windowRectAfter.Top),$($windowRectAfter.Right),$($windowRectAfter.Bottom) expected=leftHalfOf($($monitorInfo.rcWork.Left),$($monitorInfo.rcWork.Top),$($monitorInfo.rcWork.Right),$($monitorInfo.rcWork.Bottom))" $stepSw.Elapsed.TotalMilliseconds
            }
        }
    }
    catch {
        Add-GateStep 'DragSnap' 'ERROR' $_.Exception.Message $stepSw.Elapsed.TotalMilliseconds
    }

    # Step 4: DragAway -- drag the (possibly snapped) window to screen center; its size must
    # return to the pre-snap floating size.
    $stepSw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        if ($blocked) {
            Add-GateStep 'DragAway' 'SKIPPED' 'gate already blocked' $stepSw.Elapsed.TotalMilliseconds
        }
        else {
            $geom = Get-AeroMainWindowGeometry -Hwnd $hwnd
            $captionX = $geom.Origin.X + $geom.Points.caption.x
            $captionY = $geom.Origin.Y + $geom.Points.caption.y

            $monitorInfo = Get-AeroMonitorInfoForWindow -Hwnd $hwnd
            $centerX = [int](($monitorInfo.rcMonitor.Left + $monitorInfo.rcMonitor.Right) / 2)
            $centerY = [int](($monitorInfo.rcMonitor.Top + $monitorInfo.rcMonitor.Bottom) / 2)

            Invoke-AeroDrag -Session $session -FromX $captionX -FromY $captionY -ToX $centerX -ToY $centerY -Steps 20 -DurationMs 700 -HoldMs 200

            if (-not $DryRun) { Start-Sleep -Milliseconds 500 }

            $windowRectRestored = New-Object AeroWinProbe.WRect
            [AeroWinProbe.Native]::GetWindowRect($hwnd, [ref]$windowRectRestored) | Out-Null

            $restoredW = $windowRectRestored.Right - $windowRectRestored.Left
            $restoredH = $windowRectRestored.Bottom - $windowRectRestored.Top
            $preSnapW = if ($preSnapRect) { $preSnapRect.Right - $preSnapRect.Left } else { -1 }
            $preSnapH = if ($preSnapRect) { $preSnapRect.Bottom - $preSnapRect.Top } else { -1 }
            $sizeMatch = ([Math]::Abs($restoredW - $preSnapW) -le 2) -and ([Math]::Abs($restoredH - $preSnapH) -le 2)

            if ($DryRun) {
                $restoreVerdict = 'RESTORE MISSING'
                Add-GateStep 'DragAway' $restoreVerdict 'dry run -- no real drag performed' $stepSw.Elapsed.TotalMilliseconds
            }
            elseif ($sizeMatch) {
                $restoreVerdict = 'RESTORE OK'
                Add-GateStep 'DragAway' $restoreVerdict "restored=${restoredW}x${restoredH} preSnap=${preSnapW}x${preSnapH}" $stepSw.Elapsed.TotalMilliseconds
            }
            else {
                $restoreVerdict = 'RESTORE MISSING'
                Add-GateStep 'DragAway' $restoreVerdict "restored=${restoredW}x${restoredH} preSnap=${preSnapW}x${preSnapH}" $stepSw.Elapsed.TotalMilliseconds
            }
        }
    }
    catch {
        Add-GateStep 'DragAway' 'ERROR' $_.Exception.Message $stepSw.Elapsed.TotalMilliseconds
    }
}
finally {
    # Step 5: Cleanup -- always restore the cursor and stop what this run started, regardless of
    # what happened above.
    $realInputStopwatch.Stop()
    try {
        if ($savedCursor) { Restore-AeroCursor -Session $session -Saved $savedCursor }
    }
    catch {
        Add-GateStep 'Cleanup' 'ERROR' "cursor restore failed: $($_.Exception.Message)" 0
    }

    if ($hwnd -ne [IntPtr]::Zero) {
        Stop-AeroProcessOfWindow -Hwnd $hwnd
        if ($launchInfo -and $launchInfo.Process) {
            $exited = $launchInfo.Process.WaitForExit(30000)
            if (-not $exited -and -not $launchInfo.Process.HasExited) {
                & taskkill /PID $launchInfo.Process.Id /T /F 2>$null | Out-Null
            }
        }
    }
    Remove-AeroOrphanShowcaseProcesses | Out-Null

    if (-not $blocked) {
        if ($flyoutVerdict -eq 'FLYOUT OK' -and $snapVerdict -eq 'SNAP OK' -and $restoreVerdict -eq 'RESTORE OK' -and -not $flyoutUnobservable) {
            $verdict = 'GATE PASS'
        }
        else {
            $reasons = New-Object System.Collections.Generic.List[string]
            if ($flyoutUnobservable) { $reasons.Add('D04 UNOBSERVABLE') }
            if ($flyoutVerdict -ne 'FLYOUT OK') { $reasons.Add($flyoutVerdict) }
            if ($snapVerdict -ne 'SNAP OK') { $reasons.Add($snapVerdict) }
            if ($restoreVerdict -ne 'RESTORE OK') { $reasons.Add($restoreVerdict) }
            $verdict = "GATE FAIL $($reasons -join ',')"
        }
    }

    $cursorAfter = Save-AeroCursor
    Write-Host "GATE_DURATION realInputMs=$($realInputStopwatch.Elapsed.TotalMilliseconds) overallMs=$($overallStopwatch.Elapsed.TotalMilliseconds)"
    Write-Host 'GATE_PLAN:'
    foreach ($a in $session.ActionLog) { Write-Host "  $a" }
    Write-Host "GATE_CURSOR before=$($savedCursor.X),$($savedCursor.Y) after=$($cursorAfter.X),$($cursorAfter.Y)"
    Write-Host $verdict

    if ($Json) {
        $jsonDir = Split-Path -Path $Json -Parent
        if ($jsonDir -and -not (Test-Path -LiteralPath $jsonDir)) {
            New-Item -ItemType Directory -Path $jsonDir -Force | Out-Null
        }
        $output = [ordered]@{
            Verdict       = $verdict
            Blocked       = $blocked
            DryRun        = [bool]$DryRun
            Steps         = $steps
            ActionLog     = @($session.ActionLog)
            CursorBefore  = $savedCursor
            CursorAfter   = $cursorAfter
            RealInputMs   = $realInputStopwatch.Elapsed.TotalMilliseconds
            OverallMs     = $overallStopwatch.Elapsed.TotalMilliseconds
        }
        $output | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Json -Encoding UTF8
    }
}

if ($DryRun) {
    # A dry run never sends real input, so FLYOUT/SNAP/RESTORE can never legitimately be OK --
    # its exit code reports whether the dry run itself completed and proved the cursor unchanged,
    # not the (deliberately inapplicable) pass/fail verdict.
    $cursorUnchanged = $savedCursor -and $cursorAfter -and ($savedCursor.X -eq $cursorAfter.X) -and ($savedCursor.Y -eq $cursorAfter.Y)
    if ($cursorUnchanged) { exit 0 } else { exit 1 }
}
elseif ($verdict -eq 'GATE PASS') { exit 0 } else { exit 1 }
