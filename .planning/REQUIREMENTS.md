# Requirements: aero-compose-ui

**Defined:** 2026-04-27
**Core Value:** Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна и showcase-витриной

## v1.0 Requirements (shipped)

Требования первого релиза. Все 53 завершены и проверены через showcase в Phases 1–3.

### Foundation (система тем и токены)

- [x] **FOUND-01**: Разработчик может подключить `AeroTheme {}` как корневой провайдер и все вложенные компоненты автоматически получают цвета и типографику активной темы
- [x] **FOUND-02**: `AeroColorScheme` — `@Immutable` data class с полным набором цветовых токенов (primary, surface, glass, border, status, hover и др.)
- [x] **FOUND-03**: Тема **AeroBlue** включена по умолчанию (фон #0D1B2A, акцент #4FC3F7) — перенос из mordred
- [x] **FOUND-04**: Тема **AeroDark** включена по умолчанию (фон #0A0A1A, акцент #90CAF9) — перенос из mordred
- [x] **FOUND-05**: Тема **Classic** включена по умолчанию (фон #1E1E1E, акцент #5C8ABF) — перенос из mordred
- [x] **FOUND-06**: Разработчик может создать собственную тему, передав кастомный `AeroColorScheme` в `AeroTheme`
- [x] **FOUND-07**: Модификатор `Modifier.glassEffect()` применяет эффект стекла (градиент + рамка + тень) через единый `drawBehind`-блок без overdraw
- [x] **FOUND-08**: Модификаторы `Modifier.glassPanel()` и `Modifier.glassSurface()` доступны для секций и поверхностей
- [x] **FOUND-09**: `AeroTypography` — data class с набором `TextStyle` (title 18sp, body 14sp/13sp/12sp, label 11sp)
- [x] **FOUND-10**: `explicitApi()` объявлен в `:library` — все публичные API явно помечены `public`

### Showcase (демо-витрина)

- [x] **SHW-01**: Отдельное Compose Desktop приложение `:showcase` запускается самостоятельно и отображает все реализованные компоненты сгруппированными по категориям
- [x] **SHW-02**: В showcase есть переключатель тем (AeroBlue / AeroDark / Classic), мгновенно переключающий оформление всех компонентов
- [x] **SHW-03**: Showcase обновляется параллельно каждой фазе — после фазы N новые компоненты появляются на витрине

### Buttons (кнопки)

- [x] **BTN-01**: `AeroButton` — заполненная кнопка с glass-рамкой; поддерживает состояния enabled/disabled/hover/pressed
- [x] **BTN-02**: `AeroOutlinedButton` — контурная кнопка с прозрачным фоном; те же состояния
- [x] **BTN-03**: `AeroIconButton` — квадратная кнопка с иконкой; hover-подсветка через `buttonHover` токен
- [x] **BTN-04**: `AeroToolbar` — горизонтальная панель с группой `AeroIconButton`, разделителями и glass-фоном

### Input (поля ввода)

- [x] **INP-01**: `AeroTextField` — однострочный ввод с Aero-рамкой, placeholder, анимацией фокуса; поддерживает trailing icon
- [x] **INP-02**: `AeroTextArea` — многострочный ввод с вертикальной прокруткой и `AeroScrollBar`
- [x] **INP-03**: `AeroPasswordField` — поле с маскировкой символов и кнопкой показать/скрыть
- [x] **INP-04**: `AeroNumberSpinner` — числовой ввод с кнопками ▲▼, параметрами min/max/step
- [x] **INP-05**: `AeroSearchField` — поле с иконкой лупы и кнопкой очистки ✕, которая появляется только при наличии текста
- [x] **INP-06**: `AeroFilePicker` — поле пути + кнопка «Обзор», открывающая нативный файловый диалог ОС

### Selection (выбор значений)

- [x] **SEL-01**: `AeroCheckbox` — чекбокс в Aero-стиле с поддержкой tri-state (checked / unchecked / indeterminate)
- [x] **SEL-02**: `AeroRadioButton` и `AeroRadioGroup` — группа взаимоисключающих радиокнопок
- [x] **SEL-03**: `AeroSwitch` — тумблер вкл/выкл с анимацией перемещения ручки
- [x] **SEL-04**: `AeroChip` — фильтр-чип с выбранным и невыбранным состоянием
- [x] **SEL-05**: `AeroSegmentedControl` — горизонтальная группа кнопок-переключателей, ровно одна активна в момент времени

### Dropdown (выпадающие списки)

- [x] **DRP-01**: `AeroComboBox` — редактируемый комбобокс: можно ввести текст или выбрать значение из выпадающего списка
- [x] **DRP-02**: `AeroDropdown` — нередактируемый select с выпадающим списком значений

### Range & Progress (диапазоны и прогресс)

- [x] **RNG-01**: `AeroSlider` — ползунок с одной ручкой, Aero-трек, tooltip со значением при перетаскивании
- [x] **RNG-02**: `AeroProgressBar` — детерминированный с процентом и индетерминированный (бегущий блик)

### Containers (контейнеры)

- [x] **CNT-01**: `AeroCard` — панель с glass-эффектом, тенью, скруглёнными углами; принимает произвольный `content`
- [x] **CNT-02**: `AeroPanel` — фоновая поверхность `glassPanel` для крупных секций
- [x] **CNT-03**: `AeroDivider` — горизонтальный и вертикальный разделитель с `borderDefault` токеном
- [x] **CNT-04**: `AeroGroupBox` — именованная рамка вокруг группы элементов (как Windows Forms GroupBox)
- [x] **CNT-05**: `AeroScrollArea` — прокручиваемая область с кастомным Aero-скроллбаром; поддерживает произвольный `content`
- [x] **CNT-06**: `AeroScrollBar` — отдельный скроллбар-компонент, совместимый со стандартным Compose `ScrollState`

### Overlays & Notifications (оверлеи и уведомления)

- [x] **OVL-01**: `AeroDialog` — модальное окно с заголовком, контентным слотом и кнопками действий
- [x] **OVL-02**: `AeroAlertDialog` — диалог подтверждения/ошибки с иконкой и предустановленными кнопками OK/Cancel
- [x] **OVL-03**: `AeroTooltip` — подсказка при наведении с задержкой появления и glass-фоном
- [x] **OVL-04**: `AeroContextMenu` — контекстное меню по правой кнопке мыши с пунктами и разделителями
- [x] **OVL-05**: `AeroToast` / `AeroSnackbar` — временное уведомление в углу экрана с автоисчезновением (настраиваемый timeout)
- [x] **OVL-06**: `AeroNotificationBanner` — горизонтальная строка info / warning / error / success с иконкой и кнопкой закрытия
- [x] **OVL-07**: `AeroPopover` — всплывающая панель рядом с элементом с произвольным содержимым
- [x] **OVL-08**: `AeroDrawer` — боковая панель, выезжающая поверх основного контента с анимацией

### Navigation & Window Chrome (навигация и хромка окна)

- [x] **NAV-01**: `AeroTitleBar` — кастомная шапка окна: Aero-градиент, кнопки Minimize/Maximize/Close с hover-эффектами, draggable область
- [x] **NAV-02**: `AeroMenuBar` — верхнее горизонтальное меню с пунктами и выпадающими подменю
- [x] **NAV-03**: `AeroStatusBar` — нижняя строка состояния с текстовыми секциями и цветными индикаторами
- [x] **NAV-04**: `AeroBreadcrumb` — навигационная цепочка «Главная › Раздел › Страница» с кликабельными элементами
- [x] **NAV-05**: `AeroTabBar` — переключатель вкладок с активным/неактивным состоянием и опциональными иконками

### Lists & Data Display (списки и данные)

- [x] **LST-01**: `AeroListItem` — стилизованный элемент списка с hover-состоянием, поддержкой leading/trailing содержимого
- [x] **LST-02**: `AeroBadge` / `AeroTag` — компактный ярлык-метка для статусов и числовых счётчиков

## v1.1 Requirements

Milestone v1.1 — Icon System. Замена всех текстовых символов-иконок и Material Icons на единый набор векторных иконок `AeroIcons` (порт Phosphor Regular, ~138 шт.). Подробности — в `.planning/research/SUMMARY.md`.

### Icon Set (Foundation)

- [x] **ICN-01**: `public object AeroIcons` существует в `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt`; каждая иконка — `public val Name: ImageVector` с lazy backing-property pattern (private nullable `_Name` + getter, как в Material Icons Extended); explicitApi-совместимо
- [x] **ICN-02**: KDoc на `AeroIcons` объясняет именование (Phosphor kebab-case → Kotlin PascalCase: `AeroIcons.X` вместо `Close`, `CaretDown` вместо `ChevronDown`, `Gear` вместо `Settings`, `House` вместо `Home`, `Funnel` вместо `Filter`, `MagnifyingGlass` вместо `Search`) и рекомендации по размеру (16/20/24dp); упоминает обязательность явного `tint` на тёмных темах
- [x] **ICN-03**: 138 ImageVector-констант (порт Phosphor Regular, viewBox 256×256, stroke 16, rounded caps/joins, `defaultWidth=defaultHeight=24.dp`) доступны через `AeroIcons.*` autocomplete; полный список соответствует Part 3 из `.planning/research/FEATURES.md`

### Component Migration (Library)

- [x] **MIG-01**: `AeroCheckbox` — `Icon(AeroIcons.Check)` (checked) и `Icon(AeroIcons.Minus)` (indeterminate) вместо `Text("✓")` / `Text("–")`
- [x] **MIG-02**: `AeroDropdown` — `Icon(AeroIcons.CaretDown)` вместо `Text("▼")`
- [x] **MIG-03**: `AeroNumberSpinner` — `Icon(AeroIcons.CaretUp)` / `Icon(AeroIcons.CaretDown)` вместо `Text("▲")` / `Text("▼")`; sub-pixel pitfall учтён (либо размер кнопки увеличен, либо эквивалентное визуальное решение, проверяется на UAT)
- [x] **MIG-04**: `AeroTitleBar` window controls — `AeroIcons.X` (close), `AeroIcons.Minus` (minimize), `AeroIcons.Square` (maximize), `AeroIcons.FrameCorners` (restore from maximized); внутренний `TitleBarButton` принимает `ImageVector` вместо `String`
- [x] **MIG-05**: `AeroContextMenu` submenu indicator — `Icon(AeroIcons.CaretRight)` вместо `Text("▶")`
- [x] **MIG-06**: `AeroToastHost` close-кнопка — `Icon(AeroIcons.X)` вместо `Text("✕")`
- [x] **MIG-07**: `AeroNotificationBanner` close-кнопка — `Icon(AeroIcons.X)` вместо `Text("✕")`
- [x] **MIG-08**: `AeroSearchField` — `Icon(AeroIcons.MagnifyingGlass)` для лупы и `Icon(AeroIcons.X)` для clear-кнопки; private composable `SearchIcon()` (Canvas) удалён
- [x] **MIG-09**: `AeroPasswordField` — `Icon(AeroIcons.Eye)` / `Icon(AeroIcons.EyeSlash)` для toggle show/hide; private composables `EyeOpenIcon()` / `EyeClosedIcon()` (Canvas) удалены
- [x] **MIG-10**: `AeroAlertKind` enum — поле `icon: ImageVector` ссылается на `AeroIcons.Info` / `AeroIcons.Warning` / `AeroIcons.XCircle` / `AeroIcons.Question` вместо `Icons.Outlined.*`
- [x] **MIG-11**: `AeroBannerKind` enum — поле `icon: ImageVector` ссылается на `AeroIcons.Info` / `AeroIcons.Warning` / `AeroIcons.XCircle` / `AeroIcons.CheckCircle` вместо `Icons.Outlined.*`

### Dependency Cleanup

- [x] **CLN-01**: Тесты `AeroAlertKindTest.kt` и `AeroBannerKindTest.kt` обновлены — используют `AeroIcons.*` (предусловие для CLN-02; их компиляция ломается без рефакторинга)
- [x] **CLN-02**: Строка `implementation(compose.materialIconsExtended)` удалена из `library/build.gradle.kts`; `./gradlew :library:dependencies --configuration compileClasspath` не показывает `material-icons-extended` ни на одном уровне
- [x] **CLN-03**: `grep -rn "androidx.compose.material.icons" library/src/` возвращает 0 результатов в production-коде (включая тесты)

### Showcase

- [x] **SHW-04**: Showcase содержит `IconsSection` с `LazyVerticalGrid` всех 138 иконок (имя + рендер, минимум 80dp ширина ячейки); проходит визуальный checkpoint во всех трёх темах (AeroBlue / AeroDark / Classic)
- [x] **SHW-05**: `IconsSection` имеет `AeroSearchField` сверху, фильтрующий иконки по подстроке имени (case-insensitive, real-time); пустая выдача показывает текстовое сообщение «не найдено»
- [x] **SHW-06**: `ButtonsSection` демо-глифы (`▲ ▼ ×`) заменены на `AeroIcons.*` (демонстрация AeroIconButton с реальной векторной иконкой)

## v2 Requirements

Отложены на следующий этап. Сложные компоненты без референсных реализаций в Compose Desktop.

### Complex Stateful Components

- **CMPLX-01**: `AeroDataTable` — таблица с заголовками колонок, сортировкой, выделением строк, виртуализацией
- **CMPLX-02**: `AeroTreeView` — дерево с раскрытием/свёрткой узлов, поддержкой иконок
- **CMPLX-03**: `AeroDatePicker` — календарь с выбором числа/месяца/года
- **CMPLX-04**: `AeroTimePicker` — выбор часов и минут (крутилки или поля)
- **CMPLX-05**: `AeroDateTimePicker` — комбинированный выбор даты + времени
- **CMPLX-06**: `AeroDateRangePicker` — двойной календарь для выбора диапазона
- **CMPLX-07**: `AeroColorPicker` — палитра + RGB-ползунки + HEX-ввод
- **CMPLX-08**: `AeroRangeSlider` — ползунок с двумя ручками (от–до)

### Advanced Layout

- **ADVL-01**: `AeroAccordion` — сворачиваемый раздел с анимацией раскрытия
- **ADVL-02**: `AeroSplitPane` — две панели с перетаскиваемым разделителем (горизонтальный / вертикальный)
- **ADVL-03**: `AeroSidebar` — боковая панель навигации с иконками и подписями, опционально сворачиваемая
- **ADVL-04**: `AeroStepperWizard` — шаговый процесс: Шаг 1 → Шаг 2 → … с прогресс-индикатором

## Out of Scope

| Feature | Reason |
|---------|--------|
| Android / iOS / Web таргеты | Compose Desktop only — добавление мобильных таргетов меняет архитектуру |
| Настоящий DWM Aero blur (через JNI/WinAPI) | Требует нативного кода; симуляция через градиенты визуально достаточна |
| Публикация в Maven Central | Только локальный Maven для v1; Maven Central — отдельный процесс с подписанием |
| i18n / RTL | На усмотрение разработчика-потребителя |
| WCAG-совместимость (гарантии) | Цвета Aero-тем не оптимизированы под контрастность |
| Aero Snap на кастомном окне | `WindowDraggableArea` не передаёт HTCAPTION OS — известное ограничение, документируется |
| Avatar / Rating (звёзды) | Низкий приоритет, пользователь не выбрал |

## Traceability

Updated: 2026-04-29 (v1.1 roadmap created — Phases 4-6; all 17 v1.1 requirements mapped)

| Requirement | Phase | Status |
|-------------|-------|--------|
| FOUND-01 | Phase 1 | Complete |
| FOUND-02 | Phase 1 | Complete |
| FOUND-03 | Phase 1 | Complete |
| FOUND-04 | Phase 1 | Complete |
| FOUND-05 | Phase 1 | Complete |
| FOUND-06 | Phase 1 | Complete |
| FOUND-07 | Phase 1 | Complete |
| FOUND-08 | Phase 1 | Complete |
| FOUND-09 | Phase 1 | Complete |
| FOUND-10 | Phase 1 | Complete |
| SHW-01 | Phase 1 | Complete |
| SHW-02 | Phase 1 | Complete |
| SHW-03 | Phase 1 (ongoing through all phases) | Complete |
| BTN-01 | Phase 2 | Complete |
| BTN-02 | Phase 2 | Complete |
| BTN-03 | Phase 2 | Complete |
| BTN-04 | Phase 2 | Complete |
| INP-01 | Phase 2 | Complete |
| INP-02 | Phase 2 | Complete |
| INP-03 | Phase 2 | Complete |
| INP-04 | Phase 2 | Complete |
| INP-05 | Phase 2 | Complete |
| INP-06 | Phase 2 | Complete |
| SEL-01 | Phase 2 | Complete |
| SEL-02 | Phase 2 | Complete |
| SEL-03 | Phase 2 | Complete |
| SEL-04 | Phase 2 | Complete |
| SEL-05 | Phase 2 | Complete |
| DRP-01 | Phase 2 | Complete |
| DRP-02 | Phase 2 | Complete |
| RNG-01 | Phase 2 | Complete |
| RNG-02 | Phase 2 | Complete |
| LST-01 | Phase 2 | Complete |
| LST-02 | Phase 2 | Complete |
| CNT-01 | Phase 3 | Complete |
| CNT-02 | Phase 3 | Complete |
| CNT-03 | Phase 3 | Complete |
| CNT-04 | Phase 3 | Complete |
| CNT-05 | Phase 3 | Complete |
| CNT-06 | Phase 3 | Complete |
| OVL-01 | Phase 3 | Complete |
| OVL-02 | Phase 3 | Complete |
| OVL-03 | Phase 3 | Complete |
| OVL-04 | Phase 3 | Complete |
| OVL-05 | Phase 3 | Complete |
| OVL-06 | Phase 3 | Complete |
| OVL-07 | Phase 3 | Complete |
| OVL-08 | Phase 3 | Complete |
| NAV-01 | Phase 3 | Complete |
| NAV-02 | Phase 3 | Complete |
| NAV-03 | Phase 3 | Complete |
| NAV-04 | Phase 3 | Complete |
| NAV-05 | Phase 3 | Complete |
| ICN-01 | Phase 4 | Complete |
| ICN-02 | Phase 4 | Complete |
| ICN-03 | Phase 4 | Complete |
| MIG-01 | Phase 5 | Complete |
| MIG-02 | Phase 5 | Complete |
| MIG-03 | Phase 5 | Complete |
| MIG-04 | Phase 5 | Complete |
| MIG-05 | Phase 5 | Complete |
| MIG-06 | Phase 5 | Complete |
| MIG-07 | Phase 5 | Complete |
| MIG-08 | Phase 5 | Complete |
| MIG-09 | Phase 5 | Complete |
| MIG-10 | Phase 5 | Complete |
| MIG-11 | Phase 5 | Complete |
| CLN-01 | Phase 5 | Complete |
| CLN-02 | Phase 5 | Complete |
| CLN-03 | Phase 5 | Complete |
| SHW-04 | Phase 6 | Complete |
| SHW-05 | Phase 6 | Complete |
| SHW-06 | Phase 6 | Complete |

**Coverage:**
- v1.0 requirements: 53 (all Complete) — Phase 1 (13) + Phase 2 (21) + Phase 3 (19)
- v1.1 requirements: 17 total — all mapped
  - Phase 4: ICN-01, ICN-02, ICN-03 (3 requirements)
  - Phase 5: MIG-01..11, CLN-01..03 (14 requirements)
  - Phase 6: SHW-04, SHW-05, SHW-06 (3 requirements)
- Unmapped: 0 (coverage 100%)

---
*Requirements defined: 2026-04-27 (v1.0); 2026-04-28 (v1.1 Icon System)*
*Last updated: 2026-04-29 — v1.1 phase mapping complete; all 17 requirements assigned to Phases 4-6*
