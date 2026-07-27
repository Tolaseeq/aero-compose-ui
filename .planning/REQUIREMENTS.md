# Requirements: aero-compose-ui — v3.0 Glass Refinement

**Defined:** 2026-07-21
**Core Value:** Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна, типизированным набором иконок и демо-витриной — без необходимости реализовывать стиль или искать совместимый icon pack самостоятельно.

**Milestone goal:** Переделать визуал восьми компонентов, читающихся как Material3, в узнаваемо-стеклянный Aero-облик — на общем слое Aero-примитивов, поверх мигрированного тулчейна. Поведение и публичный API компонентов не меняются.

**Research:** `.planning/research/SUMMARY.md` (+ STACK / FEATURES / ARCHITECTURE / PITFALLS / UPGRADE)

---

## v3.0 Requirements

### Toolchain Migration (TOOL)

Обязательная фаза, изолированная от визуальной работы: смешивать риск апгрейда зависимостей с риском кода отрисовки нельзя — иначе регрессию не отнести ни к одной причине.

- [x] **TOOL-01**: Проект собирается (`./gradlew build`) на Kotlin 2.4.10 + Compose Multiplatform 1.11.1. Это **первая задача фазы** — пара не выпускалась JetBrains как согласованная (CMP 1.11.1 вышла на шесть недель раньше Kotlin 2.4.10). При провале — эскалация пользователю с тремя названными запасными вариантами, без молчаливой подмены
- [x] **TOOL-02**: Material3 припинен к явной **стабильной** координате; алиас `compose.material3` не используется как источник версии (иначе резолвится в 1.5.0-alpha17)
- [x] **TOOL-03**: `AeroPanelGroupRecomposeUiTest` портирован под изменения тест-инфраструктуры CMP 1.11 (депрекейшн `runComposeUiTest` v1, смена дефолтного `TestDispatcher` с `Unconfined` на `Standard`)
- [x] **TOOL-04**: Портированный страж **заново доказан как падающий на несломанном коде** — фикс с не-`@Composable` DSL временно откачен, дублирование заголовков наблюдается, фикс возвращён. Без этого страж считается неработающим (урок v2.0.3)
- [x] **TOOL-05**: Весь тест-сьют библиотеки зелёный (232 теста, включая 12 `PanelGroupLogicTest`)
- [x] **TOOL-06**: Showcase компилируется и запускается; дымовой прогон на трёх темах не показывает изменений относительно до-миграционного состояния
- [x] **TOOL-07**: Сигнатуры `Modifier.dropShadow` / `Modifier.innerShadow` подтверждены компиляцией против реального артефакта 1.11.1 (scratch-composable), а не по документации
- [x] **TOOL-08**: Сборка на JitPack проходит на новом тулчейне

### Aero Primitives Foundation (PRIM)

Enabling-фаза по образцу Phase 7 из v2.0. Каждый визуальный приём зависит от этого слоя — строить компоненты раньше значит переделывать.

- [x] **PRIM-01**: `Color.lighten()` / `Color.darken()` — RGB-миксующие хелперы. Несущий примитив: `.copy(alpha=)` умеет только гасить цвет, но не осветлять, а в Classic токены непрозрачные
- [x] **PRIM-02**: `AeroOrnamentTokens` + `derive(base: AeroColorScheme)` — токены орнамента выводятся алгоритмически для всех трёх тем, а не задаются десятком литералов на тему
- [x] **PRIM-03**: `AeroColorScheme` расширен исходно-совместимо — замыкающее `ornamentOverride: AeroOrnamentTokens? = null` как escape-hatch; существующие вызовы конструктора не ломаются
- [x] **PRIM-04**: `AeroSurfaceStyle` описывает поверхность декларативно: стопы заливки, глянец, бевель, рим, тень
- [x] **PRIM-05**: Одна `internal fun DrawScope.drawAeroSurfaceCore(style, cornerPx)` — единственная реализация, наружу двумя путями: `Modifier.aeroSurface(style, shape)` для компонентов-Box'ов и прямой вызов для владеющих Canvas
- [x] **PRIM-06**: `Modifier.aeroGlowRing` — общий примитив свечения для hover/focus
- [x] **PRIM-07**: Примитив выпуклой ручки (`aeroThumbSurface` / `drawAeroThumb`) — гейтит `AeroSwitch`, `AeroSlider`, `AeroRangeSlider`
- [x] **PRIM-08**: Примитив утопленного желобка трека — гейтит `AeroSwitch`, оба слайдера и `AeroProgressBar`
- [x] **PRIM-09**: Глянец `glassSurface` пропорционален высоте компонента; пиксельный литерал `endY = 100f` устранён
- [x] **PRIM-10**: Бордер `glassSurface` рисуется на всю заявленную толщину — порядок отрисовки и `.clip()` исправлен (сейчас внешняя половина 1.dp-штриха срезается)
- [x] **PRIM-11**: `glassEffect(elevation)` либо действительно рисует тень, либо параметр удалён. Мёртвых параметров не остаётся
- [x] **PRIM-12**: Клиппинг централизован внутри `aeroSurface()` — `.clip(shape)` внешний по отношению ко всей собственной отрисовке компонента, а не выводится заново на каждом месте вызова
- [x] **PRIM-13**: Геометрия и кисти подняты в `drawWithCache`; `Brush` не пересоздаётся на каждом кадре отрисовки
- [x] **PRIM-14**: Все новые градиенты гаснут в `baseColor.copy(alpha = 0f)`, а не в захардкоженный `Color.Transparent` — иначе Classic рисует плоский цветной блок вместо стекла
- [x] **PRIM-15**: `InteractionStates.kt` перенесён в `components/common/` и расширен `rememberAeroInteractionState()` — единая точка подключения состояний для всех восьми
- [x] **PRIM-16**: Каждый новый примитив проверен на трёх темах на первой же итерации, а не отложен до приёмки
- [x] **PRIM-17**: Дымовой прогон **всей библиотеки (~50 компонентов)**, а не только восьми целевых — починка `GlassModifiers.kt` перерисовывает всех потребителей `glassSurface`/`glassPanel`/`glassEffect`
- [x] **PRIM-18**: Спайк: слоты `thumb`/`track` нестандартного размера вписываются во внутреннюю раскладку M3 `Slider` без обрезки и смещения. Несущее допущение исправленной архитектуры `AeroSlider` — при провале откат к полному удалению M3 для этого компонента

### Buttons (VBTN)

- [x] **VBTN-01**: `AeroButton` — двухтоновая заливка со швом, верхний глянец, внутренний бевель, внешний контур
- [x] **VBTN-02**: `AeroButton` — состояния hover / press / focus / disabled в идиоме Aero (свечение при наведении, инверсия градиента при нажатии)
- [x] **VBTN-03**: Оверлей ховера клипуется по форме компонента — квадратные углы на скруглённой кнопке устранены (сейчас `drawWithContent` вешается снаружи внутреннего клипа M3)
- [x] **VBTN-04**: Семантика `Role.Button` и активация с клавиатуры сохранены — контейнер M3 снимается, но `Modifier.clickable(role = Role.Button, ...)` остаётся. Прецедент `AeroRangeSlider` с нулевой семантикой не повторяется
- [x] **VBTN-05**: `AeroOutlinedButton` — эквивалентная отделка в outlined-варианте
- [x] **VBTN-06**: Общая internal-композабл поверхность для обеих кнопок — визуально разойтись они не могут

### Range (VRNG)

- [x] **VRNG-01**: `AeroSlider` **сохраняет** M3 `Slider` и передаёт собственные `thumb =` / `track =` слоты — исправленная архитектура, не полное удаление M3
- [x] **VRNG-02**: Поведение `AeroSlider` не регрессирует: drag, стрелки клавиатуры, привязка к `steps`, `onValueChangeFinished`, семантика. Параметр `steps` не становится мёртвым
- [x] **VRNG-03**: `AeroSlider` — утопленный желобок трека, выпуклая ручка, hover/focus на ручке
- [x] **VRNG-04**: `AeroRangeSlider` — желобок и выпуклые ручки; отрисовка меняется, логика drag не трогается
- [x] **VRNG-05**: `AeroRangeSlider` — hover/press отдельно для каждой ручки
- [x] **VRNG-06**: `AeroProgressBar` — утопленное ложе трека, градиентная заливка с глянцем
- [x] **VRNG-07**: `AeroProgressBar` — периодический блик опционален, **по умолчанию выключен**
- [x] **VRNG-08**: Indeterminate-режим перерисован; тайминг 1500ms restart сохранён, ping-pong не вводится
- [x] **VRNG-09**: Там, где анимация и drag пишут одно значение, переиспользован локнутый Pattern 3 (анимация читает цель, drag пишет напрямую, `isDragging` переключает на `snap()`)

### Selectors + Lists (VSEL / VLST)

- [x] **VSEL-01**: `AeroSwitch` — желобок трека, выпуклая ручка с глянцем и тенью
- [x] **VSEL-02**: `AeroSwitch` — hover / press / focus впервые (сейчас отсутствуют полностью)
- [x] **VSEL-03**: `AeroSegmentedControl` — выбранный сегмент **утоплен**: инвертированный градиент плюс внутренняя тень, переиспользуя код нажатой кнопки
- [x] **VSEL-04**: `AeroSegmentedControl` — hover и focus впервые
- [x] **VLST-01**: `AeroListItem` — выделение клипуется в скруглённую «пилюлю» с градиентом и римом (сейчас не клипуется вовсе)
- [x] **VLST-02**: Ховер виден на выделенной строке — комбинированное состояние (сейчас выделение полностью подавляет ховер)
- [x] **VLST-03**: `AeroListItem` — визуал фокуса
- [x] **VLST-04**: Компоненты, получающие ховер впервые, копируют уже корректную связку `Modifier.hoverable` + `collectIsHoveredAsState` из `AeroListItem`, а не изобретают отслеживание позиции указателя

### Showcase + Verification (SHW / VER)

- [ ] **SHW-15**: Showcase демонстрирует обновлённый вид всех восьми компонентов во всех состояниях, включая hover/press/focus
- [ ] **SHW-16**: Человеческая приёмка на трёх темах (AeroBlue / AeroDark / Classic), минимум один прогон при масштабе DPI, отличном от 100%
- [ ] **VER-01**: Grep-гейт: в стопах градиентов нет пиксельных литералов
- [ ] **VER-02**: Grep-гейт: никто не обходит централизованный порядок клиппинга `aeroSurface()`
- [ ] **VER-03**: Snapshot-тест дефолтов против до-миграционного базиса — размеры и радиусы скругления не поползли незаметно
- [ ] **VER-04**: UI-тест активации с клавиатуры для двух переведённых кнопок
- [ ] **VER-05**: Минимальный scratch-потребитель вне соглашений showcase — ни `aska`, ни `satellite-control` эту веху не отслеживают, поэтому исторически сильнейший ловец регрессий недоступен
- [ ] **VER-06**: Каждый новый гейт доказан как падающий на несломанном коде

---

## Future Requirements

Отложено осознанно, вне текущего роадмапа.

### AeroListItem

- **VLST-F01**: Зеркальное отражение по нижнему краю выделенной строки — лишний слой отрисовки на каждую строку длинного списка

### Progress

- **VRNG-F01**: Win7-аутентичный ping-pong для indeterminate (полоса с замедлением у краёв)

### Прочие компоненты

- **VIS-F01**: Ревизия визуала остальных ~40 компонентов библиотеки на «материальность»

### Carry-over из прошлых вех

- **DROP-FIX-01**: Регресс смещения popup у `AeroDropdown` (тянется с v1.0)
- **PNL-REORDER-01**: Перетаскивание секций `AeroPanelGroup`
- **PNL-NEST-01**: Вложенные `AeroPanelGroup` как first-class API
- **PNL-KBD-01**: Клавиатурный ресайз разделителей

---

## Out of Scope

| Feature | Reason |
|---------|--------|
| Обратная совместимость с потребителями на Compose 1.7.3 | Решение пользователя: они мигрируют отдельно либо остаются на `2.0.4`. Причина мажорной версии |
| Настоящий DWM backdrop blur | Требует JNI/WinAPI; симуляция градиентами визуально достаточна (локнуто с v1.0) |
| Шум / зернистая текстура | Стоимость отрисовки не оправдана; тянет в устаревшую скевоморфную подачу |
| AGSL / `RuntimeShader` | Android-only. Десктопный аналог (SkSL) существует, но для градиентно-достижимых эффектов избыточен |
| Screenshot-регрессия (Roborazzi / Paparazzi) | Обе Android-only, на Compose Desktop не работают. Пиксельный diff признан хрупким — оставляем человеческую приёмку плюс механические гейты |
| Изменение поведения и сигнатур компонентов | Веха визуальная; функционал признан хорошим |
| Смена размеров и радиусов по умолчанию сверх необходимого | Сдвигает раскладку у потребителей; охраняется VER-03 |

---

## Traceability

Roadmap: `.planning/ROADMAP.md` (created 2026-07-21, Phases 15–20).

| Requirement | Phase | Status |
|-------------|-------|--------|
| TOOL-01..08 | Phase 15 (Toolchain Upgrade) | Complete |
| PRIM-01..18 | Phase 16 (Foundation — Aero Primitives Layer) | Pending |
| VBTN-01..06 | Phase 17 (Buttons) | Complete |
| VRNG-01..09 | Phase 18 (Range) | Pending |
| VSEL-01..04, VLST-01..04 | Phase 19 (Selectors + Lists) | Pending |
| SHW-15..16, VER-01..06 | Phase 20 (Verification) | Pending |

**Coverage:**

- v3.0 requirements: 57 total (TOOL 8 + PRIM 18 + VBTN 6 + VRNG 9 + VSEL 4 + VLST 4 + SHW 2 + VER 6)
- Mapped to phases: 57/57 — validated by roadmapper against the enumerated REQ-IDs above (per-category sum), not the pre-roadmap estimate
- Unmapped: 0

**Correction (2026-07-21, roadmap creation):** this section previously stated "53 total" as a pre-roadmap estimate. The actual enumerated REQ-ID count across all eight categories in this document sums to 57, confirmed during roadmap creation. No requirements were added or removed — this is a recount, not a scope change.

---
*Requirements defined: 2026-07-21*
*Last updated: 2026-07-21 after roadmap creation — traceability filled in, coverage count corrected 53→57*
