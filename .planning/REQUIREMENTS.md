# Requirements: aero-compose-ui — v2.0.3 PanelGroup Recompose Fix

**Defined:** 2026-06-25
**Core Value:** Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна, типизированным набором иконок и демо-витриной — без необходимости реализовывать стиль или искать совместимый icon pack самостоятельно.

## v1 Requirements

Patch milestone. Один баг-фикс + регрессионные гарантии + релиз на JitPack. Все требования покрываются одной фазой (Phase 14). Без breaking changes к v2.x API.

### Recompose Fix (RCMP)

- [x] **RCMP-01**: В режиме `Orientation.Horizontal` + controlled (`expandedKeys` + `onExpandedChange`) при рекомпозиции контента секции одновременно с активным drag разделителя секции НЕ дублируются — `N` объявленных `section(...)` рендерятся ровно как `N` полос-заголовков (раньше наблюдалось ×N: 3→9).
- [x] **RCMP-02**: Boolean-массив раскрытости для size-математики (`computeAvailablePx` / `distributePx`) вычисляется напрямую из `isExpanded(sec)` в каждой композиции, а не читается из записанного `expandedState` (`AeroPanelGroup.kt:346`).
- [x] **RCMP-03**: Синхронизация `expandedState` (нужная для uncontrolled-пути/анимаций) вынесена из тела композиции в `SideEffect`; seed-блок (`AeroPanelGroup.kt:297-320`) не мутирует читаемый в той же композиции state. Инвариант: ни один проход композиции `BoxWithConstraints` не пишет в `expandedState` / `sizePx`, которые он же читает.
- [x] **RCMP-04**: В `LayoutSection.kt` добавлен минимальный repro — горизонтальный controlled `AeroPanelGroup`, где контент одной секции читает внешний `mutableStateOf`, меняющийся одновременно с drag разделителя; демо воспроизводит баг до фикса и проходит чисто после.

### Regression Guard (REG)

- [x] **REG-01**: Vertical-путь (Phase 13) и uncontrolled-режим byte-identical по поведению — drag-resize, collapse/expand-анимации (`snap()` во время drag, `tween(200ms, FastOutSlowInEasing)` после), `onLayoutChange` (срабатывает на drag-end + toggle, не на каждый кадр), сохранение пропорций при resize окна — без визуальных и поведенческих изменений.
- [x] **REG-02**: 12 чистых JVM-юнит-тестов (`PanelGroupLogicTest`) остаются GREEN; Compose остаётся 1.7.3 (без дрейфа на 1.8.x); фикс не вводит новых зависимостей.

### Release (REL)

- [x] **REL-01**: Версия в `build.gradle.kts` поднята с `2.0.2` до `2.0.3`.
- [x] **REL-02**: Создан и запушен тег `v2.0.3` на GitHub (`Tolaseeq/aero-compose-ui`); JitPack собирает релиз, артефакт резолвится как `com.github.Tolaseeq:aero-compose-ui:2.0.3`.

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### AeroPanelGroup enhancements

- **PNL-REORDER-01**: drag-to-reorder секций
- **PNL-NEST-01**: вложенные `AeroPanelGroup` как first-class API
- **PNL-KBD-01**: клавиатурный ресайз разделителей

### Carry-over bug

- **DROP-FIX-01**: AeroDropdown popup-offset regression (открыт с v1.0)

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Drag-to-reorder / nested-API / keyboard-resize AeroPanelGroup | Не относится к этому багу; отдельный feature-milestone (PNL-REORDER/NEST/KBD-01) |
| DROP-FIX-01 (AeroDropdown popup offset) | Не связан с PanelGroup; отдельный gap-closure |
| Миграция на Compose 1.8.x | Явная регрессионная гарантия — остаёмся на 1.7.3 |
| Изменения публичного API / новые параметры | Patch-релиз: внутренний фикс, zero breaking change |
| Maven Central публикация | JitPack — текущий канал распространения |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| RCMP-01 | Phase 14 | Complete |
| RCMP-02 | Phase 14 | Roadmapped |
| RCMP-03 | Phase 14 | Roadmapped |
| RCMP-04 | Phase 14 | Complete |
| REG-01 | Phase 14 | Complete |
| REG-02 | Phase 14 | Roadmapped |
| REL-01 | Phase 14 | Roadmapped |
| REL-02 | Phase 14 | Roadmapped |

**Coverage:**
- v1 requirements: 8 total
- Mapped to phases: 8 (Phase 14, 100%)
- Unmapped: 0

---
*Requirements defined: 2026-06-25*
*Last updated: 2026-06-25 after roadmap creation (all 8 → Phase 14)*
