<#
.SYNOPSIS
    Starts and locates the VER-12 early gate's positive-control window (aero-compose-ui v3.2.0,
    D-04) -- a real standard Win32 window (a WinForms Form) hosted in its own powershell.exe -STA
    child process, used as the reference for the Snap Layouts flyout.
.DESCRIPTION
    Dot-sourceable. Start-AeroPositiveControlWindow launches PositiveControlHost.ps1 with an
    exact, unique title (AeroGate PositiveControl <guid>) so the caller can find it with
    Find-AeroShowcaseWindow (reused from AeroCapture.ps1, exact-title match, no locale
    dependency). -NoActivate starts it without taking the foreground.
.NOTES
    PowerShell 5.1 only. Closes the window by PID only (Stop-AeroPositiveControlWindow /
    Stop-AeroProcessOfWindow), never by window message.
#>

#Requires -Version 5.1
Set-StrictMode -Version 2

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here '..\capture\AeroCapture.ps1')

function Start-AeroPositiveControlWindow {
    <#
    .SYNOPSIS
        Launches the positive-control host process and returns {Process, Title}. The caller finds
        the HWND with Find-AeroShowcaseWindow -Title <returned Title>.
    #>
    [CmdletBinding()]
    param(
        [switch]$NoActivate,
        [string]$RepoRoot = 'C:\1A_WORK\ui_lib'
    )
    $title = "AeroGate PositiveControl $([Guid]::NewGuid().ToString())"
    $hostScript = Join-Path $here 'PositiveControlHost.ps1'

    # Start-Process -WindowStyle Hidden sets STARTUPINFO.wShowWindow = SW_HIDE, which Win32
    # applies to a launched GUI process's *first* ShowWindow call regardless of what that call
    # actually requests (documented ShowWindow behavior) -- it would hide the Form itself, not
    # just the console host. System.Diagnostics.Process with CreateNoWindow=true instead
    # suppresses console allocation only (CREATE_NO_WINDOW), leaving the Form's own visibility
    # exactly as PositiveControlHost.ps1 sets it.
    $argumentLine = '-NoProfile -STA -ExecutionPolicy Bypass -File "{0}" -Title "{1}"' -f $hostScript, $title
    if ($NoActivate) { $argumentLine += ' -NoActivate' }

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'powershell.exe'
    $psi.Arguments = $argumentLine
    $psi.WorkingDirectory = $RepoRoot
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $process = [System.Diagnostics.Process]::Start($psi)

    [pscustomobject]@{
        Process = $process
        Title   = $title
    }
}

function Stop-AeroPositiveControlWindow {
    <#
    .SYNOPSIS
        Stops the positive-control process by PID (never by window message).
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)]$Info)
    if ($Info -and $Info.Process -and -not $Info.Process.HasExited) {
        try { Stop-Process -Id $Info.Process.Id -Force -ErrorAction SilentlyContinue } catch { }
    }
}
