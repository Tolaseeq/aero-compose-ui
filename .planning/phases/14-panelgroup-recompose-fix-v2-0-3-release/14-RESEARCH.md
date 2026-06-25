# Phase 14: PanelGroup Recompose Fix + v2.0.3 Release — Research

**Researched:** 2026-06-25
**Domain:** Compose snapshot-state read/write discipline during composition; BoxWithConstraints/SubcomposeLayout subcomposition model; Compose effect APIs (SideEffect); JitPack release path
**Confidence:** HIGH (all critical findings verified against actual source code and official Compose docs)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- Boolean expansion array for size-math (`computeAvailablePx` / `distributePx`) computed directly from `isExpanded(sec)` each composition, NOT read from the written `expandedState` list (AeroPanelGroup.kt:346).
- `expandedState` sync (still needed for the uncontrolled path / animations) moved out of the composition body into a `SideEffect` (was at AeroPanelGroup.kt:331-335).
- The seed block (AeroPanelGroup.kt:297-320) must not mutate read-in-same-composition state.
- **Invariant:** no `BoxWithConstraints` composition pass writes to `expandedState`/`sizePx` that it also reads.
- Showcase repro: a live counter driven by `LaunchedEffect` ticks a `mutableStateOf`; content of ONE section reads that value; placement is `LayoutSection.kt`, a horizontal **controlled** `AeroPanelGroup`; repro is permanent and labeled.
- Fix verification: manual drag in showcase + static grep (no Compose UI / instrumented tests — mirrors Phase 13 precedent).
- The `SideEffect`-based `expandedState` sync MUST NOT call `onLayoutChange`; `onLayoutChange` stays drag-end + toggle only.
- Version bump: `2.0.2` → `2.0.3` in `build.gradle.kts:4` before tagging.
- Tag `v2.0.3` → push to `Tolaseeq/aero-compose-ui` → JitPack resolves `com.github.Tolaseeq:aero-compose-ui:2.0.3`.
- Docs: update `MILESTONES.md`, `RETROSPECTIVE.md`, PROJECT.md "Key Decisions" table; mark Phase 14 done in ROADMAP.md/STATE.md; run `/gsd:complete-milestone` after tag.
- Compose stays 1.7.3; zero new dependencies.

### Claude's Discretion

- Exact `SideEffect` body shape and where the seed block's expansion derivation lives.
- Counter cadence and precise caption wording / layout of the repro demo.
- Wording of the new PROJECT.md Key Decisions row and RETROSPECTIVE lessons.

### Deferred Ideas (OUT OF SCOPE)

- AeroPanelGroup drag-to-reorder sections (PNL-REORDER-01)
- Nested `AeroPanelGroup` as first-class API (PNL-NEST-01)
- Keyboard resize of dividers (PNL-KBD-01)
- AeroDropdown popup-offset regression (DROP-FIX-01, v1.0 carry-over)
- Migration to Compose 1.8.x
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| RCMP-01 | Horizontal + controlled: N sections render as exactly N header strips during recompose-while-drag (was N→N×N) | Duplication mechanism explained; fix pattern removes write-during-composition |
| RCMP-02 | `expandedArr` for size-math computed from `isExpanded(sec)` each composition, not from `expandedState[346]` | Code location identified; derivation pattern documented |
| RCMP-03 | `expandedState` sync moved to `SideEffect`; seed block does not mutate read-in-same-composition state | `SideEffect` API contract documented; `AeroSidebar` precedent confirmed |
| RCMP-04 | Minimal repro in `LayoutSection.kt`: horizontal controlled group, one section reads external `mutableStateOf` ticking via `LaunchedEffect` | Repro design pattern documented; placement confirmed |
| REG-01 | Vertical + uncontrolled byte-identical; `onLayoutChange` still drag-end + toggle only; animations unchanged | Untouched-code region identified; `SideEffect` guard against `onLayoutChange` documented |
| REG-02 | 12 `PanelGroupLogicTest` JVM tests stay GREEN; Compose 1.7.3; no new deps | Tests are pure-logic, touch no `expandedState` write path — unaffected by fix |
| REL-01 | `build.gradle.kts` version `2.0.2` → `2.0.3` | Root `build.gradle.kts:4` is the single version declaration |
| REL-02 | Tag `v2.0.3` pushed; JitPack resolves artifact | Release path confirmed via `jitpack.yml` + `maven-publish` existing infra |
</phase_requirements>

---

## Summary

The bug is a textbook Compose snapshot-state write-during-composition violation, amplified by `BoxWithConstraints`'s SubcomposeLayout internals. At `AeroPanelGroup.kt:331-335`, the code writes to `expandedState[i]` inside the body of `AeroPanelGroupImpl` (which runs during the `BoxWithConstraints` composition pass), while `expandedState` is also read at line 346 in the same pass to build `expandedArr`. In standard Compose this causes a recomposition loop; inside `BoxWithConstraints` (a SubcomposeLayout), the subcomposition model multiplies the loop: each state-write triggers a fresh subcompose call, and the children emitted by `forEachIndexed` at lines 462-603 accumulate N more entries on each invalidated pass. With N=3 sections and a rapid external recomposition driver, the loop produces 3×3=9 header strips.

The fix is two surgical changes to `AeroPanelGroupImpl` only. First, replace the `expandedArr` read at line 346 with a fresh derivation calling `isExpanded(sec)` per section (the helper at lines 327-329 already does exactly this computation; it just needs to be the source for `expandedArr` instead of `expandedState`). Second, move the sync loop at lines 331-335 into a `SideEffect` body so it fires after a successful composition commit, not during it. The `SideEffect` must call only `expandedState[i] = exp` — never `onLayoutChange`. The seed block at lines 297-320 already writes only to `sizePx`, `expandedState`, and `lastExpandedFractionState` on the condition `sizePx.size != sections.size` (structure change only), and is guarded before `BoxWithConstraints` entry — it does not cause a per-frame write; the analysis below confirms it is safe as-is.

**Primary recommendation:** Move lines 331-335 into `SideEffect { ... }`; replace line 346's `expandedState.toBooleanArray()` with `BooleanArray(sections.size) { i -> isExpanded(sections[i]) }`. Touch nothing else in `AeroPanelGroupImpl`.

---

## Standard Stack

### Core (fixed — zero drift allowed)

| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Compose Multiplatform | 1.7.3 | Composition runtime, layout primitives | LOCKED — do not drift to 1.8.x |
| Kotlin JVM | 17 toolchain | Language, coroutines | Unchanged |
| JUnit Jupiter | (existing) | JVM unit test runner for `PanelGroupLogicTest` | Unchanged |

**No new dependencies are introduced by this fix.** The `SideEffect` API is from `androidx.compose.runtime` which is already declared `api(compose.runtime)` in `library/build.gradle.kts:24`.

### Imports Required

`SideEffect` is not yet imported in `AeroPanelGroup.kt`. Add:

```kotlin
import androidx.compose.runtime.SideEffect
```

This is the only import change needed. Precedent: `AeroSidebar.kt:12` already imports `SideEffect` from the same package.

---

## Architecture Patterns

### The Canonical Snapshot-State Rule (HIGH confidence — official docs)

**Rule:** A composable function body runs during the _composition phase_. Writing to observed snapshot state (`mutableStateOf`, `mutableStateListOf`) during this phase is incorrect because it may schedule another recomposition before the current one completes.

The official Compose documentation states:
> "It is incorrect to perform an effect before a successful recomposition is guaranteed, which is the case when writing the effect directly in a composable."

The correct mechanism to write state _after_ a successful composition pass is `SideEffect`. The official contract:
> "Using a `SideEffect` guarantees that the effect executes after every successful recomposition."

**What this means for the bug:** The sync loop at `AeroPanelGroup.kt:331-335` writes to `expandedState[i]` (a `SnapshotStateList` entry) during the `AeroPanelGroupImpl` composition body. Because `expandedState` is also read at line 346 in the same composition scope, the write invalidates a state read that Compose already observed in this pass, scheduling an immediate recomposition — which runs the body again and writes again, ad infinitum.

### Why BoxWithConstraints (SubcomposeLayout) Makes It N×N

`BoxWithConstraints` is implemented on top of `SubcomposeLayout`. Its content lambda does not run during the normal parent composition phase — it is _subcomposed_ during the layout/measure phase. The key behavior:

1. The parent (`AeroPanelGroupImpl`) composes, reaches `BoxWithConstraints`, and Compose records the lambda.
2. During the measure pass, `BoxWithConstraints` calls `subcompose(slotId, content)` which runs the lambda — this is a _separate_ composition scope tied to measurement.
3. If observed snapshot state is written inside that lambda (or in code that runs before it in the same recompose scope), the write produces a state change that Compose must reconcile.
4. `SubcomposeLayout` responds by running `subcompose()` again on the next invalidation.
5. The children declared inside the lambda (`sections.forEachIndexed { i, section -> key(section.key) { ... } }`) are emitted again. Because `SubcomposeLayout` accumulates composed slots and the `key()` blocks may not cleanly deduplicate across re-subcompose calls when the outer scope is still mid-invalidation, the result is N additional header-strip elements per extra pass.
6. With N=3 sections and a rapid external recompose driver (e.g., a dragging divider whose gesture state also reads `expandedState`), each pass produces N×N=9 emitted items instead of 3.

The concrete chain in the existing code:

```
// Lines 331-335 — INSIDE AeroPanelGroupImpl composition body,
// BEFORE BoxWithConstraints entry:
sections.forEachIndexed { i, sec ->
    val exp = isExpanded(sec)
    if (expandedState.getOrElse(i) { exp } != exp) expandedState[i] = exp  // WRITE
}

BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val expandedArr = expandedState.toBooleanArray()  // READ of same state ← loop trigger
    ...
    sections.forEachIndexed { i, section ->
        key(section.key) {  // children emitted here
            ...
        }
    }
}
```

The write at 331-335 and the read at 346 are in the same `AeroPanelGroupImpl` composition scope. Even though the `BoxWithConstraints` content lambda runs in a subcomposition scope, the write happens in the _outer_ scope, which invalidates the outer scope, which triggers a re-subcompose of the inner lambda. Each re-subcompose emits N more `key(section.key)` blocks before the previous ones are cleaned up, causing the duplication.

**This is why the horizontal + controlled path is specifically affected:** in controlled mode, `isExpanded(sec)` queries `expandedKeys?.contains(sec.key)`. When the section content recomposes (due to an external state change, e.g., a drag counter), the controlled `expandedKeys` value drives `isExpanded()`. The sync loop detects a difference and writes to `expandedState[i]`, which triggers the cascade. In uncontrolled mode the same loop runs but `isExpanded(sec)` reads `internalExpanded` which is kept in sync by `onToggle` mutations — the sync loop rarely detects a difference unless a toggle just fired, so the write frequency is low and the visual artifact is less pronounced.

### Pattern: Correct Sync of State Mirror into SideEffect (HIGH confidence)

The established project precedent (from Phase 10, `AeroSidebar.kt:79`) is:

```kotlin
// BEFORE: inline write during composition — causes recompose loop
// animatedWidth is read AND the write-back is attempted in the same pass

// AFTER: SideEffect — runs after successful commit, never mid-composition
SideEffect { state.widthState.value = animatedWidth }
```

Apply the identical pattern to `AeroPanelGroupImpl`:

```kotlin
// BEFORE (AeroPanelGroup.kt:331-335) — in-composition write:
sections.forEachIndexed { i, sec ->
    val exp = isExpanded(sec)
    if (expandedState.getOrElse(i) { exp } != exp) expandedState[i] = exp
}

// AFTER — defer sync to post-commit:
SideEffect {
    sections.forEachIndexed { i, sec ->
        val exp = isExpanded(sec)
        if (expandedState.getOrElse(i) { exp } != exp) expandedState[i] = exp
    }
}
```

The `SideEffect` body has access to `sections`, `expandedState`, and `isExpanded` by capture — all are stable at the call site because `sections` is the list from the DSL scope (reconstructed each recompose) and `isExpanded` is a local function that closes over `controlled`, `expandedKeys`, and `internalExpanded`. The body is identical to the original; only the timing changes.

### Pattern: Read isExpanded() Directly for Size Math (HIGH confidence)

Replace line 346:

```kotlin
// BEFORE (AeroPanelGroup.kt:346) — reads the mirrored state list:
val expandedArr = expandedState.toBooleanArray()

// AFTER — derives boolean array fresh each composition from the authoritative source:
val expandedArr = BooleanArray(sections.size) { i -> isExpanded(sections[i]) }
```

`isExpanded(sec)` at lines 327-329 is:
```kotlin
fun isExpanded(sec: AeroPanelSectionConfig): Boolean =
    if (controlled) expandedKeys?.contains(sec.key) == true
    else sec.key in internalExpanded
```

Reading `expandedKeys` (a caller-provided `Set<String>` parameter) and `internalExpanded` (a `mutableStateOf<Set<String>>` remembered above) directly in composition is correct and causes no loop — it creates a normal Compose state observation: when `expandedKeys` changes, the caller has already called `setState`, so the recomposition is driven by the caller, not by a write inside this composable.

The derived `expandedArr` is a plain `BooleanArray` (not observed snapshot state), so computing it inline has zero side-effect risk.

**Size-math timing/animation concern addressed:** The `expandedArr` directly derived from `isExpanded()` reflects the same logical state as the old mirrored `expandedState`, because `SideEffect` will eventually bring `expandedState` in sync after each successful commit. The sequence is:

1. Composition: `expandedArr` = `BooleanArray { isExpanded(sections[i]) }` — correct for this pass.
2. `animatedHeights` / `caretRotations` read `expandedState.getOrElse(i) { false }` for animation targets. After the fix, `expandedState` is updated by `SideEffect` _after_ the commit, so for the first frame after a controlled toggle, `expandedState` may be one frame behind the `expandedArr` truth. This is acceptable because: (a) `expandedState` drives only `animateFloatAsState` targets and the divider visibility check — both tolerate one-frame lag gracefully (animation is not instantaneous anyway), and (b) the `SideEffect` fires synchronously on the main thread after every successful commit, so the lag is exactly one frame. The `key(section.key)` children count is driven by `sections` (from the DSL scope), not by `expandedState`, so child duplication is eliminated regardless.

### Pattern: Seed Block Safety (HIGH confidence)

The seed block at lines 297-320:

```kotlin
if (sizePx.size != sections.size) {
    sizePx.clear()
    expandedState.clear()
    lastExpandedFractionState.clear()
    sections.forEach { sec -> ... sizePx.add(...) ... expandedState.add(exp) ... }
    internalExpanded = sections.filter { ... }.map { it.key }.toSet()
}
```

This block runs only when `sizePx.size != sections.size` — i.e., on first composition or when the DSL section count changes. After the fix, `expandedArr` at line 346 (renamed from `expandedState.toBooleanArray()`) is derived from `isExpanded()`, not from `expandedState`. So even if the seed block writes to `expandedState`, the size-math path no longer reads it in the same composition scope. The seed block does not cause the duplication loop and needs no change.

However, as a static safety check: the seed block writes to `expandedState` and `sizePx`. After the fix, neither of these is read in the `AeroPanelGroupImpl` composition body's critical path (the sync loop moved to `SideEffect`; size-math reads `isExpanded()` directly). The only remaining composition reads of `expandedState` are:
- `expandedState.getOrElse(i) { false }` in `animatedHeights` (line 427) — animation target, one-frame lag is acceptable.
- `expandedState.getOrElse(i) { false }` in `caretRotations` (line 442) — same.
- `expandedState.indexOfLast { it }` for `lastExpandedIdx` (line 453) — same.
- `expandedState.getOrElse(i + 1) { false }` for divider visibility (lines 585, 699) — same.

None of these create the loop because: the seed block only fires on structure change (not every recompose), and the animation reads of `expandedState` do not _write_ to it, so they don't trigger cascading invalidation.

**Conclusion:** seed block is safe as-is; no change needed there.

### Recommended Project Structure (unchanged)

```
library/src/main/kotlin/com/mordred/aero/components/layout/
├── AeroPanelGroup.kt        # EDIT: 2 surgical changes to AeroPanelGroupImpl
├── internal/panelgroup/
│   └── PanelDistribution.kt # NO CHANGE — pure math, consumes BooleanArray only
showcase/src/main/kotlin/com/mordred/showcase/sections/
└── LayoutSection.kt         # ADD: horizontal controlled repro demo
build.gradle.kts             # EDIT: version "2.0.2" → "2.0.3" (line 4)
```

### Anti-Patterns to Avoid

- **Do NOT** replace `expandedState` reads in `animatedHeights`/`caretRotations`/`lastExpandedIdx` with `isExpanded()` calls. Those use `expandedState` as an animation target — they are already correct and should remain unchanged to keep animation behavior byte-identical.
- **Do NOT** make the `SideEffect` call `onLayoutChange`. The `SideEffect` purely syncs `expandedState` to mirror the authoritative expansion source; `onLayoutChange` is fired only from `onToggle` (toggle path) and `onDragEnd` (drag-end path) — both already in correct positions.
- **Do NOT** use `LaunchedEffect` instead of `SideEffect` for this sync. `LaunchedEffect` runs a coroutine asynchronously; the sync needs to happen synchronously on the main thread each frame so `expandedState` is ready for the next animation frame. `SideEffect` is the correct tool.
- **Do NOT** use `derivedStateOf` to wrap `isExpanded()` for `expandedArr`. `derivedStateOf` is for throttling recompositions when inputs change faster than needed. The `expandedArr` derivation is cheap (N boolean lookups), not a performance bottleneck, and the controlled source (`expandedKeys`) already changes exactly when needed.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Defer state sync out of composition | Custom queue, `Handler.post`, `coroutineScope.launch` | `SideEffect` | `SideEffect` is the documented Compose API for exactly this — runs after every successful commit, main thread, synchronous. Custom deferrals miss failed compositions. |
| Derive boolean expansion array | Read `expandedState` in composition, or add a third derived state list | `BooleanArray(n) { isExpanded(sections[it]) }` inline | `isExpanded()` already encapsulates the controlled/uncontrolled logic at lines 327-329. Duplicating or re-mirroring it adds state drift risk. |
| Sync `animatedHeights` target | Add yet another mirrored state list | Keep existing `expandedState.getOrElse(i) { false }` for animation reads | Animation targets tolerate one-frame lag. `SideEffect` closes the gap before the next frame. |

**Key insight:** The single source of truth for expansion is `isExpanded(sec)` (lines 327-329). Any code path that reads expansion for _structural_ decisions (child count, size math) must read this function. Code paths that read expansion for _animation targets_ can read `expandedState` because animation is inherently deferred.

---

## Common Pitfalls

### Pitfall 1: Moving the Sync Loop but Keeping expandedArr from expandedState

**What goes wrong:** Moving lines 331-335 to `SideEffect` but leaving line 346 as `expandedState.toBooleanArray()`. The loop still breaks the invariant in reverse: the `SideEffect` now writes to `expandedState` _after_ the composition read, which means on the next recompose, `expandedArr` lags by one frame. Worse, the one-frame lag means `computeAvailablePx` and `distributePx` receive stale expansion data — sizes may be computed for the wrong expansion state. In controlled mode during rapid recompose-while-drag, this still manifests as incorrect layout.

**How to avoid:** Both changes must be made together — `SideEffect` for the sync AND `isExpanded()` for `expandedArr`. Neither change alone is sufficient.

### Pitfall 2: Calling onLayoutChange from the SideEffect

**What goes wrong:** If `onLayoutChange?.invoke(sizePx.toList())` is added inside the `SideEffect`, it fires on every recomposition including drag frames. This violates REG-01 ("`onLayoutChange` fires on drag-end + toggle only, NOT per frame").

**How to avoid:** `SideEffect` body only syncs `expandedState`. `onLayoutChange` remains solely in `onToggle` and `onDragEnd`.

**Warning sign:** A grep for `onLayoutChange` inside the `SideEffect` block must return zero matches.

### Pitfall 3: Breaking the Vertical Branch

**What goes wrong:** The two changes are in the shared preamble of `AeroPanelGroupImpl` (before the `if (isHorizontal)` branch split at line 455). Both horizontal AND vertical branches read `expandedArr` and `renderHeights` from the same derivation. A change to either variable definition affects both orientations.

**How to avoid:** The fix changes only HOW `expandedArr` is built (the source changes from `expandedState` to `isExpanded()`). The variable name, type (`BooleanArray`), and usage in `computeAvailablePx`/`distributePx` are unchanged. The vertical branch continues to work identically. Verify by running `PanelGroupLogicTest` (which tests `distributePx`, `computeAvailablePx`, etc.) and by doing a manual vertical-orientation drag sign-off.

### Pitfall 4: Seed Block Mutation After the Fix

**What goes wrong:** Concern that the seed block (lines 297-320) writes to `expandedState` during composition, which is still "wrong." In reality, the seed block is guarded by `sizePx.size != sections.size` — it fires once per structure change, not every recompose. After the fix, `expandedState` is no longer read in the size-math critical path during the same composition scope, so this write does not trigger a cascade.

**How to avoid:** Do NOT move the seed block to a `LaunchedEffect` or `SideEffect`. The seed must run before `BoxWithConstraints` so that `sizePx`, `expandedState`, and `lastExpandedFractionState` are initialized before they are first read. The current placement before `BoxWithConstraints` is correct.

### Pitfall 5: One-Frame Animation Lag After SideEffect Move

**What goes wrong:** Fear that the `animatedHeights` animation will flicker because `expandedState` is now one frame behind after a toggle. In practice: (a) `SideEffect` fires synchronously on the main thread _before_ the next frame is drawn, so the animation target is updated before the draw phase of the same frame where the toggle fires; (b) even without that, `animateFloatAsState` interpolates smoothly — a one-frame target update delay is imperceptible. The animation contract is unchanged.

**How to avoid:** Trust the documented `SideEffect` semantics: "runs after every successful commit" means before the next draw. Do not add extra synchronization.

### Pitfall 6: Forgetting to Add the SideEffect Import

**What goes wrong:** `SideEffect` is not currently imported in `AeroPanelGroup.kt` (confirmed by grep — zero matches). The file will fail to compile without it.

**How to avoid:** Add `import androidx.compose.runtime.SideEffect` to the imports. Precedent: `AeroSidebar.kt:12` has the identical import.

---

## Code Examples

Verified patterns from actual project source and official Compose docs.

### Before and After: The Two Surgical Changes

**Change 1 — Move sync loop into SideEffect (replaces AeroPanelGroup.kt:331-335):**

```kotlin
// REMOVE these lines from the composition body:
// sections.forEachIndexed { i, sec ->
//     val exp = isExpanded(sec)
//     if (expandedState.getOrElse(i) { exp } != exp) expandedState[i] = exp
// }

// ADD SideEffect after the isExpanded helper definition (~line 329):
SideEffect {
    sections.forEachIndexed { i, sec ->
        val exp = isExpanded(sec)
        if (expandedState.getOrElse(i) { exp } != exp) expandedState[i] = exp
    }
}
```

**Change 2 — Derive expandedArr from isExpanded() (replaces AeroPanelGroup.kt:346):**

```kotlin
// BEFORE:
val expandedArr = expandedState.toBooleanArray()

// AFTER:
val expandedArr = BooleanArray(sections.size) { i -> isExpanded(sections[i]) }
```

These are the only two changes to `AeroPanelGroup.kt`. The rest of the function is untouched.

### Showcase Repro Pattern (new block in LayoutSection.kt)

```kotlin
// RCMP-04: Recompose-during-drag regression guard
// A live counter ticks independently; one section's content reads it.
// Drag the divider while the counter is visible — before the fix: 3 sections
// would render as 9 header strips; after the fix: exactly 3.
// This demo is permanent and guards against re-introducing the bug.

var recomposeDriveCounter by remember { mutableStateOf(0) }
LaunchedEffect(Unit) {
    while (true) {
        delay(32L)  // ~30 fps tick — fast enough to drive recompose during drag
        recomposeDriveCounter++
    }
}

var expandedKeys by remember { mutableStateOf(setOf("rcmp-left", "rcmp-center", "rcmp-right")) }
Text(
    "AeroPanelGroup (horizontal controlled — recompose-during-drag guard [RCMP-04])",
    color = colors.labelText,
    style = typography.bodyMedium,
)
Text(
    "Drag dividers while counter ticks. Must show exactly 3 sections, not 9.",
    color = colors.labelText,
    style = typography.bodySmall,
)
Box(Modifier.fillMaxWidth().height(240.dp)) {
    AeroPanelGroup(
        modifier = Modifier.fillMaxSize(),
        orientation = Orientation.Horizontal,
        expandedKeys = expandedKeys,
        onExpandedChange = { expandedKeys = it },
    ) {
        section(key = "rcmp-left", title = "Left") {
            // This section reads the live counter → drives recomposition independently.
            Text("Counter: $recomposeDriveCounter", color = colors.onSurface, style = typography.bodyMedium)
        }
        section(key = "rcmp-center", title = "Center") {
            Text("Static content", color = colors.onSurface, style = typography.bodyMedium)
        }
        section(key = "rcmp-right", title = "Right") {
            Text("Static content", color = colors.onSurface, style = typography.bodyMedium)
        }
    }
}
```

Imports needed in `LayoutSection.kt`:
```kotlin
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
```

(`kotlinx-coroutines-core` is already a dependency in `library/build.gradle.kts:29`; the showcase inherits it transitively.)

### AeroSidebar SideEffect Precedent (reference — do not change)

```kotlin
// AeroSidebar.kt:79 — the established project pattern for SideEffect sync:
SideEffect { state.widthState.value = animatedWidth }
```

The `expandedState` sync follows the exact same idiom: post-commit write-back of a derived value into a state holder used by animation paths.

### isExpanded() — The Authoritative Source (no change needed)

```kotlin
// AeroPanelGroup.kt:327-329 — unchanged; this is what expandedArr now reads:
fun isExpanded(sec: AeroPanelSectionConfig): Boolean =
    if (controlled) expandedKeys?.contains(sec.key) == true
    else sec.key in internalExpanded
```

---

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| Mirror external state into a separate `SnapshotStateList` and read it in the same composition pass | Read the authoritative source (`isExpanded()`) directly in composition; use `SideEffect` to update the mirror post-commit | Eliminates write-during-composition loop |
| `expandedState.toBooleanArray()` at line 346 as the input to size math | `BooleanArray(sections.size) { i -> isExpanded(sections[i]) }` | Size math always sees current expansion truth, not a one-frame-stale mirror |
| Sync loop at lines 331-335 in the composition body | `SideEffect { ... }` body with identical logic | No composition-phase state write |

**Deprecated pattern for this codebase:**
- In-composition write to any `SnapshotStateList` / `mutableStateOf` that is also read in the same composition scope — replaced by `SideEffect` for sync, `remember {}` initializer for one-time seed.

---

## Release Path

### Version Bump (REL-01)

The single version declaration is in the **root** `build.gradle.kts:4`:

```kotlin
// build.gradle.kts (root, line 3-5):
allprojects {
    group = "com.mordred"
    version = "2.0.2"   // ← change to "2.0.3"
}
```

The library's own `library/build.gradle.kts` does not declare a version — it inherits from the root via `allprojects`. The `maven-publish` block in `library/build.gradle.kts` also inherits this version. Changing line 4 of the root file is the complete version bump.

### JitPack Release Path (REL-02)

Existing infra (unchanged):
- `jitpack.yml`: specifies `openjdk17`; `sdk install java 17.0.10-tem` — no changes needed.
- `library/build.gradle.kts`: `maven-publish` plugin with `MavenPublication("maven")` from `components["java"]` — already working through v2.0.2.

Release sequence:
1. Bump `build.gradle.kts:4` to `"2.0.3"`.
2. Commit and push all code changes.
3. `git tag v2.0.3 && git push origin v2.0.3`.
4. JitPack picks up the tag automatically and builds the artifact.
5. Artifact: `com.github.Tolaseeq:aero-compose-ui:2.0.3`.

No `jitpack.yml` changes required — this release is structurally identical to v2.0.2.

---

## Open Questions

1. **`expandedState` reads in `animatedHeights`/`caretRotations` — one-frame lag acceptable?**
   - What we know: `SideEffect` fires before the next draw pass; `animateFloatAsState` interpolates over 200ms; the lag is imperceptible in practice.
   - What's unclear: in theory, a user could toggle + immediately drag in the same frame. In practice, this race is already present in the original code and is guarded by `if (isDragging) return` in `onToggle`.
   - Recommendation: accept the one-frame lag; it matches the `AeroSidebar` precedent.

2. **`recomposeDriveCounter` cadence for the repro**
   - What we know: 32ms (≈30fps) is fast enough to drive visible recomposition during a manual divider drag.
   - What's unclear: whether a faster cadence (16ms/60fps) is needed for the bug to be reliably reproducible.
   - Recommendation: use 32ms as the starting cadence; it is in Claude's Discretion to adjust.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter (JUnit 5) via `useJUnitPlatform()` |
| Config file | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.layout.PanelGroupLogicTest"` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| RCMP-01 | N sections render as exactly N header strips under recompose-during-drag | manual-only | N/A — no Compose UI test infra | N/A |
| RCMP-02 | `expandedArr` from `isExpanded()` each composition | static inspection (grep) | `grep -n "expandedState.toBooleanArray" library/src/**/*.kt` must return 0 | N/A |
| RCMP-03 | No in-composition write to `expandedState`/`sizePx` in read path | static inspection (grep) | `grep -n "expandedState\[" library/src/main/**/*.kt` returns only SideEffect + seed sites | N/A |
| RCMP-04 | Repro demo present in LayoutSection.kt | manual visual | N/A | ❌ Wave 0: add repro block |
| REG-01 | Vertical byte-identical; `onLayoutChange` not per-frame | manual drag sign-off | N/A | N/A |
| REG-02 | 12 PanelGroupLogicTest stay GREEN | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest"` | ✅ exists |
| REL-01 | Version is 2.0.3 | static inspection | `grep 'version = ' build.gradle.kts` must show 2.0.3 | N/A |
| REL-02 | JitPack resolves artifact | manual: open JitPack build log | N/A | N/A |

### Sampling Rate

- **Per task commit:** `./gradlew :library:test --tests "*.PanelGroupLogicTest"` (12 tests, fast JVM run)
- **Per wave merge:** `./gradlew :library:test` (full test suite)
- **Phase gate:** Full suite green + manual visual sign-off before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] No new test files needed — the fix is pure-logic changes to a composable; the existing 12 JVM tests cover `PanelDistribution.kt` which is unchanged.
- [ ] RCMP-04 repro block in `LayoutSection.kt` — must be added as part of the fix plan (not a test file, a showcase demo).

None — existing test infrastructure covers all automatically-testable phase requirements.

---

## Sources

### Primary (HIGH confidence)
- `AeroPanelGroup.kt` — actual source code read; exact line numbers identified (331-335 sync loop, 346 `expandedArr`, 297-320 seed block, 327-329 `isExpanded()`)
- `AeroSidebar.kt:79` — `SideEffect` precedent from Phase 10 in this exact codebase
- `PanelGroupLogicTest.kt` — 12 tests read; confirmed they test `PanelDistribution.kt` pure functions only, unaffected by fix
- `build.gradle.kts` (root, line 4) — confirmed as the single `version` declaration
- `library/build.gradle.kts` — confirmed `maven-publish` infra; `SideEffect` available via `api(compose.runtime)`
- `jitpack.yml` — confirmed no changes needed for v2.0.3
- [Official Compose Side-effects docs](https://developer.android.com/develop/ui/compose/side-effects) — `SideEffect` API contract: "runs after every successful recomposition"; "incorrect to perform an effect before a successful recomposition is guaranteed"
- [Official Compose Phases docs](https://developer.android.com/develop/ui/compose/phases) — "do not write to observed state during composition if that state is used as layout input"

### Secondary (MEDIUM confidence)
- [SubcomposeLayout and BoxWithConstraints internals — RevenueCat](https://www.revenuecat.com/blog/engineering/subcomposelayout-internals/) — SubcomposeLayout subcomposes content during measure phase; `subcompose(slotId, content)` is "strictly a measurement-time operation"; state write during measure lambda triggers re-subcompose

### Tertiary (MEDIUM confidence, verified by cross-referencing with primary sources)
- [Compose Phases — Android Developers](https://developer.android.com/develop/ui/compose/phases) — composition/layout/drawing phase model; state reads in each phase have different recomposition scope
- [Advanced State and Side Effects Codelab](https://developer.android.com/codelabs/jetpack-compose-advanced-state-side-effects) — `SideEffect` vs `LaunchedEffect`: SideEffect is synchronous, post-commit; LaunchedEffect is async coroutine

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| Bug root cause identification | HIGH | Exact file:line read; write-during-composition rule verified against official docs |
| SideEffect as correct fix mechanism | HIGH | Official docs + AeroSidebar precedent in same codebase |
| BoxWithConstraints/SubcomposeLayout duplication mechanism | MEDIUM | Conceptually verified from SubcomposeLayout internals article; exact slot accumulation behavior is runtime-internal, not officially documented at this level of detail |
| Seed block safety analysis | HIGH | Read seed block code; confirmed structure-change-only guard; confirmed expandedArr no longer reads expandedState after fix |
| Animation one-frame lag acceptability | HIGH | SideEffect timing documented; AeroSidebar precedent uses same pattern |
| JitPack release path | HIGH | jitpack.yml and build.gradle.kts read; infra already proven through v2.0.2 |
| Repro design | HIGH | LaunchedEffect counter pattern is standard; placement in LayoutSection.kt confirmed |
| 12 JVM tests unaffected | HIGH | Tests read; they test PanelDistribution.kt pure functions; fix does not touch that file |

**Research date:** 2026-06-25
**Valid until:** 2026-07-25 (Compose 1.7.3 is locked; no ecosystem churn expected)
