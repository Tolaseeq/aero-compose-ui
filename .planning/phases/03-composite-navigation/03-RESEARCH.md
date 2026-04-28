# Phase 3: Composite + Navigation - Research

**Researched:** 2026-04-28
**Domain:** Compose Desktop (CMP 1.7.3, Kotlin 2.1.21, JDK 17) — composite containers, modal/overlay surfaces, popups, custom window chrome, scrollbar theming, showcase wiring
**Confidence:** HIGH (decisions are locked in CONTEXT.md; this research validates them and surfaces precise APIs)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Window chrome deployment (NAV-01 AeroTitleBar)
- **showcase Main.kt switches to `Window(undecorated = true)`** — demonstrates the real Aero window, not a visual mock-up. This validates that the Win11 EXCEPTION_ACCESS_VIOLATION blocker only triggers with `transparent = true`; using `undecorated` alone is safe.
- **AeroTitleBar lives inside `FrameWindowScope`** — the composable signature is `fun FrameWindowScope.AeroTitleBar(...)` so it can call `WindowDraggableArea` and access `WindowState`. Direct port of `mordred/AeroTitleBar.kt`.
- **WindowDraggableArea wraps the entire title-bar Row** — same pattern as mordred. The Min/Max/Close buttons have their own `clickable {}` handlers, so click wins over drag automatically.
- **API signature includes `leading: @Composable (() -> Unit)? = null`** — optional slot for an app icon before the title text. Min/Max/Close are fixed (built-in to component, not slot-overrideable in v1).
- **Custom resize handles inside the window** — invisible 4-6dp pointer-input zones along the four edges and four corners; on drag they update `windowState.size`. Cursor changes to native resize cursor on hover. When `windowState.placement = Maximized`, resize handles are disabled.
- **Aero Snap (HTCAPTION) is NOT implemented** — `WindowDraggableArea` does not pass HTCAPTION to the OS, so native snap-to-edge / snap-to-half / snap-to-quarter behaviors are unavailable. Documented in `AeroTitleBar` KDoc + `PROJECT.md` Out-of-Scope.

#### Dialog & overlay infrastructure (OVL-01/02/04/07/08, AeroDrawer)
- **AeroDialog and AeroAlertDialog use `androidx.compose.ui.window.DialogWindow`** — real OS-level child window. Inside the DialogWindow, the chrome itself uses `undecorated = true` (NO `transparent = true` — Win11 rule still applies).
- **AeroDialog API has 3 slots**: `AeroDialog(onDismissRequest, title: @Composable () -> Unit, content: @Composable () -> Unit, buttons: @Composable RowScope.() -> Unit = {})`. `buttons` is a `RowScope` lambda so callers naturally use `Arrangement.End` and place 1+ AeroButton/AeroOutlinedButton children.
- **AeroAlertDialog has 4 variants** via `enum class AeroAlertKind { Info, Warning, Error, Question }`. Each kind picks an icon (Material `Icons.Outlined.Info / Warning / Error / HelpOutline`) and accent color. Default buttons match kind: Info/Warning/Error → single OK; Question → OK + Cancel. Caller can override button text via parameters.
- **Scrim color is `Color.Black.copy(alpha = 0.5f)`** for AeroDrawer and any in-window overlays that need one. AeroDialog/AeroAlertDialog don't paint a scrim themselves — DialogWindow handles modality at the OS level. **No new token added to AeroColorScheme.**
- **`AeroPopupPositionProvider` becomes public and is extended with a `side` parameter**: `class AeroPopupPositionProvider(side: AeroPopupSide = AeroPopupSide.Bottom, gap: Int = 4)`, where `AeroPopupSide = { Top, Bottom, Start, End }`. Auto-flips to opposite side on overflow. Moves out of `dropdown/AeroDropdownMenu.kt` into `library/.../popup/AeroPopupPositionProvider.kt`. AeroDropdown keeps its current behavior (Bottom is the default).
- **AeroContextMenu uses a separate `AeroCursorPositionProvider`** — positions the popup at the right-click cursor coordinates (clamped to window bounds).
- **AeroTooltip behavior: anchor-fixed, 600ms in-delay, 0ms out-delay**. Uses `AeroPopupPositionProvider(side = Top)` with auto-flip. Tooltip does NOT follow the cursor. Public API: `AeroTooltip(text: String, content: @Composable () -> Unit)` plus convenience `Modifier.aeroTooltip("text")` extension.
- **AeroContextMenu API: `Modifier.aeroContextMenu(items: List<AeroContextMenuItem>)`**. The modifier handles right-click detection, captures cursor position, and shows the popup. `AeroContextMenuItem` is a `sealed class`: `Action(label, icon?, shortcut?, onClick)`, `Divider`, `Submenu(label, items)`. Submenus supported recursively (1-2 levels expected).
- **AeroDrawer: modal with scrim, side parameter `AeroDrawerSide { Start, End }`**. Slide-in via `animateFloatAsState` (offset.x), 200-250ms tween. Dismiss = scrim click + Esc. Drawer width caller-controlled. Top/Bottom variants deferred.

#### AeroScrollBar / AeroScrollArea (CNT-05/06)
- **Wrap Compose Foundation's standard scrollbar API** — reuse `androidx.compose.foundation.VerticalScrollbar`, `HorizontalScrollbar`, and `rememberScrollbarAdapter`. We override visual via `ScrollbarStyle` / `LocalScrollbarStyle`, getting drag-thumb, click-track-to-page, OS fling integration, and accessibility for free.
- **Width = 12dp** for both vertical (width) and horizontal (height).
- **Always visible** — Windows-style (no auto-hide).
- **AeroTheme installs `LocalScrollbarStyle`** with Aero tokens — every `VerticalScrollbar` call inside the theme automatically picks up Aero styling.
- **Retrofit existing Phase 2 components**: `AeroDropdownPopup` and `AeroTextArea` migrate to use `AeroScrollArea` (or explicitly attach an `AeroScrollBar`). Phase 2 code is edited as part of Phase 3.
- **AeroScrollArea API**: takes content slot, manages its own `ScrollState` by default, optionally accepts an external `state` for caller control. Vertical-only in v1.

#### Toast / Banner notification hosting (OVL-05/06)
- **AeroToast/AeroSnackbar position: Bottom-end**, fixed 16dp from window edges. Single `AeroToastHost(state)` placed once at the root of `ShowcaseApp`.
- **API matches Material3 SnackbarHostState pattern**: `class AeroToastHostState { suspend fun showToast(message: String, duration: AeroToastDuration = Short, action: AeroToastAction? = null): AeroToastResult }`.
- **Queue: stack** — multiple toasts visible simultaneously (max 3-5 stacked). When the stack is full, oldest auto-dismissed early. **NOTE: stacked toast lifecycle/animation is the trickiest part of this phase — needs a small spike during planning.**
- **AeroToastDuration**: `Short = 4_000ms`, `Long = 10_000ms`, `Indefinite = Long.MAX_VALUE`.
- **AeroNotificationBanner: in-flow component, NOT host-based**. Signature: `AeroNotificationBanner(kind: AeroBannerKind, text: String, onDismiss: (() -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null)`. `AeroBannerKind = { Info, Warning, Error, Success }`.

#### Carry-forward from Phase 1 / Phase 2
- All state animations: `animateFloatAsState` / `animateColorAsState` with `tween(150, easing = LinearEasing)`.
- Hover overlay = `buttonHover` token; Focus = border swap to `borderSelected`; Disabled = 40% alpha; Pressed = Claude's discretion per component.
- All public types in `:library` carry explicit `public` modifier (`explicitApi()`).
- Material3 wraps inside `AeroTheme {}` — components MAY delegate to M3 primitives.
- Each category gets its own `*Section.kt` in `showcase/sections/` (Phase 3 adds: `ContainersSection.kt`, `OverlaysSection.kt`, `NavigationSection.kt`). Row template = 140dp label column + variants Row.
- Win11 chrome rule: `undecorated = true` is allowed; `transparent = true` is FORBIDDEN. Applies to showcase Main.kt + every DialogWindow.
- 23-token `AeroColorScheme` is NOT extended — Phase 3 reuses existing tokens. New tokens require explicit deviation.

### Claude's Discretion
- **AeroCard**: padding default, optional `header` slot vs `content`-only, elevation parameter default
- **AeroPanel**: same shape choices as AeroCard but using `glassPanel` modifier (no border, no shadow)
- **AeroDivider**: thickness (1.dp standard), padding around (none vs 8dp), color (`borderDefault`)
- **AeroGroupBox**: label position (top-left inset on the border, mordred-style), inner padding default
- **AeroBreadcrumb**: separator character (`›` recommended), overflow handling (truncate-middle with ellipsis), `onItemClick: (index, item) -> Unit`
- **AeroTabBar**: visual style (browser-tab vs pill vs underline — mordred reference is browser-style); overflow handling; optional close button per tab
- **AeroMenuBar**: top-level interaction (click-to-open + click-other-item-switches; hover-reveal after one item already opened); submenu cascade via hover-with-delay; **Alt+letter mnemonics — DEFERRED if non-trivial (note as v2)**
- **AeroStatusBar**: API shape (`sections: List<AeroStatusSection>` data-driven vs slot-based)
- **AeroPopover**: trigger model (anchor-element-clickable vs imperative `expanded` state), optional arrow/caret pointing at anchor (recommend yes if cheap)
- **AeroDialog/AeroAlertDialog**: appearance animation (use minimal fade ≤150ms by default)
- **AeroToast** stacked animation: exact ease/timing for shift-up
- **AeroScrollArea**: whether to expose horizontal-scroll variant in v1 (deferable if non-trivial)
- **AeroIconButton role inside Phase 3**: AeroDialog close-X / AeroNotificationBanner close-X / AeroTitleBar Min/Max/Close — use AeroIconButton OR inline (mordred uses inline; both are fine)

### Deferred Ideas (OUT OF SCOPE)
- **Aero Snap via JNI/HTCAPTION** — would need native code, breaks platform-neutral JAR — explicitly v2+. Add to `PROJECT.md` Out-of-Scope.
- **Top/Bottom AeroDrawer variants** — v1 ships only `Start`/`End`.
- **Alt+letter mnemonics in AeroMenuBar** — v2 enhancement.
- **Horizontal AeroScrollArea variant** — Claude's discretion in v1; if not trivial, defer to v2.
- **AeroPopover arrow/caret pointing at anchor** — Claude's discretion; if cheap, include in v1, otherwise defer.
- **AeroDataTable, AeroTreeView, AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker, AeroColorPicker, AeroRangeSlider** — already declared v2 in REQUIREMENTS.md (CMPLX-01..08).
- **AeroAccordion, AeroSplitPane, AeroSidebar, AeroStepperWizard** — already declared v2 in REQUIREMENTS.md (ADVL-01..04).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| **CNT-01** | `AeroCard` — glass panel with shadow, rounded corners, custom content | Reuse `Modifier.glassEffect()` (already does shadow + gradient + border); slot-only API mirrors Phase 2 patterns |
| **CNT-02** | `AeroPanel` — large-section background using `glassPanel` | `Modifier.glassPanel(cornerRadius)` already implemented in `theme/GlassModifiers.kt` |
| **CNT-03** | `AeroDivider` — horizontal + vertical separator, `borderDefault` token | Plain `Box` + `background(borderDefault)` + 1.dp height/width; trivial |
| **CNT-04** | `AeroGroupBox` — labeled border around group | `Layout` composable to position the label inset on the top border (carve-out via 2-pass measure or simple Column with overlapping label Box) |
| **CNT-05** | `AeroScrollArea` — scrollable area with custom scrollbar | Wrap `Modifier.verticalScroll(scrollState)` + `VerticalScrollbar(rememberScrollbarAdapter(scrollState))` from `androidx.compose.foundation` |
| **CNT-06** | `AeroScrollBar` — standalone Aero-styled scrollbar | Thin wrapper over `VerticalScrollbar` / `HorizontalScrollbar`; styling via `ScrollbarStyle` / `LocalScrollbarStyle` |
| **OVL-01** | `AeroDialog` — modal window with title/content/buttons slots | `androidx.compose.ui.window.DialogWindow(onCloseRequest, state, undecorated = true, transparent = false)` + glass-styled inner Box |
| **OVL-02** | `AeroAlertDialog` — confirm/error dialog with icon + default buttons | Built on top of AeroDialog; `AeroAlertKind` enum drives icon/color/buttons mapping |
| **OVL-03** | `AeroTooltip` — hover with delay + glass background | Custom `Popup` + 600ms `LaunchedEffect(hoverInteraction)` delay; `AeroPopupPositionProvider(side = Top)` |
| **OVL-04** | `AeroContextMenu` — right-click menu with items + dividers | `Modifier.onPointerEvent(PointerEventType.Press)` filtering `event.buttons.isSecondaryPressed`; capture position; `Popup` with `AeroCursorPositionProvider` |
| **OVL-05** | `AeroToast` / `AeroSnackbar` — auto-dismiss notification | `AeroToastHostState` with `mutableStateListOf<AeroToastData>` + per-toast `LaunchedEffect` dismiss timer; `Box(align = BottomEnd)` host placement; `AnimatedVisibility(slideInVertically + fadeIn)` |
| **OVL-06** | `AeroNotificationBanner` — info/warning/error/success inline banner | In-flow Row with icon + text + close-X; `AeroBannerKind` enum drives icon/tint |
| **OVL-07** | `AeroPopover` — anchor-relative panel | `Popup` + `AeroPopupPositionProvider(side)`; `glassEffect` background |
| **OVL-08** | `AeroDrawer` — sliding side panel | Full-screen `Box`: scrim layer + animated `offset.x` panel; ESC + scrim-click dismiss |
| **NAV-01** | `AeroTitleBar` — Aero gradient + draggable + Min/Max/Close | `fun FrameWindowScope.AeroTitleBar(...)` + `WindowDraggableArea`; `windowState.placement` toggle; reuse mordred port |
| **NAV-02** | `AeroMenuBar` — top horizontal menu with submenus | Row of click-to-toggle items; each opens an `AeroDropdownPopup`-style popup; cross-item hover-switch when one is already open |
| **NAV-03** | `AeroStatusBar` — bottom row with text sections + indicators | `Row` with `glassPanel` background + slot-based content; data-driven `AeroStatusSection` is a viable alternative |
| **NAV-04** | `AeroBreadcrumb` — clickable navigation chain | `FlowRow` of clickable text segments separated by `›` glyph; `onItemClick(index, item)` callback |
| **NAV-05** | `AeroTabBar` — tab switcher with active/inactive | Row of clickable tabs; active = `borderSelected` underline + `glassSurface`; horizontal-scroll overflow |
</phase_requirements>

## Summary

Phase 3 is a **port-and-extend phase**, not an exploration phase. The Compose Desktop primitives needed for every component class are present in CMP 1.7.3 and well-documented: `DialogWindow`, `Popup`, `WindowDraggableArea`, `FrameWindowScope`, `WindowState`, `VerticalScrollbar`/`ScrollbarStyle`/`LocalScrollbarStyle`, `AnimatedVisibility`, `onPointerEvent`. The Phase 1-2 primitives (`glassEffect` / `glassPanel` / `glassSurface` modifiers, `AeroTheme`, 23-token `AeroColorScheme`, `AeroDropdownPopup` pattern, `AeroPopupPositionProvider`, `InteractionStates` helpers) cover every styling and state-collection need in Phase 3 — no new theme tokens are required.

The two non-trivial pieces are: (1) the **stacked toast lifecycle** for AeroToast — Material3's `SnackbarHostState` is single-toast-only, so Phase 3 implements its own `mutableStateListOf<AeroToastData>` + per-toast dismiss-timer pattern; (2) the **AeroMenuBar cross-item hover switch** — when one top-level menu is already open, hovering a sibling top-level item must auto-open that one without a click. Both are explicitly called out as Claude's discretion in CONTEXT.md and need a small spike during planning.

The Win11 `undecorated=true` + `transparent=true` crash is a **known, still-unfixed CMP issue** (CMP-3757 / GH#3171) — Phase 3 strictly uses `undecorated=true` alone, never `transparent=true`. This rule applies equally to the showcase main `Window` and every `DialogWindow` opened by `AeroDialog`/`AeroAlertDialog`.

**Primary recommendation:** Use Compose Desktop's standard primitives (`DialogWindow`, `Popup`, `WindowDraggableArea`, `VerticalScrollbar` + `LocalScrollbarStyle`) wrapped behind Aero-themed APIs. Don't hand-roll modal infrastructure, scrollbar adapters, or window draggable regions — the standard library already handles edge cases (focus trap, OS fling, drag throttling).

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.foundation` (CMP 1.7.3) | 1.7.3 | `VerticalScrollbar`, `HorizontalScrollbar`, `rememberScrollbarAdapter`, `ScrollbarStyle`, `LocalScrollbarStyle`, `defaultScrollbarStyle()`, `WindowDraggableArea`, `onPointerEvent`, `TooltipArea` (alternative) | Shipped with CMP, OS-integrated fling + drag, accessibility hooks free |
| `androidx.compose.ui.window` (CMP 1.7.3) | 1.7.3 | `DialogWindow`, `Popup`, `PopupPositionProvider`, `PopupProperties`, `Window`, `WindowState`, `WindowPlacement`, `FrameWindowScope`, `rememberWindowState`, `rememberDialogState` | Real OS-level windows for AeroDialog (true modality, free focus trap) and Popup for in-window overlays |
| `androidx.compose.animation` (CMP 1.7.3) | 1.7.3 | `AnimatedVisibility`, `slideInHorizontally`, `slideOutHorizontally`, `slideInVertically`, `fadeIn`, `fadeOut`, `animateFloatAsState`, `animateColorAsState`, `animateOffsetAsState` | Already in `library/build.gradle.kts`; drives drawer slide, toast appearance, dialog fade |
| `androidx.compose.material3` (CMP 1.7.3) | 1.7.3 | `Icons.Outlined.Info / Warning / Error / HelpOutline / CheckCircle`, `Text`, `Icon` | Bridged inside `AeroTheme`; reused for AeroAlertDialog kind icons + AeroNotificationBanner banner icons |
| `kotlinx.coroutines` | 1.10.2 | `LaunchedEffect`, `delay`, `CompletableDeferred` for `AeroToastHostState.showToast` suspend semantics | Already in dependencies |

### Supporting (already in :library)
| API | Source | Purpose |
|-----|--------|---------|
| `Modifier.glassEffect(cornerRadius, elevation)` | `theme/GlassModifiers.kt` | AeroCard background |
| `Modifier.glassPanel(cornerRadius)` | `theme/GlassModifiers.kt` | AeroPanel, AeroToolbar, AeroStatusBar background |
| `Modifier.glassSurface(cornerRadius)` | `theme/GlassModifiers.kt` | AeroDialog inner container, AeroPopover, AeroDrawer panel |
| `AeroDropdownPopup` (internal) | `components/dropdown/AeroDropdownMenu.kt` | Pattern reference for AeroMenuBar dropdown + AeroContextMenu |
| `AeroPopupPositionProvider` (internal → public in Phase 3) | `components/dropdown/AeroDropdownMenu.kt` → `components/popup/` | Shared position provider for AeroPopover, AeroTooltip, AeroMenuBar dropdowns |
| `AeroDropdownItem` (internal) | `components/dropdown/AeroDropdownMenu.kt` | Pattern for AeroMenuBar item, AeroContextMenu Action item |
| `rememberHoverState`, `rememberPressedState`, `rememberFocusState`, `animatedAlpha`, `ANIMATION_DURATION_MS = 150` | `components/buttons/InteractionStates.kt` | State collection — do NOT re-implement |
| `AeroIconButton` | `components/buttons/AeroIconButton.kt` | AeroTitleBar Min/Max/Close (option), AeroDialog close-X, AeroNotificationBanner close-X |
| `AeroButton`, `AeroOutlinedButton` | `components/buttons/` | AeroDialog `buttons` slot, AeroAlertDialog default OK/Cancel |
| `AeroListItem` | `components/list/AeroListItem.kt` | Pattern reference for AeroMenuBar dropdown items |

### Alternatives Considered
| Instead of | Could Use | Tradeoff | Decision |
|------------|-----------|----------|----------|
| Custom `DialogWindow` for AeroDialog | `androidx.compose.ui.window.Dialog` (in-window scrim Popup) | Dialog avoids spawning OS child window — simpler, no Win11 chrome rules. **But:** loses true modality + free focus trap + native Esc handling | Use `DialogWindow` per CONTEXT.md decision (true modality outweighs simplicity) |
| Custom Popup for AeroTooltip | `TooltipArea` from `androidx.compose.foundation` | TooltipArea is built-in with `delayMillis` + `TooltipPlacement`. **But:** requires `@OptIn(ExperimentalFoundationApi::class)`, anchors to cursor by default (we want anchor-fixed), and theming is via the wrapped composable's surface — less ergonomic for `AeroPopupPositionProvider` reuse | Use custom `Popup` + delayed `LaunchedEffect`; reuses our position provider, no opt-in API |
| Custom right-click for AeroContextMenu | `androidx.compose.foundation.ContextMenuArea` + `LocalContextMenuRepresentation` | Built-in `ContextMenuArea` handles right-click detection + cursor positioning. **But:** items are `ContextMenuItem(label, onClick)` only — no icon, no shortcut, no submenu support; `ContextMenuRepresentation` interface drives full custom rendering but its API is not designed for arbitrary anchors | Use `Modifier.aeroContextMenu(items)` extension wrapping `Modifier.onPointerEvent(PointerEventType.Press)` + `event.buttons.isSecondaryPressed` + custom `Popup`. Gives us icon/shortcut/submenu support cleanly |
| Hand-roll scrollbar | `VerticalScrollbar` + `LocalScrollbarStyle` | Built-in handles drag, click-track-page, fling, accessibility. Custom would re-derive thumb size from viewport/content ratios | Wrap `VerticalScrollbar`; provide `ScrollbarStyle` via `LocalScrollbarStyle` in `AeroTheme` |
| Material3 `BasicAlertDialog` for AeroAlertDialog | M3 dialog content composable | Simpler — just composable layout, no DialogWindow spawning. **But:** doesn't give true OS-level modality (it's an in-window popup) and the showcase loses the "modal child window" demo value | Use AeroDialog (DialogWindow-based) per CONTEXT.md — consistent with AeroDialog approach |

**Installation:** No new dependencies — all required APIs are in CMP 1.7.3 modules already declared in `library/build.gradle.kts`:
```kotlin
implementation(compose.desktop.common)
implementation(compose.material3)
implementation(compose.animation)
implementation(compose.foundation)
implementation(compose.runtime)
implementation(compose.ui)
implementation(libs.kotlinx.coroutines.core)
```

**Version verification:** CMP 1.7.3 is the project's locked version (per `gradle/libs.versions.toml`). All Phase 3 APIs were verified against CMP 1.7.3 documentation at `kotlinlang.org/docs/multiplatform/compose-desktop-*`. No version bump is required or recommended for Phase 3.

## Architecture Patterns

### Recommended Project Structure
```
library/src/main/kotlin/com/mordred/aero/
├── theme/                  # (Phase 1, extended in Phase 3)
│   └── AeroTheme.kt        # add LocalScrollbarStyle provision
├── components/
│   ├── buttons/            # (Phase 2, used by Phase 3)
│   ├── input/              # (Phase 2, AeroTextArea retrofitted)
│   ├── selection/, dropdown/, list/, range/  # (Phase 2)
│   ├── popup/              # NEW — shared popup infrastructure
│   │   ├── AeroPopupPositionProvider.kt   # public, side + flip
│   │   ├── AeroCursorPositionProvider.kt  # for AeroContextMenu
│   │   └── PopupSide.kt                   # enum
│   ├── containers/         # NEW — CNT-01..06
│   │   ├── AeroCard.kt
│   │   ├── AeroPanel.kt
│   │   ├── AeroDivider.kt
│   │   ├── AeroGroupBox.kt
│   │   ├── AeroScrollArea.kt
│   │   └── AeroScrollBar.kt
│   ├── overlay/            # NEW — OVL-01..08
│   │   ├── AeroDialog.kt
│   │   ├── AeroAlertDialog.kt
│   │   ├── AeroAlertKind.kt
│   │   ├── AeroTooltip.kt
│   │   ├── AeroContextMenu.kt
│   │   ├── AeroContextMenuItem.kt
│   │   ├── AeroToast.kt
│   │   ├── AeroToastHostState.kt
│   │   ├── AeroNotificationBanner.kt
│   │   ├── AeroBannerKind.kt
│   │   ├── AeroPopover.kt
│   │   └── AeroDrawer.kt
│   └── navigation/         # NEW — NAV-01..05
│       ├── AeroTitleBar.kt
│       ├── AeroMenuBar.kt
│       ├── AeroStatusBar.kt
│       ├── AeroBreadcrumb.kt
│       └── AeroTabBar.kt

showcase/src/main/kotlin/com/mordred/showcase/
├── Main.kt                 # MODIFIED — Window(undecorated = true) + FrameWindowScope.AeroTitleBar + resize handles
├── ShowcaseApp.kt          # MODIFIED — adds 3 new sections + AeroToastHost(state) at root
└── sections/
    ├── ContainersSection.kt    # NEW
    ├── OverlaysSection.kt      # NEW
    └── NavigationSection.kt    # NEW
```

### Pattern 1: Composable Receiver on Window Scope
**What:** Compose Desktop window-scope APIs (`WindowDraggableArea`, custom resize handles via `WindowState`) require the composable to be defined on `FrameWindowScope` (Window) or `DialogWindowScope` (DialogWindow).

**When to use:** AeroTitleBar (must be `FrameWindowScope.AeroTitleBar(...)`); resize-handle helpers in showcase Main.kt.

**Example:**
```kotlin
// Source: kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html
// + mordred AeroTitleBar reference

@Composable
public fun FrameWindowScope.AeroTitleBar(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AeroTheme.colors
    WindowDraggableArea(modifier = modifier) {  // <- pulled in by FrameWindowScope receiver
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.titleBarGradientStart, colors.titleBarGradientEnd)
                    )
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading?.invoke()
            Text(title, color = colors.titleBarText, modifier = Modifier.weight(1f))

            // Min / Max / Close — fixed buttons
            TitleBarButton(text = "—", hoverColor = colors.buttonHover) {
                windowState.isMinimized = true
            }
            TitleBarButton(
                text = if (windowState.placement == WindowPlacement.Maximized) "❐" else "□",
                hoverColor = colors.buttonHover
            ) {
                windowState.placement =
                    if (windowState.placement == WindowPlacement.Maximized) WindowPlacement.Floating
                    else WindowPlacement.Maximized
            }
            TitleBarButton(text = "✕", hoverColor = colors.closeButtonHover, onClick = onCloseRequest)
        }
    }
}
```

**KEY:** the click handlers on TitleBarButton must NOT consume the drag — use plain `clickable {}` so drag from drag-area still works on the rest of the row.

### Pattern 2: DialogWindow with Glass-Styled Inner Surface
**What:** `androidx.compose.ui.window.DialogWindow` opens an OS-level child window. With `undecorated = true`, we lose the OS chrome and draw our own.

**When to use:** AeroDialog, AeroAlertDialog.

**Example:**
```kotlin
// Source: kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html
// CRITICAL: undecorated=true ONLY; transparent=false (Win11 crash rule)

@Composable
public fun AeroDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = rememberDialogState(width = 480.dp, height = 320.dp),
        undecorated = true,
        transparent = false,  // <-- MUST be false on Win11
        resizable = false,
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                onDismissRequest(); true
            } else false
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .glassSurface(cornerRadius = 8.dp)  // glass background w/ border
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                title()
                Box(Modifier.weight(1f)) { content() }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    content = buttons
                )
            }
        }
    }
}
```

### Pattern 3: Anchor-Relative Popup with Side + Auto-Flip
**What:** `Popup` with a custom `PopupPositionProvider` lets us place arbitrary content next to a host element. CONTEXT.md requires extending the existing `AeroPopupPositionProvider` with a `side: AeroPopupSide` parameter.

**When to use:** AeroPopover, AeroTooltip, AeroMenuBar dropdown menus.

**Example:**
```kotlin
// Source: extends existing AeroDropdownMenu.kt internal AeroPopupPositionProvider

public enum class AeroPopupSide { Top, Bottom, Start, End }

public class AeroPopupPositionProvider(
    private val side: AeroPopupSide = AeroPopupSide.Bottom,
    private val gap: Int = 4
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Compute proposed (x, y) for primary side
        val proposed: IntOffset = when (side) {
            AeroPopupSide.Bottom -> IntOffset(anchorBounds.left, anchorBounds.bottom + gap)
            AeroPopupSide.Top    -> IntOffset(anchorBounds.left, anchorBounds.top - popupContentSize.height - gap)
            AeroPopupSide.Start  -> IntOffset(anchorBounds.left - popupContentSize.width - gap, anchorBounds.top)
            AeroPopupSide.End    -> IntOffset(anchorBounds.right + gap, anchorBounds.top)
        }
        // Auto-flip: if primary side overflows, flip to opposite
        val flipped = autoFlip(proposed, anchorBounds, windowSize, popupContentSize)
        // Clamp X/Y to window bounds
        return clamp(flipped, windowSize, popupContentSize)
    }
    // ... helper functions for autoFlip + clamp
}
```

### Pattern 4: Cursor-Position Popup (right-click context menu)
**What:** Right-click captures cursor coordinates and opens a `Popup` at that exact point.

**When to use:** AeroContextMenu.

**Example:**
```kotlin
// Source: kotlinlang.org/docs/multiplatform/compose-desktop-mouse-events.html

@OptIn(ExperimentalComposeUiApi::class)
public fun Modifier.aeroContextMenu(items: List<AeroContextMenuItem>): Modifier = composed {
    var expanded by remember { mutableStateOf(false) }
    var pos by remember { mutableStateOf(IntOffset.Zero) }

    val pointerModifier = Modifier.onPointerEvent(PointerEventType.Press) { event ->
        if (event.buttons.isSecondaryPressed) {
            val change = event.changes.firstOrNull() ?: return@onPointerEvent
            pos = IntOffset(change.position.x.roundToInt(), change.position.y.roundToInt())
            expanded = true
        }
    }

    if (expanded) {
        Popup(
            popupPositionProvider = remember(pos) { AeroCursorPositionProvider(pos) },
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
        ) {
            AeroContextMenuContent(items = items, onItemClick = { expanded = false })
        }
    }
    this.then(pointerModifier)
}
```

### Pattern 5: Toast Host + Stacked Lifecycle
**What:** A single `AeroToastHost(state)` mounted at root receives suspend calls from anywhere via `state.showToast(...)`. Each toast has its own dismiss timer; the stack uses position-derived offsets via `animateOffsetAsState`.

**When to use:** AeroToast / AeroSnackbar (OVL-05).

**Example:**
```kotlin
// Source: pattern derived from Material3 SnackbarHostState extended with mutableStateListOf

public data class AeroToastData(
    val id: Long,
    val message: String,
    val duration: AeroToastDuration,
    val action: AeroToastAction?,
    val onDismissed: (AeroToastResult) -> Unit
)

public class AeroToastHostState {
    internal val toasts: SnapshotStateList<AeroToastData> = mutableStateListOf()
    private var nextId = 0L

    public suspend fun showToast(
        message: String,
        duration: AeroToastDuration = AeroToastDuration.Short,
        action: AeroToastAction? = null
    ): AeroToastResult = suspendCancellableCoroutine { cont ->
        val data = AeroToastData(
            id = nextId++,
            message = message,
            duration = duration,
            action = action,
            onDismissed = { result -> cont.resume(result) }
        )
        // If stack is full (5+), evict oldest
        while (toasts.size >= 5) toasts.removeFirst().onDismissed(AeroToastResult.Evicted)
        toasts.add(data)
    }
}

@Composable
public fun AeroToastHost(state: AeroToastHostState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.toasts.forEachIndexed { index, data ->
                key(data.id) {
                    LaunchedEffect(data.id) {
                        delay(data.duration.millis)
                        if (state.toasts.contains(data)) {
                            state.toasts.remove(data)
                            data.onDismissed(AeroToastResult.Dismissed)
                        }
                    }
                    AeroToastItem(data) {
                        state.toasts.remove(data)
                        data.onDismissed(AeroToastResult.ActionPerformed)
                    }
                }
            }
        }
    }
}
```

**KEY POINT for the planning spike:** `key(data.id)` ensures animation state survives stack reordering. `AnimatedVisibility(slideInVertically + fadeIn)` on each toast item gives the appearance animation. `Column(verticalArrangement = Arrangement.spacedBy(...))` automatically shifts existing toasts upward as new ones append — but if you want a smoother shift you can wrap each in a `Box(Modifier.offset { ... })` driven by `animateIntOffsetAsState(targetValue = positionForIndex(index))`.

### Pattern 6: Drawer with Scrim + Slide Animation
**What:** Full-screen `Box` with two children: scrim (clickable, dismisses) + sliding panel (animated `offset.x`).

**When to use:** AeroDrawer (OVL-08).

**Example:**
```kotlin
@Composable
public fun AeroDrawer(
    open: Boolean,
    onDismissRequest: () -> Unit,
    side: AeroDrawerSide = AeroDrawerSide.Start,
    drawerWidth: Dp = 280.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val targetOffset by animateFloatAsState(
        targetValue = if (open) 0f else 1f,
        animationSpec = tween(220, easing = LinearEasing),
        label = "drawerOffset"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (open) 0.5f else 0f,
        animationSpec = tween(220, easing = LinearEasing),
        label = "scrimAlpha"
    )
    if (!open && targetOffset == 1f) return
    Box(Modifier.fillMaxSize()) {
        // Scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(onClick = onDismissRequest, indication = null,
                           interactionSource = remember { MutableInteractionSource() })
        )
        // Panel
        val align = if (side == AeroDrawerSide.Start) Alignment.CenterStart else Alignment.CenterEnd
        val signedOffset = if (side == AeroDrawerSide.Start) -targetOffset else targetOffset
        Box(
            Modifier
                .align(align)
                .offset { IntOffset((signedOffset * drawerWidth.toPx()).roundToInt(), 0) }
                .width(drawerWidth)
                .fillMaxHeight()
                .glassSurface(cornerRadius = 0.dp)
        ) {
            Column(content = content)
        }
    }
}
```

### Pattern 7: Themed Scrollbar via LocalScrollbarStyle
**What:** Provide `LocalScrollbarStyle` inside `AeroTheme {}` so every `VerticalScrollbar`/`HorizontalScrollbar` call automatically picks up Aero colors and dimensions.

**When to use:** AeroScrollBar (CNT-06), AeroScrollArea (CNT-05). Foundation `VerticalScrollbar` calls inside dropdown popup, text area, etc.

**Example:**
```kotlin
// Modify theme/AeroTheme.kt to provide LocalScrollbarStyle:

@Composable
public fun AeroTheme(
    colorScheme: AeroColorScheme = AeroColorScheme.AeroBlue,
    typography: AeroTypography = AeroTypography(),
    content: @Composable () -> Unit
) {
    val scrollbarStyle = ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 12.dp,
        shape = RoundedCornerShape(6.dp),
        hoverDurationMillis = 150,
        unhoverColor = colorScheme.cardBackground,        // thumb default
        hoverColor = colorScheme.borderSelected           // thumb on hover
    )
    CompositionLocalProvider(
        LocalAeroColors provides colorScheme,
        LocalAeroTypography provides typography,
        LocalScrollbarStyle provides scrollbarStyle
    ) {
        MaterialTheme(...) { content() }
    }
}

// Then AeroScrollBar is just:
@Composable
public fun AeroScrollBar(scrollState: ScrollState, modifier: Modifier = Modifier) {
    VerticalScrollbar(adapter = rememberScrollbarAdapter(scrollState), modifier = modifier)
}

// AeroScrollArea wraps content + auto-attaches AeroScrollBar:
@Composable
public fun AeroScrollArea(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier) {
        Column(Modifier.verticalScroll(state)) { content() }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(state),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}
```

### Anti-Patterns to Avoid
- **`transparent = true` on any Window or DialogWindow** — Win11 EXCEPTION_ACCESS_VIOLATION (CMP-3757, GH#3171). Even after CMP 1.7.3 the bug persists. Strict rule: `undecorated = true` only.
- **Custom HTCAPTION via JNI for Aero Snap** — declared out of scope (would break platform-neutral JAR). Document the limitation in `AeroTitleBar` KDoc and proceed.
- **Re-implementing scrollbar drag/page logic** — `VerticalScrollbar` already handles thumb drag, click-track-to-page, mouse-wheel routing, and OS fling. Wrap, don't replace.
- **Material3 `SnackbarHostState` for AeroToast** — single-toast queue; can't show 3-5 stacked toasts. Build a custom `AeroToastHostState` with `mutableStateListOf` (per CONTEXT.md spike note).
- **Hard-coding scrollbar colors per call-site** — install once in `AeroTheme` via `LocalScrollbarStyle`; every `VerticalScrollbar` call inherits the styling.
- **Putting Min/Max/Close buttons inside `WindowDraggableArea` without their own `clickable`** — `WindowDraggableArea` swallows pointer events; the click handlers on each TitleBarButton must be on the Box, and the order matters (clickable wraps before drag area can capture).
- **`onPreviewKeyEvent` on a non-focused Window** — Esc-to-close on AeroDialog requires the DialogWindow content to actually have focus; CMP 1.7.3 typically focuses the first focusable composable, but if AeroDialog content is purely text, add a `Modifier.focusable()` to a hidden Box, or install the `onPreviewKeyEvent` directly on the DialogWindow itself (the `onPreviewKeyEvent` parameter on Window/DialogWindow exists in CMP 1.7.x).
- **Drawer that's always in the composition** — gate with `if (!open && offset == 1f) return` so the composable disappears when fully closed; otherwise hidden but composed children eat input.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Modal dialog with focus trap, Esc handling, OS-level modality | Custom in-window scrim + Popup | `androidx.compose.ui.window.DialogWindow(undecorated = true, transparent = false)` | DialogWindow is a real OS child window; the OS handles modality, focus trap, and Esc routing for free |
| Window drag region | Custom `pointerInput` that calls `setLocation` on the AWT window | `androidx.compose.foundation.window.WindowDraggableArea { ... }` | WindowDraggableArea handles AWT window-mover wiring, drag throttling, and titlebar/maximize toggle correctly |
| Scrollbar with thumb sizing, click-track paging, mouse-wheel routing, hover transitions | Custom `Layout` measuring viewport/content ratios | `VerticalScrollbar(adapter = rememberScrollbarAdapter(scrollState))` | Built-in handles every edge case + accessibility; restyle via `ScrollbarStyle`/`LocalScrollbarStyle` |
| Anchor-relative popup positioning with overflow flip | Manual `Layout` placing children at calculated offset | `Popup(popupPositionProvider = AeroPopupPositionProvider(side, gap))` | Popup handles re-layout on window resize, focus management, and dismissal |
| Right-click detection with cursor coordinates | Native AWT MouseListener | `Modifier.onPointerEvent(PointerEventType.Press) { event -> if (event.buttons.isSecondaryPressed) ... }` | Compose-idiomatic, works inside any composable, no AWT bridging |
| Tooltip with hover-delay + dismiss on leave | Custom `LaunchedEffect` + Popup | Either `TooltipArea(delayMillis = 600, ...)` (built-in) **or** custom Popup + `LaunchedEffect(hovered)` + `delay(600)` | CONTEXT.md decision: custom path for `AeroPopupPositionProvider` reuse — but TooltipArea is a viable simpler alternative if reuse isn't a hard requirement |
| Slide-in animation for drawer | Hand-tuned `Animatable<Float>` | `animateFloatAsState(target, tween(220))` driving an `offset { IntOffset(...) }` modifier | Standard pattern; survives recomposition; `AnimatedVisibility` is also viable but offset gives finer control |
| Focus-trap inside Dialog content | Custom focus walker | DialogWindow auto-traps focus to its content | OS-level child window naturally contains keyboard focus |
| App icon, title, and chrome rendering | Custom `AwtWindow` | `Window(undecorated = true)` + `FrameWindowScope.AeroTitleBar` | Standard CMP path; integrates with `WindowState`, `application { ... }`, `exitApplication` |

**Key insight:** Every Phase 3 component has a Compose Desktop primitive that handles the hard parts (focus, drag, scrollbar mechanics, popup positioning). Hand-rolling these would add hundreds of lines of edge-case code with no visual gain — Aero theming sits *on top* of these primitives, never replaces them.

## Common Pitfalls

### Pitfall 1: Win11 EXCEPTION_ACCESS_VIOLATION on `transparent = true`
**What goes wrong:** Setting `transparent = true` on `Window` or `DialogWindow` on Windows 11 x64 causes a fatal SKIKO crash (`EXCEPTION_ACCESS_VIOLATION` from `gdi32full.dll`) — the app dies immediately with no exception in Kotlin code.
**Why it happens:** Known CMP bug, tracked at CMP-3757 / GH#3171; rendering-pipeline interaction with DWM. Not fixed in CMP 1.7.3.
**How to avoid:** Use `undecorated = true` ALONE — never combine with `transparent = true`. Glass effect is faked with gradient on `background` (already proven in Phase 1).
**Warning signs:** App launches and immediately exits with code 0xC0000005; native crash log mentions skiko or gdi.
**Verification:** Phase 3 plan must include a manual smoke step: launch showcase main with `undecorated = true, transparent = false` — must not crash. Then open any AeroDialog — must also not crash.

### Pitfall 2: WindowDraggableArea Doesn't Pass HTCAPTION (No Aero Snap)
**What goes wrong:** Dragging a custom title bar to a screen edge does NOT trigger Windows' Aero Snap (snap-to-half, snap-to-quadrant). It just moves the window normally.
**Why it happens:** `WindowDraggableArea` calls `Component.setLocation` directly via Swing — it doesn't return HTCAPTION from the WM_NCHITTEST hook, so the OS doesn't recognize the drag as a titlebar drag.
**How to avoid:** Cannot avoid in Compose-only code. **Document the limitation** in `AeroTitleBar` KDoc and `PROJECT.md` Out-of-Scope. Aero Snap is explicitly v2+ (would require JNI).
**Warning signs:** User drags window to screen edge, expects half-screen snap, gets nothing.

### Pitfall 3: DialogWindow + `transparent = false` Means No Rounded Corners on the OS Window Frame
**What goes wrong:** Because we can't use `transparent = true` on Win11, the DialogWindow's outer rectangle is opaque — when the Aero glass content has `cornerRadius = 8.dp`, the corners of the OS window are still square (we see the OS background through the corner gaps).
**Why it happens:** Without transparency, the OS paints the window rectangle as the system "background" color before our Compose content draws.
**How to avoid:** (a) Make the DialogWindow's Compose root a fully-opaque Box that fills the entire window with the glass background — accept square outer corners as a Win11 trade-off; or (b) use `cornerRadius = 0.dp` on the dialog's root surface.
**Warning signs:** Sliver of grey/system color visible at the four corners of an open AeroDialog. Cosmetic — not a functional bug.

### Pitfall 4: Esc Key Doesn't Dismiss AeroDialog
**What goes wrong:** User presses Esc inside AeroDialog content; nothing happens.
**Why it happens:** DialogWindow doesn't auto-handle Esc — the standard close paths are the OS Close button (which we removed via `undecorated = true`!) and the `onCloseRequest` callback. Esc must be wired manually.
**How to avoid:** Pass `onPreviewKeyEvent` on the DialogWindow:
```kotlin
DialogWindow(
    onCloseRequest = onDismissRequest,
    undecorated = true,
    onPreviewKeyEvent = { e ->
        if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
            onDismissRequest(); true
        } else false
    }
) { ... }
```
**Warning signs:** AeroDialog has no visible close button (we removed OS chrome) AND Esc doesn't work — user is "trapped" in the dialog.

### Pitfall 5: Stacked Toast Animation Stutters on Stack Reorder
**What goes wrong:** When a toast in the middle of the stack dismisses, the toasts above it should slide down to fill the gap — but if you re-key the list each composition, Compose recomposes the toasts as new elements and the slide animation never plays.
**Why it happens:** `AnimatedVisibility` and `animateOffsetAsState` rely on stable composition keys. If the toast Composable is keyed by `index`, removing item 2 reassigns indexes of items 3+ to 2+ — Compose thinks they're new.
**How to avoid:** Use `key(data.id)` (stable per-toast unique ID) so each toast retains its composition slot and animation state across stack reorders.
**Warning signs:** Toasts pop into position instead of sliding; animation only plays on first appearance.

### Pitfall 6: AeroTooltip "Sticky" — Stays Visible After Mouse Leaves
**What goes wrong:** Tooltip appears on hover, then never dismisses even after the mouse moves away.
**Why it happens:** Two common causes: (a) `Popup` with `properties = PopupProperties(focusable = true)` steals hover state from the anchor element — the anchor's `MutableInteractionSource` never sees `Hover.Exit`; (b) `LaunchedEffect(hovered) { delay(600); show = true }` uses a key that doesn't reset when hover ends.
**How to avoid:** (a) Tooltip Popup must have `focusable = false`; (b) use `LaunchedEffect(hovered) { if (hovered) { delay(600); show = true } else { show = false } }` so leaving hover immediately hides.
**Warning signs:** Tooltip lingers visually; hover-leave doesn't dismiss; tooltip blocks mouse interaction with element below.

### Pitfall 7: AeroContextMenu Fires on Left-Click
**What goes wrong:** Context menu opens for both left and right clicks (or doesn't open at all).
**Why it happens:** Using `Modifier.clickable` triggers on ANY mouse button. Using `onPointerEvent(PointerEventType.Press)` without checking `event.buttons.isSecondaryPressed` triggers on every mouse-down.
**How to avoid:** `if (event.buttons.isSecondaryPressed) { ... }` inside `onPointerEvent(PointerEventType.Press)` block.
**Warning signs:** Left-clicking an element with context menu opens both the click action AND the menu.

### Pitfall 8: Popup Outside Window Bounds (No Auto-Flip)
**What goes wrong:** Tooltip or popover appears partially off-screen because it's anchored near the window edge and the position provider doesn't flip.
**Why it happens:** Default position providers don't flip — they just clamp to window bounds, which can produce a tooltip that's clipped on one side.
**How to avoid:** Use `AeroPopupPositionProvider` with auto-flip logic (already implemented; CONTEXT.md extends with `side` parameter). Verify all 4 sides flip correctly: Top↔Bottom, Start↔End.
**Warning signs:** Tooltip on a button at the top of the window appears partially clipped at top — should flip to below.

### Pitfall 9: AeroDrawer Eats Pointer Input When "Closed but Composed"
**What goes wrong:** User clicks somewhere on the main content; nothing happens because an invisible (offset to -100%) AeroDrawer still has its scrim Box above the content.
**Why it happens:** Animating `offset` doesn't remove the composable from the layout — the scrim Box is still receiving pointer events, just visually pushed off-screen.
**How to avoid:** `if (!open && targetOffset == 1f) return` — early-return when fully closed AND animation finished; or wrap in `AnimatedVisibility` which removes from composition.
**Warning signs:** Main UI becomes unresponsive after drawer is "closed"; dev console shows clicks consumed by Drawer scrim.

### Pitfall 10: AeroTitleBar Buttons Trigger Drag Instead of Click
**What goes wrong:** Clicking Min/Max/Close on the title bar drags the window instead of triggering the action.
**Why it happens:** Order of modifier matters. If `WindowDraggableArea` wraps children that include a button without its own `Modifier.clickable`, the drag area swallows the press.
**How to avoid:** Each title-bar button must have `Modifier.clickable { ... }` on its outer Box. Compose's pointer-event system gives priority to inner clickables over outer drag handlers — but only when both exist as distinct modifiers.
**Warning signs:** Window starts dragging when you click Close; close doesn't fire.

## Code Examples

Verified patterns from official sources and existing code:

### Mounting AeroTitleBar in `Window(undecorated = true)`
```kotlin
// Source: ported from mordred/AeroTitleBar.kt + CMP 1.7.3 docs

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "aero-compose-ui Showcase",
        state = windowState,
        undecorated = true,
        transparent = false  // CRITICAL Win11 rule
    ) {
        Column(Modifier.fillMaxSize()) {
            AeroTitleBar(
                title = "aero-compose-ui Showcase",
                windowState = windowState,
                onCloseRequest = ::exitApplication
            )
            ShowcaseApp(modifier = Modifier.weight(1f))
        }
    }
}
```

### Custom Resize Handles for Undecorated Window
```kotlin
// 4-edge + 4-corner invisible resize zones; only active when not maximized

@Composable
private fun FrameWindowScope.ResizeHandles(windowState: WindowState) {
    if (windowState.placement != WindowPlacement.Floating) return
    Box(Modifier.fillMaxSize()) {
        // Right edge — 4dp wide, full height
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(4.dp).fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        windowState.size = DpSize(
                            (windowState.size.width.value + drag.x.toDp().value).dp,
                            windowState.size.height
                        )
                    }
                }
        )
        // Repeat for left, top, bottom, and 4 corners with appropriate cursor
    }
}
```

### AeroAlertDialog Built on Top of AeroDialog
```kotlin
public enum class AeroAlertKind { Info, Warning, Error, Question }

@Composable
public fun AeroAlertDialog(
    onDismissRequest: () -> Unit,
    kind: AeroAlertKind,
    title: String,
    message: String,
    confirmText: String = "OK",
    cancelText: String = "Cancel",
    onConfirm: () -> Unit = onDismissRequest
) {
    val (icon, accent) = when (kind) {
        AeroAlertKind.Info     -> Icons.Outlined.Info to AeroTheme.colors.primary
        AeroAlertKind.Warning  -> Icons.Outlined.Warning to Color(0xFFFFA726)
        AeroAlertKind.Error    -> Icons.Outlined.Error to AeroTheme.colors.error
        AeroAlertKind.Question -> Icons.Outlined.HelpOutline to AeroTheme.colors.primary
    }
    AeroDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = accent)
                Text(title, style = AeroTheme.typography.title, color = AeroTheme.colors.onSurface)
            }
        },
        content = {
            Text(message, color = AeroTheme.colors.onSurface,
                 style = AeroTheme.typography.bodyLarge)
        },
        buttons = {
            if (kind == AeroAlertKind.Question) {
                AeroOutlinedButton(text = cancelText, onClick = onDismissRequest)
                Spacer(Modifier.width(8.dp))
            }
            AeroButton(text = confirmText, onClick = onConfirm)
        }
    )
}
```

### AeroNotificationBanner (in-flow, OVL-06)
```kotlin
public enum class AeroBannerKind { Info, Warning, Error, Success }

@Composable
public fun AeroNotificationBanner(
    kind: AeroBannerKind,
    text: String,
    onDismiss: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (icon, accent) = when (kind) {
        AeroBannerKind.Info    -> Icons.Outlined.Info to AeroTheme.colors.primary
        AeroBannerKind.Warning -> Icons.Outlined.Warning to Color(0xFFFFA726)
        AeroBannerKind.Error   -> Icons.Outlined.Error to AeroTheme.colors.error
        AeroBannerKind.Success -> Icons.Outlined.CheckCircle to Color(0xFF66BB6A)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(cornerRadius = 4.dp)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent)
        Text(text, modifier = Modifier.weight(1f),
             color = AeroTheme.colors.onSurface, style = AeroTheme.typography.bodyLarge)
        actions?.invoke(this)
        if (onDismiss != null) {
            AeroIconButton(onClick = onDismiss, size = 24.dp) {
                Text("✕", color = AeroTheme.colors.onSurface)
            }
        }
    }
}
```

### AeroBreadcrumb (NAV-04)
```kotlin
public data class AeroBreadcrumbItem(val label: String, val payload: Any? = null)

@Composable
public fun AeroBreadcrumb(
    items: List<AeroBreadcrumbItem>,
    onItemClick: (Int, AeroBreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    separator: String = "›"
) {
    val colors = AeroTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            Text(
                text = item.label,
                color = if (isLast) colors.onSurface else colors.primary,
                style = AeroTheme.typography.bodyLarge,
                modifier = if (!isLast) Modifier.clickable { onItemClick(index, item) } else Modifier
            )
            if (!isLast) {
                Text(separator, color = colors.labelText, style = AeroTheme.typography.bodyLarge)
            }
        }
    }
}
```

### Showcase Wiring — Phase 3 Sections
The current showcase pattern (verified in `ButtonsSection.kt`):
- Each section is a `*Section.kt` file in `showcase/src/main/kotlin/com/mordred/showcase/sections/`
- Each section starts with `Text("CategoryName", ...)` title at `typography.title`
- Each row uses a private `SectionRow(label = "..."){ /* variants in RowScope */ }` helper
- The label column is fixed `Modifier.width(140.dp)`; variants are arranged in a `Row` with `spacedBy(8.dp)`

Phase 3 adds three sections (`ContainersSection`, `OverlaysSection`, `NavigationSection`) following the exact same template. `ShowcaseApp.kt` mounts them at the top level along with `AeroToastHost(toastState)` which sits inside the root `Surface` so it overlays the entire showcase window:
```kotlin
@Composable
fun ShowcaseApp() {
    var currentScheme by remember { mutableStateOf(AeroColorScheme.AeroBlue) }
    val toastState = remember { AeroToastHostState() }
    AeroTheme(colorScheme = currentScheme) {
        Surface(...) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ThemeSwitcher(...)
                    FoundationSection(); ButtonsSection(); InputSection()
                    SelectionSection(); DropdownSection(); RangeSection(); ListSection()
                    ContainersSection()  // NEW
                    OverlaysSection(toastState)  // NEW — passes toastState for showToast demo
                    NavigationSection()  // NEW
                }
                AeroToastHost(toastState, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Window(transparent = true, undecorated = true)` for glass effect | `Window(undecorated = true, transparent = false)` + gradient simulation | CMP 1.5+ (Win11 crash) | Required compromise; visually equivalent thanks to `glassEffect` |
| `androidx.compose.foundation.v2.VerticalScrollbar` (separate v2 namespace) | `androidx.compose.foundation.VerticalScrollbar` (top-level since CMP 1.6) | CMP 1.6+ | Simpler imports; identical API. Use top-level namespace. |
| `Dialog` (in-window scrim Popup) for modal | `DialogWindow` (real OS child window) | CMP 1.5+ | DialogWindow gives true OS modality; better focus trap; more "native feel" |
| Material3 `BasicAlertDialog` for app dialogs | `DialogWindow` + custom content | CMP 1.4+ for desktop apps | Custom DialogWindow gives full control over chrome; M3 dialog still fine if OS modality not needed |

**Deprecated/outdated:**
- `androidx.compose.ui.window.Dialog` (the older in-window scrim version) — still works, but `DialogWindow` is preferred for desktop modal needs.
- Hand-rolled scroll thumbs based on viewport-ratio — the standard `VerticalScrollbar` does it correctly; no reason to reinvent.

## Open Questions

1. **AeroMenuBar cross-item hover-switch implementation**
   - **What we know:** When one top-level menu (e.g., "File") is open, hovering "Edit" should auto-open Edit's submenu without a click. This is standard desktop OS behavior.
   - **What's unclear:** How to track "any menu is currently open" state shared across siblings — likely a hoisted `var openIndex: Int by mutableStateOf(-1)` lifted into the AeroMenuBar root.
   - **Recommendation:** Plan stage should sketch the state-hoisting model. A Claude's discretion item per CONTEXT.md.

2. **AeroToast stacked animation: shift-up vs. simple Column reorder**
   - **What we know:** CONTEXT.md flags this as needing a planning spike. Three viable approaches: (a) plain `Column(Arrangement.spacedBy(8.dp))` — Compose layout engine handles repositioning, but no animation; (b) `Column` + each toast wrapped in `Box(Modifier.animateContentSize())` — animates layout changes for free; (c) manual `Modifier.offset { IntOffset(0, indexToY(index).roundToInt()) }` driven by `animateIntAsState`.
   - **What's unclear:** Whether `animateContentSize()` produces a visually pleasing shift on a parent Column when a sibling is removed.
   - **Recommendation:** Spike all three in a 30-min timebox during planning; pick the simplest that gives smooth visual transitions. Default if no time: option (b).

3. **AeroTabBar overflow handling**
   - **What we know:** Browser-style tabs with horizontal scroll + arrow buttons OR truncate. CONTEXT.md leaves this Claude's discretion.
   - **What's unclear:** How many tabs is "too many" — a typical desktop tabbar fits 6-10 tabs; beyond that overflow needs handling.
   - **Recommendation:** v1: horizontal scroll inside a `Row(Modifier.horizontalScroll(...))` + small arrow buttons that page-scroll the tab row. Document as "v1 simple overflow"; sophisticated overflow menu is v2.

4. **AeroPopover arrow/caret pointing at anchor**
   - **What we know:** Claude's discretion. Adds visual clarity but requires extra `Canvas` work to draw the caret triangle.
   - **What's unclear:** Whether the caret color/shape should adapt per `side` (Top/Bottom/Start/End).
   - **Recommendation:** Skip in v1 unless trivially achievable. Document as "v1 ships flat popover; arrow is v2 enhancement."

5. **AeroDialog content region focus management**
   - **What we know:** DialogWindow auto-focuses first focusable child.
   - **What's unclear:** What happens when content is purely text (e.g., AeroAlertDialog with just an icon + message)?
   - **Recommendation:** Add `Modifier.focusable()` + `LaunchedEffect { focusRequester.requestFocus() }` to the dialog root Box so `onPreviewKeyEvent` for Esc fires reliably. Verify in showcase smoke test.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) 5.10.0 + kotlin-test (uses junit-jupiter runner) |
| Config file | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| Quick run command | `./gradlew :library:test --tests com.mordred.aero.**` |
| Full suite command | `./gradlew :library:test :showcase:assemble` |

**Key constraint:** Phase 1 + 2 used compile-time + smoke tests only (no UI rendering tests in CI). The pivot in `AeroThemeTest` notes that `ProvidableCompositionLocal.defaultFactory` isn't accessible at runtime in CMP 1.7.3 — so even simple "factory returns AeroBlue" assertions had to fall back to compile-reachability tests.

**Phase 3 implication:** Most Phase 3 components are visual/interactive — true verification is **manual visual checkpoint via the showcase**. Automated tests cover (1) public API surface compile-reachability, (2) data class equality semantics (e.g., `AeroAlertKind`, `AeroBannerKind`, `AeroContextMenuItem.Action`), (3) state-model unit tests (e.g., `AeroToastHostState.showToast` returns expected `AeroToastResult`).

The full suite of 19 components reaches "verified" via the showcase visual checkpoint that Phase 2 already established as the project's verification primitive.

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CNT-01 | AeroCard renders with glassEffect background | compile + visual | `./gradlew :library:compileKotlin` + showcase visual | Wave 0: `library/src/test/kotlin/.../components/containers/AeroCardTest.kt` |
| CNT-02 | AeroPanel renders with glassPanel background | compile + visual | as above | Wave 0: `AeroPanelTest.kt` |
| CNT-03 | AeroDivider renders horizontal + vertical | compile + visual | as above | Wave 0: `AeroDividerTest.kt` |
| CNT-04 | AeroGroupBox renders labeled border | compile + visual | as above | Wave 0: `AeroGroupBoxTest.kt` |
| CNT-05 | AeroScrollArea wraps ScrollState | compile + smoke + visual | `AeroScrollAreaTest.compileSmoke` | Wave 0: `AeroScrollAreaTest.kt` |
| CNT-06 | AeroScrollBar accepts ScrollState; uses LocalScrollbarStyle | compile + smoke + visual | `AeroScrollBarTest.compileSmoke` | Wave 0: `AeroScrollBarTest.kt` |
| OVL-01 | AeroDialog opens DialogWindow with title/content/buttons slots | compile + visual + manual smoke (Esc closes) | compile + manual showcase | Wave 0: `AeroDialogTest.kt` (compile-only — DialogWindow is OS-level, not unit-testable headlessly) |
| OVL-02 | AeroAlertDialog has 4 kinds with correct icon/color | compile + data-class + visual | `AeroAlertKindTest.allVariantsHaveIcon` | Wave 0: `AeroAlertDialogTest.kt`, `AeroAlertKindTest.kt` |
| OVL-03 | AeroTooltip shows after 600ms hover; AeroPopupPositionProvider(Top) flips to Bottom on overflow | compile + position-provider unit + visual | `AeroPopupPositionProviderTest.flipsTopToBottom` | Wave 0: `AeroTooltipTest.kt`, `AeroPopupPositionProviderTest.kt` |
| OVL-04 | AeroContextMenu opens on right-click; sealed-class items | compile + sealed-class + visual | `AeroContextMenuItemTest.sealedHierarchy` | Wave 0: `AeroContextMenuTest.kt`, `AeroContextMenuItemTest.kt` |
| OVL-05 | AeroToast auto-dismisses after duration; stacks up to 5 | unit (state model) + visual | `AeroToastHostStateTest.dismissesAfterDuration`, `evictsOldestAtStackLimit` | Wave 0: `AeroToastHostStateTest.kt` |
| OVL-06 | AeroNotificationBanner has 4 kinds with icon + close button | compile + data-class + visual | `AeroBannerKindTest.allVariantsHaveIcon` | Wave 0: `AeroNotificationBannerTest.kt`, `AeroBannerKindTest.kt` |
| OVL-07 | AeroPopover anchors to host element; flips on overflow | compile + position-provider unit + visual | `AeroPopupPositionProviderTest` (shared) | shared test from OVL-03 |
| OVL-08 | AeroDrawer slides in from Start/End; dismisses on scrim click + Esc | compile + visual + manual smoke | manual showcase | Wave 0: `AeroDrawerTest.kt` (compile-only) |
| NAV-01 | AeroTitleBar renders gradient + Min/Max/Close + drag area; toggles WindowPlacement | compile + visual + manual smoke | manual showcase: drag, minimize, maximize/restore, close | Wave 0: `AeroTitleBarTest.kt` (compile-only — needs FrameWindowScope) |
| NAV-02 | AeroMenuBar shows top items + dropdowns; cross-item hover switches | compile + visual + manual smoke | manual showcase | Wave 0: `AeroMenuBarTest.kt` (compile-only) |
| NAV-03 | AeroStatusBar renders sections + indicators | compile + visual | as above | Wave 0: `AeroStatusBarTest.kt` |
| NAV-04 | AeroBreadcrumb emits onItemClick(index, item) | compile + unit + visual | `AeroBreadcrumbTest.clickEmitsCorrectIndex` | Wave 0: `AeroBreadcrumbTest.kt` |
| NAV-05 | AeroTabBar switches active tab; horizontal-scroll overflow | compile + visual | as above | Wave 0: `AeroTabBarTest.kt` |

**Verification primitives by component class:**

| Class | Primary Verification | Notes |
|-------|---------------------|-------|
| Containers (CNT) | Visual check via showcase + compile reachability | Static layout + glass styling — visual is decisive |
| Overlays w/ DialogWindow (OVL-01, OVL-02) | Manual smoke: open dialog, press Esc, verify Win11 no-crash | DialogWindow is OS-level — no headless unit test |
| Popup-based (OVL-03, OVL-04, OVL-07) | Position-provider unit test (auto-flip) + visual | Positioning logic is unit-testable; visual confirms theme |
| State-model (OVL-05) | Unit test on `AeroToastHostState`: enqueue → delay → dismiss → evict | Pure-Kotlin state machine, fully testable |
| In-flow widgets (OVL-06, NAV-03, NAV-04, NAV-05) | Visual + small unit test on enum/data class | Plain Compose widgets |
| Window-chrome (NAV-01) | Compile-reachability + manual smoke (drag, min, max, close) | Cannot be unit-tested without spinning up a real window |
| Menu structures (NAV-02, OVL-04) | Compile + sealed-class hierarchy unit + manual smoke | Submenu behavior is interaction-heavy |

### Sampling Rate
- **Per task commit:** `./gradlew :library:test --tests com.mordred.aero.components.<package>.*` (~8s incremental)
- **Per wave merge:** `./gradlew :library:test :library:compileKotlin :showcase:compileKotlin` (~25s)
- **Phase gate:** Full suite + manual visual checkpoint (showcase launched, all 53 components clicked through each theme) — green required before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `library/src/test/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProviderTest.kt` — position + flip logic unit tests (shared by OVL-03, OVL-04, OVL-07)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroToastHostStateTest.kt` — state-model lifecycle (enqueue/dismiss/evict)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt` — enum coverage (each kind maps to icon + color)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt` — same pattern
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroContextMenuItemTest.kt` — sealed-class structural test
- [ ] `library/src/test/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumbTest.kt` — onItemClick callback
- [ ] Compile-reachability tests for the rest (one-liner per public composable, mirroring Phase 1 pattern)

*Existing test infrastructure (JUnit 5 + kotlin-test) is sufficient — no framework install needed. The test runner already runs successfully (Phase 1/2 tests pass).*

## Sources

### Primary (HIGH confidence)
- **`C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/components/AeroTitleBar.kt`** — direct port reference for NAV-01 (read in this research session)
- **`.planning/phases/03-composite-navigation/03-CONTEXT.md`** — locked decisions (read in this research session)
- **`library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt`, `AeroColorScheme.kt`, `GlassModifiers.kt`** — Phase 1 primitives (read in this research session)
- **`library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdownMenu.kt`** — internal AeroPopupPositionProvider + AeroDropdownPopup pattern (read in this research session)
- **`library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt`, `AeroToolbar.kt`, `InteractionStates.kt`** — interaction state collection patterns (read in this research session)
- **Kotlin Multiplatform docs — Top-level windows management** (`https://kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html`) — DialogWindow, FrameWindowScope, WindowState, WindowPlacement, WindowDraggableArea
- **Kotlin Multiplatform docs — Scrollbars** (`https://kotlinlang.org/docs/multiplatform/compose-desktop-scrollbars.html`) — VerticalScrollbar, rememberScrollbarAdapter API
- **JetBrains/compose-multiplatform-core — Scrollbar.skiko.kt** (`https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/foundation/foundation/src/skikoMain/kotlin/androidx/compose/foundation/Scrollbar.skiko.kt`) — ScrollbarStyle data class fields (minimalHeight, thickness, shape, hoverDurationMillis, unhoverColor, hoverColor); LocalScrollbarStyle composition local; defaultScrollbarStyle()
- **Kotlin Multiplatform docs — Tooltips** (`https://kotlinlang.org/docs/multiplatform/compose-desktop-tooltips.html`) — TooltipArea, TooltipPlacement, delayMillis (default 500ms; CONTEXT.md picks 600ms)
- **Kotlin Multiplatform docs — Context menus** (`https://kotlinlang.org/docs/multiplatform/compose-desktop-context-menus.html`) — ContextMenuArea, ContextMenuRepresentation, LocalContextMenuRepresentation
- **Kotlin Multiplatform docs — Mouse event listeners** (`https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-desktop-mouse-events.html`) — onPointerEvent(PointerEventType.Press), event.buttons.isSecondaryPressed, event.changes.firstOrNull()?.position

### Secondary (MEDIUM confidence — verified against multiple sources)
- **JetBrains GitHub issue #3757 / CMP-3757** — Win11 EXCEPTION_ACCESS_VIOLATION on `transparent = true` + `undecorated = true`; not fixed in CMP 1.7.3
- **JetBrains GitHub issue #3171** — same crash, broader scope (any Window/Dialog with both flags)
- **`https://www.sasikanth.dev/dragging-undecorated-windows-in-compose-desktop/`** — WindowDraggableArea integration patterns
- **`https://dev.to/tkuenneth/centering-top-level-dialog-windows-in-compose-multiplatform-1nmk`** — DialogWindow positioning
- **`https://dev.to/tkuenneth/customize-a-compose-for-desktop-alertdialog-a6e`** — DialogWindow undecorated + custom content

### Tertiary (LOW confidence — single source, may need validation)
- AeroMenuBar cross-item hover-switch state model — derived from general OS UX expectations, no specific Compose Desktop reference; flag for spike during planning.
- AeroToast stacked-shift animation — three viable approaches identified, none verified in production Compose Desktop code; flag for spike during planning.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — every primitive verified against CMP 1.7.3 docs and existing Phase 1/2 code. CMP 1.7.3 is the locked project version (`gradle/libs.versions.toml`).
- Architecture: HIGH — every pattern follows CONTEXT.md decisions and existing Phase 2 patterns (AeroDropdownPopup, AeroIconButton state collection)
- Pitfalls: HIGH — Win11 crash is documented JetBrains issue; HTCAPTION limitation is acknowledged in STATE.md; Esc-handling, hover-leave, and stack-key gotchas are well-documented Compose patterns
- Validation Architecture: HIGH — mirrors Phase 1/2 pattern (compile-reachability + smoke + manual visual checkpoint), consistent with existing test infrastructure (JUnit 5 + kotlin-test)
- Open questions: explicitly flagged as Claude's discretion items per CONTEXT.md — no LOW confidence claims masquerading as fact

**Research date:** 2026-04-28
**Valid until:** 2026-05-28 (30 days — CMP 1.7.3 is stable; no version bump expected during Phase 3 execution)
