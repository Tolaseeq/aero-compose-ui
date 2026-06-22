# Phase 13: AeroPanelGroup - Context

**Gathered:** 2026-06-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Один аддитивный layout-компонент `AeroPanelGroup` (+ элемент секции `AeroPanelSection`): вертикальное разбиение на N секций, где любую секцию можно свернуть в полоску-заголовок (~36dp), а раскрытые соседи забирают освободившуюся высоту; границу между двумя соседними **раскрытыми** секциями можно перетаскивать (модель VS Code Side Bar). Только вертикальная ориентация. Без breaking changes к v2.x API. Покрывает требования PNL-01..PNL-18.

Горизонтальная ориентация, drag-reorder секций, вложенность как first-class и клавиатурный ресайз — вне границы этой фазы (см. Deferred).

</domain>

<decisions>
## Implementation Decisions

### Форма публичного API (PNL-01)
- **Scope-DSL**, по образцу `AeroSidebar` (`content: @Composable AeroPanelGroupScope.() -> Unit`), НЕ `List<data class>` как `AeroAccordion`.
- Объявление секции: `section(key, title, …) { content }` с trailing content-лямбдой.
- Пер-секционные параметры объявляются в `section(...)`: `key` (обяз.), `title`, `minSize`, `collapsible`, `resizable`, `defaultSize`, `leadingIcon`, `headerActions`.
- Причина: много пер-секционных параметров + контент-слот читаются чище в DSL; совпадает с исходным спеком пользователя. Controlled/uncontrolled-модель раскрытия при этом всё равно зеркалит `AeroAccordion` (см. ниже) — выбор DSL её не отменяет.

### Раскрытие — гибрид controlled/uncontrolled (PNL-08)
- Точно по паттерну `AeroAccordion`: `onExpandedChange == null` → uncontrolled (внутренний `mutableStateOf`, seed из `initiallyExpanded`); non-null → controlled pure renderer (caller владеет состоянием раскрытия). **Обе ветки намеренные — не схлопывать в одну.**
- Идентичность секции — явный `key` (устойчиво к дублирующимся `title` и переупорядочиванию). Состояние (раскрытость, размер, `lastExpandedFraction`) привязывается к `key`.

### Размеры секций (PNL-04, PNL-09)
- Внутреннее uncontrolled-состояние размеров. Наружу — через `onLayoutChange`, срабатывает **на drag-end и на collapse/expand** (НЕ на каждый кадр) для персиста/восстановления.
- Хранить **fraction** (стабильная координата), `px` выводить из `totalPx` каждый recompose — точно как `AeroSplitPane` (нет `remember(totalPx)`-ключа → переживает ресайз окна и outer-drag, PITFALL-A).
- `lastExpandedFraction` (НЕ абсолютный px) запоминается при сворачивании, восстанавливается при раскрытии (PITFALL: абсолютный px ломается после уменьшения окна).

### Начальная раскладка / sizing (PNL-04)
- По умолчанию **все секции раскрыты, делят высоту поровну**.
- Caller может задать `section(defaultSize = …dp)` для неравного старта (нормируется под доступную высоту).
- Стартово свёрнуть часть секций — через гибридный `initiallyExpanded: Set<key>` (uncontrolled) / controlled-параметры, как `AeroAccordion`.

### Переполнение контента секции (новое — секции теперь фиксированной высоты)
- Секция **клипает** контент к своему слоту. Скролл — **забота вызывающего** (оборачивает контент в `AeroScrollArea` сам).
- НЕ авто-оборачивать в `AeroScrollArea` (избегаем навязанного скроллбара и известных нюансов `AeroScrollArea`/виртуализации).

### Заголовок и взаимодействие (PNL-14, PNL-02)
- **Клик по всей полоске-заголовку** → toggle collapse/expand (как `AeroAccordion`: `clickable` на всём header-Row).
- `headerActions` — в **отдельном кликабельном слоте справа**; их клик НЕ сворачивает секцию (не должен всплывать к toggle заголовка).
- `headerActions` **видны и в раскрытом, и в свёрнутом** состоянии (модель VS Code — действия доступны на полоске).
- Полоска-заголовок: `glassPanel(cornerRadius = 8.dp)`-поверхность; шеврон `AeroIcons.CaretRight` вращается 0°→90° при раскрытии через `animateFloatAsState` + `graphicsLayer { rotationZ }` (как `AeroAccordion`); опциональный `leadingIcon` (явный `tint`, v1.1-правило).
- Высота полоски-заголовка — **фиксированная internal-константа ~36dp** (не публичный параметр).

### Collapse/expand vs drag-resize (PNL-07 — ГЛАВНЫЙ РИСК, спайк первым)
- Размеры — в px/fraction. **Drag пишет размер напрямую без анимации**; **toggle анимирует целевой размер** через `animateFloatAsState` (200ms `FastOutSlowInEasing`, в тон `AeroSidebar`).
- Никакого height-measurement / лишних layout-проходов и `SubcomposeLayout` на кадр (явный PITFALL проекта).
- Drag-loop читает **live state** (`rememberUpdatedState` для `totalPx`, MutableState читается через делегат) — НЕ captured-копию из `pointerInput`-лямбды (FIXSP-01 регрессия v2.0.1).
- Спайк (POC) подтверждает сосуществование `animateFloatAsState`-target и прямой drag-записи на одной переменной размера ДО любого UI-кода.

### Разделитель (PNL-05, PNL-06)
- Грип-разделитель рисуется **ТОЛЬКО между двумя соседними РАСКРЫТЫМИ секциями**. Активные разделители = соседние раскрытые пары (через `zipWithNext`-фильтр), НЕ `expandedCount - 1`.
- Граница рядом со свёрнутой секцией — статичный стык заголовков (без грипа, без drag, без resize-курсора).
- Drag переносит px между двумя соседями с клампом по `minSize` обеих; N-section кламп учитывает Σ минимумов секций ниже разделителя, не только непосредственного соседа.
- Реюз `Modifier.aeroDragSplitter(Orientation.Vertical, onDrag)` и `clampDividerPx` **verbatim** (inverted-range guard `safeMax = maxPx.coerceAtLeast(minFirstPx)` уже внутри — PITFALL-B). Визуал разделителя — 3 грип-точки + 1dp линия + 8dp hit-area, как `SplitPaneDivider`.

### Per-section флаги (PNL-11, PNL-12)
- `collapsible = false`: нет шеврона, секция не сворачивается, но **участвует в ресайзе**.
- `resizable = false`: разделители без грипа, drag отключён (чистый collapse/expand).

### Краевые случаи (PNL-15)
- Все свёрнуты → стопка заголовков сверху, остаток контейнера пустой.
- Одна раскрыта → занимает весь `availableForExpanded` (`= totalPx − Σ заголовки свёрнутых − Σ разделители`).

### Тесты и интеграция (PNL-16, PNL-17, PNL-18)
- Чистая логика в `components/layout/internal/panelgroup/` (без Compose-импортов): распределение px, N-section кламп, перенос доли при collapse/expand, нормализация при ресайзе, `availableForExpanded`. **TDD: RED → GREEN до Compose-кода** (образец `SplitClampTest`/`AccordionToggleTest`).
- Демо в `showcase/.../sections/LayoutSection.kt`; three-theme visual sign-off (AeroBlue / AeroDark / Classic).
- KDoc со ссылками на REQ-ID и PITFALL, единообразно с соседними layout-компонентами.

### Claude's Discretion
- Точные имена internal-helper'ов и файлов (`PanelDistribution.kt` и т.п.) и сигнатуры pure-функций.
- Точная структура `AeroPanelGroupScope` и сигнатура `section(...)` (порядок/дефолты параметров).
- Дефолтное значение `minSize` (константа) и точная константа высоты заголовка (~36dp).
- Внутренняя механика анимации (один `animateFloatAsState` на секцию vs производная), пока соблюдены px-source-of-truth + no-extra-layout-pass правила.
- Точный набор и порядок шагов внутри единственной фазы (при сохранении: спайк → pure-logic TDD → … → showcase sign-off).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

Внешних ADR/спеков у проекта нет — требования зафиксированы в решениях выше и в файлах ниже.

### Milestone research (читать первым)
- `.planning/research/SUMMARY.md` — синтез; build-order (спайк ведёт), open questions (уже разрешены в этом CONTEXT)
- `.planning/research/STACK.md` — подтверждение «zero new dependencies», точные Compose API
- `.planning/research/ARCHITECTURE.md` — state-модель, интеграция, разделение pure/Compose, build order
- `.planning/research/PITFALLS.md` — PNL-PITFALL-01..08 (animation-vs-drag первым), carry-forward уроки
- `.planning/research/FEATURES.md` — table-stakes / differentiators / anti-features, поведение эталонов

### Requirements & project
- `.planning/REQUIREMENTS.md` — PNL-01..PNL-18 (+ locked design decisions, future, out-of-scope)
- `.planning/ROADMAP.md` — Phase 13 goal, success criteria, encoded 8-step build order
- `.planning/PROJECT.md` §"Key Decisions" / "Context" — v2.0/v2.0.1 locked decisions, PITFALL-03/A/B

### Reuse verbatim (исходники-образцы)
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt` — fraction-state, `rememberUpdatedState`, divider+grip, BoxWithConstraints→totalPx
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroAccordion.kt` — controlled/uncontrolled hybrid, caret 0→90° анимация, glassPanel header
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSidebar.kt` — scope-DSL образец (`AeroSidebarScope`, `item()`/`section()`)
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt` — `clampDividerPx` (safeMax guard), `fractionToPx`/`pxToFraction` — реюз
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — `Modifier.aeroDragSplitter` (awaitPointerEventScope, no touchSlop)
- `library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt`, `AccordionToggleTest.kt`, `SidebarStateTest.kt` — образцы pure-logic тестов

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Modifier.aeroDragSplitter(orientation, onDrag)` — реюз verbatim для drag разделителя (Orientation.Vertical → N_RESIZE).
- `clampDividerPx` / `fractionToPx` / `pxToFraction` (SplitClamp.kt) — реюз; inverted-range guard уже внутри. N-section кламп — новая pure-функция поверх той же идеи.
- `SplitPaneDivider`-визуал (1dp линия + 3 грип-точки + 8dp hit-area, hover-tint) — образец для разделителя PanelGroup.
- `glassPanel(cornerRadius = 8.dp)` + `AeroIcons.CaretRight` + `graphicsLayer{rotationZ}` — заголовок и шеврон (как AeroAccordion).
- `AeroSidebarScope` — образец конструкции scope-DSL.

### Established Patterns
- **Fraction-as-stable-coordinate**: храним fraction, выводим px каждый recompose; НЕТ `remember(totalPx)` (AeroSplitPane:107-108, PITFALL-A).
- **Live-state в drag-loop**: `rememberUpdatedState(totalPx)` + чтение MutableState через делегат (AeroSplitPane:116-124, FIXSP-01).
- **Hybrid controlled/uncontrolled** (AeroAccordion:117-164): `controlled = onX != null`; внутренний `mutableStateOf` seed из `initially*`.
- **Анимация без height-measurement**: `animateFloatAsState`/`graphicsLayer` для шеврона; px-driven высоты — НЕ `animateContentSize` (несовместим с явными px-высотами; в отличие от AeroAccordion, который content-sized).
- **Pure-logic в `internal/<feature>/`** + JUnit-тест без Compose (SplitClamp, AccordionToggle, SidebarState, WizardStep).
- **W11/perf**: один `drawBehind` glass; `BoxWithConstraints` один раз, drag обновляет только state; никакого `SubcomposeLayout` на кадр.

### Integration Points
- Новый публичный файл: `library/.../components/layout/AeroPanelGroup.kt` (+ `AeroPanelGroupScope`).
- Новый internal-пакет: `library/.../components/layout/internal/panelgroup/` (distribution + N-section clamp helpers).
- Новый тест: `library/src/test/.../components/layout/Panel*Test.kt`.
- Showcase: append-only в `showcase/.../sections/LayoutSection.kt`.
- Без изменений `build.gradle.kts` / `libs.versions.toml` — zero new dependencies.

</code_context>

<specifics>
## Specific Ideas

- Эталоны: **VS Code Side Bar** (Explorer/Outline/Timeline — accordion-of-resizable-panels) и **react-resizable-panels** (headless; `onLayout`, `Panel.collapsible/minSize/defaultSize`). Поведение «грип только между раскрытыми, у свёрнутой — статичный стык» и «headerActions видны на свёрнутой полоске» — прямо из VS Code-модели.
- Эстетика строго Win7 Aero (gloss/gradient/rounded/depth), НЕ плоский modern-flat — `glassPanel`-заголовок, 200ms-анимации в тон AeroSidebar/AeroAccordion.

</specifics>

<deferred>
## Deferred Ideas

- **Горизонтальная ориентация** (`orientation = horizontal`) — PNL-HORIZ-01, v2.x.
- **Drag-to-reorder секций** — PNL-REORDER-01, не входит в модель VS Code Side Bar.
- **Вложенные `AeroPanelGroup` как first-class API** — PNL-NEST-01 (вложение через каллер работает и так).
- **Клавиатурный ресайз/навигация по разделителям** — PNL-KBD-01, v2.x (как у AeroSplitPane keyboard-nudge).
- **`headerHeight` как публичный параметр** — пока фиксированная ~36dp константа.
- **Carry-over AeroDropdown popup-offset fix (DROP-FIX-01)** — явно вне scope v2.0.2.

</deferred>

---

*Phase: 13-aeropanelgroup*
*Context gathered: 2026-06-22*
