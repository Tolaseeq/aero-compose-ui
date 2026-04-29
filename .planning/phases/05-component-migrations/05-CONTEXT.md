# Phase 5: Component Migrations + Dependency Removal - Context

**Gathered:** 2026-04-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Migrate every text-glyph icon (✓, –, ▼, ▲, ▶, ✕, x, ─, ❒, □) and every `Icons.Outlined.*` reference across the 11 affected `:library` components and their tests to `AeroIcons.*`, then delete `implementation(compose.materialIconsExtended)` from `library/build.gradle.kts` and confirm zero residual `androidx.compose.material.icons` references in `library/src/`.

**Wave order is fixed by ROADMAP** and must be respected:
- **Wave 1** (parallel, internal-only, no API impact): `AeroCheckbox` (MIG-01), `AeroDropdown` (MIG-02), `AeroNumberSpinner` (MIG-03), `AeroToastHost` (MIG-06), `AeroNotificationBanner` (MIG-07), `AeroContextMenu` (MIG-05), `AeroAlertKind` (MIG-10), `AeroBannerKind` (MIG-11)
- **Wave 2** (Canvas-deletion, parallel per file): `AeroSearchField` (MIG-08), `AeroPasswordField` (MIG-09)
- **Wave 3** (isolated private-API restructure): `AeroTitleBar` — `TitleBarButton(glyph: String)` → `TitleBarButton(icon: ImageVector)` (MIG-04)
- **Wave 4** (gate for dep removal): `AeroAlertKindTest.kt` and `AeroBannerKindTest.kt` rewritten to assert `AeroIcons.*` instances (CLN-01) — MUST complete before Wave 5
- **Wave 5** (final): remove `implementation(compose.materialIconsExtended)` from `library/build.gradle.kts` (CLN-02) + grep verification (CLN-03) + JAR-size delta documentation

**Out of scope for Phase 5 (handled elsewhere or locked out of v1.1):**
- Showcase `IconsSection` grid + search — Phase 6 (SHW-04, SHW-05)
- Three-theme formal visual sign-off across all 138 icons — Phase 6 (Phase 5 has only the AeroNumberSpinner inline checkpoint)
- Showcase `ButtonsSection` demo glyphs (▲▼×) → AeroIcons — Phase 6 (SHW-06)
- `AeroBreadcrumb.separator: String` → `ImageVector` — explicitly excluded from v1.1; locked out
- Any change to the 23-token `AeroColorScheme` — locked at current size for v1.x
- Introducing i18n / strings file — out of scope per PROJECT.md "Out of Scope"
- Compose UI screenshot or rendered-size tests — no infra exists; not added in v1.1

</domain>

<decisions>
## Implementation Decisions

### AeroNumberSpinner sub-pixel mitigation (MIG-03)
- **Mitigation: raise inner button slot height ≥ 14dp + render `Icon(AeroIcons.CaretUp/CaretDown)` at `Modifier.size(12.dp)`**. The 12dp icon at stroke 16/256 = ~0.75dp at 96 DPI — above the sub-pixel collapse threshold. Spinner total height grows from current ~32dp to ~32dp (button slot was already ~16dp; 14dp is the minimum guarantee, not an increase from current).
- **Inline visual checkpoint inside MIG-03 plan** — `checkpoint:visual` gate after the migration: user runs showcase, eyes spinner up/down icons in **AeroDark + AeroBlue + Classic** (all three themes), types `approved` or describes issue. No separate prep plan; same-plan-as-MIG-03 is atomic and easy to revert.
- **Acceptance criterion combo (all three required):**
  1. `grep -rn 'Text("▲"\|Text("▼"' library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` returns 0 hits
  2. `grep -n 'Modifier.size(12.dp)' library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` returns at least 2 hits (one per button)
  3. `checkpoint:visual` in AeroDark passes
- **Fallback ladder if visual checkpoint fails at 12dp:** 12dp → 14dp → 16dp. Each step grep-verifiable. The plan documents the ladder upfront so a fallback doesn't require a new plan.
- **Phase 5 owns this checkpoint;** Phase 6's three-theme grid is a separate, milestone-level sign-off — does not replace MIG-03's inline gate.

### Icon size + tint map per migrated call site
**Per-site sizing** (matched to current visual footprint — no unified default; planner writes exact `Modifier.size(Xdp)` per task):

| File | Site | Icon | Size | Tint token |
|------|------|------|------|-----------|
| `AeroCheckbox.kt` | checked-state indicator | `AeroIcons.Check` | `12.dp` | `colors.onPrimary` |
| `AeroCheckbox.kt` | indeterminate-state indicator | `AeroIcons.Minus` | `12.dp` | `colors.onPrimary` |
| `AeroDropdown.kt` | trailing caret | `AeroIcons.CaretDown` | `14.dp` | `colors.labelText` |
| `AeroNumberSpinner.kt` | up arrow (×2 incl. fallback) | `AeroIcons.CaretUp` | `12.dp` (fallback 14→16) | `colors.onSurface` |
| `AeroNumberSpinner.kt` | down arrow | `AeroIcons.CaretDown` | `12.dp` (fallback 14→16) | `colors.onSurface` |
| `AeroContextMenu.kt` | submenu indicator | `AeroIcons.CaretRight` | `12.dp` | `colors.labelText` |
| `AeroTitleBar.kt` | minimize button | `AeroIcons.Minus` | `12.dp` (inside 30dp button) | `colors.onSurface` |
| `AeroTitleBar.kt` | maximize/restore button | `AeroIcons.Square` (when not maximized) / `AeroIcons.FrameCorners` (when maximized) | `12.dp` | `colors.onSurface` |
| `AeroTitleBar.kt` | close button | `AeroIcons.X` | `12.dp` | `colors.onSurface` |
| `AeroToastHost.kt` | close button | `AeroIcons.X` | `14.dp` | `colors.onSurface` |
| `AeroNotificationBanner.kt` | close button | `AeroIcons.X` | `14.dp` | `colors.onSurface` |
| `AeroSearchField.kt` | leading magnifier | `AeroIcons.MagnifyingGlass` | `14.dp` | `colors.labelText` |
| `AeroSearchField.kt` | trailing clear button | `AeroIcons.X` | `14.dp` | `colors.labelText` |
| `AeroPasswordField.kt` | toggle when masked | `AeroIcons.Eye` | `14.dp` | `colors.labelText` |
| `AeroPasswordField.kt` | toggle when visible | `AeroIcons.EyeSlash` | `14.dp` | `colors.labelText` |
| `AeroAlertDialog.kt` (consumes `AeroAlertKind.icon`) | header kind icon | `AeroIcons.Info` / `AeroIcons.Warning` / `AeroIcons.XCircle` (Error) / `AeroIcons.Question` | default 24dp (no `Modifier.size` change) | `accentFor(kind)` (existing call) |
| `AeroNotificationBanner.kt` (consumes `AeroBannerKind.icon`) | leading kind icon | `AeroIcons.Info` / `AeroIcons.Warning` / `AeroIcons.XCircle` (Error) / `AeroIcons.CheckCircle` (Success) | default 24dp (no `Modifier.size` change) | `accent` (existing local) |

**Tint convention rule (formalized — no actual drift from v1.0):**
- Affordance icons (caret, magnifier, clear, eye, submenu indicator) → `colors.labelText`
- Window-control / close-action chrome (TitleBar buttons, Toast/Banner close) → `colors.onSurface`
- Filled-chip indicators (Checkbox check/minus inside primary-colored fill) → `colors.onPrimary`
- Kind-keyed alert/banner icons → `accentFor(kind)` (Phase 3 helper; unchanged)

**No new theme tokens** — `AeroColorScheme` stays at 23 tokens. Tints come exclusively from existing tokens.

### contentDescription convention
- **Decorative icons** → `contentDescription = null`. Decorative = paired with semantic state or label that already conveys meaning to assistive tech.
  - Sites: `AeroCheckbox` (semantic toggleable state covers it), `AeroDropdown` caret (the field is the click target), `AeroContextMenu` submenu indicator (item label provides meaning), `AeroAlertDialog` kind icon (title text), `AeroNotificationBanner` kind icon (banner body conveys severity)
- **Actionable icons** → hard-coded **English imperative verb-noun, sentence case** literal at the call site:
  - `AeroTitleBar` minimize → `"Minimize window"`
  - `AeroTitleBar` maximize → `"Maximize window"`; restore → `"Restore window"`
  - `AeroTitleBar` close → `"Close window"`
  - `AeroToastHost` close → `"Close toast"`
  - `AeroNotificationBanner` close → `"Close notification"`
  - `AeroSearchField` clear → `"Clear search"`
  - `AeroPasswordField` toggle → dynamic by state: when masked → `"Show password"`; when visible → `"Hide password"` (announces action, not state)
- **Where the strings live: inline at call sites** — string literals passed directly to `Icon(contentDescription = ...)`. No `IconStrings` object, no `private const` block, no `strings.xml`. Honors PROJECT.md i18n out-of-scope. Library consumers who need i18n override via `Modifier.semantics`.

### CLN-01 test rewrite (AeroAlertKindTest / AeroBannerKindTest)
- **Assertion: instance match by `AeroIcons.*` reference.** `assertEquals(AeroIcons.Info, AeroAlertKind.Info.icon)` for each kind. Lazy backing-property pattern guarantees the same cached instance on every access; instance equality holds. Direct port of current pattern.
- **Test file imports change** — drop `androidx.compose.material.icons.*`, add `com.mordred.aero.icons.AeroIcons`. No other structural change.
- **Do NOT add a defensive grep-style check** that the Material Icons import is gone — that's CLN-03's job (`grep -rn "androidx.compose.material.icons" library/src/` returns 0). Concerns separated.
- **Test file count covered**: exactly 2 — `AeroAlertKindTest.kt`, `AeroBannerKindTest.kt`. No other test files reference Material Icons (verified by Wave-1 grep before CLN-02).

### JAR-size measurement (success criterion 5)
- **Command:** `./gradlew :library:jar` then `ls -l library/build/libs/library-*.jar` (or `dir` on Windows-native; both yield the byte count).
- **Baseline:** the JAR produced from the **pre-Phase-5** working tree (last commit on `master` before any Wave-1 plan lands). Captured at the START of Wave 5 (after Wave 4 completes), BEFORE removing `materialIconsExtended`. This isolates the dep-removal delta from any incidental size change in Waves 1-4.
  - Procedure: `git stash` → revert `library/build.gradle.kts` to pre-Phase-5 → `./gradlew :library:jar` → record bytes → `git stash pop`. Or: capture the pre-Phase-5 byte count from CI artifact / earlier `master` checkout.
- **Post:** run `./gradlew :library:jar` after CLN-02 lands. Record bytes.
- **Delta expected:** ~6–8 MB savings (per ROADMAP success criterion 5).
- **Where the numbers live:**
  1. **Phase 5 SUMMARY.md** — pre-bytes, post-bytes, delta-bytes, delta-MB. Mandatory.
  2. **STATE.md `Performance Metrics` block** — single line: `JAR size: pre-v1.1 = X MB, post-Phase-5 = Y MB (delta: -Z MB after materialIconsExtended removal)`. Cross-reference to SUMMARY.
- Not in PROJECT.md (decisions doc, not metrics doc).

### Plan structure
- **One plan per wave is the default** — Wave 1 = 1 plan covering all 8 components in parallel sub-tasks (matches "parallel, internal-only, no API impact" from ROADMAP); Wave 2 = 1 plan (2 components); Wave 3 = 1 plan (TitleBar restructure); Wave 4 = 1 plan (2 test files); Wave 5 = 1 plan (Gradle line + grep + JAR-size). Total: ~5 plans.
- **Atomic commits within each plan, one commit per natural step** — same convention as Phase 4. Wave 1 may produce 8 commits (one per migrated component) inside its single plan; the plan's `quality_gate` lists each component's grep-zero check.
- **Commit granularity not separately asked of the user** — carrying forward Phase 4's "one commit per natural step" convention.

### Carry-forward from milestone-level locked decisions (NOT re-asked)
- Wave order: Wave 1+2 → 3 → 4 → 5, **non-negotiable** (ROADMAP).
- `AeroBreadcrumb.separator: String` stays as-is in v1.1 — locked exclusion; planner does NOT touch this parameter.
- `material3.Icon()` directly at all call sites; no `AeroIcon()` wrapper; tint always passed explicitly.
- `AeroIcons.*` naming verbatim from Phosphor PascalCase: `X` (not Close), `CaretDown` (not ChevronDown), `MagnifyingGlass` (not Search), `XCircle` (not ErrorCircle), `Question` (not Help), `FrameCorners` (not RestoreWindow).
- Lazy backing-property pattern is the icon contract — instance equality across calls is guaranteed (basis for CLN-01 test approach).
- 23-token `AeroColorScheme` is NOT extended for icons; tints map only to existing tokens.
- Win11 transparent=false rule is irrelevant to Phase 5 (no window/dialog rework) but still in effect.
- AeroNumberSpinner mitigation deferred from Phase 4 to Phase 5 — closed by this CONTEXT (option A: 12dp + ≥14dp button).
- Phase 4 `Square=maximize / FrameCorners=restore` mapping confirmed; Phase 5 wires the toggle.

### Claude's Discretion
- Exact wording of `contentDescription` strings beyond the verb-noun pattern (e.g., "Close toast" vs "Dismiss toast" — both fit the pattern).
- Visual placement adjustments inside Wave-1 components (e.g., `Modifier.padding` tweaks around the new Icon vs the old Text glyph) when the migration changes glyph footprint.
- Whether the AeroTitleBar `TitleBarButton` private composable's parameter rename is `icon` or `imageVector` (both are valid); ROADMAP says `icon: ImageVector`, prefer that.
- Wave 4 test rewrite — whether each kind gets its own `@Test` method (current pattern, 4 tests per file) or a single parameterized test; keep current pattern unless the test class becomes unwieldy.
- Verification command for JAR-size delta on Windows vs Unix shells (planner picks based on the existing project convention; both bash/MINGW and PowerShell are available per session env).
- Order of components within Wave 1's single plan (alphabetical vs by complexity) — cosmetic.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 5 source-of-truth roadmap & requirements
- `.planning/ROADMAP.md` §Phase 5: Component Migrations + Dependency Removal — 5 success criteria, 5-wave ordering (mandatory), AeroBreadcrumb-exclusion lock, AeroNumberSpinner sub-pixel deferral note
- `.planning/REQUIREMENTS.md` §v1.1 Component Migration (Library) — MIG-01..11 with per-component target icon names; §Dependency Cleanup — CLN-01..03 with verification grep commands and exact `library/build.gradle.kts` line to remove
- `.planning/PROJECT.md` §Current Milestone v1.1 — Phase 5 scope statement; §Constraints — Compose Desktop only, Material 3 base; §Out of Scope — i18n explicitly excluded (drives `contentDescription` stance)
- `.planning/STATE.md` §Accumulated Context `[v1.1 locked decisions]` — wave ordering, AeroBreadcrumb exclusion, AeroNumberSpinner mitigation deferral, all milestone-level locks; §Known Follow-up — AeroDropdown popup regression (Phase 5 does NOT address; tracked for separate gap-closure)

### Phase 4 closure (icons exist; references for the Phase 5 planner)
- `.planning/phases/04-aeroicons-foundation/04-CONTEXT.md` — Phase 4 decisions; §Carry-forward from milestone-level locked decisions; §Things Phase 4 must NOT touch (defines exactly what Phase 5 is now allowed to touch); §Decisions / Verification gates (Phase 4 done criteria — preconditions for Phase 5)
- `.planning/phases/04-aeroicons-foundation/04-RESEARCH.md` — Valkyrie 1.1.1 output format, Shape A (extension properties), per-icon `internal/<Name>.kt` file structure (every migration site references `AeroIcons.<Name>` which is an extension property under the hood)
- `.planning/phases/04-aeroicons-foundation/04-VERIFICATION.md` — Phase 4 verification artifacts; confirms 138 icons + facade + compileKotlin pass + spot-checked 7 icons; the input contract for Phase 5

### Project research (still authoritative for v1.1)
- `.planning/research/SUMMARY.md` — milestone-level rationale and pitfall summary; §Phase 5 risks (sub-pixel, dep-removal sequencing, Material→Phosphor visual swap)
- `.planning/research/FEATURES.md` §Part 3 — master 138-icon list; the 17 icons used in Phase 5 are a strict subset
- `.planning/research/ARCHITECTURE.md` §Migration patterns — call-site replacement strategy reference (Text→Icon, Canvas→Icon, Material→AeroIcons)
- `.planning/research/PITFALLS.md` §1 (lazy init), §5 (tint discipline — every Phase 5 call site MUST pass explicit tint), §11 (explicitApi() surprises — applies to TitleBarButton private fn signature change)
- `.planning/research/STACK.md` — Kotlin 2.1.21, CMP 1.7.3, JDK 17, Gradle 8.14.3 (versions Phase 5 plans reference for dependency removal)

### Existing codebase — sites Phase 5 modifies
- `library/build.gradle.kts` — line 15 contains `implementation(compose.materialIconsExtended)` to remove (CLN-02)
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — facade with object-level KDoc; mandatory-tint warning is the basis for Phase 5's tint map
- `library/src/main/kotlin/com/mordred/aero/icons/internal/{X,Check,Minus,CaretUp,CaretDown,CaretRight,FrameCorners,Square,MagnifyingGlass,Eye,EyeSlash,Info,Warning,XCircle,Question,CheckCircle}.kt` — the 17 icons consumed by Phase 5; all confirmed to exist
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt` lines 97-98 — current Text("✓"/"–") sites
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt` lines 107-111 — current Text("▼") site
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` lines 129, 141 — current Text("▲"/"▼", 8sp) sites; button slot height to enforce ≥14dp
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt` lines 61, 81-83, 121 — `SearchIcon()` Canvas (delete) and `Text("x")` clear (replace with Icon)
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt` lines 121-123, 136-137, 171-172 — `EyeOpenIcon()` / `EyeClosedIcon()` Canvas composables (delete) + call sites
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` lines 106-127 (calls), 136-156 (private `TitleBarButton(glyph: String)`) — Wave-3 restructure target
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt` line 183 — current Text("▶") submenu indicator
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt` line 92 — current Text("✕") close
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt` line 65 (close ✕) and line 55 (kind icon — `imageVector = kind.icon`) — two sites in one file
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt` lines 3-7 (imports), lines 23-26 (mapping) — Material→AeroIcons mapping rewrite
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt` lines 3-7 (imports), lines 20-23 (mapping) — Material→AeroIcons mapping rewrite
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertDialog.kt` lines 53-57 — consumer of `AeroAlertKind.icon`; default Icon size 24dp (no change required)
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt` lines 3-7, 21-36 — Wave-4 rewrite target
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt` lines 3-7, 21-36 — Wave-4 rewrite target

### Compose / Material reference
- `androidx.compose.material3.Icon` — accepts `imageVector: ImageVector`, `contentDescription: String?`, `modifier: Modifier`, `tint: Color`. Default size = 24dp (when no `Modifier.size` is applied). The contract Phase 5's tint map and contentDescription convention are written against.
- `androidx.compose.foundation.layout.Modifier.size(Dp)` — the per-call-site sizing modifier; planner writes exact dp values from the size table above.
- `androidx.compose.ui.semantics.Modifier.semantics` — the override path for library consumers who need to localize `contentDescription` (mentioned for completeness; library does NOT use it itself).

### External / upstream sources (already in Phase 4)
- `https://github.com/phosphor-icons/core` — vendored to `tools/phosphor-svgs/regular/`; pinned in `tools/phosphor-svgs/.pin`. Phase 5 does not re-vendor; Phase 4 closed this surface.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`AeroIcons.<Name>` (138 properties)** — Phase 4 deliverable; 17 used by Phase 5: X, Check, Minus, CaretUp, CaretDown, CaretRight, MagnifyingGlass, Eye, EyeSlash, Info, Warning, XCircle, Question, CheckCircle, FrameCorners, Square. All confirmed to exist as `internal/<Name>.kt` extension property files.
- **`material3.Icon(...)`** — already on the `:library` classpath via existing `material3` dependency; no new Gradle dep needed when removing `materialIconsExtended`. Phase 5 only REMOVES; doesn't add anything to `library/build.gradle.kts`.
- **`AeroTheme.colors.{onPrimary, labelText, onSurface}`** — existing 23-token scheme provides every tint Phase 5 needs.
- **`accentFor(kind)`** — Phase 3 @Composable helper in AeroAlertKind/BannerKind area; unchanged in Phase 5; consumers (AeroAlertDialog, AeroNotificationBanner) keep using it.
- **`@Composable` private fns inside components** — pattern already established (TitleBarButton, EyeOpenIcon, EyeClosedIcon, SearchIcon); Phase 5 deletes the Canvas-based ones and modifies TitleBarButton's signature.

### Established Patterns
- **explicitApi() throughout `:library`** — every public declaration carries explicit `public`. Wave 3's `TitleBarButton(icon: ImageVector)` stays `private`, so explicitApi doesn't constrain it; the public `AeroTitleBar` signature is unchanged.
- **One file per component** — components live at `components/<category>/<Component>.kt`; Phase 5 modifies in place, no new files in `components/`. AeroAlertKind/BannerKind enums also stay in their existing files.
- **Tests collocated by package** — `library/src/test/kotlin/.../components/overlay/AeroAlertKindTest.kt` mirrors the component's package; CLN-01 rewrites in place.
- **`Icon(contentDescription = null)` for decorative icons** — already used in `AeroAlertDialog.kt:55` and `AeroNotificationBanner.kt:55` and `AeroContextMenu.kt:139,175`. Phase 5 extends this pattern to the new decorative sites (Checkbox, Dropdown, ContextMenu submenu).
- **Imperative grep-verifiable acceptance criteria** — Phase 4's plan structure; Phase 5 plans must continue this style. Every wave-acceptance is a grep command + (for Wave 1) a checkpoint:visual where required.
- **One commit per natural step inside a plan** — Phase 4 convention; carries forward to Phase 5 (Wave 1 = ~8 commits in one plan).

### Integration Points
- **No new public API surface in :library** — every Phase 5 change is at the call-site level (private composables, internal layout adjustments, signature change of a private fn). Public AeroCheckbox/Dropdown/Spinner/etc. signatures are unchanged. Library consumers see zero diff.
- **No theme/token wiring change** — `AeroColorScheme` untouched; Phase 5 only references existing tokens.
- **No Gradle dependency add** — only the removal in Wave 5. `compose.materialIconsExtended` is the ONLY line removed from `library/build.gradle.kts`. `compose.material3` stays (provides `Icon()`).
- **Showcase is unchanged in Phase 5** — Phase 6 owns the IconsSection. Phase 5 does NOT touch any file under `showcase/src/`.

### Things Phase 5 must NOT touch
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumb.kt` `separator: String` parameter — locked exclusion from v1.1 migration.
- `library/src/main/kotlin/com/mordred/aero/icons/**` — Phase 4 territory; complete; do not modify.
- `tools/phosphor-svgs/**` — Phase 4 territory; complete; do not re-vendor.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — locked at 23 tokens.
- Any file under `showcase/src/` — Phase 6 territory.
- `.planning/research/**` — already authoritative; Phase 5 reads, does not write.

### File-modification manifest (Phase 5 produces)
**Modified (in place, no new files):**
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt` (Wave 1 — MIG-01)
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt` (Wave 1 — MIG-02)
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` (Wave 1 — MIG-03 + button slot ≥14dp)
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt` (Wave 1 — MIG-05)
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToastHost.kt` (Wave 1 — MIG-06)
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroNotificationBanner.kt` (Wave 1 — MIG-07; close button + kind-icon path)
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt` (Wave 1 — MIG-10)
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroBannerKind.kt` (Wave 1 — MIG-11)
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt` (Wave 2 — MIG-08; delete `SearchIcon()` Canvas, replace `Text("x")`)
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt` (Wave 2 — MIG-09; delete `EyeOpenIcon()` / `EyeClosedIcon()` Canvas)
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` (Wave 3 — MIG-04; `TitleBarButton(glyph: String)` → `TitleBarButton(icon: ImageVector)` + 3 call sites)
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt` (Wave 4 — CLN-01)
- `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt` (Wave 4 — CLN-01)
- `library/build.gradle.kts` (Wave 5 — CLN-02; delete one line)

**Created:**
- `.planning/phases/05-component-migrations/05-PLAN-*.md` (planning artifacts; ~5 plans expected)
- `.planning/phases/05-component-migrations/05-SUMMARY.md` (post-execution; includes JAR-size delta)
- `.planning/phases/05-component-migrations/05-VERIFICATION.md` (post-execution)

**Deleted (private composables inside existing files):**
- `SearchIcon()` private fn in `AeroSearchField.kt`
- `EyeOpenIcon()` private fn in `AeroPasswordField.kt`
- `EyeClosedIcon()` private fn in `AeroPasswordField.kt`

</code_context>

<specifics>
## Specific Ideas

- **Spinner mitigation choice rationale** — option A (12dp Icon + ≥14dp button) keeps the migration uniform across all 11 components: every glyph site becomes a `material3.Icon()` call with explicit size + tint + contentDescription. No per-component exception. The button slot constraint is a one-line `Modifier.heightIn(min = 14.dp)` change that preserves the spinner's existing visual height (button slot was already ≥14dp at default field height of 32dp).
- **Tint convention is "formalized current state, not redesigned"** — every per-site tint listed in the size+tint table matches what the corresponding Text or Canvas already uses today. Migration is a pure mechanical replacement at the tint level. This minimizes regression risk; visual checkpoints only need to confirm icon shape, not color drift.
- **AeroTitleBar maximize/restore icon — "shows the action"** — when not maximized, button shows `Square` (action: "click here to maximize, becoming a square"); when maximized, button shows `FrameCorners` (action: "click here to restore, framing the corners back to a window"). Matches Win11/macOS chrome convention; matches Phase 4 CONTEXT confirmation; matches user's Aero-aesthetic preference (Win7 chrome was already this direction).
- **Why no new strings file or i18n seed** — PROJECT.md "Out of Scope" lists i18n explicitly. Adding even a tiny `IconStrings` object now creates an inconsistent state (other library hard-coded strings remain inline) and would need a follow-up sweep. Cleaner: keep all library hard-coded English inline until v2 introduces i18n properly.
- **Why instance-match for CLN-01 tests, not name-match** — `AeroIcons.Info` is a property accessed via the lazy backing-property pattern; on first access it creates the `ImageVector` and caches in `_Info`; on every subsequent access it returns the SAME instance. So `assertEquals(AeroIcons.Info, x)` is identity-stable — as stable as `assertEquals(Icons.Outlined.Info, x)` was before. Name-match would also work but adds a layer of indirection (.name lookup) for no benefit.
- **JAR-size baseline isolation** — taking the baseline at the start of Wave 5 (after Waves 1-4 complete) rather than from pre-Phase-5 master means the delta cleanly attributes to dep removal alone. Migration of glyphs to Icons doesn't materially change source file size; the dep removal is what shrinks the JAR.
- **Spinner inline checkpoint timing** — the visual gate happens DURING Wave 1 (before Waves 4-5 complete). If the spinner fallback ladder needs to escalate, the planner has freedom to do so within Wave 1 without blocking Wave 5's dep removal. The only Wave-4-blocking concern is CLN-01 test rewrite; the spinner is not on the critical path for dep removal.

</specifics>

<deferred>
## Deferred Ideas

- **Compose UI screenshot tests** — would catch visual regressions but require significant test infra setup (RoboTester, Paparazzi, or compose-multiplatform-test-screenshot). Not justified for a 17-icon migration; current grep + visual checkpoint is sufficient. Revisit in v2 if the icon set or visual surface grows substantially.
- **AeroDropdown caret rotation animation when opened** — a separate visual delight; unrelated to migrating the glyph; could be a v1.2 polish item.
- **i18n strings file / `Strings` object** — explicitly out of scope per PROJECT.md. Will require its own milestone if/when the project goes multi-locale.
- **Renaming `AeroAlertKind.Question` to `AeroAlertKind.Help`** — Phosphor name is `Question`, matches naming convention; locked.
- **Per-property KDoc on `AeroIcons.*` properties** — Phase 4 deferred; not relevant to Phase 5.
- **Adding new icons mid-migration** if a new chrome site is discovered — should not happen; the 17 icons cover all 11 component sites. If a gap surfaces during execution, raise as a deviation per gsd-executor protocol; do NOT add icons inline.
- **AeroDropdown popup offset regression** (from v1.0 STATE.md "Known Follow-up") — out of scope for Phase 5; tracked separately for gap-closure plan.
- **AeroNumberSpinner Canvas-draw fallback** — option B from ROADMAP (Canvas-drawn solid triangles) is the LAST step of the fallback ladder, not the primary plan. Documented but not pre-implemented.
- **Adding `iconAffordance` / `iconChrome` / `iconOnFilled` tokens to `AeroColorScheme`** — out of scope; PROJECT.md locks token count at v1.x.
- **Localizing `contentDescription` strings via `Modifier.semantics` override at consumer level** — left to the library consumer; documented stance in this CONTEXT, not a deliverable.
- **JAR-size measurement via `:library:dependencies` size-summing** — rejected; inferior to direct JAR file inspection.
- **Placing JAR-size delta in PROJECT.md** — rejected; PROJECT.md is for scope/decisions, not metrics. STATE.md Performance Metrics is the right home.
- **Test for Material Icons import absence inside the test files themselves** — redundant with CLN-03 grep over `library/src/`; rejected.

</deferred>

---

*Phase: 05-component-migrations*
*Context gathered: 2026-04-29*
