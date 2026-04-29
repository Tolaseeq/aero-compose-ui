# Phase 6: Showcase IconsSection - Research

**Researched:** 2026-04-29
**Domain:** Compose Desktop showcase composition — `LazyVerticalGrid` of all 138 `AeroIcons.*` constants with live `AeroSearchField` filter, plus three text-glyph migrations in `ButtonsSection`
**Confidence:** HIGH

## Summary

Phase 6 is **almost entirely a wiring exercise inside `showcase/src/`**. Every primitive it needs already exists and was verified by reading the source: `AeroIcons` facade with 138 `public val AeroIcons.X: ImageVector` extension properties (one per file in `library/src/main/kotlin/com/mordred/aero/icons/internal/`), `AeroSearchField` with the exact `(value, onValueChange)` API the section needs, `Modifier.glassSurface`, `AeroToastHostState` already mounted at `ShowcaseApp.kt:53`, and Compose Desktop's `androidx.compose.ui.platform.LocalClipboardManager` (already on showcase classpath via `implementation(compose.ui)` in `showcase/build.gradle.kts:19`).

The three real planning constraints are: (1) `LazyVerticalGrid` needs an explicit bounded height when it's nested in a vertically-scrolling `Column` — without it, layout crashes; the user has locked `Modifier.height(400.dp)`; (2) the icon-list source must be **hand-authored alphabetized** because the 138 properties are *extension* properties at file-scope, not members of `AeroIcons::class` — reflection via `AeroIcons::class.memberProperties` returns empty (verified by reading `internal/X.kt` line 12: `public val AeroIcons.X: ImageVector`), and the showcase module has no `kotlin-reflect` dependency anyway (verified `showcase/build.gradle.kts`); (3) `AeroToastHostState.showToast` is a `suspend` function (verified `AeroToastHostState.kt:51`) so the click-to-copy callback needs `rememberCoroutineScope()` to launch from a non-suspending click handler.

**Primary recommendation:** Single section file `showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` accepts a `toastState: AeroToastHostState` param (matching `OverlaysSection`), holds `var query by remember { mutableStateOf("") }`, derives the filtered list with `remember(query) { ICONS.filter { it.name.contains(query, ignoreCase = true) } }` from a hand-authored alphabetized `private val ICONS = listOf(IconEntry("Archive", AeroIcons.Archive), … all 138 …)`, renders `AeroSearchField` + match-count text + a 400dp-bounded `LazyVerticalGrid(GridCells.Adaptive(80.dp))` (or empty-state), and copies `"AeroIcons.<Name>"` via `LocalClipboardManager.current.setText(AnnotatedString(...))` then `scope.launch { toastState.showToast("Copied AeroIcons.<Name>") }`. Insert at `ShowcaseApp.kt:80` between the `FoundationSection` block (line 78–79) and `ButtonsSection()` (line 81). Migrate `ButtonsSection.kt:50-52` to `Icon(AeroIcons.CaretUp/CaretDown/X, ...)` mechanically.

## User Constraints (from CONTEXT.md)

### Locked Decisions

**IconsSection cell visual:**
- Cell background: `Modifier.glassSurface(cornerRadius = 6.dp)` (not flat) — Win7 Aero priority per user memory.
- Hover state: implementation discretion (alpha raise / `glassEffect` swap / `colors.buttonHover` overlay) — must read as Aero hover, not flat color flip.
- Click action: copy full Kotlin identifier `"AeroIcons.<Name>"` to system clipboard, show `AeroToast`: literal `"Copied AeroIcons.<Name>"`. Reuse existing `AeroToastHostState` from `ShowcaseApp.kt:53`.
- Label typography: `AeroTheme.typography.label` (11sp), `colors.labelText`, `Text(maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)`.
- Cell sizing: `GridCells.Adaptive(80.dp)`, `Modifier.height(88.dp)` per cell, `Arrangement.spacedBy(8.dp)` on both axes.
- Icon: `material3.Icon(imageVector = aeroIcon, contentDescription = null, tint = AeroTheme.colors.onSurface, modifier = Modifier.size(24.dp))`.

**Search field + filter:**
- Component: reuse `AeroSearchField` at top of `IconsSection`, full available width (or capped at 400.dp). Placeholder: `"Search icons"`.
- Match algorithm: case-insensitive substring on **PascalCase Kotlin identifier only**. NO kebab-case fallback.
- Filter timing: real-time per keystroke. No debounce.
- Match-count display: `"<filtered> of 138"` next to or below the field, `typography.label` + `colors.labelText`.
- Empty state: plain `Text("No icons match '<query>'")` in `colors.onBackground` at `typography.body`. No illustration, no icon.
- Clearing the field restores all 138 (built into `AeroSearchField`'s clear button).

**Section framing:**
- Section header: `Text("Icons", style = typography.title, color = colors.onBackground)`. No caption under header.
- Grid bounded height: `Modifier.height(400.dp)` (NOT `heightIn(max = 600.dp)`).
- Section ordering: `ThemeSwitcher` → Foundation block → **IconsSection (NEW)** → `ButtonsSection` → `InputSection` → … (existing order preserved after IconsSection).

**ButtonsSection migration (SHW-06):**
- Migrate exactly the three glyphs at `ButtonsSection.kt:50-52`: `▲ ▼ ×` → `Icon(AeroIcons.CaretUp, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))`, `Icon(AeroIcons.CaretDown, ...)`, `Icon(AeroIcons.X, ...)`.
- Tint: `AeroTheme.colors.onSurface` for all three.
- Icon size inside `AeroIconButton`: `Modifier.size(14.dp)` (matches Phase 5 affordance-icon size).
- `AeroToolbar` row's `B I U S` letter glyphs at `ButtonsSection.kt:58-62` are **NOT** migrated — locked exclusion (would expand SHW-06 scope and require grep update).

**Three-theme visual checkpoint (formal v1.1 milestone gate):**
- Single ceremony at end of Phase 6 execution after both deliverables land.
- Order: AeroBlue (default) → AeroDark → Classic. User runs `./gradlew :showcase:run`, eyes-on inspects all three, confirms inline.
- AeroBlue: all 138 icons visible; correct contrast; `"caret"` filters to CaretDown/CaretUp/CaretLeft/CaretRight; clearing restores; `"xyzzy"` shows empty-state; spot-check `X`, `CaretDown`, `MagnifyingGlass`, `FrameCorners`, `Warning`, `Square`; `AeroIconButton` row shows real Phosphor caret/X glyphs.
- AeroDark: all 138 still visible (no black-on-black); `FrameCorners`, `Square`, `Warning` dot hold up at 24dp; `AeroNumberSpinner` disabled-state up/down icons re-eyed (final confirmation, NOT re-decision — Phase 5 already approved 12dp/14dp).
- Classic: same as AeroBlue with Classic palette; Phosphor stroke contrast acceptable at 24dp.
- Outcome: user types `approved` (or describes issue) inline. Approval closes Phase 6 AND closes v1.1 milestone visual sign-off.
- Failure handling: deviation per gsd-executor protocol; no re-spec.

**Click-to-copy:**
- Use `androidx.compose.ui.platform.LocalClipboardManager.current.setText(AnnotatedString("AeroIcons.$name"))` (preferred over raw AWT for Compose Desktop type-safety).
- Toast wording: literal `"Copied AeroIcons.<Name>"`.

**Carry-forward locked decisions:**
- 138 icons (not 139). All 138 enumerated in IconsSection grid.
- Phosphor PascalCase verbatim (`X`, `CaretDown`, `MagnifyingGlass`, `Gear`, `House`, `Funnel`, `EyeSlash`, `FrameCorners`, `Square`).
- Mandatory explicit `tint` on every `Icon()` call.
- `AeroBreadcrumb.separator: String` stays as-is.
- 23-token `AeroColorScheme` is NOT extended.
- `material3.Icon()` directly at all call sites; no custom `AeroIcon()` wrapper.
- `:library` source code is NOT modified — every Phase 6 change is in `showcase/src/`.
- Aero-aesthetic priority: Win7 gloss/gradient/rounded/depth — NOT generic flat.

### Claude's Discretion
- Exact hover-state visual mechanism (`glassEffect` swap vs alpha raise vs `buttonHover` overlay).
- Match-count placement (inline-right of field vs caption-below).
- Empty-state vertical placement (centered vs top-aligned).
- Cell-internal spacing within ~4dp.
- Whether to drop the live filter-count display if implementation friction shows up (not strictly required by SHW-04/05).
- Plan structure: single plan recommended (IconsSection + ButtonsSection migration + closing checkpoint), optionally split into two plans if planner sees a parallelization win.
- **(Resolved by this research, see Don't Hand-Roll below)**: reflection vs hand-authored 138-list — research finding strongly recommends **hand-authored**; reflection over `AeroIcons::class.memberProperties` returns empty because the 138 properties are file-scope extension properties, not class members.

### Deferred Ideas (OUT OF SCOPE)
- Migrate `B I U S` toolbar letter glyphs (would expand SHW-06 scope; v1.2 polish item).
- Categorize icon grid by topic (Arrows / Files / Chrome / Brand) — alphabetical single grid only in v1.1.
- Filter by Phosphor original kebab-case name — rejected, trains PascalCase.
- Tooltip on cell hover showing full identifier — redundant with click-to-copy + toast.
- More elaborate match-count display (progress bar, percentage badge) — over-engineering.
- Compose UI screenshot tests / Paparazzi infra — out of v1.1; eyes-on is the spec.
- Search field debouncing — unnecessary at 138 entries.
- Phosphor source name (`magnifying-glass`) as secondary subtitle — visual noise.
- Click-to-insert into a "scratch" code-preview area — over-engineering vs clipboard.
- Reusable `AeroIconPicker` in `:library` — not a v1.1 deliverable.
- Letting grid expand vertically when search filters down — implementation complexity for cosmetic gain.
- "Open phosphoricons.com" link in empty state — adds external-URL dep.
- `AeroDropdown` popup-offset regression fix — known follow-up, NOT a Phase 6 concern.

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| SHW-04 | `IconsSection` with `LazyVerticalGrid` of all 138 icons (name + render, ≥80dp cell width); passes visual checkpoint in AeroBlue / AeroDark / Classic | `LazyVerticalGrid` block with `GridCells.Adaptive(80.dp)`, fixed `Modifier.height(400.dp)`, `IconCell` composable using `material3.Icon` with `tint = AeroTheme.colors.onSurface` and `Modifier.size(24.dp)`; alphabetized 138-entry hand-authored list (one entry per file in `library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt`); section file inserted at `ShowcaseApp.kt:80` between `FoundationSection` block and `ButtonsSection()`. Three-theme checkpoint procedure documented under Validation Architecture. |
| SHW-05 | `IconsSection` has `AeroSearchField` at top, filters by case-insensitive substring of identifier name in real time; empty result shows "not found" message | `AeroSearchField(value, onValueChange, placeholder = "Search icons")` from `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt`; filter via `remember(query) { ICONS.filter { it.name.contains(query, ignoreCase = true) } }`; empty-state branch renders `Text("No icons match '$query'")`. PascalCase-only match per locked decision. |
| SHW-06 | `ButtonsSection` `▲ ▼ ×` glyphs replaced with `AeroIcons.CaretUp / CaretDown / X` | Three mechanical edits at `ButtonsSection.kt:50-52`; required imports: `androidx.compose.material3.Icon`, `androidx.compose.foundation.layout.size`, `com.mordred.aero.icons.AeroIcons`, plus `com.mordred.aero.icons.internal.{CaretUp, CaretDown, X}` (extension property imports — same pattern Phase 5 used in `AeroSearchField.kt:17-18` and `AeroNumberSpinner.kt:33-34`). Tint: `colors.onSurface`. Size: `Modifier.size(14.dp)`. Verification grep: `grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/` returns 0 hits (currently returns 3 — verified). |

## Standard Stack

### Core (already on classpath; no new deps)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.foundation.lazy.grid` (`LazyVerticalGrid`) | CMP 1.7.3 | Virtualized grid layout | Compose Desktop's standard primitive for grids of arbitrary length; available via `implementation(compose.foundation)` in `showcase/build.gradle.kts:18` |
| `androidx.compose.material3.Icon` | CMP 1.7.3 | Render `ImageVector` with `tint` | Already standard across `:library` (Phase 5 migrations); zero new surface |
| `androidx.compose.ui.platform.LocalClipboardManager` | CMP 1.7.3 (`compose.ui`) | System clipboard via Compose Desktop API | Type-safe, declarative; works on Windows-primary target. Available via `implementation(compose.ui)` in `showcase/build.gradle.kts:19` |
| `androidx.compose.ui.text.AnnotatedString` | CMP 1.7.3 | Required wrapper for `LocalClipboardManager.setText(...)` | Standard CMP API |
| `kotlinx.coroutines` (`rememberCoroutineScope`) | already transitive via `:library` (`libs.kotlinx.coroutines.core`) | Launch suspend `toastState.showToast(...)` from a non-suspending click handler | `AeroToastHostState.showToast` is `suspend` (verified `AeroToastHostState.kt:51`); `rememberCoroutineScope()` is the canonical pattern for "fire-and-forget from a click" |

### Supporting (already on classpath)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `Modifier.glassSurface(cornerRadius = 6.dp)` | `:library` (Phase 1) | Cell resting background | Default for all icon-grid cells |
| `Modifier.glassEffect(cornerRadius, elevation)` | `:library` (Phase 1) | Optional hover-state mechanism | If implementer chooses elevation-based hover lift |
| `AeroSearchField` | `:library` (INP-05) | Search input at top of section | The literal component required by SHW-05 |
| `AeroToastHostState` | `:library` (OVL-05) | Click-to-copy confirmation | Reuses the instance already mounted at `ShowcaseApp.kt:53` |
| `AeroTheme.colors.{onSurface, labelText, onBackground, buttonHover}` | `:library` (Phase 1) | Tint + label + empty-state text + optional hover overlay | Tokens locked at 23; do not extend |
| `AeroTheme.typography.{title, label, body}` | `:library` (Phase 1) | Section header / cell label / empty-state | Same precedent as `FoundationSection.kt:79` and every other section |

### Alternatives Considered (and why rejected)
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `LocalClipboardManager` | `java.awt.Toolkit.getDefaultToolkit().systemClipboard` (raw AWT) | Works as a Linux/macOS fallback if `LocalClipboardManager` misbehaves, but Windows-primary doesn't need it; raw AWT is non-Compose-idiomatic |
| Hand-authored alphabetized list of 138 entries | Reflection over `AeroIcons::class.memberProperties` | **Reflection does not work**: `_X.kt:12` declares `public val AeroIcons.X: ImageVector` as an *extension* property at file-scope. `AeroIcons::class.memberProperties` returns empty; you'd need per-file `Class.forName("com.mordred.aero.icons.internal.XKt")` + `kotlin-reflect` (not on showcase classpath, see `showcase/build.gradle.kts`). Fragile and adds a Gradle dep; hand-authored list is safer. |
| Reflection over `AeroIcons::class` | Code-gen at build time via KSP | Out of scope for v1.1; adds build-tooling complexity for a one-off list |
| Outer scrolling `Column` height-bound on the grid | `heightIn(max = 600.dp)` | Locked at `height(400.dp)` per CONTEXT — fixed bound makes screen real estate predictable |

**Installation:** No new dependencies. All required APIs already on the showcase classpath.

**Version verification:** No package-version checks needed (Phase 6 adds zero new dependencies; CMP 1.7.3 / Kotlin 2.1.21 are pinned and verified by Phases 1–5).

## Architecture Patterns

### Recommended Project Structure (file-modification manifest)

```
showcase/src/main/kotlin/com/mordred/showcase/
├── ShowcaseApp.kt                  # MODIFY: insert IconsSection(...) at line 80; add 1 import
└── sections/
    ├── IconsSection.kt              # CREATE: ~120-180 lines (see Pattern 1)
    └── ButtonsSection.kt            # MODIFY: lines 50-52, three Text(...) → Icon(...)
```

### Pattern 1: IconsSection skeleton (the file to create)

**What:** Single-file section composable with hand-authored 138-entry list, search state, filter derivation, bounded grid, click-to-copy + toast, and empty-state branch.

**When to use:** This is the literal Phase 6 deliverable; the planner may split tasks within it but the structure is fixed.

**Example:**

```kotlin
// Source: derived from CONTEXT.md decisions + verified APIs in
// library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt,
// library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHostState.kt,
// library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt
package com.mordred.showcase.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.input.AeroSearchField
import com.mordred.aero.components.overlay.AeroToastHostState
import com.mordred.aero.icons.AeroIcons
// Required: every used icon needs an explicit extension-property import. See Pattern 2.
import com.mordred.aero.icons.`internal`.Archive
import com.mordred.aero.icons.`internal`.ArrowBendUpLeft
// ... 136 more imports (one per icon) ...
import com.mordred.aero.icons.`internal`.XCircle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassSurface
import kotlinx.coroutines.launch

private data class IconEntry(val name: String, val vector: ImageVector)

// Hand-authored alphabetized list (138 entries). Reflection over AeroIcons::class.memberProperties
// returns empty because these are file-scope extension properties. See "Don't Hand-Roll" §Reflection.
private val ICONS: List<IconEntry> = listOf(
    IconEntry("Archive", AeroIcons.Archive),
    IconEntry("ArrowBendUpLeft", AeroIcons.ArrowBendUpLeft),
    IconEntry("ArrowClockwise", AeroIcons.ArrowClockwise),
    IconEntry("ArrowCounterClockwise", AeroIcons.ArrowCounterClockwise),
    // ... full alphabetized list, 138 entries total ...
    IconEntry("X", AeroIcons.X),
    IconEntry("XCircle", AeroIcons.XCircle),
)

@Composable
fun IconsSection(toastState: AeroToastHostState) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) ICONS
        else ICONS.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Icons", color = colors.onBackground, style = typography.title)

        // Search field + match count
        AeroSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search icons",
            modifier = Modifier.width(400.dp)
        )
        Text(
            text = "${filtered.size} of ${ICONS.size}",
            color = colors.labelText,
            style = typography.label
        )

        // Bounded grid OR empty state
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No icons match '$query'",
                    color = colors.onBackground,
                    style = typography.body
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(80.dp),
                modifier = Modifier.fillMaxWidth().height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.name }) { entry ->
                    IconCell(
                        entry = entry,
                        onClick = {
                            clipboard.setText(AnnotatedString("AeroIcons.${entry.name}"))
                            scope.launch { toastState.showToast("Copied AeroIcons.${entry.name}") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IconCell(entry: IconEntry, onClick: () -> Unit) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    Column(
        modifier = Modifier
            .height(88.dp)
            .glassSurface(cornerRadius = 6.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = entry.vector,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = entry.name,
            color = colors.labelText,
            style = typography.label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
```

### Pattern 2: Extension-property import requirement

**What:** Every `AeroIcons.<Name>` reference at a call site requires an explicit `import com.mordred.aero.icons.internal.<Name>` alongside `import com.mordred.aero.icons.AeroIcons`. Importing only the facade is **not** sufficient — the 138 properties are extension properties at file-scope.

**When to use:** Always, in any file that references `AeroIcons.<Name>`. Phase 5 verified this in `AeroSearchField.kt:17-18`, `AeroNumberSpinner.kt:33-34`, etc. (locked decision in STATE.md `[Phase 05-component-migrations]`: "Explicit `internal.*` extension property imports required alongside `AeroIcons` facade in all migration files").

**Example (ButtonsSection.kt edit):**

```kotlin
// ADD imports:
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretDown
import com.mordred.aero.icons.`internal`.CaretUp
import com.mordred.aero.icons.`internal`.X

// REPLACE lines 50-52:
AeroIconButton(onClick = {}) {
    Icon(AeroIcons.CaretUp, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
}
AeroIconButton(onClick = {}) {
    Icon(AeroIcons.CaretDown, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
}
AeroIconButton(onClick = {}, enabled = false) {
    Icon(AeroIcons.X, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
}
```

For IconsSection.kt's 138-entry list, the planner has two options: (a) add 138 individual imports (verbose but explicit), or (b) use a wildcard `import com.mordred.aero.icons.internal.*`. Wildcard is acceptable here because the section enumerates the entire namespace by design; the verbose form mirrors Phase 5 precedent. Implementer's discretion.

### Pattern 3: ShowcaseApp.kt insertion

**What:** Two edits — one import, one composable call.

```kotlin
// ADD import at top of ShowcaseApp.kt:
import com.mordred.showcase.sections.IconsSection

// REPLACE this block (currently ShowcaseApp.kt:72-79):
Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
        text = "Foundation",
        color = colors.onBackground,
        style = typography.title
    )
    FoundationSection()
}

// ButtonsSection()  ← line 81

// WITH (insert IconsSection between Foundation block and ButtonsSection):
Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
        text = "Foundation",
        color = colors.onBackground,
        style = typography.title
    )
    FoundationSection()
}

IconsSection(toastState = toastState)   // ← NEW

ButtonsSection()
```

### Anti-Patterns to Avoid

- **Unbounded `LazyVerticalGrid` inside the outer scrolling `Column`** — crashes at layout time with "Vertically scrollable component was measured with an infinity maximum height constraint". Mandatory `Modifier.height(400.dp)`.
- **Reflection-based icon enumeration** — `AeroIcons::class.memberProperties` returns empty (extension properties are file-scope, not class members). See Don't Hand-Roll §Reflection.
- **Calling `toastState.showToast(...)` directly from `onClick`** — `showToast` is `suspend`. Use `rememberCoroutineScope()` and `scope.launch { ... }`.
- **Importing only the `AeroIcons` facade and expecting `AeroIcons.X` to resolve** — must also import the corresponding `internal.<Name>` extension property (or use wildcard).
- **Using `LocalContentColor` (default `Icon` tint) instead of explicit `colors.onSurface`** — `AeroTheme` does NOT bridge `LocalContentColor`; result on AeroDark is unpredictable / invisible icons. Locked since Phase 4.
- **Putting the section after `ButtonsSection`** — locked: `IconsSection` is foundational and goes immediately after the Foundation block.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Search input field with magnifier + clear button | New search field component | `AeroSearchField` from `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt` | Already INP-05; built-in clear button + Phosphor magnifier (Phase 5 migrated) |
| Toast confirmation after copy | New toast / overlay | Existing `AeroToastHostState` from `ShowcaseApp.kt:53`; pass as `toastState` param to `IconsSection` | OVL-05 already shipped; one toast host per app (singleton in showcase) |
| System clipboard write | Raw AWT `Toolkit.getDefaultToolkit().systemClipboard` | `LocalClipboardManager.current.setText(AnnotatedString("AeroIcons.$name"))` | Compose-idiomatic, works on Compose Desktop's Windows-primary target |
| Glass cell background | Custom `Brush` + `drawBehind` | `Modifier.glassSurface(cornerRadius = 6.dp)` | FOUND-08 modifier; matches Aero aesthetic |
| Hover lift | New animation primitive | `Modifier.glassEffect(elevation = 2.dp)` swap on hover, OR alpha raise via `colors.buttonHover` overlay | Both already on the surface; pick whichever reads best |
| Filter logic | Debounce / channel / Flow | `remember(query) { ICONS.filter { it.name.contains(query, ignoreCase = true) } }` | 138 entries × `String.contains` is microsecond-fast; `remember(query)` re-derives only on key change — no extra state |
| 138-entry icon list | **Reflection** via `AeroIcons::class.memberProperties` | Hand-authored alphabetized `List<IconEntry>` literal | **VERIFIED**: properties are `public val AeroIcons.X: ImageVector` extension properties at file-scope, not members of `AeroIcons`. `AeroIcons::class.memberProperties` returns empty. Reflective discovery would require per-file `Class.forName("...XKt")` + `kotlin-reflect` (not on showcase classpath; see `showcase/build.gradle.kts`). Hand-authored is the safe choice; the file directory `library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt` is the canonical source — implementer reads `ls` of that directory and types out the alphabetized list. |
| Section header style / typography | Custom font sizing | `Text("Icons", style = typography.title, color = colors.onBackground)` | Every section uses this pattern (Foundation, Buttons, Input, etc.) |
| Cell label style | Custom typography | `typography.label` + `colors.labelText` | Precedent from `FoundationSection.kt:79` |
| Coroutine for `showToast` | Manual suspend | `rememberCoroutineScope()` + `scope.launch { state.showToast(...) }` | Standard Compose pattern for fire-and-forget from a click handler |

**Key insight:** Phase 6 is wiring, not invention. Every component, modifier, and theme token Phase 6 needs is already shipped and verified by Phases 1–5. The only Phase-6-specific code is the 138-entry list literal, the filter expression, the `LazyVerticalGrid` block with `height(400.dp)`, and the click-to-copy plumbing. Resist the urge to build anything else.

## Common Pitfalls

### Pitfall 1: Unbounded LazyVerticalGrid in scrolling parent
**What goes wrong:** `IllegalStateException` / "Vertically scrollable component was measured with an infinity maximum height constraint" at runtime when the showcase scrolls into the IconsSection.
**Why it happens:** `ShowcaseApp.kt` wraps everything in `Column(modifier = Modifier.verticalScroll(...))` (line 60-65). Any nested vertically-scrollable component inside it must declare a bounded height — the outer scroll provides infinite vertical space, and `LazyVerticalGrid` cannot measure itself.
**How to avoid:** Always pass `Modifier.height(400.dp)` (locked) to `LazyVerticalGrid`. Do not pass `fillMaxHeight` or omit height.
**Warning signs:** Crash on first render of IconsSection. Stack trace mentions `LazyVerticalGrid` / `androidx.compose.foundation.lazy`.

### Pitfall 2: Empty extension-property import on call site
**What goes wrong:** "Unresolved reference: X" / "Unresolved reference: CaretDown" at compile time.
**Why it happens:** Importing only `com.mordred.aero.icons.AeroIcons` resolves the receiver but not the extension property. The property lives in `com.mordred.aero.icons.internal.<Name>Kt`; Kotlin needs an explicit import for each used extension property.
**How to avoid:** For `IconsSection.kt` enumerating all 138, use `import com.mordred.aero.icons.internal.*` (wildcard) or list 138 explicit imports. For `ButtonsSection.kt` migration: three imports (`internal.CaretUp`, `internal.CaretDown`, `internal.X`). Pattern is locked since Phase 5 (STATE.md `[Phase 05-component-migrations]`).
**Warning signs:** `compileKotlin` fails on the call site, not on the facade import.

### Pitfall 3: showToast called directly from onClick
**What goes wrong:** "Suspend function 'showToast' should be called only from a coroutine or another suspend function".
**Why it happens:** `AeroToastHostState.showToast` is `suspend` (verified `AeroToastHostState.kt:51`); `onClick` is a regular `() -> Unit`.
**How to avoid:** `val scope = rememberCoroutineScope()` at the top of the composable; call `scope.launch { toastState.showToast(...) }` inside `onClick`.
**Warning signs:** Compile error at the click handler.

### Pitfall 4: Reflection over AeroIcons returns empty
**What goes wrong:** `AeroIcons::class.memberProperties.size == 0` at runtime; the IconsSection grid renders zero cells; debugging takes hours because the facade compiles cleanly.
**Why it happens:** The 138 properties are extension properties on `AeroIcons` declared at file-scope (`public val AeroIcons.X: ImageVector get() = ...` in `internal/X.kt:12`). They're NOT members of `AeroIcons::class`. They live in synthetic `XKt`, `MagnifyingGlassKt`, etc. file classes.
**How to avoid:** Use a hand-authored `List<IconEntry>` literal. Don't try to reflect.
**Warning signs:** Empty grid; logging `AeroIcons::class.memberProperties` shows `[]`.

### Pitfall 5: Tint = LocalContentColor (default) on AeroDark
**What goes wrong:** Icons render invisible or low-contrast on AeroDark / Classic themes.
**Why it happens:** `material3.Icon` defaults `tint = LocalContentColor.current`. `AeroTheme` does NOT bridge `LocalContentColor` (Phase 4 lock). Result is an inherited Material default (often `MaterialTheme.colorScheme.onSurface` from a non-Aero parent or `Color.Black`).
**How to avoid:** Pass explicit `tint = AeroTheme.colors.onSurface` at every `Icon()` call site. Locked across the entire codebase since Phase 4.
**Warning signs:** Icons disappear or look wrong when switching themes; inspector shows `tint = Color.Black` on dark backgrounds.

### Pitfall 6: Phosphor stroke at 24dp on AeroDark glass
**What goes wrong:** Fine-detail icons (`FrameCorners`, `Square`, `Warning` dot) look thin or low-contrast on AeroDark glass backgrounds.
**Why it happens:** Phosphor Regular stroke = 16/256 ≈ 6.25% of viewBox. At 24dp render, that's ~1.5dp stroke; AeroDark's `glassSurface` reduces apparent contrast.
**How to avoid:** Eyes-on at the three-theme checkpoint. If a regression is caught, file a deviation per gsd-executor protocol — most likely fix is a tint-token swap (e.g. `colors.onSurface` → a higher-contrast token), but per Phase 5 visual approval the 24dp size has already cleared this bar.
**Warning signs:** User flags specific icons during the AeroDark portion of the checkpoint.

### Pitfall 7: showcase parallel staging interleaving (carry-forward)
**What goes wrong:** Atomic commits absorb sibling files when waves run in parallel. Documented in STATE.md (Phase 03-05/03-07 sumamries).
**How to avoid:** Phase 6 is two files (`IconsSection.kt` create + `ShowcaseApp.kt` modify) plus one mechanical edit (`ButtonsSection.kt`). If split into two plans, plans are atomic per file; no parallelization conflict. Single-plan execution sidesteps it entirely.

## Code Examples

### LazyVerticalGrid with bounded height + adaptive cells
```kotlin
// Source: androidx.compose.foundation.lazy.grid (CMP 1.7.3)
LazyVerticalGrid(
    columns = GridCells.Adaptive(80.dp),
    modifier = Modifier.fillMaxWidth().height(400.dp),  // mandatory bounded height
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(filtered, key = { it.name }) { entry ->
        IconCell(entry = entry, onClick = { /* copy */ })
    }
}
```

### Filter derivation with `remember(query)`
```kotlin
var query by remember { mutableStateOf("") }
val filtered = remember(query) {
    if (query.isBlank()) ICONS
    else ICONS.filter { it.name.contains(query, ignoreCase = true) }
}
```
`remember(query) { ... }` re-runs the lambda only when `query` changes; same effect as `derivedStateOf` for this case but cheaper / more readable for a pure transform of two stable inputs.

### Click-to-copy + suspend toast
```kotlin
// Source: androidx.compose.ui.platform.LocalClipboardManager (CMP 1.7.3, compose.ui)
//         androidx.compose.runtime.rememberCoroutineScope
//         AeroToastHostState.kt:51 (showToast is suspend)
val clipboard = LocalClipboardManager.current
val scope = rememberCoroutineScope()
// in onClick:
clipboard.setText(AnnotatedString("AeroIcons.${entry.name}"))
scope.launch { toastState.showToast("Copied AeroIcons.${entry.name}") }
```

### Empty-state branch
```kotlin
if (filtered.isEmpty()) {
    Box(
        modifier = Modifier.fillMaxWidth().height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No icons match '$query'",
            color = colors.onBackground,
            style = typography.body
        )
    }
} else {
    // grid
}
```

### Cell with glassSurface + Aero label
```kotlin
// Source: GlassModifiers.kt + FoundationSection.kt:79 caption pattern
Column(
    modifier = Modifier
        .height(88.dp)
        .glassSurface(cornerRadius = 6.dp)
        .clickable(onClick = onClick)
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Icon(
        imageVector = entry.vector,
        contentDescription = null,
        tint = AeroTheme.colors.onSurface,
        modifier = Modifier.size(24.dp)
    )
    Text(
        text = entry.name,
        color = AeroTheme.colors.labelText,
        style = AeroTheme.typography.label,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}
```

### Concrete grep commands the executor + checker will run
```bash
# Pre-migration (must return 3 hits in ButtonsSection.kt:50-52, 0 elsewhere):
grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/

# Post-migration SHW-06 verification (must return 0):
grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/

# Optional sanity (must NOT return 0 — the new Icon calls):
grep -rn "Icon(AeroIcons.CaretUp\|Icon(AeroIcons.CaretDown\|Icon(AeroIcons.X" showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt

# Confirm IconsSection registers in ShowcaseApp:
grep -n "IconsSection" showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt

# Confirm 138-entry list count (count IconEntry( occurrences in IconsSection.kt — must be 138):
grep -c "IconEntry(" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt

# Confirm directory canonical source (ls of internal/ — must list 138 .kt files):
ls library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt | wc -l
# Verified at research time: returns 138.
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Text glyphs (`Text("▲")`, `Text("✕")`) for icon-button mockups | `Icon(AeroIcons.CaretUp / X / ...)` | v1.1 Phase 5 (library) + Phase 6 (showcase) | Cross-theme contrast + stroke discipline; no font-fallback regressions |
| Material Icons Extended (`Icons.Outlined.Search`, etc.) | `AeroIcons.MagnifyingGlass` (Phosphor verbatim) | v1.1 Phase 5 | -36 MB JAR; vocabulary aligns with Phosphor naming |
| Reflection-based "all properties of object X" patterns | Hand-authored alphabetized literal | This phase (Phase 6) | Avoids the extension-property scope blindspot |
| `@Composable` icon-grid sections without bounded height | Mandatory `Modifier.height(400.dp)` for `LazyVerticalGrid` inside scrolling parent | Compose runtime contract | Prevents layout crash |

**Deprecated/outdated:**
- `compose.materialIconsExtended` removed from `:library` Phase 5 — do NOT re-add.
- Custom `Canvas`-drawn icons (`SearchIcon()`, `EyeOpenIcon()`, `EyeClosedIcon()`) deleted Phase 5 — do NOT recreate.

## Open Questions

1. **Hover-state visual mechanism**
   - What we know: CONTEXT marks this as Claude's discretion. Three options exist (`glassEffect` swap, alpha raise, `buttonHover` overlay). All read as Aero-style hover.
   - What's unclear: Which reads best at the visual checkpoint — only eyes-on testing decides.
   - Recommendation: Default to `Modifier.background(colors.buttonHover.copy(alpha = 0.25f))` overlay box on hover state; if that looks flat, swap to `glassEffect(elevation = 2.dp)` lift. Implementer's call at implementation time.

2. **Match-count placement**
   - What we know: CONTEXT marks this as discretion. Inline-right of search field vs caption-below both fit.
   - What's unclear: Cosmetic — depends on field width and surrounding spacing.
   - Recommendation: Caption-below, `typography.label` + `colors.labelText`, separated by 4–6dp from the field.

3. **138-entry list maintenance burden**
   - What we know: Hand-authored is the verified-safe path. Reflection doesn't work without `kotlin-reflect` + per-file `Class.forName` plumbing.
   - What's unclear: If a v1.2 ever expands the icon set, the list must be updated alongside `internal/<Name>.kt` adds.
   - Recommendation: Add a comment at the top of `ICONS` listing block: `// Mirror of library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt — alphabetized. Update both when adding a new icon.` Out of v1.1 scope to automate, but the comment prevents drift.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None for Phase 6 visual deliverables — eyes-on three-theme checkpoint is the spec (locked in CONTEXT.md `### Three-theme visual checkpoint`). Existing JUnit 5 / `kotlin-test` infra in `:library` (`library/build.gradle.kts:21-25`) is unchanged; showcase has no test config (verified `showcase/build.gradle.kts` — no `testImplementation`). |
| Config file | `library/build.gradle.kts` (existing JUnit 5 setup); no showcase test config |
| Quick run command | `./gradlew :showcase:run` (visual surface; the only Phase 6 verification surface) |
| Full suite command | `./gradlew :library:test :showcase:run` (the library test suite must remain green; showcase run is the visual gate) |
| Compile gate | `./gradlew :showcase:compileKotlin` (must succeed; catches missing extension-property imports + LazyVerticalGrid signature errors) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SHW-04 | `IconsSection` exists in `ShowcaseApp.kt` after `FoundationSection` and before `ButtonsSection` | grep + visual | `grep -n "IconsSection" showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` (must return 1+ hits, line number must fall between FoundationSection block end and `ButtonsSection()`); then `./gradlew :showcase:run` for eyes-on | grep: ✅ tools available; visual: manual via `./gradlew :showcase:run` |
| SHW-04 | `IconsSection` enumerates all 138 icons | grep | `grep -c "IconEntry(" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must equal 138); cross-check against `ls library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt \| wc -l` (must equal 138; verified at research time) | grep: ✅ |
| SHW-04 | Each cell uses `Modifier.size(24.dp)` icon + `tint = AeroTheme.colors.onSurface` | grep | `grep -E "Modifier.size\(24.dp\)\|tint = .*onSurface" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must return both patterns) | grep: ✅ |
| SHW-04 | `LazyVerticalGrid` with `GridCells.Adaptive(80.dp)` and bounded `height(400.dp)` | grep | `grep -E "GridCells.Adaptive\(80.dp\)\|height\(400.dp\)" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (both patterns must hit) | grep: ✅ |
| SHW-04 | Three-theme visual sign-off (AeroBlue + AeroDark + Classic) | manual | `./gradlew :showcase:run` → user toggles ThemeSwitcher → user types `approved` (or describes regression) | manual eyes-on, recorded in `06-VERIFICATION.md` |
| SHW-05 | `AeroSearchField` mounted at top of `IconsSection` with placeholder "Search icons" | grep | `grep -E "AeroSearchField\\(.*placeholder" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must hit) | grep: ✅ |
| SHW-05 | Filter derivation uses case-insensitive `contains` on identifier name | grep | `grep "ignoreCase = true" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must hit) | grep: ✅ |
| SHW-05 | Empty-state branch shows "No icons match '<query>'" | grep | `grep "No icons match" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must hit) | grep: ✅ |
| SHW-05 | Real-time filter behavior + clear-button restore + empty-state visible | manual | `./gradlew :showcase:run` → type `caret` (must show CaretDown/CaretUp/CaretLeft/CaretRight) → clear (must restore 138) → type `xyzzy` (must show empty state) | manual eyes-on |
| SHW-06 | `ButtonsSection.kt:50-52` Text glyphs replaced with `Icon(AeroIcons.X)` calls | grep | `grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/` must return 0 hits (currently 3 — verified); `grep -E "Icon\(AeroIcons\.(CaretUp\|CaretDown\|X)" showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` must return 3 hits | grep: ✅ |
| SHW-06 | Visual confirmation: AeroIconButton row shows real Phosphor caret/X glyphs in all three themes | manual | Folded into SHW-04 three-theme checkpoint | manual eyes-on |
| SHW-06 | Tint and size: `colors.onSurface` + `Modifier.size(14.dp)` on each migrated `Icon(...)` | grep | `grep -A1 "Icon(AeroIcons.CaretUp\|Icon(AeroIcons.CaretDown\|Icon(AeroIcons.X" showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt \| grep -E "tint = colors.onSurface\|Modifier.size\(14.dp\)"` (must hit both patterns) | grep: ✅ |
| Compile gate | All edits compile cleanly | automated | `./gradlew :showcase:compileKotlin` must exit 0 | ✅ |
| AeroNumberSpinner regression check (carry-forward verification, not re-decision) | Disabled-state up/down icons remain visible in AeroDark | manual | Folded into SHW-04 AeroDark visual portion (representative sample: switch to AeroDark, scroll to InputSection, observe AeroNumberSpinner) | manual eyes-on |

### Sampling Rate
- **Per task commit:** `./gradlew :showcase:compileKotlin` (must succeed); SHW-06 grep `grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/` after the ButtonsSection edit (must return 0).
- **Per wave merge:** all grep commands above; `./gradlew :showcase:compileKotlin`; running `:library:test` to confirm Phase 5 tests still pass.
- **Phase gate:** full grep suite green; `./gradlew :showcase:run` invoked; user inspects all three themes (AeroBlue → AeroDark → Classic), confirms the eight checkpoint items below, and types `approved` or describes regressions.
- **Minimum visual sampling per theme:**
  - All 138 cells visible after scrolling the bounded grid (count not enforced; eye-on confirmation that the grid shows multiple full pages and no obvious gaps).
  - Spot-check icons with explicit naming: `X`, `CaretDown`, `MagnifyingGlass`, `FrameCorners`, `Warning`, `Square` (six representative shapes covering thin-stroke, dotted, and chrome glyphs).
  - Search behaviors: `caret` (4 results), clear (138), `xyzzy` (empty state), `magnifying` (3 results: MagnifyingGlass, MagnifyingGlassMinus, MagnifyingGlassPlus).
  - Click any icon → toast appears with `"Copied AeroIcons.<Name>"` → paste into a text editor confirms the identifier.
  - `ButtonsSection` row: three real Phosphor glyphs visible (no text characters).
  - On AeroDark only: `AeroNumberSpinner` disabled state up/down icons remain visible (regression check from Phase 5 approval).

### Wave 0 Gaps
- **None.** Phase 6 has no automated test infrastructure to install. The validation surface is:
  - `./gradlew :showcase:compileKotlin` (already configured)
  - grep verification (no tooling needed)
  - eyes-on three-theme checkpoint (already locked workflow per CONTEXT.md)
- The eight grep commands above plus `./gradlew :showcase:run` constitute the full automatable validation; no test files, framework installs, or fixtures are required.

## Sources

### Primary (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — facade declaration (60 lines, empty body); KDoc names the contract (mandatory tint, 16-32dp size range, lazy init)
- `library/src/main/kotlin/com/mordred/aero/icons/internal/X.kt` (and 137 siblings) — extension-property declaration confirms `public val AeroIcons.X: ImageVector` at file-scope; backing `_X` is file-private. Counted via `ls .../internal/*.kt | wc -l = 138` (verified)
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt` — confirms public API `(value, onValueChange, modifier, enabled, placeholder, onSearch)` with built-in clear button (line 73-75); search-field component reused as-is
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHostState.kt` — confirms `showToast` is `suspend` (line 51); `MAX_STACK_SIZE = 5`; FIFO eviction
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` — confirms 12dp icon at 14dp slot (lines 124-156) for AeroDark sub-pixel mitigation already approved Phase 5
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — confirms `glassSurface(cornerRadius)` and `glassEffect(cornerRadius, elevation)` signatures and behavior
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — confirms `toastState` mounted at line 53; insertion point line 80; section ordering
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` — confirms three Text-glyph sites at lines 50-52 (verified by grep) and the locked-exclusion BIUS toolbar at lines 58-62
- `showcase/src/main/kotlin/com/mordred/showcase/sections/FoundationSection.kt` — caption typography precedent (`typography.label` + `colors.labelText`, line 79)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/InputSection.kt` — `var searchValue by remember { mutableStateOf("") }` precedent (line 29)
- `showcase/build.gradle.kts` — confirms `compose.ui` on classpath (line 19), no `kotlin-reflect`, no test config
- `library/build.gradle.kts` — confirms `kotlinx-coroutines-core` is on the library classpath (transitive to showcase via `project(":library")`)
- `.planning/phases/06-showcase-iconssection/06-CONTEXT.md` — locked decisions and discretion areas (literal source for User Constraints section)
- `.planning/REQUIREMENTS.md` §v1.1 SHW-04, SHW-05, SHW-06
- `.planning/STATE.md` `[Phase 05-component-migrations]` entries — confirms internal.* extension property import pattern and AeroNumberSpinner 12dp/14dp visual approval
- `.planning/ROADMAP.md` §Phase 6 — five success criteria + phase notes (bounded-height crash, section ordering, three-theme gate, Phosphor-stroke concern)

### Secondary (MEDIUM confidence)
- Compose Desktop / CMP 1.7.3 documentation for `LazyVerticalGrid`, `GridCells.Adaptive`, `LocalClipboardManager`, `AnnotatedString` — these are stable, documented surfaces in the bundled `androidx.compose.foundation`, `androidx.compose.ui.platform`, `androidx.compose.ui.text` packages already on the project classpath. Direct API verification via Phase 1–5 usage (e.g., `AnnotatedString` already used elsewhere in CMP idioms; `LocalClipboardManager` is the canonical Compose Desktop clipboard surface).
- `kotlinx.coroutines` `rememberCoroutineScope` + `launch` — standard Compose pattern; confirmed transitive via `:library` dep `libs.kotlinx.coroutines.core` in `library/build.gradle.kts:19`.

### Tertiary (LOW confidence)
- None. Every claim in this RESEARCH.md is sourced from a file in this repo or a CMP API already in use.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — every dependency was verified by reading source files in this repo.
- Architecture (IconsSection skeleton): HIGH — derived directly from CONTEXT.md locked decisions plus verified APIs of `AeroSearchField`, `AeroToastHostState`, `LazyVerticalGrid`, `Modifier.glassSurface`.
- Pitfalls: HIGH — pitfall 4 (reflection-fails) verified by reading `internal/X.kt` and confirming the property is file-scope, not a class member; pitfall 1 (unbounded grid crash) is locked in CONTEXT.md and is a documented Compose runtime contract; pitfall 2 (extension-property imports) verified by reading `AeroSearchField.kt:17-18` and `AeroNumberSpinner.kt:33-34` precedent.
- Validation Architecture: HIGH — grep commands and eyes-on procedure all derived from CONTEXT.md and ROADMAP success criteria; no test framework gaps because no test framework is in scope.

**Research date:** 2026-04-29
**Valid until:** 2026-05-29 (Phase 6 is a single-phase wiring exercise on a stable Phase-5 baseline; no upstream churn expected before then)
