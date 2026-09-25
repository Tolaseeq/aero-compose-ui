# Phase 22 -- Vetted Temporary Environment for the Full VER-12 Session

Recorded by Plan 03 (Task 3), 2026-09-25. Nothing on this page was installed while writing it --
every file below was downloaded into this session's own scratch directory (outside the repo,
never committed), hashed, and Authenticode-checked in place. Both candidates are approved for the
full-session install in a later plan; this document is the sign-off that makes that install a
"run the already-vetted command" step, not a live decision.

## 1. Virtual second display (150%): VirtualDrivers "Virtual Display Driver" (MttVDD)

**Source (official):** `github.com/VirtualDrivers/Virtual-Display-Driver`, release tag `25.7.23`
("Beta: Virtual Driver Control (25.7.23)", published 2026-07-23). Confirmed via the GitHub
Releases API, not a mirror. This is the same project 22-RESEARCH.md's own candidate list named
first ("VirtualDrivers 'Virtual Display Driver' GitHub releases").

| File | Role | SHA-256 | Authenticode | Signer | Issuer |
|------|------|---------|---------------|--------|--------|
| `MttVDD.dll` (from `VirtualDisplayDriver-x86.Driver.Only.zip`) | UMDF2 IddCx driver binary | `C9CA837F57A98FBD43BC416A7F535A95843626E7759EAF85CF0CD7CE334DBB05` | **Valid** | CN=SignPath Foundation, O=SignPath Foundation, L=Lewes, S=Delaware, C=US | CN=GlobalSign GCC R45 CodeSigning CA 2020 |
| `mttvdd.cat` (driver catalog, same zip) | Driver package signature (INF/DLL) | `08A0093FC9B2E32B287A6F8A77CA4DE0A31830D29FC33D2B13A918DC859468F6` | **Valid** | CN=SignPath Foundation, O=SignPath Foundation, L=Lewes, S=Delaware, C=US | CN=GlobalSign GCC R45 CodeSigning CA 2020 |
| `VDD Control.exe` (from `VDD.Control.25.7.23.zip`, the install/config GUI) | Installer/config app | `CC6FF00C23E0E62A45EFB7ED59FC83AB0E7F8B0FEAB3E4C76203F360E5DE2F0F` | **Valid** | CN=SignPath Foundation, O=SignPath Foundation, L=Lewes, S=Delaware, C=US | CN=GlobalSign GCC R45 CodeSigning CA 2020 |
| `Dependencies\devcon.exe` (bundled inside the same zip) | Microsoft's own DevCon utility, used to add/remove the root-enumerated device | `77E87E7C4E23D6B9A4F5D86D15ED9EC61A16A242FB299C390AF6F179225BFA05` | **Valid** | CN=Microsoft Corporation, O=Microsoft Corporation, L=Redmond, S=Washington, C=US | CN=Microsoft Code Signing PCA 2010 |

All four `Get-AuthenticodeSignature` calls returned `Status=Valid` / `StatusMessage=Signature
verified.` on this machine as-is -- the GlobalSign chain is already in the Windows trusted-root
store, so **no test-signing mode and no driver-signature-enforcement toggle are needed**. This
satisfies D-04/VER-12's own bar: production-signed, Authenticode Valid on both the installer and
the driver files, official source only.

**Why this candidate over Parsec VDD:** the project's own README comparison table lists both as
signed (`Virtual-Display-Driver (HDR)`: IddCx 1.10, HDR-capable, ARM64; `parsec-vdd`: IddCx 1.5,
SDR-only) -- VirtualDrivers' driver is the newer IddCx version and was independently verifiable
end-to-end (installer, driver DLL, and catalog all fetched and Authenticode-checked directly from
this project's own GitHub Releases API in this session); Parsec VDD was not independently
re-verified this session since the primary candidate already passed every check.

**Driver identity:** UMDF2 (`UmdfLibraryVersion=2.25.0`, `UmdfExtensions=IddCx0102`), root-enumerated
hardware ID `Root\MttVDD` (`MttVDD.inf`'s `[Standard.NTamd64]` section) -- no PCI/physical hardware
needed, and its kernel-side service is `WUDFRd.sys`, the stock Windows User-Mode Driver Framework
reflector that already ships inside Windows (`%SystemRoot%\System32\drivers\WUDFRd.sys`) and needs
no separate signing of its own.

**Install command** (admin PowerShell; the maintainer confirms the UAC prompt per PROJECT.md's
pre-authorized plan):
```powershell
# From the extracted VirtualDisplayDriver-x86.Driver.Only.zip contents (MttVDD.inf/.dll/.cat) and
# the devcon.exe bundled in VDD.Control.<ver>.zip\Dependencies\devcon.exe (already Authenticode-
# verified above -- no separate download needed):
& devcon.exe install .\MttVDD.inf "Root\MttVDD"
```
Optional (silences a first-time "install this device software?" prompt on unattended runs, not
required since the chain is already trusted): import the driver's own catalog certificates into
the local machine's Trusted Publishers store before installing --
```powershell
$certs = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2Collection
$certs.Import([System.IO.File]::ReadAllBytes('.\mttvdd.cat'))
foreach ($c in $certs) { Export-Certificate/Import-Certificate into Cert:\LocalMachine\TrustedPublisher (see Community Scripts\silent-install.ps1 for the exact two-line pattern) }
```

**Uninstall command:**
```powershell
& devcon.exe remove "Root\MttVDD"
```

**Verification command** (confirms the second display exists and is the right driver, without
changing anything -- `-InstanceId`, verified live on this machine's `PnpDevice` module; the
`virtual-driver-manager.ps1` script's own `-HardwareID` parameter does not exist on this machine's
PowerShell 5.1 `PnpDevice` module, confirmed by `(Get-Command Get-PnpDevice).ParameterSets`, so the
session script must use `-InstanceId`, not copy that script's parameter verbatim):
```powershell
Add-Type -AssemblyName System.Windows.Forms   # not loaded by default in a plain PS 5.1 host
[System.Windows.Forms.Screen]::AllScreens.Count   # expect 2 after install, 1 before/after removal
Get-PnpDevice -InstanceId 'ROOT\MttVDD\*'          # Status should be OK after install
```

**150% scale, second display only (never touches the primary monitor):** the project's own
`Community Scripts\scale-VDD.ps1` targets exactly this via the `DisplayConfig` PowerShell Gallery
module (`Install-Module DisplayConfig -RequiredVersion 1.1.1`), which wraps
`DisplayConfigSetDeviceInfo`/`DISPLAYCONFIG_DEVICE_INFO_SET_DPI_SCALE` -- the same Win32 mechanism
22-CONTEXT.md's own `<specifics>` names as preferred:
```powershell
Import-Module DisplayConfig
$disp = Get-DisplayInfo | Where-Object { $_.DisplayName -eq 'VDD by MTT' }
Set-DisplayScale -DisplayId $disp.DisplayId -Scale 150   # only this DisplayId is touched
```
**Restore check:** after `devcon remove`, `[System.Windows.Forms.Screen]::AllScreens.Count` back to
`1` and `Get-PnpDevice -InstanceId 'ROOT\MttVDD\*'` returns nothing.

## 2. PowerToys (FancyZones)

**Source (official):** `github.com/microsoft/PowerToys`, release tag `v0.101.2362.0` (published
2026-08-25), fetched via the GitHub Releases API. `winget` is not present on this machine
(`winget show` -- command not found), confirming 22-RESEARCH.md's own Environment Availability
table and triggering the plan's own documented fallback: the GitHub release asset.

| Asset | Scope | UAC | SHA-256 | Authenticode | Signer |
|-------|-------|-----|---------|---------------|--------|
| `PowerToysUserSetup-0.101.2362.0-x64.exe` (downloaded and verified this session) | Per-user | **No** | `D56FA7130FA68AFE553068C15A59A6B24C8DBCC9A0989A43EF0FC5A373230DE3` | **Valid** | CN=Microsoft Corporation, O=Microsoft Corporation, L=Redmond, S=Washington, C=US (issuer: CN=Microsoft Code Signing PCA 2024) |
| `PowerToysSetup-0.101.2362.0-x64.exe` (machine-wide variant; not downloaded -- same publisher/signing pipeline as the per-user asset above, listed for completeness) | Machine-wide | Yes | -- (not independently hashed this session) | -- | Microsoft Corporation (same release) |

**Chosen variant for the full session: `PowerToysUserSetup` (per-user, no UAC)** -- lower
footprint for a temporary install/removal cycle, and FancyZones works identically per-user.

**Install command** (WiX Burn bootstrapper, standard silent switches):
```powershell
.\PowerToysUserSetup-0.101.2362.0-x64.exe /install /quiet /norestart
```

**Uninstall command:**
```powershell
.\PowerToysUserSetup-0.101.2362.0-x64.exe /uninstall /quiet
# or, if the installer file is no longer present:
Get-Package -Name 'PowerToys (Preview)' | Uninstall-Package
```

**Verification command:** `Test-Path "$env:LOCALAPPDATA\Microsoft\PowerToys\PowerToys.exe"` after
install; the plan's own pre-check already confirmed this path is `False` today (22-RESEARCH.md
Environment Availability).

**FancyZones default layout / zone-rect check:** on first run FancyZones creates its own config
directory at `%LOCALAPPDATA%\Microsoft\PowerToys\FancyZones\` (per-user scope, matching the chosen
installer variant); `zones-settings.json` there holds the active layout's zone rectangles, which
the full session's own SNAP-07 check reads to confirm the showcase window snapped into a zone.

**Restore check:** after uninstall, `Test-Path "$env:LOCALAPPDATA\Microsoft\PowerToys\PowerToys.exe"`
is `False` again; the maintainer's word is still the actual gate for removal timing per PROJECT.md
("PowerToys ставится для FancyZones ... удаляется по слову мейнтейнера").

## 3. Taskbar auto-hide toggle

No download needed -- this is a registry/Win32 state change on the machine's own shell, already
characterized empirically in 22-RESEARCH.md's Environment Availability table and 22-NOTES.md's C3
finding (`AutoHideOn=True`, `AutoHideEdges=Bottom`).

**Current value to restore:** `HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\StuckRects3`
value `Settings`, byte offset 8 = `0x03` (autohide bit + always-on-top bit both set).

**Toggle off (before the visible-panel checks in the full session) / on (after):**
```powershell
# SHAppBarMessage ABM_SETSTATE (0xA) with an APPBARDATA.lParam of ABS_AUTOHIDE (1) to turn on,
# 0 to turn off; verified immediately after via ABM_GETSTATE (4).
$data = New-Object AeroWinProbe.AppBarData   # reuse tools/winprobe/WinProbe.ps1's own struct/P-Invoke
$data.cbSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type]([AeroWinProbe.AppBarData]))
$data.lParam = [IntPtr]0    # 0 = off, [IntPtr][AeroWinProbe.Native]::ABS_AUTOHIDE = on
[AeroWinProbe.Native]::SHAppBarMessage(0xA, [ref]$data) | Out-Null   # ABM_SETSTATE
```
(`tools/winprobe/WinProbe.ps1`'s `Get-WinProbeTaskbar` already implements the `ABM_GETSTATE` read
half via the same P/Invoke surface; only `ABM_SETSTATE` -- not currently exposed there -- needs
adding when the full session's plan wires this up. No registry value is written directly; the
Explorer shell owns writing `StuckRects3` in response to the `SHAppBarMessage` call.)

**Verification command:** `Get-WinProbeTaskbar -Hwnd <any hwnd>` (existing helper) -- `AutoHideOn`
flips as expected.

**Restore check:** re-run the toggle with `ABS_AUTOHIDE` set, then `Get-WinProbeTaskbar` reports
`AutoHideOn=True` again, matching the byte-8=`0x03` value recorded above as today's baseline.

## Teardown checklist (full session, in order)

1. Toggle taskbar auto-hide back **on** (`ABM_SETSTATE` with `ABS_AUTOHIDE`); verify via
   `Get-WinProbeTaskbar`.
2. Uninstall PowerToys (`PowerToysUserSetup ... /uninstall /quiet`) **only after the maintainer's
   word**, per PROJECT.md; verify `PowerToys.exe` path is gone.
3. Remove the virtual display driver (`devcon.exe remove "Root\MttVDD"`); verify
   `[System.Windows.Forms.Screen]::AllScreens.Count` is back to `1` and `Get-PnpDevice -InstanceId
   'ROOT\MttVDD\*'` returns nothing.
4. Confirm no leftover scratch installers were copied into the repository (`git status --porcelain`
   stays clean of anything under a `.captures/` or scratch-like path) -- all installers in this
   vetting pass lived only in the session's own scratch directory outside `C:\1A_WORK\ui_lib`,
   never inside the repo.

## What was NOT installed this session

Nothing. Every file above was downloaded to this session's own scratch directory
(`%TEMP%\claude\...\scratchpad\vdd-vet\`, outside the repository), hashed, and Authenticode-checked
in place; none of `devcon.exe install`, `VDD Control.exe`, or `PowerToysUserSetup.exe /install` was
run. Confirmed after vetting: `[System.Windows.Forms.Screen]::AllScreens.Count` is still `1`, and
`Test-Path "$env:LOCALAPPDATA\Microsoft\PowerToys\PowerToys.exe"` is still `False`.

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Vetted: 2026-09-25*
