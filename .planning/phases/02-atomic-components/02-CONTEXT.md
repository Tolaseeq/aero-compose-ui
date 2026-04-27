# Phase 2: Atomic Components - Context

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement all 21 self-contained interactive atomic components: buttons (4), text inputs (6), selection controls (5), dropdowns (2), sliders/progress (2), list items + badges (2). Every component must be visible and interactive in the showcase — hover, focus, disabled, and pressed states must work correctly using Phase 1 theme tokens. Composite containers (AeroCard, AeroPanel, etc.), overlays, and window chrome are Phase 3.

</domain>

<decisions>
## Implementation Decisions

### Interactive state styling
- **Hover**: белый полупрозрачный оверлей поверх base-поверхности компонента — использовать токен `buttonHover = Color(0x40FFFFFF)`. Применяется ко всем интерактивным компонентам (кнопки, поля ввода, элементы списка, чипы и т.д.)
- **Focus** (Tab-навигация): рамка меняется с `glassBorder` на `borderSelected` (accent blue). Рамка чуть ярче и толще чем в rest-состоянии — однозначно читается как фокус
- **Pressed**: на усмотрение Claude — выбрать наиболее уместный вариант для конкретного типа компонента (например, более тёмный оверлей или scale(0.97f) для кнопок)
- **Disabled**: весь компонент на 40% прозрачности (contentAlpha = 0.4f) — и текст, и рамка, и фон. Mordred использует этот же подход
- **Анимации**: `animateFloatAsState` / `animateColorAsState` с LinearEasing 150ms для всех переходов между состояниями (hover on/off, focus on/off, toggle). AeroSwitch thumb также tween 150ms

### Showcase section structure
- Каждая категория компонентов — отдельный `*Section.kt` файл в `showcase/src/main/kotlin/com/mordred/showcase/sections/`: `ButtonsSection.kt`, `InputSection.kt`, `SelectionSection.kt`, `DropdownSection.kt`, `RangeSection.kt`, `ListSection.kt`
- Каждый файл заменяет соответствующий `PlaceholderSection` в `ShowcaseApp.kt`
- Шаблон строки в секции: фиксированная ширина текстового столбца (~140dp) с названием компонента слева, все варианты/состояния в ряд справа. Читается как таблица

### AeroFilePicker — нативный диалог
- Использовать `java.awt.FileDialog` — нативный OS-диалог (Windows Explorer / macOS Finder). Наиболее native-ощущение для desktop-приложения
- Режим по умолчанию: `FileDialog.LOAD` (выбор файла для открытия)
- Только один файл за раз (нет мультивыбора)
- Параметр `mode: Int` (значения `FileDialog.LOAD` / `FileDialog.SAVE`) позволяет потребителю переключить в режим сохранения

### AeroDropdown / AeroComboBox — кастомный popup
- Механизм: `androidx.compose.ui.window.Popup {}` с кастомным `PopupPositionProvider` — позиционируется относительно anchor (триггер-элемент), корректно смещается в зависимости от места открытия на экране (не выходит за границы окна)
- Содержимое popup: полностью кастомное Aero-стилизованное меню — glass-эффект (`glassEffect` модификатор или аналогичный), `glassBorder`, `cardBackground`, собственные `AeroDropdownItem` с hover через `buttonHover` токен
- Закрытие: клик за пределами popup (`focusable = true`, `onDismissRequest`) + клавиша Esc
- Клавиатурная навигация: Arrow Up/Down перемещает выделение по пунктам, Enter выбирает, Esc закрывает
- `AeroComboBox` дополнительно поддерживает ввод произвольного текста (требование DRP-01)

### Claude's Discretion
- Точная реализация pressed-состояния для каждого типа компонента
- Выбор между `Modifier.hoverable()` + `collectIsHoveredAsState()` и `MutableInteractionSource` + `collectIsPressedAsState()` для реализации состояний
- Точное значение alpha для pressed-оверлея
- Ширина фокусной рамки (1dp vs 2dp)
- Структура внутренних утилит для общих state-паттернов (если вынести в отдельный internal helper)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Mordred reference components
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/components/MordredButton.kt` — кнопки: ButtonDefaults.buttonColors, disabled-цвета, форма, padding
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/components/CompactTextField.kt` — BasicTextField с ручным border/background, decorationBox-паттерн, placeholder
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/components/MordredChip.kt` — чип: selected/unselected состояния, border
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/ColorScheme.kt` — все hex-значения токенов (источник истины)

### Phase 1 — тема и модификаторы
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — 23 токена, включая `buttonHover`, `borderSelected`, `borderDefault`, `glassBorder`, `cardBackground`
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` — `AeroTheme.colors`, `AeroTheme.typography`, `LocalAeroColors`, `LocalAeroTypography`, `staticCompositionLocalOf`
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — `glassEffect`, `glassPanel`, `glassSurface` — паттерн `drawBehind` для компонентов

### Showcase — структура Phase 1
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — точки интеграции: PlaceholderSection вызовы, которые нужно заменить на реальные секции
- `showcase/src/main/kotlin/com/mordred/showcase/sections/PlaceholderSection.kt` — шаблон секции для замены

### Requirements
- `.planning/REQUIREMENTS.md` §Buttons (BTN-01..04), §Input (INP-01..06), §Selection (SEL-01..05), §Dropdown (DRP-01..02), §Range & Progress (RNG-01..02), §Lists & Data Display (LST-01..02) — acceptance criteria

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AeroTheme.colors` / `AeroTheme.typography` — статические аксессоры; все компоненты читают токены через них, без explicit-параметров цвета
- `glassEffect` / `glassSurface` / `glassPanel` модификаторы — используются для фона popup-меню и потенциально для рамок input-полей
- `buttonHover` токен уже есть в `AeroColorScheme` — готов к использованию
- `borderSelected` токен — готов для focus-рамки
- `MordredButton.kt` — портируется как `AeroButton`/`AeroOutlinedButton`, адаптируется под `AeroTheme.colors`
- `CompactTextField.kt` — портируется как основа `AeroTextField`, нужно расширить (focus-анимация, trailing icon)
- `MordredChip.kt` — портируется как `AeroChip`

### Established Patterns
- Все публичные объявления в `:library` требуют explicit visibility (`explicitApi()`) — каждый публичный composable, класс, константа
- Кастомизация только через Kotlin `copy()`, никаких DSL и builder-классов
- Material3 bridged внутри `AeroTheme` — компоненты МОГУТ делегировать M3-примитивам (Button, BasicTextField и т.д.) там, где это уместно
- `staticCompositionLocalOf` для новых CompositionLocal (если Phase 2 добавит какие-либо)

### Integration Points
- `ShowcaseApp.kt`: 5 `PlaceholderSection(category = "...")` вызовов заменяются на реальные секции по мере реализации
- `:showcase` зависит от `:library` через `implementation(project(":library"))` — новые компоненты экспортируются автоматически
- `AeroSegmentedControl` (SEL-05) в Phase 2 станет заменой Material3 SegmentedButton в `ThemeSwitcher.kt` — одна строка swap в Phase 1

</code_context>

<specifics>
## Specific Ideas

- AeroSegmentedControl (SEL-05) должен заменить Material3 SegmentedButton в ThemeSwitcher — это подтверждает, что компонент живёт в `:library` и публично экспортируется
- AeroFilePicker в showcase: кнопка «Обзор», показывает выбранный путь в поле рядом — стандартный паттерн
- Кастомный Popup для AeroDropdown должен правильно позиционироваться (не выходить за границы окна) — PopupPositionProvider с clamp-логикой

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 02-atomic-components*
*Context gathered: 2026-04-27*
