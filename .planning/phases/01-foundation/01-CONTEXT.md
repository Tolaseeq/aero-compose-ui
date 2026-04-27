# Phase 1: Foundation - Context

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Establish the Gradle multi-module project structure, the complete AeroColorScheme token system with three built-in themes, AeroTypography, CompositionLocal providers, three glass effect modifiers, `explicitApi()` enforcement on `:library`, and a running `:showcase` skeleton that demonstrates theme switching and the glass modifiers.

Creating components (buttons, inputs, etc.) is Phase 2. AeroTitleBar is Phase 3.

</domain>

<decisions>
## Implementation Decisions

### AeroColorScheme token set
- Clean general-purpose set — strip all mordred-specific fields (connectionActive, connectionInactive, executionHighlight, logBackground, timeStampBackground)
- Keep roughly 20-22 general tokens: primary/onPrimary, surface/onSurface, background/onBackground, error/onError, glassSurface, glassBorder, glassHighlight, panelBackground, borderDefault, borderSelected, labelText, buttonHover, closeButtonHover, cardBackground (+ a few more as needed)
- Declared as `@Immutable data class AeroColorScheme(...)`
- Three built-in presets live as companion object properties: `AeroColorScheme.AeroBlue`, `AeroColorScheme.AeroDark`, `AeroColorScheme.Classic` — with exact hex values from mordred
- Custom themes via standard Kotlin `copy()`: `AeroTheme(colorScheme = AeroColorScheme.AeroBlue.copy(primary = myColor))`
- No DSL or builder — copy() is idiomatic and sufficient

### AeroTypography
- Separate `@Immutable data class AeroTypography(...)` with TextStyle instances: title 18sp bold, bodyLarge 14sp, bodyMedium 13sp, bodySmall 12sp, label 11sp bold
- Accessed via `LocalAeroTypography` CompositionLocal — mirrors how `LocalAeroColors` works
- `AeroTheme {}` sets both `LocalAeroColors` and `LocalAeroTypography`
- Material3 Typography is also configured inside `AeroTheme` (as in mordred) for Material3 component compatibility

### Glass modifier API
- All three modifiers auto-read from `LocalAeroColors.current` internally — no explicit colors parameter required at the call site
- Must be called within `AeroTheme {}` (crashes if not — acceptable constraint, documented in library KDoc)
- Rendering approach: keep `.shadow()` for elevation (unavoidable on Compose Desktop), put gradient fill + border stroke inside a single `drawBehind {}` block — satisfies FOUND-07 (no overdraw from background layering)
- Same rendering pattern across all three, different tokens:
  - `glassEffect` → shadow + gradient(glassSurface) + border(glassBorder) — card/element level
  - `glassPanel` → gradient(panelBackground), no border, no shadow — large section backgrounds
  - `glassSurface` → gradient(glassHighlight → transparent) + border(glassBorder) — mid-level surfaces
- Signature keeps optional `cornerRadius: Dp` and `elevation: Dp` parameters (as in mordred)

### Project / module structure
- Two modules only: `:library` + `:showcase`
- `:library` — `compose.desktop.common` plugin (platform-neutral JAR, not `currentOs`)
- `:showcase` — `compose.desktop.currentOs` plugin (runnable desktop app)
- `:showcase` depends on `:library` via `implementation(project(":library"))` — no local Maven publish needed during development
- All dependency versions in `gradle/libs.versions.toml` (version catalog)
- `explicitApi()` declared in `:library` build script

### Showcase Phase 1 scope
- Standard decorated window (OS chrome) for now — AeroTitleBar arrives in Phase 3 with `FrameWindowScope`
- Top section: theme switcher using plain Material3 SegmentedButton (replaced with AeroSegmentedControl in Phase 2 — one-line swap)
- Foundation section: three demo boxes showing glassEffect, glassPanel, glassSurface with labeled captions — proves theme system works visually
- Below Foundation: empty placeholder sections for future phases (Buttons, Input, etc.) with "coming Phase 2..." labels
- Showcase grows in each phase — Phase 1 establishes the structure, each subsequent phase adds its section

### Claude's Discretion
- Exact set of the ~20-22 token names in AeroColorScheme (stay close to mordred names where general enough)
- Whether to use `.composed {}` or `Modifier.Node` API for the CompositionLocal-reading modifiers
- Exact layout of the showcase Foundation section
- Whether to add a `secondary`/`onSecondary` token or defer to Phase 2 when buttons need it

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Mordred reference implementation
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/ColorScheme.kt` — MordredColorScheme with all 29 token values for AeroBlue, AeroDark, Classic; source of truth for hex values
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/GlassModifiers.kt` — current glassEffect/glassPanel/glassSurface implementation to port and refactor
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/MordredTheme.kt` — CompositionLocalProvider + MaterialTheme integration pattern to replicate as AeroTheme

### Requirements
- `.planning/REQUIREMENTS.md` §Foundation — FOUND-01..10, SHW-01..03 acceptance criteria
- `.planning/ROADMAP.md` §Phase 1 — success criteria (5 items)

### Project constraints
- `.planning/PROJECT.md` §Constraints — tech stack (Kotlin + Compose Desktop, Gradle Kotlin DSL, JDK 17), naming prefix, Material3 wrapping approach

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `mordred/GlassModifiers.kt`: three modifiers to port — logic is solid, needs: (1) rename color type MordredColorScheme→AeroColorScheme, (2) remove explicit colors param, (3) merge .background() + .border() into drawBehind
- `mordred/ColorScheme.kt`: exact hex Color values for all three themes — copy directly into AeroColorScheme companion object, drop domain-specific fields
- `mordred/MordredTheme.kt`: CompositionLocalProvider + MaterialTheme wiring pattern — replicate as AeroTheme with LocalAeroColors + LocalAeroTypography

### Established Patterns
- Material3 as base (not replacement): mordred wraps MaterialTheme inside the provider, components use MaterialTheme.typography and MaterialTheme.colorScheme as fallback
- `staticCompositionLocalOf` for both color and typography locals (mordred uses this correctly)

### Integration Points
- `:library` has no existing code — greenfield. `:showcase` has no existing code — greenfield.
- The only integration is the mordred reference project (read-only, not a dependency)

</code_context>

<specifics>
## Specific Ideas

- Library published as `com.mordred:aero-compose-ui` group:artifact
- Glass effect is a gradient simulation (not real DWM blur via JNI) — this is intentional and documented
- Win11 crash note from STATE.md: when implementing showcase window later, use `undecorated=true` WITHOUT `transparent=true` — prevents EXCEPTION_ACCESS_VIOLATION (issue #3757). This applies to Phase 3 AeroTitleBar, not Phase 1 showcase (which uses standard decorated window).
- After Phase 1, validate whether `undecorated+transparent` crash is fixed in CMP 1.10.3 (noted as a blocker in STATE.md — test in Phase 1 if easy to add)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-foundation*
*Context gathered: 2026-04-27*
