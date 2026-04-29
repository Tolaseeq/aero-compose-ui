# Phase 5: Component Migrations + Dependency Removal - Research

**Researched:** 2026-04-29
**Domain:** Kotlin/Compose Desktop — mechanical icon migration; Gradle dependency removal
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- Wave order is fixed: Wave 1 (8 components, parallel) → Wave 2 (2 Canvas deletions) → Wave 3 (TitleBar private restructure) → Wave 4 (test rewrites, CLN-01 gate) → Wave 5 (dep removal + grep verification + JAR delta)
- `AeroBreadcrumb.separator: String` stays as-is in v1.1 — locked exclusion; planner must NOT touch this parameter
- `material3.Icon(imageVector, contentDescription, tint, modifier)` directly at all call sites; no `AeroIcon()` wrapper; tint always passed explicitly
- `AeroIcons.*` naming verbatim: `X` (not Close), `CaretDown` (not ChevronDown), `MagnifyingGlass` (not Search), `XCircle` (not ErrorCircle), `Question` (not Help), `FrameCorners` (not RestoreWindow)
- Lazy backing-property pattern is the icon contract — instance equality across calls is guaranteed (basis for CLN-01 test approach)
- 23-token `AeroColorScheme` is NOT extended; tints map only to existing tokens
- AeroNumberSpinner mitigation: raise inner button slot height >= 14dp + render `Icon(AeroIcons.CaretUp/CaretDown)` at `Modifier.size(12.dp)`; fallback ladder: 12dp → 14dp → 16dp
- Icon size and tint per call site: see full table in CONTEXT.md §Icon size + tint map
- contentDescription convention: decorative icons → `null`; actionable icons → inline English imperative literal
- CLN-01 assertion pattern: `assertEquals(AeroIcons.Info, AeroAlertKind.Info.icon)` — instance match, not name match
- JAR baseline captured at START of Wave 5 (after Wave 4), before removing the dep
- One plan per wave; one commit per natural step inside each plan
- No new Gradle dependency add — only removal in Wave 5
- Showcase is NOT touched in Phase 5 (Phase 6 territory)
- `library/src/main/kotlin/com/mordred/aero/icons/**` — Phase 4 territory; complete; do not modify
- `AeroColorScheme` locked at 23 tokens; no new theme tokens

### Claude's Discretion

- Exact wording of contentDescription strings beyond the verb-noun pattern
- Visual placement adjustments inside Wave-1 components when Icon footprint differs from old Text glyph
- Whether TitleBarButton private param is named `icon` or `imageVector` (prefer `icon: ImageVector`)
- Wave 4 test rewrite — keep current one-`@Test`-per-kind pattern unless unwieldy
- Verification command for JAR-size on Windows vs Unix (bash/MINGW or PowerShell)
- Order of components within Wave 1's single plan (alphabetical vs complexity)

### Deferred Ideas (OUT OF SCOPE)

- Compose UI screenshot tests
- AeroDropdown caret rotation animation
- i18n strings file / `Strings` object
- Renaming `AeroAlertKind.Question` to `AeroAlertKind.Help`
- Per-property KDoc on `AeroIcons.*` properties
- Adding new icons mid-migration
- AeroDropdown popup offset regression (tracked separately)
- AeroNumberSpinner Canvas-draw fallback (Option B — last step of fallback ladder only)
- Adding `iconAffordance`/`iconChrome`/`iconOnFilled` tokens to `AeroColorScheme`
- Localizing contentDescription via `Modifier.semantics` at consumer level
- JAR-size via `:library:dependencies` size-summing
- JAR-size delta in PROJECT.md
- Test for Material Icons import absence inside test files (redundant with CLN-03)
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| MIG-01 | AeroCheckbox: `Icon(AeroIcons.Check)` checked, `Icon(AeroIcons.Minus)` indeterminate | Lines 97-98 confirmed as Text glyphs; AeroIcons.Check and AeroIcons.Minus exist |
| MIG-02 | AeroDropdown: `Icon(AeroIcons.CaretDown)` trailing caret | Line 107-111 confirmed as `Text("▼")`; AeroIcons.CaretDown exists |
| MIG-03 | AeroNumberSpinner: CaretUp/CaretDown + sub-pixel mitigation (button >= 14dp) | Lines 129, 141 confirmed as `Text("▲"/"▼", 8sp)`; current button height = 12dp (needs raise) |
| MIG-04 | AeroTitleBar: TitleBarButton(glyph: String) → TitleBarButton(icon: ImageVector) | Lines 106-130 (callers), 136-159 (private fn) confirmed; 3 call sites: minimize/max-restore/close |
| MIG-05 | AeroContextMenu: `Icon(AeroIcons.CaretRight)` submenu indicator | Line 183 confirmed as `Text("▶")` |
| MIG-06 | AeroToastHost: `Icon(AeroIcons.X)` close button | Line 92 confirmed as `Text("✕")` inside AeroIconButton |
| MIG-07 | AeroNotificationBanner: `Icon(AeroIcons.X)` close + kind.icon path already uses Icon() | Line 65 confirmed as `Text("✕")`; line 55 already uses `Icon(imageVector = kind.icon, ...)` |
| MIG-08 | AeroSearchField: delete SearchIcon() Canvas; Icon(AeroIcons.MagnifyingGlass) + Icon(AeroIcons.X) | Lines 61 (SearchIcon() call), 121 (Text("x") clear) confirmed; Canvas fn at lines 81-111 |
| MIG-09 | AeroPasswordField: delete EyeOpenIcon()/EyeClosedIcon() Canvas; Icon(AeroIcons.Eye)/Icon(AeroIcons.EyeSlash) | Lines 121-124 call sites confirmed; Canvas fns at lines 136-204 |
| MIG-10 | AeroAlertKind: replace Icons.Outlined.* with AeroIcons.* in `icon` property | Lines 3-7 imports, lines 23-26 mapping confirmed; Info/Warning/Error/Question |
| MIG-11 | AeroBannerKind: replace Icons.Outlined.* with AeroIcons.* in `icon` property | Lines 3-7 imports, lines 20-23 mapping confirmed; Info/Warning/Error/Success |
| CLN-01 | Rewrite AeroAlertKindTest.kt + AeroBannerKindTest.kt to assert AeroIcons.* | Both test files confirmed; 4 @Test methods each; drop material.icons imports, add AeroIcons |
| CLN-02 | Remove `implementation(compose.materialIconsExtended)` from library/build.gradle.kts | Line 15 confirmed as `implementation(compose.materialIconsExtended)` |
| CLN-03 | grep -rn "androidx.compose.material.icons" library/src/ returns 0 hits | Current grep shows exactly 4 files to clean: 2 production + 2 test |
</phase_requirements>

---

## Summary

Phase 5 is a mechanical migration with zero new API design. All 14 requirements boil down to: replace text characters and Canvas composables with `material3.Icon(AeroIcons.*)` calls, rewrite two test files to assert `AeroIcons.*` instances, and delete one Gradle line. The AeroIcons set (138 icons, Phase 4 deliverable) is complete and confirmed — all 17 icons needed by Phase 5 exist as extension properties in `library/src/main/kotlin/com/mordred/aero/icons/internal/`.

The research confirmed exact file paths, line numbers, and current implementations for all 11 component migration targets. Every current glyph usage is either a `Text(unicodeChar)` call, a Canvas private composable, or a `Icons.Outlined.*` property reference. None use any non-standard pattern that complicates replacement. The only structural change beyond simple substitution is MIG-04 (TitleBarButton signature change from `glyph: String` to `icon: ImageVector`) and MIG-03 (button slot height raise from current 12dp to >= 14dp).

The Material Icons dependency (`compose.materialIconsExtended`) is used in exactly 4 files: `AeroAlertKind.kt`, `AeroBannerKind.kt`, `AeroAlertKindTest.kt`, `AeroBannerKindTest.kt` — all confirmed by grep. No other production or test source imports `androidx.compose.material.icons`. CLN-02 removes one line from `library/build.gradle.kts` (line 15), and the gate for removal is Wave 4 completing successfully.

**Primary recommendation:** Execute waves strictly in order; each wave is a commit batch inside its plan. Do not attempt dep removal (Wave 5) until CLN-01 test compilation is verified green in Wave 4.

---

## Standard Stack

### Core (no additions required)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.material3.Icon` | CMP 1.7.3 (via `compose.material3`) | Renders `ImageVector` at call site | Already on classpath; no new dep |
| `AeroIcons.*` | Phase 4 deliverable (138 icons) | All migration targets | Extension properties in `internal/*.kt` |
| `androidx.compose.foundation.layout.Modifier.size(Dp)` | CMP 1.7.3 | Per-call-site icon sizing | Standard layout modifier |
| `androidx.compose.ui.graphics.vector.ImageVector` | CMP 1.7.3 | Type for TitleBarButton param | Already imported wherever Icon() is used |

No new Gradle dependencies are added in Phase 5. Only `implementation(compose.materialIconsExtended)` is removed from `library/build.gradle.kts` line 15.

### Confirmed Existing Imports (add at each migrated file)

```kotlin
import androidx.compose.material3.Icon
import com.mordred.aero.icons.AeroIcons
// for MIG-04 only:
import androidx.compose.ui.graphics.vector.ImageVector
```

---

## Architecture Patterns

### Call-Site Replacement Pattern (all Wave 1/2/3 sites)

Replace `Text(unicodeChar, ...)` with `Icon(AeroIcons.Name, contentDescription, Modifier.size(Xdp), tint)`:

```kotlin
// BEFORE (MIG-01 checked example)
Text("✓", color = colors.onPrimary, fontSize = 11.sp)

// AFTER
Icon(
    imageVector = AeroIcons.Check,
    contentDescription = null,           // decorative — state conveyed by ToggleableState
    modifier = Modifier.size(12.dp),
    tint = colors.onPrimary
)
```

### Wave 1 — 8 Components (Internal-Only, No API Impact)

All 8 components are in-place text-glyph replacements. No public signature changes.

#### MIG-01: AeroCheckbox.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt`

Current (lines 97-98):
```kotlin
ToggleableState.On -> Text("✓", color = colors.onPrimary, fontSize = 11.sp)
ToggleableState.Indeterminate -> Text("–", color = colors.onPrimary, fontSize = 11.sp)
```

Target:
```kotlin
ToggleableState.On -> Icon(
    imageVector = AeroIcons.Check,
    contentDescription = null,
    modifier = Modifier.size(12.dp),
    tint = colors.onPrimary
)
ToggleableState.Indeterminate -> Icon(
    imageVector = AeroIcons.Minus,
    contentDescription = null,
    modifier = Modifier.size(12.dp),
    tint = colors.onPrimary
)
```

Imports to add: `androidx.compose.material3.Icon`, `com.mordred.aero.icons.AeroIcons`
Imports to remove: `androidx.compose.material3.Text` is still used (line 111 — label text), keep it.
Also remove: `androidx.compose.ui.unit.sp` if only used for the old fontSize=11.sp (verify no other sp usage).

**Acceptance grep:** `grep -n 'Text("✓"\|Text("–"' AeroCheckbox.kt` → 0 hits

#### MIG-02: AeroDropdown.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt`

Current (lines 107-111):
```kotlin
Text(
    text = "▼",
    color = colors.labelText,
    style = AeroTheme.typography.label
)
```

Target:
```kotlin
Icon(
    imageVector = AeroIcons.CaretDown,
    contentDescription = null,           // decorative — field label conveys meaning
    modifier = Modifier.size(14.dp),
    tint = colors.labelText
)
```

Imports to add: `androidx.compose.material3.Icon`, `com.mordred.aero.icons.AeroIcons`, `androidx.compose.ui.unit.dp` (already present)
Note: `Text` import stays — used for label/placeholder display.

**Acceptance grep:** `grep -n 'Text("▼"' AeroDropdown.kt` → 0 hits

#### MIG-03: AeroNumberSpinner.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt`

Current (lines 122, 134 — `Box` sizes) and (lines 129, 141 — `Text` glyphs):
```kotlin
// Up button Box:
.size(width = 16.dp, height = 12.dp)      // height = 12dp < 14dp minimum
// Up glyph:
Text("▲", fontSize = 8.sp, color = colors.onSurface)

// Down button Box:
.size(width = 16.dp, height = 12.dp)      // height = 12dp < 14dp minimum
// Down glyph:
Text("▼", fontSize = 8.sp, color = colors.onSurface)
```

Target (mitigation A — locked decision):
```kotlin
// Up button Box: raise height to 14dp
.size(width = 16.dp, height = 14.dp)
// Up icon:
Icon(
    imageVector = AeroIcons.CaretUp,
    contentDescription = null,
    modifier = Modifier.size(12.dp),
    tint = colors.onSurface
)

// Down button Box: raise height to 14dp
.size(width = 16.dp, height = 14.dp)
// Down icon:
Icon(
    imageVector = AeroIcons.CaretDown,
    contentDescription = null,
    modifier = Modifier.size(12.dp),
    tint = colors.onSurface
)
```

**Sub-pixel analysis:** At 12dp render, Phosphor stroke = 12×(16/256) ≈ 0.75dp at 96 DPI — above the ~0.5dp collapse threshold. The 14dp button slot guarantees the icon is not clipped. Fallback ladder: 12dp → 14dp → 16dp (size steps); escalate only if visual checkpoint fails.

**Inline visual checkpoint** (part of MIG-03 plan): user runs showcase, eyes spinner up/down icons in AeroDark + AeroBlue + Classic (all three themes), confirms no sub-pixel collapse, types `approved` or describes issue.

**Acceptance greps (all 3 required):**
1. `grep -n 'Text("▲"\|Text("▼"' AeroNumberSpinner.kt` → 0 hits
2. `grep -n 'Modifier.size(12.dp)' AeroNumberSpinner.kt` → >= 2 hits
3. Visual checkpoint: `approved` in AeroDark

#### MIG-05: AeroContextMenu.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt`

Current (line 183):
```kotlin
Text("▶", color = colors.labelText, style = AeroTheme.typography.label)
```

Target:
```kotlin
Icon(
    imageVector = AeroIcons.CaretRight,
    contentDescription = null,           // decorative — item label provides meaning
    modifier = Modifier.size(12.dp),
    tint = colors.labelText
)
```

Note: `material3.Icon` is already imported (line 15). Add `com.mordred.aero.icons.AeroIcons` import.

**Acceptance grep:** `grep -n 'Text("▶"' AeroContextMenu.kt` → 0 hits

#### MIG-06: AeroToastHost.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt`

Current (line 92, inside `AeroIconButton`):
```kotlin
AeroIconButton(onClick = onDismiss, size = 24.dp) {
    Text("✕", color = colors.onSurface)
}
```

Target:
```kotlin
AeroIconButton(onClick = onDismiss, size = 24.dp) {
    Icon(
        imageVector = AeroIcons.X,
        contentDescription = "Close toast",
        modifier = Modifier.size(14.dp),
        tint = colors.onSurface
    )
}
```

Imports to add: `androidx.compose.material3.Icon`, `com.mordred.aero.icons.AeroIcons`, `androidx.compose.foundation.layout.size` (likely already present via other layout imports), `androidx.compose.ui.unit.dp` (already present).
Remove: `androidx.compose.material3.Text` only if no other Text usage — verify (there IS a `Text` for `data.message` at line 77, so keep `Text` import).

**Acceptance grep:** `grep -n 'Text("✕"' AeroToastHost.kt` → 0 hits

#### MIG-07: AeroNotificationBanner.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt`

**Two sites:**

Site 1 — close button (line 65):
```kotlin
// BEFORE
AeroIconButton(onClick = onDismiss, size = 24.dp) {
    Text("✕", color = colors.onSurface)
}
// AFTER
AeroIconButton(onClick = onDismiss, size = 24.dp) {
    Icon(
        imageVector = AeroIcons.X,
        contentDescription = "Close notification",
        modifier = Modifier.size(14.dp),
        tint = colors.onSurface
    )
}
```

Site 2 — kind icon (line 55) — already uses `Icon(imageVector = kind.icon, ...)`. This site requires NO code change at the AeroNotificationBanner level. The kind icon migration happens in MIG-11 (AeroBannerKind.kt). The call site at line 55 is already correctly structured — it will automatically pick up the new `AeroIcons.*` reference once AeroBannerKind.kt is migrated.

`material3.Icon` is already imported (line 10). Add `com.mordred.aero.icons.AeroIcons` import.

**Acceptance grep:** `grep -n 'Text("✕"' AeroNotificationBanner.kt` → 0 hits

#### MIG-10: AeroAlertKind.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt`

Current (lines 3-7 and 22-27):
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
// ...
public val icon: ImageVector
    get() = when (this) {
        Info     -> Icons.Outlined.Info
        Warning  -> Icons.Outlined.Warning
        Error    -> Icons.Outlined.Error
        Question -> Icons.Outlined.HelpOutline
    }
```

Target:
```kotlin
import com.mordred.aero.icons.AeroIcons
// (Icons.* imports removed; ImageVector import stays)
// ...
public val icon: ImageVector
    get() = when (this) {
        Info     -> AeroIcons.Info
        Warning  -> AeroIcons.Warning
        Error    -> AeroIcons.XCircle
        Question -> AeroIcons.Question
    }
```

Note: The lazy backing-property pattern means `AeroIcons.Info` is cached after first access — instance equality holds. The `AeroAlertDialog.kt` consumer at lines 53-57 uses `kind.icon` directly; no change needed there.

**Acceptance grep:** `grep -n 'Icons.Outlined' AeroAlertKind.kt` → 0 hits

#### MIG-11: AeroBannerKind.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt`

Current (lines 3-7 and 19-24):
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
// ...
public val icon: ImageVector
    get() = when (this) {
        Info     -> Icons.Outlined.Info
        Warning  -> Icons.Outlined.Warning
        Error    -> Icons.Outlined.Error
        Success  -> Icons.Outlined.CheckCircle
    }
```

Target:
```kotlin
import com.mordred.aero.icons.AeroIcons
// (Icons.* imports removed; ImageVector import stays)
// ...
public val icon: ImageVector
    get() = when (this) {
        Info     -> AeroIcons.Info
        Warning  -> AeroIcons.Warning
        Error    -> AeroIcons.XCircle
        Success  -> AeroIcons.CheckCircle
    }
```

**Acceptance grep:** `grep -n 'Icons.Outlined' AeroBannerKind.kt` → 0 hits

---

### Wave 2 — Canvas Composable Deletions

#### MIG-08: AeroSearchField.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt`

**Delete:** Private function `SearchIcon()` (lines 80-111) — entire Canvas composable

**Replace call site (line 61):** `SearchIcon()` → `Icon(AeroIcons.MagnifyingGlass, contentDescription = null, modifier = Modifier.size(14.dp), tint = AeroTheme.colors.labelText)`

Note: The SearchIcon is called inside a `Box` with `contentAlignment = Alignment.Center`. The new `Icon()` composable drops in directly in the same position.

**Replace ClearButton text (line 121):**
```kotlin
// BEFORE (inside ClearButton composable)
Text("x", color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyLarge)
// AFTER
Icon(
    imageVector = AeroIcons.X,
    contentDescription = "Clear search",
    modifier = Modifier.size(14.dp),
    tint = AeroTheme.colors.labelText
)
```

**Imports to remove:** `androidx.compose.foundation.Canvas`, `androidx.compose.ui.geometry.Offset`, `androidx.compose.ui.graphics.StrokeCap`, `androidx.compose.ui.graphics.drawscope.Stroke`
**Imports to add:** `androidx.compose.material3.Icon`, `com.mordred.aero.icons.AeroIcons`
**Imports to verify/keep:** `androidx.compose.ui.unit.dp`, `androidx.compose.ui.Modifier` (already present)
**Imports to remove:** `androidx.compose.material3.Text` (only used in ClearButton for old `Text("x")`) — verify no other Text usage in file; `AeroTheme.typography` ref was only in old `Text` — check KDoc; the KDoc at line 22 says "drawn via Canvas" — update KDoc too.

**Acceptance greps:**
- `grep -n 'SearchIcon\|Canvas\|StrokeCap\|drawscope' AeroSearchField.kt` → 0 hits
- `grep -n 'Text("x"' AeroSearchField.kt` → 0 hits

#### MIG-09: AeroPasswordField.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt`

**Delete:** Private function `EyeOpenIcon(tint: Color)` (lines 135-163) — entire Canvas composable
**Delete:** Private function `EyeClosedIcon(tint: Color)` (lines 170-205) — entire Canvas composable

**Replace call sites (lines 120-124):**
```kotlin
// BEFORE
if (visible) {
    EyeOpenIcon(tint = colors.labelText)
} else {
    EyeClosedIcon(tint = colors.labelText)
}
// AFTER
Icon(
    imageVector = if (visible) AeroIcons.EyeSlash else AeroIcons.Eye,
    contentDescription = if (visible) "Hide password" else "Show password",
    modifier = Modifier.size(14.dp),
    tint = colors.labelText
)
```

Note on contentDescription: `if (visible)` means password is currently SHOWN → action is "Hide password". `else` means password is currently MASKED → action is "Show password". This matches the locked convention: contentDescription announces the action, not the current state.

**Imports to remove:** `androidx.compose.foundation.Canvas`, `androidx.compose.ui.geometry.Offset`, `androidx.compose.ui.geometry.Rect`, `androidx.compose.ui.graphics.Path`, `androidx.compose.ui.graphics.StrokeCap`, `androidx.compose.ui.graphics.drawscope.Stroke`
**Imports to add:** `androidx.compose.material3.Icon`, `com.mordred.aero.icons.AeroIcons`

**Acceptance greps:**
- `grep -n 'EyeOpenIcon\|EyeClosedIcon\|Canvas\|drawPath\|drawCircle\|drawLine' AeroPasswordField.kt` → 0 hits

---

### Wave 3 — TitleBar Private Restructure

#### MIG-04: AeroTitleBar.kt

**File:** `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt`

**Private function signature change (lines 136-159):**
```kotlin
// BEFORE
@Composable
private fun TitleBarButton(
    glyph: String,
    hoverColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    // ...
    Text(
        text = glyph,
        color = textColor,
        fontSize = 13.sp
    )
}

// AFTER
@Composable
private fun TitleBarButton(
    icon: ImageVector,
    hoverColor: Color,
    contentDescription: String?,
    onClick: () -> Unit
) {
    // ...
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(12.dp),
        tint = AeroTheme.colors.onSurface
    )
}
```

Note: The `textColor: Color` parameter is removed because tint comes from `colors.onSurface` directly (no per-button color variation for icon tint — the close button's hover color is `colors.closeButtonHover` for the background, not the icon color). The icon tint is uniformly `colors.onSurface` for all three window control buttons.

**Three call sites (lines 106-129) — updated:**

```kotlin
// Minimize
TitleBarButton(
    icon = AeroIcons.Minus,
    hoverColor = colors.buttonHover,
    contentDescription = "Minimize window",
    onClick = { windowState.isMinimized = true }
)
// Maximize / Restore
TitleBarButton(
    icon = if (windowState.placement == WindowPlacement.Maximized)
               AeroIcons.FrameCorners
           else
               AeroIcons.Square,
    hoverColor = colors.buttonHover,
    contentDescription = if (windowState.placement == WindowPlacement.Maximized)
                             "Restore window"
                         else
                             "Maximize window",
    onClick = {
        windowState.placement =
            if (windowState.placement == WindowPlacement.Maximized)
                WindowPlacement.Floating
            else
                WindowPlacement.Maximized
    }
)
// Close
TitleBarButton(
    icon = AeroIcons.X,
    hoverColor = colors.closeButtonHover,
    contentDescription = "Close window",
    onClick = onCloseRequest
)
```

**Imports to add:** `androidx.compose.material3.Icon`, `com.mordred.aero.icons.AeroIcons`, `androidx.compose.ui.graphics.vector.ImageVector`
**Imports to remove:** `androidx.compose.ui.unit.sp` (only used in old `Text(fontSize=13.sp)`); `androidx.compose.material3.Text` (if no other Text usage — the main title `Text` at line 96 uses it; keep `Text` import)

**Acceptance greps:**
- `grep -n 'glyph.*String\|Text("─"\|Text("□"\|Text("❒"\|Text("✕"' AeroTitleBar.kt` → 0 hits
- `grep -n 'AeroIcons.Minus\|AeroIcons.Square\|AeroIcons.FrameCorners\|AeroIcons.X' AeroTitleBar.kt` → 3 hits (each icon used)

---

### Wave 4 — Test Rewrites (CLN-01 Gate)

#### AeroAlertKindTest.kt rewrite

**File:** `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt`

**Imports: drop all `androidx.compose.material.icons.*`, add `com.mordred.aero.icons.AeroIcons`**

**Test method changes (4 methods, keep identical structure):**
```kotlin
// BEFORE
@Test
fun infoMapsToInfoIcon() {
    assertEquals(Icons.Outlined.Info, AeroAlertKind.Info.icon)
}

// AFTER
@Test
fun infoMapsToInfoIcon() {
    assertEquals(AeroIcons.Info, AeroAlertKind.Info.icon)
}
```

Full mapping:
- `Icons.Outlined.Info` → `AeroIcons.Info`
- `Icons.Outlined.Warning` → `AeroIcons.Warning`
- `Icons.Outlined.Error` → `AeroIcons.XCircle`
- `Icons.Outlined.HelpOutline` → `AeroIcons.Question`

The `definesExactlyFourKinds()` test is unchanged — it only checks enum names.

**Instance equality rationale:** `AeroIcons.Info` uses the lazy backing-property pattern (`private var _Info: ImageVector? = null; public val Info: ImageVector get() = _Info ?: loadInfo().also { _Info = it }`). On first call within a JVM process, it loads the vector and caches it. On subsequent calls (including the test assertion), it returns the same cached instance. `assertEquals` uses `.equals()` which for `ImageVector` is structural equality — but because the lazy pattern returns the same cached instance on every access, both `AeroIcons.Info` in the test and `AeroAlertKind.Info.icon` (which returns `AeroIcons.Info`) will be the same object. The assertion is stable.

#### AeroBannerKindTest.kt rewrite

**File:** `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt`

Same pattern:
- `Icons.Outlined.Info` → `AeroIcons.Info`
- `Icons.Outlined.Warning` → `AeroIcons.Warning`
- `Icons.Outlined.Error` → `AeroIcons.XCircle`
- `Icons.Outlined.CheckCircle` → `AeroIcons.CheckCircle`

**Compile gate:** after Wave 4, run `./gradlew :library:test` to confirm both test files compile and pass with no material.icons imports.

---

### Wave 5 — Dependency Removal (CLN-02 + CLN-03)

#### CLN-02: library/build.gradle.kts

**File:** `library/build.gradle.kts`

**Remove line 15 exactly:**
```kotlin
implementation(compose.materialIconsExtended)
```

No other changes to `library/build.gradle.kts`. `compose.material3` stays (provides `material3.Icon()`).

**Verification after removal:**
```bash
./gradlew :library:compileKotlin
./gradlew :library:dependencies --configuration compileClasspath | grep -i materialIcons
```
Both must succeed with 0 `materialIcons` lines.

#### CLN-03: Grep Verification

**Final zero-hit grep (run after Wave 5 lands):**
```bash
grep -rn "androidx.compose.material.icons" library/src/
```
Must return 0 results.

**Additional success criterion grep (from ROADMAP):**
```bash
grep -rn 'Text("▲\|▼\|▶\|✕\|✓\|─\|□\|❒\|–")' library/src/
grep -rn 'Text("x"' library/src/
```
Both must return 0 results.

#### JAR-size measurement

**Baseline command (before Wave 5, on post-Wave-4 tree):**
```bash
./gradlew :library:jar
ls -l library/build/libs/library-*.jar
```
Or Windows: `dir library\build\libs\library-*.jar`

**Post-removal (after CLN-02 commits):**
```bash
./gradlew :library:jar
ls -l library/build/libs/library-*.jar
```

**Expected delta:** ~6–8 MB reduction.

**Where results are recorded:**
1. `05-SUMMARY.md` — pre-bytes, post-bytes, delta-bytes, delta-MB
2. `STATE.md` `Performance Metrics` block — single line entry

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Icon rendering | Custom Canvas draw (EyeOpenIcon, SearchIcon style) | `material3.Icon(AeroIcons.*)` | Tint, accessibility, size, color filter all handled by Icon(); Canvas needed no advantages here |
| Icon storage | Eager `val` at object level | Lazy backing-property (Phase 4 pattern) | 138 eager ImageVectors cause measurable startup spike; pattern already established |
| Instance comparison in tests | `.name` string comparison | Direct `assertEquals(AeroIcons.X, kind.icon)` | Lazy pattern guarantees same cached instance; name comparison adds indirection for no gain |
| Dep removal verification | Runtime classloading check | Gradle `dependencies` task + grep | Deterministic, CI-safe, no classpath manipulation needed |

**Key insight:** The entire phase is anti-hand-roll. The AeroIcons set exists (Phase 4). The only work is deletion of hand-rolled Canvas composables and replacement with `Icon()`. The fewer custom implementations, the better.

---

## Common Pitfalls

### Pitfall 1: Removing sp import while Text() still present

**What goes wrong:** After migrating glyphs, the `import androidx.compose.ui.unit.sp` line is removed. But `Text(label, ...)` usages in the same file don't use sp — they use `AeroTheme.typography.bodyLarge`. However, in AeroNumberSpinner.kt the old `fontSize = 8.sp` is the only sp usage; removing the sp import is safe there. In AeroCheckbox.kt, the old `fontSize = 11.sp` is also the only sp usage.

**How to avoid:** After migration, grep each file for `.sp` usage. Only remove `import androidx.compose.ui.unit.sp` if 0 hits remain.

### Pitfall 2: Forgetting to update KDoc when removing Canvas fns

**What goes wrong:** AeroSearchField.kt KDoc at line 22-23 says "drawn via Canvas". After deleting SearchIcon(), the KDoc is stale.

**How to avoid:** Update KDoc in the same commit as the migration. AeroPasswordField.kt lines 47-51 have similar Canvas-specific wording.

### Pitfall 3: TitleBarButton textColor parameter orphaned at call sites

**What goes wrong:** The old signature had `textColor: Color`. If call sites are partially updated (textColor removed from fn but still passed at call sites, or vice versa), compilation fails.

**How to avoid:** Change the private function signature and ALL THREE call sites atomically in a single edit. The function is `private` so no external callers exist.

### Pitfall 4: Wave 5 attempted before Wave 4 test compilation

**What goes wrong:** If `compose.materialIconsExtended` is removed while `AeroAlertKindTest.kt` still imports `Icons.Outlined.*`, the test module fails to compile.

**How to avoid:** The wave ordering enforces this: Wave 4 must produce a `./gradlew :library:test` green result before Wave 5 begins.

### Pitfall 5: AeroNotificationBanner.kt close button vs kind icon confusion

**What goes wrong:** MIG-07 has TWO sites. The kind icon at line 55 (`Icon(imageVector = kind.icon, ...)`) requires NO code change in AeroNotificationBanner.kt itself — it automatically uses the new AeroIcons reference once AeroBannerKind.kt (MIG-11) is migrated. Only the close button `Text("✕")` at line 65 needs changing in this file.

**How to avoid:** Document both sites in the plan but make clear line 55 is passively updated via MIG-11.

### Pitfall 6: AeroAlertDialog.kt at lines 53-57 is NOT a migration target

**What goes wrong:** AeroAlertDialog.kt at line 53-57 has `Icon(imageVector = kind.icon, ...)`. This already uses the correct pattern. It should NOT be modified in Phase 5.

**How to avoid:** Research confirms this file is a consumer, not a producer of icon references. It stays unchanged.

### Pitfall 7: Sub-pixel regression at AeroNumberSpinner during visual checkpoint

**What goes wrong:** Even with 12dp Icon + 14dp button, a specific theme or scaling factor causes the caret to render as a hairline.

**How to avoid:** The fallback ladder (12dp → 14dp → 16dp for Modifier.size) is documented in the plan upfront. If the inline visual checkpoint fails at 12dp, escalate to 14dp without requiring a new plan.

---

## Code Examples

### Material3 Icon() signature reference

```kotlin
// Source: androidx.compose.material3.Icon (CMP 1.7.3)
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
)
// Default size: 24.dp when no Modifier.size is applied
// tint MUST be passed explicitly in library code (AeroIcons KDoc mandate)
```

### AeroIcons lazy backing-property (confirmed pattern, Phase 4)

```kotlin
// In AeroIcons extension property (e.g. internal/X.kt)
private var _X: ImageVector? = null

public val AeroIcons.X: ImageVector
    get() = _X ?: materialIcon(name = "X") {
        // ...path data...
    }.also { _X = it }
```

### CLN-01 test assertion pattern

```kotlin
// AeroAlertKindTest.kt after rewrite
import com.mordred.aero.icons.AeroIcons
// ...
@Test
fun infoMapsToInfoIcon() {
    assertEquals(AeroIcons.Info, AeroAlertKind.Info.icon)
}
```

### TitleBarButton after Wave 3

```kotlin
@Composable
private fun TitleBarButton(
    icon: ImageVector,
    hoverColor: Color,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 32.dp)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .background(if (hovered) hoverColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(12.dp),
            tint = AeroTheme.colors.onSurface
        )
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Text("✓")` for checkbox | `Icon(AeroIcons.Check)` | Phase 5 | Consistent vector rendering; accessibility-correct; no font fallback risk |
| `Canvas { drawCircle/drawLine }` for SearchIcon | `Icon(AeroIcons.MagnifyingGlass)` | Phase 5 | 80+ lines → 5 lines; tint/size handled by M3 Icon |
| `Icons.Outlined.*` (Material Extended) | `AeroIcons.*` (Phosphor Regular) | Phase 5 | Removes 6-8 MB dep; consistent rounded-stroke aesthetic |
| `TitleBarButton(glyph: String)` | `TitleBarButton(icon: ImageVector)` | Phase 5 | Type-safe; no font-rendering ambiguity |

---

## Open Questions

1. **AeroNumberSpinner total height after 14dp button slot**
   - What we know: current button slot is 12dp; the overall spinner height at 28dp field is OK because the buttons are in a Column with 2dp spacer between them (total column height = 12+2+12=26dp, fitting within 28dp field height)
   - What changes: raising to 14dp gives column = 14+2+14=30dp, potentially exceeding the 28dp field height
   - Recommendation: The planner should verify that increasing button height to 14dp doesn't cause layout overflow. If it does, the outer `Row` should have `Alignment.CenterVertically` (already set) and the button column height naturally exceeds the field — this is visually acceptable as the buttons extend slightly. Alternatively, the spacer can be reduced to 0dp or the field height can be raised to 32dp. The CONTEXT.md decision says "Spinner total height grows from current ~32dp to ~32dp (button slot was already ~16dp; 14dp is the minimum guarantee, not an increase from current)" — this suggests the current 12dp buttons in a 32dp outer height was the design intent and 14dp still fits.

2. **`Modifier.size` import path**
   - What we know: `Modifier.size(Dp)` is in `androidx.compose.foundation.layout`
   - What's likely: most files already import `androidx.compose.foundation.layout.*` or specific size imports. Each migrated file must confirm the size import is present.
   - Recommendation: check existing imports in each file; `size` is used in AeroCheckbox.kt line 85 (`Modifier.size(16.dp)`) and in AeroToastHost.kt line 92 (`size = 24.dp`) so the layout imports are present.

---

## Validation Architecture

> `workflow.nyquist_validation` is `true` in `.planning/config.json` — this section is required.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter (JUnit 5) via `libs.junit.jupiter` |
| Config file | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| Quick run command | `./gradlew :library:test` |
| Full suite command | `./gradlew :library:test` (same — all tests are fast unit tests) |

### Phase Requirements → Validation Map

| ID | Behavior | Validation Type | Command / Check | Wave |
|----|----------|-----------------|-----------------|------|
| MIG-01 | AeroCheckbox uses `Icon(AeroIcons.Check/Minus)` | grep | `grep -n 'Text("✓"\|Text("–"' AeroCheckbox.kt` → 0 | Wave 1 commit |
| MIG-02 | AeroDropdown uses `Icon(AeroIcons.CaretDown)` | grep | `grep -n 'Text("▼"' AeroDropdown.kt` → 0 | Wave 1 commit |
| MIG-03 | AeroNumberSpinner uses CaretUp/CaretDown at 12dp; button >= 14dp | grep + visual checkpoint | `grep -n 'Text("▲"\|Text("▼"' AeroNumberSpinner.kt` → 0; `grep -n 'size(12.dp)' AeroNumberSpinner.kt` >= 2; visual: user approves in AeroDark | Wave 1 commit + inline checkpoint |
| MIG-04 | TitleBarButton(icon: ImageVector) wired; 3 call sites use AeroIcons.* | compile + grep | `./gradlew :library:compileKotlin`; `grep -n 'glyph.*String\|Text("─"\|Text("□"\|Text("❒"\|Text("✕"' AeroTitleBar.kt` → 0 | Wave 3 commit |
| MIG-05 | AeroContextMenu submenu indicator uses `Icon(AeroIcons.CaretRight)` | grep | `grep -n 'Text("▶"' AeroContextMenu.kt` → 0 | Wave 1 commit |
| MIG-06 | AeroToastHost close uses `Icon(AeroIcons.X)` | grep | `grep -n 'Text("✕"' AeroToastHost.kt` → 0 | Wave 1 commit |
| MIG-07 | AeroNotificationBanner close uses `Icon(AeroIcons.X)` | grep | `grep -n 'Text("✕"' AeroNotificationBanner.kt` → 0 | Wave 1 commit |
| MIG-08 | SearchIcon() Canvas deleted; MagnifyingGlass + X used | grep + compile | `grep -n 'SearchIcon\|Canvas\|Text("x"' AeroSearchField.kt` → 0; `./gradlew :library:compileKotlin` | Wave 2 commit |
| MIG-09 | EyeOpenIcon/EyeClosedIcon Canvas deleted; Eye/EyeSlash used | grep + compile | `grep -n 'EyeOpenIcon\|EyeClosedIcon\|Canvas\|drawPath' AeroPasswordField.kt` → 0; compile | Wave 2 commit |
| MIG-10 | AeroAlertKind.icon returns AeroIcons.* | grep + compile | `grep -n 'Icons.Outlined' AeroAlertKind.kt` → 0; `./gradlew :library:compileKotlin` | Wave 1 commit |
| MIG-11 | AeroBannerKind.icon returns AeroIcons.* | grep + compile | `grep -n 'Icons.Outlined' AeroBannerKind.kt` → 0; compile | Wave 1 commit |
| CLN-01 | AeroAlertKindTest + AeroBannerKindTest assert AeroIcons.* | compile + test run | `./gradlew :library:test` — both test classes pass | Wave 4 (required before Wave 5) |
| CLN-02 | `materialIconsExtended` removed from build.gradle.kts | compile + dep check | `./gradlew :library:compileKotlin`; `./gradlew :library:dependencies --configuration compileClasspath \| grep -i materialIcons` → 0 lines | Wave 5 |
| CLN-03 | Zero `androidx.compose.material.icons` refs in library/src/ | grep | `grep -rn "androidx.compose.material.icons" library/src/` → 0 hits | Wave 5 final verification |

### Sampling Rate

- **Per Wave commit (Wave 1-3):** Per-file grep from table above
- **Per Wave merge (Wave 4):** `./gradlew :library:test` — full test suite green
- **Wave 5 gate:** `./gradlew :library:compileKotlin` + CLN-03 grep + JAR size measurement
- **Phase gate:** All greps 0-hit, test suite green, JAR delta documented in 05-SUMMARY.md

### Wave 0 Gaps

None — existing test infrastructure covers CLN-01. No new test files need to be created; the two existing test files are rewritten in-place during Wave 4.

---

## Sources

### Primary (HIGH confidence)

- Direct source file reads — `AeroCheckbox.kt`, `AeroDropdown.kt`, `AeroNumberSpinner.kt`, `AeroSearchField.kt`, `AeroPasswordField.kt`, `AeroTitleBar.kt`, `AeroContextMenu.kt`, `AeroToastHost.kt`, `AeroNotificationBanner.kt`, `AeroAlertKind.kt`, `AeroBannerKind.kt`, `AeroAlertDialog.kt`, `AeroAlertKindTest.kt`, `AeroBannerKindTest.kt`, `library/build.gradle.kts`
- `ls library/src/main/kotlin/com/mordred/aero/icons/internal/` — all 17 Phase 5 icons confirmed present
- `grep -rn "androidx.compose.material.icons" library/src/` — exhaustive Material Icons usage map (exactly 4 files)
- `.planning/phases/05-component-migrations/05-CONTEXT.md` — all locked decisions, tint map, contentDescription convention
- `.planning/REQUIREMENTS.md` — MIG-01..11, CLN-01..03 requirement text
- `.planning/ROADMAP.md` — Phase 5 wave ordering and success criteria
- `.planning/STATE.md` — v1.1 locked decisions, accumulated context
- `.planning/config.json` — `nyquist_validation: true` confirmed

### Secondary (MEDIUM confidence)

- Phase 4 CONTEXT.md references (not re-read, but corroborated by icon file presence on disk)

---

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — confirmed by reading actual source files; no assumption about what exists
- Architecture: HIGH — all file paths, line numbers, and current implementations verified directly
- Pitfalls: HIGH — derived from actual code inspection; pitfalls identify real patterns present in the files
- Validation architecture: HIGH — test framework confirmed from build.gradle.kts; grep commands derived from actual file content

**Research date:** 2026-04-29
**Valid until:** End of Phase 5 execution (content is tied to current source file state; any pre-Phase-5 commits to these files would invalidate line numbers)
