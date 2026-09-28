---
phase: 22-native-window-behavior-release-3-2-0
plan: 16
subsystem: testing
tags: [ver-13, hand-off, teardown, devcon, pnputil, printwindow, lockbits]

# Dependency graph
requires:
  - phase: 22-native-window-behavior-release-3-2-0 (Plan 15)
    provides: the full VER-12 real-input session record, the installed MttVDD driver and PowerToys awaiting teardown, and the maintainer's checkpoint replies
provides:
  - Verified machine restoration (driver device + driver-store package gone, screens=1, auto-hide = recorded original, PowerToys kept by the maintainer's word, scratchpad deleted)
  - 22-UNCONFIRMED.md — the explicit not-proven list (16 sections, Windows 10 named)
  - 22-HANDOFF.md — per-requirement evidence (28 rows), VER-11 GREEN+RED tables, VER-12 per-pass totals, seven settled conflicts, FAIL blockers on top
  - Final frames + final VER-11 GREEN (18/18+8/8) and RED control (0/18+0/8) in .captures/22-final/
affects: [22 gap-closure planning, release plans 17-18, v3.2.0 closeout]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Teardown verification reads machine state (PnP device any-status sweep + driver-store pnputil check + StuckRects3 byte), never assumptions"
    - "Measurement-based frame review: LockBits full-frame + sliding band comparisons substitute for eyeball review when the executor cannot render images — with the limitation recorded"

key-files:
  created:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-UNCONFIRMED.md
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-HANDOFF.md
  modified:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION.md (## Teardown)
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md (## Final hand-off (22-16))
    - .planning/REQUIREMENTS.md (VER-12, VER-13 -> Complete)

key-decisions:
  - "Driver removal completed below the device level: pnputil /delete-driver oem73.inf removed the store package the devcon device removal leaves behind (must-have: the driver package is gone)"
  - "DisplayConfig module uninstalled (its pre-session absence is recorded), but the PSGallery trust flag is only named as residue — its pre-session value was never recorded, and reverting it would be a guess"
  - "The 22-16 frame review is measurement-based (LockBits), not visual: this executor environment cannot render images; the method and its limitation are recorded in 22-NOTES.md"
  - "tools/capture/New-HandoffPage.ps1 is bound to the v3.1 directory layout, so an optional phase-specific generator produced .captures/22-final/handoff/index.html instead"

patterns-established:
  - "Full-frame (not just title-band) LockBits comparison against the pre-phase baseline is the strongest available D-02 statement — 0 px across all three schemes held after the whole phase"

requirements-completed: [VER-12, VER-13, VER-11, API-04]

# Metrics
duration: 22min
completed: 2026-09-28
---

# Phase 22 Plan 16: Teardown + VER-13 Hand-off Summary

**Verified session teardown (devcon + pnputil driver removal, auto-hide = recorded original, PowerToys kept per the maintainer's word) plus the VER-13 hand-off: 28-row evidence table, final VER-11 GREEN 18/18+8/8 with live RED controls, and a 16-section unconfirmed list**

## Performance

- **Duration:** ~22 min (continuation session; Task 1's checkpoint was answered before spawn)
- **Started:** 2026-09-28T10:16:35Z
- **Completed:** 2026-09-28T10:38:00Z
- **Tasks:** 3 (1 checkpoint answered by the maintainer + 2 executed here)
- **Files modified:** 5 tracked (2 created, 3 modified) + REQUIREMENTS/STATE/ROADMAP bookkeeping

## Accomplishments

- Machine restored and verified from live state: MttVDD device gone (any-status PnP sweep), driver-store package `oem73.inf` deleted via pnputil, screens back to 1, StuckRects3 byte 8 = 0x03 with ABM auto-hide ON, PowerToys KEPT per «1 оставь», scratchpad deleted, DisplayConfig module removed (known pre-state)
- Final frames: main + narrow × three themes, floating + maximized, PrintWindow capture mode — the main floating window is FULL-FRAME 0 px vs the pre-phase baseline in all three schemes; cross-run stability vs the session's floating frames (jdk + jbr) also 0 px
- Final VER-11 GREEN 18/18 + 8/8 (`-AssertV11` exit 0) and RED control `RED OK` (0/18 + 0/8, style 0x960B0000, zero chrome traces), both on the same day post-teardown
- 22-HANDOFF.md: 28 requirement rows, five FAIL release-blockers listed on top, VER-11 GREEN/RED tables with the Plan 01 RED baseline cited, VER-12 per-pass totals (31/18/5 and 24/25/5), all seven conflicts with settled answers
- 22-UNCONFIRMED.md: Windows 10, non-100 % scaling, real second monitor, Linux/macOS runtime, compositor/visual effects, IME, screen readers, binary compatibility, fullscreen/peer re-creation, JDK 24+, every UNCONFIRMED session check and JVM-pass difference, single-layout live proof of the custom API, first consumer not upgraded, check-formula artifacts, real-input checks without old-path controls

## Task Commits

1. **Task 1: Maintainer confirms driver removal and decides about PowerToys** — checkpoint:human-action, answered before this session («1 оставь» = PowerToys KEPT; «2 ок» = UAC approved); no commit (checkpoint only)
2. **Task 2: Teardown and verified restoration** — `5d561dc` (docs)
3. **Task 3: VER-13 hand-off — self-review, final VER-11 green/red, unconfirmed list** — `d15185c` (docs)

**Plan metadata:** this commit (docs: complete plan)

## Files Created/Modified

- `.planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION.md` — `## Teardown`: before/action/after/verification per item, named residues
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-UNCONFIRMED.md` — the not-proven list (VER-13)
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-HANDOFF.md` — per-requirement evidence hand-off (VER-13)
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md` — `## Final hand-off (22-16)`: method limitation, final frames, final VER-11, session frame re-review
- `.planning/REQUIREMENTS.md` — VER-12 and VER-13 marked Complete (statuses of FAILed requirements deliberately left for the gap-closure round)
- Git-ignored evidence: `.captures/22-final/` (Invoke-Teardown.ps1, Invoke-FinalFrames.ps1, Invoke-RedControl.ps1, Invoke-FrameReview.ps1, New-HandoffPage22.ps1, 9 frames, v11-green/-red JSONs + consoles, finalframes-console.log, handoff/index.html)

## Decisions Made

- Removed the driver-store package (`pnputil /delete-driver oem73.inf`, one more UAC under the same approval) — the device-level check alone would have left `mttvdd.inf` in the store, contradicting the must-have "the driver package is gone"
- Uninstalled the DisplayConfig module (its pre-session absence is on record) but did NOT touch the PSGallery trust flag (pre-state unrecorded — reverting would be a guess; named as residue in 22-SESSION.md instead)
- The agent frame review is pixel-measurement-based; recorded as a method limitation rather than claimed as visual inspection
- Skipped reusing `tools/capture/New-HandoffPage.ps1` (v3.1 directory contract) in favor of a phase-specific generator for the optional HTML page

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] PS 5.1 array-unroll on function return in the teardown script**
- **Found during:** Task 2
- **Issue:** `Get-VddDevicesAnyStatus` returns `@(...)` but PowerShell unrolls it at the pipeline boundary — a one-device result bound as a scalar and `.Count` threw under StrictMode 2
- **Fix:** wrapped every call site in `@()` (the known 22-15 pitfall, re-applied)
- **Files modified:** `.captures/22-final/Invoke-Teardown.ps1` (git-ignored)
- **Verification:** script ran to `TEARDOWN RESULT VERIFIED`
- **Committed in:** n/a (git-ignored tooling)

**2. [Rule 3 - Blocking] Driver-store package left behind by the planned removal**
- **Found during:** Task 2
- **Issue:** `devcon remove` deletes the device but leaves `oem73.inf` (mttvdd.inf) in the DriverStore — the plan's "driver package is gone" would not have held
- **Fix:** `pnputil /delete-driver oem73.inf` elevated (same UAC approval scope), store re-checked: 0 `mttvdd` occurrences
- **Files modified:** none tracked
- **Verification:** `pnputil /enum-drivers` grep = 0; PnP any-status sweep = 0
- **Committed in:** recorded in `5d561dc`

**3. [Rule 2 - Completeness] Session residue beyond the plan's checklist**
- **Found during:** Task 2
- **Issue:** the 22-15 repair left a per-user DisplayConfig module and a trusted PSGallery policy; the plan's checklist did not name them
- **Fix:** module uninstalled (pre-state known: absent); PSGallery trust recorded as named residue (pre-state unknown — not reverted by guess)
- **Verification:** `Get-Module -ListAvailable DisplayConfig` = absent; policy state recorded verbatim
- **Committed in:** recorded in `5d561dc`

---

**Total deviations:** 3 auto-fixed (1 bug, 1 blocking, 1 completeness)
**Impact on plan:** All within the plan's own acceptance criteria and threat model (T-22-05 residue); no scope creep — nothing beyond the session's footprint was touched.

## Assumption Drift (advisory)

- **Found during:** Task 3 — planned: "the agent looks at every final frame AND every session frame and writes what it sees"; actual: this executor environment returns CDN references instead of rendered pixels for image Reads, so the review is LockBits-measurement-based (full-frame + band sweeps + strip/crop re-measures), with the phase's earlier visual reviews (22-06/22-09/22-19) carrying the visual dimension. Why: environment capability, not choice; recorded in 22-NOTES.md § "Inspection method (honest limitation)" and surfaced in 22-HANDOFF.md.
- **Found during:** Task 3 — planned: session frame review covers "hover/press/snapped/150 %"; actual: no 150 % frames exist (W04 contingency — already UNCONFIRMED), and the jdk `main-snapped` PNG turns out byte-identical to the floating frame (capture caught the restored window) — recorded as an evidence-context note; the JDK snap proof rests on the S01 rect records.

## Issues Encountered

- Two UAC elevations were needed for full driver removal (devcon + pnputil); the maintainer had approved the driver-removal UAC in the checkpoint and confirmed both prompts
- The first teardown attempt died on the PS 5.1 scalar `.Count` before anything was removed (deviation 1); re-run succeeded

## User Setup Required

None — no external service configuration. (The maintainer may revert `Set-PSRepository PSGallery -InstallationPolicy Untrusted` if they want the install prompt back; recorded in 22-SESSION.md § Teardown.)

## Next Phase Readiness

- The hand-off is complete for the maintainer: 22-HANDOFF.md + 22-UNCONFIRMED.md + frames (`.captures/22-final/`, offline page `handoff/index.html`)
- Gap-closure planning input: the five FAIL blockers (SNAP-04, SNAP-05 with the known C2 fix path, SNAP-06 border, BTN-01 press parity, JBR-pass drift) plus the eight check-formula artifacts to fix and re-run
- Release plans 17-18 (REL-06/07/08) remain; version/tag/JitPack untouched
- Machine state: pre-session except PowerToys (kept by word) and the named PSGallery trust flag; zero leftover showcase/gradle-wrapper processes from this plan's runs

## Self-Check: PASSED

- Files exist: 22-UNCONFIRMED.md, 22-HANDOFF.md, 22-16-SUMMARY.md, 22-SESSION.md, 22-NOTES.md (checked below)
- Commits exist: 5d561dc (Task 2), d15185c (Task 3) in `git log` (checked below)
- Verify commands re-run post-commit: Teardown grep = 1 + screens = 1; UNCONFIRMED "Windows 10" = 5; HANDOFF rows = 28, "RED OK" = 3

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-28*
