<#
.SYNOPSIS
    Prints the current cursor position and foreground window handle as compact JSON.
.DESCRIPTION
    A standalone probe (HRM-03) meant to be run before and after an MCP call, so the two outputs
    can be diffed to prove the call never moved the real cursor or changed the foreground window.
#>

#Requires -Version 5.1
Set-StrictMode -Version 2

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'AeroCapture.ps1')

Initialize-AeroDpiAwareness
Get-AeroInputState | ConvertTo-Json -Compress
