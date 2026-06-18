# Phase 10: Layout - Context

**Gathered:** 2026-06-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Доставить четыре **публичных** layout-компонента, построенных на Phase 7 primitives, строго additive к v1.x API:

1. **`AeroAccordion`** (LAYO-01, LAYO-02) — список сворачиваемых секций; `mode = single | multi` (default `multi`); раскрытие/свёртка анимированы; индикатор `AeroIcons.{CaretRight,CaretDown}` в заголовке
2. **`AeroSplitPane`** (LAYO-03, LAYO-04) — две панели (`start`, `end`) с draggable splitter'ом; `orientation = horizontal | vertical`; clamp по `minFirstPaneSize`/`minSecondPaneSize`; расширенный hit-area + resize-курсор; N-pane через вложенность caller'ом
3. **`AeroSidebar`** (LAYO-05, LAYO-06, LAYO-07) — persistent боковая навигация (НЕ overlay, рядом с `AeroDrawer`); режимы `expanded | collapsed | hidden`; анимация ширины; tooltip в collapsed; active-подсветка; composable-slot items
4. **`AeroStepperWizard`** (LAYO-08, LAYO-09) — линейный шаговый процесс; горизонтальный `AeroStepIndicator` (Phase 7); per-step `onValidate` gate; состояние шага сохраняется на Back

Эта фаза уточняет **публичную форму API и UX** этих четырёх компонентов. Внутренняя архитектура (state-lift аккордеона, drag-механика, clamp, onValidate-gate, lifted expansion) уже **зафиксирована** ROADMAP Phase notes + PITFALLS research (PITFALL-11..14) + Phase 7 primitives. Showcase wiring (`LayoutSection`) — **Phase 11**, не здесь.

**Out of scope для Phase 10 (locked / handled elsewhere):**
- `LayoutSection` / любая showcase-секция v2.0 — Phase 11
- **StepperWizard branching (non-linear)** — только линейный проход; branching отложен (STEP-BR-01)
- **AeroSidebar drag-to-resize ширины** — фиксированные ширины expanded/collapsed; ручная регулировка отложена (SIDE-RES-01)
- **AeroSplitPane как рекурсивный N-pane в API** — публичный API 2-pane; N-pane достигается вложенностью caller'ом (locked в v2.0 scoping)
- **AeroStepIndicator vertical orientation** — horizontal-only locked (07-CONTEXT.md); vertical отложен до v2.x
- Pickers (Phase 8, завершена), Data (Phase 9, завершена)
- Любое изменение v1.x публичных компонентов — v2.0 строго additive

</domain>

<decisions>
## Implementation Decisions

### AeroAccordion — API & UX
- **Декларация секций = data-list.** `sections: List<AeroAccordionSection>`, где секция = `(title: String, leadingIcon: ImageVector? = null, content: @Composable () -> Unit)`. Совпадает с `columns: List<AeroTableColumn>` (Phase 9) — единый паттерн фазы, тривиально тестируемо, без DSL-машинерии. (DSL и content-slot scope отклонены как избыточные для простого списка секций.)
- **Владение состоянием раскрытия = hybrid.** Uncontrolled по умолчанию (внутренний state + опц. `initiallyExpanded`), переключается в controlled при передаче явного параметра раскрытия + `onExpandedChange`. **Переключение uncontrolled↔controlled обязано быть явно задокументировано в KDoc** (как hybrid-sort в Phase 9), иначе executor «упростит» в одну ветку.
  - В `mode = single` controlled-параметр — `expandedIndex: Int?`; в `mode = multi` — `expandedIndices: Set<Int>`. State поднят в родитель (`AeroAccordion`), секция получает `expanded: Boolean` + `onToggle: () -> Unit` (PITFALL-13). Планировщик уточняет точную сигнатуру controlled-параметра под два режима.
- **Заголовок секции = `title` + опц. `leadingIcon` + библиотечная каретка.** `title: String` + опциональная `leadingIcon: ImageVector?` слева; библиотека сама дорисовывает caret-индикатор справа (`AeroIcons.{CaretRight,CaretDown}`, rotate 0°→90° через `graphicsLayer { rotationZ }`). Типовой Win7-expander вид; placement каретки — библиотека, не caller. (Полный header-слот отклонён — потеря единого Aero-вида заголовков.)
- **Анимация = `animateContentSize()` ~150–180ms** + caret rotate через `animateFloatAsState` → `rotationZ`. Foundation `animateContentSize` на контенте (НЕ `animateIntAsState(maxHeight)` — PITFALLS §perf: лишние layout-проходы и jank). Стартовая точка для отдельного per-animation sign-off (конвенция проекта).

### AeroSplitPane — API & divider
- **Позиция разделителя = fraction `0f..1f`.** `initialSplitFraction: Float = 0.5f`; внутри хранится px для clamp-математики (PITFALL-14). Responsive к ресайзу окна (держит пропорцию). (Dp-offset отклонён — не держит пропорцию; sealed `Fraction|Dp` отклонён как избыточный для одного разделителя.)
- **Владение позицией = uncontrolled + опц. `onSplitChange`.** Внутренний state + `initialSplitFraction` + опциональный `onSplitChange: (Float) -> Unit`. Split position — внутреннее UI-состояние; drag не обязывает caller'а держать state и применять его каждый кадр. (Сознательно НЕ hybrid как Accordion — controlled-drag слал бы callback каждый кадр; для непрерывного drag uncontrolled чище.)
- **Визуал разделителя = 1dp линия + grip-насечки.** Тонкая `borderDefault`-линия + по центру короткий grip (3–4 точки/насечки), ярче при hover. Узнаваемый desktop-splitter, ложится на Aero-эстетику (project memory: aero-aesthetic-priority — Win7, не generic-flat). Видимая линия 1dp, hit-area невидимо расширена до ~8–12dp (LAYO-04). (Широкая glass-grip полоса отклонена — съедает место между панелями.)
- **Клавиатурный nudge стрелками = Claude's Discretion.** Не обязателен по LAYO-03/04. Добавить, если ложится чисто (focusable splitter + `onPreviewKeyEvent`, ~4dp/нажатие — PITFALL-14 упоминает), иначе отложить в v2.x. Держит scope узким.

### AeroSidebar — API & state
- **Управление режимом + ширина = `rememberAeroSidebarState`.** `rememberAeroSidebarState(initialMode)` держит `var mode: AeroSidebarMode` (read/write) + `val currentWidthDp: State<Dp>` (анимируется через `animateDpAsState`). `AeroSidebar(state) { /* items */ }`; caller переключает `state.mode`, соседний layout читает `state.currentWidthDp` (PITFALL-11 — ровно требуемый контракт). Идиома `DrawerState`/`ScaffoldState`. (Controlled `mode`-param + `onWidthChange` отклонён — ширина через callback неудобна для драйва соседнего layout; internal+встроенный toggle отклонён — `currentWidthDp` наружу не вытащить, ломает PITFALL-11.)
- **Структура = опц. `header` + `footer` слоты.** `header: @Composable? = null` (лого/титул сверху) + `footer: @Composable? = null` (settings/профиль снизу), items между. Обычная desktop-nav компоновка. (Полный content-слот отклонён — библиотека потеряла бы контроль над collapsed-выравниванием.)
- **Группировка = `section(label)` + `divider()` в scope.** Кроме `item(icon, label, selected, onClick)` в `AeroSidebarScope` есть `section(label)` (заголовок группы, скрывается/сжимается в collapsed) и `divider()`. Покрывает реальные nav с разделами. (Плоский список отклонён — менее выразительно для секционированной навигации.)
- **Active/selected визуал = accent-bar + gradient fill.** Левая вертикальная primary-полоска (~3dp) + мягкая glass-градиентная подсветка строки. Узнаваемый Win7-explorer/nav вид; полоска остаётся видна и в collapsed (где строка — только иконка). (Full-row Win7-gradient отклонён — в collapsed выглядит как большой цветной квадрат; простой bg-tint отклонён — ближе к generic-flat, чем к Win7.)
- **Идентификация active — per-item `selected: Boolean`.** Из locked-сигнатуры `AeroSidebarScope.item(icon, label, selected, onClick)` (LAYO-07): caller помечает активный item флагом `selected` и обрабатывает `onClick`; встроенного selectedKey-состояния нет (additive позже при спросе).

### AeroStepperWizard — API & flow
- **Декларация шагов = data-list.** `steps: List<AeroWizardStep>`, где шаг = `(label: String, content: @Composable () -> Unit, onValidate, canProceed)`. Согласовано с Accordion data-list и DataTable columns — единый паттерн фазы. (DSL и позиционные слоты отклонены — расхождение с остальными тремя компонентами / хуже для динамического списка и метаданных.)
- **Владение текущим шагом = hybrid.** Uncontrolled по умолчанию (внутренний `currentStep` + опц. `initialStep`) → controlled при передаче `currentStep` + `onStepChange`. Согласовано с Accordion. Controlled-вариант покрывает deep-linking / внешний прыжок на шаг.
- **Кнопочный ряд = встроенные Back/Next/Finish + настраиваемые лейблы.** Библиотека рисует Back/Next; на последнем шаге Next→Finish; лейблы настраиваются (`backLabel`/`nextLabel`/`finishLabel`). AeroButton-стиль из коробки. (Кастомный slot кнопок и «встроенные + опц.slot» отклонены — больше кода/поверхности API для типового случая; кастомизация лейблов покрывает 90%.)
- **Валидация = per-step + `onFinish` (с уточнением размещения — Claude's Discretion).** Зафиксированный принцип (PITFALL-12, не пересматривается): `onValidate: () -> Boolean` вызывается **только** в `onClick` кнопки Next/Finish (никогда в теле composable); `canProceed: Boolean` — caller-driven живой сигнал для enabled-состояния кнопки. Визард несёт `onFinish: () -> Unit` (вызывается на Finish после успешного `onValidate` последнего шага). **Точное размещение** (`onValidate`/`canProceed` per-step в `AeroWizardStep` vs wizard-level по index) — на усмотрение реализации; per-step предпочтительно (локально к шагу, устойчиво к изменению порядка), но планировщик подтверждает при сборке сигнатуры `AeroWizardStep`.

### Claude's Discretion
- **AeroSplitPane:** клавиатурный nudge стрелками (добавить если чисто; иначе v2.x).
- **AeroStepperWizard:** точное размещение `onValidate`/`canProceed` (per-step в `AeroWizardStep` vs wizard-level по index) — принцип «onValidate только на клик, canProceed — живой enabled» зафиксирован; размещение — за реализацией.
- Точные токены/значения: цвет и геометрия grip-насечек splitter'а (в пределах `aeroDragSplitter` контракта и 8–12dp hit-area), ширины sidebar expanded/collapsed (ROADMAP даёт ~240dp/~48dp как ориентир), paddings, indent заголовков аккордеона, точные тайминги/кривые анимаций (каждая — отдельный sign-off).
- Имена/сигнатуры data-классов (`AeroAccordionSection`, `AeroWizardStep`, enum `AeroSidebarMode`/`AeroSplitOrientation`) — читаемость на месте, в рамках конвенций именования.
- Гранулярность планов / wave-split (build order — рекомендация ниже, не жёсткая).
- Дефолтные лейблы кнопок визарда (Back/Next/Finish) и текст tooltip'ов sidebar.

### Build order (рекомендация, не жёсткая)
SplitPane (потребляет `aeroDragSplitter` напрямую, ближе всего к Phase 9 column-resize) → Accordion (state-lift + animateContentSize) → Sidebar (state-object + 3 режима + tooltip + scope DSL) → StepperWizard (потребляет `AeroStepIndicator`, hybrid-навигация + validate-gate). Sidebar и StepperWizard крупнее (slot-scope / step-model); SplitPane и Accordion меньше.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner, executor) MUST read these before planning или implementing Phase 10.**

### Phase 10 source-of-truth (project-level)
- `.planning/ROADMAP.md` §Phase 10: Layout — Goal, 5 success criteria, 5 phase notes (PITFALL-11/12/13/14 + PITFALL-03 reuse). **Каждый success criterion — приёмочный gate.**
- `.planning/REQUIREMENTS.md` §Layout — Accordion / SplitPane / Sidebar / StepperWizard (LAYO-01..09) — полный текст всех девяти требований; §Out of Scope (StepperWizard branching, Sidebar drag-resize, рекурсивный N-pane); §v3+ Future Requirements (STEP-BR-01, SIDE-RES-01)
- `.planning/PROJECT.md` §Current Milestone v2.0 (Advanced Layout feature list — ADVL); §Key Decisions table; §Context — **каждая форма компонента и анимация требует обсуждения/sign-off с пользователем перед реализацией**
- `.planning/STATE.md` §v2.0 Locked Decisions (Accordion both modes; SplitPane 2-pane + nesting; Sidebar persistent alongside AeroDrawer; StepperWizard linear + per-step onValidate; Sidebar drag-resize OUT); §Accumulated Context → Decisions (v2.0 new: `detectDragGestures` banned — `awaitPointerEventScope`/`aeroDragSplitter` only)

### v2.0 research (authoritative — do not re-research what these cover)
- `.planning/research/PITFALLS.md` — **read all Layout entries**:
  - **PITFALL-11** (lines 262–279) — AeroSidebar width animation vs SplitPane: Sidebar НЕ внутри SplitPane-панели (top-level sibling); экспонировать `val currentWidthDp: State<Dp>` из state-объекта sidebar для драйва соседнего layout; KDoc-пример `Row { AeroSidebar(...); content() }`
  - **PITFALL-12** (lines 283–300) — `onValidate` только в `onClick` Next (никогда в теле composable — иначе валидация на каждой рекомпозиции/hover); `canProceed: Boolean` caller-driven для enabled-состояния; `onValidate` = gate, не живой сигнал
  - **PITFALL-13** (lines 304–322) — Accordion expansion state поднят в родитель (`expandedIndex: Int?` single / `expandedIndices: Set<Int>` multi); секция получает `expanded: Boolean` + `onToggle: () -> Unit`; section content = `@Composable () -> Unit` лямбда, НЕ класс (без DSL overhead)
  - **PITFALL-14** (lines 326–343) — SplitPane divider всегда clamp: `(dividerPx + delta).coerceIn(minFirst.toPx(), total - minSecond.toPx())`; экспонировать `minFirstPaneSize: Dp = 48.dp` + `minSecondPaneSize: Dp = 48.dp`; опц. arrow-key nudge 4dp
  - **§Performance traps** (lines 409–410) — SplitPane: `BoxWithConstraints` для измерения + divider в `Dp`-state, НЕ `SubcomposeLayout` на каждый drag; Accordion: `animateContentSize()` / `animateFloatAsState(scaleY)`, НЕ `animateIntAsState(maxHeight)`
  - **§"Looks done but isn't" checklist** (lines 463–466) — 4 Layout-пункта (accordion single-mode закрывает A; SplitPane clamp на краю; sidebar collapse reflow соседнего контента; stepper onValidate НЕ на focus-move) — Phase 11 gate, но проверяются на Phase 10 компонентах
  - **§Anti-patterns table** (lines 376–393) — onValidate в теле composable = Never; per-section expanded state = Never; divider без clamp = Never; `AeroDrawer`-popup паттерн для Sidebar = НЕ (Sidebar = in-layout Box с animated width, не Popup)
- `.planning/research/SUMMARY.md` §Architecture Approach + §Critical Pitfalls + §Phase Ordering
- `.planning/research/ARCHITECTURE.md` — Phase 10 структура компонентов; переиспользование Phase 7 primitives (aeroDragSplitter, AeroStepIndicator)
- `.planning/research/STACK.md` — Kotlin 2.1.21 / CMP 1.7.3 / JDK 17; `animateContentSize`, `animateDpAsState`, `BoxWithConstraints`, `onPreviewKeyEvent` API
- `.planning/research/FEATURES.md` — complexity Phase 10: MEDIUM×3 + SMALL×1

### Phase 7 hand-off (primitives this phase consumes)
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — **`internal fun Modifier.aeroDragSplitter(orientation, onDrag: (deltaPx: Float) -> Unit, onDragEnd, enabled)`** — divider drag для AeroSplitPane; `Orientation.Horizontal` → `cur.x - prev.x` (E_RESIZE_CURSOR), `Orientation.Vertical` → `cur.y - prev.y` (N_RESIZE_CURSOR); `change.consume()` только при ненулевой дельте; `enabled=false` отключает (gate на min/max позиции). Locked `awaitPointerEventScope` паттерн (PITFALL-03 defused). **Никакого нового drag-кода.**
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` — **`internal fun AeroStepIndicator(currentStep, totalSteps, modifier, onStepClick?)`** — горизонтальный индикатор для AeroStepperWizard; состояния Current (filled primary + номер) / Completed (primary@0.6f + `AeroIcons.Check`) / Upcoming (outlined `labelText`); 2dp коннекторы (primary completed-side / borderDefault upcoming-side); AeroDark-контраст проверен. **Surface-less** (glass — ответственность consumer'а, как в Phase 7 scratch). 0-based `currentStep`.
- `.planning/phases/07-shared-internal-primitives/07-CONTEXT.md` + `07-SUMMARY.md` — locked API + что реально приземлилось (aeroDragSplitter — Modifier, НЕ content-slot; 1D `Float` delta; AeroStepIndicator horizontal-only, surface-less, pinned width pattern)

### Existing codebase — v1.x компоненты, которые Phase 10 переиспользует
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroTooltip.kt` — **tooltip для collapsed-режима sidebar** (LAYO-05): hover-метка с задержкой рядом с иконкой
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDrawer.kt` — **reference-контраст**: Sidebar — НЕ overlay/Popup (как Drawer), а persistent in-layout `Box` с animated width (PITFALLS anti-pattern: не копировать FullWindowPositionProvider)
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` (+ AeroOutlinedButton) — кнопки Back/Next/Finish визарда; Next как primary, Back как outlined (ориентир)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — токены: `primary`/`onPrimary` (active sidebar, current step, accent-bar), `borderDefault` (splitter линия, accordion divider, connector upcoming-side), `labelText` (sidebar collapsed/disabled контраст, step upcoming), `buttonHover` (hover overlay)
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — `Modifier.glassPanel`/`glassEffect` для accordion-заголовков, sidebar-фона, active-row gradient fill (Aero-эстетика)
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — `AeroIcons.{CaretRight,CaretDown}` (accordion-индикатор, rotate через `graphicsLayer { rotationZ }`); `AeroIcons.Check` (уже в AeroStepIndicator)

### Compose / platform reference
- `androidx.compose.animation.animateContentSize` — accordion expand/collapse (НЕ `animateIntAsState(maxHeight)` — PITFALLS perf trap)
- `androidx.compose.animation.core.animateDpAsState` — sidebar width transition (3 режима) + `animateFloatAsState` для caret rotate
- `androidx.compose.foundation.layout.BoxWithConstraints` — SplitPane initial measurement (НЕ `SubcomposeLayout` на каждый drag — PITFALLS perf trap)
- `androidx.compose.ui.graphics.graphicsLayer { rotationZ = ... }` — поворот accordion-каретки 0°→90°
- `androidx.compose.ui.input.key.onPreviewKeyEvent` — опц. arrow-key nudge splitter'а (Claude's Discretion)

### Упреждающие правила (применяются ко всем v2.0 фазам)
- **W11-01** — никаких `Dialog(transparent=true)`; pre-flight grep `transparent = true` = 0 hits (Layout-компоненты не popup-несущие — кроме sidebar collapsed-tooltip через существующий `AeroTooltip`; grep-gate в плане по конвенции)
- **No `detectDragGestures` для drag** — `aeroDragSplitter` only (PITFALL-03); grep-gate в плане
- **No `AeroScrollArea` regression** — если sidebar/accordion контент скроллится, использовать lazy/scroll по месту, не тащить v1.0 регресс

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`Modifier.aeroDragSplitter` (Phase 7, internal, готов)** — drop-in divider drag для AeroSplitPane; `Orientation.Horizontal/Vertical`, 1D `Float` delta, cursor change, `enabled`-gate на min/max позиции. Никакой новой drag-логики (PITFALL-03 уже defused). Тот же примитив, что column-resize в Phase 9.
- **`AeroStepIndicator` (Phase 7, internal, готов)** — drop-in горизонтальный индикатор для AeroStepperWizard; current/completed/upcoming + коннекторы; surface-less (glass — на визарде), AeroDark-проверен. Визард передаёт `currentStep`/`totalSteps`.
- **`AeroTooltip` (v1.0)** — hover-метка для collapsed-sidebar (LAYO-05); задержка + позиционирование рядом с anchor уже есть.
- **`AeroButton` / `AeroOutlinedButton` (v1.0)** — кнопки Back/Next/Finish визарда; Aero-стиль из коробки.
- **`AeroIcons.{CaretRight,CaretDown}`** — accordion expand/collapse индикатор; tint всегда явный (v1.1 правило).
- **`GlassModifiers` (glassPanel/glassEffect)** — accordion-заголовки, sidebar-фон, active-row gradient fill (Aero-эстетика).
- **`AeroColorScheme` токены** — `primary`/`onPrimary`/`borderDefault`/`labelText`/`buttonHover` дают всю палитру (active, splitter, divider, accent-bar, hover) без новых токенов.

### Established Patterns
- **Controlled `value` + `onValueChange`** во всех v1.x interactive-компонентах. Phase 10 расширяет до **hybrid** (uncontrolled-default → controlled при передаче параметра) для Accordion и StepperWizard; **uncontrolled+callback** для SplitPane; **state-object** (`rememberAeroSidebarState`) для Sidebar (идиома DrawerState/ScaffoldState).
- **data-list декларация** (`columns: List<...>` в Phase 9) → Accordion `sections: List<...>` + StepperWizard `steps: List<...>` следуют тому же паттерну. Sidebar — исключение: composable-slot scope (LAYO-07 locked).
- **State поднят в родитель** для координируемого состояния (PITFALL-13 accordion; аналогично wizard currentStep) — секции/шаги получают параметры, не держат своё состояние.
- **Drag = `awaitPointerEventScope`** (через `aeroDragSplitter`), никогда `detectDragGestures`.
- **`public fun Aero*`** с `explicitApi()`; internal helpers (`aeroDragSplitter`, `AeroStepIndicator`) остаются `internal`. Phase 10 добавляет первый публичный layout-API в `components/layout/`.
- **`modifier: Modifier = Modifier`** последним среди non-callback параметров; callbacks `on<Event>`; цвета из `AeroTheme.colors` внутри composable.
- **Три темы** (AeroBlue/AeroDark/Classic) обязаны рендериться корректно; active/accent/splitter/step-палитра валидируется в AeroDark (самый низкий контраст).

### Integration Points
- Новые публичные компоненты приземляются в **`components/layout/`** пакет (ROADMAP §Phase 10). `components/layout/internal/stepper/AeroStepIndicator.kt` уже существует (Phase 7).
- AeroSplitPane потребляет `components/internal/drag/aeroDragSplitter`; AeroStepperWizard потребляет `components/layout/internal/stepper/AeroStepIndicator`.
- AeroSidebar потребляет `components/overlay/AeroTooltip` (collapsed-метки).
- Никаких новых `AeroColorScheme` токенов — Phase 10 переиспользует существующие 23.
- Никаких новых Gradle-зависимостей.
- Showcase wiring (`LayoutSection`) — Phase 11; Phase 10 ship'ает library-код + unit-тесты только (pure-функции: accordion toggle-логика, split clamp-математика, wizard step-transition + validate-gate — тестируются без Compose).

### Things Phase 10 must NOT touch
- v1.x публичные компоненты (additive milestone only).
- `AeroColorScheme` token list (locked на 23).
- Phase 7 internal primitives (`aeroDragSplitter`, `AeroStepIndicator`) — потреблять как есть, не модифицировать (если только не вскроется баг — тогда отдельное решение).
- `Phase7ScratchSection` lifecycle — его удаление это работа Phase 11.

</code_context>

<specifics>
## Specific Ideas

- **Единый data-list паттерн фазы.** Accordion `sections` + StepperWizard `steps` — plain data-class списки, как `columns` в Phase 9. Sidebar — единственное исключение (composable-slot scope, LAYO-07 locked), потому что nav-пункты выигрывают от прямого composable-контроля (иконка+лейбл+selected per-item).
- **Hybrid-владение для координируемого состояния, uncontrolled для непрерывного.** Accordion (раскрытие) и StepperWizard (текущий шаг) — hybrid (uncontrolled-default, controlled при передаче). SplitPane (позиция при drag) — uncontrolled+callback, т.к. controlled-drag слал бы callback каждый кадр. Это осознанная асимметрия, не недосмотр.
- **Sidebar state-object — ключ к PITFALL-11.** `rememberAeroSidebarState` держит `mode` + анимируемый `currentWidthDp`, чтобы соседний layout (напр. SplitPane рядом) реагировал на collapse/expand без помещения sidebar внутрь SplitPane-панели. Это явный контракт из PITFALLS, не произвольный выбор.
- **Aero-визуал не опционален.** Splitter grip-насечки, sidebar accent-bar + glass-gradient active, accordion glass-заголовки — компоненты обязаны выглядеть как Win7-desktop, не generic-flat grid (project memory: aero-aesthetic-priority). Каждая анимация мягкая/быстрая, но утверждается отдельно перед кодом (конвенция проекта).
- **onValidate — gate, не живой сигнал.** Принцип PITFALL-12 зафиксирован жёстко: `onValidate` вызывается ТОЛЬКО в onClick Next/Finish; `canProceed` — отдельный caller-driven живой Boolean для enabled-кнопки. Executor не должен «вывести» enabled из `onValidate` в теле composable.
- **Sidebar accent-bar виден в collapsed.** Левая 3dp primary-полоска выбрана именно потому, что в collapsed-режиме (только иконки) full-row gradient выглядел бы как большой цветной квадрат, а accent-bar остаётся читаемым маркером активного пункта.

</specifics>

<deferred>
## Deferred Ideas

- **StepperWizard non-linear branching** (step возвращает next-step ID) — v2.0 только линейный; branching отложен (STEP-BR-01), отдельный milestone при спросе.
- **AeroSidebar drag-to-resize ширины** в expanded — фиксированные ~240dp/~48dp; ручная регулировка отложена (SIDE-RES-01).
- **AeroSplitPane рекурсивный N-pane в API** — публичный API 2-pane; N-pane через вложенность caller'ом (locked в v2.0 scoping).
- **AeroStepIndicator vertical orientation** — horizontal-only locked (07-CONTEXT.md); vertical отложен до v2.x при появлении consumer'а.
- **AeroSidebar встроенный selectedKey-state** — v2.0 per-item `selected: Boolean` флаг (LAYO-07); централизованный selectedKey — additive позже при спросе.
- **AeroSplitPane клавиатурный nudge стрелками** — Claude's Discretion на эту фазу (добавить если чисто); иначе формально отложить в v2.x.
- **AeroAccordion полный header-слот** (`@Composable (expanded) -> Unit`) — v2.0 `title`+`leadingIcon`; полный слот — additive позже, если потребуется кастомный заголовок.

</deferred>

---

*Phase: 10-layout*
*Context gathered: 2026-06-18*
