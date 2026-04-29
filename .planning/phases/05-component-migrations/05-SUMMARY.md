---
phase: 05-component-migrations
plan: phase-summary
subsystem: ui
tags: [compose, icons, aero, phosphor, migration, dependency-removal, material3, wave-based]

# Dependency graph
requires:
  - phase: 04-aeroicons-foundation
    provides: "138 AeroIcons extension properties; Valkyrie-generated ImageVector constants"
provides:
  - "11 library components fully migrated from Text(unicode)/Canvas/Icons.Outlined.* to Icon(AeroIcons.*)"
  - "compose.materialIconsExtended removed from :library compileClasspath"
  - "Zero androidx.compose.material.icons references across library/src/ (CLN-03 confirmed)"
  - "14 requirements closed: MIG-01..11 + CLN-01..03"
  - "All 5 ROADMAP Phase 5 success criteria satisfied"
affects: [phase-06-showcase-icons-section, STATE.md, ROADMAP.md]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Icon(AeroIcons.X, contentDescription, Modifier.size(Ndp), tint=colors.*) call pattern"
    - "Explicit internal.* import alongside AeroIcons facade (CMP 1.7.3 extension property resolution)"
    - "Decorative icon: contentDescription=null; actionable icon: English imperative literal"
    - "Tint map: filled-chip->onPrimary, affordance->labelText, chrome/close->onSurface, kind-keyed->accentFor(kind)"
    - "Wave-ordered dep removal: Waves 1+2+3 (component migrations) → Wave 4 (test gate) → Wave 5 (dep removal)"

key-files:
  created:
    - .planning/phases/05-component-migrations/05-SUMMARY.md
  modified:
    - library/build.gradle.kts
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt
    - library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt

key-decisions:
  - "AeroNumberSpinner carets at 12dp/14dp slot — visual checkpoint approved across AeroBlue + AeroDark + Classic"
  - "Explicit internal.* imports required alongside AeroIcons facade in all migration files and test files (CMP 1.7.3)"
  - "Error->AeroIcons.XCircle, Question->AeroIcons.Question (Phosphor naming, replaces Icons.Outlined.Error/HelpOutline)"
  - "AeroTitleBar TitleBarButton: textColor param removed; tint uniformly colors.onSurface; AeroIcons.Square=maximize, AeroIcons.FrameCorners=restore"
  - "Library thin JAR unchanged (0-byte delta) — material-icons-extended-desktop-1.7.3.jar (~36 MB) eliminated from compileClasspath"

requirements-completed: [MIG-01, MIG-02, MIG-03, MIG-04, MIG-05, MIG-06, MIG-07, MIG-08, MIG-09, MIG-10, MIG-11, CLN-01, CLN-02, CLN-03]

# Metrics
duration: 61min
completed: 2026-04-29
---

# Phase 5: Component Migrations + Dependency Removal — Phase Summary

**All 11 library components migrated from Text(unicode)/Canvas/Icons.Outlined.* to Icon(AeroIcons.*); `compose.materialIconsExtended` removed; 14 requirements closed across 5 plans in 5 waves (~61 min total)**

## Performance

- **Total Duration:** ~61 min
- **Completed:** 2026-04-29
- **Plans:** 5 (05-01 through 05-05)
- **Waves:** 5
- **Requirements closed:** 14 (MIG-01..11, CLN-01..03)
- **Files modified:** 14 (11 component + 2 test + 1 Gradle)
- **Files deleted content:** 3 private Canvas composables (~94 lines total)

## Plan-by-Plan Summary

| Plan | Wave | Requirements | Duration | Commits |
|------|------|--------------|----------|---------|
| 05-01 | Wave 1 — 8 component text-glyph/Icons.Outlined migrations | MIG-01..03, MIG-05..07, MIG-10..11 | 25min | 9 |
| 05-02 | Wave 2 — Canvas deletion (SearchField, PasswordField) | MIG-08, MIG-09 | 12min | 2 |
| 05-03 | Wave 3 — AeroTitleBar TitleBarButton restructure | MIG-04 | 4min | 1 |
| 05-04 | Wave 4 — Test rewrite gate (CLN-01) | CLN-01 | 5min | 2 |
| 05-05 | Wave 5 — Dep removal + verification + docs | CLN-02, CLN-03 | 15min | 1 |

## Requirements Status

| Req ID | Description | Status | Plan |
|--------|-------------|--------|------|
| MIG-01 | AeroCheckbox Check/Minus glyphs → AeroIcons | DONE | 05-01 |
| MIG-02 | AeroDropdown CaretDown glyph → AeroIcons | DONE | 05-01 |
| MIG-03 | AeroNumberSpinner CaretUp/CaretDown + slot ≥14dp | DONE | 05-01 |
| MIG-04 | AeroTitleBar TitleBarButton(glyph) → TitleBarButton(icon: ImageVector) | DONE | 05-03 |
| MIG-05 | AeroContextMenu CaretRight submenu indicator → AeroIcons | DONE | 05-01 |
| MIG-06 | AeroToastHost X close button → AeroIcons | DONE | 05-01 |
| MIG-07 | AeroNotificationBanner X close + kind icon → AeroIcons | DONE | 05-01 |
| MIG-08 | AeroSearchField — delete SearchIcon Canvas; migrate to AeroIcons.MagnifyingGlass + X | DONE | 05-02 |
| MIG-09 | AeroPasswordField — delete EyeOpenIcon/EyeClosedIcon Canvas; migrate to AeroIcons.Eye/EyeSlash | DONE | 05-02 |
| MIG-10 | AeroAlertKind icons.Outlined.* → AeroIcons.* mapping | DONE | 05-01 |
| MIG-11 | AeroBannerKind icons.Outlined.* → AeroIcons.* mapping | DONE | 05-01 |
| CLN-01 | AeroAlertKindTest + AeroBannerKindTest rewritten to assert AeroIcons.* instances | DONE | 05-04 |
| CLN-02 | `implementation(compose.materialIconsExtended)` removed from library/build.gradle.kts | DONE | 05-05 |
| CLN-03 | `grep -rn "androidx.compose.material.icons" library/src/` returns 0 hits | DONE | 05-05 |

## ROADMAP Phase 5 Success Criteria

| Criterion | Evidence | Status |
|-----------|----------|--------|
| 1. Text-glyph greps zero across library/src/ | All 5 CLN-03 sweeps: 0 hits each (material.icons, glyphs, Text("x"), Icons.Outlined, Canvas-fn names) | PASS |
| 2. AeroCheckbox/Dropdown/etc. render Icon(AeroIcons.*) | Grep for AeroIcons.Check/Minus/CaretDown/CaretUp/CaretDown/CaretRight in each file: confirmed | PASS |
| 3. SearchIcon/EyeOpenIcon/EyeClosedIcon Canvas composables deleted | grep SearchIcon\|EyeOpenIcon\|EyeClosedIcon library/src/main/ → 0 hits | PASS |
| 4. compileKotlin green WITHOUT materialIconsExtended; classpath verifies absence | ./gradlew :library:compileKotlin EXIT 0; ./gradlew :library:dependencies --configuration compileClasspath \| grep materialIcons → 0 hits | PASS |
| 5. JAR-size delta documented; AeroNumberSpinner visually confirmed in three themes | See JAR Size Delta block below; AeroNumberSpinner visual checkpoint approved (Plan 05-01) | PASS |

## JAR Size Delta

| Metric | Value |
|--------|-------|
| Pre-removal (post-Wave-4) library thin JAR | 1,007,523 bytes (~0.96 MB) |
| Post-removal (post-CLN-02) library thin JAR | 1,007,523 bytes (~0.96 MB) |
| Library thin JAR delta | 0 bytes (expected — thin JAR, no embedded deps) |
| `material-icons-extended-desktop-1.7.3.jar` removed from compileClasspath | 37,768,805 bytes (~36.02 MB) |
| ROADMAP expected delta | ~6–8 MB |
| Within expected range? | No — actual dep is ~36 MB (larger than ROADMAP estimate). The estimate underestimated the dep size. The library thin JAR itself has no delta because it never embedded the dep. The meaningful delta is the 36 MB dep no longer required on the compileClasspath. |

**Measurement note:** The `:library:jar` task produces a thin JAR of the library's own compiled classes — not a fat/shadow JAR. The `material-icons-extended` dep was always classpath-only, not embedded. This is correct Compose Desktop library design. The delta is visible at the classpath level: `./gradlew :library:dependencies --configuration compileClasspath | grep materialIcons` returns 0 hits post-removal vs. 2 lines pre-removal (direct + transitive).

## AeroNumberSpinner Visual Checkpoint

**Approved at 12dp.** User confirmed CaretUp/CaretDown icons legible across AeroBlue, AeroDark, and Classic themes including disabled state. No fallback escalation performed. (Plan 05-01, Task 4 checkpoint.)

## All Commits (Phase 5)

| Commit | Type | Plan | Description |
|--------|------|------|-------------|
| `debe49d` | feat | 05-01 | MIG-01: AeroCheckbox Check/Minus glyphs |
| `e0abe32` | feat | 05-01 | MIG-02: AeroDropdown CaretDown |
| `e0ba87f` | feat | 05-01 | MIG-03: AeroNumberSpinner CaretUp/CaretDown + slot 14dp |
| `4d4f075` | feat | 05-01 | MIG-05: AeroContextMenu CaretRight |
| `f1a164b` | feat | 05-01 | MIG-06: AeroToastHost X close |
| `c902b0d` | feat | 05-01 | MIG-07: AeroNotificationBanner X + kind icons |
| `ef3208c` | feat | 05-01 | MIG-10: AeroAlertKind mapping |
| `a228bfa` | feat | 05-01 | MIG-11: AeroBannerKind mapping |
| `38b4dd1` | docs | 05-01 | Plan 01 metadata |
| `3d7669f` | feat | 05-02 | MIG-08: AeroSearchField Canvas delete + AeroIcons.MagnifyingGlass/X |
| `d6bfb79` | feat | 05-02 | MIG-09: AeroPasswordField Canvas delete + AeroIcons.Eye/EyeSlash |
| `43cf26d` | docs | 05-02 | Plan 02 metadata |
| `f1eabf6` | feat | 05-03 | MIG-04: AeroTitleBar TitleBarButton(icon: ImageVector) |
| `c2dcb35` | docs | 05-03 | Plan 03 metadata |
| `163784a` | test | 05-04 | CLN-01a: AeroAlertKindTest rewritten to AeroIcons.* |
| `c4d19de` | test | 05-04 | CLN-01b: AeroBannerKindTest rewritten to AeroIcons.* |
| `b0cbc10` | docs | 05-04 | Plan 04 metadata |
| `7ea31c7` | chore | 05-05 | CLN-02: materialIconsExtended removed from :library |

## Open Issues / Next Phase Notes

- **AeroDropdown popup offset regression** — v1.0 known follow-up (tracked in STATE.md); not addressed in Phase 5 scope; remains for gap-closure plan
- **Phase 6 (SHW-04, SHW-05, SHW-06): Showcase IconsSection** — now fully unblocked; `AeroIcons.*` available, all deps cleaned, Phase 5 complete

---
*Phase: 05-component-migrations*
*Completed: 2026-04-29*
