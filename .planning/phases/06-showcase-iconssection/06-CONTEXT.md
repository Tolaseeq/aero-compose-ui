# Phase 6: Showcase IconsSection - Context

**Gathered:** 2026-04-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Add `IconsSection` to the showcase: a `LazyVerticalGrid(GridCells.Adaptive(80.dp))` displaying all 138 `AeroIcons.*` constants with identifier labels and a live-filter `AeroSearchField` at the top. Migrate the three remaining text-glyph mockups (▲ ▼ ×) in `ButtonsSection` to `Icon(AeroIcons.CaretUp/CaretDown/X)`. Run the formal three-theme visual checkpoint (AeroBlue + AeroDark + Classic) — this is the milestone-level sign-off gate for the entire v1.1 Icon System.

`IconsSection` is positioned in `ShowcaseApp.kt` AFTER `FoundationSection` and BEFORE `ButtonsSection` (icons are foundational infrastructure; deserve early prominence per ROADMAP phase notes).

**Out of scope for Phase 6 (locked / handled elsewhere):**
- `:library` source code is NOT modified — every Phase 6 change is in `showcase/src/`
- New components / hooks / utilities in `:library` — none required; `AeroSearchField`, `AeroToast`, `AeroToastHost`, glass modifiers, `AeroTheme.colors`/`typography` already cover the surface
- Phosphor SVG re-vendoring or Valkyrie regeneration — Phase 4 territory; complete
- `compose.materialIconsExtended` re-add or any Gradle change — Phase 5 territory; resolved (dep removed)
- `AeroBreadcrumb.separator: String` migration — locked v1.1 exclusion (carried from Phases 4/5)
- `AeroNumberSpinner` sub-pixel mitigation — Phase 5 already resolved at 12dp icon + ≥14dp slot; Phase 6 only re-eyes it inside the AeroDark theme during the milestone checkpoint
- New `AeroColorScheme` tokens — locked at 23 tokens for v1.x
- i18n strings file / localized labels — out of scope per PROJECT.md
- Compose UI screenshot tests / Paparazzi infra — none in v1.1; visual checkpoint is eyes-on
- "Categorize the icon grid by topic" (e.g. arrows / chrome / brand) — out of scope; alphabetical single grid only

</domain>

<decisions>
## Implementation Decisions

### IconsSection cell visual design (discussed)
- **Background:** each cell uses `Modifier.glassSurface(cornerRadius = 6.dp)` for the resting state — produces a subtle Aero glass tile that matches the rest of the showcase aesthetic and the Aero-aesthetic priority (per user memory: lean Win7 Aero, gloss/gradient/rounded/depth — NOT generic flat).
- **Hover state:** on hover, the cell visually lifts via `glassEffect` semantics — implementation discretion (e.g. raise alpha on the existing surface, swap to `glassEffect(elevation = 2.dp)`, or add a 1-frame `colors.buttonHover` overlay box). The point is interactive feedback that reads as an Aero-style hover, not a flat color flip.
- **Click action:** clicking a cell copies the full Kotlin identifier `AeroIcons.<Name>` (e.g. `AeroIcons.MagnifyingGlass`) to the system clipboard via `java.awt.Toolkit.getDefaultToolkit().systemClipboard` (or Compose Desktop's `LocalClipboardManager` — implementer's choice; both are available), then shows an `AeroToast` confirmation: `"Copied AeroIcons.<Name>"`. Reuses the existing `AeroToastHostState` already mounted at the root of `ShowcaseApp` (no new state plumbing). Turns the grid into a developer reference, not just a visual checkpoint.
- **Name label typography:** `AeroTheme.typography.label` (11sp) — matches the showcase convention established in `FoundationSection.kt:79` (`DemoBox` caption uses `typography.label` + `colors.labelText`). Render as `Text(maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)` so long identifiers like `ArrowCounterClockwise`, `MagnifyingGlass`, `WarningCircle` truncate cleanly at the cell width. Full identifier remains accessible via the click-to-copy + toast (the toast shows the full name).
- **Cell sizing:** `GridCells.Adaptive(80.dp)` per ROADMAP. Each cell is `Modifier.height(88.dp)` (24dp icon + 8dp internal gap + ~16dp single-line label + ~20dp split top/bottom padding, with a few dp slack). `Arrangement.spacedBy(8.dp)` on both axes for the grid.
- **Icon size + tint inside the cell:** `material3.Icon(imageVector = aeroIcon, contentDescription = null, tint = AeroTheme.colors.onSurface, modifier = Modifier.size(24.dp))`. `contentDescription = null` because the visible name label conveys identity to assistive tech (decorative role, matching Phase 5's contentDescription convention §"Decorative icons").

### Search field + filter behavior (defaults locked, not separately discussed)
- **Component:** reuse `AeroSearchField` directly at the top of `IconsSection`, full available width (or width-capped at `400.dp` if the showcase column is wide — implementer's call). Placeholder text: `"Search icons"`.
- **Match algorithm:** case-insensitive substring on the **PascalCase Kotlin identifier only** (e.g. typing `caret` matches `CaretDown`, `CaretUp`, `CaretLeft`, `CaretRight`). NO kebab-case fallback — the showcase trains developers to think in `AeroIcons.*` names, which is the API surface they'll use in code. (PROJECT.md and AeroIcons KDoc both make the PascalCase naming convention the canonical one; matching against kebab-case internally would create a parallel mental model.)
- **Filter timing:** real-time per keystroke. No debounce — the in-memory list is 138 entries; substring match on each char is instant.
- **Match-count display:** show `"<filtered> of 138"` next to or below the search field (implementer chooses placement; small text in `typography.label` color `colors.labelText`). Updates live as the user types. ROADMAP/REQUIREMENTS don't require this; it's a small developer-utility add that costs nothing and confirms the filter is working.
- **Empty-state message:** when filter matches 0 icons, show plain text `"No icons match '<query>'"` (the user's literal query, single-quoted, in `colors.onBackground` at `typography.body` size). No illustration, no icon. Minimal — matches the rest of the showcase's plain-text section style.
- **Clearing the field** restores the full 138-icon list (built into `AeroSearchField`'s clear button — no new logic).

### Section framing (defaults locked)
- **Section header:** `Text("Icons", style = typography.title)` matching every other section's header pattern (`ButtonsSection`, `InputSection`, etc. all use just the singular noun). The match-count display next to the search field carries the live count; no `"Icons (138)"` redundancy in the header.
- **No caption / explanatory text** under the header. The icon set, naming convention, and tint requirement are documented in `AeroIcons.kt` KDoc; the showcase is for visual confirmation, not for re-stating contracts.
- **Grid bounded height:** `Modifier.height(400.dp)` (NOT `heightIn(max = 600.dp)`). Fixed 400dp keeps the IconsSection from dominating the showcase's outer scroll — the grid scrolls internally; 400dp shows ~4 rows of cells at any time, enough to confirm visual quality without forcing the user to re-scroll the outer page.
- **Section ordering in `ShowcaseApp.kt`:** `ThemeSwitcher` → `Foundation` (FoundationSection) → **IconsSection (NEW)** → `ButtonsSection` → `InputSection` → … (existing order preserved after IconsSection).

### ButtonsSection migration (SHW-06)
- **Migrate exactly the three text glyphs called out in ROADMAP/SHW-06:** `▲ ▼ ×` (in the `AeroIconButton` row, lines 50-52 of `ButtonsSection.kt`) → `Icon(AeroIcons.CaretUp, ...)`, `Icon(AeroIcons.CaretDown, ...)`, `Icon(AeroIcons.X, ...)`. Each `Icon` call passes `contentDescription = null` (decorative — they're inside `AeroIconButton` which has no semantic action label, so the decorative-null convention from Phase 5 applies here as a showcase-mock; consumers wiring real handlers would supply real contentDescription strings).
- **Tint:** `AeroTheme.colors.onSurface` for all three (matches the existing `Text(..., color = colors.onSurface)` tint in lines 50-52 — pure mechanical replacement, no color drift).
- **Icon size inside `AeroIconButton`:** `Modifier.size(14.dp)` — same dimension Phase 5 used for `AeroToastHost` / `AeroNotificationBanner` close buttons (matches affordance icon convention from Phase 5 size table).
- **`AeroToolbar` row's text-letter glyphs (`B I U S` lines 58-62) are NOT migrated.** Locked exclusion: SHW-06 grep (`grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/`) only checks the four arrow/close glyphs. Letter glyphs (B / I / U / S) are mockups for "bold / italic / underline / strikethrough" toolbar buttons; replacing them with `AeroIcons.TextB` / `TextItalic` / `TextUnderline` / `TextStrikethrough` would expand SHW-06 scope and require a SHW-06 grep update. Stays out of v1.1; captured in deferred ideas.

### Three-theme visual checkpoint (formal v1.1 milestone gate)
- **Trigger:** runs at the end of Phase 6 execution, after `IconsSection` lands AND after `ButtonsSection` migration lands. Single checkpoint covers both deliverables.
- **Procedure:** user runs `./gradlew :showcase:run`, opens the running showcase, and visually inspects in this order:
  1. **AeroBlue** (default-on-launch theme) — confirm: all 138 icons visible in `IconsSection`; correct contrast (`onSurface` tint reads on glassy background); typing `"caret"` filters to `CaretDown / CaretUp / CaretLeft / CaretRight`; clearing restores all 138; an empty filter (`"xyzzy"`) shows the "no icons match" message; spot-check icons render acceptably: `X`, `CaretDown`, `MagnifyingGlass`, `FrameCorners`, `Warning`, `Square` (the last three are ROADMAP-flagged for visual-weight concern on glass backgrounds); ButtonsSection `AeroIconButton` row shows three real Phosphor caret/X glyphs, not text characters.
  2. **AeroDark** — switch via `ThemeSwitcher`. Confirm: all 138 icons remain visible (no black-on-black, no invisible glyphs — `colors.onSurface` resolves to a light-on-dark token in AeroDark); fine-detail icons (`FrameCorners`, `Square`, `Warning` dot) hold up at 24dp; **`AeroNumberSpinner` disabled-state up/down icons** in InputSection re-eyed here for a final confirmation (Phase 5 approved them at 12dp/14dp slot; Phase 6 confirms one more time as part of the milestone gate, NOT as a re-decision).
  3. **Classic** — switch via `ThemeSwitcher`. Confirm: same as AeroBlue but with the Classic palette; Phosphor stroke contrast at 24dp is acceptable on the Classic background.
- **Outcome:** user types `approved` (or describes any issue) inline in the execution session. Approval closes Phase 6 AND closes the v1.1 milestone visual sign-off requirement.
- **Failure handling:** if a checkpoint catches a regression (e.g. an icon renders invisibly in AeroDark), the implementer files a deviation per gsd-executor protocol — NOT a re-spec discussion. Most likely fix: a tint-token swap, captured inline.

### Click-to-copy implementation note
- **Compose Desktop clipboard API:** prefer `androidx.compose.ui.platform.LocalClipboardManager.current.setText(AnnotatedString("AeroIcons.$name"))` over raw AWT — type-safe, declarative, and works in both the JetBrains and Google Compose Desktop branches. AWT `Toolkit.getDefaultToolkit().systemClipboard` is the fallback if `LocalClipboardManager` misbehaves on Linux/macOS, but Windows-primary (per PROJECT.md constraints) uses Compose's manager directly.
- **Toast wording:** literal `"Copied AeroIcons.<Name>"` — short enough to fit in a single-line toast, includes the full identifier so the user can confirm the right name landed in the clipboard.

### Carry-forward from milestone-level locked decisions (NOT re-asked)
- 138 icons (not 139) — Phase 4 count lock; SHW-04 grid renders all 138.
- `AeroIcons.*` naming verbatim from Phosphor PascalCase (X, CaretDown, MagnifyingGlass, Gear, House, Funnel, EyeSlash, FrameCorners, Square) — no rename layer.
- Mandatory explicit `tint` on every `Icon()` call — `AeroTheme` does NOT bridge `LocalContentColor` (Phase 4 KDoc lock; Phase 5 size+tint table lock).
- `AeroBreadcrumb.separator: String` stays as-is — locked v1.1 exclusion.
- 23-token `AeroColorScheme` is NOT extended — locked at v1.x; tints come from existing tokens (`onSurface`, `labelText`, `onBackground`).
- `material3.Icon()` directly at all call sites — no custom `AeroIcon()` wrapper.
- Win11 `transparent=false` rule — irrelevant to Phase 6 (no new window/dialog work) but still in effect for the showcase Main.kt window.
- Aero-aesthetic priority (per user memory) — visual choices lean Win7 Aero (gloss / gradient / rounded / depth), not generic modern-flat. `glassSurface` + hover-lift cell choice honors this.

### Claude's Discretion
- Exact hover-state visual: `glassEffect` swap vs. alpha raise vs. `buttonHover` overlay — picker chooses what reads best at the visual checkpoint.
- Match-count placement: inline next to AeroSearchField (right side) vs. as a small caption directly below the field — both fit; cosmetic.
- Empty-state vertical placement: centered inside the bounded grid box vs. top-aligned — both fit; cosmetic.
- Cell-internal layout: `Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally)` is the obvious pattern; spacing tweaks within ~4dp are at implementer's discretion to match the visual checkpoint.
- The exact list (sorted alphabetically) of `AeroIcons.*` properties enumerated in `IconsSection` — derive at compile time from the facade if a reflective list is clean (`AeroIcons::class.memberProperties` filtered to `ImageVector` returners), OR hand-author the alphabetized 138-entry list inside the section file. Reflection has the maintenance edge (single source of truth = the facade); a hand-authored list has the compile-time-safety edge. Implementer chooses; reflection is mildly preferred since the facade is the canonical list.
- Whether to include the live filter-count display at all if implementation friction shows up (e.g. ugly placement). Not strictly required by SHW-04/05; can be dropped without blocking the milestone gate.
- Plan structure — recommended single plan for Phase 6 covering: (a) IconsSection.kt creation + ShowcaseApp.kt wiring, (b) ButtonsSection.kt three-glyph migration, (c) three-theme visual checkpoint as the closing acceptance step. Optionally split into two plans (IconsSection vs. ButtonsSection migration) if the planner sees a parallelization win — both modify different files so they're independent.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 6 source-of-truth roadmap & requirements
- `.planning/ROADMAP.md` §Phase 6: Showcase IconsSection — 5 success criteria, phase notes (bounded-height crash mitigation, section ordering after FoundationSection, three-theme sign-off gate, Phosphor stroke at 24dp visual concern)
- `.planning/REQUIREMENTS.md` §v1.1 Showcase — SHW-04 (LazyVerticalGrid + visual checkpoint), SHW-05 (AeroSearchField + real-time substring filter + "not found" message), SHW-06 (ButtonsSection ▲▼× migration)
- `.planning/PROJECT.md` §Current Milestone v1.1 — IconsSection target feature; §Constraints — Compose Desktop only, Windows primary; §Out of Scope — i18n explicitly excluded (drives empty-state plain-English wording)
- `.planning/STATE.md` §Accumulated Context `[v1.1 locked decisions]` — 138-icon count lock, naming verbatim, tint discipline, AeroBreadcrumb exclusion; §Known Follow-up — AeroDropdown popup regression (NOT a Phase 6 concern; tracked separately); §Performance Metrics — JAR-size baseline post-Phase-5 (Phase 6 does not change JAR)

### Prior phase context (Phase 4 + Phase 5 contracts feed Phase 6)
- `.planning/phases/04-aeroicons-foundation/04-CONTEXT.md` — §Decisions / Lazy backing-property pattern (Phase 6 reads `AeroIcons.*` properties; instance equality across calls is guaranteed); §KDoc (object-level mandatory-tint warning is the rule Phase 6 follows); §Things Phase 4 must NOT touch (showcase territory — Phase 6 owns it)
- `.planning/phases/04-aeroicons-foundation/04-VERIFICATION.md` — Phase 4 verification artifacts; confirms 138 icons exist + facade compiles + spot-checked 7 icons; the input contract for Phase 6 (icons resolve as `AeroIcons.<Name>` extension properties)
- `.planning/phases/05-component-migrations/05-CONTEXT.md` — §Icon size + tint map (Phase 6 reuses the conventions: affordance → `colors.labelText`, chrome/close → `colors.onSurface`, decorative → `contentDescription = null`); §contentDescription convention (Phase 6 ButtonsSection migration follows the decorative-null rule); §Carry-forward (every milestone-level lock applies); §Things Phase 5 must NOT touch lists `showcase/src/` as Phase 6 territory
- `.planning/phases/05-component-migrations/05-SUMMARY.md` — closes Wave 5 (materialIconsExtended removed); confirms `AeroNumberSpinner` 12dp/14dp slot was approved at visual checkpoint, removing one blocker for Phase 6's milestone gate

### Project research (still authoritative for v1.1)
- `.planning/research/SUMMARY.md` — milestone-level rationale and pitfall summary; §Phase 6 risks (Phosphor stroke at 24dp on glassy backgrounds, FrameCorners/Square visual weight, AeroDark contrast)
- `.planning/research/FEATURES.md` §Part 3 — master 138-icon list (the canonical source for the IconsSection grid; the alphabetized list of properties Phase 6 enumerates)
- `.planning/research/FEATURES.md` §Part 4 — naming convention rationale (Phosphor kebab → PascalCase; Phase 6 search matches the PascalCase form, NOT kebab)
- `.planning/research/ARCHITECTURE.md` §1 — package layout (`AeroIcons` facade in `library/src/.../icons/`); §2 — public API surface (lazy-pattern instance stability, basis for click-to-copy `AeroIcons.<Name>` string formation)
- `.planning/research/PITFALLS.md` §1 — eager init pitfall (Phase 6 must NOT iterate every property at startup; reflection-based list builds during composition, after first paint, are fine since Compose lazy-recomposes); §5 — tint discipline (every `Icon()` in IconsSection passes explicit `tint = colors.onSurface`); §9 — SVG conversion verification (Phase 6 confirms acceptable visual at 24dp during the three-theme checkpoint)
- `.planning/research/STACK.md` — Kotlin 2.1.21, CMP 1.7.3, JDK 17, Gradle 8.14.3 (versions Phase 6 plans inherit; no version changes)

### Existing codebase — Phase 6 modifies
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` lines 80-90 — section ordering; insert `IconsSection()` call AFTER `FoundationSection` block (after line 78-79) and BEFORE `ButtonsSection()` (line 81). One added import.
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` lines 50-52 — three `Text("▲" / "▼" / "×")` sites in the `AeroIconButton` row → replace with `Icon(AeroIcons.CaretUp/CaretDown/X, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))`. Lines 58-62 (BIUS toolbar letters) are intentionally NOT touched (locked exclusion above).

### Existing codebase — Phase 6 reads (no changes)
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — the facade; `AeroIcons::class` for reflection-based property enumeration if Phase 6 chooses that path
- `library/src/main/kotlin/com/mordred/aero/icons/internal/<Name>.kt` × 138 — extension property declarations (Phase 6 imports the facade; extension properties are visible via the facade reference)
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt` — reused at the top of IconsSection (placeholder, value, onValueChange, modifier; no API change needed)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` — `AeroTheme.colors.{onSurface, labelText, onBackground}` and `AeroTheme.typography.{title, label, body}` token references
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — `Modifier.glassSurface(cornerRadius = 6.dp)` for cell background; `Modifier.glassEffect` if hover-state implementation chooses that path
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroToast.kt` and `AeroToastHost.kt` — toast confirmation after click-to-copy; reuses the existing `AeroToastHostState` mounted at the root of `ShowcaseApp.kt:53`
- `showcase/src/main/kotlin/com/mordred/showcase/sections/FoundationSection.kt` lines 75-83 (`DemoBox`) — caption typography precedent (`typography.label` + `colors.labelText`); IconsSection cell labels follow this pattern
- `showcase/src/main/kotlin/com/mordred/showcase/sections/InputSection.kt` — `AeroSearchField` usage example (line 30 declares `var searchValue by remember { mutableStateOf("") }` — Phase 6 mirrors this pattern at the top of IconsSection)

### Compose / platform reference
- `androidx.compose.foundation.lazy.grid.LazyVerticalGrid` + `GridCells.Adaptive(80.dp)` — the grid primitive; mandatory bounded height when nested inside a vertically-scrolling Column (Modifier.height(400.dp) per ROADMAP/this CONTEXT)
- `androidx.compose.material3.Icon(imageVector, contentDescription, modifier, tint)` — every cell + every migrated ButtonsSection site
- `androidx.compose.ui.platform.LocalClipboardManager` — Compose Desktop clipboard API; preferred over `java.awt.Toolkit` for the click-to-copy path
- `androidx.compose.ui.text.AnnotatedString` — wrapper required for `LocalClipboardManager.setText(...)`
- `androidx.compose.ui.text.style.TextOverflow.Ellipsis` — single-line label truncation
- `androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)` — both axes of the grid

### External / upstream sources (already pinned in Phase 4)
- `https://github.com/phosphor-icons/core` — vendored to `tools/phosphor-svgs/regular/`; pinned in `tools/phosphor-svgs/.pin`. Phase 6 does not re-vendor.
- `https://phosphoricons.com` — official Phosphor browser; referenced in `AeroIcons.kt` KDoc.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`AeroIcons.<Name>` (138 properties)** — Phase 4 deliverable; Phase 6 enumerates the full set in the IconsSection grid and references three (`CaretUp`, `CaretDown`, `X`) in the ButtonsSection migration.
- **`AeroSearchField`** — Phase 2 deliverable; reused verbatim at the top of IconsSection. Built-in clear button + magnifier icon (Phase 5 migrated; both are now `AeroIcons.MagnifyingGlass` / `AeroIcons.X` — bonus visual continuity inside the IconsSection's own search field).
- **`AeroToast` / `AeroToastHostState`** — Phase 3 deliverable; reused for click-to-copy confirmation. `AeroToastHost` is already mounted at `ShowcaseApp.kt:95-98` — no new state plumbing.
- **`Modifier.glassSurface(cornerRadius = 6.dp)`** — Phase 1 deliverable; cell background. `Modifier.glassEffect` available if hover-state implementation chooses elevation-based feedback.
- **`AeroTheme.colors.{onSurface, labelText, onBackground}` + `typography.{title, label, body}`** — all tints and text styles Phase 6 references; nothing new in the token scheme.
- **`material3.Icon`** — already on `:library` classpath; same as Phase 5; no Gradle change.
- **`LocalClipboardManager`** — Compose Desktop platform API; standard CMP 1.7.3 surface.

### Established Patterns
- **One section per file under `showcase/src/main/kotlin/com/mordred/showcase/sections/`** — `FoundationSection.kt`, `ButtonsSection.kt`, etc. Phase 6 adds `IconsSection.kt` to this directory.
- **`@Composable fun SectionName()` with no required params** (or one optional `state` param like `OverlaysSection(toastState = …)`) — IconsSection follows the no-param convention; the click-to-copy toast uses `LocalAeroToastHostState` lookup or accepts an explicit `toastState` param mirroring `OverlaysSection`. Implementer chooses; both work.
- **`Text("Section Name", style = typography.title, color = colors.onBackground)` as the first child of every section's outer Column** — Phase 6's IconsSection follows.
- **`Column(verticalArrangement = Arrangement.spacedBy(12.dp))` outer wrapper** — every section uses this; Phase 6 follows.
- **Cell label typography from `FoundationSection.kt:79`** — `Text(text = label, color = colors.labelText, style = typography.label)`. IconsSection grid cells reproduce this pattern.
- **`var X by remember { mutableStateOf("") }` for search/input state** — `InputSection.kt:29` precedent for `AeroSearchField` value state.
- **`showcase` module does NOT carry `explicitApi()`** — section files use bare `fun` without `public` (per Phase 1 lock); Phase 6 follows.
- **`AeroIconButton(onClick = {}) { Icon(AeroIcons.X, ...) }` content-slot pattern** — Phase 5 already established this in `:library`; Phase 6's ButtonsSection migration mirrors it at the showcase level.

### Integration Points
- **No new public API surface anywhere** — Phase 6 modifies `showcase/src/` only; `:library` stays untouched.
- **No new Gradle dependency** — `LocalClipboardManager` is in CMP 1.7.3's `androidx.compose.ui.platform`; already on the showcase classpath.
- **`ShowcaseApp.kt` insertion point** — line 80 (between `FoundationSection()` block ending at line 79 and `ButtonsSection()` at line 81). Single line + one import.
- **Section enumeration order in `ShowcaseApp.kt`** — Phase 6 inserts IconsSection between FoundationSection and ButtonsSection; existing order after that point is preserved (no reordering of Phase 1–3 sections).
- **`AeroToastHostState` reuse** — already constructed at `ShowcaseApp.kt:53` (`val toastState = remember { AeroToastHostState() }`) and passed to `OverlaysSection(toastState = ...)`. Phase 6's `IconsSection` either accepts the same `toastState` param OR uses `LocalAeroToastHostState` if it exists — verify which during planning.

### Things Phase 6 must NOT touch
- Any file under `library/src/` — including `AeroIcons.kt`, `internal/<Name>.kt`, components, theme, modifiers. All are Phase 4/5 territory; complete and locked.
- `library/build.gradle.kts` — Phase 5 finalized at the post-`materialIconsExtended` state; Phase 6 adds nothing to the dependency graph.
- `tools/phosphor-svgs/**` — Phase 4 territory; do NOT re-vendor.
- `.planning/research/**` — read-only authoritative; Phase 6 references but does NOT write.
- `AeroBreadcrumb.kt` `separator: String` — locked exclusion.
- `ButtonsSection.kt` lines 58-62 (toolbar `B I U S` text-letter glyphs) — out of v1.1 SHW-06 scope.

### File-modification manifest (Phase 6 produces)
**Created:**
- `showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` — new file, ~120-180 lines (search field state + filter + LazyVerticalGrid + cell composable + click-to-copy toast wiring + empty state)
- `.planning/phases/06-showcase-iconssection/06-PLAN-*.md` (planning artifacts; ~1-2 plans expected)
- `.planning/phases/06-showcase-iconssection/06-SUMMARY.md` (post-execution)
- `.planning/phases/06-showcase-iconssection/06-VERIFICATION.md` (post-execution; includes the three-theme checkpoint approval record)

**Modified (in place):**
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — insert `IconsSection(...)` call between `FoundationSection` block (line 78-79) and `ButtonsSection()` (line 81); add one import (`import com.mordred.showcase.sections.IconsSection`); if `IconsSection` accepts `toastState`, pass the existing `toastState` instance.
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` — three `Text("▲"/"▼"/"×")` sites at lines 50-52 → `Icon(AeroIcons.CaretUp/CaretDown/X, ...)`; add `import androidx.compose.material3.Icon`, `import androidx.compose.foundation.layout.size`, `import com.mordred.aero.icons.AeroIcons`, and the relevant `internal.*` extension property imports if Kotlin requires them (per Phase 5's pattern).

</code_context>

<specifics>
## Specific Ideas

- **"Aero glass tile" cell aesthetic** — chose `glassSurface` over flat-transparent because the user memory locks an Aero-aesthetic priority (Win7 gloss / gradient / rounded / depth, not modern-flat). The grid should look like a Win7 toolbar palette, not a Material 3 icon picker. Hover-lift via `glassEffect` reinforces the depth metaphor.
- **Click-to-copy as a developer-utility add** — SHW-04/05 don't require it, but it costs ~10 lines of code (clipboard manager + toast state lookup) and turns the showcase into a useful day-to-day reference. Developers building consumer apps will land here, find the icon they want, click, and paste `AeroIcons.MagnifyingGlass` directly into their code. The toast wording (`"Copied AeroIcons.<Name>"`) confirms the full identifier landed.
- **Substring on PascalCase only — no kebab-case fallback** — the user's real workflow is "I'm typing in Kotlin and need autocomplete". The showcase trains them in the API names (`AeroIcons.MagnifyingGlass`). Allowing kebab-case search (`magnifying-glass`) would create a parallel mental model. The KDoc on AeroIcons already documents the kebab→PascalCase mapping; the search box doesn't need to re-implement it.
- **Fixed `height(400.dp)` over `heightIn(max = 600.dp)`** — fixed bound makes the section's screen real estate predictable. The user already scrolls the outer Column to traverse all sections; the inner grid scroll doesn't need extra vertical room to be useful. 400dp comfortably shows ~4 rows of 88dp cells, enough for visual confirmation.
- **Reflection-vs-hand-authored 138-property list — reflection mildly preferred** — `AeroIcons::class.memberProperties.filter { it.returnType.classifier == ImageVector::class }` builds the full list in one line, sorted at .sortedBy { it.name }; single source of truth = the facade. Hand-authoring is compile-time-safe but creates a maintenance gap (add an icon to the facade, forget to add it to the showcase list, ship a 137-of-138 grid). Phase 6 uses reflection unless KSP/profiling shows a startup cost.
- **`ButtonsSection` migration is mechanical** — three line-edits at lines 50-52, plus three imports. No new layout, no new spacing, no visual redesign. The `AeroIconButton` already wraps the content; replacing `Text("▲")` with `Icon(AeroIcons.CaretUp, …)` keeps the button geometry identical (14dp icon size matches the visual footprint of the 8sp text glyph that was there).
- **Three-theme checkpoint as a single ceremony** — runs ONCE at end of Phase 6, not three separate checkpoints. The user toggles ThemeSwitcher, observes, types `approved` (or describes the issue). No screenshot diffing, no separate AeroDark-only sub-checkpoint — Phase 5 already cleared `AeroNumberSpinner`'s AeroDark path; Phase 6 confirms it during the milestone gate.
- **Plan structure** — single plan recommended: (a) IconsSection.kt creation + ShowcaseApp.kt wiring, (b) ButtonsSection migration, (c) closing three-theme visual checkpoint as the final acceptance task. Atomic commits per natural step (carry-forward from Phases 4/5).

</specifics>

<deferred>
## Deferred Ideas

- **Migrate ButtonsSection toolbar `B I U S` letter glyphs to `AeroIcons.TextB / TextItalic / TextUnderline / TextStrikethrough`** — out of literal SHW-06 grep scope (only ▲▼×✕ are checked); migrating would expand the v1.1 spec mid-phase. Captured here so it's not lost; could be a v1.2 polish item or a tiny mini-plan if the user wants the toolbar mockup to feel more "real".
- **Categorize the icon grid by topic** (Arrows / Files / Chrome / Brand / etc.) — out of scope for v1.1 (alphabetical single grid is the spec). Could be a v1.2 enhancement; would require either Phosphor's category metadata or a hand-curated mapping.
- **Filter by Phosphor original kebab-case name** — captured above as a rejected option; the showcase trains PascalCase. Re-visitable if developers report friction (unlikely; the KDoc documents the mapping and autocomplete handles the rest).
- **Tooltip on icon cell hover showing full identifier** — alternative to click-to-copy that was offered during gray-area discussion but not selected. The click-to-copy + toast already shows the full name; hover tooltip would be redundant. Could be added in v1.2 if developers report the truncated label is confusing.
- **Live filter-count badge replaced by a more elaborate "X of 138 visible" display** — the Phase 6 default is a small inline `"X of 138"` in `colors.labelText`. Anything fancier (progress bar, percentage badge) is over-engineering for a 138-icon grid.
- **Compose UI screenshot tests / Paparazzi infra to automate the three-theme checkpoint** — would catch regressions without eyes-on; explicitly out of scope for v1.1 (carried from Phases 4/5 deferred). Re-visitable in v2 if the icon set or visual surface grows substantially.
- **Search field debouncing** — the in-memory list is 138 entries; substring match is microsecond-fast. Debouncing would only add lag. Rejected.
- **Showing the Phosphor source name (`magnifying-glass`) as a secondary subtitle under each cell** — visual noise for negligible gain. The KDoc on `AeroIcons.kt` documents the mapping; cells should focus on the API name.
- **Click-to-insert (write into a "scratch" code-preview area instead of clipboard)** — over-engineering; clipboard is universal and works with any IDE.
- **Adding an "Icon picker" reusable component to `:library`** — out of scope for v1.1; the showcase IconsSection is a one-off display, not a reusable component. If a consumer wants an icon picker, that's a v2+ feature.
- **Allowing the IconsSection grid to expand vertically when search filters down to a small set** (so the grid shrinks to fit and avoids scrolling within scrolling) — implementation complexity for cosmetic gain. Fixed `height(400.dp)` is fine.
- **Adding an "Open phosphoricons.com" link inside the empty state** — useful but adds an external-URL handler dependency; minimal gain for v1.1.
- **`AeroDropdown` popup-offset regression fix** — known follow-up from v1.0 (STATE.md); explicitly NOT a Phase 6 concern; tracked separately for gap-closure.

</deferred>

---

*Phase: 06-showcase-iconssection*
*Context gathered: 2026-04-29*
