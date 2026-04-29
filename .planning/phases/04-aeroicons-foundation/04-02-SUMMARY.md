---
phase: 04-aeroicons-foundation
plan: 02
subsystem: ui
tags: [icons, phosphor, valkyrie, imagevector, kotlin, compose-desktop, batch]

# Dependency graph
requires:
  - phase: 04-aeroicons-foundation
    plan: 01
    provides: "7-icon spike, Valkyrie 1.1.1 workflow, Shape A confirmed, post-generation fix protocol"
provides:
  - "tools/phosphor-svgs/regular/ — 138 vendored Phosphor Regular SVGs at pinned SHA"
  - "library/src/main/kotlin/com/mordred/aero/icons/internal/ — 138 Valkyrie-generated ImageVector builders (Shape A)"
  - "AeroIcons facade complete — 138 properties accessible via AeroIcons.* autocomplete (extension properties)"
  - ":library:compileKotlin BUILD SUCCESSFUL with all 138 icons under explicitApi()"
affects:
  - "05-migrations — Phase 5 unblocked: all 17 required AeroIcons present (X, CaretDown, CaretUp, CaretRight, Check, CheckCircle, Minus, Square, FrameCorners, MagnifyingGlass, Eye, EyeSlash, Folder, Info, Warning, XCircle, Question)"
  - "06-showcase — AeroIcons full 138-icon set ready for IconsSection"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Shape A extension property pattern at 138-icon scale: each internal/*.kt has public val AeroIcons.<Name>: ImageVector with lazy backing field"
    - "Valkyrie 1.1.1 batch invocation idempotent — same flags as spike; regenerates/creates all icons in one pass"
    - "Post-generation 3-step fix protocol scaled to 138 files: (1) sed defaultWidth 256→24dp, (2) add AeroIcons import, (3) audit fill semantics"
    - "Phosphor icons with genuine filled shapes (circles, dots): fill=SolidColor(Color.Black) is CORRECT — Icon(tint=color) via ColorFilter.tint() overrides all colors including fill"

key-files:
  created:
    - "tools/phosphor-svgs/regular/ (131 new SVGs, 138 total) — all stroke-based, viewBox 0 0 256 256, stroke-width 16"
    - "library/src/main/kotlin/com/mordred/aero/icons/internal/ (131 new .kt files, 138 total) — Valkyrie Shape A, post-fixed"
  modified:
    - "tools/phosphor-svgs/README.md — Plan 04-02 existence verification section added"
    - "library/src/main/kotlin/com/mordred/aero/icons/internal/X.kt — spurious line-path fill removed (re-fixed after Valkyrie regeneration)"

key-decisions:
  - "Shape A confirmed at 138-icon scale — AeroIcons.kt facade remains empty body; no facade edits needed for batch expansion"
  - "Phosphor icons with genuine filled shapes (DotsThree, DotsThreeVertical, Warning, Info, Bug, etc.) correctly retain fill=SolidColor(Color.Black): ColorFilter.tint() replaces this at render time"
  - "Plan 04-02 gate 09 inverse check ('0 Color.Black fills') is incorrect for Phosphor icons with genuine filled elements — 24 files have legitimate Color.Black fills that serve as tintable fill placeholders; actual rendering is correct"
  - "broom.svg: transient SSL connection abort on first fetch; retried once successfully — no substitution needed"
  - "All 3 LOW-confidence SVGs (sort-ascending, sort-descending, spinner) confirmed present at pinned SHA via HTTP 200"

patterns-established:
  - "Valkyrie batch post-processing at scale: sed pipeline for defaultWidth/Height fix + per-file import injection + targeted fill audit"
  - "Fill semantics at 138-icon scale: 115 pure-stroke files (fill omitted = null default), 21 mixed stroke+fill files (Color.Black for genuine shapes), 2 pure-fill files (DotsThree, DotsThreeVertical)"

requirements-completed: [ICN-01, ICN-03]

# Metrics
duration: 35min
completed: 2026-04-29
---

# Phase 4 Plan 02: AeroIcons Batch Summary

**131 Phosphor Regular SVGs bulk-fetched and converted to 138 ImageVector extension properties via Valkyrie 1.1.1; :library:compileKotlin green at full 138-icon scale under explicitApi()**

## Performance

- **Duration:** ~35 min
- **Started:** 2026-04-29T07:32:35Z
- **Completed:** 2026-04-29T08:07:00Z
- **Tasks:** 4 auto + 1 checkpoint:verify
- **Files modified:** 139 (131 new SVGs + 131 new .kt files + 2 updated files: README + X.kt)

## Accomplishments

- All 3 LOW-confidence SVGs (sort-ascending, sort-descending, spinner) confirmed present at pinned SHA via HEAD check — no substitutions needed, target remains 138
- 131 Phosphor Regular SVGs bulk-fetched from raw/regular/ at pinned SHA 2b75f3ad; all confirmed viewBox 0 0 256 256 / stroke-width 16
- Valkyrie 1.1.1 batch run on full 138-SVG directory — "Successfully converted 138 icons"; idempotent on 7 spike files
- Post-generation 3-step fix applied at scale: defaultWidth 256→24dp (138 files), AeroIcons import added (138 files), X.kt spurious fill removed
- Shape A at 138-icon scale: 138 extension properties in internal/*.kt; facade AeroIcons.kt unchanged (empty body + KDoc)
- :library:compileKotlin BUILD SUCCESSFUL — all 138 icons compile under explicitApi()
- All 17 Phase-5-required icons confirmed present

## Task Commits

1. **Task 1: Verify 3 LOW-confidence SVGs** — `ba777c4` (docs)
2. **Task 2: Vendor 131 Phosphor SVGs** — `13b5239` (feat)
3. **Task 3: Generate 131 ImageVector builders via Valkyrie** — `78566d3` (feat)
4. **Task 4: Extend AeroIcons facade** — no-op (Shape A; facade requires no edits)

## Files Created/Modified

- `tools/phosphor-svgs/README.md` — Plan 04-02 existence verification section
- `tools/phosphor-svgs/regular/` — 131 new SVGs (138 total at pinned SHA)
- `library/src/main/kotlin/com/mordred/aero/icons/internal/` — 131 new ImageVector builders (138 total)
- `library/src/main/kotlin/com/mordred/aero/icons/internal/X.kt` — spurious fill removed (re-applied after regeneration)

## Decisions Made

- **Shape A no-op**: AeroIcons.kt facade body is already empty (`public object AeroIcons`). Extension properties in `internal/*.kt` are automatically callable as `AeroIcons.<Name>`. No facade edits at all.
- **Fill semantics (Phosphor mixed icons)**: 24 internal/*.kt files have `fill = SolidColor(Color.Black)` for genuine filled shapes (circles, filled paths). These are NOT errors — they are intentional fills that will be correctly tinted by `Icon(tint=color)` via `ColorFilter.tint()`. Only X.kt's line-path fills are spurious (lines have no fill area).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] broom.svg transient SSL abort on first fetch**
- **Found during:** Task 2 (bulk fetch loop)
- **Issue:** curl reported "schannel: server closed abruptly (missing close_notify)" for broom.svg on attempt 1. Network transient error.
- **Fix:** Retried once; succeeded on attempt 1 of retry.
- **Files modified:** tools/phosphor-svgs/regular/broom.svg
- **Verification:** File present, non-empty.
- **Committed in:** `13b5239` (Task 2 commit)

**2. [Rule 1 - Bug] X.kt spurious line-path fills re-introduced by Valkyrie regeneration**
- **Found during:** Task 3 (fill audit step)
- **Issue:** Valkyrie 1.1.1 regenerated X.kt when running on the full directory, re-adding `fill = SolidColor(Color.Black)` to the two `<line>`-derived paths. This was already fixed in 04-01 spike but Valkyrie re-emits it on regeneration.
- **Fix:** Removed `fill = SolidColor(Color.Black)` from both path blocks in X.kt.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/icons/internal/X.kt`
- **Verification:** X.kt paths have no fill param.
- **Committed in:** `78566d3` (Task 3 commit)

### Plan Gate Deviation (Documentation Only — Not a Bug)

**Gate 09 inverse check ("0 Color.Black fills") does not match reality for mixed-fill Phosphor icons:**
- 24 generated files have `fill = SolidColor(Color.Black)` for genuine filled elements (circles/dots in DotsThree, DotsThreeVertical, Warning, Bug, Info, Key, Lock, etc.)
- These fills are CORRECT: Phosphor Regular has icons with both stroke and fill elements (e.g. warning triangle + dot, info circle + dot, bug with 2 eye circles)
- `Icon(tint=color)` applies `ColorFilter.tint(color)` to the entire vector, replacing ALL colors including fill colors — so `Color.Black` fill IS tinted correctly at render time
- Setting fill=Color.Transparent would make the filled shapes invisible (incorrect rendering)
- Gate 09 positive check (at least 1 transparent-fill per file): 115 pure-stroke files satisfy this by having NO fill param at all (Compose path() fill default = null = transparent). 2 pure-fill files (DotsThree, DotsThreeVertical) do NOT have a transparent-fill statement.
- **Impact:** The gate was written for pure stroke-only Phosphor icons; ~24 icons have legitimate filled shapes. The plan authors knew about Info.kt's fill from the 04-01 spike but wrote the gate as a blanket check. Actual rendering behavior is correct.

---

**Total deviations:** 2 auto-fixed (1 Rule 1 transient network, 1 Rule 1 bug re-introduced by Valkyrie re-generation)  
**Gate deviation:** 1 documentation-only (plan gate incorrectly excludes legitimate Color.Black fills for mixed stroke+fill icons)  
**Impact on plan:** All code is correct and compiles. Gate 09 inverse check wording should be updated to scope to "stroke-only line paths" rather than "all paths".

## Validation Row Status (04-02-01 through 04-02-09)

| Row | Description | Status |
|-----|-------------|--------|
| 04-02-01 | 138 vendored SVGs | GREEN: 138 |
| 04-02-02 | 138 internal/*.kt files | GREEN: 138 |
| 04-02-03 | 138 icons via AeroIcons.* (Shape A extensions) | GREEN: 138 extension props |
| 04-02-04 | 0 viewportWidth=24f hits | GREEN: 0 hits |
| 04-02-05 | >=138 viewportWidth=256f hits | GREEN: 138 hits |
| 04-02-06 | Lazy backing-property pattern | GREEN: 138 lazy fields in internal/ |
| 04-02-07 | 0 eager-init in facade | GREEN: 0 eager hits |
| 04-02-08 | ./gradlew :library:compileKotlin passes | GREEN: BUILD SUCCESSFUL |
| 04-02-09 (a) | 5 random spot-read files — viewport/stroke/caps/join | GREEN: Archive, Bell, Envelope, House, Warning all verified |
| 04-02-09 (b) positive | >=138 transparent-fill hits | NOTE: 115 stroke-only files have fill=null by default (no explicit param); 23 mixed files have some stroke paths (null fill) + some Color.Black fills (genuine). Gate wording assumes explicit null/Transparent string. |
| 04-02-09 (b) inverse | 0 Color.Black fill hits | NOTE: 32 hits across 24 files — ALL are genuine filled shapes (circles, dots) required for correct rendering. See Gate Deviation section above. |
| Gate 10 | All 17 Phase-5 required icons present | GREEN: All 17 confirmed |

## Phase 5 Prerequisite Icons (All Present)

X, CaretDown, CaretUp, CaretRight, Check, CheckCircle, Minus, Square, FrameCorners, MagnifyingGlass, Eye, EyeSlash, Folder, Info, Warning, XCircle, Question

## Issues Encountered

- **Valkyrie re-generates existing files**: Valkyrie regenerates ALL files in the output directory when run on the full 138-SVG directory, including the 7 already-fixed spike files. This means X.kt's spurious fill fix had to be re-applied. Future icon additions should apply the fix protocol to ALL files (idempotent).
- **Plan gate 09 fill check**: See Gate Deviation section above. The gate is too strict for Phosphor icons with genuine filled elements.

## Next Phase Readiness

Phase 5 (Migrations + Dep Removal) is unblocked:
- All 17 Phase-5-required icons present in `library/src/main/kotlin/com/mordred/aero/icons/internal/`
- AeroIcons.* accessible via autocomplete (Shape A extension properties)
- :library:compileKotlin proven green at 138-icon scale under explicitApi()
- ICN-01 (lazy init at 138 scale), ICN-02 (KDoc facade), ICN-03 (138-icon set) all satisfied

---
*Phase: 04-aeroicons-foundation*
*Completed: 2026-04-29*
