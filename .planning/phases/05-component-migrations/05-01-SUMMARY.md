---
phase: 05-component-migrations
plan: 01
subsystem: ui
tags: [compose, icons, aero, phosphor, migration, material3]

# Dependency graph
requires:
  - phase: 04-aeroicons-foundation
    provides: AeroIcons facade object + 138 extension properties in icons/internal/*.kt
provides:
  - 6 component files with all text-glyph calls replaced by Icon(AeroIcons.*)
  - 2 enum kind files (AeroAlertKind, AeroBannerKind) with Icons.Outlined.* replaced by AeroIcons.*
  - 0 Text("▶▼▲✓–✕") glyph calls remaining across the 8 migrated files
affects: [05-02, 05-03, 05-04, 05-05, wave-2-migrations, wave-5-dep-removal]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Icon(AeroIcons.X, contentDescription, Modifier.size(Ndp), tint=colors.*) call pattern"
    - "Explicit internal.* import alongside AeroIcons facade import for extension property visibility"
    - "Decorative icon: contentDescription=null; actionable icon: English imperative string literal"
    - "Tint map: filled-chip indicator->onPrimary, affordance caret->labelText, close/chrome->onSurface"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt

key-decisions:
  - "AeroNumberSpinner carets rendered at 12dp / button slot 14dp — visual checkpoint approved by user across AeroBlue + AeroDark + Classic themes; no fallback escalation needed"
  - "Explicit internal.* import required alongside AeroIcons facade — Kotlin extension properties not auto-visible via facade object alone; same pattern applied to all 8 files"
  - "AeroBannerKind/AeroAlertKind enum entry names (Info, Warning, Question) do not clash with internal.* imports in when(this) branches — compiler resolves enum members correctly; verified by clean compile"

patterns-established:
  - "Icon import pattern: import com.mordred.aero.icons.AeroIcons + import com.mordred.aero.icons.`internal`.IconName for each icon used"
  - "Close button contentDescription: 'Close toast' / 'Close notification' (English imperative, not null)"
  - "Kind-to-icon enum mapping: property type ImageVector unchanged; only the RHS expression changes from Icons.Outlined.* to AeroIcons.*"

requirements-completed: [MIG-01, MIG-02, MIG-03, MIG-05, MIG-06, MIG-07, MIG-10, MIG-11]

# Metrics
duration: 25min
completed: 2026-04-29
---

# Phase 05 Plan 01: Component Text-Glyph Migration Summary

**8 library components migrated from Text(unicode) / Icons.Outlined.* to Icon(AeroIcons.*) with Phosphor Regular vectors, completing 6 of 9 ROADMAP text-glyph grep targets in Wave 1**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-04-29
- **Completed:** 2026-04-29
- **Tasks:** 9 (8 auto + 1 visual checkpoint, checkpoint approved)
- **Files modified:** 8

## Accomplishments

- All 6 component files (AeroCheckbox, AeroDropdown, AeroNumberSpinner, AeroContextMenu, AeroToastHost, AeroNotificationBanner) now render Icon(AeroIcons.*) — zero Text(unicode) calls remain in those files
- Both kind-enum files (AeroAlertKind, AeroBannerKind) replaced Icons.Outlined.* with AeroIcons.*; Icons.Outlined imports fully removed
- AeroNumberSpinner caret size locked at 12dp / 14dp button slot after visual checkpoint approval across AeroBlue + AeroDark + Classic themes
- compileKotlin green after every commit (8 incremental + 1 clean --rerun-tasks)

## Per-Component Grep Results

| Component | Old glyph | New call | Size | Tint |
|-----------|-----------|----------|------|------|
| AeroCheckbox | Text("✓"/"–") | Icon(AeroIcons.Check/Minus) | 12dp | onPrimary |
| AeroDropdown | Text("▼") | Icon(AeroIcons.CaretDown) | 14dp | labelText |
| AeroNumberSpinner | Text("▲"/"▼") | Icon(AeroIcons.CaretUp/CaretDown) | 12dp (slot 14dp) | onSurface |
| AeroContextMenu | Text("▶") | Icon(AeroIcons.CaretRight) | 12dp | labelText |
| AeroToastHost | Text("✕") | Icon(AeroIcons.X, "Close toast") | 14dp | onSurface |
| AeroNotificationBanner | Text("✕") | Icon(AeroIcons.X, "Close notification") | 14dp | onSurface |
| AeroAlertKind | Icons.Outlined.Info/Warning/Error/HelpOutline | AeroIcons.Info/Warning/XCircle/Question | n/a (24dp default) | accentFor(kind) |
| AeroBannerKind | Icons.Outlined.Info/Warning/Error/CheckCircle | AeroIcons.Info/Warning/XCircle/CheckCircle | n/a (24dp default) | accentForBanner(kind) |

## Task Commits

1. **Task 1: AeroCheckbox glyphs** - `debe49d` (feat)
2. **Task 2: AeroDropdown caret** - `e0abe32` (feat)
3. **Task 3: AeroNumberSpinner carets + slot height** - `e0ba87f` (feat)
4. **Task 4: Visual checkpoint** - approved by user (no code commit)
5. **Task 5: AeroContextMenu submenu indicator** - `4d4f075` (feat)
6. **Task 6: AeroToastHost close button** - `f1a164b` (feat)
7. **Task 7: AeroNotificationBanner close button** - `c902b0d` (feat)
8. **Task 8: AeroAlertKind enum mapping** - `ef3208c` (feat)
9. **Task 9: AeroBannerKind enum mapping** - `a228bfa` (feat)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt` — Check/Minus icons at 12dp
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt` — CaretDown at 14dp
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` — CaretUp/CaretDown at 12dp, button slot 14dp
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt` — CaretRight at 12dp
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt` — X at 14dp with "Close toast"
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt` — X at 14dp with "Close notification"
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt` — AeroIcons.Info/Warning/XCircle/Question mapping
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt` — AeroIcons.Info/Warning/XCircle/CheckCircle mapping

## Decisions Made

- AeroNumberSpinner 12dp/14dp configuration approved at visual checkpoint — no fallback ladder escalation needed
- Explicit `import com.mordred.aero.icons.`internal`.IconName` required for each Kotlin extension property; AeroIcons facade import alone is insufficient for resolution
- `Error -> AeroIcons.XCircle` (Phosphor naming convention, not `Error`); `Question -> AeroIcons.Question` (replaces `HelpOutline`)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Explicit internal.* imports required alongside AeroIcons facade**
- **Found during:** Task 1 (AeroCheckbox), carried forward to all tasks
- **Issue:** Plan's import block listed only `import com.mordred.aero.icons.AeroIcons` — Kotlin extension properties in `icons/internal/*.kt` are not auto-visible via the facade object; compiler cannot resolve `AeroIcons.Check` etc. without explicit `import com.mordred.aero.icons.\`internal\`.Check`
- **Fix:** Added explicit internal.* import for every icon name used per file, following the pattern established in Tasks 1-3 (confirmed working in earlier session)
- **Files modified:** All 8 task files
- **Verification:** compileKotlin green after each task
- **Committed in:** Part of each task's feat commit

**2. [Rule 3 - Blocking] Modifier.size import missing from AeroContextMenu, AeroToastHost, AeroNotificationBanner**
- **Found during:** Tasks 5, 6, 7 (pre-emptive check before edit)
- **Issue:** New `Modifier.size(Ndp)` calls require `import androidx.compose.foundation.layout.size`; these three files had no prior usage of `size`
- **Fix:** Added `import androidx.compose.foundation.layout.size` to each file before adding Icon calls
- **Files modified:** AeroContextMenu.kt, AeroToastHost.kt, AeroNotificationBanner.kt
- **Verification:** compileKotlin green
- **Committed in:** Part of each task's feat commit

---

**Total deviations:** 2 auto-fixed (both Rule 3 — blocking issues pre-empted before compile failure)
**Impact on plan:** Both fixes are standard implementation correctness requirements. No scope creep, no API changes.

## Issues Encountered

- JAVA_HOME pointed to Java 25.0.2 (current system default); Kotlin compiler in this project rejects Java 25 version string with `IllegalArgumentException: 25.0.2`. Fixed by prepending `JAVA_HOME="C:/Users/1/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2"` to all Gradle invocations. This is a known project constraint documented in STATE.md.

## Visual Checkpoint Outcome

**Approved at 12dp.** User confirmed AeroNumberSpinner up/down carets legible in AeroBlue, AeroDark, and Classic themes including disabled state. Phosphor stroke at 12dp (~0.75dp at 96 DPI) is above sub-pixel collapse threshold. No fallback escalation performed.

## Next Phase Readiness

- Wave 1 complete: 6 of 9 ROADMAP text-glyph targets zeroed
- Wave 2 (AeroMenuBar, AeroBreadcrumb) ready to plan; AeroBreadcrumb `separator: String` is a locked exclusion per v1.1 decisions — do not migrate
- Wave 3 (AeroTitleBar) follows after Wave 2
- Wave 5 dep removal (materialIconsExtended from :library) is now unblocked for AeroAlertKind + AeroBannerKind — verify no other Icons.Outlined uses remain before removing dep

---
*Phase: 05-component-migrations*
*Completed: 2026-04-29*
