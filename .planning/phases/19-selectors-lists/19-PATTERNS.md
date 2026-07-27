# Phase 19: Selectors + Lists - Pattern Map

**Mapped:** 2026-07-27
**Files analyzed:** 3 modified components + 8 new test files
**Analogs found:** 11 / 11

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `library/.../components/selection/AeroSwitch.kt` | component (restyle) | request-response (per-frame paint + toggle event) | `library/.../components/range/AeroSlider.kt` (thumb/track resolver + slot-layering precedent) | exact (D-05 explicitly names this the model) |
| `library/.../components/selection/AeroSegmentedControl.kt` | component (restyle) | request-response (per-segment select event) | `library/.../components/buttons/AeroButtonSurface.kt` (`resolveButtonStyle`, `pressedRecess` reuse) + `library/.../components/selection/AeroRadioButton.kt` (`selectable(role=RadioButton)` wiring) | exact (D-08/D-09 explicitly name these) |
| `library/.../components/list/AeroListItem.kt` | component (restyle) | request-response (hover/select composition) | itself (in place) — `resolveButtonStyle`'s composition shape is the structural model for D-11 | exact |
| `AeroSwitchStylesTest.kt` (NEW) | test (value-level resolver test) | transform | `library/.../components/range/AeroSliderStylesTest.kt` | exact |
| `AeroSwitchSemanticsTest.kt` (NEW) | test (compose UI semantics/keyboard) | event-driven | `library/.../components/buttons/AeroButtonSemanticsTest.kt` | role-match (toggleable vs clickable, same shape) |
| `AeroSwitchSourceTest.kt` (NEW) | test (source-scan guard) | transform | `library/.../components/range/AeroSliderSourceTest.kt` | exact |
| `AeroSegmentedControlStylesTest.kt` (NEW) | test (value-level resolver test) | transform | `library/.../components/range/AeroSliderStylesTest.kt` | exact |
| `AeroSegmentedControlSemanticsTest.kt` (NEW) | test (compose UI semantics/keyboard) | event-driven | `library/.../components/buttons/AeroButtonSemanticsTest.kt` | role-match (selectable/RadioButton vs clickable/Button) |
| `AeroSegmentedControlSourceTest.kt` (NEW) | test (source-scan guard) | transform | `library/.../components/buttons/AeroButtonSurfaceSourceTest.kt` | exact (verbatim-reuse guard is the exact same shape) |
| `AeroListItemStylesTest.kt` (NEW) | test (value-level resolver test) | transform | `library/.../components/range/AeroSliderStylesTest.kt` | role-match (nullable-style return is new; precedence-chain shape is identical) |
| `AeroListItemSourceTest.kt` (NEW) | test (source-scan guard) | transform | `library/.../components/range/AeroSliderSourceTest.kt` | exact |

## Pattern Assignments

### `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` (component restyle)

**Current state** (full file, 77 lines, read in full):
- Two flat `Box`es: outer = track (`.background(trackColor, shape)`, `toggleable(role=Role.Switch)`), inner = thumb (`.background(colors.surface, RoundedCornerShape(50))`).
- `animateColorAsState` (track) + `animateFloatAsState` (thumb x-offset), both `tween(150, LinearEasing)` — **keep unchanged** (D-07).
- `.alpha(if (enabled) 1f else 0.4f)` — **replace** with `flattenDisabled`.
- No hover/press/focus wiring at all, no `interactionSource` param — **add** (D-04/VSEL-02).

**Analog 1 — resolver shape:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` lines 244-266 (`resolveSliderThumbStyle`/`resolveSliderTrackStyle`, PRESSED_GLOSS_BOOST):
```kotlin
// AeroSlider.kt:47 / 70 — corner-radius constants pattern to mirror (7.dp / 9.dp for switch)
private val THUMB_RADIUS = 10.dp
private val TRACK_CORNER_RADIUS = 2.dp

// AeroSlider.kt:244-259 (approx) — the exact precedence chain D-05 requires:
internal fun resolveSliderThumbStyle(
    colors: AeroColorScheme,
    hovered: Boolean,
    pressed: Boolean,
    isDragging: Boolean,
    focused: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.neutralRest(colors, cornerRadius = THUMB_RADIUS)
    if (!enabled) return rest.flattenDisabled(colors)
    return when {
        pressed || isDragging -> rest.copy(glossAlpha = rest.glossAlpha + PRESSED_GLOSS_BOOST)
        hovered -> rest.hoverLighten()
        else -> rest
    }
}
private const val PRESSED_GLOSS_BOOST: Float = 0.10f
```
`resolveSwitchThumbStyle` mirrors this verbatim minus `isDragging`. `resolveSwitchGrooveStyle` is new (no isDragging/press branch — see D-02, lerp-driven fill only), built from `AeroSurfaceStyle.neutralRest`/`.rest` per 19-RESEARCH.md's Code Examples section (already-designed, copy verbatim):
```kotlin
internal fun resolveSwitchGrooveStyle(
    colors: AeroColorScheme,
    checkedProgress: Float,
    enabled: Boolean,
): AeroSurfaceStyle {
    val neutral = AeroSurfaceStyle.neutralRest(colors, cornerRadius = 9.dp)
    val accent = AeroSurfaceStyle.rest(colors, cornerRadius = 9.dp)
    val resolved = neutral.copy(
        fillTop = androidx.compose.ui.graphics.lerp(neutral.fillTop, accent.fillTop, checkedProgress),
        fillBottom = androidx.compose.ui.graphics.lerp(neutral.fillBottom, accent.fillBottom, checkedProgress),
    )
    return if (enabled) resolved else resolved.flattenDisabled(colors)
}
```

**Analog 2 — thumb-as-sibling-outside-clip layering (D-03):** `AeroSlider.kt` lines ~160-200 (thumb slot vs track slot, both independently `aeroGlowRing`-then-`aeroSurface`/`aeroThumbSurface` chained). Groove uses `Modifier.aeroGroove(grooveStyle)` (PRIM-08); thumb uses `Modifier.aeroGlowRing(...).aeroThumbSurface(thumbStyle)` (PRIM-07) — glow BEFORE the clip-applying primitive, always.

**Analog 3 — indication=null + interactionSource wiring on toggleable:** `AeroButtonSurface.kt` lines 84-105 (`Modifier.clickable(interactionSource=, indication=null, role=Role.Button, onClick=)` chained before the two `aeroGlowRing` calls, then `aeroSurface`). `AeroSwitch`'s `.toggleable(...)` call needs the same `interactionSource`/`indication = null` params (Pitfall 7) plus `.hoverable(interactionSource)` chained alongside it (Pitfall 6 — toggleable does NOT report hover for free).

**Hover/press/focus collection:** `rememberAeroInteractionState(interactionSource)` from `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` lines 46-69 — bundles `hovered`/`pressed`/`focused` booleans, used identically to `AeroButtonSurface`'s `val state = rememberAeroInteractionState(interactionSource)`.

---

### `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` (component restyle)

**Current state** (full file, 96 lines, read in full):
- `Row` + `.border(1.dp, colors.borderDefault, shape).clip(shape)`, per-segment `animateColorAsState` between `colors.primary.copy(alpha=0.3f)` and `Color.Transparent` (PRIM-14 anti-pattern, line 59) — **removed**.
- Bare `.clickable(enabled = enabled) { onSelect(opt) }` (line 74) — no role, no semantics — **replaced** by `Modifier.selectable(role = Role.RadioButton, ...)`.
- 1.dp separator `Box` (lines 87-93) — **dropped** per D-08/D-10/UI-SPEC resolution (each segment's own contour supplies the break).
- `.alpha(if (enabled) 1f else 0.4f)` (line 50) — **replaced** with `flattenDisabled`.

**Analog 1 — `Modifier.selectable(role = Role.RadioButton)` wiring:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroRadioButton.kt` lines 55-64 (already proven working in-repo):
```kotlin
Box(
    modifier = Modifier
        .alpha(if (enabled) 1f else 0.4f)
        .size(16.dp)
        .border(1.dp, borderColor, shape)
        .selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = { onClick?.invoke() }
        ),
    contentAlignment = Alignment.Center
)
```
`AeroSegmentedControl`'s per-segment call must additionally pass `interactionSource = <per-segment MutableInteractionSource>` and `indication = null` (matching `AeroButtonSurface`'s `clickable(...)` precedent — `selectable` accepts the same `interactionSource`/`indication` params) plus `.hoverable(interactionSource)` chained alongside it (Pitfall 6).

**Analog 2 — `pressedRecess` cross-package reuse verbatim (VSEL-03/D-08):** `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` lines 126-133:
```kotlin
internal fun AeroSurfaceStyle.pressedRecess(innerShadow: Shadow): AeroSurfaceStyle = copy(
    fillTop = fillBottom, fillBottom = fillTop,
    bevelLight = bevelShadow, bevelShadow = bevelLight,
    glossAlpha = 0f, innerShadow = innerShadow,
)
```
`PRESSED_INNER_SHADOW` currently lives `private` in `AeroButtonSurface.kt` lines 122-126:
```kotlin
private val PRESSED_INNER_SHADOW: Shadow = Shadow(
    radius = 2.dp,
    color = Color.Black.copy(alpha = 0.35f),
    offset = DpOffset(0.dp, 1.dp),
)
```
**Wave-0 action** (flagged by RESEARCH.md): make this `internal` (not `private`) in `AeroButtonSurface.kt` and import it into `AeroSegmentedControl.kt`, rather than redeclaring — per D-08's "reused verbatim, not a new one" requirement and the source-scan guard test that will assert this import exists.

**Analog 3 — resolver precedence-chain shape (mirrors `resolveButtonStyle`):** `AeroButtonSurface.kt` lines 244-279 (`resolveButtonStyle`) is the exact shape `resolveSegmentStyle` copies — disabled-wins, terminal `flattenDisabled`, `pressedRecess`/`hoverLighten` composed on a `rest(colors, cornerRadius=4.dp)` base:
```kotlin
internal fun resolveSegmentStyle(
    colors: AeroColorScheme,
    isSelected: Boolean,
    hovered: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val base = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)
    val selectedOrNot = if (isSelected) base.pressedRecess(PRESSED_INNER_SHADOW) else base
    if (!enabled) return selectedOrNot.flattenDisabled(colors)
    return if (hovered) selectedOrNot.hoverLighten() else selectedOrNot
}
```

**Analog 4 — outer `Row.clip(shape)` still needed** but per-segment `aeroGlowRing` must NOT be used (D-10) — instead an in-bounds inner rim (`drawRoundRect` stroke at `colors.borderSelected`) drawn as part of the segment's own paint. No existing analog draws an inner-only rim yet in this codebase (new territory) — closest structural precedent for "a second inset stroke drawn manually" is `ScratchAeroShadowProof.kt`'s native-shadow ordering (dropShadow before background, innerShadow after) cited in 16-PATTERNS.md, though this phase's focus cue is a plain stroke, not a `Shadow`.

---

### `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` (component restyle)

**Current state** (full file, 105 lines, read in full):
- Lines 59-64 — the exact anti-pattern D-11 removes:
```kotlin
val animatedBg by animateColorAsState(
    targetValue = when {
        selected -> colors.primary.copy(alpha = 0.2f)
        hovered && enabled -> colors.buttonHover
        else -> Color.Transparent
    },
    animationSpec = tween(150, easing = LinearEasing),
    label = "listItemBg"
)
```
- Line 73: `.background(animatedBg)` — unclipped, full-bleed — **replaced** by an inset `Box` with `Modifier.aeroSurface(resolved, RoundedCornerShape(6.dp))` (VLST-01).
- Lines 74-78: `.hoverable(interactionSource).then(if (onClick != null) Modifier.clickable(...) else Modifier)` — **this pairing is already correct and is the pattern VLST-04 mandates all other newly-hover-wired components copy.** Keep as-is; only the `.clickable(...)` call should gain `indication = null` (Pitfall 7, a live pre-existing gap, not one of the 8 named requirements but flagged by RESEARCH.md as worth fixing in the same pass).
- Line 80: `.alpha(if (enabled) 1f else 0.4f)` — **replaced** with `flattenDisabled` on whichever base resolves.
- `interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }` (line 54) — **already the exact shape D-04 copies onto `AeroSwitch`/`AeroSegmentedControl`.**

**Analog — D-11 base-then-transform composition, already fully designed in 19-RESEARCH.md** (copy verbatim, this is the single highest-value pure function this phase adds):
```kotlin
internal fun resolveListItemPillStyle(
    colors: AeroColorScheme,
    selected: Boolean,
    hovered: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle? {
    val selectedStyle = AeroSurfaceStyle.rest(colors, cornerRadius = 6.dp).let { s ->
        s.copy(
            fillTop = s.fillTop.copy(alpha = s.fillTop.alpha * 0.5f),
            fillBottom = s.fillBottom.copy(alpha = s.fillBottom.alpha * 0.5f),
        )
    }
    val base: AeroSurfaceStyle? = if (selected) selectedStyle else null
    val withHover = if (hovered && enabled) {
        (base ?: AeroSurfaceStyle.neutralRest(colors, cornerRadius = 6.dp)).hoverLighten()
    } else base
    return if (!enabled) withHover?.flattenDisabled(colors) else withHover
    // resolved == null -> no pill Box/aeroSurface call at all
}
```
Note: unlike `resolveButtonStyle`/`resolveSegmentStyle`, this resolver returns a **nullable** `AeroSurfaceStyle` — the call site conditionally renders (or skips) the inset pill `Box` based on `resolved != null`. This nullable-return shape is genuinely new in this codebase; there is no existing resolver to copy that exact nullability from, so implement per the RESEARCH.md pseudocode directly (flag as the one place this phase's resolver family diverges from the `resolveButtonStyle`/`resolveSliderThumbStyle`/`resolveSegmentStyle` non-null shape).

**Focus visual (VLST-03/D-13):** in-bounds inset `drawRoundRect` stroke at `colors.borderSelected`, gated on `onClick != null` — same "no aeroGlowRing, in-bounds only" exception as the segmented control's focus cue (D-10). No existing library analog for an in-bounds focus stroke; author fresh per UI-SPEC, reusing the pill's own inset/shape geometry even when the background pill itself is not drawn (unselected+unhovered+focused row).

---

### `AeroSwitchStylesTest.kt` / `AeroSegmentedControlStylesTest.kt` / `AeroListItemStylesTest.kt` (value-level resolver tests)

**Analog:** `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt` (full file, 171 lines, read in full) — the "reconstruct-expected-from-scratch" convention:
```kotlin
class AeroSliderStylesTest {
    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)

    private fun expectedThumbRest(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.neutralRest(colors, cornerRadius = 10.dp)   // hardcode radius, don't import the constant

    @Test
    fun thumbDisabledWinsRegardlessOfHoveredPressedFocused() {
        schemes.forEach { colors ->
            val disabledOnly = resolveSliderThumbStyle(colors, hovered=false, pressed=false, isDragging=false, focused=false, enabled=false)
            val disabledAll = resolveSliderThumbStyle(colors, hovered=true, pressed=true, isDragging=true, focused=true, enabled=false)
            val expected = expectedThumbRest(colors).flattenDisabled(colors)
            assertEquals(expected, disabledOnly, "...")
            assertEquals(disabledOnly, disabledAll, "disabled must win over all other flags combined")
        }
    }
    // ... pressed/hovered/focused-only branches, each asserting exact equality against a
    // from-scratch-reconstructed expected style, never importing the resolver's own constant.
}
```
No `runComposeUiTest`, no Compose runtime — plain `kotlin.test` JVM tests. Multi-scheme coverage: `listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)` (translucent + opaque) is the minimum bar; segment/list-item tests should follow the same two-scheme list. `AeroListItemStylesTest` additionally needs a "resolved == null" assertion path (rest-unselected-unhovered) that the slider test has no equivalent of — new assertion shape, same file structure.

---

### `AeroSwitchSemanticsTest.kt` / `AeroSegmentedControlSemanticsTest.kt` (compose UI semantics + keyboard)

**Analog:** `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt` (full file, 124 lines, read in full) — `runComposeUiTest` + `performKeyInput`/`pressKey` pattern:
```kotlin
@OptIn(ExperimentalTestApi::class)
class AeroButtonSemanticsTest {
    @Test
    fun aeroButtonHasRoleButtonAndClickAction() = runComposeUiTest {
        setContent { AeroTheme { AeroButton(text = "Save Changes", onClick = {}) } }
        waitForIdle()
        onNodeWithText("Save Changes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }

    @Test
    fun aeroButtonSpaceKeyInvokesOnClickAfterFocus() = runComposeUiTest {
        var clicked = false
        setContent { AeroTheme { AeroButton(text = "Save Changes", onClick = { clicked = true }) } }
        waitForIdle()
        val node = onNodeWithText("Save Changes")
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Spacebar) }
        waitForIdle()
        assertTrue(clicked, "Space must invoke onClick once the button node is focused")
    }
}
```
`AeroSwitchSemanticsTest` asserts `SemanticsProperties.Role == Role.Switch`, toggled-state via `SemanticsProperties.ToggleableState` or `onNodeWithTag`, and Space/Enter both flip `checked` via `onCheckedChange`. `AeroSegmentedControlSemanticsTest` asserts `Role.RadioButton` per segment (N nodes, one per option, per D-09's "N Tab stops" trade-off) and that each segment is independently focusable/activatable via Space/Enter — a genuinely new assertion shape (looping over N segment nodes) not present in the button precedent's single-node tests.

---

### `AeroSwitchSourceTest.kt` (source-scan guard)

**Analog:** `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt` (full file, 76 lines, read in full) — the `sourceFile()` cwd-independent resolution convention plus specific substring guards:
```kotlin
class AeroSliderSourceTest {
    @Test
    fun aeroSliderThumbPressDoesNotUsePressedRecess() {
        assertFalse(
            aeroSliderSource.contains(".pressedRecess("),
            "AeroSlider.kt's thumb resolver must not call .pressedRecess( — a dragged thumb stays " +
                "raised (picked up), it does not invert/recede like a pressed button (D-04)"
        )
    }
    private val aeroSliderSource: String get() = sourceFile("AeroSlider.kt").readText()
    private fun sourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/kotlin/com/mordred/aero/components/range/$name"),
            File("library/src/main/kotlin/com/mordred/aero/components/range/$name"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from cwd ${File(".").absolutePath} (tried: $candidates)")
    }
}
```
`AeroSwitchSourceTest` asserts the D-05 negative (`assertFalse(source.contains(".pressedRecess("))` — switch thumb press must NOT recess, mirroring the slider's own guard almost verbatim) plus a positive check that `.hoverable(` and `indication = null` are both present (Pitfall 6/7 regressions this phase must not reintroduce).

---

### `AeroSegmentedControlSourceTest.kt` (source-scan guard — verbatim-reuse focus)

**Analog:** `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` (full file, 99 lines, read in full) — the "no duplicated painter" reuse-guard shape, directly applicable since D-08/VSEL-03's core risk (re-deriving `pressedRecess` instead of importing it) is exactly what this file's `aeroButtonCallsTheSharedSurfaceNotADuplicatedPainter` test guards against for buttons:
```kotlin
@Test
fun aeroButtonCallsTheSharedSurfaceNotADuplicatedPainter() {
    assertTrue(
        aeroButtonSource.contains("AeroButtonSurface("),
        "AeroButton.kt must delegate to the shared AeroButtonSurface (VBTN-06)"
    )
    assertFalse(
        aeroButtonSource.contains("aeroSurface(") || aeroButtonSource.contains("drawAeroSurfaceCore("),
        "AeroButton.kt must not call aeroSurface(/drawAeroSurfaceCore( directly — that would " +
            "be a second, duplicated painter (VBTN-06)"
    )
}
```
`AeroSegmentedControlSourceTest` asserts: (1) `import com.mordred.aero.theme.pressedRecess` (or equivalent qualified call) is present — proving cross-package reuse, not re-derivation; (2) the source does NOT contain a second, independently-constructed `Shadow(radius = 2.dp, ...)` literal (would indicate the "redeclare an identical constant" fallback path from 19-RESEARCH.md's Pattern 4 was taken instead of making `PRESSED_INNER_SHADOW` internal and importing it); (3) `Color.Transparent` is absent (PRIM-14, the currently-shipped bug at line 59); (4) the 1.dp separator `Box` pattern (`borderDefault.copy(alpha = 0.5f)` / `.width(1.dp)`) is absent (D-08/D-10 drop). Per this project's "Fail-Then-Pass Proof" convention (both analog source tests document this), each guard should be proven to fail against a deliberately-reintroduced violation before being trusted.

---

### `AeroListItemSourceTest.kt` (source-scan guard)

**Analog:** `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt` (same file as `AeroSwitchSourceTest`'s analog — the `sourceFile()`/`assertFalse(source.contains(...))` shape is the exact template). Guards: (1) `Color.Transparent` absent (PRIM-14, the currently-shipped bug at `AeroListItem.kt:63`); (2) no `when { selected -> ...; hovered -> ...; else -> ... }` three-way branch shape reintroduced for the pill (D-11 — a plain-substring check on the shape is fragile; consider asserting `resolveListItemPillStyle(` is called instead, i.e. a positive "delegates to the resolver" check mirroring `AeroButtonSurfaceSourceTest`'s `AeroButtonSurface(` presence check); (3) `.hoverable(` and `indication = null` both still present on the `.clickable(...)` call (Pitfall 7 fix, non-regression of the already-correct VLST-04 wiring).

---

## Shared Patterns

### Disabled-wins precedence-chain resolver shape
**Source:** `resolveButtonStyle` (`AeroButtonSurface.kt:244-279`) and `resolveSliderThumbStyle`/`resolveSliderTrackStyle` (`AeroSlider.kt:244-275`)
**Apply to:** every new resolver this phase adds (`resolveSwitchThumbStyle`, `resolveSwitchGrooveStyle`, `resolveSegmentStyle`, `resolveListItemPillStyle`) — `enabled == false` short-circuits to `.flattenDisabled(colors)` before any hover/press/selection branch is evaluated; nothing else can override disabled.

### `rememberAeroInteractionState(interactionSource)` + `.hoverable(interactionSource)` pairing
**Source:** `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` lines 46-69; real call site `AeroListItem.kt` lines 57, 74
**Apply to:** all three components — every new `.toggleable`/`.selectable` call MUST also chain `.hoverable(interactionSource)` on the same modifier chain (Pitfall 6 — toggleable/selectable do not report hover for free).

### `indication = null` on every new toggleable/selectable call
**Source:** `AeroButtonSurface.kt` line 91 (`indication = null` on `.clickable(...)`)
**Apply to:** `AeroSwitch`'s `.toggleable(...)` and `AeroSegmentedControl`'s per-segment `.selectable(...)` — new-this-phase addition per Pitfall 7 (not locked by CONTEXT.md but recommended for consistency with the shipped button precedent). Also worth backfilling onto `AeroListItem`'s existing `.clickable(...)` in the same pass since it currently has no `indication = null` either.

### `aeroGlowRing` BEFORE any clip-applying primitive
**Source:** `AeroSurfacePrimitives.kt` KDoc (cited in `AeroButtonSurface.kt` lines 42-46) — `Modifier.aeroGlowRing(...).aeroSurface(...)` is correct; the reverse silently erases the bloom.
**Apply to:** `AeroSwitch`'s track hover/focus glow (chained before `.aeroGroove(...)`) and thumb hover glow (chained before `.aeroThumbSurface(...)`). Does NOT apply to `AeroSegmentedControl`/`AeroListItem` — D-10/D-13 forbid `aeroGlowRing` entirely on those two, using in-bounds inner-rim strokes instead.

### `Color.Transparent` is the anti-pattern — `baseColor.copy(alpha = 0f)` instead
**Source:** `AeroSurfaceStyle.kt`/`AeroSurfacePrimitives.kt` KDoc (PRIM-14); currently-shipped violations at `AeroSegmentedControl.kt:59` and `AeroListItem.kt:63`
**Apply to:** every gradient fade this phase introduces (pill edges, groove ends, segment fill) — this phase's job includes removing both currently-shipped violations, not just avoiding new ones.

### `pressedRecess`/`hoverLighten`/`flattenDisabled` composition over `when`-branch color-picking
**Source:** `AeroSurfaceStyle.kt` lines 126-180; the anti-pattern to remove is `AeroListItem.kt` lines 59-64
**Apply to:** all three components' resolvers — never a `when { state -> literalColor }` shape; always `baseStyle.transform().transform()` composition so combined states (e.g. hover+selected) cannot suppress each other (D-11's structural guarantee).

## No Analog Found

None — every file in this phase's scope (3 modified components, 8 new tests) has at least a role-match analog already in the codebase, consistent with 19-RESEARCH.md's framing ("disciplined reuse, not invention — every primitive, transform, and precedence-chain pattern already exists, shipped, and is proven on three themes by Phases 16-18").

## Metadata

**Analog search scope:** `library/src/main/kotlin/com/mordred/aero/components/{selection,list,buttons,range,common}/`, `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt`, `library/src/test/kotlin/com/mordred/aero/components/{buttons,range}/`
**Files scanned:** 10 (AeroSwitch.kt, AeroSegmentedControl.kt, AeroListItem.kt, AeroRadioButton.kt, AeroButtonSurface.kt, AeroSurfaceStyle.kt, InteractionStates.kt, AeroSlider.kt (targeted grep + partial read), AeroSliderStylesTest.kt, AeroButtonSemanticsTest.kt, AeroButtonSurfaceSourceTest.kt, AeroSliderSourceTest.kt)
**Pattern extraction date:** 2026-07-27
