# Phase 22 — Full VER-12 real-input session record

Plan 22-15. Task 1 (readiness) recorded 2026-09-28. The session itself (Task 3) appends
`## Pass 1 — JDK 21` / `## Pass 2 — JBR 21` below after the maintainer's "ок".

## Readiness

Recorded by Task 1, 2026-09-28, before any consent was asked. Nothing was installed, no real
input was sent, no settings were changed while recording this section.

### Builds

- `./gradlew :showcase:classes --console=plain` → BUILD SUCCESSFUL (7 tasks, all UP-TO-DATE).
- Full unfiltered `./gradlew :library:test --rerun --console=plain` → BUILD SUCCESSFUL,
  `AERO_TEST_COUNT total=592 skipped=0 expected=592 expectedSkipped=0 filtered=false` — the
  locked count from Plan 13 holds.

### Installer re-verification (against 22-SESSION-ENV.md, exact match required)

Session scratchpad (outside the repo, nothing committed, nothing executed):
`C:\Users\1\AppData\Local\Temp\aero-22-session\`. Fresh downloads from the official GitHub
Releases APIs (tag `25.7.23` and `v0.101.2362.0`), then SHA-256 + Authenticode re-verified
(`Verify-Installers.ps1` in the scratchpad):

| File | SHA-256 | Authenticode | Signer |
|------|---------|--------------|--------|
| `MttVDD.dll` | MATCH (C9CA…BB05) | Valid | CN=SignPath Foundation (GlobalSign GCC R45 CodeSigning CA 2020) |
| `mttvdd.cat` | MATCH (08A0…68F6) | Valid | CN=SignPath Foundation (GlobalSign GCC R45 CodeSigning CA 2020) |
| `VDD Control.exe` | MATCH (CC6F…2F0F) | Valid | CN=SignPath Foundation (GlobalSign GCC R45 CodeSigning CA 2020) |
| `devcon.exe` | MATCH (77E8…FA05) | Valid | CN=Microsoft Corporation (Microsoft Code Signing PCA 2010) |
| `PowerToysUserSetup-0.101.2362.0-x64.exe` | MATCH (D56F…0DE3) | Valid | CN=Microsoft Corporation (Microsoft Code Signing PCA 2024) |

`MttVDD.inf` present. `driver-flat\` (scratchpad) assembles the five vetted files in the flat
layout `Install-AeroVirtualDisplay -DriverDir` expects. Task 3's real run passes
`-DriverDir C:\Users\1\AppData\Local\Temp\aero-22-session\driver-flat` and
`-PowerToysInstaller C:\Users\1\AppData\Local\Temp\aero-22-session\PowerToysUserSetup-0.101.2362.0-x64.exe`;
`SessionEnv.ps1` re-verifies every SHA-256 again before anything elevated runs.

Driver identity for the consent message: VirtualDrivers "Virtual Display Driver" (MttVDD),
release 25.7.23, signed by SignPath Foundation (GlobalSign) — no test-signing mode needed.
PowerToys: per-user `PowerToysUserSetup` v0.101.2362.0, signed Microsoft Corporation, no UAC.

### Dry run (timed)

Command:
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/winprobe/Invoke-FullSession.ps1 -DryRun -Pass Both -Phase Checks -OutDir .captures/22-session-readiness`

- **Exit code 0**, wall-clock **412 s (6 min 52 s)**.
- jdk pass: `jvm=standard path=C:\Users\1\.jdks\ms-21.0.9\bin\java.exe`, 172.8 s,
  totals pass=2 fail=0 unconfirmed=52 (54 SESSION lines, 55 IDs — C02 emits two).
- jbr pass: `jvm=JBR path=C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe`, 181.7 s, same totals.
- All **55 check IDs listed exactly once per pass** (grep-verified), 0 `SESSION_CHECK_ERROR`.
- A04-OPTOUT RED control self-proved in both passes: on the `-Paero.nativeChrome=false` launch
  every V11 check FAILs (`v11 pass=0 fail=18`) and zero chrome-trace lines appear.
- Cursor `685,926 → 685,926` unchanged; `EnvUnchanged=True` (screens, auto-hide, PowerToys,
  FancyZones, virtual display all identical before/after); zero installs.
- Zero leftover `com.mordred.showcase.MainKt` JVMs after the run (PID-tracked teardown).
- API-04 attribution line in both pass headers:
  `AeroTitleBar -> rememberAeroWindowChrome (code references rememberAeroWindowChrome=1, NativeWindowChromeRegistry=0)`.

### Minutes estimate for the maintainer (N)

| Component | Basis | Time |
|-----------|-------|------|
| Mechanics of both passes (launches incl. hotRun, window waits, V11 probe cycles, W06/C02/F18 relaunches, frame captures) | measured in the dry run above | 412 s ≈ 7 min |
| Planned real input of both passes (real-only sleeps, drag durations, hover/flyout waits summed from the script, ~107 s per pass) | computed from `Invoke-FullSession.ps1` | ≈ 4 min |
| Environment setup (devcon install + UAC + display-appear wait ~1 min; DisplayConfig module install — confirmed absent today — + scale set ~1 min; PowerToys silent install ~2.5 min; FancyZones start ~0.5 min; auto-hide toggle instant) | estimated from 22-SESSION-ENV.md commands | ≈ 5 min |
| **Subtotal** | 7 + 4 + 5 | **16 min** |
| Round up, add 3 (plan's formula) | 16 → 16 + 3 | **N = 19 min** |

### Restore target (recorded read-only; nothing changed)

- `Get-AeroEnvState`: screens=1; primary monitor `0,0,1920,1080`, work `0,0,1920,1080`,
  96 DPI (scale 1.0); `TaskbarAutoHideOn=True`; `PowerToysInstalled=False`;
  `PowerToysRunning=False`; `FancyZonesRunning=False`; `VirtualDisplayPresent=False`.
- `HKCU:\...\Explorer\StuckRects3` value `Settings`, byte offset 8 = **0x03**
  (auto-hide + always-on-top) — the byte the session must leave unchanged.
- Snap settings: `SnapAssistFlyoutOn=True`, `WindowArrangementActive=True`.
- End-of-session obligation (Task 3): auto-hide back to ON (0x03 preserved), no leftover
  showcase JVM; the virtual display driver and PowerToys stay installed on purpose until
  Plan 16, on the maintainer's word.

### JVM paths verified

- Pass 1 (standard JDK 21): `C:\Users\1\.jdks\ms-21.0.9\bin\java.exe` — exists,
  `openjdk version "21.0.9" Microsoft-12574459`; dry-run pass reported `jvm=standard`.
- Pass 2 (JBR 21): `C:\Users\1\.jdks\jbr-21.0.9\bin\java.exe` — exists,
  `JBR-21.0.9+1-1038.76-nomod`; dry-run pass reported `jvm=JBR`.

**Readiness verdict:** the session can start the moment consent arrives — builds green at the
locked count, installers byte-identical to the vetted record, the suite dry-run-proven at
exit 0 with a measured duration, and the restore target recorded.
