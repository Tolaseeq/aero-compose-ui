# Requirements: aero-compose-ui — v2.0.2 AeroPanelGroup

**Defined:** 2026-06-22
**Core Value:** Подключить одну Gradle-зависимость и получить полный набор Aero-styled компонентов с тремя темами, кастомной оконной хромкой, типизированными `AeroIcons` и витриной — без ручной работы над стилем.

**Milestone goal:** Добавить один аддитивный layout-компонент `AeroPanelGroup` (+ `AeroPanelSection`) — вертикальное разбиение на N секций, где любую секцию можно свернуть в полоску-заголовок (соседи забирают высоту), а границу между двумя соседними раскрытыми секциями можно перетаскивать (модель VS Code Side Bar). Строго одна фаза, без breaking changes к v2.x API.

**Locked design decisions (discuss/research):**
- Ориентация: ВЕРТИКАЛЬНАЯ (горизонталь — out of scope).
- `onLayoutChange` срабатывает на drag-end + collapse/expand (не на каждый кадр).
- `minSize` — на уровне секции (`section(minSize = …)`), дефолт-константа.
- Высота полоски-заголовка — фиксированная internal-константа ~36dp (не публичный параметр).
- Идентичность секции — явный `key` на секцию (по конвенции Compose).

## v1 Requirements

Требования milestone v2.0.2. Все мапятся на Phase 13 (единственная фаза).

### Layout & State (PNL — core layout)

- [x] **PNL-01**: Разработчик может разместить N вертикальных секций в `AeroPanelGroup` через scope-DSL (`section(key, title) { content }`)
- [x] **PNL-02**: Пользователь может свернуть раскрытую секцию в полоску-заголовок (~36dp); освободившаяся высота перераспределяется раскрытым соседям
- [x] **PNL-03**: Пользователь может повторно раскрыть свёрнутую секцию; она восстанавливает прежний размер (`lastExpandedFraction`), забирая высоту у соседей
- [x] **PNL-04**: Раскрытые секции делят доступную высоту пропорционально и переживают ресайз окна (нормализованное fraction-based состояние)
- [x] **PNL-05**: Пользователь может перетащить разделитель между двумя соседними раскрытыми секциями для их ресайза; px переносится между ними с клампом по `minSize`
- [x] **PNL-06**: Грип-разделитель рисуется ТОЛЬКО между двумя соседними раскрытыми секциями; граница рядом со свёрнутой секцией — статичный стык заголовков (без грипа, без drag, без resize-курсора)

### Behavior & API (PNL — configuration)

- [x] **PNL-07**: Collapse/expand анимируется за 200ms FastOutSlowInEasing; drag пишет размер напрямую без анимации (риск-спайк: совмещение анимации и drag без конфликта two-writer)
- [x] **PNL-08**: Состояние раскрытия — гибрид controlled/uncontrolled (`onExpandedChange == null` → uncontrolled; non-null → controlled pure renderer), строго по паттерну `AeroAccordion`
- [x] **PNL-09**: Внутреннее состояние размеров — uncontrolled; наружу экспонируется через `onLayoutChange` (срабатывает на drag-end и на collapse/expand) для персиста/восстановления
- [x] **PNL-10**: Каждая секция задаёт свой `minSize` (дефолт-константа), кламп ресайза учитывает Σ минимумов всех секций ниже разделителя (N-section clamp, не только сосед)
- [ ] **PNL-11**: Секция с `collapsible = false` не имеет шеврона и не сворачивается, но участвует в ресайзе
- [ ] **PNL-12**: При `resizable = false` разделители рендерятся без грипа и drag отключён (чистый collapse/expand)
- [x] **PNL-13**: Идентичность секции задаётся явным `key` (устойчиво к дублирующимся title и переупорядочиванию)

### Visual, Tests & Integration (PNL)

- [ ] **PNL-14**: Полоска-заголовок — `glassPanel`-поверхность с шевроном `AeroIcons.CaretRight` (0°→90° при раскрытии), опциональным `leadingIcon` и опциональным правым слотом `headerActions`; эстетика строго Win7 Aero (gloss/gradient/rounded/depth)
- [x] **PNL-15**: Краевые случаи: все секции свёрнуты → стопка заголовков сверху, остаток контейнера пустой; одна раскрыта → занимает весь `availableForExpanded`
- [x] **PNL-16**: Чистая логика (распределение px, N-section кламп, перенос доли при collapse/expand, нормализация при ресайзе) покрыта юнит-тестами по образцу `SplitClampTest`/`AccordionToggleTest` (TDD: RED → GREEN до Compose-кода)
- [ ] **PNL-17**: Showcase `LayoutSection` получает демо `AeroPanelGroup`; three-theme visual sign-off PASSED на AeroBlue / AeroDark / Classic
- [x] **PNL-18**: KDoc со ссылками на REQ-ID и PITFALL, единообразно с соседними layout-компонентами

## Future Requirements

Признанные, но отложенные за пределы v2.0.2.

### AeroPanelGroup extensions

- **PNL-HORIZ-01**: Горизонтальная ориентация (`orientation = horizontal`) — N колонок вместо строк
- **PNL-REORDER-01**: Перетаскивание секций для переупорядочивания (drag-to-reorder)
- **PNL-NEST-01**: Вложенные `AeroPanelGroup` как first-class (вложение через каллер работает, но без специального API)
- **PNL-KBD-01**: Клавиатурный ресайз/навигация по разделителям

## Out of Scope

| Feature | Reason |
|---------|--------|
| Горизонтальная ориентация | Locked: вертикаль в первую очередь; горизонталь — возможное v2.x расширение (PNL-HORIZ-01) |
| Drag-to-reorder секций | Модель VS Code Side Bar — collapse + resize, не reorder; отложено (PNL-REORDER-01) |
| `headerHeight` как публичный параметр | Фиксированная ~36dp константа достаточна; открыть позже по запросу консьюмера |
| `onLayoutChange` на каждый кадр drag | Drag-end + toggle достаточно для персиста; per-frame создаёт storage thrash |
| Клавиатурный ресайз | Как и `AeroSplitPane` keyboard-nudge — отложено до v2.x (PNL-KBD-01) |
| Carry-over AeroDropdown popup-offset fix (DROP-FIX-01) | Явно НЕ в scope v2.0.2 (single-component milestone); будущий gap-closure |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PNL-01 | Phase 13 | Complete |
| PNL-02 | Phase 13 | Complete |
| PNL-03 | Phase 13 | Complete |
| PNL-04 | Phase 13 | Complete |
| PNL-05 | Phase 13 | Complete |
| PNL-06 | Phase 13 | Complete |
| PNL-07 | Phase 13 | Complete |
| PNL-08 | Phase 13 | Complete |
| PNL-09 | Phase 13 | Complete |
| PNL-10 | Phase 13 | Complete |
| PNL-11 | Phase 13 | Pending |
| PNL-12 | Phase 13 | Pending |
| PNL-13 | Phase 13 | Complete |
| PNL-14 | Phase 13 | Pending |
| PNL-15 | Phase 13 | Complete |
| PNL-16 | Phase 13 | Complete |
| PNL-17 | Phase 13 | Pending |
| PNL-18 | Phase 13 | Complete |

**Coverage:**
- v1 requirements: 18 total
- Mapped to Phase 13: 18/18
- Unmapped: 0

---
*Requirements defined: 2026-06-22*
*Last updated: 2026-06-22 — traceability confirmed after roadmap creation (all 18 → Phase 13)*
