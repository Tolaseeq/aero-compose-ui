<#
.SYNOPSIS
    Runner for the VER-11 live-window probe: launches the showcase (or attaches to Title), runs
    the requested reports, evaluates VER-11 checks, and always stops what it started.
.DESCRIPTION
    See tools/winprobe/WinProbe.ps1 for the underlying probe library. -SelfTest runs entirely
    offline (no launch, no window access) and only proves the point-computation math and the
    non-interference grep gate.
.NOTES
    PowerShell 5.1 only. Never sends synthetic mouse/keyboard input; -Capture (default on) keeps
    the launched window non-focusable and behind the reviewer's other windows.
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [ValidateSet('none', 'run', 'hotRun')][string]$Launch = 'none',
    [bool]$Capture = $true,
    [string[]]$GradleProps = @(),
    [string]$Title,
    [string[]]$Report = @('styles', 'children', 'hittest', 'maximize', 'taskbar', 'minsize', 'uia', 'process', 'v11'),
    [switch]$SkipMaximize,
    [string]$Json,
    [switch]$AssertV11,
    [switch]$ExpectRed,
    [switch]$KeepRunning,
    [switch]$SelfTest,
    [string]$RepoRoot = 'C:\1A_WORK\ui_lib',
    [int]$ReadyTimeoutSec = 300,
    [string]$Label = 'main'
)

Set-StrictMode -Version 2

# Some hosts pass a comma-separated -Report value through as a single string instead of splitting
# it into array elements before binding (mirrors tools/capture/Invoke-ShowcaseSweep.ps1's -Themes
# / -Sections normalization) — split and validate manually rather than via [ValidateSet], which
# rejects a joined string outright before this normalization can run.
$Report = @($Report | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' })
$validReports = @('styles', 'children', 'hittest', 'maximize', 'taskbar', 'minsize', 'uia', 'process', 'v11')
$invalidReports = @($Report | Where-Object { $validReports -notcontains $_ })
if ($invalidReports.Count -gt 0) {
    throw "Invoke-WinProbe: -Report has unknown value(s) $($invalidReports -join ','); valid values are $($validReports -join ',')"
}

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'WinProbe.ps1')

if ($SelfTest) {
    Write-Output "WINPROBE_SELFTEST types-compiled=$([bool]('AeroWinProbe.Native' -as [type]))"
    if (-not $WinProbeLayout.ContainsKey('main')) {
        Write-Output 'WINPROBE_SELFTEST FAIL: $WinProbeLayout.main missing'
        exit 1
    }
    Write-Output 'WINPROBE_SELFTEST layout=main present'

    $ok = $true
    foreach ($scale in @(1.0, 1.5)) {
        $points = Get-WinProbeNamedPoints -Layout $WinProbeLayout.main -ClientWidthDp 1200 -ClientHeightDp 800 -Scale $scale
        foreach ($name in $points.Keys) {
            $p = $points[$name]
            Write-Output "WINPROBE_SELFTEST scale=$scale point=$name x=$($p.x) y=$($p.y)"
        }
        $expectedMaxX = [int][Math]::Round((1200 - 8 - 46 - 23) * $scale)
        if ($points['max'].x -ne $expectedMaxX) {
            Write-Output "WINPROBE_SELFTEST FAIL: scale=$scale expected max.x=$expectedMaxX got $($points['max'].x)"
            $ok = $false
        }
    }

    if ($ok) {
        Write-Output 'WINPROBE_SELFTEST PASS'
        exit 0
    }
    else {
        exit 1
    }
}

if (-not $Title) {
    $Title = if ($Capture) { 'aero-compose-ui Showcase [capture]' } else { 'aero-compose-ui Showcase' }
}

Initialize-AeroDpiAwareness

$logDir = Join-Path $RepoRoot '.captures\22-baseline\logs'
$launchInfo = $null
$hwnd = [IntPtr]::Zero

try {
    if ($Launch -ne 'none') {
        $props = @($GradleProps)
        if ($Capture) { $props += '-Paero.capture=true' }
        $launchInfo = Start-WinProbeShowcase -Task $Launch -GradleProps $props -LogDir $logDir -RepoRoot $RepoRoot
        $hwnd = Find-AeroShowcaseWindow -Title $Title -TimeoutSec $ReadyTimeoutSec
        Wait-WinProbeReporter -Predicate { Get-WinProbeReporterState -LogPath $launchInfo.StdOut -Label $Label } -TimeoutSec $ReadyTimeoutSec | Out-Null
        Start-Sleep -Milliseconds 500
    }
    else {
        $hwnd = Find-AeroShowcaseWindow -Title $Title -TimeoutSec 15
    }

    $output = [ordered]@{}

    if ($Report -contains 'process' -or $Report -contains 'styles') {
        $windowInfo = Get-WinProbeWindowInfo -Hwnd $hwnd
        $output['windowInfo'] = $windowInfo
        Write-Output "PROCESS pid=$($windowInfo.ProcessId) path=$($windowInfo.ProcessPath) jvmKind=$($windowInfo.JvmKind)"
        Write-Output "STYLE $($windowInfo.StyleHex) exStyle=$($windowInfo.ExStyleHex) names=$($windowInfo.StyleNames -join ',')"
    }

    if ($Report -contains 'children') {
        $children = Get-WinProbeChildren -Hwnd $hwnd
        $output['children'] = $children
        foreach ($c in $children) {
            Write-Output "CHILD class=$($c.ClassName) rect=$($c.RelLeft),$($c.RelTop),$($c.RelRight),$($c.RelBottom) visible=$($c.IsVisible) coversClient=$($c.CoversClient)"
        }
    }

    if ($Report -contains 'hittest') {
        $info = Get-WinProbeWindowInfo -Hwnd $hwnd
        $clientWidthDp = ($info.ClientRect.Right - $info.ClientRect.Left) / $info.Scale
        $clientHeightDp = ($info.ClientRect.Bottom - $info.ClientRect.Top) / $info.Scale
        $points = Get-WinProbeNamedPoints -Layout $WinProbeLayout.main -ClientWidthDp $clientWidthDp -ClientHeightDp $clientHeightDp -Scale $info.Scale
        $hitResults = [ordered]@{}
        foreach ($name in $points.Keys) {
            $p = $points[$name]
            $hit = Invoke-WinProbeHitTest -Hwnd $hwnd -ClientX $p.x -ClientY $p.y
            $hitResults[$name] = $hit
            Write-Output "HITTEST point=$name code=$($hit.Code) name=$($hit.CodeName) chain=$($hit.Chain)"
        }
        $output['hittest'] = $hitResults
    }

    if ($Report -contains 'taskbar') {
        $taskbar = Get-WinProbeTaskbar -Hwnd $hwnd
        $output['taskbar'] = $taskbar
        Write-Output "TASKBAR autoHideOn=$($taskbar.AutoHideOn) autoHideEdges=$($taskbar.AutoHideEdges -join ',')"
    }

    if ($Report -contains 'minsize') {
        $minsize = Invoke-WinProbeMinSize -Hwnd $hwnd
        $output['minsize'] = $minsize
        Write-Output "MINSIZE requested=$($minsize.RequestedWidth)x$($minsize.RequestedHeight) obtained=$($minsize.ObtainedWidth)x$($minsize.ObtainedHeight)"
    }

    if ($Report -contains 'uia') {
        $uia = Get-WinProbeUiaSummary -Hwnd $hwnd
        $output['uia'] = $uia
        Write-Output "UIA descendants=$($uia.DescendantCount)"
    }

    if ($Report -contains 'maximize' -and -not $SkipMaximize) {
        $maxResult = Invoke-WinProbeMaximize -Hwnd $hwnd -LogPath $(if ($launchInfo) { $launchInfo.StdOut } else { $null }) -Label $Label
        $output['maximize'] = $maxResult
        Write-Output "MAXIMIZE foregroundTaken=$($maxResult.ForegroundTaken) rcWork=$($maxResult.MonitorRcWork.Left),$($maxResult.MonitorRcWork.Top),$($maxResult.MonitorRcWork.Right),$($maxResult.MonitorRcWork.Bottom) windowRect=$($maxResult.WindowRectMaximized.Left),$($maxResult.WindowRectMaximized.Top),$($maxResult.WindowRectMaximized.Right),$($maxResult.WindowRectMaximized.Bottom)"
    }

    $v11Results = $null
    if ($Report -contains 'v11') {
        $v11Results = Invoke-WinProbeV11 -Hwnd $hwnd -Layout 'main' -LogPath $(if ($launchInfo) { $launchInfo.StdOut } else { $null }) -Label $Label -SkipMaximize:$SkipMaximize
        $output['v11'] = $v11Results
    }

    if ($Json) {
        $jsonDir = Split-Path -Path $Json -Parent
        if ($jsonDir -and -not (Test-Path -LiteralPath $jsonDir)) {
            New-Item -ItemType Directory -Path $jsonDir -Force | Out-Null
        }
        $output | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Json -Encoding UTF8
    }

    $exitCode = 0

    if ($v11Results) {
        $failedIds = @($v11Results | Where-Object { $_.Result -eq 'FAIL' } | ForEach-Object { $_.Id })
        $passedIds = @($v11Results | Where-Object { $_.Result -eq 'PASS' } | ForEach-Object { $_.Id })

        if ($AssertV11) {
            if ($failedIds.Count -gt 0) {
                Write-Output "V11 ASSERT FAIL: $($failedIds -join ',')"
                $exitCode = 1
            }
        }

        if ($ExpectRed) {
            if ($passedIds.Count -eq 0) {
                Write-Output 'RED OK'
            }
            else {
                Write-Output "RED BROKEN $($passedIds -join ',')"
                $exitCode = 1
            }
        }
    }

    exit $exitCode
}
finally {
    if (-not $KeepRunning) {
        if ($hwnd -ne [IntPtr]::Zero -and $Launch -ne 'none') {
            # Stop by PID, never by window message: the window owner first, then the gradle
            # process tree if it's still alive after that (mirrors Invoke-ShowcaseSweep.ps1's
            # Stop-ShowcaseLaunch precedent).
            Stop-AeroProcessOfWindow -Hwnd $hwnd
            if ($launchInfo -and $launchInfo.Process) {
                $exited = $launchInfo.Process.WaitForExit(30000)
                if (-not $exited -and -not $launchInfo.Process.HasExited) {
                    & taskkill /PID $launchInfo.Process.Id /T /F 2>$null | Out-Null
                }
            }
        }
        Remove-AeroOrphanShowcaseProcesses | Out-Null
    }
}
