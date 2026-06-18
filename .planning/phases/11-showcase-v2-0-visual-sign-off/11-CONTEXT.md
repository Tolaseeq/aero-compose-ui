# Phase 11: Showcase + v2.0 Visual Sign-off - Context

**Gathered:** 2026-06-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Завершающая фаза milestone v2.0: **демонстрация + формальный sign-off**, строго additive, без изменения публичного API компонентов (всё зафиксировано в Phases 8–10).

Поставляет:
1. **`DataSection`** (SHW-07) — `AeroDataTable` на ~100 строках mock-данных + **`AeroTreeView`** (нужен для checklist-пункта lazy-callback, см. ниже); sort / Ctrl+Shift multi-selection / column-resize интерактивно проверяемы.
2. **`PickersSection`** (SHW-08) — все 5 picker-компонентов (DatePicker, TimePicker, DateTimePicker, DateRangePicker, ColorPicker) + `AeroRangeSlider`; каждый показывает текущее значение как `Text` для UAT.
3. **`LayoutSection`** (SHW-09) — `AeroAccordion` (single+multi), `AeroSplitPane` (horizontal+vertical), `AeroSidebar` с переключателем mode, `AeroStepperWizard` (3–4 шага с валидацией).
4. **`RangeSection` extension** — добавить ряд с `AeroRangeSlider` (in place, без структурных изменений файла).
5. **Wiring:** `ShowcaseApp.kt` получает **ровно три** новых вызова: `DataSection()`, `PickersSection()`, `LayoutSection()`.
6. **Scratch cleanup:** удалить `AeroPhase7Scratch.kt` (:library, public) + `Phase7ScratchSection` (:showcase) + вызов из `ShowcaseApp.kt`.
7. **v2.0 sign-off gate:** 16-item "looks done but isn't" checklist из PITFALLS.md проходит eyes-on во всех трёх темах (AeroBlue/AeroDark/Classic). Это формальный gate milestone'а.

**Out of scope (locked / handled elsewhere):**
- Любое изменение публичного API v2.0-компонентов или v1.x — milestone строго additive, фиксация прошла в Phases 8–10.
- Структурные изменения существующих section-файлов (кроме точечного добавления в `RangeSection`).
- Содержание 16-item checklist — фиксировано в PITFALLS.md, не пересматривается.
- AeroDropdown popup-offset regression (v1.0 carry-over) — НЕ в scope v2.0.

</domain>

<decisions>
## Implementation Decisions

### DataSection — mock-данные и состав
- **Предметная область = спутниковые сеансы связи** (тематика исходного проекта mordred). Связывает библиотеку с её происхождением, естественно ложится на mixed-типы колонок.
- **Объём = ~100 строк.** Уверенно доказывает виртуализацию (видимых ~15–20 в LazyColumn при 100), достаточно для Shift-диапазона выделения, не тормозит генерацию (ROADMAP-рекомендация; REQUIREMENTS допускает 50–200).
- **6 колонок, все типы + бейдж.** Ориентир: `Name` (text) · `NORAD ID` (number) · `AOS Date` (date) · `Duration min` (number) · `Elevation°` (number) · `Status` (badge). Покрывает text/number/date/status-badge → можно сортировать по каждому типу и ресайзить. Точные имена/набор — за планировщиком в рамках темы.
- **Статус-бейдж = `AeroBadge`** (v1.0, LST-02) с цветовыми вариантами под статус (напр. Active/Scheduled/Failed → success/info/error). Тянет существующий v1.x компонент в demo таблицы; семантически точнее, чем AeroChip.
- **`AeroTreeView` ОБЯЗАТЕЛЕН в DataSection** (решение, выведенное из gate, не из SHW-07): SHW-07 явно называет только DataTable, но 16-item checklist содержит пункт *«AeroTreeView lazy callback: open node, scroll off, scroll back — onExpand НЕ вызывается повторно»* (PITFALL-05). Без живого TreeView этот пункт нельзя проверить → DataSection (секция «Data», не «Table») включает компактный TreeView с логирующим `onExpand` для UAT-проверки once-only поведения. Mock-дерево — за планировщиком (напр. иерархия наземных станций / орбитальных групп).

### LayoutSection — композиция демо (хостинг bounded-компонентов в скролл-колонке)
- **`AeroSplitPane` (h+v) = в `Box` с фиксированной высотой** (ориентир ~240–300dp). SplitPane требует bounded height, а showcase — `verticalScroll` Column (infinite height). Фиксированный бокс прост, предсказуем для drag, совпадает с паттерном demo-боксов. Два демо: horizontal + vertical orientation.
- **`AeroSidebar` = top-level sibling в фиксированном `Box { Row { AeroSidebar(state); demo-контент } }`** + кнопка-переключатель `state.mode` (expanded→collapsed→hidden). Контент справа reflow'ится при collapse — прямо доказывает checklist-пункт PITFALL-11 *«collapse sidebar → adjacent content reflows»*. Sidebar НЕ помещается внутрь SplitPane-панели (PITFALL-11 запрет). Бокс с фиксированной высотой, чтобы жить внутри скролл-колонки.
- **`AeroStepperWizard` = мини-форма из реальных v1.x полей.** Шаги: (1) `AeroTextField` (напр. имя/идентификатор; `onValidate` = непустое), (2) `AeroCheckbox`/`AeroRadioGroup` (выбор), (3) summary. Минимум один шаг с реальным `onValidate`-gate. Демонстрирует и навигацию, и **сохранение state на Back** (PITFALL-12: контент шага не пересоздаётся), и gate (Next блокируется при false). 3–4 шага.
- **`AeroAccordion` = две колонки в `Row`** (`Column(weight 1f)` single + `Column(weight 1f)` multi) с подписями «single»/«multi». Буквально side-by-side как в SC; сразу видна разница поведения (в single открытие B закрывает A; в multi оба открыты).

### PickersSection — расположение и value-display
- **Раскладка = вертикальные label-ряды** по образцу существующего `RangeSection` (`RangeRow`: лейбл 160dp слева + компонент + value `Text`). Единый стиль с RangeSection; вертикальный стек даёт popup-календарям место раскрываться вниз без коллизий между соседями.
- **`AeroColorPicker` = через `AeroColorPickerButton` (popup)** — кнопка-триггер открывает HSV-панель в popup, единообразно с остальными picker'ами; компактно в ряду; value = HEX рядом. (Оба варианта — button+popup и inline-панель — существуют из Phase 8; для секции выбран popup.)
- **Value display = raw value в ISO-формате.** Показывать реальное значение callback'а: `LocalDate`/`LocalTime`/`LocalDateTime` через `.toString()` (ISO), диапазон как «start → end», HEX для цвета, float для slider. Точно отражает что пришло в callback — идеально для UAT.
  - **DateRangePicker value Text отражает ТОЛЬКО committed range** (после `onRangeSelect`); до коммита — placeholder/пусто. Это попутно визуализирует, что partial-state НЕ ликает (PITFALL-06 checklist-пункт).

### Sign-off gate + scratch cleanup
- **Проведение = интерактивный UAT, eyes-on во всех трёх темах** (AeroBlue/AeroDark/Classic). Гибрид: grep-пункты (`transparent = true`, `AeroScrollArea` внутри DataTable/TreeView) прогоняются автоматически; визуальные/drag-пункты (drag-response на первом движении, AeroDark disabled-cells, popup-positioning, контраст, four-state selection) проверяет пользователь глазами в работающем showcase. Совпадает с visual-checkpoint конвенцией всех прошлых фаз (каждый milestone подписывался через showcase).
- **Запись результата = отдельный sign-off документ в фазе** (напр. `11-SIGNOFF.md` или выделенная секция в SUMMARY) с отметками PASS/FAIL по каждому из 16 пунктов ×3 темы. Артефакт milestone-gate'а, виден позже в `/gsd:audit-milestone`. Phase 11 НЕ помечается complete пока каждый пункт не PASS.
- **Scratch cleanup = в Phase 11.** Удалить `AeroPhase7Scratch.kt` (:library, public) + `Phase7ScratchSection.kt` (:showcase) + вызов `Phase7ScratchSection()` из `ShowcaseApp.kt`. Phase 7 явно пометил AeroPhase7Scratch как «deleted Phase 11»; чистит public API v2.0 JAR от временного scratch перед ship. Обязательно до финального sign-off (grep-gate'ы и API-чистота — часть проверки).

### Claude's Discretion
- Точные имена колонок/полей и конкретные mock-значения DataSection (в рамках спутниковой темы); структура mock-дерева TreeView.
- Точный маппинг статусов на `AeroBadge`-варианты; набор статусов.
- Точные высоты demo-боксов (SplitPane / Sidebar), paddings, ширины колонок аккордеона.
- Контент summary-шага визарда и тексты валидационных сообщений.
- Точное имя sign-off файла (`11-SIGNOFF.md` vs секция в SUMMARY) и его формат-таблица.
- Гранулярность планов / wave-split (рекомендация ниже).
- Лейблы кнопок визарда и тексты tooltip'ов sidebar в demo.

### Build order (рекомендация, не жёсткая)
RangeSection extension (тривиально) → PickersSection (повторяет RangeRow-паттерн) → DataSection (DataTable + TreeView + mock-генератор) → LayoutSection (4 компонента, bounded-боксы) → ShowcaseApp wiring (3 вызова + удаление scratch) → sign-off UAT ×3 темы (формальный gate, последним).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner, executor) ОБЯЗАНЫ прочитать перед планированием/реализацией Phase 11.**

### Phase 11 source-of-truth (project-level)
- `.planning/ROADMAP.md` §Phase 11: Showcase + v2.0 Visual Sign-off — Goal, 5 success criteria (SC1 DataSection, SC2 PickersSection, SC3 LayoutSection, SC4 16-item checklist, SC5 «ровно 3 вызова + RangeSection extend»), 5 phase notes (PITFALL-09/10, W11-01/02, checklist = gate). **Каждый success criterion — приёмочный gate.**
- `.planning/REQUIREMENTS.md` §Showcase — **SHW-07** (DataSection / AeroDataTable, 50–200 строк, 5–6 mixed-колонок), **SHW-08** (PickersSection / 5 pickers + RangeSlider, value Text), **SHW-09** (LayoutSection / Accordion single+multi, SplitPane h+v, Sidebar mode-toggle, Wizard 3–4 шага + validation), **SHW-10** (three-theme checkpoint + 16-item checklist gate)
- `.planning/PROJECT.md` §Current Milestone v2.0 (полный feature-list CMPLX+ADVL); §Context — **каждая анимация утверждается отдельно перед реализацией**; §Key Decisions table (отдельный showcase-модуль, undecorated БЕЗ transparent, glass single drawBehind)
- `.planning/STATE.md` §v2.0 Locked Decisions; §Accumulated Context → Decisions (полный лог Phase 7–10 решений, включая API-формы всех компонентов, которые здесь wiring'уются)

### v2.0 research (authoritative — не перепроверять что покрыто)
- `.planning/research/PITFALLS.md` — **критично для sign-off**:
  - **§"Looks Done But Isn't" Checklist** (строки 455–471) — **ТОЧНЫЙ 16-item список = формальный gate Phase 11**. Каждый пункт проверяется ×3 темы. Включает: DataTable virtualization, selection-after-sort, column-resize reflow, **TreeView lazy callback** (требует TreeView в showcase), DatePicker popup position, DateRangePicker partial-state, ColorPicker round-trip, RangeSlider thumb overlap, Accordion single-mode, SplitPane clamp, **Sidebar adjacent reflow**, StepperWizard validation, AeroDark disabled cells, drag-response (HSV+RangeSlider+DataTable splitter), `transparent=true` grep=0, `AeroScrollArea` в DataTable/TreeView grep=0.
  - **PITFALL-09** (строки 222–238) — AeroDark/Classic disabled date-cells читаемы (`labelText`, не `onSurface@0.4f`); проверяется на DatePicker/DateRangePicker в PickersSection.
  - **PITFALL-10** (строки 242–258) — DataTable four-state row (normal/hover/selected/selected+hover); проверяется в DataSection, особенно AeroDark.
  - **PITFALL-11** (строки 262–279) — Sidebar = top-level sibling, НЕ внутри SplitPane; demo обязан держать Sidebar siblings'ом, collapse reflow'ит соседа.
  - **PITFALL-05/06/15** — TreeView once-only onExpand; DateRangePicker partial-state не лик; ColorPicker no-drift — все три проверяются живым demo в showcase.
  - **W11-01** (строки 416–428) — `transparent = true` grep=0 по всем v2.0 файлам (mandatory pre-sign-off).
  - **W11-02** (строки 434–449) — popup depth через border+glassBorder+two-layer bg, не `Modifier.shadow`; визуально подтверждается на sign-off.
- `.planning/research/SUMMARY.md` §Architecture + §Critical Pitfalls + §Phase Ordering
- `.planning/research/ARCHITECTURE.md` — общая структура showcase-секций v2.0
- `.planning/research/STACK.md` — Kotlin 2.1.21 / CMP 1.7.3 / JDK 17; kotlinx-datetime 0.6.2 (для value `.toString()`)

### Prior-phase CONTEXT/SUMMARY — API-формы компонентов, которые здесь wiring'уются
- `.planning/phases/08-pickers/08-CONTEXT.md` (+ SUMMARYs) — публичные сигнатуры всех 6 picker'ов: `AeroDatePicker`/`AeroTimePicker`/`AeroDateTimePicker`/`AeroDateRangePicker`/`AeroColorPicker`(+`AeroColorPickerButton`)/`AeroRangeSlider`; типы callback'ов (LocalDate/Time/DateTime, range, HEX/Color, Float)
- `.planning/phases/09-data/09-CONTEXT.md` (+ SUMMARYs) — `AeroDataTable` (`columns: List<AeroTableColumn>`, selection `Set<Any>` + `key: (T)->Any`, sort, resize) и `AeroTreeView` (`onExpand`, SnapshotStateMap once-only); package `components/datatable/`
- `.planning/phases/10-layout/10-CONTEXT.md` — `AeroAccordion` (sections data-list, hybrid mode single/multi), `AeroSplitPane` (orientation, initialSplitFraction, min sizes), `AeroSidebar` (`rememberAeroSidebarState`, `currentWidthDp`, scope DSL `item/section/divider`, header/footer), `AeroStepperWizard` (steps data-list, hybrid currentStep, onValidate-on-click, canProceed, onFinish)
- `.planning/phases/07-shared-internal-primitives/07-CONTEXT.md` — определяет `AeroPhase7Scratch.kt`/`Phase7ScratchSection` как **«deleted Phase 11»** (scratch cleanup scope этой фазы)

### Existing codebase — showcase wiring targets
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — корневой wiring: добавить `DataSection()` / `PickersSection()` / `LayoutSection()`; удалить `Phase7ScratchSection()` (строка 96) + import (строка 32)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt` — extend с `AeroRangeSlider`-рядом (паттерн `RangeRow`); НЕ менять структуру
- `showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` — reference-паттерн секции с поиском/grid (click-to-copy через AeroToastHostState) для DataSection mock-генератора
- `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` — **удалить** (:showcase scratch)
- `library/src/main/kotlin/com/mordred/aero/.../AeroPhase7Scratch.kt` — **удалить** (:library public scratch; точный путь подтвердить grep'ом в плане)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt` — reference для `AeroBadge` использования (status-badge в DataSection)

### Упреждающие правила (применяются ко всем v2.0 фазам)
- **W11-01** — `transparent = true` grep = 0 hits по всем новым/тронутым v2.0 файлам (sign-off gate-пункт).
- **No `detectDragGestures`** — drag только через `aeroDragSplitter`/`awaitPointerEventScope` (компоненты уже соблюдают; showcase не вводит нового drag-кода).
- **No `AeroScrollArea`** внутри DataTable/TreeView — grep = 0 (sign-off gate-пункт); в DataSection использовать компоненты как есть (они уже владеют LazyListState+AeroScrollBar).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`RangeRow` паттерн** (`RangeSection.kt`) — label 160dp + content row + value Text; копируется для PickersSection и расширяется в самом RangeSection.
- **Все 12 v2.0 публичных компонентов готовы** (Phases 8–10): pickers, DataTable, TreeView, Accordion, SplitPane, Sidebar, StepperWizard — Phase 11 только инстанцирует их с mock-данными, никакой новой логики компонентов.
- **`AeroBadge` (v1.0, LST-02)** — status-бейдж в DataSection; цветовые варианты.
- **v1.x поля для wizard-формы** — `AeroTextField`, `AeroCheckbox`, `AeroRadioGroup` как контент шагов StepperWizard.
- **`AeroToastHostState`** — уже прокидывается в IconsSection/OverlaysSection; доступен для click-to-copy в DataSection если нужно.
- **`rememberAeroSidebarState`** (Phase 10) — `mode` + `currentWidthDp`; кнопка в demo переключает mode.

### Established Patterns
- **Section-per-file** под `showcase/.../sections/`; `ShowcaseApp.kt` вызывает каждую секцию по разу. Phase 11 добавляет ровно 3 файла + 3 вызова (SC5).
- **`verticalScroll` Column в ShowcaseApp** — bounded-компоненты (SplitPane, Sidebar) ОБЯЗАНЫ оборачиваться в `Box` с фиксированной высотой, иначе infinite-constraint баг.
- **Value Text под/рядом с компонентом** — UAT-конвенция (RangeSection уже так делает); расширяется на все pickers (SHW-08).
- **Три темы** обязаны рендериться корректно — sign-off проверяет каждую секцию ×3 (AeroDark — самый низкий контраст, главный фокус).
- **Visual checkpoint как milestone-gate** — каждый прошлый milestone (v1.0/v1.1) подписывался через showcase eyes-on; Phase 11 = тот же ритуал, формализованный 16-item списком.

### Integration Points
- 3 новых section-файла в `showcase/.../sections/`: `DataSection.kt`, `PickersSection.kt`, `LayoutSection.kt`.
- `ShowcaseApp.kt`: +3 вызова, −1 вызов (`Phase7ScratchSection`) + −1 import; точное место вызовов — за планировщиком (логичный порядок: после существующих v1.x секций).
- `RangeSection.kt`: +1 ряд (`AeroRangeSlider`).
- Удаление: `Phase7ScratchSection.kt` (:showcase) + `AeroPhase7Scratch.kt` (:library).
- Никаких новых Gradle-зависимостей, никаких новых `AeroColorScheme` токенов, никаких изменений library-кода компонентов.

### Things Phase 11 must NOT touch
- Публичный API любого v2.0/v1.x компонента (milestone строго additive; фиксация прошла).
- Структуру существующих section-файлов (кроме точечного add в RangeSection).
- Внутреннюю логику компонентов — если sign-off вскроет баг, это отдельное gap-closure решение, не «правка по ходу».

</code_context>

<specifics>
## Specific Ideas

- **Спутниковая тематика DataSection** связывает библиотеку с исходным mordred-проектом (сеансы связи со спутниками) — не абстрактный «users/employees» датасет, а домен, из которого родился визуальный стиль.
- **TreeView обязателен, хотя SHW-07 его не называет** — это вывод из 16-item gate (пункт «TreeView lazy callback»). Сознательное расширение DataSection до полноценной «Data»-секции, иначе формальный gate непроверяем. Планировщик не должен «упростить» и выкинуть TreeView.
- **Bounded-боксы — не косметика, а требование корректности.** SplitPane и Sidebar в `verticalScroll`-колонке без фиксированной высоты дадут infinite-constraint баг (тот же класс, что PITFALL-01). Demo-боксы с явной height — обязательны.
- **Sidebar именно sibling, демо это доказывает.** Кнопка-переключатель mode + reflow соседнего контента — прямая визуализация PITFALL-11. Demo НЕ должен прятать sidebar внутрь SplitPane.
- **Value Text = реальный callback-вывод, ISO.** Не «красивый» форматированный текст, а сырое `.toString()` — UAT проверяет, что callback отдал ровно то, что ожидается (включая partial-state guard DateRangePicker).
- **Sign-off — формальный артефакт, не «посмотрели и ок».** Отдельный документ с 16×3 отметками; Phase 11 не complete без всех PASS. Каждая анимация (где demo вводит новую) — отдельный sign-off перед кодом (конвенция проекта).
- **Aero-эстетика в demo тоже важна** (project memory: aero-aesthetic-priority) — секции должны выглядеть как Win7-desktop, не generic-flat; но компоненты уже несут стиль, секции лишь компонуют.

</specifics>

<deferred>
## Deferred Ideas

- **Sidebar как глобальная навигация всего ShowcaseApp** — заманчиво, но нарушает SC5 («ровно 3 вызова, без структурных изменений») → out of scope Phase 11; возможно в отдельной showcase-переработке позже.
- **AeroDropdown popup-offset regression fix** (v1.0 carry-over) — НЕ в scope v2.0; отдельный gap-closure или v2.x.
- **Расширенный demo-датасет / генератор фейковых данных как переиспользуемая утилита** — для Phase 11 достаточно inline mock-генератора в DataSection; вынос в shared util — позже при спросе.
- **Inline-режим ColorPicker в секции** (вместо button+popup) — обе формы существуют; для секции выбран popup, inline-demo можно добавить позже если потребуется наглядность всех контролов сразу.

</deferred>

<gap_closure>
## Gap Closure — v2.0 Sign-off Defects (2026-06-18)

**Trigger:** Phase 11 sign-off (11-05) FAILED on human UAT — 16 component defects. Full list + severities in `11-SIGNOFF.md`. This section captures the decisions for the `--gaps` planning pass.

**Scope override:** The original Phase 11 boundary was strictly additive (no component-source edits). **Gap plans MAY edit `:library` component source** (per 11-SIGNOFF.md). The v2.0 "no breaking API change" lock still holds — fixes are additive params (defaults may change visible behavior, like F7 `showSeconds`) or internal bug fixes, never breaking signatures.

### Discussed decisions (4 areas + F7)

**F6 — Date format DD.MM.YYYY**
- Apply DD.MM.YYYY **everywhere a full date is rendered**: picker popups (selected-date display), `AeroDataTable` date cells, and showcase value-Text. The calendar **navigation header** ("Месяц ГГГГ" / month-name + year) stays as-is — it is not a full-date display.
- Implement via an **optional formatter/`dateFormat` param, default DD.MM.YYYY** (additive; default change is intentional/accepted).
- **Code reality (verified):** `AeroDatePicker` ALREADY exposes `formatter: (LocalDate) -> String = { it.toString() }` (AeroDatePicker.kt:63). Gap work = change that default to DD.MM.YYYY and add the equivalent param/default to the other date-bearing pickers (`AeroDateTimePicker`, `AeroDateRangePicker`) + the DataTable date column. Planner must verify each picker's current formatter surface.

**F14 — DateRangePicker same-month selection**
- Keep the **dual-month** view (no single-calendar redesign). Make **both endpoints clickable in either of the two visible months**, including both in the left month (e.g. 07.06–17.06). This relaxes the Phase 8 "leftMonth drives both, rightMonth = leftMonth+1" state machine so a same-month range is selectable.
- **Stacking threshold (< 560dp vertical stack, NEW-PICK-03) stays unchanged** — no single-month narrow fallback.

**F12 / F10 — ColorPicker**
- **F12 hue control:** add an **explicit vertical hue slider to the right of the HSV square**. Phase 7 already has a `HueSlider` primitive — investigate why it is not visible / mis-rendered in `AeroColorPicker` and surface it. (Square encodes S/V; hue needs its own discoverable control.)
- **F10 old/new swatch:** keep the left/right swatch as an **old (original) vs new (current) comparison, but add labels/tooltip** so its purpose is clear.
- **F10 styling:** give the ColorPicker popup the **same glass background + rounded corners as the other pickers** (W11-02 / `PickerPopupContainer` pattern) and **intrinsic/content width — not full-width**. (Aligns with aero-aesthetic priority.)

**F8 — Picker trigger width**
- **Verdict: showcase-only fix; `:library` NOT touched for F8.** Investigation showed pickers do not hardcode `fillMaxWidth`; they pass `modifier` through (AeroDatePicker.kt:75), and the width comes from `AeroTextField`'s `BasicTextField` taking parent constraints (decorationBox `Box(Modifier.weight(1f))`, AeroTextField.kt:93) — standard, modifier-controllable text-field behavior, not a component defect. Consumers can already constrain via `Modifier.width(...)`.
- Fix in `PickersSection`: give triggers a **compact/bounded width** so the **value-preview Text sits to the right** (RangeRow style: label 160dp │ compact trigger │ value Text). This restores the F8-blocked checklist item #5 (DatePicker popup right-align) verifiability too.

**F7 — TimePicker seconds (pre-decided 2026-06-18)**
- Add optional `showSeconds: Boolean = false` to `AeroTimePicker`; propagate to `AeroDateTimePicker`. Default `false` preserves HH:MM API. Showcase demo enables it (HH:MM:SS).

### Clear bug fixes — route straight to planning (no discussion)
Severities per 11-SIGNOFF.md. Planner owns approach; min-widths/paddings are Claude's discretion within the fix intent.
- **F2** 🟠 DataTable adjacent cells merge (`400842026-03-26`) — add explicit horizontal cell padding/separation.
- **F3** 🔴 Drag "ghosting"/doubling on column splitter AND `AeroSplitPane` divider — same artifact; likely shares root cause with F15.
- **F4** 🟠 Sort fires only on header **text**; whole header cell must be the click target.
- **F5** 🟠 TreeView expand fires only on `>` chevron; whole node row must toggle.
- **F9** 🔴 RangeSlider — dragging one thumb resets the other to default; both thumbs must hold values independently.
- **F11** 🟠 Accordion inter-section divider longer than the rounded section background; shorten to fit inside rounded bg.
- **F13** 🟠 Demo has no min/max → no disabled dates to verify checklist item 13; add a DatePicker/DateRangePicker demo **with disabled dates** (also exercises PITFALL-09 AeroDark readability).
- **F15** 🔴 Reduced drag sensitivity (cursor outpaces thumb) on RangeSlider thumbs, ColorPicker HSV square, DataTable column splitters; likely shared root cause with F3 (drag coordinate mapping / leftover offset).
- **F-RESIZE** 🔴 Column resize unbounded right (column leaves screen, table not width-clamped) + over-shrinks left ("Name" unreadable); add usable per-column **min width** + a table width bound.
- **F-WIZARD** 🔴 StepperWizard Back/Next both disabled — wizard non-interactive in demo; investigate validation gate + step-1 field input.

### Cross-cutting note
F3 + F15 (and possibly the drag side of F-RESIZE) are flagged in 11-SIGNOFF.md as a **likely shared drag root cause** (coordinate mapping / leftover offset in the `awaitPointerEventScope` manual loop). Researcher should investigate them together before splitting into separate fix plans — a single root-cause fix may resolve multiple checklist items.

</gap_closure>

<gap_canonical_refs>
## Gap Closure — Additional Canonical References

- `.planning/phases/11-showcase-v2-0-visual-sign-off/11-SIGNOFF.md` — **authoritative defect list** (16 findings F2–F-WIZARD with severities + the 16×3 sign-off table). Every gap fix must re-satisfy its checklist row eyes-on across AeroBlue/AeroDark/Classic.
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt` — existing `formatter` param (F6 seam), trigger via `AeroTextField` (F8 evidence).
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroTextField.kt` — `BasicTextField` width behavior (F8 root-cause evidence).
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroColorPicker.kt` + Phase 7 `HueSlider`/`HsvColorSquare` primitives — F12 hue-slider investigation targets.
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt` — sealed `AeroDateRangeState` / `nextRangeState` (F14 relaxation target; keep single-`onRangeSelect` PITFALL-06 guarantee intact).
- `.planning/research/PITFALLS.md` §"Looks Done But Isn't" Checklist (the gate); PITFALL-03 (drag `awaitPointerEventScope` pattern — F3/F15/F-RESIZE root cause); PITFALL-09 (disabled-date readability — F13).

</gap_canonical_refs>

---

*Phase: 11-showcase-v2-0-visual-sign-off*
*Context gathered: 2026-06-18 (gap-closure decisions appended 2026-06-18)*
