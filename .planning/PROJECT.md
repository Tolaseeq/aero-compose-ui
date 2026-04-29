# aero-compose-ui

## What This Is

Библиотека UI-компонентов для Compose Desktop, выдержанная в визуальном стиле Windows Aero (Windows 7): стеклянные градиентные поверхности, полупрозрачные панели, анимации и кастомная оконная хромка. Публикуется как Maven/JAR-артефакт (`com.mordred:aero-compose-ui`), подключается через Gradle. Содержит ~50 компонентов и три встроенные темы с поддержкой кастомизации цветов.

## Core Value

Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна и демо-витриной — без необходимости реализовывать стиль самостоятельно.

## Current Milestone: v1.1 Icon System

**Goal:** Полностью убрать текстовые символы-иконки (✕, ▲▼, ›, ▶, ✓ и т.п.) из библиотеки и showcase, ввести единый набор векторных иконок `AeroIcons` в мягком outline-стиле (порт Phosphor Regular, ~120–150 шт.), и заменить им как существующие глифы, так и Material Icons в `AeroAlertKind`/`AeroBannerKind`.

**Target features:**
- `AeroIcons` — типизированные `ImageVector`-константы (`AeroIcons.Close`, `AeroIcons.ChevronDown` и т.д.), порт **Phosphor Regular** (~stroke 1.5px, 256×256 viewBox, rounded caps/joins — мягче и ближе к Win7-glyph-эстетике, чем Feather/Lucide flat-outline)
- ~120–150 иконок: closed set покрывает все текущие места + общеупотребимые (edit/delete/save/copy/settings/user/home/file/calendar/clock/etc.) + домен-специфичные (bluetooth/wifi/battery/play/pause/print/mail/cloud/database/etc.)
- Миграция всех компонентов с текстовых глифов на `AeroIcons` (AeroCheckbox, AeroDropdown, AeroComboBox, AeroNumberSpinner, AeroTitleBar, AeroContextMenu, AeroToastHost, AeroNotificationBanner)
- Замена встроенных Canvas-glyphs в AeroSearchField (лупа, ✕) и AeroPasswordField (eye/eye-off) на `Icon(AeroIcons.*)`
- Замена `Icons.Outlined.*` на `AeroIcons.*` в `AeroAlertKind` и `AeroBannerKind`; удаление зависимости `compose.materialIconsExtended` из `:library`
- `IconsSection` в showcase — сетка всех иконок + поиск через `AeroSearchField`
- Иконки живут в том же модуле `:library` (тот же JAR `com.mordred:aero-compose-ui`)

## Requirements

### Validated

<!-- Shipped in v1.0 (53 requirements). See REQUIREMENTS.md for full list. -->

- ✓ **Foundation** (10): AeroTheme, AeroColorScheme, 3 темы, glass modifiers, AeroTypography, explicitApi — Phase 1
- ✓ **Buttons + Inputs + Selection + Dropdowns + Range + Lists** (21): BTN/INP/SEL/DRP/RNG/LST — Phase 2
- ✓ **Containers + Overlays + Navigation** (19): CNT/OVL/NAV — Phase 3
- ✓ **Showcase** (3): SHW-01..03 — Phases 1–3 (рос параллельно)

### Active

<!-- v1.1 scope — see REQUIREMENTS.md for full list with REQ-IDs. -->

- [ ] **Icon system** — порт Feather в AeroIcons (~120–150 ImageVector-констант, regular weight)
- [ ] **Migration** — замена всех текстовых глифов и Material Icons-вызовов на AeroIcons
- [ ] **Dependency cleanup** — удалить `compose.materialIconsExtended` из `:library`
- [ ] **Showcase IconsSection** — сетка + поиск

### Out of Scope

- Мобильные платформы (Android/iOS) — Compose Desktop only
- Web-версия — не планируется
- Публикация в Maven Central — только локальный Maven для начала
- Встроенная поддержка локализации (i18n) — на усмотрение разработчика-потребителя
- Несколько весов иконок (Phosphor-style thin/light/bold/fill) — только regular в v1.1
- Filled / duotone варианты — только outline (stroke-based) в v1.1
- Кастомные пользовательские иконки через AeroIcons API — пользователь использует обычный `ImageVector` напрямую
- Иконки в отдельном Gradle-модуле — всё в `:library` для v1.1 (отделение возможно позже)

## Context

- **Исходная программа:** `C:\1A_WORK\lastver_131\mordred` — Compose Desktop приложение управления сеансами связи со спутниками. Уже содержит рабочие реализации AeroTitleBar, CompactTextField, MordredButton, MordredChip и системы тем. Визуальный стиль и цветовые схемы берутся именно оттуда.
- **Glass-эффекты:** В mordred реализованы через `GlassModifiers.kt` — три модификатора (`glassEffect`, `glassPanel`, `glassSurface`) с градиентами, полупрозрачностью и рамками. Логика переносится в библиотеку.
- **Анимации:** Каждая анимация (hover, раскрытие, переходы) утверждается пользователем отдельно перед реализацией.
- **Обсуждение компонентов:** Форма, логика и параметры каждого компонента или группы компонентов обсуждаются с пользователем перед реализацией.

## Constraints

- **Tech stack:** Kotlin + Compose Desktop (Multiplatform), Gradle Kotlin DSL, JDK 17
- **Зависимости:** Material 3 как основа, кастомный стиль поверх — не заменять Material полностью, а оборачивать/расширять
- **Совместимость:** Compose Desktop (Windows primary, Linux/macOS secondary)
- **Именование:** Все компоненты с префиксом `Aero` (AeroButton, AeroTextField и т.д.)
- **Распространение:** Maven/JAR артефакт — `com.mordred:aero-compose-ui`

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Prefix `Aero` для всех компонентов | Избегает конфликтов с Material3 именами в проектах-потребителях | — Pending |
| Material3 как основа (не замена) | Переиспользует accessibility, семантику, state management | — Pending |
| Три темы из mordred как дефолтные | Уже отработаны визуально в реальном проекте | — Pending |
| Отдельный showcase-модуль | Позволяет проверять компоненты между фазами без внешнего проекта | — Pending |
| Каждая анимация утверждается отдельно | Контроль над визуальной сложностью и производительностью | — Pending |
| AeroIcons как порт Phosphor Regular (stroke ~1.5, 256×256 viewBox) | MIT, мягкий скруглённый outline — ближе к Win7-toolbar-glyph, чем плоский Feather; rounded caps/joins ложатся на Aero-эстетику | — Pending |
| Один вес (Regular), без filled / glass-treatment | Filled-варианты противоречат «мягкий outline без gloss»; glass-обвязка вокруг каждой иконки — отдельный AeroIconButton-уровень, не уровень иконки | — Pending |
| Типизированные константы AeroIcons.* (не name-based lookup) | Compile-time safety, IDE autocomplete, привычно после Material Icons | — Pending |
| materialIconsExtended удаляется из :library | «Единый набор векторных иконок» — Material визуально не Aero; снижает вес JAR | — Pending |
| Иконки в :library, не в отдельном :icons | Меньше Gradle-сложности для v1.1; разделение возможно в v2.0+ если появится консьюмер | — Pending |

---
*Last updated: 2026-04-28 after starting milestone v1.1 Icon System*
