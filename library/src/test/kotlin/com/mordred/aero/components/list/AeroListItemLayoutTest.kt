package com.mordred.aero.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * G1/VLST-01/VLST-03 measured-height regression tests (19-06).
 *
 * These prove the row-growth contract directly by measuring composed height, rather than by
 * reading source text — the class of defect this closes (a fixed [androidx.compose.foundation.layout.height]
 * silently clipping a two-line row) is invisible to any value-level resolver test, per this
 * plan's own `<planner_finding>`. Every case below was written BEFORE `AeroListItem.kt` was
 * changed to grow with its content (TDD RED), then confirmed to pass after (TDD GREEN) — see
 * 19-06-SUMMARY.md.
 */
@OptIn(ExperimentalTestApi::class)
class AeroListItemLayoutTest {

    @Test
    fun singleLineRowMeasuresExactly36Dp() = runComposeUiTest {
        setContent {
            AeroTheme {
                AeroListItem(
                    text = "Inbox",
                    modifier = Modifier.testTag("row"),
                )
            }
        }
        waitForIdle()

        onNodeWithTag("row").assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun rowWithPrimaryPlusSecondaryTextGrowsPast36Dp() = runComposeUiTest {
        setContent {
            AeroTheme {
                AeroListItem(
                    text = "Sent",
                    secondaryText = "Yesterday at 4:32 PM — your message was delivered",
                    modifier = Modifier.testTag("row"),
                )
            }
        }
        waitForIdle()

        onNodeWithTag("row").assertHeightIsAtLeast(37.dp)
    }

    @Test
    fun emptyPrimaryTextRowStillMeasuresAtLeast36Dp() = runComposeUiTest {
        setContent {
            AeroTheme {
                AeroListItem(
                    text = "",
                    modifier = Modifier.testTag("row"),
                )
            }
        }
        waitForIdle()

        onNodeWithTag("row").assertHeightIsAtLeast(36.dp)
    }

    @Test
    fun wrappedLongPrimaryLabelWithSecondaryTextGrowsPast36Dp() = runComposeUiTest {
        setContent {
            AeroTheme {
                Column(Modifier.width(160.dp)) {
                    AeroListItem(
                        text = "A deliberately long primary label that must wrap onto a second " +
                            "line once the row is narrow enough to force it",
                        secondaryText = "Secondary line",
                        modifier = Modifier.testTag("row"),
                    )
                }
            }
        }
        waitForIdle()

        onNodeWithTag("row").assertHeightIsAtLeast(37.dp)
    }

    @Test
    fun adjacentSelectedSingleLineRowsEachMeasure36DpAndDoNotOverlap() = runComposeUiTest {
        setContent {
            AeroTheme {
                Column {
                    AeroListItem(
                        text = "Row 1",
                        selected = true,
                        modifier = Modifier.testTag("row1"),
                    )
                    AeroListItem(
                        text = "Row 2",
                        selected = true,
                        modifier = Modifier.testTag("row2"),
                    )
                }
            }
        }
        waitForIdle()

        onNodeWithTag("row1").assertHeightIsEqualTo(36.dp)
        onNodeWithTag("row2").assertHeightIsEqualTo(36.dp)

        val bounds1 = onNodeWithTag("row1").fetchSemanticsNode().boundsInRoot
        val bounds2 = onNodeWithTag("row2").fetchSemanticsNode().boundsInRoot
        assertTrue(
            bounds2.top >= bounds1.bottom,
            "adjacent selected rows must not overlap: row1 bottom=${bounds1.bottom}, row2 top=${bounds2.top}"
        )
    }
}
