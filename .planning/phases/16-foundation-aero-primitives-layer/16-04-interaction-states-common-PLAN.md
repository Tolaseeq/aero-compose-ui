---
phase: 16-foundation-aero-primitives-layer
plan: 04
type: execute
wave: 1
depends_on: []
files_modified:
  - library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt
  - library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt
autonomous: true
requirements: [PRIM-15]
must_haves:
  truths:
    - "InteractionStates.kt lives in components/common/ under package com.mordred.aero.components.common (moved from components/buttons/), so a switch/list/range component no longer imports interaction state from the buttons package (PRIM-15)"
    - "rememberAeroInteractionState(source) returns an AeroInteractionState bundling hovered/pressed/focused booleans — a single connection point for all eight target components (PRIM-15)"
    - "rememberAeroInteractionState returns booleans only, never resolved colors/styles — each component picks its own AeroSurfaceStyle variant from the booleans"
    - "AeroButton, AeroOutlinedButton, and AeroIconButton still compile and their existing hover/press/focus behavior is unchanged after the package move (imports updated)"
    - "The existing rememberHoverState/rememberPressedState/rememberFocusState/animatedAlpha helpers and ANIMATION_DURATION_MS remain available (relocated verbatim, still internal/module-scoped)"
  artifacts:
    - library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
    - library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt
  key_links:
    - "rememberAeroInteractionState -> collectIsHoveredAsState/collectIsPressedAsState/collectIsFocusedAsState (the booleans Phase 17-19 components consume)"
    - "AeroButton/AeroOutlinedButton/AeroIconButton import path update (the only in-repo consumers of the moved helpers)"
  prohibitions:
    - { statement: "MUST NOT have rememberAeroInteractionState resolve colors or AeroSurfaceStyle variants — it returns booleans only; a second shared abstraction that picks per-component fill/rim is the drift risk to avoid (RESEARCH Pattern 4)", flagged: true }
    - { statement: "MUST NOT invent pointer-position tracking for hover — the canonical wiring is Modifier.hoverable + collectIsHoveredAsState (AeroListItem precedent, VLST-04); the KDoc must reference it", flagged: true }
    - { statement: "MUST NOT change the behavior or signatures of AeroButton/AeroOutlinedButton/AeroIconButton — this is a package relocation + additive helper, not a restyle (restyles are Phase 17)", flagged: true }
    - { statement: "MUST NOT leave a duplicate InteractionStates.kt in components/buttons/ — the old file is moved, not copied (one source of truth)", flagged: true }
  assumptions:
    - "PRIM-15 edge derived from 16-RESEARCH.md (Architecture Q4, Pattern 4) + 16-PATTERNS.md (full current file content, all helpers internal/module-scoped so relocation needs no visibility change), not the edge-probe (all 18 PRIM rows unclassified) — flagged."
    - "components/common/ does not exist yet; this plan creates the package directory. Confirmed by directory listing this session."
---

<objective>
Relocate InteractionStates.kt from components/buttons/ to a new components/common/ package and extend it with rememberAeroInteractionState() — one hover/press/focus connection point for all eight target components.

Purpose: Phase 19's AeroSwitch/AeroSegmentedControl and Phase 18's sliders need interaction state; importing it from the buttons package is the friction PRIM-15 removes. Additive and behavior-preserving.
Output: components/common/InteractionStates.kt (moved + extended), updated imports in the three button files, and an InteractionStatesTest.kt.
</objective>

<execution_context>
@$HOME/.claude/gsd-core/workflows/execute-plan.md
@$HOME/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/16-foundation-aero-primitives-layer/16-CONTEXT.md
@.planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md
@.planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md
</context>

<artifacts_this_phase_produces>
This plan (16-04) produces:
- `library/.../components/common/InteractionStates.kt` (package com.mordred.aero.components.common) with existing helpers relocated verbatim
- `internal data class AeroInteractionState(val hovered, val pressed, val focused: Boolean)`
- `@Composable internal fun rememberAeroInteractionState(source: InteractionSource): AeroInteractionState`
- `InteractionStatesTest.kt` (runComposeUiTest) proving the booleans track hover/press/focus
</artifacts_this_phase_produces>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Move InteractionStates to components/common, add rememberAeroInteractionState, fix imports</name>
  <files>
    library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt,
    library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt,
    library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt,
    library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt,
    library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt
  </files>
  <read_first>
    - library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt (the full 34-line file to relocate: package line, imports, ANIMATION_DURATION_MS, rememberHoverState/rememberPressedState/rememberFocusState/animatedAlpha — all internal)
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt (call sites of the helpers — currently same-package, no import needed; after the move they need explicit imports from components.common)
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt (same)
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt (same)
    - library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt (lines 6-10, 54, 57, 74: the canonical Modifier.hoverable + collectIsHoveredAsState pairing to cite in the KDoc)
    - .planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md (Pattern 4 shape for rememberAeroInteractionState; note it returns booleans only)
  </read_first>
  <behavior>
    - After the move, grep finds InteractionStates.kt only under components/common/, not components/buttons/.
    - rememberAeroInteractionState wired to a MutableInteractionSource driven by hoverable/clickable reports hovered=true when the pointer enters and focused=true when the element gains focus (verified via runComposeUiTest).
    - AeroButton/AeroOutlinedButton/AeroIconButton compile with updated imports and behave identically.
  </behavior>
  <action>
    Create library/.../components/common/InteractionStates.kt with the relocated content: change the package declaration to com.mordred.aero.components.common, keep all imports and the existing internal helpers (ANIMATION_DURATION_MS, rememberHoverState, rememberPressedState, rememberFocusState, animatedAlpha) verbatim, and delete the old components/buttons/InteractionStates.kt. Append internal data class AeroInteractionState(hovered, pressed, focused: Boolean) and @Composable internal fun rememberAeroInteractionState(source: InteractionSource) that collects the three booleans via collectIsHoveredAsState/collectIsPressedAsState/collectIsFocusedAsState and returns them; add a KDoc pointing to AeroListItem's Modifier.hoverable + collectIsHoveredAsState pairing as the wiring precedent (do not resolve colors here). Add the required imports of the moved helpers (com.mordred.aero.components.common.*) to AeroButton.kt, AeroOutlinedButton.kt, and AeroIconButton.kt. Add InteractionStatesTest.kt under components/common using runComposeUiTest to toggle hover/press/focus on a test element and assert AeroInteractionState reflects each.
  </action>
  <verify>
    <automated>./gradlew :library:test --tests "com.mordred.aero.components.common.InteractionStatesTest" && ./gradlew :library:test</automated>
  </verify>
  <acceptance_criteria>
    - `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` exists with `package com.mordred.aero.components.common`; the old `components/buttons/InteractionStates.kt` no longer exists (grep/glob confirms a single file).
    - The file declares `data class AeroInteractionState(` and `fun rememberAeroInteractionState(`; grep confirms the function body returns booleans and does not construct any Color/AeroSurfaceStyle.
    - AeroButton.kt / AeroOutlinedButton.kt / AeroIconButton.kt import the helpers from `com.mordred.aero.components.common` and compile.
    - InteractionStatesTest passes; the full `./gradlew :library:test` suite stays green (no behavior regression on the buttons).
  </acceptance_criteria>
  <done>InteractionStates.kt is relocated to components/common with rememberAeroInteractionState added, the three button files import from the new package, tests pass, and the full suite is green; committed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| (none) | Interaction-state collection (booleans) only; no external input, network, or persistence. |

## STRIDE Threat Register (ASVS L1)

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-16-05 | (none applicable) | rememberAeroInteractionState | low | accept | A behavior-preserving package relocation of a boolean state collector presents no authentication/session/input/crypto surface (ASVS L1); documented explicitly rather than fabricating a threat. |

No high/critical threats apply; no blocking security gate required (ASVS L1, block-on-high).
</threat_model>

<verification>
- `./gradlew :library:test --tests "com.mordred.aero.components.common.InteractionStatesTest"` green.
- Full `./gradlew :library:test` green (button behavior regression guard).
</verification>

<success_criteria>
InteractionStates.kt is a single file under components/common with rememberAeroInteractionState added (booleans only), the three button consumers compile against the new package with unchanged behavior, and the full suite stays green.
</success_criteria>

<output>
Create `.planning/phases/16-foundation-aero-primitives-layer/16-04-SUMMARY.md` when done.
</output>
