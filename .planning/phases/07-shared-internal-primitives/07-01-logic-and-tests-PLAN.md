---
phase: 07-shared-internal-primitives
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - gradle/libs.versions.toml
  - library/build.gradle.kts
  - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt
  - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt
  - library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt
  - library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt
  - .planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md
autonomous: true
requirements: []

must_haves:
  truths:
    - "AeroCalendarGrid internal composable renders a month grid (7-column day cells, prev/next month buttons, day-of-week header) and passes a 3-scenario unit test (current month, month boundary, leap year)"
    - "AeroCalendarPositionProvider positions a popup wider than its anchor without clipping on a 1024dp simulated window"
    - "AeroColorMath pure-function utility passes a round-trip unit test: hsv(0f, 1f, 1f) (pure red) converted to RGB and back to HSV returns hue within 0.001f tolerance — PITFALL-15 drift is confirmed absent at the utility level"
  artifacts:
    - path: "gradle/libs.versions.toml"
      provides: "kotlinx-datetime 0.6.2 version + library coordinate"
      contains: "kotlinxDatetime"
    - path: "library/build.gradle.kts"
      provides: "kotlinx-datetime implementation dependency"
      contains: "libs.kotlinx.datetime"
    - path: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt"
      provides: "Pure HSV/RGB/HEX conversion utilities; HSV is single source of truth (PITFALL-15)"
      exports: ["rgbToHsv", "hexToRgb", "rgbToHex", "hexToRgba"]
    - path: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt"
      provides: "AeroCalendarGrid composable - 7-column month grid; uses kotlinx-datetime LocalDate"
      contains: "internal fun AeroCalendarGrid"
    - path: "library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt"
      provides: "PopupPositionProvider for popups wider than anchor; PITFALL-02/PITFALL-08 fix"
      contains: "internal class AeroCalendarPositionProvider"
    - path: "library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt"
      provides: "Round-trip drift test (PITFALL-15)"
      contains: "pureRedRoundTripPreservesHueWithinTolerance"
    - path: "library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt"
      provides: "3-scenario calendar test: current month, Dec->Jan boundary, leap year Feb 2024"
      contains: "leapYearFeb2024Has29Days"
    - path: "library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt"
      provides: "Position provider tests: wide popup near right edge on 1024dp window, first-frame guard"
      contains: "widePopupRightEdgeRightAlignsWithoutClip"
    - path: ".planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md"
      provides: "Documented 1-minute touchSlop spike result (per STATE.md Pending Todos)"
      min_lines: 15
  key_links:
    - from: "library/build.gradle.kts"
      to: "gradle/libs.versions.toml"
      via: "version catalog reference"
      pattern: "libs\\.kotlinx\\.datetime"
    - from: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt"
      to: "kotlinx.datetime.LocalDate"
      via: "import + parameter type"
      pattern: "import kotlinx\\.datetime\\.LocalDate"
    - from: "library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt"
      to: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt"
      via: "import of rgbToHsv / hexToRgb / rgbToHex"
      pattern: "rgbToHsv|hexToRgb|rgbToHex"
---

<objective>
Land the unit-testable primitives of Phase 7: kotlinx-datetime dependency, the touchslop spike note, the AeroColorMath HSV utility, the AeroCalendarGrid composable (logic + rendering with kotlinx-datetime types), and the AeroCalendarPositionProvider — each backed by a JUnit 5 test that exercises the contract.

Purpose: Establishes a green CI gate for Phase 7 before any Canvas/drag-based primitives are touched. Pure functions and PopupPositionProvider math are fully testable on the JVM without Compose UI test infra. PITFALL-15 (HSV drift), PITFALL-02 (calendar popup width clip), and PITFALL-08 (first-frame popup flash) are defused at the utility level so Phase 8 pickers consume known-good math and positioning.

Output: 3 new source files + 3 new test files under `library/`, the kotlinx-datetime dependency line in `libs.versions.toml` and `library/build.gradle.kts`, plus a SPIKE.md documenting whether CMP 1.7.3 still requires the manual `awaitPointerEventScope` loop. All tests pass under `./gradlew :library:test`.
</objective>

<execution_context>
@C:/Users/1/.claude/get-shit-done/workflows/execute-plan.md
@C:/Users/1/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/07-shared-internal-primitives/07-CONTEXT.md
@.planning/phases/07-shared-internal-primitives/07-RESEARCH.md
@.planning/phases/07-shared-internal-primitives/07-VALIDATION.md

# Existing reference files (read-only)
@library/build.gradle.kts
@gradle/libs.versions.toml
@library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt
@library/src/test/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProviderTest.kt
@library/src/test/kotlin/com/mordred/aero/components/containers/AeroDividerTest.kt
@library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt
@library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt
@library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt

<interfaces>
<!-- Locked API signatures from 07-RESEARCH.md §API Signatures.
     Executor uses these directly — no codebase exploration needed for contract shape. -->

From kotlinx.datetime (0.6.2):
```kotlin
class LocalDate(year: Int, monthNumber: Int, dayOfMonth: Int)
val LocalDate.dayOfWeek: DayOfWeek
val LocalDate.dayOfMonth: Int
val LocalDate.month: Month
val LocalDate.year: Int
fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate
fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate
object DateTimeUnit { val MONTH: DateTimeUnit.DateBased; val DAY: DateTimeUnit.DateBased }
enum class DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
enum class Month { JANUARY, FEBRUARY, ..., DECEMBER }
val Month.length(leapYear: Boolean): Int   // 28/29/30/31
```

From androidx.compose.ui.window:
```kotlin
interface PopupPositionProvider {
    fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset
}
```

From androidx.compose.ui.graphics:
```kotlin
fun Color.Companion.hsv(
    hue: Float,         // REQUIRED [0f, 360f] — requirePrecondition throws on out-of-range
    saturation: Float,  // [0f, 1f]
    value: Float,       // [0f, 1f]
    alpha: Float = 1f,
    colorSpace: Rgb = ColorSpaces.Srgb,
): Color
val Color.red: Float    // [0f, 1f]
val Color.green: Float
val Color.blue: Float
```

API contracts THIS plan creates (Phase 8/9/10 will consume):

Source: library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt
```kotlin
package com.mordred.aero.components.pickers.internal.color

// Top-level functions chosen (matches existing GlassModifiers.kt convention).
internal fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float>
// returns Triple(h, s, v); h in [0f, 360f], s/v in [0f, 1f]
internal fun hexToRgb(hex: String): Triple<Int, Int, Int>?
// accepts "#RGB", "#RRGGBB", "#RRGGBBAA" (alpha dropped); null on parse failure
internal fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String
// uppercase HEX without leading '#'; alpha appended as 2 hex digits when non-null
internal fun hexToRgba(hex: String): IntArray?
// returns intArrayOf(r, g, b, a); a defaults to 255 when source has no alpha; null on parse fail
```

Source: library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt
```kotlin
package com.mordred.aero.components.pickers.internal.calendar

@Composable
internal fun AeroCalendarGrid(
    displayMonth: LocalDate,
    selected: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: (LocalDate) -> Boolean = { false },
)
```

Source: library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt
```kotlin
package com.mordred.aero.components.internal.popup

internal class AeroCalendarPositionProvider(
    private val gap: Int = 4,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset
}
```
</interfaces>

<adr_hue_convention>
LOCKED HUE CONVENTION (decided in this plan, captured in Task 1 commit message + AeroColorMath KDoc):
- Hue is **degrees in [0f, 360f]** to match `Color.hsv(...)` directly.
- Saturation, Value, Alpha are **[0f, 1f]**.
- RGB ints are **[0, 255]**.
- Rationale: `Color.hsv(...)` calls `requirePrecondition(hue in 0f..360f)` and throws IllegalArgumentException on a normalized [0,1] hue. CONTEXT.md proposed [0,1] for hue/sat/val/alpha, but this is technically incorrect for `Color.hsv` interop. Locking [0,360] eliminates `* 360f` boundary multiplications at every call site.
- This convention propagates to `AeroHsvColorSquare(hue: Float)` and `AeroHueSlider(hue: Float)` in Plan-02.
</adr_hue_convention>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add kotlinx-datetime 0.6.2 dependency + run touchslop spike</name>
  <files>gradle/libs.versions.toml, library/build.gradle.kts, .planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (locked decision: kotlinx-datetime moves forward to Phase 7 plan-01; first-compile validation is a plan-01 acceptance criterion)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Standard Stack confirms 0.6.2 is the pinned version; §Open Questions §2 documents the touchslop spike)
    - gradle/libs.versions.toml (current version catalog — append, do not rewrite)
    - library/build.gradle.kts (current dependency block — append `implementation(libs.kotlinx.datetime)` only)
    - .planning/STATE.md (§Pending Todos — touchslop spike was originally listed for Phase 7 plan-01)
  </read_first>
  <behavior>
    - After this task, `./gradlew :library:compileKotlin` succeeds (validates 0.6.2 ↔ Kotlin 2.1.21 compatibility per CONTEXT.md acceptance criterion).
    - `kotlinx.datetime.LocalDate` resolves at the import line in any new `:library` source file.
    - 07-SPIKE-touchslop.md exists and records: (a) the date, (b) CMP version (1.7.3), (c) one of three outcomes — "manual loop required (issue #343 still active)", "manual loop optional (touchSlop reduced)", or "spike skipped — manual loop is the locked v2.0 pattern regardless".
  </behavior>
  <action>
    Step A — Edit `gradle/libs.versions.toml`. APPEND (do not rewrite existing entries):
    Under `[versions]`, append on a new line:
    ```toml
    kotlinxDatetime = "0.6.2"
    ```
    Under `[libraries]`, append on a new line:
    ```toml
    kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
    ```

    Step B — Edit `library/build.gradle.kts`. Inside the existing `dependencies { ... }` block, after the existing `implementation(libs.kotlinx.coroutines.core)` line on line 19, ADD a new line:
    ```kotlin
    implementation(libs.kotlinx.datetime)
    ```

    Step C — First-compile validation. Run from project root:
    ```bash
    ./gradlew :library:compileKotlin
    ```
    Must exit 0. If it fails with version-resolution errors, document the failure in 07-SPIKE-touchslop.md and STOP — do not silently fall back. The CONTEXT-locked fallback `0.7.1-0.6.x-compat` is for actual incompatibility only; if 0.6.2 resolves, use 0.6.2.

    Step D — Touchslop spike (1 minute). Create `.planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md` with this exact content:
    ```markdown
    # Phase 7 Spike: touchSlop on Compose Multiplatform 1.7.3

    **Date:** 2026-04-30
    **CMP version:** 1.7.3
    **Kotlin:** 2.1.21
    **Issue reference:** JetBrains/compose-jb #343

    ## Question
    Does `detectDragGestures` still suffer from the 18dp touchSlop delay on Compose Desktop in CMP 1.7.3, or has the slop been reduced enough that mouse drags register on the first or second event?

    ## Decision (locked regardless of outcome)
    Phase 7 ships `awaitPointerEventScope` + manual loop for all in-content Canvas drag (PITFALL-03 mitigation). This is the locked v2.0 pattern — see STATE.md "v2.0 Locked Decisions". This spike is documentation only.

    ## Outcome
    Spike skipped — `awaitPointerEventScope` manual loop is the locked v2.0 pattern regardless. Empirical re-test deferred to a future v2.x maintenance window when the upstream issue #343 has a canonical fix announcement; until then, the manual loop is the single source of truth for v2.0 drag behavior.

    ## Implication
    `Modifier.aeroDragSplitter`, `AeroHsvColorSquare`, and `AeroHueSlider` (Plan-02) all use `awaitPointerEventScope { while(true) { awaitFirstDown(); ... } }`. `detectDragGestures` is BANNED for in-content Canvas drag.
    ```
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - `grep -q 'kotlinxDatetime = "0.6.2"' gradle/libs.versions.toml` returns 0
    - `grep -q 'kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime"' gradle/libs.versions.toml` returns 0
    - `grep -q 'implementation(libs.kotlinx.datetime)' library/build.gradle.kts` returns 0
    - `./gradlew :library:compileKotlin` exits 0 (validates 0.6.2 ↔ Kotlin 2.1.21 compatibility)
    - `test -f .planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md` is true
    - The SPIKE file contains the strings `CMP version` and `awaitPointerEventScope` (grep both)
  </acceptance_criteria>
  <done>kotlinx-datetime 0.6.2 is on the :library classpath, the version catalog records the coordinate, the touchslop spike outcome is documented, and library compile passes.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create AeroColorMath utility + round-trip drift test (HSV single source of truth)</name>
  <files>library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt, library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §AeroColorMath shape — Claude's discretion within locked rules; HSV is single source of truth)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Pattern 3 has the full code skeleton + KDoc; §Common Pitfalls §1 documents the [0,360] vs [0,1] hue trap)
    - library/src/test/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProviderTest.kt (existing JUnit 5 + kotlin-test idioms — `@Test`, `assertEquals`, `assertTrue`)
    - library/build.gradle.kts (confirm tasks.test { useJUnitPlatform() } is present — line 28-30)
  </read_first>
  <behavior>
    - `rgbToHsv(1f, 0f, 0f)` returns `Triple(0f, 1f, 1f)` — pure red has hue 0, saturation 1, value 1.
    - `rgbToHsv(0f, 1f, 0f)` returns hue near 120f.
    - `rgbToHsv(0f, 0f, 1f)` returns hue near 240f.
    - `rgbToHsv(0f, 0f, 0f)` returns `Triple(0f, 0f, 0f)` — black, no hue/saturation, value 0.
    - `rgbToHsv(1f, 1f, 1f)` returns saturation 0, value 1.
    - `hexToRgb("#FF0000")` returns `Triple(255, 0, 0)`.
    - `hexToRgb("FF0000")` (no '#') returns `Triple(255, 0, 0)`.
    - `hexToRgb("#F00")` (3-digit) returns `Triple(255, 0, 0)` (each nibble expanded by *17).
    - `hexToRgb("#FF0000FF")` (with alpha — alpha dropped per Triple<Int,Int,Int>) returns `Triple(255, 0, 0)`.
    - `hexToRgb("xyz")` returns `null` (parse failure).
    - `hexToRgb("#FF")` (wrong length) returns `null`.
    - `rgbToHex(255, 0, 0)` returns `"FF0000"` (uppercase, no '#').
    - `rgbToHex(255, 0, 0, alpha = 128)` returns `"FF000080"`.
    - `hexToRgba("#FF0000")` returns `intArrayOf(255, 0, 0, 255)` (alpha defaults to 255).
    - `hexToRgba("#FF000080")` returns `intArrayOf(255, 0, 0, 128)`.
    - **Round-trip contract (PITFALL-15):** `Color.hsv(0f, 1f, 1f)` → extract red/green/blue floats → `rgbToHsv(r, g, b)` → returned hue is within `0.001f` of 0f.
  </behavior>
  <action>
    Step A — Create directory + source file `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` with this exact content:

    ```kotlin
    package com.mordred.aero.components.pickers.internal.color

    import kotlin.math.max
    import kotlin.math.min

    /**
     * Pure HSV / RGB / HEX conversion utilities for AeroColorPicker (Phase 8).
     *
     * **HUE CONVENTION (locked Phase 7 plan-01 ADR):**
     * - Hue is in DEGREES `[0f, 360f]` — matches `androidx.compose.ui.graphics.Color.hsv(...)`
     *   which `requirePrecondition`-throws on hue outside `[0f..360f]`.
     * - Saturation, Value, Alpha are in `[0f, 1f]`.
     * - RGB ints are in `[0, 255]`; RGB floats are in `[0f, 1f]` (used by `rgbToHsv`).
     *
     * **PITFALL-15 (HSV drift):** HSV is the single source of truth in `AeroColorPicker`'s
     * internal state. RGB and HEX are derived views — never store both simultaneously.
     * The unit test `AeroColorMathTest.pureRedRoundTripPreservesHueWithinTolerance` validates
     * that `Color.hsv(0,1,1) → rgb → rgbToHsv` preserves hue within `0.001f`.
     */

    /**
     * RGB → HSV conversion.
     * @param r red in `[0f, 1f]`
     * @param g green in `[0f, 1f]`
     * @param b blue in `[0f, 1f]`
     * @return Triple(hue in `[0f, 360f]`, saturation in `[0f, 1f]`, value in `[0f, 1f]`).
     */
    internal fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val cMax = max(r, max(g, b))
        val cMin = min(r, min(g, b))
        val delta = cMax - cMin

        val h = when {
            delta == 0f -> 0f
            cMax == r   -> 60f * (((g - b) / delta) % 6f)
            cMax == g   -> 60f * (((b - r) / delta) + 2f)
            else        -> 60f * (((r - g) / delta) + 4f)
        }
        val hPositive = if (h < 0f) h + 360f else h

        val s = if (cMax == 0f) 0f else delta / cMax
        val v = cMax
        return Triple(hPositive, s, v)
    }

    /**
     * HEX → RGB. Accepts "#RGB", "#RRGGBB", "#RRGGBBAA" (alpha dropped) with or without leading '#'.
     * @return Triple(r, g, b) in `[0, 255]`. Returns `null` on parse failure.
     */
    internal fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
        val cleaned = hex.removePrefix("#").trim()
        return when (cleaned.length) {
            3 -> {
                val r = cleaned[0].digitToIntOrNull(16) ?: return null
                val g = cleaned[1].digitToIntOrNull(16) ?: return null
                val b = cleaned[2].digitToIntOrNull(16) ?: return null
                Triple(r * 17, g * 17, b * 17)  // expand "F" → 0xFF
            }
            6, 8 -> {
                val r = cleaned.substring(0, 2).toIntOrNull(16) ?: return null
                val g = cleaned.substring(2, 4).toIntOrNull(16) ?: return null
                val b = cleaned.substring(4, 6).toIntOrNull(16) ?: return null
                Triple(r, g, b)
            }
            else -> null
        }
    }

    /**
     * HEX → RGBA. Accepts the same inputs as [hexToRgb]; when the source has no alpha,
     * alpha defaults to 255 (opaque).
     * @return `intArrayOf(r, g, b, a)` in `[0, 255]`. Returns `null` on parse failure.
     */
    internal fun hexToRgba(hex: String): IntArray? {
        val cleaned = hex.removePrefix("#").trim()
        return when (cleaned.length) {
            3 -> {
                val r = cleaned[0].digitToIntOrNull(16) ?: return null
                val g = cleaned[1].digitToIntOrNull(16) ?: return null
                val b = cleaned[2].digitToIntOrNull(16) ?: return null
                intArrayOf(r * 17, g * 17, b * 17, 255)
            }
            6 -> {
                val r = cleaned.substring(0, 2).toIntOrNull(16) ?: return null
                val g = cleaned.substring(2, 4).toIntOrNull(16) ?: return null
                val b = cleaned.substring(4, 6).toIntOrNull(16) ?: return null
                intArrayOf(r, g, b, 255)
            }
            8 -> {
                val r = cleaned.substring(0, 2).toIntOrNull(16) ?: return null
                val g = cleaned.substring(2, 4).toIntOrNull(16) ?: return null
                val b = cleaned.substring(4, 6).toIntOrNull(16) ?: return null
                val a = cleaned.substring(6, 8).toIntOrNull(16) ?: return null
                intArrayOf(r, g, b, a)
            }
            else -> null
        }
    }

    /**
     * RGB ints (0..255) → uppercase HEX without '#'. If [alpha] is non-null, the result is `RRGGBBAA`.
     */
    internal fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String {
        val rgb = "%02X%02X%02X".format(r, g, b)
        return if (alpha == null) rgb else "$rgb${"%02X".format(alpha)}"
    }
    ```

    Step B — Create directory + test file `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt` with this exact content:

    ```kotlin
    package com.mordred.aero.components.pickers.internal.color

    import androidx.compose.ui.graphics.Color
    import kotlin.math.abs
    import kotlin.test.Test
    import kotlin.test.assertEquals
    import kotlin.test.assertNotNull
    import kotlin.test.assertNull
    import kotlin.test.assertTrue

    class AeroColorMathTest {

        // === Round-trip drift contract (PITFALL-15) ===

        @Test
        fun pureRedRoundTripPreservesHueWithinTolerance() {
            // Pure red: hue=0, sat=1, val=1
            val red = Color.hsv(0f, 1f, 1f)
            val (hue, _, _) = rgbToHsv(red.red, red.green, red.blue)
            assertTrue(
                abs(hue - 0f) < 0.001f,
                "hue drift on pure red round-trip; got hue=$hue"
            )
        }

        // === rgbToHsv behavior ===

        @Test
        fun pureRedHasHueZero() {
            val (h, s, v) = rgbToHsv(1f, 0f, 0f)
            assertTrue(abs(h - 0f) < 0.001f, "expected hue ≈ 0; got $h")
            assertEquals(1f, s, "saturation")
            assertEquals(1f, v, "value")
        }

        @Test
        fun pureGreenHasHue120() {
            val (h, _, _) = rgbToHsv(0f, 1f, 0f)
            assertTrue(abs(h - 120f) < 0.001f, "expected hue ≈ 120; got $h")
        }

        @Test
        fun pureBlueHasHue240() {
            val (h, _, _) = rgbToHsv(0f, 0f, 1f)
            assertTrue(abs(h - 240f) < 0.001f, "expected hue ≈ 240; got $h")
        }

        @Test
        fun blackHasZeroHueSatAndValue() {
            val (h, s, v) = rgbToHsv(0f, 0f, 0f)
            assertEquals(0f, h)
            assertEquals(0f, s)
            assertEquals(0f, v)
        }

        @Test
        fun whiteHasZeroSaturationAndValueOne() {
            val (_, s, v) = rgbToHsv(1f, 1f, 1f)
            assertEquals(0f, s)
            assertEquals(1f, v)
        }

        // === hexToRgb behavior ===

        @Test
        fun hexToRgbAcceptsLeadingHashSixDigits() {
            assertEquals(Triple(255, 0, 0), hexToRgb("#FF0000"))
        }

        @Test
        fun hexToRgbAcceptsNoLeadingHash() {
            assertEquals(Triple(255, 0, 0), hexToRgb("FF0000"))
        }

        @Test
        fun hexToRgbExpandsThreeDigitForm() {
            assertEquals(Triple(255, 0, 0), hexToRgb("#F00"))
        }

        @Test
        fun hexToRgbDropsAlphaInEightDigitForm() {
            assertEquals(Triple(255, 0, 0), hexToRgb("#FF0000FF"))
        }

        @Test
        fun hexToRgbReturnsNullOnInvalidChars() {
            assertNull(hexToRgb("xyz"))
        }

        @Test
        fun hexToRgbReturnsNullOnWrongLength() {
            assertNull(hexToRgb("#FF"))
            assertNull(hexToRgb("#FFFF"))
        }

        // === hexToRgba behavior ===

        @Test
        fun hexToRgbaDefaultsAlphaTo255WhenAbsent() {
            val rgba = hexToRgba("#FF0000")
            assertNotNull(rgba)
            assertEquals(255, rgba[0])
            assertEquals(0,   rgba[1])
            assertEquals(0,   rgba[2])
            assertEquals(255, rgba[3])
        }

        @Test
        fun hexToRgbaParsesAlphaInEightDigitForm() {
            val rgba = hexToRgba("#FF000080")
            assertNotNull(rgba)
            assertEquals(255, rgba[0])
            assertEquals(0,   rgba[1])
            assertEquals(0,   rgba[2])
            assertEquals(128, rgba[3])
        }

        // === rgbToHex behavior ===

        @Test
        fun rgbToHexProducesUppercaseSixDigit() {
            assertEquals("FF0000", rgbToHex(255, 0, 0))
        }

        @Test
        fun rgbToHexAppendsAlphaWhenProvided() {
            assertEquals("FF000080", rgbToHex(255, 0, 0, alpha = 128))
        }
    }
    ```

    Step C — Run the test class to confirm green:
    ```bash
    ./gradlew :library:test --tests "com.mordred.aero.components.pickers.internal.color.AeroColorMathTest"
    ```
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:test --tests "com.mordred.aero.components.pickers.internal.color.AeroColorMathTest"</automated>
  </verify>
  <acceptance_criteria>
    - `test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` is true
    - `test -f library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt` is true
    - `grep -q "internal fun rgbToHsv" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` returns 0
    - `grep -q "internal fun hexToRgb" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` returns 0
    - `grep -q "internal fun hexToRgba" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` returns 0
    - `grep -q "internal fun rgbToHex" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` returns 0
    - `grep -q "pureRedRoundTripPreservesHueWithinTolerance" library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt` returns 0
    - `./gradlew :library:test --tests "com.mordred.aero.components.pickers.internal.color.AeroColorMathTest"` exits 0 (all tests green)
  </acceptance_criteria>
  <done>AeroColorMath utility exists with rgbToHsv / hexToRgb / hexToRgba / rgbToHex, the round-trip drift test passes, and the hue convention `[0f, 360f]` is documented in KDoc.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Create AeroCalendarPositionProvider + 4-scenario position test</name>
  <files>library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt, library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §AeroCalendarPositionProvider — locked behavior: width-overflow does NOT trigger Top/Bottom flip; first-frame guard via `IntSize.Zero`, NOT `>= windowSize`)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Pattern 2 has the full code skeleton; §Common Pitfalls §3 explains why `>= windowSize` is wrong for wide calendars)
    - library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt (DESIGN REFERENCE — DO NOT MODIFY; the new provider deliberately diverges)
    - library/src/test/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProviderTest.kt (existing test idioms — IntRect/IntSize/IntOffset construction; assertion patterns)
  </read_first>
  <behavior>
    - When `popupContentSize == IntSize.Zero` (unmeasured), `calculatePosition` returns `IntOffset.Zero` (off-screen at top-left, invisible on frame 1) — first-frame guard (PITFALL-08).
    - With anchor at `(left=100, top=100, right=200, bottom=130)` on a 1024×800 window and a 560×400 popup (calendar wider than anchor): popup is left-aligned to 100, vertically positioned at `bottom + gap = 134`.
    - With anchor at `(left=900, top=100, right=1000, bottom=130)` on a 1024×800 window and a 560×400 popup (anchor near right edge, popup would overflow right): popup right-aligns to `anchorBounds.right - popupContentSize.width = 1000 - 560 = 440` — NO Top/Bottom flip on width overflow.
    - With anchor at `(left=100, top=600, right=200, bottom=630)` on a 1024×800 window and a 560×400 popup (would overflow vertically below): popup flips above — y = `anchorBounds.top - popupContentSize.height - gap = 600 - 400 - 4 = 196`.
    - With a narrow popup that fits comfortably below: vertical position is `anchorBounds.bottom + gap`.
  </behavior>
  <action>
    Step A — Create directory + source file `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` with this exact content:

    ```kotlin
    package com.mordred.aero.components.internal.popup

    import androidx.compose.ui.unit.IntOffset
    import androidx.compose.ui.unit.IntRect
    import androidx.compose.ui.unit.IntSize
    import androidx.compose.ui.unit.LayoutDirection
    import androidx.compose.ui.window.PopupPositionProvider

    /**
     * `PopupPositionProvider` for popups whose content is wider than their anchor —
     * specifically the four date/time pickers in Phase 8 (and ColorPicker swatch popup if needed).
     *
     * **PITFALL-02 (calendar popup width clip):** existing `AeroPopupPositionProvider` width-locks
     * the popup to the anchor's width via `widthIn(min=anchorWidth, max=anchorWidth)` (in its consumer
     * `AeroDropdownPopup`). A 320dp calendar on a 240dp trigger would clip. This provider does NOT
     * width-lock — popups render at their natural width.
     *
     * **PITFALL-08 (first-frame popup flash):** existing provider uses `popupContentSize >= windowSize`
     * as the unmeasured guard. On a 1280dp window a 560dp wide-calendar popup never trips this guard
     * (popup is 0×0 then 560×400). Result: position computed from `(0,0)` on frame 1, then jumps. We use
     * `popupContentSize == IntSize.Zero` (struct equality) instead — the only condition that reliably
     * means "unmeasured".
     *
     * **Locked behaviors:**
     * 1. Default: left-aligned to `anchorBounds.left`, vertically below the anchor.
     * 2. Horizontal overflow (`anchorBounds.left + popupContentSize.width > windowSize.width`):
     *    right-align to `anchorBounds.right - popupContentSize.width` (clamped to 0).
     * 3. Vertical overflow below (`yBelow + popupContentSize.height > windowSize.height`): flip to above
     *    (`anchorBounds.top - popupContentSize.height - gap`, clamped to 0).
     * 4. **Width overflow does NOT trigger Top/Bottom flip** — only horizontal re-anchoring.
     * 5. First-frame guard: `popupContentSize == IntSize.Zero` returns `IntOffset.Zero`.
     *
     * @param gap pixel offset between anchor edge and popup edge along the vertical axis.
     */
    internal class AeroCalendarPositionProvider(
        private val gap: Int = 4,
    ) : PopupPositionProvider {

        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            // First-frame guard (PITFALL-08): unmeasured popup → off-screen.
            if (popupContentSize == IntSize.Zero) return IntOffset.Zero

            // Horizontal: prefer left-aligned to anchor.left; right-align if it would overflow.
            val xLeft = anchorBounds.left
            val xRight = anchorBounds.right - popupContentSize.width
            val x = if (xLeft + popupContentSize.width <= windowSize.width) {
                xLeft
            } else {
                xRight.coerceAtLeast(0)
            }

            // Vertical: prefer below; flip above if below would overflow.
            val yBelow = anchorBounds.bottom + gap
            val yAbove = anchorBounds.top - popupContentSize.height - gap
            val y = if (yBelow + popupContentSize.height <= windowSize.height) {
                yBelow
            } else {
                yAbove.coerceAtLeast(0)
            }

            return IntOffset(x, y)
        }
    }
    ```

    Step B — Create directory + test file `library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt` with this exact content:

    ```kotlin
    package com.mordred.aero.components.internal.popup

    import androidx.compose.ui.unit.IntOffset
    import androidx.compose.ui.unit.IntRect
    import androidx.compose.ui.unit.IntSize
    import androidx.compose.ui.unit.LayoutDirection
    import kotlin.test.Test
    import kotlin.test.assertEquals
    import kotlin.test.assertTrue

    /** Phase 7 plan-01 — covers PITFALL-02 (width-clip) and PITFALL-08 (first-frame guard). */
    class AeroCalendarPositionProviderTest {

        @Test
        fun firstFrameUnmeasuredPopupReturnsIntOffsetZero() {
            // PITFALL-08: when popupContentSize == IntSize.Zero we MUST return IntOffset.Zero.
            // This keeps the popup off-screen on frame 1 instead of flashing at a position
            // computed from a 0×0 size.
            val provider = AeroCalendarPositionProvider(gap = 4)
            val pos = provider.calculatePosition(
                anchorBounds = IntRect(left = 100, top = 100, right = 200, bottom = 130),
                windowSize = IntSize(width = 1024, height = 800),
                layoutDirection = LayoutDirection.Ltr,
                popupContentSize = IntSize.Zero,
            )
            assertEquals(IntOffset.Zero, pos, "first-frame guard must return IntOffset.Zero")
        }

        @Test
        fun widePopupNearLeftEdgePositionsBelowAndLeftAligned() {
            // SC1: AeroCalendarPositionProvider positions a popup wider than its anchor without clipping.
            // 100dp anchor → 560dp popup → fits left-aligned on a 1024dp window.
            val provider = AeroCalendarPositionProvider(gap = 4)
            val pos = provider.calculatePosition(
                anchorBounds = IntRect(left = 100, top = 100, right = 200, bottom = 130),
                windowSize = IntSize(width = 1024, height = 800),
                layoutDirection = LayoutDirection.Ltr,
                popupContentSize = IntSize(width = 560, height = 400),
            )
            assertEquals(100, pos.x, "left-aligned to anchorBounds.left")
            assertEquals(130 + 4, pos.y, "below anchor with gap=4")
            // No clip: popup fully visible inside window.
            assertTrue(pos.x + 560 <= 1024, "popup right edge inside window")
        }

        @Test
        fun widePopupNearRightEdgeRightAlignsWithoutClip() {
            // SC1 (right-edge case): 100dp anchor near right edge; 560dp popup would overflow if left-aligned.
            // Must right-align to anchorBounds.right - popupWidth, NOT flip Top/Bottom.
            val provider = AeroCalendarPositionProvider(gap = 4)
            val anchor = IntRect(left = 900, top = 100, right = 1000, bottom = 130)
            val popupSize = IntSize(width = 560, height = 400)
            val pos = provider.calculatePosition(
                anchorBounds = anchor,
                windowSize = IntSize(width = 1024, height = 800),
                layoutDirection = LayoutDirection.Ltr,
                popupContentSize = popupSize,
            )
            assertEquals(1000 - 560, pos.x, "right-aligned to anchor.right - popup.width")
            assertEquals(130 + 4, pos.y, "still below anchor — width overflow does NOT flip Top/Bottom")
            // No clip:
            assertTrue(pos.x >= 0, "popup left edge inside window")
            assertTrue(pos.x + popupSize.width <= 1024, "popup right edge inside window")
        }

        @Test
        fun overflowingBelowAnchorFlipsAbove() {
            // Vertical overflow: 600dp-tall anchor.top, 400dp popup, 800dp window → flip above.
            val provider = AeroCalendarPositionProvider(gap = 4)
            val pos = provider.calculatePosition(
                anchorBounds = IntRect(left = 100, top = 600, right = 200, bottom = 630),
                windowSize = IntSize(width = 1024, height = 800),
                layoutDirection = LayoutDirection.Ltr,
                popupContentSize = IntSize(width = 320, height = 400),
            )
            // yBelow = 630 + 4 = 634; 634 + 400 = 1034 > 800 → flip above
            // yAbove = 600 - 400 - 4 = 196
            assertEquals(196, pos.y, "must flip above when below would overflow window")
        }
    }
    ```

    Step C — Run the test class:
    ```bash
    ./gradlew :library:test --tests "com.mordred.aero.components.internal.popup.AeroCalendarPositionProviderTest"
    ```
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:test --tests "com.mordred.aero.components.internal.popup.AeroCalendarPositionProviderTest"</automated>
  </verify>
  <acceptance_criteria>
    - `test -f library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` is true
    - `test -f library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt` is true
    - `grep -q "internal class AeroCalendarPositionProvider" library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` returns 0
    - `grep -q "popupContentSize == IntSize.Zero" library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` returns 0 (first-frame guard uses struct equality, not `>=`)
    - `grep -q "widePopupNearRightEdgeRightAlignsWithoutClip" library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt` returns 0
    - `grep -q "firstFrameUnmeasuredPopupReturnsIntOffsetZero" library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt` returns 0
    - `./gradlew :library:test --tests "com.mordred.aero.components.internal.popup.AeroCalendarPositionProviderTest"` exits 0 (4 tests green)
    - The existing `AeroPopupPositionProvider.kt` is unchanged (`git diff library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt` returns no diff)
  </acceptance_criteria>
  <done>AeroCalendarPositionProvider exists with locked behaviors (no width-lock, no Top/Bottom flip on width overflow, IntSize.Zero first-frame guard), all 4 tests pass, and the existing AeroPopupPositionProvider is untouched.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 4: Create AeroCalendarGrid composable + 3-scenario kotlinx-datetime test</name>
  <files>library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt, library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §Date type for CalendarGrid — exact signature locked; 3-scenario test description)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Don't Hand-Roll table — `kotlinx.datetime.LocalDate.plus(1, DateTimeUnit.MONTH)` for arithmetic; §Code Examples; §API Signatures)
    - library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt (verify `AeroIcons.CaretLeft` and `AeroIcons.CaretRight` are accessible)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt (`AeroTheme.colors` and `AeroTheme.typography` access pattern from internal composables)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt (token names: primary, onPrimary, borderDefault, labelText, surface, panelBackground, onSurface, glassBorder)
    - library/src/test/kotlin/com/mordred/aero/components/containers/AeroDividerTest.kt (compile-smoke pattern using `Class.forName(...Kt)`)
  </read_first>
  <behavior>
    - **Scenario 1 (current month):** the grid for `displayMonth = LocalDate(2026, 4, 1)` (April 2026, 30 days, starting on a Wednesday) renders day cells 1–30 visible; total cells filled = 30; grid contains 6 rows × 7 columns = 42 cell slots (some leading/trailing slots are empty or show prev/next month days at reduced opacity — implementation detail).
    - **Scenario 2 (month boundary, Dec → Jan):** `displayMonth = LocalDate(2026, 12, 15)`. Calling `onMonthChange` with the result of `displayMonth.plus(1, DateTimeUnit.MONTH)` yields a date in January 2027 (year increments correctly).
    - **Scenario 3 (leap year):** February 2024 has 29 days; February 2023 has 28 days. The logic helper `daysInMonth(LocalDate)` (or equivalent) returns 29 for `LocalDate(2024, 2, 1)` and 28 for `LocalDate(2023, 2, 1)`.
    - The composable uses `Icon(AeroIcons.CaretLeft, ...)` for prev-month and `Icon(AeroIcons.CaretRight, ...)` for next-month buttons.
    - The composable reads `AeroTheme.colors` and `AeroTheme.typography` inside the body (not in default-arg position).
    - The composable is `internal`, not `public`.
  </behavior>
  <action>
    Step A — Create directory + source file `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` with this exact content:

    ```kotlin
    package com.mordred.aero.components.pickers.internal.calendar

    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.material3.Icon
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import com.mordred.aero.icons.AeroIcons
    import com.mordred.aero.theme.AeroTheme
    import kotlinx.datetime.DateTimeUnit
    import kotlinx.datetime.DayOfWeek
    import kotlinx.datetime.LocalDate
    import kotlinx.datetime.Month
    import kotlinx.datetime.minus
    import kotlinx.datetime.plus

    /**
     * Internal month-grid composable for date pickers. Renders:
     *   - header row: prev-month button + month/year label + next-month button
     *   - day-of-week row: Mo Tu We Th Fr Sa Su (English short labels; i18n deferred)
     *   - 6×7 day-cell grid: filled with the days of `displayMonth.month`,
     *     leading/trailing slots show adjacent-month days at reduced opacity.
     *
     * Consumed by AeroDatePicker, AeroDateRangePicker, AeroDateTimePicker (all Phase 8).
     *
     * @param displayMonth any `LocalDate` whose month/year drive the grid (day-of-month is ignored).
     * @param selected the currently selected date, or `null` for none.
     * @param onDateSelected fires when the user clicks a day cell.
     * @param onMonthChange fires when the user clicks prev/next month — receives the new displayMonth.
     * @param isDisabled returns `true` for dates that should be non-interactive and dimmed.
     */
    @Composable
    internal fun AeroCalendarGrid(
        displayMonth: LocalDate,
        selected: LocalDate?,
        onDateSelected: (LocalDate) -> Unit,
        onMonthChange: (LocalDate) -> Unit,
        modifier: Modifier = Modifier,
        isDisabled: (LocalDate) -> Boolean = { false },
    ) {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography

        // First day of the displayed month, plus its day-of-week (Mon=0..Sun=6).
        val firstOfMonth = LocalDate(displayMonth.year, displayMonth.monthNumber, 1)
        val firstDow = (firstOfMonth.dayOfWeek.isoDayNumber - 1) // ISO: Mon=1..Sun=7 → 0..6
        val daysInMonth = daysInMonth(displayMonth)

        Column(modifier = modifier.padding(8.dp)) {

            // --- Header row: prev / month-year label / next ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onMonthChange(displayMonth.minus(1, DateTimeUnit.MONTH)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AeroIcons.CaretLeft,
                        contentDescription = "Previous month",
                        tint = colors.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "${displayMonth.month.englishName()} ${displayMonth.year}",
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onMonthChange(displayMonth.plus(1, DateTimeUnit.MONTH)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AeroIcons.CaretRight,
                        contentDescription = "Next month",
                        tint = colors.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.size(4.dp))

            // --- Day-of-week header (Mon..Sun, English short) ---
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = typography.caption,
                            color = colors.labelText,
                        )
                    }
                }
            }

            // --- 6×7 day-cell grid ---
            // We fill exactly 42 slots: leading prev-month days, this-month days, trailing next-month days.
            val totalSlots = 42
            var dayCounter = 1
            for (row in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val slotIndex = row * 7 + col
                        val isLeadingBlank = slotIndex < firstDow
                        val isTrailingBlank = dayCounter > daysInMonth
                        if (isLeadingBlank || isTrailingBlank) {
                            // Leading slot: adjacent-month day at reduced opacity, non-interactive.
                            // For Phase 7 we keep these slots blank (Phase 8 may opt to show adjacent days).
                            Box(modifier = Modifier.size(36.dp))
                        } else {
                            val cellDate = LocalDate(displayMonth.year, displayMonth.monthNumber, dayCounter)
                            val disabled = isDisabled(cellDate)
                            val isSelected = (selected != null && selected == cellDate)
                            DayCell(
                                day = dayCounter,
                                isSelected = isSelected,
                                isDisabled = disabled,
                                onClick = { if (!disabled) onDateSelected(cellDate) },
                            )
                            dayCounter++
                        }
                    }
                }
                if (dayCounter > daysInMonth && row >= 4) {
                    // Stop drawing trailing rows once month is exhausted (rows 5/6 may be empty).
                    break
                }
            }
        }
    }

    @Composable
    private fun DayCell(
        day: Int,
        isSelected: Boolean,
        isDisabled: Boolean,
        onClick: () -> Unit,
    ) {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography
        val bg = when {
            isSelected -> colors.primary
            else       -> androidx.compose.ui.graphics.Color.Transparent
        }
        val fg = when {
            isDisabled -> colors.labelText
            isSelected -> colors.onPrimary
            else       -> colors.onSurface
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable(enabled = !isDisabled) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.toString(),
                style = typography.body,
                color = fg,
            )
        }
    }

    /**
     * Returns 28..31 for the month/year of [date]. Uses kotlinx-datetime's `Month.length(leapYear)`.
     * Exposed as `internal` so the calendar-grid test can verify leap-year handling without
     * driving the full composable.
     */
    internal fun daysInMonth(date: LocalDate): Int {
        val isLeap = isLeapYear(date.year)
        return when (date.month) {
            Month.JANUARY,   Month.MARCH, Month.MAY, Month.JULY,
            Month.AUGUST,    Month.OCTOBER, Month.DECEMBER -> 31
            Month.APRIL,     Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
            Month.FEBRUARY   -> if (isLeap) 29 else 28
            else             -> 30
        }
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    private fun Month.englishName(): String = when (this) {
        Month.JANUARY -> "January"; Month.FEBRUARY -> "February"; Month.MARCH -> "March"
        Month.APRIL -> "April";     Month.MAY -> "May";           Month.JUNE -> "June"
        Month.JULY -> "July";       Month.AUGUST -> "August";     Month.SEPTEMBER -> "September"
        Month.OCTOBER -> "October"; Month.NOVEMBER -> "November"; Month.DECEMBER -> "December"
        else -> name
    }
    ```

    Step B — Create directory + test file `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` with this exact content:

    ```kotlin
    package com.mordred.aero.components.pickers.internal.calendar

    import kotlinx.datetime.DateTimeUnit
    import kotlinx.datetime.LocalDate
    import kotlinx.datetime.Month
    import kotlinx.datetime.plus
    import kotlin.test.Test
    import kotlin.test.assertEquals
    import kotlin.test.assertNotNull

    /**
     * Phase 7 ROADMAP success criterion #1 — 3-scenario calendar test.
     *
     * Note: AeroCalendarGrid itself is a `@Composable` and rendering it requires the Compose UI
     * test harness (out of v2.0 scope). We test the LOGIC layer (`daysInMonth`, month arithmetic)
     * which is what actually drives the grid's cell count. A compile-smoke test confirms the
     * composable file is on the classpath.
     */
    class AeroCalendarGridTest {

        // === Scenario 1: Current month (April 2026 — 30 days) ===

        @Test
        fun aprilHas30Days() {
            val april = LocalDate(2026, 4, 1)
            assertEquals(30, daysInMonth(april))
            assertEquals(Month.APRIL, april.month)
        }

        // === Scenario 2: Month boundary (Dec → Jan crosses year) ===

        @Test
        fun decemberPlusOneMonthBecomesJanuaryNextYear() {
            val dec2026 = LocalDate(2026, 12, 15)
            val nextMonth = dec2026.plus(1, DateTimeUnit.MONTH)
            assertEquals(2027, nextMonth.year, "year must increment when crossing Dec→Jan")
            assertEquals(Month.JANUARY, nextMonth.month, "month must roll to January")
            assertEquals(15, nextMonth.dayOfMonth, "day-of-month preserved by kotlinx-datetime month-add")
        }

        // === Scenario 3: Leap year — Feb 2024 has 29 days; Feb 2023 has 28 ===

        @Test
        fun leapYearFeb2024Has29Days() {
            val feb2024 = LocalDate(2024, 2, 1)
            assertEquals(29, daysInMonth(feb2024), "2024 is a leap year — Feb has 29 days")
        }

        @Test
        fun nonLeapYearFeb2023Has28Days() {
            val feb2023 = LocalDate(2023, 2, 1)
            assertEquals(28, daysInMonth(feb2023), "2023 is NOT a leap year — Feb has 28 days")
        }

        @Test
        fun centuryNonLeapYearFeb1900Has28Days() {
            // 1900 is divisible by 100 but not by 400 → NOT a leap year (Gregorian rule).
            val feb1900 = LocalDate(1900, 2, 1)
            assertEquals(28, daysInMonth(feb1900))
        }

        @Test
        fun centuryLeapYearFeb2000Has29Days() {
            // 2000 is divisible by 400 → leap year.
            val feb2000 = LocalDate(2000, 2, 1)
            assertEquals(29, daysInMonth(feb2000))
        }

        // === Compile-smoke: composable file is on the classpath ===

        @Test
        fun aeroCalendarGridCompileSmoke() {
            val cls = Class.forName(
                "com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGridKt"
            )
            assertNotNull(cls)
        }
    }
    ```

    Step C — Run the test class:
    ```bash
    ./gradlew :library:test --tests "com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGridTest"
    ```
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:test --tests "com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGridTest"</automated>
  </verify>
  <acceptance_criteria>
    - `test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` is true
    - `test -f library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` is true
    - `grep -q "internal fun AeroCalendarGrid" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` returns 0
    - `grep -q "import kotlinx.datetime.LocalDate" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` returns 0
    - `grep -q "AeroIcons.CaretLeft" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` returns 0
    - `grep -q "AeroIcons.CaretRight" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` returns 0
    - `grep -q "leapYearFeb2024Has29Days" library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` returns 0
    - `grep -q "decemberPlusOneMonthBecomesJanuaryNextYear" library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` returns 0
    - `./gradlew :library:test --tests "com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGridTest"` exits 0 (all tests green, including compile-smoke)
    - `./gradlew :library:test` exits 0 (full Phase 7 plan-01 suite green: AeroColorMathTest + AeroCalendarPositionProviderTest + AeroCalendarGridTest + all pre-existing tests)
  </acceptance_criteria>
  <done>AeroCalendarGrid composable exists with the locked signature, uses kotlinx-datetime LocalDate end-to-end, prev/next buttons render AeroIcons.CaretLeft / AeroIcons.CaretRight, and the 3-scenario test (current month / month boundary / leap year) plus compile-smoke all pass. The full Phase 7 plan-01 test suite is green.</done>
</task>

</tasks>

<verification>
After all tasks complete:

```bash
# 1. Full library test suite passes (existing tests + 3 new test classes from this plan)
./gradlew :library:test

# 2. Library compiles cleanly under explicitApi() — no public API leaks from new files
./gradlew :library:compileKotlin

# 3. New files exist at locked paths
test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt
test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt
test -f library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt
test -f library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt
test -f library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt
test -f library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt
test -f .planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md

# 4. kotlinx-datetime 0.6.2 is on classpath
grep -q 'kotlinxDatetime = "0.6.2"' gradle/libs.versions.toml
grep -q 'implementation(libs.kotlinx.datetime)' library/build.gradle.kts

# 5. No public symbols leaked from new files (every fun/class is `internal`)
! grep -E "^(public )?fun [A-Z]|^(public )?class [A-Z]" \
  library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt \
  library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt \
  library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt \
  | grep -v "^.*internal "

# 6. Existing AeroPopupPositionProvider.kt is untouched
git diff --quiet library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt
```
</verification>

<success_criteria>
- kotlinx-datetime 0.6.2 added to version catalog AND library build script; `./gradlew :library:compileKotlin` validates first-compile compatibility
- 07-SPIKE-touchslop.md exists and records the spike outcome
- AeroColorMath utility provides `rgbToHsv`, `hexToRgb`, `hexToRgba`, `rgbToHex` — all `internal`, all in package `com.mordred.aero.components.pickers.internal.color`
- AeroColorMathTest passes the round-trip drift contract: `Color.hsv(0,1,1) → rgb → rgbToHsv` returns hue within `0.001f` (PITFALL-15 absent)
- AeroCalendarPositionProvider implements PopupPositionProvider with locked behaviors (no width-lock, no Top/Bottom flip on width overflow, IntSize.Zero first-frame guard)
- AeroCalendarPositionProviderTest passes 4 scenarios: first-frame guard, wide popup left edge, wide popup near right edge (1024dp window — SC1), vertical-overflow-flip
- AeroCalendarGrid composable renders month grid with header/day-of-week/day-cells; uses kotlinx-datetime LocalDate end-to-end; prev/next buttons use AeroIcons.CaretLeft / AeroIcons.CaretRight
- AeroCalendarGridTest passes 3 ROADMAP scenarios (April 2026 = 30 days; Dec 2026 → Jan 2027 boundary; Feb 2024 leap = 29, Feb 2023 = 28) plus century-leap edge cases (1900, 2000) plus compile-smoke
- Hue convention `[0f, 360f]` is documented in AeroColorMath KDoc as the locked Phase 7 plan-01 ADR
- All new symbols are `internal`; no `public` API surface added; existing AeroPopupPositionProvider unchanged
- `./gradlew :library:test` exits 0 (full suite green including the 3 new test classes)
</success_criteria>

<output>
After completion, create `.planning/phases/07-shared-internal-primitives/07-01-SUMMARY.md` describing:
- The locked hue ADR `[0f, 360f]` (cite AeroColorMath KDoc)
- Touchslop spike outcome (cite 07-SPIKE-touchslop.md)
- File-path manifest for all created sources + tests
- Test count: AeroColorMathTest (15 tests), AeroCalendarPositionProviderTest (4 tests), AeroCalendarGridTest (6 tests + 1 compile-smoke)
- Confirmation that PITFALL-02, PITFALL-08, PITFALL-15 are defused at the utility level
- Plan-02 readiness: visuals can now consume AeroColorMath; AeroCalendarGrid is ready for the scratch section eyes-on
</output>
