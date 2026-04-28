# Phase 3: Composite + Navigation - Context

**Gathered:** 2026-04-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement all 19 composite, overlay, and navigation components that complete the library: containers (CNT-01..06: AeroCard, AeroPanel, AeroDivider, AeroGroupBox, AeroScrollArea, AeroScrollBar), overlays (OVL-01..08: AeroDialog, AeroAlertDialog, AeroTooltip, AeroContextMenu, AeroToast/Snackbar, AeroNotificationBanner, AeroPopover, AeroDrawer), and navigation/window-chrome (NAV-01..05: AeroTitleBar, AeroMenuBar, AeroStatusBar, AeroBreadcrumb, AeroTabBar). Showcase becomes the "definition of done" for v1: every component from Phases 1-3 appears in the appropriate category section, is interactive (not a placeholder), and renders correctly across all three themes.

Complex stateful components (AeroDataTable, AeroTreeView, all date/time pickers, AeroColorPicker, AeroRangeSlider) and advanced layout (AeroAccordion, AeroSplitPane, AeroSidebar, AeroStepperWizard) are explicitly v2 — out of scope for this phase.

</domain>

<decisions>
## Implementation Decisions

### Window chrome deployment (NAV-01 AeroTitleBar)
- **showcase Main.kt switches to `Window(undecorated = true)`** — demonstrates the real Aero window, not a visual mock-up. This validates that the Win11 EXCEPTION_ACCESS_VIOLATION blocker only triggers with `transparent = true`; using `undecorated` alone is safe.
- **AeroTitleBar lives inside `FrameWindowScope`** — the composable signature is `fun FrameWindowScope.AeroTitleBar(...)` so it can call `WindowDraggableArea` and access `WindowState`. Direct port of `mordred/AeroTitleBar.kt`.
- **WindowDraggableArea wraps the entire title-bar Row** — same pattern as mordred. The Min/Max/Close buttons have their own `clickable {}` handlers, so click wins over drag automatically. Familiar UX (any spot on the bar drags the window).
- **API signature includes `leading: @Composable (() -> Unit)? = null`** — optional slot for an app icon before the title text. Min/Max/Close are fixed (built-in to component, not slot-overrideable in v1).
- **Custom resize handles inside the window** — invisible 4-6dp pointer-input zones along the four edges and four corners; on drag they update `windowState.size`. Cursor changes to native resize cursor on hover (PointerIcon.Crosshair or platform-specific). When `windowState.placement = Maximized`, resize handles are disabled.
- **Aero Snap (HTCAPTION) is NOT implemented** — `WindowDraggableArea` does not pass HTCAPTION to the OS, so native snap-to-edge / snap-to-half / snap-to-quarter behaviors are unavailable. This is documented in `AeroTitleBar` KDoc and added to `PROJECT.md` Out-of-Scope. JNI/WinAPI HTCAPTION spike is explicitly v2+ (would break platform-neutral JAR).

### Dialog & overlay infrastructure (OVL-01/02/04/07/08, AeroDrawer)
- **AeroDialog and AeroAlertDialog use `androidx.compose.ui.window.DialogWindow`** — a real OS-level child window. Gives true modality (parent input is blocked), free focus trap, and built-in Esc handling. Inside the DialogWindow, the chrome itself uses `undecorated = true` (NO `transparent = true` — Win11 rule still applies) so the AeroDialog draws its own glass-styled container without a visible OS frame around it.
- **AeroDialog API has 3 slots**: `AeroDialog(onDismissRequest, title: @Composable () -> Unit, content: @Composable () -> Unit, buttons: @Composable RowScope.() -> Unit = {})`. `buttons` is a `RowScope` lambda so callers naturally use `Arrangement.End` and place 1+ AeroButton/AeroOutlinedButton children. Mirrors Material3 AlertDialog's flexibility, with the slot model the team already uses.
- **AeroAlertDialog has 4 variants** via `enum class AeroAlertKind { Info, Warning, Error, Question }`. Each kind picks an icon (Material `Icons.Outlined.Info / Warning / Error / HelpOutline`) and an accent color (primary / orange / error / primary). Default buttons match kind: Info/Warning/Error → single OK; Question → OK + Cancel. Caller can override button text via parameters.
- **Scrim color is `Color.Black.copy(alpha = 0.5f)`** for AeroDrawer and any in-window overlays that need one. AeroDialog/AeroAlertDialog don't paint a scrim themselves — DialogWindow handles modality at the OS level. **No new token added to AeroColorScheme** (keeping the locked 23-token set from Phase 1).
- **`AeroPopupPositionProvider` becomes public and is extended with a `side` parameter**: `class AeroPopupPositionProvider(side: AeroPopupSide = AeroPopupSide.Bottom, gap: Int = 4)`, where `AeroPopupSide = { Top, Bottom, Start, End }`. Auto-flips to opposite side on overflow (current "flip vertically" logic generalizes to any side). The provider moves out of `dropdown/AeroDropdownMenu.kt` into `library/.../popup/AeroPopupPositionProvider.kt` so AeroPopover, AeroTooltip, and AeroMenuBar dropdown all reuse it. AeroDropdown keeps its current behavior (Bottom is the default).
- **AeroContextMenu uses a separate `AeroCursorPositionProvider`** — positions the popup at the right-click cursor coordinates (clamped to window bounds). Cursor position is captured in the right-click handler.
- **AeroTooltip behavior: anchor-fixed, 600ms in-delay, 0ms out-delay**. Uses `AeroPopupPositionProvider(side = Top)` with auto-flip. Tooltip does NOT follow the cursor — it stays attached to the anchor element. Public API: `AeroTooltip(text: String, content: @Composable () -> Unit)` plus a convenience `Modifier.aeroTooltip("text")` extension.
- **AeroContextMenu API: `Modifier.aeroContextMenu(items: List<AeroContextMenuItem>)`** as the primary entry point. The modifier handles right-click detection, captures cursor position, and shows the popup. `AeroContextMenuItem` is a `sealed class`: `Action(label, icon?, shortcut?, onClick)`, `Divider`, `Submenu(label, items)`. Submenus are supported recursively (no hard depth cap, but expected usage is 1-2 levels).
- **AeroDrawer: modal with scrim, side parameter `AeroDrawerSide { Start, End }`**. Slide-in animation via `animateFloatAsState` (offset.x), 200-250ms tween. Dismiss = scrim click + Esc. Drawer width is caller-controlled. Top/Bottom drawer variants are deferred (not in v1 scope).

### AeroScrollBar / AeroScrollArea (CNT-05/06)
- **Wrap Compose Foundation v2 scrollbar** — reuse `androidx.compose.foundation.v2.VerticalScrollbar`, `HorizontalScrollbar`, and `ScrollbarAdapter`. We override visual via `ScrollbarStyle` / `LocalScrollbarStyle`, getting drag-thumb, click-track-to-page, OS fling integration, and accessibility for free.
- **Width = 12dp** for both vertical (width) and horizontal (height) — comfortable hit target on desktop, matches Aero/Win-classic. Compose default 8dp is too thin.
- **Always visible** — track + thumb are visible whenever there is overflow. Windows-style (no auto-hide, no Mac-style fade-out). Predictable signal that content is scrollable.
- **AeroTheme installs `LocalScrollbarStyle`** with Aero tokens (track ≈ `glassSurface` / `borderDefault`, thumb ≈ `cardBackground` / `borderSelected` on hover) — every `VerticalScrollbar` call inside the theme automatically picks up Aero styling.
- **Retrofit existing Phase 2 components**: `AeroDropdownPopup` (currently `verticalScroll(rememberScrollState())` without a visible bar) and `AeroTextArea` are migrated to use `AeroScrollArea` (or explicitly attach an `AeroScrollBar`). Phase 2 code is edited as part of Phase 3 — not "new code only."
- **AeroScrollArea API**: takes content slot, manages its own `ScrollState` by default, optionally accepts an external `state` for caller control. Vertical-only in v1 (horizontal can be a Claude's-discretion follow-up if trivial).

### Toast / Banner notification hosting (OVL-05/06)
- **AeroToast/AeroSnackbar position: Bottom-end** (bottom-right in LTR, bottom-left in RTL), fixed 16dp from window edges. Single `AeroToastHost(state)` placed once at the root of `ShowcaseApp` (or any consumer's app shell).
- **API matches Material3 SnackbarHostState pattern**: `class AeroToastHostState { suspend fun showToast(message: String, duration: AeroToastDuration = Short, action: AeroToastAction? = null): AeroToastResult }`. Caller does `val state = remember { AeroToastHostState() }`, places `AeroToastHost(state)` once, and from any coroutine `scope.launch { state.showToast("Saved") }`.
- **Queue: stack** — multiple toasts visible simultaneously (max 3-5 stacked). New toast appears at the bottom; older ones animate upward. When the stack is full, the oldest is auto-dismissed early to make room. **NOTE for planning: stacked toast lifecycle/animation is the trickiest part of this phase — needs a small spike during planning to validate the state model (each toast has independent dismiss timer + position-in-stack derived from list index + animateOffsetAsState for shift-up animation).**
- **AeroToastDuration**: `Short = 4_000ms`, `Long = 10_000ms`, `Indefinite = Long.MAX_VALUE` (caller dismisses programmatically). Mirrors Material3.
- **AeroNotificationBanner: in-flow component, NOT host-based**. Signature: `AeroNotificationBanner(kind: AeroBannerKind, text: String, onDismiss: (() -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null)`. Caller places it inline in their layout (top of a form, under title bar, inside a section) and controls visibility with `if (visible) AeroNotificationBanner(...)`. No queue, no host — it's a static-feel UI element. `AeroBannerKind = { Info, Warning, Error, Success }` — same icon/color mapping as AeroAlertDialog plus Success (green/primary tint).

### Carry-forward from Phase 1 / Phase 2 (locked, not re-asked)
- All state animations: `animateFloatAsState` / `animateColorAsState` with `tween(150, easing = LinearEasing)`.
- Hover overlay = `buttonHover` token; Focus = border swap to `borderSelected`; Disabled = 40% alpha (contentAlpha = 0.4f); Pressed = Claude's discretion per component.
- All public types in `:library` carry explicit `public` modifier (`explicitApi()`).
- Material3 wraps inside `AeroTheme {}` — components MAY delegate to M3 primitives where it fits (e.g., AeroDialog's button area can compose `AeroButton`/`AeroOutlinedButton` which themselves wrap M3 Button).
- Each category gets its own `*Section.kt` in `showcase/sections/` (Phase 3 adds: `ContainersSection.kt`, `OverlaysSection.kt`, `NavigationSection.kt`). Each row uses the table format: 140dp label column on the left, all variants/states arranged horizontally on the right.
- Win11 chrome rule: `undecorated = true` is allowed; `transparent = true` is FORBIDDEN (EXCEPTION_ACCESS_VIOLATION). Applies to showcase Main.kt window AND every DialogWindow opened by AeroDialog/AeroAlertDialog.
- 23-token `AeroColorScheme` is NOT extended — Phase 3 components reuse existing tokens (`glassSurface`, `glassBorder`, `cardBackground`, `panelBackground`, `borderDefault`, `borderSelected`, `buttonHover`, `closeButtonHover`, `titleBarGradientStart/End`, `titleBarText`, `error`, `primary`, etc.). If any new token is genuinely needed during planning, raise it as a deviation, don't silently add.

### Claude's Discretion
- **AeroCard**: padding default, optional `header` slot vs `content`-only, elevation parameter default
- **AeroPanel**: same shape choices as AeroCard but using `glassPanel` modifier (no border, no shadow)
- **AeroDivider**: thickness (1.dp standard), padding around (none vs 8dp), color (`borderDefault`)
- **AeroGroupBox**: label position (top-left inset on the border, mordred-style), inner padding default
- **AeroBreadcrumb**: separator character (`›` recommended; `>`/`/` are alternatives), overflow handling (truncate-middle with ellipsis vs horizontal scroll), click-emit-callback pattern (`onItemClick: (index, item) -> Unit`)
- **AeroTabBar**: visual style (browser-tab vs pill vs underline — mordred reference is browser-style, that's a reasonable default); overflow handling (horizontal scroll + arrow buttons vs single-row truncation); optional close button per tab
- **AeroMenuBar**: top-level item interaction (click-to-open + click-other-item-switches; hover-reveal only after one item already opened); submenu cascade rendering (hover-open-with-delay); Alt+letter mnemonics — DEFERRED if non-trivial (note as v2 enhancement)
- **AeroStatusBar**: API shape (`sections: List<AeroStatusSection>` data-driven vs slot-based `content: @Composable RowScope.() -> Unit`)
- **AeroPopover**: trigger model (anchor-element-clickable vs imperative `expanded` state), optional arrow/caret pointing at anchor (yes/no — recommend yes if cheap)
- **AeroDialog/AeroAlertDialog**: appearance animation (fade-in vs fade+scale vs none — use minimal fade ≤150ms by default)
- **AeroToast** stacked animation: exact ease/timing for shift-up
- **AeroScrollArea**: whether to expose horizontal-scroll variant in v1 (deferable to Claude's discretion if trivial)
- **AeroIconButton role inside Phase 3**: AeroDialog close-X, AeroNotificationBanner close-X, AeroTitleBar Min/Max/Close use AeroIconButton or inline custom (mordred uses inline; AeroIconButton is also fine — pick whichever cleanly meets the visual)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Mordred reference implementation
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/components/AeroTitleBar.kt` — direct port reference for NAV-01 (FrameWindowScope, WindowDraggableArea, Min/Max/Close button pattern, hover via MutableInteractionSource)
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/ColorScheme.kt` — source of truth for `titleBarGradientStart/End`, `titleBarText`, `closeButtonHover` hex values across the three themes (already ported in Phase 1)
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/GlassModifiers.kt` — `glassEffect`/`glassPanel`/`glassSurface` patterns (already ported)

### Phase 1 — theme & modifiers
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — 23 tokens; AeroBlue/AeroDark/Classic presets; tokens used in Phase 3: `cardBackground`, `panelBackground`, `borderDefault`, `borderSelected`, `glassSurface`, `glassBorder`, `glassHighlight`, `buttonHover`, `closeButtonHover`, `titleBarGradientStart/End`, `titleBarText`, `error`, `primary`, `surface`, `onSurface`, `onBackground`, `labelText`
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` — `AeroTheme {}` provider, `AeroTheme.colors`, `AeroTheme.typography` accessors, `LocalAeroColors`, `LocalAeroTypography`. **Phase 3 adds `LocalScrollbarStyle` provision here.**
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — `Modifier.glassEffect()`, `Modifier.glassPanel()`, `Modifier.glassSurface()` — used by AeroCard, AeroPanel, AeroDialog container, AeroPopover, AeroDrawer, AeroToast, AeroNotificationBanner

### Phase 2 — atomic primitives reused in Phase 3
- `library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt` — `rememberHoverState`, `rememberFocusState`, `rememberPressedState`, `animatedAlpha`, `ANIMATION_DURATION_MS = 150` constant
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdownMenu.kt` — internal `AeroPopupPositionProvider` (Phase 3 makes public, adds `side` param, moves out into shared `popup/` package); `AeroDropdownItem` (Phase 3 reuses pattern for AeroMenuBar items + AeroContextMenu items); `AeroDropdownPopup` (Phase 3 retrofits to use AeroScrollArea internally)
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt` — used by AeroDialog/AeroNotificationBanner close-X, AeroTitleBar control buttons (or inline equivalents)
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt`, `AeroOutlinedButton.kt` — used inside AeroDialog `buttons` slot, AeroAlertDialog default buttons
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroTextArea.kt` — Phase 3 retrofits its `verticalScroll` to `AeroScrollArea`
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` — pattern reference for AeroMenuBar dropdown items, AeroContextMenu Action items (hover, leading icon, trailing shortcut text)

### Showcase — wiring points for Phase 3
- `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` — **modified in Phase 3**: `Window(undecorated = true)`, `WindowState` is exposed, `AeroTitleBar` rendered inside `FrameWindowScope`. Custom resize handles wrap the window content.
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — **modified in Phase 3**: adds `ContainersSection`, `OverlaysSection`, `NavigationSection` calls; mounts `AeroToastHost(toastState)` at the root.
- `showcase/src/main/kotlin/com/mordred/showcase/sections/PlaceholderSection.kt` — pattern for new sections.
- `showcase/src/main/kotlin/com/mordred/showcase/sections/*.kt` — existing six sections (Buttons, Input, Selection, Dropdown, Range, List) follow the 140dp-label + variants-row template that Phase 3 sections must match.

### Requirements & Roadmap
- `.planning/REQUIREMENTS.md` §Containers (CNT-01..06), §Overlays & Notifications (OVL-01..08), §Navigation & Window Chrome (NAV-01..05) — acceptance criteria per requirement
- `.planning/ROADMAP.md` §Phase 3 — five success criteria
- `.planning/PROJECT.md` §Constraints — tech stack reminders; §Key Decisions — locked decisions table

### Project state — known blockers and decisions
- `.planning/STATE.md` §Decisions — Phase 1 entry "AeroTitleBar placed in Phase 3 because it requires `FrameWindowScope` — only available in real window context"; Win11 EXCEPTION_ACCESS_VIOLATION rule (`undecorated = true` only, NEVER `transparent = true`); §Blockers — Aero Snap (HTCAPTION) limitation acknowledged here, Phase 3 documents but does NOT solve.

### Compose Desktop / Foundation references (for downstream agents)
- `androidx.compose.ui.window.DialogWindow` — modal dialog with own OS window (used by AeroDialog/AeroAlertDialog)
- `androidx.compose.ui.window.Popup`, `PopupPositionProvider`, `PopupProperties` — used by AeroPopover, AeroContextMenu, AeroTooltip, AeroMenuBar dropdowns (existing pattern in `AeroDropdownPopup`)
- `androidx.compose.foundation.window.WindowDraggableArea` — used by AeroTitleBar
- `androidx.compose.foundation.v2.VerticalScrollbar`, `HorizontalScrollbar`, `ScrollbarAdapter`, `LocalScrollbarStyle`, `ScrollbarStyle`, `defaultScrollbarStyle` — wrapped by AeroScrollBar
- `androidx.compose.ui.window.FrameWindowScope` — receiver type for AeroTitleBar
- `androidx.compose.ui.window.WindowState`, `WindowPlacement` — used by AeroTitleBar Maximize toggle and showcase resize handles

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (from Phase 1/2)
- **`AeroPopupPositionProvider`** (currently internal in `dropdown/AeroDropdownMenu.kt`): Phase 3 promotes to public API, extends with `side: AeroPopupSide` param + auto-flip. Used by AeroDropdown (Bottom default), AeroPopover (any side), AeroTooltip (Top), AeroMenuBar dropdowns (Bottom).
- **`AeroDropdownPopup`** wrapper (focusable=true, dismissOnBackPress, dismissOnClickOutside, onPreviewKeyEvent for keyboard nav, glass-styled background): pattern reference for AeroMenuBar dropdown menus and AeroContextMenu rendering.
- **`AeroDropdownItem`**: pattern for AeroMenuBar item, AeroContextMenu Action item (hover via buttonHover, optional selected highlight).
- **`InteractionStates.kt` helpers**: `rememberHoverState(source)`, `rememberFocusState(source)`, `animatedAlpha(target)`, `ANIMATION_DURATION_MS = 150`. Phase 3 components use these directly (don't re-implement state collection).
- **`glassEffect` / `glassPanel` / `glassSurface` modifiers**: AeroCard uses `glassEffect`; AeroPanel uses `glassPanel`; AeroPopover/AeroDialog container use `glassSurface` or `glassEffect` (visually equivalent for popup chrome). These auto-read `LocalAeroColors` — no explicit colors arg.
- **`AeroIconButton`**: ready for AeroDialog/AeroNotificationBanner close-X. AeroTitleBar Min/Max/Close can use it OR inline (mordred uses inline; either is fine).
- **`AeroButton` / `AeroOutlinedButton`**: used inside AeroDialog `buttons` slot, AeroAlertDialog default buttons.
- **All theme tokens from Phase 1 are in place** for Phase 3 — no new tokens required.

### Established Patterns
- **All public declarations in `:library` carry `public` modifier** (`explicitApi()`); internal helpers stay `internal`. AeroPopupPositionProvider promotion to public means: rename file to `library/.../popup/AeroPopupPositionProvider.kt`, mark class `public`, add KDoc.
- **Customization via Kotlin `copy()`** (no DSL/builders) — applies to data classes Phase 3 introduces (e.g., `AeroContextMenuItem.Action`, `AeroBannerKind` is enum).
- **Material3 bridged inside `AeroTheme`** — Phase 3 components MAY delegate to M3 primitives (e.g., `BasicAlertDialog` content area, `Text`, `Icon`) where it cleanly fits.
- **Showcase section file structure**: `*Section.kt` in `showcase/src/main/kotlin/com/mordred/showcase/sections/`; row template = 140dp label column + variants in a Row.
- **No transparent windows ever** (Win11 rule). Applies to: showcase Main.kt window, every DialogWindow opened by AeroDialog/AeroAlertDialog.
- **Animations: 150ms `tween(LinearEasing)`** for state transitions; AeroDrawer slide uses 200-250ms (slower, large-area motion); AeroToast appearance can use a slightly slower curve for visual weight.

### Integration Points
- **`Main.kt` window mode change** is the single biggest delta: `Window(...)` → `Window(undecorated = true, state = windowState)`, then `AeroTitleBar(...)` inside `FrameWindowScope`. This must happen as a deliberate plan step (likely first in Phase 3) because all subsequent component visual checks depend on the new window chrome.
- **`AeroTheme` adds `LocalScrollbarStyle` provision** so Foundation `VerticalScrollbar` calls everywhere automatically pick up Aero styling. New plan step in Phase 3.
- **Phase 2 retrofits**: `AeroDropdownPopup` and `AeroTextArea` are edited to use `AeroScrollArea` (or explicit `AeroScrollBar`) once CNT-05/06 land. Plan must explicitly schedule this retrofit so Phase 2 components don't visually regress.
- **`AeroPopupPositionProvider` move + extension** is a refactor that touches `AeroDropdown` and `AeroComboBox` (existing callers). Phase 3 plan must update those call sites to the new public class.
- **Showcase wiring**: `ShowcaseApp.kt` mounts `AeroToastHost(state)` at the root once and adds 3 new section calls (Containers, Overlays, Navigation).

### Component Dependencies Within Phase 3
- AeroToolbar (Phase 2) and AeroIconButton (Phase 2) feed into AeroTitleBar/AeroDialog construction, but AeroTitleBar can also use inline button code (mordred-style) — recommend using AeroIconButton for consistency, decide during planning.
- AeroPopover, AeroContextMenu, AeroTooltip, AeroMenuBar dropdown menus all share `AeroPopupPositionProvider` (refactored from Phase 2).
- AeroScrollBar/AeroScrollArea must land BEFORE AeroDialog/AeroDrawer/AeroPopover that have scrollable content (and before retrofitting AeroDropdownPopup/AeroTextArea).
- AeroTitleBar must land BEFORE the showcase window-mode change so the new Main.kt has something to render in the title position (otherwise the showcase visually breaks during incremental development).

</code_context>

<specifics>
## Specific Ideas

- **Stacked toast lifecycle is the trickiest piece** — needs a small spike during planning. Each toast in the stack has its own dismiss timer; on each tick we re-derive each toast's `position-in-stack` from list index; new toasts insert at index 0 and animate in from below; dismissed toasts animate out (slide right or fade) and shift remaining ones down via `animateOffsetAsState`. Material3 `SnackbarHostState` queues one-at-a-time, so we can't directly copy that — implementation must drive a `mutableStateListOf<AeroToastData>` plus per-toast LaunchedEffect for the dismiss timer.
- **`undecorated=true` + `transparent=false` validation** — STATE.md notes a Phase 1 blocker to "validate whether Win11 undecorated+transparent crash is fixed in CMP 1.10.3" — that validation can be folded into Phase 3 (since we're on CMP 1.7.3, the answer is "still crashes; we use undecorated alone"). NOTE: project is on CMP 1.7.3 per Phase 1 decisions, NOT 1.10.3 — RESEARCH.md cited 1.10.3 incorrectly.
- **`WindowDraggableArea` does not pass HTCAPTION** — known limitation, drives the Aero-Snap-deferred decision. Document in `AeroTitleBar` KDoc with a link from PROJECT.md Out-of-Scope.
- **Mordred's AeroTitleBar uses fixed 32dp height + 46dp button width** — port these as defaults; allow override via parameter only if a real use case appears during showcase wiring.
- **`closeButtonHover` token is red (`Color(0xFFE81123)`) across all three themes** — Microsoft's standard close-button hover color; reused unchanged.
- **DialogWindow + glass styling**: when `undecorated = true` is applied to DialogWindow, the dialog's window chrome disappears. The AeroDialog content must therefore handle its own visual containment (rounded corners, shadow, glass background). `Modifier.glassEffect()` already does this.

</specifics>

<deferred>
## Deferred Ideas

- **Aero Snap via JNI/HTCAPTION** — would need native code, breaks platform-neutral JAR — explicitly v2+. Add to `PROJECT.md` Out-of-Scope.
- **Top/Bottom AeroDrawer variants** — v1 ships only `Start`/`End`. If a use case appears, Top/Bottom can be added later without breaking the API (extend the enum).
- **Alt+letter mnemonics in AeroMenuBar** — likely deferred unless trivially achievable on Compose Desktop. Note as a v2 enhancement.
- **Horizontal AeroScrollArea variant** — Claude's discretion in v1; if not trivial, defer to v2.
- **AeroPopover arrow/caret pointing at anchor** — Claude's discretion; if cheap, include in v1, otherwise defer.
- **AeroDataTable, AeroTreeView, AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker, AeroColorPicker, AeroRangeSlider** — already declared v2 in REQUIREMENTS.md (CMPLX-01..08); not in Phase 3 scope.
- **AeroAccordion, AeroSplitPane, AeroSidebar, AeroStepperWizard** — already declared v2 in REQUIREMENTS.md (ADVL-01..04); not in Phase 3 scope.

</deferred>

---

*Phase: 03-composite-navigation*
*Context gathered: 2026-04-28*
