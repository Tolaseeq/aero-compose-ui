<#
.SYNOPSIS
    Snap Layouts flyout watcher for the VER-12 real-input sessions (aero-compose-ui v3.2.0, D-04).
.DESCRIPTION
    Dot-sourceable. Combines three independent signals so a real appearance of the flyout is hard
    to miss: (1) an EVENT_OBJECT_SHOW/EVENT_OBJECT_UNCLOAKED WinEvent hook on shell processes
    (Start-AeroShellEventWatch/Stop-AeroShellEventWatch), (2) a cloak-aware baseline/diff of
    top-level shell windows that counts a pre-created window becoming visible or uncloaked as an
    appearance, not only a brand-new HWND, and (3) a baseline/diff of the desktop's own UI
    Automation root children. Get-AeroShellSnapshot takes the baseline for (2) and (3) (class/
    process/rect/visible/cloaked for windows; class/AutomationId/ControlType/process/rect for UIA
    elements -- Name is kept only for shell-process elements). Wait-AeroSnapFlyout polls all three
    signals for the hover window and returns which one fired, the delay, and the evidence.
    Test-AeroFlyoutSignatureMatch compares two Wait-AeroSnapFlyout results by class/process/
    AutomationId signature. Get-AeroSnapSettings reads (never writes) the two registry values
    that gate Snap Layouts and Windows snapping. Also invocable directly with -SelfTest, which
    proves the WinEvent hook itself works (no real input) by watching a freshly-launched,
    non-activating positive-control window and asserting the hook saw its own SHOW/CREATE event.
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

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'PositiveControlWindow.ps1')

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

if (-not ('AeroSnapWatch.Native' -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;

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

        [DllImport("dwmapi.dll")]
        public static extern int DwmGetWindowAttribute(IntPtr hwnd, int dwAttribute, out int pvAttribute, int cbAttribute);

        public const int DWMWA_CLOAKED = 14;

        public static string GetClassName(IntPtr hWnd)
        {
            StringBuilder sb = new StringBuilder(256);
            GetClassNameW(hWnd, sb, sb.Capacity);
            return sb.ToString();
        }

        public static bool IsCloaked(IntPtr hWnd)
        {
            int cloaked;
            int hr = DwmGetWindowAttribute(hWnd, DWMWA_CLOAKED, out cloaked, 4);
            if (hr != 0) { return false; }
            return cloaked != 0;
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

        // Includes hidden and cloaked windows -- a flyout host can be pre-created by the shell
        // and merely uncloaked/shown on hover, so a visibility-only enumeration would miss it.
        public static List<IntPtr> EnumTopLevelAll()
        {
            List<IntPtr> found = new List<IntPtr>();
            EnumWindowsProc callback = delegate(IntPtr hWnd, IntPtr lParam)
            {
                found.Add(hWnd);
                return true;
            };
            EnumWindows(callback, IntPtr.Zero);
            return found;
        }
    }

    // One WinEvent observation -- never carries a window title (GetWindowText is deliberately
    // absent from this file's P/Invoke surface).
    public class AeroEventRecord
    {
        public double TimestampMs;
        public string EventName;
        public IntPtr Hwnd;
        public int IdObject;
        public int IdChild;
        public string ProcessName;
        public string ClassName;
        public SRect Rect;
        public bool Cloaked;
        public bool Visible;
    }

    public delegate void WinEventDelegate(IntPtr hWinEventHook, uint eventType, IntPtr hwnd, int idObject, int idChild, uint idEventThread, uint dwmsEventTime);

    // A background thread with its own GetMessage loop hosts SetWinEventHook (OUT_OF_CONTEXT
    // hooks deliver their callback only while the registering thread pumps messages). Two hooks
    // cover the disjoint event-id ranges this watcher cares about; EVENT_OBJECT_LOCATIONCHANGE
    // and every other id inside the EVENT_OBJECT_* range is filtered out in OnEvent as too noisy.
    public class EventWatcher
    {
        public const uint WINEVENT_OUTOFCONTEXT = 0x0000;
        public const uint WINEVENT_SKIPOWNPROCESS = 0x0002;
        public const uint WM_QUIT = 0x0012;

        public const uint EVENT_OBJECT_CREATE = 0x8000;
        public const uint EVENT_OBJECT_DESTROY = 0x8001;
        public const uint EVENT_OBJECT_SHOW = 0x8002;
        public const uint EVENT_OBJECT_HIDE = 0x8003;
        public const uint EVENT_OBJECT_CLOAKED = 0x8017;
        public const uint EVENT_OBJECT_UNCLOAKED = 0x8018;
        public const uint EVENT_SYSTEM_FOREGROUND = 0x0003;

        [StructLayout(LayoutKind.Sequential)]
        private struct MSG
        {
            public IntPtr hwnd;
            public uint message;
            public IntPtr wParam;
            public IntPtr lParam;
            public uint time;
            public int ptX;
            public int ptY;
        }

        [DllImport("user32.dll")]
        private static extern IntPtr SetWinEventHook(uint eventMin, uint eventMax, IntPtr hmodWinEventProc, WinEventDelegate lpfnWinEventProc, uint idProcess, uint idThread, uint dwFlags);

        [DllImport("user32.dll")]
        private static extern bool UnhookWinEvent(IntPtr hWinEventHook);

        [DllImport("user32.dll")]
        private static extern bool GetMessage(out MSG lpMsg, IntPtr hWnd, uint wMsgFilterMin, uint wMsgFilterMax);

        [DllImport("user32.dll")]
        private static extern bool PostThreadMessage(uint idThread, uint msg, IntPtr wParam, IntPtr lParam);

        [DllImport("kernel32.dll")]
        private static extern uint GetCurrentThreadId();

        private Thread _thread;
        private uint _threadId;
        private IntPtr _hookObj = IntPtr.Zero;
        private IntPtr _hookFg = IntPtr.Zero;
        private WinEventDelegate _callback;
        private readonly object _eventsLock = new object();
        private readonly List<AeroEventRecord> _events = new List<AeroEventRecord>();
        private readonly object _idsLock = new object();
        private HashSet<uint> _watchedProcessIds = new HashSet<uint>();
        private List<string> _watchedProcessNames = new List<string>();
        private DateTime _startedAt;
        private readonly ManualResetEvent _ready = new ManualResetEvent(false);

        public void Start(List<string> watchedProcessNames, List<uint> watchedProcessIds)
        {
            _watchedProcessNames = watchedProcessNames ?? new List<string>();
            lock (_idsLock)
            {
                _watchedProcessIds = new HashSet<uint>(watchedProcessIds ?? new List<uint>());
            }
            _startedAt = DateTime.UtcNow;
            _callback = new WinEventDelegate(OnEvent);
            _thread = new Thread(new ThreadStart(ThreadProc));
            _thread.IsBackground = true;
            _thread.SetApartmentState(ApartmentState.STA);
            _thread.Start();
            _ready.WaitOne(5000);
        }

        public void AddWatchedProcessId(uint pid)
        {
            lock (_idsLock)
            {
                _watchedProcessIds.Add(pid);
            }
        }

        private void ThreadProc()
        {
            _threadId = GetCurrentThreadId();
            _hookObj = SetWinEventHook(EVENT_OBJECT_CREATE, EVENT_OBJECT_UNCLOAKED, IntPtr.Zero, _callback, 0, 0, WINEVENT_OUTOFCONTEXT | WINEVENT_SKIPOWNPROCESS);
            _hookFg = SetWinEventHook(EVENT_SYSTEM_FOREGROUND, EVENT_SYSTEM_FOREGROUND, IntPtr.Zero, _callback, 0, 0, WINEVENT_OUTOFCONTEXT | WINEVENT_SKIPOWNPROCESS);
            _ready.Set();

            MSG msg;
            while (GetMessage(out msg, IntPtr.Zero, 0, 0))
            {
                // No window is ever created on this thread; the hook delivers its callback
                // directly while GetMessage pumps, without an explicit dispatched message.
            }

            if (_hookObj != IntPtr.Zero) { UnhookWinEvent(_hookObj); _hookObj = IntPtr.Zero; }
            if (_hookFg != IntPtr.Zero) { UnhookWinEvent(_hookFg); _hookFg = IntPtr.Zero; }
        }

        private static string EventName(uint eventType)
        {
            if (eventType == EVENT_OBJECT_CREATE) { return "CREATE"; }
            if (eventType == EVENT_OBJECT_DESTROY) { return "DESTROY"; }
            if (eventType == EVENT_OBJECT_SHOW) { return "SHOW"; }
            if (eventType == EVENT_OBJECT_HIDE) { return "HIDE"; }
            if (eventType == EVENT_OBJECT_CLOAKED) { return "CLOAKED"; }
            if (eventType == EVENT_OBJECT_UNCLOAKED) { return "UNCLOAKED"; }
            if (eventType == EVENT_SYSTEM_FOREGROUND) { return "FOREGROUND"; }
            return "OTHER";
        }

        private static string GetProcessNameSafe(uint pid)
        {
            try
            {
                using (Process p = Process.GetProcessById((int)pid))
                {
                    return p.ProcessName;
                }
            }
            catch
            {
                return null;
            }
        }

        private void OnEvent(IntPtr hWinEventHook, uint eventType, IntPtr hwnd, int idObject, int idChild, uint idEventThread, uint dwmsEventTime)
        {
            if (eventType != EVENT_OBJECT_CREATE && eventType != EVENT_OBJECT_DESTROY &&
                eventType != EVENT_OBJECT_SHOW && eventType != EVENT_OBJECT_HIDE &&
                eventType != EVENT_OBJECT_CLOAKED && eventType != EVENT_OBJECT_UNCLOAKED &&
                eventType != EVENT_SYSTEM_FOREGROUND)
            {
                return;
            }
            if (hwnd == IntPtr.Zero) { return; }

            uint ownerPid = 0;
            Native.GetWindowThreadProcessId(hwnd, out ownerPid);
            string procName = GetProcessNameSafe(ownerPid);

            bool isShell = procName != null && _watchedProcessNames.Contains(procName);
            bool isOwn;
            lock (_idsLock)
            {
                isOwn = _watchedProcessIds.Contains(ownerPid);
            }
            if (!isShell && !isOwn) { return; }

            SRect rect = new SRect();
            Native.GetWindowRect(hwnd, out rect);

            AeroEventRecord rec = new AeroEventRecord();
            rec.TimestampMs = (DateTime.UtcNow - _startedAt).TotalMilliseconds;
            rec.EventName = EventName(eventType);
            rec.Hwnd = hwnd;
            rec.IdObject = idObject;
            rec.IdChild = idChild;
            rec.ProcessName = procName;
            rec.ClassName = Native.GetClassName(hwnd);
            rec.Rect = rect;
            rec.Cloaked = Native.IsCloaked(hwnd);
            rec.Visible = Native.IsWindowVisible(hwnd);

            lock (_eventsLock)
            {
                _events.Add(rec);
            }
        }

        public List<AeroEventRecord> GetEventsSnapshot()
        {
            lock (_eventsLock)
            {
                return new List<AeroEventRecord>(_events);
            }
        }

        public List<AeroEventRecord> Stop()
        {
            if (_threadId != 0)
            {
                PostThreadMessage(_threadId, WM_QUIT, IntPtr.Zero, IntPtr.Zero);
            }
            if (_thread != null)
            {
                _thread.Join(5000);
            }
            return GetEventsSnapshot();
        }
    }
}
"@
}

# Only these owning processes may keep a UIA element's Name -- the shell surfaces the Snap
# Layouts flyout, Start, search, and Snap-adjacent chrome through one of them. Everything else
# (including the target app itself) is a foreign window and its Name is never read into memory.
$AeroShellProcessNames = @('explorer', 'ShellExperienceHost', 'ShellHost', 'StartMenuExperienceHost', 'SearchHost')

$Script:AeroEventWatcher = $null

function Start-AeroShellEventWatch {
    <#
    .SYNOPSIS
        Starts the background WinEvent hook, recording SHOW/HIDE/CREATE/DESTROY/CLOAKED/
        UNCLOAKED/FOREGROUND events for the shell processes plus -WatchedProcessIds (this run's
        own showcase and positive-control processes). One watch at a time.
    #>
    [CmdletBinding()]
    param(
        [string[]]$WatchedProcessNames = $AeroShellProcessNames,
        [uint32[]]$WatchedProcessIds = @()
    )
    if ($Script:AeroEventWatcher) {
        throw 'Start-AeroShellEventWatch: a watch is already running -- call Stop-AeroShellEventWatch first'
    }
    $namesList = New-Object 'System.Collections.Generic.List[string]'
    foreach ($n in $WatchedProcessNames) { [void]$namesList.Add($n) }
    $idsList = New-Object 'System.Collections.Generic.List[uint32]'
    foreach ($i in $WatchedProcessIds) { [void]$idsList.Add([uint32]$i) }

    $watcher = New-Object AeroSnapWatch.EventWatcher
    $watcher.Start($namesList, $idsList)
    $Script:AeroEventWatcher = $watcher
}

function Add-AeroShellEventWatchProcessId {
    <#
    .SYNOPSIS
        Adds one more process id to the running watch's own-process allow-list (e.g. the
        positive-control window's PID, known only after the watch has already started).
    #>
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][uint32]$ProcessId)
    if ($Script:AeroEventWatcher) {
        $Script:AeroEventWatcher.AddWatchedProcessId($ProcessId)
    }
}

function Get-AeroShellEventSnapshot {
    <#
    .SYNOPSIS
        A point-in-time copy of every event recorded by the running watch so far.
    #>
    [CmdletBinding()]
    param()
    if (-not $Script:AeroEventWatcher) { return @() }
    return $Script:AeroEventWatcher.GetEventsSnapshot()
}

function Stop-AeroShellEventWatch {
    <#
    .SYNOPSIS
        Stops the background thread and returns every event it recorded.
    #>
    [CmdletBinding()]
    param()
    if (-not $Script:AeroEventWatcher) { return @() }
    $events = $Script:AeroEventWatcher.Stop()
    $Script:AeroEventWatcher = $null
    return $events
}

function Get-AeroProcessNameById {
    param([Parameter(Mandatory = $true)][uint32]$ProcessId)
    try { return (Get-Process -Id $ProcessId -ErrorAction Stop).ProcessName } catch { return $null }
}

function Get-AeroProcessNameMap {
    <#
    .SYNOPSIS
        A pid -> ProcessName lookup built from one Get-Process call. Get-AeroShellSnapshot's raw
        top-level window count (~200+ on this machine, most invisible) made a per-window
        Get-Process -Id call the dominant cost (~4s/snapshot, measured live) -- far too slow for a
        poll loop; one batched Get-Process (~60ms) plus O(1) lookups fixes it.
    #>
    [CmdletBinding()]
    param()
    $map = @{}
    foreach ($p in (Get-Process)) { $map[[uint32]$p.Id] = $p.ProcessName }
    return $map
}

function Get-AeroShellSnapshot {
    <#
    .SYNOPSIS
        (a) every shell-process top-level HWND (any visible/cloaked state) plus every visible
        top-level HWND of any other process: class name, owning process NAME, rect, Visible,
        Cloaked -- titles are never read. (b) every UI Automation root-element child: ClassName,
        AutomationId, ControlType, process name, BoundingRectangle, and the live AutomationElement
        (for later descendant walks); Name is kept only when the owning process is in
        $AeroShellProcessNames, blanked otherwise.
    #>
    [CmdletBinding()]
    param()

    $procMap = Get-AeroProcessNameMap

    $windows = New-Object System.Collections.Generic.List[object]
    foreach ($hwnd in [AeroSnapWatch.Native]::EnumTopLevelAll()) {
        [uint32]$ownerPid = 0
        [AeroSnapWatch.Native]::GetWindowThreadProcessId($hwnd, [ref]$ownerPid) | Out-Null
        $procName = $procMap[$ownerPid]
        $isVisible = [bool][AeroSnapWatch.Native]::IsWindowVisible($hwnd)
        $isShell = $AeroShellProcessNames -contains $procName
        # A hidden/cloaked window is only worth keeping when it belongs to a shell process (the
        # flyout host may be pre-created and merely uncloaked/shown on hover); every other
        # process's top-level windows are kept only while actually visible, matching prior
        # behavior for the non-shell entries this file's other consumers rely on.
        if (-not $isShell -and -not $isVisible) { continue }
        $rect = New-Object AeroSnapWatch.SRect
        [AeroSnapWatch.Native]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
        [void]$windows.Add([pscustomobject]@{
            Hwnd        = $hwnd
            ClassName   = [AeroSnapWatch.Native]::GetClassName($hwnd)
            ProcessName = $procName
            Rect        = $rect
            Visible     = $isVisible
            Cloaked     = [bool][AeroSnapWatch.Native]::IsCloaked($hwnd)
        })
    }

    $uiaElements = New-Object System.Collections.Generic.List[object]
    $root = [System.Windows.Automation.AutomationElement]::RootElement
    $children = $root.FindAll([System.Windows.Automation.TreeScope]::Children, [System.Windows.Automation.Condition]::TrueCondition)
    foreach ($child in $children) {
        $cur = $child.Current
        [uint32]$elemPid = 0
        try { $elemPid = [uint32]$cur.ProcessId } catch { $elemPid = 0 }
        $procName = $procMap[$elemPid]
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
        Watches, for at least -MinHoverMs and up to -TimeoutMs, for any of: a shell-process SHOW/
        UNCLOAKED WinEvent (requires a watch already running via Start-AeroShellEventWatch), a
        shell top-level window newly visible or newly uncloaked versus -Baseline, or a new shell
        UIA element versus -Baseline. Returns Found, Signal (Event/Window/Uia), DelayMs (from the
        first hit, not inflated by the minimum hold), NewWindows, NewUiaElements (each UIA element
        carries a Descendants property), Events.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Baseline,
        [int]$TimeoutMs = 3000,
        [int]$MinHoverMs = 1500,
        [int]$PollMs = 100,
        $MonitorRect
    )
    $deadline = (Get-Date).AddMilliseconds($TimeoutMs)
    $startedAt = Get-Date

    $baselineWindowState = @{}
    foreach ($w in $Baseline.Windows) {
        $key = "$($w.ClassName)|$($w.ProcessName)|$($w.Hwnd)"
        $baselineWindowState[$key] = [pscustomobject]@{ Visible = $w.Visible; Cloaked = $w.Cloaked }
    }
    $baselineUiaKeys = New-Object System.Collections.Generic.HashSet[string]
    foreach ($e in $Baseline.UiaElements) {
        [void]$baselineUiaKeys.Add("$($e.ClassName)|$($e.ProcessName)|$($e.AutomationId)")
    }

    $firstHit = $null
    while ((Get-Date) -lt $deadline) {
        $elapsedMs = ((Get-Date) - $startedAt).TotalMilliseconds

        if (-not $firstHit) {
            $snap = Get-AeroShellSnapshot

            $newWindows = @($snap.Windows | Where-Object {
                $key = "$($_.ClassName)|$($_.ProcessName)|$($_.Hwnd)"
                $prior = $baselineWindowState[$key]
                $becameVisible = (-not $prior) -or ((-not $prior.Visible -or $prior.Cloaked) -and $_.Visible -and -not $_.Cloaked)
                $becameVisible -and ($AeroShellProcessNames -contains $_.ProcessName) -and (Test-AeroRectIntersects -A $_.Rect -B $MonitorRect)
            })
            $newUia = @($snap.UiaElements | Where-Object {
                $key = "$($_.ClassName)|$($_.ProcessName)|$($_.AutomationId)"
                (-not $baselineUiaKeys.Contains($key)) -and
                ($AeroShellProcessNames -contains $_.ProcessName) -and
                (Test-AeroRectIntersects -A $_.BoundingRectangle -B $MonitorRect)
            })
            $eventHits = @(Get-AeroShellEventSnapshot | Where-Object {
                ($_.EventName -eq 'SHOW' -or $_.EventName -eq 'UNCLOAKED') -and ($AeroShellProcessNames -contains $_.ProcessName)
            })

            if ($newWindows.Count -gt 0 -or $newUia.Count -gt 0 -or $eventHits.Count -gt 0) {
                foreach ($elem in $newUia) {
                    $descendants = Get-AeroDescendantsForElement -Element $elem.Element
                    $elem | Add-Member -NotePropertyName Descendants -NotePropertyValue $descendants -Force
                }
                $signal = if ($eventHits.Count -gt 0) { 'Event' } elseif ($newWindows.Count -gt 0) { 'Window' } else { 'Uia' }
                $firstHit = [pscustomobject]@{
                    DelayMs        = [int]$elapsedMs
                    Signal         = $signal
                    NewWindows     = $newWindows
                    NewUiaElements = $newUia
                    Events         = $eventHits
                }
            }
        }

        if ($firstHit -and $elapsedMs -ge $MinHoverMs) { break }
        Start-Sleep -Milliseconds $PollMs
    }

    if ($firstHit) {
        return [pscustomobject]@{
            Found          = $true
            DelayMs        = $firstHit.DelayMs
            Signal         = $firstHit.Signal
            NewWindows     = $firstHit.NewWindows
            NewUiaElements = $firstHit.NewUiaElements
            Events         = $firstHit.Events
        }
    }

    return [pscustomobject]@{
        Found          = $false
        DelayMs        = $TimeoutMs
        Signal          = $null
        NewWindows     = @()
        NewUiaElements = @()
        Events         = @()
    }
}

function Test-AeroFlyoutSignatureMatch {
    <#
    .SYNOPSIS
        Compares two Wait-AeroSnapFlyout results by class/process[/AutomationId] signature (plus
        an event-based class/process signature) and reports whether any signature in -Observed
        also appears in -Reference.
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
        foreach ($ev in $result.Events) { [void]$set.Add("E|$($ev.ClassName)|$($ev.ProcessName)") }
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
        (1) Takes one snapshot, prints counts, and asserts no non-shell/non-own element carries a
        Name. (2) Starts the WinEvent watch, launches a non-activating positive-control window
        (no real input, no foreground change), and asserts the hook itself recorded a SHOW or
        CREATE event for that window's own HWND -- proving the hook works without any real input.
        Asserts the cursor position is unchanged throughout.
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

    Initialize-AeroDpiAwareness
    $cursorBefore = Get-AeroInputState

    $ok = $true
    $pcInfo = $null
    $watchStarted = $false
    try {
        $pcInfo = Start-AeroPositiveControlWindow -NoActivate
        Start-AeroShellEventWatch -WatchedProcessIds @([uint32]$pcInfo.Process.Id) | Out-Null
        $watchStarted = $true

        $pcHwnd = [IntPtr]::Zero
        try {
            $pcHwnd = Find-AeroShowcaseWindow -Title $pcInfo.Title -TimeoutSec 10
        }
        catch {
            Write-Host "WATCHSNAPFLYOUT_SELFTEST FAIL: positive-control window never appeared: $($_.Exception.Message)"
            $ok = $false
        }

        if ($ok) {
            Start-Sleep -Milliseconds 500
            $events = Get-AeroShellEventSnapshot
            $hit = @($events | Where-Object { ($_.EventName -eq 'SHOW' -or $_.EventName -eq 'CREATE') -and $_.Hwnd -eq $pcHwnd })
            if ($hit.Count -eq 0) {
                Write-Host 'WATCHSNAPFLYOUT_SELFTEST FAIL: no SHOW/CREATE event recorded for the positive-control window'
                $ok = $false
            }
            else {
                Write-Host "WATCHSNAPFLYOUT_SELFTEST hook recorded $($hit[0].EventName) for hwnd=$($pcHwnd) at $([int]$hit[0].TimestampMs)ms"
            }
        }
    }
    finally {
        if ($watchStarted) { Stop-AeroShellEventWatch | Out-Null }
        if ($pcInfo) { Stop-AeroPositiveControlWindow -Info $pcInfo }
    }

    $cursorAfter = Get-AeroInputState
    if ($cursorBefore.CursorX -ne $cursorAfter.CursorX -or $cursorBefore.CursorY -ne $cursorAfter.CursorY) {
        Write-Host 'WATCHSNAPFLYOUT_SELFTEST FAIL: cursor position changed during the self-test'
        $ok = $false
    }

    if (-not $ok) { return $false }
    Write-Host 'WATCHSNAPFLYOUT_SELFTEST PASS'
    return $true
}

if ($SelfTest) {
    $ok = Test-AeroWatchSnapFlyoutSelf
    if (-not $ok) { exit 1 }
    exit 0
}
