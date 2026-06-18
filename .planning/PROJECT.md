# aero-compose-ui

## What This Is

Библиотека UI-компонентов для Compose Desktop, выдержанная в визуальном стиле Windows Aero (Windows 7): стеклянные градиентные поверхности, полупрозрачные панели, анимации и кастомная оконная хромка. Публикуется как Maven/JAR-артефакт (`com.mordred:aero-compose-ui`), подключается через Gradle. Содержит ~50 компонентов, типизированный набор из 138 векторных иконок `AeroIcons` (порт Phosphor Regular), и три встроенные темы с поддержкой кастомизации цветов.

## Core Value

Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна, типизированным набором иконок и демо-витриной — без необходимости реализовывать стиль или искать совместимый icon pack самостоятельно.

## Current State (Shipped: v2.0 Stateful + Layout, 2026-06-18)

- **v1.0 MVP** (3 фазы, 53 требования): Foundation + Atomic Components + Composite/Navigation. Все компоненты библиотеки реализованы, три темы работают, showcase демонстрирует всё.
- **v1.1 Icon System** (3 фазы, 17 требований): 138 векторных иконок `AeroIcons` (порт Phosphor Regular) заменили все текстовые символы и `Icons.Outlined.*`; `compose.materialIconsExtended` удалён из Gradle dependency graph; showcase содержит `IconsSection` с поиском.
- **v2.0 Stateful + Layout** (5 фаз 7–11, 27 требований): 12 новых компонентов (8 complex stateful + 4 advanced layout) поверх внутреннего фундамента primitives. Pickers (`AeroDatePicker`/`TimePicker`/`DateTimePicker`/`DateRangePicker`/`ColorPicker`/`RangeSlider`), Data (`AeroDataTable` виртуализованный + `AeroTreeView` lazy), Layout (`AeroAccordion`/`SplitPane`/`Sidebar`/`StepperWizard`). `kotlinx-datetime:0.6.2` — единственная новая зависимость. Showcase: DataSection + PickersSection + LayoutSection; 16-item × 3-theme sign-off PASSED. No breaking changes к v1.x API.

**Codebase:** Kotlin / Compose Desktop 1.7.3, Kotlin 2.1.21, Gradle 8.14.3, JDK 17. v2.0 added 152 files changed (+27,406 / −2,285) across phases 7–11.

## Next Milestone

Not yet defined. Start with `/gsd:new-milestone` (questioning → research → requirements → roadmap). Candidate scope captured in archived REQUIREMENTS "v3+ Future Requirements" (`.planning/milestones/v2.0-REQUIREMENTS.md`): inline pickers, DataTable cell-editing/reorder/filter, TreeView drag-and-drop, ColorPicker eyedropper, StepperWizard branching, Sidebar drag-resize, and the carry-over AeroDropdown popup-offset fix.

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

### Active

(None — v2.0 shipped. Next milestone defined via `/gsd:new-milestone`.)

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
| **v2.0:** `kotlinx-datetime` объявлен `implementation`, не `api` | Соответствует repo-конвенции all-deps-implementation | ⚠️ Revisit на publish — picker-сигнатуры экспонируют `kotlinx.datetime.*`; для PUBLISHED-библиотеки transitive type утечёт (адресовать на POM-шаге) |

---
*Last updated: 2026-06-18 after completing milestone v2.0 Stateful + Layout*
