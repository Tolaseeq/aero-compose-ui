# Phase 9: Data - Context

**Gathered:** 2026-06-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Доставить два **публичных** stateful-компонента, построенных на Phase 7 primitives, без breaking changes к v1.x API:

1. **`AeroDataTable<T>`** (DATA-01..04) — виртуализированная таблица: заголовки колонок, виртуализация строк (`LazyColumn` + `LazyListState` + standalone `AeroScrollBar`), трёхпозиционная сортировка по клику на заголовок, выделение строк (`none | single | multi` с Ctrl/Shift), resizable колонки (drag splitter)
2. **`AeroTreeView<T>`** (DATA-05..06) — иерархическое дерево: раскрытие/свёртка узлов, lazy children loading через `onExpand` (ровно один раз на узел независимо от scroll)

Эта фаза уточняет **публичную форму API и UX** этих двух компонентов. Внутренняя архитектура (virtualization, drag-механика, selection-by-key, lazy-callback guard) уже **зафиксирована** Phase 7 + v2.0 milestone decisions + PITFALLS research (см. Canonical References). Showcase wiring (`DataSection`) — **Phase 11**, не здесь.

**Out of scope для Phase 9 (locked / handled elsewhere):**
- **Cell editing / inline editing** — v2.0 read-only render + selection; редактирование — отдельный milestone (DATA-EDIT-01)
- **Column reordering (drag-to-rearrange)** — resize ✓, reorder ✗ (DATA-REORDER-01)
- **Per-column filter UI** в заголовке — sort ✓, фильтрация откладывается; caller сам фильтрует data до передачи (DATA-FILTER-01)
- **TreeView drag-and-drop reordering узлов** — expand + lazy ✓, перетаскивание ✗ (TREE-DND-01)
- `DataSection` / любая showcase-секция v2.0 — Phase 11
- Layout-компоненты (Phase 10), Pickers (Phase 8, завершена)
- Любое изменение v1.x публичных компонентов — v2.0 строго additive

</domain>

<decisions>
## Implementation Decisions

### Column API & sizing (AeroDataTable)
- **Рендер ячейки = composable slot + text-хелпер.** Колонка несёт `cell: @Composable (T) -> Unit` (полная свобода: бейджи, иконки, цветной текст — нужно для SHW-07 status-badge column). Библиотека shipает тонкий convenience-хелпер (`textColumn(...)` или аналог) для частого строкового случая. Самый Compose-идиоматичный вариант; согласуется с тем, как `nodeContent` устроен в дереве.
- **Ширина колонки = sealed `AeroColumnWidth`.** `Fixed(Dp)` | `Weight(Float)` sealed-тип + `minWidth: Dp = 40.dp` на каждую колонку. Type-safe, self-documenting, невозможно одновременно задать fixed и weight. Ложится на DATA-04 точно (минимум предотвращает схлопывание в 0dp при drag-resize).
- **Объявление колонок = `columns: List<AeroTableColumn<T>>`.** Plain data-class список (`AeroTableColumn(header, width, alignment, cell, sortKey?)`), как item-спеки `LazyColumn` — без DSL-машинерии, тривиально тестируемо (PITFALLS favours avoiding DSL overhead, ср. PITFALL-13).
- **Выравнивание + заголовок.** На колонке `alignment: Alignment.Horizontal = Start` (числовые/дата-колонки выравниваются End). `header: String` — библиотека сама дорисовывает caret сортировки рядом с label'ом (placement управляется библиотекой, не caller'ом).

### Sort & selection ownership (AeroDataTable)
- **Сортировка = гибрид (uncontrolled по умолчанию → controlled override).** Колонка несёт опциональный `sortKey: ((T) -> Comparable<*>)?`. Если задан и `onSortChange` НЕ задан — таблица сама пересортировывает отображаемый список при клике (asc → desc → none). Если задан `onSortChange: (column, direction) -> Unit` — таблица отдаёт управление caller'у (серверная сортировка, кастомные компараторы). Покрывает оба сценария; **переключение uncontrolled↔controlled должно быть явно задокументировано в KDoc**, чтобы executor не «упростил» в одну ветку.
- **Старт = `none` (порядок данных).** Таблица стартует без сортировки — строки в порядке прихода; первый клик по заголовку → asc. Предсказуемо, ничего не навязывает. (Опциональный `initialSort` — Claude's Discretion, см. ниже.)
- **Выделение = controlled.** `selectedKeys: Set<Any>` + `onSelectionChange: (Set<Any>) -> Unit`. Совпадает с библиотечным паттерном `value + onValueChange` (AeroSlider, AeroCheckbox). Caller — единый источник правды; ключи стабильны (`key: (T) -> Any`, PITFALL-04) и переживают сортировку.
- **Ctrl/Shift-логика и Shift-anchor — internal.** Таблица считает Ctrl-toggle и Shift-range внутри и эмитит готовый `Set<Any>`. **Shift-anchor (последняя выделенная строка) — приватный internal `remember`**, наружу не торчит; диапазон считается от внутреннего anchor по текущему отображаемому порядку. Самый чистый публичный API.
- **`selectionMode = none | single | multi`** (DATA-03, locked). В `none` клики по строкам не выделяют (но `onRowClick`/`onNodeClick`-семантика возможна как Claude's Discretion).

### TreeView data & expansion model (AeroTreeView)
- **Модель дерева = функции (не node-wrapper).** `rootNodes: List<T>` + `children: (T) -> List<T>` + `isExpandable: (T) -> Boolean` + `key: (T) -> Any`. Библиотека читает `children` по требованию; caller мутирует свои данные в `onExpand` → рекомпозиция показывает их. `isExpandable` рисует caret **до** lazy-загрузки (узел «можно раскрыть», даже если children ещё не пришли). Самый чистый lazy-вариант — под node-wrapper каждая дозагрузка требовала бы перестроить иммутабельное дерево.
- **Раскрытие = internal + `onExpand`.** `expanded` живёт во внутреннем tree-level `SnapshotStateMap<NodeKey, NodeState>` рядом с `childrenLoaded` (PITFALL-05 — карта выше `LazyColumn`, переживает item disposal). Наружу — только `onExpand: (T) -> Unit`, вызывается **ровно один раз** на узел при первом раскрытии. Минимум API, полностью покрывает DATA-05/06. (Controlled `expandedKeys` сознательно НЕ выбран — лишняя ширина; `childrenLoaded` всё равно обязан быть internal.)
- **Содержимое узла = composable slot.** `nodeContent: @Composable (T) -> Unit` + опциональная иконка узла; библиотека владеет caret-индикатором (`AeroIcons.{CaretRight,CaretDown}`, rotate через `graphicsLayer`), отступом по глубине и обработкой клика. Согласуется с cell-slot таблицы.
- **Выделение в дереве = expand-only + `onNodeClick`.** Дерево раскрывает/сворачивает узлы и отдаёт `onNodeClick: (T) -> Unit`; встроенного selection-состояния НЕТ — ровно по требованиям DATA-05/06. Caller сам подсвечивает активный узел через `nodeContent`. (Встроенный single-select сознательно отложен — расширил бы scope за DATA-05/06.)

### Shared UX, states & animation
- **Высота строк = фиксированная.** `rowHeight: Dp = 36.dp` по умолчанию (плотный desktop-grid), caller может переопределить. Предсказуемая высота item'а = стабильная виртуализация и scrollbar-математика; применяется и к строкам таблицы, и к узлам дерева. (density-enum отклонён как избыточный; content-based высота отклонена — ломает выравнивание колонок.)
- **Пустые состояния = слот + дефолт.** `emptyContent: (@Composable () -> Unit)? = null`; если `null` — библиотека рисует скромный центрированный текст («Нет данных» / «Нет элементов»). Покрывает и быстрый старт, и кастомизацию. Применяется к таблице (нет строк) и дереву (нет корней).
- **Визуал = Aero (глянец/градиент/глубина).** Header-строка с glass-градиентом (`glassPanel` / светлый верхний блик, как в Win7-заголовках списков); между строками тонкий divider (`borderDefault` на низком alpha), вертикальные разделители колонок. Точные токены — на этапе реализации (Aero-aesthetic alignment — см. project memory: лидирует Win7 Aero, не generic-flat).
- **Анимация = мягко и быстро (Aero-направление).** Стартовая точка для per-animation sign-off (по конвенции проекта каждая анимация утверждается отдельно):
  - Раскрытие узла дерева: `expandVertically` / `animateContentSize` ~150–180ms
  - Caret-поворот 0°→90°: `animateFloatAsState` → `rotationZ`
  - Sort-индикатор + hover/selection highlight: мягкий color/alpha crossfade ~80–100ms
  - **Drag-resize колонок: live, без анимации** (drag обязан отвечать на первый пиксель — PITFALL-03)
  - Планировщик ставит touchpoint sign-off перед кодом анимаций.

### Claude's Discretion
- Точное имя/сигнатура text-convenience хелпера колонки (`textColumn(...)` vs `AeroTableColumn.text(...)`) — читаемость на месте.
- Опциональный `initialSortColumn`/`initialSortDirection` параметр (старт с предотсортированной колонкой) — добавить, если читается чисто; дефолт `none` зафиксирован.
- Точные значения `indentPerLevel` дерева, paddings, токены glass-header и divider'ов, ширина hit-area drag-splitter'а колонок (в пределах `aeroDragSplitter` контракта).
- `onRowClick` для таблицы в режиме `selectionMode = none` (аналог `onNodeClick`) — добавить, если естественно.
- Гранулярность планов / wave-split (build order: DataTable → TreeView предпочтителен, но не жёсткий).
- Дефолтный текст empty-state и его точная типографика.

### Build order (рекомендация, не жёсткая)
DataTable (LARGE: virtualization + sort + selection + resize) → TreeView (MEDIUM: lazy-expand). DataTable первой, т.к. она потребляет `aeroDragSplitter` напрямую и закрепляет четырёх-state selection-палитру; TreeView переиспользует `AeroScrollBar`-паттерн и caret-иконки.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner, executor) MUST read these before planning или implementing Phase 9.**

### Phase 9 source-of-truth (project-level)
- `.planning/ROADMAP.md` §Phase 9: Data — Goal, 5 success criteria, 6 phase notes (PITFALL-01/03/04/05/10 + horizontal-scroll/no-stickyHeader rule)
- `.planning/REQUIREMENTS.md` §Data — Table + Tree (DATA-01..06) — полный текст всех шести требований; §Out of Scope (cell editing / column reorder / column filter / TreeView DnD); §v3+ Future Requirements (DATA-EDIT/REORDER/FILTER-01, TREE-DND-01)
- `.planning/PROJECT.md` §Current Milestone v2.0 (AeroDataTable + AeroTreeView feature list); §Key Decisions table; §Context — **каждая форма компонента и анимация требует обсуждения/sign-off с пользователем перед реализацией**
- `.planning/STATE.md` §v2.0 Locked Decisions; §Accumulated Context → Decisions (v2.0 new: `AeroScrollArea` banned inside DataTable/TreeView; selection `Set<RowKey>` + `key`; `detectDragGestures` banned); §Pending Todos — **Phase 9 research: `rememberScrollbarAdapter(LazyListState)` API surface в CMP 1.7.3** (open question для researcher)

### v2.0 research (authoritative — do not re-research what these cover)
- `.planning/research/PITFALLS.md` — **read all DataTable/TreeView entries**:
  - **PITFALL-01** (silent showstopper) — LazyColumn в AeroScrollArea убивает виртуализацию; DataTable владеет своим `LazyColumn` + `LazyListState` + standalone `AeroScrollBar(rememberScrollbarAdapter(lazyListState))`; outer container даёт bounded height
  - **PITFALL-03** — touchSlop=18dp; column resize использует `Modifier.aeroDragSplitter` (Phase 7); никаких новых `detectDragGestures`
  - **PITFALL-04** — selection-by-index протухает после сортировки; API = `Set<RowKey>` + `key: (T) -> Any`, locked в plan-01 **до** любой реализации
  - **PITFALL-05** — TreeView lazy-callback повтор; `childrenLoaded` в tree-level `SnapshotStateMap`, не в node-композаблах
  - **PITFALL-10** — selection vs hover token; selected = `colors.borderSelected.copy(alpha = 0.15f)`, hover-on-selected = stacked `buttonHover` сверху → четыре читаемых state (normal/hover/selected/selected+hover); валидировать в AeroDark
  - **Performance traps** — `items(key = keyFn)` обязателен; ширины колонок в `remember(columns, data){}`, не на каждой row-рекомпозиции
  - **"Looks done but isn't" checklist** — 6 DataTable/TreeView пунктов (virtualization row-count, selection-after-sort, column-resize reflow, lazy-callback no-refire, no `transparent=true`, no `AeroScrollArea`) — Phase 11 gate, но проверяются на Phase 9 компонентах
- `.planning/research/SUMMARY.md` §Architecture Approach + §Critical Pitfalls + §Phase Ordering
- `.planning/research/ARCHITECTURE.md` — Phase 9 структура компонентов; переиспользование Phase 7 primitives + существующей scroll/glass инфраструктуры
- `.planning/research/STACK.md` — Kotlin 2.1.21 / CMP 1.7.3 / JDK 17; `rememberScrollbarAdapter(LazyListState)` overload
- `.planning/research/FEATURES.md` — complexity: AeroDataTable = LARGE, AeroTreeView = MEDIUM

### Phase 7 hand-off (primitives this phase consumes)
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — **`internal fun Modifier.aeroDragSplitter(orientation, onDrag: (deltaPx: Float) -> Unit, onDragEnd, enabled)`** — column-resize drag для DataTable; `Orientation.Horizontal` → reports `cur.x - prev.x`; cursor → `E_RESIZE_CURSOR`; `change.consume()` только при ненулевой дельте; `enabled=false` отключает (gate на min/max ширине). Locked `awaitPointerEventScope` паттерн.
- `.planning/phases/07-shared-internal-primitives/07-CONTEXT.md` + `07-SUMMARY.md` — locked API + что реально приземлилось (aeroDragSplitter — Modifier, НЕ content-slot composable; 1D `Float` delta; orientation выбирает ось)

### Existing codebase — v1.x компоненты, которые Phase 9 переиспользует
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollBar.kt` — **standalone scrollbar для DataTable/TreeView**; принимает adapter через `rememberScrollbarAdapter(lazyListState)`. Пара к собственному `LazyColumn` (НЕ `AeroScrollArea`)
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollArea.kt` — **ЗАПРЕЩЕНО внутри DataTable/TreeView** (PITFALL-01) — `Column + verticalScroll` ломает lazy. Reference только чтобы знать, чего НЕ делать; grep-gate в плане
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` — reference для selection/hover паттерна; **НО** DataTable использует `borderSelected.copy(0.15f)`, НЕ `primary.copy(0.2f)` как AeroListItem (PITFALL-10)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — токены: `borderSelected` (selection bg), `buttonHover` (hover overlay, stacked), `labelText` (disabled в AeroDark — PITFALL-09 при наличии disabled-строк), `borderDefault` (row/column divider), `primary`/`onPrimary`
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — `Modifier.glassPanel(cornerRadius = ...)` для glass-градиентного header'а таблицы
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — `AeroIcons.{CaretUp, CaretDown}` (sort-индикатор, DATA-02), `AeroIcons.{CaretRight, CaretDown}` (tree expand/collapse, rotate через `graphicsLayer { rotationZ }`)

### Compose / platform reference
- `androidx.compose.foundation.lazy.LazyColumn` + `rememberLazyListState()` — собственный список таблицы/дерева; `items(items = ..., key = { keyFn(it) })` (perf trap без key)
- `androidx.compose.foundation.rememberScrollbarAdapter(LazyListState)` — Compose Desktop Foundation overload; **researcher: verify API surface в CMP 1.7.3** (STATE.md pending todo)
- `androidx.compose.foundation.ScrollState` + `Modifier.horizontalScroll` — общий horizontal scroll для header `Row` и data `LazyColumn` (скроллятся вместе); **НЕ `stickyHeader`** (JetBrains bugs #3016, #2940)
- `androidx.compose.runtime.snapshots.SnapshotStateMap` — tree-level expanded + childrenLoaded (PITFALL-05)
- `androidx.compose.ui.graphics.graphicsLayer { rotationZ = ... }` — поворот caret-иконки дерева

### Упреждающие правила (применяются ко всем v2.0 фазам)
- **W11-01** — никаких `Dialog(transparent=true)`; pre-flight grep `transparent = true` = 0 hits (DataTable/TreeView не popup-несущие, но grep-gate в плане по конвенции)
- **No `AeroScrollArea` inside DataTable/TreeView** — grep-gate = 0 hits (PITFALL-01)
- **No `detectDragGestures` для drag** — `aeroDragSplitter` only (PITFALL-03)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`Modifier.aeroDragSplitter` (Phase 7, internal, готов)** — drop-in column-resize drag для DataTable; `Orientation.Horizontal`, 1D `Float` delta, cursor change, `enabled`-gate на min-width. Никакой новой drag-логики писать не нужно (PITFALL-03 уже defused).
- **`AeroScrollBar` (v1.0)** — standalone Aero-styled scrollbar; паруется с собственным `LazyListState` через `rememberScrollbarAdapter`. Это **правильная** замена `AeroScrollArea` для lazy-списков (PITFALL-01).
- **`AeroIcons.{CaretUp,CaretDown,CaretRight}`** — sort-индикатор таблицы + expand-индикатор дерева; tint всегда явный (v1.1 правило).
- **`GlassModifiers.glassPanel`** — glass-градиентная header-строка таблицы (Aero-эстетика).
- **`AeroColorScheme` токены** — `borderSelected` / `buttonHover` / `borderDefault` / `labelText` дают четырёх-state selection-палитру (PITFALL-10) без новых токенов.
- **`AeroListItem`** — визуальный reference для строки (НЕ копировать его selection-токен).

### Established Patterns
- **Controlled `value` + `onValueChange`** во всех v1.x interactive-компонентах → таблица следует: `selectedKeys` + `onSelectionChange` (и опц. `onSortChange`).
- **`public fun Aero*`** с `explicitApi()`; internal helpers остаются `internal`. Phase 9 добавляет первый публичный DataTable/TreeView API.
- **`modifier: Modifier = Modifier`** последним среди non-callback параметров; callbacks `on<Event>`; цвета из `AeroTheme.colors` внутри composable.
- **Canvas/pointer drag = `awaitPointerEventScope`** (через `aeroDragSplitter`), никогда `detectDragGestures`.
- **Три темы** (AeroBlue/AeroDark/Classic) обязаны рендериться корректно; selection-палитра валидируется в AeroDark (самый низкий контраст токенов).

### Integration Points
- Новые публичные компоненты приземляются в **`components/datatable/`** пакет (ROADMAP §Phase 9). `AeroTreeView` — там же или в соседнем пакете (планировщик решает; ROADMAP называет `components/datatable/`).
- DataTable потребляет `components/internal/drag/aeroDragSplitter` + `components/containers/AeroScrollBar` напрямую.
- Никаких новых `AeroColorScheme` токенов — Phase 9 переиспользует существующие 23.
- Никаких новых Gradle-зависимостей — `kotlinx-datetime` уже на classpath (Phase 7/8), но Phase 9 его, скорее всего, не использует напрямую (date-колонки рендерит caller через cell-slot).
- Showcase wiring (`DataSection`) — Phase 11; Phase 9 ship'ает library-код + unit-тесты только.

### Things Phase 9 must NOT touch
- v1.x публичные компоненты (additive milestone only).
- `AeroColorScheme` token list (locked на 23).
- `AeroScrollArea` — НЕ использовать внутри DataTable/TreeView (PITFALL-01), не модифицировать его.
- `Phase7ScratchSection` lifecycle — его удаление это работа Phase 11.

</code_context>

<specifics>
## Specific Ideas

- **Гибридная сортировка — осознанный выбор.** По умолчанию таблица сортирует сама по `sortKey` колонки (минимум кода у потребителя для типового грида), но наличие `onSortChange` переводит её в controlled-режим (серверная сортировка / кастомные компараторы). Это намеренно шире, чем чисто-uncontrolled или чисто-controlled — но **переключение обязано быть явным в KDoc**, иначе executor «упростит» в одну ветку и потеряет сценарий.
- **Выделение — строго by-key и controlled, но Ctrl/Shift-механика спрятана.** Публичный API видит только `selectedKeys: Set<Any>` ↔ `onSelectionChange`. Anchor для Shift-range — приватный internal `remember`; наружу не торчит. Это держит API маленьким при полноценной desktop-multiselect механике (PITFALL-04 + range-from-anchor).
- **Дерево описывается функциями, не деревом-обёрткой.** `children`/`isExpandable`/`key` поверх `rootNodes` — потому что lazy-загрузка мутирует данные caller'а; иммутабельный `AeroTreeNode<T>` заставлял бы перестраивать обёртки на каждую дозагрузку. `isExpandable` отдельно от `children`, чтобы рисовать caret **до** загрузки детей.
- **Дерево — expand-only, без встроенного selection.** Ровно по DATA-05/06. `onNodeClick` отдаёт клик, подсветку активного узла рисует caller через `nodeContent`. Встроенный single-select — чистый additive-параметр позже, если попросят.
- **Aero-визуал не опционален.** Glass-градиентный header + тонкие разделители — таблица обязана выглядеть как Win7-список, не как generic-flat grid (project memory: aero-aesthetic-priority). Анимации мягкие/быстрые, но каждая утверждается отдельно перед кодом (стандартная конвенция проекта).
- **rowHeight фиксированная.** Предсказуемая высота item'а — это и стабильная виртуализация, и корректная scrollbar-математика, и выравнивание колонок. content-based высота сознательно отклонена.

</specifics>

<deferred>
## Deferred Ideas

- **DataTable cell editing (inline / popup)** — v2.0 read-only; редактирование — отдельный milestone при consumer-запросе (DATA-EDIT-01).
- **Column reordering (drag-to-rearrange)** — resize ✓, reorder отложен (DATA-REORDER-01).
- **Per-column filter UI** в заголовке — sort ✓; фильтрация откладывается, caller фильтрует data сам (DATA-FILTER-01).
- **TreeView drag-and-drop reordering узлов** — отложено (TREE-DND-01).
- **TreeView встроенный single/multi-select** — v2.0 expand-only + `onNodeClick`; selection — additive-параметр позже, если появится спрос.
- **DataTable density-enum (compact/standard/comfortable)** — v2.0 единый `rowHeight`-параметр; пресеты плотности можно добавить позже.
- **Sticky первая колонка / frozen columns** — горизонтальный скролл общий; freeze-колонки не в scope v2.0.

</deferred>

---

*Phase: 09-data*
*Context gathered: 2026-06-18*
