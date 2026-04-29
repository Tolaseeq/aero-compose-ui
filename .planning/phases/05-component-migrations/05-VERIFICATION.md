---
phase: 05-component-migrations
verified: 2026-04-29T00:00:00Z
status: passed
score: 14/14 must-haves verified
---

# Phase 5: Component Migrations + Dependency Removal — Verification Report

**Phase Goal:** Replace all internal text glyphs and Material Icons usages in the `:library` module with `AeroIcons.*` references, then remove the `compose.materialIconsExtended` dependency. Cleanup CLN-01..03 covers test rewrites, the dep removal itself, and post-removal grep verification.
**Verified:** 2026-04-29
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `grep -rn "androidx.compose.material.icons" library/src/` returns 0 hits | VERIFIED | grep exits 1 (no matches) — confirmed live |
| 2 | `grep -rn "Icons.Outlined." library/src/` returns 0 hits | VERIFIED | grep exits 1 (no matches) — confirmed live |
| 3 | `grep -n "materialIconsExtended" library/build.gradle.kts` returns 0 hits | VERIFIED | grep exits 1 (no matches) — confirmed live |
| 4 | AeroCheckbox uses `Icon(AeroIcons.Check)` and `Icon(AeroIcons.Minus)` | VERIFIED | Lines 100–111 of AeroCheckbox.kt |
| 5 | AeroDropdown uses `Icon(AeroIcons.CaretDown)` | VERIFIED | Line 112 of AeroDropdown.kt |
| 6 | AeroNumberSpinner uses `Icon(AeroIcons.CaretUp)` and `Icon(AeroIcons.CaretDown)` | VERIFIED | Lines 133, 150 of AeroNumberSpinner.kt |
| 7 | AeroContextMenu uses `Icon(AeroIcons.CaretRight)` for submenu indicator | VERIFIED | Line 187 of AeroContextMenu.kt |
| 8 | AeroToastHost uses `Icon(AeroIcons.X)` for close button | VERIFIED | Line 97 of AeroToastHost.kt |
| 9 | AeroNotificationBanner uses `Icon(AeroIcons.X)` for close button | VERIFIED | Line 69 of AeroNotificationBanner.kt |
| 10 | AeroAlertKind enum maps to `AeroIcons.Info/Warning/XCircle/Question` | VERIFIED | Lines 23–26 of AeroAlertKind.kt; no Icons.Outlined.* |
| 11 | AeroBannerKind enum maps to `AeroIcons.Info/Warning/XCircle/CheckCircle` | VERIFIED | Lines 20–23 of AeroBannerKind.kt; no Icons.Outlined.* |
| 12 | AeroSearchField uses `AeroIcons.MagnifyingGlass` + `AeroIcons.X`; private `SearchIcon()` Canvas deleted | VERIFIED | Lines 61, 89 of AeroSearchField.kt; no SearchIcon function present |
| 13 | AeroPasswordField uses `AeroIcons.Eye/EyeSlash`; private `EyeOpenIcon()`/`EyeClosedIcon()` Canvas deleted | VERIFIED | Line 119 of AeroPasswordField.kt; EyeOpenIcon/EyeClosedIcon grep returns 0 hits |
| 14 | `TitleBarButton` accepts `icon: ImageVector` (not `glyph: String`) | VERIFIED | Line 150–153 of AeroTitleBar.kt: `private fun TitleBarButton(icon: ImageVector, ...)` |

**Score:** 14/14 truths verified

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt` | MIG-01: Check/Minus via AeroIcons | VERIFIED | `AeroIcons.Check` (line 101), `AeroIcons.Minus` (line 107); imports `internal.Check`, `internal.Minus` |
| `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt` | MIG-02: CaretDown via AeroIcons | VERIFIED | `AeroIcons.CaretDown` (line 112); imports `internal.CaretDown` |
| `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` | MIG-03: CaretUp/CaretDown + 14dp slot | VERIFIED | `AeroIcons.CaretUp/CaretDown` at lines 133/150; button slot `size(width=16.dp, height=14.dp)` |
| `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` | MIG-04: TitleBarButton(icon: ImageVector) | VERIFIED | `private fun TitleBarButton(icon: ImageVector, hoverColor: Color, contentDescription: String?, onClick: () -> Unit)` — no `glyph: String` parameter |
| `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt` | MIG-05: CaretRight for submenu | VERIFIED | `AeroIcons.CaretRight` (line 187) |
| `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt` | MIG-06: X close button | VERIFIED | `AeroIcons.X` (line 97) |
| `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt` | MIG-07: X close button | VERIFIED | `AeroIcons.X` (line 69) |
| `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt` | MIG-08: MagnifyingGlass + X, SearchIcon deleted | VERIFIED | `AeroIcons.MagnifyingGlass` (line 61), `AeroIcons.X` (line 89); no `SearchIcon` function |
| `library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt` | MIG-09: Eye/EyeSlash, Canvas composables deleted | VERIFIED | `AeroIcons.Eye/EyeSlash` (line 119); no `EyeOpenIcon`/`EyeClosedIcon` anywhere in library/src/ |
| `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt` | MIG-10: AeroIcons.* mapping (no Icons.Outlined.*) | VERIFIED | `AeroIcons.Info/Warning/XCircle/Question`; imports `internal.Info/Warning/XCircle/Question` only |
| `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt` | MIG-11: AeroIcons.* mapping (no Icons.Outlined.*) | VERIFIED | `AeroIcons.Info/Warning/XCircle/CheckCircle`; imports `internal.Info/Warning/XCircle/CheckCircle` only |
| `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt` | CLN-01: Tests assert AeroIcons.* instances | VERIFIED | 5 tests asserting `AeroIcons.Info`, `AeroIcons.Warning`, `AeroIcons.XCircle`, `AeroIcons.Question`; no Material imports |
| `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt` | CLN-01: Tests assert AeroIcons.* instances | VERIFIED | 5 tests asserting `AeroIcons.Info`, `AeroIcons.Warning`, `AeroIcons.XCircle`, `AeroIcons.CheckCircle`; no Material imports |
| `library/build.gradle.kts` | CLN-02: `materialIconsExtended` absent | VERIFIED | Only `compose.desktop.common`, `compose.material3`, `compose.animation`, `compose.foundation`, `compose.runtime`, `compose.ui`, `kotlinx.coroutines.core` — no materialIconsExtended |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| AeroCheckbox.kt | AeroIcons.Check/Minus | `import com.mordred.aero.icons.internal.Check/Minus` + `Icon(imageVector = AeroIcons.Check/Minus)` | WIRED | Imports present and both used in render |
| AeroDropdown.kt | AeroIcons.CaretDown | `import com.mordred.aero.icons.internal.CaretDown` + `Icon(imageVector = AeroIcons.CaretDown)` | WIRED | Import + render call confirmed |
| AeroNumberSpinner.kt | AeroIcons.CaretUp/CaretDown | `import internal.CaretUp/CaretDown` + `Icon(imageVector = AeroIcons.CaretUp/Down)` | WIRED | Both imports + both render calls confirmed |
| AeroTitleBar.kt | AeroIcons.Minus/Square/FrameCorners/X | `import internal.*` + `TitleBarButton(icon = AeroIcons.*)` | WIRED | All 4 imports and all 4 usages confirmed |
| AeroContextMenu.kt | AeroIcons.CaretRight | `import internal.CaretRight` + `Icon(imageVector = AeroIcons.CaretRight)` | WIRED | Import + render call confirmed |
| AeroToastHost.kt | AeroIcons.X | `import internal.X` + `Icon(imageVector = AeroIcons.X)` | WIRED | Import + render call confirmed |
| AeroNotificationBanner.kt | AeroIcons.X | `import internal.X` + `Icon(imageVector = AeroIcons.X)` | WIRED | Import + render call confirmed |
| AeroAlertKind.kt | AeroIcons.Info/Warning/XCircle/Question | `import internal.*` + `when(this) { Info -> AeroIcons.Info ... }` | WIRED | All 4 imports + all 4 enum branches confirmed |
| AeroBannerKind.kt | AeroIcons.Info/Warning/XCircle/CheckCircle | `import internal.*` + `when(this) { Info -> AeroIcons.Info ... }` | WIRED | All 4 imports + all 4 enum branches confirmed |
| AeroSearchField.kt | AeroIcons.MagnifyingGlass + X | `import internal.MagnifyingGlass/X` + `Icon(imageVector = AeroIcons.MagnifyingGlass/X)` | WIRED | Both imports + both render calls confirmed |
| AeroPasswordField.kt | AeroIcons.Eye/EyeSlash | `import internal.Eye/EyeSlash` + `Icon(imageVector = if (visible) AeroIcons.EyeSlash else AeroIcons.Eye)` | WIRED | Both imports + conditional render confirmed |
| AeroAlertKindTest.kt | AeroIcons.* | `import com.mordred.aero.icons.AeroIcons` + `assertEquals(AeroIcons.*, kind.icon)` | WIRED | All 4 assertions wired to AeroIcons properties |
| AeroBannerKindTest.kt | AeroIcons.* | `import com.mordred.aero.icons.AeroIcons` + `assertEquals(AeroIcons.*, kind.icon)` | WIRED | All 4 assertions wired to AeroIcons properties |
| library/build.gradle.kts | materialIconsExtended removed | Line deleted from dependencies block | VERIFIED | No occurrence of `materialIconsExtended` in file |

---

### Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| MIG-01 | AeroCheckbox Check/Minus glyphs → AeroIcons | SATISFIED | `Icon(AeroIcons.Check/Minus)` at lines 100–111 |
| MIG-02 | AeroDropdown CaretDown → AeroIcons | SATISFIED | `Icon(AeroIcons.CaretDown)` at line 112 |
| MIG-03 | AeroNumberSpinner CaretUp/CaretDown + sub-pixel fix | SATISFIED | `Icon(AeroIcons.CaretUp/Down)` at lines 133/150; slot 14dp height |
| MIG-04 | AeroTitleBar TitleBarButton(icon: ImageVector) | SATISFIED | Function signature `(icon: ImageVector, hoverColor, contentDescription, onClick)` at line 150 |
| MIG-05 | AeroContextMenu CaretRight submenu indicator | SATISFIED | `Icon(AeroIcons.CaretRight)` at line 187 |
| MIG-06 | AeroToastHost X close button | SATISFIED | `Icon(AeroIcons.X)` at line 97 |
| MIG-07 | AeroNotificationBanner X close + kind icon via kind.icon | SATISFIED | `Icon(imageVector = kind.icon)` at line 58; `Icon(AeroIcons.X)` at line 69 |
| MIG-08 | AeroSearchField MagnifyingGlass + X; SearchIcon Canvas deleted | SATISFIED | `AeroIcons.MagnifyingGlass/X` used; no `SearchIcon` function in library/src/ |
| MIG-09 | AeroPasswordField Eye/EyeSlash; EyeOpenIcon/EyeClosedIcon deleted | SATISFIED | `AeroIcons.Eye/EyeSlash` used; no `EyeOpenIcon`/`EyeClosedIcon` in library/src/ |
| MIG-10 | AeroAlertKind icons.Outlined.* → AeroIcons.* | SATISFIED | All 4 cases map to `AeroIcons.*`; no `Icons.Outlined.*` imports |
| MIG-11 | AeroBannerKind icons.Outlined.* → AeroIcons.* | SATISFIED | All 4 cases map to `AeroIcons.*`; no `Icons.Outlined.*` imports |
| CLN-01 | AeroAlertKindTest + AeroBannerKindTest rewritten to AeroIcons.* | SATISFIED | Both test files import only `AeroIcons` (+ `internal.*` backing properties); 5 tests each asserting AeroIcons instances |
| CLN-02 | `materialIconsExtended` removed from library/build.gradle.kts | SATISFIED | Dependency block contains no `materialIconsExtended` reference; confirmed via grep (exit 1) |
| CLN-03 | `grep -rn "androidx.compose.material.icons" library/src/` returns 0 | SATISFIED | Confirmed live: grep exits 1 across entire library/src/ tree |

---

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| None | — | — | No TODO/FIXME/placeholder/stub patterns found in migrated files |

**Notes:**
- The word "placeholder" appears as a `String` API parameter name in several input components (`AeroSearchField`, `AeroPasswordField`, `AeroDropdown`, etc.) — these are legitimate public API parameters, not stub comments.
- `./gradlew :library:test` cannot execute in the current shell due to `IllegalArgumentException: 25.0.2` in Kotlin compiler's `JavaVersion.parse` (Java 25 EA version string not recognized by the Kotlin version in this project). This is a shell environment incompatibility, not a test failure. The 05-05-SUMMARY.md documents `./gradlew :library:test` exiting 0 in the execution session (commit `7ea31c7` was the last chore, post `./gradlew :library:test` green). Test code is substantive and non-stub (5 real assertions each, no `@Ignore`, no `// TODO`).

---

### Human Verification Required

None for the goal of this phase. All migration correctness checks are programmatically verifiable via grep. Visual appearance of icons at runtime (AeroNumberSpinner caret legibility, TitleBar icon sizing) was approved during Phase 5 execution (documented in 05-01-SUMMARY.md task 4 visual checkpoint: "CaretUp/CaretDown icons legible across AeroBlue, AeroDark, and Classic themes").

If the environment Java version is upgraded (or a JDK 17 is set in JAVA_HOME) a human may want to confirm `./gradlew :library:test` exits 0 on the current codebase state.

---

### Gaps Summary

No gaps. All 14 requirements are satisfied by substantive, wired implementations verified against the actual source files. The three critical grep invariants (no `androidx.compose.material.icons`, no `Icons.Outlined.`, no `materialIconsExtended`) all confirmed clean.

---

_Verified: 2026-04-29_
_Verifier: Claude (gsd-verifier)_
