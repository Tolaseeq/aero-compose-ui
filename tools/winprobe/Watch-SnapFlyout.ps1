<#
.SYNOPSIS
    UI Automation Snap Layouts flyout watcher for the VER-12 real-input sessions
    (aero-compose-ui v3.2.0, D-04).
.DESCRIPTION
    Dot-sourceable. Get-AeroShellSnapshot takes a baseline of visible top-level HWNDs
    (class/process/rect -- never titles) and of the desktop's own UI Automation root children
    (class/AutomationId/ControlType/process/rect; Name is kept ONLY for shell-process elements).
    Wait-AeroSnapFlyout polls for NEW shell-owned windows/elements that appear after the baseline
    (the Snap Layouts flyout), records the delay, and for each new UIA element collects up to 40
    descendant elements so a caller can find a layout zone to click.
    Test-AeroFlyoutSignatureMatch compares two Wait-AeroSnapFlyout results by class/process/
    AutomationId signature. Get-AeroSnapSettings reads (never writes) the two registry values
    that gate Snap Layouts and Windows snapping. Also invocable directly with -SelfTest, which
    takes one snapshot, prints counts only, and asserts no non-shell element carries a Name.
.NOTES
    PowerShell 5.1 only. Never reads a window's title text (that Win32 entry point is deliberately
    absent from this file's own P/Invoke surface) and never writes a registry value.
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$SelfTest
)

Set-StrictMode -Version 2

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

if (-not ('AeroSnapWatch.Native' -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

namespace AeroSnapWatch
{
    [StructLayout(LayoutKind.Sequential)]
    public struct SRect
    {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    public class Native
    {
        [DllImport("user32.dll")]
        public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

        [DllImport("user32.dll")]
        public static extern bool IsWindowVisible(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool GetWindowRect(IntPtr hWnd, out SRect lpRect);

        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        public static extern int GetClassNameW(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

        public static string GetClassName(IntPtr hWnd)
        {
            StringBuilder sb = new StringBuilder(256);
            GetClassNameW(hWnd, sb, sb.Capacity);
            return sb.ToString();
        }

        public static List<IntPtr> EnumTopLevelVisible()
        {
            List<IntPtr> found = new List<IntPtr>();
            EnumWindowsProc callback = delegate(IntPtr hWnd, IntPtr lParam)
            {
                if (IsWindowVisible(hWnd))
                {
                    found.Add(hWnd);
                }
                return true;
            };
            EnumWindows(callback, IntPtr.Zero);
            return found;
        }
    }
}
"@
}

# Only these owning processes may keep a UIA element's Name -- the shell surfaces the Snap
# Layouts flyout, Start, search, and Snap-adjacent chrome through one of them. Everything else
# (including the target app itself) is a foreign window and its Name is never read into memory.
$AeroShellProcessNames = @('explorer', 'ShellExperienceHost', 'ShellHost', 'StartMenuExperienceHost', 'SearchHost')

function Get-AeroProcessNameById {
    param([Parameter(Mandatory = $true)][uint32]$ProcessId)
    try { return (Get-Process -Id $ProcessId -ErrorAction Stop).ProcessName } catch { return $null }
}

function Get-AeroShellSnapshot {
    <#
    .SYNOPSIS
        (a) every visible top-level HWND: class name, owning process NAME, rect -- titles are
        never read. (b) every UI Automation root-element child: ClassName, AutomationId,
        ControlType, process name, BoundingRectangle, and the live AutomationElement (for later
        descendant walks); Name is kept only when the owning process is in $AeroShellProcessNames,
        blanked otherwise.
    #>
    [CmdletBinding()]
    param()

    $windows = New-Object System.Collections.Generic.List[object]
    foreach ($hwnd in [AeroSnapWatch.Native]::EnumTopLevelVisible()) {
        [uint32]$ownerPid = 0
        [AeroSnapWatch.Native]::GetWindowThreadProcessId($hwnd, [ref]$ownerPid) | Out-Null
        $rect = New-Object AeroSnapWatch.SRect
        [AeroSnapWatch.Native]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
        [void]$windows.Add([pscustomobject]@{
            Hwnd        = $hwnd
            ClassName   = [AeroSnapWatch.Native]::GetClassName($hwnd)
            ProcessName = Get-AeroProcessNameById -ProcessId $ownerPid
            Rect        = $rect
        })
    }

    $uiaElements = New-Object System.Collections.Generic.List[object]
    $root = [System.Windows.Automation.AutomationElement]::RootElement
    $children = $root.FindAll([System.Windows.Automation.TreeScope]::Children, [System.Windows.Automation.Condition]::TrueCondition)
    foreach ($child in $children) {
        $cur = $child.Current
        [uint32]$elemPid = 0
        try { $elemPid = [uint32]$cur.ProcessId } catch { $elemPid = 0 }
        $procName = Get-AeroProcessNameById -ProcessId $elemPid
        $isShell = $AeroShellProcessNames -contains $procName
        [void]$uiaElements.Add([pscustomobject]@{
            ClassName         = $cur.ClassName
            AutomationId      = $cur.AutomationId
            ControlType       = $cur.ControlType.ProgrammaticName
            ProcessName       = $procName
            BoundingRectangle = $cur.BoundingRectangle
            Name              = $(if ($isShell) { $cur.Name } else { '' })
            Element           = $child
        })
    }

    [pscustomobject]@{
        CapturedAt  = Get-Date
        Windows     = $windows
        UiaElements = $uiaElements
    }
}

function Test-AeroRectIntersects {
    param([Parameter(Mandatory = $true)]$A, $B)
    if (-not $B) { return $true }
    return -not (($A.Right -le $B.Left) -or ($A.Left -ge $B.Right) -or ($A.Bottom -le $B.Top) -or ($A.Top -ge $B.Bottom))
}

function Get-AeroDescendantsForElement {
    <#
    .SYNOPSIS
        Up to 40 descendant elements of a live AutomationElement: ControlType, AutomationId,
        BoundingRectangle, and shell-only Name.
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)]$Element)
    $descendants = New-Object System.Collections.Generic.List[object]
    try {
        $found = $Element.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
        $limit = [Math]::Min(40, $found.Count)
        for ($i = 0; $i -lt $limit; $i++) {
            $dCur = $found.Item($i).Current
            [uint32]$dPid = 0
            try { $dPid = [uint32]$dCur.ProcessId } catch { $dPid = 0 }
            $dProcName = Get-AeroProcessNameById -ProcessId $dPid
            $dIsShell = $AeroShellProcessNames -contains $dProcName
            [void]$descendants.Add([pscustomobject]@{
                ControlType       = $dCur.ControlType.ProgrammaticName
                AutomationId      = $dCur.AutomationId
                BoundingRectangle = $dCur.BoundingRectangle
                Name              = $(if ($dIsShell) { $dCur.Name } else { '' })
            })
        }
    }
    catch {
        # A flyout element can dispose mid-walk (it's transient by nature); an empty descendant
        # list is a valid, non-fatal result here.
    }
    return $descendants
}

function Wait-AeroSnapFlyout {
    <#
    .SYNOPSIS
        Polls Get-AeroShellSnapshot until a NEW shell-owned window or UIA element appears (not in
        -Baseline), optionally restricted to elements whose rect intersects -MonitorRect, or
        -TimeoutMs elapses. Returns Found, DelayMs, NewWindows, NewUiaElements (each UIA element
        carries a Descendants property with up to 40 descendant elements).
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Baseline,
        [int]$TimeoutMs = 3000,
        [int]$PollMs = 100,
        $MonitorRect
    )
    $deadline = (Get-Date).AddMilliseconds($TimeoutMs)
    $startedAt = Get-Date

    $baselineWindowKeys = New-Object System.Collections.Generic.HashSet[string]
    foreach ($w in $Baseline.Windows) {
        [void]$baselineWindowKeys.Add("$($w.ClassName)|$($w.ProcessName)|$($w.Hwnd)")
    }
    $baselineUiaKeys = New-Object System.Collections.Generic.HashSet[string]
    foreach ($e in $Baseline.UiaElements) {
        [void]$baselineUiaKeys.Add("$($e.ClassName)|$($e.ProcessName)|$($e.AutomationId)")
    }

    while ((Get-Date) -lt $deadline) {
        $snap = Get-AeroShellSnapshot

        $newWindows = @($snap.Windows | Where-Object {
            $key = "$($_.ClassName)|$($_.ProcessName)|$($_.Hwnd)"
            (-not $baselineWindowKeys.Contains($key)) -and
            ($AeroShellProcessNames -contains $_.ProcessName) -and
            (Test-AeroRectIntersects -A $_.Rect -B $MonitorRect)
        })
        $newUia = @($snap.UiaElements | Where-Object {
            $key = "$($_.ClassName)|$($_.ProcessName)|$($_.AutomationId)"
            (-not $baselineUiaKeys.Contains($key)) -and
            ($AeroShellProcessNames -contains $_.ProcessName) -and
            (Test-AeroRectIntersects -A $_.BoundingRectangle -B $MonitorRect)
        })

        if ($newWindows.Count -gt 0 -or $newUia.Count -gt 0) {
            $delayMs = [int](((Get-Date) - $startedAt).TotalMilliseconds)
            foreach ($elem in $newUia) {
                $descendants = Get-AeroDescendantsForElement -Element $elem.Element
                $elem | Add-Member -NotePropertyName Descendants -NotePropertyValue $descendants -Force
            }
            return [pscustomobject]@{
                Found          = $true
                DelayMs        = $delayMs
                NewWindows     = $newWindows
                NewUiaElements = $newUia
            }
        }
        Start-Sleep -Milliseconds $PollMs
    }

    return [pscustomobject]@{
        Found          = $false
        DelayMs        = $TimeoutMs
        NewWindows     = @()
        NewUiaElements = @()
    }
}

function Test-AeroFlyoutSignatureMatch {
    <#
    .SYNOPSIS
        Compares two Wait-AeroSnapFlyout results by class/process[/AutomationId] signature and
        reports whether any signature in -Observed also appears in -Reference.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Observed,
        [Parameter(Mandatory = $true)]$Reference
    )
    function Get-AeroFlyoutSignatureSet($result) {
        $set = New-Object System.Collections.Generic.HashSet[string]
        foreach ($w in $result.NewWindows) { [void]$set.Add("W|$($w.ClassName)|$($w.ProcessName)") }
        foreach ($e in $result.NewUiaElements) { [void]$set.Add("U|$($e.ClassName)|$($e.ProcessName)|$($e.AutomationId)") }
        return $set
    }
    $observedSet = Get-AeroFlyoutSignatureSet $Observed
    $referenceSet = Get-AeroFlyoutSignatureSet $Reference
    $intersection = New-Object 'System.Collections.Generic.HashSet[string]' -ArgumentList (, $referenceSet)
    $intersection.IntersectWith($observedSet)
    [pscustomobject]@{
        IsMatch              = $intersection.Count -gt 0
        MatchedSignatures    = @($intersection)
        ObservedSignatures   = @($observedSet)
        ReferenceSignatures  = @($referenceSet)
    }
}

function Get-AeroSnapSettings {
    <#
    .SYNOPSIS
        Reads (never writes) EnableSnapAssistFlyout and WindowArrangementActive. Both keys default
        to "on" when absent, matching this codebase's own documented Windows default.
    #>
    [CmdletBinding()]
    param()

    $flyoutPath = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced'
    $flyoutRaw = $null
    try { $flyoutRaw = (Get-ItemProperty -Path $flyoutPath -Name 'EnableSnapAssistFlyout' -ErrorAction Stop).EnableSnapAssistFlyout } catch { $flyoutRaw = $null }
    $snapAssistFlyoutOn = if ($null -eq $flyoutRaw) { $true } else { [bool]$flyoutRaw }

    $desktopPath = 'HKCU:\Control Panel\Desktop'
    $arrangementRaw = $null
    try { $arrangementRaw = (Get-ItemProperty -Path $desktopPath -Name 'WindowArrangementActive' -ErrorAction Stop).WindowArrangementActive } catch { $arrangementRaw = $null }
    $windowArrangementActive = if ($null -eq $arrangementRaw) { $true } else { ("$arrangementRaw" -eq '1') }

    [pscustomobject]@{
        SnapAssistFlyoutOn      = $snapAssistFlyoutOn
        WindowArrangementActive = $windowArrangementActive
    }
}

function Test-AeroWatchSnapFlyoutSelf {
    <#
    .SYNOPSIS
        Takes one snapshot, prints counts only, and asserts no non-shell element carries a Name.
    .NOTES
        Uses Write-Host for its diagnostic lines, not Write-Output -- see RealInput.ps1's
        Test-AeroRealInputSelf for why (a captured `$ok = ...` assignment would otherwise swallow
        every diagnostic line into the return value instead of the console).
    #>
    [CmdletBinding()]
    param()
    $snap = Get-AeroShellSnapshot
    Write-Host "WATCHSNAPFLYOUT_SELFTEST windows=$($snap.Windows.Count) uiaElements=$($snap.UiaElements.Count)"

    $leak = @($snap.UiaElements | Where-Object { ($AeroShellProcessNames -notcontains $_.ProcessName) -and $_.Name -and ($_.Name -ne '') })
    if ($leak.Count -gt 0) {
        Write-Host "WATCHSNAPFLYOUT_SELFTEST FAIL: $($leak.Count) non-shell element(s) carry a Name"
        return $false
    }
    Write-Host 'WATCHSNAPFLYOUT_SELFTEST PASS'
    return $true
}

if ($SelfTest) {
    $ok = Test-AeroWatchSnapFlyoutSelf
    if (-not $ok) { exit 1 }
    exit 0
}
