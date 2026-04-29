---
phase: 05-component-migrations
plan: "03"
subsystem: navigation/AeroTitleBar
tags: [migration, icons, imageVector, window-chrome, titlebar]
dependency_graph:
  requires: [phase-04-aeroicons-foundation]
  provides: [MIG-04]
  affects: [AeroTitleBar.kt]
tech_stack:
  added: []
  patterns: [Icon(imageVector) with explicit tint, internal.* extension property imports]
key_files:
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
decisions:
  - "Tint uniformly colors.onSurface inside Icon — textColor: Color param removed; hover color only applied to button background"
  - "AeroIcons.Square for maximize (floating state), AeroIcons.FrameCorners for restore (maximized state)"
  - "contentDescription follows actionable convention: 'Minimize window' / 'Maximize window' / 'Restore window' / 'Close window'"
  - "import androidx.compose.ui.unit.sp retained — still used by title Text(fontSize=13.sp) at the top of AeroTitleBar"
metrics:
  duration: "~4 minutes"
  completed: "2026-04-29T10:17:09Z"
  tasks_completed: 1
  files_modified: 1
requirements_fulfilled: [MIG-04]
---

# Phase 05 Plan 03: AeroTitleBar TitleBarButton Icon Migration Summary

Atomic restructure of `AeroTitleBar.kt`'s private `TitleBarButton` composable from `glyph: String` to `icon: ImageVector`, with simultaneous update of all three call sites (minimize, maximize/restore toggle, close). One file, one commit, compileKotlin green.

## What Was Built

Private `TitleBarButton` composable parameter signature changed from `(glyph: String, hoverColor: Color, textColor: Color, onClick)` to `(icon: ImageVector, hoverColor: Color, contentDescription: String?, onClick)`. The body replaced `Text(glyph, fontSize=13.sp)` with `Icon(imageVector=icon, Modifier.size(12.dp), tint=AeroTheme.colors.onSurface)`. All three call sites updated atomically in the same commit.

## Acceptance Criteria Results

```
# glyph param + old glyph Text + fontSize=13 in TitleBarButton — all gone:
grep -nE 'glyph.*String|Text\("(─|□|❒|✕)"|fontSize = 13' AeroTitleBar.kt
(0 hits for glyph/old-button-Text; fontSize=13.sp at line 106 is the title Text — correct)

# AeroIcons usages present:
grep -nE 'AeroIcons\.(Minus|Square|FrameCorners|X)' AeroTitleBar.kt
114: icon = AeroIcons.Minus,
121:            AeroIcons.FrameCorners
123:            AeroIcons.Square,
138: icon = AeroIcons.X,

# contentDescriptions present:
grep -nE '"(Minimize|Maximize|Restore|Close) window"' AeroTitleBar.kt
116: contentDescription = "Minimize window",
126:             "Restore window"
128:             "Maximize window",
140: contentDescription = "Close window",

# New signature parameter present:
grep -n 'icon: ImageVector' AeroTitleBar.kt
150: icon: ImageVector,

# No glyph references remaining:
grep -n 'glyph' AeroTitleBar.kt
(0 hits — pass)

# Compile:
./gradlew :library:compileKotlin
BUILD SUCCESSFUL in 4s
```

## Deviations from Plan

None — plan executed exactly as written.

Rule 3 (prior-phase pattern) applied as documented in context: explicit `internal.*` imports added alongside AeroIcons facade:
```kotlin
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.FrameCorners
import com.mordred.aero.icons.`internal`.Minus
import com.mordred.aero.icons.`internal`.Square
import com.mordred.aero.icons.`internal`.X
```
This was specified in the plan's import block — not a deviation, just the established phase pattern.

`import androidx.compose.ui.unit.sp` was retained: still required for `fontSize = 13.sp` on the title `Text` composable. The plan's conditional removal instruction was correctly applied — `.sp` remains used.

## Commit

- `f1eabf6` — `feat(05-03-MIG-04): restructure TitleBarButton(icon: ImageVector); migrate window controls to AeroIcons.Minus/Square/FrameCorners/X`

## Self-Check: PASSED
