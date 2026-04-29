# Architecture Research — AeroIcons Integration (v1.1)

**Domain:** Icon-set integration into existing Compose Desktop UI component library
**Researched:** 2026-04-28
**Confidence:** HIGH (based on direct source inspection of all 14 migration targets + Material Icons source as reference pattern)

---

## ⚠ SOURCE REVISION (2026-04-28, supersedes Feather references below)

**Icon source changed from Feather to Phosphor Regular.** All architectural decisions in this document — file layout (`icons/AeroIcons.kt` facade + `icons/internal/` per-icon files), API shape (lazy backing-property pattern), migration recipes, sizing/tinting conventions, build order, and explicitApi compatibility — apply unchanged to Phosphor.

**The only delta in the generated `ImageVector.Builder` calls:**

```kotlin
// Old (Feather):
ImageVector.Builder(name = "Close", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, ...)
}

// New (Phosphor Regular):
ImageVector.Builder(name = "X", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 256f, viewportHeight = 256f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 12f, ...)
}
```

`defaultWidth`/`defaultHeight` stay at 24dp (render target). Only `viewportWidth`/`viewportHeight` (24f → 256f) and `strokeLineWidth` (2f → 12f) change. Valkyrie CLI handles this automatically per source SVG.

**Naming differences (relevant for Wave-by-wave migration recipes below):**
- `Close` → `X` (Phosphor calls it "x")
- `ChevronDown` → `CaretDown` (Phosphor uses "caret-*", not "chevron-*")
- `ChevronUp` → `CaretUp`
- `ChevronRight` → `CaretRight`
- `Search` → `MagnifyingGlass`
- `EyeOff` → `EyeSlash`
- `Folder` → `Folder` (same)
- `Info` / `Warning` / `XCircle` (replaces Material `Error`) / `CheckCircle` / `Question` — Phosphor names

The full Phosphor name → PascalCase mapping lives in the new FEATURES.md.

---

## 1. Package and File Layout

### Recommended Layout

```
library/src/main/kotlin/com/mordred/aero/
├── icons/
│   ├── AeroIcons.kt            ← facade object with ALL icon properties
│   └── internal/
│       ├── ChevronDown.kt      ← one file per icon, private to package
│       ├── ChevronRight.kt
│       ├── ChevronUp.kt
│       ├── Close.kt
│       ├── Check.kt
│       ├── Minus.kt
│       ├── Minimize.kt
│       ├── Maximize.kt
│       ├── Restore.kt
│       ├── ChevronDoubleRight.kt
│       ├── Search.kt
│       ├── Eye.kt
│       ├── EyeOff.kt
│       ├── Folder.kt
│       ├── Info.kt
│       ├── AlertTriangle.kt
│       ├── AlertCircle.kt
│       ├── HelpCircle.kt
│       ├── CheckCircle.kt
│       └── ... (remaining ~100-130 icons)
```

### Rationale

**Why facade + internal/:** The facade (`AeroIcons.kt`) is the only file the IDE shows in autocomplete. All `val` properties on the `AeroIcons` object reference internal backing functions defined in `icons/internal/`. This is identical to the Compose Material Icons architecture (`Icons.Outlined.Info` is a property on the `Outlined` object; the path data lives in a generated file in `material-icons-extended`). It gives:

- Clean IDE autocomplete: type `AeroIcons.` and see all icons — nothing else
- No file size limit problem: each icon file is ~40-80 lines of `ImageVector.Builder` calls; a single file with 140 icons would be ~8,000-10,000 lines (unacceptable for navigation and compilation)
- Lazy initialization per-icon: each backing property is initialized once on first access, held by its private `_Icon` variable — not on class load
- Compile-time isolation: changing one icon's path data only recompiles that file, not all 140

**Why NOT a single-file approach:** A monolithic `AeroIcons.kt` with all 140 icon builders inline would be 8,000-10,000 lines. Kotlin has no hard line-count limit, but the Kotlin compiler allocates one `$init$` block per file — a 10,000-line `object` with 140 inline `ImageVector.Builder` chains produces a very large bytecode class. IDE "Go to definition" on any property would navigate to the same file with no locality benefit. Build incremental compilation becomes coarser.

**Why NOT subpackages by category (`icons/navigation/`, `icons/files/`):** Feather icons are already a curated flat set with no official category grouping. Imposing a taxonomy creates ambiguity (does `ChevronRight` go in `navigation/` or `arrows/`?) and forces callers to know the category before autocomplete can help. A single `AeroIcons` object surface is simpler.

---

## 2. Public API Surface

### Recommended Signature: Lazy Backing Field Pattern

```kotlin
// library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt

package com.mordred.aero.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Typed ImageVector constants for aero-compose-ui.
 * All icons are Feather-derived: 24x24 viewBox, stroke-width 2, round linecap/linejoin.
 * Access: AeroIcons.Close, AeroIcons.ChevronDown, etc.
 */
public object AeroIcons {

    // ── Window chrome ──────────────────────────────────────────
    public val Close: ImageVector get() = _Close ?: close().also { _Close = it }
    private var _Close: ImageVector? = null

    public val Minimize: ImageVector get() = _Minimize ?: minimize().also { _Minimize = it }
    private var _Minimize: ImageVector? = null

    public val Maximize: ImageVector get() = _Maximize ?: maximize().also { _Maximize = it }
    private var _Maximize: ImageVector? = null

    public val Restore: ImageVector get() = _Restore ?: restore().also { _Restore = it }
    private var _Restore: ImageVector? = null

    // ── Chevrons ───────────────────────────────────────────────
    public val ChevronDown: ImageVector get() = _ChevronDown ?: chevronDown().also { _ChevronDown = it }
    private var _ChevronDown: ImageVector? = null

    public val ChevronRight: ImageVector get() = _ChevronRight ?: chevronRight().also { _ChevronRight = it }
    private var _ChevronRight: ImageVector? = null

    public val ChevronUp: ImageVector get() = _ChevronUp ?: chevronUp().also { _ChevronUp = it }
    private var _ChevronUp: ImageVector? = null

    public val ChevronLeft: ImageVector get() = _ChevronLeft ?: chevronLeft().also { _ChevronLeft = it }
    private var _ChevronLeft: ImageVector? = null

    // ── Checkmarks / Selection ─────────────────────────────────
    public val Check: ImageVector get() = _Check ?: check().also { _Check = it }
    private var _Check: ImageVector? = null

    public val Minus: ImageVector get() = _Minus ?: minus().also { _Minus = it }
    private var _Minus: ImageVector? = null

    // ── Search / Input ─────────────────────────────────────────
    public val Search: ImageVector get() = _Search ?: search().also { _Search = it }
    private var _Search: ImageVector? = null

    public val Eye: ImageVector get() = _Eye ?: eye().also { _Eye = it }
    private var _Eye: ImageVector? = null

    public val EyeOff: ImageVector get() = _EyeOff ?: eyeOff().also { _EyeOff = it }
    private var _EyeOff: ImageVector? = null

    public val Folder: ImageVector get() = _Folder ?: folder().also { _Folder = it }
    private var _Folder: ImageVector? = null

    // ── Status / Notification ──────────────────────────────────
    public val Info: ImageVector get() = _Info ?: info().also { _Info = it }
    private var _Info: ImageVector? = null

    public val AlertTriangle: ImageVector get() = _AlertTriangle ?: alertTriangle().also { _AlertTriangle = it }
    private var _AlertTriangle: ImageVector? = null

    public val AlertCircle: ImageVector get() = _AlertCircle ?: alertCircle().also { _AlertCircle = it }
    private var _AlertCircle: ImageVector? = null

    public val HelpCircle: ImageVector get() = _HelpCircle ?: helpCircle().also { _HelpCircle = it }
    private var _HelpCircle: ImageVector? = null

    public val CheckCircle: ImageVector get() = _CheckCircle ?: checkCircle().also { _CheckCircle = it }
    private var _CheckCircle: ImageVector? = null

    // ... remaining ~100-130 icons follow the same pattern
}
```

Each per-icon builder function lives in its corresponding `internal/` file:

```kotlin
// library/src/main/kotlin/com/mordred/aero/icons/internal/Close.kt

package com.mordred.aero.icons.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Package-private — only AeroIcons.kt calls this
internal fun close(): ImageVector = ImageVector.Builder(
    name = "AeroIcons.Close",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.Black),   // tint overrides this at render time
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeAlpha = 1f,
        fillAlpha = 0f
    ) {
        moveTo(18f, 6f)
        lineTo(6f, 18f)
    }
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeAlpha = 1f,
        fillAlpha = 0f
    ) {
        moveTo(6f, 6f)
        lineTo(18f, 18f)
    }
}.build()
```

### Why this pattern over alternatives

**Alternative A — `val Close: ImageVector = buildClose()`** (eager, direct assignment):

Eager initialization means all ~140 icons are constructed when `AeroIcons` object is first accessed. For a class-loading scenario where the app uses only 15 icons, the other 125 are wasted allocations. Each `ImageVector` built via `ImageVector.Builder` allocates several objects internally (the vector, its paths, the path data). Lazy is strictly better.

**Alternative B — `val Close: ImageVector by lazy { buildClose() }`** (stdlib lazy delegation):

`lazy {}` works but adds a `Lazy<T>` wrapper object per property and a `LazyThreadSafetyMode` decision. The nullable-backing-field pattern (`private var _Close: ImageVector? = null`) allocates nothing extra and avoids thread-safety overhead. It is the exact pattern Material Icons Extended uses (confirmed by reading the generated source). Prefer it for consistency with Material Icons.

**Alternative C — nested objects `AeroIcons.Navigation.ChevronRight`:**

Nested objects (`object Navigation`, `object Files`, etc.) fragment the autocomplete. A developer typing `AeroIcons.` sees `Navigation`, `Files`, `Status` — they must know the category first. Feather's flat namespace does not need sub-grouping. Reject nested objects.

**Alternative D — `materialIcon(name = "AeroIcons.Close") { ... }` helper:**

`materialIcon` is a Material library internal (`@InternalMaterialIconsApi`) not intended for external use. Writing a similar `aeroIcon {}` DSL helper is possible but adds complexity for no benefit over the builder pattern above. Reject.

### explicitApi() Compatibility

All `public val` properties on `public object AeroIcons` satisfy `explicitApi()`. The nullable backing fields (`private var _Close`) are `private` — they require no `public` annotation. The per-icon builder functions in `internal/` are `internal fun` — they satisfy `explicitApi()` because they are not `public`. No gotchas.

**One confirmed explicitApi quirk to avoid:** do not use `public val Close: ImageVector = ...` as a direct class-body initializer if the RHS is a complex expression — this is fine for explicitApi but creates eager initialization. Use the getter form `get() = _Close ?: ...` shown above. The getter form is compatible with explicitApi and remains lazy.

---

## 3. Migration Touchpoints — Per-Component Analysis

### 3.1 AeroTitleBar — Internal Change, No API Impact

**Current code (AeroTitleBar.kt lines 107-130):**
```
TitleBarButton(glyph = "─", ...)     // minimize
TitleBarButton(glyph = "□" / "❒", ...) // maximize/restore  
TitleBarButton(glyph = "✕", ...)     // close
```
`TitleBarButton` is a private composable accepting `glyph: String` and rendering it via `Text()`.

**Migration:** Replace `TitleBarButton` internals or add an overload accepting `ImageVector`. The private helper becomes:

```kotlin
@Composable
private fun TitleBarButton(
    icon: ImageVector,
    contentDescription: String,
    hoverColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    // ... Box wrapper identical to current ...
    Icon(imageVector = icon, contentDescription = contentDescription, tint = iconTint,
         modifier = Modifier.size(16.dp))
}
```

Call sites become:
```kotlin
TitleBarButton(AeroIcons.Minimize, "Minimize", colors.buttonHover, colors.titleBarText, ...)
TitleBarButton(
    icon = if (isMaximized) AeroIcons.Restore else AeroIcons.Maximize,
    ...
)
TitleBarButton(AeroIcons.Close, "Close", colors.closeButtonHover, colors.titleBarText, ...)
```

**API impact:** None. `AeroTitleBar`'s public signature (`title`, `windowState`, `onCloseRequest`, `leading`, `modifier`) is unchanged.

**Change type:** Internal-only refactor.

---

### 3.2 AeroCheckbox / AeroTriStateCheckbox — Internal Change, No API Impact

**Current code (AeroCheckbox.kt lines 97-98):**
```kotlin
ToggleableState.On -> Text("✓", color = colors.onPrimary, fontSize = 11.sp)
ToggleableState.Indeterminate -> Text("–", color = colors.onPrimary, fontSize = 11.sp)
```

**Migration:** Replace `Text()` with `Icon()`:
```kotlin
ToggleableState.On -> Icon(
    imageVector = AeroIcons.Check,
    contentDescription = null,
    tint = colors.onPrimary,
    modifier = Modifier.size(12.dp)
)
ToggleableState.Indeterminate -> Icon(
    imageVector = AeroIcons.Minus,
    contentDescription = null,
    tint = colors.onPrimary,
    modifier = Modifier.size(12.dp)
)
```

**API impact:** None. Public signatures `AeroCheckbox(checked, onCheckedChange, modifier, enabled, label)` and `AeroTriStateCheckbox(state, onClick, modifier, enabled, label)` are unchanged.

**Change type:** Internal-only refactor.

**Sizing note:** The checkbox box is 16dp. A 12dp icon inside it gives 2dp visual padding on each side, matching the current glyph appearance. Use `Modifier.size(12.dp)` on the Icon, not the default 24dp.

---

### 3.3 AeroDropdown — Internal Change, No API Impact

**Current code (AeroDropdown.kt line 108-111):**
```kotlin
Text(
    text = "▼",
    color = colors.labelText,
    style = AeroTheme.typography.label
)
```

**Migration:**
```kotlin
Icon(
    imageVector = AeroIcons.ChevronDown,
    contentDescription = null,
    tint = colors.labelText,
    modifier = Modifier.size(14.dp)
)
```

**API impact:** None. Public signature is unchanged.

**Change type:** Internal-only refactor.

**Note:** AeroComboBox also uses a chevron indicator — check its source and apply the same change. It was not in the migration touchpoints list but should be migrated in the same wave for consistency.

---

### 3.4 AeroNumberSpinner — Internal Change, No API Impact

**Current code (AeroNumberSpinner.kt lines 129, 139):**
```kotlin
Text("▲", fontSize = 8.sp, color = colors.onSurface)
Text("▼", fontSize = 8.sp, color = colors.onSurface)
```

**Migration:** The spinner buttons are 16×12dp. An Icon at 8dp fits:
```kotlin
Icon(
    imageVector = AeroIcons.ChevronUp,
    contentDescription = "Increment",
    tint = colors.onSurface,
    modifier = Modifier.size(8.dp)
)
Icon(
    imageVector = AeroIcons.ChevronDown,
    contentDescription = "Decrement",
    tint = colors.onSurface,
    modifier = Modifier.size(8.dp)
)
```

**API impact:** None. Public signature is unchanged.

**Change type:** Internal-only refactor.

**Sizing note:** 8dp is unusually small for a vector icon — stroke paths at 8dp on a 24-unit viewport yield ~0.33dp-per-unit resolution. Test visually. If the chevron appears too thin, increase button height to 14dp and icon to 10dp. This is a visual decision to validate during implementation, not a blocker.

---

### 3.5 AeroBreadcrumb — BREAKING CHANGE (with backward-compat path)

**Current signature:**
```kotlin
@Composable
public fun AeroBreadcrumb(
    items: List<AeroBreadcrumbItem>,
    onItemClick: (Int, AeroBreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    separator: String = "›"
)
```

The `separator: String` parameter is part of the public API surface. Changing it to `separator: ImageVector` is a source-breaking change for any caller passing a `String` explicitly.

**Two options:**

**Option A — Break it (recommended for v1.1, single-consumer library):**
```kotlin
@Composable
public fun AeroBreadcrumb(
    items: List<AeroBreadcrumbItem>,
    onItemClick: (Int, AeroBreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    separator: ImageVector = AeroIcons.ChevronRight
)
```
Internal rendering changes from `Text(separator)` to:
```kotlin
Icon(
    imageVector = separator,
    contentDescription = null,
    tint = colors.labelText,
    modifier = Modifier.size(12.dp)
)
```
This removes the ability to pass `"/"` or `">"` as text separators. Since the library is pre-v1 public release (local Maven only), there are no external consumers to break. **Recommended.**

**Option B — Additive overload (no break, dual API):**
```kotlin
// Keep original for source-compat
@Composable
public fun AeroBreadcrumb(
    items: List<AeroBreadcrumbItem>,
    onItemClick: (Int, AeroBreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    separator: String = "›"
)

// New overload with ImageVector
@Composable
public fun AeroBreadcrumb(
    items: List<AeroBreadcrumbItem>,
    onItemClick: (Int, AeroBreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    separator: ImageVector = AeroIcons.ChevronRight
)
```
This is more work (two implementations or a shared private helper) and is only needed if external callers with `separator: String` must continue to compile. Not warranted for v1.1.

**Decision: Option A. Change `separator: String` to `separator: ImageVector = AeroIcons.ChevronRight`. Treat as breaking. Document in changelog.**

**Change type:** BREAKING — public parameter type changes from `String` to `ImageVector`.

---

### 3.6 AeroContextMenu — Internal Change, No API Impact

**Current code (AeroContextMenu.kt line 183):**
```kotlin
Text("▶", color = colors.labelText, style = AeroTheme.typography.label)
```

**Migration:**
```kotlin
Icon(
    imageVector = AeroIcons.ChevronRight,
    contentDescription = null,
    tint = colors.labelText,
    modifier = Modifier.size(12.dp)
)
```

**API impact:** None. The public API is `Modifier.aeroContextMenu(items: List<AeroContextMenuItem>)`. The submenu indicator is a private rendering detail.

**Note:** `AeroContextMenuItem.Action` and `AeroContextMenuItem.Submenu` already accept `icon: ImageVector?` in their public `data class` constructors. These icon slots already use `Icon()` (AeroContextMenu.kt lines 138, 175). The Material Icons import (`Icons.Outlined.*`) is not present in this file — the submenu `▶` glyph is the only text glyph here. Single-line change.

**Change type:** Internal-only refactor.

---

### 3.7 AeroToastHost — Internal Change, No API Impact

**Current code (AeroToastHost.kt line 91):**
```kotlin
AeroIconButton(onClick = onDismiss, size = 24.dp) {
    Text("✕", color = colors.onSurface)
}
```

**Migration:**
```kotlin
AeroIconButton(onClick = onDismiss, size = 24.dp) {
    Icon(
        imageVector = AeroIcons.Close,
        contentDescription = "Dismiss",
        tint = colors.onSurface,
        modifier = Modifier.size(16.dp)
    )
}
```

**API impact:** None. `AeroToastHost(state, modifier)` signature is unchanged.

**Change type:** Internal-only refactor.

---

### 3.8 AeroNotificationBanner — Internal Change, No API Impact

**Current code (AeroNotificationBanner.kt line 64):**
```kotlin
AeroIconButton(onClick = onDismiss, size = 24.dp) {
    Text("✕", color = colors.onSurface)
}
```

Same pattern as AeroToastHost — replace with `Icon(AeroIcons.Close, ...)`.

**API impact:** None.

**Change type:** Internal-only refactor.

---

### 3.9 AeroAlertKind — Internal Change, No API Impact (type preserved)

**Current code (AeroAlertKind.kt):**
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning

public val icon: ImageVector
    get() = when (this) {
        Info     -> Icons.Outlined.Info
        Warning  -> Icons.Outlined.Warning
        Error    -> Icons.Outlined.Error
        Question -> Icons.Outlined.HelpOutline
    }
```

**Migration:** The `icon: ImageVector` property type stays `ImageVector` — the public return type is identical. Only the source of the vector changes:

```kotlin
import com.mordred.aero.icons.AeroIcons
// Remove all Icons.Outlined imports

public val icon: ImageVector
    get() = when (this) {
        Info     -> AeroIcons.Info
        Warning  -> AeroIcons.AlertTriangle
        Error    -> AeroIcons.AlertCircle
        Question -> AeroIcons.HelpCircle
    }
```

**API impact:** None. `AeroAlertKind.icon` returns `ImageVector` before and after. Any caller that uses `kind.icon` continues to compile unchanged.

**Change type:** Internal-only refactor. Removes `import androidx.compose.material.icons.*`.

---

### 3.10 AeroBannerKind — Internal Change, No API Impact (type preserved)

**Current code (AeroBannerKind.kt):**
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning

public val icon: ImageVector
    get() = when (this) {
        Info     -> Icons.Outlined.Info
        Warning  -> Icons.Outlined.Warning
        Error    -> Icons.Outlined.Error
        Success  -> Icons.Outlined.CheckCircle
    }
```

**Migration:**
```kotlin
import com.mordred.aero.icons.AeroIcons

public val icon: ImageVector
    get() = when (this) {
        Info    -> AeroIcons.Info
        Warning -> AeroIcons.AlertTriangle
        Error   -> AeroIcons.AlertCircle
        Success -> AeroIcons.CheckCircle
    }
```

**API impact:** None. Return type `ImageVector` is preserved.

**Change type:** Internal-only refactor. Removes `import androidx.compose.material.icons.*`.

---

### 3.11 AeroSearchField — Canvas Replacement, No API Impact

**Current implementation:** `SearchIcon()` and `ClearButton()` are private composables using `Canvas` (a hand-drawn magnifier circle+handle) and `Text("x", ...)`.

**Migration:** Replace both private composables with `Icon()` calls:

```kotlin
// In search icon Box:
Icon(
    imageVector = AeroIcons.Search,
    contentDescription = null,
    tint = AeroTheme.colors.labelText,
    modifier = Modifier.size(14.dp)
)

// In clear button Box:
Icon(
    imageVector = AeroIcons.Close,
    contentDescription = "Clear search",
    tint = AeroTheme.colors.labelText,
    modifier = Modifier.size(12.dp)
)
```

The Canvas-based `SearchIcon()` and `EyeClosedIcon()`/`EyeOpenIcon()` private composables are deleted entirely — their Canvas drawing is superseded by the Feather vector data.

**API impact:** None. `AeroSearchField(value, onValueChange, modifier, enabled, placeholder, onSearch)` is unchanged.

**Change type:** Internal-only refactor. Significant code deletion (removes ~50 lines of Canvas drawing code from AeroSearchField.kt).

---

### 3.12 AeroPasswordField — Canvas Replacement, No API Impact

**Current implementation:** `EyeOpenIcon(tint)` and `EyeClosedIcon(tint)` are private Canvas-drawn composables (~70 lines of bezier path code).

**Migration:** Replace with:
```kotlin
// In toggle Box:
Icon(
    imageVector = if (visible) AeroIcons.Eye else AeroIcons.EyeOff,
    contentDescription = if (visible) "Hide password" else "Show password",
    tint = colors.labelText,
    modifier = Modifier.size(14.dp)
)
```

Delete `EyeOpenIcon` and `EyeClosedIcon` private composables entirely.

**API impact:** None. `AeroPasswordField(value, onValueChange, modifier, enabled, placeholder, interactionSource)` is unchanged.

**Change type:** Internal-only refactor. Significant code deletion (~70 lines of Canvas path code removed).

---

### 3.13 AeroFilePicker — No Icon Currently, Out of Scope for This Migration

**Current implementation:** AeroFilePicker uses a text button labelled "Обзор" (a localized Russian label) — it has no icon. There is no glyph or Material Icon to migrate.

**API impact:** None — nothing to change.

**Optional enhancement (not in scope for this milestone):** A `Folder` icon could be added to the "Обзор" button as a leading icon, but this is a new feature, not a migration. Defer.

---

### 3.14 ButtonsSection in Showcase — Content Update

**Current code (ButtonsSection.kt lines 50-52):**
```kotlin
AeroIconButton(onClick = {}) { Text("▲", color = colors.onSurface) }
AeroIconButton(onClick = {}) { Text("▼", color = colors.onSurface) }
AeroIconButton(onClick = {}, enabled = false) { Text("×", color = colors.onSurface) }
```

**Migration:**
```kotlin
AeroIconButton(onClick = {}) {
    Icon(AeroIcons.ChevronUp, null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
}
AeroIconButton(onClick = {}) {
    Icon(AeroIcons.ChevronDown, null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
}
AeroIconButton(onClick = {}, enabled = false) {
    Icon(AeroIcons.Close, null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
}
```

**API impact:** None (showcase internal).

**Change type:** Showcase-internal update.

---

## 4. Sizing Convention

### Default Icon Size: 16dp

Material Design uses 24dp as the default `Icon()` size. For aero-compose-ui's desktop context, 24dp is too large for most use sites:

- Checkbox indicator inside a 16dp box: requires 12dp
- Breadcrumb separator: 12dp
- Spinner buttons (16×12dp area): 8-10dp
- TitleBar buttons (46×32dp area): 14-16dp
- SearchField leading icon panel (28dp height): 14dp
- Toast/Banner close button inside AeroIconButton(size=24dp): 14-16dp
- ContextMenu submenu indicator: 12dp

The consistent internal usage is 12-16dp. A default of **16dp** is the right baseline for standalone `Icon()` usage. Callers override via `Modifier.size(N.dp)`.

**Rule:**
- Default icon size when used standalone or in standard slots: `Modifier.size(16.dp)`
- Small indicator glyphs (separators, spinners, checkmarks): `Modifier.size(12.dp)`
- Do NOT set a default on the `ImageVector` itself — size is always caller-controlled via `Modifier.size()`

**No `defaultWidth`/`defaultHeight` override needed:** The `ImageVector.Builder` sets `defaultWidth = 24.dp` and `defaultHeight = 24.dp` (following Feather's 24×24 viewBox). The Compose `Icon()` composable ignores `defaultWidth`/`defaultHeight` when a `Modifier.size()` is present — it scales the vector to fit the modifier bounds. So setting `defaultWidth = 16.dp` in the builder would only affect `Icon()` calls without a size modifier, which are rare. Keep the canonical 24dp viewport and control size at call site.

---

## 5. Tinting — Use Material `Icon()`, Not a Custom `AeroIcon()`

### Decision: Use `androidx.compose.material3.Icon()` directly

**Rationale:**

`Icon()` from Material3 reads `LocalContentColor` for its default tint. In the existing codebase, `AeroIconButton` already sets `LocalContentColor` via `CompositionLocalProvider` (AeroIconButton.kt lines 101-104). This propagation is already working correctly for any `Icon()` placed inside `AeroIconButton`.

For icon usage outside `AeroIconButton` (TitleBar buttons, checkbox checkmarks, breadcrumb separators, etc.), the tint is always passed explicitly: `Icon(icon, null, tint = colors.onSurface)`. There is no ambient tint ambiguity.

**Do NOT create a custom `AeroIcon()` composable** that wraps `Icon()`. The reasons:

1. It adds a layer of indirection callers must learn
2. `Icon()` from Material3 already handles `LocalContentColor` correctly
3. The library already mixes `MaterialTheme` and `AeroTheme` — Material's `Icon()` participates in both hierarchies correctly
4. `Icon()` handles accessibility (`contentDescription`) semantics out of the box

**Tint interaction with AeroTheme:**

`Icon(imageVector = x, contentDescription = null, tint = AeroTheme.colors.onSurface)` — explicit tint from `AeroColorScheme`. This is the recommended pattern for all icon uses inside library components. Never rely on `LocalContentColor` ambient for library-internal icon rendering — always pass tint explicitly to be theme-safe.

**The one exception:** Icons inside `AeroIconButton { }` slots. `AeroIconButton` already `CompositionLocalProvider(LocalContentColor provides ...)`, so `Icon()` placed in its content lambda can safely omit the explicit `tint` parameter and inherit from `LocalContentColor`. Both approaches work; explicit tint is clearer in library code.

---

## 6. Showcase IconsSection

### Proposed Layout

```kotlin
// showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt

@Composable
fun IconsSection() {
    val allIcons: List<Pair<String, ImageVector>> = remember {
        listOf(
            "Close" to AeroIcons.Close,
            "Minimize" to AeroIcons.Minimize,
            // ... all ~140 icons in alphabetical order
        )
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) allIcons
        else allIcons.filter { it.first.contains(query, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Icons", color = colors.onBackground, style = typography.title)
        AeroSearchField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(0.4f),
            placeholder = "Filter icons..."
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(400.dp)  // bounded height inside parent scroll
        ) {
            items(filtered, key = { it.first }) { (name, vector) ->
                IconCell(name = name, vector = vector)
            }
        }
    }
}

@Composable
private fun IconCell(name: String, vector: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .glassEffect(cornerRadius = 4.dp)
            .padding(8.dp)
            .size(72.dp)
    ) {
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = AeroTheme.colors.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = name,
            style = AeroTheme.typography.label,
            color = AeroTheme.colors.labelText,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}
```

**Placement in ShowcaseApp.kt:** Add `IconsSection()` immediately after `FoundationSection()` and before `ButtonsSection()`. Icons are foundational infrastructure; showing them early lets developers verify the icon set before seeing components that use them. Alternatively, place at the very end as an appendix. Either is acceptable — recommend "after Foundation, before Buttons" since the icon system is the primary feature of v1.1 and deserves early prominence.

**LazyVerticalGrid inside a vertically-scrolling Column:** This requires a bounded height on the grid (e.g., `Modifier.height(400.dp)`). Without a bounded height, `LazyVerticalGrid` cannot measure itself inside an unbounded parent Column. Use `Modifier.height(400.dp)` or `heightIn(max = 600.dp)`.

**AeroSearchField reuse:** The showcase self-exercises `AeroSearchField` inside `IconsSection`, demonstrating both the icon set AND the search field component in context. No new primitives needed.

---

## 7. Build Order

### Phase Sequence

```
Phase A: AeroIcons foundation (ALL icon constants + internal/ files)
    ↓ compile-time gate: library compiles with AeroIcons.* available
Phase B: Migrate internal-only consumers (parallel-safe)
    ├── AeroCheckbox (Check, Minus)
    ├── AeroDropdown + AeroComboBox (ChevronDown)
    ├── AeroNumberSpinner (ChevronUp, ChevronDown)
    ├── AeroToastHost (Close)
    ├── AeroNotificationBanner (Close)
    └── AeroContextMenu (ChevronRight)
Phase C: Migrate enum icon properties (removes materialIconsExtended dependency)
    ├── AeroAlertKind.icon → AeroIcons.*
    └── AeroBannerKind.icon → AeroIcons.*
Phase D: Replace Canvas-drawn icons
    ├── AeroSearchField (Search, Close — replaces hand-drawn Canvas SearchIcon + text "x")
    └── AeroPasswordField (Eye, EyeOff — replaces hand-drawn Canvas EyeOpenIcon/EyeClosedIcon)
Phase E: AeroTitleBar (Minimize, Maximize, Restore, Close)
Phase F: AeroBreadcrumb (ChevronRight — BREAKING parameter type change)
Phase G: Remove compose.materialIconsExtended from library/build.gradle.kts
    ↑ Only safe after Phase C completes — AeroAlertKind and AeroBannerKind are the last
      direct consumers of Icons.Outlined.*
Phase H: Showcase updates
    ├── IconsSection (new)
    ├── ButtonsSection (▲▼× → Icon)
    └── Any other sections using text glyphs
```

**Why icons first:** All migration phases (B through H) import `AeroIcons.*`. If AeroIcons is not built first, no migration step can compile. This is a hard dependency ordering.

**Why Canvas replacements (Phase D) are separate from internal glyphs (Phase B):** The Canvas composables (`SearchIcon`, `EyeOpenIcon`, `EyeClosedIcon`) in `AeroSearchField` and `AeroPasswordField` are more invasive — they involve deleting significant code blocks and restructuring the composable internals. They are lower risk for misalignment and can be done after the simpler text-glyph replacements are validated.

**Why TitleBar is separate (Phase E):** `AeroTitleBar` is the highest-visibility component. Its three buttons control window state. It deserves isolated testing after the Canvas replacements are done. The private `TitleBarButton` helper must be restructured (currently takes `glyph: String`; must change to take `ImageVector`).

**Why Breadcrumb is last (Phase F):** It's the only breaking API change. Doing it last means all internal changes are validated before making the public API break visible.

**Why dependency removal (Phase G) is its own step:** Running `./gradlew :library:build` after removing `compose.materialIconsExtended` confirms no remaining `Icons.Outlined.*` imports exist. If any were missed, the build fails immediately. This acts as an integration test.

**Migration waves for parallel development (Phases B + C can run in parallel):**

```
Wave 1 (independent, no API impact):
  AeroCheckbox, AeroDropdown, AeroComboBox, AeroNumberSpinner,
  AeroToastHost, AeroNotificationBanner, AeroContextMenu,
  AeroAlertKind, AeroBannerKind

Wave 2 (Canvas deletion, independent per file):
  AeroSearchField, AeroPasswordField

Wave 3 (TitleBar private restructure):
  AeroTitleBar

Wave 4 (breaking public API change):
  AeroBreadcrumb

Wave 5 (dependency removal):
  library/build.gradle.kts — remove compose.materialIconsExtended

Wave 6 (showcase):
  IconsSection (new), ButtonsSection (update)
```

Waves 1 and 2 can be developed simultaneously by different task tickets if needed. Waves 3-6 are sequential.

---

## 8. explicitApi() Compatibility — Confirmed

The `kotlin { explicitApi() }` block in `library/build.gradle.kts` (line 9) requires all public declarations to have explicit visibility modifiers.

**AeroIcons object:** `public object AeroIcons` — satisfies explicitApi.

**Icon properties:** `public val Close: ImageVector get() = ...` — satisfies explicitApi. The getter syntax is required for the lazy pattern and is fully compatible with explicitApi.

**Private backing fields:** `private var _Close: ImageVector? = null` — `private` is explicit, satisfies explicitApi.

**Internal builder functions:** `internal fun close(): ImageVector` in `icons/internal/` files — `internal` is explicit, satisfies explicitApi.

**Confirmed gotcha to avoid:** Forgetting `public` on any `val` in `AeroIcons` raises a compile error under explicitApi: `"Visibility must be specified in explicit API mode"`. The Kotlin compiler error message is clear. Since every property in `AeroIcons` is intended to be public, annotating all of them with `public` is the correct action.

**No delegation or operator issues:** The `get() = _Field ?: builder().also { _Field = it }` pattern is a custom getter, not a `by` delegation. Custom getters require no special annotation treatment under explicitApi. `by lazy {}` is also compatible but adds a `Lazy<T>` object — stick with the custom getter pattern for consistency with Material Icons.

---

## 9. Dependency Cleanup

**Current library/build.gradle.kts dependency causing removal:**
```kotlin
implementation(compose.materialIconsExtended)   // line 15 — REMOVE in Phase G
```

**After migration:** Only `AeroAlertKind` and `AeroBannerKind` import from `androidx.compose.material.icons.*`. All other library files use only standard Compose UI (`ImageVector`, `Icon`). Once those two enums are migrated (Phase C), the dependency can be removed.

**Verify before removing:** Run `./gradlew :library:build` after removing the dependency. If any file still imports `Icons.Outlined.*` or `Icons.Default.*`, the build will fail with `Unresolved reference: Icons`. This is the intended gate.

**What remains:** `implementation(compose.material3)` stays — `Icon()` from Material3 (`androidx.compose.material3.Icon`) is the rendering primitive and is not in `materialIconsExtended`. Only the icon *data* (the `Icons.Outlined.*` constants) lives in `materialIconsExtended`.

---

## 10. Component Boundaries Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                   :library module                            │
│                                                              │
│  com.mordred.aero.icons/                                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  AeroIcons (public object)                           │   │
│  │  ├── val Close: ImageVector    (lazy, public)        │   │
│  │  ├── val ChevronDown: ImageVector                    │   │
│  │  ├── val Check: ImageVector                          │   │
│  │  └── ... ~140 more                                   │   │
│  │                                                      │   │
│  │  icons/internal/ (package-private)                   │   │
│  │  ├── fun close(): ImageVector                        │   │
│  │  ├── fun chevronDown(): ImageVector                  │   │
│  │  └── ... one file per icon                           │   │
│  └──────────────────────────────────────────────────────┘   │
│              ↓ imports AeroIcons.*                           │
│  com.mordred.aero.components/                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  AeroCheckbox  → AeroIcons.Check, Minus              │   │
│  │  AeroDropdown  → AeroIcons.ChevronDown               │   │
│  │  AeroNumberSpinner → AeroIcons.ChevronUp/Down        │   │
│  │  AeroTitleBar  → AeroIcons.Close/Minimize/Maximize   │   │
│  │  AeroBreadcrumb → AeroIcons.ChevronRight (default)   │   │
│  │  AeroContextMenu → AeroIcons.ChevronRight            │   │
│  │  AeroToastHost → AeroIcons.Close                     │   │
│  │  AeroNotificationBanner → AeroIcons.Close            │   │
│  │  AeroAlertKind → AeroIcons.Info/AlertTriangle/etc.   │   │
│  │  AeroBannerKind → AeroIcons.Info/AlertTriangle/etc.  │   │
│  │  AeroSearchField → AeroIcons.Search, Close           │   │
│  │  AeroPasswordField → AeroIcons.Eye, EyeOff           │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  com.mordred.aero.theme/ — unchanged, no AeroIcons deps      │
└─────────────────────────────────────────────────────────────┘
              ↓ project dependency
┌─────────────────────────────────────────────────────────────┐
│                   :showcase module                           │
│  IconsSection  → AeroIcons.* (all icons for grid display)   │
│  ButtonsSection → AeroIcons.ChevronUp/Down/Close            │
│  All other sections → no direct AeroIcons usage             │
│    (they use components which internally use AeroIcons)      │
└─────────────────────────────────────────────────────────────┘
```

---

## Sources

- Direct source inspection: all 14 migration target files in `library/src/main/kotlin/com/mordred/aero/` — HIGH confidence
- Material Icons Extended source pattern for lazy backing field: confirmed by reading generated Compose Material Icons files — HIGH confidence
- Compose `ImageVector.Builder` API: `androidx.compose.ui.graphics.vector.ImageVector.Builder` — HIGH confidence (standard Compose UI API, stable since 1.0)
- `androidx.compose.material3.Icon()` API and `LocalContentColor` behavior: standard Material3, unchanged — HIGH confidence
- Feather Icons SVG specification: 24×24 viewBox, stroke-width 2, round linecap/linejoin (feathericons.com) — HIGH confidence
- Kotlin `explicitApi()` behavior with custom getters: confirmed by project's own existing usage (`AeroTheme.kt` lines 97-105 uses same `@Composable get()` pattern under explicitApi) — HIGH confidence
- `LazyVerticalGrid` bounded-height requirement inside unbounded Column: standard Compose layout constraint — HIGH confidence

---

*Architecture research for: AeroIcons integration into aero-compose-ui (v1.1)*
*Researched: 2026-04-28*
