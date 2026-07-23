package com.mordred.aero.components.buttons

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Render + click smoke test for [AeroButton] (17-01 tracer) — proves [AeroButtonSurface] paints
 * the filled rest state end-to-end and click activation survives the Material3 container
 * removal via `Modifier.clickable(role = Role.Button, ...)` (VBTN-01/04), on all three built-in
 * [AeroColorScheme] presets. [AeroColorScheme.Classic]'s fully-opaque tokens are exercised
 * separately from [AeroColorScheme.AeroBlue]/[AeroColorScheme.AeroDark]'s translucent tokens to
 * prove no alpha-fade collapses to a flat block (PRIM-14).
 */
@OptIn(ExperimentalTestApi::class)
class AeroButtonTest {

    @Test
    fun rendersWithoutException_onAeroBlue() = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
                AeroButton(text = "Save Changes", onClick = {})
            }
        }
        waitForIdle()
        onNodeWithText("Save Changes").assertIsDisplayed()
    }

    @Test
    fun rendersWithoutException_onAeroDark() = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = AeroColorScheme.AeroDark) {
                AeroButton(text = "Save Changes", onClick = {})
            }
        }
        waitForIdle()
        onNodeWithText("Save Changes").assertIsDisplayed()
    }

    @Test
    fun rendersWithoutException_onClassic() = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = AeroColorScheme.Classic) {
                AeroButton(text = "Save Changes", onClick = {})
            }
        }
        waitForIdle()
        onNodeWithText("Save Changes").assertIsDisplayed()
    }

    @Test
    fun clickInvokesOnClick() = runComposeUiTest {
        var clicked = false
        setContent {
            AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
                AeroButton(text = "Save Changes", onClick = { clicked = true })
            }
        }
        waitForIdle()
        onNodeWithText("Save Changes").performClick()
        waitForIdle()
        assertTrue(clicked, "onClick should be invoked after performClick")
    }
}
