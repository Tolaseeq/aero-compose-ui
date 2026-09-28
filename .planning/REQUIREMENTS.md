# Requirements: aero-compose-ui — v3.2 Native Window Behavior

**Defined:** 2026-09-25
**Core Value:** Разработчик подключает одну зависимость и получает полный набор Aero-styled компонентов с тремя темами, кастомной шапкой окна, типизированным набором иконок и демо-витриной — без необходимости реализовывать стиль или искать совместимый icon pack самостоятельно.

**Milestone goal:** Windows обращается с окнами на `AeroTitleBar` (+ `AeroResizeHandles`) как с родными окнами — в каждом приложении на библиотеке. Первый потребитель — Pinya (Phase 2, D-25): её открепляемое окно очереди опирается на снаппинг Windows и ждёт релиза `v3.2.0`.

**Research:** `.planning/research/SUMMARY.md` (+ STACK / FEATURES / ARCHITECTURE / PITFALLS). Ожидаемый подход мейнтейнера проверяется, а не принимается на веру; противоречия между исследованиями решает первая проверка на живом окне, а не новое исследование.

**Стоп-правило:** если окажется, что на `undecorated = true, transparent = false` и обычном JDK 21 (без JBR) нужного поведения не добиться, работа останавливается и вопрос уходит мейнтейнеру. Переход на декорированное окно, прозрачность или JBR-only API без его решения не допускается.

## v3.2 Requirements

### Window management (SNAP)

Всё это делает сама Windows; библиотека только правильно сообщает ей, где у окна шапка, кнопка «развернуть» и края. Собственной логики прилипания нет.

- [ ] **SNAP-01**: Перетаскивание окна за шапку к левому или правому краю экрана прилепляет его к половине, в угол — к четверти, к верхнему краю — развёртывает. Утаскивание прилипшего или развёрнутого окна за шапку возвращает ему прежний размер
- [ ] **SNAP-02**: На Windows 11 наведение на кнопку «развернуть» показывает меню раскладок (Snap Layouts); выбор раскладки размещает окно
- [x] **SNAP-03**: Win+← / Win+→ прилепляют окно к половине экрана, Win+↑ развёртывает, Win+↓ восстанавливает и сворачивает — как у родного окна
- [ ] **SNAP-04**: Двойной щелчок по области перетаскивания шапки развёртывает окно, повторный — восстанавливает
- [ ] **SNAP-05**: Alt+Space открывает системное меню окна; его пункты (восстановить, переместить, размер, свернуть, развернуть, закрыть) работают, «закрыть» идёт тем же путём, что и Alt+F4 (`onCloseRequest` окна)
- [ ] **SNAP-06**: Два окна, прилепленные рядом, делят общую границу — растягивание одного двигает второе; пара видна в панели задач как группа (Snap Groups, Windows 11)
- [ ] **SNAP-07**: FancyZones из PowerToys подхватывает окно: перетаскивание с Shift раскладывает его по зонам

### Window frame and geometry (WIN)

- [x] **WIN-01**: Развёрнутое окно занимает ровно рабочую область своего монитора: видимую панель задач не перекрывает, а автоскрываемой оставляет край, чтобы она по-прежнему выезжала при наведении
- [x] **WIN-02**: Окно растягивается за любой край и угол средствами Windows (системный курсор, живое изменение размера), в том числе узкое окно ~300 px. На Windows зоны `AeroResizeHandles` сами размер больше не меняют — иначе окно растягивается дважды и не работает общая граница SNAP-06; на других ОС они работают как сейчас
- [x] **WIN-03**: Окно без системной рамки и её следов: нет белой полосы сверху, нет классических кнопок заголовка, содержимое не съезжает и не обрезается ни в обычном, ни в развёрнутом состоянии. Если Windows 11 после изменения начинает скруглять углы и рисовать тень, вид углов и тени выбирает мейнтейнер по превью с вариантами в ходе фазы
- [ ] **WIN-04**: Переход окна между мониторами 100% ↔ 150% (перетаскиванием и Win+Shift+стрелкой) сохраняет разумный размер: шапка и кнопки в правильном масштабе, зоны нажатия совпадают с нарисованным, размер не скачет
- [x] **WIN-05**: Значок «развернуть / восстановить» в `AeroTitleBar` и `WindowState.placement` соответствуют настоящему состоянию окна после развёртывания, прилипания и восстановления средствами Windows
- [x] **WIN-06**: Несколько таких окон в одном приложении работают независимо, включая узкое окно ~300 px с интерактивным элементом в шапке; закрытие одного не ломает остальные

### Title bar controls (BTN)

- [x] **BTN-01**: Кнопка «развернуть» подсвечивается при наведении, показывает нажатие и срабатывает по клику так же, как «свернуть» и «закрыть», хотя для Windows она теперь системная и мышь над ней приходит неклиентскими сообщениями
- [x] **BTN-02**: «Свернуть», «закрыть» и интерактивное содержимое шапки (слот `leading` и элементы, помеченные по API-02) получают обычные клики и не начинают перетаскивание окна

### Public API (API)

- [x] **API-01**: Существующие вызовы `AeroTitleBar(...)` и `AeroResizeHandles(...)` компилируются без изменений; публичный API только дополняется, типы JNA в публичные сигнатуры не попадают. На Linux и macOS поведение прежнее (`WindowDraggableArea` + зоны растягивания Compose)
- [x] **API-02**: Потребитель может пометить свой элемент внутри шапки (например, кнопку «вернуть очередь» в Pinya) как кликабельный — он получает клики, а не перетаскивает окно
- [x] **API-03**: Потребитель может отключить нативное поведение для отдельного окна и получить сегодняшнее
- [x] **API-04**: Потребитель, который рисует свою шапку без `AeroTitleBar`, получает то же нативное поведение окна через публичный низкоуровневый API (`rememberAeroWindowChrome()`): сам помечает перетаскиваемую область шапки, кнопку «развернуть» и кликабельные элементы и получает состояние наведения и нажатия кнопки «развернуть»; типы JNA в публичные сигнатуры не попадают. Бывший API-F01, перенесён в v3.2 решением мейнтейнера

### Dependency (DEP)

- [x] **DEP-01**: JNA + jna-platform `5.19.1` подключены к `:library` как внутренняя зависимость (`implementation`); потребитель, который уже зависит от JNA 5.19.1 (Pinya), получает одну версию JNA без конфликта

### Showcase (SHW)

- [x] **SHW-17**: Витрина умеет открыть второе узкое окно (~300 px) с `AeroTitleBar` и интерактивным элементом в шапке — сценарий Pinya для проверки нескольких окон (WIN-06, API-02)

### Verification (VER)

- [x] **VER-11**: Автоматические проверки на живом окне без движения мыши: что окно отвечает Windows на вопрос «что в этой точке» (шапка, три кнопки, края и углы, клиентская область, помеченные элементы), какие у окна стили, совпадает ли прямоугольник развёрнутого окна с рабочей областью монитора и остаётся ли край для автоскрываемой панели. Каждая проверка доказана падающей на старой реализации с `WindowDraggableArea`
- [x] **VER-12**: Две сессии с настоящими мышью и клавиатурой, обе — после предупреждения мейнтейнера не трогать компьютер и его «ок»; по окончании агент сообщает, что можно вернуться. **Ранняя, 1–2 минуты**, сразу после первого черновика на окне Compose: наведение на «развернуть» показывает меню раскладок, перетаскивание к краю прилепляет окно; если нет — стоп и вопрос мейнтейнеру до остальной работы. **Полная, в конце фазы:** агент прогоняет SNAP-01..07, WIN-01..06 и BTN-01..02 на живом Windows 11 — на обычном JDK 21 и на JBR 21 (витрина под Hot Reload). Появление меню раскладок фиксируется без снимка экрана (например, через UI Automation). На время полной сессии ставятся виртуальный второй экран на 150% (запрос прав подтверждает мейнтейнер) и PowerToys, выключается автоскрытие панели задач; после неё драйвер экрана удалён, автоскрытие включено, PowerToys удаляется по слову мейнтейнера
- [x] **VER-13**: Агент сам осматривает окна (кадры через `PrintWindow`) до того, как их увидит мейнтейнер. Передача — кадры, результаты VER-11 и VER-12 и список «не подтверждено»: всё, что не подтверждено, в том числе поведение на Windows 10, не выдаётся за пройденное
- [x] **VER-14**: Все 541 существующий тест зелёные; новые тесты поднимают залоченное число в `library/build.gradle.kts` коммитом, который называет причину

### Release (REL)

- [x] **REL-06**: В README раздел о поведении окна на Windows: что работает, чем отличаются Windows 10 и 11, как пометить интерактивный элемент шапки и как отключить нативное поведение; новая зависимость JNA 5.19.1 названа
- [x] **REL-07**: KDoc `AeroTitleBar` и `AeroResizeHandles` описывает новое поведение; оговорка «Aero Snap limitation» убрана
- [ ] **REL-08**: Версия `3.2.0` стоит в `build.gradle.kts` до тега; одноразовый проверочный тег собирается на JitPack до настоящего; тег `v3.2.0` отправлен, сборка JitPack `ok`, `com.github.Tolaseeq:aero-compose-ui:v3.2.0` резолвится

## Future Requirements

Признано, но в текущий roadmap не входит.

- **VER-F04**: Проверка на настоящем Windows 10
- **DLG-F01**: Нативное поведение для `AeroDialog` (отдельное undecorated-окно без `AeroTitleBar`)
- Долги v3.0 / v3.1 и кандидаты из PROJECT.md «Next Milestone Goals» — без изменений

## Out of Scope

| Feature | Reason |
|---------|--------|
| Собственная логика прилипания, превью зон, AppBar-докинг, always-on-top | Всё это делает Windows; Pinya D-24 / D-26 прямо полагаются на системное поведение |
| `transparent = true` | Падение на Windows 11 (CMP-3757 / GH#3171) |
| Декорированное окно или JBR-only API вместо `undecorated = true` | Решение мейнтейнера; библиотека должна работать на обычном JDK 21. Если иначе нельзя — стоп и вопрос, не обход |
| Mica / Acrylic / настоящий DWM blur | Отдельная тема; стекло симулируется градиентами |
| Нативная шапка на macOS / Linux | Там поведение остаётся прежним |
| Подсказки на кнопках шапки | Не запрошено |
| Аэро-встряхивание, Win+D, «на всех рабочих столах», миниатюры Alt+Tab как отдельные требования | Приходят с правильными стилями окна сами; если попадутся в сессии VER-12 — записываются, но гейтом не являются |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| SNAP-01 | Phase 22 | Pending |
| SNAP-02 | Phase 22 | Pending |
| SNAP-03 | Phase 22 | Complete |
| SNAP-04 | Phase 22 | Pending |
| SNAP-05 | Phase 22 | Pending |
| SNAP-06 | Phase 22 | Pending |
| SNAP-07 | Phase 22 | Pending |
| WIN-01 | Phase 22 | Complete |
| WIN-02 | Phase 22 | Complete |
| WIN-03 | Phase 22 | Complete |
| WIN-04 | Phase 22 | Pending |
| WIN-05 | Phase 22 | Complete |
| WIN-06 | Phase 22 | Complete |
| BTN-01 | Phase 22 | Complete |
| BTN-02 | Phase 22 | Complete |
| API-01 | Phase 22 | Complete |
| API-02 | Phase 22 | Complete |
| API-03 | Phase 22 | Complete |
| API-04 | Phase 22 | Complete |
| DEP-01 | Phase 22 | Complete |
| SHW-17 | Phase 22 | Complete |
| VER-11 | Phase 22 | Complete |
| VER-12 | Phase 22 | Complete |
| VER-13 | Phase 22 | Complete |
| VER-14 | Phase 22 | Complete |
| REL-06 | Phase 22 | Complete |
| REL-07 | Phase 22 | Complete |
| REL-08 | Phase 22 | Pending |

**Coverage:**
- v3.2 requirements: 28 total
- Mapped to phases: 28
- Unmapped: 0 ✓

### Verification states (updated by 22-26 from the 22-25 re-verification)

Flipped Complete — every clause has a green 22-25 row or still-standing evidence
(`22-25-SUMMARY.md` verdict table; evidence `.captures/22-reverify/{jdk,jbr}/`):

- **SNAP-03** — chords work on both JVMs: JBR 22-25 S03-WIN-RIGHT/UP/DOWN PASS with
  `foregroundOurs=True` (WIN-LEFT FAIL was an un-retried foreground fluke — RIGHT passed
  foreground-ours one check later); JDK 22-15 LEFT/RIGHT/UP PASS, WIN-DOWN behavior proven
  by the standalone rows. The 22-15 JBR chord inertness was foreground-shaped (22-21's
  leading explanation confirmed).
- **WIN-01** — 22-15 W01-MAX-VISIBLE-TASKBAR + W01-AUTOHIDE-REVEAL PASS both JVMs; headless
  `V11-MAX-WORKAREA`/`V11-AUTOHIDE-EDGE` re-proven green on the post-gap-closure build in
  the 22-20/22-23/22-24 proof packs (18/18).
- **WIN-02** — 22-25: floors PASS both JVMs (320/260 exact, D-01 honored), corners TR/BL/BR
  PASS both JVMs (correct shapes + 60x60); edges PASS both JVMs 22-15 + headless
  V11-HT-EDGE/CORNER throughout. The TL-corner 22-25 zero-delta row is recorded as an open
  anomaly (`22-UNCONFIRMED.md` § 17), not a green clause.
- **WIN-03** — standing 22-15/22-16 evidence (W03-FRAMES both JVMs, final FULL-FRAME 0 px
  all schemes); no gap-closure change paints; post-change V11 18/18 proof packs.
- **WIN-05** — placement correspondence 17/17 (JDK, 22-15) + 16/16 (JBR, 22-25, zero
  mismatches); glyph switch proven 22-15 both JVMs (588/616 px); the 22-15 JBR
  float-instability did NOT reproduce (float1-vs-float2 = 0). The 22-25 glyph-switch capture
  read flat (recorded in `22-UNCONFIRMED.md` § 17).
- **BTN-01** — press parity exact on JDK 22-25 (B01-PRESS-FRAME PASS, diff=0, press paints
  144; the 22-15 FAIL was a check-formula artifact, 22-23); hover parity 0 px both JVMs and
  real click toggle both JVMs (22-15, re-proven JDK 22-25). The JBR 22-25 B01 triple FAIL is
  the end-of-pass zero-diff cluster (environment-blocked, not a parity difference).
- **BTN-02** — standing 22-15 evidence: B02-MIN/LEADING/MARKED/CLOSE-CLICK PASS both JVMs;
  no gap-closure change touches client-click routing.

Still Pending — named clause / blocker (NOT failed):

- **SNAP-01** — JDK 22-15 PASS predates the WndProc changes (stale for flipping, per 22-25);
  JBR S01 ×4 FAIL 22-25 — the caption-drag drift is real and narrowed to the drag modal
  loop (not hit-test, not foreground, not click delivery).
- **SNAP-02** — human clause unproven: the flyout layout-pick (UIA blind to flyout zones;
  the 22-25 re-run threw a harness error before the verdict — reference control matched).
- **SNAP-04** — double-click PROVEN on JBR 22-25 (real MAX+RESTORE pair) but FAILED on JDK
  the same run from a verified floating reset — a JVM-differential the record cannot
  explain; not flippable as a requirement.
- **SNAP-05** — menu OPEN proven on JBR (S05-ALTSPACE-MENU PASS, the 22-23 fix); command
  navigation inert (Move/Size/Minimize/Maximize/Close produce no state change with the menu
  open and window foreground); JDK family rows were foreground-blocked (VS Code race).
- **SNAP-06** — border clause unproven: both 22-25 passes failed at PRE-SNAP
  (foreground-blocked chords), so the border drag never ran against a landed pair; the Snap
  Groups taskbar thumbnail was never observed (maintainer observation declined-by-protocol).
- **SNAP-07** — 22-25 S07 FAIL both JVMs: the drag never started foreground-ours even after
  the script's one retry (environment-blocked); the 22-15 JBR observed-snap stands as
  observed-only. No green S07 verdict row on the current build.
- **WIN-04** — environment-blocked (the 150 % virtual display never attached).
- **REL-08** — plan 22-18 (release): version `3.2.0`, verify tag, `v3.2.0`, JitPack.

---
*Requirements defined: 2026-09-25*
*Last updated: 2026-09-28 by plan 22-26 — checkbox flips from the 22-25 re-verification verdict table (SNAP-03, WIN-01..03, WIN-05, BTN-01..02 Complete; audit notes above)*
