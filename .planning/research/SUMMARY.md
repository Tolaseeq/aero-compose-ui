# Project Research Summary

**Project:** aero-compose-ui
**Domain:** Compose Desktop UI component library (Windows Aero / glassmorphism visual style)
**Researched:** 2026-04-27
**Confidence:** HIGH

## Executive Summary

aero-compose-ui — Kotlin/Compose Multiplatform Desktop библиотека компонентов с эстетикой Windows 7 Aero. Рекомендуемый подход: оборачивать Material3-примитивы, а не заменять их; поверх строить систему токенов (`AeroColorScheme` / `AeroTypography` / `AeroShapes`) через `CompositionLocal`; стеклянный вид реализовывать через единый `drawBehind`-модификатор, а не стеком полупрозрачных `Box`.

Проект имеет сильный задел: `GlassModifiers.kt`, `MordredColorScheme` и три валидированные темы (AeroBlue, AeroDark, Classic) переносятся из mordred напрямую. Стек полностью зафиксирован: Kotlin 2.3.21, CMP 1.10.3, Gradle 9.4.1 с Kotlin DSL и `libs.versions.toml`, `maven-publish` для локальной доставки.

**Главный риск — `AeroTitleBar`:** комбинация `undecorated=true` + `transparent=true` вызывает `EXCEPTION_ACCESS_VIOLATION` на Windows 11 (JetBrains issue #3757). Митигация: `undecorated=true` без `transparent=true`, стекло симулируется градиентом на тёмном фоне. `WindowDraggableArea` не передаёт `HTCAPTION` ОС — Aero Snap невозможен, это задокументированное ограничение.

## Key Findings

### Stack

| Компонент | Версия | Уверенность |
|-----------|--------|-------------|
| Kotlin | 2.3.21 | HIGH |
| Compose Multiplatform | 1.10.3 | HIGH |
| Gradle | 9.4.1 (Kotlin DSL) | HIGH |
| JDK | 17 min / 21 рекомендован | HIGH |
| kotlinx-coroutines | 1.10.2 | HIGH |
| maven-publish (built-in) | — | HIGH |
| Haze (GPU blur) | 1.7.2 | MEDIUM — опционально, не нужно для v1 |

**Критично:** в библиотечном модуле использовать `compose.desktop.common`, **не** `currentOs` — иначе в JAR попадёт платформо-зависимый Skiko-бинарник.

### Features

**Table stakes (P1 — обязательно для v1):**
- `AeroTheme` + `AeroColorScheme` + 3 темы + `GlassModifiers`
- `AeroButton` (filled + outlined + icon), `AeroTextField`, `AeroCheckbox`, `AeroRadioButton`, `AeroSwitch`
- `AeroDropdown` / `AeroComboBox`, `AeroSlider`, `AeroProgressBar`
- `AeroCard` / `AeroPanel`, `AeroScrollArea` + `AeroScrollBar`
- `AeroDialog` / `AeroAlertDialog`, `AeroTooltip`, `AeroContextMenu`
- `AeroToast` / `AeroSnackbar` / `AeroNotificationBanner`
- `AeroTitleBar`, `AeroMenuBar`, `AeroTabBar`, `AeroStatusBar`
- Showcase-приложение (отдельный модуль)

**Should-have (P2 — v1.x):**
- `AeroDataTable` (нет аналога в экосистеме Compose Desktop)
- `AeroTreeView` (Jewel имеет только несвязанный `BasicLazyTree`)
- `AeroDatePicker` / `AeroTimePicker` / `AeroDateRangePicker` (нет аналога нигде)
- `AeroColorPicker`, `AeroRangeSlider`, `AeroSplitPane`, `AeroAccordion`, `AeroSidebar`, `AeroStepperWizard`

**Defer v2+:** DWM/OS-blur, Maven Central, Android/iOS/Web, RTL/i18n.

**Anti-features:** настоящее OS-стекло как дефолт, мультиплатформа (не Desktop), WCAG-гарантии в v1.

### Architecture

4 слоя без обратных зависимостей:

```
theme/          AeroColorScheme (@Immutable), staticCompositionLocalOf, AeroTheme → MaterialTheme
    ↓
modifiers/      GlassModifiers (drawBehind, явный AeroColorScheme-параметр, не @Composable)
    ↓
components/     7 пакетов; stateless-ядро + stateful-обёртка; читают AeroTheme.colors
    ↓
:showcase       отдельный Gradle-модуль, project-зависимость от :library
```

**Порядок сборки:** theme → glass modifiers → atomic components → showcase skeleton → composite → complex stateful → window chrome.

### Pitfalls (топ-5)

| # | Проблема | Митигация | Фаза |
|---|----------|-----------|------|
| 1 | `undecorated+transparent` крашится на Win11 | `undecorated=true` без `transparent`; симулировать стекло градиентом | Фаза 1 |
| 2 | Glass-overdraw из стека `Box`-слоёв → 20 FPS на iGPU | Всё стекло в одном `drawBehind`-блоке | Фаза 1 |
| 3 | M3-дефолты просачиваются (фиолетовый ripple) | Явно переопределять `LocalRippleConfiguration` в каждом компоненте | Фазы 2–3 |
| 4 | Нестабильные `List<T>`-параметры → вся ветка пересчитывается | `ImmutableList` + `@Immutable` на всех токенах | Фаза 1 |
| 5 | Внутренние типы утекают в публичный JAR | `explicitApi()` в `build.gradle.kts` с первого дня | Фаза 1 |

## Implications for Roadmap

**Рекомендуемые 5 фаз:**

1. **Foundation and Theme System** — `AeroColorScheme` + `GlassModifiers` + валидация краша на Win11; всё блокируется этим. `explicitApi()` и структура модулей фиксируются до любого кода.

2. **Atomic Components** — кнопки, поля ввода, чекбоксы, слайдер, разделители; нет межкомпонентных зависимостей; определяет паттерны hover/focus/disabled, унаследованные всеми последующими.

3. **Composite and Navigation** — `AeroCard`, `AeroScrollArea`, `AeroDialog`, `AeroDropdown`, `AeroContextMenu`, `AeroTooltip`, `AeroTitleBar`, `AeroMenuBar`, `AeroTabBar`, `AeroStatusBar`; зависят от атомарных компонентов.

4. **Complex Stateful Components** — `AeroDataTable`, `AeroTreeView`, `AeroDatePicker`, `AeroColorPicker`, `AeroRangeSlider`; аналогов нет, наибольшая внутренняя сложность; `ImmutableList` критичен.

5. **Advanced Layout and v1 Polish** — `AeroAccordion`, `AeroSplitPane`, `AeroDrawer`, `AeroSidebar`, `AeroStepperWizard`; тест software-рендерера; аудит JAR; `publishToMavenLocal` верификация; полная showcase.

**Нужен доп. ресёрч при планировании:**
- Фаза 3: `AeroTitleBar` + JNA / `WM_NCHITTEST` для нативного Aero Snap
- Фаза 4: `AeroDatePicker` — нет референсной реализации для Compose Desktop
- Фаза 4: `AeroColorPicker` — нет референсной реализации для Compose Desktop

**Стандартные паттерны (ресёрч не нужен):**
- Фаза 1: зеркалит mordred + официальная документация
- Фаза 2: канонический паттерн оборачивания M3
- Фаза 5: стандартный Gradle-инструментарий

## Confidence Assessment

| Область | Уверенность | Примечание |
|---------|-------------|-----------|
| Stack | HIGH | Версии верифицированы через официальные docs + GitHub releases |
| Features | MEDIUM-HIGH | Table stakes подтверждены Jewel; Aero-паттерны — из mordred + API Guidelines |
| Architecture | HIGH | Официальные Android docs + инспекция mordred + Jewel как референс |
| Pitfalls (критические) | HIGH | Issue tracker с репродьюсерами |
| Pitfalls (производительность) | MEDIUM | Общая документация Compose, экстраполяция |

**Итого: HIGH**

### Gaps

- Статус краша Win11 в CMP 1.10.3 (может быть починено — валидировать в фазе 1)
- Haze vs. gradient-only — решение в фазе 1 по итогам демо
- AeroTitleBar + нативный snap — нужен отдельный spike
- Лицензирование шрифтов (если будут бандлиться Windows-эра шрифты)
- Baseline Compose compiler metrics не установлен
