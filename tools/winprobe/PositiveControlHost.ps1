<#
.SYNOPSIS
    Hosts one standard Win32 window (a WinForms Form) as its own process for the VER-12 early
    gate's positive control (aero-compose-ui v3.2.0, D-04).
.DESCRIPTION
    Launched as a separate powershell.exe -STA process by Start-AeroPositiveControlWindow
    (PositiveControlWindow.ps1). [System.Windows.Forms.Application]::Run gives the Form a real
    Win32 message loop in a process the caller can find by exact-title match and close by PID --
    its frame is drawn by DefWindowProc, so it answers WM_NCHITTEST HTMAXBUTTON (9) exactly like
    any other standard resizable window, cross-process, unlike Windows 11's own Notepad (a WinUI
    app with no HTMAXBUTTON span in its top row).
.NOTES
    PowerShell 5.1 only, run under -STA (required by WinForms). -NoActivate shows the window
    without taking the foreground (ShowWithoutActivation override + WS_EX_NOACTIVATE) and pins it
    to the bottom of the z-order with SWP_NOACTIVATE -- used whenever the caller must not steal
    focus from whoever is using this machine (a dry run, or a self-test).
#>

#Requires -Version 5.1
param(
    [Parameter(Mandatory = $true)][string]$Title,
    [switch]$NoActivate
)

Set-StrictMode -Version 2
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

if (-not ('AeroGate.PositiveControlForm' -as [type])) {
    Add-Type -ReferencedAssemblies System.Windows.Forms -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace AeroGate
{
    public class PositiveControlForm : Form
    {
        public bool NoActivateFlag = false;

        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams cp = base.CreateParams;
                if (NoActivateFlag)
                {
                    cp.ExStyle |= 0x08000000; // WS_EX_NOACTIVATE
                }
                return cp;
            }
        }

        protected override bool ShowWithoutActivation
        {
            get { return NoActivateFlag; }
        }
    }

    public class PositiveControlNative
    {
        public static readonly IntPtr HWND_BOTTOM = (IntPtr)1;
        public const uint SWP_NOSIZE = 0x1;
        public const uint SWP_NOMOVE = 0x2;
        public const uint SWP_NOACTIVATE = 0x10;

        [DllImport("user32.dll")]
        public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);
    }
}
"@
}

$form = New-Object AeroGate.PositiveControlForm
$form.NoActivateFlag = [bool]$NoActivate
$form.Text = $Title
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::Sizable
$form.MaximizeBox = $true
$form.MinimizeBox = $true
$form.StartPosition = [System.Windows.Forms.FormStartPosition]::Manual
$form.Location = New-Object System.Drawing.Point(200, 200)
$form.Size = New-Object System.Drawing.Size(800, 500)

if ($NoActivate) {
    $form.Add_Shown({
        [AeroGate.PositiveControlNative]::SetWindowPos(
            $form.Handle, [AeroGate.PositiveControlNative]::HWND_BOTTOM, 0, 0, 0, 0,
            [AeroGate.PositiveControlNative]::SWP_NOSIZE -bor [AeroGate.PositiveControlNative]::SWP_NOMOVE -bor [AeroGate.PositiveControlNative]::SWP_NOACTIVATE
        ) | Out-Null
    })
}

[System.Windows.Forms.Application]::Run($form)
