package com.mordred.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.overlay.AeroToastHost
import com.mordred.aero.components.overlay.AeroToastHostState
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import com.mordred.showcase.sections.ButtonsSection
import com.mordred.showcase.sections.ContainersSection
import com.mordred.showcase.sections.DropdownSection
import com.mordred.showcase.sections.FoundationSection
import com.mordred.showcase.sections.IconsSection
import com.mordred.showcase.sections.InputSection
import com.mordred.showcase.sections.ListSection
import com.mordred.showcase.sections.DataSection
import com.mordred.showcase.sections.LayoutSection
import com.mordred.showcase.sections.NavigationSection
import com.mordred.showcase.sections.OverlaysSection
import com.mordred.showcase.sections.PickersSection
import com.mordred.showcase.sections.PrimitivesSection
import com.mordred.showcase.sections.RangeSection
import com.mordred.showcase.sections.SelectionSection
import com.mordred.showcase.sections.ThemeSwitcher
import com.mordred.showcase.sections.VerificationSection
import kotlin.math.ceil
import kotlin.math.min
import kotlinx.coroutines.flow.first

/**
 * Canonical, addressable section names, in the order they appear on the page. Backs the
 * `-Daero.section=<Name>` launch parameter (see [ShowcaseApp]'s `section` parameter).
 */
internal val SHOWCASE_SECTIONS: List<String> = listOf(
    "ThemeSwitcher",
    "Verification",
    "Foundation",
    "Primitives",
    "Icons",
    "Buttons",
    "Input",
    "Selection",
    "Dropdown",
    "Range",
    "List",
    "Containers",
    "Overlays",
    "Navigation",
    "Data",
    "Pickers",
    "Layout"
)

/**
 * ShowcaseApp now accepts the active color scheme from its caller (Main.kt) so that
 * the AeroTitleBar in Main.kt shares the same theme. AeroToastHost is mounted at
 * the root of this composable's Box and overlays the scrolling content.
 *
 * NOTE: ContainersSection / OverlaysSection / NavigationSection calls are added by
 * Task 4 of Plan 03-08 once the section files exist. After Task 1 of Plan 03-08
 * runs alone, the showcase shows only the existing Phase 1+2 sections.
 *
 * [section] and [page] drive a capture pass: when [section] names one of
 * [SHOWCASE_SECTIONS], only that section renders and the page scrolls to the requested
 * viewport-sized page, then an `AERO_READY` line is printed once the frame has settled. An
 * unrecognised [section] falls back to rendering the full page and prints
 * `AERO_SECTION_UNKNOWN` once. Absent [section] (the default) renders every section exactly
 * as before, with no reporter line.
 */
@Composable
fun ShowcaseApp(
    currentScheme: AeroColorScheme,
    onSchemeChange: (AeroColorScheme) -> Unit,
    section: String? = null,
    page: Int = 0
) {
    // Note: we do NOT wrap in AeroTheme {} here — that wrapping happens in Main.kt
    // so the title bar participates in the same theme.
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val toastState = remember { AeroToastHostState() }
    val scrollState = rememberScrollState()
    val active = section?.takeIf { it in SHOWCASE_SECTIONS }

    if (section != null && active == null) {
        LaunchedEffect(section) {
            println("AERO_SECTION_UNKNOWN name=$section known=${SHOWCASE_SECTIONS.joinToString(",")}")
            System.out.flush()
        }
    }

    fun shows(name: String) = active == null || active == name

    if (active != null) {
        LaunchedEffect(active, page) {
            snapshotFlow { scrollState.viewportSize }.first { it > 0 }
            val maxValue = scrollState.maxValue
            val viewport = scrollState.viewportSize
            val pages = if (maxValue == 0) 1 else ceil((maxValue + viewport) / viewport.toDouble()).toInt()
            val clampedPage = min(page, pages - 1)
            val target = min(clampedPage * viewport, maxValue)
            scrollState.scrollTo(target)
            withFrameNanos { }
            withFrameNanos { }
            val schemeLabel = when (currentScheme) {
                AeroColorScheme.AeroDark -> "AeroDark"
                AeroColorScheme.Classic -> "Classic"
                else -> "AeroBlue"
            }
            val backgroundArgb = "%08X".format(colors.background.toArgb())
            val jvmVendor = System.getProperty("java.vendor")?.replace(' ', '_')
            val jvm = "${System.getProperty("java.version")}/$jvmVendor"
            println(
                "AERO_READY scheme=$schemeLabel section=$active page=$clampedPage pages=$pages " +
                    "viewportPx=$viewport contentPx=${maxValue + viewport} scrollPx=$target " +
                    "background=$backgroundArgb jvm=$jvm"
            )
            System.out.flush()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // NOTE (20-06/SHW-16): AeroTheme itself now paints a fillMaxSize Surface(colors.background)
        // around its content (Main.kt wraps this whole app in AeroTheme {}), so this Surface call
        // is now redundant with the identical color underneath it - kept as-is rather than removed:
        // ShowcaseApp is reachable from places that may not always sit directly under an
        // AeroTheme-owned background Surface, and a same-color redundant paint changes nothing
        // visible (verified: no elevation/shadow/shape difference between the two Surface calls).
        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(48.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                if (shows("ThemeSwitcher")) {
                    ThemeSwitcher(
                        current = currentScheme,
                        onSelect = onSchemeChange
                    )
                }

                if (shows("Verification")) {
                    VerificationSection()
                }

                if (shows("Foundation")) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Foundation",
                            color = colors.onBackground,
                            style = typography.title
                        )
                        FoundationSection()
                    }
                }

                if (shows("Primitives")) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Primitives",
                            color = colors.onBackground,
                            style = typography.title
                        )
                        PrimitivesSection()
                    }
                }

                if (shows("Icons")) {
                    IconsSection(toastState = toastState)
                }

                if (shows("Buttons")) {
                    ButtonsSection()
                }
                if (shows("Input")) {
                    InputSection()
                }
                if (shows("Selection")) {
                    SelectionSection()
                }
                if (shows("Dropdown")) {
                    DropdownSection()
                }
                if (shows("Range")) {
                    RangeSection()
                }
                if (shows("List")) {
                    ListSection()
                }

                if (shows("Containers")) {
                    ContainersSection()
                }
                if (shows("Overlays")) {
                    OverlaysSection(toastState = toastState)
                }
                if (shows("Navigation")) {
                    NavigationSection()
                }

                if (shows("Data")) {
                    DataSection()
                }
                if (shows("Pickers")) {
                    PickersSection()
                }
                if (shows("Layout")) {
                    LayoutSection()
                }

                Spacer(Modifier.height(24.dp))
            }
        }
        AeroToastHost(
            state = toastState,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
