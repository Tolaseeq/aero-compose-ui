# aero-compose-ui

## What This Is

Библиотека UI-компонентов для Compose Desktop, выдержанная в визуальном стиле Windows Aero (Windows 7): стеклянные градиентные поверхности, полупрозрачные панели, анимации и кастомная оконная хромка. Публикуется как Maven/JAR-артефакт (`com.mordred:aero-compose-ui`), подключается через Gradle. Содержит ~50 компонентов, типизированный набор из 138 векторных иконок `AeroIcons` (порт Phosphor Regular), и три встроенные темы с поддержкой кастомизации цветов.

## Core Value

Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна, типизированным набором иконок и демо-витриной — без необходимости реализовывать стиль или искать совместимый icon pack самостоятельно.

## Current State (Shipped: v2.0.2 AeroPanelGroup, 2026-06-23)

- **v1.0 MVP** (3 фазы, 53 требования): Foundation + Atomic Components + Composite/Navigation. Все компоненты библиотеки реализованы, три темы работают, showcase демонстрирует всё.
- **v1.1 Icon System** (3 фазы, 17 требований): 138 векторных иконок `AeroIcons` (порт Phosphor Regular) заменили все текстовые символы и `Icons.Outlined.*`; `compose.materialIconsExtended` удалён из Gradle dependency graph; showcase содержит `IconsSection` с поиском.
- **v2.0 Stateful + Layout** (5 фаз 7–11, 27 требований): 12 новых компонентов (8 complex stateful + 4 advanced layout) поверх внутреннего фундамента primitives. Pickers (`AeroDatePicker`/`TimePicker`/`DateTimePicker`/`DateRangePicker`/`ColorPicker`/`RangeSlider`), Data (`AeroDataTable` виртуализованный + `AeroTreeView` lazy), Layout (`AeroAccordion`/`SplitPane`/`Sidebar`/`StepperWizard`). `kotlinx-datetime:0.6.2` — единственная новая зависимость. Showcase: DataSection + PickersSection + LayoutSection; 16-item × 3-theme sign-off PASSED. No breaking changes к v1.x API.
- **v2.0.1 Picker & SplitPane Fixes** (1 фаза 12, 18 требований): patch milestone — два bug-фикса + один аддитивный компонент. `AeroDateTimePicker` теперь показывает секунды в триггере при `showSeconds=true` (FIXDT); вложенный N-pane `AeroSplitPane` перетаскивается без snap-back и без краша (FIXSP, fraction-based divider state + inverted-range clamp guard); новый `AeroDateTimeRangePicker` — Apply-gate dual-calendar picker, emits `(LocalDateTime, LocalDateTime)` (DTR). Zero new dependencies, no breaking changes. Three-theme showcase sign-off PASSED.
- **v2.0.2 AeroPanelGroup** (2 фазы 13 + 13.1, 18 + 1 требований): аддитивный layout-компонент `AeroPanelGroup` (+ `AeroPanelSection` через scope-DSL) — N секций заполняют родителя, сворачиваются в ~36dp полоску-заголовок (соседи забирают высоту), drag-resize между соседними раскрытыми секциями (модель VS Code Side Bar); fraction-based размеры переживают ресайз окна; гибридный controlled/uncontrolled API по паттерну `AeroAccordion`; Pattern 3 (animate-target vs. drag-write + `isDragging`→`snap()`) разрешает PNL-PITFALL-01; 12 чистых JVM-юнит-тестов без Compose; Win7 Aero визуал (glassPanel, CaretRight 0°→90°, headerActions, grip dots). Вставленная Phase 13.1 добавила горизонтальную ориентацию через общий internal-core `AeroPanelGroupImpl(orientation)` + аддитивный default-param — zero breaking change, zero vertical regression (PNL-HORIZ-01). Three-theme sign-off PASSED на обеих ориентациях. Zero new dependencies.

**Codebase:** Kotlin / Compose Desktop 1.7.3, Kotlin 2.1.21, Gradle 8.14.3, JDK 17. v2.0 added 152 files changed (+27,406 / −2,285) across phases 7–11; v2.0.1 added 9 code files (+520 / −14) in Phase 12; v2.0.2 added 4 code files (+1,516: `AeroPanelGroup.kt` 818, `PanelDistribution.kt` 245, `PanelGroupLogicTest.kt` 235, `LayoutSection.kt` +218) across Phases 13 + 13.1. Project version bumped to `2.0.2` (`build.gradle.kts`).

## Current Milestone: v2.0.3 PanelGroup Recompose Fix

**Goal:** Patch milestone — устранить дублирование секций в `AeroPanelGroup` при сочетании `Orientation.Horizontal` + controlled-режим + рекомпозиция контента секции во время активного drag разделителя. Первопричина — запись наблюдаемого snapshot-state (`expandedState`) в теле композиции, которое читается в той же композиции внутри `BoxWithConstraints` (SubcomposeLayout). Затем релиз v2.0.3 на JitPack. Без breaking changes к v2.x API.

**Target features:**
- **Фикс рекомпозиции:** `expandedArr` для size-математики считается напрямую из `isExpanded(sec)` каждую композицию; sync `expandedState` вынесен в `SideEffect`; seed-блок не мутирует читаемый в той же композиции state. Инвариант: ни один проход композиции `BoxWithConstraints` не пишет в `expandedState`/`sizePx`, которые он же читает.
- **Минимальный repro в showcase** (`LayoutSection.kt`): горизонтальный controlled `AeroPanelGroup`, контент одной секции читает внешний `mutableStateOf`, меняющийся одновременно с drag разделителя — до фикса ловит дубли (N→×N), после фикса N остаётся N.
- **Регрессии:** Vertical-путь (Phase 13) и uncontrolled-режим byte-identical; drag-resize, collapse/expand-анимации (snap() при drag, tween(200ms) после), `onLayoutChange` (drag-end + toggle), сохранение пропорций при resize окна — без изменений. Compose остаётся 1.7.3.
- **Релиз:** bump до `2.0.3` в `build.gradle.kts`, commit, tag `v2.0.3`, push на GitHub → JitPack build.

**Открытые кандидаты на будущие milestone** (НЕ в scope v2.0.3):
- AeroPanelGroup: drag-to-reorder секций (PNL-REORDER-01), вложенные `AeroPanelGroup` как first-class API (PNL-NEST-01), клавиатурный ресайз разделителей (PNL-KBD-01).
- Carry-over: AeroDropdown popup-offset regression (DROP-FIX-01, v1.0).
- Прежний candidate-список: inline pickers, DataTable cell-edit/reorder/filter, TreeView DnD, ColorPicker eyedropper, StepperWizard branching, Sidebar drag-resize, AeroDateTimeRangePicker hover-preview.

<details>
<summary>📦 v2.0.2 AeroPanelGroup — shipped 2026-06-23 (milestone goal & target features)</summary>

**Goal:** Добавить один аддитивный layout-компонент `AeroPanelGroup` (+ `AeroPanelSection`) — вертикальное разбиение на N секций, где любую секцию можно свернуть в полоску-заголовок (соседи забирают освободившуюся высоту), а границу между двумя соседними раскрытыми секциями можно перетаскивать (модель VS Code Side Bar). Без breaking changes к v2.x API.

**Target features (all delivered):**
- `AeroPanelGroup` — контейнер N секций; `BoxWithConstraints → totalPx` (как `AeroSplitPane`). Раскрытые секции делят `availableForExpanded = totalPx − Σ(заголовки) − Σ(разделители)` по нормированным `sizePx` — переживают ресайз окна. ✓
- `AeroPanelSection` (scope-DSL `section(key, title) { content }`) — `expanded`, `sizePx`, `lastExpandedFraction` для возврата размера; `collapsible` / `resizable` флаги. ✓
- **Collapse/expand:** анимация целевых px через `animateFloatAsState` (200ms FastOutSlowInEasing); collapse → ~36dp заголовок, доля переходит соседям; expand → восстанавливает `lastExpandedFraction`. ✓
- **Resize:** грип-разделитель (`aeroDragSplitter` + `clampPanelDividerPx`) ТОЛЬКО между двумя соседними раскрытыми; drag пишет px напрямую без анимации (`isDragging`→`snap()`), кламп по minSize. ✓
- **State API:** раскрытие — гибрид controlled/uncontrolled по паттерну `AeroAccordion`; размеры — uncontrolled, наружу через `onLayoutChange` (drag-end + toggle). ✓
- **Главный риск (PNL-PITFALL-01, разрешён спайком):** совмещение collapse/expand-анимации с drag-ресайзом — Pattern 3 (animate-target vs. direct-write). ✓
- Win7 Aero визуал (glassPanel, CaretRight 0°→90°, `leadingIcon`, `headerActions`); 12 чистых JVM-юнит-тестов; демо в `LayoutSection.kt`; KDoc с REQ-ID + PITFALL. ✓
- **Phase 13.1 (inserted):** горизонтальная ориентация через `AeroPanelGroupImpl(orientation)` + аддитивный `orientation` default-param — zero breaking change, zero vertical regression (PNL-HORIZ-01). ✓

</details>

<details>
<summary>📦 v2.0.1 Picker & SplitPane Fixes — shipped 2026-06-22 (milestone goal & target features)</summary>

**Goal:** Маленький milestone: исправить два известных бага (AeroDateTimePicker не показывает секунды в триггере; AeroSplitPane фризит правый сплиттер при вложенной N-pane компоновке) и добавить один аддитивный компонент `AeroDateTimeRangePicker` — без breaking changes к v2.0 API.

**Target features (all delivered):**
- **Fix — AeroDateTimePicker seconds:** default `formatter` хардкодил `HH:MM` и игнорировал `showSeconds`; введённые секунды коммитились, но не рендерились в триггере. ✓ Fixed via `formatAeroDateTime` helper + nullable-formatter dispatch.
- **Fix — AeroSplitPane nested freeze:** при 3+ pane через 2+ сплиттера (вложение в `end`-слот) перетаскивание левого сплиттера меняло `totalPx` вложенного pane, что ре-кеило `remember(totalPx)` и сбрасывало внутренний divider; при сжатии вложенного pane ниже `minFirst+minSecond` `clampDividerPx`'s `coerceIn(min,max)` получал `min > max` и бросал исключение. ✓ Fixed via fraction-based divider state + `coerceAtLeast` clamp guard (TDD).
- **New — AeroDateTimeRangePicker:** как `AeroDateRangePicker` (двойной календарь, range-выбор), но с временем — отдельные time-rows для start и end + Cancel/Apply commit-gate; emits `(LocalDateTime, LocalDateTime)`; full API parity (`showSeconds` + `minuteStep`) с `AeroDateTimePicker`. ✓ Shipped.

</details>

<details>
<summary>📦 v2.0 Stateful + Layout — shipped 2026-06-18 (milestone goal & target features)</summary>

**Goal:** Добавить полный набор сложных stateful-компонентов (data table, tree, date/time pickers, color picker, range slider) и продвинутых layout-примитивов (accordion, split pane, sidebar, stepper wizard) — те самые «v2 deferred» из v1.0/v1.1, которые в реальном desktop-приложении нужны для полноценного UI. Это major-feature drop сравнимый с v1.0 по объёму (12 новых компонентов), без breaking changes к существующему API.

**Target features:**

**Complex Stateful (CMPLX):**
- `AeroDataTable` — таблица с заголовками, виртуализацией строк (LazyColumn), сортировкой по клику на заголовок, выделением строк (single/multi с Ctrl/Shift), и resizable колонками (drag splitter)
- `AeroTreeView` — иерархическое дерево с раскрытием/свёрткой узлов через `onExpand` callback (lazy children loading), опциональными иконками
- `AeroDatePicker` — выбор одной даты через popup-календарь
- `AeroTimePicker` — выбор времени (часы + минуты)
- `AeroDateTimePicker` — комбинированный выбор даты + времени
- `AeroDateRangePicker` — выбор диапазона дат через двойной календарь
- `AeroColorPicker` — HSV-квадрат + hue полоса + RGB sliders + HEX input + палитра предустановленных swatches; альфа-канал опционален
- `AeroRangeSlider` — ползунок с двумя ручками (от–до), композиция поверх AeroSlider

**Advanced Layout (ADVL):**
- `AeroAccordion` — сворачиваемые секции; параметр `mode = single | multi`
- `AeroSplitPane` — N-pane через рекурсивную композицию (публичный API — 2-pane с `orientation = horizontal | vertical`); вложенность через каллер
- `AeroSidebar` — persistent боковая навигация (новый компонент рядом с `AeroDrawer`, разная механика); три режима: expanded (иконка+лейбл) / collapsed (только иконки + tooltip) / hidden
- `AeroStepperWizard` — линейный шаговый процесс с `onValidate: () -> Boolean` per-step (next блокируется при false)

**Integration:**
- Все компоненты следуют существующим конвенциям: префикс `Aero`, `Icon(AeroIcons.*)` для глифов, явный `tint`, glass modifiers где уместно, поддержка трёх тем (AeroBlue / AeroDark / Classic)
- `:library` остаётся единым модулем — отдельный `:datepickers` или `:datatable` НЕ создаётся в v2.0
- showcase получает по секции на каждую группу (DataSection, PickersSection, LayoutSection и т.д.) или расширения существующих секций — решается на этапе планирования

</details>

## Requirements

### Validated

<!-- All shipped through v2.0. See archived REQUIREMENTS at .planning/milestones/v1.0-REQUIREMENTS.md (informal, captured in v1.1 archive snapshot), .planning/milestones/v1.1-REQUIREMENTS.md, and .planning/milestones/v2.0-REQUIREMENTS.md. -->

**v1.0 (53):**
- ✓ **Foundation** (10): AeroTheme, AeroColorScheme, 3 темы, glass modifiers, AeroTypography, explicitApi — Phase 1 — v1.0
- ✓ **Buttons + Inputs + Selection + Dropdowns + Range + Lists** (21): BTN/INP/SEL/DRP/RNG/LST — Phase 2 — v1.0
- ✓ **Containers + Overlays + Navigation** (19): CNT/OVL/NAV — Phase 3 — v1.0
- ✓ **Showcase** (3): SHW-01..03 — Phases 1–3 (рос параллельно) — v1.0

**v1.1 (17):**
- ✓ **AeroIcons Foundation** (3): ICN-01..03 — 138 Phosphor Regular ImageVector constants, lazy backing-property, KDoc, explicitApi — Phase 4 — v1.1
- ✓ **Component Migration** (11): MIG-01..11 — все текстовые глифы и Material Icons заменены на `AeroIcons.*` в 11 компонентах — Phase 5 — v1.1
- ✓ **Dependency Cleanup** (3): CLN-01..03 — `compose.materialIconsExtended` удалён, тесты переписаны, grep-gate чист — Phase 5 — v1.1
- ✓ **Showcase IconsSection** (3): SHW-04..06 — `LazyVerticalGrid` всех 138 иконок + поиск; ButtonsSection migrated; three-theme visual sign-off — Phase 6 — v1.1

**v2.0 (27):**
- ✓ **AeroDataTable** (DATA-01..04) — sortable columns (3-position), virtualized rows, single/multi row selection (`Set<RowKey>`, survives sort), drag-resize columns — Phase 9 — v2.0
- ✓ **AeroTreeView** (DATA-05..06) — lazy children via once-only `onExpand` (`SnapshotStateMap`) — Phase 9 — v2.0
- ✓ **Date/time pickers** (PICK-01..04) — `AeroDatePicker`, `AeroTimePicker`, `AeroDateTimePicker`, `AeroDateRangePicker` (kotlinx-datetime types; popup positioning + partial-range-leak resolved) — Phase 8 — v2.0
- ✓ **AeroColorPicker** (PICK-05..07) — HSV square + hue + RGB + HEX + swatches + optional alpha; HSV single-source-of-truth (drift-free) — Phase 8 — v2.0
- ✓ **AeroRangeSlider** (PICK-08) — dual-thumb, `awaitPointerEventScope` drag (no touchSlop), no-cross — Phase 8 — v2.0
- ✓ **AeroAccordion** (LAYO-01..02) — single | multi mode, lifted state, animated — Phase 10 — v2.0
- ✓ **AeroSplitPane** (LAYO-03..04) — 2-pane public API, clamped divider, 8dp hit-area — Phase 10 — v2.0
- ✓ **AeroSidebar** (LAYO-05..07) — expanded | collapsed | hidden, animated, scope DSL — Phase 10 — v2.0
- ✓ **AeroStepperWizard** (LAYO-08..09) — linear, per-step `onValidate` commit-gate, Back preserves state — Phase 10 — v2.0
- ✓ **Showcase v2.0** (SHW-07..10) — DataSection + PickersSection + LayoutSection; 16-item × 3-theme sign-off PASSED — Phase 11 — v2.0

**v2.0.1 (18):**
- ✓ **AeroDateTimePicker seconds fix** (FIXDT-01..02) — `formatAeroDateTime` helper + nullable-formatter dispatch; trigger shows `HH:MM:SS` при `showSeconds=true`, custom formatter сохраняется verbatim — Phase 12 — v2.0.1
- ✓ **AeroSplitPane nested-freeze fix** (FIXSP-01..04) — fraction-based divider state (no `remember(totalPx)` re-key) + `clampDividerPx` inverted-range guard; nested N-pane drag без snap-back/crash; single-level не регрессирует; TDD-locked — Phase 12 — v2.0.1
- ✓ **AeroDateTimeRangePicker** (DTR-01..08) — Apply-gate dual-calendar datetime range picker; `onRangeSelect` ровно один раз по Apply; `orderDateTimeRange` same-day swap; no cross-open state leak; `showSeconds`/`minuteStep` parity — Phase 12 — v2.0.1
- ✓ **Showcase + docs** (SHW-11..14) — `AeroDateTimeRangePicker` live-label row, `showSeconds` contrast demos, nested 3-pane SplitPane demo; three-theme sign-off PASSED; kotlinx-datetime doc-note corrected — Phase 12 — v2.0.1

**v2.0.2 (18 + 1):**
- ✓ **AeroPanelGroup** (PNL-01..13) — scope-DSL N-секционный layout; collapse-в-~36dp-заголовок с перераспределением высоты соседям; drag-resize между раскрытыми соседями (VS Code Side Bar); fraction-based размеры переживают ресайз окна; гибрид controlled/uncontrolled по паттерну `AeroAccordion`; `collapsible`/`resizable` флаги; явный `key` идентичности секции — Phase 13 — v2.0.2
- ✓ **AeroPanelGroup поведение + визуал** (PNL-14..18) — Win7 Aero glassPanel-заголовок (CaretRight 0°→90°, `leadingIcon`, `headerActions`); краевые случаи (все свёрнуты / одна раскрыта); 12 чистых JVM-юнит-тестов (`PanelGroupLogicTest`); showcase демо + three-theme sign-off PASSED; KDoc с REQ-ID + PITFALL — Phase 13 — v2.0.2
- ✓ **AeroPanelGroup horizontal orientation** (PNL-HORIZ-01) — горизонтальная ориентация через общий internal-core `AeroPanelGroupImpl(orientation)` + аддитивный `orientation` default-param; N колонок, vertical dividers, drag-resizes-width, rotated header strip; zero breaking change, zero vertical regression; three-theme sign-off на обеих ориентациях — Phase 13.1 — v2.0.2

### Active

<!-- v2.0.3 PanelGroup Recompose Fix — scoped 2026-06-25. См. .planning/REQUIREMENTS.md. -->

**v2.0.3 PanelGroup Recompose Fix:**
- [ ] **RCMP-01..04** — устранить дублирование секций в horizontal controlled при рекомпозиции-во-время-drag; size-math из `isExpanded`; sync в `SideEffect`; repro в showcase
- [ ] **REG-01..02** — Vertical + uncontrolled byte-identical; 12 JVM-тестов GREEN; Compose 1.7.3
- [ ] **REL-01..02** — bump `2.0.3`, tag `v2.0.3`, push на JitPack

### Out of Scope

- Мобильные платформы (Android/iOS) — Compose Desktop only
- Web-версия — не планируется
- Публикация в Maven Central — только локальный Maven для начала
- Встроенная поддержка локализации (i18n) — на усмотрение разработчика-потребителя
- Несколько весов иконок (Phosphor-style thin/light/bold/fill) — только regular в v1.1; пересмотр возможен в v2.x при появлении консьюмер-запроса
- Filled / duotone варианты иконок — только outline (stroke-based)
- Кастомные пользовательские иконки через AeroIcons API — пользователь использует обычный `ImageVector` напрямую
- Иконки в отдельном Gradle-модуле — всё в `:library` для v1.x (отделение возможно в v2.0+)
- Настоящий DWM Aero blur через JNI/WinAPI — симуляция через градиенты визуально достаточна
- WCAG-совместимость гарантии — цвета Aero-тем не оптимизированы под контрастность
- Aero Snap на кастомном окне — `WindowDraggableArea` не передаёт HTCAPTION OS, известное ограничение
- v2.0-specific exclusions:
  - **Inline-mode date/time pickers** — только popup-based варианты; inline-режим (всегда видимый календарь) откладывается до v2.x
  - **DataTable cell editing / inline editing** — только read-only render с selection в v2.0; редактирование добавится в отдельном milestone если появится consumer-запрос
  - **DataTable column reordering (drag-to-rearrange)** — resize ✓, reorder ✗
  - **DataTable column filtering UI** — sort ✓, фильтрация откладывается (caller сам фильтрует data до передачи)
  - **TreeView drag-and-drop reordering** — выбор и раскрытие ✓, перетаскивание узлов ✗
  - **ColorPicker eyedropper** — палитра + sliders + HEX, screen color picking требует platform-specific и откладывается
  - **StepperWizard branching (non-linear)** — только линейный проход в v2.0; branching откладывается
  - **AeroSidebar drag-to-resize width** — фиксированные ширины для expanded/collapsed; ручная регулировка откладывается
  - **AeroDropdown popup-offset regression fix** (v1.0 carry-over) — НЕ в scope v2.0; отдельный gap-closure phase или v2.x

## Context

- **Исходная программа:** `C:\1A_WORK\lastver_131\mordred` — Compose Desktop приложение управления сеансами связи со спутниками. Содержит рабочие реализации AeroTitleBar, CompactTextField, MordredButton, MordredChip и системы тем — визуальный стиль и цветовые схемы взяты оттуда.
- **Glass-эффекты:** Реализованы в едином `drawBehind`-блоке (`GlassModifiers.kt`) — три модификатора (`glassEffect`, `glassPanel`, `glassSurface`) с градиентами, полупрозрачностью и рамками; перенесены и переработаны в `:library`.
- **Иконки:** Vendored Phosphor Regular SVGs в `tools/phosphor-svgs/regular/` с `.pin` файлом, фиксирующим upstream SHA. Конвертация через Valkyrie CLI 1.1.1 (`--output-format BackingProperty`); сгенерированные `.kt` коммитятся в `src/main/`, build-time generation НЕ используется.
- **Анимации:** Каждая анимация (hover, раскрытие, переходы) утверждается пользователем отдельно перед реализацией.
- **Обсуждение компонентов:** Форма, логика и параметры каждого компонента или группы компонентов обсуждаются с пользователем перед реализацией.
- **Известный регресс v1.0:** AeroDropdown popup offset — root cause в `AeroScrollArea` (`Column.fillMaxSize()` форсит 320dp при `heightIn(max=320.dp)`); запланирован gap-closure без блокировки следующих milestone.

## Constraints

- **Tech stack:** Kotlin 2.1.21 + Compose Desktop 1.7.3, Gradle Kotlin DSL 8.14.3, JDK 17
- **Зависимости:** Material 3 как основа, кастомный стиль поверх — не заменять Material полностью, а оборачивать/расширять. `compose.materialIconsExtended` НЕ используется (удалён в v1.1).
- **Совместимость:** Compose Desktop (Windows primary, Linux/macOS secondary)
- **Именование:** Все компоненты с префиксом `Aero` (AeroButton, AeroTextField и т.д.); иконки следуют Phosphor verbatim (`AeroIcons.X`, `AeroIcons.CaretDown`)
- **Распространение:** Maven/JAR артефакт — `com.mordred:aero-compose-ui`
- **Window chrome:** `undecorated=true` БЕЗ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION (issue #3757); glass-эффект симулируется внутри окна через градиент

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Prefix `Aero` для всех компонентов | Избегает конфликтов с Material3 именами в проектах-потребителях | ✓ Good — нет конфликтов наблюдалось через v1.0/v1.1 |
| Material3 как основа (не замена) | Переиспользует accessibility, семантику, state management | ✓ Good — `Icon()` из material3 используется напрямую без обёртки |
| Три темы из mordred как дефолтные | Уже отработаны визуально в реальном проекте | ✓ Good — три visual checkpoints прошли без правок цветов |
| Отдельный showcase-модуль | Позволяет проверять компоненты между фазами без внешнего проекта | ✓ Good — каждый visual checkpoint v1.0/v1.1 опирался на showcase |
| Каждая анимация утверждается отдельно | Контроль над визуальной сложностью и производительностью | ✓ Good — анимации согласованы пофазно |
| `undecorated=true` БЕЗ `transparent=true` | Win11 EXCEPTION_ACCESS_VIOLATION (issue #3757); glass через gradient | ✓ Good — нет крашей в v1.0/v1.1; правило закреплено в трёх source-of-truth точках |
| Glass effect в одном `drawBehind` | Избегает overdraw и iGPU performance collapse | ✓ Good — стабильно на v1.0/v1.1 |
| `:library` использует `compose.desktop.common`, `:showcase` — `currentOs` | Platform-neutral JAR для библиотеки, native binary для приложения | ✓ Good |
| AeroIcons как порт Phosphor Regular (stroke ~1.5, 256×256 viewBox) | MIT, мягкий скруглённый outline ближе к Win7-toolbar-glyph; rounded caps/joins ложатся на Aero-эстетику | ✓ Good — three-theme visual sign-off PASSED v1.1 |
| Один вес (Regular), без filled / glass-treatment иконок | Filled противоречит «мягкий outline без gloss»; glass-обвязка — отдельный AeroIconButton-уровень | ✓ Good — нет запросов на filled через v1.1 |
| Типизированные константы `AeroIcons.*` (не name-based lookup) | Compile-time safety, IDE autocomplete, привычно после Material Icons | ✓ Good — autocomplete работает, нет typo-багов |
| `materialIconsExtended` удалён из `:library` | «Единый набор векторных иконок» — Material визуально не Aero; снижает classpath на ~36 MB | ✓ Good — JAR неизменен (был classpath-only); compileClasspath чище |
| Иконки в `:library`, не в отдельном `:icons` модуле | Меньше Gradle-сложности для v1.1; разделение возможно в v2.0+ если появится консьюмер | ✓ Good для v1.1; ⚠️ Revisit если консьюмерам понадобится icon-only зависимость |
| Phosphor naming verbatim (`X` не `Close`, `CaretDown` не `ChevronDown`) | Совместимость с phosphoricons.com lookup; нет «двойных имён» при апгрейдах | ✓ Good — KDoc naming-table документирует map |
| Lazy backing-property pattern для всех 138 констант | Eager `val` при таком масштабе вызывает измеримый startup spike | ✓ Good — нет startup-регрессии замечено |
| Generated `.kt` коммитятся в `src/main/` (Valkyrie не вызывается на build) | Воспроизводимость, ревьюабельность, нет build-time зависимости от CLI | ✓ Good — Phase 4/5 миграции опирались на статичные файлы |
| Phase 5 wave ordering (миграции → TitleBar → тесты → dep removal) | Test-rewrites должны предшествовать удалению `materialIconsExtended` (CLN-01 gates CLN-02) | ✓ Good — нет broken-build промежуточных коммитов |
| AeroBreadcrumb `separator: String` НЕ мигрирован | Единственный intentional text-rendered glyph; в v1.1 не оверфит миграцию | ✓ Good — locked-decision, не пересматривался |
| `Icon()` из material3 напрямую, без `AeroIcon()` wrapper | Меньше поверхности API; tint всегда явно передаётся в library-коде | ✓ Good — все 11 миграций следуют паттерну единообразно |
| **v2.0:** `awaitPointerEventScope` + manual loop для всего Canvas-drag (не `detectDragGestures`) | touchSlop=18dp молча ломает Canvas-drag на Compose Desktop (PITFALL-03) | ✓ Good — `Modifier.aeroDragSplitter` shared utility; drag отвечает на первый пиксель в RangeSlider/ColorPicker/SplitPane/DataTable |
| **v2.0:** Phase 7 enabling-фаза перед публичными компонентами | Calendar/color-math/drag/step-indicator нужны 2+ компонентам — строим один раз | ✓ Good — нулевое дублирование, единые баги, 27 unit-тестов как gate перед Phase 8 |
| **v2.0:** DataTable selection = `Set<RowKey>` + caller `key:(T)->Any` (не `Set<Int>`) | Индексы устаревают после сортировки (PITFALL-04); смена post-ship — breaking change | ✓ Good — выделение переживает sort в showcase sign-off |
| **v2.0:** ColorPicker внутреннее состояние — только HSV float tuple; RGB/HEX derived | Хранение и HSV, и RGB вызывает round-trip drift (PITFALL-15) | ✓ Good — drift-gate тест (sat 1.0→0.5→1.0 → #FF0000) зелёный |
| **v2.0:** `AeroScrollArea` запрещён внутри DataTable/TreeView — raw `LazyListState + AeroScrollBar` | `LazyColumn` в `AeroScrollArea` уничтожает виртуализацию (PITFALL-01) | ✓ Good — grep-gate чист; виртуализация подтверждена eyes-on |
| **v2.0:** `AeroCalendarPositionProvider` (Phase 7) вместо `AeroDropdownPopup` для date-popup | `AeroDropdownPopup` залочен по ширине anchor — календарь шире обрезается (PITFALL-02) | ✓ Good — нет clip на правом крае 1024dp окна |
| **v2.0:** 16-item × 3-theme "looks done but isn't" checklist как формальный sign-off gate | Stateful-компоненты молча ломаются способами, невидимыми в коде | ✓ Good — первый проход FAILED (16 дефектов), все закрыты, 48/48 cells PASS |
| **v2.0:** `kotlinx-datetime` объявлен `api(libs.kotlinx.datetime)` (library/build.gradle.kts:27) | Picker-сигнатуры экспонируют `kotlinx.datetime.*` — `api` гарантирует transitive-доступ для consumer'ов | ✓ Good — transitive leak отсутствует; стале-нота закрыта в v2.0.1 (SHW-14) |
| **v2.0.1:** `formatAeroDateTime(ldt, showSeconds)` internal helper + nullable `formatter: ((T)->String)? = null` с body-level dispatch | Default-лямбда не может закрыться над `showSeconds`, объявленным после `formatter` (PITFALL-H); helper — единый источник trigger-формата для DateTime + DateTimeRange pickers | ✓ Good — FIXDT-01/02 закрыты, дублирования бага в новом компоненте нет; конвенция для обоих pickers |
| **v2.0.1:** Fraction-based `AeroSplitPane` divider state (хранится фракция, `dividerPx` derived каждый recompose; нет `remember(totalPx)` ключа) | `remember(totalPx)` ре-кеил state при изменении `totalPx` вложенного pane → внутренний divider сбрасывался во время outer-drag (PITFALL-A) | ✓ Good — nested N-pane drag держит позицию; single-level не регрессирует (FIXSP-01/03) |
| **v2.0.1:** `clampDividerPx` guard — `val safeMax = maxPx.coerceAtLeast(minFirstPx)` перед `coerceIn` | При сжатии inner pane ниже combined minima `maxPx < minFirstPx` → `coerceIn(min>max)` бросал `IllegalArgumentException` (PITFALL-B) | ✓ Good — no-throw, тихий clamp; inverted-range unit test написан RED до фикса (FIXSP-02/04, TDD) |
| **v2.0.1:** Apply-gate для `AeroDateTimeRangePicker` — `onDayClick` отбрасывает commit pair; единственный `onRangeSelect` emit+close site — кнопка Apply, gated `rangeState is Selected` | Клик по второй дате не должен ни закрывать popup, ни эмитить partial range (PITFALL-E); `orderDateTimeRange` тихо свопает same-day reversed times | ✓ Good — emit ровно один раз; 4 remember(expanded) блока убирают cross-open leak; verification 18/18 |
| **v2.0.1:** SplitPane drag читает live state в drag-loop (`rememberUpdatedState`-паттерн), не captured copy из `pointerInput` lambda | Stale captured `dividerFraction` снапил inner splitter обратно — FIXSP-01 регрессия, тот же класс бага что AeroRangeSlider F9 (Phase 11) | ✓ Good — пойман на three-theme sign-off, исправлен (7f38c0c) до approval; подтверждает ценность визуального gate |
| **v2.0.2:** Pattern 3 для совмещения анимации и drag на одном `sizePx` — `animateFloatAsState` читает target-only, drag пишет state напрямую, `isDragging` переключает spec на `snap()` | Два writer'а на одно значение дают snap-back/осцилляцию (PNL-PITFALL-01); обязательный спайк первым пунктом плана | ✓ Good — спайк подтвердил отсутствие snap-back/осцилляции; collapse-then-drag чист; залочено как ответ на «animate vs. drag the same value» |
| **v2.0.2:** Header reservation — `availableForExpanded = totalPx − sectionCount*headerPx − activeDividers*thickness`, резервируется заголовок на КАЖДУЮ секцию (не только свёрнутую) | Каждая секция всегда рендерит 36dp заголовок независимо от expanded; иначе layout-math съезжает (спайк finding 1) | ✓ Good — все секции рендерят header strip; распределение корректно при любом collapse-наборе |
| **v2.0.2:** Чистая логика в `PanelDistribution.kt` (8 функций, zero Compose imports) + 12 GREEN JVM-тестов до Compose-кода (TDD) | Образец `SplitClampTest`/`AccordionToggleTest`; N-section кламп с PITFALL-B `coerceAtLeast` guard тестируется без runtime | ✓ Good — 12/12 GREEN; кламп-краш предотвращён RED→GREEN |
| **v2.0.2:** Public-wrapper + internal-core `AeroPanelGroupImpl(orientation)` для горизонтали; аддитивный `orientation: Orientation = Orientation.Vertical` default-param | Mirrors `AeroSplitPane`; orientation добавляется без breaking change и без дублирования layout/state/drag (только 3 branch-точки) | ✓ Good — zero breaking change, 12 logic-тестов unchanged/GREEN, zero vertical regression; three-theme sign-off на обеих ориентациях |
| **v2.0.2:** Rotated header strip через `BoxWithConstraints` + `requiredWidth(maxHeight)` + `rotate(-90f)` | `graphicsLayer`-only и `placeRelativeWithLayer` подходы давали неправильную ширину/позицию вертикального заголовка (GAP-1, пойман на sign-off) | ✓ Good — корректный bottom-to-top заголовок в 36dp полоске; подходы-кандидаты отброшены |
| **v2.0.3:** `AeroPanelGroup` size-math читает `isExpanded()` каждую композицию; sync `expandedState` перенесён в `SideEffect` | Запись в наблюдаемый snapshot-state во время композиции (`BoxWithConstraints`/`SubcomposeLayout`) вызывала ×N дублирование секций в horizontal+controlled при рекомпозиции во время drag | ✓ Fixed — no write-during-composition; vertical/uncontrolled byte-identical; 12 PanelGroupLogicTest GREEN |

---
*Last updated: 2026-06-25 — after starting v2.0.3 PanelGroup Recompose Fix milestone*
