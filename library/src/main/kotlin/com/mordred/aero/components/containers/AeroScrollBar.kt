package com.mordred.aero.components.containers

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * CNT-06: Standalone Aero-styled vertical scrollbar.
 *
 * Wraps Compose Foundation's `VerticalScrollbar` so it gains theme-driven styling
 * via `LocalScrollbarStyle` (installed by `AeroTheme`). Width, hover behavior,
 * thumb colors, and OS fling are inherited from Foundation — only visual tokens
 * differ from the default style.
 *
 * Pair with [AeroScrollArea] for a complete scrollable container, or attach manually
 * to an existing `ScrollState`.
 *
 * @param scrollState the [ScrollState] driving the bar; usually the same one passed
 *   to a sibling `Modifier.verticalScroll(...)`.
 * @param modifier optional layout modifier (typically `Modifier.align(Alignment.CenterEnd).fillMaxHeight()`).
 */
@Composable
public fun AeroScrollBar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier
    )
}

/**
 * Standalone Aero-styled vertical scrollbar bound to a [LazyListState].
 *
 * Use this overload with your own `LazyColumn` / `LazyRow` — for example inside
 * [AeroDataTable] and [AeroTreeView], which own their `LazyColumn` directly and must
 * NOT be wrapped in [AeroScrollArea] (that would destroy virtualization — PITFALL-01).
 *
 * @param lazyListState the [LazyListState] passed to the sibling `LazyColumn(state = ...)`.
 * @param modifier typically `Modifier.align(Alignment.CenterEnd).fillMaxHeight()`.
 */
@Composable
public fun AeroScrollBar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(lazyListState),
        modifier = modifier
    )
}
