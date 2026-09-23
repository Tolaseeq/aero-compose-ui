<#
.SYNOPSIS
    Drives the showcase through every section x theme x page, captures each frame via
    Invoke-AeroWindowCapture, and writes a manifest (BASE-02 driver).
.DESCRIPTION
    Launches `:showcase:run -Paero.capture=true` once per (theme, section, page), waits for the
    single AERO_READY line, captures -Captures frames spaced -CaptureGapMs apart, then terminates
    the launch by PID before moving to the next combination — never two Gradle launches at once.
    -SelfTest runs the covered/minimized/no-interference proof instead of a sweep.
#>

#Requires -Version 5.1

[CmdletBinding()]
param(
    [string]$RepoRoot = 'C:\1A_WORK\ui_lib',
    [Parameter(Mandatory = $true)][string]$OutDir,
    [string[]]$Themes = @('AeroBlue', 'AeroDark', 'Classic'),
    [string[]]$Sections = @(
        'ThemeSwitcher', 'Verification', 'Foundation', 'Primitives', 'Icons', 'Buttons',
        'Input', 'Selection', 'Dropdown', 'Range', 'List', 'Containers', 'Overlays',
        'Navigation', 'Data', 'Pickers', 'Layout'
    ),
    [int[]]$Pages,
    [int]$Captures = 3,
    [int]$CaptureGapMs = 700,
    [int]$SettleMs = 1500,
    [int]$ReadyTimeoutSec = 300,
    [switch]$SelfTest
)

Set-StrictMode -Version 2

# Some hosts pass a comma-separated -Themes/-Sections value through as a single string instead of
# splitting it into array elements before binding; normalize either shape the same way.
$Themes = @($Themes | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' })
$Sections = @($Sections | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' })

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'AeroCapture.ps1')
Initialize-AeroDpiAwareness

# D-01: every image artifact of this phase lives under this exact folder — never a worktree path.
$CanonicalCapturesRoot = 'C:\1A_WORK\ui_lib\.captures'
$resolvedOutDir = [System.IO.Path]::GetFullPath($OutDir).TrimEnd('\')
if ($resolvedOutDir -ne $CanonicalCapturesRoot -and
    -not $resolvedOutDir.StartsWith($CanonicalCapturesRoot + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Invoke-ShowcaseSweep: -OutDir must resolve under $CanonicalCapturesRoot (got $resolvedOutDir)"
}
$OutDir = $resolvedOutDir
if (-not (Test-Path -LiteralPath $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}
$logDir = Join-Path $OutDir 'logs'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

$CaptureTitle = 'aero-compose-ui Showcase [capture]'

function Get-AeroToolchainInfo {
    <#
    .SYNOPSIS
        Reads kotlin/composeMultiplatform/kotlinxCoroutines/kotlinxDatetime/junit from the version
        catalog and the Gradle version from the wrapper properties.
    #>
    param([Parameter(Mandatory = $true)][string]$RepoRoot)

    $toolchain = [ordered]@{}
    $catalogPath = Join-Path $RepoRoot 'gradle\libs.versions.toml'
    if (Test-Path -LiteralPath $catalogPath) {
        $catalog = Get-Content -LiteralPath $catalogPath -Raw
        foreach ($name in 'kotlin', 'composeMultiplatform', 'kotlinxCoroutines', 'kotlinxDatetime', 'junit') {
            if ($catalog -match "(?m)^$name\s*=\s*""([^""]+)""") {
                $toolchain[$name] = $Matches[1]
            }
        }
    }
    $wrapperPath = Join-Path $RepoRoot 'gradle\wrapper\gradle-wrapper.properties'
    if (Test-Path -LiteralPath $wrapperPath) {
        $wrapper = Get-Content -LiteralPath $wrapperPath -Raw
        if ($wrapper -match 'gradle-([0-9.]+)-bin\.zip') {
            $toolchain['gradle'] = $Matches[1]
        }
    }
    return $toolchain
}

function Start-ShowcaseCaptureLaunch {
    <#
    .SYNOPSIS
        Starts one `gradlew.bat :showcase:run -Paero.capture=true` launch in the background,
        redirecting stdout/stderr to per-launch log files under $LogDir.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$Theme,
        [Parameter(Mandatory = $true)][string]$Section,
        [Parameter(Mandatory = $true)][int]$Page,
        [Parameter(Mandatory = $true)][string]$LogDir
    )
    $stamp = Get-Date -Format 'HHmmss.fff'
    $stdout = Join-Path $LogDir "$Theme-$Section-p$Page-$stamp.out.log"
    $stderr = Join-Path $LogDir "$Theme-$Section-p$Page-$stamp.err.log"
    $gradlewPath = Join-Path $RepoRoot 'gradlew.bat'
    $argList = @(
        '--console=plain',
        ':showcase:run',
        "-Paero.scheme=$Theme",
        "-Paero.section=$Section",
        "-Paero.page=$Page",
        '-Paero.capture=true'
    )
    $process = Start-Process -FilePath $gradlewPath -ArgumentList $argList -WorkingDirectory $RepoRoot `
        -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    [pscustomobject]@{
        Process = $process
        StdOut  = $stdout
        StdErr  = $stderr
    }
}

function Wait-AeroReadyLine {
    <#
    .SYNOPSIS
        Polls a launch's stdout log for a single AERO_READY line; throws on AERO_SECTION_UNKNOWN
        or on timeout.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$LogPath,
        [Parameter(Mandatory = $true)][int]$TimeoutSec
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Path -LiteralPath $LogPath) {
            $content = Get-Content -LiteralPath $LogPath -ErrorAction SilentlyContinue
            foreach ($line in $content) {
                if ($line -match '^AERO_SECTION_UNKNOWN ') {
                    throw "Wait-AeroReadyLine: $line"
                }
                if ($line -match '^AERO_READY ') {
                    return $line
                }
            }
        }
        Start-Sleep -Milliseconds 200
    }
    throw "Wait-AeroReadyLine: timed out after $TimeoutSec s waiting for AERO_READY in $LogPath"
}

function ConvertFrom-AeroReadyLine {
    <#
    .SYNOPSIS
        Parses an `AERO_READY key=value ...` line into a hashtable.
    #>
    param([Parameter(Mandatory = $true)][string]$Line)
    $fields = @{}
    foreach ($part in ($Line -split ' ')) {
        if ($part -match '^([A-Za-z]+)=(.*)$') {
            $fields[$Matches[1]] = $Matches[2]
        }
    }
    return $fields
}

function Stop-ShowcaseLaunch {
    <#
    .SYNOPSIS
        Terminates a launch by PID (window owner first, then the gradle process tree), never by
        window message.
    #>
    param(
        [IntPtr]$Hwnd,
        [System.Diagnostics.Process]$GradleProcess
    )
    if ($Hwnd -and $Hwnd -ne [IntPtr]::Zero) {
        Stop-AeroProcessOfWindow -Hwnd $Hwnd
    }
    if ($GradleProcess) {
        $exited = $GradleProcess.WaitForExit(60000)
        if (-not $exited) {
            try {
                if (-not $GradleProcess.HasExited) {
                    & taskkill /PID $GradleProcess.Id /T /F 2>$null | Out-Null
                }
            }
            catch {
                # Process already gone.
            }
        }
    }
}

function Remove-AeroOrphanShowcaseProcesses {
    <#
    .SYNOPSIS
        Kills any leftover showcase JVM by command line, in case a launch's own termination
        missed a child process.
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

function Invoke-AeroCaptureLaunchCycle {
    <#
    .SYNOPSIS
        Launches one (theme, section, page), waits for AERO_READY, asserts the window never took
        foreground on appearance, captures -Captures frames, then always terminates the launch.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$Theme,
        [Parameter(Mandatory = $true)][string]$Section,
        [Parameter(Mandatory = $true)][int]$Page,
        [Parameter(Mandatory = $true)][string]$LogDir,
        [Parameter(Mandatory = $true)][int]$ReadyTimeoutSec,
        [Parameter(Mandatory = $true)][int]$SettleMs,
        [Parameter(Mandatory = $true)][int]$CaptureGapMs,
        [Parameter(Mandatory = $true)][int]$Captures,
        [string]$OutFileBase
    )

    $hwnd = [IntPtr]::Zero
    $launch = Start-ShowcaseCaptureLaunch -RepoRoot $RepoRoot -Theme $Theme -Section $Section -Page $Page -LogDir $LogDir
    try {
        $readyLine = Wait-AeroReadyLine -LogPath $launch.StdOut -TimeoutSec $ReadyTimeoutSec
        $ready = ConvertFrom-AeroReadyLine -Line $readyLine

        $hwnd = Find-AeroShowcaseWindow -Title $CaptureTitle -TimeoutSec $ReadyTimeoutSec
        $stateAfterAppear = Get-AeroInputState
        if ($stateAfterAppear.ForegroundHwnd -eq [int64]$hwnd) {
            throw "LAUNCH ACTIVATED WINDOW: $Theme/$Section p$Page took foreground on appearance"
        }

        Start-Sleep -Milliseconds $SettleMs

        $files = New-Object System.Collections.Generic.List[string]
        $lastCapture = $null
        for ($k = 1; $k -le $Captures; $k++) {
            $file = if ($OutFileBase) { "$OutFileBase-c$k.png" } else { Join-Path $OutDir "$Theme\$Section-p$Page-c$k.png" }
            $lastCapture = Invoke-AeroWindowCapture -Hwnd $hwnd -OutFile $file -ExpectedBackgroundArgb $ready.background
            [void]$files.Add($file)
            if ($k -lt $Captures) {
                Start-Sleep -Milliseconds $CaptureGapMs
            }
        }

        return [pscustomobject]@{
            Ready   = $ready
            Files   = $files
            Capture = $lastCapture
            Hwnd    = $hwnd
        }
    }
    finally {
        Stop-ShowcaseLaunch -Hwnd $hwnd -GradleProcess $launch.Process
    }
}

if ($SelfTest) {
    # One launch, kept alive through all three captures (c1/c2/c3) — Invoke-AeroCaptureLaunchCycle
    # terminates its launch on return, which would kill the window before the covered/minimized
    # cases ran, so this block owns the launch lifecycle itself instead of calling that helper.
    $hwnd = [IntPtr]::Zero
    $launch = Start-ShowcaseCaptureLaunch -RepoRoot $RepoRoot -Theme 'AeroBlue' -Section 'Buttons' -Page 0 -LogDir $logDir
    try {
        $readyLine = Wait-AeroReadyLine -LogPath $launch.StdOut -TimeoutSec $ReadyTimeoutSec
        $ready = ConvertFrom-AeroReadyLine -Line $readyLine

        $hwnd = Find-AeroShowcaseWindow -Title $CaptureTitle -TimeoutSec $ReadyTimeoutSec
        $stateAfterAppear = Get-AeroInputState
        if ($stateAfterAppear.ForegroundHwnd -eq [int64]$hwnd) {
            throw "LAUNCH ACTIVATED WINDOW: AeroBlue/Buttons p0 took foreground on appearance"
        }

        Start-Sleep -Milliseconds $SettleMs

        $c1File = Join-Path $OutDir 'c1.png'
        $c1Capture = Invoke-AeroWindowCapture -Hwnd $hwnd -OutFile $c1File -ExpectedBackgroundArgb $ready.background
        $selftestDpi = $c1Capture.Dpi

        # --- Covered case ---
        $occluderProcess = Show-AeroOccluder -TargetHwnd $hwnd -Seconds 20
        Start-Sleep -Milliseconds 1000
        $occludedVerified = Test-AeroOccluded -Hwnd $hwnd

        $c2File = Join-Path $OutDir 'c2.png'
        $c2Capture = Invoke-AeroWindowCapture -Hwnd $hwnd -OutFile $c2File -ExpectedBackgroundArgb $ready.background

        if ($occluderProcess -and -not $occluderProcess.HasExited) {
            Stop-Process -Id $occluderProcess.Id -Force -ErrorAction SilentlyContinue
        }

        $c2Bitmap = New-Object System.Drawing.Bitmap($c2File)
        $magentaPresent = [AeroPixels]::HasColorBlock($c2Bitmap, [System.Drawing.Color]::FromArgb(255, 255, 0, 255).ToArgb(), 16)
        $c2Bitmap.Dispose()
        $magentaAbsent = -not $magentaPresent

        # --- Minimized case ---
        $inputBeforeMinimize = Get-AeroInputState
        [AeroCaptureNative]::ShowWindow($hwnd, [AeroCaptureNative]::SW_SHOWMINNOACTIVE) | Out-Null
        Start-Sleep -Milliseconds 500
        $isIconicNow = [bool][AeroCaptureNative]::IsIconic($hwnd)
        $inputAfterMinimize = Get-AeroInputState

        $c3File = Join-Path $OutDir 'c3.png'
        $c3Capture = Invoke-AeroWindowCapture -Hwnd $hwnd -OutFile $c3File -ExpectedBackgroundArgb $ready.background
        $stillIconicAfterCapture = [bool][AeroCaptureNative]::IsIconic($hwnd)
        $minimizedRestored = $isIconicNow -and $c3Capture.WasIconic -and (-not $stillIconicAfterCapture)

        $c1Bitmap = New-Object System.Drawing.Bitmap($c1File)
        $c2BitmapAgain = New-Object System.Drawing.Bitmap($c2File)
        $c3Bitmap = New-Object System.Drawing.Bitmap($c3File)
        $diffNormalVsCovered = [AeroPixels]::CountDiff($c1Bitmap, $c2BitmapAgain)
        $diffNormalVsMinimized = [AeroPixels]::CountDiff($c1Bitmap, $c3Bitmap)
        $c1Bitmap.Dispose()
        $c2BitmapAgain.Dispose()
        $c3Bitmap.Dispose()

        $externalActivityEvents = 0
        if ($c1Capture.ExternalActivity) { $externalActivityEvents++ }
        if ($c2Capture.ExternalActivity) { $externalActivityEvents++ }
        if ($c3Capture.ExternalActivity) { $externalActivityEvents++ }

        $minimizeStateStable = ($inputBeforeMinimize.CursorX -eq $inputAfterMinimize.CursorX) -and
            ($inputBeforeMinimize.CursorY -eq $inputAfterMinimize.CursorY) -and
            ($inputBeforeMinimize.ForegroundHwnd -eq $inputAfterMinimize.ForegroundHwnd)

        $inputUnchanged = ($externalActivityEvents -eq 0) -and $minimizeStateStable

        $selftestResult = [ordered]@{
            occludedVerified       = [bool]$occludedVerified
            magentaAbsent          = [bool]$magentaAbsent
            minimizedRestored      = [bool]$minimizedRestored
            diffNormalVsCovered    = $diffNormalVsCovered
            diffNormalVsMinimized  = $diffNormalVsMinimized
            inputUnchanged         = [bool]$inputUnchanged
            externalActivityEvents = $externalActivityEvents
            dpi                    = $selftestDpi
        }
        $selftestResult | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutDir 'selftest.json') -Encoding UTF8

        if ($selftestResult.occludedVerified -and $selftestResult.magentaAbsent -and $selftestResult.minimizedRestored -and $selftestResult.inputUnchanged) {
            Write-Output 'AERO_SELFTEST PASS'
        }
        else {
            $reasons = @()
            if (-not $selftestResult.occludedVerified) { $reasons += 'occludedVerified=false' }
            if (-not $selftestResult.magentaAbsent) { $reasons += 'magentaAbsent=false' }
            if (-not $selftestResult.minimizedRestored) { $reasons += 'minimizedRestored=false' }
            if (-not $selftestResult.inputUnchanged) { $reasons += 'inputUnchanged=false' }
            Write-Output "AERO_SELFTEST FAIL $($reasons -join ',')"
        }
    }
    finally {
        Stop-ShowcaseLaunch -Hwnd $hwnd -GradleProcess $launch.Process
        Remove-AeroOrphanShowcaseProcesses | Out-Null
    }
    return
}

# --- Full sweep ---
$startedAt = (Get-Date).ToString('o')
$frames = New-Object System.Collections.Generic.List[object]

foreach ($theme in $Themes) {
    foreach ($section in $Sections) {
        $pagesToCapture = @(0)
        $discoveredPages = $false

        for ($i = 0; $i -lt $pagesToCapture.Count; $i++) {
            $page = $pagesToCapture[$i]
            $result = Invoke-AeroCaptureLaunchCycle -RepoRoot $RepoRoot -Theme $theme -Section $section -Page $page `
                -LogDir $logDir -ReadyTimeoutSec $ReadyTimeoutSec -SettleMs $SettleMs -CaptureGapMs $CaptureGapMs `
                -Captures $Captures

            if (-not $discoveredPages) {
                $totalPages = [int]$result.Ready.pages
                if ($Pages -and $Pages.Count -gt 0) {
                    $filtered = @($Pages | Where-Object { $_ -ge 0 -and $_ -lt $totalPages } | Sort-Object -Unique)
                    $pagesToCapture = if ($filtered.Count -gt 0) { $filtered } else { @(0) }
                }
                else {
                    $pagesToCapture = @(0..($totalPages - 1))
                }
                $discoveredPages = $true
                if ($pagesToCapture[0] -ne 0) {
                    # Page 0 was captured for discovery; make sure the loop still visits every
                    # requested page even if 0 is not among them.
                    $i = -1
                }
            }

            [void]$frames.Add([pscustomobject]@{
                key              = "$theme/$section/p$page"
                files            = $result.Files
                scheme           = $result.Ready.scheme
                section          = $result.Ready.section
                page             = $result.Ready.page
                pages            = $result.Ready.pages
                viewportPx       = $result.Ready.viewportPx
                contentPx        = $result.Ready.contentPx
                scrollPx         = $result.Ready.scrollPx
                background       = $result.Ready.background
                jvm              = $result.Ready.jvm
                width            = $result.Capture.Width
                height           = $result.Capture.Height
                dpi              = $result.Capture.Dpi
                externalActivity = $result.Capture.ExternalActivity
            })
        }
    }
}

$orphansKilled = @(Remove-AeroOrphanShowcaseProcesses)

$toolchain = Get-AeroToolchainInfo -RepoRoot $RepoRoot
if ($frames.Count -gt 0) {
    $toolchain['jvm'] = $frames[0].jvm
}

$manifest = [ordered]@{
    toolchain     = $toolchain
    osVersion     = [Environment]::OSVersion.VersionString
    commit        = (& git -C $RepoRoot rev-parse HEAD 2>$null | Select-Object -First 1)
    startedAt     = $startedAt
    finishedAt    = (Get-Date).ToString('o')
    frames        = $frames
    orphansKilled = $orphansKilled
}
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $OutDir 'manifest.json') -Encoding UTF8

Write-Output "AERO_SWEEP_DONE frames=$($frames.Count) out=$OutDir"
