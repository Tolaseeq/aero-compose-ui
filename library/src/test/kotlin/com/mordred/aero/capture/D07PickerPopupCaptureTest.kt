package com.mordred.aero.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.pickers.AeroDatePicker
import com.mordred.aero.components.pickers.AeroDateRangePicker
import com.mordred.aero.components.pickers.AeroDateTimePicker
import com.mordred.aero.components.pickers.AeroDateTimeRangePicker
import com.mordred.aero.components.pickers.AeroTimePicker
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * D-07: opened-state UI-test coverage for the 5 picker components whose source contains
 * `Popup(` — [AeroDatePicker], [AeroDateRangePicker], [AeroDateTimePicker],
 * [AeroDateTimeRangePicker], [AeroTimePicker] — three themes, captured on the pre-upgrade
 * toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1). Reuses `captureOpened()` and
 * `assertOpenedDiffers()` from `UiCapture.kt` (the method `D07MenuPopupCaptureTest`'s AeroDropdown
 * probe decided).
 *
 * **D-09 determinism.** Every test below passes a fixed, non-null `value`/`startValue`+`endValue`
 * so the calendar opens on that month rather than on `todayLocalDate()` (`Clock.System.now()`),
 * per D-09 (confirmed in `D07MenuPopupCaptureTest`'s Task 1: `AeroCalendarGrid` has no
 * today-highlight). All constructors are POSITIONAL (`LocalDate(2026, 3, 14)`,
 * `LocalDateTime(2026, 3, 14, 10, 30)`, `LocalTime(10, 30)`) — never named — and this file never
 * reads `.dayOfMonth` / `.monthNumber` / `Clock`, so it compiles unchanged through the
 * kotlinx-datetime 0.6.2 -> 0.8.0 bump (those members were renamed/removed on `Instant`/`Clock`,
 * not on the positional constructors used here).
 *
 * **Not composable in unit tests (D-07/D-08, recorded in `21-UITEST-COVERAGE.md`):**
 * [com.mordred.aero.components.overlay.AeroDialog] / [com.mordred.aero.components.overlay.AeroAlertDialog]
 * build a real `Window` (`AeroDialog.kt:53`) and [com.mordred.aero.components.input.AeroFilePicker]
 * opens a native `java.awt.FileDialog` — composing either in `runComposeUiTest` would open a real
 * OS window on the maintainer's desktop, so none of the three is ever composed here.
 */
@OptIn(ExperimentalTestApi::class)
class D07PickerPopupCaptureTest {

    // ---------------------------------------------------------------------------------------
    // AeroDatePicker — fixed value 2026-03-14; popup-only marker: the month header "March 2026".
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroDatePickerAeroBlueOpened() = runDatePickerOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroDatePickerAeroDarkOpened() = runDatePickerOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroDatePickerClassicOpened() = runDatePickerOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runDatePickerOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(720.dp, 560.dp).padding(16.dp)) {
                    AeroDatePicker(
                        value = LocalDate(2026, 3, 14),
                        onValueChange = {},
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithContentDescription("Open calendar").performClick()
        waitForIdle()

        onNodeWithText("March 2026").assertExists(
            "D-07: AeroDatePicker/$theme must show its \"March 2026\" month header after a real " +
                "click on the calendar trigger"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroDatePicker", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroDateRangePicker — fixed range 2026-03-10..2026-03-20; popup-only marker: the right
    // calendar's month header "April 2026" (leftMonth = startValue's month, right = left + 1).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroDateRangePickerAeroBlueOpened() =
        runDateRangePickerOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroDateRangePickerAeroDarkOpened() =
        runDateRangePickerOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroDateRangePickerClassicOpened() =
        runDateRangePickerOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runDateRangePickerOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(720.dp, 560.dp).padding(16.dp)) {
                    AeroDateRangePicker(
                        startValue = LocalDate(2026, 3, 10),
                        endValue = LocalDate(2026, 3, 20),
                        onRangeSelect = { _, _ -> },
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithContentDescription("Open range calendar").performClick()
        waitForIdle()

        onNodeWithText("April 2026").assertExists(
            "D-07: AeroDateRangePicker/$theme must show its right calendar's \"April 2026\" month " +
                "header after a real click on the range-calendar trigger"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroDateRangePicker", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroDateTimePicker — fixed value 2026-03-14T10:30; popup-only marker: the "Apply" button.
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroDateTimePickerAeroBlueOpened() =
        runDateTimePickerOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroDateTimePickerAeroDarkOpened() =
        runDateTimePickerOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroDateTimePickerClassicOpened() =
        runDateTimePickerOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runDateTimePickerOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(720.dp, 560.dp).padding(16.dp)) {
                    AeroDateTimePicker(
                        value = LocalDateTime(2026, 3, 14, 10, 30),
                        onValueChange = {},
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithContentDescription("Open date & time picker").performClick()
        waitForIdle()

        onNodeWithText("Apply").assertExists(
            "D-07: AeroDateTimePicker/$theme must show its \"Apply\" button after a real click on " +
                "the trigger"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroDateTimePicker", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroDateTimeRangePicker — fixed range 2026-03-10T09:00..2026-03-20T17:30; popup-only
    // marker: the "Apply" button (same commit-gate shape as AeroDateTimePicker).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroDateTimeRangePickerAeroBlueOpened() =
        runDateTimeRangePickerOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroDateTimeRangePickerAeroDarkOpened() =
        runDateTimeRangePickerOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroDateTimeRangePickerClassicOpened() =
        runDateTimeRangePickerOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runDateTimeRangePickerOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(720.dp, 560.dp).padding(16.dp)) {
                    AeroDateTimeRangePicker(
                        startValue = LocalDateTime(2026, 3, 10, 9, 0),
                        endValue = LocalDateTime(2026, 3, 20, 17, 30),
                        onRangeSelect = { _, _ -> },
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithContentDescription("Open date & time range picker").performClick()
        waitForIdle()

        onNodeWithText("Apply").assertExists(
            "D-07: AeroDateTimeRangePicker/$theme must show its \"Apply\" button after a real " +
                "click on the trigger"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroDateTimeRangePicker", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroTimePicker — fixed value 10:30; popup-only marker: the minute spinner's "30" field
    // (the trigger's own read-only text is the single node "10:30", never "30" alone).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroTimePickerAeroBlueOpened() = runTimePickerOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroTimePickerAeroDarkOpened() = runTimePickerOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroTimePickerClassicOpened() = runTimePickerOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runTimePickerOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(720.dp, 560.dp).padding(16.dp)) {
                    AeroTimePicker(
                        value = LocalTime(10, 30),
                        onValueChange = {},
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithContentDescription("Open time picker").performClick()
        waitForIdle()

        onNodeWithText("30").assertExists(
            "D-07: AeroTimePicker/$theme must show its minute spinner's \"30\" field after a real " +
                "click on the trigger"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroTimePicker", theme, closed, opened)
    }
}
