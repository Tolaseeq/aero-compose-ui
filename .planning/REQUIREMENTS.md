# Requirements: aero-compose-ui

**Defined:** 2026-04-27
**Core Value:** Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна и showcase-витриной

## v1 Requirements

Требования первого релиза. Каждое отображается на фазу роадмапа.

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

- [ ] **SHW-01**: Отдельное Compose Desktop приложение `:showcase` запускается самостоятельно и отображает все реализованные компоненты сгруппированными по категориям
- [ ] **SHW-02**: В showcase есть переключатель тем (AeroBlue / AeroDark / Classic), мгновенно переключающий оформление всех компонентов
- [ ] **SHW-03**: Showcase обновляется параллельно каждой фазе — после фазы N новые компоненты появляются на витрине

### Buttons (кнопки)

- [ ] **BTN-01**: `AeroButton` — заполненная кнопка с glass-рамкой; поддерживает состояния enabled/disabled/hover/pressed
- [ ] **BTN-02**: `AeroOutlinedButton` — контурная кнопка с прозрачным фоном; те же состояния
- [ ] **BTN-03**: `AeroIconButton` — квадратная кнопка с иконкой; hover-подсветка через `buttonHover` токен
- [ ] **BTN-04**: `AeroToolbar` — горизонтальная панель с группой `AeroIconButton`, разделителями и glass-фоном

### Input (поля ввода)

- [ ] **INP-01**: `AeroTextField` — однострочный ввод с Aero-рамкой, placeholder, анимацией фокуса; поддерживает trailing icon
- [ ] **INP-02**: `AeroTextArea` — многострочный ввод с вертикальной прокруткой и `AeroScrollBar`
- [ ] **INP-03**: `AeroPasswordField` — поле с маскировкой символов и кнопкой показать/скрыть
- [ ] **INP-04**: `AeroNumberSpinner` — числовой ввод с кнопками ▲▼, параметрами min/max/step
- [ ] **INP-05**: `AeroSearchField` — поле с иконкой лупы и кнопкой очистки ✕, которая появляется только при наличии текста
- [ ] **INP-06**: `AeroFilePicker` — поле пути + кнопка «Обзор», открывающая нативный файловый диалог ОС

### Selection (выбор значений)

- [ ] **SEL-01**: `AeroCheckbox` — чекбокс в Aero-стиле с поддержкой tri-state (checked / unchecked / indeterminate)
- [ ] **SEL-02**: `AeroRadioButton` и `AeroRadioGroup` — группа взаимоисключающих радиокнопок
- [ ] **SEL-03**: `AeroSwitch` — тумблер вкл/выкл с анимацией перемещения ручки
- [ ] **SEL-04**: `AeroChip` — фильтр-чип с выбранным и невыбранным состоянием
- [ ] **SEL-05**: `AeroSegmentedControl` — горизонтальная группа кнопок-переключателей, ровно одна активна в момент времени

### Dropdown (выпадающие списки)

- [ ] **DRP-01**: `AeroComboBox` — редактируемый комбобокс: можно ввести текст или выбрать значение из выпадающего списка
- [ ] **DRP-02**: `AeroDropdown` — нередактируемый select с выпадающим списком значений

### Range & Progress (диапазоны и прогресс)

- [ ] **RNG-01**: `AeroSlider` — ползунок с одной ручкой, Aero-трек, tooltip со значением при перетаскивании
- [ ] **RNG-02**: `AeroProgressBar` — детерминированный с процентом и индетерминированный (бегущий блик)

### Containers (контейнеры)

- [ ] **CNT-01**: `AeroCard` — панель с glass-эффектом, тенью, скруглёнными углами; принимает произвольный `content`
- [ ] **CNT-02**: `AeroPanel` — фоновая поверхность `glassPanel` для крупных секций
- [ ] **CNT-03**: `AeroDivider` — горизонтальный и вертикальный разделитель с `borderDefault` токеном
- [ ] **CNT-04**: `AeroGroupBox` — именованная рамка вокруг группы элементов (как Windows Forms GroupBox)
- [ ] **CNT-05**: `AeroScrollArea` — прокручиваемая область с кастомным Aero-скроллбаром; поддерживает произвольный `content`
- [ ] **CNT-06**: `AeroScrollBar` — отдельный скроллбар-компонент, совместимый со стандартным Compose `ScrollState`

### Overlays & Notifications (оверлеи и уведомления)

- [ ] **OVL-01**: `AeroDialog` — модальное окно с заголовком, контентным слотом и кнопками действий
- [ ] **OVL-02**: `AeroAlertDialog` — диалог подтверждения/ошибки с иконкой и предустановленными кнопками OK/Cancel
- [ ] **OVL-03**: `AeroTooltip` — подсказка при наведении с задержкой появления и glass-фоном
- [ ] **OVL-04**: `AeroContextMenu` — контекстное меню по правой кнопке мыши с пунктами и разделителями
- [ ] **OVL-05**: `AeroToast` / `AeroSnackbar` — временное уведомление в углу экрана с автоисчезновением (настраиваемый timeout)
- [ ] **OVL-06**: `AeroNotificationBanner` — горизонтальная строка info / warning / error / success с иконкой и кнопкой закрытия
- [ ] **OVL-07**: `AeroPopover` — всплывающая панель рядом с элементом с произвольным содержимым
- [ ] **OVL-08**: `AeroDrawer` — боковая панель, выезжающая поверх основного контента с анимацией

### Navigation & Window Chrome (навигация и хромка окна)

- [ ] **NAV-01**: `AeroTitleBar` — кастомная шапка окна: Aero-градиент, кнопки Minimize/Maximize/Close с hover-эффектами, draggable область
- [ ] **NAV-02**: `AeroMenuBar` — верхнее горизонтальное меню с пунктами и выпадающими подменю
- [ ] **NAV-03**: `AeroStatusBar` — нижняя строка состояния с текстовыми секциями и цветными индикаторами
- [ ] **NAV-04**: `AeroBreadcrumb` — навигационная цепочка «Главная › Раздел › Страница» с кликабельными элементами
- [ ] **NAV-05**: `AeroTabBar` — переключатель вкладок с активным/неактивным состоянием и опциональными иконками

### Lists & Data Display (списки и данные)

- [ ] **LST-01**: `AeroListItem` — стилизованный элемент списка с hover-состоянием, поддержкой leading/trailing содержимого
- [ ] **LST-02**: `AeroBadge` / `AeroTag` — компактный ярлык-метка для статусов и числовых счётчиков

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

Updated: 2026-04-27 (roadmap created — 3 phases)

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
| SHW-01 | Phase 1 | Pending |
| SHW-02 | Phase 1 | Pending |
| SHW-03 | Phase 1 (ongoing through all phases) | Pending |
| BTN-01 | Phase 2 | Pending |
| BTN-02 | Phase 2 | Pending |
| BTN-03 | Phase 2 | Pending |
| BTN-04 | Phase 2 | Pending |
| INP-01 | Phase 2 | Pending |
| INP-02 | Phase 2 | Pending |
| INP-03 | Phase 2 | Pending |
| INP-04 | Phase 2 | Pending |
| INP-05 | Phase 2 | Pending |
| INP-06 | Phase 2 | Pending |
| SEL-01 | Phase 2 | Pending |
| SEL-02 | Phase 2 | Pending |
| SEL-03 | Phase 2 | Pending |
| SEL-04 | Phase 2 | Pending |
| SEL-05 | Phase 2 | Pending |
| DRP-01 | Phase 2 | Pending |
| DRP-02 | Phase 2 | Pending |
| RNG-01 | Phase 2 | Pending |
| RNG-02 | Phase 2 | Pending |
| LST-01 | Phase 2 | Pending |
| LST-02 | Phase 2 | Pending |
| CNT-01 | Phase 3 | Pending |
| CNT-02 | Phase 3 | Pending |
| CNT-03 | Phase 3 | Pending |
| CNT-04 | Phase 3 | Pending |
| CNT-05 | Phase 3 | Pending |
| CNT-06 | Phase 3 | Pending |
| OVL-01 | Phase 3 | Pending |
| OVL-02 | Phase 3 | Pending |
| OVL-03 | Phase 3 | Pending |
| OVL-04 | Phase 3 | Pending |
| OVL-05 | Phase 3 | Pending |
| OVL-06 | Phase 3 | Pending |
| OVL-07 | Phase 3 | Pending |
| OVL-08 | Phase 3 | Pending |
| NAV-01 | Phase 3 | Pending |
| NAV-02 | Phase 3 | Pending |
| NAV-03 | Phase 3 | Pending |
| NAV-04 | Phase 3 | Pending |
| NAV-05 | Phase 3 | Pending |

**Coverage:**
- v1 requirements: 53 total
- Phase 1: 13 (FOUND-01..10, SHW-01..03)
- Phase 2: 21 (BTN-01..04, INP-01..06, SEL-01..05, DRP-01..02, RNG-01..02, LST-01..02)
- Phase 3: 19 (CNT-01..06, OVL-01..08, NAV-01..05)
- Unmapped: 0

---
*Requirements defined: 2026-04-27*
*Last updated: 2026-04-27 — roadmap created, traceability expanded to individual requirement level*
