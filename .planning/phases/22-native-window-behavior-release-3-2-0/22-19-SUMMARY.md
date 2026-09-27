---
phase: 22-native-window-behavior-release-3-2-0
plan: 19
subsystem: windows-native
tags: [api-04, d-05, ver-11, api-01, api-03, public-api, rememberAeroWindowChrome, hit-test, captions-map, single-native-path]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-07 live-region pipeline (HitTestRegionRegistry, classifyHitTest, onGloballyPositioned publishing); 22-09 AeroMaxButtonInteraction bridge + MaxButtonDirectory; 22-11 full native V11 GREEN on both windows; 22-12 opt-out + markAeroTitleBarInteractive + the permanent -Paero.nativeChrome=false RED control"
provides:
  - "public @Composable FrameWindowScope.rememberAeroWindowChrome(windowState, nativeWindowManagement = true): AeroWindowChromeState in AeroWindowChrome.kt — installs the subclass once per window, wires the native maximize click to the Maximized<->Floating toggle, hands out the shared interaction source — API-04"
  - "public @Stable AeroWindowChromeState: isNative, Modifier.captionArea()/captionExclude()/maximizeButtonArea(), maximizeInteractionSource, maximizeHovered/maximizePressed — several caption areas supported, all modifiers inert when not native, no JNA type in any public signature"
  - "HitTestSnapshot.captions id-keyed map (publishCaption/newCaptionId, copy-on-write); classifyHitTest treats captions entries like the Caption role with unchanged precedence"
  - "AeroTitleBar rebuilt on rememberAeroWindowChrome — one native title-bar path in the library, and it is the public one (D-05); TitleBarRole reduced to Maximize, LEADING_INTERACTIVE_ID removed"
  - "Live proof: VER-11 main 18/18 + narrow 8/8 through the API (identical observed values to 22-12), RED control 0/18+0/8 with pre-phase style, one install per HWND with zero reuse, NC click toggle preserved, title band 0 px diff"
affects: [22-13, 22-15, 22-16, 22-17]

tech-stack:
  added: []
  patterns:
    - "Member-extension modifiers on a @Stable state interface implemented through ONE composed{} factory with a kind parameter — ids allocated once, regions removed on per-modifier dispose"
    - "A component can prove its low-level API by being built ON it (D-05): AeroTitleBar carries no subclass wiring of its own, so the public API inherits the phase's entire live-verification weight instead of adding a second native path"
    - "Semantic uniqueness belongs to the enum (Maximize); multiplicity belongs to id-keyed maps (captions, interactive) — an app can have several caption areas but one maximize rect"

key-files:
  created:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-19-SUMMARY.md
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistry.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestClassification.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "D-05 proof choice executed as recorded in the plan: AeroTitleBar is built on rememberAeroWindowChrome (no separate non-AeroTitleBar showcase window) — reasons: full verification weight on the API, zero extra maintainer hands-off time, generality locked headlessly by Plan 13"
  - "impl class takes (registry?, maxInteraction?, isNative, source) as constructor parameters (plus the two collected states) so Plan 13 can compose it headlessly against a free-standing registry"
  - "maximizeButtonArea publishes the Maximize role (enum — one rect per window, last laid out wins); captionArea/captionExclude publish id-keyed captions/interactive entries; every region self-removes on dispose, replacing the row-level clearing DisposableEffect"
  - "API-04 and VER-11 marked complete; the hover/press PIXEL proof of the maximize button stays with BTN-01 / Plan 13 / Plan 15 (hover cannot hold headlessly — 22-09 finding), the state surface and press path are live-proven here"

requirements-completed: [API-04, VER-11, API-01, API-03]

duration: ~18min
completed: 2026-09-27
---

# Phase 22 Plan 19: Custom title-bar API — rememberAeroWindowChrome — Summary

**The native window wiring became a public, JNA-free API (`rememberAeroWindowChrome` with `captionArea` / `captionExclude` / `maximizeButtonArea` / hover+press state), AeroTitleBar was rebuilt on top of it so the library has exactly one native title-bar path, and that path re-proved the full VER-11 suite (18/18 + 8/8), the opt-out RED control (0/18 + 0/8) and the pixel-identical title band.**

## Performance

- **Duration:** ~18min (started 2026-09-27T20:43:27Z)
- **Tasks:** 3/3
- **Files modified:** 4 library + 1 NOTES (+ this SUMMARY); git-ignored probe scripts/evidence under `.captures/22-apicustom/`
- `./gradlew :library:test --rerun` — AERO_TEST_COUNT total=541 green after Task 1 and after Task 2; `:library:compileKotlin :showcase:compileKotlin` green after both; `git diff --quiet HEAD -- showcase/` held (no showcase edit needed)

## Accomplishments

- **API-04 (Task 1, `d34c4a6`):** public `rememberAeroWindowChrome(windowState, nativeWindowManagement = true): AeroWindowChromeState` in `AeroWindowChrome.kt` — `isNative`, the three marking modifiers, `maximizeInteractionSource` (the window's native-fed bridge source; a plain remembered source on the inert path), `maximizeHovered` / `maximizePressed`. Registry grew the id-keyed `captions` map (`publishCaption` / `newCaptionId`, copy-on-write through the same `AtomicReference`); `classifyHitTest` treats every captions entry exactly like the old Caption role with unchanged precedence (bands → Maximize → interactive → caption → client). No `com.sun.jna` import in the public file (grep-clean).
- **D-05 by construction (Task 2, `d151d75`):** AeroTitleBar calls `rememberAeroWindowChrome` and marks its row `captionArea()`, its buttons `captionExclude()` / `maximizeButtonArea()`, its leading Box `captionExclude()`; its own install effect, `nativeFailed`, direct `WindowRegionsDirectory` / `MaxButtonDirectory` use and max-toggle wiring are deleted (grep: `NativeWindowChromeRegistry` = 0 in AeroTitleBar.kt; exactly one `WindowDraggableArea(` remains, in the legacy branch). `TitleBarRole` is reduced to `Maximize`; `LEADING_INTERACTIVE_ID` is gone (the leading slot allocates its own interactive id).
- **Live proof (Task 3, `d931e06`):** through the public API — GREEN `V11 SUMMARY main pass=18 fail=0` + `narrow pass=8 fail=0` with observed values byte-identical to the 22-12 GREEN run; RED control (`-Paero.nativeChrome=false` into the API's own opt-out) `RED OK` 0/18 + 0/8 at pre-phase `GWL_STYLE 0x960B0000` with zero `AERO_CHROME` lines; exactly one `event=install` per HWND and zero `event=reuse` in both the two-window and single-window runs (no duplicate acquire from the refactor, T-22-34); a posted NC down/up pair at HTMAXBUTTON still toggles `placement` Maximized ↔ Floating (the toggle now lives only in `rememberAeroWindowChrome`); title band + white strip vs `.captures/22-baseline/AeroBlue-rest.png` = 0 differing pixels (D-02). Evidence: `.captures/22-apicustom/` (green/red-control/click-toggle JSON + console logs + `AeroBlue-rest.png`), NOTES section "Custom title-bar API (22-19)".

## Task Commits

1. **Task 1: Public rememberAeroWindowChrome / AeroWindowChromeState (API-04)** - `d34c4a6` (feat)
2. **Task 2: AeroTitleBar rebuilt on the public API (D-05 proof by construction)** - `d151d75` (refactor)
3. **Task 3: Live proof — VER-11 GREEN through the API, opt-out RED control, single install** - `d931e06` (docs)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt` — the public chrome API (plus the unchanged Plan-12 `markAeroTitleBarInteractive`)
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` — rebuilt on the public API; legacy branch byte-identical
- `library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistry.kt` — captions map, `publishCaption`, `newCaptionId`; `TitleBarRole { Maximize }`; `LEADING_INTERACTIVE_ID` removed
- `library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestClassification.kt` — captions entries classify as HTCAPTION; Minimize/Close/Caption role lookups removed
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md` — "Custom title-bar API (22-19)" section (proof choice + results)

## Decisions Made

- The impl class (`AeroWindowChromeStateImpl`) is private and takes `(registry?, maxInteraction?, isNative, source)` plus the two collected states as constructor parameters, per the plan's headless-composability requirement for Plan 13.
- The three modifiers share one `composed{}` factory with a `ChromeRegionKind` parameter (Caption / Maximize / Exclude); `composed` was chosen over `Modifier.Node` exactly as in Plan 12's `markAeroTitleBarInteractive` (no functional gain from Node here).
- `maximizeButtonArea()` also publishes a null rect on dispose (a removed maximize button must stop claiming pixels) — symmetric with the id-keyed kinds, replacing the old row-level clearing effect.
- Requirements: API-04 and VER-11 marked complete (every clause proven live; the hover/press PIXEL proof of BTN-01 remains Plan 13/15's, 22-09's headless-hover limitation stands). API-01 and API-03 were already Complete; this plan re-proved their clauses (showcase compiles byte-unchanged, opt-out RED control through the API's own `nativeWindowManagement`).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None — both compile gates and both full-suite runs (541) were green on the first attempt; all three probe runs produced the expected results on the first invocation (wrapper `.ps1` shape for `-GradleProps`, per the 22-12 pitfall).

## Threat Model Coverage

- **T-22-32 (API leakage):** mitigated — no `com.sun.jna` import in `AeroWindowChrome.kt` (grep empty); the public surface uses Compose/AWT types only; Plan 13's reflection test adds the automated guard.
- **T-22-33 (DoS by misuse):** accepted + documented — KDoc states interactive content inside a caption area must use `captionExclude()`; misuse affects only that app's own window.
- **T-22-34 (duplicate installs):** mitigated — install lifecycle exists only in `rememberAeroWindowChrome` (AeroTitleBar grep-clean of `NativeWindowChromeRegistry`); trace shows one install per HWND and zero reuse.
- **T-22-02 (opt-out regressions):** mitigated — `isNative == false` makes every modifier inert; RED control reproduces pre-phase answers (0/18 + 0/8, `0x960B0000`, zero install traces).

## Known Stubs

None — the API is wired end to end (publishing modifiers → registry → WndProc classification → native click toggle → hover/press states) and proven live through AeroTitleBar.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 13 (headless lock): compose `AeroWindowChromeStateImpl` against a free-standing registry (constructor already parameterized for it); cover the multi-caption / left-hand-maximize generality and the JNA-reflection guard; it raises the locked 541 deliberately.
- Plan 15 (real input): the max-button hover/press frames and the marked/leading real clicks remain the open proof obligations (BTN-01/BTN-02).
- Plans 16-17 (README/KDoc): document `rememberAeroWindowChrome` (REL-06 names it).

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-27*

## Self-Check: PASSED

- All created/modified files exist on disk (SUMMARY, AeroWindowChrome.kt, AeroTitleBar.kt, HitTestRegionRegistry.kt, HitTestClassification.kt); task commits d34c4a6 / d151d75 / d931e06 present in git log; no showcase JVM left running.
