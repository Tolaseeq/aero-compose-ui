package com.mordred.aero.components.containers

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * CNT-05: Scrollable area with an Aero-styled vertical scrollbar that appears only
 * when content overflows. Vertical-only in v1 (horizontal deferred to v2).
 *
 * @param modifier outer layout modifier.
 * @param state optional `ScrollState`; defaults to `rememberScrollState()`.
 * @param content scrollable content; placed inside a `Column` so children stack vertically.
 */
@Composable
public fun AeroScrollArea(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state),
            content = content
        )
        if (state.maxValue > 0) {
            AeroScrollBar(
                scrollState = state,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }
}
