# aero-compose-ui

## What This Is

Библиотека UI-компонентов для Compose Desktop, выдержанная в визуальном стиле Windows Aero (Windows 7): стеклянные градиентные поверхности, полупрозрачные панели, анимации и кастомная оконная хромка. Публикуется как Maven/JAR-артефакт (`com.mordred:aero-compose-ui`), подключается через Gradle. Содержит ~50 компонентов, типизированный набор из 138 векторных иконок `AeroIcons` (порт Phosphor Regular), и три встроенные темы с поддержкой кастомизации цветов.

## Core Value

Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна, типизированным набором иконок и демо-витриной — без необходимости реализовывать стиль или искать совместимый icon pack самостоятельно.

## Current State (Shipped: v3.0 Glass Refinement, 2026-07-29)

- **v1.0 MVP** (3 фазы, 53 требования): Foundation + Atomic Components + Composite/Navigation. Все компоненты библиотеки реализованы, три темы работают, showcase демонстрирует всё.
- **v1.1 Icon System** (3 фазы, 17 требований): 138 векторных иконок `AeroIcons` (порт Phosphor Regular) заменили все текстовые символы и `Icons.Outlined.*`; `compose.materialIconsExtended` удалён из Gradle dependency graph; showcase содержит `IconsSection` с поиском.
- **v2.0 Stateful + Layout** (5 фаз 7–11, 27 требований): 12 новых компонентов (8 complex stateful + 4 advanced layout) поверх внутреннего фундамента primitives. Pickers (`AeroDatePicker`/`TimePicker`/`DateTimePicker`/`DateRangePicker`/`ColorPicker`/`RangeSlider`), Data (`AeroDataTable` виртуализованный + `AeroTreeView` lazy), Layout (`AeroAccordion`/`SplitPane`/`Sidebar`/`StepperWizard`). `kotlinx-datetime:0.6.2` — единственная новая зависимость. Showcase: DataSection + PickersSection + LayoutSection; 16-item × 3-theme sign-off PASSED. No breaking changes к v1.x API.
- **v2.0.1 Picker & SplitPane Fixes** (1 фаза 12, 18 требований): patch milestone — два bug-фикса + один аддитивный компонент. `AeroDateTimePicker` теперь показывает секунды в триггере при `showSeconds=true` (FIXDT); вложенный N-pane `AeroSplitPane` перетаскивается без snap-back и без краша (FIXSP, fraction-based divider state + inverted-range clamp guard); новый `AeroDateTimeRangePicker` — Apply-gate dual-calendar picker, emits `(LocalDateTime, LocalDateTime)` (DTR). Zero new dependencies, no breaking changes. Three-theme showcase sign-off PASSED.
- **v2.0.4 PanelGroup Recompose Fix** (1 фаза 14, 8 требований): устранено дублирование header-полос в горизонтальном CONTROLLED `AeroPanelGroup` при перетаскивании разделителя во время рекомпозиции родителя. **v2.0.3 был неудачной попыткой** (фикс не той причины — `SideEffect`/`isExpanded()`); баг воспроизвёлся в реальном приложении. Настоящая первопричина: section-DSL-лямбда `content` была `@Composable` → имела свой recompose-scope → при drag + рекомпоз дописывала `section()` в persisted `scope` (3→9→…→33). Фикс: DSL-лямбда стала не-`@Composable` (как `LazyListScope`). Добавлен детерминированный Compose UI-тест с программным drag (11→1). Подтверждено в реальном приложении. Source-compatible, zero new runtime deps.
- **v2.0.2 AeroPanelGroup** (2 фазы 13 + 13.1, 18 + 1 требований): аддитивный layout-компонент `AeroPanelGroup` (+ `AeroPanelSection` через scope-DSL) — N секций заполняют родителя, сворачиваются в ~36dp полоску-заголовок (соседи забирают высоту), drag-resize между соседними раскрытыми секциями (модель VS Code Side Bar); fraction-based размеры переживают ресайз окна; гибридный controlled/uncontrolled API по паттерну `AeroAccordion`; Pattern 3 (animate-target vs. drag-write + `isDragging`→`snap()`) разрешает PNL-PITFALL-01; 12 чистых JVM-юнит-тестов без Compose; Win7 Aero визуал (glassPanel, CaretRight 0°→90°, headerActions, grip dots). Вставленная Phase 13.1 добавила горизонтальную ориентацию через общий internal-core `AeroPanelGroupImpl(orientation)` + аддитивный default-param — zero breaking change, zero vertical regression (PNL-HORIZ-01). Three-theme sign-off PASSED на обеих ориентациях. Zero new dependencies.

- **v3.0 Glass Refinement** (6 фаз 15–20, 57 требований): обязательная миграция тулчейна на Kotlin 2.4.10 + Compose Multiplatform 1.11.1 (Material3 припинен к стабильной 1.9.0), доказанно инертная — 232/232 теста зелёные и человеческий вердикт «нет визуального дрейфа», подтверждённый пиксельным diff. Новый общий слой Aero-примитивов: единственная реализация отрисовки `drawAeroSurfaceCore` с четырьмя путями наружу (`Modifier.aeroSurface`, прямой `DrawScope`-вызов, `aeroGlowRing`, `aeroThumbSurface`/`aeroGroove`), алгоритмически выводимые `AeroOrnamentTokens.derive` поверх новой RGB-математики `Color.lighten()`/`darken()`, исходно-совместимый `ornamentOverride`. Починены три давних дефекта `GlassModifiers.kt` (пиксельный литерал глянца, срезаемый бордер, мёртвый `elevation`). Восемь компонентов переведены на этот слой без изменения публичного API и поведения; `AeroSwitch` и `AeroSegmentedControl` впервые получили hover/press/focus, `AeroListItem` — клипованную «пилюлю» выделения, где ховер компонуется поверх выделения. Добавлен общий механизм focus-visible (клавиатура показывает фокус, указатель — нет). Тесты 232 → 467. Внешний scratch-потребитель поймал реальный дефект (`AeroTheme` не рисовал фон), скрытый собственным showcase.

**Codebase:** Kotlin 2.4.10 / Compose Multiplatform 1.11.1 (Material3 припинен к стабильной 1.9.0), Gradle 8.14.3, JDK 17. 35 975 строк Kotlin в 351 файле. v2.0 added 152 files changed (+27,406 / −2,285) across phases 7–11; v2.0.1 added 9 code files (+520 / −14) in Phase 12; v2.0.2 added 4 code files (+1,516) across Phases 13 + 13.1; v3.0 changed 248 files (+43,770 / −2,638), of which 71 code files (+11,104 / −409), across Phases 15–20. Project version bumped `2.0.4` → `3.0.0` at milestone close (`build.gradle.kts`), per the locked bump-on-milestone rule — Phase 15 deliberately proved JitPack with throwaway tags (`v3.0.0-alpha01`, `-verify01/02`) instead of bumping mid-milestone.

## Last Milestone: v3.0 Glass Refinement — SHIPPED 2026-07-29

Шесть фаз (15–20), 41 план, 57/57 требований. Восемь компонентов, читавшихся как Material3, переведены на новый общий слой Aero-примитивов поверх мигрированного тулчейна — публичный API и поведение не изменились. Приёмка мейнтейнера на трёх темах пройдена на 100% DPI и подтверждена захватами; прогоны на 125%/200% DPI, которых требовал SHW-16, сознательно отменены решением мейнтейнера — пробел зафиксирован, а не выдан за пройденный. Тип закрытия — `override_closeout`: девять принятых отложенных позиций перечислены в STATE.md § Deferred Items. Подробности: MILESTONES.md, `.planning/milestones/v3.0-ROADMAP.md`, RETROSPECTIVE.md.

**Что осталось открытым после v3.0:** яркость `AeroOrnamentTokens` на AeroBlue/AeroDark (G4, направление — пробовать более тёмные значения именно для этих двух тем); отсутствие клавиатурного фокуса у `AeroRangeSlider` (доставшееся по наследству); клип-пробел в `GlassModifiers.kt` (IN-01, радиус поражения ~40 компонентов); scratch/proof-файлы, до сих пор лежащие в `showcase/src/main` (WR-01); контраст ярлыков ниже WCAG-порога 4.5:1 у части поверхностей.

<details>
<summary>📦 v3.0 Glass Refinement — milestone goal & target features (scoped 2026-07-21)</summary>

**Goal:** Переделать визуал восьми компонентов, которые сейчас читаются как Material3, в узнаваемо-стеклянный Aero-облик — опираясь на расширенный и починенный общий слой Aero-примитивов, поверх мигрированного на актуальный стабильный Compose Multiplatform тулчейна. Функционал и публичный API компонентов не меняются.

**Major-версия, потому что:** обязательная миграция тулчейна поднимает нижнюю границу Kotlin/Compose для всех потребителей — подключиться к новой версии на старом Compose нельзя. Плюс заметная смена внешнего вида восьми компонентов. Линия 2.x остаётся рабочей на Compose 1.7.3 (последняя — `2.0.4`).

**Target features:**
- **Миграция тулчейна (обязательно, первой фазой)** — подъём на актуальный стабильный Compose Multiplatform с сопутствующим подъёмом Kotlin/Gradle/JDK. Разблокирует `Modifier.dropShadow` / `Modifier.innerShadow` (появились в CMP 1.9.0, на 1.7.3 отсутствуют) — штатные тени и внутренний rim вместо ручных приближений. Совместимость со старыми потребителями сознательно не поддерживается.
- **Фундамент Aero-примитивов** — починить существующие `GlassModifiers` (мёртвый `elevation`, хардкод 100px-градиента в `glassSurface`, обрезаемый `clip`'ом бордер) и расширить набор визуальных приёмов Aero (двухтоновая заливка, пропорциональный верхний глянец, внутренний бевел/rim light, тень/свечение, желобок-трек). Новые токены в `AeroColorScheme` для трёх тем.
- **Кнопки** — `AeroButton`, `AeroOutlinedButton`: собственная отрисовка вместо плоской M3-заливки; полноценные hover/press/focus/disabled состояния в Aero-логике.
- **Селекторы** — `AeroSwitch` (сейчас полностью плоский, без hover/press/focus), `AeroSegmentedControl` (нет объёма у выбранного сегмента, нет hover).
- **Range** — `AeroSlider` (M3 `Slider`), `AeroRangeSlider` (плоский Canvas), `AeroProgressBar` (плоские Box'ы без градиента и желобка).
- **Списки** — `AeroListItem`: сейчас неклипованный прямоугольник сплошного цвета; нужен Aero-подсвет выделения/ховера.
- **Приёмка** — showcase-демонстрации и визуальный sign-off на AeroBlue / AeroDark / Classic.

**Явно НЕ в scope:** ревизия остальных ~40 компонентов библиотеки; изменение поведения, сигнатур и размеров-по-умолчанию сверх необходимого для нового визуала; обратная совместимость с потребителями на Compose 1.7.3 (`aska`, `satellite-control` мигрируют отдельно или остаются на `2.0.4`).

**Ключевые решения, принятые при постановке:**
- Собственная отрисовка допустима там, где M3-геометрия мешает Aero — публичный API и поведение сохраняются 1:1.
- Сначала слой примитивов, затем перевод компонентов на него (повторяет удачную схему Phase 7 в v2.0).
- Вернность: «дух Aero, современное исполнение» — узнаваемо стеклянно и объёмно, без буквального копирования пропорций Win7.

</details>

<details>
<summary>📦 v2.0.4 PanelGroup Recompose Fix — shipped 2026-06-26</summary>

Patch milestone (Phase 14). Eliminated header-strip duplication in horizontal CONTROLLED `AeroPanelGroup` under drag-while-recompose. **v2.0.3 shipped a wrong-cause fix** (`SideEffect`/`isExpanded()`, a write-during-composition theory) and the bug persisted in a real consumer; the real cause was the `@Composable` section-DSL lambda accumulating `scope.sections` (3→9→…→33) when re-run independently during an active drag. **v2.0.4 fix:** non-`@Composable` DSL lambda (like `LazyListScope`), guarded by a deterministic `runComposeUiTest` programmatic-drag test. See MILESTONES.md / RETROSPECTIVE.md and the `project_panelgroup_composable_dsl_pitfall` + `feedback_repro_must_exercise_path` memories. Confirmed working in the consumer app. Released as `com.github.Tolaseeq:aero-compose-ui:2.0.4`; v2.0.3 remains tagged but superseded.

</details>

## Current Milestone: v3.1 Dependency Refresh + Hot Reload MCP

**Status (2026-09-24):** Phase 21 complete and verified (24/24 requirements). `3.1.0` is tagged and green on JitPack (`com.github.Tolaseeq:aero-compose-ui:v3.1.0`, tag only — origin master not moved). Validated in Phase 21: Migration + Release 3.1.0. Next: `/bm:complete-milestone`.

**Goal:** Принудительно перевести весь проект на последние стабильные версии зависимостей и тулчейна, доказать, что ничего не сломалось, и выпустить `3.1.0` на JitPack — и одновременно поставить Compose Hot Reload с MCP-сервером, через который агент сам смотрит и кликает работающую витрину, доказав, что этот способ отладки не мешает мейнтейнеру пользоваться компьютером.

**Target features:**
- **Жёсткое обновление всех версий до последних стабильных** (сверено с Maven Central / gradle.org 2026-09-21): Kotlin 2.4.10 → 2.4.20, Compose Multiplatform 1.11.1 → 1.12.0, kotlinx-coroutines 1.10.2 → 1.11.0, kotlinx-datetime 0.6.2 → чистая 0.8.0 (без `-0.6.x-compat`), JUnit 5.10.0 → 6.1.3, Gradle 8.14.3 → 9.7.1, JDK 17 → 21 везде (библиотека, витрина, `jitpack.yml`). Material3 остаётся на `1.9.0` — стабильной новее не существует, пин сохраняется.
- **Compose Hot Reload + MCP-сервер** — Hot Reload 1.2.0 только в `:showcase` (в публикуемый артефакт не попадает), JetBrains Runtime 21 как JVM запуска, `.mcp.json` в корне с запуском через `cmd /c gradlew.bat --no-daemon --quiet --console=plain <hotMcpServer-task>` (на Windows `./gradlew` не спавнится). Установка — одним коммитом.
- **Hot Reload + MCP — инструмент, а не цель** — ставится по ходу миграции, чтобы агент сам смотрел и кликал витрину. Агент сам замеряет, что захват, чтение дерева и клик не двигают курсор и не забирают активное окно, в том числе при перекрытом и свёрнутом окне; итог — короткий факт в отчёте.
- **Доказательство «всё норм» после обновления** — все существующие тесты зелёные (467 на входе), витрина стартует, агент сам через MCP обходит все разделы витрины в трёх темах и сравнивает со снимками, снятыми ДО обновления тем же способом (`PrintWindow`, не серверный `take_screenshot`); hover, фокус и перетаскивание, которых у MCP нет, проверяются Compose UI-тестами со снимками в картинку (без окна и без системного ввода); всё неподтверждённое — отдельным списком. Мейнтейнеру предъявляется только итог и найденные расхождения.
- **Релиз** — версия `3.1.0` в `build.gradle.kts`, тег `v3.1.0`, зелёная сборка JitPack, в README новые минимальные требования для потребителей (Java 21, Compose 1.12, Kotlin 2.4.20, kotlinx-datetime 0.8).

**Явно НЕ в scope:** новые компоненты, изменения публичного API, визуальные правки (кроме починки дрейфа, если его вызовет само обновление); закрытие долгов v3.0; внешний scratch-потребитель как гейт релиза (мейнтейнер выбрал «тесты + обход витрины агентом»).

**Ключевые решения, принятые при постановке:**
- Минорный номер `3.1.0` при фактически ломающем для потребителей релизе (Java 21, Compose 1.12, исчезновение старых `kotlinx.datetime.Instant`/`Clock`) — осознанный выбор мейнтейнера; требования прописываются в README, а не выражаются номером версии.
- Java 21 поднимается везде, а не только для запуска витрины — «жёстко обновляем всё».
- Снимки «до» снимаются раньше обновления прежним ручным способом: MCP-сервер появляется только с Compose Multiplatform 1.12.0.
- Начиная с этой вехи GUI уходит мейнтейнеру на осмотр только после того, как агент сам всё просмотрел через MCP и доволен результатом в рамках задач фазы.
- Если апгрейд или установка JBR требуют большего, чем смена версии, либо MCP двигает реальный курсор / крадёт фокус — остановка и вопрос мейнтейнеру, без обходных путей.

## Next Milestone Goals

Кандидаты на веху после v3.1. Нумерация фаз v3.1 продолжается с **21**.

**Открытые кандидаты:**
- **Долг v3.0:** G4 (яркость ornament-токенов на AeroBlue/AeroDark), клавиатурный фокус `AeroRangeSlider`, IN-01 (клип-пробел `GlassModifiers.kt`, ~40 компонентов), вынос scratch/proof-файлов из `showcase/src/main`, контраст ярлыков до WCAG 4.5:1, квадратная тень ховера `AeroRadioButton`, ABI-совместимость `AeroTheme.establishBackground`, расширение теста контраста на outlined и disabled-сегменты.
- **VIS-F01** — ревизия визуала остальных ~40 компонентов: восемь теперь стеклянные, остальные всё ещё нет. Самый естественный преемник v3.0.
- Отложенное из v3.0: VLST-F01 (зеркальное отражение строки списка), VRNG-F01 (Win7-ping-pong для indeterminate).
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

**v2.0.4 (8):**
- ✓ **PanelGroup recompose fix** (RCMP-01..04, REG-01..02, REL-01..02) — non-`@Composable` section-DSL лямбда устраняет накопление `scope.sections` при drag-во-время-рекомпозиции; детерминированный `runComposeUiTest` drag-тест 11→1 — Phase 14 — v2.0.4

**v3.0 (57):**
- ✓ **Toolchain Migration** (TOOL-01..08) — Kotlin 2.4.10 + CMP 1.11.1 собрались с первой попытки, Material3 припинен к стабильной 1.9.0; RCMP-страж портирован и заново доказан падающим на несломанном коде; 232/232 теста + человеческий вердикт «нет визуального дрейфа»; сигнатуры `dropShadow`/`innerShadow` подтверждены bytecode-инспекцией реального артефакта; JitPack зелёный — Phase 15 — v3.0
- ✓ **Aero Primitives Foundation** (PRIM-01..18) — `Color.lighten()`/`darken()`, `AeroOrnamentTokens.derive` + исходно-совместимый `ornamentOverride`, декларативный `AeroSurfaceStyle`, единственная реализация `drawAeroSurfaceCore` с четырьмя путями наружу, `aeroGlowRing`/`aeroThumbSurface`/`aeroGroove`, три починенных дефекта `GlassModifiers.kt`, `drawWithCache`-геометрия, `rememberAeroInteractionState()` в `components/common/`, дымовой прогон всей библиотеки, спайк M3-слотов PASS — Phase 16 — v3.0
- ✓ **Buttons** (VBTN-01..06) — общий internal `AeroButtonSurface` (M3-контейнер убран), пять состояний, клип ховера по форме, `Role.Button` + Space/Enter, outlined как фиксированная дельта заливной — Phase 17 — v3.0
- ✓ **Range** (VRNG-01..09) — `AeroSlider` сохраняет M3 `Slider` с кастомными слотами, `AeroRangeSlider` перерисован при byte-identical drag-логике, `AeroProgressBar` на `aeroGroove`/`aeroSurface` с опциональным (по умолчанию выключенным) бликом и сохранённым таймингом 1500ms — Phase 18 — v3.0
- ✓ **Selectors + Lists** (VSEL-01..04, VLST-01..04) — `AeroSwitch` и `AeroSegmentedControl` впервые получили hover/press/focus, выбранный сегмент утоплен кодом нажатой кнопки, `AeroListItem` клипует выделение в «пилюлю», ховер компонуется поверх выделения — Phase 19 — v3.0
- ✓ **Showcase + Verification** (SHW-15..16, VER-01..06) — постоянная секция Verification со всеми восемью компонентами, два grep-гейта, snapshot размеров/радиусов против тега v2.0.4, UI-тесты клавиатуры, внешний scratch-потребитель, каждый гейт доказан падающим на несломанном коде; приёмка на трёх темах пройдена на 100% DPI, прогоны 125%/200% отменены решением мейнтейнера — Phase 20 — v3.0

**v3.1 (24):**
- ✓ **Toolchain + dependencies** (TOOL-09..17): Gradle 9.7.1, JDK 21, Kotlin 2.4.20, Compose Multiplatform 1.12.0, kotlinx-coroutines 1.11.0, kotlinx-datetime 0.8.0, JUnit 6.1.3; Material3 pinned 1.9.0; locked test count 541 — Phase 21 — v3.1
- ✓ **Pre-upgrade baseline** (BASE-01..05): launch-parameter navigation, PrintWindow capture, run-to-run noise, UI-test state captures — Phase 21 — v3.1
- ✓ **Hot Reload + MCP** (HRM-01..03): `:showcase` only, `:showcase:hotMcpServer`, 27/27 non-interference measurements at 96 DPI — Phase 21 — v3.1
- ✓ **Post-upgrade verification** (VER-07..10): 102 outside-noise differences, all accepted by the maintainer; unconfirmed list; maintainer hand-off approved — Phase 21 — v3.1
- ✓ **Release** (REL-03..05): verify tag green on JitPack, README consumer floor, `3.1.0` published as `com.github.Tolaseeq:aero-compose-ui:v3.1.0` (tag only) — Phase 21 — v3.1

### Active

<!-- v3.1 Dependency Refresh + Hot Reload MCP. REQ-ID и трассировка — в .planning/REQUIREMENTS.md. -->

- [x] Все зависимости и тулчейн проекта на последних стабильных версиях (Kotlin 2.4.20, Compose Multiplatform 1.12.0, coroutines 1.11.0, kotlinx-datetime 0.8.0, JUnit 6.1.3, Gradle 9.7.1, JDK 21)
- [x] Compose Hot Reload + MCP-сервер установлены в `:showcase`, `.mcp.json` подключает сервер на Windows
- [x] Доказано, что MCP-отладка не двигает реальный курсор, не крадёт фокус и работает при перекрытом/свёрнутом окне
- [x] После обновления тесты зелёные, витрина стартует, обход витрины агентом в трёх темах не выявил необъяснённого визуального дрейфа
- [x] `3.1.0` опубликована на JitPack, README называет новые минимальные требования для потребителей

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
- WCAG-совместимость как гарантия уровня библиотеки — цвета Aero-тем не оптимизированы под контрастность. Уточнено в v3.0: контраст ярлыков на кнопках и сегментах теперь охраняется value-level тестами, но часть поверхностей остаётся ниже порога 4.5:1 (заведено в `.planning/todos/pending/`), и гарантии на весь набор компонентов по-прежнему нет
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

- **Tech stack:** Kotlin 2.4.10 + Compose Multiplatform 1.11.1, Gradle Kotlin DSL 8.14.3, JDK 17 (поднято в v3.0 с Kotlin 2.1.21 / Compose 1.7.3 — линия 2.x остаётся рабочей на старом тулчейне, последняя `2.0.4`)
- **Зависимости:** Material 3 как основа, кастомный стиль поверх — не заменять Material полностью, а оборачивать/расширять. Координата Material3 припинена явно к стабильной `1.9.0`: алиас `compose.material3` на CMP 1.11.x молча резолвится в alpha. `compose.materialIconsExtended` НЕ используется (удалён в v1.1).
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
| **v2.0.3:** `AeroPanelGroup` size-math читает `isExpanded()` каждую композицию; sync `expandedState` перенесён в `SideEffect` | Теория write-during-composition внутри `BoxWithConstraints`/`SubcomposeLayout` | ⚠️ Superseded — НЕ та причина; баг воспроизвёлся в реальном приложении. Правка безвредна, но не лечит. Реальный фикс — v2.0.4 ниже |
| **v2.0.4:** section-DSL-лямбда `content` сделана НЕ-`@Composable` (`content: AeroPanelGroupScope.() -> Unit`, как `LazyListScope`) | Реальная первопричина RCMP: `@Composable` DSL-лямбда имела свой recompose-scope → при активном drag рекомпоз родителя перезапускал её независимо, дописывая `section()` в persisted `scope` (3→9→…→33), `key()`-цикл рендерил всё больше header-полос | ✓ Good — подтверждено инструментированием; детерминированный `runComposeUiTest` drag-тест 11→1; 232 теста GREEN; подтверждено в реальном приложении. Builder/DSL-лямбды с side-effect в коллекцию НИКОГДА не должны быть `@Composable` |
| **v3.0:** Миграция тулчейна изолирована в отдельную первую фазу с нулевыми визуальными правками | Смешать риск апгрейда зависимостей с риском кода отрисовки — значит потерять возможность отнести регрессию к одной из причин | ✓ Good — пара Kotlin 2.4.10 + CMP 1.11.1 (никогда не выпускавшаяся согласованной) собралась с первой попытки; «нет дрейфа» доказано и тестами, и пиксельным diff, потому что визуальный код был неподвижен |
| **v3.0:** Одна `drawAeroSurfaceCore`, наружу четырьмя путями (`aeroSurface`, прямой `DrawScope`-вызов, `aeroGlowRing`, `aeroThumbSurface`/`aeroGroove`) — вместо независимых реализаций | Три копии градиентно-бевельной логики разойдутся визуально; повторяет удачную схему enabling-фазы Phase 7 в v2.0 | ✓ Good — все производные примитивы получены `style.copy()`-подменой полей, ни одной кустарной градиентной реализации ниже по стеку |
| **v3.0:** `AeroOrnamentTokens.derive(base)` выводит токены алгоритмически через RGB `lighten`/`darken`, а не десятком литералов на тему | `.copy(alpha=)` умеет гасить, но не осветлять, а в Classic токены непрозрачные — альфа-манипуляция там не работает вовсе | ✓ Good — три темы обслуживаются одной формулой; ⚠️ Revisit — G4: на AeroBlue/AeroDark яркость всё ещё великовата, направление правки — более тёмные значения именно для этих двух тем |
| **v3.0:** `AeroSlider` СОХРАНЯЕТ M3 `Slider` и передаёт кастомные `thumb =`/`track =` слоты; спайк выполнен до, а не после планирования фазы | Полное удаление M3 переносит на себя drag/keyboard/steps/семантику — самый дорогой из возможных вариантов; допущение проверено bytecode-инспекцией реального артефакта | ✓ Good — спайк PASS понизил сложность Phase 18 с HIGH до MEDIUM; поведение слайдера не регрессировало |
| **v3.0:** focus-visible выведен из потока interaction'ов (свой reducer), а не из `LocalInputModeManager` | Платформенный focus-visible гейтит только `Indication`, которую библиотека повсеместно отключает — то есть на этом коде он не работает в принципе | ✓ Good — одна общая механика гейтит кольца фокуса на switch, сегментах, строках списка и обеих кнопках; клавиатура показывает фокус, указатель — нет |
| **v3.0:** Цвет ярлыка — свойство схемы, выбираемое по полярности поверхности (`labelOnFilledSurface` / `labelOnOutlinedSurface`), а не вычисление на месте вызова из анимирующейся заливки | Пер-компонентная подкрутка констант уже дважды разъезжалась (сегмент против кнопки); вычисление из анимирующегося цвета даёт мигание и неповторяемые замеры | ✓ Good — сегменты и кнопки перестали расходиться; ⚠️ Revisit — часть поверхностей всё ещё ниже WCAG 4.5:1, заведено в todos |
| **v3.0:** Каждый гейт (grep, snapshot, contrast, UI-тест) доказывается падающим на намеренно сломанном коде до того, как ему верят | Прямое следствие урока v2.0.3: страж, который не был проверен на red, — не страж | ✓ Good — VER-06 закрыт для всех новых гейтов; тесты 232 → 467 |
| **v3.0:** Приёмка через внешний scratch-потребитель, собранный против опубликованного тега, а не через собственный showcase | Ни `aska`, ни `satellite-control` эту веху не отслеживают — исторически сильнейший ловец регрессий недоступен | ✓ Good — поймал реальный дефект (`AeroTheme` не рисовал фон), который собственный `Surface` showcase'а маскировал; починено в библиотеке, а не обойдено |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/bm:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/bm:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-09-24 — Phase 21 (Migration + Release 3.1.0) complete: whole toolchain on latest stable, no unexplained drift against the pre-upgrade baseline, Hot Reload + MCP in `:showcase` proven not to move the cursor or steal focus, `3.1.0` on JitPack. Previous: 2026-09-21 — milestone v3.1 started.*
