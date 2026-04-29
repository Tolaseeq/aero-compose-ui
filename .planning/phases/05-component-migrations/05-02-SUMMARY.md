---
phase: 05-component-migrations
plan: 02
subsystem: input-components
tags: [icon-migration, canvas-deletion, wave-2, MIG-08, MIG-09]
dependency_graph:
  requires: [04-aeroicons-foundation]
  provides: [MIG-08, MIG-09]
  affects: [AeroSearchField, AeroPasswordField]
tech_stack:
  added: []
  patterns: [material3.Icon at call site, internal.* extension property imports alongside AeroIcons facade]
key_files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt
decisions:
  - "Carry-over Rule 3 deviation applied: explicit internal.* imports (MagnifyingGlass, X, Eye, EyeSlash) added alongside AeroIcons facade — required for extension property resolution in CMP 1.7.3"
metrics:
  duration: 12min
  completed: "2026-04-29"
  tasks_completed: 2
  files_modified: 2
---

# Phase 05 Plan 02: Wave 2 Canvas Deletion — AeroSearchField + AeroPasswordField Summary

**One-liner:** Deleted 3 hand-rolled Canvas composables (~94 lines) and replaced with `Icon(AeroIcons.*)` calls with proper contentDescription; compile green.

## What Was Built

Wave 2 of Phase 5 eliminated all Canvas-based icon drawing from two input components:

- **AeroSearchField.kt**: Deleted `SearchIcon()` Canvas composable (circle + line drawn manually). Replaced call site with `Icon(AeroIcons.MagnifyingGlass, null, 14dp, labelText)`. Replaced `Text("x")` clear button with `Icon(AeroIcons.X, "Clear search", 14dp, labelText)`.
- **AeroPasswordField.kt**: Deleted `EyeOpenIcon(tint)` and `EyeClosedIcon(tint)` Canvas composables (almond-path + iris dot / almond-path + slash). Replaced the visibility toggle with `Icon(imageVector = if (visible) AeroIcons.EyeSlash else AeroIcons.Eye, contentDescription = if (visible) "Hide password" else "Show password", ...)`.

## Lines Deleted per File

| File | Canvas Composables Deleted | Lines Removed |
|------|---------------------------|---------------|
| AeroSearchField.kt | `SearchIcon()` | ~31 lines (fn + KDoc) |
| AeroPasswordField.kt | `EyeOpenIcon(tint)`, `EyeClosedIcon(tint)` | ~63 lines (both fns + KDocs) |
| **Total** | 3 composables | **~94 lines** |

## Per-File Grep Results

### AeroSearchField.kt

```
grep -nE 'SearchIcon|Canvas|StrokeCap|drawscope' → 0 hits (PASS)
grep -n 'Text("x"'                               → 0 hits (PASS)
grep -n 'AeroIcons.MagnifyingGlass'              → 2 hits (line 24 KDoc, line 61)
grep -n 'AeroIcons.X'                            → 1 hit  (line 89)
grep -n '"Clear search"'                         → 1 hit  (line 90)
grep -n 'androidx.compose.foundation.Canvas'     → 0 hits (PASS)
```

### AeroPasswordField.kt

```
grep -nE 'EyeOpenIcon|EyeClosedIcon|Canvas|drawPath|drawCircle|drawLine' → 0 hits (PASS)
grep -n 'AeroIcons.Eye'                          → 2 hits (lines 47, 119)
grep -n 'AeroIcons.EyeSlash'                     → 2 hits (lines 48, 119)
grep -n '"Show password"'                        → 1 hit  (line 120)
grep -n '"Hide password"'                        → 1 hit  (line 120)
grep -n 'androidx.compose.foundation.Canvas'     → 0 hits (PASS)
```

## Compile Status

`./gradlew :library:compileKotlin` — **BUILD SUCCESSFUL** after each commit (JDK 17 via Gradle toolchain).

## Commit List

| Hash | Message |
|------|---------|
| `3d7669f` | feat(05-02-MIG-08): delete SearchIcon Canvas; migrate AeroSearchField to AeroIcons.MagnifyingGlass + X |
| `d6bfb79` | feat(05-02-MIG-09): delete Eye{Open,Closed}Icon Canvas; migrate AeroPasswordField toggle to AeroIcons.Eye/EyeSlash |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Carry-over] Explicit internal.* extension property imports added**
- **Found during:** Task 1 (applying carry-over deviation pattern from 05-01)
- **Issue:** AeroIcons extension properties are defined in `com.mordred.aero.icons.\`internal\`.*` packages; importing only the `AeroIcons` facade object is insufficient for the Kotlin compiler to resolve them in CMP 1.7.3
- **Fix:** Added explicit imports for each icon used: `com.mordred.aero.icons.\`internal\`.MagnifyingGlass`, `com.mordred.aero.icons.\`internal\`.X` (Task 1); `com.mordred.aero.icons.\`internal\`.Eye`, `com.mordred.aero.icons.\`internal\`.EyeSlash` (Task 2) — same pattern established in Phase 05-01
- **Files modified:** AeroSearchField.kt, AeroPasswordField.kt
- **Commit:** included in each task's commit

**2. [Rule 3 - Environment] Build required JAVA_HOME override to JDK 17**
- **Found during:** Task 1 verification
- **Issue:** Default Java 25.0.2 on PATH causes `JavaVersion.parse()` failure in Kotlin compiler — known pre-existing issue documented in STATE.md
- **Fix:** Used `JAVA_HOME="/c/Users/1/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2"` for all Gradle invocations (JDK 17 already cached in Gradle toolchain cache)
- **Files modified:** None (environment-only fix)
- **Commit:** N/A

## Self-Check: PASSED

- [x] `AeroSearchField.kt` exists and contains `AeroIcons.MagnifyingGlass`
- [x] `AeroPasswordField.kt` exists and contains `AeroIcons.EyeSlash`
- [x] Commit `3d7669f` exists in git log
- [x] Commit `d6bfb79` exists in git log
- [x] compileKotlin green after both commits
